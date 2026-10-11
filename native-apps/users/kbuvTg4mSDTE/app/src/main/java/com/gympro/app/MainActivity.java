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

    private TextView tvUserHeader;
    private Button btnLogout;
    private ProgressBar pbDashboard;
    private TextView tvTotalMembers;
    private TextView tvTodayAttendance;
    private TextView tvTotalRevenue;
    private TextView tvTotalExpenses;
    private TextView tvNetProfit;

    private Button btnNavMembers;
    private Button btnNavAttendance;
    private Button btnNavPayments;
    private Button btnNavExpenses;
    private Button btnNavTrainers;
    private Button btnRefreshDashboard;

    private AuthManager authManager;
    private BackendApi backendApi;

    private double totalRevenue = 0.0;
    private double totalExpenses = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        authManager = new AuthManager(this);
        backendApi = new BackendApi(this);

        tvUserHeader = (TextView) findViewById(R.id.tvUserHeader);
        btnLogout = (Button) findViewById(R.id.btnLogout);
        pbDashboard = (ProgressBar) findViewById(R.id.pbDashboard);
        tvTotalMembers = (TextView) findViewById(R.id.tvTotalMembers);
        tvTodayAttendance = (TextView) findViewById(R.id.tvTodayAttendance);
        tvTotalRevenue = (TextView) findViewById(R.id.tvTotalRevenue);
        tvTotalExpenses = (TextView) findViewById(R.id.tvTotalExpenses);
        tvNetProfit = (TextView) findViewById(R.id.tvNetProfit);

        btnNavMembers = (Button) findViewById(R.id.btnNavMembers);
        btnNavAttendance = (Button) findViewById(R.id.btnNavAttendance);
        btnNavPayments = (Button) findViewById(R.id.btnNavPayments);
        btnNavExpenses = (Button) findViewById(R.id.btnNavExpenses);
        btnNavTrainers = (Button) findViewById(R.id.btnNavTrainers);
        btnRefreshDashboard = (Button) findViewById(R.id.btnRefreshDashboard);

        tvUserHeader.setText("Owner: " + authManager.getUserEmail());

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

        btnNavMembers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, MembersActivity.class));
            }
        });

        btnNavAttendance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AttendanceActivity.class));
            }
        });

        btnNavPayments.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, PaymentsActivity.class));
            }
        });

        btnNavExpenses.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, ExpensesActivity.class));
            }
        });

        btnNavTrainers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, TrainersActivity.class));
            }
        });

        btnRefreshDashboard.setOnClickListener(new View.OnClickListener() {
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
        totalRevenue = 0.0;
        totalExpenses = 0.0;

        // Fetch Members
        backendApi.getCollection("members", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                JSONArray records = response.optJSONArray("records");
                int count = (records != null) ? records.length() : 0;
                tvTotalMembers.setText(String.valueOf(count));
            }

            @Override
            public void onError(String errorMessage) {
                tvTotalMembers.setText("0");
            }
        });

        // Fetch Today Attendance
        final String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        backendApi.getCollection("attendance", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                JSONArray records = response.optJSONArray("records");
                int todayCount = 0;
                if (records != null) {
                    for (int i = 0; i < records.length(); i++) {
                        JSONObject rec = records.optJSONObject(i);
                        if (rec != null) {
                            JSONObject data = rec.optJSONObject("data");
                            if (data != null && today.equals(data.optString("date"))) {
                                todayCount++;
                            }
                        }
                    }
                }
                tvTodayAttendance.setText(String.valueOf(todayCount));
            }

            @Override
            public void onError(String errorMessage) {
                tvTodayAttendance.setText("0");
            }
        });

        // Fetch Payments for Revenue calculation
        backendApi.getCollection("payments", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                JSONArray records = response.optJSONArray("records");
                double rev = 0.0;
                if (records != null) {
                    for (int i = 0; i < records.length(); i++) {
                        JSONObject rec = records.optJSONObject(i);
                        if (rec != null) {
                            JSONObject data = rec.optJSONObject("data");
                            if (data != null) {
                                rev += data.optDouble("amount", 0.0);
                            }
                        }
                    }
                }
                totalRevenue = rev;
                tvTotalRevenue.setText(String.format(Locale.US, "$%.2f", totalRevenue));
                calculateNetProfit();
            }

            @Override
            public void onError(String errorMessage) {
                tvTotalRevenue.setText("$0.00");
                calculateNetProfit();
            }
        });

        // Fetch Expenses
        backendApi.getCollection("expenses", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                pbDashboard.setVisibility(View.GONE);
                JSONArray records = response.optJSONArray("records");
                double exp = 0.0;
                if (records != null) {
                    for (int i = 0; i < records.length(); i++) {
                        JSONObject rec = records.optJSONObject(i);
                        if (rec != null) {
                            JSONObject data = rec.optJSONObject("data");
                            if (data != null) {
                                exp += data.optDouble("amount", 0.0);
                            }
                        }
                    }
                }
                totalExpenses = exp;
                tvTotalExpenses.setText(String.format(Locale.US, "$%.2f", totalExpenses));
                calculateNetProfit();
            }

            @Override
            public void onError(String errorMessage) {
                pbDashboard.setVisibility(View.GONE);
                tvTotalExpenses.setText("$0.00");
                calculateNetProfit();
            }
        });
    }

    // MANDATORY OFFLINE CALCULATION inside Android Java
    private void calculateNetProfit() {
        double netProfit = totalRevenue - totalExpenses;
        tvNetProfit.setText(String.format(Locale.US, "$%.2f", netProfit));
        if (netProfit < 0) {
            tvNetProfit.setTextColor(0xFFFF5B64);
        } else {
            tvNetProfit.setTextColor(0xFFB8FF38);
        }
    }
}