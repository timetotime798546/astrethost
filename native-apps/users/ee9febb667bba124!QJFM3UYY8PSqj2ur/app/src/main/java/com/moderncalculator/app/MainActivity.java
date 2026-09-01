package com.moderncalculator.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {

    private TextView tvHistory;
    private TextView tvDisplay;

    private double firstValue = 0;
    private String currentOperator = "";
    private boolean isOperatorPressed = false;
    private boolean hasJustEvaluated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvHistory = (TextView) findViewById(R.id.tvHistory);
        tvDisplay = (TextView) findViewById(R.id.tvDisplay);

        setupButtonListeners();
    }

    private void setupButtonListeners() {
        int[] numberIds = {
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9
        };

        View.OnClickListener numberClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button b = (Button) v;
                String digit = b.getText().toString();
                appendNumber(digit);
            }
        };

        for (int id : numberIds) {
            findViewById(id).setOnClickListener(numberClickListener);
        }

        findViewById(R.id.btnDot).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                appendDot();
            }
        });

        findViewById(R.id.btnAC).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearAll();
            }
        });

        findViewById(R.id.btnPlusMinus).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleSign();
            }
        });

        findViewById(R.id.btnPercent).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyPercent();
            }
        });

        int[] operatorIds = {
            R.id.btnPlus, R.id.btnMinus, R.id.btnMultiply, R.id.btnDivide
        };

        View.OnClickListener operatorClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button b = (Button) v;
                setOperator(b.getText().toString());
            }
        };

        for (int id : operatorIds) {
            findViewById(id).setOnClickListener(operatorClickListener);
        }

        findViewById(R.id.btnEquals).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calculateResult();
            }
        });
    }

    private void appendNumber(String digit) {
        if (tvDisplay.getText().toString().equals("0") || isOperatorPressed || hasJustEvaluated) {
            tvDisplay.setText(digit);
            isOperatorPressed = false;
            hasJustEvaluated = false;
        } else {
            String current = tvDisplay.getText().toString();
            if (current.length() < 12) {
                tvDisplay.setText(current + digit);
            }
        }
    }

    private void appendDot() {
        if (isOperatorPressed || hasJustEvaluated) {
            tvDisplay.setText("0.");
            isOperatorPressed = false;
            hasJustEvaluated = false;
            return;
        }
        String current = tvDisplay.getText().toString();
        if (!current.contains(".")) {
            tvDisplay.setText(current + ".");
        }
    }

    private void clearAll() {
        tvDisplay.setText("0");
        tvHistory.setText("");
        firstValue = 0;
        currentOperator = "";
        isOperatorPressed = false;
        hasJustEvaluated = false;
    }

    private void toggleSign() {
        String current = tvDisplay.getText().toString();
        if (current.equals("0") || current.equals("Error")) {
            return;
        }
        if (current.startsWith("-")) {
            tvDisplay.setText(current.substring(1));
        } else {
            tvDisplay.setText("-" + current);
        }
    }

    private void applyPercent() {
        String current = tvDisplay.getText().toString();
        try {
            double val = Double.parseDouble(current);
            val = val / 100.0;
            tvDisplay.setText(formatValue(val));
        } catch (NumberFormatException e) {
            tvDisplay.setText("Error");
        }
    }

    private void setOperator(String op) {
        try {
            String current = tvDisplay.getText().toString();
            double secondValue = Double.parseDouble(current);

            if (!currentOperator.isEmpty() && !isOperatorPressed) {
                firstValue = performOperation(firstValue, secondValue, currentOperator);
                tvDisplay.setText(formatValue(firstValue));
            } else {
                firstValue = secondValue;
            }

            currentOperator = op;
            tvHistory.setText(formatValue(firstValue) + " " + currentOperator);
            isOperatorPressed = true;
            hasJustEvaluated = false;
        } catch (NumberFormatException e) {
            tvDisplay.setText("Error");
        }
    }

    private void calculateResult() {
        if (currentOperator.isEmpty()) {
            return;
        }
        try {
            String current = tvDisplay.getText().toString();
            double secondValue = Double.parseDouble(current);
            double result = performOperation(firstValue, secondValue, currentOperator);

            tvHistory.setText(formatValue(firstValue) + " " + currentOperator + " " + formatValue(secondValue) + " =");
            tvDisplay.setText(formatValue(result));

            firstValue = result;
            currentOperator = "";
            hasJustEvaluated = true;
        } catch (NumberFormatException e) {
            tvDisplay.setText("Error");
        }
    }

    private double performOperation(double v1, double v2, String op) {
        if (op.equals("+")) return v1 + v2;
        if (op.equals("-")) return v1 - v2;
        if (op.equals("×")) return v1 * v2;
        if (op.equals("÷")) {
            if (v2 == 0) {
                return Double.NaN;
            }
            return v1 / v2;
        }
        return v2;
    }

    private String formatValue(double value) {
        if (Double.isInfinite(value) || Double.isNaN(value)) {
            return "Error";
        }
        if (value == (long) value) {
            return String.format("%d", (long) value);
        } else {
            String formatted = String.format("%.8f", value);
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
