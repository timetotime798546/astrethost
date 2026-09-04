package com.expensetrackerpro.app;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {

    private DatabaseHelper dbHelper;
    private List<Transaction> transactionList;
    private TransactionAdapter transactionAdapter;

    // Loader overlay layout elements
    private View loaderOverlay;
    private TextView txtLoaderPercentage;
    private int loaderProgress = 0;
    private Handler loaderHandler;

    // Touch layout Pull-to-Refresh elements
    private LinearLayout layoutRefreshHeader;
    private TextView txtRefreshStatus;
    private ProgressBar prgRefresh;
    private ListView lstTransactions;
    private float initialTouchY = 0;
    private boolean isDraggingHeader = false;
    private boolean isRefreshing = false;
    private static final int REFRESH_THRESHOLD = 150; // drag pixels threshold

    // Programmatic Beep Sound Generator
    private ToneGenerator toneGen;

    // Content container panels
    private View layoutHome;
    private View layoutAdd;
    private View layoutReports;

    // Bottom Navigation Elements
    private View tabHomeTrigger;
    private View tabAddTrigger;
    private View tabReportsTrigger;
    private TextView txtTabHome;
    private TextView txtTabAdd;
    private TextView txtTabReports;

    // Top metrics Dashboard TextView references
    private TextView txtNetBalance;
    private TextView txtIncomeSummary;
    private TextView txtExpenseSummary;

    // Form selection variables
    private Button btnTypeExpense;
    private Button btnTypeIncome;
    private EditText edtAmount;
    private Spinner spnCategory;
    private Button btnSelectDate;
    private EditText edtNote;
    private Button btnSaveTransaction;

    private String selectedType = "EXPENSE"; // Default
    private Calendar selectedCalendar;
    private String[] categories = {"Food", "Rent", "Salary", "Entertainment", "Utilities", "Transport", "Freelance", "Other"};
    private Map<String, Integer> categoryColors;

    // Reports layout elements
    private ChartView chartView;
    private LinearLayout layoutBreakdownList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        transactionList = new ArrayList<>();
        selectedCalendar = Calendar.getInstance();

        // Instantiate standard high fidelity acoustic Tone Generator
        try {
            toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 95);
        } catch (Exception e) {
            e.printStackTrace();
        }

        initCategoryColors();
        initViews();
        setupNavigationListeners();
        setupFormControllers();
        setupPullToRefresh();
        
        // Initial setup data synchronization
        loadTransactionsData();
        switchToTab("HOME");

        // Run the dynamic App-Open Loader Progress Simulation
        runAppOpenLoader();
    }

    private void initCategoryColors() {
        categoryColors = new HashMap<>();
        categoryColors.put("Food", Color.parseColor("#EF4444")); // red
        categoryColors.put("Rent", Color.parseColor("#3B82F6")); // blue
        categoryColors.put("Salary", Color.parseColor("#10B981")); // green
        categoryColors.put("Entertainment", Color.parseColor("#8B5CF6")); // purple
        categoryColors.put("Utilities", Color.parseColor("#F59E0B")); // amber
        categoryColors.put("Transport", Color.parseColor("#06B6D4")); // cyan
        categoryColors.put("Freelance", Color.parseColor("#6366F1")); // indigo
        categoryColors.put("Other", Color.parseColor("#64748B")); // slate
    }

    private void initViews() {
        // App Open loader view
        loaderOverlay = findViewById(R.id.loaderOverlay);
        txtLoaderPercentage = (TextView) findViewById(R.id.txtLoaderPercentage);

        // Pull to refresh headers
        layoutRefreshHeader = (LinearLayout) findViewById(R.id.layoutRefreshHeader);
        txtRefreshStatus = (TextView) findViewById(R.id.txtRefreshStatus);
        prgRefresh = (ProgressBar) findViewById(R.id.prgRefresh);

        // Tab screens panels
        layoutHome = findViewById(R.id.layoutHome);
        layoutAdd = findViewById(R.id.layoutAdd);
        layoutReports = findViewById(R.id.layoutReports);

        // Tab item trigger blocks
        tabHomeTrigger = findViewById(R.id.tabHomeTrigger);
        tabAddTrigger = findViewById(R.id.tabAddTrigger);
        tabReportsTrigger = findViewById(R.id.tabReportsTrigger);
        txtTabHome = (TextView) findViewById(R.id.txtTabHome);
        txtTabAdd = (TextView) findViewById(R.id.txtTabAdd);
        txtTabReports = (TextView) findViewById(R.id.txtTabReports);

        // Header analytics labels
        txtNetBalance = (TextView) findViewById(R.id.txtNetBalance);
        txtIncomeSummary = (TextView) findViewById(R.id.txtIncomeSummary);
        txtExpenseSummary = (TextView) findViewById(R.id.txtExpenseSummary);

        // Input Form attributes
        btnTypeExpense = (Button) findViewById(R.id.btnTypeExpense);
        btnTypeIncome = (Button) findViewById(R.id.btnTypeIncome);
        edtAmount = (EditText) findViewById(R.id.edtAmount);
        spnCategory = (Spinner) findViewById(R.id.spnCategory);
        btnSelectDate = (Button) findViewById(R.id.btnSelectDate);
        edtNote = (EditText) findViewById(R.id.edtNote);
        btnSaveTransaction = (Button) findViewById(R.id.btnSaveTransaction);

        // Report components
        chartView = (ChartView) findViewById(R.id.chartView);
        layoutBreakdownList = (LinearLayout) findViewById(R.id.layoutBreakdownList);

        // Setup Main Listing Adapter
        lstTransactions = (ListView) findViewById(R.id.lstTransactions);
        transactionAdapter = new TransactionAdapter(this, transactionList);
        lstTransactions.setAdapter(transactionAdapter);

        TextView btnDeleteAll = (TextView) findViewById(R.id.btnDeleteAll);
        btnDeleteAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                db.delete(DatabaseHelper.TABLE_TRANSACTIONS, null, null);
                db.close();
                Toast.makeText(MainActivity.this, "Local database flushed", Toast.LENGTH_SHORT).show();
                loadTransactionsData();
                playSystemTone(ToneGenerator.TONE_PROP_ACK);
            }
        });
    }

    // Programmatic play tones helper
    private void playSystemTone(int toneType) {
        if (toneGen != null) {
            try {
                toneGen.startTone(toneType, 130);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void runAppOpenLoader() {
        loaderProgress = 0;
        loaderOverlay.setVisibility(View.VISIBLE);
        loaderHandler = new Handler();
        Runnable progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (loaderProgress <= 100) {
                    txtLoaderPercentage.setText("Initializing core DB... " + loaderProgress + "%");
                    if (loaderProgress == 20 || loaderProgress == 60 || loaderProgress == 90) {
                        playSystemTone(ToneGenerator.TONE_PROP_BEEP);
                    }
                    loaderProgress += 5;
                    loaderHandler.postDelayed(this, 50);
                } else {
                    // Fade out loader overlay beautifully
                    playSystemTone(ToneGenerator.TONE_PROP_ACK);
                    AlphaAnimation fadeOut = new AlphaAnimation(1.0f, 0.0f);
                    fadeOut.setDuration(350);
                    fadeOut.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {}
                        @Override
                        public void onAnimationEnd(Animation animation) {
                            loaderOverlay.setVisibility(View.GONE);
                        }
                        @Override
                        public void onAnimationRepeat(Animation animation) {}
                    });
                    loaderOverlay.startAnimation(fadeOut);
                }
            }
        };
        loaderHandler.postDelayed(progressRunnable, 100);
    }

    private void setupPullToRefresh() {
        // Handle gestures on the ListView to enable pull-down-refresh manually
        lstTransactions.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (isRefreshing) return false;

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // Record coordinates if list view is at the top limit
                        if (isListAtTop()) {
                            initialTouchY = event.getY();
                            isDraggingHeader = true;
                        }
                        break;

                    case MotionEvent.ACTION_MOVE:
                        if (isDraggingHeader) {
                            float currentY = event.getY();
                            float diff = currentY - initialTouchY;

                            if (diff > 0 && isListAtTop()) {
                                // Dynamic stretching animation calculation
                                int targetHeight = (int) (diff / 2.2f);
                                if (targetHeight > 250) targetHeight = 250; // cap pull height

                                ViewGroup.LayoutParams params = layoutRefreshHeader.getLayoutParams();
                                params.height = targetHeight;
                                layoutRefreshHeader.setLayoutParams(params);

                                // Update pull feedback status texts
                                if (targetHeight >= REFRESH_THRESHOLD) {
                                    txtRefreshStatus.setText("Release to refresh records!");
                                    txtRefreshStatus.setTextColor(Color.parseColor("#10B981"));
                                } else {
                                    txtRefreshStatus.setText("Pull down to refresh finance ledger...");
                                    txtRefreshStatus.setTextColor(Color.parseColor("#475569"));
                                }
                                return true; // absorb event
                            }
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if (isDraggingHeader) {
                            isDraggingHeader = false;
                            float finalDiff = event.getY() - initialTouchY;
                            int finalHeight = (int) (finalDiff / 2.2f);

                            if (finalHeight >= REFRESH_THRESHOLD && isListAtTop()) {
                                triggerRefreshAction();
                            } else {
                                resetRefreshHeader();
                            }
                        }
                        break;
                }
                return false;
            }
        });
    }

    private boolean isListAtTop() {
        if (lstTransactions.getChildCount() == 0) return true;
        return lstTransactions.getFirstVisiblePosition() == 0 && lstTransactions.getChildAt(0).getTop() >= 0;
    }

    private void triggerRefreshAction() {
        isRefreshing = true;
        txtRefreshStatus.setText("Refreshing ledger records...");
        txtRefreshStatus.setTextColor(Color.parseColor("#1E3A8A"));
        
        // Play premium refresh initiated chime sounds
        playSystemTone(ToneGenerator.TONE_CDMA_PIP);

        // Keep header open during loading delay
        ViewGroup.LayoutParams params = layoutRefreshHeader.getLayoutParams();
        params.height = REFRESH_THRESHOLD;
        layoutRefreshHeader.setLayoutParams(params);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Perform complete local database sync
                loadTransactionsData();
                playSystemTone(ToneGenerator.TONE_PROP_ACK);
                Toast.makeText(MainActivity.this, "Ledger accounts up to date", Toast.LENGTH_SHORT).show();
                resetRefreshHeader();
                isRefreshing = false;
            }
        }, 1500);
    }

    private void resetRefreshHeader() {
        ViewGroup.LayoutParams params = layoutRefreshHeader.getLayoutParams();
        params.height = 0;
        layoutRefreshHeader.setLayoutParams(params);
    }

    private void setupNavigationListeners() {
        tabHomeTrigger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchToTab("HOME");
            }
        });

        tabAddTrigger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchToTab("ADD");
            }
        });

        tabReportsTrigger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchToTab("REPORTS");
            }
        });
    }

    private void switchToTab(String tabName) {
        // Toggle view container panels layout visibility
        layoutHome.setVisibility(View.GONE);
        layoutAdd.setVisibility(View.GONE);
        layoutReports.setVisibility(View.GONE);

        // Clear active highlighting labels colors (Premium slate design)
        txtTabHome.setTextColor(Color.parseColor("#94A3B8"));
        txtTabAdd.setTextColor(Color.parseColor("#94A3B8"));
        txtTabReports.setTextColor(Color.parseColor("#94A3B8"));

        if (tabName.equals("HOME")) {
            layoutHome.setVisibility(View.VISIBLE);
            txtTabHome.setTextColor(Color.parseColor("#0F172A"));
            loadTransactionsData();
        } else if (tabName.equals("ADD")) {
            layoutAdd.setVisibility(View.VISIBLE);
            txtTabAdd.setTextColor(Color.parseColor("#0F172A"));
            resetFormFields();
        } else if (tabName.equals("REPORTS")) {
            layoutReports.setVisibility(View.VISIBLE);
            txtTabReports.setTextColor(Color.parseColor("#0F172A"));
            generateReports();
        }
        playSystemTone(ToneGenerator.TONE_PROP_BEEP);
    }

    private void setupFormControllers() {
        // Form Transaction Type selection toggle switches (Custom 3D drawables toggles)
        btnTypeExpense.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedType = "EXPENSE";
                btnTypeExpense.setBackgroundResource(R.drawable.btn_bg_expense);
                btnTypeExpense.setTextColor(Color.WHITE);
                btnTypeIncome.setBackgroundResource(R.drawable.btn_bg_neutral);
                btnTypeIncome.setTextColor(Color.parseColor("#94A3B8"));
                playSystemTone(ToneGenerator.TONE_PROP_BEEP);
            }
        });

        btnTypeIncome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedType = "INCOME";
                btnTypeIncome.setBackgroundResource(R.drawable.btn_bg_income);
                btnTypeIncome.setTextColor(Color.WHITE);
                btnTypeExpense.setBackgroundResource(R.drawable.btn_bg_neutral);
                btnTypeExpense.setTextColor(Color.parseColor("#94A3B8"));
                playSystemTone(ToneGenerator.TONE_PROP_BEEP);
            }
        });

        // Initialize Category dropdown items Spinner layout adapter
        ArrayAdapter<String> spinAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        spinAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnCategory.setAdapter(spinAdapter);

        // Wire Date picker dialog action popup triggers
        updateDateButtonText();
        btnSelectDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playSystemTone(ToneGenerator.TONE_PROP_BEEP);
                new DatePickerDialog(MainActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        selectedCalendar.set(Calendar.YEAR, year);
                        selectedCalendar.set(Calendar.MONTH, month);
                        selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        updateDateButtonText();
                    }
                }, selectedCalendar.get(Calendar.YEAR), selectedCalendar.get(Calendar.MONTH), selectedCalendar.get(Calendar.DAY_OF_MONTH)).show();
            }
        });

        // Save entry click handler
        btnSaveTransaction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveTransaction();
            }
        });
    }

    private void updateDateButtonText() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        btnSelectDate.setText("Date Selected: " + sdf.format(selectedCalendar.getTime()));
    }

    private void resetFormFields() {
        edtAmount.setText("");
        edtNote.setText("");
        selectedCalendar = Calendar.getInstance();
        updateDateButtonText();
        spnCategory.setSelection(0);
        
        // Default select status back to Expense
        selectedType = "EXPENSE";
        btnTypeExpense.setBackgroundResource(R.drawable.btn_bg_expense);
        btnTypeExpense.setTextColor(Color.WHITE);
        btnTypeIncome.setBackgroundResource(R.drawable.btn_bg_neutral);
        btnTypeIncome.setTextColor(Color.parseColor("#94A3B8"));
    }

    private void saveTransaction() {
        String amountStr = edtAmount.getText().toString().trim();
        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Please insert valid transaction amount", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Number calculation format error", Toast.LENGTH_SHORT).show();
            return;
        }

        if (amount <= 0) {
            Toast.makeText(this, "Value must exceed 0", Toast.LENGTH_SHORT).show();
            return;
        }

        String category = spnCategory.getSelectedItem().toString();
        String noteStr = edtNote.getText().toString().trim();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String dateStr = sdf.format(selectedCalendar.getTime());

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_AMOUNT, amount);
        values.put(DatabaseHelper.COLUMN_TYPE, selectedType);
        values.put(DatabaseHelper.COLUMN_CATEGORY, category);
        values.put(DatabaseHelper.COLUMN_DATE, dateStr);
        values.put(DatabaseHelper.COLUMN_NOTE, noteStr);

        long resultRowId = db.insert(DatabaseHelper.TABLE_TRANSACTIONS, null, values);
        db.close();

        if (resultRowId != -1) {
            playSystemTone(ToneGenerator.TONE_PROP_ACK);
            Toast.makeText(this, "Transaction logged", Toast.LENGTH_SHORT).show();
            loadTransactionsData();
            switchToTab("HOME");
        } else {
            Toast.makeText(this, "Database storage failure", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadTransactionsData() {
        transactionList.clear();

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_TRANSACTIONS,
                null, null, null, null, null,
                DatabaseHelper.COLUMN_DATE + " DESC, " + DatabaseHelper.COLUMN_ID + " DESC"
        );

        double totalIncome = 0;
        double totalExpenses = 0;

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID));
                double amount = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_AMOUNT));
                String type = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TYPE));
                String category = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CATEGORY));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DATE));
                String note = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NOTE));

                Transaction tx = new Transaction(id, amount, type, category, date, note);
                transactionList.add(tx);

                if (type.equals("INCOME")) {
                    totalIncome += amount;
                } else {
                    totalExpenses += amount;
                }

            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();

        // Refresh adapter elements
        transactionAdapter.notifyDataSetChanged();

        // Format and render summaries
        txtIncomeSummary.setText("$" + String.format("%.2f", totalIncome));
        txtExpenseSummary.setText("$" + String.format("%.2f", totalExpenses));

        double balance = totalIncome - totalExpenses;
        txtNetBalance.setText((balance < 0 ? "-" : "") + "$" + String.format("%.2f", Math.abs(balance)));
        if (balance < 0) {
            txtNetBalance.setTextColor(Color.parseColor("#EF4444"));
        } else {
            txtNetBalance.setTextColor(Color.parseColor("#10B981"));
        }
    }

    private void generateReports() {
        layoutBreakdownList.removeAllViews();

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT " + DatabaseHelper.COLUMN_CATEGORY + ", SUM(" + DatabaseHelper.COLUMN_AMOUNT + ") AS total_val FROM " +
                        DatabaseHelper.TABLE_TRANSACTIONS +
                        " WHERE " + DatabaseHelper.COLUMN_TYPE + " = 'EXPENSE' " +
                        " GROUP BY " + DatabaseHelper.COLUMN_CATEGORY +
                        " ORDER BY total_val DESC", null
        );

        List<ChartView.ChartData> chartDataList = new ArrayList<>();

        if (cursor.moveToFirst()) {
            do {
                String category = cursor.getString(0);
                double sum = cursor.getDouble(1);

                int color = Color.GRAY;
                if (categoryColors.containsKey(category)) {
                    color = categoryColors.get(category);
                }

                chartDataList.add(new ChartView.ChartData(category, sum, color));

                // Generate 3D styled breakdown rows dynamically
                LinearLayout itemRow = new LinearLayout(this);
                itemRow.setOrientation(LinearLayout.HORIZONTAL);
                itemRow.setPadding(16, 16, 16, 16);
                itemRow.setBackgroundResource(R.drawable.card_bg);
                
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                rowParams.bottomMargin = 10;
                itemRow.setLayoutParams(rowParams);

                // Indicator
                View indicator = new View(this);
                LinearLayout.LayoutParams indicatorParams = new LinearLayout.LayoutParams(26, 26);
                indicatorParams.gravity = android.view.Gravity.CENTER_VERTICAL;
                indicatorParams.rightMargin = 22;
                indicator.setLayoutParams(indicatorParams);
                indicator.setBackgroundColor(color);
                itemRow.addView(indicator);

                // Title
                TextView labelView = new TextView(this);
                labelView.setText(category);
                labelView.setTextSize(14);
                labelView.setTextColor(Color.parseColor("#334155"));
                itemRow.addView(labelView, new LinearLayout.LayoutParams(0, -2, 1.0f));

                // Aggregate Valuation
                TextView valView = new TextView(this);
                valView.setText("$" + String.format("%.2f", sum));
                valView.setTextSize(14);
                valView.setTextColor(Color.parseColor("#0F172A"));
                valView.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                itemRow.addView(valView);

                layoutBreakdownList.addView(itemRow);

            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();

        chartView.setData(chartDataList);
    }

    // Inner Custom 3D transaction row adapter
    private class TransactionAdapter extends BaseAdapter {
        private List<Transaction> list;

        public TransactionAdapter(android.content.Context context, List<Transaction> list) {
            this.list = list;
        }

        @Override
        public int getCount() { return list.size(); }
        @Override
        public Object getItem(int position) { return list.get(position); }
        @Override
        public long getItemId(int position) { return list.get(position).getId(); }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                // Outer elevated container layout
                LinearLayout itemContainer = new LinearLayout(MainActivity.this);
                itemContainer.setOrientation(LinearLayout.VERTICAL);
                itemContainer.setPadding(6, 6, 6, 6);

                // Row elements layout surface using 3D card drawable
                LinearLayout rowLayout = new LinearLayout(MainActivity.this);
                rowLayout.setOrientation(LinearLayout.HORIZONTAL);
                rowLayout.setPadding(24, 20, 24, 20);
                rowLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);
                rowLayout.setBackgroundResource(R.drawable.card_bg);

                // Category Text
                TextView txtCategory = new TextView(MainActivity.this);
                txtCategory.setId(101);
                txtCategory.setTextSize(15);
                txtCategory.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                txtCategory.setTextColor(Color.parseColor("#1E293B"));
                LinearLayout.LayoutParams paramsCat = new LinearLayout.LayoutParams(0, -2, 1.1f);
                rowLayout.addView(txtCategory, paramsCat);

                // Note descriptions metadata block
                LinearLayout detailsLayout = new LinearLayout(MainActivity.this);
                detailsLayout.setOrientation(LinearLayout.VERTICAL);
                detailsLayout.setPadding(12, 0, 12, 0);

                TextView txtNote = new TextView(MainActivity.this);
                txtNote.setId(102);
                txtNote.setTextSize(12);
                txtNote.setTextColor(Color.parseColor("#475569"));
                detailsLayout.addView(txtNote);

                TextView txtDate = new TextView(MainActivity.this);
                txtDate.setId(103);
                txtDate.setTextSize(10);
                txtDate.setTextColor(Color.parseColor("#94A3B8"));
                detailsLayout.addView(txtDate);

                LinearLayout.LayoutParams paramsDetails = new LinearLayout.LayoutParams(0, -2, 1.4f);
                rowLayout.addView(detailsLayout, paramsDetails);

                // Final Numeric valuation display
                TextView txtAmount = new TextView(MainActivity.this);
                txtAmount.setId(104);
                txtAmount.setTextSize(15);
                txtAmount.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                txtAmount.setGravity(android.view.Gravity.RIGHT);
                LinearLayout.LayoutParams paramsAmount = new LinearLayout.LayoutParams(-2, -2);
                rowLayout.addView(txtAmount, paramsAmount);

                itemContainer.addView(rowLayout);
                convertView = itemContainer;
            }

            Transaction item = list.get(position);
            TextView cat = (TextView) convertView.findViewById(101);
            TextView note = (TextView) convertView.findViewById(102);
            TextView date = (TextView) convertView.findViewById(103);
            TextView amt = (TextView) convertView.findViewById(104);

            cat.setText(item.getCategory());
            note.setText(item.getNote() != null && !item.getNote().isEmpty() ? item.getNote() : "No details logged");
            date.setText(item.getDate());

            if (item.getType().equals("INCOME")) {
                amt.setText("+$" + String.format("%.2f", item.getAmount()));
                amt.setTextColor(Color.parseColor("#10B981"));
            } else {
                amt.setText("-$" + String.format("%.2f", item.getAmount()));
                amt.setTextColor(Color.parseColor("#EF4444"));
            }

            return convertView;
        }
    }
}