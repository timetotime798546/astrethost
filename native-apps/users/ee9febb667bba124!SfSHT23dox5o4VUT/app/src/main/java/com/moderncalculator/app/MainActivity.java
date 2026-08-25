package com.moderncalculator.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {

    private TextView display;
    private String currentNumber = "";
    private String previousNumber = "";
    private String operation = "";
    private boolean newOperation = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        display = (TextView) findViewById(R.id.display_text_view);

        // Set up number buttons
        setupNumberButton(R.id.button0, "0");
        setupNumberButton(R.id.button1, "1");
        setupNumberButton(R.id.button2, "2");
        setupNumberButton(R.id.button3, "3");
        setupNumberButton(R.id.button4, "4");
        setupNumberButton(R.id.button5, "5");
        setupNumberButton(R.id.button6, "6");
        setupNumberButton(R.id.button7, "7");
        setupNumberButton(R.id.button8, "8");
        setupNumberButton(R.id.button9, "9");
        setupNumberButton(R.id.button_dot, ".");

        // Set up operation buttons
        setupOperationButton(R.id.button_plus, "+");
        setupOperationButton(R.id.button_minus, "-");
        setupOperationButton(R.id.button_multiply, "*");
        setupOperationButton(R.id.button_divide, "/");

        // Set up clear and equals buttons
        Button clearButton = (Button) findViewById(R.id.button_clear);
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clear();
            }
        });

        Button equalsButton = (Button) findViewById(R.id.button_equals);
        equalsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calculate();
            }
        });

        display.setText("0");
    }

    private void setupNumberButton(int id, final String number) {
        Button button = (Button) findViewById(id);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onNumberClick(number);
            }
        });
    }

    private void setupOperationButton(int id, final String op) {
        Button button = (Button) findViewById(id);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onOperationClick(op);
            }
        });
    }

    private void onNumberClick(String num) {
        if (newOperation) {
            currentNumber = num;
            newOperation = false;
        } else {
            if (num.equals(".") && currentNumber.contains(".")) {
                // Do nothing if dot is already present
                return;
            }
            currentNumber = currentNumber + num;
        }
        display.setText(currentNumber);
    }

    private void onOperationClick(String op) {
        if (!currentNumber.isEmpty()) {
            if (!previousNumber.isEmpty() && !newOperation) {
                calculate(); // Calculate previous operation if exists
            }
            previousNumber = currentNumber;
            operation = op;
            newOperation = true;
            // display.setText(op); // Optional: show operation on display briefly
        }
    }

    private void calculate() {
        if (previousNumber.isEmpty() || currentNumber.isEmpty() || operation.isEmpty()) {
            return;
        }

        double num1 = Double.parseDouble(previousNumber);
        double num2 = Double.parseDouble(currentNumber);
        double result = 0;

        if (operation.equals("+")) {
            result = num1 + num2;
        } else if (operation.equals("-")) {
            result = num1 - num2;
        } else if (operation.equals("*")) {
            result = num1 * num2;
        } else if (operation.equals("/")) {
            if (num2 != 0) {
                result = num1 / num2;
            } else {
                display.setText("Error");
                clearAll();
                return;
            }
        }

        currentNumber = formatResult(result);
        display.setText(currentNumber);
        previousNumber = "";
        operation = "";
        newOperation = true;
    }

    private void clear() {
        clearAll();
        display.setText("0");
    }

    private void clearAll() {
        currentNumber = "";
        previousNumber = "";
        operation = "";
        newOperation = true;
    }

    private String formatResult(double result) {
        if (result == (long) result) {
            return String.valueOf((long) result);
        } else {
            return String.valueOf(result);
        }
    }
}
