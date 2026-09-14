package com.tinycounter.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private int count = 0;
    private TextView counterText;
    private Button btnIncrement;
    private Button btnDecrement;
    private Button btnReset;

    private static final String KEY_COUNT = "current_count";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        counterText = (TextView) findViewById(R.id.counter_text);
        btnIncrement = (Button) findViewById(R.id.btn_increment);
        btnDecrement = (Button) findViewById(R.id.btn_decrement);
        btnReset = (Button) findViewById(R.id.btn_reset);

        if (savedInstanceState != null) {
            count = savedInstanceState.getInt(KEY_COUNT, 0);
        }

        updateCounterDisplay();

        btnIncrement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                count++;
                updateCounterDisplay();
            }
        });

        btnDecrement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (count > 0) {
                    count--;
                    updateCounterDisplay();
                } else {
                    Toast.makeText(MainActivity.this, "Count cannot be negative!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                count = 0;
                updateCounterDisplay();
                Toast.makeText(MainActivity.this, "Counter reset", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateCounterDisplay() {
        if (counterText != null) {
            counterText.setText(String.valueOf(count));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_COUNT, count);
    }
}