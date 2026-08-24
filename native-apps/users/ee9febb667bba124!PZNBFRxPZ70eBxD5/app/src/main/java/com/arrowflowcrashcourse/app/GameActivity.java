package com.arrowflowcrashcourse.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.view.View;

public class GameActivity extends Activity implements GameEngine.GameListener {
    private GameEngine gameEngine;
    private GameView gameView;
    private TextView levelText;
    private TextView scoreText;
    private int levelNum;
    private boolean darkTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) { 
        super.onCreate(savedInstanceState);
        darkTheme = ThemeManager.isDarkMode(this);
        setTheme(darkTheme ? android.R.style.Theme_Material : android.R.style.Theme_Material_Light);
        setContentView(R.layout.activity_game);

        levelNum = getIntent().getIntExtra("LEVEL_NUM", 1);

        levelText = findViewById(R.id.text_level_hud);
        scoreText = findViewById(R.id.text_score_hud);
        gameView = findViewById(R.id.game_view_render);

        Button restartButton = findViewById(R.id.btn_restart_level);
        restartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                restartLevel();
            }
        });

        initGame();
    }

    private void initGame() {
        levelText.setText("Level: " + levelNum + " / 10");
        scoreText.setText("Score: 0");

        gameEngine = new GameEngine(this, levelNum, this);
        gameView.setGameEngine(gameEngine);
        gameView.startAnimation();
    }

    private void restartLevel() {
        // Halt active anim frames to ensure atomic grid updates
        gameView.stopAnimation();
        gameEngine.loadLevel(levelNum);
        gameView.setGameEngine(gameEngine);
        gameView.startAnimation();
    }

    @Override
    public void onLevelComplete() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                gameView.stopAnimation();
                showLevelCompleteDialog();
            }
        });
    }

    @Override
    public void onGameOver() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                gameView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        gameView.stopAnimation();
                        showGameOverDialog();
                    }
                }, 1500);
            }
        });
    }

    @Override
    public void onScoreUpdated(final int score) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                scoreText.setText("Score: " + score);
            }
        });
    }

    @Override
    public void onParticlesSpawned() {}

    private void showLevelCompleteDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🌌 LEVEL COMPLETED!");
        builder.setMessage("Outstanding flight path calculations!\nYou scored " + gameEngine.getScore() + " points.");

        if (levelNum < 10) {
            builder.setPositiveButton("NEXT LEVEL", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    levelNum++;
                    initGame();
                }
            });
        } else {
            builder.setMessage("Outstanding cosmic logic! You completed all 10 Levels of Arrow Flow: Crash Course! 🎉");
            builder.setPositiveButton("MAIN MENU", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
        }

        builder.setNegativeButton("REPLAY", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                restartLevel();
            }
        });

        builder.setCancelable(false);
        builder.create().show();
    }

    private void showGameOverDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("💥 CRASH COLLISION!");
        builder.setMessage("Two arrows occupied the same space coordinate. Sector failed!\nTry again to find the correct exit order.");

        builder.setPositiveButton("RETRY", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                restartLevel();
            }
        });

        builder.setNegativeButton("EXIT", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });

        builder.setCancelable(false);
        builder.create().show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        gameView.stopAnimation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        gameView.startAnimation();
    }
}