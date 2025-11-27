package com.biowaymexico.ui.screens.recolector

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biowaymexico.ui.components.BioWayCard
import com.biowaymexico.ui.components.BioWayStatCard
import com.biowaymexico.ui.theme.BioWayColors

@Composable
fun RecolectorHistorialScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BioWayColors.BackgroundGrey),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Historial de Recolecciones",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BioWayStatCard(
                    value = "48",
                    label = "Recolecciones",
                    modifier = Modifier.weight(1f)
                )
                BioWayStatCard(
                    value = "235 kg",
                    label = "Total",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        items(10) { index ->
            BioWayCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = BioWayColors.Success,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Recolección #${10 - index}",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Completada • ${index + 5} kg",
                            fontSize = 14.sp,
                            color = BioWayColors.TextGrey
                        )
                        Text(
                            "Hace ${index + 1} días",
                            fontSize = 12.sp,
                            color = BioWayColors.TextGrey
                        )
                    }
                }
            }
        }
    }
}
