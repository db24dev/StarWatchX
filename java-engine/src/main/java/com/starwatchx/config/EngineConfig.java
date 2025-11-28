package com.starwatchx.config;

import java.util.List;
import java.util.Objects;

/**
 * Holds configuration options for the StarWatch-X engine.
 */
public class EngineConfig {

    private final String modelPath;
    private final int telemetryPort;
    private final float detectionConfidence;
    private final float detectionNms;
    private final List<CameraConfig> cameras;

    private EngineConfig(String modelPath,
                         int telemetryPort,
                         float detectionConfidence,
                         float detectionNms,
                         List<CameraConfig> cameras) {
        this.modelPath = modelPath;
        this.telemetryPort = telemetryPort;
        this.detectionConfidence = detectionConfidence;
        this.detectionNms = detectionNms;
        this.cameras = cameras;
    }

    public static EngineConfig loadDefault() {
        return new EngineConfig(
            "src/main/resources/model/starwatchx_yolov8.onnx",
            8081,
            0.25f,
            0.45f,
            List.of(
                new CameraConfig("CAM-1", "CAM-1", 30),
                new CameraConfig("CAM-2", "CAM-2", 30),
                new CameraConfig("CAM-3", "CAM-3", 30)
            )
        );
    }

    public String getModelPath() {
        return modelPath;
    }

    public int getTelemetryPort() {
        return telemetryPort;
    }

    public float getDetectionConfidence() {
        return detectionConfidence;
    }

    public float getDetectionNms() {
        return detectionNms;
    }

    public List<CameraConfig> getCameras() {
        return cameras;
    }

    /**
     * Represents a single camera/video source definition.
     */
    public static class CameraConfig {
        private final String cameraId;
        private final String source;
        private final int targetFps;

        public CameraConfig(String cameraId, String source, int targetFps) {
            this.cameraId = Objects.requireNonNull(cameraId, "cameraId");
            this.source = Objects.requireNonNull(source, "source");
            this.targetFps = targetFps;
        }

        public String getCameraId() {
            return cameraId;
        }

        public String getSource() {
            return source;
        }

        public int getTargetFps() {
            return targetFps;
        }
    }
}