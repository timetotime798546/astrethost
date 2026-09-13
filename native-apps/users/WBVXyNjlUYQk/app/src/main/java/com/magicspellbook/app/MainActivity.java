package com.magicspellbook.app;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Random;

public class MainActivity extends Activity implements SensorEventListener {

    // Tab Views
    private LinearLayout layoutTabWand;
    private ScrollViewWrapper layoutTabSpells; // Will cast layout dynamically safely
    private View layoutTabSpellsRaw;
    private LinearLayout layoutTabPotion;
    private LinearLayout layoutTabOracle;

    private Button btnWand, btnSpells, btnPotion, btnOracle;

    // Custom Sparkle view inside Wand tab
    private MagicWandView magicWandView;

    // Potion Mixer Components
    private CheckBox chkPhoenix, chkDragon, chkFairy, chkMoonstone;
    private Button btnBrew;
    private LinearLayout potionCauldron;
    private TextView txtPotionState, txtPotionDesc;

    // Oracle Components
    private View btnOracleSphere;
    private TextView txtOracleResponse;

    // Accelerometer Sensors
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private float lastX, lastY, lastZ;
    private long lastSensorUpdate = 0;
    private static final int SHAKE_THRESHOLD = 800;

    // Vibrator
    private Vibrator vibrator;

    // Oracle response list
    private String[] oracleAnswers = {
        "The Ancient Stars align in your favor!",
        "A dark shadow clouds your answer. Seek wisdom.",
        "Yes, the magical currents confirm it!",
        "The potion is still bubbling, ask again later.",
        "Alas, the wizard council says NO.",
        "Your magic reserves are too low to perceive.",
        "A sudden stroke of magical luck is coming!",
        "Look inside your own spellbook for the truth."
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize general system modules safely
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // Initialize layouts
        layoutTabWand = (LinearLayout) findViewById(R.id.layout_tab_wand);
        layoutTabSpellsRaw = findViewById(R.id.layout_tab_spells);
        layoutTabPotion = (LinearLayout) findViewById(R.id.layout_tab_potion);
        layoutTabOracle = (LinearLayout) findViewById(R.id.layout_tab_oracle);

        // Initialize tabs buttons
        btnWand = (Button) findViewById(R.id.btn_tab_wand);
        btnSpells = (Button) findViewById(R.id.btn_tab_spells);
        btnPotion = (Button) findViewById(R.id.btn_tab_potion);
        btnOracle = (Button) findViewById(R.id.btn_tab_oracle);

        // Magic Wand canvas
        magicWandView = (MagicWandView) findViewById(R.id.magic_wand_canvas);

        // Setup Tab switching triggers
        btnWand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(0);
            }
        });
        btnSpells.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(1);
            }
        });
        btnPotion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(2);
            }
        });
        btnOracle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(3);
            }
        });

        // Spell cards setup
        findViewById(R.id.spell_lumos).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerSpellEffect("Lumos Maxima", Color.WHITE, 1200);
            }
        });
        findViewById(R.id.spell_pyro).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerSpellEffect("Pyrokinesis", Color.parseColor("#FF1744"), 800);
            }
        });
        findViewById(R.id.spell_tempest).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerSpellEffect("Lightning Tempest", Color.parseColor("#00E5FF"), 1500);
            }
        });
        findViewById(R.id.spell_levitate).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerSpellEffect("Wingardium Leviosa", Color.parseColor("#00E676"), 600);
            }
        });

        // Potion Mixing views
        chkPhoenix = (CheckBox) findViewById(R.id.chk_phoenix_feather);
        chkDragon = (CheckBox) findViewById(R.id.chk_dragon_scale);
        chkFairy = (CheckBox) findViewById(R.id.chk_fairy_dust);
        chkMoonstone = (CheckBox) findViewById(R.id.chk_moonstone);
        btnBrew = (Button) findViewById(R.id.btn_brew);
        potionCauldron = (LinearLayout) findViewById(R.id.potion_cauldron);
        txtPotionState = (TextView) findViewById(R.id.txt_potion_state);
        txtPotionDesc = (TextView) findViewById(R.id.txt_potion_desc);

        btnBrew.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                brewPotion();
            }
        });

        // Oracle Views
        btnOracleSphere = findViewById(R.id.btn_oracle_sphere);
        txtOracleResponse = (TextView) findViewById(R.id.txt_oracle_response);
        btnOracleSphere.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                consultOracle();
            }
        });

        // Default screen state
        switchTab(0);
    }

    private void switchTab(int index) {
        layoutTabWand.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        layoutTabSpellsRaw.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        layoutTabPotion.setVisibility(index == 2 ? View.VISIBLE : View.GONE);
        layoutTabOracle.setVisibility(index == 3 ? View.VISIBLE : View.GONE);

        // Highlight selected tab visually using basic colors
        btnWand.setBackgroundColor(index == 0 ? Color.parseColor("#4A154B") : Color.parseColor("#18162D"));
        btnSpells.setBackgroundColor(index == 1 ? Color.parseColor("#4A154B") : Color.parseColor("#18162D"));
        btnPotion.setBackgroundColor(index == 2 ? Color.parseColor("#4A154B") : Color.parseColor("#18162D"));
        btnOracle.setBackgroundColor(index == 3 ? Color.parseColor("#4A154B") : Color.parseColor("#18162D"));

        triggerHapticFeedback(20);
    }

    private void triggerSpellEffect(String name, final int spellColor, final int soundFreq) {
        Toast.makeText(this, "Casting " + name + "!", Toast.LENGTH_SHORT).show();
        triggerHapticFeedback(150);

        // Visual feedback inside active tab: force particles
        magicWandView.addSpellParticles(spellColor);

        // Flash screen background momentarily using a brief animation sequence
        final View decorView = getWindow().getDecorView();
        final int oldColor = Color.parseColor("#0A0915");
        decorView.setBackgroundColor(spellColor);

        decorView.postDelayed(new Runnable() {
            @Override
            public void run() {
                decorView.setBackgroundColor(oldColor);
            }
        }, 150);

        // Synthesize native audio magic sweep
        playDynamicMagicSound(soundFreq);
    }

    private void brewPotion() {
        // Count active ingredients
        int checkedCount = 0;
        if (chkPhoenix.isChecked()) checkedCount++;
        if (chkDragon.isChecked()) checkedCount++;
        if (chkFairy.isChecked()) checkedCount++;
        if (chkMoonstone.isChecked()) checkedCount++;

        if (checkedCount != 2) {
            Toast.makeText(this, "You must select EXACTLY 2 mystical ingredients!", Toast.LENGTH_LONG).show();
            return;
        }

        triggerHapticFeedback(300);

        // Determine dynamic brew outcomes
        String potionName = "Unknown Sludge";
        String description = "A volatile bubbling soup that tastes like burnt wood.";
        int backgroundMysticColor = Color.parseColor("#303F9F");

        if (chkPhoenix.isChecked() && chkDragon.isChecked()) {
            potionName = "Elixir of Pyros";
            description = "Unleashes infinite warm dragon rage within. Glowing and dangerously hot!";
            backgroundMysticColor = Color.parseColor("#D84315");
        } else if (chkPhoenix.isChecked() && chkFairy.isChecked()) {
            potionName = "Grace of the Phoenix";
            description = "Tears of restoration! Healing energy surges through your magical veins.";
            backgroundMysticColor = Color.parseColor("#E91E63");
        } else if (chkPhoenix.isChecked() && chkMoonstone.isChecked()) {
            potionName = "Cosmic Aurora tonic";
            description = "Flashes of stellar light illuminate your vision, granting ancient luck.";
            backgroundMysticColor = Color.parseColor("#6A1B9A");
        } else if (chkDragon.isChecked() && chkFairy.isChecked()) {
            potionName = "Emerald Pixie Brew";
            description = "Extremely light and effervescent. Causes temporary gravity defying levitation!";
            backgroundMysticColor = Color.parseColor("#2E7D32");
        } else if (chkDragon.isChecked() && chkMoonstone.isChecked()) {
            potionName = "Titan Stone Shield";
            description = "Hardens skin like pure crystalline dragon stone. Perfect protection.";
            backgroundMysticColor = Color.parseColor("#37474F");
        } else if (chkFairy.isChecked() && chkMoonstone.isChecked()) {
            potionName = "Starlight Dew";
            description = "A glittering cyan fluid containing pure essence of night sky magic.";
            backgroundMysticColor = Color.parseColor("#00838F");
        }

        // Apply visual and sound feedback
        potionCauldron.setBackgroundColor(backgroundMysticColor);
        txtPotionState.setText("🧪 SUCCESS: " + potionName);
        txtPotionState.setTextColor(Color.WHITE);
        txtPotionDesc.setText(description);
        txtPotionDesc.setTextColor(Color.WHITE);

        // Bubble sound sequence
        playDynamicMagicSound(350);
        potionCauldron.postDelayed(new Runnable() {
            @Override
            public void run() {
                playDynamicMagicSound(600);
            }
        }, 120);
    }

    private void consultOracle() {
        // Simple visual scaling fade animation using AlphaAnimation
        AlphaAnimation fadeOut = new AlphaAnimation(1.0f, 0.1f);
        fadeOut.setDuration(300);
        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                Random rand = new Random();
                String chosenAnswer = oracleAnswers[rand.nextInt(oracleAnswers.length)];
                txtOracleResponse.setText("🔮\n" + chosenAnswer);

                AlphaAnimation fadeIn = new AlphaAnimation(0.1f, 1.0f);
                fadeIn.setDuration(500);
                txtOracleResponse.startAnimation(fadeIn);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });

        txtOracleResponse.startAnimation(fadeOut);
        triggerHapticFeedback(100);
        playDynamicMagicSound(900);
    }

    // Dynamic wave sound synthesizer via low-level standard AudioTrack
    // This allows custom frequencies without storing any asset footprint!
    private void playDynamicMagicSound(final int startingFreq) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                int sampleRate = 8000;
                int duration = 1600; // samples
                double[] sample = new double[duration];
                byte[] generatedSnd = new byte[2 * duration];

                for (int i = 0; i < duration; ++i) {
                    // Sweep pitch upwards smoothly to create a "magical rise" sound effect
                    double currentFreq = startingFreq + (i * 0.7);
                    sample[i] = Math.sin(2 * Math.PI * i / (sampleRate / currentFreq));
                }

                int idx = 0;
                for (final double dVal : sample) {
                    // Fade out near the end
                    double scale = 1.0;
                    if (idx > (duration * 1.5)) {
                        scale = (duration * 2.0 - idx) / (double) (duration * 0.5);
                    }
                    final short val = (short) ((dVal * 32767) * Math.max(0.0, Math.min(1.0, scale)));
                    generatedSnd[idx++] = (byte) (val & 0x00ff);
                    generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
                }

                try {
                    AudioTrack audioTrack = new AudioTrack(
                        AudioManager.STREAM_MUSIC,
                        sampleRate,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        generatedSnd.length,
                        AudioTrack.MODE_STATIC
                    );
                    audioTrack.write(generatedSnd, 0, generatedSnd.length);
                    audioTrack.play();
                    // Release audioTrack resources after simple playback
                    Thread.sleep(250);
                    audioTrack.release();
                } catch (Exception e) {
                    // Fail silently, sound generation is auxiliary
                }
            }
        }).start();
    }

    private void triggerHapticFeedback(int milliseconds) {
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(milliseconds);
        }
    }

    // Accelerometer Handlers
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            long curTime = System.currentTimeMillis();
            if ((curTime - lastSensorUpdate) > 100) {
                long diffTime = (curTime - lastSensorUpdate);
                lastSensorUpdate = curTime;

                float speed = Math.abs(x + y + z - lastX - lastY - lastZ) / diffTime * 10000;

                if (speed > SHAKE_THRESHOLD) {
                    // Spark phone flash/vibrate & feed wand canvas
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // If tab is oracle, trigger crystal response
                            if (layoutTabOracle.getVisibility() == View.VISIBLE) {
                                consultOracle();
                            } else {
                                triggerSpellEffect("Wand Wave Sparks", Color.parseColor("#FFD600"), 700);
                            }
                        }
                    });
                }

                lastX = x;
                lastY = y;
                lastZ = z;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
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