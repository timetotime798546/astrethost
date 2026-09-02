package com.smartnotes.app;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

public class SoundManager {

    public static void play3DLaunchSound() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int sampleRate = 44100;
                    double durationSec = 2.4;
                    int numSamples = (int) (sampleRate * durationSec);
                    // Stereo dynamic stream: 2 channels (Left, Right) and 16-bit encoding (2 bytes per sample)
                    byte[] generatedSnd = new byte[4 * numSamples];

                    for (int i = 0; i < numSamples; ++i) {
                        double t = (double) i / sampleRate;
                        
                        // Spatial panning calculation from Left to Right and back dynamically
                        double panAngle = t * Math.PI * 1.5;
                        double leftGain = Math.cos(panAngle) * 0.5 + 0.5;
                        double rightGain = Math.sin(panAngle) * 0.5 + 0.5;

                        // Sci-fi low hum frequency vibrato
                        double baseFreq = 180.0 + (30.0 * Math.sin(2 * Math.PI * 4.0 * t));
                        double mainWave = Math.sin(2 * Math.PI * baseFreq * t);

                        // High pitch spatial cosmic orbit ping
                        double ringFreq = 440.0 + (t * 220.0);
                        double ringWave = Math.sin(2 * Math.PI * ringFreq * t) * Math.exp(-t * 2.5);

                        double combinedSample = (mainWave * 0.6 + ringWave * 0.4) * Math.exp(-t * 0.4);

                        // Left panned signal conversion
                        short valL = (short) (combinedSample * leftGain * 32767);
                        // Right panned signal conversion
                        short valR = (short) (combinedSample * rightGain * 32767);

                        int idx = i * 4;
                        // Stereo frame placement
                        generatedSnd[idx] = (byte) (valL & 0x00ff);
                        generatedSnd[idx + 1] = (byte) ((valL & 0xff00) >>> 8);
                        generatedSnd[idx + 2] = (byte) (valR & 0x00ff);
                        generatedSnd[idx + 3] = (byte) ((valR & 0xff00) >>> 8);
                    }

                    AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                            sampleRate, AudioFormat.CHANNEL_OUT_STEREO,
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

    public static void playPullTick() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int sampleRate = 44100;
                    int durationMs = 15;
                    int numSamples = (int) (sampleRate * (durationMs / 1000.0));
                    double[] sample = new double[numSamples];
                    byte[] generatedSnd = new byte[2 * numSamples];

                    for (int i = 0; i < numSamples; ++i) {
                        double t = (double) i / sampleRate;
                        double decay = Math.exp(-t * 250.0);
                        sample[i] = Math.sin(2 * Math.PI * 1500 * t) * decay;
                    }

                    int idx = 0;
                    for (double dVal : sample) {
                        short val = (short) (dVal * 15000);
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

    public static void playRefreshTrigger() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int sampleRate = 44100;
                    int durationMs = 250;
                    int numSamples = (int) (sampleRate * (durationMs / 1000.0));
                    double[] sample = new double[numSamples];
                    byte[] generatedSnd = new byte[2 * numSamples];

                    for (int i = 0; i < numSamples; ++i) {
                        double t = (double) i / sampleRate;
                        double decay = Math.exp(-t * 12.0);
                        double freq = 400.0 + (t * 800.0);
                        sample[i] = (Math.sin(2 * Math.PI * freq * t) + Math.sin(2 * Math.PI * (freq * 1.5) * t)) * 0.5 * decay;
                    }

                    int idx = 0;
                    for (double dVal : sample) {
                        short val = (short) (dVal * 25000);
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

    public static void playRefreshComplete() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int sampleRate = 44100;
                    int durationMs = 400;
                    int numSamples = (int) (sampleRate * (durationMs / 1000.0));
                    double[] sample = new double[numSamples];
                    byte[] generatedSnd = new byte[2 * numSamples];

                    for (int i = 0; i < numSamples; ++i) {
                        double t = (double) i / sampleRate;
                        double decay = Math.exp(-t * 6.0);
                        double freq1 = 523.25;
                        double freq2 = 659.25;
                        double freq3 = 783.99;
                        double signal = Math.sin(2 * Math.PI * freq1 * t) +
                                        Math.sin(2 * Math.PI * freq2 * t) +
                                        Math.sin(2 * Math.PI * freq3 * t);
                        sample[i] = (signal / 3.0) * decay;
                    }

                    int idx = 0;
                    for (double dVal : sample) {
                        short val = (short) (dVal * 25000);
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