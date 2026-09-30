package com.generatedappname.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {

    private TextView display;
    private String currentInput = "";
    private double result = 0;
    private String pendingOperator = "";
    private boolean resetNext = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        display = findViewById(R.id.display);

        int[] buttonIds = new int[]{
                R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
                R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9,
                R.id.btnAdd, R.id.btnSub, R.id.btnMul, R.id.btnDiv,
                R.id.btnEquals, R.id.btnClear
        };

        for (int id : buttonIds) {
            findViewById(id).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onButtonClick(v);
                }
            });
        }
    }

    private void onButtonClick(View v) {
        int id = v.getId();
        switch (id) {
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
        if (resetNext) {
            currentInput = "";
            resetNext = false;
        }
        currentInput += digit;
        display.setText(currentInput);
    }

    private void setOperator(String op) {
        if (!currentInput.isEmpty()) {
            calculateResult();
            pendingOperator = op;
            resetNext = true;
        } else if (!display.getText().toString().isEmpty()) {
            pendingOperator = op;
        }
    }

    private void calculateResult() {
        if (currentInput.isEmpty() && !display.getText().toString().isEmpty()) {
            currentInput = display.getText().toString();
        }
        if (currentInput.isEmpty()) return;
        double inputValue = Double.parseDouble(currentInput);
        if (pendingOperator.isEmpty()) {
            result = inputValue;
        } else {
            switch (pendingOperator) {
                case "+": result += inputValue; break;
                case "-": result -= inputValue; break;
                case "*": result *= inputValue; break;
                case "/": result /= inputValue; break;
            }
        }
        display.setText(String.valueOf(result));
        currentInput = "";
        pendingOperator = "";
        resetNext = true;
    }

    private void clearAll() {
        currentInput = "";
        result = 0;
        pendingOperator = "";
        resetNext = false;
        display.setText("0");
    }
}
