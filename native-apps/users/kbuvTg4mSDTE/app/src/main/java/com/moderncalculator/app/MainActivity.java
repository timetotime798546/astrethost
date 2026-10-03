package com.moderncalculator.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.text.DecimalFormat;
import java.util.Locale;

public class MainActivity extends Activity {

    private TextView tvExpression;
    private TextView tvResult;
    private String currentExpression = "";
    private boolean isResultDisplayed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvExpression = (TextView) findViewById(R.id.tv_expression);
        tvResult = (TextView) findViewById(R.id.tv_result);

        // Bind standard number and operators using helper methods
        setupButton(R.id.btn_0, "0");
        setupButton(R.id.btn_1, "1");
        setupButton(R.id.btn_2, "2");
        setupButton(R.id.btn_3, "3");
        setupButton(R.id.btn_4, "4");
        setupButton(R.id.btn_5, "5");
        setupButton(R.id.btn_6, "6");
        setupButton(R.id.btn_7, "7");
        setupButton(R.id.btn_8, "8");
        setupButton(R.id.btn_9, "9");
        setupButton(R.id.btn_decimal, ".");

        setupButton(R.id.btn_add, " + ");
        setupButton(R.id.btn_subtract, " − ");
        setupButton(R.id.btn_multiply, " × ");
        setupButton(R.id.btn_divide, " ÷ ");

        // Bind special button events using traditional anonymous inner classes
        findViewById(R.id.btn_clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearExpression();
            }
        });

        findViewById(R.id.btn_negate).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleNegate();
            }
        });

        findViewById(R.id.btn_percent).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                appendPercent();
            }
        });

        findViewById(R.id.btn_backspace).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performBackspace();
            }
        });

        findViewById(R.id.btn_equals).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performCalculation();
            }
        });
    }

    private void setupButton(int id, final String value) {
        View btn = findViewById(id);
        if (btn != null) {
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    appendExpression(value);
                }
            });
        }
    }

    private void appendExpression(String val) {
        if (isResultDisplayed) {
            if (isOperator(val)) {
                currentExpression = tvResult.getText().toString() + val;
            } else {
                currentExpression = val;
            }
            isResultDisplayed = false;
        } else {
            if (val.equals(".")) {
                if (canAppendDecimal()) {
                    currentExpression += val;
                }
            } else if (isOperator(val)) {
                if (currentExpression.endsWith(" ")) {
                    int len = currentExpression.length();
                    if (len >= 3) {
                        currentExpression = currentExpression.substring(0, len - 3) + val;
                    }
                } else if (!currentExpression.isEmpty()) {
                    currentExpression += val;
                }
            } else {
                currentExpression += val;
            }
        }
        updateViews();
    }

    private boolean isOperator(String val) {
        return val.equals(" + ") || val.equals(" − ") || val.equals(" × ") || val.equals(" ÷ ");
    }

    private boolean canAppendDecimal() {
        if (currentExpression.isEmpty()) return true;
        String[] parts = currentExpression.split(" ");
        if (parts.length == 0) return true;
        String lastWord = parts[parts.length - 1];
        return !lastWord.contains(".");
    }

    private void clearExpression() {
        currentExpression = "";
        isResultDisplayed = false;
        updateViews();
    }

    private void appendPercent() {
        if (currentExpression.isEmpty()) return;
        char lastChar = currentExpression.charAt(currentExpression.length() - 1);
        if (Character.isDigit(lastChar) || lastChar == '%') {
            currentExpression += "%";
            updateViews();
        }
    }

    private void performBackspace() {
        if (currentExpression.isEmpty()) return;
        int len = currentExpression.length();
        if (currentExpression.endsWith(" ")) {
            if (len >= 3) {
                currentExpression = currentExpression.substring(0, len - 3);
            } else {
                currentExpression = "";
            }
        } else {
            currentExpression = currentExpression.substring(0, len - 1);
        }
        updateViews();
    }

    private void toggleNegate() {
        if (currentExpression.isEmpty()) return;

        int len = currentExpression.length();
        int i = len - 1;
        while (i >= 0 && currentExpression.charAt(i) == ' ') {
            i--;
        }
        if (i < 0) return;

        int end = i + 1;
        while (i >= 0 && (Character.isDigit(currentExpression.charAt(i)) || currentExpression.charAt(i) == '.' || currentExpression.charAt(i) == '%')) {
            i--;
        }

        if (i >= 0 && currentExpression.charAt(i) == '-') {
            boolean isUnary = false;
            if (i == 0) {
                isUnary = true;
            } else {
                char before = currentExpression.charAt(i - 1);
                if (before == ' ' || before == '+' || before == '*' || before == '/' || before == '(') {
                    isUnary = true;
                }
            }

            if (isUnary) {
                currentExpression = currentExpression.substring(0, i) + currentExpression.substring(i + 1, end);
                updateViews();
                return;
            }
        }

        currentExpression = currentExpression.substring(0, i + 1) + "-" + currentExpression.substring(i + 1, end);
        updateViews();
    }

    private void updateViews() {
        tvExpression.setText(currentExpression);

        if (currentExpression.isEmpty()) {
            tvResult.setText("0");
            return;
        }

        try {
            String evalTarget = currentExpression;
            if (evalTarget.endsWith(" ")) {
                int len = evalTarget.length();
                if (len >= 3) {
                    evalTarget = evalTarget.substring(0, len - 3);
                }
            }

            if (!evalTarget.isEmpty()) {
                double res = evaluate(evalTarget);
                tvResult.setText(formatResult(res));
            }
        } catch (Exception e) {
            // Keep current text unchanged on live preview parsing errors
        }
    }

    private void performCalculation() {
        if (currentExpression.isEmpty()) return;

        try {
            double res = evaluate(currentExpression);
            String finalResult = formatResult(res);

            tvExpression.setText(currentExpression + " =");
            tvResult.setText(finalResult);

            currentExpression = finalResult;
            isResultDisplayed = true;
        } catch (ArithmeticException e) {
            tvResult.setText("Error");
            currentExpression = "";
            isResultDisplayed = true;
        } catch (Exception e) {
            tvResult.setText("Error");
            currentExpression = "";
            isResultDisplayed = true;
        }
    }

    private String formatResult(double val) {
        if (Double.isInfinite(val) || Double.isNaN(val)) {
            return "Error";
        }

        if (val == (long) val) {
            return String.valueOf((long) val);
        }

        String s = String.format(Locale.US, "%.10f", val);
        if (s.contains(".")) {
            while (s.endsWith("0")) {
                s = s.substring(0, s.length() - 1);
            }
            if (s.endsWith(".")) {
                s = s.substring(0, s.length() - 1);
            }
        }

        if (Math.abs(val) >= 1e12 || (Math.abs(val) > 0 && Math.abs(val) < 1e-6)) {
            DecimalFormat df = new DecimalFormat("0.######E0");
            return df.format(val).replace("E", "e");
        }

        return s;
    }

    private double evaluate(String expression) throws Exception {
        final String cleanExpr = expression.replace("×", "*").replace("÷", "/").replace("−", "-").replaceAll("\\s+", "");

        return new Object() {
            int pos = -1, ch;

            void nextChar() {
                ch = (++pos < cleanExpr.length()) ? cleanExpr.charAt(pos) : -1;
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
                if (pos < cleanExpr.length()) throw new RuntimeException("Unexpected: " + (char)ch);
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
                        if (divisor == 0) throw new ArithmeticException("Divide by zero");
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
                if ((ch >= '0' && ch <= '9') || ch == '.') {
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(cleanExpr.substring(startPos, this.pos));
                } else {
                    throw new RuntimeException("Unexpected: " + (char)ch);
                }

                while (eat('%')) {
                    x = x / 100.0;
                }

                return x;
            }
        }.parse();
    }
}