package com.offlineapp.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {

    private static final String PREF_NAME = "OfflineAppPrefs";
    private static final String KEY_COUNT = "counter_value";

    private TextView countTextView;
    private Button incrementButton;
    private int currentCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        countTextView = (TextView) findViewById(R.id.countTextView);
        incrementButton = (Button) findViewById(R.id.incrementButton);

        loadCount();
        updateCountDisplay();

        incrementButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentCount++;
                updateCountDisplay();
                saveCount();
            }
        });
    }

    private void updateCountDisplay() {
        countTextView.setText(String.valueOf(currentCount));
    }

    private void loadCount() {
        SharedPreferences sharedPref = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        currentCount = sharedPref.getInt(KEY_COUNT, 0); // Default to 0 if not found
    }

    private void saveCount() {
        SharedPreferences sharedPref = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(KEY_COUNT, currentCount);
        editor.apply(); // Use apply for asynchronous save
    }
}