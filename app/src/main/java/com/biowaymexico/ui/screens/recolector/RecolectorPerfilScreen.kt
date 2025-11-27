package com.biowaymexico.ui.screens.recolector

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biowaymexico.ui.theme.BioWayColors

/**
 * Pantalla de perfil del Recolector
 * Replicado fielmente del diseño Flutter original
 */
@Composable
fun RecolectorPerfilScreen() {
    Scaffold(
        containerColor = Color(0xFFF8F9FA)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            item { BuildHeader() }
            item { Spacer(modifier = Modifier.height(20.dp)) }
            item { 
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    BuildStatsOverview()
                }
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
            item {
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    BuildTodayStats()
                }
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
            item {
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    BuildImpactSection()
                }
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
            item {
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    BuildMaterialsBreakdown()
                }
            }
        }
    }
}

@Composable
private fun BuildHeader() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 10.dp,
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Carlos Recolector",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(25.dp),
                color = BioWayColors.NavGreen.copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalShipping,
                        contentDescription = null,
                        tint = BioWayColors.NavGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Recolector Certificado",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BioWayColors.NavGreen
                    )
                }
            }
        }
    }
}

@Composable
private fun BuildStatsOverview() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 20.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = BioWayColors.NavGreen.copy(alpha = 0.3f)
            ),
        shape = RoundedCornerShape(20.dp),
        color = Color.White
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            BioWayColors.MediumGreen,
                            BioWayColors.AquaGreen
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                BuildStatItem(
                    icon = Icons.Default.MonetizationOn,
                    value = "2500",
                    label = "BioCoins",
                    iconColor = Color(0xFFFFB300)
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .background(Color.White.copy(alpha = 0.3f))
                )
                BuildStatItem(
                    icon = Icons.Default.EmojiEvents,
                    value = "Guardián Verde",
                    label = "Nivel",
                    iconColor = Color.White
                )
            }
        }
    }
}

@Composable
private fun BuildStatItem(
    icon: ImageVector,
    value: String,
    label: String,
    iconColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF00553F)
        )
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color(0xFF00553F)
        )
    }
}

@Composable
private fun BuildTodayStats() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 10.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = BioWayColors.NavGreen.copy(alpha = 0.1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Today,
                        contentDescription = null,
                        tint = BioWayColors.NavGreen,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Actividad de Hoy",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                BuildSimpleStatCard(
                    icon = Icons.Default.LocationOn,
                    value = "12",
                    label = "Puntos\nvisitados",
                    color = BioWayColors.NavGreen,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                BuildSimpleStatCard(
                    icon = Icons.Default.Scale,
                    value = "250",
                    label = "Kilos\nrecolectados",
                    color = BioWayColors.InfoBlue,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun BuildSimpleStatCard(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 14.sp,
                color = Color(0xFF999999),
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun BuildImpactSection() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 10.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = BioWayColors.SuccessGreen.copy(alpha = 0.1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Eco,
                        contentDescription = null,
                        tint = BioWayColors.SuccessGreen,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Impacto Ambiental Total",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                BuildSimpleStatCard(
                    icon = Icons.Default.Delete,
                    value = "120.5",
                    label = "Kg totales\nreciclados",
                    color = BioWayColors.SuccessGreen,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                BuildSimpleStatCard(
                    icon = Icons.Default.Cloud,
                    value = "250.8",
                    label = "Kg CO₂\nevitados",
                    color = BioWayColors.PrimaryGreen,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun BuildMaterialsBreakdown() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 10.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Materiales Recolectados",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A)
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            MaterialItem("Plástico", 45.5, BioWayColors.BlueAccent)
            Spacer(modifier = Modifier.height(12.dp))
            MaterialItem("Vidrio", 28.3, BioWayColors.GreenAccent)
            Spacer(modifier = Modifier.height(12.dp))
            MaterialItem("Papel", 18.0, BioWayColors.BrownAccent)
            Spacer(modifier = Modifier.height(12.dp))
            MaterialItem("Metal", 15.2, Color(0xFF9E9E9E))
            Spacer(modifier = Modifier.height(12.dp))
            MaterialItem("Orgánico", 10.0, BioWayColors.OrangeAccent)
            Spacer(modifier = Modifier.height(12.dp))
            MaterialItem("Electrónico", 3.5, BioWayColors.InfoBlue)
        }
    }
}

@Composable
private fun MaterialItem(name: String, kg: Double, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, shape = RoundedCornerShape(6.dp))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = name,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF333333),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "$kg kg",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
