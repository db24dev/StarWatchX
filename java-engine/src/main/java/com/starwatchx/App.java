package com.starwatchx;

import com.starwatchx.config.EngineConfig;
import com.starwatchx.detection.ObjectDetector;
import com.starwatchx.detection.ONNXModelLoader;
import com.starwatchx.hud.HUDOverlay;
import com.starwatchx.telemetry.TelemetryServer;
import com.starwatchx.tracking.TrackerManager;
import com.starwatchx.trajectory.TrajectoryPredictor;
import com.starwatchx.video.FrameProcessor;
import com.starwatchx.video.VideoStreamManager;

/**
 * Entry point for the StarWatch-X Java engine.
 */
public final class App {

    private App() {
    }

    public static void main(String[] args) {
        EngineConfig config = EngineConfig.loadDefault();

        ONNXModelLoader modelLoader = new ONNXModelLoader();
        ObjectDetector objectDetector = new ObjectDetector(modelLoader);
        objectDetector.initialize(config.getModelPath());
        objectDetector.setThresholds(config.getDetectionConfidence(), config.getDetectionNms());

        TrackerManager trackerManager = new TrackerManager();
        TrajectoryPredictor predictor = new TrajectoryPredictor();
        HUDOverlay hudOverlay = new HUDOverlay();
        TelemetryServer telemetryServer = createTelemetryServer(config);
        FrameProcessor frameProcessor = new FrameProcessor(
            objectDetector,
            trackerManager,
            predictor,
            hudOverlay,
            telemetryServer
        );
        VideoStreamManager streamManager = createVideoStreamManager(config, frameProcessor);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[App] Shutting down...");
            streamManager.stopAll();
            telemetryServer.stop();
            modelLoader.close();
        }));

        telemetryServer.start();
        streamManager.startAll();

        try {
            Thread.currentThread().join();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private static TelemetryServer createTelemetryServer(EngineConfig config) {
        return new TelemetryServer(config.getTelemetryPort());
    }

    private static VideoStreamManager createVideoStreamManager(EngineConfig config,
                                                               FrameProcessor frameProcessor) {
        return new VideoStreamManager(config, frameProcessor);
    }
}
