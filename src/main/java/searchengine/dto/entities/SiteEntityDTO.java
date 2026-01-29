package searchengine.dto.entities;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Status;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class SiteEntityDTO {
    private int id;
    private String name;
    private Status status;
    private LocalDateTime statusTime;
    private String lastError;
    private String url;
    private List<PageDTO> pages;
    private List<LemmaDTO> lemmaList;
}
