package com.generatedappname.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {

    private TextView display;
    private StringBuilder currentInput = new StringBuilder();
    private double firstOperand = 0;
    private String pendingOperator = null;
    private boolean resetInput = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        display = findViewById(R.id.display);

        int[] digitIds = {
                R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3,
                R.id.btn4, R.id.btn5, R.id.btn6, R.id.btn7,
                R.id.btn8, R.id.btn9
        };

        for (int id : digitIds) {
            findViewById(id).setOnClickListener(digitClickListener);
        }

        findViewById(R.id.btnAdd).setOnClickListener(operatorClickListener);
        findViewById(R.id.btnSubtract).setOnClickListener(operatorClickListener);
        findViewById(R.id.btnMultiply).setOnClickListener(operatorClickListener);
        findViewById(R.id.btnDivide).setOnClickListener(operatorClickListener);

        findViewById(R.id.btnEquals).setOnClickListener(equalsClickListener);
        findViewById(R.id.btnClear).setOnClickListener(clearClickListener);
    }

    private View.OnClickListener digitClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (resetInput) {
                currentInput.setLength(0);
                resetInput = false;
            }
            Button b = (Button) v;
            currentInput.append(b.getText());
            display.setText(currentInput.toString());
        }
    };

    private View.OnClickListener operatorClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (currentInput.length() == 0) {
                return;
            }
            firstOperand = Double.parseDouble(currentInput.toString());
            pendingOperator = ((Button) v).getText().toString();
            resetInput = true;
        }
    };

    private View.OnClickListener equalsClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (pendingOperator == null || currentInput.length() == 0) {
                return;
            }
            double secondOperand = Double.parseDouble(currentInput.toString());
            double result = 0;
            if (pendingOperator.equals("+")) {
                result = firstOperand + secondOperand;
            } else if (pendingOperator.equals("-")) {
                result = firstOperand - secondOperand;
            } else if (pendingOperator.equals("*")) {
                result = firstOperand * secondOperand;
            } else if (pendingOperator.equals("/")) {
                if (secondOperand == 0) {
                    display.setText("Error");
                    resetInput = true;
                    pendingOperator = null;
                    return;
                }
                result = firstOperand / secondOperand;
            }
            display.setText(String.valueOf(result));
            currentInput.setLength(0);
            currentInput.append(String.valueOf(result));
            resetInput = true;
            pendingOperator = null;
        }
    };

    private View.OnClickListener clearClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            currentInput.setLength(0);
            display.setText("0");
            firstOperand = 0;
            pendingOperator = null;
            resetInput = false;
        }
    };
}
