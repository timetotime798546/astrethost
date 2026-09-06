package com.sonicwavelab.app;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class MainActivity extends Activity {

    private SoundVisualizerView soundVisualizer;
    private View ledIndicator;
    private RelativeLayout loaderOverlay;
    private TextView txtLoaderDetails;

    private SeekBar seekFrequency;
    private SeekBar seekDuration;
    private SeekBar seekEcho;

    private TextView txtFreqValue;
    private TextView txtDurationValue;
    private TextView txtEchoValue;

    private Button btnLaser;
    private Button btnJump;
    private Button btnSwell;
    private Button btnBell;
    private Button btnNoise;
    private Button btnClearHistory;

    private LinearLayout historyContainer;

    private HistoryDatabaseHelper dbHelper;
    private Handler mainHandler;
    private AudioTrack activeAudioTrack = null;

    private int baseFreqVal = 440;
    private double durationVal = 0.6;
    private int echoDecayVal = 150;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new HistoryDatabaseHelper(this);
        mainHandler = new Handler(Looper.getMainLooper());

        initViews();
        setupListeners();
        refreshHistoryView();
    }

    private void initViews() {
        soundVisualizer = (SoundVisualizerView) findViewById(R.id.soundVisualizer);
        ledIndicator = findViewById(R.id.ledIndicator);
        loaderOverlay = (RelativeLayout) findViewById(R.id.loaderOverlay);
        txtLoaderDetails = (TextView) findViewById(R.id.txtLoaderDetails);

        seekFrequency = (SeekBar) findViewById(R.id.seekFrequency);
        seekDuration = (SeekBar) findViewById(R.id.seekDuration);
        seekEcho = (SeekBar) findViewById(R.id.seekEcho);

        txtFreqValue = (TextView) findViewById(R.id.txtFreqValue);
        txtDurationValue = (TextView) findViewById(R.id.txtDurationValue);
        txtEchoValue = (TextView) findViewById(R.id.txtEchoValue);

        btnLaser = (Button) findViewById(R.id.btnSynthLaser);
        btnJump = (Button) findViewById(R.id.btnSynthJump);
        btnSwell = (Button) findViewById(R.id.btnSynthSwell);
        btnBell = (Button) findViewById(R.id.btnSynthBell);
        btnNoise = (Button) findViewById(R.id.btnSynthNoise);
        btnClearHistory = (Button) findViewById(R.id.btnClearHistory);

        historyContainer = (LinearLayout) findViewById(R.id.historyContainer);
    }

    private void setupListeners() {
        seekFrequency.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                baseFreqVal = progress + 100; // range 100Hz to 1900Hz
                txtFreqValue.setText(baseFreqVal + " Hz");
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekDuration.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                durationVal = (progress + 2) / 10.0; // range 0.2s to 2.0s
                txtDurationValue.setText(String.format(Locale.US, "%.1fs", durationVal));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekEcho.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                echoDecayVal = progress;
                txtEchoValue.setText(echoDecayVal + "ms");
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Trigger Synth Sound Actions with Processing Loaders
        btnLaser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateButtonClick(v);
                processAndPlayPreset("Laser Sweep", 0xFFFF1744);
            }
        });

        btnJump.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateButtonClick(v);
                processAndPlayPreset("Chiptune Jump", 0xFF29B6F6);
            }
        });

        btnSwell.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateButtonClick(v);
                processAndPlayPreset("Ambient Swell", 0xFFAB47BC);
            }
        });

        btnBell.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateButtonClick(v);
                processAndPlayPreset("Zen Bell", 0xFFFFEB3B);
            }
        });

        btnNoise.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateButtonClick(v);
                processAndPlayPreset("Cosmic Noise", 0xFFFF5722);
            }
        });

        btnClearHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dbHelper.clearAllHistory();
                refreshHistoryView();
                Toast.makeText(MainActivity.this, "Synthesis History Logs Cleared", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void animateButtonClick(View v) {
        ScaleAnimation anim = new ScaleAnimation(0.95f, 1.0f, 0.95f, 1.0f,
                v.getWidth() / 2.0f, v.getHeight() / 2.0f);
        anim.setDuration(120);
        v.startAnimation(anim);
    }

    private void processAndPlayPreset(final String name, final int color) {
        // Show loader to simulate dynamic filter loading/rendering (Effect Demonstration)
        loaderOverlay.setVisibility(View.VISIBLE);
        txtLoaderDetails.setText("Applying filters: Freq: " + baseFreqVal + "Hz, Echo: " + echoDecayVal + "ms");

        ledIndicator.setAlpha(1.0f);

        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                loaderOverlay.setVisibility(View.GONE);

                // Start active color pulsing visualizer wave
                soundVisualizer.triggerWave(color, 65f, 8f);

                // Generate Sound in Background Thread
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        synthesizeAndPlaySound(name, color);
                    }
                }).start();

                // Save to SQLite
                String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
                dbHelper.insertHistoryItem(name, baseFreqVal, durationVal, echoDecayVal, currentTime);

                // Update dynamic history list views
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        refreshHistoryView();
                    }
                });
            }
        }, 650); // Simulated delay to show loading animation and build user feel
    }

    private void synthesizeAndPlaySound(String name, int color) {
        int sampleRate = 44100;
        int totalSamples = (int) (durationVal * sampleRate);
        short[] buffer = new short[totalSamples];

        double baseFreq = baseFreqVal;
        double delaySamples = (echoDecayVal / 1000.0) * sampleRate;
        Random random = new Random();

        // Safe sound wave creation loops
        for (int i = 0; i < totalSamples; i++) {
            double t = (double) i / sampleRate;
            double currentFrequency = baseFreq;

            // Compute dynamic algorithm based on chosen Preset
            if (name.equals("Laser Sweep")) {
                // Descending laser pitch sweep
                double sweepFactor = 1.0 - (t / durationVal);
                currentFrequency = baseFreq * (0.1 + 0.9 * sweepFactor);
            } else if (name.equals("Chiptune Jump")) {
                // Fast rising sweep
                double sweepFactor = (t / durationVal);
                currentFrequency = baseFreq * (0.8 + 1.8 * sweepFactor);
            } else if (name.equals("Ambient Swell")) {
                // Soft sweeping waves with low vibrato
                currentFrequency = baseFreq + 35.0 * Math.sin(2.0 * Math.PI * 4.0 * t);
            } else if (name.equals("Zen Bell")) {
                // Decaying constant bell frequency
                currentFrequency = baseFreq;
            }

            double rawSample = 0.0;

            if (name.equals("Cosmic Noise")) {
                // Mix white noise and bandpassed wave frequencies
                double randomValue = (random.nextDouble() * 2.0) - 1.0;
                double sineValue = Math.sin(2.0 * Math.PI * currentFrequency * t);
                rawSample = 0.6 * randomValue + 0.4 * sineValue;
            } else {
                // standard Sine tone preset
                rawSample = Math.sin(2.0 * Math.PI * currentFrequency * t);
            }

            // Simple Echo filter delay simulation
            if (delaySamples > 0 && i > delaySamples) {
                int indexDelay = i - (int) delaySamples;
                rawSample = 0.7 * rawSample + 0.3 * (double) (buffer[indexDelay] / 32767.0);
            }

            // Dynamic volume fade envelope (ADSR windowing to prevent speaker pop)
            double envelope = 1.0;
            double attackTime = 0.04;
            double decayTime = 0.12;

            if (t < attackTime) {
                envelope = t / attackTime;
            } else if (t > durationVal - decayTime) {
                envelope = (durationVal - t) / decayTime;
            }

            // Additional Zen Bell natural volume damping
            if (name.equals("Zen Bell")) {
                envelope *= Math.exp(-4.5 * t);
            }

            double finalSample = rawSample * envelope;
            buffer[i] = (short) (finalSample * 32767.0);
        }

        // Play Synthesized Audio Track
        try {
            if (activeAudioTrack != null) {
                try {
                    activeAudioTrack.stop();
                    activeAudioTrack.release();
                } catch (Exception ignore) {}
            }

            activeAudioTrack = new AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    totalSamples * 2,
                    AudioTrack.MODE_STATIC
            );

            activeAudioTrack.write(buffer, 0, totalSamples);
            activeAudioTrack.play();

            // Active LED visual sync animation
            mainHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    ledIndicator.setAlpha(0.3f);
                }
            }, (long) (durationVal * 1000));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void refreshHistoryView() {
        historyContainer.removeAllViews();
        List<Map<String, String>> historyList = dbHelper.getAllHistory();

        if (historyList.isEmpty()) {
            TextView emptyTxt = new TextView(this);
            emptyTxt.setText("No tracks rendered yet. Choose a preset to start.");
            emptyTxt.setTextColor(Color.parseColor("#7F8C8D"));
            emptyTxt.setTextSize(13sp);
            emptyTxt.setPadding(16, 16, 16, 16);
            emptyTxt.setGravity(Gravity.CENTER);
            historyContainer.addView(emptyTxt);
            return;
        }

        for (int i = 0; i < historyList.size(); i++) {
            final Map<String, String> item = historyList.get(i);
            final String presetName = item.get("preset");
            final String pitchVal = item.get("pitch");
            final String durationStr = item.get("duration");
            final String echoStr = item.get("echo");
            final String timestamp = item.get("timestamp");

            // Build dynamic row layout
            LinearLayout itemLayout = new LinearLayout(this);
            itemLayout.setOrientation(LinearLayout.HORIZONTAL);
            itemLayout.setBackgroundColor(Color.WHITE);
            itemLayout.setPadding(16, 12, 16, 12);
            itemLayout.setGravity(Gravity.CENTER_VERTICAL);

            // Left colored tag indicator matching original trigger color
            View tagColor = new View(this);
            int dotColor = 0xFF29B6F6;
            if (presetName.contains("Laser")) dotColor = 0xFFFF1744;
            if (presetName.contains("Ambient")) dotColor = 0xFFAB47BC;
            if (presetName.contains("Bell")) dotColor = 0xFFFFEB3B;
            if (presetName.contains("Cosmic")) dotColor = 0xFFFF5722;
            tagColor.setBackgroundColor(dotColor);
            LinearLayout.LayoutParams tagParams = new LinearLayout.LayoutParams(8, 40);
            tagParams.setMargins(0, 0, 16, 0);
            tagColor.setLayoutParams(tagParams);
            itemLayout.addView(tagColor);

            // Information details Column
            LinearLayout textCol = new LinearLayout(this);
            textCol.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams textColParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            textCol.setLayoutParams(textColParams);

            TextView titleTxt = new TextView(this);
            titleTxt.setText(presetName + " Tone (" + timestamp + ")");
            titleTxt.setTextSize(14sp);
            titleTxt.setTextColor(Color.parseColor("#2C3E50"));
            titleTxt.setTextStyle(android.graphics.Typeface.BOLD);
            textCol.addView(titleTxt);

            TextView statsTxt = new TextView(this);
            statsTxt.setText("Pitch: " + pitchVal + "Hz | Duration: " + durationStr + "s | Delay: " + echoStr + "ms");
            statsTxt.setTextSize(11sp);
            statsTxt.setTextColor(Color.parseColor("#7F8C8D"));
            textCol.addView(statsTxt);

            itemLayout.addView(textCol);

            // Quick replay action button
            Button actionBtn = new Button(this);
            actionBtn.setText("▶ Replay");
            actionBtn.setTextSize(10sp);
            actionBtn.setTextColor(Color.parseColor("#FFFFFF"));
            actionBtn.setBackgroundColor(Color.parseColor("#2C3E50"));
            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, 70);
            actionBtn.setLayoutParams(btnParams);

            final int colorVal = dotColor;
            actionBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Update main seek values dynamically to display history parameters
                    baseFreqVal = Integer.parseInt(pitchVal);
                    durationVal = Double.parseDouble(durationStr);
                    echoDecayVal = Integer.parseInt(echoStr);

                    seekFrequency.setProgress(baseFreqVal - 100);
                    seekDuration.setProgress((int) (durationVal * 10 - 2));
                    seekEcho.setProgress(echoDecayVal);

                    txtFreqValue.setText(baseFreqVal + " Hz");
                    txtDurationValue.setText(String.format(Locale.US, "%.1fs", durationVal));
                    txtEchoValue.setText(echoDecayVal + "ms");

                    // Trigger dynamic sound replay instantly
                    soundVisualizer.triggerWave(colorVal, 50f, 9f);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            synthesizeAndPlaySound(presetName, colorVal);
                        }
                    }).start();

                    Toast.makeText(MainActivity.this, "Replaying: " + presetName, Toast.LENGTH_SHORT).show();
                }
            });

            itemLayout.addView(actionBtn);

            // Spacer divider line
            View divider = new View(this);
            divider.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2));
            divider.setBackgroundColor(Color.parseColor("#EAEDED"));

            // Dynamic entry effect animations (Fade-in and Slide-in effect on log load)
            AlphaAnimation rowAnim = new AlphaAnimation(0.0f, 1.0f);
            rowAnim.setDuration(350);
            itemLayout.startAnimation(rowAnim);

            historyContainer.addView(itemLayout);
            historyContainer.addView(divider);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Safe releases of Android Core hardware resources
        if (activeAudioTrack != null) {
            try {
                activeAudioTrack.stop();
                activeAudioTrack.release();
            } catch (Exception ignore) {}
        }
    }
}