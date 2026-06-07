package dev.tr7zw.mango2j.task;

import java.util.List;
import java.util.Map;

public interface InternalTaskProcessor {

    String getKey();

    String getDisplayName();

    default boolean isInternalOnly() {
        return false;
    }

    default List<TaskFormField> getFormFields() {
        return List.of();
    }

    default String getFormFragment() {
        return "fragments/task-form :: form";
    }

    default Map<String, Object> getFormModel() {
        return Map.of();
    }

    void process(TaskExecutionContext context) throws Exception;
}
