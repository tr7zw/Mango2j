package dev.tr7zw.mango2j.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import dev.tr7zw.mango2j.Settings;
import lombok.Getter;
import lombok.extern.java.Log;

@Component
@Log
public class MoveTargetService {

    @Getter
    private List<File> moveTargets = new ArrayList<File>();
    
    @Autowired
    public MoveTargetService(Settings settings) throws IOException {
        File configFile = new File(settings.getBaseDir(), "moveTargets.txt");
        log.log(Level.INFO, "Checking " + configFile.getAbsolutePath());
        if(configFile.exists()) {
            log.log(Level.INFO, "Processing moveTargets.txt");
            Files.readAllLines(configFile.toPath()).forEach(line -> {
                File target = new File(settings.getBaseDir(), line);
                if(target.exists()) {
                    log.log(Level.INFO, "Added move folder: " + line);
                    moveTargets.add(target);
                } else {
                    log.log(Level.WARNING, "Cant find move folder: " + line);
                }
            });
        } else {
            log.log(Level.WARNING, "moveTargets.txt not found: " + configFile.getAbsolutePath());
        }
    }


}
