package dev.tr7zw.mango2j;

import java.io.File;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Settings {

    @Value("${mango.baseDir}")
    private String baseDir;

    public File getBaseDir() {
        return new File(baseDir);
    }
    
}
