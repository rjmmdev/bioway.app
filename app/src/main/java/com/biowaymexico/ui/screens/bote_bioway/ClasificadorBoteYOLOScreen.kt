package com.biowaymexico.ui.screens.bote_bioway

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.graphics.PathEffect
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
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private const val TAG = "ClasificadorBoteYOLO"
private const val TAG_FILTER = "PlateFilter"

/**
 * Filtro inteligente para ignorar el plato blanco del bote
 *
 * Estrategia principal basada en CONFIANZA:
 * - El plato tiene confianza BAJA (26-34%) porque es un falso positivo
 * - Los objetos reales tienen confianza ALTA (70%+)
 * - Cuando hay 2+ plásticos, descartar el de mayor área (el plato)
 * - Cuando hay 1 plástico con baja confianza, probablemente es el plato
 */
object BackgroundPlateFilter {

    // Umbral de confianza - detecciones por debajo son probablemente el plato
    var minConfidenceThreshold = 0.50f  // Objetos reales tienen >50% confianza

    // Umbral de área - solo aplica junto con baja confianza
    var suspiciousAreaThreshold = 0.35f  // Si área >35% Y confianza baja = plato

    /**
     * Filtra las detecciones, removiendo las que parecen ser el plato de fondo
     */
    fun filterDetections(
        detections: List<Detection>,
        roiWidth: Int,
        roiHeight: Int
    ): List<Detection> {
        val roiArea = roiWidth.toFloat() * roiHeight.toFloat()

        Log.d(TAG_FILTER, "═══════════════════════════════════════")
        Log.d(TAG_FILTER, "📊 Analizando ${detections.size} detecciones")
        Log.d(TAG_FILTER, "   ROI: ${roiWidth}x${roiHeight} = ${roiArea.toInt()} px²")

        if (detections.isEmpty()) {
            Log.d(TAG_FILTER, "   (sin detecciones)")
            Log.d(TAG_FILTER, "═══════════════════════════════════════")
            return emptyList()
        }

        // Separar por tipo de material
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

        // Procesar plásticos con la lógica especial
        val filteredPlastics = filterPlasticDetections(plasticDetections, roiArea, roiWidth, roiHeight)

        // Otros materiales pasan sin filtro (no son el plato)
        otherDetections.forEach { detection ->
            val box = detection.boundingBox
            val areaRatio = (box.width() * box.height()) / roiArea
            Log.d(TAG_FILTER, "─────────────────────────────────────")
            Log.d(TAG_FILTER, "🔍 ${detection.className} (${(detection.confidence * 100).toInt()}%)")
            Log.d(TAG_FILTER, "   📊 Área: ${(areaRatio * 100).toInt()}% del ROI")
            Log.d(TAG_FILTER, "   ✅ ACEPTADO: No es plástico")
        }

        val result = filteredPlastics + otherDetections

        Log.d(TAG_FILTER, "═══════════════════════════════════════")
        Log.d(TAG_FILTER, "✅ Resultado: ${result.size}/${detections.size} detecciones válidas")
        Log.d(TAG_FILTER, "═══════════════════════════════════════")

        return result
    }

    private fun filterPlasticDetections(
        plastics: List<Detection>,
        roiArea: Float,
        roiWidth: Int,
        roiHeight: Int
    ): List<Detection> {
        if (plastics.isEmpty()) return emptyList()

        // Calcular área y métricas de cada detección
        data class PlasticWithMetrics(
            val detection: Detection,
            val area: Float,
            val areaRatio: Float,
            val confidence: Float
        )

        val plasticsWithMetrics = plastics.map { detection ->
            val box = detection.boundingBox
            val area = box.width() * box.height()
            val areaRatio = area / roiArea
            PlasticWithMetrics(detection, area, areaRatio, detection.confidence)
        }.sortedByDescending { it.area }  // Ordenar por área (mayor primero)

        // Log de todas las detecciones de plástico
        plasticsWithMetrics.forEach { p ->
            Log.d(TAG_FILTER, "─────────────────────────────────────")
            Log.d(TAG_FILTER, "🔍 ${p.detection.className}")
            Log.d(TAG_FILTER, "   🎯 Confianza: ${(p.confidence * 100).toInt()}%")
            Log.d(TAG_FILTER, "   📐 Tamaño: ${p.detection.boundingBox.width().toInt()}x${p.detection.boundingBox.height().toInt()} px")
            Log.d(TAG_FILTER, "   📊 Área: ${(p.areaRatio * 100).toInt()}% del ROI")
        }

        return when {
            // CASO 1: Solo hay 1 plástico detectado
            plasticsWithMetrics.size == 1 -> {
                val single = plasticsWithMetrics[0]

                // El plato tiene baja confianza (26-34%) y área grande
                // Los objetos reales tienen alta confianza (70%+)
                val isLowConfidence = single.confidence < minConfidenceThreshold
                val isLargeArea = single.areaRatio > suspiciousAreaThreshold
                val isProbablyPlate = isLowConfidence && isLargeArea

                if (isProbablyPlate) {
                    Log.d(TAG_FILTER, "   🚫 FILTRADO: Confianza baja (${(single.confidence * 100).toInt()}%) + área grande (${(single.areaRatio * 100).toInt()}%)")
                    Log.d(TAG_FILTER, "   💡 Probablemente es el plato (falso positivo)")
                    emptyList()
                } else {
                    Log.d(TAG_FILTER, "   ✅ ACEPTADO: Confianza ${(single.confidence * 100).toInt()}% >= umbral ${(minConfidenceThreshold * 100).toInt()}%")
                    listOf(single.detection)
                }
            }

            // CASO 2: Hay 2+ plásticos - descartar el más grande (el plato)
            else -> {
                val largest = plasticsWithMetrics[0]
                val smaller = plasticsWithMetrics.drop(1)  // Todos excepto el más grande

                Log.d(TAG_FILTER, "   🚫 FILTRADO: '${largest.detection.className}' - Mayor área (${(largest.areaRatio * 100).toInt()}%)")
                Log.d(TAG_FILTER, "   💡 El plástico más grande es probablemente el plato")

                smaller.forEach { p ->
                    Log.d(TAG_FILTER, "   ✅ ACEPTADO: '${p.detection.className}' - Confianza ${(p.confidence * 100).toInt()}%, Área ${(p.areaRatio * 100).toInt()}%")
                }

                smaller.map { it.detection }
            }
        }
    }

    /**
     * Resetear el filtro (por compatibilidad)
     */
    fun reset() {
        Log.d(TAG_FILTER, "🔄 Filtro reseteado")
    }

    /**
     * Log del estado actual de la configuración
     */
    fun logConfiguration() {
        Log.d(TAG_FILTER, "═══════════════════════════════════════")
        Log.d(TAG_FILTER, "⚙️ CONFIGURACIÓN DEL FILTRO")
        Log.d(TAG_FILTER, "   Estrategia: Basada en CONFIANZA + ÁREA")
        Log.d(TAG_FILTER, "   Umbral confianza: ${(minConfidenceThreshold*100).toInt()}%")
        Log.d(TAG_FILTER, "   Umbral área sospechosa: ${(suspiciousAreaThreshold*100).toInt()}%")
        Log.d(TAG_FILTER, "   Plástico único: filtrar si confianza <${(minConfidenceThreshold*100).toInt()}% Y área >${(suspiciousAreaThreshold*100).toInt()}%")
        Log.d(TAG_FILTER, "   Múltiples plásticos: descartar el de mayor área")
        Log.d(TAG_FILTER, "═══════════════════════════════════════")
    }
}

/**
 * Categorías de materiales para el bote BioWay
 * Mapea las 12 clases YOLO a 4 categorías físicas del bote
 */
enum class MaterialCategory(
    val displayName: String,
    val emoji: String,
    val giro: Int,
    val inclinacion: Int,
    val color: Long  // Color en formato Long para usar con Color()
) {
    PLASTICO(
        displayName = "Plástico",
        emoji = "♻️",
        giro = -30,
        inclinacion = -45,
        color = 0xFF2196F3  // Azul
    ),
    PAPEL_CARTON(
        displayName = "Papel/Cartón",
        emoji = "📄",
        giro = -30,
        inclinacion = 45,
        color = 0xFF4CAF50  // Verde
    ),
    ALUMINIO_METAL(
        displayName = "Aluminio/Metal",
        emoji = "🥫",
        giro = 59,
        inclinacion = -45,
        color = 0xFF9C27B0  // Morado
    ),
    GENERAL(
        displayName = "General",
        emoji = "🗑️",
        giro = 59,
        inclinacion = 45,
        color = 0xFFFF9800  // Naranja
    );

    companion object {
        private const val TAG_CAT = "MaterialCategory"

        /**
         * Clasifica una detección YOLO en una de las 4 categorías
         */
        fun fromYoloClass(className: String): MaterialCategory {
            val lowerName = className.lowercase()

            return when {
                // PLÁSTICO: todos los tipos de plástico
                lowerName.contains("plastic") -> {
                    Log.d(TAG_CAT, "🔵 '$className' → PLÁSTICO")
                    PLASTICO
                }

                // PAPEL/CARTÓN
                lowerName == "paper" || lowerName == "cardboard" ||
                lowerName.contains("papel") || lowerName.contains("carton") -> {
                    Log.d(TAG_CAT, "🟢 '$className' → PAPEL/CARTÓN")
                    PAPEL_CARTON
                }

                // ALUMINIO/METAL (incluye vidrio para reciclables)
                lowerName == "metal" || lowerName == "glass" ||
                lowerName.contains("aluminio") || lowerName.contains("vidrio") -> {
                    Log.d(TAG_CAT, "🟣 '$className' → ALUMINIO/METAL")
                    ALUMINIO_METAL
                }

                // GENERAL: biological, trash, y todo lo demás
                else -> {
                    Log.d(TAG_CAT, "🟠 '$className' → GENERAL")
                    GENERAL
                }
            }
        }
    }
}

/**
 * Tracker de estabilidad de detección
 * Verifica que el mismo material se detecte consistentemente durante N segundos
 */
object DetectionStabilityTracker {
    private const val TAG_STAB = "StabilityTracker"
    private const val STABILITY_DURATION_MS = 3000L  // 3 segundos

    private var currentCategory: MaterialCategory? = null
    private var categoryStartTime: Long = 0L
    private var isStable = false

    /**
     * Actualiza el tracker con la detección actual
     * @return MaterialCategory si está estable por 3 segundos, null si no
     */
    fun update(detection: Detection?): MaterialCategory? {
        val now = System.currentTimeMillis()

        if (detection == null) {
            // Sin detección - resetear
            if (currentCategory != null) {
                Log.d(TAG_STAB, "❌ Detección perdida, reseteando...")
            }
            reset()
            return null
        }

        val newCategory = MaterialCategory.fromYoloClass(detection.className)

        if (newCategory != currentCategory) {
            // Cambió la categoría - reiniciar contador
            Log.d(TAG_STAB, "🔄 Cambio de categoría: ${currentCategory?.displayName ?: "ninguna"} → ${newCategory.displayName}")
            currentCategory = newCategory
            categoryStartTime = now
            isStable = false
            return null
        }

        // Misma categoría - verificar tiempo
        val elapsedTime = now - categoryStartTime
        val remainingTime = STABILITY_DURATION_MS - elapsedTime

        if (elapsedTime >= STABILITY_DURATION_MS && !isStable) {
            // ¡Estable por 3 segundos!
            isStable = true
            Log.d(TAG_STAB, "✅ ¡ESTABLE! ${newCategory.displayName} detectado por ${elapsedTime}ms")
            return newCategory
        }

        if (!isStable && remainingTime > 0) {
            Log.d(TAG_STAB, "⏳ ${newCategory.displayName}: ${remainingTime/1000.0}s restantes...")
        }

        return null
    }

    /**
     * Obtiene el progreso actual (0.0 a 1.0)
     */
    fun getProgress(): Float {
        if (currentCategory == null) return 0f
        val elapsed = System.currentTimeMillis() - categoryStartTime
        return (elapsed.toFloat() / STABILITY_DURATION_MS).coerceIn(0f, 1f)
    }

    /**
     * Obtiene la categoría actual siendo rastreada
     */
    fun getCurrentCategory(): MaterialCategory? = currentCategory

    /**
     * Verifica si ya se alcanzó estabilidad
     */
    fun isCurrentlyStable(): Boolean = isStable

    /**
     * Resetea el tracker (después de depositar o al perder detección)
     */
    fun reset() {
        currentCategory = null
        categoryStartTime = 0L
        isStable = false
    }

    /**
     * Resetea solo el estado de estabilidad (para permitir nuevo depósito)
     */
    fun resetStability() {
        isStable = false
        categoryStartTime = System.currentTimeMillis()
    }
}

/**
 * Representa la region de interes (ROI) para el recorte
 * Valores normalizados de 0 a 1
 * Siempre mantiene forma rectangular uniforme
 */
data class ROIRect(
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
 * Estados de la pantalla del clasificador YOLO
 */
private enum class YoloScreenState {
    PERMISSION_REQUEST,
    ROI_CONFIGURATION,
    DETECTION_RUNNING
}

/**
 * Pantalla de Clasificador IA v2 para Bote BioWay
 * Flujo por pasos:
 * 1. Configurar ROI
 * 2. Iniciar deteccion con ROI fijo
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ClasificadorBoteYOLOScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Estado de permisos
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    // Estado de la pantalla
    var screenState by remember {
        mutableStateOf(
            if (cameraPermissionState.status.isGranted)
                YoloScreenState.ROI_CONFIGURATION
            else
                YoloScreenState.PERMISSION_REQUEST
        )
    }

    // Estado del ROI
    var roiRect by remember { mutableStateOf(ROIRect()) }

    // Dimensiones de la imagen de camara
    var cameraImageSize by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    // Actualizar estado cuando cambie el permiso
    LaunchedEffect(cameraPermissionState.status.isGranted) {
        if (cameraPermissionState.status.isGranted && screenState == YoloScreenState.PERMISSION_REQUEST) {
            screenState = YoloScreenState.ROI_CONFIGURATION
        }
    }

    // Manejar boton de retroceso del sistema
    BackHandler {
        when (screenState) {
            YoloScreenState.DETECTION_RUNNING -> {
                // Desde deteccion, volver a configuracion de ROI
                screenState = YoloScreenState.ROI_CONFIGURATION
            }
            else -> {
                // Desde configuracion o permisos, salir de la pantalla
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
                YoloScreenState.PERMISSION_REQUEST -> {
                    PermissionRequest(
                        onRequestPermission = { cameraPermissionState.launchPermissionRequest() },
                        onNavigateBack = onNavigateBack
                    )
                }

                YoloScreenState.ROI_CONFIGURATION -> {
                    ROIConfigurationScreen(
                        lifecycleOwner = lifecycleOwner,
                        roiRect = roiRect,
                        onROIChange = { roiRect = it },
                        onCameraImageSize = { w, h -> cameraImageSize = Pair(w, h) },
                        cameraImageSize = cameraImageSize,
                        onNavigateBack = onNavigateBack,
                        onConfirmROI = {
                            screenState = YoloScreenState.DETECTION_RUNNING
                        }
                    )
                }

                YoloScreenState.DETECTION_RUNNING -> {
                    DetectionScreen(
                        lifecycleOwner = lifecycleOwner,
                        roiRect = roiRect,
                        cameraImageSize = cameraImageSize,
                        onNavigateBack = {
                            // Volver a configuracion de ROI
                            screenState = YoloScreenState.ROI_CONFIGURATION
                        },
                        onExit = onNavigateBack
                    )
                }
            }
        }
    }
}

// ==================== PANTALLA DE CONFIGURACION DE ROI ====================

@Composable
private fun ROIConfigurationScreen(
    lifecycleOwner: LifecycleOwner,
    roiRect: ROIRect,
    onROIChange: (ROIRect) -> Unit,
    onCameraImageSize: (Int, Int) -> Unit,
    cameraImageSize: Pair<Int, Int>?,
    onNavigateBack: () -> Unit,
    onConfirmROI: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Preview de camara (sin modelo)
        CameraPreviewOnly(
            lifecycleOwner = lifecycleOwner,
            onCameraImageSize = onCameraImageSize
        )

        // Overlay para configurar ROI
        cameraImageSize?.let { (imgWidth, imgHeight) ->
            ROIConfigurationOverlay(
                imageWidth = imgWidth,
                imageHeight = imgHeight,
                roiRect = roiRect,
                onROIChange = onROIChange,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Header
        Column(modifier = Modifier.fillMaxSize()) {
            // Barra superior
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
                        text = "Paso 1: Configurar Area",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Ajusta el area de deteccion",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }

                Icon(
                    imageVector = Icons.Default.Crop,
                    contentDescription = null,
                    tint = BioWayColors.BrandGreen,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Boton inferior
            Button(
                onClick = onConfirmROI,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BioWayColors.BrandGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Iniciar Clasificacion",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun CameraPreviewOnly(
    lifecycleOwner: LifecycleOwner,
    onCameraImageSize: (Int, Int) -> Unit
) {
    val context = LocalContext.current
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var hasReportedSize by remember { mutableStateOf(false) }

    // Limpiar camara al salir
    DisposableEffect(Unit) {
        onDispose {
            try {
                cameraProvider?.unbindAll()
                Log.d(TAG, "CameraPreviewOnly: Camara liberada")
            } catch (e: Exception) {
                Log.e(TAG, "Error liberando camara: ${e.message}")
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

                        // ImageAnalysis solo para obtener dimensiones
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
                        Log.e(TAG, "Error iniciando camara: ${e.message}", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun ROIConfigurationOverlay(
    imageWidth: Int,
    imageHeight: Int,
    roiRect: ROIRect,
    onROIChange: (ROIRect) -> Unit,
    modifier: Modifier = Modifier
) {
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var draggedHandle by remember { mutableStateOf<String?>(null) }
    var isDraggingCenter by remember { mutableStateOf(false) }

    // Mantener referencia actualizada al ROI actual
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

                        // Solo esquinas para mantener forma rectangular
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

                        val minSize = 0.10f // Minimo 10% del area
                        val roi = currentRoiRect

                        if (isDraggingCenter) {
                            // Mover todo el ROI manteniendo dimensiones
                            val newLeft = (roi.left + deltaX).coerceIn(0f, 1f - roi.width)
                            val newTop = (roi.top + deltaY).coerceIn(0f, 1f - roi.height)
                            currentOnROIChange(ROIRect(
                                left = newLeft,
                                top = newTop,
                                right = newLeft + roi.width,
                                bottom = newTop + roi.height
                            ))
                        } else {
                            // Redimensionar libremente desde esquinas
                            when (draggedHandle) {
                                "topLeft" -> {
                                    val newLeft = (roi.left + deltaX).coerceIn(0f, roi.right - minSize)
                                    val newTop = (roi.top + deltaY).coerceIn(0f, roi.bottom - minSize)
                                    currentOnROIChange(ROIRect(
                                        left = newLeft,
                                        top = newTop,
                                        right = roi.right,
                                        bottom = roi.bottom
                                    ))
                                }
                                "topRight" -> {
                                    val newRight = (roi.right + deltaX).coerceIn(roi.left + minSize, 1f)
                                    val newTop = (roi.top + deltaY).coerceIn(0f, roi.bottom - minSize)
                                    currentOnROIChange(ROIRect(
                                        left = roi.left,
                                        top = newTop,
                                        right = newRight,
                                        bottom = roi.bottom
                                    ))
                                }
                                "bottomLeft" -> {
                                    val newLeft = (roi.left + deltaX).coerceIn(0f, roi.right - minSize)
                                    val newBottom = (roi.bottom + deltaY).coerceIn(roi.top + minSize, 1f)
                                    currentOnROIChange(ROIRect(
                                        left = newLeft,
                                        top = roi.top,
                                        right = roi.right,
                                        bottom = newBottom
                                    ))
                                }
                                "bottomRight" -> {
                                    val newRight = (roi.right + deltaX).coerceIn(roi.left + minSize, 1f)
                                    val newBottom = (roi.bottom + deltaY).coerceIn(roi.top + minSize, 1f)
                                    currentOnROIChange(ROIRect(
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

        // Area oscurecida fuera del ROI
        val dimColor = Color.Black.copy(alpha = 0.65f)

        // Arriba
        drawRect(
            color = dimColor,
            topLeft = Offset(offsetX, offsetY),
            size = Size(imageWidth * scale, roiTop - offsetY)
        )
        // Abajo
        drawRect(
            color = dimColor,
            topLeft = Offset(offsetX, roiBottom),
            size = Size(imageWidth * scale, (offsetY + imageHeight * scale) - roiBottom)
        )
        // Izquierda
        drawRect(
            color = dimColor,
            topLeft = Offset(offsetX, roiTop),
            size = Size(roiLeft - offsetX, roiBottom - roiTop)
        )
        // Derecha
        drawRect(
            color = dimColor,
            topLeft = Offset(roiRight, roiTop),
            size = Size((offsetX + imageWidth * scale) - roiRight, roiBottom - roiTop)
        )

        // Borde del ROI
        drawRect(
            color = BioWayColors.BrandGreen,
            topLeft = Offset(roiLeft, roiTop),
            size = Size(roiRight - roiLeft, roiBottom - roiTop),
            style = Stroke(width = 4f)
        )

        // Lineas guia (tercios)
        val thirdWidth = (roiRight - roiLeft) / 3
        val thirdHeight = (roiBottom - roiTop) / 3
        val guideColor = BioWayColors.BrandGreen.copy(alpha = 0.3f)

        // Lineas verticales
        drawLine(guideColor, Offset(roiLeft + thirdWidth, roiTop), Offset(roiLeft + thirdWidth, roiBottom), strokeWidth = 1f)
        drawLine(guideColor, Offset(roiLeft + thirdWidth * 2, roiTop), Offset(roiLeft + thirdWidth * 2, roiBottom), strokeWidth = 1f)
        // Lineas horizontales
        drawLine(guideColor, Offset(roiLeft, roiTop + thirdHeight), Offset(roiRight, roiTop + thirdHeight), strokeWidth = 1f)
        drawLine(guideColor, Offset(roiLeft, roiTop + thirdHeight * 2), Offset(roiRight, roiTop + thirdHeight * 2), strokeWidth = 1f)

        // Handles en las esquinas
        val handleRadius = handleSizePx / 2
        val corners = listOf(
            Offset(roiLeft, roiTop),
            Offset(roiRight, roiTop),
            Offset(roiLeft, roiBottom),
            Offset(roiRight, roiBottom)
        )

        corners.forEach { pos ->
            // Circulo exterior
            drawCircle(
                color = BioWayColors.BrandGreen,
                radius = handleRadius,
                center = pos
            )
            // Circulo interior
            drawCircle(
                color = Color.White,
                radius = handleRadius - 6f,
                center = pos
            )
        }
    }
}

// ==================== PANTALLA DE DETECCION ====================

@Composable
private fun DetectionScreen(
    lifecycleOwner: LifecycleOwner,
    roiRect: ROIRect,
    cameraImageSize: Pair<Int, Int>?,
    onNavigateBack: () -> Unit,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Estado del detector
    var detector by remember { mutableStateOf<WasteDetector?>(null) }
    var isDetectorReady by remember { mutableStateOf(false) }
    var detectorError by remember { mutableStateOf<String?>(null) }

    // Estado de detecciones
    var currentResult by remember { mutableStateOf<DetectionResult?>(null) }

    // ═══════════════════════════════════════════════════════════════
    // ESTADO BLUETOOTH / ESP32
    // ═══════════════════════════════════════════════════════════════
    val bluetoothManager = remember { BluetoothManager() }
    var bluetoothConectado by remember { mutableStateOf(false) }
    var estadoConexion by remember { mutableStateOf("Desconectado") }
    var conectando by remember { mutableStateOf(false) }
    var triggerConexion by remember { mutableStateOf(0) }  // Trigger para iniciar conexion

    // Manejar conexion Bluetooth cuando se dispara el trigger
    LaunchedEffect(triggerConexion) {
        if (triggerConexion == 0) return@LaunchedEffect
        if (conectando) return@LaunchedEffect

        conectando = true
        estadoConexion = "Conectando..."

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

    // ═══════════════════════════════════════════════════════════════
    // ESTADO DE ESTABILIDAD Y DEPOSITO
    // ═══════════════════════════════════════════════════════════════
    var stabilityProgress by remember { mutableStateOf(0f) }
    var currentCategory by remember { mutableStateOf<MaterialCategory?>(null) }
    var isDepositing by remember { mutableStateOf(false) }
    var depositStatus by remember { mutableStateOf("") }
    var lastDepositCategory by remember { mutableStateOf<MaterialCategory?>(null) }

    // Inicializar detector
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Inicializando WasteDetector...")

                val labels = WasteDetector.loadLabels(
                    context,
                    "labels/waste_detector_labels.txt"
                )
                Log.d(TAG, "Etiquetas cargadas: ${labels.size}")

                val newDetector = WasteDetector(
                    context = context,
                    modelPath = "models/waste_detector_v2.tflite",
                    labels = labels,
                    useGpu = true
                )

                newDetector.confidenceThreshold = 0.25f
                newDetector.iouThreshold = 0.4f
                newDetector.numItemsThreshold = 30

                detector = newDetector
                isDetectorReady = true
                Log.d(TAG, "WasteDetector inicializado correctamente")

                // Resetear y configurar filtro del plato de fondo
                BackgroundPlateFilter.reset()
                BackgroundPlateFilter.logConfiguration()

                // Resetear tracker de estabilidad
                DetectionStabilityTracker.reset()

            } catch (e: Exception) {
                Log.e(TAG, "Error inicializando detector: ${e.message}", e)
                detectorError = e.message
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // LOGICA DE ESTABILIDAD Y AUTO-DEPOSITO
    // ═══════════════════════════════════════════════════════════════
    var stableCategoryToDeposit by remember { mutableStateOf<MaterialCategory?>(null) }

    // Actualizar tracker de estabilidad con cada resultado
    LaunchedEffect(currentResult) {
        // Solo procesar si no estamos depositando
        if (isDepositing) return@LaunchedEffect

        val detections = currentResult?.boxes ?: emptyList()
        val primaryDetection = detections.maxByOrNull { it.confidence }

        // Actualizar tracker de estabilidad
        val stableCategory = DetectionStabilityTracker.update(primaryDetection)

        // Actualizar UI
        stabilityProgress = DetectionStabilityTracker.getProgress()
        currentCategory = DetectionStabilityTracker.getCurrentCategory()

        // Si alcanzamos estabilidad y estamos conectados al ESP32, marcar para depositar
        if (stableCategory != null && bluetoothConectado && !isDepositing) {
            stableCategoryToDeposit = stableCategory
        }
    }

    // Ejecutar deposito cuando se detecta material estable
    LaunchedEffect(stableCategoryToDeposit) {
        val categoryToDeposit = stableCategoryToDeposit ?: return@LaunchedEffect
        if (isDepositing) return@LaunchedEffect

        Log.d(TAG, "═══════════════════════════════════════")
        Log.d(TAG, "🎯 MATERIAL ESTABLE: ${categoryToDeposit.displayName}")
        Log.d(TAG, "   Iniciando depósito automático...")
        Log.d(TAG, "═══════════════════════════════════════")

        isDepositing = true
        depositStatus = "Depositando ${categoryToDeposit.displayName}..."
        stableCategoryToDeposit = null  // Resetear trigger

        val result = bluetoothManager.enviarMaterial(categoryToDeposit.displayName)
        result.fold(
            onSuccess = {
                depositStatus = "✓ ${categoryToDeposit.displayName} depositado"
                lastDepositCategory = categoryToDeposit
                Log.d(TAG, "✅ Depósito completado: ${categoryToDeposit.displayName}")
            },
            onFailure = { error ->
                depositStatus = "Error: ${error.message}"
                Log.e(TAG, "❌ Error en depósito: ${error.message}")
            }
        )

        // Esperar un momento para mostrar el resultado
        kotlinx.coroutines.delay(2000)

        // Resetear para nueva detección
        isDepositing = false
        depositStatus = ""
        DetectionStabilityTracker.reset()
    }

    // Limpiar al salir
    DisposableEffect(Unit) {
        onDispose {
            detector?.close()
            detector = null
            bluetoothManager.desconectar()
            DetectionStabilityTracker.reset()
            Log.d(TAG, "WasteDetector y Bluetooth cerrados")
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            detectorError != null -> {
                ErrorScreen(
                    error = detectorError!!,
                    onNavigateBack = onNavigateBack
                )
            }
            !isDetectorReady -> {
                LoadingScreen()
            }
            else -> {
                // Camara con deteccion y ROI fijo
                CameraPreviewWithDetection(
                    lifecycleOwner = lifecycleOwner,
                    detector = detector!!,
                    roiRect = roiRect,
                    onDetectionResult = { result ->
                        currentResult = result
                    }
                )

                // Overlay con ROI fijo y detecciones
                cameraImageSize?.let { (imgWidth, imgHeight) ->
                    DetectionOverlay(
                        detections = currentResult?.boxes ?: emptyList(),
                        imageWidth = imgWidth,
                        imageHeight = imgHeight,
                        roiRect = roiRect,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // UI superpuesta
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header con conexion ESP32
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
                                text = "Clasificador IA v2",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (bluetoothConectado) "ESP32 conectado" else "ESP32 desconectado",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (bluetoothConectado) BioWayColors.BrandGreen else Color.Red.copy(alpha = 0.8f)
                            )
                        }

                        // Boton de conexion ESP32 (minimalista)
                        Surface(
                            onClick = {
                                if (!bluetoothConectado && !conectando) {
                                    // Incrementar trigger para iniciar conexion
                                    triggerConexion++
                                } else if (bluetoothConectado) {
                                    bluetoothManager.desconectar()
                                    bluetoothConectado = false
                                    estadoConexion = "Desconectado"
                                }
                            },
                            shape = RoundedCornerShape(20.dp),
                            color = if (bluetoothConectado)
                                BioWayColors.BrandGreen.copy(alpha = 0.2f)
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
                                        tint = if (bluetoothConectado) BioWayColors.BrandGreen else Color.Red,
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

                    // ═══════════════════════════════════════════════════════════════
                    // PANEL DE ESTABILIDAD Y DEPOSITO
                    // ═══════════════════════════════════════════════════════════════
                    if (currentCategory != null || isDepositing || depositStatus.isNotEmpty()) {
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
                                if (isDepositing) {
                                    // Estado: Depositando
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = BioWayColors.BrandGreen,
                                            strokeWidth = 3.dp
                                        )
                                        Text(
                                            text = depositStatus,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = BioWayColors.BrandGreen,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                } else if (depositStatus.startsWith("✓")) {
                                    // Estado: Deposito completado
                                    Text(
                                        text = depositStatus,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = BioWayColors.BrandGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else if (currentCategory != null) {
                                    // Estado: Detectando / Esperando estabilidad
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            text = currentCategory!!.emoji,
                                            fontSize = 28.sp
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = currentCategory!!.displayName,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = Color(currentCategory!!.color),
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = if (bluetoothConectado)
                                                    "Mantenlo ${String.format("%.1f", (1f - stabilityProgress) * 3)}s para depositar"
                                                else
                                                    "Conecta ESP32 para depositar",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.White.copy(alpha = 0.7f)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Barra de progreso de estabilidad
                                    LinearProgressIndicator(
                                        progress = { stabilityProgress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = Color(currentCategory!!.color),
                                        trackColor = Color.White.copy(alpha = 0.2f)
                                    )
                                }
                            }
                        }
                    }

                    // Panel de resultados (detecciones)
                    DetectionResultsPanel(
                        detections = currentResult?.boxes ?: emptyList()
                    )
                }

                // Indicador de FPS y tiempo de inferencia (esquina superior derecha)
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
                                color = BioWayColors.BrandGreen
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
private fun CameraPreviewWithDetection(
    lifecycleOwner: LifecycleOwner,
    detector: WasteDetector,
    roiRect: ROIRect,
    onDetectionResult: (DetectionResult) -> Unit
) {
    val context = LocalContext.current
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val isProcessing = remember { AtomicBoolean(false) }

    // Limpiar camara y executor al salir
    DisposableEffect(Unit) {
        onDispose {
            try {
                cameraProvider?.unbindAll()
                executor.shutdown()
                Log.d(TAG, "CameraPreviewWithDetection: Camara y executor liberados")
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
                                        processImageProxyWithROI(
                                            imageProxy = imageProxy,
                                            detector = detector,
                                            roiRect = roiRect,
                                            onResult = { result ->
                                                onDetectionResult(result)
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
                        Log.d(TAG, "Camara con deteccion iniciada")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error iniciando camara: ${e.message}", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

private fun processImageProxyWithROI(
    imageProxy: ImageProxy,
    detector: WasteDetector,
    roiRect: ROIRect,
    onResult: (DetectionResult) -> Unit,
    onError: () -> Unit
) {
    try {
        val bitmap = imageProxyToBitmap(imageProxy)
        if (bitmap != null) {
            val rotatedBitmap = rotateBitmap(bitmap, imageProxy.imageInfo.rotationDegrees)

            // Recortar al ROI
            val croppedBitmap = cropBitmapToROI(rotatedBitmap, roiRect)
            val result = detector.detect(croppedBitmap)

            // ══════════════════════════════════════════════════════════
            // FILTRAR DETECCIONES DEL PLATO DE FONDO
            // ══════════════════════════════════════════════════════════
            val filteredBoxes = BackgroundPlateFilter.filterDetections(
                detections = result.boxes,
                roiWidth = croppedBitmap.width,
                roiHeight = croppedBitmap.height
            )

            val filteredResult = DetectionResult(
                boxes = filteredBoxes,
                inferenceTimeMs = result.inferenceTimeMs,
                fps = result.fps,
                imageWidth = result.imageWidth,
                imageHeight = result.imageHeight
            )
            // ══════════════════════════════════════════════════════════

            // Ajustar coordenadas al espacio original
            val adjustedResult = adjustDetectionsToFullImage(
                filteredResult,
                roiRect,
                rotatedBitmap.width,
                rotatedBitmap.height
            )
            onResult(adjustedResult)
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

private fun cropBitmapToROI(bitmap: Bitmap, roi: ROIRect): Bitmap {
    val x = (roi.left * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
    val y = (roi.top * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
    val width = (roi.width * bitmap.width).toInt().coerceIn(1, bitmap.width - x)
    val height = (roi.height * bitmap.height).toInt().coerceIn(1, bitmap.height - y)

    return Bitmap.createBitmap(bitmap, x, y, width, height)
}

private fun adjustDetectionsToFullImage(
    result: DetectionResult,
    roi: ROIRect,
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

private fun rotateBitmap(bitmap: Bitmap, rotationDegrees: Int): Bitmap {
    if (rotationDegrees == 0) return bitmap
    val matrix = android.graphics.Matrix()
    matrix.postRotate(rotationDegrees.toFloat())
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
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
            90,
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
private fun DetectionOverlay(
    detections: List<Detection>,
    imageWidth: Int,
    imageHeight: Int,
    roiRect: ROIRect,
    modifier: Modifier = Modifier
) {
    val imageAspect = imageWidth.toFloat() / imageHeight.toFloat()

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

        // Area oscurecida fuera del ROI
        val dimColor = Color.Black.copy(alpha = 0.5f)

        drawRect(dimColor, Offset(offsetX, offsetY), Size(imageWidth * scale, roiTop - offsetY))
        drawRect(dimColor, Offset(offsetX, roiBottom), Size(imageWidth * scale, (offsetY + imageHeight * scale) - roiBottom))
        drawRect(dimColor, Offset(offsetX, roiTop), Size(roiLeft - offsetX, roiBottom - roiTop))
        drawRect(dimColor, Offset(roiRight, roiTop), Size((offsetX + imageWidth * scale) - roiRight, roiBottom - roiTop))

        // Borde del ROI (fijo, no editable)
        drawRect(
            color = BioWayColors.BrandGreen,
            topLeft = Offset(roiLeft, roiTop),
            size = Size(roiRight - roiLeft, roiBottom - roiTop),
            style = Stroke(width = 3f)
        )

        // Bounding boxes de detecciones
        for (detection in detections) {
            val color = getColorForClass(detection.classIndex)
            val box = detection.boundingBox

            val left = box.left * scale + offsetX
            val top = box.top * scale + offsetY
            val right = box.right * scale + offsetX
            val bottom = box.bottom * scale + offsetY

            drawRect(
                color = color,
                topLeft = Offset(left, top),
                size = Size(right - left, bottom - top),
                style = Stroke(width = 4f)
            )

            val labelHeight = 36f
            drawRect(
                color = color,
                topLeft = Offset(left, top - labelHeight),
                size = Size(right - left, labelHeight)
            )
        }
    }
}

// ==================== COMPONENTES COMUNES ====================

private fun getColorForClass(classIndex: Int): Color {
    val colors = listOf(
        Color(0xFFFF0000),
        Color(0xFF8B4513),
        Color(0xFF00BFFF),
        Color(0xFFC0C0C0),
        Color(0xFFFFFF00),
        Color(0xFF00FF00),
        Color(0xFF32CD32),
        Color(0xFF008000),
        Color(0xFF228B22),
        Color(0xFF90EE90),
        Color(0xFF006400),
        Color(0xFF808080)
    )
    return colors.getOrElse(classIndex % colors.size) { Color.White }
}

@Composable
private fun PermissionRequest(
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
            tint = BioWayColors.BrandGreen
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Permiso de Camara Requerido",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Para clasificar residuos, necesitamos acceso a la camara.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(containerColor = BioWayColors.BrandGreen)
        ) {
            Text("Permitir Camara")
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onNavigateBack) {
            Text("Volver", color = Color.White.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = BioWayColors.BrandGreen,
                modifier = Modifier.size(56.dp),
                strokeWidth = 5.dp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Cargando modelo de IA...",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Esto puede tardar unos segundos",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun ErrorScreen(
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
            colors = ButtonDefaults.buttonColors(containerColor = BioWayColors.BrandGreen)
        ) {
            Text("Volver")
        }
    }
}

@Composable
private fun DetectionResultsPanel(detections: List<Detection>) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.Black.copy(alpha = 0.8f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Detecciones: ${detections.size}",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (detections.isEmpty()) {
                Text(
                    text = "Apunta la camara hacia un residuo...",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
            } else {
                detections.take(5).forEach { detection ->
                    DetectionItem(detection = detection)
                }
                if (detections.size > 5) {
                    Text(
                        text = "+ ${detections.size - 5} mas...",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DetectionItem(detection: Detection) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(
                    getColorForClass(detection.classIndex),
                    RoundedCornerShape(2.dp)
                )
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = translateClassName(detection.className),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = String.format("%.0f%%", detection.confidence * 100),
            style = MaterialTheme.typography.bodyMedium,
            color = BioWayColors.BrandGreen,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun translateClassName(className: String): String {
    return when (className.lowercase()) {
        "biological" -> "Organico"
        "cardboard" -> "Carton"
        "glass" -> "Vidrio"
        "metal" -> "Metal"
        "paper" -> "Papel"
        "plastic" -> "Plastico"
        "plastic-others" -> "Plastico (Otros)"
        "plastic-pet" -> "Plastico PET"
        "plastic-pe_hd" -> "Plastico PE-HD"
        "plastic-pp" -> "Plastico PP"
        "plastic-ps" -> "Plastico PS"
        "trash" -> "Basura"
        else -> className
    }
}
