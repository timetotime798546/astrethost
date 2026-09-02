package com.threedcalculator.app;

import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private RelativeLayout loaderOverlay;
    private ThreeDLoaderView loaderAnimation;
    private TextView loaderStatus;
    private TextView loaderProgressText;
    
    private TextView tvFormula;
    private TextView tvDisplay;
    private ThreeDCalculatorView calcGrid;
    private ScrollView historyScroll;
    private LinearLayout historyListContainer;
    private Button btnToggleHistory;
    private Button btnClearHistory;
    
    private String currentFormula = "";
    private String currentDisplay = "0";
    private boolean isResultCalculated = false;
    private boolean isHistoryVisible = false;
    
    private TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) { 
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        loaderOverlay = (RelativeLayout) findViewById(R.id.loader_overlay);
        loaderAnimation = (ThreeDLoaderView) findViewById(R.id.loader_animation);
        loaderStatus = (TextView) findViewById(R.id.loader_status);
        loaderProgressText = (TextView) findViewById(R.id.loader_progress_text);
        
        tvFormula = (TextView) findViewById(R.id.tv_formula);
        tvDisplay = (TextView) findViewById(R.id.tv_display);
        calcGrid = (ThreeDCalculatorView) findViewById(R.id.calc_grid);
        historyScroll = (ScrollView) findViewById(R.id.history_scroll);
        historyListContainer = (LinearLayout) findViewById(R.id.history_list_container);
        btnToggleHistory = (Button) findViewById(R.id.btn_toggle_history);
        btnClearHistory = (Button) findViewById(R.id.btn_clear_history);
        
        // Apply beautiful Linear Gradients dynamically
        applyProgrammaticGradients();
        
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    if (tts != null) {
                        tts.setLanguage(Locale.US);
                        tts.setPitch(1.05f);
                        tts.setSpeechRate(0.95f);
                    }
                }
            }
        });

        startLoaderSequence();
        
        calcGrid.setOnButtonClickListener(new ThreeDCalculatorView.OnButtonClickListener() {
            @Override
            public void onButtonClick(String label) {
                handleCalculatorInput(label);
            } 
        });
        
        btnToggleHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleHistoryView();
            }
        });
        
        btnClearHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SoundSynth.playOperatorClick();
                HistoryManager.clearHistory(MainActivity.this);
                populateHistoryUI();
            }
        });
        
        populateHistoryUI();
    }

    private void applyProgrammaticGradients() {
        // Root main layout cosmic background gradient
        RelativeLayout rootLayout = (RelativeLayout) findViewById(R.id.root_layout);
        if (rootLayout != null) {
            GradientDrawable rootGrad = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[] { 0xFF141824, 0xFF0B0D13 }
            );
            rootLayout.setBackground(rootGrad);
        }

        // Display Container with double glow neon borders and deep space gradient
        LinearLayout displayContainer = (LinearLayout) findViewById(R.id.display_container);
        if (displayContainer != null) {
            GradientDrawable displayGrad = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] { 0xFF080B12, 0xFF121822 }
            );
            displayGrad.setCornerRadius(16f);
            displayGrad.setStroke(3, 0xFF00FFCC);
            displayContainer.setBackground(displayGrad);
        }

        // Top Navigation Header bar sleek slate metallic gradient
        RelativeLayout headerBar = (RelativeLayout) findViewById(R.id.header_bar);
        if (headerBar != null) {
            GradientDrawable headerGrad = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] { 0xFF1B1E29, 0xFF0F1118 }
            );
            headerBar.setBackground(headerGrad);
        }

        // Show History trigger button aesthetic gradient
        if (btnToggleHistory != null) {
            GradientDrawable btnGrad = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] { 0xFF2F3647, 0xFF1B202E }
            );
            btnGrad.setCornerRadius(10f);
            btnToggleHistory.setBackground(btnGrad);
        }
    }

    private void startLoaderSequence() {
        final Handler handler = new Handler();
        
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                loaderStatus.setText("Booting 3D Gradient Systems...");
                loaderProgressText.setText("25%");
                SoundSynth.playLoaderSound(1);
                speakWelcome();
            } 
        }, 500);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                loaderStatus.setText("Initializing Dynamic DSP Synthesizer...");
                loaderProgressText.setText("50%");
                SoundSynth.playLoaderSound(2);
                SoundSynth.playLoaderRiser();
            }
        }, 1100);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                loaderStatus.setText("Calibrating Specular Light Extrusion...");
                loaderProgressText.setText("75%");
                SoundSynth.playLoaderSound(3);
            }
        }, 1700);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                loaderStatus.setText("3D Interface Fully Operational!");
                loaderProgressText.setText("100%");
                SoundSynth.playLoaderSound(4);
            }
        }, 2300);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                loaderOverlay.animate()
                    .alpha(0f)
                    .setDuration(400)
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            loaderOverlay.setVisibility(View.GONE);
                        }
                    });
            } 
        }, 2800);
    }

    private void speakWelcome() {
        if (tts != null) {
            String text = "Welcome to amazing 3D synthesizer calculator";
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "welcome_speech");
        }
    }

    private void handleCalculatorInput(String label) {
        if (label.equals("C")) {
            currentFormula = "";
            currentDisplay = "0";
            isResultCalculated = false;
        } else if (label.equals("←")) {
            if (currentFormula.length() > 0) {
                currentFormula = currentFormula.substring(0, currentFormula.length() - 1);
            }
            if (currentFormula.isEmpty()) {
                currentDisplay = "0";
            } else {
                currentDisplay = currentFormula;
            }
        } else if (label.equals("=")) {
            if (!currentFormula.isEmpty()) {
                try {
                    double result = eval(currentFormula);
                    String resultString;
                    if (result == (long) result) {
                        resultString = String.format("%d", (long) result);
                    } else {
                        resultString = String.format("%s", result);
                    }
                    
                    HistoryManager.saveCalculation(MainActivity.this, currentFormula, resultString);
                    populateHistoryUI();
                    
                    currentFormula = currentFormula + " = " + resultString;
                    currentDisplay = resultString;
                    isResultCalculated = true;
                } catch (Exception e) {
                    SoundSynth.playError();
                    currentDisplay = "Error";
                    isResultCalculated = true;
                }
            }
        } else {
            if (isResultCalculated) {
                if ("+-*/".contains(label)) {
                    currentFormula = currentDisplay + label;
                } else {
                    currentFormula = label;
                }
                isResultCalculated = false;
            } else {
                currentFormula += label;
            }
            currentDisplay = currentFormula;
        }
        
        tvFormula.setText(currentFormula);
        tvDisplay.setText(currentDisplay);
    }

    private void toggleHistoryView() {
        SoundSynth.playClick();
        if (isHistoryVisible) {
            historyScroll.setVisibility(View.GONE);
            btnClearHistory.setVisibility(View.GONE);
            btnToggleHistory.setText("SHOW HISTORY");
            isHistoryVisible = false;
        } else {
            historyScroll.setVisibility(View.VISIBLE);
            btnClearHistory.setVisibility(View.VISIBLE);
            btnToggleHistory.setText("HIDE HISTORY");
            isHistoryVisible = true;
            populateHistoryUI();
        }
    }

    private void populateHistoryUI() {
        historyListContainer.removeAllViews();
        List<String> items = HistoryManager.getHistory(MainActivity.this);
        
        if (items.isEmpty()) {
            TextView emptyText = new TextView(MainActivity.this);
            emptyText.setText("No calculation history logs");
            emptyText.setTextColor(0xFF7E8694);
            emptyText.setPadding(30, 20, 30, 20);
            emptyText.setTextSize(15f);
            historyListContainer.addView(emptyText);
            return;
        }
        
        for (int i = 0; i < items.size(); i++) {
            final String record = items.get(i);
            
            LinearLayout itemLayout = new LinearLayout(MainActivity.this);
            itemLayout.setOrientation(LinearLayout.VERTICAL);
            itemLayout.setPadding(30, 20, 30, 20);
            itemLayout.setClickable(true);
            itemLayout.setBackgroundColor(0xFF161922);
            
            TextView tvRecord = new TextView(MainActivity.this);
            tvRecord.setText(record);
            tvRecord.setTextColor(0xFF00FFCC);
            tvRecord.setTextSize(17f);
            
            itemLayout.addView(tvRecord);
            
            itemLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SoundSynth.playClick();
                    String[] parts = record.split(" = ");
                    if (parts.length > 0) {
                        currentFormula = parts[0];
                        currentDisplay = parts[parts.length - 1];
                        tvFormula.setText(currentFormula);
                        tvDisplay.setText(currentDisplay);
                        isResultCalculated = false;
                    }
                }
            });
            
            View divider = new View(MainActivity.this);
            divider.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 2));
            divider.setBackgroundColor(0xFF232836);
            
            historyListContainer.addView(itemLayout);
            historyListContainer.addView(divider);
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
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
                    if      (eat('*')) x *= parseFactor();
                    else if (eat('/')) {
                        double div = parseFactor();
                        if (div == 0) throw new ArithmeticException("Divide by zero error");
                        x /= div;
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
                    throw new RuntimeException("Unexpected math syntax: " + (char)ch);
                } 
                return x;
            }
        }.parse();
    }
}