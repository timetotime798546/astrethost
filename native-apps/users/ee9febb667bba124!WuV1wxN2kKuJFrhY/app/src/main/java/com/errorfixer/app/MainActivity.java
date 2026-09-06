package com.errorfixer.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private Button btnTabDiagnostics;
    private Button btnTabGuide;
    private Button btnTabCrash;

    private ScrollView layoutDiagnostics;
    private View layoutGuide;
    private ScrollView layoutCrash;

    private TextView txtDeviceModel;
    private TextView txtDeviceSdk;
    private TextView txtDeviceBrand;
    private TextView txtNetStatus;
    private Button btnTestNet;
    private TextView txtHeapInfo;
    private TextView txtHeapMax;
    private TextView txtPermissionState;
    private Button btnReqPermission;

    private EditText searchBox;
    private ListView listErrors;

    private CheckBox chkInterceptor;
    private Button btnTriggerNpe;
    private Button btnTriggerArithmetic;
    private Button btnTriggerOops;

    private List<AppError> allErrors;
    private List<AppError> displayedErrors;
    private ArrayAdapter<AppError> errorAdapter;

    private Thread.UncaughtExceptionHandler defaultHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Dynamic resource ID mapping to comply with package compilation environments
        int layoutId = getResources().getIdentifier("activity_main", "layout", getPackageName());
        setContentView(layoutId);

        defaultHandler = Thread.getDefaultUncaughtExceptionHandler();

        setupViews();
        setupNavigation();
        setupDiagnostics();
        setupErrorGuide();
        setupSandbox();
        setupGlobalExceptionHandler();

        // Validate if standard crash parameters exist in the launching intent
        checkForCrashReport();
    }

    private void setupViews() {
        btnTabDiagnostics = (Button) findViewById(getResources().getIdentifier("btn_tab_diagnostics", "id", getPackageName()));
        btnTabGuide = (Button) findViewById(getResources().getIdentifier("btn_tab_guide", "id", getPackageName()));
        btnTabCrash = (Button) findViewById(getResources().getIdentifier("btn_tab_crash", "id", getPackageName()));

        layoutDiagnostics = (ScrollView) findViewById(getResources().getIdentifier("layout_diagnostics", "id", getPackageName()));
        layoutGuide = findViewById(getResources().getIdentifier("layout_guide", "id", getPackageName()));
        layoutCrash = (ScrollView) findViewById(getResources().getIdentifier("layout_crash", "id", getPackageName()));

        txtDeviceModel = (TextView) findViewById(getResources().getIdentifier("txt_device_model", "id", getPackageName()));
        txtDeviceSdk = (TextView) findViewById(getResources().getIdentifier("txt_device_sdk", "id", getPackageName()));
        txtDeviceBrand = (TextView) findViewById(getResources().getIdentifier("txt_device_brand", "id", getPackageName()));
        txtNetStatus = (TextView) findViewById(getResources().getIdentifier("txt_net_status", "id", getPackageName()));
        btnTestNet = (Button) findViewById(getResources().getIdentifier("btn_test_net", "id", getPackageName()));
        txtHeapInfo = (TextView) findViewById(getResources().getIdentifier("txt_heap_info", "id", getPackageName()));
        txtHeapMax = (TextView) findViewById(getResources().getIdentifier("txt_heap_max", "id", getPackageName()));
        txtPermissionState = (TextView) findViewById(getResources().getIdentifier("txt_permission_state", "id", getPackageName()));
        btnReqPermission = (Button) findViewById(getResources().getIdentifier("btn_req_permission", "id", getPackageName()));

        searchBox = (EditText) findViewById(getResources().getIdentifier("search_box", "id", getPackageName()));
        listErrors = (ListView) findViewById(getResources().getIdentifier("list_errors", "id", getPackageName()));

        chkInterceptor = (CheckBox) findViewById(getResources().getIdentifier("chk_interceptor", "id", getPackageName()));
        btnTriggerNpe = (Button) findViewById(getResources().getIdentifier("btn_trigger_npe", "id", getPackageName()));
        btnTriggerArithmetic = (Button) findViewById(getResources().getIdentifier("btn_trigger_arithmetic", "id", getPackageName()));
        btnTriggerOops = (Button) findViewById(getResources().getIdentifier("btn_trigger_oops", "id", getPackageName()));
    }

    private void setupNavigation() {
        btnTabDiagnostics.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(1);
            }
        });

        btnTabGuide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(2);
            }
        });

        btnTabCrash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(3);
            }
        });
    }

    private void switchTab(int tabIndex) {
        btnTabDiagnostics.setBackgroundColor(0xFF34495E);
        btnTabDiagnostics.setTextColor(0xFFCCCCCC);
        btnTabGuide.setBackgroundColor(0xFF34495E);
        btnTabGuide.setTextColor(0xFFCCCCCC);
        btnTabCrash.setBackgroundColor(0xFF34495E);
        btnTabCrash.setTextColor(0xFFCCCCCC);

        layoutDiagnostics.setVisibility(View.GONE);
        layoutGuide.setVisibility(View.GONE);
        layoutCrash.setVisibility(View.GONE);

        if (tabIndex == 1) {
            btnTabDiagnostics.setBackgroundColor(0xFF1ABC9C);
            btnTabDiagnostics.setTextColor(0xFFFFFFFF);
            layoutDiagnostics.setVisibility(View.VISIBLE);
        } else if (tabIndex == 2) {
            btnTabGuide.setBackgroundColor(0xFF1ABC9C);
            btnTabGuide.setTextColor(0xFFFFFFFF);
            layoutGuide.setVisibility(View.VISIBLE);
        } else if (tabIndex == 3) {
            btnTabCrash.setBackgroundColor(0xFF1ABC9C);
            btnTabCrash.setTextColor(0xFFFFFFFF);
            layoutCrash.setVisibility(View.VISIBLE);
        }
    }

    private void setupDiagnostics() {
        // Evaluate Device Build specifications
        txtDeviceModel.setText("Model: " + Build.MODEL);
        txtDeviceSdk.setText("Android Version: " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")");
        txtDeviceBrand.setText("Brand: " + Build.BRAND.toUpperCase(Locale.ENGLISH));

        // Evaluate Dynamic Network metrics
        checkNetworkStatus();
        btnTestNet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkNetworkStatus();
                Toast.makeText(MainActivity.this, "Network stats updated successfully!", Toast.LENGTH_SHORT).show();
            }
        });

        // Evaluate Local JVM heap boundaries
        long freeMemory = Runtime.getRuntime().freeMemory();
        long maxMemory = Runtime.getRuntime().maxMemory();
        long totalMemory = Runtime.getRuntime().totalMemory();

        txtHeapInfo.setText("JVM Heap (Total/Free): " + (totalMemory / (1024 * 1024)) + " MB / " + (freeMemory / (1024 * 1024)) + " MB");
        txtHeapMax.setText("JVM Max Allocation Limit: " + (maxMemory / (1024 * 1024)) + " MB");

        // Dynamic Permissions Evaluation
        updatePermissionStatus();
        btnReqPermission.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 101);
                } else {
                    Toast.makeText(MainActivity.this, "Permission automatically granted by manifest specifications (API < 23).", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void checkNetworkStatus() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            if (activeNetwork != null && activeNetwork.isConnectedOrConnecting()) {
                String typeName = activeNetwork.getTypeName();
                txtNetStatus.setText("Status: CONNECTED (" + typeName + ")");
                txtNetStatus.setTextColor(0xFF27AE60);
            } else {
                txtNetStatus.setText("Status: DISCONNECTED");
                txtNetStatus.setTextColor(0xFFC0392B);
            }
        } else {
            txtNetStatus.setText("Status: Check Failed");
            txtNetStatus.setTextColor(0xFFE67E22);
        }
    }

    private void updatePermissionStatus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int status = checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION);
            if (status == PackageManager.PERMISSION_GRANTED) {
                txtPermissionState.setText("Location State: GRANTED");
                txtPermissionState.setTextColor(0xFF27AE60);
                btnReqPermission.setVisibility(View.GONE);
            } else {
                txtPermissionState.setText("Location State: DENIED");
                txtPermissionState.setTextColor(0xFFC0392B);
                btnReqPermission.setVisibility(View.VISIBLE);
            }
        } else {
            txtPermissionState.setText("Location State: GRANTED (Pre-Marshmallow rules)");
            txtPermissionState.setTextColor(0xFF27AE60);
            btnReqPermission.setVisibility(View.GONE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            updatePermissionStatus();
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Fine location permission granted!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Fine location permission denied.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setupErrorGuide() {
        allErrors = new ArrayList<AppError>();
        populateErrorGuide();

        displayedErrors = new ArrayList<AppError>(allErrors);

        errorAdapter = new ArrayAdapter<AppError>(this, android.R.layout.simple_list_item_2, android.R.id.text1, displayedErrors) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                TextView text2 = (TextView) view.findViewById(android.R.id.text2);

                AppError item = getItem(position);
                if (item != null) {
                    text1.setText(item.title);
                    text1.setTextSize(15);
                    text1.setTextColor(0xFF2C3E50);
                    text1.setTypeface(null, android.graphics.Typeface.BOLD);

                    String desc = item.description;
                    if (desc.length() > 65) {
                        desc = desc.substring(0, 62) + "...";
                    }
                    text2.setText(desc);
                    text2.setTextSize(12);
                    text2.setTextColor(0xFF7F8C8D);
                }
                return view;
            }
        };

        listErrors.setAdapter(errorAdapter);

        listErrors.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AppError selected = displayedErrors.get(position);
                showErrorDetailDialog(selected);
            }
        });

        searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterErrors(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterErrors(String query) {
        displayedErrors.clear();
        String lowerQuery = query.toLowerCase(Locale.ENGLISH);
        for (int i = 0; i < allErrors.size(); i++) {
            AppError err = allErrors.get(i);
            if (err.title.toLowerCase(Locale.ENGLISH).contains(lowerQuery) ||
                err.description.toLowerCase(Locale.ENGLISH).contains(lowerQuery) ||
                err.solution.toLowerCase(Locale.ENGLISH).contains(lowerQuery)) {
                displayedErrors.add(err);
            }
        }
        errorAdapter.notifyDataSetChanged();
    }

    private void showErrorDetailDialog(AppError error) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(error.title);
        
        String message = "DETAILED PROBLEM EXPLANATION:\n" + error.description + 
                         "\n\n=================================\n\n" +
                         "HOW TO FIX / RESOLUTION CODE:\n" + error.solution;
        
        builder.setMessage(message);
        builder.setPositiveButton("Dismiss", null);
        
        AlertDialog dialog = builder.create();
        dialog.show();
        
        TextView textView = (TextView) dialog.findViewById(android.R.id.message);
        if (textView != null) {
            textView.setTextSize(12);
            textView.setTypeface(android.graphics.Typeface.MONOSPACE);
        }
    }

    private void populateErrorGuide() {
        allErrors.add(new AppError(
            "Manifest Namespace Validation Bug",
            "Build systems crash stating 'Missing name key attribute on element activity/action' even though physical properties appear fully written inside the Manifest file.",
            "Verify the exact root namespace url mapping. If the manifest has a typo pointing to Google servers instead of Android open-source servers, elements will map poorly.\n\n" +
            "CORRECT CONFIGURATION:\nxmlns:android=\"http://schemas.android.com/apk/res/android\"\n\n" +
            "INCORRECT TYPO:\nxmlns:android=\"http://schemas.google.com/apk/res/android\""
        ));

        allErrors.add(new AppError(
            "OSM Map Tile Blocking Exception (418)",
            "WebView integrations using Leaflet.js or standard OpenStreetMap public tiles suddenly crash or display absolute grey tiles with immediate HTTP 418 code errors.",
            "Never implement public OSM map layer paths directly inside WebView structures without user headers. Transition completely to CARTO's free Voyager basemap URL systems:\n\n" +
            "URL:\nhttps://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png\n\n" +
            "Additionally, initialize loadDataWithBaseURL pointing references directly to: 'https://carto.com'"
        ));

        allErrors.add(new AppError(
            "Cleartext HTTP Connection Blocked",
            "Attempts to run HTTP calls to external API databases fail silently or dump security errors saying cleartext communication is not permitted on target API 28+ devices.",
            "Enforce secure connections (HTTPS). For development, bypass security by modifying the manifest application block:\n\n" +
            "android:usesCleartextTraffic=\"true\""
        ));

        allErrors.add(new AppError(
            "SecurityException: Missing Permission",
            "Starting processes related to tracking locations, accessing devices, or capturing images crash instantly throwing a runtime Exception.",
            "Ensure system permissions are declared in the Manifest. On Marshmallow (API 23) and above, execute explicit checks and call runtime prompts:\n\n" +
            "if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {\n" +
            "    requestPermissions(new String[]{Manifest.permission.CAMERA}, 101);\n" +
            "}"
        ));

        allErrors.add(new AppError(
            "Gradle Version Mismatch Exception",
            "Compiling crashes saying 'Unsupported class file major version' when matching new Gradle scripts with old JDK command tools.",
            "Gradle 8.5 & AGP 8.3.2 mandate compilation SDK target levels of 34, backed strictly by a Java JDK 17 environment. Configure the workspace toolsets to match Temurin 17 guidelines."
        ));

        allErrors.add(new AppError(
            "Theme.AppCompat Support Exception",
            "Starting execution of standard activities crashes immediately logging: 'You need to use a Theme.AppCompat theme (or descendant) with this activity'.",
            "This crash happens when extending 'AppCompatActivity' but linking standard platform styles inside the manifest. Either subclass the basic platform class 'Activity' directly, or swap manifest application theme tags to compatible AppCompat types."
        ));
    }

    private void setupSandbox() {
        final SharedPreferences prefs = getSharedPreferences("diagnostic_prefs", Context.MODE_PRIVATE);
        boolean isEnabled = prefs.getBoolean("interceptor_enabled", true);
        chkInterceptor.setChecked(isEnabled);

        chkInterceptor.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean("interceptor_enabled", isChecked).apply();
                Toast.makeText(MainActivity.this, 
                    isChecked ? "Custom Exception Interceptor: ENABLED" : "Using standard system crash handler", 
                    Toast.LENGTH_SHORT).show();
            }
        });

        btnTriggerNpe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Generate controlled NullPointerException
                String nullPointerString = null;
                int len = nullPointerString.length();
            }
        });

        btnTriggerArithmetic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Generate division by zero ArithmeticException
                int zeroValue = 0;
                int triggerValue = 999 / zeroValue;
            }
        });

        btnTriggerOops.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                throw new RuntimeException("This is a custom simulated runtime error triggered inside the Crash Sandbox panel!");
            }
        });
    }

    private void setupGlobalExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                SharedPreferences prefs = getSharedPreferences("diagnostic_prefs", Context.MODE_PRIVATE);
                boolean enabled = prefs.getBoolean("interceptor_enabled", true);

                if (enabled) {
                    // Convert captured trace to diagnostic string
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    throwable.printStackTrace(pw);
                    String stackTrace = sw.toString();

                    // Relaunch the MainActivity safely with absolute task clear flags
                    Intent intent = new Intent(MainActivity.this, MainActivity.class);
                    intent.putExtra("crash_log", stackTrace);
                    intent.putExtra("crash_type", throwable.getClass().getSimpleName());
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                    startActivity(intent);

                    // Terminate crashed instance immediately so system crash popups do not intercept
                    android.os.Process.killProcess(android.os.Process.myPid());
                    System.exit(10);
                } else {
                    if (defaultHandler != null) {
                        defaultHandler.uncaughtException(thread, throwable);
                    }
                }
            }
        });
    }

    private void checkForCrashReport() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("crash_log")) {
            String log = intent.getStringExtra("crash_log");
            String type = intent.getStringExtra("crash_type");

            showInteractiveCrashReport(type, log);
        }
    }

    private void showInteractiveCrashReport(String exceptionType, String stackTrace) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("⚠ Sandbox Exception Caught!");
        
        String message = "CRASH DIAGNOSTICS LOG:\n" +
                         "An exception of type '" + exceptionType + "' was intercepted by the Error Fixer global sandbox handler.\n\n" +
                         "TUTORIAL: Review the stack trace below. Locate lines matching '" + getPackageName() + "' to discover exactly where the issue lies!\n\n" +
                         "INTERCEPTED TRACE:\n" + stackTrace;

        builder.setMessage(message);
        builder.setPositiveButton("Continue", null);
        builder.setNeutralButton("Open Crash Tab", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switchTab(3);
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();

        TextView textView = (TextView) dialog.findViewById(android.R.id.message);
        if (textView != null) {
            textView.setTextSize(10);
            textView.setTypeface(android.graphics.Typeface.MONOSPACE);
            textView.setTextColor(0xFFC0392B);
        }
    }

    // Diagnostic help structure data class
    private static class AppError {
        String title;
        String description;
        String solution;

        AppError(String title, String description, String solution) {
            this.title = title;
            this.description = description;
            this.solution = solution;
        }
    }
}