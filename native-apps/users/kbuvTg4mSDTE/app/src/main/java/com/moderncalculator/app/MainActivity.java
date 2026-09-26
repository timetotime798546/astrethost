package com.moderncalculator.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {

    private TextView display;
    private String currentInput = "";
    private String currentOperator = "";
    private double result = 0;
    private boolean operatorPressed = false;
    private boolean newCalculation = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        display = (TextView) findViewById(R.id.display_text_view);

        // Find all number buttons and set click listener
        int[] numberButtonIds = {
            R.id.button0, R.id.button1, R.id.button2, R.id.button3,
            R.id.button4, R.id.button5, R.id.button6, R.id.button7,
            R.id.button8, R.id.button9
        };

        for (int id : numberButtonIds) {
            findViewById(id).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onNumberClick(((Button) v).getText().toString());
                }
            });
        }

        // Find all operator buttons and set click listener
        int[] operatorButtonIds = {
            R.id.button_plus, R.id.button_minus, R.id.button_multiply, R.id.button_divide
        };

        for (int id : operatorButtonIds) {
            findViewById(id).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onOperatorClick(((Button) v).getText().toString());
                }
            });
        }

        // Set click listener for other buttons
        findViewById(R.id.button_clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClearClick();
            }
        });

        findViewById(R.id.button_equals).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onEqualsClick();
            }
        });

        findViewById(R.id.button_dot).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDecimalClick();
            }
        });

        updateDisplay();
    }

    private void onNumberClick(String number) {
        if (newCalculation) {
            currentInput = "";
            newCalculation = false;
        }
        if (operatorPressed) {
            currentInput = "";
            operatorPressed = false;
        }
        if (currentInput.equals("0") && number.equals("0")) {
            return; // Prevent multiple leading zeros
        }
        if (currentInput.equals("0") && !number.equals("0")) {
            currentInput = number; // Replace single leading zero
        } else {
            currentInput += number;
        }
        updateDisplay();
    }

    private void onOperatorClick(String operator) {
        if (currentInput.isEmpty() && !newCalculation) {
            // If only operator is changed, update the operator
            currentOperator = operator;
            updateDisplay();
            return;
        }

        if (!operatorPressed && !newCalculation) {
            // If an operator was not just pressed, calculate previous result
            calculate();
        }

        currentOperator = operator;
        operatorPressed = true;
        newCalculation = false;
        updateDisplay();
    }

    private void onClearClick() {
        currentInput = "";
        currentOperator = "";
        result = 0;
        operatorPressed = false;
        newCalculation = true;
        updateDisplay();
    }

    private void onEqualsClick() {
        if (!currentInput.isEmpty() && !newCalculation) {
            calculate();
            currentOperator = "";
            newCalculation = true; // Ready for a new calculation
        }
        updateDisplay();
    }

    private void onDecimalClick() {
        if (newCalculation) {
            currentInput = "0.";
            newCalculation = false;
        } else if (operatorPressed) {
            currentInput = "0.";
            operatorPressed = false;
        } else if (!currentInput.contains(".")) {
            currentInput += ".";
        }
        updateDisplay();
    }

    private void calculate() {
        if (currentInput.isEmpty()) {
            return; // Nothing to calculate if no current input
        }

        try {
            double value = Double.parseDouble(currentInput);

            if (currentOperator.equals("+")) {
                result += value;
            } else if (currentOperator.equals("-")) {
                result -= value;
            } else if (currentOperator.equals("*")) {
                result *= value;
            } else if (currentOperator.equals("/")) {
                if (value == 0) {
                    display.setText("Error: Div by zero");
                    result = 0;
                    currentInput = "";
                    currentOperator = "";
                    newCalculation = true;
                    return;
                }
                result /= value;
            } else {
                // First number input or after clear
                result = value;
            }
            // Update currentInput to result for continuous calculations
            currentInput = String.valueOf(result);

            // Remove trailing .0 if present for integer results
            if (currentInput.endsWith(".0")) {
                currentInput = currentInput.substring(0, currentInput.length() - 2);
            }
        } catch (NumberFormatException e) {
            display.setText("Error");
            result = 0;
            currentInput = "";
            currentOperator = "";
            newCalculation = true;
        }
    }

    private void updateDisplay() {
        if (currentInput.isEmpty()) {
            display.setText(String.valueOf(result));
        } else {
            display.setText(currentInput);
        }
    }
}