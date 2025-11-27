# 🎯 Resumen: Modelos Pre-entrenados para BioWay

## 🚀 Opción Más Rápida: Teachable Machine (15 min)

**Recomendada para empezar HOY**

```bash
1. Ve a: https://teachablemachine.withgoogle.com/train/image
2. Crea 7 clases: Vidrio, Papel, Cartón, Plástico, Metal, Orgánico, Basura
3. Graba 50-100 imágenes por clase con webcam (toma fotos de objetos reales)
4. Entrena el modelo (5 minutos)
5. Export → TensorFlow Lite → Download
6. Copia model.tflite a app/src/main/assets/modelo_residuos.tflite
7. Actualiza etiquetas en ClasificadorResiduos.kt según labels.txt
```

📖 **Guía detallada:** `TEACHABLE_MACHINE_GUIA.md`

---

## ⭐ Opción Automatizada: Script Python

```bash
# Instalar dependencias
pip install tensorflow huggingface-hub

# Ejecutar script
python3 descargar_modelo.py

# Seguir las instrucciones en pantalla
# Opción 1: Descarga automática de Hugging Face
# Opción 2: Convierte modelo Keras a TFLite
# Opción 3: Guía de Teachable Machine
```

El script:
- ✅ Descarga modelo automáticamente
- ✅ Convierte a TFLite si es necesario
- ✅ Copia al proyecto Android
- ✅ Genera archivo de etiquetas
- ✅ Muestra instrucciones de integración

---

## 📚 Modelos Pre-entrenados Recomendados

### 1. TrashNet MobileNetV2 (Recomendado)
- **Fuente:** Hugging Face - ahmzakif/TrashNet-Classification
- **Precisión:** ~90%
- **Tamaño:** ~10 MB
- **Clases:** 6 (Cardboard, Glass, Metal, Paper, Plastic, Trash)
- **URL:** https://huggingface.co/ahmzakif/TrashNet-Classification

### 2. ViT TrashNet (Mejor Precisión)
- **Fuente:** Hugging Face - edwinpalegre/vit-base-trashnet-demo
- **Precisión:** 98.22%
- **Tamaño:** ~40 MB
- **Clases:** 6
- **Requiere:** Conversión a TFLite

### 3. Teachable Machine Custom (Tu Propio Modelo)
- **Fuente:** Tu entrenamiento
- **Precisión:** 85-95% (depende de tus datos)
- **Tamaño:** 5-15 MB
- **Clases:** Las que tú definas (recomendado 7 para México)

---

## 🔧 Integración Rápida

### Paso 1: Reemplazar Modelo

```bash
cp nuevo_modelo.tflite app/src/main/assets/modelo_residuos.tflite
```

### Paso 2: Actualizar Etiquetas

Edita `app/src/main/java/com/biowaymexico/utils/ClasificadorResiduos.kt` línea 22:

```kotlin
private val etiquetas = listOf(
    "Cardboard",   // 0 - según tu labels.txt
    "Glass",       // 1
    "Metal",       // 2
    "Paper",       // 3
    "Plastic",     // 4
    "Trash"        // 5
)
```

**IMPORTANTE:** El orden debe coincidir EXACTAMENTE con el modelo.

### Paso 3: Compilar

```bash
./gradlew clean assembleDebug
```

### Paso 4: Probar

1. Abre la app
2. Ve al Clasificador IA
3. Captura fotos de objetos reales
4. Verifica que las predicciones sean correctas
5. Revisa logs en Logcat para debugging

---

## 📖 Documentación Completa

- **`MODELOS_PREENTRENADOS.md`** - Todas las opciones de modelos disponibles
- **`TEACHABLE_MACHINE_GUIA.md`** - Guía paso a paso para Teachable Machine
- **`CLASIFICADOR_DEBUG_INSTRUCCIONES.md`** - Cómo debuggear el modelo actual
- **`MODELO_DEBUG.md`** - Información técnica de debugging
- **`descargar_modelo.py`** - Script automático de descarga/conversión

---

## 🎯 Comparación de Opciones

| Opción | Tiempo | Precisión | Dificultad | Personalización |
|--------|--------|-----------|------------|-----------------|
| Teachable Machine | 15 min | 85% | ⭐ Fácil | ⭐⭐⭐ Alta |
| Script Auto | 5 min | 90% | ⭐⭐ Media | ⭐ Baja |
| TrashNet HuggingFace | 10 min | 90% | ⭐⭐ Media | ⭐ Baja |
| ViT Custom | 30 min | 98% | ⭐⭐⭐ Difícil | ⭐⭐ Media |
| TFLite Model Maker | 2 horas | 95% | ⭐⭐⭐ Difícil | ⭐⭐⭐ Alta |

---

## ❓ ¿Qué Opción Elegir?

### Si quieres empezar AHORA (< 30 min)
👉 **Teachable Machine**
- Sin código
- Interfaz visual
- Entrena con tu webcam
- Perfecto para MVP

### Si quieres la mejor precisión YA (< 10 min)
👉 **Script Python + TrashNet**
- Modelo profesional
- 90% precisión
- Automático
- Listo para producción

### Si necesitas categorías específicas de México
👉 **Teachable Machine + Dataset Custom**
- Entrena con residuos locales
- Categorías en español
- Objetos de marcas mexicanas
- 85-95% precisión

### Si tienes tiempo y quieres el mejor modelo
👉 **TensorFlow Lite Model Maker**
- Control total
- >95% precisión
- Requiere dataset grande
- Ver `MODELOS_PREENTRENADOS.md` para tutorial

---

## 🐛 Debug del Modelo Actual

Si ya tienes un modelo pero no funciona correctamente:

```bash
# 1. Revisa la documentación de debug
cat CLASIFICADOR_DEBUG_INSTRUCCIONES.md

# 2. Ejecuta la app y revisa Logcat
adb logcat -s ClasificadorResiduos

# 3. Captura fotos de objetos conocidos
# 4. Observa qué índice [0-6] tiene mayor %
# 5. Ajusta el orden de etiquetas en ClasificadorResiduos.kt
```

La app ahora incluye:
- ✅ Vista de debug visual con índices
- ✅ Logs detallados en Logcat
- ✅ Barras de probabilidad para todas las clases

---

## 🎓 Recursos Adicionales

### Datasets Públicos
- TrashNet: https://github.com/garythung/trashnet
- Kaggle Waste: https://www.kaggle.com/datasets/techsash/waste-classification-data
- RealWaste: https://archive.realwaste.org/

### Tutoriales
- TensorFlow Lite Guide: https://www.tensorflow.org/lite/guide
- Teachable Machine: https://teachablemachine.withgoogle.com/
- Hugging Face Models: https://huggingface.co/models?library=tflite

### Herramientas
- TFLite Model Maker: https://www.tensorflow.org/lite/models/modify/model_maker
- Netron (visualizar modelos): https://netron.app/
- TensorFlow Hub: https://tfhub.dev/

---

## ✅ Checklist de Integración

Después de elegir e integrar tu modelo:

- [ ] Modelo copiado a `app/src/main/assets/modelo_residuos.tflite`
- [ ] Etiquetas actualizadas en `ClasificadorResiduos.kt`
- [ ] `INPUT_SIZE` verificado (224, 299, etc.)
- [ ] Normalización correcta ([0,1], [-1,1], ImageNet)
- [ ] Proyecto compila sin errores: `./gradlew clean assembleDebug`
- [ ] App instalada en dispositivo: `./gradlew installDebug`
- [ ] Probado con 7 objetos reales (uno por clase)
- [ ] Logs verificados en Logcat
- [ ] Precisión >70% en pruebas
- [ ] UI muestra todas las probabilidades correctamente

---

## 🚨 Troubleshooting

### El modelo siempre detecta la misma clase
- ❌ **Problema:** Orden de etiquetas incorrecto
- ✅ **Solución:** Usa el modo debug visual o revisa logs en Logcat

### Precisión muy baja (<50%)
- ❌ **Problema:** Preprocesamiento incorrecto
- ✅ **Solución:** Verifica normalización en `preprocesar()`

### App crashea al cargar modelo
- ❌ **Problema:** Modelo corrupto o muy grande
- ✅ **Solución:** Verifica tamaño (<50 MB) y que sea TFLite válido

### Todas las clases tienen ~14% (similar)
- ❌ **Problema:** Modelo no se cargó correctamente
- ✅ **Solución:** Verifica que el archivo existe y es válido

---

## 🎉 ¡Listo!

Con estas opciones, puedes integrar un modelo de clasificación de residuos en BioWay en **menos de 30 minutos**.

**Recomendación final:**
1. Empieza con **Teachable Machine** (15 min)
2. Si funciona bien, úsalo en producción
3. Si necesitas más precisión, entrena con dataset más grande
4. O usa **TrashNet** de Hugging Face para 90% precisión inmediata

¿Preguntas? Revisa la documentación detallada en los archivos `.md` o ejecuta `python3 descargar_modelo.py` para opción guiada.
