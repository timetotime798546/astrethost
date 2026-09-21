package com.moderncalculator.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.text.DecimalFormat;
import java.util.ArrayList;

public class MainActivity extends Activity {

    private TextView tvFormula;
    private TextView tvResult;
    private LinearLayout historyPanel;
    private LinearLayout historyListContainer;
    private Button btnToggleHistory;

    private String expression = "";
    private boolean isResultDisplayed = false;
    private final ArrayList<String> historyList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind layout elements
        tvFormula = (TextView) findViewById(R.id.tv_formula);
        tvResult = (TextView) findViewById(R.id.tv_result);
        historyPanel = (LinearLayout) findViewById(R.id.history_panel);
        historyListContainer = (LinearLayout) findViewById(R.id.history_list_container);
        btnToggleHistory = (Button) findViewById(R.id.btn_toggle_history);

        // Standard Numbers
        int[] numericButtons = {
                R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4,
                R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9
        };

        View.OnClickListener numberClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button b = (Button) v;
                if (isResultDisplayed) {
                    expression = "";
                    isResultDisplayed = false;
                }
                expression += b.getText().toString();
                updateDisplay();
            }
        };

        for (int id : numericButtons) {
            findViewById(id).setOnClickListener(numberClickListener);
        }

        // Decimal point
        findViewById(R.id.btn_decimal).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isResultDisplayed) {
                    expression = "0";
                    isResultDisplayed = false;
                }
                if (expression.isEmpty()) {
                    expression = "0";
                }
                expression += ".";
                updateDisplay();
            }
        });

        // Sign change (+/-)
        findViewById(R.id.btn_plus_minus).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (expression.isEmpty()) return;
                // Add brackets or toggle negative on current trailing expression
                if (expression.startsWith("-")) {
                    expression = expression.substring(1);
                } else {
                    expression = "-" + expression;
                }
                updateDisplay();
            }
        });

        // Basic operators (+, -, *, /)
        findViewById(R.id.btn_add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { appendOperator("+"); }
        });
        findViewById(R.id.btn_subtract).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { appendOperator("-"); }
        });
        findViewById(R.id.btn_multiply).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { appendOperator("×"); }
        });
        findViewById(R.id.btn_divide).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { appendOperator("÷"); }
        });

        // Action Keys (Clear, Backspace, Percentage)
        findViewById(R.id.btn_clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                expression = "";
                tvFormula.setText("");
                tvResult.setText("0");
                isResultDisplayed = false;
            }
        });

        findViewById(R.id.btn_backspace).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isResultDisplayed) {
                    expression = "";
                    isResultDisplayed = false;
                } else if (!expression.isEmpty()) {
                    // Check if it's a multi-character function (sin(, cos(, tan(, sqrt(, ln(, log()
                    if (expression.endsWith("sin(") || expression.endsWith("cos(") || expression.endsWith("tan(") || expression.endsWith("log(")) {
                        expression = expression.substring(0, expression.length() - 4);
                    } else if (expression.endsWith("sqrt(")) {
                        expression = expression.substring(0, expression.length() - 5);
                    } else if (expression.endsWith("ln(")) {
                        expression = expression.substring(0, expression.length() - 3);
                    } else {
                        expression = expression.substring(0, expression.length() - 1);
                    }
                }
                updateDisplay();
            }
        });

        findViewById(R.id.btn_percent).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isResultDisplayed) {
                    isResultDisplayed = false;
                }
                expression += "%";
                updateDisplay();
            }
        });

        // Scientific Keys
        findViewById(R.id.btn_sin).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { appendFunction("sin("); }
        });
        findViewById(R.id.btn_cos).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { appendFunction("cos("); }
        });
        findViewById(R.id.btn_tan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { appendFunction("tan("); }
        });
        findViewById(R.id.btn_sqrt).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { appendFunction("sqrt("); }
        });
        findViewById(R.id.btn_pow).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { appendOperator("^"); }
        });
        findViewById(R.id.btn_sqr).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isResultDisplayed) {
                    isResultDisplayed = false;
                }
                expression += "^2";
                updateDisplay();
            }
        });
        findViewById(R.id.btn_recip).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isResultDisplayed) {
                    expression = "1÷(" + expression + ")";
                    isResultDisplayed = false;
                } else {
                    expression += "1÷";
                }
                updateDisplay();
            }
        });
        findViewById(R.id.btn_ln).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { appendFunction("ln("); }
        });
        findViewById(R.id.btn_log).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { appendFunction("log("); }
        });
        findViewById(R.id.btn_pi).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isResultDisplayed) {
                    expression = "";
                    isResultDisplayed = false;
                }
                expression += "π";
                updateDisplay();
            }
        });
        findViewById(R.id.btn_e).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isResultDisplayed) {
                    expression = "";
                    isResultDisplayed = false;
                }
                expression += "e";
                updateDisplay();
            }
        });
        findViewById(R.id.btn_open_paren).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { appendFunction("("); }
        });
        findViewById(R.id.btn_close_paren).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { appendFunction(")"); }
        });

        // Equals (=) computation trigger
        findViewById(R.id.btn_equals).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calculateResult();
            }
        });

        // History UI Toggles
        btnToggleHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                historyPanel.setVisibility(View.VISIBLE);
                populateHistoryUI();
            }
        });

        findViewById(R.id.btn_close_history).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                historyPanel.setVisibility(View.GONE);
            }
        });

        findViewById(R.id.btn_clear_history).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                historyList.clear();
                populateHistoryUI();
            }
        });
    }

    private void appendOperator(String op) {
        if (isResultDisplayed) {
            isResultDisplayed = false;
        }
        if (expression.isEmpty() && op.equals("-")) {
            expression = "-";
            updateDisplay();
            return;
        }
        if (!expression.isEmpty()) {
            char lastChar = expression.charAt(expression.length() - 1);
            if (lastChar == '+' || lastChar == '-' || lastChar == '×' || lastChar == '÷' || lastChar == '^') {
                // replace operator
                expression = expression.substring(0, expression.length() - 1) + op;
            } else {
                expression += op;
            }
            updateDisplay();
        }
    }

    private void appendFunction(String func) {
        if (isResultDisplayed) {
            expression = "";
            isResultDisplayed = false;
        }
        expression += func;
        updateDisplay();
    }

    private void updateDisplay() {
        if (expression.isEmpty()) {
            tvResult.setText("0");
        } else {
            tvResult.setText(expression);
        }
    }

    private void calculateResult() {
        if (expression.isEmpty()) return;

        try {
            String cleanExpr = expression.replace("×", "*").replace("÷", "/");
            double resultVal = eval(cleanExpr);

            // Format result beautifully
            DecimalFormat df = new DecimalFormat("#.########");
            String formattedResult = df.format(resultVal);

            // Avoid displaying NaN or Infinity issues programmatically
            if (Double.isNaN(resultVal) || Double.isInfinite(resultVal)) {
                tvResult.setText("Error");
                return;
            }

            // Update displays
            tvFormula.setText(expression + " =");
            tvResult.setText(formattedResult);

            // Log calculation to History
            historyList.add(0, expression + " = " + formattedResult);

            expression = formattedResult;
            isResultDisplayed = true;

        } catch (Exception e) {
            tvResult.setText("Error");
        }
    }

    private void populateHistoryUI() {
        historyListContainer.removeAllViews();
        if (historyList.isEmpty()) {
            TextView emptyTv = new TextView(this);
            emptyTv.setText(getString(R.string.history_empty));
            emptyTv.setTextColor(0xFF94A3B8);
            emptyTv.setTextSize(14);
            emptyTv.setPadding(0, 16, 0, 16);
            historyListContainer.addView(emptyTv);
        } else {
            for (int i = 0; i < historyList.size(); i++) {
                final String record = historyList.get(i);
                TextView rowTv = new TextView(this);
                rowTv.setText(record);
                rowTv.setTextColor(0xFFF1F5F9);
                rowTv.setTextSize(15);
                rowTv.setPadding(8, 12, 8, 12);
                rowTv.setClickable(true);
                rowTv.setFocusable(true);
                rowTv.setBackgroundResource(R.drawable.btn_numeric);

                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                lp.setMargins(0, 4, 0, 8);
                rowTv.setLayoutParams(lp);

                // Restore record on click
                rowTv.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String[] parts = record.split(" = ");
                        if (parts.length > 0) {
                            expression = parts[0];
                            isResultDisplayed = false;
                            updateDisplay();
                            tvFormula.setText("");
                            historyPanel.setVisibility(View.GONE);
                        }
                    }
                });

                historyListContainer.addView(rowTv);
            }
        }
    }

    // Mathematical formula parser logic
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
                if (pos < str.length()) throw new RuntimeException("Unexpected: " + (char) ch);
                return x;
            }

            double parseExpression() {
                double x = parseTerm();
                for (; ; ) {
                    if (eat('+')) x += parseTerm(); // addition
                    else if (eat('-')) x -= parseTerm(); // subtraction
                    else return x;
                }
            }

            double parseTerm() {
                double x = parseFactor();
                for (; ; ) {
                    if (eat('*')) x *= parseFactor(); // multiplication
                    else if (eat('/')) x /= parseFactor(); // division
                    else return x;
                }
            }

            double parseFactor() {
                if (eat('+')) return +parseFactor(); // unary plus
                if (eat('-')) return -parseFactor(); // unary minus

                double x;
                int startPos = this.pos;
                if (eat('(')) { // parentheses
                    x = parseExpression();
                    if (!eat(')')) throw new RuntimeException("Missing closing parenthesis");
                } else if ((ch >= '0' && ch <= '9') || ch == '.') { // numbers
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(str.substring(startPos, this.pos));
                } else if (ch >= 'a' && ch <= 'z' || ch == 'π' || ch == 'e') { // functions or constants
                    if (eat('π')) {
                        x = Math.PI;
                    } else if (eat('e')) {
                        x = Math.E;
                    } else {
                        while (ch >= 'a' && ch <= 'z') nextChar();
                        String func = str.substring(startPos, this.pos);
                        if (eat('(')) {
                            x = parseExpression();
                            if (!eat(')')) throw new RuntimeException("Missing closing parenthesis for " + func);
                        } else {
                            x = parseFactor();
                        }
                        if (func.equals("sqrt")) x = Math.sqrt(x);
                        else if (func.equals("sin")) x = Math.sin(Math.toRadians(x));
                        else if (func.equals("cos")) x = Math.cos(Math.toRadians(x));
                        else if (func.equals("tan")) x = Math.tan(Math.toRadians(x));
                        else if (func.equals("log")) x = Math.log10(x);
                        else if (func.equals("ln")) x = Math.log(x);
                        else if (func.equals("abs")) x = Math.abs(x);
                        else throw new RuntimeException("Unknown function: " + func);
                    }
                } else {
                    throw new RuntimeException("Unexpected character: " + (char) ch);
                }

                if (eat('^')) x = Math.pow(x, parseFactor()); // exponentiation
                if (eat('%')) x = x / 100.0; // percentage suffix evaluation

                return x;
            }
        }.parse();
    }
}