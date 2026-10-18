package com.fmradio.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;

import java.util.Random;

public class VisualizerView extends View {
    private static final int BAR_COUNT = 24;
    private final float[] barHeights = new float[BAR_COUNT];
    private final float[] targetHeights = new float[BAR_COUNT];
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint peakPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random = new Random();
    private boolean isPlaying = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable animateRunnable = new Runnable() {
        @Override
        public void run() {
            if (isPlaying) {
                for (int i = 0; i < BAR_COUNT; i++) {
                    targetHeights[i] = 0.15f + (random.nextFloat() * 0.80f);
                    barHeights[i] += (targetHeights[i] - barHeights[i]) * 0.35f;
                }
            } else {
                for (int i = 0; i < BAR_COUNT; i++) {
                    barHeights[i] = Math.max(0.04f, barHeights[i] * 0.85f);
                }
            }
            invalidate();
            handler.postDelayed(this, 60);
        }
    };

    public VisualizerView(Context context) {
        super(context);
        init();
    }

    public VisualizerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public VisualizerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        barPaint.setColor(Color.parseColor("#00E676"));
        barPaint.setStyle(Paint.Style.FILL);

        peakPaint.setColor(Color.parseColor("#FFD600"));
        peakPaint.setStyle(Paint.Style.FILL);

        for (int i = 0; i < BAR_COUNT; i++) {
            barHeights[i] = 0.05f;
            targetHeights[i] = 0.05f;
        }
        handler.post(animateRunnable);
    }

    public void setPlaying(boolean playing) {
        this.isPlaying = playing;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();

        if (width <= 0 || height <= 0) return;

        float totalBarWidth = (float) width / BAR_COUNT;
        float barWidth = totalBarWidth * 0.65f;
        float spacing = totalBarWidth * 0.35f;

        for (int i = 0; i < BAR_COUNT; i++) {
            float left = (i * totalBarWidth) + (spacing / 2.0f);
            float right = left + barWidth;
            float barH = barHeights[i] * height;
            float top = height - barH;
            float bottom = height;

            // Draw primary equalizer bar
            canvas.drawRect(left, top, right, bottom, barPaint);

            // Draw peak indicator pip at the top
            float peakTop = Math.max(0, top - 4f);
            canvas.drawRect(left, peakTop, right, peakTop + 3f, peakPaint);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        handler.removeCallbacks(animateRunnable);
    }
}