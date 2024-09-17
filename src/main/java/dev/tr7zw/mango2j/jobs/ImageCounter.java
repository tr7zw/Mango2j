package dev.tr7zw.mango2j.jobs;

import java.io.File;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import dev.tr7zw.mango2j.db.Chapter;
import dev.tr7zw.mango2j.db.ChapterRepository;
import dev.tr7zw.mango2j.service.ChapterWrapper;
import dev.tr7zw.mango2j.service.FileService;
import lombok.Getter;
import lombok.extern.java.Log;

@Component
@Log
public class ImageCounter implements DisposableBean {

    @Autowired
    private ChapterRepository chapterRepo;
    @Autowired
    private FileService fileService;
    private final Lock lock = new ReentrantLock();
    @Getter
    private boolean isRunning = false;
    private boolean cancel = false;

    @Async
    public void executeLongRunningTask() {
        if (lock.tryLock()) {
            try {
                if (!isRunning) {
                    isRunning = true;
                    log.info("ImageCounter task started.");
                    processChapters();
                    log.info("ImageCounter task completed.");
                } else {
                    log.info("ImageCounter task is already in progress.");
                }
            } finally {
                lock.unlock();
                isRunning = false;
            }
        } else {
            log.info("ImageCounter task is already locked.");
        }
    }

    private void processChapters() {
        for (Chapter chapter : chapterRepo.findAll()) {
            if (cancel)
                return;
            try {
                ChapterWrapper chapterWrapper = fileService.getChapterWrapper(new File(chapter.getFullPath()).toPath());
                Integer size = chapterWrapper.getFilesTyped(chapter.getId()).size();
                Integer old = chapter.getPageCount();
                if(!Objects.equals(size, old)) {
                    chapter.setPageCount(chapterWrapper.getFilesTyped(chapter.getId()).size());
                    chapterRepo.save(chapter);
                    log.log(Level.INFO, "Updated chapter size of " + chapter.getFullPath() + " from " + old + " to " + size);
                }
            } catch (Exception ex) {
                log.log(Level.WARNING, "Error while processing chapter " + chapter.getFullPath(), ex);
            }
        }
    }

    @Override
    public void destroy() throws Exception {
        cancel = true;
    }

}