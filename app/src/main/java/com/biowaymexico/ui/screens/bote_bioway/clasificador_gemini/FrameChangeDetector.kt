package com.biowaymexico.ui.screens.bote_bioway.clasificador_gemini

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import kotlin.math.abs

private const val TAG = "FrameChangeDetector"

/**
 * Detector de cambios en el frame para detectar objetos que YOLO no reconoce.
 *
 * Funciona comparando el frame actual con un frame de referencia (plato vacío).
 * Si hay un cambio significativo en el contenido visual, indica que hay algo nuevo.
 *
 * Esto permite detectar CUALQUIER objeto colocado sobre el plato, incluso si
 * YOLO no lo reconoce porque no está en su conjunto de entrenamiento.
 */
object FrameChangeDetector {

    // Frame de referencia (plato vacío)
    private var baselineBitmap: Bitmap? = null
    private var hasBaseline = false

    // Configuración de sensibilidad
    private const val CHANGE_THRESHOLD = 0.08f  // 8% de cambio mínimo para detectar objeto
    private const val SAMPLE_GRID_SIZE = 20     // Grid de 20x20 para comparación (400 puntos)
    private const val COLOR_DIFF_THRESHOLD = 40 // Diferencia mínima de color por canal

    /**
     * Captura el frame actual como baseline (plato vacío).
     * Debe llamarse cuando el plato está vacío y listo para recibir objetos.
     */
    fun captureBaseline(bitmap: Bitmap) {
        try {
            // Crear una copia escalada para comparación eficiente
            baselineBitmap?.recycle()
            baselineBitmap = Bitmap.createScaledBitmap(bitmap, SAMPLE_GRID_SIZE, SAMPLE_GRID_SIZE, true)
            hasBaseline = true

            Log.d(TAG, "═══════════════════════════════════════")
            Log.d(TAG, "📷 Baseline capturado (plato vacío)")
            Log.d(TAG, "   Tamaño original: ${bitmap.width}x${bitmap.height}")
            Log.d(TAG, "   Tamaño sample: ${SAMPLE_GRID_SIZE}x${SAMPLE_GRID_SIZE}")
            Log.d(TAG, "═══════════════════════════════════════")
        } catch (e: Exception) {
            Log.e(TAG, "Error capturando baseline: ${e.message}")
            hasBaseline = false
        }
    }

    /**
     * Compara el frame actual con el baseline para detectar cambios.
     *
     * @param currentBitmap Frame actual de la cámara
     * @return true si hay un cambio significativo (objeto detectado), false si no
     */
    fun detectChange(currentBitmap: Bitmap): FrameChangeResult {
        if (!hasBaseline || baselineBitmap == null) {
            return FrameChangeResult(
                hasChange = false,
                changePercentage = 0f,
                reason = "Sin baseline - captura primero el plato vacío"
            )
        }

        try {
            // Escalar frame actual al mismo tamaño que baseline
            val scaledCurrent = Bitmap.createScaledBitmap(
                currentBitmap,
                SAMPLE_GRID_SIZE,
                SAMPLE_GRID_SIZE,
                true
            )

            // Comparar píxeles
            var changedPixels = 0
            val totalPixels = SAMPLE_GRID_SIZE * SAMPLE_GRID_SIZE

            for (x in 0 until SAMPLE_GRID_SIZE) {
                for (y in 0 until SAMPLE_GRID_SIZE) {
                    val basePixel = baselineBitmap!!.getPixel(x, y)
                    val currPixel = scaledCurrent.getPixel(x, y)

                    // Comparar cada canal de color
                    val rDiff = abs(Color.red(basePixel) - Color.red(currPixel))
                    val gDiff = abs(Color.green(basePixel) - Color.green(currPixel))
                    val bDiff = abs(Color.blue(basePixel) - Color.blue(currPixel))

                    // Si cualquier canal tiene diferencia significativa
                    if (rDiff > COLOR_DIFF_THRESHOLD ||
                        gDiff > COLOR_DIFF_THRESHOLD ||
                        bDiff > COLOR_DIFF_THRESHOLD) {
                        changedPixels++
                    }
                }
            }

            scaledCurrent.recycle()

            val changePercentage = changedPixels.toFloat() / totalPixels
            val hasChange = changePercentage >= CHANGE_THRESHOLD

            val result = FrameChangeResult(
                hasChange = hasChange,
                changePercentage = changePercentage,
                reason = if (hasChange) {
                    "Cambio detectado: ${(changePercentage * 100).toInt()}% del frame"
                } else {
                    "Sin cambio significativo: ${(changePercentage * 100).toInt()}%"
                }
            )

            // Log solo cuando hay cambio significativo para no spam
            if (hasChange) {
                Log.d(TAG, "🔄 ${result.reason}")
            }

            return result

        } catch (e: Exception) {
            Log.e(TAG, "Error detectando cambio: ${e.message}")
            return FrameChangeResult(
                hasChange = false,
                changePercentage = 0f,
                reason = "Error: ${e.message}"
            )
        }
    }

    /**
     * Verifica si hay un baseline válido capturado
     */
    fun hasValidBaseline(): Boolean = hasBaseline && baselineBitmap != null

    /**
     * Resetea el detector, liberando el baseline
     */
    fun reset() {
        baselineBitmap?.recycle()
        baselineBitmap = null
        hasBaseline = false
        Log.d(TAG, "🔄 Detector reseteado")
    }

    /**
     * Libera recursos
     */
    fun release() {
        reset()
    }
}

/**
 * Resultado de la detección de cambio de frame
 */
data class FrameChangeResult(
    val hasChange: Boolean,
    val changePercentage: Float,
    val reason: String
)
