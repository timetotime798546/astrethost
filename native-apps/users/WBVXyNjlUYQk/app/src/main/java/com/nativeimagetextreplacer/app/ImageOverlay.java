package com.nativeimagetextreplacer.app;

import android.graphics.Bitmap;
import android.graphics.RectF;

public class ImageOverlay {
    public Bitmap bitmap;
    public RectF rect; // in main bitmap coordinate space

    public ImageOverlay(Bitmap bitmap, RectF rect) {
        this.bitmap = bitmap;
        this.rect = rect;
    }
}