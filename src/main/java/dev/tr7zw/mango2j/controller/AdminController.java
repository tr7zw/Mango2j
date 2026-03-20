package dev.tr7zw.mango2j.controller;

import dev.tr7zw.mango2j.db.*;
import dev.tr7zw.mango2j.jobs.*;
import dev.tr7zw.mango2j.service.*;
import dev.tr7zw.mango2j.util.*;
import lombok.extern.java.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.http.*;
import org.springframework.stereotype.*;
import org.springframework.ui.*;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

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
    @Autowired
    private MoveTargetService moveTargetService;
    @Autowired
    private StatusUtil statusUtil;

    @GetMapping("/admin/status")
    public ResponseEntity<String> getStatus() {
        return new ResponseEntity<>(statusUtil.getScanStatus(), HttpHeaders.EMPTY, HttpStatus.OK);
    }

    @GetMapping("/admin/dashboard")
    public String dashboard(Model model) {
        List<Title> allTitles = titleRepo.findAll();
        List<Chapter> allChapters = chapterRepo.findAll();

        long totalViews = allChapters.stream()
                .mapToLong(c -> c.getViews() != null ? c.getViews() : 0)
                .sum();

        long chaptersWithoutThumbnails = allChapters.stream()
                .filter(c -> c.getThumbnail() == null || c.getThumbnail().length == 0)
                .count();

        double avgViewsPerChapter = allChapters.isEmpty() ? 0 : (double) totalViews / allChapters.size();
        // round to 2 decimal places
        avgViewsPerChapter = Math.round(avgViewsPerChapter * 100.0) / 100.0;

        // File size statistics
        long totalFileSize = allChapters.stream()
                .mapToLong(c -> c.getFileSize() != null ? c.getFileSize() : 0)
                .sum();

        long maxFileSize = allChapters.stream()
                .mapToLong(c -> c.getFileSize() != null ? c.getFileSize() : 0)
                .max()
                .orElse(0);

        double avgFileSize = allChapters.isEmpty() ? 0 : (double) totalFileSize / allChapters.size();

        model.addAttribute("totalTitles", allTitles.size());
        model.addAttribute("totalChapters", allChapters.size());
        model.addAttribute("totalViews", totalViews);
        model.addAttribute("chaptersWithoutThumbnails", chaptersWithoutThumbnails);
        model.addAttribute("avgViewsPerChapter", avgViewsPerChapter);
        model.addAttribute("totalFileSize", FormatUtil.formatFileSize(totalFileSize));
        model.addAttribute("maxFileSize", FormatUtil.formatFileSize(maxFileSize));
        model.addAttribute("avgFileSize", FormatUtil.formatFileSize((long) avgFileSize));
        model.addAttribute("mostViewedChapter", allChapters.stream().max(Comparator.comparingInt(a -> a.getViews() != null ? a.getViews() : 0)).orElse(null));
        model.addAttribute("leastViewedChapter", allChapters.stream().filter(c -> c.getViews() != null && c.getViews() > 0).min(Comparator.comparingInt(a -> a.getViews() != null ? a.getViews() : 0)).orElse(null));

        return "admin-dashboard";
    }

    @GetMapping("/admin/generateThumbnails")
    public ResponseEntity<String> generateThumbnails() throws IOException {
        if (thumbnailGenerator.isRunning()) {
            return new ResponseEntity<>("Already running", HttpHeaders.EMPTY, HttpStatus.OK);
        }
        thumbnailGenerator.executeLongRunningTask();
        return new ResponseEntity<>("Ok", HttpHeaders.EMPTY, HttpStatus.OK);
    }

    @GetMapping("/admin/scanFiles")
    public ResponseEntity<String> scanFiles() throws IOException {
        if (fileScanner.isRunning()) {
            return new ResponseEntity<>("Already running", HttpHeaders.EMPTY, HttpStatus.OK);
        }
        fileScanner.executeLongRunningTask();
        return new ResponseEntity<>("Ok", HttpHeaders.EMPTY, HttpStatus.OK);
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

    @GetMapping("/admin/resetDescriptions")
    public ResponseEntity<String> reset() throws IOException {
        for (Chapter chapter : chapterRepo.findAll()) {
            chapter.setDescription(null);
            chapterRepo.save(chapter);
        }
        return new ResponseEntity<>("Ok", HttpHeaders.EMPTY, HttpStatus.OK);
    }

    @GetMapping("/admin/refreshThumbnails")
    public ResponseEntity<String> refreshThumbnails() throws IOException {
        if (thumbnailGenerator.isRunning()) {
            return new ResponseEntity<>("Already running", HttpHeaders.EMPTY, HttpStatus.OK);
        }
        thumbnailGenerator.setRefreshImages(true);
        thumbnailGenerator.executeLongRunningTask();
        return new ResponseEntity<>("Ok", HttpHeaders.EMPTY, HttpStatus.OK);
    }

}
