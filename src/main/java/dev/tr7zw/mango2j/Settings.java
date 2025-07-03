package dev.tr7zw.mango2j;

import java.io.File;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Getter;

@Component
public class Settings {

    @Value("${mango.baseDir}")
    private String baseDir;
    @Getter
    @Value("${mango.ollamaHost}")
    private String ollamaHost;
    
    @Getter
    @Value("${mango.ollamaPrompt}")
    private String ollamaPrompt;

    public File getBaseDir() {
        return new File(baseDir);
    }
    
}
