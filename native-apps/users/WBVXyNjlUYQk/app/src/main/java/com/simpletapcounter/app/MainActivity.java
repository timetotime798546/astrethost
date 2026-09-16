package com.simpletapcounter.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.SoundEffectConstants;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private int counterValue = 0;
    private int highScoreValue = 0;
    private int activeStep = 1;
    private int currentThemeIndex = 0; // 0: Classic Indigo, 1: Sunset Aura, 2: Mint Fresh, 3: Obsidian Flame
    private boolean soundEnabled = true;

    // View bindings
    private LinearLayout mainRootLayout;
    private LinearLayout statsCard;
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

    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind all UI components
        mainRootLayout = (LinearLayout) findViewById(R.id.main_root_layout);
        statsCard = (LinearLayout) findViewById(R.id.stats_card);
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
                changeStepSize(1);
            }
        });

        btnStep5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeStepSize(5);
            }
        });

        btnStep10.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeStepSize(10);
            }
        });

        btnStepMinus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeStepSize(-1);
            }
        });

        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetCounter();
            }
        });

        btnTheme.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cycleTheme();
            }
        });

        btnHaptic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleSound();
            }
        });

        // Initialize history with loaded notification
        logEvent("Counter Initialized. Value: " + counterValue);
    }

    private void performTap() {
        triggerTapScaleAnimation();
        playClickSoundFeedback();

        counterValue += activeStep;
        
        boolean recordBeaten = false;
        if (counterValue > highScoreValue) {
            highScoreValue = counterValue;
            recordBeaten = true;
            saveHighScoreState();
        }

        updateDisplayMetrics();
        persistCounterState();

        String changeSign = activeStep >= 0 ? "+" : "";
        logEvent(timeFormat.format(new Date()) + " - Added " + changeSign + activeStep + " (New Total: " + counterValue + ")" + (recordBeaten ? " ★ BEST!" : ""));
    }

    private void changeStepSize(int val) {
        activeStep = val;
        playClickSoundFeedback();
        persistStepState();
        updateDisplayMetrics();
        applyTheme();
        logEvent("Step Mode Switched to " + (val >= 0 ? "+" : "") + val);
    }

    private void resetCounter() {
        playClickSoundFeedback();
        int oldValue = counterValue;
        counterValue = 0;
        persistCounterState();
        updateDisplayMetrics();
        logEvent("Counter reset to 0 (Previous: " + oldValue + ")");
    }

    private void cycleTheme() {
        playClickSoundFeedback();
        currentThemeIndex = (currentThemeIndex + 1) % 4;
        persistThemeIndex();
        applyTheme();
        
        String themeName = "";
        switch (currentThemeIndex) {
            case 0: themeName = "Classic Indigo"; break;
            case 1: themeName = "Sunset Aura"; break;
            case 2: themeName = "Mint Fresh"; break;
            case 3: themeName = "Obsidian Flame"; break;
        }
        logEvent("App theme switched to: " + themeName);
    }

    private void toggleSound() {
        soundEnabled = !soundEnabled;
        persistSoundState();
        playClickSoundFeedback();
        
        btnHaptic.setText("SOUND: " + (soundEnabled ? "ON" : "OFF"));
        applyTheme();
        logEvent("Sound configuration toggled to: " + (soundEnabled ? "ENABLED" : "MUTED"));
    }

    private void updateDisplayMetrics() {
        counterDisplay.setText(String.valueOf(counterValue));
        highScoreBadge.setText("BEST: " + highScoreValue);
        
        String stepSymbol = activeStep >= 0 ? "+" : "";
        stepSizeDisplay.setText("Incrementing by " + stepSymbol + activeStep);
        btnHaptic.setText("SOUND: " + (soundEnabled ? "ON" : "OFF"));
    }

    private void applyTheme() {
        int rootBg, cardBg, textPrimary, textSecondary, activeBtnBg, inactiveBtnBg, circleInner, circleStroke, accentText;

        switch (currentThemeIndex) {
            case 0: // Classic Indigo
                rootBg = 0xFF0F172A;
                cardBg = 0xFF1E293B;
                textPrimary = 0xFFF8FAFC;
                textSecondary = 0xFF94A3B8;
                accentText = 0xFF3B82F6;
                activeBtnBg = 0xFF2563EB;
                inactiveBtnBg = 0xFF334155;
                circleInner = 0xFF1D4ED8;
                circleStroke = 0xFF60A5FA;
                break;

            case 1: // Sunset Aura
                rootBg = 0xFF1E1B4B;
                cardBg = 0xFF311042;
                textPrimary = 0xFFFFF7ED;
                textSecondary = 0xFFFDA4AF;
                accentText = 0xFFF97316;
                activeBtnBg = 0xFFBE185D;
                inactiveBtnBg = 0xFF4C1D95;
                circleInner = 0xFFEA580C;
                circleStroke = 0xFFFDBA74;
                break;

            case 2: // Mint Fresh
                rootBg = 0xFF022C22;
                cardBg = 0xFF064E3B;
                textPrimary = 0xFFF0FDFA;
                textSecondary = 0xFF5EEAD4;
                accentText = 0xFF10B981;
                activeBtnBg = 0xFF0F766E;
                inactiveBtnBg = 0xFF115E59;
                circleInner = 0xFF047857;
                circleStroke = 0xFF34D399;
                break;

            case 3: // Obsidian Flame
                rootBg = 0xFF09090B;
                cardBg = 0xFF18181B;
                textPrimary = 0xFFFAFAFA;
                textSecondary = 0xFFA1A1AA;
                accentText = 0xFFEF4444;
                activeBtnBg = 0xFFDC2626;
                inactiveBtnBg = 0xFF27272A;
                circleInner = 0xFF991B1B;
                circleStroke = 0xFFF87171;
                break;

            default:
                return;
        }

        // Apply programmatic gradients and solid backgrounds
        mainRootLayout.setBackgroundColor(rootBg);

        GradientDrawable cardShape = new GradientDrawable();
        cardShape.setColor(cardBg);
        cardShape.setCornerRadius(dpToPx(16));
        statsCard.setBackground(cardShape);

        GradientDrawable circleShape = new GradientDrawable();
        circleShape.setColor(circleInner);
        circleShape.setShape(GradientDrawable.OVAL);
        circleShape.setStroke(dpToPx(6), circleStroke);
        tapCircleButton.setBackground(circleShape);

        // Update typography colors
        appTitleText.setTextColor(textPrimary);
        appSubtitleText.setTextColor(textSecondary);
        labelTally.setTextColor(textSecondary);
        counterDisplay.setTextColor(textPrimary);
        stepSizeDisplay.setTextColor(accentText);
        tapActionLabel.setTextColor(textPrimary);
        tapActionSublabel.setTextColor(textSecondary);

        // Apply styling to all option buttons dynamically
        setButtonStyle(btnStep1, activeStep == 1 ? activeBtnBg : inactiveBtnBg);
        setButtonStyle(btnStep5, activeStep == 5 ? activeBtnBg : inactiveBtnBg);
        setButtonStyle(btnStep10, activeStep == 10 ? activeBtnBg : inactiveBtnBg);
        setButtonStyle(btnStepMinus, activeStep == -1 ? activeBtnBg : inactiveBtnBg);

        setButtonStyle(btnReset, inactiveBtnBg);
        setButtonStyle(btnTheme, activeBtnBg);
        setButtonStyle(btnHaptic, soundEnabled ? activeBtnBg : inactiveBtnBg);

        // Style the high score badge
        GradientDrawable badgeShape = new GradientDrawable();
        badgeShape.setColor(cardBg);
        badgeShape.setCornerRadius(dpToPx(8));
        badgeShape.setStroke(dpToPx(1), 0xFFF59E0B);
        highScoreBadge.setBackground(badgeShape);
    }

    private void setButtonStyle(Button btn, int color) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dpToPx(10));
        btn.setBackground(shape);
        btn.setTextColor(0xFFFFFFFF);
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

    private void playClickSoundFeedback() {
        if (soundEnabled) {
            mainRootLayout.playSoundEffect(SoundEffectConstants.CLICK);
        }
        mainRootLayout.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
    }

    private void logEvent(String msg) {
        TextView entry = new TextView(this);
        entry.setText(msg);
        entry.setTextColor(0xFFE2E8F0);
        entry.setTextSize(12f);
        entry.setPadding(0, dpToPx(5), 0, dpToPx(5));

        View divider = new View(this);
        divider.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dpToPx(1)
        ));
        divider.setBackgroundColor(0x2294A3B8);

        historyContainer.addView(divider, 0);
        historyContainer.addView(entry, 0);

        // Limit feed bounds to 40 events to save layout metrics overheads
        if (historyContainer.getChildCount() > 80) {
            historyContainer.removeViews(80, historyContainer.getChildCount() - 80);
        }

        // Auto-scroll logic upward focus
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