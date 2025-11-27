package com.biowaymexico.data.models

import com.google.firebase.Timestamp

/**
 * Modelo de datos del Brindador
 */
data class BrindadorModel(
    val userId: String = "",
    val nombre: String = "",
    val email: String = "",
    val telefono: String = "",
    val tipoUsuario: String = "Brindador",
    val platform: String = "android",
    val bioCoins: Int = 0,
    val nivel: String = "Bronce",
    val totalKgReciclados: Double = 0.0,
    val materialesReciclados: Map<String, Double> = emptyMap(),  // materialId -> kg totales
    val bioImpulso: Int = 1,
    val bioImpulsoActivo: Boolean = false,
    val fechaRegistro: Timestamp? = null,
    val ultimaActividad: Timestamp? = null,
    val telefonoVerificado: Boolean = false,
    val emailVerificado: Boolean = false
)

@Suppress("UNCHECKED_CAST")
fun Map<String, Any?>.toBrindadorModel(): BrindadorModel {
    return BrindadorModel(
        userId = this["userId"] as? String ?: "",
        nombre = this["nombre"] as? String ?: "",
        email = this["email"] as? String ?: "",
        telefono = this["telefono"] as? String ?: "",
        tipoUsuario = this["tipoUsuario"] as? String ?: "Brindador",
        platform = this["platform"] as? String ?: "android",
        bioCoins = (this["bioCoins"] as? Long)?.toInt() ?: 0,
        nivel = this["nivel"] as? String ?: "Bronce",
        totalKgReciclados = (this["totalKgReciclados"] as? Number)?.toDouble() ?: 0.0,
        materialesReciclados = (this["materialesReciclados"] as? Map<String, Any>)?.mapValues {
            (it.value as? Number)?.toDouble() ?: 0.0
        } ?: emptyMap(),
        bioImpulso = (this["bioImpulso"] as? Long)?.toInt() ?: 1,
        bioImpulsoActivo = this["bioImpulsoActivo"] as? Boolean ?: false,
        fechaRegistro = this["fechaRegistro"] as? Timestamp,
        ultimaActividad = this["ultimaActividad"] as? Timestamp,
        telefonoVerificado = this["telefonoVerificado"] as? Boolean ?: false,
        emailVerificado = this["emailVerificado"] as? Boolean ?: false
    )
}
