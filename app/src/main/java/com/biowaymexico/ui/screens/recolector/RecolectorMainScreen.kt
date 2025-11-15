package com.biowaymexico.ui.screens.recolector

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
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
 * Pantalla principal del módulo Recolector
 * Con 2 pantallas: Mapa OSM optimizado y Perfil
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun RecolectorMainScreen() {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = Color(0xFFF8F9FA),
        bottomBar = {
            RecolectorBottomNavigationBar(
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
                0 -> RecolectorMapaScreenSimple()  // Mapa OSM optimizado
                1 -> RecolectorPerfilScreen()
            }
        }
    }
}

/**
 * Barra de navegación inferior del Recolector
 * Diseño exacto del Flutter original
 */
@Composable
private fun RecolectorBottomNavigationBar(
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
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            RecolectorBottomNavItem(
                icon = Icons.Default.Map,
                label = "Mapa",
                isSelected = currentIndex == 0,
                onClick = { onTap(0) },
                modifier = Modifier.weight(1f)
            )
            RecolectorBottomNavItem(
                icon = Icons.Default.Person,
                label = "Perfil",
                isSelected = currentIndex == 1,
                onClick = { onTap(1) },
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
private fun RecolectorBottomNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconColor = if (isSelected) BioWayColors.NavGreen else Color(0xFFBBBBBB)
    val backgroundColor = if (isSelected) {
        BioWayColors.NavGreen.copy(alpha = 0.1f)
    } else {
        Color.Transparent
    }
    val iconSize = if (isSelected) 28.dp else 24.dp

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
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(iconSize)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = iconColor
            )
        }
    }
}
