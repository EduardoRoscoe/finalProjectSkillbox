package searchengine.dto.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import searchengine.model.IndexEntity;
import searchengine.model.SiteEntity;

import javax.persistence.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class LemmaDTO {
    private int id;
    private int siteId;
    private String lemma;
    private int frequency;
    private List<IndexEntityDTO> indexList;
}
