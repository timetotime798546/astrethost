package com.brickgameclassic.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * Authentic Retro LCD Screen custom component.
 * Renders the dot-matrix LCD glass, active & ghost pixel blocks,
 * side information panel, preview box, indicators, and retro pixel mascot.
 */
public class LcdScreenView extends View {

    private BrickGameEngine engine;

    // Paints
    private Paint bgPaint;
    private Paint activePixelPaint;
    private Paint innerPixelPaint;
    private Paint ghostPixelPaint;
    private Paint dividerPaint;
    private Paint textLabelPaint;
    private Paint textValuePaint;
    private Paint bannerBgPaint;
    private Paint bannerTextPaint;

    private final RectF cellRect = new RectF();
    private final RectF innerRect = new RectF();

    // Pixel art for cute 8x8 retro mascot running/dancing
    private static final byte[][][] MASCOT = {
        {
            {0,0,1,1,1,1,0,0},
            {0,1,1,0,0,1,1,0},
            {1,1,1,1,1,1,1,1},
            {1,0,1,1,1,1,0,1},
            {1,1,1,1,1,1,1,1},
            {0,0,1,0,0,1,0,0},
            {0,1,1,1,1,1,1,0},
            {1,0,1,0,0,1,0,1}
        },
        {
            {0,0,1,1,1,1,0,0},
            {0,1,1,0,0,1,1,0},
            {1,1,1,1,1,1,1,1},
            {1,0,1,1,1,1,0,1},
            {1,1,1,1,1,1,1,1},
            {0,1,1,0,0,1,1,0},
            {1,0,1,1,1,1,0,1},
            {0,1,0,0,0,0,1,0}
        }
    };

    public LcdScreenView(Context context) {
        super(context);
        init();
    }

    public LcdScreenView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LcdScreenView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        bgPaint = new Paint();
        bgPaint.setColor(Color.parseColor("#9EAE8E"));
        bgPaint.setStyle(Paint.Style.FILL);

        activePixelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        activePixelPaint.setColor(Color.parseColor("#182312"));
        activePixelPaint.setStyle(Paint.Style.FILL);

        innerPixelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        innerPixelPaint.setColor(Color.parseColor("#9EAE8E"));
        innerPixelPaint.setStyle(Paint.Style.FILL);

        ghostPixelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ghostPixelPaint.setColor(Color.parseColor("#8E9E7E"));
        ghostPixelPaint.setStyle(Paint.Style.STROKE);
        ghostPixelPaint.setStrokeWidth(1.5f);

        dividerPaint = new Paint();
        dividerPaint.setColor(Color.parseColor("#182312"));
        dividerPaint.setStrokeWidth(3f);

        textLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textLabelPaint.setColor(Color.parseColor("#182312"));
        textLabelPaint.setTextSize(22f);
        textLabelPaint.setFakeBoldText(true);

        textValuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textValuePaint.setColor(Color.parseColor("#182312"));
        textValuePaint.setTextSize(30f);
        textValuePaint.setFakeBoldText(true);

        bannerBgPaint = new Paint();
        bannerBgPaint.setColor(Color.parseColor("#182312"));
        bannerBgPaint.setStyle(Paint.Style.FILL);

        bannerTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bannerTextPaint.setColor(Color.parseColor("#9EAE8E"));
        bannerTextPaint.setTextSize(32f);
        bannerTextPaint.setFakeBoldText(true);
        bannerTextPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setEngine(BrickGameEngine engine) {
        this.engine = engine;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) return;

        // Draw LCD background
        canvas.drawRect(0, 0, width, height, bgPaint);

        float gridAreaWidth = width * 0.62f;
        float sideAreaLeft = gridAreaWidth + 10f;

        float padLeft = 12f;
        float padTop = 14f;
        float padBottom = 14f;

        float availGridWidth = gridAreaWidth - padLeft - 6f;
        float availGridHeight = height - padTop - padBottom;

        if (availGridWidth <= 0 || availGridHeight <= 0) return;

        // Compute square cell size based on 10 cols x 20 rows
        float cellSize = Math.min(availGridWidth / BrickGameEngine.COLS, availGridHeight / BrickGameEngine.ROWS);
        if (cellSize <= 0) return;

        float gridActualHeight = cellSize * BrickGameEngine.ROWS;
        float gridTop = padTop + (availGridHeight - gridActualHeight) / 2f;
        float gridLeft = padLeft;

        // Draw Game Board Grid Cells
        drawBoardGrid(canvas, gridLeft, gridTop, cellSize);

        // Draw Divider Line
        canvas.drawLine(gridAreaWidth, padTop, gridAreaWidth, height - padBottom, dividerPaint);

        // Draw Side Info Panel
        drawSidePanel(canvas, sideAreaLeft, padTop, width - sideAreaLeft - 10f, height - padTop - padBottom);

        // Draw Game Over or Pause Overlay if needed
        if (engine != null) {
            if (engine.isPaused()) {
                drawBanner(canvas, width / 2f, height / 2f, "PAUSE");
            } else if (engine.isGameOver() && !engine.isRunning()) {
                drawBanner(canvas, width / 2f, height / 2f, "GAME OVER");
            }
        }
    }

    private void drawBoardGrid(Canvas canvas, float startX, float startY, float cellSize) {
        byte[][] board = (engine != null) ? engine.getBoard() : null;

        // Render base board
        for (int r = 0; r < BrickGameEngine.ROWS; r++) {
            for (int c = 0; c < BrickGameEngine.COLS; c++) {
                float x = startX + c * cellSize;
                float y = startY + r * cellSize;
                boolean isFilled = (board != null && board[r][c] == 1);
                drawBrickPixel(canvas, x, y, cellSize, isFilled);
            }
        }

        // Render current active falling tetromino
        if (engine != null && engine.isRunning() && !engine.isGameOver()) {
            int type = engine.getCurType();
            int rot = engine.getCurRotation();
            int curX = engine.getCurX();
            int curY = engine.getCurY();
            int[][] matrix = Tetromino.getMatrix(type, rot);

            for (int r = 0; r < 4; r++) {
                for (int c = 0; c < 4; c++) {
                    if (matrix[r][c] == 1) {
                        int boardX = curX + c;
                        int boardY = curY + r;
                        if (boardY >= 0 && boardY < BrickGameEngine.ROWS && boardX >= 0 && boardX < BrickGameEngine.COLS) {
                            float x = startX + boardX * cellSize;
                            float y = startY + boardY * cellSize;
                            drawBrickPixel(canvas, x, y, cellSize, true);
                        }
                    }
                }
            }
        }
    }

    private void drawBrickPixel(Canvas canvas, float x, float y, float size, boolean active) {
        float gap = size * 0.08f;
        float pX = x + gap;
        float pY = y + gap;
        float pS = size - (gap * 2);

        cellRect.set(pX, pY, pX + pS, pY + pS);

        if (active) {
            canvas.drawRect(cellRect, activePixelPaint);

            float innerMargin = pS * 0.22f;
            innerRect.set(pX + innerMargin, pY + innerMargin, pX + pS - innerMargin, pY + pS - innerMargin);
            canvas.drawRect(innerRect, innerPixelPaint);

            float centerMargin = pS * 0.36f;
            cellRect.set(pX + centerMargin, pY + centerMargin, pX + pS - centerMargin, pY + pS - centerMargin);
            canvas.drawRect(cellRect, activePixelPaint);
        } else {
            canvas.drawRect(cellRect, ghostPixelPaint);
        }
    }

    private void drawSidePanel(Canvas canvas, float left, float top, float width, float height) {
        float curY = top + 22f;

        // HI-SCORE
        canvas.drawText("HI-SCORE", left, curY, textLabelPaint);
        curY += 28f;
        int hiScore = (engine != null) ? engine.getHiScore() : 0;
        canvas.drawText(String.format("%06d", hiScore), left, curY, textValuePaint);
        curY += 40f;

        // SCORE
        canvas.drawText("SCORE", left, curY, textLabelPaint);
        curY += 28f;
        int score = (engine != null) ? engine.getScore() : 0;
        canvas.drawText(String.format("%06d", score), left, curY, textValuePaint);
        curY += 40f;

        // SPEED
        int speed = (engine != null) ? engine.getSpeed() : 1;
        canvas.drawText("SPEED", left, curY, textLabelPaint);
        curY += 28f;
        canvas.drawText(String.valueOf(speed), left, curY, textValuePaint);
        curY += 38f;

        // LINES
        int lines = (engine != null) ? engine.getLines() : 0;
        canvas.drawText("LINES", left, curY, textLabelPaint);
        curY += 28f;
        canvas.drawText(String.format("%03d", lines), left, curY, textValuePaint);
        curY += 42f;

        // NEXT PREVIEW
        canvas.drawText("NEXT", left, curY, textLabelPaint);
        curY += 12f;

        float previewBoxSize = Math.min(width * 0.9f, 95f);
        float previewCellSize = previewBoxSize / 4f;
        int nextType = (engine != null) ? engine.getNextType() : 0;
        int[][] nextMatrix = Tetromino.getMatrix(nextType, 0);

        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                float cellX = left + c * previewCellSize;
                float cellY = curY + r * previewCellSize;
                boolean active = (nextMatrix[r][c] == 1);
                drawBrickPixel(canvas, cellX, cellY, previewCellSize, active);
            }
        }
        curY += (previewBoxSize + 25f);

        // Retro Mascot Pixel Art Character
        drawMascot(canvas, left + 10f, curY, 6f);
    }

    private void drawMascot(Canvas canvas, float startX, float startY, float pixelSize) {
        // Use bitwise AND 1 to strictly evaluate to 0 or 1, avoiding negative array indices on timestamp wrap
        int frame = (int) ((System.currentTimeMillis() / 450L) & 1L);
        byte[][] sprite = MASCOT[frame];

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                if (sprite[r][c] == 1) {
                    float x = startX + c * pixelSize;
                    float y = startY + r * pixelSize;
                    canvas.drawRect(x, y, x + pixelSize, y + pixelSize, activePixelPaint);
                }
            }
        }
    }

    private void drawBanner(Canvas canvas, float centerX, float centerY, String text) {
        float bannerWidth = getWidth() * 0.72f;
        float bannerHeight = 70f;
        float left = centerX - (bannerWidth / 2f);
        float top = centerY - (bannerHeight / 2f);

        RectF bannerRect = new RectF(left, top, left + bannerWidth, top + bannerHeight);
        canvas.drawRect(bannerRect, bannerBgPaint);
        canvas.drawText(text, centerX, centerY + 11f, bannerTextPaint);
    }
}