package com.moderncalculator.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {

    private TextView tvDisplay;
    private TextView tvHistory;

    private String currentNumber = "";
    private String operator = "";
    private double firstOperand = Double.NaN;
    private boolean isResultDisplayed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvDisplay = (TextView) findViewById(R.id.tvDisplay);
        tvHistory = (TextView) findViewById(R.id.tvHistory);

        setupClickListeners();
    }

    private void setupClickListeners() {
        int[] numberIds = {
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9,
            R.id.btnDot
        };

        View.OnClickListener numberClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button b = (Button) v;
                String text = b.getText().toString();
                if (isResultDisplayed) {
                    currentNumber = "";
                    isResultDisplayed = false;
                }
                if (text.equals(".") && currentNumber.contains(".")) {
                    return;
                }
                if (currentNumber.equals("0") && !text.equals(".")) {
                    currentNumber = text;
                } else {
                    currentNumber += text;
                }
                updateDisplay();
            }
        };

        for (int i = 0; i < numberIds.length; i++) {
            findViewById(numberIds[i]).setOnClickListener(numberClickListener);
        }

        int[] operatorIds = {
            R.id.btnAdd, R.id.btnSubtract, R.id.btnMultiply, R.id.btnDivide
        };

        View.OnClickListener operatorClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button b = (Button) v;
                String op = b.getText().toString();
                
                if (!Double.isNaN(firstOperand) && !currentNumber.isEmpty()) {
                    calculate();
                } else if (!currentNumber.isEmpty()) {
                    firstOperand = Double.parseDouble(currentNumber);
                }
                
                operator = op;
                if (!Double.isNaN(firstOperand)) {
                    tvHistory.setText(formatValue(firstOperand) + " " + operator);
                }
                currentNumber = "";
                isResultDisplayed = false;
            }
        };

        for (int i = 0; i < operatorIds.length; i++) {
            findViewById(operatorIds[i]).setOnClickListener(operatorClickListener);
        }

        findViewById(R.id.btnEqual).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calculate();
                operator = "";
                tvHistory.setText("");
            }
        });

        findViewById(R.id.btnClear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentNumber = "0";
                firstOperand = Double.NaN;
                operator = "";
                tvHistory.setText("");
                updateDisplay();
            }
        });

        findViewById(R.id.btnSign).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!currentNumber.isEmpty() && !currentNumber.equals("0")) {
                    double val = Double.parseDouble(currentNumber);
                    val = val * -1;
                    currentNumber = formatValue(val);
                    updateDisplay();
                }
            }
        });

        findViewById(R.id.btnPercent).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!currentNumber.isEmpty()) {
                    double val = Double.parseDouble(currentNumber);
                    val = val / 100.0;
                    currentNumber = formatValue(val);
                    updateDisplay();
                }
            }
        });
    }

    private void calculate() {
        if (Double.isNaN(firstOperand) || currentNumber.isEmpty()) {
            return;
        }

        double secondOperand = Double.parseDouble(currentNumber);
        double result = 0;

        if (operator.equals("+")) {
            result = firstOperand + secondOperand;
        } else if (operator.equals("-")) {
            result = firstOperand - secondOperand;
        } else if (operator.equals("×")) {
            result = firstOperand * secondOperand;
        } else if (operator.equals("÷")) {
            if (secondOperand == 0) {
                tvDisplay.setText("Error");
                currentNumber = "";
                firstOperand = Double.NaN;
                operator = "";
                return;
            }
            result = firstOperand / secondOperand;
        }

        tvHistory.setText(formatValue(firstOperand) + " " + operator + " " + formatValue(secondOperand) + " =");
        firstOperand = result;
        currentNumber = formatValue(result);
        isResultDisplayed = true;
        updateDisplay();
    }

    private void updateDisplay() {
        if (currentNumber.isEmpty()) {
            tvDisplay.setText("0");
        } else {
            tvDisplay.setText(currentNumber);
        }
    }

    private String formatValue(double val) {
        if (val == (long) val) {
            return String.format("%d", (long) val);
        } else {
            return String.valueOf(val);
        }
    }
}