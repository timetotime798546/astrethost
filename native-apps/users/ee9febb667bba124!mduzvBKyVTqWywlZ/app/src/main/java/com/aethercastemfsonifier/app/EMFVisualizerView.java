package com.aethercastemfsonifier.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class EMFVisualizerView extends View {

    private Paint gridPaint;
    private Paint xPaint;
    private Paint yPaint;
    private Paint zPaint;
    private Paint magnitudePaint;
    private Paint textPaint;

    private float currentX = 0f;
    private float currentY = 0f;
    private float currentZ = 0f;
    private float currentMagnitude = 0f;

    private final List<Float> history = new ArrayList<>();
    private final int MAX_HISTORY_SIZE = 120;

    public EMFVisualizerView(Context context) {
        super(context);
        init();
    }

    public EMFVisualizerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        gridPaint = new Paint();
        gridPaint.setColor(0x1F00FFCC);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(1f);

        xPaint = new Paint();
        xPaint.setColor(0xCC00FFCC);
        xPaint.setStyle(Paint.Style.STROKE);
        xPaint.setStrokeWidth(3f);
        xPaint.setAntiAlias(true);

        yPaint = new Paint();
        yPaint.setColor(0xCC33B5E5);
        yPaint.setStyle(Paint.Style.STROKE);
        yPaint.setStrokeWidth(3f);
        yPaint.setAntiAlias(true);

        zPaint = new Paint();
        zPaint.setColor(0xCCAA66CC);
        zPaint.setStyle(Paint.Style.STROKE);
        zPaint.setStrokeWidth(3f);
        zPaint.setAntiAlias(true);

        magnitudePaint = new Paint();
        magnitudePaint.setColor(0xFFFF3B30);
        magnitudePaint.setStyle(Paint.Style.STROKE);
        magnitudePaint.setStrokeWidth(5f);
        magnitudePaint.setAntiAlias(true);

        textPaint = new Paint();
        textPaint.setColor(0x99FFFFFF);
        textPaint.setTextSize(24f);
        textPaint.setAntiAlias(true);
    }

    public void updateData(float x, float y, float z, float magnitude) {
        this.currentX = x;
        this.currentY = y;
        this.currentZ = z;
        this.currentMagnitude = magnitude;

        history.add(magnitude);
        if (history.size() > MAX_HISTORY_SIZE) {
            history.remove(0);
        }
        invalidate();
    }

    public void clearHistory() {
        history.clear();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();

        // 1. Draw grid lines
        int numLines = 8;
        for (int i = 1; i < numLines; i++) {
            float yPos = (float) height / numLines * i;
            canvas.drawLine(0, yPos, width, yPos, gridPaint);
            float xPos = (float) width / numLines * i;
            canvas.drawLine(xPos, 0, xPos, height, gridPaint);
        }

        // Draw Center Zero Reference line
        float midY = height / 2.0f;

        // 2. Plot vector historic waveforms
        if (history.size() > 1) {
            float stepX = (float) width / (MAX_HISTORY_SIZE - 1);
            float maxEMFExpected = 200f; // Scale reference 200 µT

            // Draw total magnitude scrolling chart
            for (int i = 0; i < history.size() - 1; i++) {
                float hVal1 = history.get(i);
                float hVal2 = history.get(i + 1);

                // Map amplitude to layout limits
                float yVal1 = height - ((hVal1 / maxEMFExpected) * height);
                float yVal2 = height - ((hVal2 / maxEMFExpected) * height);

                // clamp bounds
                yVal1 = Math.max(2, Math.min(height - 2, yVal1));
                yVal2 = Math.max(2, Math.min(height - 2, yVal2));

                canvas.drawLine(i * stepX, yVal1, (i + 1) * stepX, yVal2, magnitudePaint);
            }
        }

        // 3. Draw a 3D-like vector diagram in the right corner
        float circleCenterX = width - 120f;
        float circleCenterY = 100f;
        float maxVectorLen = 60f;

        // Vector circle background reference
        Paint circleBg = new Paint();
        circleBg.setColor(0x1F8898A6);
        circleBg.setStyle(Paint.Style.FILL);
        canvas.drawCircle(circleCenterX, circleCenterY, maxVectorLen, circleBg);

        Paint circleBorder = new Paint();
        circleBorder.setColor(0x3D8898A6);
        circleBorder.setStyle(Paint.Style.STROKE);
        circleBorder.setStrokeWidth(2f);
        canvas.drawCircle(circleCenterX, circleCenterY, maxVectorLen, circleBorder);

        // Normalize X, Y, Z vector components relative to ~100 µT maximum
        float normX = Math.max(-1f, Math.min(1f, currentX / 100f)) * maxVectorLen;
        float normY = Math.max(-1f, Math.min(1f, currentY / 100f)) * maxVectorLen;
        float normZ = Math.max(-1f, Math.min(1f, currentZ / 100f)) * maxVectorLen;

        // Draw axis vectors
        canvas.drawLine(circleCenterX, circleCenterY, circleCenterX + normX, circleCenterY, xPaint); // X-axis mapping
        canvas.drawLine(circleCenterX, circleCenterY, circleCenterX, circleCenterY - normY, yPaint); // Y-axis mapping
        // Represent Z-axis component by an offset oblique line (3D effect)
        canvas.drawLine(circleCenterX, circleCenterY, circleCenterX + (normZ * 0.7f), circleCenterY + (normZ * 0.7f), zPaint);

        // Vector labels
        canvas.drawText("X", circleCenterX + maxVectorLen + 10f, circleCenterY + 8f, textPaint);
        canvas.drawText("Y", circleCenterX - 10f, circleCenterY - maxVectorLen - 10f, textPaint);

        // Telemetry details in top left corner of visualizer
        canvas.drawText("Real-time Flux Oscillogram", 20f, 40f, textPaint);
    }
}