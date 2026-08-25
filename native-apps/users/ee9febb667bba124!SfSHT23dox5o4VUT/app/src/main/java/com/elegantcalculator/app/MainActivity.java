package com.elegantcalculator.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends Activity {

    private EditText display;
    private String currentNumber = "";
    private String previousNumber = "";
    private String operator = "";
    private boolean newCalculation = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        display = (EditText) findViewById(R.id.displayEditText);
        display.setEnabled(false); // Make it non-editable

        initButtons();
    }

    private void initButtons() {
        int[] numberButtonIds = {R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4, R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9};
        int[] operatorButtonIds = {R.id.btnAdd, R.id.btnSubtract, R.id.btnMultiply, R.id.btnDivide};

        View.OnClickListener numberClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button b = (Button) v;
                if (newCalculation) {
                    currentNumber = b.getText().toString();
                    newCalculation = false;
                } else {
                    currentNumber = currentNumber + b.getText().toString();
                }
                display.setText(currentNumber);
            }
        };

        View.OnClickListener operatorClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button b = (Button) v;
                if (!currentNumber.isEmpty()) {
                    if (!previousNumber.isEmpty() && !operator.isEmpty()) {
                        calculate(); // Perform previous calculation before setting new operator
                    }
                    previousNumber = currentNumber;
                    operator = b.getText().toString();
                    currentNumber = "";
                    newCalculation = true;
                }
            }
        };

        for (int id : numberButtonIds) {
            findViewById(id).setOnClickListener(numberClickListener);
        }

        for (int id : operatorButtonIds) {
            findViewById(id).setOnClickListener(operatorClickListener);
        }

        findViewById(R.id.btnClear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentNumber = "";
                previousNumber = "";
                operator = "";
                newCalculation = true;
                display.setText("");
            }
        });

        findViewById(R.id.btnDecimal).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (newCalculation || currentNumber.isEmpty()) {
                    currentNumber = "0.";
                    newCalculation = false;
                } else if (!currentNumber.contains(".")) {
                    currentNumber = currentNumber + ".";
                }
                display.setText(currentNumber);
            }
        });

        findViewById(R.id.btnEquals).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calculate();
                operator = ""; // Clear operator after equals
                newCalculation = true;
            }
        });
    }

    private void calculate() {
        if (currentNumber.isEmpty() || previousNumber.isEmpty() || operator.isEmpty()) {
            return; // Not enough operands or no operator
        }

        try {
            double num1 = Double.parseDouble(previousNumber);
            double num2 = Double.parseDouble(currentNumber);
            double result = 0;

            if (operator.equals("+")) {
                result = num1 + num2;
            } else if (operator.equals("-")) {
                result = num1 - num2;
            } else if (operator.equals("*")) {
                result = num1 * num2;
            } else if (operator.equals("/")) {
                if (num2 == 0) {
                    display.setText("Error");
                    currentNumber = "";
                    previousNumber = "";
                    operator = "";
                    return;
                }
                result = num1 / num2;
            }

            // Format result to avoid unnecessary .0
            String formattedResult;
            if (result == (long) result) {
                formattedResult = String.valueOf((long) result);
            } else {
                formattedResult = String.valueOf(result);
            }

            display.setText(formattedResult);
            currentNumber = formattedResult;
            previousNumber = ""; // Clear previous number after calculation
            operator = ""; // Clear operator after calculation
        } catch (NumberFormatException e) {
            display.setText("Error");
            currentNumber = "";
            previousNumber = "";
            operator = "";
        }
    }
}