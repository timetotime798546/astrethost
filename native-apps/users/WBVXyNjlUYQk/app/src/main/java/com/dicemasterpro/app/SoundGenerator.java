package com.dicemasterpro.app;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

public class SoundGenerator {
    private static final int SAMPLE_RATE = 44100;
    private static boolean soundEnabled = true;

    public static void setSoundEnabled(boolean enabled) {
        soundEnabled = enabled;
    }

    public static boolean isSoundEnabled() {
        return soundEnabled;
    }

    public static void playRollSound() {
        if (!soundEnabled) return;

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int durationMs = 280;
                    int numSamples = (SAMPLE_RATE * durationMs) / 1000;
                    short[] sample = new short[numSamples];
                    java.util.Random random = new java.util.Random();

                    for (int i = 0; i < numSamples; i++) {
                        double t = (double) i / SAMPLE_RATE;
                        double envelope = Math.exp(-14.0 * t);
                        
                        // Noise factor simulating friction clatter
                        double noise = random.nextDouble() * 2.0 - 1.0;
                        
                        // Resonant strike component modeling plastic impact density
                        double frequency = 140.0 + 90.0 * Math.sin(2.0 * Math.PI * 6.0 * t);
                        double sine = Math.sin(2.0 * Math.PI * frequency * t);
                        
                        double val = (noise * 0.35 + sine * 0.65) * envelope;
                        
                        // Periodic collision bounces inside container
                        if (t > 0.07) {
                            val += (random.nextDouble() * 2.0 - 1.0) * 0.3 * Math.exp(-28.0 * (t - 0.07));
                        }
                        if (t > 0.15) {
                            val += (random.nextDouble() * 2.0 - 1.0) * 0.22 * Math.exp(-34.0 * (t - 0.15));
                        }
                        if (t > 0.22) {
                            val += (random.nextDouble() * 2.0 - 1.0) * 0.15 * Math.exp(-40.0 * (t - 0.22));
                        }
                        
                        val = Math.max(-1.0, Math.min(1.0, val));
                        sample[i] = (short) (val * 32767);
                    }
                    
                    AudioTrack audioTrack = new AudioTrack(
                        AudioManager.STREAM_MUSIC,
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        sample.length * 2,
                        AudioTrack.MODE_STATIC
                    );
                    audioTrack.write(sample, 0, sample.length);
                    audioTrack.play();
                    Thread.sleep(durationMs + 50);
                    audioTrack.release();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static void playWinSound() {
        if (!soundEnabled) return;

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int durationMs = 500;
                    int numSamples = (SAMPLE_RATE * durationMs) / 1000;
                    short[] sample = new short[numSamples];
                    
                    for (int i = 0; i < numSamples; i++) {
                        double t = (double) i / SAMPLE_RATE;
                        double envelope = Math.exp(-5.0 * t);
                        
                        // Arpeggio notes (C5, E5, G5)
                        double f1 = 523.25; 
                        double f2 = 659.25; 
                        double f3 = 783.99; 
                        
                        double val = 0.3 * Math.sin(2.0 * Math.PI * f1 * t);
                        if (t > 0.12) {
                            val += 0.3 * Math.sin(2.0 * Math.PI * f2 * (t - 0.12)) * Math.exp(-6.0 * (t - 0.12));
                        }
                        if (t > 0.24) {
                            val += 0.3 * Math.sin(2.0 * Math.PI * f3 * (t - 0.24)) * Math.exp(-7.0 * (t - 0.24));
                        }
                        
                        val = val * envelope;
                        val = Math.max(-1.0, Math.min(1.0, val));
                        sample[i] = (short) (val * 32767);
                    }
                    
                    AudioTrack audioTrack = new AudioTrack(
                        AudioManager.STREAM_MUSIC,
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        sample.length * 2,
                        AudioTrack.MODE_STATIC
                    );
                    audioTrack.write(sample, 0, sample.length);
                    audioTrack.play();
                    Thread.sleep(durationMs + 50);
                    audioTrack.release();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static void playPigSquealSound() {
        if (!soundEnabled) return;

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int durationMs = 300;
                    int numSamples = (SAMPLE_RATE * durationMs) / 1000;
                    short[] sample = new short[numSamples];
                    
                    for (int i = 0; i < numSamples; i++) {
                        double t = (double) i / SAMPLE_RATE;
                        double envelope = Math.exp(-6.0 * t);
                        
                        // High pitch sweep frequency simulating a fast buzz/squeal
                        double freq = 850.0 - 450.0 * t;
                        double val = 0.4 * Math.sin(2.0 * Math.PI * freq * t) * (1.0 + Math.sin(2.0 * Math.PI * 110.0 * t) * 0.3);
                        
                        val = val * envelope;
                        val = Math.max(-1.0, Math.min(1.0, val));
                        sample[i] = (short) (val * 32767);
                    }
                    
                    AudioTrack audioTrack = new AudioTrack(
                        AudioManager.STREAM_MUSIC,
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        sample.length * 2,
                        AudioTrack.MODE_STATIC
                    );
                    audioTrack.write(sample, 0, sample.length);
                    audioTrack.play();
                    Thread.sleep(durationMs + 50);
                    audioTrack.release();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}