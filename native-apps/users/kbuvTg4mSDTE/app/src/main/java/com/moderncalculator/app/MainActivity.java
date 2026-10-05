package com.moderncalculator.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class MainActivity extends Activity {

    private TextView tvExpression;
    private TextView tvResult;

    private String currentInput = "";
    private boolean hasEvaluated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvExpression = (TextView) findViewById(R.id.tv_expression);
        tvResult = (TextView) findViewById(R.id.tv_result);

        setupKeypadListeners();
    }

    private void setupKeypadListeners() {
        // Digits
        int[] numButtonIds = {
            R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4,
            R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9
        };

        View.OnClickListener numberClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button b = (Button) v;
                if (hasEvaluated) {
                    currentInput = b.getText().toString();
                    hasEvaluated = false;
                } else {
                    currentInput += b.getText().toString();
                }
                updateUI();
            }
        };

        for (int id : numButtonIds) {
            findViewById(id).setOnClickListener(numberClickListener);
        }

        // Decimal point
        findViewById(R.id.btn_dot).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (hasEvaluated) {
                    currentInput = "0.";
                    hasEvaluated = false;
                } else {
                    if (currentInput.isEmpty() || isLastCharOperator(currentInput)) {
                        currentInput += "0.";
                    } else {
                        // Check if the last number in expression already has a decimal point
                        String[] tokens = currentInput.split("[+\\-×÷%]");
                        if (tokens.length > 0) {
                            String lastToken = tokens[tokens.length - 1];
                            if (!lastToken.contains(".")) {
                                currentInput += ".";
                            }
                        }
                    }
                }
                updateUI();
            }
        });

        // Basic Operators
        setupOperator(R.id.btn_add, "+");
        setupOperator(R.id.btn_subtract, "-");
        setupOperator(R.id.btn_multiply, "×");
        setupOperator(R.id.btn_divide, "÷");
        setupOperator(R.id.btn_modulo, "%");

        // Clear Action
        findViewById(R.id.btn_clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentInput = "";
                hasEvaluated = false;
                tvExpression.setText("");
                tvResult.setText("0");
            }
        });

        // Backspace Action
        findViewById(R.id.btn_delete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (hasEvaluated) {
                    currentInput = "";
                    hasEvaluated = false;
                } else if (!currentInput.isEmpty()) {
                    currentInput = currentInput.substring(0, currentInput.length() - 1);
                }
                updateUI();
            }
        });

        // Sign Toggle (±)
        findViewById(R.id.btn_sign).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentInput.isEmpty() || hasEvaluated) return;
                
                // Find index of last operator to identify last active numerical entity
                int lastOpIdx = -1;
                char[] chars = currentInput.toCharArray();
                for (int i = chars.length - 1; i >= 0; i--) {
                    if (chars[i] == '+' || chars[i] == '-' || chars[i] == '×' || chars[i] == '÷' || chars[i] == '%') {
                        // Ensure it's not a negative sign of a negative number (e.g. "5 + -3")
                        if (i == 0 || (chars[i-1] != '+' && chars[i-1] != '-' && chars[i-1] != '×' && chars[i-1] != '÷' && chars[i-1] != '%')) {
                            lastOpIdx = i;
                            break;
                        }
                    }
                }

                if (lastOpIdx == -1) {
                    // Whole input is one positive/negative value
                    if (currentInput.startsWith("-")) {
                        currentInput = currentInput.substring(1);
                    } else {
                        currentInput = "-" + currentInput;
                    }
                } else {
                    String base = currentInput.substring(0, lastOpIdx + 1);
                    String lastNumber = currentInput.substring(lastOpIdx + 1);
                    if (lastNumber.startsWith("-")) {
                        currentInput = base + lastNumber.substring(1);
                    } else if (!lastNumber.isEmpty()) {
                        currentInput = base + "-" + lastNumber;
                    }
                }
                updateUI();
            }
        });

        // Evaluate (=) Action
        findViewById(R.id.btn_equals).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentInput.isEmpty()) return;
                
                String expr = currentInput;
                if (isLastCharOperator(expr)) {
                    expr = expr.substring(0, expr.length() - 1);
                }

                try {
                    double finalValue = evaluateExpression(expr);
                    tvExpression.setText(currentInput);
                    tvResult.setText(formatResult(finalValue));
                    currentInput = formatResult(finalValue).replace(",", ""); // clean parsed value
                    hasEvaluated = true;
                } catch (Exception e) {
                    tvResult.setText("Error");
                }
            }
        });
    }

    private void setupOperator(int viewId, final String operatorSymbol) {
        findViewById(viewId).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentInput.isEmpty()) {
                    if (operatorSymbol.equals("-")) {
                        currentInput = "-";
                        hasEvaluated = false;
                        updateUI();
                    }
                    return;
                }
                
                if (hasEvaluated) {
                    hasEvaluated = false;
                }

                if (isLastCharOperator(currentInput)) {
                    // Replace operator if already ends with one
                    currentInput = currentInput.substring(0, currentInput.length() - 1) + operatorSymbol;
                } else {
                    currentInput += operatorSymbol;
                }
                updateUI();
            }
        });
    }

    private boolean isLastCharOperator(String input) {
        if (input.isEmpty()) return false;
        char lastChar = input.charAt(input.length() - 1);
        return lastChar == '+' || lastChar == '-' || lastChar == '×' || lastChar == '÷' || lastChar == '%';
    }

    private void updateUI() {
        tvExpression.setText(currentInput);
        
        // Calculate running result preview automatically
        if (currentInput.isEmpty()) {
            tvResult.setText("0");
            return;
        }

        String checkExpr = currentInput;
        if (isLastCharOperator(checkExpr)) {
            checkExpr = checkExpr.substring(0, checkExpr.length() - 1);
        }

        try {
            double previewVal = evaluateExpression(checkExpr);
            tvResult.setText(formatResult(previewVal));
        } catch (Exception e) {
            // Soft failure, don't update result until corrected inputs are entered
        }
    }

    private String formatResult(double d) {
        if (Double.isInfinite(d) || Double.isNaN(d)) {
            return "Error";
        }
        if (d == (long) d) {
            return String.format(Locale.US, "%d", (long) d);
        }
        
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        DecimalFormat format = new DecimalFormat("#.#######", symbols);
        return format.format(d);
    }

    /**
     * Pure Native Java offline expression evaluator.
     * Complies fully with standard operator precedence rules.
     */
    private double evaluateExpression(final String expression) throws Exception {
        // Translate visual operators to parser-compatible indicators
        final String parsedString = expression.replace("×", "*").replace("÷", "/");
        
        return new Object() {
            int pos = -1;
            int ch;

            void nextChar() {
                ch = (++pos < parsedString.length()) ? parsedString.charAt(pos) : -1;
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
                if (pos < parsedString.length()) {
                    throw new RuntimeException("Unexpected: " + (char) ch);
                }
                return x;
            }

            // expression = term | expression `+` term | expression `-` term
            double parseExpression() {
                double x = parseTerm();
                for (;;) {
                    if (eat('+')) {
                        x += parseTerm();
                    } else if (eat('-')) {
                        x -= parseTerm();
                    } else {
                        return x;
                    }
                }
            }

            // term = factor | term `*` factor | term `/` factor | term `%` factor
            double parseTerm() {
                double x = parseFactor();
                for (;;) {
                    if (eat('*')) {
                        x *= parseFactor();
                    } else if (eat('/')) {
                        double divisor = parseFactor();
                        if (divisor == 0) {
                            throw new ArithmeticException("Divide by zero");
                        }
                        x /= divisor;
                    } else if (eat('%')) {
                        double divisor = parseFactor();
                        if (divisor == 0) {
                            throw new ArithmeticException("Divide by zero");
                        }
                        x %= divisor;
                    } else {
                        return x;
                    }
                }
            }

            double parseFactor() {
                if (eat('+')) {
                    return parseFactor(); // unary plus
                }
                if (eat('-')) {
                    return -parseFactor(); // unary minus
                }

                double x;
                int startPos = this.pos;
                if (eat('(')) { // parentheses logic helper
                    x = parseExpression();
                    eat(')');
                } else if ((ch >= '0' && ch <= '9') || ch == '.') { // parses raw integer and floating strings
                    while ((ch >= '0' && ch <= '9') || ch == '.') {
                        nextChar();
                    }
                    x = Double.parseDouble(parsedString.substring(startPos, this.pos));
                } else {
                    throw new RuntimeException("Unexpected expression element: " + (char) ch);
                }

                return x;
            }
        }.parse();
    }
}