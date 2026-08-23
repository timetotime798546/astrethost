package com.simplecalculator.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements View.OnClickListener {

    private TextView tvExpression;
    private TextView tvResult;

    private String currentExpression = "";
    private boolean isEvaluated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvExpression = (TextView) findViewById(R.id.tvExpression);
        tvResult = (TextView) findViewById(R.id.tvResult);

        int[] buttonIds = {
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9,
            R.id.btnAC, R.id.btnDel, R.id.btnPercent, R.id.btnDiv,
            R.id.btnMul, R.id.btnSub, R.id.btnAdd, R.id.btnSign,
            R.id.btnDot, R.id.btnEqual
        };

        for (int id : buttonIds) {
            findViewById(id).setOnClickListener(this);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.btnAC) {
            currentExpression = "";
            tvExpression.setText("");
            tvResult.setText("0");
            isEvaluated = false;
        } else if (id == R.id.btnDel) {
            if (isEvaluated) {
                currentExpression = "";
                tvExpression.setText("");
                tvResult.setText("0");
                isEvaluated = false;
            } else if (currentExpression.length() > 0) {
                currentExpression = currentExpression.substring(0, currentExpression.length() - 1);
                updateDisplay();
            }
        } else if (id == R.id.btnEqual) {
            if (currentExpression.length() > 0) {
                try {
                    double result = evaluate(currentExpression);
                    tvExpression.setText(currentExpression + " =");
                    tvResult.setText(formatResult(result));
                    currentExpression = formatResult(result);
                    isEvaluated = true;
                } catch (Exception e) {
                    tvResult.setText("Error");
                    isEvaluated = true;
                }
            }
        } else if (id == R.id.btnSign) {
            if (isEvaluated) {
                isEvaluated = false;
            }
            negateLastNumber();
        } else if (id == R.id.btnPercent) {
            if (isEvaluated) {
                isEvaluated = false;
            }
            applyPercentage();
        } else {
            if (isEvaluated) {
                if (isOperator(getButtonChar(id))) {
                    isEvaluated = false;
                } else {
                    currentExpression = "";
                    isEvaluated = false;
                }
            }

            String btnChar = getButtonChar(id);
            if (isOperator(btnChar)) {
                if (currentExpression.length() > 0) {
                    char lastChar = currentExpression.charAt(currentExpression.length() - 1);
                    if (isOperator(String.valueOf(lastChar))) {
                        currentExpression = currentExpression.substring(0, currentExpression.length() - 1) + btnChar;
                    } else {
                        currentExpression += btnChar;
                    }
                }
            } else {
                currentExpression += btnChar;
            }
            updateDisplay();
        }
    }

    private void updateDisplay() {
        if (currentExpression.isEmpty()) {
            tvResult.setText("0");
        } else {
            tvResult.setText(currentExpression);
        }
    }

    private String getButtonChar(int id) {
        if (id == R.id.btn0) return "0";
        if (id == R.id.btn1) return "1";
        if (id == R.id.btn2) return "2";
        if (id == R.id.btn3) return "3";
        if (id == R.id.btn4) return "4";
        if (id == R.id.btn5) return "5";
        if (id == R.id.btn6) return "6";
        if (id == R.id.btn7) return "7";
        if (id == R.id.btn8) return "8";
        if (id == R.id.btn9) return "9";
        if (id == R.id.btnDot) return ".";
        if (id == R.id.btnDiv) return "÷";
        if (id == R.id.btnMul) return "×";
        if (id == R.id.btnSub) return "−";
        if (id == R.id.btnAdd) return "+";
        return "";
    }

    private boolean isOperator(String s) {
        return s.equals("+") || s.equals("−") || s.equals("×") || s.equals("÷");
    }

    private void negateLastNumber() {
        if (currentExpression.isEmpty()) return;

        int i = currentExpression.length() - 1;
        while (i >= 0) {
            char c = currentExpression.charAt(i);
            if (isOperator(String.valueOf(c)) && i > 0) {
                char prev = currentExpression.charAt(i - 1);
                if (isOperator(String.valueOf(prev))) {
                    i--;
                    continue;
                }
                break;
            }
            i--;
        }

        String prefix = currentExpression.substring(0, i + 1);
        String lastNum = currentExpression.substring(i + 1);

        if (lastNum.startsWith("-")) {
            lastNum = lastNum.substring(1);
        } else if (!lastNum.isEmpty()) {
            lastNum = "-" + lastNum;
        }

        currentExpression = prefix + lastNum;
        updateDisplay();
    }

    private void applyPercentage() {
        if (currentExpression.isEmpty()) return;

        int i = currentExpression.length() - 1;
        while (i >= 0) {
            char c = currentExpression.charAt(i);
            if (isOperator(String.valueOf(c)) && i > 0) {
                break;
            }
            i--;
        }

        String prefix = currentExpression.substring(0, i + 1);
        String lastNum = currentExpression.substring(i + 1);

        if (!lastNum.isEmpty()) {
            try {
                double val = Double.parseDouble(lastNum) / 100.0;
                currentExpression = prefix + formatResult(val);
                updateDisplay();
            } catch (NumberFormatException e) {
                // Fallback safe escape
            }
        }
    }

    private String formatResult(double d) {
        if (Double.isInfinite(d) || Double.isNaN(d)) {
            return "Error";
        }
        if (d == (long) d) {
            return String.format("%d", (long) d);
        } else {
            DecimalFormat df = new DecimalFormat("#.########");
            return df.format(d);
        }
    }

    private double evaluate(String expression) throws Exception {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);
            if (c == '+' || c == '−' || c == '×' || c == '÷') {
                if (sb.length() > 0) {
                    tokens.add(sb.toString());
                    sb.setLength(0);
                } else if (c == '−' && (tokens.isEmpty() || isOperator(tokens.get(tokens.size() - 1)))) {
                    sb.append('-');
                    continue;
                }
                tokens.add(String.valueOf(c));
            } else {
                sb.append(c);
            }
        }
        if (sb.length() > 0) {
            tokens.add(sb.toString());
        }

        if (tokens.isEmpty()) return 0;

        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            if (token.equals("−")) tokens.set(i, "-");
            if (token.equals("×")) tokens.set(i, "*");
            if (token.equals("÷")) tokens.set(i, "/");
        }

        List<String> nextTokens = new ArrayList<>();
        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            if (token.equals("*") || token.equals("/")) {
                if (nextTokens.isEmpty() || i + 1 >= tokens.size()) {
                    throw new Exception("Invalid Expression");
                }
                double left = Double.parseDouble(nextTokens.remove(nextTokens.size() - 1));
                double right = Double.parseDouble(tokens.get(++i));
                if (token.equals("/") && right == 0) {
                    throw new ArithmeticException("Division by zero");
                }
                double res = token.equals("*") ? left * right : left / right;
                nextTokens.add(String.valueOf(res));
            } else {
                nextTokens.add(token);
            }
        }

        if (nextTokens.isEmpty()) return 0;
        double result = Double.parseDouble(nextTokens.get(0));
        for (int i = 1; i < nextTokens.size(); i += 2) {
            String op = nextTokens.get(i);
            double val = Double.parseDouble(nextTokens.get(i + 1));
            if (op.equals("+")) {
                result += val;
            } else if (op.equals("-")) {
                result -= val;
            }
        }

        return result;
    }
}
