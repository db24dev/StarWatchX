package com.starwatchx.telemetry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents telemetry payload for a single frame per camera.
 */
public class TelemetryPacket {

    private String cameraId;
    private long timestamp;
    private List<ObjectTelemetry> objects;

    public TelemetryPacket() {
        this.objects = new ArrayList<>();
    }

    public TelemetryPacket(String cameraId, long timestamp, List<ObjectTelemetry> objects) {
        this.cameraId = cameraId;
        this.timestamp = timestamp;
        this.objects = new ArrayList<>(objects);
    }

    public String getCameraId() {
        return cameraId;
    }

    public void setCameraId(String cameraId) {
        this.cameraId = cameraId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public List<ObjectTelemetry> getObjects() {
        return Collections.unmodifiableList(objects);
    }

    public void setObjects(List<ObjectTelemetry> objects) {
        this.objects = new ArrayList<>(objects);
    }

    public void addObject(ObjectTelemetry objectTelemetry) {
        this.objects.add(objectTelemetry);
    }

    /**
     * Object-level telemetry data.
     */
    public static class ObjectTelemetry {
        private String id;
        private float x;
        private float y;
        private float width;
        private float height;
        private float vx;
        private float vy;
        private String label;
        private float confidence;

        public ObjectTelemetry() {
        }

        public ObjectTelemetry(String id,
                               float x,
                               float y,
                               float width,
                               float height,
                               float vx,
                               float vy,
                               String label,
                               float confidence) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.vx = vx;
            this.vy = vy;
            this.label = label;
            this.confidence = confidence;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public float getX() {
            return x;
        }

        public void setX(float x) {
            this.x = x;
        }

        public float getY() {
            return y;
        }

        public void setY(float y) {
            this.y = y;
        }

        public float getWidth() {
            return width;
        }

        public void setWidth(float width) {
            this.width = width;
        }

        public float getHeight() {
            return height;
        }

        public void setHeight(float height) {
            this.height = height;
        }

        public float getVx() {
            return vx;
        }

        public void setVx(float vx) {
            this.vx = vx;
        }

        public float getVy() {
            return vy;
        }

        public void setVy(float vy) {
            this.vy = vy;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public float getConfidence() {
            return confidence;
        }

        public void setConfidence(float confidence) {
            this.confidence = confidence;
        }
    }
}