package com.helloapp.app;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

/**
 * Pure Java native dynamic real-time Sound Synthesizer.
 * Generates custom synthesized wavetable sound effects dynamically using AudioTrack.
 * No external file dependencies required. Supports instant volume muting.
 */
public class SoundEngine {
    private static final int SAMPLE_RATE = 22050; // Lightweight optimized sample rate
    private static boolean soundEnabled = true;

    public static void setSoundEnabled(boolean enabled) {
        soundEnabled = enabled;
    }

    public static boolean isSoundEnabled() {
        return soundEnabled;
    }

    /**
     * Digital snap click sound. Used for quick touch confirmations.
     */
    public static void playClick() {
        if (!soundEnabled) return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                int durationSamples = (int) (SAMPLE_RATE * 0.08); // 80ms
                short[] buffer = new short[durationSamples];
                for (int i = 0; i < durationSamples; i++) {
                    double t = (double) i / SAMPLE_RATE;
                    // Exponential high frequency frequency sweep down to mid range
                    double freq = 1600.0 * Math.exp(-45.0 * t) + 180.0;
                    double angle = 2.0 * Math.PI * freq * t;
                    double envelope = Math.exp(-16.0 * t); // Swift exponential decay
                    buffer[i] = (short) (Math.sin(angle) * 16383 * envelope);
                }
                playBuffer(buffer);
            }
        }).start();
    }

    /**
     * Energetic sound signature. Fast rising 8-bit retro-synth arpeggio.
     */
    public static void playEnergetic() {
        if (!soundEnabled) return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                int durationSamples = (int) (SAMPLE_RATE * 0.45); // 450ms
                short[] buffer = new short[durationSamples];
                // Major chords progression representation
                double[] notes = {261.63, 329.63, 392.00, 523.25, 659.25, 783.99}; 
                for (int i = 0; i < durationSamples; i++) {
                    double t = (double) i / SAMPLE_RATE;
                    int noteIndex = (int) (t * 14.0) % notes.length;
                    double freq = notes[noteIndex];
                    double angle = 2.0 * Math.PI * freq * t;
                    double envelope = (1.0 - t / 0.45);
                    // Add moderate clipping for digital crunch matching "Energetic" mood
                    double sine = Math.sin(angle);
                    double square = Math.signum(sine);
                    double mixed = 0.7 * sine + 0.3 * square;
                    buffer[i] = (short) (mixed * 14000 * envelope);
                }
                playBuffer(buffer);
            }
        }).start();
    }

    /**
     * Calm sound signature. Rich, slowly decaying peaceful acoustic resonance.
     */
    public static void playCalm() {
        if (!soundEnabled) return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                int durationSamples = (int) (SAMPLE_RATE * 0.9); // 900ms
                short[] buffer = new short[durationSamples];
                for (int i = 0; i < durationSamples; i++) {
                    double t = (double) i / SAMPLE_RATE;
                    // Peaceful perfect fifth harmonic combination (D4 & A4)
                    double freq1 = 293.66; 
                    double freq2 = 440.00; 
                    double angle1 = 2.0 * Math.PI * freq1 * t;
                    double angle2 = 2.0 * Math.PI * freq2 * t;
                    double envelope = Math.exp(-3.5 * t); // Smooth gradual exponential decay
                    double wave = 0.5 * Math.sin(angle1) + 0.5 * Math.sin(angle2);
                    buffer[i] = (short) (wave * 15000 * envelope);
                }
                playBuffer(buffer);
            }
        }).start();
    }

    /**
     * Inspiring sound signature. Bright glittering celestial sweep with pitch vibrato.
     */
    public static void playInspire() {
        if (!soundEnabled) return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                int durationSamples = (int) (SAMPLE_RATE * 0.7); // 700ms
                short[] buffer = new short[durationSamples];
                for (int i = 0; i < durationSamples; i++) {
                    double t = (double) i / SAMPLE_RATE;
                    // Intense warm pitch vibrato
                    double vibrato = 1.0 + 0.04 * Math.sin(2.0 * Math.PI * 18.0 * t);
                    double freq = 880.00 * vibrato * (1.0 + 0.4 * t); // Shimmers up
                    double angle = 2.0 * Math.PI * freq * t;
                    double envelope = Math.sin(Math.PI * t / 0.7) * (1.0 - t); // Sparkle attack
                    buffer[i] = (short) (Math.sin(angle) * 15000 * envelope);
                }
                playBuffer(buffer);
            }
        }).start();
    }

    /**
     * Sci-fi volumetric calculations power-up. Custom progression based on the current atmosphere.
     */
    public static void playCalculation(final String vibe) {
        if (!soundEnabled) return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                int durationSamples = (int) (SAMPLE_RATE * 0.65); // 650ms
                short[] buffer = new short[durationSamples];
                
                double f1 = 220.0, f2 = 330.0, f3 = 440.0;
                if ("Energetic".equals(vibe)) {
                    f1 = 261.63; // C4
                    f2 = 329.63; // E4
                    f3 = 523.25; // C5
                } else if ("Calm".equals(vibe)) {
                    f1 = 349.23; // F4
                    f2 = 440.00; // A4
                    f3 = 523.25; // C5
                } else if ("Inspiring".equals(vibe)) {
                    f1 = 293.66; // D4
                    f2 = 392.00; // G4
                    f3 = 587.33; // D5
                }

                for (int i = 0; i < durationSamples; i++) {
                    double t = (double) i / SAMPLE_RATE;
                    // LFO dynamic low-frequency sweep added to basic chords
                    double sweep = 110.0 * Math.sin(2.0 * Math.PI * 4.5 * t);
                    double w1 = Math.sin(2.0 * Math.PI * (f1 + sweep) * t);
                    double w2 = Math.sin(2.0 * Math.PI * (f2 + sweep) * t);
                    double w3 = Math.sin(2.0 * Math.PI * (f3 + sweep) * t);
                    double wave = 0.4 * w1 + 0.3 * w2 + 0.3 * w3;
                    
                    // Low base impact rumble on starting sequence
                    double rumble = (t < 0.2) ? 0.35 * Math.sin(2.0 * Math.PI * 65.0 * t) : 0.0;
                    double envelope = (1.0 - t / 0.65);
                    
                    buffer[i] = (short) ((wave + rumble) * 15000 * envelope);
                }
                playBuffer(buffer);
            }
        }).start();
    }

    private static void playBuffer(short[] buffer) {
        AudioTrack audioTrack = null;
        try {
            int minBufSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE, 
                AudioFormat.CHANNEL_OUT_MONO, 
                AudioFormat.ENCODING_PCM_16BIT
            );
            int bufSize = Math.max(buffer.length * 2, minBufSize);
            audioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufSize,
                AudioTrack.MODE_STATIC
            );
            
            int written = audioTrack.write(buffer, 0, buffer.length);
            if (written > 0) {
                audioTrack.play();
                int durationMs = (int) (((double) buffer.length / SAMPLE_RATE) * 1000.0);
                Thread.sleep(durationMs + 30);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (audioTrack != null) {
                try {
                    audioTrack.stop();
                    audioTrack.release();
                } catch (Exception ignored) {}
            }
        }
    }
}