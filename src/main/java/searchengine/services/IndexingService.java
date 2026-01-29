package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.exceptions.*;
import searchengine.model.*;
import searchengine.services.indexers.LemmaIndexerForWebsiteScraper;
import searchengine.services.indexers.PageIndexer;
import searchengine.services.indexers.SitesIndexer;
import searchengine.util.FormatterUrl;
import searchengine.util.HtmlTextUtilities;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
public class IndexingService {

    private static final Logger logger = LoggerFactory.getLogger(IndexingService.class);

    private final SitesList sitesList;
    private final SiteCRUDService siteCRUDService;
    private final PageCRUDService pageCRUDService;
    private final IndexEntityCRUDService indexEntityCRUDService;
    private final LemmaCRUDService lemmaCRUDService;

    private SitesIndexer sitesIndexer;

    private final LuceneMorphology morphology;
    private final LemmaFinder lemmaFinder;

    public boolean getIsRunning(){
        return sitesIndexer.indexSitesIsRunning();
    }


    @Autowired
    public IndexingService(SitesList sitesList, SiteCRUDService siteCRUDService, PageCRUDService pageCRUDService,
                           IndexEntityCRUDService indexEntityCRUDService, LemmaCRUDService lemmaCRUDService) {
        this.sitesList = sitesList;
        this.siteCRUDService = siteCRUDService;
        this.pageCRUDService = pageCRUDService;
        this.indexEntityCRUDService = indexEntityCRUDService;
        this.lemmaCRUDService = lemmaCRUDService;
        try {
            this.morphology = new RussianLuceneMorphology();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.lemmaFinder = new LemmaFinder(morphology);

        sitesIndexer = new SitesIndexer(
                siteCRUDService,
                pageCRUDService,
                lemmaCRUDService,
                indexEntityCRUDService
        );
    }



    @Async
    public void indexSites(SitesList sitesList) throws Exception{
        sitesIndexer.indexSites(sitesList);
    }


    public void indexPage(String url) throws IOException {
        PageIndexer pageIndexer = new PageIndexer(siteCRUDService, pageCRUDService, indexEntityCRUDService,
                lemmaCRUDService, lemmaFinder, sitesList);
        pageIndexer.indexPage(url);
    }


    public void indexLemmasForWebsiteScraper(String html, Page pageWithId) {
        LemmaIndexerForWebsiteScraper lemmaIndexerForWebsiteScraper = new LemmaIndexerForWebsiteScraper(lemmaCRUDService, indexEntityCRUDService);
        lemmaIndexerForWebsiteScraper.indexLemmasForWebsiteScraper(html, pageWithId, lemmaFinder);
    }



    public void stopIndexing() {
        try {
            ForkJoinPool poolOfSitesIndexer = sitesIndexer.getPool();
            poolOfSitesIndexer.shutdownNow();
            List<SiteEntity> runningSites = sitesIndexer.getRunningSitesList();
            runningSites.clear();
        } catch (Exception e) {
            throw new RuntimeException("Failed to stop the application", e);
        }
        sitesIndexer.setHasStoppedByUser(true);
        sitesIndexer.setIndexSitesIsRunning(false);
    }


    @PreDestroy
    public void stoppedBeforeCompleting() {
        if (sitesIndexer.isComplete()) {
            return;
        }
        List<SiteEntity> runningSites = sitesIndexer.getRunningSitesList();
        for (SiteEntity site : runningSites) {
            site.setStatus(Status.FAILED);
            site.setLastError("Индексация остановлена");
            siteCRUDService.updateById(site);
        }
        runningSites.clear();
    }

}