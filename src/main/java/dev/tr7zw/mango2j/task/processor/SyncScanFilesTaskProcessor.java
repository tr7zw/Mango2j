package dev.tr7zw.mango2j.task.processor;

import dev.tr7zw.mango2j.jobs.FileScanner;
import dev.tr7zw.mango2j.jobs.SyncTaskScheduler;
import dev.tr7zw.mango2j.task.InternalTaskProcessor;
import dev.tr7zw.mango2j.task.TaskExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SyncScanFilesTaskProcessor implements InternalTaskProcessor {

    @Autowired
    private FileScanner fileScanner;

    @Override
    public String getKey() {
        return SyncTaskScheduler.TASK_SCAN_FILES;
    }

    @Override
    public String getDisplayName() {
        return "Sync: Scan Files";
    }

    @Override
    public boolean isInternalOnly() {
        return true;
    }

    @Override
    public void process(TaskExecutionContext context) {
        context.progress(0, 1, "Scanning files");
        fileScanner.executeLongRunningTask();
        context.progress(1, 1, "Scan complete");
        context.finish("Scan complete");
    }
}
