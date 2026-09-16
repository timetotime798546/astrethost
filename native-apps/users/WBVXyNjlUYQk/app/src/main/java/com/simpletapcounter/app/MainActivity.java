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

    private int counterValue = 0;
    private int highScoreValue = 0;
    private int activeStep = 1;
    private int currentThemeIndex = 0; // 0: Neon Indigo, 1: Cyber Sunset, 2: Mint Aurora, 3: Electric Crimson
    private boolean soundEnabled = true;

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

    // Dynamic Color definitions for themed UI components
    private int activeThemeColor;
    private int cardBgStart;
    private int cardBgEnd;
    private Random random = new Random();

    // Bubble Pop Spawner Scheduler
    private Handler bubbleHandler;
    private Runnable bubbleSpawnRunnable;

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

        // Initialize background Bubble Pop Spawner
        bubbleHandler = new Handler();
        bubbleSpawnRunnable = new Runnable() {
            @Override
            public void run() {
                spawnAmbientPopBubble();
                bubbleHandler.postDelayed(this, 2200 + random.nextInt(1200));
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Start bubble spawner when application enters foreground
        bubbleHandler.removeCallbacks(bubbleSpawnRunnable);
        bubbleHandler.postDelayed(bubbleSpawnRunnable, 1500);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Pause spawning to conserve resources
        bubbleHandler.removeCallbacks(bubbleSpawnRunnable);
    }

    private void performTap() {
        triggerTapScaleAnimation();
        triggerCounterPopAnimation();
        spawnFloatingIndicator(0, 0); // Spawns from center
        spawnParticleBurst(0, 0);

        // Also occasionally spit out mini pops bubbles on tap
        if (random.nextBoolean()) {
            spawnAmbientPopBubble();
        }

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
        logEvent("Interface Theme Switched: " + themeName);
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

        // Animate Root and Card backgrounds synchronously using smooth multi-stop linear gradient evaluation
        final ArgbEvaluator evaluator = new ArgbEvaluator();
        ValueAnimator themeAnim = ValueAnimator.ofFloat(0f, 1f);
        themeAnim.setDuration(450);
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
                cardGrad.setCornerRadius(dpToPx(20));
                cardGrad.setStroke(dpToPx(1), 0x22FFFFFF);
                statsCard.setBackground(cardGrad);
            }
        });
        themeAnim.start();

        // Style the elegant central circular control dial with a dynamic radial gradient
        GradientDrawable circleShape = new GradientDrawable();
        circleShape.setColors(new int[]{ circleInner, rootBgStart });
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

        // Style the active mode selectors with smooth vertical gradients
        setButtonStyle(btnStep1, activeStep == 1 ? activeBtnStart : inactiveBtnStart, activeStep == 1 ? activeBtnEnd : inactiveBtnEnd, activeStep == 1);
        setButtonStyle(btnStep5, activeStep == 5 ? activeBtnStart : inactiveBtnStart, activeStep == 5 ? activeBtnEnd : inactiveBtnEnd, activeStep == 5);
        setButtonStyle(btnStep10, activeStep == 10 ? activeBtnStart : inactiveBtnStart, activeStep == 10 ? activeBtnEnd : inactiveBtnEnd, activeStep == 10);
        setButtonStyle(btnStepMinus, activeStep == -1 ? activeBtnStart : inactiveBtnStart, activeStep == -1 ? activeBtnEnd : inactiveBtnEnd, activeStep == -1);

        // Style global layout action controls with rich gradients
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
    // POP BUBBLE MECHANICS
    // =========================================================================

    private void spawnAmbientPopBubble() {
        int containerWidth = tapContainerFrame.getWidth();
        int containerHeight = tapContainerFrame.getHeight();
        if (containerWidth <= 0 || containerHeight <= 0) {
            // Container layout not yet measured
            return;
        }

        final RelativeLayout bubbleLayout = new RelativeLayout(this);
        int bubbleSize = dpToPx(random.nextInt(25) + 40); // 40dp to 65dp
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

        // Add a tiny white shine reflection inside the bubble
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
        TextView numText = new TextView(this);
        RelativeLayout.LayoutParams textParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
        );
        numText.setLayoutParams(textParams);
        numText.setGravity(Gravity.CENTER);
        numText.setText(activeStep >= 0 ? "+" + activeStep : String.valueOf(activeStep));
        numText.setTextColor(Color.WHITE);
        numText.setTextSize(11);
        numText.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        bubbleLayout.addView(numText);

        // Click event on bubble triggers satisfying POP action
        bubbleLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popTheBubble(bubbleLayout);
            }
        });

        // Add to tap container
        tapContainerFrame.addView(bubbleLayout);

        // Random starting positions and paths
        float startX = random.nextFloat() * (containerWidth - bubbleSize);
        final float startY = containerHeight;
        bubbleLayout.setTranslationX(startX);
        bubbleLayout.setTranslationY(startY);
        bubbleLayout.setAlpha(0.0f);
        bubbleLayout.setScaleX(0.4f);
        bubbleLayout.setScaleY(0.4f);

        // Build continuous dynamic sine wave drift values
        final float driftOffset = (random.nextFloat() * dpToPx(60)) - dpToPx(30);
        long duration = 3000 + random.nextInt(2500);

        // Animate floating bubble upwards
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

    private void popTheBubble(final View bubbleView) {
        float popX = bubbleView.getTranslationX() + (bubbleView.getWidth() / 2f) - (tapContainerFrame.getWidth() / 2f);
        float popY = bubbleView.getTranslationY() + (bubbleView.getHeight() / 2f) - (tapContainerFrame.getHeight() / 2f);

        // Stop existing floating translation animations
        bubbleView.animate().cancel();

        // Spawn visual pops feedbacks
        spawnFloatingIndicator(popX, popY);
        spawnParticleBurst(popX, popY);

        // Trigger satisfying POP sound tone
        playPopSound();

        // Pop Animation - Shrink to zero quickly with massive expand explosion
        bubbleView.animate()
            .scaleX(1.8f)
            .scaleY(1.8f)
            .alpha(0.0f)
            .setDuration(90)
            .withEndAction(new Runnable() {
                @Override
                public void run() {
                    try {
                        tapContainerFrame.removeView(bubbleView);
                    } catch (Exception ignored) {}
                }
            })
            .start();

        // Increment Matrix score
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
        }

        String sign = activeStep >= 0 ? "+" : "";
        logEvent("🫧 POPPED! " + sign + activeStep + " (Tally: " + counterValue + ")" + (recordBeaten ? " ★ HIGH SCORE!" : ""));
        triggerCounterPopAnimation();
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

    private void spawnFloatingIndicator(float pivotOffsetX, float pivotOffsetY) {
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

        // Render float with custom/random offset coordinates
        float randomXDrift = pivotOffsetX + (random.nextFloat() * dpToPx(40)) - dpToPx(20);
        floatText.setTranslationX(randomXDrift);
        floatText.setTranslationY(pivotOffsetY);
        floatText.setAlpha(1.0f);

        // Perform 650ms dynamic upward floating motion + fading animation
        floatText.animate()
            .translationY(pivotOffsetY - dpToPx(130))
            .alpha(0f)
            .scaleX(1.3f)
            .scaleY(1.3f)
            .setDuration(650)
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
        for (int i = 0; i < 8; i++) {
            final View particle = new View(this);
            int size = dpToPx(random.nextInt(6) + 5);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(size, size);
            params.gravity = Gravity.CENTER;
            particle.setLayoutParams(params);

            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.OVAL);
            shape.setColor(activeThemeColor);
            particle.setBackground(shape);

            tapContainerFrame.addView(particle);

            double angle = random.nextDouble() * 2 * Math.PI;
            float distance = dpToPx(random.nextInt(90) + 60);
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
                .setDuration(450 + random.nextInt(200))
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

    private void playPopSound() {
        if (soundEnabled) {
            // Distinct pop sound: hyper-fast high-frequency sweep
            playSynthSound(450.0f, 2200.0f, 0.04f, "sine");
        }
        mainRootLayout.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
    }

    private void playClickSoundFeedback() {
        if (soundEnabled) {
            playSynthSound(680.0f, 680.0f, 0.07f, "sine");
        }
        mainRootLayout.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
    }

    private void playSelectionSound() {
        if (soundEnabled) {
            playSynthSound(880.0f, 880.0f, 0.05f, "sine");
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

    // =========================================================================

    private void logEvent(String msg) {
        // Build timeline log capsule item layout
        final LinearLayout itemLayout = new LinearLayout(this);
        itemLayout.setOrientation(LinearLayout.HORIZONTAL);
        itemLayout.setGravity(Gravity.CENTER_VERTICAL);
        itemLayout.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));

        // Horizontal gradient alignment for timeline capsules
        GradientDrawable itemBg = new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{ cardBgEnd, cardBgStart }
        );
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