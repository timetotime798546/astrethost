package com.luminacalc.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class MainActivity extends Activity implements HistoryAdapter.OnItemClickListener {

    private TextView tvExpression;
    private TextView tvResult;
    private Button btnModeToggle;
    private Button btnSciToggle;
    private Button btnHistory;
    private LinearLayout layoutScientificDrawer;

    private CalculatorEngine calculatorEngine;
    private HistoryManager historyManager;

    private StringBuilder currentExpression;
    private boolean isEvaluated = false;
    private Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        calculatorEngine = new CalculatorEngine();
        historyManager = new HistoryManager(this);
        currentExpression = new StringBuilder();
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        initViews();
        setupKeypadListeners();
        setupScientificListeners();
        setupHeaderControls();
    }

    private void initViews() {
        tvExpression = (TextView) findViewById(R.id.tv_expression);
        tvResult = (TextView) findViewById(R.id.tv_result);
        btnModeToggle = (Button) findViewById(R.id.btn_mode_toggle);
        btnSciToggle = (Button) findViewById(R.id.btn_sci_toggle);
        btnHistory = (Button) findViewById(R.id.btn_history);
        layoutScientificDrawer = (LinearLayout) findViewById(R.id.layout_scientific_drawer);
    }

    private void setupHeaderControls() {
        btnModeToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hapticPulse();
                boolean current = calculatorEngine.isDegreeMode();
                calculatorEngine.setDegreeMode(!current);
                btnModeToggle.setText(!current ? R.string.deg_mode : R.string.rad_mode);
                computeLivePreview();
            }
        });

        btnSciToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hapticPulse();
                if (layoutScientificDrawer.getVisibility() == View.VISIBLE) {
                    layoutScientificDrawer.setVisibility(View.GONE);
                    btnSciToggle.setTextColor(getResources().getColor(R.color.text_primary));
                } else {
                    layoutScientificDrawer.setVisibility(View.VISIBLE);
                    btnSciToggle.setTextColor(getResources().getColor(R.color.operator_accent));
                }
            }
        });

        btnHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hapticPulse();
                showHistoryDialog();
            }
        });
    }

    private void setupKeypadListeners() {
        int[] digitIds = new int[]{
                R.id.btn_digit_0, R.id.btn_digit_1, R.id.btn_digit_2, R.id.btn_digit_3,
                R.id.btn_digit_4, R.id.btn_digit_5, R.id.btn_digit_6, R.id.btn_digit_7,
                R.id.btn_digit_8, R.id.btn_digit_9
        };

        for (int i = 0; i < digitIds.length; i++) {
            final int digit = i;
            findViewById(digitIds[i]).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onDigitPressed(String.valueOf(digit));
                }
            });
        }

        findViewById(R.id.btn_decimal).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDecimalPressed();
            }
        });

        findViewById(R.id.btn_op_add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onOperatorPressed(" + ");
            }
        });

        findViewById(R.id.btn_op_subtract).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onOperatorPressed(" − ");
            }
        });

        findViewById(R.id.btn_op_multiply).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onOperatorPressed(" × ");
            }
        });

        findViewById(R.id.btn_op_divide).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onOperatorPressed(" ÷ ");
            }
        });

        findViewById(R.id.btn_percent).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onOperatorPressed(" % ");
            }
        });

        findViewById(R.id.btn_sqrt).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onFunctionWrapperPressed("sqrt");
            }
        });

        findViewById(R.id.btn_parentheses).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onParenthesesPressed();
            }
        });

        findViewById(R.id.btn_clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClearPressed();
            }
        });

        findViewById(R.id.btn_backspace).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackspacePressed();
            }
        });

        findViewById(R.id.btn_backspace).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                onClearPressed();
                return true;
            }
        });

        findViewById(R.id.btn_negate).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onNegatePressed();
            }
        });

        findViewById(R.id.btn_equal).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onEqualPressed();
            }
        });
    }

    private void setupScientificListeners() {
        findViewById(R.id.btn_sci_sin).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onFunctionWrapperPressed("sin");
            }
        });

        findViewById(R.id.btn_sci_cos).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onFunctionWrapperPressed("cos");
            }
        });

        findViewById(R.id.btn_sci_tan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onFunctionWrapperPressed("tan");
            }
        });

        findViewById(R.id.btn_sci_ln).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onFunctionWrapperPressed("ln");
            }
        });

        findViewById(R.id.btn_sci_log).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onFunctionWrapperPressed("log");
            }
        });

        findViewById(R.id.btn_sci_pi).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hapticPulse();
                if (isEvaluated) currentExpression.setLength(0);
                isEvaluated = false;
                currentExpression.append("3.14159265");
                updateDisplay();
                computeLivePreview();
            }
        });

        findViewById(R.id.btn_sci_e).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hapticPulse();
                if (isEvaluated) currentExpression.setLength(0);
                isEvaluated = false;
                currentExpression.append("2.71828182");
                updateDisplay();
                computeLivePreview();
            }
        });

        findViewById(R.id.btn_sci_pow).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onOperatorPressed(" ^ ");
            }
        });

        findViewById(R.id.btn_sci_fact).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onFunctionWrapperPressed("fact");
            }
        });

        findViewById(R.id.btn_sci_sqr).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hapticPulse();
                if (currentExpression.length() > 0) {
                    currentExpression.append(" ^ 2");
                    updateDisplay();
                    computeLivePreview();
                }
            }
        });
    }

    private void onDigitPressed(String digit) {
        hapticPulse();
        if (isEvaluated) {
            currentExpression.setLength(0);
            isEvaluated = false;
        }
        currentExpression.append(digit);
        updateDisplay();
        computeLivePreview();
    }

    private void onDecimalPressed() {
        hapticPulse();
        if (isEvaluated) {
            currentExpression.setLength(0);
            isEvaluated = false;
        }

        if (currentExpression.length() == 0 || endsWithOperator()) {
            currentExpression.append("0.");
        } else {
            // Find current number token and verify if it already contains '.'
            String str = currentExpression.toString();
            int lastSpace = str.lastIndexOf(" ");
            String token = (lastSpace >= 0) ? str.substring(lastSpace + 1) : str;
            if (!token.contains(".")) {
                currentExpression.append(".");
            }
        }
        updateDisplay();
    }

    private void onOperatorPressed(String op) {
        hapticPulse();
        if (isEvaluated) {
            // Chain from current result
            String prevRes = tvResult.getText().toString();
            if (!prevRes.equals("Error") && !prevRes.contains("Cannot")) {
                currentExpression.setLength(0);
                currentExpression.append(prevRes);
            }
            isEvaluated = false;
        }

        if (currentExpression.length() == 0) {
            if (op.contains("−")) {
                currentExpression.append("0").append(op);
            } else {
                return;
            }
        } else if (endsWithOperator()) {
            // Replace trailing operator
            currentExpression.setLength(currentExpression.length() - 3);
            currentExpression.append(op);
        } else {
            currentExpression.append(op);
        }
        updateDisplay();
    }

    private void onFunctionWrapperPressed(String funcName) {
        hapticPulse();
        if (isEvaluated) {
            currentExpression.setLength(0);
            isEvaluated = false;
        }
        currentExpression.append(funcName).append("(");
        updateDisplay();
    }

    private void onParenthesesPressed() {
        hapticPulse();
        if (isEvaluated) {
            currentExpression.setLength(0);
            isEvaluated = false;
        }

        int openCount = 0;
        int closeCount = 0;
        String str = currentExpression.toString();
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == '(') openCount++;
            else if (str.charAt(i) == ')') closeCount++;
        }

        if (openCount > closeCount && !endsWithOperator() && !str.endsWith("(")) {
            currentExpression.append(")");
        } else {
            if (!str.isEmpty() && !endsWithOperator() && !str.endsWith("(")) {
                currentExpression.append(" × (");
            } else {
                currentExpression.append("(");
            }
        }
        updateDisplay();
        computeLivePreview();
    }

    private void onNegatePressed() {
        hapticPulse();
        if (currentExpression.length() == 0) {
            currentExpression.append("−");
            updateDisplay();
            return;
        }

        String str = currentExpression.toString();
        int lastSpace = str.lastIndexOf(" ");
        if (lastSpace >= 0) {
            String prefix = str.substring(0, lastSpace + 1);
            String token = str.substring(lastSpace + 1);
            if (token.startsWith("-") || token.startsWith("−")) {
                token = token.substring(1);
            } else {
                token = "−" + token;
            }
            currentExpression.setLength(0);
            currentExpression.append(prefix).append(token);
        } else {
            if (str.startsWith("-") || str.startsWith("−")) {
                currentExpression.delete(0, 1);
            } else {
                currentExpression.insert(0, "−");
            }
        }
        updateDisplay();
        computeLivePreview();
    }

    private void onBackspacePressed() {
        hapticPulse();
        if (isEvaluated) {
            isEvaluated = false;
            return;
        }

        if (currentExpression.length() > 0) {
            if (endsWithOperator()) {
                currentExpression.setLength(currentExpression.length() - 3);
            } else {
                currentExpression.setLength(currentExpression.length() - 1);
            }
            updateDisplay();
            computeLivePreview();
        }
    }

    private void onClearPressed() {
        hapticPulse();
        currentExpression.setLength(0);
        isEvaluated = false;
        tvExpression.setText("");
        tvResult.setText("0");
    }

    private void onEqualPressed() {
        hapticPulse();
        if (currentExpression.length() == 0) return;

        String expr = currentExpression.toString();
        String result = calculatorEngine.evaluate(expr);

        tvExpression.setText(expr);
        tvResult.setText(result);
        isEvaluated = true;

        if (!result.equals("Error") && !result.contains("Cannot") && !result.equals("Invalid expression")) {
            historyManager.addEntry(expr, result);
        }
    }

    private void computeLivePreview() {
        if (currentExpression.length() == 0) {
            tvResult.setText("0");
            return;
        }

        String expr = currentExpression.toString();
        // If ends with open operator, don't show parse errors
        if (endsWithOperator()) {
            return;
        }

        String preview = calculatorEngine.evaluate(expr);
        if (!preview.equals("Error") && !preview.contains("Cannot") && !preview.equals("Invalid expression")) {
            tvResult.setText(preview);
        }
    }

    private void updateDisplay() {
        tvExpression.setText(currentExpression.toString());
    }

    private boolean endsWithOperator() {
        String str = currentExpression.toString();
        return str.endsWith(" + ") || str.endsWith(" − ") || str.endsWith(" × ")
                || str.endsWith(" ÷ ") || str.endsWith(" % ") || str.endsWith(" ^ ");
    }

    private void showHistoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_history, null);
        builder.setView(dialogView);

        final AlertDialog dialog = builder.create();

        ListView lvHistory = (ListView) dialogView.findViewById(R.id.lv_history);
        final TextView tvEmpty = (TextView) dialogView.findViewById(R.id.tv_dialog_empty);
        Button btnClearAll = (Button) dialogView.findViewById(R.id.btn_dialog_clear_all);
        Button btnClose = (Button) dialogView.findViewById(R.id.btn_dialog_close);

        final List<CalculationItem> items = historyManager.getHistory();
        final HistoryAdapter adapter = new HistoryAdapter(this, items, new HistoryAdapter.OnItemClickListener() {
            @Override
            public void onHistoryItemClicked(CalculationItem item) {
                currentExpression.setLength(0);
                currentExpression.append(item.getResult());
                tvExpression.setText(item.getExpression());
                tvResult.setText(item.getResult());
                isEvaluated = false;
                dialog.dismiss();
                Toast.makeText(MainActivity.this, "Loaded " + item.getResult(), Toast.LENGTH_SHORT).show();
            }
        });
        lvHistory.setAdapter(adapter);

        if (items.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            lvHistory.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            lvHistory.setVisibility(View.VISIBLE);
        }

        btnClearAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hapticPulse();
                historyManager.clearHistory();
                adapter.updateData(historyManager.getHistory());
                tvEmpty.setVisibility(View.VISIBLE);
                lvHistory.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, "History cleared", Toast.LENGTH_SHORT).show();
            }
        });

        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    @Override
    public void onHistoryItemClicked(CalculationItem item) {
        // Handled in dialog listener
    }

    private void hapticPulse() {
        if (vibrator != null) {
            try {
                vibrator.vibrate(15);
            } catch (Exception ignored) {
            }
        }
    }
}