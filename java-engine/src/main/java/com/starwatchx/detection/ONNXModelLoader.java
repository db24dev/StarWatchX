package com.starwatchx.detection;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.OnnxValue;
import ai.onnxruntime.TensorInfo;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

/**
 * Wraps ONNX Runtime session loading and inference.
 */
public class ONNXModelLoader implements AutoCloseable {

    private final OrtEnvironment environment;
    private OrtSession session;
    private String inputName;
    private String outputName;
    private long[] inputShape;
    private boolean loaded;

    public ONNXModelLoader() {
        this.environment = OrtEnvironment.getEnvironment();
    }

    public void loadModel(String modelPath) {
        if (loaded) {
            closeSession();
        }
        if (modelPath == null || modelPath.isBlank()) {
            System.err.println("[ONNX] Model path is blank. Skipping load.");
            loaded = false;
            return;
        }

        try {
            Path resolvedPath = resolveModelPath(modelPath);
            if (resolvedPath == null) {
                System.err.println("[ONNX] Model not found at " + modelPath + ". Fallback mode enabled.");
                loaded = false;
                return;
            }
            try (OrtSession.SessionOptions options = new OrtSession.SessionOptions()) {
                this.session = environment.createSession(resolvedPath.toString(), options);
            }
            this.inputName = extractFirstName(session.getInputNames());
            this.outputName = extractFirstName(session.getOutputNames());

            TensorInfo info = (TensorInfo) session.getInputInfo().get(inputName).getInfo();
            this.inputShape = info.getShape();
            this.loaded = true;
            System.out.println("[ONNX] Model ready: " + resolvedPath.toAbsolutePath());
        } catch (IOException | OrtException e) {
            loaded = false;
            closeSession();
            System.err.println("[ONNX] Unable to load model: " + e.getMessage());
        }
    }

    public boolean isModelLoaded() {
        return loaded;
    }

    public float[][] runInference(float[] inputTensor, long[] shape) {
        if (!loaded) {
            throw new IllegalStateException("ONNX model not loaded");
        }

        try (OnnxTensor tensor = OnnxTensor.createTensor(environment, FloatBuffer.wrap(inputTensor), shape);
             OrtSession.Result result = session.run(Collections.singletonMap(inputName, tensor))) {

            Optional<OnnxValue> valueOptional = result.get(outputName);
            if (valueOptional.isEmpty()) {
                throw new IllegalStateException("ONNX output missing for name: " + outputName);
            }
            OnnxValue value = valueOptional.get();
            if (!(value instanceof OnnxTensor)) {
                throw new IllegalStateException("Unexpected ONNX output type: " + value.getClass());
            }
            try (OnnxTensor tensorOutput = (OnnxTensor) value) {
                return toDetectionsArray(tensorOutput);
            }
        } catch (OrtException e) {
            throw new IllegalStateException("ONNX inference failed", e);
        }
    }

    public long[] getInputShape() {
        return inputShape;
    }

    @Override
    public void close() {
        closeSession();
        environment.close();
    }

    private void closeSession() {
        if (session != null) {
            try {
                session.close();
            } catch (OrtException e) {
                System.err.println("[ONNX] Failed to close session: " + e.getMessage());
            }
            session = null;
        }
        inputName = null;
        outputName = null;
        inputShape = null;
        loaded = false;
    }

    private Path resolveModelPath(String modelPath) throws IOException {
        if (modelPath == null || modelPath.isEmpty()) {
            throw new IOException("Model path is empty");
        }

        Path path = Paths.get(modelPath);
        if (Files.exists(path)) {
            return path;
        }

        // Attempt to load from classpath resources
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(modelPath)) {
            if (inputStream == null) {
                return null;
            }
            Path tempFile = Files.createTempFile("starwatchx-model", ".onnx");
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            tempFile.toFile().deleteOnExit();
            return tempFile;
        }
    }

    private float[][] toDetectionsArray(OnnxTensor tensor) throws OrtException {
        long[] shape = tensor.getInfo().getShape();
        FloatBuffer buffer = tensor.getFloatBuffer();
        float[] flattened = new float[buffer.remaining()];
        buffer.get(flattened);

        if (shape.length == 3 && shape[0] == 1) {
            int dim1 = (int) shape[1];
            int dim2 = (int) shape[2];

            if (dim1 <= dim2) {
                // Assumes layout [1, attributes, detections]
                float[][] detections = new float[dim2][dim1];
                for (int attr = 0; attr < dim1; attr++) {
                    for (int det = 0; det < dim2; det++) {
                        int srcIndex = attr * dim2 + det;
                        detections[det][attr] = flattened[srcIndex];
                    }
                }
                return detections;
            } else {
                // Layout [1, detections, attributes]
                float[][] detections = new float[dim1][dim2];
                for (int det = 0; det < dim1; det++) {
                    int offset = det * dim2;
                    System.arraycopy(flattened, offset, detections[det], 0, dim2);
                }
                return detections;
            }
        } else if (shape.length == 2) {
            int rows = (int) shape[0];
            int cols = (int) shape[1];
            float[][] detections = new float[rows][cols];
            for (int row = 0; row < rows; row++) {
                int offset = row * cols;
                System.arraycopy(flattened, offset, detections[row], 0, cols);
            }
            return detections;
        }

        throw new IllegalStateException("Unsupported output shape: " + java.util.Arrays.toString(shape));
    }

    private String extractFirstName(Iterable<String> names) {
        Iterator<String> iterator = names.iterator();
        if (!iterator.hasNext()) {
            throw new IllegalStateException("ONNX session missing inputs/outputs");
        }
        return iterator.next();
    }
}