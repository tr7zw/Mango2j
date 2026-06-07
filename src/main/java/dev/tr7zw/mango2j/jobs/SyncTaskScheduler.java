package dev.tr7zw.mango2j.jobs;

import dev.tr7zw.mango2j.service.InternalTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SyncTaskScheduler {

    public static final String TASK_SCAN_FILES = "sync.scan-files";
    public static final String TASK_THUMBNAILS = "sync.thumbnails";
    public static final String TASK_IMAGE_COUNTER = "sync.image-counter";
    public static final String TASK_CHAPTER_ANALYSER = "sync.chapter-analyser";
    public static final String TASK_TITLE_ANALYSER = "sync.title-analyser";

    @Autowired
    private InternalTaskService taskService;

    @Scheduled(fixedDelay = 3600000)
    public void enqueueScheduledSyncPipeline() {
        enqueueSyncPipeline();
    }

    public void enqueueSyncPipeline() {
        taskService.clearInternalTasks();
        taskService.enqueue(TASK_SCAN_FILES, Map.of(), true);
        taskService.enqueue(TASK_THUMBNAILS, Map.of(), true);
        taskService.enqueue(TASK_IMAGE_COUNTER, Map.of(), true);
        taskService.enqueue(TASK_CHAPTER_ANALYSER, Map.of(), true);
        taskService.enqueue(TASK_TITLE_ANALYSER, Map.of(), true);
    }
}
