package com.magiccompanion.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import java.util.Random;

public class EightBallView extends View {

    private Paint paint;
    private Random random;

    private boolean isShaking = false;
    private long shakeStartTime = 0;
    private float shakeOffsetMax = 0f;

    private String activePrediction = "Ask and tap to reveal";
    private float predictionAlpha = 1.0f;
    private int currentMistHue = 270;

    public EightBallView(Context context) {
        super(context);
        init();
    }

    public EightBallView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setAntiAlias(true);
        random = new Random();
    }

    public void triggerPrediction(String message) {
        this.activePrediction = message;
        this.isShaking = true;
        this.shakeStartTime = System.currentTimeMillis();
        this.shakeOffsetMax = 30.0f;
        this.predictionAlpha = 0.0f;
        this.currentMistHue = random.nextInt(360);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        float size = Math.min(width, height) * 0.65f;
        float cx = width / 2f;
        float cy = height / 2.3f;

        if (isShaking) {
            long elapsed = System.currentTimeMillis() - shakeStartTime;
            if (elapsed > 1200) {
                isShaking = false;
                shakeOffsetMax = 0f;
            } else {
                float decay = 1.0f - (elapsed / 1200f);
                float dx = (random.nextFloat() - 0.5f) * shakeOffsetMax * decay;
                float dy = (random.nextFloat() - 0.5f) * shakeOffsetMax * decay;
                canvas.translate(dx, dy);

                if (elapsed > 600) {
                    predictionAlpha = (elapsed - 600f) / 600f;
                }
            }
        }

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(194, 153, 50));
        Path pedestal = new Path();
        pedestal.moveTo(cx - size * 0.35f, cy + size * 0.62f);
        pedestal.lineTo(cx + size * 0.35f, cy + size * 0.62f);
        pedestal.lineTo(cx + size * 0.25f, cy + size * 0.42f);
        pedestal.lineTo(cx - size * 0.25f, cy + size * 0.42f);
        pedestal.close();
        canvas.drawPath(pedestal, paint);

        paint.setColor(Color.rgb(235, 197, 84));
        RectF baseOval = new RectF(cx - size * 0.35f, cy + size * 0.58f, cx + size * 0.35f, cy + size * 0.64f);
        canvas.drawOval(baseOval, paint);

        RectF ringOval = new RectF(cx - size * 0.25f, cy + size * 0.4f, cx + size * 0.25f, cy + size * 0.46f);
        paint.setColor(Color.rgb(148, 114, 28));
        canvas.drawOval(ringOval, paint);

        int centerColor = getMistColor(160);
        int edgeColor = Color.rgb(10, 5, 24);
        
        RadialGradient radGrad = new RadialGradient(cx, cy, size * 0.5f, centerColor, edgeColor, Shader.TileMode.CLAMP);
        paint.setShader(radGrad);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(cx, cy, size * 0.5f, paint);
        paint.setShader(null);

        paint.setColor(getMistColor(60));
        for (int i = 0; i < 4; i++) {
            float mx = cx + (float) Math.sin(System.currentTimeMillis() * 0.002 + i) * (size * 0.15f);
            float my = cy + (float) Math.cos(System.currentTimeMillis() * 0.0015 - i) * (size * 0.12f);
            canvas.drawCircle(mx, my, size * 0.15f + (i * 10f), paint);
        }

        float triSize = size * 0.26f;
        float triCy = cy - size * 0.02f;
        int triAlpha = (int) (predictionAlpha * 240);
        
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(18, 0, 41));
        paint.setAlpha(triAlpha);

        Path triPath = new Path();
        triPath.moveTo(cx, triCy - triSize * 0.7f);
        triPath.lineTo(cx + triSize * 0.8f, triCy + triSize * 0.5f);
        triPath.lineTo(cx - triSize * 0.8f, triCy + triSize * 0.5f);
        triPath.close();
        canvas.drawPath(triPath, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4f);
        paint.setColor(Color.rgb(0, 242, 255));
        paint.setAlpha(triAlpha);
        canvas.drawPath(triPath, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setAlpha((int) (predictionAlpha * 255));
        paint.setTextSize(size * 0.042f);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setFakeBoldText(true);

        String[] words = activePrediction.split(" ");
        StringBuilder line1 = new StringBuilder();
        StringBuilder line2 = new StringBuilder();
        StringBuilder line3 = new StringBuilder();

        for (int i = 0; i < words.length; i++) {
            if (i < 2) {
                line1.append(words[i]).append(" ");
            } else if (i < 5) {
                line2.append(words[i]).append(" ");
            } else {
                line3.append(words[i]).append(" ");
            }
        }

        float textY = triCy - size * 0.04f;
        if (line1.length() > 0) {
            canvas.drawText(line1.toString().trim(), cx, textY, paint);
        }
        if (line2.length() > 0) {
            canvas.drawText(line2.toString().trim(), cx, textY + size * 0.06f, paint);
        }
        if (line3.length() > 0) {
            canvas.drawText(line3.toString().trim(), cx, textY + size * 0.12f, paint);
        }

        paint.setAlpha(255);
        paint.setFakeBoldText(false);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setAlpha(35);
        RectF glassShine = new RectF(cx - size * 0.35f, cy - size * 0.38f, cx - size * 0.1f, cy - size * 0.22f);
        canvas.drawOval(glassShine, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5f);
        paint.setColor(Color.rgb(200, 230, 255));
        paint.setAlpha(60);
        canvas.drawCircle(cx, cy, size * 0.5f, paint);

        if (isShaking || predictionAlpha < 1.0f) {
            postInvalidateOnAnimation();
        }
    }

    private int getMistColor(int alpha) {
        float[] hsv = new float[] { currentMistHue, 0.8f, 0.8f };
        int rgb = Color.HSVToColor(hsv);
        return Color.argb(alpha, Color.red(rgb), Color.green(rgb), Color.blue(rgb));
    }
}