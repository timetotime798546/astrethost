package com.moderncalculator.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.text.DecimalFormat;

public class MainActivity extends Activity {

    private TextView tvFormula;
    private TextView tvResult;

    private String currentInput = "";
    private String previousInput = "";
    private String operator = "";
    private boolean isCalculated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvFormula = (TextView) findViewById(R.id.tv_formula);
        tvResult = (TextView) findViewById(R.id.tv_result);

        setNumberClickListener(R.id.btn_0, "0");
        setNumberClickListener(R.id.btn_1, "1");
        setNumberClickListener(R.id.btn_2, "2");
        setNumberClickListener(R.id.btn_3, "3");
        setNumberClickListener(R.id.btn_4, "4");
        setNumberClickListener(R.id.btn_5, "5");
        setNumberClickListener(R.id.btn_6, "6");
        setNumberClickListener(R.id.btn_7, "7");
        setNumberClickListener(R.id.btn_8, "8");
        setNumberClickListener(R.id.btn_9, "9");
        setNumberClickListener(R.id.btn_dot, ".");

        setOperatorClickListener(R.id.btn_add, "+");
        setOperatorClickListener(R.id.btn_subtract, "−");
        setOperatorClickListener(R.id.btn_multiply, "×");
        setOperatorClickListener(R.id.btn_divide, "÷");

        findViewById(R.id.btn_clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clear();
            }
        });

        findViewById(R.id.btn_backspace).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                backspace();
            }
        });

        findViewById(R.id.btn_toggle_sign).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleSign();
            }
        });

        findViewById(R.id.btn_percentage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                percentage();
            }
        });

        findViewById(R.id.btn_equal).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calculate();
            }
        });
    }

    private void setNumberClickListener(int id, final String value) {
        findViewById(id).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isCalculated) {
                    currentInput = "";
                    isCalculated = false;
                }

                if (value.equals(".")) {
                    if (currentInput.isEmpty()) {
                        currentInput = "0.";
                    } else if (!currentInput.contains(".")) {
                        currentInput += ".";
                    }
                } else {
                    if (currentInput.equals("0")) {
                        currentInput = value;
                    } else {
                        currentInput += value;
                    }
                }
                updateDisplay();
            }
        });
    }

    private void setOperatorClickListener(int id, final String op) {
        findViewById(id).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentInput.isEmpty() && !previousInput.isEmpty()) {
                    operator = op;
                    updateFormulaDisplay();
                    return;
                }

                if (!currentInput.isEmpty() && !previousInput.isEmpty() && !operator.isEmpty()) {
                    performMath();
                }

                if (!currentInput.isEmpty()) {
                    previousInput = currentInput;
                    currentInput = "";
                }

                operator = op;
                isCalculated = false;
                updateFormulaDisplay();
                updateDisplay();
            }
        });
    }

    private void performMath() {
        if (previousInput.isEmpty() || currentInput.isEmpty() || operator.isEmpty()) {
            return;
        }

        try {
            double num1 = Double.parseDouble(previousInput);
            double num2 = Double.parseDouble(currentInput);
            double result = 0.0;

            if (operator.equals("+")) {
                result = num1 + num2;
            } else if (operator.equals("−")) {
                result = num1 - num2;
            } else if (operator.equals("×")) {
                result = num1 * num2;
            } else if (operator.equals("÷")) {
                if (num2 != 0.0) {
                    result = num1 / num2;
                } else {
                    tvResult.setText("Error");
                    currentInput = "";
                    previousInput = "";
                    operator = "";
                    return;
                }
            }

            previousInput = formatResult(result);
        } catch (NumberFormatException e) {
            clear();
        }
    }

    private void calculate() {
        if (previousInput.isEmpty() || currentInput.isEmpty() || operator.isEmpty()) {
            return;
        }

        String operand1 = previousInput;
        String operand2 = currentInput;
        String activeOperator = operator;

        performMath();

        tvFormula.setText(operand1 + " " + activeOperator + " " + operand2 + " =");
        currentInput = previousInput;
        previousInput = "";
        operator = "";
        isCalculated = true;

        tvResult.setText(currentInput);
    }

    private void clear() {
        currentInput = "";
        previousInput = "";
        operator = "";
        isCalculated = false;
        tvFormula.setText("");
        tvResult.setText("0");
    }

    private void backspace() {
        if (isCalculated) {
            tvFormula.setText("");
            isCalculated = false;
            return;
        }

        if (currentInput.length() > 0) {
            currentInput = currentInput.substring(0, currentInput.length() - 1);
            updateDisplay();
        }
    }

    private void toggleSign() {
        if (currentInput.isEmpty()) {
            return;
        }
        try {
            double val = Double.parseDouble(currentInput);
            val = val * -1;
            currentInput = formatResult(val);
            updateDisplay();
        } catch (NumberFormatException e) {
            // Safe fallback
        }
    }

    private void percentage() {
        if (currentInput.isEmpty()) {
            return;
        }
        try {
            double val = Double.parseDouble(currentInput);
            val = val / 100.0;
            currentInput = formatResult(val);
            updateDisplay();
        } catch (NumberFormatException e) {
            // Safe fallback
        }
    }

    private void updateDisplay() {
        if (currentInput.isEmpty()) {
            tvResult.setText("0");
        } else {
            tvResult.setText(currentInput);
        }
    }

    private void updateFormulaDisplay() {
        if (!previousInput.isEmpty()) {
            tvFormula.setText(previousInput + " " + operator);
        } else {
            tvFormula.setText("");
        }
    }

    private String formatResult(double d) {
        if (d == (long) d) {
            return String.format("%d", (long) d);
        } else {
            DecimalFormat df = new DecimalFormat("#.########");
            return df.format(d);
        }
    }
}