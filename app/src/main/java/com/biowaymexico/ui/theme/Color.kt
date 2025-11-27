package com.biowaymexico.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Sistema de colores de BioWay
 * Migrado desde el proyecto Flutter original
 *
 * ESTÁNDAR VISUAL 2024:
 * - Verde principal: #75ee8a
 * - Verde turquesa: #b3fcd4
 * - Azul: #00dfff
 */
object BioWayColors {

    // ===== COLORES PRINCIPALES ESTÁNDAR 2024 =====

    /** Verde principal del estándar visual 2024 */
    val BrandGreen = Color(0xFF75EE8A)

    /** Verde turquesa del estándar visual 2024 */
    val BrandTurquoise = Color(0xFFB3FCD4)

    /** Azul del estándar visual 2024 */
    val BrandBlue = Color(0xFF00DFFF)

    /** Verde oscuro para textos sobre degradados - Estándar visual 2024 */
    val BrandDarkGreen = Color(0xFF007565)

    // ===== COLORES PRINCIPALES DE BIOWAY (Legacy) =====

    /** Verde principal de la marca BioWay */
    val PrimaryGreen = Color(0xFF70D997)

    /** Verde para elementos no degradados (nav bar, etc) */
    val NavGreen = Color(0xFF74D15F)

    /** Verde oscuro para textos y elementos destacados */
    val DarkGreen = Color(0xFF3DB388)

    /** Verde más oscuro para textos importantes */
    val DeepGreen = Color(0xFF00896F)

    /** Verde claro para fondos y elementos sutiles */
    val LightGreen = Color(0xFFA3FFA6)

    /** Verde medio */
    val MediumGreen = Color(0xFF90EE80)

    /** Verde agua pastel */
    val AquaGreen = Color(0xFFC3FACC)

    /** Turquesa brillante */
    val Turquoise = Color(0xFF3FD9FF)

    /** Verde lima brillante para acentos */
    val LimeGreen = Color(0xFF90EE80)

    // ===== COLORES DE ECOCE =====

    /** Verde característico de ECOCE */
    val EcoceGreen = Color(0xFF1EA24D)

    /** Verde claro de ECOCE para fondos */
    val EcoceLight = Color(0xFFC3FACC)

    /** Verde oscuro de ECOCE para textos */
    val EcoceDark = Color(0xFF00896F)

    // ===== COLOR DEL INDICADOR DE SWITCH =====

    /** Turquesa para el indicador de cambio de plataforma */
    val SwitchBlue = Color(0xFF3FD9FF)

    /** Azul medio para estados hover del switch */
    val SwitchBlueLight = Color(0xFF1F97E7)

    /** Morado para elementos especiales */
    val SwitchPurple = Color(0xFF6957BD)

    // ===== COLORES NEUTROS =====

    /** Gris para textos secundarios */
    val TextGrey = Color(0xFF666666)

    /** Gris claro para bordes y divisores */
    val LightGrey = Color(0xFFE5E5E5)

    /** Gris muy claro para fondos */
    val BackgroundGrey = Color(0xFFF9FAFB)

    /** Gris oscuro para textos importantes */
    val DarkGrey = Color(0xFF333333)

    /** Negro suave para textos */
    val SoftBlack = Color(0xFF1F2937)

    /** Color oscuro para textos principales */
    val TextDark = Color(0xFF1F2937)

    // ===== COLORES DE ESTADO =====

    /** Verde para estados de éxito */
    val Success = Color(0xFF00D665)

    /** Rojo para errores y alertas */
    val Error = Color(0xFFEF4444)

    /** Amarillo/naranja para advertencias */
    val Warning = Color(0xFFF59E0B)

    /** Azul turquesa para información */
    val Info = Color(0xFF00C4E5)

    /** Azul para información */
    val InfoBlue = Color(0xFF2196F3)

    /** Verde para éxito */
    val SuccessGreen = Color(0xFF4CAF50)

    /** Naranja para acentos */
    val OrangeAccent = Color(0xFFFF9800)

    /** Morado para acentos */
    val PurpleAccent = Color(0xFF9C27B0)

    /** Azul para acentos */
    val BlueAccent = Color(0xFF2196F3)

    /** Verde para acentos */
    val GreenAccent = Color(0xFF4CAF50)

    /** Café para acentos */
    val BrownAccent = Color(0xFF795548)

    // ===== ALIAS PARA COMPATIBILIDAD =====

    /** Alias para el color primario principal */
    val Primary = PrimaryGreen

    /** Alias para el color primario claro */
    val PrimaryLight = LightGreen

    /** Alias para el color primario oscuro */
    val PrimaryDark = DarkGreen

    /** Alias para el color secundario */
    val Secondary = Turquoise

    // ===== COLORES DE MATERIALES (RECICLAJE) =====

    /** Color para PEBD - rosa */
    val PebdPink = Color(0xFFEC4899)

    /** Color para PP - morado */
    val PpPurple = Color(0xFF9333EA)

    /** Color para Multilaminado - café */
    val MultilaminadoBrown = Color(0xFF92400E)

    /** Color para vidrio - verde esmeralda */
    val GlassGreen = Color(0xFF00D665)

    /** Color para metal - gris metálico */
    val MetalGrey = Color(0xFF6B7280)

    /** Color para reciclaje - naranja reciclaje */
    val RecycleOrange = Color(0xFFFF6B00)

    // ===== COLORES ESPECIALES PARA LA INTERFAZ =====

    /** Rosa para elementos destacados */
    val AccentPink = Color(0xFFFF6B9D)

    // ===== COLORES LEGACY (para compatibilidad) =====

    val PetBlue = Color(0xFF0085FF)
    val HdpeGreen = Color(0xFF00A854)
    val PpOrange = Color(0xFFFF7A00)
    val OtherPurple = Color(0xFF9333EA)
    val PvcRed = Color(0xFFE53935)
    val PsYellow = Color(0xFFFFB300)

    /** Amarillo vibrante para notificaciones */
    val BrightYellow = Color(0xFFFFD93D)

    /** Azul profundo para elementos de navegación */
    val DeepBlue = Color(0xFF0066CC)

    // ===== SOMBRAS Y OVERLAYS =====

    /** Color para sombras suaves */
    val ShadowColor = Color(0x1A000000)

    /** Color para overlays oscuros */
    val DarkOverlay = Color(0x80000000)

    /** Color para overlays claros */
    val LightOverlay = Color(0xCCFFFFFF)

    /** Sombra verde para elementos principales */
    val GreenShadow = Color(0x4D70D997)

    /** Sombra turquesa para elementos secundarios */
    val AquaShadow = Color(0x4DC3FACC)
}