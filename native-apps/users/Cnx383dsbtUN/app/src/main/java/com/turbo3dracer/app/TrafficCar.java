package com.turbo3dracer.app;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

public class TrafficCar {
    public float z;              // World Z distance
    public float offset;         // Road lateral offset (-0.8 to 0.8)
    public float speed;          // Speed in world units
    public int color;
    public int secondaryColor;
    public float targetOffset;
    public float width = 240f;
    public float height = 120f;

    public TrafficCar(float z, float offset, float speed, int color, int secondaryColor) {
        this.z = z;
        this.offset = offset;
        this.targetOffset = offset;
        this.speed = speed;
        this.color = color;
        this.secondaryColor = secondaryColor;
    }

    public void update(float trackLength) {
        z += speed;
        if (z >= trackLength) {
            z -= trackLength;
        }

        // Slight AI lane drifting
        if (Math.abs(offset - targetOffset) > 0.01f) {
            offset += (targetOffset - offset) * 0.05f;
        } else if (Math.random() < 0.02) {
            float[] lanes = new float[]{-0.6f, -0.2f, 0.2f, 0.6f};
            targetOffset = lanes[(int) (Math.random() * lanes.length)];
        }
    }

    public void render(Canvas canvas, float screenX, float screenY, float scale, Paint paint) {
        float w = width * scale;
        float h = height * scale;
        if (w < 4 || h < 2) return;

        float left = screenX - (w / 2.0f);
        float top = screenY - h;

        // Shadow
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(120, 0, 0, 0));
        canvas.drawOval(new RectF(left - (w * 0.1f), screenY - (h * 0.25f), left + (w * 1.1f), screenY + (h * 0.15f)), paint);

        // Body
        paint.setColor(color);
        canvas.drawRoundRect(new RectF(left, top + (h * 0.35f), left + w, top + h), w * 0.15f, w * 0.15f, paint);

        // Cabin roof
        paint.setColor(secondaryColor);
        canvas.drawRoundRect(new RectF(left + (w * 0.18f), top, left + (w * 0.82f), top + (h * 0.55f)), w * 0.1f, w * 0.1f, paint);

        // Rear window
        paint.setColor(Color.rgb(20, 30, 45));
        canvas.drawRect(left + (w * 0.24f), top + (h * 0.08f), left + (w * 0.76f), top + (h * 0.36f), paint);

        // Taillights
        paint.setColor(Color.rgb(255, 30, 30));
        canvas.drawRect(left + (w * 0.08f), top + (h * 0.6f), left + (w * 0.28f), top + (h * 0.8f), paint);
        canvas.drawRect(left + (w * 0.72f), top + (h * 0.6f), left + (w * 0.92f), top + (h * 0.8f), paint);

        // Wheels
        paint.setColor(Color.rgb(15, 15, 18));
        canvas.drawRect(left - (w * 0.05f), top + (h * 0.7f), left + (w * 0.1f), top + h, paint);
        canvas.drawRect(left + (w * 0.9f), top + (h * 0.7f), left + (w * 1.05f), top + h, paint);
    }
}