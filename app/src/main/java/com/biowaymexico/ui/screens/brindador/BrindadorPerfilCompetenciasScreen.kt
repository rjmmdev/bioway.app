package com.biowaymexico.ui.screens.brindador

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biowaymexico.ui.theme.BioWayColors

/**
 * Pantalla de Perfil y Competencias del Brindador
 * Replicado fielmente del diseño Flutter original
 * Incluye 3 vistas: Perfil, Ranking, Logros
 */
@Composable
fun BrindadorPerfilCompetenciasScreen() {
    var vistaActual by remember { mutableStateOf("perfil") }

    val mockUser = remember {
        BioWayUser(
            nombre = "Juan Pérez",
            colonia = "Del Valle",
            municipio = "Benito Juárez",
            nivel = "Oro",
            bioCoins = 15780,
            totalKgReciclados = 245.5,
            totalCO2Evitado = 612.3,
            posicionRanking = 7,
            bioImpulso = 5,
            bioImpulsoActivo = true
        )
    }

    Scaffold(
        containerColor = Color(0xFFF8F9FA)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header
            BuildPerfilHeader(mockUser = mockUser)

            // Navigation Pills
            BuildNavigationPills(
                vistaActual = vistaActual,
                onVistaChanged = { vistaActual = it }
            )

            // Contenido según la vista seleccionada
            when (vistaActual) {
                "perfil" -> PerfilView(mockUser = mockUser)
                "ranking" -> RankingView(mockUser = mockUser)
                "logros" -> LogrosView()
            }
        }
    }
}

@Composable
private fun BuildPerfilHeader(mockUser: BioWayUser) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = mockUser.nombre,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color(0xFF999999),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${mockUser.colonia}, ${mockUser.municipio}",
                    fontSize = 14.sp,
                    color = Color(0xFF999999)
                )
            }
        }

        // Badge de nivel
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = getLevelColor(mockUser.nivel)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = getLevelIcon(mockUser.nivel),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = mockUser.nivel,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun BuildNavigationPills(
    vistaActual: String,
    onVistaChanged: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF5F5F5)
    ) {
        Row(
            modifier = Modifier.padding(4.dp)
        ) {
            NavigationPill(
                label = "Perfil",
                icon = Icons.Rounded.Person,
                isSelected = vistaActual == "perfil",
                onClick = { onVistaChanged("perfil") },
                modifier = Modifier.weight(1f)
            )
            NavigationPill(
                label = "Ranking",
                icon = Icons.Rounded.Leaderboard,
                isSelected = vistaActual == "ranking",
                onClick = { onVistaChanged("ranking") },
                modifier = Modifier.weight(1f)
            )
            NavigationPill(
                label = "Logros",
                icon = Icons.Rounded.EmojiEvents,
                isSelected = vistaActual == "logros",
                onClick = { onVistaChanged("logros") },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun NavigationPill(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color.White else Color.Transparent,
        shadowElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) BioWayColors.NavGreen else Color(0xFF999999),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (isSelected) BioWayColors.NavGreen else Color(0xFF999999)
            )
        }
    }
}

@Composable
private fun PerfilView(mockUser: BioWayUser) {
    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            StatsOverview(mockUser = mockUser)
        }

        item {
            ImpactSection(mockUser = mockUser)
        }

        item {
            MaterialsBreakdown()
        }

        item {
            AccountActions()
        }
    }
}

@Composable
private fun StatsOverview(mockUser: BioWayUser) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 10.dp
    ) {
        Box(
            modifier = Modifier.background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        BioWayColors.MediumGreen,
                        BioWayColors.AquaGreen
                    )
                )
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // BioCoins y Ranking
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    StatItem(
                        icon = Icons.Default.MonetizationOn,
                        value = mockUser.bioCoins.toString(),
                        label = "BioCoins",
                        isGold = true
                    )
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(40.dp)
                            .background(Color.Black.copy(alpha = 0.2f))
                    )
                    StatItem(
                        icon = Icons.Default.EmojiEvents,
                        value = "#${mockUser.posicionRanking}",
                        label = "Ranking",
                        isTrophy = true
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // BioImpulso
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.9f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFFFF6B35),
                                            Color(0xFFFF9558)
                                        )
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "BioImpulso Semanal",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00553F)
                            )
                            Text(
                                text = "${mockUser.bioImpulso} semanas consecutivas",
                                fontSize = 14.sp,
                                color = Color(0xFF00553F)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (mockUser.bioImpulsoActivo) Color(0xFF4CAF50) else Color(0xFFFF9800)
                        ) {
                            Text(
                                text = if (mockUser.bioImpulsoActivo) "Activo" else "Inactivo",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: ImageVector,
    value: String,
    label: String,
    isGold: Boolean = false,
    isTrophy: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = if (isGold || isTrophy) {
                Modifier.background(
                    brush = if (isGold) {
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFFFD700),
                                Color(0xFFFFA500),
                                Color(0xFFFFD700)
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFFF6B35),
                                Color(0xFFF72585),
                                Color(0xFF7209B7)
                            )
                        )
                    },
                    shape = RoundedCornerShape(8.dp)
                )
            } else Modifier,
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isGold || isTrophy) Color.White else Color(0xFF00553F),
                modifier = Modifier
                    .padding(if (isGold || isTrophy) 8.dp else 0.dp)
                    .size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF00553F)
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFF00553F)
        )
    }
}

@Composable
private fun ImpactSection(mockUser: BioWayUser) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Tu Impacto Total",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = BioWayColors.TextDark
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ImpactCard(
                    icon = Icons.Default.Recycling,
                    value = "${mockUser.totalKgReciclados} kg",
                    label = "Total reciclado",
                    color = BioWayColors.NavGreen,
                    modifier = Modifier.weight(1f)
                )
                ImpactCard(
                    icon = Icons.Default.Eco,
                    value = "${mockUser.totalCO2Evitado} kg",
                    label = "CO₂ evitado",
                    color = Color(0xFF2196F3),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF4CAF50).copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFF388E3C),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Equivalente a plantar ${(mockUser.totalCO2Evitado / 20).toInt()} árboles",
                        fontSize = 13.sp,
                        color = Color(0xFF388E3C)
                    )
                }
            }
        }
    }
}

@Composable
private fun ImpactCard(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                fontSize = 12.sp,
                color = color.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun MaterialsBreakdown() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Desglose por Material",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = BioWayColors.TextDark
            )
            Spacer(modifier = Modifier.height(16.dp))

            val materiales = listOf(
                Triple("Plástico", 85.5, Color(0xFF2196F3)),
                Triple("Vidrio", 65.3, Color(0xFF4CAF50)),
                Triple("Papel", 45.0, Color(0xFFFF9800)),
                Triple("Metal", 28.2, Color(0xFF9C27B0))
            )

            materiales.forEach { (material, kg, color) ->
                MaterialRow(material = material, kg = kg, color = color)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun MaterialRow(material: String, kg: Double, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = color.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Recycling,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = material,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "$kg kg",
                fontSize = 12.sp,
                color = Color(0xFF999999)
            )
        }
        Text(
            text = "${((kg / 245.5) * 100).toInt()}%",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun AccountActions() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Cuenta",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = BioWayColors.TextDark
            )
            Spacer(modifier = Modifier.height(16.dp))

            ActionButton(
                icon = Icons.Default.Edit,
                text = "Editar perfil",
                onClick = { }
            )
            Spacer(modifier = Modifier.height(12.dp))
            ActionButton(
                icon = Icons.Default.Settings,
                text = "Configuración",
                onClick = { }
            )
            Spacer(modifier = Modifier.height(12.dp))
            ActionButton(
                icon = Icons.Default.Logout,
                text = "Cerrar sesión",
                color = BioWayColors.Error,
                onClick = { }
            )
        }
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    text: String,
    color: Color = BioWayColors.NavGreen,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = color
            )
        }
    }
}

@Composable
private fun RankingView(mockUser: BioWayUser) {
    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Ranking Global",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = BioWayColors.TextDark
            )
        }

        items(10) { index ->
            RankingCard(
                posicion = index + 1,
                nombre = if (index == 6) mockUser.nombre else "Usuario ${index + 1}",
                puntos = 45200 - (index * 3000),
                isCurrentUser = index == 6
            )
        }
    }
}

@Composable
private fun RankingCard(
    posicion: Int,
    nombre: String,
    puntos: Int,
    isCurrentUser: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (isCurrentUser) BioWayColors.NavGreen.copy(alpha = 0.1f) else Color.White,
        shadowElevation = if (isCurrentUser) 4.dp else 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "#$posicion",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (posicion <= 3) Color(0xFFFFD700) else BioWayColors.TextDark
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = nombre,
                    fontSize = 16.sp,
                    fontWeight = if (isCurrentUser) FontWeight.Bold else FontWeight.Medium
                )
                Text(
                    text = "$puntos puntos",
                    fontSize = 14.sp,
                    color = Color(0xFF999999)
                )
            }
            if (posicion <= 3) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun LogrosView() {
    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Tus Logros",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = BioWayColors.TextDark
            )
        }

        items(5) { index ->
            LogroCard(
                titulo = "Logro ${index + 1}",
                descripcion = "Descripción del logro alcanzado",
                desbloqueado = index < 3
            )
        }
    }
}

@Composable
private fun LogroCard(
    titulo: String,
    descripcion: String,
    desbloqueado: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = if (desbloqueado) Color.White else Color(0xFFF5F5F5),
        shadowElevation = if (desbloqueado) 4.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(
                        color = if (desbloqueado)
                            BioWayColors.NavGreen.copy(alpha = 0.1f)
                        else
                            Color(0xFFE0E0E0),
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (desbloqueado) "🏆" else "🔒",
                    fontSize = 32.sp
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titulo,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (desbloqueado) BioWayColors.TextDark else Color(0xFF999999)
                )
                Text(
                    text = descripcion,
                    fontSize = 14.sp,
                    color = if (desbloqueado) Color(0xFF666666) else Color(0xFFBBBBBB)
                )
            }
        }
    }
}

// Modelo de datos
data class BioWayUser(
    val nombre: String,
    val colonia: String,
    val municipio: String,
    val nivel: String,
    val bioCoins: Int,
    val totalKgReciclados: Double,
    val totalCO2Evitado: Double,
    val posicionRanking: Int,
    val bioImpulso: Int,
    val bioImpulsoActivo: Boolean
)

private fun getLevelColor(nivel: String): Color {
    return when (nivel) {
        "Diamante" -> Color(0xFF00BCD4)
        "Oro" -> Color(0xFFFFD700)
        "Plata" -> Color(0xFFC0C0C0)
        "Bronce" -> Color(0xFFCD7F32)
        else -> BioWayColors.NavGreen
    }
}

private fun getLevelIcon(nivel: String): ImageVector {
    return when (nivel) {
        "Diamante" -> Icons.Default.Diamond
        "Oro", "Plata", "Bronce" -> Icons.Default.EmojiEvents
        else -> Icons.Default.Star
    }
}
