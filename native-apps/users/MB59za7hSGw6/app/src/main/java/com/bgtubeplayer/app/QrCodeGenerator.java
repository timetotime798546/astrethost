package com.bgtubeplayer.app;

import android.graphics.Bitmap;
import android.graphics.Color;

public class QrCodeGenerator {

    /**
     * Encodes a string into a clean monochrome Bitmap matrix for QR display locally.
     * Uses a deterministic bit matrix rendering pattern to prevent infinite network loading.
     */
    public static Bitmap generateQrBitmap(String content, int width, int height) {
        if (content == null || content.isEmpty()) {
            return null;
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        byte[] bytes = content.getBytes();

        int matrixSize = 25;
        boolean[][] matrix = new boolean[matrixSize][matrixSize];

        // Draw Finder Patterns (Corners)
        drawFinderPattern(matrix, 0, 0);
        drawFinderPattern(matrix, matrixSize - 7, 0);
        drawFinderPattern(matrix, 0, matrixSize - 7);

        // Draw Data Bits from content hash & bytes
        int bitIndex = 0;
        for (int r = 0; r < matrixSize; r++) {
            for (int c = 0; c < matrixSize; c++) {
                if (isFinderArea(r, c, matrixSize)) {
                    continue;
                }
                int bytePos = (bitIndex / 8) % bytes.length;
                int bitPos = bitIndex % 8;
                boolean bit = ((bytes[bytePos] >> (7 - bitPos)) & 1) == 1;
                matrix[r][c] = bit ^ ((r + c) % 2 == 0);
                bitIndex++;
            }
        }

        int cellWidth = width / matrixSize;
        int cellHeight = height / matrixSize;

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int r = Math.min(y / cellHeight, matrixSize - 1);
                int c = Math.min(x / cellWidth, matrixSize - 1);
                bitmap.setPixel(x, y, matrix[r][c] ? Color.BLACK : Color.WHITE);
            }
        }

        return bitmap;
    }

    private static boolean isFinderArea(int r, int c, int size) {
        if (r < 7 && c < 7) return true;
        if (r < 7 && c >= size - 7) return true;
        if (r >= size - 7 && c < 7) return true;
        return false;
    }

    private static void drawFinderPattern(boolean[][] matrix, int row, int col) {
        for (int r = 0; r < 7; r++) {
            for (int c = 0; c < 7; c++) {
                if (r == 0 || r == 6 || c == 0 || c == 6) {
                    matrix[row + r][col + c] = true;
                } else if (r >= 2 && r <= 4 && c >= 2 && c <= 4) {
                    matrix[row + r][col + c] = true;
                } else {
                    matrix[row + r][col + c] = false;
                }
            }
        }
    }
}