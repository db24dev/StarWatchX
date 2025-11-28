package com.starwatchx.hud;

import com.starwatchx.tracking.TrackerManager;
import com.starwatchx.trajectory.TrajectoryPredictor;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Point;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.Size;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Renders heads-up display elements on processed frames.
 */
public class HUDOverlay {

    private static final Scalar COLOR_BOX = new Scalar((double) 0, (double) 255, (double) 0, 0.0);
    private static final Scalar COLOR_TEXT_BG = new Scalar((double) 0, (double) 0, (double) 0, 0.0);
    private static final Scalar COLOR_TEXT = new Scalar((double) 255, (double) 255, (double) 255, 0.0);
    private static final Scalar COLOR_PATH = new Scalar((double) 255, (double) 255, (double) 0, 0.0);

    public HUDOverlay() {
    }

    public Mat draw(Mat frame, List<TrackerManager.TrackerSnapshot> trackers) {
        return draw(frame, trackers, Collections.emptyMap());
    }

    public Mat draw(Mat frame,
                    List<TrackerManager.TrackerSnapshot> trackers,
                    Map<String, List<TrajectoryPredictor.Point>> predictedPaths) {
        if (frame == null || trackers == null) {
            return frame;
        }

        for (TrackerManager.TrackerSnapshot snapshot : trackers) {
            drawTracker(frame, snapshot);

            List<TrajectoryPredictor.Point> path = predictedPaths != null
                ? predictedPaths.get(snapshot.getTrackId())
                : null;
            if (path != null && path.size() > 1) {
                drawPredictedPath(frame, path);
            }
        }
        return frame;
    }

    private void drawTracker(Mat frame, TrackerManager.TrackerSnapshot snapshot) {
        double x1 = snapshot.getX();
        double y1 = snapshot.getY();
        double x2 = x1 + snapshot.getWidth();
        double y2 = y1 + snapshot.getHeight();
        Point topLeft = new Point((int) Math.round(x1), (int) Math.round(y1));
        Point bottomRight = new Point((int) Math.round(x2), (int) Math.round(y2));
        opencv_imgproc.rectangle(frame, topLeft, bottomRight, COLOR_BOX, 2, opencv_imgproc.LINE_8, 0);

        String trackShortId = snapshot.getTrackId().length() > 6
            ? snapshot.getTrackId().substring(0, 6)
            : snapshot.getTrackId();
        String label = String.format("#%s %s %.0f%%",
            trackShortId,
            snapshot.getLabel(),
            snapshot.getConfidence() * 100f
        );

        double fontScale = 0.5;
        int thickness = 1;
        int[] baseline = new int[1];
        Size textSize = opencv_imgproc.getTextSize(label, opencv_imgproc.FONT_HERSHEY_SIMPLEX, fontScale, thickness, baseline);
        double textX = x1;
        double textY = Math.max(15, y1 - 5);
        Point textOrigin = new Point((int) Math.round(textX), (int) Math.round(textY));
        Point backgroundPt2 = new Point(
            (int) Math.round(textX + textSize.width()),
            (int) Math.round(textY + textSize.height() + baseline[0])
        );

        opencv_imgproc.rectangle(frame, textOrigin, backgroundPt2, COLOR_TEXT_BG, opencv_imgproc.FILLED, 0, 0);
        opencv_imgproc.putText(
            frame,
            label,
            new Point(
                (int) Math.round(textX),
                (int) Math.round(textY + textSize.height())
            ),
            opencv_imgproc.FONT_HERSHEY_SIMPLEX,
            fontScale,
            COLOR_TEXT,
            thickness,
            opencv_imgproc.LINE_AA,
            false
        );
    }

    private void drawPredictedPath(Mat frame, List<TrajectoryPredictor.Point> path) {
        for (int i = 0; i < path.size() - 1; i++) {
            TrajectoryPredictor.Point from = path.get(i);
            TrajectoryPredictor.Point to = path.get(i + 1);
            opencv_imgproc.line(
                frame,
                new Point(
                    (int) Math.round(from.getX()),
                    (int) Math.round(from.getY())
                ),
                new Point(
                    (int) Math.round(to.getX()),
                    (int) Math.round(to.getY())
                ),
                COLOR_PATH,
                1,
                opencv_imgproc.LINE_8,
                0
            );
        }
    }
}

