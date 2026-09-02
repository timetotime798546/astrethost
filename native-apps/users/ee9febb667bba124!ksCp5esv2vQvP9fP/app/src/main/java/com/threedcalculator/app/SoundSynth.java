package com.threedcalculator.app;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

public class SoundSynth {

    public static void playTone(final double frequency, final int durationMs, final float volume) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int sampleRate = 11025;
                    int numSamples = durationMs * sampleRate / 1000;
                    short[] buffer = new short[numSamples];
                    
                    for (int i = 0; i < numSamples; ++i) {
                        double t = (double) i / sampleRate;
                        double angle = 2.0 * Math.PI * frequency * t;
                        double sample = Math.sin(angle);
                        
                        // Add slight retro buzz by layering square harmonic
                        sample += 0.22 * Math.signum(Math.sin(2.0 * angle));
                        
                        // Safe linear volume decay envelope (Attack-Decay)
                        double envelope = 1.0;
                        if (i < numSamples * 0.1) {
                            envelope = i / (numSamples * 0.1);
                        } else if (i > numSamples * 0.6) {
                            envelope = (numSamples - i) / (double) (numSamples * 0.4);
                        }
                        
                        sample *= envelope * volume;
                        buffer[i] = (short) (sample * 32767);
                    }
                    
                    AudioTrack audioTrack = new AudioTrack(
                        AudioManager.STREAM_MUSIC,
                        sampleRate,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        buffer.length * 2,
                        AudioTrack.MODE_STATIC
                    );
                    
                    audioTrack.write(buffer, 0, buffer.length);
                    audioTrack.play();
                    
                    Thread.sleep(durationMs + 30);
                    audioTrack.release();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } 
        }).start();
    }

    public static void playClick() {
        playTone(920.0, 35, 0.35f);
    }

    public static void playOperatorClick() {
        playTone(620.0, 50, 0.45f);
    }

    public static void playEqualClick() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                playTone(840.0, 70, 0.45f);
                try { Thread.sleep(90); } catch (InterruptedException e) {}
                playTone(1260.0, 110, 0.45f);
            } 
        }).start();
    }

    public static void playError() {
        playTone(190.0, 260, 0.75f);
    }

    public static void playLoaderSound(final int step) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                double freq = 261.63;
                switch(step) {
                    case 1: freq = 261.63; break;
                    case 2: freq = 329.63; break;
                    case 3: freq = 392.00; break;
                    case 4: freq = 523.25; break;
                }
                playTone(freq, 130, 0.45f);
            } 
        }).start();
    }

    public static void playLoaderRiser() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int sampleRate = 11025;
                    int durationMs = 1200;
                    int numSamples = durationMs * sampleRate / 1000;
                    short[] buffer = new short[numSamples];
                    
                    for (int i = 0; i < numSamples; ++i) {
                        double t = (double) i / sampleRate;
                        // Frequency sweeps from 150Hz to 600Hz
                        double freq = 150.0 + (450.0 * (t / (durationMs / 1000.0)));
                        double angle = 2.0 * Math.PI * freq * t;
                        double sample = Math.sin(angle);
                        
                        // Ring modulation for complex industrial retro vibe
                        sample *= Math.sin(2.0 * Math.PI * 15.0 * t);
                        
                        // Smooth envelopes
                        double envelope = 1.0;
                        if (i < numSamples * 0.15) {
                            envelope = i / (numSamples * 0.15);
                        } else if (i > numSamples * 0.8) {
                            envelope = (numSamples - i) / (double) (numSamples * 0.2);
                        } 
                        
                        sample *= envelope * 0.45;
                        buffer[i] = (short) (sample * 32767);
                    }
                    
                    AudioTrack audioTrack = new AudioTrack(
                        AudioManager.STREAM_MUSIC,
                        sampleRate,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        buffer.length * 2,
                        AudioTrack.MODE_STATIC
                    );
                    audioTrack.write(buffer, 0, buffer.length);
                    audioTrack.play();
                    Thread.sleep(durationMs + 30);
                    audioTrack.release();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}