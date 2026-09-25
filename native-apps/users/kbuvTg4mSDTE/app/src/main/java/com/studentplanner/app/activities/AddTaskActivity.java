package com.studentplanner.app.activities;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.studentplanner.app.R;
import com.studentplanner.app.data.DatabaseHelper;
import com.studentplanner.app.models.Task;
import com.studentplanner.app.notifications.NotificationHelper;

import java.util.Calendar;
import java.util.Locale;

public class AddTaskActivity extends Activity {

    private EditText taskTitleEditText;
    private EditText taskDescriptionEditText;
    private Button dueDateButton;
    private Button dueTimeButton;
    private Button saveTaskButton;
    private TextView addTaskTitle;

    private DatabaseHelper dbHelper;
    private int subjectId;
    private String subjectName;
    private int taskId = -1; // -1 for new task, otherwise editing existing

    private Calendar dueDateTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task);

        dbHelper = new DatabaseHelper(this);

        taskTitleEditText = findViewById(R.id.task_title_edit_text);
        taskDescriptionEditText = findViewById(R.id.task_description_edit_text);
        dueDateButton = findViewById(R.id.due_date_button);
        dueTimeButton = findViewById(R.id.due_time_button);
        saveTaskButton = findViewById(R.id.save_task_button);
        addTaskTitle = findViewById(R.id.add_task_title);

        dueDateTime = Calendar.getInstance();
        updateDateButtonText();
        updateTimeButtonText();

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            subjectId = extras.getInt("subject_id", -1);
            subjectName = extras.getString("subject_name", "Unknown Subject");

            if (extras.containsKey("task_id")) {
                taskId = extras.getInt("task_id");
                setTitle(R.string.edit_task); // Set activity title
                addTaskTitle.setText(R.string.edit_task); // Set layout title
                saveTaskButton.setText(R.string.edit_task);

                // Load existing task details
                Task existingTask = dbHelper.getTask(taskId);
                if (existingTask != null) {
                    taskTitleEditText.setText(existingTask.getTitle());
                    taskDescriptionEditText.setText(existingTask.getDescription());
                    dueDateTime.setTimeInMillis(existingTask.getDueDateMillis());
                    updateDateButtonText();
                    updateTimeButtonText();
                }
            } else {
                setTitle(R.string.add_task); // Set activity title
                addTaskTitle.setText(R.string.add_task); // Set layout title
                saveTaskButton.setText(R.string.save_task);
            }
        }

        if (subjectId == -1) {
            Toast.makeText(this, "Error: Subject not found for task.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        dueDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker();
            }
        });

        dueTimeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimePicker();
            }
        });

        saveTaskButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveTask();
            }
        });
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        dueDateTime.set(Calendar.YEAR, year);
                        dueDateTime.set(Calendar.MONTH, monthOfYear);
                        dueDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        updateDateButtonText();
                    }
                },
                dueDateTime.get(Calendar.YEAR),
                dueDateTime.get(Calendar.MONTH),
                dueDateTime.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void showTimePicker() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        dueDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        dueDateTime.set(Calendar.MINUTE, minute);
                        updateTimeButtonText();
                    }
                },
                dueDateTime.get(Calendar.HOUR_OF_DAY),
                dueDateTime.get(Calendar.MINUTE),
                DateFormat.is24HourFormat(this)); // Use system 24-hour format setting
        timePickerDialog.show();
    }

    private void updateDateButtonText() {
        String date = DateFormat.format("MMM dd, yyyy", dueDateTime).toString();
        dueDateButton.setText(date);
    }

    private void updateTimeButtonText() {
        String time = DateFormat.format("hh:mm a", dueDateTime).toString();
        dueTimeButton.setText(time);
    }

    private void saveTask() {
        String title = taskTitleEditText.getText().toString().trim();
        String description = taskDescriptionEditText.getText().toString().trim();
        long dueDateMillis = dueDateTime.getTimeInMillis();

        if (title.isEmpty()) {
            Toast.makeText(this, "Task title cannot be empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (dueDateMillis < System.currentTimeMillis() - (1000 * 60)) { // Allow a minute grace for past due
            Toast.makeText(this, "Due date/time cannot be in the past.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (taskId == -1) {
            // Add new task
            Task newTask = new Task(subjectId, title, description, dueDateMillis, 0); // 0 = not completed
            long id = dbHelper.addTask(newTask);
            if (id > 0) {
                newTask.setId((int) id); // Set the ID for scheduling reminder
                NotificationHelper.scheduleReminder(this, newTask);
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(this, "Failed to add task.", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Update existing task
            Task existingTask = dbHelper.getTask(taskId); // Get current completion status
            existingTask.setSubjectId(subjectId);
            existingTask.setTitle(title);
            existingTask.setDescription(description);
            existingTask.setDueDateMillis(dueDateMillis);

            int rowsAffected = dbHelper.updateTask(existingTask);
            if (rowsAffected > 0) {
                if (!existingTask.isCompleted()) { // Only reschedule if not completed
                    NotificationHelper.scheduleReminder(this, existingTask);
                }
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(this, "Failed to update task.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}