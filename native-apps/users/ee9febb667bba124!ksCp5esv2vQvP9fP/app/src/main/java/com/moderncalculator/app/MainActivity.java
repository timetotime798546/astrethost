package com.moderncalculator.app;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.RelativeLayout;
import java.text.DecimalFormat;

public class MainActivity extends Activity {

    private TextView tvExpression;
    private TextView tvResult;
    private RelativeLayout layoutLoader;
    private TextView tvLoaderStatus;
    
    private StringBuilder currentInput = new StringBuilder();
    private StringBuilder expressionStr = new StringBuilder();
    private boolean isCalculated = false;
    private final DecimalFormat decimalFormat = new DecimalFormat("0.########");
    private final Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvExpression = (TextView) findViewById(R.id.tv_expression);
        tvResult = (TextView) findViewById(R.id.tv_result);
        layoutLoader = (RelativeLayout) findViewById(R.id.layout_loader);
        tvLoaderStatus = (TextView) findViewById(R.id.tv_loader_status);

        setupButtons();
        startLoadingProcess();
    }

    private void startLoadingProcess() {
        layoutLoader.setVisibility(View.VISIBLE);
        tvLoaderStatus.setText("Initializing dynamic audio drivers...");
        playTone(5); // Atmospheric swoosh sound on start

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                tvLoaderStatus.setText("Calibrating tactile mechanical depths...");
                playTone(1);
            }
        }, 700);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                tvLoaderStatus.setText("Synthesizer Engine Ready!");
                playTone(4);
                layoutLoader.setVisibility(View.GONE);
            }
        }, 1500);
    }

    // Dynamically Synthesizes pristine tone waves with AudioTrack
    private void playTone(final int toneType) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int sampleRate = 22050;
                    double duration = 0.05;
                    double freq1 = 1000.0;
                    double freq2 = 0.0;
                    boolean isSweep = false;
                    double endFreq = 0.0;
                    
                    switch (toneType) {
                        case 1: // Numeric keystrokes
                            duration = 0.035;
                            freq1 = 1100.0;
                            break;
                        case 2: // Operators keystrokes
                            duration = 0.07;
                            freq1 = 800.0;
                            freq2 = 1200.0; // rich dual-chime
                            break;
                        case 3: // Clear & Sweep down
                            duration = 0.25;
                            freq1 = 1400.0;
                            endFreq = 300.0;
                            isSweep = true;
                            break;
                        case 4: // Equality calculation chime
                            duration = 0.15;
                            freq1 = 600.0;
                            endFreq = 1500.0;
                            isSweep = true;
                            break;
                        case 5: // Boot sequence sweep
                            duration = 0.8;
                            freq1 = 200.0;
                            endFreq = 1000.0;
                            isSweep = true;
                            break;
                    }
                    
                    int numSamples = (int) (sampleRate * duration);
                    short[] buffer = new short[numSamples];
                    
                    for (int i = 0; i < numSamples; i++) {
                        double t = (double) i / sampleRate;
                        double currentFreq = freq1;
                        if (isSweep) {
                            currentFreq = freq1 + (endFreq - freq1) * (t / duration);
                        }
                        
                        double val = Math.sin(2.0 * Math.PI * currentFreq * t);
                        if (freq2 > 0) {
                            val = (val + Math.sin(2.0 * Math.PI * freq2 * t)) * 0.5;
                        }
                        
                        // Smooth linear envelope to avoid cracking noise on static buffer start/end
                        double envelope = 1.0;
                        double margin = 0.1 * duration;
                        if (t < margin) {
                            envelope = t / margin;
                        } else if (t > duration - margin) {
                            envelope = (duration - t) / margin;
                        }
                        
                        buffer[i] = (short) (val * 32767.0 * 0.3 * envelope);
                    }
                    
                    AudioTrack track = new AudioTrack(
                            AudioManager.STREAM_MUSIC,
                            sampleRate,
                            AudioFormat.CHANNEL_OUT_MONO,
                            AudioFormat.ENCODING_PCM_16BIT,
                            numSamples * 2,
                            AudioTrack.MODE_STATIC);
                    
                    track.write(buffer, 0, numSamples);
                    track.play();
                    
                    Thread.sleep((long) (duration * 1000) + 40);
                    track.stop();
                    track.release();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    // Premium bouncy response to visual elements
    private void applyClickBounce(final View view) {
        view.setScaleX(0.92f);
        view.setScaleY(0.92f);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                view.setScaleX(1.0f);
                view.setScaleY(1.0f);
            } 
        }, 70);
    }

    private void setupButtons() {
        int[] numberIds = new int[] {
                R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4,
                R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9
        };

        View.OnClickListener numberClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyClickBounce(v);
                playTone(1);
                Button b = (Button) v;
                if (isCalculated) {
                    clearAll();
                }
                currentInput.append(b.getText().toString());
                updateDisplay();
                evaluateOnTheFly();
            }
        };

        for (int id : numberIds) {
            findViewById(id).setOnClickListener(numberClickListener);
        }

        findViewById(R.id.btn_decimal).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyClickBounce(v);
                playTone(1);
                if (isCalculated) {
                    clearAll();
                }
                if (currentInput.indexOf(".") == -1) {
                    if (currentInput.length() == 0) {
                        currentInput.append("0");
                    }
                    currentInput.append(".");
                    updateDisplay();
                }
            }
        });

        int[] operatorIds = new int[] {
                R.id.btn_add, R.id.btn_subtract, R.id.btn_multiply, R.id.btn_divide
        };

        View.OnClickListener operatorClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyClickBounce(v);
                playTone(2);
                Button b = (Button) v;
                String op = b.getText().toString();
                
                if (isCalculated) {
                    String res = tvResult.getText().toString();
                    clearAll();
                    if (!res.equals("Error")) {
                        currentInput.append(res);
                    }
                }

                if (currentInput.length() > 0) {
                    expressionStr.append(currentInput).append(" ").append(op).append(" ");
                    currentInput.setLength(0);
                } else if (expressionStr.length() > 0) {
                    expressionStr.setLength(expressionStr.length() - 3);
                    expressionStr.append(" ").append(op).append(" ");
                }
                updateDisplay();
            }
        };

        for (int id : operatorIds) {
            findViewById(id).setOnClickListener(operatorClickListener);
        }

        findViewById(R.id.btn_clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyClickBounce(v);
                clearAll();
                
                layoutLoader.setVisibility(View.VISIBLE);
                tvLoaderStatus.setText("Recalibrating sensors...");
                playTone(3);
                
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        layoutLoader.setVisibility(View.GONE);
                    }
                }, 400);
            }
        });

        findViewById(R.id.btn_delete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyClickBounce(v);
                playTone(1);
                if (isCalculated) {
                    clearAll();
                } else if (currentInput.length() > 0) {
                    currentInput.deleteCharAt(currentInput.length() - 1);
                    updateDisplay();
                    evaluateOnTheFly();
                }
            }
        });

        findViewById(R.id.btn_percent).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyClickBounce(v);
                playTone(2);
                if (currentInput.length() > 0) {
                    try {
                        double val = Double.parseDouble(currentInput.toString()) / 100.0;
                        currentInput.setLength(0);
                        currentInput.append(decimalFormat.format(val));
                        updateDisplay();
                        evaluateOnTheFly();
                    } catch (NumberFormatException e) {
                        // ignored
                    }
                }
            }
        });

        findViewById(R.id.btn_sign).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyClickBounce(v);
                playTone(2);
                if (currentInput.length() > 0) {
                    try {
                        double val = Double.parseDouble(currentInput.toString()) * -1.0;
                        currentInput.setLength(0);
                        currentInput.append(decimalFormat.format(val));
                        updateDisplay();
                        evaluateOnTheFly();
                    } catch (NumberFormatException e) {
                        // ignored
                    }
                }
            }
        });

        findViewById(R.id.btn_equal).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyClickBounce(v);
                if (currentInput.length() > 0 || expressionStr.length() > 0) {
                    final String finalExpr = expressionStr.toString() + currentInput.toString();
                    
                    layoutLoader.setVisibility(View.VISIBLE);
                    tvLoaderStatus.setText("Solving mathematical matrices...");
                    playTone(4);
                    
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            layoutLoader.setVisibility(View.GONE);
                            double result = evaluateExpression(finalExpr);
                            if (Double.isNaN(result)) {
                                tvResult.setText("Error");
                            } else {
                                tvResult.setText(decimalFormat.format(result));
                            }
                            tvExpression.setText(finalExpr);
                            isCalculated = true;
                        }
                    }, 300);
                }
            }
        });
    }

    private void clearAll() {
        currentInput.setLength(0);
        expressionStr.setLength(0);
        isCalculated = false;
        tvExpression.setText("");
        tvResult.setText("0");
    }

    private void updateDisplay() {
        String current = currentInput.toString();
        String full = expressionStr.toString() + current;
        tvExpression.setText(full);
        if (current.length() > 0) {
            tvResult.setText(current);
        } else {
            tvResult.setText("0");
        }
    }

    private void evaluateOnTheFly() {
        if (expressionStr.length() > 0) {
            String full = expressionStr.toString() + currentInput.toString();
            double preview = evaluateExpression(full);
            if (!Double.isNaN(preview)) {
                tvResult.setText(decimalFormat.format(preview));
            } 
        } 
    }

    private double evaluateExpression(String expression) {
        try {
            final String formatted = expression.replace("×", "*")
                                               .replace("÷", "/")
                                               .replaceAll("\\s+", "");
            return new Object() {
                int pos = -1, ch;

                void nextChar() {
                    ch = (++pos < formatted.length()) ? formatted.charAt(pos) : -1;
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
                    if (pos < formatted.length()) return Double.NaN;
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
                            if (divisor == 0) return Double.NaN;
                            x /= divisor;
                        } else return x;
                    }
                }

                double parseFactor() {
                    if (eat('+')) return parseFactor();
                    if (eat('-')) return -parseFactor();

                    double x;
                    int startPos = this.pos;
                    if ((ch >= '0' && ch <= '9') || ch == '.') {
                        while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                        try {
                            x = Double.parseDouble(formatted.substring(startPos, this.pos));
                        } catch (NumberFormatException e) {
                            return Double.NaN;
                        }
                    } else {
                        return Double.NaN;
                    }
                    return x;
                }
            }.parse();
        } catch (Exception e) {
            return Double.NaN;
        }
    }
}