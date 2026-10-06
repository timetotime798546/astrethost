package com.studyplanner.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;

public class SubjectActivity extends Activity {
    private EditText etSubjectName, etTargetHours;
    private Button btnAddSubject, btnSubjectBack;
    private ListView lvSubjects;

    private BackendApi backendApi;
    private String token;
    private ArrayList<JSONObject> subjectsList = new ArrayList<>();
    private SubjectAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subject);

        SharedPreferences prefs = getSharedPreferences("StudyPlanner", MODE_PRIVATE);
        token = prefs.getString("token", null);

        etSubjectName = (EditText) findViewById(R.id.etSubjectName);
        etTargetHours = (EditText) findViewById(R.id.etTargetHours);
        btnAddSubject = (Button) findViewById(R.id.btnAddSubject);
        btnSubjectBack = (Button) findViewById(R.id.btnSubjectBack);
        lvSubjects = (ListView) findViewById(R.id.lvSubjects);

        backendApi = new BackendApi(this);
        adapter = new SubjectAdapter();
        lvSubjects.setAdapter(adapter);

        btnSubjectBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnAddSubject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addNewSubject();
            }
        });

        lvSubjects.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                confirmDelete(position);
                return true;
            }
        });

        loadSubjects();
    }

    private void loadSubjects() {
        backendApi.readRecords(token, "subjects", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject obj = new JSONObject(response);
                    JSONArray arr = obj.optJSONArray("records");
                    subjectsList.clear();
                    if (arr != null) {
                        for (int i = 0; i < arr.length(); i++) {
                            subjectsList.add(arr.getJSONObject(i));
                        }
                    }
                    adapter.notifyDataSetChanged();
                } catch (Exception e) {
                    Toast.makeText(SubjectActivity.this, "JSON parse failure", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(SubjectActivity.this, "Error load subjects: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void addNewSubject() {
        String name = etSubjectName.getText().toString().trim();
        String hoursStr = etTargetHours.getText().toString().trim();

        if (name.isEmpty() || hoursStr.isEmpty()) {
            Toast.makeText(this, "Please enter all details to register", Toast.LENGTH_SHORT).show();
            return;
        }

        double hours = 0.0;
        try {
            hours = Double.parseDouble(hoursStr);
        } catch (Exception e) {
            Toast.makeText(this, "Target Hours must be a valid float", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            JSONObject data = new JSONObject();
            data.put("name", name);
            data.put("target_hours", hours);

            backendApi.createRecord(token, "subjects", data, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    etSubjectName.setText("");
                    etTargetHours.setText("");
                    Toast.makeText(SubjectActivity.this, "Subject target enrolled", Toast.LENGTH_SHORT).show();
                    loadSubjects();
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(SubjectActivity.this, "Failed saving subject: " + error, Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "Configuration failure: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmDelete(final int position) {
        new AlertDialog.Builder(this)
            .setTitle("Remove Subject")
            .setMessage("Are you sure you want to drop this subject and all its metric logs?")
            .setPositiveButton("Yes, Delete", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    try {
                        JSONObject subObj = subjectsList.get(position);
                        String id = subObj.getString("id");
                        backendApi.deleteRecord(token, id, new BackendApi.ApiCallback() {
                            @Override
                            public void onSuccess(String response) {
                                Toast.makeText(SubjectActivity.this, "Subject deleted", Toast.LENGTH_SHORT).show();
                                loadSubjects();
                            }

                            @Override
                            public void onError(String error) {
                                Toast.makeText(SubjectActivity.this, "Failed deletion: " + error, Toast.LENGTH_LONG).show();
                            }
                        });
                    } catch (Exception e) {
                        Toast.makeText(SubjectActivity.this, "Error deleting: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private class SubjectAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return subjectsList.size();
        }

        @Override
        public Object getItem(int position) {
            return subjectsList.get(position);
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
                JSONObject subject = subjectsList.get(position);
                JSONObject data = subject.getJSONObject("data");
                String name = data.optString("name", "Untitled");
                double hours = data.optDouble("target_hours", 0.0);

                text1.setText(name);
                text1.setTextColor(0xFF3F51B5);
                text1.setTextSize(16);
                text2.setText("Weekly Target Goal: " + hours + " Study Hours");
            } catch (Exception e) {
                text1.setText("Error reading item");
            }

            return convertView;
        }
    }
}