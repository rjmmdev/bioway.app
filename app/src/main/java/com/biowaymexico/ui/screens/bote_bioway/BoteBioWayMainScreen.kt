package com.biowaymexico.ui.screens.bote_bioway

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.biowaymexico.ui.theme.BioWayColors

/**
 * Pantalla Principal del Bote BioWay
 * Acceso a las funcionalidades del bote inteligente
 */
@Composable
fun BoteBioWayMainScreen(
    onNavigateToNFC: () -> Unit,
    onNavigateToNearby: () -> Unit,
    onNavigateToClasificador: () -> Unit,
    onNavigateToClasificadorYOLO: () -> Unit = {},
    onNavigateToClasificadorGemini: () -> Unit = {},
    onNavigateToPruebaServos: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    Scaffold(
        containerColor = Color(0xFFFAFAFA)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Text(
                text = "Bote BioWay",
                style = MaterialTheme.typography.headlineMedium,
                color = BioWayColors.BrandDarkGreen
            )

            Text(
                text = "Funcionalidades del bote inteligente",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF2E7D6C)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Opción NFC
            BoteOptionCard(
                icon = Icons.Default.Nfc,
                title = "Detector NFC",
                description = "Detecta usuarios cercanos por NFC (6-10cm)",
                onClick = onNavigateToNFC
            )

            // Opción Nearby
            BoteOptionCard(
                icon = Icons.Default.Wifi,
                title = "Detector Nearby",
                description = "Detecta usuarios cercanos por Nearby (1-10m)",
                onClick = onNavigateToNearby
            )

            // Opción Clasificador (existente)
            BoteOptionCard(
                icon = Icons.Default.PhotoCamera,
                title = "Clasificador IA",
                description = "Identifica residuos con inteligencia artificial",
                onClick = onNavigateToClasificador
            )

            // Opción Clasificador YOLO v2 (nuevo)
            BoteOptionCard(
                icon = Icons.Default.AutoAwesome,
                title = "Clasificador IA v2",
                description = "Nuevo modelo YOLO con 12 categorias (precision mejorada)",
                onClick = onNavigateToClasificadorYOLO
            )

            // Opción Clasificador Gemini (YOLO + Gemini AI)
            BoteOptionCard(
                icon = Icons.Default.Psychology,
                title = "Clasificador IA + Gemini",
                description = "YOLO detecta presencia, Gemini clasifica (mayor precision)",
                onClick = onNavigateToClasificadorGemini
            )

            // Opción Prueba Servos
            BoteOptionCard(
                icon = Icons.Default.Settings,
                title = "Prueba Servos",
                description = "Controlar servomotores manualmente (desarrollo)",
                onClick = onNavigateToPruebaServos
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Cerrar sesión
            BoteOptionCard(
                icon = Icons.Default.Logout,
                title = "Cerrar Sesión",
                description = "Salir del modo Bote BioWay",
                onClick = {
                    com.biowaymexico.data.AuthRepository().logout()
                    onLogout()
                }
            )
        }
    }
}

@Composable
private fun BoteOptionCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = BioWayColors.BrandGreen,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = BioWayColors.BrandDarkGreen,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF2E7D6C)
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Ir",
                tint = Color(0xFF2E7D6C).copy(alpha = 0.4f)
            )
        }
    }
}
