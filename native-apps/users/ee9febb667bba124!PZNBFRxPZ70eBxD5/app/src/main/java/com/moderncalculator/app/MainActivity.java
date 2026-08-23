package com.moderncalculator.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends Activity {

    private TextView tvExpression;
    private TextView tvResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvExpression = (TextView) findViewById(R.id.tv_expression);
        tvResult = (TextView) findViewById(R.id.tv_result);

        // Bind Digits
        setDigitBtn(R.id.btn_0, "0");
        setDigitBtn(R.id.btn_1, "1");
        setDigitBtn(R.id.btn_2, "2");
        setDigitBtn(R.id.btn_3, "3");
        setDigitBtn(R.id.btn_4, "4");
        setDigitBtn(R.id.btn_5, "5");
        setDigitBtn(R.id.btn_6, "6");
        setDigitBtn(R.id.btn_7, "7");
        setDigitBtn(R.id.btn_8, "8");
        setDigitBtn(R.id.btn_9, "9");

        // Bind Operations
        setOperatorBtn(R.id.btn_add, "+");
        setOperatorBtn(R.id.btn_subtract, "-");
        setOperatorBtn(R.id.btn_multiply, "*");
        setOperatorBtn(R.id.btn_divide, "/");
        setOperatorBtn(R.id.btn_parenthesis_open, "(");
        setOperatorBtn(R.id.btn_parenthesis_close, ")");
        setDigitBtn(R.id.btn_decimal, ".");

        // Clear and Delete Action Bindings
        findViewById(R.id.btn_clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearExpression();
            }
        });

        findViewById(R.id.btn_backspace).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                backspace();
            }
        });

        findViewById(R.id.btn_equals).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                evaluate();
            }
        });
    }

    private void setDigitBtn(int resId, final String value) {
        findViewById(resId).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                appendToExpression(value);
            }
        });
    }

    private void setOperatorBtn(int resId, final String value) {
        findViewById(resId).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                appendOperatorToExpression(value);
            }
        });
    }

    private void appendToExpression(String value) {
        String current = tvExpression.getText().toString();
        current += value;
        tvExpression.setText(current);
    }

    private void appendOperatorToExpression(String value) {
        String current = tvExpression.getText().toString();
        if (current.isEmpty()) {
            if (value.equals("-") || value.equals("(")) {
                current += value;
            }
        } else {
            char lastChar = current.charAt(current.length() - 1);
            if (isOperatorChar(lastChar)) {
                if (isReplacingSafe(lastChar, value.charAt(0))) {
                    current = current.substring(0, current.length() - 1) + value;
                } else {
                    current += value;
                }
            } else {
                current += value;
            }
        }
        tvExpression.setText(current);
    }

    private boolean isOperatorChar(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/' || c == '%';
    }

    private boolean isReplacingSafe(char current, char incoming) {
        return (current == '+' || current == '-' || current == '*' || current == '/') && 
               (incoming == '+' || incoming == '-' || incoming == '*' || incoming == '/');
    }

    private void backspace() {
        String current = tvExpression.getText().toString();
        if (!current.isEmpty()) {
            current = current.substring(0, current.length() - 1);
            tvExpression.setText(current);
        }
    }

    private void clearExpression() {
        tvExpression.setText("");
        tvResult.setText("0");
    }

    private void evaluate() {
        String expression = tvExpression.getText().toString();
        if (expression.isEmpty()) {
            return;
        }
        try {
            double result = eval(expression);
            tvResult.setText(formatResult(result));
        } catch (Exception e) {
            tvResult.setText("Error");
        }
    }

    private String formatResult(double result) {
        if (Double.isInfinite(result) || Double.isNaN(result)) {
            return "Error";
        }
        if (result == (long) result) {
            return String.valueOf((long) result);
        }
        String s = String.valueOf(result);
        if (s.length() > 15) {
            return String.format("%.8g", result);
        }
        return s;
    }

    public static double eval(final String str) {
        return new Object() {
            int pos = -1, ch;

            void nextChar() {
                ch = (++pos < str.length()) ? str.charAt(pos) : -1;
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
                if (pos < str.length()) throw new RuntimeException("Unexpected: " + (char)ch);
                return x;
            }

            double parseExpression() {
                double x = parseTerm();
                for (;;) {
                    if      (consume('+')) x += parseTerm();
                    else if (consume('-')) x -= parseTerm();
                    else return x;
                } 
            }

            double parseTerm() {
                double x = parseFactor();
                for (;;) {
                    if      (consume('*')) x *= parseFactor();
                    else if (consume('/')) x /= parseFactor();
                    else if (consume('%')) x %= parseFactor();
                    else return x;
                }
            }

            double parseFactor() {
                if (consume('+')) return +parseFactor();
                if (consume('-')) return -parseFactor();

                double x;
                int startPos = this.pos;
                if (consume('(')) {
                    x = parseExpression();
                    if (!consume(')')) throw new RuntimeException("Missing closing parenthesis");
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