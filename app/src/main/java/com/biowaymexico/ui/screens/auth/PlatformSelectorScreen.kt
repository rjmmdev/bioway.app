package com.biowaymexico.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biowaymexico.ui.theme.BioWayColors

/**
 * Pantalla de Selección de Plataforma
 * Permite elegir entre BioWay y ECOCE
 * Migrado desde el proyecto Flutter original
 */
@Composable
fun PlatformSelectorScreen(
    onBioWaySelected: () -> Unit,
    onEcoceSelected: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        BioWayColors.PrimaryGreen,
                        BioWayColors.MediumGreen,
                        BioWayColors.AquaGreen
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Seleccionar Plataforma",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(50.dp))

            Column(
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                PlatformButton(
                    title = "BioWay",
                    icon = Icons.Default.Recycling,
                    color = BioWayColors.PrimaryGreen,
                    onClick = onBioWaySelected
                )

                Spacer(modifier = Modifier.height(20.dp))

                PlatformButton(
                    title = "ECOCE",
                    icon = Icons.Default.Eco,
                    color = BioWayColors.EcoceGreen,
                    onClick = onEcoceSelected,
                    enabled = false // ECOCE próximamente disponible
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Nota de ECOCE próximamente
            Text(
                text = "ECOCE próximamente disponible",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun PlatformButton(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(16.dp))
            .then(
                if (enabled) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) Color.White else Color.White.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 2.dp,
                    color = color.copy(alpha = if (enabled) 0.3f else 0.1f),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color.copy(alpha = if (enabled) 1f else 0.3f),
                    modifier = Modifier.size(32.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = color.copy(alpha = if (enabled) 1f else 0.3f)
                )
            }
        }
    }
}
