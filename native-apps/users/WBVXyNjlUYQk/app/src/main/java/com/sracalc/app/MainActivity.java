package com.sracalc.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {

    private TextView tvExpression;
    private TextView tvResult;
    private String currentExpression = "";
    private boolean isEvaluated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvExpression = (TextView) findViewById(R.id.tv_expression);
        tvResult = (TextView) findViewById(R.id.tv_result);

        // Standard Numbers and Decimal mapping
        int[] numBtnIds = {
            R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4,
            R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9,
            R.id.btn_decimal
        };

        for (int i = 0; i < numBtnIds.length; i++) {
            final Button btn = (Button) findViewById(numBtnIds[i]);
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    appendToken(btn.getText().toString());
                }
            });
        }

        // Standard Operators mapping
        int[] opBtnIds = {
            R.id.btn_add, R.id.btn_subtract, R.id.btn_multiply, R.id.btn_divide, R.id.btn_power
        };

        for (int i = 0; i < opBtnIds.length; i++) {
            final Button btn = (Button) findViewById(opBtnIds[i]);
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    appendToken(btn.getText().toString());
                }
            });
        }

        // Parentheses and Constants mapping
        int[] constBtnIds = {
            R.id.btn_pi, R.id.btn_e, R.id.btn_paren_open, R.id.btn_paren_close
        };

        for (int i = 0; i < constBtnIds.length; i++) {
            final Button btn = (Button) findViewById(constBtnIds[i]);
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    appendToken(btn.getText().toString());
                }
            });
        }

        // Scientific math functions triggers
        findViewById(R.id.btn_sin).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                appendToken("sin(");
            }
        });

        findViewById(R.id.btn_cos).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                appendToken("cos(");
            }
        });

        findViewById(R.id.btn_tan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                appendToken("tan(");
            }
        });

        findViewById(R.id.btn_sqrt).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                appendToken("√(");
            }
        });

        // Reset/Clear application display
        findViewById(R.id.btn_clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentExpression = "";
                isEvaluated = false;
                tvExpression.setText("");
                tvResult.setText("0");
            }
        });

        // Backspace utility execution
        findViewById(R.id.btn_backspace).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isEvaluated) {
                    currentExpression = "";
                    isEvaluated = false;
                } else if (currentExpression.length() > 0) {
                    if (currentExpression.endsWith("sin(") || currentExpression.endsWith("cos(") || currentExpression.endsWith("tan(")) {
                        currentExpression = currentExpression.substring(0, currentExpression.length() - 4);
                    } else if (currentExpression.endsWith("√(")) {
                        currentExpression = currentExpression.substring(0, currentExpression.length() - 2);
                    } else {
                        currentExpression = currentExpression.substring(0, currentExpression.length() - 1);
                    }
                }
                updateDisplay();
            }
        });

        // Plus/Minus +/- modifier key
        findViewById(R.id.btn_plus_minus).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isEvaluated) {
                    isEvaluated = false;
                }
                if (currentExpression.isEmpty()) {
                    currentExpression = "-";
                } else if (currentExpression.startsWith("-") && !currentExpression.contains(" ") && !currentExpression.contains("+") && !currentExpression.contains("−") && !currentExpression.contains("×") && !currentExpression.contains("÷")) {
                    currentExpression = currentExpression.substring(1);
                } else if (currentExpression.endsWith("+")) {
                    currentExpression = currentExpression.substring(0, currentExpression.length() - 1) + "−";
                } else if (currentExpression.endsWith("−")) {
                    currentExpression = currentExpression.substring(0, currentExpression.length() - 1) + "+";
                } else {
                    currentExpression += "-";
                }
                updateDisplay();
            }
        });

        // Mathematical Evaluator trigger
        findViewById(R.id.btn_equal).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentExpression.isEmpty()) return;
                
                try {
                    double value = evaluateExpression(currentExpression);
                    String resultStr = formatResult(value);
                    tvExpression.setText(currentExpression);
                    tvResult.setText(resultStr);
                    currentExpression = resultStr;
                    isEvaluated = true;
                } catch (Exception e) {
                    tvResult.setText("Error");
                }
            }
        });
    }

    private void appendToken(String token) {
        if (isEvaluated) {
            isEvaluated = false;
            if (isOperator(token)) {
                // Carry evaluated result as first operand
            } else {
                currentExpression = "";
            }
        }
        currentExpression += token;
        updateDisplay();
    }

    private boolean isOperator(String token) {
        return token.equals("+") || token.equals("−") || token.equals("×") || token.equals("÷") || token.equals("^");
    }

    private void updateDisplay() {
        if (currentExpression.isEmpty()) {
            tvExpression.setText("");
            tvResult.setText("0");
        } else {
            tvExpression.setText(currentExpression);
            // Real-time calculation visual output
            try {
                double liveValue = evaluateExpression(currentExpression);
                tvResult.setText(formatResult(liveValue));
            } catch (Exception e) {
                // Incomplete expression, mute output
            }
        }
    }

    private double evaluateExpression(String expr) {
        String normalized = expr
            .replace("×", "*")
            .replace("÷", "/")
            .replace("−", "-")
            .replace("√", "sqrt");
        return ExpressionEvaluator.eval(normalized);
    }

    private String formatResult(double val) {
        if (Double.isInfinite(val)) return "Error";
        if (Double.isNaN(val)) return "Error";
        if (val == (long) val) {
            return String.valueOf((long) val);
        }
        try {
            double rounded = Math.round(val * 100000000.0) / 100000000.0;
            if (rounded == (long) rounded) {
                return String.valueOf((long) rounded);
            }
            return String.valueOf(rounded);
        } catch (Exception e) {
            return String.valueOf(val);
        }
    }

    // High performance mathematical recursive descent parser class (Java 8 compatible)
    private static class ExpressionEvaluator {
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
                    if (pos < str.length()) throw new RuntimeException("Unexpected: " + (char)ch);
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
                        else if (eat('/')) x /= parseFactor();
                        else return x;
                    }
                }

                double parseFactor() {
                    if (eat('+')) return +parseFactor();
                    if (eat('-')) return -parseFactor();

                    double x;
                    int startPos = this.pos;
                    if (eat('(')) {
                        x = parseExpression();
                        eat(')');
                    } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                        while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                        x = Double.parseDouble(str.substring(startPos, this.pos));
                    } else if ((ch >= 'a' && ch <= 'z') || ch == 'π' || ch == 'e') {
                        while ((ch >= 'a' && ch <= 'z') || ch == 'π' || ch == 'e') nextChar();
                        String func = str.substring(startPos, this.pos);
                        if (func.equals("π")) {
                            x = Math.PI;
                        } else if (func.equals("e")) {
                            x = Math.E;
                        } else {
                            double arg;
                            if (eat('(')) {
                                arg = parseExpression();
                                eat(')');
                            } else {
                                arg = parseFactor();
                            }
                            if (func.equals("sqrt")) x = Math.sqrt(arg);
                            else if (func.equals("sin")) x = Math.sin(Math.toRadians(arg));
                            else if (func.equals("cos")) x = Math.cos(Math.toRadians(arg));
                            else if (func.equals("tan")) x = Math.tan(Math.toRadians(arg));
                            else if (func.equals("log")) x = Math.log10(arg);
                            else if (func.equals("ln")) x = Math.log(arg);
                            else throw new RuntimeException("Unknown function: " + func);
                        }
                    } else {
                        throw new RuntimeException("Unexpected character: " + (char)ch);
                    }

                    if (eat('^')) x = Math.pow(x, parseFactor());

                    return x;
                }
            }.parse();
        }
    }
}