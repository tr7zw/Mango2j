package dev.tr7zw.mango2j.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.springframework.stereotype.Service;

import dev.tr7zw.mango2j.util.ImageNameSorterUtil;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Service
public class FileService {

    private final Map<String, String> fileTypeMap = new HashMap<>() {
         {
            put(".jpg", "IMG");
            put(".jpeg", "IMG");
            put(".png", "IMG");
            put(".gif", "IMG");
            put(".bmp", "IMG");
            put(".jxl", "IMG");
            put(".webp", "IMG");
            put(".mp4", "VIDEO");
            put(".mov", "VIDEO");
            put(".wmv", "VIDEO");
            put(".txt", "TXT");
            put(".pdf", "PDF");
            put(".db", "IGNORE");
        }
    };
    
    public ChapterWrapper getChapterWrapper(Path location) throws IOException {
        if (location.toFile().isDirectory()) {
            return new FlatDirChapter(location.toFile());
        } else {
            return new ZipChapter(location.toFile());
        }
    }

    public TitleWrapper getTitleWrapper(Path location) {
        return new FlatDirTitle(location.toFile());
    }

    @RequiredArgsConstructor
    private class FlatDirTitle implements TitleWrapper {

        @NonNull
        private File dir;

        public List<Path> getChapters() throws IOException {
            try (Stream<Path> pathStream = Files.list(dir.toPath())) {
                return pathStream.filter(this::isChapter).sorted(Comparator.comparingLong(this::getLastModifiedTime))
                        .collect(Collectors.toList());
            }
        }

        public List<Path> getTitles() throws IOException {
            try (Stream<Path> pathStream = Files.list(dir.toPath())) {
                return pathStream.filter(p -> p.toFile().isDirectory() && !isChapter(p) && !isEmptyDir(p))
                        .sorted(Comparator.comparingLong(this::getLastModifiedTime)).collect(Collectors.toList());
            }
        }

        private long getLastModifiedTime(Path path) {
            try {
                return Files.getLastModifiedTime(path).toMillis();
            } catch (IOException e) {
                // Handle exception, e.g., log it
                e.printStackTrace();
                return 0; // Default value
            }
        }

        private boolean hasSubDirs(Path path) {
            try (Stream<Path> subDirStream = Files.list(path)) {
                return subDirStream.anyMatch(p -> p.toFile().isDirectory());
            } catch (IOException e) {
                e.printStackTrace();
                return true; // in doubt filter
            }
        }

        private boolean isEmptyDir(Path path) {
            try (Stream<Path> subFileStream = Files.list(path)) {
                return !subFileStream.findAny().isPresent();
            } catch (IOException e) {
                e.printStackTrace();
                return true; // in doubt filter
            }
        }

        private boolean isChapter(Path p) {
            try {
                File f = p.toFile();
                if (f.isFile()
                        && (f.getName().toLowerCase().endsWith(".zip") || f.getName().toLowerCase().endsWith(".cbz"))) {
                    return true; // is a chapter file
                }
                if (f.isDirectory() && !hasSubDirs(p)) {
                    try (Stream<Path> subFileStream = Files.list(f.toPath())) {
                        return subFileStream.noneMatch(this::isChapter) && !new FlatDirChapter(f).getInternalFiles().isEmpty();
                    }
                }
                return false;
            } catch (IOException e) {
                e.printStackTrace();
                return false; // in doubt filter
            }
        }

    }

    @RequiredArgsConstructor
    private class FlatDirChapter implements ChapterWrapper {

        @NonNull
        private File dir;

        @Override
        public List<Entry> getFilesTyped() {
            List<Entry> entries = new ArrayList<>();
            List<String> files = getInternalFiles();
            for(int i = 0; i < files.size(); i++) {
                String filetype = files.get(i).toLowerCase().substring(files.get(i).lastIndexOf('.'));
                entries.add(new Entry(i, fileTypeMap.getOrDefault(filetype, "IMG"), filetype));
            }
            return entries;
        }
        
        private List<String> getInternalFiles() {
            String[] fileList = dir.list();
            if (fileList != null) {
                List<String> files = new ArrayList<>(Arrays.asList(fileList));
                files.remove("Thumbs.db");
                files.remove("info.json");
                files.removeIf(s -> s.toLowerCase().endsWith(".zip") || s.toLowerCase().endsWith(".cbz"));
                files.sort(ImageNameSorterUtil.COMPARATOR);
                return files;
            } else {
                return Collections.emptyList();
            }
        }
        
        @Override
        public int getFiles() {
            try {
                return getInternalFiles().size();
            } catch (SecurityException e) {
                e.printStackTrace();
                return -1;
            }
        }

        @Override
        public InputStream getInputStream(int id) throws FileNotFoundException {
            return new FileInputStream(new File(dir, getInternalFiles().get(id)));
        }

        @Override
        public boolean hasFile(int id) {
            List<String> files = getInternalFiles();
            if(id >= files.size()) {
                return false;
            }
            return new File(dir, files.get(id)).exists();
        }

        @Override
        public String getFileType(int id) {
            List<String> files = getInternalFiles();
            return files.get(id).split("\\.")[files.get(id).split("\\.").length-1].toLowerCase();
        }

        @Override
        public boolean hasFile(String name) {
            return new File(dir, name).exists();
        }

        @Override
        public InputStream getFile(String name) throws FileNotFoundException {
            return new FileInputStream(new File(dir, name));
        }

        @Override
        public void close() throws Exception {
            // Nothing to close for flat directory
        }

        @Override
        public Instant getLastModified() {
            try {
                return Instant.ofEpochMilli(Files.getLastModifiedTime(dir.toPath()).toMillis());
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        public Long getFileSize() {
            try {
                return Files.walk(dir.toPath())
                        .filter(Files::isRegularFile)
                        .mapToLong(p -> {
                            try {
                                return Files.size(p);
                            } catch (IOException e) {
                                return 0;
                            }
                        })
                        .sum();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    private class ZipChapter implements ChapterWrapper {

        @NonNull
        private File file;
        private ZipFile zipFile;
        private List<String> filesListCache;

        public ZipChapter(File file) throws IOException {
            this.file = file;
            this.zipFile = new ZipFile(file);
        }

        private List<String> getInternalFiles() {
            if (filesListCache == null) {
                filesListCache = zipFile.stream().filter(z -> !z.isDirectory()).map(ZipEntry::getName).sorted(ImageNameSorterUtil.COMPARATOR).toList();
            }
            return filesListCache;
        }
        
        @Override
        public List<Entry> getFilesTyped() {
            List<Entry> entries = new ArrayList<>();
            List<String> files = getInternalFiles();
            for(int i = 0; i < files.size(); i++) {
                String filetype = files.get(i).toLowerCase().substring(files.get(i).lastIndexOf('.'));
                entries.add(new Entry(i, fileTypeMap.getOrDefault(filetype, "IMG"), filetype));
            }
            return entries;
        }
        
        @Override
        public int getFiles() {
            return getInternalFiles().size();
        }

        @Override
        public InputStream getInputStream(int id) throws FileNotFoundException {
            try {
                List<String> list = getInternalFiles();
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                zipFile.getInputStream(zipFile.getEntry(list.get(id))).transferTo(buffer);
                return new ByteArrayInputStream(buffer.toByteArray());
            } catch (ZipException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            throw new FileNotFoundException();
        }

        @Override
        public boolean hasFile(int id) {
            return getInternalFiles().size() >= id;
        }

        @Override
        public String getFileType(int id) {
            List<String> list = getInternalFiles();
            return list.get(id).split("\\.")[list.get(id).split("\\.").length-1].toLowerCase();
        }

        @Override
        public boolean hasFile(String name) {
            return getInternalFiles().contains(name);
        }

        @Override
        public InputStream getFile(String name) throws FileNotFoundException {
            return getInputStream(getInternalFiles().indexOf(name));
        }

        @Override
        public void close() throws Exception {
            try {
                zipFile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public Instant getLastModified() {
            try {
                return Instant.ofEpochMilli(Files.getLastModifiedTime(file.toPath()).toMillis());
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        public Long getFileSize() {
            try {
                return Files.size(file.toPath());
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

}
