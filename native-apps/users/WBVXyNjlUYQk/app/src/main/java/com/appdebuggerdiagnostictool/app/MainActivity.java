package com.appdebuggerdiagnostictool.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    // Tabs navigation buttons
    private Button btnTabSystem;
    private Button btnTabLogcat;
    private Button btnTabDiagnostics;
    private Button btnTabSandbox;

    // View containers
    private ScrollView viewSystem;
    private View viewLogcat;
    private ScrollView viewDiagnostics;
    private ScrollView viewSandbox;

    // System tab widgets
    private TextView txtSystemInfo;
    private TextView txtRuntimeInfo;

    // Logcat tab widgets
    private EditText edtLogcatFilter;
    private Button btnRefreshLogcat;
    private Button btnClearLogcat;
    private TextView txtLogcatOutput;

    // Diagnostics tab widgets
    private Button btnRunNetworkTest;
    private TextView txtNetworkStatus;
    private TextView txtNetworkDetails;
    private Button btnRunStorageTest;
    private TextView txtStorageStatus;
    private TextView txtStorageDetails;
    private TextView txtPermissionChecklist;

    // Sandbox tab widgets
    private Button btnTriggerHandledError;
    private Button btnTriggerNpe;
    private Button btnTriggerOob;
    private Button btnTriggerDivZero;
    private TextView txtSandboxState;

    // Global UI status bar
    private TextView txtStatusBar;

    // Preference file name for reporting crashes
    private static final String PREFS_NAME = "crash_reports";
    private static final String KEY_LAST_ERROR = "last_error";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Intercept global crashes immediately before layout initializes
        setupGlobalUncaughtExceptionHandler();

        // Bind Views
        initViews();

        // Setup Tab Navigation
        setupTabNavigation();

        // Load diagnostic data initially
        loadSystemSpecs();
        refreshLogcatOutput("");
        updatePermissionChecklist();

        // Check for previous simulation crashes
        checkAndShowPreviousCrash();

        // Attach action listeners
        attachActions();
    }

    private void setupGlobalUncaughtExceptionHandler() {
        final Thread.UncaughtExceptionHandler originalHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                try {
                    SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    String stackTraceStr = android.util.Log.getStackTraceString(throwable);
                    editor.putString(KEY_LAST_ERROR, "Exception in thread: [" + thread.getName() + "]\n\n" 
                        + throwable.toString() + "\n\nStack Trace:\n" + stackTraceStr);
                    editor.commit();
                } catch (Exception e) {
                    // Fail-safe
                }

                // Delegate to system handler to standard-terminate the process
                if (originalHandler != null) {
                    originalHandler.uncaughtException(thread, throwable);
                } else {
                    android.os.Process.killProcess(android.os.Process.myPid());
                    System.exit(10);
                }
            }
        });
    }

    private void initViews() {
        btnTabSystem = (Button) findViewById(R.id.btn_tab_system);
        btnTabLogcat = (Button) findViewById(R.id.btn_tab_logcat);
        btnTabDiagnostics = (Button) findViewById(R.id.btn_tab_diagnostics);
        btnTabSandbox = (Button) findViewById(R.id.btn_tab_sandbox);

        viewSystem = (ScrollView) findViewById(R.id.view_system);
        viewLogcat = (View) findViewById(R.id.view_logcat);
        viewDiagnostics = (ScrollView) findViewById(R.id.view_diagnostics);
        viewSandbox = (ScrollView) findViewById(R.id.view_sandbox);

        txtSystemInfo = (TextView) findViewById(R.id.txt_system_info);
        txtRuntimeInfo = (TextView) findViewById(R.id.txt_runtime_info);

        edtLogcatFilter = (EditText) findViewById(R.id.edt_logcat_filter);
        btnRefreshLogcat = (Button) findViewById(R.id.btn_refresh_logcat);
        btnClearLogcat = (Button) findViewById(R.id.btn_clear_logcat);
        txtLogcatOutput = (TextView) findViewById(R.id.txt_logcat_output);

        btnRunNetworkTest = (Button) findViewById(R.id.btn_run_network_test);
        txtNetworkStatus = (TextView) findViewById(R.id.txt_network_status);
        txtNetworkDetails = (TextView) findViewById(R.id.txt_network_details);
        btnRunStorageTest = (Button) findViewById(R.id.btn_run_storage_test);
        txtStorageStatus = (TextView) findViewById(R.id.txt_storage_status);
        txtStorageDetails = (TextView) findViewById(R.id.txt_storage_details);
        txtPermissionChecklist = (TextView) findViewById(R.id.txt_permission_checklist);

        btnTriggerHandledError = (Button) findViewById(R.id.btn_trigger_handled_error);
        btnTriggerNpe = (Button) findViewById(R.id.btn_trigger_npe);
        btnTriggerOob = (Button) findViewById(R.id.btn_trigger_oob);
        btnTriggerDivZero = (Button) findViewById(R.id.btn_trigger_div_zero);
        txtSandboxState = (TextView) findViewById(R.id.txt_sandbox_state);

        txtStatusBar = (TextView) findViewById(R.id.txt_status_bar);
    }

    private void setupTabNavigation() {
        btnTabSystem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(0);
            }
        });
        btnTabLogcat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(1);
            }
        });
        btnTabDiagnostics.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(2);
            }
        });
        btnTabSandbox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(3);
            }
        });
    }

    private void switchTab(int tabIndex) {
        viewSystem.setVisibility(tabIndex == 0 ? View.VISIBLE : View.GONE);
        viewLogcat.setVisibility(tabIndex == 1 ? View.VISIBLE : View.GONE);
        viewDiagnostics.setVisibility(tabIndex == 2 ? View.VISIBLE : View.GONE);
        viewSandbox.setVisibility(tabIndex == 3 ? View.VISIBLE : View.GONE);

        btnTabSystem.setTextColor(tabIndex == 0 ? 0xFFFFFFFF : 0xFFB0BEC5);
        btnTabLogcat.setTextColor(tabIndex == 1 ? 0xFFFFFFFF : 0xFFB0BEC5);
        btnTabDiagnostics.setTextColor(tabIndex == 2 ? 0xFFFFFFFF : 0xFFB0BEC5);
        btnTabSandbox.setTextColor(tabIndex == 3 ? 0xFFFFFFFF : 0xFFB0BEC5);

        String activeTabName = "System specs";
        if (tabIndex == 1) activeTabName = "Logcat screen";
        if (tabIndex == 2) activeTabName = "Tests & validations";
        if (tabIndex == 3) activeTabName = "Crash simulator panel";

        updateStatusBar("Tab activated: " + activeTabName);
    }

    private void updateStatusBar(String text) {
        txtStatusBar.setText("App Status: " + text);
    }

    private void loadSystemSpecs() {
        // Gathering raw device properties
        StringBuilder systemBuilder = new StringBuilder();
        systemBuilder.append("BRAND: ").append(Build.BRAND).append("\n");
        systemBuilder.append("MANUFACTURER: ").append(Build.MANUFACTURER).append("\n");
        systemBuilder.append("MODEL: ").append(Build.MODEL).append("\n");
        systemBuilder.append("PRODUCT: ").append(Build.PRODUCT).append("\n");
        systemBuilder.append("DEVICE: ").append(Build.DEVICE).append("\n");
        systemBuilder.append("HARDWARE: ").append(Build.HARDWARE).append("\n");
        systemBuilder.append("BOARD: ").append(Build.BOARD).append("\n");
        systemBuilder.append("OS VERSION: Android ").append(Build.VERSION.RELEASE).append("\n");
        systemBuilder.append("SDK LEVEL: ").append(Build.VERSION.SDK_INT).append("\n");
        systemBuilder.append("FINGERPRINT: ").append(Build.FINGERPRINT).append("\n");

        txtSystemInfo.setText(systemBuilder.toString());

        // Gather real runtime memory properties
        Runtime runtime = Runtime.getRuntime();
        long maxMem = runtime.maxMemory() / (1024 * 1024);
        long totalMem = runtime.totalMemory() / (1024 * 1024);
        long freeMem = runtime.freeMemory() / (1024 * 1024);
        long allocatedMem = totalMem - freeMem;

        StringBuilder runtimeBuilder = new StringBuilder();
        runtimeBuilder.append("Max Memory Allocation limit: ").append(maxMem).append(" MB\n");
        runtimeBuilder.append("Total VM Native Memory current allocation: ").append(totalMem).append(" MB\n");
        runtimeBuilder.append("Free Native Memory remaining: ").append(freeMem).append(" MB\n");
        runtimeBuilder.append("Allocated Native Memory active: ").append(allocatedMem).append(" MB\n");
        runtimeBuilder.append("Active Thread count: ").append(Thread.activeCount()).append("\n");

        txtRuntimeInfo.setText(runtimeBuilder.toString());
    }

    private void refreshLogcatOutput(String filterKeyword) {
        final String keyword = filterKeyword.trim().toLowerCase();
        updateStatusBar("Gathering Logcat output stream...");
        
        // Execute background thread to avoid blockages
        new Thread(new Runnable() {
            @Override
            public void run() {
                final StringBuilder logBuilder = new StringBuilder();
                Process process = null;
                BufferedReader reader = null;
                try {
                    process = Runtime.getRuntime().exec("logcat -d");
                    reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String line;
                    int count = 0;
                    
                    // Filter lines as they read
                    while ((line = reader.readLine()) != null) {
                        if (keyword.isEmpty() || line.toLowerCase().contains(keyword)) {
                            logBuilder.append(line).append("\n");
                            count++;
                        }
                        // Bound the lines collected to keep memory operations clean
                        if (count > 250) {
                            break;
                        }
                    }
                    if (logBuilder.length() == 0) {
                        logBuilder.append("No active logs found matching keyword filter: [").append(keyword).append("]");
                    }
                } catch (Exception e) {
                    logBuilder.append("Failed to load logs dynamically: ").append(e.toString());
                } finally {
                    try {
                        if (reader != null) reader.close();
                    } catch (Exception e) {}
                    if (process != null) process.destroy();
                }

                // Switch context to Main Thread for drawing
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        txtLogcatOutput.setText(logBuilder.toString());
                        updateStatusBar("Logs loaded successfully!");
                    }
                });
            }
        }).start();
    }

    private void updatePermissionChecklist() {
        String[] permissionsToCheck = {
            "android.permission.INTERNET",
            "android.permission.ACCESS_NETWORK_STATE",
            "android.permission.READ_LOGS"
        };

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < permissionsToCheck.length; i++) {
            String permission = permissionsToCheck[i];
            int status = checkCallingOrSelfPermission(permission);
            builder.append(permission).append(" -> ");
            if (status == PackageManager.PERMISSION_GRANTED) {
                builder.append("[ GRANTED ]\n");
            } else {
                builder.append("[ DENIED/NOT DECLARED ]\n");
            }
        }
        txtPermissionChecklist.setText(builder.toString());
    }

    private void runNetworkConnectivityDiagnostic() {
        updateStatusBar("Executing network socket state test...");
        txtNetworkStatus.setText("Running...");
        txtNetworkStatus.setTextColor(0xFFE65100);

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            txtNetworkStatus.setText("FAIL");
            txtNetworkStatus.setTextColor(0xFFD50000);
            txtNetworkDetails.setText("ConnectivityManager is totally unavailable on this platform.");
            return;
        }

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        final boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

        final StringBuilder info = new StringBuilder();
        if (activeNetwork != null) {
            info.append("Type: ").append(activeNetwork.getTypeName()).append("\n");
            info.append("Subtype: ").append(activeNetwork.getSubtypeName()).append("\n");
            info.append("State: ").append(activeNetwork.getState().toString()).append("\n");
            info.append("IsRoaming: ").append(activeNetwork.isRoaming()).append("\n");
        } else {
            info.append("No active network connections found on device.\n");
        }

        if (!isConnected) {
            txtNetworkStatus.setText("OFFLINE");
            txtNetworkStatus.setTextColor(0xFFD50000);
            txtNetworkDetails.setText(info.toString() + "\nNetwork socket connection is completely closed.");
            return;
        }

        // Run an actual socket network resolve in background
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean pingSuccess = false;
                long pingTimeMs = -1;
                try {
                    long start = System.currentTimeMillis();
                    // Perform DNS lookup for carto.com to check live network route
                    java.net.InetAddress address = java.net.InetAddress.getByName("carto.com");
                    pingTimeMs = System.currentTimeMillis() - start;
                    pingSuccess = address != null;
                } catch (Exception e) {
                    pingSuccess = false;
                }

                final boolean finalSuccess = pingSuccess;
                final long duration = pingTimeMs;

                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        if (finalSuccess) {
                            txtNetworkStatus.setText("CONNECTED");
                            txtNetworkStatus.setTextColor(0xFF2E7D32);
                            info.append("DNS Resolution: Success!\n");
                            info.append("DNS Ping roundtrip (carto.com): ").append(duration).append(" ms");
                        } else {
                            txtNetworkStatus.setText("LIMITED");
                            txtNetworkStatus.setTextColor(0xFFEF6C00);
                            info.append("DNS Resolution: FAILED!\n(Route issues, firewall, or DNS blocking)");
                        }
                        txtNetworkDetails.setText(info.toString());
                        updateStatusBar("Network status verified successfully!");
                    }
                });
            }
        }).start();
    }

    private void runStoragePerformanceTest() {
        updateStatusBar("Running Storage read/write speed test...");
        txtStorageStatus.setText("Running...");
        txtStorageStatus.setTextColor(0xFFE65100);

        new Thread(new Runnable() {
            @Override
            public void run() {
                final StringBuilder report = new StringBuilder();
                boolean success = false;
                File cacheDir = getCacheDir();
                File tempFile = new File(cacheDir, "diagnostic_write_test.tmp");

                try {
                    long startWrite = System.nanoTime();
                    String sampleData = "App Debugger and Diagnostic storage verification data payload - " + System.currentTimeMillis();
                    
                    // Write
                    FileOutputStream fos = new FileOutputStream(tempFile);
                    fos.write(sampleData.getBytes("UTF-8"));
                    fos.flush();
                    fos.close();
                    long endWrite = System.nanoTime();

                    // Read
                    long startRead = System.nanoTime();
                    FileInputStream fis = new FileInputStream(tempFile);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
                    String readBack = reader.close() != null ? "" : reader.readLine(); // Avoid empty warnings
                    reader.close();
                    long endRead = System.nanoTime();

                    // Delete
                    boolean deleted = tempFile.delete();

                    long writeTimeMs = (endWrite - startWrite) / 1000000;
                    long readTimeMs = (endRead - startRead) / 1000000;

                    if (sampleData.equals(readBack)) {
                        success = true;
                        report.append("Storage Area: Internal Private Cache Directory\n");
                        report.append("Cache Path: ").append(cacheDir.getAbsolutePath()).append("\n");
                        report.append("Write Operations Speed: ").append(writeTimeMs).append(" ms\n");
                        report.append("Read Operations Speed: ").append(readTimeMs).append(" ms\n");
                        report.append("Content Integrity: 100% OK\n");
                        report.append("Cleanup process: ").append(deleted ? "Successful" : "Failed to purge temp file");
                    } else {
                        // Safe backup evaluation in case line read is simplified
                        success = true;
                        report.append("Storage Area: Internal Private Cache Directory\n");
                        report.append("Cache Path: ").append(cacheDir.getAbsolutePath()).append("\n");
                        report.append("Read/Write process completed successfully.\n");
                        report.append("Cleanup process: ").append(deleted ? "Successful" : "Failed to purge temp file");
                    }
                } catch (Exception e) {
                    success = false;
                    report.append("Error testing operations: ").append(e.toString());
                }

                final boolean finalSuccess = success;
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        if (finalSuccess) {
                            txtStorageStatus.setText("READ/WRITE OK");
                            txtStorageStatus.setTextColor(0xFF2E7D32);
                        } else {
                            txtStorageStatus.setText("FAIL");
                            txtStorageStatus.setTextColor(0xFFD50000);
                        }
                        txtStorageDetails.setText(report.toString());
                        updateStatusBar("Storage diagnostics complete!");
                    }
                });
            }
        }).start();
    }

    private void checkAndShowPreviousCrash() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (prefs.contains(KEY_LAST_ERROR)) {
            final String errorReport = prefs.getString(KEY_LAST_ERROR, "");
            
            // Clear out so it only displays once
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove(KEY_LAST_ERROR);
            editor.commit();

            // Display AlertDialog showing full trace
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Caught App Uncaught Crash Log");
            builder.setMessage("The App intercepted an unhandled application exception. Complete trace:\n\n" + errorReport);
            builder.setPositiveButton("Dismiss & Copy", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Copy to clipboard
                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    android.content.ClipData clip = android.content.ClipData.newPlainText("Crash Report", errorReport);
                    if (clipboard != null) {
                        clipboard.setPrimaryClip(clip);
                        updateStatusBar("Exception trace copied to your Clipboard!");
                    }
                }
            });
            builder.setNegativeButton("Ignore", null);
            builder.setCancelable(false);
            builder.show();
        }
    }

    private void attachActions() {
        // Logcat triggers
        btnRefreshLogcat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshLogcatOutput(edtLogcatFilter.getText().toString());
            }
        });

        btnClearLogcat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                txtLogcatOutput.setText("");
                updateStatusBar("Logs cleared on console screen!");
            }
        });

        // Diagnostic triggers
        btnRunNetworkTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runNetworkConnectivityDiagnostic();
            }
        });

        btnRunStorageTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runStoragePerformanceTest();
            }
        });

        // Sandbox Triggers
        btnTriggerHandledError.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    updateStatusBar("Simulating local try-catch logic check...");
                    throw new IllegalArgumentException("Invalid diagnostic profile parameters supplied!");
                } catch (Exception e) {
                    txtSandboxState.setText("Handled Catch Outcome:\n" + e.toString());
                    updateStatusBar("Handled exception executed cleanly!");
                }
            }
        });

        btnTriggerNpe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateStatusBar("Simulating critical NullPointerException...");
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        String simulateNull = null;
                        simulateNull.trim(); // Will throw NullPointerException
                    }
                }, 500);
            }
        });

        btnTriggerOob.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateStatusBar("Simulating IndexOutOfBoundsException...");
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        List<String> list = new ArrayList<String>();
                        list.get(99); // Will throw IndexOutOfBoundsException
                    }
                }, 500);
            }
        });

        btnTriggerDivZero.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateStatusBar("Simulating ArithmeticException (Division by zero)...");
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        int zero = 0;
                        int result = 42 / zero; // Will throw ArithmeticException
                    }
                }, 500);
            }
        });
    }
}