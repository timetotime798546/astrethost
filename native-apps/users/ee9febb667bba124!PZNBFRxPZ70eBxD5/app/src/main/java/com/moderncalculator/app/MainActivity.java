package com.moderncalculator.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.LinearLayout;
import java.text.DecimalFormat;
import java.util.ArrayList;
import android.media.ToneGenerator;
import android.media.AudioManager;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;

public class MainActivity extends Activity {

    private TextView tvExpression;
    private TextView tvResult;
    private boolean isResultShown = false;

    // Feedback & Theme additions
    private ToneGenerator toneGenerator;
    private ArrayList<String> historyList;
    private TextView tvHistoryList;
    private int currentThemeIndex = 0;
    private AppTheme[] themes;

    // Hold colors for runtime adjustments
    static class AppTheme {
        String name;
        int bgColor;
        int headerColor;
        int displayTextColor;
        int displaySubTextColor;
        int historyBgColor;
        int historyTextColor;
        int numBtnColor;
        int numTextColor;
        int opBtnColor;
        int opTextColor;
        int actBtnColor;
        int actTextColor;
        int eqBtnColor;
        int eqTextColor;

        AppTheme(String name, int bgColor, int headerColor, int displayTextColor, int displaySubTextColor,
                 int historyBgColor, int historyTextColor,
                 int numBtnColor, int numTextColor, int opBtnColor, int opTextColor,
                 int actBtnColor, int actTextColor, int eqBtnColor, int eqTextColor) {
            this.name = name;
            this.bgColor = bgColor;
            this.headerColor = headerColor;
            this.displayTextColor = displayTextColor;
            this.displaySubTextColor = displaySubTextColor;
            this.historyBgColor = historyBgColor;
            this.historyTextColor = historyTextColor;
            this.numBtnColor = numBtnColor;
            this.numTextColor = numTextColor;
            this.opBtnColor = opBtnColor;
            this.opTextColor = opTextColor;
            this.actBtnColor = actBtnColor;
            this.actTextColor = actTextColor;
            this.eqBtnColor = eqBtnColor;
            this.eqTextColor = eqTextColor;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getActionBar() != null) {
            getActionBar().hide();
        }

        // Initialize Sound Effects Generator
        try {
            toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 65);
        } catch (Exception e) {
            toneGenerator = null;
        }

        historyList = new ArrayList<String>();

        tvExpression = (TextView) findViewById(R.id.tvExpression);
        tvResult = (TextView) findViewById(R.id.tvResult);
        tvHistoryList = (TextView) findViewById(R.id.tvHistoryList);

        // Main Keyboards
        Button btnClear = (Button) findViewById(R.id.btnClear);
        Button btnBracket = (Button) findViewById(R.id.btnBracket);
        Button btnPercent = (Button) findViewById(R.id.btnPercent);
        Button btnDivide = (Button) findViewById(R.id.btnDivide);
        Button btn7 = (Button) findViewById(R.id.btn7);
        Button btn8 = (Button) findViewById(R.id.btn8);
        Button btn9 = (Button) findViewById(R.id.btn9);
        Button btnMultiply = (Button) findViewById(R.id.btnMultiply);
        Button btn4 = (Button) findViewById(R.id.btn4);
        Button btn5 = (Button) findViewById(R.id.btn5);
        Button btn6 = (Button) findViewById(R.id.btn6);
        Button btnSubtract = (Button) findViewById(R.id.btnSubtract);
        Button btn1 = (Button) findViewById(R.id.btn1);
        Button btn2 = (Button) findViewById(R.id.btn2);
        Button btn3 = (Button) findViewById(R.id.btn3);
        Button btnAdd = (Button) findViewById(R.id.btnAdd);
        Button btnDelete = (Button) findViewById(R.id.btnDelete);
        Button btn0 = (Button) findViewById(R.id.btn0);
        Button btnDot = (Button) findViewById(R.id.btnDot);
        Button btnEqual = (Button) findViewById(R.id.btnEqual);

        // Header Buttons
        Button btnHistoryToggle = (Button) findViewById(R.id.btnHistoryToggle);
        Button btnThemeCycle = (Button) findViewById(R.id.btnThemeCycle);
        Button btnClearHistory = (Button) findViewById(R.id.btnClearHistory);

        View.OnClickListener numListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playClickSound();
                Button b = (Button) v;
                appendToken(b.getText().toString(), false);
            }
        };

        btn0.setOnClickListener(numListener);
        btn1.setOnClickListener(numListener);
        btn2.setOnClickListener(numListener);
        btn3.setOnClickListener(numListener);
        btn4.setOnClickListener(numListener);
        btn5.setOnClickListener(numListener);
        btn6.setOnClickListener(numListener);
        btn7.setOnClickListener(numListener);
        btn8.setOnClickListener(numListener);
        btn9.setOnClickListener(numListener);

        View.OnClickListener opListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playClickSound();
                Button b = (Button) v;
                appendToken(b.getText().toString(), true);
            }
        };

        btnAdd.setOnClickListener(opListener);
        btnSubtract.setOnClickListener(opListener);
        btnMultiply.setOnClickListener(opListener);
        btnDivide.setOnClickListener(opListener);

        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playClickSound();
                tvExpression.setText("");
                tvResult.setText("0");
                isResultShown = false;
            }
        });

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playClickSound();
                handleDelete();
            }
        });

        btnBracket.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playClickSound();
                handleBracket();
            }
        });

        btnPercent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playClickSound();
                appendToken("%", true);
            }
        });

        btnDot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playClickSound();
                appendDot();
            }
        });

        btnEqual.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playClickSound();
                calculateFinalResult();
            }
        });

        // History toggler with animations
        btnHistoryToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playClickSound();
                toggleHistoryPanel();
            }
        });

        // Theme Cycle selector click
        btnThemeCycle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playClickSound();
                cycleTheme();
            }
        });

        // Clear History implementation
        btnClearHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playClickSound();
                historyList.clear();
                updateHistoryView();
            }
        });

        // Setup dynamic themes array
        initThemes();
        applyTheme(themes[0]);
    }

    private void playClickSound() {
        if (toneGenerator != null) {
            try {
                toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 35);
            } catch (Exception e) {
                // Suppress sound failures
            }
        } 
    }

    private void initThemes() {
        themes = new AppTheme[] {
            // 1. Classic Light Theme
            new AppTheme("Classic Light", 0xFFF8F9FA, 0xFFECEFF1, 0xFF1F1F1F, 0xFF888888, 0xFFFFFFFF, 0xFF37474F, 0xFFFFFFFF, 0xFF212121, 0xFFFF9800, 0xFFFFFFFF, 0xFFECEFF1, 0xFF37474F, 0xFF4CAF50, 0xFFFFFFFF),
            // 2. Slate Dark Theme
            new AppTheme("Slate Dark", 0xFF121212, 0xFF1E1E1E, 0xFFFFFFFF, 0xFFB0BEC5, 0xFF1E1E1E, 0xFFECEFF1, 0xFF263238, 0xFFFFFFFF, 0xFFFF9800, 0xFFFFFFFF, 0xFF37474F, 0xFFECEFF1, 0xFF2E7D32, 0xFFFFFFFF),
            // 3. Cyberpunk Theme
            new AppTheme("Cyberpunk", 0xFF0A0915, 0xFF181124, 0xFF00FFCC, 0xFFFF007F, 0xFF181124, 0xFF00FFCC, 0xFF1F1A3A, 0xFF00FFCC, 0xFFFF007F, 0xFFFFFFFF, 0xFF3A1C54, 0xFF00FFFF, 0xFF00E5FF, 0xFF0A0915),
            // 4. Forest Green Theme
            new AppTheme("Forest Green", 0xFFE8F5E9, 0xFFC8E6C9, 0xFF1B5E20, 0xFF4CAF50, 0xFFC8E6C9, 0xFF1B5E20, 0xFFA5D6A7, 0xFF1B5E20, 0xFF2E7D32, 0xFFFFFFFF, 0xFFC8E6C9, 0xFF1B5E20, 0xFF1B5E20, 0xFFFFFFFF)
        };
    }

    private void cycleTheme() {
        currentThemeIndex = (currentThemeIndex + 1) % themes.length;
        applyTheme(themes[currentThemeIndex]);
    }

    private void applyTheme(AppTheme theme) {
        View mainContainer = findViewById(R.id.mainContainer);
        if (mainContainer != null) mainContainer.setBackgroundColor(theme.bgColor);

        View layoutHeader = findViewById(R.id.layoutHeader);
        if (layoutHeader != null) layoutHeader.setBackgroundColor(theme.headerColor);

        tvExpression.setTextColor(theme.displaySubTextColor);
        tvResult.setTextColor(theme.displayTextColor);

        View layoutHistory = findViewById(R.id.layoutHistory);
        if (layoutHistory != null) layoutHistory.setBackgroundColor(theme.historyBgColor);

        TextView tvHistoryHeader = (TextView) findViewById(R.id.tvHistoryHeader);
        if (tvHistoryHeader != null) tvHistoryHeader.setTextColor(theme.historyTextColor);
        tvHistoryList.setTextColor(theme.historyTextColor);

        // Apply dynamically mutated colors preserving rounded corners
        setButtonTheme((Button) findViewById(R.id.btn0), theme.numBtnColor, theme.numTextColor);
        setButtonTheme((Button) findViewById(R.id.btn1), theme.numBtnColor, theme.numTextColor);
        setButtonTheme((Button) findViewById(R.id.btn2), theme.numBtnColor, theme.numTextColor);
        setButtonTheme((Button) findViewById(R.id.btn3), theme.numBtnColor, theme.numTextColor);
        setButtonTheme((Button) findViewById(R.id.btn4), theme.numBtnColor, theme.numTextColor);
        setButtonTheme((Button) findViewById(R.id.btn5), theme.numBtnColor, theme.numTextColor);
        setButtonTheme((Button) findViewById(R.id.btn6), theme.numBtnColor, theme.numTextColor);
        setButtonTheme((Button) findViewById(R.id.btn7), theme.numBtnColor, theme.numTextColor);
        setButtonTheme((Button) findViewById(R.id.btn8), theme.numBtnColor, theme.numTextColor);
        setButtonTheme((Button) findViewById(R.id.btn9), theme.numBtnColor, theme.numTextColor);
        setButtonTheme((Button) findViewById(R.id.btnDot), theme.numBtnColor, theme.numTextColor);

        setButtonTheme((Button) findViewById(R.id.btnAdd), theme.opBtnColor, theme.opTextColor);
        setButtonTheme((Button) findViewById(R.id.btnSubtract), theme.opBtnColor, theme.opTextColor);
        setButtonTheme((Button) findViewById(R.id.btnMultiply), theme.opBtnColor, theme.opTextColor);
        setButtonTheme((Button) findViewById(R.id.btnDivide), theme.opBtnColor, theme.opTextColor);

        // Maintain soft red for clear in light modes, theme specified in dark mode/cyberpunk
        int clearTextColor = (theme.name.equals("Classic Light")) ? 0xFFF44336 : theme.actTextColor;
        setButtonTheme((Button) findViewById(R.id.btnClear), theme.actBtnColor, clearTextColor);
        setButtonTheme((Button) findViewById(R.id.btnBracket), theme.actBtnColor, theme.actTextColor);
        setButtonTheme((Button) findViewById(R.id.btnPercent), theme.actBtnColor, theme.actTextColor);
        setButtonTheme((Button) findViewById(R.id.btnDelete), theme.actBtnColor, theme.actTextColor);
        setButtonTheme((Button) findViewById(R.id.btnEqual), theme.eqBtnColor, theme.eqTextColor);

        setButtonTheme((Button) findViewById(R.id.btnHistoryToggle), theme.actBtnColor, theme.actTextColor);
        setButtonTheme((Button) findViewById(R.id.btnThemeCycle), theme.actBtnColor, theme.actTextColor);
        setButtonTheme((Button) findViewById(R.id.btnClearHistory), theme.eqBtnColor, theme.eqTextColor);

        Button btnThemeCycle = (Button) findViewById(R.id.btnThemeCycle);
        if (btnThemeCycle != null) {
            btnThemeCycle.setText("🎨 " + theme.name);
        }
    }

    private void setButtonTheme(Button btn, int normalColor, int textColor) {
        if (btn == null) return;
        btn.setTextColor(textColor);
        Drawable bg = btn.getBackground();
        if (bg != null) {
            bg.mutate().setColorFilter(normalColor, PorterDuff.Mode.SRC_IN);
        }
    }

    private void toggleHistoryPanel() {
        final LinearLayout layoutHistory = (LinearLayout) findViewById(R.id.layoutHistory);
        if (layoutHistory == null) return;

        if (layoutHistory.getVisibility() == View.GONE) {
            layoutHistory.setVisibility(View.VISIBLE);
            layoutHistory.setAlpha(0.0f);
            layoutHistory.setTranslationY(-60f);
            layoutHistory.animate()
                    .alpha(1.0f)
                    .translationY(0.0f)
                    .setDuration(240)
                    .start();
        } else {
            layoutHistory.animate()
                    .alpha(0.0f)
                    .translationY(-60f)
                    .setDuration(200)
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            layoutHistory.setVisibility(View.GONE);
                        }
                    })
                    .start();
        }
    }

    private void updateHistoryView() {
        if (historyList.isEmpty()) {
            tvHistoryList.setText("No history yet.");
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = historyList.size() - 1; i >= 0; i--) {
                sb.append(historyList.get(i)).append("\n\n");
            }
            tvHistoryList.setText(sb.toString());
        }
    }

    private void appendToken(String token, boolean isOperator) {
        if (isResultShown) {
            if (isOperator) {
                isResultShown = false;
            } else {
                tvExpression.setText("");
                isResultShown = false;
            }
        }
        tvExpression.append(token);
        updateAutoResult();
    }

    private void handleDelete() {
        String exp = tvExpression.getText().toString();
        if (exp.length() > 0) {
            tvExpression.setText(exp.substring(0, exp.length() - 1));
            isResultShown = false;
            updateAutoResult();
        }
    }

    private void handleBracket() {
        String exp = tvExpression.getText().toString();
        int openCount = 0;
        int closeCount = 0;
        for (int i = 0; i < exp.length(); i++) {
            if (exp.charAt(i) == '(') openCount++;
            else if (exp.charAt(i) == ')') closeCount++;
        }
        
        if (exp.length() == 0) {
            appendToken("(", false);
            return;
        }
        
        char last = exp.charAt(exp.length() - 1);
        if (openCount > closeCount) {
            if (Character.isDigit(last) || last == ')') {
                appendToken(")", false);
            } else {
                appendToken("(", false);
            }
        } else {
            if (Character.isDigit(last) || last == ')') {
                appendToken("×(", false);
            } else {
                appendToken("(", false);
            }
        }
    }

    private void appendDot() {
        String exp = tvExpression.getText().toString();
        if (exp.isEmpty()) {
            appendToken("0.", false);
            return;
        }
        
        int lastOpIdx = -1;
        for (int i = exp.length() - 1; i >= 0; i--) {
            char c = exp.charAt(i);
            if (c == '+' || c == '-' || c == '×' || c == '÷' || c == '(' || c == ')') {
                lastOpIdx = i;
                break; 
            }
        }
        
        String lastNumber = exp.substring(lastOpIdx + 1);
        if (!lastNumber.contains(".")) {
            appendToken(".", false);
        }
    }

    private void updateAutoResult() {
        String exp = tvExpression.getText().toString();
        if (exp.trim().isEmpty()) {
            tvResult.setText("0");
            return;
        }
        
        String cleanExp = exp;
        while (cleanExp.length() > 0 && isOperator(cleanExp.charAt(cleanExp.length() - 1))) {
            cleanExp = cleanExp.substring(0, cleanExp.length() - 1);
        }
        
        if (cleanExp.isEmpty()) {
            tvResult.setText("");
            return;
        }
        
        try {
            double res = eval(cleanExp);
            if (Double.isInfinite(res) || Double.isNaN(res)) {
                tvResult.setText("");
            } else {
                tvResult.setText(formatResult(res));
            }
        } catch (Exception e) {
            // Silent for live evaluation updates
        }
    }

    private void calculateFinalResult() {
        String exp = tvExpression.getText().toString();
        if (exp.trim().isEmpty()) {
            return;
        }
        try {
            double res = eval(exp);
            String finalRes = formatResult(res);
            
            // Capture into user log history
            String historyEntry = exp + " = " + finalRes;
            historyList.add(historyEntry);
            updateHistoryView();

            tvExpression.setText(finalRes);
            tvResult.setText(finalRes);
            isResultShown = true;
        } catch (ArithmeticException e) {
            tvResult.setText("Error: Div by 0");
        } catch (Exception e) {
            tvResult.setText("Error");
        }
    }

    private boolean isOperator(char c) {
        return c == '+' || c == '-' || c == '×' || c == '÷' || c == '*' || c == '/';
    }

    private String formatResult(double d) {
        if (d == (long) d) {
            return String.format("%d", (long) d);
        } else {
            DecimalFormat df = new DecimalFormat("#.##########");
            return df.format(d);
        }
    }

    public static double eval(final String str) {
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
                        double divisor = parseFactor();
                        if (divisor == 0) throw new ArithmeticException("Division by zero");
                        x /= divisor;
                    }
                    else return x;
                }
            }

            double parseFactor() {
                if (consume('+')) return parseFactor();
                if (consume('-')) return -parseFactor();

                double x;
                int startPos = pos;
                if (consume('(')) {
                    x = parseExpression();
                    consume(')');
                } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(str.substring(startPos, pos));
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
        super.onDestroy();
        if (toneGenerator != null) {
            toneGenerator.release();
        }
    }
}