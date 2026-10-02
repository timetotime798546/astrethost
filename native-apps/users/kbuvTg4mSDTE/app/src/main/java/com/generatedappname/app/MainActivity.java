package com.generatedappname.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity implements View.OnClickListener {

    private TextView display;
    private StringBuilder input = new StringBuilder();
    private double operand = 0;
    private char pendingOperator = 0;
    private boolean resetInput = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        display = (TextView) findViewById(R.id.display);
        int[] ids = {
                R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
                R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9,
                R.id.btnAdd, R.id.btnSub, R.id.btnMul, R.id.btnDiv,
                R.id.btnEq, R.id.btnClear, R.id.btnDot, R.id.btnBack
        };
        for (int id : ids) {
            View v = findViewById(id);
            if (v != null) {
                v.setOnClickListener(this);
            }
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.btn0) {
            appendDigit("0");
        } else if (id == R.id.btn1) {
            appendDigit("1");
        } else if (id == R.id.btn2) {
            appendDigit("2");
        } else if (id == R.id.btn3) {
            appendDigit("3");
        } else if (id == R.id.btn4) {
            appendDigit("4");
        } else if (id == R.id.btn5) {
            appendDigit("5");
        } else if (id == R.id.btn6) {
            appendDigit("6");
        } else if (id == R.id.btn7) {
            appendDigit("7");
        } else if (id == R.id.btn8) {
            appendDigit("8");
        } else if (id == R.id.btn9) {
            appendDigit("9");
        } else if (id == R.id.btnDot) {
            appendDot();
        } else if (id == R.id.btnClear) {
            clearAll();
        } else if (id == R.id.btnBack) {
            backspace();
        } else if (id == R.id.btnAdd) {
            applyOperator('+');
        } else if (id == R.id.btnSub) {
            applyOperator('-');
        } else if (id == R.id.btnMul) {
            applyOperator('*');
        } else if (id == R.id.btnDiv) {
            applyOperator('/');
        } else if (id == R.id.btnEq) {
            calculateResult();
        }
    }

    private void appendDigit(String digit) {
        if (resetInput) {
            input.setLength(0);
            resetInput = false;
        }
        input.append(digit);
        updateDisplay();
    }

    private void appendDot() {
        if (resetInput) {
            input.setLength(0);
            resetInput = false;
        }
        if (input.indexOf(".") == -1) {
            if (input.length() == 0) {
                input.append("0");
            }
            input.append(".");
            updateDisplay();
        }
    }

    private void clearAll() {
        input.setLength(0);
        operand = 0;
        pendingOperator = 0;
        resetInput = false;
        updateDisplay();
    }

    private void backspace() {
        if (resetInput) {
            return;
        }
        int len = input.length();
        if (len > 0) {
            input.deleteCharAt(len - 1);
            updateDisplay();
        }
    }

    private void applyOperator(char op) {
        if (input.length() == 0 && pendingOperator != 0) {
            pendingOperator = op;
            return;
        }
        double value = parseInput();
        if (pendingOperator == 0) {
            operand = value;
        } else {
            operand = compute(operand, value, pendingOperator);
        }
        pendingOperator = op;
        resetInput = true;
        display.setText(formatNumber(operand));
    }

    private void calculateResult() {
        if (pendingOperator == 0) {
            return;
        }
        double value = parseInput();
        double result = compute(operand, value, pendingOperator);
        display.setText(formatNumber(result));
        input.setLength(0);
        input.append(formatNumber(result));
        operand = 0;
        pendingOperator = 0;
        resetInput = true;
    }

    private double parseInput() {
        try {
            return Double.parseDouble(input.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private double compute(double a, double b, char op) {
        switch (op) {
            case '+':
                return a + b;
            case '-':
                return a - b;
            case '*':
                return a * b;
            case '/':
                if (b == 0) {
                    return 0;
                }
                return a / b;
            default:
                return b;
        }
    }

    private String formatNumber(double value) {
        if (value == (long) value) {
            return String.valueOf((long) value);
        } else {
            return String.valueOf(value);
        }
    }

    private void updateDisplay() {
        display.setText(input.length() == 0 ? "0" : input.toString());
    }
}
