package com.cricketpro3d.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Lightweight, zero-latency native audio engine using pure Android SDK AudioTrack.
 * Generates crisp, realistic procedural cricket SFX with zero external asset files:
 * 1. Bat Swing Whoosh
 * 2. Ball-on-Willow Timber Crack (Sweet spot strike)
 * 3. Ball-on-Willow Six Power Blast
 * 4. Stumps Shattering Clatter
 * 5. Crowd Cheering & Whistle
 * 6. Keeper Glove Thud / Dot Ball Miss
 */
public class SoundManager {

    private static final String PREF_SOUND = "cricket_sound_enabled";
    private static final int SAMPLE_RATE = 22050;

    private static SoundManager instance;
    private final SharedPreferences prefs;
    private boolean soundEnabled;
    private final ExecutorService audioExecutor = Executors.newFixedThreadPool(2);
    private final Random random = new Random();

    // Pre-synthesized PCM buffers for instantaneous playback
    private byte[] sfxBatWhoosh;
    private byte[] sfxBatCrackFour;
    private byte[] sfxBatCrackSix;
    private byte[] sfxWicketCrash;
    private byte[] sfxCrowdCheer;
    private byte[] sfxKeeperThud;

    private SoundManager(Context context) {
        prefs = context.getSharedPreferences("cricket_audio_prefs", Context.MODE_PRIVATE);
        soundEnabled = prefs.getBoolean(PREF_SOUND, true);
        generateAllAudioBuffers();
    }

    public static synchronized SoundManager getInstance(Context context) {
        if (instance == null) {
            instance = new SoundManager(context.getApplicationContext());
        }
        return instance;
    }

    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    public void setSoundEnabled(boolean enabled) {
        this.soundEnabled = enabled;
        prefs.edit().putBoolean(PREF_SOUND, enabled).apply();
    }

    public void toggleSound() {
        setSoundEnabled(!soundEnabled);
    }

    private void generateAllAudioBuffers() {
        audioExecutor.execute(new Runnable() {
            @Override
            public void run() {
                sfxBatWhoosh = generateWhooshBuffer();
                sfxBatCrackFour = generateWoodCrackBuffer(false);
                sfxBatCrackSix = generateWoodCrackBuffer(true);
                sfxWicketCrash = generateWicketBuffer();
                sfxCrowdCheer = generateCrowdCheerBuffer();
                sfxKeeperThud = generateKeeperThudBuffer();
            }
        });
    }

    // --- Sound Triggers ---

    public void playBatWhoosh() {
        if (!soundEnabled || sfxBatWhoosh == null) return;
        playPcmBuffer(sfxBatWhoosh);
    }

    public void playBatHit(boolean isSix) {
        if (!soundEnabled) return;
        byte[] buffer = isSix ? sfxBatCrackSix : sfxBatCrackFour;
        if (buffer != null) {
            playPcmBuffer(buffer);
        }
    }

    public void playWicket() {
        if (!soundEnabled || sfxWicketCrash == null) return;
        playPcmBuffer(sfxWicketCrash);
    }

    public void playCrowdCheer() {
        if (!soundEnabled || sfxCrowdCheer == null) return;
        playPcmBuffer(sfxCrowdCheer);
    }

    public void playMissBall() {
        if (!soundEnabled || sfxKeeperThud == null) return;
        playPcmBuffer(sfxKeeperThud);
    }

    // --- Audio Track Dispatcher ---

    private void playPcmBuffer(final byte[] pcmData) {
        audioExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    int bufferSize = pcmData.length;
                    AudioTrack track;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        AudioAttributes attributes = new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_GAME)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build();
                        AudioFormat format = new AudioFormat.Builder()
                                .setSampleRate(SAMPLE_RATE)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .build();
                        track = new AudioTrack(attributes, format, bufferSize, AudioTrack.MODE_STATIC, AudioManager.AUDIO_SESSION_ID_GENERATE);
                    } else {
                        track = new AudioTrack(
                                AudioManager.STREAM_MUSIC,
                                SAMPLE_RATE,
                                AudioFormat.CHANNEL_OUT_MONO,
                                AudioFormat.ENCODING_PCM_16BIT,
                                bufferSize,
                                AudioTrack.MODE_STATIC
                        );
                    }

                    track.write(pcmData, 0, bufferSize);
                    track.play();

                    // Cleanup track after playback length
                    int durationMs = (bufferSize * 1000) / (SAMPLE_RATE * 2);
                    Thread.sleep(durationMs + 60);
                    track.stop();
                    track.release();
                } catch (Exception ignored) {}
            }
        });
    }

    // --- Synthetic Wave Generators (Pure Math / PCM 16-bit Mono) ---

    private byte[] generateWhooshBuffer() {
        int numSamples = (int) (SAMPLE_RATE * 0.22); // 220ms
        byte[] buffer = new byte[numSamples * 2];
        double filterState = 0.0;

        for (int i = 0; i < numSamples; i++) {
            double progress = (double) i / numSamples;
            double env = Math.sin(progress * Math.PI); // Smooth envelope
            double whiteNoise = (random.nextDouble() * 2.0) - 1.0;

            // Low-pass sweeping cutoff for whoosh sound
            double cutoff = 0.05 + 0.35 * Math.sin(progress * Math.PI);
            filterState += cutoff * (whiteNoise - filterState);

            double sample = filterState * env * 0.75;
            short val = (short) (sample * 32767.0);

            buffer[i * 2] = (byte) (val & 0xff);
            buffer[i * 2 + 1] = (byte) ((val >> 8) & 0xff);
        }
        return buffer;
    }

    private byte[] generateWoodCrackBuffer(boolean isSix) {
        int numSamples = (int) (SAMPLE_RATE * (isSix ? 0.35 : 0.24));
        byte[] buffer = new byte[numSamples * 2];

        double freqBase = isSix ? 340.0 : 420.0;
        double woodThump = isSix ? 110.0 : 135.0;

        for (int i = 0; i < numSamples; i++) {
            double t = (double) i / SAMPLE_RATE;
            double progress = (double) i / numSamples;

            // Sharp, high-impact exponential attack
            double decay = Math.exp(-t * 22.0);
            double highCrack = Math.sin(2.0 * Math.PI * (freqBase + (1.0 - progress) * 200.0) * t);
            double lowThud = Math.sin(2.0 * Math.PI * woodThump * t) * Math.exp(-t * 12.0);
            double impactNoise = ((random.nextDouble() * 2.0) - 1.0) * Math.exp(-t * 70.0);

            double sample = (highCrack * 0.55 + lowThud * 0.45 + impactNoise * 0.70) * decay;
            if (sample > 1.0) sample = 1.0;
            if (sample < -1.0) sample = -1.0;

            short val = (short) (sample * 32767.0);
            buffer[i * 2] = (byte) (val & 0xff);
            buffer[i * 2 + 1] = (byte) ((val >> 8) & 0xff);
        }
        return buffer;
    }

    private byte[] generateWicketBuffer() {
        int numSamples = (int) (SAMPLE_RATE * 0.48); // 480ms
        byte[] buffer = new byte[numSamples * 2];

        for (int i = 0; i < numSamples; i++) {
            double t = (double) i / SAMPLE_RATE;
            // Primary timber knock
            double timber1 = Math.sin(2.0 * Math.PI * 260.0 * t) * Math.exp(-t * 16.0);
            // Secondary bounce clatter
            double clatter = 0.0;
            if (t > 0.08) {
                clatter = Math.sin(2.0 * Math.PI * 480.0 * (t - 0.08)) * Math.exp(-(t - 0.08) * 24.0);
            }
            if (t > 0.16) {
                clatter += Math.sin(2.0 * Math.PI * 650.0 * (t - 0.16)) * Math.exp(-(t - 0.16) * 30.0);
            }
            double noise = ((random.nextDouble() * 2.0) - 1.0) * Math.exp(-t * 35.0);

            double sample = (timber1 * 0.6 + clatter * 0.4 + noise * 0.35);
            if (sample > 1.0) sample = 1.0;
            if (sample < -1.0) sample = -1.0;

            short val = (short) (sample * 32767.0);
            buffer[i * 2] = (byte) (val & 0xff);
            buffer[i * 2 + 1] = (byte) ((val >> 8) & 0xff);
        }
        return buffer;
    }

    private byte[] generateCrowdCheerBuffer() {
        int numSamples = (int) (SAMPLE_RATE * 1.25); // 1.25 seconds crowd swell
        byte[] buffer = new byte[numSamples * 2];
        double filter1 = 0.0;
        double filter2 = 0.0;

        for (int i = 0; i < numSamples; i++) {
            double progress = (double) i / numSamples;
            // Swell envelope: rises quickly then tapers off
            double env = Math.pow(Math.sin(progress * Math.PI), 0.7);

            double noise = (random.nextDouble() * 2.0) - 1.0;
            filter1 += 0.18 * (noise - filter1);
            filter2 += 0.09 * (filter1 - filter2);

            // Subtle celebration whistle tone modulated in background
            double whistle = Math.sin(2.0 * Math.PI * 1850.0 * ((double) i / SAMPLE_RATE)) * 0.08 * env;

            double sample = (filter2 * 0.85 + whistle) * env * 0.75;
            if (sample > 1.0) sample = 1.0;
            if (sample < -1.0) sample = -1.0;

            short val = (short) (sample * 32767.0);
            buffer[i * 2] = (byte) (val & 0xff);
            buffer[i * 2 + 1] = (byte) ((val >> 8) & 0xff);
        }
        return buffer;
    }

    private byte[] generateKeeperThudBuffer() {
        int numSamples = (int) (SAMPLE_RATE * 0.16);
        byte[] buffer = new byte[numSamples * 2];

        for (int i = 0; i < numSamples; i++) {
            double t = (double) i / SAMPLE_RATE;
            double decay = Math.exp(-t * 32.0);
            double thud = Math.sin(2.0 * Math.PI * 95.0 * t) * decay;
            double gloveNoise = ((random.nextDouble() * 2.0) - 1.0) * Math.exp(-t * 55.0);

            double sample = (thud * 0.7 + gloveNoise * 0.3);
            short val = (short) (sample * 32767.0);
            buffer[i * 2] = (byte) (val & 0xff);
            buffer[i * 2 + 1] = (byte) ((val >> 8) & 0xff);
        }
        return buffer;
    }
}