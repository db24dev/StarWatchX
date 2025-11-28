package com.starwatchx.video;

import com.starwatchx.detection.DetectedObject;
import com.starwatchx.detection.ObjectDetector;
import com.starwatchx.hud.HUDOverlay;
import com.starwatchx.telemetry.TelemetryPacket;
import com.starwatchx.telemetry.TelemetryServer;
import com.starwatchx.tracking.TrackerManager;
import com.starwatchx.trajectory.TrajectoryPredictor;
import org.bytedeco.opencv.opencv_core.Mat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Executes detection, tracking, trajectory prediction, and HUD overlay for frames.
 */
public class FrameProcessor {

    private final ObjectDetector objectDetector;
    private final TrackerManager trackerManager;
    private final TrajectoryPredictor trajectoryPredictor;
    private final HUDOverlay hudOverlay;
    private final TelemetryServer telemetryServer;

    public FrameProcessor(ObjectDetector objectDetector,
                          TrackerManager trackerManager,
                          TrajectoryPredictor trajectoryPredictor,
                          HUDOverlay hudOverlay,
                          TelemetryServer telemetryServer) {
        this.objectDetector = objectDetector;
        this.trackerManager = trackerManager;
        this.trajectoryPredictor = trajectoryPredictor;
        this.hudOverlay = hudOverlay;
        this.telemetryServer = telemetryServer;
    }

    public void processFrame(Mat frame, String cameraId, long timestamp) {
        if (frame == null || frame.empty()) {
            return;
        }

        TelemetryPacket packet = null;
        try {
            List<DetectedObject> detections = objectDetector.detect(frame, cameraId, timestamp);
            if (detections == null) {
                detections = Collections.emptyList();
            }

            List<TrackerManager.TrackerSnapshot> snapshots =
                trackerManager.update(cameraId, detections, timestamp);

            Map<String, List<TrajectoryPredictor.Point>> predictedPaths = new HashMap<>();
            for (TrackerManager.TrackerSnapshot snapshot : snapshots) {
                predictedPaths.put(snapshot.getTrackId(), trajectoryPredictor.predictPath(snapshot));
            }

            hudOverlay.draw(frame, snapshots, predictedPaths);

            packet = buildTelemetryPacket(cameraId, timestamp, snapshots);
        } catch (Exception ex) {
            System.err.println("[FrameProcessor] Error processing frame for " + cameraId + ": " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            if (packet == null) {
                packet = buildTelemetryPacket(cameraId, timestamp, Collections.emptyList());
            }
            telemetryServer.broadcastTelemetry(packet);
        }
    }

    private TelemetryPacket buildTelemetryPacket(String cameraId,
                                                 long timestamp,
                                                 List<TrackerManager.TrackerSnapshot> snapshots) {
        TelemetryPacket packet = new TelemetryPacket();
        packet.setCameraId(cameraId);
        packet.setTimestamp(timestamp);

        List<TelemetryPacket.ObjectTelemetry> objects = new ArrayList<>();
        for (TrackerManager.TrackerSnapshot snapshot : snapshots) {
            TelemetryPacket.ObjectTelemetry objectTelemetry = new TelemetryPacket.ObjectTelemetry();
            objectTelemetry.setId(snapshot.getTrackId());
            objectTelemetry.setLabel(snapshot.getLabel());
            objectTelemetry.setConfidence(snapshot.getConfidence());
            objectTelemetry.setX(snapshot.getX());
            objectTelemetry.setY(snapshot.getY());
            objectTelemetry.setWidth(snapshot.getWidth());
            objectTelemetry.setHeight(snapshot.getHeight());
            objectTelemetry.setVx(snapshot.getVelocityX());
            objectTelemetry.setVy(snapshot.getVelocityY());
            objects.add(objectTelemetry);
        }
        packet.setObjects(objects);
        return packet;
    }
}
