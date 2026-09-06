package com.moderncalculator.app;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import java.text.DecimalFormat;

public class MainActivity extends Activity {

    private TextView expressionTv;
    private TextView resultTv;
    private boolean isEvaluated = false;

    // Built-in Pure Android Tone Generator wrapper
    private static class AudioSynthesizer {
        private static ToneGenerator toneGen;

        static {
            try {
                // Initialize ToneGenerator mapped to standard media stream
                toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 85);
            } catch (Exception e) {
                // Fallback in case of hardware/audio track unavailability
            }
        }

        static void playClick() {
            if (toneGen != null) {
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 40);
            }
        }

        static void playOperator() {
            if (toneGen != null) {
                toneGen.startTone(ToneGenerator.TONE_CDMA_PIP, 50);
            }
        }

        static void playEquals() {
            if (toneGen != null) {
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 120);
            }
        }

        static void playClear() {
            if (toneGen != null) {
                toneGen.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 150);
            }
        }

        static void playStartup() {
            if (toneGen == null) return;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        toneGen.startTone(ToneGenerator.TONE_DTMF_1, 100);
                        Thread.sleep(140);
                        toneGen.startTone(ToneGenerator.TONE_DTMF_5, 100);
                        Thread.sleep(140);
                        toneGen.startTone(ToneGenerator.TONE_DTMF_9, 100);
                        Thread.sleep(140);
                        toneGen.startTone(ToneGenerator.TONE_DTMF_A, 220);
                    } catch (Exception e) {
                        // ignore interruptions
                    }
                }
            }).start();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        expressionTv = (TextView) findViewById(R.id.expression_tv);
        resultTv = (TextView) findViewById(R.id.result_tv);

        setupButtons();
        setupHistorySystem();
        setupSplashLoader();
    }

    private void setupSplashLoader() {
        final View loaderOverlay = findViewById(R.id.loader_overlay);
        
        // Play the boot audio sweep
        AudioSynthesizer.playStartup();

        // Pulsing scale animation to the logo
        ScaleAnimation pulse = new ScaleAnimation(
                1.0f, 1.04f,
                1.0f, 1.04f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        pulse.setDuration(1100);
        pulse.setRepeatMode(Animation.REVERSE);
        pulse.setRepeatCount(Animation.INFINITE);
        findViewById(R.id.loader_title).startAnimation(pulse);

        // Auto transition and fade away splash screen after 2.6 seconds
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                AlphaAnimation fadeOut = new AlphaAnimation(1.0f, 0.0f);
                fadeOut.setDuration(450);
                fadeOut.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {}

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        loaderOverlay.setVisibility(View.GONE);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {}
                });
                loaderOverlay.startAnimation(fadeOut);
            }
        }, 2600);
    }

    private void setupButtons() {
        int[] numButtons = {
            R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4,
            R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9
        };

        for (int i = 0; i < numButtons.length; i++) {
            final Button btn = (Button) findViewById(numButtons[i]);
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    applyBouncyAnimation(v);
                    AudioSynthesizer.playClick();
                    onNumberClicked(btn.getText().toString());
                }
            });
        }

        int[] opButtons = {
            R.id.btn_plus, R.id.btn_minus, R.id.btn_multiply, R.id.btn_divide
        };

        for (int i = 0; i < opButtons.length; i++) {
            final Button btn = (Button) findViewById(opButtons[i]);
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    applyBouncyAnimation(v);
                    AudioSynthesizer.playOperator();
                    onOperatorClicked(btn.getText().toString());
                }
            });
        }

        findViewById(R.id.btn_ac).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyBouncyAnimation(v);
                AudioSynthesizer.playClear();
                clearAll();
            }
        });

        findViewById(R.id.btn_backspace).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyBouncyAnimation(v);
                AudioSynthesizer.playClick();
                onBackspaceClicked();
            }
        });

        findViewById(R.id.btn_dot).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyBouncyAnimation(v);
                AudioSynthesizer.playClick();
                onDotClicked();
            }
        });

        findViewById(R.id.btn_percent).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyBouncyAnimation(v);
                AudioSynthesizer.playOperator();
                onPercentClicked();
            }
        });

        findViewById(R.id.btn_toggle).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyBouncyAnimation(v);
                AudioSynthesizer.playOperator();
                onToggleClicked();
            }
        });

        findViewById(R.id.btn_equals).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyBouncyAnimation(v);
                AudioSynthesizer.playEquals();
                onEqualsClicked();
            }
        });
    }

    private void setupHistorySystem() {
        findViewById(R.id.btn_open_history).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyBouncyAnimation(v);
                AudioSynthesizer.playClick();
                showHistoryPanel();
            }
        });

        findViewById(R.id.btn_close_history).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyBouncyAnimation(v);
                AudioSynthesizer.playClick();
                hideHistoryPanel();
            }
        });

        findViewById(R.id.btn_clear_history).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyBouncyAnimation(v);
                AudioSynthesizer.playClear();
                clearHistory();
            }
        });
    }

    // Programmatic Bouncy scale feedback animation on views
    private void applyBouncyAnimation(View view) {
        ScaleAnimation anim = new ScaleAnimation(
                1.0f, 0.88f,
                1.0f, 0.88f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        anim.setDuration(70);
        anim.setRepeatMode(Animation.REVERSE);
        anim.setRepeatCount(1);
        view.startAnimation(anim);
    }

    private void showHistoryPanel() {
        final View panel = findViewById(R.id.history_panel);
        populateHistoryList();
        panel.setVisibility(View.VISIBLE);

        // Slide dropdown curtains visual animation
        TranslateAnimation slideIn = new TranslateAnimation(0, 0, -1000, 0);
        slideIn.setDuration(280);
        panel.startAnimation(slideIn);
    }

    private void hideHistoryPanel() {
        final View panel = findViewById(R.id.history_panel);
        TranslateAnimation slideOut = new TranslateAnimation(0, 0, 0, -1000);
        slideOut.setDuration(220);
        slideOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                panel.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        panel.startAnimation(slideOut);
    }

    private void saveCalculationToHistory(String expression, String result) {
        if (expression.isEmpty() || result.isEmpty() || result.equals("Error")) {
            return;
        }

        SharedPreferences prefs = getSharedPreferences("calc_history_data", MODE_PRIVATE);
        String currentHistory = prefs.getString("items", "");
        String newEntry = expression + " = " + result;

        if (currentHistory.isEmpty()) {
            currentHistory = newEntry;
        } else {
            currentHistory = newEntry + "##" + currentHistory;
        }

        // Cap list size at 50 records
        String[] parts = currentHistory.split("##");
        if (parts.length > 50) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 50; i++) {
                if (i > 0) sb.append("##");
                sb.append(parts[i]);
            }
            currentHistory = sb.toString();
        }

        prefs.edit().putString("items", currentHistory).apply();
    }

    private void populateHistoryList() {
        LinearLayout container = (LinearLayout) findViewById(R.id.history_list_container);
        container.removeAllViews();

        SharedPreferences prefs = getSharedPreferences("calc_history_data", MODE_PRIVATE);
        String rawHistory = prefs.getString("items", "");

        if (rawHistory.isEmpty()) {
            TextView emptyTv = new TextView(this);
            emptyTv.setText("Your computation history is empty.");
            emptyTv.setTextColor(0xFF747477);
            emptyTv.setTextSize(15);
            emptyTv.setGravity(Gravity.CENTER);
            emptyTv.setPadding(0, 60, 0, 60);
            container.addView(emptyTv);
            return;
        }

        String[] items = rawHistory.split("##");
        for (int i = 0; i < items.length; i++) {
            final String record = items[i];

            LinearLayout itemLayout = new LinearLayout(this);
            itemLayout.setOrientation(LinearLayout.VERTICAL);
            itemLayout.setPadding(16, 16, 16, 16);

            GradientDrawable backgroundShape = new GradientDrawable();
            backgroundShape.setColor(0x13FFFFFF);
            backgroundShape.setCornerRadius(16);
            itemLayout.setBackground(backgroundShape);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            lp.setMargins(0, 0, 0, 10);
            itemLayout.setLayoutParams(lp);

            TextView historyTv = new TextView(this);
            historyTv.setText(record);
            historyTv.setTextColor(0xFFFFFFFF);
            historyTv.setTextSize(16);
            historyTv.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            itemLayout.addView(historyTv);

            itemLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    applyBouncyAnimation(v);
                    AudioSynthesizer.playClick();

                    int splitIdx = record.indexOf("=");
                    if (splitIdx != -1) {
                        String savedExpression = record.substring(0, splitIdx).trim();
                        expressionTv.setText(savedExpression);
                        isEvaluated = false;
                        updateRealTimeResult();
                        hideHistoryPanel();
                    }
                }
            });

            container.addView(itemLayout);
        }
    }

    private void clearHistory() {
        getSharedPreferences("calc_history_data", MODE_PRIVATE).edit().clear().apply();
        populateHistoryList();
    }

    private void onNumberClicked(String number) {
        if (isEvaluated) {
            expressionTv.setText("");
            isEvaluated = false;
        }
        String current = expressionTv.getText().toString();
        if (current.equals("0")) {
            expressionTv.setText(number);
        } else {
            expressionTv.append(number);
        }
        updateRealTimeResult();
    }

    private void onOperatorClicked(String op) {
        isEvaluated = false;
        String current = expressionTv.getText().toString();
        if (current.isEmpty()) {
            if (op.equals("-")) {
                expressionTv.append(op);
            }
            return;
        }

        char lastChar = current.charAt(current.length() - 1);
        if (isOperator(lastChar)) {
            expressionTv.setText(current.substring(0, current.length() - 1) + op);
        } else {
            expressionTv.append(op);
        }
    }

    private boolean isOperator(char c) {
        return c == '+' || c == '-' || c == '×' || c == '÷' || c == '*' || c == '/';
    }

    private void onDotClicked() {
        if (isEvaluated) {
            expressionTv.setText("0");
            isEvaluated = false;
        }
        String current = expressionTv.getText().toString();
        if (current.isEmpty()) {
            expressionTv.append("0.");
            return;
        }

        int lastOpIdx = -1;
        for (int i = current.length() - 1; i >= 0; i--) {
            if (isOperator(current.charAt(i))) {
                lastOpIdx = i;
                break;
            }
        }

        String lastNumberSegment = current.substring(lastOpIdx + 1);
        if (!lastNumberSegment.contains(".")) {
            expressionTv.append(".");
        }
    }

    private void onPercentClicked() {
        if (isEvaluated) {
            isEvaluated = false;
        }
        String current = expressionTv.getText().toString();
        if (!current.isEmpty()) {
            char lastChar = current.charAt(current.length() - 1);
            if (!isOperator(lastChar) && lastChar != '%') {
                expressionTv.append("%");
                updateRealTimeResult();
            }
        }
    }

    private void onToggleClicked() {
        if (isEvaluated) {
            isEvaluated = false;
        }
        String exp = expressionTv.getText().toString();
        if (exp.isEmpty()) {
            expressionTv.setText("-");
            return;
        }

        int i = exp.length() - 1;
        while (i >= 0 && (Character.isDigit(exp.charAt(i)) || exp.charAt(i) == '.' || exp.charAt(i) == '%')) {
            i--;
        }

        if (i >= 0 && exp.charAt(i) == '-') {
            boolean isUnaryMinus = false;
            if (i == 0) {
                isUnaryMinus = true;
            } else {
                char prev = exp.charAt(i - 1);
                if (isOperator(prev) || prev == '(') {
                    isUnaryMinus = true;
                }
            }
            if (isUnaryMinus) {
                expressionTv.setText(exp.substring(0, i) + exp.substring(i + 1));
                updateRealTimeResult();
                return;
            }
        }

        expressionTv.setText(exp.substring(0, i + 1) + "-" + exp.substring(i + 1));
        updateRealTimeResult();
    }

    private void onBackspaceClicked() {
        if (isEvaluated) {
            clearAll();
            return;
        }
        String current = expressionTv.getText().toString();
        if (!current.isEmpty()) {
            expressionTv.setText(current.substring(0, current.length() - 1));
            updateRealTimeResult();
        }
    }

    private void clearAll() {
        expressionTv.setText("");
        resultTv.setText("0");
        isEvaluated = false;
    }

    private void onEqualsClicked() {
        String current = expressionTv.getText().toString();
        if (current.isEmpty()) return;

        try {
            double rawResult = eval(current);
            String formatted = formatResult(rawResult);
            
            // Save this expression to persistent storage log
            saveCalculationToHistory(current, formatted);
            
            resultTv.setText(formatted);
            expressionTv.setText(formatted);
            isEvaluated = true;
        } catch (Exception e) {
            resultTv.setText("Error");
        }
    }

    private void updateRealTimeResult() {
        String current = expressionTv.getText().toString();
        if (current.isEmpty()) {
            resultTv.setText("0");
            return;
        }

        String cleanExp = current;
        while (!cleanExp.isEmpty() && isOperator(cleanExp.charAt(cleanExp.length() - 1))) {
            cleanExp = cleanExp.substring(0, cleanExp.length() - 1);
        }

        if (cleanExp.isEmpty()) {
            resultTv.setText("0");
            return;
        }

        try {
            double rawResult = eval(cleanExp);
            resultTv.setText(formatResult(rawResult));
        } catch (Exception e) {
            // Silence evaluation anomalies during standard expression composition
        }
    }

    private String formatResult(double value) {
        if (Double.isInfinite(value) || Double.isNaN(value)) {
            return "Error";
        }
        if (value == (long) value) {
            return String.format("%d", (long) value);
        } else {
            DecimalFormat df = new DecimalFormat("#.########");
            return df.format(value);
        }
    }

    public static double eval(final String str) {
        return new Object() {
            int pos = -1, ch;

            void nextChar() {
                ch = (++pos < str.length()) ? str.charAt(pos) : -1;
            }

            boolean eat(int charToEat) {
                while (ch == ' ') nextChar();
                if (ch == charToEat) {
                    nextChar();
                    return true;
                }
                return false;
            }

            double parse() {
                nextChar();
                double x = parseExpression();
                if (pos < str.length()) throw new RuntimeException("Unexpected character: " + (char)ch);
                return x;
            }

            double parseExpression() {
                double x = parseTerm();
                for (;;) {
                    if      (eat('+')) x += parseTerm();
                    else if (eat('-')) x -= parseTerm();
                    else return x;
                }
            }

            double parseTerm() {
                double x = parseFactor();
                for (;;) {
                    if      (eat('×') || eat('*')) x *= parseFactor();
                    else if (eat('÷') || eat('/')) {
                        double divisor = parseFactor();
                        if (divisor == 0) throw new ArithmeticException("Division by zero");
                        x /= divisor;
                    }
                    else return x;
                }
            }

            double parseFactor() {
                if (eat('+')) return parseFactor();
                if (eat('-')) return -parseFactor();

                double x;
                int startPos = this.pos;
                if (eat('(')) {
                    x = parseExpression();
                    eat(')');
                } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(str.substring(startPos, this.pos));
                } else {
                    throw new RuntimeException("Unexpected: " + (char)ch);
                }

                if (eat('%')) x = x / 100.0;

                return x;
            }
        }.parse();
    }
}