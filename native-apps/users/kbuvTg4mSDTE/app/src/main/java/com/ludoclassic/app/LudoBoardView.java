package com.ludoclassic.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class LudoBoardView extends View {

    public static class Cell {
        public final int col;
        public final int row;

        public Cell(int col, int row) {
            this.col = col;
            this.row = row;
        }
    }

    public static final Cell[] OUTER_LOOP = new Cell[] {
        new Cell(1, 6), new Cell(2, 6), new Cell(3, 6), new Cell(4, 6), new Cell(5, 6),
        new Cell(6, 5), new Cell(6, 4), new Cell(6, 3), new Cell(6, 2), new Cell(6, 1), new Cell(6, 0),
        new Cell(7, 0),
        new Cell(8, 0), new Cell(8, 1), new Cell(8, 2), new Cell(8, 3), new Cell(8, 4), new Cell(8, 5),
        new Cell(9, 6), new Cell(10, 6), new Cell(11, 6), new Cell(12, 6), new Cell(13, 6), new Cell(14, 6),
        new Cell(14, 7),
        new Cell(14, 8), new Cell(13, 8), new Cell(12, 8), new Cell(11, 8), new Cell(10, 8), new Cell(9, 8),
        new Cell(8, 9), new Cell(8, 10), new Cell(8, 11), new Cell(8, 12), new Cell(8, 13), new Cell(8, 14),
        new Cell(7, 14),
        new Cell(6, 14), new Cell(6, 13), new Cell(6, 12), new Cell(6, 11), new Cell(6, 10), new Cell(6, 9),
        new Cell(5, 8), new Cell(4, 8), new Cell(3, 8), new Cell(2, 8), new Cell(1, 8), new Cell(0, 8),
        new Cell(0, 7),
        new Cell(0, 6)
    };

    public static Cell[][] playerPaths = new Cell[4][58];

    static {
        for (int p = 0; p < 4; p++) {
            int startIndex = 0;
            if (p == 0) startIndex = 0;       // Red starts at (1, 6)
            else if (p == 1) startIndex = 13; // Green starts at (8, 1)
            else if (p == 2) startIndex = 26; // Yellow starts at (13, 8)
            else if (p == 3) startIndex = 39; // Blue starts at (6, 13)

            for (int i = 0; i < 51; i++) {
                int idx = (startIndex + i) % 52;
                playerPaths[p][i + 1] = OUTER_LOOP[idx];
            }

            if (p == 0) { 
                playerPaths[0][52] = new Cell(1, 7);
                playerPaths[0][53] = new Cell(2, 7);
                playerPaths[0][54] = new Cell(3, 7);
                playerPaths[0][55] = new Cell(4, 7);
                playerPaths[0][56] = new Cell(5, 7);
                playerPaths[0][57] = new Cell(6, 7);
            } else if (p == 1) { 
                playerPaths[1][52] = new Cell(7, 1);
                playerPaths[1][53] = new Cell(7, 2);
                playerPaths[1][54] = new Cell(7, 3);
                playerPaths[1][55] = new Cell(7, 4);
                playerPaths[1][56] = new Cell(7, 5);
                playerPaths[1][57] = new Cell(7, 6);
            } else if (p == 2) { 
                playerPaths[2][52] = new Cell(13, 7);
                playerPaths[2][53] = new Cell(12, 7);
                playerPaths[2][54] = new Cell(11, 7);
                playerPaths[2][55] = new Cell(10, 7);
                playerPaths[2][56] = new Cell(9, 7);
                playerPaths[2][57] = new Cell(8, 7);
            } else if (p == 3) { 
                playerPaths[3][52] = new Cell(7, 13);
                playerPaths[3][53] = new Cell(7, 12);
                playerPaths[3][54] = new Cell(7, 11);
                playerPaths[3][55] = new Cell(7, 10);
                playerPaths[3][56] = new Cell(7, 9);
                playerPaths[3][57] = new Cell(7, 8);
            }
        }
    }

    public interface OnTokenSelectedListener {
        void onTokenSelected(int playerIndex, int tokenIndex);
    }

    private static final int COLOR_RED = 0xFFE53935;
    private static final int COLOR_GREEN = 0xFF43A047;
    private static final int COLOR_YELLOW = 0xFFFFB300;
    private static final int COLOR_BLUE = 0xFF1E88E5;

    private static final int COLOR_GRID = 0xFF37474F;
    private static final int COLOR_PATH = 0xFFFFFFFF;
    private static final int COLOR_SAFE = 0xFFECEFF1;

    private Paint fillPaint;
    private Paint strokePaint;
    private Paint highlightPaint;

    private int[][] tokenPositions = new int[4][4]; 
    private boolean[] playerActive = new boolean[4];
    private boolean[] hasWon = new boolean[4];
    private int currentTurn = 0;
    private int lastDiceValue = 0;
    private boolean diceRolled = false;

    // Jumping offset array to achieve natural bounce transitions [playerIndex][tokenIndex]
    private float[][] tokenJumpOffsetsY = new float[4][4];

    private OnTokenSelectedListener listener;

    public LudoBoardView(Context context) {
        super(context);
        init();
    }

    public LudoBoardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(2f);

        highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        highlightPaint.setStyle(Paint.Style.STROKE);
        highlightPaint.setColor(0xFFFFFFFF);
        highlightPaint.setStrokeWidth(4f);
    }

    public void setOnTokenSelectedListener(OnTokenSelectedListener listener) {
        this.listener = listener;
    }

    public void updateState(int[][] tokens, boolean[] active, boolean[] won, int turn, int dice, boolean rolled) {
        for (int i = 0; i < 4; i++) {
            System.arraycopy(tokens[i], 0, this.tokenPositions[i], 0, 4);
            this.playerActive[i] = active[i];
            this.hasWon[i] = won[i];
        }
        this.currentTurn = turn;
        this.lastDiceValue = dice;
        this.diceRolled = rolled;
        invalidate();
    }

    public void setTokenHopOffset(int player, int token, float offset) {
        this.tokenJumpOffsetsY[player][token] = offset;
        invalidate();
    }

    public void animateTokenHop(final int player, final int token, final Runnable onFinished) {
        final int frames = 8;
        final long frameDelay = 15; 
        final float maxHopHeight = 35f; 

        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i <= frames; i++) {
                    double progress = (double) i / frames;
                    final float currentHopY = (float) (Math.sin(progress * Math.PI) * maxHopHeight);
                    post(new Runnable() {
                        @Override
                        public void run() {
                            setTokenHopOffset(player, token, currentHopY);
                        }
                    });
                    try {
                        Thread.sleep(frameDelay);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                post(new Runnable() {
                    @Override
                    public void run() {
                        setTokenHopOffset(player, token, 0f);
                        if (onFinished != null) {
                            onFinished.run();
                        }
                    }
                });
            }
        }).start();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int size = Math.min(width, height);
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int size = Math.min(getWidth(), getHeight());
        float cellSize = size / 15f;

        float offsetX = (getWidth() - size) / 2f;
        float offsetY = (getHeight() - size) / 2f;

        canvas.save();
        canvas.translate(offsetX, offsetY);

        fillPaint.setColor(COLOR_PATH);
        canvas.drawRect(0, 0, size, size, fillPaint);

        for (int col = 0; col < 15; col++) {
            for (int row = 0; row < 15; row++) {
                if (col < 6 && row < 6) continue;
                if (col > 8 && row < 6) continue;
                if (col > 8 && row > 8) continue;
                if (col < 6 && row > 8) continue;
                if (col >= 6 && col <= 8 && row >= 6 && row <= 8) continue;

                int cellColor = COLOR_PATH;
                boolean isSafe = isSafeCell(col, row);

                if (isSafe) {
                    cellColor = COLOR_SAFE;
                }

                if (row == 7 && col >= 1 && col <= 5) { cellColor = COLOR_RED; }
                else if (col == 7 && row >= 1 && row <= 5) { cellColor = COLOR_GREEN; }
                else if (row == 7 && col >= 9 && col <= 13) { cellColor = COLOR_YELLOW; }
                else if (col == 7 && row >= 9 && row <= 13) { cellColor = COLOR_BLUE; }

                if (col == 1 && row == 6) { cellColor = COLOR_RED; }
                else if (col == 8 && row == 1) { cellColor = COLOR_GREEN; }
                else if (col == 13 && row == 8) { cellColor = COLOR_YELLOW; }
                else if (col == 6 && row == 13) { cellColor = COLOR_BLUE; }

                fillPaint.setColor(cellColor);
                canvas.drawRect(col * cellSize, row * cellSize, (col + 1) * cellSize, (row + 1) * cellSize, fillPaint);

                strokePaint.setColor(COLOR_GRID);
                strokePaint.setStrokeWidth(1.5f);
                canvas.drawRect(col * cellSize, row * cellSize, (col + 1) * cellSize, (row + 1) * cellSize, strokePaint);

                if (isSafe && !isStartingCell(col, row)) {
                    drawStar(canvas, (col + 0.5f) * cellSize, (row + 0.5f) * cellSize, cellSize * 0.35f, COLOR_GRID);
                }
            }
        }

        drawYard(canvas, 0, 0, cellSize, COLOR_RED);
        drawYard(canvas, 9, 0, cellSize, COLOR_GREEN);
        drawYard(canvas, 9, 9, cellSize, COLOR_YELLOW);
        drawYard(canvas, 0, 9, cellSize, COLOR_BLUE);

        drawHomeCenter(canvas, cellSize);

        Map<String, ArrayList<Integer>> cellGroups = new HashMap<String, ArrayList<Integer>>();
        for (int p = 0; p < 4; p++) {
            if (!playerActive[p]) continue;
            for (int t = 0; t < 4; t++) {
                int pos = tokenPositions[p][t];
                if (pos > 0) { 
                    Cell cell = playerPaths[p][pos];
                    String key = cell.col + "_" + cell.row;
                    if (!cellGroups.containsKey(key)) {
                        cellGroups.put(key, new ArrayList<Integer>());
                    }
                    cellGroups.get(key).add(p * 4 + t);
                }
            }
        }

        for (int p = 0; p < 4; p++) {
            if (!playerActive[p]) continue;
            for (int t = 0; t < 4; t++) {
                int pos = tokenPositions[p][t];

                float cx, cy;
                float radius = cellSize * 0.28f;

                if (pos == 0) {
                    float[] coords = getYardSlotCoords(p, t);
                    cx = coords[0] * cellSize;
                    cy = coords[1] * cellSize;
                } else {
                    Cell cell = playerPaths[p][pos];
                    cx = (cell.col + 0.5f) * cellSize;
                    cy = (cell.row + 0.5f) * cellSize;

                    String key = cell.col + "_" + cell.row;
                    ArrayList<Integer> group = cellGroups.get(key);
                    if (group != null && group.size() > 1) {
                        int indexInGroup = group.indexOf(p * 4 + t);
                        int N = group.size();
                        float offset = cellSize * 0.2f;

                        radius = cellSize * 0.16f; 

                        if (N == 2) {
                            if (indexInGroup == 0) { cx -= offset; }
                            else { cx += offset; }
                        } else if (N == 3) {
                            if (indexInGroup == 0) { cx -= offset; cy += offset / 2; }
                            else if (indexInGroup == 1) { cx += offset; cy += offset / 2; }
                            else { cy -= offset; }
                        } else if (N >= 4) {
                            if (indexInGroup == 0) { cx -= offset; cy -= offset; }
                            else if (indexInGroup == 1) { cx += offset; cy -= offset; }
                            else if (indexInGroup == 2) { cx -= offset; cy += offset; }
                            else { cx += offset; cy += offset; }
                        }
                    }
                }

                // Apply jumping hop offset to token vertical coordinate
                cy -= tokenJumpOffsetsY[p][t];

                int tokenColor = getPlayerColor(p);
                fillPaint.setColor(tokenColor);
                canvas.drawCircle(cx, cy, radius, fillPaint);

                strokePaint.setColor(0xFFFFFFFF);
                strokePaint.setStrokeWidth(2.5f);
                canvas.drawCircle(cx, cy, radius, strokePaint);

                fillPaint.setColor(0xFFFFFFFF);
                canvas.drawCircle(cx, cy, radius * 0.4f, fillPaint);

                if (p == currentTurn && diceRolled && isMoveValid(p, t)) {
                    highlightPaint.setColor(0xFFFFFF00); 
                    canvas.drawCircle(cx, cy, radius + 4f, highlightPaint);
                }
            }
        }

        canvas.restore();
    }

    private boolean isMoveValid(int p, int t) {
        int pos = tokenPositions[p][t];
        if (pos == 0) {
            return lastDiceValue == 6;
        } else {
            return pos + lastDiceValue <= 57;
        }
    }

    private void drawYard(Canvas canvas, int startCol, int startRow, float cellSize, int color) {
        float left = startCol * cellSize;
        float top = startRow * cellSize;
        float right = (startCol + 6) * cellSize;
        float bottom = (startRow + 6) * cellSize;

        fillPaint.setColor(color);
        canvas.drawRect(left, top, right, bottom, fillPaint);

        strokePaint.setColor(COLOR_GRID);
        strokePaint.setStrokeWidth(3f);
        canvas.drawRect(left, top, right, bottom, strokePaint);

        fillPaint.setColor(0xFFFFFFFF);
        canvas.drawRect(left + cellSize, top + cellSize, right - cellSize, bottom - cellSize, fillPaint);
        canvas.drawRect(left + cellSize, top + cellSize, right - cellSize, bottom - cellSize, strokePaint);

        for (int i = 0; i < 4; i++) {
            float[] coords = getYardSlotCoords(startCol == 0 ? 0 : 1, i);
            float cx = (startCol + (coords[0] % 6)) * cellSize;
            float cy = (startRow + (coords[1] % 6)) * cellSize;

            fillPaint.setColor(0xFFFFFFFF);
            canvas.drawCircle(cx, cy, cellSize * 0.42f, fillPaint);

            strokePaint.setColor(color);
            strokePaint.setStrokeWidth(4f);
            canvas.drawCircle(cx, cy, cellSize * 0.42f, strokePaint);
        }
    }

    private float[] getYardSlotCoords(int player, int tokenIndex) {
        if (player == 0) { 
            if (tokenIndex == 0) return new float[]{1.5f, 1.5f};
            if (tokenIndex == 1) return new float[]{4.5f, 1.5f};
            if (tokenIndex == 2) return new float[]{1.5f, 4.5f};
            return new float[]{4.5f, 4.5f};
        } else if (player == 1) { 
            if (tokenIndex == 0) return new float[]{10.5f, 1.5f};
            if (tokenIndex == 1) return new float[]{13.5f, 1.5f};
            if (tokenIndex == 2) return new float[]{10.5f, 4.5f};
            return new float[]{13.5f, 4.5f};
        } else if (player == 2) { 
            if (tokenIndex == 0) return new float[]{10.5f, 10.5f};
            if (tokenIndex == 1) return new float[]{13.5f, 10.5f};
            if (tokenIndex == 2) return new float[]{10.5f, 13.5f};
            return new float[]{13.5f, 13.5f};
        } else { 
            if (tokenIndex == 0) return new float[]{1.5f, 10.5f};
            if (tokenIndex == 1) return new float[]{4.5f, 10.5f};
            if (tokenIndex == 2) return new float[]{1.5f, 13.5f};
            return new float[]{4.5f, 13.5f};
        }
    }

    private void drawHomeCenter(Canvas canvas, float cellSize) {
        float start = 6 * cellSize;
        float center = 7.5f * cellSize;
        float end = 9 * cellSize;

        fillPaint.setColor(COLOR_RED);
        Path path = new Path();
        path.moveTo(start, start);
        path.lineTo(center, center);
        path.lineTo(start, end);
        path.close();
        canvas.drawPath(path, fillPaint);

        fillPaint.setColor(COLOR_GREEN);
        path.reset();
        path.moveTo(start, start);
        path.lineTo(center, center);
        path.lineTo(end, start);
        path.close();
        canvas.drawPath(path, fillPaint);

        fillPaint.setColor(COLOR_YELLOW);
        path.reset();
        path.moveTo(end, start);
        path.lineTo(center, center);
        path.lineTo(end, end);
        path.close();
        canvas.drawPath(path, fillPaint);

        fillPaint.setColor(COLOR_BLUE);
        path.reset();
        path.moveTo(start, end);
        path.lineTo(center, center);
        path.lineTo(end, end);
        path.close();
        canvas.drawPath(path, fillPaint);

        strokePaint.setColor(COLOR_GRID);
        strokePaint.setStrokeWidth(3f);
        canvas.drawRect(start, start, end, end, strokePaint);

        canvas.drawLine(start, start, end, end, strokePaint);
        canvas.drawLine(start, end, end, start, strokePaint);
    }

    private void drawStar(Canvas canvas, float cx, float cy, float radius, int color) {
        Paint starPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        starPaint.setColor(color);
        starPaint.setStyle(Paint.Style.FILL);

        Path path = new Path();
        path.moveTo(cx, cy - radius);
        path.lineTo(cx + radius * 0.22f, cy - radius * 0.22f);
        path.lineTo(cx + radius, cy);
        path.lineTo(cx + radius * 0.22f, cy + radius * 0.22f);
        path.lineTo(cx, cy + radius);
        path.lineTo(cx - radius * 0.22f, cy + radius * 0.22f);
        path.lineTo(cx - radius, cy);
        path.lineTo(cx - radius * 0.22f, cy - radius * 0.22f);
        path.close();
        canvas.drawPath(path, starPaint);
    }

    private boolean isSafeCell(int col, int row) {
        if (col == 1 && row == 6) return true;
        if (col == 8 && row == 1) return true;
        if (col == 13 && row == 8) return true;
        if (col == 6 && row == 13) return true;

        if (col == 8 && row == 6) return true;
        if (col == 6 && row == 8) return true;
        if (col == 2 && row == 6) return true;
        if (col == 8 && row == 2) return true;
        if (col == 12 && row == 8) return true;
        if (col == 6 && row == 12) return true;

        return false;
    }

    private boolean isStartingCell(int col, int row) {
        if (col == 1 && row == 6) return true;
        if (col == 8 && row == 1) return true;
        if (col == 13 && row == 8) return true;
        if (col == 6 && row == 13) return true;
        return false;
    }

    private int getPlayerColor(int player) {
        switch (player) {
            case 0: return COLOR_RED;
            case 1: return COLOR_GREEN;
            case 2: return COLOR_YELLOW;
            case 3: return COLOR_BLUE;
            default: return 0xFF000000;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float x = event.getX();
            float y = event.getY();

            int size = Math.min(getWidth(), getHeight());
            float cellSize = size / 15f;
            float offsetX = (getWidth() - size) / 2f;
            float offsetY = (getHeight() - size) / 2f;

            float localX = x - offsetX;
            float localY = y - offsetY;

            if (localX >= 0 && localX < size && localY >= 0 && localY < size) {
                int col = (int) (localX / cellSize);
                int row = (int) (localY / cellSize);
                handleTouch(col, row);
                return true;
            }
        }
        return super.onTouchEvent(event);
    }

    private void handleTouch(int col, int row) {
        if (listener == null || !diceRolled) return;

        int p = currentTurn;
        for (int t = 0; t < 4; t++) {
            int pos = tokenPositions[p][t];
            if (pos == 0) {
                float[] coords = getYardSlotCoords(p, t);
                int slotCol = (int) coords[0];
                int slotRow = (int) coords[1];
                if (col == slotCol && row == slotRow) {
                    if (isMoveValid(p, t)) {
                        listener.onTokenSelected(p, t);
                        return;
                    }
                }
            } else {
                Cell cell = playerPaths[p][pos];
                if (cell.col == col && cell.row == row) {
                    if (isMoveValid(p, t)) {
                        listener.onTokenSelected(p, t);
                        return;
                    }
                }
            }
        }
    }
}