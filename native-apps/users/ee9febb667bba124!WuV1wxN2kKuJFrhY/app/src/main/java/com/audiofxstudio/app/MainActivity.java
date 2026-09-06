package com.audiofxstudio.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    // Models
    private static class HistoryItem {
        String name;
        String type;
        int freq;
        float duration;
        String timestamp;
    }

    // UI elements
    private Button btnLaser;
    private Button btnJump;
    private Button btnSiren;
    private Button btnCoin;
    private Button btnExplosion;
    private Button btnTonePulse;
    private Button btnGenerateCustom;
    private SeekBar sbFrequency;
    private TextView tvFreqValue;
    private SeekBar sbDuration;
    private TextView tvDurationValue;
    private CheckBox cbEnableLoader;
    private LinearLayout layoutLoader;
    private TextView tvLoaderStatus;
    private TextView tvStatusHelp;
    private Button btnClearHistory;
    private TextView tvHistoryEmpty;
    private LinearLayout llHistoryList;

    private List<HistoryItem> historyList = new ArrayList<HistoryItem>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind layouts
        btnLaser = (Button) findViewById(R.id.btnLaser);
        btnJump = (Button) findViewById(R.id.btnJump);
        btnSiren = (Button) findViewById(R.id.btnSiren);
        btnCoin = (Button) findViewById(R.id.btnCoin);
        btnExplosion = (Button) findViewById(R.id.btnExplosion);
        btnTonePulse = (Button) findViewById(R.id.btnTonePulse);
        btnGenerateCustom = (Button) findViewById(R.id.btnGenerateCustom);
        sbFrequency = (SeekBar) findViewById(R.id.sbFrequency);
        tvFreqValue = (TextView) findViewById(R.id.tvFreqValue);
        sbDuration = (SeekBar) findViewById(R.id.sbDuration);
        tvDurationValue = (TextView) findViewById(R.id.tvDurationValue);
        cbEnableLoader = (CheckBox) findViewById(R.id.cbEnableLoader);
        layoutLoader = (LinearLayout) findViewById(R.id.layoutLoader);
        tvLoaderStatus = (TextView) findViewById(R.id.tvLoaderStatus);
        tvStatusHelp = (TextView) findViewById(R.id.tvStatusHelp);
        btnClearHistory = (Button) findViewById(R.id.btnClearHistory);
        tvHistoryEmpty = (TextView) findViewById(R.id.tvHistoryEmpty);
        llHistoryList = (LinearLayout) findViewById(R.id.llHistoryList);

        // Set listeners for SeekBars
        sbFrequency.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int hz = progress + 100;
                tvFreqValue.setText(hz + " Hz");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        sbDuration.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float sec = (progress + 1) / 10.0f;
                tvDurationValue.setText(sec + "s");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Click handlers for pads
        btnLaser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerSoundPlayback("laser", 0, 0.4f, "Laser Zap ⚡", true);
            }
        });

        btnJump.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerSoundPlayback("jump", 0, 0.5f, "Retro Jump 🦘", true);
            }
        });

        btnSiren.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerSoundPlayback("siren", 0, 1.2f, "Alarm Siren 🚨", true);
            }
        });

        btnCoin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerSoundPlayback("coin", 0, 0.3f, "8-Bit Coin 🪙", true);
            }
        });

        btnExplosion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerSoundPlayback("explosion", 0, 0.8f, "Explosion 💥", true);
            }
        });

        btnTonePulse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerSoundPlayback("beep", 0, 0.25f, "Beep Tone 👾", true);
            }
        });

        // Click handler for Custom Synthesis
        btnGenerateCustom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int freq = sbFrequency.getProgress() + 100;
                float dur = (sbDuration.getProgress() + 1) / 10.0f;
                triggerSoundPlayback("custom", freq, dur, "Synthesized " + freq + "Hz", true);
            }
        });

        // Click handler for history clear
        btnClearHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearHistoryLogs();
            }
        });

        // Load state and build initial logs list
        loadHistory();
        renderHistoryList();
    }

    private void setButtonsEnabled(boolean enabled) {
        btnLaser.setEnabled(enabled);
        btnJump.setEnabled(enabled);
        btnSiren.setEnabled(enabled);
        btnCoin.setEnabled(enabled);
        btnExplosion.setEnabled(enabled);
        btnTonePulse.setEnabled(enabled);
        btnGenerateCustom.setEnabled(enabled);
        btnClearHistory.setEnabled(enabled);
    }

    private void triggerSoundPlayback(final String type, final int frequency, final float duration, final String displayName, boolean allowDelay) {
        boolean renderingDelayEnabled = cbEnableLoader.isChecked();

        if (renderingDelayEnabled && allowDelay) {
            // Show Loader UI
            layoutLoader.setVisibility(View.VISIBLE);
            tvLoaderStatus.setText("Synthesizing: " + displayName + "...");
            tvStatusHelp.setText("Generating 16-bit PCM sound wave buffer...");
            setButtonsEnabled(false);

            // Execute after processing delay
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    layoutLoader.setVisibility(View.GONE);
                    tvStatusHelp.setText("Playback active: " + displayName);
                    setButtonsEnabled(true);

                    // Execute Audio Track Playback
                    executeAudioPlayback(type, frequency, duration);

                    // Add play history
                    addHistoryItem(displayName, type, frequency, duration);
                }
            }, 1000); // 1.0 second synthesis rendering simulation delay
        } else {
            // Play instantly
            tvStatusHelp.setText("Playback active: " + displayName);
            executeAudioPlayback(type, frequency, duration);
            addHistoryItem(displayName, type, frequency, duration);
        }
    }

    private void executeAudioPlayback(final String type, final int frequency, final float durationSec) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                int sampleRate = 44100;
                int numSamples = (int) (durationSec * sampleRate);
                double[] sample = new double[numSamples];
                byte[] generatedSnd = new byte[2 * numSamples];

                double phase = 0.0;
                java.util.Random rand = new java.util.Random();

                for (int i = 0; i < numSamples; ++i) {
                    double progress = (double) i / numSamples;

                    if ("laser".equals(type)) {
                        // Dynamic frequency drop (Laser chirp)
                        double freq = 1800.0 - (1600.0 * progress);
                        phase += 2.0 * Math.PI * freq / sampleRate;
                        sample[i] = Math.sin(phase);
                    } else if ("jump".equals(type)) {
                        // Dynamic frequency sweep upward (retro hop)
                        double freq = 200.0 + (1300.0 * progress);
                        phase += 2.0 * Math.PI * freq / sampleRate;
                        sample[i] = Math.sin(phase);
                    } else if ("siren".equals(type)) {
                        // Oscillating frequency sweep
                        double freq = 800.0 + 350.0 * Math.sin(2.0 * Math.PI * 3.0 * progress);
                        phase += 2.0 * Math.PI * freq / sampleRate;
                        sample[i] = Math.sin(phase);
                    } else if ("coin".equals(type)) {
                        // Distinct dual step tone pitch shifts
                        double freq = (progress < 0.20) ? 987.77 : 1318.51;
                        phase += 2.0 * Math.PI * freq / sampleRate;
                        sample[i] = Math.sin(phase);
                    } else if ("explosion".equals(type)) {
                        // Sweep downward rumbles overlayed with noise components
                        double freq = 250.0 * (1.0 - progress);
                        phase += 2.0 * Math.PI * freq / sampleRate;
                        double noise = rand.nextDouble() * 2.0 - 1.0;
                        sample[i] = (Math.sin(phase) * (1.0 - progress)) + (noise * 0.45 * (1.0 - progress));
                    } else if ("beep".equals(type)) {
                        // Standard constant 8-bit pulse beep
                        double freq = 650.0;
                        phase += 2.0 * Math.PI * freq / sampleRate;
                        sample[i] = Math.sin(phase);
                    } else {
                        // User-custom slider tone wave
                        phase += 2.0 * Math.PI * frequency / sampleRate;
                        sample[i] = Math.sin(phase);
                    }
                }

                // Transform sound samples to 16-bit native linear PCM formats
                int idx = 0;
                for (int i = 0; i < numSamples; ++i) {
                    // Normalize standard scaling limits to fit maximum amplitude short values
                    short val = (short) (sample[i] * 32767);
                    generatedSnd[idx++] = (byte) (val & 0x00ff);
                    generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
                }

                // Render play track properties
                try {
                    AudioTrack audioTrack = new AudioTrack(
                            AudioManager.STREAM_MUSIC,
                            sampleRate,
                            AudioFormat.CHANNEL_OUT_MONO,
                            AudioFormat.ENCODING_PCM_16BIT,
                            generatedSnd.length,
                            AudioTrack.MODE_STATIC);
                    audioTrack.write(generatedSnd, 0, generatedSnd.length);
                    audioTrack.play();

                    // Lock loop block sleep checks for playback lifetimes before cleaning tracks
                    Thread.sleep((long) (durationSec * 1000) + 100);
                    audioTrack.release();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void addHistoryItem(String name, String type, int freq, float duration) {
        HistoryItem item = new HistoryItem();
        item.name = name;
        item.type = type;
        item.freq = freq;
        item.duration = duration;

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        item.timestamp = sdf.format(new Date());

        // Prepend new playback events first
        historyList.add(0, item);

        // Keep maximum lists size bounded
        if (historyList.size() > 20) {
            historyList.remove(historyList.size() - 1);
        }

        saveHistory();
        renderHistoryList();
    }

    private void saveHistory() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < historyList.size(); i++) {
            HistoryItem item = historyList.get(i);
            sb.append(item.name).append(";")
              .append(item.type).append(";")
              .append(item.freq).append(";")
              .append(item.duration).append(";")
              .append(item.timestamp);
            if (i < historyList.size() - 1) {
                sb.append("|");
            }
        }
        SharedPreferences.Editor editor = getSharedPreferences("AudioStudioPrefs", MODE_PRIVATE).edit();
        editor.putString("sound_log_data", sb.toString());
        editor.apply();
    }

    private void loadHistory() {
        historyList.clear();
        SharedPreferences prefs = getSharedPreferences("AudioStudioPrefs", MODE_PRIVATE);
        String raw = prefs.getString("sound_log_data", "");
        if (!raw.trim().isEmpty()) {
            String[] items = raw.split("\\|");
            for (String itemStr : items) {
                if (itemStr.trim().isEmpty()) continue;
                String[] parts = itemStr.split(";");
                if (parts.length >= 5) {
                    HistoryItem item = new HistoryItem();
                    item.name = parts[0];
                    item.type = parts[1];
                    try {
                        item.freq = Integer.parseInt(parts[2]);
                        item.duration = Float.parseFloat(parts[3]);
                    } catch (NumberFormatException e) {
                        item.freq = 440;
                        item.duration = 0.5f;
                    }
                    item.timestamp = parts[4];
                    historyList.add(item);
                }
            }
        }
    }

    private void renderHistoryList() {
        llHistoryList.removeAllViews();

        if (historyList.isEmpty()) {
            tvHistoryEmpty.setVisibility(View.VISIBLE);
        } else {
            tvHistoryEmpty.setVisibility(View.GONE);
            for (int i = 0; i < historyList.size(); i++) {
                final HistoryItem item = historyList.get(i);

                LinearLayout rowLayout = new LinearLayout(this);
                rowLayout.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                rowLayout.setLayoutParams(rowParams);
                rowLayout.setPadding(8, 10, 8, 10);
                rowLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);

                // Alternate list colors
                if (i % 2 == 0) {
                    rowLayout.setBackgroundColor(0xFFFAFAFA);
                } else {
                    rowLayout.setBackgroundColor(0xFFFFFFFF);
                }

                // Text properties column wrapper
                LinearLayout textCol = new LinearLayout(this);
                textCol.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams colParams = new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
                textCol.setLayoutParams(colParams);

                TextView tvName = new TextView(this);
                tvName.setText(item.name);
                tvName.setTextColor(0xFF2C3E50);
                tvName.setTextSize(14f);
                tvName.setTypeface(null, android.graphics.Typeface.BOLD);

                TextView tvMeta = new TextView(this);
                String details = "";
                if ("laser".equals(item.type) || "jump".equals(item.type) || "siren".equals(item.type) || "coin".equals(item.type) || "explosion".equals(item.type) || "beep".equals(item.type)) {
                    details = "Type: Preset FX | Duration: " + item.duration + "s | " + item.timestamp;
                } else {
                    details = "Type: Synth (" + item.freq + "Hz) | Duration: " + item.duration + "s | " + item.timestamp;
                }
                tvMeta.setText(details);
                tvMeta.setTextColor(0xFF7F8C8D);
                tvMeta.setTextSize(11f);

                textCol.addView(tvName);
                textCol.addView(tvMeta);

                // Inline Play/Replay trigger button
                Button btnReplay = new Button(this);
                btnReplay.setText("Play 🔊");
                btnReplay.setTextSize(11f);
                LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                btnReplay.setLayoutParams(btnParams);
                btnReplay.setPadding(12, 6, 12, 6);
                btnReplay.setBackgroundColor(0xFF34495E);
                btnReplay.setTextColor(0xFFFFFFFF);

                btnReplay.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // History list replays bypass simulated loaders for slick responsive replay speeds
                        triggerSoundPlayback(item.type, item.freq, item.duration, item.name, false);
                    }
                });

                rowLayout.addView(textCol);
                rowLayout.addView(btnReplay);

                llHistoryList.addView(rowLayout);

                // Divider line item row separators
                View div = new View(this);
                LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1);
                div.setLayoutParams(divParams);
                div.setBackgroundColor(0xFFECF0F1);
                llHistoryList.addView(div);
            }
        }
    }

    private void clearHistoryLogs() {
        historyList.clear();
        saveHistory();
        renderHistoryList();
        tvStatusHelp.setText("History logs cleared successfully.");
        Toast.makeText(this, "Logs Cleared", Toast.LENGTH_SHORT).show();
    }
}