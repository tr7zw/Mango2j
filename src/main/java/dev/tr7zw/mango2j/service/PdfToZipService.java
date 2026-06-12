package dev.tr7zw.mango2j.service;

import dev.tr7zw.mango2j.util.*;
import org.apache.pdfbox.*;
import org.apache.pdfbox.contentstream.*;
import org.apache.pdfbox.contentstream.operator.*;
import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.graphics.*;
import org.apache.pdfbox.pdmodel.graphics.form.*;
import org.apache.pdfbox.pdmodel.graphics.image.*;
import org.springframework.stereotype.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

@Service
public class PdfToZipService implements PdfToZipConversionService {

    @Override
    public void convertPdfToZip(Path sourcePdf, Path targetZip) throws Exception {
        new PdfToZip().convertPdf(sourcePdf.toFile(), targetZip.toFile());
    }

    public class PdfToZip extends PDFStreamEngine {

        public PdfToZip() throws IOException {
        }

        public int imageNumber = 1;

        private ZipCreator zipCreator;

        public void convertPdf(File pdf, File out) throws Exception {
            PDDocument document = null;
            try (ZipCreator zip = new ZipCreator(out)) {
                this.zipCreator = zip;
                document = Loader.loadPDF(pdf);
                for (PDPage page : document.getPages()) {
                    processPage(page);
                }
            } finally {
                if (document != null) {
                    document.close();
                }
            }
        }

        /**
         * @param operator The operation to perform.
         * @param operands The list of arguments.
         * @throws IOException If there is an error processing the operation.
         */
        @Override
        protected void processOperator(Operator operator, List<COSBase> operands) throws IOException {
            String operation = operator.getName();
            if ("Do".equals(operation)) {
                COSName objectName = (COSName) operands.get(0);
                PDXObject xobject = getResources().getXObject(objectName);
                if (xobject instanceof PDImageXObject) {
                    PDImageXObject image = (PDImageXObject) xobject;

                    // same image to local
                    zipCreator.addFile(imageNumber + ".png", image.getImage());
                    imageNumber++;

                } else if (xobject instanceof PDFormXObject) {
                    PDFormXObject form = (PDFormXObject) xobject;
                    showForm(form);
                }
            } else {
                super.processOperator(operator, operands);
            }
        }
    }
}
