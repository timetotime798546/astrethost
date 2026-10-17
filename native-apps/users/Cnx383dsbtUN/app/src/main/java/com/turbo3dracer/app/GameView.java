package com.turbo3dracer.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.util.ArrayList;
import java.util.List;

public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    public interface GameCallback {
        void onStatsUpdated(int speedKmh, int distanceMeters, int coins, int nitroPct, int score);
        void onGameOver(int finalScore, int coinsEarned, int distanceMeters);
    }

    private GameCallback gameCallback;
    private Thread gameThread;
    private volatile boolean isRunning = false;
    private SurfaceHolder surfaceHolder;

    // Road & World Configuration
    private List<RoadSegment> segments;
    private int segmentLength = 200;
    private int totalSegments = 1600;
    private float trackLength;
    private float cameraHeight = 1000f;
    private float cameraDepth = 0.84f;
    private float roadWidth = 2000f;

    // Player State
    private float playerZ = 0f;
    private float playerX = 0f; // -1 (left) to 1 (right)
    private float currentSpeed = 0f;
    private float maxSpeed = 240f;
    private float acceleration = 0.8f;
    private float braking = 2.0f;
    private float decel = 0.35f;
    private float steering = 0f;
    private boolean isAccelerating = false;
    private boolean isBraking = false;
    private boolean isSteeringLeft = false;
    private boolean isSteeringRight = false;
    private boolean isNitroActive = false;
    private float nitroTank = 100f;

    // Game stats
    private int coinsCollected = 0;
    private int totalDistance = 0;
    private int currentScore = 0;
    private int trackTheme = 0;

    // Traffic Cars
    private List<TrafficCar> trafficCars;
    private CarModel playerCar;
    private Paint paint;
    private Path polygonPath;
    private Vibrator vibrator;

    // Particle FX
    private float nitroFlamePhase = 0f;
    private float skyOffset = 0f;

    public GameView(Context context) {
        super(context);
        init();
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        polygonPath = new Path();
        vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        trafficCars = new ArrayList<TrafficCar>();
    }

    public void setGameCallback(GameCallback callback) {
        this.gameCallback = callback;
    }

    public void setupRace(CarModel car, int trackIndex) {
        this.playerCar = car;
        this.trackTheme = trackIndex;
        this.maxSpeed = car.getTopSpeed() * 1.5f;
        this.acceleration = 0.4f + (car.getAcceleration() * 0.12f);
        buildTrack(trackIndex);
        spawnTraffic();
    }

    private void buildTrack(int theme) {
        segments = new ArrayList<RoadSegment>();
        int grassColor1, grassColor2, roadColor1, roadColor2;

        if (theme == 1) { // Canyon Desert
            grassColor1 = Color.rgb(180, 100, 45);
            grassColor2 = Color.rgb(160, 85, 35);
            roadColor1 = Color.rgb(40, 35, 35);
            roadColor2 = Color.rgb(55, 48, 48);
        } else if (theme == 2) { // Cyber Highway
            grassColor1 = Color.rgb(12, 10, 30);
            grassColor2 = Color.rgb(20, 16, 45);
            roadColor1 = Color.rgb(25, 20, 50);
            roadColor2 = Color.rgb(35, 30, 65);
        } else { // Neon City
            grassColor1 = Color.rgb(10, 20, 28);
            grassColor2 = Color.rgb(15, 28, 38);
            roadColor1 = Color.rgb(26, 30, 36);
            roadColor2 = Color.rgb(35, 40, 48);
        }

        for (int i = 0; i < totalSegments; i++) {
            RoadSegment seg = new RoadSegment(i);
            seg.p1WorldZ = i * segmentLength;
            seg.p2WorldZ = (i + 1) * segmentLength;
            seg.p1WorldX = 0;
            seg.p2WorldX = 0;

            // Generate hills and undulating terrain
            seg.p1WorldY = (float) (Math.sin(i / 30.0) * 1200.0);
            seg.p2WorldY = (float) (Math.sin((i + 1) / 30.0) * 1200.0);

            // Curves
            if (i > 100 && i < 250) seg.curve = 1.8f;
            else if (i > 350 && i < 500) seg.curve = -2.2f;
            else if (i > 650 && i < 850) seg.curve = 3.0f;
            else if (i > 1000 && i < 1250) seg.curve = -2.6f;
            else seg.curve = 0f;

            // Rumble and stripe colors
            boolean alternate = (i / 4) % 2 == 0;
            seg.colorGrass = alternate ? grassColor1 : grassColor2;
            seg.colorRumble = alternate ? Color.rgb(255, 0, 85) : Color.rgb(255, 255, 255);
            seg.colorRoad = alternate ? roadColor1 : roadColor2;
            seg.colorLane = alternate ? Color.rgb(0, 229, 255) : Color.TRANSPARENT;

            // Spawn collectable coins along the track
            if (i % 25 == 0 && i > 40) {
                seg.hasCoin = true;
                seg.coinX = (float) (Math.sin(i * 0.4) * 0.65);
            }

            segments.add(seg);
        }
        trackLength = totalSegments * segmentLength;
    }

    private void spawnTraffic() {
        trafficCars.clear();
        int[] colors = new int[]{
                Color.rgb(255, 171, 0),
                Color.rgb(213, 0, 0),
                Color.rgb(124, 77, 255),
                Color.rgb(0, 230, 118),
                Color.rgb(255, 214, 0)
        };

        for (int i = 0; i < 28; i++) {
            float z = 1200f + (i * (trackLength / 32f));
            float offset = ((i % 4) - 1.5f) * 0.45f;
            float speed = 80f + (float) (Math.random() * 70f);
            int primary = colors[i % colors.length];
            int secondary = Color.rgb(220, 220, 220);
            trafficCars.add(new TrafficCar(z, offset, speed, primary, secondary));
        }
    }

    public void setAccelerating(boolean acc) { this.isAccelerating = acc; }
    public void setBraking(boolean brk) { this.isBraking = brk; }
    public void setSteering(boolean left, boolean right) {
        this.isSteeringLeft = left;
        this.isSteeringRight = right;
    }
    public void setNitro(boolean nitro) {
        if (nitro && nitroTank > 5f) {
            this.isNitroActive = true;
            SoundEffects.getInstance().playNitroBoost();
        } else {
            this.isNitroActive = false;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        isRunning = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        isRunning = false;
        try {
            if (gameThread != null) gameThread.join();
        } catch (InterruptedException ignored) {}
    }

    public void pauseGame() {
        isRunning = false;
    }

    public void resumeGame() {
        if (!isRunning) {
            isRunning = true;
            gameThread = new Thread(this);
            gameThread.start();
        }
    }

    @Override
    public void run() {
        long lastTime = System.currentTimeMillis();
        while (isRunning) {
            long now = System.currentTimeMillis();
            float dt = (now - lastTime) / 1000f;
            if (dt > 0.05f) dt = 0.05f;
            lastTime = now;

            updatePhysics(dt);
            drawFrame();

            try {
                Thread.sleep(16); // Target ~60 FPS
            } catch (InterruptedException ignored) {}
        }
    }

    private void updatePhysics(float dt) {
        // Nitro management
        float speedLimit = maxSpeed;
        if (isNitroActive && nitroTank > 0f) {
            speedLimit *= 1.4f;
            currentSpeed += acceleration * 2.2f;
            nitroTank -= 15f * dt;
            if (nitroTank <= 0f) {
                nitroTank = 0f;
                isNitroActive = false;
            }
        } else {
            // Passive nitro recharge over time
            if (nitroTank < 100f) {
                nitroTank += 4f * dt;
            }
        }

        // Acceleration & braking
        if (isAccelerating) {
            currentSpeed += acceleration;
            if (currentSpeed > speedLimit) currentSpeed = speedLimit;
        } else if (isBraking) {
            currentSpeed -= braking;
            if (currentSpeed < 0f) currentSpeed = 0f;
        } else {
            currentSpeed -= decel;
            if (currentSpeed < 0f) currentSpeed = 0f;
        }

        // Steering
        float speedRatio = (currentSpeed / maxSpeed);
        if (isSteeringLeft) {
            steering = -1f;
            playerX -= 1.15f * speedRatio * (playerCar != null ? (playerCar.getHandling() / 8.0f) : 1f) * dt * 2.0f;
        } else if (isSteeringRight) {
            steering = 1f;
            playerX += 1.15f * speedRatio * (playerCar != null ? (playerCar.getHandling() / 8.0f) : 1f) * dt * 2.0f;
        } else {
            steering = 0f;
        }

        // Boundary constraint
        if (playerX < -1.4f) playerX = -1.4f;
        if (playerX > 1.4f) playerX = 1.4f;

        // Off-road friction penalty
        if (Math.abs(playerX) > 1.0f && currentSpeed > 80f) {
            currentSpeed -= 1.8f;
        }

        // Update player world position along the 3D track
        playerZ += currentSpeed * dt * 30f;
        while (playerZ >= trackLength) {
            playerZ -= trackLength;
        }

        totalDistance += (int) (currentSpeed * dt * 8f);
        skyOffset += steering * 4f;

        // Current road segment
        int currentSegIdx = (int) (playerZ / segmentLength) % totalSegments;
        RoadSegment baseSegment = segments.get(currentSegIdx);

        // Centrifugal curve force
        playerX -= (baseSegment.curve * speedRatio * speedRatio * 0.015f);

        // Sound effects revving
        if (Math.random() < 0.25) {
            SoundEffects.getInstance().playEngineRev((int) (speedRatio * 50));
        }

        // Coin collection detection
        if (baseSegment.hasCoin && !baseSegment.coinCollected) {
            if (Math.abs(playerX - baseSegment.coinX) < 0.35f) {
                baseSegment.coinCollected = true;
                coinsCollected++;
                SoundEffects.getInstance().playCoinPickup();
                if (vibrator != null) vibrator.vibrate(20);
            }
        }

        // Traffic AI updates & Collision logic
        for (int i = 0; i < trafficCars.size(); i++) {
            TrafficCar tc = trafficCars.get(i);
            tc.update(trackLength);

            // Check collision with player
            float dz = Math.abs(tc.z - playerZ);
            if (dz < 160f || (trackLength - dz) < 160f) {
                if (Math.abs(tc.offset - playerX) < 0.28f) {
                    // Impact collision
                    currentSpeed *= 0.35f;
                    SoundEffects.getInstance().playCrash();
                    if (vibrator != null) vibrator.vibrate(180);
                    // Push traffic away
                    tc.z += 400f;
                }
            }
        }

        // Live calculation of score
        currentScore = ScoreHelper.calculateScore(totalDistance, coinsCollected, currentSpeed);

        if (gameCallback != null) {
            final int displaySpeed = (int) (currentSpeed * 0.95f);
            final int displayDist = totalDistance;
            final int displayCoins = coinsCollected;
            final int displayNitro = (int) nitroTank;
            final int displayScore = currentScore;

            post(new Runnable() {
                @Override
                public void run() {
                    if (gameCallback != null) {
                        gameCallback.onStatsUpdated(displaySpeed, displayDist, displayCoins, displayNitro, displayScore);
                    }
                }
            });
        }
    }

    private void drawFrame() {
        Canvas canvas = surfaceHolder.lockCanvas();
        if (canvas == null) return;

        int width = getWidth();
        int height = getHeight();

        // 1. Draw Horizon & Sky
        drawHorizon(canvas, width, height);

        // 2. Render 3D Perspective Road
        drawRoad(canvas, width, height);

        // 3. Render 3D Traffic Cars
        drawTraffic(canvas, width, height);

        // 4. Render Player Vehicle & Nitro Effects
        drawPlayerCar(canvas, width, height);

        surfaceHolder.unlockCanvasAndPost(canvas);
    }

    private void drawHorizon(Canvas canvas, int width, int height) {
        int horizonY = height / 2;

        // Cyber / Synthwave gradient sky
        paint.setStyle(Paint.Style.FILL);
        if (trackTheme == 1) { // Canyon Desert
            paint.setColor(Color.rgb(255, 112, 67));
            canvas.drawRect(0, 0, width, horizonY, paint);
            paint.setColor(Color.rgb(180, 50, 30));
            canvas.drawRect(0, horizonY * 0.6f, width, horizonY, paint);
        } else if (trackTheme == 2) { // Cyber Highway
            paint.setColor(Color.rgb(15, 12, 38));
            canvas.drawRect(0, 0, width, horizonY, paint);
            // Cyber Sun
            paint.setColor(Color.rgb(255, 0, 128));
            canvas.drawCircle(width * 0.5f, horizonY * 0.85f, horizonY * 0.45f, paint);
        } else { // Neon City
            paint.setColor(Color.rgb(10, 15, 25));
            canvas.drawRect(0, 0, width, horizonY, paint);

            // Distant neon skyscrapers
            paint.setColor(Color.rgb(20, 30, 48));
            for (int i = 0; i < 15; i++) {
                float bldgW = width * 0.08f;
                float bldgH = (float) (horizonY * (0.3 + ((i * 37) % 50) / 100.0));
                float bldgX = ((i * bldgW) + skyOffset) % (width + bldgW) - bldgW;
                canvas.drawRect(bldgX, horizonY - bldgH, bldgX + bldgW - 4, horizonY, paint);
            }
        }
    }

    private void drawRoad(Canvas canvas, int width, int height) {
        if (segments == null || segments.isEmpty()) return;

        int baseIdx = (int) (playerZ / segmentLength);
        float maxY = height;
        float x = 0;
        float dx = 0;

        int drawDistance = Math.min(220, totalSegments - 1);

        for (int n = 0; n < drawDistance; n++) {
            int segIdx = (baseIdx + n) % totalSegments;
            RoadSegment segment = segments.get(segIdx);

            float camX = (playerX * roadWidth) - x;
            float camY = cameraHeight + segments.get(baseIdx % totalSegments).p1WorldY;
            float camZ = playerZ - (segIdx < baseIdx ? trackLength : 0);

            segment.project(camX, camY, camZ, cameraDepth, width, height, roadWidth);

            x += dx;
            dx += segment.curve;

            if (segment.p1ScreenY >= maxY || segment.p2ScreenY >= segment.p1ScreenY) {
                continue;
            }

            // Draw Grass / Terrain
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(segment.colorGrass);
            canvas.drawRect(0, segment.p2ScreenY, width, segment.p1ScreenY, paint);

            // Draw Rumble Strip
            paint.setColor(segment.colorRumble);
            drawPolygon(canvas,
                    segment.p1ScreenX - segment.p1ScreenW * 1.15f, segment.p1ScreenY,
                    segment.p1ScreenX - segment.p1ScreenW, segment.p1ScreenY,
                    segment.p2ScreenX - segment.p2ScreenW, segment.p2ScreenY,
                    segment.p2ScreenX - segment.p2ScreenW * 1.15f, segment.p2ScreenY);

            drawPolygon(canvas,
                    segment.p1ScreenX + segment.p1ScreenW, segment.p1ScreenY,
                    segment.p1ScreenX + segment.p1ScreenW * 1.15f, segment.p1ScreenY,
                    segment.p2ScreenX + segment.p2ScreenW * 1.15f, segment.p2ScreenY,
                    segment.p2ScreenX + segment.p2ScreenW, segment.p2ScreenY);

            // Draw Road Surface
            paint.setColor(segment.colorRoad);
            drawPolygon(canvas,
                    segment.p1ScreenX - segment.p1ScreenW, segment.p1ScreenY,
                    segment.p1ScreenX + segment.p1ScreenW, segment.p1ScreenY,
                    segment.p2ScreenX + segment.p2ScreenW, segment.p2ScreenY,
                    segment.p2ScreenX - segment.p2ScreenW, segment.p2ScreenY);

            // Draw Lane Stripes
            if (segment.colorLane != Color.TRANSPARENT) {
                paint.setColor(segment.colorLane);
                float laneW1 = segment.p1ScreenW * 0.04f;
                float laneW2 = segment.p2ScreenW * 0.04f;
                drawPolygon(canvas,
                        segment.p1ScreenX - laneW1, segment.p1ScreenY,
                        segment.p1ScreenX + laneW1, segment.p1ScreenY,
                        segment.p2ScreenX + laneW2, segment.p2ScreenY,
                        segment.p2ScreenX - laneW2, segment.p2ScreenY);
            }

            // Draw Coins in 3D Perspective
            if (segment.hasCoin && !segment.coinCollected) {
                float coinScreenX = segment.p1ScreenX + (segment.p1ScreenW * segment.coinX);
                float coinSize = Math.max(4f, segment.p1ScreenW * 0.09f);
                paint.setColor(Color.rgb(255, 214, 0));
                canvas.drawCircle(coinScreenX, segment.p1ScreenY - (coinSize * 1.2f), coinSize, paint);
                paint.setColor(Color.rgb(255, 255, 255));
                canvas.drawCircle(coinScreenX, segment.p1ScreenY - (coinSize * 1.2f), coinSize * 0.45f, paint);
            }

            maxY = segment.p1ScreenY;
        }
    }

    private void drawTraffic(Canvas canvas, int width, int height) {
        int baseIdx = (int) (playerZ / segmentLength);
        for (int i = 0; i < trafficCars.size(); i++) {
            TrafficCar tc = trafficCars.get(i);
            float relZ = tc.z - playerZ;
            if (relZ < 0) relZ += trackLength;

            if (relZ > 0 && relZ < (segmentLength * 160)) {
                int segIdx = (int) (tc.z / segmentLength) % totalSegments;
                RoadSegment segment = segments.get(segIdx);
                float scale = cameraDepth / (relZ);
                float screenX = (width / 2.0f) + (scale * ((tc.offset * roadWidth) - (playerX * roadWidth)) * (width / 2.0f));
                float screenY = (height / 2.0f) - (scale * (segment.p1WorldY - (cameraHeight + segments.get(baseIdx % totalSegments).p1WorldY)) * (height / 2.0f));

                tc.render(canvas, screenX, screenY, scale * 18f, paint);
            }
        }
    }

    private void drawPlayerCar(Canvas canvas, int width, int height) {
        float carCenterX = width / 2.0f;
        float carBottomY = height * 0.94f;
        float carWidth = width * 0.22f;
        float carHeight = carWidth * 0.52f;

        // Tilt effect based on steering
        float tiltOffset = steering * (carWidth * 0.08f);

        // Wheels
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(15, 15, 20));
        canvas.drawRoundRect(new RectF(carCenterX - (carWidth * 0.52f), carBottomY - (carHeight * 0.35f),
                carCenterX - (carWidth * 0.36f), carBottomY + (carHeight * 0.05f)), 8, 8, paint);
        canvas.drawRoundRect(new RectF(carCenterX + (carWidth * 0.36f), carBottomY - (carHeight * 0.35f),
                carCenterX + (carWidth * 0.52f), carBottomY + (carHeight * 0.05f)), 8, 8, paint);

        // Ground Shadow
        paint.setColor(Color.argb(140, 0, 0, 0));
        canvas.drawOval(new RectF(carCenterX - (carWidth * 0.55f), carBottomY - (carHeight * 0.2f),
                carCenterX + (carWidth * 0.55f), carBottomY + (carHeight * 0.15f)), paint);

        // Chassis Main Body
        int pColor = (playerCar != null) ? playerCar.getPrimaryColor() : Color.rgb(0, 229, 255);
        int aColor = (playerCar != null) ? playerCar.getAccentColor() : Color.rgb(255, 0, 85);

        paint.setColor(pColor);
        RectF bodyRect = new RectF(carCenterX - (carWidth * 0.48f) + tiltOffset, carBottomY - carHeight,
                carCenterX + (carWidth * 0.48f) + tiltOffset, carBottomY);
        canvas.drawRoundRect(bodyRect, 18, 18, paint);

        // Cockpit / Rear Glass
        paint.setColor(Color.rgb(20, 28, 40));
        RectF glassRect = new RectF(carCenterX - (carWidth * 0.32f) + (tiltOffset * 1.2f),
                carBottomY - (carHeight * 1.05f),
                carCenterX + (carWidth * 0.32f) + (tiltOffset * 1.2f),
                carBottomY - (carHeight * 0.45f));
        canvas.drawRoundRect(glassRect, 12, 12, paint);

        // Neon spoiler & Accent trims
        paint.setColor(aColor);
        canvas.drawRect(carCenterX - (carWidth * 0.45f) + tiltOffset, carBottomY - (carHeight * 1.15f),
                carCenterX + (carWidth * 0.45f) + tiltOffset, carBottomY - (carHeight * 1.05f), paint);

        // High-intensity Dual Exhaust & Nitro Flames
        paint.setColor(Color.rgb(60, 60, 60));
        float exhaustL = carCenterX - (carWidth * 0.22f) + tiltOffset;
        float exhaustR = carCenterX + (carWidth * 0.22f) + tiltOffset;
        canvas.drawCircle(exhaustL, carBottomY - (carHeight * 0.15f), carWidth * 0.045f, paint);
        canvas.drawCircle(exhaustR, carBottomY - (carHeight * 0.15f), carWidth * 0.045f, paint);

        if (isNitroActive || isAccelerating) {
            nitroFlamePhase += 0.35f;
            float flameH = isNitroActive ? (carHeight * 0.9f) : (carHeight * 0.35f);
            flameH += (float) (Math.sin(nitroFlamePhase) * 15f);

            paint.setColor(isNitroActive ? Color.rgb(0, 229, 255) : Color.rgb(255, 120, 0));
            canvas.drawOval(new RectF(exhaustL - (carWidth * 0.04f), carBottomY - (carHeight * 0.15f),
                    exhaustL + (carWidth * 0.04f), carBottomY - (carHeight * 0.15f) + flameH), paint);
            canvas.drawOval(new RectF(exhaustR - (carWidth * 0.04f), carBottomY - (carHeight * 0.15f),
                    exhaustR + (carWidth * 0.04f), carBottomY - (carHeight * 0.15f) + flameH), paint);
        }

        // Taillights
        paint.setColor(Color.rgb(255, 0, 50));
        canvas.drawRect(carCenterX - (carWidth * 0.42f) + tiltOffset, carBottomY - (carHeight * 0.38f),
                carCenterX - (carWidth * 0.24f) + tiltOffset, carBottomY - (carHeight * 0.24f), paint);
        canvas.drawRect(carCenterX + (carWidth * 0.24f) + tiltOffset, carBottomY - (carHeight * 0.38f),
                carCenterX + (carWidth * 0.42f) + tiltOffset, carBottomY - (carHeight * 0.24f), paint);
    }

    private void drawPolygon(Canvas canvas, float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4) {
        polygonPath.reset();
        polygonPath.moveTo(x1, y1);
        polygonPath.lineTo(x2, y2);
        polygonPath.lineTo(x3, y3);
        polygonPath.lineTo(x4, y4);
        polygonPath.close();
        canvas.drawPath(polygonPath, paint);
    }
}