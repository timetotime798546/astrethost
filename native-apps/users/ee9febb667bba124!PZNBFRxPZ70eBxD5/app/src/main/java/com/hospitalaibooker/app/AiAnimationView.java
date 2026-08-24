package com.hospitalaibooker.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class AiAnimationView extends View {

    public static final int STATE_IDLE = 0;
    public static final int STATE_LISTENING = 1;
    public static final int STATE_THINKING = 2;
    public static final int STATE_SPEAKING = 3;

    private int currentState = STATE_IDLE;
    private Paint paint;
    private float animationProgress = 0f;
    private boolean isAnimating = true;

    public AiAnimationView(Context context) {
        super(context);
        init();
    }

    public AiAnimationView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStrokeCap(Paint.Cap.ROUND);
    }

    public void setUiState(int state) {
        this.currentState = state;
        this.animationProgress = 0f;
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        int cx = width / 2;
        int cy = height / 2;

        animationProgress += 0.05f;
        if (animationProgress > (float) (2 * Math.PI)) {
            animationProgress = 0f;
        }

        switch (currentState) {
            case STATE_IDLE:
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(0xFF008080);
                float idleRadius = 12f + (float) Math.sin(animationProgress) * 3f;
                canvas.drawCircle(cx, cy, idleRadius, paint);
                break;

            case STATE_LISTENING:
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(4f);
                for (int i = 0; i < 3; i++) {
                    float offset = (animationProgress * 15f + i * 18f) % 50f;
                    int alpha = (int) (255 * (1f - (offset / 50f)));
                    paint.setColor((0xFF00B0FF & 0x00FFFFFF) | (alpha << 24));
                    canvas.drawCircle(cx, cy, 10f + offset, paint);
                }
                break;

            case STATE_THINKING:
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(0xFFFFB300);
                float orbitRadius = 25f;
                for (int i = 0; i < 3; i++) {
                    double angle = animationProgress + (i * Math.PI * 2 / 3);
                    float dotX = cx + (float) Math.cos(angle) * orbitRadius;
                    float dotY = cy + (float) Math.sin(angle) * orbitRadius;
                    canvas.drawCircle(dotX, dotY, 6f, paint);
                }
                break;

            case STATE_SPEAKING:
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(0xFF00E676);
                int barCount = 7;
                float barWidth = 8f;
                float spacing = 12f;
                float startX = cx - ((barCount / 2f) * (barWidth + spacing));
                
                for (int i = 0; i < barCount; i++) {
                    float x = startX + i * (barWidth + spacing);
                    double offsetAngle = animationProgress * 2.0 + (i * 0.5);
                    float barHeight = 12f + (float) Math.abs(Math.sin(offsetAngle)) * 24f;
                    canvas.drawRect(
                        x,
                        cy - barHeight / 2f,
                        x + barWidth,
                        cy + barHeight / 2f,
                        paint
                    );
                }
                break;
        }

        if (isAnimating) {
            postInvalidateDelayed(32);
        }
    }
}