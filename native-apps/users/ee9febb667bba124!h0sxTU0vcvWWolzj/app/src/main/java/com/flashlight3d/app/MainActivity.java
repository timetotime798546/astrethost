package com.flashlight3d.app;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.Toast;

public class MainActivity extends Activity {

    private ToggleButton toggleButton3D;
    private ImageView bulbStateView;
    private TextView textStatus;
    private TextView textStrobeLabel;
    private SeekBar strobeSeekBar;

    private CameraManager cameraManager;
    private String cameraId;
    private boolean isFlashAvailable;
    private boolean isPowerOn = false;
    private boolean isActualFlashOn = false;

    // Strobe management variables
    private Handler strobeHandler = new Handler();
    private int strobeInterval = 0; // 0 = off
    private boolean isStrobeRunning = false;

    // Strobe Thread Runner Loop
    private final Runnable strobeRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isPowerOn || strobeInterval == 0) {
                isStrobeRunning = false;
                return;
            }
            try {
                isActualFlashOn = !isActualFlashOn;
                setTorch(isActualFlashOn);
                // Update bulb state to show flashing visually on screen
                updateBulbImage(isActualFlashOn);
                strobeHandler.postDelayed(this, strobeInterval);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI Initialization
        toggleButton3D = (ToggleButton) findViewById(R.id.btnToggle3d);
        bulbStateView = (ImageView) findViewById(R.id.bulbStateView);
        textStatus = (TextView) findViewById(R.id.textStatus);
        textStrobeLabel = (TextView) findViewById(R.id.textStrobeLabel);
        strobeSeekBar = (SeekBar) findViewById(R.id.strobeSeekBar);

        // Hardware initialization
        isFlashAvailable = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if (cameraManager != null) {
                String[] list = cameraManager.getCameraIdList();
                if (list.length > 0) {
                    cameraId = list[0];
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
            isFlashAvailable = false;
        }

        // Power toggle listener
        toggleButton3D.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isPowerOn = isChecked;
                SoundGenerator.playTactileClick(isPowerOn);
                
                if (isPowerOn) {
                    textStatus.setText("POWER: ON");
                    textStatus.setTextColor(getResources().getColor(R.color.led_glow));
                    keepScreenOn(true);
                    
                    if (strobeInterval > 0) {
                        startStrobe();
                    } else {
                        isActualFlashOn = true;
                        setTorch(true);
                        updateBulbImage(true);
                    }
                } else {
                    textStatus.setText("POWER: OFF");
                    textStatus.setTextColor(getResources().getColor(R.color.text_dim));
                    keepScreenOn(false);
                    stopStrobe();
                    isActualFlashOn = false;
                    setTorch(false);
                    updateBulbImage(false);
                }
            }
        });

        // Strobe controller progress bar changes
        strobeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress == 0) {
                    strobeInterval = 0;
                    textStrobeLabel.setText("STROBE SPEED: OFF");
                    if (isPowerOn) {
                        stopStrobe();
                        isActualFlashOn = true;
                        setTorch(true);
                        updateBulbImage(true);
                    }
                } else {
                    // Strobe speed formula: progress 1 is slow, 10 is fast
                    strobeInterval = 600 - (progress * 55); 
                    textStrobeLabel.setText("STROBE SPEED: " + progress + " Hz");
                    
                    if (isPowerOn) {
                        if (!isStrobeRunning) {
                            startStrobe();
                        }
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // System Compatibility Fallback Warning
        if (!isFlashAvailable) {
            Toast.makeText(this, "Physical Flash LED absent. Using Screen brightness backup.", Toast.LENGTH_LONG).show();
        }
    }

    private void startStrobe() {
        strobeHandler.removeCallbacks(strobeRunnable);
        isStrobeRunning = true;
        strobeHandler.post(strobeRunnable);
    }

    private void stopStrobe() {
        strobeHandler.removeCallbacks(strobeRunnable);
        isStrobeRunning = false;
    }

    private void setTorch(boolean enabled) {
        if (!isFlashAvailable || cameraId == null) {
            // Screen-based backup illumination if camera hardware LED doesn't exist
            adjustScreenBrightness(enabled);
            return;
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cameraManager.setTorchMode(cameraId, enabled);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateBulbImage(final boolean isOn) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isOn) {
                    bulbStateView.setImageResource(R.drawable.bulb_on);
                } else {
                    bulbStateView.setImageResource(R.drawable.bulb_off);
                }
            }
        });
    }

    private void adjustScreenBrightness(boolean maximum) {
        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        if (maximum) {
            layoutParams.screenBrightness = 1.0f; // Max white screen glare
        } else {
            layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE; // Reset
        }
        getWindow().setAttributes(layoutParams);
    }

    private void keepScreenOn(boolean active) {
        if (active) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Turn off all illumination elements during app suspension to save hardware lifetime
        if (isPowerOn) {
            toggleButton3D.setChecked(false);
        }
    }
}