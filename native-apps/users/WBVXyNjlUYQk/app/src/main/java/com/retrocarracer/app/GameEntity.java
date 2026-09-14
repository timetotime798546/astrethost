package com.retrocarracer.app;

import android.graphics.Color;

public class GameEntity {
    public static final int TYPE_CAR = 0;
    public static final int TYPE_COIN = 1;
    public static final int TYPE_SLICK = 2;

    public float x;
    public float y;
    public int type;
    public int color;
    public boolean collected;
    public float speedY;
    public float width;
    public float height;

    public GameEntity(float x, float y, int type, float width, float height) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.width = width;
        this.height = height;
        this.collected = false;

        if (type == TYPE_CAR) {
            int[] colors = {Color.BLUE, Color.YELLOW, Color.CYAN, Color.MAGENTA};
            int randomIndex = (int) (Math.random() * colors.length);
            this.color = colors[randomIndex];
            this.speedY = 10.0f + (float) (Math.random() * 8.0f);
        } else if (type == TYPE_COIN) {
            this.color = Color.rgb(255, 215, 0); // Golden Yellow
            this.speedY = 10.0f;
        } else {
            this.color = Color.DKGRAY;
            this.speedY = 10.0f;
        }
    }

    public void update(float baseSpeed) {
        this.y += (this.speedY + baseSpeed * 0.5f);
    }
}