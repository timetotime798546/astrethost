package com.errorfixerpro.app;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    // Common Troubleshooter Error Model
    public static class AndroidError {
        private String title;
        private String summary;
        private String cause;
        private String solution;

        public AndroidError(String title, String summary, String cause, String solution) {
            this.title = title;
            this.summary = summary;
            this.cause = cause;
            this.solution = solution;
        }

        public String getTitle() { return title; }
        public String getSummary() { return summary; }
        public String getCause() { return cause; }
        public String getSolution() { return solution; }
    }

    // Tabs navigation buttons
    private Button btnTabTroubleshoot;
    private Button btnTabAnalyzer;
    private Button btnTabTracker;

    // View Containers
    private LinearLayout layoutTroubleshoot;
    private ScrollView layoutAnalyzer;
    private LinearLayout layoutTracker;

    // UI elements inside Troubleshooter
    private EditText etSearchError;
    private ListView lvCommonErrors;
    private ScrollView layoutErrorDetail;
    private Button btnDetailBack;
    private TextView tvDetailTitle;
    private TextView tvDetailCause;
    private TextView tvDetailSolution;

    // UI elements inside Log Analyzer
    private EditText etLogInput;
    private Button btnAnalyzeLog;
    private LinearLayout layoutAnalysisResults;
    private TextView tvAnalysisSummary;
    private TextView tvAnalysisSolution;

    // UI elements inside Bug Tracker
    private EditText etBugTitle;
    private EditText etBugDesc;
    private Spinner spinnerPriority;
    private Button btnSaveBug;
    private ListView lvMyBugs;

    // Datasets & Adaptors
    private List<AndroidError> allErrors;
    private CommonErrorAdapter commonErrorAdapter;
    private BugDatabaseHelper dbHelper;
    private BugAdapter bugAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new BugDatabaseHelper(this);

        // Bind core tab navigation elements
        btnTabTroubleshoot = findViewById(R.id.btn_tab_troubleshoot);
        btnTabAnalyzer = findViewById(R.id.btn_tab_analyzer);
        btnTabTracker = findViewById(R.id.btn_tab_tracker);

        layoutTroubleshoot = findViewById(R.id.layout_troubleshoot);
        layoutAnalyzer = findViewById(R.id.layout_analyzer);
        layoutTracker = findViewById(R.id.layout_tracker);

        // Navigation setup using anonymous classes
        btnTabTroubleshoot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectTab(0);
            }
        });

        btnTabAnalyzer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectTab(1);
            }
        });

        btnTabTracker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectTab(2);
            }
        });

        // Setup Panel 1: Troubleshooter
        etSearchError = findViewById(R.id.et_search_error);
        lvCommonErrors = findViewById(R.id.lv_common_errors);
        layoutErrorDetail = findViewById(R.id.layout_error_detail);
        btnDetailBack = findViewById(R.id.btn_detail_back);
        tvDetailTitle = findViewById(R.id.tv_detail_title);
        tvDetailCause = findViewById(R.id.tv_detail_cause);
        tvDetailSolution = findViewById(R.id.tv_detail_solution);

        initCommonErrorsData();

        commonErrorAdapter = new CommonErrorAdapter(allErrors);
        lvCommonErrors.setAdapter(commonErrorAdapter);

        lvCommonErrors.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AndroidError selected = (AndroidError) commonErrorAdapter.getItem(position);
                showErrorDetail(selected);
            }
        });

        btnDetailBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                layoutErrorDetail.setVisibility(View.GONE);
                lvCommonErrors.setVisibility(View.VISIBLE);
                etSearchError.setVisibility(View.VISIBLE);
            }
        });

        etSearchError.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterErrors(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Setup Panel 2: Log Analyzer
        etLogInput = findViewById(R.id.et_log_input);
        btnAnalyzeLog = findViewById(R.id.btn_analyze_log);
        layoutAnalysisResults = findViewById(R.id.layout_analysis_results);
        tvAnalysisSummary = findViewById(R.id.tv_analysis_summary);
        tvAnalysisSolution = findViewById(R.id.tv_analysis_solution);

        btnAnalyzeLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                analyzeLog(etLogInput.getText().toString());
            }
        });

        // Setup Panel 3: Bug Tracker
        etBugTitle = findViewById(R.id.et_bug_title);
        etBugDesc = findViewById(R.id.et_bug_desc);
        spinnerPriority = findViewById(R.id.spinner_priority);
        btnSaveBug = findViewById(R.id.btn_save_bug);
        lvMyBugs = findViewById(R.id.lv_my_bugs);

        // Spinners binding
        String[] priorities = new String[]{"Low", "Medium", "High"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, priorities);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPriority.setAdapter(spinnerAdapter);

        btnSaveBug.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveBug();
            }
        });

        refreshBugsList();
    }

    private void selectTab(int tabIndex) {
        if (tabIndex == 0) {
            layoutTroubleshoot.setVisibility(View.VISIBLE);
            layoutAnalyzer.setVisibility(View.GONE);
            layoutTracker.setVisibility(View.GONE);

            btnTabTroubleshoot.setBackgroundColor(0xFF1976D2);
            btnTabTroubleshoot.setTextColor(0xFFFFFFFF);

            btnTabAnalyzer.setBackgroundColor(0xFFE3F2FD);
            btnTabAnalyzer.setTextColor(0xFF1976D2);

            btnTabTracker.setBackgroundColor(0xFFE3F2FD);
            btnTabTracker.setTextColor(0xFF1976D2);
        } else if (tabIndex == 1) {
            layoutTroubleshoot.setVisibility(View.GONE);
            layoutAnalyzer.setVisibility(View.VISIBLE);
            layoutTracker.setVisibility(View.GONE);

            btnTabTroubleshoot.setBackgroundColor(0xFFE3F2FD);
            btnTabTroubleshoot.setTextColor(0xFF1976D2);

            btnTabAnalyzer.setBackgroundColor(0xFF1976D2);
            btnTabAnalyzer.setTextColor(0xFFFFFFFF);

            btnTabTracker.setBackgroundColor(0xFFE3F2FD);
            btnTabTracker.setTextColor(0xFF1976D2);
        } else {
            layoutTroubleshoot.setVisibility(View.GONE);
            layoutAnalyzer.setVisibility(View.GONE);
            layoutTracker.setVisibility(View.VISIBLE);

            btnTabTroubleshoot.setBackgroundColor(0xFFE3F2FD);
            btnTabTroubleshoot.setTextColor(0xFF1976D2);

            btnTabAnalyzer.setBackgroundColor(0xFFE3F2FD);
            btnTabAnalyzer.setTextColor(0xFF1976D2);

            btnTabTracker.setBackgroundColor(0xFF1976D2);
            btnTabTracker.setTextColor(0xFFFFFFFF);
        }
    }

    private void initCommonErrorsData() {
        allErrors = new ArrayList<>();
        allErrors.add(new AndroidError(
                "NullPointerException (NPE)",
                "Attempt to invoke virtual method on a null object reference.",
                "Occurs when calling dynamic methods, reading properties, or referencing a variable containing null without instantiation.",
                "Ensure object variables are instantiated before access. Utilize defensive conditional checks: 'if (variable != null) { variable.execute(); }'. Check that findViewById matches layout definitions correctly."
        ));
        allErrors.add(new AndroidError(
                "ActivityNotFoundException",
                "Unable to find explicit activity class; have you declared this activity in your AndroidManifest.xml?",
                "Triggered when launching an Intent targeting an Activity class that does not exist in the app's Manifest configuration.",
                "Ensure your target Activity is declared within the <application> element inside 'AndroidManifest.xml'. Example: <activity android:name=\".MyTargetActivity\" />"
        ));
        allErrors.add(new AndroidError(
                "NetworkOnMainThreadException",
                "An attempt was made to perform a networking operation on the main application thread.",
                "The system blocks synchronous HTTP network calls on the main execution thread to avoid hanging the visual user interface.",
                "Offload web operations to static asynchronous frameworks. Run tasks in a separate background Executor Service or standard Thread, updating the UI using Handlers or runOnUiThread()."
        ));
        allErrors.add(new AndroidError(
                "Cleartext HTTP Traffic Blocked",
                "Cleartext HTTP traffic to host not permitted.",
                "Modern Android API versions block unencrypted HTTP communication by default to guarantee developer and customer security.",
                "Upgrade connection targets to encrypted HTTPS. If strictly necessary, configure 'android:usesCleartextTraffic=\"true\"' inside your <application> node in AndroidManifest.xml."
        ));
        allErrors.add(new AndroidError(
                "SecurityException (Permission Denied)",
                "Requires permission or user authentication.",
                "Attempting to invoke sensitive protected functions (such as Camera, GPS Location, or Storage access) without declarations or user authorization.",
                "Declare matching permission in AndroidManifest.xml (e.g. <uses-permission android:name=\"android.permission.CAMERA\" />) and dynamically prompt requestPermissions() for user approval at runtime."
        ));
        allErrors.add(new AndroidError(
                "OutOfMemoryError (OOM)",
                "The application has depleted the maximum allocated memory heap.",
                "Loading oversized image bitmaps directly, failing to clear database cursor resources, or retaining static context cycles.",
                "Optimize assets processing. Rescale loaded bitmaps, clear listeners, recycle database assets, and append android:largeHeap=\"true\" in manifest for temporary runtime cushion."
        ));
        allErrors.add(new AndroidError(
                "ClassCastException",
                "Object casting is invalid for targeting object type.",
                "Casting UI views or custom database objects to classes of which they are not true child instances.",
                "Double-check visual types. Ensure XML widget IDs match requested class types during casting. Apply protective 'instanceof' conditions."
        ));
        allErrors.add(new AndroidError(
                "Manifest Merger Failed",
                "Errors encountered during build when consolidating project dependency manifests.",
                "Conflicting library variables such as targetSdkVersion, themes, or package references.",
                "Verify values in build.gradle matches external library versions. Inspect compile logs. Integrate 'tools:replace=\"android:theme\"' under application properties to resolve specific mergers."
        ));
    }

    private void filterErrors(String query) {
        if (query == null || query.trim().isEmpty()) {
            commonErrorAdapter.updateList(allErrors);
            return;
        }
        List<AndroidError> filtered = new ArrayList<>();
        String queryLower = query.toLowerCase();
        for (int i = 0; i < allErrors.size(); i++) {
            AndroidError err = allErrors.get(i);
            if (err.getTitle().toLowerCase().contains(queryLower) ||
                err.getSummary().toLowerCase().contains(queryLower) ||
                err.getCause().toLowerCase().contains(queryLower)) {
                filtered.add(err);
            }
        }
        commonErrorAdapter.updateList(filtered);
    }

    private void showErrorDetail(AndroidError error) {
        etSearchError.setVisibility(View.GONE);
        lvCommonErrors.setVisibility(View.GONE);
        layoutErrorDetail.setVisibility(View.VISIBLE);

        tvDetailTitle.setText(error.getTitle());
        tvDetailCause.setText(error.getCause());
        tvDetailSolution.setText(error.getSolution());
    }

    private void analyzeLog(String log) {
        if (log == null || log.trim().isEmpty()) {
            Toast.makeText(this, "Please paste an Android logcat stacktrace to analyze.", Toast.LENGTH_SHORT).show();
            return;
        }

        String exceptionType = "Generic/Custom Exception";
        String matchedSolution = "We parsed your log and recommend the following diagnostics:\n" +
                "1. Find the first occurrence of package paths (marked in bullets below).\n" +
                "2. Confirm parameters are instantiated properly.\n" +
                "3. Ensure the active view states and lifecycle transitions are handled safely.";

        if (log.contains("NullPointerException")) {
            exceptionType = "NullPointerException (NPE)";
            matchedSolution = "CRITICAL: You are trying to read/write properties or call methods on an uninitialized null object reference.\n\n" +
                    "Fixes:\n" +
                    "• Verify variables are instantiated correctly.\n" +
                    "• Add null safety conditions: if (obj != null) { ... }.\n" +
                    "• Check layout XML to ensure matching view IDs exists.";
        } else if (log.contains("ActivityNotFoundException")) {
            exceptionType = "ActivityNotFoundException";
            matchedSolution = "CRITICAL: The runtime target activity is missing in compile logs.\n\n" +
                    "Fixes:\n" +
                    "• Add the Activity block into your AndroidManifest.xml.\n" +
                    "• Code: <activity android:name=\".MyTargetActivity\" />";
        } else if (log.contains("NetworkOnMainThreadException")) {
            exceptionType = "NetworkOnMainThreadException";
            matchedSolution = "CRITICAL: Blocking networking request running directly on visual Main UI Thread.\n\n" +
                    "Fixes:\n" +
                    "• Offload your internet requests into a background Worker thread or standard Executor Service.";
        } else if (log.contains("SecurityException") || log.contains("Permission denied")) {
            exceptionType = "SecurityException (Permission Error)";
            matchedSolution = "CRITICAL: System functions called without necessary permission approvals.\n\n" +
                    "Fixes:\n" +
                    "• Declare permissions inside manifest <uses-permission> tags.\n" +
                    "• Trigger runtime user request dialogs on devices with API 23+.";
        } else if (log.contains("ClassCastException")) {
            exceptionType = "ClassCastException";
            matchedSolution = "CRITICAL: Invalid class type conversion executed.\n\n" +
                    "Fixes:\n" +
                    "• Verify layout definitions match target widget variables. Avoid converting TextViews into Buttons.";
        } else if (log.contains("OutOfMemoryError")) {
            exceptionType = "OutOfMemoryError";
            matchedSolution = "CRITICAL: Application memory bounds fully depleted.\n\n" +
                    "Fixes:\n" +
                    "• Compress bitmaps, clean listeners, reuse UI nodes, and utilize android:largeHeap=\"true\" properties.";
        } else if (log.contains("Cleartext HTTP traffic")) {
            exceptionType = "Cleartext HTTP Blocked";
            matchedSolution = "CRITICAL: Unsecured plain-text HTTP connections are disabled.\n\n" +
                    "Fixes:\n" +
                    "• Upgrade connection hosts to HTTPS or configure usesCleartextTraffic=\"true\" in your manifest application settings.";
        }

        // Trace search algorithms
        StringBuilder traceResults = new StringBuilder();
        boolean foundLines = false;
        String[] logsArray = log.split("\n");
        for (int i = 0; i < logsArray.length; i++) {
            String line = logsArray[i];
            if (line.contains("at ") && (line.contains(".java:") || line.contains(".kt:"))) {
                int startIdx = line.lastIndexOf('(');
                int endIdx = line.lastIndexOf(')');
                if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
                    String fileLine = line.substring(startIdx + 1, endIdx);
                    int prefix = line.indexOf("at ");
                    String details = line.substring(prefix + 3, startIdx).trim();
                    traceResults.append("\n• Trace location: ").append(details).append(" (").append(fileLine).append(")");
                    foundLines = true;
                }
            }
        }

        layoutAnalysisResults.setVisibility(View.VISIBLE);
        tvAnalysisSummary.setText("Detected issue: " + exceptionType);

        String fullReport = matchedSolution;
        if (foundLines) {
            fullReport += "\n\nDETAILED TRACE ANALYSIS:" + traceResults.toString();
        } else {
            fullReport += "\n\nDETAILED TRACE ANALYSIS:\nCould not isolate target stacktrace package names. Please copy and paste the full exception error stack.";
        }
        tvAnalysisSolution.setText(fullReport);
    }

    private void saveBug() {
        String title = etBugTitle.getText().toString().trim();
        String desc = etBugDesc.getText().toString().trim();
        String priority = spinnerPriority.getSelectedItem().toString();

        if (title.isEmpty() || desc.isEmpty()) {
            Toast.makeText(this, "Please write both bug title and description.", Toast.LENGTH_SHORT).show();
            return;
        }

        dbHelper.addBug(title, desc, priority, "Pending");
        etBugTitle.setText("");
        etBugDesc.setText("");
        Toast.makeText(this, "Bug report saved successfully!", Toast.LENGTH_SHORT).show();

        refreshBugsList();
    }

    private void refreshBugsList() {
        List<Bug> bugList = dbHelper.getAllBugs();
        if (bugAdapter == null) {
            bugAdapter = new BugAdapter(bugList);
            lvMyBugs.setAdapter(bugAdapter);
        } else {
            bugAdapter.updateBugs(bugList);
        }
    }

    // Custom adapter for diagnostic errors list
    private class CommonErrorAdapter extends BaseAdapter {
        private List<AndroidError> displayedErrors;

        public CommonErrorAdapter(List<AndroidError> errors) {
            this.displayedErrors = errors;
        }

        public void updateList(List<AndroidError> newList) {
            this.displayedErrors = newList;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return displayedErrors.size();
        }

        @Override
        public Object getItem(int position) {
            return displayedErrors.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.item_common_error, parent, false);
            }
            TextView tvTitle = convertView.findViewById(R.id.tv_item_error_title);
            TextView tvSummary = convertView.findViewById(R.id.tv_item_error_summary);

            AndroidError error = displayedErrors.get(position);
            tvTitle.setText(error.getTitle());
            tvSummary.setText(error.getSummary());

            return convertView;
        }
    }

    // Custom adapter for logged bug items
    private class BugAdapter extends BaseAdapter {
        private List<Bug> bugs;

        public BugAdapter(List<Bug> bugs) {
            this.bugs = bugs;
        }

        public void updateBugs(List<Bug> newBugs) {
            this.bugs = newBugs;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return bugs.size();
        }

        @Override
        public Object getItem(int position) {
            return bugs.get(position);
        }

        @Override
        public long getItemId(int position) {
            return bugs.get(position).getId();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.item_bug, parent, false);
            }
            final Bug bug = bugs.get(position);

            TextView tvTitle = convertView.findViewById(R.id.tv_bug_title);
            TextView tvDesc = convertView.findViewById(R.id.tv_bug_desc);
            TextView tvPriority = convertView.findViewById(R.id.tv_bug_priority);
            TextView tvStatus = convertView.findViewById(R.id.tv_bug_status);
            Button btnToggle = convertView.findViewById(R.id.btn_bug_toggle);
            Button btnDelete = convertView.findViewById(R.id.btn_bug_delete);

            tvTitle.setText(bug.getTitle());
            tvDesc.setText(bug.getDescription());

            String priority = bug.getPriority();
            tvPriority.setText(priority);
            if ("High".equalsIgnoreCase(priority)) {
                tvPriority.setBackgroundColor(0xFFC62828); // Red
            } else if ("Medium".equalsIgnoreCase(priority)) {
                tvPriority.setBackgroundColor(0xFFEF6C00); // Orange
            } else {
                tvPriority.setBackgroundColor(0xFF2E7D32); // Green
            }

            String status = bug.getStatus();
            tvStatus.setText("Status: " + status);
            if ("Fixed".equalsIgnoreCase(status)) {
                tvStatus.setTextColor(0xFF2E7D32);
            } else {
                tvStatus.setTextColor(0xFFC62828);
            }

            btnToggle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String newStatus = "Fixed".equalsIgnoreCase(bug.getStatus()) ? "Pending" : "Fixed";
                    dbHelper.updateBugStatus(bug.getId(), newStatus);
                    refreshBugsList();
                }
            });

            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dbHelper.deleteBug(bug.getId());
                    refreshBugsList();
                }
            });

            return convertView;
        }
    }
}