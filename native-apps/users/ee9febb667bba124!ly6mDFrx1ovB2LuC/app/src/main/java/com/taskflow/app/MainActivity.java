package com.taskflow.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends Activity {

    private DatabaseHelper dbHelper;
    private ListView listViewTasks;
    private TaskAdapter taskAdapter;
    private TextView textSummary;
    private TextView textNoTasks;
    private ImageButton fabAddTask;

    private Button btnFilterAll;
    private Button btnFilterPending;
    private Button btnFilterCompleted;

    private String currentFilter = "ALL";

    @Override
    protected void onCreate(Bundle savedInstanceState) { 
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        listViewTasks = (ListView) findViewById(R.id.listViewTasks);
        textSummary = (TextView) findViewById(R.id.textSummary);
        textNoTasks = (TextView) findViewById(R.id.textNoTasks);
        fabAddTask = (ImageButton) findViewById(R.id.fabAddTask);

        btnFilterAll = (Button) findViewById(R.id.btnFilterAll);
        btnFilterPending = (Button) findViewById(R.id.btnFilterPending);
        btnFilterCompleted = (Button) findViewById(R.id.btnFilterCompleted);

        btnFilterAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentFilter = "ALL";
                updateFilterUI();
                loadTasks();
            }
        });

        btnFilterPending.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentFilter = "PENDING";
                updateFilterUI();
                loadTasks();
            }
        });

        btnFilterCompleted.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentFilter = "COMPLETED";
                updateFilterUI();
                loadTasks();
            }
        });

        fabAddTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddTaskDialog();
            }
        });

        List<Task> initialTasks = dbHelper.getAllTasks();
        taskAdapter = new TaskAdapter(this, initialTasks, new TaskAdapter.OnTaskStatusChangeListener() {
            @Override
            public void onTaskStatusChanged(Task task, boolean isCompleted) {
                task.setCompleted(isCompleted ? 1 : 0);
                dbHelper.updateTask(task);
                loadTasks();
                updateSummaryText();
            }

            @Override
            public void onTaskDeleted(Task task) {
                dbHelper.deleteTask(task.getId());
                loadTasks();
                updateSummaryText();
                Toast.makeText(MainActivity.this, "Task deleted", Toast.LENGTH_SHORT).show();
            }
        });
        listViewTasks.setAdapter(taskAdapter);

        loadTasks();
        updateFilterUI();
        updateSummaryText();
    }

    private void loadTasks() {
        List<Task> tasks;
        if ("PENDING".equals(currentFilter)) {
            tasks = dbHelper.getTasksByStatus(0);
        } else if ("COMPLETED".equals(currentFilter)) {
            tasks = dbHelper.getTasksByStatus(1);
        } else {
            tasks = dbHelper.getAllTasks();
        }

        taskAdapter.updateData(tasks);

        if (tasks.isEmpty()) {
            textNoTasks.setVisibility(View.VISIBLE);
        } else {
            textNoTasks.setVisibility(View.GONE);
        }
    }

    private void updateFilterUI() {
        btnFilterAll.setBackgroundColor(Color.parseColor("#EEEEEE"));
        btnFilterAll.setTextColor(Color.parseColor("#333333"));
        btnFilterPending.setBackgroundColor(Color.parseColor("#EEEEEE"));
        btnFilterPending.setTextColor(Color.parseColor("#333333"));
        btnFilterCompleted.setBackgroundColor(Color.parseColor("#EEEEEE"));
        btnFilterCompleted.setTextColor(Color.parseColor("#333333"));

        if ("ALL".equals(currentFilter)) {
            btnFilterAll.setBackgroundColor(Color.parseColor("#2196F3"));
            btnFilterAll.setTextColor(Color.parseColor("#FFFFFF"));
        } else if ("PENDING".equals(currentFilter)) {
            btnFilterPending.setBackgroundColor(Color.parseColor("#2196F3"));
            btnFilterPending.setTextColor(Color.parseColor("#FFFFFF"));
        } else if ("COMPLETED".equals(currentFilter)) {
            btnFilterCompleted.setBackgroundColor(Color.parseColor("#2196F3"));
            btnFilterCompleted.setTextColor(Color.parseColor("#FFFFFF"));
        }
    }

    private void updateSummaryText() {
        int total = dbHelper.getTaskCount();
        int completed = dbHelper.getCompletedTaskCount();
        textSummary.setText(completed + " of " + total + " tasks completed");
    }

    private void showAddTaskDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_task, null);
        builder.setView(dialogView);

        final EditText editTitle = (EditText) dialogView.findViewById(R.id.editTaskTitle);
        final EditText editDesc = (EditText) dialogView.findViewById(R.id.editTaskDesc);
        final Button btnDatePicker = (Button) dialogView.findViewById(R.id.btnDatePicker);
        final RadioGroup radioGroupPriority = (RadioGroup) dialogView.findViewById(R.id.radioGroupPriority);

        final Calendar calendar = Calendar.getInstance();

        btnDatePicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int year = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MONTH);
                int day = calendar.get(Calendar.DAY_OF_MONTH);

                DatePickerDialog datePickerDialog = new DatePickerDialog(MainActivity.this,
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int yearSelected, int monthOfYear, int dayOfMonth) {
                                String dateStr = dayOfMonth + "/" + (monthOfYear + 1) + "/" + yearSelected;
                                btnDatePicker.setText(dateStr);
                            }
                        }, year, month, day);
                datePickerDialog.show();
            }
        });

        builder.setTitle("Create Task")
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        final AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = editTitle.getText().toString().trim();
                String desc = editDesc.getText().toString().trim();
                String date = btnDatePicker.getText().toString().trim();
                if ("Select Due Date".equals(date)) {
                    date = "";
                }

                if (title.isEmpty()) {
                    editTitle.setError("Task title is required!");
                    return;
                }

                String priority = "LOW";
                int selectedRadioId = radioGroupPriority.getCheckedRadioButtonId();
                if (selectedRadioId == R.id.radioHigh) {
                    priority = "HIGH";
                } else if (selectedRadioId == R.id.radioMedium) {
                    priority = "MEDIUM";
                }

                Task newTask = new Task(title, desc, priority, 0, date);
                dbHelper.addTask(newTask);

                loadTasks();
                updateSummaryText();

                Toast.makeText(MainActivity.this, "Task created successfully", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            } 
        });
    }
}