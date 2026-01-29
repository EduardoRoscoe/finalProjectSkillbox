package searchengine.config;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import searchengine.model.SiteEntity;
import searchengine.repositories.SiteRepository;
import searchengine.services.SiteCRUDService;

import java.util.Optional;

@Component
@AllArgsConstructor
public class StringToSiteEntityConverter implements Converter<String, Optional<SiteEntity>> {

    private final SiteCRUDService siteCRUDService;

    @Override
    public Optional<SiteEntity> convert(String source) {
        SiteEntity siteEntity = siteCRUDService.getByURL(source); // or findById(Long.parseLong(source))
        return Optional.of(siteEntity);
    }
}
