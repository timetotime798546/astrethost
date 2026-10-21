package com.studymasterplanner.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.studymasterplanner.app.models.Subject;
import com.studymasterplanner.app.network.BackendApi;
import com.studymasterplanner.app.utils.SessionManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SubjectsActivity extends Activity {

    private EditText etSubjectName, etSubjectHours;
    private Button btnAddSubject, btnBack;
    private LinearLayout containerSubjects;

    private BackendApi backendApi;
    private SessionManager sessionManager;
    private List<Subject> subjectList = new ArrayList<Subject>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subjects);

        backendApi = new BackendApi(this);
        sessionManager = new SessionManager(this);

        etSubjectName = (EditText) findViewById(R.id.et_subject_name);
        etSubjectHours = (EditText) findViewById(R.id.et_subject_hours);
        btnAddSubject = (Button) findViewById(R.id.btn_add_subject);
        btnBack = (Button) findViewById(R.id.btn_back_subjects);
        containerSubjects = (LinearLayout) findViewById(R.id.container_subjects_list);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnAddSubject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSubject();
            }
        });

        loadSubjects();
    }

    private void loadSubjects() {
        subjectList.clear();
        // Load default subjects
        subjectList.add(new Subject("sub_1", "Calculus", "#3F51B5", 3.0));
        subjectList.add(new Subject("sub_2", "Physics", "#009688", 2.0));
        subjectList.add(new Subject("sub_3", "Chemistry", "#FF9800", 2.0));

        if (sessionManager.isLoggedIn()) {
            backendApi.getData("subjects", new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    JSONArray records = response.optJSONArray("records");
                    if (records != null && records.length() > 0) {
                        subjectList.clear();
                        for (int i = 0; i < records.length(); i++) {
                            JSONObject rec = records.optJSONObject(i);
                            if (rec != null) {
                                String id = rec.optString("id", "");
                                JSONObject data = rec.optJSONObject("data");
                                if (data != null) {
                                    subjectList.add(Subject.fromJson(id, data));
                                }
                            }
                        }
                    }
                    renderSubjects();
                }

                @Override
                public void onError(String message) {
                    renderSubjects();
                }
            });
        } else {
            renderSubjects();
        }
    }

    private void saveSubject() {
        final String name = etSubjectName.getText().toString().trim();
        final String hoursStr = etSubjectHours.getText().toString().trim();

        if (name.isEmpty() || hoursStr.isEmpty()) {
            Toast.makeText(this, "Please enter all subject fields", Toast.LENGTH_SHORT).show();
            return;
        }

        double hours = 2.0;
        try {
            hours = Double.parseDouble(hoursStr);
        } catch (Exception ignored) {}

        final Subject newSubject = new Subject("sub_" + System.currentTimeMillis(), name, "#3F51B5", hours);
        subjectList.add(newSubject);
        renderSubjects();

        etSubjectName.setText("");
        etSubjectHours.setText("");

        if (sessionManager.isLoggedIn()) {
            backendApi.createData("subjects", newSubject.toJson(), new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    Toast.makeText(SubjectsActivity.this, "Subject synced online!", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(String message) {
                    Toast.makeText(SubjectsActivity.this, "Subject stored locally", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void renderSubjects() {
        containerSubjects.removeAllViews();
        for (int i = 0; i < subjectList.size(); i++) {
            Subject sub = subjectList.get(i);

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundColor(Color.WHITE);
            card.setPadding(16, 14, 16, 14);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, 12);
            card.setLayoutParams(lp);

            TextView tvName = new TextView(this);
            tvName.setText(sub.getTitle());
            tvName.setTextSize(16);
            tvName.setTextColor(Color.parseColor("#212121"));
            card.addView(tvName);

            TextView tvHours = new TextView(this);
            tvHours.setText("Target: " + sub.getTargetHours() + " hours / day");
            tvHours.setTextSize(13);
            tvHours.setTextColor(Color.parseColor("#757575"));
            card.addView(tvHours);

            containerSubjects.addView(card);
        }
    }
}