package com.simpleapp.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {

    private int currentCount = 0;
    private TextView counterTextView;
    private Button incrementButton;
    private Button resetButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        counterTextView = (TextView) findViewById(R.id.counterTextView);
        incrementButton = (Button) findViewById(R.id.incrementButton);
        resetButton = (Button) findViewById(R.id.resetButton);

        incrementButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentCount++;
                counterTextView.setText(String.valueOf(currentCount));
            }
        });

        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentCount = 0;
                counterTextView.setText(String.valueOf(currentCount));
            }
        });
    }
}