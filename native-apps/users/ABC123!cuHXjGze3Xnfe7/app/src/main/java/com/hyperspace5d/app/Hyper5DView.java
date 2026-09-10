package com.hyperspace5d.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class Hyper5DView extends View {

    public static final int SHAPE_SIMPLEX_5D = 0;
    public static final int SHAPE_PENTERACT_5D = 1;
    public static final int SHAPE_CROSS_POLYTOPE_5D = 2;

    // Projection Plane Constants
    public static final int PLANE_XY = 0;
    public static final int PLANE_XW = 1;
    public static final int PLANE_ZV = 2;
    public static final int PLANE_YW = 3;
    public static final int PLANE_XV = 4;
    public static final int PLANE_WV = 5;

    // 5-Simplex Hexateron vertices in 5-space (6 vertices)
    private static final float[][] SIMPLEX_VERTICES = {
        {1.0f, 0.0f, 0.0f, 0.0f, 0.0f},
        {0.0f, 1.0f, 0.0f, 0.0f, 0.0f},
        {0.0f, 0.0f, 1.0f, 0.0f, 0.0f},
        {0.0f, 0.0f, 0.0f, 1.0f, 0.0f},
        {0.0f, 0.0f, 0.0f, 0.0f, 1.0f},
        {-0.34f, -0.34f, -0.34f, -0.34f, -0.34f}
    };

    // 5D Cross Polytope (10 vertices)
    private static final float[][] CROSS_VERTICES = {
        {1.2f, 0, 0, 0, 0}, {-1.2f, 0, 0, 0, 0},
        {0, 1.2f, 0, 0, 0}, {0, -1.2f, 0, 0, 0},
        {0, 0, 1.2f, 0, 0}, {0, 0, -1.2f, 0, 0},
        {0, 0, 0, 1.2f, 0}, {0, 0, 0, -1.2f, 0},
        {0, 0, 0, 0, 1.2f}, {0, 0, 0, 0, -1.2f}
    };

    // Penteract (5-Cube, 32 vertices)
    private static final float[][] PENTERACT_VERTICES = new float[32][5];
    static {
        int index = 0;
        for (int x = -1; x <= 1; x += 2) {
            for (int y = -1; y <= 1; y += 2) {
                for (int z = -1; z <= 1; z += 2) {
                    for (int w = -1; w <= 1; w += 2) {
                        for (int v = -1; v <= 1; v += 2) {
                            PENTERACT_VERTICES[index++] = new float[]{ x * 0.95f, y * 0.95f, z * 0.95f, w * 0.95f, v * 0.95f };
                        }
                    }
                }
            }
        }
    }

    private int shapeMode = SHAPE_SIMPLEX_5D;

    // Plane rotation enabled status
    private boolean[] enabledPlanes = new boolean[]{ true, true, false, true, false, false };

    // Dynamic rotation angles
    private float angleXY = 0.0f;
    private float angleXW = 0.0f;
    private float angleZV = 0.0f;
    private float angleYW = 0.0f;
    private float angleXV = 0.0f;
    private float angleWV = 0.0f;

    // User Interactive Shifts (Sliders)
    private float wAxisShift = 0.0f;
    private float vAxisShift = 0.0f;

    private float lastTouchX;
    private float lastTouchY;
    private boolean isUserDragging = false;

    private Paint linePaint;
    private Paint fillPaint;
    private Paint nodePaint;
    private Paint gridPaint;
    private Paint annotationPaint;

    public Hyper5DView(Context context) {
        super(context);
        init();
    }

    public Hyper5DView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        linePaint = new Paint();
        linePaint.setStrokeWidth(3.0f);
        linePaint.setAntiAlias(true);
        linePaint.setStyle(Paint.Style.STROKE);

        fillPaint = new Paint();
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setAntiAlias(true);

        nodePaint = new Paint();
        nodePaint.setAntiAlias(true);
        nodePaint.setStyle(Paint.Style.FILL);

        gridPaint = new Paint();
        gridPaint.setColor(Color.parseColor("#1E293B"));
        gridPaint.setStrokeWidth(1.0f);
        gridPaint.setStyle(Paint.Style.STROKE);

        annotationPaint = new Paint();
        annotationPaint.setColor(Color.parseColor("#64748B"));
        annotationPaint.setTextSize(dpToPx(10));
        annotationPaint.setAntiAlias(true);
    }

    public void setShapeMode(int mode) {
        this.shapeMode = mode;
        invalidate();
    }

    public int getShapeMode() {
        return shapeMode;
    }

    public String getShapeModeName() {
        if (shapeMode == SHAPE_SIMPLEX_5D) return "5-Simplex";
        if (shapeMode == SHAPE_PENTERACT_5D) return "Penteract (5-Cube)";
        return "5D Cross Poly";
    }

    public void setWAxisShift(float shift) {
        this.wAxisShift = shift;
        invalidate();
    }

    public float getWAxisShift() {
        return wAxisShift;
    }

    public void setVAxisShift(float shift) {
        this.vAxisShift = shift;
        invalidate();
    }

    public float getVAxisShift() {
        return vAxisShift;
    }

    public void togglePlane(int planeConstant) {
        if (planeConstant >= 0 && planeConstant < enabledPlanes.length) {
            enabledPlanes[planeConstant] = !enabledPlanes[planeConstant];
            invalidate();
        }
    }

    public boolean isPlaneEnabled(int planeConstant) {
        return planeConstant >= 0 && planeConstant < enabledPlanes.length && enabledPlanes[planeConstant];
    }

    public void incrementAngles() {
        if (!isUserDragging) {
            if (enabledPlanes[PLANE_XY]) angleXY += 1.4f;
            if (enabledPlanes[PLANE_XW]) angleXW += 0.9f;
            if (enabledPlanes[PLANE_ZV]) angleZV += 1.2f;
            if (enabledPlanes[PLANE_YW]) angleYW += 1.6f;
            if (enabledPlanes[PLANE_XV]) angleXV += 0.8f;
            if (enabledPlanes[PLANE_WV]) angleWV += 1.1f;
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        int centerX = width / 2;
        int centerY = height / 2;
        int radius = Math.min(width, height) / 3;

        // Draw geometric grid background
        canvas.drawCircle(centerX, centerY, radius * 1.2f, gridPaint);
        canvas.drawLine(0, centerY, width, centerY, gridPaint);
        canvas.drawLine(centerX, 0, centerX, height, gridPaint);

        // Get matching vertices
        float[][] sourceVertices;
        if (shapeMode == SHAPE_SIMPLEX_5D) {
            sourceVertices = SIMPLEX_VERTICES;
        } else if (shapeMode == SHAPE_CROSS_POLYTOPE_5D) {
            sourceVertices = CROSS_VERTICES;
        } else {
            sourceVertices = PENTERACT_VERTICES;
        }

        int size = sourceVertices.length;
        float[] projX = new float[size];
        float[] projY = new float[size];
        float[] calculatedZ = new float[size];

        // Trigonometric values for active rotations
        double radXY = Math.toRadians(angleXY);
        double radXW = Math.toRadians(angleXW);
        double radZV = Math.toRadians(angleZV);
        double radYW = Math.toRadians(angleYW);
        double radXV = Math.toRadians(angleXV);
        double radWV = Math.toRadians(angleWV);

        for (int i = 0; i < size; i++) {
            // Copy base 5-vector (X, Y, Z, W, V)
            float[] pt = new float[5];
            System.arraycopy(sourceVertices[i], 0, pt, 0, 5);

            // Apply 5-Dimensional plane rotations conditionally
            if (enabledPlanes[PLANE_XY]) pt = rotatePlane(pt, 0, 1, radXY); // XY
            if (enabledPlanes[PLANE_XW]) pt = rotatePlane(pt, 0, 3, radXW); // XW
            if (enabledPlanes[PLANE_ZV]) pt = rotatePlane(pt, 2, 4, radZV); // ZV
            if (enabledPlanes[PLANE_YW]) pt = rotatePlane(pt, 1, 3, radYW); // YW
            if (enabledPlanes[PLANE_XV]) pt = rotatePlane(pt, 0, 4, radXV); // XV
            if (enabledPlanes[PLANE_WV]) pt = rotatePlane(pt, 3, 4, radWV); // WV

            // CASCADED DIMENSIONAL PERSPECTIVE PROJECTION
            // 5D -> 4D (Using V scale)
            float distanceV = 3.2f + vAxisShift;
            float scaleV = distanceV / (distanceV + pt[4]);
            float x4 = pt[0] * scaleV;
            float y4 = pt[1] * scaleV;
            float z4 = pt[2] * scaleV;
            float w4 = pt[3] * scaleV;

            // 4D -> 3D (Using W scale)
            float distanceW = 3.2f + wAxisShift;
            float scaleW = distanceW / (distanceW + w4);
            float x3 = x4 * scaleW;
            float y3 = y4 * scaleW;
            float z3 = z4 * scaleW;

            // 3D -> 2D (Using standard viewport Z-scaling)
            float distanceZ = 3.5f;
            float scaleZ = distanceZ / (distanceZ + z3);

            projX[i] = centerX + x3 * scaleZ * radius;
            projY[i] = centerY + y3 * scaleZ * radius;
            calculatedZ[i] = z3; // Store for relative node color rendering
        }

        // Render connected projection edges based on selected model rules
        linePaint.setColor(Color.parseColor("#38BDF8")); // Glow cyan outline

        if (shapeMode == SHAPE_SIMPLEX_5D) {
            // Hexateron edges (all points connect)
            for (int i = 0; i < size; i++) {
                for (int j = i + 1; j < size; j++) {
                    canvas.drawLine(projX[i], projY[i], projX[j], projY[j], linePaint);
                }
            }
        } else if (shapeMode == SHAPE_CROSS_POLYTOPE_5D) {
            // 5-Cross edges (connects all except opposite poles)
            for (int i = 0; i < size; i++) {
                for (int j = i + 1; j < size; j++) {
                    if (i / 2 != j / 2) { // Skip connectors between opposite poles (+/- pair on same axis)
                        canvas.drawLine(projX[i], projY[i], projX[j], projY[j], linePaint);
                    }
                }
            }
        } else {
            // Penteract edges (connects vertices with Hamming Distance = 1)
            for (int i = 0; i < size; i++) {
                for (int j = i + 1; j < size; j++) {
                    int diff = 0;
                    for (int k = 0; k < 5; k++) {
                        if (Math.abs(sourceVertices[i][k] - sourceVertices[j][k]) > 0.1f) {
                            diff++;
                        }
                    }
                    if (diff == 1) {
                        canvas.drawLine(projX[i], projY[i], projX[j], projY[j], linePaint);
                    }
                }
            }
        }

        // Draw glowing neon vertices
        for (int i = 0; i < size; i++) {
            // Shift node color dynamically depending on the computed virtual depth (Z value)
            int alphaVal = Math.max(80, Math.min(255, (int)(180 + calculatedZ[i] * 60)));
            nodePaint.setColor(Color.argb(alphaVal, 244, 63, 94)); // Deep Neon Rose
            canvas.drawCircle(projX[i], projY[i], 7.0f, nodePaint);
        }

        // Ambient layout text guides
        canvas.drawText("W/V SHIFT ACTIVE", 16, height - 12, annotationPaint);
    }

    private float[] rotatePlane(float[] pt, int axis1, int axis2, double rad) {
        float[] rotated = new float[5];
        System.arraycopy(pt, 0, rotated, 0, 5);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        rotated[axis1] = (float) (pt[axis1] * cos - pt[axis2] * sin);
        rotated[axis2] = (float) (pt[axis1] * sin + pt[axis2] * cos);
        return rotated;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isUserDragging = true;
                lastTouchX = x;
                lastTouchY = y;
                SoundEngine.playClick();
                break;
            case MotionEvent.ACTION_MOVE:
                float dx = x - lastTouchX;
                float dy = y - lastTouchY;
                // Distribute dragging across standard spatial XY rotation and complex XW plane rotation!
                angleXY += dx * 0.35f;
                angleXW += dy * 0.35f;
                invalidate();
                lastTouchX = x;
                lastTouchY = y;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isUserDragging = false;
                break;
        }
        return true;
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }
}