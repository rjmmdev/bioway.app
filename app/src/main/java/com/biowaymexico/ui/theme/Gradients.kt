package com.biowaymexico.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Gradientes de BioWay
 * Migrado desde el proyecto Flutter original
 */
object BioWayGradients {

    /**
     * Gradiente principal de fondo
     * Verde lime -> Verde agua pastel -> Turquesa brillante
     */
    val BackgroundGradient = Brush.linearGradient(
        colors = listOf(
            BioWayColors.MediumGreen,      // Verde lime
            BioWayColors.AquaGreen,        // Verde agua pastel
            BioWayColors.Turquoise,        // Turquesa brillante
        ),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    /**
     * Gradiente suave verde
     * Verde oscuro -> Verde principal
     */
    val SoftGradient = Brush.linearGradient(
        colors = listOf(
            BioWayColors.DarkGreen,        // Verde oscuro
            BioWayColors.PrimaryGreen,     // Verde principal
        ),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    /**
     * Gradiente verde claro
     * Verde principal -> Verde claro
     */
    val AccentGradient = Brush.linearGradient(
        colors = listOf(
            BioWayColors.PrimaryGreen,     // Verde principal
            BioWayColors.LightGreen,       // Verde claro
        ),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    /**
     * Gradiente turquesa-morado
     * Turquesa brillante -> Azul medio -> Morado
     */
    val AquaGradient = Brush.linearGradient(
        colors = listOf(
            BioWayColors.Turquoise,        // Turquesa brillante
            BioWayColors.SwitchBlueLight,  // Azul medio
            BioWayColors.SwitchPurple,     // Morado
        ),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    /**
     * Gradiente principal inverso
     * Turquesa brillante -> Verde agua pastel -> Verde lime
     */
    val MainGradient = Brush.linearGradient(
        colors = listOf(
            BioWayColors.Turquoise,        // Turquesa brillante
            BioWayColors.AquaGreen,        // Verde agua pastel
            BioWayColors.MediumGreen,      // Verde lime
        ),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    /**
     * Gradiente cálido
     * Amarillo suave -> Verde profundo
     */
    val WarmGradient = Brush.linearGradient(
        colors = listOf(
            BioWayColors.Warning.copy(alpha = 0.7f), // Amarillo suave
            BioWayColors.DeepGreen,                   // Verde profundo
        ),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    /**
     * Gradiente vertical (de arriba hacia abajo)
     */
    fun vertical(colors: List<Color>) = Brush.verticalGradient(
        colors = colors
    )

    /**
     * Gradiente horizontal (de izquierda a derecha)
     */
    fun horizontal(colors: List<Color>) = Brush.horizontalGradient(
        colors = colors
    )

    /**
     * Gradiente radial (desde el centro)
     */
    fun radial(colors: List<Color>) = Brush.radialGradient(
        colors = colors
    )
}
