package com.biowaymexico.ui.screens.bote_bioway

import android.Manifest
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import android.util.Log
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as ComposeSize
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.biowaymexico.ui.theme.BioWayColors
import com.biowaymexico.utils.WasteClassifierYOLO
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
// NFC imports
import android.app.Activity
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.os.Vibrator
import android.os.VibrationEffect
import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.biowaymexico.data.BoteSesionRepository
import com.biowaymexico.data.models.SesionActiva
import com.biowaymexico.data.models.toSesionActiva
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

// ═══════════════════════════════════════════════════════════════════════════════
// SINGLETON DE NFC - Completamente independiente de Compose
// ═══════════════════════════════════════════════════════════════════════════════
/**
 * Singleton que maneja NFC Reader Mode completamente fuera del ciclo de vida de Compose.
 * Esto evita todos los problemas de closures y recomposiciones.
 */
object BoteNfcReaderSingleton {
    private const val TAG = "BoteNfcSingleton"

    // Constantes APDU
    private val SELECT_APDU = byteArrayOf(
        0x00.toByte(), 0xA4.toByte(), 0x04.toByte(), 0x00.toByte(),
        0x07.toByte(), 0xF0.toByte(), 0x01, 0x02, 0x03, 0x04, 0x05, 0x06,
        0x00.toByte()
    )

    private val GET_USER_ID_APDU = byteArrayOf(
        0x00.toByte(), 0xCA.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()
    )

    // Estado
    private var nfcAdapter: NfcAdapter? = null
    private var isEnabled = false
    private var registrationCount = 0

    // Callback para notificar detecciones - se actualiza desde Compose
    var onUserIdDetected: ((String) -> Unit)? = null
    var onVibrate: (() -> Unit)? = null

    // El callback de NFC - definido como propiedad del objeto
    private val readerCallback = NfcAdapter.ReaderCallback { tag ->
        Log.d(TAG, "========================================")
        Log.d(TAG, "🔵🔵🔵 TAG DETECTADO EN SINGLETON 🔵🔵🔵")
        Log.d(TAG, "========================================")
        Log.d(TAG, "Tag ID: ${tag.id.joinToString(":")}")
        Log.d(TAG, "Tecnologías: ${tag.techList.joinToString(", ")}")

        // Procesar en un hilo IO
        Thread {
            try {
                val userId = readUserIdFromTag(tag)
                if (userId != null) {
                    Log.d(TAG, "✅ USER ID DETECTADO: $userId")
                    // Notificar en el hilo principal
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        onUserIdDetected?.invoke(userId)
                        onVibrate?.invoke()
                    }
                } else {
                    Log.w(TAG, "❌ No se pudo extraer User ID")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error procesando tag: ${e.message}", e)
            }
        }.start()
    }

    private fun readUserIdFromTag(tag: Tag): String? {
        Log.d(TAG, "=== Leyendo User ID ===")

        val isoDep = IsoDep.get(tag)
        if (isoDep == null) {
            Log.w(TAG, "❌ Tag no soporta IsoDep")
            return null
        }

        return try {
            isoDep.connect()
            isoDep.timeout = 10000
            Log.d(TAG, "✅ IsoDep conectado")

            // SELECT AID
            Log.d(TAG, "📤 SELECT AID...")
            val selectResponse = isoDep.transceive(SELECT_APDU)
            Log.d(TAG, "📥 Respuesta: ${selectResponse.joinToString(" ") { "%02X".format(it) }}")

            if (selectResponse.size < 2 ||
                selectResponse[selectResponse.size - 2] != 0x90.toByte() ||
                selectResponse[selectResponse.size - 1] != 0x00.toByte()) {
                Log.e(TAG, "❌ SELECT falló")
                isoDep.close()
                return null
            }

            // GET USER ID
            Log.d(TAG, "📤 GET USER ID...")
            val userIdResponse = isoDep.transceive(GET_USER_ID_APDU)
            Log.d(TAG, "📥 Respuesta: ${userIdResponse.joinToString(" ") { "%02X".format(it) }}")

            if (userIdResponse.size < 2) {
                isoDep.close()
                return null
            }

            val userIdBytes = userIdResponse.copyOfRange(0, userIdResponse.size - 2)
            val statusBytes = userIdResponse.copyOfRange(userIdResponse.size - 2, userIdResponse.size)

            if (statusBytes[0] != 0x90.toByte() || statusBytes[1] != 0x00.toByte()) {
                isoDep.close()
                return null
            }

            val userId = String(userIdBytes, Charsets.UTF_8)
            isoDep.close()
            Log.d(TAG, "✅ User ID: '$userId'")
            userId

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error: ${e.message}")
            try { isoDep.close() } catch (_: Exception) {}
            null
        }
    }

    fun initialize(context: Context) {
        Log.d(TAG, "========================================")
        Log.d(TAG, "=== INICIALIZANDO NFC SINGLETON ===")
        Log.d(TAG, "========================================")

        nfcAdapter = NfcAdapter.getDefaultAdapter(context)
        if (nfcAdapter == null) {
            Log.w(TAG, "❌ NFC no soportado")
            return
        }

        isEnabled = nfcAdapter!!.isEnabled
        Log.d(TAG, "NFC: ${if (isEnabled) "Habilitado ✅" else "Deshabilitado ❌"}")
    }

    fun startReaderMode(activity: Activity) {
        Log.d(TAG, "📡 startReaderMode() llamado")
        Log.d(TAG, "   activity: ${activity::class.simpleName}")
        Log.d(TAG, "   adapter: $nfcAdapter")
        Log.d(TAG, "   isEnabled: $isEnabled")
        Log.d(TAG, "   readerCallback: $readerCallback")

        if (nfcAdapter == null || !isEnabled) {
            Log.w(TAG, "⚠️ No se puede iniciar: adapter=$nfcAdapter, enabled=$isEnabled")
            return
        }

        try {
            val options = Bundle().apply {
                putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 5000)
            }

            val flags = NfcAdapter.FLAG_READER_NFC_A or
                    NfcAdapter.FLAG_READER_NFC_B or
                    NfcAdapter.FLAG_READER_NFC_F or
                    NfcAdapter.FLAG_READER_NFC_V or
                    NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK or
                    NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS

            Log.d(TAG, "   Flags: $flags (0x${flags.toString(16)})")

            nfcAdapter!!.enableReaderMode(
                activity,
                readerCallback,
                flags,
                options
            )

            registrationCount++
            Log.d(TAG, "✅ Reader mode ACTIVO (count: $registrationCount)")
            Log.d(TAG, "   Esperando tags... (callback está listo para detectar)")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error: ${e.message}")
        }
    }

    fun stopReaderMode(activity: Activity) {
        try {
            nfcAdapter?.disableReaderMode(activity)
            Log.d(TAG, "⏹️ Reader mode detenido")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al detener: ${e.message}")
        }
    }

    fun isNfcEnabled() = isEnabled
    fun getRegistrationCount() = registrationCount
}

// Constantes de tiempo
private const val COOLDOWN_DURATION_MS = 3000L  // 3 segundos después de enviar a ESP32
private const val INITIAL_DETECTION_MS = 2000L  // 2 segundos de detección inicial antes de zoom
private const val ZOOM_CONFIRMATION_MS = 3000L  // 3 segundos de confirmación con zoom

// Constantes de zoom
private const val MIN_BASE_ZOOM = 1.0f   // Sin zoom base
private const val MAX_BASE_ZOOM = 4.0f   // Máximo zoom base 4x
private const val DEFAULT_BASE_ZOOM = 1.0f

// Estados del sistema de detección con zoom adaptativo
enum class DetectionPhase {
    SCANNING,           // Escaneando, buscando objetos (sin zoom)
    LOCKING_ON,         // Objeto detectado, esperando 2s para hacer zoom
    ZOOMED_CONFIRMING,  // Zoom aplicado, confirmando material por 3s
    CONFIRMED,          // Material confirmado, enviando a ESP32
    COOLDOWN            // Esperando antes de siguiente detección
}

// Estado de la pantalla principal
enum class ScreenState {
    ZOOM_CONFIG,        // Configurando zoom base inicial
    DETECTING           // Detectando con el sistema de zoom
}

/**
 * Pantalla de clasificación para BoteBioWay
 * - Detección en tiempo real con YOLOv8 + TTA
 * - Sistema de votación para confirmar material
 * - Comunicación con ESP32 vía Bluetooth
 * - Lectura de brindador via NFC para otorgar puntos
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ClasificadorBoteScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity  // Necesario para NFC
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    // ═══════════════════════════════════════
    // NFC VIA SINGLETON (independiente de Compose)
    // ═══════════════════════════════════════
    // Solo necesitamos un estado para mostrar el contador en la UI
    var nfcRegistrationCount by remember { mutableStateOf(0) }

    // ═══════════════════════════════════════
    // ESTADO DE PANTALLA
    // ═══════════════════════════════════════
    var screenState by remember { mutableStateOf(ScreenState.ZOOM_CONFIG) }

    // ═══════════════════════════════════════
    // CONFIGURACIÓN DE ZOOM BASE
    // ═══════════════════════════════════════
    // Zoom base configurado por el usuario (1x a 4x)
    // Este zoom se aplica ANTES del zoom adaptativo del tracking
    var baseZoomLevel by remember { mutableStateOf(DEFAULT_BASE_ZOOM) }

    // Rotación del área de recorte (0, 90, 180, 270 grados)
    var cropRotation by remember { mutableStateOf(0) }

    // ═══════════════════════════════════════
    // MODO LARGA DISTANCIA
    // ═══════════════════════════════════════
    // Habilita detección multi-escala + umbral de confianza más bajo
    // para mejorar reconocimiento de objetos lejanos
    // ACTIVADO POR DEFECTO para mejor experiencia
    var longDistanceMode by remember { mutableStateOf(true) }

    // Región de zoom base (calculada a partir de baseZoomLevel)
    // Representa el área central de la imagen que se usa como "nueva área completa"
    var baseZoomRegion by remember { mutableStateOf<RectF?>(null) }

    // ═══════════════════════════════════════
    // ESTADOS DE DETECCIÓN
    // ═══════════════════════════════════════
    var currentDetections by remember { mutableStateOf<List<WasteClassifierYOLO.Detection>>(emptyList()) }
    var isClassifierReady by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // ═══════════════════════════════════════
    // SISTEMA DE ZOOM ADAPTATIVO
    // ═══════════════════════════════════════
    var detectionPhase by remember { mutableStateOf(DetectionPhase.SCANNING) }
    var phaseStartTime by remember { mutableStateOf(0L) }

    // Objeto detectado durante LOCKING_ON (para comparar consistencia)
    var lockedMaterial by remember { mutableStateOf<String?>(null) }
    var lockedBoundingBox by remember { mutableStateOf<RectF?>(null) }
    var lockedDetectionCount by remember { mutableStateOf(0) }

    // Zoom region adaptativo - coordenadas normalizadas (0-1) del área a recortar
    // IMPORTANTE: Este zoom se aplica SOBRE el baseZoomRegion
    var adaptiveZoomRegion by remember { mutableStateOf<RectF?>(null) }

    // Zoom region final combinado (base + adaptativo)
    val effectiveZoomRegion: RectF? = remember(baseZoomRegion, adaptiveZoomRegion) {
        when {
            // Solo zoom base activo
            baseZoomRegion != null && adaptiveZoomRegion == null -> baseZoomRegion
            // Zoom base + adaptativo: combinar
            baseZoomRegion != null && adaptiveZoomRegion != null -> {
                // El adaptiveZoomRegion es relativo al baseZoomRegion
                // Convertir a coordenadas absolutas
                RectF(
                    baseZoomRegion!!.left + adaptiveZoomRegion!!.left * baseZoomRegion!!.width(),
                    baseZoomRegion!!.top + adaptiveZoomRegion!!.top * baseZoomRegion!!.height(),
                    baseZoomRegion!!.left + adaptiveZoomRegion!!.right * baseZoomRegion!!.width(),
                    baseZoomRegion!!.top + adaptiveZoomRegion!!.bottom * baseZoomRegion!!.height()
                )
            }
            // Solo zoom adaptativo (sin base)
            adaptiveZoomRegion != null -> adaptiveZoomRegion
            // Sin zoom
            else -> null
        }
    }

    // Contadores de confirmación con zoom
    var zoomConfirmationVotes by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }

    // Material confirmado
    var confirmedMaterial by remember { mutableStateOf<String?>(null) }
    var confirmedConfidence by remember { mutableStateOf(0f) }

    // Cooldown timestamp
    var cooldownStartTime by remember { mutableStateOf(0L) }

    // Función para calcular la región de zoom base desde el nivel de zoom
    fun calculateBaseZoomRegion(zoomLevel: Float): RectF? {
        if (zoomLevel <= 1.0f) return null // Sin zoom base

        // El zoom es centrado: si zoom = 2x, usamos el 50% central
        // zoom = 1x → 100% del frame (null)
        // zoom = 2x → 50% central
        // zoom = 4x → 25% central
        val regionSize = 1.0f / zoomLevel
        val offset = (1.0f - regionSize) / 2.0f

        return RectF(offset, offset, offset + regionSize, offset + regionSize)
    }

    // ═══════════════════════════════════════
    // CLASIFICADOR YOLO
    // ═══════════════════════════════════════
    val classifier = remember { WasteClassifierYOLO(context) }

    // ═══════════════════════════════════════
    // CAMERAX
    // ═══════════════════════════════════════
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var cameraProvider: ProcessCameraProvider? by remember { mutableStateOf(null) }

    // ═══════════════════════════════════════
    // BLUETOOTH / ESP32
    // ═══════════════════════════════════════
    val bluetoothManager = remember { com.biowaymexico.utils.BluetoothManager() }
    var bluetoothConectado by remember { mutableStateOf(false) }
    var enviandoAESP32 by remember { mutableStateOf(false) }
    var estadoConexion by remember { mutableStateOf("Conectando a ESP32...") }

    // ═══════════════════════════════════════
    // NFC - ESCÁNER DE BRINDADOR (usando componente independiente)
    // ═══════════════════════════════════════
    var brindadorId by remember { mutableStateOf<String?>(null) }
    var brindadorNombre by remember { mutableStateOf<String?>(null) }

    // Repositorio para otorgar puntos
    val boteSesionRepository = remember { BoteSesionRepository() }
    val firestore = remember { FirebaseFirestore.getInstance() }

    // ═══════════════════════════════════════
    // ESTADO DE SESIÓN ACTIVA
    // ═══════════════════════════════════════
    var sesionActiva by remember { mutableStateOf<SesionActiva?>(null) }
    var sesionListener by remember { mutableStateOf<ListenerRegistration?>(null) }

    // Timers de sesión
    var tiempoInicioSesion by remember { mutableStateOf(0L) }
    var tiempoUltimaActividad by remember { mutableStateOf(0L) }
    val TIEMPO_MAXIMO_SESION_MS = 180_000L  // 3 minutos
    val TIEMPO_INACTIVIDAD_MS = 45_000L     // 45 segundos

    // Contadores de sesión (actualizados desde sesionActiva)
    var materialesOtorgadosEnSesion by remember { mutableStateOf(0) }
    var puntosOtorgadosEnSesion by remember { mutableStateOf(0) }
    var gramosOtorgadosEnSesion by remember { mutableStateOf(0.0) }

    // Función para finalizar la sesión actual
    fun finalizarSesionActual(razon: String) {
        brindadorId?.let { bId ->
            scope.launch {
                Log.d("ClasificadorBote", "🏁 Finalizando sesión: $razon")
                boteSesionRepository.finalizarSesion(bId, razon)

                // Limpiar estado local
                sesionListener?.remove()
                sesionListener = null
                sesionActiva = null
                brindadorId = null
                brindadorNombre = null
                tiempoInicioSesion = 0L
                tiempoUltimaActividad = 0L
                materialesOtorgadosEnSesion = 0
                puntosOtorgadosEnSesion = 0
                gramosOtorgadosEnSesion = 0.0
            }
        }
    }

    // Función para iniciar listener de sesión
    fun iniciarListenerSesion(userId: String) {
        // Remover listener anterior si existe
        sesionListener?.remove()

        // Crear nuevo listener
        sesionListener = firestore.collection("Brindador")
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ClasificadorBote", "❌ Error en listener: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    @Suppress("UNCHECKED_CAST")
                    val sesionData = snapshot.get("sesionActiva") as? Map<String, Any?>

                    if (sesionData != null) {
                        val sesion = sesionData.toSesionActiva()

                        // Verificar si el brindador finalizó la sesión
                        if (sesion.estado != SesionActiva.ESTADO_ACTIVA) {
                            Log.d("ClasificadorBote", "🔔 Brindador finalizó la sesión: ${sesion.estado}")
                            finalizarSesionActual(sesion.estado)
                        } else {
                            // Actualizar estado local con datos de Firestore
                            sesionActiva = sesion
                            materialesOtorgadosEnSesion = sesion.materialesDepositados.size
                            puntosOtorgadosEnSesion = sesion.puntosAcumulados
                            gramosOtorgadosEnSesion = sesion.gramosAcumulados.toDouble()
                        }
                    } else {
                        // Sesión eliminada
                        if (sesionActiva != null) {
                            Log.d("ClasificadorBote", "🔔 Sesión eliminada de Firestore")
                            sesionActiva = null
                            brindadorId = null
                            brindadorNombre = null
                        }
                    }
                }
            }
    }

    // Callback cuando NFC detecta un usuario - se procesa aquí
    val onNfcUserDetected: (String) -> Unit = { userId ->
        Log.d("ClasificadorBote", "═══════════════════════════════════════")
        Log.d("ClasificadorBote", "📱 NFC: USUARIO DETECTADO via Singleton")
        Log.d("ClasificadorBote", "   UID: $userId")
        Log.d("ClasificadorBote", "   Longitud: ${userId.length}")
        Log.d("ClasificadorBote", "═══════════════════════════════════════")

        if (userId.length >= 20) {
            // Verificar si ya hay una sesión activa localmente
            if (sesionActiva != null) {
                Log.w("ClasificadorBote", "⚠️ Ya hay sesión activa, ignorando NFC")
                Log.d("ClasificadorBote", "   Sesión actual: ${brindadorNombre}")
            } else {
                scope.launch {
                    // Verificar que no haya sesión activa en Firestore
                    val sesionExistente = boteSesionRepository.verificarSesionActiva(userId).getOrNull()
                    if (sesionExistente != null) {
                        Log.w("ClasificadorBote", "⚠️ Usuario ya tiene sesión activa en otro bote")
                    } else {
                        // Obtener datos del brindador
                        val result = boteSesionRepository.obtenerBrindadorPorId(userId)
                        result.fold(
                            onSuccess = { brindador ->
                                // Iniciar sesión en Firestore
                                val sesionResult = boteSesionRepository.iniciarSesion(
                                    brindadorId = userId,
                                    boteNombre = "Bote BioWay"  // TODO: Obtener nombre real del bote
                                )

                                sesionResult.fold(
                                    onSuccess = { nuevaSesion ->
                                        // Actualizar estado local
                                        brindadorId = userId
                                        brindadorNombre = brindador.nombre
                                        sesionActiva = nuevaSesion
                                        tiempoInicioSesion = System.currentTimeMillis()
                                        tiempoUltimaActividad = System.currentTimeMillis()
                                        materialesOtorgadosEnSesion = 0
                                        puntosOtorgadosEnSesion = 0
                                        gramosOtorgadosEnSesion = 0.0

                                        // Iniciar listener para detectar cambios (ej: brindador finaliza)
                                        iniciarListenerSesion(userId)

                                        Log.d("ClasificadorBote", "   ✅ SESIÓN INICIADA")
                                        Log.d("ClasificadorBote", "   Nombre: ${brindador.nombre}")
                                        Log.d("ClasificadorBote", "   BioCoins actuales: ${brindador.bioCoins}")
                                        Log.d("ClasificadorBote", "═══════════════════════════════════════")
                                    },
                                    onFailure = { error ->
                                        Log.e("ClasificadorBote", "❌ Error iniciando sesión: ${error.message}")
                                    }
                                )
                            },
                            onFailure = { error ->
                                Log.e("ClasificadorBote", "❌ NFC Error al buscar brindador: ${error.message}")
                            }
                        )
                    }
                }
            }
        } else {
            Log.w("ClasificadorBote", "⚠️ NFC: ID inválido (muy corto) - $userId")
        }
    }

    // ═══════════════════════════════════════
    // PERMISO DE CÁMARA
    // ═══════════════════════════════════════
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    // ═══════════════════════════════════════
    // INICIALIZACIÓN
    // ═══════════════════════════════════════

    // Inicializar clasificador con modo larga distancia activado por defecto
    LaunchedEffect(Unit) {
        try {
            classifier.initialize()
            // Activar modo multi-escala por defecto para mejor detección a distancia
            classifier.multiScaleMode = true
            classifier.confidenceThreshold = WasteClassifierYOLO.CONFIDENCE_THRESHOLD_DISTANT
            isClassifierReady = true
            Log.d("ClasificadorBote", "✅ Clasificador YOLOv8 inicializado (modo larga distancia activo)")
        } catch (e: Exception) {
            errorMessage = "Error al inicializar clasificador: ${e.message}"
            Log.e("ClasificadorBote", "❌ Error inicializando clasificador", e)
        }
    }

    // Conectar a ESP32
    LaunchedEffect(Unit) {
        estadoConexion = "Buscando ESP32_Detector..."
        val result = bluetoothManager.conectarConHandshake()
        result.fold(
            onSuccess = {
                bluetoothConectado = true
                estadoConexion = "ESP32 conectado"
                Log.d("ClasificadorBote", "✅ ESP32 conectado")
            },
            onFailure = { error ->
                bluetoothConectado = false
                estadoConexion = "Error: ${error.message}"
                Log.e("ClasificadorBote", "❌ Error conectando ESP32: ${error.message}")
            }
        )
    }

    // Limpiar recursos
    DisposableEffect(Unit) {
        onDispose {
            classifier.close()
            cameraExecutor.shutdown()
            cameraProvider?.unbindAll()
            bluetoothManager.desconectar()
            // Limpiar listener de sesión
            sesionListener?.remove()
        }
    }

    // Timer de verificación de sesión (timeout e inactividad)
    LaunchedEffect(sesionActiva) {
        if (sesionActiva == null) return@LaunchedEffect

        while (sesionActiva != null) {
            kotlinx.coroutines.delay(1000)  // Verificar cada segundo

            val ahora = System.currentTimeMillis()

            // Verificar timeout máximo (3 minutos)
            if (tiempoInicioSesion > 0 && (ahora - tiempoInicioSesion) >= TIEMPO_MAXIMO_SESION_MS) {
                Log.d("ClasificadorBote", "⏱️ Timeout máximo alcanzado (3 min)")
                finalizarSesionActual(SesionActiva.ESTADO_FINALIZADA_TIMEOUT)
                break
            }

            // Verificar inactividad (45 segundos sin material)
            if (tiempoUltimaActividad > 0 && (ahora - tiempoUltimaActividad) >= TIEMPO_INACTIVIDAD_MS) {
                Log.d("ClasificadorBote", "⏱️ Inactividad detectada (45s)")
                finalizarSesionActual(SesionActiva.ESTADO_FINALIZADA_INACTIVIDAD)
                break
            }
        }
    }

    // ═══════════════════════════════════════
    // LÓGICA DE ZOOM ADAPTATIVO Y DETECCIÓN
    // ═══════════════════════════════════════

    // Función para resetear el sistema de detección
    fun resetDetectionSystem() {
        detectionPhase = DetectionPhase.SCANNING
        phaseStartTime = 0L
        lockedMaterial = null
        lockedBoundingBox = null
        lockedDetectionCount = 0
        adaptiveZoomRegion = null  // Solo resetear zoom adaptativo, NO el base
        zoomConfirmationVotes = emptyMap()
        confirmedMaterial = null
        confirmedConfidence = 0f
        Log.d("ClasificadorBote", "🔄 Sistema de detección reseteado → SCANNING (zoom base: ${baseZoomLevel}x)")
    }

    // Función para calcular la región de zoom con padding
    fun calculateZoomRegion(boundingBox: RectF, padding: Float = 0.15f): RectF {
        val width = boundingBox.width()
        val height = boundingBox.height()
        val paddingX = width * padding
        val paddingY = height * padding

        return RectF(
            (boundingBox.left - paddingX).coerceIn(0f, 1f),
            (boundingBox.top - paddingY).coerceIn(0f, 1f),
            (boundingBox.right + paddingX).coerceIn(0f, 1f),
            (boundingBox.bottom + paddingY).coerceIn(0f, 1f)
        )
    }

    // Máquina de estados para el sistema de detección con zoom
    LaunchedEffect(currentDetections, detectionPhase) {
        if (enviandoAESP32) return@LaunchedEffect

        val topDetection = currentDetections.maxByOrNull { it.confidence }
        val currentTime = System.currentTimeMillis()

        when (detectionPhase) {
            DetectionPhase.SCANNING -> {
                // Buscando objetos en el frame completo
                if (topDetection != null && topDetection.confidence > 0.4f) {
                    // Objeto detectado, iniciar fase de bloqueo
                    detectionPhase = DetectionPhase.LOCKING_ON
                    phaseStartTime = currentTime
                    lockedMaterial = topDetection.className.lowercase()
                    lockedBoundingBox = topDetection.boundingBox
                    lockedDetectionCount = 1

                    Log.d("ClasificadorBote", "═══════════════════════════════════════")
                    Log.d("ClasificadorBote", "🎯 OBJETO DETECTADO: ${topDetection.className}")
                    Log.d("ClasificadorBote", "   Confianza: ${(topDetection.confidence * 100).toInt()}%")
                    Log.d("ClasificadorBote", "   BBox: ${topDetection.boundingBox}")
                    Log.d("ClasificadorBote", "   → Iniciando LOCKING_ON (2s)")
                    Log.d("ClasificadorBote", "═══════════════════════════════════════")
                }
            }

            DetectionPhase.LOCKING_ON -> {
                // Esperando 2 segundos con detección consistente antes de zoom
                val elapsed = currentTime - phaseStartTime

                if (topDetection != null && topDetection.confidence > 0.4f) {
                    val detectedMaterial = topDetection.className.lowercase()

                    // Verificar si es el mismo material (o similar)
                    if (detectedMaterial == lockedMaterial) {
                        lockedDetectionCount++
                        // Actualizar bounding box con promedio móvil para estabilidad
                        lockedBoundingBox?.let { current ->
                            val alpha = 0.3f // Factor de suavizado
                            lockedBoundingBox = RectF(
                                current.left + alpha * (topDetection.boundingBox.left - current.left),
                                current.top + alpha * (topDetection.boundingBox.top - current.top),
                                current.right + alpha * (topDetection.boundingBox.right - current.right),
                                current.bottom + alpha * (topDetection.boundingBox.bottom - current.bottom)
                            )
                        }

                        // ¿Pasaron 2 segundos?
                        if (elapsed >= INITIAL_DETECTION_MS) {
                            // Calcular región de zoom ADAPTATIVO y activar
                            lockedBoundingBox?.let { bbox ->
                                adaptiveZoomRegion = calculateZoomRegion(bbox, padding = 0.2f)
                                detectionPhase = DetectionPhase.ZOOMED_CONFIRMING
                                phaseStartTime = currentTime
                                zoomConfirmationVotes = mapOf(lockedMaterial!! to 1)

                                Log.d("ClasificadorBote", "═══════════════════════════════════════")
                                Log.d("ClasificadorBote", "🔍 ZOOM ADAPTATIVO ACTIVADO")
                                Log.d("ClasificadorBote", "   Material: $lockedMaterial")
                                Log.d("ClasificadorBote", "   Detecciones consistentes: $lockedDetectionCount")
                                Log.d("ClasificadorBote", "   Zoom base: ${baseZoomLevel}x")
                                Log.d("ClasificadorBote", "   Zoom adaptativo: $adaptiveZoomRegion")
                                Log.d("ClasificadorBote", "   Zoom efectivo: $effectiveZoomRegion")
                                Log.d("ClasificadorBote", "   → Iniciando ZOOMED_CONFIRMING (3s)")
                                Log.d("ClasificadorBote", "═══════════════════════════════════════")

                                // Vibrar para indicar zoom activado
                                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                                vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                            }
                        }
                    } else {
                        // Material diferente detectado, reiniciar con nuevo material
                        Log.d("ClasificadorBote", "⚠️ Material cambió: $lockedMaterial → $detectedMaterial")
                        lockedMaterial = detectedMaterial
                        lockedBoundingBox = topDetection.boundingBox
                        lockedDetectionCount = 1
                        phaseStartTime = currentTime
                    }
                } else {
                    // Objeto perdido durante bloqueo
                    if (elapsed > 500) { // 500ms de gracia
                        Log.d("ClasificadorBote", "⚠️ Objeto perdido durante LOCKING_ON → SCANNING")
                        resetDetectionSystem()
                    }
                }
            }

            DetectionPhase.ZOOMED_CONFIRMING -> {
                // Confirmando material con zoom durante 3 segundos
                val elapsed = currentTime - phaseStartTime

                if (topDetection != null && topDetection.confidence > 0.5f) {
                    val detectedMaterial = topDetection.className.lowercase()

                    // Agregar voto
                    val newVotes = zoomConfirmationVotes.toMutableMap()
                    newVotes[detectedMaterial] = (newVotes[detectedMaterial] ?: 0) + 1
                    zoomConfirmationVotes = newVotes

                    // ¿Pasaron 3 segundos?
                    if (elapsed >= ZOOM_CONFIRMATION_MS) {
                        // Verificar si el material dominante coincide con el bloqueado
                        val winner = zoomConfirmationVotes.maxByOrNull { it.value }
                        val totalVotes = zoomConfirmationVotes.values.sum()
                        val winnerPercentage = if (totalVotes > 0) (winner?.value ?: 0) * 100 / totalVotes else 0

                        Log.d("ClasificadorBote", "📊 Resultados de confirmación:")
                        zoomConfirmationVotes.forEach { (mat, votes) ->
                            Log.d("ClasificadorBote", "   $mat: $votes votos")
                        }

                        if (winner != null && winner.key == lockedMaterial && winnerPercentage >= 60) {
                            // ¡Material confirmado!
                            confirmedMaterial = winner.key
                            confirmedConfidence = topDetection.confidence
                            detectionPhase = DetectionPhase.CONFIRMED

                            Log.d("ClasificadorBote", "═══════════════════════════════════════")
                            Log.d("ClasificadorBote", "✅ MATERIAL CONFIRMADO CON ZOOM: ${winner.key}")
                            Log.d("ClasificadorBote", "   Votos: ${winner.value}/$totalVotes ($winnerPercentage%)")
                            Log.d("ClasificadorBote", "   Confianza final: ${(confirmedConfidence * 100).toInt()}%")
                            Log.d("ClasificadorBote", "═══════════════════════════════════════")

                            // Vibrar confirmación
                            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                            vibrator?.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))

                            // Enviar a ESP32 - ejecutar secuencia de depósito
                            if (bluetoothConectado) {
                                enviandoAESP32 = true

                                scope.launch(Dispatchers.IO) {
                                    Log.d("ClasificadorBote", "📤 Ejecutando depósito para: ${winner.key}")
                                    val sendResult = bluetoothManager.enviarMaterial(winner.key)

                                    withContext(Dispatchers.Main) {
                                        enviandoAESP32 = false

                                        if (sendResult.isSuccess) {
                                            Log.d("ClasificadorBote", "✅ Depósito completado correctamente")

                                            // Otorgar puntos al brindador (solo si hay sesión activa)
                                            if (sesionActiva != null && brindadorId != null) {
                                                scope.launch(Dispatchers.IO) {
                                                    val puntosResult = boteSesionRepository.agregarMaterialASesion(
                                                        brindadorId = brindadorId!!,
                                                        tipoMaterial = winner.key,
                                                        confianza = confirmedConfidence
                                                    )
                                                    withContext(Dispatchers.Main) {
                                                        puntosResult.fold(
                                                            onSuccess = { otorgamiento ->
                                                                // Actualizar tiempo de última actividad
                                                                tiempoUltimaActividad = System.currentTimeMillis()
                                                                // Los contadores se actualizan vía el listener de Firestore
                                                                Log.d("ClasificadorBote", "🎁 Material agregado a sesión: +${otorgamiento.puntosOtorgados} puntos")
                                                            },
                                                            onFailure = { error ->
                                                                Log.e("ClasificadorBote", "❌ Error agregando material: ${error.message}")
                                                            }
                                                        )
                                                    }
                                                }
                                            } else {
                                                Log.w("ClasificadorBote", "⚠️ No hay sesión activa, material no registrado")
                                            }

                                            // Iniciar cooldown
                                            detectionPhase = DetectionPhase.COOLDOWN
                                            cooldownStartTime = System.currentTimeMillis()
                                        } else {
                                            Log.e("ClasificadorBote", "❌ Error en depósito ESP32, reseteando")
                                            resetDetectionSystem()
                                        }
                                    }
                                }
                            } else {
                                // Sin ESP32, solo cooldown
                                detectionPhase = DetectionPhase.COOLDOWN
                                cooldownStartTime = System.currentTimeMillis()
                            }
                        } else {
                            // Material no confirmado consistentemente, reiniciar
                            Log.d("ClasificadorBote", "⚠️ Material no confirmado (${winnerPercentage}% < 60%), reseteando")
                            resetDetectionSystem()
                        }
                    }
                } else {
                    // Objeto perdido durante confirmación con zoom
                    if (elapsed > 1000) { // 1 segundo de gracia
                        Log.d("ClasificadorBote", "⚠️ Objeto perdido durante ZOOMED_CONFIRMING → SCANNING")
                        resetDetectionSystem()
                    }
                }
            }

            DetectionPhase.CONFIRMED -> {
                // Esperando que se envíe a ESP32
                // La transición a COOLDOWN se hace en el callback de ESP32
            }

            DetectionPhase.COOLDOWN -> {
                // Esperando antes de siguiente detección
                val elapsed = currentTime - cooldownStartTime
                if (elapsed >= COOLDOWN_DURATION_MS) {
                    Log.d("ClasificadorBote", "✅ Cooldown terminado → SCANNING")
                    resetDetectionSystem()
                }
            }
        }
    }

    // ═══════════════════════════════════════
    // NFC VIA SINGLETON (completamente independiente de Compose)
    // ═══════════════════════════════════════

    // Configurar callbacks del singleton (se actualizan en cada recomposición)
    // El singleton mantiene su propio callback estático que llama a estos
    LaunchedEffect(Unit) {
        // Configurar el callback de detección
        BoteNfcReaderSingleton.onUserIdDetected = { userId ->
            Log.d("ClasificadorBote", "📲 SINGLETON detectó usuario: $userId")
            onNfcUserDetected(userId)
        }

        // Configurar vibración
        BoteNfcReaderSingleton.onVibrate = {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
        }

        // Inicializar el singleton
        BoteNfcReaderSingleton.initialize(context)

        // Iniciar reader mode
        activity?.let { act ->
            BoteNfcReaderSingleton.startReaderMode(act)
            nfcRegistrationCount = BoteNfcReaderSingleton.getRegistrationCount()
        }
    }

    // Heartbeat: Actualizar contador y re-registrar cada 3 segundos
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(3000)
            activity?.let { act ->
                Log.d("ClasificadorBote", "🔄 [SINGLETON HEARTBEAT] Re-registrando NFC...")
                BoteNfcReaderSingleton.startReaderMode(act)
                nfcRegistrationCount = BoteNfcReaderSingleton.getRegistrationCount()
            }
        }
    }

    // Lifecycle observer para NFC singleton
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    Log.d("ClasificadorBote", "📱 ON_RESUME - activando NFC singleton...")
                    activity?.let { act ->
                        BoteNfcReaderSingleton.startReaderMode(act)
                        nfcRegistrationCount = BoteNfcReaderSingleton.getRegistrationCount()
                    }
                }
                Lifecycle.Event.ON_STOP -> {
                    Log.d("ClasificadorBote", "🛑 ON_STOP - deteniendo NFC singleton")
                    activity?.let { act ->
                        BoteNfcReaderSingleton.stopReaderMode(act)
                    }
                }
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            Log.d("ClasificadorBote", "🗑️ NFC SINGLETON DISPOSED")
            lifecycleOwner.lifecycle.removeObserver(observer)
            activity?.let { act ->
                BoteNfcReaderSingleton.stopReaderMode(act)
            }
            // Limpiar callbacks para evitar memory leaks
            BoteNfcReaderSingleton.onUserIdDetected = null
            BoteNfcReaderSingleton.onVibrate = null
        }
    }

    // ═══════════════════════════════════════
    // UI
    // ═══════════════════════════════════════

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when {
            !cameraPermissionState.status.isGranted -> {
                PermissionRequiredScreen(
                    onRequestPermission = { cameraPermissionState.launchPermissionRequest() }
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
            !bluetoothConectado -> {
                ConexionESP32Screen(
                    estadoConexion = estadoConexion,
                    onNavigateBack = onNavigateBack
                )
            }
            screenState == ScreenState.ZOOM_CONFIG -> {
                // Pantalla de configuración de zoom base
                ZoomConfigScreen(
                    currentZoom = baseZoomLevel,
                    onZoomChange = { newZoom ->
                        baseZoomLevel = newZoom
                        baseZoomRegion = calculateBaseZoomRegion(newZoom)
                    },
                    currentRotation = cropRotation,
                    onRotationChange = { newRotation ->
                        cropRotation = newRotation
                        Log.d("ClasificadorBote", "🔄 Rotación de recorte cambiada a: ${newRotation}°")
                    },
                    longDistanceMode = longDistanceMode,
                    onLongDistanceModeChange = { enabled ->
                        longDistanceMode = enabled
                        // Activar/desactivar modo multi-escala en el clasificador
                        classifier.multiScaleMode = enabled
                        // Ajustar umbral de confianza para modo larga distancia
                        classifier.confidenceThreshold = if (enabled) {
                            WasteClassifierYOLO.CONFIDENCE_THRESHOLD_DISTANT
                        } else {
                            WasteClassifierYOLO.CONFIDENCE_THRESHOLD
                        }
                        Log.d("ClasificadorBote", "🔭 Modo larga distancia: ${if (enabled) "ACTIVADO" else "DESACTIVADO"}")
                    },
                    onConfirm = {
                        baseZoomRegion = calculateBaseZoomRegion(baseZoomLevel)
                        screenState = ScreenState.DETECTING
                        Log.d("ClasificadorBote", "═══════════════════════════════════════")
                        Log.d("ClasificadorBote", "🔧 CONFIGURACIÓN DE ZOOM CONFIRMADA")
                        Log.d("ClasificadorBote", "   Zoom base: ${baseZoomLevel}x")
                        Log.d("ClasificadorBote", "   Región base: $baseZoomRegion")
                        Log.d("ClasificadorBote", "   Rotación recorte: ${cropRotation}°")
                        Log.d("ClasificadorBote", "   Modo larga distancia: $longDistanceMode")
                        Log.d("ClasificadorBote", "═══════════════════════════════════════")
                    },
                    onNavigateBack = onNavigateBack,
                    classifier = classifier,
                    isClassifierReady = isClassifierReady
                )
            }
            else -> {
                // Vista principal con cámara y detección
                Box(modifier = Modifier.fillMaxSize()) {
                    // Cámara con análisis y zoom combinado (base + adaptativo)
                    CameraPreviewWithZoom(
                        onCameraProviderReady = { cameraProvider = it },
                        classifier = classifier,
                        isAnalyzing = detectionPhase != DetectionPhase.COOLDOWN &&
                                     detectionPhase != DetectionPhase.CONFIRMED &&
                                     !enviandoAESP32 && isClassifierReady,
                        zoomRegion = effectiveZoomRegion,  // Usa zoom combinado
                        cropRotation = cropRotation,
                        onDetections = { detections ->
                            currentDetections = detections
                        }
                    )

                    // Canvas para bounding boxes
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawDetections(currentDetections)
                    }

                    // Barra de estado superior
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Black.copy(alpha = 0.85f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // ESP32 status
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(
                                            if (bluetoothConectado) Color(0xFF4CAF50) else Color(0xFF757575),
                                            CircleShape
                                        )
                                )
                                Text(
                                    if (enviandoAESP32) "Procesando..." else "ESP32",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Estado de detección con fases de zoom
                            when (detectionPhase) {
                                DetectionPhase.SCANNING -> {
                                    Text("🔍 Escaneando", color = Color(0xFF4CAF50), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                DetectionPhase.LOCKING_ON -> {
                                    val remaining = ((INITIAL_DETECTION_MS - (System.currentTimeMillis() - phaseStartTime)) / 1000f).coerceAtLeast(0f)
                                    Text("🎯 Bloqueando ${String.format("%.1f", remaining)}s", color = Color(0xFF2196F3), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                DetectionPhase.ZOOMED_CONFIRMING -> {
                                    val remaining = ((ZOOM_CONFIRMATION_MS - (System.currentTimeMillis() - phaseStartTime)) / 1000f).coerceAtLeast(0f)
                                    Text("🔬 ZOOM ${String.format("%.1f", remaining)}s", color = Color(0xFFE040FB), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                DetectionPhase.CONFIRMED -> {
                                    Text("✅ Confirmado", color = Color(0xFF4CAF50), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                DetectionPhase.COOLDOWN -> {
                                    val remaining = ((COOLDOWN_DURATION_MS - (System.currentTimeMillis() - cooldownStartTime)) / 1000).coerceAtLeast(0)
                                    Text("⏳ Espera ${remaining}s", color = Color(0xFFFFA726), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Barra de brindador NFC
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 80.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = if (brindadorId != null) Color(0xFF4A90E2).copy(alpha = 0.9f) else Color(0xFF424242).copy(alpha = 0.8f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Nfc, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                if (brindadorId != null) {
                                    Column {
                                        Text(brindadorNombre ?: "Usuario", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Text("$materialesOtorgadosEnSesion items • $puntosOtorgadosEnSesion pts • ${gramosOtorgadosEnSesion.toInt()}g",
                                            color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                                    }
                                } else {
                                    Text("Esperando NFC del usuario...", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                                }
                            }
                            if (brindadorId != null) {
                                IconButton(onClick = { brindadorId = null; brindadorNombre = null }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Close, "Desconectar", tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    // Resultado confirmado
                    if (confirmedMaterial != null) {
                        Surface(
                            modifier = Modifier.align(Alignment.Center).padding(32.dp),
                            shape = RoundedCornerShape(24.dp),
                            color = Color(0xFF4CAF50).copy(alpha = 0.95f)
                        ) {
                            Column(
                                modifier = Modifier.padding(40.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.size(64.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(confirmedMaterial!!.uppercase(), color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                                Text("${(confirmedConfidence * 100).toInt()}%", color = Color.White.copy(alpha = 0.9f), fontSize = 18.sp)
                            }
                        }
                    }

                    // Indicador visual de zoom activo (muestra zoom efectivo combinado)
                    if (effectiveZoomRegion != null) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawZoomIndicator(
                                zoomRegion = effectiveZoomRegion!!,
                                isBaseZoom = baseZoomRegion != null && adaptiveZoomRegion == null,
                                isAdaptiveZoom = adaptiveZoomRegion != null
                            )
                        }
                    }

                    // Detecciones en vivo con info de fase
                    AnimatedVisibility(
                        visible = currentDetections.isNotEmpty() && confirmedMaterial == null,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        DetectionResultsCardWithPhase(
                            detections = currentDetections,
                            phase = detectionPhase,
                            lockedMaterial = lockedMaterial,
                            votesMap = zoomConfirmationVotes,
                            isZoomed = adaptiveZoomRegion != null,
                            baseZoomLevel = baseZoomLevel
                        )
                    }
                }

                // Botón volver
                FloatingActionButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.padding(16.dp),
                    containerColor = Color.Black.copy(alpha = 0.7f),
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.ArrowBack, "Volver")
                }
            }
        }
    }
}

// ═══════════════════════════════════════
// COMPONENTES
// ═══════════════════════════════════════

/**
 * Componente de cámara con soporte para zoom adaptativo y rotación del área de recorte.
 * Cuando zoomRegion no es null, recorta la imagen a esa región antes de clasificar.
 * La rotación (0, 90, 180, 270) se aplica al área recortada.
 */
@Composable
private fun CameraPreviewWithZoom(
    onCameraProviderReady: (ProcessCameraProvider) -> Unit,
    classifier: WasteClassifierYOLO,
    isAnalyzing: Boolean,
    zoomRegion: RectF?,
    cropRotation: Int = 0,
    onDetections: (List<WasteClassifierYOLO.Detection>) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    // Guardar referencia al zoomRegion y rotación actual para usar en el analyzer
    val currentZoomRegion = rememberUpdatedState(zoomRegion)
    val currentCropRotation = rememberUpdatedState(cropRotation)

    // Aplicar rotación visual al preview
    // Cuando la rotación es 90 o 270, necesitamos escalar para compensar el cambio de aspecto
    val rotationModifier = when (cropRotation) {
        90, 270 -> Modifier
            .fillMaxSize()
            .graphicsLayer {
                rotationZ = cropRotation.toFloat()
                // Escalar para que llene la pantalla después de rotar
                val scale = if (size.width > 0 && size.height > 0) {
                    maxOf(size.width / size.height, size.height / size.width)
                } else 1f
                scaleX = scale
                scaleY = scale
            }
        180 -> Modifier
            .fillMaxSize()
            .graphicsLayer { rotationZ = 180f }
        else -> Modifier.fillMaxSize()
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FIT_CENTER
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                onCameraProviderReady(cameraProvider)

                val preview = Preview.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                    .build()
                    .also { it.setSurfaceProvider(previewView.surfaceProvider) }

                // Usar resolución HD para mejor reconocimiento a distancia
                // 1280x720 proporciona 4x más píxeles que 640x480
                // Esto permite detectar objetos más pequeños/lejanos con mayor precisión
                val imageAnalyzer = ImageAnalysis.Builder()
                    .setTargetResolution(Size(1280, 720))  // HD para mejor reconocimiento a distancia
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build()

                imageAnalyzer.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                    if (isAnalyzing && classifier.isReady()) {
                        processImageWithZoom(
                            imageProxy,
                            classifier,
                            scope,
                            currentZoomRegion.value,
                            currentCropRotation.value,
                            onDetections
                        )
                    } else {
                        imageProxy.close()
                    }
                }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalyzer
                    )
                } catch (e: Exception) {
                    Log.e("ClasificadorBote", "Error vinculando cámara", e)
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = rotationModifier
    )
}

private var lastProcessTime = 0L
private const val PROCESS_INTERVAL_MS = 80L  // Más rápido para mejor respuesta durante zoom

/**
 * Procesa imagen con soporte para zoom adaptativo y rotación.
 * Si zoomRegion está definida, recorta la imagen a esa región antes de clasificar.
 * La rotación (0, 90, 180, 270) se aplica al área recortada.
 */
private fun processImageWithZoom(
    imageProxy: ImageProxy,
    classifier: WasteClassifierYOLO,
    scope: CoroutineScope,
    zoomRegion: RectF?,
    cropRotation: Int = 0,
    onDetections: (List<WasteClassifierYOLO.Detection>) -> Unit
) {
    val currentTime = System.currentTimeMillis()
    if (currentTime - lastProcessTime < PROCESS_INTERVAL_MS) {
        imageProxy.close()
        return
    }
    lastProcessTime = currentTime

    scope.launch {
        try {
            val bitmap = imageProxy.toBitmap()
            val rotation = imageProxy.imageInfo.rotationDegrees
            val rotatedBitmap = if (rotation != 0) rotateBitmap(bitmap, rotation.toFloat()) else bitmap

            // Aplicar zoom si hay región definida
            val croppedBitmap = if (zoomRegion != null) {
                cropBitmapToRegion(rotatedBitmap, zoomRegion)
            } else {
                rotatedBitmap
            }

            // Aplicar rotación del área de recorte si es necesario
            // La rotación se aplica siempre que no sea 0, independiente del zoom
            val bitmapToClassify = if (cropRotation != 0) {
                rotateBitmap(croppedBitmap, cropRotation.toFloat())
            } else {
                croppedBitmap
            }

            val result = withContext(Dispatchers.Default) {
                classifier.classifyImage(bitmapToClassify)
            }

            // Si estamos en modo zoom, ajustar las coordenadas de detección
            val adjustedDetections = if (zoomRegion != null) {
                result.detections.map { detection ->
                    // Las coordenadas de la detección son relativas a la imagen recortada
                    // Necesitamos convertirlas de vuelta al espacio del frame completo
                    val adjustedBox = RectF(
                        zoomRegion.left + detection.boundingBox.left * zoomRegion.width(),
                        zoomRegion.top + detection.boundingBox.top * zoomRegion.height(),
                        zoomRegion.left + detection.boundingBox.right * zoomRegion.width(),
                        zoomRegion.top + detection.boundingBox.bottom * zoomRegion.height()
                    )
                    WasteClassifierYOLO.Detection(
                        classId = detection.classId,
                        className = detection.className,
                        confidence = detection.confidence,
                        boundingBox = adjustedBox
                    )
                }
            } else {
                result.detections
            }

            withContext(Dispatchers.Main) {
                onDetections(adjustedDetections)
            }

            // Log para debug del zoom
            if (zoomRegion != null && result.detections.isNotEmpty()) {
                Log.d("ClasificadorBote", "🔬 ZOOM: Detectado ${result.detections.first().className} " +
                    "con ${(result.detections.first().confidence * 100).toInt()}% en región recortada")
            }
        } catch (e: Exception) {
            Log.e("ClasificadorBote", "Error procesando imagen", e)
        } finally {
            imageProxy.close()
        }
    }
}

/**
 * Recorta un bitmap a una región específica (coordenadas normalizadas 0-1)
 */
private fun cropBitmapToRegion(bitmap: Bitmap, region: RectF): Bitmap {
    val x = (region.left * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
    val y = (region.top * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
    val width = (region.width() * bitmap.width).toInt().coerceIn(1, bitmap.width - x)
    val height = (region.height() * bitmap.height).toInt().coerceIn(1, bitmap.height - y)

    return try {
        Bitmap.createBitmap(bitmap, x, y, width, height)
    } catch (e: Exception) {
        Log.e("ClasificadorBote", "Error recortando bitmap: ${e.message}")
        bitmap // Fallback al bitmap original
    }
}

/**
 * Dibuja las detecciones considerando el FOV real de la cámara con letterboxing.
 * La cámara usa aspect ratio 4:3 con FIT_CENTER, lo que crea barras negras.
 */
private fun DrawScope.drawDetections(detections: List<WasteClassifierYOLO.Detection>) {
    val colors = listOf(
        Color(0xFF00FF41), Color(0xFFFF0080), Color(0xFF00E5FF),
        Color(0xFFFFEA00), Color(0xFFAA00FF), Color(0xFFFF3D00)
    )

    // Calcular el área real del FOV de la cámara (4:3) dentro del Canvas
    val canvasWidth = size.width
    val canvasHeight = size.height
    val canvasAspect = canvasWidth / canvasHeight
    val cameraAspect = 4f / 3f  // Aspect ratio de la cámara

    // Calcular dimensiones y offset del FOV real con FIT_CENTER
    val fovWidth: Float
    val fovHeight: Float
    val fovOffsetX: Float
    val fovOffsetY: Float

    if (canvasAspect > cameraAspect) {
        // Canvas es más ancho que la cámara → barras negras a los lados
        fovHeight = canvasHeight
        fovWidth = canvasHeight * cameraAspect
        fovOffsetX = (canvasWidth - fovWidth) / 2f
        fovOffsetY = 0f
    } else {
        // Canvas es más alto que la cámara → barras negras arriba/abajo
        fovWidth = canvasWidth
        fovHeight = canvasWidth / cameraAspect
        fovOffsetX = 0f
        fovOffsetY = (canvasHeight - fovHeight) / 2f
    }

    detections.forEach { detection ->
        val color = colors[detection.classId % colors.size]

        // Transformar coordenadas normalizadas (0-1) al área real del FOV
        val left = fovOffsetX + detection.boundingBox.left * fovWidth
        val top = fovOffsetY + detection.boundingBox.top * fovHeight
        val right = fovOffsetX + detection.boundingBox.right * fovWidth
        val bottom = fovOffsetY + detection.boundingBox.bottom * fovHeight

        // Bounding box
        drawRect(
            color = color,
            topLeft = Offset(left, top),
            size = ComposeSize(right - left, bottom - top),
            style = Stroke(width = 3.dp.toPx())
        )

        // Esquinas para mejor visibilidad
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

        // Label
        val label = "${detection.className} ${(detection.confidence * 100).toInt()}%"
        val textPaint = android.graphics.Paint().apply {
            textSize = 28f
            setColor(android.graphics.Color.WHITE)
            isAntiAlias = true
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setShadowLayer(3f, 0f, 0f, android.graphics.Color.BLACK)
        }

        val textBounds = android.graphics.Rect()
        textPaint.getTextBounds(label, 0, label.length, textBounds)

        val labelY = if (top > textBounds.height() + 10) top - 5 else bottom + textBounds.height() + 5

        drawRoundRect(
            color = color.copy(alpha = 0.9f),
            topLeft = Offset(left, labelY - textBounds.height() - 6),
            size = ComposeSize(textBounds.width() + 16f, textBounds.height() + 12f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
        )

        drawContext.canvas.nativeCanvas.drawText(label, left + 8, labelY - 2, textPaint)
    }
}

/**
 * Dibuja el indicador visual del área de zoom activa
 * @param zoomRegion Región de zoom a dibujar
 * @param isBaseZoom true si es solo zoom base (sin adaptativo)
 * @param isAdaptiveZoom true si hay zoom adaptativo activo
 */
private fun DrawScope.drawZoomIndicator(
    zoomRegion: RectF,
    isBaseZoom: Boolean = false,
    isAdaptiveZoom: Boolean = false
) {
    val canvasWidth = size.width
    val canvasHeight = size.height
    val canvasAspect = canvasWidth / canvasHeight
    val cameraAspect = 4f / 3f

    val fovWidth: Float
    val fovHeight: Float
    val fovOffsetX: Float
    val fovOffsetY: Float

    if (canvasAspect > cameraAspect) {
        fovHeight = canvasHeight
        fovWidth = canvasHeight * cameraAspect
        fovOffsetX = (canvasWidth - fovWidth) / 2f
        fovOffsetY = 0f
    } else {
        fovWidth = canvasWidth
        fovHeight = canvasWidth / cameraAspect
        fovOffsetX = 0f
        fovOffsetY = (canvasHeight - fovHeight) / 2f
    }

    // Convertir coordenadas normalizadas a píxeles
    val left = fovOffsetX + zoomRegion.left * fovWidth
    val top = fovOffsetY + zoomRegion.top * fovHeight
    val right = fovOffsetX + zoomRegion.right * fovWidth
    val bottom = fovOffsetY + zoomRegion.bottom * fovHeight

    // Color según tipo de zoom
    val zoomColor = when {
        isAdaptiveZoom -> Color(0xFFE040FB)  // Magenta para zoom adaptativo
        isBaseZoom -> Color(0xFF00BCD4)       // Cyan para zoom base
        else -> Color(0xFFE040FB)
    }

    // Oscurecer áreas fuera del zoom
    val dimColor = Color.Black.copy(alpha = if (isAdaptiveZoom) 0.5f else 0.3f)

    // Área superior
    drawRect(
        color = dimColor,
        topLeft = Offset(fovOffsetX, fovOffsetY),
        size = ComposeSize(fovWidth, top - fovOffsetY)
    )
    // Área inferior
    drawRect(
        color = dimColor,
        topLeft = Offset(fovOffsetX, bottom),
        size = ComposeSize(fovWidth, fovHeight - (bottom - fovOffsetY))
    )
    // Área izquierda
    drawRect(
        color = dimColor,
        topLeft = Offset(fovOffsetX, top),
        size = ComposeSize(left - fovOffsetX, bottom - top)
    )
    // Área derecha
    drawRect(
        color = dimColor,
        topLeft = Offset(right, top),
        size = ComposeSize(fovWidth - (right - fovOffsetX), bottom - top)
    )

    // Borde del área de zoom
    drawRect(
        color = zoomColor,
        topLeft = Offset(left, top),
        size = ComposeSize(right - left, bottom - top),
        style = Stroke(width = if (isAdaptiveZoom) 4.dp.toPx() else 2.dp.toPx())
    )

    // Esquinas brillantes del zoom (solo para zoom adaptativo o combinado)
    if (isAdaptiveZoom) {
        val cornerLength = minOf(right - left, bottom - top) * 0.2f
        val cornerStroke = 6.dp.toPx()

        drawLine(zoomColor, Offset(left, top), Offset(left + cornerLength, top), cornerStroke)
        drawLine(zoomColor, Offset(left, top), Offset(left, top + cornerLength), cornerStroke)
        drawLine(zoomColor, Offset(right - cornerLength, top), Offset(right, top), cornerStroke)
        drawLine(zoomColor, Offset(right, top), Offset(right, top + cornerLength), cornerStroke)
        drawLine(zoomColor, Offset(left, bottom - cornerLength), Offset(left, bottom), cornerStroke)
        drawLine(zoomColor, Offset(left, bottom), Offset(left + cornerLength, bottom), cornerStroke)
        drawLine(zoomColor, Offset(right - cornerLength, bottom), Offset(right, bottom), cornerStroke)
        drawLine(zoomColor, Offset(right, bottom - cornerLength), Offset(right, bottom), cornerStroke)

        // Icono de zoom adaptativo
        val iconPaint = android.graphics.Paint().apply {
            textSize = 32f
            color = android.graphics.Color.WHITE
            isAntiAlias = true
        }
        drawContext.canvas.nativeCanvas.drawText("🔬", left + 8, top + 36, iconPaint)
    }
}

/**
 * Tarjeta de resultados mejorada que muestra info de la fase actual
 */
@Composable
private fun DetectionResultsCardWithPhase(
    detections: List<WasteClassifierYOLO.Detection>,
    phase: DetectionPhase,
    lockedMaterial: String?,
    votesMap: Map<String, Int>,
    isZoomed: Boolean,
    baseZoomLevel: Float = 1.0f
) {
    val backgroundColor = when (phase) {
        DetectionPhase.SCANNING -> Color.Black.copy(alpha = 0.85f)
        DetectionPhase.LOCKING_ON -> Color(0xFF1565C0).copy(alpha = 0.9f)  // Azul
        DetectionPhase.ZOOMED_CONFIRMING -> Color(0xFF7B1FA2).copy(alpha = 0.9f)  // Púrpura
        DetectionPhase.CONFIRMED -> Color(0xFF2E7D32).copy(alpha = 0.9f)  // Verde
        DetectionPhase.COOLDOWN -> Color(0xFF424242).copy(alpha = 0.85f)  // Gris
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            // Fila superior: Estado y material bloqueado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Estado de fase
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val (icon, label) = when (phase) {
                        DetectionPhase.SCANNING -> Icons.Default.Search to "Buscando"
                        DetectionPhase.LOCKING_ON -> Icons.Default.GpsFixed to "Bloqueando"
                        DetectionPhase.ZOOMED_CONFIRMING -> Icons.Default.ZoomIn to "ZOOM"
                        DetectionPhase.CONFIRMED -> Icons.Default.Check to "Confirmado"
                        DetectionPhase.COOLDOWN -> Icons.Default.HourglassBottom to "Espera"
                    }
                    Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)

                    // Badge de zoom base
                    if (baseZoomLevel > 1.0f) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF00BCD4)
                        ) {
                            Text(
                                "${baseZoomLevel}x",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Badge de zoom adaptativo
                    if (isZoomed) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFE040FB)
                        ) {
                            Text(
                                "TRACKING",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Material bloqueado o detectado
                if (lockedMaterial != null) {
                    Text(
                        lockedMaterial.uppercase(),
                        color = Color(0xFF00E676),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Fila inferior: Detección actual y votos
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                detections.maxByOrNull { it.confidence }?.let { top ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Detectando: ${top.className}",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp
                        )
                        Text(
                            "${(top.confidence * 100).toInt()}%",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                }

                // Votos (solo en fase de confirmación)
                if (phase == DetectionPhase.ZOOMED_CONFIRMING && votesMap.isNotEmpty()) {
                    val totalVotes = votesMap.values.sum()
                    val topVote = votesMap.maxByOrNull { it.value }
                    Text(
                        "${topVote?.value ?: 0}/$totalVotes votos",
                        color = Color(0xFFFFEB3B),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * Pantalla de configuración de zoom y rotación.
 * Muestra la cámara a pantalla completa con controles flotantes debajo del FOV.
 */
@Composable
private fun ZoomConfigScreen(
    currentZoom: Float,
    onZoomChange: (Float) -> Unit,
    currentRotation: Int,
    onRotationChange: (Int) -> Unit,
    longDistanceMode: Boolean,
    onLongDistanceModeChange: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onNavigateBack: () -> Unit,
    classifier: WasteClassifierYOLO,
    isClassifierReady: Boolean
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    // Estado para preview de detecciones
    var previewDetections by remember { mutableStateOf<List<WasteClassifierYOLO.Detection>>(emptyList()) }
    var cameraProvider: ProcessCameraProvider? by remember { mutableStateOf(null) }

    // Calcular región de zoom para preview
    val previewZoomRegion = remember(currentZoom) {
        if (currentZoom <= 1.0f) null
        else {
            val regionSize = 1.0f / currentZoom
            val offset = (1.0f - regionSize) / 2.0f
            RectF(offset, offset, offset + regionSize, offset + regionSize)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Preview de cámara a pantalla completa
        CameraPreviewWithZoom(
            onCameraProviderReady = { cameraProvider = it },
            classifier = classifier,
            isAnalyzing = isClassifierReady,
            zoomRegion = previewZoomRegion,
            cropRotation = currentRotation,
            onDetections = { detections ->
                previewDetections = detections
            }
        )

        // Canvas para bounding boxes y zoom indicator
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawDetections(previewDetections)

            // Mostrar indicador de zoom si está activo
            if (previewZoomRegion != null) {
                drawZoomIndicator(
                    zoomRegion = previewZoomRegion,
                    isBaseZoom = true,
                    isAdaptiveZoom = false
                )
            }
        }

        // Botón volver (arriba izquierda)
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .size(48.dp)
                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = "Volver",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        // Indicador de zoom y rotación actual (arriba centro)
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 20.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color.Black.copy(alpha = 0.6f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${String.format("%.1f", currentZoom)}x",
                    color = Color(0xFF00BCD4),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                if (currentRotation != 0) {
                    Text(
                        text = "${currentRotation}°",
                        color = Color(0xFFFF9800),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Detección en vivo (arriba derecha)
        if (previewDetections.isNotEmpty()) {
            val topDetection = previewDetections.maxByOrNull { it.confidence }
            topDetection?.let {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF4CAF50).copy(alpha = 0.85f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            it.className,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${(it.confidence * 100).toInt()}%",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Controles inferiores minimalistas
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Slider de zoom con más rango (1x a 8x)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.Black.copy(alpha = 0.6f)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Zoom:",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                        Text(
                            text = "${String.format("%.1f", currentZoom)}x",
                            color = Color(0xFF00BCD4),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = currentZoom,
                        onValueChange = { onZoomChange(it) },
                        valueRange = 1f..8f,
                        steps = 27,  // Para incrementos de 0.25x
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF00BCD4),
                            activeTrackColor = Color(0xFF00BCD4),
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        )
                    )
                    // Marcadores de referencia
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("1x", "2x", "4x", "6x", "8x").forEach { label ->
                            Text(
                                text = label,
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            // Botones de rotación compactos
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Rotación:",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
                listOf(0, 90, 180, 270).forEach { rotation ->
                    val isSelected = currentRotation == rotation
                    val rotationLabel = "${rotation}°"

                    Surface(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .clickable { onRotationChange(rotation) },
                        shape = CircleShape,
                        color = if (isSelected) Color(0xFFFF9800) else Color.Black.copy(alpha = 0.6f),
                        border = if (isSelected) null else BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = rotationLabel,
                            color = Color.White,
                            fontSize = if (isSelected) 14.sp else 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(
                                horizontal = if (isSelected) 14.dp else 10.dp,
                                vertical = if (isSelected) 8.dp else 6.dp
                            )
                        )
                    }
                }
            }

            // Botón de iniciar grande
            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Iniciar",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ConexionESP32Screen(estadoConexion: String, onNavigateBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFFAFAFA)).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Bluetooth, null, tint = Color(0xFF2196F3), modifier = Modifier.size(80.dp))
        Spacer(modifier = Modifier.height(32.dp))
        CircularProgressIndicator(modifier = Modifier.size(48.dp), color = Color(0xFF2196F3))
        Spacer(modifier = Modifier.height(24.dp))
        Text(estadoConexion, style = MaterialTheme.typography.titleLarge, color = BioWayColors.BrandDarkGreen, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onNavigateBack, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF70D162))) {
            Text("Volver")
        }
    }
}

@Composable
private fun PermissionRequiredScreen(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(80.dp), tint = BioWayColors.PrimaryGreen)
        Spacer(modifier = Modifier.height(24.dp))
        Text("Permiso de Cámara Necesario", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = BioWayColors.DarkGreen, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Para clasificar residuos necesitamos acceso a tu cámara", fontSize = 16.sp, color = BioWayColors.TextGrey, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onRequestPermission, colors = ButtonDefaults.buttonColors(containerColor = BioWayColors.PrimaryGreen), shape = RoundedCornerShape(12.dp)) {
            Text("Conceder Permiso", fontSize = 16.sp)
        }
    }
}

@Composable
private fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Warning, null, modifier = Modifier.size(80.dp), tint = Color.Red)
        Spacer(modifier = Modifier.height(24.dp))
        Text("Error", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = BioWayColors.DarkGreen)
        Spacer(modifier = Modifier.height(16.dp))
        Text(message, fontSize = 16.sp, color = BioWayColors.TextGrey, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = BioWayColors.PrimaryGreen), shape = RoundedCornerShape(12.dp)) {
            Text("Reintentar")
        }
    }
}

// ═══════════════════════════════════════
// FUNCIONES AUXILIARES
// ═══════════════════════════════════════

private fun mapMaterialToESP32(material: String): String {
    return when (material.lowercase()) {
        "plastic", "plastico", "plastic-pet", "plastic-pe_hd", "plastic-pp", "plastic-ps", "plastic-others" -> "PLASTICO"
        "paper", "papel" -> "PAPEL"
        "cardboard", "carton" -> "PAPEL"
        "metal", "aluminio" -> "ALUMINIO"
        else -> "GENERAL"
    }
}

private fun rotateBitmap(bitmap: Bitmap, rotationDegrees: Float): Bitmap {
    val matrix = Matrix().apply { postRotate(rotationDegrees) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

private fun ImageProxy.toBitmap(): Bitmap {
    val buffer = planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}

// ═══════════════════════════════════════
// NFC HELPER FUNCTIONS
// ═══════════════════════════════════════

// AID de BioWay (debe coincidir con el del servicio HCE)
private val BIOWAY_AID = byteArrayOf(
    0xF0.toByte(), 0x01, 0x02, 0x03, 0x04, 0x05, 0x06
)

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

// Comando SELECT AID
private val SELECT_APDU = buildSelectAidCommand(BIOWAY_AID)

// Comando GET USER ID
private val GET_USER_ID_APDU = byteArrayOf(
    0x00.toByte(),        // CLA
    0xCA.toByte(),        // INS - GET DATA
    0x00.toByte(),        // P1
    0x00.toByte(),        // P2
    0x00.toByte()         // Le (longitud esperada)
)

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

/**
 * Lee el User ID de un dispositivo con HCE usando comandos APDU
 * Versión asíncrona que debe ejecutarse en Dispatchers.IO
 * (Implementación idéntica a CelularEnBoteNFCScreen.readUserIdFromHce)
 */
private fun readUserIdFromHceAsync(tag: Tag): String? {
    Log.d("ClasificadorBote", "=== INICIO readUserIdFromHceAsync ===")

    // Verificar si el tag soporta IsoDep
    val isoDep = IsoDep.get(tag)
    if (isoDep == null) {
        Log.w("ClasificadorBote", "❌ Tag no soporta IsoDep")
        Log.d("ClasificadorBote", "Tecnologías disponibles: ${tag.techList.joinToString(", ")}")
        return null
    }

    try {
        Log.d("ClasificadorBote", "✅ IsoDep disponible, conectando...")
        isoDep.connect()
        Log.d("ClasificadorBote", "✅ Conectado!")
        Log.d("ClasificadorBote", "Timeout inicial: ${isoDep.timeout}ms")
        Log.d("ClasificadorBote", "Max transceive length: ${isoDep.maxTransceiveLength} bytes")

        // OPTIMIZACIÓN 1: Aumentar timeout al máximo para mejor estabilidad
        isoDep.timeout = 10000 // 10 segundos
        Log.d("ClasificadorBote", "🔧 Timeout actualizado a: ${isoDep.timeout}ms (mejor estabilidad)")

        // OPTIMIZACIÓN 2: Verificar Extended Length APDU
        try {
            if (isoDep.isExtendedLengthApduSupported) {
                Log.d("ClasificadorBote", "✅ Extended Length APDU soportado!")
            } else {
                Log.d("ClasificadorBote", "ℹ️ Extended Length APDU no soportado (normal en HCE)")
            }
        } catch (e: Exception) {
            Log.d("ClasificadorBote", "ℹ️ No se pudo verificar Extended Length APDU support")
        }

        // Paso 1: Enviar comando SELECT AID
        Log.d("ClasificadorBote", "📤 Enviando SELECT APDU: ${SELECT_APDU.toHexString()}")
        val selectResponse = isoDep.transceive(SELECT_APDU)
        Log.d("ClasificadorBote", "📥 Respuesta SELECT: ${selectResponse.toHexString()}")

        // Verificar que el SELECT fue exitoso (90 00)
        if (!isSuccessResponse(selectResponse)) {
            Log.e("ClasificadorBote", "❌ SELECT falló. Respuesta: ${selectResponse.toHexString()}")
            isoDep.close()
            return null
        }
        Log.d("ClasificadorBote", "✅ SELECT exitoso!")

        // Paso 2: Enviar comando GET USER ID
        Log.d("ClasificadorBote", "📤 Enviando GET_USER_ID APDU: ${GET_USER_ID_APDU.toHexString()}")
        val userIdResponse = isoDep.transceive(GET_USER_ID_APDU)
        Log.d("ClasificadorBote", "📥 Respuesta GET_USER_ID: ${userIdResponse.toHexString()}")
        Log.d("ClasificadorBote", "Longitud de respuesta: ${userIdResponse.size} bytes")

        // La respuesta debe terminar en 90 00 (éxito)
        if (userIdResponse.size < 2) {
            Log.e("ClasificadorBote", "❌ Respuesta muy corta")
            isoDep.close()
            return null
        }

        // Extraer el User ID (todos los bytes excepto los últimos 2 que son el status)
        val userIdBytes = userIdResponse.copyOfRange(0, userIdResponse.size - 2)
        val statusBytes = userIdResponse.copyOfRange(userIdResponse.size - 2, userIdResponse.size)

        Log.d("ClasificadorBote", "Status bytes: ${statusBytes.toHexString()}")
        Log.d("ClasificadorBote", "User ID bytes: ${userIdBytes.toHexString()}")

        if (!isSuccessResponse(statusBytes)) {
            Log.e("ClasificadorBote", "❌ GET_USER_ID falló. Status: ${statusBytes.toHexString()}")
            isoDep.close()
            return null
        }

        // Convertir bytes a string
        val userId = String(userIdBytes, Charsets.UTF_8)
        Log.d("ClasificadorBote", "✅ User ID extraído: '$userId'")

        isoDep.close()
        Log.d("ClasificadorBote", "=== FIN readUserIdFromHceAsync (ÉXITO) ===")
        return userId

    } catch (e: Exception) {
        Log.e("ClasificadorBote", "❌ ERROR durante transceive: ${e.javaClass.simpleName}: ${e.message}")
        e.printStackTrace()

        try {
            isoDep.close()
        } catch (closeException: Exception) {
            Log.e("ClasificadorBote", "Error al cerrar IsoDep: ${closeException.message}")
        }

        return null
    } finally {
        Log.d("ClasificadorBote", "=== FIN readUserIdFromHceAsync ===")
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// COMPONENTE NFC INDEPENDIENTE
// ═══════════════════════════════════════════════════════════════════════════════

// Constantes APDU definidas a nivel de archivo (fuera de cualquier composable)
// para evitar problemas de closures
private val NFC_BIOWAY_AID = byteArrayOf(
    0xF0.toByte(), 0x01, 0x02, 0x03, 0x04, 0x05, 0x06
)

private val NFC_SELECT_APDU = byteArrayOf(
    0x00.toByte(), // CLA
    0xA4.toByte(), // INS (SELECT)
    0x04.toByte(), // P1 (Select by name)
    0x00.toByte(), // P2
    0x07.toByte(), // Lc (7 bytes de AID)
    0xF0.toByte(), 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, // AID
    0x00.toByte()  // Le
)

private val NFC_GET_USER_ID_APDU = byteArrayOf(
    0x00.toByte(), // CLA
    0xCA.toByte(), // INS - GET DATA
    0x00.toByte(), // P1
    0x00.toByte(), // P2
    0x00.toByte()  // Le
)

/**
 * Componente NFC completamente independiente.
 * Maneja su propio ciclo de vida y no interfiere con otros componentes.
 * IMPORTANTE: Re-registra el callback cada 3 segundos para mantenerlo activo
 * incluso cuando hay recomposiciones frecuentes de la cámara.
 */
@Composable
fun IndependentNfcReader(
    onUserIdDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    // Estado del NFC
    var nfcAdapter: NfcAdapter? by remember { mutableStateOf(null) }
    var isNfcEnabled by remember { mutableStateOf(false) }
    var registrationCount by remember { mutableStateOf(0) }

    // Mantener referencias actualizadas para el callback
    val currentOnUserIdDetected by rememberUpdatedState(onUserIdDetected)
    val currentContext by rememberUpdatedState(context)
    val currentScope by rememberUpdatedState(scope)

    // Función para registrar el reader mode con callback fresco
    fun registerReaderMode() {
        val adapter = nfcAdapter
        val act = activity

        if (adapter == null || act == null || !isNfcEnabled) {
            Log.w("IndependentNFC", "⚠️ No se puede registrar: adapter=$adapter, activity=$act, enabled=$isNfcEnabled")
            return
        }

        try {
            // Crear callback FRESCO cada vez que se registra
            val freshCallback = NfcAdapter.ReaderCallback { tag ->
                Log.d("IndependentNFC", "========================================")
                Log.d("IndependentNFC", "🔵 TAG DETECTADO!!")
                Log.d("IndependentNFC", "========================================")
                Log.d("IndependentNFC", "Tag ID: ${tag.id.joinToString(":")}")
                Log.d("IndependentNFC", "Tecnologías: ${tag.techList.joinToString(", ")}")

                currentScope.launch {
                    try {
                        val userId = withContext(Dispatchers.IO) {
                            readUserIdFromHceStatic(tag, NFC_SELECT_APDU, NFC_GET_USER_ID_APDU)
                        }

                        if (userId != null) {
                            Log.d("IndependentNFC", "✅ DETECCIÓN EXITOSA: $userId")
                            currentOnUserIdDetected(userId)

                            val vibrator = currentContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                            vibrator?.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
                        } else {
                            Log.w("IndependentNFC", "❌ No se pudo obtener User ID")
                        }
                    } catch (e: Exception) {
                        Log.e("IndependentNFC", "❌ Error en callback: ${e.message}", e)
                    }
                }
            }

            val options = Bundle().apply {
                putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 5000)
            }

            // Re-registrar (enableReaderMode reemplaza el anterior automáticamente)
            adapter.enableReaderMode(
                act,
                freshCallback,
                NfcAdapter.FLAG_READER_NFC_A or
                    NfcAdapter.FLAG_READER_NFC_B or
                    NfcAdapter.FLAG_READER_NFC_F or
                    NfcAdapter.FLAG_READER_NFC_V or
                    NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK or
                    NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,
                options
            )

            registrationCount++
            Log.d("IndependentNFC", "✅ Reader mode REGISTRADO (count: $registrationCount)")

        } catch (e: Exception) {
            Log.e("IndependentNFC", "❌ Error al registrar reader mode: ${e.message}")
        }
    }

    // Función para detener reader mode
    fun stopReaderMode() {
        val adapter = nfcAdapter
        val act = activity

        if (act != null && adapter != null) {
            try {
                adapter.disableReaderMode(act)
                Log.d("IndependentNFC", "⏹️ Reader mode deshabilitado")
            } catch (e: Exception) {
                Log.e("IndependentNFC", "❌ Error al deshabilitar: ${e.message}")
            }
        }
    }

    // Inicializar NFC
    LaunchedEffect(Unit) {
        nfcAdapter = NfcAdapter.getDefaultAdapter(context)
        Log.d("IndependentNFC", "========================================")
        Log.d("IndependentNFC", "=== IndependentNfcReader INICIADO ===")
        Log.d("IndependentNFC", "========================================")

        if (nfcAdapter == null) {
            Log.w("IndependentNFC", "❌ NFC no soportado")
        } else {
            isNfcEnabled = nfcAdapter!!.isEnabled
            Log.d("IndependentNFC", "NFC soportado: ${if (isNfcEnabled) "Habilitado ✅" else "Deshabilitado ❌"}")

            if (isNfcEnabled) {
                registerReaderMode()
            }
        }
    }

    // RE-REGISTRAR el NFC callback cada 3 segundos para mantenerlo activo
    // Esto es necesario porque las recomposiciones de la cámara pueden invalidar el callback
    LaunchedEffect(isNfcEnabled) {
        if (isNfcEnabled && nfcAdapter != null) {
            while (true) {
                kotlinx.coroutines.delay(3000)  // Esperar 3 segundos
                Log.d("IndependentNFC", "🔄 [HEARTBEAT] Re-registrando NFC callback...")
                registerReaderMode()
            }
        }
    }

    // Lifecycle observer
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    Log.d("IndependentNFC", "📱 ON_RESUME - registrando NFC...")
                    if (isNfcEnabled) {
                        registerReaderMode()
                    }
                }
                Lifecycle.Event.ON_STOP -> {
                    Log.d("IndependentNFC", "🛑 ON_STOP - deshabilitando NFC")
                    stopReaderMode()
                }
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            Log.d("IndependentNFC", "🗑️ IndependentNfcReader DISPOSED")
            lifecycleOwner.lifecycle.removeObserver(observer)
            stopReaderMode()
        }
    }

    // Este componente no renderiza nada visible
}

/**
 * Función estática para leer User ID desde HCE.
 * Definida fuera del composable para evitar problemas de closures.
 */
private fun readUserIdFromHceStatic(
    tag: Tag,
    selectApdu: ByteArray,
    getUserIdApdu: ByteArray
): String? {
    Log.d("IndependentNFC", "=== INICIO readUserIdFromHceStatic ===")

    val isoDep = IsoDep.get(tag)
    if (isoDep == null) {
        Log.w("IndependentNFC", "❌ Tag no soporta IsoDep")
        return null
    }

    return try {
        Log.d("IndependentNFC", "✅ IsoDep disponible, conectando...")
        isoDep.connect()
        Log.d("IndependentNFC", "✅ Conectado! Timeout: ${isoDep.timeout}ms")

        isoDep.timeout = 10000
        Log.d("IndependentNFC", "🔧 Timeout actualizado a: 10000ms")

        // SELECT AID
        Log.d("IndependentNFC", "📤 Enviando SELECT APDU")
        val selectResponse = isoDep.transceive(selectApdu)
        Log.d("IndependentNFC", "📥 Respuesta SELECT: ${selectResponse.joinToString(" ") { "%02X".format(it) }}")

        if (selectResponse.size < 2 ||
            selectResponse[selectResponse.size - 2] != 0x90.toByte() ||
            selectResponse[selectResponse.size - 1] != 0x00.toByte()) {
            Log.e("IndependentNFC", "❌ SELECT falló")
            isoDep.close()
            return null
        }
        Log.d("IndependentNFC", "✅ SELECT exitoso!")

        // GET USER ID
        Log.d("IndependentNFC", "📤 Enviando GET_USER_ID APDU")
        val userIdResponse = isoDep.transceive(getUserIdApdu)
        Log.d("IndependentNFC", "📥 Respuesta: ${userIdResponse.joinToString(" ") { "%02X".format(it) }}")

        if (userIdResponse.size < 2) {
            Log.e("IndependentNFC", "❌ Respuesta muy corta")
            isoDep.close()
            return null
        }

        val userIdBytes = userIdResponse.copyOfRange(0, userIdResponse.size - 2)
        val statusBytes = userIdResponse.copyOfRange(userIdResponse.size - 2, userIdResponse.size)

        if (statusBytes[0] != 0x90.toByte() || statusBytes[1] != 0x00.toByte()) {
            Log.e("IndependentNFC", "❌ GET_USER_ID falló")
            isoDep.close()
            return null
        }

        val userId = String(userIdBytes, Charsets.UTF_8)
        Log.d("IndependentNFC", "✅ User ID extraído: '$userId'")

        isoDep.close()
        userId

    } catch (e: Exception) {
        Log.e("IndependentNFC", "❌ ERROR: ${e.message}")
        try { isoDep.close() } catch (_: Exception) {}
        null
    }
}
