package com.multiutilitysuite.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class CalculatorActivity extends Activity {
    private TextView display;
    private String currentInput = "";
    private double firstValue = Double.NaN;
    private String currentOp = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout parent = new LinearLayout(this);
        parent.setOrientation(LinearLayout.VERTICAL);
        parent.setBackgroundColor(Color.parseColor("#F5F5F5"));

        // Custom Header Bar
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setBackgroundColor(Color.parseColor("#3F51B5"));
        header.setPadding(32, 24, 32, 24);
        header.setGravity(Gravity.CENTER_VERTICAL);

        Button backButton = new Button(this);
        backButton.setText("< Back");
        backButton.setTextColor(Color.WHITE);
        backButton.setBackgroundColor(Color.TRANSPARENT);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        header.addView(backButton);

        TextView headerTitle = new TextView(this);
        headerTitle.setText("Calculator Tool");
        headerTitle.setTextColor(Color.WHITE);
        headerTitle.setTextSize(20);
        headerTitle.setPadding(32, 0, 0, 0);
        header.addView(headerTitle);
        parent.addView(header);

        // Display
        display = new TextView(this);
        display.setText("0");
        display.setTextSize(40);
        display.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        display.setPadding(32, 48, 32, 48);
        display.setBackgroundColor(Color.parseColor("#E0E0E0"));
        display.setTextColor(Color.BLACK);
        parent.addView(display);

        // Keypad Matrix Rows
        String[][] buttons = {
            {"7", "8", "9", "/"},
            {"4", "5", "6", "*"},
            {"1", "2", "3", "-"},
            {"C", "0", "=", "+"}
        };

        for (int r = 0; r < 4; r++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f);
            row.setLayoutParams(rowParams);

            for (int c = 0; c < 4; c++) {
                final String text = buttons[r][c];
                Button btn = new Button(this);
                btn.setText(text);
                btn.setTextSize(22);
                LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.MATCH_PARENT, 1.0f);
                btnParams.setMargins(4, 4, 4, 4);
                btn.setLayoutParams(btnParams);

                if ("/\"*-+=".contains(text)) {
                    btn.setBackgroundColor(Color.parseColor("#FF9800"));
                    btn.setTextColor(Color.WHITE);
                } else if ("C".equals(text)) {
                    btn.setBackgroundColor(Color.parseColor("#F44336"));
                    btn.setTextColor(Color.WHITE);
                } else {
                    btn.setBackgroundColor(Color.parseColor("#EEEEEE"));
                    btn.setTextColor(Color.BLACK);
                }

                btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        handleKeypress(text);
                    }
                });
                row.addView(btn);
            }
            parent.addView(row);
        }

        setContentView(parent);
    }

    private void handleKeypress(String key) {
        if ("C".equals(key)) {
            currentInput = "";
            firstValue = Double.NaN;
            currentOp = "";
            display.setText("0");
        } else if ("=".equals(key)) {
            calculate();
            currentOp = "";
        } else if ("+".equals(key) || "-".equals(key) || "*".equals(key) || "/".equals(key)) {
            if (!currentInput.isEmpty()) {
                firstValue = Double.parseDouble(currentInput);
                currentOp = key;
                currentInput = "";
            }
        } else {
            currentInput += key;
            display.setText(currentInput);
        }
    }

    private void calculate() {
        if (!Double.isNaN(firstValue) && !currentInput.isEmpty() && !currentOp.isEmpty()) {
            double secondValue = Double.parseDouble(currentInput);
            double result = 0;
            if ("+".equals(currentOp)) result = firstValue + secondValue;
            else if ("-".equals(currentOp)) result = firstValue - secondValue;
            else if ("*".equals(currentOp)) result = firstValue * secondValue;
            else if ("/".equals(currentOp)) {
                if (secondValue != 0) {
                    result = firstValue / secondValue;
                } else {
                    display.setText("Error");
                    return;
                }
            }
            String resultStr = String.valueOf(result);
            if (resultStr.endsWith(".0")) {
                resultStr = resultStr.substring(0, resultStr.length() - 2);
            }
            display.setText(resultStr);
            currentInput = resultStr;
            firstValue = Double.NaN;
        }
    }
}