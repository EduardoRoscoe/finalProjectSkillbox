package searchengine.services.indexers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import searchengine.model.IndexEntity;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.SiteEntity;
import searchengine.services.IndexEntityCRUDService;
import searchengine.services.LemmaCRUDService;
import searchengine.services.LemmaFinder;
import searchengine.util.HtmlTextUtilities;

import java.util.Map;

public class LemmaIndexerForWebsiteScraper {
    private static Logger logger = LoggerFactory.getLogger(LemmaIndexerForWebsiteScraper.class);

    private final LemmaCRUDService lemmaCRUDService;
    private final IndexEntityCRUDService indexEntityCRUDService;

    public LemmaIndexerForWebsiteScraper(LemmaCRUDService lemmaCRUDService, IndexEntityCRUDService indexEntityCRUDService) {
        this.lemmaCRUDService = lemmaCRUDService;
        this.indexEntityCRUDService = indexEntityCRUDService;
    }

    public void indexLemmasForWebsiteScraper(String html, Page pageWithId, LemmaFinder lemmaFinder) {
        String cleanedText = HtmlTextUtilities.cleanHtml(html);
        Map<String, Integer> lemmas = lemmaFinder.collectLemmas(cleanedText);
        processLemmasForWebsiteScraper(lemmas, pageWithId);
        logger.info("Леммы со страницы '{}' были проиндексированы.", pageWithId.getSite().getUrl() + pageWithId.getPath());;
    }

    private void processLemmasForWebsiteScraper(Map<String, Integer> lemmas, Page pageWithId) {
        SiteEntity siteEntity = pageWithId.getSite();
        for (Map.Entry<String, Integer> entry : lemmas.entrySet()) {
            Lemma lemma = createAndGetLemma(entry.getKey(), siteEntity);
            synchronized (indexEntityCRUDService) {
                indexEntityCRUDService.createOrUpdateByPageLemmaAndRank(pageWithId, lemma, entry.getValue());
            }
        }
    }

    private Lemma createAndGetLemma(String lemmaText, SiteEntity siteEntity) {
        Lemma lemma = new Lemma();
        lemma.setLemma(lemmaText);
        lemma.setSite(siteEntity);
        lemma.setFrequency(0);

        synchronized (lemmaCRUDService) {
            lemmaCRUDService.createWithoutThrowingError(lemma);
        }
        return lemmaCRUDService.getLemmaByLemmaAndSite(lemmaText, siteEntity);
    }
}
