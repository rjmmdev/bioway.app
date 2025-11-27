package com.biowaymexico.data

import android.util.Log
import com.biowaymexico.data.models.BrindadorModel
import com.biowaymexico.data.models.MaterialSesion
import com.biowaymexico.data.models.SesionActiva
import com.biowaymexico.data.models.toBrindadorModel
import com.biowaymexico.data.models.toSesionActiva
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Repositorio para gestionar puntos de reciclaje en BoteBioWay
 *
 * Este repositorio es usado por el celular del bote para:
 * 1. Leer datos del brindador (por Firebase UID escaneado via NFC)
 * 2. Otorgar puntos al brindador directamente (sin sesiones)
 *
 * NOTA: No se almacenan sesiones. Los puntos se otorgan directamente al brindador.
 */
class BoteSesionRepository {

    companion object {
        private const val TAG = "BoteSesionRepository"

        // Sistema de puntos por material
        val PUNTOS_POR_MATERIAL = mapOf(
            "plastico" to 10,
            "plastic" to 10,
            "plastic-pet" to 12,
            "plastic-pe_hd" to 10,
            "plastic-pp" to 10,
            "plastic-ps" to 8,
            "plastic-others" to 6,
            "vidrio" to 8,
            "glass" to 8,
            "metal" to 12,
            "aluminio" to 12,
            "carton" to 5,
            "cardboard" to 5,
            "papel" to 3,
            "paper" to 3,
            "organico" to 2,
            "biological" to 2,
            "basura" to 1,
            "trash" to 1,
            "general" to 1
        )

        // Peso por material (en kg) - cada item pesa 5g = 0.005 kg
        const val PESO_POR_ITEM_KG = 0.005
    }

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    /**
     * Obtiene los datos de un brindador por su Firebase UID
     * Usado cuando el bote escanea el NFC del usuario
     */
    suspend fun obtenerBrindadorPorId(brindadorId: String): Result<BrindadorModel> {
        return try {
            Log.d(TAG, "Buscando brindador: $brindadorId")

            val doc = firestore.collection("Brindador")
                .document(brindadorId)
                .get()
                .await()

            if (doc.exists()) {
                val data = doc.data ?: return Result.failure(Exception("Datos vacíos"))
                val brindador = data.toBrindadorModel()
                Log.d(TAG, "✅ Brindador encontrado: ${brindador.nombre}")
                Result.success(brindador)
            } else {
                Log.w(TAG, "⚠️ Brindador no encontrado: $brindadorId")
                Result.failure(Exception("Usuario no encontrado"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al buscar brindador: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Calcula los puntos para un tipo de material
     */
    fun calcularPuntos(tipoMaterial: String): Int {
        val tipo = tipoMaterial.lowercase().trim()
        return PUNTOS_POR_MATERIAL[tipo] ?: 1
    }

    /**
     * Otorga puntos a un brindador por un material depositado
     * Esta función actualiza directamente el brindador sin crear sesiones
     *
     * @param brindadorId Firebase UID del brindador (escaneado via NFC)
     * @param tipoMaterial Tipo de material clasificado (plastico, metal, etc.)
     * @return Result con los nuevos bioCoins del brindador o error
     */
    suspend fun otorgarPuntosPorMaterial(
        brindadorId: String,
        tipoMaterial: String
    ): Result<OtorgamientoResult> {
        return try {
            // Verificar que el bote está autenticado
            val boteId = auth.currentUser?.uid
            if (boteId == null) {
                Log.e(TAG, "❌ Bote no autenticado")
                return Result.failure(Exception("Bote no autenticado"))
            }

            // Verificar que es un BoteBioWay válido
            if (!verificarEsBoteBioWay()) {
                Log.e(TAG, "❌ Usuario no es un BoteBioWay válido")
                return Result.failure(Exception("No autorizado como BoteBioWay"))
            }

            val puntos = calcularPuntos(tipoMaterial)
            val pesoKg = PESO_POR_ITEM_KG  // 5 gramos = 0.005 kg

            Log.d(TAG, "═══════════════════════════════════════")
            Log.d(TAG, "📝 OTORGANDO PUNTOS")
            Log.d(TAG, "   Brindador: $brindadorId")
            Log.d(TAG, "   Material: $tipoMaterial")
            Log.d(TAG, "   Puntos: $puntos")
            Log.d(TAG, "   Peso: ${pesoKg * 1000}g")
            Log.d(TAG, "═══════════════════════════════════════")

            // Obtener datos actuales del brindador
            val brindadorRef = firestore.collection("Brindador").document(brindadorId)
            val brindadorDoc = brindadorRef.get().await()

            if (!brindadorDoc.exists()) {
                return Result.failure(Exception("Brindador no existe"))
            }

            // Leer valores actuales
            val bioCoinsActuales = (brindadorDoc.getLong("bioCoins") ?: 0).toInt()
            val kgActuales = brindadorDoc.getDouble("totalKgReciclados") ?: 0.0

            @Suppress("UNCHECKED_CAST")
            val materialesActuales = (brindadorDoc.get("materialesReciclados") as? Map<String, Any>)
                ?.mapValues { (it.value as? Number)?.toDouble() ?: 0.0 }
                ?.toMutableMap() ?: mutableMapOf()

            // Calcular nuevos valores
            val nuevosBioCoins = bioCoinsActuales + puntos
            val nuevoTotalKg = kgActuales + pesoKg

            // Actualizar materiales reciclados por tipo
            val tipoNormalizado = tipoMaterial.lowercase()
            val kgActualTipo = materialesActuales[tipoNormalizado] ?: 0.0
            materialesActuales[tipoNormalizado] = kgActualTipo + pesoKg

            // Determinar nuevo nivel basado en bioCoins
            val nuevoNivel = when {
                nuevosBioCoins >= 10000 -> "Diamante"
                nuevosBioCoins >= 5000 -> "Platino"
                nuevosBioCoins >= 2000 -> "Oro"
                nuevosBioCoins >= 500 -> "Plata"
                else -> "Bronce"
            }

            // Actualizar brindador en Firestore (solo campos permitidos por las reglas)
            brindadorRef.update(
                mapOf(
                    "bioCoins" to nuevosBioCoins,
                    "nivel" to nuevoNivel,
                    "totalKgReciclados" to nuevoTotalKg,
                    "materialesReciclados" to materialesActuales,
                    "ultimaActividad" to FieldValue.serverTimestamp()
                )
            ).await()

            Log.d(TAG, "✅ Brindador actualizado:")
            Log.d(TAG, "   BioCoins: $bioCoinsActuales → $nuevosBioCoins (+$puntos)")
            Log.d(TAG, "   Kg: $kgActuales → $nuevoTotalKg (+$pesoKg)")
            Log.d(TAG, "   Nivel: $nuevoNivel")
            Log.d(TAG, "═══════════════════════════════════════")

            Result.success(
                OtorgamientoResult(
                    puntosOtorgados = puntos,
                    nuevoTotalBioCoins = nuevosBioCoins,
                    pesoKgOtorgado = pesoKg,
                    nuevoTotalKg = nuevoTotalKg,
                    nuevoNivel = nuevoNivel
                )
            )

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al otorgar puntos: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Verifica si el usuario actual es un BoteBioWay válido
     */
    suspend fun verificarEsBoteBioWay(): Boolean {
        return try {
            val userId = auth.currentUser?.uid ?: return false
            val doc = firestore.collection("BoteBioWay").document(userId).get().await()
            doc.exists()
        } catch (e: Exception) {
            Log.e(TAG, "Error verificando BoteBioWay: ${e.message}")
            false
        }
    }

    /**
     * Obtiene la información del bote actual
     */
    suspend fun obtenerInfoBoteActual(): Result<Map<String, Any>> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("No autenticado"))

            val doc = firestore.collection("BoteBioWay").document(userId).get().await()

            if (doc.exists()) {
                Result.success(doc.data ?: emptyMap())
            } else {
                Result.failure(Exception("Bote no encontrado"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MANEJO DE SESIONES ACTIVAS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Verifica si un brindador tiene una sesión activa
     */
    suspend fun verificarSesionActiva(brindadorId: String): Result<SesionActiva?> {
        return try {
            val doc = firestore.collection("Brindador")
                .document(brindadorId)
                .get()
                .await()

            if (!doc.exists()) {
                return Result.failure(Exception("Brindador no encontrado"))
            }

            @Suppress("UNCHECKED_CAST")
            val sesionData = doc.get("sesionActiva") as? Map<String, Any?>

            if (sesionData != null) {
                val sesion = sesionData.toSesionActiva()
                // Verificar que la sesión esté activa
                if (sesion.estado == SesionActiva.ESTADO_ACTIVA) {
                    Result.success(sesion)
                } else {
                    Result.success(null)
                }
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error verificando sesión activa: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Inicia una nueva sesión de reciclaje para un brindador
     * @param brindadorId Firebase UID del brindador
     * @param boteNombre Nombre del bote (opcional, para mostrar al usuario)
     * @return Result con la sesión creada o error si ya hay una activa
     */
    suspend fun iniciarSesion(
        brindadorId: String,
        boteNombre: String = "Bote BioWay"
    ): Result<SesionActiva> {
        return try {
            val boteId = auth.currentUser?.uid
                ?: return Result.failure(Exception("Bote no autenticado"))

            // Verificar que es un BoteBioWay válido
            if (!verificarEsBoteBioWay()) {
                return Result.failure(Exception("No autorizado como BoteBioWay"))
            }

            // Verificar si ya hay sesión activa
            val sesionExistente = verificarSesionActiva(brindadorId).getOrNull()
            if (sesionExistente != null) {
                Log.w(TAG, "⚠️ Ya existe sesión activa para brindador $brindadorId")
                return Result.failure(Exception("Ya existe una sesión activa"))
            }

            val ahora = Timestamp.now()
            val nuevaSesion = SesionActiva(
                boteId = boteId,
                boteNombre = boteNombre,
                inicioSesion = ahora,
                ultimaActividad = ahora,
                tiempoMaximoSegundos = 180,  // 3 minutos
                tiempoInactividadSegundos = 45,  // 45 segundos
                materialesDepositados = emptyList(),
                puntosAcumulados = 0,
                gramosAcumulados = 0,
                estado = SesionActiva.ESTADO_ACTIVA
            )

            // Guardar sesión en el documento del brindador
            firestore.collection("Brindador")
                .document(brindadorId)
                .update("sesionActiva", nuevaSesion.toMap())
                .await()

            Log.d(TAG, "═══════════════════════════════════════")
            Log.d(TAG, "✅ SESIÓN INICIADA")
            Log.d(TAG, "   Brindador: $brindadorId")
            Log.d(TAG, "   Bote: $boteNombre")
            Log.d(TAG, "   Tiempo máximo: 180s")
            Log.d(TAG, "═══════════════════════════════════════")

            Result.success(nuevaSesion)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error iniciando sesión: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Agrega un material a la sesión activa y otorga puntos
     * Esta función:
     * 1. Actualiza la sesión activa (materialesDepositados, puntosAcumulados, gramosAcumulados)
     * 2. Actualiza los campos permanentes del brindador (bioCoins, totalKgReciclados, etc.)
     */
    suspend fun agregarMaterialASesion(
        brindadorId: String,
        tipoMaterial: String,
        confianza: Float = 0f
    ): Result<OtorgamientoResult> {
        return try {
            val boteId = auth.currentUser?.uid
                ?: return Result.failure(Exception("Bote no autenticado"))

            // Verificar sesión activa
            val sesionResult = verificarSesionActiva(brindadorId)
            val sesionActual = sesionResult.getOrNull()
                ?: return Result.failure(Exception("No hay sesión activa"))

            // Verificar que la sesión pertenece a este bote
            if (sesionActual.boteId != boteId) {
                return Result.failure(Exception("La sesión pertenece a otro bote"))
            }

            val puntos = calcularPuntos(tipoMaterial)
            val gramos = SesionActiva.GRAMOS_POR_MATERIAL  // 60 gramos
            val pesoKg = gramos / 1000.0  // 0.06 kg

            val nuevoMaterial = MaterialSesion(
                tipo = tipoMaterial.lowercase(),
                puntos = puntos,
                gramos = gramos,
                confianza = confianza,
                timestamp = Timestamp.now()
            )

            // Obtener datos actuales del brindador
            val brindadorRef = firestore.collection("Brindador").document(brindadorId)
            val brindadorDoc = brindadorRef.get().await()

            val bioCoinsActuales = (brindadorDoc.getLong("bioCoins") ?: 0).toInt()
            val kgActuales = brindadorDoc.getDouble("totalKgReciclados") ?: 0.0

            @Suppress("UNCHECKED_CAST")
            val materialesActuales = (brindadorDoc.get("materialesReciclados") as? Map<String, Any>)
                ?.mapValues { (it.value as? Number)?.toDouble() ?: 0.0 }
                ?.toMutableMap() ?: mutableMapOf()

            // Calcular nuevos valores
            val nuevosBioCoins = bioCoinsActuales + puntos
            val nuevoTotalKg = kgActuales + pesoKg
            val tipoNormalizado = tipoMaterial.lowercase()
            materialesActuales[tipoNormalizado] = (materialesActuales[tipoNormalizado] ?: 0.0) + pesoKg

            // Determinar nuevo nivel
            val nuevoNivel = when {
                nuevosBioCoins >= 10000 -> "Diamante"
                nuevosBioCoins >= 5000 -> "Platino"
                nuevosBioCoins >= 2000 -> "Oro"
                nuevosBioCoins >= 500 -> "Plata"
                else -> "Bronce"
            }

            // Actualizar sesión activa con nuevo material
            val nuevosMateriales = sesionActual.materialesDepositados + nuevoMaterial
            val nuevaSesion = sesionActual.copy(
                materialesDepositados = nuevosMateriales,
                puntosAcumulados = sesionActual.puntosAcumulados + puntos,
                gramosAcumulados = sesionActual.gramosAcumulados + gramos,
                ultimaActividad = Timestamp.now()
            )

            // Actualizar todo en una sola operación
            brindadorRef.update(
                mapOf(
                    "bioCoins" to nuevosBioCoins,
                    "nivel" to nuevoNivel,
                    "totalKgReciclados" to nuevoTotalKg,
                    "materialesReciclados" to materialesActuales,
                    "ultimaActividad" to FieldValue.serverTimestamp(),
                    "sesionActiva" to nuevaSesion.toMap()
                )
            ).await()

            Log.d(TAG, "═══════════════════════════════════════")
            Log.d(TAG, "📦 MATERIAL AGREGADO A SESIÓN")
            Log.d(TAG, "   Tipo: $tipoMaterial")
            Log.d(TAG, "   Puntos: +$puntos (Total sesión: ${nuevaSesion.puntosAcumulados})")
            Log.d(TAG, "   Gramos: +$gramos (Total sesión: ${nuevaSesion.gramosAcumulados}g)")
            Log.d(TAG, "   BioCoins usuario: $nuevosBioCoins")
            Log.d(TAG, "═══════════════════════════════════════")

            Result.success(
                OtorgamientoResult(
                    puntosOtorgados = puntos,
                    nuevoTotalBioCoins = nuevosBioCoins,
                    pesoKgOtorgado = pesoKg,
                    nuevoTotalKg = nuevoTotalKg,
                    nuevoNivel = nuevoNivel
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error agregando material: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Finaliza la sesión activa de un brindador
     * Elimina el campo sesionActiva del documento
     * @param razon Razón de finalización (brindador, timeout, inactividad)
     */
    suspend fun finalizarSesion(
        brindadorId: String,
        razon: String = SesionActiva.ESTADO_FINALIZADA_TIMEOUT
    ): Result<SesionActiva> {
        return try {
            // Obtener sesión antes de eliminar (para retornar resumen)
            val sesionResult = verificarSesionActiva(brindadorId)
            val sesionFinal = sesionResult.getOrNull()

            if (sesionFinal == null) {
                Log.w(TAG, "⚠️ No hay sesión activa para finalizar")
                return Result.failure(Exception("No hay sesión activa"))
            }

            // Eliminar campo sesionActiva
            firestore.collection("Brindador")
                .document(brindadorId)
                .update("sesionActiva", FieldValue.delete())
                .await()

            Log.d(TAG, "═══════════════════════════════════════")
            Log.d(TAG, "🏁 SESIÓN FINALIZADA")
            Log.d(TAG, "   Razón: $razon")
            Log.d(TAG, "   Materiales: ${sesionFinal.materialesDepositados.size}")
            Log.d(TAG, "   Puntos totales: ${sesionFinal.puntosAcumulados}")
            Log.d(TAG, "   Gramos totales: ${sesionFinal.gramosAcumulados}g")
            Log.d(TAG, "═══════════════════════════════════════")

            // Retornar copia con estado actualizado
            Result.success(sesionFinal.copy(estado = razon))
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error finalizando sesión: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Actualiza solo el estado de la sesión (usado cuando el brindador finaliza)
     * Esta función marca la sesión como finalizada pero NO la elimina
     * El bote detectará el cambio y llamará a finalizarSesion()
     */
    suspend fun marcarSesionFinalizada(
        brindadorId: String,
        razon: String = SesionActiva.ESTADO_FINALIZADA_BRINDADOR
    ): Result<Unit> {
        return try {
            firestore.collection("Brindador")
                .document(brindadorId)
                .update("sesionActiva.estado", razon)
                .await()

            Log.d(TAG, "✅ Sesión marcada como finalizada: $razon")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error marcando sesión: ${e.message}")
            Result.failure(e)
        }
    }
}

/**
 * Resultado de otorgar puntos a un brindador
 */
data class OtorgamientoResult(
    val puntosOtorgados: Int,
    val nuevoTotalBioCoins: Int,
    val pesoKgOtorgado: Double,
    val nuevoTotalKg: Double,
    val nuevoNivel: String
)
