package dev.tr7zw.mango2j.util;

import java.util.*;
import java.util.stream.*;

public class SimilarityUtil {

    public static double cosineSimilarity(Map<String, Double> vec1, Map<String, Double> vec2) {
        if (vec1.isEmpty() || vec2.isEmpty()) {
            return 0.0;
        }

        // Calculate dot product
        double dotProduct = 0.0;
        for (String term : vec1.keySet()) {
            if (vec2.containsKey(term)) {
                dotProduct += vec1.get(term) * vec2.get(term);
            }
        }

        // Calculate magnitudes
        double mag1 = Math.sqrt(vec1.values().stream()
                .mapToDouble(v -> v * v)
                .sum());
        double mag2 = Math.sqrt(vec2.values().stream()
                .mapToDouble(v -> v * v)
                .sum());

        if (mag1 == 0.0 || mag2 == 0.0) {
            return 0.0;
        }

        return dotProduct / (mag1 * mag2);
    }

    public static Map<String, Double> toVector(String description, int corpusSize, Map<String, Integer> documentFrequency) {
        Map<String, Double> vector = new HashMap<>();

        if (description == null || description.isBlank()) {
            return vector;
        }

        Set<String> tags = TagUtil.extractTags(description);
        Map<String, Integer> termFrequency = new HashMap<>();

        // Calculate term frequency
        for (String tag : tags) {
            termFrequency.put(tag, termFrequency.getOrDefault(tag, 0) + 1);
        }

        // Calculate TF-IDF
        for (Map.Entry<String, Integer> entry : termFrequency.entrySet()) {
            String term = entry.getKey();
            int tf = entry.getValue();

            // IDF calculation: log(corpus_size / document_frequency)
            int docFreq = documentFrequency.getOrDefault(term, 1);
            double idf = Math.log((double) corpusSize / Math.max(1, docFreq));

            // TF-IDF = TF * IDF
            vector.put(term, tf * idf);
        }

        return vector;
    }

    public static List<Object> findSimilar(Object query, List<Object> allChapters,
                                           java.util.function.Function<Object, String> descriptionExtractor,
                                           java.util.function.Function<Object, Integer> idExtractor,
                                           int limit) {
        if (allChapters.isEmpty()) {
            return List.of();
        }

        // Calculate document frequency for IDF
        Map<String, Integer> documentFrequency = new HashMap<>();
        for (Object chapter : allChapters) {
            String desc = descriptionExtractor.apply(chapter);
            Set<String> tags = TagUtil.extractTags(desc);
            for (String tag : tags) {
                documentFrequency.put(tag, documentFrequency.getOrDefault(tag, 0) + 1);
            }
        }

        // Get query vector
        String queryDesc = descriptionExtractor.apply(query);
        Map<String, Double> queryVector = toVector(queryDesc, allChapters.size(), documentFrequency);
        Integer queryId = idExtractor.apply(query);

        // Calculate similarity with all chapters
        return allChapters.stream()
                .filter(c -> !idExtractor.apply(c).equals(queryId))
                .map(c -> {
                    Map<String, Double> chapterVector = toVector(
                            descriptionExtractor.apply(c),
                            allChapters.size(),
                            documentFrequency
                    );
                    double similarity = cosineSimilarity(queryVector, chapterVector);
                    return new SimilarityScore(c, similarity);
                })
                .filter(s -> s.similarity > 0.0)  // Only keep chapters with some similarity
                .sorted((a, b) -> Double.compare(b.similarity, a.similarity))
                .limit(limit)
                .map(s -> s.item)
                .collect(Collectors.toList());
    }

    static class SimilarityScore {
        Object item;
        double similarity;

        SimilarityScore(Object item, double similarity) {
            this.item = item;
            this.similarity = similarity;
        }
    }
}
