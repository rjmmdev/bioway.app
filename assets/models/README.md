# Modelos de IA para Detección de Residuos

## Archivos necesarios

Para que la aplicación funcione correctamente con detección real, necesitas:

1. **waste_classifier_v1.tflite** - Modelo TensorFlow Lite entrenado
2. **waste_labels.txt** - Etiquetas de categorías (ya incluido)

## Cómo obtener el modelo

### Opción 1: Usar un modelo pre-entrenado
Puedes descargar modelos de detección de objetos desde:
- [TensorFlow Hub](https://tfhub.dev/)
- [MediaPipe Models](https://developers.google.com/mediapipe/solutions/vision/object_detector)
- [MobileNet Models](https://github.com/tensorflow/models/tree/master/research/slim/nets/mobilenet)

### Opción 2: Entrenar tu propio modelo
1. Recopilar dataset de imágenes de residuos
2. Usar TensorFlow/Keras para entrenar
3. Convertir a TFLite format
4. Optimizar para móviles

## Formato esperado del modelo

- **Input**: Imagen RGB 224x224x3 normalizada [-1, 1]
- **Output**: Vector de probabilidades para cada categoría
- **Arquitectura recomendada**: MobileNet V3 Small

## Categorías soportadas

El sistema actualmente soporta 18 categorías:
- plastic_bottle (Botellas PET)
- cardboard (Cartón)
- glass_bottle (Vidrio)
- aluminum_can (Latas de aluminio)
- paper (Papel)
- organic (Orgánico)
- metal (Metal)
- plastic_bag (Bolsas plásticas)
- styrofoam (Poliestireno)
- electronic_waste (Electrónicos)
- battery (Baterías)
- textiles (Textiles)
- Y más...

## Modo simulado

Si no hay modelo disponible, la app funcionará en modo simulado para desarrollo.