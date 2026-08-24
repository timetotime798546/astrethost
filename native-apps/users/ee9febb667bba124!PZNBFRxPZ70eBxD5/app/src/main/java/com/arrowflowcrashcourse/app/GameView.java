package com.arrowflowcrashcourse.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class GameView extends View {
    private GameEngine gameEngine;
    private Paint gridPaint;
    private Paint arrowPaint;
    private Paint accentPaint;
    private Paint bgPaint;
    private Paint textPaint;

    private int cols = 6;
    private int rows = 8;
    private float cellWidth;
    private float cellHeight;
    private float padding = 40f;

    private long lastFrameTime = 0;
    private boolean isRunning = false;

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setStrokeWidth(3f);
        gridPaint.setStyle(Paint.Style.STROKE);

        arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arrowPaint.setStyle(Paint.Style.FILL);

        accentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        accentPaint.setStyle(Paint.Style.FILL);

        bgPaint = new Paint();

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
    }

    public void setGameEngine(GameEngine engine) {
        this.gameEngine = engine;
        if (engine != null && engine.getLevelData() != null) {
            this.cols = engine.getLevelData().cols;
            this.rows = engine.getLevelData().rows;
        }
        lastFrameTime = System.nanoTime();
        invalidate();
    }

    public void startAnimation() {
        isRunning = true;
        lastFrameTime = System.nanoTime();
        postInvalidateOnAnimation();
    }

    public void stopAnimation() {
        isRunning = false;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        calculateDimensions(w, h);
    }

    private void calculateDimensions(int w, int h) {
        float availableWidth = w - (padding * 2);
        float availableHeight = h - (padding * 2);

        cellWidth = availableWidth / cols;
        cellHeight = availableHeight / rows;

        if (gameEngine != null && gameEngine.getLevelData() != null) {
            for (Arrow arrow : gameEngine.getLevelData().arrows) {
                if (arrow.state == ArrowState.IDLE) {
                    arrow.posX = padding + (arrow.gridX * cellWidth) + (cellWidth / 2.0f);
                    arrow.posY = padding + (arrow.gridY * cellHeight) + (cellHeight / 2.0f);
                    arrow.startX = arrow.posX;
                    arrow.startY = arrow.posY;
                }
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        boolean dark = ThemeManager.isDarkMode(getContext());
        bgPaint.setColor(ThemeManager.getBackgroundColor(dark));
        canvas.drawRect(0, 0, getWidth(), getHeight(), bgPaint);

        if (gameEngine == null) return;

        long now = System.nanoTime();
        float dt = (now - lastFrameTime) / 1000000000f;
        lastFrameTime = now;

        if (dt > 0.05f) dt = 0.05f;

        gameEngine.update(dt, cellWidth, cellHeight, getWidth(), getHeight());

        gridPaint.setColor(ThemeManager.getGridColor(dark));
        for (int c = 0; c <= cols; c++) {
            float x = padding + c * cellWidth;
            canvas.drawLine(x, padding, x, padding + rows * cellHeight, gridPaint);
        }
        for (int r = 0; r <= rows; r++) {
            float y = padding + r * cellHeight;
            canvas.drawLine(padding, y, padding + cols * cellWidth, y, gridPaint);
        }

        for (Arrow arrow : gameEngine.getLevelData().arrows) {
            if (arrow.state == ArrowState.EXITED) continue;

            if (arrow.state == ArrowState.CRASHED) {
                arrowPaint.setColor(ThemeManager.getSecondaryAccent(dark));
            } else {
                arrowPaint.setColor(ThemeManager.getPrimaryAccent(dark));
            }

            drawCosmicArrow(canvas, arrow.posX, arrow.posY, arrow.direction, arrow.state == ArrowState.CRASHED);
        }

        for (Particle p : gameEngine.getParticles()) {
            accentPaint.setColor(p.color);
            accentPaint.setAlpha((int) (p.alpha * 255));
            canvas.drawCircle(p.x, p.y, p.size, accentPaint);
        }
        accentPaint.setAlpha(255);

        if (isRunning) {
            postInvalidateOnAnimation();
        }
    }

    private void drawCosmicArrow(Canvas canvas, float cx, float cy, ArrowDirection dir, boolean isCrashed) {
        float size = Math.min(cellWidth, cellHeight) * 0.32f;
        canvas.save();
        canvas.translate(cx, cy);

        switch (dir) {
            case UP:
                canvas.rotate(0);
                break;
            case RIGHT:
                canvas.rotate(90);
                break;
            case DOWN:
                canvas.rotate(180);
                break;
            case LEFT:
                canvas.rotate(270);
                break;
        }

        Path path = new Path();
        if (isCrashed) {
            path.moveTo(0, -size);
            path.lineTo(size * 0.5f, -size * 0.2f);
            path.lineTo(size * 0.2f, -size * 0.1f);
            path.lineTo(size * 0.6f, size * 0.4f);
            path.lineTo(0, size * 0.1f);
            path.lineTo(-size * 0.6f, size * 0.4f);
            path.lineTo(-size * 0.2f, -size * 0.1f);
            path.lineTo(-size * 0.5f, -size * 0.2f);
        } else {
            path.moveTo(0, -size);
            path.lineTo(size * 0.7f, size * 0.3f);
            path.lineTo(size * 0.25f, size * 0.15f);
            path.lineTo(size * 0.25f, size * 0.8f);
            path.lineTo(-size * 0.25f, size * 0.8f);
            path.lineTo(-size * 0.25f, size * 0.15f);
            path.lineTo(-size * 0.7f, size * 0.3f);
        }
        path.close();

        canvas.drawPath(path, arrowPaint);
        canvas.restore();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) { 
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float x = event.getX();
            float y = event.getY();

            if (gameEngine != null && gameEngine.getLevelData() != null) {
                for (Arrow arrow : gameEngine.getLevelData().arrows) {
                    if (arrow.state != ArrowState.IDLE) continue;

                    float dx = x - arrow.posX;
                    float dy = y - arrow.posY;
                    float clickRadius = Math.min(cellWidth, cellHeight) * 0.6f;

                    if ((dx * dx + dy * dy) < (clickRadius * clickRadius)) {
                        if (gameEngine.triggerArrow(arrow, cellWidth, cellHeight)) {
                            if (!isRunning) {
                                startAnimation();
                            }
                            return true;
                        }
                    }
                }
            }
        }
        return super.onTouchEvent(event);
    }
}