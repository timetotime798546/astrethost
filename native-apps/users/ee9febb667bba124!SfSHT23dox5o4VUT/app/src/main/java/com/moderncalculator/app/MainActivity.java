package com.moderncalculator.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.text.DecimalFormat;

public class MainActivity extends Activity implements View.OnClickListener {

    private TextView tvFormula;
    private TextView tvDisplay;

    private double operand1 = Double.NaN;
    private double operand2 = Double.NaN;
    private String pendingOperator = "";
    private boolean isNewOp = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvFormula = (TextView) findViewById(R.id.tvFormula);
        tvDisplay = (TextView) findViewById(R.id.tvDisplay);

        // Numeric Buttons
        findViewById(R.id.btn0).setOnClickListener(this);
        findViewById(R.id.btn1).setOnClickListener(this);
        findViewById(R.id.btn2).setOnClickListener(this);
        findViewById(R.id.btn3).setOnClickListener(this);
        findViewById(R.id.btn4).setOnClickListener(this);
        findViewById(R.id.btn5).setOnClickListener(this);
        findViewById(R.id.btn6).setOnClickListener(this);
        findViewById(R.id.btn7).setOnClickListener(this);
        findViewById(R.id.btn8).setOnClickListener(this);
        findViewById(R.id.btn9).setOnClickListener(this);
        findViewById(R.id.btnDot).setOnClickListener(this);

        // Operators & Special
        findViewById(R.id.btnAC).setOnClickListener(this);
        findViewById(R.id.btnDEL).setOnClickListener(this);
        findViewById(R.id.btnPercent).setOnClickListener(this);
        findViewById(R.id.btnToggleSign).setOnClickListener(this);
        findViewById(R.id.btnDiv).setOnClickListener(this);
        findViewById(R.id.btnMul).setOnClickListener(this);
        findViewById(R.id.btnSub).setOnClickListener(this);
        findViewById(R.id.btnAdd).setOnClickListener(this);
        findViewById(R.id.btnEqual).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        // Handle numeric inputs
        if (id == R.id.btn0) { appendDigit("0"); }
        else if (id == R.id.btn1) { appendDigit("1"); }
        else if (id == R.id.btn2) { appendDigit("2"); }
        else if (id == R.id.btn3) { appendDigit("3"); }
        else if (id == R.id.btn4) { appendDigit("4"); }
        else if (id == R.id.btn5) { appendDigit("5"); }
        else if (id == R.id.btn6) { appendDigit("6"); }
        else if (id == R.id.btn7) { appendDigit("7"); }
        else if (id == R.id.btn8) { appendDigit("8"); }
        else if (id == R.id.btn9) { appendDigit("9"); }
        else if (id == R.id.btnDot) {
            if (isNewOp) {
                tvDisplay.setText("0.");
                isNewOp = false;
            } else {
                String current = tvDisplay.getText().toString();
                if (!current.contains(".")) {
                    tvDisplay.setText(current + ".");
                }
            }
        }
        // Handle Action buttons
        else if (id == R.id.btnAC) {
            clearAll();
        }
        else if (id == R.id.btnDEL) {
            if (!isNewOp) {
                String current = tvDisplay.getText().toString();
                if (current.length() > 1) {
                    tvDisplay.setText(current.substring(0, current.length() - 1));
                } else {
                    tvDisplay.setText("0");
                    isNewOp = true;
                }
            }
        }
        else if (id == R.id.btnPercent) {
            try {
                double val = Double.parseDouble(tvDisplay.getText().toString());
                val = val / 100.0;
                tvDisplay.setText(formatResult(val));
                isNewOp = true;
            } catch (NumberFormatException e) {
                tvDisplay.setText("Error");
            }
        }
        else if (id == R.id.btnToggleSign) {
            try {
                double val = Double.parseDouble(tvDisplay.getText().toString());
                val = val * -1.0;
                tvDisplay.setText(formatResult(val));
            } catch (NumberFormatException e) {
                tvDisplay.setText("Error");
            }
        }
        // Handle Operators
        else if (id == R.id.btnDiv) { setOperator("÷"); }
        else if (id == R.id.btnMul) { setOperator("×"); }
        else if (id == R.id.btnSub) { setOperator("-"); }
        else if (id == R.id.btnAdd) { setOperator("+"); }
        // Equal
        else if (id == R.id.btnEqual) {
            performEqual();
        }
    }

    private void appendDigit(String digit) {
        if (isNewOp) {
            tvDisplay.setText(digit);
            isNewOp = false;
        } else {
            String current = tvDisplay.getText().toString();
            if (current.equals("0")) {
                tvDisplay.setText(digit);
            } else {
                tvDisplay.setText(current + digit);
            }
        }
    }

    private void setOperator(String op) {
        try {
            double currentVal = Double.parseDouble(tvDisplay.getText().toString());
            if (!Double.isNaN(operand1) && !isNewOp) {
                calculate(currentVal);
            }
            operand1 = Double.parseDouble(tvDisplay.getText().toString());
            pendingOperator = op;
            tvFormula.setText(formatResult(operand1) + " " + pendingOperator);
            isNewOp = true;
        } catch (NumberFormatException e) {
            tvDisplay.setText("Error");
        }
    }

    private void performEqual() {
        if (Double.isNaN(operand1)) {
            return;
        }
        try {
            double currentVal = Double.parseDouble(tvDisplay.getText().toString());
            operand2 = currentVal;
            tvFormula.setText(formatResult(operand1) + " " + pendingOperator + " " + formatResult(operand2) + " =");
            calculate(currentVal);
            operand1 = Double.NaN;
            pendingOperator = "";
        } catch (NumberFormatException e) {
            tvDisplay.setText("Error");
        }
    }

    private void calculate(double nextOperand) {
        double result = 0;
        boolean hasError = false;
        if (pendingOperator.equals("+")) {
            result = operand1 + nextOperand;
        } else if (pendingOperator.equals("-")) {
            result = operand1 - nextOperand;
        } else if (pendingOperator.equals("×")) {
            result = operand1 * nextOperand;
        } else if (pendingOperator.equals("÷")) {
            if (nextOperand == 0) {
                hasError = true;
            } else {
                result = operand1 / nextOperand;
            }
        }

        if (hasError) {
            tvDisplay.setText("Error");
            operand1 = Double.NaN;
        } else {
            tvDisplay.setText(formatResult(result));
            operand1 = result;
        }
        isNewOp = true;
    }

    private void clearAll() {
        operand1 = Double.NaN;
        operand2 = Double.NaN;
        pendingOperator = "";
        tvDisplay.setText("0");
        tvFormula.setText("");
        isNewOp = true;
    }

    private String formatResult(double d) {
        if (Double.isInfinite(d) || Double.isNaN(d)) {
            return "Error";
        }
        if (d == (long) d) {
            return String.format("%d", (long) d);
        } else {
            DecimalFormat df = new DecimalFormat("#.##########");
            return df.format(d);
        }
    }
}
