package com.multitoolsuite.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class CalculatorActivity extends Activity {
    private TextView tvDisplay;
    private double firstOperand = 0;
    private String activeOperator = "";
    private boolean isNewInput = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calculator);

        tvDisplay = (TextView) findViewById(R.id.tv_display);

        int[] numberButtons = {
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3,
            R.id.btn4, R.id.btn5, R.id.btn6, R.id.btn7,
            R.id.btn8, R.id.btn9, R.id.btnDot
        };

        View.OnClickListener numberListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button b = (Button) v;
                String digit = b.getText().toString();
                if (isNewInput) {
                    tvDisplay.setText(digit);
                    isNewInput = false;
                } else {
                    tvDisplay.append(digit);
                }
            }
        };

        for (int id : numberButtons) {
            findViewById(id).setOnClickListener(numberListener);
        }

        int[] operatorButtons = {
            R.id.btnPlus, R.id.btnMinus, R.id.btnMultiply, R.id.btnDivide
        };

        View.OnClickListener opListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button b = (Button) v;
                try {
                    firstOperand = Double.parseDouble(tvDisplay.getText().toString());
                    activeOperator = b.getText().toString();
                    isNewInput = true;
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }
        };

        for (int id : operatorButtons) {
            findViewById(id).setOnClickListener(opListener);
        }

        findViewById(R.id.btnClear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tvDisplay.setText("0");
                firstOperand = 0;
                activeOperator = "";
                isNewInput = true;
            }
        });

        findViewById(R.id.btnEquals).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (activeOperator.isEmpty()) return;
                try {
                    double secondOperand = Double.parseDouble(tvDisplay.getText().toString());
                    double result = 0;
                    if (activeOperator.equals("+")) {
                        result = firstOperand + secondOperand;
                    } else if (activeOperator.equals("-")) {
                        result = firstOperand - secondOperand;
                    } else if (activeOperator.equals("*")) {
                        result = firstOperand * secondOperand;
                    } else if (activeOperator.equals("/")) {
                        if (secondOperand != 0) {
                            result = firstOperand / secondOperand;
                        } else {
                            tvDisplay.setText("Error");
                            isNewInput = true;
                            return;
                        }
                    }
                    if (result == (long) result) {
                        tvDisplay.setText(String.valueOf((long) result));
                    } else {
                        tvDisplay.setText(String.valueOf(result));
                    }
                    activeOperator = "";
                    isNewInput = true;
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }
        });
    }
}
