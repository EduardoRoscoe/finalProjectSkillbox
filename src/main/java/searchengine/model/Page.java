package searchengine.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.List;
import java.util.Objects;

@Entity
@Getter
@Setter
@Table(name = "page")
public class Page {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @ManyToOne
    @JsonBackReference
    @JoinColumn(name = "site_id", foreignKey = @ForeignKey(name = "FK_page_site_id"))
    private SiteEntity site;

    @Column(name = "path", columnDefinition = "TEXT", nullable = true)
    // , nullable = true, unique = true - with TEXT I need to do this command in the database ALTER TABLE page ADD UNIQUE INDEX unique_path (path(255));
    private String path;

    @Column(name = "code")
    private int code;

    @Column(name = "content", columnDefinition = "MEDIUMTEXT")
    private String content;

//    @JsonManagedReference
    @OneToMany(mappedBy = "page", cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
    private List<IndexEntity> indexList;


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Page page = (Page) o;
        return Objects.equals(path, page.path) && Objects.equals(site.getUrl(), page.getSite().getUrl());
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, site.getUrl());
    }
}
