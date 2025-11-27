package com.biowaymexico.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Repositorio para registrar reciclajes
 */
class ReciclajeRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    /**
     * Registra un reciclaje y actualiza solo kg totales y por material
     */
    suspend fun registrarReciclaje(
        materiales: List<MaterialReciclado>
    ): Result<ReciclajeSummary> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("Usuario no autenticado"))

            // Calcular total de kg y BioCoins
            var totalKg = 0.0
            var totalBioCoins = 0

            materiales.forEach { mat ->
                totalKg += mat.cantidad
                totalBioCoins += (mat.cantidad * 10).toInt()  // 10 BioCoins por kg
            }

            // Obtener datos actuales del brindador
            val brindadorRef = firestore.collection("Brindador").document(userId)
            val brindadorDoc = brindadorRef.get().await()

            val kgActuales = (brindadorDoc.getDouble("totalKgReciclados") ?: 0.0)
            val bioCoinsActuales = (brindadorDoc.getLong("bioCoins") ?: 0).toInt()

            @Suppress("UNCHECKED_CAST")
            val materialesActuales = (brindadorDoc.get("materialesReciclados") as? Map<String, Any>)?.mapValues {
                (it.value as? Number)?.toDouble() ?: 0.0
            }?.toMutableMap() ?: mutableMapOf()

            // Actualizar cantidades por material
            materiales.forEach { mat ->
                val cantidadActual = materialesActuales[mat.materialId] ?: 0.0
                materialesActuales[mat.materialId] = cantidadActual + mat.cantidad
            }

            val nuevoTotalKg = kgActuales + totalKg
            val nuevosBioCoins = bioCoinsActuales + totalBioCoins

            // Determinar nuevo nivel
            val nuevoNivel = when {
                nuevosBioCoins >= 10000 -> "Diamante"
                nuevosBioCoins >= 5000 -> "Platino"
                nuevosBioCoins >= 2000 -> "Oro"
                nuevosBioCoins >= 500 -> "Plata"
                else -> "Bronce"
            }

            // Actualizar en Firestore
            brindadorRef.update(
                mapOf(
                    "totalKgReciclados" to nuevoTotalKg,
                    "materialesReciclados" to materialesActuales,
                    "bioCoins" to nuevosBioCoins,
                    "nivel" to nuevoNivel
                )
            ).await()

            android.util.Log.d("ReciclajeRepository", "✅ Reciclaje registrado: +${totalKg}kg, +${totalBioCoins} BioCoins")

            Result.success(
                ReciclajeSummary(
                    kgReciclados = totalKg,
                    bioCoinsGanados = totalBioCoins,
                    nuevoNivel = nuevoNivel
                )
            )

        } catch (e: Exception) {
            android.util.Log.e("ReciclajeRepository", "❌ Error al registrar reciclaje: ${e.message}", e)
            Result.failure(e)
        }
    }
}

data class MaterialReciclado(
    val materialId: String,  // ID del material en Firebase
    val nombre: String,
    val cantidad: Double  // en kg
)

data class ReciclajeSummary(
    val kgReciclados: Double,
    val bioCoinsGanados: Int,
    val nuevoNivel: String
)
