package com.cyberrunner.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
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
        void onScoreUpdated(int score, int coins, float speed);
        void onGameOver(int finalScore, int coins);
        void onGameStarted();
    }

    private SurfaceHolder holder;
    private Thread gameThread;
    private volatile boolean isRunning;

    private SoundManager soundManager;
    private ScoreManager scoreManager;
    private GameStateListener stateListener;

    public enum State {
        READY,
        PLAYING,
        GAMEOVER
    }

    private State currentState = State.READY;

    // Canvas measurements
    private int screenWidth;
    private int screenHeight;
    private float groundY;

    // Player physics and character
    private float playerX;
    private float playerY;
    private float playerVelocityY;
    private final float playerWidth = 55f;
    private final float playerHeight = 85f;
    private int jumpsLeft = 2;
    private float runAnimTimer = 0f;

    // Movement physics
    private static final float GRAVITY = 1.35f;
    private static final float JUMP_FORCE = -24.5f;
    private float baseSpeed = 13f;
    private float currentSpeed = 13f;
    private float distanceScore = 0f;
    private int coinCount = 0;

    // Obstacles & Pickups
    private final List<Obstacle> obstacles = new ArrayList<Obstacle>();
    private final List<Coin> coins = new ArrayList<Coin>();
    private final List<Particle> particles = new ArrayList<Particle>();

    private float nextObstacleDistance = 0f;
    private float nextCoinDistance = 0f;
    private final Random random = new Random();

    // Graphic Paints
    private Paint paintGrid;
    private Paint paintNeonCyan;
    private Paint paintNeonMagenta;
    private Paint paintNeonYellow;
    private Paint paintSkyline;
    private Paint paintGround;
    private Paint paintPlayer;
    private Paint paintText;

    // Background parallax buildings
    private static final int BUILDING_COUNT = 8;
    private float[] buildingX;
    private float[] buildingW;
    private float[] buildingH;

    public GameView(Context context) {
        super(context);
        init();
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void setSoundManager(SoundManager soundManager) {
        this.soundManager = soundManager;
    }

    public void setScoreManager(ScoreManager scoreManager) {
        this.scoreManager = scoreManager;
    }

    public void setGameStateListener(GameStateListener listener) {
        this.stateListener = listener;
    }

    private void init() {
        holder = getHolder();
        holder.addCallback(this);
        setFocusable(true);

        paintGrid = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintGrid.setColor(Color.parseColor("#251D42"));
        paintGrid.setStrokeWidth(2f);

        paintNeonCyan = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintNeonCyan.setColor(Color.parseColor("#00F3FF"));
        paintNeonCyan.setStyle(Paint.Style.FILL);

        paintNeonMagenta = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintNeonMagenta.setColor(Color.parseColor("#FF0055"));
        paintNeonMagenta.setStyle(Paint.Style.FILL);

        paintNeonYellow = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintNeonYellow.setColor(Color.parseColor("#FFE600"));
        paintNeonYellow.setStyle(Paint.Style.FILL);

        paintSkyline = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintSkyline.setColor(Color.parseColor("#121124"));

        paintGround = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintGround.setColor(Color.parseColor("#0A0915"));

        paintPlayer = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintPlayer.setColor(Color.parseColor("#00F3FF"));

        paintText = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintText.setColor(Color.WHITE);
        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setFakeBoldText(true);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        screenWidth = getWidth();
        screenHeight = getHeight();
        groundY = screenHeight * 0.78f;

        initBackgroundSkyline();
        resetGameVariables();

        isRunning = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
        screenWidth = width;
        screenHeight = height;
        groundY = screenHeight * 0.78f;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
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

    private void initBackgroundSkyline() {
        buildingX = new float[BUILDING_COUNT];
        buildingW = new float[BUILDING_COUNT];
        buildingH = new float[BUILDING_COUNT];

        float currX = 0f;
        for (int i = 0; i < BUILDING_COUNT; i++) {
            buildingX[i] = currX;
            buildingW[i] = 120f + random.nextFloat() * 140f;
            buildingH[i] = 150f + random.nextFloat() * 250f;
            currX += buildingW[i] + 15f;
        }
    }

    public void resetGameVariables() {
        playerX = screenWidth * 0.20f;
        playerY = groundY - playerHeight;
        playerVelocityY = 0f;
        jumpsLeft = 2;

        currentSpeed = baseSpeed;
        distanceScore = 0f;
        coinCount = 0;

        obstacles.clear();
        coins.clear();
        particles.clear();

        nextObstacleDistance = 500f;
        nextCoinDistance = 300f;
    }

    public void startOrRestartGame() {
        resetGameVariables();
        currentState = State.PLAYING;
        if (stateListener != null) {
            stateListener.onGameStarted();
        }
    }

    public State getCurrentState() {
        return currentState;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (currentState == State.READY) {
                startOrRestartGame();
                return true;
            } else if (currentState == State.PLAYING) {
                triggerJump();
                return true;
            } else if (currentState == State.GAMEOVER) {
                startOrRestartGame();
                return true;
            }
        }
        return super.onTouchEvent(event);
    }

    private void triggerJump() {
        if (jumpsLeft > 0) {
            playerVelocityY = JUMP_FORCE;
            jumpsLeft--;
            spawnJumpSparks(playerX + playerWidth / 2, playerY + playerHeight);
            if (soundManager != null) {
                soundManager.playJumpSound();
            }
        }
    }

    @Override
    public void run() {
        long lastTime = System.currentTimeMillis();
        while (isRunning) {
            long now = System.currentTimeMillis();
            float deltaTime = (now - lastTime) / 1000f;
            if (deltaTime > 0.1f) deltaTime = 0.1f;
            lastTime = now;

            updateGame(deltaTime);
            drawGame();

            try {
                Thread.sleep(16); // Target ~60 FPS
            } catch (InterruptedException ignored) {
            }
        }
    }

    private void updateGame(float dt) {
        if (currentState != State.PLAYING) {
            // Update particles even when in game over
            updateParticles();
            return;
        }

        // Increase distance & speed smoothly over time
        distanceScore += currentSpeed * 0.15f;
        currentSpeed = baseSpeed + (distanceScore / 600f);
        if (currentSpeed > 32f) currentSpeed = 32f;

        // Player Gravity
        playerVelocityY += GRAVITY;
        playerY += playerVelocityY;

        if (playerY >= groundY - playerHeight) {
            playerY = groundY - playerHeight;
            playerVelocityY = 0f;
            jumpsLeft = 2; // Restore double jump on ground contact
        }

        runAnimTimer += dt * 16f;

        // Parallax skyline movement
        for (int i = 0; i < BUILDING_COUNT; i++) {
            buildingX[i] -= currentSpeed * 0.18f;
            if (buildingX[i] + buildingW[i] < 0) {
                // Find rightmost building
                float rightmost = 0;
                for (int j = 0; j < BUILDING_COUNT; j++) {
                    if (buildingX[j] > rightmost) rightmost = buildingX[j] + buildingW[j];
                }
                buildingX[i] = rightmost + 15f;
                buildingW[i] = 100f + random.nextFloat() * 140f;
                buildingH[i] = 140f + random.nextFloat() * 260f;
            }
        }

        // Spawn Obstacles
        nextObstacleDistance -= currentSpeed;
        if (nextObstacleDistance <= 0) {
            int obsType = random.nextInt(3); // 0: Drone, 1: Spikes, 2: Cyber Pillar
            float obsW = 50f + random.nextInt(35);
            float obsH = 45f + random.nextInt(60);
            float obsY = groundY - obsH;

            if (obsType == 0) {
                // Flying drone obstacle
                obsY = groundY - playerHeight - 40f - (random.nextInt(60));
                obsW = 55f;
                obsH = 35f;
            }

            obstacles.add(new Obstacle(screenWidth + 50f, obsY, obsW, obsH, obsType));
            // Space next obstacle based on current velocity
            nextObstacleDistance = 380f + random.nextInt(320) + (currentSpeed * 7f);
        }

        // Spawn Coins
        nextCoinDistance -= currentSpeed;
        if (nextCoinDistance <= 0) {
            int coinPattern = random.nextInt(3);
            float startSpawnX = screenWidth + 50f;
            float coinY = groundY - 50f - (random.nextFloat() * 110f);

            for (int k = 0; k < (coinPattern + 1); k++) {
                coins.add(new Coin(startSpawnX + (k * 60f), coinY));
            }
            nextCoinDistance = 220f + random.nextInt(340);
        }

        // Move and collide obstacles
        Iterator<Obstacle> obsIter = obstacles.iterator();
        while (obsIter.hasNext()) {
            Obstacle obs = obsIter.next();
            obs.x -= currentSpeed;

            // Collision check with player
            if (checkCollision(playerX, playerY, playerWidth, playerHeight, obs.x, obs.y, obs.width, obs.height)) {
                // Crash
                triggerGameOver();
                break;
            }

            if (obs.x + obs.width < -100f) {
                obsIter.remove();
            }
        }

        // Move and collect coins
        Iterator<Coin> coinIter = coins.iterator();
        while (coinIter.hasNext()) {
            Coin coin = coinIter.next();
            coin.x -= currentSpeed;
            coin.rotation += 8f;

            // Coin collection check
            if (checkCollision(playerX, playerY, playerWidth, playerHeight, coin.x - 22f, coin.y - 22f, 44f, 44f)) {
                coinCount++;
                distanceScore += 50f;
                spawnCoinSparkles(coin.x, coin.y);
                if (soundManager != null) {
                    soundManager.playCoinSound();
                }
                coinIter.remove();
                continue;
            }

            if (coin.x < -100f) {
                coinIter.remove();
            }
        }

        updateParticles();

        if (stateListener != null) {
            stateListener.onScoreUpdated((int) distanceScore, coinCount, currentSpeed);
        }
    }

    private void updateParticles() {
        Iterator<Particle> pIter = particles.iterator();
        while (pIter.hasNext()) {
            Particle p = pIter.next();
            p.update();
            if (p.isDead()) {
                pIter.remove();
            }
        }
    }

    private boolean checkCollision(float r1x, float r1y, float r1w, float r1h, float r2x, float r2y, float r2w, float r2h) {
        // Inner hitboxes slightly smaller for forgiving arcade feel
        float padding = 8f;
        return r1x + padding < r2x + r2w - padding &&
                r1x + r1w - padding > r2x + padding &&
                r1y + padding < r2y + r2h - padding &&
                r1y + r1h - padding > r2y + padding;
    }

    private void triggerGameOver() {
        currentState = State.GAMEOVER;
        spawnExplosion(playerX + playerWidth / 2, playerY + playerHeight / 2);
        if (soundManager != null) {
            soundManager.playGameOverSound();
        }

        final int finalScore = (int) distanceScore;
        final int finalCoins = coinCount;

        if (scoreManager != null) {
            scoreManager.recordNewScore(finalScore, finalCoins);
        }

        if (stateListener != null) {
            post(new Runnable() {
                @Override
                public void run() {
                    stateListener.onGameOver(finalScore, finalCoins);
                }
            });
        }
    }

    private void spawnJumpSparks(float x, float y) {
        for (int i = 0; i < 12; i++) {
            particles.add(new Particle(x, y, (random.nextFloat() - 0.5f) * 8f, (random.nextFloat() * 4f) + 2f, Color.parseColor("#00F3FF"), 20));
        }
    }

    private void spawnCoinSparkles(float x, float y) {
        for (int i = 0; i < 14; i++) {
            particles.add(new Particle(x, y, (random.nextFloat() - 0.5f) * 10f, (random.nextFloat() - 0.5f) * 10f, Color.parseColor("#FFE600"), 25));
        }
    }

    private void spawnExplosion(float x, float y) {
        for (int i = 0; i < 35; i++) {
            int color = (i % 2 == 0) ? Color.parseColor("#FF0055") : Color.parseColor("#00F3FF");
            particles.add(new Particle(x, y, (random.nextFloat() - 0.5f) * 18f, (random.nextFloat() - 0.5f) * 18f, color, 40));
        }
    }

    private void drawGame() {
        if (!holder.getSurface().isValid()) return;
        Canvas canvas = holder.lockCanvas();
        if (canvas == null) return;

        try {
            // Background deep cyberpunk gradient
            LinearGradient bgGradient = new LinearGradient(
                    0, 0, 0, screenHeight,
                    Color.parseColor("#06050C"),
                    Color.parseColor("#120E24"),
                    Shader.TileMode.CLAMP
            );
            Paint bgPaint = new Paint();
            bgPaint.setShader(bgGradient);
            canvas.drawRect(0, 0, screenWidth, screenHeight, bgPaint);

            // Parallax skyline silhouette
            if (buildingX != null) {
                for (int i = 0; i < BUILDING_COUNT; i++) {
                    float bx = buildingX[i];
                    float bw = buildingW[i];
                    float bh = buildingH[i];
                    canvas.drawRect(bx, groundY - bh, bx + bw, groundY, paintSkyline);
                    // Building neon top light
                    paintNeonMagenta.setStrokeWidth(3f);
                    canvas.drawLine(bx, groundY - bh, bx + bw, groundY - bh, paintNeonMagenta);
                }
            }

            // Neon synthwave ground platform
            canvas.drawRect(0, groundY, screenWidth, screenHeight, paintGround);

            // Ground grid perspective lines
            paintGrid.setColor(Color.parseColor("#281E48"));
            canvas.drawLine(0, groundY, screenWidth, groundY, paintNeonCyan);

            float gridOffset = (distanceScore * 3f) % 60f;
            for (float x = -gridOffset; x < screenWidth + 60f; x += 60f) {
                canvas.drawLine(x, groundY, x - 120f, screenHeight, paintGrid);
            }
            for (float y = groundY; y < screenHeight; y += 22f) {
                canvas.drawLine(0, y, screenWidth, y, paintGrid);
            }

            // Draw Obstacles
            for (int i = 0; i < obstacles.size(); i++) {
                Obstacle obs = obstacles.get(i);
                drawObstacle(canvas, obs);
            }

            // Draw Coins
            for (int i = 0; i < coins.size(); i++) {
                Coin coin = coins.get(i);
                drawCoin(canvas, coin);
            }

            // Draw Particles
            for (int i = 0; i < particles.size(); i++) {
                Particle p = particles.get(i);
                p.draw(canvas);
            }

            // Draw Player if playing or ready
            if (currentState != State.GAMEOVER) {
                drawPlayer(canvas);
            }

            // Draw State Overlays if not playing
            if (currentState == State.READY) {
                drawReadyScreen(canvas);
            } else if (currentState == State.GAMEOVER) {
                drawGameOverScreen(canvas);
            }

        } finally {
            holder.unlockCanvasAndPost(canvas);
        }
    }

    private void drawObstacle(Canvas canvas, Obstacle obs) {
        if (obs.type == 0) {
            // Drone hazard
            paintNeonMagenta.setColor(Color.parseColor("#FF0055"));
            canvas.drawRoundRect(new RectF(obs.x, obs.y, obs.x + obs.width, obs.y + obs.height), 10f, 10f, paintNeonMagenta);
            // Drone eye
            paintNeonYellow.setColor(Color.parseColor("#FFE600"));
            canvas.drawCircle(obs.x + obs.width / 2, obs.y + obs.height / 2, 7f, paintNeonYellow);
        } else if (obs.type == 1) {
            // Neon Spikes
            Path path = new Path();
            path.moveTo(obs.x, obs.y + obs.height);
            path.lineTo(obs.x + obs.width / 2, obs.y);
            path.lineTo(obs.x + obs.width, obs.y + obs.height);
            path.close();
            paintNeonMagenta.setColor(Color.parseColor("#FF2A6D"));
            canvas.drawPath(path, paintNeonMagenta);
        } else {
            // Cyber pillar with laser stripes
            paintNeonMagenta.setColor(Color.parseColor("#1B142F"));
            canvas.drawRect(obs.x, obs.y, obs.x + obs.width, obs.y + obs.height, paintNeonMagenta);
            paintNeonCyan.setStrokeWidth(4f);
            canvas.drawLine(obs.x + 4f, obs.y, obs.x + 4f, obs.y + obs.height, paintNeonCyan);
            canvas.drawLine(obs.x + obs.width - 4f, obs.y, obs.x + obs.width - 4f, obs.y + obs.height, paintNeonCyan);
        }
    }

    private void drawCoin(Canvas canvas, Coin coin) {
        canvas.save();
        canvas.translate(coin.x, coin.y);
        canvas.rotate(coin.rotation);

        paintNeonYellow.setColor(Color.parseColor("#FFE600"));
        RectF diamond = new RectF(-16f, -16f, 16f, 16f);
        canvas.drawRoundRect(diamond, 4f, 4f, paintNeonYellow);

        Paint inner = new Paint(Paint.ANTI_ALIAS_FLAG);
        inner.setColor(Color.parseColor("#FFFFFF"));
        canvas.drawCircle(0, 0, 6f, inner);

        canvas.restore();
    }

    private void drawPlayer(Canvas canvas) {
        // Player Body: Neon cyber-runner avatar
        float legCycle = (float) Math.sin(runAnimTimer);
        float legSwing = (playerVelocityY != 0) ? 0f : legCycle * 14f;

        // Glowing visor trail
        Paint trailPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        trailPaint.setColor(Color.argb(80, 0, 243, 255));
        canvas.drawRect(playerX - 22f, playerY + 10f, playerX + 5f, playerY + 30f, trailPaint);

        // Torso / Suit
        Paint suitPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        suitPaint.setColor(Color.parseColor("#0F172A"));
        canvas.drawRoundRect(new RectF(playerX, playerY, playerX + playerWidth, playerY + playerHeight - 20f), 12f, 12f, suitPaint);

        // Cyber Chest Armor Glow
        Paint chestGlow = new Paint(Paint.ANTI_ALIAS_FLAG);
        chestGlow.setColor(Color.parseColor("#00F3FF"));
        canvas.drawCircle(playerX + playerWidth / 2, playerY + 28f, 7f, chestGlow);

        // Visor Headband
        Paint visorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        visorPaint.setColor(Color.parseColor("#00F3FF"));
        canvas.drawRect(playerX + 10f, playerY + 8f, playerX + playerWidth + 2f, playerY + 20f, visorPaint);

        // Running Legs
        Paint legPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        legPaint.setColor(Color.parseColor("#FF0055"));
        legPaint.setStrokeWidth(8f);
        legPaint.setStrokeCap(Paint.Cap.ROUND);

        float legYBase = playerY + playerHeight - 20f;
        // Left Leg
        canvas.drawLine(playerX + 16f, legYBase, playerX + 10f - legSwing, playerY + playerHeight, legPaint);
        // Right Leg
        canvas.drawLine(playerX + playerWidth - 16f, legYBase, playerX + playerWidth - 10f + legSwing, playerY + playerHeight, legPaint);
    }

    private void drawReadyScreen(Canvas canvas) {
        paintText.setTextSize(64f);
        paintText.setColor(Color.parseColor("#00F3FF"));
        canvas.drawText("CYBER RUNNER", screenWidth / 2f, screenHeight * 0.38f, paintText);

        paintText.setTextSize(36f);
        paintText.setColor(Color.parseColor("#FFE600"));
        canvas.drawText("TAP TO JUMP OVER HAZARDS", screenWidth / 2f, screenHeight * 0.45f, paintText);

        paintText.setTextSize(28f);
        paintText.setColor(Color.parseColor("#8E8EB2"));
        canvas.drawText("DOUBLE TAP IN MID-AIR FOR DOUBLE JUMP", screenWidth / 2f, screenHeight * 0.51f, paintText);

        paintText.setTextSize(40f);
        paintText.setColor(Color.WHITE);
        canvas.drawText("[ TAP SCREEN TO START ]", screenWidth / 2f, screenHeight * 0.62f, paintText);
    }

    private void drawGameOverScreen(Canvas canvas) {
        Paint overlay = new Paint();
        overlay.setColor(Color.argb(190, 8, 8, 17));
        canvas.drawRect(0, 0, screenWidth, screenHeight, overlay);

        paintText.setTextSize(60f);
        paintText.setColor(Color.parseColor("#FF0055"));
        canvas.drawText("SYSTEM BREACHED", screenWidth / 2f, screenHeight * 0.35f, paintText);

        paintText.setTextSize(36f);
        paintText.setColor(Color.WHITE);
        canvas.drawText("SCORE: " + (int) distanceScore, screenWidth / 2f, screenHeight * 0.43f, paintText);

        paintText.setTextSize(32f);
        paintText.setColor(Color.parseColor("#FFE600"));
        canvas.drawText("CYBER COINS: " + coinCount, screenWidth / 2f, screenHeight * 0.49f, paintText);

        paintText.setTextSize(42f);
        paintText.setColor(Color.parseColor("#00F3FF"));
        canvas.drawText("[ TAP TO RETRY RUN ]", screenWidth / 2f, screenHeight * 0.60f, paintText);
    }

    // Helper data structures
    private static class Obstacle {
        float x, y, width, height;
        int type;

        Obstacle(float x, float y, float width, float height, int type) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.type = type;
        }
    }

    private static class Coin {
        float x, y;
        float rotation = 0f;

        Coin(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    private static class Particle {
        float x, y, vx, vy;
        int color;
        int life;
        int maxLife;
        Paint paint;

        Particle(float x, float y, float vx, float vy, int color, int maxLife) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.color = color;
            this.life = maxLife;
            this.maxLife = maxLife;
            this.paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        }

        void update() {
            x += vx;
            y += vy;
            life--;
        }

        boolean isDead() {
            return life <= 0;
        }

        void draw(Canvas canvas) {
            float alpha = (float) life / maxLife;
            paint.setColor(color);
            paint.setAlpha((int) (alpha * 255));
            canvas.drawCircle(x, y, 4f + alpha * 3f, paint);
        }
    }
}