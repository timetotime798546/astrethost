package com.simpleclicker.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class ThreeDCanvasView extends View {

    public interface OnUIActionListener {
        void onIncrement();
        void onDecrement();
        void onReset();
        void onStepSelected(int step);
        void onHapticToggled();
    }

    private OnUIActionListener actionListener;

    // Standard Math 3D structures
    private static class Point3D {
        double x, y, z;
        Point3D(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    private static class Face {
        int[] indices;
        int baseColor;
        Face(int[] indices, int baseColor) {
            this.indices = indices;
            this.baseColor = baseColor;
        }
    }

    private static class ThreeDObject {
        String id;
        Point3D[] localVertices;
        Point3D[] rotatedVertices;
        Face[] faces;

        // Position & Angles in 3D Space
        double x, y, z;
        double rotX, rotY, rotZ;

        // Spring mass simulation properties
        double scale = 1.0;
        double targetScale = 1.0;
        double scaleVelocity = 0.0;

        int baseColor;
        String label;
        boolean isInteractive = false;
        boolean isPressed = false;

        // Visual layout metrics
        double hitRadius = 1.0;
        double rotatedCenterX, rotatedCenterY, rotatedCenterZ;
        float projCenterX, projCenterY;
        float projRadius;
    }

    private static class RenderableFace {
        ThreeDObject parent;
        Face face;
        double avgZ;
        RenderableFace(ThreeDObject parent, Face face, double avgZ) {
            this.parent = parent;
            this.face = face;
            this.avgZ = avgZ;
        }
    }

    private static class Particle {
        double x, y, z;
        double vx, vy, vz;
        int color;
        float size;
        int age;
        int maxAge;
        Particle(double x, double y, double z, double vx, double vy, double vz, int color, float size, int maxAge) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.vx = vx;
            this.vy = vy;
            this.vz = vz;
            this.color = color;
            this.size = size;
            this.age = 0;
            this.maxAge = maxAge;
        }
    }

    private final List<ThreeDObject> objects = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();

    private Paint paint;
    private Paint textPaint;
    private Paint particlePaint;
    private Path path;

    // View camera angles adjusted dynamically via dragging
    private double angleX = -0.15;
    private double angleY = 0.12;

    private float lastTouchX;
    private float lastTouchY;
    private boolean isDragging = false;
    private float touchDownX;
    private float touchDownY;
    private ThreeDObject pressedObject = null;

    private static final int TOUCH_SLOP = 12;

    // Current synchronized State indicators
    private int countValue = 0;
    private int highScoreValue = 0;
    private int activeStep = 1;
    private boolean hapticEnabled = true;
    private int themeColor = 0xFF10B981; // default Emerald Green

    public ThreeDCanvasView(Context context) {
        super(context);
        init();
    }

    public ThreeDCanvasView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ThreeDCanvasView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setOnUIActionListener(OnUIActionListener listener) {
        this.actionListener = listener;
    }

    public void setCountValue(int count) {
        this.countValue = count;
        updateObjectStates();
    }

    public void setHighScoreValue(int highScore) {
        this.highScoreValue = highScore;
        updateObjectStates();
    }

    public void setActiveStep(int step) {
        this.activeStep = step;
        updateObjectStates();
    }

    public void setHapticEnabled(boolean enabled) {
        this.hapticEnabled = enabled;
        updateObjectStates();
    }

    public void setThemeColor(int color) {
        this.themeColor = color;
        updateObjectStates();
    }

    private void init() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStrokeWidth(2.0f);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextAlign(Paint.Align.CENTER);

        particlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        particlePaint.setStyle(Paint.Style.FILL);

        path = new Path();

        // Build total 3D layout representation
        rebuildScene();

        // Start constant render interval
        post(new Runnable() {
            @Override
            public void run() {
                postOnAnimation(animationRunnable);
            }
        });
    }

    private void rebuildScene() {
        objects.clear();

        // 1. Title Board Box: Y = -2.1
        objects.add(createCuboid("title_banner", 0, -2.15, 0, 1.35, 0.18, 0.1, 0xFF1E293B, "SIMPLE CLICKER 3D", false));

        // 2. Scoreboard Stats Box: Y = -1.3
        objects.add(createCuboid("scoreboard", 0, -1.3, 0, 1.4, 0.42, 0.15, 0xFF1E293B, "", false));

        // 3. Central gemstone Clicker: Y = -0.1
        objects.add(createGemstone("clicker", 0, -0.1, 0, 0.46, themeColor));

        // 4. Row 1 Increment steps buttons: Y = 0.7
        objects.add(createCuboid("step_1", -0.92, 0.7, 0, 0.36, 0.16, 0.12, 0xFF2563EB, "+1", true));
        objects.add(createCuboid("step_5", 0.0, 0.7, 0, 0.36, 0.16, 0.12, 0xFF475569, "+5", true));
        objects.add(createCuboid("step_10", 0.92, 0.7, 0, 0.36, 0.16, 0.12, 0xFF475569, "+10", true));

        // 5. Row 2 Action controls: Y = 1.35
        objects.add(createCuboid("btn_minus", -0.92, 1.35, 0, 0.36, 0.22, 0.15, 0xFFEF4444, "-", true));
        objects.add(createCuboid("btn_reset", 0.0, 1.35, 0, 0.36, 0.22, 0.15, 0xFF64748B, "RESET", true));
        objects.add(createCuboid("btn_plus", 0.92, 1.35, 0, 0.36, 0.22, 0.15, 0xFF10B981, "+", true));

        // 6. Row 3 Preferences Haptic toggle: Y = 2.05
        objects.add(createCuboid("toggle_haptic", 0.0, 2.05, 0, 1.35, 0.18, 0.12, 0xFF10B981, "HAPTICS: ON", true));

        updateObjectStates();
    }

    private void updateObjectStates() {
        for (ThreeDObject obj : objects) {
            if ("scoreboard".equals(obj.id)) {
                obj.label = "COUNT: " + countValue;
            } else if ("toggle_haptic".equals(obj.id)) {
                obj.label = "HAPTICS: " + (hapticEnabled ? "ON" : "OFF");
                obj.baseColor = hapticEnabled ? 0xFF10B981 : 0xFF64748B;
            } else if ("step_1".equals(obj.id)) {
                obj.baseColor = (activeStep == 1) ? 0xFF2563EB : 0xFF475569;
                obj.targetScale = (activeStep == 1) ? 1.08 : 0.95;
            } else if ("step_5".equals(obj.id)) {
                obj.baseColor = (activeStep == 5) ? 0xFF2563EB : 0xFF475569;
                obj.targetScale = (activeStep == 5) ? 1.08 : 0.95;
            } else if ("step_10".equals(obj.id)) {
                obj.baseColor = (activeStep == 10) ? 0xFF2563EB : 0xFF475569;
                obj.targetScale = (activeStep == 10) ? 1.08 : 0.95;
            } else if ("clicker".equals(obj.id)) {
                obj.baseColor = themeColor;
            }
        }
    }

    private ThreeDObject createCuboid(String id, double x, double y, double z, double dx, double dy, double dz, int baseColor, String label, boolean isInteractive) {
        ThreeDObject obj = new ThreeDObject();
        obj.id = id;
        obj.x = x;
        obj.y = y;
        obj.z = z;
        obj.baseColor = baseColor;
        obj.label = label;
        obj.isInteractive = isInteractive;
        obj.hitRadius = Math.max(dx, dy) * 1.5;

        obj.localVertices = new Point3D[] {
            new Point3D(-dx, -dy, -dz), // 0
            new Point3D(dx, -dy, -dz),  // 1
            new Point3D(dx, dy, -dz),   // 2
            new Point3D(-dx, dy, -dz),  // 3
            new Point3D(-dx, -dy, dz),  // 4
            new Point3D(dx, -dy, dz),   // 5
            new Point3D(dx, dy, dz),    // 6
            new Point3D(-dx, dy, dz)    // 7
        };

        obj.rotatedVertices = new Point3D[8];
        for (int i = 0; i < 8; i++) {
            obj.rotatedVertices[i] = new Point3D(0, 0, 0);
        }

        obj.faces = new Face[] {
            new Face(new int[]{0, 3, 2, 1}, baseColor), // Front
            new Face(new int[]{4, 5, 6, 7}, baseColor), // Back
            new Face(new int[]{0, 1, 5, 4}, baseColor), // Top
            new Face(new int[]{3, 7, 6, 2}, baseColor), // Bottom
            new Face(new int[]{0, 4, 7, 3}, baseColor), // Left
            new Face(new int[]{1, 2, 6, 5}, baseColor)  // Right
        };

        return obj;
    }

    private ThreeDObject createGemstone(String id, double x, double y, double z, double radius, int baseColor) {
        ThreeDObject obj = new ThreeDObject();
        obj.id = id;
        obj.x = x;
        obj.y = y;
        obj.z = z;
        obj.baseColor = baseColor;
        obj.label = "";
        obj.isInteractive = true;
        obj.hitRadius = radius * 1.8;

        obj.localVertices = new Point3D[12];
        obj.localVertices[0] = new Point3D(0, -1.8 * radius, 0);
        obj.localVertices[1] = new Point3D(0, 1.8 * radius, 0);

        for (int i = 0; i < 5; i++) {
            double r = Math.toRadians(i * 72);
            obj.localVertices[2 + i] = new Point3D(1.25 * radius * Math.cos(r), -0.4 * radius, 1.25 * radius * Math.sin(r));
        }

        for (int i = 0; i < 5; i++) {
            double r = Math.toRadians(i * 72 + 36);
            obj.localVertices[7 + i] = new Point3D(1.25 * radius * Math.cos(r), 0.4 * radius, 1.25 * radius * Math.sin(r));
        }

        obj.rotatedVertices = new Point3D[12];
        for (int i = 0; i < 12; i++) {
            obj.rotatedVertices[i] = new Point3D(0, 0, 0);
        }

        obj.faces = new Face[] {
            new Face(new int[]{0, 2, 3}, baseColor),
            new Face(new int[]{0, 3, 4}, baseColor),
            new Face(new int[]{0, 4, 5}, baseColor),
            new Face(new int[]{0, 5, 6}, baseColor),
            new Face(new int[]{0, 6, 2}, baseColor),

            new Face(new int[]{1, 8, 7}, baseColor),
            new Face(new int[]{1, 9, 8}, baseColor),
            new Face(new int[]{1, 10, 9}, baseColor),
            new Face(new int[]{1, 11, 10}, baseColor),
            new Face(new int[]{1, 7, 11}, baseColor),

            new Face(new int[]{2, 7, 3}, baseColor),
            new Face(new int[]{3, 7, 8}, baseColor),
            new Face(new int[]{3, 8, 4}, baseColor),
            new Face(new int[]{4, 8, 9}, baseColor),
            new Face(new int[]{4, 9, 5}, baseColor),
            new Face(new int[]{5, 9, 10}, baseColor),
            new Face(new int[]{5, 10, 6}, baseColor),
            new Face(new int[]{6, 10, 11}, baseColor),
            new Face(new int[]{6, 11, 2}, baseColor),
            new Face(new int[]{2, 11, 7}, baseColor)
        };

        return obj;
    }

    private final Runnable animationRunnable = new Runnable() {
        @Override
        public void run() {
            updatePhysics();
            invalidate();
            postOnAnimation(this);
        }
    };

    private void updatePhysics() {
        // Automatically recover camera back to base viewing perspective when not dragging
        if (!isDragging) {
            angleY = angleY * 0.95 + 0.12 * 0.05;
            angleX = angleX * 0.95 + (-0.15) * 0.05;
        }

        // Apply constant continuous local spins on clicker gemstone
        for (ThreeDObject obj : objects) {
            if ("clicker".equals(obj.id)) {
                obj.rotY += 0.015;
            }
        }

        // Run mass elastic equations individually across all layout elements
        for (ThreeDObject obj : objects) {
            double k = 0.16;
            double damping = 0.82;
            double force = -k * (obj.scale - obj.targetScale);
            obj.scaleVelocity = (obj.scaleVelocity + force) * damping;
            obj.scale += obj.scaleVelocity;
        }

        // Run spark particles kinematics loop
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.age++;
            if (p.age >= p.maxAge) {
                particles.remove(i);
                continue;
            }
            p.x += p.vx;
            p.y += p.vy;
            p.z += p.vz;
            p.vy += 0.008; // subtle gravity acceleration
            p.vx *= 0.97;
            p.vy *= 0.97;
            p.vz *= 0.97;
        }
    }

    public void bounceObject(String id) {
        for (ThreeDObject obj : objects) {
            if (obj.id.equals(id)) {
                obj.scale = 0.76;
                obj.scaleVelocity = 0.14;
                if ("clicker".equals(id)) {
                    spawnSparks();
                }
                break;
            }
        }
    }

    private void spawnSparks() {
        Random rand = new Random();
        int[] sparkColors;
        if (themeColor == 0xFF10B981) { // Emerald
            sparkColors = new int[]{0xFFA7F3D0, 0xFF34D399, 0xFF10B981, 0xFF059669, 0xFFFFFFFF};
        } else if (themeColor == 0xFF3B82F6) { // Sapphire Blue
            sparkColors = new int[]{0xFFBFDBFE, 0xFF60A5FA, 0xFF3B82F6, 0xFF2563EB, 0xFFFFFFFF};
        } else if (themeColor == 0xFFEF4444) { // Ruby
            sparkColors = new int[]{0xFFFECACA, 0xFFF87171, 0xFFEF4444, 0xFFDC2626, 0xFFFFFFFF};
        } else { // Amber Gold
            sparkColors = new int[]{0xFFFEF3C7, 0xFFFBBF24, 0xFFF59E0B, 0xFFD97706, 0xFFFFFFFF};
        }

        // Particles emit from the gemstone's center coordinates (0, -0.1, 0)
        for (int i = 0; i < 20; i++) {
            double vx = (rand.nextDouble() - 0.5) * 0.16;
            double vy = (rand.nextDouble() - 0.6) * 0.16; // upward bias expansion
            double vz = (rand.nextDouble() - 0.5) * 0.16;
            float size = 4f + rand.nextFloat() * 10f;
            int maxAge = 20 + rand.nextInt(15);
            int color = sparkColors[rand.nextInt(sparkColors.length)];
            particles.add(new Particle(0.0, -0.1, 0.0, vx, vy, vz, color, size, maxAge));
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchDownX = x;
                touchDownY = y;
                isDragging = false;
                pressedObject = null;

                // Trace which interactive 3D object was tapped
                double minDistance = Double.MAX_VALUE;
                for (ThreeDObject obj : objects) {
                    if (obj.isInteractive) {
                        double dist = Math.hypot(x - obj.projCenterX, y - obj.projCenterY);
                        if (dist < obj.projRadius) {
                            if (dist < minDistance) {
                                minDistance = dist;
                                pressedObject = obj;
                            }
                        }
                    }
                }

                if (pressedObject != null) {
                    pressedObject.isPressed = true;
                    pressedObject.targetScale = 0.84; // tactile squish down
                    invalidate();
                } else {
                    // Let user rotate camera angles manually
                    lastTouchX = x;
                    lastTouchY = y;
                    isDragging = true;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (isDragging) {
                    float dx = x - lastTouchX;
                    float dy = y - lastTouchY;
                    angleY += dx * 0.005;
                    angleX += dy * 0.005;

                    // Restrict rotation axes slightly to keep layouts vertically structured
                    if (angleX > 0.5) angleX = 0.5;
                    if (angleX < -0.5) angleX = -0.5;

                    lastTouchX = x;
                    lastTouchY = y;
                    invalidate();
                } else if (pressedObject != null) {
                    double dist = Math.hypot(x - pressedObject.projCenterX, y - pressedObject.projCenterY);
                    if (dist > pressedObject.projRadius) {
                        pressedObject.isPressed = false;
                        pressedObject.targetScale = isStepActiveObject(pressedObject.id) ? 1.08 : 1.0;
                        pressedObject = null;
                        invalidate();
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
                if (pressedObject != null) {
                    double dist = Math.hypot(x - pressedObject.projCenterX, y - pressedObject.projCenterY);
                    if (dist < pressedObject.projRadius) {
                        triggerAction(pressedObject.id);
                    }
                    pressedObject.isPressed = false;
                    pressedObject.targetScale = isStepActiveObject(pressedObject.id) ? 1.08 : 1.0;
                    pressedObject = null;
                    invalidate();
                }
                isDragging = false;
                break;

            case MotionEvent.ACTION_CANCEL:
                if (pressedObject != null) {
                    pressedObject.isPressed = false;
                    pressedObject.targetScale = isStepActiveObject(pressedObject.id) ? 1.08 : 1.0;
                    pressedObject = null;
                }
                isDragging = false;
                invalidate();
                break;
        }
        return true;
    }

    private boolean isStepActiveObject(String id) {
        return ("step_1".equals(id) && activeStep == 1)
                || ("step_5".equals(id) && activeStep == 5)
                || ("step_10".equals(id) && activeStep == 10);
    }

    private void triggerAction(String id) {
        if (actionListener == null) return;

        if (hapticEnabled) {
            performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
        }

        if ("clicker".equals(id)) {
            actionListener.onIncrement();
            bounceObject("clicker");
        } else if ("btn_plus".equals(id)) {
            actionListener.onIncrement();
            bounceObject("btn_plus");
            bounceObject("clicker");
        } else if ("btn_minus".equals(id)) {
            actionListener.onDecrement();
            bounceObject("btn_minus");
            bounceObject("clicker");
        } else if ("btn_reset".equals(id)) {
            actionListener.onReset();
            bounceObject("btn_reset");
            bounceObject("clicker");
        } else if ("step_1".equals(id)) {
            actionListener.onStepSelected(1);
            bounceObject("step_1");
        } else if ("step_5".equals(id)) {
            actionListener.onStepSelected(5);
            bounceObject("step_5");
        } else if ("step_10".equals(id)) {
            actionListener.onStepSelected(10);
            bounceObject("step_10");
        } else if ("toggle_haptic".equals(id)) {
            actionListener.onHapticToggled();
            bounceObject("toggle_haptic");
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        double centerX = width / 2.0;
        double centerY = height / 2.0;

        // Auto-scale coordinate grid sizes relative to layout bounds
        double referenceScale = height / 5.5;
        double maxAllowedScale = width / 3.4;
        if (referenceScale > maxAllowedScale) {
            referenceScale = maxAllowedScale;
        }

        // Camera focal projection factor
        double d = 4.0;

        // Step 1: Compute 3D translations on vertices across all scene components
        double cosCamY = Math.cos(angleY);
        double sinCamY = Math.sin(angleY);
        double cosCamX = Math.cos(angleX);
        double sinCamX = Math.sin(angleX);

        List<RenderableFace> renderableFaces = new ArrayList<>();

        for (ThreeDObject obj : objects) {
            double cosLocalY = Math.cos(obj.rotY);
            double sinLocalY = Math.sin(obj.rotY);
            double cosLocalX = Math.cos(obj.rotX);
            double sinLocalX = Math.sin(obj.rotX);

            for (int i = 0; i < obj.localVertices.length; i++) {
                Point3D v = obj.localVertices[i];

                // Scale local element sizes
                double lx = v.x * obj.scale;
                double ly = v.y * obj.scale;
                double lz = v.z * obj.scale;

                // Local continuous spins
                double rx1 = lx * cosLocalY - lz * sinLocalY;
                double rz1 = lx * sinLocalY + lz * cosLocalY;

                // Add physical translate offsets inside coordinates
                double wx = rx1 + obj.x;
                double wy = ly + obj.y;
                double wz = rz1 + obj.z;

                // Apply global camera viewing tilts
                double cx1 = wx * cosCamY - wz * sinCamY;
                double cz1 = wx * sinCamY + wz * cosCamY;

                double cy2 = wy * cosCamX - cz1 * sinCamX;
                double cz2 = wy * sinCamX + cz1 * cosCamX;

                obj.rotatedVertices[i].x = cx1;
                obj.rotatedVertices[i].y = cy2;
                obj.rotatedVertices[i].z = cz2;
            }

            // Estimate spatial depths of physical centers to calibrate click hits
            double ccx1 = obj.x * cosCamY - obj.z * sinCamY;
            double ccz1 = obj.x * sinCamY + obj.z * cosCamY;
            double ccy2 = obj.y * cosCamX - ccz1 * sinCamX;
            double ccz2 = obj.y * sinCamX + ccz1 * cosCamX;

            obj.rotatedCenterX = ccx1;
            obj.rotatedCenterY = ccy2;
            obj.rotatedCenterZ = ccz2;

            double szCenter = d / (d + obj.rotatedCenterZ);
            obj.projCenterX = (float) (centerX + obj.rotatedCenterX * szCenter * referenceScale);
            obj.projCenterY = (float) (centerY + obj.rotatedCenterY * szCenter * referenceScale);
            obj.projRadius = (float) (obj.hitRadius * szCenter * referenceScale);

            // Store individual faces for unified depth painter calculations
            for (Face f : obj.faces) {
                double sumZ = 0;
                for (int idx : f.indices) {
                    sumZ += obj.rotatedVertices[idx].z;
                }
                double avgZ = sumZ / f.indices.length;
                renderableFaces.add(new RenderableFace(obj, f, avgZ));
            }
        }

        // Step 2: Global Painter's Algorithm Depth-Sorting
        RenderableFace[] facesArr = renderableFaces.toArray(new RenderableFace[0]);
        Arrays.sort(facesArr, new Comparator<RenderableFace>() {
            @Override
            public int compare(RenderableFace f1, RenderableFace f2) {
                return Double.compare(f2.avgZ, f1.avgZ); // furthest drawn first
            }
        });

        // Step 3: Illumination shading models
        double lx = 0.58;
        double ly = -0.58;
        double lz = -0.58;
        double lenL = Math.sqrt(lx * lx + ly * ly + lz * lz);
        lx /= lenL;
        ly /= lenL;
        lz /= lenL;

        // Draw solid, flat-shaded polygon shapes
        for (RenderableFace rf : facesArr) {
            ThreeDObject obj = rf.parent;
            Face f = rf.face;

            Point3D pA = obj.rotatedVertices[f.indices[0]];
            Point3D pB = obj.rotatedVertices[f.indices[1]];
            Point3D pC = obj.rotatedVertices[f.indices[2]];

            // Generate cross product plane normals
            double ux = pB.x - pA.x;
            double uy = pB.y - pA.y;
            double uz = pB.z - pA.z;

            double vx = pC.x - pA.x;
            double vy = pC.y - pA.y;
            double vz = pC.z - pA.z;

            double nx = uy * vz - uz * vy;
            double ny = uz * vx - ux * vz;
            double nz = ux * vy - uy * vx;

            double lenN = Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (lenN > 0) {
                nx /= lenN;
                ny /= lenN;
                nz /= lenN;
            }

            // Perform backface culling to omit hidden polygon drawing
            if (nz >= 0) {
                continue;
            }

            // Flat Lambertian diffuse factor
            double dot = nx * lx + ny * ly + nz * lz;
            double intensity = 0.38 + 0.62 * Math.max(0.0, dot);

            int color = obj.baseColor;
            int r = (color >> 16) & 0xFF;
            int g = (color >> 8) & 0xFF;
            int b = color & 0xFF;

            int shadedR = Math.min(255, (int) (r * intensity));
            int shadedG = Math.min(255, (int) (g * intensity));
            int shadedB = Math.min(255, (int) (b * intensity));
            int faceColor = 0xFF000000 | (shadedR << 16) | (shadedG << 8) | shadedB;
            int strokeColor = 0x22FFFFFF | (shadedR << 16) | (shadedG << 8) | shadedB;

            paint.setColor(faceColor);
            paint.setStyle(Paint.Style.FILL);

            path.reset();
            for (int i = 0; i < f.indices.length; i++) {
                Point3D vertex = obj.rotatedVertices[f.indices[i]];
                double sz = d / (d + vertex.z);
                float px = (float) (centerX + vertex.x * sz * referenceScale);
                float py = (float) (centerY + vertex.y * sz * referenceScale);

                if (i == 0) {
                    path.moveTo(px, py);
                } else {
                    path.lineTo(px, py);
                }
            }
            path.close();
            canvas.drawPath(path, paint);

            // Highlight wireframe contours
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(strokeColor);
            paint.setStrokeWidth(2.2f);
            canvas.drawPath(path, paint);
        }

        // Draw projected floating 3D spark particles
        for (Particle p : particles) {
            double rx1 = p.x * cosCamY - p.z * sinCamY;
            double rz1 = p.x * sinCamY + p.z * cosCamY;
            double ry2 = p.y * cosCamX - rz1 * sinCamX;
            double rz2 = p.y * sinCamX + rz1 * cosCamX;

            double sz = d / (d + rz2);
            float px = (float) (centerX + rx1 * sz * referenceScale);
            float py = (float) (centerY + ry2 * sz * referenceScale);

            particlePaint.setColor(p.color);
            int alpha = (int) (255.0 * (1.0 - (double) p.age / p.maxAge));
            particlePaint.setAlpha(alpha);

            canvas.drawCircle(px, py, (float) (p.size * sz * referenceScale * 0.1), particlePaint);
        }

        // Step 4: Text overlay rendering mapped to projected coordinates
        for (ThreeDObject obj : objects) {
            if ("clicker".equals(obj.id)) {
                continue; // rotating click target has no core labels
            }

            double sz = d / (d + obj.rotatedCenterZ);
            float scaleFactor = (float) (sz * obj.scale);

            if ("scoreboard".equals(obj.id)) {
                textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

                // Title label
                textPaint.setColor(0xBB94A3B8);
                textPaint.setTextSize(10f * scaleFactor * getResources().getDisplayMetrics().density);
                canvas.drawText("CURRENT TALLY", obj.projCenterX, obj.projCenterY - 14f * scaleFactor, textPaint);

                // Main total count
                textPaint.setColor(0xFFFFFFFF);
                textPaint.setTextSize(34f * scaleFactor * getResources().getDisplayMetrics().density);
                canvas.drawText(String.valueOf(countValue), obj.projCenterX, obj.projCenterY + 14f * scaleFactor, textPaint);

                // Highest high score achieved
                textPaint.setColor(0xBB64748B);
                textPaint.setTextSize(11f * scaleFactor * getResources().getDisplayMetrics().density);
                canvas.drawText("PEAK HIGH: " + highScoreValue, obj.projCenterX, obj.projCenterY + 34f * scaleFactor, textPaint);

            } else if ("title_banner".equals(obj.id)) {
                textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                textPaint.setColor(0xFFF1F5F9);
                textPaint.setTextSize(13f * scaleFactor * getResources().getDisplayMetrics().density);
                canvas.drawText("SIMPLE CLICKER 3D", obj.projCenterX, obj.projCenterY + 5f * scaleFactor, textPaint);

            } else if (obj.label != null && !obj.label.isEmpty()) {
                textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                textPaint.setColor(0xFFFFFFFF);

                float baseSize = obj.label.length() > 6 ? 10f : 14f;
                textPaint.setTextSize(baseSize * scaleFactor * getResources().getDisplayMetrics().density);
                canvas.drawText(obj.label, obj.projCenterX, obj.projCenterY + (baseSize / 3f) * scaleFactor, textPaint);
            }
        }
    }
}