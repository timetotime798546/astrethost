package com.gympro.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private TextView tvUserHeader, tvTotalMembers, tvTodayAttendance, tvTotalRevenue, tvTotalExpenses, tvNetProfit;
    private ProgressBar pbDashboard;
    private Button btnLogout, btnMembers, btnAttendance, btnPayments, btnExpenses, btnTrainers, btnRefresh;
    private BackendApi api;
    private AuthManager authManager;

    private double currentRevenue = 0.0;
    private double currentExpenses = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        authManager = new AuthManager(this);
        api = new BackendApi(this);

        tvUserHeader = findViewById(R.id.tvUserHeader);
        tvTotalMembers = findViewById(R.id.tvTotalMembers);
        tvTodayAttendance = findViewById(R.id.tvTodayAttendance);
        tvTotalRevenue = findViewById(R.id.tvTotalRevenue);
        tvTotalExpenses = findViewById(R.id.tvTotalExpenses);
        tvNetProfit = findViewById(R.id.tvNetProfit);
        pbDashboard = findViewById(R.id.pbDashboard);

        btnLogout = findViewById(R.id.btnLogout);
        btnMembers = findViewById(R.id.btnNavMembers);
        btnAttendance = findViewById(R.id.btnNavAttendance);
        btnPayments = findViewById(R.id.btnNavPayments);
        btnExpenses = findViewById(R.id.btnNavExpenses);
        btnTrainers = findViewById(R.id.btnNavTrainers);
        btnRefresh = findViewById(R.id.btnRefreshDashboard);

        tvUserHeader.setText("Gym Account: " + authManager.getUserEmail());

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                api.logout(new BackendApi.ApiCallback() {
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

        btnMembers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, MembersActivity.class));
            }
        });

        btnAttendance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AttendanceActivity.class));
            }
        });

        btnPayments.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, PaymentsActivity.class));
            }
        });

        btnExpenses.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, ExpensesActivity.class));
            }
        });

        btnTrainers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, TrainersActivity.class));
            }
        });

        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadDashboardData();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboardData();
    }

    private void loadDashboardData() {
        pbDashboard.setVisibility(View.VISIBLE);
        currentRevenue = 0.0;
        currentExpenses = 0.0;

        // Fetch Members count
        api.getCollection("members", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                JSONArray records = response.optJSONArray("records");
                int count = records != null ? records.length() : 0;
                tvTotalMembers.setText(String.valueOf(count));
            }

            @Override
            public void onError(String errorMessage) {}
        });

        // Fetch Today Attendance
        final String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        api.getCollection("attendance", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                JSONArray records = response.optJSONArray("records");
                int todayCount = 0;
                if (records != null) {
                    for (int i = 0; i < records.length(); i++) {
                        JSONObject r = records.optJSONObject(i);
                        if (r != null) {
                            JSONObject d = r.optJSONObject("data");
                            if (d != null && todayDate.equals(d.optString("date"))) {
                                todayCount++;
                            }
                        }
                    }
                }
                tvTodayAttendance.setText(String.valueOf(todayCount));
            }

            @Override
            public void onError(String errorMessage) {}
        });

        // Fetch Payments for Revenue Calculation
        api.getCollection("payments", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                JSONArray records = response.optJSONArray("records");
                double revSum = 0.0;
                if (records != null) {
                    for (int i = 0; i < records.length(); i++) {
                        JSONObject r = records.optJSONObject(i);
                        if (r != null) {
                            JSONObject d = r.optJSONObject("data");
                            if (d != null) {
                                revSum += d.optDouble("amount", 0.0);
                            }
                        }
                    }
                }
                currentRevenue = revSum;
                tvTotalRevenue.setText("$" + String.format(Locale.US, "%.2f", currentRevenue));
                calculateNetProfit();
            }

            @Override
            public void onError(String errorMessage) {}
        });

        // Fetch Expenses for Financial Calculation
        api.getCollection("expenses", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                pbDashboard.setVisibility(View.GONE);
                JSONArray records = response.optJSONArray("records");
                double expSum = 0.0;
                if (records != null) {
                    for (int i = 0; i < records.length(); i++) {
                        JSONObject r = records.optJSONObject(i);
                        if (r != null) {
                            JSONObject d = r.optJSONObject("data");
                            if (d != null) {
                                expSum += d.optDouble("amount", 0.0);
                            }
                        }
                    }
                }
                currentExpenses = expSum;
                tvTotalExpenses.setText("$" + String.format(Locale.US, "%.2f", currentExpenses));
                calculateNetProfit();
            }

            @Override
            public void onError(String errorMessage) {
                pbDashboard.setVisibility(View.GONE);
            }
        });
    }

    private void calculateNetProfit() {
        double net = currentRevenue - currentExpenses;
        tvNetProfit.setText("$" + String.format(Locale.US, "%.2f", net));
        if (net < 0) {
            tvNetProfit.setTextColor(0xFFFF5B64);
        } else {
            tvNetProfit.setTextColor(0xFFB8FF38);
        }
    }
}