package com.slicemaster.app;

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
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    public enum State {
        READY,
        PLAYING,
        GAME_OVER
    }

    private static final String PREF_NAME = "slice_master_prefs";
    private static final String KEY_HIGH_SCORE = "high_score_key";

    private Thread gameThread;
    private volatile boolean isRunning;
    private SurfaceHolder holder;

    // Game state
    private State state = State.READY;
    private int score = 0;
    private int highScore = 0;
    private int lives = 3;
    private final int MAX_LIVES = 3;

    // Multi-slice combo system
    private int swipeFruitCount = 0;
    private long lastSliceTime = 0;
    private String comboBannerText = "";
    private float comboBannerAlpha = 0f;
    private float comboBannerY = 0f;

    // Screen Shake effect
    private float shakeIntensity = 0f;
    private float shakeDecay = 18f;

    // Entities
    private final List<Fruit> fruits = new ArrayList<Fruit>();
    private final List<Particle> particles = new ArrayList<Particle>();
    private static final int MAX_PARTICLES = 250;

    private final SliceTrail sliceTrail = new SliceTrail();
    private final SoundHelper soundHelper;
    private final SharedPreferences prefs;
    private final Random random = new Random();

    // Spawner timers
    private float spawnTimer = 0f;
    private float spawnInterval = 1.35f;

    // Graphics paints
    private final Paint bgPaint = new Paint();
    private final Paint hudPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint buttonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint particlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF btnRect = new RectF();

    private int screenWidth = 1080;
    private int screenHeight = 1920;
    private float density = 1f;

    public GameView(Context context) {
        this(context, null);
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        holder = getHolder();
        holder.addCallback(this);

        soundHelper = new SoundHelper(context);
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        highScore = prefs.getInt(KEY_HIGH_SCORE, 0);

        density = context.getResources().getDisplayMetrics().density;

        // Pre-allocate particle pool
        for (int i = 0; i < MAX_PARTICLES; i++) {
            particles.add(new Particle());
        }

        setFocusable(true);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        screenWidth = getWidth();
        screenHeight = getHeight();
        isRunning = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        screenWidth = width;
        screenHeight = height;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
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
        long lastTime = SystemClock.uptimeMillis();

        while (isRunning) {
            long now = SystemClock.uptimeMillis();
            float dt = (now - lastTime) / 1000f;
            lastTime = now;

            if (dt > 0.1f) dt = 0.1f; // Clamp frame skips

            update(dt);
            drawFrame();

            // Cap at ~60 FPS
            long elapsed = SystemClock.uptimeMillis() - now;
            long sleepTime = 16 - elapsed;
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    private void update(float dt) {
        sliceTrail.update();

        // Screen shake decay
        if (shakeIntensity > 0) {
            shakeIntensity -= shakeDecay * dt;
            if (shakeIntensity < 0) shakeIntensity = 0;
        }

        // Combo banner fade
        if (comboBannerAlpha > 0) {
            comboBannerAlpha -= 1.8f * dt;
            comboBannerY -= 60f * dt;
            if (comboBannerAlpha < 0) comboBannerAlpha = 0;
        }

        // Update active particles
        for (int i = 0; i < particles.size(); i++) {
            Particle p = particles.get(i);
            if (p.active) {
                p.update(dt);
            }
        }

        if (state == State.PLAYING) {
            // Spawner
            spawnTimer += dt;
            if (spawnTimer >= spawnInterval) {
                spawnTimer = 0f;
                spawnWave();
                // Gradually increase pace as score rises
                spawnInterval = Math.max(0.75f, 1.35f - (score * 0.015f));
            }

            // Update fruits
            for (int i = fruits.size() - 1; i >= 0; i--) {
                Fruit fruit = fruits.get(i);
                fruit.update(dt, screenHeight);

                // Missed non-sliced fruit penalty
                if (fruit.isOffscreen) {
                    if (!fruit.sliced && fruit.type != Fruit.Type.BOMB) {
                        loseLife();
                    }
                    fruits.remove(i);
                }
            }
        } else if (state == State.READY) {
            // In READY mode, spawn a slow demo fruit occasionally for interactive slice-to-start
            spawnTimer += dt;
            if (spawnTimer >= 2.0f && fruits.isEmpty()) {
                spawnTimer = 0f;
                Fruit demoFruit = new Fruit(Fruit.Type.WATERMELON, screenWidth * 0.5f, screenHeight + 50, 0, -1100f, 65f * density);
                fruits.add(demoFruit);
            }
            for (int i = fruits.size() - 1; i >= 0; i--) {
                Fruit fruit = fruits.get(i);
                fruit.update(dt, screenHeight);
                if (fruit.isOffscreen) {
                    fruits.remove(i);
                }
            }
        } else if (state == State.GAME_OVER) {
            for (int i = fruits.size() - 1; i >= 0; i--) {
                Fruit fruit = fruits.get(i);
                fruit.update(dt, screenHeight);
                if (fruit.isOffscreen) {
                    fruits.remove(i);
                }
            }
        }
    }

    private void spawnWave() {
        int count = 1 + random.nextInt(Math.min(3, 1 + score / 12));
        boolean spawnBomb = random.nextFloat() < (0.22f + Math.min(0.25f, score * 0.005f));

        for (int i = 0; i < count; i++) {
            float startX = screenWidth * 0.15f + random.nextFloat() * (screenWidth * 0.70f);
            float startY = screenHeight + 40f;

            float targetX = screenWidth * 0.5f + (random.nextFloat() - 0.5f) * screenWidth * 0.6f;
            float vx = (targetX - startX) * 0.75f;
            float vy = -1200f - random.nextFloat() * 450f;

            Fruit.Type type;
            if (spawnBomb && i == 0) {
                type = Fruit.Type.BOMB;
            } else {
                Fruit.Type[] fruitTypes = new Fruit.Type[]{
                        Fruit.Type.WATERMELON,
                        Fruit.Type.APPLE,
                        Fruit.Type.BANANA,
                        Fruit.Type.ORANGE,
                        Fruit.Type.STRAWBERRY
                };
                type = fruitTypes[random.nextInt(fruitTypes.length)];
            }

            float radius = (type == Fruit.Type.STRAWBERRY ? 42f : 55f) * density;
            fruits.add(new Fruit(type, startX, startY, vx, vy, radius));
        }
    }

    private void emitJuice(float x, float y, int color, int count) {
        int emitted = 0;
        for (int i = 0; i < particles.size() && emitted < count; i++) {
            Particle p = particles.get(i);
            if (!p.active) {
                float angle = random.nextFloat() * 360f;
                float speed = 250f + random.nextFloat() * 700f;
                float rad = (float) Math.toRadians(angle);
                float vx = (float) Math.cos(rad) * speed;
                float vy = (float) Math.sin(rad) * speed;
                float radius = (4f + random.nextFloat() * 8f) * density;
                float decay = 400f + random.nextFloat() * 350f;

                p.init(x, y, vx, vy, radius, color, decay);
                emitted++;
            }
        }
    }

    private void checkSliceCollisions(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float cutAngle = (float) Math.toDegrees(Math.atan2(dy, dx));

        for (int i = 0; i < fruits.size(); i++) {
            Fruit fruit = fruits.get(i);
            if (!fruit.sliced) {
                boolean hit = SliceTrail.checkCircleIntersect(x1, y1, x2, y2, fruit.x, fruit.y, fruit.radius);
                if (hit) {
                    if (fruit.type == Fruit.Type.BOMB) {
                        fruit.slice(cutAngle);
                        triggerBombHit(fruit.x, fruit.y);
                    } else {
                        fruit.slice(cutAngle);
                        swipeFruitCount++;
                        lastSliceTime = SystemClock.uptimeMillis();

                        int points = fruit.getPoints();
                        score += points;
                        emitJuice(fruit.x, fruit.y, fruit.getJuiceColor(), 28);
                        soundHelper.playSlice();

                        // Check combo (2+ fruits sliced in one swipe stroke)
                        if (swipeFruitCount >= 2) {
                            int bonus = swipeFruitCount * 2;
                            score += bonus;
                            comboBannerText = "COMBO " + swipeFruitCount + "x! +" + bonus;
                            comboBannerAlpha = 1f;
                            comboBannerY = fruit.y - 40f;
                            soundHelper.playCombo();
                        }

                        if (score > highScore) {
                            highScore = score;
                            prefs.edit().putInt(KEY_HIGH_SCORE, highScore).apply();
                        }

                        if (state == State.READY) {
                            // First swipe slice starts the game!
                            startGame();
                        }
                    }
                }
            }
        }
    }

    private void triggerBombHit(float x, float y) {
        emitJuice(x, y, Color.parseColor("#FF3D00"), 50);
        emitJuice(x, y, Color.parseColor("#FFD600"), 40);
        soundHelper.playBombExplode();
        shakeIntensity = 25f;

        loseLife();
    }

    private void loseLife() {
        lives--;
        shakeIntensity = 15f;
        if (lives <= 0) {
            lives = 0;
            state = State.GAME_OVER;
            if (score > highScore) {
                highScore = score;
                prefs.edit().putInt(KEY_HIGH_SCORE, highScore).apply();
            }
        }
    }

    public void startGame() {
        score = 0;
        lives = MAX_LIVES;
        fruits.clear();
        spawnTimer = 0f;
        spawnInterval = 1.35f;
        swipeFruitCount = 0;
        comboBannerAlpha = 0f;
        state = State.PLAYING;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (state == State.GAME_OVER) {
                    // Check if replay button touched
                    if (btnRect.contains(x, y)) {
                        startGame();
                        return true;
                    }
                }
                sliceTrail.clear();
                sliceTrail.addPoint(x, y);
                swipeFruitCount = 0;
                break;

            case MotionEvent.ACTION_MOVE:
                SliceTrail.Point lastPt = sliceTrail.getLastPoint();
                if (lastPt != null) {
                    checkSliceCollisions(lastPt.x, lastPt.y, x, y);
                }
                sliceTrail.addPoint(x, y);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                sliceTrail.clear();
                swipeFruitCount = 0;
                break;
        }
        return true;
    }

    private void drawFrame() {
        Canvas canvas = null;
        try {
            canvas = holder.lockCanvas();
            if (canvas == null) return;

            // Apply screen shake
            canvas.save();
            if (shakeIntensity > 0) {
                float sx = (random.nextFloat() - 0.5f) * shakeIntensity * density;
                float sy = (random.nextFloat() - 0.5f) * shakeIntensity * density;
                canvas.translate(sx, sy);
            }

            // Draw Background with subtle vertical arcade gradient
            Shader bgShader = new LinearGradient(
                    0, 0, 0, screenHeight,
                    Color.parseColor("#0C0D14"),
                    Color.parseColor("#171927"),
                    Shader.TileMode.CLAMP
            );
            bgPaint.setShader(bgShader);
            canvas.drawRect(0, 0, screenWidth, screenHeight, bgPaint);

            // Draw Dojo Background Grid Accent Lines
            hudPaint.setColor(Color.argb(20, 255, 255, 255));
            hudPaint.setStrokeWidth(2f);
            for (int i = 1; i < 6; i++) {
                float lineX = screenWidth * (i / 6.0f);
                canvas.drawLine(lineX, 0, lineX, screenHeight, hudPaint);
            }

            // Draw Fruits
            for (int i = 0; i < fruits.size(); i++) {
                fruits.get(i).draw(canvas);
            }

            // Draw Particles
            for (int i = 0; i < particles.size(); i++) {
                particles.get(i).draw(canvas, particlePaint);
            }

            // Draw Blade Trail
            sliceTrail.draw(canvas);

            // Draw Combo popup text
            if (comboBannerAlpha > 0) {
                textPaint.setColor(Color.parseColor("#FFD600"));
                textPaint.setTextSize(36f * density);
                textPaint.setTextAlign(Paint.Align.CENTER);
                textPaint.setFakeBoldText(true);
                textPaint.setAlpha((int) (255 * comboBannerAlpha));
                canvas.drawText(comboBannerText, screenWidth * 0.5f, comboBannerY, textPaint);
            }

            // Draw HUD
            drawHUD(canvas);

            // Draw Overlays according to state
            if (state == State.READY) {
                drawReadyOverlay(canvas);
            } else if (state == State.GAME_OVER) {
                drawGameOverOverlay(canvas);
            }

            canvas.restore();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (canvas != null) {
                holder.unlockCanvasAndPost(canvas);
            }
        }
    }

    private void drawHUD(Canvas canvas) {
        // Top HUD panel
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(26f * density);
        textPaint.setFakeBoldText(true);
        textPaint.setAlpha(255);

        // Score display
        canvas.drawText("SCORE: " + score, 24f * density, 48f * density, textPaint);

        // High score display
        textPaint.setTextSize(16f * density);
        textPaint.setColor(Color.parseColor("#FFD600"));
        canvas.drawText("BEST: " + highScore, 24f * density, 76f * density, textPaint);

        // Lives indicators (Hearts / Blades)
        float lifeRadius = 10f * density;
        float rightMargin = screenWidth - 28f * density;
        for (int i = 0; i < MAX_LIVES; i++) {
            float cx = rightMargin - i * (lifeRadius * 2.8f);
            float cy = 45f * density;

            hudPaint.setStyle(Paint.Style.FILL);
            if (i < lives) {
                hudPaint.setColor(Color.parseColor("#FF1744"));
            } else {
                hudPaint.setColor(Color.parseColor("#424242"));
            }
            canvas.drawCircle(cx, cy, lifeRadius, hudPaint);

            // Cross in lost life
            if (i >= lives) {
                hudPaint.setColor(Color.WHITE);
                hudPaint.setStrokeWidth(3f);
                hudPaint.setStyle(Paint.Style.STROKE);
                canvas.drawLine(cx - lifeRadius * 0.5f, cy - lifeRadius * 0.5f, cx + lifeRadius * 0.5f, cy + lifeRadius * 0.5f, hudPaint);
                canvas.drawLine(cx + lifeRadius * 0.5f, cy - lifeRadius * 0.5f, cx - lifeRadius * 0.5f, cy + lifeRadius * 0.5f, hudPaint);
            }
        }
    }

    private void drawReadyOverlay(Canvas canvas) {
        // Title logo
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(44f * density);
        textPaint.setColor(Color.WHITE);
        textPaint.setFakeBoldText(true);
        canvas.drawText("SLICE MASTER", screenWidth * 0.5f, screenHeight * 0.35f, textPaint);

        textPaint.setTextSize(18f * density);
        textPaint.setColor(Color.parseColor("#00E5FF"));
        canvas.drawText("VIBRANT ARCADE ACTION", screenWidth * 0.5f, screenHeight * 0.39f, textPaint);

        // Call to action
        textPaint.setTextSize(22f * density);
        textPaint.setColor(Color.parseColor("#FFEA00"));
        long pulseTime = SystemClock.uptimeMillis() % 1000;
        int alpha = (int) (160 + 95 * Math.sin(pulseTime * Math.PI / 500.0));
        textPaint.setAlpha(alpha);
        canvas.drawText("⚡ SWIPE TO SLICE FRUIT & PLAY! ⚡", screenWidth * 0.5f, screenHeight * 0.65f, textPaint);
    }

    private void drawGameOverOverlay(Canvas canvas) {
        // Semi-transparent backdrop
        hudPaint.setStyle(Paint.Style.FILL);
        hudPaint.setColor(Color.argb(200, 10, 12, 18));
        canvas.drawRect(0, 0, screenWidth, screenHeight, hudPaint);

        // Game Over card
        float cardWidth = screenWidth * 0.84f;
        float cardHeight = 360f * density;
        float cardLeft = (screenWidth - cardWidth) * 0.5f;
        float cardTop = screenHeight * 0.28f;
        RectF cardRect = new RectF(cardLeft, cardTop, cardLeft + cardWidth, cardTop + cardHeight);

        hudPaint.setColor(Color.parseColor("#1F2438"));
        canvas.drawRoundRect(cardRect, 28f * density, 28f * density, hudPaint);

        // Stroke border
        hudPaint.setStyle(Paint.Style.STROKE);
        hudPaint.setStrokeWidth(3f * density);
        hudPaint.setColor(Color.parseColor("#374151"));
        canvas.drawRoundRect(cardRect, 28f * density, 28f * density, hudPaint);

        // Title text
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.parseColor("#FF1744"));
        textPaint.setTextSize(38f * density);
        textPaint.setFakeBoldText(true);
        textPaint.setAlpha(255);
        canvas.drawText("GAME OVER", screenWidth * 0.5f, cardTop + 65f * density, textPaint);

        // Final score
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(22f * density);
        canvas.drawText("FINAL SCORE: " + score, screenWidth * 0.5f, cardTop + 130f * density, textPaint);

        // Best score
        textPaint.setColor(Color.parseColor("#FFD600"));
        textPaint.setTextSize(20f * density);
        canvas.drawText("BEST RECORD: " + highScore, screenWidth * 0.5f, cardTop + 175f * density, textPaint);

        // "Play Again" Button
        float btnWidth = cardWidth * 0.72f;
        float btnHeight = 54f * density;
        float btnLeft = (screenWidth - btnWidth) * 0.5f;
        float btnTop = cardTop + 245f * density;
        btnRect.set(btnLeft, btnTop, btnLeft + btnWidth, btnTop + btnHeight);

        buttonPaint.setStyle(Paint.Style.FILL);
        buttonPaint.setColor(Color.parseColor("#00E676"));
        canvas.drawRoundRect(btnRect, 18f * density, 18f * density, buttonPaint);

        // Button label
        textPaint.setColor(Color.parseColor("#0A2613"));
        textPaint.setTextSize(20f * density);
        textPaint.setFakeBoldText(true);
        canvas.drawText("PLAY AGAIN", screenWidth * 0.5f, btnTop + 35f * density, textPaint);
    }

    public void release() {
        isRunning = false;
        if (soundHelper != null) {
            soundHelper.release();
        }
    }
}