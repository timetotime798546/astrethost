package com.moderncalculator.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements View.OnClickListener {

    private TextView tvFormula;
    private TextView tvResult;
    private View layoutHistoryPanel;
    private LinearLayout layoutHistoryItems;

    private String currentExpression = "";
    private boolean isResultDisplayed = false;
    private List<String> calculationHistory = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize display views
        tvFormula = (TextView) findViewById(R.id.tv_formula);
        tvResult = (TextView) findViewById(R.id.tv_result);

        // Initialize history views
        layoutHistoryPanel = findViewById(R.id.layout_history_panel);
        layoutHistoryItems = (LinearLayout) findViewById(R.id.layout_history_items);

        // Wire up control and special logic buttons
        findViewById(R.id.btn_ac).setOnClickListener(this);
        findViewById(R.id.btn_parenthesis).setOnClickListener(this);
        findViewById(R.id.btn_percent).setOnClickListener(this);
        findViewById(R.id.btn_divide).setOnClickListener(this);
        findViewById(R.id.btn_multiply).setOnClickListener(this);
        findViewById(R.id.btn_subtract).setOnClickListener(this);
        findViewById(R.id.btn_add).setOnClickListener(this);
        findViewById(R.id.btn_backspace).setOnClickListener(this);
        findViewById(R.id.btn_dot).setOnClickListener(this);
        findViewById(R.id.btn_equal).setOnClickListener(this);

        // Wire numeric digits keys
        findViewById(R.id.btn_0).setOnClickListener(this);
        findViewById(R.id.btn_1).setOnClickListener(this);
        findViewById(R.id.btn_2).setOnClickListener(this);
        findViewById(R.id.btn_3).setOnClickListener(this);
        findViewById(R.id.btn_4).setOnClickListener(this);
        findViewById(R.id.btn_5).setOnClickListener(this);
        findViewById(R.id.btn_6).setOnClickListener(this);
        findViewById(R.id.btn_7).setOnClickListener(this);
        findViewById(R.id.btn_8).setOnClickListener(this);
        findViewById(R.id.btn_9).setOnClickListener(this);

        // Wire history UI controls
        findViewById(R.id.btn_history_toggle).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showHistoryPanel(true);
            }
        });

        findViewById(R.id.btn_close_history).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showHistoryPanel(false);
            }
        });

        findViewById(R.id.btn_clear_history).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calculationHistory.clear();
                populateHistoryList();
                Toast.makeText(MainActivity.this, "History cleared", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        // Handle Clear operation
        if (id == R.id.btn_ac) {
            currentExpression = "";
            isResultDisplayed = false;
            tvFormula.setText("");
            tvResult.setText("0");
            return;
        }

        // Handle Backspace
        if (id == R.id.btn_backspace) {
            if (isResultDisplayed) {
                currentExpression = "";
                tvFormula.setText("");
                tvResult.setText("0");
                isResultDisplayed = false;
            } else if (currentExpression.length() > 0) {
                currentExpression = currentExpression.substring(0, currentExpression.length() - 1);
                tvFormula.setText(currentExpression);
            }
            return;
        }

        // Handle Evaluation / Equals Click
        if (id == R.id.btn_equal) {
            evaluateCurrentExpression();
            return;
        }

        // Parentheses matching assistant logic
        if (id == R.id.btn_parenthesis) {
            if (isResultDisplayed) {
                currentExpression = "(";
                isResultDisplayed = false;
            } else {
                currentExpression = getAutoParenthesis(currentExpression);
            }
            tvFormula.setText(currentExpression);
            return;
        }

        // Get key string of standard inputs
        String inputCharacter = "";
        if (id == R.id.btn_0) inputCharacter = "0";
        else if (id == R.id.btn_1) inputCharacter = "1";
        else if (id == R.id.btn_2) inputCharacter = "2";
        else if (id == R.id.btn_3) inputCharacter = "3";
        else if (id == R.id.btn_4) inputCharacter = "4";
        else if (id == R.id.btn_5) inputCharacter = "5";
        else if (id == R.id.btn_6) inputCharacter = "6";
        else if (id == R.id.btn_7) inputCharacter = "7";
        else if (id == R.id.btn_8) inputCharacter = "8";
        else if (id == R.id.btn_9) inputCharacter = "9";
        else if (id == R.id.btn_dot) inputCharacter = ".";
        else if (id == R.id.btn_add) inputCharacter = "+";
        else if (id == R.id.btn_subtract) inputCharacter = "-";
        else if (id == R.id.btn_multiply) inputCharacter = "*";
        else if (id == R.id.btn_divide) inputCharacter = "/";
        else if (id == R.id.btn_percent) inputCharacter = "%";

        // If result was shown and user inputs a new operator, continue operation on result.
        // Otherwise, clear and start fresh with number input.
        if (isResultDisplayed) {
            if (isOperator(inputCharacter)) {
                currentExpression = tvResult.getText().toString() + inputCharacter;
            } else {
                currentExpression = inputCharacter;
            }
            isResultDisplayed = false;
        } else {
            // Prevent multiple adjacent duplicate math operators
            if (isOperator(inputCharacter) && currentExpression.length() > 0) {
                char lastChar = currentExpression.charAt(currentExpression.length() - 1);
                if (isOperator(String.valueOf(lastChar))) {
                    currentExpression = currentExpression.substring(0, currentExpression.length() - 1);
                }
            }
            currentExpression += inputCharacter;
        }

        tvFormula.setText(currentExpression);
    }

    private boolean isOperator(String s) {
        return s.equals("+") || s.equals("-") || s.equals("*") || s.equals("/") || s.equals("%");
    }

    private String getAutoParenthesis(String expr) {
        if (expr.isEmpty()) {
            return "(";
        }

        char lastChar = expr.charAt(expr.length() - 1);
        int openCount = 0;
        int closeCount = 0;

        for (int i = 0; i < expr.length(); i++) {
            if (expr.charAt(i) == '(') openCount++;
            if (expr.charAt(i) == ')') closeCount++;
        }

        if (openCount > closeCount) {
            // If the last character is an operator or open bracket, it is safer to open another
            if (isOperator(String.valueOf(lastChar)) || lastChar == '(') {
                return expr + "(";
            } else {
                return expr + ")";
            }
        } else {
            return expr + "(";
        }
    }

    private void evaluateCurrentExpression() {
        if (currentExpression == null || currentExpression.trim().isEmpty()) {
            return;
        }

        try {
            // Balance parentheses before evaluating
            String balancedExpression = balanceParentheses(currentExpression);
            
            double result = eval(balancedExpression);
            
            // Format result decimal format nicely (avoiding exponents unless extreme)
            DecimalFormat df = new DecimalFormat("#.##########");
            String resultStr = df.format(result);

            tvResult.setText(resultStr);

            // Record into local runtime memory history array
            String historyRecord = currentExpression + " = " + resultStr;
            calculationHistory.add(0, historyRecord); // insert at top

            isResultDisplayed = true;
        } catch (Exception e) {
            tvResult.setText("Error");
            isResultDisplayed = true;
        }
    }

    private String balanceParentheses(String expr) {
        int openCount = 0;
        int closeCount = 0;
        for (int i = 0; i < expr.length(); i++) {
            if (expr.charAt(i) == '(') openCount++;
            if (expr.charAt(i) == ')') closeCount++;
        }
        StringBuilder exprBuilder = new StringBuilder(expr);
        while (openCount > closeCount) {
            exprBuilder.append(")");
            closeCount++;
        }
        return exprBuilder.toString();
    }

    /**
     * Pure Java implementation of mathematical expression evaluator utilizing Recursive Descent.
     * Compliant with pure Java 8 standard compiler criteria (no external libraries required).
     */
    private static double eval(final String str) {
        return new Object() {
            int pos = -1, ch;

            void nextChar() {
                ch = (++pos < str.length()) ? str.charAt(pos) : -1;
            }

            boolean eat(int charToEat) {
                while (ch == ' ') nextChar();
                if (ch == charToEat) {
                    nextChar();
                    return true;
                }
                return false;
            }

            double parse() {
                nextChar();
                double x = parseExpression();
                if (pos < str.length()) throw new RuntimeException("Unexpected: " + (char) ch);
                return x;
            }

            double parseExpression() {
                double x = parseTerm();
                for (; ; ) {
                    if (eat('+')) x += parseTerm(); // addition
                    else if (eat('-')) x -= parseTerm(); // subtraction
                    else return x;
                }
            }

            double parseTerm() {
                double x = parseFactor();
                for (; ; ) {
                    if (eat('*')) x *= parseFactor(); // multiplication
                    else if (eat('/')) {
                        double divisor = parseFactor();
                        if (divisor == 0) {
                            throw new ArithmeticException("Division by zero");
                        }
                        x /= divisor; // division
                    } else if (eat('%')) {
                        x %= parseFactor(); // modulo remainder
                    } else return x;
                }
            }

            double parseFactor() {
                if (eat('+')) return parseFactor(); // unary plus
                if (eat('-')) return -parseFactor(); // unary minus

                double x;
                int startPos = this.pos;
                if (eat('(')) { // parentheses logic
                    x = parseExpression();
                    eat(')');
                } else if ((ch >= '0' && ch <= '9') || ch == '.') { // numbers parse
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(str.substring(startPos, this.pos));
                } else {
                    throw new RuntimeException("Unexpected: " + (char) ch);
                }

                return x;
            }
        }.parse();
    }

    private void showHistoryPanel(boolean show) {
        if (show) {
            populateHistoryList();
            layoutHistoryPanel.setVisibility(View.VISIBLE);
            AlphaAnimation anim = new AlphaAnimation(0.0f, 1.0f);
            anim.setDuration(300);
            layoutHistoryPanel.startAnimation(anim);
        } else {
            AlphaAnimation anim = new AlphaAnimation(1.0f, 0.0f);
            anim.setDuration(250);
            layoutHistoryPanel.startAnimation(anim);
            layoutHistoryPanel.setVisibility(View.GONE);
        }
    }

    private void populateHistoryList() {
        layoutHistoryItems.removeAllViews();

        if (calculationHistory.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText(R.string.history_empty);
            emptyText.setTextColor(Color.parseColor("#9CA3AF"));
            emptyText.setGravity(Gravity.CENTER);
            emptyText.setPadding(0, 50, 0, 50);
            emptyText.setTextSize(16sp);
            layoutHistoryItems.addView(emptyText);
            return;
        }

        for (final String record : calculationHistory) {
            LinearLayout itemLayout = new LinearLayout(this);
            itemLayout.setOrientation(LinearLayout.VERTICAL);
            itemLayout.setBackgroundResource(R.drawable.history_item_bg);
            
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 0, 16);
            itemLayout.setLayoutParams(params);
            itemLayout.setPadding(20, 16, 20, 16);
            itemLayout.setClickable(true);
            itemLayout.setFocusable(true);

            TextView tvRecord = new TextView(this);
            tvRecord.setText(record);
            tvRecord.setTextColor(Color.parseColor("#1F2937"));
            tvRecord.setTextSize(16sp);
            tvRecord.setGravity(Gravity.RIGHT);
            itemLayout.addView(tvRecord);

            // Re-load calculation formula upon item click
            itemLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (record.contains("=")) {
                        String[] parts = record.split("=");
                        currentExpression = parts[0].trim();
                        tvFormula.setText(currentExpression);
                        tvResult.setText(parts[1].trim());
                        isResultDisplayed = true;
                        showHistoryPanel(false);
                    }
                }
            });

            layoutHistoryItems.addView(itemLayout);
        }
    }
}