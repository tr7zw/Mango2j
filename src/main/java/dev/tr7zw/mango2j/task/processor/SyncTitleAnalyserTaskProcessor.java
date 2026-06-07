package dev.tr7zw.mango2j.task.processor;

import dev.tr7zw.mango2j.jobs.SyncTaskScheduler;
import dev.tr7zw.mango2j.jobs.TitleAnalyser;
import dev.tr7zw.mango2j.task.InternalTaskProcessor;
import dev.tr7zw.mango2j.task.TaskExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SyncTitleAnalyserTaskProcessor implements InternalTaskProcessor {

    @Autowired
    private TitleAnalyser titleAnalyser;

    @Override
    public String getKey() {
        return SyncTaskScheduler.TASK_TITLE_ANALYSER;
    }

    @Override
    public String getDisplayName() {
        return "Sync: Analyse Titles";
    }

    @Override
    public boolean isInternalOnly() {
        return true;
    }

    @Override
    public void process(TaskExecutionContext context) {
        int total = titleAnalyser.getPlannedCount();
        context.progress(0, total, "Analysing titles");
        titleAnalyser.executeLongRunningTask((current, max, msg) -> context.progress(current, max, msg));
        context.progress(total, total, "Title analysis complete");
        context.finish("Title analysis complete");
    }
}
