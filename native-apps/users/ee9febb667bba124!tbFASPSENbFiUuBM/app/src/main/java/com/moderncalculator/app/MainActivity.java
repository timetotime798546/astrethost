package com.moderncalculator.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {

    private TextView tvFormula;
    private TextView tvDisplay;
    private StringBuilder currentExpression = new StringBuilder();
    private boolean isResultState = false;
    private boolean isErrorState = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvFormula = (TextView) findViewById(R.id.tvFormula);
        tvDisplay = (TextView) findViewById(R.id.tvDisplay);

        int[] numericButtons = {
            R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4,
            R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9
        };

        View.OnClickListener numericListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isErrorState || isResultState) {
                    currentExpression.setLength(0);
                    isErrorState = false;
                    isResultState = false;
                }
                Button b = (Button) v;
                currentExpression.append(b.getText().toString());
                updateDisplay();
            }
        };

        for (int id : numericButtons) {
            findViewById(id).setOnClickListener(numericListener);
        }

        int[] operatorButtons = {
            R.id.btn_add, R.id.btn_subtract, R.id.btn_multiply, R.id.btn_divide,
            R.id.btn_bracket_open, R.id.btn_bracket_close, R.id.btn_dot
        };

        View.OnClickListener operatorListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isErrorState) {
                    return;
                }
                if (isResultState) {
                    isResultState = false;
                }
                Button b = (Button) v;
                String op = b.getText().toString();
                currentExpression.append(op);
                updateDisplay();
            }
        };

        for (int id : operatorButtons) {
            findViewById(id).setOnClickListener(operatorListener);
        }

        findViewById(R.id.btn_c).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentExpression.setLength(0);
                tvFormula.setText("");
                tvDisplay.setText("0");
                isErrorState = false;
                isResultState = false;
            }
        });

        findViewById(R.id.btn_backspace).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isErrorState) {
                    currentExpression.setLength(0);
                    tvDisplay.setText("0");
                    isErrorState = false;
                    return;
                }
                if (isResultState) {
                    tvFormula.setText("");
                    isResultState = false;
                }
                int len = currentExpression.length();
                if (len > 0) {
                    currentExpression.setLength(len - 1);
                }
                updateDisplay();
            }
        });

        findViewById(R.id.btn_equals).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isErrorState || currentExpression.length() == 0) {
                    return;
                }
                String exprStr = currentExpression.toString();
                try {
                    double result = evalExpression(exprStr);
                    tvFormula.setText(exprStr);

                    String resultString;
                    if (result == (long) result) {
                        resultString = String.format("%d", (long) result);
                    } else {
                        resultString = String.valueOf(result);
                        if (resultString.length() > 12) {
                            resultString = String.format("%.8f", result);
                            resultString = resultString.replaceAll("0+$", "").replaceAll("\\.$", "");
                        }
                    }

                    tvDisplay.setText(resultString);
                    currentExpression.setLength(0);
                    currentExpression.append(resultString);
                    isResultState = true;
                } catch (Exception e) {
                    tvDisplay.setText("Error");
                    isErrorState = true;
                }
            }
        });
    }

    private void updateDisplay() {
        if (currentExpression.length() == 0) {
            tvDisplay.setText("0");
        } else {
            tvDisplay.setText(currentExpression.toString());
        }
    }

    private double evalExpression(String expr) {
        String cleaned = expr.replace("×", "*")
                             .replace("÷", "/")
                             .replace("−", "-");
        return eval(cleaned);
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
                    throw new RuntimeException("Unexpected character: " + (char) ch);
                }

                return x;
            }
        }.parse();
    }
}