package com.ultralytics.yolo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.util.Log;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Delegate;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;
import org.tensorflow.lite.support.common.ops.CastOp;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.image.ops.Rot90Op;
import org.tensorflow.lite.support.common.FileUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * ObjectDetector - YOLO Object Detection for Android
 *
 * This class handles loading a TFLite YOLO model and performing object detection
 * with the same precision as the original implementation.
 *
 * Usage:
 *   ObjectDetector detector = new ObjectDetector(context, "best.tflite", labels, useGpu);
 *   YOLOResult result = detector.detect(bitmap);
 */
public class ObjectDetector {
    private static final String TAG = "ObjectDetector";

    // Default thresholds (same as original app)
    private static final float DEFAULT_CONFIDENCE_THRESHOLD = 0.25f;
    private static final float DEFAULT_IOU_THRESHOLD = 0.4f;
    private static final int DEFAULT_NUM_ITEMS_THRESHOLD = 30;
    private static final float INPUT_MEAN = 0.0f;
    private static final float INPUT_STANDARD_DEVIATION = 255.0f;

    private final Context context;
    private final Interpreter interpreter;
    private final List<String> labels;
    private final int inputWidth;
    private final int inputHeight;
    private final int numClasses;

    private float confidenceThreshold = DEFAULT_CONFIDENCE_THRESHOLD;
    private float iouThreshold = DEFAULT_IOU_THRESHOLD;
    private int numItemsThreshold = DEFAULT_NUM_ITEMS_THRESHOLD;

    // Output dimensions
    private int out1; // Number of predictions
    private int out2; // Features per prediction (4 + numClasses)

    // Preprocessors for different orientations
    private ImageProcessor imageProcessorSingleImage;
    private ImageProcessor imageProcessorCameraPortrait;
    private ImageProcessor imageProcessorCameraPortraitFront;
    private ImageProcessor imageProcessorCameraLandscape;

    // Buffers
    private ByteBuffer inputBuffer;
    private float[][][] rawOutput;

    /**
     * Create an ObjectDetector instance.
     *
     * @param context Android context
     * @param modelPath Path to the .tflite model file (in assets or absolute path)
     * @param labels List of class labels
     * @param useGpu Whether to use GPU acceleration
     */
    public ObjectDetector(Context context, String modelPath, List<String> labels, boolean useGpu) throws Exception {
        this.context = context;
        this.labels = labels;
        this.numClasses = labels.size();

        // Configure interpreter options
        Interpreter.Options options = new Interpreter.Options();
        options.setNumThreads(Runtime.getRuntime().availableProcessors());

        if (useGpu) {
            try {
                options.addDelegate(new GpuDelegate());
                Log.d(TAG, "GPU delegate is used.");
            } catch (Exception e) {
                Log.e(TAG, "GPU delegate error: " + e.getMessage() + ". Falling back to CPU.");
            }
        }

        // Load model
        MappedByteBuffer modelBuffer = loadModelFile(modelPath);
        this.interpreter = new Interpreter(modelBuffer, options);
        interpreter.allocateTensors();

        // Get input dimensions
        int[] inputShape = interpreter.getInputTensor(0).shape();
        if (!(inputShape[0] == 1 && inputShape[3] == 3)) {
            throw new IllegalArgumentException("Input tensor shape not supported. Expected [1, H, W, 3]. Got " + Arrays.toString(inputShape));
        }
        this.inputHeight = inputShape[1];
        this.inputWidth = inputShape[2];
        Log.d(TAG, "Model input size = " + inputWidth + " x " + inputHeight);

        // Get output dimensions
        int[] outputShape = interpreter.getOutputTensor(0).shape();
        this.out1 = outputShape[1];
        this.out2 = outputShape[2];
        Log.d(TAG, "Model output shape = [1, " + out1 + ", " + out2 + "]");

        // Initialize preprocessing
        initPreprocessors();
        initBuffers();

        Log.d(TAG, "ObjectDetector initialized successfully.");
    }

    /**
     * Load model from assets or filesystem.
     */
    private MappedByteBuffer loadModelFile(String modelPath) throws Exception {
        String path = ensureTFLiteExtension(modelPath);

        // Try absolute path first
        if (path.startsWith("/") && new File(path).exists()) {
            Log.d(TAG, "Loading model from filesystem: " + path);
            File file = new File(path);
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            return raf.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, file.length());
        }

        // Load from assets
        Log.d(TAG, "Loading model from assets: " + path);
        return FileUtil.loadMappedFile(context, path);
    }

    private String ensureTFLiteExtension(String modelPath) {
        if (modelPath.toLowerCase().endsWith(".tflite")) {
            return modelPath;
        }
        return modelPath + ".tflite";
    }

    private void initPreprocessors() {
        // For single images (no rotation)
        imageProcessorSingleImage = new ImageProcessor.Builder()
            .add(new ResizeOp(inputHeight, inputWidth, ResizeOp.ResizeMethod.BILINEAR))
            .add(new NormalizeOp(INPUT_MEAN, INPUT_STANDARD_DEVIATION))
            .add(new CastOp(DataType.FLOAT32))
            .build();

        // For camera portrait mode (rotate 270 degrees = 3 * 90)
        imageProcessorCameraPortrait = new ImageProcessor.Builder()
            .add(new Rot90Op(3))
            .add(new ResizeOp(inputHeight, inputWidth, ResizeOp.ResizeMethod.BILINEAR))
            .add(new NormalizeOp(INPUT_MEAN, INPUT_STANDARD_DEVIATION))
            .add(new CastOp(DataType.FLOAT32))
            .build();

        // For front camera portrait mode (rotate 90 degrees = 1 * 90)
        imageProcessorCameraPortraitFront = new ImageProcessor.Builder()
            .add(new Rot90Op(1))
            .add(new ResizeOp(inputHeight, inputWidth, ResizeOp.ResizeMethod.BILINEAR))
            .add(new NormalizeOp(INPUT_MEAN, INPUT_STANDARD_DEVIATION))
            .add(new CastOp(DataType.FLOAT32))
            .build();

        // For landscape mode (no rotation)
        imageProcessorCameraLandscape = new ImageProcessor.Builder()
            .add(new ResizeOp(inputHeight, inputWidth, ResizeOp.ResizeMethod.BILINEAR))
            .add(new NormalizeOp(INPUT_MEAN, INPUT_STANDARD_DEVIATION))
            .add(new CastOp(DataType.FLOAT32))
            .build();
    }

    private void initBuffers() {
        inputBuffer = ByteBuffer.allocateDirect(inputWidth * inputHeight * 3 * 4); // 3 channels, 4 bytes per float
        inputBuffer.order(ByteOrder.nativeOrder());

        rawOutput = new float[1][out1][out2];
    }

    /**
     * Detect objects in an image.
     *
     * @param bitmap Input image
     * @return YOLOResult containing detected boxes
     */
    public YOLOResult detect(Bitmap bitmap) {
        return detect(bitmap, false, false, false);
    }

    /**
     * Detect objects in an image with camera options.
     *
     * @param bitmap Input image
     * @param rotateForCamera Whether to apply camera rotation
     * @param isLandscape Whether the camera is in landscape mode
     * @param isFrontCamera Whether using front camera
     * @return YOLOResult containing detected boxes
     */
    public YOLOResult detect(Bitmap bitmap, boolean rotateForCamera, boolean isLandscape, boolean isFrontCamera) {
        int origWidth = bitmap.getWidth();
        int origHeight = bitmap.getHeight();

        long startTime = System.nanoTime();

        // ===== PREPROCESSING =====
        long preprocessStart = System.nanoTime();

        TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
        tensorImage.load(bitmap);

        inputBuffer.clear();

        TensorImage processedImage;
        if (!rotateForCamera) {
            processedImage = imageProcessorSingleImage.process(tensorImage);
        } else if (isLandscape) {
            processedImage = imageProcessorCameraLandscape.process(tensorImage);
        } else if (isFrontCamera) {
            processedImage = imageProcessorCameraPortraitFront.process(tensorImage);
        } else {
            processedImage = imageProcessorCameraPortrait.process(tensorImage);
        }

        inputBuffer.put(processedImage.getBuffer());
        inputBuffer.rewind();

        double preprocessTime = (System.nanoTime() - preprocessStart) / 1_000_000.0;
        Log.d(TAG, "Preprocessing done in " + preprocessTime + " ms");

        // ===== INFERENCE =====
        long inferenceStart = System.nanoTime();

        interpreter.run(inputBuffer, rawOutput);

        double inferenceTime = (System.nanoTime() - inferenceStart) / 1_000_000.0;
        Log.d(TAG, "Inference done in " + inferenceTime + " ms");

        // ===== POSTPROCESSING =====
        long postprocessStart = System.nanoTime();

        List<Box> boxes = postprocess(rawOutput[0], origWidth, origHeight);

        double postprocessTime = (System.nanoTime() - postprocessStart) / 1_000_000.0;
        Log.d(TAG, "Postprocessing done in " + postprocessTime + " ms");

        double totalTime = (System.nanoTime() - startTime) / 1_000_000.0;
        double fps = totalTime > 0 ? 1000.0 / totalTime : 0;

        Log.d(TAG, "Total time: " + totalTime + " ms, FPS: " + fps);

        return new YOLOResult(
            new Size(origWidth, origHeight),
            boxes,
            null,
            totalTime,
            fps,
            bitmap,
            labels
        );
    }

    /**
     * Postprocess model output to extract bounding boxes.
     * Implements Non-Maximum Suppression (NMS) in pure Java.
     *
     * This is equivalent to the native C++ postprocess function.
     */
    private List<Box> postprocess(float[][] output, int origWidth, int origHeight) {
        List<Detection> detections = new ArrayList<>();

        // Output format: [out1][out2] where out2 = 4 (bbox) + numClasses
        // Each row: [x_center, y_center, width, height, class1_conf, class2_conf, ...]

        // The output is transposed: [features][predictions]
        // So we iterate through predictions
        int numPredictions = out2;
        int numFeatures = out1;

        for (int i = 0; i < numPredictions; i++) {
            // Get bbox coordinates (normalized 0-1)
            float xCenter = output[0][i];
            float yCenter = output[1][i];
            float width = output[2][i];
            float height = output[3][i];

            // Find best class
            int bestClassIdx = -1;
            float bestClassConf = 0;

            for (int c = 0; c < numClasses; c++) {
                float classConf = output[4 + c][i];
                if (classConf > bestClassConf) {
                    bestClassConf = classConf;
                    bestClassIdx = c;
                }
            }

            // Apply confidence threshold
            if (bestClassConf >= confidenceThreshold) {
                // Convert from center format to corner format
                float x1 = xCenter - width / 2;
                float y1 = yCenter - height / 2;
                float x2 = xCenter + width / 2;
                float y2 = yCenter + height / 2;

                // Clamp to valid range
                x1 = Math.max(0, Math.min(1, x1));
                y1 = Math.max(0, Math.min(1, y1));
                x2 = Math.max(0, Math.min(1, x2));
                y2 = Math.max(0, Math.min(1, y2));

                if (x2 > x1 && y2 > y1) {
                    Detection det = new Detection();
                    det.x1 = x1;
                    det.y1 = y1;
                    det.x2 = x2;
                    det.y2 = y2;
                    det.confidence = bestClassConf;
                    det.classIndex = bestClassIdx;
                    detections.add(det);
                }
            }
        }

        // Apply NMS
        List<Detection> nmsDetections = applyNMS(detections);

        // Limit number of detections
        if (nmsDetections.size() > numItemsThreshold) {
            nmsDetections = nmsDetections.subList(0, numItemsThreshold);
        }

        // Convert to Box objects
        List<Box> boxes = new ArrayList<>();
        for (Detection det : nmsDetections) {
            // Pixel coordinates
            RectF xywh = new RectF(
                det.x1 * origWidth,
                det.y1 * origHeight,
                det.x2 * origWidth,
                det.y2 * origHeight
            );

            // Normalized coordinates
            RectF xywhn = new RectF(det.x1, det.y1, det.x2, det.y2);

            String className = (det.classIndex >= 0 && det.classIndex < labels.size())
                ? labels.get(det.classIndex)
                : "Unknown";

            boxes.add(new Box(det.classIndex, className, det.confidence, xywh, xywhn));
        }

        return boxes;
    }

    /**
     * Apply Non-Maximum Suppression to filter overlapping detections.
     */
    private List<Detection> applyNMS(List<Detection> detections) {
        if (detections.isEmpty()) {
            return detections;
        }

        // Sort by confidence (descending)
        Collections.sort(detections, new Comparator<Detection>() {
            @Override
            public int compare(Detection d1, Detection d2) {
                return Float.compare(d2.confidence, d1.confidence);
            }
        });

        List<Detection> result = new ArrayList<>();
        boolean[] suppressed = new boolean[detections.size()];

        for (int i = 0; i < detections.size(); i++) {
            if (suppressed[i]) continue;

            Detection current = detections.get(i);
            result.add(current);

            for (int j = i + 1; j < detections.size(); j++) {
                if (suppressed[j]) continue;

                Detection other = detections.get(j);

                // Only suppress if same class
                if (current.classIndex == other.classIndex) {
                    float iou = calculateIoU(current, other);
                    if (iou > iouThreshold) {
                        suppressed[j] = true;
                    }
                }
            }
        }

        return result;
    }

    /**
     * Calculate Intersection over Union (IoU) between two detections.
     */
    private float calculateIoU(Detection a, Detection b) {
        float intersectionX1 = Math.max(a.x1, b.x1);
        float intersectionY1 = Math.max(a.y1, b.y1);
        float intersectionX2 = Math.min(a.x2, b.x2);
        float intersectionY2 = Math.min(a.y2, b.y2);

        float intersectionArea = Math.max(0, intersectionX2 - intersectionX1) *
                                  Math.max(0, intersectionY2 - intersectionY1);

        float areaA = (a.x2 - a.x1) * (a.y2 - a.y1);
        float areaB = (b.x2 - b.x1) * (b.y2 - b.y1);

        float unionArea = areaA + areaB - intersectionArea;

        if (unionArea <= 0) return 0;

        return intersectionArea / unionArea;
    }

    /**
     * Internal class for detection during postprocessing.
     */
    private static class Detection {
        float x1, y1, x2, y2;
        float confidence;
        int classIndex;
    }

    // ===== GETTERS AND SETTERS =====

    public float getConfidenceThreshold() {
        return confidenceThreshold;
    }

    public void setConfidenceThreshold(float threshold) {
        this.confidenceThreshold = threshold;
    }

    public float getIouThreshold() {
        return iouThreshold;
    }

    public void setIouThreshold(float threshold) {
        this.iouThreshold = threshold;
    }

    public int getNumItemsThreshold() {
        return numItemsThreshold;
    }

    public void setNumItemsThreshold(int threshold) {
        this.numItemsThreshold = threshold;
    }

    public List<String> getLabels() {
        return labels;
    }

    public int getInputWidth() {
        return inputWidth;
    }

    public int getInputHeight() {
        return inputHeight;
    }

    /**
     * Release resources.
     */
    public void close() {
        if (interpreter != null) {
            interpreter.close();
        }
    }

    // ===== STATIC UTILITY METHODS =====

    /**
     * Load labels from a text file in assets.
     *
     * @param context Android context
     * @param labelsPath Path to labels file in assets
     * @return List of label strings
     */
    public static List<String> loadLabelsFromAssets(Context context, String labelsPath) {
        List<String> labels = new ArrayList<>();
        try {
            InputStream is = context.getAssets().open(labelsPath);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    labels.add(line);
                }
            }
            reader.close();
        } catch (IOException e) {
            Log.e(TAG, "Error loading labels: " + e.getMessage());
        }
        return labels;
    }
}
