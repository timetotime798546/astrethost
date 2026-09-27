package com.roadcrossing.app;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

public class Vehicle {
    public float x;
    public float width;
    public float height;
    public int type; // 0=Small, 1=Sedan, 2=Taxi, 3=Truck, 4=Fast Sport
    public int color;

    public Vehicle(float startX, float width, float height, int type, int color) {
        this.x = startX;
        this.width = width;
        this.height = height;
        this.type = type;
        this.color = color;
    }

    public void update(float speed, int direction, float screenWidth, float speedFactor) {
        x += speed * direction * speedFactor;

        // Reset positions at offscreen limits
        float buffer = width + 60;
        if (direction == 1 && x > screenWidth + buffer) {
            x = -buffer;
        } else if (direction == -1 && x < -buffer) {
            x = screenWidth + buffer;
        }
    }

    public void draw(Canvas canvas, float centerY, Paint paint) {
        paint.setStyle(Paint.Style.FILL);

        // Shadow layer
        paint.setColor(Color.argb(55, 0, 0, 0));
        canvas.drawRoundRect(new RectF(x, centerY - height/2 + 6, x + width, centerY + height/2 + 6), 10, 10, paint);

        // Main Base Vehicle Body
        paint.setColor(color);
        RectF baseRect = new RectF(x, centerY - height/2, x + width, centerY + height/2);
        canvas.drawRoundRect(baseRect, 10, 10, paint);

        // Sub decorations per model type
        if (type == 3) { // Large heavy truck container
            paint.setColor(Color.rgb(220, 222, 225));
            RectF container = new RectF(x + 8, centerY - height/2.2f, x + width - 30, centerY + height/2.2f);
            canvas.drawRoundRect(container, 5, 5, paint);
        } else if (type == 2) { // Yellow Taxi Roof Signboard
            paint.setColor(Color.BLACK);
            canvas.drawRect(x + width/2 - 12, centerY - height/2 - 4, x + width/2 + 12, centerY - height/2, paint);
            paint.setColor(Color.rgb(255, 193, 7));
            canvas.drawRect(x + width/2 - 8, centerY - height/2 - 3, x + width/2 + 8, centerY - height/2, paint);
        } else if (type == 4) { // Fast Sports Stripes
            paint.setColor(Color.WHITE);
            canvas.drawRect(x + 10, centerY - height/4.5f, x + width - 10, centerY - height/6f, paint);
            canvas.drawRect(x + 10, centerY + height/6f, x + width - 10, centerY + height/4.5f, paint);
        }

        // Windshields & Cabins
        paint.setColor(Color.rgb(45, 45, 45)); // Windows tint
        float startWin = x + width * 0.2f;
        float endWin = x + width * 0.8f;
        if (type == 3) {
            startWin = x + width - 25;
            endWin = x + width - 5;
        }
        RectF windshield = new RectF(startWin, centerY - height/2.5f, endWin, centerY + height/2.5f);
        canvas.drawRoundRect(windshield, 4, 4, paint);

        // Front Headlights
        paint.setColor(Color.rgb(255, 235, 59));
        canvas.drawCircle(x + width - 4, centerY - height/3f, 4, paint);
        canvas.drawCircle(x + width - 4, centerY + height/3f, 4, paint);

        // Taillights
        paint.setColor(Color.rgb(244, 67, 54));
        canvas.drawCircle(x + 4, centerY - height/3f, 3, paint);
        canvas.drawCircle(x + 4, centerY + height/3f, 3, paint);

        // Rubber Tires
        paint.setColor(Color.rgb(28, 28, 28));
        canvas.drawCircle(x + width * 0.22f, centerY - height/2, 5, paint);
        canvas.drawCircle(x + width * 0.22f, centerY + height/2, 5, paint);
        canvas.drawCircle(x + width * 0.78f, centerY - height/2, 5, paint);
        canvas.drawCircle(x + width * 0.78f, centerY + height/2, 5, paint);
    }
}