package dev.tr7zw.mango2j.jobs;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import dev.tr7zw.mango2j.db.*;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.extern.java.Log;

@Component
@Log
public class TitleAnalyser implements DisposableBean {

    @Autowired
    private JobLock jobLock;
    @Autowired
    private TitleRepository titleRepo;
    @Autowired
    private ChapterRepository chapterRepo;
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
                    log.info("TitleAnalyser task started.");
                    analyzeTitles();
                    log.info("TitleAnalyser task completed.");
                } else {
                    log.info("TitleAnalyser task is already in progress.");
                }
            } finally {
                jobLock.getLock().unlock();
                lock.unlock();
                isRunning = false;
            }
        } else {
            log.info("TitleAnalyser task is already locked.");
        }
    }

    private void analyzeTitles() {
        for (Title title : titleRepo.findAll()) {
            if (cancel)
                return;
            analyzeTitleAndChildren(title);
        }
    }

    private void analyzeTitleAndChildren(Title title) {
        // Analyze direct chapters
        int totalViews = 0;
        int chapterCount = 0;
        long totalFileSize = 0;
        java.time.Instant newestTime = null;

        // Get direct chapters
        List<Chapter> directChapters = chapterRepo.findByPath(title.getFullPath());
        totalViews = directChapters.stream()
                .mapToInt(c -> c.getViews() == null ? 0 : c.getViews())
                .sum();
        totalFileSize = directChapters.stream()
                .mapToLong(c -> c.getFileSize() == null ? 0 : c.getFileSize())
                .sum();
        chapterCount = directChapters.size();

        // Find newest timestamp from direct chapters
        Optional<java.time.Instant> newestDirectTime = directChapters.stream()
                .map(Chapter::getLastView)
                .filter(Objects::nonNull)
                .max(java.time.Instant::compareTo);

        // Recursively analyze child titles
        List<Title> childTitles = titleRepo.findByPath(title.getFullPath());
        for (Title child : childTitles) {
            analyzeTitleAndChildren(child);
            // Add child stats to parent
            totalViews += child.getTotalViews() == null ? 0 : child.getTotalViews();
            chapterCount += child.getChapterCount() == null ? 0 : child.getChapterCount();
            totalFileSize += child.getFileSize() == null ? 0 : child.getFileSize();

            // Update newest time if child has a newer one
            if (child.getNewestChapterTime() != null) {
                if (newestTime == null || child.getNewestChapterTime().isAfter(newestTime)) {
                    newestTime = child.getNewestChapterTime();
                }
            }
        }

        // Use newest from direct chapters if it's newer than what we found in children
        if (newestDirectTime.isPresent()) {
            if (newestTime == null || newestDirectTime.get().isAfter(newestTime)) {
                newestTime = newestDirectTime.get();
            }
        }

        // Update title with calculated stats
        title.setTotalViews(totalViews);
        title.setChapterCount(chapterCount);
        title.setFileSize(totalFileSize > 0 ? totalFileSize : null);
        title.setNewestChapterTime(newestTime);
        titleRepo.save(title);
    }

    @Override
    public void destroy() throws Exception {
        cancel = true;
    }

}
