package dev.tr7zw.mango2j.controller;

import java.io.File;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import dev.tr7zw.mango2j.db.Chapter;
import dev.tr7zw.mango2j.db.ChapterRepository;
import dev.tr7zw.mango2j.db.Title;
import dev.tr7zw.mango2j.db.TitleRepository;
import dev.tr7zw.mango2j.jobs.FileScanner;
import dev.tr7zw.mango2j.jobs.ThumbnailGenerator;
import lombok.extern.java.Log;

@Controller
@Log
public class AdminController {

    @Autowired
    private ThumbnailGenerator thumbnailGenerator;
    @Autowired
    private FileScanner fileScanner;
    @Autowired
    private TitleRepository titleRepo;
    @Autowired
    private ChapterRepository chapterRepo;
    
    @GetMapping("/admin/generateThumbnails")
    public ResponseEntity<String> generateThumbnails() throws IOException {
        if(thumbnailGenerator.isRunning()) {
            return new ResponseEntity<>("Already running", null, HttpStatus.OK);
        }
        thumbnailGenerator.executeLongRunningTask();
        return new ResponseEntity<>("Ok", null, HttpStatus.OK);
    }
    
    @GetMapping("/admin/scanFiles")
    public ResponseEntity<String> scanFiles() throws IOException {
        if(fileScanner.isRunning()) {
            return new ResponseEntity<>("Already running", null, HttpStatus.OK);
        }
        fileScanner.executeLongRunningTask();
        return new ResponseEntity<>("Ok", null, HttpStatus.OK);
    }
    
    @GetMapping("/admin/delete/{id}")
    public String deleteChapter(@PathVariable Integer id) {
        // Add necessary attributes to the model
        Chapter chapter = chapterRepo.getReferenceById(id);
        Title title = titleRepo.findByFullPath(chapter.getPath());
        File f = new File(chapter.getFullPath());
        f.delete();
        log.info("Deleting " + f.getAbsolutePath());
        chapterRepo.delete(chapter);
        return "redirect:/library/" + title.getId();
    }
    
}
