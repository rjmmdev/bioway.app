package com.biowaymexico.ui.screens.brindador

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biowaymexico.ui.theme.BioWayColors
import kotlinx.coroutines.launch

/**
 * Pantalla de Perfil y Competencias - Diseño Moderno Minimalista 2024
 * Aplica gamificación y estándar visual de BioWay
 * Referencias: Fitbit, LinkedIn, tendencias 2024
 */
@Composable
fun BrindadorPerfilCompetenciasScreen(
    onNavigateToImpactoAmbiental: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) }
    var emailVerificado by remember { mutableStateOf(false) }
    var showVerificationDialog by remember { mutableStateOf(false) }
    var isLoadingVerification by remember { mutableStateOf(false) }
    var verificationMessage by remember { mutableStateOf<String?>(null) }

    val authRepository = remember { com.biowaymexico.data.AuthRepository() }
    val brindadorRepository = remember { com.biowaymexico.data.BrindadorRepository() }
    val scope = rememberCoroutineScope()

    var brindadorData by remember { mutableStateOf<com.biowaymexico.data.models.BrindadorModel?>(null) }

    // Cargar datos del brindador y verificar email
    LaunchedEffect(Unit) {
        authRepository.getCurrentUser()?.let { user ->
            emailVerificado = user.isEmailVerified
        }

        scope.launch {
            val result = brindadorRepository.obtenerBrindador()
            if (result.isSuccess) {
                brindadorData = result.getOrNull()
            }
        }
    }

    var co2Calculado by remember { mutableStateOf(0.0) }
    val materialesRepository = remember { com.biowaymexico.data.MaterialesRepository() }

    // Calcular CO2 en tiempo real cuando cambian los datos
    LaunchedEffect(brindadorData) {
        val currentData = brindadorData
        if (currentData != null) {
            scope.launch {
                val materialesResult = materialesRepository.obtenerMateriales()
                if (materialesResult.isSuccess) {
                    val catalogo = materialesResult.getOrNull() ?: emptyList()
                    var co2Total = 0.0

                    currentData.materialesReciclados.forEach { (materialId, cantidad) ->
                        val material = catalogo.find { it.id == materialId }
                        if (material != null) {
                            co2Total += cantidad * material.factorCO2
                        }
                    }

                    co2Calculado = co2Total
                }
            }
        }
    }

    val displayUser = brindadorData?.let {
        BioWayUser(
            nombre = it.nombre,
            colonia = "",
            municipio = "",
            nivel = it.nivel,
            bioCoins = it.bioCoins,
            totalKgReciclados = it.totalKgReciclados,
            totalCO2Evitado = co2Calculado,
            posicionRanking = 0,
            bioImpulso = it.bioImpulso,
            bioImpulsoActivo = it.bioImpulsoActivo
        )
    } ?: BioWayUser(
        nombre = "Cargando...",
        colonia = "",
        municipio = "",
        nivel = "Bronce",
        bioCoins = 0,
        totalKgReciclados = 0.0,
        totalCO2Evitado = 0.0,
        posicionRanking = 0,
        bioImpulso = 1,
        bioImpulsoActivo = false
    )

    Scaffold(
        containerColor = Color(0xFFFAFAFA)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            PerfilHeader(mockUser = displayUser)

            // Banner de verificación
            if (!emailVerificado) {
                EmailVerificationBanner(
                    onVerifyClick = { showVerificationDialog = true }
                )
            }

            NavigationTabs(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )

            when (selectedTab) {
                0 -> PerfilView(
                    mockUser = displayUser,
                    onNavigateToImpactoAmbiental = onNavigateToImpactoAmbiental,
                    onLogout = onLogout,
                    authRepository = authRepository
                )
                1 -> RankingView(mockUser = displayUser)
                2 -> LogrosView()
            }
        }

        // Diálogo de verificación
        if (showVerificationDialog) {
            AlertDialog(
                onDismissRequest = { showVerificationDialog = false },
                title = {
                    Text(
                        text = "Verificación de Email",
                        style = MaterialTheme.typography.titleLarge,
                        color = BioWayColors.BrandDarkGreen
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Para verificar tu email:\n\n" +
                                   "1. Revisa tu bandeja de entrada\n" +
                                   "2. Busca el correo de BioWay\n" +
                                   "3. Haz clic en el enlace de verificación\n" +
                                   "4. Si no lo encuentras, revisa SPAM/Correo no deseado\n\n" +
                                   "⚠️ Si no verificas tu email en 10 días, tu cuenta será eliminada automáticamente.",
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 20.sp
                        )

                        if (verificationMessage != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = verificationMessage!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF70D162),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            scope.launch {
                                isLoadingVerification = true
                                val result = authRepository.reenviarEmailVerificacion()
                                isLoadingVerification = false

                                if (result.isSuccess) {
                                    verificationMessage = "✅ Email enviado correctamente"
                                } else {
                                    verificationMessage = "❌ Error al enviar email"
                                }
                            }
                        },
                        enabled = !isLoadingVerification,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF70D162)
                        )
                    ) {
                        if (isLoadingVerification) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Reenviar Email")
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showVerificationDialog = false }) {
                        Text("Cerrar", color = Color(0xFF70D162))
                    }
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(24.dp)
            )
        }
    }
}

@Composable
private fun EmailVerificationBanner(onVerifyClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFFFF3CD),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Advertencia",
                tint = Color(0xFFFF9800),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Email no verificado",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF856404),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Tu cuenta será eliminada en 10 días",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF856404),
                    fontSize = 11.sp
                )
            }

            TextButton(onClick = onVerifyClick) {
                Text(
                    "Verificar",
                    color = Color(0xFF70D162),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PerfilHeader(mockUser: BioWayUser) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 0.dp, bottom = 8.dp),  // Misma altura que Dashboard
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = mockUser.nombre,
                style = MaterialTheme.typography.headlineMedium,  // Hammersmith One 28sp
                color = BioWayColors.BrandDarkGreen
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color(0xFF2E7D6C),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${mockUser.colonia}, ${mockUser.municipio}",
                    style = MaterialTheme.typography.bodySmall,  // Montserrat
                    color = Color(0xFF2E7D6C)
                )
            }
        }

        // Badge de nivel
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = getNivelColor(mockUser.nivel)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = getNivelIcon(mockUser.nivel),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = mockUser.nivel,
                    style = MaterialTheme.typography.labelLarge,  // Montserrat Medium
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun NavigationTabs(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabs = listOf("Perfil", "Ranking", "Logros")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tabs.forEachIndexed { index, tab ->
            Button(
                onClick = { onTabSelected(index) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (index == selectedTab)
                        Color(0xFF70D162)
                    else
                        Color.White,
                    contentColor = if (index == selectedTab)
                        Color.White
                    else
                        Color(0xFF2E7D6C)
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = if (index == selectedTab) 3.dp else 1.dp
                ),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Text(
                    text = tab,
                    style = MaterialTheme.typography.labelLarge,  // Montserrat Medium
                    fontWeight = if (index == selectedTab) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun PerfilView(
    mockUser: BioWayUser,
    onNavigateToImpactoAmbiental: () -> Unit,
    onLogout: () -> Unit,
    authRepository: com.biowaymexico.data.AuthRepository
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Stats principales (clickeables para ver detalle)
            StatsCards(
                mockUser = mockUser,
                onNavigateToImpactoAmbiental = onNavigateToImpactoAmbiental
            )
        }

        item {
            // Impacto ambiental (clickeable para ver detalle)
            ImpactoAmbientalCard(
                mockUser = mockUser,
                onClick = onNavigateToImpactoAmbiental
            )
        }

        item {
            // BioImpulso
            BioImpulsoCard(mockUser = mockUser)
        }

        item {
            // Configuración
            ConfiguracionSection(
                onLogout = onLogout,
                authRepository = authRepository
            )
        }
    }
}

@Composable
private fun StatsCards(
    mockUser: BioWayUser,
    onNavigateToImpactoAmbiental: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // BioCoins - NO clickeable
        StatCard(
            icon = Icons.Default.MonetizationOn,
            valor = "${mockUser.bioCoins}",
            label = "BioCoins",
            color = Color(0xFF70D162),
            modifier = Modifier.weight(1f),
            onClick = null
        )

        // Kg Reciclados - CLICKEABLE para ver impacto
        StatCard(
            icon = Icons.Default.Recycling,
            valor = "${mockUser.totalKgReciclados}kg",
            label = "Reciclados",
            color = BioWayColors.BrandGreen,
            modifier = Modifier.weight(1f),
            onClick = onNavigateToImpactoAmbiental  // ⭐ Clickeable
        )
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    valor: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null  // ⭐ Opcional clickeable
) {
    Surface(
        onClick = onClick ?: {},  // Si no hay onClick, no hace nada
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 3.dp,
        enabled = onClick != null  // Solo habilitado si es clickeable
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(
                        color = color.copy(alpha = 0.15f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = valor,
                style = MaterialTheme.typography.headlineSmall,  // Hammersmith One
                color = BioWayColors.BrandDarkGreen
            )

            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,  // Montserrat
                color = Color(0xFF2E7D6C)
            )
        }
    }
}

@Composable
private fun ImpactoAmbientalCard(
    mockUser: BioWayUser,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,  // ⭐ Clickeable
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Eco,
                    contentDescription = null,
                    tint = BioWayColors.BrandGreen,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Impacto Ambiental",
                    style = MaterialTheme.typography.titleMedium,  // Hammersmith One
                    color = BioWayColors.BrandDarkGreen
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // CO2 Evitado
            ImpactStat(
                icon = Icons.Default.Cloud,
                label = "CO₂ Evitado",
                valor = "${mockUser.totalCO2Evitado} kg",
                color = BioWayColors.BrandBlue
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Árboles equivalentes (calculado)
            val arbolesEquivalentes = (mockUser.totalCO2Evitado / 21).toInt()
            ImpactStat(
                icon = Icons.Default.Park,
                label = "Equivale a plantar",
                valor = "$arbolesEquivalentes árboles",
                color = BioWayColors.BrandGreen
            )
        }
    }
}

@Composable
private fun ImpactStat(
    icon: ImageVector,
    label: String,
    valor: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = color.copy(alpha = 0.15f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,  // Montserrat
                color = Color(0xFF2E7D6C)
            )
            Text(
                text = valor,
                style = MaterialTheme.typography.titleMedium,  // Hammersmith One
                color = BioWayColors.BrandDarkGreen
            )
        }
    }
}

@Composable
private fun BioImpulsoCard(mockUser: BioWayUser) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                BioWayColors.BrandGreen,
                                BioWayColors.BrandTurquoise
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "BioImpulso",
                    style = MaterialTheme.typography.titleMedium,  // Hammersmith One
                    color = BioWayColors.BrandDarkGreen
                )
                Text(
                    text = if (mockUser.bioImpulsoActivo) "Activo" else "Inactivo",
                    style = MaterialTheme.typography.bodySmall,  // Montserrat
                    color = if (mockUser.bioImpulsoActivo)
                        BioWayColors.BrandGreen
                    else
                        Color(0xFF999999)
                )
            }

            Text(
                text = "x${mockUser.bioImpulso}",
                style = MaterialTheme.typography.headlineSmall,  // Hammersmith One
                color = Color(0xFF70D162),
                fontSize = 24.sp
            )
        }
    }
}

@Composable
private fun ConfiguracionSection(
    onLogout: () -> Unit,
    authRepository: com.biowaymexico.data.AuthRepository
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ConfigMenuItem(
            icon = Icons.Default.Person,
            texto = "Editar perfil",
            onClick = { /* TODO */ }
        )
        ConfigMenuItem(
            icon = Icons.Default.Notifications,
            texto = "Notificaciones",
            onClick = { /* TODO */ }
        )
        ConfigMenuItem(
            icon = Icons.Default.Security,
            texto = "Privacidad",
            onClick = { /* TODO */ }
        )
        ConfigMenuItem(
            icon = Icons.Default.Logout,
            texto = "Cerrar sesión",
            onClick = {
                authRepository.logout()
                onLogout()
            },
            isDestructive = true
        )
    }
}

@Composable
private fun ConfigMenuItem(
    icon: ImageVector,
    texto: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDestructive) Color(0xFFE53935) else Color(0xFF2E7D6C),
                modifier = Modifier.size(22.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = texto,
                style = MaterialTheme.typography.bodyMedium,  // Montserrat
                color = if (isDestructive)
                    Color(0xFFE53935)
                else
                    BioWayColors.BrandDarkGreen,
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF2E7D6C).copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun RankingView(mockUser: BioWayUser) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Tu posición
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 3.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Tu posición",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF2E7D6C)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "#${mockUser.posicionRanking}",
                        style = MaterialTheme.typography.displayMedium,  // Hammersmith One 45sp
                        color = Color(0xFF70D162)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "en tu zona",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF2E7D6C)
                    )
                }
            }
        }

        item {
            Text(
                text = "Top 10",
                style = MaterialTheme.typography.titleMedium,
                color = BioWayColors.BrandDarkGreen,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Top 10 usuarios
        items(getMockRanking()) { usuario ->
            RankingItem(usuario = usuario, currentUserId = mockUser.nombre)
        }
    }
}

@Composable
private fun RankingItem(usuario: RankingUser, currentUserId: String) {
    val isCurrentUser = usuario.nombre == currentUserId

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (isCurrentUser)
            BioWayColors.BrandGreen.copy(alpha = 0.1f)
        else
            Color.White,
        shadowElevation = if (isCurrentUser) 3.dp else 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Posición
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = when (usuario.posicion) {
                            1 -> Color(0xFFFFD700)  // Oro
                            2 -> Color(0xFFC0C0C0)  // Plata
                            3 -> Color(0xFFCD7F32)  // Bronce
                            else -> Color(0xFF2E7D6C).copy(alpha = 0.15f)
                        }.copy(alpha = 0.2f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${usuario.posicion}",
                    style = MaterialTheme.typography.titleSmall,
                    color = when (usuario.posicion) {
                        1, 2, 3 -> Color(0xFF70D162)
                        else -> Color(0xFF2E7D6C)
                    },
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = usuario.nombre,
                    style = MaterialTheme.typography.bodyMedium,
                    color = BioWayColors.BrandDarkGreen,
                    fontWeight = if (isCurrentUser) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    text = "${usuario.kgReciclados} kg",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF2E7D6C)
                )
            }

            Text(
                text = "${usuario.puntos}",
                style = MaterialTheme.typography.titleSmall,
                color = Color(0xFF70D162)
            )
        }
    }
}

@Composable
private fun LogrosView() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Tus logros",
                style = MaterialTheme.typography.titleLarge,
                color = BioWayColors.BrandDarkGreen
            )
        }

        items(getMockLogros()) { logro ->
            LogroCard(logro = logro)
        }
    }
}

@Composable
private fun LogroCard(logro: Logro) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = if (logro.desbloqueado) 3.dp else 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(
                        color = if (logro.desbloqueado)
                            Color(0xFF70D162).copy(alpha = 0.15f)
                        else
                            Color(0xFF999999).copy(alpha = 0.1f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = logro.icono,
                    contentDescription = null,
                    tint = if (logro.desbloqueado)
                        Color(0xFF70D162)
                    else
                        Color(0xFF999999),
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = logro.nombre,
                    style = MaterialTheme.typography.titleSmall,  // Hammersmith One
                    color = if (logro.desbloqueado)
                        BioWayColors.BrandDarkGreen
                    else
                        Color(0xFF999999)
                )
                Text(
                    text = logro.descripcion,
                    style = MaterialTheme.typography.bodySmall,  // Montserrat
                    color = Color(0xFF2E7D6C),
                    fontSize = 11.sp
                )

                if (logro.progreso < 100 && !logro.desbloqueado) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { logro.progreso / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = BioWayColors.BrandGreen,
                        trackColor = Color(0xFFE0E0E0)
                    )
                }
            }

            if (logro.desbloqueado) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Desbloqueado",
                    tint = Color(0xFF70D162),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// Modelos de datos
data class BioWayUser(
    val nombre: String,
    val colonia: String,
    val municipio: String,
    val nivel: String,
    val bioCoins: Int,
    val totalKgReciclados: Double,
    val totalCO2Evitado: Double,
    val posicionRanking: Int,
    val bioImpulso: Int,
    val bioImpulsoActivo: Boolean
)

data class RankingUser(
    val posicion: Int,
    val nombre: String,
    val kgReciclados: Double,
    val puntos: Int
)

data class Logro(
    val id: Int,
    val nombre: String,
    val descripcion: String,
    val icono: ImageVector,
    val desbloqueado: Boolean,
    val progreso: Int = 100
)

private fun getNivelColor(nivel: String): Color {
    return when (nivel.lowercase()) {
        "oro" -> Color(0xFFFFD700)
        "plata" -> Color(0xFFC0C0C0)
        "bronce" -> Color(0xFFCD7F32)
        else -> Color(0xFF70D162)
    }
}

private fun getNivelIcon(nivel: String): ImageVector {
    return when (nivel.lowercase()) {
        "oro", "plata", "bronce" -> Icons.Default.EmojiEvents
        else -> Icons.Default.Star
    }
}

private fun getMockRanking(): List<RankingUser> {
    return listOf(
        RankingUser(1, "María González", 450.0, 2500),
        RankingUser(2, "Carlos Rodríguez", 380.5, 2100),
        RankingUser(3, "Ana Martínez", 320.0, 1850),
        RankingUser(4, "Pedro López", 290.5, 1620),
        RankingUser(5, "Laura Sánchez", 275.0, 1500),
        RankingUser(6, "José Hernández", 260.5, 1420),
        RankingUser(7, "Juan Pérez", 245.5, 1350),
        RankingUser(8, "Carmen Díaz", 230.0, 1280),
        RankingUser(9, "Roberto García", 215.5, 1200),
        RankingUser(10, "Patricia Torres", 200.0, 1150)
    )
}

private fun getMockLogros(): List<Logro> {
    return listOf(
        Logro(1, "Primer Paso", "Recicla tu primer material", Icons.Default.Flag, true),
        Logro(2, "Eco Guerrero", "Recicla 100 kg de materiales", Icons.Default.EmojiEvents, true),
        Logro(3, "Racha de 7 días", "Recicla 7 días seguidos", Icons.Default.LocalFireDepartment, false, 60),
        Logro(4, "Guardián Verde", "Evita 500 kg de CO₂", Icons.Default.Eco, false, 40),
        Logro(5, "Influencer Verde", "Invita a 5 amigos", Icons.Default.People, false, 20),
        Logro(6, "Maestro Reciclador", "Recicla en todas las categorías", Icons.Default.School, true),
        Logro(7, "100% Comprometido", "Completa tu perfil al 100%", Icons.Default.AccountCircle, false, 75)
    )
}
