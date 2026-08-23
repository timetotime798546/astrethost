package com.calculatorplus.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {

    private TextView tvFormula;
    private TextView tvDisplay;
    private StringBuilder inputExpression = new StringBuilder();
    private boolean isResultDisplayed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvFormula = (TextView) findViewById(R.id.tvFormula);
        tvDisplay = (TextView) findViewById(R.id.tvDisplay);

        setupButtons();
    }

    private void setupButtons() {
        int[] numericButtons = {
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9,
            R.id.btnDot
        };

        View.OnClickListener numListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button b = (Button) v;
                if (isResultDisplayed) {
                    inputExpression.setLength(0);
                    isResultDisplayed = false;
                }
                String text = b.getText().toString();
                if (text.equals(".") && hasLastTokenDecimal()) {
                    return;
                }
                inputExpression.append(text);
                updateDisplay();
            }
        };

        for (int id : numericButtons) {
            findViewById(id).setOnClickListener(numListener);
        }

        int[] operatorButtons = {
            R.id.btnAdd, R.id.btnSubtract, R.id.btnMultiply, R.id.btnDivide, R.id.btnPercent
        };

        View.OnClickListener opListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button b = (Button) v;
                if (isResultDisplayed) {
                    isResultDisplayed = false;
                }
                String opSymbol = b.getText().toString();
                if (opSymbol.equals("×")) {
                    opSymbol = "*";
                } else if (opSymbol.equals("÷")) {
                    opSymbol = "/";
                }
                
                if (inputExpression.length() > 0) {
                    char lastChar = inputExpression.charAt(inputExpression.length() - 1);
                    if (isOperator(lastChar)) {
                        inputExpression.setLength(inputExpression.length() - 1);
                    }
                }
                inputExpression.append(opSymbol);
                updateDisplay();
            }
        };

        for (int id : operatorButtons) {
            findViewById(id).setOnClickListener(opListener);
        }

        findViewById(R.id.btnClear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                inputExpression.setLength(0);
                tvFormula.setText("");
                tvDisplay.setText("0");
                isResultDisplayed = false;
            }
        });

        findViewById(R.id.btnBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isResultDisplayed) {
                    inputExpression.setLength(0);
                    isResultDisplayed = false;
                } else if (inputExpression.length() > 0) {
                    inputExpression.setLength(inputExpression.length() - 1);
                }
                updateDisplay();
            }
        });

        findViewById(R.id.btnBrackets).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isResultDisplayed) {
                    inputExpression.setLength(0);
                    isResultDisplayed = false;
                }
                int openCount = 0;
                int closeCount = 0;
                String expr = inputExpression.toString();
                for (int i = 0; i < expr.length(); i++) {
                    if (expr.charAt(i) == '(') openCount++;
                    if (expr.charAt(i) == ')') closeCount++;
                }

                if (openCount == closeCount || (expr.length() > 0 && isOperator(expr.charAt(expr.length() - 1))) || (expr.length() > 0 && expr.charAt(expr.length() - 1) == '(')) {
                    inputExpression.append("(");
                } else if (openCount > closeCount && expr.length() > 0 && !isOperator(expr.charAt(expr.length() - 1)) && expr.charAt(expr.length() - 1) != '(') {
                    inputExpression.append(")");
                }
                updateDisplay();
            }
        });

        findViewById(R.id.btnEquals).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (inputExpression.length() == 0) return;
                String rawExpression = inputExpression.toString();
                try {
                    double result = evaluate(rawExpression);
                    tvFormula.setText(formatExpressionDisplay(rawExpression));
                    
                    if (result == (long) result) {
                        tvDisplay.setText(String.format("%d", (long) result));
                    } else {
                        tvDisplay.setText(String.valueOf(result));
                    }
                    
                    inputExpression.setLength(0);
                    if (result == (long) result) {
                        inputExpression.append((long) result);
                    } else {
                        inputExpression.append(result);
                    }
                    isResultDisplayed = true;
                } catch (Exception e) {
                    tvDisplay.setText("Error");
                    isResultDisplayed = true;
                }
            }
        });
    }

    private boolean isOperator(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/' || c == '%';
    }

    private boolean hasLastTokenDecimal() {
        String expr = inputExpression.toString();
        if (expr.isEmpty()) return false;
        for (int i = expr.length() - 1; i >= 0; i--) {
            char c = expr.charAt(i);
            if (c == '.') return true;
            if (isOperator(c) || c == '(' || c == ')') return false;
        }
        return false;
    }

    private void updateDisplay() {
        if (inputExpression.length() == 0) {
            tvDisplay.setText("0");
        } else {
            tvDisplay.setText(formatExpressionDisplay(inputExpression.toString()));
        }
    }

    private String formatExpressionDisplay(String expr) {
        return expr.replace("*", "×").replace("/", "÷");
    }

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
                    } else if (eat('%')) {
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

                return x;
            }
        }.parse();
    }
}