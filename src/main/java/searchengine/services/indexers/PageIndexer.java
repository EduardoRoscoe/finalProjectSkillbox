package searchengine.services.indexers;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.exceptions.LemmaNotFoundException;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.SiteEntity;
import searchengine.services.*;
import searchengine.util.FormatterUrl;
import searchengine.util.HtmlTextUtilities;

import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Map;

public class PageIndexer {
    private static Logger logger = LoggerFactory.getLogger(PageIndexer.class);
    private final SiteCRUDService siteCRUDService;
    private final PageCRUDService pageCRUDService;
    private final IndexEntityCRUDService indexEntityCRUDService;
    private final LemmaCRUDService lemmaCRUDService;
    private final LemmaFinder lemmaFinder;
    private final SitesList sitesList;

    public PageIndexer(SiteCRUDService siteCRUDService, PageCRUDService pageCRUDService,
                       IndexEntityCRUDService indexEntityCRUDService, LemmaCRUDService lemmaCRUDService,
                       LemmaFinder lemmaFinder, SitesList sitesList) {
        this.siteCRUDService = siteCRUDService;
        this.pageCRUDService = pageCRUDService;
        this.indexEntityCRUDService = indexEntityCRUDService;
        this.lemmaCRUDService = lemmaCRUDService;
        this.lemmaFinder = lemmaFinder;
        this.sitesList = sitesList;
    }

    public void indexPage(String url) throws IOException {
        logger.info("Url from indexPage Indexing service method: {}", url);
        String formattedUrl = FormatterUrl.verifyAndFormatUrl(url);
        URL urlFormat = new URL(formattedUrl);

        String path = urlFormat.getPath();
        String pageSiteURL = "https://" + urlFormat.getHost() + "/";

        Connection connection = Jsoup.connect(formattedUrl);
        Document doc = fetchDocument(connection);
        int httpCode = fetchStatusCode(connection);

        SiteEntity siteEntity = findSiteEntity(pageSiteURL);
        boolean pageAlreadyExisted = pageCRUDService.existsByPathAndSite(path, siteEntity);
        if (pageAlreadyExisted) {
            Page oldPage = pageCRUDService.getByPathAndSite(path, siteEntity);
            HashSet<Lemma> lemmasOfOldPage = indexEntityCRUDService.findLemmasByPage(oldPage);
            pageCRUDService.deleteAndDecreaseFrequencyLema(oldPage, lemmasOfOldPage);
        }
        Page page = createOrUpdatePage(path, doc.html(), httpCode, siteEntity);

        Page pageWithId = pageCRUDService.getByPathAndSite(path, siteEntity);
        try {
            indexLemmasForSinglePage(doc.html(), pageWithId);
        } catch (Exception e) {
            handleIndexLemmaForSinglePageError(e, formattedUrl, pageWithId);
            throw e;
        }

        logPageAction(pageAlreadyExisted, formattedUrl);
    }

    private Document fetchDocument(Connection connection) throws IOException {
        return connection
                .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                .referrer("http://www.google.com")
                .get();
    }

    private int fetchStatusCode(Connection connection) throws IOException {
        return connection.execute().statusCode();
    }

    private SiteEntity findSiteEntity(String pageSiteURL) {
        for (Site site : sitesList.getSites()) {
            String configURL = site.getUrl();
            if (pageSiteURL.equals(configURL) || configURL.equals("https://" + pageSiteURL)
                    || configURL.equals("http://" + pageSiteURL)) {
                return siteCRUDService.getSiteFromSiteConfigOrCreate(site);
            }
        }
        throw new IllegalArgumentException("Page is outside configured sites");
    }

    private Page createOrUpdatePage(String path, String html, int httpCode, SiteEntity siteEntity) {
        Page page = new Page();
        page.setContent(html);
        page.setCode(httpCode);
        page.setPath(path);
        page.setSite(siteEntity);

        synchronized (pageCRUDService) {
            pageCRUDService.createOrUpdate(page);
        }
        return page;
    }

    private void logPageAction(boolean pageAlreadyExisted, String url) {
        if (pageAlreadyExisted) {
            logger.info("Page from indexPage Indexing Service method has been updated: {}", url);
        } else {
            logger.info("Page from indexPage Indexing Service method has been added: {}", url);
        }
    }

    public void indexLemmasForSinglePage(String html, Page pageWithId) {
        String cleanedText = HtmlTextUtilities.cleanHtml(html);
        Map<String, Integer> lemmas = lemmaFinder.collectLemmas(cleanedText);
        processLemmasForSinglePage(lemmas, pageWithId);
        logger.info("Леммы со страницы '{}' были проиндексированы.", pageWithId.getSite().getUrl() + pageWithId.getPath());
    }

    private void processLemmasForSinglePage(Map<String, Integer> lemmas, Page pageWithId) {
        SiteEntity siteEntity = pageWithId.getSite();
        for (Map.Entry<String, Integer> entry : lemmas.entrySet()) {
            lemmaCRUDService.createOrUpdateAndIncreaseFrequencyByOne(entry.getKey(), siteEntity);
            Lemma lemmaWithId = lemmaCRUDService.getLemmaByLemmaAndSite(entry.getKey(), siteEntity);
            indexEntityCRUDService.createOrUpdateByPageLemmaAndRank(pageWithId, lemmaWithId, entry.getValue());
        }
    }

    private void handleIndexLemmaForSinglePageError(Exception e, String formattedUrl, Page pageWithId) {
        logger.error("Error indexing lemmas for page {}: {}", formattedUrl, e.getMessage());
        try {
            HashSet<Lemma> lemmasOfFailedPage = indexEntityCRUDService.findLemmasByPage(pageWithId);
            pageCRUDService.deleteAndDecreaseFrequencyLema(pageWithId, lemmasOfFailedPage);
        } catch (LemmaNotFoundException le) {
            logger.debug("No lemmas found for page {}: {}", formattedUrl, le.getMessage());
        }
        pageCRUDService.delete(pageWithId.getId());
    }
}
