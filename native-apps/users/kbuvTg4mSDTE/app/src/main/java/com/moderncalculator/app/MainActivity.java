package com.moderncalculator.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class MainActivity extends Activity {

    private TextView displayTextView;
    private String currentInput = "";
    private String operator = "";
    private BigDecimal firstOperand = BigDecimal.ZERO;
    private boolean awaitingNewInput = true; // True if the next digit pressed should start a new number

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        displayTextView = (TextView) findViewById(R.id.displayTextView);
        displayTextView.setText("0");

        // Initialize all number buttons
        findViewById(R.id.button0).setOnClickListener(numberButtonListener);
        findViewById(R.id.button1).setOnClickListener(numberButtonListener);
        findViewById(R.id.button2).setOnClickListener(numberButtonListener);
        findViewById(R.id.button3).setOnClickListener(numberButtonListener);
        findViewById(R.id.button4).setOnClickListener(numberButtonListener);
        findViewById(R.id.button5).setOnClickListener(numberButtonListener);
        findViewById(R.id.button6).setOnClickListener(numberButtonListener);
        findViewById(R.id.button7).setOnClickListener(numberButtonListener);
        findViewById(R.id.button8).setOnClickListener(numberButtonListener);
        findViewById(R.id.button9).setOnClickListener(numberButtonListener);

        // Initialize operator buttons
        findViewById(R.id.buttonAdd).setOnClickListener(operatorButtonListener);
        findViewById(R.id.buttonSubtract).setOnClickListener(operatorButtonListener);
        findViewById(R.id.buttonMultiply).setOnClickListener(operatorButtonListener);
        findViewById(R.id.buttonDivide).setOnClickListener(operatorButtonListener);

        // Initialize other buttons
        findViewById(R.id.buttonEquals).setOnClickListener(equalsButtonListener);
        findViewById(R.id.buttonClear).setOnClickListener(clearButtonListener);
        findViewById(R.id.buttonDecimal).setOnClickListener(decimalButtonListener);
    }

    private View.OnClickListener numberButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Button b = (Button) v;
            if (awaitingNewInput) {
                currentInput = b.getText().toString();
                awaitingNewInput = false;
            } else {
                if (currentInput.equals("0") && !b.getText().toString().equals(".")) {
                    currentInput = b.getText().toString(); // Replace leading zero
                } else {
                    currentInput += b.getText().toString();
                }
            }
            displayTextView.setText(currentInput);
        }
    };

    private View.OnClickListener operatorButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Button b = (Button) v;
            if (currentInput.isEmpty() && firstOperand.equals(BigDecimal.ZERO)) {
                // If no number entered yet, allow operator if display is "0"
                firstOperand = BigDecimal.ZERO;
            } else if (!currentInput.isEmpty() && !awaitingNewInput) {
                // If there's a current input and it's not a fresh start after equals
                if (!operator.isEmpty()) { // If an operator was already selected, perform previous calculation first
                    firstOperand = performCalculation(firstOperand, new BigDecimal(currentInput), operator);
                    displayTextView.setText(formatResult(firstOperand));
                } else {
                    firstOperand = new BigDecimal(currentInput);
                }
            } else if (awaitingNewInput && !operator.isEmpty() && currentInput.isEmpty()) {
                // This case handles changing the operator after an operation without typing a new number
                // The firstOperand already holds the previous result. Just change operator.
            } else if (currentInput.isEmpty() && operator.isEmpty()) {
                // No input and no operator, e.g., opened app and pressed an operator
                firstOperand = BigDecimal.ZERO;
            }


            operator = b.getText().toString();
            awaitingNewInput = true;
            currentInput = ""; // Clear current input for the next operand
        }
    };

    private View.OnClickListener equalsButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!operator.isEmpty() && !currentInput.isEmpty()) {
                BigDecimal secondOperand = new BigDecimal(currentInput);
                firstOperand = performCalculation(firstOperand, secondOperand, operator);
                displayTextView.setText(formatResult(firstOperand));
                operator = ""; // Reset operator after calculation
                currentInput = formatResult(firstOperand); // Store result for potential chaining or subsequent operations
                awaitingNewInput = true; // Next number pressed should start a new input
            }
        }
    };

    private View.OnClickListener clearButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            currentInput = "";
            operator = "";
            firstOperand = BigDecimal.ZERO;
            awaitingNewInput = true;
            displayTextView.setText("0");
        }
    };

    private View.OnClickListener decimalButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (awaitingNewInput) {
                currentInput = "0.";
                awaitingNewInput = false;
            } else if (!currentInput.contains(".")) {
                currentInput += ".";
            }
            displayTextView.setText(currentInput);
        }
    };


    private BigDecimal performCalculation(BigDecimal operand1, BigDecimal operand2, String op) {
        try {
            if (op.equals("+")) {
                return operand1.add(operand2);
            } else if (op.equals("-")) {
                return operand1.subtract(operand2);
            } else if (op.equals("x")) { // Using 'x' for multiply in UI
                return operand1.multiply(operand2);
            } else if (op.equals("/")) {
                if (operand2.compareTo(BigDecimal.ZERO) == 0) {
                    // Handle division by zero
                    displayTextView.setText("Error");
                    clearCalculatorState(); // Reset calculator state
                    return BigDecimal.ZERO; // Return a default value or throw an exception
                }
                // Use a higher precision for division
                return operand1.divide(operand2, 10, RoundingMode.HALF_UP);
            }
        } catch (ArithmeticException e) {
            displayTextView.setText("Error");
            clearCalculatorState();
        }
        return BigDecimal.ZERO; // Should not reach here if operators are handled
    }

    private String formatResult(BigDecimal result) {
        // Remove trailing zeros if it's an integer
        if (result.stripTrailingZeros().scale() <= 0) {
            return result.toBigInteger().toString();
        }
        return result.toPlainString();
    }

    private void clearCalculatorState() {
        currentInput = "";
        operator = "";
        firstOperand = BigDecimal.ZERO;
        awaitingNewInput = true;
    }
}