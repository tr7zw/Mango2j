package dev.tr7zw.mango2j.util;

import jakarta.annotation.*;

import java.time.Instant;

public class DateFormatUtil {

    /**
     * Formats an Instant as a human-readable time-ago string.
     * Returns null if the instant is null.
     * Format: "now", "5m", "2h", "3d", "2w", "3mo", "1y", etc.
     */
    public static String formatTimeAgo(@Nullable Instant instant) {
        if (instant == null) {
            return null;
        }

        long nowMs = System.currentTimeMillis();
        long instantMs = instant.toEpochMilli();
        long diffMs = nowMs - instantMs;

        if (diffMs < 0) {
            return "now"; // Future date, shouldn't happen, fall back to "now"
        }

        // Less than 1 minute
        if (diffMs < 60_000) {
            return "now";
        }

        // Minutes
        long minutes = diffMs / 60_000;
        if (minutes < 60) {
            return minutes + "m";
        }

        // Hours
        long hours = minutes / 60;
        if (hours < 24) {
            return hours + "h";
        }

        // Days
        long days = hours / 24;
        if (days < 7) {
            return days + "d";
        }

        // Weeks
        long weeks = days / 7;
        if (weeks < 4) {
            return weeks + "w";
        }

        // Months (approximate as 30 days)
        long months = days / 30;
        if (months < 12) {
            return months + "mo";
        }

        // Years
        long years = months / 12;
        return years + "y";
    }
}
