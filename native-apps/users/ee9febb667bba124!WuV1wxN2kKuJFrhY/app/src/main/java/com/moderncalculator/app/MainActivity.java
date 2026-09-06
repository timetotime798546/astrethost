package com.moderncalculator.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.text.DecimalFormat;

public class MainActivity extends Activity {

    private TextView expressionTv;
    private TextView resultTv;
    private boolean isEvaluated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        expressionTv = (TextView) findViewById(R.id.expression_tv);
        resultTv = (TextView) findViewById(R.id.result_tv);

        setupButtons();
    }

    private void setupButtons() {
        int[] numButtons = {
            R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4,
            R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9
        };

        for (int i = 0; i < numButtons.length; i++) {
            final Button btn = (Button) findViewById(numButtons[i]);
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onNumberClicked(btn.getText().toString());
                }
            });
        }

        int[] opButtons = {
            R.id.btn_plus, R.id.btn_minus, R.id.btn_multiply, R.id.btn_divide
        };

        for (int i = 0; i < opButtons.length; i++) {
            final Button btn = (Button) findViewById(opButtons[i]);
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onOperatorClicked(btn.getText().toString());
                }
            });
        }

        findViewById(R.id.btn_ac).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearAll();
            }
        });

        findViewById(R.id.btn_backspace).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackspaceClicked();
            }
        });

        findViewById(R.id.btn_dot).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDotClicked();
            }
        });

        findViewById(R.id.btn_percent).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onPercentClicked();
            }
        });

        findViewById(R.id.btn_toggle).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onToggleClicked();
            }
        });

        findViewById(R.id.btn_equals).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onEqualsClicked();
            }
        });
    }

    private void onNumberClicked(String number) {
        if (isEvaluated) {
            expressionTv.setText("");
            isEvaluated = false;
        }
        String current = expressionTv.getText().toString();
        if (current.equals("0")) {
            expressionTv.setText(number);
        } else {
            expressionTv.append(number);
        }
        updateRealTimeResult();
    }

    private void onOperatorClicked(String op) {
        isEvaluated = false;
        String current = expressionTv.getText().toString();
        if (current.isEmpty()) {
            if (op.equals("-")) {
                expressionTv.append(op);
            }
            return;
        }

        char lastChar = current.charAt(current.length() - 1);
        if (isOperator(lastChar)) {
            expressionTv.setText(current.substring(0, current.length() - 1) + op);
        } else {
            expressionTv.append(op);
        }
    }

    private boolean isOperator(char c) {
        return c == '+' || c == '-' || c == '×' || c == '÷' || c == '*' || c == '/';
    }

    private void onDotClicked() {
        if (isEvaluated) {
            expressionTv.setText("0");
            isEvaluated = false;
        }
        String current = expressionTv.getText().toString();
        if (current.isEmpty()) {
            expressionTv.append("0.");
            return;
        }

        int lastOpIdx = -1;
        for (int i = current.length() - 1; i >= 0; i--) {
            if (isOperator(current.charAt(i))) {
                lastOpIdx = i;
                break;
            }
        }

        String lastNumberSegment = current.substring(lastOpIdx + 1);
        if (!lastNumberSegment.contains(".")) {
            expressionTv.append(".");
        }
    }

    private void onPercentClicked() {
        if (isEvaluated) {
            isEvaluated = false;
        }
        String current = expressionTv.getText().toString();
        if (!current.isEmpty()) {
            char lastChar = current.charAt(current.length() - 1);
            if (!isOperator(lastChar) && lastChar != '%') {
                expressionTv.append("%");
                updateRealTimeResult();
            }
        }
    }

    private void onToggleClicked() {
        if (isEvaluated) {
            isEvaluated = false;
        }
        String exp = expressionTv.getText().toString();
        if (exp.isEmpty()) {
            expressionTv.setText("-");
            return;
        }

        int i = exp.length() - 1;
        while (i >= 0 && (Character.isDigit(exp.charAt(i)) || exp.charAt(i) == '.' || exp.charAt(i) == '%')) {
            i--;
        }

        if (i >= 0 && exp.charAt(i) == '-') {
            boolean isUnaryMinus = false;
            if (i == 0) {
                isUnaryMinus = true;
            } else {
                char prev = exp.charAt(i - 1);
                if (isOperator(prev) || prev == '(') {
                    isUnaryMinus = true;
                }
            }
            if (isUnaryMinus) {
                expressionTv.setText(exp.substring(0, i) + exp.substring(i + 1));
                updateRealTimeResult();
                return;
            }
        }

        expressionTv.setText(exp.substring(0, i + 1) + "-" + exp.substring(i + 1));
        updateRealTimeResult();
    }

    private void onBackspaceClicked() {
        if (isEvaluated) {
            clearAll();
            return;
        }
        String current = expressionTv.getText().toString();
        if (!current.isEmpty()) {
            expressionTv.setText(current.substring(0, current.length() - 1));
            updateRealTimeResult();
        }
    }

    private void clearAll() {
        expressionTv.setText("");
        resultTv.setText("0");
        isEvaluated = false;
    }

    private void onEqualsClicked() {
        String current = expressionTv.getText().toString();
        if (current.isEmpty()) return;

        try {
            double rawResult = eval(current);
            String formatted = formatResult(rawResult);
            resultTv.setText(formatted);
            expressionTv.setText(formatted);
            isEvaluated = true;
        } catch (Exception e) {
            resultTv.setText("Error");
        }
    }

    private void updateRealTimeResult() {
        String current = expressionTv.getText().toString();
        if (current.isEmpty()) {
            resultTv.setText("0");
            return;
        }

        String cleanExp = current;
        while (!cleanExp.isEmpty() && isOperator(cleanExp.charAt(cleanExp.length() - 1))) {
            cleanExp = cleanExp.substring(0, cleanExp.length() - 1);
        }

        if (cleanExp.isEmpty()) {
            resultTv.setText("0");
            return;
        }

        try {
            double rawResult = eval(cleanExp);
            resultTv.setText(formatResult(rawResult));
        } catch (Exception e) {
            // Silence real-time eval errors during typing
        }
    }

    private String formatResult(double value) {
        if (Double.isInfinite(value) || Double.isNaN(value)) {
            return "Error";
        }
        if (value == (long) value) {
            return String.format("%d", (long) value);
        } else {
            DecimalFormat df = new DecimalFormat("#.########");
            return df.format(value);
        }
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
                    if      (eat('×') || eat('*')) x *= parseFactor();
                    else if (eat('÷') || eat('/')) {
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
                } else {
                    throw new RuntimeException("Unexpected: " + (char)ch);
                }

                if (eat('%')) x = x / 100.0;

                return x;
            }
        }.parse();
    }
}