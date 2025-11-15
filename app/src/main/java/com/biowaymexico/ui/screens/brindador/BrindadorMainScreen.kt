package com.biowaymexico.ui.screens.brindador

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biowaymexico.ui.theme.BioWayColors
import kotlinx.coroutines.launch

/**
 * Pantalla principal del módulo Brindador (Ciudadano)
 * Replicado fielmente del diseño Flutter original
 * Usa HorizontalPager (equivalente a PageView en Flutter)
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun BrindadorMainScreen(
    onNavigateToClasificador: () -> Unit = {}
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = Color(0xFFF8F9FA),
        bottomBar = {
            BrindadorBottomNavigationBar(
                currentIndex = pagerState.currentPage,
                onTap = { index ->
                    scope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                }
            )
        }
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.padding(paddingValues),
            userScrollEnabled = false // Deshabilitar swipe manual como en Flutter
        ) { page ->
            when (page) {
                0 -> BrindadorDashboardScreen(
                    onNavigateToClasificador = onNavigateToClasificador
                )
                1 -> BrindadorComercioLocalScreen()
                2 -> BrindadorPerfilCompetenciasScreen()
            }
        }
    }
}

/**
 * Barra de navegación inferior del Brindador
 * Diseño exacto del Flutter original
 */
@Composable
private fun BrindadorBottomNavigationBar(
    currentIndex: Int,
    onTap: (Int) -> Unit
) {
    Surface(
        shadowElevation = 10.dp,
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(65.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            BrindadorBottomNavItem(
                icon = Icons.Rounded.Home,
                label = "Inicio",
                isSelected = currentIndex == 0,
                onClick = { onTap(0) },
                modifier = Modifier.weight(1f)
            )
            BrindadorBottomNavItem(
                icon = Icons.Rounded.Store,
                label = "Comercio",
                isSelected = currentIndex == 1,
                onClick = { onTap(1) },
                modifier = Modifier.weight(1f)
            )
            BrindadorBottomNavItem(
                icon = Icons.Rounded.Person,
                label = "Perfil",
                isSelected = currentIndex == 2,
                onClick = { onTap(2) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Item individual de la barra de navegación
 * Replicado fielmente del Flutter original
 */
@Composable
private fun BrindadorBottomNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = if (isSelected) BioWayColors.NavGreen else Color(0xFF666666)
    val backgroundColor = if (isSelected) {
        BioWayColors.NavGreen.copy(alpha = 0.1f)
    } else {
        Color.Transparent
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = BioWayColors.NavGreen),
                onClick = onClick
            )
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = color
            )
        }
    }
}
