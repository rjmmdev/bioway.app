package com.ultralytics.yolo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.max
import kotlin.math.min

/**
 * Detector de residuos usando modelo YOLO
 * Parámetros optimizados según README:
 * - Confidence threshold: 0.25
 * - IoU threshold: 0.4
 * - Max detections: 30
 */
class WasteDetector(
    private val context: Context,
    modelPath: String,
    val labels: List<String>,
    useGpu: Boolean = true
) : AutoCloseable {

    companion object {
        private const val TAG = "WasteDetector"
        private const val DEFAULT_CONFIDENCE_THRESHOLD = 0.25f
        private const val DEFAULT_IOU_THRESHOLD = 0.4f
        private const val DEFAULT_NUM_ITEMS_THRESHOLD = 30
        private const val INPUT_MEAN = 0.0f
        private const val INPUT_STD = 255.0f

        fun loadLabels(context: Context, labelsPath: String): List<String> {
            return context.assets.open(labelsPath).bufferedReader().readLines()
        }
    }

    private var interpreter: Interpreter? = null
    private var gpuDelegate: GpuDelegate? = null
    private val inputSize: Int
    private val numClasses: Int
    private val numDetections: Int  // Numero de detecciones candidatas del modelo

    var confidenceThreshold = DEFAULT_CONFIDENCE_THRESHOLD
    var iouThreshold = DEFAULT_IOU_THRESHOLD
    var numItemsThreshold = DEFAULT_NUM_ITEMS_THRESHOLD

    init {
        try {
            val options = Interpreter.Options()

            if (useGpu) {
                try {
                    gpuDelegate = GpuDelegate()
                    options.addDelegate(gpuDelegate)
                    Log.d(TAG, "GPU delegate enabled")
                } catch (e: Exception) {
                    Log.w(TAG, "GPU delegate failed, using CPU: ${e.message}")
                }
            }

            options.setNumThreads(4)
            interpreter = Interpreter(loadModelFile(modelPath), options)

            val inputTensor = interpreter!!.getInputTensor(0)
            val inputShape = inputTensor.shape()
            inputSize = inputShape[1]

            val outputTensor = interpreter!!.getOutputTensor(0)
            val outputShape = outputTensor.shape()
            // Output shape: [1, 4+numClasses, numDetections]
            numClasses = outputShape[1] - 4
            numDetections = outputShape[2]

            Log.d(TAG, "Model loaded: input=$inputSize, classes=$numClasses, detections=$numDetections")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing detector: ${e.message}")
            throw e
        }
    }

    private fun loadModelFile(modelPath: String): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd(modelPath)
        val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = fileInputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun detect(bitmap: Bitmap): DetectionResult {
        val startTime = System.nanoTime()

        // Resize bitmap to model input size
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)

        // Prepare input buffer
        val inputBuffer = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * 4)
        inputBuffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(inputSize * inputSize)
        resizedBitmap.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)

        for (pixel in pixels) {
            val r = ((pixel shr 16) and 0xFF)
            val g = ((pixel shr 8) and 0xFF)
            val b = (pixel and 0xFF)

            // Normalize: (pixel - mean) / std = pixel / 255
            inputBuffer.putFloat((r - INPUT_MEAN) / INPUT_STD)
            inputBuffer.putFloat((g - INPUT_MEAN) / INPUT_STD)
            inputBuffer.putFloat((b - INPUT_MEAN) / INPUT_STD)
        }

        // Prepare output buffer [1, 4+numClasses, numDetections]
        val outputBuffer = Array(1) { Array(4 + numClasses) { FloatArray(numDetections) } }

        // Run inference
        inputBuffer.rewind()
        interpreter?.run(inputBuffer, outputBuffer)

        val inferenceTime = (System.nanoTime() - startTime) / 1_000_000.0

        // Post-process results
        val detections = postprocess(outputBuffer[0], bitmap.width, bitmap.height)

        return DetectionResult(
            boxes = detections,
            inferenceTimeMs = inferenceTime,
            fps = 1000.0 / inferenceTime,
            imageWidth = bitmap.width,
            imageHeight = bitmap.height
        )
    }

    private fun postprocess(output: Array<FloatArray>, imgWidth: Int, imgHeight: Int): List<Detection> {
        val candidates = mutableListOf<Detection>()
        val numDetections = output[0].size

        for (i in 0 until numDetections) {
            // Get coordinates (normalized 0-1)
            val cx = output[0][i]
            val cy = output[1][i]
            val w = output[2][i]
            val h = output[3][i]

            // Find best class
            var maxConf = 0f
            var maxIdx = 0
            for (c in 0 until numClasses) {
                val conf = output[4 + c][i]
                if (conf > maxConf) {
                    maxConf = conf
                    maxIdx = c
                }
            }

            if (maxConf >= confidenceThreshold) {
                // Convert to corner coordinates (normalized)
                val x1 = cx - w / 2
                val y1 = cy - h / 2
                val x2 = cx + w / 2
                val y2 = cy + h / 2

                // Clamp normalized values
                val nx1 = max(0f, min(1f, x1))
                val ny1 = max(0f, min(1f, y1))
                val nx2 = max(0f, min(1f, x2))
                val ny2 = max(0f, min(1f, y2))

                // Convert to pixel coordinates
                val px1 = nx1 * imgWidth
                val py1 = ny1 * imgHeight
                val px2 = nx2 * imgWidth
                val py2 = ny2 * imgHeight

                val className = if (maxIdx < labels.size) labels[maxIdx] else "unknown"

                candidates.add(
                    Detection(
                        classIndex = maxIdx,
                        className = className,
                        confidence = maxConf,
                        boundingBox = RectF(px1, py1, px2, py2),
                        normalizedBox = RectF(nx1, ny1, nx2, ny2)
                    )
                )
            }
        }

        // Apply NMS
        return applyNMS(candidates)
    }

    private fun applyNMS(detections: List<Detection>): List<Detection> {
        if (detections.isEmpty()) return emptyList()

        // Sort by confidence descending
        val sorted = detections.sortedByDescending { it.confidence }.toMutableList()
        val result = mutableListOf<Detection>()

        while (sorted.isNotEmpty() && result.size < numItemsThreshold) {
            val best = sorted.removeAt(0)
            result.add(best)

            sorted.removeAll { other ->
                other.classIndex == best.classIndex &&
                        calculateIoU(best.normalizedBox, other.normalizedBox) > iouThreshold
            }
        }

        return result
    }

    private fun calculateIoU(box1: RectF, box2: RectF): Float {
        val x1 = max(box1.left, box2.left)
        val y1 = max(box1.top, box2.top)
        val x2 = min(box1.right, box2.right)
        val y2 = min(box1.bottom, box2.bottom)

        val intersection = max(0f, x2 - x1) * max(0f, y2 - y1)
        val area1 = (box1.right - box1.left) * (box1.bottom - box1.top)
        val area2 = (box2.right - box2.left) * (box2.bottom - box2.top)
        val union = area1 + area2 - intersection

        return if (union > 0) intersection / union else 0f
    }

    override fun close() {
        interpreter?.close()
        gpuDelegate?.close()
        interpreter = null
        gpuDelegate = null
        Log.d(TAG, "Detector closed")
    }
}

/**
 * Representa una detección individual
 */
data class Detection(
    val classIndex: Int,
    val className: String,
    val confidence: Float,
    val boundingBox: RectF,      // Coordenadas en píxeles
    val normalizedBox: RectF     // Coordenadas normalizadas (0-1)
)

/**
 * Resultado de la detección
 */
data class DetectionResult(
    val boxes: List<Detection>,
    val inferenceTimeMs: Double,
    val fps: Double,
    val imageWidth: Int,
    val imageHeight: Int
)
