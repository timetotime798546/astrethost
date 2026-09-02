package com.smartnotes.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class BeautifulLoaderView extends View {
    private Paint paint;
    private RectF rectF;
    private float pullProgress = 0.0f;
    private boolean isRefreshing = false;
    private float rotationAngle = 0f;
    private float pulseScale = 1.0f;
    private boolean pulseGrowing = true;

    private Runnable animator = new Runnable() {
        @Override
        public void run() {
            if (isRefreshing) {
                rotationAngle = (rotationAngle + 8f) % 360f;
                if (pulseGrowing) {
                    pulseScale += 0.015f;
                    if (pulseScale >= 1.15f) {
                        pulseGrowing = false;
                    }
                } else {
                    pulseScale -= 0.015f;
                    if (pulseScale <= 0.85f) {
                        pulseGrowing = true;
                    }
                }
                invalidate();
                postDelayed(this, 16);
            }
        }
    };

    public BeautifulLoaderView(Context context) {
        super(context);
        init();
    }

    public BeautifulLoaderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        rectF = new RectF();
    }

    public void setPullProgress(float progress) {
        this.pullProgress = progress;
        this.rotationAngle = progress * 180f;
        invalidate();
    }

    public void setRefreshing(boolean refreshing) {
        if (this.isRefreshing == refreshing) return;
        this.isRefreshing = refreshing;
        if (refreshing) {
            removeCallbacks(animator);
            post(animator);
        } else {
            removeCallbacks(animator);
            pullProgress = 0.0f;
            rotationAngle = 0f;
            pulseScale = 1.0f;
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        if (width == 0 || height == 0) return;

        float cx = width / 2f;
        float cy = height / 2f;
        float size = Math.min(width, height) * 0.6f;

        canvas.save();
        if (isRefreshing) {
            canvas.scale(pulseScale, pulseScale, cx, cy);
        }
        canvas.rotate(rotationAngle, cx, cy);

        rectF.set(cx - size / 2f, cy - size / 2f, cx + size / 2f, cy + size / 2f);

        // Background Track Ring
        paint.setColor(0x223F51B5);
        paint.setStrokeWidth(8f);
        canvas.drawCircle(cx, cy, size / 2f, paint);

        // Active Progress Arc
        paint.setStrokeWidth(10f);
        if (isRefreshing) {
            paint.setColor(0xFFE91E63);
            canvas.drawArc(rectF, 0, 90, false, paint);
            paint.setColor(0xFF3F51B5);
            canvas.drawArc(rectF, 180, 90, false, paint);
        } else {
            paint.setColor(0xFF3F51B5);
            float sweepAngle = pullProgress * 300f;
            canvas.drawArc(rectF, -90, sweepAngle, false, paint);
        }
        canvas.restore();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(animator);
    }
}