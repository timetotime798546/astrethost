package com.ludoclassic.app;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

public class SoundManager {
    private static final int SAMPLE_RATE = 8000;

    public static void playSound(final int type) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    byte[] buffer = null;
                    if (type == 1) { 
                        buffer = generateRollSound();
                    } else if (type == 2) { 
                        buffer = generateHopSound();
                    } else if (type == 3) { 
                        buffer = generateCaptureSound();
                    } else if (type == 4) { 
                        buffer = generateWinSound();
                    }

                    if (buffer != null) {
                        AudioTrack audioTrack = new AudioTrack(
                                AudioManager.STREAM_MUSIC,
                                SAMPLE_RATE,
                                AudioFormat.CHANNEL_OUT_MONO,
                                AudioFormat.ENCODING_PCM_8BIT,
                                buffer.length,
                                AudioTrack.MODE_STATIC
                        );
                        audioTrack.write(buffer, 0, buffer.length);
                        audioTrack.play();
                        
                        Thread.sleep(buffer.length * 1000L / SAMPLE_RATE + 50);
                        audioTrack.stop();
                        audioTrack.release();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private static byte[] generateRollSound() {
        int duration = (int) (SAMPLE_RATE * 0.25); 
        byte[] buffer = new byte[duration];
        for (int i = 0; i < duration; i++) {
            double frequency = 120 + 80 * Math.sin(2 * Math.PI * i / 120.0);
            double angle = 2.0 * Math.PI * i * frequency / SAMPLE_RATE;
            buffer[i] = (byte) (Math.sin(angle) * 127);
        }
        return buffer;
    }

    private static byte[] generateHopSound() {
        int duration = (int) (SAMPLE_RATE * 0.08); 
        byte[] buffer = new byte[duration];
        for (int i = 0; i < duration; i++) {
            double freq = 350 + (450 * ((double) i / duration));
            double angle = 2.0 * Math.PI * i * freq / SAMPLE_RATE;
            buffer[i] = (byte) (Math.sin(angle) * 127);
        }
        return buffer;
    }

    private static byte[] generateCaptureSound() {
        int duration = (int) (SAMPLE_RATE * 0.35); 
        byte[] buffer = new byte[duration];
        for (int i = 0; i < duration; i++) {
            double freq = 900 - (700 * ((double) i / duration));
            double angle = 2.0 * Math.PI * i * freq / SAMPLE_RATE;
            double decay = 1.0 - ((double) i / duration);
            buffer[i] = (byte) (Math.sin(angle) * 127 * decay);
        }
        return buffer;
    }

    private static byte[] generateWinSound() {
        int duration = (int) (SAMPLE_RATE * 0.5); 
        byte[] buffer = new byte[duration];
        for (int i = 0; i < duration; i++) {
            double progress = (double) i / duration;
            double freq = 523.25; 
            if (progress > 0.25 && progress <= 0.5) {
                freq = 659.25; 
            } else if (progress > 0.5 && progress <= 0.75) {
                freq = 783.99; 
            } else if (progress > 0.75) {
                freq = 1046.50; 
            }
            double angle = 2.0 * Math.PI * i * freq / SAMPLE_RATE;
            buffer[i] = (byte) (Math.sin(angle) * 127);
        }
        return buffer;
    }
}