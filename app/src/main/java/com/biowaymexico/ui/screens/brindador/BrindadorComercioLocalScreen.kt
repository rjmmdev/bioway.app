package com.biowaymexico.ui.screens.brindador

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biowaymexico.ui.theme.BioWayColors

/**
 * Pantalla de Comercio Local del Brindador
 * Replicado fielmente del diseño Flutter original
 */
@Composable
fun BrindadorComercioLocalScreen() {
    var searchText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Todos") }
    var showFilters by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }

    val userBioCoins = 1250
    val productos = remember { getMockProductos() }
    val comercios = remember { getMockComercios() }

    Scaffold(
        containerColor = Color(0xFFF8F9FA)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item {
                // Header
                BuildComercioHeader(userBioCoins = userBioCoins)
            }

            item {
                // Barra de búsqueda
                BuildSearchBar(
                    searchText = searchText,
                    onSearchTextChange = { searchText = it },
                    isSearching = isSearching,
                    onSearchFocusChange = { isSearching = it },
                    showFilters = showFilters,
                    onFilterClick = { showFilters = !showFilters }
                )
            }

            item {
                // Categorías
                Spacer(modifier = Modifier.height(16.dp))
                CategoryChips(
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it }
                )
            }

            if (!isSearching && !showFilters) {
                item {
                    // Productos destacados
                    Spacer(modifier = Modifier.height(20.dp))
                    FeaturedProducts(productos = productos.filter { it.destacado })
                }
            }

            if (showFilters) {
                item {
                    // Filtros expandibles
                    Spacer(modifier = Modifier.height(16.dp))
                    ExpandedFilters()
                }
            }

            item {
                // Lista de comercios
                Spacer(modifier = Modifier.height(20.dp))
                ComerciosList(comercios = comercios, productos = productos)
            }
        }
    }
}

@Composable
private fun BuildComercioHeader(userBioCoins: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 5.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "Comercio Local",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF007460)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Descuentos exclusivos para ti",
                        fontSize = 14.sp,
                        color = Color(0xFF999999)
                    )
                }

                // Balance card
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Transparent,
                    shadowElevation = 4.dp
                ) {
                    Box(
                        modifier = Modifier.background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    BioWayColors.MediumGreen,
                                    BioWayColors.AquaGreen
                                )
                            )
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = Color(0xFF00553F),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = userBioCoins.toString(),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00553F),
                                    lineHeight = 20.sp
                                )
                                Text(
                                    text = "BioCoins",
                                    fontSize = 11.sp,
                                    color = Color(0xFF00553F),
                                    lineHeight = 11.sp
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
private fun BuildSearchBar(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    isSearching: Boolean,
    onSearchFocusChange: (Boolean) -> Unit,
    showFilters: Boolean,
    onFilterClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF5F5F5),
        border = BorderStroke(
            width = 2.dp,
            color = if (isSearching) BioWayColors.NavGreen else Color.Transparent
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = if (isSearching) BioWayColors.NavGreen else Color(0xFFBBBBBB),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))

            Box(modifier = Modifier.weight(1f)) {
                if (searchText.isEmpty()) {
                    Text(
                        text = "Buscar tiendas, restaurantes...",
                        fontSize = 15.sp,
                        color = Color(0xFF999999)
                    )
                }
                BasicTextField(
                    value = searchText,
                    onValueChange = onSearchTextChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 15.sp,
                        color = Color(0xFF1A1A1A)
                    )
                )
            }

            if (searchText.isNotEmpty()) {
                IconButton(onClick = { onSearchTextChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Limpiar",
                        tint = Color(0xFFBBBBBB),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Botón de filtros
            Surface(
                shape = CircleShape,
                color = if (showFilters) BioWayColors.NavGreen else Color.Transparent
            ) {
                IconButton(onClick = onFilterClick) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Filtros",
                        tint = if (showFilters) Color.White else Color(0xFF666666)
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryChips(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    val categories = listOf(
        "Todos" to Icons.Default.Apps,
        "Cafetería" to Icons.Default.Coffee,
        "Restaurante" to Icons.Default.Restaurant,
        "Supermercado" to Icons.Default.ShoppingCart,
        "Deportes" to Icons.Default.SportsBasketball,
        "Salud" to Icons.Default.MedicalServices
    )

    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { (label, icon) ->
            val isSelected = selectedCategory == label
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) BioWayColors.NavGreen else Color.White,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) BioWayColors.NavGreen else Color(0xFFDDDDDD)
                ),
                modifier = Modifier.clickable { onCategorySelected(label) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) Color.White else Color(0xFF666666),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = label,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) Color.White else Color(0xFF555555)
                    )
                }
            }
        }
    }
}

@Composable
private fun FeaturedProducts(productos: List<Producto>) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🔥 Ofertas del día",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF007460)
            )
            TextButton(onClick = { /* Ver todas */ }) {
                Text(
                    text = "Ver todas",
                    color = BioWayColors.NavGreen,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(productos) { producto ->
                FeaturedProductCard(producto = producto)
            }
        }
    }
}

@Composable
private fun FeaturedProductCard(producto: Producto) {
    Surface(
        modifier = Modifier.width(300.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 10.dp
    ) {
        Box {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = BioWayColors.NavGreen.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = producto.icono,
                            fontSize = 24.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = producto.nombre,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2C3E50),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = producto.comercioNombre,
                            fontSize = 13.sp,
                            color = Color(0xFF999999),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${producto.bioCoins} BioCoins",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = BioWayColors.NavGreen
                        )
                        Text(
                            text = "Ahorra ${producto.descuento}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF4CAF50)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = BioWayColors.NavGreen
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .padding(10.dp)
                                .size(20.dp)
                        )
                    }
                }
            }

            // Badge de descuento
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFFF5252)
            ) {
                Text(
                    text = "-${producto.descuento}%",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun ExpandedFilters() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Filtrar por ubicación",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF007460)
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Estado dropdown
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF5F5F5)
                ) {
                    Text(
                        text = "Todos los estados",
                        modifier = Modifier.padding(12.dp),
                        fontSize = 14.sp,
                        color = Color(0xFF666666)
                    )
                }

                // Municipio dropdown
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF5F5F5)
                ) {
                    Text(
                        text = "Todos los municipios",
                        modifier = Modifier.padding(12.dp),
                        fontSize = 14.sp,
                        color = Color(0xFF666666)
                    )
                }
            }
        }
    }
}

@Composable
private fun ComerciosList(comercios: List<Comercio>, productos: List<Producto>) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp)
    ) {
        Text(
            text = "Comercios cerca de ti (${comercios.size})",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF007460)
        )

        Spacer(modifier = Modifier.height(16.dp))

        comercios.forEach { comercio ->
            ComercioCard(
                comercio = comercio,
                productosCount = productos.count { it.comercioId == comercio.id }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ComercioCard(comercio: Comercio, productosCount: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 5.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                BioWayColors.NavGreen.copy(alpha = 0.1f),
                                BioWayColors.LightGreen.copy(alpha = 0.1f)
                            )
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getIconForCategory(comercio.categoria),
                    contentDescription = null,
                    tint = BioWayColors.NavGreen,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Información
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = comercio.nombre,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2C3E50)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFF999999),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${comercio.municipio}, ${comercio.estado}",
                        fontSize = 13.sp,
                        color = Color(0xFF999999),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = Color(0xFF999999),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = comercio.horario,
                        fontSize = 13.sp,
                        color = Color(0xFF999999)
                    )
                }
            }

            // Badge de ofertas
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = BioWayColors.NavGreen
                ) {
                    Text(
                        text = "$productosCount ofertas",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Icon(
                    imageVector = Icons.Default.ArrowForwardIos,
                    contentDescription = null,
                    tint = BioWayColors.NavGreen,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private fun getIconForCategory(categoria: String): ImageVector {
    return when (categoria) {
        "Cafetería" -> Icons.Default.Coffee
        "Restaurante" -> Icons.Default.Restaurant
        "Supermercado" -> Icons.Default.ShoppingCart
        "Deportes" -> Icons.Default.SportsBasketball
        "Salud" -> Icons.Default.MedicalServices
        else -> Icons.Default.Store
    }
}

// Modelos de datos
data class Producto(
    val id: String,
    val comercioId: String,
    val comercioNombre: String,
    val nombre: String,
    val icono: String,
    val bioCoins: Int,
    val descuento: Int,
    val destacado: Boolean
)

data class Comercio(
    val id: String,
    val nombre: String,
    val categoria: String,
    val municipio: String,
    val estado: String,
    val horario: String
)

private fun getMockProductos(): List<Producto> {
    return listOf(
        Producto(
            id = "1",
            comercioId = "c1",
            comercioNombre = "Café Verde",
            nombre = "2x1 en cappuccinos",
            icono = "☕",
            bioCoins = 300,
            descuento = 50,
            destacado = true
        ),
        Producto(
            id = "2",
            comercioId = "c2",
            comercioNombre = "Restaurante Natura",
            nombre = "Buffet vegetariano",
            icono = "🥗",
            bioCoins = 500,
            descuento = 40,
            destacado = true
        ),
        Producto(
            id = "3",
            comercioId = "c3",
            comercioNombre = "Super Eco",
            nombre = "Descuento en productos orgánicos",
            icono = "🛒",
            bioCoins = 200,
            descuento = 30,
            destacado = true
        )
    )
}

private fun getMockComercios(): List<Comercio> {
    return listOf(
        Comercio(
            id = "c1",
            nombre = "Café Verde",
            categoria = "Cafetería",
            municipio = "Benito Juárez",
            estado = "CDMX",
            horario = "8:00 - 20:00"
        ),
        Comercio(
            id = "c2",
            nombre = "Restaurante Natura",
            categoria = "Restaurante",
            municipio = "Coyoacán",
            estado = "CDMX",
            horario = "12:00 - 22:00"
        ),
        Comercio(
            id = "c3",
            nombre = "Super Eco",
            categoria = "Supermercado",
            municipio = "Miguel Hidalgo",
            estado = "CDMX",
            horario = "7:00 - 23:00"
        ),
        Comercio(
            id = "c4",
            nombre = "Gimnasio FitGreen",
            categoria = "Deportes",
            municipio = "Cuauhtémoc",
            estado = "CDMX",
            horario = "6:00 - 22:00"
        )
    )
}
