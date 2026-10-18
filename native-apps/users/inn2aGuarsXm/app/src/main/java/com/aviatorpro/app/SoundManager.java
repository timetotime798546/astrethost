package com.aviatorpro.app;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SoundManager {

    private static SoundManager instance;
    private final ExecutorService soundExecutor;
    private boolean soundEnabled = true;

    private SoundManager(Context context) {
        soundExecutor = Executors.newSingleThreadExecutor();
    }

    public static synchronized SoundManager getInstance(Context context) {
        if (instance == null) {
            instance = new SoundManager(context.getApplicationContext());
        }
        return instance;
    }

    public void setSoundEnabled(boolean enabled) {
        this.soundEnabled = enabled;
    }

    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    // Short crisp click for buttons & preset options
    public void playClick() {
        if (!soundEnabled) return;
        soundExecutor.execute(new Runnable() {
            @Override
            public void run() {
                playTone(880, 45, 0.5f);
            }
        });
    }

    // Step change sound for plus / minus bet adjusters
    public void playStep() {
        if (!soundEnabled) return;
        soundExecutor.execute(new Runnable() {
            @Override
            public void run() {
                playTone(1100, 35, 0.45f);
            }
        });
    }

    // Bet placed confirmation sound (two-tone rising confirmation)
    public void playBetPlaced() {
        if (!soundEnabled) return;
        soundExecutor.execute(new Runnable() {
            @Override
            public void run() {
                playTone(520, 60, 0.6f);
                try { Thread.sleep(20); } catch (Exception ignored) {}
                playTone(780, 80, 0.7f);
            }
        });
    }

    // Ascending flight tick while aeroplane climbs
    public void playFlightTick(final double multiplier) {
        if (!soundEnabled) return;
        soundExecutor.execute(new Runnable() {
            @Override
            public void run() {
                // Pitch rises smoothly with the current multiplier
                int freq = (int) Math.min(2200, 400 + (multiplier * 180));
                playTone(freq, 30, 0.25f);
            }
        });
    }

    // Big Cashout / Win celebration fanfare (ascending chords C5 - E5 - G5 - C6)
    public void playCashoutWin() {
        if (!soundEnabled) return;
        soundExecutor.execute(new Runnable() {
            @Override
            public void run() {
                int[] notes = {523, 659, 784, 1046};
                for (int i = 0; i < notes.length; i++) {
                    playTone(notes[i], 80, 0.75f);
                    try { Thread.sleep(25); } catch (Exception ignored) {}
                }
            }
        });
    }

    // Flew Away / Crash sound (descending buzzer)
    public void playCrash() {
        if (!soundEnabled) return;
        soundExecutor.execute(new Runnable() {
            @Override
            public void run() {
                playPitchSweep(420, 110, 260, 0.8f);
            }
        });
    }

    // Deposit success & credit sound (bright coin chime)
    public void playDepositSuccess() {
        if (!soundEnabled) return;
        soundExecutor.execute(new Runnable() {
            @Override
            public void run() {
                playTone(1318, 90, 0.7f);
                try { Thread.sleep(20); } catch (Exception ignored) {}
                playTone(1760, 140, 0.85f);
            }
        });
    }

    // Withdrawal submission notification sound
    public void playWithdrawSuccess() {
        if (!soundEnabled) return;
        soundExecutor.execute(new Runnable() {
            @Override
            public void run() {
                playTone(740, 70, 0.6f);
                try { Thread.sleep(20); } catch (Exception ignored) {}
                playTone(980, 100, 0.7f);
            }
        });
    }

    // Authentication / Registration Success Sound
    public void playAuthSuccess() {
        if (!soundEnabled) return;
        soundExecutor.execute(new Runnable() {
            @Override
            public void run() {
                playTone(660, 70, 0.65f);
                try { Thread.sleep(20); } catch (Exception ignored) {}
                playTone(880, 110, 0.75f);
            }
        });
    }

    // Error or alert buzz
    public void playError() {
        if (!soundEnabled) return;
        soundExecutor.execute(new Runnable() {
            @Override
            public void run() {
                playTone(220, 90, 0.7f);
                try { Thread.sleep(30); } catch (Exception ignored) {}
                playTone(180, 120, 0.75f);
            }
        });
    }

    // Pure audio tone synthesizer using AudioTrack
    private void playTone(int frequencyHz, int durationMs, float volume) {
        try {
            int sampleRate = 22050;
            int numSamples = (int) ((sampleRate * durationMs) / 1000.0);
            short[] buffer = new short[numSamples];

            double angleStep = 2.0 * Math.PI * frequencyHz / sampleRate;
            double angle = 0.0;

            for (int i = 0; i < numSamples; i++) {
                // Envelope decay to avoid clicking artifacts
                float envelope = 1.0f - ((float) i / numSamples);
                short val = (short) (Math.sin(angle) * Short.MAX_VALUE * volume * envelope);
                buffer[i] = val;
                angle += angleStep;
            }

            AudioTrack audioTrack = createAudioTrack(sampleRate, buffer.length);
            audioTrack.write(buffer, 0, buffer.length);
            audioTrack.play();
            Thread.sleep(durationMs + 20);
            audioTrack.stop();
            audioTrack.release();
        } catch (Exception ignored) {}
    }

    // Descending or ascending frequency sweep
    private void playPitchSweep(int startFreq, int endFreq, int durationMs, float volume) {
        try {
            int sampleRate = 22050;
            int numSamples = (int) ((sampleRate * durationMs) / 1000.0);
            short[] buffer = new short[numSamples];

            double phase = 0.0;
            for (int i = 0; i < numSamples; i++) {
                float frac = (float) i / numSamples;
                double currentFreq = startFreq + (endFreq - startFreq) * frac;
                double step = 2.0 * Math.PI * currentFreq / sampleRate;
                phase += step;

                float envelope = 1.0f - (frac * 0.7f);
                short val = (short) (Math.sin(phase) * Short.MAX_VALUE * volume * envelope);
                buffer[i] = val;
            }

            AudioTrack audioTrack = createAudioTrack(sampleRate, buffer.length);
            audioTrack.write(buffer, 0, buffer.length);
            audioTrack.play();
            Thread.sleep(durationMs + 20);
            audioTrack.stop();
            audioTrack.release();
        } catch (Exception ignored) {}
    }

    private AudioTrack createAudioTrack(int sampleRate, int bufferSamples) {
        int bufferBytes = bufferSamples * 2;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AudioAttributes attributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            AudioFormat format = new AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build();

            return new AudioTrack(
                    attributes,
                    format,
                    bufferBytes,
                    AudioTrack.MODE_STATIC,
                    AudioManager.AUDIO_SESSION_ID_GENERATE
            );
        } else {
            return new AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferBytes,
                    AudioTrack.MODE_STATIC
            );
        }
    }
}