package com.mkdeviceapp.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {

    private BackendApi api;
    private TextView tvUserIdentity;
    private Button btnLogout;
    private TextView tvLocalBattery;
    private TextView tvLocalNetwork;
    private Button btnBroadcast;
    private Button btnAddDevice;
    private Button btnRefresh;
    private ProgressBar pbLoading;
    private TextView tvNoDevices;
    private LinearLayout llDeviceContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        api = new BackendApi(this);

        if (!api.isLoggedIn()) {
            Intent intent = new Intent(this, AuthActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        tvUserIdentity = (TextView) findViewById(R.id.tv_user_identity);
        btnLogout = (Button) findViewById(R.id.btn_logout);
        tvLocalBattery = (TextView) findViewById(R.id.tv_local_battery);
        tvLocalNetwork = (TextView) findViewById(R.id.tv_local_network);
        btnBroadcast = (Button) findViewById(R.id.btn_broadcast_status);
        btnAddDevice = (Button) findViewById(R.id.btn_add_device);
        btnRefresh = (Button) findViewById(R.id.btn_refresh_devices);
        pbLoading = (ProgressBar) findViewById(R.id.pb_devices_loading);
        tvNoDevices = (TextView) findViewById(R.id.tv_no_devices);
        llDeviceContainer = (LinearLayout) findViewById(R.id.ll_device_list_container);

        tvUserIdentity.setText("Controller: " + api.getUserEmail());

        updateLocalTelemetryDisplay();

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                api.logout(new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Intent intent = new Intent(MainActivity.this, AuthActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        });
                    }

                    @Override
                    public void onError(String errorMessage) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Intent intent = new Intent(MainActivity.this, AuthActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        });
                    }
                });
            }
        });

        btnBroadcast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                syncLocalDevice();
            }
        });

        btnAddDevice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, PairingActivity.class);
                startActivity(intent);
            }
        });

        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadDevices();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (api.isLoggedIn()) {
            loadDevices();
            updateLocalTelemetryDisplay();
        }
    }

    private void updateLocalTelemetryDisplay() {
        DeviceTelemetryHelper.TelemetryInfo info = DeviceTelemetryHelper.gatherDeviceInfo(this);
        tvLocalBattery.setText("Battery: " + info.batteryPercent + "% (" + (info.isCharging ? "Charging" : "Discharging") + ")");
        tvLocalNetwork.setText("Network: " + info.networkType + " (" + info.ipAddress + ")");
    }

    private void syncLocalDevice() {
        DeviceTelemetryHelper.TelemetryInfo info = DeviceTelemetryHelper.gatherDeviceInfo(this);
        updateLocalTelemetryDisplay();
        Toast.makeText(this, "Local Telemetry Updated", Toast.LENGTH_SHORT).show();
    }

    private void loadDevices() {
        pbLoading.setVisibility(View.VISIBLE);
        tvNoDevices.setVisibility(View.GONE);
        llDeviceContainer.removeAllViews();

        api.readData("devices", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(final JSONObject response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pbLoading.setVisibility(View.GONE);
                        try {
                            JSONArray records = response.optJSONArray("records");
                            if (records == null || records.length() == 0) {
                                tvNoDevices.setVisibility(View.VISIBLE);
                                return;
                            }
                            tvNoDevices.setVisibility(View.GONE);

                            for (int i = 0; i < records.length(); i++) {
                                JSONObject rec = records.getJSONObject(i);
                                final String recordId = rec.optString("id");
                                final JSONObject data = rec.optJSONObject("data");
                                if (data == null) continue;

                                createDeviceCard(recordId, data);
                            }
                        } catch (Exception e) {
                            tvNoDevices.setText("Error reading records: " + e.getMessage());
                            tvNoDevices.setVisibility(View.VISIBLE);
                        }
                    }
                });
            }

            @Override
            public void onError(final String errorMessage) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pbLoading.setVisibility(View.GONE);
                        tvNoDevices.setText("Error loading devices: " + errorMessage);
                        tvNoDevices.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
    }

    private void createDeviceCard(final String recordId, final JSONObject data) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(0xFF1E293B);
        card.setPadding(20, 20, 20, 20);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16);
        card.setLayoutParams(params);

        String name = data.optString("device_name", "Android Device");
        String status = data.optString("status", "unknown");
        int battery = data.optInt("battery_pct", 100);
        String net = data.optString("network_type", "Unknown");
        String pairingCode = data.optString("pairing_code", "");

        TextView tvName = new TextView(this);
        tvName.setText(name);
        tvName.setTextColor(0xFFFFFFFF);
        tvName.setTextSize(16);
        tvName.setTypeface(null, android.graphics.Typeface.BOLD);
        card.addView(tvName);

        TextView tvDetails = new TextView(this);
        tvDetails.setText("Status: " + status.toUpperCase() + " | Battery: " + battery + "% | Network: " + net);
        tvDetails.setTextColor(status.equalsIgnoreCase("paired") ? 0xFF34D399 : 0xFFF59E0B);
        tvDetails.setTextSize(12);
        tvDetails.setPadding(0, 6, 0, 8);
        card.addView(tvDetails);

        if (!pairingCode.isEmpty()) {
            TextView tvCode = new TextView(this);
            tvCode.setText("Pairing Code: " + pairingCode);
            tvCode.setTextColor(0xFF94A3B8);
            tvCode.setTextSize(11);
            card.addView(tvCode);
        }

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setPadding(0, 12, 0, 0);

        Button btnManage = new Button(this);
        btnManage.setText("Manage & Console");
        btnManage.setBackgroundColor(0xFF2563EB);
        btnManage.setTextColor(0xFFFFFFFF);
        btnManage.setTextSize(12);
        LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(0, 80, 1.0f);
        p1.setMargins(0, 0, 8, 0);
        btnManage.setLayoutParams(p1);
        btnManage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, DeviceDetailActivity.class);
                intent.putExtra("record_id", recordId);
                intent.putExtra("device_data", data.toString());
                startActivity(intent);
            }
        });
        btnRow.addView(btnManage);

        Button btnRevoke = new Button(this);
        btnRevoke.setText("Revoke");
        btnRevoke.setBackgroundColor(0xFFDC2626);
        btnRevoke.setTextColor(0xFFFFFFFF);
        btnRevoke.setTextSize(12);
        LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(0, 80, 0.5f);
        btnRevoke.setLayoutParams(p2);
        btnRevoke.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteDevice(recordId);
            }
        });
        btnRow.addView(btnRevoke);

        card.addView(btnRow);
        llDeviceContainer.addView(card);
    }

    private void deleteDevice(String recordId) {
        pbLoading.setVisibility(View.VISIBLE);
        api.deleteData(recordId, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Device access revoked", Toast.LENGTH_SHORT).show();
                        loadDevices();
                    }
                });
            }

            @Override
            public void onError(final String errorMessage) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pbLoading.setVisibility(View.GONE);
                        Toast.makeText(MainActivity.this, "Revoke failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}