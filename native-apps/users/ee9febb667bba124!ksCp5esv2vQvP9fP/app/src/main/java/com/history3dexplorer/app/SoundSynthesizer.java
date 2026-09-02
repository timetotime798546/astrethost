package com.history3dexplorer.app;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

public class SoundSynthesizer {
    public static boolean isSoundEnabled = true;

    public static void playSound(final int type) {
        if (!isSoundEnabled) return;
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int sampleRate = 44100;
                    double duration = 0.0;
                    if (type == 1) duration = 0.5;      // Start Up
                    else if (type == 2) duration = 0.07;     // Slide sweep
                    else if (type == 3) duration = 0.18;     // Menu tap
                    else if (type == 4) duration = 0.45;     // Success sound
                    else if (type == 5) duration = 0.35;     // Wrong answer buzz

                    int numSamples = (int) (duration * sampleRate);
                    double[] sample = new double[numSamples];
                    byte[] generatedSnd = new byte[2 * numSamples];

                    if (type == 1) {
                        // Majestic synthesizer start up chime (G chord sliding to C)
                        for (int i = 0; i < numSamples; ++i) {
                            double t = (double) i / sampleRate;
                            double progress = (double) i / numSamples;
                            double freq1 = 392.0 + (130.8 * progress); // G4 to C5
                            double freq2 = 523.25; // Constant C5 anchor
                            double signal = Math.sin(2 * Math.PI * freq1 * t) + 0.5 * Math.sin(2 * Math.PI * freq2 * t);
                            signal = signal / 1.5;
                            double fade = 1.0 - progress;
                            sample[i] = signal * fade;
                        }
                    } else if (type == 2) {
                        // Elegant friction tick for spinning action
                        for (int i = 0; i < numSamples; ++i) {
                            double t = (double) i / sampleRate;
                            double progress = (double) i / numSamples;
                            double freq = 450.0 + (180.0 * progress);
                            double signal = Math.sin(2 * Math.PI * freq * t);
                            double fade = 1.0 - progress;
                            sample[i] = signal * fade * 0.3;
                        }
                    } else if (type == 3) {
                        // Crisp double beep on confirmation
                        for (int i = 0; i < numSamples; ++i) {
                            double t = (double) i / sampleRate;
                            double progress = (double) i / numSamples;
                            double freq = (progress < 0.5) ? 880.0 : 1320.0;
                            double signal = Math.sin(2 * Math.PI * freq * t);
                            double fade = 1.0 - progress;
                            sample[i] = signal * fade * 0.45;
                        }
                    } else if (type == 4) {
                        // Bright victory arpeggio scaling upwards
                        for (int i = 0; i < numSamples; ++i) {
                            double t = (double) i / sampleRate;
                            double progress = (double) i / numSamples;
                            double freq = 523.25; // C5
                            if (progress > 0.75) freq = 1046.50; // C6
                            else if (progress > 0.5) freq = 783.99;  // G5
                            else if (progress > 0.25) freq = 659.25; // E5

                            double signal = Math.sin(2 * Math.PI * freq * t) + 0.3 * Math.sin(2 * Math.PI * (freq * 2) * t);
                            signal /= 1.3;
                            double fade = 1.0 - progress;
                            sample[i] = signal * fade * 0.5;
                        }
                    } else if (type == 5) {
                        // Classic error buzz sound sliding downward
                        for (int i = 0; i < numSamples; ++i) {
                            double t = (double) i / sampleRate;
                            double progress = (double) i / numSamples;
                            double freq = 196.0 - (98.0 * progress); // G3 descending
                            // Multi-wave square approximation for retro buzzing timbre
                            double signal = Math.sin(2 * Math.PI * freq * t) 
                                            + 0.5 * Math.sin(2 * Math.PI * 3 * freq * t)
                                            + 0.25 * Math.sin(2 * Math.PI * 5 * freq * t);
                            signal /= 1.75;
                            double fade = 1.0 - progress;
                            sample[i] = signal * fade * 0.5;
                        }
                    }

                    int idx = 0;
                    for (double dVal : sample) {
                        short val = (short) (dVal * 32767);
                        generatedSnd[idx++] = (byte) (val & 0x00ff);
                        generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
                    }

                    AudioTrack audioTrack = new AudioTrack(
                            AudioManager.STREAM_MUSIC,
                            sampleRate,
                            AudioFormat.CHANNEL_OUT_MONO,
                            AudioFormat.ENCODING_PCM_16BIT,
                            generatedSnd.length,
                            AudioTrack.MODE_STATIC
                    );
                    audioTrack.write(generatedSnd, 0, generatedSnd.length);
                    audioTrack.play();
                    Thread.sleep((long) (duration * 1000) + 120);
                    audioTrack.stop();
                    audioTrack.release();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}