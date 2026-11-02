package com.brickgameclassic.app;

import android.graphics.Path;

/**
 * Represents a single 3D quadrilateral face in view coordinates,
 * used for depth sorting (Painter's Algorithm) and directional shading.
 */
public class PolyFace3D implements Comparable<PolyFace3D> {
    public final Vector3D[] verts = new Vector3D[4];
    public final float[] screenX = new float[4];
    public final float[] screenY = new float[4];
    public final Path path = new Path();

    public float avgZ;
    public int color;
    public int strokeColor;
    public boolean isWireframeOnly = false;
    public float lightingMultiplier = 1.0f;

    public PolyFace3D() {
        for (int i = 0; i < 4; i++) {
            verts[i] = new Vector3D();
        }
    }

    public void computeAvgZ() {
        avgZ = (verts[0].z + verts[1].z + verts[2].z + verts[3].z) * 0.25f;
    }

    @Override
    public int compareTo(PolyFace3D other) {
        // Sort from furthest (largest Z) to nearest (smallest Z) for Painter's algorithm
        if (other.avgZ > this.avgZ) return 1;
        if (other.avgZ < this.avgZ) return -1;
        return 0;
    }
}