package com.roadcrossing.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    private GameView gameView;
    private LinearLayout menuOverlay;
    private LinearLayout howToPlayOverlay;
    private LinearLayout gameOverOverlay;
    private RelativeLayout hudOverlay;

    private TextView menuBestScoreText;
    private TextView gameOverScoreText;
    private TextView gameOverBestScoreText;
    private TextView hudScoreText;
    private TextView hudBestText;

    private SharedPreferences prefs;
    private GestureDetector gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Configure Fullscreen layout elements
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("road_crossing_prefs", Context.MODE_PRIVATE);

        gameView = (GameView) findViewById(R.id.game_view);
        menuOverlay = (LinearLayout) findViewById(R.id.menu_overlay);
        howToPlayOverlay = (LinearLayout) findViewById(R.id.how_to_play_overlay);
        gameOverOverlay = (LinearLayout) findViewById(R.id.game_over_overlay);
        hudOverlay = (RelativeLayout) findViewById(R.id.hud_overlay);

        menuBestScoreText = (TextView) findViewById(R.id.menu_best_score);
        gameOverScoreText = (TextView) findViewById(R.id.game_over_score);
        gameOverBestScoreText = (TextView) findViewById(R.id.game_over_best_score);
        hudScoreText = (TextView) findViewById(R.id.hud_score);
        hudBestText = (TextView) findViewById(R.id.hud_best);

        Button btnPlay = (Button) findViewById(R.id.btn_play);
        Button btnHowToPlay = (Button) findViewById(R.id.btn_how_to_play);
        Button btnQuit = (Button) findViewById(R.id.btn_quit);
        Button btnHowToBack = (Button) findViewById(R.id.btn_how_to_play_back);
        Button btnRetry = (Button) findViewById(R.id.btn_retry);
        Button btnHome = (Button) findViewById(R.id.btn_home);

        Button btnUp = (Button) findViewById(R.id.btn_up);
        Button btnDown = (Button) findViewById(R.id.btn_down);
        Button btnLeft = (Button) findViewById(R.id.btn_left);
        Button btnRight = (Button) findViewById(R.id.btn_right);

        gameView.setMainActivity(this);
        updateHighScoresDisplay();

        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startGame();
            }
        });

        btnHowToPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                menuOverlay.setVisibility(View.GONE);
                howToPlayOverlay.setVisibility(View.VISIBLE);
            }
        });

        btnHowToBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                howToPlayOverlay.setVisibility(View.GONE);
                menuOverlay.setVisibility(View.VISIBLE);
            }
        });

        btnQuit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startGame();
            }
        });

        btnHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMenu();
            }
        });

        // Direction controller clicks
        btnUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gameView.movePlayerUp();
            }
        });

        btnDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gameView.movePlayerDown();
            }
        });

        btnLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gameView.movePlayerLeft();
            }
        });

        btnRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gameView.movePlayerRight();
            }
        });

        // Swipe detector configuration
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            private static final int SWIPE_THRESHOLD = 60;
            private static final int SWIPE_VELOCITY_THRESHOLD = 60;

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || e2 == null) return false;
                float dY = e2.getY() - e1.getY();
                float dX = e2.getX() - e1.getX();

                if (Math.abs(dX) > Math.abs(dY)) {
                    if (Math.abs(dX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (dX > 0) {
                            gameView.movePlayerRight();
                        } else {
                            gameView.movePlayerLeft();
                        }
                        return true;
                    }
                } else {
                    if (Math.abs(dY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                        if (dY > 0) {
                            gameView.movePlayerDown();
                        } else {
                            gameView.movePlayerUp();
                        }
                        return true;
                    }
                }
                return false;
            }
        });

        findViewById(R.id.root_layout).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return gestureDetector.onTouchEvent(event);
            }
        });
    }

    private void updateHighScoresDisplay() {
        int best = prefs.getInt("best_score", 0) * 10;
        menuBestScoreText.setText("Best Score: " + best);
        hudBestText.setText("BEST: " + best);
    }

    public void startGame() {
        menuOverlay.setVisibility(View.GONE);
        howToPlayOverlay.setVisibility(View.GONE);
        gameOverOverlay.setVisibility(View.GONE);
        hudOverlay.setVisibility(View.VISIBLE);

        gameView.initGame();
        gameView.resumeGame();
    }

    public void showMenu() {
        gameView.pauseGame();
        gameOverOverlay.setVisibility(View.GONE);
        howToPlayOverlay.setVisibility(View.GONE);
        hudOverlay.setVisibility(View.GONE);
        menuOverlay.setVisibility(View.VISIBLE);

        updateHighScoresDisplay();
    }

    public void updateHud(int score) {
        final int finalScore = score * 10;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                hudScoreText.setText("SCORE: " + finalScore);
            }
        });
    }

    public void onGameOver(final int score) {
        final int finalScore = score * 10;
        int best = prefs.getInt("best_score", 0);
        if (score > best) {
            best = score;
            prefs.edit().putInt("best_score", score).apply();
        }
        final int finalBest = best * 10;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                hudOverlay.setVisibility(View.GONE);
                gameOverScoreText.setText("Score: " + finalScore);
                gameOverBestScoreText.setText("Best Score: " + finalBest);
                gameOverOverlay.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (gameView != null) {
            gameView.pauseGame();
        }
    }
}