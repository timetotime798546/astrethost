package com.carepulseclinic.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.Locale;

public class AppointmentDetailActivity extends Activity {

    private String recordId, doctorName, serviceName, patientName, patientPhone, date, time, notes, status;
    private double fee, tax, totalFee;

    private TextView tvDetailDoc, tvDetailService, tvDetailPatient, tvDetailPhone, tvDetailDateTime, tvDetailNotes, tvDetailFee, tvDetailTax, tvDetailTotal, tvDetailStatus;
    private Button btnCancelAppointment, btnDeleteAppointment;
    private ProgressBar progressBar;
    private BackendApi backendApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointment_detail);

        backendApi = new BackendApi(this);

        recordId = getIntent().getStringExtra("id");
        doctorName = getIntent().getStringExtra("doctor_name");
        serviceName = getIntent().getStringExtra("service_name");
        patientName = getIntent().getStringExtra("patient_name");
        patientPhone = getIntent().getStringExtra("patient_phone");
        date = getIntent().getStringExtra("date");
        time = getIntent().getStringExtra("time");
        notes = getIntent().getStringExtra("notes");
        fee = getIntent().getDoubleExtra("fee", 0.0);
        tax = getIntent().getDoubleExtra("tax", 0.0);
        totalFee = getIntent().getDoubleExtra("total_fee", fee + tax);
        status = getIntent().getStringExtra("status");

        tvDetailDoc = findViewById(R.id.tvDetailDoc);
        tvDetailService = findViewById(R.id.tvDetailService);
        tvDetailPatient = findViewById(R.id.tvDetailPatient);
        tvDetailPhone = findViewById(R.id.tvDetailPhone);
        tvDetailDateTime = findViewById(R.id.tvDetailDateTime);
        tvDetailNotes = findViewById(R.id.tvDetailNotes);
        tvDetailFee = findViewById(R.id.tvDetailFee);
        tvDetailTax = findViewById(R.id.tvDetailTax);
        tvDetailTotal = findViewById(R.id.tvDetailTotal);
        tvDetailStatus = findViewById(R.id.tvDetailStatus);

        btnCancelAppointment = findViewById(R.id.btnCancelAppointment);
        btnDeleteAppointment = findViewById(R.id.btnDeleteAppointment);
        progressBar = findViewById(R.id.detailProgressBar);

        populateDetails();

        btnCancelAppointment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelAppointment();
            }
        });

        btnDeleteAppointment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteAppointment();
            }
        });
    }

    private void populateDetails() {
        tvDetailDoc.setText(doctorName);
        tvDetailService.setText("Department: " + serviceName);
        tvDetailPatient.setText(patientName);
        tvDetailPhone.setText("Phone: " + (patientPhone == null || patientPhone.isEmpty() ? "N/A" : patientPhone));
        tvDetailDateTime.setText(date + " at " + time);
        tvDetailNotes.setText(notes == null || notes.isEmpty() ? "No additional medical notes" : notes);

        tvDetailFee.setText(String.format(Locale.US, "$%.2f", fee));
        tvDetailTax.setText(String.format(Locale.US, "$%.2f", tax));
        tvDetailTotal.setText(String.format(Locale.US, "$%.2f", totalFee));
        tvDetailStatus.setText(status);

        if ("Cancelled".equalsIgnoreCase(status)) {
            btnCancelAppointment.setVisibility(View.GONE);
        }
    }

    private void cancelAppointment() {
        progressBar.setVisibility(View.VISIBLE);
        btnCancelAppointment.setEnabled(false);

        try {
            JSONObject data = new JSONObject();
            data.put("doctor_name", doctorName);
            data.put("service_name", serviceName);
            data.put("patient_name", patientName);
            data.put("patient_phone", patientPhone);
            data.put("appointment_date", date);
            data.put("appointment_time", time);
            data.put("notes", notes);
            data.put("fee", fee);
            data.put("tax", tax);
            data.put("total_fee", totalFee);
            data.put("status", "Cancelled");

            backendApi.updateAppointment(recordId, data, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    progressBar.setVisibility(View.GONE);
                    btnCancelAppointment.setEnabled(true);
                    Toast.makeText(AppointmentDetailActivity.this, "Appointment marked as Cancelled", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onError(String errorMessage) {
                    progressBar.setVisibility(View.GONE);
                    btnCancelAppointment.setEnabled(true);
                    Toast.makeText(AppointmentDetailActivity.this, "Error cancelling: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            progressBar.setVisibility(View.GONE);
            btnCancelAppointment.setEnabled(true);
            Toast.makeText(this, "Error: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteAppointment() {
        progressBar.setVisibility(View.VISIBLE);
        btnDeleteAppointment.setEnabled(false);

        backendApi.deleteAppointment(recordId, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AppointmentDetailActivity.this, "Appointment record deleted", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                btnDeleteAppointment.setEnabled(true);
                Toast.makeText(AppointmentDetailActivity.this, "Error deleting: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
}