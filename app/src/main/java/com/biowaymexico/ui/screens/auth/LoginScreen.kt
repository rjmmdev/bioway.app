package com.biowaymexico.ui.screens.auth

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biowaymexico.R
import com.biowaymexico.data.AuthRepository
import com.biowaymexico.data.FirestoreDebugger
import com.biowaymexico.ui.theme.BioWayColors
import com.biowaymexico.ui.navigation.UserType
import kotlinx.coroutines.launch

/**
 * Pantalla de Login - Diseño Moderno Minimalista 2024
 * Tendencias aplicadas: Glassmorphism, degradados vibrantes, micro-animaciones
 */
@Composable
fun LoginScreen(
    onLoginSuccess: (UserType) -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authRepository = remember { AuthRepository() }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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

    val animatedOffset by animateFloatAsState(
        targetValue = if (isVisible) 0f else 50f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "offset"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                // Degradado del estándar visual 2024
                brush = Brush.verticalGradient(
                    colors = listOf(
                        BioWayColors.BrandGreen,      // #75ee8a
                        BioWayColors.BrandTurquoise,  // #b3fcd4
                        BioWayColors.BrandBlue        // #00dfff
                    )
                )
            )
    ) {
        // Círculos decorativos de fondo con blur
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(100.dp)
        ) {
            // Círculo superior
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .offset(x = (-50).dp, y = (-100).dp)
                    .background(
                        color = BioWayColors.BrandBlue.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(50)
                    )
            )

            // Círculo inferior
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
                .offset(y = animatedOffset.dp)
                .alpha(animatedAlpha),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(100.dp))

            // Logo BioWay
            Image(
                painter = painterResource(id = R.drawable.ic_bioway_logo),
                contentDescription = "BioWay Logo",
                modifier = Modifier.size(100.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Título con Hammersmith One
            Text(
                text = "BioWay",
                style = MaterialTheme.typography.displayMedium,  // Hammersmith One - más grande (45sp)
                color = BioWayColors.BrandDarkGreen,  // Verde oscuro del estándar visual (#007565)
                letterSpacing = 0.sp  // Sin espacio extra entre letras
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Subtítulo con Montserrat - más cerca del card
            Text(
                text = "¡Bienvenido de vuelta!",
                style = MaterialTheme.typography.bodyLarge,  // Montserrat
                color = BioWayColors.BrandDarkGreen  // #007565 - Mismo que el logo
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Card glassmorphism para el formulario
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                color = Color.White.copy(alpha = 0.75f),  // Más opaco para mejor legibilidad
                shadowElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),  // Reducido de 32dp a 28dp
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Email Field con estilo moderno
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = {
                            Text(
                                "Correo electrónico",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
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

                    Spacer(modifier = Modifier.height(20.dp))

                    // Password Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = {
                            Text(
                                "Contraseña",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Ocultar" else "Mostrar",
                                    tint = Color(0xFF2E7D6C)
                                )
                            }
                        },
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

                    Spacer(modifier = Modifier.height(32.dp))

                    // Botón Login con degradado
                    Button(
                        onClick = {
                            isLoading = true
                            errorMessage = null

                            scope.launch {
                                try {
                                    val result = authRepository.login(email, password)

                                    if (result.isSuccess) {
                                        val (user, tipoUsuario) = result.getOrNull()!!

                                        // Si es maestro, imprimir estructura de Firebase
                                        if (tipoUsuario == "Maestro") {
                                            Log.d("LOGIN", "🔐 Maestro detectado - Imprimiendo Firebase...")
                                            FirestoreDebugger.imprimirTodasLasColecciones()
                                        }

                                        // Navegar según tipo de usuario
                                        val userType = when (tipoUsuario) {
                                            "Maestro" -> UserType.MAESTRO
                                            "Recolector" -> UserType.RECOLECTOR
                                            "CentroAcopio" -> UserType.CENTRO_ACOPIO
                                            "BoteBioWay" -> UserType.BOTE_BIOWAY
                                            else -> UserType.BRINDADOR
                                        }
                                        onLoginSuccess(userType)
                                    } else {
                                        // Error de login
                                        errorMessage = result.exceptionOrNull()?.message ?: "Error al iniciar sesión"
                                        isLoading = false
                                    }
                                } catch (e: Exception) {
                                    errorMessage = e.message ?: "Error desconocido"
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
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
                            if (isLoading) {
                                CircularProgressIndicator(
                                    color = Color(0xFF70D162),  // #70d162
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 3.dp
                                )
                            } else {
                                Text(
                                    text = "Iniciar Sesión",
                                    style = MaterialTheme.typography.titleMedium,  // Hammersmith One
                                    color = Color(0xFF70D162),  // #70d162 - Verde del logo SVG
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Link "Olvidé mi contraseña"
                    TextButton(
                        onClick = { /* TODO */ }
                    ) {
                        Text(
                            text = "¿Olvidaste tu contraseña?",
                            style = MaterialTheme.typography.bodyMedium,  // Montserrat
                            color = Color(0xFF2E7D6C),  // Verde oscuro harmonioso
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Sección de registro
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "¿No tienes cuenta?",
                    style = MaterialTheme.typography.bodyMedium,  // Montserrat
                    color = Color.White.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Regístrate",
                    style = MaterialTheme.typography.bodyMedium,  // Montserrat
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onNavigateToRegister() }
                )
            }

            Spacer(modifier = Modifier.height(60.dp))

            // Divisor visual
            Row(
                modifier = Modifier.fillMaxWidth(0.8f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Divider(
                    modifier = Modifier.weight(1f),
                    color = Color.White.copy(alpha = 0.3f),
                    thickness = 1.dp
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Acceso rápido (Demo)",
                    style = MaterialTheme.typography.bodySmall,  // Montserrat
                    color = Color.White.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Divider(
                    modifier = Modifier.weight(1f),
                    color = Color.White.copy(alpha = 0.3f),
                    thickness = 1.dp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Botones de acceso rápido para testing (glassmorphism)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickAccessButton(
                    text = "Acceso Brindador",
                    onClick = { onLoginSuccess(UserType.BRINDADOR) }
                )
                QuickAccessButton(
                    text = "Acceso Recolector",
                    onClick = { onLoginSuccess(UserType.RECOLECTOR) }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickAccessButton(
                        text = "Centro Acopio",
                        onClick = { onLoginSuccess(UserType.CENTRO_ACOPIO) },
                        modifier = Modifier.weight(1f)
                    )
                    QuickAccessButton(
                        text = "Admin",
                        onClick = { onLoginSuccess(UserType.MAESTRO) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun QuickAccessButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.25f),  // Glassmorphism más visible
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,  // Montserrat Medium
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}
