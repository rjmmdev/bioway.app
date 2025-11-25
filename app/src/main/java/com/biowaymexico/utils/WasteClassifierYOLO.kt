package com.biowaymexico.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Clasificador de residuos usando YOLOv8 (modelo de clasificacionBioWay)
 * Detecta 12 categorías de residuos con bounding boxes
 * Modelo más eficiente: 3MB vs 43MB del modelo anterior
 */
class WasteClassifierYOLO(private val context: Context) {

    companion object {
        private const val TAG = "WasteClassifierYOLO"
        private const val MODEL_PATH = "models/best.tflite"
        private const val LABELS_PATH = "labels/labels.txt"
        private const val INPUT_SIZE = 320  // IMPORTANTE: YOLOv8 usa 320x320, no 640x640
        private const val CONFIDENCE_THRESHOLD = 0.45f
        private const val IOU_THRESHOLD = 0.50f
        private const val MAX_DETECTIONS = 100
        private const val NUM_THREADS = 4
    }

    private var interpreter: Interpreter? = null
    private var labels: List<String> = emptyList()
    private var gpuDelegate: GpuDelegate? = null

    // Data class para resultados de detección
    data class Detection(
        val className: String,
        val confidence: Float,
        val boundingBox: RectF,
        val classId: Int
    )

    data class ClassificationResult(
        val detections: List<Detection>,
        val inferenceTime: Long
    )

    // Data class para información de preprocesamiento
    private data class PreprocessingInfo(
        val scaleFactor: Float,
        val xOffset: Float,
        val yOffset: Float
    )

    /**
     * Inicializa el clasificador, cargando modelo y labels
     */
    suspend fun initialize() = withContext(Dispatchers.IO) {
        try {
            // Cargar labels
            labels = loadLabels()
            Log.d(TAG, "Labels cargados: ${labels.size} categorías")

            // Cargar modelo
            val modelBuffer = loadModelFile()

            // Configurar opciones del intérprete
            val options = Interpreter.Options().apply {
                setNumThreads(NUM_THREADS)

                // Intentar usar GPU si está disponible
                val compatList = CompatibilityList()
                if (compatList.isDelegateSupportedOnThisDevice) {
                    try {
                        val delegateOptions = compatList.bestOptionsForThisDevice
                        gpuDelegate = GpuDelegate(delegateOptions)
                        addDelegate(gpuDelegate)
                        Log.d(TAG, "GPU Delegate habilitado")
                    } catch (e: Exception) {
                        Log.w(TAG, "No se pudo habilitar GPU, usando CPU", e)
                        setUseXNNPACK(true)
                    }
                } else {
                    setUseXNNPACK(true)
                    Log.d(TAG, "GPU no disponible, usando CPU con XNNPACK")
                }
            }

            interpreter = Interpreter(modelBuffer, options)

            // Log información del modelo
            printModelInfo()

            Log.i(TAG, "Clasificador YOLOv8 inicializado correctamente")
        } catch (e: Exception) {
            Log.e(TAG, "Error al inicializar clasificador", e)
            throw e
        }
    }

    /**
     * Clasifica una imagen y retorna las detecciones
     */
    suspend fun classifyImage(bitmap: Bitmap): ClassificationResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        requireNotNull(interpreter) { "El clasificador no ha sido inicializado" }

        // Preprocesar imagen con letterboxing
        val (processedImage, preprocessingInfo) = preprocessImage(bitmap)

        // Preparar buffer de salida
        val outputBuffer = ByteBuffer.allocateDirect(4 * 16 * 2100)
        outputBuffer.order(ByteOrder.nativeOrder())

        // Ejecutar inferencia
        interpreter?.run(processedImage.buffer, outputBuffer)

        // Parsear salida del modelo
        outputBuffer.rewind()
        val detections = parseOutput(
            outputBuffer,
            intArrayOf(1, 16, 2100),
            bitmap.width,
            bitmap.height,
            preprocessingInfo
        )

        // Aplicar Non-Maximum Suppression
        val nmsDetections = nonMaxSuppression(detections)

        val inferenceTime = System.currentTimeMillis() - startTime
        Log.d(TAG, "Clasificación completada: ${nmsDetections.size} detecciones en ${inferenceTime}ms")

        ClassificationResult(nmsDetections, inferenceTime)
    }

    /**
     * Preprocesa la imagen con letterboxing para mantener aspect ratio
     * Retorna la imagen procesada y la información de transformación
     */
    private fun preprocessImage(bitmap: Bitmap): Pair<TensorImage, PreprocessingInfo> {
        // Calcular letterboxing
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height
        val targetSize = INPUT_SIZE

        // Calcular escala manteniendo aspect ratio
        val scale = minOf(
            targetSize.toFloat() / originalWidth,
            targetSize.toFloat() / originalHeight
        )

        val scaledWidth = (originalWidth * scale).toInt()
        val scaledHeight = (originalHeight * scale).toInt()

        // Calcular padding
        val padX = (targetSize - scaledWidth) / 2f
        val padY = (targetSize - scaledHeight) / 2f

        // Crear TensorImage
        var tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(bitmap)

        // Crear bitmap con letterbox (padding)
        val paddedBitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(paddedBitmap)

        // Fondo gris (114, 114, 114) como en YOLOv8 original
        canvas.drawColor(android.graphics.Color.rgb(114, 114, 114))

        // Redimensionar el bitmap original
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)

        // Dibujar el bitmap escalado en el centro con padding
        canvas.drawBitmap(scaledBitmap, padX, padY, null)

        // Crear tensor image con el bitmap padded
        tensorImage.load(paddedBitmap)

        // Aplicar normalización
        val imageProcessor = ImageProcessor.Builder()
            .add(NormalizeOp(0f, 255f))  // Normalizar a [0, 1]
            .build()

        tensorImage = imageProcessor.process(tensorImage)

        val preprocessingInfo = PreprocessingInfo(
            scaleFactor = scale,
            xOffset = padX,
            yOffset = padY
        )

        return Pair(tensorImage, preprocessingInfo)
    }


    /**
     * Parsea la salida del modelo YOLOv8
     * Formato: [1, 16, 2100] donde 16 = 4 bbox + 12 clases
     */
    private fun parseOutput(
        outputBuffer: ByteBuffer,
        outputShape: IntArray,
        originalWidth: Int,
        originalHeight: Int,
        preprocessingInfo: PreprocessingInfo
    ): List<Detection> {
        val detections = mutableListOf<Detection>()
        val numAnchors = outputShape[2]  // 2100
        val numClasses = labels.size  // 12

        // El output está transpuesto: [1, features, anchors]
        for (i in 0 until numAnchors) {
            // Leer coordenadas del bounding box (normalizadas 0-1)
            val xCenter = outputBuffer.getFloat(i * 4)
            val yCenter = outputBuffer.getFloat(numAnchors * 4 + i * 4)
            val width = outputBuffer.getFloat(2 * numAnchors * 4 + i * 4)
            val height = outputBuffer.getFloat(3 * numAnchors * 4 + i * 4)

            // Leer probabilidades de clases
            var maxProb = 0f
            var maxClass = 0
            for (c in 0 until numClasses) {
                val prob = outputBuffer.getFloat((4 + c) * numAnchors * 4 + i * 4)
                if (prob > maxProb) {
                    maxProb = prob
                    maxClass = c
                }
            }

            // Filtrar por threshold de confianza
            if (maxProb > CONFIDENCE_THRESHOLD) {
                // Convertir de coordenadas del modelo a coordenadas originales
                // Primero, deshacer la normalización del modelo (0-1 a pixeles)
                var x1 = (xCenter - width / 2) * INPUT_SIZE
                var y1 = (yCenter - height / 2) * INPUT_SIZE
                var x2 = (xCenter + width / 2) * INPUT_SIZE
                var y2 = (yCenter + height / 2) * INPUT_SIZE

                // Deshacer el letterboxing
                x1 = (x1 - preprocessingInfo.xOffset) / preprocessingInfo.scaleFactor
                y1 = (y1 - preprocessingInfo.yOffset) / preprocessingInfo.scaleFactor
                x2 = (x2 - preprocessingInfo.xOffset) / preprocessingInfo.scaleFactor
                y2 = (y2 - preprocessingInfo.yOffset) / preprocessingInfo.scaleFactor

                // Asegurar que las coordenadas estén dentro de los límites
                x1 = x1.coerceIn(0f, originalWidth.toFloat())
                y1 = y1.coerceIn(0f, originalHeight.toFloat())
                x2 = x2.coerceIn(0f, originalWidth.toFloat())
                y2 = y2.coerceIn(0f, originalHeight.toFloat())

                // Normalizar coordenadas a valores entre 0 y 1 para ser independiente de la resolución
                val normalizedX1 = x1 / originalWidth.toFloat()
                val normalizedY1 = y1 / originalHeight.toFloat()
                val normalizedX2 = x2 / originalWidth.toFloat()
                val normalizedY2 = y2 / originalHeight.toFloat()

                val boundingBox = RectF(normalizedX1, normalizedY1, normalizedX2, normalizedY2)

                detections.add(
                    Detection(
                        className = labels[maxClass],
                        confidence = maxProb,
                        boundingBox = boundingBox,
                        classId = maxClass
                    )
                )
            }
        }

        return detections
    }

    /**
     * Non-Maximum Suppression para eliminar detecciones duplicadas
     */
    private fun nonMaxSuppression(detections: List<Detection>): List<Detection> {
        if (detections.isEmpty()) return emptyList()

        // Ordenar por confianza descendente
        val sortedDetections = detections.sortedByDescending { it.confidence }
        val selectedDetections = mutableListOf<Detection>()

        for (detection in sortedDetections) {
            var shouldAdd = true

            // Verificar overlap con detecciones ya seleccionadas
            for (selected in selectedDetections) {
                if (detection.classId == selected.classId) {
                    val iou = calculateIoU(detection.boundingBox, selected.boundingBox)
                    if (iou > IOU_THRESHOLD) {
                        shouldAdd = false
                        break
                    }
                }
            }

            if (shouldAdd) {
                selectedDetections.add(detection)
                if (selectedDetections.size >= MAX_DETECTIONS) break
            }
        }

        return selectedDetections
    }

    /**
     * Calcula Intersection over Union entre dos bounding boxes
     */
    private fun calculateIoU(box1: RectF, box2: RectF): Float {
        val intersectionLeft = maxOf(box1.left, box2.left)
        val intersectionTop = maxOf(box1.top, box2.top)
        val intersectionRight = minOf(box1.right, box2.right)
        val intersectionBottom = minOf(box1.bottom, box2.bottom)

        if (intersectionLeft >= intersectionRight || intersectionTop >= intersectionBottom) {
            return 0f
        }

        val intersectionArea = (intersectionRight - intersectionLeft) * (intersectionBottom - intersectionTop)
        val box1Area = box1.width() * box1.height()
        val box2Area = box2.width() * box2.height()
        val unionArea = box1Area + box2Area - intersectionArea

        return if (unionArea > 0) intersectionArea / unionArea else 0f
    }

    /**
     * Carga el archivo del modelo desde assets
     */
    private fun loadModelFile(): ByteBuffer {
        val assetFileDescriptor = context.assets.openFd(MODEL_PATH)
        val inputStream = assetFileDescriptor.createInputStream()
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength

        val modelBuffer = ByteBuffer.allocateDirect(declaredLength.toInt())
        modelBuffer.order(ByteOrder.nativeOrder())

        fileChannel.read(modelBuffer, startOffset)
        modelBuffer.rewind()

        return modelBuffer
    }

    /**
     * Carga las etiquetas desde assets
     */
    private fun loadLabels(): List<String> {
        val labels = mutableListOf<String>()
        context.assets.open(LABELS_PATH).use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                reader.forEachLine { line ->
                    if (line.isNotBlank()) {
                        labels.add(line.trim())
                    }
                }
            }
        }
        return labels
    }

    /**
     * Imprime información del modelo para debugging
     */
    private fun printModelInfo() {
        interpreter?.let { interp ->
            Log.d(TAG, "=== Información del Modelo ===")

            // Input
            val inputTensor = interp.getInputTensor(0)
            Log.d(TAG, "Input shape: ${inputTensor.shape().contentToString()}")
            Log.d(TAG, "Input type: ${inputTensor.dataType()}")

            // Output
            val outputTensor = interp.getOutputTensor(0)
            Log.d(TAG, "Output shape: ${outputTensor.shape().contentToString()}")
            Log.d(TAG, "Output type: ${outputTensor.dataType()}")

            Log.d(TAG, "Número de clases: ${labels.size}")
            Log.d(TAG, "Clases: ${labels.joinToString(", ")}")
        }
    }

    /**
     * Mapea las 12 categorías del modelo a las 7 categorías de bioway
     */
    fun mapToSimplifiedCategories(className: String): String {
        return when (className) {
            "plastic", "plastic-PET", "plastic-PE_HD", "plastic-PP", "plastic-PS", "plastic-Others" -> "Plástico"
            "cardboard" -> "Cartón"
            "paper" -> "Papel"
            "glass" -> "Vidrio"
            "metal" -> "Metal"
            "biological" -> "Orgánico"
            "trash" -> "Basura"
            else -> "Basura"
        }
    }

    /**
     * Libera recursos del clasificador
     */
    fun close() {
        try {
            interpreter?.close()
            interpreter = null
            gpuDelegate?.close()
            gpuDelegate = null
            Log.d(TAG, "Recursos del clasificador liberados")
        } catch (e: Exception) {
            Log.e(TAG, "Error al cerrar clasificador", e)
        }
    }

    /**
     * Verifica si el clasificador está listo
     */
    fun isReady(): Boolean = interpreter != null
}