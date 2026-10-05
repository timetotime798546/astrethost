package com.generatedappname.app;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.view.MotionEvent;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class GLCubeRenderer implements GLSurfaceView.Renderer {

    // Shader program
    private int program;

    // Vertex and fragment shader sources
    private String vertexShaderCode =
            "uniform mat4 uMVPMatrix;" +
            "attribute vec4 vPosition;" +
            "attribute vec4 vColor;" +
            "varying vec4 vColorOut;" +
            "void main() {" +
            "  gl_Position = uMVPMatrix * vPosition;" +
            "  vColorOut = vColor;" +
            "}";

    private String fragmentShaderCode =
            "precision mediump float;" +
            "varying vec4 vColorOut;" +
            "void main() {" +
            "  gl_FragColor = vColorOut;" +
            "}";

    // Cube vertices (8 vertices)
    // Order: bottom-left-front, bottom-right-front, top-right-front, top-left-front,
    //         bottom-left-back, bottom-right-back, top-right-back, top-left-back
    private float[] cubeVertices = {
        -0.5f, -0.5f,  0.5f,  // 0: bottom-left-front
         0.5f, -0.5f,  0.5f,  // 1: bottom-right-front
         0.5f,  0.5f,  0.5f,  // 2: top-right-front
        -0.5f,  0.5f,  0.5f,  // 3: top-left-front
        -0.5f, -0.5f, -0.5f,  // 4: bottom-left-back
         0.5f, -0.5f, -0.5f,  // 5: bottom-right-back
         0.5f,  0.5f, -0.5f,  // 6: top-right-back
        -0.5f,  0.5f, -0.5f   // 7: top-left-back
    };

    // Colors for each vertex (RGBA)
    // Front face: Red
    // Back face: Green
    // Top face: Blue
    // Bottom face: Yellow
    // Left face: Cyan
    // Right face: Magenta
    private float[] cubeColors = {
        // Front face (Red)
        1.0f, 0.0f, 0.0f, 1.0f,  // 0
        1.0f, 0.0f, 0.0f, 1.0f,  // 1
        1.0f, 0.0f, 0.0f, 1.0f,  // 2
        1.0f, 0.0f, 0.0f, 1.0f,  // 3
        // Back face (Green)
        0.0f, 1.0f, 0.0f, 1.0f,  // 4
        0.0f, 1.0f, 0.0f, 1.0f,  // 5
        0.0f, 1.0f, 0.0f, 1.0f,  // 6
        0.0f, 1.0f, 0.0f, 1.0f   // 7
    };

    // Indices for drawing the cube (6 faces, 2 triangles each)
    private short[] cubeIndices = {
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
    private float[] mProjectionMatrix = new float[16];
    private float[] mViewMatrix = new float[16];
    private float[] mMVPMatrix = new float[16];
    private float[] mModelMatrix = new float[16];

    // Rotation angles
    private float rotationX = 0.0f;
    private float rotationY = 0.0f;

    // Touch handling
    private float lastTouchX = 0.0f;
    private float lastTouchY = 0.0f;
    private boolean isTouching = false;

    // Vertex buffer
    private int vertexBufferId;
    private int colorBufferId;
    private int indexBufferId;

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // Clear the background to black
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        // Create the shader program
        program = createProgram(vertexShaderCode, fragmentShaderCode);

        // Enable vertex array
        GLES20.glEnableVertexAttribArray(0);
        GLES20.glEnableVertexAttribArray(1);

        // Create vertex buffer
        int[] vertexBuffer = new int[1];
        GLES20.glGenBuffers(1, vertexBuffer, 0);
        vertexBufferId = vertexBuffer[0];
        GLES20.glBindBuffer(GLES20.ARRAY_BUFFER, vertexBufferId);
        GLES20.glBufferData(GLES20.ARRAY_BUFFER, cubeVertices.length * 4,
                java.nio.ByteBuffer.allocateDirect(cubeVertices.length * 4)
                        .order(java.nio.ByteOrder.nativeOrder())
                        .asFloatBuffer()
                        .put(cubeVertices)
                        .position(0),
                GLES20.STATIC_DRAW);

        // Create color buffer
        int[] colorBuffer = new int[1];
        GLES20.glGenBuffers(1, colorBuffer, 0);
        colorBufferId = colorBuffer[0];
        GLES20.glBindBuffer(GLES20.ARRAY_BUFFER, colorBufferId);
        GLES20.glBufferData(GLES20.ARRAY_BUFFER, cubeColors.length * 4,
                java.nio.ByteBuffer.allocateDirect(cubeColors.length * 4)
                        .order(java.nio.ByteOrder.nativeOrder())
                        .asFloatBuffer()
                        .put(cubeColors)
                        .position(0),
                GLES20.STATIC_DRAW);

        // Create index buffer
        int[] indexBuffer = new int[1];
        GLES20.glGenBuffers(1, indexBuffer, 0);
        indexBufferId = indexBuffer[0];
        GLES20.glBindBuffer(GLES20.ELEMENT_ARRAY_BUFFER, indexBufferId);
        GLES20.glBufferData(GLES20.ELEMENT_ARRAY_BUFFER, cubeIndices.length * 2,
                java.nio.ByteBuffer.allocateDirect(cubeIndices.length * 2)
                        .order(java.nio.ByteOrder.nativeOrder())
                        .asShortBuffer()
                        .put(cubeIndices)
                        .position(0),
                GLES20.STATIC_DRAW);

        // Initialize model matrix to identity
        Matrix.setIdentityM(mModelMatrix, 0);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);

        // Calculate the projection matrix
        float ratio = (float) width / (float) height;
        float left = -ratio;
        float right = ratio;
        float bottom = -1.0f;
        float top = 1.0f;
        float near = 1.0f;
        float far = 100.0f;

        Matrix.frustumM(mProjectionMatrix, 0, left, right, bottom, top, near, far);

        // Set view matrix (camera position)
        Matrix.setLookAtM(mViewMatrix, 0, 0.0f, 0.0f, 5.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        // Clear the color buffer
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        // Use the program
        GLES20.glUseProgram(program);

        // Set the position attribute
        int positionHandle = GLES20.glGetAttribLocation(program, "vPosition");
        int colorHandle = GLES20.glGetAttribLocation(program, "vColor");

        // Bind vertex buffer
        GLES20.glBindBuffer(GLES20.ARRAY_BUFFER, vertexBufferId);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 12, 0);

        // Bind color buffer
        GLES20.glBindBuffer(GLES20.ARRAY_BUFFER, colorBufferId);
        GLES20.glVertexAttribPointer(colorHandle, 4, GLES20.GL_FLOAT, false, 16, 0);

        // Bind index buffer
        GLES20.glBindBuffer(GLES20.ELEMENT_ARRAY_BUFFER, indexBufferId);

        // Update rotation
        if (!isTouching) {
            rotationX += 1.0f;
            rotationY += 1.0f;
        }

        // Apply rotation to model matrix
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.rotateM(mModelMatrix, 0, rotationX, 1.0f, 0.0f, 0.0f);
        Matrix.rotateM(mModelMatrix, 0, rotationY, 0.0f, 1.0f, 0.0f);

        // Calculate MVP matrix
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mMVPMatrix, 0, mModelMatrix, 0);

        // Set the MVP matrix uniform
        int mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix");
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mMVPMatrix, 0);

        // Draw the cube
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, cubeIndices.length, GLES20.GL_UNSIGNED_SHORT, 0);
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
                rotationY += deltaX * 0.5f;
                rotationX += deltaY * 0.5f;
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

