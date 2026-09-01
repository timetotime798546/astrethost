package com.retro3dturboracer.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends Activity {

    private GameView gameView;
    private FrameLayout container;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        container = (FrameLayout) findViewById(R.id.game_container);
        gameView = new GameView(this);
        container.addView(gameView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gameView != null) {
            gameView.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (gameView != null) {
            gameView.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (gameView != null) {
            gameView.release();
        }
    }

    // Game Sub-Systems Container representing high quality pseudo-3D
    public static class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable, SensorEventListener {

        // Game States
        private static final int STATE_MENU = 0;
        private static final int STATE_PLAYING = 1;
        private static final int STATE_PAUSED = 2;
        private static final int STATE_GAMEOVER = 3;
        private static final int STATE_HIGHSCORES = 4;

        private int gameState = STATE_MENU;

        // Engine Loop Parameters
        private Thread gameThread = null;
        private boolean running = false;
        private SurfaceHolder holder;
        private Canvas canvas;
        private Paint paint;

        // Screen Size Dimensions
        private int width = 800;
        private int height = 480;

        // Math Scale Projection Parameters
        private final double CAMERA_HEIGHT = 1000.0;
        private final double ROAD_WIDTH = 2000.0;
        private final double CAMERA_DEPTH = 0.8; // Perspective scaling
        private final int SEGMENT_LENGTH = 200;
        private final int DRAW_DISTANCE = 300;

        // Game Track Database
        private List<Segment> track = new ArrayList<>();
        private int trackLength = 0;

        // Player State Variables
        private double playerZ = 0; 
        private double playerX = 0; // -1.0 to 1.0 (limits of standard road lane)
        private double playerSpeed = 0;
        private final double MAX_SPEED = 220; // MPH
        private final double TURBO_SPEED = 300;
        private double targetSpeed = 0;
        private int score = 0;
        private double fuel = 100.0; // Starts at 100%
        private int coins = 0;
        private boolean turboActive = false;
        private double turboTime = 0;
        private boolean firstPersonCam = false;

        // Selected Car Colors
        private int carColorIdx = 0;
        private final int[] carColors = { Color.RED, Color.BLUE, Color.YELLOW, Color.GREEN, Color.CYAN };
        private final String[] carColorNames = { "RED DEMON", "BLUE FLASH", "YELLOW VOLT", "GREEN VIPER", "NEON CYCLONE" };

        // Control Systems
        private boolean controlUseTilt = false;
        private double steerX = 0; // current active horizontal input (-1.0 to 1.0)
        private float steerWheelAngle = 0; // visual aspect
        private boolean acceleratePressed = false;
        private boolean brakePressed = false;

        // Background Parallax Layer offsets
        private double skyOffset = 0;
        private double hillsOffset = 0;

        // Sensor & Haptic Feedback
        private SensorManager sensorManager;
        private Sensor accelerometer;
        private Vibrator vibrator;

        // Synthesized Engine Sound system
        private AudioTrack audioTrack;
        private Thread audioThread;
        private boolean soundActive = false;

        // Enemy Traffic Cars
        private List<TrafficCar> traffic = new ArrayList<>();
        private final int NUM_TRAFFIC = 30;

        // High Scores
        private int highScore = 0;
        private SharedPreferences prefs;

        // Custom UI Floating Texts
        private class FloatingText {
            String text;
            float x, y;
            int color;
            int timer;
        }
        private List<FloatingText> floatingTexts = new ArrayList<>();

        // Static structures for Segment and Game Objects
        public static class Segment {
            int index;
            double worldX, worldY, worldZ;
            double curve;
            double hill;
            int colorStyle; // 0=Desert, 1=Forest, 2=Snowy, 3=Cyber
            List<GameObject> objects = new ArrayList<>();
            
            // Projected Coordinates cached per frame
            double p1ScreenX, p1ScreenY, p1ScreenWidth;
            double p2ScreenX, p2ScreenY, p2ScreenWidth;
        }

        public static class GameObject {
            static final int TYPE_TREE = 1;
            static final int TYPE_ROCK = 2;
            static final int TYPE_CACTUS = 3;
            static final int TYPE_SNOW_TREE = 4;
            static final int TYPE_CITY_LAMP = 5;
            static final int TYPE_FUEL = 6;
            static final int TYPE_COIN = 7;
            static final int TYPE_FINISH = 8;

            int type;
            double offsetX; // offset relative to road center (-1.5 to 1.5)
            boolean collected = false;

            GameObject(int type, double offsetX) {
                this.type = type;
                this.offsetX = offsetX;
            }
        }

        public static class TrafficCar {
            double z;
            double offsetX;
            double speed;
            int color;
            boolean isOvertaken = false;
            int animFrame = 0;
        }

        public GameView(Context context) {
            super(context);
            holder = getHolder();
            holder.addCallback(this);
            paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setTextSize(18f);

            vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            if (sensorManager != null) {
                accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            }

            prefs = context.getSharedPreferences("retro_racer_scores", Context.MODE_PRIVATE);
            highScore = prefs.getInt("highscore", 12500);

            generateTrack();
            spawnTraffic();
        }

        // Generates completely varied procedural retro 3D race courses
        private void generateTrack() {
            track.clear();
            double currentX = 0;
            double currentY = 0;

            for (int i = 0; i < 2000; i++) {
                Segment s = new Segment();
                s.index = i;
                s.worldZ = i * SEGMENT_LENGTH;

                // Select Scenery Theme Styles based on progress
                if (i < 500) {
                    s.colorStyle = 0; // Desert Canyon Theme
                } else if (i < 1000) {
                    s.colorStyle = 1; // Green Highlands / Forest
                } else if (i < 1500) {
                    s.colorStyle = 2; // Frozen Snowy Slopes
                } else {
                    s.colorStyle = 3; // Cyberpunk Grid City
                }

                // Curves Generation Math
                if (i > 100 && i < 200) s.curve = 2.0;       // Light curve right
                else if (i > 250 && i < 350) s.curve = -3.0; // Mid curve left
                else if (i > 520 && i < 680) s.curve = 4.5;  // Hard curve right
                else if (i > 720 && i < 860) s.curve = -4.0; // Hard curve left
                else if (i > 1050 && i < 1180) s.curve = 3.0;
                else if (i > 1250 && i < 1350) s.curve = -2.5;
                else if (i > 1550 && i < 1800) {
                    s.curve = Math.sin(i / 15.0) * 5.0;      // Challenging Hairpin S-Curves
                }

                // Elevation and Hill Generation Math
                if (i > 300 && i < 450) s.hill = Math.sin((i - 300) / 150.0 * Math.PI) * 500;  // High crest
                else if (i > 600 && i < 800) s.hill = -Math.sin((i - 600) / 200.0 * Math.PI) * 400; // Dip
                else if (i > 1100 && i < 1350) s.hill = Math.sin((i - 1100) / 250.0 * Math.PI) * 700; // Giant mountain
                else if (i > 1600 && i < 1900) s.hill = Math.cos(i / 30.0) * 300; // Continuous undulating roller hills

                currentX += s.curve * 12;
                currentY += s.hill / 20;
                s.worldX = currentX;
                s.worldY = currentY;

                // Add Roadside Sprites
                if (i % 6 == 0) {
                    double side = (i % 12 == 0) ? -2.0 : 2.0;
                    if (s.colorStyle == 0) { 
                        s.objects.add(new GameObject(GameObject.TYPE_CACTUS, side));
                        if (Math.random() < 0.2) s.objects.add(new GameObject(GameObject.TYPE_ROCK, -side * 1.5));
                    } else if (s.colorStyle == 1) {
                        s.objects.add(new GameObject(GameObject.TYPE_TREE, side));
                        s.objects.add(new GameObject(GameObject.TYPE_TREE, -side * 1.4));
                    } else if (s.colorStyle == 2) {
                        s.objects.add(new GameObject(GameObject.TYPE_SNOW_TREE, side));
                    } else {
                        s.objects.add(new GameObject(GameObject.TYPE_CITY_LAMP, side));
                        s.objects.add(new GameObject(GameObject.TYPE_CITY_LAMP, -side));
                    }
                }

                // Add Fuel Refills & Coins placement
                if (i > 40 && i % 40 == 0) {
                    s.objects.add(new GameObject(GameObject.TYPE_FUEL, (Math.random() * 1.2) - 0.6));
                }
                if (i > 20 && i % 15 == 0) {
                    s.objects.add(new GameObject(GameObject.TYPE_COIN, (Math.random() * 1.4) - 0.7));
                }

                // Set Finish Line Object near index 1950
                if (i == 1950) {
                    s.objects.add(new GameObject(GameObject.TYPE_FINISH, 0.0));
                }

                track.add(s);
            }
            trackLength = track.size();
        }

        // Creates obstacle vehicle traffic at varying positions
        private void spawnTraffic() {
            traffic.clear();
            Random r = new Random();
            int[] trafficCarColors = { Color.GRAY, Color.MAGENTA, Color.WHITE, Color.rgb(255,140,0), Color.rgb(75,0,130) };

            for (int i = 0; i < NUM_TRAFFIC; i++) {
                TrafficCar tc = new TrafficCar();
                // Distribute evenly along road starting from segment 100
                tc.z = 100 * SEGMENT_LENGTH + i * ((1800 * SEGMENT_LENGTH) / NUM_TRAFFIC) + r.nextInt(1000);
                tc.offsetX = -0.6 + (r.nextDouble() * 1.2);
                tc.speed = 50 + r.nextInt(60); // 50 to 110 mph
                tc.color = trafficCarColors[r.nextInt(trafficCarColors.length)];
                traffic.add(tc);
            }
        }

        // Triggers clean custom sound generation on background thread
        private void startSynthesizedSounds() {
            if (soundActive) return;
            soundActive = true;
            audioThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    int minSize = AudioTrack.getMinBufferSize(11025, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_8BIT);
                    audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 11025, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_8BIT, Math.max(minSize, 2048), AudioTrack.MODE_STREAM);
                    
                    try {
                        audioTrack.play();
                    } catch (Exception e) {
                        return;
                    }

                    byte[] buffer = new byte[512];
                    double cyclePhase = 0;

                    while (soundActive) {
                        if (gameState == STATE_PLAYING) {
                            // Base motor pitch scaled proportionally to speed + RPM sound factor
                            double baseFreq = 45.0 + (playerSpeed / MAX_SPEED) * 110.0;
                            // Simulate high pitch engine gear shifting mechanics
                            if (playerSpeed > 50 && playerSpeed < 90) baseFreq -= 25.0;
                            if (playerSpeed >= 90 && playerSpeed < 140) baseFreq -= 35.0;
                            if (playerSpeed >= 140) baseFreq -= 40.0;
                            if (turboActive) baseFreq += 20.0; // Aggressive scream

                            double step = (2.0 * Math.PI * baseFreq) / 11025.0;
                            for (int i = 0; i < buffer.length; i++) {
                                cyclePhase += step;
                                // Multi-harmonic raw engine sound output synthesis
                                double sampleValue = Math.sin(cyclePhase) * 0.5 + Math.sin(cyclePhase * 2.5) * 0.3 + (Math.random() - 0.5) * 0.12;
                                buffer[i] = (byte) ((sampleValue * 127) + 128);
                            }
                        } else {
                            // Low engine hum on title screen
                            double step = (2.0 * Math.PI * 40.0) / 11025.0;
                            for (int i = 0; i < buffer.length; i++) {
                                cyclePhase += step;
                                double sampleValue = Math.sin(cyclePhase) * 0.3 + (Math.random() - 0.5) * 0.05;
                                buffer[i] = (byte) ((sampleValue * 127) + 128);
                            }
                        }
                        try {
                            audioTrack.write(buffer, 0, buffer.length);
                            Thread.sleep(15);
                        } catch (Exception e) {
                            break;
                        }
                    }
                }
            });
            audioThread.start();
        }

        private void stopSynthesizedSounds() {
            soundActive = false;
            if (audioTrack != null) {
                try {
                    audioTrack.stop();
                    audioTrack.release();
                } catch (Exception ignored) {}
                audioTrack = null;
            }
            if (audioThread != null) {
                try {
                    audioThread.join(200);
                } catch (Exception ignored) {}
                audioThread = null;
            }
        }

        // Master loop runner
        @Override
        public void run() {
            long lastTime = System.currentTimeMillis();
            startSynthesizedSounds();

            while (running) {
                if (!holder.getSurface().isValid()) continue;

                long now = System.currentTimeMillis();
                double dt = (now - lastTime) / 1000.0;
                lastTime = now;

                if (dt > 0.1) dt = 0.1; // Cap delta time spikes

                update(dt);
                drawFrame();

                try {
                    Thread.sleep(16);
                } catch (InterruptedException e) {}
            }
        }

        private void update(double dt) {
            // Update floating texts
            for (int i = floatingTexts.size() - 1; i >= 0; i--) {
                FloatingText ft = floatingTexts.get(i);
                ft.y -= 120 * dt;
                ft.timer--;
                if (ft.timer <= 0) {
                    floatingTexts.remove(i);
                }
            }

            if (gameState == STATE_PLAYING) {
                // Fuel Consumption
                if (!turboActive) {
                    fuel -= 2.2 * dt;
                } else {
                    fuel -= 4.5 * dt; // Turbo consumes massive fuel!
                }
                if (fuel <= 0) {
                    fuel = 0;
                    targetSpeed = 0; // Coast to stop
                }

                // Steering Inputs
                if (controlUseTilt) {
                    // Accelerometer inputs
                } else {
                    // Visual Steering wheel return math
                    if (steerX > 0) steerWheelAngle += 15.0f; 
                    else if (steerX < 0) steerWheelAngle -= 15.0f;
                    else steerWheelAngle *= 0.85f;
                    if (steerWheelAngle > 90f) steerWheelAngle = 90f;
                    if (steerWheelAngle < -90f) steerWheelAngle = -90f;
                }

                // Throttle / Braking Physics
                double activeMaxSpeed = turboActive ? TURBO_SPEED : MAX_SPEED;
                if (fuel <= 0) activeMaxSpeed = 0;

                if (acceleratePressed && fuel > 0) {
                    targetSpeed = activeMaxSpeed;
                    playerSpeed = playerSpeed + (80 * dt); // Acceleration rate
                } else if (brakePressed) {
                    targetSpeed = 0;
                    playerSpeed = playerSpeed - (180 * dt); // Strong braking deceleration
                } else {
                    targetSpeed = 0;
                    playerSpeed = playerSpeed - (35 * dt); // Engine drag deceleration
                }

                // Lock Speed parameters
                if (playerSpeed > activeMaxSpeed) playerSpeed = activeMaxSpeed;
                if (playerSpeed < 0) playerSpeed = 0;

                // Drive progress on Z road
                playerZ += playerSpeed * 40 * dt;
                score += (int) (playerSpeed * dt * 0.5);

                // Segment calculation
                int currentSegIdx = (int) (playerZ / SEGMENT_LENGTH);
                Segment currentSeg = getSegment(currentSegIdx);

                // Steer response and Centrifugal Lateral forces on Curves
                playerX += steerX * 2.2 * dt * (playerSpeed / 100.0);
                if (playerSpeed > 10) {
                    // Pushed outwards on curves
                    playerX -= currentSeg.curve * 0.15 * (playerSpeed / MAX_SPEED) * dt;
                }

                // Left / Right bounds and extreme grass friction physics
                if (playerX < -1.0 || playerX > 1.0) {
                    // Slow down drastically and create physical rumbles
                    if (playerSpeed > 50) {
                        playerSpeed = playerSpeed - (120 * dt);
                        if (Math.random() < 0.3) triggerVibration(25);
                    }
                    // Scrape roadside crash penalties
                    if (playerX < -1.8) {
                        playerX = -1.8;
                        playerSpeed = 20;
                    }
                    if (playerX > 1.8) {
                        playerX = 1.8;
                        playerSpeed = 20;
                    }
                }

                // Parallax layers update
                skyOffset -= currentSeg.curve * 0.08 * (playerSpeed / MAX_SPEED);
                hillsOffset -= currentSeg.curve * 0.15 * (playerSpeed / MAX_SPEED);

                // Turbo mechanics
                if (turboActive) {
                    turboTime -= dt;
                    if (turboTime <= 0) {
                        turboActive = false;
                    }
                }

                // Checkpoint check
                if (playerZ >= (trackLength - 50) * SEGMENT_LENGTH) {
                    // Game Finished, loop it or state winner!
                    gameState = STATE_GAMEOVER;
                    saveHighScore();
                }

                // Update AI Enemy Traffic Movement
                for (int i = 0; i < traffic.size(); i++) {
                    TrafficCar tc = traffic.get(i);
                    tc.z += tc.speed * 40 * dt;
                    tc.animFrame = (tc.animFrame + 1) % 4;

                    // Loop traffic cars back if far behind player
                    if (tc.z < playerZ - 2000) {
                        tc.z = playerZ + (1800 * SEGMENT_LENGTH / NUM_TRAFFIC) + 500;
                        tc.isOvertaken = false;
                    }
                    // Recycle cars pushed extremely far ahead
                    if (tc.z > playerZ + 20000) {
                        tc.z = playerZ + 200;
                        tc.isOvertaken = false;
                    }

                    // Overtake Score points bonus
                    if (!tc.isOvertaken && tc.z < playerZ) {
                        tc.isOvertaken = true;
                        score += 500;
                        addFloatingText("OVERTAKE! +500", width / 2, height / 3, Color.YELLOW);
                    }

                    // Verify collisions with AI traffic cars
                    if (Math.abs(tc.z - playerZ) < 250) {
                        // Same segment region check lateral overlap
                        if (Math.abs(tc.offsetX - playerX) < 0.35) {
                            // Trigger severe crash mechanics
                            playerSpeed = tc.speed * 0.5;
                            triggerVibration(180);
                            addFloatingText("CAR IMPACT!", width / 2, height / 2, Color.RED);
                            tc.z += 1000; // Bounce AI traffic forward
                        }
                    }
                }

                // Verify collisions with static Objects & Collectibles on current segment
                int checkSegRange = (int)(playerZ / SEGMENT_LENGTH);
                for (int sIdx = checkSegRange; sIdx <= checkSegRange + 1; sIdx++) {
                    Segment seg = getSegment(sIdx);
                    for (GameObject go : seg.objects) {
                        if (go.collected) continue;
                        
                        // Check spatial alignment with item
                        if (Math.abs(seg.worldZ - playerZ) < 220) {
                            if (Math.abs(go.offsetX - playerX) < 0.35) {
                                // Handle Item Types
                                if (go.type == GameObject.TYPE_COIN) {
                                    go.collected = true;
                                    coins++;
                                    score += 250;
                                    triggerVibration(30);
                                    addFloatingText("GOLD +250", width / 2, height / 3, Color.YELLOW);
                                } else if (go.type == GameObject.TYPE_FUEL) {
                                    go.collected = true;
                                    fuel = Math.min(100.0, fuel + 25.0);
                                    triggerVibration(45);
                                    addFloatingText("FUEL SPEEDUP +25%", width / 2, height / 3, Color.GREEN);
                                    // Trigger instant brief turbo boost!
                                    turboActive = true;
                                    turboTime = 3.0;
                                } else if (go.type == GameObject.TYPE_FINISH) {
                                    gameState = STATE_GAMEOVER;
                                    saveHighScore();
                                }
                            } else if (Math.abs(go.offsetX - playerX) < 0.7 && go.type != GameObject.TYPE_COIN && go.type != GameObject.TYPE_FUEL) {
                                // Side crash with hard obstacles like trees, lamp posts or cacti
                                if (playerSpeed > 30) {
                                    playerSpeed = 10;
                                    triggerVibration(250);
                                    addFloatingText("ROAD CRASH! -SPEED", width / 2, height / 2, Color.RED);
                                    go.collected = true;
                                }
                            }
                        }
                    }
                }

                // Trigger game over if fuel runs completely out and speed drops to zero
                if (fuel <= 0 && playerSpeed <= 2) {
                    gameState = STATE_GAMEOVER;
                    saveHighScore();
                }
            }
        }

        private void saveHighScore() {
            if (score > highScore) {
                highScore = score;
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt("highscore", highScore);
                editor.apply();
            }
        }

        private void resetGame() {
            playerZ = 0;
            playerX = 0;
            playerSpeed = 0;
            score = 0;
            fuel = 100.0;
            coins = 0;
            turboActive = false;
            turboTime = 0;
            floatingTexts.clear();
            generateTrack();
            spawnTraffic();
        }

        private Segment getSegment(int index) {
            if (index < 0) return track.get(0);
            return track.get(index % trackLength);
        }

        // Retro Pseudo 3D Engine Projection Pipeline Graphics drawer
        private void drawFrame() {
            canvas = holder.lockCanvas();
            if (canvas == null) return;

            width = canvas.getWidth();
            height = canvas.getHeight();

            // Clear Screen Canvas
            canvas.drawColor(Color.BLACK);

            if (gameState == STATE_MENU) {
                drawMenu(canvas);
            } else if (gameState == STATE_PLAYING || gameState == STATE_PAUSED) {
                draw3DGamePlay(canvas);
                if (gameState == STATE_PAUSED) {
                    drawPauseScreen(canvas);
                }
            } else if (gameState == STATE_GAMEOVER) {
                drawGameOverScreen(canvas);
            } else if (gameState == STATE_HIGHSCORES) {
                drawHighscoresScreen(canvas);
            }

            // Draw custom floating texts on top of everything
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setFakeBoldText(true);
            for (FloatingText ft : floatingTexts) {
                paint.setColor(ft.color);
                paint.setTextSize(32f);
                canvas.drawText(ft.text, ft.x, ft.y, paint);
            }
            paint.setFakeBoldText(false);

            holder.unlockCanvasAndPost(canvas);
        }

        private void drawMenu(Canvas canvas) {
            // Draw Beautiful Retro Animated Space Sunset Parallax Sky
            int skyColorStart = Color.rgb(20, 10, 45);
            int skyColorEnd = Color.rgb(190, 40, 100);
            LinearGradient grad = new LinearGradient(0, 0, 0, height, skyColorStart, skyColorEnd, Shader.TileMode.CLAMP);
            paint.setShader(grad);
            canvas.drawRect(0, 0, width, height, paint);
            paint.setShader(null);

            // Draw glowing retro synth grid lines below center
            paint.setColor(Color.rgb(240, 0, 150));
            paint.setStrokeWidth(2f);
            int horizonY = height * 4 / 10;
            for (int i = 0; i < width; i += 40) {
                canvas.drawLine(i, horizonY, (i - width / 2) * 3 + width / 2, height, paint);
            }
            for (int j = horizonY; j < height; j += 25) {
                canvas.drawLine(0, j, width, j, paint);
            }

            // Giant title with dual retro neon orange/yellow fill glowing layers
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setFakeBoldText(true);
            paint.setTextSize(58f);
            paint.setColor(Color.rgb(0, 255, 230));
            canvas.drawText("RETRO 3D TURBO RACER", width / 2 + 3, height / 4 + 3, paint);
            paint.setColor(Color.rgb(255, 240, 0));
            canvas.drawText("RETRO 3D TURBO RACER", width / 2, height / 4, paint);

            // Play Buttons UI region boxes
            drawButton(canvas, "PRESS ACCEL TO START RACE", width / 2, height * 52 / 100, Color.GREEN);
            drawButton(canvas, "CAR COLOR: " + carColorNames[carColorIdx], width / 2, height * 68 / 100, Color.CYAN);
            drawButton(canvas, "CONTROLS: " + (controlUseTilt ? "TILT/ACCEL" : "ON-SCREEN PEDALS"), width / 2, height * 80 / 100, Color.WHITE);
            drawButton(canvas, "VIEW LEADERBOARD", width / 2, height * 90 / 100, Color.YELLOW);

            // Decorative Sports Car Vector in center
            drawVectorCar(canvas, width / 2, height * 43 / 100, 1.2, carColors[carColorIdx], 0);
        }

        private void draw3DGamePlay(Canvas canvas) {
            int currentSegIdx = (int) (playerZ / SEGMENT_LENGTH);
            double percentProgress = (playerZ % SEGMENT_LENGTH) / SEGMENT_LENGTH;
            Segment playerSeg = getSegment(currentSegIdx);
            double cameraY = CAMERA_HEIGHT + playerSeg.worldY;
            double cameraZ = playerZ;

            // Parallax Sky, Mountains & Backdrop Cityscapes
            drawBackgroundScenery(canvas, playerSeg.colorStyle);

            // Draw 3D road elements
            double maxSegmentsDraw = DRAW_DISTANCE;
            double cumulativeX = 0;
            double currentCurveSum = 0;

            // Back up initial relative scaling factors and loop
            for (int i = 0; i < maxSegmentsDraw; i++) {
                int sIdx = currentSegIdx + i;
                Segment curr = getSegment(sIdx);
                
                // Wrap loops around track limits
                double trackLoopZ = (sIdx >= trackLength) ? trackLength * SEGMENT_LENGTH : 0;
                
                // Project 3D geometry coordinates into 2D viewport coordinates
                project3D(curr, -playerX * ROAD_WIDTH - currentCurveSum, cameraY, cameraZ - trackLoopZ, curr.colorStyle);

                currentCurveSum += curr.curve * 12;
                
                // Draw standard road slices
                if (i > 0) {
                    Segment prev = getSegment(sIdx - 1);
                    if (curr.p1ScreenY < height && prev.p1ScreenY < height && curr.p1ScreenY > prev.p1ScreenY) {
                        // Skip rendering behind or culled segments
                        continue;
                    }
                    drawRoadSlice(canvas, prev, curr);
                }
            }

            // Render Objects and Sprites on top (Back-to-front Painter's algorithm)
            for (int i = (int) maxSegmentsDraw - 1; i >= 0; i--) {
                Segment curr = getSegment(currentSegIdx + i);
                double scale = curr.p1ScreenWidth / ROAD_WIDTH;

                // Draw Game Obstacles, pickups, decor
                for (GameObject go : curr.objects) {
                    if (!go.collected) {
                        double spriteX = curr.p1ScreenX + go.offsetX * curr.p1ScreenWidth;
                        double spriteY = curr.p1ScreenY;
                        drawSpriteObject(canvas, go.type, spriteX, spriteY, scale, curr.colorStyle);
                    }
                }

                // Draw AI vehicles on correct visual Z layers
                for (TrafficCar tc : traffic) {
                    int tcSegIdx = (int) (tc.z / SEGMENT_LENGTH);
                    if (tcSegIdx == curr.index) {
                        double relativeZ = tc.z - playerZ;
                        if (relativeZ > 0 && relativeZ < DRAW_DISTANCE * SEGMENT_LENGTH) {
                            double tcScale = CAMERA_DEPTH / relativeZ;
                            double tcScreenX = curr.p1ScreenX + (tc.offsetX - playerX) * curr.p1ScreenWidth;
                            double tcScreenY = curr.p1ScreenY;
                            drawVectorCar(canvas, (int) tcScreenX, (int) tcScreenY, tcScale * 800, tc.color, tc.animFrame);
                        }
                    }
                }
            }

            // Draw Player Vehicle (Third Person Chase Cam vs First Person Hood Cam)
            if (!firstPersonCam) {
                // Determine visual turn lean factor based on steer input
                int lean = 0;
                if (steerX < -0.2) lean = -1;
                if (steerX > 0.2) lean = 1;
                drawVectorCar(canvas, width / 2, height * 88 / 100, 1.8, carColors[carColorIdx], lean);
            } else {
                // Draw visual subtle front steering hood outline
                paint.setColor(carColors[carColorIdx]);
                canvas.drawRect(width / 4, height - 15, width * 3 / 4, height, paint);
            }

            // Draw Cockpit / Dashboard and Game controls
            drawDashboardControls(canvas);
        }

        // Projection math formula calculating 3D -> 2D
        private void project3D(Segment s, double translationX, double camY, double camZ, int theme) {
            double relativeZ = s.worldZ - camZ;
            if (relativeZ <= 0) {
                s.p1ScreenY = 0;
                return;
            }

            double scale = CAMERA_DEPTH / relativeZ;
            
            s.p1ScreenX = (width / 2) + (translationX * scale * width / 2);
            s.p1ScreenY = (height / 2) - ((s.worldY - camY) * scale * height / 2);
            s.p1ScreenWidth = ROAD_WIDTH * scale * width / 2;
        }

        private void drawBackgroundScenery(Canvas canvas, int theme) {
            int bgCol1, bgCol2;
            if (theme == 0) {
                // Desert Sunset Gradient
                bgCol1 = Color.rgb(255, 110, 0);
                bgCol2 = Color.rgb(90, 0, 70);
            } else if (theme == 1) {
                // Forest Sky
                bgCol1 = Color.rgb(135, 206, 235);
                bgCol2 = Color.rgb(240, 255, 255);
            } else if (theme == 2) {
                // Snow Highlands grey sky
                bgCol1 = Color.rgb(180, 190, 210);
                bgCol2 = Color.rgb(245, 245, 250);
            } else {
                // Neon Cyber City sky gradient
                bgCol1 = Color.rgb(15, 0, 25);
                bgCol2 = Color.rgb(100, 0, 80);
            }

            LinearGradient grad = new LinearGradient(0, 0, 0, height / 2, bgCol1, bgCol2, Shader.TileMode.CLAMP);
            paint.setShader(grad);
            canvas.drawRect(0, 0, width, height / 2, paint);
            paint.setShader(null);

            // Draw Skyline Mountain Silhouette elements
            paint.setColor(Color.rgb(40, 30, 55));
            int mountainBaseY = height / 2;
            Path mPath = new Path();
            mPath.moveTo(0, mountainBaseY);
            mPath.lineTo(width * 1 / 10, mountainBaseY - 100);
            mPath.lineTo(width * 3 / 10, mountainBaseY - 30);
            mPath.lineTo(width * 5 / 10, mountainBaseY - 140);
            mPath.lineTo(width * 7 / 10, mountainBaseY - 40);
            mPath.lineTo(width * 9 / 10, mountainBaseY - 110);
            mPath.lineTo(width, mountainBaseY);
            mPath.close();
            canvas.drawPath(mPath, paint);
        }

        // Core vector drawing method connecting segments in polygons
        private void drawRoadSlice(Canvas canvas, Segment prev, Segment curr) {
            if (prev.p1ScreenY <= 0 || curr.p1ScreenY <= 0) return;

            float x1 = (float) prev.p1ScreenX;
            float y1 = (float) prev.p1ScreenY;
            float w1 = (float) prev.p1ScreenWidth;
            float x2 = (float) curr.p1ScreenX;
            float y2 = (float) curr.p1ScreenY;
            float w2 = (float) curr.p1ScreenWidth;

            // Determine Alternating colors of stripes based on segment indexes
            int grassCol, roadCol, rumbleCol, linesCol;
            boolean alt = (curr.index / 3) % 2 == 0;

            if (curr.colorStyle == 0) {
                // Desert themes
                grassCol = alt ? Color.rgb(220, 160, 90) : Color.rgb(205, 145, 75);
                roadCol = alt ? Color.rgb(100, 95, 90) : Color.rgb(90, 85, 80);
                rumbleCol = alt ? Color.rgb(180, 50, 50) : Color.rgb(220, 220, 220);
                linesCol = Color.WHITE;
            } else if (curr.colorStyle == 1) {
                // Forest theme
                grassCol = alt ? Color.rgb(34, 139, 34) : Color.rgb(46, 139, 87);
                roadCol = alt ? Color.rgb(80, 80, 80) : Color.rgb(70, 70, 70);
                rumbleCol = alt ? Color.RED : Color.WHITE;
                linesCol = Color.rgb(255, 215, 0);
            } else if (curr.colorStyle == 2) {
                // Snowy road theme
                grassCol = alt ? Color.rgb(230, 240, 245) : Color.rgb(255, 255, 255);
                roadCol = alt ? Color.rgb(95, 105, 115) : Color.rgb(85, 95, 105);
                rumbleCol = alt ? Color.RED : Color.BLUE;
                linesCol = Color.WHITE;
            } else {
                // Cyber Neon road theme
                grassCol = alt ? Color.rgb(20, 0, 40) : Color.rgb(10, 0, 30);
                roadCol = alt ? Color.rgb(25, 20, 35) : Color.rgb(15, 10, 25);
                rumbleCol = alt ? Color.rgb(255, 0, 127) : Color.rgb(0, 255, 255);
                linesCol = Color.rgb(0, 255, 127);
            }

            // Draw Grass / Sidewalk Polygon
            paint.setColor(grassCol);
            canvas.drawRect(0, y2, width, y1, paint);

            // Draw actual asphalt Road Polygon
            drawPolygon(canvas, x1, y1, w1, x2, y2, w2, roadCol);

            // Draw Side Shoulder Rumble Strips
            float rumbleW1 = w1 * 0.12f;
            float rumbleW2 = w2 * 0.12f;
            drawPolygon(canvas, x1 - w1, y1, rumbleW1, x2 - w2, y2, rumbleW2, rumbleCol);
            drawPolygon(canvas, x1 + w1, y1, rumbleW1, x2 + w2, y2, rumbleW2, rumbleCol);

            // Center stripe dashes
            if (alt) {
                float lineW1 = w1 * 0.02f;
                float lineW2 = w2 * 0.02f;
                drawPolygon(canvas, x1, y1, lineW1, x2, y2, lineW2, linesCol);
            }
        }

        private void drawPolygon(Canvas canvas, float x1, float y1, float w1, float x2, float y2, float w2, int color) {
            Path p = new Path();
            p.moveTo(x1 - w1, y1);
            p.lineTo(x2 - w2, y2);
            p.lineTo(x2 + w2, y2);
            p.lineTo(x1 + w1, y1);
            p.close();
            paint.setColor(color);
            canvas.drawPath(p, paint);
        }

        // Draws detailed Vector Obstacles and Collectible items
        private void drawSpriteObject(Canvas canvas, int type, double x, double y, double scale, int theme) {
            float size = (float) (240 * scale);
            if (size < 2) return;

            switch (type) {
                case GameObject.TYPE_TREE:
                    // Tree trunk
                    paint.setColor(Color.rgb(101, 67, 33));
                    canvas.drawRect((float) x - size * 0.1f, (float) y, (float) x + size * 0.1f, (float) y + size, paint);
                    // Pine leafy triangle green canopy
                    paint.setColor(Color.rgb(34, 100, 34));
                    Path treePath = new Path();
                    treePath.moveTo((float) x, (float) y - size * 1.5f);
                    treePath.lineTo((float) x - size * 0.6f, (float) y);
                    treePath.lineTo((float) x + size * 0.6f, (float) y);
                    treePath.close();
                    canvas.drawPath(treePath, paint);
                    break;

                case GameObject.TYPE_CACTUS:
                    paint.setColor(Color.rgb(46, 139, 87));
                    canvas.drawRect((float) x - size * 0.12f, (float) y - size, (float) x + size * 0.12f, (float) y + size, paint);
                    // Arm branch curves
                    canvas.drawRect((float) x - size * 0.4f, (float) y - size * 0.7f, (float) x, (float) y - size * 0.5f, paint);
                    canvas.drawRect((float) x - size * 0.4f, (float) y - size * 0.9f, (float) x - size * 0.28f, (float) y - size * 0.6f, paint);
                    break;

                case GameObject.TYPE_SNOW_TREE:
                    // White snow covered pine tree
                    paint.setColor(Color.rgb(112, 128, 144));
                    canvas.drawRect((float) x - size * 0.1f, (float) y, (float) x + size * 0.1f, (float) y + size, paint);
                    paint.setColor(Color.rgb(240, 248, 255));
                    Path snowPath = new Path();
                    snowPath.moveTo((float) x, (float) y - size * 1.3f);
                    snowPath.lineTo((float) x - size * 0.5f, (float) y);
                    snowPath.lineTo((float) x + size * 0.5f, (float) y);
                    snowPath.close();
                    canvas.drawPath(snowPath, paint);
                    break;

                case GameObject.TYPE_ROCK:
                    paint.setColor(Color.rgb(130, 130, 130));
                    Path rPath = new Path();
                    rPath.moveTo((float) x - size * 0.5f, (float) y);
                    rPath.lineTo((float) x - size * 0.4f, (float) y - size * 0.4f);
                    rPath.lineTo((float) x + size * 0.2f, (float) y - size * 0.5f);
                    rPath.lineTo((float) x + size * 0.5f, (float) y);
                    rPath.close();
                    canvas.drawPath(rPath, paint);
                    break;

                case GameObject.TYPE_CITY_LAMP:
                    // Cyber glowing lampposts
                    paint.setColor(Color.rgb(180, 180, 200));
                    canvas.drawRect((float) x - 2, (float) y - size * 1.6f, (float) x + 2, (float) y, paint);
                    // Light emitter
                    paint.setColor(Color.rgb(255, 0, 255));
                    canvas.drawCircle((float) x, (float) y - size * 1.6f, size * 0.15f, paint);
                    break;

                case GameObject.TYPE_FUEL:
                    // Metallic red gas can shape
                    paint.setColor(Color.RED);
                    canvas.drawRect((float) x - size * 0.2f, (float) y - size * 0.4f, (float) x + size * 0.2f, (float) y, paint);
                    paint.setColor(Color.WHITE);
                    canvas.drawRect((float) x - size * 0.05f, (float) y - size * 0.3f, (float) x + size * 0.05f, (float) y - size * 0.1f, paint);
                    break;

                case GameObject.TYPE_COIN:
                    // Rotating yellow star coin lookalike
                    paint.setColor(Color.rgb(255, 215, 0));
                    canvas.drawCircle((float) x, (float) y - size * 0.2f, size * 0.22f, paint);
                    paint.setColor(Color.WHITE);
                    canvas.drawCircle((float) x, (float) y - size * 0.2f, size * 0.12f, paint);
                    break;

                case GameObject.TYPE_FINISH:
                    // Finish arch
                    paint.setColor(Color.YELLOW);
                    canvas.drawRect((float) x - size * 2, (float) y - size * 2.5f, (float) x - size * 1.6f, (float) y, paint);
                    canvas.drawRect((float) x + size * 1.6f, (float) y - size * 2.5f, (float) x + size * 2, (float) y, paint);
                    canvas.drawRect((float) x - size * 2, (float) y - size * 2.5f, (float) x + size * 2, (float) y - size * 2, paint);
                    paint.setColor(Color.BLACK);
                    paint.setTextAlign(Paint.Align.CENTER);
                    paint.setTextSize(size * 0.35f);
                    canvas.drawText("FINISH CHAMPION!", (float) x, (float) y - size * 2.1f, paint);
                    break;
            }
        }

        // Complete custom procedural Vector drawing of player's or AI sports car
        private void drawVectorCar(Canvas canvas, int x, int y, double scale, int bodyColor, int steerLean) {
            float w = (float) (140 * scale);
            float h = (float) (80 * scale);
            if (w < 5) return;

            // Apply rotation matrices mathematically using canvas rotates
            canvas.save();
            canvas.translate(x, y);
            if (steerLean < 0) {
                canvas.rotate(-5f);
            } else if (steerLean > 0) {
                canvas.rotate(5f);
            }

            // Back sports wheels (thick racing rubber tires)
            paint.setColor(Color.BLACK);
            canvas.drawRoundRect(new RectF(-w * 0.48f, -h * 0.3f, -w * 0.3f, 0), 6, 6, paint);
            canvas.drawRoundRect(new RectF(w * 0.3f, -h * 0.3f, w * 0.48f, 0), 6, 6, paint);

            // Dynamic custom underglow shadow glow effect
            paint.setColor(Color.argb(90, 0, 0, 0));
            canvas.drawCircle(0, 0, w * 0.5f, paint);

            // Aerodynamic wide sports car lower bumper body
            paint.setColor(bodyColor);
            RectF mainBody = new RectF(-w * 0.45f, -h * 0.45f, w * 0.45f, -h * 0.1f);
            canvas.drawRoundRect(mainBody, 12, 12, paint);

            // Glass Windshield Cockpit roof
            paint.setColor(Color.rgb(30, 40, 50));
            RectF roof = new RectF(-w * 0.3f, -h * 0.85f, w * 0.3f, -h * 0.45f);
            canvas.drawRoundRect(roof, 14, 14, paint);
            paint.setColor(Color.rgb(100, 200, 255)); // glass reflection
            canvas.drawRoundRect(new RectF(-w * 0.25f, -h * 0.8f, w * 0.25f, -h * 0.52f), 8, 8, paint);

            // Sporty Spoiler wing winglets on top
            paint.setColor(Color.BLACK);
            canvas.drawRect(-w * 0.5f, -h * 0.95f, w * 0.5f, -h * 0.85f, paint);
            canvas.drawRect(-w * 0.5f, -h * 0.98f, -w * 0.42f, -h * 0.8f, paint);
            canvas.drawRect(w * 0.42f, -h * 0.98f, w * 0.5f, -h * 0.8f, paint);

            // Red rear tail lights (glowing brighter when braking)
            int tailColor = brakePressed ? Color.rgb(255, 10, 10) : Color.rgb(160, 0, 0);
            paint.setColor(tailColor);
            canvas.drawRoundRect(new RectF(-w * 0.42f, -h * 0.42f, -w * 0.28f, -h * 0.3f), 4, 4, paint);
            canvas.drawRoundRect(new RectF(w * 0.28f, -h * 0.42f, w * 0.42f, -h * 0.3f), 4, 4, paint);

            // Dual exhaust pipes throwing neon flames during turbo boosts
            paint.setColor(Color.rgb(100, 100, 100));
            canvas.drawCircle(-w * 0.2f, -h * 0.1f, w * 0.04f, paint);
            canvas.drawCircle(w * 0.2f, -h * 0.1f, w * 0.04f, paint);

            if (turboActive) {
                paint.setColor(Color.rgb(255, 69, 0));
                canvas.drawCircle(-w * 0.2f, -h * 0.02f, (float) (w * 0.08f * (1.0 + Math.random())), paint);
                canvas.drawCircle(w * 0.2f, -h * 0.02f, (float) (w * 0.08f * (1.0 + Math.random())), paint);
            }

            canvas.restore();
        }

        // Dynamic overlays and customized controls drawer
        private void drawDashboardControls(Canvas canvas) {
            // HUD panel top right - Glowing scores, speedometer, items
            paint.setColor(Color.argb(160, 0, 0, 0));
            canvas.drawRoundRect(new RectF(15, 15, 340, 130), 10, 10, paint);

            paint.setTextAlign(Paint.Align.LEFT);
            paint.setFakeBoldText(true);
            paint.setTextSize(22f);
            paint.setColor(Color.WHITE);
            canvas.drawText("SCORE: " + score, 30, 45, paint);
            canvas.drawText("COINS: " + coins, 30, 75, paint);

            // Progressive slider indicating race course track completion
            paint.setColor(Color.DKGRAY);
            canvas.drawRect(30, 100, 320, 112, paint);
            paint.setColor(Color.CYAN);
            double trackPct = playerZ / ((trackLength - 50) * SEGMENT_LENGTH);
            if (trackPct > 1.0) trackPct = 1.0;
            canvas.drawRect(30, 100, (float) (30 + 290 * trackPct), 112, paint);

            // Top Left panel - Speedometer, Fuel capacity indicator bars
            canvas.drawRoundRect(new RectF(width - 340, 15, width - 15, 130), 10, 10, paint);
            paint.setColor(Color.WHITE);
            paint.setTextSize(24f);
            canvas.drawText("SPEED: " + (int) playerSpeed + " MPH", width - 325, 45, paint);

            // Draw color coded fuel bar
            int fuelCol = Color.GREEN;
            if (fuel < 30) fuelCol = Color.RED;
            else if (fuel < 60) fuelCol = Color.YELLOW;
            paint.setColor(Color.rgb(60,60,60));
            canvas.drawRect(width - 325, 80, width - 35, 105, paint);
            paint.setColor(fuelCol);
            canvas.drawRect(width - 325, 80, (float) (width - 325 + 290 * (fuel / 100.0)), 105, paint);
            paint.setColor(Color.WHITE);
            paint.setTextSize(16f);
            canvas.drawText("FUEL TANK", width - 315, 98, paint);

            // Draw virtual overlay steering control zones when not using sensor tilt inputs
            if (!controlUseTilt) {
                // Left and right steer arrows
                paint.setColor(Color.argb(130, 50, 50, 50));
                canvas.drawCircle(80, height - 80, 60, paint);
                canvas.drawCircle(220, height - 80, 60, paint);

                paint.setColor(Color.WHITE);
                paint.setTextSize(40f);
                paint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText("◀", 80, height - 68, paint);
                canvas.drawText("▶", 220, height - 68, paint);

                // Right accelerator pedals, brakes
                paint.setColor(Color.argb(140, 10, 180, 10)); // Green GAS
                canvas.drawRoundRect(new RectF(width - 240, height - 130, width - 140, height - 20), 8, 8, paint);
                paint.setColor(Color.WHITE);
                paint.setTextSize(20f);
                canvas.drawText("GAS", width - 190, height - 70, paint);

                paint.setColor(Color.argb(140, 180, 10, 10)); // Red BRAKE
                canvas.drawRoundRect(new RectF(width - 110, height - 110, width - 20, height - 20), 8, 8, paint);
                paint.setColor(Color.WHITE);
                canvas.drawText("BRAKE", width - 65, height - 55, paint);
            } else {
                // Instructions overlays for tilt
                paint.setColor(Color.WHITE);
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setTextSize(18f);
                canvas.drawText("TILT DEVICE TO STEER CAR • TAP SCREEN RIGHT HALF TO ACCELERATE", width / 2, height - 30, paint);
            }

            // Pause upper visual icon toggle
            paint.setColor(Color.argb(180, 40, 40, 40));
            canvas.drawRect(width / 2 - 50, 10, width / 2 + 50, 55, paint);
            paint.setColor(Color.WHITE);
            paint.setTextSize(20f);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("PAUSE", width / 2, 40, paint);
        }

        private void drawPauseScreen(Canvas canvas) {
            paint.setColor(Color.argb(190, 0, 0, 0));
            canvas.drawRect(0, 0, width, height, paint);

            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(48f);
            paint.setColor(Color.YELLOW);
            canvas.drawText("GAME PAUSED", width / 2, height / 3, paint);

            drawButton(canvas, "RESUME GAME", width / 2, height * 55 / 100, Color.GREEN);
            drawButton(canvas, "EXIT TO MAIN MENU", width / 2, height * 70 / 100, Color.RED);
        }

        private void drawGameOverScreen(Canvas canvas) {
            paint.setColor(Color.argb(230, 15, 10, 30));
            canvas.drawRect(0, 0, width, height, paint);

            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(54f);
            paint.setColor(Color.RED);
            canvas.drawText("RACE CONCLUDED!", width / 2, height / 5, paint);

            paint.setColor(Color.WHITE);
            paint.setTextSize(28f);
            canvas.drawText("TOTAL DISTANCE SCORED: " + score, width / 2, height * 38 / 100, paint);
            canvas.drawText("GOLD COINS COLLECTED: " + coins, width / 2, height * 48 / 100, paint);

            if (score >= highScore) {
                paint.setColor(Color.GREEN);
                canvas.drawText("★ NEW PERSONAL HIGHSCORE! ★", width / 2, height * 58 / 100, paint);
            } else {
                paint.setColor(Color.GRAY);
                canvas.drawText("CURRENT PERSONAL RECORD: " + highScore, width / 2, height * 58 / 100, paint);
            }

            drawButton(canvas, "TAP ACCEL / REDO CHALLENGE", width / 2, height * 74 / 100, Color.GREEN);
            drawButton(canvas, "BACK TO MENU", width / 2, height * 88 / 100, Color.WHITE);
        }

        private void drawHighscoresScreen(Canvas canvas) {
            canvas.drawColor(Color.BLACK);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(40f);
            paint.setColor(Color.YELLOW);
            canvas.drawText("RACER LEADERBOARDS", width / 2, height / 4, paint);

            paint.setColor(Color.WHITE);
            paint.setTextSize(26f);
            canvas.drawText("1ST RANK: " + highScore + " PTS", width / 2, height / 2, paint);
            canvas.drawText("2ND RANK: 10000 PTS", width / 2, height / 2 + 50, paint);
            canvas.drawText("3RD RANK: 5000 PTS", width / 2, height / 2 + 100, paint);

            drawButton(canvas, "BACK", width / 2, height * 85 / 100, Color.RED);
        }

        private void drawButton(Canvas canvas, String label, float x, float y, int color) {
            paint.setTextSize(20f);
            float txtWidth = paint.measureText(label);
            RectF rect = new RectF(x - txtWidth / 2 - 25, y - 24, x + txtWidth / 2 + 25, y + 14); 
            paint.setColor(Color.argb(150, 40, 40, 40));
            canvas.drawRoundRect(rect, 6, 6, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2f);
            paint.setColor(color);
            canvas.drawRoundRect(rect, 6, 6, paint);

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(color);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(label, x, y, paint);
        }

        private void addFloatingText(String txt, float x, float y, int color) {
            FloatingText ft = new FloatingText();
            ft.text = txt;
            ft.x = x;
            ft.y = y;
            ft.color = color;
            ft.timer = 50; // frames lifespan
            floatingTexts.add(ft);
        }

        private void triggerVibration(int duration) {
            if (vibrator != null) {
                try {
                    vibrator.vibrate(duration);
                } catch (Exception ignored) {}
            }
        }

        // Handle screen interactions, inputs, menus
        @Override
        public boolean onTouchEvent(MotionEvent event) {
            float ex = event.getX();
            float ey = event.getY();
            int action = event.getActionMasked();

            if (gameState == STATE_MENU) {
                if (action == MotionEvent.ACTION_DOWN) {
                    if (ey > height * 45 / 100 && ey < height * 58 / 100) {
                        // Start Game
                        resetGame();
                        gameState = STATE_PLAYING;
                        triggerVibration(50);
                    } else if (ey > height * 62 / 100 && ey < height * 74 / 100) {
                        // Cycle Car color body
                        carColorIdx = (carColorIdx + 1) % carColors.length;
                        triggerVibration(25);
                    } else if (ey > height * 76 / 100 && ey < height * 85 / 100) {
                        // Toggle controls mode
                        controlUseTilt = !controlUseTilt;
                        if (controlUseTilt) {
                            registerAccelerometer();
                        } else {
                            unregisterAccelerometer();
                        }
                        triggerVibration(25);
                    } else if (ey > height * 86 / 100) {
                        // View Leaderboard
                        gameState = STATE_HIGHSCORES;
                        triggerVibration(25);
                    }
                }
            } else if (gameState == STATE_PLAYING) {
                if (controlUseTilt) {
                    // Simple Full right half screen triggers accelerate
                    if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
                        if (ex > width / 2) {
                            acceleratePressed = true;
                        } else {
                            brakePressed = true;
                        }
                    } else if (action == MotionEvent.ACTION_UP) {
                        acceleratePressed = false;
                        brakePressed = false;
                    }
                } else {
                    // Standard Multi-touch virtual dashboard controls
                    if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN || action == MotionEvent.ACTION_MOVE) {
                        boolean leftSteerPressed = false;
                        boolean rightSteerPressed = false;
                        boolean acc = false;
                        boolean brk = false;

                        // Check touch indexes
                        for (int i = 0; i < event.getPointerCount(); i++) {
                            float px = event.getX(i);
                            float py = event.getY(i);

                            // Steering triggers range
                            if (py > height - 160) {
                                if (px > 20 && px < 140) {
                                    leftSteerPressed = true;
                                } else if (px > 160 && px < 280) {
                                    rightSteerPressed = true;
                                }
                            }

                            // Pedals ranges
                            if (py > height - 150) {
                                if (px > width - 250 && px < width - 130) {
                                    acc = true;
                                } else if (px > width - 120 && px < width - 10) {
                                    brk = true;
                                }
                            }

                            // Pause toggle
                            if (py < 60 && px > width / 2 - 60 && px < width / 2 + 60) {
                                gameState = STATE_PAUSED;
                                triggerVibration(30);
                                return true;
                            }
                        }

                        if (leftSteerPressed) steerX = -1.0;
                        else if (rightSteerPressed) steerX = 1.0;
                        else steerX = 0;

                        acceleratePressed = acc;
                        brakePressed = brk;
                    } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
                        steerX = 0;
                        acceleratePressed = false;
                        brakePressed = false;
                    }
                }
            } else if (gameState == STATE_PAUSED) {
                if (action == MotionEvent.ACTION_DOWN) {
                    if (ey > height * 50 / 100 && ey < height * 62 / 100) {
                        gameState = STATE_PLAYING;
                        triggerVibration(25);
                    } else if (ey > height * 65 / 100 && ey < height * 78 / 100) {
                        gameState = STATE_MENU;
                        triggerVibration(25);
                    }
                }
            } else if (gameState == STATE_GAMEOVER) {
                if (action == MotionEvent.ACTION_DOWN) {
                    if (ey > height * 70 / 100 && ey < height * 82 / 100) {
                        resetGame();
                        gameState = STATE_PLAYING;
                        triggerVibration(25);
                    } else if (ey > height * 83 / 100) {
                        gameState = STATE_MENU;
                        triggerVibration(25);
                    }
                }
            } else if (gameState == STATE_HIGHSCORES) {
                if (action == MotionEvent.ACTION_DOWN) {
                    if (ey > height * 80 / 100) {
                        gameState = STATE_MENU;
                        triggerVibration(25);
                    }
                }
            }

            return true;
        }

        // Hardware Sensor management for dynamic tilt-steering
        private void registerAccelerometer() {
            if (sensorManager != null && accelerometer != null) {
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
            }
        }

        private void unregisterAccelerometer() {
            if (sensorManager != null) {
                sensorManager.unregisterListener(this);
            }
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (gameState == STATE_PLAYING && controlUseTilt) {
                // Extract landscape values mapping
                float tiltY = event.values[1]; // lateral axis mapping
                steerX = -tiltY * 0.22;
                if (steerX > 1.0) steerX = 1.0;
                if (steerX < -1.0) steerX = -1.0;
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}

        // Surface Lifecycle Handlers
        public void resume() {
            running = true;
            gameThread = new Thread(this);
            gameThread.start();
            if (controlUseTilt) {
                registerAccelerometer();
            }
        }

        public void pause() {
            running = false;
            stopSynthesizedSounds();
            unregisterAccelerometer();
            try {
                gameThread.join();
            } catch (InterruptedException e) {}
        }

        public void release() {
            stopSynthesizedSounds();
            unregisterAccelerometer();
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {}

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {}
    }
}
