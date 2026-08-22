package com.easycalculator.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity implements View.OnClickListener {

    private TextView tvFormula;
    private TextView tvDisplay;
    private double operand1 = Double.NaN;
    private double operand2;
    private String pendingOperator = "";
    private boolean isNewOp = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvFormula = (TextView) findViewById(R.id.tv_formula);
        tvDisplay = (TextView) findViewById(R.id.tv_display);

        findViewById(R.id.btn_0).setOnClickListener(this);
        findViewById(R.id.btn_1).setOnClickListener(this);
        findViewById(R.id.btn_2).setOnClickListener(this);
        findViewById(R.id.btn_3).setOnClickListener(this);
        findViewById(R.id.btn_4).setOnClickListener(this);
        findViewById(R.id.btn_5).setOnClickListener(this);
        findViewById(R.id.btn_6).setOnClickListener(this);
        findViewById(R.id.btn_7).setOnClickListener(this);
        findViewById(R.id.btn_8).setOnClickListener(this);
        findViewById(R.id.btn_9).setOnClickListener(this);

        findViewById(R.id.btn_add).setOnClickListener(this);
        findViewById(R.id.btn_sub).setOnClickListener(this);
        findViewById(R.id.btn_mul).setOnClickListener(this);
        findViewById(R.id.btn_div).setOnClickListener(this);
        findViewById(R.id.btn_dec).setOnClickListener(this);
        findViewById(R.id.btn_sign).setOnClickListener(this);
        findViewById(R.id.btn_clear).setOnClickListener(this);
        findViewById(R.id.btn_del).setOnClickListener(this);
        findViewById(R.id.btn_eq).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_0) { onDigitClick("0"); }
        else if (id == R.id.btn_1) { onDigitClick("1"); }
        else if (id == R.id.btn_2) { onDigitClick("2"); }
        else if (id == R.id.btn_3) { onDigitClick("3"); }
        else if (id == R.id.btn_4) { onDigitClick("4"); }
        else if (id == R.id.btn_5) { onDigitClick("5"); }
        else if (id == R.id.btn_6) { onDigitClick("6"); }
        else if (id == R.id.btn_7) { onDigitClick("7"); }
        else if (id == R.id.btn_8) { onDigitClick("8"); }
        else if (id == R.id.btn_9) { onDigitClick("9"); }
        else if (id == R.id.btn_dec) { onDigitClick("."); }
        else if (id == R.id.btn_add) { onOperatorClick("+"); }
        else if (id == R.id.btn_sub) { onOperatorClick("-"); }
        else if (id == R.id.btn_mul) { onOperatorClick("*"); }
        else if (id == R.id.btn_div) { onOperatorClick("/"); }
        else if (id == R.id.btn_clear) { onClearClick(); }
        else if (id == R.id.btn_del) { onDelClick(); }
        else if (id == R.id.btn_sign) { onSignClick(); }
        else if (id == R.id.btn_eq) { onEqualClick(); }
    }

    private void onDigitClick(String digit) {
        if (isNewOp) {
            tvDisplay.setText("");
            isNewOp = false;
        }
        String current = tvDisplay.getText().toString();
        if (digit.equals(".")) {
            if (current.contains(".")) {
                return;
            }
            if (current.isEmpty()) {
                current = "0";
            }
        }
        tvDisplay.setText(current + digit);
    }

    private void onOperatorClick(String op) {
        String valStr = tvDisplay.getText().toString();
        if (!valStr.isEmpty() && !valStr.equals("Error")) {
            if (!pendingOperator.isEmpty() && !isNewOp) {
                calculate();
            } else {
                try {
                    operand1 = Double.parseDouble(valStr);
                } catch (NumberFormatException e) {
                    return;
                }
            }
            pendingOperator = op;
            tvFormula.setText(formatResult(operand1) + " " + op);
            isNewOp = true;
        }
    }

    private void onEqualClick() {
        String valStr = tvDisplay.getText().toString();
        if (!valStr.isEmpty() && !valStr.equals("Error") && !pendingOperator.isEmpty()) {
            calculate();
            tvFormula.setText("");
            pendingOperator = "";
            isNewOp = true;
        }
    }

    private void onClearClick() {
        tvDisplay.setText("0");
        tvFormula.setText("");
        operand1 = Double.NaN;
        pendingOperator = "";
        isNewOp = true;
    }

    private void onDelClick() {
        if (isNewOp) {
            tvDisplay.setText("0");
            return;
        }
        String current = tvDisplay.getText().toString();
        if (current.length() > 0 && !current.equals("Error")) {
            current = current.substring(0, current.length() - 1);
            if (current.isEmpty()) {
                current = "0";
                isNewOp = true;
            }
            tvDisplay.setText(current);
        }
    }

    private void onSignClick() {
        String valStr = tvDisplay.getText().toString();
        if (!valStr.isEmpty() && !valStr.equals("Error") && !valStr.equals("0")) {
            if (valStr.startsWith("-")) {
                tvDisplay.setText(valStr.substring(1));
            } else {
                tvDisplay.setText("-" + valStr);
            }
        }
    }

    private void calculate() {
        if (!Double.isNaN(operand1) && !pendingOperator.isEmpty()) {
            String valStr = tvDisplay.getText().toString();
            if (!valStr.isEmpty()) {
                try {
                    operand2 = Double.parseDouble(valStr);
                } catch (NumberFormatException e) {
                    return;
                }
                double result = 0;
                if (pendingOperator.equals("+")) {
                    result = operand1 + operand2;
                } else if (pendingOperator.equals("-")) {
                    result = operand1 - operand2;
                } else if (pendingOperator.equals("*")) {
                    result = operand1 * operand2;
                } else if (pendingOperator.equals("/")) {
                    if (operand2 == 0) {
                        tvDisplay.setText("Error");
                        operand1 = Double.NaN;
                        pendingOperator = "";
                        isNewOp = true;
                        return;
                    }
                    result = operand1 / operand2;
                }
                String resultStr = formatResult(result);
                tvDisplay.setText(resultStr);
                operand1 = result;
            }
        } else {
            try {
                operand1 = Double.parseDouble(tvDisplay.getText().toString());
            } catch (NumberFormatException e) {
                // ignore
            }
        }
    }

    private String formatResult(double d) {
        if (d == (long) d) {
            return String.format("%d", (long) d);
        } else {
            return String.valueOf(d);
        }
    }
}