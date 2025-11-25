package com.biowaymexico.ui.screens.brindador

import android.app.Activity
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.util.Log
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.biowaymexico.ui.theme.BioWayColors
import kotlinx.coroutines.*
import java.nio.charset.Charset

/**
 * Pantalla de Celular en Bote NFC - Modo Lector con IsoDep
 * Lee tarjetas NFC emuladas (HCE) de otros dispositivos Android
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CelularEnBoteNFCScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    // Estado del NFC
    var nfcAdapter: NfcAdapter? by remember { mutableStateOf(null) }
    var isNfcEnabled by remember { mutableStateOf(false) }
    var isNfcSupported by remember { mutableStateOf(true) }
    var isListening by remember { mutableStateOf(false) }

    // Estado de detección
    var detectedUserId by remember { mutableStateOf<String?>(null) }
    var showSuccessAnimation by remember { mutableStateOf(false) }
    var detectionCount by remember { mutableStateOf(0) }
    var lastDetectionTime by remember { mutableStateOf("") }

    // AID de BioWay (debe coincidir con el del servicio HCE)
    val BIOWAY_AID = byteArrayOf(
        0xF0.toByte(), 0x01, 0x02, 0x03, 0x04, 0x05, 0x06
    )

    // Comando SELECT AID
    val SELECT_APDU = buildSelectAidCommand(BIOWAY_AID)

    // Comando GET USER ID
    val GET_USER_ID_APDU = byteArrayOf(
        0x00.toByte(), // CLA
        0xCA.toByte(), // INS - GET DATA
        0x00.toByte(), // P1
        0x00.toByte(), // P2
        0x00.toByte()  // Le (longitud esperada)
    )

    // Callback del NFC Reader
    val nfcReaderCallback = NfcAdapter.ReaderCallback { tag ->
        Log.d("CelularEnBoteNFC", "========================================")
        Log.d("CelularEnBoteNFC", "🔵 TAG DETECTADO")
        Log.d("CelularEnBoteNFC", "========================================")
        Log.d("CelularEnBoteNFC", "Tag ID: ${tag.id.joinToString(":")}")
        Log.d("CelularEnBoteNFC", "Tecnologías soportadas: ${tag.techList.joinToString(", ")}")

        // Usar coroutine para operaciones IO
        scope.launch {
            try {
                val userId = withContext(Dispatchers.IO) {
                    readUserIdFromHce(tag, SELECT_APDU, GET_USER_ID_APDU)
                }

                if (userId != null) {
                    detectedUserId = userId
                    showSuccessAnimation = true
                    detectionCount++
                    lastDetectionTime = java.text.SimpleDateFormat(
                        "HH:mm:ss",
                        java.util.Locale.getDefault()
                    ).format(java.util.Date())

                    Log.d("CelularEnBoteNFC", "========================================")
                    Log.d("CelularEnBoteNFC", "✅ DETECCIÓN EXITOSA")
                    Log.d("CelularEnBoteNFC", "User ID: $userId")
                    Log.d("CelularEnBoteNFC", "Total detecciones: $detectionCount")
                    Log.d("CelularEnBoteNFC", "========================================")

                    // Vibrar para feedback
                    val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                    vibrator?.vibrate(200)
                } else {
                    Log.w("CelularEnBoteNFC", "❌ No se pudo obtener el User ID")
                }
            } catch (e: Exception) {
                Log.e("CelularEnBoteNFC", "❌ Error en callback: ${e.message}", e)
            }
        }
    }

    // Función para iniciar el reader mode
    fun startReaderMode() {
        if (nfcAdapter != null && isNfcEnabled && activity != null) {
            try {
                val options = Bundle()
                // Configuración optimizada para máxima distancia de detección
                options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 50)

                nfcAdapter!!.enableReaderMode(
                    activity,
                    nfcReaderCallback,
                    // Todos los tipos de NFC + skip NDEF check para HCE
                    NfcAdapter.FLAG_READER_NFC_A or
                    NfcAdapter.FLAG_READER_NFC_B or
                    NfcAdapter.FLAG_READER_NFC_F or
                    NfcAdapter.FLAG_READER_NFC_V or
                    NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK or // Importante para HCE
                    NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,
                    options
                )
                isListening = true
                Log.d("CelularEnBoteNFC", "✅ Reader mode habilitado con FLAG_READER_SKIP_NDEF_CHECK")
            } catch (e: Exception) {
                Log.e("CelularEnBoteNFC", "❌ Error al habilitar reader mode", e)
            }
        } else {
            Log.w("CelularEnBoteNFC", "⚠️ No se puede iniciar reader mode: adapter=$nfcAdapter, enabled=$isNfcEnabled, activity=$activity")
        }
    }

    // Función para detener el reader mode
    fun stopReaderMode() {
        if (activity != null && nfcAdapter != null) {
            try {
                nfcAdapter!!.disableReaderMode(activity)
                isListening = false
                Log.d("CelularEnBoteNFC", "⏹️ Reader mode deshabilitado")
            } catch (e: Exception) {
                Log.e("CelularEnBoteNFC", "❌ Error al deshabilitar reader mode", e)
            }
        }
    }

    // Función para verificar el estado del NFC
    fun checkNfcStatus() {
        if (nfcAdapter != null) {
            val wasEnabled = isNfcEnabled
            isNfcEnabled = nfcAdapter!!.isEnabled
            Log.d("CelularEnBoteNFC", "Estado NFC: ${if (isNfcEnabled) "Habilitado ✅" else "Deshabilitado ❌"}")

            if (isNfcEnabled && !wasEnabled) {
                Log.d("CelularEnBoteNFC", "🔄 NFC se habilitó, iniciando reader mode...")
                startReaderMode()
            } else if (!isNfcEnabled && wasEnabled) {
                Log.d("CelularEnBoteNFC", "⚠️ NFC se deshabilitó, deteniendo reader mode...")
                stopReaderMode()
            }
        }
    }

    // Animación de escaneo
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

    // Inicializar NFC
    LaunchedEffect(Unit) {
        nfcAdapter = NfcAdapter.getDefaultAdapter(context)
        Log.d("CelularEnBoteNFC", "========================================")
        Log.d("CelularEnBoteNFC", "=== Inicializando Celular en Bote NFC (Reader Mode) ===")
        Log.d("CelularEnBoteNFC", "========================================")

        if (nfcAdapter == null) {
            isNfcSupported = false
            Log.w("CelularEnBoteNFC", "❌ NFC no soportado en este dispositivo")
        } else {
            isNfcEnabled = nfcAdapter!!.isEnabled
            Log.d("CelularEnBoteNFC", "✅ NFC soportado")
            Log.d("CelularEnBoteNFC", "Estado: ${if (isNfcEnabled) "Habilitado ✅" else "Deshabilitado ❌"}")

            if (isNfcEnabled) {
                startReaderMode()
            } else {
                Log.w("CelularEnBoteNFC", "⚠️ NFC está deshabilitado. Por favor habilítalo en configuración.")
            }
        }
    }

    // Observer de lifecycle
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    Log.d("CelularEnBoteNFC", "📱 Pantalla reanudada, verificando estado NFC...")
                    checkNfcStatus()
                    if (isNfcEnabled && !isListening) {
                        startReaderMode()
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    Log.d("CelularEnBoteNFC", "⏸️ Pantalla pausada, deteniendo reader mode...")
                    stopReaderMode()
                }
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            stopReaderMode()
        }
    }

    // Auto-cerrar el mensaje de éxito después de 3 segundos
    LaunchedEffect(showSuccessAnimation) {
        if (showSuccessAnimation) {
            kotlinx.coroutines.delay(3000)
            showSuccessAnimation = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Celular en Bote - NFC",
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
                    containerColor = Color(0xFF9B59B6)
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
                            Color(0xFFF3E5F5)
                        )
                    )
                )
                .padding(paddingValues)
        ) {
            if (!isNfcSupported) {
                NFCNotSupportedScreen()
            } else if (!isNfcEnabled) {
                NFCDisabledScreen()
            } else {
                // Pantalla principal de recepción
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Animación de escaneo
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(200.dp)
                    ) {
                        // Círculos pulsantes de escaneo
                        repeat(3) { index ->
                            Box(
                                modifier = Modifier
                                    .size(200.dp - (index * 40).dp)
                                    .scale(pulseScale - (index * 0.05f))
                                    .clip(CircleShape)
                                    .background(
                                        Color(0xFF9B59B6).copy(alpha = 0.3f - (index * 0.08f))
                                    )
                            )
                        }

                        // Icono de sensores central
                        Surface(
                            modifier = Modifier.size(100.dp),
                            shape = CircleShape,
                            color = Color(0xFF9B59B6),
                            shadowElevation = 8.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Sensors,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(60.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    // Estado de escucha
                    Text(
                        text = if (isListening) "Escuchando..." else "Listo para recibir",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Esperando dispositivo emisor cercano",
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
                                color = Color(0xFF9B59B6)
                            )

                            if (detectedUserId != null) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Divider(color = Color(0xFFECF0F1))
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
                                                    Color(0xFF9B59B6).copy(alpha = 0.1f),
                                                    Color(0xFFAB47BC).copy(alpha = 0.1f)
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
                                        color = Color(0xFF9B59B6),
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
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Estado del NFC (Debug info)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF3E5F5)
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
                                    tint = Color(0xFF9B59B6),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Estado del Sistema",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF7B1FA2)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "• NFC Soportado: ${if (isNfcSupported) "Sí" else "No"}\n" +
                                       "• NFC Habilitado: ${if (isNfcEnabled) "Sí" else "No"}\n" +
                                       "• Escuchando: ${if (isListening) "Sí" else "No"}\n" +
                                       "• Modo: IsoDep Reader\n" +
                                       "• Detecciones: $detectionCount",
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
                                    text = "1. Este dispositivo está en modo lector\n" +
                                           "2. El otro debe estar en 'Usuario Normal'\n" +
                                           "3. Acerca las partes traseras de ambos\n" +
                                           "4. El ID aparecerá automáticamente",
                                    fontSize = 13.sp,
                                    color = Color(0xFF2E7D32),
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                }

                // Animación de éxito flotante
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
                                color = Color(0xFF9B59B6),
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
                    val intent = android.content.Intent(android.provider.Settings.ACTION_NFC_SETTINGS)
                    androidx.core.content.ContextCompat.startActivity(context, intent, null)
                } catch (e: Exception) {
                    val intent = android.content.Intent(android.provider.Settings.ACTION_SETTINGS)
                    androidx.core.content.ContextCompat.startActivity(context, intent, null)
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF9B59B6)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Abrir Configuración")
        }
    }
}

/**
 * Lee el User ID de un dispositivo con HCE usando comandos APDU
 */
private fun readUserIdFromHce(tag: Tag, selectApdu: ByteArray, getUserIdApdu: ByteArray): String? {
    Log.d("CelularEnBoteNFC", "=== INICIO readUserIdFromHce ===")

    // Verificar si el tag soporta IsoDep
    val isoDep = IsoDep.get(tag)
    if (isoDep == null) {
        Log.w("CelularEnBoteNFC", "❌ Tag no soporta IsoDep")
        Log.d("CelularEnBoteNFC", "Tecnologías disponibles: ${tag.techList.joinToString(", ")}")
        return null
    }

    try {
        Log.d("CelularEnBoteNFC", "✅ IsoDep disponible, conectando...")
        isoDep.connect()
        Log.d("CelularEnBoteNFC", "✅ Conectado!")
        Log.d("CelularEnBoteNFC", "Timeout: ${isoDep.timeout}ms")
        Log.d("CelularEnBoteNFC", "Max transceive length: ${isoDep.maxTransceiveLength} bytes")

        // Aumentar timeout para mejorar comunicación
        isoDep.timeout = 3000
        Log.d("CelularEnBoteNFC", "Timeout actualizado a: ${isoDep.timeout}ms")

        // Paso 1: Enviar comando SELECT AID
        Log.d("CelularEnBoteNFC", "📤 Enviando SELECT APDU: ${selectApdu.toHexString()}")
        val selectResponse = isoDep.transceive(selectApdu)
        Log.d("CelularEnBoteNFC", "📥 Respuesta SELECT: ${selectResponse.toHexString()}")

        // Verificar que el SELECT fue exitoso (90 00)
        if (!isSuccessResponse(selectResponse)) {
            Log.e("CelularEnBoteNFC", "❌ SELECT falló. Respuesta: ${selectResponse.toHexString()}")
            isoDep.close()
            return null
        }
        Log.d("CelularEnBoteNFC", "✅ SELECT exitoso!")

        // Paso 2: Enviar comando GET USER ID
        Log.d("CelularEnBoteNFC", "📤 Enviando GET_USER_ID APDU: ${getUserIdApdu.toHexString()}")
        val userIdResponse = isoDep.transceive(getUserIdApdu)
        Log.d("CelularEnBoteNFC", "📥 Respuesta GET_USER_ID: ${userIdResponse.toHexString()}")
        Log.d("CelularEnBoteNFC", "Longitud de respuesta: ${userIdResponse.size} bytes")

        // La respuesta debe terminar en 90 00 (éxito)
        if (userIdResponse.size < 2) {
            Log.e("CelularEnBoteNFC", "❌ Respuesta muy corta")
            isoDep.close()
            return null
        }

        // Extraer el User ID (todos los bytes excepto los últimos 2 que son el status)
        val userIdBytes = userIdResponse.copyOfRange(0, userIdResponse.size - 2)
        val statusBytes = userIdResponse.copyOfRange(userIdResponse.size - 2, userIdResponse.size)

        Log.d("CelularEnBoteNFC", "Status bytes: ${statusBytes.toHexString()}")
        Log.d("CelularEnBoteNFC", "User ID bytes: ${userIdBytes.toHexString()}")

        if (!isSuccessResponse(statusBytes)) {
            Log.e("CelularEnBoteNFC", "❌ GET_USER_ID falló. Status: ${statusBytes.toHexString()}")
            isoDep.close()
            return null
        }

        // Convertir bytes a string
        val userId = String(userIdBytes, Charset.forName("UTF-8"))
        Log.d("CelularEnBoteNFC", "✅ User ID extraído: '$userId'")

        // Validar formato
        if (userId.matches(Regex("\\d{8}"))) {
            Log.d("CelularEnBoteNFC", "✅ Formato de User ID válido!")
            isoDep.close()
            Log.d("CelularEnBoteNFC", "=== FIN readUserIdFromHce (ÉXITO) ===")
            return userId
        } else {
            Log.w("CelularEnBoteNFC", "⚠️ Formato de User ID inválido: '$userId'")
            isoDep.close()
            return null
        }

    } catch (e: Exception) {
        Log.e("CelularEnBoteNFC", "❌ ERROR durante transceive: ${e.javaClass.simpleName}: ${e.message}")
        e.printStackTrace()

        try {
            isoDep.close()
        } catch (closeException: Exception) {
            Log.e("CelularEnBoteNFC", "Error al cerrar IsoDep: ${closeException.message}")
        }

        return null
    } finally {
        Log.d("CelularEnBoteNFC", "=== FIN readUserIdFromHce ===")
    }
}

/**
 * Construye el comando SELECT AID APDU
 */
private fun buildSelectAidCommand(aid: ByteArray): ByteArray {
    return byteArrayOf(
        0x00.toByte(),        // CLA
        0xA4.toByte(),        // INS - SELECT
        0x04.toByte(),        // P1
        0x00.toByte(),        // P2
        aid.size.toByte()     // Lc (longitud del AID)
    ) + aid + byteArrayOf(0x00.toByte()) // Le
}

/**
 * Verifica si la respuesta APDU indica éxito (90 00)
 */
private fun isSuccessResponse(response: ByteArray): Boolean {
    return response.size >= 2 &&
           response[response.size - 2] == 0x90.toByte() &&
           response[response.size - 1] == 0x00.toByte()
}

/**
 * Extensión para convertir ByteArray a String hexadecimal
 */
private fun ByteArray.toHexString(): String {
    return joinToString(" ") { "%02X".format(it) }
}
