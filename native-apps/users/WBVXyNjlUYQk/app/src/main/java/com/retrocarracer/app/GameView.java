package com.retrocarracer.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

public class GameView extends View {

    public interface GameInteractionListener {
        void onScoreUpdated(int score);
        void onCoinCollected();
        void onCrashOccurred();
    }

    private GameInteractionListener mListener;
    private Paint mPaint;
    private Handler mHandler;
    private Runnable mGameLoop;

    private boolean mIsRunning = false;
    private int mScore = 0;
    private float mBaseSpeed = 8f;

    // Track state
    private float mRoadWidthRatio = 0.70f;
    private float mRoadLeft;
    private float mRoadRight;
    private float mRoadLineOffset = 0f;

    // Player attributes
    private float mPlayerX = 0f;
    private float mPlayerY = 0f;
    private float mPlayerWidth = 0f;
    private float mPlayerHeight = 0f;
    private float mTargetPlayerX = 0f;
    private float mPlayerSteeringSpeed = 15f;

    // Entities List
    private List<GameEntity> mEntities;
    private long mFrameCounter = 0;

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mEntities = new ArrayList<>();
        mHandler = new Handler();

        mGameLoop = new Runnable() {
            @Override
            public void run() {
                if (mIsRunning) {
                    updatePhysics();
                    invalidate();
                    mHandler.postDelayed(this, 16); // ~60fps standard execution
                }
            }
        };
    }

    public void setGameInteractionListener(GameInteractionListener listener) {
        this.mListener = listener;
    }

    public void startGame() {
        mEntities.clear();
        mScore = 0;
        mBaseSpeed = 8f;
        mFrameCounter = 0;
        mIsRunning = true;
        mPlayerX = getWidth() / 2f;
        mTargetPlayerX = mPlayerX;
        
        mHandler.removeCallbacks(mGameLoop);
        mHandler.post(mGameLoop);
    }

    public void stopGame() {
        mIsRunning = false;
        mHandler.removeCallbacks(mGameLoop);
    }

    public void steerLeft() {
        if (!mIsRunning) return;
        mTargetPlayerX = Math.max(mRoadLeft + mPlayerWidth/2f, mPlayerX - (getWidth() * 0.18f));
    }

    public void steerRight() {
        if (!mIsRunning) return;
        mTargetPlayerX = Math.min(mRoadRight - mPlayerWidth/2f, mPlayerX + (getWidth() * 0.18f));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float roadWidth = w * mRoadWidthRatio;
        mRoadLeft = (w - roadWidth) / 2f;
        mRoadRight = mRoadLeft + roadWidth;

        mPlayerWidth = w * 0.12f;
        mPlayerHeight = mPlayerWidth * 1.7f;
        mPlayerX = w / 2f;
        mTargetPlayerX = mPlayerX;
        mPlayerY = h - mPlayerHeight - 150f;
    }

    private void updatePhysics() {
        mFrameCounter++;

        // Interpolate steering controls smoothly
        if (Math.abs(mPlayerX - mTargetPlayerX) > 1.0f) {
            if (mPlayerX < mTargetPlayerX) {
                mPlayerX += Math.min(mPlayerSteeringSpeed, mTargetPlayerX - mPlayerX);
            } else {
                mPlayerX -= Math.min(mPlayerSteeringSpeed, mPlayerX - mTargetPlayerX);
            }
        }

        // Road movement feel
        mRoadLineOffset += mBaseSpeed;
        if (mRoadLineOffset > 100f) {
            mRoadLineOffset = 0f;
        }

        // Spawn obstacles and bonus coins randomly
        if (mFrameCounter % 45 == 0) {
            spawnEntity();
        }

        // Gradual gameplay acceleration
        if (mFrameCounter % 300 == 0) {
            mBaseSpeed += 0.5f;
        }

        // Progress entities
        for (int i = mEntities.size() - 1; i >= 0; i--) {
            GameEntity entity = mEntities.get(i);
            entity.update(mBaseSpeed);

            // Clean up off-screen items
            if (entity.y > getHeight() + 100) {
                mEntities.remove(i);
                if (entity.type == GameEntity.TYPE_CAR && !entity.collected) {
                    mScore += 10;
                    if (mListener != null) {
                        mListener.onScoreUpdated(mScore);
                    }
                }
                continue;
            }

            // Check boundaries overlaps
            if (!entity.collected && checkOverlap(mPlayerX - mPlayerWidth/2f, mPlayerY, mPlayerWidth, mPlayerHeight,
                    entity.x - entity.width/2f, entity.y, entity.width, entity.height)) {
                
                if (entity.type == GameEntity.TYPE_CAR) {
                    // Collision triggers GAME OVER
                    stopGame();
                    if (mListener != null) {
                        mListener.onCrashOccurred();
                    }
                } else if (entity.type == GameEntity.TYPE_COIN) {
                    entity.collected = true;
                    mScore += 50;
                    mEntities.remove(i);
                    if (mListener != null) {
                        mListener.onCoinCollected();
                        mListener.onScoreUpdated(mScore);
                    }
                } else if (entity.type == GameEntity.TYPE_SLICK) {
                    // Spin-out slick slides car away
                    entity.collected = true;
                    if (mPlayerX > getWidth() / 2f) {
                        mTargetPlayerX = mRoadLeft + mPlayerWidth/2f;
                    } else {
                        mTargetPlayerX = mRoadRight - mPlayerWidth/2f;
                    }
                }
            }
        }
    }

    private void spawnEntity() {
        int lane = (int) (Math.random() * 3); // 3-lane design
        float laneWidth = (mRoadRight - mRoadLeft) / 3f;
        float spawnX = mRoadLeft + (lane * laneWidth) + (laneWidth / 2f);
        float spawnY = -120f;

        double chance = Math.random();
        GameEntity entity;
        if (chance < 0.6) {
            entity = new GameEntity(spawnX, spawnY, GameEntity.TYPE_CAR, mPlayerWidth * 0.9f, mPlayerHeight * 0.95f);
        } else if (chance < 0.85) {
            entity = new GameEntity(spawnX, spawnY, GameEntity.TYPE_COIN, mPlayerWidth * 0.5f, mPlayerWidth * 0.5f);
        } else {
            entity = new GameEntity(spawnX, spawnY, GameEntity.TYPE_SLICK, mPlayerWidth * 0.8f, mPlayerWidth * 0.5f);
        }
        mEntities.add(entity);
    }

    private boolean checkOverlap(float x1, float y1, float w1, float h1, float x2, float y2, float w2, float h2) {
        return x1 < x2 + w2 && x1 + w1 > x2 && y1 < y2 + h2 && y1 + h1 > y2;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();

        // 1. Fill Side Grass
        mPaint.setColor(Color.rgb(34, 139, 34));
        canvas.drawRect(0, 0, w, h, mPaint);

        // 2. Draw Asphalt Center Lane
        mPaint.setColor(Color.rgb(44, 44, 44));
        canvas.drawRect(mRoadLeft, 0, mRoadRight, h, mPaint);

        // 3. Side Boundary Road Lines
        mPaint.setColor(Color.WHITE);
        mPaint.setStrokeWidth(8f);
        canvas.drawLine(mRoadLeft, 0, mRoadLeft, h, mPaint);
        canvas.drawLine(mRoadRight, 0, mRoadRight, h, mPaint);

        // 4. White Dash Lane Dividers
        mPaint.setColor(Color.rgb(255, 235, 59));
        mPaint.setStrokeWidth(5f);
        float laneWidth = (mRoadRight - mRoadLeft) / 3f;
        for (int laneIdx = 1; laneIdx <= 2; laneIdx++) {
            float lineX = mRoadLeft + (laneIdx * laneWidth);
            float startY = -100f + mRoadLineOffset;
            while (startY < h) {
                canvas.drawLine(lineX, startY, lineX, startY + 40f, mPaint);
                startY += 80f;
            }
        }

        // 5. Render Generated Entities
        for (int i = 0; i < mEntities.size(); i++) {
            GameEntity entity = mEntities.get(i);
            if (entity.collected) continue;

            if (entity.type == GameEntity.TYPE_CAR) {
                drawClassicSportsCar(canvas, entity.x, entity.y, entity.width, entity.height, entity.color, true);
            } else if (entity.type == GameEntity.TYPE_COIN) {
                mPaint.setColor(entity.color);
                canvas.drawCircle(entity.x, entity.y + entity.height/2f, entity.width / 2f, mPaint);
                mPaint.setColor(Color.rgb(245, 166, 35));
                canvas.drawCircle(entity.x, entity.y + entity.height/2f, entity.width / 3.5f, mPaint);
            } else if (entity.type == GameEntity.TYPE_SLICK) {
                mPaint.setColor(entity.color);
                RectF oval = new RectF(entity.x - entity.width/2f, entity.y, entity.x + entity.width/2f, entity.y + entity.height);
                canvas.drawOval(oval, mPaint);
            }
        }

        // 6. Draw Player Car
        drawClassicSportsCar(canvas, mPlayerX, mPlayerY, mPlayerWidth, mPlayerHeight, Color.rgb(230, 0, 0), false);
    }

    private void drawClassicSportsCar(Canvas canvas, float cx, float cy, float w, float h, int bodyColor, boolean isFacingDown) {
        float rxLeft = cx - w/2f;
        float rxRight = cx + w/2f;
        float rxTop = cy;
        float rxBottom = cy + h;

        // Draw main retro chassis body
        mPaint.setColor(bodyColor);
        RectF chassis = new RectF(rxLeft + w * 0.1f, rxTop, rxRight - w * 0.1f, rxBottom);
        canvas.drawRoundRect(chassis, 15f, 15f, mPaint);

        // Tires
        mPaint.setColor(Color.BLACK);
        float tireW = w * 0.15f;
        float tireH = h * 0.18f;

        // Draw wheels offset correctly based on scale orientation
        RectF wheelTL = new RectF(rxLeft, rxTop + h * 0.12f, rxLeft + tireW, rxTop + h * 0.12f + tireH);
        RectF wheelTR = new RectF(rxRight - tireW, rxTop + h * 0.12f, rxRight, rxTop + h * 0.12f + tireH);
        RectF wheelBL = new RectF(rxLeft, rxBottom - h * 0.30f, rxLeft + tireW, rxBottom - h * 0.30f + tireH);
        RectF wheelBR = new RectF(rxRight - tireW, rxBottom - h * 0.30f, rxRight, rxBottom - h * 0.30f + tireH);

        canvas.drawRoundRect(wheelTL, 6f, 6f, mPaint);
        canvas.drawRoundRect(wheelTR, 6f, 6f, mPaint);
        canvas.drawRoundRect(wheelBL, 6f, 6f, mPaint);
        canvas.drawRoundRect(wheelBR, 6f, 6f, mPaint);

        // Windshield Glass Window
        mPaint.setColor(Color.rgb(173, 216, 230));
        RectF glass;
        if (isFacingDown) {
            glass = new RectF(rxLeft + w * 0.22f, rxTop + h * 0.25f, rxRight - w * 0.22f, rxTop + h * 0.45f);
        } else {
            glass = new RectF(rxLeft + w * 0.22f, rxBottom - h * 0.55f, rxRight - w * 0.22f, rxBottom - h * 0.35f);
        }
        canvas.drawRoundRect(glass, 5f, 5f, mPaint);

        // Spoiler Wing
        mPaint.setColor(Color.rgb(30, 30, 30));
        RectF wing;
        if (isFacingDown) {
            wing = new RectF(rxLeft + w * 0.05f, rxTop - h * 0.04f, rxRight - w * 0.05f, rxTop + h * 0.05f);
        } else {
            wing = new RectF(rxLeft + w * 0.05f, rxBottom - h * 0.05f, rxRight - w * 0.05f, rxBottom + h * 0.04f);
        }
        canvas.drawRect(wing, mPaint);
    }
}