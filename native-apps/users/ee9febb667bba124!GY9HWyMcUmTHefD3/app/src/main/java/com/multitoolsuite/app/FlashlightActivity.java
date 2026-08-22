package com.multitoolsuite.app;

import android.app.Activity;
import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class FlashlightActivity extends Activity {
    private RelativeLayout rootLayout;
    private Button btnToggle;
    private TextView tvStatus;
    private boolean isLightOn = false;
    private CameraManager cameraManager;
    private String cameraId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flashlight);

        rootLayout = (RelativeLayout) findViewById(R.id.root_flashlight);
        btnToggle = (Button) findViewById(R.id.btn_flashlight_toggle);
        tvStatus = (TextView) findViewById(R.id.tv_flashlight_status);

        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if (cameraManager != null) {
                String[] ids = cameraManager.getCameraIdList();
                if (ids.length > 0) {
                    cameraId = ids[0];
                }
            }
        } catch (Exception e) {
            // Flashlight hardware might not be accessible
        }

        btnToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isLightOn = !isLightOn;
                updateFlashlight(isLightOn);
            }
        });
    }

    private void updateFlashlight(boolean state) {
        if (cameraId != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    cameraManager.setTorchMode(cameraId, state);
                }
            } catch (Exception e) {
                // ignore and fall back to screen light
            }
        }

        if (state) {
            btnToggle.setText("TURN OFF");
            tvStatus.setText("Flashlight ACTIVE");
            rootLayout.setBackgroundColor(0xFFFFFFFF);
            setScreenBrightness(1.0f);
        } else {
            btnToggle.setText("TURN ON");
            tvStatus.setText("Flashlight INACTIVE");
            rootLayout.setBackgroundColor(0xFF121212);
            setScreenBrightness(-1.0f);
        }
    }

    private void setScreenBrightness(float value) {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = value;
        getWindow().setAttributes(lp);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isLightOn) {
            updateFlashlight(false);
        }
    }
}
