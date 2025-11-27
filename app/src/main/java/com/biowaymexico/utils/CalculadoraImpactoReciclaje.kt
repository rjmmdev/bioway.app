package com.biowaymexico.utils

/**
 * Calculadora de Impacto Ambiental del Reciclaje
 *
 * FUENTES VERIFICADAS:
 * 1. GUIA_CALCULADORA_IMPACTO_RECICLAJE.md (Guía completa)
 * 2. FACTORES_IMPACTO_RECICLAJE.xlsx (Hoja: FACTORES_RESUMEN) ✅ VERIFICADO
 * 3. Guia_Conversiones_Reciclaje_ECOCE.docx (Detalles y referencias)
 *
 * Basado en: CALCULADORA ECOCE 2021
 * Referencias científicas: APR (2018), EPA, Franklin Associates (1995), ASIPLA
 *
 * TODOS LOS FACTORES HAN SIDO VERIFICADOS CONTRA LOS DOCUMENTOS OFICIALES
 */
object CalculadoraImpactoReciclaje {

    /**
     * Factores de CO₂ evitado por kilogramo de material reciclado
     * Unidad: kg CO₂e / kg de material
     * Fuente: FACTORES_IMPACTO_RECICLAJE.xlsx - Columna "CO₂ (kg CO₂e)"
     */
    private val factoresCO2 = mapOf(
        "PET" to 1.87,
        "PEAD" to 1.33,
        "PEBD" to 1.29,
        "BOPP" to 1.31,
        "Polipropileno" to 1.31,
        "Aluminio" to 7.93,
        "Hojalata" to 1.5,
        "Vidrio" to 0.67,
        "Cartón Multilaminado" to 0.796,
        "Cartón" to 0.796,
        "Papel" to 0.796
    )

    /**
     * Factores de energía ahorrada por kilogramo de material reciclado
     * Unidad: kWh / kg de material
     * Fuente: FACTORES_IMPACTO_RECICLAJE.xlsx - Hoja FACTORES_RESUMEN
     */
    private val factoresEnergia = mapOf(
        "PET" to 15.277,
        "PEAD" to 18.507,
        "PEBD" to 18.0,  // ✅ Corregido según Excel
        "BOPP" to 18.197,
        "Polipropileno" to 18.0,  // ✅ Corregido según Excel
        "Aluminio" to 35.0,
        "Hojalata" to 10.0,
        "Vidrio" to 1.6,
        "Cartón Multilaminado" to 4.0,
        "Cartón" to 4.0,
        "Papel" to 4.0
    )

    /**
     * Factores de agua ahorrada por kilogramo de material reciclado
     * Unidad: litros / kg de material
     */
    private val factoresAgua = mapOf(
        "PEAD" to 4.9,
        "BOPP" to 3.93,
        "Aluminio" to 90.0,
        "Cartón Multilaminado" to 26.5,
        "Cartón" to 26.5,
        "Papel" to 26.5
        // PET no ahorra agua (consume más en el reciclaje)
    )

    /**
     * Factores de materia prima ahorrada por kilogramo de material reciclado
     * Unidad: kg de materia prima / kg de material
     */
    private val factoresMateriaPrima = mapOf(
        "PET" to 0.755,
        "PEAD" to 0.838,
        "PEBD" to 0.832,
        "BOPP" to 0.706,
        "Aluminio" to 4.643,
        "Hojalata" to 2.45,
        "Vidrio" to 1.2,
        "Cartón Multilaminado" to 1.256,
        "Cartón" to 1.256,
        "Papel" to 1.256
    )

    /**
     * Resultado del cálculo de impacto
     */
    data class ImpactoAmbiental(
        val co2Evitado: Double,  // kg CO₂e
        val energiaAhorrada: Double,  // kWh
        val aguaAhorrada: Double,  // litros
        val materiaPrimaAhorrada: Double,  // kg
        val arbolesEquivalentes: Double,  // número de árboles
        val kmEnAuto: Double,  // km recorridos en auto
        val litrosGasolina: Double  // litros de gasolina
    )

    /**
     * Calcula el impacto ambiental del reciclaje de un material
     *
     * @param tipoMaterial Tipo de material (debe coincidir con las claves del map)
     * @param pesoKg Peso del material en kilogramos
     * @return ImpactoAmbiental con todos los indicadores calculados
     */
    fun calcularImpacto(tipoMaterial: String, pesoKg: Double): ImpactoAmbiental {
        // CO₂ evitado
        val co2 = (factoresCO2[tipoMaterial] ?: 0.0) * pesoKg

        // Energía ahorrada
        val energia = (factoresEnergia[tipoMaterial] ?: 0.0) * pesoKg

        // Agua ahorrada
        val agua = (factoresAgua[tipoMaterial] ?: 0.0) * pesoKg

        // Materia prima ahorrada
        val materiaPrima = (factoresMateriaPrima[tipoMaterial] ?: 0.0) * pesoKg

        // Equivalencias
        val arboles = co2 / 150.0  // 1 árbol absorbe 150 kg CO₂/año
        val kmAuto = (co2 / 2.4) * 20.09  // Factor gasolina × rendimiento
        val litrosGas = co2 / 2.4  // 1 L gasolina = 2.4 kg CO₂

        return ImpactoAmbiental(
            co2Evitado = co2,
            energiaAhorrada = energia,
            aguaAhorrada = agua,
            materiaPrimaAhorrada = materiaPrima,
            arbolesEquivalentes = arboles,
            kmEnAuto = kmAuto,
            litrosGasolina = litrosGas
        )
    }

    /**
     * Calcula el impacto total de múltiples materiales
     *
     * @param materiales Map de tipo de material → peso en kg
     * @return ImpactoAmbiental total sumado
     */
    fun calcularImpactoTotal(materiales: Map<String, Double>): ImpactoAmbiental {
        var totalCO2 = 0.0
        var totalEnergia = 0.0
        var totalAgua = 0.0
        var totalMateriaPrima = 0.0

        materiales.forEach { (tipo, peso) ->
            val impacto = calcularImpacto(tipo, peso)
            totalCO2 += impacto.co2Evitado
            totalEnergia += impacto.energiaAhorrada
            totalAgua += impacto.aguaAhorrada
            totalMateriaPrima += impacto.materiaPrimaAhorrada
        }

        val arboles = totalCO2 / 150.0
        val kmAuto = (totalCO2 / 2.4) * 20.09
        val litrosGas = totalCO2 / 2.4

        return ImpactoAmbiental(
            co2Evitado = totalCO2,
            energiaAhorrada = totalEnergia,
            aguaAhorrada = totalAgua,
            materiaPrimaAhorrada = totalMateriaPrima,
            arbolesEquivalentes = arboles,
            kmEnAuto = kmAuto,
            litrosGasolina = litrosGas
        )
    }

    /**
     * Obtiene un mensaje de impacto amigable para el usuario
     */
    fun getMensajeImpacto(impacto: ImpactoAmbiental): String {
        return buildString {
            append("¡Felicitaciones! Con este reciclaje:")
            append("\n\n")

            if (impacto.arbolesEquivalentes >= 0.1) {
                append("🌳 Equivale a plantar ${String.format("%.1f", impacto.arbolesEquivalentes)} árboles\n")
            }

            if (impacto.co2Evitado >= 1.0) {
                append("🌍 Evitaste ${String.format("%.1f", impacto.co2Evitado)} kg de CO₂\n")
            }

            if (impacto.energiaAhorrada >= 1.0) {
                val casas = impacto.energiaAhorrada / 1.242
                append("⚡ Ahorraste energía para iluminar ${String.format("%.0f", casas)} casas por 1 día\n")
            }

            if (impacto.aguaAhorrada >= 1.0) {
                val duchas = impacto.aguaAhorrada / 200.0
                append("💧 Ahorraste ${String.format("%.0f", impacto.aguaAhorrada)} litros de agua (${String.format("%.1f", duchas)} duchas)\n")
            }

            append("\n¡Gracias por reciclar y cuidar el planeta!")
        }
    }

    /**
     * Mapeo de nombres simplificados a nombres técnicos
     */
    fun mapearNombreMaterial(nombreSimple: String): String {
        return when (nombreSimple.lowercase()) {
            "plástico pet", "pet", "botellas pet" -> "PET"
            "plástico pead", "pead", "plástico hdpe" -> "PEAD"
            "plástico pebd", "pebd", "bolsas plásticas" -> "PEBD"
            "polipropileno", "pp" -> "Polipropileno"
            "papel y cartón", "cartón" -> "Cartón"
            "papel" -> "Papel"
            "vidrio" -> "Vidrio"
            "metal (latas)", "aluminio", "latas aluminio" -> "Aluminio"
            "hojalata", "latas hojalata" -> "Hojalata"
            "tetra pak", "cartón multilaminado" -> "Cartón Multilaminado"
            else -> nombreSimple
        }
    }

    /**
     * Calcula BioCoins ganados basados en el impacto
     *
     * Regla: 1 BioC oin por cada 0.1 kg de CO₂ evitado
     * (Puede ajustarse según necesidad del negocio)
     */
    fun calcularBioCoins(impacto: ImpactoAmbiental): Int {
        return (impacto.co2Evitado / 0.1).toInt()
    }

    /**
     * Calcula puntos de experiencia para gamificación
     *
     * Basado en: kg de material reciclado + bonus por frecuencia
     */
    fun calcularPuntos(pesoTotal: Double, esReciclajeConsecutivo: Boolean = false): Int {
        val puntosBase = (pesoTotal * 10).toInt()  // 10 puntos por kg
        val bonusConsecutivo = if (esReciclajeConsecutivo) (puntosBase * 0.2).toInt() else 0
        return puntosBase + bonusConsecutivo
    }
}
