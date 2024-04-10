package dev.tr7zw.mango2j.service;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface ChapterWrapper {

    List<String> getFiles();
    InputStream getInputStream(String id) throws FileNotFoundException;
    boolean hasFile(String id);
    default byte[] getBytes(String id) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try(InputStream in = getInputStream(id)){
            in.transferTo(buffer);
        }
        return buffer.toByteArray();
    }
    
}
