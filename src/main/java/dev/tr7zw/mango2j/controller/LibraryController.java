package dev.tr7zw.mango2j.controller;

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
    public String libraryDir(@PathVariable Integer id, Model model) {
        // Add necessary attributes to the model
        Title title = titleRepo.getReferenceById(id);
        model.addAttribute("is_admin", true); // Example attribute, replace with your logic
        List<Title> titles = titleRepo.findByPath(title.getFullPath());
        titles.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        model.addAttribute("titles", titles);
        List<Chapter> chapters = chapterRepo.findByPath(title.getFullPath());
        chapters.sort((a, b) -> Integer.compare(b.getId(), a.getId())); // newest to oldest
        model.addAttribute("chapters", chapters);
        model.addAttribute("name", title.getName());
        model.addAttribute("chapterThumbnails", generateThumbnails(titles));
        // Return the name of the Thymeleaf template without the extension
        return "library";
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
