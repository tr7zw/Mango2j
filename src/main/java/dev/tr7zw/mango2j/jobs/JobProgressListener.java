package dev.tr7zw.mango2j.jobs;

@FunctionalInterface
public interface JobProgressListener {
    void onProgress(int current, int total, String message);
}
