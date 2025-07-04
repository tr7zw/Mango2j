package dev.tr7zw.mango2j.jobs;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageOutputStream;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import dev.tr7zw.mango2j.db.Chapter;
import dev.tr7zw.mango2j.db.ChapterRepository;
import dev.tr7zw.mango2j.service.ChapterWrapper;
import dev.tr7zw.mango2j.service.FileService;
import dev.tr7zw.mango2j.util.JxlUtil;
import dev.tr7zw.mango2j.util.WebpUtil;
import lombok.Getter;
import lombok.extern.java.Log;
import net.coobird.thumbnailator.Thumbnails;

@Component
@Log
public class ThumbnailGenerator implements DisposableBean {

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

    @Async
    public void executeLongRunningTask() {
        if (lock.tryLock()) {
            jobLock.getLock().lock();
            try {
                if (!isRunning) {
                    isRunning = true;
                    log.info("Thumbnail task started.");
                    processChapters();
                    log.info("Thumbnail task completed.");
                } else {
                    log.info("Thumbnail task is already in progress.");
                }
            } finally {
                jobLock.getLock().unlock();
                lock.unlock();
                isRunning = false;
            }
        } else {
            log.info("Thumbnail task is already locked.");
        }
    }

    private void processChapters() {
        for (Chapter chapter : chapterRepo.findByThumbnailIsNull()) {
            if (cancel)
                return;
            try {
                ChapterWrapper chapterWrapper = fileService.getChapterWrapper(new File(chapter.getFullPath()).toPath());
                int id = 0;
                if (!chapterWrapper.hasFile(id)) {
                    log.info("No images in file " + chapter.getFullPath());
                    continue;
                }
                BufferedImage image = null;
                if ("jxl".equals(chapterWrapper.getFileType(id))) {
                    image = ImageIO.read(JxlUtil.jxlToPng(chapterWrapper.getInputStream(id)));
                } else {
                    image = ImageIO.read(chapterWrapper.getInputStream(id));
                }
                if (image == null) {
                    log.info("Got null for image " + id + "(" + chapterWrapper.getFileType(id) + ") in file "
                            + chapter.getFullPath());
                    continue;
                }
                BufferedImage thumbnail = Thumbnails.of(image).height(300).asBufferedImage();
                ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
                WebpUtil.writeAsWebp(new MemoryCacheImageOutputStream(outBuffer), thumbnail);
                chapter.setThumbnail(outBuffer.toByteArray());
                chapterRepo.save(chapter);
                log.info("Generated thumbnail for " + chapter.getFullPath() + ". Size: " + outBuffer.size() + "bytes");
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