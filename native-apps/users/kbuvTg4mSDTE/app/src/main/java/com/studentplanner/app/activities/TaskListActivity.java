package com.studentplanner.app.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.studentplanner.app.R;
import com.studentplanner.app.adapters.TaskAdapter;
import com.studentplanner.app.data.DatabaseHelper;
import com.studentplanner.app.models.Task;
import com.studentplanner.app.notifications.NotificationHelper;

import java.util.ArrayList;

public class TaskListActivity extends Activity {

    private ListView taskListView;
    private Button addTaskButton;
    private TextView taskListTitle;
    private DatabaseHelper dbHelper;
    private TaskAdapter taskAdapter;
    private ArrayList<Task> tasks;

    private int subjectId;
    private String subjectName;

    private static final int REQUEST_ADD_TASK = 3;
    private static final int REQUEST_EDIT_TASK = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_list);

        dbHelper = new DatabaseHelper(this);

        taskListView = findViewById(R.id.task_list_view);
        addTaskButton = findViewById(R.id.add_task_button);
        taskListTitle = findViewById(R.id.task_list_title);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            subjectId = extras.getInt("subject_id", -1);
            subjectName = extras.getString("subject_name", "Unknown Subject");
        }

        if (subjectId == -1) {
            Toast.makeText(this, "Error: Subject not found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        taskListTitle.setText(getString(R.string.title_activity_task_list, subjectName));
        setTitle(getString(R.string.title_activity_task_list, subjectName));

        loadTasks();

        addTaskButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TaskListActivity.this, AddTaskActivity.class);
                intent.putExtra("subject_id", subjectId);
                intent.putExtra("subject_name", subjectName);
                startActivityForResult(intent, REQUEST_ADD_TASK);
            }
        });

        taskListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Task selectedTask = (Task) parent.getItemAtPosition(position);
                toggleTaskCompletion(selectedTask);
            }
        });

        taskListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Task selectedTask = (Task) parent.getItemAtPosition(position);
                Intent intent = new Intent(TaskListActivity.this, AddTaskActivity.class);
                intent.putExtra("task_id", selectedTask.getId());
                intent.putExtra("subject_id", subjectId);
                intent.putExtra("subject_name", subjectName);
                startActivityForResult(intent, REQUEST_EDIT_TASK);
                return true;
            }
        });
    }

    private void loadTasks() {
        tasks = dbHelper.getTasksBySubject(subjectId);
        taskAdapter = new TaskAdapter(this, tasks);
        taskListView.setAdapter(taskAdapter);
    }

    private void toggleTaskCompletion(Task task) {
        task.setCompleted(!task.isCompleted());
        dbHelper.updateTask(task);
        if (task.isCompleted()) {
            NotificationHelper.cancelReminder(this, task.getId());
        } else {
            NotificationHelper.scheduleReminder(this, task);
        }
        loadTasks(); // Refresh the list
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            loadTasks(); // Refresh the list
            if (requestCode == REQUEST_ADD_TASK) {
                Toast.makeText(this, "Task added!", Toast.LENGTH_SHORT).show();
            } else if (requestCode == REQUEST_EDIT_TASK) {
                Toast.makeText(this, "Task updated!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTasks();
    }
}