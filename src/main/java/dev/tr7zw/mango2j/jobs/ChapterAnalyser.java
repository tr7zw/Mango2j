package dev.tr7zw.mango2j.jobs;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import dev.tr7zw.mango2j.db.Chapter;
import dev.tr7zw.mango2j.db.ChapterRepository;
import dev.tr7zw.mango2j.service.AiService;
import lombok.Getter;
import lombok.extern.java.Log;

@Component
@Log
public class ChapterAnalyser implements DisposableBean {

    @Autowired
    private JobLock jobLock;
    @Autowired
    private AiService aiService;
    @Autowired
    private ChapterRepository chapterRepo;
    private final Lock lock = new ReentrantLock();
    @Getter
    private boolean isRunning = false;
    private boolean cancel = false;

    @Async
    public void executeLongRunningTask() {
        if (!aiService.available()) {
            log.warning("AI Service is not available, skipping ChapterAnalyser task.");
            return;
        }
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
        for (Chapter chapter : chapterRepo.findReadChaptersWithoutDescription()) {
            if (cancel)
                return;
            try {
                if (chapter.getDescription() == null || chapter.getDescription().isEmpty()) {
                    log.info("Generating description for chapter: " + chapter.getFullPath());
                    String description = aiService.generateDescription(chapter);
                    if (description != null && !description.isEmpty()) {
                        chapter.setDescription(description);
                        chapter.setEmbedding(aiService.embed(description));
                        chapterRepo.save(chapter);
                    }
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