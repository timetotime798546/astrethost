package com.threedcalculator.app;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.List;

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

    private void startLoaderSequence() {
        final Handler handler = new Handler();
        
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                loaderStatus.setText("Booting 3D Systems...");
                loaderProgressText.setText("25%");
                SoundSynth.playLoaderSound(1);
            }
        }, 500);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                loaderStatus.setText("Initializing Dynamic DSP Synthesizer...");
                loaderProgressText.setText("50%");
                SoundSynth.playLoaderSound(2);
            }
        }, 1100);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                loaderStatus.setText("Calibrating Extrusion Matrices...");
                loaderProgressText.setText("75%");
                SoundSynth.playLoaderSound(3);
            }
        }, 1700);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                loaderStatus.setText("Interface Operational!");
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
            itemLayout.setBackgroundColor(0xFF1E222B);
            
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
            divider.setBackgroundColor(0xFF2C3241);
            
            historyListContainer.addView(itemLayout);
            historyListContainer.addView(divider);
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