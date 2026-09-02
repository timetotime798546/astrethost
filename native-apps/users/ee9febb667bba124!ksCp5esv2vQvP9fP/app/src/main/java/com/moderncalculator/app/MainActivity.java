package com.moderncalculator.app;

import android.app.Activity;
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
        tvLoaderStatus.setText("Initializing sound & engine...");
        playLoaderSound(400.0, 1000.0, 1.2);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                tvLoaderStatus.setText("Calibrating matrices...");
                playLoaderSound(600.0, 1400.0, 0.4);
            }
        }, 700);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                tvLoaderStatus.setText("Ready!");
                layoutLoader.setVisibility(View.GONE);
            }
        }, 1500);
    }

    private void playLoaderSound(final double startFreq, final double endFreq, final double durationSeconds) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                int sampleRate = 44100;
                int numSamples = (int) (sampleRate * durationSeconds);
                double[] sample = new double[numSamples];
                byte[] generatedSnd = new byte[2 * numSamples];

                for (int i = 0; i < numSamples; ++i) {
                    double t = (double) i / sampleRate;
                    double freq = startFreq + ((endFreq - startFreq) * (t / durationSeconds));
                    sample[i] = Math.sin(2 * Math.PI * freq * t);
                    
                    // Apply fade envelope to avoid audio clicks
                    double envelope = 1.0;
                    if (t > durationSeconds - 0.08) {
                        envelope = (durationSeconds - t) / 0.08;
                    } else if (t < 0.02) {
                        envelope = t / 0.02;
                    }
                    sample[i] *= envelope;
                }

                int idx = 0;
                for (final double dVal : sample) {
                    final short val = (short) ((dVal * 22000));
                    generatedSnd[idx++] = (byte) (val & 0x00ff);
                    generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
                }

                try {
                    android.media.AudioTrack audioTrack = new android.media.AudioTrack(
                            android.media.AudioManager.STREAM_MUSIC,
                            sampleRate,
                            android.media.AudioFormat.CHANNEL_OUT_MONO,
                            android.media.AudioFormat.ENCODING_PCM_16BIT,
                            generatedSnd.length,
                            android.media.AudioTrack.MODE_STATIC);
                    audioTrack.write(generatedSnd, 0, generatedSnd.length);
                    audioTrack.play();
                    
                    try {
                        Thread.sleep((long) (durationSeconds * 1000) + 100);
                    } catch (InterruptedException ignored) {}
                    audioTrack.stop();
                    audioTrack.release();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void setupButtons() {
        int[] numberIds = new int[] {
                R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4,
                R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9
        };

        View.OnClickListener numberClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
                clearAll();
                
                // Trigger loader effect dynamically on clear/reset
                layoutLoader.setVisibility(View.VISIBLE);
                tvLoaderStatus.setText("System Reset...");
                playLoaderSound(1200.0, 600.0, 0.35);
                
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
                if (currentInput.length() > 0) {
                    try {
                        double val = Double.parseDouble(currentInput.toString()) / 100.0;
                        currentInput.setLength(0);
                        currentInput.append(decimalFormat.format(val));
                        updateDisplay();
                        evaluateOnTheFly();
                    } catch (NumberFormatException e) {
                        // ignore
                    }
                }
            }
        });

        findViewById(R.id.btn_sign).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentInput.length() > 0) {
                    try {
                        double val = Double.parseDouble(currentInput.toString()) * -1.0;
                        currentInput.setLength(0);
                        currentInput.append(decimalFormat.format(val));
                        updateDisplay();
                        evaluateOnTheFly();
                    } catch (NumberFormatException e) {
                        // ignore
                    }
                }
            }
        });

        findViewById(R.id.btn_equal).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentInput.length() > 0 || expressionStr.length() > 0) {
                    final String finalExpr = expressionStr.toString() + currentInput.toString();
                    
                    // Show calculated loader sequence for a quick visual touch
                    layoutLoader.setVisibility(View.VISIBLE);
                    tvLoaderStatus.setText("Solving equation...");
                    playLoaderSound(900.0, 1500.0, 0.25);
                    
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