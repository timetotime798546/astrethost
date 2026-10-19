package com.soundcraftaudiofx.app;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

public class MainActivity extends Activity {

    private TextView tvStatus;
    private TextView tvPitchLabel;
    private TextView tvFreqLabel;
    private SeekBar sbPitch;
    private SeekBar sbFrequency;
    private Button btnResetPitch;
    private Button btnPlayCustomTone;

    private float pitchMultiplier = 1.0f;
    private int currentFrequency = 440;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainHandler = new Handler(Looper.getMainLooper());

        tvStatus = (TextView) findViewById(R.id.tvStatus);
        tvPitchLabel = (TextView) findViewById(R.id.tvPitchLabel);
        tvFreqLabel = (TextView) findViewById(R.id.tvFreqLabel);
        sbPitch = (SeekBar) findViewById(R.id.sbPitch);
        sbFrequency = (SeekBar) findViewById(R.id.sbFrequency);
        btnResetPitch = (Button) findViewById(R.id.btnResetPitch);
        btnPlayCustomTone = (Button) findViewById(R.id.btnPlayCustomTone);

        setupControls();
        setupFxPads();
        setupPianoKeys();
    }

    private void updateStatus(final String message) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (tvStatus != null) {
                    tvStatus.setText(message);
                }
            }
        });
    }

    private void setupControls() {
        sbPitch.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Range from 0.5x (progress 0) to 2.0x (progress 200)
                pitchMultiplier = 0.5f + (progress / 200.0f) * 1.5f;
                // Offline calculation of pitch multiplier
                String formatted = String.format("%.2f", pitchMultiplier);
                tvPitchLabel.setText("Pitch Multiplier: " + formatted + "x");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnResetPitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sbPitch.setProgress(100);
                pitchMultiplier = 1.0f;
                tvPitchLabel.setText("Pitch Multiplier: 1.00x");
                updateStatus("Pitch reset to standard 1.00x");
            }
        });

        sbFrequency.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // 130 Hz to 1730 Hz
                currentFrequency = 130 + progress;
                tvFreqLabel.setText("Synth Frequency: " + currentFrequency + " Hz");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnPlayCustomTone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Offline deterministic calculation of modulated frequency
                final double finalFreq = currentFrequency * pitchMultiplier;
                updateStatus("Playing Pure Tone: " + Math.round(finalFreq) + " Hz");
                AudioSynth.playTone(finalFreq, 450, 0.8f, new AudioSynth.SoundCallback() {
                    @Override
                    public void onSoundCompleted() {
                        updateStatus("Tone completed (" + Math.round(finalFreq) + " Hz)");
                    }
                });
            }
        });
    }

    private void setupFxPads() {
        Button btnFxLaser = (Button) findViewById(R.id.btnFxLaser);
        Button btnFxCoin = (Button) findViewById(R.id.btnFxCoin);
        Button btnFxJump = (Button) findViewById(R.id.btnFxJump);
        Button btnFxSiren = (Button) findViewById(R.id.btnFxSiren);
        Button btnFxKick = (Button) findViewById(R.id.btnFxKick);
        Button btnFxSnare = (Button) findViewById(R.id.btnFxSnare);

        btnFxLaser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateStatus("Triggered: Laser Sweep FX");
                AudioSynth.playLaserFx(pitchMultiplier);
            }
        });

        btnFxCoin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateStatus("Triggered: 8-Bit Coin Chime");
                AudioSynth.playCoinChime(pitchMultiplier);
            }
        });

        btnFxJump.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateStatus("Triggered: Arcade Jump Blip");
                AudioSynth.play8BitJump(pitchMultiplier);
            }
        });

        btnFxSiren.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateStatus("Triggered: Siren Alert Sweep");
                AudioSynth.playSirenSweep(pitchMultiplier);
            }
        });

        btnFxKick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateStatus("Triggered: Deep Bass Drum Kick");
                AudioSynth.playDrumKick();
            }
        });

        btnFxSnare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateStatus("Triggered: White-Noise Snare Crack");
                AudioSynth.playSnareNoise();
            }
        });
    }

    private void setupPianoKeys() {
        // Standard 4th octave pitch frequencies (Hz)
        final double noteC = 261.63;
        final double noteD = 293.66;
        final double noteE = 329.63;
        final double noteF = 349.23;
        final double noteG = 392.00;
        final double noteA = 440.00;
        final double noteB = 493.88;

        findViewById(R.id.btnNoteC).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPianoNote("C4", noteC);
            }
        });

        findViewById(R.id.btnNoteD).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPianoNote("D4", noteD);
            }
        });

        findViewById(R.id.btnNoteE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPianoNote("E4", noteE);
            }
        });

        findViewById(R.id.btnNoteF).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPianoNote("F4", noteF);
            }
        });

        findViewById(R.id.btnNoteG).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPianoNote("G4", noteG);
            }
        });

        findViewById(R.id.btnNoteA).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPianoNote("A4", noteA);
            }
        });

        findViewById(R.id.btnNoteB).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPianoNote("B4", noteB);
            }
        });
    }

    private void playPianoNote(final String noteName, final double baseFreq) {
        // Calculate modulated frequency locally
        final double modulatedFreq = baseFreq * pitchMultiplier;
        updateStatus("Playing Note: " + noteName + " (" + Math.round(modulatedFreq) + " Hz)");
        AudioSynth.playTone(modulatedFreq, 350, 0.85f, null);
    }
}