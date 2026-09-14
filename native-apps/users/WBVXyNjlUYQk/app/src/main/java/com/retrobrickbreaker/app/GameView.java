package com.retrobrickbreaker.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.Random;

public class GameView extends View {

    // Interfaces for Game Lifecycle events communicating with MainActivity UI
    public interface GameListener {
        void onScoreChanged(int score);
        void onLivesChanged(int lives);
        void onLevelChanged(int level);
        void onGameOver(int score, boolean isHighScore);
        void onGameWon(int score, boolean isHighScore);
    }

    private GameListener mListener;
    private Handler mHandler;
    private Runnable mGameLoop;
    private boolean mIsPlaying = false;
    private boolean mSoundEnabled = true;

    // Canvas paints
    private Paint mPaintBrick;
    private Paint mPaintPaddle;
    private Paint mPaintBall;
    private Paint mPaintPowerUp;
    private Paint mPaintBgGrid;

    // Game objects variables
    private float mPaddleX, mPaddleY;
    private float mPaddleWidth, mPaddleHeight;
    private final float DEFAULT_PADDLE_WIDTH = 200f;
    private final float DEFAULT_PADDLE_HEIGHT = 30f;

    // Multiple Balls system
    private ArrayList<Ball> mBalls;
    
    // Bricks list
    private ArrayList<Brick> mBricks;
    
    // Power-ups list
    private ArrayList<PowerUp> mPowerUps;

    // Starfield drifting stars background
    public static class Star {
        float x, y;
        float speed;
        float size;
        int alpha;
        int baseAlpha;
        boolean dimming;

        public Star(float x, float y, float speed, float size, int alpha) {
            this.x = x;
            this.y = y;
            this.speed = speed;
            this.size = size;
            this.alpha = alpha;
            this.baseAlpha = alpha;
            this.dimming = false;
        }

        public void update(int maxHeight) {
            if (maxHeight <= 0) return;
            y += speed;
            if (y > maxHeight) {
                y = 0;
            }
            if (dimming) {
                alpha -= 5;
                if (alpha < 60) {
                    alpha = 60;
                    dimming = false;
                }
            } else {
                alpha += 5;
                if (alpha > baseAlpha) {
                    alpha = baseAlpha;
                    dimming = true;
                }
            }
        }
    }

    // Explosive neon fragments
    public static class Particle {
        float x, y;
        float vx, vy;
        float size;
        int color;
        float alpha = 255f;
        float fadeSpeed;

        public Particle(float x, float y, float vx, float vy, float size, int color, float fadeSpeed) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.size = size;
            this.color = color;
            this.fadeSpeed = fadeSpeed;
        }

        public boolean update() {
            x += vx;
            y += vy;
            alpha -= fadeSpeed;
            return alpha <= 0;
        }
    }

    private ArrayList<Star> mStars;
    private ArrayList<Particle> mParticles;
    private float mGridOffset = 0f;

    // Viewport Screen Shake properties
    private float mShakeX = 0f;
    private float mShakeY = 0f;
    private int mShakeFrames = 0;
    private final float SHAKE_INTENSITY = 8f;

    // Game stats
    private int mScore = 0;
    private int mLives = 3;
    private int mCurrentLevel = 1;
    private final int MAX_LEVEL = 3;

    // Hardware Audio Synth Beep Engine
    private ToneGenerator mToneGenerator;
    private Random mRandom;

    // Nested Class structures inside single View bundle for safety and direct modular rendering
    public static class Ball {
        float x, y;
        float vx, vy;
        float radius = 18f;
        
        // Tracking lists for trailing paths
        float[] prevX = new float[6];
        float[] prevY = new float[6];
        int prevCount = 0;

        public Ball(float x, float y, float vx, float vy) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
        }

        public void update() {
            // Shift coordinates backwards
            for (int i = prevX.length - 1; i > 0; i--) {
                prevX[i] = prevX[i - 1];
                prevY[i] = prevY[i - 1];
            }
            prevX[0] = x;
            prevY[0] = y;
            if (prevCount < prevX.length) {
                prevCount++;
            }

            x += vx;
            y += vy;
        }
    }

    public static class Brick {
        RectF rect;
        int maxHits;
        int currentHits;
        int color;

        public Brick(float left, float top, float right, float bottom, int hits, int color) {
            this.rect = new RectF(left, top, right, bottom);
            this.maxHits = hits;
            this.currentHits = hits;
            this.color = color;
        }

        public boolean hit() {
            currentHits--;
            return currentHits <= 0;
        }
    }

    public static class PowerUp {
        float x, y;
        float radius = 16f;
        float vy = 6f;
        int type; // 1 = Wide Paddle, 2 = Slow Ball, 3 = Extra Life, 4 = Split Ball

        public PowerUp(float x, float y, int type) {
            this.x = x;
            this.y = y;
            this.type = type;
        }

        public void update() {
            y += vy;
        }
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        mRandom = new Random();
        mHandler = new Handler();
        
        // Setup retro paint resources
        mPaintBrick = new Paint();
        mPaintBrick.setStyle(Paint.Style.FILL);
        
        mPaintPaddle = new Paint();
        mPaintPaddle.setColor(Color.parseColor("#00FFCC"));
        mPaintPaddle.setStyle(Paint.Style.FILL);
        mPaintPaddle.setShadowLayer(8, 0, 0, Color.parseColor("#00FFCC"));

        mPaintBall = new Paint();
        mPaintBall.setColor(Color.parseColor("#FFFFFF"));
        mPaintBall.setStyle(Paint.Style.FILL);
        mPaintBall.setShadowLayer(6, 0, 0, Color.WHITE);

        mPaintPowerUp = new Paint();
        mPaintPowerUp.setStyle(Paint.Style.FILL);

        mPaintBgGrid = new Paint();
        mPaintBgGrid.setColor(Color.parseColor("#1500FFCC"));
        mPaintBgGrid.setStrokeWidth(2);

        mBalls = new ArrayList<Ball>();
        mBricks = new ArrayList<Brick>();
        mPowerUps = new ArrayList<PowerUp>();
        mStars = new ArrayList<Star>();
        mParticles = new ArrayList<Particle>();

        // Init synthesizer with media stream audio level
        try {
            mToneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 70);
        } catch (Exception e) {
            mToneGenerator = null;
        }

        // Define primary hardware-timed game loop thread execution
        mGameLoop = new Runnable() {
            @Override
            public void run() {
                if (mIsPlaying) {
                    updatePhysics();
                    invalidate(); // Redraw Canvas frame
                    mHandler.postDelayed(this, 16); // Direct Target Frame pacing of 60 FPS
                }
            }
        };
    }

    public void setGameListener(GameListener listener) {
        this.mListener = listener;
    }

    public void setSoundEnabled(boolean enabled) {
        this.mSoundEnabled = enabled;
    }

    // Play retro beep effects using direct Tone Synthesizer
    private void playSound(int toneType, int duration) {
        if (mSoundEnabled && mToneGenerator != null) {
            try {
                mToneGenerator.startTone(toneType, duration);
            } catch (Exception ignored) {}
        }
    }

    private void triggerScreenShake(int frames) {
        mShakeFrames = frames;
    }

    // Initial setup for the selected levels
    public void setupLevel(int level) {
        mCurrentLevel = level;
        mPowerUps.clear();
        mBalls.clear();
        mParticles.clear();

        mPaddleWidth = DEFAULT_PADDLE_WIDTH;
        mPaddleHeight = DEFAULT_PADDLE_HEIGHT;

        if (getWidth() > 0 && getHeight() > 0) {
            mPaddleX = (getWidth() - mPaddleWidth) / 2f;
            mPaddleY = getHeight() - 150f;

            // Spawn standard single central launcher ball
            mBalls.add(new Ball(getWidth() / 2f, mPaddleY - 50f, 6f, -12f));

            // Generate brick layouts for levels
            generateBricks(level);
        }
    }

    private void generateBricks(int level) {
        mBricks.clear();
        if (getWidth() <= 0) return;

        int cols = 6;
        int rows = 4 + level; // Level 1 = 5 rows, Level 2 = 6 rows, Level 3 = 7 rows
        float padding = 8f;
        float topOffset = 180f;

        float totalAvailableWidth = getWidth() - (padding * (cols + 1));
        float brickWidth = totalAvailableWidth / cols;
        float brickHeight = 45f;

        // Visual brick palette array matching high arcade retro themes
        int[] palette = {
            Color.parseColor("#FF3366"), // Pinkish Red
            Color.parseColor("#FF9900"), // Intense Orange
            Color.parseColor("#FFFF00"), // Lemon Yellow
            Color.parseColor("#33FF33"), // Retro Lime
            Color.parseColor("#00FFFF"), // Retro Cyan
            Color.parseColor("#CC33FF")  // Retro Purple
        };

        for (int r = 0; r < rows; r++) {
            int hitStrength = 1;
            if (r == 0) hitStrength = 3; // Top Row requires 3 hits
            else if (r == 1 || r == 2) hitStrength = 2; // Medium rows require 2 hits

            int rowColor = palette[r % palette.length];

            for (int c = 0; c < cols; c++) {
                float left = padding + c * (brickWidth + padding);
                float top = topOffset + r * (brickHeight + padding);
                float right = left + brickWidth;
                float bottom = top + brickHeight;

                mBricks.add(new Brick(left, top, right, bottom, hitStrength, rowColor));
            }
        }
    }

    public void startNewGame() {
        mScore = 0;
        mLives = 3;
        mCurrentLevel = 1;
        if (mListener != null) {
            mListener.onScoreChanged(mScore);
            mListener.onLivesChanged(mLives);
            mListener.onLevelChanged(mCurrentLevel);
        }
        setupLevel(mCurrentLevel);
        mIsPlaying = true;
        mHandler.removeCallbacks(mGameLoop);
        mHandler.post(mGameLoop);
        playSound(ToneGenerator.TONE_PROP_ACK, 200);
    }

    public void pauseGame() {
        mIsPlaying = false;
        mHandler.removeCallbacks(mGameLoop);
    }

    public void resumeGame() {
        if (!mIsPlaying && mLives > 0) {
            mIsPlaying = true;
            mHandler.removeCallbacks(mGameLoop);
            mHandler.post(mGameLoop);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        
        // Generate space background star systems
        mStars.clear();
        if (w > 0 && h > 0) {
            for (int i = 0; i < 35; i++) {
                mStars.add(new Star(
                    mRandom.nextFloat() * w,
                    mRandom.nextFloat() * h,
                    0.8f + mRandom.nextFloat() * 1.8f,
                    3f + mRandom.nextFloat() * 5f,
                    80 + mRandom.nextInt(175)
                ));
            }
        }
        setupLevel(mCurrentLevel);
    }

    // Touch events for ultra-responsive paddle tracking
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!mIsPlaying) return true;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                float touchX = event.getX();
                mPaddleX = touchX - (mPaddleWidth / 2f);

                // Boundary collision checking for the paddle
                if (mPaddleX < 0) {
                    mPaddleX = 0;
                }
                if (mPaddleX + mPaddleWidth > getWidth()) {
                    mPaddleX = getWidth() - mPaddleWidth;
                }
                break;
        }
        return true;
    }

    // Frame update sequence - core physics/collision engine
    private void updatePhysics() {
        if (getWidth() <= 0 || getHeight() <= 0) return;

        // 0. UPDATE DECORATIVE ANIMATIONS (Starfield & scroll grid offsets)
        for (int i = 0; i < mStars.size(); i++) {
            mStars.get(i).update(getHeight());
        }
        mGridOffset += 1.5f;
        if (mGridOffset >= 80f) {
            mGridOffset = 0f;
        }

        // Apply screen shake reduction steps
        if (mShakeFrames > 0) {
            mShakeX = (mRandom.nextFloat() * 2f - 1f) * SHAKE_INTENSITY;
            mShakeY = (mRandom.nextFloat() * 2f - 1f) * SHAKE_INTENSITY;
            mShakeFrames--;
        } else {
            mShakeX = 0f;
            mShakeY = 0f;
        }

        // Update active explosive particles
        ArrayList<Particle> deadParticles = new ArrayList<Particle>();
        for (int i = 0; i < mParticles.size(); i++) {
            Particle p = mParticles.get(i);
            if (p.update()) {
                deadParticles.add(p);
            }
        }
        for (int i = 0; i < deadParticles.size(); i++) {
            mParticles.remove(deadParticles.get(i));
        }

        // 1. BALL PHYSICS & SCREEN COLLISION BOUNDS
        ArrayList<Ball> lostBalls = new ArrayList<Ball>();

        for (int i = 0; i < mBalls.size(); i++) {
            Ball b = mBalls.get(i);
            b.update();

            // Left/Right Wall Bouncing
            if (b.x - b.radius < 0) {
                b.x = b.radius;
                b.vx = -b.vx;
                playSound(ToneGenerator.TONE_PROP_BEEP, 50);
            } else if (b.x + b.radius > getWidth()) {
                b.x = getWidth() - b.radius;
                b.vx = -b.vx;
                playSound(ToneGenerator.TONE_PROP_BEEP, 50);
            }

            // Ceiling Bounce
            if (b.y - b.radius < 0) {
                b.y = b.radius;
                b.vy = -b.vy;
                playSound(ToneGenerator.TONE_PROP_BEEP, 50);
            }

            // Bottom Gutter (Loss of single ball instance)
            if (b.y - b.radius > getHeight()) {
                lostBalls.add(b);
            }
        }

        // Clean up lost balls
        for (int i = 0; i < lostBalls.size(); i++) {
            mBalls.remove(lostBalls.get(i));
        }

        // If all active balls are gone, player loses a life
        if (mBalls.isEmpty()) {
            mLives--;
            triggerScreenShake(16); // Heavy screen shake on player life loss
            playSound(ToneGenerator.TONE_PROP_NACK, 300);
            if (mListener != null) {
                mListener.onLivesChanged(mLives);
            }

            if (mLives <= 0) {
                mIsPlaying = false;
                if (mListener != null) {
                    mListener.onGameOver(mScore, false);
                }
                return;
            } else {
                // Respawn standard launcher ball
                mBalls.add(new Ball(getWidth() / 2f, mPaddleY - 50f, 6f, -12f));
                // Clear active power-ups to reset stage pace
                mPowerUps.clear();
                mPaddleWidth = DEFAULT_PADDLE_WIDTH;
            }
        }

        // 2. BALL VS PADDLE BOUNCE RESOLUTION
        for (int i = 0; i < mBalls.size(); i++) {
            Ball b = mBalls.get(i);

            // Bounding collision check
            if (b.y + b.radius >= mPaddleY && b.y - b.radius <= mPaddleY + mPaddleHeight) {
                if (b.x >= mPaddleX && b.x <= mPaddleX + mPaddleWidth) {
                    // Normalize bounce position to dynamically scale bounce speed angles
                    float hitPercent = (b.x - mPaddleX) / mPaddleWidth;
                    float angleDeg = 180f - (hitPercent * 140f + 20f); // Map from 20 to 160 degrees
                    float angleRad = (float) Math.toRadians(angleDeg);

                    float speed = (float) Math.sqrt(b.vx * b.vx + b.vy * b.vy);
                    speed = Math.min(speed + 0.3f, 22f); // Gradually boost physics pacing

                    b.vx = speed * (float) Math.cos(angleRad);
                    b.vy = -speed * (float) Math.sin(angleRad);

                    // Ensure ball sits properly on top of paddle to prevent multi-hit trapping glitches
                    b.y = mPaddleY - b.radius;

                    // Little impact spark particles on the paddle
                    for (int pIdx = 0; pIdx < 3; pIdx++) {
                        mParticles.add(new Particle(b.x, b.y, (mRandom.nextFloat() * 4f - 2f), -3f - mRandom.nextFloat() * 3f, 4f, Color.parseColor("#00FFCC"), 8f));
                    }

                    playSound(ToneGenerator.TONE_PROP_BEEP2, 60);
                }
            }
        }

        // 3. BALL VS BRICKS DETAILED COLLISION AND DESTRUCTION
        for (int i = 0; i < mBalls.size(); i++) {
            Ball b = mBalls.get(i);
            boolean ballRedirected = false;

            for (int j = 0; j < mBricks.size(); j++) {
                Brick brick = mBricks.get(j);

                // Simple collision vector checks
                if (RectF.intersects(brick.rect, new RectF(b.x - b.radius, b.y - b.radius, b.x + b.radius, b.y + b.radius))) {
                    if (!ballRedirected) {
                        // Check hit sides (Horizontal versus Vertical)
                        float overlapLeft = (b.x + b.radius) - brick.rect.left;
                        float overlapRight = brick.rect.right - (b.x - b.radius);
                        float overlapTop = (b.y + b.radius) - brick.rect.top;
                        float overlapBottom = brick.rect.bottom - (b.y - b.radius);

                        float minX = Math.min(overlapLeft, overlapRight);
                        float minY = Math.min(overlapTop, overlapBottom);

                        if (minX < minY) {
                            b.vx = -b.vx; // Reverse horizontal flow
                        } else {
                            b.vy = -b.vy; // Reverse vertical flow
                        }
                        ballRedirected = true;
                    }

                    // Register brick damage
                    if (brick.hit()) {
                        mBricks.remove(j);
                        mScore += 100 * mCurrentLevel;
                        if (mListener != null) {
                            mListener.onScoreChanged(mScore);
                        }

                        // Chance to drop interactive arcade PowerUps (20% drop rate)
                        if (mRandom.nextFloat() < 0.20f) {
                            int type = mRandom.nextInt(4) + 1; // Type 1 to 4
                            mPowerUps.add(new PowerUp(brick.rect.centerX(), brick.rect.centerY(), type));
                        }

                        // Explode beautiful colored neon particles
                        int numParticles = 12;
                        for (int pIdx = 0; pIdx < numParticles; pIdx++) {
                            float angle = mRandom.nextFloat() * 360f;
                            float rad = (float) Math.toRadians(angle);
                            float speed = 2f + mRandom.nextFloat() * 6f;
                            float vx = (float) Math.cos(rad) * speed;
                            float vy = (float) Math.sin(rad) * speed;
                            float pSize = 5f + mRandom.nextFloat() * 6f;
                            float fade = 3f + mRandom.nextFloat() * 5f;
                            mParticles.add(new Particle(brick.rect.centerX(), brick.rect.centerY(), vx, vy, pSize, brick.color, fade));
                        }

                        triggerScreenShake(8);
                        playSound(ToneGenerator.TONE_CDMA_PIP, 45);
                    } else {
                        mScore += 25 * mCurrentLevel;
                        if (mListener != null) {
                            mListener.onScoreChanged(mScore);
                        }

                        // Tiny damage sparkles on brick structure
                        int numParticles = 4;
                        for (int pIdx = 0; pIdx < numParticles; pIdx++) {
                            float angle = mRandom.nextFloat() * 360f;
                            float rad = (float) Math.toRadians(angle);
                            float speed = 1f + mRandom.nextFloat() * 3f;
                            float vx = (float) Math.cos(rad) * speed;
                            float vy = (float) Math.sin(rad) * speed;
                            float pSize = 3f + mRandom.nextFloat() * 4f;
                            float fade = 5f + mRandom.nextFloat() * 5f;
                            mParticles.add(new Particle(b.x, b.y, vx, vy, pSize, brick.color, fade));
                        }

                        triggerScreenShake(3);
                        playSound(ToneGenerator.TONE_CDMA_ANSWER, 35);
                    }
                    break; // Process next step in physics loops safely
                }
            }
        }

        // 4. CHECK WINNING CONDITIONS
        if (mBricks.isEmpty()) {
            mIsPlaying = false;
            if (mCurrentLevel < MAX_LEVEL) {
                mCurrentLevel++;
                if (mListener != null) {
                    mListener.onLevelChanged(mCurrentLevel);
                }
                setupLevel(mCurrentLevel);
                mIsPlaying = true;
                mHandler.post(mGameLoop);
                playSound(ToneGenerator.TONE_PROP_ACK, 250);
            } else {
                if (mListener != null) {
                    mListener.onGameWon(mScore, false);
                }
            }
            return;
        }

        // 5. POWER-UPS FLOW AND CAPTURE LOGIC
        ArrayList<PowerUp> powerUpsLost = new ArrayList<PowerUp>();
        for (int i = 0; i < mPowerUps.size(); i++) {
            PowerUp p = mPowerUps.get(i);
            p.update();

            // Intersect with moving screen paddle bounds
            if (p.y + p.radius >= mPaddleY && p.y - p.radius <= mPaddleY + mPaddleHeight) {
                if (p.x >= mPaddleX && p.x <= mPaddleX + mPaddleWidth) {
                    // Activate power-up action based on type
                    applyPowerUp(p.type);
                    powerUpsLost.add(p);
                    playSound(ToneGenerator.TONE_PROP_PROMPT, 80);
                    continue;
                }
            }

            // Gutter boundary removal
            if (p.y - p.radius > getHeight()) {
                powerUpsLost.add(p);
            }
        }

        // Wipe resolved power-ups
        for (int i = 0; i < powerUpsLost.size(); i++) {
            mPowerUps.remove(powerUpsLost.get(i));
        }
    }

    private void applyPowerUp(int type) {
        switch (type) {
            case 1: // Widen paddle width
                mPaddleWidth = Math.min(mPaddleWidth + 100f, 400f);
                break;
            case 2: // Slow down current balls velocities
                for (int i = 0; i < mBalls.size(); i++) {
                    Ball b = mBalls.get(i);
                    b.vx *= 0.7f;
                    b.vy *= 0.7f;
                }
                break;
            case 3: // Extra Life reward
                mLives++;
                if (mListener != null) {
                    mListener.onLivesChanged(mLives);
                }
                break;
            case 4: // Multi-ball trigger (Splits active balls)
                if (mBalls.size() < 6) {
                    int originalCount = mBalls.size();
                    for (int i = 0; i < originalCount; i++) {
                        Ball parent = mBalls.get(i);
                        // Launch two offspring at mirrored flight angles
                        mBalls.add(new Ball(parent.x, parent.y, parent.vx - 3f, -Math.abs(parent.vy)));
                        mBalls.add(new Ball(parent.x, parent.y, parent.vx + 3f, -Math.abs(parent.vy)));
                    }
                }
                break;
        }
    }

    // High performance rendering method mapping raw objects onto the canvas views
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Apply dynamic translation for screen shake
        canvas.save();
        canvas.translate(mShakeX, mShakeY);

        // A. Draw Retro Background Space Twinkles
        Paint starPaint = new Paint();
        starPaint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < mStars.size(); i++) {
            Star star = mStars.get(i);
            starPaint.setColor(Color.WHITE);
            starPaint.setAlpha(star.alpha);
            canvas.drawCircle(star.x, star.y, star.size, starPaint);
        }

        // B. Draw Retro Background Matrix Grid
        drawBackgroundGrid(canvas);

        // C. Draw Bricks Grid
        for (int i = 0; i < mBricks.size(); i++) {
            Brick b = mBricks.get(i);
            mPaintBrick.setColor(b.color);
            
            // Render full brick or cracked variant depending on remaining hits required
            if (b.currentHits < b.maxHits) {
                mPaintBrick.setAlpha(120); // semi-translucent styling representing damage
            } else {
                mPaintBrick.setAlpha(255);
            }
            canvas.drawRect(b.rect, mPaintBrick);

            // Specular shiny bar across the top of the brick
            Paint glassPaint = new Paint();
            glassPaint.setColor(Color.WHITE);
            glassPaint.setAlpha(80);
            canvas.drawRect(b.rect.left, b.rect.top, b.rect.right, b.rect.top + (b.rect.height() * 0.25f), glassPaint);

            // Draw clean flat vector outline
            mPaintBrick.setStyle(Paint.Style.STROKE);
            mPaintBrick.setColor(Color.parseColor("#44000000"));
            mPaintBrick.setStrokeWidth(3);
            canvas.drawRect(b.rect, mPaintBrick);
            mPaintBrick.setStyle(Paint.Style.FILL);

            // Render strength counter values inside multi-hit bricks
            if (b.maxHits > 1) {
                Paint textPaint = new Paint();
                textPaint.setColor(Color.BLACK);
                textPaint.setAlpha(160);
                textPaint.setTextSize(b.rect.height() * 0.5f);
                textPaint.setTextAlign(Paint.Align.CENTER);
                textPaint.setTypeface(android.graphics.Typeface.MONOSPACE);
                textPaint.setFakeBoldText(true);
                float yOffset = (textPaint.descent() + textPaint.ascent()) / 2f;
                canvas.drawText(String.valueOf(b.currentHits), b.rect.centerX(), b.rect.centerY() - yOffset, textPaint);
            }
        }

        // D. Draw Paddle with tubular neon gradients
        mPaintPaddle.setColor(Color.parseColor("#00FFCC"));
        mPaintPaddle.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(new RectF(mPaddleX, mPaddleY, mPaddleX + mPaddleWidth, mPaddleY + mPaddleHeight), 15f, 15f, mPaintPaddle);

        Paint paddleInner = new Paint();
        paddleInner.setColor(Color.parseColor("#121216"));
        canvas.drawRoundRect(new RectF(mPaddleX + 6, mPaddleY + 6, mPaddleX + mPaddleWidth - 6, mPaddleY + mPaddleHeight - 6), 10f, 10f, paddleInner);

        paddleInner.setColor(Color.WHITE);
        paddleInner.setAlpha(180);
        canvas.drawRoundRect(new RectF(mPaddleX + 12, mPaddleY + 8, mPaddleX + mPaddleWidth - 12, mPaddleY + mPaddleHeight - 14), 4f, 4f, paddleInner);

        // E. Draw Dropping PowerUp items with styled identifiers
        for (int i = 0; i < mPowerUps.size(); i++) {
            PowerUp p = mPowerUps.get(i);
            int pColor = Color.WHITE;
            String label = "?";
            switch (p.type) {
                case 1:
                    pColor = Color.parseColor("#00FFCC"); // Neon Cyan [W]
                    label = "W";
                    break;
                case 2:
                    pColor = Color.parseColor("#FFFF00"); // Neon Yellow [S]
                    label = "S";
                    break;
                case 3:
                    pColor = Color.parseColor("#FF3366"); // Neon Pink/Red [♥]
                    label = "♥";
                    break;
                case 4:
                    pColor = Color.parseColor("#CC33FF"); // Neon Purple [M]
                    label = "M";
                    break;
            }
            mPaintPowerUp.setColor(pColor);
            canvas.drawCircle(p.x, p.y, p.radius, mPaintPowerUp);

            // Draw clean structural outer border
            mPaintPowerUp.setStyle(Paint.Style.STROKE);
            mPaintPowerUp.setColor(Color.WHITE);
            mPaintPowerUp.setStrokeWidth(3);
            canvas.drawCircle(p.x, p.y, p.radius - 1, mPaintPowerUp);
            mPaintPowerUp.setStyle(Paint.Style.FILL);

            // Draw labeled text inside
            Paint labelPaint = new Paint();
            labelPaint.setColor(Color.BLACK);
            labelPaint.setTextSize(p.radius * 1.3f);
            labelPaint.setTextAlign(Paint.Align.CENTER);
            labelPaint.setTypeface(android.graphics.Typeface.MONOSPACE);
            labelPaint.setFakeBoldText(true);
            float yOffset = (labelPaint.descent() + labelPaint.ascent()) / 2f;
            canvas.drawText(label, p.x, p.y - yOffset, labelPaint);
        }

        // F. Draw Active Emitter Particles
        Paint particlePaint = new Paint();
        for (int i = 0; i < mParticles.size(); i++) {
            Particle p = mParticles.get(i);
            particlePaint.setColor(p.color);
            particlePaint.setAlpha((int) p.alpha);
            canvas.drawCircle(p.x, p.y, p.size, particlePaint);
        }

        // G. Draw Active Balls with Trails
        for (int i = 0; i < mBalls.size(); i++) {
            Ball b = mBalls.get(i);

            // Render fading motion trailing segments
            for (int j = 0; j < b.prevCount; j++) {
                int opacity = (int) (140 * (1f - (float) j / b.prevX.length));
                mPaintBall.setAlpha(opacity);
                float radiusReduction = b.radius * (1f - 0.15f * j);
                canvas.drawCircle(b.prevX[j], b.prevY[j], radiusReduction, mPaintBall);
            }
            mPaintBall.setAlpha(255);

            // Main body
            mPaintBall.setColor(Color.WHITE);
            canvas.drawCircle(b.x, b.y, b.radius, mPaintBall);

            // Specular light glow highlight
            mPaintBall.setColor(Color.parseColor("#E0F7FA"));
            canvas.drawCircle(b.x - b.radius * 0.3f, b.y - b.radius * 0.3f, b.radius * 0.35f, mPaintBall);
            mPaintBall.setColor(Color.WHITE);
        }

        // Restore translation matrices
        canvas.restore();
    }

    private void drawBackgroundGrid(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();
        int step = 80;

        // Draw scrolling horizontal grid lines
        for (float y = mGridOffset; y < height; y += step) {
            canvas.drawLine(0, y, width, y, mPaintBgGrid);
        }
        // Draw static vertical lines
        for (int x = 0; x < width; x += step) {
            canvas.drawLine(x, 0, x, height, mPaintBgGrid);
        }
    }
}