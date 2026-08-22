package com.multitoolsuite.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) { 
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupButton(R.id.btn_calculator, CalculatorActivity.class);
        setupButton(R.id.btn_notes, NotesActivity.class);
        setupButton(R.id.btn_stopwatch, StopwatchActivity.class);
        setupButton(R.id.btn_unit, UnitConverterActivity.class);
        setupButton(R.id.btn_flashlight, FlashlightActivity.class);
        setupButton(R.id.btn_bmi, BmiActivity.class);
        setupButton(R.id.btn_dice, DiceActivity.class);
    }

    private void setupButton(int resId, final Class<?> cls) {
        Button btn = (Button) findViewById(resId);
        if (btn != null) {
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MainActivity.this, cls);
                    startActivity(intent);
                }
            });
        }
    }
}
