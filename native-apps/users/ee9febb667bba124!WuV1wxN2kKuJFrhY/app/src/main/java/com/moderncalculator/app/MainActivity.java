package com.moderncalculator.app;

import android.app.Activity;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class MainActivity extends Activity {

    private TextView tvFormula;
    private TextView tvDisplay;

    private String currentInput = "";
    private String activeOperator = "";
    private double firstOperand = Double.NaN;
    private boolean isOperatorJustPressed = false;
    private boolean hasEvaluated = false;

    private DecimalFormat decimalFormat;
    private ToneGenerator toneGenerator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize display numeric formatter
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        decimalFormat = new DecimalFormat("#.##########", symbols);

        // Map layout elements
        tvFormula = (TextView) findViewById(R.id.tv_formula);
        tvDisplay = (TextView) findViewById(R.id.tv_display);

        // Bind interactive event handlers
        setupButtonListeners();
        setupHistoryAndLoader();
    }

    private void setupButtonListeners() {
        // Digit selections
        int[] numIds = {
                R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4,
                R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9
        };

        View.OnClickListener numListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playClickFeedback(view, 1);
                Button b = (Button) view;
                appendNumber(b.getText().toString());
            }
        };

        for (int id : numIds) {
            findViewById(id).setOnClickListener(numListener);
        }

        // Action operations
        findViewById(R.id.btn_decimal).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playClickFeedback(v, 1);
                appendDecimal();
            }
        });

        findViewById(R.id.btn_clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playClickFeedback(v, 2);
                clearAll();
            }
        });

        findViewById(R.id.btn_delete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playClickFeedback(v, 2);
                performDelete();
            }
        });

        findViewById(R.id.btn_toggle_sign).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playClickFeedback(v, 2);
                toggleSign();
            }
        });

        findViewById(R.id.btn_percent).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playClickFeedback(v, 2);
                applyPercent();
            }
        });

        // Basic functional operators
        findViewById(R.id.btn_add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playClickFeedback(v, 2);
                applyOperator("+");
            }
        });

        findViewById(R.id.btn_subtract).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playClickFeedback(v, 2);
                applyOperator("−");
            }
        });

        findViewById(R.id.btn_multiply).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playClickFeedback(v, 2);
                applyOperator("×");
            }
        });

        findViewById(R.id.btn_divide).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playClickFeedback(v, 2);
                applyOperator("÷");
            }
        });

        // Compute results
        findViewById(R.id.btn_equals).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playClickFeedback(v, 3);
                evaluate();
            }
        });
    }

    private void setupHistoryAndLoader() {
        // App loader setup: Fade-out animation timer
        final View loader = findViewById(R.id.loader_screen);
        if (loader != null) {
            loader.postDelayed(new Runnable() {
                @Override
                public void run() {
                    AlphaAnimation fadeOut = new AlphaAnimation(1.0f, 0.0f);
                    fadeOut.setDuration(600);
                    fadeOut.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {}

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            loader.setVisibility(View.GONE);
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {}
                    });
                    loader.startAnimation(fadeOut);
                }
            }, 2000);
        }

        // Toggle history panels
        findViewById(R.id.btn_open_history).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playClickFeedback(v, 2);
                showHistory();
            }
        });

        findViewById(R.id.btn_close_history).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playClickFeedback(v, 2);
                hideHistory();
            }
        });

        findViewById(R.id.btn_clear_history).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playClickFeedback(v, 2);
                clearHistory();
            }
        });
    }

    // Audio synthesizer & tactile tap feedback module
    private void playClickFeedback(View view, int level) {
        // 1. Tactile trigger
        try {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        } catch (Exception ignored) {}

        // 2. Interactive visual click animation
        ScaleAnimation scaleAnim = new ScaleAnimation(
                1.0f, 0.92f, 1.0f, 0.92f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        scaleAnim.setDuration(100);
        scaleAnim.setInterpolator(new DecelerateInterpolator());
        view.startAnimation(scaleAnim);

        // 3. Synth Audio feedback using system ToneGenerator
        try {
            if (toneGenerator == null) {
                toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 65);
            }
            if (level == 1) {
                toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 40);
            } else if (level == 2) {
                toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP2, 60);
            } else {
                toneGenerator.startTone(ToneGenerator.TONE_DTMF_D, 120);
            }
        } catch (Exception ignored) {}
    }

    private void appendNumber(String num) {
        if (hasEvaluated || isOperatorJustPressed) {
            currentInput = "";
            hasEvaluated = false;
            isOperatorJustPressed = false;
        }

        // Character ceiling constraint
        if (currentInput.length() >= 15) {
            return;
        }

        if (currentInput.equals("0")) {
            currentInput = num;
        } else {
            currentInput += num;
        }

        updateDisplay(currentInput);
    }

    private void appendDecimal() {
        if (hasEvaluated || isOperatorJustPressed) {
            currentInput = "0";
            hasEvaluated = false;
            isOperatorJustPressed = false;
        }

        if (!currentInput.contains(".")) {
            if (currentInput.isEmpty()) {
                currentInput = "0.";
            } else {
                currentInput += ".";
            }
            updateDisplay(currentInput);
        }
    }

    private void clearAll() {
        currentInput = "";
        firstOperand = Double.NaN;
        activeOperator = "";
        isOperatorJustPressed = false;
        hasEvaluated = false;
        tvFormula.setText("");
        tvDisplay.setText("0");
    }

    private void performDelete() {
        if (hasEvaluated) {
            tvFormula.setText("");
            return;
        }
        if (currentInput.length() > 0) {
            currentInput = currentInput.substring(0, currentInput.length() - 1);
            if (currentInput.isEmpty() || currentInput.equals("-")) {
                currentInput = "0";
            }
            updateDisplay(currentInput);
        }
    }

    private void toggleSign() {
        if (currentInput.isEmpty() || currentInput.equals("0")) {
            return;
        }
        try {
            double val = Double.parseDouble(currentInput);
            val = val * -1;
            currentInput = formatValue(val);
            updateDisplay(currentInput);
        } catch (NumberFormatException ignored) {}
    }

    private void applyPercent() {
        if (currentInput.isEmpty()) {
            return;
        }
        try {
            double val = Double.parseDouble(currentInput);
            val = val / 100.0;
            currentInput = formatValue(val);
            updateDisplay(currentInput);
        } catch (NumberFormatException ignored) {}
    }

    private void applyOperator(String op) {
        try {
            if (!Double.isNaN(firstOperand) && !isOperatorJustPressed && !currentInput.isEmpty()) {
                evaluateIntermediate();
            } else if (!currentInput.isEmpty()) {
                firstOperand = Double.parseDouble(currentInput);
            } else if (Double.isNaN(firstOperand)) {
                firstOperand = 0;
            }

            activeOperator = op;
            isOperatorJustPressed = true;
            hasEvaluated = false;

            tvFormula.setText(formatValue(firstOperand) + " " + activeOperator);
        } catch (NumberFormatException ignored) {}
    }

    private void evaluateIntermediate() {
        if (Double.isNaN(firstOperand) || currentInput.isEmpty()) {
            return;
        }
        try {
            double secondOperand = Double.parseDouble(currentInput);
            double result = compute(firstOperand, secondOperand, activeOperator);
            firstOperand = result;
            updateDisplay(formatValue(result));
        } catch (NumberFormatException ignored) {}
    }

    private void evaluate() {
        if (Double.isNaN(firstOperand) || currentInput.isEmpty() || activeOperator.isEmpty()) {
            return;
        }
        try {
            double secondOperand = Double.parseDouble(currentInput);
            double result = compute(firstOperand, secondOperand, activeOperator);

            String formulaText = formatValue(firstOperand) + " " + activeOperator + " " + formatValue(secondOperand);
            tvFormula.setText(formulaText + " =");

            // Evaluate screen animations
            AlphaAnimation textFlash = new AlphaAnimation(0.3f, 1.0f);
            textFlash.setDuration(250);
            tvDisplay.startAnimation(textFlash);

            if (Double.isInfinite(result) || Double.isNaN(result)) {
                tvDisplay.setText("Error");
                currentInput = "";
                firstOperand = Double.NaN;
                activeOperator = "";
            } else {
                String resultText = formatValue(result);
                currentInput = resultText;
                updateDisplay(currentInput);
                
                // Save computation parameters to SharedPreferences History
                saveToHistory(formulaText, resultText);
                
                firstOperand = result;
            }

            hasEvaluated = true;
            isOperatorJustPressed = false;
        } catch (NumberFormatException ignored) {}
    }

    private double compute(double op1, double op2, String operator) {
        if (operator.equals("+")) {
            return op1 + op2;
        } else if (operator.equals("−")) {
            return op1 - op2;
        } else if (operator.equals("×")) {
            return op1 * op2;
        } else if (operator.equals("÷")) {
            if (op2 == 0) {
                return Double.NaN;
            }
            return op1 / op2;
        }
        return op2;
    }

    private String formatValue(double value) {
        if (Double.isNaN(value)) {
            return "Error";
        }
        if (value == (long) value) {
            return String.format(Locale.US, "%d", (long) value);
        } else {
            return decimalFormat.format(value);
        }
    }

    private void updateDisplay(String text) {
        tvDisplay.setText(text);
    }

    // Calculation Persistence Module
    private void saveToHistory(String formula, String result) {
        if (formula == null || formula.isEmpty() || result == null || result.isEmpty() || result.equals("Error")) {
            return;
        }
        SharedPreferences prefs = getSharedPreferences("calc_history", MODE_PRIVATE);
        String savedLogs = prefs.getString("items", "");
        String item = formula + " = " + result;

        if (savedLogs.isEmpty()) {
            savedLogs = item;
        } else {
            savedLogs = item + "##" + savedLogs;
        }

        // Cap stored logs list size to 50
        String[] list = savedLogs.split("##");
        if (list.length > 50) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 50; i++) {
                if (i > 0) sb.append("##");
                sb.append(list[i]);
            }
            savedLogs = sb.toString();
        }

        prefs.edit().putString("items", savedLogs).apply();
    }

    private void showHistory() {
        final View panel = findViewById(R.id.history_panel);
        if (panel != null && panel.getVisibility() != View.VISIBLE) {
            panel.setVisibility(View.VISIBLE);
            
            // Slide-up drawer animation
            TranslateAnimation slideUp = new TranslateAnimation(
                    0, 0, 1200, 0
            );
            slideUp.setDuration(350);
            panel.startAnimation(slideUp);
            loadHistoryView();
        }
    }

    private void hideHistory() {
        final View panel = findViewById(R.id.history_panel);
        if (panel != null && panel.getVisibility() == View.VISIBLE) {
            TranslateAnimation slideDown = new TranslateAnimation(
                    0, 0, 0, 1200
            );
            slideDown.setDuration(300);
            slideDown.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {}

                @Override
                public void onAnimationEnd(Animation animation) {
                    panel.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {}
            });
            panel.startAnimation(slideDown);
        }
    }

    private void clearHistory() {
        SharedPreferences prefs = getSharedPreferences("calc_history", MODE_PRIVATE);
        prefs.edit().clear().apply();
        loadHistoryView();
    }

    private void loadHistoryView() {
        LinearLayout container = (LinearLayout) findViewById(R.id.history_list_container);
        if (container == null) return;
        container.removeAllViews();

        SharedPreferences prefs = getSharedPreferences("calc_history", MODE_PRIVATE);
        String rawLogs = prefs.getString("items", "");

        if (rawLogs.isEmpty()) {
            TextView emptyMsg = new TextView(this);
            emptyMsg.setText("No recent calculations found.");
            emptyMsg.setTextColor(getResources().getColor(R.color.text_display_secondary));
            emptyMsg.setGravity(Gravity.CENTER);
            emptyMsg.setPadding(0, 80, 0, 0);
            emptyMsg.setTextSize(16sp);
            container.addView(emptyMsg);
            return;
        }

        String[] logs = rawLogs.split("##");
        for (int i = 0; i < logs.length; i++) {
            final String record = logs[i];

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(16, 24, 16, 24);
            row.setClickable(true);
            row.setFocusable(true);

            TextView tv = new TextView(this);
            tv.setText(record);
            tv.setTextColor(getResources().getColor(R.color.text_white));
            tv.setTextSize(17sp);

            // Click log entry to restore calculated value to output screen
            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    playClickFeedback(v, 1);
                    String[] parts = record.split("=");
                    if (parts.length == 2) {
                        String restoredValue = parts[1].trim();
                        if (!restoredValue.equals("Error")) {
                            currentInput = restoredValue;
                            updateDisplay(currentInput);
                            hasEvaluated = true; 
                            hideHistory();
                        }
                    }
                }
            });

            View divider = new View(this);
            divider.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 2));
            divider.setBackgroundColor(0x18FFFFFF);

            row.addView(tv);
            container.addView(row);
            container.addView(divider);
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