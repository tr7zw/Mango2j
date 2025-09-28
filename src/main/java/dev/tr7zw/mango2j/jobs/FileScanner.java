package dev.tr7zw.mango2j.jobs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dev.tr7zw.mango2j.Settings;
import dev.tr7zw.mango2j.db.Chapter;
import dev.tr7zw.mango2j.db.ChapterRepository;
import dev.tr7zw.mango2j.db.Title;
import dev.tr7zw.mango2j.db.TitleRepository;
import dev.tr7zw.mango2j.service.FileService;
import dev.tr7zw.mango2j.service.TitleWrapper;
import lombok.Getter;
import lombok.extern.java.Log;

@Component
@Log
public class FileScanner implements DisposableBean {

    @Autowired
    private JobLock jobLock;
    @Autowired
    private Settings settings;
    @Autowired
    private TitleRepository titleRepo;
    @Autowired
    private ChapterRepository chapterRepo;
    @Autowired
    private FileService fileService;
    @Autowired
    private ThumbnailGenerator thumbnailGenerator;
    @Autowired
    private ChapterAnalyser chapterAnalyser;
    @Autowired
    private ImageCounter imageCounter;
    private final Lock lock = new ReentrantLock();
    @Getter
    private boolean isRunning = false;
    private boolean cancel = false;

    @Async
    @Scheduled(fixedDelay = 3600000) // Run once per hour
    public void executeLongRunningTask() {
        boolean triggered = false;
        if (lock.tryLock()) {
            jobLock.getLock().lock();
            try {
                if (!isRunning) {
                    isRunning = true;
                    log.info("File Scanner task started.");
                    scanTitle(settings.getBaseDir().toPath());
                    if (cancel)
                        return;
                    deleteRemovedTitles();
                    if (cancel)
                        return;
                    scanChapters();
                    if (cancel)
                        return;
                    deleteRemovedChapters();
                    if (cancel)
                        return;
                    deleteEmptyTitles();
                    if (cancel)
                        return;
                    triggered = true;
                    log.info("File Scanner task completed.");
                } else {
                    log.info("File Scanner task is already in progress.");
                }
            } finally {
                jobLock.getLock().unlock();
                lock.unlock();
                isRunning = false;
            }
        } else {
            log.info("File Scanner task is already locked.");
        }
        if (triggered) {
            thumbnailGenerator.executeLongRunningTask();
            imageCounter.executeLongRunningTask();
            chapterAnalyser.executeLongRunningTask();
        }
    }

    private void deleteRemovedTitles() {
        titleRepo.findAll().forEach(dbTitle -> {
            File f = new File(dbTitle.getFullPath());
            if (!f.exists()) {
                titleRepo.delete(dbTitle);
                log.info("Deleted " + f);
            }
        });
    }

    private void scanTitle(Path path) {
        if (cancel)
            return;
        try {
            if (!titleRepo.existsByFullPath(path.toString())) {
                titleRepo.save(new Title(path.toString(), path.getParent().toString(), path.getFileName().toString()));
                log.info("Saved new Title:\t" + path.toString());
            }
            TitleWrapper title = fileService.getTitleWrapper(path);
            for (Path subTitle : title.getTitles()) {
                scanTitle(subTitle);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void scanChapters() {
        for (Title dbTitle : titleRepo.findAll()) {
            if (cancel)
                return;
            try {
                TitleWrapper title = fileService.getTitleWrapper(new File(dbTitle.getFullPath()).toPath());
                for (Path chapterPath : title.getChapters()) {
                    if (!chapterRepo.existsByFullPath(chapterPath.toString())) {
                        chapterRepo.save(new Chapter(chapterPath.toString(), chapterPath.getParent().toString(),
                                chapterPath.getFileName().toString()));
                        log.info("Saved new Chapter:\t" + chapterPath.toString());
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void deleteRemovedChapters() {
        chapterRepo.findAll().forEach(dbChapter -> {
            if (cancel)
                return;
            File f = new File(dbChapter.getFullPath());
            if (!f.exists() || (dbChapter.getPageCount() != null && dbChapter.getPageCount() == 0)) {
                chapterRepo.delete(dbChapter);
                log.info("Deleted " + f);
            }
        });
    }
    
    private void deleteEmptyTitles() {
        titleRepo.findAll().forEach(dbTitle -> {
            if (cancel)
                return;
            if (chapterRepo.findByPath(dbTitle.getFullPath()).isEmpty() && titleRepo.findByPath(dbTitle.getFullPath()).isEmpty()) {
                File f = new File(dbTitle.getFullPath());
                titleRepo.delete(dbTitle);
                log.info("Deleted " + f);
            }
        });
    }


    @Override
    public void destroy() throws Exception {
        cancel = true;
    }

}