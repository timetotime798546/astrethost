package com.soundmemorychampion.app;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Random;

public class MainActivity extends Activity {

    // Game Modes Constants
    private static final int MODE_MEMORY = 0;
    private static final int MODE_EAR_TRAIN = 1;
    private static final int MODE_FREE_PLAY = 2;

    private int currentMode = MODE_MEMORY;

    // Frequencies for C Major triad tones: C4 (Red), E4 (Yellow), G4 (Blue), C5 (Green)
    private final double[] FREQUENCIES = {261.63, 329.63, 392.00, 523.25};
    
    // Low-light colors for normal state
    private final int[] DIM_COLORS = {
        0xFF7F0000, // Dim Red
        0xFF7F7F00, // Dim Yellow
        0xFF00007F, // Dim Blue
        0xFF005F00  // Dim Green
    };

    // Neon bright colors for active states
    private final int[] BRIGHT_COLORS = {
        0xFFFF1744, // Bright Neon Red
        0xFFFFEA00, // Bright Neon Yellow
        0xFF2979FF, // Bright Neon Blue
        0xFF00E676  // Bright Neon Green
    };

    private View[] pads = new View[4];
    private TextView tvScore;
    private TextView tvHighScore;
    private TextView tvStatus;
    private TextView tvInstructionTitle;
    private TextView tvInstructionBody;

    private Button btnModeMemory;
    private Button btnModeEar;
    private Button btnModeFree;
    private Button btnStartGame;
    private Button btnReplay;

    // Game states variables
    private ArrayList<Integer> gameSequence = new ArrayList<>();
    private int userStepIndex = 0;
    private boolean isPlayingSequence = false;
    private int currentScore = 0;
    private int highScoreMemory = 0;
    private int highScoreEar = 0;
    
    // Ear Training target variables
    private int targetPitchIndex = -1;
    private boolean targetPitchPlayed = false;

    private Random random = new Random();
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = getSharedPreferences("SoundGamePrefs", MODE_PRIVATE);
        highScoreMemory = sharedPreferences.getInt("high_score_memory", 0);
        highScoreEar = sharedPreferences.getInt("high_score_ear", 0);

        initializeUI();
        updateScoresUI();
        switchMode(MODE_MEMORY);
    }

    private void initializeUI() {
        pads[0] = findViewById(R.id.pad_red);
        pads[1] = findViewById(R.id.pad_yellow);
        pads[2] = findViewById(R.id.pad_blue);
        pads[3] = findViewById(R.id.pad_green);

        tvScore = (TextView) findViewById(R.id.tv_score);
        tvHighScore = (TextView) findViewById(R.id.tv_high_score);
        tvStatus = (TextView) findViewById(R.id.tv_status);
        tvInstructionTitle = (TextView) findViewById(R.id.tv_instruction_title);
        tvInstructionBody = (TextView) findViewById(R.id.tv_instruction_body);

        btnModeMemory = (Button) findViewById(R.id.btn_mode_memory);
        btnModeEar = (Button) findViewById(R.id.btn_mode_ear);
        btnModeFree = (Button) findViewById(R.id.btn_mode_free);
        btnStartGame = (Button) findViewById(R.id.btn_start_game);
        btnReplay = (Button) findViewById(R.id.btn_replay);

        // Set up the shapes dynamically so they look neat with rounded corners
        for (int i = 0; i < 4; i++) {
            pads[i].setBackground(createRoundedDrawable(DIM_COLORS[i], 16));
            final int index = i;
            pads[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onPadClicked(index);
                }
            });
        }

        // Mode switch triggers
        btnModeMemory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchMode(MODE_MEMORY);
            }
        });

        btnModeEar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchMode(MODE_EAR_TRAIN);
            }
        });

        btnModeFree.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchMode(MODE_FREE_PLAY);
            }
        });

        // Game actions triggers
        btnStartGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startGame();
            }
        });

        btnReplay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                replayCurrentCue();
            }
        });
    }

    private void switchMode(int mode) {
        if (isPlayingSequence) return;
        currentMode = mode;
        currentScore = 0;
        updateScoresUI();

        // Style selected buttons
        btnModeMemory.setBackgroundColor(currentMode == MODE_MEMORY ? 0xFF2E7D32 : 0xFF37474F);
        btnModeMemory.setTextColor(currentMode == MODE_MEMORY ? 0xFFFFFFFF : 0xFFBBBBBB);
        
        btnModeEar.setBackgroundColor(currentMode == MODE_EAR_TRAIN ? 0xFF2E7D32 : 0xFF37474F);
        btnModeEar.setTextColor(currentMode == MODE_EAR_TRAIN ? 0xFFFFFFFF : 0xFFBBBBBB);
        
        btnModeFree.setBackgroundColor(currentMode == MODE_FREE_PLAY ? 0xFF2E7D32 : 0xFF37474F);
        btnModeFree.setTextColor(currentMode == MODE_FREE_PLAY ? 0xFFFFFFFF : 0xFFBBBBBB);

        if (currentMode == MODE_MEMORY) {
            tvStatus.setText("Memory Mode: Replicate the growing patterns!");
            tvInstructionTitle.setText("How to Play Memory Mode:");
            tvInstructionBody.setText("1. Listen to the sequence of synthesized sound notes.\n2. Tap the color pads in the exact same order.\n3. The app adds one note to the sound chain each turn.\n4. Score increases after each correct sequence completed.");
            btnStartGame.setVisibility(View.VISIBLE);
            btnReplay.setVisibility(View.VISIBLE);
            btnReplay.setEnabled(false);
            gameSequence.clear();
        } else if (currentMode == MODE_EAR_TRAIN) {
            tvStatus.setText("Pitch Guess Mode: Train your relative hearing!");
            tvInstructionTitle.setText("How to Play Pitch Guess Mode:");
            tvInstructionBody.setText("1. Tap 'START' to play a random musical note.\n2. Tap the color pad you think matches that frequency.\n3. Guess correctly to build your score streak.\n4. Red is Low C, Yellow is E, Blue is G, Green is High C.");
            btnStartGame.setVisibility(View.VISIBLE);
            btnReplay.setVisibility(View.VISIBLE);
            btnReplay.setEnabled(false);
            targetPitchIndex = -1;
            targetPitchPlayed = false;
        } else {
            tvStatus.setText("Free Play Mode: Play the pads freely like an instrument!");
            tvInstructionTitle.setText("How to Play Free Play Mode:");
            tvInstructionBody.setText("1. No sequences or scoring rules!\n2. Tap any color pad to synthesized pure C-major triad frequencies.\n3. Tap multiple buttons to explore sound pitches.\n4. Perfect to test your synth setup.");
            btnStartGame.setVisibility(View.GONE);
            btnReplay.setVisibility(View.GONE);
        }
    }

    private void startGame() {
        if (isPlayingSequence) return;

        currentScore = 0;
        updateScoresUI();

        if (currentMode == MODE_MEMORY) {
            gameSequence.clear();
            userStepIndex = 0;
            btnReplay.setEnabled(true);
            addSequenceStep();
            playSequence();
        } else if (currentMode == MODE_EAR_TRAIN) {
            generateNewTargetPitch();
        }
    }

    private void generateNewTargetPitch() {
        targetPitchIndex = random.nextInt(4);
        targetPitchPlayed = true;
        btnReplay.setEnabled(true);
        tvStatus.setText("Listening to the pitch query...");
        
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                playTone(FREQUENCIES[targetPitchIndex], 600);
                flashPad(targetPitchIndex, 500);
                mainHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        tvStatus.setText("Which color pad did you hear? Tap to guess!");
                    }
                }, 600);
            }
        }, 300);
    }

    private void replayCurrentCue() {
        if (isPlayingSequence) return;

        if (currentMode == MODE_MEMORY && !gameSequence.isEmpty()) {
            playSequence();
        } else if (currentMode == MODE_EAR_TRAIN && targetPitchIndex != -1) {
            tvStatus.setText("Replaying pitch query...");
            playTone(FREQUENCIES[targetPitchIndex], 600);
            flashPad(targetPitchIndex, 500);
            mainHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    tvStatus.setText("Which color pad did you hear?");
                }
            }, 650);
        }
    }

    private void addSequenceStep() {
        gameSequence.add(random.nextInt(4));
    }

    private void playSequence() {
        if (gameSequence.isEmpty()) return;
        
        isPlayingSequence = true;
        tvStatus.setText("Listen carefully to the sound sequence...");
        setClickablePads(false);

        for (int i = 0; i < gameSequence.size(); i++) {
            final int index = i;
            final int padToPlay = gameSequence.get(i);
            
            mainHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    playTone(FREQUENCIES[padToPlay], 450);
                    flashPad(padToPlay, 400);

                    // If this is the final step in the sequence playback
                    if (index == gameSequence.size() - 1) {
                        mainHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                isPlayingSequence = false;
                                tvStatus.setText("Your Turn! Repeat the sequence.");
                                setClickablePads(true);
                            }
                        }, 550);
                    }
                }
            }, i * 650);
        }
    }

    private void onPadClicked(int index) {
        if (isPlayingSequence) return;

        if (currentMode == MODE_FREE_PLAY) {
            playTone(FREQUENCIES[index], 350);
            flashPad(index, 300);
        } else if (currentMode == MODE_MEMORY) {
            if (gameSequence.isEmpty()) return;
            
            playTone(FREQUENCIES[index], 350);
            flashPad(index, 300);

            // Compare user click with index position in sequence
            if (index == gameSequence.get(userStepIndex)) {
                userStepIndex++;
                if (userStepIndex >= gameSequence.size()) {
                    // Completed whole pattern sequence correctly
                    currentScore++;
                    updateScoresUI();
                    tvStatus.setText("Perfect! Keep it up!");
                    userStepIndex = 0;
                    
                    if (currentScore > highScoreMemory) {
                        highScoreMemory = currentScore;
                        saveHighScore("high_score_memory", highScoreMemory);
                    }

                    setClickablePads(false);
                    // Add next challenge step and auto replay
                    mainHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            addSequenceStep();
                            playSequence();
                        }
                    }, 1200);
                }
            } else {
                // Game over
                tvStatus.setText("Wrong note! GAME OVER.");
                playGameOverSnd();
                gameSequence.clear();
                btnReplay.setEnabled(false);
            }
        } else if (currentMode == MODE_EAR_TRAIN) {
            if (targetPitchIndex == -1 || !targetPitchPlayed) {
                tvStatus.setText("Press START GAME to generate a pitch cue!");
                return;
            }

            playTone(FREQUENCIES[index], 350);
            flashPad(index, 300);

            if (index == targetPitchIndex) {
                // Correct match!
                currentScore++;
                updateScoresUI();
                tvStatus.setText("Bullseye! That was correct!");
                playCorrectSnd();
                
                if (currentScore > highScoreEar) {
                    highScoreEar = currentScore;
                    saveHighScore("high_score_ear", highScoreEar);
                }

                targetPitchPlayed = false;
                // Move on to next dynamic tone
                mainHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        generateNewTargetPitch();
                    }
                }, 1400);
            } else {
                // Wrong pitch
                tvStatus.setText("Incorrect! That was NOT the note pitch.");
                playGameOverSnd();
                targetPitchIndex = -1;
                targetPitchPlayed = false;
                btnReplay.setEnabled(false);
            }
        }
    }

    private void flashPad(final int padIndex, int durationMs) {
        pads[padIndex].setBackground(createRoundedDrawable(BRIGHT_COLORS[padIndex], 16));
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                pads[padIndex].setBackground(createRoundedDrawable(DIM_COLORS[padIndex], 16));
            }
        }, durationMs);
    }

    private void setClickablePads(boolean clickable) {
        for (View p : pads) {
            p.setEnabled(clickable);
        }
    }

    private void updateScoresUI() {
        tvScore.setText(String.valueOf(currentScore));
        if (currentMode == MODE_MEMORY) {
            tvHighScore.setText(String.valueOf(highScoreMemory));
        } else if (currentMode == MODE_EAR_TRAIN) {
            tvHighScore.setText(String.valueOf(highScoreEar));
        } else {
            tvHighScore.setText("-");
        }
    }

    private void saveHighScore(String key, int val) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(key, val);
        editor.apply();
        updateScoresUI();
    }

    private GradientDrawable createRoundedDrawable(int color, float radius) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(color);
        gd.setCornerRadius(radius);
        gd.setStroke(3, 0xFF444444);
        return gd;
    }

    // Audio synthesizer method using native AudioTrack PCM 16-bit generator
    private synchronized void playTone(final double frequency, final int durationMs) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                int sampleRate = 8000;
                int numSamples = durationMs * sampleRate / 1000;
                double[] sample = new double[numSamples];
                byte[] generatedSnd = new byte[2 * numSamples];

                // Generate a pure sine wave with fade-out envelope to prevent clicky endings
                for (int i = 0; i < numSamples; ++i) {
                    double amplitudeEnvelope = 1.0;
                    // Last 15% of the sound fades out
                    if (i > (numSamples * 0.85)) {
                        amplitudeEnvelope = (numSamples - i) / (double)(numSamples * 0.15);
                    }
                    sample[i] = Math.sin(2 * Math.PI * i / (sampleRate / frequency)) * amplitudeEnvelope;
                }

                int idx = 0;
                for (final double dVal : sample) {
                    final short val = (short) ((dVal * 32767));
                    generatedSnd[idx++] = (byte) (val & 0x00ff);
                    generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
                }

                AudioTrack audioTrack = null;
                try {
                    audioTrack = new AudioTrack(
                            AudioManager.STREAM_MUSIC,
                            sampleRate,
                            AudioFormat.CHANNEL_OUT_MONO,
                            AudioFormat.ENCODING_PCM_16BIT,
                            generatedSnd.length,
                            AudioTrack.MODE_STATIC);
                    
                    audioTrack.write(generatedSnd, 0, generatedSnd.length);
                    audioTrack.play();
                    
                    Thread.sleep(durationMs + 30);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (audioTrack != null) {
                        try {
                            audioTrack.stop();
                            audioTrack.release();
                        } catch (Exception e) {
                            // Already stopped
                        }
                    }
                }
            }
        }).start();
    }

    private void playGameOverSnd() {
        // Double note drop chord
        playTone(180.0, 350);
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                playTone(135.0, 450);
            }
        }, 250);
    }

    private void playCorrectSnd() {
        // High octave chime
        playTone(523.25, 120);
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                playTone(659.25, 200);
            }
        }, 120);
    }
}