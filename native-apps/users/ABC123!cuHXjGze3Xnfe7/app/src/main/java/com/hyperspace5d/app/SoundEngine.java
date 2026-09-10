package com.hyperspace5d.app;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

public class SoundEngine {
    private static final int SAMPLE_RATE = 22050; // Performance efficient
    private static boolean soundEnabled = true;

    // Pitch adjustment variables based on W and V space shifting
    private static float wModifier = 0.0f;
    private static float vModifier = 0.0f;

    public static void setSoundEnabled(boolean enabled) {
        soundEnabled = enabled;
    }

    public static boolean isSoundEnabled() {
        return soundEnabled;
    }

    public static void setFrequencyModifiers(float w, float v) {
        wModifier = w;
        vModifier = v;
    }

    /**
     * Digital futuristic tactile tap sound.
     */
    public static void playClick() {
        if (!soundEnabled) return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                int samples = (int) (SAMPLE_RATE * 0.06); // Short 60ms
                short[] buffer = new short[samples];
                for (int i = 0; i < samples; i++) {
                    double t = (double) i / SAMPLE_RATE;
                    // Slide downward frequency representation
                    double freq = 1200.0 * Math.exp(-60.0 * t) + 150.0;
                    double angle = 2.0 * Math.PI * freq * t;
                    double env = Math.exp(-22.0 * t);
                    buffer[i] = (short) (Math.sin(angle) * 15000 * env);
                }
                playBuffer(buffer);
            }
        }).start();
    }

    /**
     * Dynamic rising sweep indicating dimensional transition.
     */
    public static void playSweepUp() {
        if (!soundEnabled) return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                int samples = (int) (SAMPLE_RATE * 0.35); // 350ms
                short[] buffer = new short[samples];
                for (int i = 0; i < samples; i++) {
                    double t = (double) i / SAMPLE_RATE;
                    // Synthesize smooth linear rising sweep
                    double freq = 320.0 + (t / 0.35) * 440.0;
                    double angle = 2.0 * Math.PI * freq * t;
                    double env = Math.sin(Math.PI * t / 0.35); // Attack/decay envelope
                    buffer[i] = (short) (Math.sin(angle) * 14000 * env);
                }
                playBuffer(buffer);
            }
        }).start();
    }

    /**
     * Complex real-time synthesized chords incorporating 4D and 5D slider coordinates.
     */
    public static void playDissonantResonance(final float wShift, final float vShift) {
        if (!soundEnabled) return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                int samples = (int) (SAMPLE_RATE * 0.7); // 700ms
                short[] buffer = new short[samples];

                // Scale fundamental space pitches using current slider values!
                double baseFreq = 220.0 + (wShift * 50.0); // W changes fundamental pitch
                double resonanceFreq = 330.0 + (vShift * 80.0); // V changes harmony interval

                for (int i = 0; i < samples; i++) {
                    double t = (double) i / SAMPLE_RATE;
                    
                    // Wave combinations
                    double w1 = Math.sin(2.0 * Math.PI * baseFreq * t);
                    double w2 = Math.sin(2.0 * Math.PI * resonanceFreq * t);
                    
                    // Complex sub-harmonic LFO sweep
                    double lfo = Math.sin(2.0 * Math.PI * 6.0 * t);
                    double w3 = Math.sin(2.0 * Math.PI * (baseFreq * 1.5 + lfo * 15.0) * t);

                    double mixed = 0.4 * w1 + 0.3 * w2 + 0.3 * w3;
                    double env = (1.0 - t / 0.7); // Fade envelope
                    
                    buffer[i] = (short) (mixed * 16000 * env);
                }
                playBuffer(buffer);
            }
        }).start();
    }

    private static void playBuffer(short[] buffer) {
        AudioTrack track = null;
        try {
            int minSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            );
            int trackSize = Math.max(buffer.length * 2, minSize);
            track = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                trackSize,
                AudioTrack.MODE_STATIC
            );

            int written = track.write(buffer, 0, buffer.length);
            if (written > 0) {
                track.play();
                int ms = (int) (((double) buffer.length / SAMPLE_RATE) * 1000.0);
                Thread.sleep(ms + 20);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (track != null) {
                try {
                    track.stop();
                    track.release();
                } catch (Exception ignored) {}
            }
        }
    }
}