package com.motohillracer.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    private GameView gameView;
    private LinearLayout menuLayout;
    private LinearLayout instructionsLayout;
    private LinearLayout highScoreLayout;
    private RelativeLayout hudLayout;
    private LinearLayout gameOverLayout;

    // HUD items
    private TextView tvHudDistance;
    private TextView tvHudCoins;
    private ProgressBar pbFuel;

    // Game Over items
    private TextView tvGameOverTitle;
    private TextView tvGameOverReason;
    private TextView tvGameOverStats;

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) { 
        super.onCreate(savedInstanceState);

        // Set fullscreen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        sharedPreferences = getSharedPreferences("MotoHillRacerPrefs", Context.MODE_PRIVATE);

        // UI references
        gameView = (GameView) findViewById(R.id.game_view);
        menuLayout = (LinearLayout) findViewById(R.id.menu_layout);
        instructionsLayout = (LinearLayout) findViewById(R.id.instructions_layout);
        highScoreLayout = (LinearLayout) findViewById(R.id.high_score_layout);
        hudLayout = (RelativeLayout) findViewById(R.id.hud_layout);
        gameOverLayout = (LinearLayout) findViewById(R.id.game_over_layout);

        tvHudDistance = (TextView) findViewById(R.id.tv_hud_distance);
        tvHudCoins = (TextView) findViewById(R.id.tv_hud_coins);
        pbFuel = (ProgressBar) findViewById(R.id.pb_fuel);

        tvGameOverTitle = (TextView) findViewById(R.id.tv_game_over_title);
        tvGameOverReason = (TextView) findViewById(R.id.tv_game_over_reason);
        tvGameOverStats = (TextView) findViewById(R.id.tv_game_over_stats);

        // Setup Main Menu Buttons
        Button btnPlay = (Button) findViewById(R.id.btn_play);
        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startGame();
            }
        });

        Button btnHighScores = (Button) findViewById(R.id.btn_high_score);
        btnHighScores.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showHighScoreScreen();
            }
        });

        Button btnInstructions = (Button) findViewById(R.id.btn_instructions);
        btnInstructions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                instructionsLayout.setVisibility(View.VISIBLE);
            }
        });

        Button btnCloseInstructions = (Button) findViewById(R.id.btn_close_instructions);
        btnCloseInstructions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                instructionsLayout.setVisibility(View.GONE);
            }
        });

        Button btnCloseScores = (Button) findViewById(R.id.btn_close_scores);
        btnCloseScores.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                highScoreLayout.setVisibility(View.GONE);
            }
        });

        Button btnResetScores = (Button) findViewById(R.id.btn_reset_scores);
        btnResetScores.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetScores();
            }
        });

        // Setup Game Over Buttons
        Button btnRestart = (Button) findViewById(R.id.btn_restart);
        btnRestart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gameOverLayout.setVisibility(View.GONE);
                startGame();
            }
        });

        Button btnMenu = (Button) findViewById(R.id.btn_menu);
        btnMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gameOverLayout.setVisibility(View.GONE);
                menuLayout.setVisibility(View.VISIBLE);
                hudLayout.setVisibility(View.GONE);
            }
        });

        // Game Controls setup
        setupGameControls();

        // Register listener for HUD/State updates from GameView
        gameView.setGameListener(new GameView.GameListener() {
            @Override
            public void onUpdateHud(final float distance, final int coins, final float fuel) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvHudDistance.setText("Distance: " + (int) distance + "m");
                        tvHudCoins.setText("Coins: " + coins);
                        pbFuel.setProgress((int) fuel);
                    } 
                });
            }

            @Override
            public void onGameOver(final String reason, final int finalCoins, final float finalDistance, final boolean success) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        saveScores(finalCoins, (int) finalDistance);
                        hudLayout.setVisibility(View.GONE);
                        gameOverLayout.setVisibility(View.VISIBLE);

                        if (success) {
                            tvGameOverTitle.setText("VICTORY!");
                            tvGameOverTitle.setTextColor(0xFF10B981); // Emerald Green
                            tvGameOverReason.setText("You conquered the Moto Hill Race!");
                        } else {
                            tvGameOverTitle.setText("CRASHED!");
                            tvGameOverTitle.setTextColor(0xFFEF4444); // Red
                            tvGameOverReason.setText(reason);
                        }

                        tvGameOverStats.setText("Coins Collected: " + finalCoins + "\nDistance Covered: " + (int) finalDistance + "m");
                    }
                });
            }
        });
    }

    private void setupGameControls() {
        Button btnBrake = (Button) findViewById(R.id.btn_control_brake);
        btnBrake.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    gameView.setBrakePressed(true);
                } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                    gameView.setBrakePressed(false);
                }
                return true;
            }
        });

        Button btnTiltLeft = (Button) findViewById(R.id.btn_control_tilt_left);
        btnTiltLeft.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    gameView.setTiltLeftPressed(true);
                } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                    gameView.setTiltLeftPressed(false);
                }
                return true;
            }
        });

        Button btnTiltRight = (Button) findViewById(R.id.btn_control_tilt_right);
        btnTiltRight.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    gameView.setTiltRightPressed(true);
                } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                    gameView.setTiltRightPressed(false);
                }
                return true;
            }
        });

        Button btnGas = (Button) findViewById(R.id.btn_control_gas);
        btnGas.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    gameView.setGasPressed(true);
                } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                    gameView.setGasPressed(false);
                }
                return true;
            }
        });
    }

    private void startGame() {
        menuLayout.setVisibility(View.GONE);
        instructionsLayout.setVisibility(View.GONE);
        highScoreLayout.setVisibility(View.GONE);
        gameOverLayout.setVisibility(View.GONE);
        hudLayout.setVisibility(View.VISIBLE);

        gameView.initGame();
    }

    private void showHighScoreScreen() {
        highScoreLayout.setVisibility(View.VISIBLE);
        int bestCoins = sharedPreferences.getInt("bestCoins", 0);
        int maxDistance = sharedPreferences.getInt("maxDistance", 0);

        TextView tvScores = (TextView) findViewById(R.id.tv_high_score);
        tvScores.setText("Best Score: " + bestCoins + " Coins\nMax Distance: " + maxDistance + "m");
    }

    private void saveScores(int coins, int distance) {
        int bestCoins = sharedPreferences.getInt("bestCoins", 0);
        int maxDistance = sharedPreferences.getInt("maxDistance", 0);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (coins > bestCoins) {
            editor.putInt("bestCoins", coins);
        }
        if (distance > maxDistance) {
            editor.putInt("maxDistance", distance);
        }
        editor.apply();
    }

    private void resetScores() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("bestCoins", 0);
        editor.putInt("maxDistance", 0);
        editor.apply();
        showHighScoreScreen();
    }

    @Override
    protected void onPause() {
        super.onPause();
        gameView.pauseGame();
    }
}