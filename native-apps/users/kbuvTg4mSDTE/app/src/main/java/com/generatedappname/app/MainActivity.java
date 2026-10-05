package com.generatedappname.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity implements View.OnClickListener {

    private TextView tvExpression;
    private TextView tvResult;

    private String currentInput = "0";
    private String previousInput = "";
    private String operator = "";
    private boolean startNewInput = true;
    private boolean hasError = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvExpression = findViewById(R.id.tvExpression);
        tvResult = findViewById(R.id.tvResult);

        // Number buttons
        Button btn0 = findViewById(R.id.btn0);
        Button btn1 = findViewById(R.id.btn1);
        Button btn2 = findViewById(R.id.btn2);
        Button btn3 = findViewById(R.id.btn3);
        Button btn4 = findViewById(R.id.btn4);
        Button btn5 = findViewById(R.id.btn5);
        Button btn6 = findViewById(R.id.btn6);
        Button btn7 = findViewById(R.id.btn7);
        Button btn8 = findViewById(R.id.btn8);
        Button btn9 = findViewById(R.id.btn9);

        // Operator buttons
        Button btnAdd = findViewById(R.id.btnAdd);
        Button btnSubtract = findViewById(R.id.btnSubtract);
        Button btnMultiply = findViewById(R.id.btnMultiply);
        Button btnDivide = findViewById(R.id.btnDivide);
        Button btnPercent = findViewById(R.id.btnPercent);

        // Function buttons
        Button btnClear = findViewById(R.id.btnClear);
        Button btnBackspace = findViewById(R.id.btnBackspace);
        Button btnDecimal = findViewById(R.id.btnDecimal);
        Button btnEquals = findViewById(R.id.btnEquals);

        // Set click listeners
        btn0.setOnClickListener(this);
        btn1.setOnClickListener(this);
        btn2.setOnClickListener(this);
        btn3.setOnClickListener(this);
        btn4.setOnClickListener(this);
        btn5.setOnClickListener(this);
        btn6.setOnClickListener(this);
        btn7.setOnClickListener(this);
        btn8.setOnClickListener(this);
        btn9.setOnClickListener(this);

        btnAdd.setOnClickListener(this);
        btnSubtract.setOnClickListener(this);
        btnMultiply.setOnClickListener(this);
        btnDivide.setOnClickListener(this);
        btnPercent.setOnClickListener(this);

        btnClear.setOnClickListener(this);
        btnBackspace.setOnClickListener(this);
        btnDecimal.setOnClickListener(this);
        btnEquals.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (hasError) {
            clearAll();
        }

        int id = v.getId();

        if (id == R.id.btn0) {
            handleNumber("0");
        } else if (id == R.id.btn1) {
            handleNumber("1");
        } else if (id == R.id.btn2) {
            handleNumber("2");
        } else if (id == R.id.btn3) {
            handleNumber("3");
        } else if (id == R.id.btn4) {
            handleNumber("4");
        } else if (id == R.id.btn5) {
            handleNumber("5");
        } else if (id == R.id.btn6) {
            handleNumber("6");
        } else if (id == R.id.btn7) {
            handleNumber("7");
        } else if (id == R.id.btn8) {
            handleNumber("8");
        } else if (id == R.id.btn9) {
            handleNumber("9");
        } else if (id == R.id.btnDecimal) {
            handleDecimal();
        } else if (id == R.id.btnAdd) {
            handleOperator("+");
        } else if (id == R.id.btnSubtract) {
            handleOperator("-");
        } else if (id == R.id.btnMultiply) {
            handleOperator("*");
        } else if (id == R.id.btnDivide) {
            handleOperator("/");
        } else if (id == R.id.btnPercent) {
            handlePercent();
        } else if (id == R.id.btnEquals) {
            handleEquals();
        } else if (id == R.id.btnClear) {
            clearAll();
        } else if (id == R.id.btnBackspace) {
            handleBackspace();
        }
    }

    private void handleNumber(String number) {
        if (startNewInput) {
            currentInput = number;
            startNewInput = false;
        } else {
            if (currentInput.equals("0")) {
                currentInput = number;
            } else {
                if (currentInput.length() < 15) {
                    currentInput = currentInput + number;
                }
            }
        }
        updateDisplay();
    }

    private void handleDecimal() {
        if (startNewInput) {
            currentInput = "0.";
            startNewInput = false;
        } else {
            if (!currentInput.contains(".")) {
                currentInput = currentInput + ".";
            }
        }
        updateDisplay();
    }

    private void handleOperator(String op) {
        if (!operator.isEmpty() && !startNewInput) {
            calculate();
        }
        previousInput = currentInput;
        operator = op;
        startNewInput = true;
        updateDisplay();
    }

    private void handleEquals() {
        if (!operator.isEmpty()) {
            calculate();
            operator = "";
            startNewInput = true;
        }
        updateDisplay();
    }

    private void handlePercent() {
        try {
            double value = Double.parseDouble(currentInput);
            value = value / 100.0;
            currentInput = formatNumber(value);
            updateDisplay();
        } catch (NumberFormatException e) {
            hasError = true;
            tvResult.setText("Error");
            tvExpression.setText("");
        }
    }

    private void handleBackspace() {
        if (startNewInput) {
            return;
        }
        if (currentInput.length() > 1) {
            currentInput = currentInput.substring(0, currentInput.length() - 1);
        } else {
            currentInput = "0";
        }
        updateDisplay();
    }

    private void clearAll() {
        currentInput = "0";
        previousInput = "";
        operator = "";
        startNewInput = true;
        hasError = false;
        updateDisplay();
    }

    private void calculate() {
        try {
            double num1 = Double.parseDouble(previousInput);
            double num2 = Double.parseDouble(currentInput);
            double result = 0;

            switch (operator) {
                case "+":
                    result = num1 + num2;
                    break;
                case "-":
                    result = num1 - num2;
                    break;
                case "*":
                    result = num1 * num2;
                    break;
                case "/":
                    if (num2 == 0) {
                        hasError = true;
                        tvResult.setText("Error");
                        tvExpression.setText("");
                        return;
                    }
                    result = num1 / num2;
                    break;
                default:
                    return;
            }

            currentInput = formatNumber(result);
            previousInput = "";
        } catch (NumberFormatException e) {
            hasError = true;
            tvResult.setText("Error");
            tvExpression.setText("");
        }
    }

    private String formatNumber(double number) {
        if (number == (long) number) {
            return String.valueOf((long) number);
        } else {
            String formatted = String.format("%.10f", number);
            // Remove trailing zeros
            formatted = formatted.replaceAll("0+$", "");
            formatted = formatted.replaceAll("\\.$", "");
            return formatted;
        }
    }

    private void updateDisplay() {
        tvResult.setText(currentInput);
        
        if (!previousInput.isEmpty() && !operator.isEmpty()) {
            String opSymbol = getOperatorSymbol(operator);
            tvExpression.setText(previousInput + " " + opSymbol);
        } else if (!operator.isEmpty()) {
            String opSymbol = getOperatorSymbol(operator);
            tvExpression.setText(opSymbol);
        } else {
            tvExpression.setText("");
        }
    }

    private String getOperatorSymbol(String op) {
        switch (op) {
            case "+":
                return "+";
            case "-":
                return "−";
            case "*":
                return "×";
            case "/":
                return "÷";
            default:
                return "";
        }
    }
}