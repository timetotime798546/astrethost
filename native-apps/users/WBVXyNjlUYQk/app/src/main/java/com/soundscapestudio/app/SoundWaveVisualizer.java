package com.soundscapestudio.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

public class SoundWaveVisualizer extends View {
    private float phase = 0f;
    private float carrierFreq = 250f;
    private float beatFreq = 15f;
    private Paint paintLeft;
    private Paint paintRight;
    private Paint paintGrid;
    private boolean isPlaying = false;

    public SoundWaveVisualizer(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paintLeft = new Paint();
        paintLeft.setColor(0xFF00E676); // Neon Green
        paintLeft.setStrokeWidth(4f);
        paintLeft.setStyle(Paint.Style.STROKE);
        paintLeft.setAntiAlias(true);

        paintRight = new Paint();
        paintRight.setColor(0xFF00B0FF); // Neon Blue
        paintRight.setStrokeWidth(4f);
        paintRight.setStyle(Paint.Style.STROKE);
        paintRight.setAntiAlias(true);

        paintGrid = new Paint();
        paintGrid.setColor(0xFF1E1E2F);
        paintGrid.setStrokeWidth(2f);
        paintGrid.setStyle(Paint.Style.STROKE);
    }

    public void setPlaying(boolean playing) {
        this.isPlaying = playing;
        invalidate();
    }

    public void setFrequencies(float carrier, float beat) {
        this.carrierFreq = carrier;
        this.beatFreq = beat;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        int midY = h / 2;

        // Draw background grid lines
        for (int i = 0; i < w; i += w / 8) {
            canvas.drawLine(i, 0, i, h, paintGrid);
        }
        canvas.drawLine(0, midY, w, midY, paintGrid);

        if (isPlaying) {
            phase += 0.08f;
        }

        Path pathLeft = new Path();
        Path pathRight = new Path();

        pathLeft.moveTo(0, midY);
        pathRight.moveTo(0, midY);

        // Derive dynamic visual wavelength from frequency selection
        float speedScaleL = (carrierFreq - beatFreq / 2f) * 0.03f;
        float speedScaleR = (carrierFreq + beatFreq / 2f) * 0.03f;

        // Clamp to visually appealing boundaries on screen
        if (speedScaleL < 4f) speedScaleL = 4f;
        if (speedScaleL > 22f) speedScaleL = 22f;
        if (speedScaleR < 4f) speedScaleR = 4f;
        if (speedScaleR > 22f) speedScaleR = 22f;

        float maxAmplitude = midY * 0.45f;
        // If not playing, render static waves flatly
        float activeAmp = isPlaying ? maxAmplitude : maxAmplitude * 0.15f;

        for (int x = 0; x < w; x += 4) {
            // Left wave (Neon Green)
            float yL = midY + (float) Math.sin(x * 0.015f * speedScaleL + phase) * activeAmp;
            pathLeft.lineTo(x, yL);

            // Right wave (Neon Blue)
            float yR = midY + (float) Math.sin(x * 0.015f * speedScaleR - phase) * activeAmp;
            pathRight.lineTo(x, yR);
        }

        canvas.drawPath(pathLeft, paintLeft);
        canvas.drawPath(pathRight, paintRight);

        if (isPlaying) {
            postInvalidateOnAnimation();
        }
    }
}