package com.flashlight3d.app;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

/**
 * Procedural Synthesized Sound effect generator to deliver realistic physical tactile click sounds
 * without adding external assets, keeping the environment lightweight and robust.
 */
public class SoundGenerator {

    public static void playTactileClick(final boolean stateOn) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int sampleRate = 44100;
                    double duration = 0.04; // Very snappy 40ms mechanical switch sound
                    int numSamples = (int) (duration * sampleRate);
                    double[] sample = new double[numSamples];
                    byte[] generatedSnd = new byte[2 * numSamples];

                    // Select tone: switch ON is crisp and higher pitch, OFF is heavy metallic drop tone
                    double frequency = stateOn ? 1400.0 : 750.0;

                    for (int i = 0; i < numSamples; ++i) {
                        double t = (double) i / sampleRate;
                        // Sharp exponential envelope decay mimicking a physical click
                        double envelope = Math.exp(-85.0 * t);
                        // Blend a base tone and subtle static white noise for realistic friction texture
                        double noise = Math.random() * 0.15;
                        sample[i] = (Math.sin(2 * Math.PI * frequency * t) + noise) * envelope;
                    }

                    int idx = 0;
                    for (double dVal : sample) {
                        // Clamp value boundary safety check
                        if (dVal > 1.0) dVal = 1.0;
                        else if (dVal < -1.0) dVal = -1.0;
                        
                        short val = (short) (dVal * 32767);
                        generatedSnd[idx++] = (byte) (val & 0x00ff);
                        generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
                    }

                    AudioTrack audioTrack = new AudioTrack(
                            AudioManager.STREAM_SYSTEM,
                            sampleRate,
                            AudioFormat.CHANNEL_OUT_MONO,
                            AudioFormat.ENCODING_PCM_16BIT,
                            generatedSnd.length,
                            AudioTrack.MODE_STATIC
                    );

                    audioTrack.write(generatedSnd, 0, generatedSnd.length);
                    audioTrack.play();

                    // Block thread slightly to let audio execute properly and clear memory trace
                    Thread.sleep((long) (duration * 1000) + 100);
                    audioTrack.release();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}