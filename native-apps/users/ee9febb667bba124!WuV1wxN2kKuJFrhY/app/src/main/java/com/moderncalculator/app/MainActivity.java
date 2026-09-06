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
    private StringBuilder currentExpression;
    private boolean isResultDisplayed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvExpression = (TextView) findViewById(R.id.tvExpression);
        tvResult = (TextView) findViewById(R.id.tvResult);
        currentExpression = new StringBuilder();
        isResultDisplayed = false;

        setupButtons();
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
                    onNumericClick(numStr);
                }
            });
        }

        // Dot operator click handler
        findViewById(R.id.btnDot).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDotClick();
            }
        });

        // Main operations click handlers
        findViewById(R.id.btnPlus).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onOperatorClick("+");
            }
        });
        findViewById(R.id.btnSubtract).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onOperatorClick("-");
            }
        });
        findViewById(R.id.btnMultiply).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onOperatorClick("*");
            }
        });
        findViewById(R.id.btnDivide).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onOperatorClick("/");
            }
        });

        // Special / Helper buttons handlers
        findViewById(R.id.btnC).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClearClick();
            }
        });
        findViewById(R.id.btnBackspace).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackspaceClick();
            }
        });
        findViewById(R.id.btnPercent).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onPercentClick();
            }
        });
        findViewById(R.id.btnToggleSign).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onToggleSignClick();
            }
        });
        findViewById(R.id.btnEqual).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onEqualClick();
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
                // Swap the last operator in case of error
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
            String originalExpr = currentExpression.toString();
            try {
                double finalResult = evaluateExpression(originalExpr);
                String formattedResult = formatNumber(finalResult);

                tvExpression.setText(getDisplayString(originalExpr));
                tvResult.setText(formattedResult);

                currentExpression.setLength(0);
                currentExpression.append(formattedResult);
                isResultDisplayed = true;
            } catch (Exception e) {
                tvResult.setText("Error");
                isResultDisplayed = true;
            }
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
                // Keep the current result untouched in case parsing details are incomplete
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

    // Mathematical expression evaluator utilizing dynamic top-down parser
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
                    if (pos < str.length()) throw new RuntimeException("Unexpected: " + (char)ch);
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