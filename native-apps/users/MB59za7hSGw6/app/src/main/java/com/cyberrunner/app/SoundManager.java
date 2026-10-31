package com.cyberrunner.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

public class SoundManager {
    private static final String PREFS_NAME = "cyber_runner_audio";
    private static final String KEY_SOUND_ENABLED = "sound_enabled";

    private final Context context;
    private boolean soundEnabled;

    public SoundManager(Context context) {
        this.context = context.getApplicationContext();
        SharedPreferences prefs = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.soundEnabled = prefs.getBoolean(KEY_SOUND_ENABLED, true);
    }

    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    public void setSoundEnabled(boolean enabled) {
        this.soundEnabled = enabled;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply();
    }

    public void toggleSound() {
        setSoundEnabled(!soundEnabled);
    }

    public void playJumpSound() {
        if (!soundEnabled) return;
        playSynthesizedTone(360, 920, 0.12f);
    }

    public void playCoinSound() {
        if (!soundEnabled) return;
        playSynthesizedTone(900, 1600, 0.09f);
    }

    public void playGameOverSound() {
        if (!soundEnabled) return;
        playSynthesizedTone(480, 85, 0.38f);
    }

    private void playSynthesizedTone(final float startFreq, final float endFreq, final float durationSec) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int sampleRate = 22050;
                    int numSamples = (int) (sampleRate * durationSec);
                    byte[] generatedSnd = new byte[2 * numSamples];

                    double currentFreq = startFreq;
                    double freqStep = (endFreq - startFreq) / numSamples;
                    double phase = 0;

                    for (int i = 0; i < numSamples; ++i) {
                        currentFreq += freqStep;
                        phase += 2.0 * Math.PI * currentFreq / sampleRate;
                        if (phase > 2.0 * Math.PI) {
                            phase -= 2.0 * Math.PI;
                        }
                        // Apply decay envelope
                        float envelope = 1.0f - ((float) i / numSamples);
                        short sampleVal = (short) (Math.sin(phase) * 32767 * envelope * 0.7f);

                        generatedSnd[2 * i] = (byte) (sampleVal & 0x00ff);
                        generatedSnd[2 * i + 1] = (byte) ((sampleVal & 0xff00) >>> 8);
                    }

                    int bufferSize = AudioTrack.getMinBufferSize(
                            sampleRate,
                            AudioFormat.CHANNEL_OUT_MONO,
                            AudioFormat.ENCODING_PCM_16BIT
                    );

                    int actualBufferSize = Math.max(bufferSize, generatedSnd.length);

                    AudioTrack audioTrack = new AudioTrack(
                            AudioManager.STREAM_MUSIC,
                            sampleRate,
                            AudioFormat.CHANNEL_OUT_MONO,
                            AudioFormat.ENCODING_PCM_16BIT,
                            actualBufferSize,
                            AudioTrack.MODE_STATIC
                    );

                    audioTrack.write(generatedSnd, 0, generatedSnd.length);
                    audioTrack.play();

                    Thread.sleep((long) (durationSec * 1000) + 40);
                    audioTrack.stop();
                    audioTrack.release();
                } catch (Exception ignored) {
                }
            }
        }).start();
    }
}