package com.biowaymexico.ui.screens.brindador

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biowaymexico.ui.theme.BioWayColors
import kotlinx.coroutines.launch

/**
 * Pantalla de Comercio Local - Diseño Moderno Minimalista 2024
 * Aplica estándar visual de BioWay con grid de productos limpio
 */
@Composable
fun BrindadorComercioLocalScreen() {
    var searchText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Todos") }
    var userBioCoins by remember { mutableStateOf(0) }

    val brindadorRepository = remember { com.biowaymexico.data.BrindadorRepository() }
    val scope = rememberCoroutineScope()

    val productos = remember { getMockProductos() }
    val categorias = listOf("Todos", "Alimentos", "Productos", "Servicios", "Descuentos")

    // Cargar BioCoins del usuario
    LaunchedEffect(Unit) {
        scope.launch {
            val result = brindadorRepository.obtenerBrindador()
            if (result.isSuccess) {
                userBioCoins = result.getOrNull()?.bioCoins ?: 0
            }
        }
    }

    // Filtrar productos por categoría
    val productosFiltrados = if (selectedCategory == "Todos") {
        productos
    } else {
        productos.filter { it.categoria == selectedCategory }
    }

    Scaffold(
        containerColor = Color(0xFFFAFAFA)  // Mismo fondo que Dashboard
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 20.dp)  // Sin top padding
        ) {
            item {
                // Header - misma altura que Dashboard
                ComercioHeader(userBioCoins = userBioCoins)
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                // Barra de búsqueda
                SearchBar(
                    searchText = searchText,
                    onSearchTextChange = { searchText = it }
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            item {
                // Categorías horizontales
                CategoryTabs(
                    categories = categorias,
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                // Productos destacados
                FeaturedProductsSection(
                    productos = productos.filter { it.destacado }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                // Grid de productos
                ProductGrid(productos = productosFiltrados)
            }
        }
    }
}

@Composable
private fun ComercioHeader(userBioCoins: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 0.dp, bottom = 8.dp),  // Misma altura que Dashboard
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Comercio Local",
                style = MaterialTheme.typography.headlineMedium,  // Hammersmith One 28sp
                color = BioWayColors.BrandDarkGreen  // #007565
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Descuentos exclusivos para ti",
                style = MaterialTheme.typography.bodySmall,  // Montserrat
                color = Color(0xFF2E7D6C)
            )
        }

        // Badge de BioCoins - mismo estilo que Dashboard
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF70D162).copy(alpha = 0.12f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.MonetizationOn,
                    contentDescription = null,
                    tint = Color(0xFF70D162),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = userBioCoins.toString(),
                    style = MaterialTheme.typography.titleMedium,  // Hammersmith One
                    color = Color(0xFF70D162)
                )
            }
        }
    }
}

@Composable
private fun SearchBar(
    searchText: String,
    onSearchTextChange: (String) -> Unit
) {
    OutlinedTextField(
        value = searchText,
        onValueChange = onSearchTextChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        placeholder = {
            Text(
                "Buscar productos...",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF2E7D6C).copy(alpha = 0.6f)
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Buscar",
                tint = BioWayColors.BrandGreen
            )
        },
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedBorderColor = BioWayColors.BrandDarkGreen,
            unfocusedBorderColor = Color(0xFF2E7D6C).copy(alpha = 0.3f),
            focusedTextColor = BioWayColors.BrandDarkGreen,
            unfocusedTextColor = Color(0xFF2E7D6C),
            cursorColor = BioWayColors.BrandDarkGreen
        ),
        singleLine = true
    )
}

@Composable
private fun CategoryTabs(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(categories.size) { index ->
            val category = categories[index]
            val isSelected = category == selectedCategory

            Surface(
                onClick = {
                    onCategorySelected(category)
                },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected)
                    Color(0xFF70D162).copy(alpha = 0.15f)
                else
                    Color.White,
                shadowElevation = if (isSelected) 2.dp else 1.dp
            ) {
                Text(
                    text = category,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.labelLarge,  // Montserrat Medium
                    color = if (isSelected)
                        Color(0xFF70D162)
                    else
                        Color(0xFF2E7D6C),
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun FeaturedProductsSection(productos: List<Producto>) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Destacados",
                style = MaterialTheme.typography.titleLarge,  // Hammersmith One
                color = BioWayColors.BrandDarkGreen
            )
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = BioWayColors.BrandGreen,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Scroll horizontal de productos destacados
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
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
        onClick = { /* TODO: Ver detalle */ },
        modifier = Modifier.width(180.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Badge "Destacado"
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = BioWayColors.BrandGreen.copy(alpha = 0.15f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = BioWayColors.BrandGreen,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Destacado",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF70D162),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Placeholder de imagen
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        color = BioWayColors.BrandGreen.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingBag,
                    contentDescription = null,
                    tint = BioWayColors.BrandGreen,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Nombre
            Text(
                text = producto.nombre,
                style = MaterialTheme.typography.titleSmall,  // Hammersmith One
                color = BioWayColors.BrandDarkGreen,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                minLines = 2
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Precio
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MonetizationOn,
                        contentDescription = null,
                        tint = Color(0xFF70D162),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${producto.precio}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF70D162),
                        fontSize = 18.sp
                    )
                }

                if (producto.descuento > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF70D162).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "-${producto.descuento}%",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF70D162),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductGrid(productos: List<Producto>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Text(
            text = "${productos.size} productos disponibles",
            style = MaterialTheme.typography.bodyMedium,  // Montserrat
            color = Color(0xFF2E7D6C),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Grid de 2 columnas
        productos.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                row.forEach { producto ->
                    ProductCard(
                        producto = producto,
                        modifier = Modifier.weight(1f)
                    )
                }
                // Si la fila tiene solo 1 elemento, agregar espacio
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ProductCard(
    producto: Producto,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = { /* TODO: Ver detalle */ },
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Placeholder de imagen
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        color = BioWayColors.BrandGreen.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingBag,
                    contentDescription = null,
                    tint = BioWayColors.BrandGreen,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Nombre del producto
            Text(
                text = producto.nombre,
                style = MaterialTheme.typography.titleSmall,  // Hammersmith One
                color = BioWayColors.BrandDarkGreen,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                minLines = 2
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Comercio
            Text(
                text = producto.comercio,
                style = MaterialTheme.typography.bodySmall,  // Montserrat
                color = Color(0xFF2E7D6C).copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Precio en BioCoins
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.MonetizationOn,
                        contentDescription = null,
                        tint = Color(0xFF70D162),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${producto.precio}",
                        style = MaterialTheme.typography.titleMedium,  // Hammersmith One
                        color = Color(0xFF70D162),
                        fontSize = 18.sp
                    )
                }

                // Badge de descuento si aplica
                if (producto.descuento > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF70D162).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "-${producto.descuento}%",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,  // Montserrat
                            color = Color(0xFF70D162),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

// Modelo de datos
data class Producto(
    val id: Int,
    val nombre: String,
    val comercio: String,
    val categoria: String,
    val precio: Int,
    val descuento: Int = 0,
    val destacado: Boolean = false
)

private fun getMockProductos(): List<Producto> {
    return listOf(
        Producto(1, "Café Orgánico Premium", "Café del Bosque", "Alimentos", 50, 10, true),
        Producto(2, "Jabón Artesanal Natural", "Eco Limpio", "Productos", 30, 15),
        Producto(3, "Bolsas Reutilizables Set x3", "Verde Market", "Productos", 25, 0, true),
        Producto(4, "Miel Orgánica 500g", "Apiario Luna", "Alimentos", 80, 5),
        Producto(5, "Shampoo Sólido Vegano", "Natura Bella", "Productos", 45, 20),
        Producto(6, "Pan Integral Artesanal", "Panadería Sol", "Alimentos", 35, 0),
        Producto(7, "Aceite de Coco Orgánico", "Coco Verde", "Alimentos", 60, 10),
        Producto(8, "Cepillo de Bambú", "Eco Dental", "Productos", 20, 0, true),
        Producto(9, "Té Verde Orgánico", "Té del Valle", "Alimentos", 40, 15),
        Producto(10, "Velas de Soya Aromáticas", "Luz Natural", "Productos", 55, 5),
        Producto(11, "Chocolate Orgánico 70%", "Cacao Real", "Alimentos", 45, 0),
        Producto(12, "Limpieza Ecológica", "Eco Service", "Servicios", 100, 20)
    )
}
