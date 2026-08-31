package com.motohillracer.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;

public class GameView extends View {

    public interface GameListener {
        void onUpdateHud(float distance, int coins, float fuel);
        void onGameOver(String reason, int finalCoins, float finalDistance, boolean success);
    }

    private GameListener gameListener;

    // Track Properties
    private static final float SEGMENT_WIDTH = 150f;
    private static final int TRACK_LENGTH = 30000; // Total track x in pixels

    // Canvas & Camera Size
    private float viewWidth = 1080f;
    private float viewHeight = 1920f;
    private float cameraX = 0f;

    // Physics constants
    private static final float GRAVITY = 0.45f;

    // Bike States
    private float bikeX;
    private float bikeY;
    private float vx;
    private float vy;
    private float bikeAngle; // in radians
    private float angularVelocity;
    private float fuel;
    private int coinsCollected;

    // Control Inputs
    private boolean isGasPressed = false;
    private boolean isBrakePressed = false;
    private boolean isTiltLeftPressed = false;
    private boolean isTiltRightPressed = false;

    // Game Control States
    private boolean isPlaying = false;
    private boolean isGameOver = false;

    // Game Loop Handler
    private Handler gameHandler;
    private Runnable gameRunnable;
    private static final int FPS_DELAY = 16; // ~60fps

    // Graphics Paints
    private Paint skyPaint;
    private Paint terrainPaint;
    private Paint terrainEdgePaint;
    private Paint sunPaint;
    private Paint mountainPaint;
    private Paint bikePaint;
    private Paint wheelPaint;
    private Paint wheelSpokePaint;
    private Paint coinPaint;
    private Paint coinInnerPaint;
    private Paint gasPaint;
    private Paint gasDetailPaint;
    private Paint textPaint;

    // Objects
    private ArrayList<Collectible> collectibles;

    // Helper classes
    static class Collectible {
        static final int TYPE_COIN = 0;
        static final int TYPE_GAS = 1;

        int type;
        float x;
        float y;
        boolean collected;

        Collectible(int type, float x, float y) {
            this.type = type;
            this.x = x;
            this.y = y;
            this.collected = false;
        }
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initPaints();
        gameHandler = new Handler(Looper.getMainLooper());
        gameRunnable = new Runnable() {
            @Override
            public void run() {
                if (isPlaying && !isGameOver) {
                    updatePhysics();
                    invalidate();
                    gameHandler.postDelayed(this, FPS_DELAY);
                }
            }
        };
    }

    public void setGameListener(GameListener listener) {
        this.gameListener = listener;
    }

    private void initPaints() {
        skyPaint = new Paint();
        skyPaint.setStyle(Paint.Style.FILL);

        terrainPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        terrainPaint.setStyle(Paint.Style.FILL);

        terrainEdgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        terrainEdgePaint.setStyle(Paint.Style.STROKE);
        terrainEdgePaint.setStrokeWidth(8f);
        terrainEdgePaint.setColor(0xFF22D3EE); // Glow Cyan

        sunPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        sunPaint.setStyle(Paint.Style.FILL);

        mountainPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mountainPaint.setStyle(Paint.Style.FILL);

        bikePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bikePaint.setStyle(Paint.Style.STROKE);
        bikePaint.setStrokeWidth(6f);
        bikePaint.setColor(0xFF06B6D4); // Neon Cyan

        wheelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        wheelPaint.setStyle(Paint.Style.STROKE);
        wheelPaint.setStrokeWidth(8f);
        wheelPaint.setColor(0xFFF97316); // Neon Orange

        wheelSpokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        wheelSpokePaint.setStyle(Paint.Style.STROKE);
        wheelSpokePaint.setStrokeWidth(3f);
        wheelSpokePaint.setColor(0xFFF97316);

        coinPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        coinPaint.setStyle(Paint.Style.FILL);
        coinPaint.setColor(0xFFFBBF24); // Gold

        coinInnerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        coinInnerPaint.setStyle(Paint.Style.STROKE);
        coinInnerPaint.setStrokeWidth(3f);
        coinInnerPaint.setColor(0xFF78350F);

        gasPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gasPaint.setStyle(Paint.Style.FILL);
        gasPaint.setColor(0xFFEF4444); // Red

        gasDetailPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gasDetailPaint.setColor(Color.WHITE);
        gasDetailPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(14f);
        textPaint.setStyle(Paint.Style.FILL);
    }

    public void initGame() {
        bikeX = 150f;
        bikeY = getTerrainHeight(bikeX) - 60f;
        vx = 0f;
        vy = 0f;
        bikeAngle = 0f;
        angularVelocity = 0f;
        fuel = 100f;
        coinsCollected = 0;
        cameraX = 0f;
        isGameOver = false;
        isPlaying = true;

        // Generate collectibles
        collectibles = new ArrayList<Collectible>();
        for (float x = 500f; x < TRACK_LENGTH - 500f; x += 180f) {
            float gy = getTerrainHeight(x);
            // Place fuel cans every 1400px
            if ((int)(x / 1400) != (int)((x - 180) / 1400)) {
                collectibles.add(new Collectible(Collectible.TYPE_GAS, x, gy - 50f));
            } else {
                collectibles.add(new Collectible(Collectible.TYPE_COIN, x, gy - 40f));
            } 
        }

        gameHandler.removeCallbacks(gameRunnable);
        gameHandler.post(gameRunnable);
    }

    public void pauseGame() {
        isPlaying = false;
        gameHandler.removeCallbacks(gameRunnable);
    }

    // Input state updates from Activity
    public void setGasPressed(boolean pressed) { this.isGasPressed = pressed; }
    public void setBrakePressed(boolean pressed) { this.isBrakePressed = pressed; }
    public void setTiltLeftPressed(boolean pressed) { this.isTiltLeftPressed = pressed; }
    public void setTiltRightPressed(boolean pressed) { this.isTiltRightPressed = pressed; }

    // Hilly terrain generator utilizing cosine interpolation
    private float getTerrainHeight(float x) {
        if (x < 0) return 1200f;
        float x1 = (float) Math.floor(x / SEGMENT_WIDTH) * SEGMENT_WIDTH;
        float x2 = x1 + SEGMENT_WIDTH;
        int index1 = (int) (x1 / SEGMENT_WIDTH);
        int index2 = index1 + 1;

        float y1 = getTerrainControlHeight(index1);
        float y2 = getTerrainControlHeight(index2);

        float t = (x - x1) / SEGMENT_WIDTH;
        float t2 = (float) (1.0 - Math.cos(t * Math.PI)) / 2.0f;
        return y1 * (1.0f - t2) + y2 * t2;
    }

    private float getTerrainControlHeight(int index) {
        if (index < 0) return 1200f;
        if (index < 4) return 1100f; // Flat starting area

        // Deterministic pseudorandom hill generation
        double angle = index * 0.45;
        double bigHills = Math.sin(index * 0.12) * 220f;
        double bumps = Math.cos(index * 0.75) * 45f;
        
        // Custom steep slopes/ramps at specific stretches
        double extraObstacle = 0;
        if (index > 15 && index < 20) {
            extraObstacle = -120f; // ramp up
        } else if (index >= 35 && index < 42) {
            extraObstacle = -200f; // high plateau
        } else if (index >= 65 && index < 72) {
            extraObstacle = 100f; // steep canyon
        } else if (index >= 110 && index < 120) {
            extraObstacle = -250f; // ultimate jump
        }

        // Base elevation height relative to screen sizes (around 1200)
        float baseHeight = 1100f;
        return (float) (baseHeight + bigHills + bumps + extraObstacle);
    }

    private void updatePhysics() {
        if (!isPlaying || isGameOver) return;

        // Fuel depletion
        fuel -= 0.07f;
        if (fuel <= 0) {
            fuel = 0;
            triggerGameOver("OUT OF FUEL!");
            return;
        }

        // Wheel horizontal layout values relative to frame
        float r_offset_x = -45f;
        float r_offset_y = 15f;
        float f_offset_x = 45f;
        float f_offset_y = 15f;

        float cosA = (float) Math.cos(bikeAngle);
        float sinA = (float) Math.sin(bikeAngle);

        // Compute actual wheel coordinates in world space
        float rx = bikeX + (r_offset_x * cosA - r_offset_y * sinA);
        float ry = bikeY + (r_offset_x * sinA + r_offset_y * cosA);
        float fx = bikeX + (f_offset_x * cosA - f_offset_y * sinA);
        float fy = bikeY + (f_offset_x * sinA + f_offset_y * cosA);

        float r_ground_y = getTerrainHeight(rx);
        float f_ground_y = getTerrainHeight(fx);

        boolean rearGrounded = (ry >= r_ground_y - 24f);
        boolean frontGrounded = (fy >= f_ground_y - 24f);
        boolean grounded = rearGrounded || frontGrounded;

        // Apply control forces
        if (isGasPressed) {
            if (grounded) {
                vx += 0.38f * cosA;
                vy += 0.38f * sinA;
            } else {
                // minor in-air control rotation
                angularVelocity += 0.003f;
            }
        }
        if (isBrakePressed) {
            if (grounded) {
                vx -= 0.28f * cosA;
                vy -= 0.28f * sinA;
            } else {
                // minor in-air control rotation
                angularVelocity -= 0.003f;
            }
        }

        // In-air balancing/tilting inputs
        if (isTiltLeftPressed) {
            angularVelocity -= 0.006f;
        }
        if (isTiltRightPressed) {
            angularVelocity += 0.006f;
        }

        // Apply general physics/gravity
        vy += GRAVITY;

        // Update positions
        bikeX += vx;
        bikeY += vy;

        // Recompute world coordinates for grounded checks after updating position
        rx = bikeX + (r_offset_x * cosA - r_offset_y * sinA);
        ry = bikeY + (r_offset_x * sinA + r_offset_y * cosA);
        fx = bikeX + (f_offset_x * cosA - f_offset_y * sinA);
        fy = bikeY + (f_offset_x * sinA + f_offset_y * cosA);

        r_ground_y = getTerrainHeight(rx);
        f_ground_y = getTerrainHeight(fx);

        // Ground collisions and positioning resolution
        if (ry > r_ground_y - 24f) {
            float depth = ry - (r_ground_y - 24f);
            bikeY -= depth * 0.5f;
            vy = -1.5f;
            vx *= 0.98f; // Ground friction
            rearGrounded = true;
        }

        if (fy > f_ground_y - 24f) {
            float depth = fy - (f_ground_y - 24f);
            bikeY -= depth * 0.5f;
            vy = -1.5f;
            vx *= 0.98f;
            frontGrounded = true;
        }

        grounded = rearGrounded || frontGrounded;

        if (grounded) {
            // Align bike structure towards terrain slope angle smoothly
            float slopeAngle = (float) Math.atan2(f_ground_y - r_ground_y, 90.0f);
            bikeAngle = bikeAngle + (slopeAngle - bikeAngle) * 0.18f;
            angularVelocity *= 0.6f; // Dampen spin
        } else {
            // In-air damping and application of air rotation
            angularVelocity *= 0.96f;
            bikeAngle += angularVelocity;
        }

        // Check crash conditions (rider head collision with hills)
        // Rider head offset relative to frame center
        float hx = bikeX - sinA * 55f;
        float hy = bikeY - cosA * 55f;
        if (hy >= getTerrainHeight(hx) - 8f) {
            triggerGameOver("CRASHED! Driver head flipped over!");
            return;
        }

        // Limit coordinates
        if (bikeX < 50f) {
            bikeX = 50f;
            vx = 0f;
        }

        // Level finish check
        if (bikeX >= TRACK_LENGTH - 300f) {
            triggerGameOverSuccess();
            return;
        }

        // Camera positioning tracking
        float targetCameraX = bikeX - (viewWidth / 3.5f);
        cameraX = cameraX + (targetCameraX - cameraX) * 0.12f;
        if (cameraX < 0) cameraX = 0f;
        if (cameraX > TRACK_LENGTH - viewWidth) cameraX = TRACK_LENGTH - viewWidth;

        // Check items pickup collision
        for (int i = 0; i < collectibles.size(); i++) {
            Collectible item = collectibles.get(i);
            if (!item.collected) {
                float dx = bikeX - item.x;
                float dy = bikeY - item.y;
                float distSq = dx * dx + dy * dy;
                if (distSq < 4800f) { // approx 69px distance
                    item.collected = true;
                    if (item.type == Collectible.TYPE_COIN) {
                        coinsCollected += 10;
                    } else if (item.type == Collectible.TYPE_GAS) {
                        fuel = 100f;
                    }
                }
            }
        }

        // Update activity HUD
        if (gameListener != null) {
            gameListener.onUpdateHud(bikeX / 10f, coinsCollected, fuel);
        }
    }

    private void triggerGameOver(String reason) {
        isGameOver = true;
        isPlaying = false;
        if (gameListener != null) {
            gameListener.onGameOver(reason, coinsCollected, bikeX / 10f, false);
        }
    }

    private void triggerGameOverSuccess() {
        isGameOver = true;
        isPlaying = false;
        if (gameListener != null) {
            gameListener.onGameOver("FINISHED!", coinsCollected, bikeX / 10f, true);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        viewWidth = w;
        viewHeight = h;

        // Create sky background dynamic sunset gradient
        skyPaint.setShader(new LinearGradient(0f, 0f, 0f, viewHeight,
                new int[]{0xFF2E0854, 0xFF120C3E, 0xFFE11D48},
                null, Shader.TileMode.CLAMP));

        // Create deep mountain parallax colors
        mountainPaint.setShader(new LinearGradient(0f, viewHeight * 0.4f, 0f, viewHeight,
                new int[]{0xFF1E1B4B, 0xFF0F172A},
                null, Shader.TileMode.CLAMP));

        // Create hill foreground gradient
        terrainPaint.setShader(new LinearGradient(0f, viewHeight * 0.5f, 0f, viewHeight,
                new int[]{0xFF020617, 0xFF0B1329},
                null, Shader.TileMode.CLAMP));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 1. Draw Sky Background
        canvas.drawRect(0f, 0f, viewWidth, viewHeight, skyPaint);

        // 2. Draw Retro Glowing Sun in Background (with parallax)
        float sunX = viewWidth * 0.75f - (cameraX * 0.03f) % viewWidth;
        float sunY = viewHeight * 0.35f;
        sunPaint.setColor(0xFFF59E0B);
        canvas.drawCircle(sunX, sunY, 110f, sunPaint);

        // Draw glowing neon sunset bands inside the sun
        sunPaint.setColor(0xFFE11D48);
        for (float h = sunY + 20f; h < sunY + 110f; h += 25f) {
            canvas.drawRect(sunX - 110f, h, sunX + 110f, h + 8f, sunPaint);
        }

        // 3. Draw Parallax Background Mountains
        Path mountPath = new Path();
        mountPath.moveTo(0f, viewHeight);
        float stepMount = viewWidth / 4f;
        for (int i = 0; i <= 5; i++) {
            float mx = i * stepMount;
            // Wave generated dynamically relative to camera displacement
            float my = viewHeight * 0.65f + (float) Math.sin((i + cameraX * 0.0003f) * 2f) * 50f;
            if (i == 0) {
                mountPath.moveTo(mx, my);
            } else {
                mountPath.lineTo(mx, my);
            }
        }
        mountPath.lineTo(viewWidth, viewHeight);
        mountPath.close();
        canvas.drawPath(mountPath, mountainPaint);

        // 4. Draw Foreground Hills (Terrain path relative to camera)
        canvas.save();
        canvas.translate(-cameraX, 0f);

        Path hillPath = new Path();
        hillPath.moveTo(cameraX, viewHeight);
        
        float startX = cameraX - 100f;
        if (startX < 0) startX = 0f;
        float endX = cameraX + viewWidth + 100f;
        if (endX > TRACK_LENGTH) endX = TRACK_LENGTH;

        hillPath.lineTo(startX, getTerrainHeight(startX));
        for (float px = startX; px <= endX; px += 15f) {
            hillPath.lineTo(px, getTerrainHeight(px));
        }
        hillPath.lineTo(endX, viewHeight);
        hillPath.close();
        canvas.drawPath(hillPath, terrainPaint);

        // Draw the neon cyber edge of the hills
        Path edgePath = new Path();
        edgePath.moveTo(startX, getTerrainHeight(startX));
        for (float px = startX; px <= endX; px += 15f) {
            edgePath.lineTo(px, getTerrainHeight(px));
        }
        canvas.drawPath(edgePath, terrainEdgePaint);

        // 5. Draw Collectibles
        for (int i = 0; i < collectibles.size(); i++) {
            Collectible item = collectibles.get(i);
            if (!item.collected && item.x >= startX && item.x <= endX) {
                if (item.type == Collectible.TYPE_COIN) {
                    // Golden glowing coin
                    canvas.drawCircle(item.x, item.y, 16f, coinPaint);
                    canvas.drawCircle(item.x, item.y, 11f, coinInnerPaint);
                } else if (item.type == Collectible.TYPE_GAS) {
                    // Red dynamic jerrycan container
                    canvas.drawRect(item.x - 14f, item.y - 20f, item.x + 14f, item.y + 14f, gasPaint);
                    canvas.drawRect(item.x - 6f, item.y - 25f, item.x + 6f, item.y - 20f, gasDetailPaint);
                    // Draw horizontal tag stripes on the canister
                    canvas.drawRect(item.x - 10f, item.y - 6f, item.x + 10f, item.y - 1f, gasDetailPaint);
                }
            }
        }

        // 6. Draw Finish Checkered Flag Structure
        float finishLineX = TRACK_LENGTH - 300f;
        if (finishLineX >= startX && finishLineX <= endX) {
            float f_ground = getTerrainHeight(finishLineX);
            // Draw poles
            Paint polePaint = new Paint();
            polePaint.setColor(Color.WHITE);
            polePaint.setStrokeWidth(6f);
            canvas.drawLine(finishLineX, f_ground, finishLineX, f_ground - 150f, polePaint);

            // Draw Checkered rectangular grid
            Paint checkeredPaint = new Paint();
            boolean alt = false;
            for (float r = f_ground - 150f; r < f_ground - 90f; r += 15f) {
                for (float c = finishLineX; c < finishLineX + 60f; c += 15f) {
                    checkeredPaint.setColor(alt ? Color.BLACK : Color.WHITE);
                    canvas.drawRect(c, r, c + 15f, r + 15f, checkeredPaint);
                    alt = !alt;
                }
                alt = !alt;
            }
        }

        // 7. Draw Bike and Rider
        drawBike(canvas);

        canvas.restore();
    }

    private void drawBike(Canvas canvas) {
        float cosA = (float) Math.cos(bikeAngle);
        float sinA = (float) Math.sin(bikeAngle);

        float r_offset_x = -45f;
        float r_offset_y = 15f;
        float f_offset_x = 45f;
        float f_offset_y = 15f;

        // Computed coordinates for components
        float rx = bikeX + (r_offset_x * cosA - r_offset_y * sinA);
        float ry = bikeY + (r_offset_x * sinA + r_offset_y * cosA);

        float fx = bikeX + (f_offset_x * cosA - f_offset_y * sinA);
        float fy = bikeY + (f_offset_x * sinA + f_offset_y * cosA);

        // Center seat coordinates of chassis
        float cx = bikeX - sinA * 24f;
        float cy = bikeY + cosA * 24f;

        // Handlebars position
        float hbx = bikeX + (24f * cosA - 35f * sinA);
        float hby = bikeY + (24f * sinA + 35f * cosA);

        // Draw chassis connection rods (Frame struts)
        canvas.drawLine(rx, ry, cx, cy, bikePaint);
        canvas.drawLine(fx, fy, cx, cy, bikePaint);
        canvas.drawLine(cx, cy, hbx, hby, bikePaint);
        canvas.drawLine(fx, fy, hbx, hby, bikePaint);

        // Draw Rider figure
        Paint riderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        riderPaint.setColor(Color.WHITE);
        riderPaint.setStrokeWidth(5f);
        riderPaint.setStyle(Paint.Style.STROKE);

        // Seat point (hips)
        float hipsX = cx - cosA * 10f;
        float hipsY = cy - sinA * 10f;

        // Torso/Back
        float shoulderX = hipsX - sinA * 30f + cosA * 10f;
        float shoulderY = hipsY + cosA * 30f + sinA * 10f;
        canvas.drawLine(hipsX, hipsY, shoulderX, shoulderY, riderPaint);

        // Head/Helmet
        float headX = shoulderX - sinA * 14f;
        float headY = shoulderY + cosA * 14f;
        Paint helmetPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        helmetPaint.setColor(0xFF22D3EE); // Neon Cyber-Helmet
        helmetPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(headX, headY, 11f, helmetPaint);

        // Arms holding handle bars
        canvas.drawLine(shoulderX, shoulderY, hbx, hby, riderPaint);

        // Legs/Pedal connection
        float footX = cx + 10f * cosA + 10f * sinA;
        float footY = cy + 10f * sinA - 10f * cosA;
        canvas.drawLine(hipsX, hipsY, footX, footY, riderPaint);

        // Draw wheels with rotating spokes
        float rotationAngle = bikeX / 24f; // Spin rate relative to forward displacement
        drawWheel(canvas, rx, ry, rotationAngle);
        drawWheel(canvas, fx, fy, rotationAngle);
    }

    private void drawWheel(Canvas canvas, float wx, float wy, float spin) {
        // Draw outer thick rubber tyre
        canvas.drawCircle(wx, wy, 24f, wheelPaint);

        // Draw central hub
        Paint hubPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        hubPaint.setColor(Color.WHITE);
        hubPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(wx, wy, 6f, hubPaint);

        // Draw spokes rotating
        for (int angleDeg = 0; angleDeg < 360; angleDeg += 60) {
            double rad = Math.toRadians(angleDeg) + spin;
            float sx = wx + (float) Math.cos(rad) * 24f;
            float sy = wy + (float) Math.sin(rad) * 24f;
            canvas.drawLine(wx, wy, sx, sy, wheelSpokePaint);
        }
    }
}