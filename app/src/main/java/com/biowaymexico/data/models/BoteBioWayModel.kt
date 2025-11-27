package com.biowaymexico.data.models

import com.google.firebase.Timestamp

data class BoteBioWayModel(
    val userId: String = "",
    val identificador: String = "",
    val email: String = "",
    val estado: String = "",
    val municipio: String = "",
    val colonia: String = "",
    val codigoPostal: String = "",
    val latitud: Double? = null,
    val longitud: Double? = null,
    val tipoUsuario: String = "BoteBioWay",
    val estadoOperativo: Boolean = true,
    val fechaRegistro: Timestamp? = null
)

@Suppress("UNCHECKED_CAST")
fun Map<String, Any?>.toBoteBioWayModel(): BoteBioWayModel {
    return BoteBioWayModel(
        userId = this["userId"] as? String ?: "",
        identificador = this["identificador"] as? String ?: "",
        email = this["email"] as? String ?: "",
        estado = this["estado"] as? String ?: "",
        municipio = this["municipio"] as? String ?: "",
        colonia = this["colonia"] as? String ?: "",
        codigoPostal = this["codigoPostal"] as? String ?: "",
        latitud = (this["latitud"] as? Number)?.toDouble(),
        longitud = (this["longitud"] as? Number)?.toDouble(),
        tipoUsuario = this["tipoUsuario"] as? String ?: "BoteBioWay",
        estadoOperativo = this["estadoOperativo"] as? Boolean ?: true,
        fechaRegistro = this["fechaRegistro"] as? Timestamp
    )
}
