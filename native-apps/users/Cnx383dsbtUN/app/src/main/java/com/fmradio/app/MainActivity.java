package com.fmradio.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class MainActivity extends Activity implements RadioManager.RadioListener {
    private static final String PREFS_NAME = "FmRadioPrefs";
    private static final String KEY_PRESET_PREFIX = "preset_freq_";

    private TextView tvFrequency;
    private TextView tvStationTitle;
    private TextView tvRdsSubtitle;
    private TextView tvSignalStrength;
    private TextView tvStereoIndicator;
    private VisualizerView visualizerView;
    private SeekBar sbFrequencyTuner;
    private SeekBar sbVolume;
    private Button btnPlayPause;
    private Button btnSeekDown;
    private Button btnSeekUp;
    private Button btnFineStepDown;
    private Button btnFineStepUp;
    private Button btnAutoScan;
    private Button btnSleepTimer;
    private TextView btnSavePreset;

    private Button[] presetButtons = new Button[6];

    private RadioManager radioManager;
    private AudioManager audioManager;
    private SharedPreferences prefs;
    private CountDownTimer sleepTimer;
    private boolean isScanning = false;
    private final Handler scanHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        radioManager = new RadioManager(this, this);

        initViews();
        setupListeners();
        loadPresetsUI();

        // Tune to default frequency
        radioManager.tuneTo(98.5);
    }

    private void initViews() {
        tvFrequency = (TextView) findViewById(R.id.tvFrequency);
        tvStationTitle = (TextView) findViewById(R.id.tvStationTitle);
        tvRdsSubtitle = (TextView) findViewById(R.id.tvRdsSubtitle);
        tvSignalStrength = (TextView) findViewById(R.id.tvSignalStrength);
        tvStereoIndicator = (TextView) findViewById(R.id.tvStereoIndicator);
        visualizerView = (VisualizerView) findViewById(R.id.visualizerView);
        sbFrequencyTuner = (SeekBar) findViewById(R.id.sbFrequencyTuner);
        sbVolume = (SeekBar) findViewById(R.id.sbVolume);
        btnPlayPause = (Button) findViewById(R.id.btnPlayPause);
        btnSeekDown = (Button) findViewById(R.id.btnSeekDown);
        btnSeekUp = (Button) findViewById(R.id.btnSeekUp);
        btnFineStepDown = (Button) findViewById(R.id.btnFineStepDown);
        btnFineStepUp = (Button) findViewById(R.id.btnFineStepUp);
        btnAutoScan = (Button) findViewById(R.id.btnAutoScan);
        btnSleepTimer = (Button) findViewById(R.id.btnSleepTimer);
        btnSavePreset = (TextView) findViewById(R.id.btnSavePreset);

        presetButtons[0] = (Button) findViewById(R.id.btnPreset1);
        presetButtons[1] = (Button) findViewById(R.id.btnPreset2);
        presetButtons[2] = (Button) findViewById(R.id.btnPreset3);
        presetButtons[3] = (Button) findViewById(R.id.btnPreset4);
        presetButtons[4] = (Button) findViewById(R.id.btnPreset5);
        presetButtons[5] = (Button) findViewById(R.id.btnPreset6);

        // Configure system media volume slider
        if (audioManager != null) {
            int maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            sbVolume.setMax(maxVol);
            sbVolume.setProgress(curVol);
        }
    }

    private void setupListeners() {
        btnPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                radioManager.togglePlayPause();
            }
        });

        btnSeekDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                radioManager.seekPrevious();
            }
        });

        btnSeekUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                radioManager.seekNext();
            }
        });

        btnFineStepDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                radioManager.stepFrequency(-0.1);
            }
        });

        btnFineStepUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                radioManager.stepFrequency(0.1);
            }
        });

        // Tuner Slider: 0 to 205 (87.5 + progress * 0.1)
        sbFrequencyTuner.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    double freq = 87.5 + (progress * 0.1);
                    radioManager.tuneTo(freq);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        sbVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && audioManager != null) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnAutoScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleAutoScan();
            }
        });

        btnSavePreset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSavePresetDialog();
            }
        });

        btnSleepTimer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSleepTimerDialog();
            }
        });

        // Preset button click handlers
        for (int i = 0; i < presetButtons.length; i++) {
            final int index = i;
            presetButtons[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    double freq = getPresetFrequency(index);
                    radioManager.tuneTo(freq);
                }
            });
        }
    }

    private void toggleAutoScan() {
        if (isScanning) {
            isScanning = false;
            btnAutoScan.setText(R.string.btn_scan);
            scanHandler.removeCallbacksAndMessages(null);
            Toast.makeText(this, "Auto-scan stopped", Toast.LENGTH_SHORT).show();
        } else {
            isScanning = true;
            btnAutoScan.setText("STOP");
            Toast.makeText(this, "Scanning FM frequencies...", Toast.LENGTH_SHORT).show();
            runScanLoop();
        }
    }

    private void runScanLoop() {
        if (!isScanning) return;

        radioManager.stepFrequency(0.2);
        RadioStation found = radioManager.findNearestStation(radioManager.getCurrentFrequency());

        if (found != null) {
            // Station found, lock and pause scan
            isScanning = false;
            btnAutoScan.setText(R.string.btn_scan);
            Toast.makeText(this, "Locked: " + found.getName(), Toast.LENGTH_SHORT).show();
            if (!radioManager.isPlaying()) {
                radioManager.playCurrent();
            }
            return;
        }

        scanHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                runScanLoop();
            }
        }, 120);
    }

    private double getPresetFrequency(int slot) {
        // Defaults if not set
        double[] defaults = { 88.5, 91.1, 94.9, 98.5, 101.3, 104.7 };
        float saved = prefs.getFloat(KEY_PRESET_PREFIX + slot, (float) defaults[slot]);
        return (double) saved;
    }

    private void loadPresetsUI() {
        for (int i = 0; i < presetButtons.length; i++) {
            double freq = getPresetFrequency(i);
            presetButtons[i].setText(String.format(Locale.US, "P%d\n%.1f", (i + 1), freq));
        }
    }

    private void showSavePresetDialog() {
        final String[] items = { "Preset 1", "Preset 2", "Preset 3", "Preset 4", "Preset 5", "Preset 6" };
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Save " + String.format(Locale.US, "%.1f MHz", radioManager.getCurrentFrequency()) + " to:");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                float currentFreq = (float) radioManager.getCurrentFrequency();
                prefs.edit().putFloat(KEY_PRESET_PREFIX + which, currentFreq).apply();
                loadPresetsUI();
                Toast.makeText(MainActivity.this, "Saved to Preset " + (which + 1), Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showSleepTimerDialog() {
        final String[] times = { "15 Minutes", "30 Minutes", "45 Minutes", "60 Minutes", "Turn Off Timer" };
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Sleep Timer");
        builder.setItems(times, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (sleepTimer != null) {
                    sleepTimer.cancel();
                    sleepTimer = null;
                }
                long millis = 0;
                switch (which) {
                    case 0: millis = 15 * 60 * 1000L; break;
                    case 1: millis = 30 * 60 * 1000L; break;
                    case 2: millis = 45 * 60 * 1000L; break;
                    case 3: millis = 60 * 60 * 1000L; break;
                    case 4:
                        btnSleepTimer.setText(R.string.sleep_timer);
                        Toast.makeText(MainActivity.this, "Sleep timer cancelled", Toast.LENGTH_SHORT).show();
                        return;
                }

                btnSleepTimer.setText("TIMER ON");
                Toast.makeText(MainActivity.this, "Radio will sleep in " + times[which], Toast.LENGTH_SHORT).show();

                sleepTimer = new CountDownTimer(millis, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        long mins = millisUntilFinished / 60000;
                        btnSleepTimer.setText(String.format(Locale.US, "%dm SLEEP", mins + 1));
                    }

                    @Override
                    public void onFinish() {
                        btnSleepTimer.setText(R.string.sleep_timer);
                        radioManager.pause();
                        Toast.makeText(MainActivity.this, "Sleep timer: Radio stopped", Toast.LENGTH_LONG).show();
                    }
                }.start();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    @Override
    public void onPlaybackStateChanged(boolean isPlaying, boolean isBuffering) {
        if (isBuffering) {
            btnPlayPause.setText("⏳");
            tvSignalStrength.setText("CONNECTING STREAM...");
            visualizerView.setPlaying(false);
        } else if (isPlaying) {
            btnPlayPause.setText("❚❚");
            visualizerView.setPlaying(true);
        } else {
            btnPlayPause.setText("▶");
            visualizerView.setPlaying(false);
        }
    }

    @Override
    public void onStationTuned(RadioStation station, double frequency, boolean isSignalLocked) {
        tvFrequency.setText(String.format(Locale.US, "%.1f", frequency));

        int progress = (int) Math.round((frequency - 87.5) * 10.0);
        sbFrequencyTuner.setProgress(progress);

        if (station != null && isSignalLocked) {
            tvStationTitle.setText(station.getName() + " • " + station.getGenre());
            tvRdsSubtitle.setText("Broadcasting live from " + station.getCity() + " • RDS Active");
            tvSignalStrength.setText("SIGNAL: 99% (STEREO LOCKED)");
            tvStereoIndicator.setText(R.string.stereo_label);
            tvStereoIndicator.setTextColor(getResources().getColor(R.color.lcd_glow));
        } else {
            tvStationTitle.setText("Frequency " + String.format(Locale.US, "%.1f MHz", frequency));
            tvRdsSubtitle.setText("Static Carrier • Unassigned FM Channel");
            tvSignalStrength.setText("SIGNAL: 12% (WEAK / NOISE)");
            tvStereoIndicator.setText(R.string.mono_label);
            tvStereoIndicator.setTextColor(getResources().getColor(R.color.text_secondary));
        }
    }

    @Override
    public void onError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sleepTimer != null) {
            sleepTimer.cancel();
        }
        scanHandler.removeCallbacksAndMessages(null);
        if (radioManager != null) {
            radioManager.release();
        }
    }
}