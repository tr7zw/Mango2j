package dev.tr7zw.mango2j.util;

import dev.tr7zw.mango2j.jobs.*;
import dev.tr7zw.mango2j.service.InternalTaskService;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.*;

@Component
public class StatusUtil {

    @Autowired
    private FileScanner fileScanner;
    @Autowired
    private ThumbnailGenerator thumbnailGenerator;
    @Autowired
    private ChapterAnalyser chapterAnalyser;
    @Autowired
    private ImageCounter imageCounter;
    @Autowired
    private TitleAnalyser titleAnalyser;
    @Autowired
    private InternalTaskService internalTaskService;

    public String getScanStatus() {
        String taskStatus = internalTaskService.getRunningStatusText();
        if (!"Idle".equals(taskStatus)) {
            return taskStatus;
        }
        if (fileScanner.isRunning()) {
            return "Scanning Files...";
        } else if (thumbnailGenerator.isRunning()) {
            return "Generating Thumbnails...";
        } else if (chapterAnalyser.isRunning()) {
            return "Analysing Chapters...";
        } else if (imageCounter.isRunning()) {
            return "Counting Images...";
        } else if (titleAnalyser.isRunning()) {
            return "Analysing Titles...";
        } else {
            return "Idle";
        }
    }
}
