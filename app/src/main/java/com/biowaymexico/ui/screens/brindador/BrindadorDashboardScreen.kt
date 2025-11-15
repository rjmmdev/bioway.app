package com.biowaymexico.ui.screens.brindador

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import com.biowaymexico.ui.theme.BioWayColors
import java.time.LocalDate

/**
 * Dashboard del Brindador - Pantalla principal
 * Replicado fielmente del diseño Flutter original
 */
@Composable
fun BrindadorDashboardScreen(
    onNavigateToScanner: () -> Unit = {},
    onNavigateToResiduos: () -> Unit = {},
    onNavigateToClasificador: () -> Unit = {}
) {
    var selectedIndex by remember { mutableStateOf(1) } // HOY por defecto

    // Datos mock
    val userName = "Juan Pérez"
    val bioCoins = 1250
    val horarios = remember { getMockHorarios() }
    val days = remember { getAyerHoyManana(horarios) }

    // Animaciones
    val infiniteTransition = rememberInfiniteTransition(label = "scale")
    val scaleAnimation by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val fabInfiniteTransition = rememberInfiniteTransition(label = "fab")
    val fabScaleAnimation by fabInfiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fabScale"
    )

    Scaffold(
        containerColor = Color(0xFFF8F9FA),
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToScanner,
                modifier = Modifier.scale(fabScaleAnimation),
                containerColor = BioWayColors.PrimaryGreen,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 12.dp
                )
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "Escanear",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            item {
                // Header
                BuildHeader(userName = userName, bioCoins = bioCoins)
                Spacer(modifier = Modifier.height(20.dp))
            }

            item {
                // Sección de Herramientas IA
                BuildAIToolsSection(
                    onNavigateToClasificador = onNavigateToClasificador
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            item {
                // Tarjeta "Reciclar ahora"
                BuildRecycleNowCard(
                    horario = days[selectedIndex],
                    scaleAnimation = scaleAnimation,
                    onRecycleClick = onNavigateToResiduos
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                // Calendario de reciclaje
                BuildScheduleSection(
                    days = days,
                    selectedIndex = selectedIndex,
                    onIndexChanged = { selectedIndex = it }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                // Tips de reciclaje
                BuildTipsSection()
            }
        }
    }
}

@Composable
private fun BuildHeader(userName: String, bioCoins: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Buenos días,",
                fontSize = 14.sp,
                color = Color(0xFF999999)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = userName,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A)
            )
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = BioWayColors.NavGreen.copy(alpha = 0.1f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.MonetizationOn,
                    contentDescription = null,
                    tint = BioWayColors.NavGreen,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = bioCoins.toString(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = BioWayColors.NavGreen
                )
            }
        }
    }
}

@Composable
private fun BuildRecycleNowCard(
    horario: Horario?,
    scaleAnimation: Float,
    onRecycleClick: () -> Unit
) {
    val canRecycle = horario != null

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 8.dp
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = if (canRecycle) {
                        Brush.linearGradient(
                            colors = listOf(
                                BioWayColors.MediumGreen,
                                BioWayColors.AquaGreen
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFCCCCCC).copy(alpha = 0.3f),
                                Color(0xFF999999).copy(alpha = 0.3f)
                            )
                        )
                    }
                )
                .clickable(enabled = canRecycle) { onRecycleClick() }
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (horario != null) {
                    // Badge con el día
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "HOY - ${horario.dia}",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF00553F),
                            letterSpacing = 0.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Material info
                    Text(
                        text = horario.matinfo,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00553F),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Horario y cantidad mínima
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = Color(0xFF00553F),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = horario.horario,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF00553F)
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Icon(
                            imageVector = Icons.Default.Scale,
                            contentDescription = null,
                            tint = Color(0xFF00553F),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Mín: ${horario.cantidadMinima}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF00553F)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Botón "Reciclar ahora"
                Surface(
                    modifier = Modifier.scale(if (canRecycle) scaleAnimation else 1.0f),
                    shape = RoundedCornerShape(35.dp),
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 32.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Recycling,
                            contentDescription = null,
                            tint = if (canRecycle) BioWayColors.NavGreen else Color.Gray,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (canRecycle) "Reciclar ahora" else "Sin recolección hoy",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (canRecycle) BioWayColors.NavGreen else Color.Gray
                        )
                    }
                }

                if (!canRecycle) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Revisa el calendario para próximas recolecciones",
                        fontSize = 14.sp,
                        color = Color(0xFF00553F),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun BuildScheduleSection(
    days: List<Horario?>,
    selectedIndex: Int,
    onIndexChanged: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Calendario de Reciclaje",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = BioWayColors.TextDark
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Desliza para ver los próximos días",
            fontSize = 13.sp,
            color = Color(0xFF999999)
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Tarjetas de días
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val labels = listOf("AYER", "HOY", "MAÑANA")
            labels.forEachIndexed { index, label ->
                DayCard(
                    horario = days[index],
                    label = label,
                    isSelected = index == selectedIndex,
                    onClick = { onIndexChanged(index) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Detalles del horario seleccionado
        if (days[selectedIndex] != null) {
            Spacer(modifier = Modifier.height(20.dp))
            ScheduleDetails(
                horario = days[selectedIndex]!!,
                label = when(selectedIndex) {
                    0 -> "AYER"
                    1 -> "HOY"
                    2 -> "MAÑANA"
                    else -> ""
                }
            )
        }
    }
}

@Composable
private fun DayCard(
    horario: Horario?,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(85.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) Color.Transparent else Color.White,
        shadowElevation = if (isSelected) 12.dp else 6.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = if (isSelected) {
                        Brush.linearGradient(
                            colors = listOf(
                                BioWayColors.MediumGreen,
                                BioWayColors.AquaGreen
                            )
                        )
                    } else {
                        Brush.linearGradient(listOf(Color.White, Color.White))
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = label,
                    fontSize = if (isSelected) 15.sp else 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color(0xFF00553F) else BioWayColors.TextDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (horario != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected)
                            Color.White.copy(alpha = 0.2f)
                        else
                            BioWayColors.NavGreen.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = horario.dia.take(3).uppercase(),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) Color(0xFF00553F) else BioWayColors.NavGreen
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleDetails(horario: Horario, label: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = BioWayColors.NavGreen.copy(alpha = 0.1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Recycling,
                        contentDescription = null,
                        tint = BioWayColors.NavGreen,
                        modifier = Modifier
                            .padding(12.dp)
                            .size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = horario.matinfo,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$label - ${horario.dia}",
                        fontSize = 14.sp,
                        color = Color(0xFF999999)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            DetailRow(Icons.Default.Schedule, "Horario", horario.horario)
            Spacer(modifier = Modifier.height(12.dp))
            DetailRow(Icons.Default.Scale, "Cantidad mínima", horario.cantidadMinima)
            Spacer(modifier = Modifier.height(12.dp))
            DetailRow(Icons.Default.NotInterested, "No se recibe", horario.qnr)

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedButton(
                onClick = { /* Más información */ },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = BioWayColors.NavGreen
                ),
                border = BorderStroke(1.dp, BioWayColors.NavGreen)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Más información",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF999999),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color(0xFF999999)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1A1A1A)
            )
        }
    }
}

@Composable
private fun BuildAIToolsSection(
    onNavigateToClasificador: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToClasificador() },
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 6.dp
        ) {
            Box {
                // Fondo con gradiente
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    BioWayColors.PrimaryGreen.copy(alpha = 0.1f),
                                    BioWayColors.Turquoise.copy(alpha = 0.1f)
                                )
                            )
                        )
                )

                // Contenido
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icono con fondo
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = BioWayColors.PrimaryGreen.copy(alpha = 0.2f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            BioWayColors.PrimaryGreen.copy(alpha = 0.3f),
                                            BioWayColors.Turquoise.copy(alpha = 0.2f)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = null,
                                tint = BioWayColors.DarkGreen,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Texto
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Clasificador IA",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = BioWayColors.DarkGreen
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = BioWayColors.Turquoise.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "NUEVO",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BioWayColors.DarkGreen
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Identifica residuos con tu cámara usando Inteligencia Artificial",
                            fontSize = 13.sp,
                            color = Color(0xFF666666),
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = BioWayColors.Turquoise,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "86% de precisión",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = BioWayColors.DarkGreen
                            )
                        }
                    }

                    // Flecha
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Ir",
                        tint = BioWayColors.DarkGreen,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BuildTipsSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = null,
                tint = Color(0xFFFFA000),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Tips de Reciclaje",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        TipCard(
            icon = Icons.Default.CleaningServices,
            title = "Limpia tus residuos",
            description = "Asegúrate de que estén limpios y secos",
            color = Color(0xFF2196F3)
        )
        Spacer(modifier = Modifier.height(12.dp))
        TipCard(
            icon = Icons.Default.Compress,
            title = "Compacta el material",
            description = "Aplasta botellas y latas para ahorrar espacio",
            color = Color(0xFF4CAF50)
        )
        Spacer(modifier = Modifier.height(12.dp))
        TipCard(
            icon = Icons.Default.Category,
            title = "Separa correctamente",
            description = "Clasifica por tipo de material",
            color = Color(0xFF9C27B0)
        )
    }
}

@Composable
private fun TipCard(
    icon: ImageVector,
    title: String,
    description: String,
    color: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = color.copy(alpha = 0.1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier
                        .padding(10.dp)
                        .size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1A1A1A)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = Color(0xFF999999)
                )
            }
        }
    }
}

// Modelos de datos
data class Horario(
    val dia: String,
    val numDia: Int,
    val matinfo: String,
    val horario: String,
    val cantidadMinima: String,
    val qnr: String
)

private fun getMockHorarios(): List<Horario> {
    return listOf(
        Horario(
            dia = "Lunes",
            numDia = 1,
            matinfo = "Plástico y Latas",
            horario = "8:00 AM - 2:00 PM",
            cantidadMinima = "5 kg",
            qnr = "Vidrio, Papel mojado"
        ),
        Horario(
            dia = "Miércoles",
            numDia = 3,
            matinfo = "Papel y Cartón",
            horario = "9:00 AM - 3:00 PM",
            cantidadMinima = "3 kg",
            qnr = "Plástico sucio, Orgánicos"
        ),
        Horario(
            dia = "Viernes",
            numDia = 5,
            matinfo = "Vidrio y Metal",
            horario = "7:00 AM - 1:00 PM",
            cantidadMinima = "4 kg",
            qnr = "Papel, Electrónicos"
        )
    )
}

private fun getAyerHoyManana(horarios: List<Horario>): List<Horario?> {
    val today = LocalDate.now()
    val todayNum = today.dayOfWeek.value
    val yesterdayNum = if (todayNum == 1) 7 else todayNum - 1
    val tomorrowNum = if (todayNum == 7) 1 else todayNum + 1

    return listOf(
        horarios.find { it.numDia == yesterdayNum },
        horarios.find { it.numDia == todayNum },
        horarios.find { it.numDia == tomorrowNum }
    )
}
