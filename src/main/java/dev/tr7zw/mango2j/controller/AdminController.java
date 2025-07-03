package dev.tr7zw.mango2j.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import dev.tr7zw.mango2j.db.Chapter;
import dev.tr7zw.mango2j.db.ChapterRepository;
import dev.tr7zw.mango2j.db.Title;
import dev.tr7zw.mango2j.db.TitleRepository;
import dev.tr7zw.mango2j.jobs.FileScanner;
import dev.tr7zw.mango2j.jobs.ThumbnailGenerator;
import dev.tr7zw.mango2j.service.AiService;
import dev.tr7zw.mango2j.service.MoveTargetService;
import lombok.extern.java.Log;

@Controller
@Log
public class AdminController {

    @Autowired
    private AiService aiService;
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
        return new ResponseEntity<>("Ollama Status: " + aiService.available(), null, HttpStatus.OK);
    }
    
    
    @GetMapping("/admin/find")
    public ResponseEntity<String> find(@RequestParam(name = "value") String value) throws IOException {
        List<Chapter> chapters = aiService.findClosest(value, 3);
        return new ResponseEntity<>("Chapters: " + chapters.stream().map(c -> c.getName() + "<br>" + c.getDescription() + "<br><br>").toList(), null, HttpStatus.OK);
    }
    
    @GetMapping("/admin/reset")
    public ResponseEntity<String> reset() throws IOException {
        for(Chapter chapter : chapterRepo.findAll()) {
            chapter.setDescription(null);
            chapter.setEmbedding(null);
            chapterRepo.save(chapter);
        }
        return new ResponseEntity<>("Ok", null, HttpStatus.OK);
    }
    
}
