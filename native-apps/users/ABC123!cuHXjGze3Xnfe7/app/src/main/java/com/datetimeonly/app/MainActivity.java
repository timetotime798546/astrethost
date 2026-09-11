package com.datetimeonly.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private TextView timeTextView;
    private TextView dateTextView;
    private TextView hintTextView;
    private View rootLayout;
    private LinearLayout controlsLayout;

    private final Handler handler = new Handler();
    private Runnable tickRunnable;

    // Default configuration settings states
    private boolean is24HourFormat = true;
    private boolean showSeconds = true;
    private boolean isFullscreen = false;
    private int currentThemeIndex = 0;

    // Visual styles presets [Background Color, Text Color, Buttons/Hints highlight]
    private static final int[][] PALETTES = {
        {0xFF000000, 0xFF00FF00, 0xFF008800}, // Retro Green on Pitch Black
        {0xFF080D1A, 0xFF00E5FF, 0xFF00838F}, // Cyberpunk Blue on Dark Indigo
        {0xFF120C06, 0xFFFFB300, 0xFFFF8F00}, // Amber Glow on Dark Slate Charcoal
        {0xFF111111, 0xFFFFFFFF, 0xFF888888}, // Classic Crisp White on Slate
        {0xFF000000, 0xFFFF1744, 0xFFB71C1C}  // Crimson Red on Pitch Black
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rootLayout = findViewById(R.id.rootLayout);
        timeTextView = (TextView) findViewById(R.id.timeTextView);
        dateTextView = (TextView) findViewById(R.id.dateTextView);
        hintTextView = (TextView) findViewById(R.id.hintTextView);
        controlsLayout = (LinearLayout) findViewById(R.id.controlsLayout);

        Button btnToggleFormat = (Button) findViewById(R.id.btnToggleFormat);
        Button btnToggleSeconds = (Button) findViewById(R.id.btnToggleSeconds);
        Button btnChangeTheme = (Button) findViewById(R.id.btnChangeTheme);
        Button btnToggleFullscreen = (Button) findViewById(R.id.btnToggleFullscreen);

        applyTheme();

        // Standard Java 8 Click Listeners (Strict Compatibility Mode, No Lambdas)
        btnToggleFormat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                is24HourFormat = !is24HourFormat;
                updateTimeDisplay();
            }
        });

        btnToggleSeconds.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSeconds = !showSeconds;
                updateTimeDisplay();
            }
        });

        btnChangeTheme.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentThemeIndex = (currentThemeIndex + 1) % PALETTES.length;
                applyTheme();
            }
        });

        btnToggleFullscreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleImmersiveMode();
            }
        });

        // Background single tap hides/displays configuration panel for a clean bedside display
        rootLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (controlsLayout.getVisibility() == View.VISIBLE) {
                    controlsLayout.setVisibility(View.GONE);
                } else {
                    controlsLayout.setVisibility(View.VISIBLE);
                }
            }
        });

        // Long press triggers immersive mode toggling directly
        rootLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                toggleImmersiveMode();
                return true;
            }
        });

        // Initialize update loop
        tickRunnable = new Runnable() {
            @Override
            public void run() {
                updateTimeDisplay();
                handler.postDelayed(this, 200);
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(tickRunnable);
        if (isFullscreen) {
            applyImmersiveFlags();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(tickRunnable);
    }

    private void updateTimeDisplay() {
        Date now = new Date();
        String formatString;

        if (is24HourFormat) {
            formatString = showSeconds ? "HH:mm:ss" : "HH:mm";
        } else {
            formatString = showSeconds ? "hh:mm:ss a" : "hh:mm a";
        }

        SimpleDateFormat timeFormat = new SimpleDateFormat(formatString, Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault());

        timeTextView.setText(timeFormat.format(now));
        dateTextView.setText(dateFormat.format(now));
    }

    private void applyTheme() {
        int[] palette = PALETTES[currentThemeIndex];
        int bgColor = palette[0];
        int textColor = palette[1];
        int accentColor = palette[2];

        rootLayout.setBackgroundColor(bgColor);
        timeTextView.setTextColor(textColor);
        dateTextView.setTextColor(textColor);
        hintTextView.setTextColor(accentColor);

        // Styling our button elements according to the selected theme palette
        for (int i = 0; i < controlsLayout.getChildCount(); i++) {
            View child = controlsLayout.getChildAt(i);
            if (child instanceof Button) {
                Button btn = (Button) child;
                btn.setTextColor(bgColor); // readable contrast
                btn.setBackgroundColor(textColor);
                if (btn.getBackground() != null) {
                    btn.getBackground().setAlpha(225);
                }
            }
        }
    }

    private void toggleImmersiveMode() {
        isFullscreen = !isFullscreen;
        if (isFullscreen) {
            applyImmersiveFlags();
        } else {
            clearImmersiveFlags();
        }
    }

    private void applyImmersiveFlags() {
        if (android.os.Build.VERSION.SDK_INT >= 19) {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    private void clearImmersiveFlags() {
        if (android.os.Build.VERSION.SDK_INT >= 19) {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }
}