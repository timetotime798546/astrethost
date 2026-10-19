package com.trirunner.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class GameView extends View {

    public interface GameListener {
        void onCrashOccurred();
        void onScoreUpdated(int distanceMeters);
    }

    private static final int LANE_COUNT = 3;
    private int currentLane = 1; // 0: Left, 1: Center, 2: Right
    private float targetPlayerX = 0f;
    private float currentPlayerX = 0f;

    private final Paint lanePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dividerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint playerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint obstaclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint crashFlashPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private static class Obstacle {
        int lane;
        float y;
        float height;
        int color;
    }

    private final List<Obstacle> obstacles = new ArrayList<Obstacle>();
    private final Random random = new Random();

    private boolean isRunning = false;
    private boolean isCrashing = false;
    private int crashFlashFrames = 0;

    private float gameSpeed = 16f;
    private int distanceTraveled = 0;
    private long lastSpawnTime = 0;
    private GameListener gameListener;

    private final Handler gameLoopHandler = new Handler(Looper.getMainLooper());
    private final Runnable gameLoopRunnable = new Runnable() {
        @Override
        public void run() {
            if (isRunning) {
                updateGameState();
                invalidate();
                gameLoopHandler.postDelayed(this, 16); // ~60fps
            }
        }
    };

    public GameView(Context context) {
        super(context);
        init();
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        lanePaint.setColor(Color.parseColor("#131B2E"));
        dividerPaint.setColor(Color.parseColor("#334155"));
        dividerPaint.setStrokeWidth(4f);
        dividerPaint.setPathEffect(new DashPathEffect(new float[]{30f, 20f}, 0f));

        playerPaint.setColor(Color.parseColor("#38BDF8"));
        obstaclePaint.setColor(Color.parseColor("#EF4444"));
        shadowPaint.setColor(Color.argb(80, 0, 0, 0));
        crashFlashPaint.setColor(Color.argb(160, 239, 68, 68));
    }

    public void setGameListener(GameListener listener) {
        this.gameListener = listener;
    }

    public void startGame() {
        obstacles.clear();
        currentLane = 1;
        distanceTraveled = 0;
        gameSpeed = 16f;
        isRunning = true;
        isCrashing = false;
        crashFlashFrames = 0;
        lastSpawnTime = System.currentTimeMillis();
        gameLoopHandler.post(gameLoopRunnable);
    }

    public void stopGame() {
        isRunning = false;
        gameLoopHandler.removeCallbacks(gameLoopRunnable);
    }

    public void moveLeft() {
        if (currentLane > 0 && !isCrashing) {
            currentLane--;
        }
    }

    public void moveRight() {
        if (currentLane < LANE_COUNT - 1 && !isCrashing) {
            currentLane++;
        }
    }

    private void updateGameState() {
        float width = getWidth();
        float height = getHeight();
        if (width <= 0 || height <= 0) return;

        float laneWidth = width / LANE_COUNT;
        targetPlayerX = (currentLane * laneWidth) + (laneWidth / 2f);
        currentPlayerX += (targetPlayerX - currentPlayerX) * 0.25f;

        // Increase distance & progressive speed
        distanceTraveled += 1;
        if (distanceTraveled % 10 == 0 && gameListener != null) {
            gameListener.onScoreUpdated(distanceTraveled / 10);
        }
        if (gameSpeed < 32f) {
            gameSpeed += 0.002f;
        }

        // Spawn obstacles
        long now = System.currentTimeMillis();
        long spawnInterval = Math.max(700, 1500 - (long)(gameSpeed * 25));
        if (now - lastSpawnTime > spawnInterval) {
            lastSpawnTime = now;
            Obstacle obs = new Obstacle();
            obs.lane = random.nextInt(LANE_COUNT);
            obs.y = -80f;
            obs.height = 70f;
            obs.color = (random.nextBoolean()) ? Color.parseColor("#EF4444") : Color.parseColor("#F59E0B");
            obstacles.add(obs);
        }

        float playerY = height - 200f;
        float playerRadius = laneWidth * 0.22f;

        // Move obstacles & detect collisions
        Iterator<Obstacle> iter = obstacles.iterator();
        while (iter.hasNext()) {
            Obstacle obs = iter.next();
            obs.y += gameSpeed;

            // Check collision with player
            float obsCenterX = (obs.lane * laneWidth) + (laneWidth / 2f);
            if (obs.y + obs.height >= playerY - playerRadius && obs.y <= playerY + playerRadius) {
                if (Math.abs(currentPlayerX - obsCenterX) < laneWidth * 0.45f) {
                    // Collision occurred!
                    triggerCrash();
                    iter.remove();
                    continue;
                }
            }

            if (obs.y > height + 100f) {
                iter.remove();
            }
        }

        if (crashFlashFrames > 0) {
            crashFlashFrames--;
            if (crashFlashFrames == 0) {
                isCrashing = false;
            }
        }
    }

    private void triggerCrash() {
        isCrashing = true;
        crashFlashFrames = 15;
        if (gameListener != null) {
            gameListener.onCrashOccurred();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth();
        float height = getHeight();
        if (width <= 0 || height <= 0) return;

        float laneWidth = width / LANE_COUNT;

        // Draw Lanes background
        canvas.drawRect(0, 0, width, height, lanePaint);

        // Draw Lane Dividers
        for (int i = 1; i < LANE_COUNT; i++) {
            float lineX = i * laneWidth;
            canvas.drawLine(lineX, 0, lineX, height, dividerPaint);
        }

        // Draw Obstacles
        for (Obstacle obs : obstacles) {
            float obsCenterX = (obs.lane * laneWidth) + (laneWidth / 2f);
            float obsWidth = laneWidth * 0.65f;
            obstaclePaint.setColor(obs.color);

            RectF obsRect = new RectF(
                    obsCenterX - (obsWidth / 2f),
                    obs.y,
                    obsCenterX + (obsWidth / 2f),
                    obs.y + obs.height
            );
            canvas.drawRoundRect(obsRect, 16f, 16f, obstaclePaint);

            // Warning stripes on obstacle
            Paint stripePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            stripePaint.setColor(Color.argb(120, 0, 0, 0));
            canvas.drawCircle(obsCenterX, obs.y + (obs.height / 2f), 12f, stripePaint);
        }

        // Draw Player
        float playerY = height - 200f;
        float playerRadius = laneWidth * 0.22f;

        // Player shadow
        canvas.drawOval(
                currentPlayerX - (playerRadius * 1.1f),
                playerY + (playerRadius * 0.7f),
                currentPlayerX + (playerRadius * 1.1f),
                playerY + (playerRadius * 1.3f),
                shadowPaint
        );

        // Player Ship / Orb
        if (isCrashing) {
            playerPaint.setColor(Color.parseColor("#F87171"));
        } else {
            playerPaint.setColor(Color.parseColor("#38BDF8"));
        }
        canvas.drawCircle(currentPlayerX, playerY, playerRadius, playerPaint);

        // Inner glowing core
        Paint corePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        corePaint.setColor(Color.WHITE);
        canvas.drawCircle(currentPlayerX, playerY - (playerRadius * 0.15f), playerRadius * 0.45f, corePaint);

        // Crash flash overlay
        if (crashFlashFrames > 0) {
            canvas.drawRect(0, 0, width, height, crashFlashPaint);
        }
    }

    private float touchStartX = 0f;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchStartX = event.getX();
                return true;
            case MotionEvent.ACTION_UP:
                float deltaX = event.getX() - touchStartX;
                if (Math.abs(deltaX) > 50) {
                    if (deltaX > 0) {
                        moveRight();
                    } else {
                        moveLeft();
                    }
                } else {
                    // Tap on screen left/right
                    if (event.getX() < getWidth() / 2f) {
                        moveLeft();
                    } else {
                        moveRight();
                    }
                }
                return true;
        }
        return super.onTouchEvent(event);
    }
}