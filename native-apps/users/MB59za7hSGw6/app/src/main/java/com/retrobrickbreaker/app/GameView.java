package com.retrobrickbreaker.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.List;

public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    public interface GameStateListener {
        void onScoreUpdated(int score, int highScore, int lives, int level);
        void onGameOver(int finalScore);
        void onLevelCleared(int nextLevel);
    }

    public static final int STATE_START = 0;
    public static final int STATE_PLAYING = 1;
    public static final int STATE_PAUSED = 2;
    public static final int STATE_GAME_OVER = 3;
    public static final int STATE_LEVEL_CLEARED = 4;

    private Thread gameThread = null;
    private SurfaceHolder surfaceHolder;
    private volatile boolean isRunning = false;

    private int gameState = STATE_START;
    private GameStateListener gameStateListener;

    // Canvas Dimensions
    private int screenWidth = 0;
    private int screenHeight = 0;

    // Gameplay Attributes
    private int score = 0;
    private int highScore = 0;
    private int lives = 3;
    private int currentLevel = 1;

    // Paddle
    private RectF paddleRect;
    private float paddleWidth;
    private float paddleHeight;
    private float paddleX;
    private float paddleY;

    // Ball
    private float ballX;
    private float ballY;
    private float ballRadius;
    private float ballSpeedX;
    private float ballSpeedY;
    private float baseBallSpeed = 14f;

    // Bricks Grid
    private List<Brick> bricks;
    private int totalBricksCount = 0;

    // Particles
    private List<Particle> particles;

    // Paints
    private Paint paintHud;
    private Paint paintPaddle;
    private Paint paintBall;
    private Paint paintBrick;
    private Paint paintParticle;
    private Paint paintText;
    private Paint paintOverlay;

    // High score prefs
    private SharedPreferences prefs;

    public GameView(Context context) {
        super(context);
        init(context);
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);

        prefs = context.getSharedPreferences("RetroBrickBreakerPrefs", Context.MODE_PRIVATE);
        highScore = prefs.getInt("high_score", 0);

        bricks = new ArrayList<Brick>();
        particles = new ArrayList<Particle>();

        paddleRect = new RectF();

        // Setup Paints
        paintHud = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintHud.setColor(Color.parseColor("#1A1A28"));

        paintPaddle = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintPaddle.setColor(Color.parseColor("#00E5FF"));

        paintBall = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintBall.setColor(Color.WHITE);

        paintBrick = new Paint(Paint.ANTI_ALIAS_FLAG);

        paintParticle = new Paint(Paint.ANTI_ALIAS_FLAG);

        paintText = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintText.setTextAlign(Paint.Align.CENTER);

        paintOverlay = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintOverlay.setColor(Color.argb(180, 10, 10, 20));

        setFocusable(true);
    }

    public void setGameStateListener(GameStateListener listener) {
        this.gameStateListener = listener;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        screenWidth = getWidth();
        screenHeight = getHeight();

        setupGameDimensions();
        resetGame(true);

        resumeGameThread();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        screenWidth = width;
        screenHeight = height;
        setupGameDimensions();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        pauseGameThread();
    }

    private void setupGameDimensions() {
        if (screenWidth <= 0 || screenHeight <= 0) return;

        paddleWidth = screenWidth * 0.22f;
        paddleHeight = screenHeight * 0.022f;
        paddleY = screenHeight * 0.88f;

        ballRadius = screenWidth * 0.022f;
        baseBallSpeed = screenHeight * 0.009f;
    }

    public void startNewGame() {
        score = 0;
        lives = 3;
        currentLevel = 1;
        resetGame(false);
        gameState = STATE_PLAYING;
        notifyListener();
    }

    public void nextLevel() {
        currentLevel++;
        resetGame(false);
        gameState = STATE_PLAYING;
        notifyListener();
    }

    private void resetGame(boolean fullReset) {
        if (fullReset) {
            score = 0;
            lives = 3;
            currentLevel = 1;
        }

        // Center paddle
        paddleX = (screenWidth - paddleWidth) / 2f;
        updatePaddleRect();

        // Place ball on paddle
        resetBall();

        // Build Bricks layout
        createBricksForLevel(currentLevel);
        particles.clear();
    }

    private void resetBall() {
        ballX = paddleX + paddleWidth / 2f;
        ballY = paddleY - ballRadius - 4f;

        float speedMultiplier = 1.0f + (currentLevel - 1) * 0.12f;
        float currentSpeed = baseBallSpeed * speedMultiplier;

        // Angle pointing upwards diagonally
        double angleRad = Math.toRadians(-45.0 - (Math.random() * 90.0));
        ballSpeedX = (float) (currentSpeed * Math.cos(angleRad));
        ballSpeedY = (float) (currentSpeed * Math.sin(angleRad));

        // Ensure vertical velocity is sufficient
        if (Math.abs(ballSpeedY) < currentSpeed * 0.5f) {
            ballSpeedY = -currentSpeed * 0.7f;
        }
    }

    private void createBricksForLevel(int level) {
        bricks.clear();
        if (screenWidth <= 0 || screenHeight <= 0) return;

        int rows = Math.min(4 + level, 8);
        int cols = 7;

        float topMargin = screenHeight * 0.12f;
        float sideMargin = screenWidth * 0.04f;
        float availableWidth = screenWidth - (sideMargin * 2);

        float brickSpacing = 6f;
        float brickWidth = (availableWidth - (brickSpacing * (cols - 1))) / cols;
        float brickHeight = screenHeight * 0.028f;

        int[] brickColors = new int[]{
                Color.parseColor("#FF3366"), // Red
                Color.parseColor("#FF9900"), // Orange
                Color.parseColor("#FFCC00"), // Yellow
                Color.parseColor("#33CC66"), // Green
                Color.parseColor("#00CCFF"), // Cyan
                Color.parseColor("#CC33FF")  // Purple
        };

        for (int r = 0; r < rows; r++) {
            int colorIndex = (r + level) % brickColors.length;
            int hitsRequired = (r == 0 && level > 1) ? 2 : 1;
            int points = (rows - r) * 10 * level;

            for (int c = 0; c < cols; c++) {
                float left = sideMargin + c * (brickWidth + brickSpacing);
                float top = topMargin + r * (brickHeight + brickSpacing);
                float right = left + brickWidth;
                float bottom = top + brickHeight;

                Brick brick = new Brick(left, top, right, bottom, brickColors[colorIndex], hitsRequired, points);
                bricks.add(brick);
            }
        }
        totalBricksCount = bricks.size();
    }

    private void updatePaddleRect() {
        paddleRect.set(paddleX, paddleY, paddleX + paddleWidth, paddleY + paddleHeight);
    }

    @Override
    public void run() {
        long targetFpsMs = 1000 / 60; // 60 FPS
        while (isRunning) {
            long startTime = SystemClock.uptimeMillis();

            if (surfaceHolder.getSurface().isValid()) {
                Canvas canvas = surfaceHolder.lockCanvas();
                if (canvas != null) {
                    try {
                        synchronized (surfaceHolder) {
                            if (gameState == STATE_PLAYING) {
                                updatePhysics();
                            }
                            updateParticles();
                            drawGame(canvas);
                        }
                    } finally {
                        surfaceHolder.unlockCanvasAndPost(canvas);
                    }
                }
            }

            long timeTaken = SystemClock.uptimeMillis() - startTime;
            long sleepTime = targetFpsMs - timeTaken;
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    private void updatePhysics() {
        // Ball movement
        ballX += ballSpeedX;
        ballY += ballSpeedY;

        // Wall collisions (Left & Right)
        if (ballX - ballRadius <= 0) {
            ballX = ballRadius;
            ballSpeedX = -ballSpeedX;
        } else if (ballX + ballRadius >= screenWidth) {
            ballX = screenWidth - ballRadius;
            ballSpeedX = -ballSpeedX;
        }

        // Top Wall Collision
        float hudHeight = screenHeight * 0.08f;
        if (ballY - ballRadius <= hudHeight) {
            ballY = hudHeight + ballRadius;
            ballSpeedY = -ballSpeedY;
        }

        // Paddle Collision
        if (ballY + ballRadius >= paddleY && ballY - ballRadius <= paddleY + paddleHeight) {
            if (ballX + ballRadius >= paddleX && ballX - ballRadius <= paddleX + paddleWidth) {
                // Angle bounce based on hit point on paddle
                float hitOffset = (ballX - (paddleX + paddleWidth / 2f)) / (paddleWidth / 2f);
                hitOffset = Math.max(-1.0f, Math.min(1.0f, hitOffset));

                float totalSpeed = (float) Math.hypot(ballSpeedX, ballSpeedY);
                double maxBounceAngle = Math.toRadians(60.0);
                double bounceAngle = hitOffset * maxBounceAngle;

                ballSpeedX = (float) (totalSpeed * Math.sin(bounceAngle));
                ballSpeedY = (float) (-totalSpeed * Math.cos(bounceAngle));

                ballY = paddleY - ballRadius - 2f;
                performHaptic();
            }
        }

        // Bottom Boundary (Life Lost)
        if (ballY - ballRadius > screenHeight) {
            lives--;
            notifyListener();
            performHaptic();

            if (lives <= 0) {
                gameState = STATE_GAME_OVER;
                checkHighScore();
                if (gameStateListener != null) {
                    gameStateListener.onGameOver(score);
                }
            } else {
                resetBall();
            }
            return;
        }

        // Brick Collisions
        boolean allCleared = true;
        for (int i = 0; i < bricks.size(); i++) {
            Brick brick = bricks.get(i);
            if (brick.isDestroyed()) continue;

            allCleared = false;
            RectF r = brick.getRect();

            // Check if ball intersects brick
            if (RectF.intersects(r, new RectF(ballX - ballRadius, ballY - ballRadius, ballX + ballRadius, ballY + ballRadius))) {
                if (brick.hit()) {
                    score += brick.getPoints();
                    spawnParticles(r.centerX(), r.centerY(), brick.getColor());
                    performHaptic();
                    if (score > highScore) {
                        highScore = score;
                    }
                    notifyListener();
                }

                // Bounce logic from brick boundary
                float overlapLeft = (ballX + ballRadius) - r.left;
                float overlapRight = r.right - (ballX - ballRadius);
                float overlapTop = (ballY + ballRadius) - r.top;
                float overlapBottom = r.bottom - (ballY - ballRadius);

                float minOverlap = Math.min(Math.min(overlapLeft, overlapRight), Math.min(overlapTop, overlapBottom));

                if (minOverlap == overlapLeft || minOverlap == overlapRight) {
                    ballSpeedX = -ballSpeedX;
                } else {
                    ballSpeedY = -ballSpeedY;
                }
                break;
            }
        }

        if (allCleared && totalBricksCount > 0) {
            gameState = STATE_LEVEL_CLEARED;
            checkHighScore();
            if (gameStateListener != null) {
                gameStateListener.onLevelCleared(currentLevel + 1);
            }
        }
    }

    private void spawnParticles(float x, float y, int color) {
        for (int i = 0; i < 12; i++) {
            particles.add(new Particle(x, y, color));
        }
    }

    private void updateParticles() {
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            if (!p.update()) {
                particles.remove(i);
            }
        }
    }

    private void drawGame(Canvas canvas) {
        // Background
        canvas.drawColor(Color.parseColor("#12121A"));

        // Draw HUD Bar background
        float hudHeight = screenHeight * 0.08f;
        paintHud.setColor(Color.parseColor("#1A1A28"));
        canvas.drawRect(0, 0, screenWidth, hudHeight, paintHud);

        // HUD Text
        paintText.setTextSize(screenHeight * 0.026f);
        paintText.setColor(Color.parseColor("#FFCC00"));
        paintText.setTextAlign(Paint.Align.LEFT);

        // Level (L1, L2, etc)
        canvas.drawText("LVL " + currentLevel, screenWidth * 0.04f, hudHeight * 0.65f, paintText);

        // Score (520)
        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setColor(Color.WHITE);
        canvas.drawText("SCORE: " + score, screenWidth * 0.5f, hudHeight * 0.65f, paintText);

        // Lives visual (Hearts ❤️)
        paintText.setTextAlign(Paint.Align.RIGHT);
        StringBuilder hearts = new StringBuilder();
        for (int i = 0; i < lives; i++) {
            hearts.append("❤️ ");
        }
        canvas.drawText(hearts.toString().trim(), screenWidth * 0.96f, hudHeight * 0.65f, paintText);

        // Draw HUD Divider Line
        paintHud.setColor(Color.parseColor("#33334D"));
        paintHud.setStrokeWidth(3f);
        canvas.drawLine(0, hudHeight, screenWidth, hudHeight, paintHud);

        // Draw Bricks
        for (int i = 0; i < bricks.size(); i++) {
            Brick brick = bricks.get(i);
            if (!brick.isDestroyed()) {
                RectF r = brick.getRect();
                paintBrick.setColor(brick.getColor());
                paintBrick.setAlpha(brick.getOpacityAlpha());
                canvas.drawRoundRect(r, 6f, 6f, paintBrick);
            }
        }

        // Draw Particles
        for (int i = 0; i < particles.size(); i++) {
            particles.get(i).draw(canvas, paintParticle);
        }

        // Draw Paddle
        paintPaddle.setShader(new LinearGradient(
                paddleRect.left, paddleRect.top, paddleRect.right, paddleRect.bottom,
                Color.parseColor("#00E5FF"), Color.parseColor("#0088FF"),
                Shader.TileMode.CLAMP
        ));
        canvas.drawRoundRect(paddleRect, paddleHeight / 2f, paddleHeight / 2f, paintPaddle);

        // Draw Ball
        paintBall.setColor(Color.WHITE);
        canvas.drawCircle(ballX, ballY, ballRadius, paintBall);

        // Draw Overlay Screens if not active playing
        if (gameState == STATE_START) {
            drawOverlayMessage(canvas, "BRICK BREAKER", "Touch to Launch / tap 'PLAY'");
        } else if (gameState == STATE_PAUSED) {
            drawOverlayMessage(canvas, "PAUSED", "Tap anywhere to resume");
        } else if (gameState == STATE_GAME_OVER) {
            drawOverlayMessage(canvas, "GAME OVER", "Final Score: " + score);
        } else if (gameState == STATE_LEVEL_CLEARED) {
            drawOverlayMessage(canvas, "STAGE CLEARED!", "Get Ready for Level " + (currentLevel + 1));
        }

        // Bottom retro aesthetic label border
        float bottomLabelY = screenHeight * 0.96f;
        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setTextSize(screenHeight * 0.018f);
        paintText.setColor(Color.parseColor("#777799"));
        canvas.drawText("RETRO ARCADE CLASSIC - TOUCH & DRAG TO MOVE", screenWidth * 0.5f, bottomLabelY, paintText);
    }

    private void drawOverlayMessage(Canvas canvas, String title, String subtitle) {
        canvas.drawRect(0, 0, screenWidth, screenHeight, paintOverlay);

        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setTextSize(screenHeight * 0.045f);
        paintText.setColor(Color.parseColor("#FFCC00"));
        canvas.drawText(title, screenWidth * 0.5f, screenHeight * 0.45f, paintText);

        paintText.setTextSize(screenHeight * 0.024f);
        paintText.setColor(Color.WHITE);
        canvas.drawText(subtitle, screenWidth * 0.5f, screenHeight * 0.52f, paintText);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float touchX = event.getX();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (gameState == STATE_START) {
                    gameState = STATE_PLAYING;
                    notifyListener();
                    return true;
                } else if (gameState == STATE_PAUSED) {
                    gameState = STATE_PLAYING;
                    notifyListener();
                    return true;
                } else if (gameState == STATE_GAME_OVER) {
                    startNewGame();
                    return true;
                } else if (gameState == STATE_LEVEL_CLEARED) {
                    nextLevel();
                    return true;
                }
                // Clamp paddle position to touch location
                paddleX = touchX - (paddleWidth / 2f);
                clampPaddle();
                updatePaddleRect();
                return true;

            case MotionEvent.ACTION_MOVE:
                if (gameState == STATE_PLAYING) {
                    paddleX = touchX - (paddleWidth / 2f);
                    clampPaddle();
                    updatePaddleRect();
                }
                return true;
        }

        return super.onTouchEvent(event);
    }

    private void clampPaddle() {
        if (paddleX < 0) {
            paddleX = 0;
        } else if (paddleX + paddleWidth > screenWidth) {
            paddleX = screenWidth - paddleWidth;
        }
    }

    private void checkHighScore() {
        if (score > highScore) {
            highScore = score;
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("high_score", highScore);
            editor.apply();
        }
    }

    private void notifyListener() {
        if (gameStateListener != null) {
            post(new Runnable() {
                @Override
                public void run() {
                    gameStateListener.onScoreUpdated(score, highScore, lives, currentLevel);
                }
            });
        }
    }

    private void performHaptic() {
        try {
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        } catch (Exception ignored) {
        }
    }

    public void pauseGameThread() {
        isRunning = false;
        if (gameThread != null) {
            try {
                gameThread.join();
            } catch (InterruptedException ignored) {
            }
            gameThread = null;
        }
    }

    public void resumeGameThread() {
        if (!isRunning) {
            isRunning = true;
            gameThread = new Thread(this);
            gameThread.start();
        }
    }

    public int getGameState() {
        return gameState;
    }

    public void setGameState(int state) {
        this.gameState = state;
    }
}