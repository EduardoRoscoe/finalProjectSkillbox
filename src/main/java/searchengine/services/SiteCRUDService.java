package searchengine.services;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.entities.PageDTO;
import searchengine.dto.entities.SiteEntityDTO;
import searchengine.exceptions.SiteAlreadyExistsException;
import searchengine.exceptions.SiteNotFoundException;
import searchengine.model.SiteEntity;
import searchengine.model.Status;
import searchengine.repositories.SiteRepository;
import searchengine.util.FormatterUrl;

import java.time.LocalDateTime;
import java.util.*;

@Service
@AllArgsConstructor
public class SiteCRUDService implements CRUDService<SiteEntity> {

    private final SiteRepository siteRepository;
    private static final Logger logger = LoggerFactory.getLogger(SiteCRUDService.class);


    @Override
    public void create(SiteEntity siteEntity) {
        logger.debug("создание siteEntity: " + siteEntity.getUrl());
        String url = siteEntity.getUrl();
        String formattedSiteUrl = FormatterUrl.verifyAndFormatUrl(url);
        logger.debug("formattedSiteURl : {}", formattedSiteUrl);
        if (!siteExistsByFormattedUrl(formattedSiteUrl)) {
            siteRepository.save(siteEntity);
            logger.debug("Сайт с formattedUrl '{}' был создан с помощью метода create.", formattedSiteUrl);
        } else {
            throw new SiteAlreadyExistsException("сайт с '" + formattedSiteUrl + "' уже существует.");
        }
    }

    public void createSitesWithIndexingStatus(SitesList sitesList) {
        List<Site> listOfSites = sitesList.getSites();
        for (Site site : listOfSites) {
            SiteEntity siteEntity = mapSiteConfigToSiteEntity(site);
            siteEntity.setStatus(Status.INDEXING);
            create(siteEntity);
        }
        logger.debug("Все сайты из sitesList были созданы.");
    }

    @Override
    public SiteEntity getById(Integer id) {
        SiteEntity foundSite = siteRepository.findById(id).orElseThrow(() -> new SiteNotFoundException(id));
        logger.debug("Сайт '" + foundSite.getUrl() + "' c id '" + id +
                "' был извлечен из базы данных с помощью метода getById");
        return foundSite;
    }

    @Override
    public List<SiteEntity> getAll() {
        List<SiteEntity> foundSiteEntityList = Optional.ofNullable(siteRepository.findAll())
                .orElseThrow(() -> new SiteNotFoundException("сайты не найдены"));
        logger.debug("Все сайты были извлечены из базы данных с помощью метода getAll");
        return foundSiteEntityList;
    }

    public SiteEntity getSiteFromSiteConfigOrCreate(Site siteConfig) {
        String configURL = siteConfig.getUrl();
        SiteEntity siteEntity;
        if (!siteExistsByNotFormattedUrl(configURL)) {
            SiteEntity siteEntityNotInDB = mapSiteConfigToSiteEntity(siteConfig);
            siteEntityNotInDB.setStatus(Status.FAILED);
            create(siteEntityNotInDB);
            siteEntity = getByURL(configURL);
        } else {
            siteEntity = getByURL(configURL);
        }
        return siteEntity;
    }

    public int getIdByURL(String url) {
        String formattedUrl = FormatterUrl.verifyAndFormatUrl(url);
        int foundId = getByURL(formattedUrl ).getId();
        logger.debug("ID '{}' из URL {} был извлечен из базы данных с помощью метода getIdByURL", foundId, url);
        return foundId;
    }

    public SiteEntity getByURL (String url) {
        String formattedUrl = FormatterUrl.verifyAndFormatUrl(url);
        SiteEntity foundSite = siteRepository.findByUrl(formattedUrl).
                orElseThrow(() -> new SiteNotFoundException("Сайт с url '" + formattedUrl + "' не найден."));
        logger.debug("Сайт c URL {} был извлечен из базы данных с помощью метода getByURL", url);
        return foundSite;
    }

    @Override
    public void updateById(SiteEntity siteEntity) {
        int id = siteEntity.getId();
        if (siteExistsById(id)) {
            siteRepository.save(siteEntity);
            logger.debug("Сайт '{}' c id {} был обновлен с помощью метода update", siteEntity.getUrl(), id);
        } else {
            throw new SiteNotFoundException(id);
        }
    }

    @Override
    public void delete(Integer id) {
        if (siteExistsById(id)) {
            siteRepository.deleteById(id);
            logger.debug("Сайт с id '{}' был удален.", id);
        } else {
            throw new SiteNotFoundException("Не удалось удалить сайт c id '" + id + "' потому что он не существует");
        }
    }
    public void deleteExistingSitesOfSitesList(SitesList sitesList) {
        List<Site> listOfSites = sitesList.getSites();
        int id;
        for (Site site : listOfSites) {
            String siteUrl = site.getUrl();
            if (siteExistsByNotFormattedUrl(siteUrl)) {
                id = getIdByURL(siteUrl);
                delete(id);
            }
        }
    }

    public void deleteSitesOfSitesList(SitesList sitesList) {
        List<Site> listOfSites = sitesList.getSites();
        for (Site site : listOfSites) {
            String siteUrl = site.getUrl();
            try {
                int id = getIdByURL(siteUrl);
                delete(id);
            } catch (SiteNotFoundException e) {
                throw new SiteNotFoundException("Не удалось удалить сайт " + siteUrl + " потому что он не существует");
            }
        }
        logger.debug("Все сайты из sitesList в application.yaml были удалены");
    }

    public boolean siteExistsById(int id) {
        return siteRepository.existsById(id);
    }

    public boolean siteExistsByNotFormattedUrl(String notFormattedUrl) {
        String formattedUrl = FormatterUrl.verifyAndFormatUrl(notFormattedUrl);
        return siteRepository.existsByUrl(formattedUrl);
    }

    public boolean siteExistsByFormattedUrl(String formattedUrl) {
        return siteRepository.existsByUrl(formattedUrl);
    }

    public SiteEntity getByName (String name) {
        try {
             SiteEntity site = siteRepository.findByName(name).
                     orElseThrow(() -> new SiteNotFoundException("Сайт с name '" + name + "' не найден."));
             logger.debug("Сайт c именем {} был извлечен из базы данных с помощью метода getByName", name);
             return site;
        } catch (IncorrectResultSizeDataAccessException e) {
            throw new IllegalStateException("найдено несколько результатов для имени сайта: '" + name + "'", e);
        }
    }
    public SiteEntity mapSiteConfigToSiteEntity(Site site) {
        SiteEntity siteEntity = new SiteEntity();
        siteEntity.setName(site.getName());
        siteEntity.setUrl(site.getUrl());
        siteEntity.setStatusTime(LocalDateTime.now());
        return siteEntity;
    }

    public SiteEntityDTO mapToSiteEntityDTO(SiteEntity siteEntity) {
        SiteEntityDTO siteEntityDTO = new SiteEntityDTO();
        siteEntityDTO.setId(siteEntity.getId());
        siteEntityDTO.setStatus(siteEntity.getStatus());
        siteEntityDTO.setName(siteEntity.getName());
        siteEntityDTO.setUrl(siteEntity.getUrl());
        siteEntityDTO.setLastError(siteEntity.getLastError());
        siteEntityDTO.setStatusTime(siteEntity.getStatusTime());
        if (!(siteEntity.getPages() == null)) {
            List<PageDTO> pageDTOList = siteEntity.getPages()
                    .stream()
                    .map(PageCRUDService::mapToPageDTO)
                    .toList();
            siteEntityDTO.setPages(pageDTOList);
        }
        return siteEntityDTO;
    }
 }
