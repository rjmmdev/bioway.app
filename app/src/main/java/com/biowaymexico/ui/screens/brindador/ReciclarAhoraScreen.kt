package com.biowaymexico.ui.screens.brindador

import androidx.compose.animation.core.*
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
import com.google.accompanist.pager.*
import kotlinx.coroutines.launch

/**
 * Pantalla de Flujo de Reciclaje - Diseño Minimalista Card-Based
 * Aplica estándar visual del módulo Brindador (fondo claro, cards blancas)
 * Referencias: Material Design Steppers, tendencias 2024-2025
 */
@OptIn(ExperimentalPagerApi::class)
@Composable
fun ReciclarAhoraScreen(
    onNavigateBack: () -> Unit,
    onReciclajeCompletado: () -> Unit
) {
    var materialesFirebase by remember { mutableStateOf<List<com.biowaymexico.data.MaterialReciclable>>(emptyList()) }
    var materialesSeleccionados by remember { mutableStateOf(mapOf<String, Double>()) }  // materialId -> cantidad en kg
    var showConfirmDialog by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }

    val pagerState = rememberPagerState()
    val currentPage = pagerState.currentPage
    val scope = rememberCoroutineScope()
    val brindadorRepository = remember { com.biowaymexico.data.BrindadorRepository() }
    val materialesRepository = remember { com.biowaymexico.data.MaterialesRepository() }
    val reciclajeRepository = remember { com.biowaymexico.data.ReciclajeRepository() }

    // Cargar materiales de Firebase
    LaunchedEffect(Unit) {
        scope.launch {
            val result = materialesRepository.obtenerMateriales()
            if (result.isSuccess) {
                materialesFirebase = result.getOrNull() ?: emptyList()
            }
        }
    }

    Scaffold(
        containerColor = Color(0xFFFAFAFA),  // Mismo fondo que Dashboard
        topBar = {
            // Header minimalista - mismo estilo que Dashboard
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 12.dp),  // +16dp top para barra del sistema
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
                    text = "Reciclar Ahora",
                    style = MaterialTheme.typography.titleLarge,  // Hammersmith One
                    color = BioWayColors.BrandDarkGreen
                )

                // Espacio para balance
                Spacer(modifier = Modifier.width(48.dp))
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Stepper horizontal moderno
            HorizontalStepper(
                currentStep = currentPage,
                totalSteps = 4,
                stepLabels = listOf("Materiales", "Aviso", "Depositar", "Finalizar")
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Contenido del paso actual
            HorizontalPager(
                count = 4,
                state = pagerState,
                userScrollEnabled = false,
                modifier = Modifier.weight(1f)
            ) { page ->
                when (page) {
                    0 -> Paso1SelectMaterialesFirebase(
                        materiales = materialesFirebase,
                        materialesSeleccionados = materialesSeleccionados,
                        onCantidadChange = { materialId, cantidad ->
                            materialesSeleccionados = if (cantidad > 0) {
                                materialesSeleccionados + (materialId to cantidad)
                            } else {
                                materialesSeleccionados - materialId
                            }
                        }
                    )
                    1 -> Paso2AvisoMinimo()
                    2 -> Paso3InstruccionesDeposito(
                        materialesSeleccionados = materialesSeleccionados.keys.toSet()
                    )
                    3 -> Paso4FinalizarTarea()
                }
            }

            // Botones de navegación
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (currentPage > 0) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(currentPage - 1)
                            }
                        },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = BioWayColors.BrandDarkGreen
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            BioWayColors.BrandDarkGreen
                        )
                    ) {
                        Text(
                            "Anterior",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                Button(
                    onClick = {
                        if (currentPage < 3) {
                            scope.launch {
                                pagerState.animateScrollToPage(currentPage + 1)
                            }
                        } else {
                            showConfirmDialog = true
                        }
                    },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF70D162)
                    )
                ) {
                    Text(
                        text = if (currentPage < 3) "Siguiente" else "Finalizar",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }
            }
        }

        // Diálogo de confirmación
        if (showConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = BioWayColors.BrandGreen,
                        modifier = Modifier.size(48.dp)
                    )
                },
                title = {
                    Text(
                        "¿Confirmar recolección?",
                        style = MaterialTheme.typography.titleLarge,
                        color = BioWayColors.BrandDarkGreen,
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Text(
                        "¿Estás seguro que ya colocaste tus reciclables para recolección?\n\nNo podrás solicitar otra recolección hasta que el recolector recoja estos materiales.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF2E7D6C),
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            scope.launch {
                                isProcessing = true

                                // Preparar materiales reciclados
                                val materialesReciclados = materialesSeleccionados.mapNotNull { (materialId, cantidad) ->
                                    val material = materialesFirebase.find { it.id == materialId }
                                    material?.let {
                                        com.biowaymexico.data.MaterialReciclado(
                                            materialId = materialId,
                                            nombre = it.nombre,
                                            cantidad = cantidad
                                        )
                                    }
                                }

                                // Registrar reciclaje
                                val result = reciclajeRepository.registrarReciclaje(materialesReciclados)

                                isProcessing = false
                                showConfirmDialog = false

                                if (result.isSuccess) {
                                    onReciclajeCompletado()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF70D162)
                        ),
                        enabled = !isProcessing
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Sí, confirmar")
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmDialog = false }) {
                        Text(
                            "Cancelar",
                            color = Color(0xFF2E7D6C)
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun HorizontalStepper(
    currentStep: Int,
    totalSteps: Int,
    stepLabels: List<String>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        // Indicador de progreso lineal
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(totalSteps) { index ->
                // Círculo numerado
                Box(
                    modifier = Modifier
                        .size(if (index == currentStep) 40.dp else 32.dp)
                        .background(
                            color = when {
                                index < currentStep -> BioWayColors.BrandGreen  // Completado
                                index == currentStep -> Color(0xFF70D162)  // Actual
                                else -> Color.White  // Pendiente
                            },
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (index < currentStep) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (index == currentStep) Color.White else Color(0xFF2E7D6C),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Línea conectora (excepto después del último)
                if (index < totalSteps - 1) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp)
                            .background(
                                color = if (index < currentStep)
                                    BioWayColors.BrandGreen.copy(alpha = 0.6f)
                                else
                                    Color(0xFF2E7D6C).copy(alpha = 0.2f)
                            )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Labels de los pasos
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            stepLabels.forEachIndexed { index, label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (index == currentStep)
                        Color(0xFF70D162)
                    else
                        Color(0xFF2E7D6C).copy(alpha = 0.7f),
                    fontWeight = if (index == currentStep) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 11.sp,
                    modifier = Modifier.width(70.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun Paso1SelectMateriales(
    materiales: List<Material>,
    materialesSeleccionados: Set<String>,
    onMaterialToggle: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Selecciona tus materiales",
                style = MaterialTheme.typography.headlineSmall,
                color = BioWayColors.BrandDarkGreen,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Aviso mínimo
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = BioWayColors.BrandGreen.copy(alpha = 0.12f)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFF70D162),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Debes donar mínimo 1 kg",
                        style = MaterialTheme.typography.bodyMedium,
                        color = BioWayColors.BrandDarkGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Lista de materiales
        items(materiales.size) { index ->
            val material = materiales[index]
            val isSelected = material.nombre in materialesSeleccionados

            MaterialCard(
                material = material,
                isSelected = isSelected,
                onClick = { onMaterialToggle(material.nombre) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))

            // Aviso de frecuencia
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = BioWayColors.BrandBlue.copy(alpha = 0.12f)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = BioWayColors.BrandBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "¡Recuerda que premiamos la frecuencia con la que reciclas!",
                        style = MaterialTheme.typography.bodySmall,
                        color = BioWayColors.BrandDarkGreen,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun MaterialCard(
    material: Material,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = if (isSelected) 3.dp else 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono del material
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = if (isSelected)
                            BioWayColors.BrandGreen.copy(alpha = 0.15f)
                        else
                            Color(0xFF2E7D6C).copy(alpha = 0.08f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = material.icono,
                    contentDescription = null,
                    tint = if (isSelected) BioWayColors.BrandGreen else Color(0xFF2E7D6C),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = material.nombre,
                    style = MaterialTheme.typography.titleSmall,  // Hammersmith One
                    color = if (isSelected) BioWayColors.BrandDarkGreen else Color(0xFF2E7D6C),
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    text = material.descripcion,
                    style = MaterialTheme.typography.bodySmall,  // Montserrat
                    color = Color(0xFF2E7D6C).copy(alpha = 0.8f),
                    fontSize = 11.sp
                )
            }

            Checkbox(
                checked = isSelected,
                onCheckedChange = { onClick() },
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFF70D162),
                    uncheckedColor = Color(0xFF2E7D6C).copy(alpha = 0.5f),
                    checkmarkColor = Color.White
                )
            )
        }
    }
}

@Composable
private fun Paso2AvisoMinimo() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icono
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(
                    color = BioWayColors.BrandGreen.copy(alpha = 0.15f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Scale,
                contentDescription = null,
                tint = BioWayColors.BrandGreen,
                modifier = Modifier.size(50.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Requisito mínimo",
            style = MaterialTheme.typography.headlineMedium,  // Hammersmith One
            color = BioWayColors.BrandDarkGreen,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 3.dp
        ) {
            Text(
                text = "Para donar debes tener el mínimo especificado de material",
                modifier = Modifier.padding(28.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = BioWayColors.BrandDarkGreen,
                textAlign = TextAlign.Center,
                lineHeight = 26.sp
            )
        }
    }
}

@Composable
private fun Paso3InstruccionesDeposito(
    materialesSeleccionados: Set<String>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(
                            color = BioWayColors.BrandTurquoise.copy(alpha = 0.15f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingBag,
                        contentDescription = null,
                        tint = BioWayColors.BrandTurquoise,
                        modifier = Modifier.size(50.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Deposita tus reciclables",
                    style = MaterialTheme.typography.headlineMedium,
                    color = BioWayColors.BrandDarkGreen,
                    textAlign = TextAlign.Center
                )
            }
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                shadowElevation = 3.dp
            ) {
                Text(
                    text = "Ahora que tienes tus reciclables separados y ordenados, es momento de depositarlo(s) en una bolsa/contenedor para que el recolector pueda llevarlo(s) a los centros de acopio.",
                    modifier = Modifier.padding(24.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF2E7D6C),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }
        }

        // Materiales seleccionados
        if (materialesSeleccionados.isNotEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "Materiales a depositar:",
                            style = MaterialTheme.typography.titleSmall,
                            color = BioWayColors.BrandDarkGreen,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        materialesSeleccionados.forEach { material ->
                            Row(
                                modifier = Modifier.padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF70D162),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = material,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF2E7D6C)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Paso4FinalizarTarea() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(
                    color = Color(0xFF70D162).copy(alpha = 0.15f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = Color(0xFF70D162),
                modifier = Modifier.size(50.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Finaliza Tarea",
            style = MaterialTheme.typography.headlineMedium,
            color = BioWayColors.BrandDarkGreen,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 3.dp
        ) {
            Text(
                text = "Por favor, pulsa sobre el botón finalizar tarea, se te activará como punto de recolección para que el recolector sepa que hay reciclables en tu domicilio.",
                modifier = Modifier.padding(24.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF2E7D6C),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Aviso importante
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFFF9800).copy(alpha = 0.12f)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Importante: No podrás volver a brindar hasta que el recolector recoja estos materiales.",
                    style = MaterialTheme.typography.bodySmall,
                    color = BioWayColors.BrandDarkGreen,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

// Modelo de datos
@Composable
private fun Paso1SelectMaterialesFirebase(
    materiales: List<com.biowaymexico.data.MaterialReciclable>,
    materialesSeleccionados: Map<String, Double>,
    onCantidadChange: (String, Double) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Selecciona tus materiales",
                style = MaterialTheme.typography.headlineSmall,
                color = BioWayColors.BrandDarkGreen,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(materiales.size) { index ->
            val material = materiales[index]
            val cantidadActual = materialesSeleccionados[material.id] ?: 0.0

            MaterialFirebaseCard(
                material = material,
                cantidad = cantidadActual,
                onCantidadChange = { nuevaCantidad ->
                    onCantidadChange(material.id, nuevaCantidad)
                }
            )
        }
    }
}

@Composable
private fun MaterialFirebaseCard(
    material: com.biowaymexico.data.MaterialReciclable,
    cantidad: Double,
    onCantidadChange: (Double) -> Unit
) {
    val materialColor = try {
        Color(android.graphics.Color.parseColor(material.color))
    } catch (e: Exception) {
        BioWayColors.BrandGreen
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
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
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Cantidad (${material.unidad}):",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF2E7D6C)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (cantidad > 0) {
                                onCantidadChange((cantidad - 0.5).coerceAtLeast(0.0))
                            }
                        },
                        enabled = cantidad > 0
                    ) {
                        Icon(
                            Icons.Default.Remove,
                            contentDescription = "Disminuir",
                            tint = if (cantidad > 0) materialColor else Color.Gray
                        )
                    }

                    Text(
                        text = String.format("%.1f", cantidad),
                        style = MaterialTheme.typography.titleMedium,
                        color = BioWayColors.BrandDarkGreen,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(
                        onClick = {
                            onCantidadChange(cantidad + 0.5)
                        }
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Aumentar",
                            tint = materialColor
                        )
                    }
                }
            }
        }
    }
}

data class Material(
    val nombre: String,
    val descripcion: String,
    val icono: ImageVector
)
