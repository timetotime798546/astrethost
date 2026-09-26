package com.studentstudyplanner.app;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class SubjectDetailActivity extends Activity {

    private TextView subjectNameTextView;
    private ListView taskListView;
    private Button addTaskButton;
    private Button editSubjectButton;
    private Button deleteSubjectButton;

    private StudyPlannerDatabaseHelper db;
    private TaskAdapter taskAdapter;
    private List<Task> currentTasks;
    private long subjectId;
    private String subjectName;

    public static final int RESULT_SUBJECT_DELETED = 1002;
    private static final int REQUEST_CODE_ADD_TASK = 1;
    private static final int REQUEST_CODE_EDIT_TASK = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subject_detail);

        subjectNameTextView = (TextView) findViewById(R.id.subject_detail_name);
        taskListView = (ListView) findViewById(R.id.taskListView);
        addTaskButton = (Button) findViewById(R.id.button_add_task);
        editSubjectButton = (Button) findViewById(R.id.button_edit_subject);
        deleteSubjectButton = (Button) findViewById(R.id.button_delete_subject);

        db = new StudyPlannerDatabaseHelper(this);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("subject_id")) {
            subjectId = intent.getLongExtra("subject_id", -1);
            loadSubjectAndTasks(subjectId);
        } else {
            Toast.makeText(this, "Error: Subject ID not found.", Toast.LENGTH_SHORT).show();
            finish();
        }

        currentTasks = new ArrayList<Task>();
        taskAdapter = new TaskAdapter(this, currentTasks);
        taskListView.setAdapter(taskAdapter);

        addTaskButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent taskIntent = new Intent(SubjectDetailActivity.this, TaskDetailActivity.class);
                taskIntent.putExtra("subject_id", subjectId);
                startActivityForResult(taskIntent, REQUEST_CODE_ADD_TASK);
            }
        });

        editSubjectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEditSubjectDialog();
            }
        });

        deleteSubjectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmDeleteSubject();
            }
        });

        taskListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Task selectedTask = (Task) parent.getItemAtPosition(position);
                Intent taskIntent = new Intent(SubjectDetailActivity.this, TaskDetailActivity.class);
                taskIntent.putExtra("task_id", selectedTask.getId());
                taskIntent.putExtra("subject_id", subjectId); // Pass subject_id for context
                startActivityForResult(taskIntent, REQUEST_CODE_EDIT_TASK);
            }
        });

        taskListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Task selectedTask = (Task) parent.getItemAtPosition(position);
                toggleTaskStatus(selectedTask);
                return true;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSubjectAndTasks(subjectId); // Refresh tasks when returning
    }

    private void loadSubjectAndTasks(long id) {
        Subject subject = db.getSubject(id);
        if (subject != null) {
            subjectName = subject.getName();
            subjectNameTextView.setText(subjectName);
            getActionBar().setTitle(subjectName); // Set activity title

            currentTasks.clear();
            currentTasks.addAll(db.getTasksBySubject(subjectId));
            taskAdapter.notifyDataSetChanged();
            updateProgressText();
        } else {
            Toast.makeText(this, "Subject not found!", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void updateProgressText() {
        int totalTasks = db.getTotalTasksCountForSubject(subjectId);
        int completedTasks = db.getCompletedTasksCountForSubject(subjectId);
        TextView progressText = (TextView) findViewById(R.id.subject_tasks_progress_text);
        progressText.setText(completedTasks + " / " + totalTasks + " tasks completed");
    }

    private void showEditSubjectDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Subject Name");

        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_subject, null); // Reuse the add dialog layout
        final EditText subjectNameEditText = (EditText) dialogView.findViewById(R.id.edit_text_subject_name);
        subjectNameEditText.setText(subjectName); // Pre-fill with current name
        builder.setView(dialogView);

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newName = subjectNameEditText.getText().toString().trim();
                if (newName.isEmpty()) {
                    Toast.makeText(SubjectDetailActivity.this, "Subject name cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }
                Subject updatedSubject = new Subject(subjectId, newName);
                int rowsAffected = db.updateSubject(updatedSubject);
                if (rowsAffected > 0) {
                    Toast.makeText(SubjectDetailActivity.this, "Subject updated!", Toast.LENGTH_SHORT).show();
                    loadSubjectAndTasks(subjectId);
                    setResult(RESULT_OK); // Notify MainActivity of change
                } else {
                    Toast.makeText(SubjectDetailActivity.this, "Failed to update subject", Toast.LENGTH_SHORT).show();
                }
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

    private void confirmDeleteSubject() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Subject");
        builder.setMessage("Are you sure you want to delete this subject and all its tasks and reminders? This cannot be undone.");
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                db.deleteSubject(subjectId);
                Toast.makeText(SubjectDetailActivity.this, "Subject deleted!", Toast.LENGTH_SHORT).show();
                setResult(RESULT_SUBJECT_DELETED); // Custom result code for deletion
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

    private void toggleTaskStatus(Task task) {
        int newStatus = (task.getStatus() == 0) ? 1 : 0; // Toggle between Pending (0) and Completed (1)
        task.setStatus(newStatus);
        int rowsAffected = db.updateTask(task);
        if (rowsAffected > 0) {
            Toast.makeText(this, "Task marked as " + task.getStatusString(), Toast.LENGTH_SHORT).show();
            loadSubjectAndTasks(subjectId); // Refresh the list
        } else {
            Toast.makeText(this, "Failed to update task status", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            loadSubjectAndTasks(subjectId); // Reload tasks if an update or addition occurred
            setResult(RESULT_OK); // Propagate success to MainActivity
            Toast.makeText(this, "Task saved!", Toast.LENGTH_SHORT).show();
        } else if (resultCode == TaskDetailActivity.RESULT_TASK_DELETED) {
            loadSubjectAndTasks(subjectId);
            setResult(RESULT_OK); // Propagate success to MainActivity
            Toast.makeText(this, "Task deleted!", Toast.LENGTH_SHORT).show();
        }
    }
}