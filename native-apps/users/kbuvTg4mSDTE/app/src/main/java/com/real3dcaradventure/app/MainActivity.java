package com.real3dcaradventure.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Random;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MainActivity extends Activity {

    private GLSurfaceView mGLSurfaceView;
    private CarRenderer mRenderer;
    private FrameLayout mGLContainer;

    // HUD Elements
    private TextView mTvSpeed;
    private TextView mTvGear;
    private TextView mTvScore;
    private TextView mTvBestScore;
    private LinearLayout mOverlayScreen;
    private TextView mTvOverlayTitle;
    private TextView mTvOverlaySubtitle;
    private Button mBtnStart;
    private Button mBtnToggleSensor;
    private View mVRpmBar;

    // Control States
    private boolean mSteerLeft = false;
    private boolean mSteerRight = false;
    private boolean mGasPressed = false;
    private boolean mBrakePressed = false;

    // Accelerometer Sensor variables
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private boolean mSensorEnabled = true;
    private float mSensorSteering = 0.0f;

    // Game variables
    private boolean mIsPlaying = false;
    private float mSpeedMPH = 0.0f;
    private int mScore = 0;
    private int mBestScore = 0;
    private float mDistanceTraveled = 0.0f;
    private SharedPreferences mPrefs;

    // Main Game Thread loop run
    private Handler mHandler;
    private Runnable mGameUpdateTask;

    // Dynamic Sound Engine Synthesizer
    private Thread mSoundThread;
    private volatile boolean mSoundRunning = false;
    private volatile float mTargetSoundFreq = 80.0f;
    private volatile float mCurrentSoundFreq = 80.0f;
    private volatile float mCrashSoundVolume = 0.0f;

    // Accelerometer Sensor Listener
    private final SensorEventListener mSensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (!mIsPlaying || !mSensorEnabled) {
                mSensorSteering = 0.0f;
                return;
            }
            // In Landscape, tilt steering maps to the Y-axis (index 1) of accelerometer
            float tiltY = event.values[1];
            
            // Apply neutral dead-zone and scale factor
            if (Math.abs(tiltY) > 0.8f) {
                mSensorSteering = -tiltY * 0.15f; 
            } else {
                mSensorSteering = 0.0f;
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPrefs = getSharedPreferences("Real3DRacerPrefs", MODE_PRIVATE);
        mBestScore = mPrefs.getInt("best_score", 0);
        mSensorEnabled = mPrefs.getBoolean("sensor_enabled", true);

        // Bind Views
        mGLContainer = findViewById(R.id.gl_container);
        mTvSpeed = findViewById(R.id.tv_speed);
        mTvGear = findViewById(R.id.tv_gear);
        mTvScore = findViewById(R.id.tv_score);
        mTvBestScore = findViewById(R.id.tv_best_score);
        mOverlayScreen = findViewById(R.id.overlay_screen);
        mTvOverlayTitle = findViewById(R.id.tv_overlay_title);
        mTvOverlaySubtitle = findViewById(R.id.tv_overlay_subtitle);
        mBtnStart = findViewById(R.id.btn_start);
        mBtnToggleSensor = findViewById(R.id.btn_toggle_sensor);
        mVRpmBar = findViewById(R.id.v_rpm_bar);

        mTvBestScore.setText("BEST: " + String.format("%05d", mBestScore));

        // Initialize Sensors
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (mSensorManager != null) {
            mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        // Create and setup GLSurfaceView
        mGLSurfaceView = new GLSurfaceView(this);
        mGLSurfaceView.setEGLContextClientVersion(2);
        mRenderer = new CarRenderer(this);
        mGLSurfaceView.setRenderer(mRenderer);
        mGLContainer.addView(mGLSurfaceView);

        // Dynamic Interactive Controls & Handlers
        setupControls();
        updateSensorButtonUI();

        mHandler = new Handler(Looper.getMainLooper());
        mGameUpdateTask = new Runnable() {
            @Override
            public void run() {
                if (mIsPlaying) {
                    updatePhysics();
                    mHandler.postDelayed(this, 16); // ~60fps game ticks
                }
            }
        };

        // Sound thread start
        startSoundSynth();
    }

    private void updateSensorButtonUI() {
        if (mSensorEnabled) {
            mBtnToggleSensor.setText("TILT: ON");
            mBtnToggleSensor.setBackgroundColor(0xFF4CAF50); // Material Green
            findViewById(R.id.steering_controls).setAlpha(0.35f); // Fade out steering buttons visually
        } else {
            mBtnToggleSensor.setText("TILT: OFF");
            mBtnToggleSensor.setBackgroundColor(0xFF757575); // Dark Gray
            findViewById(R.id.steering_controls).setAlpha(1.0f); // Bright steering buttons
        }
    }

    private void setupControls() {
        mBtnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startGame();
            }
        });

        mBtnToggleSensor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSensorEnabled = !mSensorEnabled;
                mPrefs.edit().putBoolean("sensor_enabled", mSensorEnabled).apply();
                updateSensorButtonUI();
            }
        });

        // Left steer action
        findViewById(R.id.btn_left).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        mSteerLeft = true;
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        mSteerLeft = false;
                        break;
                }
                return true;
            }
        });

        // Right steer action
        findViewById(R.id.btn_right).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        mSteerRight = true;
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        mSteerRight = false;
                        break;
                }
                return true;
            }
        });

        // Gas (accelerate) action
        findViewById(R.id.btn_gas).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        mGasPressed = true;
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        mGasPressed = false;
                        break;
                }
                return true;
            }
        });

        // Brake (decelerate) action
        findViewById(R.id.btn_brake).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        mBrakePressed = true;
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        mBrakePressed = false;
                        break;
                }
                return true;
            }
        });
    }

    private void startGame() {
        mOverlayScreen.setVisibility(View.GONE);
        mScore = 0;
        mDistanceTraveled = 0.0f;
        mSpeedMPH = 0.0f;
        mIsPlaying = true;
        mRenderer.resetGame();
        mHandler.post(mGameUpdateTask);
    }

    private void gameOver() {
        mIsPlaying = false;
        mHandler.removeCallbacks(mGameUpdateTask);

        // Trigger dynamic impact sound explosion
        mCrashSoundVolume = 1.0f;

        if (mScore > mBestScore) {
            mBestScore = mScore;
            mPrefs.edit().putInt("best_score", mBestScore).apply();
            mTvBestScore.setText("BEST: " + String.format("%05d", mBestScore));
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTvOverlayTitle.setText("COLLISION!");
                mTvOverlaySubtitle.setText("Score achieved: " + mScore + " | Best: " + mBestScore);
                mBtnStart.setText("TRY AGAIN");
                mOverlayScreen.setVisibility(View.VISIBLE);
            }
        });
    }

    private void updatePhysics() {
        // Handle Gas / Acceleration
        if (mGasPressed) {
            mSpeedMPH += 1.8f;
            if (mSpeedMPH > 180.0f) mSpeedMPH = 180.0f;
        } else {
            // Natural engine braking/drag
            mSpeedMPH -= 0.6f;
            if (mSpeedMPH < 0.0f) mSpeedMPH = 0.0f;
        }

        // Handle Heavy Braking
        if (mBrakePressed) {
            mSpeedMPH -= 3.5f;
            if (mSpeedMPH < 0.0f) mSpeedMPH = 0.0f;
        }

        // Calculate dynamic shifting gears & RPM values
        String gearStr = "GEAR: N";
        float gearMax = 30.0f;
        float gearMin = 0.0f;

        if (mSpeedMPH > 140) {
            gearStr = "GEAR: 5";
            gearMin = 140.0f;
            gearMax = 180.0f;
        } else if (mSpeedMPH > 100) {
            gearStr = "GEAR: 4";
            gearMin = 100.0f;
            gearMax = 140.0f;
        } else if (mSpeedMPH > 60) {
            gearStr = "GEAR: 3";
            gearMin = 60.0f;
            gearMax = 100.0f;
        } else if (mSpeedMPH > 30) {
            gearStr = "GEAR: 2";
            gearMin = 30.0f;
            gearMax = 60.0f;
        } else if (mSpeedMPH > 0) {
            gearStr = "GEAR: 1";
            gearMin = 0.0f;
            gearMax = 30.0f;
        }

        mTvGear.setText(gearStr);
        mTvSpeed.setText((int) mSpeedMPH + " MPH");

        // Scale sound synth engine frequency linearly based on current velocity
        mTargetSoundFreq = 70.0f + (mSpeedMPH * 1.6f);

        // Dynamic light up shift RPM bar representation
        float gearPercent = (mSpeedMPH - gearMin) / (gearMax - gearMin + 0.1f);
        if (gearPercent < 0.1f) gearPercent = 0.1f;
        if (gearPercent > 1.0f) gearPercent = 1.0f;
        final float finalPercent = gearPercent;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int totalWidth = findViewById(R.id.rpm_bar_container).getWidth();
                if (totalWidth > 0) {
                    android.view.ViewGroup.LayoutParams lp = mVRpmBar.getLayoutParams();
                    lp.width = (int) (totalWidth * finalPercent);
                    mVRpmBar.setLayoutParams(lp);
                }
            }
        });

        // Update player horizontal lane position based on steering (sensor or buttons)
        float steeringScale = 0.08f * (mSpeedMPH / 100.0f + 0.3f);
        if (mSteerLeft) {
            mRenderer.movePlayer(-steeringScale);
        } else if (mSteerRight) {
            mRenderer.movePlayer(steeringScale);
        } else if (mSensorEnabled && Math.abs(mSensorSteering) > 0.01f) {
            float speedFactor = (mSpeedMPH / 150.0f + 0.4f);
            mRenderer.movePlayer(mSensorSteering * speedFactor * 0.16f);
        }

        // Update Road Scroll & Traffic distances
        float speedMultiplier = mSpeedMPH / 3600.0f;
        mDistanceTraveled += speedMultiplier;
        mRenderer.scrollWorld(mSpeedMPH * 0.012f);

        // Update Score counter based on successful distance covered
        if (mSpeedMPH > 5) {
            mScore += (int)(mSpeedMPH * 0.06f);
        }
        mTvScore.setText("SCORE: " + String.format("%05d", mScore));

        // Spawn logic updates
        mRenderer.updateSpawns();

        // Safe collision termination checking
        if (mRenderer.isCollided()) {
            gameOver();
        }
    }

    private void startSoundSynth() {
        mSoundRunning = true;
        mSoundThread = new Thread(new Runnable() {
            @Override
            public void run() {
                int sampleRate = 22050;
                int bufferSize = AudioTrack.getMinBufferSize(sampleRate,
                        AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);

                AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                        sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);

                audioTrack.play();

                short[] buffer = new short[256];
                float phase = 0.0f;
                Random random = new Random();

                while (mSoundRunning) {
                    mCurrentSoundFreq = mCurrentSoundFreq * 0.95f + mTargetSoundFreq * 0.05f;

                    for (int i = 0; i < buffer.length; i++) {
                        double val = Math.sin(phase) * 0.5f + (phase % 1.0f) * 0.3f;
                        
                        if (mCrashSoundVolume > 0.01f) {
                            val += (random.nextFloat() - 0.5f) * mCrashSoundVolume * 2.2f;
                            mCrashSoundVolume *= 0.999f;
                        }

                        buffer[i] = (short) (val * 16384.0f);
                        phase += (mCurrentSoundFreq / sampleRate);
                        if (phase > (2.0f * Math.PI)) {
                            phase -= (2.0f * Math.PI);
                        }
                    }
                    audioTrack.write(buffer, 0, buffer.length);
                }
                audioTrack.stop();
                audioTrack.release();
            }
        });
        mSoundThread.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mGLSurfaceView.onPause();
        mIsPlaying = false;
        mHandler.removeCallbacks(mGameUpdateTask);
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(mSensorListener);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGLSurfaceView.onResume();
        if (mSensorManager != null && mAccelerometer != null) {
            mSensorManager.registerListener(mSensorListener, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
        }
        if (mIsPlaying) {
            mHandler.post(mGameUpdateTask);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSoundRunning = false;
        try {
            if (mSoundThread != null) {
                mSoundThread.join();
            }
        } catch (InterruptedException ignored) {}
    }

    // ==========================================
    // OPENGL ES 2.0 3D ENGINE GRAPHICS CLASSES
    // ==========================================

    public static class CarRenderer implements GLSurfaceView.Renderer {

        private final Context mContext;
        private final float[] mViewMatrix = new float[16];
        private final float[] mProjectionMatrix = new float[16];
        private final float[] mMVPMatrix = new float[16];
        private final float[] mModelMatrix = new float[16];

        private int mProgram;
        private int mPositionHandle;
        private int mNormalHandle;
        private int mColorHandle;
        private int mMVPMatrixHandle;
        private int mMVMatrixHandle;
        private int mLightPosHandle;

        private Cube mCubeGeometry;

        // Game positioning values
        private float mPlayerX = 0.0f; // bounds: -2.2f to 2.2f
        private float mScrollDistance = 0.0f;

        // Dynamic Running Traffic obstacles
        private static class Obstacle {
            float x;
            float z;
            float r, g, b;
            int type; // 0: Dynamic Traffic Car, 1: Static Road Barrier block
            int lane; // 0: Left, 1: Center, 2: Right
            boolean active;
            float speed; // Custom movement velocity
        }

        private final ArrayList<Obstacle> mObstacles = new ArrayList<Obstacle>();
        private final Random mRandom = new Random();
        private volatile boolean mCollided = false;
        private float mCameraShake = 0.0f;

        public CarRenderer(Context context) {
            mContext = context;
        }

        public void resetGame() {
            synchronized (mObstacles) {
                mPlayerX = 0.0f;
                mScrollDistance = 0.0f;
                mCollided = false;
                mCameraShake = 0.0f;
                mObstacles.clear();

                // Build initial procedural running highway vehicles
                for (int i = 0; i < 5; i++) {
                    spawnObstacle(-40.0f - (i * 35.0f));
                }
            }
        }

        private void spawnObstacle(float zPos) {
            synchronized (mObstacles) {
                Obstacle obs = new Obstacle();
                int lane = mRandom.nextInt(3);
                obs.lane = lane;
                obs.x = (lane - 1) * 1.8f;
                obs.z = zPos;
                obs.type = mRandom.nextInt(2); // 0 = Traffic Car, 1 = Hazard Barrier
                obs.active = true;
                
                // Assign realistic movement speed
                obs.speed = 0.12f + mRandom.nextFloat() * 0.14f;

                if (obs.type == 0) {
                    // Random sleek colors for traffic cars
                    obs.r = 0.1f + mRandom.nextFloat() * 0.9f;
                    obs.g = 0.1f + mRandom.nextFloat() * 0.9f;
                    obs.b = 0.1f + mRandom.nextFloat() * 0.9f;
                } else {
                    // Bright caution hazard orange
                    obs.r = 1.0f;
                    obs.g = 0.45f;
                    obs.b = 0.0f;
                }
                mObstacles.add(obs);
            }
        }

        public void movePlayer(float amount) {
            if (mCollided) return;
            mPlayerX += amount;
            // Bound safety values
            if (mPlayerX < -2.4f) mPlayerX = -2.4f;
            if (mPlayerX > 2.4f) mPlayerX = 2.4f;
        }

        public void scrollWorld(float speedFactor) {
            if (mCollided) return;
            mScrollDistance += speedFactor;

            // Update positions of dynamic obstacles in a synchronized loop
            synchronized (mObstacles) {
                for (Obstacle obs : mObstacles) {
                    // Base relative scrolling towards player
                    obs.z += speedFactor;

                    if (obs.type == 0) {
                        // Dynamic running car speed rules:
                        if (obs.lane == 0) {
                            // Oncoming lane: speeds towards player rapidly
                            obs.z += obs.speed * 1.2f;
                        } else {
                            // Forward lane: moves forward away from player, requiring player to pass them
                            obs.z -= obs.speed * 0.45f;
                        }
                    }
                }
            }
        }

        public void updateSpawns() {
            if (mCollided) return;

            synchronized (mObstacles) {
                // Clear out offscreen vehicles and replenish
                for (int i = 0; i < mObstacles.size(); i++) {
                    Obstacle obs = mObstacles.get(i);
                    if (obs.z > 5.0f) {
                        mObstacles.remove(i);
                        i--;
                        float spawnPointZ = -140.0f;
                        if (!mObstacles.isEmpty()) {
                            spawnPointZ = mObstacles.get(mObstacles.size() - 1).z - 35.0f;
                        }
                        spawnObstacle(spawnPointZ);
                    }
                }

                // Check collision states safely
                checkCollision();
            }
        }

        private void checkCollision() {
            // Player occupies around Z = -2.9f, length = 2.3f, width = 1.2f
            float playerZMin = -4.05f;
            float playerZMax = -1.75f;
            float playerXMin = mPlayerX - 0.6f;
            float playerXMax = mPlayerX + 0.6f;

            for (Obstacle obs : mObstacles) {
                if (obs.active) {
                    float obsZMin = obs.z - 1.1f;
                    float obsZMax = obs.z + 1.1f;
                    float obsXMin = obs.x - 0.55f;
                    float obsXMax = obs.x + 0.55f;

                    if (playerXMax >= obsXMin && playerXMin <= obsXMax) {
                        if (playerZMax >= obsZMin && playerZMin <= obsZMax) {
                            mCollided = true;
                            mCameraShake = 1.6f; // Initiate hard impact crash vibration camera screen
                        }
                    }
                }
            }
        }

        public boolean isCollided() {
            return mCollided;
        }

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            GLES20.glClearColor(0.02f, 0.04f, 0.08f, 1.0f); // Sleek cyber night atmosphere
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);

            // Shaders compiled with strict Java 8 GLES2 compatibility
            String vertexShaderCode =
                    "uniform mat4 u_MVPMatrix;" +
                    "uniform mat4 u_MVMatrix;" +
                    "uniform vec3 u_LightPos;" +
                    "attribute vec4 a_Position;" +
                    "attribute vec3 a_Normal;" +
                    "varying vec4 v_Color;" +
                    "uniform vec4 u_Color;" +
                    "void main() {" +
                    "  gl_Position = u_MVPMatrix * a_Position;" +
                    "  vec3 normal = normalize(vec3(u_MVMatrix * vec4(a_Normal, 0.0)));" +
                    "  vec3 lightDir = normalize(u_LightPos - vec3(u_MVMatrix * a_Position));" +
                    "  float diffuse = max(dot(normal, lightDir), 0.4);" +
                    "  v_Color = u_Color * (diffuse + 0.25);" +
                    "}";

            String fragmentShaderCode =
                    "precision mediump float;" +
                    "varying vec4 v_Color;" +
                    "void main() {" +
                    "  gl_FragColor = v_Color;" +
                    "}";

            int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
            int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

            mProgram = GLES20.glCreateProgram();
            GLES20.glAttachShader(mProgram, vertexShader);
            GLES20.glAttachShader(mProgram, fragmentShader);
            GLES20.glLinkProgram(mProgram);

            mPositionHandle = GLES20.glGetAttribLocation(mProgram, "a_Position");
            mNormalHandle = GLES20.glGetAttribLocation(mProgram, "a_Normal");
            mColorHandle = GLES20.glGetUniformLocation(mProgram, "u_Color");
            mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "u_MVPMatrix");
            mMVMatrixHandle = GLES20.glGetUniformLocation(mProgram, "u_MVMatrix");
            mLightPosHandle = GLES20.glGetUniformLocation(mProgram, "u_LightPos");

            mCubeGeometry = new Cube();
            resetGame();
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            GLES20.glViewport(0, 0, width, height);
            float ratio = (float) width / height;
            Matrix.perspectiveM(mProjectionMatrix, 0, 45.0f, ratio, 1.0f, 200.0f);
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

            // PERFECTED CAMERA PERSPECTIVE COCKPIT VIEWPORT
            float lookAtX = mPlayerX * 0.72f;
            float lookAtY = 0.85f;
            float lookAtZ = 1.35f;

            // Handle Camera vibrations on collisions
            if (mCameraShake > 0.01f) {
                lookAtX += (mRandom.nextFloat() - 0.5f) * mCameraShake * 0.4f;
                lookAtY += (mRandom.nextFloat() - 0.5f) * mCameraShake * 0.4f;
                mCameraShake *= 0.94f;
            }

            // Set perfect Chase View looking forward at the player's car
            Matrix.setLookAtM(mViewMatrix, 0,
                    lookAtX, lookAtY, lookAtZ,      // Eye position
                    mPlayerX, -0.55f, -20.0f,       // Looking down highway
                    0.0f, 1.0f, 0.0f);              // Up Vector

            GLES20.glUseProgram(mProgram);

            // Primary overhead illumination
            float[] lightPos = {0.0f, 16.0f, -4.0f};
            GLES20.glUniform3fv(mLightPosHandle, 1, lightPos, 0);

            // 1. Draw Massive Infinite Sidewalk Green grass panels
            Matrix.setIdentityM(mModelMatrix, 0);
            Matrix.translateM(mModelMatrix, 0, 0.0f, -1.02f, -80.0f);
            Matrix.scaleM(mModelMatrix, 0, 160.0f, 0.02f, 180.0f);
            drawBox(0.04f, 0.28f, 0.12f, 1.0f);

            // 2. Draw Solid Main Road Bed Asphalt
            Matrix.setIdentityM(mModelMatrix, 0);
            Matrix.translateM(mModelMatrix, 0, 0.0f, -0.85f, -80.0f);
            Matrix.scaleM(mModelMatrix, 0, 7.2f, 0.02f, 180.0f);
            drawBox(0.12f, 0.12f, 0.14f, 1.0f); // Dark industrial charcoal black

            // 3. Draw Guardrails / Concrete Walls
            // Left barrier wall
            Matrix.setIdentityM(mModelMatrix, 0);
            Matrix.translateM(mModelMatrix, 0, -3.7f, -0.55f, -80.0f);
            Matrix.scaleM(mModelMatrix, 0, 0.25f, 0.6f, 180.0f);
            drawBox(0.45f, 0.45f, 0.48f, 1.0f);

            // Right barrier wall
            Matrix.setIdentityM(mModelMatrix, 0);
            Matrix.translateM(mModelMatrix, 0, 3.7f, -0.55f, -80.0f);
            Matrix.scaleM(mModelMatrix, 0, 0.25f, 0.6f, 180.0f);
            drawBox(0.45f, 0.45f, 0.48f, 1.0f);

            // 4. Draw Animated Scrolling Highway Center Lanes Dashed Lines
            float stripeOffset = mScrollDistance % 10.0f;
            for (int zLine = 0; zLine < 20; zLine++) {
                float zPosition = 10.0f - (zLine * 10.0f) + stripeOffset;
                if (zPosition > 5.0f || zPosition < -150.0f) continue;

                // Left Lane divider
                Matrix.setIdentityM(mModelMatrix, 0);
                Matrix.translateM(mModelMatrix, 0, -0.9f, -0.83f, zPosition);
                Matrix.scaleM(mModelMatrix, 0, 0.12f, 0.02f, 4.0f);
                drawBox(0.95f, 0.95f, 0.05f, 1.0f); // Bright yellow stripes

                // Right Lane divider
                Matrix.setIdentityM(mModelMatrix, 0);
                Matrix.translateM(mModelMatrix, 0, 0.9f, -0.83f, zPosition);
                Matrix.scaleM(mModelMatrix, 0, 0.12f, 0.02f, 4.0f);
                drawBox(0.95f, 0.95f, 0.05f, 1.0f);
            }

            // 5. Draw Scenic 3D Pine Trees on the Sidewalk
            for (int treeIdx = 0; treeIdx < 12; treeIdx++) {
                float treeZ = 15.0f - (treeIdx * 15.0f) + (mScrollDistance % 15.0f);
                if (treeZ > 5.0f || treeZ < -150.0f) continue;

                // Left Scenery Tree
                Matrix.setIdentityM(mModelMatrix, 0);
                Matrix.translateM(mModelMatrix, 0, -5.5f, -0.2f, treeZ);
                Matrix.scaleM(mModelMatrix, 0, 0.5f, 1.3f, 0.5f);
                drawBox(0.08f, 0.45f, 0.15f, 1.0f);

                // Right Scenery Tree
                Matrix.setIdentityM(mModelMatrix, 0);
                Matrix.translateM(mModelMatrix, 0, 5.5f, -0.2f, treeZ);
                Matrix.scaleM(mModelMatrix, 0, 0.5f, 1.3f, 0.5f);
                drawBox(0.08f, 0.45f, 0.15f, 1.0f);
            }

            // 6. Draw 3D Scenic Architectural Skyscrapers (Buildings)
            for (int bldIdx = 0; bldIdx < 8; bldIdx++) {
                float bldZ = 20.0f - (bldIdx * 25.0f) + (mScrollDistance % 25.0f);
                if (bldZ > 5.0f || bldZ < -150.0f) continue;

                // Left procedural skyscraper with windows
                float bldHeightL = 6.5f + (bldIdx % 3) * 2.2f;
                Matrix.setIdentityM(mModelMatrix, 0);
                Matrix.translateM(mModelMatrix, 0, -8.2f, -0.85f + bldHeightL * 0.5f, bldZ);
                Matrix.scaleM(mModelMatrix, 0, 3.2f, bldHeightL, 3.2f);
                drawBox(0.24f, 0.26f, 0.32f, 1.0f); // Dark metallic blue block

                // Lit glass window strip (Cyberpunk glow overlay)
                Matrix.setIdentityM(mModelMatrix, 0);
                Matrix.translateM(mModelMatrix, 0, -6.55f, -0.85f + bldHeightL * 0.5f, bldZ);
                Matrix.scaleM(mModelMatrix, 0, 0.1f, bldHeightL * 0.75f, 1.4f);
                drawBox(0.0f, 0.95f, 1.0f, 1.0f); // cyan glowing window matrix

                // Right procedural skyscraper
                float bldHeightR = 7.2f + ((bldIdx + 1) % 3) * 2.0f;
                Matrix.setIdentityM(mModelMatrix, 0);
                Matrix.translateM(mModelMatrix, 0, 8.2f, -0.85f + bldHeightR * 0.5f, bldZ);
                Matrix.scaleM(mModelMatrix, 0, 3.2f, bldHeightR, 3.2f);
                drawBox(0.28f, 0.24f, 0.32f, 1.0f);

                // Lit orange windows on the right side
                Matrix.setIdentityM(mModelMatrix, 0);
                Matrix.translateM(mModelMatrix, 0, 6.55f, -0.85f + bldHeightR * 0.5f, bldZ);
                Matrix.scaleM(mModelMatrix, 0, 0.1f, bldHeightR * 0.75f, 1.4f);
                drawBox(1.0f, 0.55f, 0.0f, 1.0f); // gold-orange neon windows
            }

            // 7. Draw Dynamic Running Highway traffic obstacles
            synchronized (mObstacles) {
                for (Obstacle obs : mObstacles) {
                    if (obs.z > 5.0f || obs.z < -160.0f) continue;

                    if (obs.type == 0) {
                        // Traffic Car chassis
                        Matrix.setIdentityM(mModelMatrix, 0);
                        Matrix.translateM(mModelMatrix, 0, obs.x, -0.55f, obs.z);
                        Matrix.scaleM(mModelMatrix, 0, 1.15f, 0.55f, 2.2f);
                        drawBox(obs.r, obs.g, obs.b, 1.0f);

                        // Traffic Cabin top
                        Matrix.setIdentityM(mModelMatrix, 0);
                        Matrix.translateM(mModelMatrix, 0, obs.x, -0.2f, obs.z - 0.15f);
                        Matrix.scaleM(mModelMatrix, 0, 0.95f, 0.42f, 1.15f);
                        drawBox(0.08f, 0.08f, 0.08f, 1.0f);

                        // Glowing tail brake lights
                        Matrix.setIdentityM(mModelMatrix, 0);
                        Matrix.translateM(mModelMatrix, 0, obs.x - 0.42f, -0.48f, obs.z + 1.12f);
                        Matrix.scaleM(mModelMatrix, 0, 0.18f, 0.12f, 0.04f);
                        drawBox(1.0f, 0.0f, 0.0f, 1.0f);

                        Matrix.setIdentityM(mModelMatrix, 0);
                        Matrix.translateM(mModelMatrix, 0, obs.x + 0.42f, -0.48f, obs.z + 1.12f);
                        Matrix.scaleM(mModelMatrix, 0, 0.18f, 0.12f, 0.04f);
                        drawBox(1.0f, 0.0f, 0.0f, 1.0f);
                    } else {
                        // Static Hazard orange barriers
                        Matrix.setIdentityM(mModelMatrix, 0);
                        Matrix.translateM(mModelMatrix, 0, obs.x, -0.52f, obs.z);
                        Matrix.scaleM(mModelMatrix, 0, 1.45f, 0.65f, 0.55f);
                        drawBox(obs.r, obs.g, obs.b, 1.0f);

                        // Black stripe on barrier
                        Matrix.setIdentityM(mModelMatrix, 0);
                        Matrix.translateM(mModelMatrix, 0, obs.x, -0.52f, obs.z + 0.28f);
                        Matrix.scaleM(mModelMatrix, 0, 0.45f, 0.68f, 0.02f);
                        drawBox(0.05f, 0.05f, 0.05f, 1.0f);
                    }
                }
            }

            // 8. Render Player's 3D Real Sports Car
            // Placed at fixed Z = -2.9f relative to camera to frame perfectly
            float playerZPos = -2.9f;

            // Draw lower main sports car chassis
            Matrix.setIdentityM(mModelMatrix, 0);
            Matrix.translateM(mModelMatrix, 0, mPlayerX, -0.5f, playerZPos);
            Matrix.scaleM(mModelMatrix, 0, 1.25f, 0.46f, 2.35f);
            drawBox(0.85f, 0.04f, 0.04f, 1.0f); // Vibrant Gloss Red

            // Sleek aerodynamic cabin top windshield
            Matrix.setIdentityM(mModelMatrix, 0);
            Matrix.translateM(mModelMatrix, 0, mPlayerX, -0.16f, playerZPos - 0.2f);
            Matrix.scaleM(mModelMatrix, 0, 0.98f, 0.38f, 1.25f);
            drawBox(0.1f, 0.12f, 0.16f, 1.0f); // Tinted glass

            // Rear racing spoiler
            Matrix.setIdentityM(mModelMatrix, 0);
            Matrix.translateM(mModelMatrix, 0, mPlayerX, -0.18f, playerZPos + 1.12f);
            Matrix.scaleM(mModelMatrix, 0, 1.2f, 0.12f, 0.24f);
            drawBox(0.12f, 0.12f, 0.12f, 1.0f);

            // Left rear tail light
            Matrix.setIdentityM(mModelMatrix, 0);
            Matrix.translateM(mModelMatrix, 0, mPlayerX - 0.48f, -0.42f, playerZPos + 1.18f);
            Matrix.scaleM(mModelMatrix, 0, 0.22f, 0.08f, 0.02f);
            drawBox(1.0f, 0.0f, 0.0f, 1.0f);

            // Right rear tail light
            Matrix.setIdentityM(mModelMatrix, 0);
            Matrix.translateM(mModelMatrix, 0, mPlayerX + 0.48f, -0.42f, playerZPos + 1.18f);
            Matrix.scaleM(mModelMatrix, 0, 0.22f, 0.08f, 0.02f);
            drawBox(1.0f, 0.0f, 0.0f, 1.0f);

            // 4 Sport Racing Wheels (perfectly aligned above road bed)
            float wheelY = -0.7f;
            // Front Left
            Matrix.setIdentityM(mModelMatrix, 0);
            Matrix.translateM(mModelMatrix, 0, mPlayerX - 0.65f, wheelY, playerZPos - 0.8f);
            Matrix.scaleM(mModelMatrix, 0, 0.15f, 0.35f, 0.35f);
            drawBox(0.08f, 0.08f, 0.08f, 1.0f);

            // Front Right
            Matrix.setIdentityM(mModelMatrix, 0);
            Matrix.translateM(mModelMatrix, 0, mPlayerX + 0.65f, wheelY, playerZPos - 0.8f);
            Matrix.scaleM(mModelMatrix, 0, 0.15f, 0.35f, 0.35f);
            drawBox(0.08f, 0.08f, 0.08f, 1.0f);

            // Rear Left
            Matrix.setIdentityM(mModelMatrix, 0);
            Matrix.translateM(mModelMatrix, 0, mPlayerX - 0.65f, wheelY, playerZPos + 0.8f);
            Matrix.scaleM(mModelMatrix, 0, 0.15f, 0.35f, 0.35f);
            drawBox(0.08f, 0.08f, 0.08f, 1.0f);

            // Rear Right
            Matrix.setIdentityM(mModelMatrix, 0);
            Matrix.translateM(mModelMatrix, 0, mPlayerX + 0.65f, wheelY, playerZPos + 0.8f);
            Matrix.scaleM(mModelMatrix, 0, 0.15f, 0.35f, 0.35f);
            drawBox(0.08f, 0.08f, 0.08f, 1.0f);
        }

        private void drawBox(float r, float g, float b, float a) {
            // Matrix model transformation configurations
            Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
            GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, mMVPMatrix, 0);

            Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);
            GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);

            GLES20.glUniform4f(mColorHandle, r, g, b, a);

            mCubeGeometry.draw(mPositionHandle, mNormalHandle);
        }

        private int loadShader(int type, String shaderCode) {
            int shader = GLES20.glCreateShader(type);
            GLES20.glShaderSource(shader, shaderCode);
            GLES20.glCompileShader(shader);
            return shader;
        }
    }

    // High performance Vertex buffers class for full 3D Cube Renderings
    public static class Cube {
        private final FloatBuffer mVertexBuffer;
        private final FloatBuffer mNormalBuffer;
        private final ShortBuffer mIndexBuffer;

        private final float[] mVertices = {
                // Front Face
                -0.5f, -0.5f,  0.5f,   0.5f, -0.5f,  0.5f,   0.5f,  0.5f,  0.5f,  -0.5f,  0.5f,  0.5f,
                // Back Face
                -0.5f, -0.5f, -0.5f,  -0.5f,  0.5f, -0.5f,   0.5f,  0.5f, -0.5f,   0.5f, -0.5f, -0.5f,
                // Top Face
                -0.5f,  0.5f, -0.5f,  -0.5f,  0.5f,  0.5f,   0.5f,  0.5f,  0.5f,   0.5f,  0.5f, -0.5f,
                // Bottom Face
                -0.5f, -0.5f, -0.5f,   0.5f, -0.5f, -0.5f,   0.5f, -0.5f,  0.5f,  -0.5f, -0.5f,  0.5f,
                // Right Face
                 0.5f, -0.5f, -0.5f,   0.5f,  0.5f, -0.5f,   0.5f,  0.5f,  0.5f,   0.5f, -0.5f,  0.5f,
                // Left Face
                -0.5f, -0.5f, -0.5f,  -0.5f, -0.5f,  0.5f,  -0.5f,  0.5f,  0.5f,  -0.5f,  0.5f, -0.5f
        };

        private final float[] mNormals = {
                // Front
                0f, 0f, 1f,   0f, 0f, 1f,   0f, 0f, 1f,   0f, 0f, 1f,
                // Back
                0f, 0f, -1f,  0f, 0f, -1f,  0f, 0f, -1f,  0f, 0f, -1f,
                // Top
                0f, 1f, 0f,   0f, 1f, 0f,   0f, 1f, 0f,   0f, 1f, 0f,
                // Bottom
                0f, -1f, 0f,  0f, -1f, 0f,  0f, -1f, 0f,  0f, -1f, 0f,
                // Right
                1f, 0f, 0f,   1f, 0f, 0f,   1f, 0f, 0f,   1f, 0f, 0f,
                // Left
                -1f, 0f, 0f, -1f, 0f, 0f,  -1f, 0f, 0f,  -1f, 0f, 0f
        };

        private final short[] mIndices = {
                0, 1, 2,  0, 2, 3,       // Front
                4, 5, 6,  4, 6, 7,       // Back
                8, 9, 10, 8, 10, 11,     // Top
                12, 13, 14, 12, 14, 15,  // Bottom
                16, 17, 18, 16, 18, 19,  // Right
                20, 21, 22, 20, 22, 23   // Left
        };

        public Cube() {
            ByteBuffer vbb = ByteBuffer.allocateDirect(mVertices.length * 4);
            vbb.order(ByteOrder.nativeOrder());
            mVertexBuffer = vbb.asFloatBuffer();
            mVertexBuffer.put(mVertices);
            mVertexBuffer.position(0);

            ByteBuffer nbb = ByteBuffer.allocateDirect(mNormals.length * 4);
            nbb.order(ByteOrder.nativeOrder());
            mNormalBuffer = nbb.asFloatBuffer();
            mNormalBuffer.put(mNormals);
            mNormalBuffer.position(0);

            ByteBuffer ibb = ByteBuffer.allocateDirect(mIndices.length * 2);
            ibb.order(ByteOrder.nativeOrder());
            mIndexBuffer = ibb.asShortBuffer();
            mIndexBuffer.put(mIndices);
            mIndexBuffer.position(0);
        }

        public void draw(int posHandle, int normalHandle) {
            GLES20.glEnableVertexAttribArray(posHandle);
            GLES20.glVertexAttribPointer(posHandle, 3, GLES20.GL_FLOAT, false, 12, mVertexBuffer);

            GLES20.glEnableVertexAttribArray(normalHandle);
            GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT, false, 12, mNormalBuffer);

            GLES20.glDrawElements(GLES20.GL_TRIANGLES, mIndices.length, GLES20.GL_UNSIGNED_SHORT, mIndexBuffer);

            GLES20.glDisableVertexAttribArray(posHandle);
            GLES20.glDisableVertexAttribArray(normalHandle);
        }
    }
}