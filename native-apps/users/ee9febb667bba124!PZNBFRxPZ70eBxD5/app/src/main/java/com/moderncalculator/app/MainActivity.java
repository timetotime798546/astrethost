package com.moderncalculator.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.text.DecimalFormat;

public class MainActivity extends Activity {

    private TextView tvExpression;
    private TextView tvResult;
    private boolean isResultShown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getActionBar() != null) {
            getActionBar().hide();
        }

        tvExpression = (TextView) findViewById(R.id.tvExpression);
        tvResult = (TextView) findViewById(R.id.tvResult);

        Button btnClear = (Button) findViewById(R.id.btnClear);
        Button btnBracket = (Button) findViewById(R.id.btnBracket);
        Button btnPercent = (Button) findViewById(R.id.btnPercent);
        Button btnDivide = (Button) findViewById(R.id.btnDivide);
        Button btn7 = (Button) findViewById(R.id.btn7);
        Button btn8 = (Button) findViewById(R.id.btn8);
        Button btn9 = (Button) findViewById(R.id.btn9);
        Button btnMultiply = (Button) findViewById(R.id.btnMultiply);
        Button btn4 = (Button) findViewById(R.id.btn4);
        Button btn5 = (Button) findViewById(R.id.btn5);
        Button btn6 = (Button) findViewById(R.id.btn6);
        Button btnSubtract = (Button) findViewById(R.id.btnSubtract);
        Button btn1 = (Button) findViewById(R.id.btn1);
        Button btn2 = (Button) findViewById(R.id.btn2);
        Button btn3 = (Button) findViewById(R.id.btn3);
        Button btnAdd = (Button) findViewById(R.id.btnAdd);
        Button btnDelete = (Button) findViewById(R.id.btnDelete);
        Button btn0 = (Button) findViewById(R.id.btn0);
        Button btnDot = (Button) findViewById(R.id.btnDot);
        Button btnEqual = (Button) findViewById(R.id.btnEqual);

        View.OnClickListener numListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button b = (Button) v;
                appendToken(b.getText().toString(), false);
            }
        };

        btn0.setOnClickListener(numListener);
        btn1.setOnClickListener(numListener);
        btn2.setOnClickListener(numListener);
        btn3.setOnClickListener(numListener);
        btn4.setOnClickListener(numListener);
        btn5.setOnClickListener(numListener);
        btn6.setOnClickListener(numListener);
        btn7.setOnClickListener(numListener);
        btn8.setOnClickListener(numListener);
        btn9.setOnClickListener(numListener);

        View.OnClickListener opListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button b = (Button) v;
                appendToken(b.getText().toString(), true);
            }
        };

        btnAdd.setOnClickListener(opListener);
        btnSubtract.setOnClickListener(opListener);
        btnMultiply.setOnClickListener(opListener);
        btnDivide.setOnClickListener(opListener);

        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tvExpression.setText("");
                tvResult.setText("0");
                isResultShown = false;
            }
        });

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleDelete();
            }
        });

        btnBracket.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleBracket();
            }
        });

        btnPercent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                appendToken("%", true);
            }
        });

        btnDot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                appendDot();
            }
        });

        btnEqual.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calculateFinalResult();
            }
        });
    }

    private void appendToken(String token, boolean isOperator) {
        if (isResultShown) {
            if (isOperator) {
                isResultShown = false;
            } else {
                tvExpression.setText("");
                isResultShown = false;
            }
        }
        tvExpression.append(token);
        updateAutoResult();
    }

    private void handleDelete() {
        String exp = tvExpression.getText().toString();
        if (exp.length() > 0) {
            tvExpression.setText(exp.substring(0, exp.length() - 1));
            isResultShown = false;
            updateAutoResult();
        }
    }

    private void handleBracket() {
        String exp = tvExpression.getText().toString();
        int openCount = 0;
        int closeCount = 0;
        for (int i = 0; i < exp.length(); i++) {
            if (exp.charAt(i) == '(') openCount++;
            else if (exp.charAt(i) == ')') closeCount++;
        }
        
        if (exp.length() == 0) {
            appendToken("(", false);
            return;
        }
        
        char last = exp.charAt(exp.length() - 1);
        if (openCount > closeCount) {
            if (Character.isDigit(last) || last == ')') {
                appendToken(")", false);
            } else {
                appendToken("(", false);
            }
        } else {
            if (Character.isDigit(last) || last == ')') {
                appendToken("×(", false);
            } else {
                appendToken("(", false);
            }
        }
    }

    private void appendDot() {
        String exp = tvExpression.getText().toString();
        if (exp.isEmpty()) {
            appendToken("0.", false);
            return;
        }
        
        int lastOpIdx = -1;
        for (int i = exp.length() - 1; i >= 0; i--) {
            char c = exp.charAt(i);
            if (c == '+' || c == '-' || c == '×' || c == '÷' || c == '(' || c == ')') {
                lastOpIdx = i;
                break;
            }
        }
        
        String lastNumber = exp.substring(lastOpIdx + 1);
        if (!lastNumber.contains(".")) {
            appendToken(".", false);
        }
    }

    private void updateAutoResult() {
        String exp = tvExpression.getText().toString();
        if (exp.trim().isEmpty()) {
            tvResult.setText("0");
            return;
        }
        
        String cleanExp = exp;
        while (cleanExp.length() > 0 && isOperator(cleanExp.charAt(cleanExp.length() - 1))) {
            cleanExp = cleanExp.substring(0, cleanExp.length() - 1);
        }
        
        if (cleanExp.isEmpty()) {
            tvResult.setText("");
            return;
        }
        
        try {
            double res = eval(cleanExp);
            if (Double.isInfinite(res) || Double.isNaN(res)) {
                tvResult.setText("");
            } else {
                tvResult.setText(formatResult(res));
            }
        } catch (Exception e) {
            // Intentionally silent for preview updates
        }
    }

    private void calculateFinalResult() {
        String exp = tvExpression.getText().toString();
        if (exp.trim().isEmpty()) {
            return;
        }
        try {
            double res = eval(exp);
            String finalRes = formatResult(res);
            tvExpression.setText(finalRes);
            tvResult.setText(finalRes);
            isResultShown = true;
        } catch (ArithmeticException e) {
            tvResult.setText("Error: Div by 0");
        } catch (Exception e) {
            tvResult.setText("Error");
        }
    }

    private boolean isOperator(char c) {
        return c == '+' || c == '-' || c == '×' || c == '÷' || c == '*' || c == '/';
    }

    private String formatResult(double d) {
        if (d == (long) d) {
            return String.format("%d", (long) d);
        } else {
            DecimalFormat df = new DecimalFormat("#.##########");
            return df.format(d);
        }
    }

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
                if (pos < str.length()) throw new RuntimeException("Unexpected: " + (char)ch);
                return x;
            }

            double parseExpression() {
                double x = parseTerm();
                for (;;) {
                    if      (consume('+')) x += parseTerm();
                    else if (consume('-')) x -= parseTerm();
                    else return x;
                }
            }

            double parseTerm() {
                double x = parseFactor();
                for (;;) {
                    if      (consume('×') || consume('*')) x *= parseFactor();
                    else if (consume('÷') || consume('/')) {
                        double divisor = parseFactor();
                        if (divisor == 0) throw new ArithmeticException("Division by zero");
                        x /= divisor;
                    }
                    else return x;
                }
            }

            double parseFactor() {
                if (consume('+')) return parseFactor();
                if (consume('-')) return -parseFactor();

                double x;
                int startPos = this.pos;
                if (consume('(')) {
                    x = parseExpression();
                    consume(')');
                } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(str.substring(startPos, this.pos));
                } else {
                    throw new RuntimeException("Unexpected: " + (char)ch);
                }

                if (consume('%')) {
                    x = x / 100.0;
                }

                return x;
            }
        }.parse();
    }
}