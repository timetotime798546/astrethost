package com.retrobrickbreaker.app;

import android.graphics.RectF;

public class Brick {
    private RectF rect;
    private int maxHits;
    private int currentHits;
    private int color;
    private int points;
    private boolean destroyed;

    public Brick(float left, float top, float right, float bottom, int color, int hits, int points) {
        this.rect = new RectF(left, top, right, bottom);
        this.color = color;
        this.maxHits = hits;
        this.currentHits = 0;
        this.points = points;
        this.destroyed = false;
    }

    public RectF getRect() {
        return rect;
    }

    public int getColor() {
        return color;
    }

    public int getPoints() {
        return points;
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    public void setDestroyed(boolean destroyed) {
        this.destroyed = destroyed;
    }

    public boolean hit() {
        currentHits++;
        if (currentHits >= maxHits) {
            destroyed = true;
            return true;
        }
        return false;
    }

    public int getOpacityAlpha() {
        if (maxHits <= 1) return 255;
        float remainingRatio = (float) (maxHits - currentHits) / (float) maxHits;
        return Math.max(80, (int) (255 * remainingRatio));
    }
}