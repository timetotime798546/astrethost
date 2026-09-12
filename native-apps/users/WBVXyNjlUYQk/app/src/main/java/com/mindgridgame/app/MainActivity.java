package com.mindgridgame.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class MainActivity extends Activity {

    // Gameplay Modes
    private static final int MODE_SEQUENCE = 0;
    private static final int MODE_MATRIX = 1;

    // Dimensions
    private static final int GRID_SIZE = 4; // 4x4 grid system

    // Sleek Theme Hex Colors
    private static final int COLOR_DEFAULT = 0xFF37474F;  // Dark Blue Gray
    private static final int COLOR_FLASH = 0xFFFFEB3B;    // Bright Yellow Accent
    private static final int COLOR_CORRECT = 0xFF4CAF50;  // Vibrant Green Success
    private static final int COLOR_WRONG = 0xFFF44336;    // Bright Red Failure
    private static final int COLOR_ACTIVE_MODE = 0xFF2196F3; // Cyan Blue
    private static final int COLOR_INACTIVE_MODE = 0xFF37474F; // Dark Tone

    // UI Widgets
    private TextView tvLevel;
    private TextView tvScore;
    private TextView tvHighScore;
    private TextView tvStatus;
    private TableLayout tableGrid;
    private Button btnModeSequence;
    private Button btnModeMatrix;
    private Button btnAction;

    private Button[][] tiles;

    // Game Core State Properties
    private int currentMode = MODE_SEQUENCE;
    private boolean isPlaying = false;
    private boolean isShowingPattern = false;
    private int currentLevel = 1;
    private int currentScore = 0;
    private int highScoreSequence = 0;
    private int highScoreMatrix = 0;

    private Random random;
    private Handler delayHandler;
    private ToneGenerator audioEngine;

    // State Sequences
    private List<Integer> challengeSequence;
    private int userSequenceStepIndex = 0;

    // State Matrices
    private Set<Integer> targetMatrixSet;
    private Set<Integer> userSelectedMatrixSet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        random = new Random();
        delayHandler = new Handler();
        challengeSequence = new ArrayList<>();
        targetMatrixSet = new HashSet<>();
        userSelectedMatrixSet = new HashSet<>();

        // Tone alert system
        try {
            audioEngine = new ToneGenerator(AudioManager.STREAM_MUSIC, 90);
        } catch (Exception e) {
            e.printStackTrace();
        }

        initializeViews();
        loadHighScores();
        setupGameLayout();
        refreshStatsView();

        // Register action listeners using Java 8 compatible anonymous classes
        btnModeSequence.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isPlaying) {
                    currentMode = MODE_SEQUENCE;
                    updateModeButtons();
                    refreshStatsView();
                    resetGameSessionState();
                }
            }
        });

        btnModeMatrix.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isPlaying) {
                    currentMode = MODE_MATRIX;
                    updateModeButtons();
                    refreshStatsView();
                    resetGameSessionState();
                }
            }
        });

        btnAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isPlaying) {
                    startGameSession();
                } else {
                    terminateGameSession(false);
                }
            }
        });
    }

    private void initializeViews() {
        tvLevel = (TextView) findViewById(R.id.tv_level);
        tvScore = (TextView) findViewById(R.id.tv_score);
        tvHighScore = (TextView) findViewById(R.id.tv_high_score);
        tvStatus = (TextView) findViewById(R.id.tv_status);
        tableGrid = (TableLayout) findViewById(R.id.table_grid);
        btnModeSequence = (Button) findViewById(R.id.btn_mode_sequence);
        btnModeMatrix = (Button) findViewById(R.id.btn_mode_matrix);
        btnAction = (Button) findViewById(R.id.btn_action);
    }

    private void setupGameLayout() {
        tableGrid.removeAllViews();
        tiles = new Button[GRID_SIZE][GRID_SIZE];

        for (int r = 0; r < GRID_SIZE; r++) {
            TableRow tableRow = new TableRow(this);
            tableRow.setLayoutParams(new TableLayout.LayoutParams(
                    TableLayout.LayoutParams.MATCH_PARENT,
                    0, 1.0f));

            for (int c = 0; c < GRID_SIZE; c++) {
                final int tileIndex = (r * GRID_SIZE) + c;
                final Button tile = new Button(this);
                
                TableRow.LayoutParams rowParams = new TableRow.LayoutParams(0, TableRow.LayoutParams.MATCH_PARENT, 1.0f);
                rowParams.setMargins(8, 8, 8, 8);
                tile.setLayoutParams(rowParams);

                setTileThemeColor(tile, COLOR_DEFAULT);
                tile.setClickable(false);

                tile.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (isPlaying && !isShowingPattern) {
                            handleTileTap(tileIndex);
                        }
                    }
                });

                tiles[r][c] = tile;
                tableRow.addView(tile);
            }
            tableGrid.addView(tableRow);
        }
        updateModeButtons();
    }

    private void setTileThemeColor(Button tile, int colorHex) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(colorHex);
        shape.setCornerRadius(20.0f);
        shape.setStroke(3, 0xFF121212);
        tile.setBackground(shape);
    }

    private void updateModeButtons() {
        if (currentMode == MODE_SEQUENCE) {
            btnModeSequence.setBackgroundTintList(android.content.res.ColorStateList.valueOf(COLOR_ACTIVE_MODE));
            btnModeMatrix.setBackgroundTintList(android.content.res.ColorStateList.valueOf(COLOR_INACTIVE_MODE));
            tvStatus.setText("Sequence Mode: Watch and copy exactly!");
        } else {
            btnModeSequence.setBackgroundTintList(android.content.res.ColorStateList.valueOf(COLOR_INACTIVE_MODE));
            btnModeMatrix.setBackgroundTintList(android.content.res.ColorStateList.valueOf(COLOR_ACTIVE_MODE));
            tvStatus.setText("Matrix Mode: Memorize and match the pattern!");
        }
    }

    private void loadHighScores() {
        SharedPreferences pref = getSharedPreferences("mind_grid_prefs", Context.MODE_PRIVATE);
        highScoreSequence = pref.getInt("high_score_seq", 0);
        highScoreMatrix = pref.getInt("high_score_mat", 0);
    }

    private void saveHighScores() {
        SharedPreferences pref = getSharedPreferences("mind_grid_prefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt("high_score_seq", highScoreSequence);
        editor.putInt("high_score_mat", highScoreMatrix);
        editor.apply();
    }

    private void refreshStatsView() {
        tvLevel.setText(String.valueOf(currentLevel));
        tvScore.setText(String.valueOf(currentScore));
        if (currentMode == MODE_SEQUENCE) {
            tvHighScore.setText(String.valueOf(highScoreSequence));
        } else {
            tvHighScore.setText(String.valueOf(highScoreMatrix));
        }
    }

    private void resetGameSessionState() {
        currentLevel = 1;
        currentScore = 0;
        isPlaying = false;
        isShowingPattern = false;
        btnAction.setText("START GAME");
        btnAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF4CAF50));
        setGridClickable(false);
        clearAllTiles();
        refreshStatsView();
    }

    private void clearAllTiles() {
        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                setTileThemeColor(tiles[r][c], COLOR_DEFAULT);
            }
        }
    }

    private void setGridClickable(boolean clickable) {
        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                tiles[r][c].setClickable(clickable);
            }
        }
    }

    private void startGameSession() {
        isPlaying = true;
        currentLevel = 1;
        currentScore = 0;
        btnAction.setText("STOP GAME");
        btnAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(COLOR_WRONG));
        setGridClickable(true);
        refreshStatsView();
        triggerLevelRoutine();
    }

    private void triggerLevelRoutine() {
        isShowingPattern = true;
        clearAllTiles();
        setGridClickable(false);
        tvStatus.setText("Level " + currentLevel + ": Focus!");

        delayHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (currentMode == MODE_SEQUENCE) {
                    generateNextSequenceStep();
                    playbackSequencePattern();
                } else {
                    generateNextMatrixPattern();
                    playbackMatrixPattern();
                }
            }
        }, 1000);
    }

    // --- SEQUENCE RECALL GAME MODE CONTROLS ---

    private void generateNextSequenceStep() {
        if (currentLevel == 1 && challengeSequence.isEmpty()) {
            challengeSequence.clear();
            challengeSequence.add(random.nextInt(GRID_SIZE * GRID_SIZE));
            challengeSequence.add(random.nextInt(GRID_SIZE * GRID_SIZE));
        }
        // Add one extra step per level
        challengeSequence.add(random.nextInt(GRID_SIZE * GRID_SIZE));
        userSequenceStepIndex = 0;
    }

    private void playbackSequencePattern() {
        tvStatus.setText("Watch sequence closely...");
        long interval = 700; // milliseconds between flashes

        for (int i = 0; i < challengeSequence.size(); i++) {
            final int listIndex = i;
            final int tileIndex = challengeSequence.get(i);

            delayHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!isPlaying) return;
                    triggerTileBeep(tileIndex, 250);

                    // Revert color delay
                    delayHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (!isPlaying) return;
                            setTileThemeColor(getTileByIndex(tileIndex), COLOR_DEFAULT);

                            // Enable user feedback once complete
                            if (listIndex == challengeSequence.size() - 1) {
                                isShowingPattern = false;
                                setGridClickable(true);
                                tvStatus.setText("Now repeat the pattern!");
                            }
                        }
                    }, 400);
                }
            }, i * interval + 300);
        }
    }

    // --- MATRIX MEMORY GAME MODE CONTROLS ---

    private void generateNextMatrixPattern() {
        targetMatrixSet.clear();
        userSelectedMatrixSet.clear();

        // Calculate targets count: scales up with level progression capped optimally
        int numTargets = Math.min(3 + currentLevel, 9);
        List<Integer> randomizedIndices = new ArrayList<>();
        for (int i = 0; i < GRID_SIZE * GRID_SIZE; i++) {
            randomizedIndices.add(i);
        }
        Collections.shuffle(randomizedIndices);

        for (int i = 0; i < numTargets; i++) {
            targetMatrixSet.add(randomizedIndices.get(i));
        }
    }

    private void playbackMatrixPattern() {
        tvStatus.setText("Memorize highlighted spots!");

        // Illuminate all targets together
        for (int index : targetMatrixSet) {
            setTileThemeColor(getTileByIndex(index), COLOR_FLASH);
        }
        triggerSoundTone(ToneGenerator.TONE_PROP_BEEP, 300);

        // Hide tiles and activate input stage
        delayHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isPlaying) return;
                clearAllTiles();
                isShowingPattern = false;
                setGridClickable(true);
                tvStatus.setText("Uncover all active tiles!");
            }
        }, 1500 + (currentLevel * 100)); // Slight duration scale per level
    }

    // --- GENERIC INTERACTION CONTROLLERS ---

    private void handleTileTap(int index) {
        if (currentMode == MODE_SEQUENCE) {
            processSequenceTap(index);
        } else {
            processMatrixTap(index);
        }
    }

    private void processSequenceTap(int tappedIndex) {
        int expectedIndex = challengeSequence.get(userSequenceStepIndex);

        if (tappedIndex == expectedIndex) {
            // Correct tap selection
            triggerTileBeep(tappedIndex, 200);
            userSequenceStepIndex++;

            if (userSequenceStepIndex >= challengeSequence.size()) {
                // Completed the full level sequence successfully
                updateScoreAndAssessLevel();
            }
        } else {
            // Incorrect choice
            triggerSoundTone(ToneGenerator.TONE_SUP_ERROR, 500);
            setTileThemeColor(getTileByIndex(tappedIndex), COLOR_WRONG);
            setTileThemeColor(getTileByIndex(expectedIndex), COLOR_CORRECT);
            terminateGameSession(true);
        }
    }

    private void processMatrixTap(int tappedIndex) {
        if (targetMatrixSet.contains(tappedIndex)) {
            // Selected a correct spot
            if (!userSelectedMatrixSet.contains(tappedIndex)) {
                userSelectedMatrixSet.add(tappedIndex);
                setTileThemeColor(getTileByIndex(tappedIndex), COLOR_CORRECT);
                triggerSoundTone(ToneGenerator.TONE_PROP_BEEP, 150);

                if (userSelectedMatrixSet.size() == targetMatrixSet.size()) {
                    // Uncovered all matrix targets successfully
                    updateScoreAndAssessLevel();
                }
            }
        } else {
            // Incorrect choice
            triggerSoundTone(ToneGenerator.TONE_SUP_ERROR, 500);
            setTileThemeColor(getTileByIndex(tappedIndex), COLOR_WRONG);
            
            // Show all the missed correct locations in yellow
            for (int index : targetMatrixSet) {
                if (!userSelectedMatrixSet.contains(index)) {
                    setTileThemeColor(getTileByIndex(index), COLOR_FLASH);
                }
            }
            terminateGameSession(true);
        }
    }

    private void triggerTileBeep(int tileIndex, int duration) {
        setTileThemeColor(getTileByIndex(tileIndex), COLOR_FLASH);
        // Base note calculation based on coordinate location
        int toneType = ToneGenerator.TONE_PROP_BEEP;
        triggerSoundTone(toneType, duration);
    }

    private Button getTileByIndex(int index) {
        int r = index / GRID_SIZE;
        int c = index % GRID_SIZE;
        return tiles[r][c];
    }

    private void triggerSoundTone(int tone, int durationMs) {
        if (audioEngine != null) {
            audioEngine.startTone(tone, durationMs);
        }
    }

    private void updateScoreAndAssessLevel() {
        currentScore += currentLevel * 10;
        currentLevel++;
        tvStatus.setText("Success! Loading next...");
        refreshStatsView();

        // Safe progression timeout delay
        setGridClickable(false);
        delayHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isPlaying) {
                    triggerLevelRoutine();
                }
            }
        }, 1200);
    }

    private void terminateGameSession(boolean isFailure) {
        isPlaying = false;
        isShowingPattern = false;
        setGridClickable(false);
        btnAction.setText("TRY AGAIN");
        btnAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(COLOR_ACTIVE_MODE));

        if (isFailure) {
            tvStatus.setText("GAME OVER! Keep training.");
            // Evaluate leaderboard records
            if (currentMode == MODE_SEQUENCE) {
                if (currentScore > highScoreSequence) {
                    highScoreSequence = currentScore;
                    tvStatus.setText("NEW BEST SCORE!");
                    saveHighScores();
                }
            } else {
                if (currentScore > highScoreMatrix) {
                    highScoreMatrix = currentScore;
                    tvStatus.setText("NEW BEST SCORE!");
                    saveHighScores();
                }
            }
            refreshStatsView();
        } else {
            tvStatus.setText("Game stopped! Tap start to begin.");
            clearAllTiles();
            challengeSequence.clear();
            targetMatrixSet.clear();
            userSelectedMatrixSet.clear();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (audioEngine != null) {
            audioEngine.release();
        }
    }
}