package com.biowaymexico.ui.screens.bote_bioway.clasificador_gemini

import android.Manifest
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberUpdatedState
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.ultralytics.yolo.Detection
import com.ultralytics.yolo.DetectionResult
import com.ultralytics.yolo.WasteDetector
import com.biowaymexico.ui.theme.BioWayColors
import com.biowaymexico.utils.BluetoothManager
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "ClasificadorGemini"
private const val TAG_FILTER = "PlateFilterGemini"

// ══════════════════════════════════════════════════════════════════════════════
// CONFIGURACIÓN - Modifica la API Key aquí
// ══════════════════════════════════════════════════════════════════════════════
private const val GEMINI_API_KEY = "AIzaSyDbyZH75v5JfOOmDG77nlkZvZIRxRYlx3U"
// ══════════════════════════════════════════════════════════════════════════════

/**
 * Filtro para ignorar el plato blanco del bote (versión Gemini)
 * Solo detecta PRESENCIA de material, no clasifica
 *
 * Lógica mejorada basada en CONFIANZA + FORMA + POSICIÓN:
 * - El plato tiene confianza BAJA (26-50%) porque es un falso positivo
 * - Los objetos reales tienen confianza ALTA (60%+)
 * - El plato es CIRCULAR (aspect ratio cercano a 1)
 * - El plato está CENTRADO en el ROI
 * - Cuando hay 2+ plásticos, descartar el de mayor área (el plato)
 * - Cuando hay 1 plástico con baja confianza Y área grande, probablemente es el plato
 */
object MaterialPresenceFilter {

    // Umbral de confianza - MÁS ESTRICTO para evitar falsos positivos
    var minConfidenceThreshold = 0.60f  // Objetos reales tienen >60% confianza (antes 50%)

    // Umbral de área - solo aplica junto con baja confianza
    var suspiciousAreaThreshold = 0.25f  // Si área >25% Y confianza baja = plato (antes 35%)

    // Umbral de aspect ratio para detectar objetos circulares (plato)
    private const val CIRCULAR_ASPECT_MIN = 0.7f  // El plato tiene aspect ratio 0.7-1.3
    private const val CIRCULAR_ASPECT_MAX = 1.3f

    // Umbral de centrado - el plato está típicamente centrado
    private const val CENTER_THRESHOLD = 0.35f  // Distancia máxima del centro (normalizada)

    /**
     * Filtra las detecciones para determinar si hay un material presente
     * @return true si hay material válido presente, false si solo es el plato
     */
    fun hasMaterialPresent(
        detections: List<Detection>,
        roiWidth: Int,
        roiHeight: Int
    ): Boolean {
        val roiArea = roiWidth.toFloat() * roiHeight.toFloat()

        Log.d(TAG_FILTER, "═══════════════════════════════════════")
        Log.d(TAG_FILTER, "🔍 Verificando presencia de material...")
        Log.d(TAG_FILTER, "   Detecciones: ${detections.size}")

        if (detections.isEmpty()) {
            Log.d(TAG_FILTER, "   ❌ Sin detecciones - plato vacío")
            return false
        }

        // Separar plásticos de otros materiales
        val plasticDetections = detections.filter {
            it.className.lowercase().contains("plastic") ||
            it.className.lowercase().contains("plastico")
        }
        val otherDetections = detections.filter {
            !it.className.lowercase().contains("plastic") &&
            !it.className.lowercase().contains("plastico")
        }

        Log.d(TAG_FILTER, "   🔹 Plásticos: ${plasticDetections.size}")
        Log.d(TAG_FILTER, "   🔹 Otros materiales: ${otherDetections.size}")

        // Si hay materiales que NO son plástico, hay material presente
        if (otherDetections.isNotEmpty()) {
            Log.d(TAG_FILTER, "   ✅ Material NO plástico detectado: ${otherDetections.first().className}")
            return true
        }

        // Para plásticos, aplicar filtro del plato (lógica mejorada igual que YOLO)
        if (plasticDetections.isNotEmpty()) {
            val hasValidPlastic = filterPlasticDetections(plasticDetections, roiArea, roiWidth, roiHeight)
            if (hasValidPlastic) {
                Log.d(TAG_FILTER, "   ✅ Plástico válido detectado")
                return true
            }
        }

        Log.d(TAG_FILTER, "   ❌ Solo plato detectado (falso positivo)")
        return false
    }

    /**
     * Filtra detecciones de plástico usando la misma lógica mejorada que YOLO
     */
    private fun filterPlasticDetections(
        plastics: List<Detection>,
        roiArea: Float,
        roiWidth: Int,
        roiHeight: Int
    ): Boolean {
        if (plastics.isEmpty()) return false

        // Calcular área y métricas de cada detección
        data class PlasticWithMetrics(
            val detection: Detection,
            val areaRatio: Float,
            val confidence: Float,
            val aspectRatio: Float,
            val isCentered: Boolean,
            val isCircular: Boolean
        )

        val plasticsWithMetrics = plastics.map { detection ->
            val box = detection.boundingBox
            val area = box.width() * box.height()
            val areaRatio = area / roiArea

            // Calcular aspect ratio (1.0 = cuadrado/circular)
            val aspectRatio = if (box.height() > 0) box.width() / box.height() else 1f

            // Verificar si es circular (aspect ratio cercano a 1)
            val isCircular = aspectRatio in CIRCULAR_ASPECT_MIN..CIRCULAR_ASPECT_MAX

            // Verificar si está centrado en el ROI
            val boxCenterX = (box.left + box.right) / 2f / roiWidth
            val boxCenterY = (box.top + box.bottom) / 2f / roiHeight
            val distFromCenter = kotlin.math.sqrt(
                (boxCenterX - 0.5f) * (boxCenterX - 0.5f) +
                (boxCenterY - 0.5f) * (boxCenterY - 0.5f)
            )
            val isCentered = distFromCenter < CENTER_THRESHOLD

            PlasticWithMetrics(detection, areaRatio, detection.confidence, aspectRatio, isCentered, isCircular)
        }.sortedByDescending { it.areaRatio }  // Ordenar por área (mayor primero)

        // Log de todas las detecciones de plástico
        plasticsWithMetrics.forEach { p ->
            Log.d(TAG_FILTER, "   📊 ${p.detection.className}: conf=${(p.confidence * 100).toInt()}%, área=${(p.areaRatio * 100).toInt()}%")
            Log.d(TAG_FILTER, "      aspect=${String.format("%.2f", p.aspectRatio)} circular=${p.isCircular} centrado=${p.isCentered}")
        }

        return when {
            // CASO 1: Solo hay 1 plástico detectado
            plasticsWithMetrics.size == 1 -> {
                val single = plasticsWithMetrics[0]

                val isLowConfidence = single.confidence < minConfidenceThreshold
                val isLargeArea = single.areaRatio > suspiciousAreaThreshold

                // Más estricto: si tiene CUALQUIERA de estas combinaciones, filtrar
                val isProbablyPlate = when {
                    // Caso clásico: baja confianza + área grande
                    isLowConfidence && isLargeArea -> true

                    // Caso adicional: área MUY grande (>40%) + circular + centrado
                    single.areaRatio > 0.40f && single.isCircular && single.isCentered -> true

                    // Caso adicional: confianza muy baja (<45%) + circular + centrado
                    single.confidence < 0.45f && single.isCircular && single.isCentered -> true

                    else -> false
                }

                if (isProbablyPlate) {
                    Log.d(TAG_FILTER, "   🚫 FILTRADO COMO PLATO:")
                    Log.d(TAG_FILTER, "      conf=${(single.confidence * 100).toInt()}% área=${(single.areaRatio * 100).toInt()}% circular=${single.isCircular}")
                    false
                } else {
                    Log.d(TAG_FILTER, "   ✅ ACEPTADO: Confianza ${(single.confidence * 100).toInt()}% >= umbral ${(minConfidenceThreshold * 100).toInt()}%")
                    true
                }
            }

            // CASO 2: Hay 2+ plásticos - verificar si hay material real además del plato
            else -> {
                val largest = plasticsWithMetrics[0]
                val smaller = plasticsWithMetrics.drop(1)

                Log.d(TAG_FILTER, "   🚫 Mayor área (${(largest.areaRatio * 100).toInt()}%) = plato")

                // Verificar si hay plásticos válidos (con buena confianza)
                val validSmaller = smaller.filter { it.confidence >= minConfidenceThreshold }

                if (validSmaller.isNotEmpty()) {
                    Log.d(TAG_FILTER, "   ✅ Hay ${validSmaller.size} plástico(s) válido(s) = material real")
                    true
                } else {
                    Log.d(TAG_FILTER, "   🚫 Plásticos adicionales tienen baja confianza, ignorando")
                    false
                }
            }
        }
    }

    /**
     * Obtiene la detección principal (para logging)
     */
    fun getPrimaryDetection(detections: List<Detection>): Detection? {
        return detections.maxByOrNull { it.confidence }
    }
}

/**
 * Tracker de presencia estable para disparar clasificación Gemini
 * Espera 2 segundos de detección consistente antes de enviar a Gemini
 * (para ahorrar tokens de API)
 */
object PresenceStabilityTracker {
    private const val TAG_STAB = "PresenceStability"
    private const val STABILITY_DURATION_MS = 3000L  // 3 segundos

    private var presenceStartTime: Long = 0L
    private var hasPresence = false
    private var isStable = false

    /**
     * Actualiza el tracker con el estado de presencia actual
     * @return true si la presencia es estable por 1.5 segundos
     */
    fun update(hasMaterial: Boolean): Boolean {
        val now = System.currentTimeMillis()

        if (!hasMaterial) {
            if (hasPresence) {
                Log.d(TAG_STAB, "❌ Material perdido, reseteando...")
            }
            reset()
            return false
        }

        if (!hasPresence) {
            // Primera detección de material
            Log.d(TAG_STAB, "🔄 Material detectado, iniciando contador...")
            hasPresence = true
            presenceStartTime = now
            isStable = false
            return false
        }

        val elapsedTime = now - presenceStartTime
        val remainingTime = STABILITY_DURATION_MS - elapsedTime

        if (elapsedTime >= STABILITY_DURATION_MS && !isStable) {
            isStable = true
            Log.d(TAG_STAB, "✅ ¡PRESENCIA ESTABLE! (${elapsedTime}ms)")
            return true
        }

        if (!isStable && remainingTime > 0) {
            Log.d(TAG_STAB, "⏳ Esperando: ${remainingTime/1000.0}s restantes...")
        }

        return false
    }

    fun getProgress(): Float {
        if (!hasPresence) return 0f
        val elapsed = System.currentTimeMillis() - presenceStartTime
        return (elapsed.toFloat() / STABILITY_DURATION_MS).coerceIn(0f, 1f)
    }

    fun isCurrentlyStable(): Boolean = isStable

    fun reset() {
        hasPresence = false
        presenceStartTime = 0L
        isStable = false
    }
}

/**
 * ROI para el recorte de imagen
 */
data class ROIRectGemini(
    val left: Float = 0.15f,
    val top: Float = 0.15f,
    val right: Float = 0.85f,
    val bottom: Float = 0.85f
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
}

/**
 * Estados de la pantalla
 */
private enum class GeminiScreenState {
    PERMISSION_REQUEST,
    ROI_CONFIGURATION,
    DETECTION_RUNNING
}

/**
 * Estados del proceso de clasificación
 */
private enum class ClassificationState {
    IDLE,                    // Esperando detección
    DETECTING_PRESENCE,      // YOLO/FrameChange detectando presencia
    PRESENCE_STABLE,         // Presencia confirmada, listo para capturar
    CAPTURING_IMAGE,         // Capturando imagen
    CLASSIFYING_GEMINI,      // Enviando a Gemini
    CLASSIFICATION_READY,    // Clasificación lista
    DEPOSITING,              // Depositando material
    DEPOSIT_COMPLETE         // Depósito completado
}

/**
 * Fuente de la detección
 */
private enum class DetectionSource {
    NONE,
    YOLO,           // YOLO reconoció el objeto
    FRAME_CHANGE    // Cambio de frame (objeto no reconocido por YOLO)
}

/**
 * Pantalla de Clasificador IA con Gemini
 * Flujo: YOLO detecta presencia → Captura imagen → Gemini clasifica → Depósito
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ClasificadorGeminiScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    var screenState by remember {
        mutableStateOf(
            if (cameraPermissionState.status.isGranted)
                GeminiScreenState.ROI_CONFIGURATION
            else
                GeminiScreenState.PERMISSION_REQUEST
        )
    }

    var roiRect by remember { mutableStateOf(ROIRectGemini()) }
    var cameraImageSize by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    LaunchedEffect(cameraPermissionState.status.isGranted) {
        if (cameraPermissionState.status.isGranted && screenState == GeminiScreenState.PERMISSION_REQUEST) {
            screenState = GeminiScreenState.ROI_CONFIGURATION
        }
    }

    BackHandler {
        when (screenState) {
            GeminiScreenState.DETECTION_RUNNING -> {
                screenState = GeminiScreenState.ROI_CONFIGURATION
            }
            else -> {
                onNavigateBack()
            }
        }
    }

    Scaffold(
        containerColor = Color(0xFF1A1A1A)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (screenState) {
                GeminiScreenState.PERMISSION_REQUEST -> {
                    PermissionRequestGemini(
                        onRequestPermission = { cameraPermissionState.launchPermissionRequest() },
                        onNavigateBack = onNavigateBack
                    )
                }

                GeminiScreenState.ROI_CONFIGURATION -> {
                    ROIConfigurationScreenGemini(
                        lifecycleOwner = lifecycleOwner,
                        roiRect = roiRect,
                        onROIChange = { roiRect = it },
                        onCameraImageSize = { w, h -> cameraImageSize = Pair(w, h) },
                        cameraImageSize = cameraImageSize,
                        onNavigateBack = onNavigateBack,
                        onConfirmROI = {
                            screenState = GeminiScreenState.DETECTION_RUNNING
                        }
                    )
                }

                GeminiScreenState.DETECTION_RUNNING -> {
                    DetectionScreenGemini(
                        lifecycleOwner = lifecycleOwner,
                        roiRect = roiRect,
                        cameraImageSize = cameraImageSize,
                        onNavigateBack = {
                            screenState = GeminiScreenState.ROI_CONFIGURATION
                        },
                        onExit = onNavigateBack
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// PANTALLA DE CONFIGURACIÓN DE ROI
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ROIConfigurationScreenGemini(
    lifecycleOwner: LifecycleOwner,
    roiRect: ROIRectGemini,
    onROIChange: (ROIRectGemini) -> Unit,
    onCameraImageSize: (Int, Int) -> Unit,
    cameraImageSize: Pair<Int, Int>?,
    onNavigateBack: () -> Unit,
    onConfirmROI: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        CameraPreviewOnlyGemini(
            lifecycleOwner = lifecycleOwner,
            onCameraImageSize = onCameraImageSize
        )

        cameraImageSize?.let { (imgWidth, imgHeight) ->
            ROIConfigurationOverlayGemini(
                imageWidth = imgWidth,
                imageHeight = imgHeight,
                roiRect = roiRect,
                onROIChange = onROIChange,
                modifier = Modifier.fillMaxSize()
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)
                        )
                    )
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = Color.White
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Paso 1: Configurar Área",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Clasificador YOLO + Gemini AI",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9C7BFF)  // Morado para indicar Gemini
                    )
                }

                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    tint = Color(0xFF9C7BFF),
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onConfirmROI,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C7BFF)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Iniciar Clasificación",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun CameraPreviewOnlyGemini(
    lifecycleOwner: LifecycleOwner,
    onCameraImageSize: (Int, Int) -> Unit
) {
    val context = LocalContext.current
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var hasReportedSize by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            try {
                cameraProvider?.unbindAll()
                Log.d(TAG, "CameraPreviewOnlyGemini: Cámara liberada")
            } catch (e: Exception) {
                Log.e(TAG, "Error liberando cámara: ${e.message}")
            }
        }
    }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = PreviewView.ScaleType.FIT_CENTER

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    try {
                        val provider = cameraProviderFuture.get()
                        cameraProvider = provider

                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(surfaceProvider)
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also {
                                it.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                                    if (!hasReportedSize) {
                                        val rotatedWidth: Int
                                        val rotatedHeight: Int
                                        if (imageProxy.imageInfo.rotationDegrees == 90 ||
                                            imageProxy.imageInfo.rotationDegrees == 270) {
                                            rotatedWidth = imageProxy.height
                                            rotatedHeight = imageProxy.width
                                        } else {
                                            rotatedWidth = imageProxy.width
                                            rotatedHeight = imageProxy.height
                                        }
                                        onCameraImageSize(rotatedWidth, rotatedHeight)
                                        hasReportedSize = true
                                    }
                                    imageProxy.close()
                                }
                            }

                        provider.unbindAll()
                        provider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error iniciando cámara: ${e.message}", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun ROIConfigurationOverlayGemini(
    imageWidth: Int,
    imageHeight: Int,
    roiRect: ROIRectGemini,
    onROIChange: (ROIRectGemini) -> Unit,
    modifier: Modifier = Modifier
) {
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var draggedHandle by remember { mutableStateOf<String?>(null) }
    var isDraggingCenter by remember { mutableStateOf(false) }

    val currentRoiRect by rememberUpdatedState(roiRect)
    val currentOnROIChange by rememberUpdatedState(onROIChange)

    val handleSize = 40.dp
    val handleSizePx = with(LocalDensity.current) { handleSize.toPx() }

    val imageAspect = imageWidth.toFloat() / imageHeight.toFloat()

    Canvas(
        modifier = modifier
            .pointerInput(imageWidth, imageHeight) {
                detectDragGestures(
                    onDragStart = { offset ->
                        if (canvasSize.width <= 0 || canvasSize.height <= 0) return@detectDragGestures

                        val canvasAspect = canvasSize.width / canvasSize.height
                        val scale: Float
                        val offsetX: Float
                        val offsetY: Float

                        if (imageAspect > canvasAspect) {
                            scale = canvasSize.width / imageWidth
                            offsetX = 0f
                            offsetY = (canvasSize.height - imageHeight * scale) / 2f
                        } else {
                            scale = canvasSize.height / imageHeight
                            offsetX = (canvasSize.width - imageWidth * scale) / 2f
                            offsetY = 0f
                        }

                        val roi = currentRoiRect
                        val roiLeft = roi.left * imageWidth * scale + offsetX
                        val roiTop = roi.top * imageHeight * scale + offsetY
                        val roiRight = roi.right * imageWidth * scale + offsetX
                        val roiBottom = roi.bottom * imageHeight * scale + offsetY

                        val handles = listOf(
                            "topLeft" to Offset(roiLeft, roiTop),
                            "topRight" to Offset(roiRight, roiTop),
                            "bottomLeft" to Offset(roiLeft, roiBottom),
                            "bottomRight" to Offset(roiRight, roiBottom)
                        )

                        draggedHandle = handles.find { (_, pos) ->
                            (offset - pos).getDistance() < handleSizePx
                        }?.first

                        if (draggedHandle == null) {
                            if (offset.x in roiLeft..roiRight && offset.y in roiTop..roiBottom) {
                                isDraggingCenter = true
                            }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()

                        if (canvasSize.width <= 0 || canvasSize.height <= 0) return@detectDragGestures
                        if (draggedHandle == null && !isDraggingCenter) return@detectDragGestures

                        val canvasAspect = canvasSize.width / canvasSize.height
                        val scale: Float

                        if (imageAspect > canvasAspect) {
                            scale = canvasSize.width / imageWidth
                        } else {
                            scale = canvasSize.height / imageHeight
                        }

                        val deltaX = dragAmount.x / (imageWidth * scale)
                        val deltaY = dragAmount.y / (imageHeight * scale)

                        val minSize = 0.10f
                        val roi = currentRoiRect

                        if (isDraggingCenter) {
                            val newLeft = (roi.left + deltaX).coerceIn(0f, 1f - roi.width)
                            val newTop = (roi.top + deltaY).coerceIn(0f, 1f - roi.height)
                            currentOnROIChange(ROIRectGemini(
                                left = newLeft,
                                top = newTop,
                                right = newLeft + roi.width,
                                bottom = newTop + roi.height
                            ))
                        } else {
                            when (draggedHandle) {
                                "topLeft" -> {
                                    val newLeft = (roi.left + deltaX).coerceIn(0f, roi.right - minSize)
                                    val newTop = (roi.top + deltaY).coerceIn(0f, roi.bottom - minSize)
                                    currentOnROIChange(ROIRectGemini(
                                        left = newLeft,
                                        top = newTop,
                                        right = roi.right,
                                        bottom = roi.bottom
                                    ))
                                }
                                "topRight" -> {
                                    val newRight = (roi.right + deltaX).coerceIn(roi.left + minSize, 1f)
                                    val newTop = (roi.top + deltaY).coerceIn(0f, roi.bottom - minSize)
                                    currentOnROIChange(ROIRectGemini(
                                        left = roi.left,
                                        top = newTop,
                                        right = newRight,
                                        bottom = roi.bottom
                                    ))
                                }
                                "bottomLeft" -> {
                                    val newLeft = (roi.left + deltaX).coerceIn(0f, roi.right - minSize)
                                    val newBottom = (roi.bottom + deltaY).coerceIn(roi.top + minSize, 1f)
                                    currentOnROIChange(ROIRectGemini(
                                        left = newLeft,
                                        top = roi.top,
                                        right = roi.right,
                                        bottom = newBottom
                                    ))
                                }
                                "bottomRight" -> {
                                    val newRight = (roi.right + deltaX).coerceIn(roi.left + minSize, 1f)
                                    val newBottom = (roi.bottom + deltaY).coerceIn(roi.top + minSize, 1f)
                                    currentOnROIChange(ROIRectGemini(
                                        left = roi.left,
                                        top = roi.top,
                                        right = newRight,
                                        bottom = newBottom
                                    ))
                                }
                            }
                        }
                    },
                    onDragEnd = {
                        draggedHandle = null
                        isDraggingCenter = false
                    }
                )
            }
    ) {
        canvasSize = size

        val canvasAspect = size.width / size.height
        val scale: Float
        val offsetX: Float
        val offsetY: Float

        if (imageAspect > canvasAspect) {
            scale = size.width / imageWidth
            offsetX = 0f
            offsetY = (size.height - imageHeight * scale) / 2f
        } else {
            scale = size.height / imageHeight
            offsetX = (size.width - imageWidth * scale) / 2f
            offsetY = 0f
        }

        val roiLeft = roiRect.left * imageWidth * scale + offsetX
        val roiTop = roiRect.top * imageHeight * scale + offsetY
        val roiRight = roiRect.right * imageWidth * scale + offsetX
        val roiBottom = roiRect.bottom * imageHeight * scale + offsetY

        val dimColor = Color.Black.copy(alpha = 0.65f)

        drawRect(dimColor, Offset(offsetX, offsetY), Size(imageWidth * scale, roiTop - offsetY))
        drawRect(dimColor, Offset(offsetX, roiBottom), Size(imageWidth * scale, (offsetY + imageHeight * scale) - roiBottom))
        drawRect(dimColor, Offset(offsetX, roiTop), Size(roiLeft - offsetX, roiBottom - roiTop))
        drawRect(dimColor, Offset(roiRight, roiTop), Size((offsetX + imageWidth * scale) - roiRight, roiBottom - roiTop))

        // Borde del ROI - Color morado para Gemini
        val geminiColor = Color(0xFF9C7BFF)
        drawRect(
            color = geminiColor,
            topLeft = Offset(roiLeft, roiTop),
            size = Size(roiRight - roiLeft, roiBottom - roiTop),
            style = Stroke(width = 4f)
        )

        val thirdWidth = (roiRight - roiLeft) / 3
        val thirdHeight = (roiBottom - roiTop) / 3
        val guideColor = geminiColor.copy(alpha = 0.3f)

        drawLine(guideColor, Offset(roiLeft + thirdWidth, roiTop), Offset(roiLeft + thirdWidth, roiBottom), strokeWidth = 1f)
        drawLine(guideColor, Offset(roiLeft + thirdWidth * 2, roiTop), Offset(roiLeft + thirdWidth * 2, roiBottom), strokeWidth = 1f)
        drawLine(guideColor, Offset(roiLeft, roiTop + thirdHeight), Offset(roiRight, roiTop + thirdHeight), strokeWidth = 1f)
        drawLine(guideColor, Offset(roiLeft, roiTop + thirdHeight * 2), Offset(roiRight, roiTop + thirdHeight * 2), strokeWidth = 1f)

        val handleRadius = handleSizePx / 2
        val corners = listOf(
            Offset(roiLeft, roiTop),
            Offset(roiRight, roiTop),
            Offset(roiLeft, roiBottom),
            Offset(roiRight, roiBottom)
        )

        corners.forEach { pos ->
            drawCircle(color = geminiColor, radius = handleRadius, center = pos)
            drawCircle(color = Color.White, radius = handleRadius - 6f, center = pos)
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// PANTALLA DE DETECCIÓN PRINCIPAL
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun DetectionScreenGemini(
    lifecycleOwner: LifecycleOwner,
    roiRect: ROIRectGemini,
    cameraImageSize: Pair<Int, Int>?,
    onNavigateBack: () -> Unit,
    onExit: () -> Unit
) {
    val context = LocalContext.current

    // Permisos de Bluetooth (Android 12+)
    val bluetoothPermissions = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
        )
    )

    // Estado del detector YOLO
    var detector by remember { mutableStateOf<WasteDetector?>(null) }
    var isDetectorReady by remember { mutableStateOf(false) }
    var detectorError by remember { mutableStateOf<String?>(null) }

    // Estado del clasificador Gemini
    var geminiClassifier by remember { mutableStateOf<GeminiClassifier?>(null) }
    var isGeminiReady by remember { mutableStateOf(false) }
    var geminiError by remember { mutableStateOf<String?>(null) }

    // Estado de detecciones YOLO
    var currentResult by remember { mutableStateOf<DetectionResult?>(null) }

    // Estado Bluetooth/ESP32
    val bluetoothManager = remember { BluetoothManager(context) }
    var bluetoothConectado by remember { mutableStateOf(false) }
    var estadoConexion by remember { mutableStateOf("Desconectado") }
    var conectando by remember { mutableStateOf(false) }
    var triggerConexion by remember { mutableStateOf(0) }

    // Estado de clasificación
    var classificationState by remember { mutableStateOf(ClassificationState.IDLE) }
    var presenceProgress by remember { mutableStateOf(0f) }
    var geminiResult by remember { mutableStateOf<GeminiClassificationResult?>(null) }
    var depositStatus by remember { mutableStateOf("") }

    // Imagen capturada para Gemini
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Estado del FrameChangeDetector (fallback cuando YOLO no detecta)
    var hasBaseline by remember { mutableStateOf(false) }
    var detectionSource by remember { mutableStateOf(DetectionSource.NONE) }
    var triggerCaptureBaseline by remember { mutableStateOf(0) }

    // Trigger para captura de imagen
    var triggerCapture by remember { mutableStateOf(0) }

    // Manejar conexión Bluetooth - SOLO si los permisos están otorgados
    LaunchedEffect(triggerConexion, bluetoothPermissions.allPermissionsGranted) {
        if (triggerConexion == 0) return@LaunchedEffect
        if (conectando) return@LaunchedEffect

        // NO intentar conectar si no hay permisos
        if (!bluetoothPermissions.allPermissionsGranted) {
            Log.d(TAG, "⚠️ Esperando permisos de Bluetooth...")
            estadoConexion = "Sin permisos"
            return@LaunchedEffect
        }

        conectando = true
        estadoConexion = "Conectando..."
        Log.d(TAG, "🔌 Iniciando conexión con permisos otorgados...")

        val result = bluetoothManager.conectarConHandshake()
        result.fold(
            onSuccess = {
                bluetoothConectado = true
                estadoConexion = "Conectado"
                Log.d(TAG, "✅ ESP32 conectado")
            },
            onFailure = { error ->
                bluetoothConectado = false
                estadoConexion = "Error"
                Log.e(TAG, "❌ Error: ${error.message}")
            }
        )
        conectando = false
    }

    // Solicitar permisos de Bluetooth al inicio
    LaunchedEffect(Unit) {
        if (!bluetoothPermissions.allPermissionsGranted) {
            bluetoothPermissions.launchMultiplePermissionRequest()
        }
    }

    // Reintentar conexión después de obtener permisos
    LaunchedEffect(bluetoothPermissions.allPermissionsGranted) {
        if (bluetoothPermissions.allPermissionsGranted && triggerConexion > 0 && !bluetoothConectado && !conectando) {
            triggerConexion++
        }
    }

    // Capturar baseline cuando se solicite
    LaunchedEffect(triggerCaptureBaseline) {
        if (triggerCaptureBaseline == 0) return@LaunchedEffect
        if (capturedBitmap == null) {
            Log.d(TAG, "⚠️ No hay imagen para baseline")
            return@LaunchedEffect
        }

        Log.d(TAG, "📷 Capturando baseline del plato vacío...")
        FrameChangeDetector.captureBaseline(capturedBitmap!!)
        hasBaseline = FrameChangeDetector.hasValidBaseline()

        if (hasBaseline) {
            Log.d(TAG, "✅ Baseline capturado - listo para detectar objetos")
        }
    }

    // Auto-capturar baseline cuando ESP32 se conecta (si no hay baseline)
    LaunchedEffect(bluetoothConectado, isDetectorReady, isGeminiReady) {
        if (bluetoothConectado && isDetectorReady && isGeminiReady && !hasBaseline) {
            // Esperar un momento para que el frame se estabilice
            delay(500)
            if (capturedBitmap != null) {
                Log.d(TAG, "📷 Auto-capturando baseline inicial...")
                triggerCaptureBaseline++
            }
        }
    }

    // Inicializar YOLO y Gemini
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                // Inicializar YOLO (solo para detección de presencia)
                Log.d(TAG, "═══════════════════════════════════════")
                Log.d(TAG, "🔧 Inicializando sistema YOLO + Gemini...")

                val labels = WasteDetector.loadLabels(
                    context,
                    "labels/waste_detector_labels.txt"
                )

                val newDetector = WasteDetector(
                    context = context,
                    modelPath = "models/waste_detector_v2.tflite",
                    labels = labels,
                    useGpu = true
                )

                newDetector.confidenceThreshold = 0.20f  // Más sensible para detección
                newDetector.iouThreshold = 0.4f
                newDetector.numItemsThreshold = 30

                detector = newDetector
                isDetectorReady = true
                Log.d(TAG, "✅ YOLO inicializado (modo presencia)")

                // Inicializar Gemini
                val classifier = GeminiClassifier(GEMINI_API_KEY)
                if (classifier.initialize()) {
                    geminiClassifier = classifier
                    isGeminiReady = true
                    Log.d(TAG, "✅ Gemini inicializado")
                } else {
                    geminiError = "Error inicializando Gemini"
                }

                // Resetear trackers
                PresenceStabilityTracker.reset()

                Log.d(TAG, "═══════════════════════════════════════")

            } catch (e: Exception) {
                Log.e(TAG, "Error inicializando: ${e.message}", e)
                detectorError = e.message
            }
        }
    }

    // Lógica de detección de presencia y clasificación Gemini
    // IMPORTANTE: Solo clasifica si ESP32 está conectado para no desperdiciar tokens
    // USA: YOLO (primario) OR FrameChangeDetector (fallback para objetos no reconocidos)
    LaunchedEffect(currentResult, bluetoothConectado, capturedBitmap) {
        // No procesar si ESP32 no está conectado
        if (!bluetoothConectado) {
            // Resetear si estábamos detectando
            if (classificationState == ClassificationState.DETECTING_PRESENCE) {
                classificationState = ClassificationState.IDLE
                presenceProgress = 0f
                detectionSource = DetectionSource.NONE
                PresenceStabilityTracker.reset()
            }
            return@LaunchedEffect
        }

        // No procesar si estamos en medio de clasificación/depósito
        if (classificationState != ClassificationState.IDLE &&
            classificationState != ClassificationState.DETECTING_PRESENCE) {
            return@LaunchedEffect
        }

        val detections = currentResult?.boxes ?: emptyList()
        val roiWidth = currentResult?.imageWidth ?: 320
        val roiHeight = currentResult?.imageHeight ?: 320

        // PASO 1: Verificar con YOLO (detección primaria)
        val yoloDetected = MaterialPresenceFilter.hasMaterialPresent(
            detections, roiWidth, roiHeight
        )

        // PASO 2: Si YOLO no detectó, usar FrameChangeDetector (fallback)
        val frameChangeDetected = if (!yoloDetected && hasBaseline && capturedBitmap != null) {
            val frameResult = FrameChangeDetector.detectChange(capturedBitmap!!)
            if (frameResult.hasChange) {
                Log.d(TAG, "🔄 FALLBACK: Cambio detectado por FrameChangeDetector")
                Log.d(TAG, "   ${frameResult.reason}")
            }
            frameResult.hasChange
        } else {
            false
        }

        // Combinar resultados: YOLO OR FrameChange
        val hasMaterial = yoloDetected || frameChangeDetected

        // Actualizar fuente de detección
        if (hasMaterial) {
            detectionSource = when {
                yoloDetected -> DetectionSource.YOLO
                frameChangeDetected -> DetectionSource.FRAME_CHANGE
                else -> DetectionSource.NONE
            }
            classificationState = ClassificationState.DETECTING_PRESENCE
        }

        val isStable = PresenceStabilityTracker.update(hasMaterial)
        presenceProgress = PresenceStabilityTracker.getProgress()

        if (isStable && classificationState == ClassificationState.DETECTING_PRESENCE) {
            val sourceText = when (detectionSource) {
                DetectionSource.YOLO -> "YOLO"
                DetectionSource.FRAME_CHANGE -> "FrameChange (objeto no reconocido)"
                else -> "Desconocido"
            }
            Log.d(TAG, "🎯 Presencia estable por 2s (fuente: $sourceText) - disparando Gemini")
            classificationState = ClassificationState.PRESENCE_STABLE
            triggerCapture++
        }

        if (!hasMaterial && classificationState == ClassificationState.DETECTING_PRESENCE) {
            // Material perdido durante la detección - resetear
            Log.d(TAG, "❌ Material perdido durante detección - reseteando")
            classificationState = ClassificationState.IDLE
            geminiResult = null
            presenceProgress = 0f
            detectionSource = DetectionSource.NONE
            PresenceStabilityTracker.reset()
        }
    }

    // Capturar imagen y enviar a Gemini
    LaunchedEffect(triggerCapture) {
        if (triggerCapture == 0) return@LaunchedEffect
        if (capturedBitmap == null) {
            Log.e(TAG, "❌ No hay imagen capturada")
            classificationState = ClassificationState.IDLE
            PresenceStabilityTracker.reset()
            return@LaunchedEffect
        }
        if (!isGeminiReady || geminiClassifier == null) {
            Log.e(TAG, "❌ Gemini no está listo")
            classificationState = ClassificationState.IDLE
            PresenceStabilityTracker.reset()
            return@LaunchedEffect
        }

        classificationState = ClassificationState.CAPTURING_IMAGE
        Log.d(TAG, "═══════════════════════════════════════")
        Log.d(TAG, "📷 Esperando 500ms para autofocus...")

        // Delay para permitir que la cámara enfoque bien antes de enviar a Gemini
        delay(500)

        // Verificar que aún tenemos una imagen válida después del delay
        if (capturedBitmap == null) {
            Log.e(TAG, "❌ Imagen perdida durante el delay de enfoque")
            classificationState = ClassificationState.IDLE
            PresenceStabilityTracker.reset()
            return@LaunchedEffect
        }

        classificationState = ClassificationState.CLASSIFYING_GEMINI
        Log.d(TAG, "🤖 Enviando imagen a Gemini...")

        val result = geminiClassifier!!.classifyWaste(capturedBitmap!!)

        if (result != null && result.category != MaterialCategoryGemini.NO_DETECTADO) {
            geminiResult = result
            classificationState = ClassificationState.CLASSIFICATION_READY
            Log.d(TAG, "✅ Clasificación Gemini: ${result.category.displayName}")

            // Proceder a depositar (ESP32 ya está conectado - verificado antes de llamar a Gemini)
            classificationState = ClassificationState.DEPOSITING
            depositStatus = "Depositando ${result.category.displayName}..."
            Log.d(TAG, "   Giro: ${result.category.giro}°, Inclinación: ${result.category.inclinacion}°")

            val depositResult = bluetoothManager.depositarYEsperarListo(
                categoria = result.category.displayName,
                giro = result.category.giro,
                inclinacion = result.category.inclinacion
            )
            depositResult.fold(
                onSuccess = {
                    depositStatus = "✓ ${result.category.displayName} depositado"
                    classificationState = ClassificationState.DEPOSIT_COMPLETE
                    Log.d(TAG, "✅ Depósito completado - Señal LISTO recibida del ESP32")
                    delay(1500)  // Mostrar mensaje de éxito
                },
                onFailure = { error ->
                    depositStatus = "Error: ${error.message}"
                    Log.e(TAG, "❌ Error en depósito: ${error.message}")
                    delay(2000)
                }
            )

            // Reset para nueva detección (después de recibir LISTO del ESP32)
            Log.d(TAG, "🔄 Reiniciando para nueva detección...")
            classificationState = ClassificationState.IDLE
            depositStatus = ""
            geminiResult = null
            presenceProgress = 0f
            PresenceStabilityTracker.reset()
        } else {
            // Gemini no pudo detectar el material
            val reason = result?.reasoning ?: "Error de conexión"
            Log.d(TAG, "═══════════════════════════════════════")
            Log.d(TAG, "❌ GEMINI NO DETECTÓ MATERIAL")
            Log.d(TAG, "   Razón: $reason")
            Log.d(TAG, "   Respuesta raw: ${result?.rawResponse ?: "null"}")
            Log.d(TAG, "═══════════════════════════════════════")

            // Mostrar temporalmente el resultado NO_DETECTADO para feedback visual
            if (result != null) {
                geminiResult = result
                classificationState = ClassificationState.CLASSIFICATION_READY
            }

            // Esperar un momento para que el usuario vea el mensaje
            delay(2000)

            // Resetear para intentar de nuevo
            Log.d(TAG, "🔄 Reseteando para nueva detección...")
            classificationState = ClassificationState.IDLE
            geminiResult = null
            presenceProgress = 0f
            detectionSource = DetectionSource.NONE
            PresenceStabilityTracker.reset()

            // Recapturar baseline si hubo error de detección
            if (hasBaseline && capturedBitmap != null) {
                Log.d(TAG, "📷 Recapturando baseline...")
                FrameChangeDetector.captureBaseline(capturedBitmap!!)
            }
        }

        Log.d(TAG, "═══════════════════════════════════════")
    }

    // Limpiar al salir
    DisposableEffect(Unit) {
        onDispose {
            detector?.close()
            detector = null
            geminiClassifier?.close()
            geminiClassifier = null
            bluetoothManager.desconectar()
            PresenceStabilityTracker.reset()
            FrameChangeDetector.release()
            Log.d(TAG, "Recursos liberados")
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            detectorError != null -> {
                ErrorScreenGemini(
                    error = detectorError!!,
                    onNavigateBack = onNavigateBack
                )
            }
            geminiError != null -> {
                ErrorScreenGemini(
                    error = "Error de Gemini: $geminiError\n\nVerifica tu API key.",
                    onNavigateBack = onNavigateBack
                )
            }
            !isDetectorReady || !isGeminiReady -> {
                LoadingScreenGemini()
            }
            else -> {
                // Cámara con detección YOLO
                CameraPreviewWithDetectionGemini(
                    lifecycleOwner = lifecycleOwner,
                    detector = detector!!,
                    roiRect = roiRect,
                    onDetectionResult = { result ->
                        currentResult = result
                    },
                    onBitmapCaptured = { bitmap ->
                        capturedBitmap = bitmap
                    }
                )

                // Overlay con ROI
                cameraImageSize?.let { (imgWidth, imgHeight) ->
                    DetectionOverlayGemini(
                        detections = currentResult?.boxes ?: emptyList(),
                        imageWidth = imgWidth,
                        imageHeight = imgHeight,
                        roiRect = roiRect,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // UI superpuesta
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)
                                )
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Reconfigurar",
                                tint = Color.White
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "YOLO + Gemini AI",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (bluetoothConectado) "ESP32 conectado" else "ESP32 desconectado",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (bluetoothConectado) BioWayColors.PrimaryGreen else Color.Red.copy(alpha = 0.8f)
                            )
                        }

                        // Botón de conexión ESP32
                        Surface(
                            onClick = {
                                if (!bluetoothPermissions.allPermissionsGranted) {
                                    // Solicitar permisos primero
                                    bluetoothPermissions.launchMultiplePermissionRequest()
                                } else if (!bluetoothConectado && !conectando) {
                                    triggerConexion++
                                } else if (bluetoothConectado) {
                                    bluetoothManager.desconectar()
                                    bluetoothConectado = false
                                    estadoConexion = "Desconectado"
                                }
                            },
                            shape = RoundedCornerShape(20.dp),
                            color = if (bluetoothConectado)
                                BioWayColors.PrimaryGreen.copy(alpha = 0.2f)
                            else
                                Color.Red.copy(alpha = 0.2f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (conectando) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (bluetoothConectado)
                                            Icons.Default.BluetoothConnected
                                        else
                                            Icons.Default.BluetoothDisabled,
                                        contentDescription = null,
                                        tint = if (bluetoothConectado) BioWayColors.PrimaryGreen else Color.Red,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = if (conectando) "..." else if (bluetoothConectado) "ON" else "OFF",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Panel de estado de clasificación
                    ClassificationStatusPanel(
                        classificationState = classificationState,
                        presenceProgress = presenceProgress,
                        geminiResult = geminiResult,
                        depositStatus = depositStatus,
                        bluetoothConectado = bluetoothConectado,
                        hasBaseline = hasBaseline,
                        detectionSource = detectionSource,
                        onCaptureBaseline = {
                            Log.d(TAG, "📷 Recapturando baseline manualmente...")
                            triggerCaptureBaseline++
                        }
                    )

                    // Panel de detecciones YOLO (debug)
                    DetectionResultsPanelGemini(
                        detections = currentResult?.boxes ?: emptyList(),
                        classificationState = classificationState
                    )
                }

                // Indicador de estado en esquina
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 60.dp, end = 8.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.5f)
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = String.format("%.0f ms", currentResult?.inferenceTimeMs ?: 0.0),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF9C7BFF)
                            )
                            Text(
                                text = String.format("%.1f FPS", currentResult?.fps ?: 0.0),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClassificationStatusPanel(
    classificationState: ClassificationState,
    presenceProgress: Float,
    geminiResult: GeminiClassificationResult?,
    depositStatus: String,
    bluetoothConectado: Boolean,
    hasBaseline: Boolean,
    detectionSource: DetectionSource,
    onCaptureBaseline: () -> Unit
) {
    val geminiColor = Color(0xFF9C7BFF)
    val frameChangeColor = Color(0xFFFF9800) // Naranja para indicar fallback

    // Mostrar panel de baseline si está en IDLE y falta baseline
    if (classificationState == ClassificationState.IDLE) {
        if (bluetoothConectado && !hasBaseline) {
            // Mostrar aviso de que falta baseline
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                color = frameChangeColor.copy(alpha = 0.2f)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = "📷", fontSize = 24.sp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Capturar plato vacío",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Necesario para detectar objetos no reconocidos por YOLO",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                    Button(
                        onClick = onCaptureBaseline,
                        colors = ButtonDefaults.buttonColors(containerColor = frameChangeColor),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Capturar", fontSize = 12.sp)
                    }
                }
            }
        }
        return
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.Black.copy(alpha = 0.85f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (classificationState) {
                ClassificationState.DETECTING_PRESENCE -> {
                    val isFrameChangeSource = detectionSource == DetectionSource.FRAME_CHANGE
                    val sourceColor = if (isFrameChangeSource) frameChangeColor else geminiColor

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (isFrameChangeSource) "🔄" else "🔍",
                            fontSize = 28.sp
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isFrameChangeSource) "Objeto detectado" else "Material detectado",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isFrameChangeSource)
                                    "Por cambio de frame (YOLO no reconoce)"
                                else
                                    "Por YOLO",
                                style = MaterialTheme.typography.bodySmall,
                                color = sourceColor.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "Estabilizando... ${String.format("%.1f", (1f - presenceProgress) * 2.0)}s",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { presenceProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = sourceColor,
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )
                }

                ClassificationState.PRESENCE_STABLE, ClassificationState.CAPTURING_IMAGE -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = geminiColor,
                            strokeWidth = 3.dp
                        )
                        Text(
                            text = "Capturando imagen...",
                            style = MaterialTheme.typography.titleMedium,
                            color = geminiColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                ClassificationState.CLASSIFYING_GEMINI -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = geminiColor,
                            strokeWidth = 3.dp
                        )
                        Column {
                            Text(
                                text = "Gemini AI analizando...",
                                style = MaterialTheme.typography.titleMedium,
                                color = geminiColor,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Clasificando residuo",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                ClassificationState.CLASSIFICATION_READY -> {
                    geminiResult?.let { result ->
                        // Verificar si es NO_DETECTADO para mostrar mensaje diferente
                        val isNoDetectado = result.category == MaterialCategoryGemini.NO_DETECTADO

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = if (isNoDetectado) "🔄" else result.category.emoji,
                                fontSize = 32.sp
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isNoDetectado) "No detectado" else result.category.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isNoDetectado) Color(0xFFFF9800) else Color(result.category.color),
                                    fontWeight = FontWeight.Bold
                                )
                                if (isNoDetectado) {
                                    Text(
                                        text = "Reintentando en 2s...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFFF9800).copy(alpha = 0.8f)
                                    )
                                } else {
                                    Text(
                                        text = "Confianza: ${result.confidence}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                                Text(
                                    text = result.reasoning,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.5f),
                                    maxLines = 2
                                )
                            }
                        }
                        if (!bluetoothConectado && !isNoDetectado) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Conecta ESP32 para depositar",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Red.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                ClassificationState.DEPOSITING -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = BioWayColors.PrimaryGreen,
                            strokeWidth = 3.dp
                        )
                        Column {
                            Text(
                                text = depositStatus,
                                style = MaterialTheme.typography.titleMedium,
                                color = BioWayColors.PrimaryGreen,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Esperando confirmación del ESP32...",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                ClassificationState.DEPOSIT_COMPLETE -> {
                    Text(
                        text = depositStatus,
                        style = MaterialTheme.typography.titleMedium,
                        color = BioWayColors.PrimaryGreen,
                        fontWeight = FontWeight.Bold
                    )
                }

                else -> {}
            }
        }
    }
}

@Composable
private fun CameraPreviewWithDetectionGemini(
    lifecycleOwner: LifecycleOwner,
    detector: WasteDetector,
    roiRect: ROIRectGemini,
    onDetectionResult: (DetectionResult) -> Unit,
    onBitmapCaptured: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val isProcessing = remember { AtomicBoolean(false) }

    DisposableEffect(Unit) {
        onDispose {
            try {
                cameraProvider?.unbindAll()
                executor.shutdown()
                Log.d(TAG, "CameraPreviewWithDetectionGemini: Recursos liberados")
            } catch (e: Exception) {
                Log.e(TAG, "Error liberando recursos: ${e.message}")
            }
        }
    }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = PreviewView.ScaleType.FIT_CENTER

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    try {
                        val provider = cameraProviderFuture.get()
                        cameraProvider = provider

                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(surfaceProvider)
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also {
                                it.setAnalyzer(executor) { imageProxy ->
                                    if (!isProcessing.getAndSet(true)) {
                                        processImageProxyWithROIGemini(
                                            imageProxy = imageProxy,
                                            detector = detector,
                                            roiRect = roiRect,
                                            onResult = { result, bitmap ->
                                                onDetectionResult(result)
                                                onBitmapCaptured(bitmap)
                                                isProcessing.set(false)
                                            },
                                            onError = {
                                                isProcessing.set(false)
                                            }
                                        )
                                    } else {
                                        imageProxy.close()
                                    }
                                }
                            }

                        provider.unbindAll()
                        provider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalysis
                        )
                        Log.d(TAG, "Cámara con detección Gemini iniciada")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error iniciando cámara: ${e.message}", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

private fun processImageProxyWithROIGemini(
    imageProxy: ImageProxy,
    detector: WasteDetector,
    roiRect: ROIRectGemini,
    onResult: (DetectionResult, Bitmap) -> Unit,
    onError: () -> Unit
) {
    try {
        val bitmap = imageProxyToBitmapGemini(imageProxy)
        if (bitmap != null) {
            val rotatedBitmap = rotateBitmapGemini(bitmap, imageProxy.imageInfo.rotationDegrees)
            val croppedBitmap = cropBitmapToROIGemini(rotatedBitmap, roiRect)

            val result = detector.detect(croppedBitmap)

            val adjustedResult = adjustDetectionsToFullImageGemini(
                result,
                roiRect,
                rotatedBitmap.width,
                rotatedBitmap.height
            )

            // Pasar el bitmap COMPLETO (no recortado) para Gemini
            // Gemini es capaz de interpretar la imagen completa y encontrar el objeto
            // El ROI recortado (336x448) puede ser muy pequeño o cortar el objeto
            onResult(adjustedResult, rotatedBitmap)
        } else {
            onError()
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error procesando imagen: ${e.message}", e)
        onError()
    } finally {
        imageProxy.close()
    }
}

private fun cropBitmapToROIGemini(bitmap: Bitmap, roi: ROIRectGemini): Bitmap {
    val x = (roi.left * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
    val y = (roi.top * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
    val width = (roi.width * bitmap.width).toInt().coerceIn(1, bitmap.width - x)
    val height = (roi.height * bitmap.height).toInt().coerceIn(1, bitmap.height - y)

    return Bitmap.createBitmap(bitmap, x, y, width, height)
}

private fun adjustDetectionsToFullImageGemini(
    result: DetectionResult,
    roi: ROIRectGemini,
    fullWidth: Int,
    fullHeight: Int
): DetectionResult {
    val roiOffsetX = roi.left * fullWidth
    val roiOffsetY = roi.top * fullHeight

    val adjustedBoxes = result.boxes.map { detection ->
        val adjustedBox = RectF(
            detection.boundingBox.left + roiOffsetX,
            detection.boundingBox.top + roiOffsetY,
            detection.boundingBox.right + roiOffsetX,
            detection.boundingBox.bottom + roiOffsetY
        )
        Detection(
            classIndex = detection.classIndex,
            className = detection.className,
            confidence = detection.confidence,
            boundingBox = adjustedBox,
            normalizedBox = RectF(
                adjustedBox.left / fullWidth,
                adjustedBox.top / fullHeight,
                adjustedBox.right / fullWidth,
                adjustedBox.bottom / fullHeight
            )
        )
    }

    return DetectionResult(
        boxes = adjustedBoxes,
        inferenceTimeMs = result.inferenceTimeMs,
        fps = result.fps,
        imageWidth = fullWidth,
        imageHeight = fullHeight
    )
}

private fun rotateBitmapGemini(bitmap: Bitmap, rotationDegrees: Int): Bitmap {
    if (rotationDegrees == 0) return bitmap
    val matrix = android.graphics.Matrix()
    matrix.postRotate(rotationDegrees.toFloat())
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
private fun imageProxyToBitmapGemini(imageProxy: ImageProxy): Bitmap? {
    return try {
        val image = imageProxy.image ?: return null
        val planes = image.planes
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = android.graphics.YuvImage(
            nv21,
            android.graphics.ImageFormat.NV21,
            image.width,
            image.height,
            null
        )

        val out = java.io.ByteArrayOutputStream()
        yuvImage.compressToJpeg(
            android.graphics.Rect(0, 0, image.width, image.height),
            100,  // Máxima calidad para evitar imagen borrosa en Gemini
            out
        )

        val imageBytes = out.toByteArray()
        android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    } catch (e: Exception) {
        Log.e(TAG, "Error convirtiendo ImageProxy a Bitmap: ${e.message}")
        null
    }
}

@Composable
private fun DetectionOverlayGemini(
    detections: List<Detection>,
    imageWidth: Int,
    imageHeight: Int,
    roiRect: ROIRectGemini,
    modifier: Modifier = Modifier
) {
    val imageAspect = imageWidth.toFloat() / imageHeight.toFloat()
    val geminiColor = Color(0xFF9C7BFF)

    Canvas(modifier = modifier) {
        val canvasAspect = size.width / size.height
        val scale: Float
        val offsetX: Float
        val offsetY: Float

        if (imageAspect > canvasAspect) {
            scale = size.width / imageWidth
            offsetX = 0f
            offsetY = (size.height - imageHeight * scale) / 2f
        } else {
            scale = size.height / imageHeight
            offsetX = (size.width - imageWidth * scale) / 2f
            offsetY = 0f
        }

        val roiLeft = roiRect.left * imageWidth * scale + offsetX
        val roiTop = roiRect.top * imageHeight * scale + offsetY
        val roiRight = roiRect.right * imageWidth * scale + offsetX
        val roiBottom = roiRect.bottom * imageHeight * scale + offsetY

        val dimColor = Color.Black.copy(alpha = 0.5f)

        drawRect(dimColor, Offset(offsetX, offsetY), Size(imageWidth * scale, roiTop - offsetY))
        drawRect(dimColor, Offset(offsetX, roiBottom), Size(imageWidth * scale, (offsetY + imageHeight * scale) - roiBottom))
        drawRect(dimColor, Offset(offsetX, roiTop), Size(roiLeft - offsetX, roiBottom - roiTop))
        drawRect(dimColor, Offset(roiRight, roiTop), Size((offsetX + imageWidth * scale) - roiRight, roiBottom - roiTop))

        drawRect(
            color = geminiColor,
            topLeft = Offset(roiLeft, roiTop),
            size = Size(roiRight - roiLeft, roiBottom - roiTop),
            style = Stroke(width = 3f)
        )

        // Bounding boxes de detecciones YOLO (para debug)
        for (detection in detections) {
            val color = Color.White.copy(alpha = 0.5f)
            val box = detection.boundingBox

            val left = box.left * scale + offsetX
            val top = box.top * scale + offsetY
            val right = box.right * scale + offsetX
            val bottom = box.bottom * scale + offsetY

            drawRect(
                color = color,
                topLeft = Offset(left, top),
                size = Size(right - left, bottom - top),
                style = Stroke(width = 2f)
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// COMPONENTES UI AUXILIARES
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun PermissionRequestGemini(
    onRequestPermission: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color(0xFF9C7BFF)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Permiso de Cámara Requerido",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Para clasificar residuos con Gemini AI, necesitamos acceso a la cámara.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C7BFF))
        ) {
            Text("Permitir Cámara")
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onNavigateBack) {
            Text("Volver", color = Color.White.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun LoadingScreenGemini() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = Color(0xFF9C7BFF),
                modifier = Modifier.size(56.dp),
                strokeWidth = 5.dp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Iniciando YOLO + Gemini AI...",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Cargando modelos de IA",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun ErrorScreenGemini(
    error: String,
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Error",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.Red
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onNavigateBack,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C7BFF))
        ) {
            Text("Volver")
        }
    }
}

@Composable
private fun DetectionResultsPanelGemini(
    detections: List<Detection>,
    classificationState: ClassificationState
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.Black.copy(alpha = 0.8f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "YOLO (Presencia)",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "• ${detections.size} detecciones",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = when (classificationState) {
                    ClassificationState.IDLE -> "Esperando material..."
                    ClassificationState.DETECTING_PRESENCE -> "Material detectado, estabilizando..."
                    ClassificationState.CLASSIFYING_GEMINI -> "Gemini clasificando..."
                    ClassificationState.CLASSIFICATION_READY -> "Clasificación lista"
                    ClassificationState.DEPOSITING -> "Depositando..."
                    else -> "Procesando..."
                },
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF9C7BFF).copy(alpha = 0.8f)
            )
        }
    }
}
