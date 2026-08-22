package com.simplecalculator.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {

    private TextView tvFormula;
    private TextView tvDisplay;

    private StringBuilder currentInput = new StringBuilder();
    private Double operand1 = null;
    private String pendingOperator = null;
    private boolean isNewOp = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) { 
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvFormula = (TextView) findViewById(R.id.tvFormula);
        tvDisplay = (TextView) findViewById(R.id.tvDisplay);

        setupButtonListeners();
    }

    private void setupButtonListeners() {
        int[] digitButtons = {
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9,
            R.id.btn00
        };

        for (int i = 0; i < digitButtons.length; i++) {
            int id = digitButtons[i];
            final Button btn = (Button) findViewById(id);
            if (btn != null) {
                btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onDigitPressed(btn.getText().toString());
                    }
                });
            }
        }

        int[] operatorButtons = {
            R.id.btnAdd, R.id.btnSubtract, R.id.btnMultiply, R.id.btnDivide
        };

        for (int i = 0; i < operatorButtons.length; i++) {
            int id = operatorButtons[i];
            final Button btn = (Button) findViewById(id);
            if (btn != null) {
                btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onOperatorPressed(btn.getText().toString());
                    }
                });
            }
        }

        Button btnDot = (Button) findViewById(R.id.btnDot);
        if (btnDot != null) {
            btnDot.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onDotPressed();
                }
            });
        }

        Button btnClear = (Button) findViewById(R.id.btnClear);
        if (btnClear != null) {
            btnClear.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onClearPressed();
                }
            });
        }

        Button btnDelete = (Button) findViewById(R.id.btnDelete);
        if (btnDelete != null) {
            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onDeletePressed();
                }
            });
        }

        Button btnPercent = (Button) findViewById(R.id.btnPercent);
        if (btnPercent != null) {
            btnPercent.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onPercentPressed();
                }
            });
        }

        Button btnEqual = (Button) findViewById(R.id.btnEqual);
        if (btnEqual != null) {
            btnEqual.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onEqualPressed();
                }
            });
        }
    }

    private void onDigitPressed(String digit) {
        if (isNewOp) {
            currentInput.setLength(0);
            isNewOp = false;
        }
        
        if (currentInput.toString().equals("0")) {
            if (digit.equals("0") || digit.equals("00")) {
                return;
            } else {
                currentInput.setLength(0);
            }
        }
        
        currentInput.append(digit);
        tvDisplay.setText(currentInput.toString());
    }

    private void onDotPressed() {
        if (isNewOp) {
            currentInput.setLength(0);
            currentInput.append("0");
            isNewOp = false;
        }
        if (currentInput.length() == 0) {
            currentInput.append("0");
        }
        if (currentInput.indexOf(".") == -1) {
            currentInput.append(".");
        }
        tvDisplay.setText(currentInput.toString());
    }

    private void onClearPressed() {
        currentInput.setLength(0);
        operand1 = null;
        pendingOperator = null;
        isNewOp = false;
        tvDisplay.setText("0");
        tvFormula.setText("");
    }

    private void onDeletePressed() {
        if (isNewOp) {
            tvFormula.setText("");
            return;
        }
        if (currentInput.length() > 0) {
            currentInput.setLength(currentInput.length() - 1);
            if (currentInput.length() == 0) {
                tvDisplay.setText("0");
            } else {
                tvDisplay.setText(currentInput.toString());
            }
        }
    }

    private void onPercentPressed() {
        if (currentInput.length() > 0) {
            try {
                double val = Double.parseDouble(currentInput.toString()) / 100.0;
                currentInput.setLength(0);
                currentInput.append(formatValue(val));
                tvDisplay.setText(currentInput.toString());
            } catch (NumberFormatException e) {
                tvDisplay.setText("Error");
            }
        }
    }

    private void onOperatorPressed(String operator) {
        if (currentInput.length() == 0) {
            if (operand1 != null) {
                pendingOperator = operator;
                tvFormula.setText(formatValue(operand1) + " " + pendingOperator);
            }
            return;
        }

        try {
            double currentVal = Double.parseDouble(currentInput.toString());
            if (operand1 == null) {
                operand1 = currentVal;
            } else if (pendingOperator != null) {
                double result = calculate(operand1, currentVal, pendingOperator);
                if (Double.isNaN(result)) {
                    tvDisplay.setText("Error");
                    onClearPressed();
                    return;
                }
                operand1 = result;
                tvDisplay.setText(formatValue(operand1));
            }
            pendingOperator = operator;
            tvFormula.setText(formatValue(operand1) + " " + pendingOperator);
            currentInput.setLength(0);
            isNewOp = false;
        } catch (NumberFormatException e) {
            tvDisplay.setText("Error");
        }
    }

    private void onEqualPressed() {
        if (pendingOperator == null || currentInput.length() == 0) {
            return;
        }

        try {
            double operand2 = Double.parseDouble(currentInput.toString());
            double result = calculate(operand1, operand2, pendingOperator);
            if (Double.isNaN(result)) {
                tvDisplay.setText("Error");
                onClearPressed();
                return;
            }
            tvFormula.setText(formatValue(operand1) + " " + pendingOperator + " " + formatValue(operand2) + " =");
            tvDisplay.setText(formatValue(result));
            operand1 = result;
            currentInput.setLength(0);
            currentInput.append(formatValue(result));
            pendingOperator = null;
            isNewOp = true;
        } catch (NumberFormatException e) {
            tvDisplay.setText("Error");
        }
    }

    private double calculate(double op1, double op2, String operator) { 
        if (operator.equals("+")) return op1 + op2;
        if (operator.equals("-")) return op1 - op2;
        if (operator.equals("x")) return op1 * op2;
        if (operator.equals("/")) {
            if (op2 == 0) return Double.NaN;
            return op1 / op2;
        }
        return op2;
    }

    private String formatValue(double value) {
        if (value == (long) value) {
            return String.format("%d", (long) value);
        } else {
            return String.valueOf(value);
        }
    }
}