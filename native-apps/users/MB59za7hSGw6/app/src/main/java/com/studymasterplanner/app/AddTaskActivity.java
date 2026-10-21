package com.studymasterplanner.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.studymasterplanner.app.models.StudyTask;
import com.studymasterplanner.app.network.BackendApi;
import com.studymasterplanner.app.utils.SessionManager;

import org.json.JSONArray;
import org.json.JSONObject;

public class AddTaskActivity extends Activity {

    private EditText etTitle, etSubject, etMinutes, etDue, etNotes;
    private RadioGroup rgPriority;
    private Button btnSave, btnCancel;

    private BackendApi backendApi;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task);

        backendApi = new BackendApi(this);
        sessionManager = new SessionManager(this);

        etTitle = (EditText) findViewById(R.id.et_task_title);
        etSubject = (EditText) findViewById(R.id.et_task_subject);
        etMinutes = (EditText) findViewById(R.id.et_task_minutes);
        etDue = (EditText) findViewById(R.id.et_task_due);
        etNotes = (EditText) findViewById(R.id.et_task_notes);
        rgPriority = (RadioGroup) findViewById(R.id.rg_priority);
        btnSave = (Button) findViewById(R.id.btn_save_task);
        btnCancel = (Button) findViewById(R.id.btn_cancel_task);

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveTask();
            }
        });
    }

    private void saveTask() {
        String title = etTitle.getText().toString().trim();
        String subject = etSubject.getText().toString().trim();
        String minsStr = etMinutes.getText().toString().trim();
        String due = etDue.getText().toString().trim();
        String notes = etNotes.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(this, "Task title is required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (subject.isEmpty()) subject = "General";
        if (due.isEmpty()) due = "Today";

        int mins = 30;
        try {
            if (!minsStr.isEmpty()) mins = Integer.parseInt(minsStr);
        } catch (Exception ignored) {}

        String priority = "Medium";
        int checkedId = rgPriority.getCheckedRadioButtonId();
        if (checkedId == R.id.rb_high) {
            priority = "High";
        } else if (checkedId == R.id.rb_low) {
            priority = "Low";
        }

        final StudyTask task = new StudyTask("loc_" + System.currentTimeMillis(), title, subject, due, priority, false, mins, notes);

        // Append locally first
        try {
            String cache = sessionManager.getLocalTasksCache();
            JSONArray arr = new JSONArray(cache);
            JSONObject item = new JSONObject();
            item.put("id", task.getId());
            item.put("data", task.toJson());
            arr.put(item);
            sessionManager.saveLocalTasksCache(arr.toString());
        } catch (Exception ignored) {}

        // Sync with backend if logged in
        if (sessionManager.isLoggedIn()) {
            backendApi.createData("tasks", task.toJson(), new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    Toast.makeText(AddTaskActivity.this, "Study task saved and synced!", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onError(String message) {
                    Toast.makeText(AddTaskActivity.this, "Study task saved offline", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        } else {
            Toast.makeText(this, "Study task added!", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}