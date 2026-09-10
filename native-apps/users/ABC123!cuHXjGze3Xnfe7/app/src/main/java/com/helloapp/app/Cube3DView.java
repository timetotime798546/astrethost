package com.helloapp.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class Cube3DView extends View {

    // 8 vertices representing standard normalized 3D cube coordinates
    private static final float[][] VERTICES = {
        {-1.0f, -1.0f, -1.0f}, {1.0f, -1.0f, -1.0f}, {1.0f, 1.0f, -1.0f}, {-1.0f, 1.0f, -1.0f},
        {-1.0f, -1.0f, 1.0f},  {1.0f, -1.0f, 1.0f},  {1.0f, 1.0f, 1.0f},  {-1.0f, 1.0f, 1.0f}
    };

    // 12 connecting edges between the vertices to draw wireframes
    private static final int[][] EDGES = {
        {0, 1}, {1, 2}, {2, 3}, {3, 0}, // Back boundary
        {4, 5}, {5, 6}, {6, 7}, {7, 4}, // Front boundary
        {0, 4}, {1, 5}, {2, 6}, {3, 7}  // Connectors
    };

    private float angleX = 25.0f;
    private float angleY = 35.0f;
    private float lastTouchX;
    private float lastTouchY;
    private boolean isDragging = false;

    private Paint linePaint;
    private Paint nodePaint;
    private Paint hintPaint;

    public Cube3DView(Context context) {
        super(context);
        init();
    }

    public Cube3DView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        linePaint = new Paint();
        linePaint.setColor(Color.parseColor("#8B5CF6")); // Purple structural beams
        linePaint.setStrokeWidth(6.0f);
        linePaint.setAntiAlias(true);
        linePaint.setStyle(Paint.Style.STROKE);

        nodePaint = new Paint();
        nodePaint.setColor(Color.parseColor("#F43F5E")); // Red solid corner caps
        nodePaint.setAntiAlias(true);
        nodePaint.setStyle(Paint.Style.FILL);

        hintPaint = new Paint();
        hintPaint.setColor(Color.parseColor("#94A3B8"));
        hintPaint.setTextSize(dpToPx(11));
        hintPaint.setAntiAlias(true);
        hintPaint.setTextAlign(Paint.Align.CENTER);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        int size = Math.min(width, height) / 4;
        int centerX = width / 2;
        int centerY = height / 2;

        double radX = Math.toRadians(angleX);
        double radY = Math.toRadians(angleY);

        float[] projectedX = new float[8];
        float[] projectedY = new float[8];

        for (int i = 0; i < 8; i++) {
            float x = VERTICES[i][0] * size;
            float y = VERTICES[i][1] * size;
            float z = VERTICES[i][2] * size;

            // Compute Y-Axis rotation
            float x1 = (float) (x * Math.cos(radY) - z * Math.sin(radY));
            float z1 = (float) (x * Math.sin(radY) + z * Math.cos(radY));

            // Compute X-Axis rotation
            float y2 = (float) (y * Math.cos(radX) - z1 * Math.sin(radX));
            float z2 = (float) (y * Math.sin(radX) + z1 * Math.cos(radX));

            // Perspective factor based on physical offset
            float distance = size * 3.5f;
            float perspective = distance / (distance + z2);

            projectedX[i] = centerX + x1 * perspective;
            projectedY[i] = centerY + y2 * perspective;
        }

        // Render line bounds
        for (int[] edge : EDGES) {
            canvas.drawLine(
                projectedX[edge[0]], projectedY[edge[0]],
                projectedX[edge[1]], projectedY[edge[1]],
                linePaint
            );
        }

        // Render point anchors
        for (int i = 0; i < 8; i++) {
            canvas.drawCircle(projectedX[i], projectedY[i], 10.0f, nodePaint);
        }

        canvas.drawText("Swipe layout to rotate viewport", centerX, height - 10, hintPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isDragging = true;
                lastTouchX = x;
                lastTouchY = y;
                SoundEngine.playClick(); // Play a pleasant click on viewport physical touch
                break;
            case MotionEvent.ACTION_MOVE:
                float dx = x - lastTouchX;
                float dy = y - lastTouchY;
                angleY += dx * 0.4f;
                angleX -= dy * 0.4f;
                invalidate();
                lastTouchX = x;
                lastTouchY = y;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isDragging = false;
                break;
        }
        return true;
    }

    public void autoRotate() {
        if (!isDragging) {
            angleY += 1.2f;
            angleX += 0.6f;
            invalidate();
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }
}