package com.ultralytics.yolo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom View to draw detection bounding boxes over an image.
 *
 * Usage:
 *   detectionOverlay.setDetections(result.getBoxes());
 *   detectionOverlay.invalidate();
 */
public class DetectionOverlay extends View {

    private List<Box> detections = new ArrayList<>();
    private Paint boxPaint;
    private Paint textPaint;
    private Paint textBackgroundPaint;

    // Colors for different classes
    private static final int[] COLORS = {
        Color.rgb(255, 0, 0),     // biological - red
        Color.rgb(139, 69, 19),   // cardboard - brown
        Color.rgb(0, 191, 255),   // glass - light blue
        Color.rgb(192, 192, 192), // metal - silver
        Color.rgb(255, 255, 0),   // paper - yellow
        Color.rgb(0, 255, 0),     // plastic - green
        Color.rgb(50, 205, 50),   // plastic-Others - lime green
        Color.rgb(0, 128, 0),     // plastic-PET - dark green
        Color.rgb(34, 139, 34),   // plastic-PE_HD - forest green
        Color.rgb(144, 238, 144), // plastic-PP - light green
        Color.rgb(0, 100, 0),     // plastic-PS - dark green
        Color.rgb(128, 128, 128)  // trash - gray
    };

    private float scaleX = 1.0f;
    private float scaleY = 1.0f;

    public DetectionOverlay(Context context) {
        super(context);
        init();
    }

    public DetectionOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DetectionOverlay(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        boxPaint = new Paint();
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(4f);
        boxPaint.setAntiAlias(true);

        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(32f);
        textPaint.setAntiAlias(true);

        textBackgroundPaint = new Paint();
        textBackgroundPaint.setStyle(Paint.Style.FILL);
    }

    /**
     * Set the detections to draw.
     */
    public void setDetections(List<Box> detections) {
        this.detections = detections != null ? detections : new ArrayList<>();
    }

    /**
     * Set the scale factors for converting detection coordinates to view coordinates.
     *
     * @param scaleX Horizontal scale factor
     * @param scaleY Vertical scale factor
     */
    public void setScale(float scaleX, float scaleY) {
        this.scaleX = scaleX;
        this.scaleY = scaleY;
    }

    /**
     * Set scale based on original image size and view size.
     */
    public void setScale(int imageWidth, int imageHeight, int viewWidth, int viewHeight) {
        this.scaleX = (float) viewWidth / imageWidth;
        this.scaleY = (float) viewHeight / imageHeight;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for (Box box : detections) {
            int colorIndex = box.getIndex() % COLORS.length;
            int color = COLORS[colorIndex];

            boxPaint.setColor(color);
            textBackgroundPaint.setColor(color);

            // Get scaled coordinates
            RectF rect = box.getXywh();
            float left = rect.left * scaleX;
            float top = rect.top * scaleY;
            float right = rect.right * scaleX;
            float bottom = rect.bottom * scaleY;

            // Draw bounding box
            canvas.drawRect(left, top, right, bottom, boxPaint);

            // Prepare label text
            String label = box.getCls() + " " + String.format("%.1f%%", box.getConf() * 100);

            // Measure text
            float textWidth = textPaint.measureText(label);
            float textHeight = textPaint.getTextSize();

            // Draw text background
            float padding = 4f;
            canvas.drawRect(
                left,
                top - textHeight - padding * 2,
                left + textWidth + padding * 2,
                top,
                textBackgroundPaint
            );

            // Draw label text
            canvas.drawText(label, left + padding, top - padding, textPaint);
        }
    }

    /**
     * Clear all detections.
     */
    public void clear() {
        this.detections.clear();
        invalidate();
    }
}
