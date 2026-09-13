package com.simplecalculator.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class MainActivity extends Activity {

    private TextView tvExpression;
    private TextView tvDisplay;

    private StringBuilder currentInput;
    private double previousValue;
    private String currentOperator;
    private boolean isNewInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind layout views
        tvExpression = (TextView) findViewById(R.id.tvExpression);
        tvDisplay = (TextView) findViewById(R.id.tvDisplay);

        // Initialize state variables
        currentInput = new StringBuilder("0");
        previousValue = 0;
        currentOperator = "";
        isNewInput = true;

        setupClickListeners();
    }

    private void setupClickListeners() {
        // Shared Click Listener for digits 0-9
        View.OnClickListener digitListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button b = (Button) v;
                onDigitClick(b.getText().toString());
            }
        };

        findViewById(R.id.btn0).setOnClickListener(digitListener);
        findViewById(R.id.btn1).setOnClickListener(digitListener);
        findViewById(R.id.btn2).setOnClickListener(digitListener);
        findViewById(R.id.btn3).setOnClickListener(digitListener);
        findViewById(R.id.btn4).setOnClickListener(digitListener);
        findViewById(R.id.btn5).setOnClickListener(digitListener);
        findViewById(R.id.btn6).setOnClickListener(digitListener);
        findViewById(R.id.btn7).setOnClickListener(digitListener);
        findViewById(R.id.btn8).setOnClickListener(digitListener);
        findViewById(R.id.btn9).setOnClickListener(digitListener);

        // Shared Click Listener for base mathematical operators
        View.OnClickListener operatorListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button b = (Button) v;
                onOperatorClick(b.getText().toString());
            }
        };

        findViewById(R.id.btnAdd).setOnClickListener(operatorListener);
        findViewById(R.id.btnSubtract).setOnClickListener(operatorListener);
        findViewById(R.id.btnMultiply).setOnClickListener(operatorListener);
        findViewById(R.id.btnDivide).setOnClickListener(operatorListener);

        // Direct setups for functional control buttons
        findViewById(R.id.btnClear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearAll();
            }
        });

        findViewById(R.id.btnBackspace).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackspaceClick();
            }
        });

        findViewById(R.id.btnPercent).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onPercentClick();
            }
        });

        findViewById(R.id.btnToggleSign).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onToggleSignClick();
            }
        });

        findViewById(R.id.btnDecimal).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDecimalClick();
            }
        });

        findViewById(R.id.btnEquals).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onEqualsClick();
            }
        });
    }

    private void onDigitClick(String digit) {
        if (isNewInput) {
            currentInput.setLength(0);
            isNewInput = false;
        }

        // Avoid multiple leading zeros
        if (digit.equals("0") && currentInput.toString().equals("0")) {
            return;
        }

        // Remove single default leading zero if a non-zero digit is pressed
        if (currentInput.toString().equals("0")) {
            currentInput.setLength(0);
        }

        currentInput.append(digit);
        updateDisplay();
    }

    private void onDecimalClick() {
        if (isNewInput) {
            currentInput.setLength(0);
            currentInput.append("0");
            isNewInput = false;
        }

        if (currentInput.length() == 0) {
            currentInput.append("0");
        }

        // Add decimal dot if it doesn't already exist
        if (!currentInput.toString().contains(".")) {
            currentInput.append(".");
        }
        updateDisplay();
    }

    private void onToggleSignClick() {
        if (currentInput.length() == 0) return;
        try {
            double val = Double.parseDouble(currentInput.toString());
            if (val == 0.0) return;

            val = -val;
            currentInput.setLength(0);
            currentInput.append(formatResult(val));
            updateDisplay();
        } catch (NumberFormatException e) {
            // Safe fallback
        }
    }

    private void onPercentClick() {
        if (currentInput.length() == 0) return;
        try {
            double val = Double.parseDouble(currentInput.toString());
            val = val / 100.0;
            currentInput.setLength(0);
            currentInput.append(formatResult(val));
            updateDisplay();
            isNewInput = true;
        } catch (NumberFormatException e) {
            // Safe fallback
        }
    }

    private void onOperatorClick(String op) {
        if (currentInput.length() == 0) {
            currentInput.append("0");
        }

        double currentValue;
        try {
            currentValue = Double.parseDouble(currentInput.toString());
        } catch (NumberFormatException e) {
            currentValue = 0;
        }

        if (currentOperator.isEmpty()) {
            previousValue = currentValue;
            currentOperator = op;
            tvExpression.setText(formatResult(previousValue) + " " + currentOperator);
            isNewInput = true;
        } else {
            if (!isNewInput) {
                double result = calculate(previousValue, currentValue, currentOperator);
                previousValue = result;
                currentOperator = op;
                tvExpression.setText(formatResult(previousValue) + " " + currentOperator);
                currentInput.setLength(0);
                currentInput.append(formatResult(result));
                updateDisplay();
                isNewInput = true;
            } else {
                // User clicked consecutive operators; simply switch the selected operator
                currentOperator = op;
                tvExpression.setText(formatResult(previousValue) + " " + currentOperator);
            }
        }
    }

    private void onEqualsClick() {
        if (currentOperator.isEmpty()) {
            return;
        }

        double currentValue;
        try {
            currentValue = Double.parseDouble(currentInput.toString());
        } catch (NumberFormatException e) {
            currentValue = 0;
        }

        double result = calculate(previousValue, currentValue, currentOperator);

        // Show the evaluated expression history
        tvExpression.setText(formatResult(previousValue) + " " + currentOperator + " " + formatResult(currentValue) + " =");

        currentInput.setLength(0);
        currentInput.append(formatResult(result));
        updateDisplay();

        currentOperator = "";
        previousValue = 0;
        isNewInput = true;
    }

    private void onBackspaceClick() {
        if (isNewInput) {
            clearAll();
            return;
        }

        int length = currentInput.length();
        if (length > 0) {
            currentInput.deleteCharAt(length - 1);
            if (currentInput.length() == 0 || currentInput.toString().equals("-")) {
                currentInput.setLength(0);
                currentInput.append("0");
            }
        }
        updateDisplay();
    }

    private void clearAll() {
        currentInput.setLength(0);
        currentInput.append("0");
        previousValue = 0;
        currentOperator = "";
        tvExpression.setText("");
        isNewInput = true;
        updateDisplay();
    }

    private double calculate(double val1, double val2, String op) {
        if (op.equals("+")) {
            return val1 + val2;
        } else if (op.equals("-")) {
            return val1 - val2;
        } else if (op.equals("×")) {
            return val1 * val2;
        } else if (op.equals("÷")) {
            if (val2 == 0) {
                return Double.NaN; // Guard against arithmetic zero division errors
            }
            return val1 / val2;
        }
        return val2;
    }

    private String formatResult(double d) {
        if (Double.isNaN(d)) {
            return "Error";
        }
        if (Double.isInfinite(d)) {
            return "Error";
        }

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        DecimalFormat df = new DecimalFormat("#.##########", symbols);
        return df.format(d);
    }

    private void updateDisplay() {
        if (currentInput.length() == 0) {
            tvDisplay.setText("0");
        } else {
            tvDisplay.setText(currentInput.toString());
        }
    }
}