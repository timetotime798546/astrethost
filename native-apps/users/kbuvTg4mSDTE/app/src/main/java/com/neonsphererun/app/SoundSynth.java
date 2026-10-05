package com.neonsphererun.app;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

public class SoundSynth {

    public static void playMove() {
        playTone(600f, 800f, 60);
    }

    public static void playSpeedUp() {
        playTone(400f, 1000f, 150);
    }

    public static void playCrash() {
        playTone(180f, 40f, 400);
    }

    @SuppressWarnings("deprecation")
    private static void playTone(final float startFreq, final float endFreq, final int durationMs) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int sampleRate = 8000;
                    int numSamples = durationMs * sampleRate / 1000;
                    byte[] generatedSnd = new byte[2 * numSamples];
                    
                    for (int i = 0; i < numSamples; ++i) {
                        float t = (float) i / numSamples;
                        float freq = startFreq + (endFreq - startFreq) * t;
                        double angle = 2.0 * Math.PI * i / (sampleRate / freq);
                        
                        // Arcade style square/saw wave approximation for a heavy retro sound
                        double sampleVal = Math.sin(angle);
                        if (sampleVal > 0.4) {
                            sampleVal = 0.7;
                        } else if (sampleVal < -0.4) {
                            sampleVal = -0.7;
                        } else {
                            sampleVal = 0.0;
                        }
                        
                        short val = (short) (sampleVal * 20000 * (1.0f - t)); // exponential sweep/fade decay
                        generatedSnd[2 * i] = (byte) (val & 0x00ff);
                        generatedSnd[2 * i + 1] = (byte) ((val & 0xff00) >>> 8);
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
                    Thread.sleep(durationMs + 20);
                    audioTrack.release();
                } catch (Exception e) {
                    // Silently fail if audio devices are occupied
                }
            }
        }).start();
    }
}