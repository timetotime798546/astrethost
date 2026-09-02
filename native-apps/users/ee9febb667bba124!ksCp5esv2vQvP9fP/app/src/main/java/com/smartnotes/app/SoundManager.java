package com.smartnotes.app;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

public class SoundManager {

    public static void playClick() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int sampleRate = 44100;
                    int durationMs = 50;
                    int numSamples = (int) (sampleRate * (durationMs / 1000.0));
                    double[] sample = new double[numSamples];
                    byte[] generatedSnd = new byte[2 * numSamples];

                    for (int i = 0; i < numSamples; ++i) {
                        double t = (double) i / sampleRate;
                        double decay = Math.exp(-t * 100.0);
                        sample[i] = Math.sin(2 * Math.PI * 1000 * t) * decay;
                    }

                    int idx = 0;
                    for (double dVal : sample) {
                        short val = (short) (dVal * 32767);
                        generatedSnd[idx++] = (byte) (val & 0x00ff);
                        generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
                    }

                    AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                            sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                            AudioFormat.ENCODING_PCM_16BIT, generatedSnd.length,
                            AudioTrack.MODE_STATIC);
                    audioTrack.write(generatedSnd, 0, generatedSnd.length);
                    audioTrack.play();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } 
        }).start();
    }

    public static void playSuccess() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int sampleRate = 44100;
                    int durationMs = 300;
                    int numSamples = (int) (sampleRate * (durationMs / 1000.0));
                    double[] sample = new double[numSamples];
                    byte[] generatedSnd = new byte[2 * numSamples];

                    for (int i = 0; i < numSamples; ++i) {
                        double t = (double) i / sampleRate;
                        double decay = Math.exp(-t * 8.0);
                        double freq = 523.25; 
                        if (t > 0.08) freq = 659.25; 
                        if (t > 0.16) freq = 783.99; 
                        sample[i] = Math.sin(2 * Math.PI * freq * t) * decay;
                    }

                    int idx = 0;
                    for (double dVal : sample) {
                        short val = (short) (dVal * 32767);
                        generatedSnd[idx++] = (byte) (val & 0x00ff);
                        generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
                    }

                    AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                            sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                            AudioFormat.ENCODING_PCM_16BIT, generatedSnd.length,
                            AudioTrack.MODE_STATIC);
                    audioTrack.write(generatedSnd, 0, generatedSnd.length);
                    audioTrack.play();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static void playDelete() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int sampleRate = 44100;
                    int durationMs = 350;
                    int numSamples = (int) (sampleRate * (durationMs / 1000.0));
                    double[] sample = new double[numSamples];
                    byte[] generatedSnd = new byte[2 * numSamples];

                    for (int i = 0; i < numSamples; ++i) {
                        double t = (double) i / sampleRate;
                        double decay = Math.exp(-t * 6.0);
                        double freq = 500.0 - (t * 1100.0);
                        if (freq < 80.0) freq = 80.0;
                        sample[i] = Math.sin(2 * Math.PI * freq * t) * decay;
                    }

                    int idx = 0;
                    for (double dVal : sample) {
                        short val = (short) (dVal * 32767);
                        generatedSnd[idx++] = (byte) (val & 0x00ff);
                        generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
                    }

                    AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                            sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                            AudioFormat.ENCODING_PCM_16BIT, generatedSnd.length,
                            AudioTrack.MODE_STATIC);
                    audioTrack.write(generatedSnd, 0, generatedSnd.length);
                    audioTrack.play();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static void playOpen() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int sampleRate = 44100;
                    int durationMs = 150;
                    int numSamples = (int) (sampleRate * (durationMs / 1000.0));
                    double[] sample = new double[numSamples];
                    byte[] generatedSnd = new byte[2 * numSamples];

                    for (int i = 0; i < numSamples; ++i) {
                        double t = (double) i / sampleRate;
                        double decay = Math.exp(-t * 12.0);
                        double freq = 440.0 + (t * 700.0);
                        sample[i] = Math.sin(2 * Math.PI * freq * t) * decay;
                    }

                    int idx = 0;
                    for (double dVal : sample) {
                        short val = (short) (dVal * 32767);
                        generatedSnd[idx++] = (byte) (val & 0x00ff);
                        generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
                    }

                    AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                            sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                            AudioFormat.ENCODING_PCM_16BIT, generatedSnd.length,
                            AudioTrack.MODE_STATIC);
                    audioTrack.write(generatedSnd, 0, generatedSnd.length);
                    audioTrack.play();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } 
        }).start();
    }
}