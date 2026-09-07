package com.tinycounter.app;

import android.app.Activity;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    private int count = 0;
    private boolean isDarkMode = false;
    private ToneGenerator toneGenerator;

    private LinearLayout mainContainer;
    private TextView titleTextView;
    private TextView counterTextView;
    private Button btnDecrement;
    private Button btnReset;
    private Button btnIncrement;
    private Button btnThemeToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize audio system for click feed audio feedback
        try {
            toneGenerator = new ToneGenerator(AudioManager.STREAM_SYSTEM, 85);
        } catch (Exception e) {
            toneGenerator = null;
        }

        // Connect Layout Elements
        mainContainer = (LinearLayout) findViewById(R.id.mainContainer);
        titleTextView = (TextView) findViewById(R.id.titleTextView);
        counterTextView = (TextView) findViewById(R.id.counterTextView);
        btnDecrement = (Button) findViewById(R.id.btnDecrement);
        btnReset = (Button) findViewById(R.id.btnReset);
        btnIncrement = (Button) findViewById(R.id.btnIncrement);
        btnThemeToggle = (Button) findViewById(R.id.btnThemeToggle);

        // Define Java 8 compatible Click Handlers
        btnIncrement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                count++;
                updateCounterDisplay();
                playClickTone(ToneGenerator.TONE_PROP_BEEP);
            }
        });

        btnDecrement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (count > 0) {
                    count--;
                    updateCounterDisplay();
                    playClickTone(ToneGenerator.TONE_PROP_BEEP2);
                } else {
                    playClickTone(ToneGenerator.TONE_CDMA_PIP);
                }
            }
        });

        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                count = 0;
                updateCounterDisplay();
                playClickTone(ToneGenerator.TONE_PROP_ACK);
            }
        });

        btnThemeToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isDarkMode = !isDarkMode;
                applyLayoutTheme();
                playClickTone(ToneGenerator.TONE_PROP_PROMPT);
            }
        });

        // Initialize display state
        updateCounterDisplay();
        applyLayoutTheme();
    }

    private void updateCounterDisplay() {
        counterTextView.setText(String.valueOf(count));
    }

    private void applyLayoutTheme() {
        if (isDarkMode) {
            mainContainer.setBackgroundColor(Color.parseColor("#121212"));
            titleTextView.setTextColor(Color.parseColor("#FFFFFF"));
            counterTextView.setTextColor(Color.parseColor("#FFC107")); // Material gold color
            btnThemeToggle.setText("Light Mode");
            btnThemeToggle.setTextColor(Color.parseColor("#FFFFFF"));
            btnThemeToggle.setBackgroundColor(Color.parseColor("#424242"));
            btnDecrement.setTextColor(Color.parseColor("#FFFFFF"));
            btnDecrement.setBackgroundColor(Color.parseColor("#37474F"));
            btnReset.setTextColor(Color.parseColor("#FFFFFF"));
            btnReset.setBackgroundColor(Color.parseColor("#37474F"));
            btnIncrement.setTextColor(Color.parseColor("#FFFFFF"));
            btnIncrement.setBackgroundColor(Color.parseColor("#37474F"));
        } else {
            mainContainer.setBackgroundColor(Color.parseColor("#FAFAFA"));
            titleTextView.setTextColor(Color.parseColor("#212121"));
            counterTextView.setTextColor(Color.parseColor("#1A237E")); // Deep rich indigo color
            btnThemeToggle.setText("Dark Mode");
            btnThemeToggle.setTextColor(Color.parseColor("#FFFFFF"));
            btnThemeToggle.setBackgroundColor(Color.parseColor("#2196F3"));
            btnDecrement.setTextColor(Color.parseColor("#FFFFFF"));
            btnDecrement.setBackgroundColor(Color.parseColor("#E53935"));
            btnReset.setTextColor(Color.parseColor("#FFFFFF"));
            btnReset.setBackgroundColor(Color.parseColor("#757575"));
            btnIncrement.setTextColor(Color.parseColor("#FFFFFF"));
            btnIncrement.setBackgroundColor(Color.parseColor("#4CAF50"));
        }
    }

    private void playClickTone(int soundId) {
        if (toneGenerator != null) {
            try {
                toneGenerator.startTone(soundId, 100);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (toneGenerator != null) {
            toneGenerator.release();
        }
    }
}