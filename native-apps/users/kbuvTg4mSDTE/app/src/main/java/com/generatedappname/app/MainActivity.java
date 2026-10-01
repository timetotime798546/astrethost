package com.generatedappname.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity implements View.OnClickListener {

    private EditText inputField;
    private TextView resultView;
    private String currentInput = "";
    private double firstOperand = 0;
    private String operator = "";
    private boolean operatorPressed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inputField = findViewById(R.id.inputField);
        resultView = findViewById(R.id.resultView);

        int[] buttonIds = new int[]{
                R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
                R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9,
                R.id.btnAdd, R.id.btnSub, R.id.btnMul, R.id.btnDiv,
                R.id.btnEquals, R.id.btnClear
        };

        for (int id : buttonIds) {
            findViewById(id).setOnClickListener(this);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn0) {
            appendDigit("0");
        } else if (id == R.id.btn1) {
            appendDigit("1");
        } else if (id == R.id.btn2) {
            appendDigit("2");
        } else if (id == R.id.btn3) {
            appendDigit("3");
        } else if (id == R.id.btn4) {
            appendDigit("4");
        } else if (id == R.id.btn5) {
            appendDigit("5");
        } else if (id == R.id.btn6) {
            appendDigit("6");
        } else if (id == R.id.btn7) {
            appendDigit("7");
        } else if (id == R.id.btn8) {
            appendDigit("8");
        } else if (id == R.id.btn9) {
            appendDigit("9");
        } else if (id == R.id.btnAdd) {
            setOperator("+");
        } else if (id == R.id.btnSub) {
            setOperator("-");
        } else if (id == R.id.btnMul) {
            setOperator("*");
        } else if (id == R.id.btnDiv) {
            setOperator("/");
        } else if (id == R.id.btnEquals) {
            calculateResult();
        } else if (id == R.id.btnClear) {
            clearAll();
        }
    }

    private void appendDigit(String digit) {
        if (operatorPressed) {
            inputField.setText("");
            operatorPressed = false;
        }
        currentInput += digit;
        inputField.setText(currentInput);
    }

    private void setOperator(String op) {
        if (!currentInput.isEmpty()) {
            firstOperand = Double.parseDouble(currentInput);
            operator = op;
            operatorPressed = true;
        }
    }

    private void calculateResult() {
        if (operator.isEmpty() || currentInput.isEmpty()) {
            return;
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
            if (secondOperand == 0) {
                resultView.setText("Error");
                return;
            }
            result = firstOperand / secondOperand;
        }
        
        resultView.setText(String.valueOf(result));
        currentInput = String.valueOf(result);
        operator = "";
    }

    private void clearAll() {
        currentInput = "";
        firstOperand = 0;
        operator = "";
        operatorPressed = false;
        inputField.setText("");
        resultView.setText("");
    }
}