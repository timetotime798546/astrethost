package com.retrosnake.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Random;

public class SnakeView extends View {

    public enum Direction {
        UP, DOWN, LEFT, RIGHT
    }

    public enum GameState {
        READY, RUNNING, PAUSED, GAME_OVER
    }

    public static class Coordinate {
        public int x;
        public int y;

        public Coordinate(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public boolean equals(Coordinate other) {
            return other != null && this.x == other.x && this.y == other.y;
        }
    }

    public interface GameListener {
        void onScoreUpdated(int score);
        void onGameOver(int finalScore);
        void onFoodEaten(boolean isBonus);
    }

    private static final int GRID_WIDTH = 20;
    private static final int GRID_HEIGHT = 26;

    private int cellWidth = 0;
    private int cellHeight = 0;
    private int offsetX = 0;
    private int offsetY = 0;

    private final ArrayList<Coordinate> snake = new ArrayList<Coordinate>();
    private Direction currentDirection = Direction.RIGHT;
    private Direction nextDirection = Direction.RIGHT;

    private Coordinate regularFood = null;
    private Coordinate bonusFood = null;
    private int bonusFoodCounter = 0;
    private static final int BONUS_DURATION = 35; // game ticks

    private GameState gameState = GameState.READY;
    private int currentScore = 0;
    private final Random random = new Random();

    // Paints
    private Paint paintGrid;
    private Paint paintSnakeHead;
    private Paint paintSnakeBody;
    private Paint paintFood;
    private Paint paintBonusFood;
    private Paint paintEye;

    private final RectF rectBuffer = new RectF();
    private GameListener gameListener;
    private GestureDetector gestureDetector;

    public SnakeView(Context context) {
        super(context);
        init(context);
    }

    public SnakeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SnakeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        paintGrid = new Paint();
        paintGrid.setColor(Color.parseColor("#1A2333"));
        paintGrid.setStyle(Paint.Style.STROKE);
        paintGrid.setStrokeWidth(1.0f);

        paintSnakeHead = new Paint();
        paintSnakeHead.setColor(Color.parseColor("#00E676"));
        paintSnakeHead.setStyle(Paint.Style.FILL);
        paintSnakeHead.setAntiAlias(true);

        paintSnakeBody = new Paint();
        paintSnakeBody.setColor(Color.parseColor("#00B0FF"));
        paintSnakeBody.setStyle(Paint.Style.FILL);
        paintSnakeBody.setAntiAlias(true);

        paintFood = new Paint();
        paintFood.setColor(Color.parseColor("#FF1744"));
        paintFood.setStyle(Paint.Style.FILL);
        paintFood.setAntiAlias(true);

        paintBonusFood = new Paint();
        paintBonusFood.setColor(Color.parseColor("#FFEA00"));
        paintBonusFood.setStyle(Paint.Style.FILL);
        paintBonusFood.setAntiAlias(true);

        paintEye = new Paint();
        paintEye.setColor(Color.parseColor("#121820"));
        paintEye.setStyle(Paint.Style.FILL);
        paintEye.setAntiAlias(true);

        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || e2 == null) {
                    return false;
                }
                float diffX = e2.getX() - e1.getX();
                float diffY = e2.getY() - e1.getY();

                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > 50 && Math.abs(velocityX) > 100) {
                        if (diffX > 0) {
                            setDirection(Direction.RIGHT);
                        } else {
                            setDirection(Direction.LEFT);
                        }
                        return true;
                    }
                } else {
                    if (Math.abs(diffY) > 50 && Math.abs(velocityY) > 100) {
                        if (diffY > 0) {
                            setDirection(Direction.DOWN);
                        } else {
                            setDirection(Direction.UP);
                        }
                        return true;
                    }
                }
                return false;
            }

            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }
        });

        resetGame();
    }

    public void setGameListener(GameListener listener) {
        this.gameListener = listener;
    }

    public void resetGame() {
        snake.clear();
        int startX = GRID_WIDTH / 2;
        int startY = GRID_HEIGHT / 2;

        snake.add(new Coordinate(startX, startY));
        snake.add(new Coordinate(startX - 1, startY));
        snake.add(new Coordinate(startX - 2, startY));

        currentDirection = Direction.RIGHT;
        nextDirection = Direction.RIGHT;
        currentScore = 0;
        bonusFood = null;
        bonusFoodCounter = 0;

        spawnFood();
        gameState = GameState.READY;

        if (gameListener != null) {
            gameListener.onScoreUpdated(currentScore);
        }
        invalidate();
    }

    public void startGame() {
        gameState = GameState.RUNNING;
        invalidate();
    }

    public void pauseGame() {
        if (gameState == GameState.RUNNING) {
            gameState = GameState.PAUSED;
            invalidate();
        }
    }

    public void resumeGame() {
        if (gameState == GameState.PAUSED) {
            gameState = GameState.RUNNING;
            invalidate();
        }
    }

    public GameState getGameState() {
        return gameState;
    }

    public void setDirection(Direction dir) {
        if (dir == null) return;

        // Prevent 180-degree instant reversal into own neck
        if (currentDirection == Direction.UP && dir == Direction.DOWN) return;
        if (currentDirection == Direction.DOWN && dir == Direction.UP) return;
        if (currentDirection == Direction.LEFT && dir == Direction.RIGHT) return;
        if (currentDirection == Direction.RIGHT && dir == Direction.LEFT) return;

        this.nextDirection = dir;
    }

    public void step() {
        if (gameState != GameState.RUNNING) {
            return;
        }

        currentDirection = nextDirection;
        Coordinate head = snake.get(0);
        int newX = head.x;
        int newY = head.y;

        switch (currentDirection) {
            case UP:
                newY--;
                break;
            case DOWN:
                newY++;
                break;
            case LEFT:
                newX--;
                break;
            case RIGHT:
                newX++;
                break;
        }

        // Wall collision check
        if (newX < 0 || newX >= GRID_WIDTH || newY < 0 || newY >= GRID_HEIGHT) {
            triggerGameOver();
            return;
        }

        Coordinate newHead = new Coordinate(newX, newY);

        // Self collision check (ignore tail if tail moves, but safe to check all body except tail)
        for (int i = 0; i < snake.size() - 1; i++) {
            if (newHead.equals(snake.get(i))) {
                triggerGameOver();
                return;
            }
        }

        snake.add(0, newHead);

        boolean ateFood = false;

        // Check regular food
        if (regularFood != null && newHead.equals(regularFood)) {
            currentScore += 10;
            ateFood = true;
            if (gameListener != null) {
                gameListener.onFoodEaten(false);
                gameListener.onScoreUpdated(currentScore);
            }
            spawnFood();
            // Chance to spawn bonus food if not currently active
            if (bonusFood == null && random.nextInt(5) == 0) {
                spawnBonusFood();
            }
        }

        // Check bonus food
        if (bonusFood != null && newHead.equals(bonusFood)) {
            currentScore += 30;
            ateFood = true;
            bonusFood = null;
            bonusFoodCounter = 0;
            if (gameListener != null) {
                gameListener.onFoodEaten(true);
                gameListener.onScoreUpdated(currentScore);
            }
        }

        if (!ateFood) {
            // Remove tail if didn't eat
            snake.remove(snake.size() - 1);
        }

        // Tick down bonus food timer
        if (bonusFood != null) {
            bonusFoodCounter--;
            if (bonusFoodCounter <= 0) {
                bonusFood = null;
            }
        }

        invalidate();
    }

    private void triggerGameOver() {
        gameState = GameState.GAME_OVER;
        if (gameListener != null) {
            gameListener.onGameOver(currentScore);
        }
        invalidate();
    }

    private void spawnFood() {
        ArrayList<Coordinate> freeSpots = new ArrayList<Coordinate>();
        for (int x = 0; x < GRID_WIDTH; x++) {
            for (int y = 0; y < GRID_HEIGHT; y++) {
                Coordinate c = new Coordinate(x, y);
                if (!isSnakeAt(c) && (bonusFood == null || !bonusFood.equals(c))) {
                    freeSpots.add(c);
                }
            }
        }
        if (!freeSpots.isEmpty()) {
            regularFood = freeSpots.get(random.nextInt(freeSpots.size()));
        }
    }

    private void spawnBonusFood() {
        ArrayList<Coordinate> freeSpots = new ArrayList<Coordinate>();
        for (int x = 0; x < GRID_WIDTH; x++) {
            for (int y = 0; y < GRID_HEIGHT; y++) {
                Coordinate c = new Coordinate(x, y);
                if (!isSnakeAt(c) && (regularFood == null || !regularFood.equals(c))) {
                    freeSpots.add(c);
                }
            }
        }
        if (!freeSpots.isEmpty()) {
            bonusFood = freeSpots.get(random.nextInt(freeSpots.size()));
            bonusFoodCounter = BONUS_DURATION;
        }
    }

    private boolean isSnakeAt(Coordinate coord) {
        for (int i = 0; i < snake.size(); i++) {
            if (snake.get(i).equals(coord)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        int availableWidth = w - getPaddingLeft() - getPaddingRight();
        int availableHeight = h - getPaddingTop() - getPaddingBottom();

        cellWidth = availableWidth / GRID_WIDTH;
        cellHeight = availableHeight / GRID_HEIGHT;

        // Keep square cells
        int cellSize = Math.min(cellWidth, cellHeight);
        if (cellSize < 4) cellSize = 4;
        cellWidth = cellSize;
        cellHeight = cellSize;

        offsetX = getPaddingLeft() + (availableWidth - (cellWidth * GRID_WIDTH)) / 2;
        offsetY = getPaddingTop() + (availableHeight - (cellHeight * GRID_HEIGHT)) / 2;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (cellWidth <= 0 || cellHeight <= 0) return;

        // Draw grid lines
        for (int x = 0; x <= GRID_WIDTH; x++) {
            float startX = offsetX + x * cellWidth;
            canvas.drawLine(startX, offsetY, startX, offsetY + GRID_HEIGHT * cellHeight, paintGrid);
        }
        for (int y = 0; y <= GRID_HEIGHT; y++) {
            float startY = offsetY + y * cellHeight;
            canvas.drawLine(offsetX, startY, offsetX + GRID_WIDTH * cellWidth, startY, paintGrid);
        }

        // Draw regular food (Apple shape)
        if (regularFood != null) {
            float fx = offsetX + regularFood.x * cellWidth;
            float fy = offsetY + regularFood.y * cellHeight;
            rectBuffer.set(fx + 2, fy + 2, fx + cellWidth - 2, fy + cellHeight - 2);
            canvas.drawRoundRect(rectBuffer, 6f, 6f, paintFood);
        }

        // Draw bonus food (Pulsing golden star shape)
        if (bonusFood != null) {
            float bx = offsetX + bonusFood.x * cellWidth;
            float by = offsetY + bonusFood.y * cellHeight;
            rectBuffer.set(bx + 1, by + 1, bx + cellWidth - 1, by + cellHeight - 1);
            canvas.drawOval(rectBuffer, paintBonusFood);
        }

        // Draw Snake body
        for (int i = snake.size() - 1; i >= 1; i--) {
            Coordinate c = snake.get(i);
            float sx = offsetX + c.x * cellWidth;
            float sy = offsetY + c.y * cellHeight;

            rectBuffer.set(sx + 1, sy + 1, sx + cellWidth - 1, sy + cellHeight - 1);
            paintSnakeBody.setColor((i % 2 == 0) ? Color.parseColor("#00B0FF") : Color.parseColor("#2979FF"));
            canvas.drawRoundRect(rectBuffer, 4f, 4f, paintSnakeBody);
        }

        // Draw Snake Head with eyes
        if (!snake.isEmpty()) {
            Coordinate head = snake.get(0);
            float hx = offsetX + head.x * cellWidth;
            float hy = offsetY + head.y * cellHeight;

            rectBuffer.set(hx + 1, hy + 1, hx + cellWidth - 1, hy + cellHeight - 1);
            canvas.drawRoundRect(rectBuffer, 8f, 8f, paintSnakeHead);

            // Draw directional eyes
            float eyeRadius = cellWidth * 0.12f;
            float eyeOffset1X = 0f, eyeOffset1Y = 0f;
            float eyeOffset2X = 0f, eyeOffset2Y = 0f;

            switch (currentDirection) {
                case UP:
                    eyeOffset1X = hx + cellWidth * 0.3f;
                    eyeOffset1Y = hy + cellHeight * 0.3f;
                    eyeOffset2X = hx + cellWidth * 0.7f;
                    eyeOffset2Y = hy + cellHeight * 0.3f;
                    break;
                case DOWN:
                    eyeOffset1X = hx + cellWidth * 0.3f;
                    eyeOffset1Y = hy + cellHeight * 0.7f;
                    eyeOffset2X = hx + cellWidth * 0.7f;
                    eyeOffset2Y = hy + cellHeight * 0.7f;
                    break;
                case LEFT:
                    eyeOffset1X = hx + cellWidth * 0.3f;
                    eyeOffset1Y = hy + cellHeight * 0.3f;
                    eyeOffset2X = hx + cellWidth * 0.3f;
                    eyeOffset2Y = hy + cellHeight * 0.7f;
                    break;
                case RIGHT:
                    eyeOffset1X = hx + cellWidth * 0.7f;
                    eyeOffset1Y = hy + cellHeight * 0.3f;
                    eyeOffset2X = hx + cellWidth * 0.7f;
                    eyeOffset2Y = hy + cellHeight * 0.7f;
                    break;
            }

            canvas.drawCircle(eyeOffset1X, eyeOffset1Y, eyeRadius, paintEye);
            canvas.drawCircle(eyeOffset2X, eyeOffset2Y, eyeRadius, paintEye);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (gestureDetector != null && gestureDetector.onTouchEvent(event)) {
            return true;
        }
        return super.onTouchEvent(event);
    }
}