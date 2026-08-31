package com.omnitoolspremium.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class ConverterActivity extends Activity {

    private Spinner spinnerCategory;
    private EditText editInputVal;
    private LinearLayout layoutResults;
    private final String[] categories = {"Length (Meters)", "Weight (Kilograms)", "Temperature (Celsius)"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_converter);

        spinnerCategory = (Spinner) findViewById(R.id.spinner_category);
        editInputVal = (EditText) findViewById(R.id.edit_input_val);
        layoutResults = (LinearLayout) findViewById(R.id.layout_results);
        Button btnConvert = (Button) findViewById(R.id.btn_convert);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, categories);
        spinnerCategory.setAdapter(adapter);

        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                layoutResults.removeAllViews();
            } 
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnConvert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performConversion();
            }
        });
    }

    private void performConversion() {
        String inputStr = editInputVal.getText().toString().trim();
        if (inputStr.isEmpty()) {
            Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show();
            return;
        }

        double val;
        try {
            val = Double.parseDouble(inputStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid decimal entry", Toast.LENGTH_SHORT).show();
            return;
        }

        layoutResults.removeAllViews();
        int categoryIndex = spinnerCategory.getSelectedItemPosition();

        if (categoryIndex == 0) {
            // Length: Meters to Kilometers, Miles, Feet, Inches
            addResultRow("Kilometers", String.format("%.4f km", val / 1000.0));
            addResultRow("Miles", String.format("%.4f miles", val * 0.000621371));
            addResultRow("Feet", String.format("%.2f ft", val * 3.28084));
            addResultRow("Inches", String.format("%.2f in", val * 39.3701));
        } else if (categoryIndex == 1) {
            // Weight: KG to Grams, Pounds, Ounces, Tons
            addResultRow("Grams", String.format("%.2f g", val * 1000.0));
            addResultRow("Pounds (lbs)", String.format("%.4f lbs", val * 2.20462));
            addResultRow("Ounces (oz)", String.format("%.2f oz", val * 35.274));
            addResultRow("Stone", String.format("%.4f st", val * 0.157473));
        } else if (categoryIndex == 2) {
            // Temp: C to F, K
            double f = (val * 9.0/5.0) + 32.0;
            double k = val + 273.15;
            addResultRow("Fahrenheit", String.format("%.2f °F", f));
            addResultRow("Kelvin", String.format("%.2f K", k));
        }
    }

    private void addResultRow(String targetUnit, String calculatedVal) { 
        TextView tv = new TextView(this);
        tv.setText(targetUnit + " : " + calculatedVal);
        tv.setTextSize(16);
        tv.setTextColor(0xFF2C3E50);
        tv.setPadding(0, 8, 0, 8);
        layoutResults.addView(tv);
    }
}