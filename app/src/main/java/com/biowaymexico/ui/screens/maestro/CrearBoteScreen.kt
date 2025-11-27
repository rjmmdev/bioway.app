package com.biowaymexico.ui.screens.maestro

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.biowaymexico.ui.theme.BioWayColors
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearBoteScreen(navController: NavHostController) {
    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle

    var email by remember(savedStateHandle) {
        mutableStateOf(savedStateHandle?.get<String>("email") ?: "")
    }
    var password by remember(savedStateHandle) {
        mutableStateOf(savedStateHandle?.get<String>("password") ?: "")
    }
    var estado by remember(savedStateHandle) {
        mutableStateOf(savedStateHandle?.get<String>("estado") ?: "CDMX")
    }
    var municipio by remember(savedStateHandle) {
        mutableStateOf(savedStateHandle?.get<String>("municipio") ?: "")
    }
    var colonia by remember(savedStateHandle) {
        mutableStateOf(savedStateHandle?.get<String>("colonia") ?: "")
    }
    var codigoPostal by remember(savedStateHandle) {
        mutableStateOf(savedStateHandle?.get<String>("codigoPostal") ?: "")
    }
    var latitud by remember(savedStateHandle) {
        mutableStateOf(savedStateHandle?.get<Double>("latitud"))
    }
    var longitud by remember(savedStateHandle) {
        mutableStateOf(savedStateHandle?.get<Double>("longitud"))
    }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var nextBoteNumber by remember { mutableStateOf(1) }

    val authRepository = remember { com.biowaymexico.data.AuthRepository() }
    val firestore = remember { com.google.firebase.firestore.FirebaseFirestore.getInstance() }
    val scope = rememberCoroutineScope()

    // Guardar estado en SavedStateHandle al cambiar
    LaunchedEffect(email) { savedStateHandle?.set("email", email) }
    LaunchedEffect(password) { savedStateHandle?.set("password", password) }
    LaunchedEffect(estado) { savedStateHandle?.set("estado", estado) }
    LaunchedEffect(municipio) { savedStateHandle?.set("municipio", municipio) }
    LaunchedEffect(colonia) { savedStateHandle?.set("colonia", colonia) }
    LaunchedEffect(codigoPostal) { savedStateHandle?.set("codigoPostal", codigoPostal) }
    LaunchedEffect(latitud) { savedStateHandle?.set("latitud", latitud) }
    LaunchedEffect(longitud) { savedStateHandle?.set("longitud", longitud) }

    // Obtener siguiente número
    LaunchedEffect(Unit) {
        scope.launch {
            val snapshot = firestore.collection("BoteBioWay").get().await()
            nextBoteNumber = snapshot.size() + 1
        }
    }

    Scaffold(
        containerColor = Color(0xFFFAFAFA),
        topBar = {
            TopAppBar(
                title = { Text("Crear Bote BioWay") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = BioWayColors.BrandDarkGreen
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Identificador automático
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = BioWayColors.BrandGreen.copy(alpha = 0.12f)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        null,
                        tint = Color(0xFF70D162),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Identificador automático",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF2E7D6C)
                        )
                        Text(
                            text = "BOTE-${String.format("%03d", nextBoteNumber)}",
                            style = MaterialTheme.typography.titleLarge,
                            color = BioWayColors.BrandDarkGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Credenciales
            Text(
                text = "Credenciales de Acceso",
                style = MaterialTheme.typography.titleMedium,
                color = BioWayColors.BrandDarkGreen,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email para login") },
                leadingIcon = { Icon(Icons.Default.Email, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BioWayColors.BrandGreen,
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black
                )
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                leadingIcon = { Icon(Icons.Default.Lock, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BioWayColors.BrandGreen,
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black
                )
            )

            HorizontalDivider()

            // Ubicación
            Text(
                text = "Ubicación (opcional)",
                style = MaterialTheme.typography.titleMedium,
                color = BioWayColors.BrandDarkGreen,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = estado,
                onValueChange = { estado = it },
                label = { Text("Estado") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BioWayColors.BrandGreen,
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = municipio,
                    onValueChange = { municipio = it },
                    label = { Text("Municipio") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BioWayColors.BrandGreen,
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black
                    )
                )
                OutlinedTextField(
                    value = codigoPostal,
                    onValueChange = { codigoPostal = it },
                    label = { Text("C.P.") },
                    singleLine = true,
                    modifier = Modifier.weight(0.6f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BioWayColors.BrandGreen,
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black
                    )
                )
            }

            OutlinedTextField(
                value = colonia,
                onValueChange = { colonia = it },
                label = { Text("Colonia") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BioWayColors.BrandGreen,
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black
                )
            )

            // Botón generar mapa
            Button(
                onClick = {
                    // Guardar todos los datos antes de navegar
                    savedStateHandle?.apply {
                        set("email", email)
                        set("password", password)
                        set("estado", estado)
                        set("municipio", municipio)
                        set("colonia", colonia)
                        set("codigoPostal", codigoPostal)
                    }

                    navController.navigate(
                        com.biowaymexico.ui.navigation.BioWayDestinations.MaestroMapaSelectorBote.createRoute(
                            estado = estado.ifBlank { "CDMX" },
                            municipio = municipio.ifBlank { "Benito Juarez" },
                            colonia = colonia.ifBlank { "Centro" },
                            cp = codigoPostal.ifBlank { "06000" }
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (latitud != null) Color(0xFF4CAF50) else Color(0xFF2196F3)
                )
            ) {
                Icon(Icons.Default.Map, null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (latitud != null && longitud != null)
                        "Ubicación: %.4f, %.4f".format(latitud, longitud)
                    else
                        "Generar Mapa",
                    color = Color.White
                )
            }

            if (errorMessage != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFCDD2)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, null, tint = Color.Red)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = errorMessage!!,
                            color = Color.Red,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Botón crear
            Button(
                onClick = {
                    val camposUbicacionLlenos = listOf(estado, municipio, colonia, codigoPostal)
                        .count { it.isNotBlank() }

                    if (camposUbicacionLlenos >= 2 && (latitud == null || longitud == null)) {
                        errorMessage = "Debes generar el mapa y seleccionar la ubicación exacta"
                        return@Button
                    }

                    scope.launch {
                        isLoading = true
                        errorMessage = null

                        val identificadorBote = "BOTE-${String.format("%03d", nextBoteNumber)}"

                        val result = authRepository.registrarUsuarioDirecto(
                            email = email,
                            password = password,
                            nombre = identificadorBote,
                            telefono = "",
                            tipoUsuario = "Bote BioWay"
                        )

                        isLoading = false

                        if (result.isSuccess) {
                            val userId = result.getOrNull()
                            if (userId != null) {
                                val updateData = mutableMapOf<String, Any>(
                                    "identificador" to identificadorBote,
                                    "estado" to estado,
                                    "municipio" to municipio,
                                    "colonia" to colonia,
                                    "codigoPostal" to codigoPostal
                                )

                                val lat = latitud
                                val lon = longitud
                                if (lat != null && lon != null) {
                                    updateData["latitud"] = lat
                                    updateData["longitud"] = lon
                                }

                                firestore.collection("BoteBioWay")
                                    .document(userId)
                                    .update(updateData)
                                    .await()
                            }

                            navController.popBackStack()
                        } else {
                            errorMessage = result.exceptionOrNull()?.message ?: "Error al crear bote"
                        }
                    }
                },
                enabled = !isLoading && email.isNotBlank() && password.length >= 6,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF70D162)
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Check, null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Crear Bote BioWay", color = Color.White)
                }
            }
        }
    }
}
