package com.edgeborderlight.app;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final int OVERLAY_PERMISSION_REQ_CODE = 1001;
    private static final int NOTIFICATION_PERMISSION_REQ_CODE = 1002;

    private Switch toggleSwitch;
    private TextView tvStatus;
    
    private SeekBar sbWidth, sbSpeed, sbRadius;
    private TextView tvWidthLabel, tvSpeedLabel, tvRadiusLabel;
    
    private RadioGroup rgColorStyles;

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("EdgeBorderLightPrefs", Context.MODE_PRIVATE);

        initViews();
        loadSavedConfig();
        setupListeners();
    }

    private void initViews() {
        toggleSwitch = (Switch) findViewById(R.id.switch_light_toggle);
        tvStatus = (TextView) findViewById(R.id.tv_service_status);

        sbWidth = (SeekBar) findViewById(R.id.sb_border_width);
        sbSpeed = (SeekBar) findViewById(R.id.sb_speed);
        sbRadius = (SeekBar) findViewById(R.id.sb_radius);

        tvWidthLabel = (TextView) findViewById(R.id.tv_label_width);
        tvSpeedLabel = (TextView) findViewById(R.id.tv_label_speed);
        tvRadiusLabel = (TextView) findViewById(R.id.tv_label_radius);

        rgColorStyles = (RadioGroup) findViewById(R.id.rg_color_styles);
    }

    private void loadSavedConfig() {
        int width = prefs.getInt("width", 12);
        int speed = prefs.getInt("speed", 5);
        int radius = prefs.getInt("radius", 45);
        int style = prefs.getInt("style", 0);

        sbWidth.setProgress(width);
        sbSpeed.setProgress(speed);
        sbRadius.setProgress(radius);

        tvWidthLabel.setText("Border Width: " + width + "dp");
        tvSpeedLabel.setText("Animation Speed: " + speed);
        tvRadiusLabel.setText("Border Corner Radius: " + radius + "dp");

        switch (style) {
            case 1:
                rgColorStyles.check(R.id.rb_style_blue_purple);
                break;
            case 2:
                rgColorStyles.check(R.id.rb_style_fire);
                break;
            case 3:
                rgColorStyles.check(R.id.rb_style_aurora);
                break;
            case 4:
                rgColorStyles.check(R.id.rb_style_pastel);
                break;
            case 0:
            default:
                rgColorStyles.check(R.id.rb_style_rainbow);
                break;
        }

        updateStatusUI();
    }

    private void updateStatusUI() {
        if (BorderLightService.isRunning) {
            toggleSwitch.setChecked(true);
            tvStatus.setText("Status: Enabled");
            tvStatus.setTextColor(0xFF4CAF50); // Green color
        } else {
            toggleSwitch.setChecked(false);
            tvStatus.setText("Status: Disabled");
            tvStatus.setTextColor(0xFFFF5252); // Red color
        }
    }

    private void setupListeners() {
        toggleSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    checkAndRequestPermissions();
                } else {
                    stopEdgeLighting();
                }
            }
        });

        sbWidth.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress < 2) {
                    progress = 2; // Keep a visible limit
                    sbWidth.setProgress(2);
                }
                tvWidthLabel.setText("Border Width: " + progress + "dp");
                saveConfigInt("width", progress);
                syncServiceParams();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        sbSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress < 1) {
                    progress = 1; // Keep animation alive
                    sbSpeed.setProgress(1);
                }
                tvSpeedLabel.setText("Animation Speed: " + progress);
                saveConfigInt("speed", progress);
                syncServiceParams();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        sbRadius.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvRadiusLabel.setText("Border Corner Radius: " + progress + "dp");
                saveConfigInt("radius", progress);
                syncServiceParams();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        rgColorStyles.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                int styleId = 0;
                if (checkedId == R.id.rb_style_blue_purple) {
                    styleId = 1;
                } else if (checkedId == R.id.rb_style_fire) {
                    styleId = 2;
                } else if (checkedId == R.id.rb_style_aurora) {
                    styleId = 3;
                } else if (checkedId == R.id.rb_style_pastel) {
                    styleId = 4;
                }
                saveConfigInt("style", styleId);
                syncServiceParams();
            }
        });
    }

    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Permit Overlay drawing option is required to run in background.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE);
        } else {
            checkNotificationPermission();
        }
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQ_CODE);
            } else {
                startEdgeLighting();
            }
        } else {
            startEdgeLighting();
        }
    }

    private void startEdgeLighting() {
        Intent intent = new Intent(this, BorderLightService.class);
        intent.putExtra("width", sbWidth.getProgress());
        intent.putExtra("speed", sbSpeed.getProgress());
        intent.putExtra("radius", sbRadius.getProgress());
        intent.putExtra("style", getSelectedStyle());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        updateStatusUI();
    }

    private void stopEdgeLighting() {
        Intent intent = new Intent(this, BorderLightService.class);
        stopService(intent);
        updateStatusUI();
    }

    private void syncServiceParams() {
        if (BorderLightService.isRunning) {
            // Hot swap configuration without restarting foreground state
            startEdgeLighting();
        }
    }

    private int getSelectedStyle() {
        int checked = rgColorStyles.getCheckedRadioButtonId();
        if (checked == R.id.rb_style_blue_purple) return 1;
        if (checked == R.id.rb_style_fire) return 2;
        if (checked == R.id.rb_style_aurora) return 3;
        if (checked == R.id.rb_style_pastel) return 4;
        return 0;
    }

    private void saveConfigInt(String key, int val) {
        prefs.edit().putInt(key, val).apply();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatusUI();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_PERMISSION_REQ_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                checkNotificationPermission();
            } else {
                Toast.makeText(this, "Permission to overlay was denied. Border Light cannot start.", Toast.LENGTH_SHORT).show();
                updateStatusUI();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQ_CODE) {
            // Notification permission is good to have for UI on modern versions, but continue anyway
            startEdgeLighting();
        }
    }
}