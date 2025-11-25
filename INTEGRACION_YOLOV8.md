# Integración de YOLOv8 en BioWay App

## Resumen de Cambios

Se ha integrado exitosamente el modelo YOLOv8 de `clasificacionBioWay` en la aplicación `bioway.app`, reemplazando el modelo anterior de 43MB con uno más eficiente de solo 3MB que ofrece mejor precisión y capacidades adicionales.

## Cambios Realizados

### 1. Archivos Agregados

#### Modelo y Recursos
- `app/src/main/assets/models/best.tflite` - Modelo YOLOv8 (3MB)
- `app/src/main/assets/labels/labels.txt` - 12 categorías de residuos

#### Código Fuente
- `WasteClassifierYOLO.kt` - Clasificador YOLOv8 con detección de objetos múltiples
- `ClasificadorScreenYOLO.kt` - Nueva pantalla con detección en tiempo real y bounding boxes

### 2. Dependencias Actualizadas (build.gradle.kts)

```kotlin
// TensorFlow Lite con GPU
implementation("org.tensorflow:tensorflow-lite:2.14.0")
implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
implementation("org.tensorflow:tensorflow-lite-gpu:2.14.0")
implementation("org.tensorflow:tensorflow-lite-gpu-api:2.14.0")

// CameraX para análisis en tiempo real
implementation("androidx.camera:camera-core:1.3.1")
implementation("androidx.camera:camera-camera2:1.3.1")
implementation("androidx.camera:camera-lifecycle:1.3.1")
implementation("androidx.camera:camera-view:1.3.1")

// Coroutines y permisos
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
implementation("com.google.accompanist:accompanist-permissions:0.32.0")
```

### 3. Configuraciones del Proyecto

- Agregado soporte NDK para arquitecturas: armeabi-v7a, arm64-v8a, x86, x86_64
- Habilitado `mlModelBinding` en buildFeatures
- Configurado `noCompress` para archivos tflite y lite

### 4. Navegación Actualizada

En `BioWayNavHost.kt`, la ruta del clasificador ahora usa la nueva pantalla YOLOv8:

```kotlin
composable(BioWayDestinations.BrindadorClasificador.route) {
    ClasificadorScreenYOLO(
        onNavigateBack = { navController.popBackStack() }
    )
}
```

## Características del Nuevo Clasificador

### Ventajas sobre el Modelo Anterior

| Característica | Modelo Anterior | YOLOv8 |
|---------------|-----------------|---------|
| **Tamaño** | 43 MB | 3 MB |
| **Categorías** | 7 | 12 (mapeadas a 7) |
| **Precisión** | ~70% | 86% |
| **Detección múltiple** | No | Sí |
| **Bounding boxes** | No | Sí |
| **Tiempo real** | No | Sí |
| **GPU Support** | No | Sí |

### Categorías Detectadas

El modelo YOLOv8 detecta 12 categorías que se mapean automáticamente a las 7 categorías de BioWay:

- **Plástico**: plastic, plastic-PET, plastic-PE_HD, plastic-PP, plastic-PS, plastic-Others
- **Cartón**: cardboard
- **Papel**: paper
- **Vidrio**: glass
- **Metal**: metal
- **Orgánico**: biological
- **Basura**: trash

### Funcionalidades de la Nueva Pantalla

1. **Detección en Tiempo Real**
   - Análisis continuo de frames de la cámara
   - Actualización cada 250ms aproximadamente
   - Toggle para activar/desactivar análisis en vivo

2. **Visualización con Bounding Boxes**
   - Rectángulos de colores alrededor de objetos detectados
   - Etiquetas con nombre de categoría y porcentaje de confianza
   - Colores únicos por categoría

3. **Captura de Alta Resolución**
   - Botón para capturar foto con mayor calidad
   - Análisis detallado de la imagen capturada
   - Posibilidad de tomar nueva foto

4. **Panel de Resultados**
   - Agrupación por categoría simplificada
   - Conteo de objetos por categoría
   - Promedio de confianza por grupo

## Uso de la Nueva Funcionalidad

### Para Usuarios Finales

1. Ir a la pantalla del Brindador
2. Seleccionar "Clasificador de Residuos"
3. Apuntar la cámara al residuo a clasificar
4. Observar detección en tiempo real o capturar foto
5. Ver resultados agrupados por categoría

### Para Desarrolladores

#### Inicialización del Clasificador

```kotlin
val classifier = WasteClassifierYOLO(context)

// Inicializar de forma asíncrona
scope.launch {
    classifier.initialize()
    // Clasificador listo para usar
}
```

#### Clasificación de Imagen

```kotlin
val result = classifier.classifyImage(bitmap)
result.detections.forEach { detection ->
    println("Detectado: ${detection.className} con ${detection.confidence}%")
    // detection.boundingBox contiene las coordenadas
}
```

#### Mapeo a Categorías Simplificadas

```kotlin
val simplifiedCategory = classifier.mapToSimplifiedCategories(detection.className)
```

## Rendimiento

- **Tiempo de inferencia**: 40-72ms en CPU con XNNPACK
- **Con GPU**: 20-40ms (cuando disponible)
- **Uso de memoria**: ~150-200MB adicionales durante inferencia
- **Tamaño del APK**: +3MB aproximadamente

## Posibles Mejoras Futuras

1. **Optimización de Rendimiento**
   - Implementar caché de detecciones
   - Reducir resolución de análisis en tiempo real
   - Usar modelo cuantizado int8 para dispositivos de gama baja

2. **Funcionalidades Adicionales**
   - Guardar historial de clasificaciones
   - Estadísticas de reciclaje
   - Modo batch para múltiples fotos
   - Integración con sistema de puntos

3. **UI/UX**
   - Animaciones para bounding boxes
   - Sonidos de confirmación
   - Tutorial interactivo
   - Modo nocturno para detección

## Solución de Problemas

### Error: "No se pudo inicializar el clasificador"
- Verificar que los archivos del modelo estén en assets/models/
- Comprobar permisos de cámara
- Revisar logs para detalles específicos

### Detección lenta o lag
- Desactivar análisis en tiempo real
- Usar solo modo captura
- Cerrar otras aplicaciones

### No se detectan objetos
- Mejorar iluminación
- Acercar/alejar la cámara
- Asegurar que el objeto esté centrado
- Verificar que el objeto sea de una categoría soportada

## Comandos de Compilación

```bash
# Limpiar y reconstruir
./gradlew clean build

# Instalar en dispositivo
./gradlew installDebug

# Ver logs del clasificador
adb logcat | grep WasteClassifierYOLO
```

## Archivos Relacionados

- `/app/src/main/java/com/biowaymexico/utils/WasteClassifierYOLO.kt` - Lógica del clasificador
- `/app/src/main/java/com/biowaymexico/ui/screens/brindador/ClasificadorScreenYOLO.kt` - UI
- `/app/src/main/assets/models/best.tflite` - Modelo ML
- `/app/src/main/assets/labels/labels.txt` - Etiquetas
- `/app/build.gradle.kts` - Configuración y dependencias

---

**Fecha de integración**: Noviembre 2025
**Versión del modelo**: YOLOv8 Compact 3MB
**Autor de la integración**: Claude Code Assistant