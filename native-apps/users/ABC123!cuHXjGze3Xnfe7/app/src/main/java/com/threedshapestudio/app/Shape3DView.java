package com.threedshapestudio.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.Choreographer;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Shape3DView extends View {

    // 3D Geometry classes
    public static class Vertex {
        public float x, y, z;
        public Vertex(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    public static class Face {
        public int[] indices;
        public Face(int... indices) {
            this.indices = indices;
        }
    }

    public static class Model {
        public Vertex[] vertices;
        public Face[] faces;
        public Model(Vertex[] vertices, Face[] faces) {
            this.vertices = vertices;
            this.faces = faces;
        }
    }

    public static class ColorScheme {
        public String name;
        public int baseColor;
        public int wireColor;
        public int vertexColor;

        public ColorScheme(String name, int baseColor, int wireColor, int vertexColor) {
            this.name = name;
            this.baseColor = baseColor;
            this.wireColor = wireColor;
            this.vertexColor = vertexColor;
        }
    }

    // Helper for Painter's algorithm depth sorting
    private static class RenderFace implements Comparable<RenderFace> {
        public int index;
        public float avgZ;

        public RenderFace(int index, float avgZ) {
            this.index = index;
            this.avgZ = avgZ;
        }

        @Override
        public int compareTo(RenderFace other) {
            // Sort from farthest (smaller Z) to closest (larger Z)
            return Float.compare(this.avgZ, other.avgZ);
        }
    }

    // Interactive and rendering parameters
    private Model[] models = new Model[5];
    private int currentModelIndex = 0;

    private ColorScheme[] schemes = new ColorScheme[5];
    private int currentSchemeIndex = 0;

    private float angleX = 0.4f;
    private float angleY = 0.6f;
    private float angleZ = 0.0f;

    private float scale = 1.0f;
    private float autoRotateSpeed = 0.015f;
    private float lightAngleX = 45.0f;
    private float lightAngleY = 45.0f;

    private boolean isWireframeMode = true;
    private boolean isShadingMode = true;
    private boolean isVerticesVisible = true;
    private boolean isAutoRotateX = true;
    private boolean isAutoRotateY = true;

    private float lastTouchX;
    private float lastTouchY;
    private float velocityX = 0.0f;
    private float velocityY = 0.0f;

    private float cameraDistance = 4.5f;

    private Paint fillPaint;
    private Paint strokePaint;
    private Paint vertexPaint;

    private ScaleGestureDetector scaleGestureDetector;
    private Choreographer.FrameCallback animatorCallback;
    private boolean isAnimating = false;

    public Shape3DView(Context context) {
        super(context);
        init(context);
    }

    public Shape3DView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        // Build rendering shapes
        initModels();

        // Build premium high fidelity color presets
        schemes[0] = new ColorScheme("CYAN", 0xFF00E5FF, 0xFF00B0FF, 0xFFFFFFFF);
        schemes[1] = new ColorScheme("PURPLE", 0xFFE040FB, 0xFF9C27B0, 0xFFFFFFFF);
        schemes[2] = new ColorScheme("EMERALD", 0xFF00E676, 0xFF00C853, 0xFFB9F6CA);
        schemes[3] = new ColorScheme("LAVA", 0xFFFF3D00, 0xFFDD2C00, 0xFFFFE0B2);
        schemes[4] = new ColorScheme("GOLD", 0xFFFFEA00, 0xFFFFD600, 0xFFFFFFFF);

        // Setup drawing paint assets
        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setStyle(Paint.Style.FILL);

        strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(3.0f);

        vertexPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        vertexPaint.setStyle(Paint.Style.FILL);

        // Multi-touch scale listener
        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                scale *= detector.getScaleFactor();
                scale = Math.max(0.3f, Math.min(scale, 3.0f));
                invalidate();
                return true;
            }
        });

        // 60FPS Refresh rate loop animation handler
        animatorCallback = new Choreographer.FrameCallback() {
            @Override
            public void doFrame(long frameTimeNanos) {
                if (!isAnimating) return;

                if (isAutoRotateX) {
                    angleX += autoRotateSpeed;
                }
                if (isAutoRotateY) {
                    angleY += autoRotateSpeed * 1.2f;
                }

                // Decelerate drag velocity inertia
                angleY += velocityX;
                angleX += velocityY;
                velocityX *= 0.92f;
                velocityY *= 0.92f;

                invalidate();
                Choreographer.getInstance().postFrameCallback(this);
            }
        };
    }

    private void initModels() {
        // 1. CUBE
        Vertex[] cubeVerts = new Vertex[]{
                new Vertex(-1f, -1f, -1f), new Vertex(1f, -1f, -1f),
                new Vertex(1f, 1f, -1f), new Vertex(-1f, 1f, -1f),
                new Vertex(-1f, -1f, 1f), new Vertex(1f, -1f, 1f),
                new Vertex(1f, 1f, 1f), new Vertex(-1f, 1f, 1f)
        };
        Face[] cubeFaces = new Face[]{
                new Face(4, 5, 6, 7), // Front
                new Face(1, 0, 3, 2), // Back
                new Face(3, 2, 6, 7), // Top
                new Face(0, 1, 5, 4), // Bottom
                new Face(1, 2, 6, 5), // Right
                new Face(0, 4, 7, 3)  // Left
        };
        models[0] = new Model(cubeVerts, cubeFaces);

        // 2. TETRAHEDRON (Pyramid)
        Vertex[] tetraVerts = new Vertex[]{
                new Vertex(0f, 1.2f, 0f),       // Peak
                new Vertex(-1.1f, -0.9f, -1.1f), // Base LB
                new Vertex(1.1f, -0.9f, -1.1f),  // Base RB
                new Vertex(0f, -0.9f, 1.3f)      // Base Front
        };
        Face[] tetraFaces = new Face[]{
                new Face(1, 2, 3), // Base
                new Face(1, 3, 0), // Left Front
                new Face(3, 2, 0), // Right Front
                new Face(2, 1, 0)  // Back
        };
        models[1] = new Model(tetraVerts, tetraFaces);

        // 3. OCTAHEDRON (Double Pyramid)
        Vertex[] octaVerts = new Vertex[]{
                new Vertex(0f, 1.4f, 0f),  // Top Peak
                new Vertex(-1f, 0f, -1f),
                new Vertex(1f, 0f, -1f),
                new Vertex(1f, 0f, 1f),
                new Vertex(-1f, 0f, 1f),
                new Vertex(0f, -1.4f, 0f)  // Bottom Peak
        };
        Face[] octaFaces = new Face[]{
                new Face(4, 3, 0), new Face(3, 2, 0), new Face(2, 1, 0), new Face(1, 4, 0), // Top 4
                new Face(3, 4, 5), new Face(2, 3, 5), new Face(1, 2, 5), new Face(4, 1, 5)  // Bottom 4
        };
        models[2] = new Model(octaVerts, octaFaces);

        // 4. TRIANGULAR PRISM
        Vertex[] prismVerts = new Vertex[]{
                new Vertex(-1f, -1f, -0.9f), new Vertex(1f, -1f, -0.9f), new Vertex(0f, 1f, -0.9f), // Back
                new Vertex(-1f, -1f, 0.9f),  new Vertex(1f, -1f, 0.9f),  new Vertex(0f, 1f, 0.9f)   // Front
        };
        Face[] prismFaces = new Face[]{
                new Face(3, 4, 5),     // Front triangle
                new Face(1, 0, 2),     // Back triangle
                new Face(0, 1, 4, 3),  // Bottom rectangular face
                new Face(1, 2, 5, 4),  // Right rectangular face
                new Face(2, 0, 3, 5)   // Left rectangular face
        };
        models[3] = new Model(prismVerts, prismFaces);

        // 5. 24-FACED 3D STAR (Merkabah geometry)
        Vertex[] starVerts = new Vertex[]{
                // Inner core bounds
                new Vertex(-0.6f, -0.6f, -0.6f), new Vertex(0.6f, -0.6f, -0.6f),
                new Vertex(0.6f, 0.6f, -0.6f),  new Vertex(-0.6f, 0.6f, -0.6f),
                new Vertex(-0.6f, -0.6f, 0.6f),  new Vertex(0.6f, -0.6f, 0.6f),
                new Vertex(0.6f, 0.6f, 0.6f),   new Vertex(-0.6f, 0.6f, 0.6f),
                // Six outstanding peaks
                new Vertex(0f, 1.8f, 0f),   // Top
                new Vertex(0f, -1.8f, 0f),  // Bottom
                new Vertex(1.8f, 0f, 0f),   // Right
                new Vertex(-1.8f, 0f, 0f),  // Left
                new Vertex(0f, 0f, 1.8f),   // Front
                new Vertex(0f, 0f, -1.8f)   // Back
        };
        Face[] starFaces = new Face[]{
                // Faces radiating from Top peak (8)
                new Face(3, 2, 8), new Face(2, 6, 8), new Face(6, 7, 8), new Face(7, 3, 8),
                // Faces radiating from Bottom peak (9)
                new Face(1, 0, 9), new Face(0, 4, 9), new Face(4, 5, 9), new Face(5, 1, 9),
                // Faces radiating from Right peak (10)
                new Face(1, 5, 10), new Face(5, 6, 10), new Face(6, 2, 10), new Face(2, 1, 10),
                // Faces radiating from Left peak (11)
                new Face(0, 11, 4), new Face(4, 11, 7), new Face(7, 11, 3), new Face(3, 11, 0),
                // Faces radiating from Front peak (12)
                new Face(4, 12, 5), new Face(5, 12, 6), new Face(6, 12, 7), new Face(7, 12, 4),
                // Faces radiating from Back peak (13)
                new Face(1, 13, 0), new Face(0, 13, 3), new Face(3, 13, 2), new Face(2, 13, 1)
        };
        models[4] = new Model(starVerts, starFaces);
    }

    public void startAnimating() {
        if (!isAnimating) {
            isAnimating = true;
            Choreographer.getInstance().postFrameCallback(animatorCallback);
        }
    }

    public void stopAnimating() {
        isAnimating = false;
    }

    public void setShapeIndex(int index) {
        if (index >= 0 && index < models.length) {
            currentModelIndex = index;
            invalidate();
        }
    }

    public void setColorScheme(int index) {
        if (index >= 0 && index < schemes.length) {
            currentSchemeIndex = index;
            invalidate();
        }
    }

    public void setScaleValue(float multiplier) {
        this.scale = multiplier;
        invalidate();
    }

    public void setAutoRotationSpeed(float speed) {
        this.autoRotateSpeed = speed;
    }

    public void setLightAngles(float xAngle, float yAngle) {
        this.lightAngleX = xAngle;
        this.lightAngleY = yAngle;
        invalidate();
    }

    public void setWireframeMode(boolean wireframe) {
        this.isWireframeMode = wireframe;
        invalidate();
    }

    public void setShadingMode(boolean shading) {
        this.isShadingMode = shading;
        invalidate();
    }

    public void setVerticesVisible(boolean visible) {
        this.isVerticesVisible = visible;
        invalidate();
    }

    public void setAutoRotateX(boolean enabled) {
        this.isAutoRotateX = enabled;
    }

    public void setAutoRotateY(boolean enabled) {
        this.isAutoRotateY = enabled;
    }

    public void resetView() {
        angleX = 0.4f;
        angleY = 0.6f;
        angleZ = 0.0f;
        velocityX = 0.0f;
        velocityY = 0.0f;
        scale = 1.0f;
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleGestureDetector.onTouchEvent(event);

        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = x;
                lastTouchY = y;
                velocityX = 0f;
                velocityY = 0f;
                break;

            case MotionEvent.ACTION_MOVE:
                if (!scaleGestureDetector.isInProgress()) {
                    float dx = x - lastTouchX;
                    float dy = y - lastTouchY;

                    angleY += dx * 0.008f;
                    angleX += dy * 0.008f;

                    // Set drag velocity inertia
                    velocityX = dx * 0.005f;
                    velocityY = dy * 0.005f;

                    lastTouchX = x;
                    lastTouchY = y;
                    invalidate();
                }
                break;
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        float centerX = w / 2f;
        float centerY = h / 2f;
        float drawScale = Math.min(w, h) * 0.38f * scale;

        Model model = models[currentModelIndex];
        ColorScheme scheme = schemes[currentSchemeIndex];

        // 1. Math trigonometric pre-computations
        float cosX = (float) Math.cos(angleX);
        float sinX = (float) Math.sin(angleX);
        float cosY = (float) Math.cos(angleY);
        float sinY = (float) Math.sin(angleY);
        float cosZ = (float) Math.cos(angleZ);
        float sinZ = (float) Math.sin(angleZ);

        // Project coordinate vertices into rotated 3D spaces
        Vertex[] rotatedVertices = new Vertex[model.vertices.length];
        float[] projX = new float[model.vertices.length];
        float[] projY = new float[model.vertices.length];

        for (int i = 0; i < model.vertices.length; i++) {
            Vertex v = model.vertices[i];

            // Rotation around X axis
            float y1 = v.y * cosX - v.z * sinX;
            float z1 = v.y * sinX + v.z * cosX;

            // Rotation around Y axis
            float x2 = v.x * cosY + z1 * sinY;
            float z2 = -v.x * sinY + z1 * cosY;

            // Rotation around Z axis
            float x3 = x2 * cosZ - y1 * sinZ;
            float y3 = x2 * sinZ + y1 * cosZ;

            rotatedVertices[i] = new Vertex(x3, y3, z2);

            // Perspective computation projection formulas
            float depthOffset = z2 + cameraDistance;
            if (depthOffset < 0.1f) depthOffset = 0.1f;

            projX[i] = centerX + (x3 * drawScale) / depthOffset;
            projY[i] = centerY - (y3 * drawScale) / depthOffset;
        }

        // Calculate dynamic directional light coordinates
        double lxRad = Math.toRadians(lightAngleX);
        double lyRad = Math.toRadians(lightAngleY);
        float lx = (float) (Math.cos(lxRad) * Math.sin(lyRad));
        float ly = (float) Math.sin(lxRad);
        float lz = (float) (Math.cos(lxRad) * Math.cos(lyRad));

        // Normalize Light coordinates vector
        float lightLen = (float) Math.sqrt(lx * lx + ly * ly + lz * lz);
        if (lightLen > 0) {
            lx /= lightLen; ly /= lightLen; lz /= lightLen;
        }

        // 2. Painter's Algorithm Depth-sorting buffer
        List<RenderFace> sortedFaces = new ArrayList<>();
        for (int i = 0; i < model.faces.length; i++) {
            Face f = model.faces[i];
            float sumZ = 0;
            for (int idx : f.indices) {
                sumZ += rotatedVertices[idx].z;
            }
            float avgZ = sumZ / f.indices.length;
            sortedFaces.add(new RenderFace(i, avgZ));
        }
        Collections.sort(sortedFaces);

        // 3. Render 3D polygons
        Path facePath = new Path();
        for (RenderFace rf : sortedFaces) {
            Face face = model.faces[rf.index];
            if (face.indices.length < 3) continue;

            // Setup face layout vectors
            facePath.reset();
            facePath.moveTo(projX[face.indices[0]], projY[face.indices[0]]);
            for (int j = 1; j < face.indices.length; j++) {
                facePath.lineTo(projX[face.indices[j]], projY[face.indices[j]]);
            }
            facePath.close();

            // Calculate cross-product normal shading vectors
            Vertex v0 = rotatedVertices[face.indices[0]];
            Vertex v1 = rotatedVertices[face.indices[1]];
            Vertex v2 = rotatedVertices[face.indices[2]];

            float ax = v1.x - v0.x;
            float ay = v1.y - v0.y;
            float az = v1.z - v0.z;

            float bx = v2.x - v0.x;
            float by = v2.y - v0.y;
            float bz = v2.z - v0.z;

            // Normal Vector 3D cross components
            float nx = ay * bz - az * by;
            float ny = az * bx - ax * bz;
            float nz = ax * by - ay * bx;

            // Normalize Normal Vector
            float normalLen = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (normalLen > 0) {
                nx /= normalLen; ny /= normalLen; nz /= normalLen;
            }

            // Dot product lighting scalar shading formula
            float dot = nx * lx + ny * ly + nz * lz;
            // Ambient base scalar intensity (0.15 ambient + 0.85 diffuse)
            float intensity = 0.15f + 0.85f * Math.max(0.0f, dot);

            // solid shading layer execution
            if (isShadingMode) {
                int baseColor = scheme.baseColor;
                int r = (baseColor >> 16) & 0xFF;
                int g = (baseColor >> 8) & 0xFF;
                int b = baseColor & 0xFF;

                // Scale light intensities
                int litR = Math.max(0, Math.min(255, (int) (r * intensity)));
                int litG = Math.max(0, Math.min(255, (int) (g * intensity)));
                int litB = Math.max(0, Math.min(255, (int) (b * intensity)));
                int litColor = 0xFF000000 | (litR << 16) | (litG << 8) | litB;

                fillPaint.setColor(litColor);
                canvas.drawPath(facePath, fillPaint);
            }

            // wireframe border execution
            if (isWireframeMode) {
                strokePaint.setColor(scheme.wireColor);
                canvas.drawPath(facePath, strokePaint);
            }
        }

        // Draw structural vertex dots
        if (isVerticesVisible) {
            vertexPaint.setColor(scheme.vertexColor);
            for (int i = 0; i < model.vertices.length; i++) {
                canvas.drawCircle(projX[i], projY[i], 7.0f, vertexPaint);
            }
        }
    }
}