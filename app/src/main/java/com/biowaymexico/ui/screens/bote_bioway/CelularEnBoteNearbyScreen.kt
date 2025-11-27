package com.biowaymexico.ui.screens.bote_bioway

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import java.nio.charset.Charset
import kotlin.random.Random

/**
 * Pantalla de Celular en Bote con Google Nearby Connections - Modo Discovery
 * Descubre y se conecta a dispositivos cercanos (rango 1-10m)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CelularEnBoteNearbyScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Estado
    var isDiscovering by remember { mutableStateOf(false) }
    var detectedUserId by remember { mutableStateOf<String?>(null) }
    var showSuccessAnimation by remember { mutableStateOf(false) }
    var detectionCount by remember { mutableStateOf(0) }
    var lastDetectionTime by remember { mutableStateOf("") }
    var discoveredEndpoints by remember { mutableStateOf<Set<String>>(emptySet()) }
    var connectedEndpoints by remember { mutableStateOf<Set<String>>(emptySet()) }
    var hasPermissions by remember { mutableStateOf(false) }

    // Nearby Connections client
    val connectionsClient = remember { Nearby.getConnectionsClient(context) }

    // Service ID (debe coincidir con el advertiser)
    val SERVICE_ID = "com.biowaymexico.nearby"

    // Verificar permisos
    fun checkPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    // Launcher para permisos
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermissions = permissions.all { it.value }
        if (hasPermissions) {
            startDiscovery(context, connectionsClient, SERVICE_ID,
                onEndpointFound = { endpointId, info ->
                    discoveredEndpoints = discoveredEndpoints + endpointId
                    Log.d("CelularEnBoteNearby", "🔍 Dispositivo encontrado: ${info.endpointName}")

                    // Conectar automáticamente
                    connectionsClient.requestConnection(
                        "CelularEnBote",
                        endpointId,
                        object : ConnectionLifecycleCallback() {
                            override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
                                Log.d("CelularEnBoteNearby", "🔵 Conexión iniciada con: ${connectionInfo.endpointName}")
                                // Aceptar automáticamente
                                connectionsClient.acceptConnection(endpointId, object : PayloadCallback() {
                                    override fun onPayloadReceived(endpointId: String, payload: Payload) {
                                        val receivedUserId = String(payload.asBytes()!!, Charset.forName("UTF-8"))
                                        Log.d("CelularEnBoteNearby", "📥 User ID recibido: $receivedUserId")

                                        detectedUserId = receivedUserId
                                        showSuccessAnimation = true
                                        detectionCount++
                                        lastDetectionTime = java.text.SimpleDateFormat(
                                            "HH:mm:ss",
                                            java.util.Locale.getDefault()
                                        ).format(java.util.Date())

                                        // Vibrar
                                        val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                                        vibrator?.vibrate(200)
                                    }

                                    override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
                                        Log.d("CelularEnBoteNearby", "📊 Transfer: ${update.status}")
                                    }
                                })
                            }

                            override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
                                if (result.status.isSuccess) {
                                    connectedEndpoints = connectedEndpoints + endpointId
                                    Log.d("CelularEnBoteNearby", "✅ Conectado con: $endpointId")
                                }
                            }

                            override fun onDisconnected(endpointId: String) {
                                connectedEndpoints = connectedEndpoints - endpointId
                                Log.d("CelularEnBoteNearby", "🔴 Desconectado de: $endpointId")
                            }
                        }
                    )
                },
                onEndpointLost = { endpointId ->
                    discoveredEndpoints = discoveredEndpoints - endpointId
                    Log.d("CelularEnBoteNearby", "📴 Dispositivo perdido: $endpointId")
                },
                onDiscoveryStarted = {
                    isDiscovering = true
                }
            )
        }
    }

    // Verificar permisos al inicio
    LaunchedEffect(Unit) {
        hasPermissions = checkPermissions()
        if (!hasPermissions) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT
                    )
                )
            } else {
                hasPermissions = true
            }
        } else {
            startDiscovery(context, connectionsClient, SERVICE_ID,
                onEndpointFound = { endpointId, info ->
                    discoveredEndpoints = discoveredEndpoints + endpointId
                    Log.d("CelularEnBoteNearby", "🔍 Dispositivo encontrado: ${info.endpointName}")

                    // Conectar automáticamente
                    connectionsClient.requestConnection(
                        "CelularEnBote",
                        endpointId,
                        object : ConnectionLifecycleCallback() {
                            override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
                                Log.d("CelularEnBoteNearby", "🔵 Conexión iniciada")
                                connectionsClient.acceptConnection(endpointId, object : PayloadCallback() {
                                    override fun onPayloadReceived(endpointId: String, payload: Payload) {
                                        val receivedUserId = String(payload.asBytes()!!, Charset.forName("UTF-8"))
                                        detectedUserId = receivedUserId
                                        showSuccessAnimation = true
                                        detectionCount++
                                        lastDetectionTime = java.text.SimpleDateFormat(
                                            "HH:mm:ss",
                                            java.util.Locale.getDefault()
                                        ).format(java.util.Date())

                                        val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                                        vibrator?.vibrate(200)
                                    }

                                    override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {}
                                })
                            }

                            override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
                                if (result.status.isSuccess) {
                                    connectedEndpoints = connectedEndpoints + endpointId
                                }
                            }

                            override fun onDisconnected(endpointId: String) {
                                connectedEndpoints = connectedEndpoints - endpointId
                            }
                        }
                    )
                },
                onEndpointLost = { endpointId ->
                    discoveredEndpoints = discoveredEndpoints - endpointId
                },
                onDiscoveryStarted = {
                    isDiscovering = true
                }
            )
        }
    }

    // Lifecycle observer
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    Log.d("CelularEnBoteNearby", "⏸️ Deteniendo discovery...")
                    connectionsClient.stopDiscovery()
                    connectionsClient.stopAllEndpoints()
                    isDiscovering = false
                }
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            connectionsClient.stopDiscovery()
            connectionsClient.stopAllEndpoints()
        }
    }

    // Auto-cerrar animación de éxito
    LaunchedEffect(showSuccessAnimation) {
        if (showSuccessAnimation) {
            kotlinx.coroutines.delay(3000)
            showSuccessAnimation = false
        }
    }

    // Animación
    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Celular en Bote - Nearby",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFF6F00)
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFF8F9FA),
                            Color(0xFFFFF3E0)
                        )
                    )
                )
                .padding(paddingValues)
        ) {
            if (!hasPermissions) {
                PermissionsRequiredScreen(
                    onRequestPermissions = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.BLUETOOTH_SCAN,
                                    Manifest.permission.BLUETOOTH_CONNECT
                                )
                            )
                        }
                    }
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Animación de búsqueda
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(200.dp)
                    ) {
                        repeat(3) { index ->
                            Box(
                                modifier = Modifier
                                    .size(200.dp - (index * 40).dp)
                                    .scale(pulseScale - (index * 0.05f))
                                    .clip(CircleShape)
                                    .background(
                                        Color(0xFFFF6F00).copy(alpha = 0.3f - (index * 0.08f))
                                    )
                            )
                        }

                        Surface(
                            modifier = Modifier.size(100.dp),
                            shape = CircleShape,
                            color = Color(0xFFFF6F00),
                            shadowElevation = 8.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Radar,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(60.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    Text(
                        text = if (isDiscovering) "Buscando..." else "Iniciando...",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Rango de búsqueda: 1-10 metros",
                        fontSize = 16.sp,
                        color = Color(0xFF7F8C8D),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Estadísticas
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF27AE60),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Detecciones",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF7F8C8D)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = detectionCount.toString(),
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF6F00)
                            )

                            if (detectedUserId != null) {
                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(color = Color(0xFFECF0F1))
                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "Último ID detectado:",
                                    fontSize = 14.sp,
                                    color = Color(0xFF7F8C8D)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            brush = Brush.horizontalGradient(
                                                colors = listOf(
                                                    Color(0xFFFF6F00).copy(alpha = 0.1f),
                                                    Color(0xFFFF8F00).copy(alpha = 0.1f)
                                                )
                                            )
                                        )
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = detectedUserId!!,
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFF6F00),
                                        letterSpacing = 2.sp
                                    )
                                }

                                if (lastDetectionTime.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Hora: $lastDetectionTime",
                                        fontSize = 12.sp,
                                        color = Color(0xFF95A5A6)
                                    )
                                }
                            }

                            // Dispositivos descubiertos
                            if (discoveredEndpoints.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Devices,
                                        contentDescription = null,
                                        tint = Color(0xFF00BCD4),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Descubiertos: ${discoveredEndpoints.size}",
                                        fontSize = 12.sp,
                                        color = Color(0xFF00BCD4)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Estado del sistema
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFF3E0)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BugReport,
                                    contentDescription = null,
                                    tint = Color(0xFFFF6F00),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Estado del Sistema",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE65100)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "• Permisos: ${if (hasPermissions) "Otorgados" else "Pendientes"}\n" +
                                       "• Discovery: ${if (isDiscovering) "Activo" else "Inactivo"}\n" +
                                       "• Descubiertos: ${discoveredEndpoints.size}\n" +
                                       "• Conectados: ${connectedEndpoints.size}\n" +
                                       "• Detecciones: $detectionCount\n" +
                                       "• Rango: ~1-10 metros\n" +
                                       "• Modo: Nearby Connections",
                                fontSize = 12.sp,
                                color = Color(0xFF37474F),
                                lineHeight = 18.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Instrucciones
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFE8F5E9)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Instrucciones:",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B5E20)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "1. Este dispositivo está buscando emisores\n" +
                                           "2. El otro debe estar en 'Usuario Normal (Nearby)'\n" +
                                           "3. Acerca los dispositivos (1-10 metros)\n" +
                                           "4. Detección y conexión automática\n" +
                                           "5. Se desconecta al alejarse",
                                    fontSize = 13.sp,
                                    color = Color(0xFF2E7D32),
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                }

                // Animación de éxito
                AnimatedVisibility(
                    visible = showSuccessAnimation,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut(),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .padding(32.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF27AE60),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "¡Detectado!",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF27AE60)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Usuario: $detectedUserId",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF6F00),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionsRequiredScreen(
    onRequestPermissions: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.BluetoothSearching,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color(0xFFFF6F00)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Permisos Necesarios",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2C3E50),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Para usar Nearby Connections necesitamos permisos de Bluetooth",
            fontSize = 16.sp,
            color = Color(0xFF7F8C8D),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onRequestPermissions,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF6F00)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Conceder Permisos")
        }
    }
}

/**
 * Inicia discovery de dispositivos cercanos
 */
private fun startDiscovery(
    context: Context,
    connectionsClient: ConnectionsClient,
    serviceId: String,
    onEndpointFound: (String, DiscoveredEndpointInfo) -> Unit,
    onEndpointLost: (String) -> Unit,
    onDiscoveryStarted: () -> Unit
) {
    Log.d("CelularEnBoteNearby", "========================================")
    Log.d("CelularEnBoteNearby", "=== Iniciando Discovery ===")
    Log.d("CelularEnBoteNearby", "========================================")
    Log.d("CelularEnBoteNearby", "Service ID: $serviceId")

    val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            Log.d("CelularEnBoteNearby", "========================================")
            Log.d("CelularEnBoteNearby", "🔍 DISPOSITIVO ENCONTRADO")
            Log.d("CelularEnBoteNearby", "========================================")
            Log.d("CelularEnBoteNearby", "Endpoint ID: $endpointId")
            Log.d("CelularEnBoteNearby", "Endpoint Name (User ID): ${info.endpointName}")
            Log.d("CelularEnBoteNearby", "Service ID: ${info.serviceId}")
            onEndpointFound(endpointId, info)
        }

        override fun onEndpointLost(endpointId: String) {
            Log.d("CelularEnBoteNearby", "📴 Dispositivo perdido (se alejó): $endpointId")
            onEndpointLost(endpointId)
        }
    }

    // Opciones de discovery para proximidad cercana
    val discoveryOptions = DiscoveryOptions.Builder()
        .setStrategy(Strategy.P2P_CLUSTER) // 1-10 metros, ideal para proximidad
        .build()

    Log.d("CelularEnBoteNearby", "📡 Estrategia: P2P_CLUSTER (optimizado para 1-10m)")

    connectionsClient.startDiscovery(
        serviceId,
        endpointDiscoveryCallback,
        discoveryOptions
    ).addOnSuccessListener {
        Log.d("CelularEnBoteNearby", "✅ Discovery iniciado exitosamente")
        Log.d("CelularEnBoteNearby", "🔍 Buscando dispositivos en rango de 1-10 metros...")
        onDiscoveryStarted()
    }.addOnFailureListener { e ->
        Log.e("CelularEnBoteNearby", "❌ Error al iniciar discovery: ${e.message}", e)
    }
}

/**
 * Genera un User ID aleatorio de 8 dígitos
 */
private fun generateRandomUserId(): String {
    return Random.nextInt(10000000, 99999999).toString()
}
