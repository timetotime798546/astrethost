package com.voiceassistantai.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class WaveformView extends View {
    private Paint paint;
    private float amplitude = 0f;
    private float[] phaseOffsets = new float[]{0.0f, 0.4f, 0.8f};

    public WaveformView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(0xFF00E676); // Beautiful light visualizer green
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4.5f);
        paint.setAntiAlias(true);
    }

    public void setAmplitude(float rmsdB) {
        // RMSdB range normalization from approximate scale -2 to 10
        float normAmp = (rmsdB + 2.0f) / 12.0f;
        if (normAmp < 0) normAmp = 0;
        if (normAmp > 1) normAmp = 1;
        this.amplitude = normAmp;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        int middleY = height / 2;

        for (int i = 0; i < phaseOffsets.length; i++) {
            phaseOffsets[i] += 0.08f; // Animating wave dynamics incrementally
            paint.setAlpha(150 - (i * 45));
            paint.setStrokeWidth(5.0f - i);

            float lastX = 0;
            float lastY = middleY;

            for (float x = 0; x < width; x += 8) {
                float scalingValue = (float) Math.sin((x / (float) width * 2 * Math.PI * 1.6) + phaseOffsets[i]);
                float y = middleY + scalingValue * amplitude * (height / 2.0f - 10);
                if (x > 0) {
                    canvas.drawLine(lastX, lastY, x, y, paint);
                }
                lastX = x;
                lastY = y;
            }
        }

        if (amplitude > 0.02f) {
            postInvalidateDelayed(35);
        }
    }
}