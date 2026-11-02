package com.brickgameclassic.app;

import java.util.Random;

/**
 * 3D Tetromino piece models with distinct color schemes and rotational topologies.
 */
public class Tetromino {
    public static final int TYPE_I = 0;
    public static final int TYPE_J = 1;
    public static final int TYPE_L = 2;
    public static final int TYPE_O = 3;
    public static final int TYPE_S = 4;
    public static final int TYPE_T = 5;
    public static final int TYPE_Z = 6;

    // Vibrant 3D Block Color Scheme
    public static final int[] COLORS = {
        0xFF00E5FF, // Cyan (I)
        0xFF2979FF, // Royal Blue (J)
        0xFFFF9100, // Vibrant Orange (L)
        0xFFFFEA00, // Bright Gold (O)
        0xFF00E676, // Neon Emerald (S)
        0xFFD500F9, // Vivid Violet (T)
        0xFFFF1744  // Hot Crimson (Z)
    };

    private static final int[][][][] SHAPES = {
        // I
        {
            {{0,0,0,0},{1,1,1,1},{0,0,0,0},{0,0,0,0}},
            {{0,0,1,0},{0,0,1,0},{0,0,1,0},{0,0,1,0}},
            {{0,0,0,0},{0,0,0,0},{1,1,1,1},{0,0,0,0}},
            {{0,1,0,0},{0,1,0,0},{0,1,0,0},{0,1,0,0}}
        },
        // J
        {
            {{1,0,0,0},{1,1,1,0},{0,0,0,0},{0,0,0,0}},
            {{0,1,1,0},{0,1,0,0},{0,1,0,0},{0,0,0,0}},
            {{0,0,0,0},{1,1,1,0},{0,0,1,0},{0,0,0,0}},
            {{0,1,0,0},{0,1,0,0},{1,1,0,0},{0,0,0,0}}
        },
        // L
        {
            {{0,0,1,0},{1,1,1,0},{0,0,0,0},{0,0,0,0}},
            {{0,1,0,0},{0,1,0,0},{0,1,1,0},{0,0,0,0}},
            {{0,0,0,0},{1,1,1,0},{1,0,0,0},{0,0,0,0}},
            {{1,1,0,0},{0,1,0,0},{0,1,0,0},{0,0,0,0}}
        },
        // O
        {
            {{0,1,1,0},{0,1,1,0},{0,0,0,0},{0,0,0,0}},
            {{0,1,1,0},{0,1,1,0},{0,0,0,0},{0,0,0,0}},
            {{0,1,1,0},{0,1,1,0},{0,0,0,0},{0,0,0,0}},
            {{0,1,1,0},{0,1,1,0},{0,0,0,0},{0,0,0,0}}
        },
        // S
        {
            {{0,1,1,0},{1,1,0,0},{0,0,0,0},{0,0,0,0}},
            {{0,1,0,0},{0,1,1,0},{0,0,1,0},{0,0,0,0}},
            {{0,0,0,0},{0,1,1,0},{1,1,0,0},{0,0,0,0}},
            {{1,0,0,0},{1,1,0,0},{0,1,0,0},{0,0,0,0}}
        },
        // T
        {
            {{0,1,0,0},{1,1,1,0},{0,0,0,0},{0,0,0,0}},
            {{0,1,0,0},{0,1,1,0},{0,1,0,0},{0,0,0,0}},
            {{0,0,0,0},{1,1,1,0},{0,1,0,0},{0,0,0,0}},
            {{0,1,0,0},{1,1,0,0},{0,1,0,0},{0,0,0,0}}
        },
        // Z
        {
            {{1,1,0,0},{0,1,1,0},{0,0,0,0},{0,0,0,0}},
            {{0,0,1,0},{0,1,1,0},{0,1,0,0},{0,0,0,0}},
            {{0,0,0,0},{1,1,0,0},{0,1,1,0},{0,0,0,0}},
            {{0,1,0,0},{1,1,0,0},{1,0,0,0},{0,0,0,0}}
        }
    };

    private static final Random RANDOM = new Random();

    public static int getRandomType() {
        return RANDOM.nextInt(7);
    }

    public static int[][] getMatrix(int type, int rotation) {
        int safeType = Math.max(0, Math.min(6, type));
        int safeRot = (rotation % 4 + 4) % 4;
        return SHAPES[safeType][safeRot];
    }

    public static int getColor(int type) {
        int safeType = Math.max(0, Math.min(6, type));
        return COLORS[safeType];
    }
}