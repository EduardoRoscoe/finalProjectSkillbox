package searchengine.dto.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import searchengine.model.Lemma;
import searchengine.model.Page;

import javax.persistence.*;

@Getter
@Setter
@NoArgsConstructor
public class IndexEntityDTO {
    private int id;
    private int pageId;
    private int lemmaId;
    private Float rank;
}
