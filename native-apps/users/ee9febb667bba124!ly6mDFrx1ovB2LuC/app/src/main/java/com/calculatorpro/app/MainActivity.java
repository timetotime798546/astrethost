package com.calculatorpro.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity implements View.OnClickListener {

    private TextView tvExpression;
    private TextView tvResult;
    private StringBuilder expressionStr;
    private boolean isResultDisplayed;

    @Override
    protected void onCreate(Bundle savedInstanceState) { 
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        expressionStr = new StringBuilder();
        isResultDisplayed = false;

        tvExpression = (TextView) findViewById(R.id.tvExpression);
        tvResult = (TextView) findViewById(R.id.tvResult);

        int[] buttonIds = new int[]{
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9,
            R.id.btnDot, R.id.btnPlus, R.id.btnMinus, R.id.btnMultiply,
            R.id.btnDivide, R.id.btnBracketOpen, R.id.btnBracketClose,
            R.id.btnClear, R.id.btnDelete, R.id.btnEquals
        };

        for (int id : buttonIds) {
            View btn = findViewById(id);
            if (btn != null) {
                btn.setOnClickListener(this);
            }
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.btnClear) {
            expressionStr.setLength(0);
            tvExpression.setText("");
            tvResult.setText("0");
            isResultDisplayed = false;
        } else if (id == R.id.btnDelete) {
            if (isResultDisplayed) {
                expressionStr.setLength(0);
                tvExpression.setText("");
                tvResult.setText("0");
                isResultDisplayed = false;
            } else if (expressionStr.length() > 0) {
                expressionStr.deleteCharAt(expressionStr.length() - 1);
                tvExpression.setText(expressionStr.toString());
            }
        } else if (id == R.id.btnEquals) {
            String exp = expressionStr.toString();
            if (exp.trim().isEmpty()) {
                return;
            }
            try {
                double val = eval(exp);
                String resultStr;
                if (val == (long) val) {
                    resultStr = String.valueOf((long) val);
                } else {
                    resultStr = String.valueOf(val);
                }
                tvResult.setText(resultStr);
                isResultDisplayed = true;
            } catch (Exception e) {
                tvResult.setText("Error");
                isResultDisplayed = true;
            }
        } else {
            if (isResultDisplayed) {
                Button b = (Button) v;
                String buttonText = b.getText().toString();
                boolean isOperator = buttonText.equals("+") || buttonText.equals("-") ||
                                     buttonText.equals("*") || buttonText.equals("/");
                if (isOperator) {
                    expressionStr.setLength(0);
                    expressionStr.append(tvResult.getText().toString());
                    expressionStr.append(buttonText);
                } else {
                    expressionStr.setLength(0);
                    expressionStr.append(buttonText);
                }
                isResultDisplayed = false;
            } else {
                Button b = (Button) v;
                expressionStr.append(b.getText().toString());
            }
            tvExpression.setText(expressionStr.toString());
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
                if (pos < str.length()) throw new RuntimeException("Unexpected: " + (char) ch);
                return x;
            }

            double parseExpression() {
                double x = parseTerm();
                for (;;) {
                    if (eat('+')) x += parseTerm();
                    else if (eat('-')) x -= parseTerm();
                    else return x;
                } 
            }

            double parseTerm() {
                double x = parseFactor();
                for (;;) {
                    if (eat('*')) x *= parseFactor();
                    else if (eat('/')) {
                        double divisor = parseFactor();
                        if (divisor == 0) throw new ArithmeticException("Division by zero");
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
                    x = Double.parseDouble(str.substring(startPos, this.pos));
                } else {
                    throw new RuntimeException("Unexpected: " + (char) ch);
                }

                return x;
            }
        }.parse();
    }
}
