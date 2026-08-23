package com.omnicontrolsuite.app;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class SettingsActivity extends Activity {

    private EditText editUserName;
    private EditText editUserDept;
    private SharedPreferences prefs;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences("OmniControlPrefs", MODE_PRIVATE);
        dbHelper = new DatabaseHelper(this);

        editUserName = (EditText) findViewById(R.id.editUserName);
        editUserDept = (EditText) findViewById(R.id.editUserDept);
        Button btnBack = (Button) findViewById(R.id.btnBack);
        Button btnSave = (Button) findViewById(R.id.btnSaveProfile);
        Button btnPurge = (Button) findViewById(R.id.btnPurgeDatabase);

        // Load preferences
        editUserName.setText(prefs.getString("operator_name", "Administrator"));
        editUserDept.setText(prefs.getString("department_group", "IT Security Operations"));

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = editUserName.getText().toString().trim();
                String dept = editUserDept.getText().toString().trim();

                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("operator_name", name);
                editor.putString("department_group", dept);
                editor.apply();

                Toast.makeText(SettingsActivity.this, "Configuration Settings updated successfully", Toast.LENGTH_SHORT).show();
            }
        });

        btnPurge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dbHelper.purgeTasks();
                dbHelper.purgeNotes();
                Toast.makeText(SettingsActivity.this, "Entire databases and caches wiped completely", Toast.LENGTH_SHORT).show();
            }
        });
    }
}