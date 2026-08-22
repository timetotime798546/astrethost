package com.multitoolsuite.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.util.Random;

public class DiceActivity extends Activity {
    private TextView tvDiceValue;
    private Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dice);

        tvDiceValue = (TextView) findViewById(R.id.tv_dice_value);
        Button btnRoll = (Button) findViewById(R.id.btn_roll_dice);

        btnRoll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int rollVal = random.nextInt(6) + 1;
                tvDiceValue.setText(String.valueOf(rollVal));
            }
        });
    }
}
