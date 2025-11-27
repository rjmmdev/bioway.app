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
        private const val INPUT_SIZE = 320
        const val CONFIDENCE_THRESHOLD = 0.45f  // Public para usar desde pantallas
        const val CONFIDENCE_THRESHOLD_DISTANT = 0.35f  // Umbral más bajo para objetos distantes/pequeños
        private const val IOU_THRESHOLD = 0.50f
        private const val MAX_DETECTIONS = 100
        private const val NUM_THREADS = 4

        // Escalas para detección multi-escala (mejora reconocimiento a distancia)
        private val MULTI_SCALE_FACTORS = listOf(1.0f, 1.5f, 2.0f)  // Original, 1.5x, 2x
        private const val SMALL_OBJECT_THRESHOLD = 0.15f  // Objetos que ocupan menos del 15% del frame
    }

    // Threshold personalizable (solo para Bote)
    var confidenceThreshold = CONFIDENCE_THRESHOLD

    // Modo multi-escala: mejora detección de objetos pequeños/lejanos
    // IMPORTANTE: Activar esto aumenta el tiempo de inferencia ~3x
    var multiScaleMode = false

    // Umbral de confianza para objetos distantes (más permisivo)
    var distantObjectThreshold = CONFIDENCE_THRESHOLD_DISTANT

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

    // Modo de TTA: false = 2 rotaciones (rápido), true = 4 rotaciones (completo)
    var fullTTAMode = false

    /**
     * Clasifica una imagen y retorna las detecciones
     * Usa Test-Time Augmentation (TTA) con múltiples rotaciones para detección invariante a orientación
     * Opcionalmente usa Multi-Scale Detection para mejorar reconocimiento a distancia
     */
    suspend fun classifyImage(bitmap: Bitmap): ClassificationResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        requireNotNull(interpreter) { "El clasificador no ha sido inicializado" }

        val allDetections = mutableListOf<Detection>()

        // Si está habilitado el modo multi-escala, ejecutar a diferentes escalas
        val scales = if (multiScaleMode) MULTI_SCALE_FACTORS else listOf(1.0f)

        for (scale in scales) {
            // Escalar imagen si es necesario
            val scaledBitmap = if (scale != 1.0f) {
                scaleAndCropCenter(bitmap, scale)
            } else {
                bitmap
            }

            // TTA: 2 rotaciones (rápido) o 4 rotaciones (completo)
            val rotations = if (fullTTAMode) {
                listOf(0f, 90f, 180f, 270f)  // Completo: 4 rotaciones
            } else {
                listOf(0f, 90f, 180f)  // Rápido: 3 rotaciones (cubre la mayoría de casos)
            }

            for (rotation in rotations) {
                // Rotar imagen
                val rotatedBitmap = if (rotation != 0f) {
                    rotateBitmapForTTA(scaledBitmap, rotation)
                } else {
                    scaledBitmap
                }

                // Clasificar imagen rotada con umbral ajustado por escala
                // Para escalas mayores (zoom), usar umbral más permisivo ya que son objetos distantes
                val originalThreshold = confidenceThreshold
                if (scale > 1.0f) {
                    confidenceThreshold = distantObjectThreshold
                }

                val detections = classifySingleOrientation(rotatedBitmap)

                // Restaurar umbral
                confidenceThreshold = originalThreshold

                // Transformar detecciones de vuelta al espacio original
                val transformedDetections = detections.map { detection ->
                    val rotationTransformed = transformDetectionBack(detection, rotation, scaledBitmap.width, scaledBitmap.height)
                    // Si usamos escala, transformar coordenadas al espacio original
                    if (scale != 1.0f) {
                        transformDetectionFromScale(rotationTransformed, scale)
                    } else {
                        rotationTransformed
                    }
                }

                allDetections.addAll(transformedDetections)

                // Reciclar bitmap rotado si no es el original/escalado
                if (rotation != 0f && rotatedBitmap != scaledBitmap) {
                    rotatedBitmap.recycle()
                }
            }

            // Reciclar bitmap escalado si no es el original
            if (scale != 1.0f && scaledBitmap != bitmap) {
                scaledBitmap.recycle()
            }
        }

        // Aplicar NMS global para eliminar duplicados entre rotaciones y escalas
        val nmsDetections = nonMaxSuppressionGlobal(allDetections)

        val inferenceTime = System.currentTimeMillis() - startTime
        val scalesInfo = if (multiScaleMode) " + ${scales.size} escalas" else ""
        Log.d(TAG, "Clasificación completada: ${nmsDetections.size} detecciones en ${inferenceTime}ms$scalesInfo")

        ClassificationResult(nmsDetections, inferenceTime)
    }

    /**
     * Escala y recorta el centro de una imagen para detección multi-escala.
     * Por ejemplo, scale=2.0 toma el 50% central de la imagen y lo amplía.
     */
    private fun scaleAndCropCenter(bitmap: Bitmap, scale: Float): Bitmap {
        // Calcular el área central a recortar
        val cropWidth = (bitmap.width / scale).toInt()
        val cropHeight = (bitmap.height / scale).toInt()
        val cropX = (bitmap.width - cropWidth) / 2
        val cropY = (bitmap.height - cropHeight) / 2

        // Recortar la región central
        val cropped = Bitmap.createBitmap(
            bitmap,
            cropX.coerceAtLeast(0),
            cropY.coerceAtLeast(0),
            cropWidth.coerceAtMost(bitmap.width - cropX),
            cropHeight.coerceAtMost(bitmap.height - cropY)
        )

        // Escalar de vuelta al tamaño original para mantener la resolución del modelo
        return Bitmap.createScaledBitmap(cropped, bitmap.width, bitmap.height, true).also {
            if (cropped != it) cropped.recycle()
        }
    }

    /**
     * Transforma coordenadas de detección del espacio escalado al espacio original.
     */
    private fun transformDetectionFromScale(detection: Detection, scale: Float): Detection {
        // Las coordenadas en el espacio escalado corresponden al centro de la imagen original
        // Necesitamos mapearlas de vuelta
        val cropFactor = 1.0f / scale
        val offset = (1.0f - cropFactor) / 2.0f

        val box = detection.boundingBox
        val newLeft = offset + box.left * cropFactor
        val newTop = offset + box.top * cropFactor
        val newRight = offset + box.right * cropFactor
        val newBottom = offset + box.bottom * cropFactor

        return Detection(
            className = detection.className,
            confidence = detection.confidence,
            boundingBox = RectF(
                newLeft.coerceIn(0f, 1f),
                newTop.coerceIn(0f, 1f),
                newRight.coerceIn(0f, 1f),
                newBottom.coerceIn(0f, 1f)
            ),
            classId = detection.classId
        )
    }

    /**
     * Clasificación en una sola orientación (sin TTA)
     */
    private fun classifySingleOrientation(bitmap: Bitmap): List<Detection> {
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

        return detections
    }

    /**
     * Rota un bitmap para TTA
     */
    private fun rotateBitmapForTTA(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = android.graphics.Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * Transforma una detección del espacio rotado al espacio original
     */
    private fun transformDetectionBack(
        detection: Detection,
        rotationDegrees: Float,
        originalWidth: Int,
        originalHeight: Int
    ): Detection {
        val box = detection.boundingBox

        // Transformar coordenadas según la rotación aplicada
        val (newLeft, newTop, newRight, newBottom) = when (rotationDegrees.toInt()) {
            90 -> {
                // Rotación 90° CW: (x,y) -> (1-y, x)
                listOf(
                    box.top,
                    1f - box.right,
                    box.bottom,
                    1f - box.left
                )
            }
            180 -> {
                // Rotación 180°: (x,y) -> (1-x, 1-y)
                listOf(
                    1f - box.right,
                    1f - box.bottom,
                    1f - box.left,
                    1f - box.top
                )
            }
            270 -> {
                // Rotación 270° CW (90° CCW): (x,y) -> (y, 1-x)
                listOf(
                    1f - box.bottom,
                    box.left,
                    1f - box.top,
                    box.right
                )
            }
            else -> {
                // Sin rotación
                listOf(box.left, box.top, box.right, box.bottom)
            }
        }

        return Detection(
            className = detection.className,
            confidence = detection.confidence,
            boundingBox = RectF(
                newLeft.coerceIn(0f, 1f),
                newTop.coerceIn(0f, 1f),
                newRight.coerceIn(0f, 1f),
                newBottom.coerceIn(0f, 1f)
            ),
            classId = detection.classId
        )
    }

    /**
     * NMS global que considera detecciones de todas las rotaciones
     * Más agresivo para fusionar detecciones similares
     */
    private fun nonMaxSuppressionGlobal(detections: List<Detection>): List<Detection> {
        if (detections.isEmpty()) return emptyList()

        // Ordenar por confianza descendente
        val sortedDetections = detections.sortedByDescending { it.confidence }
        val selectedDetections = mutableListOf<Detection>()

        for (detection in sortedDetections) {
            var shouldAdd = true

            // Verificar overlap con detecciones ya seleccionadas
            // Usamos un IOU más bajo (0.3) para fusionar detecciones de diferentes rotaciones
            for (selected in selectedDetections) {
                // Para TTA, fusionar si es la misma clase Y hay overlap significativo
                if (detection.classId == selected.classId) {
                    val iou = calculateIoU(detection.boundingBox, selected.boundingBox)
                    if (iou > 0.3f) {  // IOU más bajo para TTA
                        shouldAdd = false
                        break
                    }
                }
                // También fusionar si el centro está muy cerca aunque sean clases diferentes
                val centerDist = calculateCenterDistance(detection.boundingBox, selected.boundingBox)
                if (centerDist < 0.1f && detection.confidence < selected.confidence) {
                    shouldAdd = false
                    break
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
     * Calcula la distancia entre los centros de dos bounding boxes (normalizada)
     */
    private fun calculateCenterDistance(box1: RectF, box2: RectF): Float {
        val center1X = (box1.left + box1.right) / 2
        val center1Y = (box1.top + box1.bottom) / 2
        val center2X = (box2.left + box2.right) / 2
        val center2Y = (box2.top + box2.bottom) / 2

        val dx = center1X - center2X
        val dy = center1Y - center2Y
        return kotlin.math.sqrt(dx * dx + dy * dy)
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

            // Filtrar por threshold de confianza (usa threshold personalizable)
            if (maxProb > confidenceThreshold) {
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