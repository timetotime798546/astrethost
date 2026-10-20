package com.runner3d.app;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity implements GameView.GameStateListener {

    private static final String PREFS_NAME = "Runner3DPrefs";
    private static final String KEY_HIGH_SCORE = "high_score_distance";

    private GameView gameView;
    private TextView tvScore;
    private TextView tvGems;
    private TextView tvSpeed;

    private LinearLayout startOverlay;
    private TextView tvStartHighScore;
    private Button btnStart;

    private LinearLayout gameOverOverlay;
    private TextView tvFinalScore;
    private TextView tvFinalGems;
    private TextView tvBestRecord;
    private Button btnRetry;

    private Button btnLeft;
    private Button btnRight;
    private Button btnJump;
    private Button btnSlide;

    private SharedPreferences prefs;
    private int bestDistance = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Keep screen on during dynamic 3D runner gameplay
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        bestDistance = prefs.getInt(KEY_HIGH_SCORE, 0);

        gameView = (GameView) findViewById(R.id.game_view);
        gameView.setGameStateListener(this);

        tvScore = (TextView) findViewById(R.id.tv_score);
        tvGems = (TextView) findViewById(R.id.tv_gems);
        tvSpeed = (TextView) findViewById(R.id.tv_speed);

        startOverlay = (LinearLayout) findViewById(R.id.start_overlay);
        tvStartHighScore = (TextView) findViewById(R.id.tv_start_high_score);
        btnStart = (Button) findViewById(R.id.btn_start);

        gameOverOverlay = (LinearLayout) findViewById(R.id.game_over_overlay);
        tvFinalScore = (TextView) findViewById(R.id.tv_final_score);
        tvFinalGems = (TextView) findViewById(R.id.tv_final_gems);
        tvBestRecord = (TextView) findViewById(R.id.tv_best_record);
        btnRetry = (Button) findViewById(R.id.btn_retry);

        btnLeft = (Button) findViewById(R.id.btn_left);
        btnRight = (Button) findViewById(R.id.btn_right);
        btnJump = (Button) findViewById(R.id.btn_jump);
        btnSlide = (Button) findViewById(R.id.btn_slide);

        tvStartHighScore.setText("BEST RECORD: " + bestDistance + " m");

        // Action button listeners
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startOverlay.setVisibility(View.GONE);
                gameView.startNewGame();
            }
        });

        btnRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gameOverOverlay.setVisibility(View.GONE);
                gameView.startNewGame();
            }
        });

        btnLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gameView.moveLeft();
            }
        });

        btnRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gameView.moveRight();
            }
        });

        btnJump.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gameView.jump();
            }
        });

        btnSlide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gameView.slide();
            }
        });
    }

    @Override
    public void onScoreUpdated(final int distanceMeters, final int gems, final float speedMultiplier) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvScore.setText(distanceMeters + " m");
                tvGems.setText(String.valueOf(gems));
                tvSpeed.setText(String.format("%.1fx", speedMultiplier));
            }
        });
    }

    @Override
    public void onGameOver(final int finalDistance, final int finalGems) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                boolean isNewRecord = false;
                if (finalDistance > bestDistance) {
                    bestDistance = finalDistance;
                    prefs.edit().putInt(KEY_HIGH_SCORE, bestDistance).apply();
                    isNewRecord = true;
                }

                tvFinalScore.setText("DISTANCE: " + finalDistance + " m");
                tvFinalGems.setText("GEMS COLLECTED: " + finalGems);

                if (isNewRecord) {
                    tvBestRecord.setText("NEW RECORD ACHIEVED: " + bestDistance + " m!");
                    tvBestRecord.setTextColor(getResources().getColor(R.color.neon_yellow));
                } else {
                    tvBestRecord.setText("BEST RECORD: " + bestDistance + " m");
                    tvBestRecord.setTextColor(getResources().getColor(R.color.neon_cyan));
                }

                gameOverOverlay.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
}