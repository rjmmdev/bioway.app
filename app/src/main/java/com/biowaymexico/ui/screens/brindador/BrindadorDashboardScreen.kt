package com.biowaymexico.ui.screens.brindador

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.HorizontalDivider
import com.biowaymexico.data.MaterialesRepository
import com.biowaymexico.ui.theme.BioWayColors
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Dashboard del Brindador - Pantalla principal
 * Diseño minimalista y moderno con estándar visual 2024
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BrindadorDashboardScreen(
    onNavigateToScanner: () -> Unit = {},
    onNavigateToResiduos: () -> Unit = {},
    onNavigateToClasificador: () -> Unit = {},
    onNavigateToUsuarioNormalNFC: () -> Unit = {},
    onNavigateToCelularEnBoteNFC: () -> Unit = {},
    onNavigateToUsuarioNormalNearby: () -> Unit = {},
    onNavigateToCelularEnBoteNearby: () -> Unit = {}
) {
    // Datos del usuario desde Firebase
    var userName by remember { mutableStateOf("Usuario") }
    var bioCoins by remember { mutableStateOf(0) }
    val horarios = remember { getMockHorarios() }
    val days = remember { getAyerHoyMananaYMas(horarios) }

    var selectedDayIndex by remember { mutableStateOf(1) }

    val materialesRepository = remember { MaterialesRepository() }
    val brindadorRepository = remember { com.biowaymexico.data.BrindadorRepository() }
    var materiales by remember { mutableStateOf<List<com.biowaymexico.data.MaterialReciclable>>(emptyList()) }
    val scope = rememberCoroutineScope()

    // Cargar datos del usuario y materiales
    LaunchedEffect(Unit) {
        scope.launch {
            // Cargar materiales
            val materialesResult = materialesRepository.obtenerMateriales()
            if (materialesResult.isSuccess) {
                materiales = materialesResult.getOrNull() ?: emptyList()
            }

            // Cargar datos del brindador
            val brindadorResult = brindadorRepository.obtenerBrindador()
            if (brindadorResult.isSuccess) {
                val brindador = brindadorResult.getOrNull()
                userName = brindador?.nombre ?: "Usuario"
                bioCoins = brindador?.bioCoins ?: 0
            }
        }
    }

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
        containerColor = Color(0xFFFAFAFA),  // Fondo claro minimalista
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToClasificador,  // Ahora va a Clasificador IA
                modifier = Modifier.scale(fabScaleAnimation),
                containerColor = Color(0xFF70D162),  // Verde del logo (estándar visual)
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 12.dp
                )
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,  // Icono de cámara para IA
                    contentDescription = "Clasificador IA",
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
            contentPadding = PaddingValues(bottom = 20.dp)  // Sin top padding
        ) {
            item {
                // Header
                BuildHeader(userName = userName, bioCoins = bioCoins)
                Spacer(modifier = Modifier.height(8.dp))  // Reducido: 12dp → 8dp
            }

            item {
                // Tarjeta "Reciclar ahora" - PRIMERO
                BuildRecycleNowCard(
                    horario = days[selectedDayIndex],
                    scaleAnimation = scaleAnimation,
                    onRecycleClick = onNavigateToResiduos,
                    dayLabel = when(selectedDayIndex) {
                        0 -> "AYER"
                        1 -> "HOY"
                        2 -> "MAÑANA"
                        3 -> "+2 DÍAS"
                        else -> ""
                    }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                // Calendario de reciclaje (selector) - SEGUNDO
                BuildScheduleSelector(
                    days = days,
                    selectedDayIndex = selectedDayIndex,
                    onDaySelected = { selectedDayIndex = it }
                )
                Spacer(modifier = Modifier.height(32.dp))
            }

            item {
                // Botón "Conectarse a bote BioWay" - TERCERO
                ConnectToBoteButton(
                    onNavigateToUsuarioNormalNFC = onNavigateToUsuarioNormalNFC,
                    onNavigateToUsuarioNormalNearby = onNavigateToUsuarioNormalNearby
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                // Materiales reciclables desde Firebase - ÚLTIMO
                BuildMaterialesSection(materiales = materiales)
            }
        }
    }
}

@Composable
private fun BuildHeader(userName: String, bioCoins: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 0.dp, bottom = 8.dp),  // Sin padding superior
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Buenos días,",
                style = MaterialTheme.typography.bodyMedium,  // Montserrat
                color = Color(0xFF2E7D6C)  // Verde medio del estándar
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = userName,
                style = MaterialTheme.typography.headlineMedium,  // Hammersmith One 28sp
                color = BioWayColors.BrandDarkGreen  // #007565
            )
        }

        Surface(
            shape = RoundedCornerShape(16.dp),  // Border radius del estándar
            color = Color(0xFF70D162).copy(alpha = 0.12f)  // Verde del logo suave
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.MonetizationOn,
                    contentDescription = null,
                    tint = Color(0xFF70D162),  // Verde del logo
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = bioCoins.toString(),
                    style = MaterialTheme.typography.titleMedium,  // Hammersmith One
                    color = Color(0xFF70D162)
                )
            }
        }
    }
}

@Composable
private fun BuildRecycleNowCard(
    horario: Horario?,
    scaleAnimation: Float,
    onRecycleClick: () -> Unit,
    dayLabel: String
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
                        // Degradado con colores del estándar visual 2024
                        Brush.linearGradient(
                            colors = listOf(
                                BioWayColors.BrandGreen,      // #75ee8a
                                BioWayColors.BrandTurquoise,  // #b3fcd4
                                BioWayColors.BrandBlue        // #00dfff
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
                        color = Color.White.copy(alpha = 0.25f)
                    ) {
                        Text(
                            text = "$dayLabel - ${horario.dia}",  // Usa dayLabel dinámico
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,  // Montserrat Medium
                            color = BioWayColors.BrandDarkGreen,
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
                            tint = if (canRecycle) BioWayColors.NavGreen else Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (canRecycle) "Reciclar ahora" else "Sin recolección hoy",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (canRecycle) BioWayColors.NavGreen else Color.White
                        )
                    }
                }

                if (!canRecycle) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Revisa el calendario para próximas recolecciones",
                        fontSize = 14.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun BuildScheduleSelector(
    days: List<Horario?>,
    selectedDayIndex: Int,
    onDaySelected: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Selecciona el día",
            style = MaterialTheme.typography.titleMedium,  // Hammersmith One
            color = BioWayColors.BrandDarkGreen
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Selector de días - Solo muestra nombre del día
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf(0, 1, 2, 3).forEach { index ->
                val horario = days[index]
                val isSelected = index == selectedDayIndex
                val isToday = index == 1

                // Determinar el label (solo HOY se queda como está)
                val label = if (isToday) {
                    "HOY"
                } else {
                    horario?.dia?.take(3)?.uppercase() ?: "---"
                }

                Button(
                    onClick = {
                        onDaySelected(index)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(68.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when {
                            isSelected && isToday -> BioWayColors.BrandGreen  // HOY seleccionado
                            isToday -> Color.White  // HOY no seleccionado - fondo blanco con borde
                            isSelected -> Color(0xFF70D162)  // Otro día seleccionado
                            else -> Color.White
                        },
                        contentColor = when {
                            isSelected && isToday -> Color.White  // HOY seleccionado texto blanco (PRIMERO)
                            isSelected -> Color.White  // Otros días seleccionados texto blanco
                            isToday -> BioWayColors.BrandGreen  // HOY no seleccionado texto verde brillante
                            else -> Color(0xFF2E7D6C)  // Otros días no seleccionados
                        }
                    ),
                    border = if (isToday && !isSelected) {
                        androidx.compose.foundation.BorderStroke(
                            width = 2.dp,
                            color = BioWayColors.BrandGreen  // Borde verde para HOY
                        )
                    } else null,
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = if (isSelected) 4.dp else 2.dp,
                        pressedElevation = 6.dp
                    ),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (isToday) {
                            // HOY: Muestra "HOY" + día de la semana
                            Text(
                                text = "HOY",
                                style = MaterialTheme.typography.titleSmall,  // Hammersmith One
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                            if (horario != null) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = horario.dia.take(3).uppercase(),  // LUN, MAR, MIÉ...
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            // Otros días: Solo el día de la semana
                            Text(
                                text = label,  // DOM, MAR, MIÉ...
                                style = MaterialTheme.typography.titleSmall,  // Hammersmith One
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

// FUNCIÓN LEGACY - Mantener comentada por si se necesita
/*
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
*/

// FUNCIONES LEGACY COMENTADAS - Ya no se usan (DayCard, ScheduleDetails)
/*
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
*/

@Composable
private fun ConnectToBoteButton(
    onNavigateToUsuarioNormalNFC: () -> Unit,
    onNavigateToUsuarioNormalNearby: () -> Unit
) {
    var showOptions by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        // Botón principal "Conectarse a bote BioWay"
        Surface(
            onClick = { showOptions = !showOptions },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icono
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = BioWayColors.BrandGreen.copy(alpha = 0.15f)
                ) {
                    Box(
                        modifier = Modifier.size(60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = null,
                            tint = BioWayColors.BrandGreen,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(20.dp))

                // Texto
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Conectarse a bote BioWay",
                        style = MaterialTheme.typography.titleMedium,
                        color = BioWayColors.BrandDarkGreen
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Deposita tus residuos en el bote inteligente",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF2E7D6C),
                        lineHeight = 18.sp
                    )
                }

                // Icono expandir/contraer
                Icon(
                    imageVector = if (showOptions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (showOptions) "Contraer" else "Expandir",
                    tint = Color(0xFF2E7D6C).copy(alpha = 0.5f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Opciones expandibles (NFC y Nearby)
        androidx.compose.animation.AnimatedVisibility(
            visible = showOptions,
            enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Opción NFC (corto alcance)
                ConnectionOptionCard(
                    icon = Icons.Default.Nfc,
                    title = "NFC (Contacto directo)",
                    description = "Acerca tu dispositivo al bote (6-10cm)",
                    onClick = onNavigateToUsuarioNormalNFC
                )

                // Opción Nearby (largo alcance)
                ConnectionOptionCard(
                    icon = Icons.Default.Wifi,
                    title = "Nearby (Sin contacto)",
                    description = "Conecta desde 1-10 metros de distancia",
                    onClick = onNavigateToUsuarioNormalNearby
                )
            }
        }
    }
}

@Composable
private fun ConnectionOptionCard(
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
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = BioWayColors.BrandGreen,
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = BioWayColors.BrandDarkGreen,
                    fontWeight = FontWeight.SemiBold
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
                tint = Color(0xFF2E7D6C).copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// SECCIÓN ELIMINADA - Ya no se usa BuildAIToolsSection
/*
@Composable
private fun BuildAIToolsSection(
    onNavigateToClasificador: () -> Unit,
    onNavigateToUsuarioNormalNFC: () -> Unit,
    onNavigateToCelularEnBoteNFC: () -> Unit,
    onNavigateToUsuarioNormalNearby: () -> Unit,
    onNavigateToCelularEnBoteNearby: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),  // Padding del estándar
        verticalArrangement = Arrangement.spacedBy(16.dp)  // Más espaciado
    ) {
        // Clasificador IA
        ToolCard(
            icon = Icons.Default.PhotoCamera,
            title = "Clasificador IA",
            description = "Identifica residuos con tu cámara usando Inteligencia Artificial",
            badge = "NUEVO",
            detail = "86% de precisión",
            detailIcon = Icons.Default.AutoAwesome,
            gradientColors = listOf(
                BioWayColors.PrimaryGreen.copy(alpha = 0.1f),
                BioWayColors.Turquoise.copy(alpha = 0.1f)
            ),
            onClick = onNavigateToClasificador
        )

        // Usuario Normal NFC
        ToolCard(
            icon = Icons.Default.Nfc,
            title = "Usuario Normal",
            description = "Genera un ID único para compartir mediante NFC",
            badge = "NFC",
            detail = "Modo emisor",
            detailIcon = Icons.Default.Send,
            gradientColors = listOf(
                Color(0xFF4A90E2).copy(alpha = 0.1f),
                Color(0xFF5AB9EA).copy(alpha = 0.1f)
            ),
            onClick = onNavigateToUsuarioNormalNFC
        )

        // Celular en Bote NFC
        ToolCard(
            icon = Icons.Default.PhoneAndroid,
            title = "Celular en Bote",
            description = "Detecta y reconoce dispositivos cercanos mediante NFC",
            badge = "NFC",
            detail = "Modo receptor",
            detailIcon = Icons.Default.Sensors,
            gradientColors = listOf(
                Color(0xFF9B59B6).copy(alpha = 0.1f),
                Color(0xFFAB47BC).copy(alpha = 0.1f)
            ),
            onClick = onNavigateToCelularEnBoteNFC
        )

        // Usuario Normal Nearby (Mayor alcance)
        ToolCard(
            icon = Icons.Default.Wifi,
            title = "Usuario Normal (Nearby)",
            description = "Emite señal para compartir ID con mayor alcance (1-10m)",
            badge = "NEARBY",
            detail = "Alcance: 1-10m",
            detailIcon = Icons.Default.SignalCellularAlt,
            gradientColors = listOf(
                Color(0xFF00BCD4).copy(alpha = 0.1f),
                Color(0xFF00ACC1).copy(alpha = 0.1f)
            ),
            onClick = onNavigateToUsuarioNormalNearby
        )

        // Celular en Bote Nearby (Mayor alcance)
        ToolCard(
            icon = Icons.Default.Radar,
            title = "Celular en Bote (Nearby)",
            description = "Detecta dispositivos cercanos con mayor alcance (1-10m)",
            badge = "NEARBY",
            detail = "Alcance: 1-10m",
            detailIcon = Icons.Default.SignalCellularAlt,
            gradientColors = listOf(
                Color(0xFFFF6F00).copy(alpha = 0.1f),
                Color(0xFFFF8F00).copy(alpha = 0.1f)
            ),
            onClick = onNavigateToCelularEnBoteNearby
        )
    }
}
*/

// FUNCIÓN ELIMINADA - Ya no se usa ToolCard (fue reemplazada por nueva versión)
/*
@Composable
private fun ToolCard(
    icon: ImageVector,
    title: String,
    description: String,
    badge: String,
    detail: String,
    detailIcon: ImageVector,
    gradientColors: List<Color>,  // Mantenemos para compatibilidad pero no se usa igual
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),  // Border radius del estándar
        color = Color.White,
        shadowElevation = 4.dp,  // Sombra suave minimalista
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),  // Padding del estándar
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono con fondo sólido del color del estándar
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF70D162).copy(alpha = 0.12f)  // Verde del logo suave
            ) {
                Box(
                    modifier = Modifier.size(60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color(0xFF70D162),  // Verde del logo
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(20.dp))

            // Texto
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,  // Hammersmith One
                        color = BioWayColors.BrandDarkGreen  // #007565
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = BioWayColors.BrandGreen.copy(alpha = 0.15f)  // Verde principal suave
                    ) {
                        Text(
                            text = badge,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,  // Montserrat Medium
                            color = Color(0xFF70D162)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,  // Montserrat
                    color = Color(0xFF2E7D6C),  // Verde medio
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = detailIcon,
                        contentDescription = null,
                        tint = BioWayColors.BrandGreen,  // Verde principal
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodySmall,  // Montserrat
                        color = Color(0xFF2E7D6C)
                    )
                }
            }

            // Flecha
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Ir",
                tint = Color(0xFF2E7D6C).copy(alpha = 0.5f),  // Sutil
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
*/

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
            dia = "Martes",
            numDia = 2,
            matinfo = "Residuos Orgánicos",
            horario = "7:00 AM - 12:00 PM",
            cantidadMinima = "2 kg",
            qnr = "Plástico, Vidrio"
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
            dia = "Jueves",
            numDia = 4,
            matinfo = "Plástico PET",
            horario = "8:00 AM - 1:00 PM",
            cantidadMinima = "4 kg",
            qnr = "Unicel, Papel encerado"
        ),
        Horario(
            dia = "Viernes",
            numDia = 5,
            matinfo = "Vidrio y Metal",
            horario = "7:00 AM - 1:00 PM",
            cantidadMinima = "4 kg",
            qnr = "Papel, Electrónicos"
        ),
        Horario(
            dia = "Sábado",
            numDia = 6,
            matinfo = "Reciclaje Mixto",
            horario = "9:00 AM - 2:00 PM",
            cantidadMinima = "6 kg",
            qnr = "Vidrio roto, Baterías"
        ),
        Horario(
            dia = "Domingo",
            numDia = 7,
            matinfo = "Electrónicos",
            horario = "10:00 AM - 1:00 PM",
            cantidadMinima = "3 kg",
            qnr = "Orgánicos, Papel mojado"
        )
    )
}

private fun getAyerHoyMananaYMas(horarios: List<Horario>): List<Horario?> {
    val today = LocalDate.now()
    val todayNum = today.dayOfWeek.value
    val yesterdayNum = if (todayNum == 1) 7 else todayNum - 1
    val tomorrowNum = if (todayNum == 7) 1 else todayNum + 1
    val dayAfterTomorrowNum = when(tomorrowNum) {
        7 -> 1
        else -> tomorrowNum + 1
    }

    return listOf(
        horarios.find { it.numDia == yesterdayNum },
        horarios.find { it.numDia == todayNum },
        horarios.find { it.numDia == tomorrowNum },
        horarios.find { it.numDia == dayAfterTomorrowNum }
    )
}

@Composable
private fun BuildMaterialesSection(materiales: List<com.biowaymexico.data.MaterialReciclable>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Text(
            text = "Materiales Reciclables",
            style = MaterialTheme.typography.titleLarge,
            color = BioWayColors.BrandDarkGreen
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (materiales.isEmpty()) {
            Text(
                text = "Cargando materiales...",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF2E7D6C)
            )
        } else {
            materiales.forEach { material ->
                MaterialCard(material = material)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun MaterialCard(material: com.biowaymexico.data.MaterialReciclable) {
    var showDetails by remember { mutableStateOf(false) }

    val colorHex = material.color.removePrefix("#")
    val materialColor = try {
        Color(android.graphics.Color.parseColor("#$colorHex"))
    } catch (e: Exception) {
        BioWayColors.BrandGreen
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDetails = !showDetails },
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // SVG Icon usando color de fondo
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = materialColor.copy(alpha = 0.15f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Recycling,
                        contentDescription = null,
                        tint = materialColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = material.nombre,
                        style = MaterialTheme.typography.titleSmall,
                        color = BioWayColors.BrandDarkGreen,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = material.info,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF2E7D6C),
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Mínimo: ${material.cantMin} ${material.unidad}",
                        style = MaterialTheme.typography.bodySmall,
                        color = materialColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Icon(
                    imageVector = if (showDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (showDetails) "Ocultar" else "Ver más",
                    tint = Color(0xFF2E7D6C).copy(alpha = 0.5f)
                )
            }

            // Información detallada expandible
            if (showDetails && material.detailedInfo != null) {
                Spacer(modifier = Modifier.height(16.dp))
                androidx.compose.material3.HorizontalDivider(
                    color = materialColor.copy(alpha = 0.2f),
                    thickness = 1.dp
                )
                Spacer(modifier = Modifier.height(16.dp))

                material.detailedInfo.siReciclables.takeIf { it.isNotEmpty() }?.let { lista ->
                    DetailSection(
                        title = "✅ Sí se recicla:",
                        items = lista,
                        color = Color(0xFF4CAF50)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                material.detailedInfo.noReciclables.takeIf { it.isNotEmpty() }?.let { lista ->
                    DetailSection(
                        title = "❌ No se recicla:",
                        items = lista,
                        color = Color(0xFFE53935)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                material.detailedInfo.consejos.takeIf { it.isNotEmpty() }?.let { lista ->
                    DetailSection(
                        title = "💡 Consejos:",
                        items = lista,
                        color = Color(0xFF2196F3)
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailSection(
    title: String,
    items: List<String>,
    color: Color
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        items.forEach { item ->
            Row(
                modifier = Modifier.padding(bottom = 4.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodySmall,
                    color = color,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = item,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF2E7D6C),
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

// Función legacy - mantener por si se necesita
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
