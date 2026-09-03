package com.moderncalculator.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.text.DecimalFormat;

public class MainActivity extends Activity implements View.OnClickListener {

    private TextView tvExpression;
    private TextView tvResult;

    private String currentInput = "";
    private String operand1 = "";
    private String operator = "";
    private boolean isOperatorPressed = false;
    private boolean isResultDisplayed = false;

    private DecimalFormat decimalFormat = new DecimalFormat("#.########");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvExpression = (TextView) findViewById(R.id.tvExpression);
        tvResult = (TextView) findViewById(R.id.tvResult);

        int[] buttonIds = {
            R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4,
            R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9,
            R.id.btn_clear, R.id.btn_delete, R.id.btn_percent, R.id.btn_divide,
            R.id.btn_multiply, R.id.btn_subtract, R.id.btn_add,
            R.id.btn_plusminus, R.id.btn_decimal, R.id.btn_equal
        };

        for (int i = 0; i < buttonIds.length; i++) {
            findViewById(buttonIds[i]).setOnClickListener(this);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.btn_0 || id == R.id.btn_1 || id == R.id.btn_2 || id == R.id.btn_3 ||
            id == R.id.btn_4 || id == R.id.btn_5 || id == R.id.btn_6 || id == R.id.btn_7 ||
            id == R.id.btn_8 || id == R.id.btn_9) {
            
            Button btn = (Button) v;
            handleNumber(btn.getText().toString());

        } else if (id == R.id.btn_decimal) {
            handleDecimal();

        } else if (id == R.id.btn_clear) {
            handleClear();

        } else if (id == R.id.btn_delete) {
            handleDelete();

        } else if (id == R.id.btn_add || id == R.id.btn_subtract || id == R.id.btn_multiply || id == R.id.btn_divide) {
            Button btn = (Button) v;
            handleOperator(btn.getText().toString());

        } else if (id == R.id.btn_equal) {
            handleEqual();

        } else if (id == R.id.btn_plusminus) {
            handlePlusMinus();

        } else if (id == R.id.btn_percent) {
            handlePercent();
        }
    }

    private void handleNumber(String num) {
        if (isResultDisplayed) {
            currentInput = num;
            tvExpression.setText("");
            isResultDisplayed = false;
        } else if (isOperatorPressed) {
            currentInput = num;
            isOperatorPressed = false;
        } else {
            if (currentInput.equals("0")) {
                currentInput = num;
            } else {
                currentInput += num;
            }
        }
        updateDisplay();
    }

    private void handleDecimal() {
        if (isResultDisplayed) {
            currentInput = "0.";
            tvExpression.setText("");
            isResultDisplayed = false;
        } else if (isOperatorPressed) {
            currentInput = "0.";
            isOperatorPressed = false;
        } else if (!currentInput.contains(".")) {
            if (currentInput.isEmpty()) {
                currentInput = "0.";
            } else {
                currentInput += ".";
            }
        }
        updateDisplay();
    }

    private void handleClear() {
        currentInput = "";
        operand1 = "";
        operator = "";
        isOperatorPressed = false;
        isResultDisplayed = false;
        tvExpression.setText("");
        tvResult.setText("0");
    }

    private void handleDelete() {
        if (isResultDisplayed) {
            tvExpression.setText("");
            return;
        }
        if (currentInput.length() > 0) {
            currentInput = currentInput.substring(0, currentInput.length() - 1);
            if (currentInput.isEmpty() || currentInput.equals("-")) {
                currentInput = "0";
            }
            updateDisplay();
        }
    }

    private void handleOperator(String op) {
        if (!currentInput.isEmpty() && !currentInput.equals("-")) {
            if (!operand1.isEmpty() && !operator.isEmpty() && !isOperatorPressed) {
                calculate();
            }
            operand1 = currentInput;
            operator = op;
            isOperatorPressed = true;
            isResultDisplayed = false;
            tvExpression.setText(operand1 + " " + operator);
        } else if (isResultDisplayed) {
            operand1 = currentInput;
            operator = op;
            isOperatorPressed = true;
            isResultDisplayed = false;
            tvExpression.setText(operand1 + " " + operator);
        }
    }

    private void handleEqual() {
        if (!operand1.isEmpty() && !operator.isEmpty() && !currentInput.isEmpty() && !isOperatorPressed) {
            String tempOperand2 = currentInput;
            calculate();
            tvExpression.setText(operand1 + " " + operator + " " + tempOperand2 + " =");
            operand1 = "";
            operator = "";
            isResultDisplayed = true;
        }
    }

    private void calculate() {
        try {
            double num1 = Double.parseDouble(operand1);
            double num2 = Double.parseDouble(currentInput);
            double result = 0;

            if (operator.equals("+")) {
                result = num1 + num2;
            } else if (operator.equals("−")) {
                result = num1 - num2;
            } else if (operator.equals("×")) {
                result = num1 * num2;
            } else if (operator.equals("÷")) {
                if (num2 == 0) {
                    tvResult.setText("Error");
                    currentInput = "";
                    operand1 = "";
                    operator = "";
                    return;
                }
                result = num1 / num2;
            }

            currentInput = formatResult(result);
            updateDisplay();
        } catch (NumberFormatException e) {
            tvResult.setText("Error");
            currentInput = "";
            operand1 = "";
            operator = "";
        }
    }

    private void handlePlusMinus() {
        if (currentInput.isEmpty() || currentInput.equals("0")) {
            return;
        }
        if (currentInput.startsWith("-")) {
            currentInput = currentInput.substring(1);
        } else {
            currentInput = "-" + currentInput;
        }
        updateDisplay();
    }

    private void handlePercent() {
        if (!currentInput.isEmpty()) {
            try {
                double val = Double.parseDouble(currentInput);
                val = val / 100.0;
                currentInput = formatResult(val);
                updateDisplay();
            } catch (NumberFormatException e) {
                // Ignore parsing errors gracefully
            }
        }
    }

    private String formatResult(double d) {
        if (d == (long) d) {
            return String.format("%d", (long) d);
        } else {
            return decimalFormat.format(d);
        }
    }

    private void updateDisplay() {
        if (currentInput.isEmpty()) {
            tvResult.setText("0");
        } else {
            tvResult.setText(currentInput);
        }
    }
}