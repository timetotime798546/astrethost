package com.aethercastemfsonifier.app;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;

public class MainActivity extends Activity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor magnetometer;
    private Vibrator vibrator;

    // Custom View Widget
    private EMFVisualizerView emfVisualizerView;

    // Layout components
    private TextView tvTotalEmf;
    private TextView tvVectorX;
    private TextView tvVectorY;
    private TextView tvVectorZ;
    private TextView tvFreqMultiplierLabel;
    private TextView tvSensitivityLabel;
    private TextView tvAlarmStatus;
    private TextView tvAlarmThresholdLabel;
    private TextView tvTerminalStats;

    private Button btnWaveSine;
    private Button btnWaveSquare;
    private Button btnWaveTri;
    private Button btnAudioToggle;
    private Button btnAlarmToggle;
    private Button btnZeroCalibrate;
    private Button btnClearLog;

    private SeekBar sbFreqMultiplier;
    private SeekBar sbSensitivity;
    private SeekBar sbAlarmThreshold;

    // Audio variables
    private AudioTrack audioTrack;
    private boolean isAudioPlaying = false;
    private final int SAMPLE_RATE = 44100;
    private Thread audioThread;
    private final Object audioLock = new Object();

    // Synthesizer frequency calibration limits
    private float targetFrequency = 220.0f;
    private float targetVolume = 0.0f;
    private int selectedWaveType = 0; // 0 = Sine, 1 = Square, 2 = Triangle / Sawtooth

    // App physics configurations
    private float sensitivityFactor = 1.0f;
    private int freqMultiplier = 10;
    private float calibratedZeroOffset_X = 0f;
    private float calibratedZeroOffset_Y = 0f;
    private float calibratedZeroOffset_Z = 0f;

    // Alarm configuration
    private boolean isAlarmEnabled = false;
    private int alarmThresholdMicroTesla = 120;
    private boolean isFlashStateActive = false;

    // Peak statistics logger
    private float peakMagnitude = 0f;
    private long anomalyCounter = 0;
    private DecimalFormat decFormat = new DecimalFormat("0.00");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Retrieve native services
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        }
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // Bind layouts
        emfVisualizerView = (EMFVisualizerView) findViewById(R.id.emf_visualizer_view);
        tvTotalEmf = (TextView) findViewById(R.id.tv_total_emf);
        tvVectorX = (TextView) findViewById(R.id.tv_vector_x);
        tvVectorY = (TextView) findViewById(R.id.tv_vector_y);
        tvVectorZ = (TextView) findViewById(R.id.tv_vector_z);
        tvFreqMultiplierLabel = (TextView) findViewById(R.id.tv_freq_multiplier_label);
        tvSensitivityLabel = (TextView) findViewById(R.id.tv_sensitivity_label);
        tvAlarmStatus = (TextView) findViewById(R.id.tv_alarm_status);
        tvAlarmThresholdLabel = (TextView) findViewById(R.id.tv_alarm_threshold_label);
        tvTerminalStats = (TextView) findViewById(R.id.tv_terminal_stats);

        btnWaveSine = (Button) findViewById(R.id.btn_wave_sine);
        btnWaveSquare = (Button) findViewById(R.id.btn_wave_square);
        btnWaveTri = (Button) findViewById(R.id.btn_wave_tri);
        btnAudioToggle = (Button) findViewById(R.id.btn_audio_toggle);
        btnAlarmToggle = (Button) findViewById(R.id.btn_alarm_toggle);
        btnZeroCalibrate = (Button) findViewById(R.id.btn_zero_calibrate);
        btnClearLog = (Button) findViewById(R.id.btn_clear_log);

        sbFreqMultiplier = (SeekBar) findViewById(R.id.sb_freq_multiplier);
        sbSensitivity = (SeekBar) findViewById(R.id.sb_sensitivity);
        sbAlarmThreshold = (SeekBar) findViewById(R.id.sb_alarm_threshold);

        // Initialize actions
        setupInteractionListeners();

        if (magnetometer == null) {
            Toast.makeText(this, "Hardware Magnetometer (Compass) sensor not detected!", Toast.LENGTH_LONG).show();
            tvTerminalStats.setText("CRITICAL HARDWARE FAIL:\nCompass sensor missing. Static simulation enabled.\n");
        }
    }

    private void setupInteractionListeners() {

        // Choose waveforms
        btnWaveSine.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setWaveformType(0);
            }
        });

        btnWaveSquare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setWaveformType(1);
            }
        });

        btnWaveTri.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setWaveformType(2);
            }
        });

        // Toggle real-time synthesized audio generator
        btnAudioToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleSynthesizerAudio();
            }
        });

        // Alarm toggler
        btnAlarmToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isAlarmEnabled = !isAlarmEnabled;
                if (isAlarmEnabled) {
                    btnAlarmToggle.setText("DEACTIVATE");
                    btnAlarmToggle.setBackgroundColor(0xFF4CD964);
                    tvAlarmStatus.setText("Vibe / Screen Flash Alert: ON");
                    appendTerminalLog("Alarm activated above " + alarmThresholdMicroTesla + " µT.");
                } else {
                    btnAlarmToggle.setText("ACTIVATE");
                    btnAlarmToggle.setBackgroundColor(0xFFFF3B30);
                    tvAlarmStatus.setText("Vibe / Screen Flash Alert: OFF");
                    appendTerminalLog("Threshold monitoring disabled.");
                }
            }
        });

        // Calibrate button - Sets baseline offsets dynamically to zero-out environmental fields
        btnZeroCalibrate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calibratedZeroOffset_X = currentRawX;
                calibratedZeroOffset_Y = currentRawY;
                calibratedZeroOffset_Z = currentRawZ;
                appendTerminalLog("Calibrated baseline dynamic zero-out complete!");
            }
        });

        // Clear statistics logs
        btnClearLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                peakMagnitude = 0;
                anomalyCounter = 0;
                calibratedZeroOffset_X = 0f;
                calibratedZeroOffset_Y = 0f;
                calibratedZeroOffset_Z = 0f;
                emfVisualizerView.clearHistory();
                tvTerminalStats.setText("Logs, history records and baseline calibrations reset.\n");
            }
        });

        // Frequency adjustment slider
        sbFreqMultiplier.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                freqMultiplier = Math.max(1, progress);
                int minRange = freqMultiplier * 20;
                int maxRange = freqMultiplier * 150;
                tvFreqMultiplierLabel.setText("Frequency Multiplier: " + freqMultiplier + "x (" + minRange + "Hz - " + maxRange + "Hz)");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Volume amplitude gain scaling slider
        sbSensitivity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                sensitivityFactor = progress / 20.0f;
                tvSensitivityLabel.setText("Gain Sensitivity: " + decFormat.format(sensitivityFactor) + "x");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Alarm slider settings
        sbAlarmThreshold.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                alarmThresholdMicroTesla = progress;
                tvAlarmThresholdLabel.setText("Threshold Level: " + alarmThresholdMicroTesla + " µT");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void setWaveformType(int type) {
        synchronized (audioLock) {
            selectedWaveType = type;
        }
        btnWaveSine.setBackgroundColor(0xFF0A0F14);
        btnWaveSine.setTextColor(0xFFFFFFFF);
        btnWaveSquare.setBackgroundColor(0xFF0A0F14);
        btnWaveSquare.setTextColor(0xFFFFFFFF);
        btnWaveTri.setBackgroundColor(0xFF0A0F14);
        btnWaveTri.setTextColor(0xFFFFFFFF);

        if (type == 0) {
            btnWaveSine.setBackgroundColor(0xFF121A24);
            btnWaveSine.setTextColor(0xFF00FFCC);
            appendTerminalLog("Oscillator set to Pure Sine Wave.");
        } else if (type == 1) {
            btnWaveSquare.setBackgroundColor(0xFF121A24);
            btnWaveSquare.setTextColor(0xFF00FFCC);
            appendTerminalLog("Oscillator set to Raw Square Wave.");
        } else {
            btnWaveTri.setBackgroundColor(0xFF121A24);
            btnWaveTri.setTextColor(0xFF00FFCC);
            appendTerminalLog("Oscillator set to Hybrid Triangle Sawtooth.");
        }
    }

    private void toggleSynthesizerAudio() {
        if (isAudioPlaying) {
            stopSoundSynthesis();
            btnAudioToggle.setText("START AUDIO");
            btnAudioToggle.setBackgroundColor(0xFFFF8800);
            appendTerminalLog("Synthesis audio engine deactivated.");
        } else {
            startSoundSynthesis();
            btnAudioToggle.setText("MUTE AUDIO");
            btnAudioToggle.setBackgroundColor(0xFF00FFCC);
            appendTerminalLog("Synthesis audio engine running...");
        }
    }

    private void startSoundSynthesis() {
        synchronized (audioLock) {
            isAudioPlaying = true;
        }
        audioThread = new Thread(new Runnable() {
            @Override
            public void run() {
                int minBufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT);

                if (minBufferSize == AudioTrack.ERROR || minBufferSize == AudioTrack.ERROR_BAD_VALUE) {
                    minBufferSize = 8192;
                }

                audioTrack = new AudioTrack(
                        AudioManager.STREAM_MUSIC,
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        minBufferSize,
                        AudioTrack.MODE_STREAM);

                try {
                    audioTrack.play();
                } catch (IllegalStateException e) {
                    return;
                }

                short[] audioBuffer = new short[minBufferSize / 2];
                double phaseAngle = 0.0;

                while (true) {
                    boolean runState;
                    float freq;
                    float vol;
                    int type;

                    synchronized (audioLock) {
                        runState = isAudioPlaying;
                        freq = targetFrequency;
                        vol = targetVolume;
                        type = selectedWaveType;
                    }

                    if (!runState) {
                        break;
                    }

                    for (int i = 0; i < audioBuffer.length; i++) {
                        double sample = 0.0;

                        if (type == 0) {
                            // Sine wave
                            sample = Math.sin(phaseAngle);
                        } else if (type == 1) {
                            // Square wave
                            sample = phaseAngle < Math.PI ? 1.0 : -1.0;
                        } else {
                            // Sawtooth / Triangle hybrid wave
                            sample = (phaseAngle / Math.PI) - 1.0;
                        }

                        // Assign sample scaled to standard 16-bit PCM integer boundaries
                        audioBuffer[i] = (short) (sample * vol * 32767.0);

                        // Increment tracking dynamic phase safely to stop static distortion popping
                        double increment = (2.0 * Math.PI * freq) / SAMPLE_RATE;
                        phaseAngle += increment;
                        if (phaseAngle >= 2.0 * Math.PI) {
                            phaseAngle -= 2.0 * Math.PI;
                        }
                    }

                    audioTrack.write(audioBuffer, 0, audioBuffer.length);
                }

                try {
                    audioTrack.stop();
                    audioTrack.release();
                } catch (Exception ignored) {}
            }
        });
        audioThread.start();
    }

    private void stopSoundSynthesis() {
        synchronized (audioLock) {
            isAudioPlaying = false;
        }
        if (audioThread != null) {
            try {
                audioThread.join(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            audioThread = null;
        }
    }

    // Capture raw hardware magnetometer levels
    private float currentRawX = 0f;
    private float currentRawY = 0f;
    private float currentRawZ = 0f;

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            currentRawX = event.values[0];
            currentRawY = event.values[1];
            currentRawZ = event.values[2];

            // Substract the calibrated offset
            float finalX = currentRawX - calibratedZeroOffset_X;
            float finalY = currentRawY - calibratedZeroOffset_Y;
            float finalZ = currentRawZ - calibratedZeroOffset_Z;

            // Compute direct absolute 3D Pythagorean magnetic vector magnitude
            final float absoluteEMF = (float) Math.sqrt(finalX * finalX + finalY * finalY + finalZ * finalZ);

            // Display numerical telemetry reads
            tvTotalEmf.setText(decFormat.format(absoluteEMF));
            tvVectorX.setText("X: " + decFormat.format(finalX) + " µT");
            tvVectorY.setText("Y: " + decFormat.format(finalY) + " µT");
            tvVectorZ.setText("Z: " + decFormat.format(finalZ) + " µT");

            // Update custom oscillogram widget
            emfVisualizerView.updateData(finalX, finalY, finalZ, absoluteEMF);

            // Synthesizer logic modulation: map magnetic flux to raw synthesizer parameters
            // Standard background radiation is around 30 to 60 µT
            // Frequency range modulated by multiplication factor bounds
            float freshFrequency = 200f + (absoluteEMF * freqMultiplier);
            if (freshFrequency > 4000f) freshFrequency = 4000f; // Human hearing safety threshold clamp

            // Volume derived directly from EMF severity scaled by gain sensitivity
            float freshVolume = (absoluteEMF / 100.0f) * sensitivityFactor;
            if (freshVolume > 1.0f) freshVolume = 1.0f; // Full scale amplitude saturation ceiling clamp
            if (freshVolume < 0.01f) freshVolume = 0.0f; // Silence low ambient noise completely

            synchronized (audioLock) {
                targetFrequency = freshFrequency;
                targetVolume = freshVolume;
            }

            // Real-time alarm monitoring
            monitorAnomalyBounds(absoluteEMF);
        }
    }

    private void monitorAnomalyBounds(float currentAbsoluteEMF) {
        // Log peak statistics
        if (currentAbsoluteEMF > peakMagnitude) {
            peakMagnitude = currentAbsoluteEMF;
        }

        if (isAlarmEnabled && currentAbsoluteEMF > alarmThresholdMicroTesla) {
            anomalyCounter++;
            if (anomalyCounter % 15 == 0) { // Throttle notifications and vibration pulses
                appendTerminalLog("EMF PEAK DETECTED: " + decFormat.format(currentAbsoluteEMF) + " µT! Above critical trigger.");
                triggerHardwareVibeAlert();
                toggleFlashVisualState(true);
            }
        } else {
            if (isFlashStateActive) {
                toggleFlashVisualState(false);
            }
        }
    }

    private void triggerHardwareVibeAlert() {
        if (vibrator != null) {
            try {
                // Compatible vibrate pattern for standard permissions
                vibrator.vibrate(120);
            } catch (Exception ignored) {}
        }
    }

    private void toggleFlashVisualState(boolean active) {
        isFlashStateActive = active;
        if (active) {
            tvTotalEmf.setTextColor(Color.YELLOW);
        } else {
            tvTotalEmf.setTextColor(0xFFFF3B30);
        }
    }

    private void appendTerminalLog(String message) {
        String currentLogs = tvTerminalStats.getText().toString();
        // Keep logs compact
        if (currentLogs.length() > 500) {
            currentLogs = currentLogs.substring(0, 250) + "\n... (recycled records) ...\n";
        }
        String updatedLogs = "» " + message + "\n" + currentLogs;
        tvTerminalStats.setText(updatedLogs);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No-op required by interface contract
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null && magnetometer != null) {
            sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
        }
        if (isAudioPlaying) {
            startSoundSynthesis();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        stopSoundSynthesis();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopSoundSynthesis();
    }
}