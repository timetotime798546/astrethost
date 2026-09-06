package com.acresizer.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import java.util.Random;

public class ParticleBackgroundView extends View {
    private Paint paint;
    private float[] px, py, pSpeedX, pSpeedY, pRadius;
    private int[] pAlpha;
    private int particleCount = 22;
    private Random random = new Random();
    private boolean initialized = false;

    public ParticleBackgroundView(Context context) {
        super(context);
        init();
    }

    public ParticleBackgroundView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setAntiAlias(true);
        px = new float[particleCount];
        py = new float[particleCount];
        pSpeedX = new float[particleCount];
        pSpeedY = new float[particleCount];
        pRadius = new float[particleCount];
        pAlpha = new int[particleCount];
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        if (!initialized) {
            for (int i = 0; i < particleCount; i++) {
                px[i] = random.nextFloat() * w;
                py[i] = random.nextFloat() * h;
                pSpeedX[i] = (random.nextFloat() * 1.2f - 0.6f);
                pSpeedY[i] = (random.nextFloat() * 1.2f - 0.6f);
                pRadius[i] = random.nextFloat() * 80 + 30;
                pAlpha[i] = random.nextInt(35) + 10;
            }
            initialized = true;
        }

        // Render ambient glowing 3D back-spheres
        for (int i = 0; i < particleCount; i++) {
            px[i] += pSpeedX[i];
            py[i] += pSpeedY[i];

            if (px[i] < -100) px[i] = w + 100;
            if (px[i] > w + 100) px[i] = -100;
            if (py[i] < -100) py[i] = h + 100;
            if (py[i] > h + 100) py[i] = -100;

            // Gradient-like premium aesthetic
            if (i % 3 == 0) {
                paint.setColor(0xFF3B82F6); // Soft Blue Glowing node
            } else if (i % 3 == 1) {
                paint.setColor(0xFF8B5CF6); // Soft Purple Ambient node
            } else {
                paint.setColor(0xFF10B981); // Soft Emerald Green node
            }
            
            paint.setAlpha(pAlpha[i]);
            canvas.drawCircle(px[i], py[i], pRadius[i], paint);
        }

        // Invalidate continuously to handle high-performance animation updates
        postInvalidateOnAnimation();
    }
}