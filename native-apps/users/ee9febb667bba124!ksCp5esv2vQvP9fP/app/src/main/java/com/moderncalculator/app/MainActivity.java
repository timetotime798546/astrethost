package com.moderncalculator.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import java.util.Locale;

public class MainActivity extends Activity {

    private TextView tvHistory;
    private TextView tvInput;
    private boolean isResultDisplayed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set window to full screen without Title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        tvHistory = (TextView) findViewById(R.id.tvHistory);
        tvInput = (TextView) findViewById(R.id.tvInput);

        setupButtons();
    }

    private void setupButtons() {
        int[] buttonIds = new int[]{
                R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4,
                R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9,
                R.id.btn_dot, R.id.btn_add, R.id.btn_subtract, R.id.btn_multiply,
                R.id.btn_divide, R.id.btn_power, R.id.btn_percent, R.id.btn_sin,
                R.id.btn_cos, R.id.btn_tan, R.id.btn_log, R.id.btn_ln,
                R.id.btn_sqrt, R.id.btn_pi, R.id.btn_e, R.id.btn_open_paren,
                R.id.btn_close_paren, R.id.btn_ac, R.id.btn_back, R.id.btn_equals
        };

        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int id = v.getId();
                handleButtonClick(id);
            }
        };

        for (int i = 0; i < buttonIds.length; i++) {
            findViewById(buttonIds[i]).setOnClickListener(clickListener);
        }
    }

    private void handleButtonClick(int id) {
        if (isResultDisplayed) {
            // If a result is currently on display, typing standard values resets the field
            if (id != R.id.btn_add && id != R.id.btn_subtract && id != R.id.btn_multiply 
                    && id != R.id.btn_divide && id != R.id.btn_power && id != R.id.btn_percent 
                    && id != R.id.btn_back && id != R.id.btn_equals) {
                tvInput.setText("");
            }
            isResultDisplayed = false;
        }

        if (id == R.id.btn_0) appendText("0");
        else if (id == R.id.btn_1) appendText("1");
        else if (id == R.id.btn_2) appendText("2");
        else if (id == R.id.btn_3) appendText("3");
        else if (id == R.id.btn_4) appendText("4");
        else if (id == R.id.btn_5) appendText("5");
        else if (id == R.id.btn_6) appendText("6");
        else if (id == R.id.btn_7) appendText("7");
        else if (id == R.id.btn_8) appendText("8");
        else if (id == R.id.btn_9) appendText("9");
        else if (id == R.id.btn_dot) appendText(".");
        else if (id == R.id.btn_pi) appendText("π");
        else if (id == R.id.btn_e) appendText("e");
        else if (id == R.id.btn_open_paren) appendText("(");
        else if (id == R.id.btn_close_paren) appendText(")");
        else if (id == R.id.btn_sin) appendText("sin(");
        else if (id == R.id.btn_cos) appendText("cos(");
        else if (id == R.id.btn_tan) appendText("tan(");
        else if (id == R.id.btn_log) appendText("log(");
        else if (id == R.id.btn_ln) appendText("ln(");
        else if (id == R.id.btn_sqrt) appendText("√(");
        else if (id == R.id.btn_add) appendOperator("+");
        else if (id == R.id.btn_subtract) appendOperator("-");
        else if (id == R.id.btn_multiply) appendOperator("×");
        else if (id == R.id.btn_divide) appendOperator("÷");
        else if (id == R.id.btn_power) appendOperator("^");
        else if (id == R.id.btn_percent) appendPercent();
        else if (id == R.id.btn_ac) {
            tvInput.setText("");
            tvHistory.setText("");
        } else if (id == R.id.btn_back) {
            performBackspace();
        } else if (id == R.id.btn_equals) {
            calculateResult();
        }
    }

    private void appendText(String str) {
        tvInput.setText(tvInput.getText().toString() + str);
    }

    private void appendOperator(String op) {
        String current = tvInput.getText().toString();
        if (current.length() > 0) {
            char last = current.charAt(current.length() - 1);
            if (last == '+' || last == '-' || last == '×' || last == '÷' || last == '^') {
                tvInput.setText(current.substring(0, current.length() - 1) + op);
            } else {
                tvInput.setText(current + op);
            }
        } else {
            if (op.equals("-")) {
                tvInput.setText("-");
            }
        }
    }

    private void appendPercent() {
        String current = tvInput.getText().toString();
        if (current.length() > 0) {
            char last = current.charAt(current.length() - 1);
            if (Character.isDigit(last) || last == ')' || last == 'e' || last == 'π') {
                tvInput.setText(current + "%");
            }
        }
    }

    private void performBackspace() {
        String str = tvInput.getText().toString();
        if (str.length() > 0) {
            if (str.endsWith("sin(") || str.endsWith("cos(") || str.endsWith("tan(") || str.endsWith("log(")) {
                str = str.substring(0, str.length() - 4);
            } else if (str.endsWith("ln(")) {
                str = str.substring(0, str.length() - 3);
            } else if (str.endsWith("√(")) {
                str = str.substring(0, str.length() - 2);
            } else {
                str = str.substring(0, str.length() - 1);
            }
            tvInput.setText(str);
        }
    }

    private void calculateResult() {
        String input = tvInput.getText().toString();
        if (input.trim().isEmpty()) return;

        String processed = autoCloseParentheses(input);
        tvHistory.setText(input + " =");

        // Replace display symbols with computer parsable equations
        processed = processed.replace("×", "*").replace("÷", "/");

        try {
            double result = eval(processed);
            tvInput.setText(formatResult(result));
            isResultDisplayed = true;
        } catch (Exception e) {
            tvInput.setText("Error");
            isResultDisplayed = true;
        }
    }

    private String autoCloseParentheses(String expr) {
        int open = 0;
        int close = 0;
        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);
            if (c == '(') open++;
            else if (c == ')') close++;
        }
        StringBuilder sb = new StringBuilder(expr);
        while (open > close) {
            sb.append(')');
            close++;
        }
        return sb.toString();
    }

    private String formatResult(double val) {
        if (Double.isNaN(val) || Double.isInfinite(val)) {
            return "Error";
        }
        if (val == (long) val) {
            return String.format(Locale.US, "%d", (long) val);
        } else {
            String s = String.format(Locale.US, "%.10f", val);
            while (s.endsWith("0")) {
                s = s.substring(0, s.length() - 1);
            }
            if (s.endsWith(".")) {
                s = s.substring(0, s.length() - 1);
            }
            return s;
        }
    }

    // Standard Java 8 Recursive Descent Parser
    private double eval(final String str) {
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
                    if (eat('+')) x += parseTerm();
                    else if (eat('-')) x -= parseTerm();
                    else return x;
                }
            }

            double parseTerm() {
                double x = parseFactor();
                for (;;) {
                    if (eat('*')) x *= parseFactor();
                    else if (eat('/')) {
                        double divisor = parseFactor();
                        if (divisor == 0) throw new ArithmeticException("Division by zero");
                        x /= divisor;
                    } else return x;
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
                } else if ((ch >= 'a' && ch <= 'z') || ch == '√' || ch == 'π') {
                    if (eat('π')) {
                        x = Math.PI;
                    } else if (eat('e')) {
                        x = Math.E;
                    } else if (eat('√')) {
                        x = parseFactor();
                        x = Math.sqrt(x);
                    } else {
                        while (ch >= 'a' && ch <= 'z') nextChar();
                        String func = str.substring(startPos, this.pos);
                        x = parseFactor();
                        if (func.equals("sin")) x = Math.sin(Math.toRadians(x));
                        else if (func.equals("cos")) x = Math.cos(Math.toRadians(x));
                        else if (func.equals("tan")) x = Math.tan(Math.toRadians(x));
                        else if (func.equals("log")) x = Math.log10(x);
                        else if (func.equals("ln")) x = Math.log(x);
                        else throw new RuntimeException("Unknown function: " + func);
                    }
                } else {
                    throw new RuntimeException("Unexpected character");
                }

                if (eat('^')) x = Math.pow(x, parseFactor());
                if (eat('%')) x = x / 100.0;

                return x;
            }
        }.parse();
    }
}