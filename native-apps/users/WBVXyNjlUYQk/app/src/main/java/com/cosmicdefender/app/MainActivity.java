package com.cosmicdefender.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class MainActivity extends Activity {

    private FrameLayout gameViewContainer;
    private LinearLayout mainMenuLayout;
    private LinearLayout howToPlayLayout;
    private LinearLayout highScoreLayout;
    private LinearLayout gameOverLayout;
    private LinearLayout scoreListContainer;

    private TextView txtFinalScore;
    private TextView txtNewHighScoreMsg;
    private EditText edtPlayerName;
    private Button btnSaveScore;
    private Button btnSoundToggle;

    private CosmicGameView gameView;
    private SoundGenerator soundGenerator;
    private boolean soundOn = true;

    private SharedPreferences sharedPrefs;
    private static final String PREFS_NAME = "CosmicDefenderPrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        soundGenerator = new SoundGenerator();

        // Bind layouts
        gameViewContainer = (FrameLayout) findViewById(R.id.gameViewContainer);
        mainMenuLayout = (LinearLayout) findViewById(R.id.mainMenuLayout);
        howToPlayLayout = (LinearLayout) findViewById(R.id.howToPlayLayout);
        highScoreLayout = (LinearLayout) findViewById(R.id.highScoreLayout);
        gameOverLayout = (LinearLayout) findViewById(R.id.gameOverLayout);
        scoreListContainer = (LinearLayout) findViewById(R.id.scoreListContainer);

        txtFinalScore = (TextView) findViewById(R.id.txtFinalScore);
        txtNewHighScoreMsg = (TextView) findViewById(R.id.txtNewHighScoreMsg);
        edtPlayerName = (EditText) findViewById(R.id.edtPlayerName);
        btnSaveScore = (Button) findViewById(R.id.btnSaveScore);

        // Buttons Action Listeners
        Button btnStart = (Button) findViewById(R.id.btnStart);
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startGame();
            }
        });

        Button btnHighScores = (Button) findViewById(R.id.btnHighScores);
        btnHighScores.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showHighScoreScreen();
            }
        });

        Button btnHowToPlay = (Button) findViewById(R.id.btnHowToPlay);
        btnHowToPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mainMenuLayout.setVisibility(View.GONE);
                howToPlayLayout.setVisibility(View.VISIBLE);
            }
        });

        Button btnBackFromHow = (Button) findViewById(R.id.btnBackFromHow);
        btnBackFromHow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                howToPlayLayout.setVisibility(View.GONE);
                mainMenuLayout.setVisibility(View.VISIBLE);
            }
        });

        Button btnBackFromScores = (Button) findViewById(R.id.btnBackFromScores);
        btnBackFromScores.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                highScoreLayout.setVisibility(View.GONE);
                mainMenuLayout.setVisibility(View.VISIBLE);
            }
        });

        btnSoundToggle = (Button) findViewById(R.id.btnSoundToggle);
        btnSoundToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                soundOn = !soundOn;
                soundGenerator.setSoundEnabled(soundOn);
                btnSoundToggle.setText(soundOn ? "SOUND: ON" : "SOUND: OFF");
            }
        });

        Button btnRestart = (Button) findViewById(R.id.btnRestart);
        btnRestart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gameOverLayout.setVisibility(View.GONE);
                startGame();
            }
        });

        Button btnQuitMenu = (Button) findViewById(R.id.btnQuitMenu);
        btnQuitMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gameOverLayout.setVisibility(View.GONE);
                mainMenuLayout.setVisibility(View.VISIBLE);
            }
        });
    }

    private void startGame() {
        mainMenuLayout.setVisibility(View.GONE);
        gameOverLayout.setVisibility(View.GONE);
        gameViewContainer.removeAllViews();

        gameView = new CosmicGameView(this);
        gameViewContainer.addView(gameView);
    }

    public void onGameOver(final int finalScore) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                gameViewContainer.removeAllViews();
                gameOverLayout.setVisibility(View.VISIBLE);
                txtFinalScore.setText("FINAL SCORE: " + finalScore);

                // Check high score list
                List<HighScoreEntry> scores = getSavedScores();
                boolean isEligible = scores.size() < 5 || finalScore > scores.get(scores.size() - 1).score;

                if (isEligible && finalScore > 0) {
                    txtNewHighScoreMsg.setVisibility(View.VISIBLE);
                    edtPlayerName.setVisibility(View.VISIBLE);
                    btnSaveScore.setVisibility(View.VISIBLE);
                    edtPlayerName.setText("");

                    btnSaveScore.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            String name = edtPlayerName.getText().toString().trim();
                            if (name.isEmpty()) {
                                name = "UNKNOWN";
                            }
                            saveScore(name, finalScore);
                            txtNewHighScoreMsg.setVisibility(View.GONE);
                            edtPlayerName.setVisibility(View.GONE);
                            btnSaveScore.setVisibility(View.GONE);
                            Toast.makeText(MainActivity.this, "Callsign Saved!", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    txtNewHighScoreMsg.setVisibility(View.GONE);
                    edtPlayerName.setVisibility(View.GONE);
                    btnSaveScore.setVisibility(View.GONE);
                }
            }
        });
    }

    private void showHighScoreScreen() {
        mainMenuLayout.setVisibility(View.GONE);
        highScoreLayout.setVisibility(View.VISIBLE);
        scoreListContainer.removeAllViews();

        List<HighScoreEntry> scores = getSavedScores();
        if (scores.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText("NO SAVED MISSION LOGS");
            emptyText.setTextColor(Color.LTGRAY);
            emptyText.setTextSize(18);
            emptyText.setGravity(android.view.Gravity.CENTER);
            scoreListContainer.addView(emptyText);
        } else {
            for (int i = 0; i < scores.size(); i++) {
                HighScoreEntry entry = scores.get(i);
                TextView entryText = new TextView(this);
                entryText.setText((i + 1) + ". " + entry.name + "  -  " + entry.score + " PTS");
                entryText.setTextColor(i == 0 ? Color.YELLOW : Color.WHITE);
                entryText.setTextSize(20);
                entryText.setPadding(0, 8, 0, 8);
                entryText.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
                scoreListContainer.addView(entryText);
            }
        }
    }

    private List<HighScoreEntry> getSavedScores() {
        List<HighScoreEntry> scores = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            String name = sharedPrefs.getString("score_name_" + i, "");
            int score = sharedPrefs.getInt("score_val_" + i, -1);
            if (!name.isEmpty() && score != -1) {
                scores.add(new HighScoreEntry(name, score));
            }
        }
        Collections.sort(scores, new Comparator<HighScoreEntry>() {
            @Override
            public int compare(HighScoreEntry o1, HighScoreEntry o2) {
                return o2.score - o1.score;
            }
        });
        return scores;
    }

    private void saveScore(String name, int score) {
        List<HighScoreEntry> scores = getSavedScores();
        scores.add(new HighScoreEntry(name, score));

        Collections.sort(scores, new Comparator<HighScoreEntry>() {
            @Override
            public int compare(HighScoreEntry o1, HighScoreEntry o2) {
                return o2.score - o1.score;
            }
        });

        // Keep top 5
        SharedPreferences.Editor editor = sharedPrefs.edit();
        for (int i = 0; i < 5; i++) {
            if (i < scores.size()) {
                editor.putString("score_name_" + i, scores.get(i).name);
                editor.putInt("score_val_" + i, scores.get(i).score);
            } else {
                editor.remove("score_name_" + i);
                editor.remove("score_val_" + i);
            }
        }
        editor.apply();
    }

    private static class HighScoreEntry {
        String name;
        int score;

        HighScoreEntry(String name, int score) {
            this.name = name;
            this.score = score;
        }
    }

    // Direct retro audio waveform generator
    private static class SoundGenerator {
        private boolean soundEnabled = true;

        void setSoundEnabled(boolean enabled) {
            this.soundEnabled = enabled;
        }

        void playLaser() {
            if (!soundEnabled) return;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    playTone(900, 100, true);
                }
            }).start();
        }

        void playExplosion() {
            if (!soundEnabled) return;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    playNoise(250);
                }
            }).start();
        }

        void playPowerup() {
            if (!soundEnabled) return;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    playTone(400, 80, false);
                    try { Thread.sleep(80); } catch (InterruptedException e) {}
                    playTone(800, 120, false);
                }
            }).start();
        }

        private void playTone(double frequency, int durationMs, boolean sweepDown) {
            int sampleRate = 8000;
            int numSamples = durationMs * sampleRate / 1000;
            byte[] generatedSnd = new byte[2 * numSamples];

            int idx = 0;
            for (int i = 0; i < numSamples; ++i) {
                double freq = frequency;
                if (sweepDown) {
                    freq = frequency * (1.0 - (double) i / numSamples);
                }
                double dVal = Math.sin(2 * Math.PI * i / (sampleRate / freq));
                short val = (short) (dVal * 15000); // sound volume scale
                generatedSnd[idx++] = (byte) (val & 0x00ff);
                generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
            }

            try {
                AudioTrack audioTrack = new AudioTrack(
                        AudioManager.STREAM_MUSIC,
                        sampleRate,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        generatedSnd.length,
                        AudioTrack.MODE_STATIC);
                audioTrack.write(generatedSnd, 0, generatedSnd.length);
                audioTrack.play();
                Thread.sleep(durationMs);
                audioTrack.release();
            } catch (Exception e) {
                // Ignore audio track thread interrupts
            }
        }

        private void playNoise(int durationMs) {
            int sampleRate = 8000;
            int numSamples = durationMs * sampleRate / 1000;
            byte[] generatedSnd = new byte[2 * numSamples];
            Random random = new Random();

            int idx = 0;
            for (int i = 0; i < numSamples; ++i) {
                double factor = 1.0 - ((double) i / numSamples);
                short val = (short) ((random.nextDouble() * 2.0 - 1.0) * 16000 * factor);
                generatedSnd[idx++] = (byte) (val & 0x00ff);
                generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
            }

            try {
                AudioTrack audioTrack = new AudioTrack(
                        AudioManager.STREAM_MUSIC,
                        sampleRate,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        generatedSnd.length,
                        AudioTrack.MODE_STATIC);
                audioTrack.write(generatedSnd, 0, generatedSnd.length);
                audioTrack.play();
                Thread.sleep(durationMs);
                audioTrack.release();
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    // Custom Interactive canvas engine using dedicated thread loop
    private class CosmicGameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

        private Thread gameThread;
        private boolean isPlaying = false;
        private final SurfaceHolder holder;

        // Space Canvas dimensions
        private int screenW;
        private int screenH;

        // Entities
        private PlayerShip player;
        private final List<Laser> lasers = new ArrayList<>();
        private final List<Meteor> meteors = new ArrayList<>();
        private final List<PowerUp> powerups = new ArrayList<>();
        private final List<Particle> particles = new ArrayList<>();
        private final List<Star> starfield = new ArrayList<>();

        // Spawning Timers
        private long lastLaserTime = 0;
        private long lastMeteorTime = 0;
        private long lastPowerupTime = 0;

        // Match States
        private int currentScore = 0;
        private boolean isGameOver = false;

        // Rendering tools
        private final Paint mainPaint = new Paint();
        private final Random random = new Random();

        public CosmicGameView(Context context) {
            super(context);
            holder = getHolder();
            holder.addCallback(this);
            mainPaint.setAntiAlias(true);
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            screenW = getWidth();
            screenH = getHeight();

            // Initialize background cosmic particle field
            starfield.clear();
            for (int i = 0; i < 60; i++) {
                starfield.add(new Star(random.nextInt(screenW), random.nextInt(screenH), random.nextFloat() * 3.5f + 1));
            }

            // Initialize Player Ship Cruiser
            player = new PlayerShip(screenW / 2, screenH - 220);

            // Setup state
            currentScore = 0;
            isGameOver = false;
            lasers.clear();
            meteors.clear();
            powerups.clear();
            particles.clear();

            isPlaying = true;
            gameThread = new Thread(this);
            gameThread.start();
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            screenW = width;
            screenH = height;
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            boolean retry = true;
            isPlaying = false;
            while (retry) {
                try {
                    gameThread.join();
                    retry = false;
                } catch (InterruptedException e) {
                    // loop
                }
            }
        }

        @Override
        public void run() {
            while (isPlaying) {
                long frameStart = System.currentTimeMillis();

                updatePhysics();
                drawCanvas();

                long frameDuration = System.currentTimeMillis() - frameStart;
                long sleepTime = 16 - frameDuration; // Cap at roughly 60 fps
                if (sleepTime > 0) {
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        // ignore
                    }
                }
            }
        }

        private void updatePhysics() {
            if (isGameOver) return;

            // Parallax scroll stars background
            for (Star star : starfield) {
                star.y += star.speed;
                if (star.y > screenH) {
                    star.y = 0;
                    star.x = random.nextInt(screenW);
                }
            }

            long now = System.currentTimeMillis();

            // Automatic Blasters
            long fireInterval = player.hasTripleShot ? 220 : 350;
            if (now - lastLaserTime > fireInterval) {
                soundGenerator.playLaser();
                if (player.hasTripleShot) {
                    lasers.add(new Laser(player.x - 20, player.y - 15, -3));
                    lasers.add(new Laser(player.x, player.y - 25, 0));
                    lasers.add(new Laser(player.x + 20, player.y - 15, 3));
                } else {
                    lasers.add(new Laser(player.x, player.y - 25, 0));
                }
                lastLaserTime = now;
            }

            // Spawn incoming meteors
            long meteorInterval = Math.max(800 - (currentScore / 15), 350);
            if (now - lastMeteorTime > meteorInterval) {
                float mx = random.nextInt(screenW - 100) + 50;
                float size = random.nextFloat() * 45 + 30; // random size range
                float speed = random.nextFloat() * 6 + 4 + (currentScore / 250.0f);
                meteors.add(new Meteor(mx, -size, size, speed));
                lastMeteorTime = now;
            }

            // Spawn defensive assistance canisters
            if (now - lastPowerupTime > 9000) {
                float px = random.nextInt(screenW - 80) + 40;
                int type = random.nextInt(3); // 0: Triple shot, 1: Shield, 2: Health Restore
                powerups.add(new PowerUp(px, -30, type));
                lastPowerupTime = now;
            }

            // Move Lasers
            for (int i = lasers.size() - 1; i >= 0; i--) {
                Laser laser = lasers.get(i);
                laser.y -= 18;
                laser.x += laser.xVelocity;
                if (laser.y < 0 || laser.x < 0 || laser.x > screenW) {
                    lasers.remove(i);
                }
            }

            // Move Powerups
            for (int i = powerups.size() - 1; i >= 0; i--) {
                PowerUp power = powerups.get(i);
                power.y += 6;
                if (power.y > screenH) {
                    powerups.remove(i);
                    continue;
                }

                // Check intersection with defender ship
                double dist = Math.hypot(power.x - player.x, power.y - player.y);
                if (dist < (player.radius + 24)) {
                    soundGenerator.playPowerup();
                    if (power.type == 0) {
                        player.activateTripleShot();
                    } else if (power.type == 1) {
                        player.shieldStrength = 2; // Can absorb 2 full laser shocks
                    } else if (power.type == 2) {
                        if (player.lives < 5) player.lives++;
                    }
                    powerups.remove(i);
                }
            }

            // Move Meteors & detect collisions with player/lasers
            for (int i = meteors.size() - 1; i >= 0; i--) {
                Meteor m = meteors.get(i);
                m.y += m.speed;
                m.angle += m.rotSpeed;

                if (m.y - m.radius > screenH) {
                    meteors.remove(i);
                    continue;
                }

                // Hit against player?
                double pDist = Math.hypot(m.x - player.x, m.y - player.y);
                if (pDist < (m.radius + player.radius)) {
                    soundGenerator.playExplosion();
                    triggerExplosion(m.x, m.y, Color.RED, 15);

                    if (player.shieldStrength > 0) {
                        player.shieldStrength--;
                    } else {
                        player.lives--;
                        if (player.lives <= 0) {
                            isGameOver = true;
                            isPlaying = false;
                            onGameOver(currentScore);
                        }
                    }
                    meteors.remove(i);
                    continue;
                }

                // Hit by lasers?
                for (int j = lasers.size() - 1; j >= 0; j--) {
                    Laser l = lasers.get(j);
                    double lDist = Math.hypot(m.x - l.x, m.y - l.y);
                    if (lDist < m.radius) {
                        soundGenerator.playExplosion();
                        triggerExplosion(m.x, m.y, Color.rgb(255, 120, 0), 12);

                        // If meteor size is substantial, split into twins
                        if (m.radius > 45) {
                            meteors.add(new Meteor(m.x - 20, m.y, m.radius / 2, m.speed + 1.5f));
                            meteors.add(new Meteor(m.x + 20, m.y, m.radius / 2, m.speed + 1.5f));
                        }

                        currentScore += 10;
                        meteors.remove(i);
                        lasers.remove(j);
                        break;
                    }
                }
            }

            // Update particle explosions
            for (int i = particles.size() - 1; i >= 0; i--) {
                Particle p = particles.get(i);
                p.x += p.vx;
                p.y += p.vy;
                p.alpha -= 8;
                if (p.alpha <= 0) {
                    particles.remove(i);
                }
            }

            // Check powerups expirations
            if (player.hasTripleShot && now > player.tripleShotExpiry) {
                player.hasTripleShot = false;
            }
        }

        private void triggerExplosion(float x, float y, int baseColor, int count) {
            for (int i = 0; i < count; i++) {
                float angle = random.nextFloat() * 360;
                float speed = random.nextFloat() * 8 + 3;
                float vx = (float) (Math.cos(Math.toRadians(angle)) * speed);
                float vy = (float) (Math.sin(Math.toRadians(angle)) * speed);
                particles.add(new Particle(x, y, vx, vy, baseColor));
            }
        }

        private void drawCanvas() {
            if (!holder.getSurface().isValid()) return;

            Canvas canvas = holder.lockCanvas();
            if (canvas == null) return;

            // Draw galactic deep-space backdrop
            canvas.drawColor(Color.rgb(4, 4, 15));

            // Stars
            mainPaint.setStyle(Paint.Style.FILL);
            for (Star star : starfield) {
                mainPaint.setColor(Color.argb(190, 255, 255, 255));
                canvas.drawCircle(star.x, star.y, star.speed / 2.0f, mainPaint);
            }

            // Powerups
            for (PowerUp p : powerups) {
                if (p.type == 0) {
                    mainPaint.setColor(Color.GREEN);
                } else if (p.type == 1) {
                    mainPaint.setColor(Color.CYAN);
                } else {
                    mainPaint.setColor(Color.MAGENTA);
                }
                mainPaint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(p.x, p.y, 22, mainPaint);

                // Internal core glow
                mainPaint.setColor(Color.WHITE);
                canvas.drawCircle(p.x, p.y, 10, mainPaint);
            }

            // Lasers
            mainPaint.setStyle(Paint.Style.FILL);
            for (Laser l : lasers) {
                mainPaint.setColor(Color.rgb(0, 255, 255));
                canvas.drawRect(l.x - 4, l.y - 12, l.x + 4, l.y + 12, mainPaint);
            }

            // Meteors
            for (Meteor m : meteors) {
                mainPaint.setColor(Color.rgb(120, 100, 90));
                mainPaint.setStyle(Paint.Style.FILL);

                // Draw asteroid with jagged paths using rotation
                canvas.save();
                canvas.translate(m.x, m.y);
                canvas.rotate(m.angle);

                Path path = new Path();
                int numVertices = 8;
                for (int v = 0; v < numVertices; v++) {
                    double angle = (2 * Math.PI / numVertices) * v;
                    float r = m.radius + (float) (Math.sin(angle * 3) * (m.radius * 0.15f));
                    float vx = (float) (Math.cos(angle) * r);
                    float vy = (float) (Math.sin(angle) * r);
                    if (v == 0) path.moveTo(vx, vy);
                    else path.lineTo(vx, vy);
                }
                path.close();
                canvas.drawPath(path, mainPaint);

                // Crater details inside meteor
                mainPaint.setColor(Color.rgb(80, 65, 55));
                canvas.drawCircle(-m.radius * 0.3f, -m.radius * 0.2f, m.radius * 0.2f, mainPaint);
                canvas.drawCircle(m.radius * 0.3f, m.radius * 0.3f, m.radius * 0.15f, mainPaint);

                canvas.restore();
            }

            // Exploded Sparks
            for (Particle p : particles) {
                mainPaint.setColor(p.color);
                mainPaint.setAlpha(p.alpha);
                mainPaint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(p.x, p.y, random.nextFloat() * 5 + 3, mainPaint);
            }
            mainPaint.setAlpha(255); // restore alpha

            // Player Spacecraft
            mainPaint.setStyle(Paint.Style.FILL);
            canvas.save();
            canvas.translate(player.x, player.y);

            // Left Thruster Wing
            Path leftWing = new Path();
            leftWing.moveTo(-player.radius, player.radius);
            leftWing.lineTo(-player.radius - 12, player.radius + 15);
            leftWing.lineTo(-player.radius + 8, player.radius - 10);
            leftWing.close();
            mainPaint.setColor(Color.rgb(0, 150, 255));
            canvas.drawPath(leftWing, mainPaint);

            // Right Thruster Wing
            Path rightWing = new Path();
            rightWing.moveTo(player.radius, player.radius);
            rightWing.lineTo(player.radius + 12, player.radius + 15);
            rightWing.lineTo(player.radius - 8, player.radius - 10);
            rightWing.close();
            canvas.drawPath(rightWing, mainPaint);

            // Main Core Chassis
            Path core = new Path();
            core.moveTo(0, -player.radius - 10);
            core.lineTo(player.radius, player.radius);
            core.lineTo(0, player.radius / 2);
            core.lineTo(-player.radius, player.radius);
            core.close();
            mainPaint.setColor(Color.CYAN);
            canvas.drawPath(core, mainPaint);

            // Cockpit Canopy Glass
            mainPaint.setColor(Color.WHITE);
            canvas.drawCircle(0, -5, player.radius * 0.25f, mainPaint);

            // Jet Engine fire thruster animation
            if (random.nextBoolean()) {
                mainPaint.setColor(Color.rgb(255, 120, 0));
                Path exhaust = new Path();
                exhaust.moveTo(-10, player.radius);
                exhaust.lineTo(0, player.radius + 28);
                exhaust.lineTo(10, player.radius);
                exhaust.close();
                canvas.drawPath(exhaust, mainPaint);
            }

            canvas.restore();

            // Dynamic Protective Shield
            if (player.shieldStrength > 0) {
                mainPaint.setStyle(Paint.Style.STROKE);
                mainPaint.setStrokeWidth(6.0f);
                mainPaint.setColor(player.shieldStrength == 2 ? Color.CYAN : Color.BLUE);
                canvas.drawCircle(player.x, player.y, player.radius + 28, mainPaint);
                mainPaint.setStrokeWidth(1.0f);
            }

            // HUD Overlays (Top Banner status logs)
            mainPaint.setStyle(Paint.Style.FILL);
            mainPaint.setColor(Color.WHITE);
            mainPaint.setTextSize(40);

            // Draw Score metric
            canvas.drawText("SCORE: " + currentScore, 35, 75, mainPaint);

            // Draw hearts for surviving player lives
            float heartStartX = screenW - 240;
            mainPaint.setColor(Color.rgb(255, 40, 100));
            for (int h = 0; h < player.lives; h++) {
                canvas.drawCircle(heartStartX + (h * 42), 62, 14, mainPaint);
            }

            // Active Special Upgrades label
            if (player.hasTripleShot) {
                mainPaint.setColor(Color.GREEN);
                mainPaint.setTextSize(30);
                canvas.drawText("TRIPLE LAUNCHER ACTIVE", 35, 120, mainPaint);
            }

            holder.unlockCanvasAndPost(canvas);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            float tx = event.getX();
            float ty = event.getY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                    // Horizontal dynamic slide limiter bound
                    if (tx > player.radius && tx < screenW - player.radius) {
                        player.x = tx;
                    }
                    break;
            }
            return true;
        }
    }

    // Spacecraft Entity model
    private static class PlayerShip {
        float x;
        float y;
        float radius = 35;
        int lives = 3;
        int shieldStrength = 0;
        boolean hasTripleShot = false;
        long tripleShotExpiry = 0;

        PlayerShip(float startX, float startY) {
            this.x = startX;
            this.y = startY;
        }

        void activateTripleShot() {
            this.hasTripleShot = true;
            this.tripleShotExpiry = System.currentTimeMillis() + 8000; // lasts for 8 seconds
        }
    }

    // Projectile Laser Model
    private static class Laser {
        float x;
        float y;
        float xVelocity;

        Laser(float startX, float startY, float vx) {
            this.x = startX;
            this.y = startY;
            this.xVelocity = vx;
        }
    }

    // Space Meteor Model
    private static class Meteor {
        float x;
        float y;
        float radius;
        float speed;
        float angle;
        float rotSpeed;

        Meteor(float startX, float startY, float size, float velocity) {
            this.x = startX;
            this.y = startY;
            this.radius = size;
            this.speed = velocity;
            this.angle = 0;
            this.rotSpeed = (new Random().nextFloat() * 6.0f) - 3.0f; // dynamic rotation speed
        }
    }

    // Buff PowerUp drop canisters
    private static class PowerUp {
        float x;
        float y;
        int type; // 0: Triple Shot, 1: Shield, 2: Restore Life

        PowerUp(float startX, float startY, int kind) {
            this.x = startX;
            this.y = startY;
            this.type = kind;
        }
    }

    // Explosion Particle Spark Model
    private static class Particle {
        float x;
        float y;
        float vx;
        float vy;
        int color;
        int alpha = 255;

        Particle(float sx, float sy, float speedX, float speedY, int col) {
            this.x = sx;
            this.y = sy;
            this.vx = speedX;
            this.vy = speedY;
            this.color = col;
        }
    }

    // Background Stars Model
    private static class Star {
        float x;
        float y;
        float speed;

        Star(float sx, float sy, float velocity) {
            this.x = sx;
            this.y = sy;
            this.speed = velocity;
        }
    }
}