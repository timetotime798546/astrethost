package com.adminpanel.app;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends Activity {

    private static final String CHANNEL_ID = "admin_alerts_channel";
    private static final String PREFS_NAME = "AdminPrefs";

    // Navigation Tabs
    private Button mTabDashboard;
    private Button mTabUsers;
    private Button mTabNotifications;
    private Button mTabSettings;

    // View Containers
    private ScrollView mViewDashboard;
    private LinearLayout mViewUsers;
    private LinearLayout mViewNotifications;
    private ScrollView mViewSettings;

    // Header State
    private TextView mSystemStatusIndicator;

    // Dashboard UI
    private TextView mMetricTotalUsers;
    private TextView mMetricActiveNow;
    private TextView mMetricRevenue;
    private TextView mMetricLogsCount;
    private Button mBtnTriggerBackup;
    private Button mBtnSimulateCrash;
    private TextView mTvConsoleLogs;

    // User Management UI
    private EditText mEtUsername;
    private EditText mEtUserRole;
    private Button mBtnAddUser;
    private ListView mListViewUsers;

    // Notification Hub UI
    private EditText mEtNotifyTitle;
    private EditText mEtNotifyBody;
    private Button mBtnDispatchNotification;

    // System Settings UI
    private CheckBox mCbMaintenance;
    private CheckBox mCbLogs;
    private CheckBox mCbSecure;
    private Button mBtnSaveSettings;

    // Mock Database Arrays
    private ArrayList<User> mUserList;
    private ArrayAdapter<User> mUserAdapter;
    private ArrayList<String> mLogEntries;
    
    private Handler mSystemSimulatorHandler;
    private Runnable mSimulatorRunnable;
    private Random mRandom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRandom = new Random();
        mLogEntries = new ArrayList<>();
        mUserList = new ArrayList<>();

        // Initialize elements
        initNavigation();
        initDashboard();
        initUserManagement();
        initNotificationHub();
        initSystemSettings();

        // Create Default Users
        populateInitialUsers();

        // Load Persisted Config
        loadSavedConfigState();

        // Create Alert Notification Channel
        createNotificationChannel();

        // Start dynamic console simulation
        appendConsoleLog("System Boot sequence completed successfully.");
        appendConsoleLog("All core systems are operational.");
        startSimulationDaemon();
    }

    private void initNavigation() {
        mTabDashboard = (Button) findViewById(R.id.tab_dashboard);
        mTabUsers = (Button) findViewById(R.id.tab_users);
        mTabNotifications = (Button) findViewById(R.id.tab_notifications);
        mTabSettings = (Button) findViewById(R.id.tab_settings);

        mViewDashboard = (ScrollView) findViewById(R.id.view_dashboard);
        mViewUsers = (LinearLayout) findViewById(R.id.view_users);
        mViewNotifications = (LinearLayout) findViewById(R.id.view_notifications);
        mViewSettings = (ScrollView) findViewById(R.id.view_settings);

        mSystemStatusIndicator = (TextView) findViewById(R.id.system_status_indicator);

        mTabDashboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchView(mViewDashboard, mTabDashboard);
            }
        });

        mTabUsers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchView(mViewUsers, mTabUsers);
            }
        });

        mTabNotifications.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchView(mViewNotifications, mTabNotifications);
            }
        });

        mTabSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchView(mViewSettings, mTabSettings);
            }
        });

        // Set Dashboard active initially
        resetTabButtonStyles();
        mTabDashboard.setBackgroundColor(Color.parseColor("#00B0FF"));
    }

    private void switchView(View viewToDisplay, Button activeTabButton) {
        mViewDashboard.setVisibility(View.GONE);
        mViewUsers.setVisibility(View.GONE);
        mViewNotifications.setVisibility(View.GONE);
        mViewSettings.setVisibility(View.GONE);

        viewToDisplay.setVisibility(View.VISIBLE);

        resetTabButtonStyles();
        activeTabButton.setBackgroundColor(Color.parseColor("#00B0FF"));
    }

    private void resetTabButtonStyles() {
        mTabDashboard.setBackgroundColor(Color.parseColor("#0D47A1"));
        mTabUsers.setBackgroundColor(Color.parseColor("#0D47A1"));
        mTabNotifications.setBackgroundColor(Color.parseColor("#0D47A1"));
        mTabSettings.setBackgroundColor(Color.parseColor("#0D47A1"));
    }

    private void initDashboard() {
        mMetricTotalUsers = (TextView) findViewById(R.id.metric_total_users);
        mMetricActiveNow = (TextView) findViewById(R.id.metric_active_now);
        mMetricRevenue = (TextView) findViewById(R.id.metric_revenue);
        mMetricLogsCount = (TextView) findViewById(R.id.metric_logs_count);
        mBtnTriggerBackup = (Button) findViewById(R.id.btn_trigger_backup);
        mBtnSimulateCrash = (Button) findViewById(R.id.btn_simulate_crash);
        mTvConsoleLogs = (TextView) findViewById(R.id.tv_console_logs);

        mBtnTriggerBackup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                appendConsoleLog("Database hotbackup execution triggered by admin.");
                appendConsoleLog("Optimizing indexes... done.");
                appendConsoleLog("Data compressed (14.2 MB saved).");
                appendConsoleLog("Backup sync completed to secure cloud vault.");
                Toast.makeText(MainActivity.this, "Encrypted db storage backup successfully deployed!", Toast.LENGTH_SHORT).show();
            }
        });

        mBtnSimulateCrash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                appendConsoleLog("[CRITICAL ALERT] Simulated System Health Check triggered.");
                appendConsoleLog("Executing diagnostics: Stack integrity valid.");
                appendConsoleLog("No crashes detected in sandbox threads.");
                Toast.makeText(MainActivity.this, "Diagnostics executed! Logs captured.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initUserManagement() {
        mEtUsername = (EditText) findViewById(R.id.et_username);
        mEtUserRole = (EditText) findViewById(R.id.et_user_role);
        mBtnAddUser = (Button) findViewById(R.id.btn_add_user);
        mListViewUsers = (ListView) findViewById(R.id.list_view_users);

        mBtnAddUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = mEtUsername.getText().toString().trim();
                String role = mEtUserRole.getText().toString().trim();

                if (name.isEmpty()) {
                    mEtUsername.setError("Name is required");
                    return;
                }
                if (role.isEmpty()) {
                    role = "User";
                }

                User newUser = new User(name, role, true);
                mUserList.add(0, newUser);
                mUserAdapter.notifyDataSetChanged();

                appendConsoleLog("Created profile for: " + name + " with role " + role);
                updateMetricCounts();

                mEtUsername.setText("");
                mEtUserRole.setText("");
                Toast.makeText(MainActivity.this, "User Access Provisioned!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initNotificationHub() {
        mEtNotifyTitle = (EditText) findViewById(R.id.et_notify_title);
        mEtNotifyBody = (EditText) findViewById(R.id.et_notify_body);
        mBtnDispatchNotification = (Button) findViewById(R.id.btn_dispatch_notification);

        mBtnDispatchNotification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = mEtNotifyTitle.getText().toString().trim();
                String body = mEtNotifyBody.getText().toString().trim();

                if (title.isEmpty() || body.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please provide title and body", Toast.LENGTH_SHORT).show();
                    return;
                }

                dispatchSystemNotification(title, body);
                appendConsoleLog("Broadcast dispatch: '" + title + "' sent to live clients.");
                Toast.makeText(MainActivity.this, "Broadcast dynamic payload dispatched!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initSystemSettings() {
        mCbMaintenance = (CheckBox) findViewById(R.id.cb_maintenance);
        mCbLogs = (CheckBox) findViewById(R.id.cb_logs);
        mCbSecure = (CheckBox) findViewById(R.id.cb_secure);
        mBtnSaveSettings = (Button) findViewById(R.id.btn_save_settings);

        mBtnSaveSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveConfigState();
            }
        });
    }

    private void populateInitialUsers() {
        mUserList.add(new User("Rajesh Kumar", "SuperAdmin", true));
        mUserList.add(new User("Priya Sharma", "Developer", true));
        mUserList.add(new User("Amit Patel", "Editor", false));
        mUserList.add(new User("Sneha Reddy", "User", true));
        mUserList.add(new User("Vikram Singh", "Moderator", false));

        mUserAdapter = new ArrayAdapter<User>(this, android.R.layout.simple_list_item_2, android.R.id.text1, mUserList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                TextView text2 = (TextView) view.findViewById(android.R.id.text2);

                User user = mUserList.get(position);
                text1.setText(user.getName());
                text1.setTextColor(Color.parseColor("#212121"));
                text1.setTextSize(14sp);

                text2.setText(user.getRole() + " • " + (user.isActive() ? "ACTIVE NOW" : "OFFLINE"));
                text2.setTextColor(user.isActive() ? Color.parseColor("#4CAF50") : Color.parseColor("#757575"));
                text2.setTextSize(11sp);

                return view;
            }
        };

        mListViewUsers.setAdapter(mUserAdapter);
        updateMetricCounts();
    }

    private void updateMetricCounts() {
        int total = mUserList.size();
        int active = 0;
        for (int i = 0; i < mUserList.size(); i++) {
            if (mUserList.get(i).isActive()) {
                active++;
            }
        }
        mMetricTotalUsers.setText(String.valueOf(total));
        mMetricActiveNow.setText(String.valueOf(active));
    }

    private void saveConfigState() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("maintenance", mCbMaintenance.isChecked());
        editor.putBoolean("verbose_logs", mCbLogs.isChecked());
        editor.putBoolean("secure_ssl", mCbSecure.isChecked());
        editor.apply();

        appendConsoleLog("System settings configuration updated by Administrator.");

        if (mCbMaintenance.isChecked()) {
            mSystemStatusIndicator.setText("● MAINTENANCE");
            mSystemStatusIndicator.setTextColor(Color.parseColor("#F44336"));
        } else {
            mSystemStatusIndicator.setText("● SYSTEM ONLINE");
            mSystemStatusIndicator.setTextColor(Color.parseColor("#4CAF50"));
        }

        Toast.makeText(this, "Local preferences saved dynamically!", Toast.LENGTH_SHORT).show();
    }

    private void loadSavedConfigState() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean maint = prefs.getBoolean("maintenance", false);
        boolean verbose = prefs.getBoolean("verbose_logs", true);
        boolean ssl = prefs.getBoolean("secure_ssl", true);

        mCbMaintenance.setChecked(maint);
        mCbLogs.setChecked(verbose);
        mCbSecure.setChecked(ssl);

        if (maint) {
            mSystemStatusIndicator.setText("● MAINTENANCE");
            mSystemStatusIndicator.setTextColor(Color.parseColor("#F44336"));
        } else {
            mSystemStatusIndicator.setText("● SYSTEM ONLINE");
            mSystemStatusIndicator.setTextColor(Color.parseColor("#4CAF50"));
        }
    }

    private void appendConsoleLog(String text) {
        String timestamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        String entry = "[" + timestamp + "] " + text;
        mLogEntries.add(entry);

        if (mLogEntries.size() > 50) {
            mLogEntries.remove(0);
        }

        StringBuilder builder = new StringBuilder();
        for (int i = mLogEntries.size() - 1; i >= 0; i--) {
            builder.append(mLogEntries.get(i)).append("\n");
        }

        mTvConsoleLogs.setText(builder.toString());
    }

    private void startSimulationDaemon() {
        mSystemSimulatorHandler = new Handler();
        mSimulatorRunnable = new Runnable() {
            @Override
            public void run() {
                // Randomly perform light simulation duties
                int activity = mRandom.nextInt(5);
                switch (activity) {
                    case 0:
                        int active = mRandom.nextInt(50) + 10;
                        mMetricActiveNow.setText(String.valueOf(active));
                        if (mCbLogs.isChecked()) {
                            appendConsoleLog("Server metric update: " + active + " real-time socket channels open.");
                        }
                        break;
                    case 1:
                        int rev = 1500 + mRandom.nextInt(400);
                        mMetricRevenue.setText("$" + rev);
                        if (mCbLogs.isChecked()) {
                            appendConsoleLog("Payment Gateway transaction committed successfully. Cumulative: $" + rev);
                        }
                        break;
                    case 2:
                        if (mUserList.size() > 0) {
                            int idx = mRandom.nextInt(mUserList.size());
                            User u = mUserList.get(idx);
                            u.setActive(!u.isActive());
                            mUserAdapter.notifyDataSetChanged();
                            updateMetricCounts();
                            if (mCbLogs.isChecked()) {
                                appendConsoleLog("Session state changed for: " + u.getName() + " -> " + (u.isActive() ? "Connected" : "Idle"));
                            }
                        }
                        break;
                    case 3:
                        if (mCbLogs.isChecked()) {
                            appendConsoleLog("Cleaning persistent socket thread leaks... cleared 0 idle connections.");
                        }
                        break;
                }

                // Reschedule with a random offset
                mSystemSimulatorHandler.postDelayed(this, 5000 + mRandom.nextInt(5000));
            }
        };

        mSystemSimulatorHandler.postDelayed(mSimulatorRunnable, 4000);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_desc);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void dispatchSystemNotification(String title, String body) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) return;

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        builder.setSmallIcon(android.R.drawable.stat_notify_chat)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true);

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSystemSimulatorHandler != null && mSimulatorRunnable != null) {
            mSystemSimulatorHandler.removeCallbacks(mSimulatorRunnable);
        }
    }
}