package com.arrowflowcrashcourse.app;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

public class SoundManager {
    private static final int SAMPLE_RATE = 22050;

    public static void playLaunchSound() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int durationMs = 120;
                    int numSamples = (int) (SAMPLE_RATE * (durationMs / 1000.0));
                    short[] sample = new short[numSamples];
                    
                    for (int i = 0; i < numSamples; i++) {
                        double t = (double) i / SAMPLE_RATE;
                        double freq = 440.0 + (500.0 * (t / (durationMs / 1000.0)));
                        sample[i] = (short) (Math.sin(2 * Math.PI * freq * t) * 16384.0);
                    }

                    AudioTrack audioTrack = new AudioTrack(
                            AudioManager.STREAM_MUSIC,
                            SAMPLE_RATE,
                            AudioFormat.CHANNEL_OUT_MONO,
                            AudioFormat.ENCODING_PCM_16BIT,
                            numSamples * 2,
                            AudioTrack.MODE_STATIC
                    );
                    audioTrack.write(sample, 0, numSamples);
                    audioTrack.play();
                    
                    Thread.sleep(durationMs + 30);
                    audioTrack.release();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static void playCrashSound() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int durationMs = 350;
                    int numSamples = (int) (SAMPLE_RATE * (durationMs / 1000.0));
                    short[] sample = new short[numSamples];
                    java.util.Random random = new java.util.Random();
                    
                    for (int i = 0; i < numSamples; i++) {
                        double t = (double) i / SAMPLE_RATE;
                        double progress = t / (durationMs / 1000.0);
                        double noise = random.nextDouble() * 2.0 - 1.0;
                        double lowFreq = Math.sin(2 * Math.PI * 55.0 * t);
                        double signal = (noise * 0.45 + lowFreq * 0.55) * (1.0 - progress);
                        sample[i] = (short) (signal * 16384.0);
                    }

                    AudioTrack audioTrack = new AudioTrack(
                            AudioManager.STREAM_MUSIC,
                            SAMPLE_RATE,
                            AudioFormat.CHANNEL_OUT_MONO,
                            AudioFormat.ENCODING_PCM_16BIT,
                            numSamples * 2,
                            AudioTrack.MODE_STATIC
                    );
                    audioTrack.write(sample, 0, numSamples);
                    audioTrack.play();
                    
                    Thread.sleep(durationMs + 30);
                    audioTrack.release();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static void playWinSound() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int noteDuration = 110;
                    int sampleLength = (int) (SAMPLE_RATE * (noteDuration / 1000.0));
                    double[] freqs = { 523.25, 659.25, 783.99, 1046.50 };
                    
                    int totalSamples = sampleLength * 4;
                    short[] sample = new short[totalSamples];
                    
                    for (int note = 0; note < 4; note++) {
                        double freq = freqs[note];
                        for (int i = 0; i < sampleLength; i++) {
                            double t = (double) i / SAMPLE_RATE;
                            double envelope = 1.0 - ((double) i / sampleLength);
                            sample[note * sampleLength + i] = (short) (Math.sin(2 * Math.PI * freq * t) * envelope * 16384.0);
                        }
                    }

                    AudioTrack audioTrack = new AudioTrack(
                            AudioManager.STREAM_MUSIC,
                            SAMPLE_RATE,
                            AudioFormat.CHANNEL_OUT_MONO,
                            AudioFormat.ENCODING_PCM_16BIT,
                            totalSamples * 2,
                            AudioTrack.MODE_STATIC
                    );
                    audioTrack.write(sample, 0, totalSamples);
                    audioTrack.play();
                    
                    Thread.sleep(noteDuration * 4 + 30);
                    audioTrack.release();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}