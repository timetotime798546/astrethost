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
import android.os.Handler;
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

    // Multi-Channel Data Variables
    private int currentChannelIndex = 0; // 0: Alpha, 1: Beta, 2: Gamma
    private int[] counterValues = new int[]{0, 0, 0};
    private int[] highScoreValues = new int[]{0, 0, 0};
    private int[] activeSteps = new int[]{1, 1, 1};
    private int[] targetGoals = new int[]{0, 0, 0}; // 0 = None, 1: 50, 2: 100, 3: 250, 4: 500, 5: 1000
    private boolean[] goalCleared = new boolean[]{false, false, false};

    private int currentThemeIndex = 0; // 0: Neon Indigo, 1: Cyber Sunset, 2: Mint Aurora, 3: Electric Crimson
    private boolean soundEnabled = true;

    // Advanced Synthesizer lab parameters
    private String synthWaveform = "sine"; // "sine", "square", "triangle"
    private String synthPitch = "mid"; // "bass", "mid", "treble"
    private int autoPulseMode = 0; // 0: OFF, 1: 1Hz (1s), 2: 2Hz (0.5s), 3: 5Hz (0.2s)

    // Gradient Transition Cache colors to enable smooth multi-stop ValueAnimators
    private int lastRootBgStart = 0;
    private int lastRootBgEnd = 0;
    private int lastCardBgStart = 0;
    private int lastCardBgEnd = 0;

    // View bindings
    private LinearLayout mainRootLayout;
    private LinearLayout statsCard;
    private FrameLayout tapContainerFrame;
    private RelativeLayout tapCircleButton;
    private ScrollView historyScrollView;
    private LinearLayout historyContainer;
    private View goalCelebrationFlash;

    private TextView appTitleText;
    private TextView appSubtitleText;
    private TextView labelTally;
    private TextView counterDisplay;
    private TextView stepSizeDisplay;
    private TextView goalDisplayText;
    private TextView tapActionLabel;
    private TextView tapActionSublabel;
    private TextView highScoreBadge;

    // Channels
    private Button btnChan0;
    private Button btnChan1;
    private Button btnChan2;

    // Step configuration
    private Button btnStep1;
    private Button btnStep5;
    private Button btnStep10;
    private Button btnStepMinus;

    // Synth Lab
    private Button btnLabAutoPulse;
    private Button btnLabWaveform;
    private Button btnLabPitch;
    private Button btnLabGoal;

    // Actions
    private Button btnReset;
    private Button btnTheme;
    private Button btnHaptic;

    private static final String PREFS_NAME = "SimpleTapCounterPrefsExpanded";
    private static final String KEY_CURRENT_CHANNEL = "current_channel";
    private static final String KEY_COUNT_PREFIX = "counter_val_ch";
    private static final String KEY_HIGH_SCORE_PREFIX = "high_score_val_ch";
    private static final String KEY_STEP_PREFIX = "active_step_val_ch";
    private static final String KEY_GOAL_PREFIX = "target_goal_ch";
    private static final String KEY_GOAL_CLEARED_PREFIX = "goal_cleared_ch";
    
    private static final String KEY_THEME = "current_theme_index";
    private static final String KEY_SOUND = "sound_enabled_val";
    private static final String KEY_SYNTH_WAVEFORM = "synth_waveform";
    private static final String KEY_SYNTH_PITCH = "synth_pitch";
    private static final String KEY_AUTO_PULSE = "auto_pulse_mode";

    // Dynamic Color definitions for themed UI components
    private int activeThemeColor;
    private int cardBgStart;
    private int cardBgEnd;
    private Random random = new Random();

    // Bubble Pop Spawner Scheduler
    private Handler bubbleHandler;
    private Runnable bubbleSpawnRunnable;

    // Ambient Sparks Spawner Scheduler
    private Handler sparkHandler;
    private Runnable sparkRunnable;

    // Auto Clicker Timing Engine Scheduler
    private Handler autoPulseHandler;
    private Runnable autoPulseRunnable;

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
        goalCelebrationFlash = (View) findViewById(R.id.goal_celebration_flash);

        appTitleText = (TextView) findViewById(R.id.app_title_text);
        appSubtitleText = (TextView) findViewById(R.id.app_subtitle_text);
        labelTally = (TextView) findViewById(R.id.label_tally);
        counterDisplay = (TextView) findViewById(R.id.counter_display);
        stepSizeDisplay = (TextView) findViewById(R.id.step_size_display);
        goalDisplayText = (TextView) findViewById(R.id.goal_display_text);
        tapActionLabel = (TextView) findViewById(R.id.tap_action_label);
        tapActionSublabel = (TextView) findViewById(R.id.tap_action_sublabel);
        highScoreBadge = (TextView) findViewById(R.id.high_score_badge);

        btnChan0 = (Button) findViewById(R.id.btn_chan_0);
        btnChan1 = (Button) findViewById(R.id.btn_chan_1);
        btnChan2 = (Button) findViewById(R.id.btn_chan_2);

        btnStep1 = (Button) findViewById(R.id.btn_step_1);
        btnStep5 = (Button) findViewById(R.id.btn_step_5);
        btnStep10 = (Button) findViewById(R.id.btn_step_10);
        btnStepMinus = (Button) findViewById(R.id.btn_step_minus);

        btnLabAutoPulse = (Button) findViewById(R.id.btn_lab_autopulse);
        btnLabWaveform = (Button) findViewById(R.id.btn_lab_waveform);
        btnLabPitch = (Button) findViewById(R.id.btn_lab_pitch);
        btnLabGoal = (Button) findViewById(R.id.btn_lab_goal);

        btnReset = (Button) findViewById(R.id.btn_action_reset);
        btnTheme = (Button) findViewById(R.id.btn_action_theme);
        btnHaptic = (Button) findViewById(R.id.btn_action_haptic);

        // Load persisted states
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        currentChannelIndex = prefs.getInt(KEY_CURRENT_CHANNEL, 0);
        currentThemeIndex = prefs.getInt(KEY_THEME, 0);
        soundEnabled = prefs.getBoolean(KEY_SOUND, true);
        synthWaveform = prefs.getString(KEY_SYNTH_WAVEFORM, "sine");
        synthPitch = prefs.getString(KEY_SYNTH_PITCH, "mid");
        autoPulseMode = prefs.getInt(KEY_AUTO_PULSE, 0);

        for (int i = 0; i < 3; i++) {
            counterValues[i] = prefs.getInt(KEY_COUNT_PREFIX + i, 0);
            highScoreValues[i] = prefs.getInt(KEY_HIGH_SCORE_PREFIX + i, 0);
            activeSteps[i] = prefs.getInt(KEY_STEP_PREFIX + i, i == 0 ? 1 : (i == 1 ? 5 : 10));
            targetGoals[i] = prefs.getInt(KEY_GOAL_PREFIX + i, 0);
            goalCleared[i] = prefs.getBoolean(KEY_GOAL_CLEARED_PREFIX + i, false);
        }

        // Render Loaded State Parameters
        updateDisplayMetrics();
        applyTheme();

        // Bind Touch Events for Channel Switchers
        btnChan0.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchChannel(0);
            }
        });

        btnChan1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchChannel(1);
            }
        });

        btnChan2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchChannel(2);
            }
        });

        // Bind Central Tapping Button
        tapCircleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performTap(false);
            }
        });

        // Bind Step Buttons
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

        // Bind Lab Buttons
        btnLabAutoPulse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateButtonPress(v);
                cycleAutoPulseMode();
            }
        });

        btnLabWaveform.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateButtonPress(v);
                cycleWaveform();
            }
        });

        btnLabPitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateButtonPress(v);
                cyclePitch();
            }
        });

        btnLabGoal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateButtonPress(v);
                cycleGoalTarget();
            }
        });

        // Bind Footer Utility Buttons
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

        // Initialize timeline history
        logEvent("Channels initialized. Synth Engine Standby.");

        // Start ambient visual elements
        startSublabelPulse();

        // Initialize background Bubble Pop Spawner
        bubbleHandler = new Handler();
        bubbleSpawnRunnable = new Runnable() {
            @Override
            public void run() {
                spawnAmbientPopBubble();
                bubbleHandler.postDelayed(this, 2000 + random.nextInt(1500));
            }
        };

        // Initialize Ambient Spark Spawner
        sparkHandler = new Handler();
        sparkRunnable = new Runnable() {
            @Override
            public void run() {
                spawnAmbientEmberSpark();
                sparkHandler.postDelayed(this, 400 + random.nextInt(300));
            }
        };

        // Initialize Auto Pulse handler
        autoPulseHandler = new Handler();
        autoPulseRunnable = new Runnable() {
            @Override
            public void run() {
                triggerAutoPulseTick();
                long delay = 1000;
                if (autoPulseMode == 2) delay = 500;
                if (autoPulseMode == 3) delay = 200;
                autoPulseHandler.postDelayed(this, delay);
            }
        };

        startAutoPulseIfEnabled();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Spawners awake
        bubbleHandler.removeCallbacks(bubbleSpawnRunnable);
        bubbleHandler.postDelayed(bubbleSpawnRunnable, 1000);
        
        sparkHandler.removeCallbacks(sparkRunnable);
        sparkHandler.postDelayed(sparkRunnable, 500);

        startAutoPulseIfEnabled();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Conserve resources on pause
        bubbleHandler.removeCallbacks(bubbleSpawnRunnable);
        sparkHandler.removeCallbacks(sparkRunnable);
        autoPulseHandler.removeCallbacks(autoPulseRunnable);
    }

    private void switchChannel(int index) {
        if (currentChannelIndex == index) return;
        currentChannelIndex = index;
        
        // Persist channel swap
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putInt(KEY_CURRENT_CHANNEL, currentChannelIndex);
        editor.apply();

        playSelectionSound();
        updateDisplayMetrics();
        applyTheme();

        String channelLabel = getChannelName(index);
        logEvent("Matrix Route Switched to " + channelLabel);
    }

    private String getChannelName(int index) {
        switch (index) {
            case 0: return "CHANNEL ALPHA";
            case 1: return "CHANNEL BETA";
            case 2: return "CHANNEL GAMMA";
            default: return "CHANNEL MATRIX";
        }
    }

    private void performTap(boolean isAuto) {
        triggerTapScaleAnimation();
        triggerCounterPopAnimation();
        triggerRippleWave();
        spawnFloatingIndicator(0, 0); 
        spawnParticleBurst(0, 0);

        if (random.nextBoolean()) {
            spawnAmbientPopBubble();
        }

        int step = activeSteps[currentChannelIndex];
        counterValues[currentChannelIndex] += step;
        
        boolean recordBeaten = false;
        if (counterValues[currentChannelIndex] > highScoreValues[currentChannelIndex]) {
            highScoreValues[currentChannelIndex] = counterValues[currentChannelIndex];
            recordBeaten = true;
            saveHighScoreState();
            triggerHighScorePulseAnimation();
        }

        checkTargetGoalMilestone();

        updateDisplayMetrics();
        persistCounterState();

        if (recordBeaten) {
            playHighScoreSound();
        } else {
            playClickSoundFeedback();
        }

        String sign = step >= 0 ? "+" : "";
        String triggerType = isAuto ? "⚡ AUTO: " : "TAP: ";
        logEvent(triggerType + sign + step + " on " + getChannelName(currentChannelIndex) + " (" + counterValues[currentChannelIndex] + ")" + (recordBeaten ? " ★ HIGH RECORD!" : ""));
    }

    private void changeStepSize(int val) {
        activeSteps[currentChannelIndex] = val;
        playSelectionSound();
        persistStepState();
        updateDisplayMetrics();
        applyTheme();
        
        // Reset goal clearance when step interval changes to allow hitting goals again
        goalCleared[currentChannelIndex] = false;
        persistGoalClearedState();

        logEvent("Interval calibrated to " + (val >= 0 ? "+" : "") + val + " on " + getChannelName(currentChannelIndex));
    }

    private void cycleAutoPulseMode() {
        autoPulseMode = (autoPulseMode + 1) % 4;
        
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putInt(KEY_AUTO_PULSE, autoPulseMode);
        editor.apply();

        playSelectionSound();
        startAutoPulseIfEnabled();
        updateDisplayMetrics();
        applyTheme();

        String status = "OFF";
        if (autoPulseMode == 1) status = "1 Hz";
        if (autoPulseMode == 2) status = "2 Hz";
        if (autoPulseMode == 3) status = "5 Hz";
        logEvent("Automation pulse set to: " + status);
    }

    private void startAutoPulseIfEnabled() {
        autoPulseHandler.removeCallbacks(autoPulseRunnable);
        if (autoPulseMode > 0) {
            long delay = autoPulseMode == 1 ? 1000 : (autoPulseMode == 2 ? 500 : 200);
            autoPulseHandler.postDelayed(autoPulseRunnable, delay);
        }
    }

    private void triggerAutoPulseTick() {
        performTap(true);
    }

    private void cycleWaveform() {
        if ("sine".equals(synthWaveform)) {
            synthWaveform = "square";
        } else if ("square".equals(synthWaveform)) {
            synthWaveform = "triangle";
        } else {
            synthWaveform = "sine";
        }

        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putString(KEY_SYNTH_WAVEFORM, synthWaveform);
        editor.apply();

        playSynthSound(500f, 500f, 0.12f, synthWaveform);
        updateDisplayMetrics();
        applyTheme();
        logEvent("Synth wave lab updated: " + synthWaveform.toUpperCase());
    }

    private void cyclePitch() {
        if ("mid".equals(synthPitch)) {
            synthPitch = "treble";
        } else if ("treble".equals(synthPitch)) {
            synthPitch = "bass";
        } else {
            synthPitch = "mid";
        }

        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putString(KEY_SYNTH_PITCH, synthPitch);
        editor.apply();

        float sampleFreq = getBasePitchFrequency();
        playSynthSound(sampleFreq, sampleFreq, 0.10f, synthWaveform);
        updateDisplayMetrics();
        applyTheme();
        logEvent("Oscillator frequency bounds: " + synthPitch.toUpperCase());
    }

    private void cycleGoalTarget() {
        int currentGoal = targetGoals[currentChannelIndex];
        int nextGoal;
        switch (currentGoal) {
            case 0: nextGoal = 50; break;
            case 50: nextGoal = 100; break;
            case 100: nextGoal = 250; break;
            case 250: nextGoal = 500; break;
            case 500: nextGoal = 1000; break;
            default: nextGoal = 0; break;
        }

        targetGoals[currentChannelIndex] = nextGoal;
        goalCleared[currentChannelIndex] = false;

        // Persist goal changes
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putInt(KEY_GOAL_PREFIX + currentChannelIndex, nextGoal);
        editor.putBoolean(KEY_GOAL_CLEARED_PREFIX + currentChannelIndex, false);
        editor.apply();

        playSelectionSound();
        updateDisplayMetrics();
        applyTheme();

        String strGoal = nextGoal == 0 ? "NONE" : String.valueOf(nextGoal);
        logEvent("Goal target threshold for " + getChannelName(currentChannelIndex) + " mapped to: " + strGoal);
    }

    private void checkTargetGoalMilestone() {
        int goal = targetGoals[currentChannelIndex];
        if (goal > 0 && !goalCleared[currentChannelIndex]) {
            int currentVal = counterValues[currentChannelIndex];
            if (currentVal >= goal) {
                // Goal Unlocked! Celebrate with style
                goalCleared[currentChannelIndex] = true;
                persistGoalClearedState();

                triggerGoalCelebrationAnimation();
                playMilestoneChimes();
                logEvent("🏆 MILESTONE! " + getChannelName(currentChannelIndex) + " crossed " + goal + " threshold!");
            }
        }
    }

    private void triggerGoalCelebrationAnimation() {
        goalCelebrationFlash.setVisibility(View.VISIBLE);
        goalCelebrationFlash.setBackgroundColor(activeThemeColor);
        goalCelebrationFlash.setAlpha(0.7f);

        goalCelebrationFlash.animate()
            .alpha(0f)
            .setDuration(800)
            .withEndAction(new Runnable() {
                @Override
                public void run() {
                    goalCelebrationFlash.setVisibility(View.GONE);
                }
            })
            .start();

        // Screen shake on main container card too
        animateShake(statsCard);
        animateShake(tapCircleButton);
    }

    private void resetCounter() {
        if (soundEnabled) {
            playSweepSound(500.0f, 150.0f, 0.25f);
        } else {
            playClickSoundFeedback();
        }
        int oldValue = counterValues[currentChannelIndex];
        counterValues[currentChannelIndex] = 0;
        goalCleared[currentChannelIndex] = false;
        
        persistCounterState();
        persistGoalClearedState();
        updateDisplayMetrics();
        
        logEvent("Reset completed on " + getChannelName(currentChannelIndex) + " (Previous tally: " + oldValue + ")");
    }

    private void cycleTheme() {
        playThemeSound();
        currentThemeIndex = (currentThemeIndex + 1) % 4;
        
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putInt(KEY_THEME, currentThemeIndex);
        editor.apply();

        applyTheme();
        
        String themeName = "";
        switch (currentThemeIndex) {
            case 0: themeName = "Neon Indigo"; break;
            case 1: themeName = "Cyber Sunset"; break;
            case 2: themeName = "Mint Aurora"; break;
            case 3: themeName = "Electric Crimson"; break;
        }
        logEvent("Interface Theme Switched: " + themeName);
    }

    private void toggleSound() {
        soundEnabled = !soundEnabled;
        
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putBoolean(KEY_SOUND, soundEnabled);
        editor.apply();

        playClickSoundFeedback();
        updateDisplayMetrics();
        applyTheme();
        logEvent("Feedback audio signal configured to: " + (soundEnabled ? "ACTIVE" : "MUTED"));
    }

    private void updateDisplayMetrics() {
        int countVal = counterValues[currentChannelIndex];
        int highScoreVal = highScoreValues[currentChannelIndex];
        int stepVal = activeSteps[currentChannelIndex];
        int goalVal = targetGoals[currentChannelIndex];

        counterDisplay.setText(String.valueOf(countVal));
        highScoreBadge.setText("BEST: " + highScoreVal);
        
        String stepSymbol = stepVal >= 0 ? "+" : "";
        stepSizeDisplay.setText("Step " + stepSymbol + stepVal);
        
        String goalStr = goalVal == 0 ? "GOAL: NONE" : "GOAL: " + goalVal;
        goalDisplayText.setText(goalStr);

        btnHaptic.setText("SOUND: " + (soundEnabled ? "ON" : "OFF"));
        
        String pulseBtnText = "PULSE: OFF";
        if (autoPulseMode == 1) pulseBtnText = "PULSE: 1Hz";
        if (autoPulseMode == 2) pulseBtnText = "PULSE: 2Hz";
        if (autoPulseMode == 3) pulseBtnText = "PULSE: 5Hz";
        btnLabAutoPulse.setText(pulseBtnText);

        btnLabWaveform.setText("WAVE: " + synthWaveform.toUpperCase());
        btnLabPitch.setText("PITCH: " + synthPitch.toUpperCase());
        btnLabGoal.setText(goalVal == 0 ? "GOAL: NONE" : "GOAL: " + goalVal);
    }

    private void applyTheme() {
        final int rootBgStart, rootBgEnd;
        final int cardBgStartVal, cardBgEndVal;
        final int textPrimary, textSecondary;
        final int accentText, badgeBorder;
        final int activeBtnStart, activeBtnEnd;
        final int inactiveBtnStart, inactiveBtnEnd;
        final int circleInner, circleStroke;

        switch (currentThemeIndex) {
            case 0: // Neon Indigo
                rootBgStart = 0xFF080C14;
                rootBgEnd = 0xFF18233C;
                cardBgStartVal = 0xFF1E2E4A;
                cardBgEndVal = 0xFF111A2E;
                textPrimary = 0xFFF1F5F9;
                textSecondary = 0xFF94A3B8;
                accentText = 0xFF38BDF8;
                activeBtnStart = 0xFF3B82F6;
                activeBtnEnd = 0xFF1D4ED8;
                inactiveBtnStart = 0xFF27354A;
                inactiveBtnEnd = 0xFF151D2A;
                circleInner = 0xFF1E40AF;
                circleStroke = 0xFF60A5FA;
                badgeBorder = 0xFF3B82F6;
                break;

            case 1: // Cyber Sunset
                rootBgStart = 0xFF120516;
                rootBgEnd = 0xFF2C0A21;
                cardBgStartVal = 0xFF3C0D2E;
                cardBgEndVal = 0xFF1E061E;
                textPrimary = 0xFFFFF1F2;
                textSecondary = 0xFFE2E8F0;
                accentText = 0xFFF43F5E;
                activeBtnStart = 0xFFEC4899;
                activeBtnEnd = 0xFF9D174D;
                inactiveBtnStart = 0xFF2D1E36;
                inactiveBtnEnd = 0xFF1B0F22;
                circleInner = 0xFF9D174D;
                circleStroke = 0xFFF472B6;
                badgeBorder = 0xFFEC4899;
                break;

            case 2: // Mint Aurora
                rootBgStart = 0xFF01140E;
                rootBgEnd = 0xFF0C3327;
                cardBgStartVal = 0xFF0E4B3E;
                cardBgEndVal = 0xFF062B21;
                textPrimary = 0xFFF0FDF4;
                textSecondary = 0xFF94A3B8;
                accentText = 0xFF10B981;
                activeBtnStart = 0xFF10B981;
                activeBtnEnd = 0xFF047857;
                inactiveBtnStart = 0xFF163E32;
                inactiveBtnEnd = 0xFF0A201A;
                circleInner = 0xFF065F46;
                circleStroke = 0xFF34D399;
                badgeBorder = 0xFF10B981;
                break;

            case 3: // Electric Crimson
                rootBgStart = 0xFF080202;
                rootBgEnd = 0xFF220505;
                cardBgStartVal = 0xFF440808;
                cardBgEndVal = 0xFF1A0303;
                textPrimary = 0xFFFAFAFA;
                textSecondary = 0xFFA1A1AA;
                accentText = 0xFFEF4444;
                activeBtnStart = 0xFFEF4444;
                activeBtnEnd = 0xFFB91C1C;
                inactiveBtnStart = 0xFF323236;
                inactiveBtnEnd = 0xFF1F1F22;
                circleInner = 0xFF991B1B;
                circleStroke = 0xFFF87171;
                badgeBorder = 0xFFEF4444;
                break;

            default:
                return;
        }

        activeThemeColor = activeBtnStart;
        cardBgStart = cardBgStartVal;
        cardBgEnd = cardBgEndVal;

        // Initialize transition cache boundaries
        final int initialRootBgStart = (lastRootBgStart != 0) ? lastRootBgStart : rootBgStart;
        final int initialRootBgEnd = (lastRootBgEnd != 0) ? lastRootBgEnd : rootBgEnd;
        final int initialCardBgStart = (lastCardBgStart != 0) ? lastCardBgStart : cardBgStartVal;
        final int initialCardBgEnd = (lastCardBgEnd != 0) ? lastCardBgEnd : cardBgEndVal;

        lastRootBgStart = rootBgStart;
        lastRootBgEnd = rootBgEnd;
        lastCardBgStart = cardBgStartVal;
        lastCardBgEnd = cardBgEndVal;

        // Animate Root and Card backgrounds synchronously using smooth linear gradient evaluation
        final ArgbEvaluator evaluator = new ArgbEvaluator();
        ValueAnimator themeAnim = ValueAnimator.ofFloat(0f, 1f);
        themeAnim.setDuration(400);
        themeAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float fraction = animation.getAnimatedFraction();
                
                int currRootStart = (Integer) evaluator.evaluate(fraction, initialRootBgStart, rootBgStart);
                int currRootEnd = (Integer) evaluator.evaluate(fraction, initialRootBgEnd, rootBgEnd);
                
                GradientDrawable bgGrad = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{ currRootStart, currRootEnd }
                );
                mainRootLayout.setBackground(bgGrad);

                int currCardStart = (Integer) evaluator.evaluate(fraction, initialCardBgStart, cardBgStartVal);
                int currCardEnd = (Integer) evaluator.evaluate(fraction, initialCardBgEnd, cardBgEndVal);
                
                GradientDrawable cardGrad = new GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    new int[]{ currCardStart, currCardEnd }
                );
                cardGrad.setCornerRadius(dpToPx(16));
                cardGrad.setStroke(dpToPx(1), 0x22FFFFFF);
                statsCard.setBackground(cardGrad);
            }
        });
        themeAnim.start();

        // Style the central circle button with radial styling
        GradientDrawable circleShape = new GradientDrawable();
        circleShape.setColors(new int[]{ circleInner, rootBgStart });
        circleShape.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        circleShape.setGradientRadius(dpToPx(100));
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

        // Style the active mode selectors with smooth vertical gradients
        setButtonStyle(btnStep1, activeSteps[currentChannelIndex] == 1 ? activeBtnStart : inactiveBtnStart, activeSteps[currentChannelIndex] == 1 ? activeBtnEnd : inactiveBtnEnd, activeSteps[currentChannelIndex] == 1);
        setButtonStyle(btnStep5, activeSteps[currentChannelIndex] == 5 ? activeBtnStart : inactiveBtnStart, activeSteps[currentChannelIndex] == 5 ? activeBtnEnd : inactiveBtnEnd, activeSteps[currentChannelIndex] == 5);
        setButtonStyle(btnStep10, activeSteps[currentChannelIndex] == 10 ? activeBtnStart : inactiveBtnStart, activeSteps[currentChannelIndex] == 10 ? activeBtnEnd : inactiveBtnEnd, activeSteps[currentChannelIndex] == 10);
        setButtonStyle(btnStepMinus, activeSteps[currentChannelIndex] == -1 ? activeBtnStart : inactiveBtnStart, activeSteps[currentChannelIndex] == -1 ? activeBtnEnd : inactiveBtnEnd, activeSteps[currentChannelIndex] == -1);

        // Style Channel Selector Buttons
        setButtonStyle(btnChan0, currentChannelIndex == 0 ? activeBtnStart : inactiveBtnStart, currentChannelIndex == 0 ? activeBtnEnd : inactiveBtnEnd, currentChannelIndex == 0);
        setButtonStyle(btnChan1, currentChannelIndex == 1 ? activeBtnStart : inactiveBtnStart, currentChannelIndex == 1 ? activeBtnEnd : inactiveBtnEnd, currentChannelIndex == 1);
        setButtonStyle(btnChan2, currentChannelIndex == 2 ? activeBtnStart : inactiveBtnStart, currentChannelIndex == 2 ? activeBtnEnd : inactiveBtnEnd, currentChannelIndex == 2);

        // Style Advanced Synth Lab controls
        setButtonStyle(btnLabAutoPulse, autoPulseMode > 0 ? activeBtnStart : inactiveBtnStart, autoPulseMode > 0 ? activeBtnEnd : inactiveBtnEnd, autoPulseMode > 0);
        setButtonStyle(btnLabWaveform, inactiveBtnStart, inactiveBtnEnd, false);
        setButtonStyle(btnLabPitch, inactiveBtnStart, inactiveBtnEnd, false);
        setButtonStyle(btnLabGoal, targetGoals[currentChannelIndex] > 0 ? activeBtnStart : inactiveBtnStart, targetGoals[currentChannelIndex] > 0 ? activeBtnEnd : inactiveBtnEnd, targetGoals[currentChannelIndex] > 0);

        // Style bottom action rows
        setButtonStyle(btnReset, inactiveBtnStart, inactiveBtnEnd, false);
        setButtonStyle(btnTheme, activeBtnStart, activeBtnEnd, true);
        setButtonStyle(btnHaptic, soundEnabled ? activeBtnStart : inactiveBtnStart, soundEnabled ? activeBtnEnd : inactiveBtnEnd, soundEnabled);

        // Style the high score status widget with an angled background gradient
        GradientDrawable badgeShape = new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{ cardBgStartVal, cardBgEndVal }
        );
        badgeShape.setCornerRadius(dpToPx(10));
        badgeShape.setStroke(dpToPx(2), badgeBorder);
        highScoreBadge.setBackground(badgeShape);
    }

    private void setButtonStyle(Button btn, int colorStart, int colorEnd, boolean isActive) {
        GradientDrawable shape = new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{ colorStart, colorEnd }
        );
        shape.setCornerRadius(dpToPx(10));
        if (isActive) {
            shape.setStroke(dpToPx(2), Color.WHITE);
        } else {
            shape.setStroke(dpToPx(1), 0x33FFFFFF);
        }
        btn.setBackground(shape);
        btn.setTextColor(0xFFFFFFFF);
    }

    // =========================================================================
    // POP BUBBLE MECHANICS
    // =========================================================================

    private void spawnAmbientPopBubble() {
        int containerWidth = tapContainerFrame.getWidth();
        int containerHeight = tapContainerFrame.getHeight();
        if (containerWidth <= 0 || containerHeight <= 0) {
            return;
        }

        final RelativeLayout bubbleLayout = new RelativeLayout(this);
        int bubbleSize = dpToPx(random.nextInt(20) + 40); // 40dp to 60dp
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(bubbleSize, bubbleSize);
        bubbleLayout.setLayoutParams(layoutParams);

        // 3D bubble radial gradient treatment matching the theme color
        GradientDrawable bubbleDrawable = new GradientDrawable();
        bubbleDrawable.setShape(GradientDrawable.OVAL);
        int neonColor = activeThemeColor;
        int translucentColor = Color.argb(125, Color.red(neonColor), Color.green(neonColor), Color.blue(neonColor));
        bubbleDrawable.setColors(new int[]{ 0xAAFFFFFF, translucentColor, translucentColor });
        bubbleDrawable.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        bubbleDrawable.setGradientRadius(bubbleSize * 0.75f);
        bubbleDrawable.setStroke(dpToPx(2), Color.argb(200, 255, 255, 255));
        bubbleLayout.setBackground(bubbleDrawable);

        // Add a white shine reflection inside the bubble
        View reflection = new View(this);
        RelativeLayout.LayoutParams reflParams = new RelativeLayout.LayoutParams(bubbleSize / 4, bubbleSize / 4);
        reflParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        reflParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        reflParams.topMargin = bubbleSize / 6;
        reflParams.leftMargin = bubbleSize / 6;
        reflection.setLayoutParams(reflParams);
        GradientDrawable reflShape = new GradientDrawable();
        reflShape.setShape(GradientDrawable.OVAL);
        reflShape.setColor(Color.argb(220, 255, 255, 255));
        reflection.setBackground(reflShape);
        bubbleLayout.addView(reflection);

        // Add numerical value preview inside the bubble
        final int currentStep = activeSteps[currentChannelIndex];
        TextView numText = new TextView(this);
        RelativeLayout.LayoutParams textParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
        );
        numText.setLayoutParams(textParams);
        numText.setGravity(Gravity.CENTER);
        numText.setText(currentStep >= 0 ? "+" + currentStep : String.valueOf(currentStep));
        numText.setTextColor(Color.WHITE);
        numText.setTextSize(10);
        numText.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        bubbleLayout.addView(numText);

        // Click event on bubble triggers POP action
        bubbleLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popTheBubble(bubbleLayout, currentStep);
            }
        });

        tapContainerFrame.addView(bubbleLayout);

        // Random starting positions and paths
        float startX = random.nextFloat() * (containerWidth - bubbleSize);
        final float startY = containerHeight;
        bubbleLayout.setTranslationX(startX);
        bubbleLayout.setTranslationY(startY);
        bubbleLayout.setAlpha(0.0f);
        bubbleLayout.setScaleX(0.5f);
        bubbleLayout.setScaleY(0.5f);

        final float driftOffset = (random.nextFloat() * dpToPx(50)) - dpToPx(25);
        long duration = 3200 + random.nextInt(2000);

        bubbleLayout.animate()
            .translationY(-bubbleSize)
            .translationXBy(driftOffset)
            .alpha(1.0f)
            .scaleX(1.0f)
            .scaleY(1.0f)
            .setDuration(duration)
            .withEndAction(new Runnable() {
                @Override
                public void run() {
                    try {
                        tapContainerFrame.removeView(bubbleLayout);
                    } catch (Exception ignored) {}
                }
            })
            .start();
    }

    private void popTheBubble(final View bubbleView, int stepVal) {
        float popX = bubbleView.getTranslationX() + (bubbleView.getWidth() / 2f) - (tapContainerFrame.getWidth() / 2f);
        float popY = bubbleView.getTranslationY() + (bubbleView.getHeight() / 2f) - (tapContainerFrame.getHeight() / 2f);

        bubbleView.animate().cancel();

        // Spawn visual pops feedbacks
        spawnFloatingIndicator(popX, popY);
        spawnParticleBurst(popX, popY);

        playPopSound();

        bubbleView.animate()
            .scaleX(1.6f)
            .scaleY(1.6f)
            .alpha(0.0f)
            .setDuration(100)
            .withEndAction(new Runnable() {
                @Override
                public void run() {
                    try {
                        tapContainerFrame.removeView(bubbleView);
                    } catch (Exception ignored) {}
                }
            })
            .start();

        // Add bubble points to active channel
        counterValues[currentChannelIndex] += stepVal;
        boolean recordBeaten = false;
        if (counterValues[currentChannelIndex] > highScoreValues[currentChannelIndex]) {
            highScoreValues[currentChannelIndex] = counterValues[currentChannelIndex];
            recordBeaten = true;
            saveHighScoreState();
            triggerHighScorePulseAnimation();
        }

        checkTargetGoalMilestone();

        updateDisplayMetrics();
        persistCounterState();

        if (recordBeaten) {
            playHighScoreSound();
        }

        String sign = stepVal >= 0 ? "+" : "";
        logEvent("🫧 POPPED! " + sign + stepVal + " on " + getChannelName(currentChannelIndex) + " (" + counterValues[currentChannelIndex] + ")" + (recordBeaten ? " ★ HIGHSCORE!" : ""));
        triggerCounterPopAnimation();
        triggerRippleWave();
    }

    // =========================================================================
    // HIGH POLISH NATIVE UI ANIMATIONS & SHOCKWAVES
    // =========================================================================

    private void triggerRippleWave() {
        final View ripple = new View(this);
        int size = dpToPx(170); // Matches circular tap button dimension
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(size, size);
        params.gravity = Gravity.CENTER;
        ripple.setLayoutParams(params);

        GradientDrawable ring = new GradientDrawable();
        ring.setShape(GradientDrawable.OVAL);
        ring.setColor(Color.TRANSPARENT);
        ring.setStroke(dpToPx(3), activeThemeColor);
        ripple.setBackground(ring);

        // Add behind the active views in tap container FrameLayout
        tapContainerFrame.addView(ripple, 0);

        ripple.setScaleX(1.0f);
        ripple.setScaleY(1.0f);
        ripple.setAlpha(0.8f);

        ripple.animate()
            .scaleX(2.8f)
            .scaleY(2.8f)
            .alpha(0.0f)
            .setDuration(600)
            .withEndAction(new Runnable() {
                @Override
                public void run() {
                    try {
                        tapContainerFrame.removeView(ripple);
                    } catch (Exception ignored) {}
                }
            })
            .start();
    }

    private void spawnAmbientEmberSpark() {
        int containerWidth = tapContainerFrame.getWidth();
        int containerHeight = tapContainerFrame.getHeight();
        if (containerWidth <= 0 || containerHeight <= 0) return;

        final View spark = new View(this);
        int size = dpToPx(random.nextInt(5) + 3); // 3dp to 7dp sparks
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(size, size);
        params.gravity = Gravity.CENTER;
        spark.setLayoutParams(params);

        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.OVAL);
        // Randomly alternate between current active theme accent and vibrant gold amber
        int color = (random.nextBoolean()) ? activeThemeColor : 0xFFF59E0B;
        shape.setColor(color);
        spark.setBackground(shape);

        // Add background spark
        tapContainerFrame.addView(spark, 0);

        float startX = random.nextFloat() * containerWidth;
        // Offset coordinate relative to FrameLayout center alignment
        spark.setTranslationX(startX - (containerWidth / 2f));
        spark.setTranslationY((containerHeight / 2f) - dpToPx(10));
        spark.setAlpha(0.0f);

        float driftX = (random.nextFloat() * dpToPx(60)) - dpToPx(30);
        float driftY = -((containerHeight * 0.75f) + random.nextFloat() * (containerHeight * 0.25f));

        spark.animate()
            .translationXBy(driftX)
            .translationYBy(driftY)
            .alpha(0.9f)
            .scaleX(1.6f)
            .scaleY(1.6f)
            .setDuration(1200 + random.nextInt(1000))
            .withEndAction(new Runnable() {
                @Override
                public void run() {
                    spark.animate()
                        .alpha(0.0f)
                        .scaleX(0.2f)
                        .scaleY(0.2f)
                        .setDuration(300)
                        .withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    tapContainerFrame.removeView(spark);
                                } catch (Exception ignored) {}
                            }
                        })
                        .start();
                }
            })
            .start();
    }

    private void animateButtonPress(final View view) {
        view.animate()
            .scaleX(0.92f)
            .scaleY(0.92f)
            .setDuration(70)
            .withEndAction(new Runnable() {
                @Override
                public void run() {
                    view.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(100)
                        .start();
                }
            })
            .start();
    }

    private void animateShake(final View view) {
        view.animate()
            .translationX(dpToPx(10))
            .setDuration(40)
            .withEndAction(new Runnable() {
                @Override
                public void run() {
                    view.animate()
                        .translationX(-dpToPx(10))
                        .setDuration(40)
                        .withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                view.animate()
                                    .translationX(dpToPx(5))
                                    .setDuration(40)
                                    .withEndAction(new Runnable() {
                                        @Override
                                        public void run() {
                                            view.animate()
                                                .translationX(0)
                                                .setDuration(40)
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
        tapActionSublabel.setAlpha(0.3f);
        tapActionSublabel.animate()
            .alpha(1.0f)
            .setDuration(900)
            .withEndAction(new Runnable() {
                @Override
                public void run() {
                    tapActionSublabel.animate()
                        .alpha(0.3f)
                        .setDuration(900)
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
            .scaleX(0.90f)
            .scaleY(0.90f)
            .setDuration(50)
            .withEndAction(new Runnable() {
                @Override
                public void run() {
                    tapCircleButton.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(100)
                        .start();
                }
            })
            .start();
    }

    private void triggerCounterPopAnimation() {
        counterDisplay.animate()
            .scaleX(1.15f)
            .scaleY(1.15f)
            .setDuration(60)
            .withEndAction(new Runnable() {
                @Override
                public void run() {
                    counterDisplay.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(120)
                        .start();
                }
            })
            .start();
    }

    private void triggerHighScorePulseAnimation() {
        highScoreBadge.animate()
            .scaleX(1.30f)
            .scaleY(1.30f)
            .setDuration(150)
            .withEndAction(new Runnable() {
                @Override
                public void run() {
                    highScoreBadge.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(120)
                        .start();
                }
            })
            .start();
    }

    private void spawnFloatingIndicator(float pivotOffsetX, float pivotOffsetY) {
        final TextView floatText = new TextView(this);
        int step = activeSteps[currentChannelIndex];
        String textSign = step >= 0 ? "+" : "";
        floatText.setText(textSign + step);
        floatText.setTextColor(activeThemeColor);
        floatText.setTextSize(24);
        floatText.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        floatText.setGravity(Gravity.CENTER);

        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        );
        layoutParams.gravity = Gravity.CENTER;
        floatText.setLayoutParams(layoutParams);

        tapContainerFrame.addView(floatText);

        float randomXDrift = pivotOffsetX + (random.nextFloat() * dpToPx(30)) - dpToPx(15);
        floatText.setTranslationX(randomXDrift);
        floatText.setTranslationY(pivotOffsetY);
        floatText.setAlpha(1.0f);

        floatText.animate()
            .translationY(pivotOffsetY - dpToPx(110))
            .alpha(0f)
            .scaleX(1.2f)
            .scaleY(1.2f)
            .setDuration(600)
            .withEndAction(new Runnable() {
                @Override
                public void run() {
                    try {
                        tapContainerFrame.removeView(floatText);
                    } catch (Exception ignored) {}
                }
            })
            .start();
    }

    private void spawnParticleBurst(float pivotOffsetX, float pivotOffsetY) {
        for (int i = 0; i < 6; i++) {
            final View particle = new View(this);
            int size = dpToPx(random.nextInt(5) + 4);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(size, size);
            params.gravity = Gravity.CENTER;
            particle.setLayoutParams(params);

            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.OVAL);
            shape.setColor(activeThemeColor);
            particle.setBackground(shape);

            tapContainerFrame.addView(particle);

            double angle = random.nextDouble() * 2 * Math.PI;
            float distance = dpToPx(random.nextInt(70) + 50);
            float destX = pivotOffsetX + (float) (Math.cos(angle) * distance);
            float destY = pivotOffsetY + (float) (Math.sin(angle) * distance);

            particle.setTranslationX(pivotOffsetX);
            particle.setTranslationY(pivotOffsetY);
            particle.setAlpha(1.0f);
            particle.setScaleX(1.0f);
            particle.setScaleY(1.0f);

            particle.animate()
                .translationX(destX)
                .translationY(destY)
                .alpha(0f)
                .scaleX(0.1f)
                .scaleY(0.1f)
                .setDuration(400 + random.nextInt(150))
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            tapContainerFrame.removeView(particle);
                        } catch (Exception ignored) {}
                    }
                })
                .start();
        }
    }

    // =========================================================================
    // SYNTHESIZED SOUND SYSTEM (Android AudioTrack API)
    // =========================================================================

    private float getBasePitchFrequency() {
        if ("bass".equals(synthPitch)) {
            return 220.0f; // Low A
        } else if ("treble".equals(synthPitch)) {
            return 1100.0f; // High
        } else {
            return 550.0f; // Medium / Mid range
        }
    }

    private void playPopSound() {
        if (soundEnabled) {
            float baseFreq = getBasePitchFrequency();
            playSynthSound(baseFreq * 0.8f, baseFreq * 2.5f, 0.05f, synthWaveform);
        }
        mainRootLayout.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
    }

    private void playClickSoundFeedback() {
        if (soundEnabled) {
            float baseFreq = getBasePitchFrequency();
            playSynthSound(baseFreq, baseFreq, 0.06f, synthWaveform);
        }
        mainRootLayout.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
    }

    private void playSelectionSound() {
        if (soundEnabled) {
            float baseFreq = getBasePitchFrequency();
            playSynthSound(baseFreq * 1.5f, baseFreq * 1.5f, 0.05f, "sine");
        }
    }

    private void playThemeSound() {
        if (soundEnabled) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    float base = getBasePitchFrequency();
                    playSingleTone(base * 0.8f, 0.05f, "sine");
                    playSingleTone(base * 1.2f, 0.07f, "sine");
                }
            }).start();
        }
    }

    private void playHighScoreSound() {
        if (soundEnabled) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    float base = getBasePitchFrequency();
                    float[] notes = { base, base * 1.25f, base * 1.5f, base * 2.0f };
                    for (int i = 0; i < notes.length; i++) {
                        playSingleTone(notes[i], 0.08f, synthWaveform);
                        try {
                            Thread.sleep(60);
                        } catch (InterruptedException ignored) {}
                    }
                }
            }).start();
        }
    }

    private void playMilestoneChimes() {
        if (soundEnabled) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    float base = getBasePitchFrequency();
                    float[] notes = { base * 0.5f, base, base * 1.5f, base * 2.0f, base * 2.5f, base * 3.0f };
                    for (int i = 0; i < notes.length; i++) {
                        playSingleTone(notes[i], 0.12f, "triangle");
                        try {
                            Thread.sleep(80);
                        } catch (InterruptedException ignored) {}
                    }
                }
            }).start();
        }
    }

    private void playSynthSound(final float startFreq, final float endFreq, final float durationS, final String waveType) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (startFreq == endFreq) {
                    playSingleTone(startFreq, durationS, waveType);
                } else {
                    playSweepSound(startFreq, endFreq, durationS);
                }
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
            
            sample[i] = (short) (val * 32767 * envelope * 0.40);
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
            sample[i] = (short) (val * 32767 * envelope * 0.35);
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

    // =========================================================================

    private void logEvent(String msg) {
        final LinearLayout itemLayout = new LinearLayout(this);
        itemLayout.setOrientation(LinearLayout.HORIZONTAL);
        itemLayout.setGravity(Gravity.CENTER_VERTICAL);
        itemLayout.setPadding(dpToPx(6), dpToPx(6), dpToPx(6), dpToPx(6));

        GradientDrawable itemBg = new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{ cardBgEnd, cardBgStart }
        );
        itemBg.setCornerRadius(dpToPx(8));
        itemBg.setStroke(dpToPx(1), 0x11FFFFFF);
        itemLayout.setBackground(itemBg);

        LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        itemParams.setMargins(0, 0, 0, dpToPx(5));
        itemLayout.setLayoutParams(itemParams);

        View dot = new View(this);
        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dpToPx(6), dpToPx(6));
        dotParams.setMargins(0, 0, dpToPx(8), 0);
        dot.setLayoutParams(dotParams);
        
        GradientDrawable dotShape = new GradientDrawable();
        dotShape.setColor(activeThemeColor);
        dotShape.setShape(GradientDrawable.OVAL);
        dot.setBackground(dotShape);

        TextView entry = new TextView(this);
        entry.setText(msg);
        entry.setTextColor(0xFFE2E8F0);
        entry.setTextSize(10.5f);
        entry.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        itemLayout.addView(dot);
        itemLayout.addView(entry);

        historyContainer.addView(itemLayout, 0);

        itemLayout.setAlpha(0f);
        itemLayout.setTranslationY(dpToPx(15));
        itemLayout.animate()
            .alpha(1.0f)
            .translationY(0f)
            .setDuration(220)
            .start();

        if (historyContainer.getChildCount() > 30) {
            historyContainer.removeViews(30, historyContainer.getChildCount() - 30);
        }

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

    private void persistCounterState() {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putInt(KEY_COUNT_PREFIX + currentChannelIndex, counterValues[currentChannelIndex]);
        editor.apply();
    }

    private void saveHighScoreState() {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putInt(KEY_HIGH_SCORE_PREFIX + currentChannelIndex, highScoreValues[currentChannelIndex]);
        editor.apply();
    }

    private void persistStepState() {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putInt(KEY_STEP_PREFIX + currentChannelIndex, activeSteps[currentChannelIndex]);
        editor.apply();
    }

    private void persistGoalClearedState() {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putBoolean(KEY_GOAL_CLEARED_PREFIX + currentChannelIndex, goalCleared[currentChannelIndex]);
        editor.apply();
    }
}