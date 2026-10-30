package com.slicemaster.app;

import android.graphics.Canvas;
import android.graphics.Paint;

public class Particle {

    public float x;
    public float y;
    public float vx;
    public float vy;
    public float radius;
    public int color;
    public float alpha;
    public float decay;
    public boolean active;

    public Particle() {
        active = false;
    }

    public void init(float x, float y, float vx, float vy, float radius, int color, float decay) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.radius = radius;
        this.color = color;
        this.alpha = 255f;
        this.decay = decay;
        this.active = true;
    }

    public void update(float dt) {
        if (!active) return;
        x += vx * dt;
        y += vy * dt;
        vy += 1200f * dt; // gravity
        alpha -= decay * dt;
        if (alpha <= 0) {
            alpha = 0;
            active = false;
        }
    }

    public void draw(Canvas canvas, Paint paint) {
        if (!active) return;
        paint.setColor(color);
        paint.setAlpha((int) alpha);
        canvas.drawCircle(x, y, radius, paint);
    }
}