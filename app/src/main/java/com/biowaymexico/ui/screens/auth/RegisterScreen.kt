package com.biowaymexico.ui.screens.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biowaymexico.ui.theme.BioWayColors
import com.google.accompanist.pager.*

/**
 * Pantalla de Registro de BioWay - Multi-paso
 * Migrada desde Flutter con diseño exacto
 */
@OptIn(ExperimentalPagerApi::class)
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateBack: () -> Unit
) {
    // Estados del formulario
    var selectedUserType by remember { mutableStateOf<String?>(null) }
    var currentPage by remember { mutableStateOf(0) }
    val totalPages = 3
    val pagerState = rememberPagerState()

    // Campos básicos (Paso 2)
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var obscurePassword by remember { mutableStateOf(true) }
    var obscureConfirmPassword by remember { mutableStateOf(true) }

    // Campos Brindador (Paso 3A)
    var address by remember { mutableStateOf("") }
    var postalCode by remember { mutableStateOf("") }
    var colony by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }

    // Campos Recolector (Paso 3B)
    var companyCode by remember { mutableStateOf("") }
    var selectedZone by remember { mutableStateOf<String?>(null) }
    var hasSmartphone by remember { mutableStateOf(true) }

    // Estados de UI
    var acceptedTerms by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Zonas disponibles
    val zones = listOf(
        "Centro", "Norte", "Sur", "Este", "Oeste",
        "Polanco", "Condesa", "Roma Norte", "Coyoacán", "Satelite"
    )

    // Sincronizar pagerState con currentPage
    LaunchedEffect(pagerState.currentPage) {
        currentPage = pagerState.currentPage
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        BioWayColors.MediumGreen,
                        BioWayColors.AquaGreen,
                        BioWayColors.Turquoise
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header con logo y pasos
            RegisterHeader(
                currentPage = currentPage,
                totalPages = totalPages,
                onBackPressed = {
                    if (currentPage > 0) {
                        // Navegar a página anterior
                        currentPage = currentPage - 1
                    } else {
                        onNavigateBack()
                    }
                }
            )

            // Contenido con HorizontalPager
            HorizontalPager(
                count = totalPages,
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                userScrollEnabled = false // Deshabilitar swipe manual
            ) { page ->
                when (page) {
                    0 -> UserTypeSelectionStep(
                        selectedUserType = selectedUserType,
                        onUserTypeSelected = { selectedUserType = it }
                    )
                    1 -> BasicInfoStep(
                        name = name,
                        onNameChange = { name = it },
                        email = email,
                        onEmailChange = { email = it },
                        phone = phone,
                        onPhoneChange = { phone = it },
                        password = password,
                        onPasswordChange = { password = it },
                        confirmPassword = confirmPassword,
                        onConfirmPasswordChange = { confirmPassword = it },
                        obscurePassword = obscurePassword,
                        onTogglePasswordVisibility = { obscurePassword = !obscurePassword },
                        obscureConfirmPassword = obscureConfirmPassword,
                        onToggleConfirmPasswordVisibility = { obscureConfirmPassword = !obscureConfirmPassword }
                    )
                    2 -> {
                        if (selectedUserType == "brindador") {
                            BrindadorStep(
                                address = address,
                                onAddressChange = { address = it },
                                postalCode = postalCode,
                                onPostalCodeChange = { postalCode = it },
                                colony = colony,
                                onColonyChange = { colony = it },
                                city = city,
                                onCityChange = { city = it },
                                state = state,
                                onStateChange = { state = it },
                                acceptedTerms = acceptedTerms,
                                onAcceptedTermsChange = { acceptedTerms = it }
                            )
                        } else {
                            RecolectorStep(
                                companyCode = companyCode,
                                onCompanyCodeChange = { companyCode = it },
                                selectedZone = selectedZone,
                                onZoneSelected = { selectedZone = it },
                                zones = zones,
                                hasSmartphone = hasSmartphone,
                                onSmartphoneChange = { hasSmartphone = it },
                                acceptedTerms = acceptedTerms,
                                onAcceptedTermsChange = { acceptedTerms = it }
                            )
                        }
                    }
                }
            }

            // Navegación inferior
            BottomNavigationButtons(
                currentPage = currentPage,
                totalPages = totalPages,
                isLoading = isLoading,
                canProceed = when (currentPage) {
                    0 -> selectedUserType != null
                    1 -> name.isNotEmpty() && email.isNotEmpty() &&
                         phone.length == 10 && password.length >= 6 &&
                         password == confirmPassword
                    2 -> acceptedTerms && when (selectedUserType) {
                        "brindador" -> address.isNotEmpty() && postalCode.length == 5 &&
                                      colony.isNotEmpty() && city.isNotEmpty() && state.isNotEmpty()
                        "recolector" -> selectedZone != null
                        else -> false
                    }
                    else -> false
                },
                onPrevious = {
                    if (currentPage > 0) {
                        currentPage = currentPage - 1
                    }
                },
                onNext = {
                    if (currentPage < totalPages - 1) {
                        // Validar paso actual antes de avanzar
                        val canAdvance = when (currentPage) {
                            0 -> {
                                if (selectedUserType == null) {
                                    errorMessage = "Por favor selecciona un tipo de usuario"
                                    false
                                } else {
                                    errorMessage = null
                                    true
                                }
                            }
                            1 -> {
                                when {
                                    name.isEmpty() -> {
                                        errorMessage = "Por favor ingresa tu nombre"
                                        false
                                    }
                                    email.isEmpty() || !email.contains("@") -> {
                                        errorMessage = "Ingresa un correo válido"
                                        false
                                    }
                                    phone.length != 10 -> {
                                        errorMessage = "El teléfono debe tener 10 dígitos"
                                        false
                                    }
                                    password.length < 6 -> {
                                        errorMessage = "La contraseña debe tener mínimo 6 caracteres"
                                        false
                                    }
                                    password != confirmPassword -> {
                                        errorMessage = "Las contraseñas no coinciden"
                                        false
                                    }
                                    else -> {
                                        errorMessage = null
                                        true
                                    }
                                }
                            }
                            else -> true
                        }

                        if (canAdvance) {
                            currentPage = currentPage + 1
                        }
                    } else {
                        // Último paso - crear cuenta
                        if (!acceptedTerms) {
                            errorMessage = "Debes aceptar los términos y condiciones"
                        } else {
                            isLoading = true
                            // Simular registro
                            onRegisterSuccess()
                        }
                    }
                }
            )

            // Mostrar error si existe
            errorMessage?.let { message ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    containerColor = BioWayColors.Error,
                    contentColor = Color.White
                ) {
                    Text(message)
                }
            }
        }
    }

    // Navegar a la página cuando currentPage cambie
    LaunchedEffect(currentPage) {
        pagerState.animateScrollToPage(currentPage)
    }
}

@Composable
private fun RegisterHeader(
    currentPage: Int,
    totalPages: Int,
    onBackPressed: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        // Fila con botón atrás, logo y título
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackPressed) {
                Icon(
                    Icons.Default.ArrowBackIos,
                    contentDescription = "Atrás",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Logo y título (centrados)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.weight(3f)
            ) {
                // TODO: Agregar logo SVG aquí cuando esté disponible
                Icon(
                    Icons.Default.Eco,
                    contentDescription = "BioWay",
                    tint = Color.White,
                    modifier = Modifier.size(50.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Registro",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Indicador de pasos
        StepIndicator(
            currentStep = currentPage,
            totalSteps = totalPages
        )
    }
}

@Composable
private fun StepIndicator(
    currentStep: Int,
    totalSteps: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (step in 0 until totalSteps) {
            val isActive = step == currentStep
            val isPast = step < currentStep

            // Círculo numerado
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = if (isActive || isPast) Color.White else Color.Black.copy(alpha = 0.2f),
                        shape = CircleShape
                    )
                    .border(
                        width = 2.dp,
                        color = Color.White,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isPast) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Completado",
                        tint = BioWayColors.PrimaryGreen,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Text(
                        text = "${step + 1}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) BioWayColors.PrimaryGreen else Color.White
                    )
                }
            }

            // Línea conectora
            if (step < totalSteps - 1) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(2.dp)
                        .background(
                            color = if (isPast) Color.White else Color.Black.copy(alpha = 0.2f)
                        )
                )
            }
        }
    }
}

@Composable
private fun UserTypeSelectionStep(
    selectedUserType: String?,
    onUserTypeSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.PersonOutline,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color.White
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "¿Cómo participarás en BioWay?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Selecciona tu rol en la comunidad",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.9f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Tarjeta Brindador
        UserTypeCard(
            icon = Icons.Default.Home,
            title = "Brindador",
            subtitle = "Recicla desde casa",
            description = "Separa residuos, agenda recolecciones y gana recompensas",
            isSelected = selectedUserType == "brindador",
            onClick = { onUserTypeSelected("brindador") }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Tarjeta Recolector
        UserTypeCard(
            icon = Icons.Default.LocalShipping,
            title = "Recolector",
            subtitle = "Recolecta materiales",
            description = "Accede a materiales pre-separados y optimiza tus rutas",
            isSelected = selectedUserType == "recolector",
            onClick = { onUserTypeSelected("recolector") }
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Información adicional
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = Color.Black.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(16.dp)
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(16.dp)
        ) {
            Row {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "¿Tienes una empresa? Contacta con nosotros para crear una cuenta empresarial",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
private fun UserTypeCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) Color.White else Color.Black.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(2.dp, Color.White),
        shadowElevation = if (isSelected) 20.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono circular
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(
                        color = if (isSelected)
                            BioWayColors.PrimaryGreen.copy(alpha = 0.1f)
                        else
                            Color.Black.copy(alpha = 0.2f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = title,
                    modifier = Modifier.size(30.dp),
                    tint = if (isSelected) BioWayColors.PrimaryGreen else Color.White
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Textos
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) BioWayColors.DarkGreen else Color.White
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected)
                        BioWayColors.PrimaryGreen
                    else
                        Color.White.copy(alpha = 0.9f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected)
                        BioWayColors.TextGrey
                    else
                        Color.White.copy(alpha = 0.8f)
                )
            }

            // Indicador de selección
            Icon(
                if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (isSelected) BioWayColors.PrimaryGreen else Color.White
            )
        }
    }
}

@Composable
private fun BasicInfoStep(
    name: String,
    onNameChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    obscurePassword: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    obscureConfirmPassword: Boolean,
    onToggleConfirmPasswordVisibility: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.PersonAdd,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color.White
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Información básica",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(40.dp))

        RegisterTextField(
            value = name,
            onValueChange = onNameChange,
            label = "Nombre completo",
            icon = Icons.Default.PersonOutline
        )

        Spacer(modifier = Modifier.height(20.dp))

        RegisterTextField(
            value = email,
            onValueChange = onEmailChange,
            label = "Correo electrónico",
            icon = Icons.Default.Email,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        Spacer(modifier = Modifier.height(20.dp))

        RegisterTextField(
            value = phone,
            onValueChange = { if (it.length <= 10 && it.all { char -> char.isDigit() }) onPhoneChange(it) },
            label = "Teléfono (10 dígitos)",
            icon = Icons.Default.Phone,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
        )

        Spacer(modifier = Modifier.height(20.dp))

        RegisterTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = "Contraseña",
            icon = Icons.Default.Lock,
            visualTransformation = if (obscurePassword) PasswordVisualTransformation() else VisualTransformation.None,
            trailingIcon = {
                IconButton(onClick = onTogglePasswordVisibility) {
                    Icon(
                        if (obscurePassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle password visibility",
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        RegisterTextField(
            value = confirmPassword,
            onValueChange = onConfirmPasswordChange,
            label = "Confirmar contraseña",
            icon = Icons.Default.Lock,
            visualTransformation = if (obscureConfirmPassword) PasswordVisualTransformation() else VisualTransformation.None,
            trailingIcon = {
                IconButton(onClick = onToggleConfirmPasswordVisibility) {
                    Icon(
                        if (obscureConfirmPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle password visibility",
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        )
    }
}

@Composable
private fun BrindadorStep(
    address: String,
    onAddressChange: (String) -> Unit,
    postalCode: String,
    onPostalCodeChange: (String) -> Unit,
    colony: String,
    onColonyChange: (String) -> Unit,
    city: String,
    onCityChange: (String) -> Unit,
    state: String,
    onStateChange: (String) -> Unit,
    acceptedTerms: Boolean,
    onAcceptedTermsChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Home,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color.White
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Dirección de recolección",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Donde recolectaremos tus materiales",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.9f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(40.dp))

        RegisterTextField(
            value = address,
            onValueChange = onAddressChange,
            label = "Calle y número",
            icon = Icons.Default.LocationOn
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            RegisterTextField(
                value = postalCode,
                onValueChange = { if (it.length <= 5 && it.all { char -> char.isDigit() }) onPostalCodeChange(it) },
                label = "Código Postal",
                icon = Icons.Default.PinDrop,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )

            RegisterTextField(
                value = colony,
                onValueChange = onColonyChange,
                label = "Colonia",
                icon = Icons.Default.Apartment,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            RegisterTextField(
                value = city,
                onValueChange = onCityChange,
                label = "Ciudad/Municipio",
                icon = Icons.Default.LocationCity,
                modifier = Modifier.weight(1f)
            )

            RegisterTextField(
                value = state,
                onValueChange = onStateChange,
                label = "Estado",
                icon = Icons.Default.Map,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        TermsCheckbox(
            checked = acceptedTerms,
            onCheckedChange = onAcceptedTermsChange
        )
    }
}

@Composable
private fun RecolectorStep(
    companyCode: String,
    onCompanyCodeChange: (String) -> Unit,
    selectedZone: String?,
    onZoneSelected: (String) -> Unit,
    zones: List<String>,
    hasSmartphone: Boolean,
    onSmartphoneChange: (Boolean) -> Unit,
    acceptedTerms: Boolean,
    onAcceptedTermsChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.LocalShipping,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color.White
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Información de recolector",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(40.dp))

        RegisterTextField(
            value = companyCode,
            onValueChange = onCompanyCodeChange,
            label = "Código de empresa (opcional)",
            icon = Icons.Default.Business,
            helperText = "Si perteneces a una empresa asociada"
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Dropdown de zona
        ZoneDropdown(
            selectedZone = selectedZone,
            onZoneSelected = onZoneSelected,
            zones = zones
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Switch de smartphone
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = Color.Black.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(16.dp)
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Smartphone,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "¿Tienes smartphone?",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = hasSmartphone,
                        onCheckedChange = onSmartphoneChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = BioWayColors.PrimaryGreen
                        )
                    )
                }

                if (!hasSmartphone) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No te preocupes, podrás acceder a horarios fijos de recolección en tu zona",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        TermsCheckbox(
            checked = acceptedTerms,
            onCheckedChange = onAcceptedTermsChange
        )
    }
}

@Composable
private fun RegisterTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    trailingIcon: @Composable (() -> Unit)? = null,
    helperText: String? = null
) {
    Column(modifier = modifier) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 10.dp
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text(label) },
                leadingIcon = {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = BioWayColors.PrimaryGreen
                    )
                },
                trailingIcon = trailingIcon,
                visualTransformation = visualTransformation,
                keyboardOptions = keyboardOptions,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = BioWayColors.TextDark,
                    unfocusedTextColor = BioWayColors.TextDark,
                    focusedLabelColor = BioWayColors.TextGrey,
                    unfocusedLabelColor = BioWayColors.TextGrey
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        helperText?.let { text ->
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ZoneDropdown(
    selectedZone: String?,
    onZoneSelected: (String) -> Unit,
    zones: List<String>
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 10.dp
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = selectedZone ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Zona de operación") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Map,
                        contentDescription = null,
                        tint = BioWayColors.PrimaryGreen
                    )
                },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = BioWayColors.TextDark,
                    unfocusedTextColor = BioWayColors.TextDark,
                    focusedLabelColor = BioWayColors.TextGrey,
                    unfocusedLabelColor = BioWayColors.TextGrey
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                zones.forEach { zone ->
                    DropdownMenuItem(
                        text = { Text(zone) },
                        onClick = {
                            onZoneSelected(zone)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TermsCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color.Black.copy(alpha = 0.15f),
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Top
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = Color.White,
                    checkmarkColor = BioWayColors.PrimaryGreen,
                    uncheckedColor = Color.White
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Acepto los términos y condiciones y la política de privacidad",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.clickable { onCheckedChange(!checked) }
            )
        }
    }
}

@Composable
private fun BottomNavigationButtons(
    currentPage: Int,
    totalPages: Int,
    isLoading: Boolean,
    canProceed: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Botón Anterior (solo visible después del primer paso)
        if (currentPage > 0) {
            OutlinedButton(
                onClick = onPrevious,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, Color.White),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Anterior",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Botón Siguiente / Crear cuenta
        Button(
            onClick = onNext,
            enabled = !isLoading && canProceed,
            modifier = Modifier
                .weight(1f)
                .height(56.dp)
                .shadow(
                    elevation = if (canProceed) 20.dp else 0.dp,
                    shape = RoundedCornerShape(16.dp)
                ),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = BioWayColors.PrimaryGreen,
                disabledContainerColor = Color.White.copy(alpha = 0.5f),
                disabledContentColor = BioWayColors.PrimaryGreen.copy(alpha = 0.5f)
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = BioWayColors.PrimaryGreen,
                    strokeWidth = 2.5.dp
                )
            } else {
                Text(
                    text = if (currentPage == totalPages - 1) "Crear cuenta" else "Siguiente",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
