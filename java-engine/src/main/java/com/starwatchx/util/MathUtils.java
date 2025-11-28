package com.starwatchx.util;

/**
 * Utility math helpers.
 */
public final class MathUtils {

    private MathUtils() {
        // Utility class
    }

    public static float clamp(float value, float min, float max) {
        if (min > max) {
            throw new IllegalArgumentException("min must be <= max");
        }
        return Math.max(min, Math.min(max, value));
    }

    public static float iou(float x1, float y1, float w1, float h1,
                            float x2, float y2, float w2, float h2) {
        float ax1 = x1;
        float ay1 = y1;
        float ax2 = x1 + w1;
        float ay2 = y1 + h1;

        float bx1 = x2;
        float by1 = y2;
        float bx2 = x2 + w2;
        float by2 = y2 + h2;

        float interWidth = Math.max(0f, Math.min(ax2, bx2) - Math.max(ax1, bx1));
        float interHeight = Math.max(0f, Math.min(ay2, by2) - Math.max(ay1, by1));
        float intersection = interWidth * interHeight;

        float union = (w1 * h1) + (w2 * h2) - intersection;
        if (union <= 0f) {
            return 0f;
        }
        return intersection / union;
    }
}

