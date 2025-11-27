package com.ultralytics.yolo;

import android.graphics.Bitmap;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the result of a YOLO detection inference.
 */
public final class YOLOResult {
    private final Size origShape;
    private final List<Box> boxes;
    private final Bitmap annotatedImage;
    private final double speed;
    private final Double fps;
    private final Bitmap originalImage;
    private final List<String> names;

    public YOLOResult(Size origShape, List<Box> boxes, Bitmap annotatedImage,
                      double speed, Double fps, Bitmap originalImage, List<String> names) {
        this.origShape = origShape;
        this.boxes = boxes != null ? boxes : new ArrayList<>();
        this.annotatedImage = annotatedImage;
        this.speed = speed;
        this.fps = fps;
        this.originalImage = originalImage;
        this.names = names != null ? names : new ArrayList<>();
    }

    public final Size getOrigShape() {
        return this.origShape;
    }

    public final List<Box> getBoxes() {
        return this.boxes;
    }

    public final Bitmap getAnnotatedImage() {
        return this.annotatedImage;
    }

    public final double getSpeed() {
        return this.speed;
    }

    public final Double getFps() {
        return this.fps;
    }

    public final Bitmap getOriginalImage() {
        return this.originalImage;
    }

    public final List<String> getNames() {
        return this.names;
    }

    @Override
    public String toString() {
        return "YOLOResult(origShape=" + this.origShape + ", boxes=" + this.boxes +
               ", speed=" + this.speed + ", fps=" + this.fps + ", names=" + this.names + ")";
    }
}
