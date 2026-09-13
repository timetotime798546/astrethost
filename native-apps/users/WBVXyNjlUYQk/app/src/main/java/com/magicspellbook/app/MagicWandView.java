package com.magicspellbook.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class MagicWandView extends View {

    private Paint paint;
    private ArrayList<MagicParticle> particles;
    private Random random;
    private int[] magicColors = {
        Color.parseColor("#00E5FF"), // Cyan
        Color.parseColor("#FFD600"), // Yellow Gold
        Color.parseColor("#FF1744"), // Ruby Red
        Color.parseColor("#E040FB"), // Purple
        Color.parseColor("#00E676")  // Emerald Green
    };

    public MagicWandView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        particles = new ArrayList<MagicParticle>();
        random = new Random();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw all particles
        Iterator<MagicParticle> iterator = particles.iterator();
        while (iterator.hasNext()) {
            MagicParticle p = iterator.next();
            p.update();
            if (p.alpha <= 0) {
                iterator.remove();
            } else {
                paint.setColor(p.color);
                paint.setAlpha((int) p.alpha);
                canvas.drawCircle(p.x, p.y, p.radius, paint);
            }
        }

        // If there are remaining particles, request redraw
        if (!particles.isEmpty()) {
            postInvalidateDelayed(16); // Target ~60fps
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
            // Spawn multiple sparkles at current coordinates
            for (int i = 0; i < 4; i++) {
                int randomColor = magicColors[random.nextInt(magicColors.length)];
                particles.add(new MagicParticle(x, y, randomColor, random));
            }
            invalidate();
            return true;
        }
        return super.onTouchEvent(event);
    }

    // Force external trigger from accelerometer or spell casting
    public void addSpellParticles(int customColor) {
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) {
            width = 500;
            height = 800;
        }

        for (int i = 0; i < 40; i++) {
            float rx = random.nextFloat() * width;
            float ry = random.nextFloat() * height;
            particles.add(new MagicParticle(rx, ry, customColor, random));
        }
        invalidate();
    }

    // Single particle model representation
    private static class MagicParticle {
        float x, y;
        float vx, vy;
        float radius;
        float alpha;
        int color;

        MagicParticle(float startX, float startY, int colorValue, Random rand) {
            this.x = startX;
            this.y = startY;
            this.color = colorValue;
            this.radius = rand.nextFloat() * 14f + 6f;
            this.alpha = 255;
            this.vx = (rand.nextFloat() - 0.5f) * 10f;
            this.vy = (rand.nextFloat() - 0.5f) * 10f;
        }

        void update() {
            x += vx;
            y += vy;
            vy += 0.15f; // Add gravity simulation
            alpha -= 5.0f; // Rapid fade out
            if (alpha < 0) {
                alpha = 0;
            }
        }
    }
}