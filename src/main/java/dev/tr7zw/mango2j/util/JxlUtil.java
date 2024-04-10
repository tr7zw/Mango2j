package dev.tr7zw.mango2j.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

public class JxlUtil {

    private static final File djxl = new File("/usr/bin/djxl");

    public static InputStream jxlToPng(InputStream imageStream) throws Exception {
        if (!djxl.exists()) {
            return imageStream;
        }
        File tmpFile = File.createTempFile("imageconvert", ".jxl");
        try (FileOutputStream out = new FileOutputStream(tmpFile)) {
            imageStream.transferTo(out);
        }
        File targetFile = File.createTempFile("imageconvert", ".png");
        ProcessBuilder builder = new ProcessBuilder(djxl.getAbsolutePath(), tmpFile.getAbsolutePath(),
                targetFile.getAbsolutePath());
        builder.redirectErrorStream(true);
        Process p = builder.start();
        while (p.isAlive()) {
            Thread.sleep(10);
        }
        tmpFile.delete();
        targetFile.deleteOnExit();
        return new FileInputStream(targetFile);
    }

}
