package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.exceptions.SiteNotFoundException;
import searchengine.model.SiteEntity;
import searchengine.model.Status;

import java.net.MalformedURLException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatisticsServiceImpl implements StatisticsService {
    private final PageCRUDService pageCRUDService;

    private final LemmaCRUDService lemmaCRUDService;

    private final SiteCRUDService siteCRUDService;

    private final Random random = new Random();
    private final SitesList sites;

    Logger logger = LoggerFactory.getLogger(StatisticsServiceImpl.class);

    @Override
    public StatisticsResponse getStatistics() {
        String[] statuses = { "INDEXED", "FAILED", "INDEXING" };
        String[] errors = {
                "Ошибка индексации: главная страница сайта не доступна",
                "Ошибка индексации: сайт не доступен",
                ""
        };

        TotalStatistics total = new TotalStatistics();
        total.setSites(sites.getSites().size());
        total.setIndexing(true);

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<Site> sitesList = sites.getSites();

        Instant start = Instant.now();
        for(int i = 0; i < sitesList.size(); i++) {
            Site site = sitesList.get(i);
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(site.getName());
            item.setUrl(site.getUrl());
//            int pages = random.nextInt(1_000);
//            int lemmas = pages * random.nextInt(1_000);

            SiteEntity siteEntityDefault = new SiteEntity();
            siteEntityDefault.setUrl(site.getUrl());
            siteEntityDefault.setName(site.getName());
            siteEntityDefault.setLastError("No error");
            siteEntityDefault.setStatusTime(LocalDateTime.now());
            siteEntityDefault.setStatus(Status.WAITING_TO_BE_INDEXED);
            siteEntityDefault.setId(-1);
//            item.setStatus(statuses[i % 3]);
            SiteEntity siteEntity = new SiteEntity();
            try {
                siteEntity = siteCRUDService.getByURL(site.getUrl());
                logger.info(siteEntity.getUrl());
            } catch (SiteNotFoundException e) {
                siteEntity = null;
            }
            Instant end = Instant.now();
            long durationOfloop = Duration.between(start, end).toMillis();
            logger.info("Duration of loop for site '{}': {} ms", site.getUrl(), durationOfloop);
            if (!(siteEntity == null)) {
                int siteEntityId = siteEntity.getId();
                item = siteEntityToDetailedStatisticsItem(siteEntity);
                int lemmasAmount = item.getLemmas();
                logger.info("Lemmas amount of '{}' is: {}", siteEntity.getUrl(), lemmasAmount);
                int numberPagesSite = item.getPages();
                total.setLemmas(total.getLemmas() + lemmasAmount);
                total.setPages(total.getPages() + numberPagesSite);
            } else {
                item = siteEntityToDetailedStatisticsItem(siteEntityDefault);
            }
//            item.setStatus(siteEntityOptional.orElse(siteEntityDefault).getStatus().name());
//            item.setError(siteEntityOptional.orElse(siteEntityDefault).getLastError());
//            item.setStatusTime(siteEntityOptional.orElse(siteEntityDefault).getStatusTime().toString());
//            int pages = pageCRUDService.getPagesBySiteId(siteEntityOptional.orElse(siteEntityDefault).getId()).size();
//            int lemmas = lemmaCRUDService.getLemmasBySiteId(siteEntityOptional.orElse(siteEntityDefault).getId()).size();

            detailed.add(item);
        }
        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }

    public DetailedStatisticsItem siteEntityToDetailedStatisticsItem(SiteEntity siteEntity) {
        DetailedStatisticsItem item = new DetailedStatisticsItem();
        int siteEntityId = siteEntity.getId();
        item.setError(siteEntity.getLastError());
        item.setStatus(siteEntity.getStatus().name());
        item.setName(siteEntity.getName());
        item.setUrl(siteEntity.getUrl());
        item.setStatusTime(siteEntity.getStatusTime().toString());
        item.setPages(pageCRUDService.countPagesBySiteId(siteEntityId));
        item.setLemmas(lemmaCRUDService.countLemmasBySiteId(siteEntityId));
        return item;
    }
}
