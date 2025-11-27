package com.biowaymexico.ui.screens.brindador

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
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
import kotlin.random.Random

/**
 * Pantalla de Usuario Normal con Google Nearby Connections - Modo Advertiser
 * Emite su ID para que dispositivos cercanos puedan detectarlo (rango 1-10m)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsuarioNormalNearbyScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Estado
    var userId by remember { mutableStateOf(generateRandomUserId()) }
    var isAdvertising by remember { mutableStateOf(false) }
    var connectionCount by remember { mutableStateOf(0) }
    var lastConnectionTime by remember { mutableStateOf("") }
    var connectedEndpoints by remember { mutableStateOf<Set<String>>(emptySet()) }
    var hasPermissions by remember { mutableStateOf(false) }

    // Nearby Connections client
    val connectionsClient = remember { Nearby.getConnectionsClient(context) }

    // Service ID único para BioWay
    val SERVICE_ID = "com.biowaymexico.nearby"

    // Verificar permisos necesarios
    fun checkPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ requiere BLUETOOTH_ADVERTISE y BLUETOOTH_CONNECT
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            // Android 11 y anteriores
            true
        }
    }

    // Launcher para permisos
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermissions = permissions.all { it.value }
        if (hasPermissions) {
            startAdvertising(context, connectionsClient, userId, SERVICE_ID,
                onConnectionInitiated = { endpointId, info ->
                    Log.d("UsuarioNormalNearby", "🔵 Conexión iniciada con: $endpointId (${info.endpointName})")
                },
                onConnectionResult = { endpointId, result ->
                    if (result.status.isSuccess) {
                        connectedEndpoints = connectedEndpoints + endpointId
                        connectionCount++
                        lastConnectionTime = java.text.SimpleDateFormat(
                            "HH:mm:ss",
                            java.util.Locale.getDefault()
                        ).format(java.util.Date())
                        Log.d("UsuarioNormalNearby", "✅ Conectado con: $endpointId")
                    }
                },
                onDisconnected = { endpointId ->
                    connectedEndpoints = connectedEndpoints - endpointId
                    Log.d("UsuarioNormalNearby", "🔴 Desconectado de: $endpointId")
                },
                onAdvertisingStarted = {
                    isAdvertising = true
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
                        Manifest.permission.BLUETOOTH_ADVERTISE,
                        Manifest.permission.BLUETOOTH_CONNECT
                    )
                )
            } else {
                hasPermissions = true
            }
        } else {
            startAdvertising(context, connectionsClient, userId, SERVICE_ID,
                onConnectionInitiated = { endpointId, info ->
                    Log.d("UsuarioNormalNearby", "🔵 Conexión iniciada con: $endpointId (${info.endpointName})")
                },
                onConnectionResult = { endpointId, result ->
                    if (result.status.isSuccess) {
                        connectedEndpoints = connectedEndpoints + endpointId
                        connectionCount++
                        lastConnectionTime = java.text.SimpleDateFormat(
                            "HH:mm:ss",
                            java.util.Locale.getDefault()
                        ).format(java.util.Date())
                        Log.d("UsuarioNormalNearby", "✅ Conectado con: $endpointId")
                    }
                },
                onDisconnected = { endpointId ->
                    connectedEndpoints = connectedEndpoints - endpointId
                    Log.d("UsuarioNormalNearby", "🔴 Desconectado de: $endpointId")
                },
                onAdvertisingStarted = {
                    isAdvertising = true
                }
            )
        }
    }

    // Lifecycle observer
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    Log.d("UsuarioNormalNearby", "⏸️ Pantalla pausada, deteniendo advertising...")
                    connectionsClient.stopAdvertising()
                    connectionsClient.stopAllEndpoints()
                    isAdvertising = false
                }
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            connectionsClient.stopAdvertising()
            connectionsClient.stopAllEndpoints()
        }
    }

    // Animación de pulso
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Usuario Normal - Nearby",
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
                    containerColor = Color(0xFF00BCD4)
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
                            Color(0xFFE0F7FA)
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
                                    Manifest.permission.BLUETOOTH_ADVERTISE,
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
                    // Animación de señal
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(200.dp)
                    ) {
                        // Círculos pulsantes
                        repeat(3) { index ->
                            Box(
                                modifier = Modifier
                                    .size(200.dp - (index * 40).dp)
                                    .scale(pulseScale - (index * 0.05f))
                                    .clip(CircleShape)
                                    .background(
                                        Color(0xFF00BCD4).copy(alpha = 0.4f - (index * 0.1f))
                                    )
                            )
                        }

                        // Icono central
                        Surface(
                            modifier = Modifier.size(100.dp),
                            shape = CircleShape,
                            color = Color(0xFF00BCD4),
                            shadowElevation = 8.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Wifi,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(60.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    // Estado
                    Text(
                        text = if (isAdvertising) "Emitiendo Señal" else "Iniciando...",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Rango de detección: 1-10 metros",
                        fontSize = 16.sp,
                        color = Color(0xFF7F8C8D),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Tarjeta con User ID
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
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color(0xFF00BCD4),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Tu ID de Usuario",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF7F8C8D)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(
                                                Color(0xFF00BCD4).copy(alpha = 0.1f),
                                                Color(0xFF00ACC1).copy(alpha = 0.1f)
                                            )
                                        )
                                    )
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = userId,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00BCD4),
                                    letterSpacing = 2.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Conexiones activas
                            if (connectedEndpoints.isNotEmpty()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Link,
                                        contentDescription = null,
                                        tint = Color(0xFF27AE60),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Conectado: ${connectedEndpoints.size} dispositivo(s)",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF27AE60)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            // Contador de conexiones
                            if (connectionCount > 0) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF00BCD4),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Total: $connectionCount",
                                        fontSize = 12.sp,
                                        color = Color(0xFF7F8C8D)
                                    )
                                    if (lastConnectionTime.isNotEmpty()) {
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "Última: $lastConnectionTime",
                                            fontSize = 12.sp,
                                            color = Color(0xFF95A5A6)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            // Botón regenerar ID
                            OutlinedButton(
                                onClick = {
                                    val newId = generateRandomUserId()
                                    userId = newId
                                    // Reiniciar advertising con nuevo ID
                                    if (isAdvertising) {
                                        connectionsClient.stopAdvertising()
                                        connectionsClient.stopAllEndpoints()
                                        startAdvertising(context, connectionsClient, newId, SERVICE_ID,
                                            onConnectionInitiated = { endpointId, info ->
                                                Log.d("UsuarioNormalNearby", "🔵 Conexión iniciada con: $endpointId")
                                            },
                                            onConnectionResult = { endpointId, result ->
                                                if (result.status.isSuccess) {
                                                    connectedEndpoints = connectedEndpoints + endpointId
                                                    connectionCount++
                                                    lastConnectionTime = java.text.SimpleDateFormat(
                                                        "HH:mm:ss",
                                                        java.util.Locale.getDefault()
                                                    ).format(java.util.Date())
                                                }
                                            },
                                            onDisconnected = { endpointId ->
                                                connectedEndpoints = connectedEndpoints - endpointId
                                            },
                                            onAdvertisingStarted = {
                                                isAdvertising = true
                                            }
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFF00BCD4)
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generar Nuevo ID")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Estado del sistema
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFE0F7FA)
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
                                    tint = Color(0xFF00838F),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Estado del Sistema",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF006064)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "• Permisos: ${if (hasPermissions) "Otorgados" else "Pendientes"}\n" +
                                       "• Advertising: ${if (isAdvertising) "Activo" else "Inactivo"}\n" +
                                       "• Conectados: ${connectedEndpoints.size}\n" +
                                       "• Total conexiones: $connectionCount\n" +
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
                            containerColor = Color(0xFFFFF9E6)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFFF57C00),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Instrucciones:",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE65100)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "1. Tu dispositivo está emitiendo señal\n" +
                                           "2. El otro debe estar en 'Celular en Bote (Nearby)'\n" +
                                           "3. Acerca los dispositivos (1-10 metros)\n" +
                                           "4. Conexión automática al detectarse\n" +
                                           "5. Desconexión automática al alejarse",
                                    fontSize = 13.sp,
                                    color = Color(0xFF5D4037),
                                    lineHeight = 20.sp
                                )
                            }
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
            tint = Color(0xFF00BCD4)
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
                containerColor = Color(0xFF00BCD4)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Conceder Permisos")
        }
    }
}

/**
 * Inicia advertising con Nearby Connections
 */
private fun startAdvertising(
    context: Context,
    connectionsClient: ConnectionsClient,
    userId: String,
    serviceId: String,
    onConnectionInitiated: (String, ConnectionInfo) -> Unit,
    onConnectionResult: (String, ConnectionResolution) -> Unit,
    onDisconnected: (String) -> Unit,
    onAdvertisingStarted: () -> Unit
) {
    Log.d("UsuarioNormalNearby", "========================================")
    Log.d("UsuarioNormalNearby", "=== Iniciando Advertising ===")
    Log.d("UsuarioNormalNearby", "========================================")
    Log.d("UsuarioNormalNearby", "User ID: $userId")
    Log.d("UsuarioNormalNearby", "Service ID: $serviceId")

    val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            Log.d("UsuarioNormalNearby", "🔵 onConnectionInitiated")
            Log.d("UsuarioNormalNearby", "Endpoint ID: $endpointId")
            Log.d("UsuarioNormalNearby", "Endpoint Name: ${connectionInfo.endpointName}")
            onConnectionInitiated(endpointId, connectionInfo)

            // Aceptar la conexión automáticamente
            connectionsClient.acceptConnection(endpointId, object : PayloadCallback() {
                override fun onPayloadReceived(endpointId: String, payload: Payload) {
                    Log.d("UsuarioNormalNearby", "📥 Payload recibido de: $endpointId")
                }

                override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
                    Log.d("UsuarioNormalNearby", "📊 Transfer update: ${update.status}")
                }
            })
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            Log.d("UsuarioNormalNearby", "🔗 onConnectionResult")
            Log.d("UsuarioNormalNearby", "Endpoint ID: $endpointId")
            Log.d("UsuarioNormalNearby", "Status: ${if (result.status.isSuccess) "✅ Exitoso" else "❌ Fallido"}")
            onConnectionResult(endpointId, result)

            if (result.status.isSuccess) {
                // Enviar el User ID al dispositivo conectado
                val payload = Payload.fromBytes(userId.toByteArray())
                connectionsClient.sendPayload(endpointId, payload)
                Log.d("UsuarioNormalNearby", "📤 User ID enviado: $userId")
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.d("UsuarioNormalNearby", "🔴 onDisconnected: $endpointId")
            onDisconnected(endpointId)
        }
    }

    // Configurar opciones de advertising para proximidad cercana
    val advertisingOptions = AdvertisingOptions.Builder()
        .setStrategy(Strategy.P2P_CLUSTER) // Optimizado para 1-10 metros
        .build()

    // Iniciar advertising
    connectionsClient.startAdvertising(
        userId, // El nombre del endpoint será el User ID
        serviceId,
        connectionLifecycleCallback,
        advertisingOptions
    ).addOnSuccessListener {
        Log.d("UsuarioNormalNearby", "✅ Advertising iniciado exitosamente")
        Log.d("UsuarioNormalNearby", "📡 Emitiendo señal en rango de 1-10 metros")
        onAdvertisingStarted()
    }.addOnFailureListener { e ->
        Log.e("UsuarioNormalNearby", "❌ Error al iniciar advertising: ${e.message}", e)
    }
}

/**
 * Genera un User ID aleatorio de 8 dígitos
 */
private fun generateRandomUserId(): String {
    return Random.nextInt(10000000, 99999999).toString()
}
