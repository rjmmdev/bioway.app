package com.biowaymexico.ui.screens.bote_bioway

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biowaymexico.ui.theme.BioWayColors
import com.biowaymexico.utils.BluetoothManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "PruebaServos"

/**
 * Pantalla de prueba para controlar los servomotores del ESP32 manualmente.
 * Permite ajustar:
 * - GIRO (Servo 13): 0-160 grados
 * - INCLINACIÓN (Servos 12 y 14 juntos): 0-48 grados
 *
 * Características:
 * - Definir posición inicial
 * - Ver ángulos en tiempo real de cada servo
 * - Guardar posiciones para cada categoría de material
 *
 * Requiere cargar ESP32_PRUEBA_SERVOS.txt en el ESP32
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PruebaServosScreen(
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()

    // Estado de conexión Bluetooth
    val bluetoothManager = remember { BluetoothManager() }
    var bluetoothConectado by remember { mutableStateOf(false) }
    var estadoConexion by remember { mutableStateOf("Desconectado") }
    var conectando by remember { mutableStateOf(false) }

    // POSICIÓN INICIAL (configurable)
    var giroInicial by remember { mutableStateOf(0f) }
    var inclinacionInicial by remember { mutableStateOf(0f) }
    var posicionInicialDefinida by remember { mutableStateOf(false) }

    // Posiciones actuales de los servos (permiten negativos)
    var posicionGiro by remember { mutableStateOf(0f) }           // -80 a 160
    var posicionInclinacion by remember { mutableStateOf(0f) }    // -45 a 45 (adelante/atrás)

    // Estado de envío
    var enviando by remember { mutableStateOf(false) }
    var ultimoComando by remember { mutableStateOf("") }
    var ultimaRespuesta by remember { mutableStateOf("") }

    // Limpiar al salir
    DisposableEffect(Unit) {
        onDispose {
            bluetoothManager.desconectar()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Control de Servos",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BioWayColors.DarkGreen,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ═══════════════════════════════════════
            // SECCIÓN: CONEXIÓN BLUETOOTH
            // ═══════════════════════════════════════
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (bluetoothConectado)
                        BioWayColors.PrimaryGreen.copy(alpha = 0.1f)
                    else
                        Color.Red.copy(alpha = 0.1f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (bluetoothConectado)
                                Icons.Default.BluetoothConnected
                            else
                                Icons.Default.BluetoothDisabled,
                            contentDescription = null,
                            tint = if (bluetoothConectado) BioWayColors.PrimaryGreen else Color.Red
                        )
                        Text(
                            text = estadoConexion,
                            fontWeight = FontWeight.Medium,
                            color = if (bluetoothConectado) BioWayColors.DarkGreen else Color.Red
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (!bluetoothConectado && !conectando) {
                                conectando = true
                                estadoConexion = "Conectando..."
                                scope.launch {
                                    val result = bluetoothManager.conectarConHandshake()
                                    result.fold(
                                        onSuccess = {
                                            bluetoothConectado = true
                                            estadoConexion = "ESP32 conectado ✓"
                                            Log.d(TAG, "✅ Conectado al ESP32")
                                        },
                                        onFailure = { error ->
                                            bluetoothConectado = false
                                            estadoConexion = "Error: ${error.message}"
                                            Log.e(TAG, "❌ Error: ${error.message}")
                                        }
                                    )
                                    conectando = false
                                }
                            } else if (bluetoothConectado) {
                                bluetoothManager.desconectar()
                                bluetoothConectado = false
                                estadoConexion = "Desconectado"
                            }
                        },
                        enabled = !conectando,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (bluetoothConectado) Color.Red else BioWayColors.PrimaryGreen
                        )
                    ) {
                        if (conectando) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Conectando...")
                        } else {
                            Text(if (bluetoothConectado) "Desconectar" else "Conectar ESP32")
                        }
                    }

                    if (!bluetoothConectado) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "⚠️ Cargar ESP32_PRUEBA_SERVOS.txt en el ESP32",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // ═══════════════════════════════════════
            // SECCIÓN: VISUALIZACIÓN DE ÁNGULOS EN TIEMPO REAL
            // ═══════════════════════════════════════
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF37474F)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "📐 ÁNGULOS ACTUALES",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Calcular ángulos reales de los servos
                    val giroReal = (posicionGiro.toInt() + 80).coerceIn(0, 180)
                    val inclVal = posicionInclinacion.toInt()
                    // Nueva lógica: posición neutra S12=150°, S14=30°
                    val s12Display = (135 - inclVal).coerceIn(0, 180)
                    val s14Display = (45 + inclVal).coerceIn(0, 180)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Servo 13 (Giro)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("SERVO 13", fontSize = 10.sp, color = Color(0xFF90CAF9))
                            Text("(Giro)", fontSize = 10.sp, color = Color.Gray)
                            Text(
                                text = "${posicionGiro.toInt()}°",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64B5F6),
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "real: ${giroReal}°",
                                fontSize = 9.sp,
                                color = Color.Gray
                            )
                        }

                        // Servo 12
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("SERVO 12", fontSize = 10.sp, color = Color(0xFFFFCC80))
                            Text("(Incl.)", fontSize = 10.sp, color = Color.Gray)
                            Text(
                                text = "${s12Display}°",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFB74D),
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Servo 14
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("SERVO 14", fontSize = 10.sp, color = Color(0xFFA5D6A7))
                            Text("(Incl.)", fontSize = 10.sp, color = Color.Gray)
                            Text(
                                text = "${s14Display}°",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF81C784),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Mostrar posición inicial si está definida
                    if (posicionInicialDefinida) {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Posición Inicial: Giro=${giroInicial.toInt()}° | Incl=${inclinacionInicial.toInt()}°",
                            fontSize = 11.sp,
                            color = Color(0xFF4CAF50),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // ═══════════════════════════════════════
            // SECCIÓN: DEFINIR POSICIÓN INICIAL
            // ═══════════════════════════════════════
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (posicionInicialDefinida)
                        Color(0xFF4CAF50).copy(alpha = 0.1f)
                    else
                        Color(0xFFFFF8E1)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "🏠 POSICIÓN INICIAL",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (posicionInicialDefinida) Color(0xFF2E7D32) else Color(0xFFE65100)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (!posicionInicialDefinida) {
                        Text(
                            text = "Mueve los servos a la posición de reposo y presiona 'Definir como Inicial'",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    } else {
                        Text(
                            text = "✓ Posición inicial definida. Los materiales se depositarán desde esta posición.",
                            fontSize = 12.sp,
                            color = Color(0xFF2E7D32)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                giroInicial = posicionGiro
                                inclinacionInicial = posicionInclinacion
                                posicionInicialDefinida = true
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            )
                        ) {
                            Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Definir Inicial", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                // Ir a posición inicial
                                if (posicionInicialDefinida && bluetoothConectado && !enviando) {
                                    enviando = true
                                    scope.launch {
                                        // Enviar giro
                                        enviarComando(bluetoothManager, "GIRO:${giroInicial.toInt()}") {}
                                        kotlinx.coroutines.delay(500)
                                        // Enviar inclinación
                                        enviarComando(bluetoothManager, "INCL:${inclinacionInicial.toInt()}") {}
                                        posicionGiro = giroInicial
                                        posicionInclinacion = inclinacionInicial
                                        enviando = false
                                    }
                                }
                            },
                            enabled = posicionInicialDefinida && bluetoothConectado && !enviando,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ir a Inicial", fontSize = 12.sp)
                        }
                    }
                }
            }

            // ═══════════════════════════════════════
            // SECCIÓN: CONTROL DE GIRO (Servo 13)
            // ═══════════════════════════════════════
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE3F2FD)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🔄 GIRO (Servo 13)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF1565C0)
                        )
                        Text(
                            text = "${posicionGiro.toInt()}°",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1565C0),
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Slider(
                        value = posicionGiro,
                        onValueChange = { posicionGiro = it },
                        valueRange = -80f..160f,
                        steps = 23,  // cada 10 grados
                        enabled = bluetoothConectado && !enviando,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF1565C0),
                            activeTrackColor = Color(0xFF1565C0)
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("-80°", fontSize = 10.sp, color = Color.Gray)
                        Text("-40°", fontSize = 10.sp, color = Color.Gray)
                        Text("0°", fontSize = 10.sp, color = Color.Gray)
                        Text("40°", fontSize = 10.sp, color = Color.Gray)
                        Text("80°", fontSize = 10.sp, color = Color.Gray)
                        Text("120°", fontSize = 10.sp, color = Color.Gray)
                        Text("160°", fontSize = 10.sp, color = Color.Gray)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            if (bluetoothConectado && !enviando) {
                                enviando = true
                                ultimoComando = "GIRO:${posicionGiro.toInt()}"
                                scope.launch {
                                    enviarComando(bluetoothManager, ultimoComando) { respuesta ->
                                        ultimaRespuesta = respuesta
                                        enviando = false
                                    }
                                }
                            }
                        },
                        enabled = bluetoothConectado && !enviando,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1565C0)
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Enviar GIRO → ${posicionGiro.toInt()}°")
                    }
                }
            }

            // ═══════════════════════════════════════
            // SECCIÓN: CONTROL DE INCLINACIÓN (Servos 12 y 14)
            // ═══════════════════════════════════════
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF3E0)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "📐 INCLINACIÓN",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFFE65100)
                            )
                            Text(
                                text = "Servos 12 y 14",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                        Text(
                            text = "${posicionInclinacion.toInt()}°",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE65100),
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Mostrar valores individuales (lógica espalda con espalda)
                    // Posición neutra: S12=135°, S14=45° (brazo hacia arriba)
                    // Positivo (+45): S12=90°, S14=90° (inclina adelante)
                    // Negativo (-45): S12=180°, S14=0° (inclina atrás)
                    val incl = posicionInclinacion.toInt()
                    val s12Real = (135 - incl).coerceIn(0, 180)
                    val s14Real = (45 + incl).coerceIn(0, 180)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Text(
                            text = "S12: ${s12Real}°",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFE65100)
                        )
                        Text(
                            text = "S14: ${s14Real}°",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFE65100)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Slider(
                        value = posicionInclinacion,
                        onValueChange = { posicionInclinacion = it },
                        valueRange = -45f..45f,
                        steps = 89,  // cada 1 grado
                        enabled = bluetoothConectado && !enviando,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFE65100),
                            activeTrackColor = Color(0xFFE65100)
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("-45°", fontSize = 10.sp, color = Color.Gray)
                        Text("Atrás", fontSize = 9.sp, color = Color(0xFFE65100))
                        Text("0°", fontSize = 10.sp, color = Color.Gray)
                        Text("Adelante", fontSize = 9.sp, color = Color(0xFFE65100))
                        Text("+45°", fontSize = 10.sp, color = Color.Gray)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            if (bluetoothConectado && !enviando) {
                                enviando = true
                                ultimoComando = "INCL:${posicionInclinacion.toInt()}"
                                scope.launch {
                                    enviarComando(bluetoothManager, ultimoComando) { respuesta ->
                                        ultimaRespuesta = respuesta
                                        enviando = false
                                    }
                                }
                            }
                        },
                        enabled = bluetoothConectado && !enviando,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE65100)
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Enviar INCL → ${posicionInclinacion.toInt()}°")
                    }
                }
            }

            // ═══════════════════════════════════════
            // SECCIÓN: EJECUTAR MOVIMIENTOS POR MATERIAL
            // ═══════════════════════════════════════
            // Configuración de materiales:
            // Material 1: Giro -30° (real 50°), Incl -30° (hacia la puerta)
            // Material 2: Giro 59° (real 139°), Incl +30° (hacia el ajolote)
            // Material 3: Giro -30° (real 50°), Incl +30° (hacia los logos)
            // Material 4: Giro 59° (real 139°), Incl -30° (hacia el zorro)

            var ejecutandoMaterial by remember { mutableStateOf<String?>(null) }
            var pasoActual by remember { mutableStateOf("") }

            // Función para ejecutar secuencia de material
            fun ejecutarSecuenciaMaterial(
                nombreMaterial: String,
                giro: Int,
                inclinacion: Int
            ) {
                if (ejecutandoMaterial != null || !bluetoothConectado) return

                ejecutandoMaterial = nombreMaterial
                scope.launch {
                    try {
                        // Paso 1: Girar a posición
                        pasoActual = "Girando a $giro°..."
                        enviarComando(bluetoothManager, "GIRO:$giro") {}
                        posicionGiro = giro.toFloat()
                        kotlinx.coroutines.delay(800)

                        // Paso 2: Inclinar
                        pasoActual = "Inclinando a $inclinacion°..."
                        enviarComando(bluetoothManager, "INCL:$inclinacion") {}
                        posicionInclinacion = inclinacion.toFloat()
                        kotlinx.coroutines.delay(1000)

                        // Paso 3: Mantener posición
                        pasoActual = "Depositando material..."
                        kotlinx.coroutines.delay(400)

                        // Paso 4: Volver inclinación a 0
                        pasoActual = "Volviendo inclinación..."
                        enviarComando(bluetoothManager, "INCL:0") {}
                        posicionInclinacion = 0f
                        kotlinx.coroutines.delay(800)

                        // Paso 5: Volver giro a posición inicial (giro -80 = servo real 0°)
                        pasoActual = "Volviendo a inicio..."
                        enviarComando(bluetoothManager, "GIRO:-80") {}
                        posicionGiro = -80f
                        kotlinx.coroutines.delay(800)

                        pasoActual = "✓ Completado"
                        kotlinx.coroutines.delay(500)

                    } catch (e: Exception) {
                        pasoActual = "Error: ${e.message}"
                    } finally {
                        ejecutandoMaterial = null
                        pasoActual = ""
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE8F5E9)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "🗑️ EJECUTAR DEPÓSITO DE MATERIAL",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = BioWayColors.DarkGreen
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Presiona un material para ejecutar la secuencia completa",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )

                    // Mostrar progreso si está ejecutando
                    if (ejecutandoMaterial != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF1565C0).copy(alpha = 0.1f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF1565C0)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Ejecutando: $ejecutandoMaterial",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFF1565C0)
                                    )
                                    Text(
                                        text = pasoActual,
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Definición de materiales con sus movimientos
                    data class MaterialConfig(
                        val nombre: String,
                        val giro: Int,
                        val inclinacion: Int,
                        val color: Color,
                        val descripcion: String,
                        val icono: String
                    )

                    val materiales = listOf(
                        MaterialConfig("MATERIAL 1", -30, -45, Color(0xFF2196F3), "Hacia la puerta", "🚪"),
                        MaterialConfig("MATERIAL 2", 59, 45, Color(0xFFFF9800), "Hacia el ajolote", "🦎"),
                        MaterialConfig("MATERIAL 3", -30, 45, Color(0xFF4CAF50), "Hacia los logos", "🏷️"),
                        MaterialConfig("MATERIAL 4", 59, -45, Color(0xFF9C27B0), "Hacia el zorro", "🦊")
                    )

                    // Grid 2x2 de materiales
                    materiales.chunked(2).forEach { rowMats ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowMats.forEach { mat ->
                                val isEjecutando = ejecutandoMaterial == mat.nombre
                                Button(
                                    onClick = {
                                        ejecutarSecuenciaMaterial(mat.nombre, mat.giro, mat.inclinacion)
                                    },
                                    enabled = ejecutandoMaterial == null && bluetoothConectado,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(90.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isEjecutando) mat.color else mat.color.copy(alpha = 0.8f),
                                        disabledContainerColor = mat.color.copy(alpha = 0.3f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = mat.icono,
                                            fontSize = 20.sp
                                        )
                                        Text(
                                            text = mat.nombre,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "G:${mat.giro}° I:${mat.inclinacion}°",
                                            fontSize = 9.sp,
                                            color = Color.White.copy(alpha = 0.8f)
                                        )
                                        Text(
                                            text = mat.descripcion,
                                            fontSize = 8.sp,
                                            color = Color.White.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Información de secuencia
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "📋 Secuencia de movimiento:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "1. Girar → 2. Inclinar → 3. Mantener → 4. Volver incl. → 5. Volver giro",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "Posición inicial: Giro -80° (servo real 0°)",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
            }

            // ═══════════════════════════════════════
            // SECCIÓN: ACCIONES RÁPIDAS
            // ═══════════════════════════════════════
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "⚡ ACCIONES",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Botón RESET a 0,0
                        OutlinedButton(
                            onClick = {
                                if (bluetoothConectado && !enviando) {
                                    enviando = true
                                    ultimoComando = "RESET"
                                    scope.launch {
                                        enviarComando(bluetoothManager, "RESET") { respuesta ->
                                            ultimaRespuesta = respuesta
                                            posicionGiro = 0f
                                            posicionInclinacion = 0f
                                            enviando = false
                                        }
                                    }
                                }
                            },
                            enabled = bluetoothConectado && !enviando,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("RESET (0°,0°)", fontSize = 11.sp)
                        }

                        // Botón para ir a una posición guardada
                        OutlinedButton(
                            onClick = {
                                if (bluetoothConectado && !enviando) {
                                    enviando = true
                                    ultimoComando = "GUARDAR"
                                    scope.launch {
                                        enviarComando(bluetoothManager, "GUARDAR") { respuesta ->
                                            ultimaRespuesta = respuesta
                                            enviando = false
                                        }
                                    }
                                }
                            },
                            enabled = bluetoothConectado && !enviando,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Leer ESP32", fontSize = 11.sp)
                        }
                    }
                }
            }

            // ═══════════════════════════════════════
            // SECCIÓN: LOG DE COMUNICACIÓN
            // ═══════════════════════════════════════
            if (ultimoComando.isNotEmpty() || enviando) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF5F5F5)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (enviando) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = "→ $ultimoComando",
                                fontSize = 11.sp,
                                color = Color(0xFF1565C0),
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        if (ultimaRespuesta.isNotEmpty()) {
                            Text(
                                text = "← $ultimaRespuesta",
                                fontSize = 11.sp,
                                color = if (ultimaRespuesta == "OK") Color(0xFF4CAF50) else Color.Red,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Envía un comando al ESP32 y espera respuesta
 */
private suspend fun enviarComando(
    bluetoothManager: BluetoothManager,
    comando: String,
    onRespuesta: (String) -> Unit
) {
    withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📤 Enviando: $comando")

            // Usar reflexión para acceder al outputStream y inputStream
            val outputStreamField = bluetoothManager::class.java.getDeclaredField("outputStream")
            outputStreamField.isAccessible = true
            val outputStream = outputStreamField.get(bluetoothManager) as? java.io.OutputStream

            val inputStreamField = bluetoothManager::class.java.getDeclaredField("inputStream")
            inputStreamField.isAccessible = true
            val inputStream = inputStreamField.get(bluetoothManager) as? java.io.InputStream

            if (outputStream == null || inputStream == null) {
                withContext(Dispatchers.Main) {
                    onRespuesta("ERROR: No conectado")
                }
                return@withContext
            }

            // Enviar comando
            outputStream.write("$comando\n".toByteArray())
            outputStream.flush()

            // Esperar respuesta (máximo 5 segundos)
            val buffer = ByteArray(1024)
            val startTime = System.currentTimeMillis()

            while (System.currentTimeMillis() - startTime < 5000) {
                if (inputStream.available() > 0) {
                    val bytes = inputStream.read(buffer)
                    val respuesta = String(buffer, 0, bytes).trim()
                    Log.d(TAG, "📥 Respuesta: $respuesta")

                    withContext(Dispatchers.Main) {
                        onRespuesta(respuesta)
                    }
                    return@withContext
                }
                Thread.sleep(100)
            }

            withContext(Dispatchers.Main) {
                onRespuesta("TIMEOUT")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error: ${e.message}")
            withContext(Dispatchers.Main) {
                onRespuesta("ERROR: ${e.message}")
            }
        }
    }
}
