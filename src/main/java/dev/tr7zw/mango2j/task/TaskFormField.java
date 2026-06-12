package dev.tr7zw.mango2j.task;

public record TaskFormField(
        String name,
        String label,
        String type,
        String placeholder,
        boolean required
) {
}
