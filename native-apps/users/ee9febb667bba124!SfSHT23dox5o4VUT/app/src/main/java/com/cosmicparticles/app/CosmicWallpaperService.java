package com.cosmicparticles.app;

import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Shader;
import android.os.Handler;
import android.os.Looper;
import android.service.wallpaper.WallpaperService;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CosmicWallpaperService extends WallpaperService {

    @Override
    public Engine onCreateEngine() {
        return new CosmicEngine();
    }

    private static class Particle {
        float x, y;
        float vx, vy;
        float size;
        int color;
        float life;
        boolean isTouchSpawned;

        Particle(float x, float y, float vx, float vy, float size, int color, boolean isTouchSpawned) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.size = size;
            this.color = color;
            this.isTouchSpawned = isTouchSpawned;
            this.life = 1.0f;
        }
    }

    private class CosmicEngine extends Engine {
        private final Handler handler = new Handler(Looper.getMainLooper());
        private boolean visible = false;
        private int width = 0;
        private int height = 0;

        private final List<Particle> particles = new ArrayList<>();
        private final List<Particle> touchParticles = new ArrayList<>();

        private Shader bgGradientShader = null;

        private int currentTheme = MainActivity.THEME_BLUE;
        private int currentSpeed = 5;
        private int lastParticleCount = -1;
        private int lastTheme = -1;
        private boolean touchEnabled = true;

        private final Runnable drawRunnable = new Runnable() {
            @Override
            public void run() {
                drawFrame();
            }
        };

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            setTouchEventsEnabled(true);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            handler.removeCallbacks(drawRunnable);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            this.visible = visible;
            if (visible) {
                loadCurrentPrefs();
                drawFrame();
            } else {
                handler.removeCallbacks(drawRunnable);
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            this.width = width;
            this.height = height;
            bgGradientShader = null;
            initParticles();
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            this.visible = false;
            handler.removeCallbacks(drawRunnable);
        }

        @Override
        public void onTouchEvent(MotionEvent event) {
            if (!touchEnabled) {
                super.onTouchEvent(event);
                return;
            }
            int action = event.getAction();
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
                spawnTouchParticles(event.getX(), event.getY());
            }
            super.onTouchEvent(event);
        }

        private void loadCurrentPrefs() {
            SharedPreferences prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE);
            currentTheme = prefs.getInt(MainActivity.KEY_THEME, MainActivity.THEME_BLUE);
            currentSpeed = prefs.getInt(MainActivity.KEY_SPEED, 5);
            int count = prefs.getInt(MainActivity.KEY_COUNT, 80);
            touchEnabled = prefs.getBoolean(MainActivity.KEY_TOUCH, true);

            if (count != lastParticleCount || currentTheme != lastTheme) {
                lastParticleCount = count;
                lastTheme = currentTheme;
                bgGradientShader = null;
                initParticles();
            }
        }

        private void initParticles() {
            particles.clear();
            if (width <= 0 || height <= 0) return;

            Random rand = new Random();
            int count = lastParticleCount;
            if (count <= 0) count = 80;

            for (int i = 0; i < count; i++) {
                float px = rand.nextFloat() * width;
                float py = rand.nextFloat() * height;
                float pvx = (rand.nextFloat() * 2f - 1f) * 1.5f;
                float pvy = (rand.nextFloat() * 2f - 1f) * 1.5f;
                float psize = rand.nextFloat() * 12 + 4;
                int pcolor = getRandomColor(rand);
                particles.add(new Particle(px, py, pvx, pvy, psize, pcolor, false));
            }
        }

        private void spawnTouchParticles(float x, float y) {
            Random rand = new Random();
            for (int i = 0; i < 6; i++) {
                double angle = rand.nextDouble() * 2 * Math.PI;
                double speed = 1.5 + rand.nextDouble() * 4.5;
                float vx = (float) (Math.cos(angle) * speed);
                float vy = (float) (Math.sin(angle) * speed);
                float size = rand.nextFloat() * 10 + 6;
                int color = getRandomColor(rand);
                if (touchParticles.size() < 120) {
                    touchParticles.add(new Particle(x, y, vx, vy, size, color, true));
                }
            }
        }

        private void drawFrame() {
            SurfaceHolder holder = getSurfaceHolder();
            Canvas canvas = null;
            try {
                canvas = holder.lockCanvas();
                if (canvas != null) {
                    drawBackground(canvas);
                    drawParticles(canvas);
                }
            } catch (Exception e) {
                // Surface locked or state changes
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas);
                    } catch (Exception e) {
                        // Ignore
                    }
                }
            }

            handler.removeCallbacks(drawRunnable);
            if (visible) {
                handler.postDelayed(drawRunnable, 1000 / 60);
            }
        }

        private void drawBackground(Canvas canvas) {
            if (bgGradientShader == null) {
                int[] colors = getThemeBgColors();
                bgGradientShader = new android.graphics.LinearGradient(
                        0, 0, 0, height,
                        colors[0], colors[1],
                        Shader.TileMode.CLAMP
                );
            }
            Paint bgPaint = new Paint();
            bgPaint.setShader(bgGradientShader);
            canvas.drawRect(0, 0, width, height, bgPaint);
        }

        private void drawParticles(Canvas canvas) {
            Random rand = new Random();
            Paint paint = new Paint();
            paint.setAntiAlias(true);

            float speedMultiplier = currentSpeed * 0.35f;
            if (speedMultiplier < 0.1f) speedMultiplier = 0.1f;

            for (int i = 0; i < particles.size(); i++) {
                Particle p = particles.get(i);
                p.x += p.vx * speedMultiplier;
                p.y += p.vy * speedMultiplier;

                if (p.x < -p.size) p.x = width + p.size;
                if (p.x > width + p.size) p.x = -p.size;
                if (p.y < -p.size) p.y = height + p.size;
                if (p.y > height + p.size) p.y = -p.size;

                paint.setColor(p.color);
                paint.setAlpha(200);
                canvas.drawCircle(p.x, p.y, p.size, paint);
            }

            for (int i = touchParticles.size() - 1; i >= 0; i--) {
                Particle tp = touchParticles.get(i);
                tp.x += tp.vx;
                tp.y += tp.vy;
                tp.vx *= 0.95f;
                tp.vy *= 0.95f;
                tp.life -= 0.025f;

                if (tp.life <= 0) {
                    touchParticles.remove(i);
                    continue;
                }

                paint.setColor(tp.color);
                paint.setAlpha((int) (tp.life * 255));
                canvas.drawCircle(tp.x, tp.y, tp.size * (0.4f + tp.life * 0.6f), paint);
            }
        }

        private int[] getThemeBgColors() {
            switch (currentTheme) {
                case MainActivity.THEME_PURPLE:
                    return new int[]{0xFF090314, 0xFF1D0933};
                case MainActivity.THEME_EMERALD:
                    return new int[]{0xFF020D04, 0xFF0A2210};
                case MainActivity.THEME_FIRE:
                    return new int[]{0xFF0F0402, 0xFF2B0A04};
                case MainActivity.THEME_BLUE:
                default: 
                    return new int[]{0xFF040B15, 0xFF0A182F};
            }
        }

        private int getRandomColor(Random rand) {
            int[] colors;
            switch (currentTheme) {
                case MainActivity.THEME_PURPLE:
                    colors = new int[]{0xFFE040FB, 0xFFD500F9, 0xFFBA68C8, 0xFFFF4081};
                    break;
                case MainActivity.THEME_EMERALD:
                    colors = new int[]{0xFF69F0AE, 0xFF00E676, 0xFFB9F6CA, 0xFF00C853};
                    break;
                case MainActivity.THEME_FIRE:
                    colors = new int[]{0xFFFFD700, 0xFFFF9100, 0xFFFF3D00, 0xFFFFEA00};
                    break;
                case MainActivity.THEME_BLUE:
                default:
                    colors = new int[]{0xFF64FFDA, 0xFF00E5FF, 0xFF80DEEA, 0xFF00B0FF};
                    break;
            }
            return colors[rand.nextInt(colors.length)];
        }
    }
}