package com.multiutilitysuite.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

public class UnitConverterActivity extends Activity {
    private EditText inputField;
    private RadioGroup selectionGroup;
    private TextView resultView;

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
        headerTitle.setText("Unit Converter");
        headerTitle.setTextColor(Color.WHITE);
        headerTitle.setTextSize(20);
        headerTitle.setPadding(32, 0, 0, 0);
        header.addView(headerTitle);
        parent.addView(header);

        // Input Body Layout
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(32, 32, 32, 32);

        TextView promptLabel = new TextView(this);
        promptLabel.setText("Enter value to convert:");
        promptLabel.setTextSize(16);
        body.addView(promptLabel);

        inputField = new EditText(this);
        inputField.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        body.addView(inputField);

        TextView optionLabel = new TextView(this);
        optionLabel.setText("Select Mode:");
        optionLabel.setTextSize(16);
        optionLabel.setPadding(0, 32, 0, 16);
        body.addView(optionLabel);

        selectionGroup = new RadioGroup(this);
        
        RadioButton cmToInches = new RadioButton(this);
        cmToInches.setId(View.generateViewId());
        cmToInches.setText("Centimeters to Inches");
        selectionGroup.addView(cmToInches);

        RadioButton inchesToCm = new RadioButton(this);
        inchesToCm.setId(View.generateViewId());
        inchesToCm.setText("Inches to Centimeters");
        selectionGroup.addView(inchesToCm);

        RadioButton kgToLbs = new RadioButton(this);
        kgToLbs.setId(View.generateViewId());
        kgToLbs.setText("Kilograms to Pounds");
        selectionGroup.addView(kgToLbs);

        RadioButton lbsToKg = new RadioButton(this);
        lbsToKg.setId(View.generateViewId());
        lbsToKg.setText("Pounds to Kilograms");
        selectionGroup.addView(lbsToKg);

        selectionGroup.check(cmToInches.getId());
        body.addView(selectionGroup);

        Button convertBtn = new Button(this);
        convertBtn.setText("CONVERT");
        convertBtn.setBackgroundColor(Color.parseColor("#3F51B5"));
        convertBtn.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        btnParams.setMargins(0, 32, 0, 32);
        convertBtn.setLayoutParams(btnParams);
        convertBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performConversion();
            }
        });
        body.addView(convertBtn);

        resultView = new TextView(this);
        resultView.setText("Result: 0.0");
        resultView.setTextSize(24);
        resultView.setGravity(Gravity.CENTER);
        resultView.setTextColor(Color.BLACK);
        body.addView(resultView);

        parent.addView(body);
        setContentView(parent);
    }

    private void performConversion() {
        String rawText = inputField.getText().toString();
        if (rawText.isEmpty()) {
            resultView.setText("Please enter a value");
            return;
        }

        double val = Double.parseDouble(rawText);
        int checkedId = selectionGroup.getCheckedRadioButtonId();
        double conversion = 0.0;
        String textSuffix = "";

        // Get dynamic children items indices
        int idx = selectionGroup.indexOfChild(findViewById(checkedId));
        switch (idx) {
            case 0:
                conversion = val * 0.393701;
                textSuffix = " in";
                break;
            case 1:
                conversion = val * 2.54;
                textSuffix = " cm";
                break;
            case 2:
                conversion = val * 2.20462;
                textSuffix = " lbs";
                break;
            case 3:
                conversion = val * 0.453592;
                textSuffix = " kg";
                break;
        }

        resultView.setText(String.format("Result: %.3f", conversion) + textSuffix);
    }
}