package com.patiententrymanager.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
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

public class MainActivity extends Activity {

    private EditText etSearch;
    private ListView lvPatients;
    private TextView tvEmptyState, tvUserEmail;
    private Button btnAddPatient, btnLogout, btnRefresh;
    private ProgressBar progressBar;

    private BackendApi backendApi;
    private PatientAdapter adapter;
    private List<Patient> patientList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        backendApi = new BackendApi(this);

        etSearch = (EditText) findViewById(R.id.etSearch);
        lvPatients = (ListView) findViewById(R.id.lvPatients);
        tvEmptyState = (TextView) findViewById(R.id.tvEmptyState);
        tvUserEmail = (TextView) findViewById(R.id.tvUserEmail);
        btnAddPatient = (Button) findViewById(R.id.btnAddPatient);
        btnLogout = (Button) findViewById(R.id.btnLogout);
        btnRefresh = (Button) findViewById(R.id.btnRefresh);
        progressBar = (ProgressBar) findViewById(R.id.mainProgressBar);

        tvUserEmail.setText("Logged in: " + backendApi.getUserEmail());

        patientList = new ArrayList<Patient>();
        adapter = new PatientAdapter(this, patientList);
        lvPatients.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        lvPatients.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Patient selectedPatient = adapter.getItem(position);
                Intent intent = new Intent(MainActivity.this, PatientDetailActivity.class);
                intent.putExtra("patient", selectedPatient);
                startActivity(intent);
            }
        });

        btnAddPatient.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AddEditPatientActivity.class));
            }
        });

        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchPatients();
            }
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                backendApi.logout(new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        startActivity(new Intent(MainActivity.this, LoginActivity.class));
                        finish();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        startActivity(new Intent(MainActivity.this, LoginActivity.class));
                        finish();
                    }
                });
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchPatients();
    }

    private void fetchPatients() {
        setLoading(true);
        backendApi.getRecords("patients", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                setLoading(false);
                patientList.clear();
                try {
                    JSONArray records = response.optJSONArray("records");
                    if (records != null) {
                        for (int i = 0; i < records.length(); i++) {
                            JSONObject recordObj = records.getJSONObject(i);
                            Patient p = Patient.fromJsonRecord(recordObj);
                            if (p != null) {
                                patientList.add(p);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                adapter.updateData(patientList);
                if (patientList.isEmpty()) {
                    tvEmptyState.setVisibility(View.VISIBLE);
                    lvPatients.setVisibility(View.GONE);
                } else {
                    tvEmptyState.setVisibility(View.GONE);
                    lvPatients.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(String errorMessage) {
                setLoading(false);
                Toast.makeText(MainActivity.this, "Error fetching records: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}