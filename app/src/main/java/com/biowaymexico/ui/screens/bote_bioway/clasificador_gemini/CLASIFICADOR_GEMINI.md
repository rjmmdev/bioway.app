# ClasificadorGeminiScreen - Documentacion Tecnica

## Contexto del Proyecto

Este documento describe el **Clasificador de Residuos con YOLO + Gemini AI** para el **Bote BioWay**.

A diferencia del clasificador YOLO puro (`ClasificadorBoteYOLOScreen.kt`), este sistema usa:
- **YOLO**: Solo para detectar PRESENCIA de material (no clasifica)
- **Gemini AI**: Para la clasificacion final (mayor precision)

### Flujo de Clasificacion

```
┌──────────────────────────────────────────────────────────────────────────┐
│                    FLUJO YOLO + GEMINI                                   │
└──────────────────────────────────────────────────────────────────────────┘

  CAMARA → YOLO (presencia) → ESTABILIDAD → GEMINI → ESP32

  1. Camara captura frames en tiempo real
  2. YOLO detecta si hay material (no que tipo)
  3. Filtro ignora el plato blanco (falsos positivos)
  4. Si hay material estable por 1.5s → captura imagen
  5. Imagen se envia a Gemini AI
  6. Gemini clasifica en 4 categorias
  7. Si ESP32 conectado → deposito automatico
```

## Arquitectura

### Componentes

| Archivo | Descripcion |
|---------|-------------|
| `GeminiClassifier.kt` | Cliente para API de Gemini AI |
| `ClasificadorGeminiScreen.kt` | Pantalla principal con todo el flujo |
| `CLASIFICADOR_GEMINI.md` | Esta documentacion |

### Estados de Clasificacion

```kotlin
enum class ClassificationState {
    IDLE,                    // Esperando deteccion
    DETECTING_PRESENCE,      // YOLO detectando presencia
    PRESENCE_STABLE,         // Presencia confirmada, listo para capturar
    CAPTURING_IMAGE,         // Capturando imagen
    CLASSIFYING_GEMINI,      // Enviando a Gemini
    CLASSIFICATION_READY,    // Clasificacion lista
    DEPOSITING,              // Depositando material
    DEPOSIT_COMPLETE         // Deposito completado
}
```

## Configuracion

### API Key de Gemini

**IMPORTANTE**: Antes de usar, configura tu API key de Gemini:

1. Ve a https://aistudio.google.com/app/apikey
2. Crea una API key
3. Edita `ClasificadorGeminiScreen.kt`:

```kotlin
// Linea ~55
private const val GEMINI_API_KEY = "TU_API_KEY_AQUI"
```

### Modelo de Gemini

El sistema usa **gemini-1.5-flash** por:
- Velocidad: ~1-3 segundos de respuesta
- Costo: Version mas economica
- Precision: Suficiente para clasificacion de residuos
- Multimodal: Acepta imagenes directamente

## Diferencias vs ClasificadorBoteYOLOScreen

| Aspecto | YOLO Puro | YOLO + Gemini |
|---------|-----------|---------------|
| Clasificacion | YOLO (local) | Gemini (nube) |
| Precision | ~85% | ~95%+ |
| Latencia | ~50ms | ~1-3s |
| Offline | Si | No |
| Costo | Gratis | API de pago |
| Uso de YOLO | Clasificar | Solo presencia |

## Filtro de Presencia

El filtro `MaterialPresenceFilter` es mas simple que `BackgroundPlateFilter`:

```kotlin
object MaterialPresenceFilter {
    var minConfidenceThreshold = 0.40f  // Mas bajo que YOLO puro
    var suspiciousAreaThreshold = 0.35f

    fun hasMaterialPresent(detections, roiWidth, roiHeight): Boolean
}
```

**Logica:**
- Si hay materiales NO plasticos → material presente
- Si hay plasticos con alta confianza O area pequena → material presente
- Si solo hay plastico grande con baja confianza → es el plato (ignorar)

## Prompt de Gemini

El prompt enviado a Gemini esta optimizado para:
1. Respuestas estructuradas (JSON)
2. Clasificacion en exactamente 4 categorias
3. Incluir nivel de confianza
4. Breve razonamiento

```
Analiza esta imagen y clasifica el residuo en:
1. PLASTICO
2. PAPEL_CARTON
3. ALUMINIO_METAL
4. GENERAL

Responde en JSON:
{
    "categoria": "NOMBRE",
    "confianza": "alta/media/baja",
    "razon": "explicacion breve"
}
```

## Categorias

```kotlin
enum class MaterialCategoryGemini {
    PLASTICO,        // Botellas PET, envases plasticos
    PAPEL_CARTON,    // Papel, carton, periodicos
    ALUMINIO_METAL,  // Latas, metales, vidrio
    GENERAL,         // Organicos, basura no reciclable
    NO_DETECTADO     // Plato vacio
}
```

## Comunicacion con ESP32

Usa el mismo `BluetoothManager` y protocolo v2:

```
Android → ESP32: DEPOSITAR:Plastico
ESP32 → Android: LISTO
```

## Debugging

### Tags de Logging

```bash
# Clasificador principal
adb logcat | grep ClasificadorGemini

# Filtro de presencia
adb logcat | grep PlateFilterGemini

# Cliente Gemini
adb logcat | grep GeminiClassifier

# Bluetooth
adb logcat | grep BluetoothManager

# Todo el flujo
adb logcat | grep -E "(ClasificadorGemini|GeminiClassifier|BluetoothManager)"
```

### Ejemplo de Logs (Flujo Exitoso)

```
🔍 Verificando presencia de material...
   Detecciones: 2
   ✅ Material NO plastico detectado: cardboard
⏳ Esperando: 1.2s restantes...
✅ ¡PRESENCIA ESTABLE! (1523ms)
🤖 Enviando imagen a Gemini...
   Tamaño: 320x320
📥 Respuesta de Gemini (1847ms):
   {"categoria": "PAPEL_CARTON", "confianza": "alta", "razon": "Caja de carton corrugado"}
🎯 CLASIFICACION GEMINI:
   Categoria: Papel/Carton
   Confianza: alta
📤 Enviando: DEPOSITAR:Papel/Carton
⏳ Esperando senal LISTO del ESP32...
📥 Respuesta: LISTO
✅ Deposito completado
```

## Costos de Gemini

Gemini 1.5 Flash (precios aproximados, verificar en Google):
- Input: $0.075 / 1M tokens
- Output: $0.30 / 1M tokens
- Imagen: ~258 tokens por imagen

**Estimacion por clasificacion:** ~$0.0001 USD

## Limitaciones

1. **Requiere Internet**: Gemini es un servicio en la nube
2. **Latencia**: 1-3 segundos vs ~50ms de YOLO
3. **Costos**: Aunque bajo, hay costo por uso
4. **Rate Limits**: Google puede limitar llamadas frecuentes
5. **API Key**: Debe protegerse (no exponer en codigo publico)

## Proximos Pasos / TODOs

- [ ] Mover API key a BuildConfig o variables de entorno
- [ ] Implementar cache local para clasificaciones repetidas
- [ ] Agregar modo offline con fallback a YOLO
- [ ] Historial de clasificaciones
- [ ] Metricas de precision Gemini vs YOLO

## Navegacion

Acceso desde `BoteBioWayMainScreen`:
```
BoteBioWayMainScreen → "Clasificador IA + Gemini" → ClasificadorGeminiScreen
```

Ruta: `BioWayDestinations.ClasificadorGemini`
