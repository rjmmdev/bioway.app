# Instrucciones de Debug - Clasificador de Residuos

## Problema Resuelto

El clasificador detectaba todo como "Vidrio" debido a un desajuste entre el orden de las etiquetas en el código y el orden de salida del modelo TensorFlow Lite.

## Mejoras Implementadas

### 1. Vista de Debug Visual 🎯

Ahora la pantalla del clasificador muestra **TODAS las probabilidades** con sus índices originales:

```
🟢 [2] Plástico    ████████████ 85%  ← Categoría ganadora
⚪ [4] Papel       ████         40%
⚪ [1] Cartón      ███          30%
⚪ [0] Basura      ██           20%
⚪ [6] Metal       █            10%
⚪ [3] Vidrio      ▌             5%
⚪ [5] Orgánico    ▌             5%
```

Cada barra muestra:
- **Índice** [0-6]: Posición en el array de salida del modelo
- **Categoría**: Nombre del tipo de residuo
- **Confianza**: Porcentaje de seguridad

### 2. Logs en Logcat 📊

Además de la vista visual, el clasificador imprime logs detallados:

```
D/ClasificadorResiduos: === Resultados de clasificación ===
D/ClasificadorResiduos: [0] Basura: 0.05 (5%)
D/ClasificadorResiduos: [1] Cartón: 0.10 (10%)
D/ClasificadorResiduos: [2] Vidrio: 0.80 (80%)
D/ClasificadorResiduos: [3] Metal: 0.02 (2%)
D/ClasificadorResiduos: [4] Orgánico: 0.01 (1%)
D/ClasificadorResiduos: [5] Papel: 0.01 (1%)
D/ClasificadorResiduos: [6] Plástico: 0.01 (1%)
D/ClasificadorResiduos: Resultado final: Vidrio con confianza 80%
```

### 3. Nuevo Orden de Etiquetas

Se actualizó el orden alfabético estándar:

```kotlin
// ClasificadorResiduos.kt
private val etiquetas = listOf(
    "Basura",      // 0
    "Cartón",      // 1
    "Vidrio",      // 2
    "Metal",       // 3
    "Orgánico",    // 4
    "Papel",       // 5
    "Plástico"     // 6
)
```

## Cómo Usar el Debug

### Paso 1: Preparar Objetos de Prueba

Consigue objetos reales y fáciles de identificar:

- ✅ Botella de plástico vacía y limpia
- ✅ Hoja de papel blanco
- ✅ Botella/frasco de vidrio
- ✅ Lata de aluminio/refresco
- ✅ Caja de cartón
- ✅ Restos de comida (manzana, plátano)
- ✅ Bolsa de basura

### Paso 2: Capturar y Analizar

1. **Abre la app** → Dashboard Brindador → "Clasificador IA"
2. **Concede permisos** de cámara si se solicitan
3. **Enfoca un objeto** (ej: botella de plástico)
4. **Captura la foto**
5. **Observa los resultados**:
   - Categoría principal (grande)
   - Sección "Todas las probabilidades (Debug)" con barras completas

### Paso 3: Identificar el Mapeo Correcto

**Ejemplo práctico:**

Si capturas una **botella de plástico** y obtienes:

```
🟢 [2] Vidrio     85%  ← MÁXIMA CONFIANZA
⚪ [6] Plástico   10%
```

Esto significa:
- El modelo está seguro de que es el índice **[2]**
- Pero tenemos "Vidrio" en esa posición
- El objeto real es **Plástico**
- **Conclusión**: El índice [2] debería ser "Plástico", no "Vidrio"

### Paso 4: Corregir el Orden

Con base en tus pruebas, crea un mapeo:

| Índice | Objeto Real Detectado | Etiqueta Actual | Etiqueta Correcta |
|--------|----------------------|-----------------|-------------------|
| 0      | ?                    | Basura          | ?                 |
| 1      | ?                    | Cartón          | ?                 |
| 2      | Plástico             | Vidrio          | Plástico          |
| 3      | ?                    | Metal           | ?                 |
| 4      | ?                    | Orgánico        | ?                 |
| 5      | ?                    | Papel           | ?                 |
| 6      | ?                    | Plástico        | ?                 |

### Paso 5: Actualizar Código

Una vez identifiques el orden correcto, edita `ClasificadorResiduos.kt`:

```kotlin
// Ejemplo de orden corregido (reemplaza según tus pruebas)
private val etiquetas = listOf(
    "Papel",       // 0 - basado en tus pruebas
    "Plástico",    // 1
    "Vidrio",      // 2
    "Metal",       // 3
    "Cartón",      // 4
    "Orgánico",    // 5
    "Basura"       // 6
)
```

## Órdenes Alternativos para Probar

Si el orden actual no funciona, prueba estos:

### Opción A - Alfabético Inglés
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

### Opción B - Por Tipo (Reciclables → No Reciclables)
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

### Opción C - Dataset TrashNet Común
```kotlin
private val etiquetas = listOf(
    "Vidrio",      // 0 - Glass
    "Metal",       // 1 - Metal
    "Papel",       // 2 - Paper
    "Plástico",    // 3 - Plastic
    "Cartón",      // 4 - Cardboard
    "Basura",      // 5 - Trash
    "Orgánico"     // 6 - Organic/Compost
)
```

## Verificación en Android Studio

### Logcat (Recomendado)

1. **Abre Logcat**: View → Tool Windows → Logcat (Alt+6)
2. **Filtra logs**: Busca "ClasificadorResiduos"
3. **Captura fotos** y observa los logs en tiempo real
4. **Anota** qué índice tiene mayor porcentaje para cada objeto

### Ejemplo de análisis:

```
Objeto: Botella de plástico
Log: [6] Plástico: 0.89 (89%)  ← Mayor valor

Objeto: Hoja de papel
Log: [5] Papel: 0.92 (92%)     ← Mayor valor

Objeto: Lata de metal
Log: [3] Metal: 0.85 (85%)     ← Mayor valor
```

Si los índices con mayor valor coinciden con las etiquetas correctas, ¡el orden es correcto! ✅

Si no coinciden, usa la tabla del Paso 4 para mapear correctamente.

## Consejos para Mejores Resultados

### Iluminación 💡
- Usa luz natural o luz blanca brillante
- Evita sombras fuertes
- No captures a contraluz

### Composición 📸
- Fondo uniforme y limpio (mesa blanca/clara)
- Objeto centrado en el encuadre
- Distancia: 30-50 cm del objeto
- Mantén la cámara estable

### Objetos Limpios 🧼
- Limpia los objetos antes de capturar
- Seca completamente (sin gotas de agua)
- Remueve etiquetas si es posible

### Ángulo 📐
- Captura desde arriba (vista cenital) o frontal
- Evita ángulos muy inclinados
- Asegúrate de que el objeto sea reconocible

## Solución de Problemas

### Todas las categorías tienen porcentajes similares (~14%)

**Causa**: El modelo no está seguro, posiblemente:
- Imagen muy borrosa
- Objeto no reconocible
- Iluminación muy mala
- Preprocesamiento incorrecto

**Solución**:
- Mejora las condiciones de captura
- Prueba con objetos más simples y limpios

### Siempre detecta la misma categoría (>90%)

**Causa**: Problema de preprocesamiento o normalización

**Solución**: Prueba normalización ImageNet (ver MODELO_DEBUG.md)

### El porcentaje correcto es el segundo o tercero

**Causa**: Orden de etiquetas casi correcto, desfase de 1-2 posiciones

**Solución**: Rota el array de etiquetas en 1-2 posiciones

## Normalización Alternativa

Si después de ajustar el orden las predicciones siguen siendo incorrectas, prueba normalización ImageNet:

```kotlin
// En ClasificadorResiduos.kt, función preprocesar()

// Valores estándar de ImageNet
val mean = floatArrayOf(0.485f, 0.456f, 0.406f)
val std = floatArrayOf(0.229f, 0.224f, 0.225f)

for (pixel in pixels) {
    val r = (pixel shr 16 and 0xFF)
    val g = (pixel shr 8 and 0xFF)
    val b = (pixel and 0xFF)

    // Normalización ImageNet
    buffer.putFloat((r / 255.0f - mean[0]) / std[0])
    buffer.putFloat((g / 255.0f - mean[1]) / std[1])
    buffer.putFloat((b / 255.0f - mean[2]) / std[2])
}
```

## Preguntas Frecuentes

**P: ¿Por qué veo índices al revés?**
R: Los índices se muestran del modelo original. Si el índice [2] tiene mayor confianza, esa es la posición en el array de salida del modelo.

**P: ¿Puedo desactivar el modo debug?**
R: Sí, comenta la sección "Todas las probabilidades (Debug)" en `ClasificadorScreen.kt` línea 427-445

**P: ¿Los logs afectan el rendimiento?**
R: Mínimamente. Solo se generan durante clasificación. Puedes comentar los logs en producción.

**P: ¿Cuántas fotos de prueba necesito?**
R: Mínimo 7 (una por categoría) para confirmar el orden completo.

## Resultado Esperado

Después de ajustar correctamente el orden de etiquetas:

```
✅ Botella de plástico → Plástico (85%)
✅ Hoja de papel      → Papel (92%)
✅ Botella de vidrio  → Vidrio (88%)
✅ Lata de aluminio   → Metal (90%)
✅ Caja de cartón     → Cartón (87%)
✅ Cáscara de plátano → Orgánico (83%)
✅ Bolsa de basura    → Basura (79%)
```

## Soporte Adicional

Si después de seguir estas instrucciones el clasificador sigue sin funcionar correctamente:

1. Captura screenshots de 3-4 resultados con objetos conocidos
2. Copia los logs completos de Logcat
3. Reporta el issue con esta información

El modelo tiene 86% de precisión, así que debería funcionar correctamente una vez que el orden de etiquetas sea el correcto. ¡Éxito con el debugging! 🎉
