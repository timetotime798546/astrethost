package com.simpletapcounter.app;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.Random;

public class MainActivity extends Activity {

    private int counterValue = 0;
    private int highScoreValue = 0;
    private int activeStep = 1;
    private int currentThemeIndex = 0; // 0: Neon Indigo, 1: Cyber Sunset, 2: Mint Aurora, 3: Electric Crimson
    private boolean soundEnabled = true;

    // Transition Cache colors to enable smooth ValueAnimators
    private int lastBgColor = 0;
    private int lastCardBgColor = 0;

    // View bindings
    private LinearLayout mainRootLayout;
    private LinearLayout statsCard;
    private FrameLayout tapContainerFrame;
    private RelativeLayout tapCircleButton;
    private ScrollView historyScrollView;
    private LinearLayout historyContainer;

    private TextView appTitleText;
    private TextView appSubtitleText;
    private TextView labelTally;
    private TextView counterDisplay;
    private TextView stepSizeDisplay;
    private TextView tapActionLabel;
    private TextView tapActionSublabel;
    private TextView highScoreBadge;

    private Button btnStep1;
    private Button btnStep5;
    private Button btnStep10;
    private Button btnStepMinus;

    private Button btnReset;
    private Button btnTheme;
    private Button btnHaptic;

    private static final String PREFS_NAME = "SimpleTapCounterPrefs";
    private static final String KEY_COUNT = "counter_val";
    private static final String KEY_HIGH_SCORE = "high_score_val";
    private static final String KEY_STEP = "active_step_val";
    private static final String KEY_THEME = "current_theme_index";
    private static final String KEY_SOUND = "sound_enabled_val";

    // Color definitions for UI elements
    private int activeThemeColor;
    private int inactiveCardColor;
    private Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind all UI components
        mainRootLayout = (LinearLayout) findViewById(R.id.main_root_layout);
        statsCard = (LinearLayout) findViewById(R.id.stats_card);
        tapContainerFrame = (FrameLayout) findViewById(R.id.tap_container_frame);
        tapCircleButton = (RelativeLayout) findViewById(R.id.tap_circle_button);
        historyScrollView = (ScrollView) findViewById(R.id.history_scroll_view);
        historyContainer = (LinearLayout) findViewById(R.id.history_container);

        appTitleText = (TextView) findViewById(R.id.app_title_text);
        appSubtitleText = (TextView) findViewById(R.id.app_subtitle_text);
        labelTally = (TextView) findViewById(R.id.label_tally);
        counterDisplay = (TextView) findViewById(R.id.counter_display);
        stepSizeDisplay = (TextView) findViewById(R.id.step_size_display);
        tapActionLabel = (TextView) findViewById(R.id.tap_action_label);
        tapActionSublabel = (TextView) findViewById(R.id.tap_action_sublabel);
        highScoreBadge = (TextView) findViewById(R.id.high_score_badge);

        btnStep1 = (Button) findViewById(R.id.btn_step_1);
        btnStep5 = (Button) findViewById(R.id.btn_step_5);
        btnStep10 = (Button) findViewById(R.id.btn_step_10);
        btnStepMinus = (Button) findViewById(R.id.btn_step_minus);

        btnReset = (Button) findViewById(R.id.btn_action_reset);
        btnTheme = (Button) findViewById(R.id.btn_action_theme);
        btnHaptic = (Button) findViewById(R.id.btn_action_haptic);

        // Load persisted states
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        counterValue = prefs.getInt(KEY_COUNT, 0);
        highScoreValue = prefs.getInt(KEY_HIGH_SCORE, 0);
        activeStep = prefs.getInt(KEY_STEP, 1);
        currentThemeIndex = prefs.getInt(KEY_THEME, 0);
        soundEnabled = prefs.getBoolean(KEY_SOUND, true);

        // Render Loaded State Parameters
        updateDisplayMetrics();
        applyTheme();

        // Bind Touch Events
        tapCircleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performTap();
            }
        });

        btnStep1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateButtonPress(v);
                changeStepSize(1);
            }
        });

        btnStep5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateButtonPress(v);
                changeStepSize(5);
            }
        });

        btnStep10.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateButtonPress(v);
                changeStepSize(10);
            }
        });

        btnStepMinus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateButtonPress(v);
                changeStepSize(-1);
            }
        });

        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateButtonPress(v);
                animateShake(statsCard);
                resetCounter();
            }
        });

        btnTheme.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateButtonPress(v);
                cycleTheme();
            }
        });

        btnHaptic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateButtonPress(v);
                toggleSound();
            }
        });

        // Initialize history with loaded notification
        logEvent("Counter Matrix Online. Standard Tally Ready.");

        // Start ambient animation sequences
        startSublabelPulse();
    }

    private void performTap() {
        triggerTapScaleAnimation();
        triggerCounterPopAnimation();
        spawnFloatingIndicator();
        spawnParticleBurst();

        counterValue += activeStep;
        
        boolean recordBeaten = false;
        if (counterValue > highScoreValue) {
            highScoreValue = counterValue;
            recordBeaten = true;
            saveHighScoreState();
            triggerHighScorePulseAnimation();
        }

        updateDisplayMetrics();
        persistCounterState();

        if (recordBeaten) {
            playHighScoreSound();
        } else {
            playClickSoundFeedback();
        }

        String changeSign = activeStep >= 0 ? "+" : "";
        logEvent("Added " + changeSign + activeStep + " (New: " + counterValue + ")" + (recordBeaten ? " ★ NEW HIGH!" : ""));
    }

    private void changeStepSize(int val) {
        activeStep = val;
        playSelectionSound();
        persistStepState();
        updateDisplayMetrics();
        applyTheme();
        logEvent("Step interval modified to " + (val >= 0 ? "+" : "") + val);
    }

    private void resetCounter() {
        if (soundEnabled) {
            playSweepSound(520.0f, 130.0f, 0.22f);
        } else {
            playClickSoundFeedback();
        }
        int oldValue = counterValue;
        counterValue = 0;
        persistCounterState();
        updateDisplayMetrics();
        logEvent("Counter flushed to 0 (Prior tally: " + oldValue + ")");
    }

    private void cycleTheme() {
        playThemeSound();
        currentThemeIndex = (currentThemeIndex + 1) % 4;
        persistThemeIndex();
        applyTheme();
        
        String themeName = "";
        switch (currentThemeIndex) {
            case 0: themeName = "Neon Indigo"; break;
            case 1: themeName = "Cyber Sunset"; break;
            case 2: themeName = "Mint Aurora"; break;
            case 3: themeName = "Electric Crimson"; break;
        }
        logEvent("Interface Theme Matrix Switched: " + themeName);
    }

    private void toggleSound() {
        soundEnabled = !soundEnabled;
        persistSoundState();
        playClickSoundFeedback();
        
        btnHaptic.setText("SOUND: " + (soundEnabled ? "ON" : "OFF"));
        applyTheme();
        logEvent("Feedback audio signal configured to: " + (soundEnabled ? "ACTIVE" : "MUTED"));
    }

    private void updateDisplayMetrics() {
        counterDisplay.setText(String.valueOf(counterValue));
        highScoreBadge.setText("BEST: " + highScoreValue);
        
        String stepSymbol = activeStep >= 0 ? "+" : "";
        stepSizeDisplay.setText("Incrementing by " + stepSymbol + activeStep);
        btnHaptic.setText("SOUND: " + (soundEnabled ? "ON" : "OFF"));
    }

    private void applyTheme() {
        final int rootBg, cardBg, textPrimary, textSecondary, activeBtnBg, inactiveBtnBg, circleInner, circleStroke, accentText, badgeBorder;

        switch (currentThemeIndex) {
            case 0: // Neon Indigo
                rootBg = 0xFF0B111E;
                cardBg = 0xFF141F32;
                textPrimary = 0xFFF1F5F9;
                textSecondary = 0xFF94A3B8;
                accentText = 0xFF38BDF8;
                activeBtnBg = 0xFF2563EB;
                inactiveBtnBg = 0xFF1E293B;
                circleInner = 0xFF1E40AF;
                circleStroke = 0xFF60A5FA;
                badgeBorder = 0xFF3B82F6;
                break;

            case 1: // Cyber Sunset
                rootBg = 0xFF17091A;
                cardBg = 0xFF27132B;
                textPrimary = 0xFFFFF1F2;
                textSecondary = 0xFFE2E8F0;
                accentText = 0xFFF43F5E;
                activeBtnBg = 0xFFBE185D;
                inactiveBtnBg = 0xFF24152F;
                circleInner = 0xFF9D174D;
                circleStroke = 0xFFF472B6;
                badgeBorder = 0xFFEC4899;
                break;

            case 2: // Mint Aurora
                rootBg = 0xFF021E17;
                cardBg = 0xFF0A362B;
                textPrimary = 0xFFF0FDF4;
                textSecondary = 0xFF94A3B8;
                accentText = 0xFF10B981;
                activeBtnBg = 0xFF059669;
                inactiveBtnBg = 0xFF0F2D24;
                circleInner = 0xFF065F46;
                circleStroke = 0xFF34D399;
                badgeBorder = 0xFF10B981;
                break;

            case 3: // Electric Crimson
                rootBg = 0xFF09090B;
                cardBg = 0xFF18181B;
                textPrimary = 0xFFFAFAFA;
                textSecondary = 0xFFA1A1AA;
                accentText = 0xFFEF4444;
                activeBtnBg = 0xFFDC2626;
                inactiveBtnBg = 0xFF27272A;
                circleInner = 0xFF991B1B;
                circleStroke = 0xFFF87171;
                badgeBorder = 0xFFEF4444;
                break;

            default:
                return;
        }

        activeThemeColor = activeBtnBg;
        inactiveCardColor = cardBg;

        // Perform smooth ValueAnimator for the layout background color
        int initialBgColor = (lastBgColor != 0) ? lastBgColor : rootBg;
        lastBgColor = rootBg;
        ValueAnimator bgAnim = ValueAnimator.ofObject(new ArgbEvaluator(), initialBgColor, rootBg);
        bgAnim.setDuration(400);
        bgAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mainRootLayout.setBackgroundColor((Integer) animation.getAnimatedValue());
            }
        });
        bgAnim.start();

        // Smooth transition for card background color
        int initialCardBg = (lastCardBgColor != 0) ? lastCardBgColor : cardBg;
        lastCardBgColor = cardBg;
        ValueAnimator cardAnim = ValueAnimator.ofObject(new ArgbEvaluator(), initialCardBg, cardBg);
        cardAnim.setDuration(400);
        cardAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int animatedVal = (Integer) animation.getAnimatedValue();
                GradientDrawable cardShape = new GradientDrawable();
                cardShape.setColor(animatedVal);
                cardShape.setCornerRadius(dpToPx(20));
                cardShape.setStroke(dpToPx(1), 0x22FFFFFF);
                statsCard.setBackground(cardShape);
            }
        });
        cardAnim.start();

        // Highly interactive circular control dial layout
        GradientDrawable circleShape = new GradientDrawable();
        circleShape.setColors(new int[]{ circleInner, rootBg });
        circleShape.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        circleShape.setGradientRadius(dpToPx(130));
        circleShape.setShape(GradientDrawable.OVAL);
        circleShape.setStroke(dpToPx(5), circleStroke);
        tapCircleButton.setBackground(circleShape);

        // Refine central action typography colors
        appTitleText.setTextColor(textPrimary);
        appSubtitleText.setTextColor(textSecondary);
        labelTally.setTextColor(textSecondary);
        counterDisplay.setTextColor(textPrimary);
        stepSizeDisplay.setTextColor(accentText);
        tapActionLabel.setTextColor(textPrimary);
        tapActionSublabel.setTextColor(accentText);

        // Style the active mode selectors
        setButtonStyle(btnStep1, activeStep == 1 ? activeBtnBg : inactiveBtnBg, activeStep == 1);
        setButtonStyle(btnStep5, activeStep == 5 ? activeBtnBg : inactiveBtnBg, activeStep == 5);
        setButtonStyle(btnStep10, activeStep == 10 ? activeBtnBg : inactiveBtnBg, activeStep == 10);
        setButtonStyle(btnStepMinus, activeStep == -1 ? activeBtnBg : inactiveBtnBg, activeStep == -1);

        // Style global layout action controls
        setButtonStyle(btnReset, inactiveBtnBg, false);
        setButtonStyle(btnTheme, activeBtnBg, true);
        setButtonStyle(btnHaptic, soundEnabled ? activeBtnBg : inactiveBtnBg, soundEnabled);

        // Style the elegant high score floating status widget
        GradientDrawable badgeShape = new GradientDrawable();
        badgeShape.setColor(cardBg);
        badgeShape.setCornerRadius(dpToPx(10));
        badgeShape.setStroke(dpToPx(2), badgeBorder);
        highScoreBadge.setBackground(badgeShape);
    }

    private void setButtonStyle(Button btn, int color, boolean isActive) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dpToPx(12));
        if (isActive) {
            shape.setStroke(dpToPx(2), Color.WHITE);
        } else {
            shape.setStroke(dpToPx(1), 0x33FFFFFF);
        }
        btn.setBackground(shape);
        btn.setTextColor(0xFFFFFFFF);
    }

    // =========================================================================
    // HIGH POLISH NATIVE UI ANIMATIONS
    // =========================================================================

    private void animateButtonPress(final View view) {
        view.animate()
            .scaleX(0.9f)
            .scaleY(0.9f)
            .setDuration(80)
            .withEndAction(new Runnable() {
                @Override
                public void run() {
                    view.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(120)
                        .start();
                }
            })
            .start();
    }

    private void animateShake(final View view) {
        view.animate()
            .translationX(dpToPx(12))
            .setDuration(50)
            .withEndAction(new Runnable() {
                @Override
                public void run() {
                    view.animate()
                        .translationX(-dpToPx(12))
                        .setDuration(50)
                        .withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                view.animate()
                                    .translationX(dpToPx(6))
                                    .setDuration(50)
                                    .withEndAction(new Runnable() {
                                        @Override
                                        public void run() {
                                            view.animate()
                                                .translationX(0)
                                                .setDuration(50)
                                                .start();
                                        }
                                    })
                                    .start();
                            }
                        })
                        .start();
                }
            })
            .start();
    }

    private void startSublabelPulse() {
        tapActionSublabel.animate().cancel();
        tapActionSublabel.setAlpha(0.4f);
        tapActionSublabel.animate()
            .alpha(1.0f)
            .setDuration(1000)
            .withEndAction(new Runnable() {
                @Override
                public void run() {
                    tapActionSublabel.animate()
                        .alpha(0.4f)
                        .setDuration(1000)
                        .withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                startSublabelPulse();
                            }
                        })
                        .start();
                }
            })
            .start();
    }

    private void triggerTapScaleAnimation() {
        tapCircleButton.animate()
            .scaleX(0.88f)
            .scaleY(0.88f)
            .setDuration(60)
            .withEndAction(new Runnable() {
                @Override
                public void run() {
                    tapCircleButton.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(120)
                        .start();
                }
            })
            .start();
    }

    private void triggerCounterPopAnimation() {
        counterDisplay.animate()
            .scaleX(1.18f)
            .scaleY(1.18f)
            .setDuration(70)
            .withEndAction(new Runnable() {
                @Override
                public void run() {
                    counterDisplay.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(130)
                        .start();
                }
            })
            .start();
    }

    private void triggerHighScorePulseAnimation() {
        highScoreBadge.animate()
            .scaleX(1.35f)
            .scaleY(1.35f)
            .setDuration(160)
            .withEndAction(new Runnable() {
                @Override
                public void run() {
                    highScoreBadge.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(140)
                        .start();
                }
            })
            .start();
    }

    private void spawnFloatingIndicator() {
        // Dynamically instantiate flying step bubble indicator
        final TextView floatText = new TextView(this);
        String textSign = activeStep >= 0 ? "+" : "";
        floatText.setText(textSign + activeStep);
        floatText.setTextColor(activeThemeColor);
        floatText.setTextSize(26);
        floatText.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        floatText.setGravity(Gravity.CENTER);

        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        );
        layoutParams.gravity = Gravity.CENTER;
        floatText.setLayoutParams(layoutParams);

        tapContainerFrame.addView(floatText);

        // Render float with random horizontal drift values
        float randomXDrift = (random.nextFloat() * dpToPx(100)) - dpToPx(50);
        floatText.setTranslationX(randomXDrift);
        floatText.setTranslationY(0f);
        floatText.setAlpha(1.0f);

        // Perform 650ms dynamic upward floating motion + fading animation
        floatText.animate()
            .translationY(-dpToPx(130))
            .alpha(0f)
            .scaleX(1.3f)
            .scaleY(1.3f)
            .setDuration(650)
            .withEndAction(new Runnable() {
                @Override
                public void run() {
                    tapContainerFrame.removeView(floatText);
                }
            })
            .start();
    }

    private void spawnParticleBurst() {
        for (int i = 0; i < 6; i++) {
            final View particle = new View(this);
            int size = dpToPx(random.nextInt(6) + 4);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(size, size);
            params.gravity = Gravity.CENTER;
            particle.setLayoutParams(params);

            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.OVAL);
            shape.setColor(activeThemeColor);
            particle.setBackground(shape);

            tapContainerFrame.addView(particle);

            double angle = random.nextDouble() * 2 * Math.PI;
            float distance = dpToPx(random.nextInt(80) + 60);
            float destX = (float) (Math.cos(angle) * distance);
            float destY = (float) (Math.sin(angle) * distance);

            particle.setAlpha(1.0f);
            particle.setScaleX(1.0f);
            particle.setScaleY(1.0f);

            particle.animate()
                .translationX(destX)
                .translationY(destY)
                .alpha(0f)
                .scaleX(0.2f)
                .scaleY(0.2f)
                .setDuration(500 + random.nextInt(200))
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        tapContainerFrame.removeView(particle);
                    }
                })
                .start();
        }
    }

    // =========================================================================
    // HIGH FIDELITY SYNTHESIZED SOUND SYSTEM (Android AudioTrack API)
    // =========================================================================

    private void playClickSoundFeedback() {
        if (soundEnabled) {
            playSynthSound(680.0f, 0.07f, "sine");
        }
        mainRootLayout.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
    }

    private void playSelectionSound() {
        if (soundEnabled) {
            playSynthSound(880.0f, 0.05f, "sine");
        }
    }

    private void playThemeSound() {
        if (soundEnabled) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    playSingleTone(523.25f, 0.06f, "sine");
                    playSingleTone(659.25f, 0.08f, "sine");
                }
            }).start();
        }
    }

    private void playHighScoreSound() {
        if (soundEnabled) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    float[] notes = { 587.33f, 659.25f, 783.99f, 1046.50f }; // D5, E5, G5, C6 arpeggio
                    for (int i = 0; i < notes.length; i++) {
                        playSingleTone(notes[i], 0.10f, "sine");
                        try {
                            Thread.sleep(70);
                        } catch (InterruptedException ignored) {}
                    }
                }
            }).start();
        }
    }

    private void playSynthSound(final float frequency, final float durationS, final String waveType) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                playSingleTone(frequency, durationS, waveType);
            }
        }).start();
    }

    private void playSingleTone(float frequency, float durationS, String waveType) {
        int sampleRate = 22050;
        int numSamples = (int) (durationS * sampleRate);
        short[] sample = new short[numSamples];
        
        double freqOfTone = frequency;
        for (int i = 0; i < numSamples; ++i) {
            double t = (double) i / sampleRate;
            double envelope = 1.0;
            if (i > numSamples * 0.1) {
                envelope = 1.0 - ((double)(i - numSamples * 0.1) / (numSamples * 0.9));
            }
            if (envelope < 0) envelope = 0;

            double angle = 2.0 * Math.PI * freqOfTone * t;
            double val = 0;
            if ("sine".equals(waveType)) {
                val = Math.sin(angle);
            } else if ("square".equals(waveType)) {
                val = Math.signum(Math.sin(angle));
            } else if ("triangle".equals(waveType)) {
                val = (2.0 / Math.PI) * Math.asin(Math.sin(angle));
            }
            
            sample[i] = (short) (val * 32767 * envelope * 0.45);
        }
        
        AudioTrack audioTrack = null;
        try {
            int minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            );
            int bufferSize = Math.max(minBufferSize, numSamples * 2);
            audioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
                AudioTrack.MODE_STATIC
            );
            
            audioTrack.write(sample, 0, numSamples);
            audioTrack.play();
            Thread.sleep((long) (durationS * 1000) + 10);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (audioTrack != null) {
                try {
                    audioTrack.stop();
                    audioTrack.release();
                } catch (Exception ignored) {}
            }
        }
    }

    private void playSweepSound(final float startFreq, final float endFreq, final float durationS) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                int sampleRate = 22050;
                int numSamples = (int) (durationS * sampleRate);
                short[] sample = new short[numSamples];
                
                for (int i = 0; i < numSamples; ++i) {
                    double progress = (double) i / numSamples;
                    double t = (double) i / sampleRate;
                    double angle = 2.0 * Math.PI * (startFreq * t + 0.5 * (endFreq - startFreq) / durationS * t * t);
                    
                    double envelope = 1.0 - progress;
                    if (envelope < 0) envelope = 0;
                    
                    double val = Math.sin(angle);
                    sample[i] = (short) (val * 32767 * envelope * 0.4);
                }
                AudioTrack audioTrack = null;
                try {
                    int minBufferSize = AudioTrack.getMinBufferSize(
                        sampleRate,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT
                    );
                    int bufferSize = Math.max(minBufferSize, numSamples * 2);
                    audioTrack = new AudioTrack(
                        AudioManager.STREAM_MUSIC,
                        sampleRate,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize,
                        AudioTrack.MODE_STATIC
                    );
                    audioTrack.write(sample, 0, numSamples);
                    audioTrack.play();
                    Thread.sleep((long) (durationS * 1000) + 10);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (audioTrack != null) {
                        try {
                            audioTrack.stop();
                            audioTrack.release();
                        } catch (Exception ignored) {}
                    }
                }
            }
        }).start();
    }

    // =========================================================================

    private void logEvent(String msg) {
        // Build timeline log capsule item layout
        final LinearLayout itemLayout = new LinearLayout(this);
        itemLayout.setOrientation(LinearLayout.HORIZONTAL);
        itemLayout.setGravity(Gravity.CENTER_VERTICAL);
        itemLayout.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));

        GradientDrawable itemBg = new GradientDrawable();
        itemBg.setColor(inactiveCardColor);
        itemBg.setCornerRadius(dpToPx(10));
        itemBg.setStroke(dpToPx(1), 0x11FFFFFF);
        itemLayout.setBackground(itemBg);

        LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        itemParams.setMargins(0, 0, 0, dpToPx(6));
        itemLayout.setLayoutParams(itemParams);

        // Bullet point dot indicator
        View dot = new View(this);
        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dpToPx(8), dpToPx(8));
        dotParams.setMargins(0, 0, dpToPx(10), 0);
        dot.setLayoutParams(dotParams);
        
        GradientDrawable dotShape = new GradientDrawable();
        dotShape.setColor(activeThemeColor);
        dotShape.setShape(GradientDrawable.OVAL);
        dot.setBackground(dotShape);

        // Log description text
        TextView entry = new TextView(this);
        entry.setText(msg);
        entry.setTextColor(0xFFE2E8F0);
        entry.setTextSize(11.5f);
        entry.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        itemLayout.addView(dot);
        itemLayout.addView(entry);

        // Position log item inside the view
        historyContainer.addView(itemLayout, 0);

        // Perform Slide-In + Fade animation for newly appended capsule log item
        itemLayout.setAlpha(0f);
        itemLayout.setTranslationY(dpToPx(20));
        itemLayout.animate()
            .alpha(1.0f)
            .translationY(0f)
            .setDuration(280)
            .start();

        // Bound history limits smoothly
        if (historyContainer.getChildCount() > 40) {
            historyContainer.removeViews(40, historyContainer.getChildCount() - 40);
        }

        // Keep view focused at the beginning of the logger log dynamic stream
        historyScrollView.post(new Runnable() {
            @Override
            public void run() {
                historyScrollView.fullScroll(ScrollView.FOCUS_UP);
            }
        });
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    // SharedPreferences State Persistence Operations
    private void persistCounterState() {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putInt(KEY_COUNT, counterValue);
        editor.apply();
    }

    private void saveHighScoreState() {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putInt(KEY_HIGH_SCORE, highScoreValue);
        editor.apply();
    }

    private void persistStepState() {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putInt(KEY_STEP, activeStep);
        editor.apply();
    }

    private void persistThemeIndex() {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putInt(KEY_THEME, currentThemeIndex);
        editor.apply();
    }

    private void persistSoundState() {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putBoolean(KEY_SOUND, soundEnabled);
        editor.apply();
    }
}