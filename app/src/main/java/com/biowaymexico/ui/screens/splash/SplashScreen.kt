package com.biowaymexico.ui.screens.splash

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biowaymexico.R
import com.biowaymexico.ui.theme.BioWayColors
import com.biowaymexico.ui.theme.BioWayGradients
import kotlinx.coroutines.delay

/**
 * Pantalla de Splash de BioWay
 * Muestra el logo animado y navega automáticamente a la siguiente pantalla
 * Migrado desde el proyecto Flutter original
 */
@Composable
fun SplashScreen(
    onNavigateNext: () -> Unit
) {
    // Estados de animación
    var isVisible by remember { mutableStateOf(false) }

    // Animaciones
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "alpha"
    )

    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    // Efecto para iniciar la animación y navegar
    LaunchedEffect(Unit) {
        isVisible = true
        delay(2500) // Mostrar splash por 2.5 segundos
        onNavigateNext()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        BioWayColors.MediumGreen,
                        BioWayColors.AquaGreen,
                        BioWayColors.Turquoise
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(2f))

            // Logo BioWay oficial con padding
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .scale(scale)
                    .alpha(alpha)
            ) {
                // Contenedor con padding para evitar corte
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_bioway_logo),
                        contentDescription = "BioWay Logo",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Texto animado
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .alpha(alpha)
            ) {
                Text(
                    text = "BioWay",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = BioWayColors.DarkGreen,
                    letterSpacing = 1.5.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Subtítulo
                Box(
                    modifier = Modifier
                        .background(
                            color = BioWayColors.PrimaryGreen.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "MÉXICO",
                        fontSize = 16.sp,
                        color = BioWayColors.DarkGreen,
                        letterSpacing = 4.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Indicador de carga
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.alpha(alpha)
            ) {
                CircularProgressIndicator(
                    color = BioWayColors.PrimaryGreen,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(40.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Iniciando...",
                    fontSize = 14.sp,
                    color = BioWayColors.DarkGreen.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.weight(2f))

            // Versión de la app
            Text(
                text = "v1.0.0",
                fontSize = 12.sp,
                color = BioWayColors.DarkGreen.copy(alpha = 0.4f),
                modifier = Modifier
                    .padding(bottom = 20.dp)
                    .alpha(alpha)
            )
        }
    }
}
