package com.biowaymexico.utils

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * Clasificador de residuos utilizando TensorFlow Lite
 * Modelo entrenado para detectar 7 categorías de residuos
 */
class ClasificadorResiduos(context: Context) {
    private var interpreter: Interpreter? = null

    // Etiquetas de clasificación (orden alfabético común en modelos de reciclaje)
    // Orden actualizado basado en nomenclatura estándar
    private val etiquetas = listOf(
        "Basura",      // 0 - Trash/Garbage
        "Cartón",      // 1 - Cardboard
        "Vidrio",      // 2 - Glass
        "Metal",       // 3 - Metal
        "Orgánico",    // 4 - Organic/Compost
        "Papel",       // 5 - Paper
        "Plástico"     // 6 - Plastic
    )

    // Tamaño de entrada esperado por el modelo
    private val INPUT_SIZE = 224

    companion object {
        private const val TAG = "ClasificadorResiduos"
    }

    init {
        try {
            interpreter = Interpreter(cargarModelo(context))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Clasifica una imagen de residuo
     * @param bitmap Imagen a clasificar
     * @return Par de (categoría, confianza) donde confianza está entre 0 y 1
     */
    fun clasificar(bitmap: Bitmap): Pair<String, Float> {
        return try {
            val inputBuffer = preprocesar(bitmap)
            val output = Array(1) { FloatArray(etiquetas.size) }

            interpreter?.run(inputBuffer, output)

            // Debug: Imprimir todas las probabilidades
            Log.d(TAG, "=== Resultados de clasificación ===")
            etiquetas.indices.forEach { i ->
                Log.d(TAG, "[$i] ${etiquetas[i]}: ${output[0][i]} (${(output[0][i] * 100).toInt()}%)")
            }

            // Encontrar la clase con mayor probabilidad
            val maxIndex = output[0].indices.maxByOrNull { output[0][it] } ?: 0
            val confianza = output[0][maxIndex]

            Log.d(TAG, "Resultado final: ${etiquetas[maxIndex]} con confianza ${(confianza * 100).toInt()}%")

            Pair(etiquetas[maxIndex], confianza)
        } catch (e: Exception) {
            Log.e(TAG, "Error en clasificación", e)
            e.printStackTrace()
            Pair("Error", 0f)
        }
    }

    /**
     * Obtiene todas las probabilidades de clasificación
     * @param bitmap Imagen a clasificar
     * @return Lista de triples (índice, categoría, confianza) ordenada por confianza descendente
     */
    fun clasificarTodas(bitmap: Bitmap): List<Triple<Int, String, Float>> {
        return try {
            val inputBuffer = preprocesar(bitmap)
            val output = Array(1) { FloatArray(etiquetas.size) }

            interpreter?.run(inputBuffer, output)

            // Crear lista de todas las categorías con sus índices y probabilidades
            etiquetas.indices.map { i ->
                Triple(i, etiquetas[i], output[0][i])
            }.sortedByDescending { it.third }
        } catch (e: Exception) {
            Log.e(TAG, "Error en clasificarTodas", e)
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Preprocesa la imagen para el modelo
     * - Redimensiona a 224x224
     * - Normaliza píxeles según estándar ImageNet
     * - Convierte a ByteBuffer en formato RGB
     */
    private fun preprocesar(bitmap: Bitmap): ByteBuffer {
        // Redimensionar imagen a tamaño esperado con filtrado bilineal
        val resized = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)

        // Crear buffer para los datos de entrada (float32)
        val buffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * 3)
        buffer.order(ByteOrder.nativeOrder())

        // Extraer píxeles
        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        resized.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)

        // Normalización estándar: (pixel / 255.0)
        // Algunos modelos usan normalización ImageNet: (pixel/255.0 - mean) / std
        // Mean = [0.485, 0.456, 0.406], Std = [0.229, 0.224, 0.225]

        for (pixel in pixels) {
            // Extraer componentes RGB (no BGR)
            val r = (pixel shr 16 and 0xFF)
            val g = (pixel shr 8 and 0xFF)
            val b = (pixel and 0xFF)

            // Normalización simple [0, 1]
            buffer.putFloat(r / 255.0f)
            buffer.putFloat(g / 255.0f)
            buffer.putFloat(b / 255.0f)
        }

        Log.d(TAG, "Imagen preprocesada: ${INPUT_SIZE}x${INPUT_SIZE}")
        return buffer
    }

    /**
     * Carga el modelo TFLite desde assets
     */
    private fun cargarModelo(context: Context): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd("modelo_residuos.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    /**
     * Libera recursos del intérprete
     * Llamar cuando ya no se necesite el clasificador
     */
    fun cerrar() {
        interpreter?.close()
        interpreter = null
    }

    /**
     * Verifica si el clasificador está listo para usar
     */
    fun estaListo(): Boolean = interpreter != null
}
