package dev.tr7zw.mango2j.util;

import jakarta.annotation.Nullable;

public class FormatUtil {

    public static String formatFileSize(@Nullable Long bytes) {
        if (bytes == null || bytes == 0) {
            return "0 B";
        }
        final String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double size = bytes;
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024.0;
            unitIndex++;
        }
        return String.format("%.1f %s", size, units[unitIndex]);
    }

    public static String escapeHtml(@Nullable String text) {
        if (text == null) {
            return "";
        }

        // First escape all HTML
        String escaped = text.replace("<", "&lt;")
                            .replace(">", "&gt;");

        // Convert newlines to <br> tags
        escaped = escaped.replace("\n", "<br>");

        // Unescape safe <br> tags
        escaped = escaped.replace("&lt;br&gt;", "<br>")
                        .replace("&lt;br /&gt;", "<br>")
                        .replace("&lt;br/&gt;", "<br>");

        return escaped;
    }
}
