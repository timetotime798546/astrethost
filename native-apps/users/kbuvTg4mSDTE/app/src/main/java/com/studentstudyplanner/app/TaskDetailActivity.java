package com.studentstudyplanner.app;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ArrayAdapter;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class TaskDetailActivity extends Activity {

    private EditText titleEditText;
    private EditText descriptionEditText;
    private TextView dueDateTextView;
    private Spinner prioritySpinner;
    private CheckBox completedCheckBox;
    private Button saveButton;
    private Button deleteButton;
    private Button setReminderButton;
    private Button clearReminderButton;
    private TextView reminderStatusTextView;

    private StudyPlannerDatabaseHelper db;
    private long taskId = -1; // -1 indicates a new task
    private long subjectId;
    private long selectedDueDate = System.currentTimeMillis(); // Default to current time
    private long currentReminderTime = -1; // -1 indicates no reminder set

    public static final int RESULT_TASK_DELETED = 1003;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_detail);

        titleEditText = (EditText) findViewById(R.id.edit_text_task_title);
        descriptionEditText = (EditText) findViewById(R.id.edit_text_task_description);
        dueDateTextView = (TextView) findViewById(R.id.text_view_due_date);
        prioritySpinner = (Spinner) findViewById(R.id.spinner_priority);
        completedCheckBox = (CheckBox) findViewById(R.id.checkbox_completed);
        saveButton = (Button) findViewById(R.id.button_save_task);
        deleteButton = (Button) findViewById(R.id.button_delete_task);
        setReminderButton = (Button) findViewById(R.id.button_set_reminder);
        clearReminderButton = (Button) findViewById(R.id.button_clear_reminder);
        reminderStatusTextView = (TextView) findViewById(R.id.text_view_reminder_status);

        db = new StudyPlannerDatabaseHelper(this);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.priority_array,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        prioritySpinner.setAdapter(adapter);

        Intent intent = getIntent();
        if (intent != null) {
            subjectId = intent.getLongExtra("subject_id", -1); // Always expect subjectId

            if (intent.hasExtra("task_id")) {
                taskId = intent.getLongExtra("task_id", -1);
                loadTaskData(taskId);
                deleteButton.setVisibility(View.VISIBLE); // Show delete button for existing tasks
            } else {
                deleteButton.setVisibility(View.GONE); // Hide delete button for new tasks
                // Set default due date to today + 1 day
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DAY_OF_YEAR, 1);
                selectedDueDate = calendar.getTimeInMillis();
                updateDueDateTextView();
            }
        }

        dueDateTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog();
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveTask();
            }
        });

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmDeleteTask();
            }
        });

        setReminderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showReminderPickerDialog();
            }
        });

        clearReminderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmClearReminder();
            }
        });

        updateReminderUI();
    }

    private void loadTaskData(long id) {
        Task task = db.getTask(id);
        if (task != null) {
            titleEditText.setText(task.getTitle());
            descriptionEditText.setText(task.getDescription());
            selectedDueDate = task.getDueDate();
            updateDueDateTextView();
            prioritySpinner.setSelection(task.getPriority());
            completedCheckBox.setChecked(task.getStatus() == 1);
            getActionBar().setTitle("Edit Task");

            Reminder existingReminder = db.getReminderByTaskId(taskId);
            if (existingReminder != null && existingReminder.getIsActive() == 1) {
                currentReminderTime = existingReminder.getReminderTime();
            } else {
                currentReminderTime = -1;
            }
        } else {
            Toast.makeText(this, "Task not found!", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void updateDueDateTextView() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        dueDateTextView.setText(sdf.format(new Date(selectedDueDate)));
    }

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(selectedDueDate);
        new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Calendar newDate = Calendar.getInstance();
                newDate.setTimeInMillis(selectedDueDate); // Keep current time, just update date
                newDate.set(Calendar.YEAR, year);
                newDate.set(Calendar.MONTH, monthOfYear);
                newDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                selectedDueDate = newDate.getTimeInMillis();
                updateDueDateTextView();
            }
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void saveTask() {
        String title = titleEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();
        int priority = prioritySpinner.getSelectedItemPosition();
        int status = completedCheckBox.isChecked() ? 1 : 0;

        if (title.isEmpty()) {
            Toast.makeText(this, "Task title cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }
        if (subjectId == -1) {
            Toast.makeText(this, "Error: Subject not associated with task.", Toast.LENGTH_SHORT).show();
            return;
        }

        Task task = new Task();
        task.setSubjectId(subjectId);
        task.setTitle(title);
        task.setDescription(description);
        task.setDueDate(selectedDueDate);
        task.setPriority(priority);
        task.setStatus(status);

        if (taskId == -1) { // New task
            long newTaskId = db.addTask(task);
            if (newTaskId != -1) {
                taskId = newTaskId; // Update taskId for potential reminder saving
                saveReminder(taskId);
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(this, "Failed to add task", Toast.LENGTH_SHORT).show();
            }
        } else { // Existing task
            task.setId(taskId);
            int rowsAffected = db.updateTask(task);
            if (rowsAffected > 0) {
                saveReminder(taskId);
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(this, "Failed to update task", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void confirmDeleteTask() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Task");
        builder.setMessage("Are you sure you want to delete this task and its reminder?");
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ReminderUtils.cancelReminder(TaskDetailActivity.this, (int) taskId);
                db.deleteTask(taskId);
                setResult(RESULT_TASK_DELETED); // Custom result code for deletion
                finish();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    private void showReminderPickerDialog() {
        // Use existing due date time as initial for reminder if no reminder set
        Calendar calendar = Calendar.getInstance();
        if (currentReminderTime != -1) {
            calendar.setTimeInMillis(currentReminderTime);
        } else {
            calendar.setTimeInMillis(selectedDueDate); // Use task due date as initial time
        }

        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        new TimePickerDialog(this, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                Calendar reminderCalendar = Calendar.getInstance();
                // Set to today's date initially
                reminderCalendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR));
                reminderCalendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH));
                reminderCalendar.set(Calendar.DAY_OF_MONTH, calendar.get(Calendar.DAY_OF_MONTH));
                reminderCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                reminderCalendar.set(Calendar.MINUTE, minute);
                reminderCalendar.set(Calendar.SECOND, 0);

                currentReminderTime = reminderCalendar.getTimeInMillis();
                // If reminder time is in the past, move it to the next day
                if (currentReminderTime < System.currentTimeMillis()) {
                    reminderCalendar.add(Calendar.DAY_OF_YEAR, 1);
                    currentReminderTime = reminderCalendar.getTimeInMillis();
                }

                updateReminderUI();
                Toast.makeText(TaskDetailActivity.this, "Reminder time set. Save task to apply.", Toast.LENGTH_SHORT).show();
            }
        }, hour, minute, android.text.format.DateFormat.is24HourFormat(this)).show();
    }

    private void saveReminder(long associatedTaskId) {
        if (currentReminderTime != -1) {
            String taskTitle = titleEditText.getText().toString().trim();
            String message = "Reminder: " + taskTitle;

            Reminder existingReminder = db.getReminderByTaskId(associatedTaskId);
            if (existingReminder != null) {
                existingReminder.setMessage(message);
                existingReminder.setReminderTime(currentReminderTime);
                existingReminder.setIsActive(1);
                db.updateReminder(existingReminder);
            } else {
                Reminder newReminder = new Reminder();
                newReminder.setTaskId(associatedTaskId);
                newReminder.setMessage(message);
                newReminder.setReminderTime(currentReminderTime);
                newReminder.setIsActive(1);
                db.addReminder(newReminder);
            }
            ReminderUtils.setReminder(this, (int) associatedTaskId, currentReminderTime, message);
            Toast.makeText(this, "Reminder set for task!", Toast.LENGTH_SHORT).show();
        } else {
            // If currentReminderTime is -1, means user cleared it or never set it
            Reminder existingReminder = db.getReminderByTaskId(associatedTaskId);
            if (existingReminder != null) {
                ReminderUtils.cancelReminder(this, (int) associatedTaskId);
                db.deleteReminder(existingReminder.getId()); // Remove from DB
                Toast.makeText(this, "Reminder cleared for task!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void confirmClearReminder() {
        if (currentReminderTime == -1) {
            Toast.makeText(this, "No reminder is currently set.", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Clear Reminder");
        builder.setMessage("Are you sure you want to clear this task's reminder?");
        builder.setPositiveButton("Clear", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                currentReminderTime = -1; // Mark for clearing
                updateReminderUI();
                Toast.makeText(TaskDetailActivity.this, "Reminder marked for clearing. Save task to apply.", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    private void updateReminderUI() {
        if (currentReminderTime != -1) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            reminderStatusTextView.setText("Reminder set for: " + sdf.format(new Date(currentReminderTime)));
            clearReminderButton.setVisibility(View.VISIBLE);
        } else {
            reminderStatusTextView.setText("No reminder set.");
            clearReminderButton.setVisibility(View.GONE);
        }
    }
}