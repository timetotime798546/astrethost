package com.cyberrunner.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {

    private GameView gameView;
    private TextView tvScore;
    private TextView tvCoins;
    private TextView tvHighScore;
    private Button btnSoundToggle;
    private Button btnLeaderboard;

    private SoundManager soundManager;
    private ScoreManager scoreManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        soundManager = new SoundManager(this);
        scoreManager = new ScoreManager(this);

        gameView = (GameView) findViewById(R.id.game_view);
        tvScore = (TextView) findViewById(R.id.tv_score);
        tvCoins = (TextView) findViewById(R.id.tv_coins);
        tvHighScore = (TextView) findViewById(R.id.tv_high_score);
        btnSoundToggle = (Button) findViewById(R.id.btn_sound_toggle);
        btnLeaderboard = (Button) findViewById(R.id.btn_leaderboard);

        gameView.setSoundManager(soundManager);
        gameView.setScoreManager(scoreManager);

        updateSoundButtonLabel();
        updateHighScoreDisplay();

        gameView.setGameStateListener(new GameView.GameStateListener() {
            @Override
            public void onScoreUpdated(final int score, final int coins, final float speed) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvScore.setText(String.valueOf(score));
                        tvCoins.setText(String.valueOf(coins));
                    }
                });
            }

            @Override
            public void onGameOver(final int finalScore, final int coins) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateHighScoreDisplay();
                    }
                });
            }

            @Override
            public void onGameStarted() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateHighScoreDisplay();
                    }
                });
            }
        });

        btnSoundToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                soundManager.toggleSound();
                updateSoundButtonLabel();
            }
        });

        btnLeaderboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, LeaderboardActivity.class);
                startActivity(intent);
            }
        });
    }

    private void updateHighScoreDisplay() {
        if (scoreManager != null && tvHighScore != null) {
            tvHighScore.setText(String.valueOf(scoreManager.getTopScore()));
        }
    }

    private void updateSoundButtonLabel() {
        if (soundManager != null && btnSoundToggle != null) {
            btnSoundToggle.setText(soundManager.isSoundEnabled() ? getString(R.string.sound_on) : getString(R.string.sound_off));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateHighScoreDisplay();
    }
}