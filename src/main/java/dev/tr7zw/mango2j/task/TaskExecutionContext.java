package dev.tr7zw.mango2j.task;

import dev.tr7zw.mango2j.db.InternalTask;
import dev.tr7zw.mango2j.service.InternalTaskService;

import java.util.Map;

public class TaskExecutionContext {

    private final InternalTask task;
    private final InternalTaskService taskService;

    public TaskExecutionContext(InternalTask task, InternalTaskService taskService) {
        this.task = task;
        this.taskService = taskService;
    }

    public InternalTask getTask() {
        return task;
    }

    public Map<String, Object> getPayload() {
        return taskService.readPayload(task);
    }

    public void progress(int current, int total, String message) {
        taskService.updateProgress(task.getId(), current, total, message);
    }

    public void finish(String message) {
        taskService.markFinished(task.getId(), message);
    }

    public void fail(String error) {
        taskService.markFailed(task.getId(), error);
    }
}
