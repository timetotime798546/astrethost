package com.nitinlivewallpaper.app;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private EditText nameInput;
    private TextView textPreview;
    private RadioGroup colorGroup;
    private RadioGroup animGroup;
    private RadioGroup particleGroup;
    private RadioGroup speedGroup;
    private CheckBox checkRipple;
    private Button btnApply;

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("nitin_wallpaper_prefs", Context.MODE_PRIVATE);

        // Bind Views
        nameInput = (EditText) findViewById(R.id.nameInput);
        textPreview = (TextView) findViewById(R.id.textPreview);
        colorGroup = (RadioGroup) findViewById(R.id.colorGroup);
        animGroup = (RadioGroup) findViewById(R.id.animGroup);
        particleGroup = (RadioGroup) findViewById(R.id.particleGroup);
        speedGroup = (RadioGroup) findViewById(R.id.speedGroup);
        checkRipple = (CheckBox) findViewById(R.id.checkRipple);
        btnApply = (Button) findViewById(R.id.btnApply);

        // Load saved states
        loadSavedConfig();

        // Setup real-time listeners to update local configuration
        nameInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String val = s.toString().trim();
                if (val.isEmpty()) val = "नितिन";
                textPreview.setText(val);
                prefs.edit().putString("wallpaper_text", val).apply();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        colorGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                int index = 0;
                int accentColor = Color.rgb(80, 200, 255); // Default Blue
                if (checkedId == R.id.radioBlue) {
                    index = 0;
                    accentColor = Color.rgb(80, 200, 255);
                } else if (checkedId == R.id.radioRose) {
                    index = 1;
                    accentColor = Color.rgb(255, 100, 180);
                } else if (checkedId == R.id.radioGold) {
                    index = 2;
                    accentColor = Color.rgb(255, 220, 80);
                } else if (checkedId == R.id.radioPurple) {
                    index = 3;
                    accentColor = Color.rgb(255, 80, 255);
                } else if (checkedId == R.id.radioMint) {
                    index = 4;
                    accentColor = Color.rgb(80, 255, 200);
                }
                textPreview.setTextColor(accentColor);
                prefs.edit().putInt("color_scheme", index).apply();
            }
        });

        animGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                int style = 0;
                if (checkedId == R.id.animPulse) style = 0;
                else if (checkedId == R.id.animBounce) style = 1;
                else if (checkedId == R.id.animWave) style = 2;
                else if (checkedId == R.id.animFloat) style = 3;
                prefs.edit().putInt("anim_style", style).apply();
            }
        });

        particleGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                int part = 0;
                if (checkedId == R.id.partSparkle) part = 0;
                else if (checkedId == R.id.partHeart) part = 1;
                else if (checkedId == R.id.partBubble) part = 2;
                else if (checkedId == R.id.partNone) part = 3;
                prefs.edit().putInt("particle_style", part).apply();
            }
        });

        speedGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                int speed = 1;
                if (checkedId == R.id.speedSlow) speed = 0;
                else if (checkedId == R.id.speedMedium) speed = 1;
                else if (checkedId == R.id.speedFast) speed = 2;
                prefs.edit().putInt("particle_speed", speed).apply();
            }
        });

        checkRipple.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean("interactive_ripples", isChecked).apply();
            }
        });

        // Setup Wallpaper Picker trigger
        btnApply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerSetWallpaper();
            }
        });
    }

    private void loadSavedConfig() {
        String savedText = prefs.getString("wallpaper_text", "नितिन");
        nameInput.setText(savedText);
        textPreview.setText(savedText);

        int colorIdx = prefs.getInt("color_scheme", 0);
        int accentColor = Color.rgb(80, 200, 255);
        if (colorIdx == 0) {
            colorGroup.check(R.id.radioBlue);
            accentColor = Color.rgb(80, 200, 255);
        } else if (colorIdx == 1) {
            colorGroup.check(R.id.radioRose);
            accentColor = Color.rgb(255, 100, 180);
        } else if (colorIdx == 2) {
            colorGroup.check(R.id.radioGold);
            accentColor = Color.rgb(255, 220, 80);
        } else if (colorIdx == 3) {
            colorGroup.check(R.id.radioPurple);
            accentColor = Color.rgb(255, 80, 255);
        } else if (colorIdx == 4) {
            colorGroup.check(R.id.radioMint);
            accentColor = Color.rgb(80, 255, 200);
        }
        textPreview.setTextColor(accentColor);

        int animIdx = prefs.getInt("anim_style", 0);
        if (animIdx == 0) animGroup.check(R.id.animPulse);
        else if (animIdx == 1) animGroup.check(R.id.animBounce);
        else if (animIdx == 2) animGroup.check(R.id.animWave);
        else if (animIdx == 3) animGroup.check(R.id.animFloat);

        int partIdx = prefs.getInt("particle_style", 0);
        if (partIdx == 0) particleGroup.check(R.id.partSparkle);
        else if (partIdx == 1) particleGroup.check(R.id.partHeart);
        else if (partIdx == 2) particleGroup.check(R.id.partBubble);
        else if (partIdx == 3) particleGroup.check(R.id.partNone);

        int speedIdx = prefs.getInt("particle_speed", 1);
        if (speedIdx == 0) speedGroup.check(R.id.speedSlow);
        else if (speedIdx == 1) speedGroup.check(R.id.speedMedium);
        else if (speedIdx == 2) speedGroup.check(R.id.speedFast);

        checkRipple.setChecked(prefs.getBoolean("interactive_ripples", true));
    }

    private void triggerSetWallpaper() {
        try {
            Intent intent = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
            intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    new ComponentName(getPackageName(), NitinWallpaperService.class.getName()));
            startActivity(intent);
        } catch (Exception e) {
            try {
                // Alternative intent format in older or custom OS
                Intent intent = new Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER);
                startActivity(intent);
            } catch (Exception ex) {
                Toast.makeText(this, "लाइव वॉलपेपर सेटिंग्स खोलने में असमर्थ। कृपया फ़ोन की सेटिंग्स से लगाएं।", Toast.LENGTH_LONG).show();
            }
        }
    }
}