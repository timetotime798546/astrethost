package com.carracerpro.app;

import android.media.AudioManager;
import android.media.ToneGenerator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SoundManager {
    private ToneGenerator toneGenerator;
    private ExecutorService executor;

    public SoundManager() {
        try {
            toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 85);
            executor = Executors.newSingleThreadExecutor();
        } catch (Exception e) {
            toneGenerator = null;
        }
    }

    public void playCoinSound() {
        if (toneGenerator != null && executor != null) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 80);
                    } catch (Exception e) {
                        // safe catch
                    }
                }
            });
        }
    }

    public void playCrashSound() {
        if (toneGenerator != null && executor != null) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        toneGenerator.startTone(ToneGenerator.TONE_SUP_ERROR, 350);
                    } catch (Exception e) {
                        // safe catch
                    }
                }
            });
        }
    }

    public void playLevelUpSound() {
        if (toneGenerator != null && executor != null) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        toneGenerator.startTone(ToneGenerator.TONE_DTMF_D, 150);
                        Thread.sleep(130);
                        toneGenerator.startTone(ToneGenerator.TONE_DTMF_A, 250);
                    } catch (Exception e) {
                        // safe catch
                    }
                }
            });
        }
    }

    public void release() {
        if (toneGenerator != null) {
            toneGenerator.release();
        }
        if (executor != null) {
            executor.shutdown();
        }
    }
}