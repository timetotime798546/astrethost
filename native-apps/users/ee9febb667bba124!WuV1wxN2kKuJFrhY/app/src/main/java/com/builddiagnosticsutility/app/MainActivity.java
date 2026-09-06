package com.builddiagnosticsutility.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ScrollView;
import android.os.Build;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.util.DisplayMetrics;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.Date;

public class MainActivity extends Activity {

    // Tab view references
    private ScrollView scrollSystemInfo;
    private ScrollView scrollDiagnostics;
    private ScrollView scrollSandbox;

    // Tab navigation buttons
    private Button btnTabSystem;
    private Button btnTabDiagnostics;
    private Button btnTabSandbox;

    // Screen info elements
    private TextView txtSystemInfo;
    private TextView txtScreenInfo;
    private TextView txtSensorsList;
    private TextView txtPingResult;
    private TextView txtConsole;
    private ScrollView scrollConsoleContainer;

    private Button btnRunPing;
    private Button btnRefreshSys;
    private Button btnClearConsole;

    // Sandbox error trigger buttons
    private Button btnTriggerNpe;
    private Button btnTriggerArith;
    private Button btnTriggerIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Map general UI elements
        scrollSystemInfo = (ScrollView) findViewById(R.id.scroll_system_info);
        scrollDiagnostics = (ScrollView) findViewById(R.id.scroll_diagnostics);
        scrollSandbox = (ScrollView) findViewById(R.id.scroll_sandbox);

        btnTabSystem = (Button) findViewById(R.id.btn_tab_system);
        btnTabDiagnostics = (Button) findViewById(R.id.btn_tab_diagnostics);
        btnTabSandbox = (Button) findViewById(R.id.btn_tab_sandbox);

        txtSystemInfo = (TextView) findViewById(R.id.txt_system_info);
        txtScreenInfo = (TextView) findViewById(R.id.txt_screen_info);
        txtSensorsList = (TextView) findViewById(R.id.txt_sensors_list);
        txtPingResult = (TextView) findViewById(R.id.txt_ping_result);
        txtConsole = (TextView) findViewById(R.id.txt_console_output);
        scrollConsoleContainer = (ScrollView) findViewById(R.id.scroll_console_container);

        btnRunPing = (Button) findViewById(R.id.btn_run_ping);
        btnRefreshSys = (Button) findViewById(R.id.btn_refresh_sys);
        btnClearConsole = (Button) findViewById(R.id.btn_clear_console);

        btnTriggerNpe = (Button) findViewById(R.id.btn_trigger_npe);
        btnTriggerArith = (Button) findViewById(R.id.btn_trigger_arith);
        btnTriggerIndex = (Button) findViewById(R.id.btn_trigger_index);

        // Bind navigation clicks using traditional Java 8 anonymous inner classes
        btnTabSystem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(0);
            }
        });

        btnTabDiagnostics.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(1);
            }
        });

        btnTabSandbox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(2);
            }
        });

        // Diagnostics Actions
        btnRunPing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                executePingTest();
            }
        });

        btnRefreshSys.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                populateSystemData();
            }
        });

        // Sandbox Trigger Actions
        btnTriggerNpe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    String nullString = null;
                    int length = nullString.length(); // Trigger NPE
                } catch (Throwable t) {
                    printExceptionToConsole("NullPointerException Simulation", t);
                }
            }
        });

        btnTriggerArith.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    int a = 100;
                    int b = 0;
                    int result = a / b; // Trigger ArithmeticException
                } catch (Throwable t) {
                    printExceptionToConsole("ArithmeticException (Division by Zero)", t);
                }
            }
        });

        btnTriggerIndex.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    int[] numbers = new int[3];
                    int errorValue = numbers[10]; // Trigger ArrayIndexOutOfBoundsException
                } catch (Throwable t) {
                    printExceptionToConsole("ArrayIndexOutOfBoundsException", t);
                }
            }
        });

        btnClearConsole.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                txtConsole.setText("[CONSOLE CLEARED SUCCESS]\nReady for error diagnostics...\n");
            }
        });

        // Load initial diagnostic panels
        populateSystemData();
        populateScreenDetails();
        populateOnboardSensors();
    }

    private void switchTab(int tabIndex) {
        scrollSystemInfo.setVisibility(tabIndex == 0 ? View.VISIBLE : View.GONE);
        scrollDiagnostics.setVisibility(tabIndex == 1 ? View.VISIBLE : View.GONE);
        scrollSandbox.setVisibility(tabIndex == 2 ? View.VISIBLE : View.GONE);

        // Update Navigation Button States
        btnTabSystem.setBackgroundColor(tabIndex == 0 ? 0xFF005A9C : 0xFFE0E0E0);
        btnTabSystem.setTextColor(tabIndex == 0 ? 0xFFFFFFFF : 0xFF333333);

        btnTabDiagnostics.setBackgroundColor(tabIndex == 1 ? 0xFF005A9C : 0xFFE0E0E0);
        btnTabDiagnostics.setTextColor(tabIndex == 1 ? 0xFFFFFFFF : 0xFF333333);

        btnTabSandbox.setBackgroundColor(tabIndex == 2 ? 0xFF005A9C : 0xFFE0E0E0);
        btnTabSandbox.setTextColor(tabIndex == 2 ? 0xFFFFFFFF : 0xFF333333);
    }

    private void populateSystemData() {
        StringBuilder sb = new StringBuilder();
        sb.append("OS Version Release: Android ").append(Build.VERSION.RELEASE).append("\n");
        sb.append("SDK Level: API ").append(Build.VERSION.SDK_INT).append("\n");
        sb.append("Brand: ").append(Build.BRAND).append("\n");
        sb.append("Manufacturer: ").append(Build.MANUFACTURER).append("\n");
        sb.append("Device: ").append(Build.DEVICE).append("\n");
        sb.append("Hardware Board: ").append(Build.BOARD).append("\n");
        sb.append("Host: ").append(Build.HOST).append("\n");
        sb.append("User VM: ").append(System.getProperty("java.vm.name")).append(" (").append(System.getProperty("java.vm.version")).append(")\n\n");

        // Runtime System JVM Memory metrics
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / (1024 * 1024);
        long totalMemory = runtime.totalMemory() / (1024 * 1024);
        long freeMemory = runtime.freeMemory() / (1024 * 1024);
        long activeUsed = totalMemory - freeMemory;

        sb.append("--- JVM Heap Allocations ---\n");
        sb.append("Max Memory Boundary: ").append(maxMemory).append(" MB\n");
        sb.append("Allocated VM Size: ").append(totalMemory).append(" MB\n");
        sb.append("Free Allocated Memory: ").append(freeMemory).append(" MB\n");
        sb.append("Used Heap Allocated: ").append(activeUsed).append(" MB\n\n");

        // Partition storage details
        java.io.File path = android.os.Environment.getDataDirectory();
        android.os.StatFs stat = new android.os.StatFs(path.getPath());
        long blockSize = stat.getBlockSizeLong();
        long totalBlocks = stat.getBlockCountLong();
        long availableBlocks = stat.getAvailableBlocksLong();

        long storageSpaceTotal = (totalBlocks * blockSize) / (1024 * 1024);
        long storageSpaceFree = (availableBlocks * blockSize) / (1024 * 1024);

        sb.append("--- Internal Disk Diagnostics ---\n");
        sb.append("Total Volume Storage: ").append(storageSpaceTotal).append(" MB\n");
        sb.append("Available Free Space: ").append(storageSpaceFree).append(" MB\n");

        txtSystemInfo.setText(sb.toString());
    }

    private void populateScreenDetails() {
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        
        StringBuilder sb = new StringBuilder();
        sb.append("--- Screen Metrics ---\n");
        sb.append("Raw Resolution: ").append(dm.widthPixels).append(" x ").append(dm.heightPixels).append(" px\n");
        sb.append("Screen Density: ").append(dm.densityDpi).append(" DPI (Scale Factor: ").append(dm.density).append(")\n");
        sb.append("Font Scale Configuration: ").append(dm.scaledDensity).append("\n");
        
        txtScreenInfo.setText(sb.toString());
    }

    private void populateOnboardSensors() {
        SensorManager sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sm == null) {
            txtSensorsList.setText("Error: Sensor Diagnostics Service not available.");
            return;
        }

        List<Sensor> sensorsList = sm.getSensorList(Sensor.TYPE_ALL);
        StringBuilder sb = new StringBuilder();
        sb.append("Detected ").append(sensorsList.size()).append(" onboard physical/virtual sensor hardware chips:\n\n");

        for (int i = 0; i < Math.min(sensorsList.size(), 25); i++) {
            Sensor s = sensorsList.get(i);
            sb.append("[").append(i + 1).append("] ").append(s.getName()).append("\n")
              .append("Vendor: ").append(s.getVendor()).append(" | Power: ").append(s.getPower()).append("mA\n\n");
        }

        if (sensorsList.size() > 25) {
            sb.append("... and ").append(sensorsList.size() - 25).append(" additional sensors.");
        }

        txtSensorsList.setText(sb.toString());
    }

    private void executePingTest() {
        txtPingResult.setText("Testing connection loop... Pinging 1.1.1.1 on Port 53.");
        btnRunPing.setEnabled(false);

        // Run multi-threaded diagnostic connection to avoid Application Not Responding state
        new Thread(new Runnable() {
            @Override
            public void run() {
                final String diagnosticOutcome;
                long startCheckTime = System.currentTimeMillis();
                Socket socket = new Socket();
                try {
                    socket.connect(new InetSocketAddress("1.1.1.1", 53), 3500);
                    long delayMillis = System.currentTimeMillis() - startCheckTime;
                    diagnosticOutcome = "NETWORK SUCCESS!\nConnected to Cloudflare DNS IP (1.1.1.1) in " + delayMillis + " ms.";
                } catch (Exception e) {
                    diagnosticOutcome = "CONNECTION FAILED!\nError message trace:\n" + e.getLocalizedMessage() + 
                                       "\nVerify your cellular/Wi-Fi connection.";
                } finally {
                    try {
                        socket.close();
                    } catch (Exception ignored) {}
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        txtPingResult.setText(diagnosticOutcome);
                        btnRunPing.setEnabled(true);
                    }
                });
            }
        }).start();
    }

    private void printExceptionToConsole(String debugCategory, Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        String stackTraceData = sw.toString();

        StringBuilder sb = new StringBuilder();
        sb.append("\n================================================\n")
          .append(">>> DETECTED: ").append(debugCategory).append("\n")
          .append("Timestamp: ").append(new Date().toString()).append("\n")
          .append("Message: ").append(t.getMessage() != null ? t.getMessage() : "No diagnostic error message descriptive string standard.")
          .append("\n------------------------------------------------\n")
          .append(stackTraceData)
          .append("================================================\n");

        txtConsole.append(sb.toString());

        // Focus scrolling to bottom of console view
        scrollConsoleContainer.post(new Runnable() {
            @Override
            public void run() {
                scrollConsoleContainer.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }
}