package com.simplecalculator.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.text.DecimalFormat;

public class MainActivity extends Activity implements View.OnClickListener {

    private TextView tvFormula;
    private TextView tvResult;
    private boolean isEvaluated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvFormula = (TextView) findViewById(R.id.tvFormula);
        tvResult = (TextView) findViewById(R.id.tvResult);

        // Bind Digits
        findViewById(R.id.btn0).setOnClickListener(this);
        findViewById(R.id.btn1).setOnClickListener(this);
        findViewById(R.id.btn2).setOnClickListener(this);
        findViewById(R.id.btn3).setOnClickListener(this);
        findViewById(R.id.btn4).setOnClickListener(this);
        findViewById(R.id.btn5).setOnClickListener(this);
        findViewById(R.id.btn6).setOnClickListener(this);
        findViewById(R.id.btn7).setOnClickListener(this);
        findViewById(R.id.btn8).setOnClickListener(this);
        findViewById(R.id.btn9).setOnClickListener(this);
        findViewById(R.id.btnDecimal).setOnClickListener(this);

        // Bind Operators & Actions
        findViewById(R.id.btnAdd).setOnClickListener(this);
        findViewById(R.id.btnSubtract).setOnClickListener(this);
        findViewById(R.id.btnMultiply).setOnClickListener(this);
        findViewById(R.id.btnDivide).setOnClickListener(this);
        findViewById(R.id.btnPercent).setOnClickListener(this);
        findViewById(R.id.btnClear).setOnClickListener(this);
        findViewById(R.id.btnDelete).setOnClickListener(this);
        findViewById(R.id.btnEquals).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.btn0) {
            appendDigit("0");
        } else if (id == R.id.btn1) {
            appendDigit("1");
        } else if (id == R.id.btn2) {
            appendDigit("2");
        } else if (id == R.id.btn3) {
            appendDigit("3");
        } else if (id == R.id.btn4) {
            appendDigit("4");
        } else if (id == R.id.btn5) {
            appendDigit("5");
        } else if (id == R.id.btn6) {
            appendDigit("6");
        } else if (id == R.id.btn7) {
            appendDigit("7");
        } else if (id == R.id.btn8) {
            appendDigit("8");
        } else if (id == R.id.btn9) {
            appendDigit("9");
        } else if (id == R.id.btnDecimal) {
            appendDecimal();
        } else if (id == R.id.btnAdd) {
            appendOperator("+");
        } else if (id == R.id.btnSubtract) {
            appendOperator("-");
        } else if (id == R.id.btnMultiply) {
            appendOperator("×");
        } else if (id == R.id.btnDivide) {
            appendOperator("÷");
        } else if (id == R.id.btnPercent) {
            appendOperator("%");
        } else if (id == R.id.btnClear) {
            clear();
        } else if (id == R.id.btnDelete) {
            deleteLast();
        } else if (id == R.id.btnEquals) {
            evaluate();
        }
    }

    private void appendDigit(String digit) {
        if (isEvaluated) {
            tvFormula.setText("");
            isEvaluated = false;
        }
        String currentFormula = tvFormula.getText().toString();
        if (currentFormula.equals("0")) {
            tvFormula.setText(digit);
        } else {
            tvFormula.append(digit);
        }
    }

    private void appendDecimal() {
        if (isEvaluated) {
            tvFormula.setText("");
            isEvaluated = false;
        }
        String currentFormula = tvFormula.getText().toString();
        if (currentFormula.isEmpty()) {
            tvFormula.setText("0.");
        } else if (canAppendDecimal()) {
            char lastChar = currentFormula.charAt(currentFormula.length() - 1);
            if (isOperator(lastChar)) {
                tvFormula.append("0.");
            } else {
                tvFormula.append(".");
            }
        }
    }

    private void appendOperator(String op) {
        if (isEvaluated) {
            // Keep the previous result to continue calculation
            String prevResult = tvResult.getText().toString();
            if (!prevResult.equals("Error")) {
                tvFormula.setText(prevResult);
            } else {
                tvFormula.setText("");
            }
            isEvaluated = false;
        }

        String currentFormula = tvFormula.getText().toString();
        if (currentFormula.isEmpty()) {
            if (op.equals("-")) {
                tvFormula.setText("-");
            }
            return;
        }

        char lastChar = currentFormula.charAt(currentFormula.length() - 1);
        if (isOperator(lastChar)) {
            // Replace previous operator with the new one, except when typing unary minus
            if (currentFormula.length() == 1 && lastChar == '-') {
                if (op.equals("-")) return; // ignore duplicate leading minus
                tvFormula.setText(""); // clear if we replace negative
            } else {
                tvFormula.setText(currentFormula.substring(0, currentFormula.length() - 1) + op);
            }
        } else {
            tvFormula.append(op);
        }
    }

    private void clear() {
        tvFormula.setText("");
        tvResult.setText("0");
        isEvaluated = false;
    }

    private void deleteLast() {
        if (isEvaluated) {
            clear();
            return;
        }
        String currentFormula = tvFormula.getText().toString();
        if (!currentFormula.isEmpty()) {
            tvFormula.setText(currentFormula.substring(0, currentFormula.length() - 1));
        }
    }

    private boolean isOperator(char c) {
        return c == '+' || c == '-' || c == '×' || c == '÷' || c == '%';
    }

    private boolean canAppendDecimal() {
        String formula = tvFormula.getText().toString();
        if (formula.isEmpty()) {
            return true;
        }
        int lastOperatorIdx = -1;
        String operators = "+-×÷%";
        for (int i = formula.length() - 1; i >= 0; i--) {
            if (operators.indexOf(formula.charAt(i)) != -1) {
                lastOperatorIdx = i;
                break;
            }
        }
        String lastNumberSegment = formula.substring(lastOperatorIdx + 1);
        return !lastNumberSegment.contains(".");
    }

    private void evaluate() {
        String formula = tvFormula.getText().toString();
        if (formula.isEmpty()) {
            return;
        }

        // Clean trailing operators if any
        char lastChar = formula.charAt(formula.length() - 1);
        if (isOperator(lastChar)) {
            formula = formula.substring(0, formula.length() - 1);
        }

        try {
            double value = eval(formula);
            if (Double.isInfinite(value) || Double.isNaN(value)) {
                tvResult.setText("Error");
            } else {
                // Formatting clean output
                if (value == (long) value) {
                    tvResult.setText(String.valueOf((long) value));
                } else {
                    // Limit digits to avoid overflow on screens
                    DecimalFormat df = new DecimalFormat("#.########");
                    tvResult.setText(df.format(value));
                }
            }
        } catch (Exception e) {
            tvResult.setText("Error");
        }
        isEvaluated = true;
    }

    private double eval(final String str) {
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
                    if      (consume('*') || consume('×')) x *= parseFactor();
                    else if (consume('/') || consume('÷')) {
                        double divisor = parseFactor();
                        if (divisor == 0) throw new ArithmeticException("Division by zero");
                        x /= divisor;
                    }
                    else if (consume('%')) {
                        double divisor = parseFactor();
                        if (divisor == 0) throw new ArithmeticException("Modulo by zero");
                        x %= divisor;
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

                return x;
            }
        }.parse();
    }
}