package dev.tr7zw.mango2j.controller;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import dev.tr7zw.mango2j.Settings;
import dev.tr7zw.mango2j.db.Chapter;
import dev.tr7zw.mango2j.db.ChapterRepository;
import dev.tr7zw.mango2j.db.Title;
import dev.tr7zw.mango2j.db.TitleRepository;

@Controller
public class LibraryController {

    @Autowired
    private TitleRepository titleRepo;
    @Autowired
    private ChapterRepository chapterRepo;
    @Autowired
    private Settings settings;

    @GetMapping("/")
    public String index(Model model) {
        // Add necessary attributes to the model
        model.addAttribute("is_admin", true); // Example attribute, replace with your logic
        List<Chapter> chapters = chapterRepo.findTop100ByOrderByIdDesc();
        chapters.sort((a, b) -> Integer.compare(b.getId(), a.getId())); // newest to oldest
        model.addAttribute("chapters", chapters);
        model.addAttribute("titles", new ArrayList<>());
        model.addAttribute("name", "Latest");
        //model.addAttribute("chapterThumbnails", generateThumbnails(titles));
        // Return the name of the Thymeleaf template without the extension
        return "library";
    }

    @GetMapping("/library")
    public String home(Model model) {
        // Add necessary attributes to the model
        model.addAttribute("is_admin", true); // Example attribute, replace with your logic
        List<Title> titles = titleRepo.findByPath(settings.getBaseDir().getPath());
        titles.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        model.addAttribute("titles", titles);
        model.addAttribute("chapters", new ArrayList<>());
        model.addAttribute("chapterThumbnails", generateThumbnails(titles));

        // Return the name of the Thymeleaf template without the extension
        return "library";
    }

    @GetMapping("/library/{id}")
    public String libraryDir(@PathVariable Integer id,
            @RequestParam(name = "orderBy", defaultValue = "NEWEST") String orderBy, Model model) {
        // Add necessary attributes to the model
        Title title = titleRepo.getReferenceById(id);
        Title parent = titleRepo.findByFullPath(new File(title.getFullPath()).getParent());
        if (parent != null) {
            model.addAttribute("back", parent.getId());
        }
        model.addAttribute("is_admin", true); // Example attribute, replace with your logic
        List<Title> titles = titleRepo.findByPath(title.getFullPath());
        titles.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        model.addAttribute("titles", titles);
        List<Chapter> chapters = chapterRepo.findByPath(title.getFullPath());
        switch (orderBy.toUpperCase()) {
        case "NAME":
            chapters.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
            break;
        case "VIEWS":
            chapters.sort((a, b) -> Integer.compare(b.getViews() == null ? 0 : b.getViews(),
                    a.getViews() == null ? 0 : a.getViews()));
            break;
        case "PAGES":
            chapters.sort((a, b) -> Integer.compare(b.getPageCount() == null ? 0 : b.getPageCount(),
                    a.getPageCount() == null ? 0 : a.getPageCount()));
            break;
        case "LASTVIEWED":
            chapters.sort((a, b) -> {
                if (a.getLastView() == null && b.getLastView() == null)
                    return 0;
                if (a.getLastView() == null)
                    return 1;
                if (b.getLastView() == null)
                    return -1;
                return b.getLastView().compareTo(a.getLastView());
            });
            break;
        default:
            chapters.sort((a, b) -> Integer.compare(b.getId(), a.getId())); // newest to oldest
            break;
        }
        model.addAttribute("chapters", chapters);
        model.addAttribute("name", title.getName());
        model.addAttribute("path", title.getPath());
        model.addAttribute("orderBy", orderBy.toUpperCase());
        model.addAttribute("chapterThumbnails", generateThumbnails(titles));
        // Return the name of the Thymeleaf template without the extension
        return "library";
    }

    @GetMapping("/empty")
    public String empty(Model model) {
        // Add necessary attributes to the model
        model.addAttribute("is_admin", true); // Example attribute, replace with your logic
        List<Chapter> chapters = chapterRepo.findEmptyDownloads();
        model.addAttribute("chapters", chapters);
        model.addAttribute("titles", new ArrayList<>());
        model.addAttribute("name", "Empty");
        //model.addAttribute("chapterThumbnails", generateThumbnails(titles));
        // Return the name of the Thymeleaf template without the extension
        return "empty";
    }

    @GetMapping("/top")
    public String top(Model model) {
        // Add necessary attributes to the model
        model.addAttribute("is_admin", true); // Example attribute, replace with your logic
        List<Chapter> chapters = chapterRepo.findTop100ByOrderByViewsDesc();
        model.addAttribute("chapters", chapters);
        model.addAttribute("titles", new ArrayList<>());
        model.addAttribute("name", "Top Viewed");
        //model.addAttribute("chapterThumbnails", generateThumbnails(titles));
        // Return the name of the Thymeleaf template without the extension
        return "viewcount";
    }

    @GetMapping("/bottom")
    public String bottom(Model model) {
        // Add necessary attributes to the model
        model.addAttribute("is_admin", true); // Example attribute, replace with your logic
        List<Chapter> chapters = chapterRepo.findTop100ByOrderByViewsAsc();
        model.addAttribute("chapters", chapters);
        model.addAttribute("titles", new ArrayList<>());
        model.addAttribute("name", "Least Viewed");
        //model.addAttribute("chapterThumbnails", generateThumbnails(titles));
        // Return the name of the Thymeleaf template without the extension
        return "viewcount";
    }

    private Map<Title, Integer> generateThumbnails(List<Title> titles) {
        Map<Title, Integer> map = new HashMap<>();
        titles.forEach(t -> map.put(t, findThumbnail(t)));
        return map;
    }

    private Integer findThumbnail(Title title) {
        Optional<Chapter> chapter = chapterRepo.findByPath(title.getFullPath()).stream()
                .filter(c -> c.getThumbnail() != null).findAny();
        if (chapter.isPresent()) {
            return chapter.get().getId();
        }
        for (Title other : titleRepo.findByPath(title.getFullPath())) {
            Integer id = findThumbnail(other);
            if (id != null) {
                return id;
            }
        }
        return null;
    }

}
