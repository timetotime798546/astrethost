package com.moderncalculator.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity implements View.OnClickListener {

    private TextView tvHistory;
    private TextView tvExpression;
    private TextView tvResult;

    private String expression = "";
    private String history = "";
    private boolean isResultDisplayed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvHistory = (TextView) findViewById(R.id.tvHistory);
        tvExpression = (TextView) findViewById(R.id.tvExpression);
        tvResult = (TextView) findViewById(R.id.tvResult);

        // Standard UI components mappings
        int[] buttonIds = new int[]{
                R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
                R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9,
                R.id.btnDot, R.id.btnPlus, R.id.btnMinus, R.id.btnMultiply,
                R.id.btnDivide, R.id.btnPercent, R.id.btnPower, R.id.btnSqrt,
                R.id.btnSin, R.id.btnCos, R.id.btnTan, R.id.btnLn, R.id.btnPi,
                R.id.btnOpenBrac, R.id.btnCloseBrac, R.id.btnClear,
                R.id.btnBackspace, R.id.btnEqual
        };

        for (int i = 0; i < buttonIds.length; i++) {
            int id = buttonIds[i];
            View view = findViewById(id);
            if (view != null) {
                view.setOnClickListener(this);
            }
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.btnClear) {
            expression = "";
            history = "";
            isResultDisplayed = false;
            updateDisplay();
            tvResult.setText("0");
        } else if (id == R.id.btnBackspace) {
            if (isResultDisplayed) {
                isResultDisplayed = false;
            }
            if (expression.length() > 0) {
                // Handle deletion of visual functional parameters safely
                if (expression.endsWith("sin(") || expression.endsWith("cos(") || expression.endsWith("tan(")) {
                    expression = expression.substring(0, expression.length() - 4);
                } else if (expression.endsWith("ln(")) {
                    expression = expression.substring(0, expression.length() - 3);
                } else if (expression.endsWith("sqrt(")) {
                    expression = expression.substring(0, expression.length() - 5);
                } else {
                    expression = expression.substring(0, expression.length() - 1);
                }
            }
            updateDisplay();
            evaluateRealtime();
        } else if (id == R.id.btnEqual) {
            if (expression.trim().length() > 0) {
                calculateFinalResult();
            }
        } else {
            if (isResultDisplayed) {
                // Continue working on dynamic expressions if user selects operand
                if (id == R.id.btnPlus || id == R.id.btnMinus || id == R.id.btnMultiply ||
                    id == R.id.btnDivide || id == R.id.btnPower || id == R.id.btnPercent) {
                    expression = tvResult.getText().toString();
                } else {
                    expression = "";
                }
                isResultDisplayed = false;
            }

            Button btn = (Button) v;
            String btnText = btn.getText().toString();

            if (btnText.equals("sin") || btnText.equals("cos") || btnText.equals("tan")) {
                expression += btnText + "(";
            } else if (btnText.equals("ln")) {
                expression += "ln(";
            } else if (btnText.equals("√")) {
                expression += "sqrt(";
            } else {
                expression += btnText;
            }

            updateDisplay();
            evaluateRealtime();
        }
    }

    private void updateDisplay() {
        tvExpression.setText(expression);
        tvHistory.setText(history);
    }

    private void evaluateRealtime() {
        if (expression.trim().length() == 0) {
            tvResult.setText("0");
            return;
        }
        try {
            String formattedExpr = prepareExpression(expression);
            double val = parseExpressionString(formattedExpr);
            if (!Double.isNaN(val) && !Double.isInfinite(val)) {
                tvResult.setText(formatValue(val));
            }
        } catch (Exception e) {
            // Realtime parsing silences errors dynamically to keep workspace clean
        }
    }

    private void calculateFinalResult() {
        try {
            String formattedExpr = prepareExpression(expression);
            double val = parseExpressionString(formattedExpr);
            if (Double.isNaN(val)) {
                tvResult.setText("Error");
            } else if (Double.isInfinite(val)) {
                tvResult.setText("Infinity");
            } else {
                String resultStr = formatValue(val);
                history = expression + " =";
                expression = resultStr;
                tvResult.setText(resultStr);
                isResultDisplayed = true;
                updateDisplay();
            }
        } catch (ArithmeticException ae) {
            tvResult.setText("Can't divide by 0");
        } catch (Exception e) {
            tvResult.setText("Format Error");
        }
    }

    private String prepareExpression(String expr) {
        String cleaned = expr;
        cleaned = cleaned.replace("×", "*");
        cleaned = cleaned.replace("÷", "/");
        cleaned = cleaned.replace("π", "3.141592653589793");
        cleaned = cleaned.replace("%", "/100");
        return cleaned;
    }

    private String formatValue(double val) {
        if (val == (long) val) {
            return String.format("%d", (long) val);
        } else {
            String str = String.format("%.8f", val);
            while (str.endsWith("0")) {
                str = str.substring(0, str.length() - 1);
            }
            if (str.endsWith(".")) {
                str = str.substring(0, str.length() - 1);
            }
            return str;
        }
    }

    // Recursive descent parser implementation
    private double parseExpressionString(final String str) {
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
                if (pos < str.length()) throw new RuntimeException("Unexpected character");
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
                    else if (eat('/')) {
                        double divisor = parseFactor();
                        if (divisor == 0) throw new ArithmeticException("Division by zero");
                        x /= divisor;
                    }
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
                } else if (ch >= 'a' && ch <= 'z') {
                    while (ch >= 'a' && ch <= 'z') nextChar();
                    String func = str.substring(startPos, this.pos);
                    x = parseFactor();
                    if (func.equals("sin")) {
                        x = Math.sin(Math.toRadians(x));
                    } else if (func.equals("cos")) {
                        x = Math.cos(Math.toRadians(x));
                    } else if (func.equals("tan")) {
                        x = Math.tan(Math.toRadians(x));
                    } else if (func.equals("ln")) {
                        x = Math.log(x);
                    } else if (func.equals("sqrt")) {
                        x = Math.sqrt(x);
                    } else {
                        throw new RuntimeException("Unknown function: " + func);
                    }
                } else {
                    throw new RuntimeException("Unexpected evaluation");
                }

                if (eat('^')) x = Math.pow(x, parseFactor());

                return x;
            }
        }.parse();
    }
}