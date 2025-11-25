package com.biowaymexico.nfc

import android.content.Intent
import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log
import java.nio.charset.Charset

/**
 * Servicio de Host Card Emulation (HCE) para BioWay
 * Permite que el dispositivo actúe como una tarjeta NFC virtual
 * que puede ser leída por otro dispositivo
 */
class BioWayHceService : HostApduService() {

    companion object {
        private const val TAG = "BioWayHceService"

        // AID (Application ID) - Identificador único de la aplicación NFC
        // F0010203040506 - AID personalizado para BioWay
        private val AID = byteArrayOf(
            0xF0.toByte(), 0x01, 0x02, 0x03, 0x04, 0x05, 0x06
        )

        // Comandos APDU estándar
        private val SELECT_APDU_HEADER = byteArrayOf(
            0x00.toByte(), // CLA (Class)
            0xA4.toByte(), // INS (Instruction) - SELECT
            0x04.toByte(), // P1 (Parameter 1)
            0x00.toByte()  // P2 (Parameter 2)
        )

        // Comando personalizado para obtener User ID
        private val GET_USER_ID_COMMAND = byteArrayOf(
            0x00.toByte(), // CLA
            0xCA.toByte(), // INS - GET DATA personalizado
            0x00.toByte(), // P1
            0x00.toByte()  // P2
        )

        // Respuestas APDU estándar
        private val SUCCESS_SW = byteArrayOf(
            0x90.toByte(), 0x00.toByte() // SW1 SW2 - Success
        )

        private val UNKNOWN_CMD_SW = byteArrayOf(
            0x00.toByte(), 0x00.toByte() // Unknown command
        )

        // Variable estática para compartir el User ID entre el servicio y la Activity
        @Volatile
        var currentUserId: String = ""
    }

    /**
     * Procesa los comandos APDU recibidos del lector NFC
     * Este método es llamado cuando otro dispositivo (lector) envía comandos
     */
    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        Log.d(TAG, "=== processCommandApdu llamado ===")

        if (commandApdu == null) {
            Log.w(TAG, "⚠️ commandApdu es null")
            return UNKNOWN_CMD_SW
        }

        Log.d(TAG, "Comando recibido: ${commandApdu.toHexString()}")
        Log.d(TAG, "Longitud: ${commandApdu.size} bytes")

        // Verificar si es un comando SELECT de nuestra AID
        if (commandApdu.size >= 5 && isSelectAidCommand(commandApdu)) {
            Log.d(TAG, "✅ Comando SELECT AID recibido")
            // Responder con éxito - la aplicación fue seleccionada
            return SUCCESS_SW
        }

        // Verificar si es el comando GET_USER_ID
        if (isGetUserIdCommand(commandApdu)) {
            Log.d(TAG, "✅ Comando GET_USER_ID recibido")

            if (currentUserId.isEmpty()) {
                Log.w(TAG, "⚠️ User ID no está configurado")
                return UNKNOWN_CMD_SW
            }

            Log.d(TAG, "Enviando User ID: $currentUserId")

            // Convertir el User ID a bytes y agregarlo a la respuesta
            val userIdBytes = currentUserId.toByteArray(Charset.forName("UTF-8"))
            val response = ByteArray(userIdBytes.size + SUCCESS_SW.size)

            // Copiar el User ID
            System.arraycopy(userIdBytes, 0, response, 0, userIdBytes.size)
            // Agregar status word de éxito al final
            System.arraycopy(SUCCESS_SW, 0, response, userIdBytes.size, SUCCESS_SW.size)

            Log.d(TAG, "✅ Respuesta enviada: ${response.toHexString()}")
            return response
        }

        Log.w(TAG, "⚠️ Comando no reconocido")
        return UNKNOWN_CMD_SW
    }

    /**
     * Llamado cuando la sesión NFC es desactivada
     */
    override fun onDeactivated(reason: Int) {
        val reasonText = when (reason) {
            DEACTIVATION_LINK_LOSS -> "Pérdida de enlace"
            DEACTIVATION_DESELECTED -> "Deseleccionado"
            else -> "Otro ($reason)"
        }
        Log.d(TAG, "🔴 Sesión NFC desactivada. Razón: $reasonText")

        // Broadcast para notificar a la Activity
        val intent = Intent("com.biowaymexico.NFC_SESSION_END")
        sendBroadcast(intent)
    }

    /**
     * Verifica si el comando es SELECT AID
     */
    private fun isSelectAidCommand(commandApdu: ByteArray): Boolean {
        // Verificar header SELECT
        if (commandApdu.size < 5) return false

        for (i in SELECT_APDU_HEADER.indices) {
            if (commandApdu[i] != SELECT_APDU_HEADER[i]) {
                return false
            }
        }

        // Verificar que incluya nuestro AID
        val aidLength = commandApdu[4].toInt()
        if (commandApdu.size < 5 + aidLength) return false

        val receivedAid = commandApdu.copyOfRange(5, 5 + aidLength)
        return receivedAid.contentEquals(AID)
    }

    /**
     * Verifica si el comando es GET_USER_ID
     */
    private fun isGetUserIdCommand(commandApdu: ByteArray): Boolean {
        if (commandApdu.size < GET_USER_ID_COMMAND.size) return false

        for (i in GET_USER_ID_COMMAND.indices) {
            if (commandApdu[i] != GET_USER_ID_COMMAND[i]) {
                return false
            }
        }
        return true
    }

    /**
     * Extensión para convertir ByteArray a String hexadecimal
     */
    private fun ByteArray.toHexString(): String {
        return joinToString(" ") { "%02X".format(it) }
    }
}
