package com.cyberrunner.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

public class MainActivity extends Activity implements CyberRenderer.GameCallback {

    private CyberGLSurfaceView glSurfaceView;
    private TextView tvScore;
    private TextView tvCoins;
    private TextView tvHighScore;
    private TextView tvSpeed;

    private LinearLayout overlayReady;
    private LinearLayout overlayGameOver;
    private TextView tvFinalScore;
    private TextView tvFinalCoins;

    private Button btnOverlayStart;
    private Button btnOverlayRetry;
    private Button btnOverlayLeaderboard;

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

        glSurfaceView = (CyberGLSurfaceView) findViewById(R.id.gl_surface_view);
        tvScore = (TextView) findViewById(R.id.tv_score);
        tvCoins = (TextView) findViewById(R.id.tv_coins);
        tvHighScore = (TextView) findViewById(R.id.tv_high_score);
        tvSpeed = (TextView) findViewById(R.id.tv_speed);

        overlayReady = (LinearLayout) findViewById(R.id.overlay_ready);
        overlayGameOver = (LinearLayout) findViewById(R.id.overlay_game_over);
        tvFinalScore = (TextView) findViewById(R.id.tv_final_score);
        tvFinalCoins = (TextView) findViewById(R.id.tv_final_coins);

        btnOverlayStart = (Button) findViewById(R.id.btn_overlay_start);
        btnOverlayRetry = (Button) findViewById(R.id.btn_overlay_retry);
        btnOverlayLeaderboard = (Button) findViewById(R.id.btn_overlay_leaderboard);

        btnSoundToggle = (Button) findViewById(R.id.btn_sound_toggle);
        btnLeaderboard = (Button) findViewById(R.id.btn_leaderboard);

        CyberRenderer renderer = glSurfaceView.getCyberRenderer();
        renderer.setGameCallback(this);
        renderer.setSoundManager(soundManager);
        renderer.setScoreManager(scoreManager);

        updateSoundButtonLabel();
        updateHighScoreDisplay();

        btnOverlayStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                overlayReady.setVisibility(View.GONE);
                glSurfaceView.triggerStartOrRestart();
            }
        });

        btnOverlayRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                overlayGameOver.setVisibility(View.GONE);
                glSurfaceView.triggerStartOrRestart();
            }
        });

        btnOverlayLeaderboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openLeaderboard();
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
                openLeaderboard();
            }
        });
    }

    private void openLeaderboard() {
        Intent intent = new Intent(MainActivity.this, LeaderboardActivity.class);
        startActivity(intent);
    }

    private void updateHighScoreDisplay() {
        if (scoreManager != null && tvHighScore != null) {
            tvHighScore.setText(scoreManager.getTopScore() + " m");
        }
    }

    private void updateSoundButtonLabel() {
        if (soundManager != null && btnSoundToggle != null) {
            btnSoundToggle.setText(soundManager.isSoundEnabled() ? getString(R.string.sound_on) : getString(R.string.sound_off));
        }
    }

    @Override
    public void onScoreUpdated(final int distance, final int coins, final float speedMultiplier) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvScore.setText(distance + " m");
                tvCoins.setText(String.valueOf(coins));
                tvSpeed.setText(String.format(Locale.US, "%.1fx SPEED", speedMultiplier));
            }
        });
    }

    @Override
    public void onGameOver(final int finalDistance, final int finalCoins) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateHighScoreDisplay();
                tvFinalScore.setText("DISTANCE: " + finalDistance + " m");
                tvFinalCoins.setText("CYBER COINS: " + finalCoins);
                overlayGameOver.setVisibility(View.VISIBLE);
                overlayReady.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onGameStarted() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                overlayReady.setVisibility(View.GONE);
                overlayGameOver.setVisibility(View.GONE);
                updateHighScoreDisplay();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (glSurfaceView != null) {
            glSurfaceView.onResume();
        }
        updateHighScoreDisplay();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (glSurfaceView != null) {
            glSurfaceView.onPause();
        }
    }
}