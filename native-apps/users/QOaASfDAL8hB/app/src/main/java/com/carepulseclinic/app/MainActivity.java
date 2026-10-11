package com.carepulseclinic.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private TextView tvUserHeader, tvTotalBookings, tvTotalRevenue, tvAvgFee;
    private Button btnBookAppointment, btnLogout;
    private LinearLayout containerAppointments, containerDoctors;
    private ProgressBar progressBar;
    private BackendApi backendApi;

    public static class AppointmentModel {
        public String id;
        public String doctorName;
        public String serviceName;
        public String patientName;
        public String patientPhone;
        public String date;
        public String time;
        public String notes;
        public double fee;
        public double tax;
        public double totalFee;
        public String status;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        backendApi = new BackendApi(this);

        tvUserHeader = findViewById(R.id.tvUserHeader);
        tvTotalBookings = findViewById(R.id.tvTotalBookings);
        tvTotalRevenue = findViewById(R.id.tvTotalRevenue);
        tvAvgFee = findViewById(R.id.tvAvgFee);
        btnBookAppointment = findViewById(R.id.btnBookAppointment);
        btnLogout = findViewById(R.id.btnLogout);
        containerAppointments = findViewById(R.id.containerAppointments);
        containerDoctors = findViewById(R.id.containerDoctors);
        progressBar = findViewById(R.id.mainProgressBar);

        String email = backendApi.getUserEmail();
        tvUserHeader.setText("Welcome, " + (email.isEmpty() ? "Patient" : email));

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                backendApi.clearAuth();
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                finish();
            }
        });

        btnBookAppointment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, BookAppointmentActivity.class));
            }
        });

        setupDoctorList();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAppointments();
    }

    private void setupDoctorList() {
        containerDoctors.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        String[][] doctors = new String[][]{
            {"Dr. Sarah Jenkins", "Cardiology & Heart Care", "$150.00"},
            {"Dr. Marcus Vance", "General Practice & Wellness", "$80.00"},
            {"Dr. Elena Rostova", "Dermatology & Skin Care", "$120.00"},
            {"Dr. Aris Thorne", "Pediatrics Specialist", "$95.00"}
        };

        for (final String[] doc : doctors) {
            View itemView = inflater.inflate(R.layout.item_doctor_service, containerDoctors, false);
            TextView tvName = itemView.findViewById(R.id.tvDoctorName);
            TextView tvSpec = itemView.findViewById(R.id.tvDoctorSpec);
            TextView tvFee = itemView.findViewById(R.id.tvDoctorFee);
            Button btnSelect = itemView.findViewById(R.id.btnSelectDoctor);

            tvName.setText(doc[0]);
            tvSpec.setText(doc[1]);
            tvFee.setText(doc[2]);

            btnSelect.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MainActivity.this, BookAppointmentActivity.class);
                    intent.putExtra("preset_doctor", doc[0]);
                    intent.putExtra("preset_service", doc[1]);
                    startActivity(intent);
                }
            });

            containerDoctors.addView(itemView);
        }
    }

    private void loadAppointments() {
        progressBar.setVisibility(View.VISIBLE);
        containerAppointments.removeAllViews();

        backendApi.getAppointments(new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                progressBar.setVisibility(View.GONE);
                try {
                    JSONArray records = response.optJSONArray("records");
                    List<AppointmentModel> list = new ArrayList<>();

                    double aggregateRevenue = 0.0;
                    int activeCount = 0;

                    if (records != null) {
                        for (int i = 0; i < records.length(); i++) {
                            JSONObject rec = records.getJSONObject(i);
                            String id = rec.optString("id", "");
                            JSONObject data = rec.optJSONObject("data");

                            if (data != null) {
                                AppointmentModel item = new AppointmentModel();
                                item.id = id;
                                item.doctorName = data.optString("doctor_name", "General Practitioner");
                                item.serviceName = data.optString("service_name", "Consultation");
                                item.patientName = data.optString("patient_name", "Patient");
                                item.patientPhone = data.optString("patient_phone", "");
                                item.date = data.optString("appointment_date", "TBD");
                                item.time = data.optString("appointment_time", "TBD");
                                item.notes = data.optString("notes", "");
                                item.fee = data.optDouble("fee", 0.0);
                                item.tax = data.optDouble("tax", 0.0);

                                // MANDATORY NATIVE OFFLINE CALCULATION
                                item.totalFee = item.fee + item.tax;
                                item.status = data.optString("status", "Confirmed");

                                list.add(item);

                                // Accumulate statistics for offline logic calculation
                                aggregateRevenue += item.totalFee;
                                activeCount++;
                            }
                        }
                    }

                    // Display computed offline dashboard calculations
                    tvTotalBookings.setText(String.valueOf(activeCount));
                    tvTotalRevenue.setText(String.format(Locale.US, "$%.2f", aggregateRevenue));
                    double avg = activeCount > 0 ? (aggregateRevenue / activeCount) : 0.0;
                    tvAvgFee.setText(String.format(Locale.US, "$%.2f", avg));

                    renderAppointmentsUI(list);

                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Parsing error: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, "Error loading bookings: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderAppointmentsUI(List<AppointmentModel> list) {
        if (list.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("No upcoming clinic appointments yet.\nTap 'Book Appointment' to schedule one!");
            tvEmpty.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            tvEmpty.setPadding(32, 32, 32, 32);
            tvEmpty.setTextColor(Color.GRAY);
            containerAppointments.addView(tvEmpty);
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        for (final AppointmentModel appt : list) {
            View itemView = inflater.inflate(R.layout.item_appointment, containerAppointments, false);

            TextView tvDocName = itemView.findViewById(R.id.tvItemDoctorName);
            TextView tvServName = itemView.findViewById(R.id.tvItemService);
            TextView tvDateTime = itemView.findViewById(R.id.tvItemDateTime);
            TextView tvPatient = itemView.findViewById(R.id.tvItemPatient);
            TextView tvTotal = itemView.findViewById(R.id.tvItemTotal);
            TextView tvStatus = itemView.findViewById(R.id.tvItemStatus);

            tvDocName.setText(appt.doctorName);
            tvServName.setText(appt.serviceName);
            tvDateTime.setText(appt.date + " at " + appt.time);
            tvPatient.setText("Patient: " + appt.patientName);
            tvTotal.setText(String.format(Locale.US, "$%.2f", appt.totalFee));
            tvStatus.setText(appt.status);

            if ("Cancelled".equalsIgnoreCase(appt.status)) {
                tvStatus.setTextColor(Color.RED);
            } else {
                tvStatus.setTextColor(Color.parseColor("#00796B"));
            }

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MainActivity.this, AppointmentDetailActivity.class);
                    intent.putExtra("id", appt.id);
                    intent.putExtra("doctor_name", appt.doctorName);
                    intent.putExtra("service_name", appt.serviceName);
                    intent.putExtra("patient_name", appt.patientName);
                    intent.putExtra("patient_phone", appt.patientPhone);
                    intent.putExtra("date", appt.date);
                    intent.putExtra("time", appt.time);
                    intent.putExtra("notes", appt.notes);
                    intent.putExtra("fee", appt.fee);
                    intent.putExtra("tax", appt.tax);
                    intent.putExtra("total_fee", appt.totalFee);
                    intent.putExtra("status", appt.status);
                    startActivity(intent);
                }
            });

            containerAppointments.addView(itemView);
        }
    }
}