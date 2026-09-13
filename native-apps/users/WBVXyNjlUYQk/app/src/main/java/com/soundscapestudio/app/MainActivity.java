package com.soundscapestudio.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import java.util.Random;

public class MainActivity extends Activity {

    // Volatile parameters updated dynamically from UI thread, read in real-time by Synthesizer Thread
    private volatile float valCarrier = 250f;
    private volatile float valBeat = 15f;
    private volatile float valVolBinaural = 0.5f;
    private volatile float valVolRain = 0.2f;
    private volatile float valVolOcean = 0.2f;
    private volatile float masterFade = 1.0f; // Smooth automation level
    private volatile boolean isPlaying = false;

    // Synthesis phase states to avoid frequency pop/glitch noises when parameters change
    private double phaseL = 0.0;
    private double phaseR = 0.0;
    private double phaseOcean = 0.0;
    private float rainFilterState = 0f;
    private float oceanFilterState = 0f;

    // UI Widgets
    private SoundWaveVisualizer visualizer;
    private TextView tvCarrierVal, tvBeatVal;
    private TextView tvVolBinaural, tvVolRain, tvVolOcean;
    private TextView tvTimerCountdown;
    private SeekBar sbCarrier, sbBeat;
    private SeekBar sbVolBinaural, sbVolRain, sbVolOcean;
    private Button btnPlayPause, btnInfo;
    private Button btnPresetFocus, btnPresetMeditation, btnPresetSleep, btnPresetGamma, btnPresetSchumann;
    private Button btnTimerOff, btnTimer15, btnTimer30, btnTimer60;

    // Thread references
    private Thread audioThread;

    // Sleep Timer states
    private int timerRemainingSeconds = 0;
    private Handler timerHandler = new Handler();
    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isPlaying || timerRemainingSeconds <= 0) {
                stopTimer();
                if (timerRemainingSeconds <= 0 && isPlaying) {
                    stopPlayback();
                }
                return;
            }

            timerRemainingSeconds--;

            // Handle smooth fader over the last 15 seconds
            if (timerRemainingSeconds <= 15) {
                masterFade = (float) timerRemainingSeconds / 15.0f;
                if (masterFade < 0f) masterFade = 0f;
            } else {
                masterFade = 1.0f;
            }

            updateTimerUI();
            timerHandler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind Views
        visualizer = (SoundWaveVisualizer) findViewById(R.id.wave_visualizer);
        tvCarrierVal = (TextView) findViewById(R.id.tv_carrier_val);
        tvBeatVal = (TextView) findViewById(R.id.tv_beat_val);
        tvVolBinaural = (TextView) findViewById(R.id.tv_vol_binaural);
        tvVolRain = (TextView) findViewById(R.id.tv_vol_rain);
        tvVolOcean = (TextView) findViewById(R.id.tv_vol_ocean);
        tvTimerCountdown = (TextView) findViewById(R.id.tv_timer_countdown);

        sbCarrier = (SeekBar) findViewById(R.id.sb_carrier);
        sbBeat = (SeekBar) findViewById(R.id.sb_beat);
        sbVolBinaural = (SeekBar) findViewById(R.id.sb_vol_binaural);
        sbVolRain = (SeekBar) findViewById(R.id.sb_vol_rain);
        sbVolOcean = (SeekBar) findViewById(R.id.sb_vol_ocean);

        btnPlayPause = (Button) findViewById(R.id.btn_play_pause);
        btnInfo = (Button) findViewById(R.id.btn_info);

        btnPresetFocus = (Button) findViewById(R.id.btn_preset_focus);
        btnPresetMeditation = (Button) findViewById(R.id.btn_preset_meditation);
        btnPresetSleep = (Button) findViewById(R.id.btn_preset_sleep);
        btnPresetGamma = (Button) findViewById(R.id.btn_preset_gamma);
        btnPresetSchumann = (Button) findViewById(R.id.btn_preset_schumann);

        btnTimerOff = (Button) findViewById(R.id.btn_timer_off);
        btnTimer15 = (Button) findViewById(R.id.btn_timer_15);
        btnTimer30 = (Button) findViewById(R.id.btn_timer_30);
        btnTimer60 = (Button) findViewById(R.id.btn_timer_60);

        // Bind Actions using traditional anonymous inner classes to comply with strict instructions (no lambdas)
        btnPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPlaying) {
                    stopPlayback();
                } else {
                    startPlayback();
                }
            }
        });

        btnInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showInfoDialog();
            }
        });

        setupSeekbars();
        setupPresets();
        setupTimers();

        // Initial setup update
        syncFrequenciesToVisualizer();
    }

    private void setupSeekbars() {
        // Carrier Range: 100 Hz to 1000 Hz. Max value of SeekBar represents (1000 - 100) = 900. Offset = 100.
        sbCarrier.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                valCarrier = progress + 100;
                tvCarrierVal.setText((int) valCarrier + " Hz");
                syncFrequenciesToVisualizer();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Beat Range: 1.0 Hz to 50.0 Hz. Max representation: 490 (progress scaled by 0.1) Offset = 1.0.
        sbBeat.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                valBeat = (progress / 10f) + 1.0f;
                tvBeatVal.setText(String.format("%.1f Hz", valBeat));
                syncFrequenciesToVisualizer();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Volume controls
        sbVolBinaural.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                valVolBinaural = progress / 100f;
                tvVolBinaural.setText(progress + "%");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        sbVolRain.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                valVolRain = progress / 100f;
                tvVolRain.setText(progress + "%");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        sbVolOcean.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                valVolOcean = progress / 100f;
                tvVolOcean.setText(progress + "%");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void setupPresets() {
        // Preset Focus (Beta) 15Hz, Base 250Hz
        btnPresetFocus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyPreset(250, 15.0f);
                highlightPresetButton(btnPresetFocus);
            }
        });

        // Preset Meditation (Theta) 6Hz, Base 180Hz
        btnPresetMeditation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyPreset(180, 6.0f);
                highlightPresetButton(btnPresetMeditation);
            }
        });

        // Preset Deep Sleep (Delta) 2.5Hz, Base 110Hz
        btnPresetSleep.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyPreset(110, 2.5f);
                highlightPresetButton(btnPresetSleep);
            }
        });

        // Preset Gamma Peak 40Hz, Base 320Hz
        btnPresetGamma.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyPreset(320, 40.0f);
                highlightPresetButton(btnPresetGamma);
            }
        });

        // Preset Schumann Resonance 7.83Hz, Base 141.2Hz
        btnPresetSchumann.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyPreset(141, 7.8f);
                highlightPresetButton(btnPresetSchumann);
            }
        });
    }

    private void highlightPresetButton(Button activeBtn) {
        btnPresetFocus.setBackgroundResource(R.drawable.button_inactive);
        btnPresetMeditation.setBackgroundResource(R.drawable.button_inactive);
        btnPresetSleep.setBackgroundResource(R.drawable.button_inactive);
        btnPresetGamma.setBackgroundResource(R.drawable.button_inactive);
        btnPresetSchumann.setBackgroundResource(R.drawable.button_inactive);

        activeBtn.setBackgroundResource(R.drawable.button_active);
    }

    private void applyPreset(int carrier, float beat) {
        sbCarrier.setProgress(carrier - 100);
        sbBeat.setProgress((int) ((beat - 1.0f) * 10f));

        valCarrier = carrier;
        valBeat = beat;

        tvCarrierVal.setText(carrier + " Hz");
        tvBeatVal.setText(beat + " Hz");

        syncFrequenciesToVisualizer();
    }

    private void setupTimers() {
        btnTimerOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectTimer(0, btnTimerOff);
            }
        });

        btnTimer15.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectTimer(15, btnTimer15);
            }
        });

        btnTimer30.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectTimer(30, btnTimer30);
            }
        });

        btnTimer60.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectTimer(60, btnTimer60);
            }
        });
    }

    private void selectTimer(int minutes, Button selectedBtn) {
        btnTimerOff.setBackgroundResource(R.drawable.button_inactive);
        btnTimer15.setBackgroundResource(R.drawable.button_inactive);
        btnTimer30.setBackgroundResource(R.drawable.button_inactive);
        btnTimer60.setBackgroundResource(R.drawable.button_inactive);

        selectedBtn.setBackgroundResource(R.drawable.button_active);

        stopTimer();
        masterFade = 1.0f;

        if (minutes > 0) {
            timerRemainingSeconds = minutes * 60;
            updateTimerUI();
            if (isPlaying) {
                timerHandler.postDelayed(timerRunnable, 1000);
            }
        } else {
            timerRemainingSeconds = 0;
            tvTimerCountdown.setText("Off");
        }
    }

    private void updateTimerUI() {
        if (timerRemainingSeconds <= 0) {
            tvTimerCountdown.setText("Off");
            return;
        }
        int mins = timerRemainingSeconds / 60;
        int secs = timerRemainingSeconds % 60;
        tvTimerCountdown.setText(String.format("%02d:%02d Remaining", mins, secs));
    }

    private void stopTimer() {
        timerHandler.removeCallbacks(timerRunnable);
    }

    private void syncFrequenciesToVisualizer() {
        visualizer.setFrequencies(valCarrier, valBeat);
    }

    private synchronized void startPlayback() {
        if (isPlaying) return;

        isPlaying = true;
        masterFade = 1.0f;
        visualizer.setPlaying(true);
        btnPlayPause.setText("DEACTIVATE SYNTHESIS ENGINE");
        btnPlayPause.setBackgroundResource(R.drawable.button_inactive);

        // Launch real-time high priority background audio thread
        audioThread = new Thread(new AudioSynthRunnable());
        audioThread.start();

        // If sleep timer configuration is active, restart counting task
        if (timerRemainingSeconds > 0) {
            timerHandler.postDelayed(timerRunnable, 1000);
        }
    }

    private synchronized void stopPlayback() {
        if (!isPlaying) return;

        isPlaying = false;
        visualizer.setPlaying(false);
        btnPlayPause.setText("ACTIVATE SYNTHESIS ENGINE");
        btnPlayPause.setBackgroundResource(R.drawable.button_active);

        stopTimer();

        if (audioThread != null) {
            try {
                audioThread.join(500); // Allow safe exit
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            audioThread = null;
        }
    }

    private void showInfoDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("SoundScape Binaural Science");
        builder.setMessage("Binaural beats are an auditory illusion perceived when two slightly different tone pitches are presented separately to each ear.\n\n" +
                "The auditory cortex processes these inputs, syncing its baseline EEG cycles to the frequency difference. This naturally alters focus, stress, or recovery.\n\n" +
                "Guidelines:\n" +
                "• Wear stereophonic headphones (mandatory for the effect to build).\n" +
                "• Set Base Carrier Pitch to adjust ambient warmth.\n" +
                "• Select your desired target state below.\n\n" +
                "Brainwave Guidelines:\n" +
                "• Delta (0.5–4 Hz): Deep, dreamless sleep and physiological restoration.\n" +
                "• Theta (4–8 Hz): Meditation, REM sleep, deep creative hypnagogia.\n" +
                "• Alpha (8–12 Hz): Clear, relaxed, calm alertness and quick learning.\n" +
                "• Beta (12–30 Hz): Logic processing, high focus, and critical thinking.\n" +
                "• Gamma (30–50 Hz): Quick mental processing, high retention, peak awareness.");
        builder.setPositiveButton("Dismiss", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.create().show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Release audio track and thread to preserve system resources on backgrounding
        stopPlayback();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopPlayback();
    }

    // High fidelity real-time pure stereophonic audio synthesising background thread
    private class AudioSynthRunnable implements Runnable {
        @Override
        public void run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);

            int sampleRate = 44100;
            int channelConfig = AudioFormat.CHANNEL_OUT_STEREO;
            int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
            int minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat);
            int bufferSize = Math.max(minBufferSize, 4096);

            AudioTrack track = null;
            try {
                track = new AudioTrack(
                        AudioManager.STREAM_MUSIC,
                        sampleRate,
                        channelConfig,
                        audioFormat,
                        bufferSize,
                        AudioTrack.MODE_STREAM
                );
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }

            short[] buffer = new short[bufferSize];
            Random random = new Random();

            try {
                track.play();
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    track.release();
                } catch (Exception ex) {}
                return;
            }

            while (isPlaying) {
                // Safely load active properties from volatile states
                float currCarrier = valCarrier;
                float currBeat = valBeat;
                float currBinauralVol = valVolBinaural * masterFade;
                float currRainVol = valVolRain * masterFade;
                float currOceanVol = valVolOcean * masterFade;

                // Stereophonic channel distribution
                float freqL = currCarrier - (currBeat / 2.0f);
                float freqR = currCarrier + (currBeat / 2.0f);

                double incrementL = (2.0 * Math.PI * freqL) / sampleRate;
                double incrementR = (2.0 * Math.PI * freqR) / sampleRate;

                for (int i = 0; i < bufferSize; i += 2) {
                    // Left wave phase
                    phaseL += incrementL;
                    if (phaseL > 2.0 * Math.PI) phaseL -= 2.0 * Math.PI;

                    // Right wave phase
                    phaseR += incrementR;
                    if (phaseR > 2.0 * Math.PI) phaseR -= 2.0 * Math.PI;

                    // Sine signals
                    float signalL = (float) Math.sin(phaseL);
                    float signalR = (float) Math.sin(phaseR);

                    // Procedural Soft Rain Generator: Low-passed White Noise
                    float rawRain = (random.nextFloat() - 0.5f) * 2.0f;
                    // Low pass filtering block
                    rainFilterState = rainFilterState + 0.12f * (rawRain - rainFilterState);
                    float rainOut = rainFilterState;

                    // Procedural Ocean Wave Synthesizer: Low Frequency Oscillating pinkish noise
                    phaseOcean += (2.0 * Math.PI * 0.05) / sampleRate; // Extremely slow swell (approx 20 seconds period)
                    if (phaseOcean > 2.0 * Math.PI) phaseOcean -= 2.0 * Math.PI;
                    float waveSwell = (float) (Math.sin(phaseOcean) + 1.0) / 2.0f;

                    float rawOcean = (random.nextFloat() - 0.5f) * 2.0f;
                    // Extra heavy filter for deep low rumble frequencies
                    oceanFilterState = oceanFilterState + 0.035f * (rawOcean - oceanFilterState);
                    float oceanOut = oceanFilterState * (0.25f + 0.75f * waveSwell);

                    // Gain and safety limitation
                    float mixedL = (signalL * currBinauralVol) + (rainOut * currRainVol * 0.75f) + (oceanOut * currOceanVol * 0.95f);
                    float mixedR = (signalR * currBinauralVol) + (rainOut * currRainVol * 0.75f) + (oceanOut * currOceanVol * 0.95f);

                    // Clamp to prevent digital popping/clipping
                    if (mixedL > 1.0f) mixedL = 1.0f;
                    else if (mixedL < -1.0f) mixedL = -1.0f;

                    if (mixedR > 1.0f) mixedR = 1.0f;
                    else if (mixedR < -1.0f) mixedR = -1.0f;

                    // Convert to 16-bit PCM short values
                    buffer[i] = (short) (mixedL * 32767.0f);
                    buffer[i + 1] = (short) (mixedR * 32767.0f);
                }

                track.write(buffer, 0, bufferSize);
            }

            try {
                track.stop();
                track.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}