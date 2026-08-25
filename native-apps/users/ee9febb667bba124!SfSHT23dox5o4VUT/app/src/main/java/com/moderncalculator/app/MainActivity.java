package com.moderncalculator.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {

    private TextView displayTextView;
    private StringBuilder currentInput = new StringBuilder();
    private double operand1 = 0;
    private String operator = "";
    private boolean newOperation = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        displayTextView = (TextView) findViewById(R.id.displayTextView);
        displayTextView.setText("0");

        int[] buttonIds = {
                R.id.button_0, R.id.button_1, R.id.button_2, R.id.button_3,
                R.id.button_4, R.id.button_5, R.id.button_6, R.id.button_7,
                R.id.button_8, R.id.button_9, R.id.button_dot,
                R.id.button_add, R.id.button_subtract, R.id.button_multiply, R.id.button_divide,
                R.id.button_equals, R.id.button_clear
        };

        for (int id : buttonIds) {
            Button button = (Button) findViewById(id);
            if (button != null) {
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onButtonClick(v);
                    }
                });
            }
        }
    }

    private void onButtonClick(View v) {
        Button button = (Button) v;
        String buttonText = button.getText().toString();

        if (Character.isDigit(buttonText.charAt(0)) || buttonText.equals(".")) {
            if (newOperation) {
                currentInput.setLength(0);
                newOperation = false;
            }
            if (buttonText.equals(".") && currentInput.toString().contains(".")) {
                // Prevent multiple decimals
                return;
            }
            if (currentInput.length() == 1 && currentInput.charAt(0) == '0' && !buttonText.equals(".")) {
                 currentInput.setLength(0);
            }
            currentInput.append(buttonText);
            displayTextView.setText(currentInput.toString());
        } else if (buttonText.equals("C")) {
            currentInput.setLength(0);
            operand1 = 0;
            operator = "";
            newOperation = true;
            displayTextView.setText("0");
        } else if (buttonText.equals("=")) {
            if (currentInput.length() > 0 && !operator.isEmpty()) {
                double operand2 = Double.parseDouble(currentInput.toString());
                double result = performCalculation(operand1, operand2, operator);
                displayTextView.setText(formatResult(result));
                operand1 = result;
                currentInput.setLength(0);
                operator = "";
                newOperation = true;
            }
        } else { // Operator button (+, -, *, /)
            if (currentInput.length() > 0) {
                if (operator.isEmpty()) {
                    operand1 = Double.parseDouble(currentInput.toString());
                } else {
                    // Chain operations: calculate previous result
                    double operand2 = Double.parseDouble(currentInput.toString());
                    operand1 = performCalculation(operand1, operand2, operator);
                    displayTextView.setText(formatResult(operand1));
                }
                currentInput.setLength(0);
            } else if (!operator.isEmpty() && newOperation) {
                // Allow changing operator if no new number has been typed
                // E.g. 5 + * -> 5 *
            } else if (displayTextView.getText().length() > 0 && !displayTextView.getText().toString().equals("Error")) {
                // If display shows a result and no current input, use that as operand1
                operand1 = Double.parseDouble(displayTextView.getText().toString());
            }
            operator = buttonText;
            newOperation = true; // Next digit input will clear currentInput
        }
    }

    private double performCalculation(double op1, double op2, String op) {
        switch (op) {
            case "+":
                return op1 + op2;
            case "-":
                return op1 - op2;
            case "*":
                return op1 * op2;
            case "/":
                if (op2 == 0) {
                    displayTextView.setText("Error");
                    return 0; // Indicate error
                }
                return op1 / op2;
            default:
                return op2; // Should not happen
        }
    }

    private String formatResult(double result) {
        if (result == (long) result) {
            return String.format("%d", (long) result);
        } else {
            return String.format("%.8f", result).replaceAll("0*$", "").replaceAll("\\.$|-", "");
        }
    }
}