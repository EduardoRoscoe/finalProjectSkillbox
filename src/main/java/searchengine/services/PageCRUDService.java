package searchengine.services;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import searchengine.dto.entities.IndexEntityDTO;
import searchengine.dto.entities.PageDTO;
import searchengine.exceptions.PageAlreadyExistsException;
import searchengine.exceptions.PageNotFoundException;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.SiteEntity;
import searchengine.repositories.PageRepository;


import java.util.*;

@Service
@AllArgsConstructor
public class PageCRUDService implements CRUDService<Page> {

    private final PageRepository pageRepository;

    private final LemmaCRUDService lemmaCRUDService;

    private static final Logger logger = LoggerFactory.getLogger(PageCRUDService.class);


    @Override
    public void create(Page page) {
        String path = page.getPath();
        String siteUrl = page.getSite().getUrl();
        if (!existsByPathAndSite(path, page.getSite())) {
            pageRepository.save(page);
            logger.debug("Страница '{}' была создана с помощью метода create.", path);
        } else {
            throw new PageAlreadyExistsException("страница с '" + path + "' и с сайт '" + siteUrl + "' уже существует.");
        }
    }

    public void createOrUpdate(Page page) {
        String pagePath = page.getPath();
        SiteEntity pageSite = page.getSite();

        if (!existsByPathAndSite(pagePath, pageSite)) {
            pageRepository.save(page);
            logger.debug("Страница '{}' была создана с помощью метода createOrUpdate.", pagePath);
        } else {
            int id = getPageIdByPathAndSite(pagePath, pageSite);
            page.setId(id);
            pageRepository.save(page);
            logger.debug("Страница '{}' была обновлена с помощью метода createOrUpdate.", pagePath);
        }
    }

    public void createTableFromPagesSet(Set<Page> pageList) {
        for (Page page : pageList) {
            create(page);
        }
    }

    @Override
    public Page getById(Integer id) {
        Page foundPage = pageRepository.findById(id)
                .orElseThrow(() -> new PageNotFoundException(id));
        logger.debug("Страница с id '{}' была найдена с помощью метода getById", id);
        return foundPage;
    }

    public Page getByPathAndSite(String path, SiteEntity siteEntity) {
        String siteUrl = siteEntity.getUrl();
        Page foundPage = pageRepository.findByPathAndSite(path, siteEntity)
                .orElseThrow(() -> new PageNotFoundException("Page с path '" + path + "' и site '" +
                        siteUrl + "' не найдена."));
        logger.debug("Страница с path '{}' и c сайт '{}' был извлечен из базы данных с помощью метода getByPathAndSite", path, siteUrl);
        return foundPage;
    }

    public int getPageIdByPathAndSite (String path, SiteEntity siteEntity) {
        int foundPageId = getByPathAndSite(path, siteEntity).getId();
        logger.debug("Id из страницы с path '{}' и c сайт '{}' был извлечен из базы данных" +
                " с помощью метода getByPathAndSite", path, siteEntity.getUrl());
        return foundPageId;
    }

    public List<Page> getPagesBySiteId(int siteId) {
        List<Page> foundPages = pageRepository.findAllPagesBySiteId(siteId)
                .orElseThrow(() -> new PageNotFoundException("не найдено страниц с websiteId '" + siteId));
        logger.debug("страницы с websiteId '{}' найдены", siteId);
        return foundPages;
    }

    public Page getByPathAndSiteId(String path, int siteId) {
        return pageRepository.findByPathAndSiteId(path, siteId).orElseThrow(() -> new PageNotFoundException("Not found"));
    }

    public int countPagesBySiteId(int siteId) {
        int count = pageRepository.countPagesBySiteId(siteId)
                .orElseThrow(() -> new PageNotFoundException("не найдено страниц с websiteId '" + siteId));
        logger.debug("количество страниц с websiteId '{}' найдено", siteId);
        return count;
    }
    public List<Page> findPagesWithLemmaId(int lemmaId) {
        return pageRepository.findPagesByLemmaId(lemmaId)
                .orElseThrow(() -> new PageNotFoundException("не найдено страниц c lemmaId '" + lemmaId + "'"));
    }

    public List<Page> findPagesWithLemmaIdInPageList(int lemmaId, List<Page> pageList) {
        return pageRepository.findPagesByLemmaIdAndPageInPageList(lemmaId, pageList)
                .orElseThrow(() -> new PageNotFoundException("не найдено страниц c lemmaId '" + lemmaId + "' из PageList"));
    }

    public List<Page> findPagesOfAWebsiteContainingLemmas(List<Lemma> lemmaList, int websiteId) {
        List<Integer> lemmaIDlist = new ArrayList<>();
        for (Lemma lemma : lemmaList) {
            lemmaIDlist.add(lemma.getId());
        }
        List<Page> foundPages = pageRepository.findPagesOfAWebsiteContainingAllLemmas(websiteId ,lemmaIDlist, lemmaList.size())
                .orElseThrow(() -> new PageNotFoundException("не найдено страниц с websiteId '" + websiteId +
                        "' и lemmaIdlist: " + lemmaList));
        logger.debug("страницы с websiteId '{}' и lemmasIds '{}' найдены", websiteId, lemmaIDlist);
        return foundPages;
    }

    public List<Page> findPagesOfAllWebsitesContainingLemmas(List<Lemma> lemmaList) {
        List<Integer> lemmaIDlist = new ArrayList<>();
        for (Lemma lemma : lemmaList) {
            lemmaIDlist.add(lemma.getId());
        }
        List<Page> foundPages =  pageRepository.findPagesOfAllSitesContainingAllLemmas(lemmaIDlist, lemmaList.size())
                .orElseThrow(() -> new PageNotFoundException("не найдено страниц с lemmaId: " + lemmaIDlist));
        logger.debug("страницы с lemmasIds '{}' найдены", lemmaIDlist);
        return foundPages;
    }

    @Override
    public Collection<Page> getAll() {
        Collection<Page> foundPageCollection = Optional.ofNullable(pageRepository.findAll())
                .orElseThrow(() -> new PageNotFoundException("нет страниц в базе данных"));
        logger.debug("Все страницы были извлечены из базы данных с помощью метода getAll");
        return foundPageCollection;
    }

    @Override
    public void updateById(Page page) {
        int pageId = page.getId();
        if (pageRepository.existsById(page.getId())) {
            pageRepository.save(page);
            logger.debug("Страница c id '{}' была обновлена с помощью метода updateById.", pageId);
        } else {
            throw new PageNotFoundException("Page с id '" + pageId + "' не найдена.");
        }
    }

    public void updateByPageAndSite(Page page) {
        String pagePath = page.getPath();
        int pageOriginalId = getPageIdByPathAndSite(pagePath, page.getSite());
        page.setId(pageOriginalId);
        pageRepository.save(page);
        logger.debug("Страница '{}' была обновлена с помощью метода updateByPageAndSite.", pagePath);
    }

    @Override
    public void delete(Integer id) {
        if (existsById(id)) {
            pageRepository.deleteById(id);
            logger.debug("Страница с id '{}' была удалена с помощью метода delete.", id);
        } else {
            throw new PageNotFoundException("Не удалось удалить страница c id '" + id + "' потому что она не существует.");
        }
    }

    public void deleteAndDecreaseFrequencyLema(Page page, HashSet<Lemma> lemmaSet) {
        int pageId = page.getId();
        if (existsById(pageId)) {
            for (Lemma lemma : lemmaSet) {
               if (lemma.getFrequency() > 1) {
                   lemma.setFrequency(lemma.getFrequency() - 1);
                   lemmaCRUDService.updateById(lemma);
               } else {
                   lemmaCRUDService.delete(lemma.getId());
               }
            }
            pageRepository.deleteById(pageId);
            logger.debug("Страница с id '{}' была удалена и частота леммы уменьшена.", pageId);
        } else {
            throw new PageNotFoundException("Не удалось удалить страницу c id '" + pageId + "' потому что она не существует.");
        }
    }

    public boolean existsById(Integer id) {
        return pageRepository.existsById(id);
    }

    public boolean existsByPathAndSite(String path, SiteEntity site) {
        return pageRepository.existsByPathAndSite(path, site);
    }



    public static PageDTO mapToPageDTO (Page page) {
        PageDTO pageDTO = new PageDTO();
        pageDTO.setId(page.getId());
        pageDTO.setSiteId(page.getSite().getId());
        pageDTO.setPath(page.getPath());
        pageDTO.setCode(page.getCode());
        pageDTO.setContent(page.getContent());
        if (!(page.getIndexList() == null)) {
            List<IndexEntityDTO> indexEntityDTOList = page.getIndexList().stream()
                    .map(IndexEntityCRUDService::mapToIndexEntityDTO)
                    .toList();
            pageDTO.setIndexList(indexEntityDTOList);
        }
        return pageDTO;
    }
}
