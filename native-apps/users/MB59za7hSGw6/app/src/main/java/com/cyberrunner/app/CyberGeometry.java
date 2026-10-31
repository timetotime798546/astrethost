package com.cyberrunner.app;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class CyberGeometry {

    // 36 vertices defining a 3D unit cube centered at origin [-0.5, 0.5]
    private static final float[] CUBE_VERTICES = {
            // Front Face
            -0.5f, -0.5f,  0.5f,
             0.5f, -0.5f,  0.5f,
             0.5f,  0.5f,  0.5f,
            -0.5f, -0.5f,  0.5f,
             0.5f,  0.5f,  0.5f,
            -0.5f,  0.5f,  0.5f,

            // Back Face
            -0.5f, -0.5f, -0.5f,
             0.5f,  0.5f, -0.5f,
             0.5f, -0.5f, -0.5f,
            -0.5f, -0.5f, -0.5f,
            -0.5f,  0.5f, -0.5f,
             0.5f,  0.5f, -0.5f,

            // Top Face
            -0.5f,  0.5f, -0.5f,
            -0.5f,  0.5f,  0.5f,
             0.5f,  0.5f,  0.5f,
            -0.5f,  0.5f, -0.5f,
             0.5f,  0.5f,  0.5f,
             0.5f,  0.5f, -0.5f,

            // Bottom Face
            -0.5f, -0.5f, -0.5f,
             0.5f, -0.5f,  0.5f,
            -0.5f, -0.5f,  0.5f,
            -0.5f, -0.5f, -0.5f,
             0.5f, -0.5f, -0.5f,
             0.5f, -0.5f,  0.5f,

            // Right Face
             0.5f, -0.5f, -0.5f,
             0.5f,  0.5f, -0.5f,
             0.5f,  0.5f,  0.5f,
             0.5f, -0.5f, -0.5f,
             0.5f,  0.5f,  0.5f,
             0.5f, -0.5f,  0.5f,

            // Left Face
            -0.5f, -0.5f, -0.5f,
            -0.5f, -0.5f,  0.5f,
            -0.5f,  0.5f,  0.5f,
            -0.5f, -0.5f, -0.5f,
            -0.5f,  0.5f,  0.5f,
            -0.5f,  0.5f, -0.5f
    };

    // 24 vertices defining an Octahedron (3D Cyber Coin)
    private static final float[] OCTAHEDRON_VERTICES = {
            // Top Pyramid
             0.0f,  0.5f,  0.0f,    0.0f,  0.0f,  0.5f,    0.5f,  0.0f,  0.0f,
             0.0f,  0.5f,  0.0f,    0.5f,  0.0f,  0.0f,    0.0f,  0.0f, -0.5f,
             0.0f,  0.5f,  0.0f,    0.0f,  0.0f, -0.5f,   -0.5f,  0.0f,  0.0f,
             0.0f,  0.5f,  0.0f,   -0.5f,  0.0f,  0.0f,    0.0f,  0.0f,  0.5f,
            // Bottom Pyramid
             0.0f, -0.5f,  0.0f,    0.5f,  0.0f,  0.0f,    0.0f,  0.0f,  0.5f,
             0.0f, -0.5f,  0.0f,    0.0f,  0.0f, -0.5f,    0.5f,  0.0f,  0.0f,
             0.0f, -0.5f,  0.0f,   -0.5f,  0.0f,  0.0f,    0.0f,  0.0f, -0.5f,
             0.0f, -0.5f,  0.0f,    0.0f,  0.0f,  0.5f,   -0.5f,  0.0f,  0.0f
    };

    private static FloatBuffer cubeBuffer;
    private static FloatBuffer octahedronBuffer;

    public static synchronized FloatBuffer getCubeBuffer() {
        if (cubeBuffer == null) {
            ByteBuffer bb = ByteBuffer.allocateDirect(CUBE_VERTICES.length * 4);
            bb.order(ByteOrder.nativeOrder());
            cubeBuffer = bb.asFloatBuffer();
            cubeBuffer.put(CUBE_VERTICES);
            cubeBuffer.position(0);
        }
        return cubeBuffer;
    }

    public static synchronized FloatBuffer getOctahedronBuffer() {
        if (octahedronBuffer == null) {
            ByteBuffer bb = ByteBuffer.allocateDirect(OCTAHEDRON_VERTICES.length * 4);
            bb.order(ByteOrder.nativeOrder());
            octahedronBuffer = bb.asFloatBuffer();
            octahedronBuffer.put(OCTAHEDRON_VERTICES);
            octahedronBuffer.position(0);
        }
        return octahedronBuffer;
    }

    public static FloatBuffer createBuffer(float[] data) {
        ByteBuffer bb = ByteBuffer.allocateDirect(data.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(data);
        fb.position(0);
        return fb;
    }

    public static int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }
}