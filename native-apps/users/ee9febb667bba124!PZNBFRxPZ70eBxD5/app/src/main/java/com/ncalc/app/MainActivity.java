package com.ncalc.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.graphics.Color;
import android.media.AudioTrack;
import android.media.AudioManager;
import android.media.AudioFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity implements View.OnClickListener {

    private TextView tvExpression;
    private TextView tvResult;
    
    private TextView btnMute;
    private TextView btnClick;
    private TextView btnBeep;
    private TextView btnLaser;

    private String expression = "";
    private int selectedSoundType = 1; // 0 = Mute, 1 = Click, 2 = Beep, 3 = Laser
    private SoundPlayer soundPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) { 
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        soundPlayer = new SoundPlayer();

        // Bind views
        tvExpression = (TextView) findViewById(R.id.tv_expression);
        tvResult = (TextView) findViewById(R.id.tv_result);

        btnMute = (TextView) findViewById(R.id.btn_sound_mute);
        btnClick = (TextView) findViewById(R.id.btn_sound_click);
        btnBeep = (TextView) findViewById(R.id.btn_sound_beep);
        btnLaser = (TextView) findViewById(R.id.btn_sound_laser);

        btnMute.setOnClickListener(this);
        btnClick.setOnClickListener(this);
        btnBeep.setOnClickListener(this);
        btnLaser.setOnClickListener(this);

        updateSoundSelectorUI();

        // Map keys to layout targets
        int[] keypadIds = new int[]{
            R.id.btn_clear, R.id.btn_bracket_open, R.id.btn_bracket_close, R.id.btn_divide,
            R.id.btn_7, R.id.btn_8, R.id.btn_9, R.id.btn_multiply,
            R.id.btn_4, R.id.btn_5, R.id.btn_6, R.id.btn_subtract,
            R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_add,
            R.id.btn_decimal, R.id.btn_0, R.id.btn_delete, R.id.btn_equals
        };

        for (int id : keypadIds) {
            findViewById(id).setOnClickListener(this);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.btn_sound_mute) {
            selectedSoundType = 0;
            updateSoundSelectorUI();
            return;
        } else if (id == R.id.btn_sound_click) {
            selectedSoundType = 1;
            updateSoundSelectorUI();
            soundPlayer.playSound(1);
            return;
        } else if (id == R.id.btn_sound_beep) {
            selectedSoundType = 2;
            updateSoundSelectorUI();
            soundPlayer.playSound(2);
            return;
        } else if (id == R.id.btn_sound_laser) {
            selectedSoundType = 3;
            updateSoundSelectorUI();
            soundPlayer.playSound(3);
            return;
        }

        // Trigger selected click dynamic feedback sound
        soundPlayer.playSound(selectedSoundType);

        // Calculator logic
        if (id == R.id.btn_clear) {
            expression = "";
            tvExpression.setText("");
            tvResult.setText("0");
        } else if (id == R.id.btn_delete) {
            if (expression.length() > 0) {
                expression = expression.substring(0, expression.length() - 1);
                tvExpression.setText(expression);
                evaluateLivePreview();
            }
        } else if (id == R.id.btn_equals) {
            performFinalEvaluation();
        } else {
            TextView btn = (TextView) v;
            String text = btn.getText().toString();
            expression += text;
            tvExpression.setText(expression);
            evaluateLivePreview();
        }
    }

    private void updateSoundSelectorUI() {
        btnMute.setBackgroundColor(Color.parseColor("#1F1F1F"));
        btnClick.setBackgroundColor(Color.parseColor("#1F1F1F"));
        btnBeep.setBackgroundColor(Color.parseColor("#1F1F1F"));
        btnLaser.setBackgroundColor(Color.parseColor("#1F1F1F"));

        btnMute.setTextColor(Color.parseColor("#888888"));
        btnClick.setTextColor(Color.parseColor("#888888"));
        btnBeep.setTextColor(Color.parseColor("#888888"));
        btnLaser.setTextColor(Color.parseColor("#888888"));

        if (selectedSoundType == 0) {
            btnMute.setBackgroundColor(Color.parseColor("#FF453A"));
            btnMute.setTextColor(Color.parseColor("#FFFFFF"));
        } else if (selectedSoundType == 1) {
            btnClick.setBackgroundColor(Color.parseColor("#FF9F0A"));
            btnClick.setTextColor(Color.parseColor("#FFFFFF"));
        } else if (selectedSoundType == 2) {
            btnBeep.setBackgroundColor(Color.parseColor("#FF9F0A"));
            btnBeep.setTextColor(Color.parseColor("#FFFFFF"));
        } else if (selectedSoundType == 3) {
            btnLaser.setBackgroundColor(Color.parseColor("#FF9F0A"));
            btnLaser.setTextColor(Color.parseColor("#FFFFFF"));
        }
    }

    private void evaluateLivePreview() {
        if (expression.isEmpty()) {
            tvResult.setText("0");
            return;
        }
        try {
            String sanitized = sanitizeForEvaluation(expression);
            double val = ExpressionEvaluator.eval(sanitized);
            if (!Double.isInfinite(val) && !Double.isNaN(val)) {
                tvResult.setText(formatResult(val));
            }
        } catch (Exception e) {
            // Silent fail during partial entry live preview
        }
    }

    private void performFinalEvaluation() {
        if (expression.isEmpty()) return;
        try {
            String sanitized = sanitizeForEvaluation(expression);
            double val = ExpressionEvaluator.eval(sanitized);
            if (Double.isInfinite(val) || Double.isNaN(val)) {
                tvResult.setText("Error");
                return;
            }
            String formatted = formatResult(val);
            tvResult.setText(formatted);
            expression = formatted;
            tvExpression.setText(expression);
        } catch (Exception e) {
            tvResult.setText("Error");
        }
    }

    private String sanitizeForEvaluation(String expr) {
        String res = expr.replace("×", "*").replace("÷", "/");
        int openCount = 0;
        int closeCount = 0;
        for (int i = 0; i < res.length(); i++) {
            if (res.charAt(i) == '(') openCount++;
            else if (res.charAt(i) == ')') closeCount++;
        }
        while (openCount > closeCount) {
            res += ")";
            closeCount++;
        }
        return res;
    }

    private String formatResult(double val) {
        if (val == (long) val) {
            return String.valueOf((long) val);
        }
        return String.format(java.util.Locale.US, "%.6g", val);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (soundPlayer != null) {
            soundPlayer.shutdown();
        }
    }

    // Custom audio synthetics
    private static class SoundPlayer {
        private final ExecutorService executor = Executors.newSingleThreadExecutor();
        private static final int SAMPLE_RATE = 16000;
        private byte[] clickBuffer;
        private byte[] beepBuffer;
        private byte[] laserBuffer;

        public SoundPlayer() {
            generateAudioTones();
        }

        private void generateAudioTones() {
            // Click waveform
            int clickLen = SAMPLE_RATE * 5 / 100;
            clickBuffer = new byte[clickLen];
            for (int i = 0; i < clickLen; i++) {
                double t = (double) i / SAMPLE_RATE;
                double env = Math.exp(-220.0 * t);
                double val = Math.sin(2 * Math.PI * 950 * t) * env;
                clickBuffer[i] = (byte) (val * 127);
            }

            // Beep waveform
            int beepLen = SAMPLE_RATE * 12 / 100;
            beepBuffer = new byte[beepLen];
            for (int i = 0; i < beepLen; i++) {
                double t = (double) i / SAMPLE_RATE;
                double env = 1.0;
                if (t > 0.09) env = (0.12 - t) / 0.03;
                double val = Math.sin(2 * Math.PI * 1150 * t) * env;
                beepBuffer[i] = (byte) (val * 127);
            }

            // Laser swoop waveform
            int laserLen = SAMPLE_RATE * 15 / 100;
            laserBuffer = new byte[laserLen];
            double phase = 0.0;
            for (int i = 0; i < laserLen; i++) {
                double t = (double) i / laserLen;
                double freq = 1900.0 - (1650.0 * t);
                phase += 2 * Math.PI * freq / SAMPLE_RATE;
                double env = 1.0 - t;
                double val = Math.sin(phase) * env;
                laserBuffer[i] = (byte) (val * 127);
            }
        }

        public void playSound(final int soundType) {
            if (soundType == 0) return;
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    byte[] data;
                    if (soundType == 1) {
                        data = clickBuffer;
                    } else if (soundType == 2) { 
                        data = beepBuffer;
                    } else if (soundType == 3) {
                        data = laserBuffer;
                    } else {
                        return;
                    }

                    try {
                        AudioTrack track = new AudioTrack(
                            AudioManager.STREAM_MUSIC,
                            SAMPLE_RATE,
                            AudioFormat.CHANNEL_OUT_MONO,
                            AudioFormat.ENCODING_PCM_8BIT,
                            data.length,
                            AudioTrack.MODE_STATIC
                        );
                        track.write(data, 0, data.length);
                        track.play();
                        Thread.sleep(data.length * 1000L / SAMPLE_RATE + 25);
                        track.stop();
                        track.release();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        public void shutdown() {
            executor.shutdown();
        }
    }

    // Native robust mathematical parser
    private static class ExpressionEvaluator {
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
                    if (pos < str.length()) throw new RuntimeException("Unexpected: " + (char)ch);
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
                        else if (eat('/')) x /= parseFactor();
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
                    return x;
                }
            }.parse();
        }
    }
}