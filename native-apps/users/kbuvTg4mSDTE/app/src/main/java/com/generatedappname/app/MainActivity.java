package com.generatedappname.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity implements View.OnClickListener {

    private TextView display;
    private StringBuilder currentInput = new StringBuilder();
    private double operand1 = Double.NaN;
    private double operand2;
    private char pendingOperator;
    private boolean resetInput = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        display = findViewById(R.id.display);

        int[] buttonIds = {
                R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3,
                R.id.btn_4, R.id.btn_5, R.id.btn_6, R.id.btn_7,
                R.id.btn_8, R.id.btn_9, R.id.btn_dot,
                R.id.btn_add, R.id.btn_sub, R.id.btn_mul, R.id.btn_div,
                R.id.btn_eq, R.id.btn_clear, R.id.btn_plus_minus, R.id.btn_percent
        };

        for (int id : buttonIds) {
            View v = findViewById(id);
            if (v != null) {
                v.setOnClickListener(this);
            }
        }

        updateDisplay("0");
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.btn_0: case R.id.btn_1: case R.id.btn_2: case R.id.btn_3:
            case R.id.btn_4: case R.id.btn_5: case R.id.btn_6: case R.id.btn_7:
            case R.id.btn_8: case R.id.btn_9:
                numberPressed(((Button) v).getText().toString());
                break;
            case R.id.btn_dot:
                decimalPressed();
                break;
            case R.id.btn_add:
                operatorPressed('+');
                break;
            case R.id.btn_sub:
                operatorPressed('-');
                break;
            case R.id.btn_mul:
                operatorPressed('*');
                break;
            case R.id.btn_div:
                operatorPressed('/');
                break;
            case R.id.btn_eq:
                equalsPressed();
                break;
            case R.id.btn_clear:
                clearAll();
                break;
            case R.id.btn_plus_minus:
                plusMinusPressed();
                break;
            case R.id.btn_percent:
                percentPressed();
                break;
        }
    }

    private void numberPressed(String digit) {
        if (resetInput) {
            currentInput.setLength(0);
            resetInput = false;
        }
        if (currentInput.length() == 1 && currentInput.charAt(0) == '0' && !digit.equals(".")) {
            currentInput.setLength(0);
        }
        currentInput.append(digit);
        updateDisplay(currentInput.toString());
    }

    private void decimalPressed() {
        if (resetInput) {
            currentInput.setLength(0);
            currentInput.append("0");
            resetInput = false;
        }
        if (currentInput.indexOf(".") == -1) {
            if (currentInput.length() == 0) {
                currentInput.append("0");
            }
            currentInput.append(".");
            updateDisplay(currentInput.toString());
        }
    }

    private void operatorPressed(char op) {
        if (!Double.isNaN(operand1)) {
            compute();
        } else {
            operand1 = parseInput();
        }
        pendingOperator = op;
        resetInput = true;
    }

    private void equalsPressed() {
        if (!Double.isNaN(operand1) && !resetInput) {
            compute();
            pendingOperator = '\0';
            operand1 = Double.NaN;
        }
    }

    private void compute() {
        operand2 = parseInput();
        double result = 0.0;
        switch (pendingOperator) {
            case '+':
                result = operand1 + operand2;
                break;
            case '-':
                result = operand1 - operand2;
                break;
            case '*':
                result = operand1 * operand2;
                break;
            case '/':
                if (operand2 != 0) {
                    result = operand1 / operand2;
                } else {
                    updateDisplay("Error");
                    clearAll();
                    return;
                }
                break;
        }
        updateDisplay(trimResult(result));
        operand1 = result;
        resetInput = true;
    }

    private double parseInput() {
        try {
            return Double.parseDouble(currentInput.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void clearAll() {
        currentInput.setLength(0);
        operand1 = Double.NaN;
        pendingOperator = '\0';
        resetInput = false;
        updateDisplay("0");
    }

    private void plusMinusPressed() {
        if (currentInput.length() == 0) {
            return;
        }
        if (currentInput.charAt(0) == '-') {
            currentInput.deleteCharAt(0);
        } else {
            currentInput.insert(0, '-');
        }
        updateDisplay(currentInput.toString());
    }

    private void percentPressed() {
        double value = parseInput() / 100.0;
        currentInput.setLength(0);
        currentInput.append(trimResult(value));
        updateDisplay(currentInput.toString());
    }

    private void updateDisplay(String text) {
        display.setText(text);
    }

    private String trimResult(double value) {
        if (value == (long) value) {
            return String.format("%d", (long) value);
        } else {
            return String.format("%s", value);
        }
    }
}
