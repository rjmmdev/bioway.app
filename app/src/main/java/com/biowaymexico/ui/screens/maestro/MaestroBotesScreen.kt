package com.biowaymexico.ui.screens.maestro

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.biowaymexico.ui.theme.BioWayColors
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun MaestroBotesScreen(navController: NavHostController) {
    var botes by remember { mutableStateOf<List<com.biowaymexico.data.models.BoteBioWayModel>>(emptyList()) }

    val firestore = remember { com.google.firebase.firestore.FirebaseFirestore.getInstance() }
    val scope = rememberCoroutineScope()

    // Cargar botes
    LaunchedEffect(navController.currentBackStackEntry) {
        scope.launch {
            val snapshot = firestore.collection("BoteBioWay").get().await()

            botes = snapshot.documents.mapNotNull { doc ->
                try {
                    @Suppress("UNCHECKED_CAST")
                    val data = doc.data as? Map<String, Any?> ?: return@mapNotNull null

                    com.biowaymexico.data.models.BoteBioWayModel(
                        userId = data["userId"] as? String ?: "",
                        identificador = data["identificador"] as? String ?: "",
                        email = data["email"] as? String ?: "",
                        estado = data["estado"] as? String ?: "",
                        municipio = data["municipio"] as? String ?: "",
                        colonia = data["colonia"] as? String ?: "",
                        codigoPostal = data["codigoPostal"] as? String ?: "",
                        latitud = (data["latitud"] as? Number)?.toDouble(),
                        longitud = (data["longitud"] as? Number)?.toDouble(),
                        tipoUsuario = data["tipoUsuario"] as? String ?: "BoteBioWay",
                        estadoOperativo = data["estadoOperativo"] as? Boolean ?: true,
                        fechaRegistro = data["fechaRegistro"] as? com.google.firebase.Timestamp
                    )
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    Scaffold(
        containerColor = Color(0xFFFAFAFA),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    navController.navigate(com.biowaymexico.ui.navigation.BioWayDestinations.MaestroCrearBote.route)
                },
                containerColor = Color(0xFF70D162)
            ) {
                Icon(Icons.Default.Add, "Crear Bote", tint = Color.White)
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            "Volver",
                            tint = BioWayColors.BrandDarkGreen
                        )
                    }
                    Text(
                        text = "Gestión de Botes BioWay",
                        style = MaterialTheme.typography.headlineMedium,
                        color = BioWayColors.BrandDarkGreen
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                Text(
                    text = "Botes Registrados (${botes.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = BioWayColors.BrandDarkGreen,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Lista de botes
            items(botes.size) { index ->
                val bote = botes[index]
                BoteCard(
                    bote = bote,
                    onEditClick = {
                        // TODO: Navegar a pantalla de edición
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun BoteCard(
    bote: com.biowaymexico.data.models.BoteBioWayModel,
    onEditClick: () -> Unit
) {
    Surface(
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
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = BioWayColors.BrandGreen.copy(alpha = 0.15f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = null,
                    tint = BioWayColors.BrandGreen,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bote.identificador,
                    style = MaterialTheme.typography.titleMedium,
                    color = BioWayColors.BrandDarkGreen,
                    fontWeight = FontWeight.Bold
                )
                if (bote.estado.isNotBlank() || bote.municipio.isNotBlank()) {
                    Text(
                        text = "${bote.colonia}, ${bote.municipio}, ${bote.estado}".trim(',', ' '),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF2E7D6C),
                        fontSize = 11.sp
                    )
                }
                if (bote.latitud != null && bote.longitud != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            null,
                            tint = Color(0xFF70D162),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "%.4f, %.4f".format(bote.latitud, bote.longitud),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF70D162),
                            fontSize = 10.sp
                        )
                    }
                }
            }

            IconButton(onClick = onEditClick) {
                Icon(
                    Icons.Default.Edit,
                    "Editar",
                    tint = Color(0xFF2196F3)
                )
            }
        }
    }
}
