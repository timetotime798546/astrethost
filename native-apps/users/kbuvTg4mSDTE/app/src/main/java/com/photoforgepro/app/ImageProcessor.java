package com.photoforgepro.app;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;

public class ImageProcessor {

    public static ColorMatrix buildColorMatrix(EditState state) {
        ColorMatrix cmTotal = new ColorMatrix();

        // 1. Exposure calculation (Deterministic local calculation: exposure * 2.55)
        float expOffset = state.exposure * 2.55f;
        ColorMatrix expMatrix = new ColorMatrix(new float[]{
                1f, 0f, 0f, 0f, expOffset,
                0f, 1f, 0f, 0f, expOffset,
                0f, 0f, 1f, 0f, expOffset,
                0f, 0f, 0f, 1f, 0f
        });
        cmTotal.postConcat(expMatrix);

        // 2. Contrast calculation: scale = (contrast + 100) / 100
        float cScale = (state.contrast + 100.0f) / 100.0f;
        float cTranslate = (-0.5f * cScale + 0.5f) * 255.0f;
        ColorMatrix contrastMatrix = new ColorMatrix(new float[]{
                cScale, 0f, 0f, 0f, cTranslate,
                0f, cScale, 0f, 0f, cTranslate,
                0f, 0f, cScale, 0f, cTranslate,
                0f, 0f, 0f, 1f, 0f
        });
        cmTotal.postConcat(contrastMatrix);

        // 3. Saturation calculation: sat = (saturation + 100) / 100
        float satScale = (state.saturation + 100.0f) / 100.0f;
        ColorMatrix satMatrix = new ColorMatrix();
        satMatrix.setSaturation(Math.max(0.0f, satScale));
        cmTotal.postConcat(satMatrix);

        // 4. Warmth / Temperature Adjustment
        if (Math.abs(state.warmth) > 0.01f) {
            float rShift = state.warmth * 0.7f;
            float bShift = -state.warmth * 0.7f;
            ColorMatrix tempMatrix = new ColorMatrix(new float[]{
                    1f, 0f, 0f, 0f, rShift,
                    0f, 1f, 0f, 0f, 0f,
                    0f, 0f, 1f, 0f, bShift,
                    0f, 0f, 0f, 1f, 0f
            });
            cmTotal.postConcat(tempMatrix);
        }

        // 5. Highlights and Shadows
        float hsOffset = (state.highlights * 0.4f) + (state.shadows * 0.3f);
        if (Math.abs(hsOffset) > 0.01f) {
            ColorMatrix hsMatrix = new ColorMatrix(new float[]{
                    1f, 0f, 0f, 0f, hsOffset,
                    0f, 1f, 0f, 0f, hsOffset,
                    0f, 0f, 1f, 0f, hsOffset,
                    0f, 0f, 0f, 1f, 0f
            });
            cmTotal.postConcat(hsMatrix);
        }

        // 6. Cinematic Presets
        if ("Cinematic".equalsIgnoreCase(state.activePreset)) {
            ColorMatrix cine = new ColorMatrix(new float[]{
                    1.1f, 0f, 0f, 0f, 10f,
                    0f, 1.05f, 0f, 0f, 5f,
                    0f, 0f, 1.25f, 0f, -15f,
                    0f, 0f, 0f, 1f, 0f
            });
            cmTotal.postConcat(cine);
        } else if ("Noir".equalsIgnoreCase(state.activePreset)) {
            ColorMatrix mono = new ColorMatrix();
            mono.setSaturation(0.0f);
            ColorMatrix highContrast = new ColorMatrix(new float[]{
                    1.35f, 0f, 0f, 0f, -35f,
                    0f, 1.35f, 0f, 0f, -35f,
                    0f, 0f, 1.35f, 0f, -35f,
                    0f, 0f, 0f, 1f, 0f
            });
            mono.postConcat(highContrast);
            cmTotal.postConcat(mono);
        } else if ("Cyberpunk".equalsIgnoreCase(state.activePreset)) {
            ColorMatrix cyber = new ColorMatrix(new float[]{
                    1.3f, 0f, 0.2f, 0f, 20f,
                    0f, 0.9f, 0.3f, 0f, -10f,
                    0.4f, 0f, 1.4f, 0f, 30f,
                    0f, 0f, 0f, 1f, 0f
            });
            cmTotal.postConcat(cyber);
        } else if ("GoldenHour".equalsIgnoreCase(state.activePreset)) {
            ColorMatrix golden = new ColorMatrix(new float[]{
                    1.25f, 0f, 0f, 0f, 25f,
                    0f, 1.1f, 0f, 0f, 12f,
                    0f, 0f, 0.85f, 0f, -20f,
                    0f, 0f, 0f, 1f, 0f
            });
            cmTotal.postConcat(golden);
        }

        return cmTotal;
    }

    public static Bitmap renderFinalBitmap(Bitmap source, EditState state) {
        if (source == null) return null;

        Bitmap rotated = source;
        if (state.rotation != 0) {
            Matrix m = new Matrix();
            m.postRotate(state.rotation);
            rotated = Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), m, true);
        }

        Bitmap output = Bitmap.createBitmap(rotated.getWidth(), rotated.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        ColorMatrix matrix = buildColorMatrix(state);
        paint.setColorFilter(new ColorMatrixColorFilter(matrix));

        canvas.drawBitmap(rotated, 0, 0, paint);

        // Apply Vignette if enabled
        if (state.vignette > 0.0f) {
            Paint vigPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            float cx = output.getWidth() / 2.0f;
            float cy = output.getHeight() / 2.0f;
            float radius = (float) Math.hypot(cx, cy);
            int darkAlpha = (int) (Math.min(state.vignette * 2.2f, 220f));

            RadialGradient gradient = new RadialGradient(
                    cx, cy, radius,
                    new int[]{Color.TRANSPARENT, Color.argb(darkAlpha, 0, 0, 0)},
                    new float[]{0.55f, 1.0f},
                    Shader.TileMode.CLAMP
            );
            vigPaint.setShader(gradient);
            canvas.drawRect(0, 0, output.getWidth(), output.getHeight(), vigPaint);
        }

        return output;
    }

    public static Bitmap generateDefaultStudioBitmap() {
        int w = 1200;
        int h = 900;
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);

        // Studio cinematic sunset gradient landscape background
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        for (int y = 0; y < h; y++) {
            float ratio = (float) y / h;
            int r = (int) (20 + 200 * (1.0f - ratio));
            int g = (int) (30 + 100 * Math.sin(ratio * Math.PI));
            int b = (int) (80 + 120 * ratio);
            p.setColor(Color.rgb(Math.min(255, r), Math.min(255, g), Math.min(255, b)));
            canvas.drawLine(0, y, w, y, p);
        }

        // Draw mountain silhouette
        Paint mountainPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mountainPaint.setColor(Color.rgb(15, 18, 25));
        android.graphics.Path path = new android.graphics.Path();
        path.moveTo(0, h);
        path.lineTo(0, h * 0.65f);
        path.lineTo(w * 0.25f, h * 0.42f);
        path.lineTo(w * 0.45f, h * 0.60f);
        path.lineTo(w * 0.70f, h * 0.35f);
        path.lineTo(w, h * 0.70f);
        path.lineTo(w, h);
        path.close();
        canvas.drawPath(path, mountainPaint);

        // Foreground reflection / lake
        Paint lakePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        lakePaint.setColor(Color.argb(160, 10, 15, 30));
        canvas.drawRect(0, h * 0.75f, w, h, lakePaint);

        // Studio watermark title on test sample
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.argb(220, 255, 255, 255));
        textPaint.setTextSize(36f);
        textPaint.setFakeBoldText(true);
        canvas.drawText("PhotoForge Pro · RAW Master 4K", 50, h * 0.90f, textPaint);

        return bmp;
    }
}