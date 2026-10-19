package com.mkdeviceapp.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Random;

public class PairingActivity extends Activity {

    private BackendApi api;
    private EditText etManagedName;
    private Button btnRegisterManaged;
    private TextView tvGeneratedCode;
    private EditText etInputCode;
    private Button btnPairController;
    private Button btnDone;
    private ProgressBar pbPairing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pairing);

        api = new BackendApi(this);

        etManagedName = (EditText) findViewById(R.id.et_managed_device_name);
        btnRegisterManaged = (Button) findViewById(R.id.btn_register_as_managed);
        tvGeneratedCode = (TextView) findViewById(R.id.tv_generated_pairing_code);
        etInputCode = (EditText) findViewById(R.id.et_input_pairing_code);
        btnPairController = (Button) findViewById(R.id.btn_pair_controller);
        btnDone = (Button) findViewById(R.id.btn_pairing_done);
        pbPairing = (ProgressBar) findViewById(R.id.pb_pairing);

        btnRegisterManaged.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerAsSecondary();
            }
        });

        btnPairController.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pairWithCode();
            }
        });

        btnDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void registerAsSecondary() {
        String name = etManagedName.getText().toString().trim();
        if (name.isEmpty()) {
            name = android.os.Build.MODEL + " (Managed)";
        }

        // Generate a random 6-digit pairing code
        int codeNum = 100000 + new Random().nextInt(900000);
        final String pairingCode = "MK-" + codeNum;

        // Gather real telemetry to seed initial record
        DeviceTelemetryHelper.TelemetryInfo info = DeviceTelemetryHelper.gatherDeviceInfo(this);

        pbPairing.setVisibility(View.VISIBLE);
        btnRegisterManaged.setEnabled(false);

        try {
            JSONObject dataObj = new JSONObject();
            dataObj.put("device_id", "DEV-" + System.currentTimeMillis());
            dataObj.put("device_name", name);
            dataObj.put("role", "secondary");
            dataObj.put("pairing_code", pairingCode);
            dataObj.put("status", "ready");
            dataObj.put("battery_pct", info.batteryPercent);
            dataObj.put("is_charging", info.isCharging);
            dataObj.put("network_type", info.networkType);
            dataObj.put("ip_address", info.ipAddress);
            dataObj.put("last_seen", info.timestamp);
            dataObj.put("authorized_features", "telemetry,screen_consent,file_transfer");

            api.createData("devices", dataObj, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(final JSONObject response) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            pbPairing.setVisibility(View.GONE);
                            btnRegisterManaged.setEnabled(true);
                            tvGeneratedCode.setText("Pairing Code: " + pairingCode + "\nEnter this code on your Controller phone.");
                            tvGeneratedCode.setVisibility(View.VISIBLE);
                            Toast.makeText(PairingActivity.this, "Secondary Device Registered!", Toast.LENGTH_LONG).show();
                        }
                    });
                }

                @Override
                public void onError(final String errorMessage) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            pbPairing.setVisibility(View.GONE);
                            btnRegisterManaged.setEnabled(true);
                            Toast.makeText(PairingActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        } catch (Exception e) {
            pbPairing.setVisibility(View.GONE);
            btnRegisterManaged.setEnabled(true);
        }
    }

    private void pairWithCode() {
        final String code = etInputCode.getText().toString().trim();
        if (code.isEmpty()) {
            Toast.makeText(this, "Please enter the pairing code", Toast.LENGTH_SHORT).show();
            return;
        }

        pbPairing.setVisibility(View.VISIBLE);
        btnPairController.setEnabled(false);

        // Fetch devices to match pairing_code
        api.readData("devices", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(final JSONObject response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            JSONArray records = response.optJSONArray("records");
                            boolean found = false;
                            if (records != null) {
                                for (int i = 0; i < records.length(); i++) {
                                    JSONObject rec = records.getJSONObject(i);
                                    String id = rec.optString("id");
                                    JSONObject data = rec.optJSONObject("data");
                                    if (data != null && code.equalsIgnoreCase(data.optString("pairing_code"))) {
                                        found = true;
                                        // Update state to paired
                                        data.put("status", "paired");
                                        api.updateData(id, data, new BackendApi.ApiCallback() {
                                            @Override
                                            public void onSuccess(JSONObject uRes) {
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        pbPairing.setVisibility(View.GONE);
                                                        btnPairController.setEnabled(true);
                                                        Toast.makeText(PairingActivity.this, "Device Paired Successfully!", Toast.LENGTH_LONG).show();
                                                        finish();
                                                    }
                                                });
                                            }

                                            @Override
                                            public void onError(String err) {
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        pbPairing.setVisibility(View.GONE);
                                                        btnPairController.setEnabled(true);
                                                        Toast.makeText(PairingActivity.this, "Paired but update error: " + err, Toast.LENGTH_SHORT).show();
                                                        finish();
                                                    }
                                                });
                                            }
                                        });
                                        break;
                                    }
                                }
                            }
                            if (!found) {
                                pbPairing.setVisibility(View.GONE);
                                btnPairController.setEnabled(true);
                                Toast.makeText(PairingActivity.this, "No device found with this pairing code", Toast.LENGTH_LONG).show();
                            }
                        } catch (Exception e) {
                            pbPairing.setVisibility(View.GONE);
                            btnPairController.setEnabled(true);
                            Toast.makeText(PairingActivity.this, "Parsing error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

            @Override
            public void onError(final String errorMessage) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pbPairing.setVisibility(View.GONE);
                        btnPairController.setEnabled(true);
                        Toast.makeText(PairingActivity.this, "Search error: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}