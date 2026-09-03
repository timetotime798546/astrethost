package com.moderncalculator.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.text.DecimalFormat;
import java.util.ArrayList;

public class MainActivity extends Activity {

    private TextView tvDisplay;
    private TextView tvEquation;
    private TextView tvHistoryContent;
    private ScrollView scrollHistory;
    private View layoutHistory;

    private String operand1 = "";
    private String operand2 = "";
    private String operator = "";
    private boolean isResultMode = false;

    // Memory register and historical storage systems
    private double memoryValue = 0.0;
    private ArrayList<String> calculationHistory = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvDisplay = (TextView) findViewById(R.id.tv_display);
        tvEquation = (TextView) findViewById(R.id.tv_equation);
        tvHistoryContent = (TextView) findViewById(R.id.tv_history_content);
        scrollHistory = (ScrollView) findViewById(R.id.scroll_history);
        layoutHistory = findViewById(R.id.layout_history);

        // Bind Numbers
        int[] numBtnIds = {
            R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4,
            R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9
        };

        for (int id : numBtnIds) {
            findViewById(id).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    triggerHaptic(view);
                    Button btn = (Button) view;
                    onNumberClick(btn.getText().toString());
                }
            });
        }

        // Bind Operators
        findViewById(R.id.btn_add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { triggerHaptic(v); onOperatorClick("+"); }
        });
        findViewById(R.id.btn_subtract).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { triggerHaptic(v); onOperatorClick("−"); }
        });
        findViewById(R.id.btn_multiply).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { triggerHaptic(v); onOperatorClick("×"); }
        });
        findViewById(R.id.btn_divide).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { triggerHaptic(v); onOperatorClick("÷"); }
        });

        // Decimal Point
        findViewById(R.id.btn_dot).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerHaptic(v);
                onDotClick();
            }
        });

        // Backspace
        findViewById(R.id.btn_backspace).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerHaptic(v);
                onBackspaceClick();
            }
        });

        // Clear All
        findViewById(R.id.btn_clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerHaptic(v);
                onClearClick();
            }
        });

        // Toggle Sign
        findViewById(R.id.btn_toggle_sign).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerHaptic(v);
                onToggleSignClick();
            }
        });

        // Percent
        findViewById(R.id.btn_percent).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerHaptic(v);
                onPercentClick();
            }
        });

        // Equals
        findViewById(R.id.btn_equal).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerHaptic(v);
                onEqualClick();
            }
        });

        // Toggle History Drawer
        findViewById(R.id.btn_toggle_history).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerHaptic(v);
                if (layoutHistory.getVisibility() == View.VISIBLE) {
                    layoutHistory.setVisibility(View.GONE);
                } else {
                    layoutHistory.setVisibility(View.VISIBLE);
                    scrollToBottom();
                }
            }
        });

        // Clear History
        findViewById(R.id.btn_clear_history).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerHaptic(v);
                calculationHistory.clear();
                updateHistoryUI();
                Toast.makeText(MainActivity.this, "History Cleared", Toast.LENGTH_SHORT).show();
            }
        });

        // Memory Register Bindings
        findViewById(R.id.btn_mc).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerHaptic(v);
                memoryValue = 0.0;
                Toast.makeText(MainActivity.this, "Memory Cleared (MC)", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btn_mr).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerHaptic(v);
                String formatted = formatDouble(memoryValue);
                if (operator.isEmpty()) {
                    operand1 = formatted;
                    updateDisplay(operand1);
                } else {
                    operand2 = formatted;
                    updateDisplay(operand2);
                }
                updateEquation();
                Toast.makeText(MainActivity.this, "Recalled: " + formatted, Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btn_m_plus).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerHaptic(v);
                try {
                    double currentVal = Double.parseDouble(tvDisplay.getText().toString());
                    memoryValue += currentVal;
                    Toast.makeText(MainActivity.this, "Added to Memory (M+)", Toast.LENGTH_SHORT).show();
                } catch (NumberFormatException ignored) {}
            }
        });

        findViewById(R.id.btn_m_minus).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerHaptic(v);
                try {
                    double currentVal = Double.parseDouble(tvDisplay.getText().toString());
                    memoryValue -= currentVal;
                    Toast.makeText(MainActivity.this, "Subtracted from Memory (M-)", Toast.LENGTH_SHORT).show();
                } catch (NumberFormatException ignored) {}
            }
        });
    }

    private void triggerHaptic(View view) {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
    }

    private void onNumberClick(String value) {
        if (isResultMode) {
            operand1 = "";
            isResultMode = false;
        }

        if (operator.isEmpty()) {
            if (operand1.equals("0")) {
                operand1 = value;
            } else {
                operand1 += value;
            }
            updateDisplay(operand1);
        } else {
            if (operand2.equals("0")) {
                operand2 = value;
            } else {
                operand2 += value;
            }
            updateDisplay(operand2);
        }
        updateEquation();
    }

    private void onOperatorClick(String op) {
        if (operand1.isEmpty()) {
            operand1 = "0";
        }
        if (!operand2.isEmpty()) {
            calculateResult();
        }
        operator = op;
        isResultMode = false;
        updateEquation();
    }

    private void onDotClick() {
        if (isResultMode) {
            operand1 = "0";
            isResultMode = false;
        }

        if (operator.isEmpty()) {
            if (operand1.isEmpty()) {
                operand1 = "0.";
            } else if (!operand1.contains(".")) {
                operand1 += ".";
            }
            updateDisplay(operand1);
        } else {
            if (operand2.isEmpty()) {
                operand2 = "0.";
            } else if (!operand2.contains(".")) {
                operand2 += ".";
            }
            updateDisplay(operand2);
        }
        updateEquation();
    }

    private void onBackspaceClick() {
        if (isResultMode) {
            onClearClick();
            return;
        }

        if (operator.isEmpty()) {
            if (operand1.length() > 0) {
                operand1 = operand1.substring(0, operand1.length() - 1);
                if (operand1.isEmpty() || operand1.equals("-")) {
                    operand1 = "0";
                }
                updateDisplay(operand1);
            }
        } else {
            if (operand2.length() > 0) {
                operand2 = operand2.substring(0, operand2.length() - 1);
                if (operand2.isEmpty() || operand2.equals("-")) {
                    operand2 = "0";
                }
                updateDisplay(operand2);
            }
        }
        updateEquation();
    }

    private void onClearClick() {
        operand1 = "";
        operand2 = "";
        operator = "";
        isResultMode = false;
        tvDisplay.setText("0");
        tvEquation.setText("");
    }

    private void onToggleSignClick() {
        if (operator.isEmpty()) {
            if (!operand1.isEmpty() && !operand1.equals("0")) {
                if (operand1.startsWith("-")) {
                    operand1 = operand1.substring(1);
                } else {
                    operand1 = "-" + operand1;
                }
                updateDisplay(operand1);
            }
        } else {
            if (!operand2.isEmpty() && !operand2.equals("0")) {
                if (operand2.startsWith("-")) {
                    operand2 = operand2.substring(1);
                } else {
                    operand2 = "-" + operand2;
                }
                updateDisplay(operand2);
            }
        }
        updateEquation();
    }

    private void onPercentClick() {
        if (operator.isEmpty()) {
            if (!operand1.isEmpty()) {
                try {
                    double val = Double.parseDouble(operand1) / 100.0;
                    operand1 = formatDouble(val);
                    updateDisplay(operand1);
                } catch (NumberFormatException ignored) {}
            }
        } else {
            if (!operand2.isEmpty()) {
                try {
                    double val = Double.parseDouble(operand2) / 100.0;
                    operand2 = formatDouble(val);
                    updateDisplay(operand2);
                } catch (NumberFormatException ignored) {}
            }
        }
        updateEquation();
    }

    private void onEqualClick() {
        if (!operand1.isEmpty() && !operator.isEmpty() && !operand2.isEmpty()) {
            calculateResult();
            isResultMode = true;
        }
    }

    private void calculateResult() {
        try {
            double op1 = Double.parseDouble(operand1);
            double op2 = Double.parseDouble(operand2);
            double result = 0.0;
            boolean errorOccurred = false;

            if (operator.equals("+")) {
                result = op1 + op2;
            } else if (operator.equals("−")) {
                result = op1 - op2;
            } else if (operator.equals("×")) {
                result = op1 * op2;
            } else if (operator.equals("÷")) {
                if (op2 == 0) {
                    errorOccurred = true;
                } else {
                    result = op1 / op2;
                }
            }

            if (errorOccurred) {
                tvDisplay.setText("Error");
                tvEquation.setText("");
                operand1 = "";
                operand2 = "";
                operator = "";
            } else {
                String fullEquation = operand1 + " " + operator + " " + operand2;
                String resultStr = formatDouble(result);

                // Commit result details into calculationHistory records
                calculationHistory.add(fullEquation + " = " + resultStr);
                updateHistoryUI();

                tvEquation.setText(fullEquation + " =");
                operand1 = resultStr;
                tvDisplay.setText(operand1);
                operand2 = "";
                operator = "";
            }
        } catch (Exception e) {
            tvDisplay.setText("Error");
            tvEquation.setText("");
            operand1 = "";
            operand2 = "";
            operator = "";
        }
    }

    private void updateDisplay(String text) {
        if (text.isEmpty()) {
            tvDisplay.setText("0");
        } else {
            tvDisplay.setText(text);
        }
    }

    private void updateEquation() {
        if (isResultMode) return;
        StringBuilder builder = new StringBuilder();
        if (!operand1.isEmpty()) {
            builder.append(operand1);
        }
        if (!operator.isEmpty()) {
            builder.append(" ").append(operator).append(" ");
        }
        if (!operand2.isEmpty()) {
            builder.append(operand2);
        }
        tvEquation.setText(builder.toString());
    }

    private String formatDouble(double val) {
        if (val == (long) val) {
            return String.format("%d", (long) val);
        } else {
            DecimalFormat df = new DecimalFormat("#.########");
            return df.format(val);
        }
    }

    private void updateHistoryUI() {
        if (calculationHistory.isEmpty()) {
            tvHistoryContent.setText("No calculations yet");
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < calculationHistory.size(); i++) {
                sb.append(calculationHistory.get(i)).append("\n\n");
            }
            tvHistoryContent.setText(sb.toString().trim());
        }
        scrollToBottom();
    }

    private void scrollToBottom() {
        scrollHistory.post(new Runnable() {
            @Override
            public void run() {
                scrollHistory.fullScroll(View.FOCUS_DOWN);
            }
        });
    }
}