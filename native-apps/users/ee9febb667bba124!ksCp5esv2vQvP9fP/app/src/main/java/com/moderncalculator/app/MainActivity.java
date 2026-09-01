package com.moderncalculator.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity implements View.OnClickListener {

    private TextView tvFormula;
    private TextView tvResult;

    private String currentInput = "";
    private String previousInput = "";
    private String selectedOperator = "";
    private boolean isOperatorPressed = false;
    private boolean hasResultCalculated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind views
        tvFormula = (TextView) findViewById(R.id.tv_formula);
        tvResult = (TextView) findViewById(R.id.tv_result);

        // Set standard onClick listeners sequentially
        int[] actionIds = new int[]{
                R.id.btn_clear, R.id.btn_delete, R.id.btn_percent,
                R.id.btn_div, R.id.btn_mul, R.id.btn_sub, R.id.btn_add,
                R.id.btn_equal, R.id.btn_dot, R.id.btn_plus_minus,
                R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3,
                R.id.btn_4, R.id.btn_5, R.id.btn_6, R.id.btn_7,
                R.id.btn_8, R.id.btn_9
        };

        for (int i = 0; i < actionIds.length; i++) {
            findViewById(actionIds[i]).setOnClickListener(this);
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();

        if (id == R.id.btn_0) {
            onNumberClick("0");
        } else if (id == R.id.btn_1) {
            onNumberClick("1");
        } else if (id == R.id.btn_2) {
            onNumberClick("2");
        } else if (id == R.id.btn_3) {
            onNumberClick("3");
        } else if (id == R.id.btn_4) {
            onNumberClick("4");
        } else if (id == R.id.btn_5) {
            onNumberClick("5");
        } else if (id == R.id.btn_6) {
            onNumberClick("6");
        } else if (id == R.id.btn_7) {
            onNumberClick("7");
        } else if (id == R.id.btn_8) {
            onNumberClick("8");
        } else if (id == R.id.btn_9) {
            onNumberClick("9");
        } else if (id == R.id.btn_dot) {
            onDotClick();
        } else if (id == R.id.btn_plus_minus) {
            onPlusMinusClick();
        } else if (id == R.id.btn_clear) {
            onClearClick();
        } else if (id == R.id.btn_delete) {
            onDeleteClick();
        } else if (id == R.id.btn_percent) {
            onPercentClick();
        } else if (id == R.id.btn_add) {
            onOperatorClick("+");
        } else if (id == R.id.btn_sub) {
            onOperatorClick("-");
        } else if (id == R.id.btn_mul) {
            onOperatorClick("×");
        } else if (id == R.id.btn_div) {
            onOperatorClick("÷");
        } else if (id == R.id.btn_equal) {
            onEqualClick();
        }
    }

    private void onNumberClick(String number) {
        if (hasResultCalculated) {
            currentInput = "";
            hasResultCalculated = false;
        }
        if (isOperatorPressed) {
            currentInput = "";
            isOperatorPressed = false;
        }

        // Prevent multiple starting zeros
        if (currentInput.equals("0") && number.equals("0")) {
            return;
        }
        if (currentInput.equals("0")) {
            currentInput = number;
        } else {
            currentInput += number;
        }
        updateResultView(currentInput);
    }

    private void onDotClick() {
        if (hasResultCalculated) {
            currentInput = "0";
            hasResultCalculated = false;
        }
        if (isOperatorPressed) {
            currentInput = "0";
            isOperatorPressed = false;
        }
        if (!currentInput.contains(".")) {
            if (currentInput.isEmpty()) {
                currentInput = "0";
            }
            currentInput += ".";
            updateResultView(currentInput);
        }
    }

    private void onPlusMinusClick() {
        if (!currentInput.isEmpty() && !currentInput.equals("0")) {
            if (currentInput.startsWith("-")) {
                currentInput = currentInput.substring(1);
            } else {
                currentInput = "-" + currentInput;
            }
            updateResultView(currentInput);
        }
    }

    private void onClearClick() {
        currentInput = "";
        previousInput = "";
        selectedOperator = "";
        isOperatorPressed = false;
        hasResultCalculated = false;
        tvFormula.setText("");
        tvResult.setText("0");
    }

    private void onDeleteClick() {
        if (hasResultCalculated) {
            tvFormula.setText("");
            return;
        }
        if (!currentInput.isEmpty()) {
            currentInput = currentInput.substring(0, currentInput.length() - 1);
            if (currentInput.isEmpty()) {
                tvResult.setText("0");
            } else {
                updateResultView(currentInput);
            }
        }
    }

    private void onPercentClick() {
        if (!currentInput.isEmpty()) {
            try {
                double val = Double.parseDouble(currentInput);
                val = val / 100.0;
                currentInput = formatOutput(val);
                updateResultView(currentInput);
            } catch (Exception e) {
                tvResult.setText("Error");
            }
        }
    }

    private void onOperatorClick(String operator) {
        if (currentInput.isEmpty() && !previousInput.isEmpty()) {
            selectedOperator = operator;
            tvFormula.setText(previousInput + " " + selectedOperator);
            isOperatorPressed = true;
            return;
        }

        if (!currentInput.isEmpty() && !previousInput.isEmpty() && !selectedOperator.isEmpty()) {
            performCalculation();
        }

        if (!currentInput.isEmpty()) {
            previousInput = currentInput;
        }
        selectedOperator = operator;
        tvFormula.setText(previousInput + " " + selectedOperator);
        isOperatorPressed = true;
        hasResultCalculated = false;
    }

    private void onEqualClick() {
        if (previousInput.isEmpty() || currentInput.isEmpty() || selectedOperator.isEmpty()) {
            return;
        }
        tvFormula.setText(previousInput + " " + selectedOperator + " " + currentInput + " =");
        performCalculation();
        selectedOperator = "";
        hasResultCalculated = true;
    }

    private void performCalculation() {
        try {
            double op1 = Double.parseDouble(previousInput);
            double op2 = Double.parseDouble(currentInput);
            double output = 0.0;
            boolean divisionError = false;

            if (selectedOperator.equals("+")) {
                output = op1 + op2;
            } else if (selectedOperator.equals("-")) {
                output = op1 - op2;
            } else if (selectedOperator.equals("×")) {
                output = op1 * op2;
            } else if (selectedOperator.equals("÷")) {
                if (op2 == 0) {
                    divisionError = true;
                } else {
                    output = op1 / op2;
                }
            }

            if (divisionError) {
                tvResult.setText("Error: Div/0");
                currentInput = "";
                previousInput = "";
                selectedOperator = "";
            } else {
                currentInput = formatOutput(output);
                tvResult.setText(currentInput);
                previousInput = currentInput;
            }
        } catch (Exception e) {
            tvResult.setText("Error");
        }
    }

    private void updateResultView(String value) {
        if (value.startsWith("-") && value.length() > 1) {
            tvResult.setText(value);
        } else if (value.equals("-")) {
            tvResult.setText("-");
        } else {
            tvResult.setText(value);
        }
    }

    private String formatOutput(double val) {
        if (val == (long) val) {
            return String.format("%d", (long) val);
        } else {
            // Restrict long decimal output sizes inside layout screen boundaries
            String formatted = String.format("%.8f", val);
            while (formatted.endsWith("0")) {
                formatted = formatted.substring(0, formatted.length() - 1);
            }
            if (formatted.endsWith(".")) {
                formatted = formatted.substring(0, formatted.length() - 1);
            }
            return formatted;
        }
    }
}