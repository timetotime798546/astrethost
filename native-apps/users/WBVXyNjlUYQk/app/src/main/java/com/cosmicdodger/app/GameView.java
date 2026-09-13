package com.cosmicdodger.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class GameView extends View {

    public interface GameListener {
        void onScoreChanged(int score);
        void onLivesChanged(int lives);
        void onGameOver(int score);
    }

    private GameListener gameListener;
    private ToneGenerator toneGen;
    private final Random random = new Random();

    // Game state trackers
    private static final int STATE_IDLE = 0;
    private static final int STATE_PLAYING = 1;
    private static final int STATE_GAME_OVER = 2;
    private int gameState = STATE_IDLE;

    private final GameLoop gameLoop = new GameLoop();
    private boolean isPaused = false;

    // View boundaries
    private int viewWidth = 0;
    private int viewHeight = 0;

    // Player details
    private float shipX = 0;
    private float shipY = 0;
    private float targetShipX = 0;
    private float shipRadius = 42f;
    private int lives = 3;
    private int score = 0;

    // Laser weapon properties
    private long lastLaserTime = 0;
    private static final long LASER_INTERVAL = 380; // Automated gun cycle millisecond rate

    // Hit shielding properties
    private boolean isInvulnerable = false;
    private long invulnerableStartTime = 0;
    private static final long INVULNERABLE_DURATION = 1500;

    // Juice values
    private float shakeIntensity = 0f;

    // Game object vectors
    private final List<SpaceStar> backgroundStars = new ArrayList<>();
    private final List<SpaceAsteroid> asteroids = new ArrayList<>();
    private final List<LaserProj> lasers = new ArrayList<>();
    private final List<PowerStar> goldStars = new ArrayList<>();
    private final List<FxParticle> particles = new ArrayList<>();

    // Drawing resources
    private final Paint paintShip = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintShipThruster = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintAsteroid = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintLaser = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintGoldStar = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintBgStar = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintParticles = new Paint(Paint.ANTI_ALIAS_FLAG);

    public GameView(Context context) {
        super(context);
        init();
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        try {
            toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 70);
        } catch (Exception e) {
            toneGen = null;
        }

        paintShip.setColor(Color.rgb(0, 255, 255));
        paintShip.setStyle(Paint.Style.FILL);

        paintShipThruster.setColor(Color.rgb(255, 120, 0));
        paintShipThruster.setStyle(Paint.Style.FILL);

        paintAsteroid.setColor(Color.rgb(130, 120, 120));
        paintAsteroid.setStyle(Paint.Style.FILL);

        paintLaser.setColor(Color.rgb(0, 255, 120));
        paintLaser.setStrokeWidth(9f);
        paintLaser.setStyle(Paint.Style.STROKE);

        paintGoldStar.setColor(Color.rgb(255, 215, 0));
        paintGoldStar.setStyle(Paint.Style.FILL);

        paintBgStar.setColor(Color.WHITE);
        paintBgStar.setStyle(Paint.Style.FILL);
    }

    public void setGameListener(GameListener listener) {
        this.gameListener = listener;
    }

    public void startNewGame() {
        score = 0;
        lives = 3;
        gameState = STATE_PLAYING;
        isPaused = false;

        asteroids.clear();
        lasers.clear();
        goldStars.clear();
        particles.clear();

        if (viewWidth > 0 && viewHeight > 0) {
            shipX = viewWidth / 2f;
            targetShipX = shipX;
            shipY = viewHeight - 240f;

            if (backgroundStars.isEmpty()) {
                generateStarfield();
            }
        }

        if (gameListener != null) {
            gameListener.onScoreChanged(score);
            gameListener.onLivesChanged(lives);
        }

        gameLoop.start();
    }

    public void pauseGame() {
        isPaused = true;
        gameLoop.stop();
    }

    public void resumeGame() {
        if (gameState == STATE_PLAYING) {
            isPaused = false;
            gameLoop.start();
        }
    }

    public void releaseResources() {
        gameLoop.stop();
        if (toneGen != null) {
            try {
                toneGen.release();
            } catch (Exception e) {
                // Safe ignore
            }
            toneGen = null;
        }
    }

    private void generateStarfield() {
        backgroundStars.clear();
        for (int i = 0; i < 60; i++) {
            backgroundStars.add(new SpaceStar(
                random.nextFloat() * viewWidth,
                random.nextFloat() * viewHeight,
                1.5f + random.nextFloat() * 4.5f,
                1.5f + random.nextFloat() * 2.5f
            ));
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        viewWidth = w;
        viewHeight = h;

        shipX = viewWidth / 2f;
        targetShipX = shipX;
        shipY = viewHeight - 240f;

        generateStarfield();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (gameState != STATE_PLAYING) {
            return super.onTouchEvent(event);
        }

        int action = event.getAction();
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
            float x = event.getX();
            if (x < shipRadius) x = shipRadius;
            if (x > viewWidth - shipRadius) x = viewWidth - shipRadius;
            targetShipX = x;
            return true;
        }
        return true;
    }

    private void triggerSfx(int type, int duration) {
        try {
            if (toneGen != null) {
                toneGen.startTone(type, duration);
            }
        } catch (Exception e) {
            // Safe ignore
        }
    }

    private void updateEnginePhysics() {
        if (gameState != STATE_PLAYING) return;

        // Space Fighter Movement dampening
        shipX += (targetShipX - shipX) * 0.22f;

        // Auto Shooting Logic
        long curTime = System.currentTimeMillis();
        if (curTime - lastLaserTime >= LASER_INTERVAL) {
            lasers.add(new LaserProj(shipX, shipY - shipRadius));
            lastLaserTime = curTime;
            triggerSfx(ToneGenerator.TONE_PROP_BEEP, 50);
        }

        // Hit Shield/Invulnerable cycle timeout
        if (isInvulnerable) {
            if (curTime - invulnerableStartTime > INVULNERABLE_DURATION) {
                isInvulnerable = false;
            }
        }

        // Update scrolling stars
        for (int i = 0; i < backgroundStars.size(); i++) {
            SpaceStar ss = backgroundStars.get(i);
            ss.y += ss.speed;
            if (ss.y > viewHeight) {
                ss.y = 0;
                ss.x = random.nextFloat() * viewWidth;
            }
        }

        // Update fired lasers
        Iterator<LaserProj> laserIter = lasers.iterator();
        while (laserIter.hasNext()) {
            LaserProj laser = laserIter.next();
            laser.y -= 16f;
            if (laser.y < 0) {
                laserIter.remove();
            }
        }

        // Generate Gold Star Drops
        if (random.nextFloat() < 0.012f && goldStars.size() < 3) {
            goldStars.add(new PowerStar(
                random.nextFloat() * (viewWidth - 100) + 50,
                -30f,
                24f,
                4f + random.nextFloat() * 3.5f
            ));
        }

        // Update Gold Star collection mechanics
        Iterator<PowerStar> starIter = goldStars.iterator();
        while (starIter.hasNext()) {
            PowerStar gs = starIter.next();
            gs.y += gs.speedY;
            if (gs.y - gs.radius > viewHeight) {
                starIter.remove();
                continue;
            }

            // Ship Star-Collector detection
            float dx = gs.x - shipX;
            float dy = gs.y - shipY;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            if (dist < (gs.radius + shipRadius)) {
                score += 15;
                if (gameListener != null) {
                    gameListener.onScoreChanged(score);
                }
                triggerSfx(ToneGenerator.TONE_DTMF_A, 90);

                // Add collection sparkle bursts
                for (int p = 0; p < 12; p++) {
                    particles.add(new FxParticle(
                        gs.x, gs.y,
                        (random.nextFloat() - 0.5f) * 8f,
                        (random.nextFloat() - 0.5f) * 8f,
                        5f + random.nextFloat() * 6f,
                        Color.rgb(255, 215, 0),
                        1.0f,
                        0.03f + random.nextFloat() * 0.03f
                    ));
                }
                starIter.remove();
            }
        }

        // Generate incoming asteroid hazards
        float obstacleRate = 0.018f + (score / 1500f) * 0.01f;
        if (obstacleRate > 0.075f) obstacleRate = 0.075f;

        if (random.nextFloat() < obstacleRate && asteroids.size() < 7) {
            float astRad = 32f + random.nextFloat() * 52f;
            float fallSpd = 3.5f + random.nextFloat() * 4.5f + (score / 450f);
            if (fallSpd > 14f) fallSpd = 14f;

            asteroids.add(new SpaceAsteroid(
                random.nextFloat() * (viewWidth - 120) + 60,
                -astRad,
                astRad,
                fallSpd,
                (random.nextFloat() - 0.5f) * 3f,
                (int) (astRad / 18f) + 1,
                (random.nextFloat() - 0.5f) * 4f
            ));
        }

        // Update Asteroid collision, damage, and layout bounds
        Iterator<SpaceAsteroid> astIter = asteroids.iterator();
        while (astIter.hasNext()) {
            SpaceAsteroid ast = astIter.next();
            ast.y += ast.speedY;
            ast.x += ast.speedX;
            ast.rotationAngle += ast.rotSpeed;

            if (ast.x < ast.radius || ast.x > viewWidth - ast.radius) {
                ast.speedX = -ast.speedX;
            }

            if (ast.y - ast.radius > viewHeight) {
                ast.removeThis = true;
            }

            // Ship vs Asteroid Collision
            float shipDx = ast.x - shipX;
            float shipDy = ast.y - shipY;
            float shipDist = (float) Math.sqrt(shipDx * shipDx + shipDy * shipDy);
            if (shipDist < (ast.radius + shipRadius - 8f)) {
                if (!isInvulnerable) {
                    lives--;
                    if (gameListener != null) {
                        gameListener.onLivesChanged(lives);
                    }
                    shakeIntensity = 24f; // Trigger Screen Shake
                    isInvulnerable = true;
                    invulnerableStartTime = curTime;

                    triggerSfx(ToneGenerator.TONE_CDMA_PIP, 160);

                    // Massive pink/orange spark feedback
                    for (int p = 0; p < 25; p++) {
                        particles.add(new FxParticle(
                            shipX, shipY,
                            (random.nextFloat() - 0.5f) * 16f,
                            (random.nextFloat() - 0.5f) * 16f,
                            7f + random.nextFloat() * 11f,
                            Color.rgb(255, 50, 100),
                            1.0f,
                            0.02f + random.nextFloat() * 0.02f
                        ));
                    }

                    if (lives <= 0) {
                        gameState = STATE_GAME_OVER;
                        if (gameListener != null) {
                            gameListener.onGameOver(score);
                        }
                        gameLoop.stop();
                        break;
                    }
                }
            }

            // Lasers hitting Asteroid Check
            Iterator<LaserProj> lpIter = lasers.iterator();
            while (lpIter.hasNext()) {
                LaserProj lp = lpIter.next();
                float lDx = lp.x - ast.x;
                float lDy = lp.y - ast.y;
                float lDist = (float) Math.sqrt(lDx * lDx + lDy * lDy);
                if (lDist < (ast.radius + 12f)) {
                    lpIter.remove(); // Consume laser
                    ast.healthPoint--;

                    // Impact tiny green particle burst
                    for (int p = 0; p < 6; p++) {
                        particles.add(new FxParticle(
                            lp.x, lp.y,
                            (random.nextFloat() - 0.5f) * 7f,
                            (random.nextFloat() - 0.5f) * 7f,
                            3f + random.nextFloat() * 4f,
                            Color.rgb(0, 255, 120),
                            1.0f,
                            0.06f
                        ));
                    }

                    if (ast.healthPoint <= 0) {
                        ast.destroyed = true;
                        score += (int) (ast.radius / 6f) + 5;
                        if (gameListener != null) {
                            gameListener.onScoreChanged(score);
                        }
                        triggerSfx(ToneGenerator.TONE_CDMA_ONE_MIN_BEEP, 60);

                        // Gray explosion dust
                        for (int p = 0; p < 14; p++) {
                            particles.add(new FxParticle(
                                ast.x, ast.y,
                                (random.nextFloat() - 0.5f) * 10f,
                                (random.nextFloat() - 0.5f) * 10f,
                                5f + random.nextFloat() * 9f,
                                Color.rgb(130, 120, 120),
                                1.0f,
                                0.03f + random.nextFloat() * 0.03f
                            ));
                        }
                        break;
                    }
                }
            }

            if (ast.destroyed || ast.removeThis) {
                astIter.remove();
            }
        }

        // Update FX elements
        Iterator<FxParticle> partIter = particles.iterator();
        while (partIter.hasNext()) {
            FxParticle p = partIter.next();
            p.x += p.vx;
            p.y += p.vy;
            p.alpha -= p.decay;
            if (p.alpha <= 0) {
                partIter.remove();
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Screen Shake translation adjustments
        if (shakeIntensity > 0.1f) {
            float dx = (random.nextFloat() * 2 - 1) * shakeIntensity;
            float dy = (random.nextFloat() * 2 - 1) * shakeIntensity;
            canvas.translate(dx, dy);
            shakeIntensity *= 0.86f;
        }

        // Draw Sky Background
        canvas.drawColor(Color.rgb(6, 4, 16));
        for (int i = 0; i < backgroundStars.size(); i++) {
            SpaceStar ss = backgroundStars.get(i);
            paintBgStar.setAlpha((int) (90 + ss.speed * 30));
            canvas.drawCircle(ss.x, ss.y, ss.radius, paintBgStar);
        }

        if (gameState != STATE_PLAYING && gameState != STATE_GAME_OVER) {
            return;
        }

        // Draw lasers
        for (int i = 0; i < lasers.size(); i++) {
            LaserProj lp = lasers.get(i);
            canvas.drawLine(lp.x, lp.y, lp.x, lp.y - 35f, paintLaser);
        }

        // Draw drop elements (Gold Star)
        for (int i = 0; i < goldStars.size(); i++) {
            PowerStar gs = goldStars.get(i);
            drawStarVectors(canvas, gs.x, gs.y, gs.radius);
        }

        // Draw Asteroids
        for (int i = 0; i < asteroids.size(); i++) {
            drawAsteroidEntity(canvas, asteroids.get(i));
        }

        // Draw FX particles
        for (int i = 0; i < particles.size(); i++) {
            FxParticle p = particles.get(i);
            paintParticles.setColor(p.color);
            paintParticles.setAlpha((int) (p.alpha * 255));
            canvas.drawCircle(p.x, p.y, p.radius, paintParticles);
        }

        // Draw User Spacecraft
        boolean drawSpaceship = true;
        if (isInvulnerable) {
            long currentElapsed = System.currentTimeMillis() - invulnerableStartTime;
            if ((currentElapsed / 110) % 2 == 0) {
                drawSpaceship = false;
            }
        }

        if (drawSpaceship) {
            // Render Flame Engine exhaust flicker
            float thrustSize = shipRadius * (0.55f + random.nextFloat() * 0.45f);
            Path flame = new Path();
            flame.moveTo(shipX - shipRadius * 0.35f, shipY + shipRadius * 0.45f);
            flame.lineTo(shipX + shipRadius * 0.35f, shipY + shipRadius * 0.45f);
            flame.lineTo(shipX, shipY + shipRadius * 0.45f + thrustSize);
            flame.close();
            canvas.drawPath(flame, paintShipThruster);

            // Draw Core Ship Geometry
            Path shipGeometry = new Path();
            shipGeometry.moveTo(shipX, shipY - shipRadius);
            shipGeometry.lineTo(shipX - shipRadius, shipY + shipRadius * 0.45f);
            shipGeometry.lineTo(shipX - shipRadius * 0.4f, shipY + shipRadius * 0.3f);
            shipGeometry.lineTo(shipX + shipRadius * 0.4f, shipY + shipRadius * 0.3f);
            shipGeometry.lineTo(shipX + shipRadius, shipY + shipRadius * 0.45f);
            shipGeometry.close();

            paintShip.setColor(Color.rgb(0, 235, 255));
            canvas.drawPath(shipGeometry, paintShip);

            // Translucent glass cockpit bubble
            Paint bubblePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            bubblePaint.setColor(Color.WHITE);
            bubblePaint.setAlpha(170);
            bubblePaint.setStyle(Paint.Style.FILL);

            Path bubblePath = new Path();
            bubblePath.moveTo(shipX, shipY - shipRadius * 0.45f);
            bubblePath.lineTo(shipX - shipRadius * 0.22f, shipY + shipRadius * 0.05f);
            bubblePath.lineTo(shipX + shipRadius * 0.22f, shipY + shipRadius * 0.05f);
            bubblePath.close();
            canvas.drawPath(bubblePath, bubblePaint);
        }
    }

    private void drawStarVectors(Canvas canvas, float cx, float cy, float maxRad) {
        Path path = new Path();
        double angleDelta = Math.PI / 5;
        float innerRad = maxRad * 0.42f;

        for (int i = 0; i < 10; i++) {
            float r = (i % 2 == 0) ? maxRad : innerRad;
            float x = (float) (cx + Math.cos(i * angleDelta - Math.PI / 2) * r);
            float y = (float) (cy + Math.sin(i * angleDelta - Math.PI / 2) * r);
            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
        path.close();
        canvas.drawPath(path, paintGoldStar);
    }

    private void drawAsteroidEntity(Canvas canvas, SpaceAsteroid ast) {
        canvas.save();
        canvas.translate(ast.x, ast.y);
        canvas.rotate(ast.rotationAngle);

        // Generate stylized jagged edge vectors
        Path path = new Path();
        int edges = 9;
        for (int i = 0; i < edges; i++) {
            double angle = (2 * Math.PI / edges) * i;
            float noiseFactor = 1.0f + 0.16f * (float) Math.sin(i * 4 + ast.radius);
            float currentRad = ast.radius * noiseFactor;
            float vx = (float) (Math.cos(angle) * currentRad);
            float vy = (float) (Math.sin(angle) * currentRad);

            if (i == 0) {
                path.moveTo(vx, vy);
            } else {
                path.lineTo(vx, vy);
            }
        }
        path.close();

        paintAsteroid.setColor(Color.rgb(105, 95, 95));
        canvas.drawPath(path, paintAsteroid);

        // Crater highlights
        Paint craterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        craterPaint.setColor(Color.rgb(80, 70, 70));
        craterPaint.setStyle(Paint.Style.FILL);

        canvas.drawCircle(-ast.radius * 0.32f, -ast.radius * 0.18f, ast.radius * 0.19f, craterPaint);
        canvas.drawCircle(ast.radius * 0.35f, ast.radius * 0.22f, ast.radius * 0.16f, craterPaint);
        canvas.drawCircle(-ast.radius * 0.05f, ast.radius * 0.42f, ast.radius * 0.17f, craterPaint);

        canvas.restore();
    }

    // Engine loop scheduling thread handler
    private class GameLoop implements Runnable {
        private final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
        private boolean isRunning = false;

        public void start() {
            if (!isRunning) {
                isRunning = true;
                handler.post(this);
            }
        }

        public void stop() {
            isRunning = false;
            handler.removeCallbacks(this);
        }

        @Override
        public void run() {
            if (!isRunning || isPaused) return;

            updateEnginePhysics();
            invalidate();

            handler.postDelayed(this, 16); // 60 FPS Cycle
        }
    }

    // Object Specs Structure declarations
    private static class SpaceStar {
        float x, y, speed, radius;
        SpaceStar(float x, float y, float speed, float radius) {
            this.x = x;
            this.y = y;
            this.speed = speed;
            this.radius = radius;
        }
    }

    private static class SpaceAsteroid {
        float x, y, radius;
        float speedY, speedX;
        int healthPoint;
        float rotationAngle;
        float rotSpeed;
        boolean destroyed = false;
        boolean removeThis = false;

        SpaceAsteroid(float x, float y, float radius, float speedY, float speedX, int healthPoint, float rotSpeed) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.speedY = speedY;
            this.speedX = speedX;
            this.healthPoint = healthPoint;
            this.rotationAngle = 0f;
            this.rotSpeed = rotSpeed;
        }
    }

    private static class LaserProj {
        float x, y;
        LaserProj(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    private static class PowerStar {
        float x, y, radius, speedY;
        PowerStar(float x, float y, float radius, float speedY) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.speedY = speedY;
        }
    }

    private static class FxParticle {
        float x, y, vx, vy, radius;
        int color;
        float alpha;
        float decay;

        FxParticle(float x, float y, float vx, float vy, float radius, int color, float alpha, float decay) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.radius = radius;
            this.color = color;
            this.alpha = alpha;
            this.decay = decay;
        }
    }
}