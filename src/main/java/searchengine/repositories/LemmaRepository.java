package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;
import searchengine.model.SiteEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {
    boolean existsByLemma(String lemma);

    boolean existsByLemmaAndSite(String lemma, SiteEntity siteEntity);
    Optional<Lemma> findByLemma(String lemma);

    Optional<Lemma> findByLemmaAndSite(String lemma, SiteEntity siteEntity);

    Optional<List<Lemma>> findAllLemmasBySiteId(int siteId);

    Optional<Integer> countLemmasBySiteId(int siteId);
}
