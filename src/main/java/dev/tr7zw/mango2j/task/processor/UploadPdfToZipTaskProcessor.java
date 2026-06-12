package dev.tr7zw.mango2j.task.processor;

import dev.tr7zw.mango2j.service.PdfToZipConversionService;
import dev.tr7zw.mango2j.task.InternalTaskProcessor;
import dev.tr7zw.mango2j.task.TaskExecutionContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;

@Component
public class UploadPdfToZipTaskProcessor implements InternalTaskProcessor {

    @Autowired
    private ObjectProvider<PdfToZipConversionService> conversionServiceProvider;

    @Override
    public String getKey() {
        return "upload.pdf-to-zip";
    }

    @Override
    public String getDisplayName() {
        return "Upload PDF and Convert to ZIP";
    }

    @Override
    public String getFormFragment() {
        return "fragments/task-form-upload-pdf :: form";
    }

    @Override
    public void process(TaskExecutionContext context) throws Exception {
        Map<String, Object> payload = context.getPayload();
        String stagedFile = String.valueOf(payload.getOrDefault("stagedFile", "")).trim();
        String targetDir = String.valueOf(payload.getOrDefault("targetDir", "")).trim();
        String uploadedName = String.valueOf(payload.getOrDefault("uploadedFileName", "")).trim();
        String pdfFileName = String.valueOf(payload.getOrDefault("pdfFileName", "")).trim();
        if (pdfFileName.isBlank()) {
            pdfFileName = uploadedName;
        }
        String zipFileName = String.valueOf(payload.getOrDefault("zipFileName", "")).trim();
        if (zipFileName.isBlank()) {
            zipFileName = deriveZipName(pdfFileName);
        }
        pdfFileName = Path.of(pdfFileName).getFileName().toString();
        zipFileName = Path.of(zipFileName).getFileName().toString();

        if (stagedFile.isBlank() || targetDir.isBlank() || pdfFileName.isBlank()) {
            throw new IllegalArgumentException("uploadFile, targetDir and pdfFileName are required");
        }
        System.out.println("Processing PDF upload: " + stagedFile + " -> " + targetDir + "/" + pdfFileName + " -> " + zipFileName);

        String lowerPdf = pdfFileName.toLowerCase(Locale.ROOT);
        if (!lowerPdf.endsWith(".pdf")) {
            throw new IllegalArgumentException("PDF file must end with .pdf");
        }
        String lowerZip = zipFileName.toLowerCase(Locale.ROOT);
        if (!(lowerZip.endsWith(".zip") || lowerZip.endsWith(".cbz"))) {
            throw new IllegalArgumentException("ZIP output must end with .zip or .cbz");
        }

        Path source = Path.of(stagedFile);
        Path targetFolder = Path.of(targetDir).normalize();
        Path targetPdf = targetFolder.resolve(pdfFileName).normalize();
        Path targetZip = targetFolder.resolve(zipFileName).normalize();

        context.progress(0, 2, "Placing PDF " + pdfFileName);
        Files.createDirectories(targetFolder);
        if (!Files.exists(source)) {
            throw new IllegalArgumentException("Uploaded file is missing: " + source);
        }
        if (Files.exists(targetPdf)) {
            throw new IllegalArgumentException("Target PDF already exists: " + targetPdf);
        }
        if (Files.exists(targetZip)) {
            throw new IllegalArgumentException("Target ZIP already exists: " + targetZip);
        }
        Files.move(source, targetPdf, StandardCopyOption.REPLACE_EXISTING);

        if (source.getParent().toFile().list().length == 0) {
            Files.delete(source.getParent());
        }

        PdfToZipConversionService conversionService = conversionServiceProvider.getIfAvailable();
        if (conversionService == null) {
            throw new IllegalStateException("No PdfToZipConversionService bean configured. Please wire your conversion logic.");
        }

        context.progress(1, 2, "Converting PDF to ZIP");
        conversionService.convertPdfToZip(targetPdf, targetZip);
        context.progress(2, 2, "Saved to " + targetZip);
        targetPdf.toFile().delete();
        context.finish("PDF conversion complete");
    }

    private String deriveZipName(String pdfName) {
        if (pdfName == null || pdfName.isBlank()) {
            return "converted.zip";
        }
        int idx = pdfName.lastIndexOf('.');
        if (idx <= 0) {
            return pdfName + ".zip";
        }
        return pdfName.substring(0, idx) + ".zip";
    }
}
