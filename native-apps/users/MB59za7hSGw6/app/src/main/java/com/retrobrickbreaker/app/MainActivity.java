package com.retrobrickbreaker.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

public class MainActivity extends Activity implements GameView.GameStateListener {

    private GameView gameView;
    private Button btnPlay;
    private Button btnRestart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable full screen window
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        setContentView(R.layout.activity_main);

        gameView = findViewById(R.id.gameView);
        btnPlay = findViewById(R.id.btnPlay);
        btnRestart = findViewById(R.id.btnRestart);

        gameView.setGameStateListener(this);

        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int state = gameView.getGameState();
                if (state == GameView.STATE_START || state == GameView.STATE_GAME_OVER) {
                    gameView.startNewGame();
                    btnPlay.setText(R.string.btn_resume);
                } else if (state == GameView.STATE_PLAYING) {
                    gameView.setGameState(GameView.STATE_PAUSED);
                    btnPlay.setText(R.string.btn_resume);
                } else if (state == GameView.STATE_PAUSED) {
                    gameView.setGameState(GameView.STATE_PLAYING);
                    btnPlay.setText(R.string.btn_play);
                } else if (state == GameView.STATE_LEVEL_CLEARED) {
                    gameView.nextLevel();
                    btnPlay.setText(R.string.btn_play);
                }
            }
        });

        btnRestart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gameView.startNewGame();
                btnPlay.setText(R.string.btn_play);
            }
        });
    }

    @Override
    public void onScoreUpdated(int score, int highScore, int lives, int level) {
        // UI updates dispatched from GameView engine thread
    }

    @Override
    public void onGameOver(final int finalScore) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                btnPlay.setText(R.string.btn_restart);
            }
        });
    }

    @Override
    public void onLevelCleared(final int nextLevel) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                btnPlay.setText(R.string.btn_play);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (gameView != null) {
            gameView.pauseGameThread();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gameView != null) {
            gameView.resumeGameThread();
        }
    }
}