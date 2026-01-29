package searchengine.dto.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import searchengine.model.IndexEntity;
import searchengine.model.Page;
import searchengine.model.SiteEntity;

import javax.persistence.*;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
public class PageDTO {
    private int id;
    private int siteId;
    private String path;
    private int code;
    private String content;
    private List<IndexEntityDTO> indexList;
}
