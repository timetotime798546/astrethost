package com.inventorysalesmanager.app;

import android.media.AudioManager;
import android.media.ToneGenerator;

public class SoundManager {
    private static ToneGenerator toneGenerator;

    private static ToneGenerator getToneGenerator() {
        if (toneGenerator == null) {
            try {
                toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 85);
            } catch (Exception e) {
                // Keep silent on systems where hardware resources are unavailable
            }
        }
        return toneGenerator;
    }

    public static void playSuccess() {
        try {
            ToneGenerator tg = getToneGenerator();
            if (tg != null) {
                tg.startTone(ToneGenerator.TONE_PROP_ACK, 200);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void playError() {
        try {
            ToneGenerator tg = getToneGenerator();
            if (tg != null) {
                tg.startTone(ToneGenerator.TONE_PROP_NACK, 300);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void playBeep() {
        try {
            ToneGenerator tg = getToneGenerator();
            if (tg != null) {
                tg.startTone(ToneGenerator.TONE_PROP_BEEP, 100);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}