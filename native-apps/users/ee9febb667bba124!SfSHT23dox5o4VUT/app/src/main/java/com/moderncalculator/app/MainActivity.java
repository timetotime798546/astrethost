package com.moderncalculator.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.text.DecimalFormat;

public class MainActivity extends Activity {

    private TextView tvEquation;
    private TextView tvResult;
    private StringBuilder currentInput;
    private boolean hasResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvEquation = (TextView) findViewById(R.id.tv_equation);
        tvResult = (TextView) findViewById(R.id.tv_result);
        currentInput = new StringBuilder();

        setupButtons();
    }

    private void setupButtons() {
        int[] numberIds = {
            R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3,
            R.id.btn_4, R.id.btn_5, R.id.btn_6, R.id.btn_7,
            R.id.btn_8, R.id.btn_9, R.id.btn_dot
        };

        View.OnClickListener numberClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (hasResult) {
                    currentInput.setLength(0);
                    hasResult = false;
                }
                Button b = (Button) v;
                String val = b.getText().toString();
                if (val.equals(".")) {
                    if (canAddDecimal()) {
                        currentInput.append(val);
                    }
                } else {
                    currentInput.append(val);
                }
                updateDisplay();
            }
        };

        for (int id : numberIds) {
            findViewById(id).setOnClickListener(numberClickListener);
        }

        int[] opIds = {
            R.id.btn_add, R.id.btn_subtract, R.id.btn_multiply, R.id.btn_divide
        };

        View.OnClickListener opClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hasResult = false;
                Button b = (Button) v;
                String op = b.getText().toString();
                if (currentInput.length() > 0) {
                    char lastChar = currentInput.charAt(currentInput.length() - 1);
                    if (isOperator(lastChar)) {
                        currentInput.setLength(currentInput.length() - 1);
                    }
                    currentInput.append(op);
                    updateDisplay();
                }
            }
        };

        for (int id : opIds) {
            findViewById(id).setOnClickListener(opClickListener);
        }

        findViewById(R.id.btn_clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentInput.setLength(0);
                tvEquation.setText("");
                tvResult.setText("0");
                hasResult = false;
            }
        });

        findViewById(R.id.btn_delete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentInput.length() > 0) {
                    currentInput.setLength(currentInput.length() - 1);
                    updateDisplay();
                }
            }
        });

        findViewById(R.id.btn_plus_minus).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleSign();
            }
        });

        findViewById(R.id.btn_percent).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyPercent();
            }
        });

        findViewById(R.id.btn_equal).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calculate();
            }
        });
    }

    private boolean canAddDecimal() {
        String input = currentInput.toString();
        if (input.isEmpty()) return true;
        int lastOp = -1;
        for (int i = input.length() - 1; i >= 0; i--) {
            if (isOperator(input.charAt(i))) {
                lastOp = i;
                break;
            }
        }
        String lastNum = input.substring(lastOp + 1);
        return !lastNum.contains(".");
    }

    private boolean isOperator(char c) {
        return c == '+' || c == '-' || c == '×' || c == '÷';
    }

    private void updateDisplay() {
        if (currentInput.length() == 0) {
            tvResult.setText("0");
        } else {
            tvResult.setText(currentInput.toString());
        }
    }

    private void toggleSign() {
        String input = currentInput.toString();
        if (input.isEmpty()) return;

        int lastOp = -1;
        for (int i = input.length() - 1; i >= 0; i--) {
            char c = input.charAt(i);
            if (isOperator(c)) {
                if (i == 0 || isOperator(input.charAt(i - 1))) {
                    continue;
                }
                lastOp = i;
                break;
            }
        }

        if (lastOp == -1) {
            if (input.startsWith("-")) {
                currentInput.delete(0, 1);
            } else {
                currentInput.insert(0, "-");
            }
        } else {
            String part1 = input.substring(0, lastOp + 1);
            String part2 = input.substring(lastOp + 1);
            if (part2.startsWith("-")) {
                part2 = part2.substring(1);
            } else {
                part2 = "-" + part2;
            }
            currentInput.setLength(0);
            currentInput.append(part1).append(part2);
        }
        updateDisplay();
    }

    private void applyPercent() {
        String input = currentInput.toString();
        if (input.isEmpty()) return;

        int lastOp = -1;
        for (int i = input.length() - 1; i >= 0; i--) {
            if (isOperator(input.charAt(i))) {
                lastOp = i;
                break;
            }
        }

        String lastNumStr = input.substring(lastOp + 1);
        if (!lastNumStr.isEmpty()) {
            try {
                double val = Double.parseDouble(lastNumStr) / 100.0;
                DecimalFormat df = new DecimalFormat("#.#######");
                String resultStr = df.format(val);
                currentInput.setLength(0);
                currentInput.append(input.substring(0, lastOp + 1)).append(resultStr);
                updateDisplay();
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    private void calculate() {
        String expression = currentInput.toString();
        if (expression.isEmpty()) return;

        String evalExpr = expression.replace("×", "*").replace("÷", "/");

        try {
            double result = eval(evalExpr);
            DecimalFormat df = new DecimalFormat("#.########");
            String finalResult = df.format(result);

            tvEquation.setText(expression);
            tvResult.setText(finalResult);

            currentInput.setLength(0);
            currentInput.append(finalResult);
            hasResult = true;
        } catch (Exception e) {
            tvResult.setText("Error");
            currentInput.setLength(0);
            hasResult = true;
        }
    }

    private static double eval(final String str) {
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
                        if (divisor == 0) throw new ArithmeticException("Divide by zero");
                        x /= divisor;
                    }
                    else return x;
                }
            }

            double parseFactor() {
                if (eat('+')) return +parseFactor();
                if (eat('-')) return -parseFactor();

                double x;
                int startPos = this.pos;
                if (eat('(')) {
                    x = parseExpression();
                    if (!eat(')')) throw new RuntimeException("Missing closing parenthesis");
                } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(str.substring(startPos, this.pos));
                } else {
                    throw new RuntimeException("Unexpected character: " + (char)ch);
                }

                return x;
            }
        }.parse();
    }
}