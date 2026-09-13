package com.monkeyfun.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Random;

public class MainActivity extends Activity {

    // Views references
    private LinearLayout panelGame;
    private ScrollViewWrapper panelSounds; // helper representation or matching XML View structure type
    private View panelSoundsView;
    private LinearLayout panelFacts;

    private LinearLayout navGame;
    private LinearLayout navSounds;
    private LinearLayout navFacts;

    private TextView lblNavGame;
    private TextView lblNavSounds;
    private TextView lblNavFacts;

    private TextView txtScore;
    private TextView txtLives;
    private TextView txtHighScore;

    private TextView txtFactContent;
    private TextView txtFactEmoji;
    private LinearLayout cardFactContainer;

    // Game Elements
    private FrameLayout gameCanvasWrapper;
    private MonkeyGameView gameView;

    // Local state variables
    private int score = 0;
    private int lives = 3;
    private int highScore = 0;
    private SharedPreferences prefs;

    // Monkey Facts Repository
    private final String[] monkeyFacts = new String[] {
        "Monkeys can use heavy stone tools like rocks to crack open shells and tropical nuts!",
        "There are over 260 distinct monkey species, split into New World and Old World groups.",
        "Capuchin monkeys are considered among the most intelligent primates, often cooperative hunters.",
        "Monkeys show close affection and build social trust by grooming and cleaning each other.",
        "A cluster of monkeys is called a troop, tribe, or mission. They have tight family units.",
        "Howler monkeys are the loudest land animals. Their growling calls travel up to 3 miles in dense forest!",
        "Monkeys are smart omnivores! They eat healthy sweet fruits, foliage, insect larvae, and nuts.",
        "The Pygmy Marmoset is the smallest monkey in the world. It is banana-sized and weighs under 5 ounces!"
    };

    private final String[] monkeyEmojis = new String[] {
        "🛠️", "🌍", "🧠", "❤️", "🐒", "📣", "🍌", "🔎"
    };

    private int currentFactIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.monkeyfun.app.R.layout.activity_main);

        prefs = getSharedPreferences("monkey_fun_prefs", MODE_PRIVATE);
        highScore = prefs.getInt("high_score", 0);

        // Bind layouts
        panelGame = (LinearLayout) findViewById(com.monkeyfun.app.R.id.panel_game);
        panelSoundsView = findViewById(com.monkeyfun.app.R.id.panel_sounds);
        panelFacts = (LinearLayout) findViewById(com.monkeyfun.app.R.id.panel_facts);

        navGame = (LinearLayout) findViewById(com.monkeyfun.app.R.id.btn_nav_game);
        navSounds = (LinearLayout) findViewById(com.monkeyfun.app.R.id.btn_nav_sounds);
        navFacts = (LinearLayout) findViewById(com.monkeyfun.app.R.id.btn_nav_facts);

        lblNavGame = (TextView) findViewById(com.monkeyfun.app.R.id.lbl_nav_game);
        lblNavSounds = (TextView) findViewById(com.monkeyfun.app.R.id.lbl_nav_sounds);
        lblNavFacts = (TextView) findViewById(com.monkeyfun.app.R.id.lbl_nav_facts);

        txtScore = (TextView) findViewById(com.monkeyfun.app.R.id.txt_score);
        txtLives = (TextView) findViewById(com.monkeyfun.app.R.id.txt_lives);
        txtHighScore = (TextView) findViewById(com.monkeyfun.app.R.id.txt_highscore);

        txtFactContent = (TextView) findViewById(com.monkeyfun.app.R.id.txt_fact_content);
        txtFactEmoji = (TextView) findViewById(com.monkeyfun.app.R.id.txt_fact_emoji);
        cardFactContainer = (LinearLayout) findViewById(com.monkeyfun.app.R.id.card_fact_container);

        gameCanvasWrapper = (FrameLayout) findViewById(com.monkeyfun.app.R.id.game_canvas_wrapper);

        // Style the static components programmatically with high quality vectors/colors
        cardFactContainer.setBackground(createCardDrawable(0xFFFFFDF0));
        styleSoundboardButtons();

        // Initialize Game Canvas programmatically to avoid any inflation conflicts
        gameView = new MonkeyGameView(this);
        gameCanvasWrapper.addView(gameView);

        // Setup dynamic callbacks between game and outer view
        gameView.setGameListener(new GameListener() {
            @Override
            public void onScoreChanged(final int currentScore) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        score = currentScore;
                        txtScore.setText("Score: " + score);
                        if (score > highScore) {
                            highScore = score;
                            txtHighScore.setText("Best: " + highScore);
                            prefs.edit().putInt("high_score", highScore).apply();
                        }
                    }
                });
            }

            @Override
            public void onLivesChanged(final int currentLives) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        lives = currentLives;
                        StringBuilder sb = new StringBuilder("Lives: ");
                        for (int i = 0; i < 3; i++) {
                            if (i < lives) {
                                sb.append("❤️");
                            } else {
                                sb.append("🖤");
                            }
                        }
                        txtLives.setText(sb.toString());
                    }
                });
            }

            @Override
            public void onGameOver(final int finalScore) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        playSyntheticSound(5); // sad lose sweep
                    }
                });
            }
        });

        // Setup navigation actions
        navGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPanel(1);
            }
        });

        navSounds.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPanel(2);
            }
        });

        navFacts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPanel(3);
            }
        });

        // Sound Buttons
        findViewById(com.monkeyfun.app.R.id.btn_sound_screech).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playSyntheticSound(1);
            }
        });

        findViewById(com.monkeyfun.app.R.id.btn_sound_chatter).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playSyntheticSound(2);
            }
        });

        findViewById(com.monkeyfun.app.R.id.btn_sound_howl).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playSyntheticSound(4);
            }
        });

        findViewById(com.monkeyfun.app.R.id.btn_sound_gibbon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playSyntheticSound(6);
            }
        });

        findViewById(com.monkeyfun.app.R.id.btn_sound_drum).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playSyntheticSound(7);
            }
        });

        // Facts Buttons
        findViewById(com.monkeyfun.app.R.id.btn_fact_prev).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentFactIndex--;
                if (currentFactIndex < 0) {
                    currentFactIndex = monkeyFacts.length - 1;
                }
                updateFactCard();
            }
        });

        findViewById(com.monkeyfun.app.R.id.btn_fact_next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentFactIndex = (currentFactIndex + 1) % monkeyFacts.length;
                updateFactCard();
            }
        });

        // Display High Score initially
        txtHighScore.setText("Best: " + highScore);
        updateFactCard();
    }

    private void showPanel(int panelIndex) {
        panelGame.setVisibility(panelIndex == 1 ? View.VISIBLE : View.GONE);
        panelSoundsView.setVisibility(panelIndex == 2 ? View.VISIBLE : View.GONE);
        panelFacts.setVisibility(panelIndex == 3 ? View.VISIBLE : View.GONE);

        // Update nav labels styles
        lblNavGame.setTextColor(panelIndex == 1 ? 0xFFFFEB3B : 0xFFFFFFFF);
        lblNavSounds.setTextColor(panelIndex == 2 ? 0xFFFFEB3B : 0xFFFFFFFF);
        lblNavFacts.setTextColor(panelIndex == 3 ? 0xFFFFEB3B : 0xFFFFFFFF);

        // Pause game if we navigate away
        if (panelIndex != 1) {
            gameView.pauseGame();
        } else {
            gameView.resumeGame();
        }
    }

    private void updateFactCard() {
        txtFactContent.setText(monkeyFacts[currentFactIndex]);
        txtFactEmoji.setText(monkeyEmojis[currentFactIndex]);
    }

    private void styleSoundboardButtons() {
        int colorNormal = 0xFF8D6E63;
        int colorPressed = 0xFF5D4037;

        int[] ids = new int[] {
            com.monkeyfun.app.R.id.btn_sound_screech,
            com.monkeyfun.app.R.id.btn_sound_chatter,
            com.monkeyfun.app.R.id.btn_sound_howl,
            com.monkeyfun.app.R.id.btn_sound_gibbon,
            com.monkeyfun.app.R.id.btn_sound_drum,
            com.monkeyfun.app.R.id.btn_fact_prev,
            com.monkeyfun.app.R.id.btn_fact_next
        };

        for (int id : ids) {
            View btn = findViewById(id);
            if (btn != null) {
                btn.setBackground(createButtonDrawable(colorNormal, colorPressed));
            }
        }
    }

    // Programmatically generates clean shapes for UI styling without XML load errors
    public static android.graphics.drawable.Drawable createButtonDrawable(int colorNormal, int colorPressed) {
        GradientDrawable gdNormal = new GradientDrawable();
        gdNormal.setColor(colorNormal);
        gdNormal.setCornerRadius(20f);
        gdNormal.setStroke(3, 0xFF3E2723);

        GradientDrawable gdPressed = new GradientDrawable();
        gdPressed.setColor(colorPressed);
        gdPressed.setCornerRadius(20f);
        gdPressed.setStroke(3, 0xFF3E2723);

        StateListDrawable states = new StateListDrawable();
        states.addState(new int[] {android.R.attr.state_pressed}, gdPressed);
        states.addState(new int[] {}, gdNormal);
        return states;
    }

    public static android.graphics.drawable.Drawable createCardDrawable(int backgroundColor) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(backgroundColor);
        gd.setCornerRadius(30f);
        gd.setStroke(4, 0xFF8D6E63);
        return gd;
    }

    // Dynamic wave sound synthesizer for pure native AudioTrack playback
    private void playSyntheticSound(final int soundType) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                int sampleRate = 11025; // light sample rate for prompt sound loading
                double duration = 0.45;
                if (soundType == 4) duration = 0.8;  // long howl
                if (soundType == 7) duration = 0.7;  // multi chest drumbeat
                
                int numSamples = (int) (duration * sampleRate);
                double[] sample = new double[numSamples];
                byte[] generatedSnd = new byte[2 * numSamples];

                for (int i = 0; i < numSamples; ++i) {
                    double t = (double) i / sampleRate;
                    double freq;

                    switch (soundType) {
                        case 1: // Screech: sweep up extremely fast with high phase modulation
                            freq = 700.0 + (1300.0 * t / duration) + (250.0 * Math.sin(2.0 * Math.PI * 45.0 * t));
                            sample[i] = Math.sin(2.0 * Math.PI * freq * t);
                            break;
                        case 2: // Chatter: pulse up and down fast
                            double pulse = Math.sin(2.0 * Math.PI * 18.0 * t);
                            double carrier = Math.sin(2.0 * Math.PI * 1400.0 * t);
                            freq = (pulse > 0) ? 1500.0 : 1000.0;
                            sample[i] = carrier * (pulse > 0 ? 0.75 : 0.15);
                            break;
                        case 3: // Catch Banana: quick sweet ascending chime
                            freq = 880.0 + (900.0 * t / duration);
                            sample[i] = Math.sin(2.0 * Math.PI * freq * t);
                            break;
                        case 4: // Howler Growl: low frequency oscillating amplitude
                            double howlMod = Math.sin(2.0 * Math.PI * 3.5 * t);
                            freq = 190.0 + (40.0 * howlMod);
                            sample[i] = Math.sin(2.0 * Math.PI * freq * t) * (0.6 + 0.4 * Math.sin(2.0 * Math.PI * 12.0 * t));
                            break;
                        case 5: // Lose life / Sad: slide pitch down
                            freq = 900.0 * (1.0 - t / duration);
                            sample[i] = Math.sin(2.0 * Math.PI * freq * t);
                            break;
                        case 6: // Gibbon Melodic call: smooth up-down oscillation
                            freq = 600.0 + 400.0 * Math.sin(2.0 * Math.PI * 5.0 * t);
                            sample[i] = Math.sin(2.0 * Math.PI * freq * t);
                            break;
                        case 7: // Drumbeat: periodic rapid low thumps
                            double drumT = t % 0.17;
                            double dFreq = 140.0 * Math.exp(-22.0 * drumT);
                            sample[i] = Math.sin(2.0 * Math.PI * dFreq * drumT) * Math.exp(-9.0 * drumT);
                            break;
                        default:
                            freq = 1000.0;
                            sample[i] = Math.sin(2.0 * Math.PI * freq * t);
                            break;
                    }

                    // Linear fade-in and out to prevent popping or hardware clicks
                    double envelope = 1.0;
                    if (i > numSamples - 900) {
                        envelope = (numSamples - i) / 900.0;
                    }
                    if (i < 400) {
                        envelope = i / 400.0;
                    }
                    sample[i] *= envelope;
                }

                int idx = 0;
                for (double dVal : sample) {
                    short val = (short) ((dVal * 32767));
                    generatedSnd[idx++] = (byte) (val & 0x00ff);
                    generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
                }

                AudioTrack audioTrack = null;
                try {
                    audioTrack = new AudioTrack(
                        AudioManager.STREAM_MUSIC,
                        sampleRate,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        generatedSnd.length,
                        AudioTrack.MODE_STATIC
                    );
                    audioTrack.write(generatedSnd, 0, generatedSnd.length);
                    audioTrack.play();
                    Thread.sleep((long) (duration * 1000) + 120);
                } catch (Exception e) {
                    // Fail silently
                } finally {
                    if (audioTrack != null) {
                        try {
                            audioTrack.release();
                        } catch (Exception ignored) {}
                    }
                }
            }
        }).start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (gameView != null) {
            gameView.pauseGame();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gameView != null && panelGame.getVisibility() == View.VISIBLE) {
            gameView.resumeGame();
        }
    }

    // Interface linking custom View and Main Activity UI Thread
    public interface GameListener {
        void onScoreChanged(int score);
        void onLivesChanged(int lives);
        void onGameOver(int finalScore);
    }

    // Banana entity descriptor class
    public static class Banana {
        public float x, y;
        public float speed;
        public float rot;
        public float rotSpeed;

        public Banana(float x, float y, float speed) {
            this.x = x;
            this.y = y;
            this.speed = speed;
            Random r = new Random();
            this.rot = r.nextFloat() * 360f;
            this.rotSpeed = -4f + r.nextFloat() * 8f;
        }
    }

    // Custom game Canvas implementation
    public static class MonkeyGameView extends View {

        private Paint paintJungle;
        private Paint paintMonkey;
        private Paint paintFace;
        private Paint paintDetail;
        private Paint paintBanana;
        private Paint paintText;

        private float monkeyX = 200f;
        private float monkeyY = 0f;
        private float targetMonkeyX = 200f;
        private float monkeyHeightOffset = 180f;

        private ArrayList<Banana> bananas;
        private int internalScore = 0;
        private int internalLives = 3;
        private boolean isPlaying = false;
        private boolean isGameOver = false;

        private Handler gameLoopHandler;
        private Runnable updateRunnable;
        private final int FRAME_RATE_MS = 20; // ~50fps execution speed

        private GameListener listener;
        private int spawnTimer = 0;

        public MonkeyGameView(Context context) {
            super(context);
            init();
        }

        public MonkeyGameView(Context context, AttributeSet attrs) {
            super(context, attrs);
            init();
        }

        private void init() {
            bananas = new ArrayList<Banana>();

            paintJungle = new Paint(Paint.ANTI_ALIAS_FLAG);
            paintMonkey = new Paint(Paint.ANTI_ALIAS_FLAG);
            paintMonkey.setColor(0xFF5D4037); // Brown

            paintFace = new Paint(Paint.ANTI_ALIAS_FLAG);
            paintFace.setColor(0xFFFFCC80); // Light Peach mask

            paintDetail = new Paint(Paint.ANTI_ALIAS_FLAG);
            paintDetail.setStyle(Paint.Style.FILL);

            paintBanana = new Paint(Paint.ANTI_ALIAS_FLAG);
            paintBanana.setColor(0xFFFFEB3B); // Bright Yellow
            paintBanana.setStrokeWidth(12f);
            paintBanana.setStyle(Paint.Style.STROKE);

            paintText = new Paint(Paint.ANTI_ALIAS_FLAG);
            paintText.setTextAlign(Paint.Align.CENTER);

            gameLoopHandler = new Handler();
            updateRunnable = new Runnable() {
                @Override
                public void run() {
                    if (isPlaying && !isGameOver) {
                        updatePhysics();
                        invalidate();
                        gameLoopHandler.postDelayed(updateRunnable, FRAME_RATE_MS);
                    }
                }
            };

            // Touch listener to smoothly position the catcher monkey
            setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (isGameOver) {
                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            resetGame();
                        }
                        return true;
                    }

                    if (!isPlaying) {
                        isPlaying = true;
                        gameLoopHandler.postDelayed(updateRunnable, FRAME_RATE_MS);
                        return true;
                    }

                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                        case MotionEvent.ACTION_MOVE:
                            targetMonkeyX = event.getX();
                            break;
                    }
                    return true;
                }
            });
        }

        public void setGameListener(GameListener gl) {
            this.listener = gl;
        }

        public void pauseGame() {
            isPlaying = false;
            gameLoopHandler.removeCallbacks(updateRunnable);
        }

        public void resumeGame() {
            if (!isPlaying && !isGameOver) {
                isPlaying = true;
                gameLoopHandler.postDelayed(updateRunnable, FRAME_RATE_MS);
            }
        }

        private void resetGame() {
            internalScore = 0;
            internalLives = 3;
            isGameOver = false;
            isPlaying = true;
            bananas.clear();
            monkeyX = getWidth() / 2f;
            targetMonkeyX = monkeyX;

            if (listener != null) {
                listener.onScoreChanged(0);
                listener.onLivesChanged(3);
            }

            gameLoopHandler.removeCallbacks(updateRunnable);
            gameLoopHandler.postDelayed(updateRunnable, FRAME_RATE_MS);
            invalidate();
        }

        private void updatePhysics() {
            // Smoothly interpolate monkey position towards the touch coordinates
            monkeyX += (targetMonkeyX - monkeyX) * 0.25f;

            // Keep within horizontal boundaries
            if (monkeyX < 80) monkeyX = 80;
            if (monkeyX > getWidth() - 80) monkeyX = getWidth() - 80;

            monkeyY = getHeight() - monkeyHeightOffset;

            // Spawn falling bananas dynamically
            spawnTimer++;
            int speedFactor = 6 + (internalScore / 60); // progressively gets faster
            int spawnFrequency = Math.max(30, 70 - (internalScore / 30)); // progressively spawns faster

            if (spawnTimer >= spawnFrequency) {
                spawnTimer = 0;
                Random rnd = new Random();
                float spawnX = 60f + rnd.nextFloat() * (getWidth() - 120f);
                bananas.add(new Banana(spawnX, -50f, speedFactor));
            }

            // Move and check collisions for each banana
            for (int i = bananas.size() - 1; i >= 0; i--) {
                Banana b = bananas.get(i);
                b.y += b.speed;
                b.rot += b.rotSpeed;

                // Hit Detection with monkey hands/basket
                float dx = b.x - monkeyX;
                float dy = b.y - (monkeyY - 30f); // target point near hands height
                
                if (dy >= -50f && dy <= 30f && Math.abs(dx) < 95f) {
                    // Banana caught! Play synthesized melody
                    bananas.remove(i);
                    internalScore += 10;
                    if (listener != null) {
                        listener.onScoreChanged(internalScore);
                    }
                    if (getContext() instanceof MainActivity) {
                        ((MainActivity) getContext()).playSyntheticSound(3); // catch chime
                    }
                    continue;
                }

                // Bottom floor fall checking
                if (b.y > getHeight()) {
                    bananas.remove(i);
                    internalLives--;
                    if (listener != null) {
                        listener.onLivesChanged(internalLives);
                    }
                    if (getContext() instanceof MainActivity) {
                        ((MainActivity) getContext()).playSyntheticSound(5); // sad chime
                    }

                    if (internalLives <= 0) {
                        isGameOver = true;
                        isPlaying = false;
                        if (listener != null) {
                            listener.onGameOver(internalScore);
                        }
                    }
                }
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            // Draw sky/background gradient
            paintJungle.setColor(0xFFCFE8D7);
            canvas.drawRect(0, 0, getWidth(), getHeight(), paintJungle);

            // Draw some background jungle foliage vectors
            paintJungle.setColor(0xFFBCDECD);
            canvas.drawCircle(0f, getHeight(), 160f, paintJungle);
            canvas.drawCircle(getWidth(), getHeight(), 180f, paintJungle);
            canvas.drawCircle(getWidth() / 2f, getHeight() + 100, 300f, paintJungle);

            // Draw ground level
            paintJungle.setColor(0xFF7CB342); // Grass green
            canvas.drawRect(0, getHeight() - 50, getWidth(), getHeight(), paintJungle);

            // Draw each banana
            for (Banana b : bananas) {
                canvas.save();
                canvas.translate(b.x, b.y);
                canvas.rotate(b.rot);

                // Drawing crescent shape of a real banana
                RectF bananaRect = new RectF(-25f, -25f, 25f, 25f);
                canvas.drawArc(bananaRect, 30f, 150f, false, paintBanana);

                // Draw tiny brown stem details
                paintDetail.setColor(0xFF795548);
                canvas.drawRect(-26f, -4f, -19f, 2f, paintDetail);
                canvas.restore();
            }

            // Draw the player character: Custom Vector Monkey
            if (monkeyY > 0) {
                drawCartoonMonkey(canvas, monkeyX, monkeyY);
            }

            // Game over screen overlays
            if (isGameOver) {
                paintDetail.setColor(0xCC000000); // transparent background black overlay
                canvas.drawRect(0, 0, getWidth(), getHeight(), paintDetail);

                paintText.setColor(Color.WHITE);
                paintText.setTextSize(44sp);
                paintText.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                canvas.drawText("GAME OVER 🦧", getWidth() / 2f, getHeight() / 2f - 60, paintText);

                paintText.setColor(0xFFFFEB3B);
                paintText.setTextSize(26sp);
                canvas.drawText("Bananas Collected: " + (internalScore / 10), getWidth() / 2f, getHeight() / 2f, paintText);

                paintText.setColor(Color.WHITE);
                paintText.setTextSize(18sp);
                canvas.drawText("Tap Screen to Catch Again!", getWidth() / 2f, getHeight() / 2f + 80, paintText);
            } else if (!isPlaying) {
                // Game start screen instruction
                paintDetail.setColor(0x88000000);
                canvas.drawRect(0, 0, getWidth(), getHeight(), paintDetail);

                paintText.setColor(Color.WHITE);
                paintText.setTextSize(34sp);
                paintText.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                canvas.drawText("🍌 Catch the Banana!", getWidth() / 2f, getHeight() / 2f - 40, paintText);

                paintText.setTextSize(18sp);
                paintText.setTypeface(android.graphics.Typeface.DEFAULT);
                canvas.drawText("Slide/Drag finger at the bottom to catch.", getWidth() / 2f, getHeight() / 2f + 20, paintText);
                canvas.drawText("Don't let them touch the grass!", getWidth() / 2f, getHeight() / 2f + 50, paintText);
                canvas.drawText("⚡ TAP TO PLAY ⚡", getWidth() / 2f, getHeight() / 2f + 110, paintText);
            }
        }

        private void drawCartoonMonkey(Canvas canvas, float mx, float my) {
            // Reaching hands to catch bananas
            paintDetail.setColor(0xFF5D4037);
            paintDetail.setStrokeWidth(14f);
            paintDetail.setStyle(Paint.Style.STROKE);

            // Left Hand
            canvas.drawLine(mx, my + 30, mx - 65, my - 45, paintDetail);
            // Right Hand
            canvas.drawLine(mx, my + 30, mx + 65, my - 45, paintDetail);

            paintDetail.setStyle(Paint.Style.FILL);
            // Hand Circles
            canvas.drawCircle(mx - 65, my - 45, 14f, paintDetail);
            canvas.drawCircle(mx + 65, my - 45, 14f, paintDetail);

            // Monkey Ears (Large circular segments on outer head sides)
            canvas.drawCircle(mx - 52, my, 25f, paintMonkey);
            canvas.drawCircle(mx + 52, my, 25f, paintMonkey);

            paintDetail.setColor(0xFFFFCC80); // inner pink/peach ear color
            canvas.drawCircle(mx - 52, my, 14f, paintDetail);
            canvas.drawCircle(mx + 52, my, 14f, paintDetail);

            // Monkey Main Head
            canvas.drawCircle(mx, my, 54f, paintMonkey);

            // Snout/Cheeks rounded cartoon-mask details
            RectF cheekLeft = new RectF(mx - 40, my - 15, mx + 5, my + 35);
            RectF cheekRight = new RectF(mx - 5, my - 15, mx + 40, my + 35);
            canvas.drawOval(cheekLeft, paintFace);
            canvas.drawOval(cheekRight, paintFace);

            // Eyes: White backing circles
            paintDetail.setColor(Color.WHITE);
            canvas.drawCircle(mx - 16, my - 10, 11f, paintDetail);
            canvas.drawCircle(mx + 16, my - 10, 11f, paintDetail);

            // Pupils
            paintDetail.setColor(Color.BLACK);
            canvas.drawCircle(mx - 16, my - 10, 5f, paintDetail);
            canvas.drawCircle(mx + 16, my - 10, 5f, paintDetail);

            // Cheerful Smile arc
            paintDetail.setColor(0xFFD84315);
            RectF mouthRect = new RectF(mx - 22, my + 8, mx + 22, my + 28);
            canvas.drawArc(mouthRect, 0, 180, true, paintDetail);

            // Nose nostrils dots
            paintDetail.setColor(0xFF5D4037);
            canvas.drawCircle(mx - 4, my + 6, 3f, paintDetail);
            canvas.drawCircle(mx + 4, my + 6, 3f, paintDetail);

            // Cute little tuft of hair on head top
            Path hairPath = new Path();
            hairPath.moveTo(mx - 10, my - 52);
            hairPath.quadTo(mx, my - 70, mx + 10, my - 52);
            hairPath.close();
            canvas.drawPath(hairPath, paintMonkey);
        }
    }
}

// Minimalist ScrollView Wrapper subclass ensuring strict compilation types matching
class ScrollViewWrapper extends android.widget.ScrollView {
    public ScrollViewWrapper(Context context) {
        super(context);
    }
    public ScrollViewWrapper(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
}