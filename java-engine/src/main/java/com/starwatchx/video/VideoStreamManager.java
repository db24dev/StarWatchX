package com.starwatchx.video;

import com.starwatchx.config.EngineConfig;
import org.bytedeco.opencv.global.opencv_videoio;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages ingestion of multiple video streams and delegates processing.
 * Video sources are configured via {@link EngineConfig}. Place feeds in
 * dashboard/public/videos/*.mp4 (ships with repo) or point to real camera URLs.
 */
public class VideoStreamManager {

    private final EngineConfig engineConfig;
    private final FrameProcessor frameProcessor;
    private final ExecutorService executorService;
    private final List<CameraWorker> workers;
    private final List<Future<?>> workerFutures;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public VideoStreamManager(EngineConfig engineConfig, FrameProcessor frameProcessor) {
        this.engineConfig = engineConfig;
        this.frameProcessor = frameProcessor;
        this.executorService = Executors.newCachedThreadPool();
        this.workers = new ArrayList<>();
        this.workerFutures = new ArrayList<>();
    }

    public synchronized void startAll() {
        if (running.get()) {
            return;
        }
        running.set(true);

        List<EngineConfig.CameraConfig> cameras = engineConfig.getCameras();
        if (cameras == null || cameras.isEmpty()) {
            System.err.println("[VideoStream] No cameras configured. Nothing to do.");
            running.set(false);
            return;
        }

        for (EngineConfig.CameraConfig cameraConfig : cameras) {
            String resolvedSource = resolveSource(cameraConfig);
            System.out.println("[VideoStream] " + cameraConfig.getCameraId() + " -> " + resolvedSource);
            CameraWorker worker = new CameraWorker(cameraConfig, resolvedSource);
            workers.add(worker);
            workerFutures.add(executorService.submit(worker));
        }
    }

    public synchronized void stopAll() {
        if (!running.get()) {
            return;
        }
        running.set(false);

        for (CameraWorker worker : workers) {
            worker.stop();
        }
        workers.clear();

        for (Future<?> future : workerFutures) {
            future.cancel(true);
        }
        workerFutures.clear();

        executorService.shutdownNow();
    }

    public boolean isRunning() {
        return running.get();
    }

    private class CameraWorker implements Runnable {
        private final EngineConfig.CameraConfig cameraConfig;
        private final String resolvedSource;
        private final AtomicBoolean active = new AtomicBoolean(true);

        CameraWorker(EngineConfig.CameraConfig cameraConfig, String resolvedSource) {
            this.cameraConfig = cameraConfig;
            this.resolvedSource = resolvedSource;
        }

        @Override
        public void run() {
            System.out.println("[VideoStream] Worker starting for " + cameraConfig.getCameraId());
            System.out.flush();
            
            VideoCapture capture = null;
            Mat frame = null;
            
            try {
                System.out.println("[VideoStream] Creating VideoCapture for " + cameraConfig.getCameraId());
                System.out.flush();
                
                capture = new VideoCapture();
                boolean opened;
                
                if (resolvedSource.matches("\\d+")) {
                    opened = capture.open(Integer.parseInt(resolvedSource));
                } else {
                    System.out.println("[VideoStream] Trying to open: " + resolvedSource);
                    System.out.flush();
                    
                    // Try FFMPEG backend first
                    opened = capture.open(resolvedSource, opencv_videoio.CAP_FFMPEG);
                    if (opened) {
                        System.out.println("[VideoStream] Opened with FFMPEG backend");
                    } else {
                        System.out.println("[VideoStream] FFMPEG failed, trying default backend...");
                        opened = capture.open(resolvedSource);
                        if (opened) {
                            System.out.println("[VideoStream] Opened with default backend");
                        } else {
                            System.out.println("[VideoStream] Default failed, trying ANY backend...");
                            opened = capture.open(resolvedSource, opencv_videoio.CAP_ANY);
                        }
                    }
                }
                System.out.flush();

                if (!opened) {
                    System.err.println("[VideoStream] Unable to open source: " + resolvedSource);
                    System.err.flush();
                    return;
                }
                System.out.println("[VideoStream] Opened source for " + cameraConfig.getCameraId());
                System.out.flush();

                frame = new Mat();
                int frameCount = 0;
                
                while (active.get() && capture.isOpened()) {
                    boolean success = capture.read(frame);
                    if (!success || frame.empty()) {
                        System.out.println("[VideoStream] " + cameraConfig.getCameraId() + " looping video...");
                        capture.set(opencv_videoio.CAP_PROP_POS_FRAMES, 0);
                        continue;
                    }

                    frameCount++;
                    if (frameCount % 30 == 1) {
                        System.out.println("[VideoStream] " + cameraConfig.getCameraId() + " frame #" + frameCount);
                        System.out.flush();
                    }

                    Mat frameClone = frame.clone();
                    long timestamp = System.currentTimeMillis();
                    try {
                        frameProcessor.processFrame(frameClone, cameraConfig.getCameraId(), timestamp);
                    } finally {
                        frameClone.close();
                    }

                    if (cameraConfig.getTargetFps() > 0) {
                        Thread.sleep(Math.max(0, 1000 / cameraConfig.getTargetFps()));
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("[VideoStream] Worker interrupted: " + cameraConfig.getCameraId());
            } catch (Throwable t) {
                System.err.println("[VideoStream] FATAL ERROR in " + cameraConfig.getCameraId() + ": " + t);
                t.printStackTrace();
                System.err.flush();
            } finally {
                if (frame != null) frame.close();
                if (capture != null) capture.release();
                System.out.println("[VideoStream] Worker stopped: " + cameraConfig.getCameraId());
                System.out.flush();
            }
        }

        void stop() {
            active.set(false);
        }
    }

    private String resolveSource(EngineConfig.CameraConfig cameraConfig) {
        return switch (cameraConfig.getCameraId()) {
            case "CAM-1" -> "/Users/darioberretta/Desktop/CODE/StarWatchX/dashboard/public/videos/cam1.mp4";
            case "CAM-2" -> "/Users/darioberretta/Desktop/CODE/StarWatchX/dashboard/public/videos/cam2.mp4";
            case "CAM-3" -> "/Users/darioberretta/Desktop/CODE/StarWatchX/dashboard/public/videos/cam3.mp4";
            default -> cameraConfig.getSource();
        };
    }
}

