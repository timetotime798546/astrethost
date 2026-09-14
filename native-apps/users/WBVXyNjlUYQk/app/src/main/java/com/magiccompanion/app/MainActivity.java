package com.magiccompanion.app;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import java.util.Random;

public class MainActivity extends Activity implements SensorEventListener {

    private MagicWandView wandView;
    private CauldronView cauldronView;
    private EightBallView oracleView;

    private RelativeLayout moduleWand, moduleCauldron, moduleOracle;
    private LinearLayout tabWand, tabCauldron, tabOracle;
    private TextView txtTabWand, txtTabCauldron, txtTabOracle;

    private Spinner spinner1, spinner2;
    private Button btnBrew;
    private LinearLayout layoutPotionResult;
    private TextView txtPotionName, txtPotionDesc, txtResultStatus;

    private Button btnSpellLumos, btnSpellIncendio, btnSpellGlacius, btnSpellAlohomora;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private long lastUpdate = 0;
    private float last_x, last_y, last_z;
    private static final int SHAKE_THRESHOLD = 800;

    private Random random;
    private Vibrator vibrator;

    private static final String[] INGREDIENTS = {
        "Phoenix Tear", "Dragon Scale", "Unicorn Hair", "Nightshade", "Stardust", "Mandragora Root"
    };

    private static final String[] PROPHECIES = {
        "The stars align perfectly in your favor.",
        "A heavy dark shadow shrouds this destiny.",
        "Magical energies are chaotic. Ask again.",
        "Fortune smiles upon the bold sorcerer.",
        "An unexpected ancient ally will materialize.",
        "The wizarding high council foresees pure light.",
        "Beware of illusions woven by your close rivals.",
        "The ritual succeeds beyond your imagination."
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        random = new Random();
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        wandView = findViewById(R.id.wand_view);
        cauldronView = findViewById(R.id.cauldron_view);
        oracleView = findViewById(R.id.oracle_view);

        moduleWand = findViewById(R.id.module_wand);
        moduleCauldron = findViewById(R.id.module_cauldron);
        moduleOracle = findViewById(R.id.module_oracle);

        tabWand = findViewById(R.id.tab_wand);
        tabCauldron = findViewById(R.id.tab_cauldron);
        tabOracle = findViewById(R.id.tab_oracle);

        txtTabWand = findViewById(R.id.txt_tab_wand);
        txtTabCauldron = findViewById(R.id.txt_tab_cauldron);
        txtTabOracle = findViewById(R.id.txt_tab_oracle);

        btnSpellLumos = findViewById(R.id.btn_spell_lumos);
        btnSpellIncendio = findViewById(R.id.btn_spell_incendio);
        btnSpellGlacius = findViewById(R.id.btn_spell_glacius);
        btnSpellAlohomora = findViewById(R.id.btn_spell_alohomora);

        spinner1 = findViewById(R.id.spinner_ingredient1);
        spinner2 = findViewById(R.id.spinner_ingredient2);
        btnBrew = findViewById(R.id.btn_brew);
        layoutPotionResult = findViewById(R.id.layout_potion_result);
        txtPotionName = findViewById(R.id.txt_potion_name);
        txtPotionDesc = findViewById(R.id.txt_potion_desc);
        txtResultStatus = findViewById(R.id.txt_result_status);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, INGREDIENTS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner1.setAdapter(adapter);
        spinner2.setAdapter(adapter);

        tabWand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(0);
            }
        });

        tabCauldron.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(1);
            }
        });

        tabOracle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(2);
            }
        });

        btnSpellLumos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectSpell(0);
            }
        });

        btnSpellIncendio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectSpell(1);
            }
        });

        btnSpellGlacius.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectSpell(2);
            }
        });

        btnSpellAlohomora.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectSpell(3);
            }
        });

        btnBrew.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerPotionBrewing();
            }
        });

        oracleView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerOracleProphecy();
            }
        });

        selectSpell(0);
    }

    private void switchTab(int index) {
        tabWand.setBackgroundColor(Color.TRANSPARENT);
        tabCauldron.setBackgroundColor(Color.TRANSPARENT);
        tabOracle.setBackgroundColor(Color.TRANSPARENT);

        txtTabWand.setTextColor(Color.parseColor("#a19cb0"));
        txtTabCauldron.setTextColor(Color.parseColor("#a19cb0"));
        txtTabOracle.setTextColor(Color.parseColor("#a19cb0"));

        moduleWand.setVisibility(View.GONE);
        moduleCauldron.setVisibility(View.GONE);
        moduleOracle.setVisibility(View.GONE);

        if (index == 0) {
            tabWand.setBackgroundColor(Color.parseColor("#2d1e4d"));
            txtTabWand.setTextColor(Color.WHITE);
            moduleWand.setVisibility(View.VISIBLE);
        } else if (index == 1) {
            tabCauldron.setBackgroundColor(Color.parseColor("#2d1e4d"));
            txtTabCauldron.setTextColor(Color.WHITE);
            moduleCauldron.setVisibility(View.VISIBLE);
        } else if (index == 2) {
            tabOracle.setBackgroundColor(Color.parseColor("#2d1e4d"));
            txtTabOracle.setTextColor(Color.WHITE);
            moduleOracle.setVisibility(View.VISIBLE);
        }
    }

    private void selectSpell(int type) {
        wandView.setSpellType(type);
        playMagicSound(type);

        btnSpellLumos.setBackgroundColor(Color.TRANSPARENT);
        btnSpellIncendio.setBackgroundColor(Color.TRANSPARENT);
        btnSpellGlacius.setBackgroundColor(Color.TRANSPARENT);
        btnSpellAlohomora.setBackgroundColor(Color.TRANSPARENT);

        int highlightColor = Color.parseColor("#443377");
        if (type == 0) btnSpellLumos.setBackgroundColor(highlightColor);
        else if (type == 1) btnSpellIncendio.setBackgroundColor(highlightColor);
        else if (type == 2) btnSpellGlacius.setBackgroundColor(highlightColor);
        else if (type == 3) btnSpellAlohomora.setBackgroundColor(highlightColor);
    }

    private void triggerPotionBrewing() {
        btnBrew.setEnabled(false);
        layoutPotionResult.setVisibility(View.INVISIBLE);

        String ing1 = spinner1.getSelectedItem().toString();
        String ing2 = spinner2.getSelectedItem().toString();

        final Potion result = PotionBook.getRecipe(ing1, ing2);

        cauldronView.startBrewing(result.color, 1500);
        playMagicSound(4);

        if (vibrator != null) {
            vibrator.vibrate(120);
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                btnBrew.setEnabled(true);
                txtResultStatus.setText("✨ ALCHEMY BREWING COMPLETE ✨");
                txtPotionName.setText(result.name);
                txtPotionDesc.setText(result.description);
                layoutPotionResult.setVisibility(View.VISIBLE);
                
                playMagicSound(3); 
                if (vibrator != null) {
                    vibrator.vibrate(new long[]{0, 80, 50, 80}, -1);
                }
            }
        }, 1500);
    }

    private void triggerOracleProphecy() {
        String msg = PROPHECIES[random.nextInt(PROPHECIES.length)];
        oracleView.triggerPrediction(msg);
        playMagicSound(5);

        if (vibrator != null) {
            vibrator.vibrate(350);
        }
    }

    private void playMagicSound(final int type) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ToneGenerator tg = new ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 85);
                    if (type == 0) {
                        tg.startTone(ToneGenerator.TONE_CDMA_PIP, 80);
                        Thread.sleep(100);
                        tg.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 100);
                        Thread.sleep(120);
                        tg.startTone(ToneGenerator.TONE_SUP_PIP, 150);
                    } else if (type == 1) {
                        tg.startTone(ToneGenerator.TONE_SUP_CONGESTION, 150);
                        Thread.sleep(100);
                        tg.startTone(ToneGenerator.TONE_SUP_CONGESTION, 150);
                    } else if (type == 2) {
                        tg.startTone(ToneGenerator.TONE_SUP_DIAL, 120);
                        Thread.sleep(180);
                        tg.startTone(ToneGenerator.TONE_SUP_DIAL, 120);
                    } else if (type == 3) {
                        tg.startTone(ToneGenerator.TONE_PROP_ACK, 100);
                        Thread.sleep(120);
                        tg.startTone(ToneGenerator.TONE_PROP_PROMPT, 150);
                    } else if (type == 4) {
                        for (int i = 0; i < 8; i++) {
                            tg.startTone(ToneGenerator.TONE_PROP_BEEP, 50);
                            Thread.sleep(120);
                        }
                    } else if (type == 5) {
                        tg.startTone(ToneGenerator.TONE_CDMA_LOW_L, 400);
                        Thread.sleep(400);
                        tg.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_SIGNAL, 250);
                    }
                    tg.release();
                } catch (Exception e) {
                    // Fail-safe
                }
            }
        }).start();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (moduleOracle.getVisibility() != View.VISIBLE) return;

        Sensor mySensor = event.sensor;
        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            long curTime = System.currentTimeMillis();
            if ((curTime - lastUpdate) > 100) {
                long diffTime = (curTime - lastUpdate);
                lastUpdate = curTime;

                float speed = Math.abs(x + y + z - last_x - last_y - last_z) / diffTime * 10000;
                if (speed > SHAKE_THRESHOLD) {
                    triggerOracleProphecy();
                }
                last_x = x;
                last_y = y;
                last_z = z;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No-op
    }

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

    public static class Potion {
        public String name;
        public String description;
        public int color;

        public Potion(String name, String description, int color) {
            this.name = name;
            this.description = description;
            this.color = color;
        }
    }

    public static class PotionBook {
        public static Potion getRecipe(String ing1, String ing2) {
            if (ing1.equals(ing2)) {
                return new Potion("Unstable Toxic Sludge", "An explosive, bubbling, bad-smelling brew. Better clean the cauldron!", Color.rgb(105, 102, 53));
            }

            if (hasPair(ing1, ing2, "Phoenix Tear", "Dragon Scale")) {
                return new Potion("Elixir of Immortality", "Fires up the soul. Grants endless longevity and absolute immunity to normal damage.", Color.rgb(255, 215, 0));
            }
            if (hasPair(ing1, ing2, "Phoenix Tear", "Unicorn Hair")) {
                return new Potion("Celestial Blessing Essence", "A glowing white potion that fills the air with pure, sweet cosmic energy.", Color.rgb(180, 255, 240));
            }
            if (hasPair(ing1, ing2, "Phoenix Tear", "Nightshade")) {
                return new Potion("Potion of Absolute Rebirth", "A deep, bubbling violet potion that restores fallen magical beasts to life.", Color.rgb(65, 10, 130));
            }
            if (hasPair(ing1, ing2, "Phoenix Tear", "Stardust")) {
                return new Potion("Cosmic Remedy", "A swirling starry liquid that cures any known magical affliction or hex.", Color.rgb(10, 140, 255));
            }
            if (hasPair(ing1, ing2, "Phoenix Tear", "Mandragora Root")) {
                return new Potion("Revitalization Tonic", "Restores full magical energy instantly. Tastes slightly of fresh lime.", Color.rgb(0, 210, 110));
            }

            if (hasPair(ing1, ing2, "Dragon Scale", "Unicorn Hair")) {
                return new Potion("Aegis Iron Shield Draught", "Hardens your skin like tempered diamond. Immune to physical weaponry.", Color.rgb(220, 120, 40));
            }
            if (hasPair(ing1, ing2, "Dragon Scale", "Nightshade")) {
                return new Potion("Dragon's Venom Breath", "A volatile crimson oil. Highly dangerous if touched or breathed.", Color.rgb(240, 10, 10));
            }
            if (hasPair(ing1, ing2, "Dragon Scale", "Stardust")) {
                return new Potion("Supernova Serum", "Unleashes absolute solar energy in the drinker's hands. Warning: very hot!", Color.rgb(255, 185, 0));
            }
            if (hasPair(ing1, ing2, "Dragon Scale", "Mandragora Root")) {
                return new Potion("Stone Skin Oil", "Increases raw defensive capabilities while slightly decreasing agility.", Color.rgb(140, 95, 45));
            }

            if (hasPair(ing1, ing2, "Unicorn Hair", "Nightshade")) {
                return new Potion("Phantasm Ethereal Elixir", "Allows the drinker to walk through solid stone structures like a specter.", Color.rgb(210, 160, 250));
            }
            if (hasPair(ing1, ing2, "Unicorn Hair", "Stardust")) {
                return new Potion("Starlight Essence", "A glowing silver nectar that illuminates even the deepest dungeons.", Color.rgb(230, 240, 255));
            }
            if (hasPair(ing1, ing2, "Unicorn Hair", "Mandragora Root")) {
                return new Potion("Sylvan Wood Panacea", "A natural herbal brew that bonds the caster's mind to the wisdom of the old trees.", Color.rgb(34, 139, 34));
            }

            if (hasPair(ing1, ing2, "Nightshade", "Stardust")) {
                return new Potion("Void Singularity Extract", "Contains gravity-bending cosmic darkness. Handle with extra caution.", Color.rgb(30, 5, 50));
            }
            if (hasPair(ing1, ing2, "Nightshade", "Mandragora Root")) {
                return new Potion("The Ultimate Sleeping Curse", "Triggers deep, dreamless slumber. Only a true kiss can wake the drinker.", Color.rgb(160, 0, 120));
            }

            if (hasPair(ing1, ing2, "Stardust", "Mandragora Root")) {
                return new Potion("Astral Plane Projection Decoction", "Separates consciousness from the physical body to travel parallel dimensions.", Color.rgb(150, 50, 230));
            }

            return new Potion("Curious Bubbling Infusion", "A strange potion of unknown effects. Smells faintly of toasted marshmallows.", Color.rgb(20, 200, 220));
        }

        private static boolean hasPair(String a, String b, String x, String y) {
            return (a.equals(x) && b.equals(y)) || (a.equals(y) && b.equals(x));
        }
    }
}