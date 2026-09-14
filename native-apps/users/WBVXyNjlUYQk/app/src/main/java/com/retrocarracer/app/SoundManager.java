package com.retrocarracer.app;

import android.media.AudioManager;
import android.media.ToneGenerator;

public class SoundManager {
    private ToneGenerator mToneGenerator;

    public SoundManager() {
        try {
            mToneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
        } catch (Exception e) {
            mToneGenerator = null;
        }
    }

    public void playScoreSound() {
        if (mToneGenerator != null) {
            mToneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 100);
        }
    }

    public void playCoinSound() {
        if (mToneGenerator != null) {
            mToneGenerator.startTone(ToneGenerator.TONE_CDMA_PIP, 80);
        }
    }

    public void playCrashSound() {
        if (mToneGenerator != null) {
            mToneGenerator.startTone(ToneGenerator.TONE_SUP_ERROR, 500);
        }
    }

    public void release() {
        if (mToneGenerator != null) {
            mToneGenerator.release();
        }
    }
}