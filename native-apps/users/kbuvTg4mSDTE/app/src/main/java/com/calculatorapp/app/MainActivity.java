package com.calculatorapp.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private EditText display;
    private TextView formulaTextView;
    private StringBuilder currentNumber = new StringBuilder();
    private double firstOperand = 0;
    private String operator = "";
    private boolean isNewCalculation = true; // True after an operator or equals is pressed

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        display = (EditText) findViewById(R.id.displayEditText);
        formulaTextView = (TextView) findViewById(R.id.formulaTextView);

        // Assign OnClickListener to all number buttons
        int[] numberButtonIds = {
            R.id.button0, R.id.button1, R.id.button2, R.id.button3,
            R.id.button4, R.id.button5, R.id.button6, R.id.button7,
            R.id.button8, R.id.button9, R.id.buttonDecimal
        };

        for (int i = 0; i < numberButtonIds.length; i++) {
            final int id = numberButtonIds[i];
            findViewById(id).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onNumberClick(((Button) v).getText().toString());
                }
            });
        }

        // Assign OnClickListener to all operator buttons
        findViewById(R.id.buttonAdd).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onOperatorClick("+");
            }
        });
        findViewById(R.id.buttonSubtract).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onOperatorClick("-");
            }
        });
        findViewById(R.id.buttonMultiply).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onOperatorClick("*");
            }
        });
        findViewById(R.id.buttonDivide).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onOperatorClick("/");
            }
        });

        findViewById(R.id.buttonEquals).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onEqualsClick();
            }
        });

        findViewById(R.id.buttonClear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClearClick();
            }
        });
    }

    private void onNumberClick(String number) {
        if (isNewCalculation) {
            currentNumber.setLength(0); // Clear previous number
            isNewCalculation = false;
        }
        if (number.equals(".")) {
            if (currentNumber.length() == 0) {
                currentNumber.append("0.");
            } else if (currentNumber.indexOf(".") == -1) { // Only one decimal allowed
                currentNumber.append(number);
            }
        } else {
            currentNumber.append(number);
        }
        display.setText(currentNumber.toString());
    }

    private void onOperatorClick(String newOperator) {
        if (currentNumber.length() == 0 && operator.isEmpty()) {
            // No current input but we have a result stored, can chain operation
            if (isNewCalculation) {
                operator = newOperator;
                formulaTextView.setText(formatResult(firstOperand) + " " + mapOperatorSymbol(operator));
            }
            return;
        }

        if (currentNumber.length() > 0 && !operator.isEmpty()) {
            // Already have first operand and operator, perform previous calculation
            double secondOperand = parseCurrentNumber();
            if (Double.isNaN(secondOperand)) {
                return; // Error in parsing
            }
            double result = performCalculation(firstOperand, secondOperand, operator);
            display.setText(formatResult(result));
            firstOperand = result; // Result becomes the new first operand
        } else if (currentNumber.length() > 0) {
            // First time an operator is pressed
            firstOperand = parseCurrentNumber();
            if (Double.isNaN(firstOperand)) {
                return; // Error in parsing
            }
        }

        operator = newOperator;
        formulaTextView.setText(formatResult(firstOperand) + " " + mapOperatorSymbol(operator));
        isNewCalculation = true;
    }

    private void onEqualsClick() {
        if (operator.isEmpty() || currentNumber.length() == 0) {
            return;
        }

        double secondOperand = parseCurrentNumber();
        if (Double.isNaN(secondOperand)) {
            return; // Error in parsing
        }

        double result = performCalculation(firstOperand, secondOperand, operator);
        if (!Double.isNaN(result)) {
            formulaTextView.setText(formatResult(firstOperand) + " " + mapOperatorSymbol(operator) + " " + formatResult(secondOperand) + " =");
            display.setText(formatResult(result));
            firstOperand = result; // Ready for chained calculations
        }

        operator = ""; // Clear operator after equals
        isNewCalculation = true; // Ready for new number input
    }

    private void onClearClick() {
        currentNumber.setLength(0);
        firstOperand = 0;
        operator = "";
        isNewCalculation = true;
        display.setText("0");
        formulaTextView.setText("");
    }

    private double parseCurrentNumber() {
        try {
            return Double.parseDouble(currentNumber.toString());
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid number format", Toast.LENGTH_SHORT).show();
            return Double.NaN; // Indicate error
        }
    }

    private double performCalculation(double op1, double op2, String op) {
        double result = 0;
        if (op.equals("+")) {
            result = op1 + op2;
        } else if (op.equals("-")) {
            result = op1 - op2;
        } else if (op.equals("*")) {
            result = op1 * op2;
        } else if (op.equals("/")) {
            if (op2 == 0) {
                Toast.makeText(this, "Cannot divide by zero", Toast.LENGTH_LONG).show();
                return Double.NaN; // Indicate error
            }
            result = op1 / op2;
        }
        return result;
    }

    private String formatResult(double val) {
        if (Double.isNaN(val)) {
            return "Error";
        }
        if (val == (long) val) {
            return String.format("%d", (long) val);
        } else {
            return String.valueOf(val);
        }
    }

    private String mapOperatorSymbol(String op) {
        if (op.equals("+")) return "+";
        if (op.equals("-")) return "−";
        if (op.equals("*")) return "×";
        if (op.equals("/")) return "÷";
        return op;
    }
}