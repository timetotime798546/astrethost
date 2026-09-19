package com.sqlmanager.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {

    // View State enumeration
    private static final int VIEW_DASHBOARD = 1;
    private static final int VIEW_DATABASES = 2;
    private static final int VIEW_SQL_EDITOR = 3;
    private static final int VIEW_TABLES = 4;
    private static final int VIEW_TABLE_DATA = 5;
    private static final int VIEW_TABLE_STRUCTURE = 6;
    private static final int VIEW_HISTORY = 7;
    private static final int VIEW_SETTINGS = 8;

    private int currentView = VIEW_DASHBOARD;
    private final List<Integer> navigationStack = new ArrayList<>();

    // Global variables
    private String activeDbName = "default_db";
    private SQLiteDatabase activeDatabase = null;
    private SystemDbHelper systemDb;

    // Last SELECT records cached for Export functions
    private List<String[]> cachedResultRows = null;
    private String[] cachedResultColumns = null;

    // Active browsing parameters
    private String activeBrowsingTableName = "";

    // UI Widgets
    private Button backButton;
    private TextView titleText;
    private TextView activeDbBadge;

    // View Panel References
    private ScrollView dashboardPanel;
    private LinearLayout databasesPanel;
    private LinearLayout sqlEditorPanel;
    private LinearLayout tablesPanel;
    private LinearLayout tableDataPanel;
    private LinearLayout tableStructurePanel;
    private LinearLayout historyPanel;
    private ScrollView settingsPanel;

    // Dashboard widgets
    private TextView statDbCount, statTableCount, statRowCount;
    private TextView dashLastQuery;

    // Database panel widgets
    private LinearLayout databasesListContainer;

    // SQL Editor widgets
    private EditText sqlEditText;
    private TextView editorStatusBadge;
    private TableLayout resultsTableLayout;
    private View resultsHeaderView;
    private EditText editSearchResults;

    // Tables panel widgets
    private LinearLayout tablesListContainer;

    // Table Data widgets
    private TextView tableDataTitle;
    private TableLayout tableRecordsLayout;
    private EditText editSearchTableRows;

    // Table Structure widgets
    private TextView tableStructureTitle;
    private TableLayout tableSchemaLayout;

    // History log widgets
    private LinearLayout historyListContainer;

    // Examples Spinner Array
    private static final String[] SQL_EXAMPLES = {
            "Select SQL Example...",
            "CREATE TABLE IF NOT EXISTS users (\n  id INTEGER PRIMARY KEY AUTOINCREMENT,\n  name TEXT NOT NULL,\n  email TEXT,\n  age INTEGER\n);",
            "INSERT INTO users (name, email, age)\nVALUES ('John', 'john@example.com', 25);",
            "SELECT * FROM users;",
            "UPDATE users SET age = 26 WHERE id = 1;",
            "DELETE FROM users WHERE id = 1;",
            "SELECT COUNT(*) FROM users;"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Init System database
        systemDb = new SystemDbHelper(this);

        // Bind main views
        backButton = findViewById(R.id.back_button);
        titleText = findViewById(R.id.title_text);
        activeDbBadge = findViewById(R.id.active_db_badge);

        dashboardPanel = findViewById(R.id.dashboard_panel);
        databasesPanel = findViewById(R.id.databases_panel);
        sqlEditorPanel = findViewById(R.id.sql_editor_panel);
        tablesPanel = findViewById(R.id.tables_panel);
        tableDataPanel = findViewById(R.id.table_data_panel);
        tableStructurePanel = findViewById(R.id.table_structure_panel);
        historyPanel = findViewById(R.id.history_panel);
        settingsPanel = findViewById(R.id.settings_panel);

        // Bind Widgets
        statDbCount = findViewById(R.id.stat_db_count);
        statTableCount = findViewById(R.id.stat_table_count);
        statRowCount = findViewById(R.id.stat_row_count);
        dashLastQuery = findViewById(R.id.dash_last_query);

        databasesListContainer = findViewById(R.id.databases_list_container);

        sqlEditText = findViewById(R.id.sql_edit_text);
        editorStatusBadge = findViewById(R.id.editor_status_badge);
        resultsTableLayout = findViewById(R.id.results_table_layout);
        resultsHeaderView = findViewById(R.id.results_header_view);
        editSearchResults = findViewById(R.id.edit_search_results);

        tablesListContainer = findViewById(R.id.tables_list_container);

        tableDataTitle = findViewById(R.id.table_data_title);
        tableRecordsLayout = findViewById(R.id.table_records_layout);
        editSearchTableRows = findViewById(R.id.edit_search_table_rows);

        tableStructureTitle = findViewById(R.id.table_structure_title);
        tableSchemaLayout = findViewById(R.id.table_schema_layout);

        historyListContainer = findViewById(R.id.history_list_container);

        // Set up click listeners for Navigation Buttons
        findViewById(R.id.btn_to_databases).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateTo(VIEW_DATABASES);
            }
        });
        findViewById(R.id.btn_to_editor).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateTo(VIEW_SQL_EDITOR);
            }
        });
        findViewById(R.id.btn_to_tables).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateTo(VIEW_TABLES);
            }
        });
        findViewById(R.id.btn_to_history).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateTo(VIEW_HISTORY);
            }
        });
        findViewById(R.id.btn_to_settings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateTo(VIEW_SETTINGS);
            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        // Setup databases controls
        findViewById(R.id.btn_create_db).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCreateDatabaseDialog();
            }
        });

        // Setup SQL Exec controls
        findViewById(R.id.btn_execute_sql).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkAndExecuteSql(sqlEditText.getText().toString());
            }
        });
        findViewById(R.id.btn_clear_sql).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sqlEditText.setText("");
                clearEditorResults();
            }
        });

        // Load SQL examples spinner
        Spinner spinnerExamples = findViewById(R.id.spinner_examples);
        ArrayAdapter<String> examplesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, SQL_EXAMPLES);
        spinnerExamples.setAdapter(examplesAdapter);
        spinnerExamples.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    sqlEditText.setText(SQL_EXAMPLES[position]);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        findViewById(R.id.btn_export_csv).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportResultsToCSV();
            }
        });

        // Text search listeners for real-time local table grid filtering
        editSearchResults.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterTableResults(s.toString(), resultsTableLayout, cachedResultColumns, cachedResultRows);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        editSearchTableRows.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadTableDataView(activeBrowsingTableName, s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Setup Table Records control
        findViewById(R.id.btn_add_table_row).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddRowDialog(activeBrowsingTableName);
            }
        });

        // Setup History control
        findViewById(R.id.btn_clear_history_log).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                systemDb.clearHistory();
                populateHistoryList();
                Toast.makeText(MainActivity.this, "History log cleared.", Toast.LENGTH_SHORT).show();
            }
        });

        // Setup Settings control
        findViewById(R.id.btn_reset_all_data).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showConfirmationDialog("Reset Application", "Are you sure you want to delete all databases? This action is permanent.", new Runnable() {
                    @Override
                    public void run() {
                        resetAllDatabases();
                    }
                });
            }
        });

        // Restore last active database across restarts
        SharedPreferences prefs = getSharedPreferences("sql_manager_prefs", Context.MODE_PRIVATE);
        activeDbName = prefs.getString("active_db_name", "default_db");

        // Open/Init database on startup
        switchDatabase(activeDbName);
        navigateTo(VIEW_DASHBOARD);
    }

    private void switchDatabase(String dbName) {
        if (activeDatabase != null) {
            activeDatabase.close();
        }
        activeDbName = dbName;
        activeDatabase = openOrCreateDatabase(dbName + ".db", Context.MODE_PRIVATE, null);
        activeDbBadge.setText("DB: " + dbName);

        // Persist the active database name so it survives app restarts
        SharedPreferences prefs = getSharedPreferences("sql_manager_prefs", Context.MODE_PRIVATE);
        prefs.edit().putString("active_db_name", dbName).apply();

        updateDashboardStats();
    }

    private SQLiteDatabase getActiveDatabase() {
        if (activeDatabase == null || !activeDatabase.isOpen()) {
            activeDatabase = openOrCreateDatabase(activeDbName + ".db", Context.MODE_PRIVATE, null);
        }
        return activeDatabase;
    }

    // Dynamic screen panel swapper
    private void navigateTo(int viewId) {
        if (currentView != viewId) {
            navigationStack.add(currentView);
        }
        currentView = viewId;

        // Hide all screens
        dashboardPanel.setVisibility(View.GONE);
        databasesPanel.setVisibility(View.GONE);
        sqlEditorPanel.setVisibility(View.GONE);
        tablesPanel.setVisibility(View.GONE);
        tableDataPanel.setVisibility(View.GONE);
        tableStructurePanel.setVisibility(View.GONE);
        historyPanel.setVisibility(View.GONE);
        settingsPanel.setVisibility(View.GONE);

        backButton.setVisibility(navigationStack.isEmpty() ? View.GONE : View.VISIBLE);

        // Update titles and load subdata
        switch (viewId) {
            case VIEW_DASHBOARD:
                titleText.setText("SQL Database Manager");
                dashboardPanel.setVisibility(View.VISIBLE);
                updateDashboardStats();
                break;
            case VIEW_DATABASES:
                titleText.setText("Database List");
                databasesPanel.setVisibility(View.VISIBLE);
                populateDatabasesList();
                break;
            case VIEW_SQL_EDITOR:
                titleText.setText("SQL Console");
                sqlEditorPanel.setVisibility(View.VISIBLE);
                break;
            case VIEW_TABLES:
                titleText.setText("Database Tables");
                tablesPanel.setVisibility(View.VISIBLE);
                populateTablesList();
                break;
            case VIEW_TABLE_DATA:
                titleText.setText("Browse Data");
                tableDataPanel.setVisibility(View.VISIBLE);
                loadTableDataView(activeBrowsingTableName, "");
                break;
            case VIEW_TABLE_STRUCTURE:
                titleText.setText("Table Schema");
                tableStructurePanel.setVisibility(View.VISIBLE);
                loadTableStructureView(activeBrowsingTableName);
                break;
            case VIEW_HISTORY:
                titleText.setText("Query Logs");
                historyPanel.setVisibility(View.VISIBLE);
                populateHistoryList();
                break;
            case VIEW_SETTINGS:
                titleText.setText("Preferences");
                settingsPanel.setVisibility(View.VISIBLE);
                break;
        }
    }

    @Override
    public void onBackPressed() {
        if (!navigationStack.isEmpty()) {
            int prevView = navigationStack.remove(navigationStack.size() - 1);
            currentView = prevView;
            navigateTo(prevView);
            // remove duplicated stack registration triggered from navigationTo
            if (!navigationStack.isEmpty()) {
                navigationStack.remove(navigationStack.size() - 1);
            }
        } else {
            super.onBackPressed();
        }
    }

    private List<String> listDatabases() {
        List<String> dbs = new ArrayList<>();
        File dir = getDatabasePath("temp_db").getParentFile();
        if (dir != null && dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    String name = file.getName();
                    if (name.endsWith(".db") && !name.startsWith("_sql_manager_system")) {
                        dbs.add(name.substring(0, name.length() - 3));
                    }
                }
            }
        }
        if (dbs.isEmpty()) {
            dbs.add("default_db");
        }
        return dbs;
    }

    private List<String> listTables() {
        List<String> tables = new ArrayList<>();
        try {
            SQLiteDatabase db = getActiveDatabase();
            Cursor cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'android_metadata' AND name NOT LIKE 'sqlite_sequence'", null);
            if (cursor.moveToFirst()) {
                do {
                    tables.add(cursor.getString(0));
                } while (cursor.moveToNext());
            }
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tables;
    }

    private void updateDashboardStats() {
        try {
            List<String> dbs = listDatabases();
            statDbCount.setText(String.valueOf(dbs.size()));

            List<String> tables = listTables();
            statTableCount.setText(String.valueOf(tables.size()));

            long totalRows = 0;
            SQLiteDatabase db = getActiveDatabase();
            for (String table : tables) {
                Cursor c = db.rawQuery("SELECT COUNT(*) FROM [" + table + "]", null);
                if (c.moveToFirst()) {
                    totalRows += c.getLong(0);
                }
                c.close();
            }
            statRowCount.setText(String.valueOf(totalRows));

            List<Map<String, String>> logs = systemDb.getHistory();
            if (!logs.isEmpty()) {
                Map<String, String> lastLog = logs.get(0);
                dashLastQuery.setText(lastLog.get("query"));
                if ("SUCCESS".equals(lastLog.get("status"))) {
                    dashLastQuery.setTextColor(Color.parseColor("#2E7D32"));
                } else {
                    dashLastQuery.setTextColor(Color.parseColor("#C62828"));
                }
            } else {
                dashLastQuery.setText("No query log exists yet.");
                dashLastQuery.setTextColor(Color.GRAY);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void populateDatabasesList() {
        databasesListContainer.removeAllViews();
        List<String> dbs = listDatabases();
        for (final String dbName : dbs) {
            View itemView = getLayoutInflater().inflate(android.R.layout.simple_list_item_2, null);
            TextView text1 = itemView.findViewById(android.R.id.text1);
            TextView text2 = itemView.findViewById(android.R.id.text2);

            text1.setText(dbName);
            text1.setTextColor(Color.BLACK);
            text1.setTextSize(17);
            text1.setTypeface(null, Typeface.BOLD);

            boolean isActive = dbName.equals(activeDbName);
            text2.setText(isActive ? "Active Connection" : "Offline Storage");
            text2.setTextColor(isActive ? Color.parseColor("#2E7D32") : Color.GRAY);

            itemView.setPadding(24, 20, 24, 20);
            itemView.setBackgroundResource(R.drawable.card_bg);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, 12);
            itemView.setLayoutParams(params);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switchDatabase(dbName);
                    populateDatabasesList();
                    Toast.makeText(MainActivity.this, "Database Switched to: " + dbName, Toast.LENGTH_SHORT).show();
                }
            });

            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    showDatabaseOptionsDialog(dbName);
                    return true;
                }
            });

            databasesListContainer.addView(itemView);
        }
    }

    private void showDatabaseOptionsDialog(final String dbName) {
        String[] options = {"Rename Database", "Delete Database"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Database Options: " + dbName);
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    showRenameDatabaseDialog(dbName);
                } else if (which == 1) {
                    if ("default_db".equals(dbName)) {
                        Toast.makeText(MainActivity.this, "Cannot delete default_db connection file.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    showConfirmationDialog("Delete Database", "Are you sure you want to permanently delete " + dbName + "?", new Runnable() {
                        @Override
                        public void run() {
                            deleteDatabaseFile(dbName);
                        }
                    });
                }
            }
        });
        builder.show();
    }

    private void showCreateDatabaseDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create Database File");
        final EditText input = new EditText(this);
        input.setHint("Database Name");
        input.setSingleLine();
        LinearLayout layout = new LinearLayout(this);
        layout.setPadding(40, 20, 40, 20);
        layout.addView(input, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        builder.setView(layout);

        builder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = input.getText().toString().trim().replaceAll("[^a-zA-Z0-9_]", "");
                if (name.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Invalid database name input", Toast.LENGTH_SHORT).show();
                    return;
                }
                switchDatabase(name);
                populateDatabasesList();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showRenameDatabaseDialog(final String oldName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Rename Database");
        final EditText input = new EditText(this);
        input.setText(oldName);
        input.setSingleLine();
        LinearLayout layout = new LinearLayout(this);
        layout.setPadding(40, 20, 40, 20);
        layout.addView(input, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        builder.setView(layout);

        builder.setPositiveButton("Rename", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newName = input.getText().toString().trim().replaceAll("[^a-zA-Z0-9_]", "");
                if (newName.isEmpty() || newName.equals(oldName)) {
                    return;
                }
                try {
                    if (oldName.equals(activeDbName)) {
                        activeDatabase.close();
                        activeDatabase = null;
                    }
                    File oldFile = getDatabasePath(oldName + ".db");
                    File newFile = getDatabasePath(newName + ".db");
                    if (oldFile.renameTo(newFile)) {
                        switchDatabase(newName);
                        populateDatabasesList();
                        Toast.makeText(MainActivity.this, "Database renamed.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "Rename process failed.", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void deleteDatabaseFile(String dbName) {
        try {
            if (dbName.equals(activeDbName)) {
                switchDatabase("default_db");
            }
            deleteDatabase(dbName + ".db");
            populateDatabasesList();
            Toast.makeText(this, "Database deleted.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed deleting: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void populateTablesList() {
        tablesListContainer.removeAllViews();
        List<String> tables = listTables();
        if (tables.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText("No tables created yet. Go to SQL Console to execute table schemas.");
            tv.setTextColor(Color.GRAY);
            tv.setPadding(16, 16, 16, 16);
            tv.setGravity(Gravity.CENTER);
            tablesListContainer.addView(tv);
            return;
        }

        for (final String tableName : tables) {
            View itemView = getLayoutInflater().inflate(android.R.layout.simple_list_item_2, null);
            TextView text1 = itemView.findViewById(android.R.id.text1);
            TextView text2 = itemView.findViewById(android.R.id.text2);

            text1.setText(tableName);
            text1.setTextColor(Color.BLACK);
            text1.setTextSize(17);
            text1.setTypeface(null, Typeface.BOLD);

            long rowCount = 0;
            long colCount = 0;
            try {
                SQLiteDatabase db = getActiveDatabase();
                Cursor countCursor = db.rawQuery("SELECT COUNT(*) FROM [" + tableName + "]", null);
                if (countCursor.moveToFirst()) {
                    rowCount = countCursor.getLong(0);
                }
                countCursor.close();

                Cursor colCursor = db.rawQuery("PRAGMA table_info([" + tableName + "])", null);
                colCount = colCursor.getCount();
                colCursor.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            text2.setText(colCount + " columns | " + rowCount + " records");
            text2.setTextColor(Color.GRAY);

            itemView.setPadding(24, 20, 24, 20);
            itemView.setBackgroundResource(R.drawable.card_bg);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, 12);
            itemView.setLayoutParams(params);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    activeBrowsingTableName = tableName;
                    navigateTo(VIEW_TABLE_DATA);
                }
            });

            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    showTableOptionsDialog(tableName);
                    return true;
                }
            });

            tablesListContainer.addView(itemView);
        }
    }

    private void showTableOptionsDialog(final String tableName) {
        String[] options = {"Browse Row Records", "View Schema Structure", "Drop Table Schema"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Table Actions: " + tableName);
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    activeBrowsingTableName = tableName;
                    navigateTo(VIEW_TABLE_DATA);
                } else if (which == 1) {
                    activeBrowsingTableName = tableName;
                    navigateTo(VIEW_TABLE_STRUCTURE);
                } else if (which == 2) {
                    showConfirmationDialog("Drop Table " + tableName, "Warning: This will permanently delete the table. Do you wish to continue?", new Runnable() {
                        @Override
                        public void run() {
                            executeDirectSql("DROP TABLE [" + tableName + "];");
                            populateTablesList();
                        }
                    });
                }
            }
        });
        builder.show();
    }

    private void executeDirectSql(String sql) {
        try {
            SQLiteDatabase db = getActiveDatabase();
            db.execSQL(sql);
            Toast.makeText(this, "Success: Executed Command", Toast.LENGTH_SHORT).show();
            systemDb.addHistory(sql, getCurrentTimestamp(), "SUCCESS", 0, activeDbName);
            updateDashboardStats();
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            systemDb.addHistory(sql, getCurrentTimestamp(), "FAILED", 0, activeDbName);
        }
    }

    private void checkAndExecuteSql(final String sqlInput) {
        List<String> statements = SqlParser.splitStatements(sqlInput);
        boolean showDropWarning = false;
        boolean showDeleteAllWarning = false;

        for (String stmt : statements) {
            String clean = stmt.trim().toUpperCase();
            if (clean.contains("DROP TABLE") || clean.contains("DROP INDEX")) {
                showDropWarning = true;
            }
            if (clean.contains("DELETE FROM") && !clean.contains("WHERE")) {
                showDeleteAllWarning = true;
            }
        }

        if (showDropWarning) {
            showConfirmationDialog("Warning: Dangerous Command",
                    "Warning: This will permanently delete database structures (DROP TABLE/INDEX). Continue?",
                    new Runnable() {
                        @Override
                        public void run() {
                            runSql(sqlInput);
                        }
                    });
        } else if (showDeleteAllWarning) {
            showConfirmationDialog("Warning: Destructive Command",
                    "Warning: This query will delete all rows inside the target table without a WHERE clause. Continue?",
                    new Runnable() {
                        @Override
                        public void run() {
                            runSql(sqlInput);
                        }
                    });
        } else {
            runSql(sqlInput);
        }
    }

    private void runSql(String sqlInput) {
        if (sqlInput == null || sqlInput.trim().isEmpty()) {
            showExecutionError("No SQL query entered.");
            return;
        }

        List<String> statements = SqlParser.splitStatements(sqlInput);
        if (statements.isEmpty()) {
            showExecutionError("No valid SQL statement identified.");
            return;
        }

        SQLiteDatabase db;
        try {
            db = getActiveDatabase();
        } catch (Exception e) {
            showExecutionError("Failed to open database connection: " + e.getMessage());
            return;
        }

        long totalStartTime = System.currentTimeMillis();
        String lastQuery = "";
        int statementsExecuted = 0;
        int totalAffectedRows = 0;
        boolean isSelect = false;
        String[] selectColumns = null;
        List<String[]> selectRows = null;
        String errorMessage = null;

        db.beginTransaction();
        try {
            for (String statement : statements) {
                lastQuery = statement;
                String type = SqlParser.getCommandType(statement);

                if (type.equals("SELECT") || type.equals("PRAGMA")) {
                    isSelect = true;
                    Cursor cursor = db.rawQuery(statement, null);
                    selectColumns = cursor.getColumnNames();
                    selectRows = new ArrayList<>();
                    while (cursor.moveToNext()) {
                        String[] row = new String[selectColumns.length];
                        for (int i = 0; i < selectColumns.length; i++) {
                            int colType = cursor.getType(i);
                            if (colType == Cursor.FIELD_TYPE_NULL) {
                                row[i] = null;
                            } else {
                                row[i] = cursor.getString(i);
                            }
                        }
                        selectRows.add(row);
                    }
                    cursor.close();
                    statementsExecuted++;
                } else {
                    isSelect = false;
                    db.execSQL(statement);

                    Cursor c = db.rawQuery("SELECT changes()", null);
                    if (c.moveToFirst()) {
                        totalAffectedRows += c.getInt(0);
                    }
                    c.close();
                    statementsExecuted++;
                }
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            errorMessage = e.getMessage();
        } finally {
            db.endTransaction();
        }

        long totalDuration = System.currentTimeMillis() - totalStartTime;
        String status = (errorMessage == null) ? "SUCCESS" : "FAILED";
        systemDb.addHistory(sqlInput, getCurrentTimestamp(), status, totalDuration, activeDbName);

        if (errorMessage != null) {
            showExecutionError("SQL Error near: \"" + lastQuery + "\"\n\nDetails: " + errorMessage);
        } else {
            if (isSelect) {
                showSelectSuccess(selectRows, selectColumns, totalDuration);
            } else {
                showWriteSuccess(statementsExecuted, totalAffectedRows, totalDuration);
            }
            updateDashboardStats();
        }
    }

    private void clearEditorResults() {
        resultsTableLayout.removeAllViews();
        editorStatusBadge.setVisibility(View.GONE);
        resultsHeaderView.setVisibility(View.GONE);
        editSearchResults.setVisibility(View.GONE);
        cachedResultRows = null;
        cachedResultColumns = null;
    }

    private void showExecutionError(String errorMsg) {
        clearEditorResults();
        editorStatusBadge.setBackgroundResource(R.drawable.error_badge);
        editorStatusBadge.setText(errorMsg);
        editorStatusBadge.setTextColor(Color.parseColor("#C62828"));
        editorStatusBadge.setVisibility(View.VISIBLE);
    }

    private void showWriteSuccess(int statementsCount, int affectedRows, long durationMs) {
        clearEditorResults();
        editorStatusBadge.setBackgroundResource(R.drawable.success_badge);
        String msg = statementsCount + " statement(s) executed successfully.\nAffected rows: " + affectedRows + " | Execution time: " + durationMs + "ms";
        editorStatusBadge.setText(msg);
        editorStatusBadge.setTextColor(Color.parseColor("#2E7D32"));
        editorStatusBadge.setVisibility(View.VISIBLE);
    }

    private void showSelectSuccess(List<String[]> rows, String[] columns, long durationMs) {
        clearEditorResults();
        editorStatusBadge.setBackgroundResource(R.drawable.success_badge);
        String msg = rows.size() + " record(s) returned | Execution time: " + durationMs + "ms";
        editorStatusBadge.setText(msg);
        editorStatusBadge.setTextColor(Color.parseColor("#2E7D32"));
        editorStatusBadge.setVisibility(View.VISIBLE);

        cachedResultRows = rows;
        cachedResultColumns = columns;

        resultsHeaderView.setVisibility(View.VISIBLE);
        editSearchResults.setVisibility(View.VISIBLE);

        displayQueryResults(rows, columns, resultsTableLayout, true);
    }

    private void displayQueryResults(List<String[]> rows, String[] columns, TableLayout targetTable, boolean enableCellClick) {
        targetTable.removeAllViews();
        if (columns == null || columns.length == 0) return;

        // Header Row
        TableRow headerRow = new TableRow(this);
        headerRow.setBackgroundColor(Color.parseColor("#EAEDED"));
        headerRow.setPadding(0, 4, 0, 4);

        TextView thNum = new TextView(this);
        thNum.setText("#");
        thNum.setTypeface(null, Typeface.BOLD);
        thNum.setPadding(12, 10, 12, 10);
        thNum.setTextColor(Color.BLACK);
        headerRow.addView(thNum);

        for (String col : columns) {
            TextView th = new TextView(this);
            th.setText(col);
            th.setTypeface(null, Typeface.BOLD);
            th.setPadding(24, 10, 24, 10);
            th.setTextColor(Color.BLACK);
            headerRow.addView(th);
        }
        targetTable.addView(headerRow);

        // Data Rows
        int rowIdx = 1;
        for (final String[] row : rows) {
            TableRow tableRow = new TableRow(this);
            tableRow.setBackgroundColor(rowIdx % 2 == 0 ? Color.parseColor("#FAFAFA") : Color.WHITE);
            tableRow.setPadding(0, 4, 0, 4);

            TextView tdNum = new TextView(this);
            tdNum.setText(String.valueOf(rowIdx));
            tdNum.setPadding(12, 10, 12, 10);
            tdNum.setTextColor(Color.GRAY);
            tableRow.addView(tdNum);

            for (int i = 0; i < row.length; i++) {
                final String cellValue = row[i];
                TextView td = new TextView(this);
                td.setText(cellValue == null ? "NULL" : cellValue);
                if (cellValue == null) {
                    td.setTextColor(Color.RED);
                    td.setTypeface(null, Typeface.ITALIC);
                } else {
                    td.setTextColor(Color.BLACK);
                }
                td.setPadding(24, 10, 24, 10);

                if (enableCellClick) {
                    td.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            copyToClipboard(cellValue == null ? "" : cellValue);
                            Toast.makeText(MainActivity.this, "Copied: " + cellValue, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                tableRow.addView(td);
            }
            targetTable.addView(tableRow);
            rowIdx++;
        }
    }

    private void filterTableResults(String filterText, TableLayout targetTable, String[] columns, List<String[]> rows) {
        if (rows == null || columns == null) return;
        List<String[]> filtered = new ArrayList<>();
        String query = filterText.toLowerCase();

        for (String[] row : rows) {
            boolean matches = false;
            for (String val : row) {
                if (val != null && val.toLowerCase().contains(query)) {
                    matches = true;
                    break;
                }
            }
            if (matches) {
                filtered.add(row);
            }
        }
        displayQueryResults(filtered, columns, targetTable, true);
    }

    private void loadTableDataView(final String tableName, String query) {
        tableDataTitle.setText("Table Browse: " + tableName);
        tableRecordsLayout.removeAllViews();

        SQLiteDatabase db = getActiveDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT * FROM [" + tableName + "]", null);
            final String[] cols = cursor.getColumnNames();
            final List<String[]> dataRows = new ArrayList<>();

            // Find Primary Key column to allow Edit / Delete row operations
            Cursor pkCursor = db.rawQuery("PRAGMA table_info([" + tableName + "])", null);
            String primaryKeyName = null;
            if (pkCursor.moveToFirst()) {
                do {
                    int pkIndex = pkCursor.getInt(pkCursor.getColumnIndexOrThrow("pk"));
                    if (pkIndex > 0) {
                        primaryKeyName = pkCursor.getString(pkCursor.getColumnIndexOrThrow("name"));
                        break;
                    }
                } while (pkCursor.moveToNext());
            }
            pkCursor.close();

            // Default to first column if no explicit primary key was specified
            final String finalPkCol = (primaryKeyName != null) ? primaryKeyName : (cols.length > 0 ? cols[0] : null);

            while (cursor.moveToNext()) {
                String[] row = new String[cols.length];
                for (int i = 0; i < cols.length; i++) {
                    row[i] = cursor.getString(i);
                }
                dataRows.add(row);
            }
            cursor.close();

            // Display
            List<String[]> displayRows = dataRows;
            if (!query.trim().isEmpty()) {
                displayRows = new ArrayList<>();
                for (String[] r : dataRows) {
                    boolean match = false;
                    for (String cell : r) {
                        if (cell != null && cell.toLowerCase().contains(query.toLowerCase())) {
                            match = true;
                            break;
                        }
                    }
                    if (match) {
                        displayRows.add(r);
                    }
                }
            }

            // Build dynamic TableLayout
            tableRecordsLayout.removeAllViews();
            TableRow header = new TableRow(this);
            header.setBackgroundColor(Color.parseColor("#EAEDED"));

            TextView thNum = new TextView(this);
            thNum.setText("#");
            thNum.setTypeface(null, Typeface.BOLD);
            thNum.setPadding(12, 10, 12, 10);
            header.addView(thNum);

            for (String col : cols) {
                TextView th = new TextView(this);
                th.setText(col);
                th.setTypeface(null, Typeface.BOLD);
                th.setPadding(24, 10, 24, 10);
                header.addView(th);
            }
            tableRecordsLayout.addView(header);

            int rIdx = 1;
            for (final String[] r : displayRows) {
                final TableRow tr = new TableRow(this);
                tr.setBackgroundColor(rIdx % 2 == 0 ? Color.parseColor("#FAFAFA") : Color.WHITE);
                tr.setPadding(0, 4, 0, 4);

                TextView tdNum = new TextView(this);
                tdNum.setText(String.valueOf(rIdx));
                tdNum.setPadding(12, 10, 12, 10);
                tdNum.setTextColor(Color.GRAY);
                tr.addView(tdNum);

                // Fetch PK Value of this row
                int pkCellIndex = -1;
                for (int colIndex = 0; colIndex < cols.length; colIndex++) {
                    if (cols[colIndex].equals(finalPkCol)) {
                        pkCellIndex = colIndex;
                        break;
                    }
                }
                final String finalPkVal = (pkCellIndex != -1) ? r[pkCellIndex] : null;

                for (int i = 0; i < r.length; i++) {
                    final String cellValue = r[i];
                    TextView td = new TextView(this);
                    td.setText(cellValue == null ? "NULL" : cellValue);
                    if (cellValue == null) {
                        td.setTextColor(Color.RED);
                        td.setTypeface(null, Typeface.ITALIC);
                    } else {
                        td.setTextColor(Color.BLACK);
                    }
                    td.setPadding(24, 10, 24, 10);

                    td.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            copyToClipboard(cellValue == null ? "" : cellValue);
                            Toast.makeText(MainActivity.this, "Copied cell: " + cellValue, Toast.LENGTH_SHORT).show();
                        }
                    });

                    tr.addView(td);
                }

                tr.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        if (finalPkCol != null && finalPkVal != null) {
                            showRecordActionDialog(tableName, finalPkCol, finalPkVal, cols, r);
                        }
                        return true;
                    }
                });

                tableRecordsLayout.addView(tr);
                rIdx++;
            }

        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showRecordActionDialog(final String tableName, final String pkCol, final String pkVal, final String[] cols, final String[] rowData) {
        String[] options = {"Edit Record Row", "Delete Record Row"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Row Actions (PK: " + pkVal + ")");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    showEditRowDialog(tableName, pkCol, pkVal, cols, rowData);
                } else if (which == 1) {
                    showConfirmationDialog("Delete Row", "Delete this record row where " + pkCol + " = " + pkVal + "?", new Runnable() {
                        @Override
                        public void run() {
                            String sql = "DELETE FROM [" + tableName + "] WHERE [" + pkCol + "] = '" + pkVal + "';";
                            executeDirectSql(sql);
                            loadTableDataView(tableName, "");
                        }
                    });
                }
            }
        });
        builder.show();
    }

    private void showAddRowDialog(final String tableName) {
        try {
            SQLiteDatabase db = getActiveDatabase();
            Cursor c = db.rawQuery("PRAGMA table_info([" + tableName + "])", null);
            final List<String> colsList = new ArrayList<>();
            final List<String> typesList = new ArrayList<>();

            while (c.moveToNext()) {
                int isPk = c.getInt(c.getColumnIndexOrThrow("pk"));
                // Allow entering PK manually if it's not autoincrement integer
                String name = c.getString(c.getColumnIndexOrThrow("name"));
                String type = c.getString(c.getColumnIndexOrThrow("type"));
                
                colsList.add(name);
                typesList.add(type);
            }
            c.close();

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Insert Table Row");

            ScrollView scroll = new ScrollView(this);
            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(40, 20, 40, 20);

            final List<EditText> inputFields = new ArrayList<>();
            for (int i = 0; i < colsList.size(); i++) {
                TextView tv = new TextView(this);
                tv.setText(colsList.get(i) + " (" + typesList.get(i) + ")");
                tv.setTextSize(12);
                tv.setTextColor(Color.GRAY);
                layout.addView(tv);

                EditText et = new EditText(this);
                et.setSingleLine();
                layout.addView(et);
                inputFields.add(et);
            }
            scroll.addView(layout);
            builder.setView(scroll);

            builder.setPositiveButton("Insert", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    StringBuilder sbCol = new StringBuilder();
                    StringBuilder sbVal = new StringBuilder();

                    boolean first = true;
                    for (int i = 0; i < colsList.size(); i++) {
                        String val = inputFields.get(i).getText().toString().trim();
                        if (val.isEmpty()) continue; // Skip blank values

                        if (!first) {
                            sbCol.append(", ");
                            sbVal.append(", ");
                        }
                        sbCol.append("[").append(colsList.get(i)).append("]");
                        sbVal.append("'").append(val.replace("'", "''")).append("'");
                        first = false;
                    }

                    String sql = "INSERT INTO [" + tableName + "] (" + sbCol.toString() + ") VALUES (" + sbVal.toString() + ");";
                    executeDirectSql(sql);
                    loadTableDataView(tableName, "");
                }
            });
            builder.setNegativeButton("Cancel", null);
            builder.show();

        } catch (Exception e) {
            Toast.makeText(this, "Failed to load columns schema: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showEditRowDialog(final String tableName, final String pkCol, final String pkVal, final String[] cols, final String[] rowData) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Table Row");

        ScrollView scroll = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        final List<EditText> editFields = new ArrayList<>();
        for (int i = 0; i < cols.length; i++) {
            TextView tv = new TextView(this);
            tv.setText(cols[i]);
            tv.setTextSize(12);
            tv.setTextColor(Color.GRAY);
            layout.addView(tv);

            EditText et = new EditText(this);
            et.setText(rowData[i] == null ? "" : rowData[i]);
            et.setSingleLine();
            // Disable changing PK value
            if (cols[i].equals(pkCol)) {
                et.setEnabled(false);
            }
            layout.addView(et);
            editFields.add(et);
        }
        scroll.addView(layout);
        builder.setView(scroll);

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                StringBuilder sbSet = new StringBuilder();
                boolean first = true;

                for (int i = 0; i < cols.length; i++) {
                    if (cols[i].equals(pkCol)) continue; // Don't include PK in update values

                    String val = editFields.get(i).getText().toString().trim();
                    if (!first) {
                        sbSet.append(", ");
                    }
                    sbSet.append("[").append(cols[i]).append("] = '").append(val.replace("'", "''")).append("'");
                    first = false;
                }

                String sql = "UPDATE [" + tableName + "] SET " + sbSet.toString() + " WHERE [" + pkCol + "] = '" + pkVal + "';";
                executeDirectSql(sql);
                loadTableDataView(tableName, "");
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void loadTableStructureView(String tableName) {
        tableStructureTitle.setText("Structure: " + tableName);
        tableSchemaLayout.removeAllViews();

        try {
            SQLiteDatabase db = getActiveDatabase();
            Cursor cursor = db.rawQuery("PRAGMA table_info([" + tableName + "])", null);

            // Columns Header
            TableRow header = new TableRow(this);
            header.setBackgroundColor(Color.parseColor("#EAEDED"));
            header.setPadding(0, 4, 0, 4);

            String[] titles = {"Name", "Type", "Not Null", "Default Value", "Primary Key"};
            for (String t : titles) {
                TextView tv = new TextView(this);
                tv.setText(t);
                tv.setTypeface(null, Typeface.BOLD);
                tv.setPadding(24, 10, 24, 10);
                header.addView(tv);
            }
            tableSchemaLayout.addView(header);

            int rIdx = 1;
            while (cursor.moveToNext()) {
                TableRow row = new TableRow(this);
                row.setBackgroundColor(rIdx % 2 == 0 ? Color.parseColor("#FAFAFA") : Color.WHITE);
                row.setPadding(0, 4, 0, 4);

                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                String type = cursor.getString(cursor.getColumnIndexOrThrow("type"));
                int notNull = cursor.getInt(cursor.getColumnIndexOrThrow("notnull"));
                String dfltVal = cursor.getString(cursor.getColumnIndexOrThrow("dflt_value"));
                int pk = cursor.getInt(cursor.getColumnIndexOrThrow("pk"));

                String[] vals = {
                        name,
                        type,
                        (notNull == 1) ? "YES" : "NO",
                        (dfltVal != null) ? dfltVal : "NULL",
                        (pk == 1) ? "YES" : "NO"
                };

                for (String val : vals) {
                    TextView tv = new TextView(this);
                    tv.setText(val);
                    tv.setPadding(24, 10, 24, 10);
                    tv.setTextColor(Color.BLACK);
                    row.addView(tv);
                }
                tableSchemaLayout.addView(row);
                rIdx++;
            }
            cursor.close();

        } catch (Exception e) {
            Toast.makeText(this, "Failed structure retrieval: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void populateHistoryList() {
        historyListContainer.removeAllViews();
        List<Map<String, String>> logs = systemDb.getHistory();

        if (logs.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText("No executed queries logged.");
            tv.setTextColor(Color.GRAY);
            tv.setGravity(Gravity.CENTER);
            tv.setPadding(16, 24, 16, 24);
            historyListContainer.addView(tv);
            return;
        }

        for (final Map<String, String> log : logs) {
            View v = getLayoutInflater().inflate(android.R.layout.simple_list_item_2, null);
            TextView text1 = v.findViewById(android.R.id.text1);
            TextView text2 = v.findViewById(android.R.id.text2);

            final String sqlText = log.get("query");
            text1.setText(sqlText);
            text1.setTypeface(Typeface.MONOSPACE);
            text1.setTextColor(Color.BLACK);
            text1.setTextSize(13);

            String status = log.get("status");
            String duration = log.get("exec_time");
            String time = log.get("timestamp");
            String db = log.get("db_name");

            text2.setText(time + " | " + status + " (" + duration + "ms) | DB: " + db);
            text2.setTextColor("SUCCESS".equals(status) ? Color.parseColor("#2E7D32") : Color.parseColor("#C62828"));
            text2.setTextSize(11);

            v.setPadding(20, 16, 20, 16);
            v.setBackgroundResource(R.drawable.card_bg);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, 12);
            v.setLayoutParams(params);

            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sqlEditText.setText(sqlText);
                    navigateTo(VIEW_SQL_EDITOR);
                    Toast.makeText(MainActivity.this, "Copied statement to Editor", Toast.LENGTH_SHORT).show();
                }
            });

            v.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    showHistoryOptionsDialog(log);
                    return true;
                }
            });

            historyListContainer.addView(v);
        }
    }

    private void showHistoryOptionsDialog(final Map<String, String> log) {
        String[] options = {"Copy Statement Text", "Delete History Record"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("History Option");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    copyToClipboard(log.get("query"));
                    Toast.makeText(MainActivity.this, "SQL copied.", Toast.LENGTH_SHORT).show();
                } else if (which == 1) {
                    systemDb.deleteHistoryItem(Integer.parseInt(log.get("id")));
                    populateHistoryList();
                }
            }
        });
        builder.show();
    }

    private void resetAllDatabases() {
        try {
            if (activeDatabase != null) {
                activeDatabase.close();
                activeDatabase = null;
            }
            List<String> dbs = listDatabases();
            for (String db : dbs) {
                deleteDatabase(db + ".db");
            }
            systemDb.clearHistory();
            switchDatabase("default_db");
            navigateTo(VIEW_DASHBOARD);
            Toast.makeText(this, "Application metadata reset successful.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Reset failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void exportResultsToCSV() {
        if (cachedResultRows == null || cachedResultColumns == null) {
            Toast.makeText(this, "No query execution result to export.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            File exportDir = new File(getExternalFilesDir(null), "Exports");
            if (!exportDir.exists()) {
                exportDir.mkdirs();
            }
            String filename = "QueryExport_" + System.currentTimeMillis() + ".csv";
            File file = new File(exportDir, filename);
            FileWriter writer = new FileWriter(file);

            // Column Header Write
            for (int i = 0; i < cachedResultColumns.length; i++) {
                writer.append(escapeCSV(cachedResultColumns[i]));
                if (i < cachedResultColumns.length - 1) writer.append(",");
            }
            writer.append("\n");

            // Row values Write
            for (String[] row : cachedResultRows) {
                for (int i = 0; i < row.length; i++) {
                    writer.append(escapeCSV(row[i]));
                    if (i < row.length - 1) writer.append(",");
                }
                writer.append("\n");
            }

            writer.flush();
            writer.close();

            Toast.makeText(this, "CSV file saved successfully:\n" + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String escapeCSV(String value) {
        if (value == null) return "NULL";
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\n") || escaped.contains("\"")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    // Copy to system clipboard utility
    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("SQL_DATA", text);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
        }
    }

    // Global dialog alert launcher
    private void showConfirmationDialog(String title, String message, final Runnable confirmAction) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton("Proceed", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                confirmAction.run();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private String getCurrentTimestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
    }
}