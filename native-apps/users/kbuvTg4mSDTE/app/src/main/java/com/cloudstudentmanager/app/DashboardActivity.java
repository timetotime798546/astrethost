package com.cloudstudentmanager.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;

public class DashboardActivity extends Activity {
    private LinearLayout studentContainer;
    private ProgressBar loader;
    private BackendApi api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        studentContainer = findViewById(R.id.studentContainer);
        loader = findViewById(R.id.loader);
        
        api = new BackendApi(this);
        SessionManager session = new SessionManager(this);
        api.setToken(session.getToken());

        findViewById(R.id.btnAddStudent).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(DashboardActivity.this, AddEditActivity.class));
            }
        });

        findViewById(R.id.btnProfile).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(DashboardActivity.this, ProfileActivity.class));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadStudents();
    }

    private void loadStudents() {
        loader.setVisibility(View.VISIBLE);
        studentContainer.removeAllViews();

        api.getStudents(new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(final JSONObject response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        loader.setVisibility(View.GONE);
                        try {
                            JSONArray records = response.getJSONArray("records");
                            if (records.length() == 0) {
                                TextView empty = new TextView(DashboardActivity.this);
                                empty.setText("No students found. Add one!");
                                empty.setTextColor(0xFF888888);
                                empty.setGravity(android.view.Gravity.CENTER);
                                studentContainer.addView(empty);
                            }
                            for (int i = 0; i < records.length(); i++) {
                                addStudentCard(records.getJSONObject(i));
                            }
                        } catch (Exception e) {
                            Toast.makeText(DashboardActivity.this, "Parsing error", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

            @Override
            public void onError(final String message) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        loader.setVisibility(View.GONE);
                        Toast.makeText(DashboardActivity.this, "Error: " + message, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void addStudentCard(final JSONObject record) throws Exception {
        final String id = record.getString("id");
        JSONObject data = record.getJSONObject("data");
        final String name = data.getString("name");
        final String roll = data.getString("roll_no");
        final String course = data.getString("course");

        View card = getLayoutInflater().inflate(android.R.layout.simple_list_item_2, null);
        card.setBackgroundColor(0xFF1E1E1E);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, 16);
        card.setLayoutParams(lp);
        card.setPadding(32, 32, 32, 32);

        TextView text1 = card.findViewById(android.R.id.text1);
        text1.setText(name + " (" + roll + ")");
        text1.setTextColor(0xFFFFFFFF);
        text1.setTextSize(18);

        TextView text2 = card.findViewById(android.R.id.text2);
        text2.setText(course);
        text2.setTextColor(0xFF10B981);

        card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DashboardActivity.this, AddEditActivity.class);
                intent.putExtra("student_json", record.toString());
                startActivity(intent);
            }
        });

        studentContainer.addView(card);
    }
}