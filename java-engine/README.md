## Java Engine Overview
The engine is a modular pipeline that ingests frames, detects/associates objects, predicts trajectories, and pushes telemetry to the WebSocket server. It now uses the JavaCPP/Bytedeco OpenCV + FFmpeg presets, so no system-level OpenCV installation is required—Gradle/Maven will pull the bundled native libraries automatically.

## Package Map
- `com.starwatchx` – `App.java` bootstraps config, detectors, trackers, telemetry, and video streams.
- `config` – `EngineConfig` exposes model path, telemetry port, thresholds, and camera list.
- `detection` – `ObjectDetector`, `ONNXModelLoader`, `DetectedObject` handle preprocessing, ONNX inference, and fallback detections.
- `tracking` – `KalmanTracker` (per-track filter) and `TrackerManager` (association + lifecycle) deliver stable IDs.
- `trajectory` – `TrajectoryPredictor` extrapolates constant-velocity paths for HUD + telemetry.
- `hud` – `HUDOverlay` draws boxes, labels, and predicted paths on frames.
- `video` – `VideoStreamManager` spawns per-camera workers; `FrameProcessor` orchestrates detection → tracking → telemetry for each frame.
- `telemetry` – `TelemetryPacket` + `TelemetryServer` serialize/broadcast JSON packets consumed by the dashboard.
- `util` – Math/thread helpers.

## Extending the Pipeline
- **Custom detectors:** Implement your model loader inside `detection`, then swap it into `App` before `FrameProcessor` is created. Ensure detections conform to `DetectedObject`.
- **Alternate trackers:** Add a new tracker class in `tracking`, expose it via `TrackerManager`, and keep the `TrackerSnapshot` contract consistent for HUD + telemetry.
- **Telemetry enrichment:** Extend `TelemetryPacket.ObjectTelemetry` with additional fields (e.g., covariance, classification metadata) and update the dashboard WebSocket client accordingly.
- **Additional outputs:** Tap into `FrameProcessor` after HUD rendering to write frames to disk or publish to RTSP if you need downstream processing.


