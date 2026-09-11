package com.datetimeonly.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private TextView tvTime;
    private TextView tvDate;
    private TextView tvAmPm;
    private TextView tvInfo;
    private RelativeLayout rootLayout;

    private final Handler handler = new Handler();
    
    // Formatting configurations
    private int timeFormatIndex = 0; // 0: 24h with seconds, 1: 12h with seconds, 2: 24h custom, 3: 12h custom
    private int dateFormatIndex = 0; // 0: EEEE, MMMM d, yyyy, 1: dd/MM/yyyy, 2: yyyy-MM-dd, 3: EEE, MMM d, 'yy
    private int themeIndex = 0;

    // Beautiful High-Contrast themes suited for clocks (Background Color, Primary Text Color, Secondary Text Color)
    private static final int[][] THEMES = {
        {0xFF000000, 0xFFFFFFFF, 0xFF888888}, // Midnight Black (Ultra contrast / battery saver)
        {0xFF0A192F, 0xFF64FFDA, 0xFF8892B0}, // Cyber Deep Blue
        {0xFF1E1E1E, 0xFFFFB300, 0xFFB0BEC5}, // Amber Vintage Clock
        {0xFFF5F5F5, 0xFF111111, 0xFF757575}  // Minimalist Pure White
    };

    private final Runnable updateTimeRunnable = new Runnable() {
        @Override
        public void run() {
            updateDateTime();
            handler.postDelayed(this, 250); // Refresh frequently for high-accuracy updates
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Prevent display dimming / sleeping to support desk clock usage
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        rootLayout = (RelativeLayout) findViewById(R.id.rootLayout);
        tvTime = (TextView) findViewById(R.id.tvTime);
        tvDate = (TextView) findViewById(R.id.tvDate);
        tvAmPm = (TextView) findViewById(R.id.tvAmPm);
        tvInfo = (TextView) findViewById(R.id.tvInfo);

        // Tap time to cycle format
        tvTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                timeFormatIndex = (timeFormatIndex + 1) % 4;
                updateDateTime();
                showTemporaryToast("Time Format Changed");
            }
        });

        // Tap date to cycle style
        tvDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dateFormatIndex = (dateFormatIndex + 1) % 4;
                updateDateTime();
                showTemporaryToast("Date Style Changed");
            }
        });

        // Long press to cycle theme colors
        rootLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                themeIndex = (themeIndex + 1) % THEMES.length;
                applyTheme();
                showTemporaryToast("Theme Switched");
                return true;
            }
        });

        applyTheme();

        // Fade help guide text away after 6 seconds
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (tvInfo != null) {
                    tvInfo.animate().alpha(0.0f).setDuration(1000);
                }
            }
        }, 6000);
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(updateTimeRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(updateTimeRunnable);
    }

    private void updateDateTime() {
        Date now = new Date();

        // Time Formatting Configuration
        String timePattern = "HH:mm:ss";
        boolean showAmPmField = false;

        switch (timeFormatIndex) {
            case 0:
                timePattern = "HH:mm:ss";
                break;
            case 1:
                timePattern = "hh:mm:ss";
                showAmPmField = true;
                break;
            case 2:
                timePattern = "HH:mm";
                break;
            case 3:
                timePattern = "hh:mm";
                showAmPmField = true;
                break;
        }

        SimpleDateFormat timeFormatter = new SimpleDateFormat(timePattern, Locale.getDefault());
        tvTime.setText(timeFormatter.format(now));

        // AM/PM Management
        if (showAmPmField) {
            SimpleDateFormat amPmFormatter = new SimpleDateFormat("a", Locale.getDefault());
            tvAmPm.setText(amPmFormatter.format(now));
            tvAmPm.setVisibility(View.VISIBLE);
        } else {
            tvAmPm.setVisibility(View.GONE);
        }

        // Date Formatting Configuration
        String datePattern = "EEEE, MMMM d, yyyy";
        switch (dateFormatIndex) {
            case 0:
                datePattern = "EEEE, MMMM d, yyyy";
                break;
            case 1:
                datePattern = "dd/MM/yyyy";
                break;
            case 2:
                datePattern = "yyyy-MM-dd";
                break;
            case 3:
                datePattern = "EEE, MMM d, ''yy";
                break;
        }

        SimpleDateFormat dateFormatter = new SimpleDateFormat(datePattern, Locale.getDefault());
        tvDate.setText(dateFormatter.format(now));
    }

    private void applyTheme() {
        int[] themeColors = THEMES[themeIndex];
        int bgColor = themeColors[0];
        int primaryColor = themeColors[1];
        int secondaryColor = themeColors[2];

        rootLayout.setBackgroundColor(bgColor);
        tvTime.setTextColor(primaryColor);
        tvAmPm.setTextColor(primaryColor);
        tvDate.setTextColor(secondaryColor);
        tvInfo.setTextColor(secondaryColor);
    }

    private void showTemporaryToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}