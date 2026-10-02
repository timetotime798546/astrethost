package com.generatedappname.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity implements View.OnClickListener {

    private TextView display;
    private StringBuilder input = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        display = (TextView) findViewById(R.id.display);
        int[] buttonIds = {
                R.id.button0, R.id.button1, R.id.button2, R.id.button3,
                R.id.button4, R.id.button5, R.id.button6, R.id.button7,
                R.id.button8, R.id.button9, R.id.buttonAdd, R.id.buttonSubtract,
                R.id.buttonMultiply, R.id.buttonDivide, R.id.buttonDecimal,
                R.id.buttonClear, R.id.buttonEquals
        };
        for (int id : buttonIds) {
            Button b = (Button) findViewById(id);
            b.setOnClickListener(this);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.button0:
                appendInput("0");
                break;
            case R.id.button1:
                appendInput("1");
                break;
            case R.id.button2:
                appendInput("2");
                break;
            case R.id.button3:
                appendInput("3");
                break;
            case R.id.button4:
                appendInput("4");
                break;
            case R.id.button5:
                appendInput("5");
                break;
            case R.id.button6:
                appendInput("6");
                break;
            case R.id.button7:
                appendInput("7");
                break;
            case R.id.button8:
                appendInput("8");
                break;
            case R.id.button9:
                appendInput("9");
                break;
            case R.id.buttonDecimal:
                appendInput(".");
                break;
            case R.id.buttonAdd:
                appendInput("+");
                break;
            case R.id.buttonSubtract:
                appendInput("-");
                break;
            case R.id.buttonMultiply:
                appendInput("*");
                break;
            case R.id.buttonDivide:
                appendInput("/");
                break;
            case R.id.buttonClear:
                clearInput();
                break;
            case R.id.buttonEquals:
                evaluateExpression();
                break;
        }
    }

    private void appendInput(String str) {
        input.append(str);
        display.setText(input.toString());
    }

    private void clearInput() {
        input.setLength(0);
        display.setText("");
    }

    private void evaluateExpression() {
        String expr = input.toString();
        try {
            double result = evaluate(expr);
            display.setText(String.valueOf(result));
            input.setLength(0);
            input.append(result);
        } catch (Exception e) {
            display.setText("Error");
            input.setLength(0);
        }
    }

    // Simple left-to-right evaluation without operator precedence
    private double evaluate(String expr) throws Exception {
        if (expr.isEmpty()) {
            return 0;
        }
        java.util.ArrayList<Double> numbers = new java.util.ArrayList<Double>();
        java.util.ArrayList<Character> ops = new java.util.ArrayList<Character>();
        StringBuilder num = new StringBuilder();
        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);
            if ((c >= '0' && c <= '9') || c == '.') {
                num.append(c);
            } else if (c == '+' || c == '-' || c == '*' || c == '/') {
                if (num.length() == 0) {
                    // handle unary minus
                    if (c == '-' && (i == 0 || expr.charAt(i - 1) == '(')) {
                        num.append(c);
                        continue;
                    } else {
                        throw new Exception("Invalid expression");
                    }
                }
                numbers.add(Double.parseDouble(num.toString()));
                num.setLength(0);
                ops.add(c);
            } else {
                throw new Exception("Invalid character");
            }
        }
        if (num.length() > 0) {
            numbers.add(Double.parseDouble(num.toString()));
        }
        double result = numbers.get(0);
        for (int i = 0; i < ops.size(); i++) {
            char op = ops.get(i);
            double next = numbers.get(i + 1);
            switch (op) {
                case '+':
                    result += next;
                    break;
                case '-':
                    result -= next;
                    break;
                case '*':
                    result *= next;
                    break;
                case '/':
                    result /= next;
                    break;
            }
        }
        return result;
    }
}
