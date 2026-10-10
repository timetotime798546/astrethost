package com.neondigitalclock.app;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private TextView textClock;
    private TextView textAmPm;
    private TextView textDate;
    private TextView textBatteryIcon;
    private TextView textQuote;
    private TextView btnRefreshQuote;
    private TextView btnTimeFormatToggle;
    private View pulseLine;
    private View topWidgetCard;
    private LinearLayout controlsContainer;
    private View rootView;
    private SeekBar seekbarBrightness;

    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private boolean is24HourFormat = false;
    private boolean isControlsVisible = true;
    private boolean pulseState = false;
    private int currentAccentColor = Color.parseColor("#00FFFF");

    private String[] ambientQuotes = new String[]{
        "\"The stars shine brightest in the deepest dark.\"",
        "\"Rest is not idleness, it is the key to renewal.\"",
        "\"Tomorrow is a new day with fresh possibilities.\"",
        "\"Peace begins when expectations end.\"",
        "\"Dream big, sleep well, wake up grateful.\"",
        "\"Night is the quiet opportunity to recharge.\"",
        "\"Calm mind, serene spirit, peaceful night.\""
    };
    private int quoteIndex = 0;

    private BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);

                float batteryPct = (level / (float) scale) * 100f;
                boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL;

                String chargingIcon = isCharging ? "⚡ " : "🔋 ";
                textBatteryIcon.setText(chargingIcon + (int) batteryPct + "%");
            }
        }
    };

    private Runnable clockTicker = new Runnable() {
        @Override
        public void run() {
            updateClockDisplay();
            pulseState = !pulseState;
            pulseLine.setAlpha(pulseState ? 1.0f : 0.3f);
            timerHandler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Keep screen awake for ambient nightstand usage
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Set full screen layout
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        setContentView(R.layout.activity_main);

        initViews();
        setupColorPicker();
        setupBrightnessControl();
        setupClickListeners();

        // Register live battery status receiver
        registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        // Start clock ticker loop
        timerHandler.post(clockTicker);
    }

    private void initViews() {
        rootView = findViewById(R.id.rootView);
        textClock = findViewById(R.id.textClock);
        textAmPm = findViewById(R.id.textAmPm);
        textDate = findViewById(R.id.textDate);
        textBatteryIcon = findViewById(R.id.textBatteryIcon);
        textQuote = findViewById(R.id.textQuote);
        btnRefreshQuote = findViewById(R.id.btnRefreshQuote);
        btnTimeFormatToggle = findViewById(R.id.btnTimeFormatToggle);
        pulseLine = findViewById(R.id.pulseLine);
        topWidgetCard = findViewById(R.id.topWidgetCard);
        controlsContainer = findViewById(R.id.controlsContainer);
        seekbarBrightness = findViewById(R.id.seekbarBrightness);
    }

    private void updateClockDisplay() {
        Date now = new Date();

        // Format Date
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMM d", Locale.ENGLISH);
        textDate.setText(dateFormat.format(now).toUpperCase(Locale.ENGLISH));

        // Format Clock Time
        if (is24HourFormat) {
            SimpleDateFormat timeFormat24 = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH);
            textClock.setText(timeFormat24.format(now));
            textAmPm.setVisibility(View.GONE);
        } else {
            SimpleDateFormat timeFormat12 = new SimpleDateFormat("hh:mm:ss", Locale.ENGLISH);
            SimpleDateFormat amPmFormat = new SimpleDateFormat("a", Locale.ENGLISH);
            textClock.setText(timeFormat12.format(now));
            textAmPm.setText(amPmFormat.format(now).toUpperCase(Locale.ENGLISH));
            textAmPm.setVisibility(View.VISIBLE);
        }
    }

    private void applyAccentColor(int color) {
        currentAccentColor = color;

        // Apply glow effect to digital clock & AM/PM
        textClock.setTextColor(color);
        textClock.setShadowLayer(28f, 0f, 0f, color);

        textAmPm.setTextColor(color);
        textAmPm.setShadowLayer(20f, 0f, 0f, color);

        textBatteryIcon.setTextColor(color);
        pulseLine.setBackgroundColor(color);
    }

    private void setupColorPicker() {
        findViewById(R.id.colorCyan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyAccentColor(Color.parseColor("#00FFFF"));
            }
        });

        findViewById(R.id.colorPurple).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyAccentColor(Color.parseColor("#B026FF"));
            }
        });

        findViewById(R.id.colorGreen).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyAccentColor(Color.parseColor("#39FF14"));
            }
        });

        findViewById(R.id.colorPink).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyAccentColor(Color.parseColor("#FF007F"));
            }
        });

        findViewById(R.id.colorYellow).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyAccentColor(Color.parseColor("#FFD700"));
            }
        });

        findViewById(R.id.colorOrange).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyAccentColor(Color.parseColor("#FF5722"));
            }
        });
    }

    private void setupBrightnessControl() {
        seekbarBrightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    // Min brightness cap at 0.05 to prevent pitch-black screen locking
                    float brightnessValue = Math.max(0.05f, progress / 100.0f);
                    WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
                    layoutParams.screenBrightness = brightnessValue;
                    getWindow().setAttributes(layoutParams);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    private void setupClickListeners() {
        // Toggle UI controls when tapping main screen background
        rootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isControlsVisible = !isControlsVisible;
                int visibility = isControlsVisible ? View.VISIBLE : View.GONE;
                topWidgetCard.setVisibility(visibility);
                controlsContainer.setVisibility(visibility);
            }
        });

        // 12H / 24H Toggle
        btnTimeFormatToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                is24HourFormat = !is24HourFormat;
                btnTimeFormatToggle.setText(is24HourFormat ? "Format: 24H" : "Format: 12H");
                updateClockDisplay();
            }
        });

        // Refresh quote button
        btnRefreshQuote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                quoteIndex = (quoteIndex + 1) % ambientQuotes.length;
                textQuote.setText(ambientQuotes[quoteIndex]);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timerHandler.removeCallbacks(clockTicker);
        try {
            unregisterReceiver(batteryReceiver);
        } catch (Exception ignored) {
        }
    }
}