package com.biowaymexico.ui.screens.brindador

import android.Manifest
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.biowaymexico.ui.theme.BioWayColors
import com.biowaymexico.ui.theme.BioWayGradients
import com.biowaymexico.utils.ClasificadorResiduos
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Pantalla de clasificación de residuos usando IA
 * Permite al usuario capturar una foto de un residuo y obtener su clasificación
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClasificadorScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Estados
    var hasCameraPermission by remember { mutableStateOf(false) }
    var capturedImage by remember { mutableStateOf<Bitmap?>(null) }
    var clasificacion by remember { mutableStateOf<Pair<String, Float>?>(null) }
    var todasClasificaciones by remember { mutableStateOf<List<Triple<Int, String, Float>>>(emptyList()) }
    var isClassifying by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }

    // Clasificador
    val clasificador = remember { ClasificadorResiduos(context) }

    // CameraX
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // Launcher para pedir permisos
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    // Verificar permisos al inicio
    LaunchedEffect(Unit) {
        val permission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        )
        hasCameraPermission = permission == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Limpiar recursos al salir
    DisposableEffect(Unit) {
        onDispose {
            clasificador.cerrar()
            cameraExecutor.shutdown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Clasificador de Residuos",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showInfo = !showInfo }) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Información",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BioWayColors.DarkGreen
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BioWayColors.BackgroundGrey)
                .padding(paddingValues)
        ) {
            if (!hasCameraPermission) {
                // Solicitar permisos
                PermissionRequiredScreen(
                    onRequestPermission = {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Vista de cámara o imagen capturada
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(Color.Black)
                    ) {
                        if (capturedImage == null) {
                            // Vista de cámara
                            CameraPreview(
                                onImageCaptureReady = { imageCapture = it }
                            )
                        } else {
                            // Imagen capturada
                            Image(
                                bitmap = capturedImage!!.asImageBitmap(),
                                contentDescription = "Imagen capturada",
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Indicador de clasificación
                        if (isClassifying) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.7f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator(
                                        color = BioWayColors.PrimaryGreen,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        "Analizando...",
                                        color = Color.White,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                    }

                    // Resultados
                    AnimatedVisibility(
                        visible = clasificacion != null,
                        enter = slideInVertically() + fadeIn(),
                        exit = slideOutVertically() + fadeOut()
                    ) {
                        ResultsCard(
                            clasificacionPrincipal = clasificacion,
                            todasClasificaciones = todasClasificaciones
                        )
                    }

                    // Botones de acción
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (capturedImage != null) {
                            // Botón para nueva foto
                            Button(
                                onClick = {
                                    capturedImage = null
                                    clasificacion = null
                                    todasClasificaciones = emptyList()
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BioWayColors.TextGrey
                                )
                            ) {
                                Text("Nueva Foto")
                            }
                        } else {
                            // Botón para capturar
                            Button(
                                onClick = {
                                    imageCapture?.let { capture ->
                                        takePicture(
                                            imageCapture = capture,
                                            executor = cameraExecutor,
                                            onImageCaptured = { bitmap ->
                                                capturedImage = bitmap
                                                isClassifying = true

                                                // Clasificar en background
                                                cameraExecutor.execute {
                                                    val resultado = clasificador.clasificar(bitmap)
                                                    val todas = clasificador.clasificarTodas(bitmap)

                                                    // Actualizar UI en main thread
                                                    clasificacion = resultado
                                                    todasClasificaciones = todas
                                                    isClassifying = false
                                                }
                                            },
                                            onError = { exception ->
                                                exception.printStackTrace()
                                            }
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BioWayColors.PrimaryGreen
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Capturar Foto",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
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
                InfoPanel(onDismiss = { showInfo = false })
            }
        }
    }
}

@Composable
fun CameraPreview(
    onImageCaptureReady: (ImageCapture) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = androidx.camera.core.Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                onImageCaptureReady(imageCapture)

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
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

@Composable
fun ResultsCard(
    clasificacionPrincipal: Pair<String, Float>?,
    todasClasificaciones: List<Triple<Int, String, Float>>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            if (clasificacionPrincipal != null) {
                // Resultado principal
                Text(
                    "Resultado",
                    fontSize = 14.sp,
                    color = BioWayColors.TextGrey,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        clasificacionPrincipal.first,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = BioWayColors.DarkGreen
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        BioWayColors.PrimaryGreen,
                                        BioWayColors.Turquoise
                                    )
                                )
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            "${(clasificacionPrincipal.second * 100).toInt()}%",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = BioWayColors.LightGrey)
                Spacer(modifier = Modifier.height(16.dp))

                // Información del residuo
                ResidueInfo(categoria = clasificacionPrincipal.first)

                if (todasClasificaciones.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = BioWayColors.LightGrey)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Mostrar TODAS las categorías con sus índices para debug
                    Text(
                        "Todas las probabilidades (Debug)",
                        fontSize = 14.sp,
                        color = BioWayColors.TextGrey,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    todasClasificaciones.forEachIndexed { listIndex, (originalIndex, categoria, confianza) ->
                        ClassificationBarDebug(
                            index = originalIndex, // Índice original del modelo
                            categoria = categoria,
                            confianza = confianza,
                            isTop = listIndex == 0 // Primero en la lista ordenada
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ClassificationBar(
    categoria: String,
    confianza: Float
) {
    val animatedProgress by animateFloatAsState(
        targetValue = confianza,
        label = "progress"
    )

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                categoria,
                fontSize = 14.sp,
                color = BioWayColors.TextDark
            )
            Text(
                "${(confianza * 100).toInt()}%",
                fontSize = 14.sp,
                color = BioWayColors.TextGrey
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(BioWayColors.LightGrey)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(BioWayColors.PrimaryGreen)
            )
        }
    }
}

@Composable
fun ClassificationBarDebug(
    index: Int,
    categoria: String,
    confianza: Float,
    isTop: Boolean = false
) {
    val animatedProgress by animateFloatAsState(
        targetValue = confianza,
        label = "progress"
    )

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Índice en un badge
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            if (isTop) BioWayColors.PrimaryGreen else BioWayColors.LightGrey
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "$index",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isTop) Color.White else BioWayColors.TextGrey
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    categoria,
                    fontSize = 14.sp,
                    fontWeight = if (isTop) FontWeight.Bold else FontWeight.Normal,
                    color = if (isTop) BioWayColors.DarkGreen else BioWayColors.TextDark
                )
            }
            Text(
                "${(confianza * 100).toInt()}%",
                fontSize = 14.sp,
                fontWeight = if (isTop) FontWeight.Bold else FontWeight.Normal,
                color = if (isTop) BioWayColors.DarkGreen else BioWayColors.TextGrey
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(BioWayColors.LightGrey)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (isTop) BioWayColors.PrimaryGreen else BioWayColors.TextGrey
                    )
            )
        }
    }
}

@Composable
fun ResidueInfo(categoria: String) {
    val info = when (categoria) {
        "Vidrio" -> "El vidrio es 100% reciclable y puede reciclarse infinitas veces sin perder calidad."
        "Papel" -> "El papel puede reciclarse de 5 a 7 veces. Asegúrate de que esté limpio y seco."
        "Cartón" -> "El cartón es altamente reciclable. Aplánalo antes de reciclarlo para ahorrar espacio."
        "Plástico" -> "Los plásticos tienen diferentes tipos. Revisa el número en el símbolo de reciclaje."
        "Metal" -> "Los metales son valiosos y pueden reciclarse indefinidamente sin perder propiedades."
        "Orgánico" -> "Los residuos orgánicos pueden compostarse y convertirse en abono natural."
        "Basura" -> "Este residuo no es reciclable. Deposítalo en el contenedor de basura general."
        else -> "Residuo identificado. Deposítalo en el contenedor adecuado."
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BioWayColors.BackgroundGrey)
            .padding(16.dp)
    ) {
        Text(
            info,
            fontSize = 14.sp,
            color = BioWayColors.TextDark,
            lineHeight = 20.sp
        )
    }
}

@Composable
fun PermissionRequiredScreen(
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
            "Para clasificar residuos necesitamos acceso a tu cámara",
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

@Composable
fun InfoPanel(onDismiss: () -> Unit) {
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
                    "Cómo usar el clasificador",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = BioWayColors.DarkGreen
                )
                Spacer(modifier = Modifier.height(16.dp))

                val tips = listOf(
                    "Enfoca el residuo claramente en la cámara",
                    "Asegúrate de tener buena iluminación",
                    "Coloca el objeto sobre un fondo uniforme",
                    "Mantén la cámara estable al capturar",
                    "El modelo tiene 86% de precisión"
                )

                tips.forEach { tip ->
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .offset(y = 6.dp)
                                .clip(CircleShape)
                                .background(BioWayColors.PrimaryGreen)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            tip,
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
