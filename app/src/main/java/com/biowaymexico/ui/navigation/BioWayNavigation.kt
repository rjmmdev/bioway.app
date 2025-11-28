package com.biowaymexico.ui.navigation

/**
 * Rutas de navegación de BioWay
 * Define todas las pantallas/destinos de la aplicación
 */
sealed class BioWayDestinations(val route: String) {
    // === PANTALLAS DE INICIO ===
    data object Splash : BioWayDestinations("splash")
    data object PlatformSelector : BioWayDestinations("platform_selector")

    // === AUTENTICACIÓN ===
    data object Login : BioWayDestinations("login")
    data object Register : BioWayDestinations("register")

    // === BRINDADOR (CIUDADANO) ===
    data object BrindadorMain : BioWayDestinations("brindador_main")
    data object BrindadorDashboard : BioWayDestinations("brindador_dashboard")
    data object BrindadorTirar : BioWayDestinations("brindador_tirar")
    data object BrindadorScanner : BioWayDestinations("brindador_scanner")
    data object BrindadorComercio : BioWayDestinations("brindador_comercio")
    data object BrindadorPerfil : BioWayDestinations("brindador_perfil")
    data object BrindadorCompetencias : BioWayDestinations("brindador_competencias")
    data object BrindadorClasificador : BioWayDestinations("brindador_clasificador")
    data object BrindadorUsuarioNormalNFC : BioWayDestinations("brindador_usuario_normal_nfc")
    data object BrindadorCelularEnBoteNFC : BioWayDestinations("brindador_celular_en_bote_nfc")
    data object BrindadorUsuarioNormalNearby : BioWayDestinations("brindador_usuario_normal_nearby")
    data object BrindadorCelularEnBoteNearby : BioWayDestinations("brindador_celular_en_bote_nearby")
    data object BrindadorReciclarAhora : BioWayDestinations("brindador_reciclar_ahora")
    data object BrindadorImpactoAmbiental : BioWayDestinations("brindador_impacto_ambiental")

    // === RECOLECTOR ===
    data object RecolectorMain : BioWayDestinations("recolector_main")
    data object RecolectorMapa : BioWayDestinations("recolector_mapa")
    data object RecolectorPerfil : BioWayDestinations("recolector_perfil")
    data object RecolectorHistorial : BioWayDestinations("recolector_historial")

    // === CENTRO DE ACOPIO ===
    data object CentroAcopioHome : BioWayDestinations("centro_acopio_home")
    data object CentroAcopioRecepcion : BioWayDestinations("centro_acopio_recepcion")
    data object CentroAcopioInventario : BioWayDestinations("centro_acopio_inventario")
    data object CentroAcopioPrepago : BioWayDestinations("centro_acopio_prepago")
    data object CentroAcopioReportes : BioWayDestinations("centro_acopio_reportes")

    // === ADMINISTRADOR (MAESTRO) ===
    data object MaestroHome : BioWayDestinations("maestro_home")
    data object MaestroEmpresas : BioWayDestinations("maestro_empresas")
    data object MaestroEmpresaForm : BioWayDestinations("maestro_empresa_form/{empresaId}") {
        fun createRoute(empresaId: String = "new") = "maestro_empresa_form/$empresaId"
    }
    data object MaestroUsuarios : BioWayDestinations("maestro_usuarios")
    data object MaestroMateriales : BioWayDestinations("maestro_materiales")
    data object MaestroHorarios : BioWayDestinations("maestro_horarios")
    data object MaestroDisponibilidad : BioWayDestinations("maestro_disponibilidad")
    data object MaestroConfiguracion : BioWayDestinations("maestro_configuracion")
    data object MaestroBotes : BioWayDestinations("maestro_botes")
    data object MaestroCrearBote : BioWayDestinations("maestro_crear_bote")
    data object MaestroMapaSelectorBote : BioWayDestinations("maestro_mapa_selector_bote/{estado}/{municipio}/{colonia}/{cp}") {
        fun createRoute(estado: String, municipio: String, colonia: String, cp: String) =
            "maestro_mapa_selector_bote/${estado}/${municipio}/${colonia}/${cp}"
    }

    // === BOTE BIOWAY (SMART BIN) ===
    data object BoteBioWayMain : BioWayDestinations("bote_bioway_main")
    data object BoteBioWayNFC : BioWayDestinations("bote_bioway_nfc")
    data object BoteBioWayNearby : BioWayDestinations("bote_bioway_nearby")
    data object BoteBioWayClasificador : BioWayDestinations("bote_bioway_clasificador")
    data object BoteBioWayClasificadorYOLO : BioWayDestinations("bote_bioway_clasificador_yolo")
    data object BoteBioWayClasificadorGemini : BioWayDestinations("bote_bioway_clasificador_gemini")
    data object BoteBioWayPruebaServos : BioWayDestinations("bote_bioway_prueba_servos")
}

/**
 * Tipos de usuario para navegación condicional
 */
enum class UserType {
    BRINDADOR,
    RECOLECTOR,
    CENTRO_ACOPIO,
    EMPRESA,
    MAESTRO,
    BOTE_BIOWAY,
    GUEST
}

/**
 * Navegación basada en tipo de usuario
 * Retorna el destino principal según el tipo de usuario
 */
fun getUserHomeDestination(userType: UserType): BioWayDestinations {
    return when (userType) {
        UserType.BRINDADOR -> BioWayDestinations.BrindadorMain
        UserType.RECOLECTOR -> BioWayDestinations.RecolectorMain
        UserType.CENTRO_ACOPIO -> BioWayDestinations.CentroAcopioHome
        UserType.MAESTRO -> BioWayDestinations.MaestroHome
        UserType.BOTE_BIOWAY -> BioWayDestinations.BoteBioWayMain
        UserType.EMPRESA -> BioWayDestinations.MaestroHome
        UserType.GUEST -> BioWayDestinations.Login
    }
}
