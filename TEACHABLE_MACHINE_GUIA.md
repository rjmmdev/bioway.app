# 🚀 Guía Rápida: Teachable Machine para BioWay

## ⏱️ Tiempo Estimado: 15-30 minutos

## Paso 1: Ir a Teachable Machine

Abre tu navegador en:
👉 **https://teachablemachine.withgoogle.com/train/image**

Haz clic en "Get Started" → "Image Project" → "Standard image model"

---

## Paso 2: Crear las 7 Clases

Crea estas clases (haz clic en "Add a class" 7 veces):

1. **Vidrio** (Glass)
2. **Papel** (Paper)
3. **Cartón** (Cardboard)
4. **Plástico** (Plastic)
5. **Metal** (Metal)
6. **Orgánico** (Organic)
7. **Basura** (Trash)

---

## Paso 3: Agregar Imágenes

### Opción A: Usar Webcam (Más Rápido)

Para cada clase:
1. Coloca el objeto frente a la webcam
2. Haz clic en "Webcam"
3. Haz clic en "Hold to Record" y mueve el objeto
4. Graba ~50-100 imágenes por clase (toma 10-15 segundos)
5. Repite con diferentes objetos de la misma clase

**Consejos:**
- Gira el objeto mientras grabas
- Cambia el fondo
- Varía la iluminación
- Usa diferentes colores del mismo material

### Opción B: Subir Imágenes (Más Preciso)

Para cada clase:
1. Haz clic en "Upload"
2. Selecciona ~100 imágenes de esa clase
3. Repite para todas las clases

**Dónde conseguir imágenes:**
- Google Images: búsca "[material] reciclaje"
- Datasets públicos (ver abajo)
- Toma fotos con tu celular

---

## Paso 4: Entrenar el Modelo

1. Haz clic en "Train Model"
2. Espera 2-5 minutos (más tiempo = mejor modelo)
3. Prueba el modelo con la webcam o subiendo imágenes
4. Si la precisión es baja (<80%), agrega más imágenes variadas

---

## Paso 5: Exportar a TensorFlow Lite

1. Haz clic en "Export Model"
2. Selecciona la pestaña "TensorFlow Lite"
3. Elige:
   - ✅ **Quantized** (más pequeño, recomendado para móviles)
   - O **Floating point** (más preciso, más grande)
4. Haz clic en "Download my model"

Se descargará un archivo `.zip` con:
- `model.tflite` - Tu modelo entrenado
- `labels.txt` - Lista de clases en orden

---

## Paso 6: Integrar en BioWay

### 6.1 Copiar el Modelo

```bash
# Descomprime el zip descargado
unzip ~/Downloads/converted_tflite.zip -d ~/Downloads/teachable_model/

# Copia el modelo a tu proyecto
cp ~/Downloads/teachable_model/model.tflite \
   /Users/rauljmza/desarrollo/rjmmdev/proyectos/biowayandroid/app/src/main/assets/modelo_residuos.tflite
```

O manualmente:
1. Descomprime `converted_tflite.zip`
2. Renombra `model.tflite` a `modelo_residuos.tflite`
3. Copia a `app/src/main/assets/`

### 6.2 Verificar las Etiquetas

Abre el archivo `labels.txt` descargado:

```
0 Vidrio
1 Papel
2 Cartón
3 Plástico
4 Metal
5 Orgánico
6 Basura
```

### 6.3 Actualizar el Código

Abre `app/src/main/java/com/biowaymexico/utils/ClasificadorResiduos.kt`

Encuentra la línea 22 y actualiza según tu `labels.txt`:

```kotlin
// IMPORTANTE: Este orden DEBE coincidir EXACTAMENTE con labels.txt
private val etiquetas = listOf(
    "Vidrio",      // 0 - según tu labels.txt
    "Papel",       // 1
    "Cartón",      // 2
    "Plástico",    // 3
    "Metal",       // 4
    "Orgánico",    // 5
    "Basura"       // 6
)
```

### 6.4 Verificar INPUT_SIZE

Teachable Machine usa tamaño de entrada 224x224 por defecto.

Si cambiaste el tamaño en "Advanced", actualiza línea 33:

```kotlin
private val INPUT_SIZE = 224  // O 96, 160 si elegiste otro
```

---

## Paso 7: Compilar y Probar

```bash
cd /Users/rauljmza/desarrollo/rjmmdev/proyectos/biowayandroid
./gradlew clean assembleDebug
```

¡Listo! Abre la app y prueba el clasificador.

---

## 📊 Mejorando la Precisión

Si la precisión es baja (<70%), prueba:

### 1. Más Imágenes por Clase
- Mínimo: 50 imágenes
- Recomendado: 100-200 imágenes
- Ideal: 500+ imágenes

### 2. Mayor Variedad
- Diferentes colores
- Diferentes tamaños
- Diferentes fondos
- Diferentes iluminaciones
- Diferentes ángulos

### 3. Ejemplos Negativos
Agrega imágenes que NO son de esa clase en "Basura" para evitar falsos positivos.

### 4. Más Tiempo de Entrenamiento
En "Advanced Settings":
- Epochs: 100+ (más = mejor, pero más lento)
- Batch size: 16 o 32
- Learning rate: 0.001 (default está bien)

---

## 🎯 Datasets Listos para Usar

### Opción 1: TrashNet Dataset Original

**Descargar:**
```bash
# Con git
git clone https://github.com/garythung/trashnet.git

# O descarga directa
# https://github.com/garythung/trashnet/archive/refs/heads/master.zip
```

Estructura:
```
trashnet/data/
├── cardboard/    (~400 imágenes)
├── glass/        (~500 imágenes)
├── metal/        (~400 imágenes)
├── paper/        (~590 imágenes)
├── plastic/      (~480 imágenes)
└── trash/        (~130 imágenes)
```

**Usar en Teachable Machine:**
1. Descarga el dataset
2. En cada clase, haz clic en "Upload"
3. Selecciona todas las imágenes de esa carpeta

### Opción 2: Waste Classification Dataset (Kaggle)

**Descargar:**
1. Ve a https://www.kaggle.com/datasets/techsash/waste-classification-data
2. Haz clic en "Download" (requiere cuenta de Kaggle)
3. Descomprime el archivo

22,000+ imágenes organizadas por tipo.

### Opción 3: RealWaste Dataset (Más Realista)

**Descargar:**
https://archive.realwaste.org/

Imágenes de residuos reales en diferentes contextos (no studio shots).

---

## 🔄 Iteración Rápida

Para mejorar tu modelo rápidamente:

1. **Entrena versión 1** (30 min)
   - 50 imágenes por clase
   - Prueba en la app

2. **Identifica clases débiles** (5 min)
   - Prueba con objetos reales
   - Anota cuáles fallan

3. **Agrega más datos** (20 min)
   - Enfócate en las clases débiles
   - Agrega 50-100 imágenes más

4. **Re-entrena y prueba** (15 min)
   - Exporta nuevo modelo
   - Reemplaza en la app
   - Prueba de nuevo

Repite hasta lograr >85% precisión.

---

## ⚡ Pro Tips

### Tip 1: Usa Data Augmentation
Teachable Machine lo hace automáticamente:
- Rotación
- Zoom
- Flip horizontal
- Ajuste de brillo

### Tip 2: Fondos Variados
Toma fotos de objetos en:
- ✅ Mesa blanca
- ✅ Mesa de madera
- ✅ Pasto
- ✅ Concreto
- ✅ Mano (para escala)

### Tip 3: Iluminación Natural
- ✅ Luz del día
- ✅ Interior con luz artificial
- ⚠️ Evita contraluces fuertes
- ⚠️ Evita sombras muy marcadas

### Tip 4: Objetos Limpios
Para mejores resultados:
- Objetos limpios y secos
- Sin etiquetas (o con etiquetas)
- Enteros (no rotos)

### Tip 5: Prueba en Condiciones Reales
Después de entrenar, prueba con:
- Objetos del día a día
- Diferentes marcas
- Diferentes tamaños
- En diferentes lugares de tu casa

---

## 📱 Ejemplo Rápido: 30 Minutos

### Minuto 0-5: Setup
- Abre Teachable Machine
- Crea 7 clases

### Minuto 5-20: Captura Datos
- Webcam: 15 segundos por objeto
- 3-4 objetos diferentes por clase
- Total: ~70 objetos, ~100 imágenes por clase

### Minuto 20-25: Entrenar
- Click "Train Model"
- Espera 5 minutos

### Minuto 25-28: Probar
- Prueba con webcam
- Verifica precisión >80%

### Minuto 28-30: Exportar
- Export → TensorFlow Lite → Download
- Copia a proyecto Android

---

## 🎓 Tutorial en Video

Si prefieres video, busca en YouTube:
- "Teachable Machine TensorFlow Lite Android"
- "Custom Image Classification Android Teachable Machine"
- "Waste Classification Teachable Machine"

---

## ❓ Solución de Problemas

### "El modelo siempre predice la misma clase"
**Solución**: Agrega más variedad de imágenes. Posiblemente una clase tiene muchas más imágenes que otras.

### "Precisión muy baja (<60%)"
**Solución**:
1. Aumenta el número de epochs a 100+
2. Agrega más imágenes variadas
3. Limpia imágenes mal etiquetadas

### "El modelo es muy grande (>50 MB)"
**Solución**:
1. Usa el modelo Quantized (no Floating point)
2. En Advanced: Reduce el tamaño del modelo
3. O usa menos clases

### "No funciona en la app pero sí en Teachable Machine"
**Solución**:
1. Verifica que el orden de etiquetas coincida
2. Revisa la normalización (debería ser [0, 1])
3. Confirma INPUT_SIZE = 224

---

## 🎉 ¡Éxito!

Una vez integrado correctamente:
- ✅ Modelo personalizado
- ✅ 100% offline
- ✅ Optimizado para Android
- ✅ <10 MB de tamaño
- ✅ ~85%+ precisión

¡Tu clasificador de residuos con IA está listo! 🚀
