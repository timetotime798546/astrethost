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
    
    private StringBuilder currentInput = new StringBuilder();
    private StringBuilder expressionStr = new StringBuilder();
    private boolean isCalculated = false;
    private final DecimalFormat decimalFormat = new DecimalFormat("0.########");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvExpression = (TextView) findViewById(R.id.tv_expression);
        tvResult = (TextView) findViewById(R.id.tv_result);

        setupButtons();
    }

    private void setupButtons() {
        int[] numberIds = new int[] {
                R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4,
                R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9
        };

        View.OnClickListener numberClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button b = (Button) v;
                if (isCalculated) {
                    clearAll();
                }
                currentInput.append(b.getText().toString());
                updateDisplay();
                evaluateOnTheFly();
            }
        };

        for (int id : numberIds) {
            findViewById(id).setOnClickListener(numberClickListener);
        }

        findViewById(R.id.btn_decimal).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isCalculated) {
                    clearAll();
                }
                if (currentInput.indexOf(".") == -1) {
                    if (currentInput.length() == 0) {
                        currentInput.append("0");
                    }
                    currentInput.append(".");
                    updateDisplay();
                }
            }
        });

        int[] operatorIds = new int[] {
                R.id.btn_add, R.id.btn_subtract, R.id.btn_multiply, R.id.btn_divide
        };

        View.OnClickListener operatorClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button b = (Button) v;
                String op = b.getText().toString();
                
                if (isCalculated) {
                    String res = tvResult.getText().toString();
                    clearAll();
                    if (!res.equals("Error")) {
                        currentInput.append(res);
                    }
                }

                if (currentInput.length() > 0) {
                    expressionStr.append(currentInput).append(" ").append(op).append(" ");
                    currentInput.setLength(0);
                } else if (expressionStr.length() > 0) {
                    // Replace last operator if user changes mind
                    expressionStr.setLength(expressionStr.length() - 3);
                    expressionStr.append(" ").append(op).append(" ");
                }
                updateDisplay();
            }
        };

        for (int id : operatorIds) {
            findViewById(id).setOnClickListener(operatorClickListener);
        }

        findViewById(R.id.btn_clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearAll();
            }
        });

        findViewById(R.id.btn_delete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isCalculated) {
                    clearAll();
                } else if (currentInput.length() > 0) {
                    currentInput.deleteCharAt(currentInput.length() - 1);
                    updateDisplay();
                    evaluateOnTheFly();
                }
            }
        });

        findViewById(R.id.btn_percent).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentInput.length() > 0) {
                    try {
                        double val = Double.parseDouble(currentInput.toString()) / 100.0;
                        currentInput.setLength(0);
                        currentInput.append(decimalFormat.format(val));
                        updateDisplay();
                        evaluateOnTheFly();
                    } catch (NumberFormatException e) {
                        // ignore
                    }
                }
            }
        });

        findViewById(R.id.btn_sign).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentInput.length() > 0) {
                    try {
                        double val = Double.parseDouble(currentInput.toString()) * -1.0;
                        currentInput.setLength(0);
                        currentInput.append(decimalFormat.format(val));
                        updateDisplay();
                        evaluateOnTheFly();
                    } catch (NumberFormatException e) {
                        // ignore
                    }
                }
            }
        });

        findViewById(R.id.btn_equal).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentInput.length() > 0 || expressionStr.length() > 0) {
                    String finalExpr = expressionStr.toString() + currentInput.toString();
                    double result = evaluateExpression(finalExpr);
                    if (Double.isNaN(result)) {
                        tvResult.setText("Error");
                    } else {
                        tvResult.setText(decimalFormat.format(result));
                    }
                    tvExpression.setText(finalExpr);
                    isCalculated = true;
                }
            }
        });
    }

    private void clearAll() {
        currentInput.setLength(0);
        expressionStr.setLength(0);
        isCalculated = false;
        tvExpression.setText("");
        tvResult.setText("0");
    }

    private void updateDisplay() {
        String current = currentInput.toString();
        String full = expressionStr.toString() + current;
        tvExpression.setText(full);
        if (current.length() > 0) {
            tvResult.setText(current);
        } else {
            tvResult.setText("0");
        }
    }

    private void evaluateOnTheFly() {
        if (expressionStr.length() > 0) {
            String full = expressionStr.toString() + currentInput.toString();
            double preview = evaluateExpression(full);
            if (!Double.isNaN(preview)) {
                tvResult.setText(decimalFormat.format(preview));
            }
        }
    }

    private double evaluateExpression(String expression) {
        try {
            final String formatted = expression.replace("×", "*")
                                               .replace("÷", "/")
                                               .replaceAll("\\s+", "");
            return new Object() {
                int pos = -1, ch;

                void nextChar() {
                    ch = (++pos < formatted.length()) ? formatted.charAt(pos) : -1;
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
                    if (pos < formatted.length()) return Double.NaN;
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
                            if (divisor == 0) return Double.NaN;
                            x /= divisor;
                        } else return x;
                    }
                }

                double parseFactor() {
                    if (eat('+')) return parseFactor();
                    if (eat('-')) return -parseFactor();

                    double x;
                    int startPos = this.pos;
                    if ((ch >= '0' && ch <= '9') || ch == '.') {
                        while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                        try {
                            x = Double.parseDouble(formatted.substring(startPos, this.pos));
                        } catch (NumberFormatException e) {
                            return Double.NaN;
                        }
                    } else {
                        return Double.NaN;
                    }
                    return x;
                }
            }.parse();
        } catch (Exception e) {
            return Double.NaN;
        }
    }
}