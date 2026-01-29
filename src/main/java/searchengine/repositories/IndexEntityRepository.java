package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.IndexEntity;
import searchengine.model.Lemma;
import searchengine.model.Page;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface IndexEntityRepository extends JpaRepository<IndexEntity, Integer> {
    @Query("SELECT DISTINCT p FROM IndexEntity i JOIN i.lemma l JOIN i.page p WHERE l = :lemma")
    Optional<List<Page>> findPagesByLemma(@Param("lemma") Lemma lemma);

    @Query("SELECT DISTINCT i.lemma FROM IndexEntity i WHERE i.page = :page")
    Optional<Set<Lemma>> findLemmasByPage(@Param("page") Page page);

    Optional<IndexEntity> findIndexEntityByPageAndLemma(Page page, Lemma lemma);

    Optional<Integer> countByLemma_Id(int lemmaId);

    boolean existsByPageAndLemma(Page page, Lemma lemma);

    void deleteByPageAndLemma(Page page, Lemma lemma);
}
