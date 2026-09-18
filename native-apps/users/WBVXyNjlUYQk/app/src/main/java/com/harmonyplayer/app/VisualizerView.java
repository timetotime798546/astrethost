package com.harmonyplayer.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import java.util.Random;

public class VisualizerView extends View {

    private Paint paint;
    private int numBars = 16;
    private float[] barHeights;
    private float[] targetHeights;
    private boolean isPlaying = false;
    private Random random = new Random();
    private LinearGradient gradient;

    public VisualizerView(Context context) {
        super(context);
        init();
    }

    public VisualizerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setAntiAlias(true);
        barHeights = new float[numBars];
        targetHeights = new float[numBars];
        for (int i = 0; i < numBars; i++) {
            barHeights[i] = 10f;
            targetHeights[i] = 10f;
        }
    }

    public void setPlaying(boolean playing) {
        this.isPlaying = playing;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        gradient = new LinearGradient(0, h, 0, 0,
                0xFF3B82F6, // Electric Blue
                0xFF8B5CF6, // Neon Purple
                Shader.TileMode.CLAMP);
        paint.setShader(gradient);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        if (width == 0 || height == 0) return;

        float barWidth = (float) width / (numBars * 1.5f);
        float spacing = barWidth * 0.5f;

        for (int i = 0; i < numBars; i++) {
            if (isPlaying) {
                // Generate rhythmically bouncing target heights
                if (random.nextInt(4) == 0) {
                    targetHeights[i] = random.nextFloat() * (height * 0.85f) + (height * 0.15f);
                }
            } else {
                // Smooth fade to base heights when paused
                targetHeights[i] = 8f;
            }

            // Smooth interpolation
            barHeights[i] += (targetHeights[i] - barHeights[i]) * 0.15f;

            float left = i * (barWidth + spacing) + spacing / 2;
            float top = height - barHeights[i];
            float right = left + barWidth;
            float bottom = height;

            // Draw beautiful rounded bar
            canvas.drawRoundRect(left, top, right, bottom, barWidth / 2, barWidth / 2, paint);
        }

        if (isPlaying) {
            postInvalidateDelayed(35); // Keep animating at ~30 FPS
        } else {
            // Check if still settling
            boolean settling = false;
            for (int i = 0; i < numBars; i++) {
                if (barHeights[i] > 10f) {
                    settling = true;
                    break;
                }
            }
            if (settling) {
                postInvalidateDelayed(35);
            }
        }
    }
}