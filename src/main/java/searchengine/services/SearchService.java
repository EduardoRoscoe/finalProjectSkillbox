package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import searchengine.Application;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.exceptions.LemmaNotFoundException;
import searchengine.exceptions.PageNotFoundException;
import searchengine.model.IndexEntity;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.SiteEntity;
import searchengine.util.SortMap;
import searchengine.util.Verifier;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SearchService {
    private String query;

    private  LuceneMorphology luceneMorphology;

    private final LemmaCRUDService lemmaCRUDService;

    private final IndexEntityCRUDService indexEntityCRUDService;

    private final PageCRUDService pageCRUDService;

    private final SiteCRUDService siteCRUDService;
    private final Logger logger = LoggerFactory.getLogger(Application.class);




    public LinkedHashMap<Page, Float> findPagesWithQuery(String query, String websiteURL, SitesList sitesList) {
        List<Lemma> overalllemmaList = new ArrayList<>();
        List<List<Lemma>> lemmaListsBySite = new ArrayList<>();
        LinkedHashMap<Page, Float> pageLemmaRankMap = new LinkedHashMap<>();
        int siteId = 0;
        SiteEntity site = new SiteEntity();
        List<Page> pagesOfLemma = new ArrayList<>();
        logger.info("Website URL for Searching the page: " + websiteURL);
        logger.debug("Verifier URL: " + Verifier.siteIsValidFormat(websiteURL));
        if (Verifier.siteIsValidFormat(websiteURL)) {
            site = siteCRUDService.getByURL(websiteURL);
//            siteId = site.orElseThrow(() -> new ResponseStatusException(
//                            HttpStatus.NOT_FOUND, "site \"" + websiteURL + "\" not found"))
            siteId = site.getId();
        }
        boolean userSelectedAllSites = (site.getUrl() == null);

        try {
            luceneMorphology = new RussianLuceneMorphology();
            LemmaFinder lemmaFinder = new LemmaFinder(luceneMorphology);
            Set<String> queryLemmaSet = lemmaFinder.getLemmaSet(query);

            int i = 1;
//            for (String lemmaString : queryLemmaSet) {
//                logger.info("Lemma: {} {}", i, lemmaString);
//                logger.info("url of site {}", site.getUrl());
//                if (!userSelectedAllSites) {
//                    Lemma lemma = lemmaCRUDService.getLemmaByLemmaAndSite(lemmaString, site);
//                    int lemmaFrequency = lemma.getFrequency();
//                    if (lemmaFrequency > 500000) {
//                        queryLemmaSet.remove(lemmaString);
//                    } else {
//                        lemmaList.add(lemma);
//                    }
//                } else {
//                    logger.info("is inside the else part of for loop lemmaString queryLemmaSet");
//                    int errorCount = 0;
//                    for (Site listSite : sitesList.getSites()) {
//                        logger.info("Sites that we are searching for the {} time: {}", i, listSite.getUrl());
//                        site = siteCRUDService.getByURL(listSite.getUrl());
//                        try {
//                            Lemma lemma = lemmaCRUDService.getLemmaByLemmaAndSite(lemmaString, site);
//                            int lemmaFrequency = lemma.getFrequency();
//                            if (lemmaFrequency > 500000) {
//                                queryLemmaSet.remove(lemmaString);
//                            } else {
//                                lemmaList.add(lemma);
//                            }
//                        } catch (LemmaNotFoundException e) {
//                            errorCount++;
//                            if (errorCount == sitesList.getSites().size()) {
//                                logger.info("Lemma error is here, and sitesList size is: {}", sitesList.getSites().size());
//                                throw e;
//                            }
//                        }
//
//                    }
//                }
//                i++;
//            }
            if (!userSelectedAllSites) {
                for (String lemmaString : queryLemmaSet) {
                    logger.info("Lemma: {} {}", i, lemmaString);
                    logger.info("url of site {}", site.getUrl());
                    try {
                        Lemma lemma = lemmaCRUDService.getLemmaByLemmaAndSite(lemmaString, site);
                        int lemmaFrequency = lemma.getFrequency();
                        if (lemmaFrequency > 500000) {
                            queryLemmaSet.remove(lemmaString);
                        } else {
                            overalllemmaList.add(lemma);
                        }
                    } catch (LemmaNotFoundException e) {
                        throw new PageNotFoundException("Pages not found for query '" + query + "'");
                    }
                }
                lemmaListsBySite.add(overalllemmaList);
            } else {
                int overallErrorCount = 0;
                int maximumErrorsAllowed = sitesList.getSites().size();
//                for (Site siteFromSitesList : sitesList.getSites()) {
//                    List<Lemma> lemmaList = new ArrayList<>();
//                    for (String lemmaString : queryLemmaSet) {
//                        logger.info("Sites that we are searching for the {} time: {}", i, siteFromSitesList.getUrl());
//                        site = siteCRUDService.getByURL(siteFromSitesList.getUrl());
//                        try {
//                            Lemma lemma = lemmaCRUDService.getLemmaByLemmaAndSite(lemmaString, site);
//                            int lemmaFrequency = lemma.getFrequency();
//                            if (lemmaFrequency > 500000) {
//                                queryLemmaSet.remove(lemmaString);
//                            } else {
//                                overalllemmaList.add(lemma);
//                            }
//                        } catch (LemmaNotFoundException e) {
//                            overallErrorCount++;
//                            if (overallErrorCount == maximumErrorsAllowed) { // if every website couldn't find any lemma
//                                throw new PageNotFoundException("No page has been found for any given website");
//                            }
//                            lemmaList.clear(); // removes the site from the search, because it doesn't have one lemma from the query
//                            break;
//                        }
//                    }
//                    overalllemmaList.addAll(lemmaList);
//                    lemmaListsBySite.add(lemmaList);
//                    logger.info("Lemma listBySite size: {}", lemmaListsBySite.size());
//                    i++;
//                }
                for (Site siteFromSitesList : sitesList.getSites()) {
                    List<Lemma> lemmaList = processSite(siteFromSitesList, queryLemmaSet);

                    if (lemmaList == null && overallErrorCount == maximumErrorsAllowed) {
                        throw new PageNotFoundException("No page has been found for any given website");
                    } else if (lemmaList == null){
                        overallErrorCount++;
                        continue; // Skip to the next site
                    }

                    overalllemmaList.addAll(lemmaList);
                    lemmaListsBySite.add(lemmaList);
                }
            }

            overalllemmaList.sort(Comparator.comparingInt(Lemma::getFrequency));
            overalllemmaList.forEach(lemma -> System.out.println("Frequency of lemma '" + lemma.getLemma() + "' is: " + lemma.getFrequency()));

//            List<Exception> exceptionList = new ArrayList<>();
//            logger.info("Time before loop in searchService {} ", System.currentTimeMillis());
//            for (List<Lemma> lemmaListOfSite : lemmaListsBySite) {
//                List<Page> pagesOfLemmaSite = new ArrayList<>();
//                int count = 0;
//                for (Lemma lemma : lemmaListOfSite) {
//                    count++;
//                    int lemmaId = lemma.getId();
//                    if (count == 1) {
//                        pagesOfLemmaSite = pageCRUDService.findPagesWithLemmaId(lemmaId);
//                    } else {
//                        pagesOfLemmaSite = pageCRUDService.findPagesWithLemmaIdInPageList(lemmaId, pagesOfLemmaSite);
//                    }
////                    pagesOfLemma.forEach(page -> logger.info("page id of lemma '{}' is: {}", lemma.getLemma(), page.getId()));
//                    pagesOfLemma.addAll(pagesOfLemmaSite);
//                }
//            }
//            if (exceptionList.size() == overalllemmaList.size()) {
//                throw new PageNotFoundException("No pages found from query");
//            }
            // alternative version with direct search on DB of pages that contain all the lemmas from the query
            for (List<Lemma> lemmaListOfSite : lemmaListsBySite) {
                List<Page> pagesOfLemmaSite = new ArrayList<>();
                if (!lemmaListOfSite.isEmpty()) {
                    int siteIdOfPage = lemmaListOfSite.get(0).getSite().getId();
                    try {
                        pagesOfLemmaSite = pageCRUDService.findPagesOfAWebsiteContainingLemmas(lemmaListOfSite, siteIdOfPage);
                    } catch (PageNotFoundException e) {
                        throw new PageNotFoundException("No pages found for query");
                    }
                }
                pagesOfLemma.addAll(pagesOfLemmaSite);
            }


            float highestAbsoluteRank = 0;
            for (Page page : pagesOfLemma) {
                float absoluteRank = 0;
                for (Lemma lemma : overalllemmaList) {
//                    logger.info("Page url: " + page.getPath());
                    IndexEntity indexEntity = new IndexEntity();
                    try {
                        indexEntity = indexEntityCRUDService.findByPageAndLemma(page, lemma);
                    } catch (Exception e) {
                        continue;
                    }
                    float relevant = indexEntity.getRank();
//                    System.out.println("Page: " + page.getPath() + "  Lemma: " + lemma.getLemma() + "  Rank: " + relevant);
                    absoluteRank = absoluteRank + relevant;
                }
                if (highestAbsoluteRank < absoluteRank) {
                    highestAbsoluteRank = absoluteRank;
                }
                float relativeRank = absoluteRank/highestAbsoluteRank;
                pageLemmaRankMap.put(page, relativeRank);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        pageLemmaRankMap = SortMap.sortPageFloatLinkedHashMapByValueDesc(pageLemmaRankMap);
//        logger.info("Time after completion searchService {} ", System.currentTimeMillis());
        return pageLemmaRankMap;
    }

    private List<Lemma> processSite(Site siteFromSitesList, Set<String> queryLemmaSet) {
        List<Lemma> lemmaList = new ArrayList<>();

        for (String lemmaString : new HashSet<>(queryLemmaSet)) { // queryLemmaSet copy to avoid ConcurrentModificationException
            logger.info("Searching site: {}", siteFromSitesList.getUrl());
            SiteEntity site = siteCRUDService.getByURL(siteFromSitesList.getUrl());

            try {
                Lemma lemma = lemmaCRUDService.getLemmaByLemmaAndSite(lemmaString, site);
                int lemmaFrequency = lemma.getFrequency();

                if (lemmaFrequency > 500000) {
                    queryLemmaSet.remove(lemmaString);
                } else {
                    lemmaList.add(lemma);
                }
            } catch (LemmaNotFoundException e) {
                // Return null to indicate failure in finding any lemma for this site
                return null;
            }
        }

        return lemmaList; // Return the populated lemma list
    }

//    public void getLemmaOfQueryWordAndAddToList(String lemmaString, List<Lemma> lemmaList, Set<String> queryLemmaSet, SiteEntity site) {
//        Lemma lemmaOfQuery = lemmaCRUDService.getLemmaByLemmaAndSite(lemmaString, site);
//        int lemmaFrequency = lemmaOfQuery.getFrequency();
//        if (lemmaFrequency > 5000000) { // if lemma is too often don't use it in the search
//            queryLemmaSet.remove(lemmaString);
//        } else {
//            lemmaList.add(lemmaOfQuery);
//        }
//    }
//
//    public void getAllLemmasOfQueryAndAddToList(Set<String> queryLemmaSet, Site siteFromList, List<Lemma> lemmaList, int maximumErrorsAllowed) {
//        SiteEntity siteEntity = new SiteEntity();
//        int overallErrorCount = 0;
//        for (String lemmaString : queryLemmaSet) {
//            logger.info("Sites that we are searching for the {} time: {}", i, siteFromList.getUrl());
//            siteEntity = siteCRUDService.getByURL(siteFromList.getUrl());
//            try {
//                getLemmaOfQueryWordAndAddToList(lemmaString, lemmaList, queryLemmaSet, siteEntity);
//            } catch (LemmaNotFoundException e) {
//                overallErrorCount++;
//                if (overallErrorCount == maximumErrorsAllowed) { // if every website couldn't find any lemma
//                    throw new PageNotFoundException("No page has been found for any given website");
//                }
//                lemmaList.clear(); // removes the site from the search, because it doesn't have one lemma from the query
//                break;
//            }
//        }
//    }
}
