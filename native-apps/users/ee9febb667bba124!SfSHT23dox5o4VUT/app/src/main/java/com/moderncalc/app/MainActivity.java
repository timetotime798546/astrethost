package com.moderncalc.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity implements View.OnClickListener {

    private TextView displayTextView;
    private StringBuilder currentNumber = new StringBuilder();
    private double operand1 = 0;
    private String operator = "";
    private boolean newNumber = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        displayTextView = (TextView) findViewById(R.id.display_text_view);

        // Initialize number buttons
        findViewById(R.id.btn_0).setOnClickListener(this);
        findViewById(R.id.btn_1).setOnClickListener(this);
        findViewById(R.id.btn_2).setOnClickListener(this);
        findViewById(R.id.btn_3).setOnClickListener(this);
        findViewById(R.id.btn_4).setOnClickListener(this);
        findViewById(R.id.btn_5).setOnClickListener(this);
        findViewById(R.id.btn_6).setOnClickListener(this);
        findViewById(R.id.btn_7).setOnClickListener(this);
        findViewById(R.id.btn_8).setOnClickListener(this);
        findViewById(R.id.btn_9).setOnClickListener(this);
        findViewById(R.id.btn_decimal).setOnClickListener(this);

        // Initialize operator buttons
        findViewById(R.id.btn_add).setOnClickListener(this);
        findViewById(R.id.btn_subtract).setOnClickListener(this);
        findViewById(R.id.btn_multiply).setOnClickListener(this);
        findViewById(R.id.btn_divide).setOnClickListener(this);

        // Initialize function buttons
        findViewById(R.id.btn_clear).setOnClickListener(this);
        findViewById(R.id.btn_equals).setOnClickListener(this);

        updateDisplay("0");
    }

    @Override
    public void onClick(View v) {
        Button button = (Button) v;
        String buttonText = button.getText().toString();

        if (isNumeric(buttonText) || buttonText.equals(".")) {
            handleNumberInput(buttonText);
        } else if (isOperator(buttonText)) {
            handleOperatorInput(buttonText);
        } else if (buttonText.equals("C")) {
            handleClear();
        } else if (buttonText.equals("=")) {
            handleEquals();
        }
    }

    private boolean isNumeric(String str) {
        return str.matches("-?\d+");
    }

    private boolean isOperator(String str) {
        return str.equals("+") || str.equals("-") || str.equals("*") || str.equals("/");
    }

    private void handleNumberInput(String number) {
        if (newNumber) {
            currentNumber.setLength(0);
            newNumber = false;
        }
        if (number.equals(".") && currentNumber.indexOf(".") != -1) {
            // Prevent multiple decimal points
            return;
        }
        currentNumber.append(number);
        updateDisplay(currentNumber.toString());
    }

    private void handleOperatorInput(String op) {
        if (currentNumber.length() > 0 && !newNumber) {
            if (operator.isEmpty()) {
                operand1 = Double.parseDouble(currentNumber.toString());
            } else {
                // If operator is already set, calculate previous operation first
                calculate();
            }
        }
        operator = op;
        newNumber = true;
    }

    private void handleClear() {
        currentNumber.setLength(0);
        currentNumber.append("0");
        operand1 = 0;
        operator = "";
        newNumber = true;
        updateDisplay(currentNumber.toString());
    }

    private void handleEquals() {
        if (operator.isEmpty() || currentNumber.length() == 0 || newNumber) {
            return; // No operation to perform or no second operand
        }
        calculate();
        operator = "";
        newNumber = true;
    }

    private void calculate() {
        double operand2 = Double.parseDouble(currentNumber.toString());
        double result = 0;
        boolean error = false;

        if (operator.equals("+")) {
            result = operand1 + operand2;
        } else if (operator.equals("-")) {
            result = operand1 - operand2;
        } else if (operator.equals("*")) {
            result = operand1 * operand2;
        } else if (operator.equals("/")) {
            if (operand2 == 0) {
                error = true;
                updateDisplay("Error");
            } else {
                result = operand1 / operand2;
            }
        }

        if (!error) {
            // Check if result is an integer to avoid .0 suffix
            if (result == (long) result) {
                updateDisplay(String.valueOf((long) result));
            } else {
                updateDisplay(String.valueOf(result));
            }
            operand1 = result;
            currentNumber.setLength(0);
            currentNumber.append(result);
        }
    }

    private void updateDisplay(String text) {
        displayTextView.setText(text);
    }
}