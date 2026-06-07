package dev.tr7zw.mango2j.jobs;

import java.io.File;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
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
    private JobLock jobLock;
    @Autowired
    private ChapterRepository chapterRepo;
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
                    log.info("ImageCounter task started.");
                    processChapters(listener);
                    log.info("ImageCounter task completed.");
                } else {
                    log.info("ImageCounter task is already in progress.");
                }
            } finally {
                jobLock.getLock().unlock();
                lock.unlock();
                isRunning = false;
            }
        } else {
            log.info("ImageCounter task is already locked.");
        }
    }

    private void processChapters(JobProgressListener listener) {
        java.util.List<Chapter> chapters = chapterRepo.findAll();
        int total = chapters.size();
        int current = 0;
        if (listener != null) {
            listener.onProgress(0, total, "Counting images");
        }
        for (Chapter chapter : chapters) {
            if (cancel)
                return;
            try (ChapterWrapper chapterWrapper = fileService.getChapterWrapper(new File(chapter.getFullPath()).toPath())) {
                Integer size = chapterWrapper.getFilesTyped().size();
                Integer old = chapter.getPageCount();
                if (!Objects.equals(size, old) || chapter.getViews() == null) {
                    chapter.setPageCount(chapterWrapper.getFilesTyped().size());
                    if (chapter.getViews() == null) {
                        chapter.setViews(0);
                    }
                    // Reset fileSize when page count changes
                    if (!Objects.equals(size, old)) {
                        chapter.setFileSize(null);
                    }
                    chapterRepo.save(chapter);
                    log.log(Level.INFO,
                            "Updated chapter size of " + chapter.getFullPath() + " from " + old + " to " + size);
                }
            } catch (Exception ex) {
                log.log(Level.WARNING, "Error while processing chapter " + chapter.getFullPath(), ex);
            } finally {
                current++;
                if (listener != null) {
                    listener.onProgress(current, total, "Counting images");
                }
            }
        }
    }

    @Override
    public void destroy() throws Exception {
        cancel = true;
    }

}