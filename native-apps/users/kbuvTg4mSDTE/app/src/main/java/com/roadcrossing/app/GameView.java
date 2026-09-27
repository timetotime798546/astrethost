package com.roadcrossing.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameView extends View {

    private MainActivity mainActivity;
    private GameLoop gameLoop;

    private int screenWidth;
    private int screenHeight;

    private Player player;
    private final List<Lane> lanes = new ArrayList<Lane>();
    private final float LANE_HEIGHT = 150f;
    private final int COLS = 9;
    private float colWidth;

    private float cameraY = 0f;
    private float targetCameraY = 0f;

    public enum GameState {
        MENU, PLAYING, CRASHING, GAME_OVER, PAUSED
    }
    private GameState currentState = GameState.MENU;

    private float screenShakeX = 0f;
    private float screenShakeY = 0f;
    private float crashTimer = 0f;
    private static final float CRASH_DURATION = 50f; // crash frames

    private final float speedFactor = 1.0f;
    private Paint paint;
    private Random random;

    public GameView(Context context) {
        super(context);
        init(context);
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        random = new Random();
    }

    public void setMainActivity(MainActivity activity) {
        this.mainActivity = activity;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        screenWidth = w;
        screenHeight = h;
        colWidth = (float) screenWidth / COLS;
        initGame();
    }

    public void initGame() {
        synchronized (lanes) {
            lanes.clear();
            for (int i = 0; i < 25; i++) {
                generateLane(i);
            }
        }

        player = new Player(COLS / 2, 0);
        float pX = player.gridX * colWidth + colWidth / 2;
        float pY = getVirtualY(0) + LANE_HEIGHT / 2;
        player.setPosition(pX, pY);

        cameraY = 0f;
        targetCameraY = 0f;
        crashTimer = 0f;
        screenShakeX = 0f;
        screenShakeY = 0f;
    }

    private void generateLane(int index) {
        int type;
        if (index < 3) {
            type = 0; // safe zones
        } else {
            int block = index % 11;
            if (block == 0 || block == 5 || block == 6) {
                type = 0; // safe grassy divider
            } else {
                type = 1; // dangerous road
            }
        }

        int direction = random.nextBoolean() ? 1 : -1;
        float baseSpeed = 2.0f + (float) index * 0.15f;
        if (baseSpeed > 11.5f) baseSpeed = 11.5f;

        int color;
        if (type == 0) {
            color = (index % 2 == 0) ? Color.rgb(76, 175, 80) : Color.rgb(90, 180, 95);
        } else {
            color = (index % 2 == 0) ? Color.rgb(60, 70, 75) : Color.rgb(70, 80, 85);
        }

        Lane lane = new Lane(index, type, direction, baseSpeed, color);

        if (type == 1) {
            int numCars = 2 + random.nextInt(2);
            float separation = (float) screenWidth / numCars;

            for (int j = 0; j < numCars; j++) {
                int design = random.nextInt(5);
                float w = 100f, h = 60f;
                int c = Color.RED;

                switch (design) {
                    case 0: // mini red
                        w = 85f; h = 55f;
                        c = Color.rgb(229, 57, 53);
                        break;
                    case 1: // blue sedan
                        w = 105f; h = 60f;
                        c = Color.rgb(30, 136, 229);
                        break;
                    case 2: // cab yellow
                        w = 95f; h = 58f;
                        c = Color.rgb(253, 216, 53);
                        break;
                    case 3: // cargo truck
                        w = 150f; h = 72f;
                        c = Color.rgb(109, 76, 65);
                        break;
                    case 4: // racing sports
                        w = 95f; h = 55f;
                        c = Color.rgb(142, 36, 170);
                        break;
                }

                float startX = j * separation + random.nextFloat() * 40f;
                Vehicle vehicle = new Vehicle(startX, w, h, design, c);
                lane.vehicles.add(vehicle);
            }
        }
        lanes.add(lane);
    }

    private float getVirtualY(int index) {
        return screenHeight - (index + 1) * LANE_HEIGHT;
    }

    public void update() {
        if (currentState == GameState.PLAYING) {
            synchronized (lanes) {
                // Update vehicles positions
                for (int i = 0; i < lanes.size(); i++) {
                    Lane ln = lanes.get(i);
                    for (int j = 0; j < ln.vehicles.size(); j++) {
                        ln.vehicles.get(j).update(ln.speed, ln.direction, screenWidth, speedFactor);
                    }
                }

                // Check lane infinite generations
                int topIdx = lanes.get(lanes.size() - 1).index;
                if (player.gridY > topIdx - 12) {
                    for (int i = 1; i <= 15; i++) {
                        generateLane(topIdx + i);
                    }
                }

                // Unload/prune bottom lanes to prevent leak issues
                while (lanes.size() > 50) {
                    if (lanes.get(0).index < player.gridY - 8) {
                        lanes.remove(0);
                    } else {
                        break;
                    }
                }
            }

            player.update(speedFactor);

            // Interpolate tracking camera
            float pVirtY = getVirtualY(player.gridY) + LANE_HEIGHT / 2;
            float targetCam = pVirtY - screenHeight * 0.72f;
            if (targetCam < 0) targetCam = 0;
            cameraY += (targetCam - cameraY) * 0.1f * speedFactor;

            checkCollision();

            if (mainActivity != null) {
                mainActivity.updateHud(player.gridY);
            }

        } else if (currentState == GameState.CRASHING) {
            crashTimer += speedFactor;
            float remaining = 15f * (1.0f - (crashTimer / CRASH_DURATION));
            screenShakeX = (random.nextFloat() - 0.5f) * remaining * 2;
            screenShakeY = (random.nextFloat() - 0.5f) * remaining * 2;

            player.rotation += 22f;
            player.scale -= 0.016f;
            if (player.scale < 0) player.scale = 0;
            player.flashCount++;

            // Slight movement of vehicles in crash frame for physics continuity
            synchronized (lanes) {
                for (int i = 0; i < lanes.size(); i++) {
                    Lane ln = lanes.get(i);
                    for (int j = 0; j < ln.vehicles.size(); j++) {
                        ln.vehicles.get(j).update(ln.speed * 0.15f, ln.direction, screenWidth, speedFactor);
                    }
                }
            }

            if (crashTimer >= CRASH_DURATION) {
                currentState = GameState.GAME_OVER;
                screenShakeX = 0f;
                screenShakeY = 0f;
                if (mainActivity != null) {
                    mainActivity.onGameOver(player.gridY);
                }
            }
        }
    }

    private void checkCollision() {
        float sizeVal = player.size;
        RectF playerRect = new RectF(
                player.drawX - sizeVal * 0.32f,
                player.drawY - sizeVal * 0.32f,
                player.drawX + sizeVal * 0.32f,
                player.drawY + sizeVal * 0.32f
        );

        synchronized (lanes) {
            for (int i = 0; i < lanes.size(); i++) {
                Lane ln = lanes.get(i);
                if (Math.abs(ln.index - player.gridY) <= 1 && ln.type == 1) {
                    float y = getVirtualY(ln.index);

                    for (int j = 0; j < ln.vehicles.size(); j++) {
                        Vehicle v = ln.vehicles.get(j);
                        RectF vehicleRect = new RectF(
                                v.x,
                                y + LANE_HEIGHT / 2 - v.height / 2,
                                v.x + v.width,
                                y + LANE_HEIGHT / 2 + v.height / 2
                        );

                        if (RectF.intersects(playerRect, vehicleRect)) {
                            triggerCrash();
                            return;
                        }
                    }
                }
            }
        }
    }

    private void triggerCrash() {
        currentState = GameState.CRASHING;
        crashTimer = 0f;
        player.flashCount = 1;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (screenWidth == 0 || screenHeight == 0) return;

        canvas.save();
        // Camera translation with shaking factor
        canvas.translate(screenShakeX, screenShakeY + cameraY);

        synchronized (lanes) {
            for (int i = 0; i < lanes.size(); i++) {
                Lane ln = lanes.get(i);
                float y = getVirtualY(ln.index);
                ln.draw(canvas, y, LANE_HEIGHT, screenWidth, paint);
            }

            for (int i = 0; i < lanes.size(); i++) {
                Lane ln = lanes.get(i);
                if (ln.type == 1) {
                    float y = getVirtualY(ln.index);
                    float mid = y + LANE_HEIGHT / 2;
                    for (int j = 0; j < ln.vehicles.size(); j++) {
                        ln.vehicles.get(j).draw(canvas, mid, paint);
                    }
                }
            }
        }

        if (player != null) {
            player.draw(canvas, paint);
        }

        canvas.restore();
    }

    public void movePlayerUp() {
        if (currentState != GameState.PLAYING) return;
        player.gridY++;
        updatePlayerTarget();
    }

    public void movePlayerDown() {
        if (currentState != GameState.PLAYING) return;
        if (player.gridY > 0) {
            player.gridY--;
            updatePlayerTarget();
        }
    }

    public void movePlayerLeft() {
        if (currentState != GameState.PLAYING) return;
        if (player.gridX > 0) {
            player.gridX--;
            updatePlayerTarget();
        }
    }

    public void movePlayerRight() {
        if (currentState != GameState.PLAYING) return;
        if (player.gridX < COLS - 1) {
            player.gridX++;
            updatePlayerTarget();
        }
    }

    private void updatePlayerTarget() {
        float tx = player.gridX * colWidth + colWidth / 2;
        float ty = getVirtualY(player.gridY) + LANE_HEIGHT / 2;
        player.setTarget(tx, ty);
    }

    public void resumeGame() {
        currentState = GameState.PLAYING;
        if (gameLoop == null) {
            gameLoop = new GameLoop(this);
            gameLoop.setRunning(true);
            gameLoop.start();
        }
    }

    public void pauseGame() {
        if (gameLoop != null) {
            gameLoop.setRunning(false);
            try {
                gameLoop.join();
            } catch (InterruptedException e) {
                // Keep silence
            }
            gameLoop = null;
        }
        if (currentState == GameState.PLAYING) {
            currentState = GameState.PAUSED;
        }
    }
}