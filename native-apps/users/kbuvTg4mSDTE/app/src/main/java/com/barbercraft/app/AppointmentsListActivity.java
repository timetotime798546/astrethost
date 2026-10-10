package com.barbercraft.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
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

public class AppointmentsListActivity extends Activity {

    private BackendApi backendApi;
    private LinearLayout appointmentsContainer;
    private ProgressBar pbLoading;
    private TextView tvNoAppointments;
    private TextView tvTotalSpendingCalc;
    private TextView tvCountAppointments;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointments_list);

        backendApi = new BackendApi(this);

        Button btnBack = (Button) findViewById(R.id.btnAppointmentsBack);
        Button btnRefresh = (Button) findViewById(R.id.btnRefreshAppointments);
        appointmentsContainer = (LinearLayout) findViewById(R.id.appointmentsContainer);
        pbLoading = (ProgressBar) findViewById(R.id.pbAppointmentsLoading);
        tvNoAppointments = (TextView) findViewById(R.id.tvNoAppointments);
        tvTotalSpendingCalc = (TextView) findViewById(R.id.tvTotalSpendingCalc);
        tvCountAppointments = (TextView) findViewById(R.id.tvCountAppointments);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadAppointments();
            }
        });

        loadAppointments();
    }

    private void loadAppointments() {
        pbLoading.setVisibility(View.VISIBLE);
        tvNoAppointments.setVisibility(View.GONE);
        appointmentsContainer.removeAllViews();

        backendApi.readData("appointments", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                pbLoading.setVisibility(View.GONE);
                JSONArray records = response.optJSONArray("records");
                if (records == null || records.length() == 0) {
                    tvNoAppointments.setVisibility(View.VISIBLE);
                    tvTotalSpendingCalc.setText("Total Booked Value: $0.00");
                    tvCountAppointments.setText("0 appointments");
                    return;
                }

                tvNoAppointments.setVisibility(View.GONE);
                renderAppointments(records);
            }

            @Override
            public void onError(String errorMessage) {
                pbLoading.setVisibility(View.GONE);
                Toast.makeText(AppointmentsListActivity.this, "Failed to load bookings: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void renderAppointments(JSONArray records) {
        appointmentsContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        double[] totals = new double[records.length()];

        for (int i = 0; i < records.length(); i++) {
            try {
                JSONObject item = records.getJSONObject(i);
                final String recordId = item.optString("id", "");
                JSONObject data = item.optJSONObject("data");
                if (data == null) continue;

                String serviceName = data.optString("service_name", "Service");
                String customerName = data.optString("customer_name", "");
                String phone = data.optString("customer_phone", "");
                String date = data.optString("appointment_date", "");
                String time = data.optString("appointment_time", "");
                String barber = data.optString("barber_name", "");
                double total = data.optDouble("total_amount", 0.0);
                totals[i] = total;

                View card = inflater.inflate(R.layout.item_appointment, appointmentsContainer, false);
                TextView tvServiceName = (TextView) card.findViewById(R.id.tvItemServiceName);
                TextView tvTotal = (TextView) card.findViewById(R.id.tvItemTotal);
                TextView tvDetails = (TextView) card.findViewById(R.id.tvItemDetails);
                TextView tvClient = (TextView) card.findViewById(R.id.tvItemClient);
                Button btnCancel = (Button) card.findViewById(R.id.btnCancelAppointment);

                tvServiceName.setText(serviceName);
                tvTotal.setText("$" + String.format("%.2f", total));
                tvDetails.setText(date + " at " + time + " | Barber: " + barber);
                tvClient.setText("Client: " + customerName + " (" + phone + ")");

                btnCancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        confirmCancellation(recordId);
                    }
                });

                appointmentsContainer.addView(card);

            } catch (Exception ignored) {
            }
        }

        // Offline deterministic calculation of cumulative booked total
        double sum = CalculationHelper.sumAppointmentsTotal(totals);
        tvTotalSpendingCalc.setText("Total Booked Value: $" + String.format("%.2f", sum));
        tvCountAppointments.setText(records.length() + " appointments");
    }

    private void confirmCancellation(final String recordId) {
        new AlertDialog.Builder(this)
                .setTitle("Cancel Appointment")
                .setMessage("Are you sure you want to cancel this booking?")
                .setPositiveButton("Yes, Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteAppointment(recordId);
                    }
                })
                .setNegativeButton("Keep", null)
                .show();
    }

    private void deleteAppointment(String recordId) {
        pbLoading.setVisibility(View.VISIBLE);
        backendApi.deleteData(recordId, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                Toast.makeText(AppointmentsListActivity.this, "Booking cancelled successfully", Toast.LENGTH_SHORT).show();
                loadAppointments();
            }

            @Override
            public void onError(String errorMessage) {
                pbLoading.setVisibility(View.GONE);
                Toast.makeText(AppointmentsListActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
}