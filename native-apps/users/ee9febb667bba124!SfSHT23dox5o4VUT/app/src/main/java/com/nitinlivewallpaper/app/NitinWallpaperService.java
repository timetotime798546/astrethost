package com.nitinlivewallpaper.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.Handler;
import android.os.SystemClock;
import android.service.wallpaper.WallpaperService;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class NitinWallpaperService extends WallpaperService {

    @Override
    public Engine onCreateEngine() {
        return new NitinEngine();
    }

    private class NitinEngine extends Engine {
        private final Handler handler = new Handler();
        private final Runnable drawRunnable = new Runnable() {
            @Override
            public void run() {
                draw();
            }
        };
        private boolean visible = false;
        private final List<Particle> particles = new ArrayList<>();
        private final List<TouchRipple> ripples = new ArrayList<>();
        private final Random random = new Random();
        
        // Settings loaded from SharedPreferences
        private String wallpaperText = "नितिन";
        private int colorScheme = 0; // 0: Neon Blue, 1: Sunset Rose, 2: Cyber Gold, 3: Mystic Purple, 4: Mint Aurora
        private int animStyle = 0;  // 0: Pulsing Rotation, 1: Bounce & Spin, 2: Kinetic Wave, 3: Elegant Float
        private int particleStyle = 0; // 0: Cosmic Sparkles, 1: Rising Hearts, 2: Soft Bubbles, 3: None
        private boolean interactiveRipples = true;
        private int particleSpeed = 1; // 0: Slow, 1: Medium, 2: Fast
        private int textSizeMultiplier = 100; // custom size 60-150%

        NitinEngine() {
            // Load settings initially
            loadSettings();
        }

        private void loadSettings() {
            SharedPreferences prefs = getSharedPreferences("nitin_wallpaper_prefs", Context.MODE_PRIVATE);
            wallpaperText = prefs.getString("wallpaper_text", "नितिन");
            colorScheme = prefs.getInt("color_scheme", 0);
            animStyle = prefs.getInt("anim_style", 0);
            particleStyle = prefs.getInt("particle_style", 0);
            interactiveRipples = prefs.getBoolean("interactive_ripples", true);
            particleSpeed = prefs.getInt("particle_speed", 1);
            textSizeMultiplier = prefs.getInt("text_size_mult", 100);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            this.visible = visible;
            if (visible) {
                loadSettings();
                initParticles();
                draw();
            } else {
                handler.removeCallbacks(drawRunnable);
            }
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            this.visible = false;
            handler.removeCallbacks(drawRunnable);
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            initParticles();
            draw();
        }

        @Override
        public void onTouchEvent(MotionEvent event) {
            if (interactiveRipples && event.getAction() == MotionEvent.ACTION_DOWN) {
                ripples.add(new TouchRipple(event.getX(), event.getY()));
                // Add burst particles at touch point
                int count = 10;
                for (int i = 0; i < count; i++) {
                    particles.add(new Particle(event.getX(), event.getY(), random, true));
                }
            }
            super.onTouchEvent(event);
        }

        private void initParticles() {
            particles.clear();
            if (particleStyle == 3) return; // None
            
            SurfaceHolder holder = getSurfaceHolder();
            int width = holder.getSurfaceFrame().width();
            int height = holder.getSurfaceFrame().height();
            if (width <= 0 || height <= 0) return;

            int count = 40;
            for (int i = 0; i < count; i++) {
                particles.add(new Particle(random.nextFloat() * width, random.nextFloat() * height, random, false));
            }
        }

        private void draw() {
            SurfaceHolder holder = getSurfaceHolder();
            Canvas canvas = null;
            try {
                canvas = holder.lockCanvas();
                if (canvas != null) {
                    drawFrame(canvas);
                }
            } finally {
                if (canvas != null) {
                    holder.unlockCanvasAndPost(canvas);
                }
            }

            if (visible) {
                handler.postDelayed(drawRunnable, 20); // target ~50 FPS
            }
        }

        private void drawFrame(Canvas canvas) {
            int width = canvas.getWidth();
            int height = canvas.getHeight();

            // 1. Draw Animated Gradient Background
            drawGradientBackground(canvas, width, height);

            // 2. Draw & Update Particles
            drawAndUpdateParticles(canvas, width, height);

            // 3. Draw & Update Touch Ripples
            drawAndUpdateRipples(canvas);

            // 4. Draw Interactive Floating Glowing Text
            drawFloatingText(canvas, width, height);
        }

        private void drawGradientBackground(Canvas canvas, int width, int height) {
            Paint bgPaint = new Paint();
            long time = SystemClock.uptimeMillis();
            float pulse = (float) Math.sin(time * 0.0005) * 0.2f; // subtle cycling animation

            int colorStart, colorMiddle, colorEnd;
            switch (colorScheme) {
                case 1: // Sunset Rose
                    colorStart = Color.rgb((int)(120 + pulse * 50), 30, 80);
                    colorMiddle = Color.rgb(40, 10, 50);
                    colorEnd = Color.rgb(10, 5, 30);
                    break;
                case 2: // Cyber Gold
                    colorStart = Color.rgb((int)(140 + pulse * 40), (int)(110 + pulse * 30), 20);
                    colorMiddle = Color.rgb(40, 30, 5);
                    colorEnd = Color.rgb(15, 10, 2);
                    break;
                case 3: // Cosmic Purple
                    colorStart = Color.rgb(80, (int)(10 + pulse * 10), 120);
                    colorMiddle = Color.rgb(20, 5, 50);
                    colorEnd = Color.rgb(5, 2, 20);
                    break;
                case 4: // Mint Aurora
                    colorStart = Color.rgb(10, (int)(100 + pulse * 40), 110);
                    colorMiddle = Color.rgb(5, 30, 45);
                    colorEnd = Color.rgb(2, 10, 20);
                    break;
                case 0: // Neon Blue (Default)
                default:
                    colorStart = Color.rgb(15, 45, (int)(140 + pulse * 50));
                    colorMiddle = Color.rgb(5, 15, 50);
                    colorEnd = Color.rgb(2, 5, 25);
                    break;
            }

            RadialGradient gradient = new RadialGradient(
                    width / 2f, height / 2f, 
                    Math.max(width, height) * 0.8f,
                    new int[]{colorStart, colorMiddle, colorEnd}, 
                    null, 
                    Shader.TileMode.CLAMP
            );
            bgPaint.setShader(gradient);
            canvas.drawRect(0, 0, width, height, bgPaint);
        }

        private void drawAndUpdateParticles(Canvas canvas, int width, int height) {
            if (particleStyle == 3) return;

            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            float speedScale = 1.0f;
            if (particleSpeed == 0) speedScale = 0.5f;
            else if (particleSpeed == 2) speedScale = 2.0f;

            for (int i = particles.size() - 1; i >= 0; i--) {
                Particle p = particles.get(i);
                p.update(width, height, speedScale);

                paint.setColor(p.color);
                paint.setAlpha((int) (p.alpha * 255));

                if (particleStyle == 1) {
                    // Heart shape for Rising Hearts
                    drawHeart(canvas, p.x, p.y, p.size, paint);
                } else if (particleStyle == 2) {
                    // Soft translucent Bubble Orbs
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(2f);
                    canvas.drawCircle(p.x, p.y, p.size, paint);
                    paint.setStyle(Paint.Style.FILL);
                    paint.setAlpha((int) (p.alpha * 50));
                    canvas.drawCircle(p.x, p.y, p.size * 0.8f, paint);
                } else {
                    // Cosmic Sparkles (Glow Stars)
                    drawSparkle(canvas, p.x, p.y, p.size, paint);
                }

                if (p.isBurst && p.alpha <= 0) {
                    particles.remove(i);
                }
            }
        }

        private void drawSparkle(Canvas canvas, float x, float y, float size, Paint paint) {
            Path star = new Path();
            star.moveTo(x, y - size);
            star.quadTo(x, y, x + size, y);
            star.quadTo(x, y, x, y + size);
            star.quadTo(x, y, x - size, y);
            star.quadTo(x, y, x, y - size);
            canvas.drawPath(star, paint);
        }

        private void drawHeart(Canvas canvas, float x, float y, float size, Paint paint) {
            Path path = new Path();
            float width = size * 2;
            float height = size * 2;
            path.moveTo(x, y + height / 4);
            path.cubicTo(x, y - height / 4, x - width / 2, y - height / 4, x - width / 2, y + height / 4);
            path.cubicTo(x - width / 2, y + height * 0.6f, x, y + height, x, y + height);
            path.cubicTo(x, y + height, x + width / 2, y + height * 0.6f, x + width / 2, y + height / 4);
            path.cubicTo(x + width / 2, y - height / 4, x, y - height / 4, x, y + height / 4);
            canvas.drawPath(path, paint);
        }

        private void drawAndUpdateRipples(Canvas canvas) {
            if (ripples.isEmpty()) return;

            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setStyle(Paint.Style.STROKE);

            for (int i = ripples.size() - 1; i >= 0; i--) {
                TouchRipple r = ripples.get(i);
                r.update();

                if (r.alpha <= 0) {
                    ripples.remove(i);
                    continue;
                }

                paint.setStrokeWidth(5f * (1f - r.progress));
                paint.setColor(getSchemeAccentColor());
                paint.setAlpha((int) (r.alpha * 255));
                
                canvas.drawCircle(r.x, r.y, r.radius, paint);
            }
        }

        private int getSchemeAccentColor() {
            switch (colorScheme) {
                case 1: return Color.rgb(255, 100, 180);
                case 2: return Color.rgb(255, 220, 80);
                case 3: return Color.rgb(255, 80, 255);
                case 4: return Color.rgb(80, 255, 200);
                case 0:
                default: return Color.rgb(80, 200, 255);
            }
        }

        private void drawFloatingText(Canvas canvas, int width, int height) {
            long time = SystemClock.uptimeMillis();
            
            float cx = width / 2f;
            float cy = height / 2f;

            float tx = 0;
            float ty = 0;
            float scale = 1.0f;
            float angle = 0;

            switch (animStyle) {
                case 1:
                    double bounceAngle = time * 0.003;
                    ty = (float) Math.sin(bounceAngle) * 50f;
                    angle = (float) (Math.sin(time * 0.0008) * 15f);
                    scale = 1.0f + (float) Math.sin(time * 0.004) * 0.08f;
                    break;
                case 2:
                    tx = (float) Math.sin(time * 0.0015) * 60f;
                    ty = (float) Math.cos(time * 0.002) * 30f;
                    scale = 1.0f + (float) Math.sin(time * 0.003) * 0.12f;
                    angle = (float) Math.cos(time * 0.001) * 8f;
                    break;
                case 3:
                    tx = (float) Math.cos(time * 0.001) * 40f;
                    ty = (float) Math.sin(time * 0.001) * 40f;
                    scale = 0.95f + (float) Math.sin(time * 0.002) * 0.05f;
                    break;
                case 0:
                default:
                    scale = 1.0f + (float) Math.sin(time * 0.002) * 0.15f;
                    angle = (float) (Math.sin(time * 0.0005) * 20f);
                    ty = (float) Math.sin(time * 0.001) * 20f;
                    break;
            }

            canvas.save();
            canvas.translate(cx + tx, cy + ty);
            canvas.rotate(angle);
            canvas.scale(scale, scale);

            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setFakeBoldText(true);
            paint.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD));

            float baseSize = width * 0.18f;
            float adjustedSize = baseSize * (textSizeMultiplier / 100f);
            paint.setTextSize(adjustedSize);

            int shadowColor = getSchemeAccentColor();
            paint.setShadowLayer(40f, 0, 0, shadowColor);

            // Glow stroke
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(20f);
            paint.setColor(Color.argb(100, Color.red(shadowColor), Color.green(shadowColor), Color.blue(shadowColor)));
            canvas.drawText(wallpaperText, 0, adjustedSize / 3f, paint);

            // White border stroke
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(8f);
            paint.setColor(Color.WHITE);
            canvas.drawText(wallpaperText, 0, adjustedSize / 3f, paint);

            // Foreground Gradient fill
            paint.setStyle(Paint.Style.FILL);
            paint.clearShadowLayer();

            int gradientColorStart = Color.WHITE;
            int gradientColorEnd = getSchemeAccentColor();

            LinearGradient textShader = new LinearGradient(
                    0, -adjustedSize, 0, adjustedSize,
                    new int[]{gradientColorStart, gradientColorEnd},
                    null, Shader.TileMode.CLAMP
            );
            paint.setShader(textShader);

            canvas.drawText(wallpaperText, 0, adjustedSize / 3f, paint);
            canvas.restore();
        }
    }

    private static class Particle {
        float x, y;
        float vx, vy;
        float size;
        float alpha;
        int color;
        boolean isBurst;
        float decaySpeed;

        Particle(float startX, float startY, Random rand, boolean burst) {
            x = startX;
            y = startY;
            isBurst = burst;

            if (burst) {
                double angle = rand.nextDouble() * 2 * Math.PI;
                float speed = 5f + rand.nextFloat() * 15f;
                vx = (float) (Math.cos(angle) * speed);
                vy = (float) (Math.sin(angle) * speed);
                size = 8f + rand.nextFloat() * 16f;
                alpha = 1.0f;
                decaySpeed = 0.015f + rand.nextFloat() * 0.02f;
            } else {
                vx = (rand.nextFloat() - 0.5f) * 1.5f;
                vy = - (0.5f + rand.nextFloat() * 2.0f);
                size = 10f + rand.nextFloat() * 20f;
                alpha = 0.2f + rand.nextFloat() * 0.6f;
                decaySpeed = 0f;
            }

            int hueChoice = rand.nextInt(5);
            switch (hueChoice) {
                case 0: color = Color.rgb(255, 120, 180); break;
                case 1: color = Color.rgb(100, 200, 255); break;
                case 2: color = Color.rgb(150, 255, 180); break;
                case 3: color = Color.rgb(255, 230, 100); break;
                case 4: default: color = Color.rgb(220, 180, 255); break;
            }
        }

        void update(int width, int height, float speedScale) {
            x += vx * speedScale;
            y += vy * speedScale;

            if (isBurst) {
                alpha -= decaySpeed * speedScale;
                if (alpha < 0) alpha = 0;
            } else {
                if (y < -size * 2) {
                    y = height + size * 2;
                    x = new Random().nextFloat() * width;
                }
                if (x < -size * 2) x = width + size * 2;
                if (x > width + size * 2) x = -size * 2;
            }
        }
    }

    private static class TouchRipple {
        float x, y;
        float radius;
        float alpha;
        float progress;

        TouchRipple(float startX, float startY) {
            x = startX;
            y = startY;
            radius = 10f;
            alpha = 1.0f;
            progress = 0f;
        }

        void update() {
            progress += 0.03f;
            if (progress > 1f) {
                progress = 1f;
                alpha = 0f;
            } else {
                radius = 10f + progress * 240f;
                alpha = 1.0f - progress;
            }
        }
    }
}