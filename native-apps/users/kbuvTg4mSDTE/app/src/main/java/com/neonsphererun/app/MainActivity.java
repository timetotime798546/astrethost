package com.neonsphererun.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class MainActivity extends Activity {

    private GameView gameView;
    private RelativeLayout hudLayout;
    private LinearLayout menuLayout;
    private LinearLayout gameOverLayout;
    
    private TextView scoreText;
    private TextView speedText;
    private TextView menuHighScoreText;
    private TextView gameOverScore;
    private TextView gameOverHighScore;
    private TextView newHighScoreText;
    
    private SharedPreferences prefs;
    private float steeringSensitivity = 0.5f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("NeonSpherePrefs", Context.MODE_PRIVATE);

        gameView = findViewById(R.id.game_view);
        hudLayout = findViewById(R.id.hud_layout);
        menuLayout = findViewById(R.id.menu_layout);
        gameOverLayout = findViewById(R.id.game_over_layout);

        scoreText = findViewById(R.id.score_text);
        speedText = findViewById(R.id.speed_text);
        menuHighScoreText = findViewById(R.id.menu_high_score_text);
        gameOverScore = findViewById(R.id.game_over_score);
        gameOverHighScore = findViewById(R.id.game_over_highscore);
        newHighScoreText = findViewById(R.id.new_high_score_badge);

        Button startButton = findViewById(R.id.start_button);
        Button restartButton = findViewById(R.id.restart_button);
        SeekBar sensitivitySeekBar = findViewById(R.id.sensitivity_seekbar);

        // Load persisted high score and display
        int highScore = prefs.getInt("high_score", 0);
        menuHighScoreText.setText("RECORD: " + highScore + "m");

        // Set up interactive steering control sensitivity
        steeringSensitivity = prefs.getFloat("sensitivity", 0.5f);
        sensitivitySeekBar.setProgress((int) (steeringSensitivity * 100));
        gameView.setSteeringSensitivity(steeringSensitivity);

        sensitivitySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                steeringSensitivity = progress / 100f;
                if (steeringSensitivity < 0.1f) steeringSensitivity = 0.1f;
                gameView.setSteeringSensitivity(steeringSensitivity);
                prefs.edit().putFloat("sensitivity", steeringSensitivity).apply();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Set up the listener interfaces from the game execution thread
        gameView.setGameListener(new GameView.GameListener() {
            @Override
            public void onScoreChanged(final int score, final float speed) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        scoreText.setText("DISTANCE: " + score + "m");
                        speedText.setText(String.format("SPEED: %.1fx", speed));
                    }
                });
            }

            @Override
            public void onGameOver(final int finalScore) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        int currentHighScore = prefs.getInt("high_score", 0);
                        if (finalScore > currentHighScore) {
                            currentHighScore = finalScore;
                            prefs.edit().putInt("high_score", currentHighScore).apply();
                            newHighScoreText.setVisibility(View.VISIBLE);
                        } else {
                            newHighScoreText.setVisibility(View.GONE);
                        }
                        gameOverScore.setText("DISTANCE: " + finalScore + "m");
                        gameOverHighScore.setText("BEST RECORD: " + currentHighScore + "m");
                        
                        gameOverLayout.setVisibility(View.VISIBLE);
                        hudLayout.setVisibility(View.GONE);
                        menuLayout.setVisibility(View.GONE);
                    }
                });
            }

            @Override
            public void onGameStarted() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        menuLayout.setVisibility(View.GONE);
                        gameOverLayout.setVisibility(View.GONE);
                        hudLayout.setVisibility(View.VISIBLE);
                    }
                });
            }
        });

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gameView.startGame();
            }
        });

        restartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gameView.startGame();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        gameView.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        gameView.resume();
    }
}