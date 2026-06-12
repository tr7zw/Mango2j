package dev.tr7zw.mango2j.task.processor;

import dev.tr7zw.mango2j.task.InternalTaskProcessor;
import dev.tr7zw.mango2j.task.TaskExecutionContext;
import dev.tr7zw.mango2j.task.TaskFormField;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

@Component
public class DownloadFileTaskProcessor implements InternalTaskProcessor {

    @Override
    public String getKey() {
        return "download.file";
    }

    @Override
    public String getDisplayName() {
        return "Download File";
    }

    @Override
    public List<TaskFormField> getFormFields() {
        return List.of(
                new TaskFormField("url", "Source URL", "url", "https://example.org/file.cbz", true),
                new TaskFormField("targetDir", "Target Folder", "text", "Release that witch", true),
                new TaskFormField("fileName", "File Name", "text", "chapter.cbz", true)
        );
    }

    @Override
    public String getFormFragment() {
        return "fragments/task-form-download :: form";
    }

    @Override
    public void process(TaskExecutionContext context) throws Exception {
        Map<String, Object> payload = context.getPayload();
        String url = String.valueOf(payload.getOrDefault("url", "")).trim();
        String targetDir = String.valueOf(payload.getOrDefault("targetDir", "")).trim();
        String fileName = String.valueOf(payload.getOrDefault("fileName", "")).trim();
        if (url.isBlank() || targetDir.isBlank() || fileName.isBlank()) {
            throw new IllegalArgumentException("url, targetDir and fileName are required");
        }
        context.progress(0, 1, "Downloading " + fileName);
        Path target = Path.of(targetDir).resolve(fileName);
        Files.createDirectories(target.getParent());
        if (target.toFile().exists()) {
            context.fail("File already exists: " + target);
            return;
        }
        try (InputStream in = new URL(url).openStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        context.progress(1, 1, "Saved to " + target);
        context.finish("Download complete");
    }
}
