package com.moderncalculator.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.text.DecimalFormat;

public class MainActivity extends Activity {

    private TextView tvDisplay;
    private TextView tvHistory;

    private String currentInput = "";
    private String currentOperator = "";
    private double firstOperand = Double.NaN;
    private double secondOperand = Double.NaN;

    private boolean isOperatorJustPressed = false;
    private DecimalFormat decimalFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        decimalFormat = new DecimalFormat("#.########");

        tvDisplay = (TextView) findViewById(R.id.tv_display);
        tvHistory = (TextView) findViewById(R.id.tv_history);

        setupButtonListeners();
    }

    private void setupButtonListeners() {
        int[] numIds = {
            R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4,
            R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9
        };

        for (int i = 0; i < numIds.length; i++) {
            final int value = i;
            findViewById(numIds[i]).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onDigitPressed(String.valueOf(value));
                }
            });
        }

        findViewById(R.id.btn_dot).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDecimalPressed();
            }
        });

        findViewById(R.id.btn_clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearAll();
            }
        });

        findViewById(R.id.btn_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackspacePressed();
            }
        });

        findViewById(R.id.btn_negate).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onNegatePressed();
            }
        });

        findViewById(R.id.btn_percent).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onPercentPressed();
            }
        });

        findViewById(R.id.btn_add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onOperatorPressed("+");
            }
        });

        findViewById(R.id.btn_sub).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onOperatorPressed("-");
            }
        });

        findViewById(R.id.btn_mul).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onOperatorPressed("×");
            }
        });

        findViewById(R.id.btn_div).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onOperatorPressed("÷");
            }
        });

        findViewById(R.id.btn_eq).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onEqualPressed();
            }
        });
    }

    private void onDigitPressed(String digit) {
        if (isOperatorJustPressed) {
            currentInput = "";
            isOperatorJustPressed = false;
        }

        if (currentInput.equals("0")) {
            currentInput = digit;
        } else {
            currentInput += digit;
        }

        updateDisplay(currentInput);
    }

    private void onDecimalPressed() {
        if (isOperatorJustPressed) {
            currentInput = "0";
            isOperatorJustPressed = false;
        }

        if (!currentInput.contains(".")) {
            if (currentInput.isEmpty()) {
                currentInput = "0.";
            } else {
                currentInput += ".";
            }
        }
        updateDisplay(currentInput);
    }

    private void onBackspacePressed() {
        if (currentInput.length() > 0 && !isOperatorJustPressed) {
            currentInput = currentInput.substring(0, currentInput.length() - 1);
            if (currentInput.isEmpty() || currentInput.equals("-")) {
                currentInput = "0";
            }
            updateDisplay(currentInput);
        }
    }

    private void onNegatePressed() {
        if (!currentInput.isEmpty() && !currentInput.equals("0")) {
            double value = Double.parseDouble(currentInput);
            value = -value;
            currentInput = decimalFormat.format(value);
            updateDisplay(currentInput);
        }
    }

    private void onPercentPressed() {
        if (!currentInput.isEmpty()) {
            double value = Double.parseDouble(currentInput);
            value = value / 100.0;
            currentInput = decimalFormat.format(value);
            updateDisplay(currentInput);
        }
    }

    private void onOperatorPressed(String operator) {
        if (!currentInput.isEmpty()) {
            double value = Double.parseDouble(currentInput);

            if (!Double.isNaN(firstOperand) && !isOperatorJustPressed) {
                calculatePending(value);
            } else {
                firstOperand = value;
            }
        }

        currentOperator = operator;
        isOperatorJustPressed = true;
        
        String historyText = decimalFormat.format(firstOperand) + " " + currentOperator;
        tvHistory.setText(historyText);
    }

    private void calculatePending(double nextValue) {
        if (currentOperator.equals("+")) {
            firstOperand += nextValue;
        } else if (currentOperator.equals("-")) {
            firstOperand -= nextValue;
        } else if (currentOperator.equals("×")) {
            firstOperand *= nextValue;
        } else if (currentOperator.equals("÷")) {
            if (nextValue != 0) {
                firstOperand /= nextValue;
            } else {
                triggerError("Divide by Zero");
                return;
            }
        }
        currentInput = decimalFormat.format(firstOperand);
        updateDisplay(currentInput);
    }

    private void onEqualPressed() {
        if (!Double.isNaN(firstOperand) && !currentOperator.isEmpty() && !currentInput.isEmpty()) {
            double secondValue = Double.parseDouble(currentInput);
            
            String fullHistory = decimalFormat.format(firstOperand) + " " + currentOperator + " " + decimalFormat.format(secondValue) + " =";
            tvHistory.setText(fullHistory);

            calculatePending(secondValue);
            firstOperand = Double.NaN;
            currentOperator = "";
            isOperatorJustPressed = true;
        }
    }

    private void clearAll() {
        currentInput = "0";
        currentOperator = "";
        firstOperand = Double.NaN;
        secondOperand = Double.NaN;
        isOperatorJustPressed = false;
        tvHistory.setText("");
        updateDisplay("0");
    }

    private void triggerError(String errorMsg) {
        tvDisplay.setText(errorMsg);
        tvHistory.setText("");
        currentInput = "";
        firstOperand = Double.NaN;
        currentOperator = "";
        isOperatorJustPressed = true;
    }

    private void updateDisplay(String text) {
        tvDisplay.setText(text);
    }
}