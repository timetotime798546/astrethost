package com.expensetracker.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ReportsActivity extends Activity {

    private Spinner spReportMonth;
    private TextView tvNetSavings;
    private TextView tvIncomeSub;
    private TextView tvExpenseSub;
    private LinearLayout llCategoryContainer;
    private TextView btnBack;

    private BackendApi backendApi;
    private SessionManager sessionManager;
    private final List<TransactionModel> allTransactions = new ArrayList<TransactionModel>();
    private final List<String> availableMonths = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        backendApi = new BackendApi(this);
        sessionManager = new SessionManager(this);

        spReportMonth = (Spinner) findViewById(R.id.spReportMonth);
        tvNetSavings = (TextView) findViewById(R.id.tvReportNetSavings);
        tvIncomeSub = (TextView) findViewById(R.id.tvReportIncomeSub);
        tvExpenseSub = (TextView) findViewById(R.id.tvReportExpenseSub);
        llCategoryContainer = (LinearLayout) findViewById(R.id.llCategoryReportContainer);
        btnBack = (TextView) findViewById(R.id.btnBackFromReports);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        loadTransactionsForReports();
    }

    private void loadTransactionsForReports() {
        backendApi.getTransactions(sessionManager.getToken(), new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                allTransactions.clear();
                JSONArray records = response.optJSONArray("records");
                Set<String> monthSet = new HashSet<String>();

                if (records != null) {
                    for (int i = 0; i < records.length(); i++) {
                        JSONObject rec = records.optJSONObject(i);
                        if (rec != null) {
                            String id = rec.optString("id");
                            JSONObject data = rec.optJSONObject("data");
                            if (data != null) {
                                String title = data.optString("title", "Untitled");
                                double amount = data.optDouble("amount", 0.0);
                                String type = data.optString("type", "expense");
                                String category = data.optString("category", "General");
                                String date = data.optString("date", "");
                                String note = data.optString("note", "");

                                allTransactions.add(new TransactionModel(id, title, amount, type, category, date, note));

                                if (date.length() >= 7) {
                                    monthSet.add(date.substring(0, 7)); // YYYY-MM
                                }
                            }
                        }
                    }
                }

                availableMonths.clear();
                availableMonths.add("All Time");
                List<String> sortedMonths = new ArrayList<String>(monthSet);
                Collections.sort(sortedMonths, Collections.reverseOrder());
                availableMonths.addAll(sortedMonths);

                setupMonthSpinner();
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(ReportsActivity.this, "Failed to load report data: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupMonthSpinner() {
        ArrayAdapter<String> monthAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, availableMonths);
        spReportMonth.setAdapter(monthAdapter);
        spReportMonth.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = availableMonths.get(position);
                calculateAndRenderReports("All Time".equals(selected) ? null : selected);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        calculateAndRenderReports(null);
    }

    private void calculateAndRenderReports(String monthFilter) {
        List<TransactionModel> filtered = new ArrayList<TransactionModel>();
        for (int i = 0; i < allTransactions.size(); i++) {
            TransactionModel t = allTransactions.get(i);
            if (monthFilter == null || (t.getDate() != null && t.getDate().startsWith(monthFilter))) {
                filtered.add(t);
            }
        }

        // Local native Java calculations (strictly offline)
        double totalIncome = ExpenseCalculator.calculateTotalIncome(filtered);
        double totalExpense = ExpenseCalculator.calculateTotalExpense(filtered);
        double netSavings = ExpenseCalculator.calculateBalance(totalIncome, totalExpense);

        tvIncomeSub.setText(String.format(Locale.US, "Income: $%.2f", totalIncome));
        tvExpenseSub.setText(String.format(Locale.US, "Expense: $%.2f", totalExpense));
        tvNetSavings.setText(String.format(Locale.US, "$%.2f", netSavings));

        // Category breakdown calculation
        Map<String, Double> categoryMap = ExpenseCalculator.calculateCategoryExpenses(filtered, monthFilter);
        llCategoryContainer.removeAllViews();

        if (categoryMap.isEmpty()) {
            TextView emptyTv = new TextView(this);
            emptyTv.setText("No expense entries found for this period.");
            emptyTv.setTextColor(0xFF64748B);
            emptyTv.setPadding(0, 16, 0, 16);
            llCategoryContainer.addView(emptyTv);
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        for (Map.Entry<String, Double> entry : categoryMap.entrySet()) {
            String category = entry.getKey();
            double catAmount = entry.getValue();
            double percent = ExpenseCalculator.calculatePercentage(catAmount, totalExpense);

            View catView = inflater.inflate(R.layout.item_report_category, llCategoryContainer, false);
            TextView tvName = (TextView) catView.findViewById(R.id.tvReportCatName);
            TextView tvAmount = (TextView) catView.findViewById(R.id.tvReportCatAmount);
            ProgressBar pbProgress = (ProgressBar) catView.findViewById(R.id.pbReportCatProgress);
            TextView tvPercent = (TextView) catView.findViewById(R.id.tvReportCatPercent);

            tvName.setText(category);
            tvAmount.setText(String.format(Locale.US, "$%.2f", catAmount));
            pbProgress.setProgress((int) Math.round(percent));
            tvPercent.setText(String.format(Locale.US, "%.1f%%", percent));

            llCategoryContainer.addView(catView);
        }
    }
}