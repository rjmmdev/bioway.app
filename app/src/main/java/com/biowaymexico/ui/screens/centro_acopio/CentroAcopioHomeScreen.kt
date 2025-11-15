package com.biowaymexico.ui.screens.centro_acopio

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.navigation.NavHostController
import com.biowaymexico.ui.theme.BioWayColors

/**
 * Pantalla principal del Centro de Acopio
 * Replicado fielmente del diseño Flutter original
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CentroAcopioHomeScreen(navController: NavHostController) {
    Scaffold(
        containerColor = Color(0xFFF5F5F5),
        topBar = {
            CentroAcopioTopBar()
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* TODO: Escanear QR */ },
                containerColor = BioWayColors.NavGreen
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Escanear QR",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Escanear QR",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { BuildInfoCard() }
            item { BuildQuickStats() }
            item {
                Text(
                    text = "Operaciones",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = BioWayColors.DarkGreen
                )
            }
            item { BuildMenuGrid() }
            item {
                Text(
                    text = "Últimas Recepciones",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = BioWayColors.DarkGreen
                )
            }
            item { BuildRecentReceptions() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CentroAcopioTopBar() {
    MediumTopAppBar(
        title = {
            Text(
                text = "Centro de Acopio",
                color = Color(0xFF00553F),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        colors = TopAppBarDefaults.mediumTopAppBarColors(
            containerColor = Color.Transparent
        ),
        modifier = Modifier.background(
            brush = Brush.linearGradient(
                colors = listOf(
                    BioWayColors.MediumGreen,
                    BioWayColors.AquaGreen
                )
            )
        )
    )
}

@Composable
private fun BuildInfoCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Centro de Acopio BioWay",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = BioWayColors.DarkGreen
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color(0xFF666666),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Av. Tecnológico #100, Aguascalientes",
                    fontSize = 14.sp,
                    color = Color(0xFF666666)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoItem("Saldo Prepago", "$15,420.50")
                InfoItem("Reputación", "4.8 ⭐")
            }
        }
    }
}

@Composable
private fun InfoItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFF999999)
        )
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = BioWayColors.NavGreen
        )
    }
}

@Composable
private fun BuildQuickStats() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            icon = Icons.Default.Today,
            value = "450.5 kg",
            label = "Recibidos Hoy",
            color = BioWayColors.SuccessGreen,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            icon = Icons.Default.CalendarMonth,
            value = "12,340 kg",
            label = "Este Mes",
            color = BioWayColors.InfoBlue,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = BioWayColors.DarkGreen
            )
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color(0xFF999999)
            )
        }
    }
}

@Composable
private fun BuildMenuGrid() {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MenuCard(
                icon = Icons.Default.AddCircle,
                title = "Recepción",
                color = BioWayColors.SuccessGreen,
                onClick = { /* TODO */ },
                modifier = Modifier.weight(1f)
            )
            MenuCard(
                icon = Icons.Default.Inventory,
                title = "Inventario",
                color = BioWayColors.InfoBlue,
                onClick = { /* TODO */ },
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MenuCard(
                icon = Icons.Default.Assessment,
                title = "Reportes",
                color = BioWayColors.OrangeAccent,
                onClick = { /* TODO */ },
                modifier = Modifier.weight(1f)
            )
            MenuCard(
                icon = Icons.Default.AccountBalance,
                title = "Prepago",
                color = BioWayColors.PurpleAccent,
                onClick = { /* TODO */ },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MenuCard(
    icon: ImageVector,
    title: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = BioWayColors.DarkGreen
            )
        }
    }
}

@Composable
private fun BuildRecentReceptions() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            ReceptionItem("María García", "PET Tipo 1", 15.5, "$77.50", "10:30 AM")
            Divider(modifier = Modifier.padding(vertical = 12.dp))
            ReceptionItem("Juan Pérez", "Cartón", 25.0, "$50.00", "09:15 AM")
            Divider(modifier = Modifier.padding(vertical = 12.dp))
            ReceptionItem("Ana López", "Vidrio", 8.3, "$24.90", "08:45 AM")
        }
    }
}

@Composable
private fun ReceptionItem(
    name: String,
    material: String,
    kg: Double,
    amount: String,
    time: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = BioWayColors.DarkGreen
            )
            Text(
                text = "$material • $kg kg",
                fontSize = 14.sp,
                color = Color(0xFF666666)
            )
            Text(
                text = time,
                fontSize = 12.sp,
                color = Color(0xFF999999)
            )
        }
        Text(
            text = amount,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = BioWayColors.SuccessGreen
        )
    }
}
