package com.moderncalculator.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;

public class MainActivity extends Activity {

    private TextView tvDisplay;
    private TextView tvExpression;
    private TextView tvHistoryText;
    private LinearLayout historyContainer;

    private String currentInput = "";
    private String savedExpression = "";
    private boolean isResultDisplayed = false;
    private ArrayList<String> computationHistory = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind displays
        tvDisplay = (TextView) findViewById(R.id.tv_display);
        tvExpression = (TextView) findViewById(R.id.tv_expression);
        tvHistoryText = (TextView) findViewById(R.id.history_text_view);
        historyContainer = (LinearLayout) findViewById(R.id.history_container);

        // Bind utility interactive UI parts
        TextView btnToggleHistory = (TextView) findViewById(R.id.btn_toggle_history);
        TextView btnClearHistory = (TextView) findViewById(R.id.btn_clear_history);

        // Setup History layout toggles
        btnToggleHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (historyContainer.getVisibility() == View.VISIBLE) {
                    historyContainer.setVisibility(View.GONE);
                } else {
                    historyContainer.setVisibility(View.VISIBLE);
                    updateHistoryDisplay();
                }
            }
        });

        btnClearHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                computationHistory.clear();
                updateHistoryDisplay();
            }
        });

        // Initialize and bind all key actions
        int[] numButtons = {
                R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4,
                R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9
        };

        View.OnClickListener numListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button b = (Button) v;
                appendNumber(b.getText().toString());
            }
        };

        for (int id : numButtons) {
            findViewById(id).setOnClickListener(numListener);
        }

        // Action Keys
        findViewById(R.id.btn_ac).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearAll();
            }
        });

        findViewById(R.id.btn_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                backspace();
            }
        });

        findViewById(R.id.btn_percent).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyPercent();
            }
        });

        findViewById(R.id.btn_toggle_sign).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleSign();
            }
        });

        findViewById(R.id.btn_dot).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                appendDot();
            }
        });

        // Operator bindings
        int[] operatorButtons = {
                R.id.btn_add, R.id.btn_sub, R.id.btn_mul, R.id.btn_div
        };

        View.OnClickListener opListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button b = (Button) v;
                onOperatorPressed(b.getText().toString());
            }
        };

        for (int id : operatorButtons) {
            findViewById(id).setOnClickListener(opListener);
        }

        findViewById(R.id.btn_equal).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calculateResult();
            }
        });
    }

    private void appendNumber(String number) {
        if (isResultDisplayed) {
            currentInput = "";
            isResultDisplayed = false;
        }

        // Prevent redundant multi zero inputs at the start
        if (currentInput.equals("0")) {
            currentInput = number;
        } else {
            currentInput += number;
        }

        updateDisplay(currentInput);
    }

    private void appendDot() {
        if (isResultDisplayed) {
            currentInput = "0";
            isResultDisplayed = false;
        }

        if (currentInput.isEmpty()) {
            currentInput = "0.";
        } else if (!currentInput.contains(".")) {
            currentInput += ".";
        }
        updateDisplay(currentInput);
    }

    private void toggleSign() {
        if (currentInput.isEmpty() || currentInput.equals("0")) {
            return;
        }

        if (currentInput.startsWith("-")) {
            currentInput = currentInput.substring(1);
        } else {
            currentInput = "-" + currentInput;
        }
        updateDisplay(currentInput);
    }

    private void applyPercent() {
        if (!currentInput.isEmpty()) {
            try {
                double val = Double.parseDouble(currentInput) / 100.0;
                currentInput = formatResult(val);
                updateDisplay(currentInput);
            } catch (NumberFormatException e) {
                updateDisplay("Error");
            }
        }
    }

    private void onOperatorPressed(String operator) {
        if (isResultDisplayed) {
            savedExpression = currentInput + " " + operator + " ";
            currentInput = "";
            isResultDisplayed = false;
            tvExpression.setText(savedExpression);
            return;
        }

        if (currentInput.isEmpty()) {
            // Replace previous operator if empty input
            if (!savedExpression.isEmpty()) {
                savedExpression = savedExpression.substring(0, savedExpression.length() - 2) + operator + " ";
                tvExpression.setText(savedExpression);
            }
            return;
        }

        savedExpression += currentInput + " " + operator + " ";
        currentInput = "";
        tvExpression.setText(savedExpression);
        updateDisplay("0");
    }

    private void calculateResult() {
        if (savedExpression.isEmpty() && currentInput.isEmpty()) {
            return;
        }

        String fullFormula = savedExpression + currentInput;
        if (fullFormula.trim().isEmpty()) {
            return;
        }

        // Sanitize last hanging operator if nothing follows it
        String cleanFormula = fullFormula.trim();
        if (cleanFormula.endsWith("+") || cleanFormula.endsWith("-") || cleanFormula.endsWith("×") || cleanFormula.endsWith("÷")) {
            cleanFormula = cleanFormula.substring(0, cleanFormula.length() - 1).trim();
        }

        try {
            double finalResult = eval(cleanFormula);
            String finalFormatted = formatResult(finalResult);

            tvExpression.setText(fullFormula + " =");
            updateDisplay(finalFormatted);

            // Save math event history
            computationHistory.add(0, cleanFormula + " = " + finalFormatted);
            updateHistoryDisplay();

            currentInput = finalFormatted;
            savedExpression = "";
            isResultDisplayed = true;
        } catch (Exception e) {
            updateDisplay("Error");
            savedExpression = "";
            currentInput = "";
        }
    }

    private void clearAll() {
        currentInput = "";
        savedExpression = "";
        isResultDisplayed = false;
        tvExpression.setText("");
        updateDisplay("0");
    }

    private void backspace() {
        if (isResultDisplayed) {
            clearAll();
            return;
        }

        if (!currentInput.isEmpty()) {
            currentInput = currentInput.substring(0, currentInput.length() - 1);
            if (currentInput.isEmpty()) {
                updateDisplay("0");
            } else {
                updateDisplay(currentInput);
            }
        }
    }

    private void updateDisplay(String text) {
        tvDisplay.setText(text);
    }

    private void updateHistoryDisplay() {
        if (computationHistory.isEmpty()) {
            tvHistoryText.setText("No calculations recorded yet.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(computationHistory.size(), 8); i++) {
            sb.append(computationHistory.get(i)).append("\n");
        }
        tvHistoryText.setText(sb.toString().trim());
    }

    private String formatResult(double d) {
        if (Double.isInfinite(d) || Double.isNaN(d)) {
            return "Error";
        }
        // Check if value is a clean integer
        if (d == (long) d) {
            return String.format("%d", (long) d);
        }
        return String.valueOf(d);
    }

    // Pure dynamic arithmetic interpreter for standard mathematical infix syntax compatibility
    private double eval(final String str) {
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
                if (pos < str.length()) throw new RuntimeException("Unexpected expression syntax: " + (char) ch);
                return x;
            }

            double parseExpression() {
                double x = parseTerm();
                for (;;) {
                    if      (eat('+')) x += parseTerm(); // addition
                    else if (eat('-')) x -= parseTerm(); // subtraction
                    else return x;
                }
            }

            double parseTerm() {
                double x = parseFactor();
                for (;;) {
                    if      (eat('×') || eat('*')) x *= parseFactor(); // multiplication
                    else if (eat('÷') || eat('/')) {
                        double divisor = parseFactor();
                        if (divisor == 0) {
                            throw new ArithmeticException("Divide by zero exception");
                        }
                        x /= divisor; // division
                    }
                    else return x;
                }
            }

            double parseFactor() {
                if (eat('+')) return parseFactor(); // unary operator
                if (eat('-')) return -parseFactor(); // unary negative

                double x;
                int startPos = this.pos;
                if (eat('(')) { // parentheses logic
                    x = parseExpression();
                    eat(')');
                } else if ((ch >= '0' && ch <= '9') || ch == '.' || ch == 'E' || ch == 'e') { // floating/scientific number notation
                    while ((ch >= '0' && ch <= '9') || ch == '.' || ch == 'E' || ch == 'e' || (ch == '-' && (str.charAt(pos - 1) == 'E' || str.charAt(pos - 1) == 'e'))) {
                        nextChar();
                    }
                    x = Double.parseDouble(str.substring(startPos, this.pos));
                } else {
                    throw new RuntimeException("Unexpected character sequence: " + (char) ch);
                }

                return x;
            }
        }.parse();
    }
}