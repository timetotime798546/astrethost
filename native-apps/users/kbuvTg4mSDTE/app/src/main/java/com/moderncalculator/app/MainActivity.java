package com.moderncalculator.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private TextView display;
    private String currentInput = "";
    private String currentOperator = "";
    private double result = 0;
    private boolean operatorPressed = false;
    private boolean newCalculation = true;
    private List<String> historyList = new ArrayList<String>();

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

        findViewById(R.id.button_history).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onHistoryClick();
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
        historyList.clear(); // Clear history when C is pressed
        updateDisplay();
    }

    private void onEqualsClick() {
        if (!currentInput.isEmpty() || (newCalculation && !display.getText().toString().equals("0") && !display.getText().toString().isEmpty())) {
            String historyEntry = "";
            String firstOperandStr = formatDouble(result); // result before current calculation
            String operatorStr = currentOperator;
            String secondOperandStr = currentInput; // current input
            
            // Perform the calculation, this updates 'result' and 'currentInput'
            calculate(); 
            
            // Check for error condition after calculation
            if (display.getText().toString().startsWith("Error")) {
                // If calculate resulted in an error, do not add to history.
                currentOperator = "";
                newCalculation = true;
                return;
            }

            if (!operatorStr.isEmpty()) {
                historyEntry = firstOperandStr + " " + operatorStr + " " + secondOperandStr + " = " + formatDouble(result);
            } else if (!secondOperandStr.isEmpty()) {
                // Case: User types a number (e.g., "5") and presses '=' immediately.
                // Or user presses '=' after a number is already displayed without an operator.
                historyEntry = secondOperandStr + " = " + formatDouble(result);
            } else {
                // This might catch cases like repeated equals on a single number result
                historyEntry = formatDouble(result) + " = " + formatDouble(result);
            }


            if (!historyEntry.isEmpty()) {
                historyList.add(0, historyEntry); // Add to the beginning of the list
                if (historyList.size() > 20) { // Limit history size
                    historyList.remove(historyList.size() - 1);
                }
            }
            
            currentOperator = "";
            newCalculation = true;
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

    private void onHistoryClick() {
        Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
        intent.putStringArrayListExtra("history", new ArrayList<String>(historyList));
        startActivity(intent);
    }

    private void calculate() {
        if (currentInput.isEmpty()) {
            // If currentInput is empty, assume second operand is the same as the first (result)
            // This handles cases like "5 + =" => 5 + 5 = 10
            // Or "5 =" (if operator is empty)
            if (currentOperator.isEmpty()) {
                // No operation, just set result to current number
                // This scenario is mainly handled by onEqualsClick directly
                return; 
            }
            // If operator is present, but currentInput is empty, repeat last operand
            currentInput = formatDouble(result);
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
                // First number input or after clear, or just a number followed by equals
                result = value;
            }
            // Update currentInput to result for continuous calculations
            currentInput = formatDouble(result);

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
            display.setText(formatDouble(result));
        } else {
            display.setText(currentInput);
        }
    }

    // Helper method to format double values for display/history
    private String formatDouble(double value) {
        // Android's Double.toString() is usually good, but we want to remove trailing .0
        // for integer representations.
        String s = String.valueOf(value);
        if (s.endsWith(".0")) {
            return s.substring(0, s.length() - 2);
        }
        return s;
    }
}