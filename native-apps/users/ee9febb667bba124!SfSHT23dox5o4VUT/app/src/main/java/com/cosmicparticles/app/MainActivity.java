package com.cosmicparticles.app;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Toast;

public class MainActivity extends Activity {

    public static final String PREFS_NAME = "CosmicWallpaperSettings";
    public static final String KEY_THEME = "theme_color";
    public static final String KEY_SPEED = "particle_speed";
    public static final String KEY_COUNT = "particle_count";
    public static final String KEY_TOUCH = "touch_enabled";

    public static final int THEME_BLUE = 0;
    public static final int THEME_PURPLE = 1;
    public static final int THEME_EMERALD = 2;
    public static final int THEME_FIRE = 3;

    private RadioGroup themeGroup;
    private SeekBar seekBarSpeed;
    private SeekBar seekBarCount;
    private CheckBox chkTouch;
    private Button btnApplySet;

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) { 
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        themeGroup = (RadioGroup) findViewById(R.id.themeGroup);
        seekBarSpeed = (SeekBar) findViewById(R.id.seekBarSpeed);
        seekBarCount = (SeekBar) findViewById(R.id.seekBarCount);
        chkTouch = (CheckBox) findViewById(R.id.chkTouch);
        btnApplySet = (Button) findViewById(R.id.btnApplySet);

        loadSettings();

        btnApplySet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSettings();
                triggerWallpaperSelection();
            }
        });
    }

    private void loadSettings() {
        int theme = prefs.getInt(KEY_THEME, THEME_BLUE);
        int speed = prefs.getInt(KEY_SPEED, 5);
        int count = prefs.getInt(KEY_COUNT, 80);
        boolean touch = prefs.getBoolean(KEY_TOUCH, true);

        if (theme == THEME_BLUE) {
            themeGroup.check(R.id.radioBlue);
        } else if (theme == THEME_PURPLE) {
            themeGroup.check(R.id.radioPurple);
        } else if (theme == THEME_EMERALD) {
            themeGroup.check(R.id.radioEmerald);
        } else if (theme == THEME_FIRE) {
            themeGroup.check(R.id.radioFire);
        }

        seekBarSpeed.setProgress(speed);
        seekBarCount.setProgress(count);
        chkTouch.setChecked(touch);
    }

    private void saveSettings() {
        SharedPreferences.Editor editor = prefs.edit();

        int selectedId = themeGroup.getCheckedRadioButtonId();
        int theme = THEME_BLUE;
        if (selectedId == R.id.radioBlue) {
            theme = THEME_BLUE;
        } else if (selectedId == R.id.radioPurple) {
            theme = THEME_PURPLE;
        } else if (selectedId == R.id.radioEmerald) {
            theme = THEME_EMERALD;
        } else if (selectedId == R.id.radioFire) {
            theme = THEME_FIRE;
        }

        editor.putInt(KEY_THEME, theme);
        editor.putInt(KEY_SPEED, seekBarSpeed.getProgress());
        editor.putInt(KEY_COUNT, seekBarCount.getProgress());
        editor.putBoolean(KEY_TOUCH, chkTouch.isChecked());
        editor.apply();

        Toast.makeText(this, "Settings Saved!", Toast.LENGTH_SHORT).show();
    }

    private void triggerWallpaperSelection() {
        Intent intent = new Intent();
        try {
            intent.setAction(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
            intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    new ComponentName(this, CosmicWallpaperService.class));
            startActivity(intent);
        } catch (Exception e) {
            try {
                intent.setAction(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER);
                startActivity(intent);
            } catch (Exception ex) {
                Toast.makeText(this, "Could not open Live Wallpaper selection. Please choose it manually in system settings.", Toast.LENGTH_LONG).show();
            }
        }
    }
}