package com.verysimpleapp.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private int counter = 0;
    private TextView tvCounter;
    private EditText etStep;
    private Button btnIncrement;
    private Button btnReset;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Restore counter value across device configuration changes (e.g. rotation)
        if (savedInstanceState != null) {
            counter = savedInstanceState.getInt("counter_value", 0);
        }

        tvCounter = (TextView) findViewById(R.id.tv_counter);
        etStep = (EditText) findViewById(R.id.et_step);
        btnIncrement = (Button) findViewById(R.id.btn_increment);
        btnReset = (Button) findViewById(R.id.btn_reset);

        updateCounterDisplay();

        // Standard Java 8 compatibility using anonymous click listeners
        btnIncrement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int step = getStepValue();
                counter += step;
                updateCounterDisplay();
            }
        });

        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                counter = 0;
                updateCounterDisplay();
                Toast.makeText(MainActivity.this, getString(R.string.toast_reset), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private int getStepValue() {
        String input = etStep.getText().toString().trim();
        if (input.isEmpty()) {
            return 1;
        }
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            Toast.makeText(this, getString(R.string.toast_invalid_step), Toast.LENGTH_SHORT).show();
            return 1;
        }
    }

    private void updateCounterDisplay() {
        tvCounter.setText(String.valueOf(counter));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("counter_value", counter);
    }
}