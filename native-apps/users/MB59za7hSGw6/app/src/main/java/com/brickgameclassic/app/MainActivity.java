package com.brickgameclassic.app;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

/**
 * Main Activity managing the Brick Game handheld console interface.
 * Implements continuous touch holding for movement and soft drops,
 * high score persistence with SharedPreferences, and sound controls.
 */
public class MainActivity extends Activity implements BrickGameEngine.GameListener {

    private static final String PREFS_NAME = "BrickGamePrefs";
    private static final String KEY_HI_SCORE = "hi_score";
    private static final String KEY_SOUND = "sound_enabled";

    private LcdScreenView lcdScreenView;
    private BrickGameEngine engine;
    private SoundManager soundManager;
    private SharedPreferences prefs;

    private Button btnLeft;
    private Button btnRight;
    private Button btnDown;
    private Button btnHardDrop;
    private Button btnRotate;
    private Button btnStartPause;
    private Button btnReset;
    private Button btnSound;
    private TextView txtSoundStatus;

    private final Handler repeatHandler = new Handler(Looper.getMainLooper());
    private Runnable activeRepeatRunnable = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        soundManager = new SoundManager();
        boolean savedSound = prefs.getBoolean(KEY_SOUND, true);
        soundManager.setSoundEnabled(savedSound);

        engine = new BrickGameEngine(this, soundManager);
        int savedHiScore = prefs.getInt(KEY_HI_SCORE, 0);
        engine.setHiScore(savedHiScore);

        initViews();
        setupControls();

        if (lcdScreenView != null) {
            lcdScreenView.setEngine(engine);
        }
        updateSoundButtonLabel();

        // Start game on launch
        engine.startNewGame();
    }

    private void initViews() {
        lcdScreenView = (LcdScreenView) findViewById(R.id.lcdScreenView);
        btnLeft = (Button) findViewById(R.id.btnLeft);
        btnRight = (Button) findViewById(R.id.btnRight);
        btnDown = (Button) findViewById(R.id.btnDown);
        btnHardDrop = (Button) findViewById(R.id.btnHardDrop);
        btnRotate = (Button) findViewById(R.id.btnRotate);
        btnStartPause = (Button) findViewById(R.id.btnStartPause);
        btnReset = (Button) findViewById(R.id.btnReset);
        btnSound = (Button) findViewById(R.id.btnSound);
        txtSoundStatus = (TextView) findViewById(R.id.txtSoundStatus);
    }

    private void setupControls() {
        if (btnLeft != null) {
            setupRepeatingTouch(btnLeft, new Runnable() {
                @Override
                public void run() {
                    engine.moveLeft();
                }
            });
        }

        if (btnRight != null) {
            setupRepeatingTouch(btnRight, new Runnable() {
                @Override
                public void run() {
                    engine.moveRight();
                }
            });
        }

        if (btnDown != null) {
            setupRepeatingTouch(btnDown, new Runnable() {
                @Override
                public void run() {
                    engine.softDrop();
                }
            });
        }

        if (btnRotate != null) {
            btnRotate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    engine.rotate();
                }
            });
        }

        if (btnHardDrop != null) {
            btnHardDrop.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    engine.hardDrop();
                }
            });
        }

        if (btnStartPause != null) {
            btnStartPause.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (engine.isGameOver() || !engine.isRunning()) {
                        engine.startNewGame();
                    } else {
                        engine.togglePause();
                    }
                }
            });
        }

        if (btnReset != null) {
            btnReset.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    engine.startNewGame();
                }
            });
        }

        if (btnSound != null) {
            btnSound.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    soundManager.toggleSound();
                    prefs.edit().putBoolean(KEY_SOUND, soundManager.isSoundEnabled()).apply();
                    updateSoundButtonLabel();
                }
            });
        }
    }

    private void setupRepeatingTouch(final View button, final Runnable action) {
        button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        action.run();
                        startRepeat(action);
                        return true;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        stopRepeat();
                        return true;
                }
                return false;
            }
        });
    }

    private void startRepeat(final Runnable action) {
        stopRepeat();
        activeRepeatRunnable = new Runnable() {
            @Override
            public void run() {
                action.run();
                repeatHandler.postDelayed(this, 110);
            }
        };
        repeatHandler.postDelayed(activeRepeatRunnable, 250);
    }

    private void stopRepeat() {
        if (activeRepeatRunnable != null) {
            repeatHandler.removeCallbacks(activeRepeatRunnable);
            activeRepeatRunnable = null;
        }
    }

    private void updateSoundButtonLabel() {
        if (txtSoundStatus != null) {
            txtSoundStatus.setText(soundManager.isSoundEnabled() ? "SOUND: ON" : "SOUND: OFF");
        }
    }

    @Override
    public void onGameUpdated() {
        if (lcdScreenView != null) {
            lcdScreenView.invalidate();
        }
    }

    @Override
    public void onLinesCleared(int lineCount) {
        if (lcdScreenView != null) {
            lcdScreenView.invalidate();
        }
    }

    @Override
    public void onGameOver() {
        int currentHiScore = engine.getHiScore();
        int savedHiScore = prefs.getInt(KEY_HI_SCORE, 0);
        if (currentHiScore > savedHiScore) {
            prefs.edit().putInt(KEY_HI_SCORE, currentHiScore).apply();
        }
        if (lcdScreenView != null) {
            lcdScreenView.invalidate();
        }
    }

    @Override
    public void onPiecePlaced() {
        if (lcdScreenView != null) {
            lcdScreenView.invalidate();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopRepeat();
        if (engine != null && engine.isRunning() && !engine.isPaused()) {
            engine.togglePause();
        }
        if (engine != null) {
            engine.pauseEngine();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (engine != null) {
            engine.resumeEngine();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRepeat();
        if (engine != null) {
            engine.pauseEngine();
        }
        if (soundManager != null) {
            soundManager.release();
        }
    }
}