package com.retrobrickbreaker.app;

import android.graphics.Canvas;
import android.graphics.Paint;

public class Particle {
    private float x;
    private float y;
    private float vx;
    private float vy;
    private float radius;
    private int color;
    private int alpha;
    private int lifetime;
    private int maxLifetime;

    public Particle(float x, float y, int color) {
        this.x = x;
        this.y = y;
        this.color = color;
        this.alpha = 255;
        this.radius = 4f + (float) (Math.random() * 6f);
        this.vx = (float) ((Math.random() - 0.5) * 16.0);
        this.vy = (float) ((Math.random() - 0.5) * 16.0);
        this.maxLifetime = 15 + (int) (Math.random() * 15);
        this.lifetime = maxLifetime;
    }

    public boolean update() {
        x += vx;
        y += vy;
        vy += 0.4f; // Gravity
        lifetime--;
        alpha = (int) (255.0 * ((double) lifetime / (double) maxLifetime));
        return lifetime > 0;
    }

    public void draw(Canvas canvas, Paint paint) {
        if (alpha <= 0) return;
        paint.setColor(color);
        paint.setAlpha(alpha);
        canvas.drawCircle(x, y, radius, paint);
    }
}