package com.biowaymexico.ui.screens.brindador

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biowaymexico.ui.theme.BioWayColors
import com.biowaymexico.utils.CalculadoraImpactoReciclaje
import kotlinx.coroutines.launch

/**
 * Pantalla de Impacto Ambiental Detallado
 * Muestra cálculos precisos basados en CALCULADORA ECOCE 2021
 */
@Composable
fun ImpactoAmbientalScreen(
    onNavigateBack: () -> Unit
) {
    var totalKgReciclados by remember { mutableStateOf(0.0) }
    var totalCO2Calculado by remember { mutableStateOf(0.0) }
    var bioCoins by remember { mutableStateOf(0) }

    val brindadorRepository = remember { com.biowaymexico.data.BrindadorRepository() }
    val materialesRepository = remember { com.biowaymexico.data.MaterialesRepository() }
    val scope = rememberCoroutineScope()

    // Cargar datos y calcular impacto en tiempo real
    LaunchedEffect(Unit) {
        scope.launch {
            val brindadorResult = brindadorRepository.obtenerBrindador()
            val materialesResult = materialesRepository.obtenerMateriales()

            if (brindadorResult.isSuccess && materialesResult.isSuccess) {
                val brindador = brindadorResult.getOrNull()
                val materialesCatalogo = materialesResult.getOrNull() ?: emptyList()

                totalKgReciclados = brindador?.totalKgReciclados ?: 0.0
                bioCoins = brindador?.bioCoins ?: 0

                // Calcular CO2 en tiempo real usando factores actuales
                val materialesReciclados = brindador?.materialesReciclados ?: emptyMap()
                var co2Total = 0.0

                materialesReciclados.forEach { (materialId, cantidad) ->
                    val material = materialesCatalogo.find { it.id == materialId }
                    if (material != null) {
                        co2Total += cantidad * material.factorCO2
                    }
                }

                totalCO2Calculado = co2Total
            }
        }
    }

    val bioCoinsGanados = bioCoins

    Scaffold(
        containerColor = Color(0xFFFAFAFA),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Volver",
                        tint = BioWayColors.BrandDarkGreen
                    )
                }

                Text(
                    text = "Impacto Ambiental",
                    style = MaterialTheme.typography.titleLarge,
                    color = BioWayColors.BrandDarkGreen
                )

                Spacer(modifier = Modifier.width(48.dp))
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                // Resumen principal
                ResumenImpactoCard(
                    pesoTotal = totalKgReciclados,
                    co2Evitado = totalCO2Calculado,
                    arbolesEquivalentes = totalCO2Calculado / 21.77
                )
            }

            item {
                // BioCoins ganados
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = null,
                            tint = Color(0xFF70D162),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.width(20.dp))
                        Column {
                            Text(
                                text = "BioCoins Totales",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color(0xFF2E7D6C)
                            )
                            Text(
                                text = bioCoinsGanados.toString(),
                                style = MaterialTheme.typography.headlineLarge,
                                color = Color(0xFF70D162),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            item {
                // Equivalencias de CO₂
                ImpactDetailCard(
                    icon = Icons.Default.DirectionsCar,
                    titulo = "Emisiones Evitadas",
                    valorPrincipal = String.format("%.1f kg CO₂", totalCO2Calculado),
                    color = Color(0xFF66BB6A),
                    equivalencias = listOf(
                        Equivalencia(
                            "Kilómetros en auto",
                            String.format("%.0f km", totalCO2Calculado * 5.5)
                        ),
                        Equivalencia(
                            "Litros de gasolina",
                            String.format("%.1f L", totalCO2Calculado * 0.43)
                        )
                    )
                )
            }

            item {
                // Materia prima
                ImpactDetailCard(
                    icon = Icons.Default.Inventory,
                    titulo = "Materia Prima Ahorrada",
                    valorPrincipal = String.format("%.1f kg", totalKgReciclados),
                    color = Color(0xFF8D6E63),
                    equivalencias = listOf(
                        Equivalencia(
                            "Equivalente en toneladas",
                            String.format("%.3f ton", totalKgReciclados / 1000)
                        )
                    )
                )
            }

            item {
                // Separador de secciones
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Desglose por Material",
                    style = MaterialTheme.typography.titleLarge,
                    color = BioWayColors.BrandDarkGreen,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Mensaje motivacional
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = BioWayColors.BrandGreen.copy(alpha = 0.12f)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = Color(0xFF70D162),
                            modifier = Modifier.size(40.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "¡Excelente trabajo! Cada kg reciclado cuenta para un planeta más limpio.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = BioWayColors.BrandDarkGreen,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MaterialImpactoCard(
    materialImpacto: MaterialImpacto
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header del material
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = materialImpacto.nombreMaterial,
                    style = MaterialTheme.typography.titleMedium,  // Hammersmith One
                    color = BioWayColors.BrandDarkGreen
                )
                Text(
                    text = "${String.format("%.1f", materialImpacto.peso)} kg",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFF70D162)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider(
                color = Color(0xFF2E7D6C).copy(alpha = 0.1f),
                thickness = 1.dp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Stats del material en grid 2x2
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MiniImpactStat(
                        label = "CO₂ evitado",
                        valor = String.format("%.2f kg", materialImpacto.impacto.co2Evitado),
                        modifier = Modifier.weight(1f)
                    )
                    MiniImpactStat(
                        label = "Energía",
                        valor = String.format("%.1f kWh", materialImpacto.impacto.energiaAhorrada),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (materialImpacto.impacto.aguaAhorrada > 0) {
                        MiniImpactStat(
                            label = "Agua",
                            valor = String.format("%.1f L", materialImpacto.impacto.aguaAhorrada),
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Box(modifier = Modifier.weight(1f))
                    }

                    MiniImpactStat(
                        label = "Árboles equiv.",
                        valor = String.format("%.3f", materialImpacto.impacto.arbolesEquivalentes),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniImpactStat(
    label: String,
    valor: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF2E7D6C),
            fontSize = 10.sp
        )
        Text(
            text = valor,
            style = MaterialTheme.typography.bodyMedium,
            color = BioWayColors.BrandDarkGreen,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
        )
    }
}

// Modelo de datos
data class MaterialImpacto(
    val nombreMaterial: String,
    val peso: Double,
    val impacto: CalculadoraImpactoReciclaje.ImpactoAmbiental
)


@Composable
private fun ResumenImpactoCard(
    pesoTotal: Double,
    co2Evitado: Double,
    arbolesEquivalentes: Double
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Tu Impacto Total",
                style = MaterialTheme.typography.headlineMedium,  // Hammersmith One
                color = BioWayColors.BrandDarkGreen,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Peso total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = String.format("%.1f", pesoTotal),
                    style = MaterialTheme.typography.displayLarge,  // Hammersmith One
                    color = Color(0xFF70D162),
                    fontSize = 48.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "kg",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color(0xFF2E7D6C)
                )
            }

            Text(
                text = "reciclados",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF2E7D6C)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Árboles equivalentes destacado
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Park,
                    contentDescription = null,
                    tint = BioWayColors.BrandGreen,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "= ${String.format("%.1f", arbolesEquivalentes)} árboles plantados",
                    style = MaterialTheme.typography.titleMedium,
                    color = BioWayColors.BrandDarkGreen
                )
            }
        }
    }
}

@Composable
private fun StatsGrid(
    impacto: CalculadoraImpactoReciclaje.ImpactoAmbiental,
    bioCoins: Int
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MiniStatCard(
                icon = Icons.Default.MonetizationOn,
                valor = "$bioCoins",
                label = "BioCoins",
                color = Color(0xFF70D162),
                modifier = Modifier.weight(1f)
            )
            MiniStatCard(
                icon = Icons.Default.Cloud,
                valor = String.format("%.0f kg", impacto.co2Evitado),
                label = "CO₂ Evitado",
                color = Color(0xFF66BB6A),
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MiniStatCard(
                icon = Icons.Default.Bolt,
                valor = String.format("%.0f kWh", impacto.energiaAhorrada),
                label = "Energía",
                color = Color(0xFFFFB74D),
                modifier = Modifier.weight(1f)
            )
            MiniStatCard(
                icon = Icons.Default.DirectionsCar,
                valor = String.format("%.0f km", impacto.kmEnAuto),
                label = "km en Auto",
                color = Color(0xFF42A5F5),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MiniStatCard(
    icon: ImageVector,
    valor: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
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
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = valor,
                style = MaterialTheme.typography.titleMedium,  // Hammersmith One
                color = BioWayColors.BrandDarkGreen,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )

            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF2E7D6C),
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ImpactDetailCard(
    icon: ImageVector,
    titulo: String,
    valorPrincipal: String,
    color: Color,
    equivalencias: List<Equivalencia>
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
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

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = titulo,
                        style = MaterialTheme.typography.titleMedium,  // Hammersmith One
                        color = BioWayColors.BrandDarkGreen
                    )
                    Text(
                        text = valorPrincipal,
                        style = MaterialTheme.typography.headlineSmall,  // Hammersmith One
                        color = color,
                        fontSize = 24.sp
                    )
                }
            }

            if (equivalencias.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))

                HorizontalDivider(
                    color = Color(0xFF2E7D6C).copy(alpha = 0.1f),
                    thickness = 1.dp
                )

                Spacer(modifier = Modifier.height(16.dp))

                equivalencias.forEach { equiv ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = equiv.descripcion,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF2E7D6C),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = equiv.valor,
                            style = MaterialTheme.typography.bodyMedium,
                            color = BioWayColors.BrandDarkGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

data class Equivalencia(
    val descripcion: String,
    val valor: String
)
