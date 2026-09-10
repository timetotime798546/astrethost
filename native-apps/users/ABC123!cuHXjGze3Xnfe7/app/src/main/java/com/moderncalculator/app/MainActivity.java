package com.moderncalculator.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.Gravity;
import android.graphics.Color;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private TextView tvExpression;
    private TextView tvResult;
    private LinearLayout historyItemsContainer;
    private View historyPanel;

    private String currentExpression = "";
    private boolean isResultDisplayed = false;
    private final List<String> historyList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvExpression = (TextView) findViewById(R.id.tv_expression);
        tvResult = (TextView) findViewById(R.id.tv_result);
        historyPanel = findViewById(R.id.history_panel);
        historyItemsContainer = (LinearLayout) findViewById(R.id.history_items_container);

        ImageButton btnHistory = (ImageButton) findViewById(R.id.btn_history);
        Button btnCloseHistory = (Button) findViewById(R.id.btn_close_history);
        Button btnClearHistory = (Button) findViewById(R.id.btn_clear_history);

        btnHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showHistoryPanel();
            }
        });

        btnCloseHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                historyPanel.setVisibility(View.GONE);
            }
        });

        btnClearHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                historyList.clear();
                updateHistoryUI();
                Toast.makeText(MainActivity.this, "History cleared", Toast.LENGTH_SHORT).show();
            }
        });

        setupKeyboard();
    }

    private void setupKeyboard() {
        int[] numberIds = {
            R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4,
            R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9,
            R.id.btn_00
        };

        View.OnClickListener numberClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button b = (Button) v;
                if (isResultDisplayed) {
                    currentExpression = "";
                    isResultDisplayed = false;
                }
                currentExpression += b.getText().toString();
                updateDisplay();
                tryLiveEvaluate();
            }
        };

        for (int id : numberIds) {
            findViewById(id).setOnClickListener(numberClickListener);
        }

        int[] opIds = {
            R.id.btn_add, R.id.btn_sub, R.id.btn_mul, R.id.btn_div
        };

        View.OnClickListener opClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button b = (Button) v;
                String opSymbol = b.getText().toString();

                if (isResultDisplayed) {
                    currentExpression = tvResult.getText().toString();
                    isResultDisplayed = false;
                }

                if (currentExpression.length() > 0) {
                    char lastChar = currentExpression.charAt(currentExpression.length() - 1);
                    if (isOperator(lastChar)) {
                        // Replace operator
                        currentExpression = currentExpression.substring(0, currentExpression.length() - 1) + opSymbol;
                    } else {
                        currentExpression += opSymbol;
                    }
                    updateDisplay();
                }
            }
        };

        for (int id : opIds) {
            findViewById(id).setOnClickListener(opClickListener);
        }

        findViewById(R.id.btn_dot).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isResultDisplayed) {
                    currentExpression = "0";
                    isResultDisplayed = false;
                }
                if (currentExpression.isEmpty()) {
                    currentExpression = "0.";
                } else {
                    // Prevent multiple decimal points in a single segment
                    String[] parts = currentExpression.split("[+\\-×÷]");
                    String currentPart = parts.length > 0 ? parts[parts.length - 1] : "";
                    if (!currentPart.contains(".")) {
                        currentExpression += ".";
                    }
                }
                updateDisplay();
            }
        });

        findViewById(R.id.btn_clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentExpression = "";
                tvResult.setText("0");
                updateDisplay();
            }
        });

        findViewById(R.id.btn_backspace).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isResultDisplayed) {
                    currentExpression = "";
                    tvResult.setText("0");
                    isResultDisplayed = false;
                    updateDisplay();
                    return;
                }
                if (currentExpression.length() > 0) {
                    currentExpression = currentExpression.substring(0, currentExpression.length() - 1);
                    updateDisplay();
                    tryLiveEvaluate();
                }
            }
        });

        findViewById(R.id.btn_percent).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentExpression.length() > 0 && !isResultDisplayed) {
                    char lastChar = currentExpression.charAt(currentExpression.length() - 1);
                    if (!isOperator(lastChar)) {
                        currentExpression += "%";
                        updateDisplay();
                        tryLiveEvaluate();
                    }
                }
            }
        });

        findViewById(R.id.btn_equal).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!currentExpression.isEmpty()) {
                    performFinalCalculation();
                }
            }
        });
    }

    private boolean isOperator(char c) {
        return c == '+' || c == '-' || c == '×' || c == '÷';
    }

    private void updateDisplay() {
        tvExpression.setText(currentExpression);
    }

    private void tryLiveEvaluate() {
        if (currentExpression.isEmpty()) {
            tvResult.setText("0");
            return;
        }
        try {
            // Strip trailing operators or unfinished parts before live parsing
            String sanitized = currentExpression;
            if (sanitized.length() > 0 && isOperator(sanitized.charAt(sanitized.length() - 1))) {
                sanitized = sanitized.substring(0, sanitized.length() - 1);
            }
            if (sanitized.isEmpty()) {
                return;
            }
            double val = eval(sanitized);
            tvResult.setText(formatResult(val));
        } catch (Exception e) {
            // Suppress error display during live interactive typing
        }
    }

    private void performFinalCalculation() {
        try {
            double finalVal = eval(currentExpression);
            String formatted = formatResult(finalVal);
            
            // Log entry into history list
            String historyEntry = currentExpression + "\n= " + formatted;
            historyList.add(0, historyEntry);

            tvResult.setText(formatted);
            isResultDisplayed = true;
        } catch (Exception e) {
            tvResult.setText("Error");
            isResultDisplayed = true;
        }
    }

    private String formatResult(double d) {
        if (d == (long) d) {
            return String.format("%d", (long) d);
        } else {
            // Round to maximum of 8 decimal places to prevent overflow
            double rounded = Math.round(d * 100000000.0) / 100000000.0;
            return String.valueOf(rounded);
        }
    }

    private void showHistoryPanel() {
        updateHistoryUI();
        historyPanel.setVisibility(View.VISIBLE);
    }

    private void updateHistoryUI() {
        historyItemsContainer.removeAllViews();
        if (historyList.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText("No calculation logs found.");
            emptyText.setTextColor(Color.parseColor("#7E848C"));
            emptyText.setTextSize(16);
            emptyText.setGravity(Gravity.CENTER);
            emptyText.setPadding(0, 40, 0, 0);
            historyItemsContainer.addView(emptyText);
            return;
        }

        for (final String historyItem : historyList) {
            LinearLayout itemLayout = new LinearLayout(this);
            itemLayout.setOrientation(LinearLayout.VERTICAL);
            itemLayout.setPadding(0, 12, 0, 12);
            itemLayout.setClickable(true);
            itemLayout.setFocusable(true);
            itemLayout.setBackgroundResource(android.R.drawable.list_selector_background);

            TextView logText = new TextView(this);
            logText.setText(historyItem);
            logText.setTextColor(Color.WHITE);
            logText.setTextSize(18);
            logText.setGravity(Gravity.RIGHT);

            itemLayout.addView(logText);

            itemLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Restore arithmetic expression from history logic
                    String[] parts = historyItem.split("\n=");
                    if (parts.length > 0) {
                        currentExpression = parts[0].trim();
                        isResultDisplayed = false;
                        updateDisplay();
                        tryLiveEvaluate();
                    }
                    historyPanel.setVisibility(View.GONE);
                }
            });

            View divider = new View(this);
            divider.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1));
            divider.setBackgroundColor(Color.parseColor("#34383F"));

            historyItemsContainer.addView(itemLayout);
            historyItemsContainer.addView(divider);
        }
    }

    // Pure standard parser execution engine without dependency references
    public static double eval(final String str) {
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
                if (pos < str.length()) throw new RuntimeException("Unexpected character: " + (char) ch);
                return x;
            }

            double parseExpression() {
                double x = parseTerm();
                for (; ; ) {
                    if (eat('+')) x += parseTerm();
                    else if (eat('-')) x -= parseTerm();
                    else return x;
                }
            }

            double parseTerm() {
                double x = parseFactor();
                for (; ; ) {
                    if (eat('×')) x *= parseFactor();
                    else if (eat('÷')) {
                        double divisor = parseFactor();
                        if (divisor == 0) throw new ArithmeticException("Divide by zero");
                        x /= divisor;
                    } else return x;
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
                    String numberStr = str.substring(startPos, this.pos);
                    x = Double.parseDouble(numberStr);
                } else {
                    throw new RuntimeException("Unexpected character: " + (char) ch);
                }

                if (eat('%')) {
                    x = x / 100.0;
                }

                return x;
            }
        }.parse();
    }
}