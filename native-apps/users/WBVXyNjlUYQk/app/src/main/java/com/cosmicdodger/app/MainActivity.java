package com.cosmicdodger.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {

    private GameView gameView;
    private View startLayout;
    private View hudLayout;
    private View gameOverLayout;

    private TextView tvScore;
    private TextView tvLives;
    private TextView tvStartHighScore;
    private TextView tvFinalScore;
    private TextView tvGameOverHighScore;

    private SharedPreferences prefs;
    private static final String PREF_HIGH_SCORE = "high_score_key";
    private int highScore = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gameView = (GameView) findViewById(R.id.game_view);
        startLayout = findViewById(R.id.start_layout);
        hudLayout = findViewById(R.id.hud_layout);
        gameOverLayout = findViewById(R.id.game_over_layout);

        tvScore = (TextView) findViewById(R.id.tv_score);
        tvLives = (TextView) findViewById(R.id.tv_lives);
        tvStartHighScore = (TextView) findViewById(R.id.tv_start_high_score);
        tvFinalScore = (TextView) findViewById(R.id.tv_final_score);
        tvGameOverHighScore = (TextView) findViewById(R.id.tv_game_over_high_score);

        prefs = getSharedPreferences("CosmicDodgerPreferences", Context.MODE_PRIVATE);
        highScore = prefs.getInt(PREF_HIGH_SCORE, 0);
        tvStartHighScore.setText("HIGH SCORE: " + highScore);

        Button btnStart = (Button) findViewById(R.id.btn_start);
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startGame();
            }
        });

        Button btnRestart = (Button) findViewById(R.id.btn_restart);
        btnRestart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startGame();
            }
        });

        gameView.setGameListener(new GameView.GameListener() {
            @Override
            public void onScoreChanged(final int scoreValue) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvScore.setText("SCORE: " + scoreValue);
                    }
                });
            }

            @Override
            public void onLivesChanged(final int livesValue) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        StringBuilder sb = new StringBuilder("LIVES: ");
                        for (int i = 0; i < livesValue; i++) {
                            sb.append("❤");
                        }
                        if (livesValue <= 0) {
                            sb.append("💀");
                        }
                        tvLives.setText(sb.toString());
                    }
                });
            }

            @Override
            public void onGameOver(final int finalScore) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (finalScore > highScore) {
                            highScore = finalScore;
                            prefs.edit().putInt(PREF_HIGH_SCORE, highScore).apply();
                        }
                        tvFinalScore.setText("FINAL SCORE: " + finalScore);
                        tvGameOverHighScore.setText("BEST RECORD: " + highScore);
                        tvStartHighScore.setText("HIGH SCORE: " + highScore);

                        hudLayout.setVisibility(View.GONE);
                        gameOverLayout.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
    }

    private void startGame() {
        startLayout.setVisibility(View.GONE);
        gameOverLayout.setVisibility(View.GONE);
        hudLayout.setVisibility(View.VISIBLE);

        tvScore.setText("SCORE: 0");
        tvLives.setText("LIVES: ❤❤❤");

        gameView.startNewGame();
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
        if (gameView != null) {
            gameView.resumeGame();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (gameView != null) {
            gameView.releaseResources();
        }
    }
}