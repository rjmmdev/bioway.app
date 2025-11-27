package com.biowaymexico.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.biowaymexico.ui.screens.auth.LoginScreen
import com.biowaymexico.ui.screens.auth.PlatformSelectorScreen
import com.biowaymexico.ui.screens.auth.RegisterScreen
import com.biowaymexico.ui.screens.splash.SplashScreen

/**
 * NavHost principal de BioWay
 * Maneja toda la navegación de la aplicación
 */
@Composable
fun BioWayNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = BioWayDestinations.Splash.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // === SPLASH ===
        composable(BioWayDestinations.Splash.route) {
            SplashScreen(
                onNavigateNext = {
                    // Ir directo a Login (PlatformSelector deshabilitado)
                    navController.navigate(BioWayDestinations.Login.route) {
                        popUpTo(BioWayDestinations.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // === PLATFORM SELECTOR === (DESHABILITADO - mantener por si se necesita después)
        /*
        composable(BioWayDestinations.PlatformSelector.route) {
            PlatformSelectorScreen(
                onBioWaySelected = {
                    navController.navigate(BioWayDestinations.Login.route)
                },
                onEcoceSelected = {
                    // TODO: Implementar ECOCE
                }
            )
        }
        */

        // === AUTENTICACIÓN ===
        composable(BioWayDestinations.Login.route) {
            LoginScreen(
                onLoginSuccess = { userType ->
                    val destination = getUserHomeDestination(userType)
                    navController.navigate(destination.route) {
                        popUpTo(BioWayDestinations.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(BioWayDestinations.Register.route)
                }
            )
        }

        composable(BioWayDestinations.Register.route) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.popBackStack()
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // === BRINDADOR ===
        composable(BioWayDestinations.BrindadorMain.route) {
            com.biowaymexico.ui.screens.brindador.BrindadorMainScreen(
                onNavigateToClasificador = {
                    navController.navigate(BioWayDestinations.BrindadorClasificador.route)
                },
                onNavigateToUsuarioNormalNFC = {
                    navController.navigate(BioWayDestinations.BrindadorUsuarioNormalNFC.route)
                },
                onNavigateToCelularEnBoteNFC = {
                    navController.navigate(BioWayDestinations.BrindadorCelularEnBoteNFC.route)
                },
                onNavigateToUsuarioNormalNearby = {
                    navController.navigate(BioWayDestinations.BrindadorUsuarioNormalNearby.route)
                },
                onNavigateToCelularEnBoteNearby = {
                    navController.navigate(BioWayDestinations.BrindadorCelularEnBoteNearby.route)
                },
                onNavigateToReciclarAhora = {
                    navController.navigate(BioWayDestinations.BrindadorReciclarAhora.route)
                },
                onNavigateToImpactoAmbiental = {
                    navController.navigate(BioWayDestinations.BrindadorImpactoAmbiental.route)
                },
                onLogout = {
                    navController.navigate(BioWayDestinations.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(BioWayDestinations.BrindadorClasificador.route) {
            com.biowaymexico.ui.screens.brindador.ClasificadorScreenYOLO(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(BioWayDestinations.BrindadorUsuarioNormalNFC.route) {
            com.biowaymexico.ui.screens.brindador.UsuarioNormalNFCScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(BioWayDestinations.BrindadorCelularEnBoteNFC.route) {
            com.biowaymexico.ui.screens.bote_bioway.CelularEnBoteNFCScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(BioWayDestinations.BrindadorUsuarioNormalNearby.route) {
            com.biowaymexico.ui.screens.brindador.UsuarioNormalNearbyScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(BioWayDestinations.BrindadorCelularEnBoteNearby.route) {
            com.biowaymexico.ui.screens.bote_bioway.CelularEnBoteNearbyScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(BioWayDestinations.BrindadorReciclarAhora.route) {
            com.biowaymexico.ui.screens.brindador.ReciclarAhoraScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onReciclajeCompletado = {
                    navController.popBackStack()
                }
            )
        }

        composable(BioWayDestinations.BrindadorImpactoAmbiental.route) {
            com.biowaymexico.ui.screens.brindador.ImpactoAmbientalScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // === RECOLECTOR ===
        composable(BioWayDestinations.RecolectorMain.route) {
            com.biowaymexico.ui.screens.recolector.RecolectorMainScreen()
        }

        // === CENTRO DE ACOPIO ===
        composable(BioWayDestinations.CentroAcopioHome.route) {
            // Se implementará con la estructura de pantallas del módulo Centro de Acopio
            com.biowaymexico.ui.screens.centro_acopio.CentroAcopioHomeScreen(navController)
        }

        // === MAESTRO/ADMIN ===
        composable(BioWayDestinations.MaestroHome.route) {
            com.biowaymexico.ui.screens.maestro.MaestroHomeScreen(navController)
        }

        composable(BioWayDestinations.MaestroBotes.route) {
            com.biowaymexico.ui.screens.maestro.MaestroBotesScreen(navController)
        }

        composable(
            route = BioWayDestinations.MaestroCrearBote.route,
            enterTransition = { androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(0)) },
            exitTransition = { androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(0)) },
            popEnterTransition = { androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(0)) },
            popExitTransition = { androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(0)) }
        ) {
            com.biowaymexico.ui.screens.maestro.CrearBoteScreen(navController)
        }

        composable(
            route = BioWayDestinations.MaestroMapaSelectorBote.route,
            arguments = listOf(
                androidx.navigation.navArgument("estado") { type = androidx.navigation.NavType.StringType },
                androidx.navigation.navArgument("municipio") { type = androidx.navigation.NavType.StringType },
                androidx.navigation.navArgument("colonia") { type = androidx.navigation.NavType.StringType },
                androidx.navigation.navArgument("cp") { type = androidx.navigation.NavType.StringType }
            )
        ) { backStackEntry ->
            val estado = backStackEntry.arguments?.getString("estado") ?: "CDMX"
            val municipio = backStackEntry.arguments?.getString("municipio") ?: ""
            val colonia = backStackEntry.arguments?.getString("colonia") ?: ""
            val cp = backStackEntry.arguments?.getString("cp") ?: ""

            com.biowaymexico.ui.screens.maestro.MapaSelectorBoteScreen(
                estado = estado,
                municipio = municipio,
                colonia = colonia,
                codigoPostal = cp,
                navController = navController,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // === BOTE BIOWAY ===
        composable(BioWayDestinations.BoteBioWayMain.route) {
            com.biowaymexico.ui.screens.bote_bioway.BoteBioWayMainScreen(
                onNavigateToNFC = { navController.navigate(BioWayDestinations.BoteBioWayNFC.route) },
                onNavigateToNearby = { navController.navigate(BioWayDestinations.BoteBioWayNearby.route) },
                onNavigateToClasificador = { navController.navigate(BioWayDestinations.BoteBioWayClasificador.route) },
                onNavigateToClasificadorYOLO = { navController.navigate(BioWayDestinations.BoteBioWayClasificadorYOLO.route) },
                onNavigateToPruebaServos = { navController.navigate(BioWayDestinations.BoteBioWayPruebaServos.route) },
                onLogout = {
                    navController.navigate(BioWayDestinations.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(BioWayDestinations.BoteBioWayNFC.route) {
            com.biowaymexico.ui.screens.bote_bioway.CelularEnBoteNFCScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(BioWayDestinations.BoteBioWayNearby.route) {
            com.biowaymexico.ui.screens.bote_bioway.CelularEnBoteNearbyScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(BioWayDestinations.BoteBioWayClasificador.route) {
            com.biowaymexico.ui.screens.bote_bioway.ClasificadorBoteScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(BioWayDestinations.BoteBioWayPruebaServos.route) {
            com.biowaymexico.ui.screens.bote_bioway.PruebaServosScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(BioWayDestinations.BoteBioWayClasificadorYOLO.route) {
            com.biowaymexico.ui.screens.bote_bioway.ClasificadorBoteYOLOScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
