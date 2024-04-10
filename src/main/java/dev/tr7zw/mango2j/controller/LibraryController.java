package dev.tr7zw.mango2j.controller;

import java.util.ArrayList;
import java.util.List;

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

        // Return the name of the Thymeleaf template without the extension
        return "library";
    }

}
