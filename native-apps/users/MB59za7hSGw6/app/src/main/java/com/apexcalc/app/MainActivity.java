package com.apexcalc.app;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class MainActivity extends Activity implements HistoryAdapter.OnHistorySelectedListener {

    private TextView tvExpression;
    private TextView tvResult;
    private LinearLayout layoutSciRow;
    private LinearLayout layoutHistoryPanel;
    private TextView tvEmptyHistory;
    private ListView lvHistory;

    // Loader views
    private RelativeLayout layoutAppLoader;
    private TextView tvLoaderStatus;

    private Button btnToggleSci;
    private Button btnOpenHistory;
    private Button btnClearHistory;
    private Button btnCloseHistory;

    private StringBuilder currentExpression = new StringBuilder();
    private boolean isEvaluated = false;
    private boolean isSciExpanded = false;

    private HistoryManager historyManager;
    private HistoryAdapter historyAdapter;

    private Handler mainHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        historyManager = new HistoryManager(this);

        initViews();
        bindKeypadButtons();
        bindUtilityButtons();
        updateDisplay();

        // Initialize and display app startup loader
        showStartupLoader();
    }

    private void initViews() {
        tvExpression = (TextView) findViewById(R.id.tv_expression);
        tvResult = (TextView) findViewById(R.id.tv_result);
        layoutSciRow = (LinearLayout) findViewById(R.id.layout_sci_row);
        layoutHistoryPanel = (LinearLayout) findViewById(R.id.layout_history_panel);
        tvEmptyHistory = (TextView) findViewById(R.id.tv_empty_history);
        lvHistory = (ListView) findViewById(R.id.lv_history);

        layoutAppLoader = (RelativeLayout) findViewById(R.id.layout_app_loader);
        tvLoaderStatus = (TextView) findViewById(R.id.tv_loader_status);

        btnToggleSci = (Button) findViewById(R.id.btn_toggle_sci);
        btnOpenHistory = (Button) findViewById(R.id.btn_open_history);
        btnClearHistory = (Button) findViewById(R.id.btn_clear_history);
        btnCloseHistory = (Button) findViewById(R.id.btn_close_history);

        List<HistoryItem> historyList = historyManager.getHistory();
        historyAdapter = new HistoryAdapter(this, historyList, this);
        lvHistory.setAdapter(historyAdapter);
    }

    private void showStartupLoader() {
        if (layoutAppLoader == null) return;

        layoutAppLoader.setVisibility(View.VISIBLE);
        layoutAppLoader.setAlpha(1.0f);

        // Step 1: Update status to initializing engine
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (tvLoaderStatus != null) {
                    tvLoaderStatus.setText("Initializing calculation engine...");
                }
            }
        }, 500);

        // Step 2: Finalize workspace and fade out loader
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (layoutAppLoader != null) {
                    layoutAppLoader.animate()
                            .alpha(0.0f)
                            .setDuration(400)
                            .withEndAction(new Runnable() {
                                @Override
                                public void run() {
                                    if (layoutAppLoader != null) {
                                        layoutAppLoader.setVisibility(View.GONE);
                                    }
                                }
                            });
                }
            }
        }, 1200);
    }

    private void bindKeypadButtons() {
        int[] numIds = new int[]{
                R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3,
                R.id.btn_4, R.id.btn_5, R.id.btn_6, R.id.btn_7,
                R.id.btn_8, R.id.btn_9
        };

        for (int i = 0; i < numIds.length; i++) {
            final Button b = (Button) findViewById(numIds[i]);
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    appendSymbol(b.getText().toString());
                }
            });
        }

        // Operators
        findViewById(R.id.btn_add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                appendOperator("+");
            }
        });
        findViewById(R.id.btn_subtract).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                appendOperator("−");
            }
        });
        findViewById(R.id.btn_multiply).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                appendOperator("×");
            }
        });
        findViewById(R.id.btn_divide).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                appendOperator("÷");
            }
        });
        findViewById(R.id.btn_percent).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                appendOperator("%");
            }
        });

        // Decimal Point
        findViewById(R.id.btn_decimal).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                appendDecimal();
            }
        });

        // Negate
        findViewById(R.id.btn_negate).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleNegate();
            }
        });

        // Clear (AC)
        findViewById(R.id.btn_clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearAll();
            }
        });

        // Backspace
        findViewById(R.id.btn_backspace).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleBackspace();
            }
        });

        // Equals
        findViewById(R.id.btn_equals).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performEvaluation();
            }
        });

        // Scientific functions
        findViewById(R.id.btn_sqrt).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                appendFunction("√");
            }
        });
        findViewById(R.id.btn_power).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                appendOperator("^");
            }
        });
        findViewById(R.id.btn_pi).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                appendSymbol("π");
            }
        });
        findViewById(R.id.btn_open_bracket).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                appendSymbol("(");
            }
        });
        findViewById(R.id.btn_close_bracket).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                appendSymbol(")");
            }
        });
    }

    private void bindUtilityButtons() {
        btnToggleSci.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isSciExpanded = !isSciExpanded;
                layoutSciRow.setVisibility(isSciExpanded ? View.VISIBLE : View.GONE);
                btnToggleSci.setText(isSciExpanded ? "fx ▾" : "fx");
            }
        });

        btnOpenHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openHistory();
            }
        });

        btnCloseHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                layoutHistoryPanel.setVisibility(View.GONE);
            }
        });

        btnClearHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                historyManager.clearHistory();
                refreshHistoryList();
                Toast.makeText(MainActivity.this, "History cleared", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void appendSymbol(String symbol) {
        if (isEvaluated) {
            currentExpression.setLength(0);
            isEvaluated = false;
        }
        currentExpression.append(symbol);
        updateDisplay();
        calculateLivePreview();
    }

    private void appendOperator(String op) {
        if (currentExpression.length() == 0) {
            if ("−".equals(op)) {
                currentExpression.append(op);
                updateDisplay();
            }
            return;
        }

        if (isEvaluated) {
            isEvaluated = false;
        }

        char last = currentExpression.charAt(currentExpression.length() - 1);
        if (isOperatorChar(last)) {
            // Replace last operator with new one
            currentExpression.setCharAt(currentExpression.length() - 1, op.charAt(0));
        } else {
            currentExpression.append(op);
        }

        updateDisplay();
        calculateLivePreview();
    }

    private void appendDecimal() {
        if (isEvaluated) {
            currentExpression.setLength(0);
            currentExpression.append("0.");
            isEvaluated = false;
            updateDisplay();
            return;
        }

        if (currentExpression.length() == 0) {
            currentExpression.append("0.");
            updateDisplay();
            return;
        }

        // Check if current token already has a dot
        int idx = currentExpression.length() - 1;
        boolean hasDot = false;
        while (idx >= 0) {
            char c = currentExpression.charAt(idx);
            if (isOperatorChar(c) || c == '(' || c == ')') {
                break;
            }
            if (c == '.') {
                hasDot = true;
                break;
            }
            idx--;
        }

        if (!hasDot) {
            char last = currentExpression.charAt(currentExpression.length() - 1);
            if (isOperatorChar(last) || last == '(') {
                currentExpression.append("0.");
            } else {
                currentExpression.append(".");
            }
            updateDisplay();
        }
    }

    private void appendFunction(String func) {
        if (isEvaluated) {
            currentExpression.setLength(0);
            isEvaluated = false;
        }
        currentExpression.append(func);
        updateDisplay();
    }

    private void toggleNegate() {
        if (currentExpression.length() == 0) return;

        String expr = currentExpression.toString();
        if (expr.startsWith("-")) {
            currentExpression.deleteCharAt(0);
        } else {
            currentExpression.insert(0, "-");
        }
        updateDisplay();
        calculateLivePreview();
    }

    private void handleBackspace() {
        if (isEvaluated) {
            clearAll();
            return;
        }

        if (currentExpression.length() > 0) {
            currentExpression.deleteCharAt(currentExpression.length() - 1);
            updateDisplay();
            calculateLivePreview();
        }
    }

    private void clearAll() {
        currentExpression.setLength(0);
        isEvaluated = false;
        tvExpression.setText("");
        tvResult.setText("0");
    }

    private void updateDisplay() {
        tvExpression.setText(currentExpression.toString());
        if (currentExpression.length() == 0) {
            tvResult.setText("0");
        }
    }

    private void calculateLivePreview() {
        String expr = currentExpression.toString().trim();
        if (expr.isEmpty()) {
            tvResult.setText("0");
            return;
        }

        char last = expr.charAt(expr.length() - 1);
        if (isOperatorChar(last) || last == '(' || last == '√') {
            return;
        }

        try {
            double val = MathEvaluator.evaluate(expr);
            String formatted = MathEvaluator.formatResult(val);
            tvResult.setText(formatted);
        } catch (Exception e) {
            // Keep previous display silent during ongoing typing
        }
    }

    private void performEvaluation() {
        String expr = currentExpression.toString().trim();
        if (expr.isEmpty()) return;

        try {
            double val = MathEvaluator.evaluate(expr);
            String formatted = MathEvaluator.formatResult(val);

            // Record to persistent history
            historyManager.addRecord(expr, formatted);

            tvExpression.setText(expr + " =");
            tvResult.setText(formatted);

            currentExpression.setLength(0);
            currentExpression.append(formatted.replace(",", ""));
            isEvaluated = true;

        } catch (MathEvaluator.EvaluationException e) {
            if ("Divide by zero".equals(e.getMessage())) {
                tvResult.setText(getString(R.string.error_divide_zero));
            } else {
                tvResult.setText(getString(R.string.error_syntax));
            }
        }
    }

    private boolean isOperatorChar(char c) {
        return c == '+' || c == '−' || c == '-' || c == '×' || c == '*' || c == '÷' || c == '/' || c == '%' || c == '^';
    }

    private void openHistory() {
        refreshHistoryList();
        layoutHistoryPanel.setVisibility(View.VISIBLE);
    }

    private void refreshHistoryList() {
        List<HistoryItem> list = historyManager.getHistory();
        historyAdapter.updateList(list);
        if (list.isEmpty()) {
            tvEmptyHistory.setVisibility(View.VISIBLE);
            lvHistory.setVisibility(View.GONE);
        } else {
            tvEmptyHistory.setVisibility(View.GONE);
            lvHistory.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onSelectHistory(HistoryItem item) {
        currentExpression.setLength(0);
        currentExpression.append(item.getResult().replace(",", ""));
        tvExpression.setText(item.getExpression() + " =");
        tvResult.setText(item.getResult());
        isEvaluated = true;
        layoutHistoryPanel.setVisibility(View.GONE);
    }

    @Override
    public void onBackPressed() {
        if (layoutAppLoader != null && layoutAppLoader.getVisibility() == View.VISIBLE) {
            // Do not dismiss during initial launch load
            return;
        }

        if (layoutHistoryPanel.getVisibility() == View.VISIBLE) {
            layoutHistoryPanel.setVisibility(View.GONE);
        } else {
            super.onBackPressed();
        }
    }
}