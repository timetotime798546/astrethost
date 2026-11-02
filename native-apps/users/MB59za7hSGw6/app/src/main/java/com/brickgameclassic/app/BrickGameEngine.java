package com.brickgameclassic.app;

import android.os.Handler;
import android.os.Looper;

/**
 * Core 3D Brick Game Engine.
 * Manages game state, piece falling physics, collision detection,
 * ghost destination computation, line clears, and score recalculation.
 */
public class BrickGameEngine {
    public static final int COLS = 10;
    public static final int ROWS = 20;

    public interface GameListener {
        void onGameUpdated();
        void onLinesCleared(int lineCount);
        void onGameOver();
        void onPiecePlaced();
    }

    // Grid holds color value (or 0 if empty)
    private final int[][] board = new int[ROWS][COLS];
    private int curType = 0;
    private int curRotation = 0;
    private int curX = 3;
    private int curY = 0;

    private int nextType = 0;

    private int score = 0;
    private int hiScore = 0;
    private int level = 1;
    private int speed = 1;
    private int lines = 0;

    private boolean isRunning = false;
    private boolean isPaused = false;
    private boolean isGameOver = false;

    // Animation state during game over wipe
    private int wipeRow = -1;
    private boolean wipeFilling = true;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final GameListener listener;
    private final SoundManager soundManager;

    private final Runnable gameLoopRunnable = new Runnable() {
        @Override
        public void run() {
            if (isRunning && !isPaused && !isGameOver) {
                step();
                scheduleNextStep();
            } else if (isGameOver && wipeRow >= 0) {
                stepWipeAnimation();
                handler.postDelayed(this, 35);
            }
        }
    };

    public BrickGameEngine(GameListener listener, SoundManager soundManager) {
        this.listener = listener;
        this.soundManager = soundManager;
        this.nextType = Tetromino.getRandomType();
    }

    public void startNewGame() {
        clearBoard();
        score = 0;
        lines = 0;
        level = 1;
        speed = 1;
        isGameOver = false;
        isPaused = false;
        wipeRow = -1;

        spawnNewPiece();
        isRunning = true;
        soundManager.playStart();

        handler.removeCallbacks(gameLoopRunnable);
        scheduleNextStep();
        notifyUpdate();
    }

    public void togglePause() {
        if (!isRunning || isGameOver) return;
        isPaused = !isPaused;
        if (!isPaused) {
            scheduleNextStep();
        } else {
            handler.removeCallbacks(gameLoopRunnable);
        }
        notifyUpdate();
    }

    public void moveLeft() {
        if (!canControl()) return;
        if (!checkCollision(curType, curRotation, curX - 1, curY)) {
            curX--;
            soundManager.playMove();
            notifyUpdate();
        }
    }

    public void moveRight() {
        if (!canControl()) return;
        if (!checkCollision(curType, curRotation, curX + 1, curY)) {
            curX++;
            soundManager.playMove();
            notifyUpdate();
        }
    }

    public void rotate() {
        if (!canControl()) return;
        int nextRot = (curRotation + 1) % 4;
        if (!checkCollision(curType, nextRot, curX, curY)) {
            curRotation = nextRot;
            soundManager.playRotate();
            notifyUpdate();
        } else if (!checkCollision(curType, nextRot, curX - 1, curY)) {
            curX -= 1;
            curRotation = nextRot;
            soundManager.playRotate();
            notifyUpdate();
        } else if (!checkCollision(curType, nextRot, curX + 1, curY)) {
            curX += 1;
            curRotation = nextRot;
            soundManager.playRotate();
            notifyUpdate();
        } else if (!checkCollision(curType, nextRot, curX - 2, curY)) {
            curX -= 2;
            curRotation = nextRot;
            soundManager.playRotate();
            notifyUpdate();
        } else if (!checkCollision(curType, nextRot, curX + 2, curY)) {
            curX += 2;
            curRotation = nextRot;
            soundManager.playRotate();
            notifyUpdate();
        }
    }

    public void softDrop() {
        if (!canControl()) return;
        if (!checkCollision(curType, curRotation, curX, curY + 1)) {
            curY++;
            score += 1;
            if (score > hiScore) hiScore = score;
            soundManager.playDrop();
            notifyUpdate();
        } else {
            lockPiece();
        }
    }

    public void hardDrop() {
        if (!canControl()) return;
        int dropDistance = 0;
        while (!checkCollision(curType, curRotation, curX, curY + 1)) {
            curY++;
            dropDistance++;
        }
        if (dropDistance > 0) {
            score += dropDistance * 2;
            if (score > hiScore) hiScore = score;
            soundManager.playDrop();
        }
        lockPiece();
    }

    public int getGhostY() {
        int testY = curY;
        while (!checkCollision(curType, curRotation, curX, testY + 1)) {
            testY++;
        }
        return testY;
    }

    private void step() {
        if (!checkCollision(curType, curRotation, curX, curY + 1)) {
            curY++;
            notifyUpdate();
        } else {
            lockPiece();
        }
    }

    private void lockPiece() {
        int[][] matrix = Tetromino.getMatrix(curType, curRotation);
        int blockColor = Tetromino.getColor(curType);

        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                if (matrix[r][c] == 1) {
                    int bR = curY + r;
                    int bC = curX + c;
                    if (bR >= 0 && bR < ROWS && bC >= 0 && bC < COLS) {
                        board[bR][bC] = blockColor;
                    }
                }
            }
        }

        soundManager.playDrop();
        if (listener != null) listener.onPiecePlaced();

        checkLineClears();
        spawnNewPiece();

        if (checkCollision(curType, curRotation, curX, curY)) {
            triggerGameOver();
        } else {
            notifyUpdate();
        }
    }

    private void checkLineClears() {
        int clearedCount = 0;
        for (int r = ROWS - 1; r >= 0; r--) {
            boolean full = true;
            for (int c = 0; c < COLS; c++) {
                if (board[r][c] == 0) {
                    full = false;
                    break;
                }
            }
            if (full) {
                clearedCount++;
                for (int y = r; y > 0; y--) {
                    System.arraycopy(board[y - 1], 0, board[y], 0, COLS);
                }
                for (int c = 0; c < COLS; c++) {
                    board[0][c] = 0;
                }
                r++;
            }
        }

        if (clearedCount > 0) {
            lines += clearedCount;
            int basePoints;
            switch (clearedCount) {
                case 1: basePoints = 100; break;
                case 2: basePoints = 300; break;
                case 3: basePoints = 700; break;
                default: basePoints = 1500; break;
            }
            score += basePoints * level;
            if (score > hiScore) hiScore = score;

            level = Math.min(10, 1 + (lines / 10));
            speed = level;

            soundManager.playClear();
            if (listener != null) listener.onLinesCleared(clearedCount);
        }
    }

    private void spawnNewPiece() {
        curType = nextType;
        nextType = Tetromino.getRandomType();
        curRotation = 0;
        curX = 3;
        curY = 0;
    }

    private boolean checkCollision(int type, int rot, int testX, int testY) {
        int[][] matrix = Tetromino.getMatrix(type, rot);
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                if (matrix[r][c] == 1) {
                    int boardX = testX + c;
                    int boardY = testY + r;

                    if (boardX < 0 || boardX >= COLS || boardY >= ROWS) {
                        return true;
                    }
                    if (boardY >= 0 && board[boardY][boardX] != 0) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void triggerGameOver() {
        isGameOver = true;
        isRunning = false;
        wipeRow = ROWS - 1;
        wipeFilling = true;
        soundManager.playGameOver();
        if (listener != null) listener.onGameOver();
        handler.post(gameLoopRunnable);
    }

    private void stepWipeAnimation() {
        if (wipeFilling) {
            if (wipeRow >= 0) {
                for (int c = 0; c < COLS; c++) {
                    board[wipeRow][c] = 0xFF546E7A; // Muted metallic gray
                }
                wipeRow--;
            } else {
                wipeFilling = false;
                wipeRow = 0;
            }
        } else {
            if (wipeRow < ROWS) {
                for (int c = 0; c < COLS; c++) {
                    board[wipeRow][c] = 0;
                }
                wipeRow++;
            } else {
                wipeRow = -1;
            }
        }
        notifyUpdate();
    }

    private void scheduleNextStep() {
        handler.removeCallbacks(gameLoopRunnable);
        long delay = Math.max(90, 800 - (speed * 70));
        handler.postDelayed(gameLoopRunnable, delay);
    }

    private boolean canControl() {
        return isRunning && !isPaused && !isGameOver;
    }

    private void clearBoard() {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                board[r][c] = 0;
            }
        }
    }

    private void notifyUpdate() {
        if (listener != null) {
            listener.onGameUpdated();
        }
    }

    public int[][] getBoard() { return board; }
    public int getCurType() { return curType; }
    public int getCurRotation() { return curRotation; }
    public int getCurX() { return curX; }
    public int getCurY() { return curY; }
    public int getNextType() { return nextType; }
    public int getScore() { return score; }
    public int getHiScore() { return hiScore; }
    public void setHiScore(int hiScore) { this.hiScore = hiScore; }
    public int getLevel() { return level; }
    public int getSpeed() { return speed; }
    public int getLines() { return lines; }
    public boolean isRunning() { return isRunning; }
    public boolean isPaused() { return isPaused; }
    public boolean isGameOver() { return isGameOver; }

    public void pauseEngine() {
        handler.removeCallbacks(gameLoopRunnable);
    }

    public void resumeEngine() {
        if (isRunning && !isPaused && !isGameOver) {
            scheduleNextStep();
        }
    }
}