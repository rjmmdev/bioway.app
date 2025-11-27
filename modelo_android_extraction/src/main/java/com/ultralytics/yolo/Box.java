package com.ultralytics.yolo;

import android.graphics.RectF;

/**
 * Represents a detected bounding box with class information and confidence.
 */
public final class Box {
    private String cls;
    private float conf;
    private int index;
    private final RectF xywh;      // Bounding box in pixel coordinates
    private final RectF xywhn;     // Bounding box in normalized coordinates (0-1)

    public Box(int index, String cls, float conf, RectF xywh, RectF xywhn) {
        this.index = index;
        this.cls = cls;
        this.conf = conf;
        this.xywh = xywh;
        this.xywhn = xywhn;
    }

    public final int getIndex() {
        return this.index;
    }

    public final void setIndex(int index) {
        this.index = index;
    }

    public final String getCls() {
        return this.cls;
    }

    public final void setCls(String cls) {
        this.cls = cls;
    }

    public final float getConf() {
        return this.conf;
    }

    public final void setConf(float conf) {
        this.conf = conf;
    }

    public final RectF getXywh() {
        return this.xywh;
    }

    public final RectF getXywhn() {
        return this.xywhn;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Box)) {
            return false;
        }
        Box box = (Box) other;
        return this.index == box.index &&
               this.cls.equals(box.cls) &&
               Float.compare(this.conf, box.conf) == 0 &&
               this.xywh.equals(box.xywh) &&
               this.xywhn.equals(box.xywhn);
    }

    @Override
    public int hashCode() {
        return (((((((Integer.hashCode(this.index) * 31) + this.cls.hashCode()) * 31) +
                Float.hashCode(this.conf)) * 31) + this.xywh.hashCode()) * 31) + this.xywhn.hashCode();
    }

    @Override
    public String toString() {
        return "Box(index=" + this.index + ", cls=" + this.cls + ", conf=" + this.conf +
               ", xywh=" + this.xywh + ", xywhn=" + this.xywhn + ")";
    }
}
