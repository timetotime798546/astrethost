package com.turbo3dracer.app;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

public class SoundEffects {
    private static SoundEffects instance;
    private boolean soundEnabled = true;

    private SoundEffects() {}

    public static synchronized SoundEffects getInstance() {
        if (instance == null) {
            instance = new SoundEffects();
        }
        return instance;
    }

    public void setSoundEnabled(boolean enabled) {
        this.soundEnabled = enabled;
    }

    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    public void playTone(final int freqHz, final int durationMs, final float volume) {
        if (!soundEnabled) return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int sampleRate = 22050;
                    int numSamples = (durationMs * sampleRate) / 1000;
                    if (numSamples <= 0) return;
                    double[] sample = new double[numSamples];
                    byte[] generatedSnd = new byte[2 * numSamples];

                    for (int i = 0; i < numSamples; ++i) {
                        sample[i] = Math.sin(2 * Math.PI * i / (sampleRate / (double) freqHz));
                    }

                    int idx = 0;
                    for (final double dVal : sample) {
                        final short val = (short) ((dVal * 32767) * volume);
                        generatedSnd[idx++] = (byte) (val & 0x00ff);
                        generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
                    }

                    int bufferSize = AudioTrack.getMinBufferSize(sampleRate,
                            AudioFormat.CHANNEL_OUT_MONO,
                            AudioFormat.ENCODING_PCM_16BIT);
                    if (bufferSize < generatedSnd.length) {
                        bufferSize = generatedSnd.length;
                    }

                    AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                            sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                            AudioFormat.ENCODING_PCM_16BIT, bufferSize,
                            AudioTrack.MODE_STATIC);
                    audioTrack.write(generatedSnd, 0, generatedSnd.length);
                    audioTrack.play();
                    Thread.sleep(durationMs + 20);
                    audioTrack.release();
                } catch (Exception ignored) {
                }
            }
        }).start();
    }

    public void playEngineRev(int speedRatio) {
        int baseFreq = 80 + (speedRatio * 3);
        playTone(baseFreq, 60, 0.25f);
    }

    public void playNitroBoost() {
        playTone(650, 160, 0.4f);
    }

    public void playCoinPickup() {
        playTone(980, 80, 0.35f);
    }

    public void playCrash() {
        playTone(90, 220, 0.6f);
    }
}