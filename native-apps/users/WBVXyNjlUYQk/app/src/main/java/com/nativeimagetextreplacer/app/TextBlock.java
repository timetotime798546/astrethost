package com.nativeimagetextreplacer.app;

import android.graphics.RectF;

public class TextBlock {
    public RectF rect;
    public String originalText;
    public String replacementText;
    public int textColor;
    public int backgroundColor;
    public float textSize;
    public boolean isBold;
    public String alignment; // "LEFT", "CENTER", "RIGHT"
    public boolean isReplaced;

    public TextBlock(RectF rect, String originalText) {
        this.rect = rect;
        this.originalText = originalText;
        this.replacementText = "";
        this.textColor = 0xFF000000;
        this.backgroundColor = 0xFFFFFFFF;
        this.textSize = 40.0f;
        this.isBold = false;
        this.alignment = "CENTER";
        this.isReplaced = false;
    }
}