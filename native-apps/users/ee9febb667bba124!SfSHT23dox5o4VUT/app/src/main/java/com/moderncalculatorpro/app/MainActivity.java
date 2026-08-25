package com.moderncalculatorpro.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {

    private TextView displayTextView;
    private String currentNumber = "";
    private double operand1 = 0;
    private String operator = "";
    private boolean isNewNumber = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        displayTextView = (TextView) findViewById(R.id.display_text_view);
        displayTextView.setText("0");

        // Number buttons
        findViewById(R.id.btn_0).setOnClickListener(numberClickListener);
        findViewById(R.id.btn_1).setOnClickListener(numberClickListener);
        findViewById(R.id.btn_2).setOnClickListener(numberClickListener);
        findViewById(R.id.btn_3).setOnClickListener(numberClickListener);
        findViewById(R.id.btn_4).setOnClickListener(numberClickListener);
        findViewById(R.id.btn_5).setOnClickListener(numberClickListener);
        findViewById(R.id.btn_6).setOnClickListener(numberClickListener);
        findViewById(R.id.btn_7).setOnClickListener(numberClickListener);
        findViewById(R.id.btn_8).setOnClickListener(numberClickListener);
        findViewById(R.id.btn_9).setOnClickListener(numberClickListener);
        findViewById(R.id.btn_dot).setOnClickListener(numberClickListener);

        // Operator buttons
        findViewById(R.id.btn_add).setOnClickListener(operatorClickListener);
        findViewById(R.id.btn_subtract).setOnClickListener(operatorClickListener);
        findViewById(R.id.btn_multiply).setOnClickListener(operatorClickListener);
        findViewById(R.id.btn_divide).setOnClickListener(operatorClickListener);

        // Control buttons
        findViewById(R.id.btn_clear).setOnClickListener(clearClickListener);
        findViewById(R.id.btn_del).setOnClickListener(delClickListener);
        findViewById(R.id.btn_equals).setOnClickListener(equalsClickListener);
    }

    private View.OnClickListener numberClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Button b = (Button) v;
            String digit = b.getText().toString();

            if (isNewNumber) {
                currentNumber = "";
                isNewNumber = false;
            }

            if (digit.equals(".")) {
                if (!currentNumber.contains(".")) {
                    if (currentNumber.isEmpty()) {
                        currentNumber = "0.";
                    } else {
                        currentNumber += digit;
                    }
                }
            } else {
                if (currentNumber.equals("0") && !digit.equals(".")) {
                    currentNumber = digit;
                } else {
                    currentNumber += digit;
                }
            }
            displayTextView.setText(currentNumber);
        }
    };

    private View.OnClickListener operatorClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Button b = (Button) v;
            String newOperator = b.getText().toString();

            if (!currentNumber.isEmpty() && !isNewNumber) {
                if (!operator.isEmpty()) {
                    calculateResult();
                    currentNumber = String.valueOf(operand1);
                } else {
                    operand1 = Double.parseDouble(currentNumber);
                }
            } else if (isNewNumber && !operator.isEmpty()) {
                // If operator is changed without new number input, update operator
                operator = newOperator;
                displayTextView.setText(formatResult(operand1) + " " + operator);
                return;
            } else if (currentNumber.isEmpty()) {
                // If no number is input yet, do nothing for operator
                return;
            }

            operator = newOperator;
            isNewNumber = true;
            displayTextView.setText(formatResult(operand1) + " " + operator);
        }
    };

    private View.OnClickListener equalsClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!operator.isEmpty() && !currentNumber.isEmpty() && !isNewNumber) {
                calculateResult();
                displayTextView.setText(formatResult(operand1));
                currentNumber = String.valueOf(operand1);
                operator = "";
                isNewNumber = true;
            } else if (currentNumber.isEmpty()) {
                displayTextView.setText("0");
            }
        }
    };

    private View.OnClickListener clearClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            currentNumber = "";
            operand1 = 0;
            operator = "";
            isNewNumber = true;
            displayTextView.setText("0");
        }
    };

    private View.OnClickListener delClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!isNewNumber && currentNumber.length() > 0) {
                currentNumber = currentNumber.substring(0, currentNumber.length() - 1);
                if (currentNumber.isEmpty() || currentNumber.equals("-")) {
                    currentNumber = "0";
                    isNewNumber = true;
                }
                displayTextView.setText(currentNumber);
            } else if (isNewNumber && displayTextView.getText().toString().length() > 0) {
                String displayStr = displayTextView.getText().toString();
                if (displayStr.contains(" ")) {
                    // If it's something like "123 + ", clear the operator
                    operator = "";
                    displayTextView.setText(formatResult(operand1));
                } else {
                    // If it's just a number like "123", treat as new number
                    currentNumber = "0";
                    isNewNumber = true;
                    displayTextView.setText("0");
                }
            }
        }
    };

    private void calculateResult() {
        double operand2 = 0;
        try {
            operand2 = Double.parseDouble(currentNumber);
        } catch (NumberFormatException e) {
            displayTextView.setText("Error");
            currentNumber = "";
            operand1 = 0;
            operator = "";
            isNewNumber = true;
            return;
        }

        switch (operator) {
            case "+":
                operand1 += operand2;
                break;
            case "-":
                operand1 -= operand2;
                break;
            case "*":
                operand1 *= operand2;
                break;
            case "/":
                if (operand2 == 0) {
                    displayTextView.setText("Error: Div by 0");
                    currentNumber = "";
                    operand1 = 0;
                    operator = "";
                    isNewNumber = true;
                    return;
                }
                operand1 /= operand2;
                break;
            default:
                break;
        }
    }

    private String formatResult(double result) {
        if (result == (long) result) {
            return String.valueOf((long) result);
        } else {
            return String.valueOf(result);
        }
    }
}