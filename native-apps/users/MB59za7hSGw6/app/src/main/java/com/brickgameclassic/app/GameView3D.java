package com.brickgameclassic.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * High-performance 3D Arcade Viewport.
 * Projects the 10x20 well, 3D voxel cubes, transparent 3D glass cage,
 * directional lighting, 3D ghost shadow, and supports real-time touch orbital camera navigation.
 */
public class GameView3D extends View {

    private BrickGameEngine engine;

    // 3D Camera Angles and Projection parameters
    private float camPitch = 0.35f; // ~20 degrees elevation
    private float camYaw = -0.25f;  // ~14 degrees angle to left for 3D depth
    private float camDistance = 32.0f;
    private float focalLength = 700f;

    // Preset camera angles
    private int currentPresetIndex = 0;
    private static final float[][] PRESETS = {
        {0.35f, -0.28f}, // Classic 3D Isometric View
        {0.20f, 0.00f},  // Front-Center 3D View
        {0.65f, -0.35f}, // High-Angle Dramatic 3D
        {0.35f, 0.28f}   // Right Isometric 3D
    };

    // Touch interaction
    private float lastTouchX = 0f;
    private float lastTouchY = 0f;
    private boolean isDraggingCamera = false;

    // Paints
    private Paint fillPaint;
    private Paint strokePaint;
    private Paint cagePaint;
    private Paint cageFloorPaint;
    private Paint bgPaint;
    private Paint bannerBgPaint;
    private Paint bannerTextPaint;
    private Paint hudLabelPaint;
    private Paint hudValuePaint;

    // Directional light vector pointing from top-right-front
    private final Vector3D lightDir = new Vector3D(0.45f, -0.85f, -0.25f);

    // Reusable poly pool to minimize GC allocations during 60FPS draw loops
    private final List<PolyFace3D> polyList = new ArrayList<PolyFace3D>(512);
    private int polyPoolIndex = 0;

    public GameView3D(Context context) {
        super(context);
        init();
    }

    public GameView3D(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GameView3D(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setStyle(Paint.Style.FILL);

        strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(1.5f);

        cagePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cagePaint.setStyle(Paint.Style.STROKE);
        cagePaint.setColor(0x5500E5FF);
        cagePaint.setStrokeWidth(1.5f);

        cageFloorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cageFloorPaint.setStyle(Paint.Style.FILL);
        cageFloorPaint.setColor(0x2200E5FF);

        bgPaint = new Paint();
        bgPaint.setColor(0xFF090B10);

        bannerBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bannerBgPaint.setColor(0xDD000000);
        bannerBgPaint.setStyle(Paint.Style.FILL);

        bannerTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bannerTextPaint.setColor(0xFF00E5FF);
        bannerTextPaint.setTextSize(36f);
        bannerTextPaint.setFakeBoldText(true);
        bannerTextPaint.setTextAlign(Paint.Align.CENTER);

        hudLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        hudLabelPaint.setColor(0xFF90A4AE);
        hudLabelPaint.setTextSize(20f);
        hudLabelPaint.setFakeBoldText(true);

        hudValuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        hudValuePaint.setColor(0xFF00E5FF);
        hudValuePaint.setTextSize(26f);
        hudValuePaint.setFakeBoldText(true);

        // Preallocate reusable faces
        for (int i = 0; i < 700; i++) {
            polyList.add(new PolyFace3D());
        }
    }

    public void setEngine(BrickGameEngine engine) {
        this.engine = engine;
        invalidate();
    }

    public void cycleCameraPreset() {
        currentPresetIndex = (currentPresetIndex + 1) % PRESETS.length;
        camPitch = PRESETS[currentPresetIndex][0];
        camYaw = PRESETS[currentPresetIndex][1];
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = event.getX();
                lastTouchY = event.getY();
                isDraggingCamera = true;
                return true;

            case MotionEvent.ACTION_MOVE:
                if (isDraggingCamera) {
                    float dx = event.getX() - lastTouchX;
                    float dy = event.getY() - lastTouchY;

                    camYaw += dx * 0.007f;
                    camPitch += dy * 0.007f;

                    // Clamp pitch so cage stays upright
                    if (camPitch < 0.05f) camPitch = 0.05f;
                    if (camPitch > 1.25f) camPitch = 1.25f;

                    lastTouchX = event.getX();
                    lastTouchY = event.getY();
                    invalidate();
                    return true;
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isDraggingCamera = false;
                return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) return;

        // Render deep viewport gradient/background
        canvas.drawRect(0, 0, width, height, bgPaint);

        float centerX = width * 0.50f;
        float centerY = height * 0.48f;
        focalLength = Math.min(width, height) * 1.55f;

        polyPoolIndex = 0;

        // 1. Build 3D Glass Well Cage
        buildGlassCage3D();

        // 2. Build 3D Ghost Piece
        if (engine != null && engine.isRunning() && !engine.isGameOver()) {
            buildGhostPiece3D();
        }

        // 3. Build Placed 3D Blocks from Board
        if (engine != null) {
            int[][] board = engine.getBoard();
            for (int r = 0; r < BrickGameEngine.ROWS; r++) {
                for (int c = 0; c < BrickGameEngine.COLS; c++) {
                    int color = board[r][c];
                    if (color != 0) {
                        buildCube3D(c, r, color, false);
                    }
                }
            }
        }

        // 4. Build Falling Active 3D Tetromino
        if (engine != null && engine.isRunning() && !engine.isGameOver()) {
            int type = engine.getCurType();
            int rot = engine.getCurRotation();
            int curX = engine.getCurX();
            int curY = engine.getCurY();
            int color = Tetromino.getColor(type);
            int[][] matrix = Tetromino.getMatrix(type, rot);

            for (int r = 0; r < 4; r++) {
                for (int c = 0; c < 4; c++) {
                    if (matrix[r][c] == 1) {
                        int bX = curX + c;
                        int bY = curY + r;
                        if (bY >= 0 && bY < BrickGameEngine.ROWS && bX >= 0 && bX < BrickGameEngine.COLS) {
                            buildCube3D(bX, bY, color, false);
                        }
                    }
                }
            }
        }

        // 5. Transform all polygons by Camera rotation & project to 2D
        List<PolyFace3D> activePolys = new ArrayList<PolyFace3D>(polyPoolIndex);
        for (int i = 0; i < polyPoolIndex; i++) {
            PolyFace3D face = polyList.get(i);
            boolean valid = transformAndProjectFace(face, centerX, centerY);
            if (valid) {
                face.computeAvgZ();
                activePolys.add(face);
            }
        }

        // 6. Sort faces with Painter's Algorithm (Furthest to Nearest)
        Collections.sort(activePolys);

        // 7. Rasterize 3D Polygons
        for (int i = 0; i < activePolys.size(); i++) {
            PolyFace3D face = activePolys.get(i);
            face.path.reset();
            face.path.moveTo(face.screenX[0], face.screenY[0]);
            face.path.lineTo(face.screenX[1], face.screenY[1]);
            face.path.lineTo(face.screenX[2], face.screenY[2]);
            face.path.lineTo(face.screenX[3], face.screenY[3]);
            face.path.close();

            if (!face.isWireframeOnly) {
                int shadedColor = applyLighting(face.color, face.lightingMultiplier);
                fillPaint.setColor(shadedColor);
                canvas.drawPath(face.path, fillPaint);

                strokePaint.setColor(face.strokeColor);
                canvas.drawPath(face.path, strokePaint);
            } else {
                strokePaint.setColor(face.strokeColor);
                canvas.drawPath(face.path, strokePaint);
            }
        }

        // 8. Draw Next Piece HUD Preview (Top-Right)
        drawNextPreview(canvas, width - 110f, 25f);

        // 9. Overlay Pause / Game Over message
        if (engine != null) {
            if (engine.isPaused()) {
                drawBanner(canvas, centerX, centerY, "PAUSED");
            } else if (engine.isGameOver() && !engine.isRunning()) {
                drawBanner(canvas, centerX, centerY, "GAME OVER");
            }
        }
    }

    private void buildGlassCage3D() {
        float xMin = -5.0f;
        float xMax = 5.0f;
        float yMin = -10.0f;
        float yMax = 10.0f;
        float zBack = -0.55f;
        float zFront = 0.55f;

        // Floor Grid Lines (y = yMax)
        for (int c = 0; c <= BrickGameEngine.COLS; c++) {
            float x = xMin + c;
            addSegmentFace(x, yMax, zBack, x, yMax, zFront, 0x4400E5FF);
        }
        for (float z = zBack; z <= zFront + 0.1f; z += (zFront - zBack)) {
            addSegmentFace(xMin, yMax, z, xMax, yMax, z, 0x4400E5FF);
        }

        // Back Wall Wireframe Grid
        for (int r = 0; r <= BrickGameEngine.ROWS; r += 2) {
            float y = yMin + r;
            addSegmentFace(xMin, y, zBack, xMax, y, zBack, 0x2200E5FF);
        }
        for (int c = 0; c <= BrickGameEngine.COLS; c += 2) {
            float x = xMin + c;
            addSegmentFace(x, yMin, zBack, x, yMax, zBack, 0x2200E5FF);
        }

        // 4 Main 3D Corner Pillars
        addSegmentFace(xMin, yMin, zBack, xMin, yMax, zBack, 0x8800E5FF);
        addSegmentFace(xMax, yMin, zBack, xMax, yMax, zBack, 0x8800E5FF);
        addSegmentFace(xMin, yMin, zFront, xMin, yMax, zFront, 0x8800E5FF);
        addSegmentFace(xMax, yMin, zFront, xMax, yMax, zFront, 0x8800E5FF);
    }

    private void buildGhostPiece3D() {
        int ghostY = engine.getGhostY();
        if (ghostY <= engine.getCurY()) return; // Already on floor

        int type = engine.getCurType();
        int rot = engine.getCurRotation();
        int curX = engine.getCurX();
        int[][] matrix = Tetromino.getMatrix(type, rot);

        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                if (matrix[r][c] == 1) {
                    int bX = curX + c;
                    int bY = ghostY + r;
                    if (bY >= 0 && bY < BrickGameEngine.ROWS && bX >= 0 && bX < BrickGameEngine.COLS) {
                        buildCube3D(bX, bY, 0x3300E5FF, true);
                    }
                }
            }
        }
    }

    private void buildCube3D(int gridX, int gridY, int color, boolean isGhost) {
        float x0 = gridX - 5.0f;
        float x1 = x0 + 0.94f;
        float y0 = gridY - 10.0f;
        float y1 = y0 + 0.94f;
        float z0 = -0.47f;
        float z1 = 0.47f;

        int strokeCol = isGhost ? 0x6600E5FF : 0xAAFFFFFF;

        // Front Face (z1)
        addQuad(x0, y0, z1,  x1, y0, z1,  x1, y1, z1,  x0, y1, z1, color, strokeCol, isGhost, 0.95f);
        // Top Face (y0)
        addQuad(x0, y0, z0,  x1, y0, z0,  x1, y0, z1,  x0, y0, z1, color, strokeCol, isGhost, 1.15f);
        // Right Face (x1)
        addQuad(x1, y0, z1,  x1, y0, z0,  x1, y1, z0,  x1, y1, z1, color, strokeCol, isGhost, 0.80f);
        // Left Face (x0)
        addQuad(x0, y0, z0,  x0, y0, z1,  x0, y1, z1,  x0, y1, z0, color, strokeCol, isGhost, 0.70f);
        // Bottom Face (y1)
        addQuad(x0, y1, z1,  x1, y1, z1,  x1, y1, z0,  x0, y1, z0, color, strokeCol, isGhost, 0.55f);
        // Back Face (z0)
        addQuad(x1, y0, z0,  x0, y0, z0,  x0, y1, z0,  x1, y1, z0, color, strokeCol, isGhost, 0.60f);
    }

    private void addQuad(float x0, float y0, float z0,
                         float x1, float y1, float z1,
                         float x2, float y2, float z2,
                         float x3, float y3, float z3,
                         int color, int strokeColor, boolean isGhost, float lightMult) {

        if (polyPoolIndex >= polyList.size()) return;
        PolyFace3D face = polyList.get(polyPoolIndex++);
        face.verts[0].set(x0, y0, z0);
        face.verts[1].set(x1, y1, z1);
        face.verts[2].set(x2, y2, z2);
        face.verts[3].set(x3, y3, z3);

        face.color = color;
        face.strokeColor = strokeColor;
        face.isWireframeOnly = isGhost;
        face.lightingMultiplier = lightMult;
    }

    private void addSegmentFace(float x0, float y0, float z0, float x1, float y1, float z1, int strokeColor) {
        if (polyPoolIndex >= polyList.size()) return;
        PolyFace3D face = polyList.get(polyPoolIndex++);
        face.verts[0].set(x0, y0, z0);
        face.verts[1].set(x1, y1, z1);
        face.verts[2].set(x1, y1, z1);
        face.verts[3].set(x0, y0, z0);

        face.color = 0;
        face.strokeColor = strokeColor;
        face.isWireframeOnly = true;
        face.lightingMultiplier = 1.0f;
    }

    private boolean transformAndProjectFace(PolyFace3D face, float cx, float cy) {
        for (int i = 0; i < 4; i++) {
            Vector3D v = face.verts[i];
            // Rotate around Y axis (Yaw)
            v.rotateY(camYaw);
            // Rotate around X axis (Pitch)
            v.rotateX(camPitch);

            // Translate into camera view space along Z
            float vz = v.z + camDistance;
            if (vz <= 0.5f) {
                return false; // Point is behind the camera
            }

            // Perspective division
            face.screenX[i] = cx + (v.x / vz) * focalLength;
            face.screenY[i] = cy + (v.y / vz) * focalLength;
        }

        // Back-face culling for solid filled faces
        if (!face.isWireframeOnly) {
            float edgeX1 = face.screenX[1] - face.screenX[0];
            float edgeY1 = face.screenY[1] - face.screenY[0];
            float edgeX2 = face.screenX[2] - face.screenX[1];
            float edgeY2 = face.screenY[2] - face.screenY[1];

            // 2D cross-product determines clockwise vs counter-clockwise winding
            float cross = edgeX1 * edgeY2 - edgeY1 * edgeX2;
            if (cross <= 0.0f) {
                return false; // Facing away from viewer
            }
        }

        return true;
    }

    private int applyLighting(int baseColor, float multiplier) {
        int a = Color.alpha(baseColor);
        int r = Math.min(255, (int) (Color.red(baseColor) * multiplier));
        int g = Math.min(255, (int) (Color.green(baseColor) * multiplier));
        int b = Math.min(255, (int) (Color.blue(baseColor) * multiplier));
        return Color.argb(a, r, g, b);
    }

    private void drawNextPreview(Canvas canvas, float right, float top) {
        canvas.drawText("NEXT", right, top + 18f, hudLabelPaint);

        if (engine == null) return;
        int nextType = engine.getNextType();
        int color = Tetromino.getColor(nextType);
        int[][] matrix = Tetromino.getMatrix(nextType, 0);

        float blockSize = 14f;
        float startX = right - 10f;
        float startY = top + 26f;

        fillPaint.setColor(color);
        strokePaint.setColor(0x88FFFFFF);

        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                if (matrix[r][c] == 1) {
                    float px = startX + c * blockSize;
                    float py = startY + r * blockSize;
                    canvas.drawRect(px, py, px + blockSize - 2, py + blockSize - 2, fillPaint);
                    canvas.drawRect(px, py, px + blockSize - 2, py + blockSize - 2, strokePaint);
                }
            }
        }
    }

    private void drawBanner(Canvas canvas, float centerX, float centerY, String text) {
        float bannerWidth = getWidth() * 0.70f;
        float bannerHeight = 72f;
        float left = centerX - (bannerWidth / 2f);
        float top = centerY - (bannerHeight / 2f);

        RectF bannerRect = new RectF(left, top, left + bannerWidth, top + bannerHeight);
        canvas.drawRoundRect(bannerRect, 16f, 16f, bannerBgPaint);

        strokePaint.setColor(0xFF00E5FF);
        strokePaint.setStrokeWidth(2f);
        canvas.drawRoundRect(bannerRect, 16f, 16f, strokePaint);

        canvas.drawText(text, centerX, centerY + 12f, bannerTextPaint);
    }
}