package com.biowaymexico.utils

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager as AndroidBluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

/**
 * Manager de Bluetooth para comunicación con ESP32
 *
 * PROTOCOLO v2 (Comunicación Bidireccional):
 * - Android envía: DEPOSITAR:CATEGORIA
 * - ESP32 ejecuta secuencia completa
 * - ESP32 responde: LISTO (cuando termina)
 * - Android resume detección
 */
class BluetoothManager(private val context: Context? = null) {

    companion object {
        private const val TAG = "BluetoothManager"
        private const val ESP32_NAME = "ESP32_Detector"
        private val UUID_SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        // Timeout largo para esperar LISTO (secuencia completa ~4 segundos)
        private const val LISTO_TIMEOUT_MS = 15000L

        /**
         * Verificar si los permisos de Bluetooth están otorgados
         */
        fun hasBluetoothPermissions(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
            } else {
                true // En versiones anteriores a Android 12, no se necesitan estos permisos específicos
            }
        }
    }

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    /**
     * Configuración de movimientos para cada categoría de material
     * Cada categoría tiene un GIRO y una INCLINACIÓN específicos
     */
    data class MovimientoMaterial(
        val giro: Int,        // -80 a 160
        val inclinacion: Int  // -45 a 45
    )

    /**
     * Mapeo de materiales a movimientos de servos
     * - MATERIAL 1 (Giro -30°, Incl -45°): Plástico, Metal
     * - MATERIAL 2 (Giro 59°, Incl +45°): Basura/General
     * - MATERIAL 3 (Giro -30°, Incl +45°): Cartón, Papel
     * - MATERIAL 4 (Giro 59°, Incl -45°): Vidrio, Orgánico
     */
    private fun obtenerMovimientoParaMaterial(material: String): MovimientoMaterial {
        val materialUpper = material.uppercase().trim()

        Log.d(TAG, "🔄 Obteniendo movimiento para: '$material'")

        val movimiento = when {
            // MATERIAL 1 (Giro -30°, Incl -45°): Plástico, Metal
            materialUpper.contains("PLASTIC") || materialUpper.contains("PLASTICO") ||
            materialUpper.contains("PET") || materialUpper.contains("PEAD") ||
            materialUpper.contains("PEBD") || materialUpper.contains("BOPP") ||
            materialUpper.contains("POLIPROPILENO") || materialUpper.contains("UNICEL") ||
            materialUpper.contains("METAL") || materialUpper.contains("TIN") ||
            materialUpper.contains("HOJALATA") -> {
                Log.d(TAG, "📦 Categoría: PLÁSTICO/METAL → Material 1")
                MovimientoMaterial(giro = -30, inclinacion = -45)
            }

            // MATERIAL 3 (Giro -30°, Incl +45°): Cartón, Papel
            materialUpper.contains("PAPER") || materialUpper.contains("PAPEL") ||
            materialUpper.contains("CARDBOARD") || materialUpper.contains("CARTON") ||
            materialUpper.contains("TETRA") -> {
                Log.d(TAG, "📄 Categoría: PAPEL/CARTÓN → Material 3")
                MovimientoMaterial(giro = -30, inclinacion = 45)
            }

            // MATERIAL 4 (Giro 59°, Incl -45°): Vidrio, Orgánico, Aluminio
            materialUpper.contains("GLASS") || materialUpper.contains("VIDRIO") ||
            materialUpper.contains("ORGANIC") || materialUpper.contains("ORGANICO") ||
            materialUpper.contains("ALUMINUM") || materialUpper.contains("ALUMINIO") -> {
                Log.d(TAG, "🍃 Categoría: VIDRIO/ORGÁNICO/ALUMINIO → Material 4")
                MovimientoMaterial(giro = 59, inclinacion = -45)
            }

            // MATERIAL 2 (Giro 59°, Incl +45°): Basura en general (todo lo demás)
            else -> {
                Log.d(TAG, "🗑️ Categoría: BASURA GENERAL → Material 2")
                MovimientoMaterial(giro = 59, inclinacion = 45)
            }
        }

        Log.d(TAG, "✅ Movimiento: GIRO=${movimiento.giro}°, INCL=${movimiento.inclinacion}°")
        return movimiento
    }

    /**
     * Enviar comando al ESP32 y esperar respuesta OK
     */
    private fun enviarComandoYEsperar(comando: String, timeoutMs: Long = 5000L): Boolean {
        try {
            Log.d(TAG, "📤 Enviando: $comando")
            outputStream?.write("$comando\n".toByteArray())
            outputStream?.flush()

            val buffer = ByteArray(1024)
            val startTime = System.currentTimeMillis()

            while (System.currentTimeMillis() - startTime < timeoutMs) {
                if (inputStream?.available() ?: 0 > 0) {
                    val bytes = inputStream?.read(buffer)
                    val response = String(buffer, 0, bytes ?: 0).trim()
                    Log.d(TAG, "📥 Respuesta: $response")

                    if (response == "OK" || response == "PONG") {
                        return true
                    }
                }
                Thread.sleep(50)
            }

            Log.w(TAG, "⚠️ Timeout esperando respuesta para: $comando")
            return false

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error enviando comando: ${e.message}")
            return false
        }
    }

    /**
     * Conectar al ESP32 automáticamente con handshake
     */
    @SuppressLint("MissingPermission")
    suspend fun conectarConHandshake(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Verificar permisos de Bluetooth primero
            if (context != null && !hasBluetoothPermissions(context)) {
                Log.e(TAG, "═══════════════════════════════════════")
                Log.e(TAG, "❌ ERROR: Permisos de Bluetooth no otorgados")
                Log.e(TAG, "   Se requieren BLUETOOTH_CONNECT y BLUETOOTH_SCAN")
                Log.e(TAG, "═══════════════════════════════════════")
                return@withContext Result.failure(Exception("Permisos de Bluetooth no otorgados. Por favor otorgue los permisos en configuración."))
            }

            Log.d(TAG, "═══════════════════════════════════════")
            Log.d(TAG, "🔍 Iniciando conexión automática...")
            Log.d(TAG, "═══════════════════════════════════════")

            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter == null) {
                return@withContext Result.failure(Exception("Bluetooth no disponible en este dispositivo"))
            }

            if (!bluetoothAdapter!!.isEnabled) {
                Log.w(TAG, "⚠️ Bluetooth desactivado. Intentando habilitar...")
                bluetoothAdapter!!.enable()
                Thread.sleep(2000)  // Esperar a que se active
            }

            // Buscar ESP32 en dispositivos vinculados
            Log.d(TAG, "🔎 Buscando ESP32_Detector...")
            val pairedDevices = bluetoothAdapter!!.bondedDevices
            val esp32 = pairedDevices.find { it.name == ESP32_NAME }

            if (esp32 == null) {
                Log.e(TAG, "❌ ESP32_Detector no encontrado")
                Log.d(TAG, "📱 Dispositivos vinculados:")
                pairedDevices.forEach {
                    Log.d(TAG, "  - ${it.name} (${it.address})")
                }
                return@withContext Result.failure(Exception("ESP32_Detector no encontrado en dispositivos vinculados"))
            }

            Log.d(TAG, "✅ ESP32 encontrado: ${esp32.name} (${esp32.address})")
            Log.d(TAG, "🔌 Conectando...")

            // Crear socket y conectar
            bluetoothSocket = esp32.createRfcommSocketToServiceRecord(UUID_SPP)
            bluetoothSocket?.connect()

            inputStream = bluetoothSocket?.inputStream
            outputStream = bluetoothSocket?.outputStream

            Log.d(TAG, "✅ Socket conectado")

            // Handshake: enviar PING y esperar PONG
            Log.d(TAG, "🤝 Iniciando handshake...")
            outputStream?.write("PING\n".toByteArray())
            outputStream?.flush()

            val buffer = ByteArray(1024)
            val timeoutMs = 5000L
            val startTime = System.currentTimeMillis()

            while (System.currentTimeMillis() - startTime < timeoutMs) {
                if (inputStream?.available() ?: 0 > 0) {
                    val bytes = inputStream?.read(buffer)
                    val response = String(buffer, 0, bytes ?: 0).trim()

                    if (response == "PONG") {
                        Log.d(TAG, "═══════════════════════════════════════")
                        Log.d(TAG, "✅ HANDSHAKE EXITOSO")
                        Log.d(TAG, "📡 ESP32 confirmó conexión")
                        Log.d(TAG, "═══════════════════════════════════════")
                        return@withContext Result.success(Unit)
                    }
                }
                Thread.sleep(100)
            }

            Result.failure(Exception("ESP32 no respondió al handshake"))

        } catch (e: Exception) {
            Log.e(TAG, "═══════════════════════════════════════")
            Log.e(TAG, "❌ ERROR DE CONEXIÓN")
            Log.e(TAG, "  ${e.message}")
            Log.e(TAG, "═══════════════════════════════════════")
            Result.failure(e)
        }
    }

    /**
     * Ejecutar secuencia completa de depósito de material
     * Secuencia: GIRO → INCL → Mantener 400ms → INCL:0 → GIRO:-80
     */
    suspend fun enviarMaterial(material: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (bluetoothSocket?.isConnected != true) {
                return@withContext Result.failure(Exception("No conectado al ESP32"))
            }

            // Obtener movimiento específico para este material
            val movimiento = obtenerMovimientoParaMaterial(material)

            Log.d(TAG, "═══════════════════════════════════════")
            Log.d(TAG, "🔄 EJECUTANDO DEPÓSITO DE MATERIAL")
            Log.d(TAG, "  Material: $material")
            Log.d(TAG, "  Giro: ${movimiento.giro}°")
            Log.d(TAG, "  Inclinación: ${movimiento.inclinacion}°")
            Log.d(TAG, "═══════════════════════════════════════")

            // Paso 1: Girar a posición
            Log.d(TAG, "📍 Paso 1: Girando a ${movimiento.giro}°...")
            if (!enviarComandoYEsperar("GIRO:${movimiento.giro}")) {
                return@withContext Result.failure(Exception("Error en GIRO"))
            }
            Thread.sleep(1000) // Esperar que llegue

            // Paso 2: Inclinar
            Log.d(TAG, "📍 Paso 2: Inclinando a ${movimiento.inclinacion}°...")
            if (!enviarComandoYEsperar("INCL:${movimiento.inclinacion}")) {
                return@withContext Result.failure(Exception("Error en INCL"))
            }
            Thread.sleep(1000) // Esperar que llegue

            // Paso 3: Mantener posición (depositando)
            Log.d(TAG, "📍 Paso 3: Depositando material...")
            Thread.sleep(400)

            // Paso 4: Volver inclinación a 0
            Log.d(TAG, "📍 Paso 4: Volviendo inclinación a 0°...")
            if (!enviarComandoYEsperar("INCL:0")) {
                return@withContext Result.failure(Exception("Error en INCL:0"))
            }
            Thread.sleep(1000) // Esperar que llegue

            // Paso 5: Volver giro a posición inicial (-80 = servo real 0°)
            Log.d(TAG, "📍 Paso 5: Volviendo giro a posición inicial...")
            if (!enviarComandoYEsperar("GIRO:-80")) {
                return@withContext Result.failure(Exception("Error en GIRO:-80"))
            }
            Thread.sleep(500) // Esperar que llegue

            Log.d(TAG, "═══════════════════════════════════════")
            Log.d(TAG, "✅ DEPÓSITO COMPLETADO")
            Log.d(TAG, "═══════════════════════════════════════")

            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en secuencia de depósito: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * NUEVO MÉTODO v2: Depositar material y esperar señal LISTO del ESP32
     *
     * Protocolo de comunicación bidireccional:
     * 1. Android envía: DEPOSITAR:GIRO,INCL (valores numéricos)
     * 2. ESP32 ejecuta secuencia completa (GIRO→INCL→depositar→RESET)
     * 3. ESP32 responde: LISTO
     * 4. Este método retorna cuando recibe LISTO
     *
     * @param categoria Nombre de la categoría (Plástico, Papel/Cartón, Aluminio/Metal, General)
     * @param giro Valor de giro (-80 a 160)
     * @param inclinacion Valor de inclinación (-45 a 45)
     * @return Result.success cuando ESP32 confirma LISTO, Result.failure si timeout o error
     */
    suspend fun depositarYEsperarListo(
        categoria: String,
        giro: Int,
        inclinacion: Int
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (bluetoothSocket?.isConnected != true) {
                return@withContext Result.failure(Exception("No conectado al ESP32"))
            }

            Log.d(TAG, "═══════════════════════════════════════")
            Log.d(TAG, "🎯 PROTOCOLO v2: DEPOSITAR Y ESPERAR LISTO")
            Log.d(TAG, "   Categoría: $categoria")
            Log.d(TAG, "   Giro: $giro°")
            Log.d(TAG, "   Inclinación: $inclinacion°")
            Log.d(TAG, "═══════════════════════════════════════")

            // Enviar comando DEPOSITAR:GIRO,INCL con valores numéricos
            val comando = "DEPOSITAR:$giro,$inclinacion"
            Log.d(TAG, "📤 Enviando: $comando")
            outputStream?.write("$comando\n".toByteArray())
            outputStream?.flush()

            // Esperar respuesta LISTO del ESP32
            Log.d(TAG, "⏳ Esperando señal LISTO del ESP32...")
            val buffer = ByteArray(1024)
            val startTime = System.currentTimeMillis()

            while (System.currentTimeMillis() - startTime < LISTO_TIMEOUT_MS) {
                if (inputStream?.available() ?: 0 > 0) {
                    val bytes = inputStream?.read(buffer)
                    val response = String(buffer, 0, bytes ?: 0).trim()
                    Log.d(TAG, "📥 Respuesta: $response")

                    if (response == "LISTO") {
                        val elapsed = System.currentTimeMillis() - startTime
                        Log.d(TAG, "═══════════════════════════════════════")
                        Log.d(TAG, "✅ SEÑAL LISTO RECIBIDA")
                        Log.d(TAG, "   Tiempo total: ${elapsed}ms")
                        Log.d(TAG, "═══════════════════════════════════════")
                        return@withContext Result.success(Unit)
                    }
                }
                Thread.sleep(50)
            }

            // Timeout sin recibir LISTO
            Log.e(TAG, "═══════════════════════════════════════")
            Log.e(TAG, "⚠️ TIMEOUT esperando LISTO")
            Log.e(TAG, "   Tiempo máximo: ${LISTO_TIMEOUT_MS}ms")
            Log.e(TAG, "═══════════════════════════════════════")
            Result.failure(Exception("Timeout esperando LISTO del ESP32"))

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en depositarYEsperarListo: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Desconectar del ESP32
     */
    fun desconectar() {
        try {
            inputStream?.close()
            outputStream?.close()
            bluetoothSocket?.close()
            Log.d(TAG, "🔌 Desconectado del ESP32")
        } catch (e: IOException) {
            Log.e(TAG, "Error al desconectar: ${e.message}")
        }
    }

    /**
     * Verificar si está conectado
     */
    fun estaConectado(): Boolean {
        return bluetoothSocket?.isConnected == true
    }
}
