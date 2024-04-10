package dev.tr7zw.mango2j.controller;

import java.io.File;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import dev.tr7zw.mango2j.db.Chapter;
import dev.tr7zw.mango2j.db.ChapterRepository;
import dev.tr7zw.mango2j.service.ChapterWrapper;
import dev.tr7zw.mango2j.service.FileService;

@Controller
public class FileController {

    private final FileService fileService;
    private final ChapterRepository chapterRepo;
    
    @Autowired
    public FileController(FileService fileService, ChapterRepository chapterRepo) {
        this.fileService = fileService;
        this.chapterRepo = chapterRepo;
    }
    
    @GetMapping("/image/{chapter}/{id}")
    public ResponseEntity<ByteArrayResource> getImage(@PathVariable Integer chapter, @PathVariable Integer id) throws IOException {
        Chapter ch = chapterRepo.getReferenceById(chapter);
        ChapterWrapper chapterWrapper = fileService.getChapterWrapper(new File(ch.getFullPath()).toPath());
        // Load your image file into a byte array
        if(!chapterWrapper.hasFile(id)) {
            return new ResponseEntity<>(null, new HttpHeaders(), HttpStatus.NOT_FOUND);
        }
//        if(id.toLowerCase().endsWith(".png") || id.toLowerCase().endsWith(".jpg") || id.toLowerCase().endsWith(".jpeg")) {
//            try {
//                BufferedImage img = ImageIO.read(chapterWrapper.getInputStream(id));
//                ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
//                WebpUtil.writeAsWebp(new MemoryCacheImageOutputStream(byteBuffer), img);
//                // Wrap the byte array in a ByteArrayResource
//                ByteArrayResource resource = new ByteArrayResource(byteBuffer.toByteArray());
//
//                // Set the headers
//                HttpHeaders headers = new HttpHeaders();
//                headers.setContentType(MediaType.parseMediaType("image/webp"));
//                headers.setContentLength(byteBuffer.size());
//
//                // Return ResponseEntity with the image bytes and headers
//                return new ResponseEntity<>(resource, headers, HttpStatus.OK);
//            }catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
        byte[] imageBytes = chapterWrapper.getBytes(id);

        // Wrap the byte array in a ByteArrayResource
        ByteArrayResource resource = new ByteArrayResource(imageBytes);

        // Set the headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(getMediaType(chapterWrapper.getFileType(id)));
        headers.setContentLength(imageBytes.length);

        // Return ResponseEntity with the image bytes and headers
        return new ResponseEntity<>(resource, headers, HttpStatus.OK);
    }
    
    @GetMapping("/thumbnail/{id}")
    public ResponseEntity<ByteArrayResource> getThumbnail(@PathVariable Integer id) throws IOException {
        Chapter ch = chapterRepo.getReferenceById(id);
        if(ch.getThumbnail() == null) {
            return new ResponseEntity<>(null, new HttpHeaders(), HttpStatus.NOT_FOUND);
        }
        // Wrap the byte array in a ByteArrayResource
        ByteArrayResource resource = new ByteArrayResource(ch.getThumbnail());

        // Set the headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(getMediaType("webp"));
        headers.setContentLength(ch.getThumbnail().length);

        // Return ResponseEntity with the image bytes and headers
        return new ResponseEntity<>(resource, headers, HttpStatus.OK);
    }
    
    private MediaType getMediaType(String type) {
        return switch(type) {
        case "gif": yield MediaType.IMAGE_GIF;
        case "png": yield MediaType.IMAGE_PNG;
        case "webp": yield MediaType.parseMediaType("image/webp");
        case "jpeg":
        case "jpg": yield MediaType.IMAGE_JPEG;
        case "jxl": yield MediaType.parseMediaType("image/jxl");
        default: yield MediaType.IMAGE_PNG;
        };
    }
}