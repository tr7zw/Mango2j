package dev.tr7zw.mango2j.controller;

import dev.tr7zw.mango2j.Settings;
import dev.tr7zw.mango2j.db.InternalTaskStatus;
import dev.tr7zw.mango2j.service.InternalTaskProcessorRegistry;
import dev.tr7zw.mango2j.service.InternalTaskService;
import dev.tr7zw.mango2j.task.InternalTaskProcessor;
import dev.tr7zw.mango2j.util.StatusUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Controller
public class TaskController {

    @Autowired
    private InternalTaskService taskService;

    @Autowired
    private InternalTaskProcessorRegistry processorRegistry;

    @Autowired
    private StatusUtil statusUtil;

    @Autowired
    private Settings settings;

    @GetMapping("/tasks")
    public String tasksPage(Model model) {
        List<InternalTaskProcessor> processors = processorRegistry.userVisible();
        model.addAttribute("processors", processors);
        String selectedKey = processors.isEmpty() ? "" : processors.getFirst().getKey();
        model.addAttribute("selectedKey", selectedKey);
        return "tasks";
    }

    @GetMapping("/tasks/form")
    public String tasksForm(@RequestParam(name = "key", required = false) String key, Model model) {
        Optional<InternalTaskProcessor> selected = processorRegistry.find(key == null ? "" : key);
        if (selected.isEmpty()) {
            model.addAttribute("selectedProcessor", null);
            return "fragments/task-form :: form";
        }
        InternalTaskProcessor processor = selected.get();
        model.addAttribute("selectedProcessor", processor);
        processor.getFormModel().forEach(model::addAttribute);
        return processor.getFormFragment();
    }

    @GetMapping("/tasks/form/download/folder-picker")
    public String downloadFolderPicker(@RequestParam(name = "current", required = false) String current, Model model) {
        Path base = settings.getBaseDir().toPath().toAbsolutePath().normalize();
        Path selected = resolveFolder(base, current);
        String currentRelative = toRelative(base, selected);
        String parentRelative = "";
        if (!currentRelative.isBlank()) {
            int idx = currentRelative.lastIndexOf('/');
            parentRelative = idx <= 0 ? "" : currentRelative.substring(0, idx);
        }

        List<FolderOption> directories = new ArrayList<>();
        try (var stream = Files.list(selected)) {
            directories = stream
                    .filter(Files::isDirectory)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)))
                    .limit(500)
                    .map(path -> new FolderOption(path.getFileName().toString(), toRelative(base, path)))
                    .toList();
        } catch (IOException ignored) {
            directories = List.of();
        }

        model.addAttribute("basePath", base.toString());
        model.addAttribute("currentRelative", currentRelative);
        model.addAttribute("currentAbsolute", selected.toString());
        model.addAttribute("canGoUp", !currentRelative.isBlank());
        model.addAttribute("parentRelative", parentRelative);
        model.addAttribute("directories", directories);
        return "fragments/task-folder-picker :: picker";
    }

    private Path resolveFolder(Path base, String current) {
        String relative = current == null ? "" : current.trim().replace('\\', '/');
        while (relative.startsWith("/")) {
            relative = relative.substring(1);
        }
        Path selected = base;
        if (!relative.isBlank()) {
            selected = base.resolve(relative).normalize();
        }
        if (!selected.startsWith(base)) {
            return base;
        }
        if (Files.isDirectory(selected)) {
            return selected;
        }
        return base;
    }

    private String toRelative(Path base, Path target) {
        if (base.equals(target)) {
            return "";
        }
        return base.relativize(target).toString().replace('\\', '/');
    }

    public record FolderOption(String name, String relativePath) {}

    @GetMapping("/tasks/table")
    public String tasksTable(Model model) {
        model.addAttribute("tasks", taskService.latest());
        long queuedCount = taskService.countByStatus(InternalTaskStatus.QUEUED);
        long runningCount = taskService.countByStatus(InternalTaskStatus.RUNNING);
        long finishedCount = taskService.countByStatus(InternalTaskStatus.FINISHED);
        long failedCount = taskService.countByStatus(InternalTaskStatus.FAILED);

        model.addAttribute("queuedCount", queuedCount);
        model.addAttribute("runningCount", runningCount);
        model.addAttribute("finishedCount", finishedCount);
        model.addAttribute("failedCount", failedCount);
        model.addAttribute("refreshTrigger", (queuedCount > 0 || runningCount > 0) ? "every 500ms" : "every 1s");
        model.addAttribute("scanStatus", statusUtil.getScanStatus());
        return "fragments/tasks-table :: table";
    }

    @PostMapping("/tasks/submit")
    public String submitTask(@RequestParam MultiValueMap<String, String> params, Model model) {
        String key = params.getFirst("key");
        if (key != null && processorRegistry.find(key).isPresent()) {
            Map<String, Object> payload = new HashMap<>();
            params.forEach((k, v) -> {
                if (!"key".equals(k) && !v.isEmpty()) {
                    payload.put(k, v.getFirst());
                }
            });
            taskService.enqueue(key, payload, false);
        }
        return tasksTable(model);
    }

    @PostMapping("/tasks/clear")
    public String clearFinishedAndFailed(Model model) {
        taskService.clearFinishedAndFailed();
        return tasksTable(model);
    }

    @PostMapping("/tasks/{id}/retry")
    public String retryTask(@PathVariable Long id, Model model) {
        taskService.retry(id);
        return tasksTable(model);
    }

    @PostMapping("/tasks/{id}/delete")
    public String deleteTask(@PathVariable Long id, Model model) {
        taskService.delete(id);
        return tasksTable(model);
    }
}
