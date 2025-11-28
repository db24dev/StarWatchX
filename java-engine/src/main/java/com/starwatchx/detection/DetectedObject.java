package com.starwatchx.detection;

/**
 * Data transfer object representing a detection.
 */
public class DetectedObject {

    private final String cameraId;
    private final int classId;
    private final String label;
    private final float confidence;
    private final float x;
    private final float y;
    private final float width;
    private final float height;
    private final long timestamp;

    public DetectedObject(String cameraId,
                          int classId,
                          String label,
                          float confidence,
                          float x,
                          float y,
                          float width,
                          float height,
                          long timestamp) {
        this.cameraId = cameraId;
        this.classId = classId;
        this.label = label;
        this.confidence = confidence;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.timestamp = timestamp;
    }

    public String getCameraId() {
        return cameraId;
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

    public long getTimestamp() {
        return timestamp;
    }

}