package com.ultimatestopwatch.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class StopwatchProgressView extends View {
    private Paint backgroundPaint;
    private Paint progressPaint;
    private RectF rectF;
    private float progress = 0.0f; // range: 0.0f - 1.0f (representing 0 - 60 seconds loop)

    public StopwatchProgressView(Context context) {
        super(context);
        init();
    }

    public StopwatchProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public StopwatchProgressView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        backgroundPaint = new Paint();
        backgroundPaint.setColor(0xFF2C2C2C); // Dark path ring
        backgroundPaint.setStyle(Paint.Style.STROKE);
        backgroundPaint.setStrokeWidth(14f);
        backgroundPaint.setAntiAlias(true);

        progressPaint = new Paint();
        progressPaint.setColor(0xFF00E676); // Holo green accent
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(14f);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);
        progressPaint.setAntiAlias(true);

        rectF = new RectF();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        int size = Math.min(width, height);
        float padding = 18f;

        rectF.set(padding, padding, size - padding, size - padding);

        // Draw background sweep circle
        canvas.drawOval(rectF, backgroundPaint);

        // Draw radial progress sweep starting from 12 o'clock (-90 degrees)
        float sweepAngle = progress * 360f;
        canvas.drawArc(rectF, -90, sweepAngle, false, progressPaint);
    }

    public void setProgress(float progress) {
        this.progress = progress;
        invalidate();
    }

    public void setProgressColor(int color) {
        progressPaint.setColor(color);
        invalidate();
    }
}