package com.starwatchx.tracking;

import com.starwatchx.detection.DetectedObject;
import com.starwatchx.util.MathUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Coordinates multiple trackers and associates detections to tracks.
 */
public class TrackerManager {

    private static final float MAX_ASSOCIATION_DISTANCE = 150f;
    private static final float MIN_IOU_FOR_ASSOCIATION = 0.1f;
    private static final long TRACKER_TTL_MS = 2_000L;

    private final Map<String, List<TrackerEntry>> trackersByCamera = new HashMap<>();

    public TrackerManager() {
    }

    public List<TrackerSnapshot> update(String cameraId,
                                        List<DetectedObject> detections,
                                        long timestamp) {
        List<DetectedObject> safeDetections = detections != null ? detections : Collections.emptyList();
        List<TrackerEntry> trackers = trackersByCamera.computeIfAbsent(cameraId, key -> new ArrayList<>());

        // Predict existing trackers
        for (TrackerEntry entry : trackers) {
            entry.tracker.predict(timestamp);
        }

        // Associate detections to trackers
        boolean[] detectionMatched = new boolean[safeDetections.size()];
        List<Association> associations = buildAssociations(trackers, safeDetections);
        associations.sort(Comparator.comparingDouble(a -> a.distance));

        for (Association association : associations) {
            if (association.distance > MAX_ASSOCIATION_DISTANCE) {
                continue;
            }
            if (detectionMatched[association.detectionIndex]) {
                continue;
            }

            TrackerEntry entry = trackers.get(association.trackerIndex);
            if (entry.isStale(timestamp, TRACKER_TTL_MS)) {
                continue;
            }

            DetectedObject detection = safeDetections.get(association.detectionIndex);
            float[] trackerBounds = entry.getBounds();
            float iou = MathUtils.iou(
                trackerBounds[0], trackerBounds[1], trackerBounds[2], trackerBounds[3],
                detection.getX(), detection.getY(), detection.getWidth(), detection.getHeight());

            if (iou < MIN_IOU_FOR_ASSOCIATION && association.distance > MAX_ASSOCIATION_DISTANCE / 2f) {
                continue;
            }

            entry.tracker.update(detection);
            entry.lastUpdateTimestamp = timestamp;
            entry.lastDetection = detection;
            detectionMatched[association.detectionIndex] = true;
        }

        // Create new trackers for unmatched detections
        for (int i = 0; i < safeDetections.size(); i++) {
            if (detectionMatched[i]) {
                continue;
            }
            DetectedObject detection = safeDetections.get(i);
            KalmanTracker tracker = new KalmanTracker();
            tracker.update(detection);
            TrackerEntry entry = new TrackerEntry(tracker, detection, timestamp);
            trackers.add(entry);
        }

        // Remove stale trackers
        Iterator<TrackerEntry> iterator = trackers.iterator();
        while (iterator.hasNext()) {
            TrackerEntry entry = iterator.next();
            if (entry.isStale(timestamp, TRACKER_TTL_MS)) {
                iterator.remove();
            }
        }

        // Build snapshots for HUD/telemetry
        List<TrackerSnapshot> snapshots = new ArrayList<>();
        for (TrackerEntry entry : trackers) {
            KalmanTracker.TrackerState state = entry.tracker.getState();
            snapshots.add(new TrackerSnapshot(
                entry.tracker.getTrackId(),
                entry.lastDetection != null ? entry.lastDetection.getClassId() : -1,
                entry.lastDetection != null ? entry.lastDetection.getLabel() : "object",
                entry.lastDetection != null ? entry.lastDetection.getConfidence() : 0f,
                state.getX(),
                state.getY(),
                state.getWidth(),
                state.getHeight(),
                state.getVelocityX(),
                state.getVelocityY(),
                entry.lastUpdateTimestamp
            ));
        }
        return snapshots;
    }

    public Collection<TrackerSnapshot> getActiveTrackers(String cameraId, long timestamp) {
        List<TrackerEntry> entries = trackersByCamera.get(cameraId);
        if (entries == null) {
            return Collections.emptyList();
        }
        List<TrackerSnapshot> snapshots = new ArrayList<>();
        for (TrackerEntry entry : entries) {
            if (entry.isStale(timestamp, TRACKER_TTL_MS)) {
                continue;
            }
            KalmanTracker.TrackerState state = entry.tracker.getState();
            snapshots.add(new TrackerSnapshot(
                entry.tracker.getTrackId(),
                entry.lastDetection != null ? entry.lastDetection.getClassId() : -1,
                entry.lastDetection != null ? entry.lastDetection.getLabel() : "object",
                entry.lastDetection != null ? entry.lastDetection.getConfidence() : 0f,
                state.getX(),
                state.getY(),
                state.getWidth(),
                state.getHeight(),
                state.getVelocityX(),
                state.getVelocityY(),
                entry.lastUpdateTimestamp
            ));
        }
        return snapshots;
    }

    private static List<Association> buildAssociations(List<TrackerEntry> trackers,
                                                       List<DetectedObject> detections) {
        List<Association> associations = new ArrayList<>();
        for (int trackerIndex = 0; trackerIndex < trackers.size(); trackerIndex++) {
            TrackerEntry entry = trackers.get(trackerIndex);
            KalmanTracker.TrackerState state = entry.tracker.getState();
            float trackerCx = state.getCenterX();
            float trackerCy = state.getCenterY();

            for (int detectionIndex = 0; detectionIndex < detections.size(); detectionIndex++) {
                DetectedObject detection = detections.get(detectionIndex);
                float detectionCx = detection.getX() + detection.getWidth() / 2f;
                float detectionCy = detection.getY() + detection.getHeight() / 2f;
                float distance = distance(trackerCx, trackerCy, detectionCx, detectionCy);
                associations.add(new Association(trackerIndex, detectionIndex, distance));
            }
        }
        return associations;
    }

    private static float distance(float x1, float y1, float x2, float y2) {
        float dx = x1 - x2;
        float dy = y1 - y2;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private static class TrackerEntry {
        private final KalmanTracker tracker;
        private DetectedObject lastDetection;
        private long lastUpdateTimestamp;

        TrackerEntry(KalmanTracker tracker, DetectedObject lastDetection, long timestamp) {
            this.tracker = tracker;
            this.lastDetection = lastDetection;
            this.lastUpdateTimestamp = timestamp;
        }

        boolean isStale(long timestamp, long ttlMs) {
            return lastUpdateTimestamp > 0 && (timestamp - lastUpdateTimestamp) > ttlMs;
        }

        float[] getBounds() {
            KalmanTracker.TrackerState state = tracker.getState();
            return new float[]{state.getX(), state.getY(), state.getWidth(), state.getHeight()};
        }
    }

    private record Association(int trackerIndex, int detectionIndex, float distance) {}

    /**
     * Snapshot of a tracker for downstream components.
     */
    public static class TrackerSnapshot {
        private final String trackId;
        private final int classId;
        private final String label;
        private final float confidence;
        private final float x;
        private final float y;
        private final float width;
        private final float height;
        private final float velocityX;
        private final float velocityY;
        private final long lastUpdateTimestamp;

        public TrackerSnapshot(String trackId,
                               int classId,
                               String label,
                               float confidence,
                               float x,
                               float y,
                               float width,
                               float height,
                               float velocityX,
                               float velocityY,
                               long lastUpdateTimestamp) {
            this.trackId = trackId;
            this.classId = classId;
            this.label = label;
            this.confidence = confidence;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.velocityX = velocityX;
            this.velocityY = velocityY;
            this.lastUpdateTimestamp = lastUpdateTimestamp;
        }

        public String getTrackId() {
            return trackId;
        }

        public int getClassId() {
            return classId;
        }

        public String getLabel() {
            return label;
        }

        public float getConfidence() {
            return confidence;
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

        public long getLastUpdateTimestamp() {
            return lastUpdateTimestamp;
        }
    }
}

