package com.studyplanner.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;

public class TaskActivity extends Activity {
    private Button btnTaskBack, btnCreateTask, btnStartTimer, btnPauseTimer, btnSaveStudy;
    private EditText etTaskTitle, etTaskDueDate, etReminderTime;
    private Spinner spSubjectFilter, spSubjectFilterView;
    private ListView lvTasks;

    private TextView tvTimerHeader, tvTimerValue;

    private BackendApi backendApi;
    private String token;

    private ArrayList<JSONObject> subjectsList = new ArrayList<>();
    private ArrayList<JSONObject> allTasksList = new ArrayList<>();
    private ArrayList<JSONObject> filteredTasksList = new ArrayList<>();
    private ArrayList<String> subjectSpinnerNames = new ArrayList<>();

    private TaskAdapter taskListAdapter;

    // LOCAL STUDY TIMER CONTROL
    private int secondsElapsed = 0;
    private boolean timerRunning = false;
    private Handler timerHandler = new Handler();
    private Runnable timerRunnable;
    private JSONObject selectedTaskForTimer = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task);

        SharedPreferences prefs = getSharedPreferences("StudyPlanner", MODE_PRIVATE);
        token = prefs.getString("token", null);

        btnTaskBack = (Button) findViewById(R.id.btnTaskBack);
        btnCreateTask = (Button) findViewById(R.id.btnCreateTask);
        btnStartTimer = (Button) findViewById(R.id.btnStartTimer);
        btnPauseTimer = (Button) findViewById(R.id.btnPauseTimer);
        btnSaveStudy = (Button) findViewById(R.id.btnSaveStudy);

        etTaskTitle = (EditText) findViewById(R.id.etTaskTitle);
        etTaskDueDate = (EditText) findViewById(R.id.etTaskDueDate);
        etReminderTime = (EditText) findViewById(R.id.etReminderTime);

        spSubjectFilter = (Spinner) findViewById(R.id.spSubjectFilter);
        spSubjectFilterView = (Spinner) findViewById(R.id.spSubjectFilterView);
        lvTasks = (ListView) findViewById(R.id.lvTasks);

        tvTimerHeader = (TextView) findViewById(R.id.tvTimerHeader);
        tvTimerValue = (TextView) findViewById(R.id.tvTimerValue);

        backendApi = new BackendApi(this);

        taskListAdapter = new TaskAdapter();
        lvTasks.setAdapter(taskListAdapter);

        btnTaskBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnCreateTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addNewTask();
            }
        });

        lvTasks.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectTaskForTimer(position);
            }
        });

        lvTasks.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                confirmDelete(position);
                return true;
            }
        });

        spSubjectFilterView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterTasksBySelectedSubject();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        setupTimerRunnable();
        loadInitialData();
    }

    private void loadInitialData() {
        backendApi.readRecords(token, "subjects", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject obj = new JSONObject(response);
                    JSONArray arr = obj.optJSONArray("records");
                    subjectsList.clear();
                    subjectSpinnerNames.clear();

                    subjectSpinnerNames.add("All Subjects");

                    if (arr != null) {
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject sub = arr.getJSONObject(i);
                            subjectsList.add(sub);
                            subjectSpinnerNames.add(sub.getJSONObject("data").getString("name"));
                        }
                    }

                    ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                        TaskActivity.this,
                        android.R.layout.simple_spinner_item,
                        subjectSpinnerNames
                    );
                    spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                    spSubjectFilter.setAdapter(spinnerAdapter);
                    spSubjectFilterView.setAdapter(spinnerAdapter);

                    // Now load Tasks after setup
                    loadTasks();
                } catch (Exception e) {
                    Toast.makeText(TaskActivity.this, "Subjects parsing failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(TaskActivity.this, "Subjects load error: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadTasks() {
        backendApi.readRecords(token, "tasks", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject obj = new JSONObject(response);
                    JSONArray arr = obj.optJSONArray("records");
                    allTasksList.clear();
                    if (arr != null) {
                        for (int i = 0; i < arr.length(); i++) {
                            allTasksList.add(arr.getJSONObject(i));
                        }
                    }
                    filterTasksBySelectedSubject();
                } catch (Exception e) {
                    Toast.makeText(TaskActivity.this, "Tasks parsing failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(TaskActivity.this, "Tasks load error: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void filterTasksBySelectedSubject() {
        int selectedIndex = spSubjectFilterView.getSelectedItemPosition();
        filteredTasksList.clear();

        if (selectedIndex <= 0) {
            // "All Subjects" option
            filteredTasksList.addAll(allTasksList);
        } else {
            try {
                JSONObject chosenSubject = subjectsList.get(selectedIndex - 1);
                String subId = chosenSubject.getString("id");

                for (int i = 0; i < allTasksList.size(); i++) {
                    JSONObject task = allTasksList.get(i);
                    if (task.getJSONObject("data").optString("subject_id").equals(subId)) {
                        filteredTasksList.add(task);
                    }
                }
            } catch (Exception e) {
                filteredTasksList.addAll(allTasksList);
            }
        }
        taskListAdapter.notifyDataSetChanged();
    }

    private void addNewTask() {
        int selectedIndex = spSubjectFilter.getSelectedItemPosition();
        if (selectedIndex <= 0) {
            Toast.makeText(this, "Please select/create a Subject first", Toast.LENGTH_SHORT).show();
            return;
        }

        String title = etTaskTitle.getText().toString().trim();
        String dueDate = etTaskDueDate.getText().toString().trim();
        String reminder = etReminderTime.getText().toString().trim();

        if (title.isEmpty() || dueDate.isEmpty()) {
            Toast.makeText(this, "Title and Due Date are mandatory", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            JSONObject chosenSubject = subjectsList.get(selectedIndex - 1);
            String subId = chosenSubject.getString("id");

            JSONObject data = new JSONObject();
            data.put("subject_id", subId);
            data.put("title", title);
            data.put("due_date", dueDate);
            data.put("completed", false);
            data.put("study_minutes", 0);
            data.put("reminder_time", reminder.isEmpty() ? "No dynamic alert" : reminder);

            backendApi.createRecord(token, "tasks", data, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    etTaskTitle.setText("");
                    etTaskDueDate.setText("");
                    etReminderTime.setText("");
                    Toast.makeText(TaskActivity.this, "Task schedule created", Toast.LENGTH_SHORT).show();
                    loadTasks();
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(TaskActivity.this, "Failed creating task: " + error, Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "Error building request object", Toast.LENGTH_SHORT).show();
        }
    }

    private void selectTaskForTimer(int position) {
        try {
            selectedTaskForTimer = filteredTasksList.get(position);
            String title = selectedTaskForTimer.getJSONObject("data").getString("title");
            tvTimerHeader.setText("Selected Task: " + title);
            resetTimer();
        } catch (Exception e) {
            Toast.makeText(this, "Error selecting task", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupTimerRunnable() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (timerRunning) {
                    secondsElapsed++;
                    updateTimerDisplay();
                    timerHandler.postDelayed(this, 1000);
                }
            }
        };

        btnStartTimer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedTaskForTimer == null) {
                    Toast.makeText(TaskActivity.this, "Please tap a task in the list first", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!timerRunning) {
                    timerRunning = true;
                    timerHandler.postDelayed(timerRunnable, 1000);
                }
            }
        });

        btnPauseTimer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                timerRunning = false;
            }
        });

        btnSaveStudy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveAccumulatedStudyMinutes();
            }
        });
    }

    private void resetTimer() {
        timerRunning = false;
        secondsElapsed = 0;
        updateTimerDisplay();
    }

    private void updateTimerDisplay() {
        int mins = secondsElapsed / 60;
        int secs = secondsElapsed % 60;
        tvTimerValue.setText(String.format("%02d:%02d", mins, secs));
    }

    private void saveAccumulatedStudyMinutes() {
        if (selectedTaskForTimer == null) {
            Toast.makeText(this, "No task selected", Toast.LENGTH_SHORT).show();
            return;
        }

        timerRunning = false;
        // Calculate minutes offline locally
        final double minutesLogged = (double) secondsElapsed / 60.0;
        if (minutesLogged < 0.05) {
            Toast.makeText(this, "Track longer session duration to save study progress", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            final String taskId = selectedTaskForTimer.getString("id");
            JSONObject oldData = selectedTaskForTimer.getJSONObject("data");

            double previousMinutes = oldData.optDouble("study_minutes", 0);
            double updatedMinutes = previousMinutes + minutesLogged;

            JSONObject newData = new JSONObject();
            newData.put("subject_id", oldData.getString("subject_id"));
            newData.put("title", oldData.getString("title"));
            newData.put("due_date", oldData.getString("due_date"));
            newData.put("completed", oldData.optBoolean("completed", false));
            newData.put("reminder_time", oldData.optString("reminder_time", ""));
            newData.put("study_minutes", updatedMinutes);

            backendApi.updateRecord(token, taskId, newData, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    Toast.makeText(TaskActivity.this, String.format("Logged %.1f study minutes successfully!", minutesLogged), Toast.LENGTH_LONG).show();
                    resetTimer();
                    selectedTaskForTimer = null;
                    tvTimerHeader.setText("Select a pending task from list to study");
                    loadTasks();
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(TaskActivity.this, "Error saving: " + error, Toast.LENGTH_SHORT).show();
                }
            });

        } catch (Exception e) {
            Toast.makeText(this, "Logging failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleTaskCompletion(int position, final boolean completedValue) {
        try {
            JSONObject task = filteredTasksList.get(position);
            String id = task.getString("id");
            JSONObject data = task.getJSONObject("data");

            JSONObject updatedData = new JSONObject();
            updatedData.put("subject_id", data.getString("subject_id"));
            updatedData.put("title", data.getString("title"));
            updatedData.put("due_date", data.getString("due_date"));
            updatedData.put("completed", completedValue);
            updatedData.put("study_minutes", data.optDouble("study_minutes", 0));
            updatedData.put("reminder_time", data.optString("reminder_time", ""));

            backendApi.updateRecord(token, id, updatedData, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    Toast.makeText(TaskActivity.this, "Task status updated", Toast.LENGTH_SHORT).show();
                    loadTasks();
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(TaskActivity.this, "Failed updating state: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "Failed local completion setup", Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmDelete(final int position) {
        new AlertDialog.Builder(this)
            .setTitle("Delete Planner Task")
            .setMessage("Are you sure you want to drop this planner assignment record?")
            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    try {
                        JSONObject task = filteredTasksList.get(position);
                        String id = task.getString("id");
                        backendApi.deleteRecord(token, id, new BackendApi.ApiCallback() {
                            @Override
                            public void onSuccess(String response) {
                                Toast.makeText(TaskActivity.this, "Task dropped", Toast.LENGTH_SHORT).show();
                                loadTasks();
                            }

                            @Override
                            public void onError(String error) {
                                Toast.makeText(TaskActivity.this, "Drop failed: " + error, Toast.LENGTH_LONG).show();
                            }
                        });
                    } catch (Exception e) {
                        Toast.makeText(TaskActivity.this, "Error deleting task", Toast.LENGTH_SHORT).show();
                    }
                }
            })
            .setNegativeButton("No", null)
            .show();
    }

    private class TaskAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return filteredTasksList.size();
        }

        @Override
        public Object getItem(int position) {
            return filteredTasksList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(android.R.layout.simple_list_item_multiple_choice, parent, false);
            }
            final CheckBox checkView = (CheckBox) convertView.findViewById(android.R.id.text1);
            TextView descView = (TextView) convertView.findViewById(android.R.id.text2);
            if (descView == null) {
                descView = new TextView(TaskActivity.this);
                descView.setPadding(15, 0, 15, 10);
                descView.setTextSize(12sp);
                descView.setTextColor(0xFF555555);
                ((ViewGroup) convertView).addView(descView);
            }

            try {
                JSONObject item = filteredTasksList.get(position);
                final JSONObject data = item.getJSONObject("data");

                String title = data.optString("title", "");
                final boolean completed = data.optBoolean("completed", false);
                double minutes = data.optDouble("study_minutes", 0);
                String dueDate = data.optString("due_date", "");
                String reminder = data.optString("reminder_time", "None");

                checkView.setText(title);
                checkView.setChecked(completed);

                // Find subject name
                String subName = "Coursework";
                String subId = data.optString("subject_id", "");
                for (JSONObject sub : subjectsList) {
                    if (sub.getString("id").equals(subId)) {
                        subName = sub.getJSONObject("data").getString("name");
                        break;
                    }
                }

                descView.setText("Subject: " + subName + " | Due: " + dueDate + " | Alerts: " + reminder + " | Time Spent: " + String.format("%.1f", minutes) + " mins");

                checkView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        toggleTaskCompletion(position, !completed);
                    }
                });

            } catch (Exception e) {
                checkView.setText("Error reading task record");
            }

            return convertView;
        }
    }
}