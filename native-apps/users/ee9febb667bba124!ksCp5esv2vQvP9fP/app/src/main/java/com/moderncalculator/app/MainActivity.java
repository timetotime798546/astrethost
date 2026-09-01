package com.moderncalculator.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private LinearLayout rootLayout;
    private TextView tvFormula;
    private TextView tvResult;
    private Button btnSoundToggle;
    private Button btnHistory;

    private StringBuilder currentExpression = new StringBuilder();
    private boolean isResultDisplayed = false;
    private boolean isSoundEnabled = true;
    private ToneGenerator toneGenerator;

    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "calc_prefs";
    private static final String KEY_SOUND = "sound_enabled";
    private static final String KEY_HISTORY = "calc_history";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Hide Action Bar if present for a clean premium layout
        if (getActionBar() != null) {
            getActionBar().hide();
        }

        setContentView(R.layout.activity_main);

        // Initialize premium 4-color gradient on the root layout
        rootLayout = (LinearLayout) findViewById(R.id.root_layout);
        applyFourColorGradient();

        // Initialize displays
        tvFormula = (TextView) findViewById(R.id.tv_formula);
        tvResult = (TextView) findViewById(R.id.tv_result);
        btnSoundToggle = (Button) findViewById(R.id.btn_sound_toggle);
        btnHistory = (Button) findViewById(R.id.btn_history);

        // Load preferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        isSoundEnabled = sharedPreferences.getBoolean(KEY_SOUND, true);
        updateSoundToggleButton();

        // Initialize tone synthesis
        try {
            toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 75);
        } catch (Exception e) {
            toneGenerator = null;
        }

        // Assign calculator click triggers
        setupButtonListeners();
    }

    private void applyFourColorGradient() {
        // Premium 4-color palette designed for maximum depth and futuristic look
        int[] colors = new int[] {
            Color.parseColor("#0A0915"), // Dark Obsidian
            Color.parseColor("#16132D"), // Deep Midnight Purple
            Color.parseColor("#23163E"), // Intense Cosmic Amethyst
            Color.parseColor("#0E1A2E")  // Slate Navy Blue
        };

        // Create gradient from Top-Left to Bottom-Right
        GradientDrawable gradientDrawable = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            colors
        );
        gradientDrawable.setCornerRadius(0f);
        rootLayout.setBackground(gradientDrawable);
    }

    private void playTactileSound() {
        if (isSoundEnabled && toneGenerator != null) {
            try {
                toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 35);
            } catch (Exception ignored) {}
        }
    }

    private void updateSoundToggleButton() {
        btnSoundToggle.setText(isSoundEnabled ? "🔊" : "🔇");
    }

    private void setupButtonListeners() {
        // Sound Toggle Button
        btnSoundToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isSoundEnabled = !isSoundEnabled;
                sharedPreferences.edit().putBoolean(KEY_SOUND, isSoundEnabled).apply();
                updateSoundToggleButton();
                playTactileSound();
            }
        });

        // History Button
        btnHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playTactileSound();
                showHistoryDialog();
            }
        });

        // Math Buttons
        int[] numberButtons = {
            R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4,
            R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9
        };

        for (int id : numberButtons) {
            final Button button = (Button) findViewById(id);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    playTactileSound();
                    appendCharacter(button.getText().toString());
                }
            });
        }

        // Operations
        int[] opButtons = { R.id.btn_add, R.id.btn_sub, R.id.btn_mul, R.id.btn_div, R.id.btn_dot, R.id.btn_percent };
        for (int id : opButtons) {
            final Button button = (Button) findViewById(id);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    playTactileSound();
                    appendOperator(button.getText().toString());
                }
            });
        }

        // Clear
        findViewById(R.id.btn_clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playTactileSound();
                clearAll();
            }
        });

        // Delete
        findViewById(R.id.btn_delete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playTactileSound();
                deleteLast();
            }
        });

        // Plus-Minus Toggle
        findViewById(R.id.btn_plus_minus).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playTactileSound();
                togglePlusMinus();
            }
        });

        // Equals
        findViewById(R.id.btn_equal).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playTactileSound();
                performCalculation();
            }
        });
    }

    private void appendCharacter(String character) {
        if (isResultDisplayed) {
            currentExpression.setLength(0);
            isResultDisplayed = false;
        }
        currentExpression.append(character);
        tvFormula.setText(currentExpression.toString());
    }

    private void appendOperator(String op) {
        if (currentExpression.length() == 0) {
            if (op.equals("-")) {
                currentExpression.append(op);
                tvFormula.setText(currentExpression.toString());
            }
            return;
        }

        char last = currentExpression.charAt(currentExpression.length() - 1);
        if (isOperator(last)) {
            // Replace the last operator with the new one
            currentExpression.setLength(currentExpression.length() - 1);
        }
        isResultDisplayed = false;
        currentExpression.append(op);
        tvFormula.setText(currentExpression.toString());
    }

    private boolean isOperator(char ch) {
        return ch == '+' || ch == '-' || ch == '×' || ch == '÷' || ch == '%' || ch == '.';
    }

    private void clearAll() {
        currentExpression.setLength(0);
        tvFormula.setText("");
        tvResult.setText("0");
        isResultDisplayed = false;
    }

    private void deleteLast() {
        if (currentExpression.length() > 0) {
            currentExpression.setLength(currentExpression.length() - 1);
            tvFormula.setText(currentExpression.toString());
        }
    }

    private void togglePlusMinus() {
        if (currentExpression.length() == 0) return;
        
        String expr = currentExpression.toString();
        if (expr.startsWith("-")) {
            currentExpression = new StringBuilder(expr.substring(1));
        } else {
            currentExpression = new StringBuilder("-").append(expr);
        }
        tvFormula.setText(currentExpression.toString());
    }

    private void performCalculation() {
        String expression = currentExpression.toString();
        if (expression.isEmpty()) return;

        try {
            // Parse and calculate
            double resultValue = eval(expression);
            String resultStr;
            if (resultValue == (long) resultValue) {
                resultStr = String.valueOf((long) resultValue);
            } else {
                resultStr = String.valueOf(resultValue);
            }

            tvResult.setText(resultStr);
            saveToHistory(expression + " = " + resultStr);
            isResultDisplayed = true;
        } catch (Exception e) {
            tvResult.setText("Error");
            isResultDisplayed = true;
        }
    }

    private void saveToHistory(String equation) {
        String existingHistory = sharedPreferences.getString(KEY_HISTORY, "");
        String newHistory;
        if (existingHistory.isEmpty()) {
            newHistory = equation;
        } else {
            newHistory = equation + "\n" + existingHistory;
        }
        sharedPreferences.edit().putString(KEY_HISTORY, newHistory).apply();
    }

    private void showHistoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_history, null);
        builder.setView(dialogView);

        final AlertDialog dialog = builder.create();

        final TextView tvHistoryContent = (TextView) dialogView.findViewById(R.id.tv_history_content);
        Button btnClearHistory = (Button) dialogView.findViewById(R.id.btn_clear_history);
        Button btnCloseHistory = (Button) dialogView.findViewById(R.id.btn_close_history);

        String historyData = sharedPreferences.getString(KEY_HISTORY, "");
        if (historyData.isEmpty()) {
            tvHistoryContent.setText("No calculation history saved yet.");
        } else {
            tvHistoryContent.setText(historyData);
        }

        btnClearHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playTactileSound();
                sharedPreferences.edit().putString(KEY_HISTORY, "").apply();
                tvHistoryContent.setText("No calculation history saved yet.");
                Toast.makeText(MainActivity.this, "History cleared", Toast.LENGTH_SHORT).show();
            }
        });

        btnCloseHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playTactileSound();
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    // Recursive descent parser compatible with Java 8
    private static double eval(final String str) {
        return new Object() {
            int pos = -1, ch;

            void nextChar() {
                ch = (++pos < str.length()) ? str.charAt(pos) : -1;
            }

            boolean consume(int charToConsume) {
                while (ch == ' ') nextChar();
                if (ch == charToConsume) {
                    nextChar();
                    return true;
                }
                return false;
            }

            double parse() {
                nextChar();
                double x = parseExpression();
                if (pos < str.length()) throw new RuntimeException("Unexpected: " + (char)ch);
                return x;
            }

            double parseExpression() {
                double x = parseTerm();
                for (;;) {
                    if      (consume('+')) x += parseTerm(); 
                    else if (consume('-')) x -= parseTerm(); 
                    else return x;
                }
            }

            double parseTerm() {
                double x = parseFactor();
                for (;;) {
                    if      (consume('×') || consume('*')) x *= parseFactor(); 
                    else if (consume('÷') || consume('/')) {
                        double divider = parseFactor();
                        if (divider == 0) throw new ArithmeticException("Division by zero");
                        x /= divider;
                    } else {
                        return x;
                    }
                }
            }

            double parseFactor() {
                if (consume('+')) return parseFactor(); 
                if (consume('-')) return -parseFactor(); 

                double x;
                int startPos = this.pos;
                if (consume('(')) { 
                    x = parseExpression();
                    consume(')');
                } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(str.substring(startPos, this.pos));
                } else if (consume('%')) {
                    x = 0; // percentage handle
                } else {
                    throw new RuntimeException("Unexpected: " + (char)ch);
                }

                if (consume('%')) {
                    x = x / 100.0;
                }

                return x;
            }
        }.parse();
    }

    @Override
    protected void onDestroy() {
        if (toneGenerator != null) {
            toneGenerator.release();
        }
        super.onDestroy();
    }
}