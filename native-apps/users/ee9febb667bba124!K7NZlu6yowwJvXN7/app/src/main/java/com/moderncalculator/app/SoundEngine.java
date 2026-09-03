package com.moderncalculator.app;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

public class SoundEngine {
    private static final int SAMPLE_RATE = 22050;

    /**
     * Synthesizes a beautiful short click sound using a pure sine-wave sweep.
     */
    public static void playClick() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int durationMs = 45;
                    int numSamples = (int) (SAMPLE_RATE * (durationMs / 1000.0));
                    double[] sample = new double[numSamples];
                    byte[] generatedSnd = new byte[2 * numSamples];

                    for (int i = 0; i < numSamples; ++i) {
                        double t = (double) i / SAMPLE_RATE;
                        // Fast descend frequency sweep for a clean tactile sound
                        double freq = 950.0 - (400.0 * ((double) i / numSamples));
                        sample[i] = Math.sin(2 * Math.PI * freq * t);
                    }

                    int idx = 0;
                    for (final double dVal : sample) {
                        // Apply rapid envelope decay to avoid audio cracking
                        double fade = 1.0;
                        if (idx > (numSamples * 2 * 0.6)) {
                            fade = 1.0 - ((double) (idx - (numSamples * 2 * 0.6)) / (numSamples * 2 * 0.4));
                        }
                        if (fade < 0) fade = 0;
                        final short val = (short) ((dVal * 24000) * fade);
                        generatedSnd[idx++] = (byte) (val & 0x00ff);
                        generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
                    }

                    AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                            SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
                            AudioFormat.ENCODING_PCM_16BIT, generatedSnd.length,
                            AudioTrack.MODE_STATIC);
                    audioTrack.write(generatedSnd, 0, generatedSnd.length);
                    audioTrack.play();
                } catch (Exception ignored) {}
            }
        }).start();
    }

    /**
     * Plays a high-fidelity retro synthesized arpeggio startup chord sequence.
     */
    public static void playStartup() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int durationMs = 1300;
                    int numSamples = (int) (SAMPLE_RATE * (durationMs / 1000.0));
                    double[] sample = new double[numSamples];
                    byte[] generatedSnd = new byte[2 * numSamples];

                    for (int i = 0; i < numSamples; ++i) {
                        double t = (double) i / SAMPLE_RATE;
                        double progress = (double) i / numSamples;
                        
                        // Sweeping arpeggio notes (Major 7th chord feel)
                        double freq;
                        if (progress < 0.20) {
                            freq = 261.63; // C4
                        } else if (progress < 0.40) {
                            freq = 329.63; // E4
                        } else if (progress < 0.60) {
                            freq = 392.00; // G4
                        } else if (progress < 0.80) {
                            freq = 493.88; // B4
                        } else {
                            freq = 523.25; // C5 High Chord Ring
                        }

                        // Layer with simple octave harmonic
                        double val = 0.6 * Math.sin(2 * Math.PI * freq * t) 
                                   + 0.3 * Math.sin(2 * Math.PI * (freq * 1.5) * t);
                        sample[i] = val;
                    }

                    int idx = 0;
                    for (final double dVal : sample) {
                        double fade = 1.0;
                        // Smooth envelope decay on closing segment
                        if (idx > (numSamples * 2 * 0.75)) {
                            fade = 1.0 - ((double) (idx - (numSamples * 2 * 0.75)) / (numSamples * 2 * 0.25));
                        }
                        if (fade < 0) fade = 0;
                        final short val = (short) ((dVal * 20000) * fade);
                        generatedSnd[idx++] = (byte) (val & 0x00ff);
                        generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
                    }

                    AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                            SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
                            AudioFormat.ENCODING_PCM_16BIT, generatedSnd.length,
                            AudioTrack.MODE_STATIC);
                    audioTrack.write(generatedSnd, 0, generatedSnd.length);
                    audioTrack.play();
                } catch (Exception ignored) {}
            }
        }).start();
    }
}