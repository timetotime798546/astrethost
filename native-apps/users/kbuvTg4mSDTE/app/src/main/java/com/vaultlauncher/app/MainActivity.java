package com.vaultlauncher.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements View.OnClickListener {

    private TextView tvDisplay;
    private TextView tvFormula;
    private StringBuilder currentInput;
    private double firstOperand = Double.NaN;
    private char pendingOperator = ' ';
    private static final String DEFAULT_PIN = "1234";

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_main);

        currentInput = new StringBuilder();
        tvDisplay = (TextView) findViewById(R.id.tv_display);
        tvFormula = (TextView) findViewById(R.id.tv_formula);

        // Bind digital buttons
        int[] buttonIds = {
            R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4,
            R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9,
            R.id.btn_dot, R.id.btn_clear, R.id.btn_back, R.id.btn_equals,
            R.id.btn_op_add, R.id.btn_op_sub, R.id.btn_op_mul, R.id.btn_op_div
        };

        for (int id : buttonIds) {
            findViewById(id).setOnClickListener(this);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.btn_0) appendChar("0");
        else if (id == R.id.btn_1) appendChar("1");
        else if (id == R.id.btn_2) appendChar("2");
        else if (id == R.id.btn_3) appendChar("3");
        else if (id == R.id.btn_4) appendChar("4");
        else if (id == R.id.btn_5) appendChar("5");
        else if (id == R.id.btn_6) appendChar("6");
        else if (id == R.id.btn_7) appendChar("7");
        else if (id == R.id.btn_8) appendChar("8");
        else if (id == R.id.btn_9) appendChar("9");
        else if (id == R.id.btn_dot) appendChar(".");
        else if (id == R.id.btn_clear) clearScreen();
        else if (id == R.id.btn_back) backspace();
        else if (id == R.id.btn_op_add) selectOperator('+');
        else if (id == R.id.btn_op_sub) selectOperator('-');
        else if (id == R.id.btn_op_mul) selectOperator('*');
        else if (id == R.id.btn_op_div) selectOperator('/');
        else if (id == R.id.btn_equals) triggerEquals();
    }

    private void appendChar(String token) {
        if (currentInput.toString().equals("0")) {
            currentInput.setLength(0);
        }
        currentInput.append(token);
        tvDisplay.setText(currentInput.toString());
    }

    private void clearScreen() {
        currentInput.setLength(0);
        firstOperand = Double.NaN;
        pendingOperator = ' ';
        tvDisplay.setText("0");
        tvFormula.setText("");
    }

    private void backspace() {
        if (currentInput.length() > 0) {
            currentInput.deleteCharAt(currentInput.length() - 1);
            tvDisplay.setText(currentInput.length() == 0 ? "0" : currentInput.toString());
        }
    }

    private void selectOperator(char operator) {
        if (currentInput.length() > 0) {
            try {
                firstOperand = Double.parseDouble(currentInput.toString());
                pendingOperator = operator;
                tvFormula.setText(currentInput.toString() + " " + operator);
                currentInput.setLength(0);
            } catch (NumberFormatException ignored) {}
        }
    }

    private void triggerEquals() {
        String finalValue = currentInput.toString();

        // Safety Vault Decoy Check Rule: Check if sequence exactly matches Vault unlock passcode
        SharedPreferences prefs = getSharedPreferences("VaultPrefs", MODE_PRIVATE);
        String customPin = prefs.getString("vault_pin", DEFAULT_PIN);

        if (finalValue.equals(customPin)) {
            openVaultFlow();
            clearScreen();
            return;
        }

        // Standard Decoy Calculation logic executed locally on the client interface
        if (!Double.isNaN(firstOperand) && currentInput.length() > 0) {
            try {
                double secondOperand = Double.parseDouble(finalValue);
                double result = 0;
                boolean validCalculation = true;

                switch (pendingOperator) {
                    case '+':
                        result = firstOperand + secondOperand;
                        break;
                    case '-':
                        result = firstOperand - secondOperand;
                        break;
                    case '*':
                        result = firstOperand * secondOperand;
                        break;
                    case '/':
                        if (secondOperand != 0) {
                            result = firstOperand / secondOperand;
                        } else {
                            validCalculation = false;
                        }
                        break;
                    default:
                        validCalculation = false;
                        break;
                }

                if (validCalculation) {
                    tvFormula.setText(firstOperand + " " + pendingOperator + " " + secondOperand + " =");
                    String resStr = String.valueOf(result);
                    if (resStr.endsWith(".0")) {
                        resStr = resStr.substring(0, resStr.length() - 2);
                    }
                    tvDisplay.setText(resStr);
                    currentInput.setLength(0);
                    currentInput.append(resStr);
                    firstOperand = Double.NaN;
                    pendingOperator = ' ';
                } else {
                    tvDisplay.setText("Error");
                }
            } catch (Exception ignored) {
                tvDisplay.setText("Error");
            }
        }
    }

    private void openVaultFlow() {
        Toast.makeText(this, "Vault Unlocked!", Toast.LENGTH_SHORT).show();
        SharedPreferences prefs = getSharedPreferences("VaultPrefs", MODE_PRIVATE);
        boolean setupComplete = prefs.getBoolean("vault_setup", false);

        if (setupComplete) {
            startActivity(new Intent(this, VaultActivity.class));
        } else {
            startActivity(new Intent(this, LoginActivity.class));
        }
    }
}