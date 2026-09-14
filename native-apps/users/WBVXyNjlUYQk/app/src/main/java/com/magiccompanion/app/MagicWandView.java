package com.magiccompanion.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MagicWandView extends View {

    private Paint paint;
    private List<Particle> particles;
    private Random random;
    private int currentSpellType = 0;
    private float flashBrightness = 0.0f;

    public static class Particle {
        float x, y;
        float vx, vy;
        float size;
        float alpha;
        int color;

        public Particle(float x, float y, float vx, float vy, float size, int color) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.size = size;
            this.alpha = 1.0f;
            this.color = color;
        }

        public void update() {
            x += vx;
            y += vy;
            vy += 0.1f;
            alpha -= 0.02f;
            if (size > 2) {
                size -= 0.2f;
            }
        }
    }

    public MagicWandView(Context context) {
        super(context);
        init();
    }

    public MagicWandView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        particles = new ArrayList<>();
        random = new Random();
    }

    public void setSpellType(int spellType) {
        this.currentSpellType = spellType;
        if (spellType == 0) {
            flashBrightness = 1.0f;
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (flashBrightness > 0.0f) {
            int flashColor = Color.argb((int) (flashBrightness * 120), 255, 253, 220);
            canvas.drawColor(flashColor);
            flashBrightness -= 0.05f;
            if (flashBrightness < 0.0f) {
                flashBrightness = 0.0f;
            }
        }

        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.update();
            if (p.alpha <= 0.0f || p.size <= 0.0f) {
                particles.remove(i);
                continue;
            }

            paint.setColor(p.color);
            paint.setAlpha((int) (p.alpha * 255));
            canvas.drawCircle(p.x, p.y, p.size, paint);
        }

        if (!particles.isEmpty() || flashBrightness > 0.0f) {
            postInvalidateOnAnimation();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
            float x = event.getX();
            float y = event.getY();

            int count = (action == MotionEvent.ACTION_DOWN) ? 15 : 6;
            for (int i = 0; i < count; i++) {
                float vx = (random.nextFloat() - 0.5f) * 8.0f;
                float vy = (random.nextFloat() - 0.5f) * 8.0f;
                float size = 15f + random.nextFloat() * 20f;
                int color = getSpellParticleColor();
                particles.add(new Particle(x, y, vx, vy, size, color));
            }
            invalidate();
            return true;
        }
        return super.onTouchEvent(event);
    }

    private int getSpellParticleColor() {
        switch (currentSpellType) {
            case 0:
                int r = 240 + random.nextInt(16);
                int g = 230 + random.nextInt(26);
                int b = 150 + random.nextInt(106);
                return Color.rgb(r, g, b);
            case 1:
                return Color.rgb(255, 60 + random.nextInt(100), 10);
            case 2:
                return Color.rgb(100 + random.nextInt(100), 210 + random.nextInt(45), 255);
            case 3:
                return Color.rgb(20 + random.nextInt(80), 255, 100 + random.nextInt(100));
            default:
                return Color.WHITE;
        }
    }
}