package dev.tr7zw.mango2j.util;

import jakarta.annotation.*;

import java.util.*;
import java.util.stream.*;

public class TagUtil {

    public static Set<String> extractTags(@Nullable String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }

        Set<String> stopWords = Set.of(
                "the", "a", "an", "is", "are", "of", "for", "to", "and", "or",
                "in", "on", "at", "from", "by", "as", "about", "check", "my",
                "more", "content", "http", "https", "www", "com", "org", "net", "full", "posts", "here",
                "with", "that", "this", "it", "be", "have", "do", "not", "can", "want", "you", "jump", "also",
                "will", "would", "could", "should", "may", "might", "must", "shall", "enjoy", "hope", "into", "one",
                "zip", "rar", "7z", "pdf", "epub", "mobi", "cbz", "cbr", "php", "js", "css", "html", "xml", "json", "txt", "md",
                "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "mp4", "mkv", "avi", "mp3", "flac", "wav", "ogg",
                "patreon", "gumroad", "ko-fi", "paypal", "buymeacoffee", "discord", "twitter", "facebook", "instagram", "linkedin",
                "bit", "fanbox", "pixiv", "deviantart", "artstation", "github", "gitlab", "soon", "coming", "update", "new",
                "release", "latest", "chapter", "volume", "series", "author", "commission", "pages", "journal", "blog", "website", "store",
                "shop", "contact", "email", "subscribe", "follow", "support", "donate", "bookmarks"
        );

        return Arrays.stream(text.toLowerCase().split("[,\\s/\\-_\\.!?;:]+"))
                .filter(word -> word.length() >= 3)  // min 3 characters
                .filter(word -> !word.matches(".*[:/.].*"))  // remove URLs and paths
                .filter(word -> !stopWords.contains(word))
                .filter(word -> word.matches("^[\\p{L}\\p{N}]+$"))  // Unicode letters and numbers
                .collect(Collectors.toSet());
    }

    public static Map<String, Integer> calculateTagFrequency(java.util.List<?> items, java.util.function.Function<Object, String> descriptionExtractor) {
        Map<String, Integer> tagFrequency = items.stream()
                .map(descriptionExtractor)
                .flatMap(desc -> extractTags(desc).stream())
                .collect(Collectors.groupingByConcurrent(
                        java.util.function.Function.identity(),
                        Collectors.summingInt(e -> 1)
                ));

        return tagFrequency.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }
}
