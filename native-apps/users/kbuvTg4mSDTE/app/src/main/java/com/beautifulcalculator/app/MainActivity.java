package com.beautifulcalculator.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {

    private TextView tvFormula;
    private TextView tvDisplay;

    private double operand1 = Double.NaN;
    private String activeOperator = null;
    private String currentInput = "";
    private boolean isOperatorJustPressed = false;
    private boolean hasResultJustDisplayed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvFormula = (TextView) findViewById(R.id.tv_formula);
        tvDisplay = (TextView) findViewById(R.id.tv_display);

        setupButtonListeners();
    }

    private void setupButtonListeners() {
        int[] numberIds = {
            R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4,
            R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9
        };

        View.OnClickListener numberClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button b = (Button) v;
                String digit = b.getText().toString();
                handleDigit(digit);
            }
        };

        for (int id : numberIds) {
            findViewById(id).setOnClickListener(numberClickListener);
        }

        findViewById(R.id.btn_add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleOperator("+");
            }
        });

        findViewById(R.id.btn_subtract).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleOperator("−");
            }
        });

        findViewById(R.id.btn_multiply).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleOperator("×");
            }
        });

        findViewById(R.id.btn_divide).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleOperator("÷");
            }
        });

        findViewById(R.id.btn_clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleClear();
            }
        });

        findViewById(R.id.btn_backspace).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleBackspace();
            }
        });

        findViewById(R.id.btn_percent).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handlePercent();
            }
        });

        findViewById(R.id.btn_toggle_sign).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleToggleSign();
            }
        });

        findViewById(R.id.btn_decimal).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleDecimal();
            }
        });

        findViewById(R.id.btn_equals).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleEquals();
            }
        });
    }

    private void handleDigit(String digit) {
        if (isOperatorJustPressed || hasResultJustDisplayed) {
            currentInput = "";
            isOperatorJustPressed = false;
            hasResultJustDisplayed = false;
        }

        if (currentInput.equals("0")) {
            if (digit.equals("0")) {
                return;
            } else {
                currentInput = digit;
            }
        } else {
            currentInput += digit;
        }

        tvDisplay.setText(currentInput);
    }

    private void handleDecimal() {
        if (isOperatorJustPressed || hasResultJustDisplayed) {
            currentInput = "0";
            isOperatorJustPressed = false;
            hasResultJustDisplayed = false;
        }

        if (currentInput.isEmpty()) {
            currentInput = "0.";
        } else if (!currentInput.contains(".")) {
            currentInput += ".";
        }

        tvDisplay.setText(currentInput);
    }

    private void handleOperator(String operator) {
        if (currentInput.isEmpty()) {
            if (!Double.isNaN(operand1)) {
                activeOperator = operator;
                tvFormula.setText(formatDouble(operand1) + " " + activeOperator);
            }
            return;
        }

        double value;
        try {
            value = Double.parseDouble(currentInput);
        } catch (NumberFormatException e) {
            return;
        }

        if (Double.isNaN(operand1)) {
            operand1 = value;
        } else {
            if (activeOperator != null) {
                operand1 = calculate(operand1, value, activeOperator);
                tvDisplay.setText(formatDouble(operand1));
            } else {
                operand1 = value;
            }
        }

        activeOperator = operator;
        tvFormula.setText(formatDouble(operand1) + " " + activeOperator);
        isOperatorJustPressed = true;
        hasResultJustDisplayed = false;
    }

    private void handleEquals() {
        if (Double.isNaN(operand1) || activeOperator == null || currentInput.isEmpty()) {
            return;
        }

        double value;
        try {
            value = Double.parseDouble(currentInput);
        } catch (NumberFormatException e) {
            return;
        }

        double result = calculate(operand1, value, activeOperator);

        tvFormula.setText(formatDouble(operand1) + " " + activeOperator + " " + formatDouble(value) + " =");
        tvDisplay.setText(formatDouble(result));

        operand1 = result;
        currentInput = formatDouble(result);
        activeOperator = null;
        isOperatorJustPressed = false;
        hasResultJustDisplayed = true;
    }

    private void handleClear() {
        operand1 = Double.NaN;
        activeOperator = null;
        currentInput = "";
        isOperatorJustPressed = false;
        hasResultJustDisplayed = false;
        tvFormula.setText("");
        tvDisplay.setText("0");
    }

    private void handleBackspace() {
        if (hasResultJustDisplayed) {
            tvFormula.setText("");
            return;
        }

        if (currentInput.length() > 0) {
            currentInput = currentInput.substring(0, currentInput.length() - 1);
            if (currentInput.isEmpty() || currentInput.equals("-")) {
                currentInput = "0";
            }
            tvDisplay.setText(currentInput);
        }
    }

    private void handleToggleSign() {
        if (currentInput.isEmpty() || currentInput.equals("0")) {
            return;
        }

        if (currentInput.startsWith("-")) {
            currentInput = currentInput.substring(1);
        } else {
            currentInput = "-" + currentInput;
        }
        tvDisplay.setText(currentInput);
    }

    private void handlePercent() {
        if (currentInput.isEmpty() || currentInput.equals("0")) {
            return;
        }

        try {
            double val = Double.parseDouble(currentInput);
            double result = val / 100.0;
            currentInput = formatDouble(result);
            tvDisplay.setText(currentInput);
        } catch (NumberFormatException e) {
            // No-op
        }
    }

    private double calculate(double op1, double op2, String operator) {
        if (operator.equals("+")) {
            return op1 + op2;
        } else if (operator.equals("−")) {
            return op1 - op2;
        } else if (operator.equals("×")) {
            return op1 * op2;
        } else if (operator.equals("÷")) {
            if (op2 == 0) {
                return Double.NaN;
            }
            return op1 / op2;
        }
        return op2;
    }

    private String formatDouble(double value) {
        if (Double.isInfinite(value) || Double.isNaN(value)) {
            return "Error";
        }
        if (value == (long) value) {
            return String.valueOf((long) value);
        }
        String str = String.format("%.8f", value);
        str = str.replace(',', '.');
        if (str.indexOf('.') > 0) {
            str = str.replaceAll("0+$", "");
            str = str.replaceAll("\\.$", "");
        }
        return str;
    }
}