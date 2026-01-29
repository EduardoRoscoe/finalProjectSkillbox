package searchengine.services;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import searchengine.dto.entities.IndexEntityDTO;
import searchengine.exceptions.IndexEntityAlreadyExistsException;
import searchengine.exceptions.IndexEntityNotFoundException;
import searchengine.model.IndexEntity;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.repositories.IndexEntityRepository;

import java.util.*;

@Service
@AllArgsConstructor
public class IndexEntityCRUDService implements CRUDService<IndexEntity> {
    private final IndexEntityRepository indexEntityRepository;
//    private final LemmaCRUDService lemmaCRUDService;

    private static final Logger logger = LoggerFactory.getLogger(IndexEntityCRUDService.class);


    @Override
    public void create(IndexEntity index) {
        Page indexPage = index.getPage();
        Lemma indexLemma = index.getLemma();
        if (!existsByPageAndLemma(indexPage, indexLemma)) {
            indexEntityRepository.save(index);
            logger.debug("Index c page '{}' и с lemma '{}' был создан с помощью метода create", indexPage.getPath(), indexLemma.getLemma());
        } else {
            throw new IndexEntityAlreadyExistsException(
                    "Index с page '" + indexPage.getPath() + "' c lemma '" + indexLemma.getLemma() + "' уже существует.");
        }
    }

    public void createOrUpdateByPageAndLemma(Page page, Lemma lemma, IndexEntity index) {
        if (existsByPageAndLemma(page, lemma)) {
            IndexEntity indexOldVersion = findByPageAndLemma(page, lemma);
            index.setId(indexOldVersion.getId());
            indexEntityRepository.save(index);
            logger.debug("Index с page '{}' и с lemma '{}' был обновлен с помощью метода createOrUpdateByPageAndLemma",
                    page.getPath(), lemma.getLemma());
        } else {
            indexEntityRepository.save(index);
            logger.debug("Index с page '{}' и с lemma '{}' был создан с помощью метода createOrUpdateByPageAndLemma",
                    page.getPath(), lemma.getLemma());
        }
    }

    public void createOrUpdateByPageLemmaAndRank(Page page, Lemma lemma, float rank) {
        IndexEntity index = new IndexEntity();
        index.setPage(page);
        index.setLemma(lemma);
        index.setRank(rank);

        createOrUpdateByPageAndLemma(page, lemma, index);
    }

    @Override
    public IndexEntity getById(Integer id) {
        IndexEntity foundIndex = indexEntityRepository.findById(id).orElseThrow(() ->
                new IndexEntityNotFoundException("Index с id " + id + " не найден."));
        logger.debug("Index с ID: '{}' был извлечен из базы данных с помощью метода getById", id);
        return foundIndex;
    }

    @Override
    public Collection<IndexEntity> getAll() {
        Collection<IndexEntity> indexEntities = Optional.ofNullable(indexEntityRepository.findAll()).orElseThrow(
                () -> new IndexEntityNotFoundException("Indexes не найдены"));
        logger.debug("Все Indexes были извлечены из базы данных с помощью метода getAll");
        return indexEntityRepository.findAll();
    }

    public int countLemmasInAPageByLemmaID(int lemmaId) {
        return indexEntityRepository.countByLemma_Id(lemmaId).orElseThrow(() ->
                new IndexEntityNotFoundException("Index с lemma ID " + lemmaId + " не найден."));
    }

    public List<Page> findPagesByLemma(Lemma lemma) {
        List<Page> foundPageList = indexEntityRepository.findPagesByLemma(lemma).orElseThrow(() ->
                new IndexEntityNotFoundException("Pages с Lemma '" + lemma + "' не найдены."));
        logger.debug("Pages с lemma были извлечены из базы данных с помощью метода findPagesByLemma");
        return foundPageList;
    }

    public HashSet<Lemma> findLemmasByPage(Page page) {
        Set<Lemma> foundLemmaSet = indexEntityRepository.findLemmasByPage(page).orElseThrow(() ->
                new IndexEntityNotFoundException("Lemmas с Page '" + page.getPath() + "' не найдены."));
        logger.debug("Lemmas с page были извлечены из базы данных с помощью метода findLemmasByPage");
        HashSet<Lemma> foundLemmaHashSet = new HashSet<>(foundLemmaSet); // Convert to HashSet to ensure uniqueness
        return foundLemmaHashSet;
    }

    public IndexEntity findByPageAndLemma(Page page, Lemma lemma) {
        String pagePath = page.getPath();
        String pageSite = page.getSite().getUrl();
        String lemmaString = lemma.getLemma();
        IndexEntity foundIndex = indexEntityRepository.findIndexEntityByPageAndLemma(page, lemma).orElseThrow(
                () -> new IndexEntityNotFoundException("Index c page '" + pagePath +"' и site '" +
                        pageSite + "' и с lemma '" + lemmaString + "' не найден."));
        logger.debug("Index с page '{}' и сайт '{}' и с lemma '{}' был извлечены из базы данных с помощью метода findByPageAndLemma",
                pagePath, pageSite, lemmaString);
        return foundIndex;
    }

    @Override
    public void updateById(IndexEntity index) {
        Integer id = index.getId();
        if (existsById(id)) {
            indexEntityRepository.save(index);
            logger.debug("Index с id '{}' был обновлен с помощью метода update", id);
        } else {
            throw new IndexEntityNotFoundException("Index с id " + id + " не найден.");
        }
    }

    public void updateByPageAndLemma(Page page, Lemma lemma, IndexEntity index) {
        if (existsByPageAndLemma(page, lemma)) {
            IndexEntity indexOldVersion = findByPageAndLemma(page, lemma);
            index.setId(indexOldVersion.getId());
            indexEntityRepository.save(index);
            logger.debug("Index с page '{}' и с lemma '{}' был обновлен с помощью метода updateByPageAndLemma",
                    page.getPath(), lemma.getLemma());
        } else {
            throw new RuntimeException("Index с page '" + page.getPath() + "' c site '" + page.getSite() +
                    "' и  lemma '" + lemma + "' не найден.");
        }
    }

    @Override
    public void delete(Integer id) {
        if (indexEntityRepository.existsById(id)) {
            indexEntityRepository.deleteById(id);
            logger.debug("Index с id '{}' был удален с помощью метода delete.", id);
        } else {
            throw new RuntimeException("Index c id '" + id + "' не найден.");
        }
    }

    public void deleteByPageAndLemma(Page page, Lemma lemma) {
        if (existsByPageAndLemma(page, lemma)) {
            indexEntityRepository.deleteByPageAndLemma(page, lemma);
            logger.debug("Index с page '{}' и с lemma '{}' был удален с помощью метода deleteByPageAndLemma",
                    page.getPath(), lemma.getLemma());
        } else {
            throw new RuntimeException("Index с page '" + page.getPath() + "' c site '" + page.getSite() +
                    "' и  lemma '" + lemma + "' не найден.");
        }
    }

    public boolean existsById(int id) {
        return indexEntityRepository.existsById(id);
    }

    public boolean existsByPageAndLemma(Page page, Lemma lemma) {
        return indexEntityRepository.existsByPageAndLemma(page, lemma);
    }

//    public boolean existsByPageIdAndLemmaId

    public static IndexEntityDTO mapToIndexEntityDTO(IndexEntity indexEntity) {
        IndexEntityDTO indexEntityDTO = new IndexEntityDTO();
        indexEntityDTO.setId(indexEntity.getId());
        indexEntityDTO.setPageId(indexEntity.getPage().getId());
        indexEntityDTO.setLemmaId(indexEntity.getLemma().getId());
        indexEntityDTO.setRank(indexEntity.getRank());
        return indexEntityDTO;
    }
}
