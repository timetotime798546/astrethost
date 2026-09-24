package com.threeduilab.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ThreeDCubeView extends View {

    public static final int MODE_CUBE = 0;
    public static final int MODE_PYRAMID = 1;
    public static final int MODE_OCTAHEDRON = 2;
    public static final int MODE_SPHERE = 3;

    private int currentMode = MODE_CUBE;

    private float angleX = 0.5f;
    private float angleY = 0.5f;
    private float angleZ = 0.1f;

    private float autoSpeedX = 0.01f;
    private float autoSpeedY = 0.015f;
    private float autoSpeedZ = 0.005f;

    private boolean isAutoSpin = true;
    private float scaleMultiplier = 1.0f;
    private float perspectiveDistance = 3.0f; 
    private int faceAlpha = 100; 
    private float strokeWidth = 3f;

    private int strokeColor = 0xFF00E676; 
    private int fillBaseColor = 0xFF0091EA; 
    private int particleColor = 0xFFFFD700; 
    private float maxParticleSize = 16f;

    private List<Point3D> vertices = new ArrayList<>();
    private List<Face3D> faces = new ArrayList<>();
    private List<Edge3D> edges = new ArrayList<>();

    private List<PointF> projectedPoints = new ArrayList<>();
    private float[] transformedZ;

    private float lastTouchX;
    private float lastTouchY;
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public static class Point3D {
        public float x, y, z;
        public Point3D(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    public static class Face3D {
        public int[] indices;
        public int color;
        public float averageZ;

        public Face3D(int[] indices, int color) {
            this.indices = indices;
            this.color = color;
        }
    }

    public static class Edge3D {
        public int u, v;
        public Edge3D(int u, int v) {
            this.u = u;
            this.v = v;
        }
    }

    public ThreeDCubeView(Context context) {
        super(context);
        init();
    }

    public ThreeDCubeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ThreeDCubeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        buildShape();
    }

    public void setMode(int mode) {
        this.currentMode = mode;
        buildShape();
        invalidate();
    }

    public void setAutoSpin(boolean spin) {
        this.isAutoSpin = spin;
        invalidate();
    }

    public void setScaleMultiplier(float sm) {
        this.scaleMultiplier = sm;
        invalidate();
    }

    public void setPerspectiveDistance(float d) {
        this.perspectiveDistance = d;
        invalidate();
    }

    public void setFaceAlpha(int alpha) {
        this.faceAlpha = alpha;
        invalidate();
    }

    public void setSpinSpeed(float factor) {
        this.autoSpeedX = 0.01f * factor;
        this.autoSpeedY = 0.015f * factor;
        this.autoSpeedZ = 0.005f * factor;
    }

    public void setCustomColors(int stroke, int fill, int particle) {
        this.strokeColor = stroke;
        this.fillBaseColor = fill;
        this.particleColor = particle;
        buildShapeColors();
        invalidate();
    }

    private void buildShapeColors() {
        int[] palette = {
            fillBaseColor,
            adjustColorBrightness(fillBaseColor, 0.85f),
            adjustColorBrightness(fillBaseColor, 0.7f),
            adjustColorBrightness(fillBaseColor, 1.15f),
            adjustColorBrightness(fillBaseColor, 1.3f),
            adjustColorBrightness(fillBaseColor, 0.5f)
        };
        for (int i = 0; i < faces.size(); i++) {
            faces.get(i).color = palette[i % palette.length];
        }
    }

    private int adjustColorBrightness(int color, float factor) {
        int a = Color.alpha(color);
        int r = Math.min(255, (int) (Color.red(color) * factor));
        int g = Math.min(255, (int) (Color.green(color) * factor));
        int b = Math.min(255, (int) (Color.blue(color) * factor));
        return Color.argb(a, r, g, b);
    }

    private void buildShape() {
        vertices.clear();
        faces.clear();
        edges.clear();

        switch (currentMode) {
            case MODE_CUBE:
                vertices.add(new Point3D(-1, -1, -1)); 
                vertices.add(new Point3D(1, -1, -1));  
                vertices.add(new Point3D(1, 1, -1));   
                vertices.add(new Point3D(-1, 1, -1));  
                vertices.add(new Point3D(-1, -1, 1));  
                vertices.add(new Point3D(1, -1, 1));   
                vertices.add(new Point3D(1, 1, 1));    
                vertices.add(new Point3D(-1, 1, 1));   

                faces.add(new Face3D(new int[]{4, 5, 6, 7}, 0)); 
                faces.add(new Face3D(new int[]{1, 0, 3, 2}, 0)); 
                faces.add(new Face3D(new int[]{3, 2, 6, 7}, 0)); 
                faces.add(new Face3D(new int[]{4, 5, 1, 0}, 0)); 
                faces.add(new Face3D(new int[]{0, 4, 7, 3}, 0)); 
                faces.add(new Face3D(new int[]{5, 1, 2, 6}, 0)); 

                edges.add(new Edge3D(0, 1)); edges.add(new Edge3D(1, 2)); edges.add(new Edge3D(2, 3)); edges.add(new Edge3D(3, 0));
                edges.add(new Edge3D(4, 5)); edges.add(new Edge3D(5, 6)); edges.add(new Edge3D(6, 7)); edges.add(new Edge3D(7, 4));
                edges.add(new Edge3D(0, 4)); edges.add(new Edge3D(1, 5)); edges.add(new Edge3D(2, 6)); edges.add(new Edge3D(3, 7));
                break;

            case MODE_PYRAMID:
                vertices.add(new Point3D(-1, -0.6f, -1)); 
                vertices.add(new Point3D(1, -0.6f, -1));  
                vertices.add(new Point3D(1, -0.6f, 1));   
                vertices.add(new Point3D(-1, -0.6f, 1));  
                vertices.add(new Point3D(0, 1.0f, 0));     

                faces.add(new Face3D(new int[]{3, 2, 1, 0}, 0)); 
                faces.add(new Face3D(new int[]{0, 1, 4}, 0));    
                faces.add(new Face3D(new int[]{1, 2, 4}, 0));    
                faces.add(new Face3D(new int[]{2, 3, 4}, 0));    
                faces.add(new Face3D(new int[]{3, 0, 4}, 0));    

                edges.add(new Edge3D(0, 1)); edges.add(new Edge3D(1, 2)); edges.add(new Edge3D(2, 3)); edges.add(new Edge3D(3, 0));
                edges.add(new Edge3D(0, 4)); edges.add(new Edge3D(1, 4)); edges.add(new Edge3D(2, 4)); edges.add(new Edge3D(3, 4));
                break;

            case MODE_OCTAHEDRON:
                vertices.add(new Point3D(0, 1.2f, 0));   
                vertices.add(new Point3D(0, -1.2f, 0));  
                vertices.add(new Point3D(-1, 0, -1));    
                vertices.add(new Point3D(1, 0, -1));     
                vertices.add(new Point3D(1, 0, 1));      
                vertices.add(new Point3D(-1, 0, 1));     

                faces.add(new Face3D(new int[]{0, 2, 3}, 0));
                faces.add(new Face3D(new int[]{0, 3, 4}, 0));
                faces.add(new Face3D(new int[]{0, 4, 5}, 0));
                faces.add(new Face3D(new int[]{0, 5, 2}, 0));
                faces.add(new Face3D(new int[]{1, 3, 2}, 0));
                faces.add(new Face3D(new int[]{1, 4, 3}, 0));
                faces.add(new Face3D(new int[]{1, 5, 4}, 0));
                faces.add(new Face3D(new int[]{1, 2, 5}, 0));

                edges.add(new Edge3D(2, 3)); edges.add(new Edge3D(3, 4)); edges.add(new Edge3D(4, 5)); edges.add(new Edge3D(5, 2));
                edges.add(new Edge3D(0, 2)); edges.add(new Edge3D(0, 3)); edges.add(new Edge3D(0, 4)); edges.add(new Edge3D(0, 5));
                edges.add(new Edge3D(1, 2)); edges.add(new Edge3D(1, 3)); edges.add(new Edge3D(1, 4)); edges.add(new Edge3D(1, 5));
                break;

            case MODE_SPHERE:
                int n = 120;
                float goldenRatio = (float) ((1.0 + Math.sqrt(5.0)) / 2.0);
                for (int i = 0; i < n; i++) {
                    float y = 1.0f - ((float) i / (float) (n - 1)) * 2.0f;
                    float radius = (float) Math.sqrt(1.0f - y * y);
                    float theta = goldenRatio * 2.0f * (float) Math.PI * i;
                    float x = (float) Math.cos(theta) * radius;
                    float z = (float) Math.sin(theta) * radius;
                    vertices.add(new Point3D(x * 1.1f, y * 1.1f, z * 1.1f));
                }
                break;
        }

        buildShapeColors();
    }

    private void rotateAndProject() {
        float cosX = (float) Math.cos(angleX);
        float sinX = (float) Math.sin(angleX);
        float cosY = (float) Math.cos(angleY);
        float sinY = (float) Math.sin(angleY);
        float cosZ = (float) Math.cos(angleZ);
        float sinZ = (float) Math.sin(angleZ);

        int numPoints = vertices.size();
        while (projectedPoints.size() < numPoints) {
            projectedPoints.add(new PointF());
        }
        if (transformedZ == null || transformedZ.length < numPoints) {
            transformedZ = new float[numPoints];
        }

        float cx = getWidth() / 2.0f;
        float cy = getHeight() / 2.0f;
        float scaleFactor = getWidth() * 0.35f * scaleMultiplier;

        for (int i = 0; i < numPoints; i++) {
            Point3D p = vertices.get(i);

            float y1 = p.y * cosX - p.z * sinX;
            float z1 = p.y * sinX + p.z * cosX;

            float x2 = p.x * cosY + z1 * sinY;
            float z2 = -p.x * sinY + z1 * cosY;

            float x3 = x2 * cosZ - y1 * sinZ;
            float y3 = x2 * sinZ + y1 * cosZ;

            float d = perspectiveDistance;
            float divisor = d + z2;
            if (divisor <= 0.1f) divisor = 0.1f;

            float px = cx + (x3 * d / divisor) * scaleFactor;
            float py = cy - (y3 * d / divisor) * scaleFactor;

            projectedPoints.get(i).set(px, py);
            transformedZ[i] = z2;
        }

        if (currentMode != MODE_SPHERE) {
            for (Face3D face : faces) {
                float sumZ = 0;
                for (int idx : face.indices) {
                    sumZ += transformedZ[idx];
                }
                face.averageZ = sumZ / face.indices.length;
            }

            Collections.sort(faces, new Comparator<Face3D>() {
                @Override
                public int compare(Face3D f1, Face3D f2) {
                    return Float.compare(f2.averageZ, f1.averageZ);
                }
            });
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = x;
                lastTouchY = y;
                break;
            case MotionEvent.ACTION_MOVE:
                float dx = x - lastTouchX;
                float dy = y - lastTouchY;

                angleY += dx * 0.01f;
                angleX -= dy * 0.01f;

                lastTouchX = x;
                lastTouchY = y;
                invalidate();
                break;
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (getWidth() == 0 || getHeight() == 0) return;

        if (isAutoSpin) {
            angleX += autoSpeedX;
            angleY += autoSpeedY;
            angleZ += autoSpeedZ;
        }

        rotateAndProject();

        if (currentMode == MODE_SPHERE) {
            Integer[] indices = new Integer[vertices.size()];
            for (int i = 0; i < indices.length; i++) indices[i] = i;

            Arrays.sort(indices, new Comparator<Integer>() {
                @Override
                public int compare(Integer a, Integer b) {
                    return Float.compare(transformedZ[b], transformedZ[a]);
                }
            });

            for (int idx : indices) {
                PointF p = projectedPoints.get(idx);
                float zDepth = transformedZ[idx];

                float normalized = (zDepth + 1.3f) / 2.6f;
                if (normalized < 0.0f) normalized = 0.0f;
                if (normalized > 1.0f) normalized = 1.0f;

                float size = maxParticleSize * (1.1f - normalized * 0.7f);
                int alpha = (int) (255 * (1.0f - normalized * 0.65f));

                paint.setStyle(Paint.Style.FILL);
                paint.setColor(particleColor);
                paint.setAlpha(alpha);
                canvas.drawCircle(p.x, p.y, size, paint);
            }
        } else {
            Path facePath = new Path();
            for (Face3D face : faces) {
                facePath.reset();
                boolean first = true;
                for (int idx : face.indices) {
                    PointF p = projectedPoints.get(idx);
                    if (first) {
                        facePath.moveTo(p.x, p.y);
                        first = false;
                    } else {
                        facePath.lineTo(p.x, p.y);
                    }
                }
                facePath.close();

                if (faceAlpha > 0) {
                    paint.setStyle(Paint.Style.FILL);
                    paint.setColor(face.color);
                    paint.setAlpha(faceAlpha);
                    canvas.drawPath(facePath, paint);
                }

                paint.setStyle(Paint.Style.STROKE);
                paint.setColor(strokeColor);
                paint.setStrokeWidth(strokeWidth);
                paint.setAlpha(255);
                canvas.drawPath(facePath, paint);
            }
        }

        if (isAutoSpin) {
            postInvalidateOnAnimation();
        }
    }
}