package com.ultimatestopwatch.app;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.media.ToneGenerator;
import android.media.AudioManager;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    // Visual elements
    private StopwatchProgressView progressLoader;
    private TextView tvMainTime;
    private TextView tvMsTime;
    private Button btnLapReset;
    private Button btnStartPause;
    private Button btnSoundToggle;
    private ListView lvLaps;

    // Timing Engine State fields
    private boolean running = false;
    private long startTime = 0L;
    private long accumulatedTime = 0L;
    private long lastLapTotalTime = 0L;
    private long lastSecondTicked = -1L;
    private boolean soundEnabled = true;

    // Sound generation client
    private ToneGenerator toneGen;

    // Lap List Tracking
    private final List<Lap> lapsList = new ArrayList<Lap>();
    private LapAdapter lapAdapter;

    // Time update thread loop
    private final Handler handler = new Handler();
    private final Runnable updateTimeRunnable = new Runnable() {
        @Override
        public void run() {
            if (running) {
                long currentElapsed = System.currentTimeMillis() - startTime + accumulatedTime;
                updateDisplay(currentElapsed);

                // Check sound ticking requirements based on whole second changes
                long currentSeconds = currentElapsed / 1000;
                if (currentSeconds > lastSecondTicked) {
                    lastSecondTicked = currentSeconds;
                    playTickSound();
                }

                // Smooth refresh visual updates every 20ms
                handler.postDelayed(this, 20);
            }
        }
    };

    // Represent individual Lap Records
    private static class Lap {
        int id;
        long lapTime;
        long totalTime;

        Lap(int id, long lapTime, long totalTime) {
            this.id = id;
            this.lapTime = lapTime;
            this.totalTime = totalTime;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind layout views
        progressLoader = (StopwatchProgressView) findViewById(R.id.progress_loader);
        tvMainTime = (TextView) findViewById(R.id.tv_main_time);
        tvMsTime = (TextView) findViewById(R.id.tv_ms_time);
        btnLapReset = (Button) findViewById(R.id.btn_lap_reset);
        btnStartPause = (Button) findViewById(R.id.btn_start_pause);
        btnSoundToggle = (Button) findViewById(R.id.btn_sound_toggle);
        lvLaps = (ListView) findViewById(R.id.lv_laps);

        // Setup custom adapters
        lapAdapter = new LapAdapter();
        lvLaps.setAdapter(lapAdapter);

        // Lazy initialize tone players safely
        try {
            toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 60); // 60% Volume setting
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Set Click Listeners (Strict Java 8 Anonymous Inner Class format)
        btnStartPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleTimer();
            }
        });

        btnLapReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleLapOrReset();
            }
        });

        btnSoundToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleSound();
            }
        });

        // Initialize state view representation
        updateDisplay(0L);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Release Tone generators to preserve hardware channels
        if (toneGen != null) {
            toneGen.release();
            toneGen = null;
        }
        handler.removeCallbacks(updateTimeRunnable);
    }

    private void toggleTimer() {
        if (!running) {
            // START
            startTime = System.currentTimeMillis();
            running = true;
            handler.post(updateTimeRunnable);

            btnStartPause.setText("PAUSE");
            btnStartPause.setBackgroundResource(R.drawable.btn_red);

            btnLapReset.setText("LAP");
            btnLapReset.setEnabled(true);

            playNotificationTone(ToneGenerator.TONE_PROP_BEEP);
        } else {
            // PAUSE
            running = false;
            accumulatedTime += System.currentTimeMillis() - startTime;
            handler.removeCallbacks(updateTimeRunnable);

            btnStartPause.setText("RESUME");
            btnStartPause.setBackgroundResource(R.drawable.btn_green);

            btnLapReset.setText("RESET");
            btnLapReset.setEnabled(true);

            playNotificationTone(ToneGenerator.TONE_PROP_BEEP2);
        }
    }

    private void handleLapOrReset() {
        if (running) {
            // LAP TRIGGER
            long currentElapsed = System.currentTimeMillis() - startTime + accumulatedTime;
            long currentLapTime = currentElapsed - lastLapTotalTime;
            lastLapTotalTime = currentElapsed;

            int lapNumber = lapsList.size() + 1;
            lapsList.add(0, new Lap(lapNumber, currentLapTime, currentElapsed)); // Add to top
            lapAdapter.notifyDataSetChanged();

            playNotificationTone(ToneGenerator.TONE_CDMA_PIP);
        } else {
            // RESET TRIGGER
            running = false;
            handler.removeCallbacks(updateTimeRunnable);

            startTime = 0L;
            accumulatedTime = 0L;
            lastLapTotalTime = 0L;
            lastSecondTicked = -1L;

            lapsList.clear();
            lapAdapter.notifyDataSetChanged();

            updateDisplay(0L);

            btnStartPause.setText("START");
            btnStartPause.setBackgroundResource(R.drawable.btn_green);

            btnLapReset.setText("LAP");
            btnLapReset.setEnabled(false);

            playNotificationTone(ToneGenerator.TONE_CDMA_HIGH_L);
        }
    }

    private void toggleSound() {
        soundEnabled = !soundEnabled;
        if (soundEnabled) {
            btnSoundToggle.setText("Sound: ON");
            playNotificationTone(ToneGenerator.TONE_PROP_BEEP);
        } else {
            btnSoundToggle.setText("Sound: OFF");
        }
    }

    private void playTickSound() {
        if (!soundEnabled || toneGen == null) return;
        try {
            toneGen.startTone(ToneGenerator.TONE_CDMA_PIP, 20); // short tick pulse
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void playNotificationTone(int toneType) {
        if (!soundEnabled || toneGen == null) return;
        try {
            toneGen.startTone(toneType, 120);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateDisplay(long elapsedMs) {
        // Format layout strings
        long min = (elapsedMs / 1000) / 60;
        long sec = (elapsedMs / 1000) % 60;
        long msPart = (elapsedMs % 1000) / 10; // display hundredths of a second

        String timeText = String.format("%02d:%02d", min, sec);
        String msText = String.format(".%02d", msPart);

        tvMainTime.setText(timeText);
        tvMsTime.setText(msText);

        // Update progress rings. Revolution loop takes exactly 60 seconds
        float progress = (float) (elapsedMs % 60000) / 60000f;
        progressLoader.setProgress(progress);
    }

    // Custom adapter rendering lap listings
    private class LapAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return lapsList.size();
        }

        @Override
        public Object getItem(int position) {
            return lapsList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.lap_item, parent, false);
            }

            Lap lap = lapsList.get(position);

            TextView tvNumber = (TextView) convertView.findViewById(R.id.tv_lap_number);
            TextView tvSplit = (TextView) convertView.findViewById(R.id.tv_lap_split);
            TextView tvTotal = (TextView) convertView.findViewById(R.id.tv_lap_total);

            tvNumber.setText("Lap " + lap.id);
            tvSplit.setText(formatTimeText(lap.lapTime));
            tvTotal.setText(formatTimeText(lap.totalTime));

            return convertView;
        }

        private String formatTimeText(long ms) {
            long min = (ms / 1000) / 60;
            long sec = (ms / 1000) % 60;
            long msPart = (ms % 1000) / 10;
            return String.format("%02d:%02d.%02d", min, sec, msPart);
        }
    }
}