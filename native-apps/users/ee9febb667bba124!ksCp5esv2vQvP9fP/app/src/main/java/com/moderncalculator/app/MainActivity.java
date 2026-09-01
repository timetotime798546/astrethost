package com.moderncalculator.app;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {

    private TextView tvFormula;
    private TextView tvResult;
    private Button btnSoundToggle;
    private Button btnHistory;
    private View ledSound;

    private String formulaString = "";
    private ToneGenerator toneGenerator;
    private boolean soundEnabled = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) { 
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Instantly parse preferences to configure initial volume/sound switch state
        SharedPreferences prefs = getSharedPreferences("CalculatorPrefs", MODE_PRIVATE);
        soundEnabled = prefs.getBoolean("sound_enabled", true);

        // 2. Safely initialize ToneGenerator at startup so loading sounds can play instantly
        try {
            toneGenerator = new ToneGenerator(AudioManager.STREAM_SYSTEM, 70);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 3. Initiate rhythmic futuristic boot sound series alongside the loader layout
        playLoaderSoundSequence();

        // 4. Register Handler to dismiss the system loader layout after exactly 2.2 seconds
        final View loaderOverlay = findViewById(R.id.loader_overlay);
        if (loaderOverlay != null) {
            new android.os.Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    loaderOverlay.setVisibility(View.GONE);
                }
            }, 2200);
        }

        tvFormula = (TextView) findViewById(R.id.tv_formula);
        tvResult = (TextView) findViewById(R.id.tv_result);
        btnSoundToggle = (Button) findViewById(R.id.btn_sound_toggle);
        btnHistory = (Button) findViewById(R.id.btn_history);
        ledSound = findViewById(R.id.led_sound);

        updateSoundUI();
        setupNumericAndOperatorButtons();

        findViewById(R.id.btn_clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playSound();
                formulaString = "";
                updateDisplay();
            }
        });

        findViewById(R.id.btn_delete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playSound();
                if (formulaString.length() > 0) {
                    formulaString = formulaString.substring(0, formulaString.length() - 1);
                }
                updateDisplay();
            }
        });

        findViewById(R.id.btn_plus_minus).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playSound();
                toggleLastNumberSign();
                updateDisplay();
            }
        });

        findViewById(R.id.btn_equal).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playSound();
                onEqualPressed();
            }
        });

        btnSoundToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleSound();
                playSound();
            }
        });

        btnHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playSound();
                showHistoryDialog();
            }
        });
    }

    /**
     * Creates a sequential melodic loader audio chime sequence using nested delay handlers.
     */
    private void playLoaderSoundSequence() {
        if (!soundEnabled || toneGenerator == null) return;
        try {
            // First beep: system powers on
            toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 100);

            // Second beep (after 300ms): memory diagnostic sound 1
            new android.os.Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (soundEnabled && toneGenerator != null) {
                        try {
                            toneGenerator.startTone(ToneGenerator.TONE_DTMF_3, 80);
                        } catch (Exception ignored) {}
                    }
                }
            }, 300); 

            // Third beep (after 600ms): memory diagnostic sound 2
            new android.os.Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (soundEnabled && toneGenerator != null) {
                        try {
                            toneGenerator.startTone(ToneGenerator.TONE_DTMF_6, 80);
                        } catch (Exception ignored) {}
                    }
                }
            }, 600);

            // Fourth beep (after 900ms): memory diagnostic sound 3
            new android.os.Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (soundEnabled && toneGenerator != null) {
                        try {
                            toneGenerator.startTone(ToneGenerator.TONE_DTMF_9, 80);
                        } catch (Exception ignored) {}
                    }
                }
            }, 900);

            // Fifth beep (after 1200ms): console signaling interface active
            new android.os.Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (soundEnabled && toneGenerator != null) {
                        try {
                            toneGenerator.startTone(ToneGenerator.TONE_DTMF_A, 80);
                        } catch (Exception ignored) {}
                    }
                }
            }, 1200);

            // Completion chime (after 2200ms): system fully operational and loader layout dismisses
            new android.os.Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (soundEnabled && toneGenerator != null) {
                        try {
                            toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 250);
                        } catch (Exception ignored) {}
                    }
                }
            }, 2200);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateSoundUI() {
        if (btnSoundToggle != null) {
            btnSoundToggle.setText(soundEnabled ? "🔊" : "🔇");
        }
        if (ledSound != null) {
            ledSound.setBackgroundResource(soundEnabled ? R.drawable.led_indicator_on : R.drawable.led_indicator_off);
        }
    }

    private void setupNumericAndOperatorButtons() {
        int[] ids = {
            R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4,
            R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9,
            R.id.btn_dot, R.id.btn_percent
        };

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playSound();
                Button b = (Button) v;
                formulaString += b.getText().toString();
                updateDisplay();
            }
        };

        for (int i = 0; i < ids.length; i++) {
            View view = findViewById(ids[i]);
            if (view != null) {
                view.setOnClickListener(listener);
            }
        }

        int[] opIds = { R.id.btn_add, R.id.btn_sub, R.id.btn_mul, R.id.btn_div };
        View.OnClickListener opListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playSound();
                Button b = (Button) v;
                addOperator(b.getText().toString());
                updateDisplay();
            }
        };

        for (int i = 0; i < opIds.length; i++) {
            View view = findViewById(opIds[i]);
            if (view != null) {
                view.setOnClickListener(opListener);
            }
        }
    }

    private void addOperator(String op) {
        if (formulaString.isEmpty()) {
            if (op.equals("-")) {
                formulaString += op;
            }
            return;
        }
        char lastChar = formulaString.charAt(formulaString.length() - 1);
        if ("+-×÷/*".indexOf(lastChar) != -1) {
            formulaString = formulaString.substring(0, formulaString.length() - 1) + op;
        } else {
            formulaString += op;
        }
    }

    private void toggleLastNumberSign() {
        if (formulaString.isEmpty()) return;
        int len = formulaString.length();
        int i = len - 1;
        while (i >= 0 && (Character.isDigit(formulaString.charAt(i)) || formulaString.charAt(i) == '.')) {
            i--;
        }
        if (i >= 0 && formulaString.charAt(i) == '-') {
            boolean isUnary = (i == 0) || "+-×÷(".indexOf(formulaString.charAt(i - 1)) != -1;
            if (isUnary) {
                formulaString = formulaString.substring(0, i) + formulaString.substring(i + 1);
                return;
            }
        }
        formulaString = formulaString.substring(0, i + 1) + "-" + formulaString.substring(i + 1);
    }

    private void updateDisplay() {
        tvFormula.setText(formulaString);
        if (formulaString.isEmpty()) {
            tvResult.setText("0");
        } else {
            try {
                double res = evaluate(formulaString);
                tvResult.setText(formatResult(res));
            } catch (Exception e) {
                // Continue preserving existing display flow
            }
        }
    }

    private void onEqualPressed() {
        if (formulaString.isEmpty()) return;
        try {
            double res = evaluate(formulaString);
            String formatted = formatResult(res);
            saveToHistory(formulaString, formatted);
            formulaString = formatted;
            tvFormula.setText("");
            tvResult.setText(formatted);
        } catch (Exception e) {
            tvResult.setText("Error");
        }
    }

    private void saveToHistory(String expression, String result) {
        SharedPreferences prefs = getSharedPreferences("CalculatorPrefs", MODE_PRIVATE);
        String currentHistory = prefs.getString("history", "");
        String entry = expression + " = " + result + "\n";
        String updatedHistory = entry + currentHistory;
        prefs.edit().putString("history", updatedHistory).apply();
    }

    private void clearHistoryInPrefs() {
        SharedPreferences prefs = getSharedPreferences("CalculatorPrefs", MODE_PRIVATE);
        prefs.edit().putString("history", "").apply();
    }

    private void toggleSound() {
        soundEnabled = !soundEnabled;
        SharedPreferences prefs = getSharedPreferences("CalculatorPrefs", MODE_PRIVATE);
        prefs.edit().putBoolean("sound_enabled", soundEnabled).apply();
        updateSoundUI();
    }

    private void playSound() {
        if (soundEnabled && toneGenerator != null) {
            try {
                toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 50);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void showHistoryDialog() {
        final Dialog dialog = new Dialog(this, android.R.style.Theme_Material_NoActionBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_history);

        final LinearLayout container = (LinearLayout) dialog.findViewById(R.id.history_container);
        Button btnClear = (Button) dialog.findViewById(R.id.btn_clear_history);
        Button btnClose = (Button) dialog.findViewById(R.id.btn_close_history);

        SharedPreferences prefs = getSharedPreferences("CalculatorPrefs", MODE_PRIVATE);
        String rawHistory = prefs.getString("history", "");
        container.removeAllViews();

        if (rawHistory.trim().isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("No history yet");
            tvEmpty.setTextColor(0xFF8E8E93);
            tvEmpty.setTextSize(18);
            tvEmpty.setGravity(android.view.Gravity.CENTER);
            tvEmpty.setPadding(0, 50, 0, 0);
            container.addView(tvEmpty);
        } else {
            String[] items = rawHistory.split("\n");
            for (int i = 0; i < items.length; i++) {
                final String item = items[i];
                if (item.trim().isEmpty()) continue;

                LinearLayout itemLayout = new LinearLayout(this);
                itemLayout.setOrientation(LinearLayout.VERTICAL);
                itemLayout.setPadding(16, 16, 16, 16);

                View divider = new View(this);
                divider.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1));
                divider.setBackgroundColor(0xFF2C2C2E);

                TextView tvEntry = new TextView(this);
                tvEntry.setText(item);
                tvEntry.setTextColor(0xFFFFFFFF);
                tvEntry.setTextSize(18);

                itemLayout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        playSound();
                        String[] parts = item.split("=");
                        if (parts.length > 0) {
                            formulaString = parts[0].trim();
                            updateDisplay();
                        }
                        dialog.dismiss();
                    }
                });

                itemLayout.addView(tvEntry);
                container.addView(itemLayout);
                container.addView(divider);
            } 
        }

        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playSound();
                clearHistoryInPrefs();
                container.removeAllViews();
                TextView tvEmpty = new TextView(MainActivity.this);
                tvEmpty.setText("No history yet");
                tvEmpty.setTextColor(0xFF8E8E93);
                tvEmpty.setTextSize(18);
                tvEmpty.setGravity(android.view.Gravity.CENTER);
                tvEmpty.setPadding(0, 50, 0, 0);
                container.addView(tvEmpty);
            }
        });

        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playSound();
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private String formatResult(double val) {
        if (val == (long) val) {
            return String.format("%d", (long) val);
        } else {
            return String.valueOf(val);
        }
    }

    public static double evaluate(String expression) {
        final String exp = expression.replace("×", "*").replace("÷", "/");
        return new Object() {
            int pos = -1, ch;

            void nextChar() {
                ch = (++pos < exp.length()) ? exp.charAt(pos) : -1;
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
                if (pos < exp.length()) throw new RuntimeException("Unexpected: " + (char)ch);
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
                    if      (eat('*')) x *= parseFactor();
                    else if (eat('/')) {
                        double divisor = parseFactor();
                        if (divisor == 0) throw new ArithmeticException("Divide by zero");
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
                    x = Double.parseDouble(exp.substring(startPos, this.pos));
                } else {
                    throw new RuntimeException("Unexpected: " + (char)ch);
                }

                if (eat('%')) x = x / 100.0;

                return x;
            }
        }.parse();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (toneGenerator != null) {
            toneGenerator.release();
        }
    }
}