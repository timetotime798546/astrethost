package com.retrosnake.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity implements SnakeView.GameListener {

    private static final String PREFS_NAME = "RetroSnakePrefs";
    private static final String KEY_HIGH_SCORE = "high_score";

    private SnakeView snakeView;
    private TextView tvScore;
    private TextView tvHighScore;
    private Button btnSpeed;
    private Button btnAction;
    private Button btnReset;
    private LinearLayout layoutOverlay;
    private TextView tvOverlayTitle;
    private TextView tvOverlaySubtitle;
    private Button btnOverlayAction;

    // Speeds in milliseconds per tick
    private static final int SPEED_EASY = 220;
    private static final int SPEED_NORMAL = 150;
    private static final int SPEED_FAST = 95;

    private int currentSpeedMode = 1; // 0: Easy, 1: Normal, 2: Fast
    private int tickInterval = SPEED_NORMAL;

    private int currentScore = 0;
    private int highScore = 0;

    private SharedPreferences prefs;
    private Vibrator vibrator;
    private Handler gameHandler;
    private Runnable gameRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        highScore = prefs.getInt(KEY_HIGH_SCORE, 0);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        gameHandler = new Handler();

        initViews();
        setupListeners();

        tvHighScore.setText(String.valueOf(highScore));

        gameRunnable = new Runnable() {
            @Override
            public void run() {
                if (snakeView != null && snakeView.getGameState() == SnakeView.GameState.RUNNING) {
                    snakeView.step();
                    gameHandler.postDelayed(this, tickInterval);
                }
            }
        };

        // Start initial state
        showReadyOverlay();
    }

    private void initViews() {
        snakeView = (SnakeView) findViewById(R.id.snake_view);
        tvScore = (TextView) findViewById(R.id.tv_score);
        tvHighScore = (TextView) findViewById(R.id.tv_high_score);
        btnSpeed = (Button) findViewById(R.id.btn_speed);
        btnAction = (Button) findViewById(R.id.btn_action);
        btnReset = (Button) findViewById(R.id.btn_reset);

        layoutOverlay = (LinearLayout) findViewById(R.id.layout_overlay);
        tvOverlayTitle = (TextView) findViewById(R.id.tv_overlay_title);
        tvOverlaySubtitle = (TextView) findViewById(R.id.tv_overlay_subtitle);
        btnOverlayAction = (Button) findViewById(R.id.btn_overlay_action);

        snakeView.setGameListener(this);
    }

    private void setupListeners() {
        // D-Pad navigation
        findViewById(R.id.btn_up).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ensureGameRunning();
                snakeView.setDirection(SnakeView.Direction.UP);
            }
        });

        findViewById(R.id.btn_down).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ensureGameRunning();
                snakeView.setDirection(SnakeView.Direction.DOWN);
            }
        });

        findViewById(R.id.btn_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ensureGameRunning();
                snakeView.setDirection(SnakeView.Direction.LEFT);
            }
        });

        findViewById(R.id.btn_right).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ensureGameRunning();
                snakeView.setDirection(SnakeView.Direction.RIGHT);
            }
        });

        // Speed Toggle
        btnSpeed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cycleSpeed();
            }
        });

        // Action button (Pause / Resume / Start)
        btnAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePlayPause();
            }
        });

        // Reset button
        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetAndStartGame();
            }
        });

        // Overlay Action button
        btnOverlayAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetAndStartGame();
            }
        });
    }

    private void ensureGameRunning() {
        if (snakeView.getGameState() == SnakeView.GameState.READY ||
            snakeView.getGameState() == SnakeView.GameState.PAUSED) {
            startGameLoop();
        }
    }

    private void cycleSpeed() {
        currentSpeedMode = (currentSpeedMode + 1) % 3;
        switch (currentSpeedMode) {
            case 0:
                tickInterval = SPEED_EASY;
                btnSpeed.setText(getString(R.string.speed_slow));
                break;
            case 1:
                tickInterval = SPEED_NORMAL;
                btnSpeed.setText(getString(R.string.speed_normal));
                break;
            case 2:
                tickInterval = SPEED_FAST;
                btnSpeed.setText(getString(R.string.speed_fast));
                break;
        }
    }

    private void togglePlayPause() {
        SnakeView.GameState state = snakeView.getGameState();
        if (state == SnakeView.GameState.RUNNING) {
            pauseGameLoop();
        } else if (state == SnakeView.GameState.PAUSED || state == SnakeView.GameState.READY) {
            startGameLoop();
        } else if (state == SnakeView.GameState.GAME_OVER) {
            resetAndStartGame();
        }
    }

    private void startGameLoop() {
        layoutOverlay.setVisibility(View.GONE);
        snakeView.startGame();
        btnAction.setText(getString(R.string.btn_pause));
        gameHandler.removeCallbacks(gameRunnable);
        gameHandler.postDelayed(gameRunnable, tickInterval);
    }

    private void pauseGameLoop() {
        snakeView.pauseGame();
        gameHandler.removeCallbacks(gameRunnable);
        btnAction.setText(getString(R.string.btn_resume));
        showPausedOverlay();
    }

    private void resetAndStartGame() {
        gameHandler.removeCallbacks(gameRunnable);
        snakeView.resetGame();
        startGameLoop();
    }

    private void showReadyOverlay() {
        layoutOverlay.setVisibility(View.VISIBLE);
        tvOverlayTitle.setText(getString(R.string.app_name));
        tvOverlayTitle.setTextColor(getResources().getColor(R.color.snake_head));
        tvOverlaySubtitle.setText("Swipe or use D-Pad to steer!");
        btnOverlayAction.setText(getString(R.string.btn_start));
        btnAction.setText(getString(R.string.btn_start));
    }

    private void showPausedOverlay() {
        layoutOverlay.setVisibility(View.VISIBLE);
        tvOverlayTitle.setText("PAUSED");
        tvOverlayTitle.setTextColor(getResources().getColor(R.color.accent_yellow));
        tvOverlaySubtitle.setText("Game is paused");
        btnOverlayAction.setText(getString(R.string.btn_resume));
    }

    @Override
    public void onScoreUpdated(int score) {
        currentScore = score;
        tvScore.setText(String.valueOf(currentScore));

        if (currentScore > highScore) {
            highScore = currentScore;
            tvHighScore.setText(String.valueOf(highScore));
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(KEY_HIGH_SCORE, highScore);
            editor.apply();
        }
    }

    @Override
    public void onFoodEaten(boolean isBonus) {
        if (vibrator != null && vibrator.hasVibrator()) {
            try {
                vibrator.vibrate(isBonus ? 50 : 25);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void onGameOver(int finalScore) {
        gameHandler.removeCallbacks(gameRunnable);
        if (vibrator != null && vibrator.hasVibrator()) {
            try {
                vibrator.vibrate(200);
            } catch (Exception ignored) {
            }
        }

        layoutOverlay.setVisibility(View.VISIBLE);
        tvOverlayTitle.setText(getString(R.string.game_over_title));
        tvOverlayTitle.setTextColor(getResources().getColor(R.color.food_apple));
        tvOverlaySubtitle.setText("Final Score: " + finalScore);
        btnOverlayAction.setText(getString(R.string.btn_restart));
        btnAction.setText(getString(R.string.btn_restart));
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (snakeView != null && snakeView.getGameState() == SnakeView.GameState.RUNNING) {
            pauseGameLoop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (gameHandler != null && gameRunnable != null) {
            gameHandler.removeCallbacks(gameRunnable);
        }
    }
}