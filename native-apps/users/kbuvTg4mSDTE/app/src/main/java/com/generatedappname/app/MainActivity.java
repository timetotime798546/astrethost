package com.generatedappname.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity implements View.OnClickListener {

    private TextView tvDisplay;
    private TextView tvHistory;
    
    private String currentInput = "0";
    private String previousInput = "";
    private String operator = "";
    private boolean startNewInput = true;
    private boolean hasError = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvDisplay = findViewById(R.id.tvDisplay);
        tvHistory = findViewById(R.id.tvHistory);

        // Number Buttons
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
        Button btnDecimal = findViewById(R.id.btnDecimal);

        // Operator Buttons
        Button btnAdd = findViewById(R.id.btnAdd);
        Button btnSubtract = findViewById(R.id.btnSubtract);
        Button btnMultiply = findViewById(R.id.btnMultiply);
        Button btnDivide = findViewById(R.id.btnDivide);
        Button btnPercent = findViewById(R.id.btnPercent);

        // Function Buttons
        Button btnClear = findViewById(R.id.btnClear);
        Button btnDelete = findViewById(R.id.btnDelete);
        Button btnEquals = findViewById(R.id.btnEquals);

        // Set Click Listeners
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
        btnDecimal.setOnClickListener(this);
        btnAdd.setOnClickListener(this);
        btnSubtract.setOnClickListener(this);
        btnMultiply.setOnClickListener(this);
        btnDivide.setOnClickListener(this);
        btnPercent.setOnClickListener(this);
        btnClear.setOnClickListener(this);
        btnDelete.setOnClickListener(this);
        btnEquals.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (hasError) {
            clearAll();
        }

        int id = v.getId();

        if (id == R.id.btn0) {
            inputNumber("0");
        } else if (id == R.id.btn1) {
            inputNumber("1");
        } else if (id == R.id.btn2) {
            inputNumber("2");
        } else if (id == R.id.btn3) {
            inputNumber("3");
        } else if (id == R.id.btn4) {
            inputNumber("4");
        } else if (id == R.id.btn5) {
            inputNumber("5");
        } else if (id == R.id.btn6) {
            inputNumber("6");
        } else if (id == R.id.btn7) {
            inputNumber("7");
        } else if (id == R.id.btn8) {
            inputNumber("8");
        } else if (id == R.id.btn9) {
            inputNumber("9");
        } else if (id == R.id.btnDecimal) {
            inputDecimal();
        } else if (id == R.id.btnAdd) {
            setOperator("+");
        } else if (id == R.id.btnSubtract) {
            setOperator("-");
        } else if (id == R.id.btnMultiply) {
            setOperator("×");
        } else if (id == R.id.btnDivide) {
            setOperator("÷");
        } else if (id == R.id.btnPercent) {
            calculatePercent();
        } else if (id == R.id.btnClear) {
            clearAll();
        } else if (id == R.id.btnDelete) {
            deleteLast();
        } else if (id == R.id.btnEquals) {
            calculate();
        }
    }

    private void inputNumber(String number) {
        if (startNewInput) {
            currentInput = number;
            startNewInput = false;
        } else {
            if (currentInput.equals("0")) {
                currentInput = number;
            } else {
                if (currentInput.length() < 15) {
                    currentInput += number;
                }
            }
        }
        updateDisplay();
    }

    private void inputDecimal() {
        if (startNewInput) {
            currentInput = "0.";
            startNewInput = false;
        } else {
            if (!currentInput.contains(".")) {
                currentInput += ".";
            }
        }
        updateDisplay();
    }

    private void setOperator(String op) {
        if (!operator.isEmpty() && !startNewInput) {
            calculate();
        }
        previousInput = currentInput;
        operator = op;
        startNewInput = true;
        updateDisplay();
    }

    private void calculate() {
        if (operator.isEmpty() || previousInput.isEmpty()) {
            return;
        }

        try {
            double num1 = Double.parseDouble(previousInput);
            double num2 = Double.parseDouble(currentInput);
            double result = 0;

            if (operator.equals("+")) {
                result = num1 + num2;
            } else if (operator.equals("-")) {
                result = num1 - num2;
            } else if (operator.equals("×")) {
                result = num1 * num2;
            } else if (operator.equals("÷")) {
                if (num2 == 0) {
                    showError();
                    return;
                }
                result = num1 / num2;
            }

            currentInput = formatResult(result);
            tvHistory.setText(previousInput + " " + operator + " " + num2);
            previousInput = "";
            operator = "";
            startNewInput = true;
            updateDisplay();

        } catch (NumberFormatException e) {
            showError();
        }
    }

    private void calculatePercent() {
        try {
            double num = Double.parseDouble(currentInput);
            double result = num / 100.0;
            currentInput = formatResult(result);
            startNewInput = true;
            updateDisplay();
        } catch (NumberFormatException e) {
            showError();
        }
    }

    private void clearAll() {
        currentInput = "0";
        previousInput = "";
        operator = "";
        startNewInput = true;
        hasError = false;
        tvHistory.setText("");
        updateDisplay();
    }

    private void deleteLast() {
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

    private void showError() {
        currentInput = "Error";
        previousInput = "";
        operator = "";
        startNewInput = true;
        hasError = true;
        tvHistory.setText("");
        updateDisplay();
    }

    private String formatResult(double result) {
        if (result == (long) result) {
            return String.valueOf((long) result);
        } else {
            String formatted = String.format("%.10f", result);
            // Remove trailing zeros
            formatted = formatted.replaceAll("0+?$", "");
            formatted = formatted.replaceAll("[.]$", "");
            return formatted;
        }
    }

    private void updateDisplay() {
        tvDisplay.setText(currentInput);
        
        // Auto-adjust text size for long numbers
        if (currentInput.length() > 10) {
            tvDisplay.setTextSize(40);
        } else if (currentInput.length() > 7) {
            tvDisplay.setTextSize(50);
        } else {
            tvDisplay.setTextSize(64);
        }
    }
}