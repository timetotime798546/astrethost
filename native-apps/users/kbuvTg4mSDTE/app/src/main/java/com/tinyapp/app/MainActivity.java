package com.tinyapp.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.util.Random;

public class MainActivity extends Activity {

    private int count = 0;
    private Random random;

    private TextView counterValue;
    private TextView diceValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        random = new Random();

        counterValue = (TextView) findViewById(R.id.counterValue);
        diceValue = (TextView) findViewById(R.id.diceValue);

        Button btnIncrement = (Button) findViewById(R.id.btnIncrement);
        Button btnDecrement = (Button) findViewById(R.id.btnDecrement);
        Button btnResetCounter = (Button) findViewById(R.id.btnResetCounter);
        Button btnRoll = (Button) findViewById(R.id.btnRoll);

        // Click listeners coded using traditional anonymous inner classes to adhere strictly to Java 8 compatibility rules.
        btnIncrement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (count < 9999) {
                    count++;
                    updateCounterText();
                }
            }
        });

        btnDecrement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (count > -999) {
                    count--;
                    updateCounterText();
                }
            }
        });

        btnResetCounter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                count = 0;
                updateCounterText();
            }
        });

        btnRoll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int roll = random.nextInt(6) + 1;
                diceValue.setText(String.valueOf(roll));
            }
        });
    }

    private void updateCounterText() {
        counterValue.setText(String.valueOf(count));
    }
}