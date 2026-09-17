package com.focustimer.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class CircularProgressView extends View {
    private Paint backgroundPaint;
    private Paint progressPaint;
    private RectF rectF;
    private float maxProgress = 100f;
    private float currentProgress = 100f;
    private float strokeWidth = 20f;

    public CircularProgressView(Context context) {
        super(context);
        init();
    }

    public CircularProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CircularProgressView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        backgroundPaint = new Paint();
        backgroundPaint.setColor(0xFFEEEEEE); // Neutral off-white grey
        backgroundPaint.setStyle(Paint.Style.STROKE);
        backgroundPaint.setStrokeWidth(strokeWidth);
        backgroundPaint.setAntiAlias(true);

        progressPaint = new Paint();
        progressPaint.setColor(0xFFE53935); // Gorgeous Focus Red Primary Accent
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(strokeWidth);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);
        progressPaint.setAntiAlias(true);

        rectF = new RectF();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        float width = getWidth();
        float height = getHeight();
        float size = Math.min(width, height) - strokeWidth;
        
        float left = (width - size) / 2;
        float top = (height - size) / 2;
        rectF.set(left, top, left + size, top + size);
        
        // Draw background track
        canvas.drawOval(rectF, backgroundPaint);
        
        // Draw progress path arc
        if (maxProgress > 0) {
            float sweepAngle = (currentProgress / maxProgress) * 360f;
            canvas.drawArc(rectF, -90, sweepAngle, false, progressPaint);
        }
    }

    public void setMaxProgress(float maxProgress) {
        this.maxProgress = maxProgress;
        invalidate();
    }

    public void setProgress(float progress) {
        this.currentProgress = progress;
        invalidate();
    }
}