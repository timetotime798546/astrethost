package com.taskmanager.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements TaskAdapter.TaskActionListener {

    private BackendApi api;
    private List<Task> tasks;
    private TaskAdapter adapter;

    private ListView lvTasks;
    private TextView tvEmpty;
    private ProgressBar pbMainLoading;

    // Add Task widgets
    private EditText etNewTaskTitle;
    private Button btnAddTask;

    // Statistics layout widgets
    private TextView tvStatsCount;
    private TextView tvStatsPercent;
    private ProgressBar pbStatsProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        api = new BackendApi(this);
        if (!api.isLoggedIn()) {
            redirectToLogin();
            return;
        }

        tasks = new ArrayList<>();
        adapter = new TaskAdapter(this, tasks, this);

        lvTasks = (ListView) findViewById(R.id.lvTasks);
        tvEmpty = (TextView) findViewById(R.id.tvEmpty);
        pbMainLoading = (ProgressBar) findViewById(R.id.pbMainLoading);

        etNewTaskTitle = (EditText) findViewById(R.id.etNewTaskTitle);
        btnAddTask = (Button) findViewById(R.id.btnAddTask);

        tvStatsCount = (TextView) findViewById(R.id.tvStatsCount);
        tvStatsPercent = (TextView) findViewById(R.id.tvStatsPercent);
        pbStatsProgress = (ProgressBar) findViewById(R.id.pbStatsProgress);

        lvTasks.setAdapter(adapter);

        Button btnLogout = (Button) findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                api.logout();
                redirectToLogin();
            }
        });

        btnAddTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addNewTask();
            }
        });

        loadTasksFromServer();
    }

    private void redirectToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void loadTasksFromServer() {
        pbMainLoading.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        api.fetchTasks(new BackendApi.ApiCallback<List<Task>>() {
            @Override
            public void onSuccess(List<Task> result) {
                pbMainLoading.setVisibility(View.GONE);
                tasks.clear();
                tasks.addAll(result);
                adapter.notifyDataSetChanged();
                
                checkEmptyState();
                calculateStatistics();
            }

            @Override
            public void onError(String error) {
                pbMainLoading.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, error, Toast.LENGTH_LONG).show();
                checkEmptyState();
            }
        });
    }

    private void addNewTask() {
        final String title = etNewTaskTitle.getText().toString().trim();
        if (title.isEmpty()) {
            etNewTaskTitle.setError("Task title cannot be empty");
            return;
        }

        btnAddTask.setEnabled(false);

        api.createTask(title, new BackendApi.ApiCallback<Task>() {
            @Override
            public void onSuccess(Task newTask) {
                btnAddTask.setEnabled(true);
                etNewTaskTitle.setText("");
                tasks.add(newTask);
                adapter.notifyDataSetChanged();
                
                checkEmptyState();
                calculateStatistics();
                Toast.makeText(MainActivity.this, "Task Added", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String error) {
                btnAddTask.setEnabled(true);
                Toast.makeText(MainActivity.this, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    // MANDATORY OFFLINE STATS CALCULATION
    private void calculateStatistics() {
        int total = tasks.size();
        int completed = 0;
        for (int i = 0; i < total; i++) {
            if (tasks.get(i).isCompleted()) {
                completed++;
            }
        }

        int percentage = 0;
        if (total > 0) {
            // Local calculation logic: (completed / total) * 100
            percentage = (int) (((double) completed / total) * 100);
        }

        tvStatsCount.setText(completed + " of " + total + " Completed");
        tvStatsPercent.setText(percentage + "%");
        pbStatsProgress.setProgress(percentage);
    }

    private void checkEmptyState() {
        if (tasks.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            tvEmpty.setVisibility(View.GONE);
        }
    }

    // Task list interaction callback: Complete status change
    @Override
    public void onToggleStatus(final Task task, final boolean isChecked) {
        final String targetStatus = isChecked ? "completed" : "pending";
        
        api.updateTaskStatus(task.getId(), task.getTitle(), targetStatus, new BackendApi.ApiCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                task.setStatus(targetStatus);
                adapter.notifyDataSetChanged();
                calculateStatistics();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(MainActivity.this, "Failed to update: " + error, Toast.LENGTH_SHORT).show();
                adapter.notifyDataSetChanged(); // Restore UI checkbox state
            }
        });
    }

    // Task list interaction callback: Delete button click
    @Override
    public void onDelete(final Task task) {
        api.deleteTask(task.getId(), new BackendApi.ApiCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                tasks.remove(task);
                adapter.notifyDataSetChanged();
                checkEmptyState();
                calculateStatistics();
                Toast.makeText(MainActivity.this, "Task Deleted", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(MainActivity.this, "Failed to delete: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }
}