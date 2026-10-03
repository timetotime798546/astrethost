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
import java.util.List;
import java.util.Random;

public class PhysicsSandboxView extends View {
    private static class Ball {
        float x, y;
        float vx, vy;
        float radius;
        int color;
    }

    private final List<Ball> mBalls = new ArrayList<Ball>();
    private final Random mRandom = new Random();
    private final Paint mBallPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float mGravity = 1.2f;
    private float mBounciness = 0.75f;
    private ValueAnimator mAnimator;

    private static final int[] PALETTE = {
        Color.parseColor("#E74C3C"), // Red
        Color.parseColor("#3498DB"), // Blue
        Color.parseColor("#2ECC71"), // Green
        Color.parseColor("#F1C40F"), // Yellow
        Color.parseColor("#9B59B6"), // Purple
        Color.parseColor("#1ABC9C"), // Turquoise
        Color.parseColor("#E67E22")  // Orange
    };

    public PhysicsSandboxView(Context context) {
        super(context);
        init();
    }

    public PhysicsSandboxView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        mBallPaint.setStyle(Paint.Style.FILL);
        mTextPaint.setColor(Color.parseColor("#7F8C8D"));
        mTextPaint.setTextSize(32f);
        mTextPaint.setTextAlign(Paint.Align.CENTER);

        // Standard timing animation tick loop
        mAnimator = ValueAnimator.ofFloat(0f, 1f);
        mAnimator.setRepeatCount(ValueAnimator.INFINITE);
        mAnimator.setDuration(1000);
        mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                updatePhysicsState();
                invalidate();
            }
        });
        mAnimator.start();
    }

    public void setGravity(float gravity) {
        mGravity = gravity;
    }

    public void setBounciness(float bounciness) {
        mBounciness = bounciness;
    }

    public void clearBalls() {
        mBalls.clear();
        invalidate();
    }

    private void updatePhysicsState() {
        int width = getWidth();
        int height = getHeight();
        if (width == 0 || height == 0) return;

        for (int i = 0; i < mBalls.size(); i++) {
            Ball b = mBalls.get(i);

            // Apply incremental gravity acceleration to vertical velocity
            b.vy += mGravity;

            // Step position changes
            b.x += b.vx;
            b.y += b.vy;

            // Bounce calculation off ground boundaries
            if (b.y + b.radius > height) {
                b.y = height - b.radius;
                b.vy = -b.vy * mBounciness;
                
                // Ground friction decay
                b.vx = b.vx * 0.98f;
                if (Math.abs(b.vy) < 1.0f) {
                    b.vy = 0f;
                }
            }

            // Left boundary check
            if (b.x - b.radius < 0) {
                b.x = b.radius;
                b.vx = -b.vx * mBounciness;
            }

            // Right boundary check
            if (b.x + b.radius > width) {
                b.x = width - b.radius;
                b.vx = -b.vx * mBounciness;
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            addNewBall(event.getX(), event.getY());
            return true;
        }
        return super.onTouchEvent(event);
    }

    private void addNewBall(float x, float y) {
        Ball b = new Ball();
        b.x = x;
        b.y = y;
        // Set randomized horizontal velocity dynamics and scale parameters
        b.vx = -12f + mRandom.nextFloat() * 24f;
        b.vy = -8f + mRandom.nextFloat() * 8f;
        b.radius = 35f + mRandom.nextFloat() * 30f;
        b.color = PALETTE[mRandom.nextInt(PALETTE.length)];

        mBalls.add(b);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawColor(Color.parseColor("#F9FBFD"));

        if (mBalls.isEmpty()) {
            canvas.drawText("Tap inside this sandbox box to drop balls!", getWidth() / 2f, getHeight() / 2f, mTextPaint);
        } else {
            for (int i = 0; i < mBalls.size(); i++) {
                Ball b = mBalls.get(i);
                mBallPaint.setColor(b.color);
                canvas.drawCircle(b.x, b.y, b.radius, mBallPaint);
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