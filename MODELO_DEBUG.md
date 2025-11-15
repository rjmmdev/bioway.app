# Debug del Modelo de Clasificación

## Problema Detectado
El clasificador detecta todo como "Vidrio" - esto indica que el orden de las etiquetas no coincide con la salida del modelo.

## Cambios Implementados

### 1. Logging de Debug
Ahora el clasificador imprime en Logcat todas las probabilidades de cada clase:

```
D/ClasificadorResiduos: === Resultados de clasificación ===
D/ClasificadorResiduos: [0] Basura: 0.05 (5%)
D/ClasificadorResiduos: [1] Cartón: 0.10 (10%)
D/ClasificadorResiduos: [2] Vidrio: 0.80 (80%)
...
```

### 2. Orden de Etiquetas Actualizado

**Orden Anterior (incorrecto):**
```kotlin
"Vidrio", "Papel", "Cartón", "Plástico", "Metal", "Basura", "Orgánico"
```

**Orden Actual (alfabético estándar):**
```kotlin
"Basura", "Cartón", "Vidrio", "Metal", "Orgánico", "Papel", "Plástico"
```

### 3. Cómo Verificar en Android Studio

1. Abre Logcat (View > Tool Windows > Logcat)
2. Filtra por "ClasificadorResiduos"
3. Captura una foto de un objeto conocido (ej: botella de plástico, papel, etc.)
4. Observa los logs - la categoría con mayor % debería coincidir con el objeto real

### 4. Órdenes Alternativos para Probar

Si el orden actual no funciona, prueba estos en `ClasificadorResiduos.kt`:

**Opción A - Orden alfabético inglés:**
```kotlin
private val etiquetas = listOf(
    "Cartón",      // 0 - Cardboard
    "Vidrio",      // 1 - Glass
    "Metal",       // 2 - Metal
    "Orgánico",    // 3 - Organic
    "Papel",       // 4 - Paper
    "Plástico",    // 5 - Plastic
    "Basura"       // 6 - Trash
)
```

**Opción B - Orden inverso:**
```kotlin
private val etiquetas = listOf(
    "Plástico",    // 0
    "Papel",       // 1
    "Orgánico",    // 2
    "Metal",       // 3
    "Vidrio",      // 4
    "Cartón",      // 5
    "Basura"       // 6
)
```

**Opción C - Orden por tipo de material:**
```kotlin
private val etiquetas = listOf(
    "Papel",       // 0
    "Cartón",      // 1
    "Plástico",    // 2
    "Vidrio",      // 3
    "Metal",       // 4
    "Orgánico",    // 5
    "Basura"       // 6
)
```

### 5. Proceso de Debugging

1. **Captura una imagen de prueba** de un objeto conocido (ej: botella de plástico)
2. **Revisa Logcat** y anota qué índice [0-6] tiene el porcentaje más alto
3. **Compara** con el tipo de objeto real
4. **Ajusta el orden** de etiquetas para que el índice con mayor % corresponda al objeto correcto

**Ejemplo:**
Si capturas una botella de plástico y el log muestra:
```
[6] Plástico: 0.85 (85%)  <- Mayor probabilidad
```
Entonces el índice 6 corresponde a "Plástico" ✓ Correcto

Si en cambio muestra:
```
[2] Vidrio: 0.85 (85%)  <- Mayor probabilidad
```
Pero sabes que es plástico, entonces el índice 2 debería ser "Plástico", no "Vidrio".

### 6. Normalización de Imagen

El preprocesamiento actual usa normalización estándar [0, 1]:
```kotlin
buffer.putFloat(r / 255.0f)
buffer.putFloat(g / 255.0f)
buffer.putFloat(b / 255.0f)
```

Si después de ajustar etiquetas sigue sin funcionar, puede ser necesario probar:

**ImageNet normalización:**
```kotlin
// Mean y Std de ImageNet
val mean = floatArrayOf(0.485f, 0.456f, 0.406f)
val std = floatArrayOf(0.229f, 0.224f, 0.225f)

buffer.putFloat((r / 255.0f - mean[0]) / std[0])
buffer.putFloat((g / 255.0f - mean[1]) / std[1])
buffer.putFloat((b / 255.0f - mean[2]) / std[2])
```

### 7. Contacto con Desarrollador del Modelo

Si tienes acceso al creador del modelo, pregunta:
1. ¿Cuál es el orden exacto de las clases de salida?
2. ¿Qué normalización espera el modelo? (rango [0,1], [-1,1], ImageNet, etc.)
3. ¿Formato de entrada RGB o BGR?
4. ¿Tamaño de entrada? (confirmado: 224x224)

## Verificación Rápida

Después de compilar con los cambios:

1. Abre la app en el emulador/dispositivo
2. Ve al Clasificador IA desde el dashboard
3. Captura fotos de objetos conocidos:
   - Una botella de plástico
   - Una hoja de papel
   - Una botella de vidrio
   - Una lata de metal
   - Cartón
4. Verifica en Logcat qué categoría obtiene mayor porcentaje
5. Si no coinciden, ajusta el orden según los índices de los logs
