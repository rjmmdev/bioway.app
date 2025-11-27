package com.biowaymexico.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Debugger de Firestore - Solo para Maestro BioWay
 * Imprime toda la estructura de la base de datos en logs
 */
object FirestoreDebugger {

    private const val TAG = "FIRESTORE_DEBUG"

    /**
     * Imprime TODAS las colecciones con TODOS sus datos
     * Solo debe ejecutarse cuando maestro@bioway.com.mx inicia sesión
     */
    suspend fun imprimirTodasLasColecciones() {
        val db = FirebaseFirestore.getInstance()

        Log.d(TAG, "================================================================================")
        Log.d(TAG, "🔍 ANÁLISIS COMPLETO DE FIRESTORE - software-4e6b6")
        Log.d(TAG, "================================================================================")

        // Lista de colecciones conocidas
        val colecciones = listOf(
            "UsersInAct",
            "Recolectores",
            "CentrosDeAcopio",
            "Reciclables",
            "Horarios",
            "Config",
            "companies",
            "sessions",
            // Trazabilidad (para info, no modificar)
            "trazabilidad_config",
            "trazabilidad_admin",
            "trazabilidad_users",
            "trazabilidad_stats",
            "feature_requests"
        )

        for (coleccion in colecciones) {
            try {
                Log.d(TAG, "\n")
                Log.d(TAG, "================================================================================")
                Log.d(TAG, "📂 COLECCIÓN: $coleccion")
                Log.d(TAG, "================================================================================")

                val snapshot = db.collection(coleccion).get().await()

                Log.d(TAG, "Total de documentos: ${snapshot.documents.size}")
                Log.d(TAG, "")

                if (snapshot.documents.isEmpty()) {
                    Log.d(TAG, "⚠️  Colección vacía")
                } else {
                    snapshot.documents.forEachIndexed { index, document ->
                        Log.d(TAG, "--- Documento ${index + 1}/${snapshot.documents.size} ---")
                        Log.d(TAG, "ID: ${document.id}")

                        val data = document.data
                        if (data != null) {
                            data.forEach { (key, value) ->
                                val valorStr = when (value) {
                                    is String -> "\"$value\""
                                    is Number -> value.toString()
                                    is Boolean -> value.toString()
                                    is com.google.firebase.Timestamp -> value.toDate().toString()
                                    is List<*> -> "[Lista con ${value.size} elementos]"
                                    is Map<*, *> -> "{Map con ${value.size} campos}"
                                    null -> "null"
                                    else -> value.toString()
                                }
                                Log.d(TAG, "  $key: $valorStr")
                            }

                            // Si tiene subcollections, intentar listarlas
                            if (coleccion == "UsersInAct") {
                                imprimirSubcollections(db, coleccion, document.id)
                            }
                        } else {
                            Log.d(TAG, "  (Sin datos)")
                        }
                        Log.d(TAG, "")
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error al leer colección $coleccion: ${e.message}")
            }
        }

        Log.d(TAG, "================================================================================")
        Log.d(TAG, "✅ ANÁLISIS COMPLETO FINALIZADO")
        Log.d(TAG, "================================================================================")
    }

    /**
     * Imprime subcollections de un documento
     */
    private suspend fun imprimirSubcollections(
        db: FirebaseFirestore,
        coleccionPadre: String,
        documentoId: String
    ) {
        val subcollections = listOf("Historial", "Residuos")

        for (subcol in subcollections) {
            try {
                val snapshot = db.collection(coleccionPadre)
                    .document(documentoId)
                    .collection(subcol)
                    .get()
                    .await()

                if (snapshot.documents.isNotEmpty()) {
                    Log.d(TAG, "  📁 Subcollection: $subcol (${snapshot.documents.size} docs)")

                    snapshot.documents.take(3).forEach { doc ->  // Solo primeros 3 para no saturar
                        Log.d(TAG, "    - ${doc.id}: ${doc.data?.keys?.joinToString(", ")}")
                    }

                    if (snapshot.documents.size > 3) {
                        Log.d(TAG, "    ... y ${snapshot.documents.size - 3} más")
                    }
                }
            } catch (e: Exception) {
                // Ignorar si no existe la subcollection
            }
        }
    }

    /**
     * Imprime resumen ejecutivo (más compacto)
     */
    suspend fun imprimirResumenColecciones() {
        val db = FirebaseFirestore.getInstance()

        Log.d(TAG, "")
        Log.d(TAG, "📊 RESUMEN DE COLECCIONES:")
        Log.d(TAG, "=".repeat(60))

        val colecciones = listOf(
            "UsersInAct", "Recolectores", "CentrosDeAcopio",
            "Reciclables", "Horarios", "Config"
        )

        for (coleccion in colecciones) {
            try {
                val count = db.collection(coleccion).get().await().documents.size
                val icono = when {
                    count == 0 -> "⚪"
                    count < 5 -> "🟡"
                    count < 20 -> "🟢"
                    else -> "🔵"
                }
                Log.d(TAG, "$icono $coleccion: $count documentos")
            } catch (e: Exception) {
                Log.e(TAG, "❌ $coleccion: Error al contar")
            }
        }

        Log.d(TAG, "=".repeat(60))
    }
}
