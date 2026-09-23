package com.moderncalculator.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private TextView tvExpression;
    private TextView tvResult;
    private View btnToggleHistory;
    private View btnClearHistory;
    private LinearLayout historyPanel;
    private LinearLayout historyContainer;

    private String expression = "";
    private boolean isResultCalculated = false;
    private List<HistoryRecord> historyList = new ArrayList<>();

    private static class HistoryRecord {
        String expr;
        String res;
        HistoryRecord(String expr, String res) {
            this.expr = expr;
            this.res = res;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind Views
        tvExpression = (TextView) findViewById(R.id.tv_expression);
        tvResult = (TextView) findViewById(R.id.tv_result);
        btnToggleHistory = findViewById(R.id.btn_toggle_history);
        btnClearHistory = findViewById(R.id.btn_clear_history);
        historyPanel = (LinearLayout) findViewById(R.id.history_panel);
        historyContainer = (LinearLayout) findViewById(R.id.history_container);

        setupButtonListeners();
        setupHistoryActions();
    }

    private void setupButtonListeners() {
        int[] numberButtonIds = {
            R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4,
            R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9
        };

        View.OnClickListener numberClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerHaptic(v);
                Button btn = (Button) v;
                if (isResultCalculated) {
                    expression = "";
                    isResultCalculated = false;
                }
                expression += btn.getText().toString();
                updateDisplay();
            }
        };

        for (int id : numberButtonIds) {
            findViewById(id).setOnClickListener(numberClickListener);
        }

        // Action / Operator listeners
        findViewById(R.id.btn_dot).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerHaptic(v);
                if (isResultCalculated) {
                    expression = "0";
                    isResultCalculated = false;
                }
                if (canAppendDot()) {
                    if (expression.isEmpty() || isLastCharOperator()) {
                        expression += "0";
                    }
                    expression += ".";
                    updateDisplay();
                }
            }
        });

        int[] opIds = {R.id.btn_add, R.id.btn_sub, R.id.btn_mul, R.id.btn_div};
        View.OnClickListener opClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerHaptic(v);
                Button btn = (Button) v;
                String opSymbol = btn.getText().toString();
                if (isResultCalculated) {
                    expression = tvResult.getText().toString();
                    isResultCalculated = false;
                }
                
                if (!expression.isEmpty()) {
                    if (isLastCharOperator()) {
                        // Replace last operator
                        expression = expression.substring(0, expression.length() - 1) + opSymbol;
                    } else {
                        expression += opSymbol;
                    }
                    updateDisplay();
                }
            }
        };

        for (int id : opIds) {
            findViewById(id).setOnClickListener(opClickListener);
        }

        findViewById(R.id.btn_percent).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerHaptic(v);
                if (!expression.isEmpty() && !isLastCharOperator()) {
                    expression += "%";
                    updateDisplay();
                }
            }
        });

        findViewById(R.id.btn_negate).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerHaptic(v);
                applyNegation();
            }
        });

        findViewById(R.id.btn_clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerHaptic(v);
                expression = "";
                isResultCalculated = false;
                updateDisplay();
            }
        });

        findViewById(R.id.btn_backspace).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerHaptic(v);
                if (isResultCalculated) {
                    expression = "";
                    isResultCalculated = false;
                } else if (!expression.isEmpty()) {
                    expression = expression.substring(0, expression.length() - 1);
                }
                updateDisplay();
            }
        });

        findViewById(R.id.btn_equal).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerHaptic(v);
                evaluateFinal();
            }
        });
    }

    private void setupHistoryActions() {
        btnToggleHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerHaptic(v);
                if (historyPanel.getVisibility() == View.VISIBLE) {
                    historyPanel.setVisibility(View.GONE);
                } else {
                    historyPanel.setVisibility(View.VISIBLE);
                }
            }
        });

        btnClearHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerHaptic(v);
                historyList.clear();
                historyContainer.removeAllViews();
            }
        });
    }

    private void updateDisplay() {
        tvExpression.setText(expression);
        if (expression.isEmpty()) {
            tvResult.setText("0");
        } else {
            // Live temporary assessment as user types
            try {
                String sanitized = sanitizeExpression(expression);
                double liveValue = eval(sanitized);
                tvResult.setText(formatResult(liveValue));
            } catch (Exception e) {
                // Keep the current working evaluation static on parser exception
            }
        }
    }

    private void applyNegation() {
        if (expression.isEmpty()) return;
        if (isResultCalculated) {
            expression = tvResult.getText().toString();
            isResultCalculated = false;
        }

        // Toggle negation of the last numeric component
        int idx = expression.length() - 1;
        while (idx >= 0 && (Character.isDigit(expression.charAt(idx)) || expression.charAt(idx) == '.')) {
            idx--;
        }

        if (idx < 0) {
            expression = "−" + expression;
        } else if (expression.charAt(idx) == '−') {
            // If preceding operator is minus sign and represents negative, drop it or replace
            if (idx == 0 || isOperator(expression.charAt(idx - 1))) {
                expression = expression.substring(0, idx) + expression.substring(idx + 1);
            } else {
                expression = expression.substring(0, idx) + "+" + expression.substring(idx + 1);
            }
        } else if (expression.charAt(idx) == '+') {
            expression = expression.substring(0, idx) + "−" + expression.substring(idx + 1);
        } else {
            expression = expression.substring(0, idx + 1) + "−" + expression.substring(idx + 1);
        }
        updateDisplay();
    }

    private boolean isLastCharOperator() {
        if (expression.isEmpty()) return false;
        char c = expression.charAt(expression.length() - 1);
        return c == '+' || c == '−' || c == '×' || c == '÷';
    }

    private boolean isOperator(char c) {
        return c == '+' || c == '−' || c == '×' || c == '÷';
    }

    private boolean canAppendDot() {
        if (expression.isEmpty()) return true;
        int len = expression.length();
        for (int i = len - 1; i >= 0; i--) {
            char c = expression.charAt(i);
            if (c == '.') return false;
            if (isOperator(c)) return true;
        }
        return true;
    }

    private void evaluateFinal() {
        if (expression.isEmpty()) return;
        try {
            String origExpr = expression;
            String sanitized = sanitizeExpression(expression);
            double value = eval(sanitized);
            String formatted = formatResult(value);
            
            tvResult.setText(formatted);
            tvExpression.setText(origExpr);
            
            // Log History
            addHistoryItem(origExpr, formatted);
            
            expression = formatted;
            isResultCalculated = true;
        } catch (Exception e) {
            tvResult.setText("Error");
        }
    }

    private String sanitizeExpression(String expr) {
        // Map UI visual characters to clean parsing operators
        String res = expr.replace('×', '*').replace('÷', '/').replace('−', '-');
        
        // Handle standalone percentage numbers (e.g. 5% -> 5/100, 5+20% -> 5+20/100*5 or simplified 5+20/100)
        // For standard robust execution we interpret 'x%' as 'x/100' or '*0.01'
        StringBuilder sb = new StringBuilder();
        int len = res.length();
        for (int i = 0; i < len; i++) {
            char c = res.charAt(i);
            if (c == '%') {
                sb.append("/100");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private String formatResult(double val) {
        if (Double.isInfinite(val) || Double.isNaN(val)) {
            return "Error";
        }
        DecimalFormat df = new DecimalFormat("###,###.########");
        return df.format(val);
    }

    private void addHistoryItem(final String expr, final String result) {
        final HistoryRecord record = new HistoryRecord(expr, result);
        historyList.add(0, record);

        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.item_history, historyContainer, false);
        
        TextView tvHExpr = (TextView) view.findViewById(R.id.history_expr);
        TextView tvHRes = (TextView) view.findViewById(R.id.history_result);

        tvHExpr.setText(expr);
        tvHRes.setText(result);

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerHaptic(v);
                expression = expr;
                isResultCalculated = false;
                updateDisplay();
                historyPanel.setVisibility(View.GONE);
            }
        });

        // Add to topmost index inside linear scrolling area
        historyContainer.addView(view, 0);
    }

    private void triggerHaptic(View view) {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
    }

    // High performance Math Parser (Mathematical Operator Precedence Evaluator)
    public static double eval(final String str) {
        return new Object() {
            int pos = -1, ch;

            void nextChar() {
                ch = (++pos < str.length()) ? str.charAt(pos) : -1;
            }

            boolean consume(int charToConsume) {
                while (ch == ' ') nextChar();
                if (ch == charToConsume) {
                    nextChar();
                    return true;
                }
                return false;
            }

            double parse() {
                nextChar();
                double x = parseExpression();
                if (pos < str.length()) throw new RuntimeException("Unexpected expression layout: " + (char)ch);
                return x;
            }

            double parseExpression() {
                double x = parseTerm();
                for (;;) {
                    if      (consume('+')) x += parseTerm(); // Addition
                    else if (consume('-')) x -= parseTerm(); // Subtraction
                    else return x;
                }
            }

            double parseTerm() {
                double x = parseFactor();
                for (;;) {
                    if      (consume('*')) x *= parseFactor(); // Multiplication
                    else if (consume('/')) {
                        double val = parseFactor();
                        if (val == 0) throw new ArithmeticException("Division by zero");
                        x /= val; // Division
                    }
                    else return x;
                }
            }

            double parseFactor() {
                if (consume('+')) return parseFactor(); // Unary plus
                if (consume('-')) return -parseFactor(); // Unary minus

                double x;
                int startPos = this.pos;
                if (consume('(')) { // Parentheses priority grouping
                    x = parseExpression();
                    consume(')');
                } else if ((ch >= '0' && ch <= '9') || ch == '.') { // Real numerical representation
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    String doubleStr = str.substring(startPos, this.pos);
                    // Double dot correction protection
                    if (doubleStr.equals(".")) {
                        x = 0;
                    } else {
                        x = Double.parseDouble(doubleStr);
                    }
                } else {
                    throw new RuntimeException("Unexpected mathematical operator: " + (char)ch);
                }

                return x;
            }
        }.parse();
    }
}