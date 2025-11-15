package com.biowaymexico.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biowaymexico.ui.theme.BioWayColors

/**
 * Item de navegación para el BottomNavigationBar
 */
data class BioWayBottomNavItem(
    val icon: ImageVector,
    val label: String,
    val index: Int
)

/**
 * Barra de navegación inferior personalizada de BioWay
 * Migrado desde el proyecto Flutter original
 */
@Composable
fun BioWayBottomNavigationBar(
    currentIndex: Int,
    items: List<BioWayBottomNavItem>,
    onTap: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                spotColor = BioWayColors.ShadowColor,
                clip = false
            )
            .background(Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(65.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                BioWayBottomNavItem(
                    item = item,
                    isSelected = currentIndex == item.index,
                    onClick = { onTap(item.index) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun BioWayBottomNavItem(
    item: BioWayBottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = if (isSelected) BioWayColors.PrimaryGreen else BioWayColors.TextGrey
    val backgroundColor = if (isSelected) {
        BioWayColors.PrimaryGreen.copy(alpha = 0.1f)
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
                indication = ripple(color = BioWayColors.PrimaryGreen),
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
                imageVector = item.icon,
                contentDescription = item.label,
                tint = color,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = item.label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = color
            )
        }
    }
}
