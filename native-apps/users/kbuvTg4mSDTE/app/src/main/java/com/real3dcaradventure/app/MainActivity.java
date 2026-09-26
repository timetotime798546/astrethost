package com.real3dcaradventure.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
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

    // Control States
    private boolean mSteerLeft = false;
    private boolean mSteerRight = false;
    private boolean mGasPressed = false;
    private boolean mBrakePressed = false;

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
    private boolean mSoundRunning = false;
    private float mTargetSoundFreq = 80.0f;
    private float mCurrentSoundFreq = 80.0f;
    private float mCrashSoundVolume = 0.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPrefs = getSharedPreferences("Real3DRacerPrefs", MODE_PRIVATE);
        mBestScore = mPrefs.getInt("best_score", 0);

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

        mTvBestScore.setText("BEST: " + String.format("%05d", mBestScore));

        // Create and setup GLSurfaceView
        mGLSurfaceView = new GLSurfaceView(this);
        mGLSurfaceView.setEGLContextClientVersion(2);
        mRenderer = new CarRenderer(this);
        mGLSurfaceView.setRenderer(mRenderer);
        mGLContainer.addView(mGLSurfaceView);

        // Interactive control buttons configuration
        setupControls();

        // Game state loop setup
        mHandler = new Handler(Looper.getMainLooper());
        mGameUpdateTask = new Runnable() {
            @Override
            public void run() {
                if (mIsPlaying) {
                    updatePhysics();
                    mHandler.postDelayed(this, 16); // ~60fps logic
                }
            }
        };

        // Sound thread start
        startSoundSynth();
    }

    private void setupControls() {
        mBtnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startGame();
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

        // Crash effect sound trigger
        mCrashSoundVolume = 1.0f;

        if (mScore > mBestScore) {
            mBestScore = mScore;
            mPrefs.edit().putInt("best_score", mBestScore).apply();
            mTvBestScore.setText("BEST: " + String.format("%05d", mBestScore));
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTvOverlayTitle.setText("CRASHED!");
                mTvOverlaySubtitle.setText("Score: " + mScore + " | Best: " + mBestScore);
                mBtnStart.setText("DRIVE AGAIN");
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
            // Natural drag deceleration
            mSpeedMPH -= 0.6f;
            if (mSpeedMPH < 0.0f) mSpeedMPH = 0.0f;
        }

        // Handle Braking
        if (mBrakePressed) {
            mSpeedMPH -= 3.5f;
            if (mSpeedMPH < 0.0f) mSpeedMPH = 0.0f;
        }

        // Calculate dynamic shift gears
        String gearStr = "GEAR: N";
        if (mSpeedMPH > 140) gearStr = "GEAR: 5";
        else if (mSpeedMPH > 100) gearStr = "GEAR: 4";
        else if (mSpeedMPH > 60) gearStr = "GEAR: 3";
        else if (mSpeedMPH > 30) gearStr = "GEAR: 2";
        else if (mSpeedMPH > 0) gearStr = "GEAR: 1";

        mTvGear.setText(gearStr);
        mTvSpeed.setText((int) mSpeedMPH + " MPH");

        // Scale sound synth engine frequency linearly based on speeds
        mTargetSoundFreq = 70.0f + (mSpeedMPH * 1.6f);

        // Update player horizontal lane position based on steer keys
        float steeringScale = 0.08f * (mSpeedMPH / 100.0f + 0.3f);
        if (mSteerLeft) {
            mRenderer.movePlayer(-steeringScale);
        }
        if (mSteerRight) {
            mRenderer.movePlayer(steeringScale);
        }

        // Update Road Scroll / Distance
        float speedMultiplier = mSpeedMPH / 3600.0f; // Scale distance dynamically
        mDistanceTraveled += speedMultiplier;
        mRenderer.scrollWorld(mSpeedMPH * 0.012f);

        // Update Score counters
        if (mSpeedMPH > 5) {
            mScore += (int)(mSpeedMPH * 0.05f);
        }
        mTvScore.setText("SCORE: " + String.format("%05d", mScore));

        // Let the renderer update its game components
        mRenderer.updateSpawns();

        // Check if collision occurs in the GL engine
        if (mRenderer.isCollided()) {
            gameOver();
        }
    }

    private void startSoundSynth() {
        mSoundRunning = true;
        mSoundThread = new Thread(new Runnable() {
            @Override
            public void run() {
                // Initialize programmatic Mono 16-bit 22050Hz Audio Engine
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
                    // Smoothly slide current frequency towards target to prevent pops
                    mCurrentSoundFreq = mCurrentSoundFreq * 0.95f + mTargetSoundFreq * 0.05f;

                    for (int i = 0; i < buffer.length; i++) {
                        // Create deep rich double-oscillator engine growl using sine and sawtooth mixture
                        double val = Math.sin(phase) * 0.5f + (phase % 1.0f) * 0.3f;
                        
                        // Inject random crash noise on request
                        if (mCrashSoundVolume > 0.01f) {
                            val += (random.nextFloat() - 0.5f) * mCrashSoundVolume * 2.0f;
                            mCrashSoundVolume *= 0.999f; // decay
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
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGLSurfaceView.onResume();
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

        // Custom geometries
        private Cube mCubeGeometry;

        // Game positioning
        private float mPlayerX = 0.0f; // Steer offset between -2.2 and 2.2
        private float mScrollDistance = 0.0f;

        // Obstacle tracking arrays
        private static class Obstacle {
            float x;
            float z;
            float r, g, b;
            int type; // 0: Car, 1: Road Barrier
            boolean active;
        }

        private final ArrayList<Obstacle> mObstacles = new ArrayList<>();
        private final Random mRandom = new Random();
        private boolean mCollided = false;
        private float mCameraShake = 0.0f;

        public CarRenderer(Context context) {
            mContext = context;
        }

        public void resetGame() {
            mPlayerX = 0.0f;
            mScrollDistance = 0.0f;
            mCollided = false;
            mCameraShake = 0.0f;
            mObstacles.clear();

            // Populate initial procedural obstacles down the track
            for (int i = 0; i < 5; i++) {
                spawnObstacle(-40.0f - (i * 35.0f));
            }
        }

        private void spawnObstacle(float zPos) {
            Obstacle obs = new Obstacle();
            // 3 Lanes mapping (-1.8, 0.0, 1.8)
            int lane = mRandom.nextInt(3);
            obs.x = (lane - 1) * 1.8f;
            obs.z = zPos;
            obs.type = mRandom.nextInt(2); // 0 = standard car, 1 = warning orange block
            if (obs.type == 0) {
                obs.r = 0.2f + mRandom.nextFloat() * 0.8f;
                obs.g = 0.2f + mRandom.nextFloat() * 0.8f;
                obs.b = 0.2f + mRandom.nextFloat() * 0.8f;
            } else {
                obs.r = 1.0f;
                obs.g = 0.4f;
                obs.b = 0.0f;
            }
            obs.active = true;
            mObstacles.add(obs);
        }

        public void movePlayer(float amount) {
            if (mCollided) return;
            mPlayerX += amount;
            // Boundary constraints for absolute crash buffers
            if (mPlayerX < -3.2f) mPlayerX = -3.2f;
            if (mPlayerX > 3.2f) mPlayerX = 3.2f;
        }

        public void scrollWorld(float speedFactor) {
            if (mCollided) return;
            mScrollDistance += speedFactor;

            // Move each spawned obstacle closer to screen
            for (Obstacle obs : mObstacles) {
                obs.z += speedFactor;
            }
        }

        public void updateSpawns() {
            if (mCollided) return;

            // Clean up off-screen obstacles and spawn brand new ones dynamically
            for (int i = 0; i < mObstacles.size(); i++) {
                Obstacle obs = mObstacles.get(i);
                if (obs.z > 5.0f) {
                    mObstacles.remove(i);
                    i--;
                    // Spawn replacement obstacle at far end distance (approx -180 units)
                    float spawnPointZ = -140.0f;
                    if (!mObstacles.isEmpty()) {
                        spawnPointZ = mObstacles.get(mObstacles.size() - 1).z - 35.0f;
                    }
                    spawnObstacle(spawnPointZ);
                }
            }

            // Perform bounding-box crash logic
            checkCollision();
        }

        private void checkCollision() {
            // Player occupies around Z = -2.0 to -3.8, Width width: 1.0
            float playerZMin = -3.8f;
            float playerZMax = -2.0f;
            float playerXMin = mPlayerX - 0.6f;
            float playerXMax = mPlayerX + 0.6f;

            for (Obstacle obs : mObstacles) {
                if (obs.active) {
                    // Check bounding collision intersection
                    float obsZMin = obs.z - 1.2f;
                    float obsZMax = obs.z + 1.2f;
                    float obsXMin = obs.x - 0.65f;
                    float obsXMax = obs.x + 0.65f;

                    if (playerXMax >= obsXMin && playerXMin <= obsXMax) {
                        if (playerZMax >= obsZMin && playerZMin <= obsZMax) {
                            mCollided = true;
                            mCameraShake = 1.5f; // trigger intense shake
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
            // Dark night horizon sky color clear definition
            GLES20.glClearColor(0.04f, 0.08f, 0.16f, 1.0f);
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);

            // GLES20 Program assembly
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
                    "  float diffuse = max(dot(normal, lightDir), 0.35);" +
                    "  v_Color = u_Color * (diffuse + 0.15);" +
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

            // Bind indices
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

            // Define Camera lookAt matrix (Chase View style)
            float lookAtX = 0.0f;
            float lookAtY = 3.6f;
            float lookAtZ = 1.0f;

            // Handle Camera shaking on hit/crash
            if (mCameraShake > 0.01f) {
                lookAtX += (mRandom.nextFloat() - 0.5f) * mCameraShake;
                lookAtY += (mRandom.nextFloat() - 0.5f) * mCameraShake;
                mCameraShake *= 0.94f; // Decay over rendering ticks
            }

            Matrix.setLookAtM(mViewMatrix, 0,
                    lookAtX, lookAtY, lookAtZ,    // Camera position (behind & above player)
                    mPlayerX * 0.4f, 0.4f, -12.0f, // Looking at road ahead
                    0.0f, 1.0f, 0.0f);            // Up vector

            GLES20.glUseProgram(mProgram);

            // Setup static illumination source
            float[] lightPos = {0.0f, 15.0f, 0.0f};
            GLES20.glUniform3fv(mLightPosHandle, 1, lightPos, 0);

            // 1. Draw Infinite Green Sidewalk Grass Ground Plane
            Matrix.setIdentityM(mModelMatrix, 0);
            Matrix.translateM(mModelMatrix, 0, 0.0f, -1.02f, -80.0f);
            Matrix.scaleM(mModelMatrix, 0, 150.0f, 0.02f, 160.0f);
            drawBox(0.04f, 0.35f, 0.12f, 1.0f);

            // 2. Draw Long Road Bed Asphalt
            Matrix.setIdentityM(mModelMatrix, 0);
            Matrix.translateM(mModelMatrix, 0, 0.0f, -1.0f, -80.0f);
            Matrix.scaleM(mModelMatrix, 0, 7.0f, 0.02f, 160.0f);
            drawBox(0.18f, 0.18f, 0.19f, 1.0f);

            // 3. Draw Side Guardrails / Concrete Walls
            // Left Wall
            Matrix.setIdentityM(mModelMatrix, 0);
            Matrix.translateM(mModelMatrix, 0, -3.6f, -0.7f, -80.0f);
            Matrix.scaleM(mModelMatrix, 0, 0.25f, 0.6f, 160.0f);
            drawBox(0.6f, 0.6f, 0.62f, 1.0f);

            // Right Wall
            Matrix.setIdentityM(mModelMatrix, 0);
            Matrix.translateM(mModelMatrix, 0, 3.6f, -0.7f, -80.0f);
            Matrix.scaleM(mModelMatrix, 0, 0.25f, 0.6f, 160.0f);
            drawBox(0.6f, 0.6f, 0.62f, 1.0f);

            // 4. Draw Animated Scrolling Center Lanes Dashed Lines
            float stripeOffset = mScrollDistance % 10.0f;
            for (int zLine = 0; zLine < 20; zLine++) {
                float zPosition = 10.0f - (zLine * 10.0f) + stripeOffset;
                if (zPosition > 5.0f || zPosition < -150.0f) continue;

                // Left Dash Lane
                Matrix.setIdentityM(mModelMatrix, 0);
                Matrix.translateM(mModelMatrix, 0, -0.9f, -0.98f, zPosition);
                Matrix.scaleM(mModelMatrix, 0, 0.12f, 0.02f, 4.0f);
                drawBox(1.0f, 1.0f, 1.0f, 1.0f);

                // Right Dash Lane
                Matrix.setIdentityM(mModelMatrix, 0);
                Matrix.translateM(mModelMatrix, 0, 0.9f, -0.98f, zPosition);
                Matrix.scaleM(mModelMatrix, 0, 0.12f, 0.02f, 4.0f);
                drawBox(1.0f, 1.0f, 1.0f, 1.0f);
            }

            // 5. Draw Dynamic Scenery Trees Along The Road
            for (int treeIdx = 0; treeIdx < 12; treeIdx++) {
                float treeZ = 15.0f - (treeIdx * 15.0f) + (mScrollDistance % 15.0f);
                if (treeZ > 5.0f || treeZ < -150.0f) continue;

                // Left Scenery Pine Tree
                Matrix.setIdentityM(mModelMatrix, 0);
                Matrix.translateM(mModelMatrix, 0, -6.5f, -0.3f, treeZ);
                Matrix.scaleM(mModelMatrix, 0, 0.6f, 1.4f, 0.6f);
                drawBox(0.12f, 0.5f, 0.2f, 1.0f);

                // Right Scenery Pine Tree
                Matrix.setIdentityM(mModelMatrix, 0);
                Matrix.translateM(mModelMatrix, 0, 6.5f, -0.3f, treeZ);
                Matrix.scaleM(mModelMatrix, 0, 0.6f, 1.4f, 0.6f);
                drawBox(0.12f, 0.5f, 0.2f, 1.0f);
            }

            // 6. Draw Spawning Interactive Obstacles (Other Cars/Barriers)
            for (Obstacle obs : mObstacles) {
                if (obs.z > 5.0f || obs.z < -160.0f) continue;

                if (obs.type == 0) {
                    // Traffic Car Mesh representation
                    // Car body
                    Matrix.setIdentityM(mModelMatrix, 0);
                    Matrix.translateM(mModelMatrix, 0, obs.x, -0.7f, obs.z);
                    Matrix.scaleM(mModelMatrix, 0, 1.1f, 0.6f, 2.2f);
                    drawBox(obs.r, obs.g, obs.b, 1.0f);

                    // Car cabin top
                    Matrix.setIdentityM(mModelMatrix, 0);
                    Matrix.translateM(mModelMatrix, 0, obs.x, -0.3f, obs.z - 0.2f);
                    Matrix.scaleM(mModelMatrix, 0, 0.95f, 0.45f, 1.1f);
                    drawBox(0.08f, 0.08f, 0.08f, 1.0f);

                    // Tiny yellow rear head lights on back of car
                    Matrix.setIdentityM(mModelMatrix, 0);
                    Matrix.translateM(mModelMatrix, 0, obs.x - 0.4f, -0.6f, obs.z + 1.12f);
                    Matrix.scaleM(mModelMatrix, 0, 0.18f, 0.12f, 0.04f);
                    drawBox(1.0f, 0.0f, 0.0f, 1.0f);

                    Matrix.setIdentityM(mModelMatrix, 0);
                    Matrix.translateM(mModelMatrix, 0, obs.x + 0.4f, -0.6f, obs.z + 1.12f);
                    Matrix.scaleM(mModelMatrix, 0, 0.18f, 0.12f, 0.04f);
                    drawBox(1.0f, 0.0f, 0.0f, 1.0f);
                } else {
                    // Road Barrier warning brick representation
                    Matrix.setIdentityM(mModelMatrix, 0);
                    Matrix.translateM(mModelMatrix, 0, obs.x, -0.65f, obs.z);
                    Matrix.scaleM(mModelMatrix, 0, 1.4f, 0.7f, 0.5f);
                    drawBox(obs.r, obs.g, obs.b, 1.0f);

                    // Stripes pattern details
                    Matrix.setIdentityM(mModelMatrix, 0);
                    Matrix.translateM(mModelMatrix, 0, obs.x, -0.65f, obs.z + 0.26f);
                    Matrix.scaleM(mModelMatrix, 0, 0.4f, 0.72f, 0.02f);
                    drawBox(0.08f, 0.08f, 0.08f, 1.0f);
                }
            }

            // 7. Draw Player's 3D Real Sports Car
            // Placed at absolute chase coordinates (Z = -2.9)
            float playerZPos = -2.9f;

            // Draw Lower main car body shell
            Matrix.setIdentityM(mModelMatrix, 0);
            Matrix.translateM(mModelMatrix, 0, mPlayerX, -0.7f, playerZPos);
            Matrix.scaleM(mModelMatrix, 0, 1.2f, 0.5f, 2.3f);
            drawBox(0.85f, 0.04f, 0.04f, 1.0f); // Bright Red shell paint

            // Draw Sleek windshield cabin roof
            Matrix.setIdentityM(mModelMatrix, 0);
            Matrix.translateM(mModelMatrix, 0, mPlayerX, -0.34f, playerZPos - 0.2f);
            Matrix.scaleM(mModelMatrix, 0, 0.98f, 0.42f, 1.2f);
            drawBox(0.12f, 0.15f, 0.2f, 1.0f); // Tinted glass dark wind cabin

            // Draw back spoiler sport detail
            Matrix.setIdentityM(mModelMatrix, 0);
            Matrix.translateM(mModelMatrix, 0, mPlayerX, -0.38f, playerZPos + 1.1f);
            Matrix.scaleM(mModelMatrix, 0, 1.18f, 0.12f, 0.22f);
            drawBox(0.15f, 0.15f, 0.15f, 1.0f);

            // Draw 4 Black Sport Wheels
            // Front Left Wheel
            Matrix.setIdentityM(mModelMatrix, 0);
            Matrix.translateM(mModelMatrix, 0, mPlayerX - 0.62f, -0.85f, playerZPos - 0.8f);
            Matrix.scaleM(mModelMatrix, 0, 0.14f, 0.35f, 0.35f);
            drawBox(0.08f, 0.08f, 0.08f, 1.0f);

            // Front Right Wheel
            Matrix.setIdentityM(mModelMatrix, 0);
            Matrix.translateM(mModelMatrix, 0, mPlayerX + 0.62f, -0.85f, playerZPos - 0.8f);
            Matrix.scaleM(mModelMatrix, 0, 0.14f, 0.35f, 0.35f);
            drawBox(0.08f, 0.08f, 0.08f, 1.0f);

            // Rear Left Wheel
            Matrix.setIdentityM(mModelMatrix, 0);
            Matrix.translateM(mModelMatrix, 0, mPlayerX - 0.62f, -0.85f, playerZPos + 0.8f);
            Matrix.scaleM(mModelMatrix, 0, 0.14f, 0.35f, 0.35f);
            drawBox(0.08f, 0.08f, 0.08f, 1.0f);

            // Rear Right Wheel
            Matrix.setIdentityM(mModelMatrix, 0);
            Matrix.translateM(mModelMatrix, 0, mPlayerX + 0.62f, -0.85f, playerZPos + 0.8f);
            Matrix.scaleM(mModelMatrix, 0, 0.14f, 0.35f, 0.35f);
            drawBox(0.08f, 0.08f, 0.08f, 1.0f);
        }

        private void drawBox(float r, float g, float b, float a) {
            // Apply projection multiplication
            Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
            GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, mMVPMatrix, 0);

            Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);
            GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);

            // Set color state uniform
            GLES20.glUniform4f(mColorHandle, r, g, b, a);

            // Render box object attributes
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
            // Allocate float buffers directly
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