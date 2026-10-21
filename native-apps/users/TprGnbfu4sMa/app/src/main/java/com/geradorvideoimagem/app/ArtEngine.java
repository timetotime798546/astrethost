package com.geradorvideoimagem.app;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import java.util.Random;

public class ArtEngine {

    public static Bitmap generateArtwork(String prompt, String style, int width, int height, float animProgress) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        long seed = (long) prompt.toLowerCase().trim().hashCode() + 999983L;
        Random random = new Random(seed);

        int styleCode = 0;
        if ("Neon Cyberpunk".equalsIgnoreCase(style)) {
            styleCode = 1;
        } else if ("Cosmic Nebula".equalsIgnoreCase(style)) {
            styleCode = 2;
        } else if ("Natureza Fantasia".equalsIgnoreCase(style)) {
            styleCode = 3;
        } else if ("Abstrato Futurista".equalsIgnoreCase(style)) {
            styleCode = 4;
        }

        renderBackground(canvas, width, height, styleCode, animProgress, random);
        renderLandscapeOrForms(canvas, width, height, styleCode, animProgress, random);
        renderParticlesAndStars(canvas, width, height, styleCode, animProgress, random);
        renderPromptOverlayWatermark(canvas, width, height, prompt, style);

        return bitmap;
    }

    private static void renderBackground(Canvas canvas, int width, int height, int styleCode, float animProgress, Random random) {
        int colorTop;
        int colorBottom;

        switch (styleCode) {
            case 1: // Neon Cyberpunk
                colorTop = Color.rgb(10, 5, 30);
                colorBottom = Color.rgb(40, 5, 70);
                break;
            case 2: // Cosmic Nebula
                colorTop = Color.rgb(5, 5, 20);
                colorBottom = Color.rgb(20, 10, 50);
                break;
            case 3: // Natureza Fantasia
                colorTop = Color.rgb(15, 35, 30);
                colorBottom = Color.rgb(45, 85, 60);
                break;
            default: // Abstrato Futurista
                colorTop = Color.rgb(20, 20, 35);
                colorBottom = Color.rgb(10, 15, 25);
                break;
        }

        Paint bgPaint = new Paint();
        LinearGradient gradient = new LinearGradient(
                0, 0,
                0, height,
                colorTop, colorBottom,
                Shader.TileMode.CLAMP
        );
        bgPaint.setShader(gradient);
        canvas.drawRect(0, 0, width, height, bgPaint);

        // Sun or Energy Core Orb
        Paint orbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        float orbX = width * 0.5f + (float) Math.sin(animProgress * Math.PI * 2) * (width * 0.1f);
        float orbY = height * 0.42f - (float) Math.cos(animProgress * Math.PI * 2) * (height * 0.05f);
        float orbRadius = Math.min(width, height) * 0.22f;

        int coreCenterColor = styleCode == 1 ? Color.rgb(255, 60, 150) :
                (styleCode == 2 ? Color.rgb(0, 240, 255) : Color.rgb(255, 215, 0));
        int coreOuterColor = Color.TRANSPARENT;

        RadialGradient orbGradient = new RadialGradient(
                orbX, orbY, orbRadius * 1.5f,
                new int[]{Color.WHITE, coreCenterColor, coreOuterColor},
                new float[]{0.0f, 0.45f, 1.0f},
                Shader.TileMode.CLAMP
        );
        orbPaint.setShader(orbGradient);
        canvas.drawCircle(orbX, orbY, orbRadius * 1.5f, orbPaint);
    }

    private static void renderLandscapeOrForms(Canvas canvas, int width, int height, int styleCode, float animProgress, Random random) {
        Paint layerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        // Cyber Grid or Mountain Waves
        int layers = 4;
        for (int l = 1; l <= layers; l++) {
            Path path = new Path();
            float baseY = height * (0.50f + (l * 0.11f));
            path.moveTo(0, height);
            path.lineTo(0, baseY);

            int segments = 12;
            float step = (float) width / segments;
            for (int i = 0; i <= segments; i++) {
                float px = i * step;
                float waveOffset = (float) Math.sin((i * 0.7f) + (l * 1.5f) + (animProgress * 6.28f)) * (height * 0.04f / l);
                float py = baseY + waveOffset;
                path.lineTo(px, py);
            }
            path.lineTo(width, height);
            path.close();

            int alpha = 140 + (l * 28);
            if (alpha > 255) alpha = 255;

            int color;
            if (styleCode == 1) {
                color = Color.argb(alpha, 80 + (l * 30), 20, 120 + (l * 25));
            } else if (styleCode == 2) {
                color = Color.argb(alpha, 15, 40 + (l * 30), 100 + (l * 30));
            } else {
                color = Color.argb(alpha, 25 + (l * 35), 70 + (l * 30), 50 + (l * 20));
            }

            layerPaint.setColor(color);
            layerPaint.setStyle(Paint.Style.FILL);
            canvas.drawPath(path, layerPaint);

            // Draw glowing outline edge
            Paint edgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            edgePaint.setStyle(Paint.Style.STROKE);
            edgePaint.setStrokeWidth(3f);
            edgePaint.setColor(styleCode == 1 ? Color.CYAN : Color.MAGENTA);
            canvas.drawPath(path, edgePaint);
        }
    }

    private static void renderParticlesAndStars(Canvas canvas, int width, int height, int styleCode, float animProgress, Random random) {
        Paint starPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        starPaint.setColor(Color.WHITE);

        int count = 90;
        for (int i = 0; i < count; i++) {
            float x = random.nextFloat() * width;
            float initialY = random.nextFloat() * (height * 0.75f);
            float y = (initialY + (animProgress * height * 0.2f)) % (height * 0.75f);

            float radius = 1.2f + (random.nextFloat() * 2.8f);
            int starAlpha = 120 + random.nextInt(135);
            starPaint.setAlpha(starAlpha);

            if (i % 6 == 0) {
                starPaint.setColor(styleCode == 1 ? Color.rgb(0, 255, 255) : Color.rgb(255, 180, 220));
            } else {
                starPaint.setColor(Color.WHITE);
            }
            canvas.drawCircle(x, y, radius, starPaint);
        }
    }

    private static void renderPromptOverlayWatermark(Canvas canvas, int width, int height, String prompt, String style) {
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(Math.max(22f, width * 0.045f));
        textPaint.setFakeBoldText(true);
        textPaint.setShadowLayer(8f, 0, 3f, Color.BLACK);

        String display = prompt != null && prompt.trim().length() > 0 ? prompt.trim() : "AI Visual Art";
        if (display.length() > 32) {
            display = display.substring(0, 29) + "...";
        }

        canvas.drawText("✨ " + display, 24f, height - 52f, textPaint);

        Paint subPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        subPaint.setColor(Color.rgb(0, 255, 200));
        subPaint.setTextSize(Math.max(16f, width * 0.032f));
        canvas.drawText("Estilo: " + style + " • Sem Limites MP4/HD", 24f, height - 20f, subPaint);
    }
}