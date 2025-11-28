package com.biowaymexico.ui.screens.bote_bioway.clasificador_gemini

import android.graphics.Bitmap
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

private const val TAG = "GeminiClassifier"

/**
 * Resultado de clasificación de Gemini
 */
data class GeminiClassificationResult(
    val category: MaterialCategoryGemini,
    val confidence: String,  // "alta", "media", "baja"
    val rawResponse: String,
    val reasoning: String
)

/**
 * Categorías de materiales para el bote BioWay (versión Gemini)
 * Idénticas a las de YOLO pero separadas para claridad
 */
enum class MaterialCategoryGemini(
    val displayName: String,
    val emoji: String,
    val giro: Int,
    val inclinacion: Int,
    val color: Long
) {
    PLASTICO(
        displayName = "Plástico",
        emoji = "♻️",
        giro = -30,
        inclinacion = -45,
        color = 0xFF2196F3  // Azul
    ),
    PAPEL_CARTON(
        displayName = "Papel/Cartón",
        emoji = "📄",
        giro = -30,
        inclinacion = 45,
        color = 0xFF4CAF50  // Verde
    ),
    ALUMINIO_METAL(
        displayName = "Aluminio/Metal",
        emoji = "🥫",
        giro = 59,
        inclinacion = -45,
        color = 0xFF9C27B0  // Morado
    ),
    GENERAL(
        displayName = "General",
        emoji = "🗑️",
        giro = 59,
        inclinacion = 45,
        color = 0xFFFF9800  // Naranja
    ),
    NO_DETECTADO(
        displayName = "No detectado",
        emoji = "❓",
        giro = 0,
        inclinacion = 0,
        color = 0xFF9E9E9E  // Gris
    );

    companion object {
        fun fromGeminiResponse(response: String): MaterialCategoryGemini {
            val lowerResponse = response.lowercase()
            return when {
                // PRIMERO verificar NO_DETECTADO (antes que GENERAL para evitar conflicto)
                lowerResponse.contains("no_detectado") ||
                lowerResponse.contains("no detectado") ||
                lowerResponse.contains("vacio") ||
                lowerResponse.contains("vacío") ||
                lowerResponse.contains("nada") ||
                lowerResponse.contains("borrosa") ||
                lowerResponse.contains("no puedo") ||
                lowerResponse.contains("no visible") ||
                lowerResponse.contains("no hay objeto") ||
                lowerResponse.contains("no se puede") -> NO_DETECTADO

                // PLÁSTICO - amplio rango incluyendo TRANSPARENTES
                lowerResponse.contains("plastico") || lowerResponse.contains("plástico") ||
                lowerResponse.contains("plastic") ||
                lowerResponse.contains("pet") || lowerResponse.contains("botella") ||
                lowerResponse.contains("envase") || lowerResponse.contains("bolsa") ||
                lowerResponse.contains("contenedor") || lowerResponse.contains("recipiente") ||
                lowerResponse.contains("tapón") || lowerResponse.contains("tapa") ||
                lowerResponse.contains("vaso") || lowerResponse.contains("popote") ||
                lowerResponse.contains("pajilla") || lowerResponse.contains("straw") ||
                lowerResponse.contains("cubierto") || lowerResponse.contains("tenedor") ||
                lowerResponse.contains("cuchara") || lowerResponse.contains("plato") ||
                lowerResponse.contains("charola") || lowerResponse.contains("empaque") ||
                lowerResponse.contains("envoltura") || lowerResponse.contains("film") ||
                lowerResponse.contains("polietileno") || lowerResponse.contains("pvc") ||
                lowerResponse.contains("hdpe") || lowerResponse.contains("ldpe") ||
                lowerResponse.contains("pp") || lowerResponse.contains("ps") ||
                lowerResponse.contains("unicel") || lowerResponse.contains("styrofoam") ||
                lowerResponse.contains("espuma") ||
                // Plásticos transparentes específicos
                lowerResponse.contains("transparente") ||
                lowerResponse.contains("transparent") ||
                lowerResponse.contains("clear") ||
                lowerResponse.contains("lid") ||  // tapa en inglés
                lowerResponse.contains("cup") ||  // vaso en inglés
                lowerResponse.contains("coffee lid") ||
                lowerResponse.contains("tapa de cafe") ||
                lowerResponse.contains("tapa de café") ||
                lowerResponse.contains("frappe") ||
                lowerResponse.contains("smoothie") ||
                lowerResponse.contains("clamshell") ||
                lowerResponse.contains("blister") ||
                lowerResponse.contains("burbuja") ||
                lowerResponse.contains("celofan") ||
                lowerResponse.contains("celofán") ||
                lowerResponse.contains("acetato") ||
                lowerResponse.contains("acrílico") ||
                lowerResponse.contains("acrilico") -> PLASTICO

                // PAPEL/CARTÓN - papel y derivados
                lowerResponse.contains("papel") || lowerResponse.contains("carton") ||
                lowerResponse.contains("cartón") || lowerResponse.contains("periodico") ||
                lowerResponse.contains("periódico") || lowerResponse.contains("revista") ||
                lowerResponse.contains("libro") || lowerResponse.contains("hoja") ||
                lowerResponse.contains("folder") || lowerResponse.contains("carpeta") ||
                lowerResponse.contains("sobre") || lowerResponse.contains("caja") ||
                lowerResponse.contains("empaque de carton") || lowerResponse.contains("tetrapack") ||
                lowerResponse.contains("tetrapak") || lowerResponse.contains("tetra pak") ||
                lowerResponse.contains("servilleta") || lowerResponse.contains("ticket") ||
                lowerResponse.contains("recibo") || lowerResponse.contains("factura") ||
                lowerResponse.contains("documento") ||
                lowerResponse.contains("paper") ||
                lowerResponse.contains("cardboard") -> PAPEL_CARTON

                // ALUMINIO/METAL/VIDRIO - metales y vidrio (BRILLANTES)
                lowerResponse.contains("metal") || lowerResponse.contains("aluminio") ||
                lowerResponse.contains("aluminum") ||
                lowerResponse.contains("lata") || lowerResponse.contains("can") ||
                lowerResponse.contains("vidrio") || lowerResponse.contains("glass") ||
                lowerResponse.contains("cristal") || lowerResponse.contains("botella de vidrio") ||
                lowerResponse.contains("frasco") || lowerResponse.contains("tarro") ||
                lowerResponse.contains("jar") ||
                lowerResponse.contains("acero") || lowerResponse.contains("steel") ||
                lowerResponse.contains("hierro") || lowerResponse.contains("iron") ||
                lowerResponse.contains("cobre") || lowerResponse.contains("copper") ||
                lowerResponse.contains("bronce") || lowerResponse.contains("bronze") ||
                lowerResponse.contains("hojalata") || lowerResponse.contains("tin") ||
                lowerResponse.contains("aerosol") || lowerResponse.contains("spray") ||
                lowerResponse.contains("lámina") || lowerResponse.contains("alambre") ||
                lowerResponse.contains("wire") ||
                lowerResponse.contains("clavo") || lowerResponse.contains("nail") ||
                lowerResponse.contains("tornillo") || lowerResponse.contains("screw") ||
                lowerResponse.contains("tuerca") || lowerResponse.contains("nut") ||
                lowerResponse.contains("moneda") || lowerResponse.contains("coin") ||
                lowerResponse.contains("llave") || lowerResponse.contains("key") ||
                lowerResponse.contains("candado") || lowerResponse.contains("lock") ||
                // Características de metal
                lowerResponse.contains("brillante") ||
                lowerResponse.contains("metalico") ||
                lowerResponse.contains("metálico") ||
                lowerResponse.contains("shiny") ||
                lowerResponse.contains("reflective") ||
                lowerResponse.contains("refleja") ||
                lowerResponse.contains("plateado") ||
                lowerResponse.contains("silver") ||
                lowerResponse.contains("dorado") ||
                lowerResponse.contains("gold") ||
                lowerResponse.contains("cromado") ||
                lowerResponse.contains("chrome") -> ALUMINIO_METAL

                // GENERAL - orgánicos y no reciclables
                lowerResponse.contains("organico") || lowerResponse.contains("orgánico") ||
                lowerResponse.contains("organic") ||
                lowerResponse.contains("basura") || lowerResponse.contains("general") ||
                lowerResponse.contains("trash") || lowerResponse.contains("garbage") ||
                lowerResponse.contains("comida") || lowerResponse.contains("food") ||
                lowerResponse.contains("alimento") ||
                lowerResponse.contains("fruta") || lowerResponse.contains("fruit") ||
                lowerResponse.contains("verdura") || lowerResponse.contains("vegetable") ||
                lowerResponse.contains("cascara") || lowerResponse.contains("cáscara") ||
                lowerResponse.contains("hueso") || lowerResponse.contains("bone") ||
                lowerResponse.contains("residuo") ||
                lowerResponse.contains("desecho") || lowerResponse.contains("waste") ||
                lowerResponse.contains("pañal") || lowerResponse.contains("diaper") ||
                lowerResponse.contains("toalla") ||
                lowerResponse.contains("sanitario") ||
                lowerResponse.contains("higienico") || lowerResponse.contains("higiénico") ||
                lowerResponse.contains("colilla") || lowerResponse.contains("cigarette") ||
                lowerResponse.contains("cigarro") ||
                lowerResponse.contains("chicle") || lowerResponse.contains("gum") ||
                lowerResponse.contains("ceramica") || lowerResponse.contains("cerámica") ||
                lowerResponse.contains("ceramic") ||
                lowerResponse.contains("porcelana") || lowerResponse.contains("porcelain") ||
                lowerResponse.contains("tela") || lowerResponse.contains("fabric") ||
                lowerResponse.contains("ropa") || lowerResponse.contains("cloth") ||
                lowerResponse.contains("textil") || lowerResponse.contains("textile") ||
                lowerResponse.contains("cuero") || lowerResponse.contains("leather") ||
                lowerResponse.contains("madera") || lowerResponse.contains("wood") ||
                lowerResponse.contains("corcho") || lowerResponse.contains("cork") -> GENERAL

                else -> NO_DETECTADO  // Por defecto NO_DETECTADO si no reconoce
            }
        }
    }
}

/**
 * Cliente para clasificación de residuos usando Google Gemini AI
 *
 * Usa gemini-2.0-flash-lite - EL MODELO MÁS ECONÓMICO DISPONIBLE
 * Precios (Nov 2025): $0.075/1M input tokens, $0.30/1M output tokens
 *
 * Referencia: https://ai.google.dev/gemini-api/docs/pricing
 */
class GeminiClassifier(
    private val apiKey: String
) {
    private var generativeModel: GenerativeModel? = null
    private var isInitialized = false
    private var modelUsed: String = ""

    companion object {
        // gemini-2.0-flash-lite es el más barato: $0.075/1M input, $0.30/1M output
        private const val MODEL_CHEAPEST = "gemini-2.0-flash-lite"
    }

    /**
     * Inicializa el modelo de Gemini con el modelo más económico
     */
    fun initialize(): Boolean {
        return try {
            Log.d(TAG, "═══════════════════════════════════════")
            Log.d(TAG, "🤖 Inicializando Gemini AI...")
            Log.d(TAG, "   Modelo: $MODEL_CHEAPEST (más económico)")
            Log.d(TAG, "   Precio: \$0.075/1M input, \$0.30/1M output")

            modelUsed = MODEL_CHEAPEST

            generativeModel = GenerativeModel(
                modelName = MODEL_CHEAPEST,
                apiKey = apiKey,
                generationConfig = generationConfig {
                    temperature = 0.1f  // Muy baja temperatura para respuestas consistentes
                    topK = 1
                    topP = 0.95f
                    maxOutputTokens = 150  // JSON corto, no necesita mucho
                }
            )

            isInitialized = true
            Log.d(TAG, "✅ Gemini AI inicializado correctamente")
            Log.d(TAG, "═══════════════════════════════════════")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error inicializando Gemini: ${e.message}", e)
            isInitialized = false
            false
        }
    }

    /**
     * Clasifica una imagen de residuo usando Gemini
     * @param bitmap Imagen capturada del residuo
     * @return Resultado de clasificación o null si hay error
     */
    suspend fun classifyWaste(bitmap: Bitmap): GeminiClassificationResult? = withContext(Dispatchers.IO) {
        if (!isInitialized || generativeModel == null) {
            Log.e(TAG, "❌ Gemini no está inicializado")
            return@withContext null
        }

        try {
            Log.d(TAG, "═══════════════════════════════════════")
            Log.d(TAG, "📸 Enviando imagen a Gemini...")
            Log.d(TAG, "   Tamaño: ${bitmap.width}x${bitmap.height}")

            val startTime = System.currentTimeMillis()

            // Prompt optimizado para clasificación de residuos
            // Gemini debe encontrar y clasificar CUALQUIER objeto en la imagen
            // Énfasis en plásticos transparentes y metales
            val prompt = """
                TAREA: Clasificar el residuo/objeto en la imagen para reciclaje.

                IMPORTANTE:
                - IGNORA completamente la base/plato/superficie blanca circular del fondo
                - Enfócate SOLO en el objeto que está ENCIMA de la base blanca
                - Si hay CUALQUIER objeto visible (sin importar qué sea), clasifícalo
                - Los plásticos TRANSPARENTES también son PLASTICO (no los ignores)
                - Los objetos BRILLANTES o METÁLICOS son ALUMINIO_METAL

                CATEGORÍAS (elige la más apropiada):

                PLASTICO - Cualquier material plástico (INCLUYE TRANSPARENTES):
                • ⭐ TAPAS DE CAFÉ con popote/agujero (aunque sean transparentes)
                • ⭐ Vasos transparentes de café, frappe, smoothies
                • ⭐ Popotes/pajillas (transparentes o de color)
                • ⭐ Contenedores transparentes de comida (clamshell)
                • ⭐ Plástico transparente sin etiquetas ni logos
                • Botellas de agua, refresco (PET transparente o de color)
                • Envases, contenedores, recipientes plásticos
                • Bolsas de plástico, empaques, envolturas, celofán
                • Tapas, tapones de botellas (cualquier color)
                • Vasos, platos, cubiertos desechables
                • Unicel, espuma de poliestireno (styrofoam)
                • Blister de medicamentos, empaques de burbujas
                • Cualquier objeto de plástico duro o flexible

                PAPEL_CARTON - Papel y derivados:
                • Papel de cualquier tipo (blanco, kraft, reciclado)
                • Cartón, cajas de cartón
                • Periódicos, revistas, libros, cuadernos
                • Folders, carpetas, sobres
                • Servilletas de papel, tickets, recibos
                • Tetrapack/Tetrapak (envases de leche, jugos)
                • Empaques de cartón, tubos de papel

                ALUMINIO_METAL - Metales y vidrio (OBJETOS BRILLANTES):
                • ⭐ LATAS de aluminio (brillantes, cilíndricas)
                • ⭐ Latas de conservas, atún, frijoles (hojalata)
                • ⭐ Papel aluminio (arrugado o liso)
                • ⭐ Objetos BRILLANTES o METÁLICOS
                • ⭐ Monedas de cualquier denominación
                • Tapas metálicas de frascos (corona, rosca)
                • Láminas, alambre, clavos, tornillos
                • Llaves, candados, herramientas pequeñas
                • Envases de aerosol vacíos
                • Botellas de vidrio, frascos, tarros (transparentes o de color)
                • Cualquier objeto que refleje luz como metal

                GENERAL - Orgánicos y no reciclables:
                • Restos de comida, cáscaras de frutas/verduras
                • Residuos orgánicos biodegradables
                • Pañales, toallas sanitarias
                • Colillas de cigarro, chicles
                • Cerámica, porcelana rota
                • Tela, ropa, textiles
                • Madera, corcho
                • Objetos compuestos no separables

                NO_DETECTADO - SOLO si:
                • La imagen está vacía (solo se ve el plato blanco)
                • No hay NINGÚN objeto visible sobre la base
                • La imagen está completamente borrosa

                REGLAS CRÍTICAS:
                1. Si ves algo TRANSPARENTE sobre el plato → probablemente es PLASTICO
                2. Si ves algo BRILLANTE/METÁLICO → es ALUMINIO_METAL
                3. Si ves una FORMA CIRCULAR con agujero → puede ser tapa de café (PLASTICO)
                4. NUNCA uses NO_DETECTADO si hay cualquier objeto visible

                Responde ÚNICAMENTE con este JSON:
                {"categoria":"CATEGORIA","confianza":"alta/media/baja","razon":"descripción del objeto"}
            """.trimIndent()

            val response = generativeModel!!.generateContent(
                content {
                    image(bitmap)
                    text(prompt)
                }
            )

            val responseText = response.text ?: ""
            val elapsedTime = System.currentTimeMillis() - startTime

            Log.d(TAG, "📥 Respuesta de Gemini (${elapsedTime}ms):")
            Log.d(TAG, "   $responseText")

            // Parsear respuesta JSON
            val result = parseGeminiResponse(responseText)

            Log.d(TAG, "═══════════════════════════════════════")
            Log.d(TAG, "🎯 CLASIFICACIÓN GEMINI:")
            Log.d(TAG, "   Categoría: ${result.category.displayName}")
            Log.d(TAG, "   Confianza: ${result.confidence}")
            Log.d(TAG, "   Razón: ${result.reasoning}")
            Log.d(TAG, "═══════════════════════════════════════")

            result
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error clasificando con Gemini: ${e.message}", e)
            null
        }
    }

    private fun parseGeminiResponse(response: String): GeminiClassificationResult {
        return try {
            // Intentar extraer JSON de la respuesta
            val jsonMatch = Regex("""\{[^}]+\}""").find(response)
            val jsonStr = jsonMatch?.value ?: response

            // Extraer campos del JSON manualmente (sin dependencia de gson)
            val categoriaMatch = Regex(""""categoria"\s*:\s*"([^"]+)"""").find(jsonStr)
            val confianzaMatch = Regex(""""confianza"\s*:\s*"([^"]+)"""").find(jsonStr)
            val razonMatch = Regex(""""razon"\s*:\s*"([^"]+)"""").find(jsonStr)

            val categoria = categoriaMatch?.groupValues?.get(1) ?: response
            val confianza = confianzaMatch?.groupValues?.get(1) ?: "media"
            val razon = razonMatch?.groupValues?.get(1) ?: "Clasificación automática"

            GeminiClassificationResult(
                category = MaterialCategoryGemini.fromGeminiResponse(categoria),
                confidence = confianza,
                rawResponse = response,
                reasoning = razon
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parseando respuesta: ${e.message}")
            GeminiClassificationResult(
                category = MaterialCategoryGemini.fromGeminiResponse(response),
                confidence = "baja",
                rawResponse = response,
                reasoning = "Error en parseo, clasificación por texto"
            )
        }
    }

    /**
     * Verifica si Gemini está listo para usar
     */
    fun isReady(): Boolean = isInitialized && generativeModel != null

    /**
     * Libera recursos
     */
    fun close() {
        generativeModel = null
        isInitialized = false
        Log.d(TAG, "🔄 Gemini Classifier cerrado")
    }
}
