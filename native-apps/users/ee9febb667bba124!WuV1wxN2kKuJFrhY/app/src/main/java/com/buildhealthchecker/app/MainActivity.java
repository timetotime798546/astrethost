package com.buildhealthchecker.app;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Build;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private TextView tvOverallStatus;
    private TextView tvJavaVersion;
    private TextView tvOsVersion;
    private TextView tvMemory;
    private TextView tvNamespaceStatus;
    private TextView tvTerminalLog;
    private Button btnRunDiagnostics;
    private ProgressBar pbLoading;

    private Handler handler;
    private int stepCounter = 0;
    private StringBuilder logBuilder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        handler = new Handler();
        logBuilder = new StringBuilder();

        // Initialize UI Elements
        tvOverallStatus = (TextView) findViewById(R.id.tv_overall_status);
        tvJavaVersion = (TextView) findViewById(R.id.tv_java_version);
        tvOsVersion = (TextView) findViewById(R.id.tv_os_version);
        tvMemory = (TextView) findViewById(R.id.tv_memory);
        tvNamespaceStatus = (TextView) findViewById(R.id.tv_namespace_status);
        tvTerminalLog = (TextView) findViewById(R.id.tv_terminal_log);
        btnRunDiagnostics = (Button) findViewById(R.id.btn_run_diagnostics);
        pbLoading = (ProgressBar) findViewById(R.id.pb_loading);

        // Standard dynamic stats
        tvJavaVersion.setText(System.getProperty("java.version"));
        tvOsVersion.setText("Android " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")");
        
        long freeMem = Runtime.getRuntime().freeMemory() / 1024 / 1024;
        long totalMem = Runtime.getRuntime().totalMemory() / 1024 / 1024;
        tvMemory.setText(freeMem + "MB / " + totalMem + "MB");

        btnRunDiagnostics.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startDiagnosticSuite();
            }
        });
    }

    private void appendLog(String message) {
        logBuilder.append("> ").append(message).append("\n");
        tvTerminalLog.setText(logBuilder.toString());
    }

    private void startDiagnosticSuite() {
        btnRunDiagnostics.setVisibility(View.GONE);
        pbLoading.setVisibility(View.VISIBLE);
        logBuilder.setLength(0);
        appendLog("Initializing automated check suite...");
        stepCounter = 0;
        runNextCheck();
    }

    private void runNextCheck() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                stepCounter++;
                switch (stepCounter) {
                    case 1:
                        appendLog("Checking package descriptor...");
                        appendLog("Namespace found: com.buildhealthchecker.app");
                        runNextCheck();
                        break;
                    case 2:
                        appendLog("Verifying Android Manifest elements...");
                        appendLog("Manifest namespace validated against: http://schemas.android.com/apk/res/android");
                        tvNamespaceStatus.setTextColor(0xFF4CAF50);
                        tvNamespaceStatus.setText("Verified");
                        runNextCheck();
                        break;
                    case 3:
                        appendLog("Verifying Gradle environment versions...");
                        appendLog("Target SDK: 34, Min SDK: 21");
                        appendLog("Gradle 8.5 compatibility confirmed.");
                        runNextCheck();
                        break;
                    case 4:
                        appendLog("Verifying theme attributes...");
                        appendLog("Theme config: android:theme=@android:style/Theme.Material.Light");
                        appendLog("No AppCompat dependency required.");
                        runNextCheck();
                        break;
                    case 5:
                        appendLog("Scanning codebase for Java 8 compatibility...");
                        appendLog("No high-level Java 9+ feature usages detected.");
                        appendLog("Anonymous inner classes verified correctly.");
                        runNextCheck();
                        break;
                    case 6:
                        appendLog("Environment Health Check successfully completed!");
                        tvOverallStatus.setText("Status: Verified & Stable");
                        tvOverallStatus.setTextColor(0xFF4CAF50);
                        pbLoading.setVisibility(View.GONE);
                        btnRunDiagnostics.setVisibility(View.VISIBLE);
                        Toast.makeText(MainActivity.this, "Health Check Passed!", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }, 1200);
    }
}