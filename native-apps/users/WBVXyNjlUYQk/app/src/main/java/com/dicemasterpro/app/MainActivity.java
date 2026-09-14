package com.dicemasterpro.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends Activity implements SensorEventListener {

    // Preferences Keys
    private static final String PREFS_NAME = "DiceMasterProPrefs";
    private static final String KEY_SOUND = "SoundEnabled";
    private static final String KEY_SHAKE_ENABLED = "ShakeToRollEnabled";
    private static final String KEY_SHAKE_SENSITIVITY = "ShakeSensitivityVal";
    private static final String KEY_TOTAL_ROLLS = "LifetimeRollCount";
    private static final String KEY_HIGH_SCORE = "LifetimeHighScore";
    private static final String KEY_THEME = "ActiveBoardColor";
    private static final String KEY_FREQ_1 = "FreqCount1";
    private static final String KEY_FREQ_2 = "FreqCount2";
    private static final String KEY_FREQ_3 = "FreqCount3";
    private static final String KEY_FREQ_4 = "FreqCount4";
    private static final String KEY_FREQ_5 = "FreqCount5";
    private static final String KEY_FREQ_6 = "FreqCount6";

    // Layout Containers & Nav Tabs
    private LinearLayout rootContainer;
    private Button tabSolo, tabDuel, tabStats;
    private LinearLayout layoutSolo, layoutDuel, layoutStatsView;

    // Solo Mode Widgets
    private Spinner spinnerDiceCount;
    private RelativeLayout die1, die2, die3, die4, die5;
    private TextView tvRollSum, tvSoloHistory, sensorStatusText;
    private Button btnRollSolo, btnToggleSound;

    // Duel Mode Widgets
    private Button btnModeBattle, btnModePig, btnDuelRoll, btnDuelHold;
    private RelativeLayout duelDie;
    private TextView tvGameModeTitle, tvTurnIndicator, tvP1Score, tvP2Score, tvTurnPoints, tvDuelLog;

    // Stats / Settings Widgets
    private TextView statTotalRolls, statHighestRoll, statRollFrequency, tvShakeSensitivity;
    private CheckBox cbShakeToRoll;
    private SeekBar sbShakeSensitivity;
    private Button btnResetStats;
    private Button themeCasinoGreen, themeIndigo, themeCharcoal;

    // Die visual dots cache
    private View[][] soloDiceDots = new View[5][9];
    private View[] duelDieDots = new View[9];

    // Accelerometer Sensors variables
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private long lastShakeTime = 0;
    private boolean isShakeToRollEnabled = true;
    private float shakeThreshold = 16.0f; // Standard default

    // Game states
    private int selectedDiceCount = 3;
    private Random random = new Random();
    private boolean isRollingNow = false;
    private SharedPreferences prefs;

    // Duel Game Mode: 0 = High Roll Battle, 1 = Classic Pig Jeopardy Game
    private int activeDuelMode = 0; 
    private int p1TotalScore = 0;
    private int p2TotalScore = 0;
    private int pigTurnAccumulatedScore = 0;
    private int duelCurrentTurnPlayer = 1; // 1 = Player 1, 2 = Player 2
    private int duelRoundCount = 0; // Simple Battle rolls tracker

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        initViewBindings();
        loadSavedConfiguration();
        setupNavigationTabs();
        setupSoloDiceSelectionSpinner();
        setupSoundToggleControl();
        setupGameRollTriggers();
        setupSettingsAndStyleThemes();
        setupMotionSensorFramework();

        // Show Solo tab by default
        switchActiveTab(1);
    }

    private void initViewBindings() {
        // Core Layout views
        rootContainer = findViewById(R.id.root_container);
        sensorStatusText = findViewById(R.id.sensor_status);
        btnToggleSound = findViewById(R.id.btn_toggle_sound);

        // Tab views
        tabSolo = findViewById(R.id.tab_solo);
        tabDuel = findViewById(R.id.tab_duel);
        tabStats = findViewById(R.id.tab_stats);

        layoutSolo = findViewById(R.id.layout_solo);
        layoutDuel = findViewById(R.id.layout_duel);
        layoutStatsView = findViewById(R.id.layout_stats);

        // Solo configuration
        spinnerDiceCount = findViewById(R.id.spinner_dice_count);
        tvRollSum = findViewById(R.id.tv_roll_sum);
        tvSoloHistory = findViewById(R.id.tv_solo_history);
        btnRollSolo = findViewById(R.id.btn_roll_solo);

        // Individual visual Solo Dice layouts mapping
        die1 = findViewById(R.id.die_1);
        die2 = findViewById(R.id.die_2);
        die3 = findViewById(R.id.die_3);
        die4 = findViewById(R.id.die_4);
        die5 = findViewById(R.id.die_5);

        // Map visual dots array mapping for programmatic toggle values
        bindDotsToArray();

        // Duel Mode Views bindings
        btnModeBattle = findViewById(R.id.btn_mode_battle);
        btnModePig = findViewById(R.id.btn_mode_pig);
        btnDuelRoll = findViewById(R.id.btn_duel_roll);
        btnDuelHold = findViewById(R.id.btn_duel_hold);
        duelDie = findViewById(R.id.duel_die);
        tvGameModeTitle = findViewById(R.id.tv_game_mode_title);
        tvTurnIndicator = findViewById(R.id.tv_turn_indicator);
        tvP1Score = findViewById(R.id.tv_p1_score);
        tvP2Score = findViewById(R.id.tv_p2_score);
        tvTurnPoints = findViewById(R.id.tv_turn_points);
        tvDuelLog = findViewById(R.id.tv_duel_log);

        // Map duel dots into simple array
        for (int d = 1; d <= 9; d++) {
            int dotId = getResources().getIdentifier("duel_die_dot_" + d, "id", getPackageName());
            duelDieDots[d - 1] = findViewById(dotId);
        }

        // Settings statistics layouts
        statTotalRolls = findViewById(R.id.stat_total_rolls);
        statHighestRoll = findViewById(R.id.stat_highest_roll);
        statRollFrequency = findViewById(R.id.stat_roll_frequency);
        tvShakeSensitivity = findViewById(R.id.tv_shake_sensitivity);
        cbShakeToRoll = findViewById(R.id.cb_shake_to_roll);
        sbShakeSensitivity = findViewById(R.id.sb_shake_sensitivity);
        btnResetStats = findViewById(R.id.btn_reset_stats);

        // Theme colors elements
        themeCasinoGreen = findViewById(R.id.theme_casino_green);
        themeIndigo = findViewById(R.id.theme_indigo);
        themeCharcoal = findViewById(R.id.theme_charcoal);
    }

    private void bindDotsToArray() {
        for (int dieIdx = 1; dieIdx <= 5; dieIdx++) {
            for (int dotIdx = 1; dotIdx <= 9; dotIdx++) {
                int dotId = getResources().getIdentifier("die_" + dieIdx + "_dot_" + dotIdx, "id", getPackageName());
                soloDiceDots[dieIdx - 1][dotIdx - 1] = findViewById(dotId);
            }
        }
    }

    private void loadSavedConfiguration() {
        // Sound config loaded
        boolean soundOn = prefs.getBoolean(KEY_SOUND, true);
        SoundGenerator.setSoundEnabled(soundOn);
        btnToggleSound.setText(soundOn ? "🔊" : "🔇");

        // Sensor config loaded
        isShakeToRollEnabled = prefs.getBoolean(KEY_SHAKE_ENABLED, true);
        cbShakeToRoll.setChecked(isShakeToRollEnabled);
        sensorStatusText.setText(isShakeToRollEnabled ? "Shake to Roll: Enabled" : "Shake to Roll: Disabled");

        int sensProgress = prefs.getInt(KEY_SHAKE_SENSITIVITY, 10);
        sbShakeSensitivity.setProgress(sensProgress);
        calculateShakeThreshold(sensProgress);

        // Core visual theme application
        String savedTheme = prefs.getString(KEY_THEME, "GREEN");
        applyThemeColor(savedTheme);

        refreshStatsDashboard();
    }

    private void calculateShakeThreshold(int progress) {
        // progress maps from 0 (very sensitive, low g threshold) to 20 (hard shake, high g threshold)
        // threshold scales from 11m/s2 to 31m/s2
        shakeThreshold = 11.0f + (progress * 1.0f);
        String level = "Medium";
        if (progress < 7) level = "High Sensitive";
        else if (progress > 14) level = "Firm Shake Required";
        tvShakeSensitivity.setText("Shake Sensitivity Threshold: " + level + " (" + (int)shakeThreshold + ")");
    }

    private void refreshStatsDashboard() {
        int rollsCount = prefs.getInt(KEY_TOTAL_ROLLS, 0);
        int highScoreVal = prefs.getInt(KEY_HIGH_SCORE, 0);
        
        statTotalRolls.setText("Total Lifetime Rolls: " + rollsCount);
        statHighestRoll.setText("Highest Sum Rolled: " + highScoreVal);

        int f1 = prefs.getInt(KEY_FREQ_1, 0);
        int f2 = prefs.getInt(KEY_FREQ_2, 0);
        int f3 = prefs.getInt(KEY_FREQ_3, 0);
        int f4 = prefs.getInt(KEY_FREQ_4, 0);
        int f5 = prefs.getInt(KEY_FREQ_5, 0);
        int f6 = prefs.getInt(KEY_FREQ_6, 0);

        statRollFrequency.setText("1s: " + f1 + "  |  2s: " + f2 + "  |  3s: " + f3 + "\n4s: " + f4 + "  |  5s: " + f5 + "  |  6s: " + f6);
    }

    private void setupNavigationTabs() {
        tabSolo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchActiveTab(1);
            }
        });

        tabDuel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchActiveTab(2);
            }
        });

        tabStats.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchActiveTab(3);
                refreshStatsDashboard();
            }
        });
    }

    private void switchActiveTab(int tabIndex) {
        tabSolo.setSelected(tabIndex == 1);
        tabDuel.setSelected(tabIndex == 2);
        tabStats.setSelected(tabIndex == 3);

        layoutSolo.setVisibility(tabIndex == 1 ? View.VISIBLE : View.GONE);
        layoutDuel.setVisibility(tabIndex == 2 ? View.VISIBLE : View.GONE);
        layoutStatsView.setVisibility(tabIndex == 3 ? View.VISIBLE : View.GONE);
    }

    private void setupSoloDiceSelectionSpinner() {
        List<String> options = new ArrayList<String>();
        options.add("1 Die");
        options.add("2 Dice");
        options.add("3 Dice");
        options.add("4 Dice");
        options.add("5 Dice");

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDiceCount.setAdapter(adapter);
        spinnerDiceCount.setSelection(2); // default 3 dice

        spinnerDiceCount.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedDiceCount = position + 1;
                updateSoloDiceVisibility();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void updateSoloDiceVisibility() {
        die1.setVisibility(selectedDiceCount >= 1 ? View.VISIBLE : View.GONE);
        die2.setVisibility(selectedDiceCount >= 2 ? View.VISIBLE : View.GONE);
        die3.setVisibility(selectedDiceCount >= 3 ? View.VISIBLE : View.GONE);
        die4.setVisibility(selectedDiceCount >= 4 ? View.VISIBLE : View.GONE);
        die5.setVisibility(selectedDiceCount >= 5 ? View.VISIBLE : View.GONE);
    }

    private void setupSoundToggleControl() {
        btnToggleSound.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean state = !SoundGenerator.isSoundEnabled();
                SoundGenerator.setSoundEnabled(state);
                btnToggleSound.setText(state ? "🔊" : "🔇");
                prefs.edit().putBoolean(KEY_SOUND, state).apply();
                
                if (state) {
                    SoundGenerator.playWinSound();
                }
            }
        });
    }

    private void setupGameRollTriggers() {
        btnRollSolo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerSoloDiceRoll();
            }
        });

        // Duel configurations
        btnModeBattle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeDuelGameMode(0);
            }
        });

        btnModePig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeDuelGameMode(1);
            }
        });

        btnDuelRoll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerDuelDiceRoll();
            }
        });

        btnDuelHold.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handlePigHoldPoints();
            }
        });
    }

    private void setupSettingsAndStyleThemes() {
        cbShakeToRoll.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isShakeToRollEnabled = isChecked;
                sensorStatusText.setText(isChecked ? "Shake to Roll: Enabled" : "Shake to Roll: Disabled");
                prefs.edit().putBoolean(KEY_SHAKE_ENABLED, isChecked).apply();
            }
        });

        sbShakeSensitivity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                calculateShakeThreshold(progress);
                prefs.edit().putInt(KEY_SHAKE_SENSITIVITY, progress).apply();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnResetStats.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prefs.edit()
                    .putInt(KEY_TOTAL_ROLLS, 0)
                    .putInt(KEY_HIGH_SCORE, 0)
                    .putInt(KEY_FREQ_1, 0)
                    .putInt(KEY_FREQ_2, 0)
                    .putInt(KEY_FREQ_3, 0)
                    .putInt(KEY_FREQ_4, 0)
                    .putInt(KEY_FREQ_5, 0)
                    .putInt(KEY_FREQ_6, 0)
                    .apply();
                
                Toast.makeText(MainActivity.this, "Statistics reset completely!", Toast.LENGTH_SHORT).show();
                refreshStatsDashboard();
            }
        });

        // Theme colors triggering
        themeCasinoGreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyThemeColor("GREEN");
            }
        });

        themeIndigo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyThemeColor("INDIGO");
            }
        });

        themeCharcoal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyThemeColor("CHARCOAL");
            }
        });
    }

    private void applyThemeColor(String theme) {
        prefs.edit().putString(KEY_THEME, theme).apply();
        if ("GREEN".equals(theme)) {
            rootContainer.setBackgroundColor(0xFF0F5132); // Deep casino green
        } else if ("INDIGO".equals(theme)) {
            rootContainer.setBackgroundColor(0xFF1A237E); // Rich Navy Indigo
        } else if ("CHARCOAL".equals(theme)) {
            rootContainer.setBackgroundColor(0xFF263238); // Material Charcoal
        }
    }

    private void setupMotionSensorFramework() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
    }

    // Dynamic Physics/Animation Roll Logic for Solo Mode
    private void triggerSoloDiceRoll() {
        if (isRollingNow) return;
        isRollingNow = true;

        SoundGenerator.playRollSound();

        // High visual-fidelity animation cycles
        final int animationFrames = 10;
        final int frameDurationMs = 50;
        final Handler handler = new Handler();

        Runnable animationRunnable = new Runnable() {
            int frame = 0;

            @Override
            public void run() {
                // Cycle temp random dots inside the active visual layouts
                for (int i = 0; i < selectedDiceCount; i++) {
                    int randomVal = random.nextInt(6) + 1;
                    renderVisualDots(soloDiceDots[i], randomVal);
                }

                frame++;
                if (frame < animationFrames) {
                    handler.postDelayed(this, frameDurationMs);
                } else {
                    // Animation finished, execute terminal calculation state
                    completeSoloRoll();
                }
            }
        };

        handler.post(animationRunnable);
    }

    private void completeSoloRoll() {
        int totalSum = 0;
        int[] rolledValues = new int[selectedDiceCount];
        StringBuilder rollLogDetails = new StringBuilder();

        for (int i = 0; i < selectedDiceCount; i++) {
            int finalValue = random.nextInt(6) + 1;
            rolledValues[i] = finalValue;
            totalSum += finalValue;
            renderVisualDots(soloDiceDots[i], finalValue);
            
            // Record lifetime metrics
            incrementLifetimeRollStats(finalValue);

            if (i > 0) rollLogDetails.append(" + ");
            rollLogDetails.append(finalValue);
        }

        tvRollSum.setText("Total Score: " + totalSum);
        
        // Push and display roll history feed
        String updatedHistory = rollLogDetails.toString() + " = (Sum: " + totalSum + ")\n" + tvSoloHistory.getText().toString();
        if (updatedHistory.length() > 200) {
            updatedHistory = updatedHistory.substring(0, 190) + "...";
        }
        tvSoloHistory.setText(updatedHistory);

        // Update top-level max score records
        int prevHigh = prefs.getInt(KEY_HIGH_SCORE, 0);
        int totalLifetimeRollsCount = prefs.getInt(KEY_TOTAL_ROLLS, 0) + 1;
        
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_TOTAL_ROLLS, totalLifetimeRollsCount);

        if (totalSum > prevHigh) {
            editor.putInt(KEY_HIGH_SCORE, totalSum);
            Toast.makeText(this, "🎉 NEW CLASSIC RECORD: " + totalSum + "!", Toast.LENGTH_SHORT).show();
            SoundGenerator.playWinSound();
        }
        editor.apply();

        isRollingNow = false;
    }

    private void incrementLifetimeRollStats(int val) {
        String key = "FreqCount" + val;
        int currentCount = prefs.getInt(key, 0);
        prefs.edit().putInt(key, currentCount + 1).apply();
    }

    // Dynamic visual dot positioning and rendering algorithm
    private void renderVisualDots(View[] targetDieDots, int val) {
        // Clear all elements first
        for (View dot : targetDieDots) {
            if (dot != null) {
                dot.setVisibility(View.INVISIBLE);
            }
        }

        // Apply standard physical matrix layout configurations
        switch (val) {
            case 1:
                targetDieDots[4].setVisibility(View.VISIBLE); // Center dot
                break;
            case 2:
                targetDieDots[0].setVisibility(View.VISIBLE); // Top Left
                targetDieDots[8].setVisibility(View.VISIBLE); // Bottom Right
                break;
            case 3:
                targetDieDots[0].setVisibility(View.VISIBLE);
                targetDieDots[4].setVisibility(View.VISIBLE);
                targetDieDots[8].setVisibility(View.VISIBLE);
                break;
            case 4:
                targetDieDots[0].setVisibility(View.VISIBLE);
                targetDieDots[2].setVisibility(View.VISIBLE); // Top Right
                targetDieDots[6].setVisibility(View.VISIBLE); // Bottom Left
                targetDieDots[8].setVisibility(View.VISIBLE);
                break;
            case 5:
                targetDieDots[0].setVisibility(View.VISIBLE);
                targetDieDots[2].setVisibility(View.VISIBLE);
                targetDieDots[4].setVisibility(View.VISIBLE);
                targetDieDots[6].setVisibility(View.VISIBLE);
                targetDieDots[8].setVisibility(View.VISIBLE);
                break;
            case 6:
                targetDieDots[0].setVisibility(View.VISIBLE);
                targetDieDots[2].setVisibility(View.VISIBLE);
                targetDieDots[3].setVisibility(View.VISIBLE); // Mid Left
                targetDieDots[5].setVisibility(View.VISIBLE); // Mid Right
                targetDieDots[6].setVisibility(View.VISIBLE);
                targetDieDots[8].setVisibility(View.VISIBLE);
                break;
        }
    }

    // Duel Modes Toggle logic
    private void changeDuelGameMode(int modeIndex) {
        activeDuelMode = modeIndex;
        resetDuelArena();

        if (modeIndex == 0) {
            tvGameModeTitle.setText("MODE: ROLL BATTLE");
            btnDuelHold.setVisibility(View.GONE);
            tvTurnPoints.setVisibility(View.GONE);
            tvDuelLog.setText("Roll Battle: Roll 1v1 repeatedly. Best of 5 rounds determines the overall victor.");
        } else {
            tvGameModeTitle.setText("MODE: PIG JEOPARDY");
            btnDuelHold.setVisibility(View.VISIBLE);
            tvTurnPoints.setVisibility(View.VISIBLE);
            tvTurnPoints.setText("Turn points accumulated: 0");
            tvDuelLog.setText("Pig Jeopardy: Accumulate points. Roll a 1 ('Pig Squeal') and lose all turn points. Hold to save permanent score. Reach 50 to win!");
        }
    }

    private void resetDuelArena() {
        p1TotalScore = 0;
        p2TotalScore = 0;
        pigTurnAccumulatedScore = 0;
        duelCurrentTurnPlayer = 1;
        duelRoundCount = 0;

        tvP1Score.setText("0");
        tvP2Score.setText("0");
        tvTurnIndicator.setText("Player 1's Turn");
        tvTurnPoints.setText("Turn points: 0");
    }

    private void triggerDuelDiceRoll() {
        if (isRollingNow) return;
        isRollingNow = true;

        SoundGenerator.playRollSound();

        // Frame animations
        final int animFrames = 10;
        final int durationPerFrame = 50;
        final Handler handler = new Handler();

        Runnable rollAnimation = new Runnable() {
            int frameCount = 0;

            @Override
            public void run() {
                int randomFace = random.nextInt(6) + 1;
                renderVisualDots(duelDieDots, randomFace);

                frameCount++;
                if (frameCount < animFrames) {
                    handler.postDelayed(this, durationPerFrame);
                } else {
                    completeDuelRoll();
                }
            }
        };

        handler.post(rollAnimation);
    }

    private void completeDuelRoll() {
        int finalRoll = random.nextInt(6) + 1;
        renderVisualDots(duelDieDots, finalRoll);

        // Record lifetime statistics
        incrementLifetimeRollStats(finalRoll);
        prefs.edit().putInt(KEY_TOTAL_ROLLS, prefs.getInt(KEY_TOTAL_ROLLS, 0) + 1).apply();

        if (activeDuelMode == 0) {
            // ROLL BATTLE GAME MODE
            handleRollBattleLogic(finalRoll);
        } else {
            // PIG JEOPARDY GAME MODE
            handlePigGameLogic(finalRoll);
        }

        isRollingNow = false;
    }

    private void handleRollBattleLogic(int roll) {
        if (duelCurrentTurnPlayer == 1) {
            p1TotalScore += roll;
            tvP1Score.setText(String.valueOf(p1TotalScore));
            tvDuelLog.setText("Player 1 rolled a " + roll + "! Turning dice over to Player 2...");
            
            duelCurrentTurnPlayer = 2;
            tvTurnIndicator.setText("Player 2's Turn");
        } else {
            p2TotalScore += roll;
            tvP2Score.setText(String.valueOf(p2TotalScore));
            tvDuelLog.setText("Player 2 rolled a " + roll + "!");

            duelRoundCount++;
            if (duelRoundCount >= 5) {
                // Battle Match Finished
                executeDuelVictoryConclusion();
            } else {
                // Round cycle complete, next round setup
                duelCurrentTurnPlayer = 1;
                tvTurnIndicator.setText("Player 1's Turn");
                String logText = tvDuelLog.getText().toString() + "\nRound " + duelRoundCount + " complete! Ready Player 1...";
                tvDuelLog.setText(logText);
            }
        }
    }

    private void handlePigGameLogic(int roll) {
        if (roll == 1) {
            // Pig squeal! Points lost!
            SoundGenerator.playPigSquealSound();
            pigTurnAccumulatedScore = 0;
            tvTurnPoints.setText("Turn points: 0");
            tvDuelLog.setText("🐖 PIGGED OUT! Player " + duelCurrentTurnPlayer + " rolled a 1 and lost all turn points!");

            // Switch turns automatically
            togglePigTurn();
        } else {
            pigTurnAccumulatedScore += roll;
            tvTurnPoints.setText("Turn points accumulated: +" + pigTurnAccumulatedScore);
            tvDuelLog.setText("Player " + duelCurrentTurnPlayer + " rolled a " + roll + "! Cumulative turn points: " + pigTurnAccumulatedScore + ". Roll again or HOLD!");
        }
    }

    private void handlePigHoldPoints() {
        if (pigTurnAccumulatedScore == 0) {
            Toast.makeText(this, "Roll before holding points!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (duelCurrentTurnPlayer == 1) {
            p1TotalScore += pigTurnAccumulatedScore;
            tvP1Score.setText(String.valueOf(p1TotalScore));
            
            if (p1TotalScore >= 50) {
                concludePigVictory(1);
                return;
            }
        } else {
            p2TotalScore += pigTurnAccumulatedScore;
            tvP2Score.setText(String.valueOf(p2TotalScore));

            if (p2TotalScore >= 50) {
                concludePigVictory(2);
                return;
            }
        }

        tvDuelLog.setText("Player " + duelCurrentTurnPlayer + " held points! Accumulated +" + pigTurnAccumulatedScore + " saved successfully.");
        pigTurnAccumulatedScore = 0;
        tvTurnPoints.setText("Turn points: 0");
        togglePigTurn();
    }

    private void togglePigTurn() {
        duelCurrentTurnPlayer = (duelCurrentTurnPlayer == 1) ? 2 : 1;
        tvTurnIndicator.setText("Player " + duelCurrentTurnPlayer + "'s Turn");
    }

    private void executeDuelVictoryConclusion() {
        SoundGenerator.playWinSound();
        String winnerBanner;
        if (p1TotalScore > p2TotalScore) {
            winnerBanner = "🏆 Player 1 WINS Roll Battle (" + p1TotalScore + " - " + p2TotalScore + ")!";
        } else if (p2TotalScore > p1TotalScore) {
            winnerBanner = "🏆 Player 2 WINS Roll Battle (" + p2TotalScore + " - " + p1TotalScore + ")!";
        } else {
            winnerBanner = "🤝 IT'S A TIE MATCH (" + p1TotalScore + " - " + p2TotalScore + ")!";
        }

        tvTurnIndicator.setText("Match Complete!");
        tvDuelLog.setText(winnerBanner + "\nClick ROLL to start a brand new match.");
        
        // Reset scores for next clicks
        p1TotalScore = 0;
        p2TotalScore = 0;
        duelRoundCount = 0;
        duelCurrentTurnPlayer = 1;
    }

    private void concludePigVictory(int winnerIndex) {
        SoundGenerator.playWinSound();
        String victoryMessage = "🎉 Player " + winnerIndex + " reaches " + (winnerIndex == 1 ? p1TotalScore : p2TotalScore) + " points and wins the Pig Game!";
        tvTurnIndicator.setText("Player " + winnerIndex + " wins!");
        tvDuelLog.setText(victoryMessage + "\nClick ROLL to start a new match.");
        
        resetDuelArena();
    }

    // Sensor Shake detection execution
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!isShakeToRollEnabled) return;

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            // Normalize coordinate system relative to earth gravity
            double gForce = Math.sqrt(x * x + y * y + z * z);
            if (gForce > shakeThreshold) {
                long currentTime = System.currentTimeMillis();
                // Cooldown filter preventing overlapping sensor calls
                if (currentTime - lastShakeTime > 1600) {
                    lastShakeTime = currentTime;
                    
                    // Trigger active roll dependent on selected layout tab
                    if (layoutSolo.getVisibility() == View.VISIBLE) {
                        triggerSoloDiceRoll();
                    } else if (layoutDuel.getVisibility() == View.VISIBLE) {
                        triggerDuelDiceRoll();
                    }
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }
}