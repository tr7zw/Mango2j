package dev.tr7zw.mango2j.service;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface ChapterWrapper {

    List<Entry> getFilesTyped();
    int getFiles();
    InputStream getInputStream(int id) throws FileNotFoundException;
    boolean hasFile(int id);
    String getFileType(int id);
    boolean hasFile(String name);
    InputStream getFile(String name) throws FileNotFoundException;
    default byte[] getBytes(int id) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try(InputStream in = getInputStream(id)){
            in.transferTo(buffer);
        }
        return buffer.toByteArray();
    }
    
    public record Entry(int id, String type, String filetype) {
    }

}
