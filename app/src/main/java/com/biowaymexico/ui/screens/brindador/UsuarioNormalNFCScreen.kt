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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.biowaymexico.data.BoteSesionRepository
import com.biowaymexico.data.models.SesionActiva
import com.biowaymexico.data.models.toSesionActiva
import com.biowaymexico.nfc.BioWayHceService
import com.biowaymexico.ui.theme.BioWayColors
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Estados de la pantalla del brindador
 */
enum class EstadoPantallaBrindador {
    ESPERANDO_NFC,      // Mostrando HCE, esperando escaneo
    SESION_ACTIVA,      // Sesión en curso, mostrando puntos y timer
    SESION_FINALIZADA   // Resumen de la sesión
}

/**
 * Pantalla de Usuario Normal NFC - Modo Emisor con HCE + Sesión de Reciclaje
 * Emula una tarjeta NFC virtual y muestra el progreso de la sesión de reciclaje
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsuarioNormalNFCScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    // Firebase
    val firebaseAuth = remember { FirebaseAuth.getInstance() }
    val firestore = remember { FirebaseFirestore.getInstance() }
    val firebaseUserId = firebaseAuth.currentUser?.uid ?: ""

    // Si no hay usuario de Firebase (modo mock login), generar un ID de prueba
    // Este ID es temporal y solo funciona para pruebas NFC, no para Firestore
    val testUserId = remember {
        if (firebaseUserId.isEmpty()) {
            "TEST_USER_" + java.util.UUID.randomUUID().toString().substring(0, 8)
        } else {
            firebaseUserId
        }
    }

    // Estado del NFC
    var nfcAdapter: NfcAdapter? by remember { mutableStateOf(null) }
    var cardEmulation: CardEmulation? by remember { mutableStateOf(null) }
    var isNfcEnabled by remember { mutableStateOf(false) }
    var isNfcSupported by remember { mutableStateOf(true) }
    var isHceSupported by remember { mutableStateOf(false) }
    var userId by remember { mutableStateOf(testUserId) }

    // Estado de la pantalla
    var estadoPantalla by remember { mutableStateOf(EstadoPantallaBrindador.ESPERANDO_NFC) }

    // Estado de la sesión
    var sesionActiva by remember { mutableStateOf<SesionActiva?>(null) }
    var sesionFinalizada by remember { mutableStateOf<SesionActiva?>(null) }
    var sesionListener by remember { mutableStateOf<ListenerRegistration?>(null) }

    // Timer de sesión
    var tiempoRestanteSegundos by remember { mutableStateOf(0) }

    // Receiver para eventos del servicio HCE
    val nfcSessionReceiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    "com.biowaymexico.NFC_SESSION_END" -> {
                        Log.d("UsuarioNormalNFC", "Evento NFC_SESSION_END recibido")
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
            isNfcEnabled = nfcAdapter!!.isEnabled
            Log.d("UsuarioNormalNFC", "Estado NFC: ${if (isNfcEnabled) "Habilitado" else "Deshabilitado"}")
        }
    }

    // Función para finalizar la sesión desde el brindador
    fun finalizarSesionPorBrindador() {
        scope.launch {
            try {
                // Marcar la sesión como finalizada en Firestore
                firestore.collection("Brindador")
                    .document(userId)
                    .update("sesionActiva.estado", SesionActiva.ESTADO_FINALIZADA_BRINDADOR)

                Log.d("UsuarioNormalNFC", "✅ Sesión marcada como finalizada por brindador")
            } catch (e: Exception) {
                Log.e("UsuarioNormalNFC", "❌ Error finalizando sesión: ${e.message}")
            }
        }
    }

    // Función para cerrar el resumen y volver a esperar
    fun cerrarResumen() {
        sesionFinalizada = null
        estadoPantalla = EstadoPantallaBrindador.ESPERANDO_NFC
    }

    // Iniciar listener de sesión
    fun iniciarListenerSesion() {
        if (userId.isEmpty()) return

        sesionListener?.remove()

        sesionListener = firestore.collection("Brindador")
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("UsuarioNormalNFC", "Error en listener: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    @Suppress("UNCHECKED_CAST")
                    val sesionData = snapshot.get("sesionActiva") as? Map<String, Any?>

                    if (sesionData != null) {
                        val sesion = sesionData.toSesionActiva()

                        when (sesion.estado) {
                            SesionActiva.ESTADO_ACTIVA -> {
                                sesionActiva = sesion
                                if (estadoPantalla != EstadoPantallaBrindador.SESION_ACTIVA) {
                                    estadoPantalla = EstadoPantallaBrindador.SESION_ACTIVA
                                    Log.d("UsuarioNormalNFC", "🎉 Sesión detectada!")
                                }
                            }
                            else -> {
                                // Sesión finalizada
                                if (estadoPantalla == EstadoPantallaBrindador.SESION_ACTIVA) {
                                    sesionFinalizada = sesion
                                    sesionActiva = null
                                    estadoPantalla = EstadoPantallaBrindador.SESION_FINALIZADA
                                    Log.d("UsuarioNormalNFC", "🏁 Sesión finalizada: ${sesion.estado}")
                                }
                            }
                        }
                    } else {
                        // No hay sesión
                        if (estadoPantalla == EstadoPantallaBrindador.SESION_ACTIVA) {
                            // La sesión fue eliminada (finalizada por el bote)
                            sesionFinalizada = sesionActiva
                            sesionActiva = null
                            estadoPantalla = EstadoPantallaBrindador.SESION_FINALIZADA
                        } else if (estadoPantalla != EstadoPantallaBrindador.SESION_FINALIZADA) {
                            estadoPantalla = EstadoPantallaBrindador.ESPERANDO_NFC
                        }
                    }
                }
            }
    }

    // Inicializar NFC y HCE
    LaunchedEffect(Unit) {
        nfcAdapter = NfcAdapter.getDefaultAdapter(context)
        Log.d("UsuarioNormalNFC", "========================================")
        Log.d("UsuarioNormalNFC", "=== Inicializando Usuario Normal NFC (HCE Mode) ===")
        Log.d("UsuarioNormalNFC", "========================================")
        Log.d("UsuarioNormalNFC", "   firebaseUserId: '$firebaseUserId'")
        Log.d("UsuarioNormalNFC", "   testUserId: '$testUserId'")
        Log.d("UsuarioNormalNFC", "   userId (inicial): '$userId'")

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

            // Verificar si nuestro servicio es el predeterminado para el AID
            if (cardEmulation != null && activity != null) {
                val serviceComponent = android.content.ComponentName(
                    context,
                    com.biowaymexico.nfc.BioWayHceService::class.java
                )
                val aid = "F0010203040506"

                val isDefault = cardEmulation!!.isDefaultServiceForAid(serviceComponent, aid)
                Log.d("UsuarioNormalNFC", "📋 ¿Servicio es default para AID $aid? $isDefault")

                if (!isDefault) {
                    Log.w("UsuarioNormalNFC", "⚠️ Nuestro servicio NO es el default, intentando establecer...")
                    // Verificar si podemos ser el default
                    val canBeDefault = cardEmulation!!.categoryAllowsForegroundPreference(CardEmulation.CATEGORY_OTHER)
                    Log.d("UsuarioNormalNFC", "   ¿Categoría permite foreground? $canBeDefault")

                    if (canBeDefault) {
                        // Intentar establecer como servicio preferido en foreground
                        val success = cardEmulation!!.setPreferredService(activity, serviceComponent)
                        Log.d("UsuarioNormalNFC", "   setPreferredService resultado: $success")
                    }
                } else {
                    Log.d("UsuarioNormalNFC", "✅ Ya somos el servicio default para este AID")
                }
            }

            // Configurar el User ID en el servicio
            Log.d("UsuarioNormalNFC", "📱 Configurando HCE con userId: '$userId'")
            updateUserIdInService(userId)

            // Registrar receiver para eventos del servicio
            val filter = IntentFilter("com.biowaymexico.NFC_SESSION_END")
            context.registerReceiver(nfcSessionReceiver, filter, Context.RECEIVER_NOT_EXPORTED)

            // Iniciar listener de sesión
            iniciarListenerSesion()
        }
    }

    // Timer para actualizar tiempo restante
    LaunchedEffect(sesionActiva) {
        if (sesionActiva == null) return@LaunchedEffect

        while (sesionActiva != null && estadoPantalla == EstadoPantallaBrindador.SESION_ACTIVA) {
            val inicio = sesionActiva?.inicioSesion?.toDate()?.time ?: System.currentTimeMillis()
            val tiempoMaximoMs = (sesionActiva?.tiempoMaximoSegundos ?: 180) * 1000L
            val ahora = System.currentTimeMillis()
            val restante = maxOf(0, (inicio + tiempoMaximoMs - ahora) / 1000)

            tiempoRestanteSegundos = restante.toInt()

            delay(1000)
        }
    }

    // Observer de lifecycle
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    Log.d("UsuarioNormalNFC", "📱 Pantalla reanudada")
                    checkNfcStatus()
                    // Re-establecer como servicio preferido
                    if (cardEmulation != null && activity != null) {
                        val serviceComponent = android.content.ComponentName(
                            context,
                            com.biowaymexico.nfc.BioWayHceService::class.java
                        )
                        cardEmulation!!.setPreferredService(activity, serviceComponent)
                        Log.d("UsuarioNormalNFC", "🔄 Re-establecido como servicio preferido")
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    Log.d("UsuarioNormalNFC", "⏸️ Pantalla pausada")
                    // Limpiar servicio preferido
                    if (cardEmulation != null && activity != null) {
                        cardEmulation!!.unsetPreferredService(activity)
                        Log.d("UsuarioNormalNFC", "🔄 Servicio preferido removido")
                    }
                }
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            // Limpiar servicio preferido al salir
            if (cardEmulation != null && activity != null) {
                try {
                    cardEmulation!!.unsetPreferredService(activity)
                } catch (e: Exception) { }
            }
            try {
                context.unregisterReceiver(nfcSessionReceiver)
            } catch (e: Exception) { }
            sesionListener?.remove()
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
                        when (estadoPantalla) {
                            EstadoPantallaBrindador.ESPERANDO_NFC -> "Reciclar Ahora"
                            EstadoPantallaBrindador.SESION_ACTIVA -> "Sesión Activa"
                            EstadoPantallaBrindador.SESION_FINALIZADA -> "Sesión Completada"
                        },
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
                    containerColor = when (estadoPantalla) {
                        EstadoPantallaBrindador.ESPERANDO_NFC -> Color(0xFF4A90E2)
                        EstadoPantallaBrindador.SESION_ACTIVA -> BioWayColors.PrimaryGreen
                        EstadoPantallaBrindador.SESION_FINALIZADA -> Color(0xFF27AE60)
                    }
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
            when {
                !isNfcSupported -> NFCNotSupportedScreen()
                !isNfcEnabled -> NFCDisabledScreen()
                !isHceSupported -> HCENotSupportedScreen()
                estadoPantalla == EstadoPantallaBrindador.SESION_ACTIVA && sesionActiva != null -> {
                    SesionActivaScreen(
                        sesion = sesionActiva!!,
                        tiempoRestanteSegundos = tiempoRestanteSegundos,
                        onFinalizarSesion = { finalizarSesionPorBrindador() }
                    )
                }
                estadoPantalla == EstadoPantallaBrindador.SESION_FINALIZADA && sesionFinalizada != null -> {
                    SesionFinalizadaScreen(
                        sesion = sesionFinalizada!!,
                        onCerrar = { cerrarResumen() }
                    )
                }
                else -> {
                    EsperandoNFCScreen(
                        userId = userId,
                        pulseScale = pulseScale,
                        pulseAlpha = pulseAlpha,
                        isHceSupported = isHceSupported,
                        isNfcEnabled = isNfcEnabled,
                        isNfcSupported = isNfcSupported
                    )
                }
            }
        }
    }
}

@Composable
private fun EsperandoNFCScreen(
    userId: String,
    pulseScale: Float,
    pulseAlpha: Float,
    isHceSupported: Boolean,
    isNfcEnabled: Boolean,
    isNfcSupported: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
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

        Text(
            text = "Listo para Reciclar",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2C3E50)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Acerca tu celular al Bote BioWay para iniciar",
            fontSize = 16.sp,
            color = Color(0xFF7F8C8D),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Tarjeta con el User ID
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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

                // User ID (mostrar solo últimos 8 caracteres)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF4A90E2).copy(alpha = 0.1f))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "****${userId.takeLast(8)}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4A90E2),
                        letterSpacing = 2.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Instrucciones
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9E6))
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
                        text = "1. Acerca la parte trasera de tu celular al Bote BioWay\n" +
                               "2. Espera a que se inicie la sesión\n" +
                               "3. Deposita tus materiales reciclables\n" +
                               "4. ¡Gana BioCoins por cada material!",
                        fontSize = 13.sp,
                        color = Color(0xFF5D4037),
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SesionActivaScreen(
    sesion: SesionActiva,
    tiempoRestanteSegundos: Int,
    onFinalizarSesion: () -> Unit
) {
    val minutos = tiempoRestanteSegundos / 60
    val segundos = tiempoRestanteSegundos % 60

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Indicador de conexión
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Conectado a: ${sesion.boteNombre}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                    Text(
                        text = "Depositando materiales...",
                        fontSize = 14.sp,
                        color = Color(0xFF388E3C)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Timer
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = if (tiempoRestanteSegundos < 30) Color(0xFFE53935) else Color(0xFF4A90E2),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = String.format("%d:%02d", minutos, segundos),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (tiempoRestanteSegundos < 30) Color(0xFFE53935) else Color(0xFF2C3E50)
                )
                Text(
                    text = "tiempo restante",
                    fontSize = 14.sp,
                    color = Color(0xFF7F8C8D)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // BioCoins acumulados
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFFF8E1)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🪙",
                    fontSize = 40.sp
                )
                Text(
                    text = "+${sesion.puntosAcumulados}",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF8F00)
                )
                Text(
                    text = "BioCoins esta sesión",
                    fontSize = 14.sp,
                    color = Color(0xFF7F8C8D)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Lista de materiales depositados
        if (sesion.materialesDepositados.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Materiales depositados (${sesion.materialesDepositados.size})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Agrupar materiales por tipo
                    val materialesAgrupados = sesion.materialesDepositados.groupBy { it.tipo }
                    materialesAgrupados.forEach { (tipo, materiales) ->
                        val totalPuntos = materiales.sumOf { it.puntos }
                        val icono = when (tipo.lowercase()) {
                            "plastico", "plastic" -> "🥤"
                            "metal", "aluminio" -> "🥫"
                            "vidrio", "glass" -> "🍾"
                            "carton", "cardboard" -> "📦"
                            "papel", "paper" -> "📄"
                            else -> "♻️"
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = icono, fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${tipo.replaceFirstChar { it.uppercase() }} x${materiales.size}",
                                    fontSize = 14.sp,
                                    color = Color(0xFF2C3E50)
                                )
                            }
                            Text(
                                text = "+$totalPuntos 🪙",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFFF8F00)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Total: ${sesion.gramosAcumulados}g",
                            fontSize = 14.sp,
                            color = Color(0xFF7F8C8D)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Botón finalizar sesión
        Button(
            onClick = onFinalizarSesion,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFE53935)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Stop,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Finalizar Sesión",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SesionFinalizadaScreen(
    sesion: SesionActiva,
    onCerrar: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icono de éxito
        Surface(
            modifier = Modifier.size(100.dp),
            shape = CircleShape,
            color = Color(0xFF4CAF50)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(60.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "¡Sesión Completada!",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2C3E50)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Resumen
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // BioCoins ganados
                Text(text = "🪙", fontSize = 48.sp)
                Text(
                    text = "+${sesion.puntosAcumulados}",
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF8F00)
                )
                Text(
                    text = "BioCoins ganados",
                    fontSize = 16.sp,
                    color = Color(0xFF7F8C8D)
                )

                Spacer(modifier = Modifier.height(24.dp))
                Divider()
                Spacer(modifier = Modifier.height(24.dp))

                // Estadísticas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${sesion.materialesDepositados.size}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4A90E2)
                        )
                        Text(
                            text = "materiales",
                            fontSize = 14.sp,
                            color = Color(0xFF7F8C8D)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${sesion.gramosAcumulados}g",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                        Text(
                            text = "reciclados",
                            fontSize = 14.sp,
                            color = Color(0xFF7F8C8D)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Mensaje motivacional
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "🌱", fontSize = 32.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "¡Gracias por reciclar!",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                    Text(
                        text = "Cada material cuenta para un planeta más limpio",
                        fontSize = 14.sp,
                        color = Color(0xFF388E3C)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Botón cerrar
        Button(
            onClick = onCerrar,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = BioWayColors.PrimaryGreen
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Continuar",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
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
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A90E2)),
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
