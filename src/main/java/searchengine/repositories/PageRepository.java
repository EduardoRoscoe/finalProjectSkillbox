package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.Page;
import searchengine.model.SiteEntity;

import javax.persistence.criteria.CriteriaBuilder;
import java.util.List;
import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {
    boolean existsByPath(String path);
    boolean existsByPathAndSite(String path, SiteEntity siteEntity);

    Page getByPath(String path);
    Optional<Page> findByPathAndSite(String path, SiteEntity siteEntity);
    Page getByPathAndSite(String path, SiteEntity siteEntity);

    Optional<Page> findByPathAndSiteId(String path, int siteId);
    Optional<List<Page>> findAllPagesBySiteId(int siteId);

    Optional<Integer> countPagesBySiteId(int siteId);

    @Query(value = """
        SELECT p.*
        FROM page p
        JOIN index_table it ON p.id = it.page_id
        WHERE it.lemma_id IN (:lemmaIds)
           AND p.site_id = :siteId
        GROUP BY p.id
        HAVING COUNT(DISTINCT it.lemma_id) = :lemmaCount
    """, nativeQuery = true)
    Optional<List<Page>> findPagesOfAWebsiteContainingAllLemmas(@Param("siteId") int siteId,
                                            @Param("lemmaIds") List<Integer> lemmaIds,
                                            @Param("lemmaCount") int lemmaCount);
    @Query(value = """
        SELECT p.*
        FROM page p
        JOIN index_table it ON p.id = it.page_id
        WHERE it.lemma_id IN (:lemmaIds)
        GROUP BY p.id
        HAVING COUNT(DISTINCT it.lemma_id) = :lemmaCount
    """, nativeQuery = true)
    Optional<List<Page>> findPagesOfAllSitesContainingAllLemmas(@Param("lemmaIds") List<Integer> lemmaIds,
                                                      @Param("lemmaCount") int lemmaCount);

    @Query("SELECT i.page FROM IndexEntity i WHERE i.lemma.id = :lemmaId")
    Optional<List<Page>> findPagesByLemmaId(@Param("lemmaId") int lemmaId);

    @Query("SELECT i.page FROM IndexEntity i WHERE i.lemma.id = :lemmaId AND i.page IN :pageList")
    Optional<List<Page>> findPagesByLemmaIdAndPageInPageList(@Param("lemmaId") int lemmaId,
                                           @Param("pageList") List<Page> pageList);
}
