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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.springframework.stereotype.Service;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Service
public class FileService {

    public ChapterWrapper getChapterWrapper(Path location) {
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
                return pathStream.filter(p -> p.toFile().isDirectory() && !isChapter(p))
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

        private boolean isChapter(Path p) {
            try {
                File f = p.toFile();
                if (f.isFile()
                        && (f.getName().toLowerCase().endsWith(".zip") || f.getName().toLowerCase().endsWith(".cbz"))) {
                    return true; // is a chapter file
                }
                if (f.isDirectory() && !hasSubDirs(p)) {
                    try (Stream<Path> subFileStream = Files.list(f.toPath())) {
                        return subFileStream.noneMatch(this::isChapter);
                    }
                }
                return false;
            } catch (IOException e) {
                e.printStackTrace();
                return false; // in doubt filter
            }
        }

    }

    private final Comparator<String> titleComp = new Comparator<String>() {
        public int compare(String o1, String o2) {
            int a1 = extractInt(o1);
            int a2 = extractInt(o2);
            if (a1 == a2) {
                return o1.compareTo(o2);
            }
            return a1 - a2;
        }

        int extractInt(String s) {
            String num = s.replaceAll("\\D", "");
            // return 0 if no digits found
            try {
                return num.isEmpty() ? 0 : Integer.parseInt(num);
            } catch (Exception ex) {
                return 0;
            }
        }
    };

    @RequiredArgsConstructor
    private class FlatDirChapter implements ChapterWrapper {

        @NonNull
        private File dir;

        @Override
        public List<String> getFiles() {
            try {
                String[] fileList = dir.list();
                if (fileList != null) {
                    List<String> files = new ArrayList<>(Arrays.asList(fileList));
                    files.remove("Thumbs.db");
                    files.sort(titleComp);
                    return files;
                } else {
                    return Collections.emptyList();
                }
            } catch (SecurityException e) {
                e.printStackTrace();
                return Collections.emptyList();
            }
        }

        @Override
        public InputStream getInputStream(String id) throws FileNotFoundException {
            return new FileInputStream(new File(dir, id));
        }

        @Override
        public boolean hasFile(String id) {
            return new File(dir, id).exists();
        }

    }

    @RequiredArgsConstructor
    private class ZipChapter implements ChapterWrapper {

        @NonNull
        private File file;

        @Override
        public List<String> getFiles() {
            try (ZipFile zipFile = new ZipFile(file)) {
                return zipFile.stream().map(ZipEntry::getName).filter(n -> !n.toLowerCase().equals("thumbs.db"))
                        .sorted(titleComp).toList();
            } catch (ZipException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return Collections.emptyList();
        }

        @Override
        public InputStream getInputStream(String id) throws FileNotFoundException {
            try (ZipFile zipFile = new ZipFile(file)) {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                zipFile.getInputStream(zipFile.getEntry(id)).transferTo(buffer);
                return new ByteArrayInputStream(buffer.toByteArray());
            } catch (ZipException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            throw new FileNotFoundException();
        }

        @Override
        public boolean hasFile(String id) {
            try (ZipFile zipFile = new ZipFile(file)) {
                return zipFile.getEntry(id) != null;
            } catch (ZipException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }

    }

}
