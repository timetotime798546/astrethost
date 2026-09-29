package com.procalculator.app;

import android.media.AudioManager;
import android.media.ToneGenerator;

public class SoundManager {
    private ToneGenerator toneGenerator;

    public SoundManager() {
        try {
            toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 70);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void playClick() {
        if (toneGenerator != null) {
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 50);
        }
    }

    public void release() {
        if (toneGenerator != null) {
            toneGenerator.release();
        }
    }
}