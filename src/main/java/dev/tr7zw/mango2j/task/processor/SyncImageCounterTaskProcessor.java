package dev.tr7zw.mango2j.task.processor;

import dev.tr7zw.mango2j.jobs.ImageCounter;
import dev.tr7zw.mango2j.jobs.SyncTaskScheduler;
import dev.tr7zw.mango2j.task.InternalTaskProcessor;
import dev.tr7zw.mango2j.task.TaskExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SyncImageCounterTaskProcessor implements InternalTaskProcessor {

    @Autowired
    private ImageCounter imageCounter;

    @Override
    public String getKey() {
        return SyncTaskScheduler.TASK_IMAGE_COUNTER;
    }

    @Override
    public String getDisplayName() {
        return "Sync: Count Images";
    }

    @Override
    public boolean isInternalOnly() {
        return true;
    }

    @Override
    public void process(TaskExecutionContext context) {
        int total = imageCounter.getPlannedCount();
        context.progress(0, total, "Counting images");
        imageCounter.executeLongRunningTask((current, max, msg) -> context.progress(current, max, msg));
        context.progress(total, total, "Image counting complete");
        context.finish("Image counting complete");
    }
}
