package com.carracerpro.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Vibrator;
import android.view.Choreographer;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.Random;

public class GameView extends View {

    public interface GameListener {
        void onScoreUpdated(int score, int currentSpeed, int currentFuel, int level);
        void onGameOver(int score, int coinsCollected, int maxSpeedReached);
        void playCoinSfx();
        void playLevelUpSfx();
        void playCrashSfx();
    }

    private GameListener gameListener;
    private boolean isPlaying = false;
    private Random random = new Random();

    // Game Parameters
    private int score = 0;
    private int level = 1;
    private int coinsCollected = 0;
    private int targetSpeed = 100; // base visual speed
    private int currentSpeed = 100;
    private float fuel = 100.0f;
    private float speedMultiplier = 1.0f;

    // Screen Dimensions
    private int screenWidth;
    private int screenHeight;

    // Player metrics
    private float playerX;
    private float playerY;
    private float playerWidth;
    private float playerHeight;
    private float touchTargetX;

    // Road parameters
    private float roadWidth;
    private float roadLeft;
    private float roadRight;
    private float roadStripOffset = 0;

    // Entity lists
    private ArrayList<ObstacleCar> obstacleCars = new ArrayList<ObstacleCar>();
    private ArrayList<Collectible> collectibles = new ArrayList<Collectible>();
    private ArrayList<Particle> particles = new ArrayList<Particle>();
    private ArrayList<TextEffect> textEffects = new ArrayList<TextEffect>();

    // Timings
    private long lastSpawnTime = 0;
    private long lastCollectibleSpawn = 0;
    private long lastLevelUpTime = 0;

    // Paints
    private Paint roadPaint;
    private Paint asphaltPaint;
    private Paint stripePaint;
    private Paint sidePaint;
    private Paint textPaint;
    private Paint indicatorPaint;

    // Screen Shake effect indicators
    private int shakeDuration = 0;
    private int shakeMagnitude = 0;

    public GameView(Context context) {
        super(context);
        initPaints();
    }

    public void setGameListener(GameListener listener) {
        this.gameListener = listener;
    }

    private void initPaints() {
        roadPaint = new Paint();
        roadPaint.setColor(Color.parseColor("#121626"));
        roadPaint.setStyle(Paint.Style.FILL);

        asphaltPaint = new Paint();
        asphaltPaint.setColor(Color.parseColor("#070914"));
        asphaltPaint.setStyle(Paint.Style.FILL);

        stripePaint = new Paint();
        stripePaint.setColor(Color.parseColor("#00FFFF"));
        stripePaint.setStyle(Paint.Style.STROKE);
        stripePaint.setStrokeWidth(6);

        sidePaint = new Paint();
        sidePaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(36f);
        textPaint.setFakeBoldText(true);
        textPaint.setAntiAlias(true);

        indicatorPaint = new Paint();
        indicatorPaint.setAntiAlias(true);
    }

    public void configureDifficulty(float multiplier) {
        this.speedMultiplier = multiplier;
        this.targetSpeed = (int) (100 * multiplier);
        this.currentSpeed = targetSpeed;
    }

    public void startNewGame() {
        score = 0;
        level = 1;
        coinsCollected = 0;
        fuel = 100.0f;
        obstacleCars.clear();
        collectibles.clear();
        particles.clear();
        textEffects.clear();
        lastSpawnTime = System.currentTimeMillis();
        lastCollectibleSpawn = System.currentTimeMillis();

        if (screenWidth > 0) {
            playerX = screenWidth / 2f;
            touchTargetX = playerX;
        }

        isPlaying = true;
        Choreographer.getInstance().postFrameCallback(frameCallback);
    }

    public void pauseGame() {
        isPlaying = false;
    }

    private final Choreographer.FrameCallback frameCallback = new Choreographer.FrameCallback() {
        @Override
        public void doFrame(long frameTimeNanos) {
            if (isPlaying) {
                updateLogic();
                invalidate();
                Choreographer.getInstance().postFrameCallback(this);
            }
        }
    };

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        screenWidth = w;
        screenHeight = h;

        roadWidth = w * 0.75f;
        roadLeft = (w - roadWidth) / 2f;
        roadRight = roadLeft + roadWidth;

        playerWidth = w * 0.12f;
        playerHeight = playerWidth * 1.8f;
        playerX = w / 2f;
        playerY = h - playerHeight - 140f;
        touchTargetX = playerX;
    }

    private void updateLogic() {
        long now = System.currentTimeMillis();

        // Engine score over time
        score++;
        if (score % 1000 == 0) {
            level++;
            targetSpeed += 20;
            if (gameListener != null) {
                gameListener.playLevelUpSfx();
            }
            textEffects.add(new TextEffect(screenWidth / 2f, screenHeight / 2f, "LEVEL UP!", Color.parseColor("#FF007F")));
        }

        // Level progression speeds
        if (currentSpeed < targetSpeed) {
            currentSpeed += 1;
        }

        // Fuel Depletion
        float baseFuelDrain = 0.04f;
        fuel -= (baseFuelDrain * speedMultiplier);
        if (fuel <= 0) {
            fuel = 0;
            triggerGameOver();
        }

        // Player drift transition
        float damping = 0.20f;
        playerX += (touchTargetX - playerX) * damping;

        // Keep Player inside bounds of the track
        float halfCar = playerWidth / 2;
        if (playerX - halfCar < roadLeft) {
            playerX = roadLeft + halfCar;
        }
        if (playerX + halfCar > roadRight) {
            playerX = roadRight - halfCar;
        }

        // Shift road dash strip marks downwards
        float roadScrollSpeed = (currentSpeed / 5f);
        roadStripOffset += roadScrollSpeed;
        if (roadStripOffset > 80) {
            roadStripOffset = 0;
        }

        // Spawns obstacle vehicles
        long spawnInterval = Math.max(1200 - (level * 100), 600);
        if (now - lastSpawnTime > spawnInterval) {
            spawnObstacleCar();
            lastSpawnTime = now;
        }

        // Spawns items (Coins / fuel)
        long collectibleInterval = 1800;
        if (now - lastCollectibleSpawn > collectibleInterval) {
            spawnCollectible();
            lastCollectibleSpawn = now;
        }

        // Update obstacles
        for (int i = obstacleCars.size() - 1; i >= 0; i--) {
            ObstacleCar car = obstacleCars.get(i);
            car.y += (roadScrollSpeed + car.extraSpeed);
            if (car.y - car.height > screenHeight) {
                obstacleCars.remove(i);
            } else {
                // Collision checker
                RectF playerBox = new RectF(playerX - playerWidth / 2, playerY, playerX + playerWidth / 2, playerY + playerHeight);
                RectF obstacleBox = new RectF(car.x - car.width / 2, car.y - car.height, car.x + car.width / 2, car.y);
                if (RectF.intersects(playerBox, obstacleBox)) {
                    triggerCrash();
                    return;
                }
            }
        }

        // Update collectibles
        for (int i = collectibles.size() - 1; i >= 0; i--) {
            Collectible item = collectibles.get(i);
            item.y += roadScrollSpeed;
            item.pulseTime += 0.1f;

            if (item.y - item.size > screenHeight) {
                collectibles.remove(i);
            } else {
                // Collision with item
                float distSq = (playerX - item.x) * (playerX - item.x) + (playerY + playerHeight/2f - item.y) * (playerY + playerHeight/2f - item.y);
                float radiusSum = (playerWidth/2f + item.size);
                if (distSq < radiusSum * radiusSum) {
                    triggerCollection(item);
                    collectibles.remove(i);
                }
            }
        }

        // Update Particles
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.x += p.vx;
            p.y += p.vy;
            p.life--;
            if (p.life <= 0) {
                particles.remove(i);
            }
        }

        // Update floating texts
        for (int i = textEffects.size() - 1; i >= 0; i--) {
            TextEffect t = textEffects.get(i);
            t.y -= 4f;
            t.life--;
            if (t.life <= 0) {
                textEffects.remove(i);
            }
        }

        // Screen Shake decrease
        if (shakeDuration > 0) {
            shakeDuration--;
        }

        // Realtime updates to Activity HUD
        if (gameListener != null) {
            gameListener.onScoreUpdated(score, currentSpeed, (int) fuel, level);
        }
    }

    private void spawnObstacleCar() {
        float carW = screenWidth * 0.12f;
        float carH = carW * 1.8f;
        // Choose random visual lane X coordinate
        float laneWidth = roadWidth / 3f;
        int laneIndex = random.nextInt(3);
        float spawnX = roadLeft + (laneIndex * laneWidth) + (laneWidth / 2f);
        float spawnY = -carH;
        float bonusSpeed = random.nextInt(10);

        // Random color styling
        int[] colors = {Color.parseColor("#FF007F"), Color.parseColor("#FFAA00"), Color.parseColor("#B026FF"), Color.parseColor("#00FF66")};
        int color = colors[random.nextInt(colors.length)];

        obstacleCars.add(new ObstacleCar(spawnX, spawnY, carW, carH, bonusSpeed, color));
    }

    private void spawnCollectible() {
        float size = screenWidth * 0.05f;
        float laneWidth = roadWidth / 3f;
        int laneIndex = random.nextInt(3);
        float spawnX = roadLeft + (laneIndex * laneWidth) + (laneWidth / 2f);
        float spawnY = -size;

        // 75% Coin, 25% fuel canister
        boolean isFuel = (random.nextFloat() < 0.25f) || (fuel < 40f && random.nextFloat() < 0.60f);
        collectibles.add(new Collectible(spawnX, spawnY, size, isFuel));
    }

    private void triggerCollection(Collectible item) {
        if (item.isFuel) {
            fuel = Math.min(fuel + 25f, 100f);
            textEffects.add(new TextEffect(item.x, item.y, "+FUEL!", Color.parseColor("#00FF66")));
            // spawn cyan sparks
            for (int i = 0; i < 10; i++) {
                particles.add(new Particle(item.x, item.y, Color.parseColor("#00FF66")));
            }
        } else {
            score += 150;
            coinsCollected++;
            textEffects.add(new TextEffect(item.x, item.y, "+150 XP", Color.parseColor("#FFAA00")));
            // spawn gold sparks
            for (int i = 0; i < 12; i++) {
                particles.add(new Particle(item.x, item.y, Color.parseColor("#FFAA00")));
            }
        }

        if (gameListener != null) {
            gameListener.playCoinSfx();
        }
    }

    private void triggerCrash() {
        isPlaying = false;
        shakeDuration = 25;
        shakeMagnitude = 18;

        if (gameListener != null) {
            gameListener.playCrashSfx();
        }

        // Fire & debris explosion explosion effects
        int[] explosionColors = {Color.RED, Color.YELLOW, Color.WHITE, Color.GRAY};
        for (int i = 0; i < 45; i++) {
            Particle p = new Particle(playerX, playerY + playerHeight/2f, explosionColors[random.nextInt(explosionColors.length)]);
            p.vx *= 3.5f;
            p.vy *= 3.5f;
            p.size *= 2f;
            particles.add(p);
        }

        // Vibrate if permitted
        try {
            Vibrator v = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null && v.hasVibrator()) {
                v.vibrate(350);
            }
        } catch (Exception e) {
            // Safe catch
        }

        // Keep rendering active during explosion freeze animation briefly
        postDelayed(new Runnable() {
            @Override
            public void run() {
                triggerGameOver();
            }
        }, 1200);
    }

    private void triggerGameOver() {
        isPlaying = false;
        if (gameListener != null) {
            gameListener.onGameOver(score, coinsCollected, currentSpeed);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Handle Screen Shake
        if (shakeDuration > 0) {
            int shakeX = random.nextInt(shakeMagnitude * 2) - shakeMagnitude;
            int shakeY = random.nextInt(shakeMagnitude * 2) - shakeMagnitude;
            canvas.translate(shakeX, shakeY);
        }

        // Background terrain
        canvas.drawColor(Color.parseColor("#070914"));

        // Scenery: Parallax visual side panels
        drawScenery(canvas);

        // Track Roadway asphalt background
        canvas.drawRect(roadLeft, 0, roadRight, screenHeight, roadPaint);

        // Outer road glowing boundaries
        int neonBoundaryColor = Color.parseColor("#00FFFF");
        if (level % 4 == 0) neonBoundaryColor = Color.parseColor("#FF007F");
        else if (level % 4 == 1) neonBoundaryColor = Color.parseColor("#00FFFF");
        else if (level % 4 == 2) neonBoundaryColor = Color.parseColor("#FFAA00");
        else if (level % 4 == 3) neonBoundaryColor = Color.parseColor("#00FF66");

        stripePaint.setColor(neonBoundaryColor);
        canvas.drawLine(roadLeft, 0, roadLeft, screenHeight, stripePaint);
        canvas.drawLine(roadRight, 0, roadRight, screenHeight, stripePaint);

        // Center moving strip markings
        drawRoadStripes(canvas);

        // Render Collectibles
        for (Collectible item : collectibles) {
            drawCollectible(canvas, item);
        }

        // Render Obstacle Cars
        for (ObstacleCar car : obstacleCars) {
            drawCar(canvas, car.x, car.y - car.height / 2f, car.width, car.height, car.color, false);
        }

        // Render Player Car (if alive / not collapsed)
        if (isPlaying || shakeDuration > 0) {
            drawCar(canvas, playerX, playerY + playerHeight / 2f, playerWidth, playerHeight, Color.parseColor("#FF007F"), true);
        }

        // Render Active particles
        for (Particle p : particles) {
            indicatorPaint.setColor(p.color);
            canvas.drawCircle(p.x, p.y, p.size, indicatorPaint);
        }

        // Render Active floating text effects
        for (TextEffect t : textEffects) {
            textPaint.setColor(t.color);
            textPaint.setAlpha((int) ((t.life / 40f) * 255));
            canvas.drawText(t.text, t.x - textPaint.measureText(t.text) / 2f, t.y, textPaint);
        }
        textPaint.setAlpha(255); // Reset alpha
    }

    private void drawRoadStripes(Canvas canvas) {
        Paint centerStripePaint = new Paint();
        centerStripePaint.setColor(Color.WHITE);
        centerStripePaint.setStyle(Paint.Style.FILL);

        float dashWidth = 10f;
        float dashHeight = 50f;
        float gapHeight = 40f;
        float laneWidth = roadWidth / 3f;

        // Draw dividers between lane 1/2 and 2/3
        for (int laneIdx = 1; laneIdx < 3; laneIdx++) {
            float dividerX = roadLeft + (laneIdx * laneWidth);
            float currentY = -80 + roadStripOffset;
            while (currentY < screenHeight) {
                canvas.drawRect(dividerX - dashWidth / 2f, currentY, dividerX + dashWidth / 2f, currentY + dashHeight, centerStripePaint);
                currentY += dashHeight + gapHeight;
            }
        }
    }

    private void drawScenery(Canvas canvas) {
        Paint sceneryPaint = new Paint();
        sceneryPaint.setColor(Color.parseColor("#161A2D"));
        sceneryPaint.setAntiAlias(true);

        // Dynamic procedurally spaced dark hills or visual side decor
        int sideRectWidth = (int) roadLeft;
        if (sideRectWidth > 0) {
            // Side barriers or details
            sceneryPaint.setColor(Color.parseColor("#0C0F1D"));
            canvas.drawRect(0, 0, roadLeft, screenHeight, sceneryPaint);
            canvas.drawRect(roadRight, 0, screenWidth, screenHeight, sceneryPaint);
        }
    }

    private void drawCar(Canvas canvas, float centerX, float centerY, float width, float height, int color, boolean isPlayer) {
        float left = centerX - width / 2f;
        float top = centerY - height / 2f;
        float right = centerX + width / 2f;
        float bottom = centerY + height / 2f;

        Paint paint = new Paint();
        paint.setAntiAlias(true);

        // Wheels drawing
        paint.setColor(Color.parseColor("#111111"));
        float wheelW = width * 0.16f;
        float wheelH = height * 0.22f;
        // Draw wheels slightly protruding
        canvas.drawRoundRect(new RectF(left - wheelW/2f, top + height * 0.15f, left + wheelW/2f, top + height * 0.15f + wheelH), 4, 4, paint);
        canvas.drawRoundRect(new RectF(right - wheelW/2f, top + height * 0.15f, right + wheelW/2f, top + height * 0.15f + wheelH), 4, 4, paint);
        canvas.drawRoundRect(new RectF(left - wheelW/2f, bottom - height * 0.35f, left + wheelW/2f, bottom - height * 0.35f + wheelH), 4, 4, paint);
        canvas.drawRoundRect(new RectF(right - wheelW/2f, bottom - height * 0.35f, right + wheelW/2f, bottom - height * 0.35f + wheelH), 4, 4, paint);

        // Draw spoiler (at the bottom/back end of car)
        paint.setColor(Color.parseColor("#2E303E"));
        canvas.drawRect(left - 4, bottom - height * 0.08f, right + 4, bottom, paint);

        // Draw Body chassis
        paint.setColor(color);
        canvas.drawRoundRect(new RectF(left, top + height * 0.05f, right, bottom - height * 0.05f), 12, 12, paint);

        // Draw aerodynamic cabin cockpit glass window
        paint.setColor(Color.parseColor("#E600E5FF"));
        float cabinW = width * 0.60f;
        float cabinH = height * 0.30f;
        canvas.drawRoundRect(new RectF(centerX - cabinW/2f, centerY - cabinH/2f, centerX + cabinW/2f, centerY + cabinH/1.5f), 8, 8, paint);

        // Headlights glow on front track (at top forward end of car)
        paint.setColor(Color.YELLOW);
        canvas.drawCircle(left + width * 0.25f, top + height * 0.05f, 6, paint);
        canvas.drawCircle(right - width * 0.25f, top + height * 0.05f, 6, paint);

        // Back brake taillights lights
        paint.setColor(Color.RED);
        canvas.drawRect(left + 8, bottom - height * 0.05f - 4, left + 20, bottom - height * 0.05f, paint);
        canvas.drawRect(right - 20, bottom - height * 0.05f - 4, right - 8, bottom - height * 0.05f, paint);

        // Exhaust fire particle traces if is player
        if (isPlayer && isPlaying) {
            paint.setColor(Color.parseColor("#FF3300"));
            canvas.drawRect(centerX - 8, bottom + 2, centerX - 2, bottom + 12 + random.nextInt(12), paint);
            canvas.drawRect(centerX + 2, bottom + 2, centerX + 8, bottom + 12 + random.nextInt(12), paint);
        }
    }

    private void drawCollectible(Canvas canvas, Collectible item) {
        Paint paint = new Paint();
        paint.setAntiAlias(true);

        float animatedSize = item.size * (1.0f + 0.12f * (float) Math.sin(item.pulseTime));

        if (item.isFuel) {
            // Draw custom Red/Green Energy/Fuel canister capsule
            paint.setColor(Color.parseColor("#00FF66"));
            canvas.drawRoundRect(new RectF(item.x - animatedSize, item.y - animatedSize * 1.3f, item.x + animatedSize, item.y + animatedSize * 1.3f), 8, 8, paint);

            // cross logo detailing inside
            paint.setColor(Color.WHITE);
            canvas.drawRect(item.x - 3, item.y - animatedSize * 0.7f, item.x + 3, item.y + animatedSize * 0.7f, paint);
            canvas.drawRect(item.x - animatedSize * 0.7f, item.y - 3, item.x + animatedSize * 0.7f, item.y + 3, paint);
        } else {
            // Render beautiful metallic Gold Rotating Arcade Coin
            paint.setColor(Color.parseColor("#FFCC00"));
            canvas.drawCircle(item.x, item.y, animatedSize, paint);

            paint.setColor(Color.parseColor("#FF9900"));
            canvas.drawCircle(item.x, item.y, animatedSize * 0.75f, paint);

            // Inner text character design "$"
            paint.setColor(Color.WHITE);
            paint.setTextSize(animatedSize * 1.1f);
            paint.setFakeBoldText(true);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("$", item.x, item.y + (animatedSize * 0.38f), paint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                touchTargetX = event.getX();
                return true;
        }
        return super.onTouchEvent(event);
    }

    // Model Classes Helper structures
    private static class ObstacleCar {
        float x, y, width, height;
        float extraSpeed;
        int color;

        ObstacleCar(float x, float y, float w, float h, float extraSpeed, int color) {
            this.x = x;
            this.y = y;
            this.width = w;
            this.height = h;
            this.extraSpeed = extraSpeed;
            this.color = color;
        }
    }

    private static class Collectible {
        float x, y, size;
        boolean isFuel;
        float pulseTime = 0;

        Collectible(float x, float y, float size, boolean isFuel) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.isFuel = isFuel;
            this.pulseTime = new Random().nextFloat() * 10f;
        }
    }

    private static class Particle {
        float x, y, vx, vy;
        int color;
        float size;
        int life;

        Particle(float startX, float startY, int color) {
            this.x = startX;
            this.y = startY;
            this.color = color;
            Random r = new Random();
            this.vx = (r.nextFloat() * 6f) - 3f;
            this.vy = (r.nextFloat() * 6f) - 3f;
            this.size = (r.nextFloat() * 8f) + 4f;
            this.life = r.nextInt(20) + 15;
        }
    }

    private static class TextEffect {
        float x, y;
        String text;
        int color;
        int life = 40; // frames

        TextEffect(float x, float y, String text, int color) {
            this.x = x;
            this.y = y;
            this.text = text;
            this.color = color;
        }
    }
}