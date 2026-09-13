package com.threedclock.app;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements SensorEventListener {

    private Clock3DView clockView;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Vibrator vibrator;

    private TextView tvReadoutHeader;
    private TextView tvDigitalReadout;

    private LinearLayout layoutClockControls;
    private LinearLayout layoutStopwatchControls;
    private LinearLayout layoutTimerControls;

    private Button btnModeClock;
    private Button btnModeStopwatch;
    private Button btnModeTimer;

    private Button btnSwStart;
    private Button btnSwReset;

    private SeekBar sbTimerSelect;
    private TextView tvTimerVal;
    private Button btnTimerStart;
    private Button btnTimerReset;

    private Switch swSensor;
    private SeekBar sbSensitivity;
    private SeekBar sbDepth;

    private Button btnThemeGold;
    private Button btnThemeNeon;
    private Button btnThemeObsidian;
    private Button btnThemeSteampunk;

    private boolean isStopwatchRunning = false;
    private boolean isTimerRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        initViews();
        initSensors();
        setupListeners();
        
        setThemeUI(Clock3DView.THEME_GOLD);
        setModeUI(Clock3DView.MODE_CLOCK);
    }

    private void initViews() {
        clockView = (Clock3DView) findViewById(R.id.clock3DView);
        tvReadoutHeader = (TextView) findViewById(R.id.tvReadoutHeader);
        tvDigitalReadout = (TextView) findViewById(R.id.tvDigitalReadout);

        layoutClockControls = (LinearLayout) findViewById(R.id.layoutClockControls);
        layoutStopwatchControls = (LinearLayout) findViewById(R.id.layoutStopwatchControls);
        layoutTimerControls = (LinearLayout) findViewById(R.id.layoutTimerControls);

        btnModeClock = (Button) findViewById(R.id.btnModeClock);
        btnModeStopwatch = (Button) findViewById(R.id.btnModeStopwatch);
        btnModeTimer = (Button) findViewById(R.id.btnModeTimer);

        btnSwStart = (Button) findViewById(R.id.btnSwStart);
        btnSwReset = (Button) findViewById(R.id.btnSwReset);

        sbTimerSelect = (SeekBar) findViewById(R.id.sbTimerSelect);
        tvTimerVal = (TextView) findViewById(R.id.tvTimerVal);
        btnTimerStart = (Button) findViewById(R.id.btnTimerStart);
        btnTimerReset = (Button) findViewById(R.id.btnTimerReset);

        swSensor = (Switch) findViewById(R.id.swSensor);
        sbSensitivity = (SeekBar) findViewById(R.id.sbSensitivity);
        sbDepth = (SeekBar) findViewById(R.id.sbDepth);

        btnThemeGold = (Button) findViewById(R.id.btnThemeGold);
        btnThemeNeon = (Button) findViewById(R.id.btnThemeNeon);
        btnThemeObsidian = (Button) findViewById(R.id.btnThemeObsidian);
        btnThemeSteampunk = (Button) findViewById(R.id.btnThemeSteampunk);
    }

    private void initSensors() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    }

    private void setupListeners() {
        clockView.setCallback(new Clock3DView.ClockCallback() {
            @Override
            public void onTimerFinished() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        triggerVibrate(800);
                        Toast.makeText(MainActivity.this, "⏱️ Timer complete!", Toast.LENGTH_LONG).show();
                        btnTimerStart.setText("START");
                        isTimerRunning = false;
                        tvDigitalReadout.setText("00:00");
                    }
                });
            }

            @Override
            public void onTickUpdate(final String timeStr, final float progress) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvDigitalReadout.setText(timeStr);
                    }
                });
            }
        });

        btnModeClock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setModeUI(Clock3DView.MODE_CLOCK);
            }
        });

        btnModeStopwatch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setModeUI(Clock3DView.MODE_STOPWATCH);
            }
        });

        btnModeTimer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setModeUI(Clock3DView.MODE_TIMER);
            }
        });

        btnSwStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerVibrate(30);
                if (!isStopwatchRunning) {
                    clockView.startStopwatch();
                    btnSwStart.setText("PAUSE");
                    isStopwatchRunning = true;
                } else {
                    clockView.pauseStopwatch();
                    btnSwStart.setText("START");
                    isStopwatchRunning = false;
                }
            }
        });

        btnSwReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerVibrate(30);
                clockView.resetStopwatch();
                btnSwStart.setText("START");
                isStopwatchRunning = false;
                tvDigitalReadout.setText("00:00.00");
            }
        });

        sbTimerSelect.setMax(60);
        sbTimerSelect.setProgress(5);
        sbTimerSelect.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress < 1) {
                    progress = 1;
                    seekBar.setProgress(1);
                }
                tvTimerVal.setText(progress + " Min");
                if (fromUser) {
                    clockView.setTimerMinutes(progress);
                    tvDigitalReadout.setText(String.format("%02d:00", progress));
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnTimerStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerVibrate(30);
                if (!isTimerRunning) {
                    clockView.startTimer();
                    btnTimerStart.setText("PAUSE");
                    isTimerRunning = true;
                } else {
                    clockView.pauseTimer();
                    btnTimerStart.setText("START");
                    isTimerRunning = false;
                }
            }
        });

        btnTimerReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerVibrate(30);
                clockView.resetTimer();
                btnTimerStart.setText("START");
                isTimerRunning = false;
                int val = sbTimerSelect.getProgress();
                tvDigitalReadout.setText(String.format("%02d:00", val));
            }
        });

        swSensor.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                clockView.setSensorEnabled(isChecked);
                sbSensitivity.setEnabled(isChecked);
            }
        });

        sbSensitivity.setMax(100);
        sbSensitivity.setProgress(50);
        sbSensitivity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float sens = progress / 50.0f;
                clockView.setSensitivity(sens);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        sbDepth.setMax(100);
        sbDepth.setProgress(50);
        sbDepth.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float multiplier = progress / 50.0f;
                clockView.setDepthFactor(multiplier);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnThemeGold.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setThemeUI(Clock3DView.THEME_GOLD);
            }
        });

        btnThemeNeon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setThemeUI(Clock3DView.THEME_NEON);
            }
        });

        btnThemeObsidian.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setThemeUI(Clock3DView.THEME_OBSIDIAN);
            }
        });

        btnThemeSteampunk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setThemeUI(Clock3DView.THEME_STEAMPUNK);
            }
        });
    }

    private void setModeUI(int mode) {
        clockView.setMode(mode);

        btnModeClock.setBackgroundResource(R.drawable.btn_tab_normal);
        btnModeStopwatch.setBackgroundResource(R.drawable.btn_tab_normal);
        btnModeTimer.setBackgroundResource(R.drawable.btn_tab_normal);

        layoutClockControls.setVisibility(View.GONE);
        layoutStopwatchControls.setVisibility(View.GONE);
        layoutTimerControls.setVisibility(View.GONE);

        switch (mode) {
            case Clock3DView.MODE_CLOCK:
                btnModeClock.setBackgroundResource(R.drawable.btn_tab_active);
                layoutClockControls.setVisibility(View.VISIBLE);
                tvReadoutHeader.setText("HOLOGRAPHIC 3D DIAL");
                tvDigitalReadout.setText("TILT TO ROTATE");
                break;
            case Clock3DView.MODE_STOPWATCH:
                btnModeStopwatch.setBackgroundResource(R.drawable.btn_tab_active);
                layoutStopwatchControls.setVisibility(View.VISIBLE);
                tvReadoutHeader.setText("HIGH PRECISION CHRONOGRAPH");
                tvDigitalReadout.setText("00:00.00");
                clockView.resetStopwatch();
                btnSwStart.setText("START");
                isStopwatchRunning = false;
                break;
            case Clock3DView.MODE_TIMER:
                btnModeTimer.setBackgroundResource(R.drawable.btn_tab_active);
                layoutTimerControls.setVisibility(View.VISIBLE);
                tvReadoutHeader.setText("ROUND WEDGE TIMER");
                int val = sbTimerSelect.getProgress();
                tvDigitalReadout.setText(String.format("%02d:00", val));
                clockView.setTimerMinutes(val);
                btnTimerStart.setText("START");
                isTimerRunning = false;
                break;
        }
    }

    private void setThemeUI(int theme) {
        clockView.setTheme(theme);

        btnThemeGold.setSelected(false);
        btnThemeNeon.setSelected(false);
        btnThemeObsidian.setSelected(false);
        btnThemeSteampunk.setSelected(false);

        switch (theme) {
            case Clock3DView.THEME_GOLD:
                btnThemeGold.setSelected(true);
                break;
            case Clock3DView.THEME_NEON:
                btnThemeNeon.setSelected(true);
                break;
            case Clock3DView.THEME_OBSIDIAN:
                btnThemeObsidian.setSelected(true);
                break;
            case Clock3DView.THEME_STEAMPUNK:
                btnThemeSteampunk.setSelected(true);
                break;
        }
    }

    private void triggerVibrate(long ms) {
        if (vibrator != null) {
            try {
                vibrator.vibrate(ms);
            } catch (Exception ignored) {}
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        clockView.pauseStopwatch();
        clockView.pauseTimer();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float ax = event.values[0];
            float ay = event.values[1];
            clockView.updateSensorTilt(ax, ay);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}