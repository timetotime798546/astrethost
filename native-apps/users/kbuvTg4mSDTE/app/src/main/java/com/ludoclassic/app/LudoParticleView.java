package com.ludoclassic.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.Random;

public class LudoParticleView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ArrayList<Particle> particles = new ArrayList<>();
    private final Random random = new Random();
    private boolean isAnimating = true;

    private static class Particle {
        float x, y;
        float radius;
        float vx, vy;
        int color;
    }

    public LudoParticleView(Context context) {
        super(context);
        init();
    }

    public LudoParticleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        int[] colors = {0x26EF4444, 0x2610B981, 0x26F59E0B, 0x263B82F6};
        for (int i = 0; i < 20; i++) {
            Particle p = new Particle();
            p.radius = 20f + random.nextFloat() * 40f;
            p.color = colors[random.nextInt(colors.length)];
            p.vx = -1.2f + random.nextFloat() * 2.4f;
            p.vy = -1.2f + random.nextFloat() * 2.4f;
            particles.add(p);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        for (Particle p : particles) {
            p.x = random.nextFloat() * w;
            p.y = random.nextFloat() * h;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        for (Particle p : particles) {
            p.x += p.vx;
            p.y += p.vy;

            if (p.x < -p.radius) p.x = w + p.radius;
            else if (p.x > w + p.radius) p.x = -p.radius;

            if (p.y < -p.radius) p.y = h + p.radius;
            else if (p.y > h + p.radius) p.y = -p.radius;

            paint.setColor(p.color);
            canvas.drawCircle(p.x, p.y, p.radius, paint);
        }

        if (isAnimating) {
            postInvalidateDelayed(30);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        isAnimating = false;
    }
}