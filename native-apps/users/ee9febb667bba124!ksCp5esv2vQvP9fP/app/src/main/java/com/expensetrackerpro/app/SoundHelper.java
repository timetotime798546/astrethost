package com.expensetrackerpro.app;

import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;
import android.os.Looper;

public class SoundHelper {
    private ToneGenerator toneGenerator;

    public SoundHelper() {
        try {
            // Initialize sound synth framework utilizing built-in device synthesizer output
            toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 85);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void playSuccessSound() {
        if (toneGenerator != null) {
            try {
                // Dual high-pitched pleasant chime chime representing action completeness
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
                // Deeper double beep tone to express deletion or alert warning action
                toneGenerator.startTone(ToneGenerator.TONE_PROP_NACK, 250);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void playClickSound() {
        if (toneGenerator != null) {
            try {
                // Short click sound cue for tabs and general selector switching
                toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 50);
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