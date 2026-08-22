package com.calculatorpro.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.text.DecimalFormat;

public class MainActivity extends Activity implements View.OnClickListener {

    private TextView tvExpression;
    private TextView tvDisplay;

    private String currentInput = "";
    private String lastExpression = "";
    private double operand1 = Double.NaN;
    private double operand2 = Double.NaN;
    private char activeOperator = ' ';
    private boolean isResultDisplayed = false;
    private DecimalFormat decimalFormat = new DecimalFormat("#.########");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvExpression = (TextView) findViewById(R.id.tvExpression);
        tvDisplay = (TextView) findViewById(R.id.tvDisplay);

        int[] buttonIds = {
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9,
            R.id.btnDot, R.id.btnAdd, R.id.btnSub, R.id.btnMul, R.id.btnDiv,
            R.id.btnEqual, R.id.btnClear, R.id.btnDel, R.id.btnSign
        };

        for (int id : buttonIds) {
            View button = findViewById(id);
            if (button != null) {
                button.setOnClickListener(this);
            }
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.btn0) appendNumber("0");
        else if (id == R.id.btn1) appendNumber("1");
        else if (id == R.id.btn2) appendNumber("2");
        else if (id == R.id.btn3) appendNumber("3");
        else if (id == R.id.btn4) appendNumber("4");
        else if (id == R.id.btn5) appendNumber("5");
        else if (id == R.id.btn6) appendNumber("6");
        else if (id == R.id.btn7) appendNumber("7");
        else if (id == R.id.btn8) appendNumber("8");
        else if (id == R.id.btn9) appendNumber("9");
        else if (id == R.id.btnDot) appendDot();
        else if (id == R.id.btnClear) clear();
        else if (id == R.id.btnDel) deleteLast();
        else if (id == R.id.btnSign) toggleSign();
        else if (id == R.id.btnAdd) setOperator('+');
        else if (id == R.id.btnSub) setOperator('-');
        else if (id == R.id.btnMul) setOperator('*');
        else if (id == R.id.btnDiv) setOperator('/');
        else if (id == R.id.btnEqual) calculate();
    }

    private void appendNumber(String number) {
        if (isResultDisplayed) {
            currentInput = number;
            isResultDisplayed = false;
        } else {
            if (currentInput.equals("0")) {
                currentInput = number;
            } else {
                currentInput += number;
            }
        }
        updateDisplay();
    }

    private void appendDot() {
        if (isResultDisplayed) {
            currentInput = "0.";
            isResultDisplayed = false;
        } else {
            if (!currentInput.contains(".")) {
                if (currentInput.isEmpty()) {
                    currentInput = "0.";
                } else {
                    currentInput += ".";
                }
            }
        }
        updateDisplay();
    }

    private void clear() {
        currentInput = "";
        lastExpression = "";
        operand1 = Double.NaN;
        operand2 = Double.NaN;
        activeOperator = ' ';
        isResultDisplayed = false;
        tvExpression.setText("");
        tvDisplay.setText("0");
    }

    private void deleteLast() {
        if (isResultDisplayed) {
            clear();
            return;
        }
        if (currentInput.length() > 0) {
            currentInput = currentInput.substring(0, currentInput.length() - 1);
            updateDisplay();
        }
    }

    private void toggleSign() {
        if (currentInput.isEmpty() || currentInput.equals("0")) return;
        if (currentInput.startsWith("-")) {
            currentInput = currentInput.substring(1);
        } else {
            currentInput = "-" + currentInput;
        }
        updateDisplay();
    }

    private void setOperator(char operator) {
        if (!currentInput.isEmpty()) {
            if (!Double.isNaN(operand1)) {
                calculateIntermediate();
            } else {
                operand1 = Double.parseDouble(currentInput);
            }
        } else if (Double.isNaN(operand1)) {
            operand1 = 0.0;
        }
        activeOperator = operator;
        lastExpression = decimalFormat.format(operand1) + " " + activeOperator;
        tvExpression.setText(lastExpression);
        currentInput = "";
        isResultDisplayed = false;
    }

    private void calculateIntermediate() {
        if (currentInput.isEmpty()) return;
        operand2 = Double.parseDouble(currentInput);
        double result = performOperation(operand1, operand2, activeOperator);
        operand1 = result;
        currentInput = "";
    }

    private void calculate() {
        if (Double.isNaN(operand1) || currentInput.isEmpty() || activeOperator == ' ') return;
        operand2 = Double.parseDouble(currentInput);
        double result = performOperation(operand1, operand2, activeOperator);
        
        if (Double.isInfinite(result) || Double.isNaN(result)) {
            tvDisplay.setText("Error");
            currentInput = "";
            operand1 = Double.NaN;
        } else {
            tvDisplay.setText(decimalFormat.format(result));
            currentInput = String.valueOf(result);
            isResultDisplayed = true;
            operand1 = Double.NaN;
        }
        
        tvExpression.setText("");
        activeOperator = ' ';
    }

    private double performOperation(double op1, double op2, char op) {
        switch (op) {
            case '+': return op1 + op2;
            case '-': return op1 - op2;
            case '*': return op1 * op2;
            case '/': 
                if (op2 == 0) return Double.NaN;
                return op1 / op2;
            default: return op2;
        }
    }

    private void updateDisplay() {
        if (currentInput.isEmpty()) {
            tvDisplay.setText("0");
        } else {
            tvDisplay.setText(currentInput);
        }
    }
}