package com.beautifulcalculator.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class MainActivity extends Activity {

    private TextView tvExpression;
    private TextView tvResultPreview;
    private String expression = "";
    private boolean isResultDisplayed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI Elements
        tvExpression = (TextView) findViewById(R.id.tv_expression);
        tvResultPreview = (TextView) findViewById(R.id.tv_result_preview);

        // Map Buttons and set traditional anonymous inner class listeners
        int[] numButtons = {
                R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3,
                R.id.btn_4, R.id.btn_5, R.id.btn_6, R.id.btn_7,
                R.id.btn_8, R.id.btn_9
        };

        for (int id : numButtons) {
            findViewById(id).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Button b = (Button) v;
                    appendNumber(b.getText().toString());
                }
            });
        }

        // Decimal button
        findViewById(R.id.btn_decimal).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                appendDecimal();
            }
        });

        // Operators
        findViewById(R.id.btn_add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { appendOperator("+"); }
        });
        findViewById(R.id.btn_subtract).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { appendOperator("-"); }
        });
        findViewById(R.id.btn_multiply).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { appendOperator("*"); }
        });
        findViewById(R.id.btn_divide).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { appendOperator("/"); }
        });

        // Percentage Unary Operator
        findViewById(R.id.btn_percent).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                appendPercent();
            }
        });

        // Unary Negation (+/-) Toggle
        findViewById(R.id.btn_negate).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleSign();
            }
        });

        // Backspace delete
        findViewById(R.id.btn_delete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performDelete();
            }
        });

        // Clear display
        findViewById(R.id.btn_clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performClear();
            }
        });

        // Evaluate equation (=)
        findViewById(R.id.btn_equals).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performEquals();
            }
        });
    }

    private void updateDisplay() {
        // Render pretty operator glyphs for user
        String displayStr = expression.replace("*", " × ")
                                      .replace("/", " ÷ ")
                                      .replace("+", " + ")
                                      .replace("-", " − ");
        tvExpression.setText(displayStr);

        // Perform safe real-time background evaluation
        if (expression.isEmpty()) {
            tvResultPreview.setText("");
        } else {
            try {
                double val = evaluate(expression);
                tvResultPreview.setText(formatResult(val));
            } catch (Exception e) {
                // Keep the preview empty or last success during partial input typing
                tvResultPreview.setText("");
            }
        }
    }

    private void appendNumber(String num) {
        if (isResultDisplayed) {
            expression = "";
            isResultDisplayed = false;
        }
        expression += num;
        updateDisplay();
    }

    private void appendDecimal() {
        if (isResultDisplayed) {
            expression = "0";
            isResultDisplayed = false;
        }
        if (expression.isEmpty()) {
            expression = "0.";
            updateDisplay();
            return;
        }

        // Ensure decimal is only added to the active trailing number token
        int lastOpIdx = -1;
        String ops = "+-*/";
        for (int i = 0; i < ops.length(); i++) {
            int idx = expression.lastIndexOf(ops.charAt(i));
            if (idx > lastOpIdx) {
                lastOpIdx = idx;
            }
        }

        String currentToken = lastOpIdx == -1 ? expression : expression.substring(lastOpIdx + 1);
        if (!currentToken.contains(".")) {
            if (currentToken.isEmpty()) {
                expression += "0.";
            } else {
                expression += ".";
            }
        }
        updateDisplay();
    }

    private void appendOperator(String op) {
        if (expression.isEmpty()) {
            if (op.equals("-")) {
                expression = "-";
            }
            updateDisplay();
            return;
        }

        isResultDisplayed = false;
        char lastChar = expression.charAt(expression.length() - 1);

        // Replace ending operator if user decides to shift operators mid-equation
        if (lastChar == '+' || lastChar == '-' || lastChar == '*' || lastChar == '/') {
            expression = expression.substring(0, expression.length() - 1) + op;
        } else {
            expression += op;
        }
        updateDisplay();
    }

    private void appendPercent() {
        if (expression.isEmpty()) return;
        char lastChar = expression.charAt(expression.length() - 1);
        if (Character.isDigit(lastChar) || lastChar == '.' || lastChar == '%') {
            expression += "%";
            isResultDisplayed = false;
            updateDisplay();
        }
    }

    private void toggleSign() {
        if (expression.isEmpty()) {
            expression = "-";
            updateDisplay();
            return;
        }

        // Standard negation toggles between positive and negative numbers at end of equation
        int lastOpIdx = -1;
        String ops = "+-*/";
        for (int i = 0; i < ops.length(); i++) {
            int idx = expression.lastIndexOf(ops.charAt(i));
            if (idx > lastOpIdx) {
                lastOpIdx = idx;
            }
        }

        if (lastOpIdx == -1) {
            // Expression is purely one token
            if (expression.startsWith("-")) {
                expression = expression.substring(1);
            } else {
                expression = "-" + expression;
            }
        } else {
            // Expression contains operator preceding the last number
            String part1 = expression.substring(0, lastOpIdx + 1);
            String part2 = expression.substring(lastOpIdx + 1);
            if (part2.startsWith("-")) {
                expression = part1 + part2.substring(1);
            } else {
                expression = part1 + "-" + part2;
            }
        }
        isResultDisplayed = false;
        updateDisplay();
    }

    private void performDelete() {
        if (isResultDisplayed) {
            expression = "";
            isResultDisplayed = false;
        } else if (expression.length() > 0) {
            expression = expression.substring(0, expression.length() - 1);
        }
        updateDisplay();
    }

    private void performClear() {
        expression = "";
        isResultDisplayed = false;
        tvExpression.setText("");
        tvResultPreview.setText("");
    }

    private void performEquals() {
        if (expression.isEmpty()) return;
        try {
            double rawValue = evaluate(expression);
            String formatted = formatResult(rawValue);
            tvExpression.setText(formatted);
            tvResultPreview.setText("");
            expression = formatted;
            isResultDisplayed = true;
        } catch (Exception e) {
            tvExpression.setText("Error");
            tvResultPreview.setText("");
            expression = "";
            isResultDisplayed = true;
        }
    }

    private String formatResult(double value) {
        if (Double.isInfinite(value) || Double.isNaN(value)) {
            return "Error";
        }
        if (value == (long) value) {
            return String.valueOf((long) value);
        }
        
        try {
            // Avoid extreme floating precision artifacts
            BigDecimal bd = new BigDecimal(value);
            bd = bd.setScale(10, RoundingMode.HALF_UP);
            double rounded = bd.doubleValue();
            if (rounded == (long) rounded) {
                return String.valueOf((long) rounded);
            }
            String str = String.valueOf(rounded);
            if (str.contains(".")) {
                str = str.replaceAll("0*$", "");
                if (str.endsWith(".")) {
                    str = str.substring(0, str.length() - 1);
                }
            }
            return str;
        } catch (Exception ex) {
            return String.valueOf(value);
        }
    }

    /**
     * Native mathematical expression parser engine
     */
    private double evaluate(final String str) {
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

            // Grammar parser structure (Term, Expression, Factor)
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
                    if (eat('*')) x *= parseFactor();
                    else if (eat('/')) {
                        double divisor = parseFactor();
                        if (divisor == 0) throw new ArithmeticException("Division by zero");
                        x /= divisor;
                    } else if (eat('%')) {
                        // Apply percentage modifier directly to current factor
                        x = x / 100.0;
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
                    x = Double.parseDouble(str.substring(startPos, this.pos));
                } else {
                    throw new RuntimeException("Unexpected: " + (char) ch);
                }

                // Handle post-fix percent symbol
                if (eat('%')) {
                    x = x / 100.0;
                }

                return x;
            }
        }.parse();
    }
}