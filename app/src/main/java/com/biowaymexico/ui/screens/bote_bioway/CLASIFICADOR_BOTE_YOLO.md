# ClasificadorBoteYOLOScreen - Documentacion Tecnica

## Descripcion General

`ClasificadorBoteYOLOScreen.kt` es la pantalla de clasificacion de residuos con IA para el **Bote BioWay**. Utiliza un modelo YOLOv8 (TensorFlow Lite) para detectar y clasificar residuos en tiempo real a traves de la camara del dispositivo.

**Ubicacion:** `app/src/main/java/com/biowaymexico/ui/screens/bote_bioway/ClasificadorBoteYOLOScreen.kt`

**Navegacion:** `BoteBioWayMainScreen` → "Clasificador IA v2" → `ClasificadorBoteYOLOScreen`

---

## Arquitectura de la Pantalla

### Estados (YoloScreenState)

```kotlin
private enum class YoloScreenState {
    PERMISSION_REQUEST,    // Solicitar permiso de camara
    ROI_CONFIGURATION,     // Paso 1: Configurar area de deteccion
    DETECTION_RUNNING      // Paso 2: Deteccion en tiempo real
}
```

### Flujo de Usuario

```
┌─────────────────────┐
│ PERMISSION_REQUEST  │ ← Usuario sin permiso de camara
└─────────┬───────────┘
          │ (permiso otorgado)
          ▼
┌─────────────────────┐
│ ROI_CONFIGURATION   │ ← Paso 1: Ajustar area de deteccion
│ (CameraPreviewOnly) │   - Arrastrar esquinas para redimensionar
└─────────┬───────────┘   - Arrastrar centro para mover
          │ (boton "Iniciar Clasificacion")
          ▼
┌─────────────────────┐
│ DETECTION_RUNNING   │ ← Paso 2: Deteccion activa
│ (CameraPreview +    │   - ROI fijo (no editable)
│  WasteDetector +    │   - Inferencia en tiempo real
│  BackgroundFilter)  │   - Filtrado del plato de fondo
└─────────────────────┘
```

---

## Componentes Principales

### 1. ROIRect (Region de Interes)

```kotlin
data class ROIRect(
    val left: Float = 0.15f,    // Valores normalizados 0-1
    val top: Float = 0.15f,
    val right: Float = 0.85f,
    val bottom: Float = 0.85f
)
```

El ROI define el area de la imagen que sera recortada y enviada al modelo. Solo los objetos dentro de esta area son detectados.

### 2. WasteDetector

Wrapper de TensorFlow Lite para el modelo YOLOv8.

**Archivos del modelo:**
- Modelo: `app/src/main/assets/models/waste_detector_v2.tflite`
- Etiquetas: `app/src/main/assets/labels/waste_detector_labels.txt`

**Parametros de deteccion:**
```kotlin
detector.confidenceThreshold = 0.25f  // Umbral de confianza minima
detector.iouThreshold = 0.4f          // Umbral IoU para NMS
detector.numItemsThreshold = 30       // Maximo de detecciones
```

**Categorias detectables:**
- biological (Organico)
- cardboard (Carton)
- glass (Vidrio)
- metal (Metal)
- paper (Papel)
- plastic (Plastico generico)
- plastic-pet, plastic-pe_hd, plastic-pp, plastic-ps, plastic-others
- trash (Basura)

### 3. BackgroundPlateFilter

Filtro inteligente para ignorar el **plato blanco del bote** que el modelo detecta incorrectamente como plastico.

---

## BackgroundPlateFilter - Explicacion Detallada

### Problema

El bote BioWay tiene un plato/superficie blanca donde se colocan los residuos. El modelo YOLO a veces detecta este plato como "plastic" con **baja confianza (26-34%)** y **area grande (60-99% del ROI)**.

### Solucion: Filtrado por Confianza + Area

El filtro distingue entre el plato (falso positivo) y objetos reales usando dos criterios:

| Tipo | Confianza | Area |
|------|-----------|------|
| **Plato (falso positivo)** | 26-34% (baja) | 60-99% (grande) |
| **Objeto real** | 70-90% (alta) | 20-50% (mediana) |

### Logica del Filtro

```kotlin
object BackgroundPlateFilter {
    var minConfidenceThreshold = 0.50f   // Umbral de confianza
    var suspiciousAreaThreshold = 0.35f  // Umbral de area sospechosa
}
```

**Caso 1: Un solo plastico detectado**
```
SI (confianza < 50%) Y (area > 35%):
    → FILTRAR (es el plato)
SINO:
    → ACEPTAR (es un objeto real)
```

**Caso 2: Multiples plasticos detectados**
```
→ FILTRAR el de mayor area (el plato)
→ ACEPTAR todos los demas (objetos reales)
```

**Otros materiales (carton, vidrio, metal, etc.):**
```
→ ACEPTAR siempre (no pueden ser el plato)
```

### Ejemplos de Filtrado

| Deteccion | Confianza | Area | Resultado |
|-----------|-----------|------|-----------|
| Plato solo | 34% | 67% | FILTRADO (conf<50% Y area>35%) |
| Botella PET | 83% | 43% | ACEPTADO (conf>=50%) |
| Botella pequena | 34% | 32% | ACEPTADO (area<=35%) |
| Plato + Botella | 34%/82% | 60%/25% | Plato FILTRADO, Botella ACEPTADA |

---

## Flujo de Procesamiento de Imagen

```
ImageProxy (CameraX)
       │
       ▼
imageProxyToBitmap() ──→ Bitmap (YUV → RGB)
       │
       ▼
rotateBitmap() ──────→ Bitmap rotado (segun orientacion)
       │
       ▼
cropBitmapToROI() ───→ Bitmap recortado al ROI
       │
       ▼
detector.detect() ───→ DetectionResult (lista de detecciones)
       │
       ▼
BackgroundPlateFilter.filterDetections() ──→ Detecciones filtradas
       │
       ▼
adjustDetectionsToFullImage() ──→ Coordenadas ajustadas
       │
       ▼
UI (DetectionOverlay + DetectionResultsPanel)
```

---

## Tags de Logging para Debug

```bash
# Filtrar logs del clasificador
adb logcat | grep -E "(ClasificadorBoteYOLO|PlateFilter)"

# Solo filtro del plato
adb logcat | grep PlateFilter

# Solo clasificador principal
adb logcat | grep ClasificadorBoteYOLO

# Comunicacion Bluetooth/ESP32 (Protocolo v2)
adb logcat | grep BluetoothManager

# Ver todo el flujo de deposito
adb logcat | grep -E "(ClasificadorBoteYOLO|BluetoothManager)"
```

### Formato de Logs del Protocolo v2

```
═══════════════════════════════════════
🎯 PROTOCOLO v2: DEPOSITAR Y ESPERAR LISTO
   Categoría: Plástico
═══════════════════════════════════════
📤 Enviando: DEPOSITAR:Plástico
⏳ Esperando señal LISTO del ESP32...
📥 Respuesta: LISTO
═══════════════════════════════════════
✅ SEÑAL LISTO RECIBIDA
   Tiempo total: 4523ms
═══════════════════════════════════════
```

### Formato de Logs del Filtro

```
═══════════════════════════════════════
📊 Analizando 1 detecciones
   ROI: 331x345 = 114195 px²
   🔹 Plásticos: 1
   🔹 Otros materiales: 0
─────────────────────────────────────
🔍 plastic
   🎯 Confianza: 34%
   📐 Tamaño: 222x345 px
   📊 Área: 67% del ROI
   🚫 FILTRADO: Confianza baja (34%) + área grande (67%)
   💡 Probablemente es el plato (falso positivo)
═══════════════════════════════════════
✅ Resultado: 0/1 detecciones válidas
═══════════════════════════════════════
```

---

## Sistema de Deposito Automatico

### MaterialCategory (4 Categorias)

El sistema mapea las 12 clases YOLO a 4 categorias fisicas del bote:

| Categoria | Clases YOLO | Giro | Inclinacion | Color |
|-----------|-------------|------|-------------|-------|
| **Plastico** | plastic, plastic-pet, plastic-pe_hd, plastic-pp, plastic-ps, plastic-others | -30° | -45° | Azul |
| **Papel/Carton** | paper, cardboard | -30° | +45° | Verde |
| **Aluminio/Metal** | metal, glass | +59° | -45° | Morado |
| **General** | biological, trash, otros | +59° | +45° | Naranja |

### DetectionStabilityTracker

Verifica que el mismo material se detecte consistentemente durante **2 segundos** antes de ejecutar el deposito:

```kotlin
object DetectionStabilityTracker {
    const val STABILITY_DURATION_MS = 2000L  // 2 segundos

    fun update(detection: Detection?): MaterialCategory?
    fun getProgress(): Float  // 0.0 a 1.0
    fun getCurrentCategory(): MaterialCategory?
    fun reset()
}
```

**Flujo de estabilidad:**
```
Deteccion → Clasificar categoria → Mismo material 2s? → Deposito automatico
     ↑                                    ↓ (si cambia)
     └────────────────────────────────────┘
```

### Integracion con ESP32 (Bluetooth) - Protocolo v2

La pantalla incluye un boton de conexion minimalista en la esquina superior derecha:

- **OFF (Rojo)**: ESP32 desconectado - click para conectar
- **ON (Verde)**: ESP32 conectado - click para desconectar

#### Protocolo de Comunicacion Bidireccional

El sistema usa comunicacion **bidireccional precisa** sin timers:

```
┌─────────────────────┐                    ┌─────────────────────┐
│      Android        │                    │       ESP32         │
│   (Clasificador)    │                    │   (Bote BioWay)     │
└─────────────────────┘                    └─────────────────────┘
         │                                          │
         │  1. DEPOSITAR:Plástico                   │
         │─────────────────────────────────────────>│
         │                                          │ (ejecuta secuencia)
         │                                          │ - GIRO a posicion
         │                                          │ - INCL a posicion
         │                                          │ - Depositar
         │                                          │ - RESET a home
         │  2. LISTO                                │
         │<─────────────────────────────────────────│
         │                                          │
         │  (Android resume deteccion)              │
```

**Comandos ESP32:**
- `DEPOSITAR:PLASTICO` - Depositar en compartimento plastico
- `DEPOSITAR:PAPEL` - Depositar en compartimento papel/carton
- `DEPOSITAR:ALUMINIO` - Depositar en compartimento metal
- `DEPOSITAR:GENERAL` - Depositar en compartimento general

**Respuesta ESP32:**
- `LISTO` - Secuencia completada, Android puede continuar

#### Flujo de Deposito

Cuando hay conexion activa y se detecta material estable por 2s:
1. Se muestra barra de progreso llenandose
2. Al completar, se ejecuta `BluetoothManager.depositarYEsperarListo(categoria)`
3. Android muestra "Esperando confirmacion del ESP32..."
4. El bote gira e inclina automaticamente (secuencia completa)
5. ESP32 envia "LISTO" cuando termina
6. Se muestra confirmacion "✓ [Material] depositado"
7. Se resetea para nueva deteccion

---

## Ajuste de Parametros

### Para ajustar sensibilidad del filtro:

```kotlin
// En BackgroundPlateFilter object:

// Bajar si objetos reales con baja confianza son filtrados
BackgroundPlateFilter.minConfidenceThreshold = 0.40f  // Default: 0.50f

// Subir si el plato no se filtra correctamente
BackgroundPlateFilter.suspiciousAreaThreshold = 0.40f  // Default: 0.35f
```

### Para ajustar el detector YOLO:

```kotlin
// En DetectionScreen, LaunchedEffect:

detector.confidenceThreshold = 0.20f  // Bajar para mas detecciones
detector.iouThreshold = 0.5f          // Subir para menos overlap
detector.numItemsThreshold = 10       // Limitar max detecciones
```

---

## Dependencias Clave

```kotlin
// TensorFlow Lite
implementation("org.tensorflow:tensorflow-lite:2.14.0")
implementation("org.tensorflow:tensorflow-lite-gpu:2.14.0")

// CameraX
implementation("androidx.camera:camera-core:1.3.1")
implementation("androidx.camera:camera-camera2:1.3.1")
implementation("androidx.camera:camera-lifecycle:1.3.1")
implementation("androidx.camera:camera-view:1.3.1")

// Permisos
implementation("com.google.accompanist:accompanist-permissions:0.32.0")
```

---

## Archivos Relacionados

| Archivo | Descripcion |
|---------|-------------|
| `ClasificadorBoteYOLOScreen.kt` | Pantalla principal (este archivo) |
| `WasteDetector.kt` | Wrapper TFLite para YOLO (`com.ultralytics.yolo`) |
| `BluetoothManager.kt` | Comunicacion Bluetooth con ESP32 (Protocolo v2) |
| `BoteBioWayMainScreen.kt` | Menu principal del Bote BioWay |
| `BioWayNavHost.kt` | Navegacion (ruta: `BoteBioWayClasificadorYOLO`) |
| `waste_detector_v2.tflite` | Modelo YOLO (assets/models/) |
| `waste_detector_labels.txt` | Etiquetas del modelo (assets/labels/) |
| `ESP32_BoteBioWay.txt` | Codigo Arduino del ESP32 (soporte DEPOSITAR/LISTO) |

---

## Notas de Desarrollo

1. **Limpieza de camara:** Siempre usar `DisposableEffect` para liberar `cameraProvider?.unbindAll()` al salir.

2. **Estado del ROI:** Usar `rememberUpdatedState` para mantener referencia actualizada en gestos de arrastre.

3. **BackHandler:** Implementado para navegacion correcta con boton de retroceso del sistema.

4. **Filtro persistente:** `BackgroundPlateFilter` es un `object` singleton, sus parametros persisten durante la sesion.

5. **Coordenadas:** Las detecciones se hacen en espacio del ROI recortado y luego se ajustan al espacio de imagen completa con `adjustDetectionsToFullImage()`.

---

## Proximos Pasos / TODOs

- [x] Integrar con BluetoothManager para enviar material detectado al ESP32
- [x] Clasificar 12 clases YOLO en 4 categorias del bote
- [x] Deteccion estable por 2 segundos antes de depositar
- [x] Boton minimalista de conexion ESP32
- [x] Barra de progreso de estabilidad
- [x] **Protocolo v2**: Comunicacion bidireccional con senal LISTO
- [x] ESP32 envia LISTO al terminar secuencia de deposito
- [x] Android espera LISTO antes de resumir deteccion (sin timers)
- [ ] Historial de detecciones/depositos
- [ ] Animaciones de deposito exitoso
- [ ] Sonidos/vibracion al depositar
- [ ] Contador de materiales reciclados por sesion
