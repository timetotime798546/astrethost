package com.procalculator.app;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends Activity {

    private TextView tvDisplay;
    private TextView tvHistory;
    private StringBuilder currentExpression = new StringBuilder();
    private boolean isResultDisplayed = false;
    private ArrayList<String> historyList = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvDisplay = (TextView) findViewById(R.id.tvDisplay);
        tvHistory = (TextView) findViewById(R.id.tvHistory);

        // History button click listener
        findViewById(R.id.btnHistoryToggle).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showHistoryDialog();
            }
        });

        int[] numberButtonIds = {
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9,
            R.id.btnDecimal, R.id.btnOpenBracket, R.id.btnCloseBracket
        };

        View.OnClickListener numberClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button b = (Button) v;
                String val = b.getText().toString();

                if (isResultDisplayed) {
                    currentExpression.setLength(0);
                    isResultDisplayed = false;
                }

                currentExpression.append(val);
                tvDisplay.setText(currentExpression.toString());
            }
        };

        for (int id : numberButtonIds) {
            findViewById(id).setOnClickListener(numberClickListener);
        }

        View.OnClickListener operatorClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button b = (Button) v;
                String op = b.getText().toString();

                if (isResultDisplayed) {
                    isResultDisplayed = false;
                }

                if (currentExpression.length() > 0) {
                    char lastChar = currentExpression.charAt(currentExpression.length() - 1);
                    if (lastChar == '+' || lastChar == '-' || lastChar == '×' || lastChar == '÷') {
                        currentExpression.setCharAt(currentExpression.length() - 1, op.charAt(0));
                    } else {
                        currentExpression.append(op);
                    }
                    tvDisplay.setText(currentExpression.toString());
                } else if (op.equals("-")) {
                    currentExpression.append("-");
                    tvDisplay.setText(currentExpression.toString());
                }
            }
        };

        findViewById(R.id.btnAdd).setOnClickListener(operatorClickListener);
        findViewById(R.id.btnSubtract).setOnClickListener(operatorClickListener);
        findViewById(R.id.btnMultiply).setOnClickListener(operatorClickListener);
        findViewById(R.id.btnDivide).setOnClickListener(operatorClickListener);

        findViewById(R.id.btnClear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentExpression.setLength(0);
                tvDisplay.setText("0");
                tvHistory.setText("");
                isResultDisplayed = false;
            }
        });

        findViewById(R.id.btnBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isResultDisplayed) {
                    currentExpression.setLength(0);
                    tvDisplay.setText("0");
                    isResultDisplayed = false;
                    return;
                }

                if (currentExpression.length() > 0) {
                    currentExpression.deleteCharAt(currentExpression.length() - 1);
                    if (currentExpression.length() == 0) {
                        tvDisplay.setText("0");
                    } else {
                        tvDisplay.setText(currentExpression.toString());
                    }
                }
            }
        });

        findViewById(R.id.btnEquals).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentExpression.length() == 0) return;

                String expr = currentExpression.toString();
                try {
                    double result = evaluate(expr);
                    
                    String formattedResult;
                    if (result == (long) result) {
                        formattedResult = String.format("%d", (long) result);
                    } else {
                        formattedResult = String.format("%s", result);
                    }

                    String historyRecord = expr + " = " + formattedResult;
                    historyList.add(0, historyRecord);

                    tvHistory.setText(historyRecord);
                    tvDisplay.setText(formattedResult);
                    
                    currentExpression.setLength(0);
                    currentExpression.append(formattedResult);
                    isResultDisplayed = true;
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Invalid Expression", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showHistoryDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_history);
        dialog.setTitle("Calculation History");

        final ListView listHistory = (ListView) dialog.findViewById(R.id.listHistory);
        Button btnClearHistory = (Button) dialog.findViewById(R.id.btnClearHistory);
        Button btnCloseHistory = (Button) dialog.findViewById(R.id.btnCloseHistory);

        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(
            this,
            android.R.layout.simple_list_item_1,
            historyList
        ) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView tv = (TextView) view.findViewById(android.R.id.text1);
                tv.setTextColor(0xFFFFFFFF);
                tv.setTextSize(16spToPx(6)); // Approx styling
                return view;
            }
        };

        listHistory.setAdapter(adapter);

        btnClearHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                historyList.clear();
                adapter.notifyDataSetChanged();
                Toast.makeText(MainActivity.this, "History cleared", Toast.LENGTH_SHORT).show();
            }
        });

        btnCloseHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        if (historyList.isEmpty()) {
            Toast.makeText(this, "No history available", Toast.LENGTH_SHORT).show();
            return;
        }

        dialog.show();
    }

    private int spToPx(float sp) {
        float scale = getResources().getDisplayMetrics().scaledDensity;
        return (int) (sp * scale + 0.5f);
    }

    private double evaluate(String expression) {
        return new Object() {
            int pos = -1, ch;

            void nextChar() {
                ch = (++pos < expression.length()) ? expression.charAt(pos) : -1;
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
                if (pos < expression.length()) throw new RuntimeException("Unexpected: " + (char)ch);
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
                        double denom = parseFactor();
                        if (denom == 0) throw new RuntimeException("Division by zero");
                        x /= denom;
                    }
                    else return x;
                }
            }

            double parseFactor() {
                if (eat('+')) return parseFactor();
                if (eat('-')) return -parseFactor();

                double x;
                int startPos = pos;
                if (eat('(')) {
                    x = parseExpression();
                    eat(')');
                } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(expression.substring(startPos, pos));
                } else {
                    throw new RuntimeException("Unexpected: " + (char)ch);
                }

                return x;
            }
        }.parse();
    }
}