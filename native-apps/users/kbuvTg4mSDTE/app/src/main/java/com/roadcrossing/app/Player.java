package com.roadcrossing.app;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

public class Player {
    public int gridX;
    public int gridY;

    // Drawing actual positions for smooth slide rendering
    public float drawX;
    public float drawY;

    public float size = 50f;
    private float targetX;
    private float targetY;

    // Animation values
    public float rotation = 0f;
    public float scale = 1.0f;
    public int flashCount = 0;

    public Player(int startGridX, int startGridY) {
        this.gridX = startGridX;
        this.gridY = startGridY;
        this.drawX = 0f;
        this.drawY = 0f;
        this.targetX = 0f;
        this.targetY = 0f;
    }

    public void setPosition(float x, float y) {
        this.drawX = x;
        this.drawY = y;
        this.targetX = x;
        this.targetY = y;
    }

    public void update(float speedFactor) {
        // Move gradually towards targets
        drawX += (targetX - drawX) * 0.25f * speedFactor;
        drawY += (targetY - drawY) * 0.25f * speedFactor;
    }

    public void setTarget(float tx, float ty) {
        this.targetX = tx;
        this.targetY = ty;
    }

    public void draw(Canvas canvas, Paint paint) {
        canvas.save();
        canvas.translate(drawX, drawY);
        canvas.rotate(rotation);
        canvas.scale(scale, scale);

        // Flash red/yellow when crashing
        int baseColor = (flashCount % 2 != 0) ? Color.RED : Color.YELLOW;

        // Shadow under player
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(55, 0, 0, 0));
        float r = size * 0.45f;
        canvas.drawOval(-r * 0.8f, r * 0.6f, r * 0.8f, r * 1.0f, paint);

        // Body Draw
        paint.setColor(baseColor);
        canvas.drawCircle(0, 0, r, paint);

        // Chicken Head
        paint.setColor(Color.rgb(255, 245, 150));
        canvas.drawCircle(0, -r * 0.5f, r * 0.7f, paint);

        // Eye Dots
        paint.setColor(Color.BLACK);
        canvas.drawCircle(-r * 0.25f, -r * 0.6f, r * 0.12f, paint);
        canvas.drawCircle(r * 0.25f, -r * 0.6f, r * 0.12f, paint);

        // Red Crown / Comb on Head
        paint.setColor(Color.rgb(244, 67, 54));
        canvas.drawOval(-r * 0.15f, -r * 1.3f, r * 0.15f, -r * 0.9f, paint);

        // Yellow Beak
        paint.setColor(Color.rgb(255, 152, 0));
        Path beak = new Path();
        beak.moveTo(0, -r * 0.5f);
        beak.lineTo(-r * 0.2f, -r * 0.3f);
        beak.lineTo(r * 0.2f, -r * 0.3f);
        beak.close();
        canvas.drawPath(beak, paint);

        // Orange Feet
        paint.setColor(Color.rgb(255, 152, 0));
        canvas.drawRect(-r * 0.5f, r * 0.8f, -r * 0.1f, r * 1.1f, paint);
        canvas.drawRect(r * 0.1f, r * 0.8f, r * 0.5f, r * 1.1f, paint);

        canvas.restore();
    }
}