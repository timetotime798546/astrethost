package com.zenhabit.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class CustomTimerVisualizerView extends View {
    private Paint paintCircle;
    private Paint paintRing;
    private float animationScale = 1.0f;
    private boolean growing = true;
    private boolean timerRunning = false;
    private String timerText = "25:00";

    public CustomTimerVisualizerView(Context context) {
        super(context);
        init();
    }

    public CustomTimerVisualizerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paintCircle = new Paint();
        paintCircle.setAntiAlias(true);
        paintCircle.setColor(Color.parseColor("#E0F2F1"));
        paintCircle.setStyle(Paint.Style.FILL);

        paintRing = new Paint();
        paintRing.setAntiAlias(true);
        paintRing.setColor(Color.parseColor("#009688"));
        paintRing.setStyle(Paint.Style.STROKE);
        paintRing.setStrokeWidth(12f);
    }

    public void setTimerRunning(boolean running) {
        this.timerRunning = running;
        invalidate();
    }

    public void setTimerText(String text) {
        this.timerText = text;
        invalidate();
    }

    public void updateAnimation() {
        if (!timerRunning) return;
        
        if (growing) {
            animationScale += 0.012f;
            if (animationScale >= 1.25f) growing = false;
        } else {
            animationScale -= 0.012f;
            if (animationScale <= 0.92f) growing = true;
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int cx = getWidth() / 2;
        int cy = getHeight() / 2;
        float baseRadius = Math.min(cx, cy) * 0.5f;

        // Visual shadow effect represents breath depth
        if (timerRunning) {
            paintCircle.setColor(Color.parseColor("#B2DFDB"));
            canvas.drawCircle(cx, cy, baseRadius * animationScale, paintCircle);
        }

        // Inner solid focus sphere
        paintCircle.setColor(Color.parseColor("#80CBC4"));
        canvas.drawCircle(cx, cy, baseRadius, paintCircle);

        // Core outer ring
        canvas.drawCircle(cx, cy, baseRadius * 1.1f, paintRing);
    }
}