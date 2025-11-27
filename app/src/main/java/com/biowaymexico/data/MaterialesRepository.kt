package com.biowaymexico.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Repositorio de Materiales Reciclables
 * Gestiona la colección Reciclables/ en Firestore
 * Solo el Maestro BioWay puede modificar
 */
class MaterialesRepository {

    private val firestore = FirebaseFirestore.getInstance()

    /**
     * Obtiene todos los materiales reciclables
     */
    suspend fun obtenerMateriales(): Result<List<MaterialReciclable>> {
        return try {
            val snapshot = firestore.collection("Reciclables").get().await()

            val materiales = snapshot.documents.mapNotNull { doc ->
                try {
                    @Suppress("UNCHECKED_CAST")
                    val detailedInfoMap = doc.get("detailedInfo") as? Map<String, Any>
                    val detailedInfo = detailedInfoMap?.let {
                        DetailedInfo(
                            siReciclables = (it["siReciclables"] as? List<String>) ?: emptyList(),
                            noReciclables = (it["noReciclables"] as? List<String>) ?: emptyList(),
                            consejos = (it["consejos"] as? List<String>) ?: emptyList()
                        )
                    }

                    MaterialReciclable(
                        id = doc.id,
                        nombre = doc.getString("nombre") ?: "",
                        info = doc.getString("info") ?: "",
                        cantMin = doc.getDouble("cantMin") ?: 1.0,
                        unidad = doc.getString("unidad") ?: "kg",
                        color = doc.getString("color") ?: "#70D162",
                        factorCO2 = doc.getDouble("factorCO2") ?: 0.0,
                        icon = doc.getString("icon") ?: "",
                        detailedInfo = detailedInfo
                    )
                } catch (e: Exception) {
                    null
                }
            }

            Result.success(materiales)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Agregar nuevo material (solo Maestro)
     */
    suspend fun agregarMaterial(material: MaterialReciclable): Result<Unit> {
        return try {
            val materialData = hashMapOf(
                "nombre" to material.nombre,
                "info" to material.info,
                "cantMin" to material.cantMin,
                "unidad" to material.unidad,
                "color" to material.color,
                "factorCO2" to material.factorCO2,
                "icon" to material.icon
            )

            firestore.collection("Reciclables")
                .document(material.id)
                .set(materialData)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Actualizar material existente (solo Maestro)
     */
    suspend fun actualizarMaterial(material: MaterialReciclable): Result<Unit> {
        return try {
            val materialData = hashMapOf(
                "nombre" to material.nombre,
                "info" to material.info,
                "cantMin" to material.cantMin,
                "unidad" to material.unidad,
                "color" to material.color,
                "factorCO2" to material.factorCO2,
                "icon" to material.icon
            )

            firestore.collection("Reciclables")
                .document(material.id)
                .update(materialData as Map<String, Any>)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Eliminar material (solo Maestro)
     */
    suspend fun eliminarMaterial(materialId: String): Result<Unit> {
        return try {
            firestore.collection("Reciclables")
                .document(materialId)
                .delete()
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class MaterialReciclable(
    val id: String,
    val nombre: String,
    val info: String,
    val cantMin: Double,
    val unidad: String,
    val color: String,
    val factorCO2: Double,
    val icon: String,
    val detailedInfo: DetailedInfo? = null
)

data class DetailedInfo(
    val siReciclables: List<String> = emptyList(),
    val noReciclables: List<String> = emptyList(),
    val consejos: List<String> = emptyList()
)
