package com.moderncalculator.app;

import android.app.Activity;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {

    private TextView tvExpression;
    private TextView tvResult;
    private StringBuilder currentExpression;
    private boolean isResultDisplayed;

    // Loader and History Views
    private View layoutLoader;
    private View layoutHistory;
    private LinearLayout llHistoryItems;

    // Tactile Audio click feedback engine
    private ToneGenerator toneGenerator;

    // History log list values
    private ArrayList<String> historyList;
    private static final String PREFS_NAME = "ModernCalcHistoryPrefs";
    private static final String HISTORY_KEY = "calc_history_data";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvExpression = (TextView) findViewById(R.id.tvExpression);
        tvResult = (TextView) findViewById(R.id.tvResult);
        currentExpression = new StringBuilder();
        isResultDisplayed = false;

        // Overlay element bindings
        layoutLoader = findViewById(R.id.layoutLoader);
        layoutHistory = findViewById(R.id.layoutHistory);
        llHistoryItems = (LinearLayout) findViewById(R.id.llHistoryItems);

        // Sound configuration
        try {
            toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 65);
        } catch (Exception e) {
            toneGenerator = null;
        }

        // Initialize and load historical entries
        historyList = loadSavedHistory();

        setupButtons();
    }

    private void playClickSound() {
        if (toneGenerator != null) {
            try {
                toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 35);
            } catch (Exception e) {
                // Fail-safe
            }
        }
    }

    private void setupButtons() {
        // Map numeric click handlers sequentially
        int[] numericIds = {
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9
        };
        for (int i = 0; i < numericIds.length; i++) {
            final String numStr = String.valueOf(i);
            findViewById(numericIds[i]).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    playClickSound();
                    onNumericClick(numStr);
                }
            });
        }

        // Dot operator click handler
        findViewById(R.id.btnDot).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playClickSound();
                onDotClick();
            }
        });

        // Main operations click handlers
        findViewById(R.id.btnPlus).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playClickSound();
                onOperatorClick("+");
            }
        });
        findViewById(R.id.btnSubtract).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playClickSound();
                onOperatorClick("-");
            }
        });
        findViewById(R.id.btnMultiply).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playClickSound();
                onOperatorClick("*");
            }
        });
        findViewById(R.id.btnDivide).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playClickSound();
                onOperatorClick("/");
            }
        });

        // Special / Helper buttons handlers
        findViewById(R.id.btnC).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playClickSound();
                onClearClick();
            }
        });
        findViewById(R.id.btnBackspace).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playClickSound();
                onBackspaceClick();
            }
        });
        findViewById(R.id.btnPercent).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playClickSound();
                onPercentClick();
            }
        });
        findViewById(R.id.btnToggleSign).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playClickSound();
                onToggleSignClick();
            }
        });
        findViewById(R.id.btnEqual).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onEqualClick();
            }
        });

        // History Menu triggers
        findViewById(R.id.btnShowHistory).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playClickSound();
                populateHistoryLayout();
                layoutHistory.setVisibility(View.VISIBLE);
            }
        });

        findViewById(R.id.btnCloseHistory).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playClickSound();
                layoutHistory.setVisibility(View.GONE);
            }
        });

        findViewById(R.id.btnClearHistory).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playClickSound();
                layoutLoader.setVisibility(View.VISIBLE);
                layoutLoader.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        historyList.clear();
                        persistHistory();
                        populateHistoryLayout();
                        layoutLoader.setVisibility(View.GONE);
                    }
                }, 400);
            }
        });
    }

    private void onNumericClick(String digit) {
        if (isResultDisplayed) {
            currentExpression.setLength(0);
            isResultDisplayed = false;
        }
        currentExpression.append(digit);
        updateDisplay();
    }

    private void onOperatorClick(String op) {
        isResultDisplayed = false;
        if (currentExpression.length() == 0) {
            if (op.equals("-")) {
                currentExpression.append(op);
            }
        } else {
            char lastChar = currentExpression.charAt(currentExpression.length() - 1);
            if (isOperator(lastChar)) {
                currentExpression.setLength(currentExpression.length() - 1);
                currentExpression.append(op);
            } else {
                currentExpression.append(op);
            }
        }
        updateDisplay();
    }

    private void onDotClick() {
        if (isResultDisplayed) {
            currentExpression.setLength(0);
            isResultDisplayed = false;
        }

        if (currentExpression.length() == 0) {
            currentExpression.append("0.");
        } else {
            int lastOpIndex = -1;
            for (int i = currentExpression.length() - 1; i >= 0; i--) {
                char c = currentExpression.charAt(i);
                if (isOperator(c)) {
                    lastOpIndex = i;
                    break;
                }
            }
            String lastNumber = currentExpression.substring(lastOpIndex + 1);
            if (!lastNumber.contains(".")) {
                currentExpression.append(".");
            }
        }
        updateDisplay();
    }

    private void onClearClick() {
        currentExpression.setLength(0);
        isResultDisplayed = false;
        tvResult.setText("0");
        tvExpression.setText("");
    }

    private void onBackspaceClick() {
        isResultDisplayed = false;
        if (currentExpression.length() > 0) {
            currentExpression.setLength(currentExpression.length() - 1);
        }
        updateDisplay();
    }

    private void onPercentClick() {
        if (currentExpression.length() > 0) {
            char lastChar = currentExpression.charAt(currentExpression.length() - 1);
            if (!isOperator(lastChar) && lastChar != '%') {
                currentExpression.append("%");
                updateDisplay();
            }
        }
    }

    private void onToggleSignClick() {
        if (currentExpression.length() > 0) {
            try {
                String exprStr = currentExpression.toString();
                double val = Double.parseDouble(exprStr);
                val = val * -1;
                currentExpression.setLength(0);
                currentExpression.append(formatNumber(val));
            } catch (NumberFormatException e) {
                String exprStr = currentExpression.toString();
                if (exprStr.startsWith("-(") && exprStr.endsWith(")")) {
                    currentExpression.setLength(0);
                    currentExpression.append(exprStr.substring(2, exprStr.length() - 1));
                } else {
                    currentExpression.insert(0, "-(");
                    currentExpression.append(")");
                }
            }
            updateDisplay();
        } else {
            currentExpression.append("-");
            updateDisplay();
        }
    }

    private void onEqualClick() {
        if (currentExpression.length() > 0) {
            playClickSound();

            // Bring up loader overlay to display computational validation steps
            layoutLoader.setVisibility(View.VISIBLE);
            final String originalExpr = currentExpression.toString();

            layoutLoader.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        double finalResult = evaluateExpression(originalExpr);
                        String formattedResult = formatNumber(finalResult);

                        String dispExp = getDisplayString(originalExpr);
                        tvExpression.setText(dispExp);
                        tvResult.setText(formattedResult);

                        // Save calculation to persistent storage
                        saveHistoryItem(dispExp, formattedResult);

                        currentExpression.setLength(0);
                        currentExpression.append(formattedResult);
                        isResultDisplayed = true;
                    } catch (Exception e) {
                        tvResult.setText("Error");
                        isResultDisplayed = true;
                    } finally {
                        layoutLoader.setVisibility(View.GONE);
                    }
                }
            }, 350); // Transient loader execution delay
        }
    }

    private void updateDisplay() {
        String expr = currentExpression.toString();
        if (expr.isEmpty()) {
            tvResult.setText("0");
            tvExpression.setText("");
        } else {
            tvResult.setText(getDisplayString(expr));
            try {
                boolean hasOperator = false;
                for (int i = 0; i < expr.length(); i++) {
                    char c = expr.charAt(i);
                    if (isOperator(c) || c == '%') {
                        hasOperator = true;
                        break;
                    }
                }
                if (hasOperator) {
                    double preview = evaluateExpression(expr);
                    tvExpression.setText(getDisplayString(expr));
                    tvResult.setText(formatNumber(preview));
                } else {
                    tvExpression.setText("");
                }
            } catch (Exception e) {
                // Display input text unmodified in case processing inputs are temporary
            }
        }
    }

    private double evaluateExpression(String expr) {
        while (expr.length() > 0 && isOperator(expr.charAt(expr.length() - 1))) {
            expr = expr.substring(0, expr.length() - 1);
        }
        if (expr.isEmpty()) {
            return 0;
        }
        return ExpressionEvaluator.evaluate(expr);
    }

    private boolean isOperator(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/';
    }

    private String getDisplayString(String expr) {
        return expr.replace("*", "×").replace("/", "÷").replace("-", "−");
    }

    private String formatNumber(double value) {
        if (Double.isInfinite(value) || Double.isNaN(value)) {
            return "Error";
        }
        if (value == (long) value) {
            return String.format(Locale.US, "%d", (long) value);
        } else {
            DecimalFormat df = new DecimalFormat("#.##########", new DecimalFormatSymbols(Locale.US));
            return df.format(value);
        }
    }

    // ==========================================
    // PERSISTENCE ENGINE (HISTORY TRACKER)
    // ==========================================

    private void saveHistoryItem(String expression, String result) {
        String logRecord = expression + " = " + result;
        historyList.add(0, logRecord); // Push on top

        // Store max 50 calculations
        if (historyList.size() > 50) {
            historyList.remove(historyList.size() - 1);
        }
        persistHistory();
    }

    private void persistHistory() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < historyList.size(); i++) {
            sb.append(historyList.get(i));
            if (i < historyList.size() - 1) {
                sb.append("##"); // Delimiter element
            }
        }
        editor.putString(HISTORY_KEY, sb.toString());
        editor.apply();
    }

    private ArrayList<String> loadSavedHistory() {
        ArrayList<String> list = new ArrayList<>();
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String raw = prefs.getString(HISTORY_KEY, "");
        if (!raw.isEmpty()) {
            String[] items = raw.split("##");
            for (int i = 0; i < items.length; i++) {
                String item = items[i];
                if (!item.trim().isEmpty()) {
                    list.add(item);
                }
            }
        }
        return list;
    }

    private void populateHistoryLayout() {
        llHistoryItems.removeAllViews();
        if (historyList.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("No history records yet");
            tvEmpty.setTextColor(0xFF8E8E93);
            tvEmpty.setTextSize(16);
            tvEmpty.setGravity(android.view.Gravity.CENTER);
            tvEmpty.setPadding(0, 50, 0, 50);
            llHistoryItems.addView(tvEmpty);
        } else {
            for (int i = 0; i < historyList.size(); i++) {
                final String item = historyList.get(i);

                // Container layout per item
                LinearLayout itemContainer = new LinearLayout(this);
                itemContainer.setOrientation(LinearLayout.VERTICAL);
                itemContainer.setPadding(10, 16, 10, 16);
                itemContainer.setClickable(true);
                itemContainer.setFocusable(true);

                TextView tvItem = new TextView(this);
                tvItem.setText(item);
                tvItem.setTextColor(0xFFFFFFFF);
                tvItem.setTextSize(18);
                tvItem.setGravity(android.view.Gravity.END);

                itemContainer.addView(tvItem);

                // Reload selected history text back to main screen when tapped
                itemContainer.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        playClickSound();
                        restoreCalculation(item);
                        layoutHistory.setVisibility(View.GONE);
                    }
                });

                // Divider Line
                View divider = new View(this);
                LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1);
                dividerParams.topMargin = 4;
                divider.setLayoutParams(dividerParams);
                divider.setBackgroundColor(0xFF2C2C2C);

                llHistoryItems.addView(itemContainer);
                llHistoryItems.addView(divider);
            }
        }
    }

    private void restoreCalculation(String item) {
        int equalIndex = item.indexOf('=');
        if (equalIndex != -1) {
            String formula = item.substring(0, equalIndex).trim();
            // Re-convert UI math signs to parsing-friendly characters
            formula = formula.replace("×", "*").replace("÷", "/").replace("−", "-");
            currentExpression.setLength(0);
            currentExpression.append(formula);
            isResultDisplayed = false;
            updateDisplay();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (toneGenerator != null) {
            toneGenerator.release();
        }
    }

    // Dynamic mathematical recursive parser engine compatible with Java 8
    private static class ExpressionEvaluator {
        public static double evaluate(final String str) {
            return new Object() {
                int pos = -1, ch;

                void nextChar() {
                    ch = (++pos < str.length()) ? str.charAt(pos) : -1;
                }

                boolean eat(int charToEat) {
                    while (ch == ' ') nextChar();
                    if (ch == charToEat) {
                        nextChar();
                        return true;
                    }
                    return false;
                }

                double parse() {
                    nextChar();
                    double x = parseExpression();
                    if (pos < str.length()) throw new RuntimeException("Unexpected character: " + (char)ch);
                    return x;
                }

                double parseExpression() {
                    double x = parseTerm();
                    for (;;) {
                        if      (eat('+')) x += parseTerm();
                        else if (eat('-')) x -= parseTerm();
                        else return x;
                    }
                }

                double parseTerm() {
                    double x = parseFactor();
                    for (;;) {
                        if      (eat('*')) x *= parseFactor();
                        else if (eat('/')) {
                            double divisor = parseFactor();
                            if (divisor == 0) throw new ArithmeticException("Division by zero");
                            x /= divisor;
                        }
                        else return x;
                    }
                }

                double parseFactor() {
                    if (eat('+')) return parseFactor();
                    if (eat('-')) return -parseFactor();

                    double x;
                    int startPos = this.pos;
                    if (eat('(')) {
                        x = parseExpression();
                        eat(')');
                    } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                        while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                        x = Double.parseDouble(str.substring(startPos, this.pos));
                    } else {
                        throw new RuntimeException("Unexpected: " + (char)ch);
                    }

                    while (eat('%')) {
                        x = x / 100.0;
                    }

                    return x;
                }
            }.parse();
        }
    }
}