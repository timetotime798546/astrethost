package com.moderncalculator.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class MainActivity extends Activity {

    private TextView tvFormula;
    private TextView tvDisplay;

    private String currentInput = "";
    private String activeOperator = "";
    private double firstOperand = Double.NaN;
    private boolean isOperatorJustPressed = false;
    private boolean hasEvaluated = false;

    private DecimalFormat decimalFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize formatting engine
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        decimalFormat = new DecimalFormat("#.##########", symbols);

        // Map digital display layout
        tvFormula = (TextView) findViewById(R.id.tv_formula);
        tvDisplay = (TextView) findViewById(R.id.tv_display);

        // Map interface logic actions
        setupButtonListeners();
    }

    private void setupButtonListeners() {
        // Numeric inputs
        int[] numIds = {
                R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4,
                R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9
        };

        View.OnClickListener numListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Button b = (Button) view;
                appendNumber(b.getText().toString());
            }
        };

        for (int id : numIds) {
            findViewById(id).setOnClickListener(numListener);
        }

        // Action operations
        findViewById(R.id.btn_decimal).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                appendDecimal();
            }
        });

        findViewById(R.id.btn_clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearAll();
            }
        });

        findViewById(R.id.btn_delete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performDelete();
            }
        });

        findViewById(R.id.btn_toggle_sign).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleSign();
            }
        });

        findViewById(R.id.btn_percent).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyPercent();
            }
        });

        // Operators
        findViewById(R.id.btn_add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyOperator("+");
            }
        });

        findViewById(R.id.btn_subtract).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyOperator("−");
            }
        });

        findViewById(R.id.btn_multiply).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyOperator("×");
            }
        });

        findViewById(R.id.btn_divide).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyOperator("÷");
            }
        });

        // Evaluation
        findViewById(R.id.btn_equals).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                evaluate();
            }
        });
    }

    private void appendNumber(String num) {
        if (hasEvaluated || isOperatorJustPressed) {
            currentInput = "";
            hasEvaluated = false;
            isOperatorJustPressed = false;
        }

        // Block excess digit overflows
        if (currentInput.length() >= 15) {
            return;
        }

        if (currentInput.equals("0")) {
            currentInput = num;
        } else {
            currentInput += num;
        }

        updateDisplay(currentInput);
    }

    private void appendDecimal() {
        if (hasEvaluated || isOperatorJustPressed) {
            currentInput = "0";
            hasEvaluated = false;
            isOperatorJustPressed = false;
        }

        if (!currentInput.contains(".")) {
            if (currentInput.isEmpty()) {
                currentInput = "0.";
            } else {
                currentInput += ".";
            }
            updateDisplay(currentInput);
        }
    }

    private void clearAll() {
        currentInput = "";
        firstOperand = Double.NaN;
        activeOperator = "";
        isOperatorJustPressed = false;
        hasEvaluated = false;
        tvFormula.setText("");
        tvDisplay.setText("0");
    }

    private void performDelete() {
        if (hasEvaluated) {
            tvFormula.setText("");
            return;
        }
        if (currentInput.length() > 0) {
            currentInput = currentInput.substring(0, currentInput.length() - 1);
            if (currentInput.isEmpty() || currentInput.equals("-")) {
                currentInput = "0";
            }
            updateDisplay(currentInput);
        }
    }

    private void toggleSign() {
        if (currentInput.isEmpty() || currentInput.equals("0")) {
            return;
        }
        try {
            double val = Double.parseDouble(currentInput);
            val = val * -1;
            currentInput = formatValue(val);
            updateDisplay(currentInput);
        } catch (NumberFormatException ignored) {}
    }

    private void applyPercent() {
        if (currentInput.isEmpty()) {
            return;
        }
        try {
            double val = Double.parseDouble(currentInput);
            val = val / 100.0;
            currentInput = formatValue(val);
            updateDisplay(currentInput);
        } catch (NumberFormatException ignored) {}
    }

    private void applyOperator(String op) {
        try {
            if (!Double.isNaN(firstOperand) && !isOperatorJustPressed && !currentInput.isEmpty()) {
                evaluateIntermediate();
            } else if (!currentInput.isEmpty()) {
                firstOperand = Double.parseDouble(currentInput);
            } else if (Double.isNaN(firstOperand)) {
                firstOperand = 0;
            }

            activeOperator = op;
            isOperatorJustPressed = true;
            hasEvaluated = false;

            // Display standard mathematical formula step on top bar
            tvFormula.setText(formatValue(firstOperand) + " " + activeOperator);
        } catch (NumberFormatException ignored) {}
    }

    private void evaluateIntermediate() {
        if (Double.isNaN(firstOperand) || currentInput.isEmpty()) {
            return;
        }
        try {
            double secondOperand = Double.parseDouble(currentInput);
            double result = compute(firstOperand, secondOperand, activeOperator);
            firstOperand = result;
            updateDisplay(formatValue(result));
        } catch (NumberFormatException ignored) {}
    }

    private void evaluate() {
        if (Double.isNaN(firstOperand) || currentInput.isEmpty() || activeOperator.isEmpty()) {
            return;
        }
        try {
            double secondOperand = Double.parseDouble(currentInput);
            double result = compute(firstOperand, secondOperand, activeOperator);

            tvFormula.setText(formatValue(firstOperand) + " " + activeOperator + " " + formatValue(secondOperand) + " =");
            
            if (Double.isInfinite(result) || Double.isNaN(result)) {
                tvDisplay.setText("Error");
                currentInput = "";
                firstOperand = Double.NaN;
                activeOperator = "";
            } else {
                currentInput = formatValue(result);
                updateDisplay(currentInput);
                firstOperand = result;
            }
            
            hasEvaluated = true;
            isOperatorJustPressed = false;
        } catch (NumberFormatException ignored) {}
    }

    private double compute(double op1, double op2, String operator) {
        if (operator.equals("+")) {
            return op1 + op2;
        } else if (operator.equals("−")) {
            return op1 - op2;
        } else if (operator.equals("×")) {
            return op1 * op2;
        } else if (operator.equals("÷")) {
            if (op2 == 0) {
                return Double.NaN; // Guard division by zero
            }
            return op1 / op2;
        }
        return op2;
    }

    private String formatValue(double value) {
        if (Double.isNaN(value)) {
            return "Error";
        }
        if (value == (long) value) {
            return String.format(Locale.US, "%d", (long) value);
        } else {
            return decimalFormat.format(value);
        }
    }

    private void updateDisplay(String text) {
        tvDisplay.setText(text);
    }
}