package dev.tr7zw.mango2j.task.processor;

import dev.tr7zw.mango2j.jobs.ChapterAnalyser;
import dev.tr7zw.mango2j.jobs.SyncTaskScheduler;
import dev.tr7zw.mango2j.task.InternalTaskProcessor;
import dev.tr7zw.mango2j.task.TaskExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SyncChapterAnalyserTaskProcessor implements InternalTaskProcessor {

    @Autowired
    private ChapterAnalyser chapterAnalyser;

    @Override
    public String getKey() {
        return SyncTaskScheduler.TASK_CHAPTER_ANALYSER;
    }

    @Override
    public String getDisplayName() {
        return "Sync: Analyse Chapters";
    }

    @Override
    public boolean isInternalOnly() {
        return true;
    }

    @Override
    public void process(TaskExecutionContext context) {
        int total = chapterAnalyser.getPlannedCount();
        context.progress(0, total, "Analysing chapters");
        chapterAnalyser.executeLongRunningTask((current, max, msg) -> context.progress(current, max, msg));
        context.progress(total, total, "Chapter analysis complete");
        context.finish("Chapter analysis complete");
    }
}
