package com.starwatchx.trajectory;

import com.starwatchx.tracking.TrackerManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides basic constant-velocity trajectory prediction for tracked objects.
 */
public class TrajectoryPredictor {

    private static final float[] DEFAULT_HORIZONS_SECONDS = {0.5f, 1.0f};

    public TrajectoryPredictor() {
    }

    public List<Point> predictPath(TrackerManager.TrackerSnapshot snapshot) {
        return predictPath(snapshot, DEFAULT_HORIZONS_SECONDS);
    }

    public List<Point> predictPath(TrackerManager.TrackerSnapshot snapshot, float[] horizonsSeconds) {
        List<Point> predictions = new ArrayList<>();
        if (snapshot == null || horizonsSeconds == null) {
            return predictions;
        }

        float currentX = snapshot.getX() + snapshot.getWidth() / 2f;
        float currentY = snapshot.getY() + snapshot.getHeight() / 2f;
        float vx = snapshot.getVelocityX();
        float vy = snapshot.getVelocityY();

        predictions.add(new Point(currentX, currentY, 0f));
        for (float horizon : horizonsSeconds) {
            float predictedX = currentX + vx * horizon;
            float predictedY = currentY + vy * horizon;
            predictions.add(new Point(predictedX, predictedY, horizon));
        }
        return predictions;
    }

    /**
     * Represents a point in 2D space with optional prediction horizon metadata.
     */
    public static class Point {
        private final float x;
        private final float y;
        private final float horizonSeconds;

        public Point(float x, float y, float horizonSeconds) {
            this.x = x;
            this.y = y;
            this.horizonSeconds = horizonSeconds;
        }

        public float getX() {
            return x;
        }

        public float getY() {
            return y;
        }

        public float getHorizonSeconds() {
            return horizonSeconds;
        }
    }
}
