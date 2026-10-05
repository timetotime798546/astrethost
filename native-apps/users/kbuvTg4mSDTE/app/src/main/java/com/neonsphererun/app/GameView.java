package com.neonsphererun.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.util.ArrayList;
import java.util.List;

public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    // Gameplay states
    private static final int STATE_MENU = 0;
    private static final int STATE_PLAYING = 1;
    private static final int STATE_GAMEOVER = 2;

    private int gameState = STATE_MENU;

    // Render thread control
    private Thread gameThread;
    private boolean isRunning = false;
    private SurfaceHolder holder;

    // View bounds and calculation centers
    private int screenWidth = 0;
    private int screenHeight = 0;
    private float centerX = 0;
    private float centerY = 0;

    // 3D Engine Constants
    private final float fov = 380f; // Field of View projection constant
    private final float ROAD_WIDTH = 34f;

    // Dynamic camera coordinates
    private float cameraX = 0f;
    private final float cameraY = 7.5f; // elevated looking downwards
    private float cameraZ = 0f;

    // Player position parameters
    private float playerX = 0f;
    private float playerTargetX = 0f;
    private final float playerRadius = 2.4f;
    private float playerZ = 0f; // always offset slightly in front of camera

    // Speed progression
    private float currentSpeed = 12f;
    private final float baseSpeed = 12f;
    private final float maxSpeed = 45f;
    private float progress = 0f; // Cumulative distance

    // Touch movement sensitivity
    private float steeringMultiplier = 0.5f;

    // Procedural entities
    private final List<Star> stars = new ArrayList<Star>();
    private final List<Obstacle> obstacles = new ArrayList<Obstacle>();
    private final List<Particle> particles = new ArrayList<Particle>();
    private float lastSpawnZ = 0f;

    // Canvas Paints
    private Paint linePaint;
    private Paint fillPaint;
    private Paint starPaint;
    private Paint playerGlowPaint;

    // Activity binding interfaces
    private GameListener gameListener;

    public interface GameListener {
        void onScoreChanged(int score, float speedMultiplier);
        void onGameOver(int finalScore);
        void onGameStarted();
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        holder = getHolder();
        holder.addCallback(this);
        initPaints();
    }

    private void initPaints() {
        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setStyle(Paint.Style.STROKE);

        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setStyle(Paint.Style.FILL);

        starPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        
        playerGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    }

    public void setGameListener(GameListener listener) {
        this.gameListener = listener;
    }

    public void setSteeringSensitivity(float sensitivity) {
        this.steeringMultiplier = sensitivity;
    }

    public void startGame() {
        // Reset simulation variables
        progress = 0f;
        currentSpeed = baseSpeed;
        playerX = 0f;
        playerTargetX = 0f;
        cameraZ = -15f;
        playerZ = progress + 15f;

        // Populate space stars once
        stars.clear();
        for (int i = 0; i < 65; i++) {
            stars.add(new Star(
                -250f + (float) Math.random() * 500f,
                15f + (float) Math.random() * 150f,
                50f + (float) Math.random() * 400f
            ));
        }

        // Initialize hurdles
        obstacles.clear();
        particles.clear();
        lastSpawnZ = 120f;
        for (int i = 0; i < 4; i++) {
            spawnHurdlePattern();
        }

        gameState = STATE_PLAYING;
        if (gameListener != null) {
            gameListener.onGameStarted();
        }
    }

    private void spawnHurdlePattern() {
        float z = lastSpawnZ;
        // Cycle structural obstacle lanes randomly
        int type = (int) (Math.random() * 3);
        if (type == 0) {
            // Standard single middle-lane block
            float[] positions = {-11f, 0f, 11f};
            float pickX = positions[(int) (Math.random() * 3)];
            obstacles.add(new Obstacle(pickX, 0f, z, 9f, 9f, 9f, 0xFF00F3FF)); // Neon cyan
        } else if (type == 1) {
            // Block 2 lanes, player must shift fast to the remaining lane
            int clearLaneIndex = (int) (Math.random() * 3);
            if (clearLaneIndex != 0) {
                obstacles.add(new Obstacle(-11f, 0f, z, 9f, 9f, 9f, 0xFFFF007F)); // Neon pink
            }
            if (clearLaneIndex != 1) {
                obstacles.add(new Obstacle(0f, 0f, z, 9f, 9f, 9f, 0xFFFF007F));
            }
            if (clearLaneIndex != 2) {
                obstacles.add(new Obstacle(11f, 0f, z, 9f, 9f, 9f, 0xFFFF007F));
            }
        } else {
            // Flat wide lower barrier, can be dodged on the absolute left/right edges
            obstacles.add(new Obstacle(0f, 0f, z, 18f, 6f, 6f, 0xFF39FF14)); // Neon toxic green
        }

        // Incremental spacing of procedural stages
        lastSpawnZ += 95f + (float) Math.random() * 40f;
    }

    // Mathematical Perspective Projection models
    private static class Point3D {
        float x, y, z;
        Point3D(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    private static class Point2D {
        float x, y;
        boolean visible;
        Point2D(float x, float y, boolean visible) {
            this.x = x;
            this.y = y;
            this.visible = visible;
        }
    }

    private Point2D project(Point3D p) {
        float relX = p.x - cameraX;
        float relY = p.y - cameraY;
        float relZ = p.z - cameraZ;

        if (relZ <= 1.0f) {
            return new Point2D(0, 0, false);
        }

        float scale = fov / relZ;
        float screenX = centerX + relX * scale;
        float screenY = centerY - relY * scale;
        return new Point2D(screenX, screenY, true);
    }

    private void updateSimulation() {
        if (gameState != STATE_PLAYING) {
            // Just update visual effects when not active
            for (int i = 0; i < particles.size(); i++) {
                particles.get(i).update();
            }
            return;
        }

        // Steer sphere with interpolation lag
        float interpolationFactor = 0.1f + (steeringMultiplier * 0.15f);
        playerX += (playerTargetX - playerX) * interpolationFactor;

        // Progress distance metrics
        progress += currentSpeed * 0.05f;
        playerZ = progress + 15f;

        // Elastic camera tracking behind sphere
        cameraZ = progress - 12f;
        cameraX += (playerX * 0.75f - cameraX) * 0.1f;

        // Gradually increase track speed over time
        if (currentSpeed < maxSpeed) {
            currentSpeed += 0.005f;
        }

        // Inform activity layout of score milestones
        if (gameListener != null) {
            float speedRatio = currentSpeed / baseSpeed;
            gameListener.onScoreChanged((int) progress, speedRatio);
        }

        // Trigger procedural hurdle loop
        if (lastSpawnZ - progress < 350f) {
            spawnHurdlePattern();
        }

        // Filter and update game obstacles
        for (int i = obstacles.size() - 1; i >= 0; i--) {
            Obstacle o = obstacles.get(i);
            // Check if sphere cleared hurdle
            if (!o.passed && o.z < playerZ - 3f) {
                o.passed = true;
                SoundSynth.playMove(); // simple success trigger
            }

            // Remove distant past obstacles
            if (o.z < cameraZ) {
                obstacles.remove(i);
                continue;
            }

            // Collision check: 3D bounding box overlaps sphere bounding sphere
            if (Math.abs(o.z - playerZ) < (o.depth / 2f + playerRadius * 0.7f)) {
                if (Math.abs(o.x - playerX) < (o.width / 2f + playerRadius * 0.7f)) {
                    // Check if player Y is inside obstacle bounds
                    if (playerRadius < o.y + o.height) {
                        handleCrash();
                        return;
                    }
                }
            }
        }

        // Continuously generate speed dust particles
        if (Math.random() < 0.35) {
            particles.add(new Particle(
                -25f + (float) Math.random() * 50f,
                0f,
                progress + 200f,
                0f, 0f, 0f,
                0xFF00F3FF
            ));
        }

        // Update active movement particles
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.update();
            if (p.life <= 0 || p.z < cameraZ) {
                particles.remove(i);
            }
        }
    }

    private void handleCrash() {
        gameState = STATE_GAMEOVER;
        triggerExplosion();
        SoundSynth.playCrash();

        if (gameListener != null) {
            gameListener.onGameOver((int) progress);
        }
    }

    private void triggerExplosion() {
        for (int i = 0; i < 55; i++) {
            float angle = (float) (Math.random() * Math.PI * 2);
            float pitch = (float) (Math.random() * Math.PI - Math.PI / 2);
            float velocityStrength = 1.5f + (float) Math.random() * 6.5f;

            float vx = (float) (Math.cos(angle) * Math.cos(pitch)) * velocityStrength;
            float vy = (float) Math.sin(pitch) * velocityStrength + 3f; // upwards boost
            float vz = (float) (Math.sin(angle) * Math.cos(pitch)) * velocityStrength + currentSpeed * 0.1f;

            int color = (Math.random() > 0.5) ? 0xFFFF007F : 0xFF39FF14;
            particles.add(new Particle(playerX, playerRadius, playerZ, vx, vy, vz, color));
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (gameState != STATE_PLAYING) {
            return true;
        }

        float inputX = event.getX();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                // Map current screen touch ratio into physical track width space
                float fraction = inputX / screenWidth;
                float boundary = ROAD_WIDTH * 0.45f;
                playerTargetX = -boundary + fraction * (boundary * 2f);
                break;
        }
        return true;
    }

    private void drawGameFrame(Canvas canvas) {
        drawDarkSky(canvas);
        drawDistantMountains(canvas);
        drawRoadSystem(canvas);
        drawSimulationEntities(canvas);
        drawPlayerSphere(canvas);
    }

    private void drawDarkSky(Canvas canvas) {
        canvas.drawColor(0xFF060612); // Deep Synthwave Void

        // Project and render background star landscape
        starPaint.setStyle(Paint.Style.FILL);
        for (Star star : stars) {
            float relativeZ = star.z - cameraZ;
            // Wrap star positions continuously
            if (relativeZ <= 1.5f) {
                star.z = cameraZ + 380f + (float) Math.random() * 100f;
                star.x = -220f + (float) Math.random() * 440f;
                star.y = 15f + (float) Math.random() * 150f;
                relativeZ = star.z - cameraZ;
            }

            float scale = fov / relativeZ;
            float sx = centerX + star.x * scale;
            float sy = centerY - star.y * scale;

            if (sx >= 0 && sx <= screenWidth && sy >= 0 && sy <= screenHeight) {
                // Twinkle simulation
                int alpha = (int) (120 + 135 * Math.sin(star.twinkleOffset + progress * 0.04f));
                starPaint.setColor(0xFFFFFFFF);
                starPaint.setAlpha(alpha);
                canvas.drawCircle(sx, sy, star.size, starPaint);
            }
        }
    }

    private void drawDistantMountains(Canvas canvas) {
        Path mountainPath = new Path();
        float horizonY = centerY;
        mountainPath.moveTo(0, horizonY);
        mountainPath.lineTo(screenWidth * 0.18f, horizonY - 65f);
        mountainPath.lineTo(screenWidth * 0.35f, horizonY - 15f);
        mountainPath.lineTo(screenWidth * 0.52f, horizonY - 110f);
        mountainPath.lineTo(screenWidth * 0.68f, horizonY - 40f);
        mountainPath.lineTo(screenWidth * 0.83f, horizonY - 80f);
        mountainPath.lineTo(screenWidth, horizonY);

        // Neon outline double-draw for visual bloom glow effect
        linePaint.setColor(0xFFFF007F); // Pink Neon Glow
        linePaint.setAlpha(45);
        linePaint.setStrokeWidth(12f);
        canvas.drawPath(mountainPath, linePaint);

        linePaint.setAlpha(220);
        linePaint.setStrokeWidth(2.5f);
        canvas.drawPath(mountainPath, linePaint);
    }

    private void drawRoadSystem(Canvas canvas) {
        // Render road horizontal moving grid segments
        float zInterval = 14f;
        float offset = progress % zInterval;
        float segmentZ = progress - offset + 1.5f;

        for (float z = segmentZ; z < progress + 280f; z += zInterval) {
            Point2D p1 = project(new Point3D(-ROAD_WIDTH * 0.5f, 0f, z));
            Point2D p2 = project(new Point3D(ROAD_WIDTH * 0.5f, 0f, z));
            if (p1.visible && p2.visible) {
                drawGlowLine(canvas, p1.x, p1.y, p2.x, p2.y, 0xFFFF007F, 8f);
            }
        }

        // Render road boundary longitudinal tracks
        int segmentsCount = 20;
        float lenSegment = 15f;
        for (int i = 0; i < segmentsCount; i++) {
            float zStart = progress + i * lenSegment;
            float zEnd = zStart + lenSegment;

            // Left boundary segment
            Point2D leftStart = project(new Point3D(-ROAD_WIDTH * 0.5f, 0f, zStart));
            Point2D leftEnd = project(new Point3D(-ROAD_WIDTH * 0.5f, 0f, zEnd));
            if (leftStart.visible && leftEnd.visible) {
                drawGlowLine(canvas, leftStart.x, leftStart.y, leftEnd.x, leftEnd.y, 0xFF00F3FF, 10f);
            }

            // Right boundary segment
            Point2D rightStart = project(new Point3D(ROAD_WIDTH * 0.5f, 0f, zStart));
            Point2D rightEnd = project(new Point3D(ROAD_WIDTH * 0.5f, 0f, zEnd));
            if (rightStart.visible && rightEnd.visible) {
                drawGlowLine(canvas, rightStart.x, rightStart.y, rightEnd.x, rightEnd.y, 0xFF00F3FF, 10f);
            }

            // Lane markers
            Point2D dash1Start = project(new Point3D(-ROAD_WIDTH * 0.16f, 0f, zStart));
            Point2D dash1End = project(new Point3D(-ROAD_WIDTH * 0.16f, 0f, zStart + lenSegment * 0.5f));
            if (dash1Start.visible && dash1End.visible) {
                drawGlowLine(canvas, dash1Start.x, dash1Start.y, dash1End.x, dash1End.y, 0xFF00F3FF, 5f);
            }

            Point2D dash2Start = project(new Point3D(ROAD_WIDTH * 0.16f, 0f, zStart));
            Point2D dash2End = project(new Point3D(ROAD_WIDTH * 0.16f, 0f, zStart + lenSegment * 0.5f));
            if (dash2Start.visible && dash2End.visible) {
                drawGlowLine(canvas, dash2Start.x, dash2Start.y, dash2End.x, dash2End.y, 0xFF00F3FF, 5f);
            }
        }
    }

    private void drawSimulationEntities(Canvas canvas) {
        // Draw 3D wireframe hurdles
        for (int i = 0; i < obstacles.size(); i++) {
            Obstacle o = obstacles.get(i);
            drawVirtual3DBox(canvas, o.x, o.y, o.z, o.width, o.height, o.depth, o.color);
        }

        // Draw particle structures
        for (int i = 0; i < particles.size(); i++) {
            Particle p = particles.get(i);
            Point2D projected = project(new Point3D(p.x, p.y, p.z));
            if (projected.visible) {
                starPaint.setColor(p.color);
                float radius = p.size * (fov / (p.z - cameraZ));
                if (radius < 1.5f) radius = 1.5f;
                canvas.drawCircle(projected.x, projected.y, radius, starPaint);
            }
        }
    }

    private void drawPlayerSphere(Canvas canvas) {
        if (gameState == STATE_GAMEOVER) return;

        Point2D sCenter = project(new Point3D(playerX, playerRadius, playerZ));
        if (!sCenter.visible) return;

        float relativeZ = playerZ - cameraZ;
        float radius = playerRadius * (fov / relativeZ);

        // Render circular gradients glow
        playerGlowPaint.setColor(0xFF00F3FF);
        playerGlowPaint.setAlpha(45);
        playerGlowPaint.setStyle(Paint.Style.STROKE);
        playerGlowPaint.setStrokeWidth(12f);
        canvas.drawCircle(sCenter.x, sCenter.y, radius, playerGlowPaint);

        playerGlowPaint.setAlpha(255);
        playerGlowPaint.setStrokeWidth(3.5f);
        canvas.drawCircle(sCenter.x, sCenter.y, radius, playerGlowPaint);

        // Core fill layer
        fillPaint.setColor(0xFF00F3FF);
        fillPaint.setAlpha(35);
        canvas.drawCircle(sCenter.x, sCenter.y, radius, fillPaint);

        // Create rolling perspective illusion using mathematical longitude/latitude arcs
        float rollingAnimOffset = (progress * 1.5f) % (radius * 2f);
        canvas.save();
        
        // Clip rendering of ellipses inside the bounds of the sphere
        Path clipPath = new Path();
        clipPath.addCircle(sCenter.x, sCenter.y, radius, Path.Direction.CW);
        canvas.clipPath(clipPath);

        linePaint.setColor(0xFF00F3FF);
        linePaint.setAlpha(150);
        linePaint.setStrokeWidth(2f);

        // Draw latitude rolling wireframes
        for (float yVal = -radius + rollingAnimOffset; yVal < radius * 2f; yVal += radius * 0.45f) {
            float yPos = sCenter.y + yVal;
            if (yPos >= sCenter.y - radius && yPos <= sCenter.y + radius) {
                float horizontalWidth = (float) Math.sqrt(radius * radius - yVal * yVal);
                canvas.drawLine(sCenter.x - horizontalWidth, yPos, sCenter.x + horizontalWidth, yPos, linePaint);
            }
        }

        // Draw structural vertical wireframes
        for (float xOffset = -radius + radius * 0.3f; xOffset < radius; xOffset += radius * 0.5f) {
            float verticalHeight = (float) Math.sqrt(radius * radius - xOffset * xOffset);
            canvas.drawLine(sCenter.x + xOffset, sCenter.y - verticalHeight, sCenter.x + xOffset, sCenter.y + verticalHeight, linePaint);
        }

        canvas.restore();
    }

    private void drawVirtual3DBox(Canvas canvas, float x, float y, float z, float w, float h, float d, int color) {
        Point3D[] corners = new Point3D[8];
        corners[0] = new Point3D(x - w / 2, y, z - d / 2);
        corners[1] = new Point3D(x + w / 2, y, z - d / 2);
        corners[2] = new Point3D(x + w / 2, y + h, z - d / 2);
        corners[3] = new Point3D(x - w / 2, y + h, z - d / 2);
        corners[4] = new Point3D(x - w / 2, y, z + d / 2);
        corners[5] = new Point3D(x + w / 2, y, z + d / 2);
        corners[6] = new Point3D(x + w / 2, y + h, z + d / 2);
        corners[7] = new Point3D(x - w / 2, y + h, z + d / 2);

        Point2D[] screenPts = new Point2D[8];
        for (int i = 0; i < 8; i++) {
            screenPts[i] = project(corners[i]);
        }

        int[][] indices = {
            {0, 1}, {1, 2}, {2, 3}, {3, 0}, // Back side
            {4, 5}, {5, 6}, {6, 7}, {7, 4}, // Front side
            {0, 4}, {1, 5}, {2, 6}, {3, 7}  // Intersecting beams
        };

        // Draw translucent faces to improve 3D visual perception depth
        if (screenPts[4].visible && screenPts[5].visible && screenPts[6].visible && screenPts[7].visible) {
            Path frontFacePath = new Path();
            frontFacePath.moveTo(screenPts[4].x, screenPts[4].y);
            frontFacePath.lineTo(screenPts[5].x, screenPts[5].y);
            frontFacePath.lineTo(screenPts[6].x, screenPts[6].y);
            frontFacePath.lineTo(screenPts[7].x, screenPts[7].y);
            frontFacePath.close();

            fillPaint.setColor(color);
            fillPaint.setAlpha(40);
            canvas.drawPath(frontFacePath, fillPaint);
        }

        // Draw wireframe neon lines
        for (int[] line : indices) {
            Point2D ptA = screenPts[line[0]];
            Point2D ptB = screenPts[line[1]];
            if (ptA.visible && ptB.visible) {
                drawGlowLine(canvas, ptA.x, ptA.y, ptB.x, ptB.y, color, 8f);
            }
        }
    }

    private void drawGlowLine(Canvas canvas, float x1, float y1, float x2, float y2, int color, float thickness) {
        linePaint.setColor(color);
        // Step 1: Draw Thick outer low opacity glow channel
        linePaint.setAlpha(55);
        linePaint.setStrokeWidth(thickness);
        canvas.drawLine(x1, y1, x2, y2, linePaint);

        // Step 2: Draw thin intense inner core line
        linePaint.setAlpha(255);
        linePaint.setStrokeWidth(2.5f);
        canvas.drawLine(x1, y1, x2, y2, linePaint);
    }

    // SurfaceHolder Handlers
    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        resume();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
        screenWidth = width;
        screenHeight = height;
        centerX = width / 2f;
        centerY = height / 1.75f; // shift horizon down slightly for enhanced visibility
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        pause();
    }

    public void resume() {
        if (!isRunning) {
            isRunning = true;
            gameThread = new Thread(this);
            gameThread.start();
        }
    }

    public void pause() {
        isRunning = false;
        try {
            if (gameThread != null) {
                gameThread.join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (isRunning) {
            if (!holder.getSurface().isValid()) {
                continue;
            }

            long timeStart = System.currentTimeMillis();

            updateSimulation();

            Canvas canvas = null;
            try {
                canvas = holder.lockCanvas();
                if (canvas != null) {
                    drawGameFrame(canvas);
                }
            } finally {
                if (canvas != null) {
                    holder.unlockCanvasAndPost(canvas);
                }
            }

            // Sync frames at consistent 60FPS
            long timeDelta = System.currentTimeMillis() - timeStart;
            long timeTarget = 16 - timeDelta;
            if (timeTarget > 0) {
                try {
                    Thread.sleep(timeTarget);
                } catch (InterruptedException e) {
                    // Ignore interruption exceptions
                }
            }
        }
    }

    // Helper classes
    private static class Star {
        float x, y, z;
        float size;
        float twinkleOffset;

        Star(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.size = 1f + (float) Math.random() * 2f;
            this.twinkleOffset = (float) (Math.random() * Math.PI * 2);
        }
    }

    private static class Obstacle {
        float x, y, z;
        float width, height, depth;
        int color;
        boolean passed = false;

        Obstacle(float x, float y, float z, float w, float h, float d, int color) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.width = w;
            this.height = h;
            this.depth = d;
            this.color = color;
        }
    }

    private static class Particle {
        float x, y, z;
        float vx, vy, vz;
        int color;
        float life;
        float size;

        Particle(float x, float y, float z, float vx, float vy, float vz, int color) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.vx = vx;
            this.vy = vy;
            this.vz = vz;
            this.color = color;
            this.life = 25f + (float) Math.random() * 20f;
            this.size = 1f + (float) Math.random() * 2.5f;
        }

        void update() {
            x += vx;
            y += vy;
            z += vz;
            vy -= 0.12f; // simple gravity pull on explosion dust particles
            life -= 1.0f;
        }
    }
}