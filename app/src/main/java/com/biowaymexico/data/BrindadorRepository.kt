package com.biowaymexico.data

import com.biowaymexico.data.models.BrindadorModel
import com.biowaymexico.data.models.toBrindadorModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Repositorio de Brindador
 * Gestiona las operaciones de lectura/escritura del perfil del Brindador
 */
class BrindadorRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    /**
     * Obtiene los datos del brindador actual
     */
    suspend fun obtenerBrindador(): Result<BrindadorModel> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("Usuario no autenticado"))

            val doc = firestore.collection("Brindador")
                .document(userId)
                .get()
                .await()

            if (doc.exists()) {
                val data = doc.data ?: return Result.failure(Exception("Datos vacíos"))
                Result.success(data.toBrindadorModel())
            } else {
                Result.failure(Exception("Brindador no encontrado"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Actualiza el nombre del brindador
     */
    suspend fun actualizarNombre(nombre: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("Usuario no autenticado"))

            firestore.collection("Brindador")
                .document(userId)
                .update("nombre", nombre)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Actualiza los BioCoins del brindador
     */
    suspend fun actualizarBioCoins(cantidad: Int): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("Usuario no autenticado"))

            firestore.collection("Brindador")
                .document(userId)
                .update("bioCoins", cantidad)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Incrementa los BioCoins del brindador
     */
    suspend fun incrementarBioCoins(cantidad: Int): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("Usuario no autenticado"))

            val doc = firestore.collection("Brindador")
                .document(userId)
                .get()
                .await()

            val bioCoinsActuales = (doc.getLong("bioCoins") ?: 0).toInt()
            val nuevoTotal = bioCoinsActuales + cantidad

            firestore.collection("Brindador")
                .document(userId)
                .update("bioCoins", nuevoTotal)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    /**
     * Actualiza el nivel del brindador basado en BioCoins
     */
    suspend fun actualizarNivel(): Result<String> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("Usuario no autenticado"))

            val doc = firestore.collection("Brindador")
                .document(userId)
                .get()
                .await()

            val bioCoins = (doc.getLong("bioCoins") ?: 0).toInt()

            // Determinar nivel según BioCoins
            val nuevoNivel = when {
                bioCoins >= 10000 -> "Diamante"
                bioCoins >= 5000 -> "Platino"
                bioCoins >= 2000 -> "Oro"
                bioCoins >= 500 -> "Plata"
                else -> "Bronce"
            }

            firestore.collection("Brindador")
                .document(userId)
                .update("nivel", nuevoNivel)
                .await()

            Result.success(nuevoNivel)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Activa o desactiva el BioImpulso
     */
    suspend fun toggleBioImpulso(activo: Boolean): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("Usuario no autenticado"))

            firestore.collection("Brindador")
                .document(userId)
                .update("bioImpulsoActivo", activo)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
