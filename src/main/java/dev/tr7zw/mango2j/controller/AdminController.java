package dev.tr7zw.mango2j.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import dev.tr7zw.mango2j.Settings;
import dev.tr7zw.mango2j.db.Chapter;
import dev.tr7zw.mango2j.db.ChapterRepository;
import dev.tr7zw.mango2j.db.Title;
import dev.tr7zw.mango2j.db.TitleRepository;
import dev.tr7zw.mango2j.jobs.FileScanner;
import dev.tr7zw.mango2j.jobs.ThumbnailGenerator;
import dev.tr7zw.mango2j.service.MoveTargetService;
import io.github.ollama4j.OllamaAPI;
import lombok.extern.java.Log;

@Controller
@Log
public class AdminController {

    @Autowired
    private Settings settings;
    @Autowired
    private ThumbnailGenerator thumbnailGenerator;
    @Autowired
    private FileScanner fileScanner;
    @Autowired
    private TitleRepository titleRepo;
    @Autowired
    private ChapterRepository chapterRepo;
    @Autowired
    private MoveTargetService moveTargetService;
    
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
    
    @GetMapping("/admin/move/{chapterId}/{targetId}")
    public String moveChapter(@PathVariable Integer chapterId, @PathVariable Integer targetId) throws IOException {
        // move logic
        File targetFolder = moveTargetService.getMoveTargets().get(targetId);
        Chapter chapter = chapterRepo.getReferenceById(chapterId);
        Title title = titleRepo.findByFullPath(chapter.getPath());
        File f = new File(chapter.getFullPath());
        File targetFile = new File(targetFolder, f.getName());
        log.info("Moving " + f.getAbsolutePath() + " to " + targetFile.getAbsolutePath());
        Files.move(f.toPath(), targetFile.toPath());
        chapter.setFullPath(targetFile.toPath().toString());
        chapter.setPath(targetFile.toPath().getParent().toString());
        chapterRepo.save(chapter);
        return "redirect:/library/" + title.getId();
    }
    
    @GetMapping("/admin/pingOllama")
    public ResponseEntity<String> pingOllama() throws IOException {
        OllamaAPI ollamaAPI = new OllamaAPI(settings.getOllamaHost());
        return new ResponseEntity<>("Ollama Status: " + ollamaAPI.ping(), null, HttpStatus.OK);
    }
    
}
