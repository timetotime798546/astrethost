package com.simplecalculator.app;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends Activity {

    private LinearLayout rootLayout;
    private LinearLayout layoutToolbar;
    private LinearLayout layoutDisplay;
    private LinearLayout layoutHistoryPanel;
    private LinearLayout layoutKeyboard;
    private LinearLayout llHistoryContainer;

    private Button btnToggleTheme;
    private Button btnToggleSound;
    private Button btnToggleHistory;
    private Button btnClearHistory;

    private TextView tvFormula;
    private TextView tvDisplay;
    private TextView tvHistoryHeader;

    private StringBuilder currentInput = new StringBuilder();
    private Double operand1 = null;
    private String pendingOperator = null;
    private boolean isNewOp = false;

    // Sound and Theme states
    private boolean isSoundOn = true;
    private boolean isDarkMode = true;
    private boolean isHistoryVisible = false;
    private ToneGenerator toneGenerator;
    private ArrayList<String> calculationHistory = new ArrayList<>();
    private static final String PREFS_NAME = "CalcPrefs";
    private static final String KEY_SOUND = "SoundSetting";
    private static final String KEY_THEME = "ThemeSetting";
    private static final String KEY_HISTORY = "HistoryItems";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind views
        rootLayout = (LinearLayout) findViewById(R.id.rootLayout);
        layoutToolbar = (LinearLayout) findViewById(R.id.layoutToolbar);
        layoutDisplay = (LinearLayout) findViewById(R.id.layoutDisplay);
        layoutHistoryPanel = (LinearLayout) findViewById(R.id.layoutHistoryPanel);
        layoutKeyboard = (LinearLayout) findViewById(R.id.layoutKeyboard);
        llHistoryContainer = (LinearLayout) findViewById(R.id.llHistoryContainer);

        btnToggleTheme = (Button) findViewById(R.id.btnToggleTheme);
        btnToggleSound = (Button) findViewById(R.id.btnToggleSound);
        btnToggleHistory = (Button) findViewById(R.id.btnToggleHistory);
        btnClearHistory = (Button) findViewById(R.id.btnClearHistory);

        tvFormula = (TextView) findViewById(R.id.tvFormula);
        tvDisplay = (TextView) findViewById(R.id.tvDisplay);
        tvHistoryHeader = (TextView) findViewById(R.id.tvHistoryHeader);

        // Load preferences
        loadPreferences();

        // Initialize sound generator safely
        try {
            toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 70);
        } catch (Exception e) {
            toneGenerator = null;
        }

        setupButtonListeners();
        setupFunctionalToggles();
        applyThemeStyles();
        renderHistoryList();
    }

    private void loadPreferences() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        isSoundOn = prefs.getBoolean(KEY_SOUND, true);
        isDarkMode = prefs.getBoolean(KEY_THEME, true);

        Set<String> savedSet = prefs.getStringSet(KEY_HISTORY, null);
        if (savedSet != null) {
            calculationHistory = new ArrayList<>(savedSet);
        }
    }

    private void savePreferences() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_SOUND, isSoundOn);
        editor.putBoolean(KEY_THEME, isDarkMode);
        Set<String> set = new HashSet<>(calculationHistory);
        editor.putStringSet(KEY_HISTORY, set);
        editor.apply();
    }

    private void playFeedbackSound() {
        if (isSoundOn && toneGenerator != null) {
            try {
                toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 75);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void setupFunctionalToggles() {
        // Sound ON/OFF
        btnToggleSound.setText(isSoundOn ? "🔊 SOUND: ON" : "🔇 SOUND: OFF");
        btnToggleSound.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isSoundOn = !isSoundOn;
                btnToggleSound.setText(isSoundOn ? "🔊 SOUND: ON" : "🔇 SOUND: OFF");
                playFeedbackSound();
                savePreferences();
            }
        });

        // Light/Dark Theme
        btnToggleTheme.setText(isDarkMode ? "🌙 DARK" : "☀️ LIGHT");
        btnToggleTheme.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isDarkMode = !isDarkMode;
                btnToggleTheme.setText(isDarkMode ? "🌙 DARK" : "☀️ LIGHT");
                playFeedbackSound();
                applyThemeStyles();
                savePreferences();
            }
        });

        // History visibility toggle
        btnToggleHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playFeedbackSound();
                isHistoryVisible = !isHistoryVisible;
                if (isHistoryVisible) {
                    layoutHistoryPanel.setVisibility(View.VISIBLE);
                    // set history panel layout weight
                    LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.2f);
                    layoutHistoryPanel.setLayoutParams(param);
                } else {
                    layoutHistoryPanel.setVisibility(View.GONE);
                    LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 0, 0f);
                    layoutHistoryPanel.setLayoutParams(param);
                }
            }
        });

        // Clear History button
        btnClearHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playFeedbackSound();
                calculationHistory.clear();
                renderHistoryList();
                savePreferences();
            }
        });
    }

    private void applyThemeStyles() {
        int bgColor = isDarkMode ? Color.parseColor("#1C1C1E") : Color.parseColor("#F2F2F7");
        int displayBgColor = isDarkMode ? Color.parseColor("#2C2C2E") : Color.parseColor("#FFFFFF");
        int displayTextColor = isDarkMode ? Color.WHITE : Color.BLACK;
        int formulaTextColor = isDarkMode ? Color.parseColor("#AEAEB2") : Color.parseColor("#8E8E93");
        int digitBtnColor = isDarkMode ? Color.parseColor("#3A3A3C") : Color.parseColor("#E5E5EA");
        int digitTextColor = isDarkMode ? Color.WHITE : Color.BLACK;
        int utilityBtnColor = isDarkMode ? Color.parseColor("#2C2C2E") : Color.parseColor("#D1D1D6");
        int utilityTextColor = isDarkMode ? Color.WHITE : Color.BLACK;

        rootLayout.setBackgroundColor(bgColor);
        layoutDisplay.setBackgroundColor(displayBgColor);
        layoutHistoryPanel.setBackgroundColor(displayBgColor);

        tvDisplay.setTextColor(displayTextColor);
        tvFormula.setTextColor(formulaTextColor);
        tvHistoryHeader.setTextColor(displayTextColor);

        // Stylize toggle buttons
        btnToggleTheme.setBackgroundColor(utilityBtnColor);
        btnToggleTheme.setTextColor(utilityTextColor);
        btnToggleSound.setBackgroundColor(utilityBtnColor);
        btnToggleSound.setTextColor(utilityTextColor);
        btnToggleHistory.setBackgroundColor(utilityBtnColor);
        btnToggleHistory.setTextColor(utilityTextColor);
        btnClearHistory.setBackgroundColor(bgColor);

        // Standard digits list
        int[] digits = {
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9, R.id.btn00, R.id.btnDot
        };
        for (int i = 0; i < digits.length; i++) {
            Button btn = (Button) findViewById(digits[i]);
            if (btn != null) {
                btn.setBackgroundColor(digitBtnColor);
                btn.setTextColor(digitTextColor);
            }
        }

        // Utility operators
        int[] utilities = { R.id.btnClear, R.id.btnDelete, R.id.btnPercent };
        for (int i = 0; i < utilities.length; i++) {
            Button btn = (Button) findViewById(utilities[i]);
            if (btn != null) {
                btn.setBackgroundColor(digitBtnColor);
            }
        }

        // Re-render historical dynamic text colors
        renderHistoryList();
    }

    private void renderHistoryList() {
        llHistoryContainer.removeAllViews();
        int textColor = isDarkMode ? Color.parseColor("#D1D1D6") : Color.parseColor("#3A3A3C");
        
        if (calculationHistory.isEmpty()) {
            TextView emptyTv = new TextView(this);
            emptyTv.setText("No calculation history yet.");
            emptyTv.setPadding(0, 10, 0, 10);
            emptyTv.setTextColor(textColor);
            llHistoryContainer.addView(emptyTv);
            return;
        }

        for (int i = calculationHistory.size() - 1; i >= 0; i--) {
            final String record = calculationHistory.get(i);
            TextView rowTv = new TextView(this);
            rowTv.setText(record);
            rowTv.setPadding(0, 8, 0, 8);
            rowTv.setTextSize(14sp);
            rowTv.setTextColor(textColor);
            rowTv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    playFeedbackSound();
                    // Restore formulation result on touch
                    String[] split = record.split("=");
                    if (split.length > 0) {
                        String resultVal = split[split.length - 1].trim();
                        currentInput.setLength(0);
                        currentInput.append(resultVal);
                        tvDisplay.setText(resultVal);
                    }
                }
            });
            llHistoryContainer.addView(rowTv);
        }
    }

    private void saveCalculationRecord(String formula, String result) {
        String item = formula + " = " + result;
        calculationHistory.add(item);
        if (calculationHistory.size() > 30) {
            calculationHistory.remove(0); // Keep max 30 records
        }
        renderHistoryList();
        savePreferences();
    }

    private void setupButtonListeners() {
        int[] digitButtons = {
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9,
            R.id.btn00
        };

        for (int i = 0; i < digitButtons.length; i++) {
            int id = digitButtons[i];
            final Button btn = (Button) findViewById(id);
            if (btn != null) {
                btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        playFeedbackSound();
                        onDigitPressed(btn.getText().toString());
                    }                });
            }
        }

        int[] operatorButtons = {
            R.id.btnAdd, R.id.btnSubtract, R.id.btnMultiply, R.id.btnDivide
        };

        for (int i = 0; i < operatorButtons.length; i++) {
            int id = operatorButtons[i];
            final Button btn = (Button) findViewById(id);
            if (btn != null) {
                btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        playFeedbackSound();
                        onOperatorPressed(btn.getText().toString());
                    }
                });
            }
        }

        Button btnDot = (Button) findViewById(R.id.btnDot);
        if (btnDot != null) {
            btnDot.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    playFeedbackSound();
                    onDotPressed();
                }
            });
        }

        Button btnClear = (Button) findViewById(R.id.btnClear);
        if (btnClear != null) {
            btnClear.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    playFeedbackSound();
                    onClearPressed();
                }
            });
        }

        Button btnDelete = (Button) findViewById(R.id.btnDelete);
        if (btnDelete != null) {
            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    playFeedbackSound();
                    onDeletePressed();
                }
            });
        }

        Button btnPercent = (Button) findViewById(R.id.btnPercent);
        if (btnPercent != null) {
            btnPercent.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    playFeedbackSound();
                    onPercentPressed();
                }
            });
        }

        Button btnEqual = (Button) findViewById(R.id.btnEqual);
        if (btnEqual != null) {
            btnEqual.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    playFeedbackSound();
                    onEqualPressed();
                }
            });
        }
    }

    private void onDigitPressed(String digit) {
        if (isNewOp) {
            currentInput.setLength(0);
            isNewOp = false;
        }
        
        if (currentInput.toString().equals("0")) {
            if (digit.equals("0") || digit.equals("00")) {
                return;
            } else {
                currentInput.setLength(0);
            }
        }
        
        currentInput.append(digit);
        tvDisplay.setText(currentInput.toString());
    }

    private void onDotPressed() {
        if (isNewOp) {
            currentInput.setLength(0);
            currentInput.append("0");
            isNewOp = false;
        }
        if (currentInput.length() == 0) {
            currentInput.append("0");
        }
        if (currentInput.indexOf(".") == -1) {
            currentInput.append(".");
        }
        tvDisplay.setText(currentInput.toString());
    }

    private void onClearPressed() {
        currentInput.setLength(0);
        operand1 = null;
        pendingOperator = null;
        isNewOp = false;
        tvDisplay.setText("0");
        tvFormula.setText("");
    }

    private void onDeletePressed() {
        if (isNewOp) {
            tvFormula.setText("");
            return;
        }
        if (currentInput.length() > 0) {
            currentInput.setLength(currentInput.length() - 1);
            if (currentInput.length() == 0) {
                tvDisplay.setText("0");
            } else {
                tvDisplay.setText(currentInput.toString());
            }
        }
    }

    private void onPercentPressed() {
        if (currentInput.length() > 0) {
            try {
                double val = Double.parseDouble(currentInput.toString()) / 100.0;
                String inputStr = currentInput.toString();
                currentInput.setLength(0);
                currentInput.append(formatValue(val));
                tvDisplay.setText(currentInput.toString());
                saveCalculationRecord(inputStr + " / 100", formatValue(val));
            } catch (NumberFormatException e) {
                tvDisplay.setText("Error");
            }
        }
    }

    private void onOperatorPressed(String operator) {
        if (currentInput.length() == 0) {
            if (operand1 != null) {
                pendingOperator = operator;
                tvFormula.setText(formatValue(operand1) + " " + pendingOperator);
            }
            return;
        }

        try {
            double currentVal = Double.parseDouble(currentInput.toString());
            if (operand1 == null) {
                operand1 = currentVal;
            } else if (pendingOperator != null) {
                double result = calculate(operand1, currentVal, pendingOperator);
                if (Double.isNaN(result)) {
                    tvDisplay.setText("Error");
                    onClearPressed();
                    return;
                }
                operand1 = result;
                tvDisplay.setText(formatValue(operand1));
            }
            pendingOperator = operator;
            tvFormula.setText(formatValue(operand1) + " " + pendingOperator);
            currentInput.setLength(0);
            isNewOp = false;
        } catch (NumberFormatException e) {
            tvDisplay.setText("Error");
        }
    }

    private void onEqualPressed() {
        if (pendingOperator == null || currentInput.length() == 0) {
            return;
        }

        try {
            double operand2 = Double.parseDouble(currentInput.toString());
            double result = calculate(operand1, operand2, pendingOperator);
            if (Double.isNaN(result)) {
                tvDisplay.setText("Error");
                onClearPressed();
                return;
            }
            String formula = formatValue(operand1) + " " + pendingOperator + " " + formatValue(operand2);
            String formattedResult = formatValue(result);
            
            tvFormula.setText(formula + " =");
            tvDisplay.setText(formattedResult);
            
            saveCalculationRecord(formula, formattedResult);
            
            operand1 = result;
            currentInput.setLength(0);
            currentInput.append(formattedResult);
            pendingOperator = null;
            isNewOp = true;
        } catch (NumberFormatException e) {
            tvDisplay.setText("Error");
        }
    }

    private double calculate(double op1, double op2, String operator) {
        if (operator.equals("+")) return op1 + op2;
        if (operator.equals("-")) return op1 - op2;
        if (operator.equals("x")) return op1 * op2;
        if (operator.equals("/")) {
            if (op2 == 0) return Double.NaN;
            return op1 / op2;
        }
        return op2;
    }

    private String formatValue(double value) {
        if (value == (long) value) {
            return String.format("%d", (long) value);
        } else {
            return String.valueOf(value);
        }
    }

    @Override
    protected void onDestroy() {
        if (toneGenerator != null) {
            toneGenerator.release();
        }
        super.onDestroy();
    }
}