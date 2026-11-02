package com.brickgameclassic.app;

import android.media.AudioManager;
import android.media.ToneGenerator;

/**
 * Synthesizes retro 8-bit piezo buzzer sounds using Android ToneGenerator.
 * Fully native, zero files required.
 */
public class SoundManager {
    private ToneGenerator toneGen;
    private boolean soundEnabled = true;

    public SoundManager() {
        try {
            toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 70);
        } catch (Exception e) {
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
            playTone(ToneGenerator.TONE_PROP_BEEP, 50);
        }
    }

    public void playMove() {
        playTone(ToneGenerator.TONE_PROP_ACK, 25);
    }

    public void playRotate() {
        playTone(ToneGenerator.TONE_PROP_PROMPT, 30);
    }

    public void playDrop() {
        playTone(ToneGenerator.TONE_PROP_BEEP2, 40);
    }

    public void playClear() {
        playTone(ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE, 120);
    }

    public void playGameOver() {
        playTone(ToneGenerator.TONE_CDMA_ABBR_INTERCEPT, 350);
    }

    public void playStart() {
        playTone(ToneGenerator.TONE_CDMA_CONFIRM, 150);
    }

    private void playTone(final int toneType, final int durationMs) {
        if (!soundEnabled || toneGen == null) return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (toneGen != null) {
                        toneGen.startTone(toneType, durationMs);
                    }
                } catch (Exception ignored) {
                }
            }
        }).start();
    }

    public void release() {
        if (toneGen != null) {
            try {
                toneGen.release();
            } catch (Exception ignored) {}
            toneGen = null;
        }
    }
}