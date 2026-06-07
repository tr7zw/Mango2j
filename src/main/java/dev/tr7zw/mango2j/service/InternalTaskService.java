package dev.tr7zw.mango2j.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.tr7zw.mango2j.db.InternalTask;
import dev.tr7zw.mango2j.db.InternalTaskRepository;
import dev.tr7zw.mango2j.db.InternalTaskStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class InternalTaskService {

    @Autowired
    private InternalTaskRepository taskRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public synchronized InternalTask enqueue(String processorKey, Map<String, Object> payload, boolean internalTask) {
        InternalTask task = new InternalTask();
        task.setProcessorKey(processorKey);
        task.setInternalTask(internalTask);
        task.setStatus(InternalTaskStatus.QUEUED);
        task.setMessage("Queued");
        try {
            task.setPayloadJson(objectMapper.writeValueAsString(payload == null ? Map.of() : payload));
        } catch (Exception e) {
            task.setPayloadJson("{}");
        }
        return taskRepository.save(task);
    }

    public synchronized InternalTask claimNextQueued() {
        InternalTask task = taskRepository.findFirstByStatusOrderByIdAsc(InternalTaskStatus.QUEUED);
        if (task == null) {
            return null;
        }
        task.setStatus(InternalTaskStatus.RUNNING);
        task.setMessage("Running");
        return taskRepository.save(task);
    }

    public synchronized void updateProgress(Long id, int current, int total, String message) {
        Optional<InternalTask> opt = taskRepository.findById(id);
        if (opt.isEmpty()) {
            return;
        }
        InternalTask task = opt.get();
        task.setProgressCurrent(Math.max(0, current));
        task.setProgressTotal(Math.max(0, total));
        if (message != null) {
            task.setMessage(message);
        }
        taskRepository.save(task);
    }

    public synchronized void markFinished(Long id, String message) {
        Optional<InternalTask> opt = taskRepository.findById(id);
        if (opt.isEmpty()) {
            return;
        }
        InternalTask task = opt.get();
        task.setStatus(InternalTaskStatus.FINISHED);
        if (message != null) {
            task.setMessage(message);
        }
        if (task.getProgressTotal() != null && task.getProgressTotal() > 0) {
            task.setProgressCurrent(task.getProgressTotal());
        }
        taskRepository.save(task);
    }

    public synchronized void markFailed(Long id, String error) {
        Optional<InternalTask> opt = taskRepository.findById(id);
        if (opt.isEmpty()) {
            return;
        }
        InternalTask task = opt.get();
        task.setStatus(InternalTaskStatus.FAILED);
        task.setError(error == null ? "Unknown error" : error);
        task.setMessage("Failed");
        taskRepository.save(task);
    }

    public synchronized void retry(Long id) {
        Optional<InternalTask> opt = taskRepository.findById(id);
        if (opt.isEmpty()) {
            return;
        }
        InternalTask task = opt.get();
        task.setStatus(InternalTaskStatus.QUEUED);
        task.setProgressCurrent(0);
        task.setProgressTotal(0);
        task.setMessage("Queued");
        task.setError(null);
        taskRepository.save(task);
    }

    public synchronized void delete(Long id) {
        taskRepository.deleteById(id);
    }

    public synchronized void clearFinishedAndFailed() {
        List<InternalTask> old = taskRepository.findByStatusInOrderByIdDesc(List.of(
                InternalTaskStatus.FINISHED,
                InternalTaskStatus.FAILED,
                InternalTaskStatus.CANCELED
        ));
        taskRepository.deleteAll(old);
    }

    @Transactional
    public synchronized void clearInternalTasks() {
        taskRepository.deleteByInternalTaskTrue();
    }

    public List<InternalTask> latest() {
        return taskRepository.findTop300ByOrderByIdDesc();
    }

    public long countByStatus(InternalTaskStatus status) {
        return latest().stream().filter(t -> t.getStatus() == status).count();
    }

    public String getRunningStatusText() {
        InternalTask task = taskRepository.findFirstByStatusOrderByIdDesc(InternalTaskStatus.RUNNING);
        if (task == null) {
            InternalTask queued = taskRepository.findFirstByStatusOrderByIdAsc(InternalTaskStatus.QUEUED);
            if (queued != null) {
                return "Queued: " + queued.getProcessorKey();
            }
            return "Idle";
        }
        int current = task.getProgressCurrent() == null ? 0 : task.getProgressCurrent();
        int total = task.getProgressTotal() == null ? 0 : task.getProgressTotal();
        if (total > 0) {
            return task.getProcessorKey() + " (" + current + "/" + total + ")";
        }
        return task.getProcessorKey();
    }

    public Map<String, Object> readPayload(InternalTask task) {
        try {
            if (task.getPayloadJson() == null || task.getPayloadJson().isBlank()) {
                return new HashMap<>();
            }
            return objectMapper.readValue(task.getPayloadJson(), new TypeReference<>() {});
        } catch (Exception e) {
            return new HashMap<>();
        }
    }
}
