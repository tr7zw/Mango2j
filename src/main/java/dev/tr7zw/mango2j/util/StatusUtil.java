package dev.tr7zw.mango2j.util;

import dev.tr7zw.mango2j.jobs.*;
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

    public String getScanStatus() {
        if (fileScanner.isRunning()) {
            return "Scanning Files...";
        } else if (thumbnailGenerator.isRunning()) {
            return "Generating Thumbnails...";
        } else if (chapterAnalyser.isRunning()) {
            return "Analysing Chapters...";
        } else if (imageCounter.isRunning()) {
            return "Counting Images...";
        } else {
            return "Idle";
        }
    }
}
