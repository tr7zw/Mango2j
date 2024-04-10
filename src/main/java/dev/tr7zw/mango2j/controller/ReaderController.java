package dev.tr7zw.mango2j.controller;

import java.io.File;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import dev.tr7zw.mango2j.db.Chapter;
import dev.tr7zw.mango2j.db.ChapterRepository;
import dev.tr7zw.mango2j.db.Title;
import dev.tr7zw.mango2j.db.TitleRepository;
import dev.tr7zw.mango2j.service.ChapterWrapper;
import dev.tr7zw.mango2j.service.FileService;

@Controller
public class ReaderController {

    private final FileService fileService;
    private final TitleRepository titleRepo;
    private final ChapterRepository chapterRepo;

    @Autowired
    public ReaderController(FileService fileService, TitleRepository titleRepo, ChapterRepository chapterRepo) {
        this.fileService = fileService;
        this.titleRepo = titleRepo;
        this.chapterRepo = chapterRepo;
    }

    @GetMapping("/reader/{id}")
    public String home(@PathVariable Integer id, Model model) {
        // Add necessary attributes to the model
        model.addAttribute("mode", "continuous");
        Chapter chapter = chapterRepo.getReferenceById(id);
        Title title = titleRepo.findByFullPath(chapter.getPath());
        ChapterWrapper chapterWrapper = fileService.getChapterWrapper(new File(chapter.getFullPath()).toPath());
        model.addAttribute("items", chapterWrapper.getFilesTyped(id));
        model.addAttribute("titleid", "123");
        model.addAttribute("entryid", id);
        model.addAttribute("page_idx", 0);
        model.addAttribute("base_url", "http://localhost:8080");
        model.addAttribute("exit_url", "/library/" + title.getId());

        return "reader";
    }

}
