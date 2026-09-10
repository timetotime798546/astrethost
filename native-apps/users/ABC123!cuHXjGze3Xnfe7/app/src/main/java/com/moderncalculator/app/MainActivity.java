package com.moderncalculator.app;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Locale;

public class MainActivity extends Activity {

    private TextView txtExpression;
    private TextView txtResult;
    private View btnHistoryToggle;
    private View btnHistoryClose;
    private View btnHistoryClear;
    private View historyPanel;
    private LinearLayout historyListContainer;

    private String currentInput = "0";
    private String currentExpression = "";
    private boolean isResultDisplaying = false;

    private static final String PREFS_NAME = "calculator_prefs";
    private static final String KEY_HISTORY = "history_list";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind layouts and components
        txtExpression = (TextView) findViewById(R.id.txt_expression);
        txtResult = (TextView) findViewById(R.id.txt_result);
        btnHistoryToggle = findViewById(R.id.btn_history_toggle);
        btnHistoryClose = findViewById(R.id.btn_close_history);
        btnHistoryClear = findViewById(R.id.btn_clear_history);
        historyPanel = findViewById(R.id.history_panel);
        historyListContainer = (LinearLayout) findViewById(R.id.history_list_container);

        // Bind button actions
        setupButtonActions();
        setupHistoryActions();
        updateDisplay();
    }

    private void setupButtonActions() {
        // Numeric IDs
        int[] numIds = {
            R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4,
            R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9
        };

        for (int i = 0; i < numIds.length; i++) {
            final String digit = String.valueOf(i);
            findViewById(numIds[i]).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onDigitClick(digit);
                }
            });
        }

        // Dot Key
        findViewById(R.id.btn_dot).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDotClick();
            }
        });

        // Toggle sign key (±)
        findViewById(R.id.btn_toggle_sign).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onToggleSignClick();
            }
        });

        // Operators mapping
        findViewById(R.id.btn_add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onOperatorClick("+");
            }
        });

        findViewById(R.id.btn_subtract).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onOperatorClick("-");
            }
        });

        findViewById(R.id.btn_multiply).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onOperatorClick("×");
            }
        });

        findViewById(R.id.btn_divide).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onOperatorClick("÷");
            }
        });

        // Percentage
        findViewById(R.id.btn_percentage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onPercentageClick();
            }
        });

        // Clear Key
        findViewById(R.id.btn_clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClearClick();
            }
        });

        // Backspace key actions
        View btnBack = findViewById(R.id.btn_backspace);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackClick();
            }
        });
        
        btnBack.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                onClearClick();
                return true;
            }
        });

        // Equal Evaluation Action
        findViewById(R.id.btn_equal).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onEqualClick();
            }
        });
    }

    private void setupHistoryActions() {
        btnHistoryToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadHistory();
                historyPanel.setVisibility(View.VISIBLE);
            }
        });

        btnHistoryClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                historyPanel.setVisibility(View.GONE);
            }
        });

        btnHistoryClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearHistory();
            }
        });
    }

    private void onDigitClick(String digit) {
        if (isResultDisplaying) {
            currentInput = digit;
            currentExpression = "";
            isResultDisplaying = false;
        } else {
            if (currentInput.equals("0")) {
                currentInput = digit;
            } else {
                currentInput += digit;
            }
        }
        updateDisplay();
    }

    private void onDotClick() {
        if (isResultDisplaying) {
            currentInput = "0.";
            currentExpression = "";
            isResultDisplaying = false;
        } else {
            if (currentInput.isEmpty()) {
                currentInput = "0.";
            } else if (!currentInput.contains(".")) {
                currentInput += ".";
            }
        }
        updateDisplay();
    }

    private void onToggleSignClick() {
        if (currentInput.equals("0") || currentInput.isEmpty()) {
            return;
        }
        if (currentInput.startsWith("-")) {
            currentInput = currentInput.substring(1);
        } else {
            currentInput = "-" + currentInput;
        }
        updateDisplay();
    }

    private void onOperatorClick(String op) {
        if (isResultDisplaying) {
            currentExpression = currentInput + " " + op + " ";
            currentInput = "";
            isResultDisplaying = false;
        } else {
            if (currentInput.isEmpty()) {
                if (!currentExpression.isEmpty()) {
                    String trimmed = currentExpression.trim();
                    int lastSpace = trimmed.lastIndexOf(" ");
                    if (lastSpace != -1) {
                        currentExpression = trimmed.substring(0, lastSpace) + " " + op + " ";
                    } else {
                        currentExpression = "0 " + op + " ";
                    }
                } else {
                    currentExpression = "0 " + op + " ";
                }
            } else {
                currentExpression += currentInput + " " + op + " ";
                currentInput = "";
            }
        }
        updateDisplay();
    }

    private void onPercentageClick() {
        if (isResultDisplaying) {
            currentExpression = currentInput + "%";
            try {
                double val = Double.parseDouble(currentInput) / 100.0;
                currentInput = formatResult(val);
            } catch (Exception e) {
                currentInput = "Error";
            }
            isResultDisplaying = true;
        } else {
            if (!currentInput.isEmpty() && !currentInput.equals("0")) {
                try {
                    double val = Double.parseDouble(currentInput) / 100.0;
                    currentInput = formatResult(val);
                } catch (Exception e) {
                    currentInput = "Error";
                }
            }
        }
        updateDisplay();
    }

    private void onClearClick() {
        currentInput = "0";
        currentExpression = "";
        isResultDisplaying = false;
        updateDisplay();
    }

    private void onBackClick() {
        if (isResultDisplaying) {
            currentExpression = "";
            isResultDisplaying = false;
        } else {
            if (!currentInput.isEmpty()) {
                currentInput = currentInput.substring(0, currentInput.length() - 1);
                if (currentInput.isEmpty() || currentInput.equals("-")) {
                    currentInput = "0";
                }
            }
        }
        updateDisplay();
    }

    private void onEqualClick() {
        if (currentInput.isEmpty() && currentExpression.isEmpty()) {
            return;
        }
        String finalExpression = currentExpression + currentInput;
        if (finalExpression.trim().isEmpty()) {
            return;
        }

        String cleanedExpr = finalExpression.trim();
        // Clear trailing mathematical symbols
        if (cleanedExpr.endsWith("+") || cleanedExpr.endsWith("-") || cleanedExpr.endsWith("×") || cleanedExpr.endsWith("÷")) {
            cleanedExpr = cleanedExpr.substring(0, cleanedExpr.length() - 1).trim();
        }

        String rawFormula = cleanedExpr.replace("×", "*").replace("÷", "/");
        try {
            double value = eval(rawFormula);
            String resultStr = formatResult(value);

            if (!resultStr.equals("Error")) {
                saveToHistory(cleanedExpr, resultStr);
            }

            currentExpression = finalExpression + " =";
            currentInput = resultStr;
            isResultDisplaying = true;
        } catch (Exception e) {
            currentInput = "Error";
            isResultDisplaying = true;
        }
        updateDisplay();
    }

    private void updateDisplay() {
        txtResult.setText(currentInput);
        txtExpression.setText(currentExpression);

        // Dynamically shrink text sizing for very long outputs
        int len = currentInput.length();
        if (len > 15) {
            txtResult.setTextSize(26);
        } else if (len > 10) {
            txtResult.setTextSize(34);
        } else {
            txtResult.setTextSize(48);
        }
    }

    private String formatResult(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "Error";
        }
        if (value == (long) value) {
            return String.format(Locale.US, "%d", (long) value);
        } else {
            String formatStr = String.format(Locale.US, "%.10f", value);
            while (formatStr.endsWith("0")) {
                formatStr = formatStr.substring(0, formatStr.length() - 1);
            }
            if (formatStr.endsWith(".")) {
                formatStr = formatStr.substring(0, formatStr.length() - 1);
            }
            return formatStr;
        }
    }

    private void saveToHistory(String expression, String result) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String savedHistory = prefs.getString(KEY_HISTORY, "");
        String newEntry = expression + " = " + result;

        if (savedHistory.isEmpty()) {
            savedHistory = newEntry;
        } else {
            savedHistory = newEntry + "|||" + savedHistory;
        }

        // Limit the history to 15 entries maximum
        String[] logs = savedHistory.split("\\|\\|\\|");
        if (logs.length > 15) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 15; i++) {
                sb.append(logs[i]);
                if (i < 14) sb.append("|||");
            }
            savedHistory = sb.toString();
        }
        prefs.edit().putString(KEY_HISTORY, savedHistory).apply();
    }

    private void loadHistory() {
        historyListContainer.removeAllViews();
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String savedHistory = prefs.getString(KEY_HISTORY, "");

        if (savedHistory.isEmpty()) {
            TextView txtEmpty = new TextView(this);
            txtEmpty.setText("No calculations stored yet");
            txtEmpty.setTextColor(0xFF8E8E93);
            txtEmpty.setTextSize(16);
            txtEmpty.setGravity(Gravity.CENTER);
            txtEmpty.setPadding(0, 80, 0, 0);
            historyListContainer.addView(txtEmpty);
            return;
        }

        String[] logs = savedHistory.split("\\|\\|\\|");
        for (int i = 0; i < logs.length; i++) {
            final String record = logs[i];
            if (record.trim().isEmpty()) continue;

            LinearLayout layoutRow = new LinearLayout(this);
            layoutRow.setOrientation(LinearLayout.VERTICAL);
            layoutRow.setPadding(20, 24, 20, 24);
            layoutRow.setClickable(true);
            layoutRow.setFocusable(true);
            layoutRow.setBackgroundResource(android.R.drawable.list_selector_background);

            TextView txtRecord = new TextView(this);
            txtRecord.setText(record);
            txtRecord.setTextColor(0xFFFFFFFF);
            txtRecord.setTextSize(17);
            layoutRow.addView(txtRecord);

            layoutRow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String[] items = record.split(" = ");
                    if (items.length == 2) {
                        currentExpression = items[0];
                        currentInput = items[1];
                        isResultDisplaying = true;
                        updateDisplay();
                        historyPanel.setVisibility(View.GONE);
                    }
                }
            });

            View dividerLine = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1
            );
            lp.setMargins(0, 4, 0, 4);
            dividerLine.setLayoutParams(lp);
            dividerLine.setBackgroundColor(0xFF3A3A3C);

            historyListContainer.addView(layoutRow);
            historyListContainer.addView(dividerLine);
        }
    }

    private void clearHistory() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putString(KEY_HISTORY, "").apply();
        loadHistory();
    }

    // Java 8 Compatible algebraic recursive descent expression parsing logic
    private double eval(final String formula) {
        return new Object() {
            int pos = -1, ch;

            void nextChar() {
                ch = (++pos < formula.length()) ? formula.charAt(pos) : -1;
            }

            boolean consume(int charToConsume) {
                while (ch == ' ') nextChar();
                if (ch == charToConsume) {
                    nextChar();
                    return true;
                }
                return false;
            }

            double parse() {
                nextChar();
                double x = parseExpression();
                if (pos < formula.length()) throw new RuntimeException("Unexpected expression character");
                return x;
            }

            double parseExpression() {
                double x = parseTerm();
                for (;;) {
                    if      (consume('+')) x += parseTerm(); // Addition
                    else if (consume('-')) x -= parseTerm(); // Subtraction
                    else return x;
                }
            }

            double parseTerm() {
                double x = parseFactor();
                for (;;) {
                    if      (consume('*')) x *= parseFactor(); // Multiplication
                    else if (consume('/')) {
                        double bottom = parseFactor();
                        if (bottom == 0) throw new ArithmeticException("Divide by Zero");
                        x /= bottom; // Division
                    }
                    else return x;
                }
            }

            double parseFactor() {
                if (consume('+')) return parseFactor();
                if (consume('-')) return -parseFactor();

                double x;
                int startPos = this.pos;
                if (consume('(')) {
                    x = parseExpression();
                    consume(')');
                } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(formula.substring(startPos, this.pos));
                } else {
                    throw new RuntimeException("Malformed formula");
                }

                if (consume('%')) {
                    x = x / 100.0;
                }

                return x;
            }
        }.parse();
    }
}