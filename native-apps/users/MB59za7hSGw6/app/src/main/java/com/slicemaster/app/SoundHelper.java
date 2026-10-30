package com.slicemaster.app;

import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

public class SoundHelper {

    private Vibrator vibrator;
    private ToneGenerator toneGenerator;

    public SoundHelper(Context context) {
        try {
            vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        } catch (Exception e) {
            vibrator = null;
        }

        try {
            toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 65);
        } catch (Exception e) {
            toneGenerator = null;
        }
    }

    public void playSlice() {
        if (toneGenerator != null) {
            try {
                toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 35);
            } catch (Exception ignored) {
            }
        }
        vibrate(25);
    }

    public void playCombo() {
        if (toneGenerator != null) {
            try {
                toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 70);
            } catch (Exception ignored) {
            }
        }
        vibrate(45);
    }

    public void playBombExplode() {
        if (toneGenerator != null) {
            try {
                toneGenerator.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 180);
            } catch (Exception ignored) {
            }
        }
        vibrate(120);
    }

    private void vibrate(long milliseconds) {
        if (vibrator != null && vibrator.hasVibrator()) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(milliseconds);
                }
            } catch (Exception ignored) {
            }
        }
    }

    public void release() {
        if (toneGenerator != null) {
            try {
                toneGenerator.release();
            } catch (Exception ignored) {
            }
            toneGenerator = null;
        }
    }
}