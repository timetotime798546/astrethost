package com.moderncalculator.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import java.text.DecimalFormat;

public class MainActivity extends Activity {

    private TextView tvFormula;
    private TextView tvResult;

    private StringBuilder currentInput = new StringBuilder();
    private boolean isResultDisplayed = false;
    private DecimalFormat decimalFormat = new DecimalFormat("#.##########");

    @Override
    protected void onCreate(Bundle savedInstanceState) { 
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        tvFormula = (TextView) findViewById(R.id.tvFormula);
        tvResult = (TextView) findViewById(R.id.tvResult);

        setNumericClickListeners();
        setOperatorClickListeners();
        setSystemClickListeners();
    }

    private void setNumericClickListeners() {
        int[] numericIds = new int[]{
                R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3,
                R.id.btn_4, R.id.btn_5, R.id.btn_6, R.id.btn_7,
                R.id.btn_8, R.id.btn_9, R.id.btn_dot
        };

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button btn = (Button) v;
                String text = btn.getText().toString();
                
                if (isResultDisplayed) {
                    if (text.equals(".")) {
                        currentInput.setLength(0);
                        currentInput.append("0.");
                    } else {
                        currentInput.setLength(0);
                        currentInput.append(text);
                    }
                    isResultDisplayed = false;
                } else {
                    if (text.equals(".")) {
                        if (canAppendDot()) {
                            currentInput.append(text);
                        }
                    } else {
                        currentInput.append(text);
                    }
                }
                updateUI();
                evaluateCurrentExpressionSilently();
            }
        };

        for (int id : numericIds) {
            findViewById(id).setOnClickListener(listener);
        }
    }

    private void setOperatorClickListeners() {
        int[] opIds = new int[]{
                R.id.btn_add, R.id.btn_sub, R.id.btn_mul, R.id.btn_div
        };

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button btn = (Button) v;
                String op = btn.getText().toString();

                if (isResultDisplayed) {
                    String res = tvResult.getText().toString();
                    if (!res.equals("Error") && !res.equals("0")) {
                        currentInput.setLength(0);
                        currentInput.append(res);
                    } else {
                        currentInput.setLength(0);
                        currentInput.append("0");
                    }
                    isResultDisplayed = false;
                }

                if (currentInput.length() > 0) {
                    char lastChar = currentInput.charAt(currentInput.length() - 1);
                    if (isOperator(lastChar)) {
                        currentInput.setLength(currentInput.length() - 1);
                    } 
                    currentInput.append(op);
                } else if (op.equals("−")) {
                    currentInput.append("-");
                }
                updateUI();
            }
        };

        for (int id : opIds) {
            findViewById(id).setOnClickListener(listener);
        }
    }

    private void setSystemClickListeners() {
        findViewById(R.id.btn_clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentInput.setLength(0);
                tvFormula.setText("");
                tvResult.setText("0");
                isResultDisplayed = false;
            }
        });

        findViewById(R.id.btn_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isResultDisplayed) {
                    currentInput.setLength(0);
                    tvFormula.setText("");
                    tvResult.setText("0");
                    isResultDisplayed = false;
                    return;
                }
                if (currentInput.length() > 0) {
                    currentInput.setLength(currentInput.length() - 1);
                    updateUI();
                    evaluateCurrentExpressionSilently();
                } 
            }
        });

        findViewById(R.id.btn_brackets).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isResultDisplayed) {
                    currentInput.setLength(0);
                    isResultDisplayed = false;
                }
                
                int openCount = 0;
                int closeCount = 0;
                String str = currentInput.toString();
                for (int i = 0; i < str.length(); i++) {
                    if (str.charAt(i) == '(') openCount++;
                    if (str.charAt(i) == ')') closeCount++;
                }

                if (currentInput.length() == 0) {
                    currentInput.append("(");
                } else {
                    char last = currentInput.charAt(currentInput.length() - 1);
                    if (openCount > closeCount && last != '(' && !isOperator(last)) {
                        currentInput.append(")");
                    } else if (isOperator(last) || last == '(') {
                        currentInput.append("(");
                    } else {
                        currentInput.append("×(");
                    }
                }
                updateUI();
                evaluateCurrentExpressionSilently();
            }
        });

        findViewById(R.id.btn_percent).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentInput.length() > 0) {
                    char last = currentInput.charAt(currentInput.length() - 1);
                    if (!isOperator(last) && last != '(' && last != ')') {
                        currentInput.append("%");
                        updateUI();
                        evaluateCurrentExpressionSilently();
                    }
                }
            }
        });

        findViewById(R.id.btn_equal).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentInput.length() > 0) {
                    String mathExpr = formatExpressionForParsing(currentInput.toString());
                    try {
                        double output = eval(mathExpr);
                        tvFormula.setText(currentInput.toString());
                        tvResult.setText(decimalFormat.format(output));
                        isResultDisplayed = true;
                    } catch (Exception ex) {
                        tvResult.setText("Error");
                    }
                }
            }
        });
    }

    private boolean canAppendDot() {
        if (currentInput.length() == 0) {
            currentInput.append("0");
            return true;
        }
        for (int i = currentInput.length() - 1; i >= 0; i--) {
            char c = currentInput.charAt(i);
            if (c == '.') return false;
            if (isOperator(c) || c == '(' || c == ')') return true;
        }
        return true;
    }

    private boolean isOperator(char c) {
        return c == '+' || c == '−' || c == '-' || c == '×' || c == '÷';
    }

    private void updateUI() {
        tvFormula.setText(currentInput.toString());
    }

    private void evaluateCurrentExpressionSilently() {
        if (currentInput.length() == 0) {
            tvResult.setText("0");
            return;
        }
        String rawExpr = currentInput.toString();
        char lastChar = rawExpr.charAt(rawExpr.length() - 1);
        if (isOperator(lastChar)) {
            rawExpr = rawExpr.substring(0, rawExpr.length() - 1);
        }
        if (rawExpr.length() == 0) {
            tvResult.setText("0");
            return;
        }
        try {
            String formatted = formatExpressionForParsing(rawExpr);
            double result = eval(formatted);
            tvResult.setText(decimalFormat.format(result));
        } catch (Exception e) {
            // Suppress real-time preview exceptions gracefully
        }
    }

    private String formatExpressionForParsing(String expr) {
        expr = expr.replace("−", "-");
        expr = expr.replace("×", "*");
        expr = expr.replace("÷", "/");

        int open = 0, close = 0;
        for (int i = 0; i < expr.length(); i++) {
            if (expr.charAt(i) == '(') open++;
            if (expr.charAt(i) == ')') close++;
        }
        while (open > close) {
            expr += ")";
            open--;
        }

        if (expr.contains("%")) {
            StringBuilder sb = new StringBuilder();
            int len = expr.length();
            for (int i = 0; i < len; i++) {
                char c = expr.charAt(i);
                if (c == '%') {
                    sb.append("*0.01");
                } else {
                    sb.append(c);
                }
            }
            expr = sb.toString();
        }
        return expr;
    }

    public static double eval(final String str) {
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
                if (pos < str.length()) throw new RuntimeException("Unexpected trailing character: " + (char)ch);
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
                        if (divisor == 0) {
                            throw new ArithmeticException("Division by Zero Error");
                        }
                        x /= divisor;
                    }
                    else return x;
                }
            }

            double parseFactor() {
                if (eat('+')) return parseFactor();
                if (eat('-')) return -parseFactor();

                double x;
                int startPos = pos;
                if (eat('(')) {
                    x = parseExpression();
                    eat(')');
                } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(str.substring(startPos, pos));
                } else {
                    throw new RuntimeException("Unexpected formula character: " + (char)ch);
                }
                return x;
            }
        }.parse();
    }
}