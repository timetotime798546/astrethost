package com.roadcrossing.app;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Lane {
    public int index;
    public int type; // 0 = Grass Safe Area, 1 = Road
    public int direction; // -1 = Left, 1 = Right
    public float speed;
    public int color;
    public List<Vehicle> vehicles;

    // Grass assets (coordinates percentages of screen width)
    public float[] itemXCoords;
    public float[] itemSizes;

    public Lane(int index, int type, int direction, float speed, int color) {
        this.index = index;
        this.type = type;
        this.direction = direction;
        this.speed = speed;
        this.color = color;
        this.vehicles = new ArrayList<Vehicle>();

        // Generate tree decorations inside safe zones (except index 0 starting spot)
        if (type == 0 && index > 0) {
            Random r = new Random();
            int itemsCount = r.nextInt(3) + 2; // 2 to 4 items
            itemXCoords = new float[itemsCount];
            itemSizes = new float[itemsCount];
            for (int i = 0; i < itemsCount; i++) {
                itemXCoords[i] = r.nextFloat();
                itemSizes[i] = 16f + r.nextFloat() * 18f;
            }
        }
    }

    public void draw(Canvas canvas, float y, float height, float screenWidth, Paint paint) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        canvas.drawRect(0, y, screenWidth, y + height, paint);

        if (type == 1) { // Road dividing visual markers
            paint.setColor(Color.WHITE);
            paint.setStrokeWidth(3f);
            float midY = y + height / 2;
            float dashLen = 35f;
            float dashGap = 30f;
            for (float curX = 0; curX < screenWidth; curX += (dashLen + dashGap)) {
                canvas.drawLine(curX, midY, curX + dashLen, midY, paint);
            }

            // Road border lines
            paint.setColor(Color.rgb(255, 235, 59));
            canvas.drawLine(0, y + 2, screenWidth, y + 2, paint);
            canvas.drawLine(0, y + height - 2, screenWidth, y + height - 2, paint);
        } else { // Safe grass bushes
            if (itemXCoords != null) {
                for (int i = 0; i < itemXCoords.length; i++) {
                    float tx = itemXCoords[i] * screenWidth;
                    float s = itemSizes[i];

                    // Brown Wood Trunk
                    paint.setColor(Color.rgb(109, 76, 65));
                    canvas.drawRect(tx - s * 0.12f, y + height - s * 0.35f, tx + s * 0.12f, y + height, paint);

                    // Green foliage shadow
                    paint.setColor(Color.argb(50, 0, 0, 0));
                    canvas.drawCircle(tx, y + height - s * 0.35f + 2, s * 0.42f, paint);

                    // Green Leaves
                    paint.setColor(Color.rgb(56, 142, 60));
                    canvas.drawCircle(tx, y + height - s * 0.35f, s * 0.4f, paint);

                    // Light accent leaf spot
                    paint.setColor(Color.rgb(120, 190, 85));
                    canvas.drawCircle(tx - s * 0.08f, y + height - s * 0.42f, s * 0.22f, paint);
                }
            }
        }
    }
}