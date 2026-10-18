package com.arbajsbz.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import org.json.JSONObject;

public class SettingsActivity extends Activity {

    private Switch switchSound;
    private Switch switchMusic;
    private SoundManager soundManager;
    private BackendApi backendApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        soundManager = SoundManager.getInstance(this);
        backendApi = new BackendApi(this);

        switchSound = (Switch) findViewById(R.id.switch_sound);
        switchMusic = (Switch) findViewById(R.id.switch_music);

        switchSound.setChecked(soundManager.isSoundEnabled());
        switchMusic.setChecked(soundManager.isMusicEnabled());

        findViewById(R.id.btn_settings_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        switchSound.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                soundManager.setSoundEnabled(isChecked);
                Toast.makeText(SettingsActivity.this, "Sound Effects: " + (isChecked ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
            }
        });

        switchMusic.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                soundManager.setMusicEnabled(isChecked);
                Toast.makeText(SettingsActivity.this, "Music: " + (isChecked ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btn_logout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                backendApi.logout(new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onError(String message) {
                        Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                });
            }
        });
    }
}