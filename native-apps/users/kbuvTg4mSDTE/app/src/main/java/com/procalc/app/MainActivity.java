package com.procalc.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends Activity {
    private EditText display;
    private double val1 = 0;
    private String op = "";
    private boolean isNewOp = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        display = findViewById(R.id.display);
    }

    public void onNumClick(View v) {
        String num = ((Button) v).getText().toString();
        if (isNewOp) {
            display.setText(num);
            isNewOp = false;
        } else {
            String current = display.getText().toString();
            if (current.equals("0") && !num.equals(".")) {
                display.setText(num);
            } else {
                display.append(num);
            }
        }
    }

    public void onOpClick(View v) {
        try {
            val1 = Double.parseDouble(display.getText().toString());
            op = ((Button) v).getText().toString();
            isNewOp = true;
        } catch (NumberFormatException e) {}
    }

    public void onClear(View v) {
        display.setText("0");
        val1 = 0;
        op = "";
        isNewOp = true;
    }

    public void onEquals(View v) {
        try {
            double val2 = Double.parseDouble(display.getText().toString());
            double result = 0;
            switch (op) {
                case "+": result = val1 + val2; break;
                case "-": result = val1 - val2; break;
                case "*": result = val1 * val2; break;
                case "/": result = (val2 != 0) ? (val1 / val2) : 0; break;
                default: return;
            }
            display.setText(result % 1 == 0 ? String.valueOf((long)result) : String.valueOf(result));
            isNewOp = true;
        } catch (Exception e) {}
    }
}