package com.threedcalculator.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import java.util.Random;

public class ThreeDLoaderView extends View {

    private Paint linePaint;
    private Paint gridPaint;
    private Paint particlePaint;
    private float angleX = 0;
    private float angleY = 0;
    private float angleZ = 0;
    
    private float pulseFactor = 1.0f;
    private long startTime;
    private Random random = new Random();
    
    private static final int PARTICLE_COUNT = 25;
    private float[] particleX = new float[PARTICLE_COUNT];
    private float[] particleY = new float[PARTICLE_COUNT];
    private float[] particleZ = new float[PARTICLE_COUNT];
    private float[] particleSpeed = new float[PARTICLE_COUNT];

    private final float[][] vertices = {
        {-1, -1, -1}, {1, -1, -1}, {1, 1, -1}, {-1, 1, -1},
        {-1, -1, 1},  {1, -1, 1},  {1, 1, 1},  {-1, 1, 1}
    };
    
    private final int[][] edges = {
        {0,1}, {1,2}, {2,3}, {3,0},
        {4,5}, {5,6}, {6,7}, {7,4},
        {0,4}, {1,5}, {2,6}, {3,7}
    };

    public ThreeDLoaderView(Context context) { 
        super(context);
        init();
    }

    public ThreeDLoaderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        startTime = System.currentTimeMillis();
        
        linePaint = new Paint();
        linePaint.setColor(0xFF00FFCC);
        linePaint.setStrokeWidth(6.5f);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setAntiAlias(true);
        linePaint.setShadowLayer(20, 0, 0, 0xFF00FFCC);

        gridPaint = new Paint();
        gridPaint.setColor(0x3500FFCC);
        gridPaint.setStrokeWidth(3.0f);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setAntiAlias(true);

        particlePaint = new Paint();
        particlePaint.setColor(0x8800FFCC);
        particlePaint.setAntiAlias(true);
        particlePaint.setStyle(Paint.Style.FILL);
        
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            particleX[i] = (random.nextFloat() - 0.5f) * 450f;
            particleY[i] = (random.nextFloat() - 0.5f) * 450f;
            particleZ[i] = random.nextFloat() * 3.0f + 0.8f;
            particleSpeed[i] = random.nextFloat() * 1.8f + 0.8f;
        }

        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        int w = getWidth();
        int h = getHeight();
        long elapsed = System.currentTimeMillis() - startTime;
        
        // Retro Futuristic Sky and Grid Depth drawing with neon linear gradients
        drawRetroGrid(canvas, w, h, elapsed);
        
        // Moving particle dusts
        drawParticles(canvas, w, h);
        
        // Spinning animation angles
        angleX += 0.024f;
        angleY += 0.035f;
        angleZ += 0.012f;
        
        pulseFactor = 1.0f + 0.15f * (float) Math.sin(elapsed * 0.0055);
        
        boolean isGlitchActive = (elapsed % 1800 < 90);
        if (isGlitchActive) {
            linePaint.setColor(0xFFFF0088); 
            linePaint.setShadowLayer(30, 0, 0, 0xFFFF0088);
        } else {
            linePaint.setColor(0xFF00FFCC); 
            linePaint.setShadowLayer(20, 0, 0, 0xFF00FFCC);
        }
        
        float cosX = (float) Math.cos(angleX);
        float sinX = (float) Math.sin(angleX);
        float cosY = (float) Math.cos(angleY);
        float sinY = (float) Math.sin(angleY);
        float cosZ = (float) Math.cos(angleZ);
        float sinZ = (float) Math.sin(angleZ);
        
        float[][] projected = new float[8][2];
        float scale = Math.min(w, h) * 0.22f * pulseFactor;
        float distance = 3.5f;
        
        for (int i = 0; i < 8; i++) {
            float x = vertices[i][0];
            float y = vertices[i][1];
            float z = vertices[i][2];
            
            if (isGlitchActive && random.nextFloat() < 0.4) {
                x += (random.nextFloat() - 0.5f) * 0.22f;
                y += (random.nextFloat() - 0.5f) * 0.22f;
            }
            
            float y1 = y * cosX - z * sinX;
            float z1 = y * sinX + z * cosX;
            
            float x2 = x * cosY + z1 * sinY;
            float z2 = -x * sinY + z1 * cosY;
            
            float x3 = x2 * cosZ - y1 * sinZ;
            float y3 = x2 * sinZ + y1 * cosZ;
            
            float scaleProj = scale / (z2 + distance);
            projected[i][0] = x3 * scaleProj + w / 2f;
            projected[i][1] = y3 * scaleProj + h / 2f;
        }
        
        for (int i = 0; i < edges.length; i++) {
            int p1 = edges[i][0];
            int p2 = edges[i][1];
            canvas.drawLine(projected[p1][0], projected[p1][1], projected[p2][0], projected[p2][1], linePaint); 
        }
        
        invalidate();
    }
    
    private void drawRetroGrid(Canvas canvas, int w, int h, long elapsed) {
        float horizon = h * 0.68f;
        
        // Render outstanding retro gradient sky representation (Neon Blue/Purple)
        Paint bgPaintSky = new Paint();
        android.graphics.LinearGradient skyGrad = new android.graphics.LinearGradient(
            0, 0, 0, h,
            new int[]{ 0xFF070B16, 0xFF140C20, 0xFF05070A },
            new float[]{ 0.0f, 0.65f, 1.0f },
            android.graphics.Shader.TileMode.CLAMP
        );
        bgPaintSky.setShader(skyGrad);
        canvas.drawRect(0, 0, w, h, bgPaintSky);

        int totalGridLines = 14;
        for (int i = 0; i <= totalGridLines; i++) {
            float ratio = (float) i / totalGridLines;
            float startX = w * ratio;
            canvas.drawLine(startX, h, w / 2.0f + (ratio - 0.5f) * w * 0.32f, horizon, gridPaint); 
        }
        
        int levels = 6;
        for (int i = 0; i < levels; i++) {
            float cycle = ((elapsed / 20.0f) % 100.0f) / 100.0f;
            float stepRatio = (i + cycle) / levels;
            float y = horizon + stepRatio * (h - horizon);
            float scaleWidth = 0.32f + stepRatio * 0.68f;
            float xLeft = w / 2.0f - (w / 2.0f) * scaleWidth;
            float xRight = w / 2.0f + (w / 2.0f) * scaleWidth;
            canvas.drawLine(xLeft, y, xRight, y, gridPaint);
        }
    }
    
    private void drawParticles(Canvas canvas, int w, int h) {
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            particleZ[i] -= 0.015f * particleSpeed[i];
            if (particleZ[i] <= 0.1f) {
                particleX[i] = (random.nextFloat() - 0.5f) * 450f;
                particleY[i] = (random.nextFloat() - 0.5f) * 450f;
                particleZ[i] = 3.6f;
            }
            
            float px = particleX[i] / particleZ[i] + w / 2.0f;
            float py = particleY[i] / particleZ[i] + h / 2.0f;
            float size = (5.0f / particleZ[i]);
            
            if (px >= 0 && px <= w && py >= 0 && py <= h) {
                int opacity = (int) ((1.0f - (particleZ[i] / 3.6f)) * 185);
                if (opacity < 0) opacity = 0;
                if (opacity > 255) opacity = 255;
                particlePaint.setAlpha(opacity);
                canvas.drawCircle(px, py, size, particlePaint);
            }
        }
    }
}