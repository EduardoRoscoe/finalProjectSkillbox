package searchengine.dto.statistics;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import searchengine.repositories.SiteRepository;

@Data
@RequiredArgsConstructor
public class TotalStatistics {
    private int sites;
    private int pages;
    private int lemmas;
    private boolean indexing;

}
