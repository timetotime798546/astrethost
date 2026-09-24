package com.magicwandoracle.app;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import java.util.Random;

public class MainActivity extends Activity implements SensorEventListener {

    // Tab Navigation
    private Button btnTabOracle, btnTabReader, btnTabWand;
    private View containerOracle, containerReader, containerWand;

    // Magic 8-Ball
    private EditText etQuestion;
    private RelativeLayout ballLayout;
    private TextView tvBallAnswer;
    private Button btnCastOracle;
    private final Random random = new Random();
    private boolean isGeneratingAnswer = false;

    private static final String[] ORACLE_ANSWERS = {
        "It is certain.",
        "Decidedly so.",
        "Without a doubt.",
        "Yes, definitely.",
        "You may rely on it.",
        "As I see it, yes.",
        "Most likely.",
        "Outlook good.",
        "Yes, cosmic energy agrees.",
        "Signs point to yes.",
        "Reply hazy, try again.",
        "Ask again later.",
        "Better not tell you now.",
        "Cannot predict now.",
        "Concentrate and ask again.",
        "Don't count on it.",
        "My reply is no.",
        "My sources say no.",
        "Outlook not so good.",
        "Very doubtful."
    };

    // Mind Reader Game Logic
    private TextView tvReaderStepNum, tvReaderInstruction;
    private Button btnReaderNext;
    private int currentMindStep = 1;

    // Magic Wand Screen
    private MagicParticleView particleCanvas;
    private long lastToneTime = 0;

    // Accelerometer Sensors
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private long lastShakeTime = 0;
    private static final float SHAKE_THRESHOLD = 15.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initializing UI Tabs
        btnTabOracle = findViewById(R.id.btn_tab_oracle);
        btnTabReader = findViewById(R.id.btn_tab_reader);
        btnTabWand = findViewById(R.id.btn_tab_wand);

        containerOracle = findViewById(R.id.container_oracle);
        containerReader = findViewById(R.id.container_reader);
        containerWand = findViewById(R.id.container_wand);

        // Subsystems
        initTabs();
        initOracle();
        initMindReader();
        initWandEngine();
        initSensors();
    }

    private void initTabs() {
        btnTabOracle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(1);
            }
        });

        btnTabReader.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(2);
            }
        });

        btnTabWand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(3);
            }
        });
    }

    private void switchTab(int id) {
        // Change Tabs appearance
        btnTabOracle.setBackgroundResource(id == 1 ? R.drawable.tab_selected_bg : R.drawable.tab_unselected_bg);
        btnTabOracle.setTextColor(id == 1 ? Color.parseColor("#FFFFFF") : Color.parseColor("#9C97B0"));

        btnTabReader.setBackgroundResource(id == 2 ? R.drawable.tab_selected_bg : R.drawable.tab_unselected_bg);
        btnTabReader.setTextColor(id == 2 ? Color.parseColor("#FFFFFF") : Color.parseColor("#9C97B0"));

        btnTabWand.setBackgroundResource(id == 3 ? R.drawable.tab_selected_bg : R.drawable.tab_unselected_bg);
        btnTabWand.setTextColor(id == 3 ? Color.parseColor("#FFFFFF") : Color.parseColor("#9C97B0"));

        // Change view visibility
        containerOracle.setVisibility(id == 1 ? View.VISIBLE : View.GONE);
        containerReader.setVisibility(id == 2 ? View.VISIBLE : View.GONE);
        containerWand.setVisibility(id == 3 ? View.VISIBLE : View.GONE);

        playCastSoundThrottled();
    }

    private void initOracle() {
        etQuestion = findViewById(R.id.et_question);
        ballLayout = findViewById(R.id.ball_layout);
        tvBallAnswer = findViewById(R.id.tv_ball_answer);
        btnCastOracle = findViewById(R.id.btn_cast_oracle);

        View.OnClickListener triggerOracle = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerOracleSpells();
            }
        };

        ballLayout.setOnClickListener(triggerOracle);
        btnCastOracle.setOnClickListener(triggerOracle);
    }

    private void triggerOracleSpells() {
        if (isGeneratingAnswer) return;

        // Dismiss keyboard smoothly
        View focused = getCurrentFocus();
        if (focused != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(focused.getWindowToken(), 0);
        }

        isGeneratingAnswer = true;
        tvBallAnswer.setText("CONSULTING\nTHE ORACLE...");
        tvBallAnswer.setTextColor(Color.parseColor("#00FFFF"));

        // Play magical shaking motion
        TranslateAnimation shake = new TranslateAnimation(-15, 15, -15, 15);
        shake.setDuration(100);
        shake.setRepeatCount(8);
        shake.setRepeatMode(Animation.REVERSE);
        shake.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        String ans = ORACLE_ANSWERS[random.nextInt(ORACLE_ANSWERS.length)];
                        tvBallAnswer.setText(ans);
                        tvBallAnswer.setTextColor(Color.parseColor("#FFD700"));
                        isGeneratingAnswer = false;
                        triggerSpellVibrator();
                        playMagicChimeSound();
                    }
                }, 400);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });

        ballLayout.startAnimation(shake);
        playCastSoundThrottled();
    }

    private void initMindReader() {
        tvReaderStepNum = findViewById(R.id.tv_reader_step_num);
        tvReaderInstruction = findViewById(R.id.tv_reader_instruction);
        btnReaderNext = findViewById(R.id.btn_reader_next);

        btnReaderNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                advanceMindReader();
            }
        });
    }

    private void advanceMindReader() {
        currentMindStep++;
        if (currentMindStep > 7) {
            currentMindStep = 1;
        }

        playCastSoundThrottled();

        switch (currentMindStep) {
            case 1:
                tvReaderStepNum.setText("STEP 1 OF 6");
                tvReaderInstruction.setText("Think of any secret integer between 1 and 10.\n\nKeep it safe in your mind!");
                btnReaderNext.setText("CONTINUE");
                break;
            case 2:
                tvReaderStepNum.setText("STEP 2 OF 6");
                tvReaderInstruction.setText("Double your secret number now.\n\n(Multiply it by 2)");
                btnReaderNext.setText("DONE");
                break;
            case 3:
                tvReaderStepNum.setText("STEP 3 OF 6");
                tvReaderInstruction.setText("Add 10 to the current result in your head.");
                btnReaderNext.setText("DONE");
                break;
            case 4:
                tvReaderStepNum.setText("STEP 4 OF 6");
                tvReaderInstruction.setText("Divide your new accumulated sum by 2.");
                btnReaderNext.setText("DONE");
                break;
            case 5:
                tvReaderStepNum.setText("STEP 5 OF 6");
                tvReaderInstruction.setText("Now, subtract your original secret number from your current number.");
                btnReaderNext.setText("READY FOR MAGIC");
                break;
            case 6:
                tvReaderStepNum.setText("COSMIC ALIGNMENT");
                tvReaderInstruction.setText("Close your eyes and breathe.\nThe app is intercepting your brainwaves...");
                btnReaderNext.setText("REVEAL MY MIND");
                break;
            case 7:
                tvReaderStepNum.setText("🔮 REVEALED 🔮");
                tvReaderInstruction.setText("The astral portal is clear!\n\nYour final calculated magic number is...\n\n✨ 5 ✨");
                btnReaderNext.setText("PLAY AGAIN");
                triggerSpellVibrator();
                playMagicChimeSound();
                break;
        }
    }

    private void initWandEngine() {
        particleCanvas = findViewById(R.id.particle_canvas);
        particleCanvas.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
                    particleCanvas.spawnParticles(event.getX(), event.getY(), 6);
                    playCastSoundThrottled();
                    return true;
                }
                return false;
            }
        });
    }

    private void initSensors() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            float gravityForce = 9.8f;
            float netForce = (float) Math.sqrt(x*x + y*y + z*z) - gravityForce;

            if (netForce > SHAKE_THRESHOLD) {
                long now = System.currentTimeMillis();
                if (now - lastShakeTime > 1000) {
                    lastShakeTime = now;
                    triggerShakeAction();
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void triggerShakeAction() {
        triggerSpellVibrator();
        if (containerOracle.getVisibility() == View.VISIBLE) {
            triggerOracleSpells();
        } else if (containerWand.getVisibility() == View.VISIBLE) {
            // Spawn fireworks sparks on random locations
            int width = particleCanvas.getWidth();
            int height = particleCanvas.getHeight();
            if (width > 0 && height > 0) {
                for (int i = 0; i < 3; i++) {
                    particleCanvas.spawnParticles(
                        random.nextInt(width),
                        random.nextInt(height / 2) + 150,
                        15
                    );
                }
            }
            playMagicChimeSound();
        }
    }

    private void triggerSpellVibrator() {
        try {
            Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vib != null) {
                vib.vibrate(120);
            }
        } catch (Exception e) {
            // Ignore if vibration permission or hardware missing
        }
    }

    private void playCastSoundThrottled() {
        long now = System.currentTimeMillis();
        if (now - lastToneTime > 250) {
            lastToneTime = now;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_MUSIC, 75);
                        tg.startTone(ToneGenerator.TONE_PROP_BEEP, 100);
                        Thread.sleep(110);
                        tg.release();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    private void playMagicChimeSound() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
                    int[] chimeNotes = {
                        ToneGenerator.TONE_CDMA_PIP,
                        ToneGenerator.TONE_CDMA_HIGH_L,
                        ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD
                    };
                    for (int tone : chimeNotes) {
                        tg.startTone(tone, 120);
                        Thread.sleep(90);
                    }
                    tg.release();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }
}