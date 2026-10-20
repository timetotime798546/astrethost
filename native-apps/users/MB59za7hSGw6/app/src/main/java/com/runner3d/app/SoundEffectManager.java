package com.runner3d.app;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

public class SoundEffectManager {

    private static final int SAMPLE_RATE = 22050;

    public void playTone(final double startFreq, final double endFreq, final int durationMs) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int numSamples = (SAMPLE_RATE * durationMs) / 1000;
                    short[] buffer = new short[numSamples];

                    double currentFreq = startFreq;
                    double freqStep = (endFreq - startFreq) / (double) numSamples;
                    double phase = 0;

                    for (int i = 0; i < numSamples; i++) {
                        currentFreq += freqStep;
                        phase += 2.0 * Math.PI * currentFreq / SAMPLE_RATE;
                        double envelope = 1.0 - ((double) i / (double) numSamples); // smooth decay
                        short sample = (short) (Math.sin(phase) * 16384.0 * envelope);
                        buffer[i] = sample;
                    }

                    int minBufferSize = AudioTrack.getMinBufferSize(
                            SAMPLE_RATE,
                            AudioFormat.CHANNEL_OUT_MONO,
                            AudioFormat.ENCODING_PCM_16BIT
                    );

                    int bufferSize = Math.max(numSamples * 2, minBufferSize);

                    AudioTrack audioTrack = new AudioTrack(
                            AudioManager.STREAM_MUSIC,
                            SAMPLE_RATE,
                            AudioFormat.CHANNEL_OUT_MONO,
                            AudioFormat.ENCODING_PCM_16BIT,
                            bufferSize,
                            AudioTrack.MODE_STATIC
                    );

                    audioTrack.write(buffer, 0, numSamples);
                    audioTrack.play();

                    Thread.sleep(durationMs + 30);
                    audioTrack.stop();
                    audioTrack.release();
                } catch (Exception ignored) {
                }
            }
        }).start();
    }

    public void playJump() {
        playTone(320.0, 780.0, 110);
    }

    public void playSlide() {
        playTone(550.0, 220.0, 130);
    }

    public void playCoin() {
        playTone(880.0, 1760.0, 90);
    }

    public void playCrash() {
        playTone(280.0, 60.0, 240);
    }

    public void playSwitchLane() {
        playTone(440.0, 520.0, 60);
    }
}