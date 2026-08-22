package com.multitoolsuite.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class BmiActivity extends Activity {
    private EditText etHeight, etWeight;
    private TextView tvResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bmi);

        etHeight = (EditText) findViewById(R.id.et_height);
        etWeight = (EditText) findViewById(R.id.et_weight);
        tvResult = (TextView) findViewById(R.id.tv_bmi_result);

        findViewById(R.id.btn_calculate_bmi).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calculateBmi();
            }
        });
    }

    private void calculateBmi() {
        String hStr = etHeight.getText().toString();
        String wStr = etWeight.getText().toString();

        if (hStr.isEmpty() || wStr.isEmpty()) {
            Toast.makeText(this, "Please enter both weight and height", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double height = Double.parseDouble(hStr) / 100.0;
            double weight = Double.parseDouble(wStr);

            if (height <= 0 || weight <= 0) {
                Toast.makeText(this, "Please enter numbers greater than 0", Toast.LENGTH_SHORT).show();
                return;
            }

            double bmi = weight / (height * height);
            String category;

            if (bmi < 18.5) {
                category = "Underweight";
            } else if (bmi < 24.9) {
                category = "Normal Weight";
            } else if (bmi < 29.9) {
                category = "Overweight";
            } else {
                category = "Obese";
            }

            tvResult.setText(String.format("Your BMI: %.2f\nCategory: %s", bmi, category));
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid parameters entered", Toast.LENGTH_SHORT).show();
        }
    }
}
