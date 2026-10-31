package com.cyberrunner.app;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class CyberRenderer implements GLSurfaceView.Renderer {

    public interface GameCallback {
        void onScoreUpdated(int distance, int coins, float speedMultiplier);
        void onGameOver(int finalDistance, int finalCoins);
        void onGameStarted();
    }

    public enum State {
        READY,
        PLAYING,
        GAMEOVER
    }

    private State gameState = State.READY;
    private GameCallback gameCallback;
    private SoundManager soundManager;
    private ScoreManager scoreManager;

    // View & Projection Matrices
    private final float[] projectionMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] modelMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];

    // GLSL Shader handles
    private int glProgram;
    private int uMVPMatrixHandle;
    private int aPositionHandle;
    private int uColorHandle;

    // 3D Player Physics
    private float playerY = 0f;
    private float playerVelocityY = 0f;
    private final float playerX = 0f;
    private final float playerZ = 0.5f;
    private int jumpsLeft = 2;
    private float runCycle = 0f;

    private static final float GRAVITY = -42.0f;
    private static final float JUMP_FORCE = 15.5f;

    // 3D World Scrolling
    private float baseSpeed = 16.0f;
    private float currentSpeed = 16.0f;
    private float distanceTraveled = 0f;
    private int collectedCoins = 0;
    private float gridOffsetZ = 0f;

    // 3D Environment - Buildings
    private static final int BUILDING_COUNT = 14;
    private final Building[] buildings = new Building[BUILDING_COUNT];

    // 3D Obstacles, Coins, and Particles
    private final List<Obstacle3D> obstacles = new ArrayList<Obstacle3D>();
    private final List<Coin3D> coins = new ArrayList<Coin3D>();
    private final List<Particle3D> particles = new ArrayList<Particle3D>();

    private float nextObstacleZ = 20.0f;
    private float nextCoinZ = 12.0f;
    private final Random random = new Random();

    // Reusable buffers
    private FloatBuffer cubeBuffer;
    private FloatBuffer octahedronBuffer;
    private FloatBuffer lineBuffer;

    private long lastFrameTime = 0;

    public void setGameCallback(GameCallback callback) {
        this.gameCallback = callback;
    }

    public void setSoundManager(SoundManager soundManager) {
        this.soundManager = soundManager;
    }

    public void setScoreManager(ScoreManager scoreManager) {
        this.scoreManager = scoreManager;
    }

    public State getGameState() {
        return gameState;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.02f, 0.015f, 0.04f, 1.0f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthFunc(GLES20.GL_LEQUAL);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        initShaders();
        initGeometry();
        initBuildings();
        resetGameVariables();
        lastFrameTime = System.currentTimeMillis();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        float aspect = (float) width / (float) (height == 0 ? 1 : height);
        // 3D Perspective Frustum (65 degree FOV)
        Matrix.perspectiveM(projectionMatrix, 0, 65.0f, aspect, 0.5f, 150.0f);
    }

    private void initShaders() {
        String vertexShaderCode =
                "uniform mat4 uMVPMatrix;" +
                "attribute vec4 aPosition;" +
                "void main() {" +
                "  gl_Position = uMVPMatrix * aPosition;" +
                "}";

        String fragmentShaderCode =
                "precision mediump float;" +
                "uniform vec4 uColor;" +
                "void main() {" +
                "  gl_FragColor = uColor;" +
                "}";

        int vertexShader = CyberGeometry.loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = CyberGeometry.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        glProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(glProgram, vertexShader);
        GLES20.glAttachShader(glProgram, fragmentShader);
        GLES20.glLinkProgram(glProgram);

        uMVPMatrixHandle = GLES20.glGetUniformLocation(glProgram, "uMVPMatrix");
        aPositionHandle = GLES20.glGetAttribLocation(glProgram, "aPosition");
        uColorHandle = GLES20.glGetUniformLocation(glProgram, "uColor");
    }

    private void initGeometry() {
        cubeBuffer = CyberGeometry.getCubeBuffer();
        octahedronBuffer = CyberGeometry.getOctahedronBuffer();

        // 2-point unit line for grid rendering
        float[] lineCoords = {0f, 0f, 0f, 0f, 0f, 1f};
        lineBuffer = CyberGeometry.createBuffer(lineCoords);
    }

    private void initBuildings() {
        for (int i = 0; i < BUILDING_COUNT; i++) {
            boolean isLeft = (i % 2 == 0);
            float sideX = isLeft ? (-5.5f - random.nextFloat() * 4.0f) : (5.5f + random.nextFloat() * 4.0f);
            float z = -i * 8.0f;
            float width = 2.5f + random.nextFloat() * 2.0f;
            float depth = 4.0f + random.nextFloat() * 3.0f;
            float height = 7.0f + random.nextFloat() * 12.0f;
            int theme = random.nextInt(3);
            buildings[i] = new Building(sideX, height / 2.0f, z, width, height, depth, theme);
        }
    }

    public synchronized void resetGameVariables() {
        playerY = 0f;
        playerVelocityY = 0f;
        jumpsLeft = 2;
        runCycle = 0f;

        currentSpeed = baseSpeed;
        distanceTraveled = 0f;
        collectedCoins = 0;
        gridOffsetZ = 0f;

        obstacles.clear();
        coins.clear();
        particles.clear();

        nextObstacleZ = 25.0f;
        nextCoinZ = 15.0f;
    }

    public synchronized void triggerStartOrRestart() {
        resetGameVariables();
        gameState = State.PLAYING;
        if (gameCallback != null) {
            gameCallback.onGameStarted();
        }
    }

    public synchronized void triggerJump() {
        if (gameState != State.PLAYING) {
            triggerStartOrRestart();
            return;
        }

        if (jumpsLeft > 0) {
            playerVelocityY = JUMP_FORCE;
            jumpsLeft--;
            spawnSparks(playerX, playerY + 0.2f, playerZ, 0.0f, 0.95f, 1.0f, 12);
            if (soundManager != null) {
                soundManager.playJumpSound();
            }
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        long now = System.currentTimeMillis();
        float dt = (now - lastFrameTime) / 1000.0f;
        if (dt > 0.08f) dt = 0.08f;
        lastFrameTime = now;

        updateWorld(dt);

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glUseProgram(glProgram);

        // 3D Camera: Positioned behind and slightly above the runner looking forward into the cyber city
        Matrix.setLookAtM(viewMatrix, 0,
                0.0f, 3.4f, 6.2f,       // Eye position (X, Y, Z)
                0.0f, 1.8f, -14.0f,     // Look-at center (X, Y, Z)
                0.0f, 1.0f, 0.0f);      // Up vector

        drawTrackAndGrid();
        drawBuildings();
        drawObstacles();
        drawCoins();
        drawParticles();

        if (gameState != State.GAMEOVER) {
            drawPlayer();
        }
    }

    private void updateWorld(float dt) {
        if (gameState != State.PLAYING) {
            updateParticles(dt);
            return;
        }

        // Distance & dynamic speed acceleration
        distanceTraveled += currentSpeed * dt * 1.5f;
        currentSpeed = baseSpeed + (distanceTraveled / 350.0f);
        if (currentSpeed > 36.0f) currentSpeed = 36.0f;

        gridOffsetZ += currentSpeed * dt;
        if (gridOffsetZ > 3.0f) {
            gridOffsetZ -= 3.0f;
        }

        // 3D Player Vertical Physics
        playerVelocityY += GRAVITY * dt;
        playerY += playerVelocityY * dt;

        if (playerY <= 0.0f) {
            playerY = 0.0f;
            playerVelocityY = 0.0f;
            jumpsLeft = 2; // Replenish double jump upon landing
        }

        runCycle += dt * currentSpeed * 1.2f;

        // Advance 3D Buildings along the track
        for (int i = 0; i < BUILDING_COUNT; i++) {
            Building b = buildings[i];
            b.z += currentSpeed * dt;
            if (b.z > 8.0f) {
                b.z -= BUILDING_COUNT * 8.0f;
                b.height = 7.0f + random.nextFloat() * 12.0f;
                b.y = b.height / 2.0f;
                b.theme = random.nextInt(3);
            }
        }

        // Spawn 3D Obstacles
        nextObstacleZ -= currentSpeed * dt;
        if (nextObstacleZ <= 0f) {
            int type = random.nextInt(3); // 0: Ground Barrier, 1: Floating Drone, 2: Cyber Spike
            float obsY = 0.5f;
            float obsW = 3.2f;
            float obsH = 0.9f;
            float obsD = 0.8f;

            if (type == 1) {
                // Flying Drone: runner can jump or stay under
                obsY = 2.1f;
                obsW = 2.6f;
                obsH = 0.8f;
                obsD = 0.8f;
            } else if (type == 2) {
                // Tall Narrow Cyber Spike
                obsY = 0.9f;
                obsW = 1.4f;
                obsH = 1.8f;
                obsD = 0.8f;
            }

            obstacles.add(new Obstacle3D(0f, obsY, -55.0f, obsW, obsH, obsD, type));
            nextObstacleZ = 22.0f + random.nextFloat() * 14.0f;
        }

        // Spawn 3D Coins
        nextCoinZ -= currentSpeed * dt;
        if (nextCoinZ <= 0f) {
            int count = 1 + random.nextInt(3);
            float coinY = (random.nextBoolean()) ? 0.9f : 2.2f;
            for (int k = 0; k < count; k++) {
                coins.add(new Coin3D(0f, coinY, -50.0f - (k * 3.5f)));
            }
            nextCoinZ = 16.0f + random.nextFloat() * 12.0f;
        }

        // Advance and check 3D Obstacle Collisions
        Iterator<Obstacle3D> obsIter = obstacles.iterator();
        while (obsIter.hasNext()) {
            Obstacle3D obs = obsIter.next();
            obs.z += currentSpeed * dt;

            // 3D Bounding Box Collision Check with Player
            if (checkCollision3D(playerX, playerY + 0.6f, playerZ, 0.7f, 1.2f, 0.7f,
                    obs.x, obs.y, obs.z, obs.width, obs.height, obs.depth)) {
                triggerCrash();
                break;
            }

            if (obs.z > 8.0f) {
                obsIter.remove();
            }
        }

        // Advance, rotate, and check 3D Coin Collections
        Iterator<Coin3D> coinIter = coins.iterator();
        while (coinIter.hasNext()) {
            Coin3D coin = coinIter.next();
            coin.z += currentSpeed * dt;
            coin.rotationY += 180.0f * dt;

            // Collision check
            if (checkCollision3D(playerX, playerY + 0.6f, playerZ, 0.8f, 1.2f, 0.8f,
                    coin.x, coin.y, coin.z, 0.9f, 0.9f, 0.9f)) {
                collectedCoins++;
                distanceTraveled += 30.0f;
                spawnSparks(coin.x, coin.y, coin.z, 1.0f, 0.9f, 0.0f, 16);
                if (soundManager != null) {
                    soundManager.playCoinSound();
                }
                coinIter.remove();
                continue;
            }

            if (coin.z > 8.0f) {
                coinIter.remove();
            }
        }

        updateParticles(dt);

        if (gameCallback != null) {
            gameCallback.onScoreUpdated((int) distanceTraveled, collectedCoins, currentSpeed / baseSpeed);
        }
    }

    private void updateParticles(float dt) {
        Iterator<Particle3D> pIter = particles.iterator();
        while (pIter.hasNext()) {
            Particle3D p = pIter.next();
            p.x += p.vx * dt;
            p.y += p.vy * dt;
            p.z += p.vz * dt;
            p.life -= dt;
            if (p.life <= 0f) {
                pIter.remove();
            }
        }
    }

    private boolean checkCollision3D(float x1, float y1, float z1, float w1, float h1, float d1,
                                     float x2, float y2, float z2, float w2, float h2, float d2) {
        float hx1 = w1 * 0.5f; float hy1 = h1 * 0.5f; float hz1 = d1 * 0.5f;
        float hx2 = w2 * 0.5f; float hy2 = h2 * 0.5f; float hz2 = d2 * 0.5f;

        return (Math.abs(x1 - x2) <= (hx1 + hx2)) &&
               (Math.abs(y1 - y2) <= (hy1 + hy2)) &&
               (Math.abs(z1 - z2) <= (hz1 + hz2));
    }

    private void triggerCrash() {
        gameState = State.GAMEOVER;
        spawnSparks(playerX, playerY + 0.6f, playerZ, 1.0f, 0.05f, 0.35f, 40);
        if (soundManager != null) {
            soundManager.playGameOverSound();
        }

        final int finalDist = (int) distanceTraveled;
        final int finalCoins = collectedCoins;

        if (scoreManager != null) {
            scoreManager.recordNewScore(finalDist, finalCoins);
        }

        if (gameCallback != null) {
            gameCallback.onGameOver(finalDist, finalCoins);
        }
    }

    private void spawnSparks(float x, float y, float z, float r, float g, float b, int count) {
        for (int i = 0; i < count; i++) {
            float vx = (random.nextFloat() - 0.5f) * 12.0f;
            float vy = (random.nextFloat() * 10.0f) + 2.0f;
            float vz = (random.nextFloat() - 0.5f) * 12.0f;
            particles.add(new Particle3D(x, y, z, vx, vy, vz, r, g, b, 0.6f + random.nextFloat() * 0.4f));
        }
    }

    // =========================================================================
    // 3D RENDERING ROUTINES
    // =========================================================================

    private void drawTrackAndGrid() {
        // Track Platform Base (Dark purple floor)
        drawBox(0.0f, -0.2f, -30.0f, 5.0f, 0.4f, 70.0f, 0.07f, 0.05f, 0.14f, 1.0f);

        // Track Neon Borders
        drawBox(-2.5f, 0.05f, -30.0f, 0.15f, 0.15f, 70.0f, 0.0f, 0.95f, 1.0f, 1.0f);
        drawBox( 2.5f, 0.05f, -30.0f, 0.15f, 0.15f, 70.0f, 0.0f, 0.95f, 1.0f, 1.0f);

        // Center dashed neon guide line
        for (float z = -65.0f; z < 10.0f; z += 4.0f) {
            float lineZ = z + gridOffsetZ;
            drawBox(0.0f, 0.02f, lineZ, 0.10f, 0.04f, 2.0f, 1.0f, 0.0f, 0.35f, 0.85f);
        }

        // Horizontal Grid Lines scrolling towards the camera
        for (float z = -65.0f; z < 10.0f; z += 3.0f) {
            float crossZ = z + gridOffsetZ;
            drawBox(0.0f, 0.01f, crossZ, 4.8f, 0.02f, 0.08f, 0.16f, 0.12f, 0.28f, 0.7f);
        }
    }

    private void drawBuildings() {
        for (int i = 0; i < BUILDING_COUNT; i++) {
            Building b = buildings[i];
            // Main structure
            float r = (b.theme == 0) ? 0.05f : ((b.theme == 1) ? 0.08f : 0.03f);
            float g = (b.theme == 0) ? 0.06f : ((b.theme == 1) ? 0.03f : 0.07f);
            float bCol = (b.theme == 0) ? 0.16f : ((b.theme == 1) ? 0.14f : 0.12f);
            drawBox(b.x, b.y, b.z, b.width, b.height, b.depth, r, g, bCol, 0.95f);

            // Glowing rooftop neon trim
            float trimR = (b.theme == 0) ? 0.0f : 1.0f;
            float trimG = (b.theme == 0) ? 0.95f : 0.0f;
            float trimB = (b.theme == 0) ? 1.0f : 0.35f;
            drawBox(b.x, b.height, b.z, b.width + 0.1f, 0.3f, b.depth + 0.1f, trimR, trimG, trimB, 1.0f);

            // Windows / vertical accent strips
            drawBox(b.x, b.y, b.z, 0.15f, b.height * 0.8f, b.depth + 0.12f, 1.0f, 0.9f, 0.0f, 0.65f);
        }
    }

    private void drawPlayer() {
        float legAngle = (float) Math.sin(runCycle) * 0.5f;

        // Player Torso (Neon Cyber-Suit)
        drawBox(playerX, playerY + 0.75f, playerZ, 0.48f, 0.55f, 0.35f, 0.05f, 0.10f, 0.20f, 1.0f);

        // Glowing Chest Core
        drawBox(playerX, playerY + 0.80f, playerZ + 0.18f, 0.22f, 0.22f, 0.05f, 0.0f, 0.95f, 1.0f, 1.0f);

        // Head
        drawBox(playerX, playerY + 1.20f, playerZ, 0.32f, 0.30f, 0.30f, 0.08f, 0.09f, 0.18f, 1.0f);

        // Cyan Neon Visor
        drawBox(playerX, playerY + 1.20f, playerZ + 0.16f, 0.28f, 0.10f, 0.04f, 0.0f, 0.95f, 1.0f, 1.0f);

        // Left Leg
        float leftLegZ = (playerY > 0.01f) ? -0.1f : (legAngle * 0.3f);
        drawBox(playerX - 0.14f, playerY + 0.24f, playerZ + leftLegZ, 0.15f, 0.48f, 0.18f, 1.0f, 0.0f, 0.35f, 1.0f);

        // Right Leg
        float rightLegZ = (playerY > 0.01f) ? 0.1f : (-legAngle * 0.3f);
        drawBox(playerX + 0.14f, playerY + 0.24f, playerZ + rightLegZ, 0.15f, 0.48f, 0.18f, 1.0f, 0.0f, 0.35f, 1.0f);

        // Jump Jet Thrusters
        if (playerY > 0.05f) {
            drawBox(playerX - 0.14f, playerY - 0.08f, playerZ, 0.10f, 0.16f, 0.10f, 0.0f, 0.95f, 1.0f, 0.85f);
            drawBox(playerX + 0.14f, playerY - 0.08f, playerZ, 0.10f, 0.16f, 0.10f, 0.0f, 0.95f, 1.0f, 0.85f);
        }
    }

    private void drawObstacles() {
        for (int i = 0; i < obstacles.size(); i++) {
            Obstacle3D obs = obstacles.get(i);
            if (obs.type == 0) {
                // Neon Laser Barrier
                drawBox(obs.x, obs.y, obs.z, obs.width, obs.height, obs.depth, 1.0f, 0.0f, 0.35f, 0.95f);
                // Glowing Edge Beams
                drawBox(obs.x, obs.y + obs.height * 0.5f, obs.z, obs.width + 0.1f, 0.12f, obs.depth + 0.1f, 1.0f, 0.9f, 0.0f, 1.0f);
            } else if (obs.type == 1) {
                // Flying Drone Core
                drawBox(obs.x, obs.y, obs.z, obs.width * 0.5f, obs.height, obs.depth, 0.12f, 0.10f, 0.24f, 1.0f);
                // Wings
                drawBox(obs.x, obs.y, obs.z, obs.width, 0.15f, obs.depth * 0.7f, 1.0f, 0.0f, 0.35f, 1.0f);
                // Scanner Eye
                drawBox(obs.x, obs.y, obs.z + obs.depth * 0.55f, 0.25f, 0.25f, 0.05f, 1.0f, 0.9f, 0.0f, 1.0f);
            } else {
                // Cyber Spikes / Pillars
                drawBox(obs.x, obs.y, obs.z, obs.width, obs.height, obs.depth, 0.15f, 0.05f, 0.22f, 1.0f);
                drawBox(obs.x, obs.y, obs.z, obs.width * 0.6f, obs.height + 0.2f, obs.depth * 0.6f, 0.0f, 0.95f, 1.0f, 1.0f);
            }
        }
    }

    private void drawCoins() {
        for (int i = 0; i < coins.size(); i++) {
            Coin3D coin = coins.get(i);
            drawOctahedron(coin.x, coin.y, coin.z, 0.75f, coin.rotationY, 1.0f, 0.90f, 0.0f, 1.0f);
        }
    }

    private void drawParticles() {
        for (int i = 0; i < particles.size(); i++) {
            Particle3D p = particles.get(i);
            float alpha = p.life / p.maxLife;
            drawBox(p.x, p.y, p.z, 0.12f, 0.12f, 0.12f, p.r, p.g, p.b, alpha);
        }
    }

    private void drawBox(float x, float y, float z, float sx, float sy, float sz,
                         float r, float g, float b, float a) {
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, x, y, z);
        Matrix.scaleM(modelMatrix, 0, sx, sy, sz);

        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);

        GLES20.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, mvpMatrix, 0);
        GLES20.glUniform4f(uColorHandle, r, g, b, a);

        cubeBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aPositionHandle);
        GLES20.glVertexAttribPointer(aPositionHandle, 3, GLES20.GL_FLOAT, false, 0, cubeBuffer);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36);
        GLES20.glDisableVertexAttribArray(aPositionHandle);
    }

    private void drawOctahedron(float x, float y, float z, float scale, float rotY,
                               float r, float g, float b, float a) {
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, x, y, z);
        Matrix.rotateM(modelMatrix, 0, rotY, 0f, 1f, 0f);
        Matrix.rotateM(modelMatrix, 0, 25f, 1f, 0f, 0f);
        Matrix.scaleM(modelMatrix, 0, scale, scale, scale);

        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);

        GLES20.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, mvpMatrix, 0);
        GLES20.glUniform4f(uColorHandle, r, g, b, a);

        octahedronBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aPositionHandle);
        GLES20.glVertexAttribPointer(aPositionHandle, 3, GLES20.GL_FLOAT, false, 0, octahedronBuffer);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 24);
        GLES20.glDisableVertexAttribArray(aPositionHandle);
    }

    // =========================================================================
    // HELPER DATA OBJECTS
    // =========================================================================

    private static class Building {
        float x, y, z, width, height, depth;
        int theme;

        Building(float x, float y, float z, float width, float height, float depth, int theme) {
            this.x = x; this.y = y; this.z = z;
            this.width = width; this.height = height; this.depth = depth;
            this.theme = theme;
        }
    }

    private static class Obstacle3D {
        float x, y, z, width, height, depth;
        int type;

        Obstacle3D(float x, float y, float z, float width, float height, float depth, int type) {
            this.x = x; this.y = y; this.z = z;
            this.width = width; this.height = height; this.depth = depth;
            this.type = type;
        }
    }

    private static class Coin3D {
        float x, y, z;
        float rotationY = 0f;

        Coin3D(float x, float y, float z) {
            this.x = x; this.y = y; this.z = z;
        }
    }

    private static class Particle3D {
        float x, y, z, vx, vy, vz;
        float r, g, b;
        float life, maxLife;

        Particle3D(float x, float y, float z, float vx, float vy, float vz,
                   float r, float g, float b, float life) {
            this.x = x; this.y = y; this.z = z;
            this.vx = vx; this.vy = vy; this.vz = vz;
            this.r = r; this.g = g; this.b = b;
            this.life = life; this.maxLife = life;
        }
    }
}