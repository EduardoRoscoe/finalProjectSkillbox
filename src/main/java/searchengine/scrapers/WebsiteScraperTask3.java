package searchengine.scrapers;

import lombok.SneakyThrows;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import searchengine.exceptions.*;
import searchengine.model.Page;
import searchengine.model.SiteEntity;
import searchengine.services.IndexingService;
import searchengine.services.PageCRUDService;
import searchengine.services.SiteCRUDService;
import searchengine.util.Verifier;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicInteger;

public class WebsiteScraperTask3 extends RecursiveAction {
    private static final Logger logger = LoggerFactory.getLogger(WebsiteScraperTask3.class);

    private final URL pageUrl;
    private final SiteEntity mainSite;
    private final SiteCRUDService siteCRUDService;
    private final PageCRUDService pageCRUDService;
    private final IndexingService indexingService;
    private final List<WebsiteScraperTask3> taskList = new ArrayList<>();

    private static final int PAGE_MARGIN_OF_ERROR = 500000;
    public static final AtomicInteger pageErrorCount = new AtomicInteger(0);

    private Optional<Document> doc = Optional.empty();
    private boolean existsInDB = false;
    private boolean isPageMainSite = false;

    public WebsiteScraperTask3(URL pageUrl, SiteEntity mainSite, SiteCRUDService siteCRUDService,
                               PageCRUDService pageCRUDService, IndexingService indexingService) {
        this.pageUrl = pageUrl;
        this.mainSite = mainSite;
        this.siteCRUDService = siteCRUDService;
        this.pageCRUDService = pageCRUDService;
        this.indexingService = indexingService;
    }

    public SiteEntity getMainSite() {
        return mainSite;
    }

    @SneakyThrows
    @Override
    protected void compute() {
        if (Thread.currentThread().isInterrupted()) {
            logger.warn("Task interrupted before execution: {}", pageUrl);
            return;
        }

        Instant start = Instant.now();
        String mainSiteUrl = mainSite.getUrl();
        String pageUrlString = pageUrl.toString();
        isPageMainSite = pageUrlString.equals(mainSiteUrl) || pageUrlString.equals(mainSiteUrl + "/");

        if (!Verifier.siteIsValidFormat(pageUrlString) && isPageMainSite) {
            throw new SiteIndexationErrorException("Invalid format site '" + pageUrlString + "'");
        }

        Page page = new Page();
        int httpCode = 0;

        try {
            Connection connection = Jsoup.connect(pageUrlString);
            Connection.Response response = connection.execute();
            httpCode = response.statusCode();
            doc = Optional.of(connection.userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                    .referrer("http://www.google.com").get());
        } catch (HttpStatusException e) {
            httpCode = e.getStatusCode();
            page.setCode(httpCode);
            handlePageError(pageUrlString, e);
        } catch (IOException e) {
            logger.warn("IOException with site pageUrlString: {}", pageUrlString, e);
            handlePageError(pageUrlString, e);
        }

        Elements subPages = new Elements();
        String content = "";

        if (doc.isPresent()) {
            subPages = doc.get().select("a[href]");
            content = doc.get().html();
        } else {
            logger.warn("Returned an empty doc");
            return;
        }

        if (Thread.currentThread().isInterrupted()) {
            logger.warn("Task interrupted during document fetching: {}", pageUrlString);
            return;
        }

        String path = pageUrl.getPath();
        page.setPath(isPageMainSite ? pageUrlString : path);
        page.setSite(mainSite);
        page.setContent(content);
        page.setCode(httpCode);

        synchronized (pageCRUDService) {
            try {
                existsInDB = pageCRUDService.existsByPathAndSite(page.getPath(), mainSite);
                if (!existsInDB) {
                    Instant beforeCreatingPage = Instant.now();
                    pageCRUDService.create(page);
                    Instant afterCreatingPage = Instant.now();
                    long durationOfCreatingPage = Duration.between(beforeCreatingPage, afterCreatingPage).toMillis();
                    logger.debug("Duration of creating a page: {}", durationOfCreatingPage);
                    logger.info("Page has been added to DB: {}", pageUrlString);
                }
            } catch (Exception e) {
                logger.warn("Page '{}' from site '{}' failed to add to DB.", page.getPath(), page.getSite().getUrl());
            }
        }

        if (Thread.currentThread().isInterrupted()) {
            logger.warn("Task interrupted after saving page to database: {}", pageUrlString);
            return;
        }

        mainSite.setStatusTime(LocalDateTime.now());
        siteCRUDService.updateById(mainSite);

        Page pageWithId = pageCRUDService.getByPathAndSite(page.getPath(), mainSite);

        if (!existsInDB) {
            indexingService.indexLemmasForWebsiteScraper(content, pageWithId);
        }

        if (Thread.currentThread().isInterrupted()) {
            logger.warn("Task interrupted after indexing lemmas: {}", pageUrlString);
            return;
        }

        for (Element subPage : subPages) {
            String subPageUrlString = subPage.absUrl("href");
//            logger.info("SubPageUrlString: {}", subPageUrlString);
            if (!subPageUrlString.endsWith("/")) {
                subPageUrlString += "/";
            }
            URL subPageUrl;
            try {
                subPageUrl = new URL(subPageUrlString);
            } catch (MalformedURLException e) {
                continue; // Skip this link if it is malformed
            }
            String subPageUrlPath = subPageUrl.getPath();
            if (subPageUrlPath.equals("/") || subPageUrlPath.isEmpty()) { // skip the link if it's the homepage
                continue;
            }
            int siteId = mainSite.getId();
            mainSite.setId(siteId);

            synchronized (pageCRUDService) {
                existsInDB = pageCRUDService.existsByPathAndSite(subPageUrlPath, mainSite);
            }
            if (subPageUrlIsValid(mainSiteUrl, subPageUrlString) && !existsInDB) {
                WebsiteScraperTask3 scraperTask = new WebsiteScraperTask3(subPageUrl, mainSite, siteCRUDService, pageCRUDService, indexingService);
                scraperTask.fork();
                taskList.add(scraperTask);
            }
        }

        Instant end = Instant.now();
        Duration executionTime = Duration.between(start, end);
        logger.info("Task {} duration '{}'.", this, executionTime.toMillis());

        for (WebsiteScraperTask3 task : taskList) {
            task.join();
        }
    }

    private synchronized boolean subPageUrlIsValid(String mainSiteUrl, String subLinkHref) {
        if (subLinkHref.isEmpty()) {
            return false;
        }
        String[] parts = subLinkHref.split("/");
        String lastPart = parts.length > 0 ? parts[parts.length - 1] : "";
        String mainsSiteWithoutWWW = mainSiteUrl.replace("www.", "");

        return (subLinkHref.startsWith(mainSiteUrl) || subLinkHref.startsWith(mainsSiteWithoutWWW))
                && !(subLinkHref.contains("?") || subLinkHref.contains("tags") || subLinkHref.contains("tagged") || subLinkHref.contains("mailto:"))
                && !subLinkHref.matches(".*\\.(jpg|png|pdf|css|js|ico|webp|zip)$")
                && !lastPart.matches("\\d+")
                && !lastPart.contains("#");
    }

    private void handlePageError(String url, Exception e) throws SiteIndexationErrorException, UnableToConnectToSiteException {
        if (isPageMainSite){ // if the url is the main site url
            logger.info("LinkHref inside IOException catch is: {}", pageUrl);
            throw new UnableToConnectToSiteException("Unable to connect to site '" + url + "' in WebsiteScraperTask", e);
        } else {
            pageErrorCount.incrementAndGet();
            logger.warn("Page with url '{}' failed to add to DB. PageErrorCount: {}", url, pageErrorCount);
        }
        if (pageErrorCount.get() > PAGE_MARGIN_OF_ERROR) {
            logger.error("Too many page errors: {}", pageErrorCount);
            throw new TooManyPageErrorsException("Too many page errors: " + pageErrorCount, e);
        }
    }

    private void handleIndexPageError(String url, Exception e) throws SiteIndexationErrorException, UnableToConnectToSiteException {
        if (isPageMainSite){ // if the url is the main site url
            throw new SiteIndexationErrorException("Site failed to index its homepage: " + url, e);
        } else {
            pageErrorCount.incrementAndGet();
            logger.warn("Page with url '{}' failed to add to index. PageErrorCount: {}", url, pageErrorCount);
        }
        if (pageErrorCount.get() > PAGE_MARGIN_OF_ERROR) {
            logger.error("Too many page errors: {}", pageErrorCount);
            throw new TooManyPageErrorsException("Too many page errors: " + pageErrorCount, e);
        }
    }

    public void resetPageErrorCount() {
        pageErrorCount.set(0);
    }
}
