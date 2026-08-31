package com.omnitoolspremium.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class BmiActivity extends Activity {

    private EditText editHeight;
    private EditText editWeight;
    private LinearLayout layoutResult;
    private TextView txtBmiNum;
    private TextView txtBmiStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bmi);

        editHeight = (EditText) findViewById(R.id.edit_bmi_height);
        editWeight = (EditText) findViewById(R.id.edit_bmi_weight);
        layoutResult = (LinearLayout) findViewById(R.id.layout_bmi_result);
        txtBmiNum = (TextView) findViewById(R.id.txt_bmi_num);
        txtBmiStatus = (TextView) findViewById(R.id.txt_bmi_status);
        Button btnCalc = (Button) findViewById(R.id.btn_calc_bmi);

        btnCalc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                computeBmi();
            }
        });
    }

    private void computeBmi() {
        String heightStr = editHeight.getText().toString().trim();
        String weightStr = editWeight.getText().toString().trim();

        if (heightStr.isEmpty() || weightStr.isEmpty()) {
            Toast.makeText(this, "Height and weight cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        double heightCm, weightKg;
        try {
            heightCm = Double.parseDouble(heightStr);
            weightKg = Double.parseDouble(weightStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid measurements inputs", Toast.LENGTH_SHORT).show();
            return;
        }

        if (heightCm <= 0 || weightKg <= 0) {
            Toast.makeText(this, "Values must be greater than 0", Toast.LENGTH_SHORT).show();
            return;
        }

        double heightMeters = heightCm / 100.0;
        double bmi = weightKg / (heightMeters * heightMeters);

        txtBmiNum.setText(String.format("%.1f", bmi));
        
        if (bmi < 18.5) {
            txtBmiStatus.setText("UNDERWEIGHT");
            txtBmiStatus.setTextColor(0xFF3498DB);
        } else if (bmi >= 18.5 && bmi < 25.0) {
            txtBmiStatus.setText("HEALTHY PROPORTION");
            txtBmiStatus.setTextColor(0xFF2ECC71);
        } else if (bmi >= 25.0 && bmi < 30.0) {
            txtBmiStatus.setText("OVERWEIGHT");
            txtBmiStatus.setTextColor(0xFFF1C40F);
        } else {
            txtBmiStatus.setText("OBESE RANGE");
            txtBmiStatus.setTextColor(0xFFE74C3C);
        }

        layoutResult.setVisibility(View.VISIBLE);
    }
}