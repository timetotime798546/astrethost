package com.minitoolbox.app;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends Activity {

    // Dynamic UI Container Views
    private LinearLayout layoutDashboard;
    private LinearLayout layoutTipCalc;
    private LinearLayout layoutUnitConv;
    private LinearLayout layoutDice;
    private LinearLayout layoutStopwatch;

    // Stopwatch Thread Management
    private Handler stopwatchHandler;
    private long startTime = 0L;
    private long timeInMilliseconds = 0L;
    private long timeSwapBuff = 0L;
    private long updatedTime = 0L;
    private boolean isRunning = false;
    private ArrayList<String> lapsList;

    private Runnable updateTimerThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind main screen switch containers
        layoutDashboard = (LinearLayout) findViewById(R.id.layout_dashboard);
        layoutTipCalc = (LinearLayout) findViewById(R.id.layout_tip_calc);
        layoutUnitConv = (LinearLayout) findViewById(R.id.layout_unit_conv);
        layoutDice = (LinearLayout) findViewById(R.id.layout_dice_roller);
        layoutStopwatch = (LinearLayout) findViewById(R.id.layout_stopwatch);

        // Bind dashboard button-cards
        LinearLayout cardTip = (LinearLayout) findViewById(R.id.card_tip_calc);
        LinearLayout cardUnit = (LinearLayout) findViewById(R.id.card_unit_conv);
        LinearLayout cardDice = (LinearLayout) findViewById(R.id.card_dice_roller);
        LinearLayout cardStopwatch = (LinearLayout) findViewById(R.id.card_stopwatch);

        // Screen switch actions
        cardTip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showScreen(layoutTipCalc);
            }
        });

        cardUnit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showScreen(layoutUnitConv);
            }
        });

        cardDice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showScreen(layoutDice);
            }
        });

        cardStopwatch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showScreen(layoutStopwatch);
            }
        });

        // Set up individual back buttons
        setupBackNavigation();

        // Initialize features
        initTipCalculator();
        initUnitConverter();
        initDiceRoller();
        initStopwatch();
    }

    private void showScreen(View screenToShow) {
        layoutDashboard.setVisibility(View.GONE);
        layoutTipCalc.setVisibility(View.GONE);
        layoutUnitConv.setVisibility(View.GONE);
        layoutDice.setVisibility(View.GONE);
        layoutStopwatch.setVisibility(View.GONE);

        screenToShow.setVisibility(View.VISIBLE);
    }

    private void setupBackNavigation() {
        Button btnBackTip = (Button) findViewById(R.id.btn_back_tip);
        btnBackTip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showScreen(layoutDashboard);
            }
        });

        Button btnBackUnit = (Button) findViewById(R.id.btn_back_unit);
        btnBackUnit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showScreen(layoutDashboard);
            }
        });

        Button btnBackDice = (Button) findViewById(R.id.btn_back_dice);
        btnBackDice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showScreen(layoutDashboard);
            }
        });

        Button btnBackStopwatch = (Button) findViewById(R.id.btn_back_stopwatch);
        btnBackStopwatch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showScreen(layoutDashboard);
            }
        });
    }

    // 1. Tip Calculator Feature
    private void initTipCalculator() {
        final EditText etBill = (EditText) findViewById(R.id.et_bill);
        final EditText etTipPercent = (EditText) findViewById(R.id.et_tip_percent);
        final EditText etPeople = (EditText) findViewById(R.id.et_people);
        Button btnCalcTip = (Button) findViewById(R.id.btn_calc_tip);
        final TextView tvTipResult = (TextView) findViewById(R.id.tv_tip_result);

        btnCalcTip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String billStr = etBill.getText().toString().trim();
                String tipStr = etTipPercent.getText().toString().trim();
                String peopleStr = etPeople.getText().toString().trim();

                if (billStr.isEmpty()) {
                    etBill.setError("Enter bill amount");
                    return;
                }

                double bill = Double.parseDouble(billStr);
                double tipPercent = 15.0; // default value
                if (!tipStr.isEmpty()) {
                    tipPercent = Double.parseDouble(tipStr);
                }

                int people = 1;
                if (!peopleStr.isEmpty()) {
                    people = Integer.parseInt(peopleStr);
                    if (people <= 0) {
                        people = 1;
                    }
                }

                double totalTip = bill * (tipPercent / 100.0);
                double totalBill = bill + totalTip;

                double tipPerPerson = totalTip / people;
                double totalPerPerson = totalBill / people;

                String formattedOutput = String.format(Locale.US,
                        "Total Tip: $%.2f\nTip per Person: $%.2f\n\nTotal Bill: $%.2f\nTotal per Person: $%.2f",
                        totalTip, tipPerPerson, totalBill, totalPerPerson);

                tvTipResult.setText(formattedOutput);
            }
        });
    }

    // 2. Unit Converter Feature
    private void initUnitConverter() {
        final EditText etUnitInput = (EditText) findViewById(R.id.et_unit_input);
        final RadioGroup rgConversion = (RadioGroup) findViewById(R.id.rg_conversion);
        Button btnConvert = (Button) findViewById(R.id.btn_convert);
        final TextView tvConvertResult = (TextView) findViewById(R.id.tv_convert_result);

        btnConvert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String inputStr = etUnitInput.getText().toString().trim();
                if (inputStr.isEmpty()) {
                    etUnitInput.setError("Enter a value");
                    return;
                }

                double inputVal = Double.parseDouble(inputStr);
                int selectedRadioId = rgConversion.getCheckedRadioButtonId();

                double resultVal = 0.0;
                String fromUnit = "";
                String toUnit = "";

                if (selectedRadioId == R.id.rb_m_to_ft) {
                    resultVal = inputVal * 3.28084;
                    fromUnit = "m";
                    toUnit = "ft";
                } else if (selectedRadioId == R.id.rb_ft_to_m) {
                    resultVal = inputVal / 3.28084;
                    fromUnit = "ft";
                    toUnit = "m";
                } else if (selectedRadioId == R.id.rb_kg_to_lbs) {
                    resultVal = inputVal * 2.20462;
                    fromUnit = "kg";
                    toUnit = "lbs";
                } else if (selectedRadioId == R.id.rb_lbs_to_kg) {
                    resultVal = inputVal / 2.20462;
                    fromUnit = "lbs";
                    toUnit = "kg";
                } else if (selectedRadioId == R.id.rb_c_to_f) {
                    resultVal = (inputVal * 9.0 / 5.0) + 32;
                    fromUnit = "°C";
                    toUnit = "°F";
                } else if (selectedRadioId == R.id.rb_f_to_c) {
                    resultVal = (inputVal - 32) * 5.0 / 9.0;
                    fromUnit = "°F";
                    toUnit = "°C";
                } else {
                    tvConvertResult.setText("Please select a conversion type.");
                    return;
                }

                tvConvertResult.setText(String.format(Locale.US, "%.2f %s = %.2f %s", inputVal, fromUnit, resultVal, toUnit));
            }
        });
    }

    // 3. Dice Roller Feature
    private void initDiceRoller() {
        final TextView tvDiceValue = (TextView) findViewById(R.id.tv_dice_value);
        final TextView tvDiceHistory = (TextView) findViewById(R.id.tv_dice_history);
        Button btnRollDice = (Button) findViewById(R.id.btn_roll_dice);
        Button btnClearHistory = (Button) findViewById(R.id.btn_clear_dice_history);

        final ArrayList<Integer> rollHistory = new ArrayList<Integer>();

        btnRollDice.setOnClickListener(new View.OnClickListener() {
            private final Random rand = new Random();

            @Override
            public void onClick(View v) {
                int rolledValue = rand.nextInt(6) + 1;
                String face = "";
                switch (rolledValue) {
                    case 1: face = "⚀"; break;
                    case 2: face = "⚁"; break;
                    case 3: face = "⚂"; break;
                    case 4: face = "⚃"; break;
                    case 5: face = "⚄"; break;
                    case 6: face = "⚅"; break;
                }
                tvDiceValue.setText(face + "\n" + rolledValue);

                rollHistory.add(0, rolledValue);

                StringBuilder historyBuilder = new StringBuilder();
                historyBuilder.append("Recent rolls:\n");
                int limit = Math.min(rollHistory.size(), 10);
                for (int i = 0; i < limit; i++) {
                    historyBuilder.append(rollHistory.get(i));
                    if (i < limit - 1) {
                        historyBuilder.append(", ");
                    }
                }
                tvDiceHistory.setText(historyBuilder.toString());
            }
        });

        btnClearHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rollHistory.clear();
                tvDiceHistory.setText("Recent rolls:\nNone");
                tvDiceValue.setText("🎲\nRoll!");
            }
        });
    }

    // 4. Stopwatch Feature
    private void initStopwatch() {
        final TextView tvStopwatchTime = (TextView) findViewById(R.id.tv_stopwatch_time);
        Button btnStart = (Button) findViewById(R.id.btn_stopwatch_start);
        Button btnPause = (Button) findViewById(R.id.btn_stopwatch_pause);
        Button btnReset = (Button) findViewById(R.id.btn_stopwatch_reset);
        Button btnLap = (Button) findViewById(R.id.btn_stopwatch_lap);
        final TextView tvStopwatchLaps = (TextView) findViewById(R.id.tv_stopwatch_laps);

        stopwatchHandler = new Handler();
        lapsList = new ArrayList<String>();

        updateTimerThread = new Runnable() {
            @Override
            public void run() {
                if (!isRunning) return;
                timeInMilliseconds = SystemClock.uptimeMillis() - startTime;
                updatedTime = timeSwapBuff + timeInMilliseconds;

                int secs = (int) (updatedTime / 1000);
                int mins = secs / 60;
                secs = secs % 60;
                int milliseconds = (int) (updatedTime % 1000) / 100;

                tvStopwatchTime.setText(String.format(Locale.US, "%02d:%02d.%01d", mins, secs, milliseconds));
                stopwatchHandler.postDelayed(this, 100);
            }
        };

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isRunning) {
                    startTime = SystemClock.uptimeMillis();
                    isRunning = true;
                    stopwatchHandler.postDelayed(updateTimerThread, 0);
                }
            }
        });

        btnPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isRunning) {
                    timeSwapBuff += timeInMilliseconds;
                    isRunning = false;
                    stopwatchHandler.removeCallbacks(updateTimerThread);
                }
            }
        });

        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isRunning = false;
                stopwatchHandler.removeCallbacks(updateTimerThread);
                startTime = 0L;
                timeInMilliseconds = 0L;
                timeSwapBuff = 0L;
                updatedTime = 0L;
                tvStopwatchTime.setText("00:00.0");
                lapsList.clear();
                tvStopwatchLaps.setText("Laps:\nNone");
            }
        });

        btnLap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isRunning || updatedTime > 0) {
                    int secs = (int) (updatedTime / 1000);
                    int mins = secs / 60;
                    secs = secs % 60;
                    int milliseconds = (int) (updatedTime % 1000) / 100;

                    String currentLapTime = String.format(Locale.US, "%02d:%02d.%01d", mins, secs, milliseconds);
                    lapsList.add(0, "Lap " + (lapsList.size() + 1) + ": " + currentLapTime);

                    StringBuilder lapsText = new StringBuilder();
                    lapsText.append("Laps:\n");
                    for (int i = 0; i < lapsList.size(); i++) {
                        lapsText.append(lapsList.get(i)).append("\n");
                    }
                    tvStopwatchLaps.setText(lapsText.toString());
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (stopwatchHandler != null && updateTimerThread != null) {
            stopwatchHandler.removeCallbacks(updateTimerThread);
        }
    }
}