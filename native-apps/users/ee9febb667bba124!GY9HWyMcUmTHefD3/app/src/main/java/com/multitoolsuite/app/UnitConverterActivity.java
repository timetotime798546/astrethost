package com.multitoolsuite.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class UnitConverterActivity extends Activity {
    private EditText etInput;
    private TextView tvOutput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unit_converter);

        etInput = (EditText) findViewById(R.id.et_converter_input);
        tvOutput = (TextView) findViewById(R.id.tv_converter_result);

        findViewById(R.id.btn_km_to_mi).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                double val = getInputVal();
                if (val != -1) {
                    double result = val * 0.621371;
                    tvOutput.setText(String.format("%.2f Kilometers = %.2f Miles", val, result));
                }
            }
        });

        findViewById(R.id.btn_mi_to_km).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                double val = getInputVal();
                if (val != -1) {
                    double result = val * 1.60934;
                    tvOutput.setText(String.format("%.2f Miles = %.2f Kilometers", val, result));
                }
            }
        });

        findViewById(R.id.btn_c_to_f).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                double val = getInputVal();
                if (val != -1) {
                    double result = (val * 9 / 5) + 32;
                    tvOutput.setText(String.format("%.1f °C = %.1f °F", val, result));
                }
            }
        });

        findViewById(R.id.btn_f_to_c).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                double val = getInputVal();
                if (val != -1) {
                    double result = (val - 32) * 5 / 9;
                    tvOutput.setText(String.format("%.1f °F = %.1f °C", val, result));
                }
            }
        });
    }

    private double getInputVal() {
        String sText = etInput.getText().toString();
        if (sText.isEmpty()) {
            Toast.makeText(this, "Please enter a value to convert", Toast.LENGTH_SHORT).show();
            return -1;
        }
        try {
            return Double.parseDouble(sText);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid number entered", Toast.LENGTH_SHORT).show();
            return -1;
        }
    }
}
