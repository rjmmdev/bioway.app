package com.biowaymexico.ui.screens.brindador

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.cardemulation.CardEmulation
import android.util.Log
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.biowaymexico.nfc.BioWayHceService
import com.biowaymexico.ui.theme.BioWayColors
import kotlin.random.Random

/**
 * Pantalla de Usuario Normal NFC - Modo Emisor con HCE
 * Emula una tarjeta NFC virtual que puede ser leída por otro dispositivo
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsuarioNormalNFCScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current

    // Estado del NFC
    var nfcAdapter: NfcAdapter? by remember { mutableStateOf(null) }
    var cardEmulation: CardEmulation? by remember { mutableStateOf(null) }
    var isNfcEnabled by remember { mutableStateOf(false) }
    var isNfcSupported by remember { mutableStateOf(true) }
    var isHceSupported by remember { mutableStateOf(false) }
    var userId by remember { mutableStateOf(generateRandomUserId()) }
    var sessionCount by remember { mutableStateOf(0) }
    var lastSessionTime by remember { mutableStateOf("") }

    // Receiver para eventos del servicio HCE
    val nfcSessionReceiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    "com.biowaymexico.NFC_SESSION_END" -> {
                        sessionCount++
                        lastSessionTime = java.text.SimpleDateFormat(
                            "HH:mm:ss",
                            java.util.Locale.getDefault()
                        ).format(java.util.Date())
                        Log.d("UsuarioNormalNFC", "Sesión finalizada. Total: $sessionCount")
                    }
                }
            }
        }
    }

    // Función para actualizar el User ID en el servicio HCE
    fun updateUserIdInService(newUserId: String) {
        BioWayHceService.currentUserId = newUserId
        userId = newUserId
        Log.d("UsuarioNormalNFC", "User ID actualizado en servicio HCE: $newUserId")
    }

    // Función para verificar el estado del NFC
    fun checkNfcStatus() {
        if (nfcAdapter != null) {
            val wasEnabled = isNfcEnabled
            isNfcEnabled = nfcAdapter!!.isEnabled
            Log.d("UsuarioNormalNFC", "Estado NFC: ${if (isNfcEnabled) "Habilitado" else "Deshabilitado"}")

            if (isNfcEnabled && !wasEnabled) {
                Log.d("UsuarioNormalNFC", "✅ NFC se habilitó")
            } else if (!isNfcEnabled && wasEnabled) {
                Log.d("UsuarioNormalNFC", "⚠️ NFC se deshabilitó")
            }
        }
    }

    // Inicializar NFC y HCE
    LaunchedEffect(Unit) {
        nfcAdapter = NfcAdapter.getDefaultAdapter(context)
        Log.d("UsuarioNormalNFC", "=== Inicializando Usuario Normal NFC (HCE Mode) ===")

        if (nfcAdapter == null) {
            isNfcSupported = false
            Log.w("UsuarioNormalNFC", "❌ NFC no soportado en este dispositivo")
        } else {
            isNfcEnabled = nfcAdapter!!.isEnabled
            Log.d("UsuarioNormalNFC", "✅ NFC soportado. Estado: ${if (isNfcEnabled) "Habilitado" else "Deshabilitado"}")

            // Verificar soporte de HCE
            cardEmulation = CardEmulation.getInstance(nfcAdapter)
            isHceSupported = cardEmulation != null
            Log.d("UsuarioNormalNFC", "HCE soportado: $isHceSupported")

            // Configurar el User ID en el servicio
            updateUserIdInService(userId)

            // Registrar receiver para eventos del servicio
            val filter = IntentFilter("com.biowaymexico.NFC_SESSION_END")
            context.registerReceiver(nfcSessionReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        }
    }

    // Observer de lifecycle
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    Log.d("UsuarioNormalNFC", "📱 Pantalla reanudada, verificando estado NFC...")
                    checkNfcStatus()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    Log.d("UsuarioNormalNFC", "⏸️ Pantalla pausada")
                }
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            try {
                context.unregisterReceiver(nfcSessionReceiver)
            } catch (e: Exception) {
                // Receiver ya no registrado
            }
            lifecycleOwner.lifecycle.removeObserver(observer)
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

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Usuario Normal - NFC",
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
                    containerColor = Color(0xFF4A90E2)
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
                            Color(0xFFE3F2FD)
                        )
                    )
                )
                .padding(paddingValues)
        ) {
            if (!isNfcSupported) {
                NFCNotSupportedScreen()
            } else if (!isNfcEnabled) {
                NFCDisabledScreen()
            } else if (!isHceSupported) {
                HCENotSupportedScreen()
            } else {
                // Pantalla principal de emisión HCE
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Animación de NFC
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
                                        Color(0xFF4A90E2).copy(alpha = pulseAlpha - (index * 0.2f))
                                    )
                            )
                        }

                        // Icono NFC central
                        Surface(
                            modifier = Modifier.size(100.dp),
                            shape = CircleShape,
                            color = Color(0xFF4A90E2),
                            shadowElevation = 8.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Nfc,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(60.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    // Estado de emisión
                    Text(
                        text = "Modo Emisor Activo",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Acerca un dispositivo lector para compartir tu ID",
                        fontSize = 16.sp,
                        color = Color(0xFF7F8C8D),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Tarjeta con el User ID
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
                                    tint = Color(0xFF4A90E2),
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

                            // User ID con fondo
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(
                                                Color(0xFF4A90E2).copy(alpha = 0.1f),
                                                Color(0xFF5AB9EA).copy(alpha = 0.1f)
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
                                    color = Color(0xFF4A90E2),
                                    letterSpacing = 2.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Contador de sesiones
                            if (sessionCount > 0) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF27AE60),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Sesiones: $sessionCount",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF27AE60)
                                    )
                                    if (lastSessionTime.isNotEmpty()) {
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "Última: $lastSessionTime",
                                            fontSize = 12.sp,
                                            color = Color(0xFF7F8C8D)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            // Botón para regenerar ID
                            OutlinedButton(
                                onClick = {
                                    val newId = generateRandomUserId()
                                    updateUserIdInService(newId)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFF4A90E2)
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

                    // Estado del NFC (Debug info)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFE3F2FD)
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
                                    tint = Color(0xFF1976D2),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Estado del Sistema",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1976D2)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "• NFC Soportado: ${if (isNfcSupported) "Sí" else "No"}\n" +
                                       "• NFC Habilitado: ${if (isNfcEnabled) "Sí" else "No"}\n" +
                                       "• HCE Soportado: ${if (isHceSupported) "Sí" else "No"}\n" +
                                       "• Modo: Card Emulation (HCE)\n" +
                                       "• Sesiones: $sessionCount",
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
                                    text = "1. Este dispositivo está emulando una tarjeta NFC\n" +
                                           "2. El otro dispositivo debe estar en modo 'Celular en Bote'\n" +
                                           "3. Acerca las partes traseras de ambos dispositivos\n" +
                                           "4. Mantén cerca por 1-2 segundos",
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
private fun NFCNotSupportedScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color(0xFFE74C3C)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "NFC No Disponible",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2C3E50),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Tu dispositivo no tiene soporte para NFC",
            fontSize = 16.sp,
            color = Color(0xFF7F8C8D),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun NFCDisabledScreen() {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.WarningAmber,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color(0xFFF39C12)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "NFC Desactivado",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2C3E50),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Por favor, activa el NFC en la configuración de tu dispositivo",
            fontSize = 16.sp,
            color = Color(0xFF7F8C8D),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                try {
                    val intent = Intent(android.provider.Settings.ACTION_NFC_SETTINGS)
                    androidx.core.content.ContextCompat.startActivity(context, intent, null)
                } catch (e: Exception) {
                    val intent = Intent(android.provider.Settings.ACTION_SETTINGS)
                    androidx.core.content.ContextCompat.startActivity(context, intent, null)
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4A90E2)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Abrir Configuración")
        }
    }
}

@Composable
private fun HCENotSupportedScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color(0xFFE74C3C)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "HCE No Soportado",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2C3E50),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Tu dispositivo no tiene soporte para Host Card Emulation (HCE). Requiere Android 4.4 (API 19) o superior.",
            fontSize = 16.sp,
            color = Color(0xFF7F8C8D),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Genera un User ID aleatorio de 8 dígitos
 */
private fun generateRandomUserId(): String {
    return Random.nextInt(10000000, 99999999).toString()
}
