package com.biowaymexico.data.models

import com.google.firebase.Timestamp

/**
 * Modelo para una sesión de usuario en un BoteBioWay
 * Se guarda como subcolección en Brindador/{userId}/SesionesBote/{sessionId}
 */
data class SesionBoteModel(
    val sessionId: String = "",
    val boteId: String = "",                    // userId del BoteBioWay
    val boteIdentificador: String = "",         // Nombre legible del bote
    val brindadorId: String = "",               // userId del Brindador
    val fechaInicio: Timestamp? = null,
    val fechaFin: Timestamp? = null,
    val duracionSegundos: Int = 0,
    val materialesDepositados: List<MaterialDepositado> = emptyList(),
    val totalBioCoins: Int = 0,
    val ubicacion: UbicacionBote? = null
)

/**
 * Material individual depositado durante una sesión
 */
data class MaterialDepositado(
    val tipo: String = "",              // "plastico", "vidrio", etc.
    val cantidad: Int = 1,              // Número de items (por defecto 1)
    val bioCoins: Int = 0,              // Puntos otorgados por este material
    val confianza: Float = 0f,          // Confianza del modelo YOLO
    val timestamp: Timestamp? = null
)

/**
 * Ubicación simplificada del bote
 */
data class UbicacionBote(
    val estado: String = "",
    val municipio: String = "",
    val latitud: Double? = null,
    val longitud: Double? = null
)

/**
 * Sesión activa de reciclaje - Campo temporal en el documento del Brindador
 * Este campo se almacena en /Brindador/{userId}.sesionActiva
 * y se elimina cuando la sesión finaliza (los datos se consolidan en los campos permanentes)
 */
data class SesionActiva(
    val boteId: String = "",
    val boteNombre: String = "",
    val inicioSesion: Timestamp? = null,
    val ultimaActividad: Timestamp? = null,
    val tiempoMaximoSegundos: Int = 180,        // 3 minutos por defecto
    val tiempoInactividadSegundos: Int = 45,    // 45 segundos sin material
    val materialesDepositados: List<MaterialSesion> = emptyList(),
    val puntosAcumulados: Int = 0,
    val gramosAcumulados: Int = 0,
    val estado: String = "activa"  // "activa", "finalizada_por_brindador", "finalizada_por_timeout", "finalizada_por_inactividad"
) {
    companion object {
        const val ESTADO_ACTIVA = "activa"
        const val ESTADO_FINALIZADA_BRINDADOR = "finalizada_por_brindador"
        const val ESTADO_FINALIZADA_TIMEOUT = "finalizada_por_timeout"
        const val ESTADO_FINALIZADA_INACTIVIDAD = "finalizada_por_inactividad"

        const val GRAMOS_POR_MATERIAL = 60  // Cada material = 60 gramos
    }

    fun toMap(): Map<String, Any?> = mapOf(
        "boteId" to boteId,
        "boteNombre" to boteNombre,
        "inicioSesion" to inicioSesion,
        "ultimaActividad" to ultimaActividad,
        "tiempoMaximoSegundos" to tiempoMaximoSegundos,
        "tiempoInactividadSegundos" to tiempoInactividadSegundos,
        "materialesDepositados" to materialesDepositados.map { it.toMap() },
        "puntosAcumulados" to puntosAcumulados,
        "gramosAcumulados" to gramosAcumulados,
        "estado" to estado
    )
}

/**
 * Material depositado durante una sesión activa
 */
data class MaterialSesion(
    val tipo: String = "",
    val puntos: Int = 0,
    val gramos: Int = SesionActiva.GRAMOS_POR_MATERIAL,
    val confianza: Float = 0f,
    val timestamp: Timestamp? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "tipo" to tipo,
        "puntos" to puntos,
        "gramos" to gramos,
        "confianza" to confianza,
        "timestamp" to timestamp
    )
}

/**
 * Extensión para convertir Map a SesionActiva
 */
@Suppress("UNCHECKED_CAST")
fun Map<String, Any?>.toSesionActiva(): SesionActiva {
    val materialesRaw = this["materialesDepositados"] as? List<Map<String, Any?>> ?: emptyList()
    val materiales = materialesRaw.map { mat ->
        MaterialSesion(
            tipo = mat["tipo"] as? String ?: "",
            puntos = (mat["puntos"] as? Long)?.toInt() ?: 0,
            gramos = (mat["gramos"] as? Long)?.toInt() ?: SesionActiva.GRAMOS_POR_MATERIAL,
            confianza = (mat["confianza"] as? Number)?.toFloat() ?: 0f,
            timestamp = mat["timestamp"] as? Timestamp
        )
    }

    return SesionActiva(
        boteId = this["boteId"] as? String ?: "",
        boteNombre = this["boteNombre"] as? String ?: "",
        inicioSesion = this["inicioSesion"] as? Timestamp,
        ultimaActividad = this["ultimaActividad"] as? Timestamp,
        tiempoMaximoSegundos = (this["tiempoMaximoSegundos"] as? Long)?.toInt() ?: 180,
        tiempoInactividadSegundos = (this["tiempoInactividadSegundos"] as? Long)?.toInt() ?: 45,
        materialesDepositados = materiales,
        puntosAcumulados = (this["puntosAcumulados"] as? Long)?.toInt() ?: 0,
        gramosAcumulados = (this["gramosAcumulados"] as? Long)?.toInt() ?: 0,
        estado = this["estado"] as? String ?: SesionActiva.ESTADO_ACTIVA
    )
}

// Extensión para convertir Map a SesionBoteModel
@Suppress("UNCHECKED_CAST")
fun Map<String, Any?>.toSesionBoteModel(): SesionBoteModel {
    val materialesRaw = this["materialesDepositados"] as? List<Map<String, Any?>> ?: emptyList()
    val materiales = materialesRaw.map { mat ->
        MaterialDepositado(
            tipo = mat["tipo"] as? String ?: "",
            cantidad = (mat["cantidad"] as? Long)?.toInt() ?: 1,
            bioCoins = (mat["bioCoins"] as? Long)?.toInt() ?: 0,
            confianza = (mat["confianza"] as? Number)?.toFloat() ?: 0f,
            timestamp = mat["timestamp"] as? Timestamp
        )
    }

    val ubicacionRaw = this["ubicacion"] as? Map<String, Any?>
    val ubicacion = ubicacionRaw?.let {
        UbicacionBote(
            estado = it["estado"] as? String ?: "",
            municipio = it["municipio"] as? String ?: "",
            latitud = (it["latitud"] as? Number)?.toDouble(),
            longitud = (it["longitud"] as? Number)?.toDouble()
        )
    }

    return SesionBoteModel(
        sessionId = this["sessionId"] as? String ?: "",
        boteId = this["boteId"] as? String ?: "",
        boteIdentificador = this["boteIdentificador"] as? String ?: "",
        brindadorId = this["brindadorId"] as? String ?: "",
        fechaInicio = this["fechaInicio"] as? Timestamp,
        fechaFin = this["fechaFin"] as? Timestamp,
        duracionSegundos = (this["duracionSegundos"] as? Long)?.toInt() ?: 0,
        materialesDepositados = materiales,
        totalBioCoins = (this["totalBioCoins"] as? Long)?.toInt() ?: 0,
        ubicacion = ubicacion
    )
}
