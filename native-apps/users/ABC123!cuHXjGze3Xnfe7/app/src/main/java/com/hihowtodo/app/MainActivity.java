package com.hihowtodo.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {

    private TextView textTipContent;
    private Button buttonNextTip;
    private int currentTipIndex = 0;

    // A collection of lightweight instructions or "How To" answers for the user
    private final String[] tips = new String[] {
        "How to stay productive: Focus on one small, clear task for just 25 minutes without looking at your phone.",
        "How to drink water properly: Keep a reusable bottle on your desk; drinking small sips throughout the day improves focus.",
        "How to master coding: Write small pieces of code daily. Consistency is always better than rare long sessions.",
        "How to sleep better: Keep your bedroom entirely dark and cool, and avoid screens for 45 minutes before sleep.",
        "How to make good decisions: Wait 24 hours before making any major impulse purchase to see if you still want it.",
        "How to keep organized: Spend just 5 minutes every single evening clearing your direct workspace or desk.",
        "How to improve memory: Teach a newly learned concept to a friend or write it down in simple steps.",
        "How to exercise simply: Walk briskly for 15-20 minutes daily to enjoy sustained physical and mental benefits."
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize user interface components
        textTipContent = (TextView) findViewById(R.id.textTipContent);
        buttonNextTip = (Button) findViewById(R.id.buttonNextTip);

        // Set initial content state
        updateTipDisplay();

        // Implement traditional click listener (strict Java 8 compatible, no lambdas)
        buttonNextTip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Cycle index through available tips
                currentTipIndex = (currentTipIndex + 1) % tips.length;
                updateTipDisplay();
            }
        });
    }

    /**
     * Updates the main interactive container text safely.
     */
    private void updateTipDisplay() {
        if (tips != null && tips.length > 0) {
            textTipContent.setText(tips[currentTipIndex]);
        }
    }
}