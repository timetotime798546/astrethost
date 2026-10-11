package com.carepulseclinic.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.Locale;

public class BookAppointmentActivity extends Activity {

    private Spinner spinnerDoctor, spinnerTime;
    private EditText etPatientName, etPatientPhone, etDate, etNotes;
    private TextView tvBaseFee, tvTaxFee, tvTotalFee;
    private Button btnConfirmBooking;
    private ProgressBar progressBar;
    private BackendApi backendApi;

    private double currentBaseFee = 100.0;
    private static final double TAX_RATE = 0.10; // 10% clinic tax / dynamic logic

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_appointment);

        backendApi = new BackendApi(this);

        spinnerDoctor = findViewById(R.id.spinnerDoctor);
        spinnerTime = findViewById(R.id.spinnerTime);
        etPatientName = findViewById(R.id.etPatientName);
        etPatientPhone = findViewById(R.id.etPatientPhone);
        etDate = findViewById(R.id.etAppointmentDate);
        etNotes = findViewById(R.id.etNotes);
        tvBaseFee = findViewById(R.id.tvBaseFee);
        tvTaxFee = findViewById(R.id.tvTaxFee);
        tvTotalFee = findViewById(R.id.tvTotalFee);
        btnConfirmBooking = findViewById(R.id.btnConfirmBooking);
        progressBar = findViewById(R.id.bookingProgressBar);

        setupSpinners();
        recalculateFees();

        // Preset parameters from Main activity if user tapped a specific doctor card
        String presetDoctor = getIntent().getStringExtra("preset_doctor");
        if (presetDoctor != null) {
            selectDoctorInSpinner(presetDoctor);
        }

        btnConfirmBooking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveAppointment();
            }
        });
    }

    private void setupSpinners() {
        String[] doctors = new String[]{
            "Dr. Sarah Jenkins - Cardiology ($150.00)",
            "Dr. Marcus Vance - General Practice ($80.00)",
            "Dr. Elena Rostova - Dermatology ($120.00)",
            "Dr. Aris Thorne - Pediatrics ($95.00)"
        };

        ArrayAdapter<String> docAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, doctors);
        docAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDoctor.setAdapter(docAdapter);

        spinnerDoctor.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0: currentBaseFee = 150.0; break;
                    case 1: currentBaseFee = 80.0; break;
                    case 2: currentBaseFee = 120.0; break;
                    case 3: currentBaseFee = 95.0; break;
                    default: currentBaseFee = 100.0; break;
                }
                recalculateFees();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        String[] times = new String[]{"09:00 AM", "10:30 AM", "01:00 PM", "02:30 PM", "04:00 PM"};
        ArrayAdapter<String> timeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, times);
        timeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTime.setAdapter(timeAdapter);
    }

    private void selectDoctorInSpinner(String doctorName) {
        for (int i = 0; i < spinnerDoctor.getCount(); i++) {
            if (spinnerDoctor.getItemAtPosition(i).toString().contains(doctorName)) {
                spinnerDoctor.setSelection(i);
                break;
            }
        }
    }

    // MANDATORY NATIVE OFFLINE CALCULATION IMPLEMENTATION
    private void recalculateFees() {
        double fee = currentBaseFee;
        double tax = fee * TAX_RATE;
        double total = fee + tax; // formula: fee + tax

        tvBaseFee.setText(String.format(Locale.US, "$%.2f", fee));
        tvTaxFee.setText(String.format(Locale.US, "$%.2f", tax));
        tvTotalFee.setText(String.format(Locale.US, "$%.2f", total));
    }

    private void saveAppointment() {
        String patientName = etPatientName.getText().toString().trim();
        String patientPhone = etPatientPhone.getText().toString().trim();
        String date = etDate.getText().toString().trim();
        String notes = etNotes.getText().toString().trim();

        if (patientName.isEmpty() || patientPhone.isEmpty() || date.isEmpty()) {
            Toast.makeText(this, "Please complete patient name, phone, and date", Toast.LENGTH_SHORT).show();
            return;
        }

        String fullDocStr = spinnerDoctor.getSelectedItem().toString();
        String[] parts = fullDocStr.split(" - ");
        String doctorName = parts[0];
        String serviceName = parts.length > 1 ? parts[1].split(" \\(")[0] : "Consultation";
        String time = spinnerTime.getSelectedItem().toString();

        double fee = currentBaseFee;
        double tax = fee * TAX_RATE;
        double total = fee + tax; // local calculation

        progressBar.setVisibility(View.VISIBLE);
        btnConfirmBooking.setEnabled(false);

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
            data.put("total_fee", total);
            data.put("status", "Confirmed");

            backendApi.createAppointment(data, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    progressBar.setVisibility(View.GONE);
                    btnConfirmBooking.setEnabled(true);
                    Toast.makeText(BookAppointmentActivity.this, "Appointment scheduled successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onError(String errorMessage) {
                    progressBar.setVisibility(View.GONE);
                    btnConfirmBooking.setEnabled(true);
                    Toast.makeText(BookAppointmentActivity.this, "Failed to book: " + errorMessage, Toast.LENGTH_LONG).show();
                }
            });

        } catch (Exception e) {
            progressBar.setVisibility(View.GONE);
            btnConfirmBooking.setEnabled(true);
            Toast.makeText(this, "Error building request: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}