package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import searchengine.config.SitesList;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.exceptions.InvalidUrlFormatException;
import searchengine.exceptions.LemmaNotFoundException;
import searchengine.exceptions.PageNotFoundException;
import searchengine.exceptions.SiteNotFoundException;
import searchengine.services.*;
import searchengine.model.Page;
import searchengine.util.FormatterUrl;
import searchengine.util.HtmlTextUtilities;
import searchengine.util.ResponseHandler;
import searchengine.util.Verifier;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class ApiController {

    private final StatisticsService statisticsService;

    private final IndexingService indexingService;

    private final PageCRUDService pageCRUDService;

    private final SiteCRUDService siteCRUDService;

    private final LemmaCRUDService lemmaCRUDService;

    private final SearchService searchService;
    private final SitesList sitesList;

    private final IndexEntityCRUDService indexEntityCRUDService;

    private final Logger logger = LoggerFactory.getLogger(ApiController.class);


    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
//        Lemma lemma = new Lemma();
//        lemma.setId(308924);
//        lemma.setLemma("состоять");
//        SiteEntity site = siteCRUDService.getById(307543);
//        lemma.setSite(site);
//        lemmaCRUDService.createOrUpdateAndIncreaseFrequencyByOne(lemma);
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<Object> startIndexing() throws Exception {
        Map<String, Object> response = new HashMap<>();
        if (!indexingService.getIsRunning()) {
            try {
                indexingService.indexSites(sitesList); // executes asynchronously
                response.put("result", true);
                return ResponseEntity.ok(response);
            } catch (Exception e) {
                throw e;
            }
        } else {
            response.put("result", false);
            response.put("error", "Индексация уже запущена");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
//            return ResponseEntity.ok(response);
        }
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<Object> stopIndexing() {
        Map<String, Object> response = new HashMap<>();
        if (indexingService.getIsRunning()) {
            indexingService.stopIndexing();
            response.put("result", true);
            return ResponseEntity.ok(response);
        } else {
            response.put("result", false);
            response.put("error", "Индексация не запущена");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/indexPage")
    public ResponseEntity<Object> indexPage(@RequestBody String url) throws IOException {
        System.out.println(url);
        Map<String, Object> response = new HashMap<>();
        if (Verifier.siteIsValidFormat(url)) {
            url = FormatterUrl.formatStringIntoURL(url);
        } else {
            url = URLDecoder.decode(url, StandardCharsets.UTF_8);
            return ResponseHandler.invalidSiteFormat(url);
        }
        Instant beforeIndexPage = Instant.now();
        indexingService.indexPage(url);
        Instant afterIndexPage = Instant.now();
        Duration executionTime = Duration.between(beforeIndexPage, afterIndexPage);
        logger.info("Execution Time Of indexPage: {}", executionTime.toMillis());
        response.put("result", true);
        return ResponseEntity.ok(response);
    }
    @GetMapping("/search")
    public ResponseEntity<Object> search(
                @RequestParam String query,
                @RequestParam(required = false) String offset,
                @RequestParam(required = false) String limit,
                @RequestParam(required = false) String site,
                Model model) throws IOException
    {
        Instant start = Instant.now();
        LinkedHashMap<Page, Float> pageRankMap;
        LinkedHashMap<String, Object> response = new LinkedHashMap<>();
        List<Map<String, String>> responseData = new ArrayList<>();
        int limitNumber = 0;
        int offsetNumber = 0;
        int parsedLimit = 0;
        int resultsPerPage = 10;

        if (query.isEmpty()) {
            response.put("result", false);
            response.put("error", "Задан пустой поисковый запрос");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        if (!(site == null) && !site.isEmpty()) {
            try {
                siteCRUDService.getByURL(site);
            } catch (SiteNotFoundException e) {
                return ResponseHandler.siteNotFound(e.getMessage());
            } catch (InvalidUrlFormatException e) {
                return ResponseHandler.invalidSiteFormat(site);
            }
        }
        if (limit.isEmpty()) {
            limit = "10";
        }

        try {
            pageRankMap = searchService.findPagesWithQuery(query, site, sitesList);
        } catch (PageNotFoundException | LemmaNotFoundException e) {
            response.put("result", false);
            response.put("error", "результат не найден для query: '" + query + "'");
            return new ResponseEntity<>( response, HttpStatus.NOT_FOUND);
        }

        Collection<Page> pageList = pageRankMap.keySet();

        if (pageList.isEmpty()) {
            response.put("result", false);
            response.put("error", "результат не найден для query: '" + query + "'");
            response.put("count", "");
            response.put("data", "");
            return new ResponseEntity<>( response, HttpStatus.NOT_FOUND);
        }

        if (limit.isEmpty() || limit == null) {
            limitNumber = 20;
        } else {
            try {
                parsedLimit = Integer.parseInt(limit);
                if (parsedLimit == 0) {
                    limitNumber = pageList.size();
                } else if (parsedLimit < 0) {
                    throw new NumberFormatException();
                } else {
                    limitNumber = Math.min(parsedLimit, pageList.size());
                }
            } catch (NumberFormatException e) {
                return ResponseHandler.stringIsNotValidPositiveNumber("limit", limit);
            }
        }

        if (offset.isEmpty() || offset == null) {
            offsetNumber = 0;
        } else {
            try {
                offsetNumber = Integer.parseInt(offset);
                if (offsetNumber < 0) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                return ResponseHandler.stringIsNotValidPositiveNumber("offset", offset);
            }
        }


//        if (limitNumber < offsetNumber) {
//            response.put("result", false);
//            response.put("error",
//                    "Offset слишком велико для итоговой суммы. Максимальное число offset: " + limitNumber / 10);
//            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
//        }

        // calculating how much it should show
        int i = 1;
        int totalResult = pageList.size();
        int futureResults = Math.min(totalResult, offsetNumber + resultsPerPage);

        logger.info("offset number: {}", offsetNumber);
        logger.info("limit number: {}", limitNumber);
        logger.info("pageList size: {}", pageList.size());
        for (Page page : pageList) {
//            logger.info("'i' inside loop: {}", i);
            if (i > offsetNumber && i <= futureResults) {
                logger.info("'i' inside loop condition: {}", i);
                LinkedHashMap<String, String> pageData = new LinkedHashMap<>();
                String pageSiteUrl = page.getSite().getUrl();
                 logger.info("page site url inside condition loop: {}", page.getPath());
                String pageSiteUrlWithoutSlash = pageSiteUrl.substring(0, pageSiteUrl.length() - 1);
                pageData.put("site", pageSiteUrlWithoutSlash);
                pageData.put("siteName", page.getSite().getName());
                if (!pageSiteUrl.equals(page.getPath())) {
                    pageData.put("uri", page.getPath());
                } else {
                    pageData.put("uri", "");
                }
                String htmlCodePage = page.getContent();
                String title = HtmlTextUtilities.extractTitle(htmlCodePage);
                pageData.put("title", title);
    //                String snippet = "";
                String snippet = HtmlTextUtilities.extractSnippetContainingWord(htmlCodePage, query);
//                if (snippet.isEmpty()) { // don't show the page if the snippet is empty
//                    i++;
//                    continue;
//                }
                pageData.put("snippet", snippet);
                Float relevance = pageRankMap.get(page);
                pageData.put("relevance", relevance.toString());
                responseData.add(pageData);
            }
            if (i == limitNumber) {
                break;
            }
            i++;
        }
        logger.info("Response data size: {}", responseData.size());

        response.put("result", true);

        response.put("count", limitNumber);

        response.put("data", responseData);


        Instant end = Instant.now();
        Duration executionTime = Duration.between(start, end);
        logger.info("Execution Time Of whole search: {}", executionTime.toMillis());
        return ResponseEntity.ok(response);
    }

}
