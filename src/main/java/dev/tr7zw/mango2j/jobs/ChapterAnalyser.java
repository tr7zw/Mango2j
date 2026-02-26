package dev.tr7zw.mango2j.jobs;

import java.io.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

import dev.tr7zw.mango2j.db.*;
import dev.tr7zw.mango2j.service.*;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.extern.java.Log;

@Component
@Log
public class ChapterAnalyser implements DisposableBean {

    @Autowired
    private JobLock jobLock;
    @Autowired
    private ChapterRepository chapterRepo;
    @Autowired
    private TitleRepository titleRepo;
    @Autowired
    private FileService fileService;
    private final Lock lock = new ReentrantLock();
    @Getter
    private boolean isRunning = false;
    private boolean cancel = false;

    @Async
    public void executeLongRunningTask() {
        if (lock.tryLock()) {
            jobLock.getLock().lock();
            try {
                if (!isRunning) {
                    isRunning = true;
                    log.info("ChapterAnalyser task started.");
                    processChapters();
                    log.info("ChapterAnalyser task completed.");
                } else {
                    log.info("ChapterAnalyser task is already in progress.");
                }
            } finally {
                jobLock.getLock().unlock();
                lock.unlock();
                isRunning = false;
            }
        } else {
            log.info("ChapterAnalyser task is already locked.");
        }
    }

    private void processChapters() {
        for (Chapter chapter : chapterRepo.findAll()) {
            if (cancel)
                return;
            try {
                ChapterWrapper wrapper = fileService.getChapterWrapper(new File(chapter.getFullPath()).toPath());
                Title title = titleRepo.findByFullPath(chapter.getPath());
                String metadata = title.getName() + ", " + chapter.getName();
                if (wrapper.hasFile("description.txt")) {
                    String dec = metadata + ", " + new String(wrapper.getFile("description.txt").readAllBytes());
                    if (dec.equals(chapter.getDescription()))
                        continue; // No change, skip saving
                    log.info("Updating description for chapter: " + chapter.getFullPath());
                    chapter.setDescription(dec);
                    chapterRepo.save(chapter);
                } else  {
                    chapter.setDescription(metadata);
                    chapterRepo.save(chapter);
                }
            } catch (Exception ex) {
                log.log(Level.WARNING, "Error while processing chapter " + chapter.getFullPath(), ex);
                return; // Stop processing on error
            }
        }
    }

    @Override
    public void destroy() throws Exception {
        cancel = true;
    }

}