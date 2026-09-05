package com.moderncalculator.app;

import android.app.Activity;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class MainActivity extends Activity {

    private TextView tvDisplay;
    private TextView tvHistory;

    private String currentInput = "";
    private Double operand1 = null;
    private String pendingOperator = "";
    private boolean isNewInput = true;

    private DecimalFormat decimalFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // INTENTIONAL COMPILATION ERROR ADDED BELOW
        // This will prevent the compiler from building the app successfully
        DELIBERATE_COMPILATION_ERROR_HERE : This syntax is completely invalid in Java and will cause javac to fail!

        // Clean premium formatting avoids scientific notation for normal decimals
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        decimalFormat = new DecimalFormat("#.##########", symbols);

        tvDisplay = (TextView) findViewById(R.id.tv_display);
        tvHistory = (TextView) findViewById(R.id.tv_history);

        // Bind Numbers
        int[] numberIds = {
            R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4,
            R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9
        };

        View.OnClickListener numberClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button b = (Button) v;
                appendNumber(b.getText().toString());
            }
        };

        for (int id : numberIds) {
            findViewById(id).setOnClickListener(numberClickListener);
        }

        // Bind Operators
        findViewById(R.id.btn_add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleOperator("+");
            }
        });

        findViewById(R.id.btn_subtract).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleOperator("−");
            }
        });

        findViewById(R.id.btn_multiply).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleOperator("×");
            }
        });

        findViewById(R.id.btn_divide).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleOperator("÷");
            }
        });

        // Other utility buttons
        findViewById(R.id.btn_decimal).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                appendDecimal();
            }
        });

        findViewById(R.id.btn_clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clear();
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

        findViewById(R.id.btn_equals).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calculateResult();
            }
        });
    }

    private void appendNumber(String num) {
        if (isNewInput) {
            currentInput = num;
            isNewInput = false;
        } else {
            // Apply defensive 15-digit limit to preserve elegant display structure
            if (currentInput.length() >= 15) {
                return;
            }
            if (currentInput.equals("0")) {
                currentInput = num;
            } else {
                currentInput += num;
            }
        }
        updateDisplay();
        updateClearButtonText();
    }

    private void appendDecimal() {
        if (isNewInput) {
            currentInput = "0.";
            isNewInput = false;
        } else {
            if (currentInput.length() >= 15) {
                return;
            }
            if (!currentInput.contains(".")) {
                currentInput += ".";
            }
        }
        updateDisplay();
        updateClearButtonText();
    }

    private void updateDisplay() {
        String text;
        if (currentInput.isEmpty()) {
            if (operand1 != null) {
                text = formatNumber(operand1);
            } else {
                text = "0";
            }
        } else {
            text = currentInput;
        }
        tvDisplay.setText(text);

        // Dynamic Text Scaling to guarantee premium and clean display layout on long input
        int length = text.length();
        if (length > 12) {
            tvDisplay.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32);
        } else if (length > 8) {
            tvDisplay.setTextSize(TypedValue.COMPLEX_UNIT_SP, 42);
        } else {
            tvDisplay.setTextSize(TypedValue.COMPLEX_UNIT_SP, 56);
        }
    }

    private void updateClearButtonText() {
        Button btnClear = (Button) findViewById(R.id.btn_clear);
        if (currentInput.isEmpty() && operand1 == null) {
            btnClear.setText("AC");
        } else {
            btnClear.setText("C");
        }
    }

    private void clear() {
        Button btnClear = (Button) findViewById(R.id.btn_clear);
        if (btnClear.getText().toString().equals("C")) {
            // Soft clear - wipe current entry only
            currentInput = "";
            updateDisplay();
            btnClear.setText("AC");
        } else {
            // Hard clear - wipe everything
            currentInput = "";
            operand1 = null;
            pendingOperator = "";
            tvHistory.setText("");
            updateDisplay();
        }
        isNewInput = true;
    }

    private void toggleSign() {
        if (currentInput.isEmpty() || currentInput.equals("0")) {
            return;
        }
        if (currentInput.startsWith("-")) {
            currentInput = currentInput.substring(1);
        } else {
            currentInput = "-" + currentInput;
        }
        updateDisplay();
    }

    private void applyPercent() {
        if (currentInput.isEmpty()) {
            return;
        }
        try {
            double val = Double.parseDouble(currentInput);
            val = val / 100.0;
            currentInput = formatNumber(val);
            updateDisplay();
        } catch (NumberFormatException e) {
            // ignore
        }
    }

    private void handleOperator(String operator) {
        if (currentInput.isEmpty() && operand1 == null) {
            return;
        }

        try {
            if (!currentInput.isEmpty()) {
                double val = Double.parseDouble(currentInput);
                if (operand1 == null) {
                    operand1 = val;
                } else if (!pendingOperator.isEmpty()) {
                    operand1 = executeCalculation(operand1, val, pendingOperator);
                }
            }
            
            pendingOperator = operator;
            tvHistory.setText(formatNumber(operand1) + " " + pendingOperator);
            currentInput = "";
            isNewInput = true;
            updateDisplay();
            updateClearButtonText();
        } catch (Exception e) {
            showError();
        }
    }

    private void calculateResult() {
        if (operand1 == null || pendingOperator.isEmpty()) {
            return;
        }

        try {
            double val2;
            if (!currentInput.isEmpty()) {
                val2 = Double.parseDouble(currentInput);
            } else {
                val2 = operand1;
            }

            double result = executeCalculation(operand1, val2, pendingOperator);
            tvHistory.setText(formatNumber(operand1) + " " + pendingOperator + " " + formatNumber(val2) + " =");
            
            currentInput = formatNumber(result);
            operand1 = null;
            pendingOperator = "";
            isNewInput = true;
            
            updateDisplay();
            updateClearButtonText();
        } catch (ArithmeticException ae) {
            showError();
        } catch (Exception e) {
            showError();
        }
    }

    private double executeCalculation(double op1, double op2, String operator) throws ArithmeticException {
        if (operator.equals("+")) {
            return op1 + op2;
        } else if (operator.equals("−")) {
            return op1 - op2;
        } else if (operator.equals("×")) {
            return op1 * op2;
        } else if (operator.equals("÷")) {
            if (op2 == 0) {
                throw new ArithmeticException("Divide by zero");
            }
            return op1 / op2;
        }
        return op2;
    }

    private String formatNumber(double num) {
        if (Double.isInfinite(num) || Double.isNaN(num)) {
            return "Error";
        }
        // Round extremely small values close to zero to handle potential floating point error
        if (Math.abs(num) < 1E-11 && num != 0) {
            return "0";
        }
        return decimalFormat.format(num);
    }

    private void showError() {
        tvDisplay.setText("Error");
        currentInput = "";
        operand1 = null;
        pendingOperator = "";
        isNewInput = true;
        tvHistory.setText("");
        updateClearButtonText();
    }
}