package dev.tr7zw.mango2j.service;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import lombok.extern.java.*;
import org.springframework.ai.transformers.TransformersEmbeddingModel;
import org.springframework.stereotype.Service;

@Service
@Log
public class EmbeddingModelService {

    private static final long IDLE_UNLOAD_MS = TimeUnit.MINUTES.toMillis(1);

    private final Object lock = new Object();
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
    private volatile TransformersEmbeddingModel activeModel;
    private volatile long lastUsedAt = 0L;
    private volatile boolean initializationFailed = false;

    @PostConstruct
    public void init() {
        cleanupExecutor.scheduleWithFixedDelay(this::unloadIfIdle, 30, 30, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void shutdown() {
        cleanupExecutor.shutdownNow();
        unloadModel();
    }

    public float[] embed(String text) {
        if (text == null || text.isBlank()) {
            return new float[0];
        }

        TransformersEmbeddingModel model = getOrCreateModel();
        if (model == null) {
            return new float[0];
        }

        try {
            return model.embed(text);
        } catch (Exception ex) {
            log.severe("Embedding model failed during embed: " + ex.getMessage());
            unloadModel();
            return new float[0];
        } finally {
            markUsed();
        }
    }

    public void unload() {
        unloadModel();
    }

    private TransformersEmbeddingModel getOrCreateModel() {
        synchronized (lock) {
            if (activeModel != null) {
                lastUsedAt = System.currentTimeMillis();
                return activeModel;
            }

            if (initializationFailed) {
                return null;
            }

            activeModel = buildModel();
            if (activeModel == null) {
                initializationFailed = true;
                return null;
            }

            lastUsedAt = System.currentTimeMillis();
            return activeModel;
        }
    }

    private TransformersEmbeddingModel buildModel() {
        try {
            TransformersEmbeddingModel embeddingModel = new TransformersEmbeddingModel();

            embeddingModel.afterPropertiesSet();
            log.info("Loaded embedding model successfully.");
            return embeddingModel;
        } catch (Exception ex) {
            System.err.println("Failed to initialize embedding model: " + ex.getMessage());
            return null;
        }
    }

    private void markUsed() {
        synchronized (lock) {
            lastUsedAt = System.currentTimeMillis();
        }
    }

    private void unloadIfIdle() {
        synchronized (lock) {
            if (activeModel != null && System.currentTimeMillis() - lastUsedAt > IDLE_UNLOAD_MS) {
                unloadModelLocked();
            }
        }
    }

    private void unloadModel() {
        synchronized (lock) {
            unloadModelLocked();
        }
    }

    private void unloadModelLocked() {
        activeModel = null;
        initializationFailed = false;
        lastUsedAt = 0L;
        log.info("Embedding model unloaded due to inactivity.");
        System.gc();
    }
}
