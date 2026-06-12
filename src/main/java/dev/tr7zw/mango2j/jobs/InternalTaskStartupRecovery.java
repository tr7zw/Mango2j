package dev.tr7zw.mango2j.jobs;

import dev.tr7zw.mango2j.db.InternalTask;
import dev.tr7zw.mango2j.db.InternalTaskRepository;
import dev.tr7zw.mango2j.db.InternalTaskStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class InternalTaskStartupRecovery {

    @Autowired
    private InternalTaskRepository taskRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void recoverRunningTasks() {
        List<InternalTask> runningTasks = taskRepository.findByStatusOrderByIdAsc(InternalTaskStatus.RUNNING);
        if (runningTasks.isEmpty()) {
            return;
        }
        for (InternalTask task : runningTasks) {
            task.setStatus(InternalTaskStatus.QUEUED);
            task.setProgressCurrent(0);
            task.setProgressTotal(0);
            task.setMessage("Queued after restart");
            task.setError(null);
        }
        taskRepository.saveAll(runningTasks);
    }
}
