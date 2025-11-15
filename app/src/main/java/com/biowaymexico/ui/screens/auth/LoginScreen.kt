package com.biowaymexico.ui.screens.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biowaymexico.ui.theme.BioWayColors
import com.biowaymexico.ui.navigation.UserType

/**
 * Pantalla de Login - Diseño Moderno y Minimalista
 */
@Composable
fun LoginScreen(
    onLoginSuccess: (UserType) -> Unit,
    onNavigateToRegister: () -> Unit
) {
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
        animationSpec = tween(durationMillis = 600),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp)
                .alpha(animatedAlpha),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(80.dp))

            // Logo minimalista
            Text(
                text = "🌱",
                fontSize = 64.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "BioWay",
                fontSize = 36.sp,
                fontWeight = FontWeight.Light,
                color = BioWayColors.DarkGreen,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Bienvenido de vuelta",
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF888888)
            )

            Spacer(modifier = Modifier.height(64.dp))

            // Email Field
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    errorMessage = null
                },
                label = { Text("Email", fontSize = 14.sp) },
                placeholder = { Text("tu@email.com", fontSize = 14.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "Email",
                        tint = BioWayColors.PrimaryGreen
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BioWayColors.PrimaryGreen,
                    unfocusedBorderColor = Color(0xFFE0E0E0),
                    focusedLabelColor = BioWayColors.PrimaryGreen,
                    cursorColor = BioWayColors.PrimaryGreen
                ),
                singleLine = true,
                isError = errorMessage != null
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Password Field
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    errorMessage = null
                },
                label = { Text("Contraseña", fontSize = 14.sp) },
                placeholder = { Text("••••••••", fontSize = 14.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Password",
                        tint = BioWayColors.PrimaryGreen
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) "Ocultar" else "Mostrar",
                            tint = Color(0xFF888888)
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BioWayColors.PrimaryGreen,
                    unfocusedBorderColor = Color(0xFFE0E0E0),
                    focusedLabelColor = BioWayColors.PrimaryGreen,
                    cursorColor = BioWayColors.PrimaryGreen
                ),
                singleLine = true,
                isError = errorMessage != null
            )

            // Error message
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Olvidaste contraseña
            Text(
                text = "¿Olvidaste tu contraseña?",
                fontSize = 13.sp,
                color = BioWayColors.PrimaryGreen,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .align(Alignment.End)
                    .clickable { /* TODO */ }
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Botón de login
            Button(
                onClick = {
                    if (email.isNotEmpty() && password.isNotEmpty()) {
                        isLoading = true
                        val userType = when {
                            email.contains("recolector") -> UserType.RECOLECTOR
                            email.contains("maestro") || email.contains("admin") -> UserType.MAESTRO
                            email.contains("centro") -> UserType.CENTRO_ACOPIO
                            else -> UserType.BRINDADOR
                        }
                        onLoginSuccess(userType)
                    } else {
                        errorMessage = "Por favor completa todos los campos"
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BioWayColors.PrimaryGreen,
                    contentColor = Color.White
                ),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Iniciar Sesión",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Divider con texto
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Divider(
                    modifier = Modifier.weight(1f),
                    color = Color(0xFFE0E0E0)
                )
                Text(
                    text = "  Acceso rápido  ",
                    fontSize = 12.sp,
                    color = Color(0xFF888888)
                )
                Divider(
                    modifier = Modifier.weight(1f),
                    color = Color(0xFFE0E0E0)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Botones de acceso rápido
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { onLoginSuccess(UserType.BRINDADOR) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = BioWayColors.PrimaryGreen
                    )
                ) {
                    Text("Ciudadano", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = { onLoginSuccess(UserType.RECOLECTOR) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = BioWayColors.PrimaryGreen
                    )
                ) {
                    Text("Recolector", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { onLoginSuccess(UserType.CENTRO_ACOPIO) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = BioWayColors.PrimaryGreen
                    )
                ) {
                    Text("Centro", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = { onLoginSuccess(UserType.MAESTRO) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = BioWayColors.PrimaryGreen
                    )
                ) {
                    Text("Admin", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Registro
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "¿No tienes cuenta? ",
                    color = Color(0xFF888888),
                    fontSize = 14.sp
                )
                Text(
                    text = "Regístrate",
                    color = BioWayColors.PrimaryGreen,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onNavigateToRegister() }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
