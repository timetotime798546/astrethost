package com.moderncalculator.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {

    private TextView displayTextView;
    private String currentInput = "";
    private String operator = "";
    private double firstOperand = 0;
    private boolean operatorPressed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        displayTextView = (TextView) findViewById(R.id.displayTextView);
        displayTextView.setText("0"); // Initialize display

        // Number Buttons
        Button button0 = (Button) findViewById(R.id.button0);
        Button button1 = (Button) findViewById(R.id.button1);
        Button button2 = (Button) findViewById(R.id.button2);
        Button button3 = (Button) findViewById(R.id.button3);
        Button button4 = (Button) findViewById(R.id.button4);
        Button button5 = (Button) findViewById(R.id.button5);
        Button button6 = (Button) findViewById(R.id.button6);
        Button button7 = (Button) findViewById(R.id.button7);
        Button button8 = (Button) findViewById(R.id.button8);
        Button button9 = (Button) findViewById(R.id.button9);
        Button buttonDecimal = (Button) findViewById(R.id.buttonDecimal);

        // Operator Buttons
        Button buttonAdd = (Button) findViewById(R.id.buttonAdd);
        Button buttonSubtract = (Button) findViewById(R.id.buttonSubtract);
        Button buttonMultiply = (Button) findViewById(R.id.buttonMultiply);
        Button buttonDivide = (Button) findViewById(R.id.buttonDivide);
        Button buttonEquals = (Button) findViewById(R.id.buttonEquals);
        Button buttonClear = (Button) findViewById(R.id.buttonClear);

        View.OnClickListener numberClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (operatorPressed) {
                    currentInput = "";
                    operatorPressed = false;
                }
                String digit = ((Button) v).getText().toString();
                if (currentInput.equals("0") && !digit.equals(".")) {
                    currentInput = digit; // Replace initial 0
                } else {
                    currentInput += digit;
                }
                displayTextView.setText(currentInput);
            }
        };

        button0.setOnClickListener(numberClickListener);
        button1.setOnClickListener(numberClickListener);
        button2.setOnClickListener(numberClickListener);
        button3.setOnClickListener(numberClickListener);
        button4.setOnClickListener(numberClickListener);
        button5.setOnClickListener(numberClickListener);
        button6.setOnClickListener(numberClickListener);
        button7.setOnClickListener(numberClickListener);
        button8.setOnClickListener(numberClickListener);
        button9.setOnClickListener(numberClickListener);
        
        buttonDecimal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (operatorPressed) {
                    currentInput = "0.";
                    operatorPressed = false;
                } else if (!currentInput.contains(".")) {
                    if (currentInput.isEmpty()) {
                        currentInput = "0.";
                    } else {
                        currentInput += ".";
                    }
                }
                displayTextView.setText(currentInput);
            }
        });


        View.OnClickListener operatorClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!currentInput.isEmpty() && !currentInput.equals("Error")) {
                    firstOperand = Double.parseDouble(currentInput);
                } else {
                    // If no number is entered or previous was error, use 0 or keep firstOperand
                    if (currentInput.equals("Error")) {
                        firstOperand = 0;
                    }
                }
                operator = ((Button) v).getText().toString();
                operatorPressed = true;
            }
        };

        buttonAdd.setOnClickListener(operatorClickListener);
        buttonSubtract.setOnClickListener(operatorClickListener);
        buttonMultiply.setOnClickListener(operatorClickListener);
        buttonDivide.setOnClickListener(operatorClickListener);

        buttonEquals.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (operator.isEmpty() || currentInput.isEmpty() || currentInput.equals("Error")) {
                    return; // No pending operation or no second operand
                }
                double secondOperand = Double.parseDouble(currentInput);
                double result = 0;

                if (operator.equals("+")) {
                    result = firstOperand + secondOperand;
                } else if (operator.equals("-")) {
                    result = firstOperand - secondOperand;
                } else if (operator.equals("*")) {
                    result = firstOperand * secondOperand;
                } else if (operator.equals("/")) {
                    if (secondOperand != 0) {
                        result = firstOperand / secondOperand;
                    } else {
                        displayTextView.setText("Error");
                        currentInput = "Error";
                        operator = "";
                        firstOperand = 0;
                        operatorPressed = false;
                        return;
                    }
                }
                // Check if result is a whole number to avoid unnecessary .0
                if (result == (long) result) {
                    currentInput = String.valueOf((long) result);
                } else {
                    currentInput = String.valueOf(result);
                }
                
                displayTextView.setText(currentInput);
                firstOperand = result; // For chained operations
                operator = ""; // Clear operator after calculation
                operatorPressed = true; // Next input will start new number
            }
        });

        buttonClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentInput = "";
                operator = "";
                firstOperand = 0;
                operatorPressed = false;
                displayTextView.setText("0");
            }
        });
    }
}