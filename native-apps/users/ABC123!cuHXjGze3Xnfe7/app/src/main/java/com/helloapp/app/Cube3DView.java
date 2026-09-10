package com.helloapp.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class Cube3DView extends View {

    public static final int SHAPE_CUBE = 0;
    public static final int SHAPE_PYRAMID = 1;
    public static final int SHAPE_DIAMOND = 2;

    // Cube vertices
    private static final float[][] CUBE_VERTICES = {
        {-1.0f, -1.0f, -1.0f}, {1.0f, -1.0f, -1.0f}, {1.0f, 1.0f, -1.0f}, {-1.0f, 1.0f, -1.0f},
        {-1.0f, -1.0f, 1.0f},  {1.0f, -1.0f, 1.0f},  {1.0f, 1.0f, 1.0f},  {-1.0f, 1.0f, 1.0f}
    };
    private static final int[][] CUBE_FACES = {
        {0, 1, 2, 3}, // Back
        {4, 5, 6, 7}, // Front
        {0, 1, 5, 4}, // Bottom
        {2, 3, 7, 6}, // Top
        {0, 3, 7, 4}, // Left
        {1, 2, 6, 5}  // Right
    };

    // Pyramid vertices (base at y=1.0, apex at y=-1.2)
    private static final float[][] PYRAMID_VERTICES = {
        {-1.0f, 1.0f, -1.0f}, {1.0f, 1.0f, -1.0f}, {1.0f, 1.0f, 1.0f}, {-1.0f, 1.0f, 1.0f},
        {0.0f, -1.2f, 0.0f} // Apex
    };
    private static final int[][] PYRAMID_FACES = {
        {3, 2, 1, 0}, // Base
        {0, 1, 4},    // Front-facing side
        {1, 2, 4},    // Right-facing side
        {2, 3, 4},    // Back-facing side
        {3, 0, 4}     // Left-facing side
    };

    // Diamond / Octahedron vertices
    private static final float[][] DIAMOND_VERTICES = {
        {0.0f, -1.4f, 0.0f}, // Top Apex
        {-1.0f, 0.0f, -1.0f}, {1.0f, 0.0f, -1.0f}, {1.0f, 0.0f, 1.0f}, {-1.0f, 0.0f, 1.0f},
        {0.0f, 1.4f, 0.0f}  // Bottom Apex
    };
    private static final int[][] DIAMOND_FACES = {
        {0, 1, 2}, {0, 2, 3}, {0, 3, 4}, {0, 4, 1}, // Top pyramids
        {5, 2, 1}, {5, 3, 2}, {5, 4, 3}, {5, 1, 4}  // Bottom pyramids
    };

    private int currentShape = SHAPE_CUBE;
    private String currentVibe = "Energetic";

    private float angleX = 25.0f;
    private float angleY = 35.0f;
    private float lastTouchX;
    private float lastTouchY;
    private boolean isDragging = false;

    private Paint linePaint;
    private Paint fillPaint;
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
        linePaint.setStrokeWidth(5.0f);
        linePaint.setAntiAlias(true);
        linePaint.setStyle(Paint.Style.STROKE);

        fillPaint = new Paint();
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setAntiAlias(true);

        nodePaint = new Paint();
        nodePaint.setColor(Color.parseColor("#FFFFFF"));
        nodePaint.setAntiAlias(true);
        nodePaint.setStyle(Paint.Style.FILL);

        hintPaint = new Paint();
        hintPaint.setColor(Color.parseColor("#94A3B8"));
        hintPaint.setTextSize(dpToPx(11));
        hintPaint.setAntiAlias(true);
        hintPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setShape(int shape) {
        this.currentShape = shape;
        invalidate();
    }

    public int getShape() {
        return currentShape;
    }

    public void setVibe(String vibe) {
        this.currentVibe = vibe;
        invalidate();
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

        float[][] vertices;
        int[][] faces;

        if (currentShape == SHAPE_PYRAMID) {
            vertices = PYRAMID_VERTICES;
            faces = PYRAMID_FACES;
        } else if (currentShape == SHAPE_DIAMOND) {
            vertices = DIAMOND_VERTICES;
            faces = DIAMOND_FACES;
        } else {
            vertices = CUBE_VERTICES;
            faces = CUBE_FACES;
        }

        int numVertices = vertices.length;
        float[] projectedX = new float[numVertices];
        float[] projectedY = new float[numVertices];
        float[][] rotCoords = new float[numVertices][3];

        for (int i = 0; i < numVertices; i++) {
            float x = vertices[i][0] * size;
            float y = vertices[i][1] * size;
            float z = vertices[i][2] * size;

            // Y-Axis rotation
            float x1 = (float) (x * Math.cos(radY) - z * Math.sin(radY));
            float z1 = (float) (x * Math.sin(radY) + z * Math.cos(radY));

            // X-Axis rotation
            float y2 = (float) (y * Math.cos(radX) - z1 * Math.sin(radX));
            float z2 = (float) (y * Math.sin(radX) + z1 * Math.cos(radX));

            float distance = size * 4.0f;
            float perspective = distance / (distance + z2);

            projectedX[i] = centerX + x1 * perspective;
            projectedY[i] = centerY + y2 * perspective;

            rotCoords[i][0] = x1;
            rotCoords[i][1] = y2;
            rotCoords[i][2] = z2;
        }

        // Setup base RGB colors depending on current vibe selection
        int baseR = 139, baseG = 92, baseB = 246; // Violet default
        if ("Energetic".equals(currentVibe)) {
            baseR = 236; baseG = 72; baseB = 153; // Pink #EC4899
        } else if ("Calm".equals(currentVibe)) {
            baseR = 14; baseG = 165; baseB = 233; // Light Blue #0EA5E9
        }

        // Dynamic 3D depth-sorting algorithm (Painter's algorithm)
        class FaceInstance {
            int[] indices;
            float avgZ;
        }

        FaceInstance[] sortedFaces = new FaceInstance[faces.length];
        for (int i = 0; i < faces.length; i++) {
            FaceInstance f = new FaceInstance();
            f.indices = faces[i];
            float sumZ = 0;
            for (int idx : f.indices) {
                sumZ += rotCoords[idx][2];
            }
            f.avgZ = sumZ / f.indices.length;
            sortedFaces[i] = f;
        }

        // Sort in descending order of average Z-depth (furthest Z drawn first)
        for (int i = 0; i < sortedFaces.length - 1; i++) {
            for (int j = 0; j < sortedFaces.length - i - 1; j++) {
                if (sortedFaces[j].avgZ < sortedFaces[j+1].avgZ) {
                    FaceInstance temp = sortedFaces[j];
                    sortedFaces[j] = sortedFaces[j+1];
                    sortedFaces[j+1] = temp;
                }
            }
        }

        // Render each sorted face with calculated normal shading & outline
        for (FaceInstance f : sortedFaces) {
            // Get 3 vertices to compute the surface normal vector
            int idxA = f.indices[0];
            int idxB = f.indices[1];
            int idxC = f.indices[2];

            float ux = rotCoords[idxB][0] - rotCoords[idxA][0];
            float uy = rotCoords[idxB][1] - rotCoords[idxA][1];
            float uz = rotCoords[idxB][2] - rotCoords[idxA][2];

            float vx = rotCoords[idxC][0] - rotCoords[idxA][0];
            float vy = rotCoords[idxC][1] - rotCoords[idxA][1];
            float vz = rotCoords[idxC][2] - rotCoords[idxA][2];

            // Cross product
            float nx = uy * vz - uz * vy;
            float ny = uz * vx - ux * vz;
            float nz = ux * vy - uy * vx;

            float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (len > 0) {
                nx /= len;
                ny /= len;
                nz /= len;
            }

            // Directional Light Vector (from front, top, slightly left)
            float lx = 0.49f;
            float ly = -0.78f;
            float lz = -0.39f;

            float normalDotLight = Math.abs(nx * lx + ny * ly + nz * lz);
            float intensity = 0.40f + 0.60f * normalDotLight;

            int r = Math.min(255, (int) (baseR * intensity));
            int g = Math.min(255, (int) (baseG * intensity));
            int b = Math.min(255, (int) (baseB * intensity));

            // Set face fill paint with subtle translucent 3D glass effect
            fillPaint.setColor(Color.argb(200, r, g, b));

            Path path = new Path();
            path.moveTo(projectedX[f.indices[0]], projectedY[f.indices[0]]);
            for (int k = 1; k < f.indices.length; k++) {
                path.lineTo(projectedX[f.indices[k]], projectedY[f.indices[k]]);
            }
            path.close();

            // Draw face fill
            canvas.drawPath(path, fillPaint);

            // Draw outline stroke
            linePaint.setColor(Color.argb(255, Math.min(255, (int)(r * 1.35)), Math.min(255, (int)(g * 1.35)), Math.min(255, (int)(b * 1.35))));
            canvas.drawPath(path, linePaint);
        }

        // Draw point vertex node indicators on top
        for (int i = 0; i < numVertices; i++) {
            canvas.drawCircle(projectedX[i], projectedY[i], 8.0f, nodePaint);
        }

        canvas.drawText("Drag layout to rotate viewport", centerX, height - 10, hintPaint);
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
                SoundEngine.playClick();
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
            angleY += 1.3f;
            angleX += 0.7f;
            invalidate();
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }
}