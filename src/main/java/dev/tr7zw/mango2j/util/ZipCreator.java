package dev.tr7zw.mango2j.util;

import lombok.*;

import javax.imageio.*;
import java.awt.image.*;
import java.io.*;
import java.util.zip.*;

public class ZipCreator implements AutoCloseable {

    private ZipOutputStream out;

    public ZipCreator(File outputFile) throws IOException {
        out = new ZipOutputStream(new FileOutputStream(outputFile));
    }

    @Synchronized
    public void addFile(String name, InputStream stream) throws IOException {
        out.putNextEntry(new ZipEntry(name));
        // buffer size
        byte[] b = new byte[1024];
        int count;

        while ((count = stream.read(b)) > 0) {
            out.write(b, 0, count);
        }
        stream.close();
    }

    @Synchronized
    public void addFile(String name, byte[] data) throws IOException {
        out.putNextEntry(new ZipEntry(name));
        out.write(data);
    }

    @Synchronized
    public void addFile(String name, BufferedImage image) throws IOException {
        out.putNextEntry(new ZipEntry(name));
        ImageIO.write(image, "PNG", out);
    }

    @Synchronized
    @Override
    public void close() throws Exception {
        out.close();
    }

}
