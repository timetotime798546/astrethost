package com.brainblitz.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Random;

public class MainActivity extends Activity {

    // Game modes constants
    private static final int MODE_NONE = 0;
    private static final int MODE_MATRIX = 1;
    private static final int MODE_MATH = 2;
    private static final int MODE_STROOP = 3;

    // Sound effect identifiers
    private static final int SOUND_SUCCESS = 1;
    private static final int SOUND_FAIL = 2;
    private static final int SOUND_LEVELUP = 3;
    private static final int SOUND_TICK = 4;

    // UI View handles
    private LinearLayout mLayoutHeader;
    private TextView mTxtGameModeTitle;
    private TextView mTxtGameLives;
    private TextView mTxtGameScore;
    private ProgressBar mProgressTimer;

    private View mPanelMenu;
    private View mPanelMatrix;
    private View mPanelMath;
    private View mPanelStroop;
    private View mPanelGameOver;

    private TextView mTxtHighMatrix;
    private TextView mTxtHighMath;
    private TextView mTxtHighStroop;

    // Main menu options
    private LinearLayout mBtnModeMatrix;
    private LinearLayout mBtnModeMath;
    private LinearLayout mBtnModeStroop;
    private LinearLayout mCardStats;

    // Game Over UI handles
    private TextView mTxtGameOverSummary;
    private TextView mTxtFinalScore;
    private TextView mTxtCongratsSub;
    private Button mBtnReturnMenu;
    private LinearLayout mCardGameOverScore;

    // Matrix Game Mode Views
    private TextView mTxtMatrixStatus;
    private GridLayout mGridMatrix;

    // Math Game Mode Views
    private TextView mTxtMathExpression;
    private Button mBtnMathFalse;
    private Button mBtnMathTrue;
    private LinearLayout mCardMathTerm;

    // Stroop Game Mode Views
    private TextView mTxtStroopWord;
    private Button mBtnStroopFalse;
    private Button mBtnStroopTrue;
    private LinearLayout mCardStroop;

    // Tone Generator for sound syntheses
    private ToneGenerator mToneGenerator;
    private SharedPreferences mPrefs;
    private final Random mRandom = new Random();
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());

    // Central Core Engine Variables
    private int mActiveMode = MODE_NONE;
    private int mCurrentScore = 0;
    private int mCurrentLives = 3;
    private int mCurrentLevel = 1;
    private boolean mIsAcceptingInput = false;

    // Game Mode 1 variables (Memory Matrix)
    private int mMatrixSize = 3; // Start from 3x3
    private ArrayList<Integer> mSelectedPatternIndices = new ArrayList<Integer>();
    private ArrayList<Integer> mUserSelectedPatternIndices = new ArrayList<Integer>();
    private ArrayList<Button> mGridButtonsList = new ArrayList<Button>();

    // Game Mode 2 variables (Math Sprint)
    private boolean mCurrentMathEquationTruth = false;

    // Game Mode 3 variables (Stroop Reflex)
    private final String[] mStroopWords = {"RED", "BLUE", "GREEN", "YELLOW", "ORANGE"};
    private final int[] mStroopColors = {
            Color.parseColor("#EF4444"), // RED
            Color.parseColor("#3B82F6"), // BLUE
            Color.parseColor("#10B981"), // GREEN
            Color.parseColor("#F59E0B"), // YELLOW
            Color.parseColor("#F97316")  // ORANGE
    };
    private boolean mCurrentStroopTruth = false;

    // Count Down Timers
    private int mTimerValue = 100;
    private Runnable mTimerRunnable;
    private int mTimerSpeedMilliseconds = 60; // smaller = faster

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPrefs = getSharedPreferences("BrainBlitzPrefs", Context.MODE_PRIVATE);
        initSoundSystem();
        initViews();
        applyBeautifulVisualStyles();
        bindClickListeners();
        displayStoredHighScores();
    }

    private void initSoundSystem() {
        try {
            mToneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void triggerProgrammaticSound(int type) {
        if (mToneGenerator == null) return;
        try {
            if (type == SOUND_SUCCESS) {
                mToneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 100);
            } else if (type == SOUND_FAIL) {
                mToneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP2, 280);
            } else if (type == SOUND_LEVELUP) {
                mToneGenerator.startTone(ToneGenerator.TONE_CDMA_PIP, 200);
            } else if (type == SOUND_TICK) {
                mToneGenerator.startTone(ToneGenerator.TONE_CDMA_CONFIRM, 50);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initViews() {
        mLayoutHeader = (LinearLayout) findViewById(R.id.layout_header);
        mTxtGameModeTitle = (TextView) findViewById(R.id.txt_game_mode_title);
        mTxtGameLives = (TextView) findViewById(R.id.txt_game_lives);
        mTxtGameScore = (TextView) findViewById(R.id.txt_game_score);
        mProgressTimer = (ProgressBar) findViewById(R.id.progress_timer);

        mPanelMenu = findViewById(R.id.panel_menu);
        mPanelMatrix = findViewById(R.id.panel_game_matrix);
        mPanelMath = findViewById(R.id.panel_game_math);
        mPanelStroop = findViewById(R.id.panel_game_stroop);
        mPanelGameOver = findViewById(R.id.panel_game_over);

        mTxtHighMatrix = (TextView) findViewById(R.id.txt_high_matrix);
        mTxtHighMath = (TextView) findViewById(R.id.txt_high_math);
        mTxtHighStroop = (TextView) findViewById(R.id.txt_high_stroop);

        mBtnModeMatrix = (LinearLayout) findViewById(R.id.btn_mode_matrix);
        mBtnModeMath = (LinearLayout) findViewById(R.id.btn_mode_math);
        mBtnModeStroop = (LinearLayout) findViewById(R.id.btn_mode_stroop);
        mCardStats = (LinearLayout) findViewById(R.id.card_stats_summary);

        mTxtGameOverSummary = (TextView) findViewById(R.id.txt_gameover_summary);
        mTxtFinalScore = (TextView) findViewById(R.id.txt_final_score);
        mTxtCongratsSub = (TextView) findViewById(R.id.txt_congrats_sub);
        mBtnReturnMenu = (Button) findViewById(R.id.btn_return_menu);
        mCardGameOverScore = (LinearLayout) findViewById(R.id.card_gameover_score);

        mTxtMatrixStatus = (TextView) findViewById(R.id.txt_matrix_status);
        mGridMatrix = (GridLayout) findViewById(R.id.grid_matrix);

        mTxtMathExpression = (TextView) findViewById(R.id.txt_math_expression);
        mBtnMathFalse = (Button) findViewById(R.id.btn_math_false);
        mBtnMathTrue = (Button) findViewById(R.id.btn_math_true);
        mCardMathTerm = (LinearLayout) findViewById(R.id.card_math_term);

        mTxtStroopWord = (TextView) findViewById(R.id.txt_stroop_word);
        mBtnStroopFalse = (Button) findViewById(R.id.btn_stroop_false);
        mBtnStroopTrue = (Button) findViewById(R.id.btn_stroop_true);
        mCardStroop = (LinearLayout) findViewById(R.id.card_stroop);
    }

    private void applyBeautifulVisualStyles() {
        // Style main panel cards & buttons with lovely curves
        mCardStats.setBackground(createRoundedDrawable(Color.parseColor("#FFFFFF"), 32, Color.parseColor("#E2E8F0"), 3));
        mBtnModeMatrix.setBackground(createRoundedDrawable(Color.parseColor("#FFFFFF"), 24, Color.parseColor("#E2E8F0"), 2));
        mBtnModeMath.setBackground(createRoundedDrawable(Color.parseColor("#FFFFFF"), 24, Color.parseColor("#E2E8F0"), 2));
        mBtnModeStroop.setBackground(createRoundedDrawable(Color.parseColor("#FFFFFF"), 24, Color.parseColor("#E2E8F0"), 2));

        mCardMathTerm.setBackground(createRoundedDrawable(Color.parseColor("#FFFFFF"), 32, Color.parseColor("#E2E8F0"), 4));
        mCardStroop.setBackground(createRoundedDrawable(Color.parseColor("#FFFFFF"), 32, Color.parseColor("#E2E8F0"), 4));
        mCardGameOverScore.setBackground(createRoundedDrawable(Color.parseColor("#FFFFFF"), 32, Color.parseColor("#E2E8F0"), 4));

        // Standard Buttons styled programmatically to look premium
        mBtnMathTrue.setBackground(createRoundedDrawable(Color.parseColor("#10B981"), 20, 0, 0));
        mBtnMathFalse.setBackground(createRoundedDrawable(Color.parseColor("#EF4444"), 20, 0, 0));
        mBtnStroopTrue.setBackground(createRoundedDrawable(Color.parseColor("#10B981"), 20, 0, 0));
        mBtnStroopFalse.setBackground(createRoundedDrawable(Color.parseColor("#EF4444"), 20, 0, 0));
        mBtnReturnMenu.setBackground(createRoundedDrawable(Color.parseColor("#4F46E5"), 20, 0, 0));
    }

    private GradientDrawable createRoundedDrawable(int backgroundColor, float cornerRadius, int strokeColor, int strokeWidth) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(backgroundColor);
        gd.setCornerRadius(cornerRadius);
        if (strokeWidth > 0) {
            gd.setStroke(strokeWidth, strokeColor);
        }
        return gd;
    }

    private void displayStoredHighScores() {
        mTxtHighMatrix.setText("• Memory Matrix: " + mPrefs.getInt("high_matrix", 0) + " pts");
        mTxtHighMath.setText("• Math Sprint: " + mPrefs.getInt("high_math", 0) + " pts");
        mTxtHighStroop.setText("• Stroop Reflex: " + mPrefs.getInt("high_stroop", 0) + " pts");
    }

    private void bindClickListeners() {
        // Menu Buttons listeners
        mBtnModeMatrix.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startGame(MODE_MATRIX);
            }
        });

        mBtnModeMath.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startGame(MODE_MATH);
            }
        });

        mBtnModeStroop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startGame(MODE_STROOP);
            }
        });

        mBtnReturnMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchPanel(mPanelMenu);
                mLayoutHeader.setVisibility(View.GONE);
                mProgressTimer.setVisibility(View.GONE);
                displayStoredHighScores();
            }
        });

        // Math Buttons listeners
        mBtnMathTrue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleMathAnswer(true);
            }
        });
        mBtnMathFalse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleMathAnswer(false);
            }
        });

        // Stroop Buttons listeners
        mBtnStroopTrue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleStroopAnswer(true);
            }
        });
        mBtnStroopFalse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleStroopAnswer(false);
            }
        });
    }

    private void switchPanel(View activePanel) {
        mPanelMenu.setVisibility(View.GONE);
        mPanelMatrix.setVisibility(View.GONE);
        mPanelMath.setVisibility(View.GONE);
        mPanelStroop.setVisibility(View.GONE);
        mPanelGameOver.setVisibility(View.GONE);

        activePanel.setVisibility(View.VISIBLE);
    }

    private void updateHeaderUI() {
        mTxtGameScore.setText("Score: " + mCurrentScore);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            if (i < mCurrentLives) {
                sb.append("❤");
            } else {
                sb.append("🖤");
            }
        }
        mTxtGameLives.setText(sb.toString());
    }

    private void startGame(int mode) {
        mActiveMode = mode;
        mCurrentScore = 0;
        mCurrentLives = 3;
        mCurrentLevel = 1;
        updateHeaderUI();
        mLayoutHeader.setVisibility(View.VISIBLE);

        if (mode == MODE_MATRIX) {
            mTxtGameModeTitle.setText("Memory Matrix");
            mMatrixSize = 3; // Starts with 3x3
            switchPanel(mPanelMatrix);
            startNewMatrixRound();
        } else if (mode == MODE_MATH) {
            mTxtGameModeTitle.setText("Math Sprint");
            mProgressTimer.setVisibility(View.VISIBLE);
            mTimerSpeedMilliseconds = 60;
            switchPanel(mPanelMath);
            startNewMathRound();
        } else if (mode == MODE_STROOP) {
            mTxtGameModeTitle.setText("Stroop Reflex");
            mProgressTimer.setVisibility(View.VISIBLE);
            mTimerSpeedMilliseconds = 55;
            switchPanel(mPanelStroop);
            startNewStroopRound();
        }
    }

    // ========================================== 
    // GAME MODE 1: MEMORY MATRIX MODULE 
    // ========================================== 
    private void startNewMatrixRound() {
        mIsAcceptingInput = false;
        mTxtMatrixStatus.setText("Flashes ko dhyan se dekho!");
        mTxtMatrixStatus.setTextColor(Color.parseColor("#4F46E5"));

        // Determine scaling based on level progress
        if (mCurrentLevel < 4) {
            mMatrixSize = 3;
        } else if (mCurrentLevel < 8) {
            mMatrixSize = 4;
        } else {
            mMatrixSize = 5;
        }

        mGridMatrix.removeAllViews();
        mGridMatrix.setColumnCount(mMatrixSize);
        mGridMatrix.setRowCount(mMatrixSize);
        mGridButtonsList.clear();
        mSelectedPatternIndices.clear();
        mUserSelectedPatternIndices.clear();

        int totalTiles = mMatrixSize * mMatrixSize;

        // Generate dynamic grids
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int padSize = screenWidth / 18;
        int tileSize = (screenWidth - (padSize * 3)) / mMatrixSize;

        for (int i = 0; i < totalTiles; i++) {
            final int index = i;
            Button btn = new Button(this);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = tileSize;
            params.height = tileSize;
            params.setMargins(6, 6, 6, 6);
            btn.setLayoutParams(params);
            btn.setBackground(createRoundedDrawable(Color.parseColor("#CBD5E1"), 12, 0, 0));

            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onMatrixTileClicked(index);
                }
            });
            mGridMatrix.addView(btn);
            mGridButtonsList.add(btn);
        }

        // Choose random targets according to difficulty levels
        int targetCount = mMatrixSize + (mCurrentLevel / 2);
        if (targetCount > totalTiles - 2) {
            targetCount = totalTiles - 2;
        }

        while (mSelectedPatternIndices.size() < targetCount) {
            int candidate = mRandom.nextInt(totalTiles);
            if (!mSelectedPatternIndices.contains(candidate)) {
                mSelectedPatternIndices.add(candidate);
            }
        }

        // Flash correct indices in sequence or combined layout after a brief start delay
        mMainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                flashMatrixPattern();
            }
        }, 900);
    } 

    private void flashMatrixPattern() {
        for (int index : mSelectedPatternIndices) {
            mGridButtonsList.get(index).setBackground(createRoundedDrawable(Color.parseColor("#3B82F6"), 12, 0, 0));
        }

        // Revert pattern to gray standard shade after short period
        mMainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                for (int index : mSelectedPatternIndices) {
                    mGridButtonsList.get(index).setBackground(createRoundedDrawable(Color.parseColor("#CBD5E1"), 12, 0, 0));
                }
                mIsAcceptingInput = true;
                mTxtMatrixStatus.setText("Ab select karo pattern!");
                mTxtMatrixStatus.setTextColor(Color.parseColor("#1E293B"));
            }
        }, 1200 + (mCurrentLevel * 50));
    }

    private void onMatrixTileClicked(int index) {
        if (!mIsAcceptingInput) return;

        // If user selects same index, ignore
        if (mUserSelectedPatternIndices.contains(index)) return;

        if (mSelectedPatternIndices.contains(index)) {
            // Correct pick
            mUserSelectedPatternIndices.add(index);
            mGridButtonsList.get(index).setBackground(createRoundedDrawable(Color.parseColor("#10B981"), 12, 0, 0));
            triggerProgrammaticSound(SOUND_SUCCESS);

            // Complete verification of current sequence progress
            if (mUserSelectedPatternIndices.size() == mSelectedPatternIndices.size()) {
                mIsAcceptingInput = false;
                mCurrentScore += (mCurrentLevel * 15);
                mCurrentLevel++;
                updateHeaderUI();
                mTxtMatrixStatus.setText("Shandar! Level Complete 🌟");
                mTxtMatrixStatus.setTextColor(Color.parseColor("#10B981"));
                triggerProgrammaticSound(SOUND_LEVELUP);

                mMainHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startNewMatrixRound();
                    }
                }, 1200);
            }
        } else {
            // Incorrect Pick
            mIsAcceptingInput = false;
            mGridButtonsList.get(index).setBackground(createRoundedDrawable(Color.parseColor("#EF4444"), 12, 0, 0));
            triggerProgrammaticSound(SOUND_FAIL);
            mCurrentLives--;
            updateHeaderUI();

            // Reveal correct pattern
            for (int correctIdx : mSelectedPatternIndices) {
                mGridButtonsList.get(correctIdx).setBackground(createRoundedDrawable(Color.parseColor("#3B82F6"), 12, 0, 0));
            }

            if (mCurrentLives <= 0) {
                mMainHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        endActiveGame("Memory Matrix completed at level " + mCurrentLevel);
                    }
                }, 1300);
            } else {
                mTxtMatrixStatus.setText("Galat block! Try again... 💔");
                mTxtMatrixStatus.setTextColor(Color.parseColor("#EF4444"));
                mMainHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startNewMatrixRound();
                    }
                }, 1500);
            }
        }
    }

    // ========================================== 
    // GAME MODE 2: MATH SPRINT ENGINE
    // ========================================== 
    private void startNewMathRound() {
        stopActiveCountdown();

        // Scale numbers range and operators complexity with score increase
        int range = 10 + (mCurrentScore / 10);
        int numA = mRandom.nextInt(range) + 2;
        int numB = mRandom.nextInt(range) + 2;
        int operatorSelector = mRandom.nextInt(3); // 0: add, 1: subtract, 2: multiply

        int actualSolution = 0;
        char mathSymbol = '+';

        if (operatorSelector == 0) {
            actualSolution = numA + numB;
            mathSymbol = '+';
        } else if (operatorSelector == 1) {
            // Ensure outcome is non-negative generally to avoid complex subtraction sprint logic
            if (numA < numB) {
                int temporary = numA;
                numA = numB;
                numB = temporary;
            }
            actualSolution = numA - numB;
            mathSymbol = '-';
        } else {
            numA = mRandom.nextInt(8) + 2;
            numB = mRandom.nextInt(8) + 2;
            actualSolution = numA * numB;
            mathSymbol = '×';
        }

        // 50% probability to inject error into shown sum
        mCurrentMathEquationTruth = mRandom.nextBoolean();
        int displayOutcome = actualSolution;
        if (!mCurrentMathEquationTruth) {
            int errorVariance = mRandom.nextInt(5) + 1;
            if (mRandom.nextBoolean()) {
                displayOutcome += errorVariance;
            } else {
                displayOutcome -= errorVariance;
            }
            // Fallback to guarantee they are never matching
            if (displayOutcome == actualSolution) {
                displayOutcome += 2;
            }
        }

        mTxtMathExpression.setText(numA + " " + mathSymbol + " " + numB + " = " + displayOutcome);
        initGameplayTimer(100);
    }

    private void handleMathAnswer(boolean userGuessedTrue) {
        if (userGuessedTrue == mCurrentMathEquationTruth) {
            // Correct
            triggerProgrammaticSound(SOUND_SUCCESS);
            mCurrentScore += 10;
            updateHeaderUI();
            startNewMathRound();
        } else {
            // Wrong answer penalty
            triggerProgrammaticSound(SOUND_FAIL);
            mCurrentLives--;
            updateHeaderUI();
            if (mCurrentLives <= 0) {
                endActiveGame("Math Sprint completed.");
            } else {
                startNewMathRound();
            }
        }
    }

    // ========================================== 
    // GAME MODE 3: STROOP COLOR REFLEX ENGINE
    // ========================================== 
    private void startNewStroopRound() {
        stopActiveCountdown();

        // Pick index of spelling name
        int spellingIndex = mRandom.nextInt(mStroopWords.length);
        // Pick index of visual color ink
        int inkIndex;
        mCurrentStroopTruth = mRandom.nextBoolean();

        if (mCurrentStroopTruth) {
            inkIndex = spellingIndex;
        } else {
            inkIndex = mRandom.nextInt(mStroopColors.length);
            if (inkIndex == spellingIndex) {
                inkIndex = (inkIndex + 1) % mStroopColors.length;
            }
        }

        mTxtStroopWord.setText(mStroopWords[spellingIndex]);
        mTxtStroopWord.setTextColor(mStroopColors[inkIndex]);

        initGameplayTimer(100);
    }

    private void handleStroopAnswer(boolean userGuessedYes) {
        if (userGuessedYes == mCurrentStroopTruth) {
            triggerProgrammaticSound(SOUND_SUCCESS);
            mCurrentScore += 10;
            updateHeaderUI();
            startNewStroopRound();
        } else {
            triggerProgrammaticSound(SOUND_FAIL);
            mCurrentLives--;
            updateHeaderUI();
            if (mCurrentLives <= 0) {
                endActiveGame("Stroop Reflex completed.");
            } else {
                startNewStroopRound();
            }
        }
    }

    // ========================================== 
    // SHARED GAME TIMER CONTROLS
    // ========================================== 
    private void initGameplayTimer(int startingValue) {
        mTimerValue = startingValue;
        mProgressTimer.setProgress(mTimerValue);

        mTimerRunnable = new Runnable() {
            @Override
            public void run() {
                mTimerValue -= 2;
                mProgressTimer.setProgress(mTimerValue);

                if (mTimerValue <= 0) {
                    // Time Out Penalty
                    triggerProgrammaticSound(SOUND_FAIL);
                    mCurrentLives--;
                    updateHeaderUI();
                    if (mCurrentLives <= 0) {
                        endActiveGame("Waqt khatam ho gya!");
                    } else {
                        if (mActiveMode == MODE_MATH) {
                            startNewMathRound();
                        } else if (mActiveMode == MODE_STROOP) {
                            startNewStroopRound();
                        }
                    }
                } else {
                    if (mTimerValue == 30) {
                        // Warn with a brief soft tick sound when close to edge
                        triggerProgrammaticSound(SOUND_TICK);
                    }
                    // Adaptive speed mapping logic
                    int speedModifier = mTimerSpeedMilliseconds - (mCurrentScore / 15);
                    if (speedModifier < 25) {
                        speedModifier = 25;
                    }
                    mMainHandler.postDelayed(this, speedModifier);
                }
            }
        };
        mMainHandler.post(mTimerRunnable);
    }

    private void stopActiveCountdown() {
        if (mTimerRunnable != null) {
            mMainHandler.removeCallbacks(mTimerRunnable);
        }
    }

    // ========================================== 
    // GAME STATE OVER SUMMARY PANEL
    // ========================================== 
    private void endActiveGame(String reasonMessage) {
        stopActiveCountdown();
        mIsAcceptingInput = false;
        switchPanel(mPanelGameOver);
        mLayoutHeader.setVisibility(View.GONE);
        mProgressTimer.setVisibility(View.GONE);

        mTxtFinalScore.setText(String.valueOf(mCurrentScore));
        mTxtGameOverSummary.setText(reasonMessage + "\nSahi khel ke apne dimag ko dhar di! Try again to score higher!");

        // Compute score records
        String highKey = "";
        if (mActiveMode == MODE_MATRIX) {
            highKey = "high_matrix";
        } else if (mActiveMode == MODE_MATH) {
            highKey = "high_math";
        } else if (mActiveMode == MODE_STROOP) {
            highKey = "high_stroop";
        }

        int existingHighScore = mPrefs.getInt(highKey, 0);
        if (mCurrentScore > existingHighScore) {
            mPrefs.edit().putInt(highKey, mCurrentScore).apply();
            mTxtCongratsSub.setText("🏆 New Personal High Score Record!");
            mTxtCongratsSub.setVisibility(View.VISIBLE);
        } else {
            mTxtCongratsSub.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        stopActiveCountdown();
        if (mToneGenerator != null) {
            mToneGenerator.release();
        }
        super.onDestroy();
    }
}