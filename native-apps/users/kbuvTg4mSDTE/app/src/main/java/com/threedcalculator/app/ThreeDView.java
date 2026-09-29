package com.threedcalculator.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

public class ThreeDView extends View {

    // Helper classes for 3D Math
    public static class Point3D {
        public float x, y, z;
        public Point3D(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    public static class Edge {
        public int u, v;
        public int color;
        public Edge(int u, int v, int color) {
            this.u = u;
            this.v = v;
            this.color = color;
        }
    }

    private List<Point3D> vertices = new ArrayList<>();
    private List<Edge> edges = new ArrayList<>();

    // Rotation angles
    private float angleX = 25.0f;
    private float angleY = -45.0f;

    // View settings
    private float scaleMultiplier = 1.0f;
    private float lastTouchX, lastTouchY;
    private boolean isAutoRotating = false;

    // Render paints
    private Paint backgroundPaint;
    private Paint wireframePaint;
    private Paint vertexPaint;
    private Paint axisPaint;
    private Paint textPaint;

    private final Handler animHandler = new Handler();
    private final Runnable animRunnable = new Runnable() {
        @Override
        public void run() {
            if (isAutoRotating) {
                angleY += 1.0f; // Continuously rotate around horizontal circle
                invalidate();
                animHandler.postDelayed(this, 16); // Target ~60 fps
            }
        }
    };

    public ThreeDView(Context context) {
        super(context);
        init();
    }

    public ThreeDView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        backgroundPaint = new Paint();
        backgroundPaint.setColor(0xFF1E1E24); // Dark elegant slate background

        wireframePaint = new Paint();
        wireframePaint.setStyle(Paint.Style.STROKE);
        wireframePaint.setStrokeWidth(3.5f);
        wireframePaint.setAntiAlias(true);

        vertexPaint = new Paint();
        vertexPaint.setColor(0xFFFF5722); // Vibrant reddish orange
        vertexPaint.setStyle(Paint.Style.FILL);
        vertexPaint.setAntiAlias(true);

        axisPaint = new Paint();
        axisPaint.setStrokeWidth(2f);
        axisPaint.setAntiAlias(true);

        textPaint = new Paint();
        textPaint.setColor(0xFFFFFFFF);
        textPaint.setTextSize(26f);
        textPaint.setAntiAlias(true);

        // Populate with default shape
        showCube();
    }

    public void setAutoRotate(boolean enabled) {
        this.isAutoRotating = enabled;
        animHandler.removeCallbacks(animRunnable);
        if (enabled) {
            animHandler.post(animRunnable);
        }
    }

    public void zoomIn() {
        scaleMultiplier *= 1.15f;
        invalidate();
    }

    public void zoomOut() {
        scaleMultiplier /= 1.15f;
        invalidate();
    }

    public void resetView() {
        angleX = 25.0f;
        angleY = -45.0f;
        scaleMultiplier = 1.0f;
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = event.getX();
                lastTouchY = event.getY();
                return true;

            case MotionEvent.ACTION_MOVE:
                float dx = event.getX() - lastTouchX;
                float dy = event.getY() - lastTouchY;

                angleY += dx * 0.4f; // Left-right drag rotates around Y axis
                angleX -= dy * 0.4f; // Up-down drag rotates around X axis

                // Keep vertical angle within limits to avoid flipping upside down
                if (angleX > 85.0f) angleX = 85.0f;
                if (angleX < -85.0f) angleX = -85.0f;

                lastTouchX = event.getX();
                lastTouchY = event.getY();
                invalidate();
                return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // Fill dark background space
        canvas.drawRect(0, 0, getWidth(), getHeight(), backgroundPaint);

        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        // Base screen scale based on dimensions
        float baseScale = Math.min(getWidth(), getHeight()) * 0.35f * scaleMultiplier;

        // Draw fainted 3D coordinate axes: Red = X, Green = Y (Height), Blue = Z
        float[] pOrigin = project(new Point3D(0, 0, 0), centerX, centerY, baseScale);
        float[] pXAxis = project(new Point3D(1.2f, 0, 0), centerX, centerY, baseScale);
        float[] pYAxis = project(new Point3D(0, 1.2f, 0), centerX, centerY, baseScale);
        float[] pZAxis = project(new Point3D(0, 0, 1.2f), centerX, centerY, baseScale);

        // Draw X Axis (Red)
        axisPaint.setColor(0xFFFF5252);
        canvas.drawLine(pOrigin[0], pOrigin[1], pXAxis[0], pXAxis[1], axisPaint);
        canvas.drawText("+X", pXAxis[0] + 5, pXAxis[1], textPaint);

        // Draw Y Axis (Green, pointing Up in 3D, down is -Y)
        axisPaint.setColor(0xFF69F0AE);
        canvas.drawLine(pOrigin[0], pOrigin[1], pYAxis[0], pYAxis[1], axisPaint);
        canvas.drawText("+Y", pYAxis[0], pYAxis[1] - 5, textPaint);

        // Draw Z Axis (Blue)
        axisPaint.setColor(0xFF40C4FF);
        canvas.drawLine(pOrigin[0], pOrigin[1], pZAxis[0], pZAxis[1], axisPaint);
        canvas.drawText("+Z", pZAxis[0] + 5, pZAxis[1] + 5, textPaint);

        // Draw edges of our 3D model
        for (int i = 0; i < edges.size(); i++) {
            Edge edge = edges.get(i);
            if (edge.u < vertices.size() && edge.v < vertices.size()) {
                float[] p1 = project(vertices.get(edge.u), centerX, centerY, baseScale);
                float[] p2 = project(vertices.get(edge.v), centerX, centerY, baseScale);
                wireframePaint.setColor(edge.color);
                canvas.drawLine(p1[0], p1[1], p2[0], p2[1], wireframePaint);
            }
        }

        // Draw dots at vertices
        for (int i = 0; i < vertices.size(); i++) {
            float[] p = project(vertices.get(i), centerX, centerY, baseScale);
            canvas.drawCircle(p[0], p[1], 6f, vertexPaint);
        }
    }

    // Perspective Projection helper math
    private float[] project(Point3D p, float centerX, float centerY, float baseScale) {
        float cosY = (float) Math.cos(Math.toRadians(angleY));
        float sinY = (float) Math.sin(Math.toRadians(angleY));
        float cosX = (float) Math.cos(Math.toRadians(angleX));
        float sinX = (float) Math.sin(Math.toRadians(angleX));

        // 1. Rotate Y (around Y vertical axis)
        float x1 = p.x * cosY + p.z * sinY;
        float z1 = -p.x * sinY + p.z * cosY;

        // 2. Rotate X (around X horizontal axis)
        float y2 = p.y * cosX - z1 * sinX;
        float z2 = p.y * sinX + z1 * cosX;

        // Camera distance translation
        float cameraDistance = 5.0f;
        float depth = cameraDistance + z2;
        if (depth < 0.1f) depth = 0.1f;

        // Dynamic perspective scaling multiplier
        float scaleFactor = 1.3f;
        float xProj = x1 * (cameraDistance / depth) * scaleFactor;
        float yProj = y2 * (cameraDistance / depth) * scaleFactor;

        // Map to 2D pixel Screen coordinates
        float screenX = centerX + xProj * baseScale;
        float screenY = centerY - yProj * baseScale; // Subtracted because Y is upside down on screens

        return new float[]{screenX, screenY};
    }

    // 1. Setup Cube mesh
    public void showCube() {
        vertices.clear();
        edges.clear();

        float half = 0.75f;
        // Vertices list
        vertices.add(new Point3D(-half, -half, -half)); // 0
        vertices.add(new Point3D(half, -half, -half));  // 1
        vertices.add(new Point3D(half, half, -half));   // 2
        vertices.add(new Point3D(-half, half, -half));  // 3
        vertices.add(new Point3D(-half, -half, half));  // 4
        vertices.add(new Point3D(half, -half, half));   // 5
        vertices.add(new Point3D(half, half, half));    // 6
        vertices.add(new Point3D(-half, half, half));   // 7

        int c = 0xFF00E5FF; // Electric cyan
        // Base edges
        edges.add(new Edge(0, 1, c));
        edges.add(new Edge(1, 2, c));
        edges.add(new Edge(2, 3, c));
        edges.add(new Edge(3, 0, c));
        // Top edges
        edges.add(new Edge(4, 5, c));
        edges.add(new Edge(5, 6, c));
        edges.add(new Edge(6, 7, c));
        edges.add(new Edge(7, 4, c));
        // Connectors
        edges.add(new Edge(0, 4, c));
        edges.add(new Edge(1, 5, c));
        edges.add(new Edge(2, 6, c));
        edges.add(new Edge(3, 7, c));

        invalidate();
    }

    // 2. Setup Sphere wireframe rings
    public void showSphere(float ratio) {
        vertices.clear();
        edges.clear();

        int latDivs = 10;
        int lonDivs = 14;
        float r = 0.85f * ratio;

        for (int i = 0; i <= latDivs; i++) {
            double theta = i * Math.PI / latDivs;
            float sinTheta = (float) Math.sin(theta);
            float cosTheta = (float) Math.cos(theta);

            for (int j = 0; j < lonDivs; j++) {
                double phi = j * 2 * Math.PI / lonDivs;
                float x = r * sinTheta * (float) Math.cos(phi);
                float y = r * cosTheta;
                float z = r * sinTheta * (float) Math.sin(phi);
                vertices.add(new Point3D(x, y, z));
            }
        }

        int c = 0xFF00FF87; // Emerald Neon
        for (int i = 0; i <= latDivs; i++) {
            for (int j = 0; j < lonDivs; j++) {
                int current = i * lonDivs + j;
                int nextLon = i * lonDivs + ((j + 1) % lonDivs);
                edges.add(new Edge(current, nextLon, c)); // horizontal rings

                if (i < latDivs) {
                    int nextLat = (i + 1) * lonDivs + j;
                    edges.add(new Edge(current, nextLat, c)); // vertical arcs
                }
            }
        }
        invalidate();
    }

    // 3. Setup Cylinder wireframe
    public void showCylinder(float rRatio, float hRatio) {
        vertices.clear();
        edges.clear();

        int divs = 16;
        float r = 0.65f * rRatio;
        float h = 1.3f * hRatio;

        // Bottom circular cap (y = -h/2)
        for (int i = 0; i < divs; i++) {
            double angle = i * 2 * Math.PI / divs;
            float x = r * (float) Math.cos(angle);
            float z = r * (float) Math.sin(angle);
            vertices.add(new Point3D(x, -h / 2.0f, z));
        }

        // Top circular cap (y = h/2)
        for (int i = 0; i < divs; i++) {
            double angle = i * 2 * Math.PI / divs;
            float x = r * (float) Math.cos(angle);
            float z = r * (float) Math.sin(angle);
            vertices.add(new Point3D(x, h / 2.0f, z));
        }

        int c = 0xFFFFD700; // Gold Yellow
        for (int i = 0; i < divs; i++) {
            int next = (i + 1) % divs;
            edges.add(new Edge(i, next, c));              // bottom circle edge
            edges.add(new Edge(i + divs, next + divs, c)); // top circle edge
            edges.add(new Edge(i, i + divs, c));          // vertical connecting rib
        }
        invalidate();
    }

    // 4. Setup Cone wireframe
    public void showCone(float rRatio, float hRatio) {
        vertices.clear();
        edges.clear();

        int divs = 16;
        float r = 0.7f * rRatio;
        float h = 1.3f * hRatio;

        // Base circular plane
        for (int i = 0; i < divs; i++) {
            double angle = i * 2 * Math.PI / divs;
            float x = r * (float) Math.cos(angle);
            float z = r * (float) Math.sin(angle);
            vertices.add(new Point3D(x, -h / 2.0f, z));
        }

        // Apex at top
        int apexIndex = vertices.size();
        vertices.add(new Point3D(0f, h / 2.0f, 0f));

        int c = 0xFFFF007F; // Hot Neon Pink
        for (int i = 0; i < divs; i++) {
            int next = (i + 1) % divs;
            edges.add(new Edge(i, next, c));          // circular bottom boundary
            edges.add(new Edge(i, apexIndex, c));     // line from edge to apex
        }
        invalidate();
    }

    // 5. Setup Pyramid wireframe
    public void showPyramid(float wRatio, float hRatio) {
        vertices.clear();
        edges.clear();

        float w = 1.2f * wRatio;
        float h = 1.3f * hRatio;

        // Square Base
        vertices.add(new Point3D(-w / 2, -h / 2, -w / 2)); // 0
        vertices.add(new Point3D(w / 2, -h / 2, -w / 2));  // 1
        vertices.add(new Point3D(w / 2, -h / 2, w / 2));   // 2
        vertices.add(new Point3D(-w / 2, -h / 2, w / 2));  // 3

        // Apex
        vertices.add(new Point3D(0f, h / 2f, 0f));         // 4

        int c = 0xFFFFAB00; // Orange Amber
        edges.add(new Edge(0, 1, c));
        edges.add(new Edge(1, 2, c));
        edges.add(new Edge(2, 3, c));
        edges.add(new Edge(3, 0, c));

        edges.add(new Edge(0, 4, c));
        edges.add(new Edge(1, 4, c));
        edges.add(new Edge(2, 4, c));
        edges.add(new Edge(3, 4, c));

        invalidate();
    }

    // 6. Setup 3D Function Mesh surfaces
    public void showFunctionSurface(int funcType, float resolution, float amplitude) {
        vertices.clear();
        edges.clear();

        int res = (int) resolution;
        if (res < 6) res = 6;
        if (res > 24) res = 24;

        float range = 3.0f; // from -1.5 to 1.5
        float step = range / res;

        // Generate grid of vertices on X-Z plane with Y computed as height
        for (int i = 0; i <= res; i++) {
            float x = -1.5f + (i * step);
            for (int j = 0; j <= res; j++) {
                float z = -1.5f + (j * step);
                float y = 0.0f;

                if (funcType == 0) { // Ripple Surface: sin(sqrt(x^2 + z^2))
                    float distance = (float) Math.sqrt(x * x + z * z);
                    y = (float) Math.sin(distance * 4.5f) * 0.45f * amplitude;
                } else if (funcType == 1) { // Saddle / Hyperbolic Paraboloid: x^2 - z^2
                    y = (x * x - z * z) * 0.22f * amplitude;
                } else if (funcType == 2) { // Cos-Sin Waves: cos(x*4) * sin(z*4)
                    y = (float) (Math.cos(x * 4.0f) * Math.sin(z * 4.0f)) * 0.40f * amplitude;
                } else if (funcType == 3) { // Standard Paraboloid: x^2 + z^2
                    y = (float) ((x * x + z * z) * 0.18f - 0.4f) * amplitude;
                }

                vertices.add(new Point3D(x, y, z));
            }
        }

        // Stitch vertices together into a wireframe grid
        int stride = res + 1;
        int color = 0xFFBB86FC; // Vibrant purple neon
        for (int i = 0; i <= res; i++) {
            for (int j = 0; j <= res; j++) {
                int current = i * stride + j;

                // Right horizontal wire connection
                if (j < res) {
                    edges.add(new Edge(current, current + 1, color));
                }
                // Down vertical wire connection
                if (i < res) {
                    edges.add(new Edge(current, current + stride, color));
                }
            }
        }
        invalidate();
    }
}