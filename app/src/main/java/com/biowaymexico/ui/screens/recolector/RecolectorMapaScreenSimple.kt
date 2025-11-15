package com.biowaymexico.ui.screens.recolector

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.utsman.osmandcompose.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker

/**
 * Pantalla de mapa del Recolector usando OSM Android Compose
 * 100% GRATIS, SIN API KEY, OPTIMIZADO PARA VELOCIDAD
 */
@Composable
fun RecolectorMapaScreenSimple() {
    val context = LocalContext.current
    var isMapReady by remember { mutableStateOf(false) }

    // Configurar OSMDroid una sola vez
    LaunchedEffect(Unit) {
        configureOSMDroid(context)
    }

    // Estado de la cámara centrado en Ciudad de México
    val cameraState = rememberCameraState {
        geoPoint = GeoPoint(19.4326, -99.1332)
        zoom = 13.0
    }

    // Configurar propiedades del mapa
    var mapProperties by remember {
        mutableStateOf(
            DefaultMapProperties
                .copy(tileSources = getCartoDBVoyagerTileSource())
                .copy(isFlingEnable = true)
                .copy(isEnableRotationGesture = false)
                .copy(minZoomLevel = 10.0)
                .copy(maxZoomLevel = 18.0)
        )
    }

    Scaffold(
        containerColor = Color(0xFFF8F9FA)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Mapa de OpenStreetMap optimizado con estilo ligero
            OpenStreetMap(
                modifier = Modifier.fillMaxSize(),
                cameraState = cameraState,
                properties = mapProperties,
                onFirstLoadListener = {
                    isMapReady = true
                }
            ) {
                // Reducir número de marcadores para mejor rendimiento
                // Solo mostrar los más cercanos o importantes
                val importantPoints = listOf(
                    Triple(GeoPoint(19.4326, -99.1332), "Centro de Acopio Norte", "45.5 kg"),
                    Triple(GeoPoint(19.4280, -99.1380), "Punto Verde Polanco", "32.0 kg"),
                    Triple(GeoPoint(19.4360, -99.1290), "Reciclaje Condesa", "28.5 kg"),
                    Triple(GeoPoint(19.4250, -99.1350), "EcoPunto Roma", "52.0 kg"),
                    Triple(GeoPoint(19.4390, -99.1310), "Centro Comunitario Juárez", "18.5 kg"),
                    Triple(GeoPoint(19.4410, -99.1450), "Punto Limpio Anzures", "37.2 kg"),
                    Triple(GeoPoint(19.4195, -99.1420), "Centro Verde Nápoles", "41.8 kg"),
                    Triple(GeoPoint(19.4450, -99.1280), "Recicladora San Rafael", "29.3 kg")
                )

                // Agregar marcadores de forma eficiente
                importantPoints.forEach { (geoPoint, title, snippet) ->
                    Marker(
                        state = rememberMarkerState(
                            geoPoint = geoPoint
                        ),
                        title = title,
                        snippet = snippet,
                        icon = null // Usar icono por defecto para mejor rendimiento
                    )
                }
            }

            // Indicador de carga mientras el mapa se inicializa
            if (!isMapReady) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFF70D997)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Cargando mapa...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tile source de CartoDB Voyager - Estilo minimalista y MUCHO más ligero
 * Mejores servidores, menos detalle = más rápido
 */
private fun getCartoDBVoyagerTileSource(): XYTileSource {
    return XYTileSource(
        "CartoDBVoyager",
        0,
        19,
        256,
        ".png",
        arrayOf(
            "https://a.basemaps.cartocdn.com/rastertiles/voyager/",
            "https://b.basemaps.cartocdn.com/rastertiles/voyager/",
            "https://c.basemaps.cartocdn.com/rastertiles/voyager/",
            "https://d.basemaps.cartocdn.com/rastertiles/voyager/"
        ),
        "© OpenStreetMap contributors © CartoDB"
    )
}

/**
 * Configuración optimizada de OSMDroid para mejor rendimiento
 */
private fun configureOSMDroid(context: Context) {
    Configuration.getInstance().apply {
        // User agent para evitar rate limiting
        userAgentValue = "BioWayMexico/1.0"

        // Aumentar caché de tiles (más memoria = menos descargas)
        tileFileSystemCacheMaxBytes = 100L * 1024L * 1024L // 100 MB
        tileFileSystemCacheTrimBytes = 80L * 1024L * 1024L  // 80 MB

        // Optimizar descarga de tiles
        setTileDownloadThreads(6.toShort())     // Más threads = descarga más rápida
        setTileDownloadMaxQueueSize(40.toShort()) // Cola más grande

        // Configurar expiración de caché (en milisegundos)
        expirationExtendedDuration = 1000L * 60L * 60L * 24L * 30L // 30 días

        // Path de caché
        osmdroidBasePath = context.filesDir
        osmdroidTileCache = context.filesDir.resolve("osmdroid/tiles")

        // Cargar configuración
        load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
    }
}
