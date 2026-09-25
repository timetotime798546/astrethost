package com.popeen.app;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {

    private TextView tvStatus;
    private TextView tvCountdown;
    private TextView tvLastCommand;
    private TextView tvLastResponse;
    private TextView tvLogs;
    private LinearLayout layoutPermissions;
    private Spinner spinnerLanguage;
    private Switch switchBoot;

    private long sessionEndTime = 0;
    private final Handler countdownHandler = new Handler();
    private final Runnable countdownRunnable = new Runnable() {
        @Override
        public void run() {
            updateCountdownTimer();
            countdownHandler.postDelayed(this, 1000);
        }
    };

    private final String[] displayLangs = {"English", "Hindi (हिंदी)", "Hinglish"};
    private final String[] langCodes = {"en", "hi", "hinglish"};

    private final BroadcastReceiver statusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && "com.popeen.app.STATUS_UPDATE".equals(intent.getAction())) {
                String state = intent.getStringExtra("state");
                String log = intent.getStringExtra("log");
                String lastCommand = intent.getStringExtra("last_command");
                String lastResponse = intent.getStringExtra("last_response");
                sessionEndTime = intent.getLongExtra("session_end_time", 0);

                if (lastCommand != null && !lastCommand.isEmpty()) {
                    tvLastCommand.setText(lastCommand);
                }
                if (lastResponse != null && !lastResponse.isEmpty()) {
                    tvLastResponse.setText(lastResponse);
                }
                updateUiState(state, log);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatus = findViewById(R.id.tv_status);
        tvCountdown = findViewById(R.id.tv_countdown);
        tvLastCommand = findViewById(R.id.tv_last_command);
        tvLastResponse = findViewById(R.id.tv_last_response);
        tvLogs = findViewById(R.id.tv_logs);
        layoutPermissions = findViewById(R.id.layout_permissions);
        spinnerLanguage = findViewById(R.id.spinner_language);
        switchBoot = findViewById(R.id.switch_boot);

        tvLogs.setMovementMethod(new ScrollingMovementMethod());

        setupPermissionsButton();
        setupServiceButtons();
        setupLanguageSpinner();
        setupBootSwitch();

        countdownHandler.post(countdownRunnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(statusReceiver, new IntentFilter("com.popeen.app.STATUS_UPDATE"), Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(statusReceiver, new IntentFilter("com.popeen.app.STATUS_UPDATE"));
        }

        checkPermissionsDisplay();
        refreshServiceStatus();
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(statusReceiver);
        } catch (Exception e) {}
    }

    @Override
    protected void onDestroy() {
        countdownHandler.removeCallbacks(countdownRunnable);
        super.onDestroy();
    }

    private void updateCountdownTimer() {
        long curTime = System.currentTimeMillis();
        if (sessionEndTime > curTime && isServiceActive(VoiceAssistantService.class)) {
            long remainingSec = (sessionEndTime - curTime) / 1000;
            long mins = remainingSec / 60;
            long secs = remainingSec % 60;
            tvCountdown.setText(String.format(Locale.US, "Active Session: %02d:%02d remaining", mins, secs));
            tvCountdown.setTextColor(Color.parseColor("#009688"));
        } else {
            tvCountdown.setText("Active Session: Idle");
            tvCountdown.setTextColor(Color.parseColor("#757575"));
        }
    }

    private void setupPermissionsButton() {
        Button btnGrant = findViewById(R.id.btn_grant_permissions);
        btnGrant.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestRequiredPermissions();
            }
        });
    }

    private void setupServiceButtons() {
        Button btnStart = findViewById(R.id.btn_start);
        Button btnStop = findViewById(R.id.btn_stop);

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!hasPermissions()) {
                    requestRequiredPermissions();
                    return;
                }

                Intent intent = new Intent(MainActivity.this, VoiceAssistantService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent);
                } else {
                    startService(intent);
                }
                updateUiState("WAKE_WORD_LISTENING", "Launching background services...");
            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, VoiceAssistantService.class);
                stopService(intent);
                updateUiState("STOPPED", "Shutdown sequence completed.");
            }
        });
    }

    private void setupLanguageSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this, android.R.layout.simple_spinner_item, displayLangs
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLanguage.setAdapter(adapter);

        String activeLang = PreferencesHelper.getLanguage(this);
        for (int i = 0; i < langCodes.length; i++) {
            if (langCodes[i].equals(activeLang)) {
                spinnerLanguage.setSelection(i);
                break;
            }
        }

        spinnerLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String code = langCodes[position];
                PreferencesHelper.setLanguage(MainActivity.this, code);

                if (isServiceActive(VoiceAssistantService.class)) {
                    Intent updateIntent = new Intent(MainActivity.this, VoiceAssistantService.class);
                    updateIntent.setAction("ACTION_UPDATE_LANG");
                    startService(updateIntent);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupBootSwitch() {
        switchBoot.setChecked(PreferencesHelper.isAutoStart(this));
        switchBoot.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                PreferencesHelper.setAutoStart(MainActivity.this, isChecked);
            }
        });
    }

    private void checkPermissionsDisplay() {
        if (hasPermissions()) {
            layoutPermissions.setVisibility(View.GONE);
        } else {
            layoutPermissions.setVisibility(View.VISIBLE);
        }
    }

    private boolean hasPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
            if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void requestRequiredPermissions() {
        ArrayList<String> perms = new ArrayList<>();
        perms.add(android.Manifest.permission.RECORD_AUDIO);
        perms.add(android.Manifest.permission.CAMERA);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(android.Manifest.permission.POST_NOTIFICATIONS);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(perms.toArray(new String[0]), 2002);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        checkPermissionsDisplay();
    }

    private boolean isServiceActive(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            for (ActivityManager.RunningServiceInfo info : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.getName().equals(info.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void refreshServiceStatus() {
        if (isServiceActive(VoiceAssistantService.class)) {
            VoiceAssistantService.VoiceState state = VoiceAssistantService.getCurrentState();
            sessionEndTime = VoiceAssistantService.getSessionEndTime();
            updateUiState(state.name(), "Popeen Assistant running in background.");
        } else {
            updateUiState("STOPPED", "Assistant is stopped.");
        }
    }

    private void updateUiState(String state, String log) {
        if (state != null) {
            if ("WAKE_WORD_LISTENING".equals(state)) {
                tvStatus.setText("Listening for \"Hey Popeen\"");
                tvStatus.setTextColor(Color.parseColor("#0D47A1"));
            } else if ("WAKE_WORD_DETECTED".equals(state)) {
                tvStatus.setText("Wake Word Detected!");
                tvStatus.setTextColor(Color.parseColor("#E65100"));
            } else if ("COMMAND_LISTENING".equals(state)) {
                tvStatus.setText("Listening for command…");
                tvStatus.setTextColor(Color.parseColor("#00E676"));
            } else if ("PROCESSING_COMMAND".equals(state)) {
                tvStatus.setText("Processing command…");
                tvStatus.setTextColor(Color.parseColor("#AA00FF"));
            } else if ("SPEAKING".equals(state)) {
                tvStatus.setText("Speaking…");
                tvStatus.setTextColor(Color.parseColor("#00B0FF"));
            } else {
                tvStatus.setText("Inactive");
                tvStatus.setTextColor(Color.parseColor("#D32F2F"));
            }
        }

        if (log != null) {
            String existingLogs = tvLogs.getText().toString();
            if (existingLogs.startsWith("System monitors pending...")) {
                existingLogs = "";
            }
            String formattedLogs = "[" + (System.currentTimeMillis() % 100000 / 100) / 10.0 + "s] " + log + "\n" + existingLogs;
            if (formattedLogs.length() > 6000) {
                formattedLogs = formattedLogs.substring(0, 6000);
            }
            tvLogs.setText(formattedLogs);
        }
    }
}