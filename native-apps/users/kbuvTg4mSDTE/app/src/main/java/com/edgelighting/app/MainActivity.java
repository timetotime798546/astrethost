package com.edgelighting.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final int REQUEST_OVERLAY_PERMISSION = 123;
    private Button startButton;
    private Button stopButton;
    private TextView statusTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startButton = (Button) findViewById(R.id.startButton);
        stopButton = (Button) findViewById(R.id.stopButton);
        statusTextView = (TextView) findViewById(R.id.statusTextView);

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkOverlayPermissionAndStartService();
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopEdgeLightingService();
            }
        });

        // Set up service state listener to update UI elements instantly
        EdgeLightingService.setListener(new EdgeLightingService.ServiceStateListener() {
            @Override
            public void onStateChanged(final boolean running) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateUI(running);
                    }
                });
            }
        });

        updateServiceStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateServiceStatus();
    }

    private void checkOverlayPermissionAndStartService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Permission Required");
            builder.setMessage("Edge Lighting App needs permission to draw over other apps to function properly. Please grant this permission on the settings screen.");
            builder.setPositiveButton("Go to Settings", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Toast.makeText(MainActivity.this, "Permission denied. Cannot start service.", Toast.LENGTH_SHORT).show();
                }
            });
            builder.show();
        } else {
            startEdgeLightingService();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                startEdgeLightingService();
            } else {
                Toast.makeText(this, "Overlay permission denied. Cannot start edge lighting.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startEdgeLightingService() {
        Intent serviceIntent = new Intent(this, EdgeLightingService.class);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            Toast.makeText(this, "Edge Lighting Started", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error starting service: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
        updateServiceStatus();
    }

    private void stopEdgeLightingService() {
        Intent serviceIntent = new Intent(this, EdgeLightingService.class);
        stopService(serviceIntent);
        Toast.makeText(this, "Edge Lighting Stopped", Toast.LENGTH_SHORT).show();
        updateServiceStatus();
    }

    private void updateServiceStatus() {
        updateUI(EdgeLightingService.isRunning());
    }

    private void updateUI(boolean isRunning) {
        if (isRunning) {
            statusTextView.setText("Status: Running");
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
        } else {
            statusTextView.setText("Status: Stopped");
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
        }
    }
}