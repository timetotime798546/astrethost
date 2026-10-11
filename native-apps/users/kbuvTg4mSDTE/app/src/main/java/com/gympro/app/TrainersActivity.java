package com.gympro.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class TrainersActivity extends Activity {

    private EditText etTrainerName;
    private EditText etTrainerSpecialty;
    private EditText etTrainerSalary;
    private Button btnSaveTrainer;
    private ProgressBar pbTrainers;
    private ListView lvTrainers;

    private BackendApi backendApi;
    private List<String> trainerItems;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trainers);

        backendApi = new BackendApi(this);

        etTrainerName = (EditText) findViewById(R.id.etTrainerName);
        etTrainerSpecialty = (EditText) findViewById(R.id.etTrainerSpecialty);
        etTrainerSalary = (EditText) findViewById(R.id.etTrainerSalary);
        btnSaveTrainer = (Button) findViewById(R.id.btnSaveTrainer);
        pbTrainers = (ProgressBar) findViewById(R.id.pbTrainers);
        lvTrainers = (ListView) findViewById(R.id.lvTrainers);

        trainerItems = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, trainerItems);
        lvTrainers.setAdapter(adapter);

        btnSaveTrainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addTrainer();
            }
        });

        loadTrainers();
    }

    private void loadTrainers() {
        pbTrainers.setVisibility(View.VISIBLE);
        backendApi.getCollection("trainers", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                pbTrainers.setVisibility(View.GONE);
                trainerItems.clear();
                JSONArray records = response.optJSONArray("records");
                if (records != null) {
                    for (int i = 0; i < records.length(); i++) {
                        JSONObject rec = records.optJSONObject(i);
                        if (rec != null) {
                            JSONObject data = rec.optJSONObject("data");
                            if (data != null) {
                                String name = data.optString("name", "Trainer");
                                String specialty = data.optString("specialty", "Fitness");
                                double salary = data.optDouble("salary", 0.0);
                                trainerItems.add(name + " (" + specialty + ")\nSalary: $" + salary);
                            }
                        }
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onError(String errorMessage) {
                pbTrainers.setVisibility(View.GONE);
                Toast.makeText(TrainersActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addTrainer() {
        String name = etTrainerName.getText().toString().trim();
        String specialty = etTrainerSpecialty.getText().toString().trim();
        String salaryStr = etTrainerSalary.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "Trainer name is required", Toast.LENGTH_SHORT).show();
            return;
        }

        double salary = 0.0;
        try {
            salary = Double.parseDouble(salaryStr);
        } catch (Exception e) {
            salary = 0.0;
        }

        pbTrainers.setVisibility(View.VISIBLE);
        btnSaveTrainer.setEnabled(false);

        try {
            JSONObject data = new JSONObject();
            data.put("name", name);
            data.put("phone", "");
            data.put("specialty", specialty.isEmpty() ? "General Fitness" : specialty);
            data.put("salary", salary);

            backendApi.createRecord("trainers", data, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    pbTrainers.setVisibility(View.GONE);
                    btnSaveTrainer.setEnabled(true);
                    etTrainerName.setText("");
                    etTrainerSpecialty.setText("");
                    etTrainerSalary.setText("");
                    Toast.makeText(TrainersActivity.this, "Trainer added successfully", Toast.LENGTH_SHORT).show();
                    loadTrainers();
                }

                @Override
                public void onError(String errorMessage) {
                    pbTrainers.setVisibility(View.GONE);
                    btnSaveTrainer.setEnabled(true);
                    Toast.makeText(TrainersActivity.this, "Failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            pbTrainers.setVisibility(View.GONE);
            btnSaveTrainer.setEnabled(true);
        }
    }
}