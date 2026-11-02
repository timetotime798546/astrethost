package com.brickgameclassic.app;

/**
 * 3D Vector mathematical representation supporting 3D transformations,
 * rotation around X and Y axes, dot products, and perspective projection.
 */
public class Vector3D {
    public float x;
    public float y;
    public float z;

    public Vector3D() {
        this(0, 0, 0);
    }

    public Vector3D(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector3D set(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    public Vector3D rotateX(float angleRad) {
        float cos = (float) Math.cos(angleRad);
        float sin = (float) Math.sin(angleRad);
        float ny = y * cos - z * sin;
        float nz = y * sin + z * cos;
        this.y = ny;
        this.z = nz;
        return this;
    }

    public Vector3D rotateY(float angleRad) {
        float cos = (float) Math.cos(angleRad);
        float sin = (float) Math.sin(angleRad);
        float nx = x * cos + z * sin;
        float nz = -x * sin + z * cos;
        this.x = nx;
        this.z = nz;
        return this;
    }

    public static float dot(Vector3D v1, Vector3D v2) {
        return v1.x * v2.x + v1.y * v2.y + v1.z * v2.z;
    }

    public static Vector3D computeNormal(Vector3D v0, Vector3D v1, Vector3D v2) {
        float ax = v1.x - v0.x;
        float ay = v1.y - v0.y;
        float az = v1.z - v0.z;

        float bx = v2.x - v0.x;
        float by = v2.y - v0.y;
        float bz = v2.z - v0.z;

        float nx = ay * bz - az * by;
        float ny = az * bx - ax * bz;
        float nz = ax * by - ay * bx;

        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len > 0.0001f) {
            nx /= len;
            ny /= len;
            nz /= len;
        }
        return new Vector3D(nx, ny, nz);
    }
}