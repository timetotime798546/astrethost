package com.moderncalculator.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private TextView tvExpression;
    private TextView tvResult;
    private StringBuilder currentInput = new StringBuilder();
    private boolean hasEvaluated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvExpression = (TextView) findViewById(R.id.tv_expression);
        tvResult = (TextView) findViewById(R.id.tv_result);

        setupKeyBindings();
    }

    private void setupKeyBindings() {
        // Number keys initialization
        int[] numberIds = {
            R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4,
            R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9
        };

        for (int i = 0; i < numberIds.length; i++) {
            final String numStr = String.valueOf(i);
            findViewById(numberIds[i]).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    appendCharacter(numStr);
                }
            });
        }

        // Operator keys initialization
        findViewById(R.id.btn_add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                appendOperator("+");
            }
        });

        findViewById(R.id.btn_subtract).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                appendOperator("-");
            }
        });

        findViewById(R.id.btn_multiply).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                appendOperator("×");
            }
        });

        findViewById(R.id.btn_divide).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                appendOperator("÷");
            }
        });

        findViewById(R.id.btn_dot).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                appendDecimal();
            }
        });

        findViewById(R.id.btn_percent).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                appendPercent();
            }
        });

        findViewById(R.id.btn_plus_minus).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleSign();
            }
        });

        findViewById(R.id.btn_clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearAll();
            }
        });

        findViewById(R.id.btn_delete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                backspace();
            }
        });

        findViewById(R.id.btn_equal).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                evaluateFinal();
            }
        });
    }

    private void appendCharacter(String character) {
        if (hasEvaluated) {
            currentInput.setLength(0);
            hasEvaluated = false;
        }
        currentInput.append(character);
        updateDisplay();
        updateLivePreview();
    }

    private void appendOperator(String op) {
        if (currentInput.length() == 0) {
            return; 
        }
        
        if (hasEvaluated) {
            hasEvaluated = false;
        }

        char lastChar = currentInput.charAt(currentInput.length() - 1);
        if (isOperator(lastChar)) {
            // Replace the last operator with the new selection
            currentInput.setLength(currentInput.length() - 1);
        } else if (lastChar == '.') {
            currentInput.append("0");
        }
        
        currentInput.append(op);
        updateDisplay();
    }

    private void appendDecimal() {
        if (hasEvaluated) {
            currentInput.setLength(0);
            hasEvaluated = false;
        }

        if (currentInput.length() == 0) {
            currentInput.append("0.");
        } else {
            // Find the last segment of the input since the last operator
            int lastOpIndex = -1;
            for (int i = currentInput.length() - 1; i >= 0; i--) {
                if (isOperator(currentInput.charAt(i))) {
                    lastOpIndex = i;
                    break;
                }
            }
            String lastNumberSegment = currentInput.substring(lastOpIndex + 1);
            if (!lastNumberSegment.contains(".")) {
                currentInput.append(".");
            }
        }
        updateDisplay();
    }

    private void appendPercent() {
        if (currentInput.length() == 0) {
            return;
        }
        char lastChar = currentInput.charAt(currentInput.length() - 1);
        if (Character.isDigit(lastChar)) {
            currentInput.append("%");
            updateDisplay();
            updateLivePreview();
        }
    }

    private void toggleSign() {
        if (currentInput.length() == 0) {
            return;
        }
        
        // Find the index where the last numeric entry begins
        int idx = currentInput.length() - 1;
        while (idx >= 0 && !isOperator(currentInput.charAt(idx))) {
            idx--;
        }
        
        if (idx < 0) {
            // entire sequence is a number, prepend sign
            if (currentInput.charAt(0) == '-') {
                currentInput.deleteCharAt(0);
            } else {
                currentInput.insert(0, "-");
            }
        } else {
            // toggle sign of the last operand segment
            char operator = currentInput.charAt(idx);
            if (operator == '-') {
                // check if it is acts as active binary operator or sign
                if (idx == 0 || isOperator(currentInput.charAt(idx - 1))) {
                    currentInput.deleteCharAt(idx);
                } else {
                    currentInput.setCharAt(idx, '+');
                }
            } else if (operator == '+') {
                currentInput.setCharAt(idx, '-');
            } else {
                // Operator is × or ÷
                currentInput.insert(idx + 1, "-");
            }
        }
        updateDisplay();
        updateLivePreview();
    }

    private void clearAll() {
        currentInput.setLength(0);
        hasEvaluated = false;
        tvExpression.setText("");
        tvResult.setText("");
    }

    private void backspace() {
        if (currentInput.length() > 0) {
            currentInput.deleteCharAt(currentInput.length() - 1);
            updateDisplay();
            updateLivePreview();
        }
    }

    private void updateDisplay() {
        tvExpression.setText(currentInput.toString());
    }

    private void updateLivePreview() {
        if (currentInput.length() == 0) {
            tvResult.setText("");
            return;
        }
        try {
            String exp = currentInput.toString();
            char last = exp.charAt(exp.length() - 1);
            if (isOperator(last)) {
                exp = exp.substring(0, exp.length() - 1);
            }
            double parsedVal = evaluateExpression(exp);
            tvResult.setText(formatResult(parsedVal));
        } catch (Exception e) {
            tvResult.setText("");
        }
    }

    private void evaluateFinal() {
        if (currentInput.length() == 0) {
            return;
        }
        try {
            String rawExpression = currentInput.toString();
            double solution = evaluateExpression(rawExpression);
            String formattedResult = formatResult(solution);
            
            tvExpression.setText(formattedResult);
            tvResult.setText("");
            currentInput.setLength(0);
            currentInput.append(formattedResult);
            hasEvaluated = true;
        } catch (Exception e) {
            tvResult.setText("Error");
        }
    }

    private boolean isOperator(char c) {
        return c == '+' || c == '-' || c == '×' || c == '÷';
    }

    private String formatResult(double value) {
        if (Double.isInfinite(value) || Double.isNaN(value)) {
            return "Error";
        }
        if (value == (long) value) {
            return String.format("%d", (long) value);
        }
        return String.format("%.6f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    // Native safe Expression Parser via basic scanning logic without external libraries
    private double evaluateExpression(String expression) throws Exception {
        if (expression == null || expression.trim().isEmpty()) {
            return 0;
        }
        
        // Convert percentages
        expression = processPercentages(expression);
        
        // Lex operations and numeric tokens
        List<String> tokens = lex(expression);
        if (tokens.isEmpty()) {
            return 0;
        }
        
        // Level 1: Multiplication and Division
        List<String> processedTokens = new ArrayList<String>();
        for (int i = 0; i < tokens.size(); i++) {
            String current = tokens.get(i);
            if (current.equals("×") || current.equals("÷")) {
                if (processedTokens.isEmpty() || i + 1 >= tokens.size()) {
                    throw new Exception("Invalid syntax");
                }
                double left = Double.parseDouble(processedTokens.remove(processedTokens.size() - 1));
                double right = Double.parseDouble(tokens.get(++i));
                double productResult = current.equals("×") ? (left * right) : (left / right);
                processedTokens.add(String.valueOf(productResult));
            } else {
                processedTokens.add(current);
            }
        }
        
        // Level 2: Addition and Subtraction
        double runningTotal = 0;
        if (!processedTokens.isEmpty()) {
            runningTotal = Double.parseDouble(processedTokens.get(0));
            for (int i = 1; i < processedTokens.size(); i += 2) {
                String operator = processedTokens.get(i);
                double nextValue = Double.parseDouble(processedTokens.get(i + 1));
                if (operator.equals("+")) {
                    runningTotal += nextValue;
                } else if (operator.equals("-")) {
                    runningTotal -= nextValue;
                }
            }
        }
        
        return runningTotal;
    }

    private String processPercentages(String expr) {
        StringBuilder processed = new StringBuilder();
        int len = expr.length();
        for (int i = 0; i < len; i++) {
            char c = expr.charAt(i);
            if (c == '%') {
                // Extract number before %
                int start = i - 1;
                while (start >= 0 && (Character.isDigit(expr.charAt(start)) || expr.charAt(start) == '.')) {
                    start--;
                }
                start++;
                String numberStr = expr.substring(start, i);
                double pctValue = Double.parseDouble(numberStr) / 100.0;
                processed.setLength(processed.length() - numberStr.length());
                processed.append(pctValue);
            } else {
                processed.append(c);
            }
        }
        return processed.toString();
    }

    private List<String> lex(String expr) {
        List<String> tokens = new ArrayList<String>();
        StringBuilder numberBuffer = new StringBuilder();
        int len = expr.length();
        
        for (int i = 0; i < len; i++) {
            char c = expr.charAt(i);
            if (isOperator(c)) {
                // Check for negative signs starting values
                if (c == '-' && (i == 0 || isOperator(expr.charAt(i - 1)))) {
                    numberBuffer.append(c);
                } else {
                    if (numberBuffer.length() > 0) {
                        tokens.add(numberBuffer.toString());
                        numberBuffer.setLength(0);
                    }
                    tokens.add(String.valueOf(c));
                }
            } else {
                numberBuffer.append(c);
            }
        }
        if (numberBuffer.length() > 0) {
            tokens.add(numberBuffer.toString());
        }
        return tokens;
    }
}