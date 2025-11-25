package com.biowaymexico.ui.screens.brindador

import android.Manifest
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as ComposeSize
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.biowaymexico.ui.theme.BioWayColors
import com.biowaymexico.ui.theme.BioWayGradients
import com.biowaymexico.utils.WasteClassifierYOLO
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Pantalla de clasificación de residuos usando YOLOv8
 * Detecta múltiples objetos en tiempo real con bounding boxes
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ClasificadorScreenYOLO(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    // Estados
    var capturedImage by remember { mutableStateOf<Bitmap?>(null) }
    var currentDetections by remember { mutableStateOf<List<WasteClassifierYOLO.Detection>>(emptyList()) }
    var isClassifying by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    var isAnalyzing by remember { mutableStateOf(true) }  // SIEMPRE activo para detección en vivo
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var initializationStatus by remember { mutableStateOf("Inicializando clasificador...") }
    var frameCount by remember { mutableStateOf(0) }  // Contador de frames procesados

    // Clasificador YOLOv8
    val classifier = remember { WasteClassifierYOLO(context) }
    var isClassifierReady by remember { mutableStateOf(false) }

    // CameraX
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var imageAnalyzer: ImageAnalysis? by remember { mutableStateOf(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var cameraProvider: ProcessCameraProvider? by remember { mutableStateOf(null) }

    // Permiso de cámara
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    // Inicializar clasificador
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                initializationStatus = "Cargando modelo YOLOv8..."
                classifier.initialize()
                isClassifierReady = true
                initializationStatus = "Clasificador listo"
            } catch (e: Exception) {
                errorMessage = "Error al inicializar: ${e.message}"
                initializationStatus = "Error al cargar modelo"
            }
        }
    }

    // Limpiar recursos al salir
    DisposableEffect(Unit) {
        onDispose {
            classifier.close()
            cameraExecutor.shutdown()
            cameraProvider?.unbindAll()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BioWayColors.BackgroundGrey)
    ) {
            when {
                !cameraPermissionState.status.isGranted -> {
                    PermissionRequiredScreen(
                        onRequestPermission = {
                            cameraPermissionState.launchPermissionRequest()
                        }
                    )
                }
                errorMessage != null -> {
                    ErrorScreen(
                        message = errorMessage!!,
                        onRetry = {
                            errorMessage = null
                            scope.launch {
                                try {
                                    classifier.initialize()
                                    isClassifierReady = true
                                } catch (e: Exception) {
                                    errorMessage = "Error: ${e.message}"
                                }
                            }
                        }
                    )
                }
                else -> {
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Vista de cámara con overlay para bounding boxes
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black)
                        ) {
                            if (capturedImage == null) {
                                // Vista de cámara en vivo CON ANÁLISIS CONTINUO
                                CameraPreviewWithAnalysis(
                                    onImageCaptureReady = { imageCapture = it },
                                    onAnalyzerReady = { imageAnalyzer = it },
                                    onCameraProviderReady = { cameraProvider = it },
                                    isAnalyzing = true,  // SIEMPRE ACTIVO en modo cámara
                                    classifier = classifier,
                                    onDetections = { detections ->
                                        currentDetections = detections
                                        frameCount++  // Incrementar contador de frames
                                    }
                                )

                                // Canvas SIEMPRE visible para dibujar bounding boxes
                                Canvas(
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    drawDetections(currentDetections)  // Dibujar aunque esté vacío
                                }


                            } else {
                                // Imagen capturada con detecciones
                                CapturedImageWithDetections(
                                    bitmap = capturedImage!!,
                                    detections = currentDetections
                                )
                            }

                            // Indicador de procesamiento
                            if (isClassifying) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = BioWayColors.PrimaryGreen
                                    )
                                }
                            }
                        }

                        // Panel de resultados en la parte superior
                        AnimatedVisibility(
                            visible = currentDetections.isNotEmpty(),
                            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                            modifier = Modifier.align(Alignment.TopCenter)
                        ) {
                            DetectionResultsCard(
                                detections = currentDetections,
                                isLiveMode = capturedImage == null
                            )
                        }

                        // Solo mostrar botón de reset si hay imagen capturada
                        if (capturedImage != null) {
                            Button(
                                onClick = {
                                    capturedImage = null
                                    currentDetections = emptyList()
                                },
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BioWayColors.TextGrey
                                )
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Nueva Foto")
                            }
                        }
                    }
                }
            }

        // Panel de información
        AnimatedVisibility(
            visible = showInfo,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            InfoPanelYOLO(onDismiss = { showInfo = false })
        }

        // Botón flotante para volver
        FloatingActionButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            containerColor = BioWayColors.PrimaryGreen,
            contentColor = Color.White
        ) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = "Volver"
            )
        }

        // Icono de información en la parte superior
        IconButton(
            onClick = { showInfo = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .background(
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = CircleShape
                )
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = "Información",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun CameraPreviewWithAnalysis(
    onImageCaptureReady: (ImageCapture) -> Unit,
    onAnalyzerReady: (ImageAnalysis) -> Unit,
    onCameraProviderReady: (ProcessCameraProvider) -> Unit,
    isAnalyzing: Boolean,
    classifier: WasteClassifierYOLO,
    onDetections: (List<WasteClassifierYOLO.Detection>) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                onCameraProviderReady(cameraProvider)

                // Preview
                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                // Image Capture
                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                onImageCaptureReady(imageCapture)

                // Image Analysis para detección en tiempo real CONTINUA
                val imageAnalyzer = ImageAnalysis.Builder()
                    .setTargetResolution(Size(320, 320))  // Tamaño optimizado para YOLO
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)  // Procesar solo el frame más reciente
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build()

                // Analizador que procesa cada frame de la cámara
                imageAnalyzer.setAnalyzer(
                    ContextCompat.getMainExecutor(ctx)
                ) { imageProxy ->
                    // SIEMPRE procesar si el clasificador está listo y el análisis está activo
                    if (isAnalyzing && classifier.isReady()) {
                        processImage(imageProxy, classifier, scope) { detections ->
                            onDetections(detections)  // Actualizar detecciones en tiempo real
                        }
                    } else {
                        imageProxy.close()
                    }
                }

                onAnalyzerReady(imageAnalyzer)

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture,
                        imageAnalyzer
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

// Variable para controlar el throttling
private var lastProcessTime = 0L
private const val PROCESS_INTERVAL_MS = 100L  // Procesar cada 100ms (10 FPS)

// Procesar imagen del análisis en tiempo real
private fun processImage(
    imageProxy: ImageProxy,
    classifier: WasteClassifierYOLO,
    scope: CoroutineScope,
    onDetections: (List<WasteClassifierYOLO.Detection>) -> Unit
) {
    val currentTime = System.currentTimeMillis()

    // Throttling: procesar solo si ha pasado suficiente tiempo
    if (currentTime - lastProcessTime < PROCESS_INTERVAL_MS) {
        imageProxy.close()
        return
    }

    lastProcessTime = currentTime

    scope.launch {
        try {
            // Convertir ImageProxy a Bitmap y aplicar rotación si es necesaria
            val bitmap = imageProxy.toBitmap()
            val rotation = imageProxy.imageInfo.rotationDegrees

            // Log para debug
            Log.d("YOLODebug", "Image rotation: $rotation degrees")
            Log.d("YOLODebug", "Image size: ${bitmap.width} x ${bitmap.height}")

            // Aplicar rotación para que coincida con la vista
            val rotatedBitmap = if (rotation != 0) {
                rotateBitmap(bitmap, rotation.toFloat())
            } else {
                bitmap
            }

            // Procesar en background thread
            val result = withContext(Dispatchers.Default) {
                classifier.classifyImage(rotatedBitmap)
            }

            // Actualizar UI con las detecciones
            withContext(Dispatchers.Main) {
                onDetections(result.detections)
            }
        } catch (e: Exception) {
            // Ignorar errores para mantener el flujo continuo
        } finally {
            imageProxy.close()
        }
    }
}

// Función para dibujar detecciones con coordenadas normalizadas
fun DrawScope.drawDetections(detections: List<WasteClassifierYOLO.Detection>) {
    // Colores simples pero distintivos
    val colors = listOf(
        Color(0xFF00E676), // Verde
        Color(0xFFFF4081), // Rosa
        Color(0xFF00BCD4), // Cyan
        Color(0xFFFFD740), // Amarillo
        Color(0xFF7C4DFF), // Morado
        Color(0xFFFF6E40)  // Naranja
    )

    // Factores de ajuste para mejorar la precisión de los bounding boxes
    // Estos valores se pueden ajustar según sea necesario (1.0 = sin ajuste)
    val verticalStretchFactor = 0.75f  // Comprimir más la altura (80%)
    val horizontalStretchFactor = 1.40f  // Anchura expandida (108%)

    detections.forEach { detection ->
        val color = colors[detection.classId % colors.size]

        // Debug: Log de coordenadas normalizadas
        Log.d("YOLODebug", "Detection: ${detection.className} - " +
            "Normalized coords: [${detection.boundingBox.left}, ${detection.boundingBox.top}, " +
            "${detection.boundingBox.right}, ${detection.boundingBox.bottom}]")

        // Aplicar factor de ajuste horizontal uniforme
        val centerX = (detection.boundingBox.left + detection.boundingBox.right) / 2f
        val normalizedWidth = (detection.boundingBox.right - detection.boundingBox.left) * horizontalStretchFactor

        val left = (centerX - normalizedWidth / 2f) * size.width
        val right = (centerX + normalizedWidth / 2f) * size.width

        // Aplicar factor de ajuste vertical uniforme
        val centerY = (detection.boundingBox.top + detection.boundingBox.bottom) / 2f
        val normalizedHeight = (detection.boundingBox.bottom - detection.boundingBox.top) * verticalStretchFactor

        val top = (centerY - normalizedHeight / 2f) * size.height
        val bottom = (centerY + normalizedHeight / 2f) * size.height

        // Debug: Log de coordenadas en píxeles
        Log.d("YOLODebug", "Canvas: ${size.width}x${size.height}")
        Log.d("YOLODebug", "Stretch factors - H: $horizontalStretchFactor, V: $verticalStretchFactor")
        Log.d("YOLODebug", "Final coords: [$left, $top, $right, $bottom]")

        // Dibujar rectángulo con borde visible
        drawRect(
            color = color,
            topLeft = Offset(left, top),
            size = ComposeSize(right - left, bottom - top),
            style = Stroke(width = 3.dp.toPx())
        )

        // Dibujar esquinas más prominentes para mejor visibilidad
        val cornerLength = minOf(right - left, bottom - top) * 0.15f
        val cornerStroke = 5.dp.toPx()

        // Esquina superior izquierda
        drawLine(color, Offset(left, top), Offset(left + cornerLength, top), cornerStroke)
        drawLine(color, Offset(left, top), Offset(left, top + cornerLength), cornerStroke)

        // Esquina superior derecha
        drawLine(color, Offset(right - cornerLength, top), Offset(right, top), cornerStroke)
        drawLine(color, Offset(right, top), Offset(right, top + cornerLength), cornerStroke)

        // Esquina inferior izquierda
        drawLine(color, Offset(left, bottom - cornerLength), Offset(left, bottom), cornerStroke)
        drawLine(color, Offset(left, bottom), Offset(left + cornerLength, bottom), cornerStroke)

        // Esquina inferior derecha
        drawLine(color, Offset(right - cornerLength, bottom), Offset(right, bottom), cornerStroke)
        drawLine(color, Offset(right, bottom - cornerLength), Offset(right, bottom), cornerStroke)

        // Etiqueta
        val label = "${detection.className} ${(detection.confidence * 100).toInt()}%"
        val textPaint = android.graphics.Paint().apply {
            textSize = 24f
            setColor(android.graphics.Color.WHITE)
            isAntiAlias = true
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setShadowLayer(3f, 1f, 1f, android.graphics.Color.BLACK)
        }

        val textBounds = android.graphics.Rect()
        textPaint.getTextBounds(label, 0, label.length, textBounds)

        // Posición de la etiqueta (arriba o abajo según el espacio disponible)
        val labelY = if (top > textBounds.height() + 10) {
            top - 5
        } else {
            bottom + textBounds.height() + 5
        }

        // Fondo de etiqueta con bordes redondeados
        drawRoundRect(
            color = color.copy(alpha = 0.95f),
            topLeft = Offset(left, labelY - textBounds.height() - 4),
            size = ComposeSize(textBounds.width() + 16f, textBounds.height() + 8f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
        )

        // Texto
        drawContext.canvas.nativeCanvas.drawText(
            label,
            left + 8,
            labelY,
            textPaint
        )
    }
}

@Composable
fun CapturedImageWithDetections(
    bitmap: Bitmap,
    detections: List<WasteClassifierYOLO.Detection>
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Imagen capturada
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Imagen capturada",
            modifier = Modifier.fillMaxSize()
        )

        // Canvas simple para bounding boxes
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawDetections(detections)
        }
    }
}

@Composable
fun DetectionResultsCard(
    detections: List<WasteClassifierYOLO.Detection>,
    isLiveMode: Boolean
) {
    if (detections.isNotEmpty()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(top = 48.dp), // Espacio para que no se solape con el icono de info
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.9f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Contador de objetos
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Visibility,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        "${detections.size} detectados",
                        color = Color.White,
                        fontSize = 13.sp
                    )
                }

                // Objeto principal (mayor confianza)
                detections.maxByOrNull { it.confidence }?.let { topDetection ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            topDetection.className.uppercase(),
                            color = Color(0xFF00E676),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${(topDetection.confidence * 100).toInt()}%",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}



@Composable
fun InfoPanelYOLO(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    "Clasificador YOLOv8",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = BioWayColors.DarkGreen
                )
                Spacer(modifier = Modifier.height(16.dp))

                val features = listOf(
                    "Modelo YOLOv8 optimizado (3MB)",
                    "Detecta 12 categorías de residuos",
                    "Detección múltiple con bounding boxes",
                    "Análisis en tiempo real",
                    "Precisión del 86%",
                    "GPU acceleration cuando disponible"
                )

                features.forEach { feature ->
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = BioWayColors.PrimaryGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            feature,
                            fontSize = 14.sp,
                            color = BioWayColors.TextDark,
                            lineHeight = 20.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BioWayColors.PrimaryGreen
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Entendido")
                }
            }
        }
    }
}

@Composable
fun ErrorScreen(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Warning,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color.Red
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Error",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = BioWayColors.DarkGreen
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            message,
            fontSize = 16.sp,
            color = BioWayColors.TextGrey,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = BioWayColors.PrimaryGreen
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Reintentar")
        }
    }
}

@Composable
private fun PermissionRequiredScreen(
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.CameraAlt,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = BioWayColors.PrimaryGreen
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Permiso de Cámara Necesario",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = BioWayColors.DarkGreen,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Para clasificar residuos con YOLOv8 necesitamos acceso a tu cámara",
            fontSize = 16.sp,
            color = BioWayColors.TextGrey,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(
                containerColor = BioWayColors.PrimaryGreen
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Conceder Permiso", fontSize = 16.sp)
        }
    }
}

// Funciones auxiliares
private fun getCategoryIcon(category: String): ImageVector {
    return when (category) {
        "Plástico" -> Icons.Default.Delete
        "Cartón", "Papel" -> Icons.Default.Article
        "Vidrio" -> Icons.Default.LocalDrink
        "Metal" -> Icons.Default.Build
        "Orgánico" -> Icons.Default.Eco
        "Basura" -> Icons.Default.Delete
        else -> Icons.Default.Help
    }
}

private fun getCategoryColor(category: String): Color {
    return when (category) {
        "Plástico" -> Color(0xFFFFC107)  // Amarillo
        "Cartón", "Papel" -> Color(0xFF2196F3)  // Azul
        "Vidrio" -> Color(0xFF4CAF50)  // Verde
        "Metal" -> Color(0xFF9E9E9E)  // Gris
        "Orgánico" -> Color(0xFF795548)  // Marrón
        "Basura" -> Color(0xFF424242)  // Gris oscuro
        else -> Color(0xFF607D8B)
    }
}

// Función para capturar imagen
private fun takePicture(
    imageCapture: ImageCapture,
    executor: java.util.concurrent.Executor,
    onImageCaptured: (Bitmap) -> Unit,
    onError: (ImageCaptureException) -> Unit
) {
    imageCapture.takePicture(
        executor,
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val bitmap = image.toBitmap()
                val rotatedBitmap = rotateBitmap(bitmap, image.imageInfo.rotationDegrees.toFloat())
                onImageCaptured(rotatedBitmap)
                image.close()
            }

            override fun onError(exception: ImageCaptureException) {
                onError(exception)
            }
        }
    )
}

// Función para rotar bitmap según orientación de la cámara
private fun rotateBitmap(bitmap: Bitmap, rotationDegrees: Float): Bitmap {
    val matrix = Matrix().apply {
        postRotate(rotationDegrees)
    }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

// Extensión para convertir ImageProxy a Bitmap
private fun ImageProxy.toBitmap(): Bitmap {
    val buffer = planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}