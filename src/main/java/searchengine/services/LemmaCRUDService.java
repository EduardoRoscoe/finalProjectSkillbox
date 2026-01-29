package searchengine.services;

import liquibase.integration.ant.DatabaseUpdateTask;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.dto.entities.LemmaDTO;
import searchengine.exceptions.LemmaAlreadyExistsException;
import searchengine.exceptions.LemmaNotFoundException;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.SiteEntity;
import searchengine.repositories.LemmaRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LemmaCRUDService implements CRUDService<Lemma> {
    private final LemmaRepository lemmaRepository;

    private final IndexEntityCRUDService indexEntityCRUDService;

    Logger logger = LoggerFactory.getLogger(LemmaCRUDService.class);


    @Override
    public void create(Lemma lemma) {
        String lemmaString = lemma.getLemma();
        SiteEntity lemmaSite = lemma.getSite();
        if (!lemmaRepository.existsByLemmaAndSite(lemmaString, lemmaSite)) {
            lemmaRepository.save(lemma);
            logger.debug("Lemma '{}' с сайтом '{}' была создана с помощью метода create.", lemmaString, lemmaSite.getUrl());
        } else {
            throw new LemmaAlreadyExistsException("Lemma '" + lemmaString + "' уже существует.");
        }
    }

    public void createWithoutThrowingError(Lemma lemma) {
        String lemmaString = lemma.getLemma();
        SiteEntity lemmaSite = lemma.getSite();
        if (!lemmaRepository.existsByLemmaAndSite(lemmaString, lemmaSite)) {
            lemmaRepository.save(lemma);
            logger.debug("Lemma '{}' с сайтом '{}' была создана с помощью метода create.", lemmaString, lemmaSite.getUrl());
        }
    }

    public void createOrUpdateAndIncreaseFrequencyByOne(String lemmaString, SiteEntity lemmaSite) {
        Lemma lemma = new Lemma();
        lemma.setLemma(lemmaString);
        lemma.setSite(lemmaSite);
        if (!existsByLemmaAndSite(lemmaString, lemmaSite)) {
            lemma.setFrequency(1);
            lemmaRepository.save(lemma);
            logger.debug("Lemma '{}' с сайтом '{}' была создана с помощью метода createOrUpdateAndIncreaseFrequencyByOne.", lemmaString, lemmaSite.getUrl());
        } else {
            Lemma lemmaOriginal = getLemmaByLemmaAndSite(lemmaString, lemma.getSite());
            int frequencyPlusOne = lemmaOriginal.getFrequency() + 1;
            lemma.setFrequency(frequencyPlusOne);
            if (lemmaString.equals("восточный")) {
                logger.info("lemma string {}, frequency {}, newFrequency {}", lemmaString, lemma.getFrequency(), frequencyPlusOne);
            }
            lemma.setId(lemmaOriginal.getId());
            Instant before = Instant.now();
            lemmaRepository.save(lemma);
            Instant after = Instant.now();
            long duration = Duration.between(before, after).toMillis();
            logger.debug("Duration of saving lemma: {}", duration);
            logger.debug("Lemma '{}' была обновлена с помощью метода createOrUpdateAndIncreaseFrequencyByOne.", lemmaString);
        }

    }

    public void createLemmasFromList(List<Lemma> lemmaList) {
        Instant beforeLemmaSaveAll = Instant.now();
        lemmaRepository.saveAll(lemmaList);
        Instant afterLemmaSaveAll = Instant.now();
        long duration = Duration.between(beforeLemmaSaveAll, afterLemmaSaveAll).toMillis();
        logger.debug("Duration of lemma Save All: {}", duration);
    }
    @Override
    public Lemma getById(Integer id) {
        Lemma foundLemma = lemmaRepository.findById(id).orElseThrow(() -> new LemmaNotFoundException("Lemma с id " + id + " не найдена."));
        logger.debug("Lemma с id была извлечена из базы данных с помощью метода getById.");
        return foundLemma;
    }

    @Override
    public Collection<Lemma> getAll() {
        Collection<Lemma> foundLemmaCollection = Optional.ofNullable(lemmaRepository.findAll()).orElseThrow(() ->
                new LemmaNotFoundException("Lemmas не найдены."));
        logger.debug("Lemmas были извлечены из базы данных с помощью метода getAll.");
        return foundLemmaCollection;
    }

    public List<Lemma> getLemmasBySiteId(int siteId) {
        List<Lemma> foundLemmaList = lemmaRepository.findAllLemmasBySiteId(siteId).orElseThrow(
                () -> new LemmaNotFoundException("Lemmas c сайт id " + siteId + " не найдены."));
        logger.debug("Lemmas c сайт id '{}' найдены с помощью метода getLemmasBySiteId", siteId);
        return foundLemmaList;
    }

    public Lemma getLemmaByLemmaAndSite(String lemma, SiteEntity siteEntity) {
        logger.debug("Lemma '{}' and site '{}' in method getLemmaByLemmaAndSite", lemma, siteEntity.getUrl());
        Lemma foundLemma = lemmaRepository.findByLemmaAndSite(lemma, siteEntity).orElseThrow(
                () -> new LemmaNotFoundException("Lemma '" + lemma + "' с сайтом '" + siteEntity.getUrl() + "' не найдена."));
        logger.debug("Lemma '{}' была извлечена c помощью метода getLemmaByLemmaAndSite.", lemma);
        return foundLemma;
    }

    public int countLemmasBySiteId(int siteId) {
        int count = lemmaRepository.countLemmasBySiteId(siteId).orElseThrow(
                () -> new LemmaNotFoundException("Lemmas c сайт id " + siteId + " не найдены."));
        logger.debug("Количество lemmas c сайт id '{}' найдено с помощью метода countLemmasBySiteId", siteId);
        return count;
    }

    @Override
    public void updateById(Lemma lemma) {
        int lemmaId = lemma.getId();
        if (existsById(lemmaId)) {
            lemmaRepository.save(lemma);
            logger.debug("Lemma с id '{}' была обновлена с помощью метода updateById", lemmaId);
        } else {
            throw new  LemmaNotFoundException("Lemma '" + lemma + "' c id '" + lemmaId + "' не найдена.");
        }
    }

    public void updateByLemmaAndSite(Lemma lemma) {
        if (existsByLemmaAndSite(lemma.getLemma(), lemma.getSite())) {
            lemmaRepository.save(lemma);
            logger.debug("Lemma '{}' с сайтом '{}' была обновлена с помощью метода updateByLemmaAndSite", lemma, lemma.getSite().getUrl());
        } else {
            throw new LemmaNotFoundException("Lemma '" + lemma + "' не найдена.");
        }
    }

    @Override
    public void delete(Integer id) {
        if (existsById(id)) {
            lemmaRepository.deleteById(id);
            logger.debug("Lemma с id '{}' был удален c помощью метода delete.", id);
        } else {
            throw new LemmaNotFoundException("Не удалось удалить lemma c id '" + id + "' потому что он не существует.");
        }
    }

    public boolean existsById(Integer id) {
        return lemmaRepository.existsById(id);
    }

    public boolean existsByLemmaAndSite(String lemma, SiteEntity site) {
        return lemmaRepository.existsByLemmaAndSite(lemma, site);
    }

    public static LemmaDTO mapToLemmaDTO(Lemma lemma) {
        LemmaDTO lemmaDTO = new LemmaDTO();
        lemmaDTO.setId(lemma.getId());
        lemmaDTO.setSiteId(lemma.getSite().getId());
        lemmaDTO.setLemma(lemma.getLemma());
        lemmaDTO.setFrequency(lemma.getFrequency());
        if (!(lemma.getIndexList() == null)) {
            lemmaDTO.setIndexList(lemma.getIndexList()
                    .stream()
                    .map(IndexEntityCRUDService::mapToIndexEntityDTO)
                    .toList());
        }
        return lemmaDTO;
    }
}
