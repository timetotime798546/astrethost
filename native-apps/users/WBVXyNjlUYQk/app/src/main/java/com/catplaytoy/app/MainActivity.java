package com.catplaytoy.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    private int score = 0;
    private int highScore = 0;
    private SharedPreferences sharedPreferences;

    private TextView tvScore;
    private FrameLayout gameContainer;
    private View layoutPiano;
    private View layoutSounds;

    private Button btnTabLaser;
    private Button btnTabFish;
    private Button btnTabPiano;
    private Button btnTabSounds;

    private CatToyView toyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = getSharedPreferences("CatPlayToyPrefs", Context.MODE_PRIVATE);
        highScore = sharedPreferences.getInt("HighScore", 0);

        tvScore = (TextView) findViewById(R.id.tvScore);
        gameContainer = (FrameLayout) findViewById(R.id.gameContainer);
        layoutPiano = findViewById(R.id.layoutPiano);
        layoutSounds = findViewById(R.id.layoutSounds);

        btnTabLaser = (Button) findViewById(R.id.btnTabLaser);
        btnTabFish = (Button) findViewById(R.id.btnTabFish);
        btnTabPiano = (Button) findViewById(R.id.btnTabPiano);
        btnTabSounds = (Button) findViewById(R.id.btnTabSounds);

        updateScoreDisplay();

        // Inject Custom Toy View
        toyView = new CatToyView(this);
        gameContainer.addView(toyView);

        toyView.setOnToyTouchListener(new CatToyView.OnToyTouchListener() {
            @Override
            public void onToyCaught(String type) {
                incrementScore();
                if ("laser".equals(type)) {
                    playSqueak();
                } else {
                    playWobble();
                }
            }
        });

        btnTabLaser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab("laser");
            }
        });

        btnTabFish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab("fish");
            }
        });

        btnTabPiano.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab("piano");
            }
        });

        btnTabSounds.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab("sounds");
            }
        });

        // Cat Piano Clicks
        findViewById(R.id.pianoC).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { playMeow(0.7); } });
        findViewById(R.id.pianoD).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { playMeow(0.8); } });
        findViewById(R.id.pianoE).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { playMeow(0.9); } });
        findViewById(R.id.pianoF).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { playMeow(1.0); } });
        findViewById(R.id.pianoG).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { playMeow(1.1); } });
        findViewById(R.id.pianoA).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { playMeow(1.2); } });
        findViewById(R.id.pianoB).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { playMeow(1.3); } });
        findViewById(R.id.pianoC2).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { playMeow(1.5); } });

        // Soundboard Clicks
        findViewById(R.id.btnSndPurr).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { playPurr(); } });
        findViewById(R.id.btnSndSqueak).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { playSqueak(); } });
        findViewById(R.id.btnSndChirp).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { playChirp(); } });
        findViewById(R.id.btnSndAngry).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { playGrowl(); } });
        findViewById(R.id.btnSndKitten).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { playMeow(1.75); } });
        findViewById(R.id.btnSndWobble).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { playWobble(); } });

        switchTab("laser");
    }

    private void switchTab(String mode) {
        btnTabLaser.setTextColor(0x80FFFFFF);
        btnTabFish.setTextColor(0x80FFFFFF);
        btnTabPiano.setTextColor(0x80FFFFFF);
        btnTabSounds.setTextColor(0x80FFFFFF);

        gameContainer.setVisibility(View.GONE);
        layoutPiano.setVisibility(View.GONE);
        layoutSounds.setVisibility(View.GONE);

        if ("laser".equals(mode)) {
            btnTabLaser.setTextColor(0xFFFFFFFF);
            gameContainer.setVisibility(View.VISIBLE);
            toyView.setMode(CatToyView.MODE_LASER);
        } else if ("fish".equals(mode)) {
            btnTabFish.setTextColor(0xFFFFFFFF);
            gameContainer.setVisibility(View.VISIBLE);
            toyView.setMode(CatToyView.MODE_FISH);
        } else if ("piano".equals(mode)) {
            btnTabPiano.setTextColor(0xFFFFFFFF);
            layoutPiano.setVisibility(View.VISIBLE);
        } else if ("sounds".equals(mode)) {
            btnTabSounds.setTextColor(0xFFFFFFFF);
            layoutSounds.setVisibility(View.VISIBLE);
        }
    }

    private void incrementScore() {
        score++;
        if (score > highScore) {
            highScore = score;
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt("HighScore", highScore);
            editor.apply();
        }
        updateScoreDisplay();
    }

    private void updateScoreDisplay() {
        tvScore.setText("Cat Score: " + score + "  |  Best: " + highScore);
    }

    // --- SOUND ENGINE COMPACT NATIVE SYNTHESIZERS ---

    private void playSqueak() {
        final int sampleRate = 44100;
        final int duration = sampleRate / 4;
        final short[] buffer = new short[duration];
        for (int i = 0; i < duration; i++) {
            double t = (double) i / sampleRate;
            double freq = 3500.0 + (1000.0 * (double) i / duration);
            double envelope = 1.0 - (double) i / duration;
            buffer[i] = (short) (Math.sin(2.0 * Math.PI * freq * t) * envelope * 32767.0 * 0.85);
        }
        playSoundBuffer(buffer, sampleRate);
    }

    private void playPurr() {
        final int sampleRate = 44100;
        final int duration = sampleRate * 2;
        final short[] buffer = new short[duration];
        for (int i = 0; i < duration; i++) {
            double t = (double) i / sampleRate;
            double carrier = Math.sin(2.0 * Math.PI * 35.0 * t);
            double modulator = 0.6 + 0.4 * Math.sin(2.0 * Math.PI * 18.0 * t);
            double envelope = 1.0;
            if (i < 4410) {
                envelope = (double) i / 4410;
            } else if (i > duration - 4410) {
                envelope = (double) (duration - i) / 4410;
            }
            buffer[i] = (short) (carrier * modulator * envelope * 32767.0 * 0.9);
        }
        playSoundBuffer(buffer, sampleRate);
    }

    private void playMeow(final double pitchFactor) {
        final int sampleRate = 44100;
        final int duration = (int) (sampleRate * 0.65);
        final short[] buffer = new short[duration];
        for (int i = 0; i < duration; i++) {
            double t = (double) i / sampleRate;
            double progress = (double) i / duration;

            double baseFreq = 380.0 * pitchFactor;
            double freq;
            if (progress < 0.3) {
                freq = baseFreq + (450.0 * pitchFactor * (progress / 0.3));
            } else {
                freq = (baseFreq + 450.0 * pitchFactor) - (200.0 * pitchFactor * ((progress - 0.3) / 0.7));
            }

            double signal = Math.sin(2.0 * Math.PI * freq * t)
                    + 0.6 * Math.sin(2.0 * Math.PI * 2.0 * freq * t)
                    + 0.3 * Math.sin(2.0 * Math.PI * 3.0 * freq * t)
                    + 0.15 * Math.sin(2.0 * Math.PI * 4.0 * freq * t);

            signal = signal / 2.05;

            double envelope = 1.0;
            if (progress < 0.15) {
                envelope = progress / 0.15;
            } else if (progress > 0.7) {
                envelope = (1.0 - progress) / 0.3;
            }

            buffer[i] = (short) (signal * envelope * 32767.0 * 0.85);
        }
        playSoundBuffer(buffer, sampleRate);
    }

    private void playChirp() {
        final int sampleRate = 44100;
        final int burstLength = sampleRate / 8;
        final int duration = burstLength * 4 + sampleRate / 4;
        final short[] buffer = new short[duration];
        for (int chirpCount = 0; chirpCount < 4; chirpCount++) {
            int offset = chirpCount * (burstLength + sampleRate / 16);
            for (int i = 0; i < burstLength; i++) {
                double t = (double) i / sampleRate;
                double freq = 2000.0 + 1500.0 * ((double) i / burstLength);
                double envelope = Math.sin(Math.PI * (double) i / burstLength);
                buffer[offset + i] = (short) (Math.sin(2.0 * Math.PI * freq * t) * envelope * 32767.0 * 0.7);
            }
        }
        playSoundBuffer(buffer, sampleRate);
    }

    private void playGrowl() {
        final int sampleRate = 44100;
        final int duration = (int) (sampleRate * 1.2);
        final short[] buffer = new short[duration];
        for (int i = 0; i < duration; i++) {
            double t = (double) i / sampleRate;
            double noise = Math.random() * 2.0 - 1.0;
            double carrier = Math.sin(2.0 * Math.PI * 60.0 * t);
            double envelope = 1.0;
            if (i < 8820) {
                envelope = (double) i / 8820;
            } else if (i > duration - 8820) {
                envelope = (double) (duration - i) / 8820;
            }
            double signal = (carrier * 0.65) + (noise * 0.35);
            buffer[i] = (short) (signal * envelope * 32767.0 * 0.6);
        }
        playSoundBuffer(buffer, sampleRate);
    }

    private void playWobble() {
        final int sampleRate = 44100;
        final int duration = (int) (sampleRate * 0.75);
        final short[] buffer = new short[duration];
        for (int i = 0; i < duration; i++) {
            double t = (double) i / sampleRate;
            double freq = 500.0 + 350.0 * Math.sin(2.0 * Math.PI * 14.0 * t);
            double envelope = Math.sin(Math.PI * (double) i / duration);
            buffer[i] = (short) (Math.sin(2.0 * Math.PI * freq * t) * envelope * 32767.0 * 0.65);
        }
        playSoundBuffer(buffer, sampleRate);
    }

    private void playSoundBuffer(final short[] buffer, final int sampleRate) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                AudioTrack audioTrack = null;
                try {
                    audioTrack = new AudioTrack(
                            AudioManager.STREAM_MUSIC,
                            sampleRate,
                            AudioFormat.CHANNEL_OUT_MONO,
                            AudioFormat.ENCODING_PCM_16BIT,
                            buffer.length * 2,
                            AudioTrack.MODE_STATIC);
                    audioTrack.write(buffer, 0, buffer.length);
                    audioTrack.play();

                    long durationMs = (long) (((double) buffer.length / sampleRate) * 1000.0);
                    Thread.sleep(durationMs + 100);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (audioTrack != null) {
                        try {
                            audioTrack.stop();
                            audioTrack.release();
                        } catch (Exception e) {
                            // ignore double release/stop errors
                        }
                    }
                }
            }
        }).start();
    }
}