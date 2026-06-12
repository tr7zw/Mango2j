package dev.tr7zw.mango2j.task.processor;

import dev.tr7zw.mango2j.task.InternalTaskProcessor;
import dev.tr7zw.mango2j.task.TaskExecutionContext;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;

@Component
public class UploadZipTaskProcessor implements InternalTaskProcessor {

    @Override
    public String getKey() {
        return "upload.zip";
    }

    @Override
    public String getDisplayName() {
        return "Upload ZIP to Folder";
    }

    @Override
    public String getFormFragment() {
        return "fragments/task-form-upload-zip :: form";
    }

    @Override
    public void process(TaskExecutionContext context) throws Exception {
        Map<String, Object> payload = context.getPayload();
        String stagedFile = String.valueOf(payload.getOrDefault("stagedFile", "")).trim();
        String targetDir = String.valueOf(payload.getOrDefault("targetDir", "")).trim();
        String uploadedName = String.valueOf(payload.getOrDefault("uploadedFileName", "")).trim();
        String fileName = String.valueOf(payload.getOrDefault("fileName", "")).trim();
        if (fileName.isBlank()) {
            fileName = uploadedName;
        }
        fileName = Path.of(fileName).getFileName().toString();

        if (stagedFile.isBlank() || targetDir.isBlank() || fileName.isBlank()) {
            throw new IllegalArgumentException("uploadFile, targetDir and fileName are required");
        }

        String lower = fileName.toLowerCase(Locale.ROOT);
        if (!(lower.endsWith(".zip") || lower.endsWith(".cbz"))) {
            throw new IllegalArgumentException("File must be .zip or .cbz");
        }

        Path source = Path.of(stagedFile);
        Path target = Path.of(targetDir).resolve(fileName).normalize();

        context.progress(0, 1, "Placing file " + fileName);
        Files.createDirectories(target.getParent());
        if (!Files.exists(source)) {
            throw new IllegalArgumentException("Uploaded file is missing: " + source);
        }
        if (Files.exists(target)) {
            throw new IllegalArgumentException("Target file already exists: " + target);
        }
        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);

        if (source.getParent().toFile().list().length == 0) {
            Files.delete(source.getParent());
        }

        context.progress(1, 1, "Saved to " + target);
        context.finish("Upload complete");
    }
}
