package com.runner3d.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    public interface GameStateListener {
        void onScoreUpdated(int distanceMeters, int gems, float speedMultiplier);
        void onGameOver(int finalDistance, int finalGems);
    }

    private GameStateListener gameStateListener;
    private SoundEffectManager soundManager;
    private SurfaceHolder surfaceHolder;
    private Thread gameThread;
    private volatile boolean isRunning = false;
    private volatile boolean isPlaying = false;

    // 3D Engine Constants
    private static final float FOV = 400.0f;
    private static final float ROAD_WIDTH = 600.0f;
    private static final float LANE_OFFSET = 200.0f; // -1: -200, 0: 0, 1: 200
    private static final float VIEW_DISTANCE = 1600.0f;
    private static final float SEGMENT_LENGTH = 100.0f;

    // Colors
    private static final int COLOR_SKY_TOP = Color.parseColor("#060614");
    private static final int COLOR_SKY_BOTTOM = Color.parseColor("#140828");
    private static final int COLOR_ROAD_A = Color.parseColor("#15152A");
    private static final int COLOR_ROAD_B = Color.parseColor("#1F1F3D");
    private static final int COLOR_BORDER_A = Color.parseColor("#00E5FF");
    private static final int COLOR_BORDER_B = Color.parseColor("#FF007F");
    private static final int COLOR_LANE_LINE = Color.parseColor("#4466AA");

    // Player State
    private float playerX = 0f;
    private float targetX = 0f;
    private int currentLane = 0; // -1 = Left, 0 = Center, 1 = Right
    private float playerY = 0f;  // Jump altitude
    private float playerVelocityY = 0f;
    private boolean isJumping = false;
    private boolean isSliding = false;
    private int slideTimer = 0;
    private float runAnimCycle = 0f;

    // World & Movement
    private float worldZ = 0f;
    private float forwardSpeed = 380.0f; // units per second
    private int scoreDistance = 0;
    private int gemsCollected = 0;

    // Obstacles and Collectibles
    public static class WorldObject {
        public static final int TYPE_HURDLE = 0;   // Low barrier (jump over)
        public static final int TYPE_LASER = 1;    // High beam (slide under)
        public static final int TYPE_BLOCK = 2;    // Tall block (must switch lanes)
        public static final int TYPE_GEM = 3;      // Collectible energy diamond

        public int type;
        public int lane; // -1, 0, 1
        public float z;
        public boolean collected = false;
        public float animRotation = 0f;

        public WorldObject(int type, int lane, float z) {
            this.type = type;
            this.lane = lane;
            this.z = z;
        }
    }

    // Particle Burst System
    public static class Particle {
        public float x, y, z;
        public float vx, vy, vz;
        public int color;
        public int life;
        public int maxLife;

        public Particle(float x, float y, float z, float vx, float vy, float vz, int color, int life) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.vx = vx;
            this.vy = vy;
            this.vz = vz;
            this.color = color;
            this.life = life;
            this.maxLife = life;
        }
    }

    private final List<WorldObject> worldObjects = new ArrayList<WorldObject>();
    private final List<Particle> particles = new ArrayList<Particle>();
    private final Random random = new Random();

    // Rendering Paints
    private Paint roadPaint;
    private Paint linePaint;
    private Paint playerPaint;
    private Paint hazardPaint;
    private Paint gemPaint;
    private Paint particlePaint;
    private Paint horizonGridPaint;
    private Path renderPath;

    // Gesture Detection
    private float touchStartX = 0;
    private float touchStartY = 0;
    private static final int SWIPE_THRESHOLD = 50;

    public GameView(Context context) {
        super(context);
        init();
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);
        soundManager = new SoundEffectManager();

        roadPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setStrokeWidth(3f);

        playerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        hazardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gemPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        particlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        horizonGridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        horizonGridPaint.setColor(Color.parseColor("#3300E5FF"));
        horizonGridPaint.setStrokeWidth(1.5f);

        renderPath = new Path();
        setFocusable(true);
    }

    public void setGameStateListener(GameStateListener listener) {
        this.gameStateListener = listener;
    }

    public void startNewGame() {
        worldObjects.clear();
        particles.clear();
        currentLane = 0;
        playerX = 0f;
        targetX = 0f;
        playerY = 0f;
        playerVelocityY = 0f;
        isJumping = false;
        isSliding = false;
        slideTimer = 0;
        worldZ = 0f;
        forwardSpeed = 380.0f;
        scoreDistance = 0;
        gemsCollected = 0;

        // Pre-populate initial road objects
        float spawnZ = 600f;
        for (int i = 0; i < 16; i++) {
            spawnZ += 280f + random.nextInt(180);
            spawnDynamicBatch(spawnZ);
        }

        isPlaying = true;
    }

    private void spawnDynamicBatch(float z) {
        int pattern = random.nextInt(4);
        int freeLane = random.nextInt(3) - 1; // At least one clear lane

        if (pattern == 0) {
            // Hurdle in one or two lanes
            int lane = random.nextInt(3) - 1;
            worldObjects.add(new WorldObject(WorldObject.TYPE_HURDLE, lane, z));
            // Add gem in clear lane
            worldObjects.add(new WorldObject(WorldObject.TYPE_GEM, freeLane, z + 70f));
        } else if (pattern == 1) {
            // High Laser across 2 lanes
            for (int l = -1; l <= 1; l++) {
                if (l != freeLane) {
                    worldObjects.add(new WorldObject(WorldObject.TYPE_LASER, l, z));
                }
            }
            worldObjects.add(new WorldObject(WorldObject.TYPE_GEM, freeLane, z));
        } else if (pattern == 2) {
            // Tall Hazard Block
            int lane = random.nextInt(3) - 1;
            worldObjects.add(new WorldObject(WorldObject.TYPE_BLOCK, lane, z));
            worldObjects.add(new WorldObject(WorldObject.TYPE_GEM, (lane == 0) ? 1 : 0, z));
        } else {
            // Line of 3 Gems in single lane
            int gemLane = random.nextInt(3) - 1;
            worldObjects.add(new WorldObject(WorldObject.TYPE_GEM, gemLane, z));
            worldObjects.add(new WorldObject(WorldObject.TYPE_GEM, gemLane, z + 70f));
            worldObjects.add(new WorldObject(WorldObject.TYPE_GEM, gemLane, z + 140f));
        }
    }

    public void moveLeft() {
        if (!isPlaying) return;
        if (currentLane > -1) {
            currentLane--;
            targetX = currentLane * LANE_OFFSET;
            soundManager.playSwitchLane();
        }
    }

    public void moveRight() {
        if (!isPlaying) return;
        if (currentLane < 1) {
            currentLane++;
            targetX = currentLane * LANE_OFFSET;
            soundManager.playSwitchLane();
        }
    }

    public void jump() {
        if (!isPlaying) return;
        if (!isJumping && !isSliding) {
            isJumping = true;
            playerVelocityY = 480.0f;
            soundManager.playJump();
        }
    }

    public void slide() {
        if (!isPlaying) return;
        if (isJumping) {
            // Quick descent from jump
            playerVelocityY = -600.0f;
        } else if (!isSliding) {
            isSliding = true;
            slideTimer = 35; // frames
            soundManager.playSlide();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchStartX = event.getX();
                touchStartY = event.getY();
                return true;

            case MotionEvent.ACTION_UP:
                float deltaX = event.getX() - touchStartX;
                float deltaY = event.getY() - touchStartY;

                if (Math.abs(deltaX) > Math.abs(deltaY)) {
                    if (Math.abs(deltaX) > SWIPE_THRESHOLD) {
                        if (deltaX > 0) {
                            moveRight();
                        } else {
                            moveLeft();
                        }
                    }
                } else {
                    if (Math.abs(deltaY) > SWIPE_THRESHOLD) {
                        if (deltaY < 0) {
                            jump();
                        } else {
                            slide();
                        }
                    }
                }
                return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        isRunning = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry = true;
        isRunning = false;
        while (retry) {
            try {
                if (gameThread != null) {
                    gameThread.join();
                }
                retry = false;
            } catch (InterruptedException ignored) {
            }
        }
    }

    @Override
    public void run() {
        long lastTime = System.currentTimeMillis();

        while (isRunning) {
            long now = System.currentTimeMillis();
            float dt = (now - lastTime) / 1000.0f;
            if (dt > 0.1f) dt = 0.1f; // Clamp frame lag
            lastTime = now;

            if (isPlaying) {
                updateGame(dt);
            }

            renderGame();

            try {
                Thread.sleep(16); // Target ~60 FPS
            } catch (InterruptedException ignored) {
            }
        }
    }

    private void updateGame(float dt) {
        // Forward progression & gradual speed ramp
        worldZ += forwardSpeed * dt;
        forwardSpeed += 2.2f * dt;
        scoreDistance = (int) (worldZ / 10.0f);

        // Player lateral interpolation
        playerX += (targetX - playerX) * 14.0f * dt;

        // Player Jump Kinematics
        if (isJumping) {
            playerY += playerVelocityY * dt;
            playerVelocityY -= 1100.0f * dt; // Gravity
            if (playerY <= 0) {
                playerY = 0f;
                isJumping = false;
                playerVelocityY = 0f;
            }
        }

        // Player Slide Timer
        if (isSliding) {
            slideTimer--;
            if (slideTimer <= 0) {
                isSliding = false;
            }
        }

        // Run animation cycle
        runAnimCycle += forwardSpeed * dt * 0.03f;

        // Spawn new world segments ahead
        float maxZ = 0f;
        for (int i = 0; i < worldObjects.size(); i++) {
            if (worldObjects.get(i).z > maxZ) {
                maxZ = worldObjects.get(i).z;
            }
        }
        if (maxZ - worldZ < VIEW_DISTANCE) {
            spawnDynamicBatch(Math.max(worldZ + VIEW_DISTANCE, maxZ + 260f));
        }

        // Collision & Object Updates
        float playerZPos = worldZ + 120.0f; // Player 3D anchor
        float playerCollisionRadius = 45.0f;

        Iterator<WorldObject> it = worldObjects.iterator();
        while (it.hasNext()) {
            WorldObject obj = it.next();
            obj.animRotation += 90.0f * dt;

            // Remove objects behind camera
            if (obj.z < worldZ - 100.0f) {
                it.remove();
                continue;
            }

            // Check collision with player
            float zDiff = Math.abs(obj.z - playerZPos);
            float objectX = obj.lane * LANE_OFFSET;
            float xDiff = Math.abs(objectX - playerX);

            if (zDiff < 45.0f && xDiff < 75.0f && !obj.collected) {
                if (obj.type == WorldObject.TYPE_GEM) {
                    obj.collected = true;
                    gemsCollected++;
                    soundManager.playCoin();
                    spawnGemBurst(objectX, 40f, obj.z);
                    it.remove();
                } else if (obj.type == WorldObject.TYPE_HURDLE) {
                    // Must jump over hurdle (need altitude > 50)
                    if (playerY < 50.0f) {
                        triggerGameOver();
                        return;
                    }
                } else if (obj.type == WorldObject.TYPE_LASER) {
                    // Must slide under laser (need sliding crouching state)
                    if (!isSliding) {
                        triggerGameOver();
                        return;
                    }
                } else if (obj.type == WorldObject.TYPE_BLOCK) {
                    // Impassable block in lane
                    triggerGameOver();
                    return;
                }
            }
        }

        // Update Particle Burst System
        Iterator<Particle> pIt = particles.iterator();
        while (pIt.hasNext()) {
            Particle p = pIt.next();
            p.x += p.vx * dt;
            p.y += p.vy * dt;
            p.z += p.vz * dt;
            p.life--;
            if (p.life <= 0) {
                pIt.remove();
            }
        }

        // Notify UI listener
        if (gameStateListener != null) {
            float speedMult = forwardSpeed / 380.0f;
            gameStateListener.onScoreUpdated(scoreDistance, gemsCollected, speedMult);
        }
    }

    private void triggerGameOver() {
        isPlaying = false;
        soundManager.playCrash();
        spawnCrashBurst(playerX, playerY + 30f, worldZ + 120.0f);

        if (gameStateListener != null) {
            gameStateListener.onGameOver(scoreDistance, gemsCollected);
        }
    }

    private void spawnGemBurst(float x, float y, float z) {
        for (int i = 0; i < 18; i++) {
            float vx = (random.nextFloat() - 0.5f) * 350f;
            float vy = random.nextFloat() * 400f + 100f;
            float vz = (random.nextFloat() - 0.5f) * 200f;
            particles.add(new Particle(x, y, z, vx, vy, vz, Color.parseColor("#FFD700"), 30));
        }
    }

    private void spawnCrashBurst(float x, float y, float z) {
        for (int i = 0; i < 35; i++) {
            float vx = (random.nextFloat() - 0.5f) * 600f;
            float vy = random.nextFloat() * 700f;
            float vz = (random.nextFloat() - 0.5f) * 500f;
            int color = (i % 2 == 0) ? Color.parseColor("#FF007F") : Color.parseColor("#00E5FF");
            particles.add(new Particle(x, y, z, vx, vy, vz, color, 45));
        }
    }

    // 3D Perspective Projection Math
    private static class Point2D {
        float x, y, scale;
    }

    private boolean project(float x, float y, float z, float viewCenterX, float horizonY, Point2D out) {
        float relZ = z - worldZ;
        if (relZ <= 10.0f) {
            return false;
        }

        float scale = FOV / relZ;
        out.x = viewCenterX + (x * scale);
        out.y = horizonY - (y * scale);
        out.scale = scale;
        return true;
    }

    private void renderGame() {
        Canvas canvas = surfaceHolder.lockCanvas();
        if (canvas == null) return;

        try {
            int width = getWidth();
            int height = getHeight();
            if (width <= 0 || height <= 0) return;

            float viewCenterX = width * 0.5f;
            float horizonY = height * 0.38f;

            // 1. Draw Cyber Sky & Sci-Fi Background Gradient
            drawSky(canvas, width, height, horizonY);

            // 2. Draw 3D Ground and Highway Road Segments
            drawRoad(canvas, width, height, viewCenterX, horizonY);

            // 3. Draw 3D Road Objects (Obstacles & Gems) Depth-Sorted
            drawWorldObjects(canvas, viewCenterX, horizonY);

            // 4. Draw 3D Running Avatar Character
            drawPlayer(canvas, viewCenterX, horizonY);

            // 5. Draw 3D Particle Bursts
            drawParticles(canvas, viewCenterX, horizonY);

        } finally {
            surfaceHolder.unlockCanvasAndPost(canvas);
        }
    }

    private void drawSky(Canvas canvas, int width, int height, float horizonY) {
        LinearGradient skyGrad = new LinearGradient(
                0, 0, 0, horizonY,
                COLOR_SKY_TOP, COLOR_SKY_BOTTOM,
                Shader.TileMode.CLAMP
        );
        roadPaint.setShader(skyGrad);
        canvas.drawRect(0, 0, width, horizonY, roadPaint);
        roadPaint.setShader(null);

        // Cyber Grid Lines on Horizon
        for (int i = -6; i <= 6; i++) {
            float startX = (width * 0.5f) + (i * (width / 6.0f));
            canvas.drawLine(startX, 0, (width * 0.5f) + (i * 18.0f), horizonY, horizonGridPaint);
        }
        canvas.drawLine(0, horizonY, width, horizonY, horizonGridPaint);
    }

    private final Point2D pNearL = new Point2D();
    private final Point2D pNearR = new Point2D();
    private final Point2D pFarL = new Point2D();
    private final Point2D pFarR = new Point2D();

    private void drawRoad(Canvas canvas, int width, int height, float viewCenterX, float horizonY) {
        float startZ = (float) Math.floor(worldZ / SEGMENT_LENGTH) * SEGMENT_LENGTH;

        for (float z = startZ + VIEW_DISTANCE; z >= startZ; z -= SEGMENT_LENGTH) {
            float z0 = z;
            float z1 = z + SEGMENT_LENGTH;

            if (!project(-ROAD_WIDTH * 0.5f, -120f, z0, viewCenterX, horizonY, pNearL) ||
                !project(ROAD_WIDTH * 0.5f, -120f, z0, viewCenterX, horizonY, pNearR) ||
                !project(-ROAD_WIDTH * 0.5f, -120f, z1, viewCenterX, horizonY, pFarL) ||
                !project(ROAD_WIDTH * 0.5f, -120f, z1, viewCenterX, horizonY, pFarR)) {
                continue;
            }

            int segmentIndex = (int) (z / SEGMENT_LENGTH);
            boolean alternate = (Math.abs(segmentIndex) % 2 == 0);

            // Ground Highway Quad
            renderPath.reset();
            renderPath.moveTo(pNearL.x, pNearL.y);
            renderPath.lineTo(pNearR.x, pNearR.y);
            renderPath.lineTo(pFarR.x, pFarR.y);
            renderPath.lineTo(pFarL.x, pFarL.y);
            renderPath.close();

            roadPaint.setColor(alternate ? COLOR_ROAD_A : COLOR_ROAD_B);
            canvas.drawPath(renderPath, roadPaint);

            // Neon Glowing Curb Borders
            linePaint.setColor(alternate ? COLOR_BORDER_A : COLOR_BORDER_B);
            linePaint.setStrokeWidth(alternate ? 4f * pNearL.scale : 2f * pNearL.scale);
            canvas.drawLine(pNearL.x, pNearL.y, pFarL.x, pFarL.y, linePaint);
            canvas.drawLine(pNearR.x, pNearR.y, pFarR.x, pFarR.y, linePaint);

            // Lane Dividers (-LANE_OFFSET/2 and +LANE_OFFSET/2)
            Point2D pLane0Near = new Point2D();
            Point2D pLane0Far = new Point2D();
            Point2D pLane1Near = new Point2D();
            Point2D pLane1Far = new Point2D();

            if (project(-100f, -120f, z0, viewCenterX, horizonY, pLane0Near) &&
                project(-100f, -120f, z1, viewCenterX, horizonY, pLane0Far)) {
                linePaint.setColor(COLOR_LANE_LINE);
                linePaint.setStrokeWidth(2f * pLane0Near.scale);
                if (alternate) {
                    canvas.drawLine(pLane0Near.x, pLane0Near.y, pLane0Far.x, pLane0Far.y, linePaint);
                }
            }

            if (project(100f, -120f, z0, viewCenterX, horizonY, pLane1Near) &&
                project(100f, -120f, z1, viewCenterX, horizonY, pLane1Far)) {
                linePaint.setColor(COLOR_LANE_LINE);
                linePaint.setStrokeWidth(2f * pLane1Near.scale);
                if (alternate) {
                    canvas.drawLine(pLane1Near.x, pLane1Near.y, pLane1Far.x, pLane1Far.y, linePaint);
                }
            }
        }
    }

    private final Point2D pObj = new Point2D();

    private void drawWorldObjects(Canvas canvas, float viewCenterX, float horizonY) {
        // Render from furthest to nearest (painter's algorithm)
        for (int i = worldObjects.size() - 1; i >= 0; i--) {
            WorldObject obj = worldObjects.get(i);
            if (obj.collected) continue;

            float objX = obj.lane * LANE_OFFSET;
            float objY = -120f; // Ground baseline

            if (obj.type == WorldObject.TYPE_GEM) {
                // Floating rotating Diamond
                float floatY = objY + 45.0f + (float) Math.sin(Math.toRadians(obj.animRotation * 3f)) * 12.0f;
                if (!project(objX, floatY, obj.z, viewCenterX, horizonY, pObj)) continue;

                float gemSize = 22.0f * pObj.scale;
                gemPaint.setColor(Color.parseColor("#FFD700"));

                renderPath.reset();
                renderPath.moveTo(pObj.x, pObj.y - gemSize);
                renderPath.lineTo(pObj.x + gemSize * 0.7f, pObj.y);
                renderPath.lineTo(pObj.x, pObj.y + gemSize);
                renderPath.lineTo(pObj.x - gemSize * 0.7f, pObj.y);
                renderPath.close();
                canvas.drawPath(renderPath, gemPaint);

                // Inner Gem Facet Highlight
                gemPaint.setColor(Color.parseColor("#FFF3A0"));
                canvas.drawCircle(pObj.x, pObj.y, gemSize * 0.35f, gemPaint);

            } else if (obj.type == WorldObject.TYPE_HURDLE) {
                // Low Jumpable Barricade
                if (!project(objX, objY, obj.z, viewCenterX, horizonY, pObj)) continue;
                float w = 85.0f * pObj.scale;
                float h = 40.0f * pObj.scale;

                hazardPaint.setColor(Color.parseColor("#FF4444"));
                canvas.drawRect(pObj.x - w, pObj.y - h, pObj.x + w, pObj.y, hazardPaint);

                // Warning stripes
                hazardPaint.setColor(Color.parseColor("#FFFFFF"));
                canvas.drawRect(pObj.x - w * 0.5f, pObj.y - h, pObj.x - w * 0.2f, pObj.y, hazardPaint);
                canvas.drawRect(pObj.x + w * 0.2f, pObj.y - h, pObj.x + w * 0.5f, pObj.y, hazardPaint);

            } else if (obj.type == WorldObject.TYPE_LASER) {
                // Overhead Laser Beam (Must crouch / slide)
                float laserHeight = objY + 80f;
                if (!project(objX, laserHeight, obj.z, viewCenterX, horizonY, pObj)) continue;

                float beamW = 95.0f * pObj.scale;
                float beamH = 14.0f * pObj.scale;

                // Glowing Neon Laser
                hazardPaint.setColor(Color.parseColor("#FF007F"));
                canvas.drawRect(pObj.x - beamW, pObj.y - beamH, pObj.x + beamW, pObj.y + beamH, hazardPaint);

                hazardPaint.setColor(Color.parseColor("#FFFFFF"));
                canvas.drawRect(pObj.x - beamW, pObj.y - beamH * 0.35f, pObj.x + beamW, pObj.y + beamH * 0.35f, hazardPaint);

            } else if (obj.type == WorldObject.TYPE_BLOCK) {
                // High Solid Column (Must dodge lateral lane)
                if (!project(objX, objY, obj.z, viewCenterX, horizonY, pObj)) continue;
                float bw = 75.0f * pObj.scale;
                float bh = 140.0f * pObj.scale;

                hazardPaint.setColor(Color.parseColor("#7B1FA2"));
                canvas.drawRect(pObj.x - bw, pObj.y - bh, pObj.x + bw, pObj.y, hazardPaint);

                // Cyber Grid Texture on Block
                hazardPaint.setColor(Color.parseColor("#EA80FC"));
                hazardPaint.setStyle(Paint.Style.STROKE);
                hazardPaint.setStrokeWidth(2f * pObj.scale);
                canvas.drawRect(pObj.x - bw, pObj.y - bh, pObj.x + bw, pObj.y, hazardPaint);
                hazardPaint.setStyle(Paint.Style.FILL);
            }
        }
    }

    private final Point2D pPlayer = new Point2D();

    private void drawPlayer(Canvas canvas, float viewCenterX, float horizonY) {
        float playerZ = worldZ + 120.0f;
        float actualY = -120f + playerY;

        if (!project(playerX, actualY, playerZ, viewCenterX, horizonY, pPlayer)) return;

        float s = pPlayer.scale;
        float bodyHeight = isSliding ? 25.0f * s : 75.0f * s;
        float bodyWidth = 32.0f * s;

        // Dynamic Running Leg Swing
        float legSwing = (float) Math.sin(runAnimCycle) * 14.0f * s;
        if (isJumping || isSliding) legSwing = 0;

        // Player Shadow on Ground
        Point2D pShadow = new Point2D();
        if (project(playerX, -120f, playerZ, viewCenterX, horizonY, pShadow)) {
            playerPaint.setColor(Color.parseColor("#44000000"));
            canvas.drawOval(
                    pShadow.x - bodyWidth * 1.1f,
                    pShadow.y - 8f * s,
                    pShadow.x + bodyWidth * 1.1f,
                    pShadow.y + 8f * s,
                    playerPaint
            );
        }

        // Running Legs
        if (!isSliding) {
            playerPaint.setColor(Color.parseColor("#0091EA"));
            canvas.drawLine(pPlayer.x - 10f * s, pPlayer.y, pPlayer.x - 10f * s + legSwing, pPlayer.y + 22f * s, playerPaint);
            canvas.drawLine(pPlayer.x + 10f * s, pPlayer.y, pPlayer.x + 10f * s - legSwing, pPlayer.y + 22f * s, playerPaint);
        }

        // Cyber Runner Torso
        playerPaint.setColor(Color.parseColor("#00E5FF"));
        canvas.drawRoundRect(
                pPlayer.x - bodyWidth * 0.5f,
                pPlayer.y - bodyHeight,
                pPlayer.x + bodyWidth * 0.5f,
                pPlayer.y,
                8f * s, 8f * s,
                playerPaint
        );

        // Cyber Helmet / Head
        float headRadius = isSliding ? 12.0f * s : 16.0f * s;
        float headY = isSliding ? (pPlayer.y - bodyHeight - 4f * s) : (pPlayer.y - bodyHeight - headRadius);

        playerPaint.setColor(Color.parseColor("#1565C0"));
        canvas.drawCircle(pPlayer.x, headY, headRadius, playerPaint);

        // Neon Visor Glowing Band
        playerPaint.setColor(Color.parseColor("#FFD700"));
        canvas.drawRoundRect(
                pPlayer.x - headRadius * 0.7f,
                headY - 4f * s,
                pPlayer.x + headRadius * 0.7f,
                headY + 4f * s,
                3f * s, 3f * s,
                playerPaint
        );

        // Thruster Jet Trail when jumping
        if (isJumping) {
            playerPaint.setColor(Color.parseColor("#FF007F"));
            canvas.drawCircle(pPlayer.x - 8f * s, pPlayer.y + 4f * s, 5f * s, playerPaint);
            canvas.drawCircle(pPlayer.x + 8f * s, pPlayer.y + 4f * s, 5f * s, playerPaint);
        }
    }

    private final Point2D pParticle = new Point2D();

    private void drawParticles(Canvas canvas, float viewCenterX, float horizonY) {
        for (int i = 0; i < particles.size(); i++) {
            Particle p = particles.get(i);
            if (!project(p.x, p.y, p.z, viewCenterX, horizonY, pParticle)) continue;

            float alpha = (float) p.life / (float) p.maxLife;
            particlePaint.setColor(p.color);
            particlePaint.setAlpha((int) (alpha * 255));

            float size = Math.max(3f, 8f * pParticle.scale);
            canvas.drawCircle(pParticle.x, pParticle.y, size, particlePaint);
        }
    }
}