package com.retrobrickbreaker.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class MainActivity extends Activity implements GameView.GameListener {

    private GameView mGameView;
    private TextView mTvScore;
    private TextView mTvLevel;
    private TextView mTvLives;
    
    // Welcome Overlay elements
    private LinearLayout mOverlayStart;
    private TextView mTvHighScore;
    private Button mBtnPlay;
    private Button mBtnToggleSound;

    // Game Over Overlay elements
    private LinearLayout mOverlayGameOver;
    private TextView mTvGameResultTitle;
    private TextView mTvFinalScore;
    private TextView mTvNewRecord;
    private Button mBtnRetry;

    // Save states configuration
    private SharedPreferences mPrefs;
    private static final String PREFS_NAME = "RetroBrickBreakerPrefs";
    private static final String KEY_HIGH_SCORE = "high_score_key";
    private int mHighScore = 0;
    private boolean mSoundEnabled = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Load Persistent Preferences values
        mPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        mHighScore = mPrefs.getInt(KEY_HIGH_SCORE, 0);

        // Instantiate components and XML interfaces
        mGameView = (GameView) findViewById(R.id.game_view);
        mTvScore = (TextView) findViewById(R.id.tv_score);
        mTvLevel = (TextView) findViewById(R.id.tv_level);
        mTvLives = (TextView) findViewById(R.id.tv_lives);

        mOverlayStart = (LinearLayout) findViewById(R.id.overlay_start);
        mTvHighScore = (TextView) findViewById(R.id.tv_high_score);
        mBtnPlay = (Button) findViewById(R.id.btn_play);
        mBtnToggleSound = (Button) findViewById(R.id.btn_toggle_sound);

        mOverlayGameOver = (LinearLayout) findViewById(R.id.overlay_game_over);
        mTvGameResultTitle = (TextView) findViewById(R.id.tv_game_result_title);
        mTvFinalScore = (TextView) findViewById(R.id.tv_final_score);
        mTvNewRecord = (TextView) findViewById(R.id.tv_new_record);
        mBtnRetry = (Button) findViewById(R.id.btn_retry);

        mGameView.setGameListener(this);
        mTvHighScore.setText("HIGH SCORE: " + mHighScore);

        // Configure Play and Button Listeners inside clean Java 8 compatibility bounds
        mBtnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startGame();
            }
        });

        mBtnToggleSound.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSoundEnabled = !mSoundEnabled;
                mGameView.setSoundEnabled(mSoundEnabled);
                mBtnToggleSound.setText("SOUND: " + (mSoundEnabled ? "ON" : "OFF"));
            }
        });

        mBtnRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startGame();
            }
        });
    }

    private void startGame() {
        mOverlayStart.setVisibility(View.GONE);
        mOverlayGameOver.setVisibility(View.GONE);
        mGameView.startNewGame();
    }

    // Callback listeners connecting retro Canvas activities back to the primary views
    @Override
    public void onScoreChanged(final int score) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTvScore.setText("SCORE: " + padScore(score));
            }
        });
    }

    @Override
    public void onLivesChanged(final int lives) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                StringBuilder heartString = new StringBuilder();
                for (int i = 0; i < lives; i++) {
                    heartString.append("❤");
                }
                mTvLives.setText("LIVES: " + heartString.toString());
            }
        });
    }

    @Override
    public void onLevelChanged(final int level) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTvLevel.setText("LEVEL: " + level);
            }
        });
    }

    @Override
    public void onGameOver(final int score, boolean isHighScore) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mOverlayGameOver.setVisibility(View.VISIBLE);
                mTvGameResultTitle.setText("GAME OVER");
                mTvGameResultTitle.setTextColor(0xFFFF3366);
                mTvFinalScore.setText("FINAL SCORE: " + padScore(score));

                checkAndSaveHighScore(score);
            }
        });
    }

    @Override
    public void onGameWon(final int score, boolean isHighScore) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mOverlayGameOver.setVisibility(View.VISIBLE);
                mTvGameResultTitle.setText("VICTORY!");
                mTvGameResultTitle.setTextColor(0xFF00FFCC);
                mTvFinalScore.setText("FINAL SCORE: " + padScore(score));

                checkAndSaveHighScore(score);
            }
        });
    }

    private void checkAndSaveHighScore(int score) {
        if (score > mHighScore) {
            mHighScore = score;
            mPrefs.edit().putInt(KEY_HIGH_SCORE, mHighScore).apply();
            mTvHighScore.setText("HIGH SCORE: " + mHighScore);
            mTvNewRecord.setVisibility(View.VISIBLE);
        } else {
            mTvNewRecord.setVisibility(View.GONE);
        }
    }

    private String padScore(int score) {
        return String.format("%05d", score);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mGameView != null) {
            mGameView.pauseGame();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mGameView != null) {
            mGameView.resumeGame();
        }
    }
}