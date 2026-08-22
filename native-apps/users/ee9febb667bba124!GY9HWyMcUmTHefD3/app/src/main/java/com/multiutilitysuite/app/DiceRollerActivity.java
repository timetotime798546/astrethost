package com.multiutilitysuite.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Random;

public class DiceRollerActivity extends Activity {
    private TextView diceDisplay;
    private Button rollButton;
    private Random random = new Random();
    private Handler animHandler = new Handler();
    private int tickCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout parent = new LinearLayout(this);
        parent.setOrientation(LinearLayout.VERTICAL);
        parent.setBackgroundColor(Color.parseColor("#F5F5F5"));

        // Custom Header Bar
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setBackgroundColor(Color.parseColor("#3F51B5"));
        header.setPadding(32, 24, 32, 24);
        header.setGravity(Gravity.CENTER_VERTICAL);

        Button backButton = new Button(this);
        backButton.setText("< Back");
        backButton.setTextColor(Color.WHITE);
        backButton.setBackgroundColor(Color.TRANSPARENT);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        header.addView(backButton);

        TextView headerTitle = new TextView(this);
        headerTitle.setText("Dice Roller");
        headerTitle.setTextColor(Color.WHITE);
        headerTitle.setTextSize(20);
        headerTitle.setPadding(32, 0, 0, 0);
        header.addView(headerTitle);
        parent.addView(header);

        // Rolling Container Area
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setGravity(Gravity.CENTER);
        body.setPadding(32, 32, 32, 32);
        LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f);
        body.setLayoutParams(bodyParams);

        diceDisplay = new TextView(this);
        diceDisplay.setText("6");
        diceDisplay.setTextSize(120);
        diceDisplay.setTextColor(Color.WHITE);
        diceDisplay.setBackgroundColor(Color.parseColor("#E91E63"));
        diceDisplay.setGravity(Gravity.CENTER);
        
        LinearLayout.LayoutParams diceParams = new LinearLayout.LayoutParams(400, 400);
        diceParams.setMargins(0, 0, 0, 64);
        diceDisplay.setLayoutParams(diceParams);
        body.addView(diceDisplay);

        rollButton = new Button(this);
        rollButton.setText("ROLL DICE");
        rollButton.setBackgroundColor(Color.parseColor("#3F51B5"));
        rollButton.setTextColor(Color.WHITE);
        rollButton.setPadding(32, 32, 32, 32);
        rollButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerRollAnimation();
            }
        });
        body.addView(rollButton);
        parent.addView(body);

        setContentView(parent);
    }

    private void triggerRollAnimation() {
        rollButton.setEnabled(false);
        tickCount = 0;
        animHandler.post(rollingRunnable);
    }

    private final Runnable rollingRunnable = new Runnable() {
        @Override
        public void run() {
            int tempValue = random.nextInt(6) + 1;
            diceDisplay.setText(String.valueOf(tempValue));
            tickCount++;
            if (tickCount < 10) {
                animHandler.postDelayed(this, 100);
            } else {
                rollButton.setEnabled(true);
            }
        }
    };
}