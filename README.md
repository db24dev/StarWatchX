# STARWATCH-X
**Multi-camera space situational awareness prototype built with Java, Python, and TypeScript.**

---

## Overview
STARWATCH-X ingests simultaneous video feeds, detects and tracks orbital objects, predicts near-term trajectories, and streams telemetry to a mission-control dashboard. Inspired by aerospace SSA systems, it demonstrates a cohesive, portfolio-ready stack where computer vision, tracking, and telemetry UX all align.

### Core Features
- Multi-camera video ingestion with OpenCV and per-stream workers.
- YOLOv8 ONNX inference for detections with automatic fake-detection fallback.
- Kalman-based tracking plus constant-velocity trajectory prediction.
- HUD overlay rendering + telemetry packets broadcast over WebSockets.
- Next.js 14 dashboard with TailwindCSS mission-control styling.
- Python tooling to fine-tune YOLOv8 and export ONNX artifacts.

---

## Architecture Diagram (Text)
```
[Video Files / Live Cameras]
        ↓
[Java Engine: OpenCV + ONNX Runtime + Tracking + HUD]
        ↓
[TelemetryServer: WebSocket JSON stream]
        ↓
[Next.js Dashboard: Canvas HUD + Telemetry Panels]
```

**Layer Breakdown**
1. **Java Engine** – Handles ingestion, ONNX inference, tracking, trajectory prediction, HUD overlays, and telemetry serialization.
2. **Python ML** – Fine-tunes YOLOv8 (Ultralytics/Torch) and exports ONNX models consumed by the Java runtime.
3. **Next.js Dashboard** – Connects to the telemetry WebSocket, renders canvas feeds, and surfaces mission metrics.

---

## Tech Stack
- **Languages:** Java 17, Python 3.10+, TypeScript/React.
- **Java libraries:** OpenCV, ONNX Runtime, Java-WebSocket, Gson.
- **Python libraries:** ultralytics YOLOv8, torch/torchvision, onnx, onnxruntime.
- **Frontend:** Next.js 14, TailwindCSS, React Server Components.
- **Build tools:** Gradle (Java), npm (dashboard), virtualenv/pip (Python).

---

## Directory Structure
- `java-engine/` – Real-time engine (OpenCV capture, ONNX inference, tracking, telemetry).
- `python-ml/` – YOLO training/export scripts, dataset layout, venv automation.
- `dashboard/` – Next.js mission-control HUD consuming telemetry.
- `dashboard/public/videos/` – Drop in MP4 feeds referenced by the default `EngineConfig`.
- `docs/*.md` – Focused guides (OpenCV setup, ONNX instructions, etc.).

---

## Setup & Usage
1. **Prerequisites**
   - Java 17+, Python 3.10+, Node.js 18+, npm.
   - OpenCV native libs installed (macOS: `brew install opencv`, Ubuntu: `sudo apt install libopencv-dev`).
2. **Clone**
   ```bash
   git clone https://github.com/<you>/starwatch-x.git
   cd starwatch-x
   ```
3. **Model Prep**
   ```bash
   cd python-ml
   ./run_training_and_export.sh
   ```
   This creates a venv, installs ultralytics/torch, runs a demo training loop, and exports `starwatchx_yolov8.onnx` to `java-engine/src/main/resources/model/`.
4. **Add Demo Footage**
   - Copy MP4s into `dashboard/public/videos/` (named `cam1.mp4`, `cam2.mp4`, `cam3.mp4`).
   - Update `EngineConfig.loadDefault()` if your filenames or sources differ.
5. **Run Java Engine**
   ```bash
   cd java-engine
   ./gradlew run
   ```
   Make sure OpenCV native libs are discoverable (set `-Djava.library.path` if needed). Telemetry WebSocket defaults to port 8081.
6. **Run Dashboard**
   ```bash
   cd dashboard
   npm install
   npm run dev
   # visit http://localhost:3000
   ```
7. **Expected Behavior**
   - ONNX model present → real detections, tracked IDs, trajectory overlays, and telemetry streaming.
   - ONNX model missing → automatic “test-object” fallback ensures the telemetry/dash experience still works.

---

## Troubleshooting
- **OpenCV native lib not found** – Install OpenCV and expose `libopencv_java` via `java.library.path` or OS PATH.
- **ONNX model missing** – Re-run `python-ml/run_training_and_export.sh` or copy a valid ONNX file into `java-engine/src/main/resources/model/`.
- **WebSocket not connecting** – Ensure the Java engine is running on port 8081 or set `NEXT_PUBLIC_TELEMETRY_URL` before `npm run dev`.
- **Node/TypeScript errors** – Delete `dashboard/node_modules`, rerun `npm install`, and confirm Node 18+.

---

## Security & Performance Notes
- Prototype only: no auth, encryption, or hostile-environment hardening.
- CPU inference by default; production systems should use GPU-backed ONNX Runtime, batching, and advanced trackers (JPDA, DeepSORT, etc.).
- Video ingestion uses simple thread pools—scale-out deployments would require stream fan-out services or messaging layers.

---

## Why This Project Matters
STARWATCH-X showcases the ability to integrate Java CV pipelines, Python ML workflows, and modern TypeScript dashboards—mirroring SSA solutions built by companies like TRL11. It highlights real-time thinking, telemetry contract design, and operator-focused UX.

---

## Resume Bullets (Copy/Paste)
- Built STARWATCH-X, a multi-camera space-situational-awareness prototype combining Java (OpenCV + ONNX Runtime), Python (YOLOv8), and a TypeScript/Next.js mission-control dashboard.
- Implemented a real-time detection → tracking → trajectory prediction pipeline with WebSocket telemetry streaming into a custom aerospace-style HUD.
- Designed and trained a YOLOv8 model, exported it to ONNX, and integrated it into a Java inference engine with a graceful fake-detection fallback.
- Delivered an end-to-end system capable of ingesting multiple video feeds, overlaying HUD elements, and visualizing tracks live for operator workflows.

---

> Run the engine, launch the dashboard, and watch STARWATCH-X paint the orbital picture.

