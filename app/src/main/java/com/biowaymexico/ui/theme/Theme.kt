package com.biowaymexico.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Esquema de colores oscuros para BioWay
 * (Nota: BioWay principalmente usa tema claro, pero mantenemos soporte para modo oscuro)
 */
private val DarkColorScheme = darkColorScheme(
    primary = BioWayColors.PrimaryGreen,
    secondary = BioWayColors.Turquoise,
    tertiary = BioWayColors.AccentPink,
    background = BioWayColors.DarkGrey,
    surface = BioWayColors.SoftBlack,
    error = BioWayColors.Error,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    onError = Color.White,
)

/**
 * Esquema de colores claros para BioWay (Principal)
 */
private val LightColorScheme = lightColorScheme(
    primary = BioWayColors.PrimaryGreen,
    onPrimary = Color.White,
    primaryContainer = BioWayColors.LightGreen,
    onPrimaryContainer = BioWayColors.DarkGreen,

    secondary = BioWayColors.Turquoise,
    onSecondary = Color.White,
    secondaryContainer = BioWayColors.AquaGreen,
    onSecondaryContainer = BioWayColors.DeepGreen,

    tertiary = BioWayColors.AccentPink,
    onTertiary = Color.White,

    background = BioWayColors.BackgroundGrey,
    onBackground = BioWayColors.TextDark,

    surface = Color.White,
    onSurface = BioWayColors.TextDark,
    surfaceVariant = BioWayColors.LightGrey,
    onSurfaceVariant = BioWayColors.TextGrey,

    error = BioWayColors.Error,
    onError = Color.White,
    errorContainer = BioWayColors.Error.copy(alpha = 0.1f),
    onErrorContainer = BioWayColors.Error,

    outline = BioWayColors.LightGrey,
    outlineVariant = BioWayColors.BackgroundGrey,
)

/**
 * Tema principal de BioWay
 * Deshabilitamos el color dinámico para mantener la identidad de marca consistente
 */
@Composable
fun BioWayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

/**
 * Alias para compatibilidad con el nombre generado por Android Studio
 */
@Composable
fun BiowaymexicoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) = BioWayTheme(darkTheme = darkTheme, content = content)