package dev.tr7zw.mango2j.jobs;

import java.io.*;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

import dev.tr7zw.mango2j.db.*;
import dev.tr7zw.mango2j.service.*;
import dev.tr7zw.mango2j.util.EmbeddingSearchUtil;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.extern.java.Log;

@Component
@Log
public class ChapterAnalyser implements DisposableBean {

    @Autowired
    private JobLock jobLock;
    @Autowired
    private EmbeddingModelService embeddingModelService;
    @Autowired
    private ChapterRepository chapterRepo;
    @Autowired
    private ChapterEmbeddingRepository chapterEmbeddingRepo;
    @Autowired
    private TitleRepository titleRepo;
    @Autowired
    private FileService fileService;
    private final Lock lock = new ReentrantLock();
    @Getter
    private boolean isRunning = false;
    private boolean cancel = false;

    public void executeLongRunningTask() {
        executeLongRunningTask(null);
    }

    public int getPlannedCount() {
        return chapterRepo.findAll().size();
    }

    public void executeLongRunningTask(JobProgressListener listener) {
        if (lock.tryLock()) {
            jobLock.getLock().lock();
            try {
                if (!isRunning) {
                    isRunning = true;
                    log.info("ChapterAnalyser task started.");
                    processChapters(listener);
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

    private void processChapters(JobProgressListener listener) {
        java.util.List<Chapter> chapters = chapterRepo.findAll();
        int total = chapters.size();
        int current = 0;
        if (listener != null) {
            listener.onProgress(0, total, "Analysing chapters");
        }
        for (Chapter chapter : chapters) {
            if (cancel)
                return;
            try (ChapterWrapper wrapper = fileService.getChapterWrapper(new File(chapter.getFullPath()).toPath())) {
                Title title = titleRepo.findByFullPath(chapter.getPath());
                String metadata = title.getName() + ", " + chapter.getName();
                boolean updated = false;

                // Fill in last modified time if null
                if (chapter.getLastModified() == null) {
                    chapter.setLastModified(wrapper.getLastModified());
                    updated = true;
                }

                // Fill in file size if null
                if (chapter.getFileSize() == null) {
                    chapter.setFileSize(wrapper.getFileSize());
                    updated = true;
                }

                // Handle description
                if (wrapper.hasFile("description.txt")) {
                    String dec = metadata + ", " + new String(wrapper.getFile("description.txt").readAllBytes());
                    if (!dec.equals(chapter.getDescription())) {
                        log.info("Updating description for chapter: " + chapter.getFullPath());
                        chapter.setDescription(dec);
                        updated = true;
                    }
                } else {
                    if (!metadata.equals(chapter.getDescription())) {
                        chapter.setDescription(metadata);
                        updated = true;
                    }
                }

                boolean hasEmbedding = chapterEmbeddingRepo.findByChapterId(chapter.getId())
                        .map(ChapterEmbedding::getVectorBytes)
                        .filter(bytes -> bytes != null && bytes.length > 0)
                        .isPresent();
                if (updated || !hasEmbedding) {
                    ChapterEmbedding embedding = chapterEmbeddingRepo.findByChapterId(chapter.getId()).orElseGet(ChapterEmbedding::new);
                    embedding.setChapter(chapter);
                    byte[] vectorData = EmbeddingSearchUtil.toBytes(embeddingModelService.embed(chapter.getDescription()));
                    embedding.setVectorBytes(vectorData);
                    chapterEmbeddingRepo.save(embedding);
                }

                if (updated) {
                    chapterRepo.save(chapter);
                }
            } catch (Exception ex) {
                log.log(Level.WARNING, "Error while processing chapter " + chapter.getFullPath(), ex);
                return; // Stop processing on error
            } finally {
                current++;
                if (listener != null) {
                    listener.onProgress(current, total, "Analysing chapters");
                }
            }
        }
    }

    @Override
    public void destroy() throws Exception {
        cancel = true;
    }

}