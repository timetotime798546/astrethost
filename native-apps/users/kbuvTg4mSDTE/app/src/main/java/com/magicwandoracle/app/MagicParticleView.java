package com.magicwandoracle.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MagicParticleView extends View {

    private static class Particle {
        float x, y;
        float vx, vy;
        float radius;
        int color;
        float alpha;
    }

    private final List<Particle> particles = new ArrayList<>();
    private final Random random = new Random();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean isRunning = false;

    private final int[] colors = {
        Color.parseColor("#FFD700"), // Gold
        Color.parseColor("#FF1493"), // Pink
        Color.parseColor("#00FFFF"), // Cyan
        Color.parseColor("#8A2BE2"), // BlueViolet
        Color.parseColor("#FF00FF"), // Magenta
        Color.parseColor("#FFFFFF")  // Pure white core
    };

    public MagicParticleView(Context context) {
        super(context);
    }

    public MagicParticleView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void spawnParticles(float x, float y, int count) {
        for (int i = 0; i < count; i++) {
            Particle p = new Particle();
            p.x = x;
            p.y = y;
            double angle = random.nextDouble() * 2 * Math.PI;
            float speed = 3 + random.nextFloat() * 12;
            p.vx = (float) (Math.cos(angle) * speed);
            p.vy = (float) (Math.sin(angle) * speed);
            p.radius = 6 + random.nextFloat() * 12;
            p.color = colors[random.nextInt(colors.length)];
            p.alpha = 1.0f;
            particles.add(p);
        }
        if (!isRunning) {
            isRunning = true;
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (particles.isEmpty()) {
            isRunning = false;
            return;
        }

        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            paint.setColor(p.color);
            paint.setAlpha((int) (p.alpha * 255));
            canvas.drawCircle(p.x, p.y, p.radius, paint);

            // Update parameters
            p.x += p.vx;
            p.y += p.vy;
            p.vy += 0.15f; // simulated downward gravity pull
            p.alpha -= 0.025f; // gradual fade out
            p.radius *= 0.96f; // contract particle size

            if (p.alpha <= 0 || p.radius < 1) {
                particles.remove(i);
            }
        }

        invalidate();
    }
}