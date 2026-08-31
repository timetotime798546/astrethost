package com.moderncalculator.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import java.text.DecimalFormat;

public class MainActivity extends Activity implements View.OnClickListener {

    private TextView tvHistory;
    private TextView tvExpression;
    private TextView tvResultPreview;
    
    private String expression = "";
    private boolean isResultDisplayed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind Display Views
        tvHistory = (TextView) findViewById(R.id.tv_history);
        tvExpression = (TextView) findViewById(R.id.tv_expression);
        tvResultPreview = (TextView) findViewById(R.id.tv_result_preview);

        // Bind Numeric and Operation triggers
        findViewById(R.id.btn_clear).setOnClickListener(this);
        findViewById(R.id.btn_paren).setOnClickListener(this);
        findViewById(R.id.btn_percent).setOnClickListener(this);
        findViewById(R.id.btn_divide).setOnClickListener(this);

        findViewById(R.id.btn_7).setOnClickListener(this);
        findViewById(R.id.btn_8).setOnClickListener(this);
        findViewById(R.id.btn_9).setOnClickListener(this);
        findViewById(R.id.btn_multiply).setOnClickListener(this);

        findViewById(R.id.btn_4).setOnClickListener(this);
        findViewById(R.id.btn_5).setOnClickListener(this);
        findViewById(R.id.btn_6).setOnClickListener(this);
        findViewById(R.id.btn_subtract).setOnClickListener(this);

        findViewById(R.id.btn_1).setOnClickListener(this);
        findViewById(R.id.btn_2).setOnClickListener(this);
        findViewById(R.id.btn_3).setOnClickListener(this);
        findViewById(R.id.btn_add).setOnClickListener(this);

        findViewById(R.id.btn_sign).setOnClickListener(this);
        findViewById(R.id.btn_0).setOnClickListener(this);
        findViewById(R.id.btn_decimal).setOnClickListener(this);
        findViewById(R.id.btn_equal).setOnClickListener(this);

        findViewById(R.id.btn_backspace).setOnClickListener(this);
        
        updateDisplay();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.btn_clear) {
            clearAll();
        } else if (id == R.id.btn_backspace) {
            handleBackspace();
        } else if (id == R.id.btn_paren) {
            appendParenthesis();
        } else if (id == R.id.btn_percent) {
            appendOperator("%");
        } else if (id == R.id.btn_divide) {
            appendOperator("÷");
        } else if (id == R.id.btn_multiply) {
            appendOperator("×");
        } else if (id == R.id.btn_subtract) {
            appendOperator("-");
        } else if (id == R.id.btn_add) {
            appendOperator("+");
        } else if (id == R.id.btn_sign) {
            toggleSign();
        } else if (id == R.id.btn_decimal) {
            appendDecimal();
        } else if (id == R.id.btn_equal) {
            calculateFinalResult();
        } else {
            if (isResultDisplayed) {
                expression = "";
                isResultDisplayed = false;
            }
            if (id == R.id.btn_0) {
                expression += "0";
            } else if (id == R.id.btn_1) {
                expression += "1";
            } else if (id == R.id.btn_2) {
                expression += "2";
            } else if (id == R.id.btn_3) {
                expression += "3";
            } else if (id == R.id.btn_4) {
                expression += "4";
            } else if (id == R.id.btn_5) {
                expression += "5";
            } else if (id == R.id.btn_6) {
                expression += "6";
            } else if (id == R.id.btn_7) {
                expression += "7";
            } else if (id == R.id.btn_8) {
                expression += "8";
            } else if (id == R.id.btn_9) {
                expression += "9";
            }
            updateDisplay();
        }
    }

    private void clearAll() {
        expression = "";
        tvHistory.setText("");
        isResultDisplayed = false;
        updateDisplay();
    }

    private void handleBackspace() {
        if (isResultDisplayed) {
            expression = "";
            isResultDisplayed = false;
        } else if (expression.length() > 0) {
            expression = expression.substring(0, expression.length() - 1);
        }
        updateDisplay();
    }

    private void appendParenthesis() {
        if (isResultDisplayed) {
            expression = "";
            isResultDisplayed = false;
        }
        int openCount = 0;
        int closeCount = 0;
        for (int i = 0; i < expression.length(); i++) {
            if (expression.charAt(i) == '(') openCount++;
            else if (expression.charAt(i) == ')') closeCount++;
        }
        if (openCount > closeCount) {
            char lastChar = expression.length() > 0 ? expression.charAt(expression.length() - 1) : '\0';
            if (Character.isDigit(lastChar) || lastChar == ')' || lastChar == '%') {
                expression += ")";
            } else {
                expression += "(";
            }
        } else {
            expression += "(";
        }
        updateDisplay();
    }

    private void appendOperator(String op) {
        if (isResultDisplayed) {
            isResultDisplayed = false;
        }
        if (expression.length() > 0) {
            char lastChar = expression.charAt(expression.length() - 1);
            if (isOperator(lastChar)) {
                expression = expression.substring(0, expression.length() - 1) + op;
            } else {
                expression += op;
            }
        } else if (op.equals("-")) {
            expression += op;
        }
        updateDisplay();
    }

    private boolean isOperator(char ch) {
        return ch == '+' || ch == '-' || ch == '×' || ch == '÷' || ch == '%';
    }

    private void toggleSign() {
        if (expression.length() == 0) {
            expression = "-";
        } else if (expression.startsWith("-") && !expression.contains("+") && !expression.contains("×") && !expression.contains("÷") && !expression.contains("(")) {
            expression = expression.substring(1);
        } else {
            if (expression.startsWith("-(") && expression.endsWith(")")) {
                expression = expression.substring(2, expression.length() - 1);
            } else {
                expression = "-(" + expression + ")";
            }
        }
        updateDisplay();
    }

    private void appendDecimal() {
        if (isResultDisplayed) {
            expression = "0";
            isResultDisplayed = false;
        }
        if (expression.length() == 0) {
            expression = "0.";
        } else {
            int lastOpIndex = -1;
            for (int i = expression.length() - 1; i >= 0; i--) {
                char c = expression.charAt(i);
                if (isOperator(c) || c == '(' || c == ')') {
                    lastOpIndex = i;
                    break;
                }
            }
            String lastNumber = expression.substring(lastOpIndex + 1);
            if (!lastNumber.contains(".")) {
                if (lastNumber.length() == 0) {
                    expression += "0.";
                } else {
                    expression += ".";
                }
            }
        }
        updateDisplay();
    }

    private void updateDisplay() {
        if (expression.length() == 0) {
            tvExpression.setText("0");
            tvResultPreview.setText("");
        } else {
            tvExpression.setText(expression);
            try {
                String processed = preprocessExpression(expression);
                double val = ExpressionEvaluator.evaluate(processed);
                tvResultPreview.setText(formatResult(val));
            } catch (Exception e) {
                tvResultPreview.setText("");
            }
        }
    }

    private void calculateFinalResult() {
        if (expression.length() == 0) return;
        try {
            String originalExpr = expression;
            String processed = preprocessExpression(expression);
            double val = ExpressionEvaluator.evaluate(processed);
            String resultStr = formatResult(val);
            
            tvHistory.setText(originalExpr + " =");
            expression = resultStr;
            tvExpression.setText(resultStr);
            tvResultPreview.setText("");
            isResultDisplayed = true;
        } catch (Exception e) {
            tvResultPreview.setText("Error");
        }
    }

    private String preprocessExpression(String expr) {
        String cleanExpr = expr.replace("×", "*").replace("÷", "/");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cleanExpr.length(); i++) {
            char current = cleanExpr.charAt(i);
            if (i > 0) {
                char prev = cleanExpr.charAt(i - 1);
                if (current == '(' && (Character.isDigit(prev) || prev == ')' || prev == '%')) {
                    sb.append('*');
                }
                if (Character.isDigit(current) && prev == ')') {
                    sb.append('*');
                }
            }
            sb.append(current);
        }
        return sb.toString();
    }

    private String formatResult(double val) {
        if (Double.isInfinite(val) || Double.isNaN(val)) {
            return "Error";
        }
        if (val == (long) val) {
            return String.format("%d", (long) val);
        } else {
            DecimalFormat df = new DecimalFormat("#.##########");
            return df.format(val);
        }
    }

    private static class ExpressionEvaluator {
        public static double evaluate(final String str) {
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
                    if (pos < str.length()) throw new RuntimeException("Unexpected character: " + (char)ch);
                    return x;
                }

                double parseExpression() {
                    double x = parseTerm();
                    for (;;) {
                        if      (eat('+')) x += parseTerm();
                        else if (eat('-')) x -= parseTerm();
                        else return x;
                    }
                }

                double parseTerm() {
                    double x = parseFactor();
                    for (;;) {
                        if      (eat('*')) x *= parseFactor();
                        else if (eat('/')) x /= parseFactor();
                        else return x;
                    }
                }

                double parseFactor() {
                    if (eat('+')) return parseFactor();
                    if (eat('-')) return -parseFactor();

                    double x;
                    int startPos = this.pos;
                    if (eat('(')) {
                        x = parseExpression();
                        eat(')');
                    } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                        while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                        x = Double.parseDouble(str.substring(startPos, this.pos));
                    } else {
                        throw new RuntimeException("Unexpected character: " + (char)ch);
                    }

                    if (eat('%')) {
                        x = x / 100.0;
                    }

                    return x;
                }
            }.parse();
        }
    }
}