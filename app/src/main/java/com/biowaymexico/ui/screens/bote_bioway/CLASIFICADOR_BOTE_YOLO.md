# ClasificadorBoteYOLOScreen - Documentacion Tecnica

## Contexto del Proyecto (IMPORTANTE para futuras sesiones)

Este documento describe el **Clasificador de Residuos con IA** para el **Bote BioWay**, un bote de basura inteligente que:

1. **Detecta residuos** usando un modelo YOLOv8 en tiempo real via camara
2. **Clasifica** los residuos en 4 categorias fisicas (Plastico, Papel, Metal, General)
3. **Espera estabilidad** de 2 segundos para confirmar deteccion
4. **Comunica con ESP32** via Bluetooth para mover servomotores
5. **Deposita automaticamente** el residuo en el compartimento correcto

### Estado Actual (Noviembre 2024)

| Componente | Estado | Notas |
|------------|--------|-------|
| Deteccion YOLO | ✅ Completo | Modelo `waste_detector_v2.tflite` funcionando |
| Filtro plato blanco | ✅ Completo | `BackgroundPlateFilter` filtra falsos positivos |
| Clasificacion 4 categorias | ✅ Completo | `MaterialCategory` enum |
| Estabilidad 2 segundos | ✅ Completo | `DetectionStabilityTracker` |
| Conexion Bluetooth ESP32 | ✅ Completo | `BluetoothManager` con handshake |
| **Protocolo v2 (LISTO)** | ✅ Completo | Comunicacion bidireccional sin timers |
| UI de deposito | ✅ Completo | Barra progreso, estados, confirmacion |

### Arquitectura de Comunicacion (Protocolo v2)

```
┌──────────────────────────────────────────────────────────────────────────┐
│                        FLUJO COMPLETO DE DEPOSITO                        │
└──────────────────────────────────────────────────────────────────────────┘

  ANDROID (ClasificadorBoteYOLOScreen)          ESP32 (BoteBioWay)
  ═══════════════════════════════════          ══════════════════════

  1. Detecta residuo con YOLO
     ↓
  2. Filtra plato blanco (BackgroundPlateFilter)
     ↓
  3. Clasifica en 4 categorias (MaterialCategory)
     ↓
  4. Espera 2s de estabilidad (DetectionStabilityTracker)
     ↓
  5. Si ESP32 conectado:
     ├─────── DEPOSITAR:Plástico ──────────────→ 6. Recibe comando
     │                                              ↓
     │  (UI: "Esperando confirmacion...")       7. Ejecuta secuencia:
     │                                              - moverGiro(-30)
     │                                              - moverInclinacion(-45)
     │                                              - delay(400) depositar
     │                                              - moverInclinacion(0)
     │                                              - moverGiro(-80) home
     │                                              ↓
     │←──────────── LISTO ─────────────────────  8. Envia LISTO
     ↓
  9. UI: "✓ Plástico depositado"
     ↓
  10. Reset para nueva deteccion
```

---

## Descripcion General

`ClasificadorBoteYOLOScreen.kt` es la pantalla principal ubicada en:
```
app/src/main/java/com/biowaymexico/ui/screens/bote_bioway/ClasificadorBoteYOLOScreen.kt
```

**Navegacion:** `BoteBioWayMainScreen` → "Clasificador IA v2" → `ClasificadorBoteYOLOScreen`

---

## Componentes del Sistema

### 1. ClasificadorBoteYOLOScreen.kt (1939 lineas)

Contiene TODO el codigo de la pantalla incluyendo:

| Componente | Lineas | Descripcion |
|------------|--------|-------------|
| `BackgroundPlateFilter` | 80-232 | Object singleton para filtrar plato blanco |
| `MaterialCategory` | 239-313 | Enum con 4 categorias y mapeo de clases YOLO |
| `DetectionStabilityTracker` | 319-407 | Object singleton para estabilidad de 2s |
| `ROIRect` | 414-424 | Data class para region de interes |
| `YoloScreenState` | 429-433 | Enum de estados de pantalla |
| `ClasificadorBoteYOLOScreen` | 443-534 | Composable principal |
| `ROIConfigurationScreen` | 538-634 | Paso 1: Configurar area |
| `DetectionScreen` | 967-1395 | Paso 2: Deteccion activa |
| Funciones de imagen | 1483-1630 | Procesamiento de bitmap |
| UI Components | 1632-1939 | Overlays, paneles, etc. |

### 2. BluetoothManager.kt

**Ubicacion:** `app/src/main/java/com/biowaymexico/utils/BluetoothManager.kt`

```kotlin
class BluetoothManager {
    // Conexion
    suspend fun conectarConHandshake(): Result<Unit>
    fun desconectar()
    fun estaConectado(): Boolean

    // Protocolo v1 (DEPRECATED - mantener por compatibilidad)
    suspend fun enviarMaterial(material: String): Result<Unit>

    // Protocolo v2 (ACTUAL - usar este)
    suspend fun depositarYEsperarListo(categoria: String): Result<Unit>
}
```

**Constantes importantes:**
```kotlin
const val ESP32_NAME = "ESP32_Detector"  // Nombre Bluetooth del ESP32
const val LISTO_TIMEOUT_MS = 15000L      // 15 segundos max espera LISTO
val UUID_SPP = "00001101-0000-1000-8000-00805F9B34FB"  // UUID SPP
```

### 3. ESP32_BoteBioWay.txt (Codigo Arduino)

**Ubicacion:** Raiz del proyecto `ESP32_BoteBioWay.txt`

**Comandos soportados:**
| Comando | Respuesta | Descripcion |
|---------|-----------|-------------|
| `PING` | `PONG` | Handshake de conexion |
| `DEPOSITAR:CATEGORIA` | `LISTO` | Secuencia completa de deposito |
| `GIRO:XXX` | `OK` | Mover servo giro (-80 a 160) |
| `INCL:XXX` | `OK` | Mover servos inclinacion (-45 a 45) |
| `RESET` | `OK` | Volver a posicion inicial |

**Pines ESP32:**
- Servo 13: GIRO (plataforma giratoria)
- Servo 12 + 14: INCLINACION (trabajan juntos, espalda con espalda)

### 4. WasteDetector.kt

**Ubicacion:** `app/src/main/java/com/ultralytics/yolo/WasteDetector.kt`

Wrapper TensorFlow Lite para el modelo YOLO. Parametros configurables:
```kotlin
detector.confidenceThreshold = 0.25f  // Umbral confianza
detector.iouThreshold = 0.4f          // Umbral IoU para NMS
detector.numItemsThreshold = 30       // Max detecciones
```

---

## Estados de la Pantalla

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
└─────────────────────┘   - Deposito automatico si ESP32 conectado
```

---

## BackgroundPlateFilter - Filtrado de Plato Blanco

### Problema

El bote BioWay tiene un plato/superficie blanca donde se colocan los residuos. El modelo YOLO detecta este plato como "plastic" con baja confianza.

### Solucion

```kotlin
object BackgroundPlateFilter {
    var minConfidenceThreshold = 0.50f   // Umbral de confianza
    var suspiciousAreaThreshold = 0.35f  // Umbral de area sospechosa
}
```

**Logica:**
- Si confianza < 50% Y area > 35% → Filtrar (es el plato)
- Si hay multiples plasticos → Filtrar el mas grande
- Otros materiales → Aceptar siempre

---

## MaterialCategory - Clasificacion en 4 Categorias

```kotlin
enum class MaterialCategory(
    val displayName: String,
    val emoji: String,
    val giro: Int,
    val inclinacion: Int,
    val color: Long
) {
    PLASTICO("Plástico", "♻️", -30, -45, 0xFF2196F3),
    PAPEL_CARTON("Papel/Cartón", "📄", -30, 45, 0xFF4CAF50),
    ALUMINIO_METAL("Aluminio/Metal", "🥫", 59, -45, 0xFF9C27B0),
    GENERAL("General", "🗑️", 59, 45, 0xFFFF9800);
}
```

**Mapeo de clases YOLO:**
| Categoria | Clases YOLO |
|-----------|-------------|
| PLASTICO | plastic, plastic-pet, plastic-pe_hd, plastic-pp, plastic-ps, plastic-others |
| PAPEL_CARTON | paper, cardboard |
| ALUMINIO_METAL | metal, glass |
| GENERAL | biological, trash, otros |

---

## DetectionStabilityTracker - Estabilidad de Deteccion

```kotlin
object DetectionStabilityTracker {
    private const val STABILITY_DURATION_MS = 2000L  // 2 segundos

    fun update(detection: Detection?): MaterialCategory?  // Retorna categoria si estable
    fun getProgress(): Float  // 0.0 a 1.0 para barra de progreso
    fun getCurrentCategory(): MaterialCategory?
    fun reset()  // Llamar despues de depositar
}
```

**Flujo:**
1. Cada frame llama `update()` con la deteccion principal
2. Si cambia la categoria, se reinicia el contador
3. Si pasan 2 segundos con misma categoria, retorna la categoria (estable)
4. Despues de depositar, llamar `reset()`

---

## Protocolo v2 - Comunicacion Bidireccional

### Por que Protocolo v2?

El protocolo v1 usaba timers arbitrarios:
```kotlin
// PROTOCOLO v1 (DEPRECATED)
bluetoothManager.enviarMaterial(categoria)
delay(2000)  // Esperar 2s arbitrarios
// Problema: No sabemos si ESP32 termino
```

El protocolo v2 usa comunicacion precisa:
```kotlin
// PROTOCOLO v2 (ACTUAL)
bluetoothManager.depositarYEsperarListo(categoria)
// Retorna cuando ESP32 envia "LISTO"
// Sin timers arbitrarios
```

### Implementacion Android (BluetoothManager.kt)

```kotlin
suspend fun depositarYEsperarListo(categoria: String): Result<Unit> = withContext(Dispatchers.IO) {
    // 1. Enviar comando
    outputStream?.write("DEPOSITAR:$categoria\n".toByteArray())

    // 2. Esperar LISTO (max 15 segundos)
    while (System.currentTimeMillis() - startTime < LISTO_TIMEOUT_MS) {
        if (inputStream?.available() > 0) {
            val response = // leer respuesta
            if (response == "LISTO") {
                return@withContext Result.success(Unit)
            }
        }
        Thread.sleep(50)
    }

    // 3. Timeout
    Result.failure(Exception("Timeout esperando LISTO"))
}
```

### Implementacion ESP32 (ESP32_BoteBioWay.txt)

```cpp
if (comando.startsWith("DEPOSITAR:")) {
    String categoria = comando.substring(10);

    // Determinar movimientos segun categoria
    int giro, inclinacion;
    if (categoria.indexOf("PLASTICO") >= 0) {
        giro = -30; inclinacion = -45;
    } else if (categoria.indexOf("PAPEL") >= 0) {
        giro = -30; inclinacion = 45;
    } // ... etc

    // Ejecutar secuencia completa
    moverGiro(giro);
    delay(1000);
    moverInclinacion(inclinacion);
    delay(1000);
    delay(400);  // Depositar
    moverInclinacion(0);
    delay(1000);
    moverGiro(-80);  // Home
    delay(500);

    // Enviar LISTO
    SerialBT.println("LISTO");
    SerialBT.flush();
}
```

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
DetectionStabilityTracker.update() ──→ Verificar estabilidad
       │
       ▼
Si estable + ESP32 conectado → depositarYEsperarListo()
```

---

## Debugging

### Tags de Logging

```bash
# Clasificador principal
adb logcat | grep ClasificadorBoteYOLO

# Filtro del plato
adb logcat | grep PlateFilter

# Bluetooth/ESP32
adb logcat | grep BluetoothManager

# Ver todo el flujo de deposito
adb logcat | grep -E "(ClasificadorBoteYOLO|BluetoothManager)"

# Tracker de estabilidad
adb logcat | grep StabilityTracker

# Categoria de material
adb logcat | grep MaterialCategory
```

### Ejemplo de Logs (Deposito Exitoso)

```
═══════════════════════════════════════
🎯 MATERIAL ESTABLE: Plástico
   Iniciando depósito con protocolo v2...
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
✅ ESP32 confirmó LISTO - Depósito completado: Plástico
🔄 Reseteando estado para nueva detección...
✅ Estado reseteado - Listo para nueva detección
```

---

## Ajuste de Parametros

### Filtro del Plato

```kotlin
// En BackgroundPlateFilter:
BackgroundPlateFilter.minConfidenceThreshold = 0.50f   // Default
BackgroundPlateFilter.suspiciousAreaThreshold = 0.35f  // Default

// Si objetos reales con baja confianza son filtrados → bajar minConfidenceThreshold
// Si el plato no se filtra → subir suspiciousAreaThreshold
```

### Detector YOLO

```kotlin
// En DetectionScreen LaunchedEffect:
detector.confidenceThreshold = 0.25f  // Default: mas bajo = mas detecciones
detector.iouThreshold = 0.4f          // Default: mas alto = menos overlap
detector.numItemsThreshold = 30       // Default: max detecciones
```

### Tiempo de Estabilidad

```kotlin
// En DetectionStabilityTracker:
private const val STABILITY_DURATION_MS = 2000L  // 2 segundos

// Aumentar si hay falsos positivos frecuentes
// Disminuir si el sistema es muy lento
```

---

## Archivos del Sistema

| Archivo | Ubicacion | Descripcion |
|---------|-----------|-------------|
| `ClasificadorBoteYOLOScreen.kt` | `ui/screens/bote_bioway/` | Pantalla principal |
| `BluetoothManager.kt` | `utils/` | Comunicacion Bluetooth |
| `WasteDetector.kt` | `com.ultralytics.yolo/` | Wrapper TFLite YOLO |
| `ESP32_BoteBioWay.txt` | Raiz proyecto | Codigo Arduino ESP32 |
| `waste_detector_v2.tflite` | `assets/models/` | Modelo YOLO |
| `waste_detector_labels.txt` | `assets/labels/` | Etiquetas modelo |
| `BoteBioWayMainScreen.kt` | `ui/screens/bote_bioway/` | Menu principal |

---

## Notas de Desarrollo

1. **Coroutines en Compose:** Usar patron de trigger state para ejecutar coroutines desde LaunchedEffect:
   ```kotlin
   var trigger by remember { mutableStateOf(0) }
   LaunchedEffect(trigger) {
       if (trigger > 0) {
           // ejecutar coroutine
       }
   }
   // Para disparar: trigger++
   ```

2. **Limpieza de camara:** Siempre usar `DisposableEffect` para liberar `cameraProvider?.unbindAll()`.

3. **ROI en gestos:** Usar `rememberUpdatedState` para mantener referencia actualizada en lambdas de gestos.

4. **Singletons:** `BackgroundPlateFilter` y `DetectionStabilityTracker` son singletons - sus parametros persisten durante la sesion.

5. **ESP32 Bluetooth:** El nombre debe ser exactamente `ESP32_Detector` para que Android lo encuentre.

---

## Proximos Pasos / TODOs

### Completados
- [x] Integrar con BluetoothManager para enviar material detectado al ESP32
- [x] Clasificar 12 clases YOLO en 4 categorias del bote
- [x] Deteccion estable por 2 segundos antes de depositar
- [x] Boton minimalista de conexion ESP32
- [x] Barra de progreso de estabilidad
- [x] **Protocolo v2**: Comunicacion bidireccional con senal LISTO
- [x] ESP32 envia LISTO al terminar secuencia de deposito
- [x] Android espera LISTO antes de resumir deteccion (sin timers)

### Pendientes
- [ ] Historial de detecciones/depositos
- [ ] Animaciones de deposito exitoso
- [ ] Sonidos/vibracion al depositar
- [ ] Contador de materiales reciclados por sesion
- [ ] Manejo de errores mas robusto (reconexion automatica)
- [ ] Tests unitarios para DetectionStabilityTracker y BackgroundPlateFilter

---

## Referencia Rapida para Futuras Sesiones

**Para modificar el tiempo de estabilidad:**
→ `DetectionStabilityTracker.STABILITY_DURATION_MS` (linea ~321)

**Para modificar posiciones de servos:**
→ `MaterialCategory` enum (lineas 246-273)
→ `ESP32_BoteBioWay.txt` funcion `procesarComando()`

**Para modificar filtro del plato:**
→ `BackgroundPlateFilter` object (lineas 80-232)

**Para modificar timeout de LISTO:**
→ `BluetoothManager.LISTO_TIMEOUT_MS` (linea ~32)

**Para agregar nueva categoria:**
1. Agregar a `MaterialCategory` enum
2. Agregar mapeo en `MaterialCategory.fromYoloClass()`
3. Agregar handler en ESP32 `procesarComando()`
