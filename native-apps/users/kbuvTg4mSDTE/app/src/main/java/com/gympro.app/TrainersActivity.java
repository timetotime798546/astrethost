package com.gympro.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TrainersActivity extends Activity {
    private EditText etName, etSpecialty, etSalary;
    private Button btnSave;
    private ListView lvTrainers;
    private ProgressBar pb;
    private BackendApi api;
    private List<JSONObject> trainerList = new ArrayList<>();
    private ArrayAdapter<JSONObject> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trainers);

        api = new BackendApi(this);

        etName = findViewById(R.id.etTrainerName);
        etSpecialty = findViewById(R.id.etTrainerSpecialty);
        etSalary = findViewById(R.id.etTrainerSalary);
        btnSave = findViewById(R.id.btnSaveTrainer);
        lvTrainers = findViewById(R.id.lvTrainers);
        pb = findViewById(R.id.pbTrainers);

        adapter = new ArrayAdapter<JSONObject>(this, android.R.layout.simple_list_item_2, android.R.id.text1, trainerList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = view.findViewById(android.R.id.text1);
                TextView text2 = view.findViewById(android.R.id.text2);

                JSONObject item = getItem(position);
                if (item != null) {
                    JSONObject data = item.optJSONObject("data");
                    if (data != null) {
                        text1.setText("Trainer: " + data.optString("name", "N/A"));
                        text1.setTextColor(0xFFFFFFFF);
                        String sub = "Specialty: " + data.optString("specialty", "General") + " | Salary: $" + String.format(Locale.US, "%.2f", data.optDouble("salary", 0));
                        text2.setText(sub);
                        text2.setTextColor(0xFFB8FF38);
                    }
                }
                return view;
            }
        };

        lvTrainers.setAdapter(adapter);

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveTrainer();
            }
        });

        loadTrainers();
    }

    private void loadTrainers() {
        pb.setVisibility(View.VISIBLE);
        api.getCollection("trainers", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                pb.setVisibility(View.GONE);
                trainerList.clear();
                JSONArray records = response.optJSONArray("records");
                if (records != null) {
                    for (int i = 0; i < records.length(); i++) {
                        JSONObject r = records.optJSONObject(i);
                        if (r != null) {
                            trainerList.add(r);
                        }
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onError(String errorMessage) {
                pb.setVisibility(View.GONE);
                Toast.makeText(TrainersActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveTrainer() {
        String name = etName.getText().toString().trim();
        String specialty = etSpecialty.getText().toString().trim();
        String salStr = etSalary.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "Trainer name is required", Toast.LENGTH_SHORT).show();
            return;
        }

        double salary = 0;
        try {
            salary = Double.parseDouble(salStr);
        } catch (Exception ignored) {}

        try {
            JSONObject data = new JSONObject();
            data.put("name", name);
            data.put("specialty", specialty.isEmpty() ? "Fitness" : specialty);
            data.put("salary", salary);

            pb.setVisibility(View.VISIBLE);
            api.createRecord("trainers", data, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    pb.setVisibility(View.GONE);
                    etName.setText("");
                    etSpecialty.setText("");
                    etSalary.setText("");
                    Toast.makeText(TrainersActivity.this, "Trainer added!", Toast.LENGTH_SHORT).show();
                    loadTrainers();
                }

                @Override
                public void onError(String errorMessage) {
                    pb.setVisibility(View.GONE);
                    Toast.makeText(TrainersActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "Error building request", Toast.LENGTH_SHORT).show();
        }
    }
}