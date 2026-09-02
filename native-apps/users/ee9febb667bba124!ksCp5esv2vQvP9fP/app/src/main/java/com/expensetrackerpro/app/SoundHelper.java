package com.expensetrackerpro.app;

import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;
import android.os.Looper;

public class SoundHelper {
    private ToneGenerator toneGenerator;

    public SoundHelper() {
        try {
            toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 85);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void playSuccessSound() {
        if (toneGenerator != null) {
            try {
                toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 100);
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 120);
                        } catch (Exception ignored) {}
                    }
                }, 130);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void playDeleteSound() {
        if (toneGenerator != null) {
            try {
                toneGenerator.startTone(ToneGenerator.TONE_PROP_NACK, 250);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void playClickSound() {
        if (toneGenerator != null) {
            try {
                toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 50);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void playPhysicsTick() {
        if (toneGenerator != null) {
            try {
                toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 20);
            } catch (Exception ignored) {}
        } 
    }

    public void playTossSound() {
        if (toneGenerator != null) {
            try {
                toneGenerator.startTone(ToneGenerator.TONE_CDMA_PIP, 80);
            } catch (Exception ignored) {}
        }
    }

    public void playLaunchTone() {
        if (toneGenerator != null) {
            try {
                toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 120);
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 150);
                        } catch (Exception ignored) {}
                    }
                }, 180);
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 250);
                        } catch (Exception ignored) {}
                    }
                }, 350);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void release() {
        if (toneGenerator != null) {
            toneGenerator.release();
        }
    }
}