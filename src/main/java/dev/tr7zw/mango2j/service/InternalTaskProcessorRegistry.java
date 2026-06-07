package dev.tr7zw.mango2j.service;

import dev.tr7zw.mango2j.task.InternalTaskProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InternalTaskProcessorRegistry {

    private final Map<String, InternalTaskProcessor> byKey = new ConcurrentHashMap<>();

    @Autowired
    public InternalTaskProcessorRegistry(List<InternalTaskProcessor> processors) {
        for (InternalTaskProcessor processor : processors) {
            byKey.put(processor.getKey(), processor);
        }
    }

    public Optional<InternalTaskProcessor> find(String key) {
        return Optional.ofNullable(byKey.get(key));
    }

    public List<InternalTaskProcessor> all() {
        return byKey.values().stream()
                .sorted(Comparator.comparing(InternalTaskProcessor::getDisplayName))
                .toList();
    }

    public List<InternalTaskProcessor> userVisible() {
        return byKey.values().stream()
                .filter(p -> !p.isInternalOnly())
                .sorted(Comparator.comparing(InternalTaskProcessor::getDisplayName))
                .toList();
    }
}
