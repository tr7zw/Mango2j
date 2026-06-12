package dev.tr7zw.mango2j.service;

import java.nio.file.Path;

public interface PdfToZipConversionService {

    void convertPdfToZip(Path sourcePdf, Path targetZip) throws Exception;
}
