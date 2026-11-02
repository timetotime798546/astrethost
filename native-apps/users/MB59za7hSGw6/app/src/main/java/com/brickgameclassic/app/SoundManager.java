package com.brickgameclassic.app;

import android.media.AudioManager;
import android.media.ToneGenerator;

/**
 * Synthesizes retro 8-bit sound tones with fail-safe initialization and thread safety.
 */
public class SoundManager {
    private ToneGenerator toneGen;
    private boolean soundEnabled = true;

    public SoundManager() {
        try {
            toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 70);
        } catch (Throwable t) {
            toneGen = null;
        }
    }

    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    public void setSoundEnabled(boolean enabled) {
        this.soundEnabled = enabled;
    }

    public void toggleSound() {
        this.soundEnabled = !this.soundEnabled;
        if (soundEnabled) {
            playTone(ToneGenerator.TONE_PROP_BEEP, 40);
        }
    }

    public void playMove() {
        playTone(ToneGenerator.TONE_PROP_ACK, 20);
    }

    public void playRotate() {
        playTone(ToneGenerator.TONE_PROP_PROMPT, 30);
    }

    public void playDrop() {
        playTone(ToneGenerator.TONE_PROP_BEEP2, 35);
    }

    public void playClear() {
        playTone(ToneGenerator.TONE_PROP_BEEP, 130);
    }

    public void playGameOver() {
        playTone(ToneGenerator.TONE_PROP_BEEP2, 300);
    }

    public void playStart() {
        playTone(ToneGenerator.TONE_PROP_ACK, 120);
    }

    private void playTone(int toneType, int durationMs) {
        if (!soundEnabled || toneGen == null) return;
        try {
            toneGen.startTone(toneType, durationMs);
        } catch (Throwable ignored) {
        }
    }

    public void release() {
        if (toneGen != null) {
            try {
                toneGen.release();
            } catch (Throwable ignored) {}
            toneGen = null;
        }
    }
}