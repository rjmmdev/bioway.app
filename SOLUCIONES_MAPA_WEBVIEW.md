# 🗺️ Soluciones para Mapa con WebView

## 🔧 Cambios Aplicados

### 1. Agregados Logs de Debug

En `RecolectorMapaScreenLeaflet.kt`:

```kotlin
// Logs para diagnosticar
Log.d("LeafletMap", "Página cargada: $url")
Log.e("LeafletMap", "Error: $description")
Log.d("LeafletMap-JS", "${consoleMessage}")
```

### 2. Configuraciones Adicionales de WebView

```kotlin
mixedContentMode = MIXED_CONTENT_ALWAYS_ALLOW
allowFileAccess = true
allowContentAccess = true
```

### 3. Creada Versión Simple de Respaldo

`RecolectorMapaScreenSimple.kt` - Usa iframe directo de OpenStreetMap

---

## 🔍 Cómo Diagnosticar

### En Android Studio > Logcat:

1. **Filtrar por "LeafletMap"**
   ```
   Logcat > Buscar: LeafletMap
   ```

2. **Buscar estos mensajes:**
   ```
   ✅ D/LeafletMap: Página cargada: https://example.com
   ✅ D/LeafletMap-JS: (mensajes de JavaScript)
   ❌ E/LeafletMap: Error: (descripción del error)
   ```

---

## 🧪 Probar Versión Simple

Si Leaflet no funciona, prueba la versión simple:

### En `RecolectorMainScreen.kt`:

```kotlin
// Cambiar de:
0 -> RecolectorMapaScreenLeaflet()

// A:
0 -> RecolectorMapaScreenSimple()
```

Esta versión:
- ✅ Usa iframe directo de OpenStreetMap
- ✅ No requiere Leaflet.js
- ✅ Más simple, más confiable
- ❌ Sin marcadores personalizados

---

## 🔴 Si Ves Pantalla Blanca/Gris

### Causa 1: JavaScript Deshabilitado

**Verifica en logs:**
```
settings.javaScriptEnabled = true
```

### Causa 2: Sin Conexión a Internet

**Verifica:**
```bash
adb shell ping -c 4 tile.openstreetmap.org
```

### Causa 3: Mixed Content Blocked

**Solución ya aplicada:**
```kotlin
mixedContentMode = MIXED_CONTENT_ALWAYS_ALLOW
```

### Causa 4: WebView No Se Está Creando

**Busca en Logcat:**
```
cr_LibraryLoader: Successfully loaded native library
```

Si NO aparece, WebView tiene problemas.

---

## 📱 Verificación en Emulador

### 1. Abrir Chrome en el emulador

1. Abre Chrome browser
2. Navega a: https://www.openstreetmap.org
3. Si carga → Internet funciona ✅
4. Si NO carga → Problema de red del emulador ❌

### 2. Verificar WebView System

```
Settings > Apps > Show system apps
Buscar "Android System WebView"
Debe estar habilitado ✅
```

---

## 🆘 Solución de Emergencia

Si nada funciona, usa **Google Maps Lite** (gratis):

### En `build.gradle.kts`:

```kotlin
implementation("com.google.android.gms:play-services-maps:18.2.0")
implementation("com.google.maps.android:maps-compose:4.3.0")
```

### Requiere:
- API Key de Google (gratis, 10k cargas/mes)
- Configuración en Google Cloud Console

---

## 📊 Checklist de Troubleshooting

- [ ] WebView se está cargando (logs de chromium)
- [ ] JavaScript habilitado (javaScriptEnabled = true)
- [ ] Internet funciona (ping a tile.openstreetmap.org)
- [ ] Filtrado Logcat por "LeafletMap"
- [ ] Visto mensaje "Página cargada"
- [ ] Sin errores en logs de JavaScript
- [ ] Probado versión simple (iframe)
- [ ] Chrome funciona en emulador
- [ ] WebView actualizado

---

## 🎯 Siguiente Paso

### Ejecuta la app y comparte:

```
Logcat filtrado por: LeafletMap

Busca estos mensajes EXACTOS:
✅ D/LeafletMap: Página cargada: https://example.com
✅ D/LeafletMap-JS: 🗺️ Script iniciado
✅ D/LeafletMap-JS: ✅ Leaflet cargado correctamente
✅ D/LeafletMap-JS: ✅ Mapa creado
✅ D/LeafletMap-JS: ✅ Tiles agregados
✅ D/LeafletMap-JS: 🗺️ Agregando 25 marcadores...
✅ D/LeafletMap-JS: ✅ Marcadores agregados
✅ D/LeafletMap-JS: 🎉 Mapa completamente cargado

Si ves errores:
❌ E/LeafletMap: Error: (descripción del error)
❌ D/LeafletMap-JS: ❌ Leaflet NO está cargado
```

O prueba la versión simple modificando `RecolectorMainScreen.kt`

---

*Actualizado: 28 de Octubre, 2025*  
*Debug habilitado ✅*
