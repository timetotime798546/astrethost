package com.omnicontrolsuite.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Locale;

public class CalculatorActivity extends Activity implements View.OnClickListener {

    private TextView txtExpression;
    private TextView txtResult;
    private EditText editMetricValue;
    private TextView txtMetricResult;

    private String currentInput = "";
    private String lastOperator = "";
    private double storedValue = 0;
    private boolean isOperatorJustPressed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calculator);

        txtExpression = (TextView) findViewById(R.id.txtCalcExpression);
        txtResult = (TextView) findViewById(R.id.txtCalcResult);
        editMetricValue = (EditText) findViewById(R.id.editMetricValue);
        txtMetricResult = (TextView) findViewById(R.id.txtMetricResult);

        Button btnBack = (Button) findViewById(R.id.btnBack);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Calculator grid bindings
        int[] ids = {R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4, R.id.btn5,
                R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9, R.id.btnPlus, R.id.btnMinus,
                R.id.btnMult, R.id.btnDiv, R.id.btnC, R.id.btnEquals};
        for (int id : ids) {
            findViewById(id).setOnClickListener(this);
        }

        // Unit conversion utility
        Button btnCelToFahr = (Button) findViewById(R.id.btnCelsiusToFahr);
        Button btnFahrToCel = (Button) findViewById(R.id.btnFahrToCelsius);

        btnCelToFahr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    double value = Double.parseDouble(editMetricValue.getText().toString());
                    double res = (value * 9 / 5) + 32;
                    txtMetricResult.setText(String.format(Locale.getDefault(), "Result: %.2f °F", res));
                } catch (Exception ex) {
                    Toast.makeText(CalculatorActivity.this, "Enter numeric input metric", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnFahrToCel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    double value = Double.parseDouble(editMetricValue.getText().toString());
                    double res = (value - 32) * 5 / 9;
                    txtMetricResult.setText(String.format(Locale.getDefault(), "Result: %.2f °C", res));
                } catch (Exception ex) {
                    Toast.makeText(CalculatorActivity.this, "Enter numeric input metric", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn0) appendDigit("0");
        else if (id == R.id.btn1) appendDigit("1");
        else if (id == R.id.btn2) appendDigit("2");
        else if (id == R.id.btn3) appendDigit("3");
        else if (id == R.id.btn4) appendDigit("4");
        else if (id == R.id.btn5) appendDigit("5");
        else if (id == R.id.btn6) appendDigit("6");
        else if (id == R.id.btn7) appendDigit("7");
        else if (id == R.id.btn8) appendDigit("8");
        else if (id == R.id.btn9) appendDigit("9");
        else if (id == R.id.btnC) clearCalculator();
        else if (id == R.id.btnPlus) setOperator("+");
        else if (id == R.id.btnMinus) setOperator("-");
        else if (id == R.id.btnMult) setOperator("*");
        else if (id == R.id.btnDiv) setOperator("/");
        else if (id == R.id.btnEquals) evaluateResult();
    }

    private void appendDigit(String digit) {
        if (isOperatorJustPressed) {
            currentInput = "";
            isOperatorJustPressed = false;
        }
        currentInput += digit;
        txtResult.setText(currentInput);
    }

    private void clearCalculator() {
        currentInput = "";
        lastOperator = "";
        storedValue = 0;
        isOperatorJustPressed = false;
        txtExpression.setText("");
        txtResult.setText("0");
    }

    private void setOperator(String op) {
        if (!currentInput.isEmpty()) {
            storedValue = Double.parseDouble(currentInput);
        }
        lastOperator = op;
        txtExpression.setText(storedValue + " " + op);
        isOperatorJustPressed = true;
    }

    private void evaluateResult() {
        if (lastOperator.isEmpty() || currentInput.isEmpty()) return;
        double secondVal = Double.parseDouble(currentInput);
        double calculated = 0;

        if (lastOperator.equals("+")) calculated = storedValue + secondVal;
        else if (lastOperator.equals("-")) calculated = storedValue - secondVal;
        else if (lastOperator.equals("*")) calculated = storedValue * secondVal;
        else if (lastOperator.equals("/")) {
            if (secondVal != 0) calculated = storedValue / secondVal;
            else {
                txtResult.setText("ERR");
                return;
            }
        }

        txtExpression.setText("");
        txtResult.setText(String.valueOf(calculated));
        currentInput = String.valueOf(calculated);
        lastOperator = "";
    }
}