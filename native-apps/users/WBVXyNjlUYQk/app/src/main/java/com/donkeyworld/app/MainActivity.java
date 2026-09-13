package com.donkeyworld.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import java.util.Random;

public class MainActivity extends Activity {

    // Persistent storage keys
    private static final String PREFS_NAME = "DonkeyPrefs";
    private static final String KEY_CARROTS = "carrots";
    private static final String KEY_LEVEL = "level";

    // View declarations
    private TextView levelBadge;
    private TextView asciiDonkey;
    private Button btnFeedDonkey;
    private TextView textStats;
    private ProgressBar feedingProgress;
    private TextView evolutionStatus;

    private TextView textFactIndex;
    private TextView textFactContent;
    private Button btnNextFact;

    private View layoutGame;
    private View layoutSounds;
    private View layoutFacts;

    private Button tabFeed;
    private Button tabSounds;
    private Button tabFacts;

    // Local states
    private int carrotsFed = 0;
    private int currentLevel = 1;
    private int currentFactIndex = 0;

    // Donkey Fact repository
    private final String[] donkeyFacts = {
        "Donkeys can live for up to 40 years with proper standard care and dynamic nutrition.",
        "A donkey's large, distinct ears allow them to hear another donkey from miles away in desert climates.",
        "Donkeys are not stubborn; they have a keen sense of self-preservation and will refuse tasks they deem unsafe.",
        "They are extremely clean animals and will avoid wet or soiled patches of pasture whenever possible.",
        "Donkey milk was famously used by ancient Egyptian queens to keep their skin healthy and vibrant.",
        "Donkeys protect flocks of sheep and herds of goats from predators like coyotes and domestic dogs."
    };

    // Donkey ASCII configurations based on leveling
    private final String[] donkeyASCIIForms = {
        "  ,\\_,\n  (O.O)\n  (   )\n  -\"-\"-",               // Level 1: Baby Donkey
        "   /\\_/\\\n  ( 0.0 )\n  /     \\\n  \"--\"--\"",         // Level 2: Young Donkey
        "     //\\_/\\\\\n    ( O _ O )\n    /|     |\\\n    \"\"     \"\"",  // Level 3: Majestic Donkey
        "      //\\_ _/\\\\\n     (  @ _ @  )\n     /||     ||\\\n    ==\"\"     \"\"==" // Level 4+: legendary donkey
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Load persisted progress
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        carrotsFed = prefs.getInt(KEY_CARROTS, 0);
        currentLevel = prefs.getInt(KEY_LEVEL, 1);

        initViews();
        setupNavigation();
        setupGameLogic();
        setupSoundboard();
        setupFactsEngine();

        updateUI();
    }

    private void initViews() {
        levelBadge = (TextView) findViewById(R.id.levelBadge);
        asciiDonkey = (TextView) findViewById(R.id.asciiDonkey);
        btnFeedDonkey = (Button) findViewById(R.id.btnFeedDonkey);
        textStats = (TextView) findViewById(R.id.textStats);
        feedingProgress = (ProgressBar) findViewById(R.id.feedingProgress);
        evolutionStatus = (TextView) findViewById(R.id.evolutionStatus);

        textFactIndex = (TextView) findViewById(R.id.textFactIndex);
        textFactContent = (TextView) findViewById(R.id.textFactContent);
        btnNextFact = (Button) findViewById(R.id.btnNextFact);

        layoutGame = findViewById(R.id.layoutGame);
        layoutSounds = findViewById(R.id.layoutSounds);
        layoutFacts = findViewById(R.id.layoutFacts);

        tabFeed = (Button) findViewById(R.id.tabFeed);
        tabSounds = (Button) findViewById(R.id.tabSounds);
        tabFacts = (Button) findViewById(R.id.tabFacts);
    }

    private void setupNavigation() {
        tabFeed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(1);
            }
        });

        tabSounds.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(2);
            }
        });

        tabFacts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(3);
            }
        });
    }

    private void switchTab(int tabIndex) {
        layoutGame.setVisibility(tabIndex == 1 ? View.VISIBLE : View.GONE);
        layoutSounds.setVisibility(tabIndex == 2 ? View.VISIBLE : View.GONE);
        layoutFacts.setVisibility(tabIndex == 3 ? View.VISIBLE : View.GONE);

        tabFeed.setBackgroundColor(tabIndex == 1 ? 0xFFD7CCC8 : 0xFFEFEBE9);
        tabSounds.setBackgroundColor(tabIndex == 2 ? 0xFFD7CCC8 : 0xFFEFEBE9);
        tabFacts.setBackgroundColor(tabIndex == 3 ? 0xFFD7CCC8 : 0xFFEFEBE9);
    }

    private void setupGameLogic() {
        btnFeedDonkey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                feedDonkey();
            }
        });
    }

    private void feedDonkey() {
        carrotsFed++;
        int goal = currentLevel * 10;
        
        if (carrotsFed >= goal) {
            currentLevel++;
            carrotsFed = 0;
            // Play a synthetic happy pitch to celebrate level up
            playSynthesizedBray(1.8f, 0.5f);
        } else {
            // Play a shorter regular feedback sound
            playSynthesizedBray(1.0f, 0.2f);
        }

        saveState();
        updateUI();
    }

    private void saveState() {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putInt(KEY_CARROTS, carrotsFed);
        editor.putInt(KEY_LEVEL, currentLevel);
        editor.apply();
    }

    private void updateUI() {
        levelBadge.setText("Level " + currentLevel);
        textStats.setText("Carrots Fed: " + carrotsFed + " / " + (currentLevel * 10));
        
        feedingProgress.setMax(currentLevel * 10);
        feedingProgress.setProgress(carrotsFed);

        evolutionStatus.setText("Feed " + ((currentLevel * 10) - carrotsFed) + " more carrots to Level Up!");

        // Determine which ASCII form to show
        int formIndex = currentLevel - 1;
        if (formIndex >= donkeyASCIIForms.length) {
            formIndex = donkeyASCIIForms.length - 1;
        }
        asciiDonkey.setText(donkeyASCIIForms[formIndex]);
    }

    private void setupSoundboard() {
        findViewById(R.id.btnBrayClassic).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playSynthesizedBray(1.0f, 0.8dp); // Normal hee-haw
            }
        });

        findViewById(R.id.btnBrayExcited).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playSynthesizedBray(1.5f, 0.5f); // Fast bray
            }
        });

        findViewById(R.id.btnBraySleepy).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playSynthesizedBray(0.6f, 1.2f); // Slow deep bray
            }
        });
    }

    private void setupFactsEngine() {
        btnNextFact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentFactIndex = (currentFactIndex + 1) % donkeyFacts.length;
                textFactIndex.setText("Fact #" + (currentFactIndex + 1));
                textFactContent.setText(donkeyFacts[currentFactIndex]);
            }
        });
    }

    /**
     * Programmatic high-quality Donkey Sound Bray (Hee-Haw) synthesizer.
     * Modulates high/low audio frequencies in standard PCM chunks to output via AudioTrack.
     */
    private void playSynthesizedBray(final float speedFactor, final float volume) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final int sampleRate = 22050;
                // Basic duration scaling
                int durationSamples = (int) (sampleRate * 1.2f / speedFactor);
                short[] audioBuffer = new short[durationSamples];

                for (int i = 0; i < durationSamples; i++) {
                    double progress = (double) i / durationSamples;
                    double frequency;

                    // Synthesizing classic segmented sound (Hee vs Haw transition)
                    if (progress < 0.35) {
                        // "Hee": Fast, rising high frequency pitch (750Hz - 1100Hz)
                        double phaseProgress = progress / 0.35;
                        frequency = 750.0 + 350.0 * phaseProgress;
                    } else if (progress >= 0.35 && progress < 0.45) {
                        // Brief rasp silence gap transition
                        frequency = 0;
                    } else {
                        // "Haw": Low, dropping gruff frequency pitch (400Hz down to 200Hz)
                        double phaseProgress = (progress - 0.45) / 0.55;
                        frequency = 420.0 - 220.0 * phaseProgress;
                    }

                    if (frequency > 0) {
                        // Simple sine modulator with artificial amplitude raspiness
                        double noiseComponent = Math.sin(2 * Math.PI * (frequency * 1.5) * progress) * 0.2;
                        double coreWave = Math.sin(2 * Math.PI * frequency * ((double) i / sampleRate));
                        audioBuffer[i] = (short) ((coreWave + noiseComponent) * 32767 * 0.4f * volume);
                    } else {
                        audioBuffer[i] = 0;
                    }
                }

                try {
                    AudioTrack track = new AudioTrack(
                        AudioManager.STREAM_MUSIC,
                        sampleRate,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        audioBuffer.length * 2,
                        AudioTrack.MODE_STATIC
                    );
                    track.write(audioBuffer, 0, audioBuffer.length);
                    track.play();
                } catch (Exception e) {
                    // Fail-safe handling for legacy or restricted hardware configurations
                }
            }
        }).start();
    }
}