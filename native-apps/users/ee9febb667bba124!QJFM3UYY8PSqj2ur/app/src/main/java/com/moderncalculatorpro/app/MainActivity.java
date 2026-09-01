package com.moderncalculatorpro.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import java.text.DecimalFormat;

public class MainActivity extends Activity implements View.OnClickListener {

    private TextView tvExpression;
    private TextView tvResult;
    private String expression = "";
    private boolean isResultCommitted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvExpression = (TextView) findViewById(R.id.tv_expression);
        tvResult = (TextView) findViewById(R.id.tv_result);

        int[] buttonIds = new int[]{
                R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4,
                R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9,
                R.id.btn_add, R.id.btn_subtract, R.id.btn_multiply, R.id.btn_divide,
                R.id.btn_clear, R.id.btn_parentheses, R.id.btn_percent,
                R.id.btn_decimal, R.id.btn_backspace, R.id.btn_equals
        };

        for (int i = 0; i < buttonIds.length; i++) {
            findViewById(buttonIds[i]).setOnClickListener(this);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.btn_clear) {
            expression = "";
            isResultCommitted = false;
            tvExpression.setText("");
            tvResult.setText("0");
        } else if (id == R.id.btn_backspace) {
            if (isResultCommitted) {
                expression = "";
                isResultCommitted = false;
            } else if (expression.length() > 0) {
                expression = expression.substring(0, expression.length() - 1);
            }
            updateDisplays();
        } else if (id == R.id.btn_equals) {
            evaluateFinal();
        } else if (id == R.id.btn_parentheses) {
            handleParentheses();
        } else {
            String token = ((TextView) v).getText().toString();
            handleInput(token);
        }
    }

    private void handleInput(String token) {
        if (isResultCommitted) {
            if (isOperator(token)) {
                expression = tvResult.getText().toString() + token;
            } else {
                expression = token;
            }
            isResultCommitted = false;
        } else {
            if (expression.equals("0") && !isOperator(token) && !token.equals(".")) {
                expression = token;
            } else {
                if (isOperator(token) && expression.length() > 0) {
                    char lastChar = expression.charAt(expression.length() - 1);
                    if (isOperator(String.valueOf(lastChar))) {
                        expression = expression.substring(0, expression.length() - 1);
                    }
                }
                expression += token;
            }
        }
        updateDisplays();
    }

    private boolean isOperator(String s) {
        return s.equals("+") || s.equals("−") || s.equals("×") || s.equals("÷") || s.equals("-") || s.equals("*") || s.equals("/");
    }

    private void handleParentheses() {
        if (isResultCommitted) {
            expression = "(";
            isResultCommitted = false;
        } else {
            if (expression.isEmpty() || expression.equals("0")) {
                expression = "(";
            } else {
                int openCount = 0;
                for (int i = 0; i < expression.length(); i++) {
                    if (expression.charAt(i) == '(') {
                        openCount++;
                    } else if (expression.charAt(i) == ')') {
                        openCount--;
                    }
                }
                char lastChar = expression.charAt(expression.length() - 1);
                if (openCount > 0 && (Character.isDigit(lastChar) || lastChar == ')')) {
                    expression += ")";
                } else {
                    expression += "(";
                }
            }
        }
        updateDisplays();
    }

    private void updateDisplays() {
        tvExpression.setText(expression);
        if (expression.isEmpty()) {
            tvResult.setText("0");
            return;
        }

        try {
            String cleanExpr = expression;
            if (cleanExpr.length() > 0) {
                char lastChar = cleanExpr.charAt(cleanExpr.length() - 1);
                if (isOperator(String.valueOf(lastChar)) || lastChar == '(') {
                    cleanExpr = cleanExpr.substring(0, cleanExpr.length() - 1);
                }
            }

            if (!cleanExpr.isEmpty()) {
                double res = eval(cleanExpr);
                tvResult.setText(formatResult(res));
            }
        } catch (Exception e) {
            // Fallback gracefully during mid-expression typing
        }
    }

    private void evaluateFinal() {
        if (expression.isEmpty()) return;

        try {
            double res = eval(expression);
            String formatted = formatResult(res);
            tvExpression.setText(expression + " =");
            tvResult.setText(formatted);
            isResultCommitted = true;
        } catch (Exception e) {
            tvResult.setText("Error");
            isResultCommitted = true;
        }
    }

    private String formatResult(double value) {
        if (Double.isInfinite(value) || Double.isNaN(value)) {
            return "Error";
        }
        if (value == (long) value) {
            return String.format("%d", (long) value);
        } else {
            DecimalFormat df = new DecimalFormat("#.##########");
            return df.format(value);
        }
    }

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
                if (pos < str.length()) throw new RuntimeException("Unexpected: " + (char)ch);
                return x;
            }

            double parseExpression() {
                double x = parseTerm();
                for (;;) {
                    if      (eat('+')) x += parseTerm();
                    else if (eat('-') || eat('−')) x -= parseTerm();
                    else return x;
                }
            }

            double parseTerm() {
                double x = parseFactor();
                for (;;) {
                    if      (eat('*') || eat('×')) x *= parseFactor();
                    else if (eat('/') || eat('÷')) {
                        double divisor = parseFactor();
                        if (divisor == 0) throw new ArithmeticException("Divide by zero");
                        x /= divisor;
                    }
                    else return x;
                }
            }

            double parseFactor() {
                if (eat('+')) return parseFactor();
                if (eat('-') || eat('−')) return -parseFactor();

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
