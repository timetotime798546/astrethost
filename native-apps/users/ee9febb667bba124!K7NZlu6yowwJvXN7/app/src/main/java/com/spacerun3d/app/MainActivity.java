package com.spacerun3d.app;

import android.app.Activity;
import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MainActivity extends Activity {

    private GLSurfaceView mGLView;
    private FrameLayout mContainer;
    private RelativeLayout mHudLayout;
    private RelativeLayout mMenuLayout;
    private RelativeLayout mGameOverLayout;

    private TextView mScoreText;
    private TextView mSpeedText;
    private TextView mShieldText;
    private TextView mGameOverScoreText;

    private Button mBtnLeft;
    private Button mBtnRight;
    private Button mBtnStart;
    private Button mBtnRestart;

    // Game States
    private static final int STATE_MENU = 0;
    private static final int STATE_PLAYING = 1;
    private static final int STATE_GAMEOVER = 2;
    private volatile int mGameState = STATE_MENU;

    // Player State
    private volatile float mPlayerX = 0.0f;
    private static final float MAX_PLAYER_X = 3.0f;
    private static final float PLAYER_SPEED = 6.5f;

    // Control Flags
    private volatile boolean mMovingLeft = false;
    private volatile boolean mMovingRight = false;

    // Game variables
    private volatile int mScore = 0;
    private volatile int mShields = 3;
    private volatile float mGameSpeedMultiplier = 1.0f;

    // Obstacle Logic
    private static class Obstacle {
        float x;
        float z;
        float speed;
        boolean scored;
        float r, g, b;

        Obstacle(float x, float z, float speed) {
            this.x = x;
            this.z = z;
            this.speed = speed;
            this.scored = false;
            Random rand = new Random();
            this.r = 1.0f;
            this.g = rand.nextFloat() * 0.3f;
            this.b = rand.nextFloat() * 0.3f;
        }
    }

    private final List<Obstacle> mObstacles = new ArrayList<Obstacle>();
    private final List<Obstacle> mLaneMarkers = new ArrayList<Obstacle>();
    private long mLastUpdateTime = 0;
    private float mObstacleTimer = 0.0f;
    private float mObstacleSpawnInterval = 1.6f;

    private Vibrator mVibrator;
    private SpaceRenderer mRenderer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Force Fullscreen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // Bind layouts
        mContainer = (FrameLayout) findViewById(R.id.game_container);
        mHudLayout = (RelativeLayout) findViewById(R.id.hud_layout);
        mMenuLayout = (RelativeLayout) findViewById(R.id.menu_layout);
        mGameOverLayout = (RelativeLayout) findViewById(R.id.gameover_layout);

        mScoreText = (TextView) findViewById(R.id.score_text);
        mSpeedText = (TextView) findViewById(R.id.speed_text);
        mShieldText = (TextView) findViewById(R.id.shield_text);
        mGameOverScoreText = (TextView) findViewById(R.id.gameover_score);

        mBtnLeft = (Button) findViewById(R.id.btn_left);
        mBtnRight = (Button) findViewById(R.id.btn_right);
        mBtnStart = (Button) findViewById(R.id.btn_start);
        mBtnRestart = (Button) findViewById(R.id.btn_restart);

        // Configure GLView
        mGLView = new GLSurfaceView(this);
        mGLView.setEGLContextClientVersion(2);
        mRenderer = new SpaceRenderer();
        mGLView.setRenderer(mRenderer);
        mGLView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        // Add GLSurfaceView behind UI layers
        mContainer.addView(mGLView, 0);

        // Handlers
        mBtnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startGame();
            }
        });

        mBtnRestart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startGame();
            }
        });

        mBtnLeft.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    mMovingLeft = true;
                } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                    mMovingLeft = false;
                }
                return true;
            }
        });

        mBtnRight.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    mMovingRight = true;
                } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                    mMovingRight = false;
                }
                return true;
            }
        });

        initLaneMarkers();
    }

    private void initLaneMarkers() {
        mLaneMarkers.clear();
        for (int i = 0; i < 5; i++) {
            float startZ = -40.0f + (i * 8.0f);
            mLaneMarkers.add(new Obstacle(0f, startZ, 0f));
        }
    }

    private void startGame() {
        mScore = 0;
        mShields = 3;
        mPlayerX = 0.0f;
        mGameSpeedMultiplier = 1.0f;
        mObstacleTimer = 0.0f;
        mObstacleSpawnInterval = 1.6f;
        mObstacles.clear();
        initLaneMarkers();
        mLastUpdateTime = System.currentTimeMillis();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateHudValues();
                mMenuLayout.setVisibility(View.GONE);
                mGameOverLayout.setVisibility(View.GONE);
                mHudLayout.setVisibility(View.VISIBLE);
            }
        });

        mGameState = STATE_PLAYING;
    }

    private void updateHudValues() {
        mScoreText.setText("SCORE: " + String.format("%04d", mScore));
        mSpeedText.setText("WARP SPEED: " + String.format("%.1f", mGameSpeedMultiplier) + "x");
        StringBuilder shields = new StringBuilder("SHIELDS: ");
        for (int i = 0; i < 3; i++) {
            if (i < mShields) {
                shields.append("█");
            } else {
                shields.append("░");
            }
        }
        mShieldText.setText(shields.toString());
        if (mShields <= 1) {
            mShieldText.setTextColor(0xFFFF1744); // Neon red critical warning
        } else {
            mShieldText.setTextColor(0xFF00E676); // Good cyan green
        }
    }

    private void triggerDamageVibration() {
        try {
            if (mVibrator != null && mVibrator.hasVibrator()) {
                mVibrator.vibrate(300);
            }
        } catch (Exception ignored) {}
    }

    private void handleGameOver() {
        mGameState = STATE_GAMEOVER;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGameOverScoreText.setText("FINAL SCORE: " + String.format("%04d", mScore));
                mHudLayout.setVisibility(View.GONE);
                mGameOverLayout.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGLView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mGLView.onPause();
    }

    // --- OpenGL 3D Renderer Logic ---
    private class SpaceRenderer implements GLSurfaceView.Renderer {

        private final float[] mMVPMatrix = new float[16];
        private final float[] mProjectionMatrix = new float[16];
        private final float[] mViewMatrix = new float[16];

        private Cube mPlayerCube;
        private Cube mObstacleCube;
        private Floor mGridFloor;

        @Override
        public void onSurfaceCreated(GL10 unused, EGLConfig config) {
            GLES20.glClearColor(0.01f, 0.01f, 0.04f, 1.0f);
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);

            mPlayerCube = new Cube();
            mObstacleCube = new Cube();
            mGridFloor = new Floor();
        }

        @Override
        public void onSurfaceChanged(GL10 unused, int width, int height) {
            GLES20.glViewport(0, 0, width, height);
            float ratio = (float) width / height;
            Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1.0f, 1.0f, 1.0f, 50.0f);
        }

        @Override
        public void onDrawFrame(GL10 unused) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

            // Update Loop
            long now = System.currentTimeMillis();
            float elapsedSeconds = (now - mLastUpdateTime) / 1000.0f;
            mLastUpdateTime = now;

            // Restrict giant updates spikes
            if (elapsedSeconds > 0.1f) elapsedSeconds = 0.1f;

            if (mGameState == STATE_PLAYING) {
                updateGame(elapsedSeconds);
            }

            // Setup Camera matrix
            // Standard Third Person Camera trailing behind player
            Matrix.setLookAtM(mViewMatrix, 0, 
                    0.0f, 2.0f, -2.0f,    // Cam eye pos
                    0.0f, -0.4f, -12.0f,  // Look target point
                    0.0f, 1.0f, 0.0f);    // Up vector

            Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);

            // 1. Draw 3D floor grid environment
            mGridFloor.draw(mMVPMatrix);

            // 2. Draw Moving road divider stripes
            for (int i = 0; i < mLaneMarkers.size(); i++) {
                Obstacle marker = mLaneMarkers.get(i);
                float[] markerMatrix = new float[16];
                Matrix.translateM(markerMatrix, 0, mMVPMatrix, 0, marker.x, -1.0f, marker.z);
                // Scale to a thin divider block
                Matrix.scaleM(markerMatrix, 0, 0.12f, 0.01f, 1.2f);
                mPlayerCube.draw(markerMatrix, 0.0f, 0.9f, 1.0f); // bright cyan
            }

            // 3. Draw player 3D ship block
            float[] playerMatrix = new float[16];
            Matrix.translateM(playerMatrix, 0, mMVPMatrix, 0, mPlayerX, -0.65f, -6.0f);
            // Dynamic banking rotation when moving sides
            float rollAngle = 0.0f;
            if (mMovingLeft) rollAngle = 18.0f;
            if (mMovingRight) rollAngle = -18.0f;
            Matrix.rotateM(playerMatrix, 0, rollAngle, 0.0f, 0.0f, 1.0f);
            Matrix.scaleM(playerMatrix, 0, 0.7f, 0.4f, 0.8f);
            // Player glowing neon green ship
            mPlayerCube.draw(playerMatrix, 0.0f, 1.0f, 0.6f);

            // 4. Draw Danger Cubes
            synchronized (mObstacles) {
                for (int i = 0; i < mObstacles.size(); i++) {
                    Obstacle obs = mObstacles.get(i);
                    float[] obsMatrix = new float[16];
                    Matrix.translateM(obsMatrix, 0, mMVPMatrix, 0, obs.x, -0.5f, obs.z);
                    // Add spinning rotation
                    float rot = (float)(System.currentTimeMillis() % 2000) / 2000.0f * 360.0f;
                    Matrix.rotateM(obsMatrix, 0, rot, 0.4f, 1.0f, 0.2f);
                    Matrix.scaleM(obsMatrix, 0, 0.8f, 0.8f, 0.8f);
                    mObstacleCube.draw(obsMatrix, obs.r, obs.g, obs.b);
                }
            }
        }
    }

    private void updateGame(float deltaTime) {
        // Player side movement
        if (mMovingLeft) {
            mPlayerX -= PLAYER_SPEED * deltaTime;
            if (mPlayerX < -MAX_PLAYER_X) mPlayerX = -MAX_PLAYER_X;
        }
        if (mMovingRight) {
            mPlayerX += PLAYER_SPEED * deltaTime;
            if (mPlayerX > MAX_PLAYER_X) mPlayerX = MAX_PLAYER_X;
        }

        float baseSpeed = 12.0f;
        float currentSpeed = baseSpeed * mGameSpeedMultiplier;

        // Animate road stripes movement
        for (int i = 0; i < mLaneMarkers.size(); i++) {
            Obstacle marker = mLaneMarkers.get(i);
            marker.z += currentSpeed * deltaTime;
            if (marker.z > 0.0f) {
                marker.z = -40.0f;
            }
        }

        // Spawn hazards
        mObstacleTimer += deltaTime;
        if (mObstacleTimer >= mObstacleSpawnInterval) {
            mObstacleTimer = 0.0f;
            Random r = new Random();
            float randomX = -2.8f + r.nextFloat() * 5.6f;
            synchronized (mObstacles) {
                mObstacles.add(new Obstacle(randomX, -42.0f, currentSpeed));
            }
        }

        // Update active obstacles
        synchronized (mObstacles) {
            for (int i = mObstacles.size() - 1; i >= 0; i--) {
                Obstacle obs = mObstacles.get(i);
                obs.z += obs.speed * deltaTime;

                // Pass player line scoring check
                if (!obs.scored && obs.z > -6.0f) {
                    obs.scored = true;
                    mScore += 10;
                    // Slowly speed up matching system complexity progression
                    mGameSpeedMultiplier += 0.04f;
                    if (mGameSpeedMultiplier > 3.0f) mGameSpeedMultiplier = 3.0f;
                    mObstacleSpawnInterval = Math.max(0.6f, 1.6f - (mGameSpeedMultiplier * 0.25f));

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateHudValues();
                        }
                    });
                }

                // 3D Collision Detection
                // Player is at Z = -6.0. Checking bounds overlay spacing
                if (Math.abs(obs.z - (-6.0f)) < 0.75f) {
                    if (Math.abs(obs.x - mPlayerX) < 0.78f) {
                        // Collided!
                        mShields--;
                        triggerDamageVibration();
                        mObstacles.remove(i);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateHudValues();
                            }
                        });

                        if (mShields <= 0) {
                            handleGameOver();
                        }
                        continue;
                    }
                }

                // Clean memory when far behind view field
                if (obs.z > 2.0f) {
                    mObstacles.remove(i);
                }
            }
        }
    }

    // --- Standard OpenGL Geometry Definition Shaders ---

    public static class Cube {
        private final FloatBuffer vertexBuffer;
        private final ShortBuffer drawListBuffer;
        private int mProgram;

        private static final String vertexShaderCode =
            "uniform mat4 uMVPMatrix;" +
            "attribute vec4 vPosition;" +
            "varying vec4 vLightingColor;" +
            "uniform vec4 vColor;" +
            "void main() {" +
            "  gl_Position = uMVPMatrix * vPosition;" +
            // Simple lighting based on vertex positions
            "  vec3 normal = normalize(vPosition.xyz);" +
            "  vec3 lightDir = normalize(vec3(0.3, 1.0, 0.4));" +
            "  float diffuse = max(dot(normal, lightDir), 0.0);" +
            "  float ambient = 0.45;" +
            "  vLightingColor = vec4(vColor.rgb * (ambient + diffuse * 0.55), vColor.a);" +
            "}";

        private static final String fragmentShaderCode =
            "precision mediump float;" +
            "varying vec4 vLightingColor;" +
            "void main() {" +
            "  gl_FragColor = vLightingColor;" +
            "}";

        static final int COORDS_PER_VERTEX = 3;
        static float cubeCoords[] = {
            -0.5f,  0.5f,  0.5f,  // 0 front-top-left
            -0.5f, -0.5f,  0.5f,  // 1 front-bottom-left
             0.5f, -0.5f,  0.5f,  // 2 front-bottom-right
             0.5f,  0.5f,  0.5f,  // 3 front-top-right
            -0.5f,  0.5f, -0.5f,  // 4 back-top-left
            -0.5f, -0.5f, -0.5f,  // 5 back-bottom-left
             0.5f, -0.5f, -0.5f,  // 6 back-bottom-right
             0.5f,  0.5f, -0.5f   // 7 back-top-right
        };

        private final short drawOrder[] = {
            0, 1, 2, 0, 2, 3, // front
            4, 5, 6, 4, 6, 7, // back
            4, 0, 3, 4, 3, 7, // top
            5, 1, 2, 5, 2, 6, // bottom
            4, 5, 1, 4, 1, 0, // left
            3, 2, 6, 3, 6, 7  // right
        };

        public Cube() {
            ByteBuffer bb = ByteBuffer.allocateDirect(cubeCoords.length * 4);
            bb.order(ByteOrder.nativeOrder());
            vertexBuffer = bb.asFloatBuffer();
            vertexBuffer.put(cubeCoords);
            vertexBuffer.position(0);

            ByteBuffer dlb = ByteBuffer.allocateDirect(drawOrder.length * 2);
            dlb.order(ByteOrder.nativeOrder());
            drawListBuffer = dlb.asShortBuffer();
            drawListBuffer.put(drawOrder);
            drawListBuffer.position(0);

            int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
            int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

            mProgram = GLES20.glCreateProgram();
            GLES20.glAttachShader(mProgram, vertexShader);
            GLES20.glAttachShader(mProgram, fragmentShader);
            GLES20.glLinkProgram(mProgram);
        }

        public void draw(float[] mvpMatrix, float r, float g, float b) {
            GLES20.glUseProgram(mProgram);

            int positionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
            GLES20.glEnableVertexAttribArray(positionHandle);
            GLES20.glVertexAttribPointer(positionHandle, COORDS_PER_VERTEX,
                    GLES20.GL_FLOAT, false, 0, vertexBuffer);

            int colorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
            GLES20.glUniform4f(colorHandle, r, g, b, 1.0f);

            int mvpMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);

            GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.length,
                    GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

            GLES20.glDisableVertexAttribArray(positionHandle);
        }
    }

    public static class Floor {
        private final FloatBuffer vertexBuffer;
        private int mProgram;

        private static final String vertexShaderCode =
            "uniform mat4 uMVPMatrix;" +
            "attribute vec4 vPosition;" +
            "varying vec3 vWorldPos;" +
            "void main() {" +
            "  gl_Position = uMVPMatrix * vPosition;" +
            "  vWorldPos = vPosition.xyz;" +
            "}";

        private static final String fragmentShaderCode =
            "precision mediump float;" +
            "varying vec3 vWorldPos;" +
            "void main() {" +
            // Draw visual coordinate lane boundaries and horizontal perspective grid stripes
            "  float zGrid = abs(sin(vWorldPos.z * 1.5));" +
            "  float xGrid = abs(sin(vWorldPos.x * 2.5));" +
            "  vec3 color = vec3(0.01, 0.02, 0.06);" + // Base space color
            "  if (zGrid > 0.96 || xGrid > 0.98) {" +
            "    color = vec3(0.0, 0.45, 0.7); " +   // Neon grid line colors
            "  }" +
            "  gl_FragColor = vec4(color, 1.0);" +
            "}";

        static final int COORDS_PER_VERTEX = 3;
        static float floorCoords[] = {
            -4.0f, -1.02f,   0.0f,
            -4.0f, -1.02f, -44.0f,
             4.0f, -1.02f, -44.0f,
            -4.0f, -1.02f,   0.0f,
             4.0f, -1.02f, -44.0f,
             4.0f, -1.02f,   0.0f
        };

        public Floor() {
            ByteBuffer bb = ByteBuffer.allocateDirect(floorCoords.length * 4);
            bb.order(ByteOrder.nativeOrder());
            vertexBuffer = bb.asFloatBuffer();
            vertexBuffer.put(floorCoords);
            vertexBuffer.position(0);

            int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
            int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

            mProgram = GLES20.glCreateProgram();
            GLES20.glAttachShader(mProgram, vertexShader);
            GLES20.glAttachShader(mProgram, fragmentShader);
            GLES20.glLinkProgram(mProgram);
        }

        public void draw(float[] mvpMatrix) {
            GLES20.glUseProgram(mProgram);

            int positionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
            GLES20.glEnableVertexAttribArray(positionHandle);
            GLES20.glVertexAttribPointer(positionHandle, COORDS_PER_VERTEX,
                    GLES20.GL_FLOAT, false, 0, vertexBuffer);

            int mvpMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);

            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, floorCoords.length / COORDS_PER_VERTEX);

            GLES20.glDisableVertexAttribArray(positionHandle);
        }
    }

    public static int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }
}