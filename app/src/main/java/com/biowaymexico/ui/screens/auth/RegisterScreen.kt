package com.biowaymexico.ui.screens.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biowaymexico.R
import com.biowaymexico.data.AuthRepository
import com.biowaymexico.ui.theme.BioWayColors
import com.google.accompanist.pager.*
import kotlinx.coroutines.launch

/**
 * Pantalla de Registro - Diseño Moderno con Glassmorphism
 * Aplica estándar visual 2024 de BioWay
 */
@OptIn(ExperimentalPagerApi::class)
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    // Estados
    var selectedUserType by remember { mutableStateOf<String?>(null) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var acceptedTerms by remember { mutableStateOf(false) }
    // Estados de registro
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    val pagerState = rememberPagerState()
    val currentPage = pagerState.currentPage
    val scope = rememberCoroutineScope()
    val authRepository = remember { AuthRepository() }

    // Animación de entrada
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    val animatedAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                // Mismo degradado que Login
                brush = Brush.verticalGradient(
                    colors = listOf(
                        BioWayColors.BrandGreen,
                        BioWayColors.BrandTurquoise,
                        BioWayColors.BrandBlue
                    )
                )
            )
    ) {
        // Círculos decorativos (mismo patrón que Login)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(100.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .offset(x = (-50).dp, y = (-100).dp)
                    .background(
                        color = BioWayColors.BrandBlue.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(50)
                    )
            )
            Box(
                modifier = Modifier
                    .size(250.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 80.dp, y = 100.dp)
                    .background(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(50)
                    )
            )
        }

        // Contenido principal
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)  // Reducido de 32dp a 24dp para cards más anchas
                .alpha(animatedAlpha),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // Header con logo y botón atrás
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botón atrás
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Volver",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Logo pequeño
                Image(
                    painter = painterResource(id = R.drawable.ic_bioway_logo),
                    contentDescription = "BioWay Logo",
                    modifier = Modifier.size(50.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                // Espacio equivalente al botón para balancear
                Spacer(modifier = Modifier.width(48.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Título
            Text(
                text = "Registro",
                style = MaterialTheme.typography.displaySmall,  // Hammersmith One
                color = BioWayColors.BrandDarkGreen
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtítulo
            Text(
                text = "¡Únete a BioWay!",
                style = MaterialTheme.typography.bodyLarge,  // Montserrat
                color = BioWayColors.BrandDarkGreen
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Indicador de pasos
            StepIndicator(
                currentStep = currentPage,
                totalSteps = 3
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Card glassmorphism con el pager
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                color = Color.White.copy(alpha = 0.75f),  // Más opaco para mejor legibilidad
                shadowElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp)  // Reducido de 32dp a 28dp
                ) {
                    HorizontalPager(
                        count = 3,
                        state = pagerState,
                        userScrollEnabled = false
                    ) { page ->
                        when (page) {
                            0 -> UserTypeSelection(
                                selectedType = selectedUserType,
                                onTypeSelected = { selectedUserType = it }
                            )
                            1 -> BasicInfoForm(
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
                                passwordVisible = passwordVisible,
                                onPasswordVisibilityToggle = { passwordVisible = !passwordVisible },
                                confirmPasswordVisible = confirmPasswordVisible,
                                onConfirmPasswordVisibilityToggle = { confirmPasswordVisible = !confirmPasswordVisible }
                            )
                            2 -> TermsAndConditions(
                                accepted = acceptedTerms,
                                onAcceptedChange = { acceptedTerms = it }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Botones de navegación
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Botón Anterior (si no es el primer paso)
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

                        // Botón Siguiente/Registrar
                        Button(
                            onClick = {
                                if (currentPage < 2) {
                                    // Validación en paso 1: tipo de usuario
                                    if (currentPage == 0 && selectedUserType == null) {
                                        errorMessage = "Selecciona un tipo de usuario"
                                        return@Button
                                    }
                                    // Validación en paso 2: datos básicos
                                    if (currentPage == 1) {
                                        when {
                                            name.isBlank() -> {
                                                errorMessage = "Ingresa tu nombre completo"
                                                return@Button
                                            }
                                            email.isBlank() -> {
                                                errorMessage = "Ingresa tu correo electrónico"
                                                return@Button
                                            }
                                            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                                                errorMessage = "Correo electrónico inválido"
                                                return@Button
                                            }
                                            phone.isBlank() -> {
                                                errorMessage = "Ingresa tu teléfono"
                                                return@Button
                                            }
                                            password.length < 6 -> {
                                                errorMessage = "La contraseña debe tener al menos 6 caracteres"
                                                return@Button
                                            }
                                            password != confirmPassword -> {
                                                errorMessage = "Las contraseñas no coinciden"
                                                return@Button
                                            }
                                        }
                                    }

                                    errorMessage = null
                                    scope.launch {
                                        pagerState.animateScrollToPage(currentPage + 1)
                                    }
                                } else {
                                    // Paso final: registrar directamente
                                    if (!acceptedTerms) {
                                        errorMessage = "Debes aceptar los términos y condiciones"
                                        return@Button
                                    }

                                    scope.launch {
                                        isLoading = true
                                        errorMessage = null

                                        val result = authRepository.registrarUsuarioDirecto(
                                            email = email,
                                            password = password,
                                            nombre = name,
                                            telefono = phone,
                                            tipoUsuario = selectedUserType ?: "Brindador"
                                        )

                                        isLoading = false

                                        if (result.isSuccess) {
                                            showSuccessDialog = true
                                        } else {
                                            val error = result.exceptionOrNull()?.message ?: ""
                                            errorMessage = when {
                                                error.contains("email address is already in use") ->
                                                    "Este correo ya está registrado. Intenta iniciar sesión."
                                                error.contains("network error") ->
                                                    "Error de conexión. Verifica tu internet."
                                                error.contains("weak-password") ->
                                                    "La contraseña es muy débil. Usa al menos 6 caracteres."
                                                else -> "Error: $error"
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent
                            ),
                            contentPadding = PaddingValues(0.dp),
                            enabled = !isLoading
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.9f),
                                                Color.White.copy(alpha = 0.8f)
                                            )
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isLoading && currentPage == 2) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = Color(0xFF70D162),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(
                                        text = if (currentPage < 2) "Siguiente" else "Registrar",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color(0xFF70D162)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }

        // Diálogo de error
        if (errorMessage != null) {
            AlertDialog(
                onDismissRequest = { errorMessage = null },
                title = {
                    Text(
                        text = "Error",
                        style = MaterialTheme.typography.titleLarge,
                        color = BioWayColors.BrandDarkGreen
                    )
                },
                text = {
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    TextButton(onClick = { errorMessage = null }) {
                        Text(
                            "Aceptar",
                            color = Color(0xFF70D162),
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(24.dp)
            )
        }

        // Diálogo de éxito
        if (showSuccessDialog) {
            AlertDialog(
                onDismissRequest = { },
                title = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Éxito",
                            tint = Color(0xFF70D162),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "¡Registro Exitoso!",
                            style = MaterialTheme.typography.titleLarge,
                            color = BioWayColors.BrandDarkGreen,
                            textAlign = TextAlign.Center
                        )
                    }
                },
                text = {
                    Text(
                        text = "¡Tu cuenta ha sido creada exitosamente!\n\n" +
                               "Ya puedes iniciar sesión.\n\n" +
                               "⚠️ Recuerda verificar tu email desde tu perfil en los próximos 10 días.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showSuccessDialog = false
                            onNavigateBack()  // Volver al login
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF70D162)
                        )
                    ) {
                        Text(
                            "Ir al Login",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White
                        )
                    }
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(24.dp)
            )
        }
    }
}

@Composable
private fun StepIndicator(
    currentStep: Int,
    totalSteps: Int
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        repeat(totalSteps) { index ->
            // Círculo indicador
            Box(
                modifier = Modifier
                    .size(if (index == currentStep) 12.dp else 8.dp)
                    .background(
                        color = if (index == currentStep)
                            BioWayColors.BrandDarkGreen
                        else
                            Color.White.copy(alpha = 0.4f),
                        shape = CircleShape
                    )
            )

            // Línea conectora (excepto después del último)
            if (index < totalSteps - 1) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(2.dp)
                        .background(
                            color = if (index < currentStep)
                                BioWayColors.BrandDarkGreen.copy(alpha = 0.6f)
                            else
                                Color.White.copy(alpha = 0.3f)
                        )
                )
            }
        }
    }
}

@Composable
private fun UserTypeSelection(
    selectedType: String?,
    onTypeSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Selecciona tu rol",
            style = MaterialTheme.typography.headlineSmall,  // Hammersmith One
            color = BioWayColors.BrandDarkGreen,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        UserTypeCard(
            title = "Brindador",
            subtitle = "Ciudadano que recicla",
            icon = Icons.Default.Person,
            isSelected = selectedType == "Brindador",
            onClick = { onTypeSelected("Brindador") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        UserTypeCard(
            title = "Recolector",
            subtitle = "Recolecto materiales reciclables",
            icon = Icons.Default.LocalShipping,
            isSelected = selectedType == "Recolector",
            onClick = { onTypeSelected("Recolector") }
        )
    }
}

@Composable
private fun UserTypeCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected)
            Color.White.copy(alpha = 0.6f)
        else
            Color.White.copy(alpha = 0.3f),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(
                        color = if (isSelected)
                            BioWayColors.BrandDarkGreen.copy(alpha = 0.15f)
                        else
                            Color(0xFF2E7D6C).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (isSelected) BioWayColors.BrandDarkGreen else Color(0xFF2E7D6C),
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,  // Hammersmith One
                    color = if (isSelected) BioWayColors.BrandDarkGreen else Color(0xFF2E7D6C)
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,  // Montserrat
                    color = Color(0xFF2E7D6C)
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Seleccionado",
                    tint = Color(0xFF70D162),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun BasicInfoForm(
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
    passwordVisible: Boolean,
    onPasswordVisibilityToggle: () -> Unit,
    confirmPasswordVisible: Boolean,
    onConfirmPasswordVisibilityToggle: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Información básica",
            style = MaterialTheme.typography.headlineSmall,  // Hammersmith One
            color = BioWayColors.BrandDarkGreen,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Nombre
        RegisterTextField(
            value = name,
            onValueChange = onNameChange,
            label = "Nombre completo",
            leadingIcon = Icons.Default.Person
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Email
        RegisterTextField(
            value = email,
            onValueChange = onEmailChange,
            label = "Correo electrónico",
            leadingIcon = Icons.Default.Email,
            keyboardType = KeyboardType.Email
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Teléfono
        RegisterTextField(
            value = phone,
            onValueChange = onPhoneChange,
            label = "Teléfono",
            leadingIcon = Icons.Default.Phone,
            keyboardType = KeyboardType.Phone
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Contraseña
        RegisterTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = "Contraseña",
            leadingIcon = Icons.Default.Lock,
            isPassword = true,
            passwordVisible = passwordVisible,
            onPasswordVisibilityToggle = onPasswordVisibilityToggle
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Confirmar contraseña
        RegisterTextField(
            value = confirmPassword,
            onValueChange = onConfirmPasswordChange,
            label = "Confirmar contraseña",
            leadingIcon = Icons.Default.Lock,
            isPassword = true,
            passwordVisible = confirmPasswordVisible,
            onPasswordVisibilityToggle = onConfirmPasswordVisibilityToggle
        )
    }
}

@Composable
private fun RegisterTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onPasswordVisibilityToggle: (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(label, style = MaterialTheme.typography.bodyMedium)
        },
        leadingIcon = {
            Icon(
                imageVector = leadingIcon,
                contentDescription = label,
                tint = if (value.isNotEmpty()) BioWayColors.BrandDarkGreen else Color(0xFF2E7D6C)
            )
        },
        trailingIcon = if (isPassword && onPasswordVisibilityToggle != null) {
            {
                IconButton(onClick = onPasswordVisibilityToggle) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (passwordVisible) "Ocultar" else "Mostrar",
                        tint = Color(0xFF2E7D6C)
                    )
                }
            }
        } else null,
        visualTransformation = if (isPassword && !passwordVisible)
            PasswordVisualTransformation()
        else
            VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White.copy(alpha = 0.75f),  // Más legible
            unfocusedContainerColor = Color.White.copy(alpha = 0.55f),  // Más visible
            focusedBorderColor = BioWayColors.BrandDarkGreen,
            unfocusedBorderColor = BioWayColors.BrandDarkGreen.copy(alpha = 0.5f),
            focusedLabelColor = BioWayColors.BrandDarkGreen,
            unfocusedLabelColor = Color(0xFF2E7D6C),
            focusedTextColor = BioWayColors.BrandDarkGreen,
            unfocusedTextColor = Color(0xFF2E7D6C),
            cursorColor = BioWayColors.BrandDarkGreen
        ),
        singleLine = true
    )
}

@Composable
private fun TermsAndConditions(
    accepted: Boolean,
    onAcceptedChange: (Boolean) -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Términos y condiciones",
            style = MaterialTheme.typography.headlineSmall,  // Hammersmith One
            color = BioWayColors.BrandDarkGreen,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Card con términos
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.White.copy(alpha = 0.4f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text(
                    text = "Términos y Condiciones de BioWay\n\n" +
                           "1. Aceptación de términos\n" +
                           "Al utilizar BioWay, aceptas cumplir con estos términos y condiciones.\n\n" +
                           "2. Uso del servicio\n" +
                           "BioWay es una plataforma de reciclaje que conecta ciudadanos con recolectores.\n\n" +
                           "3. Privacidad\n" +
                           "Tus datos serán tratados conforme a nuestra política de privacidad.\n\n" +
                           "4. Responsabilidades\n" +
                           "Los usuarios se comprometen a usar la plataforma de manera responsable.",
                    style = MaterialTheme.typography.bodySmall,  // Montserrat
                    color = Color(0xFF2E7D6C),
                    lineHeight = 20.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Checkbox de aceptación
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onAcceptedChange(!accepted) }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = accepted,
                onCheckedChange = onAcceptedChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFF70D162),
                    uncheckedColor = Color(0xFF2E7D6C),
                    checkmarkColor = Color.White
                )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Acepto los términos y condiciones",
                style = MaterialTheme.typography.bodyMedium,  // Montserrat
                color = BioWayColors.BrandDarkGreen
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Link a política de privacidad
        TextButton(
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://bioway.com.mx/bioway-politica"))
                context.startActivity(intent)
            }
        ) {
            Text(
                text = "Ver Política de Privacidad",
                style = MaterialTheme.typography.bodyMedium,  // Montserrat
                color = Color(0xFF2E7D6C),
                fontWeight = FontWeight.Medium
            )
        }
    }
}
