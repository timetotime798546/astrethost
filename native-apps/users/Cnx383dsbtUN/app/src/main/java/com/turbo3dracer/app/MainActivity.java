package com.turbo3dracer.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private CarModel[] fleet;
    private int currentCarIndex = 0;
    private int selectedTrack = 0;

    private TextView tvCarName;
    private View viewCarColorPreview;
    private TextView tvStatSpeed;
    private ProgressBar pbStatSpeed;
    private TextView tvStatAccel;
    private ProgressBar pbStatAccel;
    private TextView tvStatHandling;
    private ProgressBar pbStatHandling;
    private Button btnUnlockCar;
    private TextView tvCoinsDisplay;
    private TextView tvHighScore;
    private Button btnTrackCity;
    private Button btnTrackDesert;
    private Button btnTrackCyber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fleet = CarModel.getDefaultFleet();
        currentCarIndex = ScoreHelper.getSelectedCarIndex(this);
        selectedTrack = ScoreHelper.getSelectedTrack(this);

        initViews();
        refreshCarView();
        refreshCoinsAndRecords();
        updateTrackButtons();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshCoinsAndRecords();
    }

    private void initViews() {
        tvCarName = (TextView) findViewById(R.id.tv_car_name);
        viewCarColorPreview = findViewById(R.id.view_car_color_preview);
        tvStatSpeed = (TextView) findViewById(R.id.tv_stat_speed);
        pbStatSpeed = (ProgressBar) findViewById(R.id.pb_stat_speed);
        tvStatAccel = (TextView) findViewById(R.id.tv_stat_accel);
        pbStatAccel = (ProgressBar) findViewById(R.id.pb_stat_accel);
        tvStatHandling = (TextView) findViewById(R.id.tv_stat_handling);
        pbStatHandling = (ProgressBar) findViewById(R.id.pb_stat_handling);
        btnUnlockCar = (Button) findViewById(R.id.btn_unlock_car);
        tvCoinsDisplay = (TextView) findViewById(R.id.tv_coins_display);
        tvHighScore = (TextView) findViewById(R.id.tv_high_score);
        btnTrackCity = (Button) findViewById(R.id.btn_track_city);
        btnTrackDesert = (Button) findViewById(R.id.btn_track_desert);
        btnTrackCyber = (Button) findViewById(R.id.btn_track_cyber);

        Button btnPrevCar = (Button) findViewById(R.id.btn_prev_car);
        Button btnNextCar = (Button) findViewById(R.id.btn_next_car);
        Button btnStartRace = (Button) findViewById(R.id.btn_start_race);

        btnPrevCar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentCarIndex > 0) {
                    currentCarIndex--;
                } else {
                    currentCarIndex = fleet.length - 1;
                }
                refreshCarView();
            }
        });

        btnNextCar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentCarIndex < fleet.length - 1) {
                    currentCarIndex++;
                } else {
                    currentCarIndex = 0;
                }
                refreshCarView();
            }
        });

        btnUnlockCar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CarModel car = fleet[currentCarIndex];
                if (car.isUnlocked()) {
                    ScoreHelper.setSelectedCarIndex(MainActivity.this, currentCarIndex);
                    Toast.makeText(MainActivity.this, car.getName() + " Selected", Toast.LENGTH_SHORT).show();
                    refreshCarView();
                } else {
                    int coins = ScoreHelper.getCoins(MainActivity.this);
                    if (coins >= car.getUnlockCost()) {
                        ScoreHelper.addCoins(MainActivity.this, -car.getUnlockCost());
                        car.setUnlocked(true);
                        ScoreHelper.setSelectedCarIndex(MainActivity.this, currentCarIndex);
                        Toast.makeText(MainActivity.this, car.getName() + " Unlocked & Selected!", Toast.LENGTH_SHORT).show();
                        refreshCoinsAndRecords();
                        refreshCarView();
                    } else {
                        Toast.makeText(MainActivity.this, "Need " + (car.getUnlockCost() - coins) + " more credits!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        btnTrackCity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedTrack = 0;
                ScoreHelper.setSelectedTrack(MainActivity.this, 0);
                updateTrackButtons();
            }
        });

        btnTrackDesert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedTrack = 1;
                ScoreHelper.setSelectedTrack(MainActivity.this, 1);
                updateTrackButtons();
            }
        });

        btnTrackCyber.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedTrack = 2;
                ScoreHelper.setSelectedTrack(MainActivity.this, 2);
                updateTrackButtons();
            }
        });

        btnStartRace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CarModel selectedCar = fleet[currentCarIndex];
                if (!selectedCar.isUnlocked()) {
                    Toast.makeText(MainActivity.this, "Unlock vehicle first!", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent intent = new Intent(MainActivity.this, RaceActivity.class);
                intent.putExtra("car_index", currentCarIndex);
                intent.putExtra("track_index", selectedTrack);
                startActivity(intent);
            }
        });
    }

    private void refreshCarView() {
        CarModel car = fleet[currentCarIndex];
        tvCarName.setText(car.getName());
        viewCarColorPreview.setBackgroundColor(car.getPrimaryColor());

        tvStatSpeed.setText("TOP SPEED: " + car.getTopSpeed() + " KM/H");
        pbStatSpeed.setProgress(car.getTopSpeed());

        tvStatAccel.setText("ACCELERATION: " + car.getAcceleration() + "/10");
        pbStatAccel.setProgress(car.getAcceleration());

        tvStatHandling.setText("HANDLING: " + car.getHandling() + "/10");
        pbStatHandling.setProgress(car.getHandling());

        int savedCar = ScoreHelper.getSelectedCarIndex(this);
        if (car.isUnlocked()) {
            if (savedCar == currentCarIndex) {
                btnUnlockCar.setText("READY FOR RACE");
                btnUnlockCar.setBackgroundColor(Color.rgb(0, 230, 118));
            } else {
                btnUnlockCar.setText("EQUIP VEHICLE");
                btnUnlockCar.setBackgroundColor(Color.rgb(0, 229, 255));
            }
        } else {
            btnUnlockCar.setText("UNLOCK FOR " + car.getUnlockCost() + " CR");
            btnUnlockCar.setBackgroundColor(Color.rgb(255, 171, 0));
        }
    }

    private void refreshCoinsAndRecords() {
        int coins = ScoreHelper.getCoins(this);
        int highScore = ScoreHelper.getHighScore(this);
        tvCoinsDisplay.setText(coins + " CR");
        tvHighScore.setText(highScore + " PTS");
    }

    private void updateTrackButtons() {
        int activeColor = Color.rgb(0, 229, 255);
        int inactiveColor = Color.rgb(26, 29, 46);
        int activeText = Color.rgb(15, 17, 26);
        int inactiveText = Color.rgb(255, 255, 255);

        btnTrackCity.setBackgroundColor(selectedTrack == 0 ? activeColor : inactiveColor);
        btnTrackCity.setTextColor(selectedTrack == 0 ? activeText : inactiveText);

        btnTrackDesert.setBackgroundColor(selectedTrack == 1 ? activeColor : inactiveColor);
        btnTrackDesert.setTextColor(selectedTrack == 1 ? activeText : inactiveText);

        btnTrackCyber.setBackgroundColor(selectedTrack == 2 ? activeColor : inactiveColor);
        btnTrackCyber.setTextColor(selectedTrack == 2 ? activeText : inactiveText);
    }
}