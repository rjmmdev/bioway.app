# Modelos Pre-entrenados para Clasificación de Residuos

## 🎯 Opción 1: Google Teachable Machine (MÁS FÁCIL)

**Recomendada para empezar rápido**

### Ventajas
- ✅ Sin código, interfaz visual
- ✅ Exporta directamente a TensorFlow Lite
- ✅ Puedes entrenar con tus propias imágenes
- ✅ Listo en minutos

### Cómo usar

1. **Ir a Teachable Machine**
   - https://teachablemachine.withgoogle.com/train/image

2. **Opción A: Usar modelo pre-entrenado**
   - Busca proyectos públicos de waste classification en la galería
   - O usa este dataset de Kaggle: https://www.kaggle.com/datasets/mostafaabla/garbage-classification

3. **Opción B: Entrenar tu propio modelo**
   - Crea 7 clases: Vidrio, Papel, Cartón, Plástico, Metal, Orgánico, Basura
   - Sube 50-100 imágenes por clase (puedes usar webcam)
   - Haz clic en "Train Model"

4. **Exportar a TensorFlow Lite**
   ```
   Export Model → TensorFlow Lite → Download
   ```
   Recibirás:
   - `model.tflite` - El modelo
   - `labels.txt` - Lista de etiquetas en orden correcto

5. **Integrar en tu app**
   - Reemplaza `modelo_residuos.tflite` con el nuevo `model.tflite`
   - Usa las etiquetas del archivo `labels.txt` en `ClasificadorResiduos.kt`

### Ejemplo de labels.txt
```
0 Basura
1 Cartón
2 Vidrio
3 Metal
4 Orgánico
5 Papel
6 Plástico
```

---

## 🎯 Opción 2: Hugging Face Models

**Mejor para modelos de alta calidad ya entrenados**

### Modelos Recomendados

#### 1. ahmzakif/TrashNet-Classification
- **URL**: https://huggingface.co/ahmzakif/TrashNet-Classification
- **Arquitectura**: MobileNetV2
- **Clases**: Cardboard, Glass, Metal, Paper, Plastic, Trash
- **Incluye**: Notebook de cuantización para TFLite

#### 2. edwinpalegre/vit-base-trashnet-demo
- **URL**: https://huggingface.co/edwinpalegre/vit-base-trashnet-demo
- **Precisión**: 98.22%
- **Arquitectura**: Vision Transformer
- **Nota**: Requiere conversión a TFLite

#### 3. aculotta/Trashnet
- **URL**: https://huggingface.co/aculotta/Trashnet
- **Especialidad**: Basura acuática y terrestre
- **Arquitectura**: ResNet

### Cómo descargar y usar

```bash
# Instalar huggingface-hub
pip install huggingface-hub

# Descargar modelo
from huggingface_hub import hf_hub_download

model_path = hf_hub_download(
    repo_id="ahmzakif/TrashNet-Classification",
    filename="model.tflite"
)
```

Si el modelo no está en formato TFLite, necesitarás convertirlo (ver sección de conversión abajo).

---

## 🎯 Opción 3: TensorFlow Hub

**Modelos de Google optimizados**

### MobileNet V2 Image Classification

```python
import tensorflow as tf
import tensorflow_hub as hub

# Cargar modelo base
model_url = "https://tfhub.dev/google/tf2-preview/mobilenet_v2/classification/4"
model = tf.keras.Sequential([
    hub.KerasLayer(model_url)
])

# Necesitarás fine-tuning con dataset de residuos
# O usar transfer learning
```

**Transfer Learning con dataset TrashNet:**

```python
# Dataset TrashNet en Hugging Face
from datasets import load_dataset
dataset = load_dataset("garythung/trashnet")

# Fine-tune el modelo
# Luego convertir a TFLite
converter = tf.lite.TFLiteConverter.from_keras_model(model)
tflite_model = converter.convert()

with open('modelo_residuos.tflite', 'wb') as f:
    f.write(tflite_model)
```

---

## 🎯 Opción 4: Modelos de Kaggle

### Garbage Classification Dataset + Modelos

**Datasets recomendados:**

1. **TrashNet Dataset**
   - https://www.kaggle.com/datasets/asdasdasasdas/garbage-classification
   - 6 clases, ~2500 imágenes
   - Incluye notebooks con modelos entrenados

2. **Waste Classification Data**
   - https://www.kaggle.com/datasets/techsash/waste-classification-data
   - 22,000+ imágenes
   - 2 categorías principales: Orgánico, Reciclable

3. **Garbage Images (12 Classes)**
   - https://www.kaggle.com/datasets/sumn2u/garbage-classification-v2
   - 12 categorías detalladas
   - Incluye notebooks con MobileNet

### Cómo usar modelos de Kaggle

1. Busca notebooks con "TensorFlow Lite" en el título
2. Ejecuta el notebook
3. Descarga el archivo `.tflite` generado
4. Copia a tu proyecto Android

---

## 🎯 Opción 5: TensorFlow Lite Model Maker

**Mejor para entrenar modelos customizados fácilmente**

### Instalación

```bash
pip install tflite-model-maker
```

### Entrenamiento Rápido

```python
from tflite_model_maker import image_classifier
from tflite_model_maker.image_classifier import DataLoader

# Cargar datos (estructura: data/clase1/, data/clase2/, etc.)
data = DataLoader.from_folder('waste_images/')

# Dividir en train/test
train_data, test_data = data.split(0.8)

# Entrenar modelo
model = image_classifier.create(train_data)

# Evaluar
loss, accuracy = model.evaluate(test_data)
print(f'Precisión: {accuracy}')

# Exportar a TFLite
model.export(export_dir='.')
```

Esto genera `model.tflite` listo para usar.

---

## 📥 Conversión de Modelos a TensorFlow Lite

Si tienes un modelo en formato `.h5`, `.pb` o Keras:

### Desde Keras/H5

```python
import tensorflow as tf

# Cargar modelo Keras
model = tf.keras.models.load_model('model.h5')

# Convertir a TFLite
converter = tf.lite.TFLiteConverter.from_keras_model(model)

# Optimizaciones (opcional)
converter.optimizations = [tf.lite.Optimize.DEFAULT]

# Convertir
tflite_model = converter.convert()

# Guardar
with open('modelo_residuos.tflite', 'wb') as f:
    f.write(tflite_model)
```

### Cuantización para Reducir Tamaño

```python
converter = tf.lite.TFLiteConverter.from_keras_model(model)

# Cuantización dinámica (reduce tamaño 4x)
converter.optimizations = [tf.lite.Optimize.DEFAULT]

# O cuantización completa (reduce 4x + más rápido)
converter.target_spec.supported_ops = [tf.lite.OpsSet.TFLITE_BUILTINS_INT8]
converter.inference_input_type = tf.uint8
converter.inference_output_type = tf.uint8

tflite_model = converter.convert()
```

---

## 🔄 Integración en tu App

### Paso 1: Reemplazar Modelo

```bash
# Copia tu nuevo modelo
cp nuevo_modelo.tflite app/src/main/assets/modelo_residuos.tflite
```

### Paso 2: Actualizar Etiquetas

En `ClasificadorResiduos.kt`, línea 22:

```kotlin
// IMPORTANTE: Orden debe coincidir EXACTAMENTE con el modelo
private val etiquetas = listOf(
    "Cardboard",   // 0 - según labels.txt o entrenamiento
    "Glass",       // 1
    "Metal",       // 2
    "Paper",       // 3
    "Plastic",     // 4
    "Trash"        // 5
)
```

### Paso 3: Verificar Entrada del Modelo

Algunos modelos usan diferentes tamaños de entrada:

```kotlin
// Verifica en la documentación del modelo
private val INPUT_SIZE = 224  // Común: 224, 299, 384
```

### Paso 4: Verificar Normalización

```kotlin
// Opción 1: [0, 1]
buffer.putFloat(r / 255.0f)

// Opción 2: [-1, 1]
buffer.putFloat((r / 255.0f - 0.5f) * 2.0f)

// Opción 3: ImageNet
val mean = floatArrayOf(0.485f, 0.456f, 0.406f)
val std = floatArrayOf(0.229f, 0.224f, 0.225f)
buffer.putFloat((r / 255.0f - mean[0]) / std[0])
```

---

## 🎯 Recomendación Final

**Para empezar HOY:**
1. ✅ **Google Teachable Machine** - 15 minutos, sin código
   - Ve a https://teachablemachine.withgoogle.com/
   - Entrena con imágenes rápidas de tu celular
   - Descarga el `.tflite` listo

**Para mejor precisión:**
2. ✅ **Hugging Face - ahmzakif/TrashNet-Classification**
   - Modelo ya entrenado y optimizado
   - MobileNetV2 perfecto para móviles
   - ~90% precisión

**Para personalización total:**
3. ✅ **TensorFlow Lite Model Maker**
   - Entrena con tu dataset específico
   - Control total sobre arquitectura
   - Días de México y tipos de residuos locales

---

## 📋 Checklist de Integración

Después de obtener tu modelo:

- [ ] Copiar `modelo.tflite` a `app/src/main/assets/`
- [ ] Actualizar array de `etiquetas` en `ClasificadorResiduos.kt`
- [ ] Verificar `INPUT_SIZE` (224, 299, etc.)
- [ ] Ajustar normalización si es necesario
- [ ] Compilar app: `./gradlew assembleDebug`
- [ ] Probar con objetos reales
- [ ] Verificar logs en Logcat
- [ ] Si precisión <70%, revisar preprocesamiento

---

## 🔗 Links Útiles

- **Teachable Machine**: https://teachablemachine.withgoogle.com/
- **TensorFlow Lite Models**: https://www.tensorflow.org/lite/models
- **Hugging Face TFLite**: https://huggingface.co/models?library=tflite
- **TrashNet Dataset**: https://huggingface.co/datasets/garythung/trashnet
- **TFLite Model Maker**: https://www.tensorflow.org/lite/models/modify/model_maker
- **TensorFlow Hub**: https://tfhub.dev/

---

## ❓ FAQ

**P: ¿Cuál es la diferencia entre los modelos?**
- **Teachable Machine**: Fácil, rápido, buena precisión (~85%)
- **TrashNet**: Alta precisión (~90%), probado
- **Custom**: Mejor precisión (>95%), requiere tiempo

**P: ¿Puedo usar múltiples modelos?**
Sí, puedes cambiar el modelo dinámicamente o tener varios y dejar que el usuario elija.

**P: ¿El modelo funciona offline?**
Sí, TensorFlow Lite funciona 100% offline en el dispositivo.

**P: ¿Qué tan grande puede ser el modelo?**
Idealmente <10 MB. Con cuantización puedes reducir de 40 MB a 10 MB.

**P: ¿Necesito GPU?**
No, TensorFlow Lite usa GPU automáticamente si está disponible, pero funciona bien en CPU.
