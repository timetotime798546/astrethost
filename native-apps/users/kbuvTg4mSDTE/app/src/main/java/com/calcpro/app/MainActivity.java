package com.calcpro.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    private TextView tvExpression;
    private TextView tvResult;
    private LinearLayout layoutScientific;
    private Button btnToggleSci;

    private boolean isScientificVisible = false;
    private boolean isNewCalculation = true;
    private String currentExpression = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupKeypadListeners();
    }

    private void initViews() {
        tvExpression = (TextView) findViewById(R.id.tvExpression);
        tvResult = (TextView) findViewById(R.id.tvResult);
        layoutScientific = (LinearLayout) findViewById(R.id.layoutScientific);
        btnToggleSci = (Button) findViewById(R.id.btnToggleSci);

        btnToggleSci.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isScientificVisible = !isScientificVisible;
                if (isScientificVisible) {
                    layoutScientific.setVisibility(View.VISIBLE);
                    btnToggleSci.setText("Standard");
                } else {
                    layoutScientific.setVisibility(View.GONE);
                    btnToggleSci.setText("Scientific");
                }
            }
        });
    }

    private void setupKeypadListeners() {
        int[] numberButtonIds = {
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9, R.id.btnDot
        };

        View.OnClickListener numberClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button b = (Button) v;
                String val = b.getText().toString();

                if (isNewCalculation) {
                    currentExpression = val;
                    isNewCalculation = false;
                } else {
                    currentExpression += val;
                }
                tvExpression.setText(currentExpression);
                updateLiveResult();
            }
        };

        for (int id : numberButtonIds) {
            findViewById(id).setOnClickListener(numberClickListener);
        }

        int[] operatorButtonIds = {
            R.id.btnPlus, R.id.btnMinus, R.id.btnMultiply, R.id.btnDivide, R.id.btnPow
        };

        View.OnClickListener operatorClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button b = (Button) v;
                String val = b.getText().toString();

                if (isNewCalculation) {
                    currentExpression = tvResult.getText().toString() + val;
                    isNewCalculation = false;
                } else {
                    if (!currentExpression.isEmpty()) {
                        char last = currentExpression.charAt(currentExpression.length() - 1);
                        if (last == '+' || last == '-' || last == '×' || last == '÷' || last == '^') {
                            currentExpression = currentExpression.substring(0, currentExpression.length() - 1) + val;
                        } else {
                            currentExpression += val;
                        }
                    } else {
                        currentExpression = "0" + val;
                    }
                }
                tvExpression.setText(currentExpression);
            }
        };

        for (int id : operatorButtonIds) {
            findViewById(id).setOnClickListener(operatorClickListener);
        }

        findViewById(R.id.btnClear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentExpression = "";
                tvExpression.setText("");
                tvResult.setText("0");
                isNewCalculation = true;
            }
        });

        findViewById(R.id.btnBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!currentExpression.isEmpty()) {
                    currentExpression = currentExpression.substring(0, currentExpression.length() - 1);
                    tvExpression.setText(currentExpression);
                    if (currentExpression.isEmpty()) {
                        tvResult.setText("0");
                        isNewCalculation = true;
                    } else {
                        updateLiveResult();
                    }
                }
            }
        });

        findViewById(R.id.btnPercent).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!currentExpression.isEmpty() && !isNewCalculation) {
                    try {
                        double val = Double.parseDouble(CalculatorEngine.evaluate(currentExpression));
                        val = val / 100.0;
                        currentExpression = String.valueOf(val);
                        tvExpression.setText(currentExpression);
                        tvResult.setText(currentExpression);
                    } catch (Exception e) {
                        tvResult.setText("Error");
                    }
                }
            }
        });

        findViewById(R.id.btnEquals).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!currentExpression.isEmpty()) {
                    String res = CalculatorEngine.evaluate(currentExpression);
                    tvResult.setText(res);
                    currentExpression = res;
                    isNewCalculation = true;
                }
            }
        });

        // Scientific functions
        View.OnClickListener sciClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button b = (Button) v;
                String func = b.getText().toString();

                if (func.equals("√")) {
                    currentExpression += "sqrt(";
                } else {
                    currentExpression += func + "(";
                }
                tvExpression.setText(currentExpression);
                isNewCalculation = false;
            }
        };

        findViewById(R.id.btnSin).setOnClickListener(sciClickListener);
        findViewById(R.id.btnCos).setOnClickListener(sciClickListener);
        findViewById(R.id.btnTan).setOnClickListener(sciClickListener);
        findViewById(R.id.btnSqrt).setOnClickListener(sciClickListener);
        findViewById(R.id.btnPow).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentExpression += "^";
                tvExpression.setText(currentExpression);
                isNewCalculation = false;
            }
        });
    }

    private void updateLiveResult() {
        try {
            String res = CalculatorEngine.evaluate(currentExpression);
            if (!res.equals("Error")) {
                tvResult.setText(res);
            }
        } catch (Exception e) {
            // Ignore intermediate expression evaluation errors
        }
    }
}