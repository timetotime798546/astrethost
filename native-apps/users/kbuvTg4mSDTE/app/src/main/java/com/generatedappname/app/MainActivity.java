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
        switch (v.getId()) {
            case R.id.btn0: appendDigit("0"); break;
            case R.id.btn1: appendDigit("1"); break;
            case R.id.btn2: appendDigit("2"); break;
            case R.id.btn3: appendDigit("3"); break;
            case R.id.btn4: appendDigit("4"); break;
            case R.id.btn5: appendDigit("5"); break;
            case R.id.btn6: appendDigit("6"); break;
            case R.id.btn7: appendDigit("7"); break;
            case R.id.btn8: appendDigit("8"); break;
            case R.id.btn9: appendDigit("9"); break;
            case R.id.btnAdd: setOperator("+"); break;
            case R.id.btnSub: setOperator("-"); break;
            case R.id.btnMul: setOperator("*"); break;
            case R.id.btnDiv: setOperator("/"); break;
            case R.id.btnEquals: calculateResult(); break;
            case R.id.btnClear: clearAll(); break;
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
        switch (operator) {
            case "+": result = firstOperand + secondOperand; break;
            case "-": result = firstOperand - secondOperand; break;
            case "*": result = firstOperand * secondOperand; break;
            case "/":
                if (secondOperand == 0) {
                    resultView.setText("Error");
                    return;
                }
                result = firstOperand / secondOperand;
                break;
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
