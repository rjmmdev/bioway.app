package com.example.wastedetector;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ultralytics.yolo.Box;
import com.ultralytics.yolo.DetectionOverlay;
import com.ultralytics.yolo.ObjectDetector;
import com.ultralytics.yolo.YOLOResult;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Ejemplo de Activity que muestra como usar el ObjectDetector.
 *
 * IMPORTANTE: Este es un ejemplo. Debes adaptarlo a tu proyecto.
 *
 * Layout requerido (activity_example.xml):
 *
 * <?xml version="1.0" encoding="utf-8"?>
 * <FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
 *     android:layout_width="match_parent"
 *     android:layout_height="match_parent">
 *
 *     <ImageView
 *         android:id="@+id/imageView"
 *         android:layout_width="match_parent"
 *         android:layout_height="match_parent"
 *         android:scaleType="fitCenter" />
 *
 *     <com.ultralytics.yolo.DetectionOverlay
 *         android:id="@+id/detectionOverlay"
 *         android:layout_width="match_parent"
 *         android:layout_height="match_parent" />
 *
 *     <TextView
 *         android:id="@+id/resultsText"
 *         android:layout_width="match_parent"
 *         android:layout_height="wrap_content"
 *         android:layout_gravity="bottom"
 *         android:background="#80000000"
 *         android:padding="16dp"
 *         android:textColor="#FFFFFF"
 *         android:textSize="14sp" />
 *
 * </FrameLayout>
 */
public class ExampleActivity extends AppCompatActivity {

    private static final String TAG = "ExampleActivity";

    private ObjectDetector detector;
    private ImageView imageView;
    private DetectionOverlay detectionOverlay;
    private TextView resultsText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // setContentView(R.layout.activity_example);

        // Inicializar vistas
        // imageView = findViewById(R.id.imageView);
        // detectionOverlay = findViewById(R.id.detectionOverlay);
        // resultsText = findViewById(R.id.resultsText);

        // Inicializar detector
        initDetector();

        // Ejemplo: detectar en una imagen de assets
        // detectFromAssets("test_image.jpg");
    }

    /**
     * Inicializa el detector de objetos.
     */
    private void initDetector() {
        try {
            // Cargar etiquetas desde assets
            List<String> labels = ObjectDetector.loadLabelsFromAssets(
                this,
                "labels/labels.txt"
            );

            Log.d(TAG, "Etiquetas cargadas: " + labels.size());
            for (int i = 0; i < labels.size(); i++) {
                Log.d(TAG, "  " + i + ": " + labels.get(i));
            }

            // Crear detector con GPU habilitado
            detector = new ObjectDetector(
                this,
                "models/best.tflite",
                labels,
                true  // useGpu = true para mejor rendimiento
            );

            // Configurar umbrales (mismos valores que la app original)
            detector.setConfidenceThreshold(0.25f);  // 25% minimo
            detector.setIouThreshold(0.4f);           // 40% IoU para NMS
            detector.setNumItemsThreshold(30);        // Max 30 detecciones

            Log.d(TAG, "Detector inicializado correctamente");
            Log.d(TAG, "Tamano de entrada del modelo: " +
                detector.getInputWidth() + "x" + detector.getInputHeight());

        } catch (Exception e) {
            Log.e(TAG, "Error inicializando detector: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(this, "Error inicializando detector", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Detecta objetos en una imagen desde assets.
     */
    private void detectFromAssets(String imageName) {
        try {
            // Cargar imagen desde assets
            InputStream is = getAssets().open(imageName);
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            is.close();

            if (bitmap == null) {
                Log.e(TAG, "No se pudo cargar la imagen: " + imageName);
                return;
            }

            // Mostrar imagen
            imageView.setImageBitmap(bitmap);

            // Detectar objetos
            detectObjects(bitmap);

        } catch (IOException e) {
            Log.e(TAG, "Error cargando imagen: " + e.getMessage());
        }
    }

    /**
     * Detecta objetos en un Bitmap.
     */
    public void detectObjects(Bitmap bitmap) {
        if (detector == null) {
            Log.e(TAG, "Detector no inicializado");
            return;
        }

        // Ejecutar deteccion
        long startTime = System.currentTimeMillis();
        YOLOResult result = detector.detect(bitmap);
        long endTime = System.currentTimeMillis();

        // Mostrar resultados
        StringBuilder sb = new StringBuilder();
        sb.append("Detecciones: ").append(result.getBoxes().size()).append("\n");
        sb.append("Tiempo: ").append(result.getSpeed()).append(" ms\n");
        sb.append("FPS: ").append(String.format("%.1f", result.getFps())).append("\n\n");

        for (Box box : result.getBoxes()) {
            String clase = box.getCls();
            float confianza = box.getConf();
            RectF coords = box.getXywh();

            sb.append(clase)
              .append(": ")
              .append(String.format("%.1f%%", confianza * 100))
              .append("\n");

            Log.d(TAG, "Detectado: " + clase +
                " (Confianza: " + (confianza * 100) + "%)" +
                " en [" + coords.left + ", " + coords.top +
                ", " + coords.right + ", " + coords.bottom + "]");
        }

        // Actualizar UI
        resultsText.setText(sb.toString());

        // Dibujar bounding boxes
        detectionOverlay.setDetections(result.getBoxes());
        detectionOverlay.setScale(
            bitmap.getWidth(),
            bitmap.getHeight(),
            imageView.getWidth(),
            imageView.getHeight()
        );
        detectionOverlay.invalidate();
    }

    /**
     * Detecta objetos desde camara con rotacion apropiada.
     */
    public void detectFromCamera(Bitmap bitmap, boolean isLandscape, boolean isFrontCamera) {
        if (detector == null) return;

        YOLOResult result = detector.detect(
            bitmap,
            true,           // rotateForCamera = true
            isLandscape,
            isFrontCamera
        );

        // Procesar resultados...
        detectionOverlay.setDetections(result.getBoxes());
        detectionOverlay.invalidate();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (detector != null) {
            detector.close();
            detector = null;
        }
    }
}
