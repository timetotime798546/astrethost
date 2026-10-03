package com.moderncalculator.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.text.DecimalFormat;

public class MainActivity extends Activity {

    private TextView tvHistory;
    private TextView tvFormula;
    private TextView tvResult;

    private String currentFormula = "";
    private boolean isResultDisplayed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvHistory = (TextView) findViewById(R.id.tvHistory);
        tvFormula = (TextView) findViewById(R.id.tvFormula);
        tvResult = (TextView) findViewById(R.id.tvResult);

        setupButtonListeners();
        updateDisplay();
    }

    private void setupButtonListeners() {
        int[] numericButtons = {
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9,
            R.id.btnDot
        };

        View.OnClickListener numericClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button b = (Button) v;
                if (isResultDisplayed) {
                    currentFormula = "";
                    isResultDisplayed = false;
                }
                String btnText = b.getText().toString();
                if (btnText.equals(".")) {
                    if (canAddDecimal()) {
                        currentFormula += ".";
                    }
                } else {
                    if (currentFormula.equals("0")) {
                        currentFormula = btnText;
                    } else {
                        currentFormula += btnText;
                    }
                }
                updateDisplay();
                triggerRealtimeCalculation();
            }
        };

        for (int id : numericButtons) {
            findViewById(id).setOnClickListener(numericClickListener);
        }

        int[] operatorButtons = {
            R.id.btnAdd, R.id.btnSubtract, R.id.btnMultiply, R.id.btnDivide, R.id.btnPercent
        };

        View.OnClickListener operatorClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button b = (Button) v;
                if (isResultDisplayed) {
                    isResultDisplayed = false;
                }
                String op = b.getText().toString();
                if (currentFormula.length() > 0) {
                    char lastChar = currentFormula.charAt(currentFormula.length() - 1);
                    if (isOperator(lastChar)) {
                        currentFormula = currentFormula.substring(0, currentFormula.length() - 1) + op;
                    } else {
                        currentFormula += op;
                    }
                } else if (op.equals("-")) {
                    currentFormula += "-";
                }
                updateDisplay();
            }
        };

        for (int id : operatorButtons) {
            findViewById(id).setOnClickListener(operatorClickListener);
        }

        findViewById(R.id.btnClear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentFormula = "";
                tvHistory.setText("");
                tvResult.setText("");
                isResultDisplayed = false;
                updateDisplay();
            }
        });

        findViewById(R.id.btnDelete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isResultDisplayed) {
                    currentFormula = "";
                    isResultDisplayed = false;
                } else if (currentFormula.length() > 0) {
                    currentFormula = currentFormula.substring(0, currentFormula.length() - 1);
                }
                updateDisplay();
                triggerRealtimeCalculation();
            }
        });

        findViewById(R.id.btnBrackets).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isResultDisplayed) {
                    currentFormula = "";
                    isResultDisplayed = false;
                }
                appendSmartBracket();
                updateDisplay();
                triggerRealtimeCalculation();
            }
        });

        findViewById(R.id.btnEquals).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performFinalCalculation();
            }
        });
    }

    private void updateDisplay() {
        if (currentFormula.isEmpty()) {
            tvFormula.setText("0");
        } else {
            tvFormula.setText(currentFormula);
        }
    }

    private boolean isOperator(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/' || c == '%';
    }

    private boolean canAddDecimal() {
        if (currentFormula.isEmpty()) return true;
        
        int len = currentFormula.length();
        for (int i = len - 1; i >= 0; i--) {
            char c = currentFormula.charAt(i);
            if (isOperator(c) || c == '(' || c == ')') {
                break;
            }
            if (c == '.') {
                return false;
            }
        }
        return true;
    }

    private void appendSmartBracket() {
        if (currentFormula.isEmpty()) {
            currentFormula += "(";
            return;
        }

        int len = currentFormula.length();
        char lastChar = currentFormula.charAt(len - 1);

        int openCount = 0;
        int closeCount = 0;
        for (int i = 0; i < len; i++) {
            if (currentFormula.charAt(i) == '(') openCount++;
            if (currentFormula.charAt(i) == ')') closeCount++;
        }

        if (openCount > closeCount && (Character.isDigit(lastChar) || lastChar == ')')) {
            currentFormula += ")";
        } else if (isOperator(lastChar) || lastChar == '(' || currentFormula.isEmpty()) {
            currentFormula += "(";
        } else {
            currentFormula += "*(";
        }
    }

    private void triggerRealtimeCalculation() {
        if (currentFormula.isEmpty()) {
            tvResult.setText("");
            return;
        }

        if (!hasOperatorsOrBrackets(currentFormula)) {
            tvResult.setText("");
            return;
        }

        String sanitized = sanitizeFormula(currentFormula);
        try {
            double result = CalculatorEngine.evaluate(sanitized);
            tvResult.setText(formatResult(result));
        } catch (Exception e) {
            tvResult.setText("");
        }
    }

    private void performFinalCalculation() {
        if (currentFormula.isEmpty()) return;

        String sanitized = sanitizeFormula(currentFormula);
        try {
            double result = CalculatorEngine.evaluate(sanitized);
            String formattedResult = formatResult(result);
            
            tvHistory.setText(currentFormula + " =");
            currentFormula = formattedResult;
            tvResult.setText("");
            isResultDisplayed = true;
            updateDisplay();
        } catch (ArithmeticException e) {
            tvResult.setText("Error: Division by 0");
        } catch (Exception e) {
            tvResult.setText("Format Error");
        }
    }

    private boolean hasOperatorsOrBrackets(String formula) {
        for (int i = 0; i < formula.length(); i++) {
            char c = formula.charAt(i);
            if (isOperator(c) || c == '(' || c == ')') {
                return true;
            }
        }
        return false;
    }

    private String sanitizeFormula(String formula) {
        String s = formula;
        
        while (s.length() > 0 && isOperator(s.charAt(s.length() - 1))) {
            s = s.substring(0, s.length() - 1);
        }

        int openCount = 0;
        int closeCount = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '(') openCount++;
            if (s.charAt(i) == ')') closeCount++;
        }
        
        while (openCount > closeCount) {
            s += ")";
            closeCount++;
        }

        return s;
    }

    private String formatResult(double val) {
        if (Double.isInfinite(val)) return "Error: Infinity";
        if (Double.isNaN(val)) return "Error";
        
        if (val == (long) val) {
            return String.format("%d", (long) val);
        } else {
            DecimalFormat df = new DecimalFormat("0.########");
            return df.format(val);
        }
    }
}