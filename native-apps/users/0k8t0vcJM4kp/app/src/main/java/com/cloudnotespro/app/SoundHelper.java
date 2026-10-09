package com.cloudnotespro.app;

import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;

public class SoundHelper {
    public static void playSuccess(Context context) {
        playTone(ToneGenerator.TONE_PROP_ACK);
    }

    public static void playClick(Context context) {
        playTone(ToneGenerator.TONE_PROP_BEEP);
    }

    public static void playDelete(Context context) {
        playTone(ToneGenerator.TONE_PROP_BEEP2);
    }

    public static void playError(Context context) {
        playTone(ToneGenerator.TONE_PROP_NACK);
    }

    private static void playTone(final int toneType) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ToneGenerator toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 85);
                    toneGen.startTone(toneType, 120);
                    Thread.sleep(150);
                    toneGen.release();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}