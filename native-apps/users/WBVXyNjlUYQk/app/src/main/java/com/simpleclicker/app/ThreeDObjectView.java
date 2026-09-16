package com.simpleclicker.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ThreeDObjectView extends View {

    private static class Vertex {
        float x, y, z;
        Vertex(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    private static class FaceSort implements Comparable<FaceSort> {
        int index;
        float avgZ;

        FaceSort(int index, float avgZ) {
            this.index = index;
            this.avgZ = avgZ;
        }

        @Override
        public int compareTo(FaceSort o) {
            return Float.compare(o.avgZ, this.avgZ);
        }
    }

    public interface OnThreeDClickListener {
        void onThreeDClick();
    }

    private OnThreeDClickListener clickListener;
    private int counterValue = 0;
    private boolean isNewHighScore = false;
    private int highScoreValue = 0;

    // 8 vertices of a standard 3D Cube
    private final Vertex[] vertices = new Vertex[] {
        new Vertex(-1f, -1f, -1f), // 0
        new Vertex( 1f, -1f, -1f), // 1
        new Vertex( 1f,  1f, -1f), // 2
        new Vertex(-1f,  1f, -1f), // 3
        new Vertex(-1f, -1f,  1f), // 4
        new Vertex( 1f, -1f,  1f), // 5
        new Vertex( 1f,  1f,  1f), // 6
        new Vertex(-1f,  1f,  1f)  // 7
    };

    // 6 faces mapped to start at Top-Left, then clockwise: TL, TR, BR, BL
    private static final int[][] FACES = new int[][] {
        {1, 0, 3, 2}, // Back face
        {5, 1, 2, 6}, // Right face
        {4, 5, 6, 7}, // Front face
        {0, 4, 7, 3}, // Left face
        {4, 5, 1, 0}, // Top face
        {3, 2, 6, 7}  // Bottom face
    };

    // Spin physics variables
    private float angleX = 0.45f;
    private float angleY = 0.55f;
    private float velX = 0.004f;
    private float velY = 0.007f;

    private boolean isDragging = false;
    private float lastX, lastY;
    private float startX, startY;
    private static final float CLICK_THRESHOLD = 15.0f;

    // Juicy Squeeze Animation spring-mass values
    private float scaleAnimation = 1.0f;
    private float scaleVelocity = 0.0f;

    public ThreeDObjectView(Context context) {
        super(context);
        init();
    }

    public ThreeDObjectView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ThreeDObjectView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setClickable(true);
    }

    public void setOnThreeDClickListener(OnThreeDClickListener listener) {
        this.clickListener = listener;
    }

    public void setCounterValue(int val) {
        this.counterValue = val;
        postInvalidate();
    }

    public void setHighScoreState(boolean isHighScore, int highScore) {
        this.isNewHighScore = isHighScore;
        this.highScoreValue = highScore;
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        // Spring physical calculations for scale squeeze recovery
        float k = 0.16f; // Stiffness coefficient
        float d = 0.80f; // Friction/Damping coefficient
        float force = (1.0f - scaleAnimation) * k;
        scaleVelocity += force;
        scaleVelocity *= d;
        scaleAnimation += scaleVelocity;

        // Auto constant idle-spin rotation when untouched
        if (!isDragging) {
            angleX += velX;
            angleY += velY;

            // Slowly decay custom kinetic velocity down to base speed
            velX *= 0.97f;
            velY *= 0.97f;

            float idleX = 0.003f;
            float idleY = 0.005f;

            velX += (idleX - velX) * 0.04f;
            velY += (idleY - velY) * 0.04f;
        }

        // Apply interactive scale to vertices & compute yaw/pitch rotations
        Vertex[] rot = new Vertex[8];
        for (int i = 0; i < 8; i++) {
            Vertex v = vertices[i];

            float rx = v.x * scaleAnimation;
            float ry = v.y * scaleAnimation;
            float rz = v.z * scaleAnimation;

            // Yaw Rotation Y
            float x1 = rx * (float) Math.cos(angleY) - rz * (float) Math.sin(angleY);
            float z1 = rx * (float) Math.sin(angleY) + rz * (float) Math.cos(angleY);

            // Pitch Rotation X
            float y2 = ry * (float) Math.cos(angleX) - z1 * (float) Math.sin(angleX);
            float z2 = ry * (float) Math.sin(angleX) + z1 * (float) Math.cos(angleX);

            rot[i] = new Vertex(x1, y2, z2);
        }

        float centerX = w / 2.0f;
        float centerY = h / 2.0f;
        float scale = Math.min(w, h) * 0.34f;

        float focalLength = 4.5f; // Perspective factor
        float[] px = new float[8];
        float[] py = new float[8];

        for (int i = 0; i < 8; i++) {
            Vertex v = rot[i];
            float perspective = focalLength / (focalLength + v.z);
            px[i] = centerX + v.x * perspective * scale;
            py[i] = centerY + v.y * perspective * scale;
        }

        // Painter's Algorithm sorting (back faces first, front closest faces last)
        List<FaceSort> sortedFaces = new ArrayList<FaceSort>();
        for (int i = 0; i < 6; i++) {
            int[] face = FACES[i];
            float avgZ = (rot[face[0]].z + rot[face[1]].z + rot[face[2]].z + rot[face[3]].z) / 4.0f;
            sortedFaces.add(new FaceSort(i, avgZ));
        }
        Collections.sort(sortedFaces);

        // Execute rendering cycle sequentially
        for (int i = 0; i < sortedFaces.size(); i++) {
            int faceIndex = sortedFaces.get(i).index;
            int[] face = FACES[faceIndex];

            // Real-time vector normal calculation for specular shader values
            Vertex A = rot[face[0]];
            Vertex B = rot[face[1]];
            Vertex C = rot[face[2]];

            float ux = B.x - A.x;
            float uy = B.y - A.y;
            float uz = B.z - A.z;

            float vx = C.x - A.x;
            float vy = C.y - A.y;
            float vz = C.z - A.z;

            float nx = uy * vz - uz * vy;
            float ny = uz * vx - ux * vz;
            float nz = ux * vy - uy * vx;

            float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (len > 0) {
                nx /= len;
                ny /= len;
                nz /= len;
            }

            // Simple ambient light vector direction mapping
            float lx = 0.577f;
            float ly = -0.577f;
            float lz = -0.577f;
            float dot = nx * lx + ny * ly + nz * lz;
            float intensity = (dot + 1.0f) * 0.5f; // Clamp lighting intensity strictly inside [0..1]

            // Perform homography matrix projection using setPolyToPoly
            Matrix matrix = new Matrix();
            float[] srcPoints = new float[] {
                0f, 0f,
                200f, 0f,
                200f, 200f,
                0f, 200f
            };
            float[] dstPoints = new float[] {
                px[face[0]], py[face[0]],
                px[face[1]], py[face[1]],
                px[face[2]], py[face[2]],
                px[face[3]], py[face[3]]
            };

            matrix.setPolyToPoly(srcPoints, 0, dstPoints, 0, 4);

            canvas.save();
            canvas.concat(matrix);

            // Establish color theme variables
            int baseR, baseG, baseB;
            if (isNewHighScore) {
                // Energetic Golden Bronze
                baseR = 245;
                baseG = 158;
                baseB = 11;
            } else if (counterValue == highScoreValue && highScoreValue > 0) {
                // Radiant Indigo Blue
                baseR = 37;
                baseG = 99;
                baseB = 235;
            } else if (counterValue < 0) {
                // Negative Ruby Alert
                baseR = 239;
                baseG = 68;
                baseB = 68;
            } else {
                // Standard Matte Slate
                baseR = 71;
                baseG = 85;
                baseB = 105;
            }

            // Apply calculated diffuse light values to colors
            int r = (int) (baseR * (0.40f + 0.60f * intensity));
            int g = (int) (baseG * (0.40f + 0.60f * intensity));
            int b = (int) (baseB * (0.40f + 0.60f * intensity));

            r = Math.max(0, Math.min(255, r));
            g = Math.max(0, Math.min(255, g));
            b = Math.max(0, Math.min(255, b));

            int faceColor = 0xFF000000 | (r << 16) | (g << 8) | b;

            Paint facePaint = new Paint();
            facePaint.setAntiAlias(true);
            facePaint.setColor(faceColor);
            facePaint.setStyle(Paint.Style.FILL);

            RectF faceRect = new RectF(2f, 2f, 198f, 198f);
            canvas.drawRoundRect(faceRect, 18f, 18f, facePaint);

            // Draw highlighting borders
            Paint borderPaint = new Paint();
            borderPaint.setAntiAlias(true);
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(4.0f);
            if (isNewHighScore) {
                borderPaint.setColor(0xEEFFFFFF);
            } else {
                borderPaint.setColor(0x7FEEF2F6);
            }
            canvas.drawRoundRect(faceRect, 18f, 18f, borderPaint);

            // Project crisp counts text values directly on the 3D surface
            Paint textPaint = new Paint();
            textPaint.setAntiAlias(true);
            textPaint.setFakeBoldText(true);
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setColor(0xFFFFFFFF);

            String text = String.valueOf(counterValue);
            float textSize = 70f;
            if (text.length() > 3) {
                textSize = 38f;
            } else if (text.length() > 2) {
                textSize = 50f;
            }
            textPaint.setTextSize(textSize);

            float textY = 100f - ((textPaint.descent() + textPaint.ascent()) / 2.0f);
            canvas.drawText(text, 100f, textY, textPaint);

            // Print decorative indicators on corners
            Paint decorPaint = new Paint();
            decorPaint.setAntiAlias(true);
            decorPaint.setColor(0x8CFFFFFF);
            decorPaint.setTextSize(16f);
            decorPaint.setTextAlign(Paint.Align.CENTER);

            String sym = getFaceSymbol(faceIndex);
            canvas.drawText(sym, 30f, 35f, decorPaint);
            canvas.drawText(sym, 170f, 180f, decorPaint);

            canvas.restore();
        }

        // Loop animation draw updates seamlessly
        postInvalidateOnAnimation();
    }

    private String getFaceSymbol(int index) {
        switch (index) {
            case 0: return "★";
            case 1: return "3D";
            case 2: return "🔥";
            case 3: return "💎";
            case 4: return "✦";
            case 5: return "🚀";
            default: return "●";
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isDragging = true;
                lastX = x;
                lastY = y;
                startX = x;
                startY = y;
                break;

            case MotionEvent.ACTION_MOVE:
                float dx = x - lastX;
                float dy = y - lastY;
                // Sensibly maps X-Y movements to corresponding Pitch and Yaw
                angleY += dx * 0.012f;
                angleX -= dy * 0.012f;
                lastX = x;
                lastY = y;
                postInvalidate();
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isDragging = false;
                float dist = (float) Math.sqrt((x - startX) * (x - startX) + (y - startY) * (y - startY));
                if (dist < CLICK_THRESHOLD) {
                    performClick();
                } else {
                    // Kinetic momentum transfer on drag release
                    velY = (x - lastX) * 0.09f;
                    velX = -(y - lastY) * 0.09f;
                }
                break;
        }
        return true;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        // Compresses scale along Z-space to visual bounce on tap
        scaleAnimation = 0.52f;
        scaleVelocity = 0.16f;

        // Visual spin momentum impulse on tap
        velY += 0.36f;
        velX += 0.14f;

        if (clickListener != null) {
            clickListener.onThreeDClick();
        }
        return true;
    }
}