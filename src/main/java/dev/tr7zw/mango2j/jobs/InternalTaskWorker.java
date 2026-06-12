package dev.tr7zw.mango2j.jobs;

import dev.tr7zw.mango2j.db.InternalTask;
import dev.tr7zw.mango2j.db.InternalTaskStatus;
import dev.tr7zw.mango2j.service.InternalTaskProcessorRegistry;
import dev.tr7zw.mango2j.service.InternalTaskService;
import dev.tr7zw.mango2j.task.InternalTaskProcessor;
import dev.tr7zw.mango2j.task.TaskExecutionContext;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Log
public class InternalTaskWorker {

    @Autowired
    private InternalTaskService taskService;

    @Autowired
    private InternalTaskProcessorRegistry processorRegistry;

    private final AtomicBoolean running = new AtomicBoolean(false);

    @Scheduled(fixedDelay = 1000)
    public void runOneTask() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        try {
            InternalTask task = taskService.claimNextQueued();
            if (task == null) {
                return;
            }
            Optional<InternalTaskProcessor> processorOpt = processorRegistry.find(task.getProcessorKey());
            if (processorOpt.isEmpty()) {
                taskService.markFailed(task.getId(), "No processor registered for key: " + task.getProcessorKey());
                return;
            }
            InternalTaskProcessor processor = processorOpt.get();
            try {
                processor.process(new TaskExecutionContext(task, taskService));
                InternalTask latest = taskService.latest().stream().filter(t -> t.getId().equals(task.getId())).findFirst().orElse(task);
                if (latest.getStatus() == InternalTaskStatus.RUNNING || latest.getStatus() == InternalTaskStatus.QUEUED) {
                    taskService.markFinished(task.getId(), "Done");
                }
            } catch (Exception ex) {
                log.warning("Task failed: " + ex.getMessage());
                taskService.markFailed(task.getId(), ex.getMessage());
            }
        } finally {
            running.set(false);
        }
    }
}
