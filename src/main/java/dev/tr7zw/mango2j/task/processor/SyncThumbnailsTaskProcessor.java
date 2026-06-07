package dev.tr7zw.mango2j.task.processor;

import dev.tr7zw.mango2j.jobs.SyncTaskScheduler;
import dev.tr7zw.mango2j.jobs.ThumbnailGenerator;
import dev.tr7zw.mango2j.task.InternalTaskProcessor;
import dev.tr7zw.mango2j.task.TaskExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SyncThumbnailsTaskProcessor implements InternalTaskProcessor {

    @Autowired
    private ThumbnailGenerator thumbnailGenerator;

    @Override
    public String getKey() {
        return SyncTaskScheduler.TASK_THUMBNAILS;
    }

    @Override
    public String getDisplayName() {
        return "Sync: Generate Thumbnails";
    }

    @Override
    public boolean isInternalOnly() {
        return true;
    }

    @Override
    public void process(TaskExecutionContext context) {
        int total = thumbnailGenerator.getPlannedCount();
        context.progress(0, total, "Generating thumbnails");
        thumbnailGenerator.executeLongRunningTask((current, max, msg) -> context.progress(current, max, msg));
        context.progress(total, total, "Thumbnails complete");
        context.finish("Thumbnails complete");
    }
}
