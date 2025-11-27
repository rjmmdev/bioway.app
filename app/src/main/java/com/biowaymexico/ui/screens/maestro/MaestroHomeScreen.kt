package com.biowaymexico.ui.screens.maestro

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.biowaymexico.R
import com.biowaymexico.ui.theme.BioWayColors

/**
 * Pantalla del Administrador (Maestro) - Diseño Moderno Minimalista 2024
 * Panel de control con estándar visual de BioWay
 */
@Composable
fun MaestroHomeScreen(navController: NavHostController) {
    Scaffold(
        containerColor = Color(0xFFFAFAFA)  // Mismo fondo que módulo Brindador
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            item {
                // Header - misma altura que Dashboard
                MaestroHeader()
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                // Stats generales
                StatsSection()
                Spacer(modifier = Modifier.height(32.dp))
            }

            item {
                // Título de módulos
                Text(
                    text = "Módulos de Administración",
                    style = MaterialTheme.typography.titleLarge,  // Hammersmith One
                    color = BioWayColors.BrandDarkGreen,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                // Módulos administrativos
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AdminModuleCard(
                        icon = Icons.Default.DeleteOutline,
                        title = "Gestión de Botes BioWay",
                        subtitle = "Crear y administrar botes inteligentes",
                        onClick = {
                            navController.navigate(com.biowaymexico.ui.navigation.BioWayDestinations.MaestroBotes.route)
                        }
                    )
                    AdminModuleCard(
                        icon = Icons.Default.Business,
                        title = "Gestión de Empresas",
                        subtitle = "Administrar empresas asociadas",
                        onClick = { /* TODO */ }
                    )
                    AdminModuleCard(
                        icon = Icons.Default.People,
                        title = "Gestión de Usuarios",
                        subtitle = "Ver y administrar usuarios",
                        onClick = { /* TODO */ }
                    )
                    AdminModuleCard(
                        icon = Icons.Default.Recycling,
                        title = "Materiales Reciclables",
                        subtitle = "Catálogo de materiales",
                        onClick = { /* TODO */ }
                    )
                    AdminModuleCard(
                        icon = Icons.Default.Schedule,
                        title = "Horarios de Recolección",
                        subtitle = "Configurar horarios por zona",
                        onClick = { /* TODO */ }
                    )
                    AdminModuleCard(
                        icon = Icons.Default.Map,
                        title = "Zonas de Cobertura",
                        subtitle = "Definir áreas de servicio",
                        onClick = { /* TODO */ }
                    )
                    AdminModuleCard(
                        icon = Icons.Default.Assessment,
                        title = "Reportes y Estadísticas",
                        subtitle = "Análisis de datos",
                        onClick = { /* TODO */ }
                    )
                    AdminModuleCard(
                        icon = Icons.Default.Settings,
                        title = "Configuración General",
                        subtitle = "Parámetros del sistema",
                        onClick = { /* TODO */ }
                    )
                    AdminModuleCard(
                        icon = Icons.Default.Logout,
                        title = "Cerrar Sesión",
                        subtitle = "Salir del panel",
                        onClick = {
                            com.biowaymexico.data.AuthRepository().logout()
                            navController.navigate(com.biowaymexico.ui.navigation.BioWayDestinations.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        isDestructive = true
                    )
                }
            }
        }
    }
}

@Composable
private fun MaestroHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 0.dp, bottom = 8.dp),  // Misma altura que Dashboard
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Panel Maestro",
                style = MaterialTheme.typography.headlineMedium,  // Hammersmith One 28sp
                color = BioWayColors.BrandDarkGreen
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Administración BioWay",
                style = MaterialTheme.typography.bodySmall,  // Montserrat
                color = Color(0xFF2E7D6C)
            )
        }

        // Logo BioWay
        Image(
            painter = painterResource(id = R.drawable.ic_bioway_logo),
            contentDescription = "BioWay Logo",
            modifier = Modifier.size(50.dp)
        )
    }
}

@Composable
private fun StatsSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                icon = Icons.Default.People,
                valor = "1,234",
                label = "Usuarios",
                color = Color(0xFF70D162),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                icon = Icons.Default.CheckCircle,
                valor = "856",
                label = "Activos",
                color = BioWayColors.BrandGreen,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                icon = Icons.Default.Recycling,
                valor = "45 ton",
                label = "Recicladas",
                color = BioWayColors.BrandTurquoise,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                icon = Icons.Default.Business,
                valor = "12",
                label = "Empresas",
                color = BioWayColors.BrandBlue,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    valor: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(
                        color = color.copy(alpha = 0.15f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = valor,
                style = MaterialTheme.typography.headlineSmall,  // Hammersmith One
                color = BioWayColors.BrandDarkGreen
            )

            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,  // Montserrat
                color = Color(0xFF2E7D6C)
            )
        }
    }
}

@Composable
private fun AdminModuleCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
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
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = if (isDestructive)
                            Color(0xFFE53935).copy(alpha = 0.12f)
                        else
                            BioWayColors.BrandGreen.copy(alpha = 0.12f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isDestructive) Color(0xFFE53935) else BioWayColors.BrandGreen,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,  // Hammersmith One
                    color = if (isDestructive)
                        Color(0xFFE53935)
                    else
                        BioWayColors.BrandDarkGreen
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,  // Montserrat
                    color = Color(0xFF2E7D6C),
                    fontSize = 11.sp
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF2E7D6C).copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
