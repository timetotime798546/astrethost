package com.magiccompanion.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CauldronView extends View {

    private Paint paint;
    private List<Bubble> bubbles;
    private Random random;
    
    private int currentLiquidColor = Color.rgb(61, 23, 117);
    private int targetLiquidColor = Color.rgb(61, 23, 117);
    private float colorTransitionProgress = 1.0f;

    private boolean isBrewing = false;

    public static class Bubble {
        float x, y;
        float radius;
        float vy;
        float wobbleOffset;
        float wobbleSpeed;
        int color;
        float alpha;

        public Bubble(float x, float y, float radius, float vy, int color) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.vy = vy;
            this.color = color;
            this.wobbleOffset = (float) (Math.random() * 100.0);
            this.wobbleSpeed = 0.05f + (float) Math.random() * 0.1f;
            this.alpha = 1.0f;
        }

        public void update() {
            y += vy;
            wobbleOffset += wobbleSpeed;
            x += Math.sin(wobbleOffset) * 1.2f;
            alpha -= 0.008f;
        }
    }

    public CauldronView(Context context) {
        super(context);
        init();
    }

    public CauldronView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setAntiAlias(true);
        bubbles = new ArrayList<>();
        random = new Random();
    }

    public void startBrewing(int targetColor, long durationMs) {
        this.targetLiquidColor = targetColor;
        this.colorTransitionProgress = 0.0f;
        this.isBrewing = true;
        invalidate();
    }

    private int interpolateColor(int colorStart, int colorEnd, float progress) {
        int rS = Color.red(colorStart);
        int gS = Color.green(colorStart);
        int bS = Color.blue(colorStart);

        int rE = Color.red(colorEnd);
        int gE = Color.green(colorEnd);
        int bE = Color.blue(colorEnd);

        int r = (int) (rS + (rE - rS) * progress);
        int g = (int) (gS + (gE - gS) * progress);
        int b = (int) (bS + (bE - bS) * progress);

        return Color.rgb(r, g, b);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        float cauldronSize = Math.min(width, height) * 0.55f;
        float cx = width / 2f;
        float cy = height / 2f + 40f;

        if (colorTransitionProgress < 1.0f) {
            colorTransitionProgress += 0.02f;
            if (colorTransitionProgress >= 1.0f) {
                colorTransitionProgress = 1.0f;
                currentLiquidColor = targetLiquidColor;
                isBrewing = false;
            } else {
                currentLiquidColor = interpolateColor(currentLiquidColor, targetLiquidColor, colorTransitionProgress);
            }
        }

        float liquidSurfaceY = cy - cauldronSize * 0.2f;
        float liquidLeft = cx - cauldronSize * 0.45f;
        float liquidRight = cx + cauldronSize * 0.45f;
        float liquidWidth = liquidRight - liquidLeft;

        int spawnThreshold = isBrewing ? 5 : 95;
        if (random.nextInt(100) > spawnThreshold) {
            float bx = liquidLeft + random.nextFloat() * liquidWidth;
            float by = liquidSurfaceY + 10f;
            float br = 6f + random.nextFloat() * 14f;
            float bvy = -1.5f - random.nextFloat() * 2f;
            int bcol = interpolateColor(currentLiquidColor, Color.WHITE, 0.4f);
            bubbles.add(new Bubble(bx, by, br, bvy, bcol));
        }

        for (int i = bubbles.size() - 1; i >= 0; i--) {
            Bubble b = bubbles.get(i);
            b.update();
            if (b.alpha <= 0f || b.y < 0) {
                bubbles.remove(i);
            }
        }

        if (isBrewing) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(35, 230, 220, 245));
            for (int i = 0; i < 3; i++) {
                float sx = cx + (random.nextFloat() - 0.5f) * (cauldronSize * 0.8f);
                float sy = liquidSurfaceY - 40f - random.nextFloat() * 80f;
                float sr = 35f + random.nextFloat() * 30f;
                canvas.drawCircle(sx, sy, sr, paint);
            }
        }

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3f);
        for (Bubble b : bubbles) {
            paint.setColor(b.color);
            paint.setAlpha((int) (b.alpha * 220));
            canvas.drawCircle(b.x, b.y, b.radius, paint);
            
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.WHITE);
            paint.setAlpha((int) (b.alpha * 120));
            canvas.drawCircle(b.x - b.radius * 0.3f, b.y - b.radius * 0.3f, b.radius * 0.25f, paint);
            paint.setStyle(Paint.Style.STROKE);
        }

        paint.setStrokeWidth(0);
        
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(255, 90, 0));
        Path firePath = new Path();
        firePath.moveTo(cx - cauldronSize * 0.35f, cy + cauldronSize * 0.45f);
        firePath.lineTo(cx + cauldronSize * 0.35f, cy + cauldronSize * 0.45f);
        firePath.lineTo(cx + cauldronSize * 0.2f, cy + cauldronSize * 0.58f);
        firePath.lineTo(cx, cy + cauldronSize * 0.48f);
        firePath.lineTo(cx - cauldronSize * 0.2f, cy + cauldronSize * 0.58f);
        firePath.close();
        canvas.drawPath(firePath, paint);

        paint.setColor(Color.rgb(255, 170, 0));
        canvas.drawCircle(cx, cy + cauldronSize * 0.46f, cauldronSize * 0.1f, paint);
        canvas.drawCircle(cx - cauldronSize * 0.15f, cy + cauldronSize * 0.48f, cauldronSize * 0.08f, paint);
        canvas.drawCircle(cx + cauldronSize * 0.15f, cy + cauldronSize * 0.48f, cauldronSize * 0.08f, paint);

        paint.setColor(Color.rgb(22, 20, 26));
        canvas.drawRect(cx - cauldronSize * 0.35f, cy + cauldronSize * 0.3f, cx - cauldronSize * 0.25f, cy + cauldronSize * 0.52f, paint);
        canvas.drawRect(cx + cauldronSize * 0.25f, cy + cauldronSize * 0.3f, cx + cauldronSize * 0.35f, cy + cauldronSize * 0.52f, paint);

        paint.setColor(Color.rgb(36, 33, 43));
        canvas.drawCircle(cx, cy + cauldronSize * 0.1f, cauldronSize * 0.45f, paint);

        paint.setColor(Color.rgb(46, 43, 56));
        canvas.drawCircle(cx - cauldronSize * 0.04f, cy + cauldronSize * 0.06f, cauldronSize * 0.42f, paint);

        paint.setColor(currentLiquidColor);
        paint.setStyle(Paint.Style.FILL);
        RectF liquidOval = new RectF(cx - cauldronSize * 0.44f, liquidSurfaceY - 20f, cx + cauldronSize * 0.44f, liquidSurfaceY + 20f);
        canvas.drawOval(liquidOval, paint);

        paint.setColor(Color.WHITE);
        paint.setAlpha(40);
        RectF liquidHighlight = new RectF(cx - cauldronSize * 0.38f, liquidSurfaceY - 14f, cx + cauldronSize * 0.38f, liquidSurfaceY + 14f);
        canvas.drawOval(liquidHighlight, paint);
        paint.setAlpha(255);

        paint.setColor(Color.rgb(22, 20, 26));
        RectF lipOval = new RectF(cx - cauldronSize * 0.47f, liquidSurfaceY - 12f, cx + cauldronSize * 0.47f, liquidSurfaceY + 12f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(14f);
        canvas.drawOval(lipOval, paint);

        paint.setStrokeWidth(8f);
        RectF handleLeft = new RectF(cx - cauldronSize * 0.56f, cy - cauldronSize * 0.1f, cx - cauldronSize * 0.42f, cy + cauldronSize * 0.1f);
        canvas.drawArc(handleLeft, 90, 180, false, paint);
        RectF handleRight = new RectF(cx + cauldronSize * 0.42f, cy - cauldronSize * 0.1f, cx + cauldronSize * 0.56f, cy + cauldronSize * 0.1f);
        canvas.drawArc(handleRight, 270, 180, false, paint);

        postInvalidateOnAnimation();
    }
}