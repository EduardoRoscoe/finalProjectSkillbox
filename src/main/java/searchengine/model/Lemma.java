package searchengine.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "lemma", indexes = {
        @Index(name = "lemma_index", columnList = "lemma")
})
public class Lemma {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @ManyToOne
    @JsonBackReference
    @JoinColumn(name = "site_id", foreignKey = @ForeignKey(name = "FK_lemma_site_id"))
    private SiteEntity site;

    @Column(name = "lemma", columnDefinition = "VARCHAR(255)")
    private String lemma;

    @Column(name = "frequency")
    private int frequency;

    @OneToMany(mappedBy = "lemma", cascade = CascadeType.REMOVE ,fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<IndexEntity> indexList;
}
