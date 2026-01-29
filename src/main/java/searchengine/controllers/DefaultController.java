package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import searchengine.config.SitesList;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class DefaultController {
    private final SitesList sitesList;

    /**
     * Метод формирует страницу из HTML-файла index.html,
     * который находится в папке resources/templates.
     * Это делает библиотека Thymeleaf.
     */
    @RequestMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/")
    public String showSearchForm(Model model) {
        List<String> sitesNames = new ArrayList<>();
        sitesList.getSites().forEach(site -> sitesNames.add(site.getUrl()));
        // You can fetch this from DB too
        model.addAttribute("sites", sitesNames);
        return "index"; // This should be your Thymeleaf template name
    }
}
