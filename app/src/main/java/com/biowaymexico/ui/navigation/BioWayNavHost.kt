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
            com.biowaymexico.ui.screens.brindador.CelularEnBoteNFCScreen(
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
            // Se implementará con la estructura de pantallas del módulo Maestro
            com.biowaymexico.ui.screens.maestro.MaestroHomeScreen(navController)
        }
    }
}
