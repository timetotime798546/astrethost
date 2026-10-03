package com.moderncalculator.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private TextView historyTextView;
    private TextView inputTextView;
    private TextView previewTextView;
    private ScrollView historyScrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        historyTextView = (TextView) findViewById(R.id.history_text);
        inputTextView = (TextView) findViewById(R.id.input_text);
        previewTextView = (TextView) findViewById(R.id.preview_text);
        historyScrollView = (ScrollView) findViewById(R.id.history_scroll);

        // Map numeric keys
        int[] numIds = {
                R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4,
                R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9
        };

        for (int id : numIds) {
            final Button b = (Button) findViewById(id);
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    appendSymbol(b.getText().toString());
                }
            });
        }

        // Dot key
        findViewById(R.id.btn_dot).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                appendDot();
            }
        });

        // Map operational operator keys
        findViewById(R.id.btn_add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { appendOperator("+"); }
        });
        findViewById(R.id.btn_subtract).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { appendOperator("−"); }
        });
        findViewById(R.id.btn_multiply).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { appendOperator("×"); }
        });
        findViewById(R.id.btn_divide).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { appendOperator("÷"); }
        });
        findViewById(R.id.btn_percent).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { appendOperator("%"); }
        });

        // Sign key (±)
        findViewById(R.id.btn_toggle_sign).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleSign();
            }
        });

        // Clear keys (Standard Click and Long Click to clear History)
        Button clearBtn = (Button) findViewById(R.id.btn_clear);
        clearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                inputTextView.setText("");
                previewTextView.setText("");
            }
        });
        clearBtn.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                historyTextView.setText("");
                Toast.makeText(MainActivity.this, "History tape cleared", Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        // Delete (Backspace) key
        findViewById(R.id.btn_delete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performBackspace();
            }
        });

        // Evaluation triggering Equals key
        findViewById(R.id.btn_equals).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onEqualsPressed();
            }
        });

        if (savedInstanceState != null) {
            historyTextView.setText(savedInstanceState.getString("HISTORY_LOG", ""));
            inputTextView.setText(savedInstanceState.getString("INPUT_EXPR", ""));
            previewTextView.setText(savedInstanceState.getString("PREVIEW_VAL", ""));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("HISTORY_LOG", historyTextView.getText().toString());
        outState.putString("INPUT_EXPR", inputTextView.getText().toString());
        outState.putString("PREVIEW_VAL", previewTextView.getText().toString());
    }

    private void appendSymbol(String symbol) {
        String currentExpr = inputTextView.getText().toString();
        // Prevent typing numbers immediately after a closed parenthesis sign negation if formatted
        if (currentExpr.endsWith(")")) {
            currentExpr += "×";
        }
        inputTextView.setText(currentExpr + symbol);
        evaluateLivePreview();
    }

    private void appendDot() {
        String expr = inputTextView.getText().toString();
        if (expr.isEmpty()) {
            inputTextView.setText("0.");
            return;
        }

        // Find the index of the start of the current numeric token
        int lastTokenStart = 0;
        for (int i = expr.length() - 1; i >= 0; i--) {
            if (isOperator(expr.charAt(i))) {
                lastTokenStart = i + 1;
                break;
            }
        }
        String currentToken = expr.substring(lastTokenStart);
        if (!currentToken.contains(".")) {
            inputTextView.setText(expr + ".");
        }
    }

    private void appendOperator(String op) {
        String expr = inputTextView.getText().toString();
        if (expr.isEmpty()) {
            if (op.equals("−")) {
                inputTextView.setText("-");
            }
            return;
        }

        char lastChar = expr.charAt(expr.length() - 1);
        if (isOperator(lastChar)) {
            // Replace the last operator with the newly keyed operator
            inputTextView.setText(expr.substring(0, expr.length() - 1) + op);
        } else {
            inputTextView.setText(expr + op);
        }
        evaluateLivePreview();
    }

    private void toggleSign() {
        String expr = inputTextView.getText().toString();
        if (expr.isEmpty()) {
            inputTextView.setText("-");
            return;
        }
        if (expr.equals("-")) {
            inputTextView.setText("");
            return;
        }

        // Find index of last operator
        int lastOpIdx = -1;
        for (int i = expr.length() - 1; i >= 0; i--) {
            if (isOperator(expr.charAt(i))) {
                lastOpIdx = i;
                break;
            }
        }

        if (lastOpIdx == -1) {
            // Toggle signs on lone numeric values
            if (expr.startsWith("-")) {
                inputTextView.setText(expr.substring(1));
            } else {
                inputTextView.setText("-" + expr);
            }
        } else {
            String leftPart = expr.substring(0, lastOpIdx + 1);
            String rightPart = expr.substring(lastOpIdx + 1);
            if (rightPart.startsWith("(-") && rightPart.endsWith(")")) {
                // Strip structural parentheses
                rightPart = rightPart.substring(2, rightPart.length() - 1);
            } else if (rightPart.startsWith("-")) {
                rightPart = rightPart.substring(1);
            } else {
                rightPart = "(-" + rightPart + ")";
            }
            inputTextView.setText(leftPart + rightPart);
        }
        evaluateLivePreview();
    }

    private void performBackspace() {
        String expr = inputTextView.getText().toString();
        if (!expr.isEmpty()) {
            inputTextView.setText(expr.substring(0, expr.length() - 1));
        }
        evaluateLivePreview();
    }

    private void evaluateLivePreview() {
        String expr = inputTextView.getText().toString().trim();
        if (expr.isEmpty()) {
            previewTextView.setText("");
            return;
        }

        // Strip trailing operators to evaluate partial equations cleanly
        while (expr.length() > 0 && isOperator(expr.charAt(expr.length() - 1))) {
            expr = expr.substring(0, expr.length() - 1).trim();
        }

        if (expr.isEmpty()) {
            previewTextView.setText("");
            return;
        }

        String cleanExpr = expr.replace("×", "*")
                .replace("÷", "/")
                .replace("−", "-");

        try {
            double result = ExpressionParser.evaluate(cleanExpr);
            previewTextView.setText(formatResult(result));
        } catch (Exception e) {
            previewTextView.setText("");
        }
    }

    private void onEqualsPressed() {
        String expr = inputTextView.getText().toString().trim();
        if (expr.isEmpty()) return;

        // Clean trailing operators
        while (expr.length() > 0 && isOperator(expr.charAt(expr.length() - 1))) {
            expr = expr.substring(0, expr.length() - 1).trim();
        }

        if (expr.isEmpty()) return;

        String cleanExpr = expr.replace("×", "*")
                .replace("÷", "/")
                .replace("−", "-");

        try {
            double result = ExpressionParser.evaluate(cleanExpr);
            String formatted = formatResult(result);

            // Add step to visual history tape
            String entry = expr + " = " + formatted + "\n";
            historyTextView.append(entry);

            // Smooth scroll tracking on history tape
            historyScrollView.post(new Runnable() {
                @Override
                public void run() {
                    historyScrollView.fullScroll(View.FOCUS_DOWN);
                }
            });

            inputTextView.setText(formatted);
            previewTextView.setText("");
        } catch (ArithmeticException e) {
            previewTextView.setText("Divide by zero");
        } catch (Exception e) {
            previewTextView.setText("Error");
        }
    }

    private boolean isOperator(char c) {
        return c == '+' || c == '−' || c == '×' || c == '÷' || c == '%';
    }

    private String formatResult(double d) {
        if (Double.isInfinite(d) || Double.isNaN(d)) {
            return "Error";
        }
        if (d == (long) d) {
            return String.format("%d", (long) d);
        } else {
            // Truncate trailing floating zeros nicely up to 8 places
            String s = String.format("%.8f", d);
            s = s.replaceAll("0+$", "");
            if (s.endsWith(".")) {
                s = s.substring(0, s.length() - 1);
            }
            return s;
        }
    }

    // Mathematical Parsing Logic utilizing a Java-8 compatible Recursive Descent parser
    private static class ExpressionParser {
        public static double evaluate(final String str) {
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
                    if (pos < str.length()) throw new RuntimeException("Unexpected character: " + (char) ch);
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
                            if (divisor == 0) throw new ArithmeticException("Divide by zero");
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
                        throw new RuntimeException("Unexpected parse: " + (char) ch);
                    }

                    // Percentage handler
                    if (eat('%')) {
                        x = x / 100.0;
                    }

                    return x;
                }
            }.parse();
        }
    }
}