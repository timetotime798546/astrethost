package com.multiutilitysuite.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class BmiCalculatorActivity extends Activity {
    private EditText editHeight, editWeight;
    private TextView textResult, textMessage;

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
        headerTitle.setText("BMI Calculator");
        headerTitle.setTextColor(Color.WHITE);
        headerTitle.setTextSize(20);
        headerTitle.setPadding(32, 0, 0, 0);
        header.addView(headerTitle);
        parent.addView(header);

        // Main Body Panel
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(32, 32, 32, 32);

        TextView heightLabel = new TextView(this);
        heightLabel.setText("Your Height (in cm):");
        heightLabel.setTextSize(16);
        body.addView(heightLabel);

        editHeight = new EditText(this);
        editHeight.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        body.addView(editHeight);

        TextView weightLabel = new TextView(this);
        weightLabel.setText("Your Weight (in kg):");
        weightLabel.setTextSize(16);
        weightLabel.setPadding(0, 16, 0, 0);
        body.addView(weightLabel);

        editWeight = new EditText(this);
        editWeight.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        body.addView(editWeight);

        Button calculateBtn = new Button(this);
        calculateBtn.setText("CALCULATE BMI");
        calculateBtn.setBackgroundColor(Color.parseColor("#3F51B5"));
        calculateBtn.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        btnParams.setMargins(0, 32, 0, 32);
        calculateBtn.setLayoutParams(btnParams);
        calculateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calculateBmiMetric();
            }
        });
        body.addView(calculateBtn);

        textResult = new TextView(this);
        textResult.setText("BMI: 0.0");
        textResult.setTextSize(24);
        textResult.setTextColor(Color.BLACK);
        textResult.setGravity(Gravity.CENTER);
        body.addView(textResult);

        textMessage = new TextView(this);
        textMessage.setText("Please input measurement entries");
        textMessage.setTextSize(16);
        textMessage.setPadding(0, 16, 0, 0);
        textMessage.setGravity(Gravity.CENTER);
        body.addView(textMessage);

        parent.addView(body);
        setContentView(parent);
    }

    private void calculateBmiMetric() {
        String rawHeight = editHeight.getText().toString();
        String rawWeight = editWeight.getText().toString();

        if (rawHeight.isEmpty() || rawWeight.isEmpty()) {
            textMessage.setText("Error: Empty values!");
            return;
        }

        float height = Float.parseFloat(rawHeight) / 100.0f; // Convert meters
        float weight = Float.parseFloat(rawWeight);

        if (height <= 0 || weight <= 0) {
            textMessage.setText("Error: Positive parameters needed!");
            return;
        }

        float bmiValue = weight / (height * height);
        textResult.setText(String.format("BMI: %.2f", bmiValue));

        String category;
        int categoryColor;

        if (bmiValue < 18.5) {
            category = "Underweight";
            categoryColor = Color.parseColor("#03A9F4");
        } else if (bmiValue < 25) {
            category = "Normal Weight";
            categoryColor = Color.parseColor("#4CAF50");
        } else if (bmiValue < 30) {
            category = "Overweight";
            categoryColor = Color.parseColor("#FF9800");
        } else {
            category = "Obese";
            categoryColor = Color.parseColor("#F44336");
        }

        textMessage.setText(category);
        textMessage.setTextColor(categoryColor);
    }
}