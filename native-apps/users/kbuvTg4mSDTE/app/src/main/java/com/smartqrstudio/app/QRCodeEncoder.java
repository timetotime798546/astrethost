package com.smartqrstudio.app;

import android.graphics.Bitmap;
import android.graphics.Color;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Native Pure Java 2D QR Code Generator.
 * Implements ISO/IEC 18004 QR Code Byte Mode with Reed-Solomon Error Correction.
 * Runs completely on-device without third-party libraries.
 */
public class QRCodeEncoder {

    // Galois Field GF(256) logarithm and antilogarithm tables for primitive polynomial 0x11D (285)
    private static final int[] EXP = new int[512];
    private static final int[] LOG = new int[256];

    static {
        int val = 1;
        for (int i = 0; i < 255; i++) {
            EXP[i] = val;
            EXP[i + 255] = val;
            LOG[val] = i;
            val = val << 1;
            if ((val & 0x100) != 0) {
                val ^= 0x11D;
            }
        }
    }

    private static int gfMul(int x, int y) {
        if (x == 0 || y == 0) return 0;
        return EXP[LOG[x] + LOG[y]];
    }

    private static int[] rsGeneratorPoly(int numEcBytes) {
        int[] poly = new int[]{1};
        for (int i = 0; i < numEcBytes; i++) {
            int factor = EXP[i];
            int[] next = new int[poly.length + 1];
            for (int j = 0; j < poly.length; j++) {
                next[j] ^= gfMul(poly[j], factor);
                next[j + 1] ^= poly[j];
            }
            poly = next;
        }
        return poly;
    }

    private static int[] rsCompute(int[] data, int numEcBytes) {
        int[] generator = rsGeneratorPoly(numEcBytes);
        int[] remainder = new int[numEcBytes];

        for (int i = 0; i < data.length; i++) {
            int factor = data[i] ^ remainder[0];
            System.arraycopy(remainder, 1, remainder, 0, numEcBytes - 1);
            remainder[numEcBytes - 1] = 0;
            for (int j = 0; j < numEcBytes; j++) {
                remainder[j] ^= gfMul(generator[j + 1], factor);
            }
        }
        return remainder;
    }

    /**
     * Determines QR Version based on text length (Version 1 to 10 byte mode, Error Correction Level M).
     */
    private static int getVersion(int dataLen) {
        int[] capacities = {14, 26, 42, 62, 84, 106, 122, 152, 180, 213};
        for (int v = 0; v < capacities.length; v++) {
            if (dataLen <= capacities[v]) {
                return v + 1;
            }
        }
        return 10;
    }

    private static int getDataCapacity(int version) {
        int[] capacities = {14, 26, 42, 62, 84, 106, 122, 152, 180, 213};
        if (version <= 0) return capacities[0];
        if (version > 10) return capacities[9];
        return capacities[version - 1];
    }

    private static int getEcCount(int version) {
        int[] ecCounts = {10, 16, 26, 36, 48, 64, 72, 88, 110, 130};
        if (version <= 0) return ecCounts[0];
        if (version > 10) return ecCounts[9];
        return ecCounts[version - 1];
    }

    /**
     * Encodes arbitrary string into high-resolution QR Bitmap.
     */
    public static Bitmap encode(String text, int targetSize) {
        if (text == null || text.trim().isEmpty()) {
            text = "https://example.com";
        }

        byte[] rawBytes = text.getBytes(StandardCharsets.UTF_8);
        int version = getVersion(rawBytes.length);
        int dataCapacity = getDataCapacity(version);
        int ecCapacity = getEcCount(version);

        // Bit buffer encoding: Mode Indicator (0100 for Byte Mode = 4 bits) + Character Count (8 bits for V1-9, 16 for V10)
        int countBits = (version <= 9) ? 8 : 16;
        int totalDataBits = dataCapacity * 8;
        boolean[] bitStream = new boolean[totalDataBits];
        int bitPos = 0;

        // Byte Mode indicator = 4 (0100)
        bitPos = writeBits(bitStream, bitPos, 4, 4);
        bitPos = writeBits(bitStream, bitPos, rawBytes.length, countBits);

        // Data payload
        for (int i = 0; i < rawBytes.length && bitPos + 8 <= totalDataBits; i++) {
            bitPos = writeBits(bitStream, bitPos, rawBytes[i] & 0xFF, 8);
        }

        // Terminator (up to 4 zeroes)
        int termLen = Math.min(4, totalDataBits - bitPos);
        bitPos = writeBits(bitStream, bitPos, 0, termLen);

        // Byte padding
        while ((bitPos % 8) != 0 && bitPos < totalDataBits) {
            bitStream[bitPos++] = false;
        }

        // Pad bytes: 0xEC and 0x11
        int[] padBytes = {0xEC, 0x11};
        int padIdx = 0;
        while (bitPos + 8 <= totalDataBits) {
            bitPos = writeBits(bitStream, bitPos, padBytes[padIdx % 2], 8);
            padIdx++;
        }

        // Convert bitstream to data bytes
        int[] dataWords = new int[dataCapacity];
        for (int i = 0; i < dataCapacity; i++) {
            int val = 0;
            for (int b = 0; b < 8; b++) {
                if (bitStream[i * 8 + b]) {
                    val |= (1 << (7 - b));
                }
            }
            dataWords[i] = val;
        }

        // Compute Reed-Solomon EC bytes
        int[] ecWords = rsCompute(dataWords, ecCapacity);

        // Interleave / Combine codewords
        int totalCodewords = dataCapacity + ecCapacity;
        int[] finalStream = new int[totalCodewords];
        System.arraycopy(dataWords, 0, finalStream, 0, dataCapacity);
        System.arraycopy(ecWords, 0, finalStream, dataCapacity, ecCapacity);

        // Dimension of matrix
        int moduleCount = 17 + 4 * version;
        int[][] matrix = new int[moduleCount][moduleCount];
        boolean[][] reserved = new boolean[moduleCount][moduleCount];

        // 1. Finder Patterns at 3 corners
        placeFinderPattern(matrix, reserved, 0, 0);
        placeFinderPattern(matrix, reserved, moduleCount - 7, 0);
        placeFinderPattern(matrix, reserved, 0, moduleCount - 7);

        // 2. Alignment pattern if Version >= 2
        if (version >= 2) {
            int alignPos = moduleCount - 7;
            placeAlignmentPattern(matrix, reserved, alignPos, alignPos);
        }

        // 3. Timing patterns
        for (int i = 8; i < moduleCount - 8; i++) {
            int val = (i % 2 == 0) ? 1 : 0;
            if (!reserved[6][i]) {
                matrix[6][i] = val;
                reserved[6][i] = true;
            }
            if (!reserved[i][6]) {
                matrix[i][6] = val;
                reserved[i][6] = true;
            }
        }

        // 4. Dark Module
        matrix[4 * version + 9][8] = 1;
        reserved[4 * version + 9][8] = true;

        // Reserve Format Information areas
        for (int i = 0; i < 9; i++) {
            reserved[8][i] = true;
            reserved[i][8] = true;
        }
        for (int i = moduleCount - 8; i < moduleCount; i++) {
            reserved[8][i] = true;
            reserved[i][8] = true;
        }

        // 5. Place Data Bits in 2-column zigzag traversal
        boolean[] fullBitStream = new boolean[totalCodewords * 8];
        for (int i = 0; i < totalCodewords; i++) {
            for (int b = 0; b < 8; b++) {
                fullBitStream[i * 8 + b] = ((finalStream[i] >> (7 - b)) & 1) == 1;
            }
        }

        int streamIdx = 0;
        int dir = -1; // Going up
        int c = moduleCount - 1;
        int r = moduleCount - 1;

        while (c > 0) {
            if (c == 6) c--; // Skip vertical timing column
            for (int i = 0; i < moduleCount; i++) {
                int row = (dir == -1) ? (moduleCount - 1 - i) : i;
                for (int colOffset = 0; colOffset < 2; colOffset++) {
                    int col = c - colOffset;
                    if (!reserved[row][col]) {
                        boolean bit = false;
                        if (streamIdx < fullBitStream.length) {
                            bit = fullBitStream[streamIdx++];
                        }
                        // Apply Mask Pattern 0: (row + col) % 2 == 0
                        boolean mask = ((row + col) % 2 == 0);
                        matrix[row][col] = (bit ^ mask) ? 1 : 0;
                    }
                }
            }
            dir = -dir;
            c -= 2;
        }

        // 6. Write Format Info: EC Level M (00) + Mask 0 (000) = Format bits 101010000010010
        int formatInfo = 0x5412; // Masked value for EC Level M, Mask 0
        writeFormatInfo(matrix, formatInfo, moduleCount);

        // 7. Render to Android Bitmap with clean quiet zone
        int quietZone = 4;
        int totalGrid = moduleCount + quietZone * 2;
        int scale = Math.max(1, targetSize / totalGrid);
        int finalDim = totalGrid * scale;

        Bitmap bitmap = Bitmap.createBitmap(finalDim, finalDim, Bitmap.Config.ARGB_8888);
        int[] pixels = new int[finalDim * finalDim];
        Arrays.fill(pixels, Color.WHITE);

        for (int y = 0; y < moduleCount; y++) {
            for (int x = 0; x < moduleCount; x++) {
                if (matrix[y][x] == 1) {
                    int startX = (x + quietZone) * scale;
                    int startY = (y + quietZone) * scale;
                    for (int py = 0; py < scale; py++) {
                        int rowOffset = (startY + py) * finalDim;
                        for (int px = 0; px < scale; px++) {
                            pixels[rowOffset + (startX + px)] = Color.BLACK;
                        }
                    }
                }
            }
        }

        bitmap.setPixels(pixels, 0, finalDim, 0, 0, finalDim, finalDim);
        return bitmap;
    }

    private static int writeBits(boolean[] dest, int startPos, int value, int bitCount) {
        for (int i = bitCount - 1; i >= 0; i--) {
            if (startPos < dest.length) {
                dest[startPos++] = ((value >> i) & 1) == 1;
            }
        }
        return startPos;
    }

    private static void placeFinderPattern(int[][] matrix, boolean[][] reserved, int x, int y) {
        for (int r = 0; r < 7; r++) {
            for (int c = 0; c < 7; c++) {
                if (r == 0 || r == 6 || c == 0 || c == 6 || (r >= 2 && r <= 4 && c >= 2 && c <= 4)) {
                    matrix[y + r][x + c] = 1;
                } else {
                    matrix[y + r][x + c] = 0;
                }
                reserved[y + r][x + c] = true;
            }
        }
        // Separators around finder pattern
        for (int r = -1; r <= 7; r++) {
            for (int c = -1; c <= 7; c++) {
                int py = y + r;
                int px = x + c;
                if (py >= 0 && py < matrix.length && px >= 0 && px < matrix.length) {
                    if (!reserved[py][px]) {
                        matrix[py][px] = 0;
                        reserved[py][px] = true;
                    }
                }
            }
        }
    }

    private static void placeAlignmentPattern(int[][] matrix, boolean[][] reserved, int x, int y) {
        for (int r = -2; r <= 2; r++) {
            for (int c = -2; c <= 2; c++) {
                int py = y + r;
                int px = x + c;
                if (py >= 0 && py < matrix.length && px >= 0 && px < matrix.length) {
                    if (Math.abs(r) == 2 || Math.abs(c) == 2 || (r == 0 && c == 0)) {
                        matrix[py][px] = 1;
                    } else {
                        matrix[py][px] = 0;
                    }
                    reserved[py][px] = true;
                }
            }
        }
    }

    private static void writeFormatInfo(int[][] matrix, int formatInfo, int size) {
        // Top-left copy
        int[] positions = {0, 1, 2, 3, 4, 5, 7, 8};
        for (int i = 0; i < 6; i++) {
            matrix[8][positions[i]] = (formatInfo >> i) & 1;
        }
        matrix[8][7] = (formatInfo >> 6) & 1;
        matrix[8][8] = (formatInfo >> 7) & 1;
        matrix[7][8] = (formatInfo >> 8) & 1;
        for (int i = 9; i < 15; i++) {
            matrix[14 - i][8] = (formatInfo >> i) & 1;
        }

        // Split copy at corners
        for (int i = 0; i < 7; i++) {
            matrix[size - 1 - i][8] = (formatInfo >> i) & 1;
        }
        for (int i = 7; i < 15; i++) {
            matrix[8][size - 15 + i] = (formatInfo >> i) & 1;
        }
    }
}