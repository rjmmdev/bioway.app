package com.biowaymexico.ui.screens.maestro

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.biowaymexico.ui.components.BioWayCard
import com.biowaymexico.ui.components.BioWayGradientCard
import com.biowaymexico.ui.components.BioWayStatCard
import com.biowaymexico.ui.theme.BioWayColors
import com.biowaymexico.ui.theme.BioWayGradients

/**
 * Pantalla principal del Maestro (Administrador)
 * Panel de control con acceso a todos los módulos administrativos
 */
@Composable
fun MaestroHomeScreen(navController: NavHostController) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BioWayColors.BackgroundGrey),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            BioWayGradientCard(
                gradient = BioWayGradients.AquaGradient,
                elevation = 6.dp
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "👨‍💼", fontSize = 64.sp)
                    Text(
                        "Panel Maestro",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "Administración BioWay",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }

        item {
            Text(
                "Estadísticas Generales",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = BioWayColors.TextDark
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BioWayStatCard(
                    value = "1,234",
                    label = "Usuarios",
                    modifier = Modifier.weight(1f)
                )
                BioWayStatCard(
                    value = "856",
                    label = "Activos",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BioWayStatCard(
                    value = "45 ton",
                    label = "Recicladas",
                    modifier = Modifier.weight(1f),
                    gradient = androidx.compose.ui.graphics.Brush.linearGradient(
                        listOf(BioWayColors.PrimaryGreen, BioWayColors.LightGreen)
                    )
                )
                BioWayStatCard(
                    value = "12",
                    label = "Empresas",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Text(
                "Módulos de Administración",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = BioWayColors.TextDark
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AdminModuleCard(
                    icon = Icons.Default.Business,
                    title = "Gestión de Empresas",
                    subtitle = "Administrar empresas asociadas",
                    color = BioWayColors.PrimaryGreen,
                    onClick = { }
                )
                AdminModuleCard(
                    icon = Icons.Default.People,
                    title = "Gestión de Usuarios",
                    subtitle = "Ver y administrar usuarios",
                    color = BioWayColors.Turquoise,
                    onClick = { }
                )
                AdminModuleCard(
                    icon = Icons.Default.Recycling,
                    title = "Materiales",
                    subtitle = "Catálogo de materiales reciclables",
                    color = BioWayColors.Success,
                    onClick = { }
                )
                AdminModuleCard(
                    icon = Icons.Default.Schedule,
                    title = "Horarios de Recolección",
                    subtitle = "Configurar horarios por zona",
                    color = BioWayColors.Warning,
                    onClick = { }
                )
                AdminModuleCard(
                    icon = Icons.Default.Map,
                    title = "Zonas Habilitadas",
                    subtitle = "Gestionar disponibilidad geográfica",
                    color = BioWayColors.Info,
                    onClick = { }
                )
                AdminModuleCard(
                    icon = Icons.Default.Settings,
                    title = "Configuración General",
                    subtitle = "Parámetros del sistema",
                    color = BioWayColors.TextGrey,
                    onClick = { }
                )
            }
        }

        item {
            Divider()
        }

        item {
            BioWayCard(
                onClick = { }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Logout,
                        contentDescription = null,
                        tint = BioWayColors.Error,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        "Cerrar Sesión",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = BioWayColors.Error
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminModuleCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit
) {
    BioWayCard(
        onClick = onClick,
        elevation = 3.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = color.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = BioWayColors.TextDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    subtitle,
                    fontSize = 13.sp,
                    color = BioWayColors.TextGrey
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = BioWayColors.TextGrey
            )
        }
    }
}
