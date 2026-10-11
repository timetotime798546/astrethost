package com.patiententrymanager.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

public class PatientDetailActivity extends Activity {

    private TextView tvName, tvAgeGender, tvPhone, tvVisitDate, tvNotes, tvNoPhoto;
    private ImageView ivPhoto;
    private Button btnEdit, btnDelete;
    private ProgressBar progressBar;

    private BackendApi backendApi;
    private ImageWorkerApi imageWorkerApi;
    private Patient patient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_detail);

        backendApi = new BackendApi(this);
        imageWorkerApi = new ImageWorkerApi(this, backendApi);

        tvName = (TextView) findViewById(R.id.tvDetailName);
        tvAgeGender = (TextView) findViewById(R.id.tvDetailAgeGender);
        tvPhone = (TextView) findViewById(R.id.tvDetailPhone);
        tvVisitDate = (TextView) findViewById(R.id.tvDetailVisitDate);
        tvNotes = (TextView) findViewById(R.id.tvDetailNotes);
        tvNoPhoto = (TextView) findViewById(R.id.tvNoPhoto);
        ivPhoto = (ImageView) findViewById(R.id.ivDetailPhoto);
        btnEdit = (Button) findViewById(R.id.btnEditPatient);
        btnDelete = (Button) findViewById(R.id.btnDeletePatient);
        progressBar = (ProgressBar) findViewById(R.id.detailProgressBar);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("patient")) {
            patient = (Patient) intent.getSerializableExtra("patient");
            displayPatientDetails();
        } else {
            Toast.makeText(this, "Patient details not found", Toast.LENGTH_SHORT).show();
            finish();
        }

        btnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent editIntent = new Intent(PatientDetailActivity.this, AddEditPatientActivity.class);
                editIntent.putExtra("patient", patient);
                startActivity(editIntent);
                finish();
            }
        });

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmDeletion();
            }
        });
    }

    private void displayPatientDetails() {
        tvName.setText(patient.getName());
        tvAgeGender.setText("Age: " + patient.getAge() + "  |  Gender: " + patient.getGender());
        tvPhone.setText("Phone: " + patient.getPhone());
        tvVisitDate.setText("Visit Date: " + patient.getVisitDate());
        tvNotes.setText(TextUtils.isEmpty(patient.getNotes()) ? "No notes recorded." : patient.getNotes());

        if (patient.getImageId() != null && !patient.getImageId().trim().isEmpty()) {
            tvNoPhoto.setVisibility(View.GONE);
            ivPhoto.setVisibility(View.VISIBLE);
            setLoading(true);
            imageWorkerApi.fetchImage(patient.getImageId(), new ImageWorkerApi.ImageFetchCallback() {
                @Override
                public void onSuccess(Bitmap bitmap) {
                    setLoading(false);
                    ivPhoto.setImageBitmap(bitmap);
                }

                @Override
                public void onError(String errorMessage) {
                    setLoading(false);
                    tvNoPhoto.setText("Failed to load attached photo.");
                    tvNoPhoto.setVisibility(View.VISIBLE);
                    ivPhoto.setVisibility(View.GONE);
                }
            });
        } else {
            ivPhoto.setVisibility(View.GONE);
            tvNoPhoto.setVisibility(View.VISIBLE);
        }
    }

    private void confirmDeletion() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Patient Record")
                .setMessage("Are you sure you want to delete " + patient.getName() + "? This action cannot be undone.")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deletePatientRecord();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deletePatientRecord() {
        setLoading(true);
        if (patient.getImageId() != null && !patient.getImageId().trim().isEmpty()) {
            imageWorkerApi.deleteImage(patient.getImageId(), null);
        }

        backendApi.deleteRecord(patient.getId(), new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                setLoading(false);
                Toast.makeText(PatientDetailActivity.this, "Patient record deleted.", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String errorMessage) {
                setLoading(false);
                Toast.makeText(PatientDetailActivity.this, "Error deleting record: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}