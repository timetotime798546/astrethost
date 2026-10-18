package com.pahadirunner.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Random;

public class GameView extends View {

    public interface GameListener {
        void onGameOver(int coinsCollected, int distance);
        void onCoinCollected(int totalCoins);
    }

    private GameListener listener;
    private Handler gameHandler;
    private Runnable gameLoopRunnable;
    private boolean isPlaying = false;

    // Paints
    private Paint skyPaint;
    private Paint farMountainPaint;
    private Paint nearMountainPaint;
    private Paint groundPaint;
    private Paint grassPaint;
    private Paint runnerPaint;
    private Paint coinPaint;
    private Paint coinInnerPaint;
    private Paint rockPaint;
    private Paint textPaint;

    // Game variables
    private float screenWidth = 0;
    private float screenHeight = 0;
    private float groundY = 0;

    // Runner Character
    private float runnerX = 120;
    private float runnerY = 0;
    private float runnerVelocityY = 0;
    private boolean isJumping = false;
    private float gravity = 1.6f;
    private float jumpStrength = -28f;
    private float legAngle = 0;
    private boolean legForward = true;

    // Obstacles and Coins
    private static class Obstacle {
        float x;
        float width = 50;
        float height = 55;
    }

    private static class CoinItem {
        float x;
        float y;
        boolean collected = false;
        int value = 50; // Each coin gives ₹50 coins!
    }

    private ArrayList<Obstacle> obstacles = new ArrayList<Obstacle>();
    private ArrayList<CoinItem> coins = new ArrayList<CoinItem>();
    private Random random = new Random();

    private float scrollOffset = 0;
    private float gameSpeed = 9f;
    private int distanceMeters = 0;
    private int sessionCoins = 0;

    public GameView(Context context) {
        super(context);
        init();
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        gameHandler = new Handler(Looper.getMainLooper());

        skyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        farMountainPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        farMountainPaint.setColor(Color.parseColor("#4A6572"));

        nearMountainPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        nearMountainPaint.setColor(Color.parseColor("#2E7D32"));

        groundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        groundPaint.setColor(Color.parseColor("#4E342E"));

        grassPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        grassPaint.setColor(Color.parseColor("#388E3C"));
        grassPaint.setStrokeWidth(12);

        runnerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        runnerPaint.setColor(Color.parseColor("#D84315"));

        coinPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        coinPaint.setColor(Color.parseColor("#FFD700"));

        coinInnerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        coinInnerPaint.setColor(Color.parseColor("#FF6F00"));
        coinInnerPaint.setTextSize(22);
        coinInnerPaint.setTextAlign(Paint.Align.CENTER);
        coinInnerPaint.setFakeBoldText(true);

        rockPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rockPaint.setColor(Color.parseColor("#37474F"));

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(36);
        textPaint.setFakeBoldText(true);

        gameLoopRunnable = new Runnable() {
            @Override
            public void run() {
                if (isPlaying) {
                    updatePhysics();
                    invalidate();
                    gameHandler.postDelayed(this, 16); // ~60 FPS
                }
            }
        };
    }

    public void setGameListener(GameListener l) {
        this.listener = l;
    }

    public void startRun() {
        isPlaying = true;
        obstacles.clear();
        coins.clear();
        runnerVelocityY = 0;
        isJumping = false;
        scrollOffset = 0;
        distanceMeters = 0;
        sessionCoins = 0;

        // Prepopulate first items ahead
        spawnInitialElements();

        gameHandler.removeCallbacks(gameLoopRunnable);
        gameHandler.post(gameLoopRunnable);
    }

    public void stopRun() {
        isPlaying = false;
        gameHandler.removeCallbacks(gameLoopRunnable);
    }

    public void jump() {
        if (!isJumping) {
            runnerVelocityY = jumpStrength;
            isJumping = true;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            jump();
            return true;
        }
        return super.onTouchEvent(event);
    }

    private void spawnInitialElements() {
        for (int i = 1; i <= 4; i++) {
            float dist = 400 + i * 450;
            if (i % 2 == 0) {
                Obstacle obs = new Obstacle();
                obs.x = dist;
                obstacles.add(obs);
            } else {
                CoinItem c = new CoinItem();
                c.x = dist;
                c.y = groundY - 80;
                coins.add(c);
            }
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        screenWidth = w;
        screenHeight = h;
        groundY = h * 0.75f;
        runnerY = groundY;
    }

    private void updatePhysics() {
        scrollOffset += gameSpeed;
        distanceMeters = (int) (scrollOffset / 10);

        // Runner Jumping & Gravity
        if (isJumping) {
            runnerY += runnerVelocityY;
            runnerVelocityY += gravity;

            if (runnerY >= groundY) {
                runnerY = groundY;
                runnerVelocityY = 0;
                isJumping = false;
            }
        }

        // Runner Leg animation
        if (!isJumping) {
            if (legForward) {
                legAngle += 6;
                if (legAngle > 30) legForward = false;
            } else {
                legAngle -= 6;
                if (legAngle < -30) legForward = true;
            }
        }

        // Move Obstacles
        for (int i = obstacles.size() - 1; i >= 0; i--) {
            Obstacle obs = obstacles.get(i);
            obs.x -= gameSpeed;

            // Collision check with Runner (Runner size approx 50x70)
            float runnerLeft = runnerX - 25;
            float runnerRight = runnerX + 25;
            float runnerBottom = runnerY;
            float runnerTop = runnerY - 70;

            float obsLeft = obs.x;
            float obsRight = obs.x + obs.width;
            float obsTop = groundY - obs.height;
            float obsBottom = groundY;

            if (runnerRight > obsLeft + 10 && runnerLeft < obsRight - 10 && runnerBottom > obsTop + 10) {
                // Collision!
                isPlaying = false;
                if (listener != null) {
                    listener.onGameOver(sessionCoins, distanceMeters);
                }
                return;
            }

            if (obs.x < -100) {
                obstacles.remove(i);
            }
        }

        // Move Coins
        for (int i = coins.size() - 1; i >= 0; i--) {
            CoinItem c = coins.get(i);
            c.x -= gameSpeed;

            if (!c.collected) {
                float runnerLeft = runnerX - 30;
                float runnerRight = runnerX + 30;
                float runnerTop = runnerY - 80;
                float runnerBottom = runnerY;

                if (c.x > runnerLeft && c.x < runnerRight && c.y > runnerTop && c.y < runnerBottom) {
                    c.collected = true;
                    sessionCoins += c.value;
                    if (listener != null) {
                        listener.onCoinCollected(sessionCoins);
                    }
                }
            }

            if (c.x < -100) {
                coins.remove(i);
            }
        }

        // Spawn new objects randomly
        if (random.nextInt(45) == 0) {
            float lastX = screenWidth;
            if (!obstacles.isEmpty()) {
                lastX = Math.max(lastX, obstacles.get(obstacles.size() - 1).x);
            }
            if (!coins.isEmpty()) {
                lastX = Math.max(lastX, coins.get(coins.size() - 1).x);
            }

            if (lastX < screenWidth + 350) {
                if (random.nextBoolean()) {
                    Obstacle obs = new Obstacle();
                    obs.x = screenWidth + 200 + random.nextInt(200);
                    obstacles.add(obs);
                } else {
                    CoinItem c = new CoinItem();
                    c.x = screenWidth + 200 + random.nextInt(200);
                    c.y = groundY - 60 - random.nextInt(70);
                    coins.add(c);
                }
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (screenWidth == 0 || screenHeight == 0) return;

        // 1. Sky
        canvas.drawColor(Color.parseColor("#B3E5FC"));

        // 2. Distant Pahad (Mountains) - parallax 0.2
        drawMountains(canvas, farMountainPaint, 0.2f, groundY - 140, 220);

        // 3. Near Pahad (Green hills) - parallax 0.5
        drawMountains(canvas, nearMountainPaint, 0.5f, groundY - 50, 140);

        // 4. Ground and grass
        canvas.drawRect(0, groundY, screenWidth, screenHeight, groundPaint);
        canvas.drawLine(0, groundY, screenWidth, groundY, grassPaint);

        // 5. Draw Obstacles (Rocks on the hills)
        for (int i = 0; i < obstacles.size(); i++) {
            Obstacle obs = obstacles.get(i);
            drawRock(canvas, obs.x, groundY, obs.width, obs.height);
        }

        // 6. Draw ₹50 Coins
        for (int i = 0; i < coins.size(); i++) {
            CoinItem c = coins.get(i);
            if (!c.collected) {
                drawCoin(canvas, c.x, c.y);
            }
        }

        // 7. Draw Pahadi Runner Character
        drawRunner(canvas, runnerX, runnerY);

        // 8. In-Game HUD
        textPaint.setColor(Color.parseColor("#0D3B13"));
        textPaint.setTextSize(34);
        canvas.drawText("दूरी (Dist): " + distanceMeters + "m", 30, 60, textPaint);

        textPaint.setColor(Color.parseColor("#E65100"));
        canvas.drawText("सिक्के (Coins): ₹" + sessionCoins, 30, 110, textPaint);
    }

    private void drawMountains(Canvas canvas, Paint paint, float speedRatio, float baseY, float peakHeight) {
        Path path = new Path();
        path.moveTo(0, screenHeight);
        path.lineTo(0, baseY);

        float mountainWidth = 320;
        float shift = (scrollOffset * speedRatio) % mountainWidth;

        for (float x = -shift - mountainWidth; x < screenWidth + mountainWidth; x += mountainWidth) {
            path.lineTo(x + mountainWidth / 2, baseY - peakHeight);
            path.lineTo(x + mountainWidth, baseY);
        }

        path.lineTo(screenWidth, screenHeight);
        path.close();
        canvas.drawPath(path, paint);
    }

    private void drawRock(Canvas canvas, float x, float ground, float w, float h) {
        Path path = new Path();
        path.moveTo(x, ground);
        path.lineTo(x + w * 0.2f, ground - h * 0.8f);
        path.lineTo(x + w * 0.6f, ground - h);
        path.lineTo(x + w, ground - h * 0.4f);
        path.lineTo(x + w * 0.9f, ground);
        path.close();
        canvas.drawPath(path, rockPaint);
    }

    private void drawCoin(Canvas canvas, float x, float y) {
        // Outer glowing gold coin
        canvas.drawCircle(x, y, 22, coinPaint);
        // Inner text '₹50'
        canvas.drawText("₹", x, y + 8, coinInnerPaint);
    }

    private void drawRunner(Canvas canvas, float x, float y) {
        // Head
        runnerPaint.setColor(Color.parseColor("#FFA726"));
        canvas.drawCircle(x, y - 65, 14, runnerPaint);

        // Pahadi Cap (Himachali / Pahadi Topi)
        runnerPaint.setColor(Color.parseColor("#2E7D32"));
        RectF cap = new RectF(x - 16, y - 82, x + 16, y - 66);
        canvas.drawRect(cap, runnerPaint);
        runnerPaint.setColor(Color.parseColor("#C62828"));
        canvas.drawRect(x - 16, y - 72, x + 16, y - 66, runnerPaint);

        // Torso / Jacket
        runnerPaint.setColor(Color.parseColor("#1565C0"));
        canvas.drawRect(x - 12, y - 50, x + 12, y - 25, runnerPaint);

        // Legs
        Paint legPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        legPaint.setColor(Color.parseColor("#37474F"));
        legPaint.setStrokeWidth(6);

        if (isJumping) {
            // Tucked legs during jump
            canvas.drawLine(x - 6, y - 25, x - 15, y - 10, legPaint);
            canvas.drawLine(x + 6, y - 25, x + 15, y - 10, legPaint);
        } else {
            // Running leg swing
            float leg1Offset = (float) Math.sin(Math.toRadians(legAngle)) * 18;
            float leg2Offset = (float) Math.sin(Math.toRadians(-legAngle)) * 18;

            canvas.drawLine(x - 6, y - 25, x - 6 + leg1Offset, y, legPaint);
            canvas.drawLine(x + 6, y - 25, x + 6 + leg2Offset, y, legPaint);
        }
    }
}