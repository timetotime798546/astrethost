package com.multiutilitysuite.app;

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
        setupButton(R.id.btn_stopwatch, StopwatchActivity.class);
        setupButton(R.id.btn_notes, NotesActivity.class);
        setupButton(R.id.btn_converter, UnitConverterActivity.class);
        setupButton(R.id.btn_dice, DiceRollerActivity.class);
        setupButton(R.id.btn_bmi, BmiCalculatorActivity.class);
        setupButton(R.id.btn_color, ColorPickerActivity.class);
        setupButton(R.id.btn_sensor, SensorExplorerActivity.class);
        setupButton(R.id.btn_info, AppInfoActivity.class);
    }

    private void setupButton(int resId, final Class<?> targetActivity) {
        Button button = (Button) findViewById(resId);
        if (button != null) {
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MainActivity.this, targetActivity);
                    startActivity(intent);
                }
            });
        }
    }
}