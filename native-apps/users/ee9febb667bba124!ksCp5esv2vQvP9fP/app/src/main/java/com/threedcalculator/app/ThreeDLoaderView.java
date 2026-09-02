package com.threedcalculator.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class ThreeDLoaderView extends View {

    private Paint linePaint;
    private float angleX = 0;
    private float angleY = 0;
    private float angleZ = 0;
    
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
        linePaint = new Paint();
        linePaint.setColor(0xFF00FFCC);
        linePaint.setStrokeWidth(6f);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setAntiAlias(true);
        linePaint.setShadowLayer(10, 0, 0, 0xFF00FFCC);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        int w = getWidth();
        int h = getHeight();
        
        angleX += 0.022f;
        angleY += 0.034f;
        angleZ += 0.012f;
        
        float cosX = (float) Math.cos(angleX);
        float sinX = (float) Math.sin(angleX);
        float cosY = (float) Math.cos(angleY);
        float sinY = (float) Math.sin(angleY);
        float cosZ = (float) Math.cos(angleZ);
        float sinZ = (float) Math.sin(angleZ);
        
        float[][] projected = new float[8][2];
        float scale = Math.min(w, h) * 0.24f;
        float distance = 3.6f;
        
        for (int i = 0; i < 8; i++) {
            float x = vertices[i][0];
            float y = vertices[i][1];
            float z = vertices[i][2];
            
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
}