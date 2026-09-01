package com.moderncalculator.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.text.DecimalFormat;

public class MainActivity extends Activity implements View.OnClickListener {

    private TextView tvHistory;
    private TextView tvDisplay;
    private StringBuilder currentExpression;
    private boolean isResultDisplayed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvHistory = (TextView) findViewById(R.id.tvHistory);
        tvDisplay = (TextView) findViewById(R.id.tvDisplay);
        currentExpression = new StringBuilder();
        isResultDisplayed = false;

        int[] buttonIds = {
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9,
            R.id.btnDot, R.id.btnPlus, R.id.btnMinus, R.id.btnMultiply,
            R.id.btnDivide, R.id.btnClear, R.id.btnDelete, R.id.btnPercent,
            R.id.btnEqual, R.id.btnToggleSign
        };

        for (int id : buttonIds) {
            View button = findViewById(id);
            if (button != null) {
                button.setOnClickListener(this);
            }
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.btnClear) {
            currentExpression.setLength(0);
            tvDisplay.setText("0");
            tvHistory.setText("");
            isResultDisplayed = false;
        } else if (id == R.id.btnDelete) {
            if (isResultDisplayed) {
                currentExpression.setLength(0);
                tvDisplay.setText("0");
                isResultDisplayed = false;
            } else if (currentExpression.length() > 0) {
                currentExpression.deleteCharAt(currentExpression.length() - 1);
                tvDisplay.setText(currentExpression.length() == 0 ? "0" : currentExpression.toString());
            }
        } else if (id == R.id.btnEqual) {
            evaluateExpression();
        } else if (id == R.id.btnPercent) {
            handlePercent();
        } else if (id == R.id.btnToggleSign) {
            handleToggleSign();
        } else if (id == R.id.btnPlus || id == R.id.btnMinus || id == R.id.btnMultiply || id == R.id.btnDivide) {
            handleOperator(((Button) v).getText().toString());
        } else {
            String value = ((Button) v).getText().toString();
            handleInput(value);
        }
    }

    private void handleInput(String input) {
        if (isResultDisplayed) {
            currentExpression.setLength(0);
            isResultDisplayed = false;
        }
        if (input.equals(".")) {
            String expr = currentExpression.toString();
            if (expr.isEmpty() || isOperator(expr.charAt(expr.length() - 1))) {
                currentExpression.append("0");
            } else {
                String[] tokens = expr.split("[+\\-×÷]");
                if (tokens.length > 0 && tokens[tokens.length - 1].contains(".")) {
                    return;
                }
            }
        }
        currentExpression.append(input);
        tvDisplay.setText(currentExpression.toString());
    }

    private void handleOperator(String operator) {
        if (currentExpression.length() == 0) {
            if (operator.equals("-")) {
                currentExpression.append(operator);
                tvDisplay.setText(currentExpression.toString());
            }
            return;
        }
        char lastChar = currentExpression.charAt(currentExpression.length() - 1);
        if (isOperator(lastChar)) {
            currentExpression.setLength(currentExpression.length() - 1);
        }
        currentExpression.append(operator);
        tvDisplay.setText(currentExpression.toString());
        isResultDisplayed = false;
    }

    private boolean isOperator(char ch) {
        return ch == '+' || ch == '-' || ch == '×' || ch == '÷';
    }

    private void handlePercent() {
        if (currentExpression.length() > 0) {
            try {
                double val = Double.parseDouble(tvDisplay.getText().toString());
                double percentVal = val / 100.0;
                tvHistory.setText(val + "%");
                String formatted = formatResult(percentVal);
                currentExpression.setLength(0);
                currentExpression.append(formatted);
                tvDisplay.setText(formatted);
                isResultDisplayed = true;
            } catch (Exception e) {
                tvDisplay.setText("Error");
            }
        }
    }

    private void handleToggleSign() {
        if (currentExpression.length() > 0) {
            try {
                double val = Double.parseDouble(tvDisplay.getText().toString());
                double toggledVal = -val;
                String formatted = formatResult(toggledVal);
                currentExpression.setLength(0);
                currentExpression.append(formatted);
                tvDisplay.setText(formatted);
                isResultDisplayed = true;
            } catch (Exception e) {
                // Ignored if current expression isn't parseable directly
            }
        }
    }

    private void evaluateExpression() {
        String expr = currentExpression.toString();
        if (expr.isEmpty()) return;
        char lastChar = expr.charAt(expr.length() - 1);
        if (isOperator(lastChar)) {
            expr = expr.substring(0, expr.length() - 1);
        }

        try {
            double result = eval(expr);
            String formattedResult = formatResult(result);
            tvHistory.setText(expr);
            tvDisplay.setText(formattedResult);
            currentExpression.setLength(0);
            currentExpression.append(formattedResult);
            isResultDisplayed = true;
        } catch (Exception e) {
            tvDisplay.setText("Error");
            currentExpression.setLength(0);
        }
    }

    private String formatResult(double value) {
        if (Double.isInfinite(value) || Double.isNaN(value)) {
            return "Error";
        }
        if (value == (long) value) {
            return String.format("%d", (long) value);
        } else {
            DecimalFormat df = new DecimalFormat("#.########");
            return df.format(value);
        }
    }

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
                    if      (eat('*') || eat('×')) x *= parseFactor();
                    else if (eat('/') || eat('÷')) x /= parseFactor();
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
                return x;
            }
        }.parse();
    }
}