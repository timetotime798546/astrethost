package com.aigirlfriendstream.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final int PERMISSION_REQUEST_CODE = 101;
    private TextView tvPermissionStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvPermissionStatus = (TextView) findViewById(R.id.tvPermissionStatus);
        LinearLayout btnModeGallery = (LinearLayout) findViewById(R.id.btnModeGallery);
        LinearLayout btnModeAi = (LinearLayout) findViewById(R.id.btnModeAi);

        checkAndRequestPermissions();

        btnModeGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (hasAllPermissions()) {
                    Intent intent = new Intent(MainActivity.this, GalleryStreamActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(MainActivity.this, "Please grant all required permissions first", Toast.LENGTH_SHORT).show();
                    checkAndRequestPermissions();
                }
            }
        });

        btnModeAi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (hasAllPermissions()) {
                    Intent intent = new Intent(MainActivity.this, AiGirlfriendActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(MainActivity.this, "Please grant all required permissions first", Toast.LENGTH_SHORT).show();
                    checkAndRequestPermissions();
                }
            }
        });
    }

    private boolean hasAllPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean cam = checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
            boolean mic = checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
            return cam && mic;
        }
        return true;
    }

    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] permissions;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions = new String[]{
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.READ_MEDIA_VIDEO
                };
            } else {
                permissions = new String[]{
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                };
            }

            requestPermissions(permissions, PERMISSION_REQUEST_CODE);
        } else {
            tvPermissionStatus.setText("All permissions granted ✓");
            tvPermissionStatus.setTextColor(0xFF4CAF50);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                tvPermissionStatus.setText("All permissions ready ✓");
                tvPermissionStatus.setTextColor(0xFF4CAF50);
            } else {
                tvPermissionStatus.setText("Some permissions denied. Features may be limited.");
                tvPermissionStatus.setTextColor(0xFFF44336);
            }
        }
    }
}