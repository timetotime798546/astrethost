package com.moderncalculator.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private TextView tvHistory;
    private TextView tvDisplay;

    private String currentInput = "0";
    private String historyText = "";
    private double result = 0;
    private String lastOperator = "";
    private boolean isOperandReset = false;
    private double memoryValue = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvHistory = (TextView) findViewById(R.id.tv_history);
        tvDisplay = (TextView) findViewById(R.id.tv_display);

        if (savedInstanceState != null) {
            currentInput = savedInstanceState.getString("currentInput", "0");
            historyText = savedInstanceState.getString("historyText", "");
            result = savedInstanceState.getDouble("result", 0);
            lastOperator = savedInstanceState.getString("lastOperator", "");
            isOperandReset = savedInstanceState.getBoolean("isOperandReset", false);
            memoryValue = savedInstanceState.getDouble("memoryValue", 0);
        }

        updateDisplay();

        // Digit buttons
        int[] digitIds = {
            R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4,
            R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9
        };

        for (int i = 0; i < digitIds.length; i++) {
            final int digit = i;
            findViewById(digitIds[i]).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onDigitPressed(String.valueOf(digit));
                }
            });
        }

        // Decimal dot button
        findViewById(R.id.btn_dot).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDotPressed();
            }
        });

        // Toggle sign button
        findViewById(R.id.btn_toggle).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onToggleSignPressed();
            }
        });

        // Operators
        findViewById(R.id.btn_add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onOperatorPressed("+");
            }
        });

        findViewById(R.id.btn_subtract).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onOperatorPressed("-");
            }
        });

        findViewById(R.id.btn_multiply).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onOperatorPressed("×");
            }
        });

        findViewById(R.id.btn_divide).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onOperatorPressed("÷");
            }
        });

        findViewById(R.id.btn_power).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onOperatorPressed("^");
            }
        });

        findViewById(R.id.btn_equals).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onEqualsPressed();
            }
        });

        // Action Keys
        findViewById(R.id.btn_clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClearPressed();
            }
        });

        findViewById(R.id.btn_del).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDeletePressed();
            }
        });

        findViewById(R.id.btn_percent).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onPercentPressed();
            }
        });

        findViewById(R.id.btn_sqrt).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSqrtPressed();
            }
        });

        // Memory Keys
        findViewById(R.id.btn_mc).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                memoryValue = 0;
                Toast.makeText(MainActivity.this, "Memory Cleared", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btn_mr).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentInput = formatNumber(memoryValue);
                isOperandReset = false;
                updateDisplay();
                Toast.makeText(MainActivity.this, "Memory Recalled: " + currentInput, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onDigitPressed(String digit) {
        if (isOperandReset) {
            currentInput = "";
            isOperandReset = false;
        }
        if (currentInput.equals("0")) {
            currentInput = digit;
        } else {
            currentInput += digit;
        }
        updateDisplay();
    }

    private void onDotPressed() {
        if (isOperandReset) {
            currentInput = "0.";
            isOperandReset = false;
        } else if (!currentInput.contains(".")) {
            if (currentInput.isEmpty()) {
                currentInput = "0.";
            } else {
                currentInput += ".";
            }
        }
        updateDisplay();
    }

    private void onToggleSignPressed() {
        if (!currentInput.equals("0") && !currentInput.isEmpty()) {
            if (currentInput.startsWith("-")) {
                currentInput = currentInput.substring(1);
            } else {
                currentInput = "-" + currentInput;
            }
            updateDisplay();
        }
    }

    private void onOperatorPressed(String operator) {
        if (!currentInput.isEmpty()) {
            try {
                double value = Double.parseDouble(currentInput);
                if (!lastOperator.isEmpty()) {
                    result = applyOperator(result, lastOperator, value);
                } else {
                    result = value;
                }
                lastOperator = operator;
                historyText = formatNumber(result) + " " + lastOperator + " ";
                currentInput = "";
                isOperandReset = true;
                updateDisplay();
            } catch (NumberFormatException e) {
                // Ignore
            }
        } else if (!lastOperator.isEmpty()) {
            lastOperator = operator;
            if (historyText.length() >= 2) {
                historyText = historyText.substring(0, historyText.length() - 2) + lastOperator + " ";
            }
            updateDisplay();
        }
    }

    private void onEqualsPressed() {
        if (!lastOperator.isEmpty() && !currentInput.isEmpty()) {
            try {
                double value = Double.parseDouble(currentInput);
                double originalResult = result;
                result = applyOperator(result, lastOperator, value);
                historyText = formatNumber(originalResult) + " " + lastOperator + " " + formatNumber(value) + " =";
                currentInput = formatNumber(result);
                memoryValue = result;
                lastOperator = "";
                isOperandReset = true;
                updateDisplay();
            } catch (NumberFormatException e) {
                // Ignore
            }
        }
    }

    private void onClearPressed() {
        currentInput = "0";
        historyText = "";
        result = 0;
        lastOperator = "";
        isOperandReset = false;
        updateDisplay();
    }

    private void onDeletePressed() {
        if (!isOperandReset && currentInput.length() > 0) {
            currentInput = currentInput.substring(0, currentInput.length() - 1);
            if (currentInput.isEmpty() || currentInput.equals("-")) {
                currentInput = "0";
            }
            updateDisplay();
        }
    }

    private void onPercentPressed() {
        if (!currentInput.isEmpty()) {
            try {
                double val = Double.parseDouble(currentInput) / 100.0;
                currentInput = formatNumber(val);
                updateDisplay();
            } catch (NumberFormatException e) {
                // Ignore
            }
        }
    }

    private void onSqrtPressed() {
        if (!currentInput.isEmpty()) {
            try {
                double val = Double.parseDouble(currentInput);
                if (val < 0) {
                    Toast.makeText(this, "Invalid Input for Square Root", Toast.LENGTH_SHORT).show();
                } else {
                    currentInput = formatNumber(Math.sqrt(val));
                    updateDisplay();
                }
            } catch (NumberFormatException e) {
                // Ignore
            }
        }
    }

    private void updateDisplay() {
        tvHistory.setText(historyText);
        if (currentInput.isEmpty()) {
            tvDisplay.setText(formatNumber(result));
        } else {
            tvDisplay.setText(currentInput);
        }
    }

    private double applyOperator(double a, String op, double b) {
        if (op.equals("+")) {
            return a + b;
        } else if (op.equals("-")) {
            return a - b;
        } else if (op.equals("×")) {
            return a * b;
        } else if (op.equals("÷")) {
            if (b == 0) {
                Toast.makeText(this, "Cannot divide by zero", Toast.LENGTH_SHORT).show();
                return Double.NaN;
            }
            return a / b;
        } else if (op.equals("^")) {
            return Math.pow(a, b);
        }
        return b;
    }

    private String formatNumber(double value) {
        if (Double.isNaN(value)) {
            return "Error";
        }
        if (Double.isInfinite(value)) {
            return "Infinity";
        }
        if (value == (long) value) {
            return String.format("%d", (long) value);
        } else {
            return String.valueOf(value);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("currentInput", currentInput);
        outState.putString("historyText", historyText);
        outState.putDouble("result", result);
        outState.putString("lastOperator", lastOperator);
        outState.putBoolean("isOperandReset", isOperandReset);
        outState.putDouble("memoryValue", memoryValue);
    }
}