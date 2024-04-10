package dev.tr7zw.mango2j.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface TitleWrapper {

    List<Path> getTitles() throws IOException;
    List<Path> getChapters() throws IOException;
    
}
