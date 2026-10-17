package com.docscanpro.app;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;

public class ImageProcessingHelper {

    public static final int FILTER_ORIGINAL = 0;
    public static final int FILTER_GRAYSCALE = 1;
    public static final int FILTER_MAGIC_COLOR = 2;
    public static final int FILTER_HIGH_CONTRAST_BW = 3;

    public static Bitmap rotateBitmap(Bitmap source, float angle) {
        if (source == null) return null;
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    public static Bitmap applyFilter(Bitmap source, int filterType) {
        if (source == null) return null;

        if (filterType == FILTER_ORIGINAL) {
            return source.copy(Bitmap.Config.ARGB_8888, true);
        }

        Bitmap output = Bitmap.createBitmap(source.getWidth(), source.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint();

        ColorMatrix cm = new ColorMatrix();

        if (filterType == FILTER_GRAYSCALE) {
            cm.setSaturation(0);
            paint.setColorFilter(new ColorMatrixColorFilter(cm));
        } else if (filterType == FILTER_MAGIC_COLOR) {
            // Enhanced contrast + slight brightness + saturated color for crisp text and vivid graphics
            float contrast = 1.4f;
            float brightness = 10f;
            float[] colorTransform = {
                contrast, 0, 0, 0, brightness,
                0, contrast, 0, 0, brightness,
                0, 0, contrast, 0, brightness,
                0, 0, 0, 1, 0
            };
            cm.set(colorTransform);
            paint.setColorFilter(new ColorMatrixColorFilter(cm));
        } else if (filterType == FILTER_HIGH_CONTRAST_BW) {
            // Document B&W text sharpener: greyscale + high-contrast thresholding
            ColorMatrix grayMatrix = new ColorMatrix();
            grayMatrix.setSaturation(0);

            float contrast = 2.0f;
            float brightness = -60f;
            float[] thresholdMatrix = {
                contrast, 0, 0, 0, brightness,
                0, contrast, 0, 0, brightness,
                0, 0, contrast, 0, brightness,
                0, 0, 0, 1, 0
            };
            ColorMatrix threshCm = new ColorMatrix(thresholdMatrix);
            threshCm.preConcat(grayMatrix);
            paint.setColorFilter(new ColorMatrixColorFilter(threshCm));
        }

        canvas.drawBitmap(source, 0, 0, paint);
        return output;
    }

    public static Bitmap cropBitmap(Bitmap src, int left, int top, int width, int height) {
        if (src == null) return null;
        int safeLeft = Math.max(0, left);
        int safeTop = Math.max(0, top);
        int safeWidth = Math.min(width, src.getWidth() - safeLeft);
        int safeHeight = Math.min(height, src.getHeight() - safeTop);
        if (safeWidth <= 0 || safeHeight <= 0) {
            return src;
        }
        return Bitmap.createBitmap(src, safeLeft, safeTop, safeWidth, safeHeight);
    }
}