package searchengine.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Getter
@Setter
@Table(name = "index_table")
public class IndexEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @ManyToOne
    @JsonBackReference
    @JoinColumn(name = "page_id", foreignKey = @ForeignKey(name = "FK_index_page_id"))
    private Page page;

    @ManyToOne
    @JsonBackReference
    @JoinColumn(name = "lemma_id", foreignKey = @ForeignKey(name = "FK_index_lemma_id"))
    private Lemma lemma;

    @Column(name = "rank_column")
    private Float rank;
}
