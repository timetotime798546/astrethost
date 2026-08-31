package com.carracerpro.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class MainActivity extends Activity implements GameView.GameListener {

    private FrameLayout gameContainer;
    private GameView gameView;
    private SoundManager soundManager;

    // Overlays & UI Components
    private LinearLayout menuLayout;
    private RelativeLayout hudLayout;
    private LinearLayout gameOverLayout;

    // Menu elements
    private TextView highScoreTxt;
    private RadioGroup difficultyGroup;
    private Button startBtn;

    // HUD elements
    private TextView hudScore;
    private TextView hudLevel;
    private TextView hudSpeed;
    private ProgressBar hudFuelProgress;

    // Game Over elements
    private TextView goFinalScore;
    private TextView goCoins;
    private TextView goMaxSpeed;
    private Button replayBtn;
    private Button goMenuBtn;

    // Storage persistence
    private SharedPreferences sharedPrefs;
    private static final String PREF_KEY_HIGH_SCORE = "high_score_pro";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Lock Screen On during play session
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Bind layouts and widgets
        gameContainer = (FrameLayout) findViewById(R.id.game_container);
        menuLayout = (LinearLayout) findViewById(R.id.menu_layout);
        hudLayout = (RelativeLayout) findViewById(R.id.hud_layout);
        gameOverLayout = (LinearLayout) findViewById(R.id.game_over_layout);

        highScoreTxt = (TextView) findViewById(R.id.high_score_txt);
        difficultyGroup = (RadioGroup) findViewById(R.id.difficulty_group);
        startBtn = (Button) findViewById(R.id.start_btn);

        hudScore = (TextView) findViewById(R.id.hud_score);
        hudLevel = (TextView) findViewById(R.id.hud_level);
        hudSpeed = (TextView) findViewById(R.id.hud_speed);
        hudFuelProgress = (ProgressBar) findViewById(R.id.hud_fuel_progress);

        goFinalScore = (TextView) findViewById(R.id.go_final_score);
        goCoins = (TextView) findViewById(R.id.go_coins);
        goMaxSpeed = (TextView) findViewById(R.id.go_max_speed);
        replayBtn = (Button) findViewById(R.id.replay_btn);
        goMenuBtn = (Button) findViewById(R.id.go_menu_btn);

        sharedPrefs = getSharedPreferences("CarRacerPrefs", Context.MODE_PRIVATE);
        soundManager = new SoundManager();

        // Set initial score records
        updateHighScoreLabel();

        // Setup dynamic Game Canvas instance
        gameView = new GameView(this);
        gameView.setGameListener(this);
        gameContainer.addView(gameView);

        // Button Triggers
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startGame();
            }
        });

        replayBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startGame();
            }
        });

        goMenuBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exitToMenu();
            }
        });
    }

    private void startGame() {
        // Set difficulty parameters
        float modifier = 1.0f;
        int checkedId = difficultyGroup.getCheckedRadioButtonId();
        if (checkedId == R.id.diff_med) {
            modifier = 1.4f;
        } else if (checkedId == R.id.diff_hard) {
            modifier = 1.9f;
        }

        gameView.configureDifficulty(modifier);

        // Toggle visual overlays
        menuLayout.setVisibility(View.GONE);
        gameOverLayout.setVisibility(View.GONE);
        hudLayout.setVisibility(View.VISIBLE);

        // Fire start sequence
        gameView.startNewGame();
    }

    private void exitToMenu() {
        gameOverLayout.setVisibility(View.GONE);
        hudLayout.setVisibility(View.GONE);
        menuLayout.setVisibility(View.VISIBLE);
        updateHighScoreLabel();
    }

    private void updateHighScoreLabel() {
        int record = sharedPrefs.getInt(PREF_KEY_HIGH_SCORE, 0);
        highScoreTxt.setText("BEST RECORD: " + record + " XP");
    }

    @Override
    public void onScoreUpdated(final int score, final int currentSpeed, final int currentFuel, final int currentLevel) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                hudScore.setText("SCORE: " + score);
                hudLevel.setText("LEVEL " + currentLevel);
                hudSpeed.setText(currentSpeed + " MPH");
                hudFuelProgress.setProgress(currentFuel);
            }
        });
    }

    @Override
    public void onGameOver(final int finalScore, final int coins, final int maxSpeed) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Save new record persistence validation
                int storedRecord = sharedPrefs.getInt(PREF_KEY_HIGH_SCORE, 0);
                if (finalScore > storedRecord) {
                    SharedPreferences.Editor editor = sharedPrefs.edit();
                    editor.putInt(PREF_KEY_HIGH_SCORE, finalScore);
                    editor.apply();
                }

                // Populate game-over metrics panel
                goFinalScore.setText("FINAL SCORE: " + finalScore);
                goCoins.setText("COINS COLLECTED: " + coins);
                goMaxSpeed.setText("TOP SPEED: " + maxSpeed + " MPH");

                // Toggle visible panes
                hudLayout.setVisibility(View.GONE);
                gameOverLayout.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void playCoinSfx() {
        if (soundManager != null) {
            soundManager.playCoinSound();
        }
    }

    @Override
    public void playLevelUpSfx() {
        if (soundManager != null) {
            soundManager.playLevelUpSound();
        }
    }

    @Override
    public void playCrashSfx() {
        if (soundManager != null) {
            soundManager.playCrashSound();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (gameView != null) {
            gameView.pauseGame();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (soundManager != null) {
            soundManager.release();
        }
    }
}