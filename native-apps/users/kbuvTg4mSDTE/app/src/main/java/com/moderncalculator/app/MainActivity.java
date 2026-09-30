package com.moderncalculator.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.text.DecimalFormat;

public class MainActivity extends Activity {

    private TextView tvFormula;
    private TextView tvPreview;
    private StringBuilder currentExpression = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvFormula = (TextView) findViewById(R.id.tv_formula);
        tvPreview = (TextView) findViewById(R.id.tv_preview);

        // Numeric Key Bindings
        setupNumericButton(R.id.btn_0, "0");
        setupNumericButton(R.id.btn_1, "1");
        setupNumericButton(R.id.btn_2, "2");
        setupNumericButton(R.id.btn_3, "3");
        setupNumericButton(R.id.btn_4, "4");
        setupNumericButton(R.id.btn_5, "5");
        setupNumericButton(R.id.btn_6, "6");
        setupNumericButton(R.id.btn_7, "7");
        setupNumericButton(R.id.btn_8, "8");
        setupNumericButton(R.id.btn_9, "9");
        setupNumericButton(R.id.btn_dot, ".");

        // Symbol and Operator Bindings
        setupOperatorButton(R.id.btn_add, "+");
        setupOperatorButton(R.id.btn_sub, "−");
        setupOperatorButton(R.id.btn_mul, "×");
        setupOperatorButton(R.id.btn_div, "÷");
        setupOperatorButton(R.id.btn_percent, "%");
        setupOperatorButton(R.id.btn_paren_open, "(");
        setupOperatorButton(R.id.btn_paren_close, ")");

        // Clear and Delete Handlers
        Button btnClear = (Button) findViewById(R.id.btn_clear);
        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentExpression.setLength(0);
                updateDisplay();
            }
        });

        Button btnDelete = (Button) findViewById(R.id.btn_delete);
        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentExpression.length() > 0) {
                    currentExpression.deleteCharAt(currentExpression.length() - 1);
                    updateDisplay();
                }
            }
        });

        // Evaluation Equal Trigger
        Button btnEqual = (Button) findViewById(R.id.btn_equal);
        btnEqual.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String expressionString = currentExpression.toString();
                if (expressionString.isEmpty()) {
                    return;
                }
                String standardExp = sanitizeExpression(expressionString);
                try {
                    double result = eval(standardExp);
                    String formatted = formatResult(result);
                    currentExpression.setLength(0);
                    currentExpression.append(formatted);
                    tvFormula.setText(expressionString);
                    tvPreview.setText(formatted);
                } catch (Exception e) {
                    tvPreview.setText("Error");
                }
            }
        });
    }

    private void setupNumericButton(int resId, final String value) {
        Button btn = (Button) findViewById(resId);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentExpression.append(value);
                updateDisplay();
            }
        });
    }

    private void setupOperatorButton(int resId, final String value) {
        Button btn = (Button) findViewById(resId);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Prevent consecutive operators of multiplication, division, or percent without bounds
                if (isOperatorSymbol(value) && currentExpression.length() > 0) {
                    char lastChar = currentExpression.charAt(currentExpression.length() - 1);
                    if (isOperatorChar(lastChar)) {
                        currentExpression.deleteCharAt(currentExpression.length() - 1);
                    }
                }
                currentExpression.append(value);
                updateDisplay();
            }
        });
    }

    private boolean isOperatorSymbol(String s) {
        return s.equals("+") || s.equals("−") || s.equals("×") || s.equals("÷") || s.equals("%");
    }

    private boolean isOperatorChar(char c) {
        return c == '+' || c == '−' || c == '×' || c == '÷' || c == '%';
    }

    private void updateDisplay() {
        String currentStr = currentExpression.toString();
        tvFormula.setText(currentStr);

        if (currentStr.isEmpty()) {
            tvPreview.setText("0");
            return;
        }

        try {
            String cleanExpr = sanitizeExpression(currentStr);
            double previewVal = eval(cleanExpr);
            tvPreview.setText(formatResult(previewVal));
        } catch (Exception e) {
            // Keep previous display or show minimal progress during incomplete formula inputs
            tvPreview.setText("");
        }
    }

    private String sanitizeExpression(String input) {
        // Translate visual elements to standard math engine tokens
        String temp = input.replace("−", "-")
                           .replace("×", "*")
                           .replace("÷", "/");
        
        // Custom transformation to support inline percentage operations e.g. 50% as 50 * 0.01
        // Replace dynamic patterns of number% with (number*0.01)
        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < temp.length()) {
            char c = temp.charAt(i);
            if (c == '%') {
                // Find start bounds of the percentage base
                int start = result.length() - 1;
                if (start >= 0) {
                    char prev = result.charAt(start);
                    if (Character.isDigit(prev) || prev == '.') {
                        while (start > 0 && (Character.isDigit(result.charAt(start - 1)) || result.charAt(start - 1) == '.')) {
                            start--;
                        }
                        String numToPercent = result.substring(start);
                        result.setLength(start);
                        result.append("(").append(numToPercent).append("*0.01)");
                    } else if (prev == ')') {
                        // Find matching open bracket index backwards
                        int bracketCount = 1;
                        while (start > 0 && bracketCount > 0) {
                            start--;
                            if (result.charAt(start) == ')') bracketCount++;
                            if (result.charAt(start) == '(') bracketCount--;
                        }
                        String bracketBlock = result.substring(start);
                        result.setLength(start);
                        result.append("(").append(bracketBlock).append("*0.01)");
                    } else {
                        result.append("*0.01");
                    }
                } else {
                    result.append("0.01");
                }
            } else {
                result.append(c);
            }
            i++;
        }
        return result.toString();
    }

    private String formatResult(double val) {
        if (Double.isInfinite(val)) {
            return "Infinity";
        }
        if (Double.isNaN(val)) {
            return "NaN";
        }
        // Round to 10 decimal places to resolve floating point issues cleanly
        DecimalFormat format = new DecimalFormat("0.##########");
        return format.format(val);
    }

    // Mathematical parser leveraging traditional recursive descent execution
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
                if (pos < str.length()) throw new RuntimeException("Unexpected: " + (char)ch);
                return x;
            }

            // Grammar:
            // Expression = Term | Expression `+` Term | Expression `-` Term
            // Term = Factor | Term `*` Factor | Term `/` Factor
            // Factor = `+` Factor | `-` Factor | `(` Expression `)` | Number

            double parseExpression() {
                double x = parseTerm();
                for (;;) {
                    if      (eat('+')) x += parseTerm(); // addition
                    else if (eat('-')) x -= parseTerm(); // subtraction
                    else return x;
                }
            }

            double parseTerm() {
                double x = parseFactor();
                for (;;) {
                    if      (eat('*')) x *= parseFactor(); // multiplication
                    else if (eat('/')) {
                        double divisor = parseFactor();
                        if (divisor == 0) {
                            throw new ArithmeticException("Division by zero");
                        }
                        x /= divisor; // division
                    }
                    else return x;
                }
            }

            double parseFactor() {
                if (eat('+')) return parseFactor(); // unary plus
                if (eat('-')) return -parseFactor(); // unary minus

                double x;
                int startPos = this.pos;
                if (eat('(')) { // parentheses
                    x = parseExpression();
                    eat(')');
                } else if ((ch >= '0' && ch <= '9') || ch == '.') { // numbers
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(str.substring(startPos, this.pos));
                } else {
                    throw new RuntimeException("Unexpected expression structure");
                }

                return x;
            }
        }.parse();
    }
}