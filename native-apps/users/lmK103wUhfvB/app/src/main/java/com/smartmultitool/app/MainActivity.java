package com.smartmultitool.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Locale;

public class MainActivity extends Activity {

    // BMI Components
    private EditText etBmiWeight;
    private EditText etBmiHeight;
    private Button btnCalculateBmi;
    private TextView tvBmiResult;

    // Tip Components
    private EditText etTipBill;
    private EditText etTipPercent;
    private EditText etTipPeople;
    private Button btnCalculateTip;
    private TextView tvTipResult;

    // Conversion Components
    private RadioGroup rgConversionType;
    private EditText etConvertInput;
    private Button btnConvert;
    private TextView tvConvertResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize BMI Components
        etBmiWeight = (EditText) findViewById(R.id.et_bmi_weight);
        etBmiHeight = (EditText) findViewById(R.id.et_bmi_height);
        btnCalculateBmi = (Button) findViewById(R.id.btn_calculate_bmi);
        tvBmiResult = (TextView) findViewById(R.id.tv_bmi_result);

        // Initialize Tip Components
        etTipBill = (EditText) findViewById(R.id.et_tip_bill);
        etTipPercent = (EditText) findViewById(R.id.et_tip_percent);
        etTipPeople = (EditText) findViewById(R.id.et_tip_people);
        btnCalculateTip = (Button) findViewById(R.id.btn_calculate_tip);
        tvTipResult = (TextView) findViewById(R.id.tv_tip_result);

        // Initialize Conversion Components
        rgConversionType = (RadioGroup) findViewById(R.id.rg_conversion_type);
        etConvertInput = (EditText) findViewById(R.id.et_convert_input);
        btnConvert = (Button) findViewById(R.id.btn_convert);
        tvConvertResult = (TextView) findViewById(R.id.tv_convert_result);

        // Set BMI Button Listener using anonymous inner class for Java 8 compatibility
        btnCalculateBmi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calculateBmi();
            }
        });

        // Set Tip Button Listener using anonymous inner class for Java 8 compatibility
        btnCalculateTip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calculateTip();
            }
        });

        // Set Convert Button Listener using anonymous inner class for Java 8 compatibility
        btnConvert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performConversion();
            }
        });
    }

    private void calculateBmi() {
        String weightStr = etBmiWeight.getText().toString().trim();
        String heightStr = etBmiHeight.getText().toString().trim();

        if (weightStr.isEmpty() || heightStr.isEmpty()) {
            Toast.makeText(this, "Please enter both weight and height", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double weight = Double.parseDouble(weightStr);
            double height = Double.parseDouble(heightStr);

            if (weight <= 0 || height <= 0) {
                Toast.makeText(this, "Values must be greater than zero", Toast.LENGTH_SHORT).show();
                return;
            }

            // Standard BMI Formula: weight (kg) / (height (m) ^ 2)
            double heightInMeters = height / 100.0;
            double bmi = weight / (heightInMeters * heightInMeters);

            String status;
            if (bmi < 18.5) {
                status = "Underweight";
            } else if (bmi < 25.0) {
                status = "Normal weight";
            } else if (bmi < 30.0) {
                status = "Overweight";
            } else {
                status = "Obese";
            }

            String resultText = String.format(Locale.US, "BMI: %.2f\nStatus: %s", bmi, status);
            tvBmiResult.setText(resultText);
            tvBmiResult.setVisibility(View.VISIBLE);

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid decimal numbers in input", Toast.LENGTH_SHORT).show();
        }
    }

    private void calculateTip() {
        String billStr = etTipBill.getText().toString().trim();
        String percentStr = etTipPercent.getText().toString().trim();
        String peopleStr = etTipPeople.getText().toString().trim();

        if (billStr.isEmpty() || percentStr.isEmpty() || peopleStr.isEmpty()) {
            Toast.makeText(this, "Please fill in all tip fields", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double bill = Double.parseDouble(billStr);
            double percent = Double.parseDouble(percentStr);
            int people = Integer.parseInt(peopleStr);

            if (bill < 0 || percent < 0 || people <= 0) {
                Toast.makeText(this, "Please enter valid arithmetic parameters", Toast.LENGTH_SHORT).show();
                return;
            }

            double tipAmount = bill * (percent / 100.0);
            double totalAmount = bill + tipAmount;
            double tipPerPerson = tipAmount / people;
            double totalPerPerson = totalAmount / people;

            String resultText = String.format(Locale.US,
                    "Total Tip: $%.2f\nTotal Bill: $%.2f\nTip Per Person: $%.2f\nTotal Per Person: $%.2f",
                    tipAmount, totalAmount, tipPerPerson, totalPerPerson);

            tvTipResult.setText(resultText);
            tvTipResult.setVisibility(View.VISIBLE);

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid digit configurations parsed", Toast.LENGTH_SHORT).show();
        }
    }

    private void performConversion() {
        String inputStr = etConvertInput.getText().toString().trim();

        if (inputStr.isEmpty()) {
            Toast.makeText(this, "Please provide a value to translate", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double inputVal = Double.parseDouble(inputStr);
            double outputVal = 0.0;
            String fromUnit = "";
            String toUnit = "";

            int checkedId = rgConversionType.getCheckedRadioButtonId();
            if (checkedId == R.id.rb_c_to_f) {
                outputVal = inputVal * 9.0 / 5.0 + 32.0;
                fromUnit = "°C";
                toUnit = "°F";
            } else if (checkedId == R.id.rb_f_to_c) {
                outputVal = (inputVal - 32.0) * 5.0 / 9.0;
                fromUnit = "°F";
                toUnit = "°C";
            } else if (checkedId == R.id.rb_m_to_ft) {
                outputVal = inputVal * 3.28084;
                fromUnit = "meters";
                toUnit = "feet";
            } else if (checkedId == R.id.rb_ft_to_m) {
                outputVal = inputVal / 3.28084;
                fromUnit = "feet";
                toUnit = "meters";
            } else {
                Toast.makeText(this, "Choose validation option", Toast.LENGTH_SHORT).show();
                return;
            }

            String resultText = String.format(Locale.US, "%.2f %s = %.2f %s", inputVal, fromUnit, outputVal, toUnit);
            tvConvertResult.setText(resultText);
            tvConvertResult.setVisibility(View.VISIBLE);

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid formatting parameters entered", Toast.LENGTH_SHORT).show();
        }
    }
}