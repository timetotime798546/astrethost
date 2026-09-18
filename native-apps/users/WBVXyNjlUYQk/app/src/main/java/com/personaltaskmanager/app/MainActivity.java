package com.personaltaskmanager.app;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    // Screens Screen View Tracker IDs
    private static final int SCREEN_DASHBOARD_LIST = 1;
    private static final int SCREEN_ADD_EDIT = 2;
    private static final int SCREEN_DETAILS = 3;

    private int activeScreen = SCREEN_DASHBOARD_LIST;

    // SQLite Instance
    private DatabaseHelper databaseHelper;

    // Shared Date Formatter
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    // Navigation and Header elements
    private Button btnHeaderBack;
    private TextView textHeaderTitle;

    // View Panel layout frames
    private View layoutScreenDashboardList;
    private View layoutScreenAddEdit;
    private View layoutScreenDetails;

    // Screen 1 widgets (Dashboard List)
    private TextView statTotal;
    private TextView statToday;
    private TextView statPending;
    private TextView statCompleted;
    private TextView textEmptyState;
    private ListView listviewTasks;
    private EditText editSearch;
    private Button btnAddTaskFab;

    private Button filterBtnAll;
    private Button filterBtnPending;
    private Button filterBtnCompleted;
    private Button filterBtnHigh;

    private String currentFilterMode = "All"; // "All", "Pending", "Completed", "High Priority"
    private String currentSearchString = "";

    // Screen 2 widgets (Add/Edit task)
    private TextView textAddEditScreenTitle;
    private EditText editTaskTitle;
    private EditText editTaskDesc;
    private Spinner spinnerTaskPriority;
    private TextView textSelectedDate;
    private Button btnPickDate;
    private Button btnSaveTask;
    private Button btnCancelAddEdit;

    // State variables for adding or editing
    private boolean isEditingMode = false;
    private long editingTaskId = -1;
    private Calendar calendarHelper;

    // Screen 3 widgets (Task details view)
    private TextView detailPriorityBadge;
    private TextView detailTaskTitle;
    private TextView detailDueDate;
    private TextView detailStatusText;
    private TextView detailDescription;
    private Button btnDetailComplete;
    private Button btnDetailEdit;
    private Button btnDetailDelete;
    private Button btnDetailBack;

    private long currentViewingTaskId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Instantiating SQLite OpenHelper Class
        databaseHelper = new DatabaseHelper(this);
        calendarHelper = Calendar.getInstance();

        initLayoutReferences();
        setupSpinnerPriorities();
        setupClickListeners();
        setupSearchFiltering();

        // Start with default dashboard list state
        navigateToScreen(SCREEN_DASHBOARD_LIST);
    }

    private void initLayoutReferences() {
        // Core framework UI references
        btnHeaderBack = (Button) findViewById(R.id.btn_header_back);
        textHeaderTitle = (TextView) findViewById(R.id.text_header_title);

        layoutScreenDashboardList = findViewById(R.id.layout_screen_dashboard_list);
        layoutScreenAddEdit = findViewById(R.id.layout_screen_add_edit);
        layoutScreenDetails = findViewById(R.id.layout_screen_details);

        // Dashboard Stats UI
        statTotal = (TextView) findViewById(R.id.stat_total);
        statToday = (TextView) findViewById(R.id.stat_today);
        statPending = (TextView) findViewById(R.id.stat_pending);
        statCompleted = (TextView) findViewById(R.id.stat_completed);
        textEmptyState = (TextView) findViewById(R.id.text_empty_state);
        listviewTasks = (ListView) findViewById(R.id.listview_tasks);
        editSearch = (EditText) findViewById(R.id.edit_search);
        btnAddTaskFab = (Button) findViewById(R.id.btn_add_task_fab);

        // Filter button items
        filterBtnAll = (Button) findViewById(R.id.filter_btn_all);
        filterBtnPending = (Button) findViewById(R.id.filter_btn_pending);
        filterBtnCompleted = (Button) findViewById(R.id.filter_btn_completed);
        filterBtnHigh = (Button) findViewById(R.id.filter_btn_high);

        // Create Task Input Form UI references
        textAddEditScreenTitle = (TextView) findViewById(R.id.text_add_edit_screen_title);
        editTaskTitle = (EditText) findViewById(R.id.edit_task_title);
        editTaskDesc = (EditText) findViewById(R.id.edit_task_desc);
        spinnerTaskPriority = (Spinner) findViewById(R.id.spinner_task_priority);
        textSelectedDate = (TextView) findViewById(R.id.text_selected_date);
        btnPickDate = (Button) findViewById(R.id.btn_pick_date);
        btnSaveTask = (Button) findViewById(R.id.btn_save_task);
        btnCancelAddEdit = (Button) findViewById(R.id.btn_cancel_add_edit);

        // Display details Screen UI references
        detailPriorityBadge = (TextView) findViewById(R.id.detail_priority_badge);
        detailTaskTitle = (TextView) findViewById(R.id.detail_task_title);
        detailDueDate = (TextView) findViewById(R.id.detail_due_date);
        detailStatusText = (TextView) findViewById(R.id.detail_status_text);
        detailDescription = (TextView) findViewById(R.id.detail_description);
        btnDetailComplete = (Button) findViewById(R.id.btn_detail_complete);
        btnDetailEdit = (Button) findViewById(R.id.btn_detail_edit);
        btnDetailDelete = (Button) findViewById(R.id.btn_detail_delete);
        btnDetailBack = (Button) findViewById(R.id.btn_detail_back);
    }

    private void setupSpinnerPriorities() {
        String[] priorities = new String[]{"High", "Medium", "Low"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, priorities);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTaskPriority.setAdapter(adapter);
    }

    private void navigateToScreen(int screenId) {
        activeScreen = screenId;

        // Hide all screens
        layoutScreenDashboardList.setVisibility(View.GONE);
        layoutScreenAddEdit.setVisibility(View.GONE);
        layoutScreenDetails.setVisibility(View.GONE);

        // Reset Back button visibility by default
        btnHeaderBack.setVisibility(View.GONE);

        if (screenId == SCREEN_DASHBOARD_LIST) {
            layoutScreenDashboardList.setVisibility(View.VISIBLE);
            textHeaderTitle.setText("Personal Task Manager");
            refreshDashboardAndList();
        } else if (screenId == SCREEN_ADD_EDIT) {
            layoutScreenAddEdit.setVisibility(View.VISIBLE);
            btnHeaderBack.setVisibility(View.VISIBLE);
            if (isEditingMode) {
                textHeaderTitle.setText("Modify Existing Task");
                textAddEditScreenTitle.setText("Edit Task Details");
            } else {
                textHeaderTitle.setText("Create New Task");
                textAddEditScreenTitle.setText("Create New Task");
            }
        } else if (screenId == SCREEN_DETAILS) {
            layoutScreenDetails.setVisibility(View.VISIBLE);
            btnHeaderBack.setVisibility(View.VISIBLE);
            textHeaderTitle.setText("Task Information details");
            populateTaskDetails(currentViewingTaskId);
        }
    }

    private void refreshDashboardAndList() {
        // Fetch current date string formatted as YYYY-MM-DD
        String todayStr = dateFormat.format(new Date());

        // Update counts
        DatabaseHelper.Stats stats = databaseHelper.getStats(todayStr);
        statTotal.setText(String.valueOf(stats.total));
        statToday.setText(String.valueOf(stats.today));
        statPending.setText(String.valueOf(stats.pending));
        statCompleted.setText(String.valueOf(stats.completed));

        // Load tasks with filters
        final List<Task> taskList = databaseHelper.getFilteredTasks(currentSearchString, currentFilterMode);

        if (taskList.isEmpty()) {
            textEmptyState.setVisibility(View.VISIBLE);
            listviewTasks.setVisibility(View.GONE);
        } else {
            textEmptyState.setVisibility(View.GONE);
            listviewTasks.setVisibility(View.VISIBLE);

            // Bind using standard BaseAdapter to resolve layout/updates synchronization bugs
            listviewTasks.setAdapter(new BaseAdapter() {
                @Override
                public int getCount() {
                    return taskList.size();
                }

                @Override
                public Object getItem(int position) {
                    return taskList.get(position);
                }

                @Override
                public long getItemId(int position) {
                    return taskList.get(position).getId();
                }

                @Override
                public View getView(final int position, View convertView, ViewGroup parent) {
                    if (convertView == null) {
                        convertView = LayoutInflater.from(MainActivity.this).inflate(R.layout.item_task, parent, false);
                    }

                    final Task task = taskList.get(position);

                    TextView titleView = (TextView) convertView.findViewById(R.id.text_task_title);
                    TextView dueView = (TextView) convertView.findViewById(R.id.text_task_due);
                    TextView priorityBadge = (TextView) convertView.findViewById(R.id.text_task_priority_badge);
                    CheckBox checkBox = (CheckBox) convertView.findViewById(R.id.checkbox_status);

                    titleView.setText(task.getTitle());
                    dueView.setText("Due: " + task.getDueDate());

                    // Custom priority styling
                    String prio = task.getPriority();
                    priorityBadge.setText(prio.toUpperCase());
                    if ("High".equalsIgnoreCase(prio)) {
                        priorityBadge.setBackgroundResource(R.drawable.badge_high);
                        priorityBadge.setTextColor(getResources().getColor(R.color.danger_color));
                    } else if ("Medium".equalsIgnoreCase(prio)) {
                        priorityBadge.setBackgroundResource(R.drawable.badge_medium);
                        priorityBadge.setTextColor(getResources().getColor(R.color.warning_color));
                    } else {
                        priorityBadge.setBackgroundResource(R.drawable.badge_low);
                        priorityBadge.setTextColor(getResources().getColor(R.color.success_color));
                    }

                    // Strict checkbox binding and state handling
                    checkBox.setOnCheckedChangeListener(null);
                    checkBox.setChecked(task.isCompleted());
                    
                    if (task.isCompleted()) {
                        titleView.setPaintFlags(titleView.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                        titleView.setTextColor(getResources().getColor(R.color.text_secondary));
                    } else {
                        titleView.setPaintFlags(titleView.getPaintFlags() & (~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG));
                        titleView.setTextColor(getResources().getColor(R.color.text_primary));
                    }

                    checkBox.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            boolean isChecked = ((CheckBox) v).isChecked();
                            databaseHelper.setTaskCompletion(task.getId(), isChecked);
                            Toast.makeText(MainActivity.this, isChecked ? "Task completed!" : "Task pending", Toast.LENGTH_SHORT).show();
                            refreshDashboardAndList();
                        }
                    });

                    convertView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            currentViewingTaskId = task.getId();
                            navigateToScreen(SCREEN_DETAILS);
                        }
                    });

                    return convertView;
                }
            });
        }
    }

    private void setupClickListeners() {
        // Floating Action Button
        btnAddTaskFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isEditingMode = false;
                editingTaskId = -1;
                clearFormFields();
                navigateToScreen(SCREEN_ADD_EDIT);
            }
        });

        // Top bar back action
        btnHeaderBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        // Date picker action
        btnPickDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openDatePickerDialog();
            }
        });

        // Save task action
        btnSaveTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveTaskFormAction();
            }
        });

        // Cancel task action
        btnCancelAddEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToScreen(SCREEN_DASHBOARD_LIST);
            }
        });

        // Details screen interaction actions
        btnDetailBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToScreen(SCREEN_DASHBOARD_LIST);
            }
        });

        btnDetailDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentViewingTaskId != -1) {
                    databaseHelper.deleteTask(currentViewingTaskId);
                    Toast.makeText(MainActivity.this, "Task Deleted Successfully", Toast.LENGTH_SHORT).show();
                    navigateToScreen(SCREEN_DASHBOARD_LIST);
                }
            }
        });

        btnDetailEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentViewingTaskId != -1) {
                    isEditingMode = true;
                    editingTaskId = currentViewingTaskId;
                    loadTaskToForm(editingTaskId);
                    navigateToScreen(SCREEN_ADD_EDIT);
                }
            }
        });

        btnDetailComplete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentViewingTaskId != -1) {
                    Task task = databaseHelper.getTask(currentViewingTaskId);
                    if (task != null) {
                        boolean targetState = !task.isCompleted();
                        databaseHelper.setTaskCompletion(currentViewingTaskId, targetState);
                        Toast.makeText(MainActivity.this, targetState ? "Task marked completed!" : "Task marked pending", Toast.LENGTH_SHORT).show();
                        populateTaskDetails(currentViewingTaskId); // Refresh view details
                    }
                }
            }
        });
    }

    private void setupSearchFiltering() {
        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchString = s.toString();
                refreshDashboardAndList();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Setup filter navigation buttons
        filterBtnAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateFilterUI("All");
            }
        });

        filterBtnPending.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateFilterUI("Pending");
            }
        });

        filterBtnCompleted.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateFilterUI("Completed");
            }
        });

        filterBtnHigh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateFilterUI("High Priority");
            }
        });
    }

    private void updateFilterUI(String mode) {
        currentFilterMode = mode;

        // Reset styling for all buttons
        filterBtnAll.setBackgroundResource(R.drawable.button_secondary);
        filterBtnAll.setTextColor(getResources().getColor(R.color.text_primary));

        filterBtnPending.setBackgroundResource(R.drawable.button_secondary);
        filterBtnPending.setTextColor(getResources().getColor(R.color.text_primary));

        filterBtnCompleted.setBackgroundResource(R.drawable.button_secondary);
        filterBtnCompleted.setTextColor(getResources().getColor(R.color.text_primary));

        filterBtnHigh.setBackgroundResource(R.drawable.button_secondary);
        filterBtnHigh.setTextColor(getResources().getColor(R.color.text_primary));

        // Highlight selected button
        if ("All".equals(mode)) {
            filterBtnAll.setBackgroundResource(R.drawable.button_primary);
            filterBtnAll.setTextColor(getResources().getColor(R.color.white));
        } else if ("Pending".equals(mode)) {
            filterBtnPending.setBackgroundResource(R.drawable.button_primary);
            filterBtnPending.setTextColor(getResources().getColor(R.color.white));
        } else if ("Completed".equals(mode)) {
            filterBtnCompleted.setBackgroundResource(R.drawable.button_primary);
            filterBtnCompleted.setTextColor(getResources().getColor(R.color.white));
        } else if ("High Priority".equals(mode)) {
            filterBtnHigh.setBackgroundResource(R.drawable.button_primary);
            filterBtnHigh.setTextColor(getResources().getColor(R.color.white));
        }

        refreshDashboardAndList();
    }

    private void openDatePickerDialog() {
        DatePickerDialog dialog = new DatePickerDialog(
                this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        calendarHelper.set(Calendar.YEAR, year);
                        calendarHelper.set(Calendar.MONTH, month);
                        calendarHelper.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        textSelectedDate.setText(dateFormat.format(calendarHelper.getTime()));
                    }
                },
                calendarHelper.get(Calendar.YEAR),
                calendarHelper.get(Calendar.MONTH),
                calendarHelper.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }

    private void clearFormFields() {
        editTaskTitle.setText("");
        editTaskDesc.setText("");
        spinnerTaskPriority.setSelection(0);
        // Default to current calendar date
        calendarHelper = Calendar.getInstance();
        textSelectedDate.setText(dateFormat.format(calendarHelper.getTime()));
    }

    private void loadTaskToForm(long taskId) {
        Task task = databaseHelper.getTask(taskId);
        if (task != null) {
            editTaskTitle.setText(task.getTitle());
            editTaskDesc.setText(task.getDescription());
            
            // Set spinner priority selection
            String taskPrio = task.getPriority();
            if ("High".equalsIgnoreCase(taskPrio)) {
                spinnerTaskPriority.setSelection(0);
            } else if ("Medium".equalsIgnoreCase(taskPrio)) {
                spinnerTaskPriority.setSelection(1);
            } else {
                spinnerTaskPriority.setSelection(2);
            }

            textSelectedDate.setText(task.getDueDate());
            try {
                Date d = dateFormat.parse(task.getDueDate());
                if (d != null) {
                    calendarHelper.setTime(d);
                }
            } catch (Exception ignored) {}
        }
    }

    private void saveTaskFormAction() {
        String titleStr = editTaskTitle.getText().toString().trim();
        String descStr = editTaskDesc.getText().toString().trim();
        String priorityStr = spinnerTaskPriority.getSelectedItem().toString();
        String dateStr = textSelectedDate.getText().toString();

        if (titleStr.isEmpty()) {
            Toast.makeText(this, "Title cannot be empty!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isEditingMode) {
            Task original = databaseHelper.getTask(editingTaskId);
            boolean originalCompletedState = original != null && original.isCompleted();
            databaseHelper.updateTask(editingTaskId, titleStr, descStr, priorityStr, dateStr, originalCompletedState);
            Toast.makeText(this, "Task Updated Successfully", Toast.LENGTH_SHORT).show();
        } else {
            databaseHelper.insertTask(titleStr, descStr, priorityStr, dateStr);
            Toast.makeText(this, "Task Added Successfully", Toast.LENGTH_SHORT).show();
        }

        navigateToScreen(SCREEN_DASHBOARD_LIST);
    }

    private void populateTaskDetails(long taskId) {
        Task task = databaseHelper.getTask(taskId);
        if (task != null) {
            detailTaskTitle.setText(task.getTitle());
            detailDueDate.setText(task.getDueDate());
            detailDescription.setText(task.getDescription() == null || task.getDescription().trim().isEmpty() ? "No additional details provided." : task.getDescription());
            
            // Setup detail priority tag visual properties
            String prio = task.getPriority();
            detailPriorityBadge.setText(prio.toUpperCase() + " PRIORITY");
            if ("High".equalsIgnoreCase(prio)) {
                detailPriorityBadge.setBackgroundResource(R.drawable.badge_high);
                detailPriorityBadge.setTextColor(getResources().getColor(R.color.danger_color));
            } else if ("Medium".equalsIgnoreCase(prio)) {
                detailPriorityBadge.setBackgroundResource(R.drawable.badge_medium);
                detailPriorityBadge.setTextColor(getResources().getColor(R.color.warning_color));
            } else {
                detailPriorityBadge.setBackgroundResource(R.drawable.badge_low);
                detailPriorityBadge.setTextColor(getResources().getColor(R.color.success_color));
            }

            if (task.isCompleted()) {
                detailStatusText.setText("Completed");
                detailStatusText.setTextColor(getResources().getColor(R.color.success_color));
                btnDetailComplete.setText("Mark as Pending");
                btnDetailComplete.setBackgroundResource(R.drawable.button_secondary);
                btnDetailComplete.setTextColor(getResources().getColor(R.color.text_primary));
            } else {
                detailStatusText.setText("Pending");
                detailStatusText.setTextColor(getResources().getColor(R.color.danger_color));
                btnDetailComplete.setText("Mark as Completed");
                btnDetailComplete.setBackgroundResource(R.drawable.button_success);
                btnDetailComplete.setTextColor(getResources().getColor(R.color.white));
            }
        } else {
            Toast.makeText(this, "Error: Task could not be loaded", Toast.LENGTH_SHORT).show();
            navigateToScreen(SCREEN_DASHBOARD_LIST);
        }
    }

    @Override
    public void onBackPressed() {
        if (activeScreen == SCREEN_ADD_EDIT) {
            navigateToScreen(SCREEN_DASHBOARD_LIST);
        } else if (activeScreen == SCREEN_DETAILS) {
            navigateToScreen(SCREEN_DASHBOARD_LIST);
        } else {
            super.onBackPressed();
        }
    }
}