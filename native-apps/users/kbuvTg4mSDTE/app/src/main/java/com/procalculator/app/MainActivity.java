package com.procalculator.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private TextView tvDisplay;
    private String currentInput = "";
    private double operand1 = Double.NaN;
    private String operator = null;
    private SoundManager soundManager;
    private BackendApi api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvDisplay = (TextView) findViewById(R.id.tvDisplay);
        soundManager = new SoundManager();
        api = new BackendApi(this);

        setupButtons();
    }

    private void setupButtons() {
        int[] ids = {
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4, 
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9,
            R.id.btnDot, R.id.btnAdd, R.id.btnSub, R.id.btnMul, R.id.btnDiv,
            R.id.btnC, R.id.btnDel, R.id.btnEqual
        };

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                soundManager.playClick();
                handleInput(((Button) v).getText().toString());
            }
        };

        for (int id : ids) {
            findViewById(id).setOnClickListener(listener);
        }

        findViewById(R.id.btnTheme).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ThemeManager.toggleTheme(MainActivity.this);
            }
        });

        findViewById(R.id.btnHistory).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, HistoryActivity.class));
            }
        });
    }

    private void handleInput(String input) {
        if (input.length() == 1 && Character.isDigit(input.charAt(0)) || ".".equals(input)) {
            currentInput += input;
            tvDisplay.setText(currentInput);
        } else if ("C".equals(input)) {
            currentInput = "";
            operand1 = Double.NaN;
            operator = null;
            tvDisplay.setText("0");
        } else if ("DEL".equals(input)) {
            if (currentInput.length() > 0) {
                currentInput = currentInput.substring(0, currentInput.length() - 1);
                tvDisplay.setText(currentInput.isEmpty() ? "0" : currentInput);
            }
        } else if ("=".equals(input)) {
            calculate();
        } else {
            if (!currentInput.isEmpty()) {
                operand1 = Double.parseDouble(currentInput);
                operator = input;
                currentInput = "";
            }
        }
    }

    private void calculate() {
        if (Double.isNaN(operand1) || operator == null || currentInput.isEmpty()) return;

        double operand2 = Double.parseDouble(currentInput);
        double result = 0;

        if ("+".equals(operator)) result = operand1 + operand2;
        else if ("-".equals(operator)) result = operand1 - operand2;
        else if ("*".equals(operator)) result = operand1 * operand2;
        else if ("/".equals(operator)) {
            if (operand2 != 0) result = operand1 / operand2;
            else {
                Toast.makeText(this, "Cannot divide by zero", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        final String expression = operand1 + " " + operator + " " + operand2;
        final String resultStr = String.valueOf(result);
        
        tvDisplay.setText(resultStr);
        
        api.saveHistory(expression, resultStr, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject res) {}
            @Override
            public void onError(String msg) {}
        });

        currentInput = resultStr;
        operand1 = Double.NaN;
        operator = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        soundManager.release();
    }
}