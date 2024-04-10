package dev.tr7zw.mango2j;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.tr7zw.mango2j.db.Chapter;
import dev.tr7zw.mango2j.db.ChapterRepository;
import dev.tr7zw.mango2j.db.Title;
import dev.tr7zw.mango2j.db.TitleRepository;

@RestController
public class RestTest {

    private final TitleRepository titleRepo;
    private final ChapterRepository chapterRepo;
    
    @Autowired
    public RestTest(TitleRepository titleRepo, ChapterRepository chapterRepo) {
        this.titleRepo = titleRepo;
        this.chapterRepo = chapterRepo;
    }
    
    @GetMapping(value = "/allTitles", produces = MediaType.APPLICATION_JSON_VALUE) 
    public List<Title> getAllTitles() throws IOException {
        return titleRepo.findAll();
    }
    
    @GetMapping(value = "/allChapters", produces = MediaType.APPLICATION_JSON_VALUE) 
    public List<Chapter> getAllChapters() throws IOException {
        return chapterRepo.findAll();
    }
}
