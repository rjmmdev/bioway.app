package com.ultralytics.yolo;

/**
 * Simple Size class to hold width and height dimensions.
 */
public final class Size {
    private final int height;
    private final int width;

    public Size(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public final int getHeight() {
        return this.height;
    }

    public final int getWidth() {
        return this.width;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Size)) {
            return false;
        }
        Size size = (Size) other;
        return this.width == size.width && this.height == size.height;
    }

    @Override
    public int hashCode() {
        return (Integer.hashCode(this.width) * 31) + Integer.hashCode(this.height);
    }

    @Override
    public String toString() {
        return "Size(width=" + this.width + ", height=" + this.height + ")";
    }
}
