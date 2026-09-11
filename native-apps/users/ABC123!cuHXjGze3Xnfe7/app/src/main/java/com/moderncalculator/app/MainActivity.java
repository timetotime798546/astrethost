package com.moderncalculator.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.util.Locale;

public class MainActivity extends Activity {

    private TextView tvFormula;
    private TextView tvDisplay;

    private String currentInput = "0";
    private Double operand1 = null;
    private String pendingOperator = null;
    private boolean isNewInput = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvFormula = (TextView) findViewById(R.id.tv_formula);
        tvDisplay = (TextView) findViewById(R.id.tv_display);

        setupButtons();
    }

    private void setupButtons() {
        int[] buttonIds = new int[] {
            R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4,
            R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9,
            R.id.btn_dot, R.id.btn_plus, R.id.btn_minus, R.id.btn_multiply, R.id.btn_divide,
            R.id.btn_equal, R.id.btn_ac, R.id.btn_del, R.id.btn_percent, R.id.btn_sign
        };

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onButtonClick(v.getId());
            }
        };

        for (int id : buttonIds) {
            View btn = findViewById(id);
            if (btn != null) {
                btn.setOnClickListener(listener);
            }
        }
    }

    private void onButtonClick(int id) {
        if (id == R.id.btn_0) handleDigit("0");
        else if (id == R.id.btn_1) handleDigit("1");
        else if (id == R.id.btn_2) handleDigit("2");
        else if (id == R.id.btn_3) handleDigit("3");
        else if (id == R.id.btn_4) handleDigit("4");
        else if (id == R.id.btn_5) handleDigit("5");
        else if (id == R.id.btn_6) handleDigit("6");
        else if (id == R.id.btn_7) handleDigit("7");
        else if (id == R.id.btn_8) handleDigit("8");
        else if (id == R.id.btn_9) handleDigit("9");
        else if (id == R.id.btn_dot) handleDot();
        else if (id == R.id.btn_plus) handleOperator("+");
        else if (id == R.id.btn_minus) handleOperator("-");
        else if (id == R.id.btn_multiply) handleOperator("*");
        else if (id == R.id.btn_divide) handleOperator("/");
        else if (id == R.id.btn_equal) handleEqual();
        else if (id == R.id.btn_ac) handleAC();
        else if (id == R.id.btn_del) handleDel();
        else if (id == R.id.btn_percent) handlePercent();
        else if (id == R.id.btn_sign) handleSign();
    }

    private void handleDigit(String digit) {
        if (isNewInput) {
            currentInput = digit;
            isNewInput = false;
        } else {
            if (currentInput.equals("0")) {
                currentInput = digit;
            } else {
                currentInput += digit;
            }
        }
        updateDisplay();
    }

    private void handleDot() {
        if (isNewInput) {
            currentInput = "0.";
            isNewInput = false;
        } else {
            if (!currentInput.contains(".")) {
                currentInput += ".";
            }
        }
        updateDisplay();
    }

    private void handleOperator(String operator) {
        double currentVal = getDisplayedValue();

        if (pendingOperator != null && !isNewInput) {
            double res = calculate(operand1, currentVal, pendingOperator);
            operand1 = res;
            currentInput = formatNumber(res);
            updateDisplay();
        } else {
            operand1 = currentVal;
        }

        pendingOperator = operator;
        isNewInput = true;
        tvFormula.setText(formatNumber(operand1) + " " + convertOperatorSymbol(operator));
    }

    private void handleEqual() {
        if (pendingOperator == null) {
            return;
        }

        double operand2 = getDisplayedValue();
        double res = calculate(operand1, operand2, pendingOperator);

        tvFormula.setText(formatNumber(operand1) + " " + convertOperatorSymbol(pendingOperator) + " " + formatNumber(operand2) + " =");
        currentInput = formatNumber(res);
        updateDisplay();

        operand1 = res;
        pendingOperator = null;
        isNewInput = true;
    }

    private void handleAC() {
        currentInput = "0";
        operand1 = null;
        pendingOperator = null;
        isNewInput = true;
        tvFormula.setText("");
        updateDisplay();
    }

    private void handleDel() {
        if (isNewInput) {
            return;
        }
        if (currentInput.length() > 1) {
            currentInput = currentInput.substring(0, currentInput.length() - 1);
        } else {
            currentInput = "0";
        }
        updateDisplay();
    }

    private void handlePercent() {
        double val = getDisplayedValue();
        double res = val / 100.0;
        currentInput = formatNumber(res);
        isNewInput = true;
        updateDisplay();
    }

    private void handleSign() {
        double val = getDisplayedValue();
        if (val != 0) {
            double res = -val;
            currentInput = formatNumber(res);
            updateDisplay();
        }
    }

    private double getDisplayedValue() {
        try {
            return Double.parseDouble(currentInput);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private double calculate(double op1, double op2, String operator) {
        if ("+".equals(operator)) return op1 + op2;
        if ("-".equals(operator)) return op1 - op2;
        if ("*".equals(operator)) return op1 * op2;
        if ("/".equals(operator)) {
            if (op2 == 0) {
                return Double.NaN;
            }
            return op1 / op2;
        }
        return op2;
    }

    private String convertOperatorSymbol(String operator) {
        if ("*".equals(operator)) return "×";
        if ("/".equals(operator)) return "÷";
        return operator;
    }

    private void updateDisplay() {
        tvDisplay.setText(currentInput);
    }

    private String formatNumber(double value) {
        if (Double.isNaN(value)) {
            return "Error";
        }
        if (Double.isInfinite(value)) {
            return "Overflow";
        }
        if (value == (long) value) {
            return String.format(Locale.US, "%d", (long) value);
        } else {
            String s = String.format(Locale.US, "%.8f", value);
            while (s.endsWith("0")) {
                s = s.substring(0, s.length() - 1);
            }
            if (s.endsWith(".")) {
                s = s.substring(0, s.length() - 1);
            }
            return s;
        }
    }
}