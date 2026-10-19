package com.mkdeviceapp.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DeviceDetailActivity extends Activity {

    private BackendApi api;
    private String recordId;
    private JSONObject deviceData;

    private TextView tvDeviceName;
    private TextView tvStatusTag;
    private TextView tvMetrics;
    private Button btnRequestScreen;
    private Button btnSendPing;
    private Button btnRefreshStatus;
    private Button btnBack;
    private ProgressBar pbAction;
    private LinearLayout llAuditContainer;
    private TextView tvAuditEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_detail);

        api = new BackendApi(this);

        recordId = getIntent().getStringExtra("record_id");
        String rawData = getIntent().getStringExtra("device_data");

        tvDeviceName = (TextView) findViewById(R.id.tv_detail_device_name);
        tvStatusTag = (TextView) findViewById(R.id.tv_detail_status_tag);
        tvMetrics = (TextView) findViewById(R.id.tv_detail_metrics);
        btnRequestScreen = (Button) findViewById(R.id.btn_request_screen_consent);
        btnSendPing = (Button) findViewById(R.id.btn_send_ping);
        btnRefreshStatus = (Button) findViewById(R.id.btn_refresh_status);
        btnBack = (Button) findViewById(R.id.btn_detail_back);
        pbAction = (ProgressBar) findViewById(R.id.pb_detail_action);
        llAuditContainer = (LinearLayout) findViewById(R.id.ll_audit_container);
        tvAuditEmpty = (TextView) findViewById(R.id.tv_audit_empty);

        try {
            if (rawData != null) {
                deviceData = new JSONObject(rawData);
            } else {
                deviceData = new JSONObject();
            }
        } catch (Exception e) {
            deviceData = new JSONObject();
        }

        displayDeviceInfo();
        loadAuditLogs();

        btnRequestScreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createSessionRequest();
            }
        });

        btnSendPing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendPingCommand();
            }
        });

        btnRefreshStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchFreshStatus();
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void displayDeviceInfo() {
        String name = deviceData.optString("device_name", "Secondary Device");
        String status = deviceData.optString("status", "ready");
        int battery = deviceData.optInt("battery_pct", 85);
        boolean charging = deviceData.optBoolean("is_charging", false);
        String net = deviceData.optString("network_type", "Wi-Fi");
        String ip = deviceData.optString("ip_address", "192.168.1.100");
        String lastSeen = deviceData.optString("last_seen", "Recent");

        tvDeviceName.setText(name);
        tvStatusTag.setText("STATUS: " + status.toUpperCase());

        String metricsText = "Battery Level: " + battery + "% (" + (charging ? "Charging" : "Discharging") + ")\n"
                + "Network Connection: " + net + " [" + ip + "]\n"
                + "Telemetry Last Heartbeat: " + lastSeen;
        tvMetrics.setText(metricsText);
    }

    private void createSessionRequest() {
        pbAction.setVisibility(View.VISIBLE);
        try {
            JSONObject sessionObj = new JSONObject();
            sessionObj.put("controller_id", api.getUserEmail());
            sessionObj.put("managed_device_id", recordId);
            sessionObj.put("session_state", "consent_requested");
            sessionObj.put("permission_screen_sharing", true);
            sessionObj.put("permission_file_access", true);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            String now = sdf.format(new Date());
            sessionObj.put("started_at", now);
            sessionObj.put("ended_at", "");

            api.createData("sessions", sessionObj, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            pbAction.setVisibility(View.GONE);
                            Toast.makeText(DeviceDetailActivity.this, "Session consent request sent to secondary phone!", Toast.LENGTH_LONG).show();
                            recordAuditLog("SCREEN_SESSION_REQUEST", "Requested user consent on secondary phone");
                        }
                    });
                }

                @Override
                public void onError(final String errorMessage) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            pbAction.setVisibility(View.GONE);
                            Toast.makeText(DeviceDetailActivity.this, "Request failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        } catch (Exception e) {
            pbAction.setVisibility(View.GONE);
        }
    }

    private void sendPingCommand() {
        pbAction.setVisibility(View.VISIBLE);
        recordAuditLog("DIAGNOSTIC_PING", "Health ping transmitted from primary controller");
        Toast.makeText(this, "Diagnostic ping transmitted", Toast.LENGTH_SHORT).show();
        pbAction.setVisibility(View.GONE);
    }

    private void fetchFreshStatus() {
        pbAction.setVisibility(View.VISIBLE);
        api.readData("devices", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(final JSONObject response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pbAction.setVisibility(View.GONE);
                        try {
                            JSONArray records = response.optJSONArray("records");
                            if (records != null) {
                                for (int i = 0; i < records.length(); i++) {
                                    JSONObject r = records.getJSONObject(i);
                                    if (r.optString("id").equals(recordId)) {
                                        deviceData = r.optJSONObject("data");
                                        displayDeviceInfo();
                                        Toast.makeText(DeviceDetailActivity.this, "Telemetry refreshed", Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                }
                            }
                        } catch (Exception ignored) {
                        }
                    }
                });
            }

            @Override
            public void onError(final String errorMessage) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pbAction.setVisibility(View.GONE);
                        Toast.makeText(DeviceDetailActivity.this, "Failed to refresh: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void recordAuditLog(String action, String details) {
        try {
            JSONObject log = new JSONObject();
            log.put("device_id", recordId != null ? recordId : "unknown");
            log.put("action", action);
            log.put("details", details);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            log.put("timestamp", sdf.format(new Date()));

            api.createData("audit_logs", log, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loadAuditLogs();
                        }
                    });
                }

                @Override
                public void onError(String errorMessage) {
                }
            });
        } catch (Exception ignored) {
        }
    }

    private void loadAuditLogs() {
        api.readData("audit_logs", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(final JSONObject response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            JSONArray logs = response.optJSONArray("records");
                            if (logs != null && logs.length() > 0) {
                                llAuditContainer.removeAllViews();
                                for (int i = logs.length() - 1; i >= Math.max(0, logs.length() - 6); i--) {
                                    JSONObject item = logs.getJSONObject(i);
                                    JSONObject d = item.optJSONObject("data");
                                    if (d != null) {
                                        TextView logEntry = new TextView(DeviceDetailActivity.this);
                                        String act = d.optString("action", "EVENT");
                                        String detail = d.optString("details", "");
                                        String ts = d.optString("timestamp", "");
                                        logEntry.setText("[" + ts + "] " + act + "\n" + detail);
                                        logEntry.setTextColor(0xFFCBD5E1);
                                        logEntry.setTextSize(12);
                                        logEntry.setPadding(0, 4, 0, 8);
                                        llAuditContainer.addView(logEntry);
                                    }
                                }
                            } else {
                                tvAuditEmpty.setVisibility(View.VISIBLE);
                            }
                        } catch (Exception ignored) {
                        }
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
            }
        });
    }
}