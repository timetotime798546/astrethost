package com.soundcraftaudiofx.app;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

public class AudioSynth {

    private static final int SAMPLE_RATE = 44100;

    public interface SoundCallback {
        void onSoundCompleted();
    }

    public static void playTone(final double frequency, final int durationMs, final float volume, final SoundCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int numSamples = (int) ((durationMs / 1000.0) * SAMPLE_RATE);
                    short[] buffer = new short[numSamples];

                    for (int i = 0; i < numSamples; i++) {
                        double angle = 2.0 * Math.PI * i / (SAMPLE_RATE / frequency);
                        // Apply attack/decay envelope to prevent audio clicking
                        double envelope = 1.0;
                        int fadeLen = numSamples / 10;
                        if (i < fadeLen) {
                            envelope = (double) i / fadeLen;
                        } else if (i > numSamples - fadeLen) {
                            envelope = (double) (numSamples - i) / fadeLen;
                        }
                        buffer[i] = (short) (Math.sin(angle) * Short.MAX_VALUE * volume * envelope);
                    }

                    writeAndPlayTrack(buffer);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (callback != null) {
                        callback.onSoundCompleted();
                    }
                }
            }
        }).start();
    }

    public static void playLaserFx(final float pitchMultiplier) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int durationMs = 280;
                    int numSamples = (int) ((durationMs / 1000.0) * SAMPLE_RATE);
                    short[] buffer = new short[numSamples];
                    double startFreq = 1600.0 * pitchMultiplier;
                    double endFreq = 200.0 * pitchMultiplier;

                    double phase = 0;
                    for (int i = 0; i < numSamples; i++) {
                        double factor = (double) i / numSamples;
                        double curFreq = startFreq - factor * (startFreq - endFreq);
                        phase += 2.0 * Math.PI * curFreq / SAMPLE_RATE;
                        // Sawtooth-like laser harmonics
                        double sample = Math.sin(phase) * 0.7 + Math.sin(phase * 2) * 0.3;
                        double envelope = 1.0 - factor;
                        buffer[i] = (short) (sample * Short.MAX_VALUE * 0.8 * envelope);
                    }
                    writeAndPlayTrack(buffer);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static void playCoinChime(final float pitchMultiplier) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Two rapid ascending square-pulse chimes
                    playNoteSegment(987.77 * pitchMultiplier, 110, 0.7f, 1);
                    playNoteSegment(1318.51 * pitchMultiplier, 350, 0.75f, 1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static void play8BitJump(final float pitchMultiplier) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int durationMs = 260;
                    int numSamples = (int) ((durationMs / 1000.0) * SAMPLE_RATE);
                    short[] buffer = new short[numSamples];
                    double startFreq = 220.0 * pitchMultiplier;
                    double endFreq = 950.0 * pitchMultiplier;

                    double phase = 0;
                    for (int i = 0; i < numSamples; i++) {
                        double factor = (double) i / numSamples;
                        double curFreq = startFreq + (factor * factor) * (endFreq - startFreq);
                        phase += 2.0 * Math.PI * curFreq / SAMPLE_RATE;
                        // Square wave pulse for 8-bit arcade feel
                        double sample = Math.sin(phase) >= 0 ? 0.6 : -0.6;
                        double envelope = 1.0 - (factor * 0.5);
                        buffer[i] = (short) (sample * Short.MAX_VALUE * 0.8 * envelope);
                    }
                    writeAndPlayTrack(buffer);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static void playDrumKick() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int durationMs = 300;
                    int numSamples = (int) ((durationMs / 1000.0) * SAMPLE_RATE);
                    short[] buffer = new short[numSamples];

                    double phase = 0;
                    for (int i = 0; i < numSamples; i++) {
                        double factor = (double) i / numSamples;
                        double curFreq = 160.0 * Math.exp(-factor * 12.0) + 40.0;
                        phase += 2.0 * Math.PI * curFreq / SAMPLE_RATE;
                        double sample = Math.sin(phase);
                        double envelope = Math.max(0, 1.0 - factor * 1.2);
                        // Add mild distortion punch
                        sample = Math.max(-1.0, Math.min(1.0, sample * 1.5));
                        buffer[i] = (short) (sample * Short.MAX_VALUE * 0.95 * envelope);
                    }
                    writeAndPlayTrack(buffer);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static void playSnareNoise() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int durationMs = 220;
                    int numSamples = (int) ((durationMs / 1000.0) * SAMPLE_RATE);
                    short[] buffer = new short[numSamples];

                    double tonePhase = 0;
                    for (int i = 0; i < numSamples; i++) {
                        double factor = (double) i / numSamples;
                        tonePhase += 2.0 * Math.PI * 180.0 / SAMPLE_RATE;
                        double tone = Math.sin(tonePhase) * 0.35;
                        double whiteNoise = (Math.random() * 2.0 - 1.0) * 0.65;
                        double envelope = Math.exp(-factor * 7.5);
                        buffer[i] = (short) ((tone + whiteNoise) * Short.MAX_VALUE * 0.85 * envelope);
                    }
                    writeAndPlayTrack(buffer);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static void playSirenSweep(final float pitchMultiplier) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int durationMs = 600;
                    int numSamples = (int) ((durationMs / 1000.0) * SAMPLE_RATE);
                    short[] buffer = new short[numSamples];

                    double phase = 0;
                    for (int i = 0; i < numSamples; i++) {
                        double factor = (double) i / numSamples;
                        double curFreq = (700.0 + 350.0 * Math.sin(2.0 * Math.PI * factor * 3)) * pitchMultiplier;
                        phase += 2.0 * Math.PI * curFreq / SAMPLE_RATE;
                        double sample = Math.sin(phase);
                        buffer[i] = (short) (sample * Short.MAX_VALUE * 0.8);
                    }
                    writeAndPlayTrack(buffer);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private static void playNoteSegment(double frequency, int durationMs, float volume, int waveformType) {
        int numSamples = (int) ((durationMs / 1000.0) * SAMPLE_RATE);
        short[] buffer = new short[numSamples];

        double phase = 0;
        for (int i = 0; i < numSamples; i++) {
            phase += 2.0 * Math.PI * frequency / SAMPLE_RATE;
            double sample;
            if (waveformType == 1) { // Square
                sample = Math.sin(phase) >= 0 ? 0.6 : -0.6;
            } else { // Sine
                sample = Math.sin(phase);
            }
            double envelope = 1.0 - ((double) i / numSamples);
            buffer[i] = (short) (sample * Short.MAX_VALUE * volume * envelope);
        }
        writeAndPlayTrack(buffer);
    }

    private static synchronized void writeAndPlayTrack(short[] buffer) {
        int bufferSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
        );
        int finalSize = Math.max(bufferSize, buffer.length * 2);

        AudioTrack audioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                finalSize,
                AudioTrack.MODE_STATIC
        );

        audioTrack.write(buffer, 0, buffer.length);
        audioTrack.play();

        // Release AudioTrack after playback finishes
        int sleepMs = (int) (((double) buffer.length / SAMPLE_RATE) * 1000) + 50;
        try {
            Thread.sleep(sleepMs);
        } catch (InterruptedException ignored) {
        }
        try {
            audioTrack.stop();
            audioTrack.release();
        } catch (Exception ignored) {
        }
    }
}