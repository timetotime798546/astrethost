package com.homecontrol.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private int targetTemperature = 72;
    private int lightBrightness = 80;
    private boolean isLightsOn = true;
    private boolean isLocked = true;

    private TextView tvHeaderStatus;
    private TextView tvCurrentTemp;
    private TextView tvLightBrightness;
    private TextView tvLockStatus;
    private Switch switchLights;
    private Switch switchLock;
    private SeekBar seekBarBrightness;
    private LinearLayout layoutLightControl;
    private LinearLayout layoutLogsContainer;

    // Filter Buttons
    private Button btnFilterAll;
    private Button btnFilterLiving;
    private Button btnFilterBackyard;

    // Cards
    private View cardThermostat;
    private View cardLights;
    private View cardLock;
    private View cardCamera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind Views
        tvHeaderStatus = (TextView) findViewById(R.id.tvHeaderStatus);
        tvCurrentTemp = (TextView) findViewById(R.id.tvCurrentTemp);
        tvLightBrightness = (TextView) findViewById(R.id.tvLightBrightness);
        tvLockStatus = (TextView) findViewById(R.id.tvLockStatus);

        switchLights = (Switch) findViewById(R.id.switchLights);
        switchLock = (Switch) findViewById(R.id.switchLock);
        seekBarBrightness = (SeekBar) findViewById(R.id.seekBarBrightness);
        layoutLightControl = (LinearLayout) findViewById(R.id.layoutLightControl);
        layoutLogsContainer = (LinearLayout) findViewById(R.id.layoutLogsContainer);

        btnFilterAll = (Button) findViewById(R.id.btnFilterAll);
        btnFilterLiving = (Button) findViewById(R.id.btnFilterLiving);
        btnFilterBackyard = (Button) findViewById(R.id.btnFilterBackyard);

        cardThermostat = findViewById(R.id.cardThermostat);
        cardLights = findViewById(R.id.cardLights);
        cardLock = findViewById(R.id.cardLock);
        cardCamera = findViewById(R.id.cardCamera);

        // Setup Buttons & Listeners using standard anonymous classes
        setupThermostatListeners();
        setupLightsListeners();
        setupLockListeners();
        setupFilterListeners();
        setupCameraListeners();

        // Clear Logs button
        findViewById(R.id.btnClearLogs).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                layoutLogsContainer.removeAllViews();
                addLogEntry("Logs cleared.");
            }
        });

        // Add Initial Logs
        addLogEntry("Home Control system online.");
        addLogEntry("Connected to Smart Thermostat (Main).");
        addLogEntry("Smart lights detected.");
        addLogEntry("Main entrance lock reported secure.");
        addLogEntry("Backyard security camera active.");

        updateHeaderStatus();
    }

    private void setupThermostatListeners() {
        Button btnTempUp = (Button) findViewById(R.id.btnTempUp);
        Button btnTempDown = (Button) findViewById(R.id.btnTempDown);

        btnTempUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (targetTemperature < 90) {
                    targetTemperature++;
                    tvCurrentTemp.setText(targetTemperature + "°F");
                    addLogEntry("Thermostat target temperature set to " + targetTemperature + "°F");
                    updateHeaderStatus();
                }
            }
        });

        btnTempDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (targetTemperature > 55) {
                    targetTemperature--;
                    tvCurrentTemp.setText(targetTemperature + "°F");
                    addLogEntry("Thermostat target temperature set to " + targetTemperature + "°F");
                    updateHeaderStatus();
                }
            }
        });
    }

    private void setupLightsListeners() {
        switchLights.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isLightsOn = isChecked;
                if (isChecked) {
                    layoutLightControl.setVisibility(View.VISIBLE);
                    addLogEntry("Living Room main lights turned ON");
                } else {
                    layoutLightControl.setVisibility(View.GONE);
                    addLogEntry("Living Room main lights turned OFF");
                }
                updateHeaderStatus();
            }
        });

        seekBarBrightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                lightBrightness = progress;
                tvLightBrightness.setText(progress + "%");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                addLogEntry("Living Room lights brightness set to " + lightBrightness + "%");
            }
        });
    }

    private void setupLockListeners() {
        switchLock.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isLocked = isChecked;
                if (isChecked) {
                    tvLockStatus.setText("SECURE & LOCKED");
                    tvLockStatus.setTextColor(Color.parseColor("#4CAF50")); // Green
                    addLogEntry("Main entrance LOCKED successfully.");
                } else {
                    tvLockStatus.setText("UNLOCKED");
                    tvLockStatus.setTextColor(Color.parseColor("#F44336")); // Red
                    addLogEntry("WARNING: Main entrance UNLOCKED.");
                }
                updateHeaderStatus();
            }
        });
    }

    private void setupCameraListeners() {
        Button btnCameraTrigger = (Button) findViewById(R.id.btnCameraTrigger);
        final CheckBox cbMotionAlerts = (CheckBox) findViewById(R.id.cbMotionAlerts);

        btnCameraTrigger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cbMotionAlerts.isChecked()) {
                    addLogEntry("ALERT: Backyard Camera detected simulated movement!");
                } else {
                    addLogEntry("Camera alert test triggered (motion alerts muted).");
                }
            }
        });

        cbMotionAlerts.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    addLogEntry("Backyard Camera motion sensor enabled.");
                } else {
                    addLogEntry("Backyard Camera motion sensor disabled.");
                }
            }
        });
    }

    private void setupFilterListeners() {
        btnFilterAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setFilterActive(btnFilterAll, btnFilterLiving, btnFilterBackyard);
                cardThermostat.setVisibility(View.VISIBLE);
                cardLights.setVisibility(View.VISIBLE);
                cardLock.setVisibility(View.VISIBLE);
                cardCamera.setVisibility(View.VISIBLE);
                addLogEntry("Showing dashboard for all rooms.");
            }
        });

        btnFilterLiving.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setFilterActive(btnFilterLiving, btnFilterAll, btnFilterBackyard);
                cardThermostat.setVisibility(View.VISIBLE);
                cardLights.setVisibility(View.VISIBLE);
                cardLock.setVisibility(View.GONE);
                cardCamera.setVisibility(View.GONE);
                addLogEntry("Filtered view: Living Room.");
            }
        });

        btnFilterBackyard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setFilterActive(btnFilterBackyard, btnFilterAll, btnFilterLiving);
                cardThermostat.setVisibility(View.GONE);
                cardLights.setVisibility(View.GONE);
                cardLock.setVisibility(View.VISIBLE);
                cardCamera.setVisibility(View.VISIBLE);
                addLogEntry("Filtered view: Backyard.");
            }
        });
    }

    private void setFilterActive(Button active, Button inActive1, Button inActive2) {
        // Change backgrounds
        active.setBackgroundResource(R.drawable.button_bg);
        active.setTextColor(Color.WHITE);

        inActive1.setBackgroundResource(R.drawable.button_bg_secondary);
        inActive1.setTextColor(Color.parseColor("#333333"));

        inActive2.setBackgroundResource(R.drawable.button_bg_secondary);
        inActive2.setTextColor(Color.parseColor("#333333"));
    }

    private void updateHeaderStatus() {
        String lightsState = isLightsOn ? "Light ON" : "Light OFF";
        String lockState = isLocked ? "Locked" : "UNLOCKED";
        String status = "Temp: " + targetTemperature + "°F | " + lightsState + " | " + lockState;
        tvHeaderStatus.setText(status);
    }

    private void addLogEntry(String text) {
        String timestamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        
        TextView logView = new TextView(this);
        logView.setText("[" + timestamp + "] " + text);
        logView.setTextColor(Color.parseColor("#424242"));
        logView.setTextSize(spToPx(11));
        logView.setPadding(0, 4, 0, 4);

        // Add to bottom of list
        layoutLogsContainer.addView(logView, 0); // insert at top of log layout
    }

    private int spToPx(int sp) {
        return (int) (sp * getResources().getDisplayMetrics().scaledDensity);
    }
}