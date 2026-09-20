package com.cashsnap.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private ExpenseDbHelper dbHelper;

    // Navigation Panels
    private View panelHome;
    private View panelAdd;
    private View panelHistory;
    private View panelChart;

    // Bottom Navigation Elements
    private LinearLayout tabHome;
    private LinearLayout tabAdd;
    private LinearLayout tabHistory;
    private LinearLayout tabChart;

    private TextView labelHome;
    private TextView labelAdd;
    private TextView labelHistory;
    private TextView labelChart;

    // Dashboard widgets
    private TextView textSpentToday;
    private TextView textSpentWeek;
    private TextView textSpentMonth;
    private ListView listRecentExpenses;

    // Add transaction fields
    private EditText editAmount;
    private Spinner spinnerCategory;
    private EditText editDate;
    private Button btnPickDate;
    private EditText editNote;
    private Button btnSaveExpense;

    // History & Search elements
    private EditText editSearch;
    private Spinner spinnerFilterCategory;
    private TextView textHistoryCount;
    private ListView listHistoryExpenses;
    private Button btnExportCsv;

    // Dynamic Chart Component
    private PieChartView pieChartView;

    private ExpenseAdapter recentAdapter;
    private ExpenseAdapter historyAdapter;

    private final String[] categories = {"Food", "Travel", "Shopping", "Bills", "Other"};
    private final String[] filterCategories = {"All", "Food", "Travel", "Shopping", "Bills", "Other"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getResources().getIdentifier("activity_main", "layout", getPackageName()));

        dbHelper = new ExpenseDbHelper(this);

        initializeUI();
        setupNavigation();
        setupAddForm();
        setupHistoryFilters();
        loadDashboardData();
    }

    private void initializeUI() {
        panelHome = findViewById(getResources().getIdentifier("panel_home", "id", getPackageName()));
        panelAdd = findViewById(getResources().getIdentifier("panel_add", "id", getPackageName()));
        panelHistory = findViewById(getResources().getIdentifier("panel_history", "id", getPackageName()));
        panelChart = findViewById(getResources().getIdentifier("panel_chart", "id", getPackageName()));

        tabHome = findViewById(getResources().getIdentifier("tab_home", "id", getPackageName()));
        tabAdd = findViewById(getResources().getIdentifier("tab_add", "id", getPackageName()));
        tabHistory = findViewById(getResources().getIdentifier("tab_history", "id", getPackageName()));
        tabChart = findViewById(getResources().getIdentifier("tab_chart", "id", getPackageName()));

        labelHome = findViewById(getResources().getIdentifier("label_home", "id", getPackageName()));
        labelAdd = findViewById(getResources().getIdentifier("label_add", "id", getPackageName()));
        labelHistory = findViewById(getResources().getIdentifier("label_history", "id", getPackageName()));
        labelChart = findViewById(getResources().getIdentifier("label_chart", "id", getPackageName()));

        textSpentToday = findViewById(getResources().getIdentifier("text_spent_today", "id", getPackageName()));
        textSpentWeek = findViewById(getResources().getIdentifier("text_spent_week", "id", getPackageName()));
        textSpentMonth = findViewById(getResources().getIdentifier("text_spent_month", "id", getPackageName()));
        listRecentExpenses = findViewById(getResources().getIdentifier("list_recent_expenses", "id", getPackageName()));

        editAmount = findViewById(getResources().getIdentifier("edit_amount", "id", getPackageName()));
        spinnerCategory = findViewById(getResources().getIdentifier("spinner_category", "id", getPackageName()));
        editDate = findViewById(getResources().getIdentifier("edit_date", "id", getPackageName()));
        btnPickDate = findViewById(getResources().getIdentifier("btn_pick_date", "id", getPackageName()));
        editNote = findViewById(getResources().getIdentifier("edit_note", "id", getPackageName()));
        btnSaveExpense = findViewById(getResources().getIdentifier("btn_save_expense", "id", getPackageName()));

        editSearch = findViewById(getResources().getIdentifier("edit_search", "id", getPackageName()));
        spinnerFilterCategory = findViewById(getResources().getIdentifier("spinner_filter_category", "id", getPackageName()));
        textHistoryCount = findViewById(getResources().getIdentifier("text_history_count", "id", getPackageName()));
        listHistoryExpenses = findViewById(getResources().getIdentifier("list_history_expenses", "id", getPackageName()));
        btnExportCsv = findViewById(getResources().getIdentifier("btn_export_csv", "id", getPackageName()));

        pieChartView = findViewById(getResources().getIdentifier("pie_chart_view", "id", getPackageName()));

        // Establish adapters with default query sets
        recentAdapter = new ExpenseAdapter(dbHelper.getAllExpensesCursor());
        listRecentExpenses.setAdapter(recentAdapter);

        historyAdapter = new ExpenseAdapter(dbHelper.getAllExpensesCursor());
        listHistoryExpenses.setAdapter(historyAdapter);
    }

    private void setupNavigation() {
        tabHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(panelHome, tabHome, labelHome);
                loadDashboardData();
            }
        });

        tabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(panelAdd, tabAdd, labelAdd);
            }
        });

        tabHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(panelHistory, tabHistory, labelHistory);
                refreshHistory();
            }
        });

        tabChart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(panelChart, tabChart, labelChart);
                loadChartData();
            }
        });
    }

    private void switchTab(View visiblePanel, LinearLayout selectedTab, TextView selectedLabel) {
        panelHome.setVisibility(View.GONE);
        panelAdd.setVisibility(View.GONE);
        panelHistory.setVisibility(View.GONE);
        panelChart.setVisibility(View.GONE);

        visiblePanel.setVisibility(View.VISIBLE);

        // Reset backgrounds of all bottom bars
        tabHome.setBackgroundColor(0xFFFFFFFF);
        tabAdd.setBackgroundColor(0xFFFFFFFF);
        tabHistory.setBackgroundColor(0xFFFFFFFF);
        tabChart.setBackgroundColor(0xFFFFFFFF);

        labelHome.setTextColor(0xFF757575);
        labelAdd.setTextColor(0xFF757575);
        labelHistory.setTextColor(0xFF757575);
        labelChart.setTextColor(0xFF757575);

        labelHome.setTypeface(null, Typeface.NORMAL);
        labelAdd.setTypeface(null, Typeface.NORMAL);
        labelHistory.setTypeface(null, Typeface.NORMAL);
        labelChart.setTypeface(null, Typeface.NORMAL);

        selectedTab.setBackgroundColor(0xFFE8F5E9);
        selectedLabel.setTextColor(0xFF1B5E20);
        selectedLabel.setTypeface(null, Typeface.BOLD);
    }

    private void setupAddForm() {
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories);
        spinnerCategory.setAdapter(categoryAdapter);

        // Autofill with today's date
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        editDate.setText(sdf.format(new Date()));

        btnPickDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar c = Calendar.getInstance();
                int year = c.get(Calendar.YEAR);
                int month = c.get(Calendar.MONTH);
                int day = c.get(Calendar.DAY_OF_MONTH);

                DatePickerDialog datePickerDialog = new DatePickerDialog(MainActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        editDate.setText(String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth));
                    }
                }, year, month, day);
                datePickerDialog.show();
            }
        });

        btnSaveExpense.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitExpense();
            }
        });
    }

    private void submitExpense() {
        String amountStr = editAmount.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();
        String dateStr = editDate.getText().toString().trim();
        String note = editNote.getText().toString().trim();

        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Please insert an expense amount!", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid amount registered!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (dateStr.isEmpty() || !dateStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
            Toast.makeText(this, "Provide date in format YYYY-MM-DD", Toast.LENGTH_SHORT).show();
            return;
        }

        if (note.isEmpty()) {
            note = "Standard Expense";
        }

        boolean success = dbHelper.insertExpense(amount, category, dateStr, note);
        if (success) {
            Toast.makeText(this, "Expense tracked successfully!", Toast.LENGTH_SHORT).show();
            editAmount.setText("");
            editNote.setText("");
            // Clear focus & close keyboard representation
            editAmount.clearFocus();
            editNote.clearFocus();
            // Default back to dashboard
            switchTab(panelHome, tabHome, labelHome);
            loadDashboardData();
        } else {
            Toast.makeText(this, "Error tracking expense item.", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupHistoryFilters() {
        ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, filterCategories);
        spinnerFilterCategory.setAdapter(filterAdapter);

        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                refreshHistory();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        spinnerFilterCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                refreshHistory();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnExportCsv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportDataToCSV();
            }
        });
    }

    private void loadDashboardData() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Calendar calendar = Calendar.getInstance();

        // Today calculation
        String todayStr = sdf.format(calendar.getTime());
        double spentToday = dbHelper.getSpentToday(todayStr);

        // Week calculation (last 7 days window)
        String endStr = sdf.format(calendar.getTime());
        calendar.add(Calendar.DAY_OF_YEAR, -6);
        String startStr = sdf.format(calendar.getTime());
        double spentWeek = dbHelper.getSpentRange(startStr, endStr);

        // Month calculation
        SimpleDateFormat sdfMonth = new SimpleDateFormat("yyyy-MM", Locale.US);
        String monthPattern = sdfMonth.format(new Date());
        double spentMonth = dbHelper.getSpentMonth(monthPattern);

        textSpentToday.setText(String.format(Locale.US, "$%.2f", spentToday));
        textSpentWeek.setText(String.format(Locale.US, "$%.2f", spentWeek));
        textSpentMonth.setText(String.format(Locale.US, "$%.2f", spentMonth));

        recentAdapter.swapCursor(dbHelper.getAllExpensesCursor());
    }

    private void refreshHistory() {
        String query = editSearch.getText().toString().trim();
        String filterCategory = spinnerFilterCategory.getSelectedItem().toString();

        Cursor updatedCursor = dbHelper.getFilteredExpensesCursor(query, filterCategory);
        historyAdapter.swapCursor(updatedCursor);

        int count = updatedCursor.getCount();
        textHistoryCount.setText(String.format(Locale.US, "Search Results Found: %d items", count));
    }

    private void loadChartData() {
        List<PieChartView.Slice> slices = new ArrayList<>();
        Cursor cursor = dbHelper.getCategorySummary();

        while (cursor.moveToNext()) {
            String category = cursor.getString(0);
            double value = cursor.getDouble(1);

            int color = 0xFF888888;
            if ("Food".equals(category)) color = 0xFFE64A19;
            else if ("Travel".equals(category)) color = 0xFF1976D2;
            else if ("Shopping".equals(category)) color = 0xFFC2185B;
            else if ("Bills".equals(category)) color = 0xFF7B1FA2;
            else if ("Other".equals(category)) color = 0xFF388E3C;

            slices.add(new PieChartView.Slice(category, value, color));
        }
        cursor.close();
        pieChartView.setSlices(slices);
    }

    private void exportDataToCSV() {
        Cursor cursor = dbHelper.getAllExpensesCursor();
        if (cursor == null || cursor.getCount() == 0) {
            Toast.makeText(this, "Empty history: No expenses to export!", Toast.LENGTH_SHORT).show();
            if (cursor != null) cursor.close();
            return;
        }

        StringBuilder csvContent = new StringBuilder();
        csvContent.append("ID,Amount,Category,Date,Note\n");

        while (cursor.moveToNext()) {
            long id = cursor.getLong(cursor.getColumnIndexOrThrow(ExpenseDbHelper.COL_ID));
            double amount = cursor.getDouble(cursor.getColumnIndexOrThrow(ExpenseDbHelper.COL_AMOUNT));
            String category = cursor.getString(cursor.getColumnIndexOrThrow(ExpenseDbHelper.COL_CATEGORY));
            String date = cursor.getString(cursor.getColumnIndexOrThrow(ExpenseDbHelper.COL_DATE));
            String note = cursor.getString(cursor.getColumnIndexOrThrow(ExpenseDbHelper.COL_NOTE));

            // Clean CSV Injection characters
            if (note != null) {
                note = note.replace("\"", "\"\"");
                if (note.contains(",") || note.contains("\n") || note.contains("\"")) {
                    note = "\"" + note + "\"";
                }
            } else {
                note = "";
            }

            csvContent.append(id).append(",")
                    .append(amount).append(",")
                    .append(category).append(",")
                    .append(date).append(",")
                    .append(note).append("\n");
        }
        cursor.close();

        try {
            // Write to private cache location
            java.io.File file = new java.io.File(getCacheDir(), "CashSnap_Statement.csv");
            java.io.FileWriter writer = new java.io.FileWriter(file);
            writer.write(csvContent.toString());
            writer.close();

            // Place on clipboard for easy direct copy-paste standard fallback
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("CashSnap Export", csvContent.toString());
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "Copied statement data to Clipboard! Paste to any spreadsheet application.", Toast.LENGTH_LONG).show();
            }

            // Save to standard downloads folder if platform allows
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                android.content.ContentValues values = new android.content.ContentValues();
                values.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "CashSnap_Statement.csv");
                values.put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "text/csv");
                values.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS);

                android.net.Uri uri = getContentResolver().insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    java.io.OutputStream os = getContentResolver().openOutputStream(uri);
                    if (os != null) {
                        os.write(csvContent.toString().getBytes());
                        os.close();
                        Toast.makeText(this, "Statement generated successfully: Saved to Downloads directory!", Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                java.io.File publicDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
                java.io.File publicFile = new java.io.File(publicDir, "CashSnap_Statement.csv");
                java.io.FileWriter fileWriter = new java.io.FileWriter(publicFile);
                fileWriter.write(csvContent.toString());
                fileWriter.close();
                Toast.makeText(this, "Statement saved to Downloads/CashSnap_Statement.csv", Toast.LENGTH_LONG).show();
            }

        } catch (Exception e) {
            Toast.makeText(this, "Generation failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void requestDeleteConfirmation(final long id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Remove transaction?");
        builder.setMessage("Are you sure you want to delete this recorded transaction?");
        builder.setPositiveButton("Remove", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dbHelper.deleteExpense(id);
                loadDashboardData();
                refreshHistory();
                Toast.makeText(MainActivity.this, "Transaction deleted successfully.", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // Custom Transaction adapter layout engine to bypass inflation issues and optimize performance
    private class ExpenseAdapter extends BaseAdapter {
        private Cursor cursor;

        public ExpenseAdapter(Cursor cursor) {
            this.cursor = cursor;
        }

        public void swapCursor(Cursor newCursor) {
            if (cursor != null) {
                cursor.close();
            }
            cursor = newCursor;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return cursor != null ? cursor.getCount() : 0;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            if (cursor != null && cursor.moveToPosition(position)) {
                return cursor.getLong(cursor.getColumnIndexOrThrow(ExpenseDbHelper.COL_ID));
            }
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (!cursor.moveToPosition(position)) {
                return new View(MainActivity.this);
            }

            final long id = cursor.getLong(cursor.getColumnIndexOrThrow(ExpenseDbHelper.COL_ID));
            double amount = cursor.getDouble(cursor.getColumnIndexOrThrow(ExpenseDbHelper.COL_AMOUNT));
            String category = cursor.getString(cursor.getColumnIndexOrThrow(ExpenseDbHelper.COL_CATEGORY));
            String date = cursor.getString(cursor.getColumnIndexOrThrow(ExpenseDbHelper.COL_DATE));
            String note = cursor.getString(cursor.getColumnIndexOrThrow(ExpenseDbHelper.COL_NOTE));

            LinearLayout layout = new LinearLayout(MainActivity.this);
            layout.setOrientation(LinearLayout.HORIZONTAL);
            layout.setPadding(28, 22, 28, 22);
            layout.setGravity(Gravity.CENTER_VERTICAL);
            layout.setBackgroundColor(0xFFFFFFFF);

            // Icon Indicator representation
            TextView iconText = new TextView(MainActivity.this);
            iconText.setTextSize(24);
            iconText.setPadding(0, 0, 24, 0);

            String emoji = "🏷️";
            if ("Food".equals(category)) emoji = "🍔";
            else if ("Travel".equals(category)) emoji = "✈️";
            else if ("Shopping".equals(category)) emoji = "🛍️";
            else if ("Bills".equals(category)) emoji = "💵";
            iconText.setText(emoji);

            // Details section layout
            LinearLayout textDetails = new LinearLayout(MainActivity.this);
            textDetails.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams detailsParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            textDetails.setLayoutParams(detailsParams);

            TextView categoryHeader = new TextView(MainActivity.this);
            categoryHeader.setText(category + " - " + date);
            categoryHeader.setTextColor(0xFF212121);
            categoryHeader.setTextSize(15.0f);
            categoryHeader.setTypeface(null, Typeface.BOLD);

            TextView noteText = new TextView(MainActivity.this);
            noteText.setText(note);
            noteText.setTextColor(0xFF757575);
            noteText.setTextSize(13.0f);

            textDetails.addView(categoryHeader);
            textDetails.addView(noteText);

            // Price tag representation
            TextView amountText = new TextView(MainActivity.this);
            amountText.setText(String.format(Locale.US, "-$%.2f", amount));
            amountText.setTextColor(0xFFC62828);
            amountText.setTextSize(16.0f);
            amountText.setTypeface(null, Typeface.BOLD);
            amountText.setPadding(16, 0, 24, 0);

            // Deletion listener handle
            TextView deleteButton = new TextView(MainActivity.this);
            deleteButton.setText("🗑️");
            deleteButton.setTextSize(18.0f);
            deleteButton.setPadding(12, 12, 12, 12);
            deleteButton.setClickable(true);
            deleteButton.setFocusable(true);
            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    requestDeleteConfirmation(id);
                }
            });

            layout.addView(iconText);
            layout.addView(textDetails);
            layout.addView(amountText);
            layout.addView(deleteButton);

            return layout;
        }
    }

    @Override
    protected void onDestroy() {
        if (dbHelper != null) {
            dbHelper.close();
        }
        super.onDestroy();
    }
}