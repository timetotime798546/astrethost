package com.animationsandbox.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.animation.ValueAnimator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class ParticleExplosionView extends View {
    private static class Particle {
        float x, y;
        float vx, vy;
        float radius;
        int color;
        float alpha;
        float decayRate;
    }

    private final List<Particle> mParticles = new ArrayList<Particle>();
    private final Random mRandom = new Random();
    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private ValueAnimator mAnimator;

    private int mParticleCount = 40;
    private float mParticleSpeed = 1.0f;
    private float mParticleLifespan = 1.0f;

    private static final int[] BURST_COLORS = {
        Color.parseColor("#FF5722"), // Intense orange
        Color.parseColor("#FFC107"), // Vivid gold
        Color.parseColor("#00E676"), // Neon green
        Color.parseColor("#00B0FF"), // Deep sky blue
        Color.parseColor("#D500F9"), // Vibrant purple
        Color.parseColor("#FF1744")  // Crimson pink
    };

    public ParticleExplosionView(Context context) {
        super(context);
        init();
    }

    public ParticleExplosionView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        mPaint.setStyle(Paint.Style.FILL);
        mTextPaint.setColor(Color.parseColor("#95A5A6"));
        mTextPaint.setTextSize(32f);
        mTextPaint.setTextAlign(Paint.Align.CENTER);

        mAnimator = ValueAnimator.ofFloat(0f, 1f);
        mAnimator.setRepeatCount(ValueAnimator.INFINITE);
        mAnimator.setDuration(1000);
        mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (!mParticles.isEmpty()) {
                    stepExplosionParticles();
                    invalidate();
                }
            }
        });
        mAnimator.start();
    }

    public void setParticleCount(int count) {
        mParticleCount = count;
    }

    public void setParticleSpeed(float speedFactor) {
        mParticleSpeed = speedFactor;
    }

    public void setParticleLifespan(float lifespanFactor) {
        mParticleLifespan = lifespanFactor;
    }

    public void clearParticles() {
        mParticles.clear();
        invalidate();
    }

    private void stepExplosionParticles() {
        Iterator<Particle> iterator = mParticles.iterator();
        while (iterator.hasNext()) {
            Particle p = iterator.next();
            // Move positions
            p.x += p.vx;
            p.y += p.vy;

            // Sinking gravity acceleration effect on individual particle items
            p.vy += 0.15f;

            // Apply friction and size shrinkage
            p.radius *= 0.96f;
            p.alpha -= p.decayRate;

            if (p.alpha <= 0.0f || p.radius < 0.5f) {
                iterator.remove();
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            triggerParticleBurst(event.getX(), event.getY());
            return true;
        }
        return super.onTouchEvent(event);
    }

    private void triggerParticleBurst(float x, float y) {
        for (int i = 0; i < mParticleCount; i++) {
            Particle p = new Particle();
            p.x = x;
            p.y = y;

            double radiansAngle = mRandom.nextDouble() * 2.0 * Math.PI;
            float forceMagnitude = (2f + mRandom.nextFloat() * 12f) * mParticleSpeed;

            p.vx = (float) (Math.cos(radiansAngle) * forceMagnitude);
            p.vy = (float) (Math.sin(radiansAngle) * forceMagnitude);

            p.radius = 8f + mRandom.nextFloat() * 14f;
            p.color = BURST_COLORS[mRandom.nextInt(BURST_COLORS.length)];
            p.alpha = 1.0f;

            // Decay base value is divided by particle lifespan multiplier slider
            float baseDecay = 0.015f + mRandom.nextFloat() * 0.02f;
            p.decayRate = baseDecay / mParticleLifespan;

            mParticles.add(p);
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawColor(Color.parseColor("#121212")); // Deep dark canvas backdrop

        if (mParticles.isEmpty()) {
            canvas.drawText("Tap anywhere to spawn color explosions!", getWidth() / 2f, getHeight() / 2f, mTextPaint);
        } else {
            for (int i = 0; i < mParticles.size(); i++) {
                Particle p = mParticles.get(i);
                mPaint.setColor(p.color);
                mPaint.setAlpha((int) (p.alpha * 255));
                canvas.drawCircle(p.x, p.y, p.radius, mPaint);
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mAnimator != null) {
            mAnimator.cancel();
        }
        super.onDetachedFromWindow();
    }
}