package com.generatedappname.app;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.view.MotionEvent;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class GLCubeRenderer implements GLSurfaceView.Renderer {

    // Shader program
    private int program;

    // Vertex and fragment shader sources
    private final String vertexShaderCode =
            "uniform mat4 uMVPMatrix;" +
            "attribute vec4 vPosition;" +
            "attribute vec4 vColor;" +
            "varying vec4 vColorOut;" +
            "void main() {" +
            "  gl_Position = uMVPMatrix * vPosition;" +
            "  vColorOut = vColor;" +
            "}";

    private final String fragmentShaderCode =
            "precision mediump float;" +
            "varying vec4 vColorOut;" +
            "void main() {" +
            "  gl_FragColor = vColorOut;" +
            "}";

    // Cube vertices (8 vertices, 3 coordinates each)
    private final float[] cubeVertices = {
        -0.5f, -0.5f,  0.5f,  // 0: bottom-left-front
         0.5f, -0.5f,  0.5f,  // 1: bottom-right-front
         0.5f,  0.5f,  0.5f,  // 2: top-right-front
        -0.5f,  0.5f,  0.5f,  // 3: top-left-front
        -0.5f, -0.5f, -0.5f,  // 4: bottom-left-back
         0.5f, -0.5f, -0.5f,  // 5: bottom-right-back
         0.5f,  0.5f, -0.5f,  // 6: top-right-back
        -0.5f,  0.5f, -0.5f   // 7: top-left-back
    };

    // Colors for each of the 8 vertices (RGBA format)
    private final float[] cubeColors = {
        1.0f, 0.0f, 0.0f, 1.0f,  // 0: Red
        0.0f, 1.0f, 0.0f, 1.0f,  // 1: Green
        0.0f, 0.0f, 1.0f, 1.0f,  // 2: Blue
        1.0f, 1.0f, 0.0f, 1.0f,  // 3: Yellow
        1.0f, 0.0f, 1.0f, 1.0f,  // 4: Magenta
        0.0f, 1.0f, 1.0f, 1.0f,  // 5: Cyan
        1.0f, 1.0f, 1.0f, 1.0f,  // 6: White
        0.5f, 0.5f, 0.5f, 1.0f   // 7: Gray
    };

    // Indices for drawing the cube (6 faces, 2 triangles each)
    private final short[] cubeIndices = {
        // Front face
        0, 1, 2, 0, 2, 3,
        // Back face
        4, 6, 5, 4, 7, 6,
        // Top face
        3, 2, 6, 3, 6, 7,
        // Bottom face
        0, 4, 5, 0, 5, 1,
        // Left face
        4, 0, 3, 4, 3, 7,
        // Right face
        1, 5, 6, 1, 6, 2
    };

    // Matrices
    private final float[] mProjectionMatrix = new float[16];
    private final float[] mViewMatrix = new float[16];
    private final float[] mMVPMatrix = new float[16];
    private final float[] mModelMatrix = new float[16];

    // Rotation parameters
    private float rotationX = 0.0f;
    private float rotationY = 0.0f;

    // Touch feedback state
    private float lastTouchX = 0.0f;
    private float lastTouchY = 0.0f;
    private boolean isTouching = false;

    // Native JVM Direct Buffers to safely store vertex structures on heap
    private FloatBuffer vertexBuffer;
    private FloatBuffer colorBuffer;
    private ShortBuffer indexBuffer;

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // Clear background with soft deep space navy blue
        GLES20.glClearColor(0.08f, 0.08f, 0.15f, 1.0f);

        // CRITICAL: Enable depth testing so overlapping geometry layers render correctly
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        // Compile and link shader program
        program = createProgram(vertexShaderCode, fragmentShaderCode);

        // Setup vertex coordinate buffers on native memory heap
        ByteBuffer vbb = ByteBuffer.allocateDirect(cubeVertices.length * 4);
        vbb.order(ByteOrder.nativeOrder());
        vertexBuffer = vbb.asFloatBuffer();
        vertexBuffer.put(cubeVertices);
        vertexBuffer.position(0);

        // Setup vertex color buffers
        ByteBuffer cbb = ByteBuffer.allocateDirect(cubeColors.length * 4);
        cbb.order(ByteOrder.nativeOrder());
        colorBuffer = cbb.asFloatBuffer();
        colorBuffer.put(cubeColors);
        colorBuffer.position(0);

        // Setup face index structural bounds
        ByteBuffer ibb = ByteBuffer.allocateDirect(cubeIndices.length * 2);
        ibb.order(ByteOrder.nativeOrder());
        indexBuffer = ibb.asShortBuffer();
        indexBuffer.put(cubeIndices);
        indexBuffer.position(0);

        Matrix.setIdentityM(mModelMatrix, 0);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);

        float ratio = (float) width / (float) height;
        Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1.0f, 1.0f, 1.0f, 100.0f);

        // Position camera back at distance 4.0
        Matrix.setLookAtM(mViewMatrix, 0, 0.0f, 0.0f, 4.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        // Clear both the color pixel buffers and the depth comparison buffers
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // Apply shader instructions
        GLES20.glUseProgram(program);

        // Retrieve system attribute locations
        int positionHandle = GLES20.glGetAttribLocation(program, "vPosition");
        int colorHandle = GLES20.glGetAttribLocation(program, "vColor");
        int mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix");

        // Enable attributes dynamically
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glEnableVertexAttribArray(colorHandle);

        // Load position structures into memory
        vertexBuffer.position(0);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        // Load color structures into memory
        colorBuffer.position(0);
        GLES20.glVertexAttribPointer(colorHandle, 4, GLES20.GL_FLOAT, false, 0, colorBuffer);

        // Slow automatic idle rotation if screen is untouched
        if (!isTouching) {
            rotationX += 0.4f;
            rotationY += 0.6f;
        }

        // Apply spatial translations and spins
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.rotateM(mModelMatrix, 0, rotationX, 1.0f, 0.0f, 0.0f);
        Matrix.rotateM(mModelMatrix, 0, rotationY, 0.0f, 1.0f, 0.0f);

        // Calculate final MVP coordinates
        float[] tempMatrix = new float[16];
        Matrix.multiplyMM(tempMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, tempMatrix, 0);

        // Load coordinate projection map matrices
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mMVPMatrix, 0);

        // Draw structural triangles
        indexBuffer.position(0);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, cubeIndices.length, GLES20.GL_UNSIGNED_SHORT, indexBuffer);

        // Disable attribute handles to prevent accidental state leaking
        GLES20.glDisableVertexAttribArray(positionHandle);
        GLES20.glDisableVertexAttribArray(colorHandle);
    }

    public void handleTouch(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = event.getX();
                lastTouchY = event.getY();
                isTouching = true;
                break;
            case MotionEvent.ACTION_MOVE:
                float deltaX = event.getX() - lastTouchX;
                float deltaY = event.getY() - lastTouchY;
                
                // Fine-tuned rotational velocity controls
                rotationY += deltaX * 0.4f;
                rotationX += deltaY * 0.4f;
                
                lastTouchX = event.getX();
                lastTouchY = event.getY();
                break;
            case MotionEvent.ACTION_UP:
                isTouching = false;
                break;
        }
    }

    private int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            GLES20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    private int createProgram(String vertexShader, String fragmentShader) {
        int vertexShaderHandle = loadShader(GLES20.GL_VERTEX_SHADER, vertexShader);
        if (vertexShaderHandle == 0) {
            return 0;
        }

        int fragmentShaderHandle = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);
        if (fragmentShaderHandle == 0) {
            return 0;
        }

        int program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShaderHandle);
        GLES20.glAttachShader(program, fragmentShaderHandle);
        GLES20.glLinkProgram(program);

        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] != GLES20.GL_TRUE) {
            GLES20.glDeleteProgram(program);
            return 0;
        }

        return program;
    }
}