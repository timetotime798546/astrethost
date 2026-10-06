package com.studyplanner.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;

public class MainActivity extends Activity {
    private TextView tvTotalStudyHours, tvCompletedTasks, tvProgressRate, tvProgressInterpretation, tvNoUpcomingTasks;
    private Button btnLogout, btnManageSubjects, btnManageTasks;
    private ListView lvUpcomingTasks;

    private BackendApi backendApi;
    private String token;

    private ArrayList<JSONObject> tasksList = new ArrayList<>();
    private ArrayList<JSONObject> subjectsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("StudyPlanner", MODE_PRIVATE);
        token = prefs.getString("token", null);
        if (token == null) {
            goToLogin();
            return;
        }

        setContentView(R.layout.activity_main);

        tvTotalStudyHours = (TextView) findViewById(R.id.tvTotalStudyHours);
        tvCompletedTasks = (TextView) findViewById(R.id.tvCompletedTasks);
        tvProgressRate = (TextView) findViewById(R.id.tvProgressRate);
        tvProgressInterpretation = (TextView) findViewById(R.id.tvProgressInterpretation);
        tvNoUpcomingTasks = (TextView) findViewById(R.id.tvNoUpcomingTasks);

        btnLogout = (Button) findViewById(R.id.btnLogout);
        btnManageSubjects = (Button) findViewById(R.id.btnManageSubjects);
        btnManageTasks = (Button) findViewById(R.id.btnManageTasks);
        lvUpcomingTasks = (ListView) findViewById(R.id.lvUpcomingTasks);

        backendApi = new BackendApi(this);

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = getSharedPreferences("StudyPlanner", MODE_PRIVATE).edit();
                editor.remove("token");
                editor.remove("email");
                editor.apply();
                goToLogin();
            }
        });

        btnManageSubjects.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SubjectActivity.class));
            }
        });

        btnManageTasks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, TaskActivity.class));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAllDataAndCalculateMetrics();
    }

    private void goToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void loadAllDataAndCalculateMetrics() {
        // Fetch subjects first, then tasks to perform accurate progress tracking math locally
        backendApi.readRecords(token, "subjects", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(String subResp) {
                try {
                    JSONObject mainObj = new JSONObject(subResp);
                    JSONArray arr = mainObj.optJSONArray("records");
                    subjectsList.clear();
                    if (arr != null) {
                        for (int i = 0; i < arr.length(); i++) {
                            subjectsList.add(arr.getJSONObject(i));
                        }
                    }
                    // Next, fetch tasks
                    fetchTasks();
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Subjects parsing failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(MainActivity.this, "Subjects API Error: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void fetchTasks() {
        backendApi.readRecords(token, "tasks", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(String taskResp) {
                try {
                    JSONObject mainObj = new JSONObject(taskResp);
                    JSONArray arr = mainObj.optJSONArray("records");
                    tasksList.clear();
                    if (arr != null) {
                        for (int i = 0; i < arr.length(); i++) {
                            tasksList.add(arr.getJSONObject(i));
                        }
                    }
                    calculateLocalDashboardStats();
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Tasks parsing failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(MainActivity.this, "Tasks API Error: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void calculateLocalDashboardStats() {
        // MANDATORY OFFLINE CALCULATION & BUSINESS LOGIC
        double totalMinutes = 0;
        int totalTasksCount = tasksList.size();
        int completedTasksCount = 0;

        for (int i = 0; i < totalTasksCount; i++) {
            JSONObject record = tasksList.get(i);
            JSONObject dataObj = record.optJSONObject("data");
            if (dataObj != null) {
                totalMinutes += dataObj.optDouble("study_minutes", 0.0);
                if (dataObj.optBoolean("completed", false)) {
                    completedTasksCount++;
                }
            }
        }

        double totalHours = totalMinutes / 60.0;
        int completionRate = 0;
        if (totalTasksCount > 0) {
            completionRate = (int) (((double) completedTasksCount / (double) totalTasksCount) * 100);
        }

        tvTotalStudyHours.setText(String.format("%.1fh", totalHours));
        tvCompletedTasks.setText(completedTasksCount + "/" + totalTasksCount);
        tvProgressRate.setText(completionRate + "%");

        if (totalTasksCount == 0) {
            tvProgressInterpretation.setText("Create academic subjects and tasks to initiate planning.");
        } else if (completionRate < 50) {
            tvProgressInterpretation.setText("Keep going! You have completed " + completionRate + "% of your dynamic task goals.");
        } else if (completionRate < 100) {
            tvProgressInterpretation.setText("Excellent effort! Over half of your syllabus tasks are done.");
        } else {
            tvProgressInterpretation.setText("Bravo! Perfect 100% study goal achievement recorded!");
        }

        // Build list adapter
        UpcomingTaskAdapter adapter = new UpcomingTaskAdapter();
        lvUpcomingTasks.setAdapter(adapter);

        if (tasksList.isEmpty()) {
            tvNoUpcomingTasks.setVisibility(View.VISIBLE);
            lvUpcomingTasks.setVisibility(View.GONE);
        } else {
            tvNoUpcomingTasks.setVisibility(View.GONE);
            lvUpcomingTasks.setVisibility(View.VISIBLE);
        }
    }

    private class UpcomingTaskAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return Math.min(tasksList.size(), 8); // show up to 8 dashboard reminders
        }

        @Override
        public Object getItem(int position) {
            return tasksList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(android.R.layout.simple_list_item_2, parent, false);
            }
            TextView text1 = (TextView) convertView.findViewById(android.R.id.text1);
            TextView text2 = (TextView) convertView.findViewById(android.R.id.text2);

            try {
                JSONObject item = tasksList.get(position);
                JSONObject data = item.getJSONObject("data");

                String title = data.optString("title", "");
                String dueDate = data.optString("due_date", "");
                boolean isCompleted = data.optBoolean("completed", false);
                double loggedMinutes = data.optDouble("study_minutes", 0);
                String reminderTime = data.optString("reminder_time", "No dynamic alert");

                String statusString = isCompleted ? "[COMPLETED]" : "[PENDING]";
                text1.setText(statusString + " " + title);
                text1.setTextColor(isCompleted ? 0xFF4CAF50 : 0xFF3F51B5);

                text2.setText("Due: " + dueDate + " | Alert Time: " + reminderTime + " | Logs: " + String.format("%.1f", loggedMinutes) + " mins");
            } catch (Exception e) {
                text1.setText("Unknown Task Record");
            }

            return convertView;
        }
    }
}