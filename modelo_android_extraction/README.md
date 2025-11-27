# YOLO Object Detector - Modelo de Clasificacion de Residuos

Este paquete contiene todo lo necesario para implementar el modelo YOLO de deteccion de objetos en tu aplicacion Android, con la **misma precision** que la app original.

## Contenido del Paquete

```
modelo_android_extraction/
├── assets/
│   ├── models/
│   │   └── best.tflite          # Modelo TensorFlow Lite
│   └── labels/
│       └── labels.txt           # Etiquetas de clases
├── src/main/java/com/ultralytics/yolo/
│   ├── ObjectDetector.java      # Clase principal del detector
│   ├── YOLOResult.java          # Resultado de la deteccion
│   ├── Box.java                 # Bounding box detectado
│   └── Size.java                # Clase auxiliar de dimensiones
├── build.gradle.example         # Dependencias necesarias
└── README.md                    # Este archivo
```

## Clases Detectadas (12 categorias)

1. biological
2. cardboard
3. glass
4. metal
5. paper
6. plastic
7. plastic-Others
8. plastic-PET
9. plastic-PE_HD
10. plastic-PP
11. plastic-PS
12. trash

## Instrucciones de Implementacion

### Paso 1: Agregar Dependencias

Agrega las siguientes dependencias a tu `app/build.gradle`:

```gradle
dependencies {
    // TensorFlow Lite - Core (REQUERIDO)
    implementation 'org.tensorflow:tensorflow-lite:2.14.0'

    // TensorFlow Lite - Support Library (REQUERIDO)
    implementation 'org.tensorflow:tensorflow-lite-support:0.4.4'

    // TensorFlow Lite - GPU Delegate (OPCIONAL pero recomendado)
    implementation 'org.tensorflow:tensorflow-lite-gpu:2.14.0'
    implementation 'org.tensorflow:tensorflow-lite-gpu-api:2.14.0'
}
```

**IMPORTANTE**: Agrega esto en la seccion `android` de tu build.gradle para evitar que el modelo se comprima:

```gradle
android {
    // ...
    aaptOptions {
        noCompress "tflite"
    }
}
```

### Paso 2: Copiar Archivos

1. **Modelo y Labels**: Copia las carpetas `assets/models/` y `assets/labels/` a tu carpeta `app/src/main/assets/`

2. **Codigo Java**: Copia la carpeta `src/main/java/com/ultralytics/` a tu `app/src/main/java/`

Tu estructura deberia quedar asi:
```
app/src/main/
├── assets/
│   ├── models/
│   │   └── best.tflite
│   └── labels/
│       └── labels.txt
└── java/
    └── com/
        └── ultralytics/
            └── yolo/
                ├── ObjectDetector.java
                ├── YOLOResult.java
                ├── Box.java
                └── Size.java
```

### Paso 3: Usar el Detector

```java
import com.ultralytics.yolo.ObjectDetector;
import com.ultralytics.yolo.YOLOResult;
import com.ultralytics.yolo.Box;

public class MainActivity extends AppCompatActivity {
    private ObjectDetector detector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            // Cargar etiquetas
            List<String> labels = ObjectDetector.loadLabelsFromAssets(
                this,
                "labels/labels.txt"
            );

            // Crear detector (con GPU = true para mejor rendimiento)
            detector = new ObjectDetector(
                this,
                "models/best.tflite",  // Ruta al modelo en assets
                labels,
                true  // useGpu = true para aceleracion GPU
            );

            // Opcional: Ajustar umbrales (valores por defecto iguales a la app original)
            detector.setConfidenceThreshold(0.25f);  // 25% confianza minima
            detector.setIouThreshold(0.4f);           // 40% IoU para NMS
            detector.setNumItemsThreshold(30);        // Max 30 detecciones

        } catch (Exception e) {
            Log.e("MainActivity", "Error inicializando detector: " + e.getMessage());
        }
    }

    // Detectar objetos en una imagen
    public void detectObjects(Bitmap bitmap) {
        if (detector == null) return;

        // Ejecutar deteccion
        YOLOResult result = detector.detect(bitmap);

        // Procesar resultados
        for (Box box : result.getBoxes()) {
            String clase = box.getCls();           // Nombre de la clase
            float confianza = box.getConf();       // Confianza (0-1)
            RectF coords = box.getXywh();          // Coordenadas en pixeles

            Log.d("Detection",
                "Detectado: " + clase +
                " (Confianza: " + (confianza * 100) + "%)" +
                " en [" + coords.left + ", " + coords.top +
                ", " + coords.right + ", " + coords.bottom + "]"
            );
        }

        // Tiempo de inferencia
        Log.d("Performance", "Tiempo total: " + result.getSpeed() + " ms");
        Log.d("Performance", "FPS: " + result.getFps());
    }

    // Detectar desde camara (con rotacion)
    public void detectFromCamera(Bitmap bitmap, boolean isLandscape, boolean isFrontCamera) {
        YOLOResult result = detector.detect(
            bitmap,
            true,           // rotateForCamera = true
            isLandscape,    // isLandscape
            isFrontCamera   // isFrontCamera
        );

        // Procesar resultados...
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (detector != null) {
            detector.close();
        }
    }
}
```

## Parametros de Configuracion

| Parametro | Valor por Defecto | Descripcion |
|-----------|------------------|-------------|
| `confidenceThreshold` | 0.25 (25%) | Umbral minimo de confianza para considerar una deteccion |
| `iouThreshold` | 0.4 (40%) | Umbral de IoU para Non-Maximum Suppression |
| `numItemsThreshold` | 30 | Numero maximo de detecciones por imagen |

## Uso con CameraX

```java
// En tu ImageAnalysis.Analyzer
@Override
public void analyze(@NonNull ImageProxy image) {
    Bitmap bitmap = imageProxyToBitmap(image);  // Convertir ImageProxy a Bitmap

    YOLOResult result = detector.detect(
        bitmap,
        true,   // rotateForCamera
        false,  // isLandscape
        false   // isFrontCamera
    );

    // Procesar resultados...

    image.close();
}
```

## Notas Importantes

1. **Precision Identica**: Este codigo implementa exactamente el mismo algoritmo de postprocesamiento (NMS) que la app original, garantizando la misma precision.

2. **GPU vs CPU**: Usar `useGpu = true` mejora significativamente el rendimiento. Si falla la inicializacion del GPU, automaticamente usa CPU.

3. **Tamano de Entrada**: El modelo espera imagenes de cualquier tamano, el preprocesamiento las redimensiona automaticamente.

4. **Formato de Salida**: Los bounding boxes se devuelven tanto en coordenadas de pixeles (`getXywh()`) como normalizadas (`getXywhn()`).

5. **Thread Safety**: El detector no es thread-safe. Para uso concurrente, crea una instancia por thread o usa sincronizacion.

## Rendimiento Esperado

- **CPU**: ~100-200ms por imagen (dependiendo del dispositivo)
- **GPU**: ~30-50ms por imagen (dependiendo del dispositivo)
- **FPS en tiempo real**: 15-30 FPS con GPU en dispositivos modernos

## Troubleshooting

### Error: "No se puede cargar el modelo"
- Verifica que el archivo `best.tflite` este en `assets/models/`
- Asegurate de tener `noCompress "tflite"` en build.gradle

### Error: "GPU delegate error"
- El dispositivo puede no soportar GPU. El codigo automaticamente usa CPU como fallback.

### Detecciones incorrectas
- Verifica que `labels.txt` contenga las 12 clases en el orden correcto
- Ajusta `confidenceThreshold` si hay muchos falsos positivos/negativos
