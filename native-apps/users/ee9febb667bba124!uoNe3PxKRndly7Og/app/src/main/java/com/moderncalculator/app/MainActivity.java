package com.moderncalculator.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.text.DecimalFormat;
import java.util.Stack;

public class MainActivity extends Activity {

    private TextView tvExpression;
    private TextView tvResult;
    private StringBuilder currentInput;
    private boolean isResultDisplayed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvExpression = findViewById(R.id.tvExpression);
        tvResult = findViewById(R.id.tvResult);
        currentInput = new StringBuilder();
        isResultDisplayed = false;

        // Binding dynamic numeric input key sequences
        setNumberClickListener(R.id.btn0, "0");
        setNumberClickListener(R.id.btn1, "1");
        setNumberClickListener(R.id.btn2, "2");
        setNumberClickListener(R.id.btn3, "3");
        setNumberClickListener(R.id.btn4, "4");
        setNumberClickListener(R.id.btn5, "5");
        setNumberClickListener(R.id.btn6, "6");
        setNumberClickListener(R.id.btn7, "7");
        setNumberClickListener(R.id.btn8, "8");
        setNumberClickListener(R.id.btn9, "9");
        setNumberClickListener(R.id.btnDot, ".");

        // Binding functional arithmetic operators
        setOperatorClickListener(R.id.btnPlus, " + ");
        setOperatorClickListener(R.id.btnMinus, " - ");
        setOperatorClickListener(R.id.btnMultiply, " × ");
        setOperatorClickListener(R.id.btnDivide, " ÷ ");

        // Utility actions
        findViewById(R.id.btnClear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clear();
            }
        });

        findViewById(R.id.btnBackspace).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                backspace();
            }
        });

        findViewById(R.id.btnPercent).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyPercent();
            }
        });

        findViewById(R.id.btnToggleSign).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleSign();
            }
        });

        findViewById(R.id.btnEqual).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calculateResult();
            }
        });
    }

    private void setNumberClickListener(int resId, final String value) {
        findViewById(resId).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isResultDisplayed) {
                    currentInput.setLength(0);
                    isResultDisplayed = false;
                }
                if (value.equals(".")) {
                    if (canAddDecimal()) {
                        currentInput.append(".");
                    }
                } else {
                    currentInput.append(value);
                }
                updateDisplay();
                tryPreviewResult();
            }
        });
    }

    private boolean canAddDecimal() {
        String input = currentInput.toString();
        if (input.isEmpty()) {
            currentInput.append("0");
            return true;
        }
        int lastSpace = input.lastIndexOf(' ');
        String currentPart = lastSpace == -1 ? input : input.substring(lastSpace + 1);
        return !currentPart.contains(".");
    }

    private void setOperatorClickListener(int resId, final String op) {
        findViewById(resId).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isResultDisplayed) {
                    isResultDisplayed = false;
                }
                if (currentInput.length() == 0) {
                    return;
                }
                String input = currentInput.toString();
                if (input.endsWith(" ")) {
                    currentInput.setLength(input.length() - 3);
                }
                currentInput.append(op);
                updateDisplay();
            }
        });
    }

    private void clear() {
        currentInput.setLength(0);
        tvExpression.setText("");
        tvResult.setText("0");
        isResultDisplayed = false;
    }

    private void backspace() {
        if (isResultDisplayed) {
            clear();
            return;
        }
        String input = currentInput.toString();
        if (input.length() > 0) {
            if (input.endsWith(" ")) {
                currentInput.setLength(input.length() - 3);
            } else {
                currentInput.setLength(input.length() - 1);
            }
            updateDisplay();
            tryPreviewResult();
        }
    }

    private void applyPercent() {
        if (currentInput.length() == 0) return;
        String input = currentInput.toString();
        int lastSpace = input.lastIndexOf(' ');
        String numberPart = lastSpace == -1 ? input : input.substring(lastSpace + 1);
        if (!numberPart.isEmpty() && !numberPart.contains(" ")) {
            try {
                double val = Double.parseDouble(numberPart) / 100.0;
                currentInput.setLength(lastSpace == -1 ? 0 : lastSpace + 1);
                currentInput.append(formatNumber(val));
                updateDisplay();
                tryPreviewResult();
            } catch (NumberFormatException e) {
                // Suppress parse failures
            }
        }
    }

    private void toggleSign() {
        if (currentInput.length() == 0) return;
        String input = currentInput.toString();
        int lastSpace = input.lastIndexOf(' ');
        String numberPart = lastSpace == -1 ? input : input.substring(lastSpace + 1);
        if (!numberPart.isEmpty()) {
            try {
                double val = Double.parseDouble(numberPart) * -1;
                currentInput.setLength(lastSpace == -1 ? 0 : lastSpace + 1);
                currentInput.append(formatNumber(val));
                updateDisplay();
                tryPreviewResult();
            } catch (NumberFormatException e) {
                // Suppress parse failures
            }
        }
    }

    private void updateDisplay() {
        tvExpression.setText(currentInput.toString());
    }

    private void tryPreviewResult() {
        String input = currentInput.toString().trim();
        if (input.isEmpty()) {
            tvResult.setText("0");
            return;
        }
        if (!input.contains(" ")) {
            tvResult.setText(input);
            return;
        }
        if (input.endsWith("×") || input.endsWith("÷") || input.endsWith("+") || input.endsWith("-")) {
            int lastSpace = input.lastIndexOf(' ');
            if (lastSpace != -1) {
                input = input.substring(0, lastSpace).trim();
            }
        }
        try {
            double res = evaluate(input);
            tvResult.setText(formatNumber(res));
        } catch (Exception e) {
            // Safe state: avoid refreshing during non-completed operation sequences.
        }
    }

    private void calculateResult() {
        String input = currentInput.toString().trim();
        if (input.isEmpty()) return;
        try {
            double res = evaluate(input);
            tvExpression.setText(input);
            tvResult.setText(formatNumber(res));
            currentInput.setLength(0);
            currentInput.append(formatNumber(res));
            isResultDisplayed = true;
        } catch (Exception e) {
            tvResult.setText("Error");
            isResultDisplayed = true;
        }
    }

    private String formatNumber(double value) {
        if (value == (long) value) {
            return String.format("%d", (long) value);
        } else {
            DecimalFormat df = new DecimalFormat("#.########");
            return df.format(value);
        }
    }

    private double evaluate(String expression) throws Exception {
        String[] tokens = expression.split("\\s+");
        Stack<Double> values = new Stack<Double>();
        Stack<String> ops = new Stack<String>();

        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            if (token.isEmpty()) continue;

            if (isNumber(token)) {
                values.push(Double.parseDouble(token));
            } else if (token.equals("+") || token.equals("-") || token.equals("×") || token.equals("÷")) {
                while (!ops.empty() && hasPrecedence(token, ops.peek())) {
                    values.push(applyOp(ops.pop(), values.pop(), values.pop()));
                }
                ops.push(token);
            }
        }

        while (!ops.empty()) {
            values.push(applyOp(ops.pop(), values.pop(), values.pop()));
        }

        if (values.empty()) return 0;
        return values.pop();
    }

    private boolean isNumber(String token) {
        try {
            Double.parseDouble(token);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean hasPrecedence(String op1, String op2) {
        if ((op1.equals("×") || op1.equals("÷")) && (op2.equals("+") || op2.equals("-"))) {
            return false;
        }
        return true;
    }

    private double applyOp(String op, double b, double a) {
        if (op.equals("+")) return a + b;
        if (op.equals("-")) return a - b;
        if (op.equals("×")) return a * b;
        if (op.equals("÷")) {
            if (b == 0) {
                throw new ArithmeticException("Cannot divide by zero");
            }
            return a / b;
        }
        return 0;
    }
}