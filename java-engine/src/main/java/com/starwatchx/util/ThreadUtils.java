package com.starwatchx.util;

/**
 * Helper utilities for thread management.
 */
public final class ThreadUtils {

    private ThreadUtils() {
        // Utility class
    }

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    public static Thread createNamedThread(Runnable runnable, String name, boolean daemon) {
        Thread thread = new Thread(runnable, name);
        thread.setDaemon(daemon);
        return thread;
    }
}

