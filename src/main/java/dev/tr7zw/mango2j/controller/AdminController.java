package dev.tr7zw.mango2j.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import dev.tr7zw.mango2j.jobs.FileScanner;
import dev.tr7zw.mango2j.jobs.ThumbnailGenerator;

@Controller
public class AdminController {

    @Autowired
    private ThumbnailGenerator thumbnailGenerator;
    @Autowired
    private FileScanner fileScanner;
    
    @GetMapping("/admin/generateThumbnails")
    public ResponseEntity<String> generateThumbnails() throws IOException {
        if(thumbnailGenerator.isRunning()) {
            return new ResponseEntity<>("Already running", null, HttpStatus.OK);
        }
        thumbnailGenerator.executeLongRunningTask();
        return new ResponseEntity<>("Ok", null, HttpStatus.OK);
    }
    
    @GetMapping("/admin/scanFiles")
    public ResponseEntity<String> scanFiles() throws IOException {
        if(fileScanner.isRunning()) {
            return new ResponseEntity<>("Already running", null, HttpStatus.OK);
        }
        fileScanner.executeLongRunningTask();
        return new ResponseEntity<>("Ok", null, HttpStatus.OK);
    }
    
}
