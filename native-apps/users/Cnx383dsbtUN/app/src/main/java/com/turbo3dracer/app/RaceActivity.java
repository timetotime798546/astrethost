package com.turbo3dracer.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public class RaceActivity extends Activity implements GameView.GameCallback {

    private GameView gameView;
    private TextView tvHudSpeed;
    private TextView tvHudDistance;
    private TextView tvHudCoins;
    private TextView tvHudScore;
    private ProgressBar pbNitroBar;

    private LinearLayout layoutModal;
    private TextView tvModalTitle;
    private TextView tvModalDetails;
    private Button btnModalAction;
    private Button btnModalExit;

    private int finalScore = 0;
    private int earnedCoins = 0;
    private int distanceMeters = 0;
    private boolean isPaused = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_race);

        int carIndex = getIntent().getIntExtra("car_index", 0);
        int trackIndex = getIntent().getIntExtra("track_index", 0);

        CarModel[] fleet = CarModel.getDefaultFleet();
        CarModel chosenCar = fleet[Math.min(carIndex, fleet.length - 1)];

        initViews();
        gameView.setGameCallback(this);
        gameView.setupRace(chosenCar, trackIndex);
    }

    private void initViews() {
        gameView = (GameView) findViewById(R.id.game_view);
        tvHudSpeed = (TextView) findViewById(R.id.tv_hud_speed);
        tvHudDistance = (TextView) findViewById(R.id.tv_hud_distance);
        tvHudCoins = (TextView) findViewById(R.id.tv_hud_coins);
        tvHudScore = (TextView) findViewById(R.id.tv_hud_score);
        pbNitroBar = (ProgressBar) findViewById(R.id.pb_nitro_bar);

        layoutModal = (LinearLayout) findViewById(R.id.layout_modal);
        tvModalTitle = (TextView) findViewById(R.id.tv_modal_title);
        tvModalDetails = (TextView) findViewById(R.id.tv_modal_details);
        btnModalAction = (Button) findViewById(R.id.btn_modal_action);
        btnModalExit = (Button) findViewById(R.id.btn_modal_exit);

        Button btnSteerLeft = (Button) findViewById(R.id.btn_steer_left);
        Button btnSteerRight = (Button) findViewById(R.id.btn_steer_right);
        Button btnGas = (Button) findViewById(R.id.btn_gas);
        Button btnBrake = (Button) findViewById(R.id.btn_brake);
        Button btnNitro = (Button) findViewById(R.id.btn_nitro);
        Button btnPause = (Button) findViewById(R.id.btn_pause);

        // Steering touch bindings
        btnSteerLeft.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    gameView.setSteering(true, false);
                } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                    gameView.setSteering(false, false);
                }
                return true;
            }
        });

        btnSteerRight.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    gameView.setSteering(false, true);
                } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                    gameView.setSteering(false, false);
                }
                return true;
            }
        });

        // Gas touch
        btnGas.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    gameView.setAccelerating(true);
                } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                    gameView.setAccelerating(false);
                }
                return true;
            }
        });

        // Brake touch
        btnBrake.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    gameView.setBraking(true);
                } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                    gameView.setBraking(false);
                }
                return true;
            }
        });

        // Nitro touch
        btnNitro.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    gameView.setNitro(true);
                } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                    gameView.setNitro(false);
                }
                return true;
            }
        });

        // Pause button
        btnPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPauseDialog();
            }
        });

        btnModalAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPaused) {
                    hidePauseDialog();
                } else {
                    finish();
                }
            }
        });

        btnModalExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveAndFinish();
            }
        });
    }

    private void showPauseDialog() {
        isPaused = true;
        gameView.pauseGame();
        tvModalTitle.setText("RACE PAUSED");
        tvModalDetails.setText("Score: " + finalScore + "\nCredits Earned: " + earnedCoins);
        btnModalAction.setText("RESUME");
        layoutModal.setVisibility(View.VISIBLE);
    }

    private void hidePauseDialog() {
        isPaused = false;
        layoutModal.setVisibility(View.GONE);
        gameView.resumeGame();
    }

    private void saveAndFinish() {
        ScoreHelper.addCoins(this, earnedCoins);
        ScoreHelper.saveScoreIfRecord(this, finalScore);
        finish();
    }

    @Override
    public void onStatsUpdated(int speedKmh, int distanceMeters, int coins, int nitroPct, int score) {
        this.finalScore = score;
        this.earnedCoins = coins;
        this.distanceMeters = distanceMeters;

        tvHudSpeed.setText(speedKmh + " KM/H");
        tvHudDistance.setText(distanceMeters + " M");
        tvHudCoins.setText(coins + " CR");
        tvHudScore.setText(String.valueOf(score));
        pbNitroBar.setProgress(nitroPct);
    }

    @Override
    public void onGameOver(int finalScore, int coinsEarned, int distanceMeters) {
        boolean isRecord = ScoreHelper.saveScoreIfRecord(this, finalScore);
        ScoreHelper.addCoins(this, coinsEarned);

        tvModalTitle.setText(isRecord ? "NEW RECORD!" : "RACE FINISHED");
        tvModalDetails.setText("Score: " + finalScore + "\nDistance: " + distanceMeters + "m\nCredits: +" + coinsEarned);
        btnModalAction.setText("PLAY AGAIN");
        layoutModal.setVisibility(View.VISIBLE);
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
        if (gameView != null && !isPaused) {
            gameView.resumeGame();
        }
    }

    @Override
    public void onBackPressed() {
        if (!isPaused) {
            showPauseDialog();
        } else {
            saveAndFinish();
        }
    }
}