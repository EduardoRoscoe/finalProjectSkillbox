package searchengine.services.indexers;

import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.exceptions.*;
import searchengine.model.Lemma;
import searchengine.model.SiteEntity;
import searchengine.model.Status;
import searchengine.scrapers.WebsiteScraperTask3;
import searchengine.services.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RejectedExecutionException;

public class SitesIndexer {

    private final SiteCRUDService siteCRUDService;
    private final PageCRUDService pageCRUDService;
    private final LemmaCRUDService lemmaCRUDService;
    private final IndexEntityCRUDService indexEntityCRUDService;
    private final List<SiteEntity> runningSitesList = new ArrayList<>();
    private final List<SiteEntity> indexedSites = new ArrayList<>();
    private ForkJoinPool pool;
    private boolean hasStoppedByUser = false;
    private boolean indexSitesIsRunning = false;
    private boolean isComplete = false;
    private final org.slf4j.Logger logger = LoggerFactory.getLogger(SitesIndexer.class);


    public SitesIndexer(
            SiteCRUDService siteCRUDService,
            PageCRUDService pageCRUDService,
            LemmaCRUDService lemmaCRUDService,
            IndexEntityCRUDService indexEntityCRUDService
    ) {
        this.siteCRUDService = siteCRUDService;
        this.pageCRUDService = pageCRUDService;
        this.lemmaCRUDService = lemmaCRUDService;
        this.indexEntityCRUDService = indexEntityCRUDService;
    }

    @Async
    public void indexSites(SitesList sitesList) throws Exception{
        indexSitesIsRunning = true;
        initializeIndexing(sitesList);
        List<WebsiteScraperTask3> tasks = createScraperTasks(sitesList);
        processScraperTasks(tasks);
        finalizeIndexing();
    }

    private void initializeIndexing(SitesList sitesList) {
        siteCRUDService.deleteExistingSitesOfSitesList(sitesList);
        siteCRUDService.createSitesWithIndexingStatus(sitesList);
//        indexSitesIsRunning = true;
        hasStoppedByUser = false;
    }

    private List<WebsiteScraperTask3> createScraperTasks(SitesList sitesList) throws MalformedURLException {
        List<WebsiteScraperTask3> tasks = new ArrayList<>();
        for (Site site : sitesList.getSites()) {
            SiteEntity siteEntity = prepareSiteEntity(site);
            URL siteURL = new URL(site.getUrl());
            IndexingService indexingService = new IndexingService(sitesList, siteCRUDService, pageCRUDService,
                    indexEntityCRUDService, lemmaCRUDService);
            WebsiteScraperTask3 scraperTask = new WebsiteScraperTask3(siteURL, siteEntity, siteCRUDService, pageCRUDService, indexingService);
            tasks.add(scraperTask);
        }
        return tasks;
    }

    private SiteEntity prepareSiteEntity(Site site) {
        int id = siteCRUDService.getIdByURL(site.getUrl());
        SiteEntity siteEntity = siteCRUDService.getById(id);
        siteEntity.setStatus(Status.QUEUED);
        siteEntity.setStatusTime(LocalDateTime.now());
        runningSitesList.add(siteEntity);
        siteCRUDService.updateById(siteEntity);
        return siteEntity;
    }

    private void processScraperTasks(List<WebsiteScraperTask3> tasks) throws LoopSiteIndexationCustomException {
        LinkedHashMap<String, Exception> exceptionsWithSiteUrl = new LinkedHashMap<>();
        for (WebsiteScraperTask3 task : tasks) {
            if (!hasStoppedByUser) {
                processSingleTask(task, exceptionsWithSiteUrl);
            } else {
                handleStoppedByUser(task.getMainSite(), task, exceptionsWithSiteUrl);
            }
        }
        if (!exceptionsWithSiteUrl.isEmpty()) {
            throw new LoopSiteIndexationCustomException(exceptionsWithSiteUrl, indexedSites);
        }
    }

    private void processSingleTask(WebsiteScraperTask3 task, LinkedHashMap<String, Exception> exceptionsWithSiteUrl) {
        // check if the task has been stopped by user
        if (!hasStoppedByUser) {
            pool = new ForkJoinPool();
        }
        SiteEntity site = task.getMainSite();
        try {
            Instant beforeInvokingTask = Instant.now();
            startTaskIndexing(task, site);
            if (hasStoppedByUser) {
                handleStoppedByUser(site, task, exceptionsWithSiteUrl);
                return;
            }
            updateLemmasFrequency(site);
            completeTaskIndexing(task, site);

            Instant finishedIndexing = Instant.now();
            logger.info("Duration of indexing site {}: {} ms", site.getUrl(),
                    Duration.between(beforeInvokingTask, finishedIndexing).toMillis());
            logger.info("Site '{}' has been indexed successfully", site.getUrl());
        } catch (RejectedExecutionException | CancellationException e) {
            String errorMessage = "Индексация остановлена пользователем";
            SiteStoppedByUserException siteStoppedByUserException = new SiteStoppedByUserException("Индексация остановлена пользователем", e);
            handleTaskException(task, site, siteStoppedByUserException, exceptionsWithSiteUrl, errorMessage);
        } catch (SiteIndexationErrorException | UnableToConnectToSiteException e) {
            String errorMessage = e.getClass().getSimpleName() + ": " + e.getMessage();
            SiteIndexationErrorException siteIndexationErrorException = new SiteIndexationErrorException(e.getMessage(), e);
            handleTaskException(task, site, siteIndexationErrorException, exceptionsWithSiteUrl, errorMessage);
        }
    }

    private void startTaskIndexing(WebsiteScraperTask3 task, SiteEntity site) {
        site.setStatus(Status.INDEXING);
        siteCRUDService.updateById(site);
        pool.invoke(task);
    }

    public void handleStoppedByUser(SiteEntity site, WebsiteScraperTask3 task, LinkedHashMap<String, Exception> exceptionsWithSiteUrl) {
        logger.error("Индексация остановлена пользователем without an error");
        site.setStatus(Status.FAILED);
        site.setLastError("Индексация остановлена пользователем");
        siteCRUDService.updateById(site);
        SiteStoppedByUserException siteStoppedByUserException = new SiteStoppedByUserException("Индексация остановлена пользователем");
        exceptionsWithSiteUrl.put(site.getUrl(), siteStoppedByUserException);
        task.resetPageErrorCount();
    }

    public void updateLemmasFrequency(SiteEntity site) {
        Instant beforeUpdatingLemmas = Instant.now();
        logger.info("Started to update lemmas frequency: {}", beforeUpdatingLemmas);
        List<Lemma> lemmaListOfSite = lemmaCRUDService.getLemmasBySiteId(site.getId());
        for (Lemma lemma : lemmaListOfSite) {
            int frequencyLemma = indexEntityCRUDService.countLemmasInAPageByLemmaID(lemma.getId());
            lemma.setFrequency(frequencyLemma);
            lemmaCRUDService.updateById(lemma);
        }
        Instant afterUpdatingLemmas = Instant.now();
        logger.info("Duration of updating {} lemmas for site {}: {} ms", lemmaListOfSite.size(),
                site.getUrl(), Duration.between(beforeUpdatingLemmas, afterUpdatingLemmas).toMillis());
    }

    private void completeTaskIndexing(WebsiteScraperTask3 task, SiteEntity site) {
        runningSitesList.remove(site);
        indexedSites.add(site);
        site.setStatus(Status.INDEXED);
        siteCRUDService.updateById(site);
        task.resetPageErrorCount();
    }

    private void handleTaskException(WebsiteScraperTask3 task, SiteEntity site, Exception e,
                                     LinkedHashMap<String, Exception> exceptionsWithSiteUrl, String errorMessage) {
        logger.error(errorMessage);
        site.setLastError(errorMessage);
        site.setStatus(Status.FAILED);
        siteCRUDService.updateById(site);
        exceptionsWithSiteUrl.put(site.getUrl(), e);
        runningSitesList.remove(site);
        task.cancel(true);
        task.resetPageErrorCount();
        if (e instanceof TooManyPageErrorsException) {
            logger.error(e.getMessage() + " and shutdown has been activated");
            pool.shutdownNow();
        }
    }

    private void finalizeIndexing() {
        indexSitesIsRunning = false;
        isComplete = true;
    }

    public ForkJoinPool getPool() {
        return pool;
    }

    public List<SiteEntity> getRunningSitesList() {
        return runningSitesList;
    }

    public boolean isComplete() {
        return isComplete;
    }

    public boolean indexSitesIsRunning() {
        return indexSitesIsRunning;
    }

    public boolean hasStoppedByUser() {
        return hasStoppedByUser;
    }

    public void setHasStoppedByUser(boolean hasStoppedByUser) {
        this.hasStoppedByUser = hasStoppedByUser;
    }

    public void setIndexSitesIsRunning(boolean indexSitesIsRunning) {
        this.indexSitesIsRunning = indexSitesIsRunning;
    }

    public void setComplete(boolean complete) {
        isComplete = complete;
    }
}
