# 🗺️ Diagnóstico del Mapa OSMDroid

## Cambios Realizados

### ✅ Mejoras Implementadas:

1. **Cache interno** - Usa `context.filesDir` en lugar de `context.cacheDir` (no requiere permisos)
2. **User Agent correcto** - Formato: `com.biowaymexico/1.0`
3. **Hardware acceleration** - Renderizado más rápido
4. **Lifecycle management** - Manejo correcto de `onPause()` y `onDetach()`
5. **Zoom más alto** - Cambiado de 13.0 a 15.0 para ver más detalles
6. **Fondo gris** - Color de fondo mientras cargan los tiles
7. **Post handler** - Marcadores se agregan después de que el mapa se inicializa

---

## 🔍 Pasos de Diagnóstico

### 1. Verificar Conexión a Internet

El mapa **REQUIERE** conexión a internet para descargar los tiles de OpenStreetMap.

**Verifica:**
- ✅ El dispositivo/emulador tiene conexión a internet
- ✅ El emulador no está bloqueando tráfico de red
- ✅ No hay firewall bloqueando `tile.openstreetmap.org`

### 2. Verificar Permisos

Revisa que los permisos estén en `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

### 3. Ver Logcat (Android Studio)

Abre **Logcat** en Android Studio y filtra por:

```
OSMDroid
TileProvider
MapView
```

**Busca errores como:**
- ❌ "Network error downloading tile"
- ❌ "User agent not set"
- ❌ "Failed to load tile"
- ✅ "Tile loaded successfully"

### 4. Verificar que el Mapa se Está Creando

En **Logcat**, deberías ver algo como:

```
MapView: onAttachedToWindow
MapView: Creating tile provider
TileProvider: Downloading tiles from MAPNIK
```

### 5. Probar con Diferentes TileSources

Si MAPNIK no funciona, intenta otros servidores:

Modifica `RecolectorMapaScreen.kt` línea 58:

```kotlin
// Opción 1: MAPNIK (predeterminado)
setTileSource(TileSourceFactory.MAPNIK)

// Opción 2: WIKIMEDIA
setTileSource(TileSourceFactory.WIKIMEDIA)

// Opción 3: OpenTopo
setTileSource(TileSourceFactory.OpenTopo)
```

### 6. Verificar Cache de Tiles

Los tiles se guardan en:

```
/data/data/com.biowaymexico/files/osmdroid/tiles/
```

**Desde Android Studio > Device Explorer:**
1. Navega a `data/data/com.biowaymexico/files/osmdroid/tiles/`
2. Verifica si hay archivos `.png` o `.jpg`
3. Si hay archivos, los tiles se están descargando

### 7. Probar en Dispositivo Real

A veces el emulador tiene problemas de red. Prueba en un dispositivo físico:

1. Conecta dispositivo Android
2. Habilita **Depuración USB**
3. Run > Run 'app'
4. Selecciona el dispositivo físico

---

## 🔧 Soluciones Comunes

### Problema: Pantalla en blanco/gris

**Solución 1: Esperar unos segundos**
- Los tiles tardan en descargar la primera vez
- Espera 10-15 segundos con buena conexión

**Solución 2: Verificar internet en emulador**

En emulador Android Studio:
```
Settings > Network & Internet > Internet
Verifica que esté "Connected"
```

**Solución 3: Reiniciar app**
```
Stop app (cuadrado rojo)
Run app de nuevo
```

### Problema: Error de User Agent

Si ves en Logcat:
```
Error: User agent must be set
```

**Ya está solucionado en el código:**
```kotlin
userAgentValue = "${context.packageName}/1.0"
```

### Problema: No se ven marcadores

Los marcadores se agregan **después** de que el mapa carga.

**Espera a que aparezcan las calles primero.**

---

## 🧪 Código de Prueba Simplificado

Si nada funciona, prueba esta versión MÍNIMA:

```kotlin
@Composable
fun RecolectorMapaScreen() {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        Configuration.getInstance().apply {
            userAgentValue = "BioWayApp/1.0"
            load(context, PreferenceManager.getDefaultSharedPreferences(context))
        }
    }

    AndroidView(
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(15.0)
                controller.setCenter(GeoPoint(19.4326, -99.1332))
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
```

---

## 📱 Alternativas si OSMDroid No Funciona

### Opción A: Usar Google Maps (requiere API key)

Cambiar a Google Maps Compose:

```kotlin
dependencies {
    implementation("com.google.maps.android:maps-compose:4.3.0")
}
```

### Opción B: Usar MapBox (gratis hasta cierto límite)

```kotlin
dependencies {
    implementation("com.mapbox.maps:android:11.0.0")
}
```

### Opción C: Usar imagen estática temporalmente

Usar una imagen de mapa estático mientras resuelves:

```kotlin
Image(
    painter = painterResource(R.drawable.mapa_cdmx),
    contentDescription = "Mapa"
)
```

---

## 📊 Checklist de Verificación

Marca lo que ya verificaste:

- [ ] Conexión a internet activa
- [ ] Permisos INTERNET y ACCESS_NETWORK_STATE en manifest
- [ ] Logcat muestra "MapView created"
- [ ] Esperé al menos 10 segundos para que carguen tiles
- [ ] Probé hacer zoom in/out
- [ ] Probé en dispositivo real (no emulador)
- [ ] Probé diferentes TileSources (MAPNIK, WIKIMEDIA)
- [ ] Cache directory existe en `/files/osmdroid/tiles/`
- [ ] No hay errores en Logcat

---

## 🆘 Si Nada Funciona

Comparte la salida de Logcat filtrando por `OSMDroid`:

```
Android Studio > Logcat > Filtrar "OSMDroid"
Copiar y pegar los mensajes
```

O envía screenshot de:
1. La pantalla del mapa (en blanco)
2. Logcat con filtro "OSMDroid"
3. Network status del emulador

---

## ✅ Indicadores de Éxito

Deberías ver:

1. **Calles y nombres** de la Ciudad de México
2. **Edificios** en color gris claro
3. **Parques** en verde
4. **25 marcadores rojos** en diferentes ubicaciones
5. **Zoom funcional** con gestos táctiles

---

*Última actualización: 28 de Octubre, 2025*
