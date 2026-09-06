package com.sleekcalc.app;

import android.app.Activity;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import java.text.DecimalFormat;
import java.util.ArrayList;

public class MainActivity extends Activity {

    // Views
    private TextView txtExpression;
    private TextView txtDisplay;
    private View layoutLoader;
    private ProgressBar loaderProgress;
    private TextView txtLoaderStatus;
    private LinearLayout layoutCalculator;
    private LinearLayout panelHistory;
    private LinearLayout listHistoryItems;
    private Button btnToggleSound;
    private Button btnOpenHistory;
    private Button btnCloseHistory;
    private Button btnClearHistory;

    // Loader Steps Threading/Timers
    private final Handler loaderHandler = new Handler();
    private int loaderStep = 0;

    // Calculation states
    private String currentInput = "";
    private double firstOperand = Double.NaN;
    private double secondOperand = Double.NaN;
    private char pendingOperator = '\0';
    private boolean isResultDisplayed = false;
    private final ArrayList<String> computationHistory = new ArrayList<>();

    // Audio Engine State
    private ToneGenerator toneGenerator;
    private boolean isSoundEnabled = true;

    // Sound Frequency profiles mapped with calculation actions
    private static final int SOUND_DIGIT = ToneGenerator.TONE_DTMF_1;
    private static final int SOUND_OPERATOR = ToneGenerator.TONE_DTMF_A;
    private static final int SOUND_RESULT = ToneGenerator.TONE_DTMF_0;
    private static final int SOUND_CLEAR = ToneGenerator.TONE_PROP_PROMPT;

    // Decimals utility representation
    private final DecimalFormat decimalFormat = new DecimalFormat("#.########");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind interactive display components
        txtExpression = (TextView) findViewById(R.id.txt_expression);
        txtDisplay = (TextView) findViewById(R.id.txt_display);
        layoutLoader = findViewById(R.id.layout_loader);
        loaderProgress = (ProgressBar) findViewById(R.id.loader_progress);
        txtLoaderStatus = (TextView) findViewById(R.id.txt_loader_status);
        layoutCalculator = (LinearLayout) findViewById(R.id.layout_calculator);
        panelHistory = (LinearLayout) findViewById(R.id.panel_history);
        listHistoryItems = (LinearLayout) findViewById(R.id.list_history_items);
        btnToggleSound = (Button) findViewById(R.id.btn_toggle_sound);
        btnOpenHistory = (Button) findViewById(R.id.btn_open_history);
        btnCloseHistory = (Button) findViewById(R.id.btn_close_history);
        btnClearHistory = (Button) findViewById(R.id.btn_clear_history);

        // Initialize Native DSP Synthesizer
        try {
            toneGenerator = new ToneGenerator(AudioManager.STREAM_SYSTEM, 70);
        } catch (Exception e) {
            toneGenerator = null;
        }

        // Apply Click listener mapping directly to elements
        registerCalculationsKeypad();
        setupHistoryActions();
        executeLoaderSequence();
    }

    /**
     * Simulation steps providing high fidelity visual indicator of loader config setup
     */
    private void executeLoaderSequence() {
        loaderHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (loaderStep == 0) {
                    txtLoaderStatus.setText("Assembling Audio Tone Synthesizer...");
                    loaderStep++;
                    loaderHandler.postDelayed(this, 750);
                } else if (loaderStep == 1) {
                    txtLoaderStatus.setText("Allocating calculation matrix workspace...");
                    loaderStep++;
                    loaderHandler.postDelayed(this, 750);
                } else if (loaderStep == 2) {
                    txtLoaderStatus.setText("Interface assets fully configured!");
                    loaderStep++;
                    loaderHandler.postDelayed(this, 500);
                } else {
                    dismissLoader();
                }
            }
        }, 500);
    }

    private void dismissLoader() {
        AlphaAnimation fadeOut = new AlphaAnimation(1.0f, 0.0f);
        fadeOut.setDuration(400);
        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                layoutLoader.setVisibility(View.GONE);
                // Sound cue for successful app initialization
                playSound(SOUND_RESULT);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        layoutLoader.startAnimation(fadeOut);
    }

    private void playSound(int soundType) {
        if (isSoundEnabled && toneGenerator != null) {
            try {
                toneGenerator.startTone(soundType, 120);
            } catch (Exception e) {
                // Recover gracefully from audio sub-system blocks
            }
        }
    }

    private void setupHistoryActions() {
        btnToggleSound.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isSoundEnabled = !isSoundEnabled;
                if (isSoundEnabled) {
                    btnToggleSound.setText("🔊");
                    playSound(SOUND_DIGIT);
                } else {
                    btnToggleSound.setText("🔇");
                }
            }
        });

        btnOpenHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playSound(SOUND_OPERATOR);
                displayHistoryPanel(true);
            }
        });

        btnCloseHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playSound(SOUND_CLEAR);
                displayHistoryPanel(false);
            }
        });

        btnClearHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playSound(SOUND_CLEAR);
                computationHistory.clear();
                populateHistoryList();
            }
        });
    }

    private void displayHistoryPanel(boolean show) {
        if (show) {
            populateHistoryList();
            panelHistory.setVisibility(View.VISIBLE);
            AlphaAnimation slideIn = new AlphaAnimation(0.0f, 1.0f);
            slideIn.setDuration(250);
            panelHistory.startAnimation(slideIn);
        } else {
            AlphaAnimation slideOut = new AlphaAnimation(1.0f, 0.0f);
            slideOut.setDuration(200);
            slideOut.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {}

                @Override
                public void onAnimationEnd(Animation animation) {
                    panelHistory.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {}
            });
            panelHistory.startAnimation(slideOut);
        }
    }

    private void populateHistoryList() {
        listHistoryItems.removeAllViews();
        if (computationHistory.isEmpty()) {
            TextView emptyText = new TextView(MainActivity.this);
            emptyText.setText(getString(R.string.no_history));
            emptyText.setTextColor(getResources().getColor(R.color.text_sub));
            emptyText.setTextSize(14sp);
            emptyText.setPadding(0, 16, 0, 0);
            listHistoryItems.addView(emptyText);
        } else {
            // Render computation items descending order
            for (int i = computationHistory.size() - 1; i >= 0; i--) {
                String historyEntry = computationHistory.get(i);
                
                LinearLayout itemLayout = new LinearLayout(MainActivity.this);
                itemLayout.setOrientation(LinearLayout.VERTICAL);
                itemLayout.setPadding(0, 12, 0, 12);

                TextView tvEntry = new TextView(MainActivity.this);
                tvEntry.setText(historyEntry);
                tvEntry.setTextColor(getResources().getColor(R.color.text_main));
                tvEntry.setTextSize(16sp);

                View separator = new View(MainActivity.this);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1);
                lp.topMargin = 12;
                separator.setLayoutParams(lp);
                separator.setBackgroundColor(getResources().getColor(R.color.btn_number_pressed));

                itemLayout.addView(tvEntry);
                itemLayout.addView(separator);
                
                listHistoryItems.addView(itemLayout);
            }
        }
    }

    private void registerCalculationsKeypad() {
        // Digits
        int[] digitsIds = {
                R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4,
                R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9,
                R.id.btn_double_zero, R.id.btn_dot
        };

        for (int id : digitsIds) {
            findViewById(id).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    TextView clicked = (TextView) v;
                    appendDigit(clicked.getText().toString());
                }
            });
        }

        // Mathematical Operations listeners
        findViewById(R.id.btn_add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { applyOperator('+'); }
        });
        findViewById(R.id.btn_subtract).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { applyOperator('-'); }
        });
        findViewById(R.id.btn_multiply).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { applyOperator('×'); }
        });
        findViewById(R.id.btn_divide).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { applyOperator('÷'); }
        });

        findViewById(R.id.btn_percent).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyPercent();
            }
        });

        findViewById(R.id.btn_clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearScreen();
            }
        });

        findViewById(R.id.btn_delete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                backspaceExpression();
            }
        });

        findViewById(R.id.btn_equals).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                solveCalculation();
            }
        });
    }

    private void appendDigit(String value) {
        playSound(SOUND_DIGIT);
        if (isResultDisplayed) {
            currentInput = "";
            isResultDisplayed = false;
        }

        if (value.equals(".")) {
            if (currentInput.contains(".")) {
                return; // Guard duplicate decimals
            }
            if (currentInput.isEmpty()) {
                currentInput = "0";
            }
        }

        if (value.equals("00") && currentInput.isEmpty()) {
            currentInput = "0";
        } else {
            currentInput += value;
        }

        txtDisplay.setText(currentInput);
    }

    private void applyOperator(char operator) {
        playSound(SOUND_OPERATOR);
        if (!currentInput.isEmpty()) {
            if (!Double.isNaN(firstOperand)) {
                solveChainCalculation();
            } else {
                firstOperand = Double.parseDouble(currentInput);
            }
        } else if (Double.isNaN(firstOperand)) {
            firstOperand = 0;
        }

        pendingOperator = operator;
        txtExpression.setText(decimalFormat.format(firstOperand) + " " + pendingOperator);
        currentInput = "";
        isResultDisplayed = false;
    }

    private void applyPercent() {
        playSound(SOUND_OPERATOR);
        if (!currentInput.isEmpty()) {
            double value = Double.parseDouble(currentInput);
            value = value / 100.0;
            currentInput = String.valueOf(value);
            txtDisplay.setText(decimalFormat.format(value));
        }
    }

    private void solveChainCalculation() {
        if (!currentInput.isEmpty() && !Double.isNaN(firstOperand)) {
            secondOperand = Double.parseDouble(currentInput);
            double solution = performOperation(firstOperand, secondOperand, pendingOperator);
            firstOperand = solution;
        }
    }

    private void solveCalculation() {
        playSound(SOUND_RESULT);
        if (Double.isNaN(firstOperand) || currentInput.isEmpty()) {
            return;
        }

        secondOperand = Double.parseDouble(currentInput);
        double result = performOperation(firstOperand, secondOperand, pendingOperator);

        if (Double.isInfinite(result) || Double.isNaN(result)) {
            txtDisplay.setText("Error");
            txtExpression.setText("");
            firstOperand = Double.NaN;
            currentInput = "";
        } else {
            String expText = decimalFormat.format(firstOperand) + " " + pendingOperator + " " + decimalFormat.format(secondOperand);
            String resultText = decimalFormat.format(result);

            txtExpression.setText(expText + " =");
            txtDisplay.setText(resultText);

            // Record dynamic computation memory entry
            computationHistory.add(expText + "\n= " + resultText);

            firstOperand = result;
            currentInput = resultText;
            isResultDisplayed = true;
        }
        pendingOperator = '\0';
    }

    private double performOperation(double a, double b, char op) {
        switch (op) {
            case '+': return a + b;
            case '-': return a - b;
            case '×': return a * b;
            case '÷': 
                if (b == 0) return Double.NaN;
                return a / b;
            default: return b;
        }
    }

    private void clearScreen() {
        playSound(SOUND_CLEAR);
        currentInput = "";
        firstOperand = Double.NaN;
        secondOperand = Double.NaN;
        pendingOperator = '\0';
        txtDisplay.setText("0");
        txtExpression.setText("");
        isResultDisplayed = false;
    }

    private void backspaceExpression() {
        playSound(SOUND_CLEAR);
        if (isResultDisplayed) {
            txtExpression.setText("");
            return;
        }
        if (currentInput != null && currentInput.length() > 0) {
            currentInput = currentInput.substring(0, currentInput.length() - 1);
            if (currentInput.isEmpty()) {
                txtDisplay.setText("0");
            } else {
                txtDisplay.setText(currentInput);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (toneGenerator != null) {
            toneGenerator.release();
            toneGenerator = null;
        }
    }
}