package com.todopro.app;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    // SQLite Constants
    private static final String DATABASE_NAME = "todopro.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_TASKS = "tasks";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_TITLE = "title";
    private static final String COLUMN_DESC = "description";
    private static final String COLUMN_PRIORITY = "priority";
    private static final String COLUMN_CATEGORY = "category";
    private static final String COLUMN_DUEDATE = "duedate";
    private static final String COLUMN_COMPLETED = "completed"; // 0 = false, 1 = true

    // UI Containers
    private View welcomeScreen;
    private View mainScreen;
    private ProgressBar welcomeProgress;
    private TextView welcomeStatus;

    // Dashboard Items
    private TextView txtStats;
    private EditText editSearch;
    private Button btnAddNew;
    private Button btnFilterAll;
    private Button btnFilterPending;
    private Button btnFilterCompleted;
    private LinearLayout taskListContainer;
    private TextView txtEmptyState;

    // Bottom Form Drawer
    private RelativeLayout addEditContainer;
    private TextView txtFormTitle;
    private EditText editTitle;
    private EditText editDesc;
    private EditText editCategory;
    private Spinner spinnerPriority;
    private EditText editDueDate;
    private Button btnCancel;
    private Button btnSaveTask;

    // App Variables
    private DatabaseHelper dbHelper;
    private String currentFilter = "ALL"; // "ALL", "PENDING", "COMPLETED"
    private String currentSearchQuery = "";
    private long editingTaskId = -1; // -1 represents a new task creation mode

    // Welcome Progress simulation
    private int progressStatus = 0;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        initializeViews();
        setupWelcomeSimulation();
    }

    private void initializeViews() {
        welcomeScreen = findViewById(R.id.welcomeScreen);
        mainScreen = findViewById(R.id.mainScreen);
        welcomeProgress = (ProgressBar) findViewById(R.id.welcomeProgress);
        welcomeStatus = (TextView) findViewById(R.id.welcomeStatus);

        txtStats = (TextView) findViewById(R.id.txtStats);
        editSearch = (EditText) findViewById(R.id.editSearch);
        btnAddNew = (Button) findViewById(R.id.btnAddNew);
        btnFilterAll = (Button) findViewById(R.id.btnFilterAll);
        btnFilterPending = (Button) findViewById(R.id.btnFilterPending);
        btnFilterCompleted = (Button) findViewById(R.id.btnFilterCompleted);
        taskListContainer = (LinearLayout) findViewById(R.id.taskListContainer);
        txtEmptyState = (TextView) findViewById(R.id.txtEmptyState);

        addEditContainer = (RelativeLayout) findViewById(R.id.addEditContainer);
        txtFormTitle = (TextView) findViewById(R.id.txtFormTitle);
        editTitle = (EditText) findViewById(R.id.editTitle);
        editDesc = (EditText) findViewById(R.id.editDesc);
        editCategory = (EditText) findViewById(R.id.editCategory);
        spinnerPriority = (Spinner) findViewById(R.id.spinnerPriority);
        editDueDate = (EditText) findViewById(R.id.editDueDate);
        btnCancel = (Button) findViewById(R.id.btnCancel);
        btnSaveTask = (Button) findViewById(R.id.btnSaveTask);

        // Setup Spinner Priorities
        String[] priorities = new String[]{"Low", "Medium", "High"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, priorities);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPriority.setAdapter(adapter);

        // Attach listeners
        setupListeners();
    }

    private void setupWelcomeSimulation() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (progressStatus < 100) {
                    progressStatus += 4;
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            welcomeProgress.setProgress(progressStatus);
                            if (progressStatus < 30) {
                                welcomeStatus.setText("Establishing connection...");
                            } else if (progressStatus < 65) {
                                welcomeStatus.setText("Accessing tasks database...");
                            } else if (progressStatus < 90) {
                                welcomeStatus.setText("Structuring clean UI views...");
                            } else {
                                welcomeStatus.setText("Starting application...");
                            }
                        }
                    });
                    try {
                        Thread.sleep(80);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        welcomeScreen.setVisibility(View.GONE);
                        mainScreen.setVisibility(View.VISIBLE);
                        loadTasks();
                    }
                });
            }
        }).start();
    }

    private void setupListeners() {
        // Add new Button
        btnAddNew.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openTaskForm(-1);
            }
        });

        // Cancel Form Button
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeTaskForm();
            }
        });

        // Save Form Task Button
        btnSaveTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveTaskDetails();
            }
        });

        // Filter Change Buttons
        btnFilterAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setFilterStyle("ALL");
            }
        });

        btnFilterPending.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setFilterStyle("PENDING");
            }
        });

        btnFilterCompleted.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setFilterStyle("COMPLETED");
            }
        });

        // Live Search Input Tracker
        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().trim();
                loadTasks();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setFilterStyle(String filter) {
        currentFilter = filter;
        btnFilterAll.setBackgroundColor(Color.parseColor("#E0E0E0"));
        btnFilterAll.setTextColor(Color.parseColor("#424242"));
        btnFilterPending.setBackgroundColor(Color.parseColor("#E0E0E0"));
        btnFilterPending.setTextColor(Color.parseColor("#424242"));
        btnFilterCompleted.setBackgroundColor(Color.parseColor("#E0E0E0"));
        btnFilterCompleted.setTextColor(Color.parseColor("#424242"));

        if (filter.equals("ALL")) {
            btnFilterAll.setBackgroundColor(Color.parseColor("#3F51B5"));
            btnFilterAll.setTextColor(Color.parseColor("#FFFFFF"));
        } else if (filter.equals("PENDING")) {
            btnFilterPending.setBackgroundColor(Color.parseColor("#3F51B5"));
            btnFilterPending.setTextColor(Color.parseColor("#FFFFFF"));
        } else if (filter.equals("COMPLETED")) {
            btnFilterCompleted.setBackgroundColor(Color.parseColor("#3F51B5"));
            btnFilterCompleted.setTextColor(Color.parseColor("#FFFFFF"));
        }
        loadTasks();
    }

    private void openTaskForm(long taskId) {
        editingTaskId = taskId;
        if (taskId == -1) {
            txtFormTitle.setText("Create New Task");
            editTitle.setText("");
            editDesc.setText("");
            editCategory.setText("");
            spinnerPriority.setSelection(0);
            editDueDate.setText("");
        } else {
            txtFormTitle.setText("Modify Existing Task");
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.query(TABLE_TASKS, null, COLUMN_ID + "=?", new String[]{String.valueOf(taskId)}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                editTitle.setText(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)));
                editDesc.setText(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESC)));
                editCategory.setText(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY)));
                editDueDate.setText(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DUEDATE)));
                
                String prio = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PRIORITY));
                if ("Low".equalsIgnoreCase(prio)) spinnerPriority.setSelection(0);
                else if ("Medium".equalsIgnoreCase(prio)) spinnerPriority.setSelection(1);
                else if ("High".equalsIgnoreCase(prio)) spinnerPriority.setSelection(2);

                cursor.close();
            }
        }
        addEditContainer.setVisibility(View.VISIBLE);
    }

    private void closeTaskForm() {
        addEditContainer.setVisibility(View.GONE);
        editingTaskId = -1;
    }

    private void saveTaskDetails() {
        String title = editTitle.getText().toString().trim();
        String desc = editDesc.getText().toString().trim();
        String category = editCategory.getText().toString().trim();
        String priority = spinnerPriority.getSelectedItem().toString();
        String dueDate = editDueDate.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(this, "Task Name can not be left blank!", Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, title);
        values.put(COLUMN_DESC, desc.isEmpty() ? "No additional description." : desc);
        values.put(COLUMN_CATEGORY, category.isEmpty() ? "General" : category);
        values.put(COLUMN_PRIORITY, priority);
        values.put(COLUMN_DUEDATE, dueDate.isEmpty() ? "No schedule date" : dueDate);

        if (editingTaskId == -1) {
            values.put(COLUMN_COMPLETED, 0);
            db.insert(TABLE_TASKS, null, values);
            Toast.makeText(this, "New task added successfully!", Toast.LENGTH_SHORT).show();
        } else {
            db.update(TABLE_TASKS, values, COLUMN_ID + "=?", new String[]{String.valueOf(editingTaskId)});
            Toast.makeText(this, "Task info updated!", Toast.LENGTH_SHORT).show();
        }
        
        closeTaskForm();
        loadTasks();
    }

    private void loadTasks() {
        // Clear previous dynamically created elements except the basic empty view placeholder
        taskListContainer.removeAllViews();
        taskListContainer.addView(txtEmptyState);

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        // Get dynamic tasks counter metrics
        int totalCount = 0;
        int completedCount = 0;
        Cursor countCursor = db.rawQuery("SELECT completed FROM " + TABLE_TASKS, null);
        if (countCursor != null) {
            totalCount = countCursor.getCount();
            while (countCursor.moveToNext()) {
                if (countCursor.getInt(0) == 1) {
                    completedCount++;
                }
            }
            countCursor.close();
        }
        txtStats.setText(completedCount + " of " + totalCount + " tasks completed");

        // Construct conditional SQL query clauses
        List<String> queryArgsList = new ArrayList<String>();
        String selection = "1=1";

        if (currentFilter.equals("PENDING")) {
            selection += " AND " + COLUMN_COMPLETED + " = 0";
        } else if (currentFilter.equals("COMPLETED")) {
            selection += " AND " + COLUMN_COMPLETED + " = 1";
        }

        if (!currentSearchQuery.isEmpty()) {
            selection += " AND (" + COLUMN_TITLE + " LIKE ? OR " + COLUMN_DESC + " LIKE ? OR " + COLUMN_CATEGORY + " LIKE ?)";
            queryArgsList.add("%" + currentSearchQuery + "%");
            queryArgsList.add("%" + currentSearchQuery + "%");
            queryArgsList.add("%" + currentSearchQuery + "%");
        }

        String[] selectionArgs = queryArgsList.toArray(new String[0]);
        Cursor cursor = db.query(
                TABLE_TASKS, 
                null, 
                selection, 
                selectionArgs, 
                null, 
                null, 
                COLUMN_ID + " DESC"
        );

        if (cursor != null && cursor.getCount() > 0) {
            txtEmptyState.setVisibility(View.GONE);
            while (cursor.moveToNext()) {
                final long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));
                final String title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE));
                final String desc = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESC));
                final String category = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY));
                final String priority = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PRIORITY));
                final String duedate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DUEDATE));
                final int isCompleted = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_COMPLETED));

                View taskCard = createTaskItemCard(id, title, desc, category, priority, duedate, isCompleted == 1);
                taskListContainer.addView(taskCard);
            }
            cursor.close();
        } else {
            txtEmptyState.setVisibility(View.VISIBLE);
        }
    }

    // Dynamically program and style material style task containers inside Java codes to achieve pure independence
    private View createTaskItemCard(final long id, final String title, String desc, String category, String priority, String duedate, final boolean isCompleted) {
        
        // Outermost card container layout
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, 12);
        card.setLayoutParams(cardParams);
        card.setBackgroundColor(Color.parseColor("#FFFFFF"));
        card.setPadding(16, 16, 16, 16);

        // Row layout containing completion checkbox, main titles details, priority badge and control triggers
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        topRow.setLayoutParams(rowParams);

        // Left Side Checkbox status
        CheckBox checkBox = new CheckBox(this);
        checkBox.setChecked(isCompleted);
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean checked) {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                ContentValues cv = new ContentValues();
                cv.put(COLUMN_COMPLETED, checked ? 1 : 0);
                db.update(TABLE_TASKS, cv, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
                loadTasks();
            }
        });
        topRow.addView(checkBox);

        // Core text layout section
        LinearLayout infoPanel = new LinearLayout(this);
        infoPanel.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        infoParams.setMargins(8, 0, 8, 0);
        infoPanel.setLayoutParams(infoParams);

        TextView txtTitle = new TextView(this);
        txtTitle.setText(title);
        txtTitle.setTextSize(16); // Modern sizing
        txtTitle.setTextColor(isCompleted ? Color.parseColor("#888888") : Color.parseColor("#212121"));
        txtTitle.setPaintFlags(isCompleted ? (txtTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG) : (txtTitle.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG));
        txtTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        infoPanel.addView(txtTitle);

        TextView txtDesc = new TextView(this);
        txtDesc.setText(desc);
        txtDesc.setTextSize(13);
        txtDesc.setTextColor(Color.parseColor("#616161"));
        txtDesc.setPadding(0, 4, 0, 0);
        infoPanel.addView(txtDesc);

        // Horizontal properties metadata container (Category & Due Date labels)
        LinearLayout metaPanel = new LinearLayout(this);
        metaPanel.setOrientation(LinearLayout.HORIZONTAL);
        metaPanel.setPadding(0, 8, 0, 0);

        TextView tagCategory = new TextView(this);
        tagCategory.setText(category.toUpperCase());
        tagCategory.setTextSize(10);
        tagCategory.setPadding(8, 2, 8, 2);
        tagCategory.setBackgroundColor(Color.parseColor("#E8EAF6"));
        tagCategory.setTextColor(Color.parseColor("#3F51B5"));
        LinearLayout.LayoutParams tagParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tagParams.setMargins(0, 0, 8, 0);
        tagCategory.setLayoutParams(tagParams);
        metaPanel.addView(tagCategory);

        TextView txtDate = new TextView(this);
        txtDate.setText("🗓 " + duedate);
        txtDate.setTextSize(11);
        txtDate.setTextColor(Color.parseColor("#757575"));
        metaPanel.addView(txtDate);

        infoPanel.addView(metaPanel);
        topRow.addView(infoPanel);

        // Right Side Priority indicator view
        TextView badgePriority = new TextView(this);
        badgePriority.setText(priority);
        badgePriority.setTextSize(10);
        badgePriority.setPadding(10, 4, 10, 4);
        badgePriority.setTextColor(Color.parseColor("#FFFFFF"));
        badgePriority.setTypeface(null, android.graphics.Typeface.BOLD);

        String priorityColor = "#4CAF50"; // Low default (Green)
        if ("Medium".equalsIgnoreCase(priority)) {
            priorityColor = "#FF9800"; // Yellow-Orange
        } else if ("High".equalsIgnoreCase(priority)) {
            priorityColor = "#F44336"; // Red
        }
        badgePriority.setBackgroundColor(Color.parseColor(priorityColor));
        LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        badgeParams.setMargins(4, 0, 8, 0);
        badgePriority.setLayoutParams(badgeParams);
        topRow.addView(badgePriority);

        // Dynamic Control Action Buttons
        LinearLayout controlBox = new LinearLayout(this);
        controlBox.setOrientation(LinearLayout.VERTICAL);

        Button btnEdit = new Button(this);
        btnEdit.setText("EDIT");
        btnEdit.setTextSize(9);
        btnEdit.setHeight(30);
        btnEdit.setPadding(4, 2, 4, 2);
        btnEdit.setBackgroundColor(Color.parseColor("#FFFFFF"));
        btnEdit.setTextColor(Color.parseColor("#2196F3"));
        btnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openTaskForm(id);
            }
        });
        controlBox.addView(btnEdit);

        Button btnDelete = new Button(this);
        btnDelete.setText("DELETE");
        btnDelete.setTextSize(9);
        btnDelete.setHeight(30);
        btnDelete.setPadding(4, 2, 4, 2);
        btnDelete.setBackgroundColor(Color.parseColor("#FFFFFF"));
        btnDelete.setTextColor(Color.parseColor("#F44336"));
        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                db.delete(TABLE_TASKS, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
                Toast.makeText(MainActivity.this, "Task permanently deleted!", Toast.LENGTH_SHORT).show();
                loadTasks();
            }
        });
        controlBox.addView(btnDelete);

        topRow.addView(controlBox);
        card.addView(topRow);

        return card;
    }

    private int spToPx(float sp) {
        return (int) (sp * getResources().getDisplayMetrics().scaledDensity);
    }

    // SQLite Database Adapter sub-class
    private static class DatabaseHelper extends SQLiteOpenHelper {
        
        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            String CREATE_SQL = "CREATE TABLE " + TABLE_TASKS + " ("
                    + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COLUMN_TITLE + " TEXT, "
                    + COLUMN_DESC + " TEXT, "
                    + COLUMN_CATEGORY + " TEXT, "
                    + COLUMN_PRIORITY + " TEXT, "
                    + COLUMN_DUEDATE + " TEXT, "
                    + COLUMN_COMPLETED + " INTEGER DEFAULT 0)";
            db.execSQL(CREATE_SQL);

            // Inject initial sample data elements dynamically
            injectSampleTask(db, "Explore Todo Pro app!", "Try testing priorities, edit fields, filter completed items and searching dynamically.", "Work", "High", "Today");
            injectSampleTask(db, "Complete workout session", "Include cardio and active full body stretching routines.", "Personal", "Medium", "Friday");
        }

        private void injectSampleTask(SQLiteDatabase db, String title, String desc, String category, String priority, String date) {
            ContentValues values = new ContentValues();
            values.put(COLUMN_TITLE, title);
            values.put(COLUMN_DESC, desc);
            values.put(COLUMN_CATEGORY, category);
            values.put(COLUMN_PRIORITY, priority);
            values.put(COLUMN_DUEDATE, date);
            values.put(COLUMN_COMPLETED, 0);
            db.insert(TABLE_TASKS, null, values);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_TASKS);
            onCreate(db);
        }
    }
}