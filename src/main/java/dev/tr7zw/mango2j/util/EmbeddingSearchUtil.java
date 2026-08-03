package dev.tr7zw.mango2j.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import dev.tr7zw.mango2j.db.Chapter;
import dev.tr7zw.mango2j.db.ChapterEmbedding;
import dev.tr7zw.mango2j.db.ChapterEmbeddingRepository;
import dev.tr7zw.mango2j.service.EmbeddingModelService;

public class EmbeddingSearchUtil {

    private EmbeddingSearchUtil() {
    }

    public static byte[] toBytes(float[] vector) {
        if (vector == null || vector.length == 0) {
            return null;
        }
        ByteBuffer buffer = ByteBuffer.allocate(vector.length * Float.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        for (float value : vector) {
            buffer.putFloat(value);
        }
        return buffer.array();
    }

    public static float[] fromBytes(byte[] data) {
        if (data == null || data.length == 0 || data.length % Float.BYTES != 0) {
            return new float[0];
        }
        ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        float[] vector = new float[data.length / Float.BYTES];
        for (int i = 0; i < vector.length; i++) {
            vector[i] = buffer.getFloat();
        }
        return vector;
    }

    public static double cosineSimilarity(float[] vec1, float[] vec2) {
        if (vec1 == null || vec2 == null || vec1.length == 0 || vec2.length == 0 || vec1.length != vec2.length) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double mag1 = 0.0;
        double mag2 = 0.0;
        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2[i];
            mag1 += vec1[i] * vec1[i];
            mag2 += vec2[i] * vec2[i];
        }

        if (mag1 == 0.0 || mag2 == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(mag1) * Math.sqrt(mag2));
    }

    public static List<Chapter> findClosestBySearch(String query, EmbeddingModelService embeddingModelService, List<Chapter> chapters, int limit) {
        return findClosestBySearch(query, embeddingModelService, chapters, limit, 0.0);
    }

    public static List<Chapter> findClosestBySearch(String query, EmbeddingModelService embeddingModelService, List<Chapter> chapters,
                                                    int limit, double minSimilarity) {
        if (query == null || query.isBlank() || embeddingModelService == null) {
            return List.of();
        }
        return findClosestByVector(embeddingModelService.embed(query), chapters, limit, minSimilarity, null);
    }

    public static List<Chapter> findClosestBySearch(String query, EmbeddingModelService embeddingModelService,
                                                    ChapterEmbeddingRepository chapterEmbeddingRepo, List<Chapter> chapters,
                                                    int limit, double minSimilarity) {
        if (query == null || query.isBlank() || embeddingModelService == null || chapterEmbeddingRepo == null) {
            return List.of();
        }
        return findClosestByVectorWithScores(embeddingModelService.embed(query), chapters, limit, minSimilarity, null, chapterEmbeddingRepo)
                .stream()
                .map(ChapterScore::chapter)
                .toList();
    }

    public static List<Chapter> findClosestByVector(float[] queryVector, List<Chapter> chapters, int limit) {
        return findClosestByVector(queryVector, chapters, limit, 0.0, null);
    }

    public static List<Chapter> findClosestByVector(float[] queryVector, List<Chapter> chapters, int limit,
                                                    double minSimilarity) {
        return findClosestByVector(queryVector, chapters, limit, minSimilarity, null);
    }

    public static List<Chapter> findClosestByVector(float[] queryVector, List<Chapter> chapters, int limit,
                                                    double minSimilarity, Integer excludedChapterId) {
        return findClosestByVectorWithScores(queryVector, chapters, limit, minSimilarity, excludedChapterId).stream()
                .map(ChapterScore::chapter)
                .toList();
    }

    public static List<ChapterScore> findClosestByVectorWithScores(float[] queryVector, List<Chapter> chapters, int limit,
                                                                   double minSimilarity, Integer excludedChapterId) {
        return findClosestByVectorWithScores(queryVector, chapters, limit, minSimilarity, excludedChapterId, null);
    }

    public static List<ChapterScore> findClosestByVectorWithScores(float[] queryVector, List<Chapter> chapters, int limit,
                                                                   double minSimilarity, Integer excludedChapterId,
                                                                   ChapterEmbeddingRepository chapterEmbeddingRepo) {
        if (queryVector == null || queryVector.length == 0 || chapters == null || chapters.isEmpty() || limit <= 0) {
            return List.of();
        }

        List<ChapterScore> scored = new ArrayList<>();
        for (Chapter chapter : chapters) {
            if (chapter == null) {
                continue;
            }
            if (excludedChapterId != null && excludedChapterId.equals(chapter.getId())) {
                continue;
            }
            byte[] candidateData = null;
            if (chapterEmbeddingRepo != null) {
                candidateData = chapterEmbeddingRepo.findByChapterId(chapter.getId())
                        .map(ChapterEmbedding::getVectorBytes)
                        .orElse(null);
            }
            float[] candidateVector = fromBytes(candidateData);
            double similarity = cosineSimilarity(queryVector, candidateVector);
            if (similarity >= minSimilarity) {
                scored.add(new ChapterScore(chapter, similarity));
            }
        }

        return scored.stream()
                .sorted(Comparator.comparingDouble(ChapterScore::similarity).reversed())
                .limit(limit)
                .toList();
    }

    public record ChapterScore(Chapter chapter, double similarity) {
    }

}
