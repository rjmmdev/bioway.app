package com.biowaymexico.ui.screens.maestro

import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.viewinterop.AndroidView
import com.biowaymexico.ui.theme.BioWayColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.net.URLEncoder

/**
 * Selector de ubicación en mapa para Bote BioWay
 * Usa OSMDroid (OpenStreetMap - gratuito, sin API keys)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapaSelectorBoteScreen(
    estado: String,
    municipio: String,
    colonia: String,
    codigoPostal: String,
    navController: androidx.navigation.NavHostController,
    onNavigateBack: () -> Unit
) {
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var isGeocoding by remember { mutableStateOf(true) }
    var geocodedLat by remember { mutableStateOf<Double?>(null) }
    var geocodedLon by remember { mutableStateOf<Double?>(null) }

    // Geocodificar dirección usando Nominatim (gratuito)
    LaunchedEffect(estado, municipio, colonia) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // Construir dirección más específica
                val direccionParts = listOfNotNull(
                    colonia.takeIf { it.isNotBlank() },
                    municipio.takeIf { it.isNotBlank() },
                    estado.takeIf { it.isNotBlank() },
                    "Mexico"
                )
                val direccion = direccionParts.joinToString(", ")

                android.util.Log.d("MapaSelector", "🔍 Geocodificando: $direccion")

                val url = "https://nominatim.openstreetmap.org/search?q=${
                    java.net.URLEncoder.encode(direccion, "UTF-8")
                }&format=json&limit=1&countrycodes=mx"

                android.util.Log.d("MapaSelector", "📡 URL: $url")

                val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "BioWayApp/1.0")
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val response = connection.inputStream.bufferedReader().readText()
                android.util.Log.d("MapaSelector", "📥 Response: $response")

                // Parse JSON
                if (response.contains("\"lat\"")) {
                    val latMatch = "\"lat\":\"([0-9.-]+)\"".toRegex().find(response)
                    val lonMatch = "\"lon\":\"([0-9.-]+)\"".toRegex().find(response)

                    if (latMatch != null && lonMatch != null) {
                        val lat = latMatch.groupValues[1].toDouble()
                        val lon = lonMatch.groupValues[1].toDouble()

                        android.util.Log.d("MapaSelector", "✅ Geocodificado: $lat, $lon")

                        geocodedLat = lat
                        geocodedLon = lon
                    } else {
                        android.util.Log.w("MapaSelector", "⚠️ No se encontraron coordenadas en response")
                    }
                } else {
                    android.util.Log.w("MapaSelector", "⚠️ Response no contiene coordenadas")
                }
            } catch (e: Exception) {
                android.util.Log.e("MapaSelector", "❌ Error geocoding: ${e.message}", e)
            } finally {
                isGeocoding = false
                android.util.Log.d("MapaSelector", "🏁 Geocoding finalizado. Lat: $geocodedLat, Lon: $geocodedLon")
            }
        }
    }

    val initialLat = geocodedLat ?: 19.4326
    val initialLon = geocodedLon ?: -99.1332

    android.util.Log.d("MapaSelector", "🗺️ Usando coordenadas: $initialLat, $initialLon")

    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = { Text("Selecciona Ubicación del Bote") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
        ) {
            // Indicador de geocodificación
            if (isGeocoding) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color(0xFF70D162),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Buscando ubicación...",
                            color = Color(0xFF2E7D6C)
                        )
                    }
                }
            }

            // Mapa OSMDroid
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                AndroidView(
                    factory = { context ->
                        Configuration.getInstance().userAgentValue = context.packageName
                        MapView(context).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            mapView = this
                        }
                    },
                    update = { map ->
                        // Actualizar cuando cambien las coordenadas geocodificadas
                        if (!isGeocoding) {
                            map.controller.setZoom(16.0)
                            map.controller.setCenter(GeoPoint(initialLat, initialLon))

                            // Limpiar y agregar marker
                            map.overlays.clear()
                            val marker = Marker(map)
                            marker.position = GeoPoint(initialLat, initialLon)
                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            marker.title = "Ubicación del Bote"
                            marker.isDraggable = true
                            map.overlays.add(marker)
                            map.invalidate()

                            android.util.Log.d("MapaSelector", "🗺️ Mapa centrado en: $initialLat, $initialLon")
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Marcador central fijo
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Ubicación",
                    tint = Color.Red,
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.Center)
                        .offset(y = (-24).dp)
                )
            }

            // Info de coordenadas
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            null,
                            tint = Color(0xFF70D162),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Ubicación aproximada:",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF2E7D6C)
                            )
                            Text(
                                text = "$colonia, $municipio, $estado",
                                style = MaterialTheme.typography.bodySmall,
                                color = BioWayColors.BrandDarkGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Arrastra el marcador rojo para ajustar la ubicación exacta",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF2E7D6C)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Lat: %.6f, Lon: %.6f".format(
                            mapView?.mapCenter?.latitude ?: initialLat,
                            mapView?.mapCenter?.longitude ?: initialLon
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = BioWayColors.BrandDarkGreen,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            // Obtener posición del marcador (puede haber sido arrastrado)
                            val marker = mapView?.overlays?.firstOrNull() as? Marker
                            val finalLat = marker?.position?.latitude ?: initialLat
                            val finalLon = marker?.position?.longitude ?: initialLon

                            android.util.Log.d("MapaSelector", "✅ Ubicación confirmada: $finalLat, $finalLon")

                            // Guardar coordenadas en SavedStateHandle para regresar
                            navController.previousBackStackEntry?.savedStateHandle?.apply {
                                set("latitud", finalLat)
                                set("longitud", finalLon)
                            }

                            onNavigateBack()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF70D162)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Check, null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Confirmar Ubicación", color = Color.White)
                    }
                }
            }
        }
    }
}
