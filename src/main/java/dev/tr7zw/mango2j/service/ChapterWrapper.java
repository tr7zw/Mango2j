package dev.tr7zw.mango2j.service;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public interface ChapterWrapper {

    int getFiles();
    InputStream getInputStream(int id) throws FileNotFoundException;
    boolean hasFile(int id);
    String getFileType(int id);
    default byte[] getBytes(int id) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try(InputStream in = getInputStream(id)){
            in.transferTo(buffer);
        }
        return buffer.toByteArray();
    }
    
}
