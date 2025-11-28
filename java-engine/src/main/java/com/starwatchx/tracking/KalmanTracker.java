package com.starwatchx.tracking;

import com.starwatchx.detection.DetectedObject;

import java.util.UUID;

/**
 * Maintains constant velocity tracking for a single object.
 */
public class KalmanTracker {

    private static final float PROCESS_NOISE = 1e-2f;
    private static final float MEASUREMENT_NOISE = 1e-1f;

    private final String trackId;

    private float x;
    private float y;
    private float width;
    private float height;
    private float velocityX;
    private float velocityY;
    private long lastTimestamp;
    private long lastUpdateTimestamp;
    private boolean initialized;

    public KalmanTracker() {
        this.trackId = UUID.randomUUID().toString();
    }

    public String getTrackId() {
        return trackId;
    }

    public long getLastUpdateTimestamp() {
        return lastUpdateTimestamp;
    }

    public void predict(long timestamp) {
        if (!initialized) {
            return;
        }

        float deltaSeconds = computeDeltaSeconds(lastTimestamp, timestamp);
        lastTimestamp = timestamp;

        x += velocityX * deltaSeconds;
        y += velocityY * deltaSeconds;
        velocityX *= (1f - PROCESS_NOISE);
        velocityY *= (1f - PROCESS_NOISE);
    }

    public void update(DetectedObject detection) {
        long timestamp = detection.getTimestamp();
        if (!initialized) {
            initializeFromDetection(detection);
            lastTimestamp = timestamp;
            lastUpdateTimestamp = timestamp;
            initialized = true;
            return;
        }

        float deltaSeconds = computeDeltaSeconds(lastTimestamp, timestamp);
        lastTimestamp = timestamp;
        lastUpdateTimestamp = timestamp;

        float measurementX = detection.getX();
        float measurementY = detection.getY();
        float measurementWidth = detection.getWidth();
        float measurementHeight = detection.getHeight();

        float alpha = 1f - MEASUREMENT_NOISE;
        x = alpha * measurementX + MEASUREMENT_NOISE * x;
        y = alpha * measurementY + MEASUREMENT_NOISE * y;
        width = alpha * measurementWidth + MEASUREMENT_NOISE * width;
        height = alpha * measurementHeight + MEASUREMENT_NOISE * height;

        if (deltaSeconds > 0f) {
            float newVx = (measurementX - x) / deltaSeconds;
            float newVy = (measurementY - y) / deltaSeconds;
            velocityX = alpha * newVx + MEASUREMENT_NOISE * velocityX;
            velocityY = alpha * newVy + MEASUREMENT_NOISE * velocityY;
        }
    }

    public TrackerState getState() {
        return new TrackerState(x, y, width, height, velocityX, velocityY);
    }

    private void initializeFromDetection(DetectedObject detection) {
        this.x = detection.getX();
        this.y = detection.getY();
        this.width = detection.getWidth();
        this.height = detection.getHeight();
        this.velocityX = 0f;
        this.velocityY = 0f;
    }

    private static float computeDeltaSeconds(long previousTimestamp, long currentTimestamp) {
        if (previousTimestamp == 0L) {
            return 0f;
        }
        return Math.max(0f, (currentTimestamp - previousTimestamp) / 1000f);
    }

    /**
     * Simple tracker state container.
     */
    public static class TrackerState {
        private final float x;
        private final float y;
        private final float width;
        private final float height;
        private final float velocityX;
        private final float velocityY;

        public TrackerState(float x,
                            float y,
                            float width,
                            float height,
                            float velocityX,
                            float velocityY) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.velocityX = velocityX;
            this.velocityY = velocityY;
        }

        public float getCenterX() {
            return x + width / 2f;
        }

        public float getCenterY() {
            return y + height / 2f;
        }

        public float getX() {
            return x;
        }

        public float getY() {
            return y;
        }

        public float getWidth() {
            return width;
        }

        public float getHeight() {
            return height;
        }

        public float getVelocityX() {
            return velocityX;
        }

        public float getVelocityY() {
            return velocityY;
        }
    }
}
