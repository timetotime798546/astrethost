package com.moderncalculator.app;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements View.OnClickListener {

    private TextView tvHistory;
    private TextView tvExpression;
    private TextView tvResultPreview;
    
    private String expression = "";
    private boolean isResultDisplayed = false;

    // Tone Generator and Audio Feedback features
    private ToneGenerator toneGenerator;
    private boolean isSoundEnabled = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize System Tone Feedback device
        try {
            toneGenerator = new ToneGenerator(AudioManager.STREAM_SYSTEM, 50);
        } catch (Exception e) {
            toneGenerator = null;
        }

        // Retrieve Sound Preference value
        SharedPreferences prefs = getSharedPreferences("calc_prefs", MODE_PRIVATE);
        isSoundEnabled = prefs.getBoolean("sound_enabled", true);

        // Bind Display Views
        tvHistory = (TextView) findViewById(R.id.tv_history);
        tvExpression = (TextView) findViewById(R.id.tv_expression);
        tvResultPreview = (TextView) findViewById(R.id.tv_result_preview);

        // Action Bar elements
        updateSoundToggleButton();
        findViewById(R.id.btn_sound_toggle).setOnClickListener(this);
        findViewById(R.id.btn_show_history).setOnClickListener(this);

        // History overlay triggers
        findViewById(R.id.btn_close_history).setOnClickListener(this);
        findViewById(R.id.btn_clear_history).setOnClickListener(this);

        // Bind Numeric and Operation triggers
        findViewById(R.id.btn_clear).setOnClickListener(this);
        findViewById(R.id.btn_paren).setOnClickListener(this);
        findViewById(R.id.btn_percent).setOnClickListener(this);
        findViewById(R.id.btn_divide).setOnClickListener(this);

        findViewById(R.id.btn_7).setOnClickListener(this);
        findViewById(R.id.btn_8).setOnClickListener(this);
        findViewById(R.id.btn_9).setOnClickListener(this);
        findViewById(R.id.btn_multiply).setOnClickListener(this);

        findViewById(R.id.btn_4).setOnClickListener(this);
        findViewById(R.id.btn_5).setOnClickListener(this);
        findViewById(R.id.btn_6).setOnClickListener(this);
        findViewById(R.id.btn_subtract).setOnClickListener(this);

        findViewById(R.id.btn_1).setOnClickListener(this);
        findViewById(R.id.btn_2).setOnClickListener(this);
        findViewById(R.id.btn_3).setOnClickListener(this);
        findViewById(R.id.btn_add).setOnClickListener(this);

        findViewById(R.id.btn_sign).setOnClickListener(this);
        findViewById(R.id.btn_0).setOnClickListener(this);
        findViewById(R.id.btn_decimal).setOnClickListener(this);
        findViewById(R.id.btn_equal).setOnClickListener(this);

        findViewById(R.id.btn_backspace).setOnClickListener(this);
        
        updateDisplay();
    }

    @Override
    public void onClick(View v) {
        playSound();
        int id = v.getId();

        if (id == R.id.btn_sound_toggle) {
            toggleSoundSetting();
        } else if (id == R.id.btn_show_history) {
            openHistoryPanel();
        } else if (id == R.id.btn_close_history) {
            closeHistoryPanel();
        } else if (id == R.id.btn_clear_history) {
            clearHistory();
        } else if (id == R.id.btn_clear) {
            clearAll();
        } else if (id == R.id.btn_backspace) {
            handleBackspace();
        } else if (id == R.id.btn_paren) {
            appendParenthesis();
        } else if (id == R.id.btn_percent) {
            appendOperator("%");
        } else if (id == R.id.btn_divide) {
            appendOperator("÷");
        } else if (id == R.id.btn_multiply) {
            appendOperator("×");
        } else if (id == R.id.btn_subtract) {
            appendOperator("-");
        } else if (id == R.id.btn_add) {
            appendOperator("+");
        } else if (id == R.id.btn_sign) {
            toggleSign();
        } else if (id == R.id.btn_decimal) {
            appendDecimal();
        } else if (id == R.id.btn_equal) {
            calculateFinalResult();
        } else {
            if (isResultDisplayed) {
                expression = "";
                isResultDisplayed = false;
            }
            if (id == R.id.btn_0) {
                expression += "0";
            } else if (id == R.id.btn_1) {
                expression += "1";
            } else if (id == R.id.btn_2) {
                expression += "2";
            } else if (id == R.id.btn_3) {
                expression += "3";
            } else if (id == R.id.btn_4) {
                expression += "4";
            } else if (id == R.id.btn_5) {
                expression += "5";
            } else if (id == R.id.btn_6) {
                expression += "6";
            } else if (id == R.id.btn_7) {
                expression += "7";
            } else if (id == R.id.btn_8) {
                expression += "8";
            } else if (id == R.id.btn_9) {
                expression += "9";
            }
            updateDisplay();
        }
    }

    private void toggleSoundSetting() {
        isSoundEnabled = !isSoundEnabled;
        SharedPreferences prefs = getSharedPreferences("calc_prefs", MODE_PRIVATE);
        prefs.edit().putBoolean("sound_enabled", isSoundEnabled).apply();
        updateSoundToggleButton();
    }

    private void updateSoundToggleButton() {
        TextView btnSound = (TextView) findViewById(R.id.btn_sound_toggle);
        if (isSoundEnabled) {
            btnSound.setText("🔊 Sound On");
        } else {
            btnSound.setText("🔇 Sound Off");
        }
    }

    private void playSound() {
        if (isSoundEnabled) {
            try {
                if (toneGenerator != null) {
                    toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 35);
                }
            } catch (Exception e) {
                // Suppress audio failure errors gracefully
            }
        }
    }

    private void openHistoryPanel() {
        populateHistoryUI();
        findViewById(R.id.layout_history_panel).setVisibility(View.VISIBLE);
    }

    private void closeHistoryPanel() {
        findViewById(R.id.layout_history_panel).setVisibility(View.GONE);
    }

    private void saveHistoryItem(String expressionStr, String resultStr) {
        SharedPreferences prefs = getSharedPreferences("calc_prefs", MODE_PRIVATE);
        String currentHistory = prefs.getString("history_log", "");
        String newItem = expressionStr + " = " + resultStr;
        
        if (currentHistory.isEmpty()) {
            currentHistory = newItem;
        } else {
            currentHistory = newItem + "\n" + currentHistory;
        }

        // Enforce maximum buffer layout depth of last 50 queries
        String[] array = currentHistory.split("\n");
        if (array.length > 50) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 50; i++) {
                if (i > 0) sb.append("\n");
                sb.append(array[i]);
            }
            currentHistory = sb.toString();
        }

        prefs.edit().putString("history_log", currentHistory).apply();
    }

    private List<String> loadHistory() {
        SharedPreferences prefs = getSharedPreferences("calc_prefs", MODE_PRIVATE);
        String historyStr = prefs.getString("history_log", "");
        List<String> list = new ArrayList<String>();
        if (!historyStr.isEmpty()) {
            String[] array = historyStr.split("\n");
            for (String s : array) {
                if (!s.trim().isEmpty()) {
                    list.add(s);
                }
            }
        }
        return list;
    }

    private void clearHistory() {
        getSharedPreferences("calc_prefs", MODE_PRIVATE).edit().remove("history_log").apply();
        populateHistoryUI();
    }

    private void populateHistoryUI() {
        LinearLayout container = (LinearLayout) findViewById(R.id.layout_history_items);
        container.removeAllViews();
        
        List<String> items = loadHistory();
        if (items.isEmpty()) {
            TextView emptyTv = new TextView(this);
            emptyTv.setText("No calculation history");
            emptyTv.setTextColor(getResources().getColor(R.color.text_secondary));
            emptyTv.setTextSize(16);
            emptyTv.setGravity(Gravity.CENTER);
            int pad = (int) (24 * getResources().getDisplayMetrics().density);
            emptyTv.setPadding(pad, pad, pad, pad);
            container.addView(emptyTv);
            return;
        }

        for (final String item : items) {
            final String[] parts = item.split(" = ");
            final String exprPart = parts[0];
            final String resPart = parts.length > 1 ? parts[1] : "";

            LinearLayout itemLayout = new LinearLayout(this);
            itemLayout.setOrientation(LinearLayout.VERTICAL);
            int padVertical = (int) (12 * getResources().getDisplayMetrics().density);
            int padHorizontal = (int) (8 * getResources().getDisplayMetrics().density);
            itemLayout.setPadding(padHorizontal, padVertical, padHorizontal, padVertical);
            itemLayout.setClickable(true);
            itemLayout.setFocusable(true);
            
            TypedValue outValue = new TypedValue();
            getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
            itemLayout.setBackgroundResource(outValue.resourceId);

            TextView tvExpr = new TextView(this);
            tvExpr.setText(exprPart);
            tvExpr.setTextColor(getResources().getColor(R.color.text_secondary));
            tvExpr.setTextSize(14);
            tvExpr.setGravity(Gravity.END);
            itemLayout.addView(tvExpr);

            TextView tvRes = new TextView(this);
            tvRes.setText(resPart);
            tvRes.setTextColor(getResources().getColor(R.color.text_primary));
            tvRes.setTextSize(20);
            tvRes.setGravity(Gravity.END);
            tvRes.setTypeface(Typeface.DEFAULT_BOLD);
            itemLayout.addView(tvRes);

            View divider = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1);
            lp.setMargins(0, (int) (6 * getResources().getDisplayMetrics().density), 0, (int) (6 * getResources().getDisplayMetrics().density));
            divider.setLayoutParams(lp);
            divider.setBackgroundColor(android.graphics.Color.parseColor("#2E2F38"));

            itemLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    playSound();
                    expression = exprPart;
                    isResultDisplayed = false;
                    updateDisplay();
                    closeHistoryPanel();
                }
            });

            container.addView(itemLayout);
            container.addView(divider);
        }
    }

    private void clearAll() {
        expression = "";
        tvHistory.setText("");
        isResultDisplayed = false;
        updateDisplay();
    }

    private void handleBackspace() {
        if (isResultDisplayed) {
            expression = "";
            isResultDisplayed = false;
        } else if (expression.length() > 0) {
            expression = expression.substring(0, expression.length() - 1);
        }
        updateDisplay();
    }

    private void appendParenthesis() {
        if (isResultDisplayed) {
            expression = "";
            isResultDisplayed = false;
        }
        int openCount = 0;
        int closeCount = 0;
        for (int i = 0; i < expression.length(); i++) {
            if (expression.charAt(i) == '(') openCount++;
            else if (expression.charAt(i) == ')') closeCount++;
        }
        if (openCount > closeCount) {
            char lastChar = expression.length() > 0 ? expression.charAt(expression.length() - 1) : '\0';
            if (Character.isDigit(lastChar) || lastChar == ')' || lastChar == '%') {
                expression += ")";
            }
            else {
                expression += "(";
            }
        } else {
            expression += "(";
        }
        updateDisplay();
    }

    private void appendOperator(String op) {
        if (isResultDisplayed) {
            isResultDisplayed = false;
        }
        if (expression.length() > 0) {
            char lastChar = expression.charAt(expression.length() - 1);
            if (isOperator(lastChar)) {
                expression = expression.substring(0, expression.length() - 1) + op;
            } else {
                expression += op;
            }
        } else if (op.equals("-")) {
            expression += op;
        }
        updateDisplay();
    }

    private boolean isOperator(char ch) {
        return ch == '+' || ch == '-' || ch == '×' || ch == '÷' || ch == '%';
    }

    private void toggleSign() {
        if (expression.length() == 0) {
            expression = "-";
        } else if (expression.startsWith("-") && !expression.contains("+") && !expression.contains("×") && !expression.contains("÷") && !expression.contains("(")) {
            expression = expression.substring(1);
        } else {
            if (expression.startsWith("-(") && expression.endsWith(")")) {
                expression = expression.substring(2, expression.length() - 1);
            } else {
                expression = "-(" + expression + ")";
            }
        }
        updateDisplay();
    }

    private void appendDecimal() {
        if (isResultDisplayed) {
            expression = "0";
            isResultDisplayed = false;
        }
        if (expression.length() == 0) {
            expression = "0.";
        } else {
            int lastOpIndex = -1;
            for (int i = expression.length() - 1; i >= 0; i--) {
                char c = expression.charAt(i);
                if (isOperator(c) || c == '(' || c == ')') {
                    lastOpIndex = i;
                    break;
                }
            }
            String lastNumber = expression.substring(lastOpIndex + 1);
            if (!lastNumber.contains(".")) {
                if (lastNumber.length() == 0) {
                    expression += "0.";
                } else {
                    expression += ".";
                }
            }
        }
        updateDisplay();
    }

    private void updateDisplay() {
        if (expression.length() == 0) {
            tvExpression.setText("0");
            tvResultPreview.setText("");
        } else {
            tvExpression.setText(expression);
            try {
                String processed = preprocessExpression(expression);
                double val = ExpressionEvaluator.evaluate(processed);
                tvResultPreview.setText(formatResult(val));
            } catch (Exception e) {
                tvResultPreview.setText("");
            }
        }
    }

    private void calculateFinalResult() {
        if (expression.length() == 0) return;
        try {
            String originalExpr = expression;
            String processed = preprocessExpression(expression);
            double val = ExpressionEvaluator.evaluate(processed);
            String resultStr = formatResult(val);
            
            tvHistory.setText(originalExpr + " =");
            expression = resultStr;
            tvExpression.setText(resultStr);
            tvResultPreview.setText("");
            isResultDisplayed = true;

            // Write record to internal application history logs
            saveHistoryItem(originalExpr, resultStr);
        } catch (Exception e) {
            tvResultPreview.setText("Error");
        }
    }

    private String preprocessExpression(String expr) {
        String cleanExpr = expr.replace("×", "*").replace("÷", "/");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cleanExpr.length(); i++) {
            char current = cleanExpr.charAt(i);
            if (i > 0) {
                char prev = cleanExpr.charAt(i - 1);
                if (current == '(' && (Character.isDigit(prev) || prev == ')' || prev == '%')) {
                    sb.append('*');
                }
                if (Character.isDigit(current) && prev == ')') {
                    sb.append('*');
                }
            }
            sb.append(current);
        }
        return sb.toString();
    }

    private String formatResult(double val) {
        if (Double.isInfinite(val) || Double.isNaN(val)) {
            return "Error";
        }
        if (val == (long) val) {
            return String.format("%d", (long) val);
        } else {
            DecimalFormat df = new DecimalFormat("#.##########");
            return df.format(val);
        }
    }

    @Override
    protected void onDestroy() {
        if (toneGenerator != null) {
            toneGenerator.release();
        }
        super.onDestroy();
    }

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
                        else if (eat('/')) x /= parseFactor();
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
                        throw new RuntimeException("Unexpected character: " + (char)ch);
                    }

                    if (eat('%')) {
                        x = x / 100.0;
                    }

                    return x;
                }
            }.parse();
        }
    }
}