package dev.tr7zw.mango2j.service;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dev.tr7zw.mango2j.Settings;
import dev.tr7zw.mango2j.db.Chapter;
import dev.tr7zw.mango2j.db.ChapterRepository;
import dev.tr7zw.mango2j.service.ChapterWrapper.Entry;
import io.github.ollama4j.OllamaAPI;
import io.github.ollama4j.exceptions.OllamaBaseException;
import io.github.ollama4j.models.response.OllamaResult;
import io.github.ollama4j.utils.OptionsBuilder;
import jakarta.annotation.PostConstruct;
import lombok.extern.java.Log;

@Service
@Log
public class AiService {

    @Autowired
    private Settings settings;
    @Autowired
    private ChapterRepository chapterRepository;
    @Autowired
    private FileService fileService;

    private OllamaAPI ollamaAPI;

    @PostConstruct
    public void init() {
        ollamaAPI = new OllamaAPI(settings.getOllamaHost());
        ollamaAPI.setRequestTimeoutSeconds(120);
        ollamaAPI.setVerbose(false);
        if (available()) {
            try {
                ollamaAPI.pullModel("gemma3:27b");
                ollamaAPI.pullModel("nomic-embed-text:latest");
            } catch (OllamaBaseException | IOException | URISyntaxException | InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public boolean available() {
        try {
            return ollamaAPI.ping();
        } catch (Exception e) {
            log.warning("Ollama API not available: " + e.getMessage());
            return false;
        }
    }

    public String generateDescription(Chapter chapter) {
        try {
            ChapterWrapper chapterWrapper = fileService.getChapterWrapper(new File(chapter.getFullPath()).toPath());
            List<Entry> entries = chapterWrapper.getFilesTyped();
            OllamaResult result = ollamaAPI.generateWithImageURLs("gemma3:27b", settings.getOllamaPrompt(),
                    entries.stream().filter(e -> e.type().equals("IMG")).filter(e -> !".gif".equals(e.filetype()))
                            .map(e -> "http://localhost:8080/image/" + chapter.getId() + "/" + e.id()).limit(50)
                            .toList(),
                    new OptionsBuilder().build());
            return result.getResponse();
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to generate description", e);
            return null;
        }
    }

    public float[] embed(String text) {
        try {
            List<Double> data = ollamaAPI.embed("nomic-embed-text:latest", List.of(text)).getEmbeddings().get(0);
            float[] embedding = new float[data.size()];
            for (int i = 0; i < data.size(); i++) {
                embedding[i] = data.get(i).floatValue();
            }
            return normalize(embedding); // Normalize the embedding
        } catch (Exception e) {
            log.warning("Failed to embed text: " + e.getMessage());
            return new float[0]; // Return empty array on failure
        }
    }

    public List<Chapter> findClosest(String query, int n) {
        // Load all chapters — for <50k this is OK, otherwise consider caching
        List<Chapter> allChapters = chapterRepository.findAll();

        // Pre-normalize the query vector
        float[] normalizedQuery = embed(query);

        // Compute similarity and store pairs (Chapter, similarity)
        List<ScoredChapter> scored = new ArrayList<>(allChapters.size());
        for (Chapter chapter : allChapters) {
            float[] emb = chapter.getEmbedding();
            if (emb == null || emb.length == 0)
                continue;

            float score = dot(normalizedQuery, emb); // cosine similarity

            scored.add(new ScoredChapter(chapter, score));
        }

        // Sort by descending similarity and return top N
        return scored.stream().sorted(Comparator.comparing(ScoredChapter::getScore).reversed()).limit(n)
                .map(ScoredChapter::getChapter).toList();
    }

    private static float[] normalize(float[] vec) {
        double length = 0.0;
        for (float v : vec) {
            length += v * v;
        }
        length = Math.sqrt(length);
        if (length == 0)
            return vec;

        float[] norm = new float[vec.length];
        for (int i = 0; i < vec.length; i++) {
            norm[i] = (float) (vec[i] / length);
        }
        return norm;
    }

    private static float dot(float[] a, float[] b) {
        float sum = 0f;
        for (int i = 0; i < Math.min(a.length, b.length); i++) {
            sum += a[i] * b[i];
        }
        return sum;
    }

    // Helper class to hold chapter and score
    private static class ScoredChapter {
        private final Chapter chapter;
        private final float score;

        public ScoredChapter(Chapter chapter, float score) {
            this.chapter = chapter;
            this.score = score;
        }

        public Chapter getChapter() {
            return chapter;
        }

        public float getScore() {
            return score;
        }
    }

}
