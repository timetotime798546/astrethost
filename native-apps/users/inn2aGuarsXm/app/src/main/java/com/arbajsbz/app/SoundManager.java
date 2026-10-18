package com.arbajsbz.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.ToneGenerator;

public class SoundManager {
    private static SoundManager instance;
    private final Context context;
    private ToneGenerator toneGenerator;
    private boolean soundEnabled = true;
    private boolean musicEnabled = true;

    private SoundManager(Context ctx) {
        this.context = ctx.getApplicationContext();
        loadPreferences();
        try {
            toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 85);
        } catch (Exception e) {
            toneGenerator = null;
        }
    }

    public static synchronized SoundManager getInstance(Context context) {
        if (instance == null) {
            instance = new SoundManager(context);
        }
        return instance;
    }

    private void loadPreferences() {
        SharedPreferences sp = context.getSharedPreferences("arbajsbz_audio_prefs", Context.MODE_PRIVATE);
        soundEnabled = sp.getBoolean("sound_effects", true);
        musicEnabled = sp.getBoolean("music_enabled", true);
    }

    public void setSoundEnabled(boolean enabled) {
        this.soundEnabled = enabled;
        SharedPreferences sp = context.getSharedPreferences("arbajsbz_audio_prefs", Context.MODE_PRIVATE);
        sp.edit().putBoolean("sound_effects", enabled).apply();
    }

    public void setMusicEnabled(boolean enabled) {
        this.musicEnabled = enabled;
        SharedPreferences sp = context.getSharedPreferences("arbajsbz_audio_prefs", Context.MODE_PRIVATE);
        sp.edit().putBoolean("music_enabled", enabled).apply();
    }

    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    public boolean isMusicEnabled() {
        return musicEnabled;
    }

    public void playButtonClick() {
        if (!soundEnabled || toneGenerator == null) return;
        try {
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 40);
        } catch (Exception ignored) {}
    }

    public void playCountdown() {
        if (!soundEnabled || toneGenerator == null) return;
        try {
            toneGenerator.startTone(ToneGenerator.TONE_CDMA_PIP, 60);
        } catch (Exception ignored) {}
    }

    public void playTakeoff() {
        if (!soundEnabled) return;
        playSynthesizedTone(350, 600, 250);
    }

    public void playWinCashout() {
        if (!soundEnabled) return;
        playSynthesizedTone(523, 880, 200);
    }

    public void playCrash() {
        if (!soundEnabled || toneGenerator == null) return;
        try {
            toneGenerator.startTone(ToneGenerator.TONE_SUP_ERROR, 220);
        } catch (Exception ignored) {}
    }

    private void playSynthesizedTone(final int startFreq, final int endFreq, final int durationMs) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int sampleRate = 8000;
                    int numSamples = durationMs * sampleRate / 1000;
                    double[] sample = new double[numSamples];
                    byte[] generatedSnd = new byte[2 * numSamples];

                    for (int i = 0; i < numSamples; ++i) {
                        double currentFreq = startFreq + ((double) i / numSamples) * (endFreq - startFreq);
                        sample[i] = Math.sin(2 * Math.PI * i / (sampleRate / currentFreq));
                    }

                    int idx = 0;
                    for (double dVal : sample) {
                        short val = (short) ((dVal * 32767));
                        generatedSnd[idx++] = (byte) (val & 0x00ff);
                        generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
                    }

                    AudioTrack audioTrack = new AudioTrack(
                            AudioManager.STREAM_MUSIC,
                            sampleRate,
                            AudioFormat.CHANNEL_OUT_MONO,
                            AudioFormat.ENCODING_PCM_16BIT,
                            generatedSnd.length,
                            AudioTrack.MODE_STATIC
                    );
                    audioTrack.write(generatedSnd, 0, generatedSnd.length);
                    audioTrack.play();
                    Thread.sleep(durationMs + 20);
                    audioTrack.release();
                } catch (Exception ignored) {}
            }
        }).start();
    }
}