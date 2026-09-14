package com.retrocarracer.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity implements GameView.GameInteractionListener {

    private GameView mGameView;
    private SoundManager mSoundManager;

    private TextView mTvScore;
    private TextView mTvHighScore;
    private TextView mTvFinalScore;
    private TextView mTvFinalHighScoreMsg;

    private LinearLayout mLayoutStartScreen;
    private LinearLayout mLayoutGameOverScreen;

    private int mHighScore = 0;
    private SharedPreferences mPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPrefs = getSharedPreferences("RetroCarRacerPrefs", Context.MODE_PRIVATE);
        mHighScore = mPrefs.getInt("HighScore", 0);

        mSoundManager = new SoundManager();

        mGameView = (GameView) findViewById(R.id.game_view);
        mTvScore = (TextView) findViewById(R.id.tv_score);
        mTvHighScore = (TextView) findViewById(R.id.tv_high_score);
        mTvFinalScore = (TextView) findViewById(R.id.tv_final_score);
        mTvFinalHighScoreMsg = (TextView) findViewById(R.id.tv_final_high_score);

        mLayoutStartScreen = (LinearLayout) findViewById(R.id.layout_start_screen);
        mLayoutGameOverScreen = (LinearLayout) findViewById(R.id.layout_game_over);

        Button btnStartGame = (Button) findViewById(R.id.btn_start_game);
        Button btnRestart = (Button) findViewById(R.id.btn_restart);
        Button btnLeft = (Button) findViewById(R.id.btn_left);
        Button btnRight = (Button) findViewById(R.id.btn_right);

        mTvHighScore.setText("HI-SCORE: " + mHighScore);
        mGameView.setGameInteractionListener(this);

        // Apply dynamic listeners compatible with older Java architectures
        btnStartGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLayoutStartScreen.setVisibility(View.GONE);
                mLayoutGameOverScreen.setVisibility(View.GONE);
                mGameView.startGame();
            }
        });

        btnRestart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLayoutGameOverScreen.setVisibility(View.GONE);
                mGameView.startGame();
            }
        });

        btnLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mGameView.steerLeft();
            }
        });

        btnRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mGameView.steerRight();
            }
        });
    }

    @Override
    public void onScoreUpdated(final int score) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTvScore.setText("SCORE: " + score);
                mSoundManager.playScoreSound();
            }
        });
    }

    @Override
    public void onCoinCollected() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mSoundManager.playCoinSound();
            }
        });
    }

    @Override
    public void onCrashOccurred() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mSoundManager.playCrashSound();
                
                // Read exact final game session run scores
                String scoreText = mTvScore.getText().toString();
                int scoreVal = 0;
                try {
                    scoreVal = Integer.parseInt(scoreText.replaceAll("[^0-9]", ""));
                } catch (Exception e) {
                    // fallbacks
                }

                mTvFinalScore.setText("FINAL SCORE: " + scoreVal);

                // Update High Scores record
                if (scoreVal > mHighScore) {
                    mHighScore = scoreVal;
                    SharedPreferences.Editor editor = mPrefs.edit();
                    editor.putInt("HighScore", mHighScore);
                    editor.apply();

                    mTvHighScore.setText("HI-SCORE: " + mHighScore);
                    mTvFinalHighScoreMsg.setText("🏆 NEW RECORD SET!");
                    mTvFinalHighScoreMsg.setVisibility(View.VISIBLE);
                } else {
                    mTvFinalHighScoreMsg.setVisibility(View.GONE);
                }

                mLayoutGameOverScreen.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mGameView != null) {
            mGameView.stopGame();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSoundManager != null) {
            mSoundManager.release();
        }
    }
}