package com.studymasterplanner.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.studymasterplanner.app.models.StudyTask;
import com.studymasterplanner.app.network.BackendApi;
import com.studymasterplanner.app.utils.ProgressCalculator;
import com.studymasterplanner.app.utils.SessionManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private ProgressBar pbOverallProgress;
    private TextView tvStatPercent, tvStatTasks, tvStatTime, tvEmptyTasks;
    private LinearLayout containerTasks;
    private Button btnAddTask, btnNavSubjects, btnNavAuth, btnRefresh;

    private BackendApi backendApi;
    private SessionManager sessionManager;
    private List<StudyTask> taskList = new ArrayList<StudyTask>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        backendApi = new BackendApi(this);
        sessionManager = new SessionManager(this);

        pbOverallProgress = (ProgressBar) findViewById(R.id.pb_overall_progress);
        tvStatPercent = (TextView) findViewById(R.id.tv_stat_percent);
        tvStatTasks = (TextView) findViewById(R.id.tv_stat_tasks);
        tvStatTime = (TextView) findViewById(R.id.tv_stat_time);
        tvEmptyTasks = (TextView) findViewById(R.id.tv_empty_tasks);
        containerTasks = (LinearLayout) findViewById(R.id.container_tasks);
        btnAddTask = (Button) findViewById(R.id.btn_add_task);
        btnNavSubjects = (Button) findViewById(R.id.btn_nav_subjects);
        btnNavAuth = (Button) findViewById(R.id.btn_nav_auth);
        btnRefresh = (Button) findViewById(R.id.btn_refresh);

        btnNavSubjects.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SubjectsActivity.class));
            }
        });

        btnNavAuth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sessionManager.isLoggedIn()) {
                    sessionManager.logout();
                    Toast.makeText(MainActivity.this, "Logged out", Toast.LENGTH_SHORT).show();
                    updateAuthButtonState();
                    loadTasks();
                } else {
                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                }
            }
        });

        btnAddTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AddTaskActivity.class));
            }
        });

        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadTasks();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateAuthButtonState();
        loadTasks();
    }

    private void updateAuthButtonState() {
        if (sessionManager.isLoggedIn()) {
            btnNavAuth.setText("Log Out");
        } else {
            btnNavAuth.setText("Login");
        }
    }

    private void loadTasks() {
        if (sessionManager.isLoggedIn()) {
            backendApi.getData("tasks", new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    taskList.clear();
                    JSONArray records = response.optJSONArray("records");
                    if (records != null) {
                        for (int i = 0; i < records.length(); i++) {
                            JSONObject rec = records.optJSONObject(i);
                            if (rec != null) {
                                String id = rec.optString("id", "");
                                JSONObject data = rec.optJSONObject("data");
                                if (data != null) {
                                    taskList.add(StudyTask.fromJson(id, data));
                                }
                            }
                        }
                    }
                    saveLocalCache();
                    renderTasksAndCalculations();
                }

                @Override
                public void onError(String message) {
                    loadFromLocalCache();
                    renderTasksAndCalculations();
                }
            });
        } else {
            loadFromLocalCache();
            renderTasksAndCalculations();
        }
    }

    private void saveLocalCache() {
        JSONArray arr = new JSONArray();
        for (int i = 0; i < taskList.size(); i++) {
            JSONObject item = new JSONObject();
            try {
                item.put("id", taskList.get(i).getId());
                item.put("data", taskList.get(i).toJson());
                arr.put(item);
            } catch (Exception ignored) {}
        }
        sessionManager.saveLocalTasksCache(arr.toString());
    }

    private void loadFromLocalCache() {
        taskList.clear();
        String json = sessionManager.getLocalTasksCache();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                String id = obj.optString("id", "loc_" + i);
                JSONObject data = obj.optJSONObject("data");
                if (data != null) {
                    taskList.add(StudyTask.fromJson(id, data));
                }
            }
        } catch (Exception ignored) {}

        // Insert mock defaults if completely blank
        if (taskList.isEmpty()) {
            taskList.add(new StudyTask("1", "Read Chapter 4 - Derivatives", "Calculus", "Today 4:00 PM", "High", false, 45, "Review formulas page 120"));
            taskList.add(new StudyTask("2", "Thermodynamics Lab Report", "Physics", "Tomorrow 10:00 AM", "Medium", true, 60, "Graph data and submit PDF"));
            taskList.add(new StudyTask("3", "Organic Chemistry Nomenclature", "Chemistry", "Friday", "Low", false, 30, "Memorize alkanes"));
            saveLocalCache();
        }
    }

    private void renderTasksAndCalculations() {
        // MANDATORY OFFLINE CALCULATION
        ProgressCalculator.Summary summary = ProgressCalculator.calculate(taskList);

        pbOverallProgress.setProgress(summary.completionPercentage);
        tvStatPercent.setText(summary.completionPercentage + "%");
        tvStatTasks.setText(summary.completedTasks + "/" + summary.totalTasks);

        int hours = summary.remainingMinutes / 60;
        int mins = summary.remainingMinutes % 60;
        if (hours > 0) {
            tvStatTime.setText(hours + "h " + mins + "m");
        } else {
            tvStatTime.setText(mins + "m");
        }

        containerTasks.removeAllViews();

        if (taskList.isEmpty()) {
            tvEmptyTasks.setVisibility(View.VISIBLE);
            return;
        } else {
            tvEmptyTasks.setVisibility(View.GONE);
        }

        for (int i = 0; i < taskList.size(); i++) {
            final StudyTask task = taskList.get(i);
            final int index = i;

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setBackgroundColor(Color.WHITE);
            card.setPadding(16, 16, 16, 16);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, 16);
            card.setLayoutParams(lp);

            final CheckBox cb = new CheckBox(this);
            cb.setChecked(task.isCompleted());
            card.addView(cb);

            LinearLayout textCol = new LinearLayout(this);
            textCol.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            tlp.setMargins(12, 0, 8, 0);
            textCol.setLayoutParams(tlp);

            TextView tvTitle = new TextView(this);
            tvTitle.setText(task.getTitle());
            tvTitle.setTextSize(16);
            tvTitle.setTextColor(Color.parseColor(task.isCompleted() ? "#9E9E9E" : "#212121"));
            tvTitle.getPaint().setStrikeThruText(task.isCompleted());
            textCol.addView(tvTitle);

            TextView tvSub = new TextView(this);
            tvSub.setText(task.getSubjectName() + " • Due " + task.getDueDate() + " (" + task.getEstimatedMinutes() + " mins)");
            tvSub.setTextSize(12);
            tvSub.setTextColor(Color.parseColor("#757575"));
            textCol.addView(tvSub);

            if (!task.getNotes().isEmpty()) {
                TextView tvNotes = new TextView(this);
                tvNotes.setText(task.getNotes());
                tvNotes.setTextSize(11);
                tvNotes.setTextColor(Color.parseColor("#B0BEC5"));
                textCol.addView(tvNotes);
            }

            card.addView(textCol);

            // Priority indicator tag
            TextView tvPriority = new TextView(this);
            tvPriority.setText(task.getPriority());
            tvPriority.setTextSize(11);
            tvPriority.setPadding(12, 6, 12, 6);
            if ("High".equalsIgnoreCase(task.getPriority())) {
                tvPriority.setTextColor(Color.parseColor("#E91E63"));
            } else if ("Medium".equalsIgnoreCase(task.getPriority())) {
                tvPriority.setTextColor(Color.parseColor("#FF9800"));
            } else {
                tvPriority.setTextColor(Color.parseColor("#009688"));
            }
            card.addView(tvPriority);

            cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    task.setCompleted(isChecked);
                    if (sessionManager.isLoggedIn() && !task.getId().startsWith("loc_")) {
                        backendApi.updateData(task.getId(), task.toJson(), null);
                    }
                    saveLocalCache();
                    renderTasksAndCalculations();
                }
            });

            containerTasks.addView(card);
        }
    }
}