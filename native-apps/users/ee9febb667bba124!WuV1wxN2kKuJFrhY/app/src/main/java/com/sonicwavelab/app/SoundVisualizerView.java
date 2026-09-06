package com.sonicwavelab.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class SoundVisualizerView extends View {

    private Paint backgroundPaint;
    private Paint gridPaint;
    private Paint wavePaint;
    private Paint ripplePaint;

    private float playProgress = 0f;
    private boolean isPlaying = false;
    private float amplitude = 0f;
    private float frequency = 5f;
    private int currentPresetColor = 0xFF00E676;

    private float rippleRadius = 0f;

    public SoundVisualizerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.parseColor("#121C24"));
        backgroundPaint.setStyle(Paint.Style.FILL);

        gridPaint = new Paint();
        gridPaint.setColor(Color.parseColor("#1D2B36"));
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(1.5f);

        wavePaint = new Paint();
        wavePaint.setAntiAlias(true);
        wavePaint.setStyle(Paint.Style.STROKE);
        wavePaint.setStrokeWidth(4f);

        ripplePaint = new Paint();
        ripplePaint.setAntiAlias(true);
        ripplePaint.setStyle(Paint.Style.STROKE);
        ripplePaint.setStrokeWidth(3f);
    }

    public void triggerWave(int color, float maxAmp, float freq) {
        this.currentPresetColor = color;
        this.amplitude = maxAmp;
        this.frequency = freq;
        this.isPlaying = true;
        this.playProgress = 0f;
        this.rippleRadius = 10f;
        invalidate();
    }

    public void stopWave() {
        this.isPlaying = false;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();

        // Draw Background
        canvas.drawRect(0, 0, width, height, backgroundPaint);

        // Draw Grid Lines
        for (int i = 0; i < width; i += 40) {
            canvas.drawLine(i, 0, i, height, gridPaint);
        }
        for (int j = 0; j < height; j += 40) {
            canvas.drawLine(0, j, width, j, gridPaint);
        }

        float centerY = height / 2.0f;

        // Draw Dynamic Ripple rings from center
        if (isPlaying) {
            ripplePaint.setColor(currentPresetColor);
            ripplePaint.setAlpha((int) (Math.max(0, (1.0f - (rippleRadius / (width / 1.5f))) * 255)));
            canvas.drawCircle(width / 2.0f, centerY, rippleRadius, ripplePaint);
            canvas.drawCircle(width / 2.0f, centerY, rippleRadius * 0.6f, ripplePaint);
            rippleRadius += 4.5f;
            if (rippleRadius > width) {
                rippleRadius = 10f;
            }
        }

        // Draw Waves
        wavePaint.setColor(currentPresetColor);
        float lastX = 0;
        float lastY = centerY;

        int pointsCount = width;
        for (int x = 0; x < pointsCount; x += 3) {
            float t = (float) x / pointsCount;
            float ampModifier = (float) Math.sin(t * Math.PI) * amplitude; // Windowing fade ends

            float sineTerm;
            if (isPlaying) {
                sineTerm = (float) Math.sin(2 * Math.PI * frequency * (t - playProgress));
            } else {
                // Static idle wave
                sineTerm = (float) Math.sin(2 * Math.PI * 2 * t);
                ampModifier = 6f;
            }

            float y = centerY + (sineTerm * ampModifier);
            if (x > 0) {
                canvas.drawLine(lastX, lastY, x, y, wavePaint);
            }
            lastX = x;
            lastY = y;
        }

        if (isPlaying) {
            playProgress += 0.015f;
            amplitude *= 0.985f; // decay
            if (amplitude < 1.0f) {
                isPlaying = false;
            }
            postInvalidateDelayed(16); // ~60fps animations
        }
    }
}