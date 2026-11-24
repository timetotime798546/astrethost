package com.varshalaxmi.loanmanager;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

public class SettingsActivity extends Activity {

    private DatabaseHelper dbHelper;
    private CheckBox cbEnablePin;
    private EditText etPin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        dbHelper = new DatabaseHelper(this);

        cbEnablePin = (CheckBox) findViewById(R.id.cb_enable_pin);
        etPin = (EditText) findViewById(R.id.et_set_pin);
        Button btnSavePin = (Button) findViewById(R.id.btn_save_pin);

        loadSettings();

        btnSavePin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSettings();
            }
        });
    }

    private void loadSettings() {
        boolean isEnabled = "true".equalsIgnoreCase(dbHelper.getSetting("pin_enabled", "false"));
        String pin = dbHelper.getSetting("app_pin", "");

        cbEnablePin.setChecked(isEnabled);
        etPin.setText(pin);
    }

    private void saveSettings() {
        boolean isEnabled = cbEnablePin.isChecked();
        String pin = etPin.getText().toString().trim();

        if (isEnabled && pin.isEmpty()) {
            Toast.makeText(this, "Please enter a PIN number to enable PIN lock", Toast.LENGTH_SHORT).show();
            return;
        }

        dbHelper.setSetting("pin_enabled", isEnabled ? "true" : "false");
        dbHelper.setSetting("app_pin", pin);

        Toast.makeText(this, "Security preferences saved successfully!", Toast.LENGTH_SHORT).show();
        finish();
    }
}