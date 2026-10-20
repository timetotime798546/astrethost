package com.expensetracker.app;

import android.app.Activity;
import android.os.Bundle;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReportsActivity extends Activity {

    private Spinner spnReportMonth;
    private TextView tvReportIncome, tvReportExpense, tvReportNet;
    private LinearLayout categoryContainer;
    private ProgressBar reportLoading;

    private BackendApi api;
    private final List<TransactionModel> allTransactions = new ArrayList<TransactionModel>();
    private final List<String> monthList = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        api = new BackendApi(this);

        spnReportMonth = (Spinner) findViewById(R.id.spnReportMonth);
        tvReportIncome = (TextView) findViewById(R.id.tvReportIncome);
        tvReportExpense = (TextView) findViewById(R.id.tvReportExpense);
        tvReportNet = (TextView) findViewById(R.id.tvReportNet);
        categoryContainer = (LinearLayout) findViewById(R.id.categoryContainer);
        reportLoading = (ProgressBar) findViewById(R.id.reportLoading);

        setupMonthSelector();
        loadAllTransactions();
    }

    private void setupMonthSelector() {
        monthList.clear();
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
        // Populate current month and past 6 months
        for (int i = 0; i < 7; i++) {
            monthList.add(sdf.format(cal.getTime()));
            cal.add(Calendar.MONTH, -1);
        }

        ArrayAdapter<String> monthAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, monthList);
        spnReportMonth.setAdapter(monthAdapter);

        spnReportMonth.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                recalculateAndRender();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadAllTransactions() {
        reportLoading.setVisibility(View.VISIBLE);
        api.getTransactions(new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                reportLoading.setVisibility(View.GONE);
                allTransactions.clear();

                JSONArray records = response.optJSONArray("records");
                if (records != null) {
                    for (int i = 0; i < records.length(); i++) {
                        JSONObject rec = records.optJSONObject(i);
                        if (rec != null) {
                            String id = rec.optString("id", "");
                            JSONObject data = rec.optJSONObject("data");
                            TransactionModel model = TransactionModel.fromJson(id, data);
                            if (model != null) {
                                allTransactions.add(model);
                            }
                        }
                    }
                }

                recalculateAndRender();
            }

            @Override
            public void onError(String errorMessage) {
                reportLoading.setVisibility(View.GONE);
                Toast.makeText(ReportsActivity.this, "Report error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void recalculateAndRender() {
        if (spnReportMonth.getSelectedItem() == null) return;
        String selectedMonth = spnReportMonth.getSelectedItem().toString();

        // 1. Mandatory Offline Calculation: Filter list for selected month
        List<TransactionModel> monthItems = ExpenseCalculator.filterByMonth(allTransactions, selectedMonth);

        // 2. Offline deterministic math for totals
        double income = ExpenseCalculator.calculateTotalIncome(monthItems);
        double expense = ExpenseCalculator.calculateTotalExpense(monthItems);
        double net = income - expense;

        tvReportIncome.setText(String.format(Locale.getDefault(), "$%.2f", income));
        tvReportExpense.setText(String.format(Locale.getDefault(), "$%.2f", expense));
        tvReportNet.setText(String.format(Locale.getDefault(), "$%.2f", net));

        if (net >= 0) {
            tvReportNet.setTextColor(getResources().getColor(R.color.accent_income));
        } else {
            tvReportNet.setTextColor(getResources().getColor(R.color.accent_expense));
        }

        // 3. Category distribution calculations
        Map<String, Double> categoryMap = ExpenseCalculator.calculateExpensesByCategory(monthItems);
        categoryContainer.removeAllViews();

        if (categoryMap.isEmpty()) {
            TextView emptyTv = new TextView(this);
            emptyTv.setText("No expense records found for " + selectedMonth);
            emptyTv.setTextColor(getResources().getColor(R.color.text_secondary));
            emptyTv.setPadding(0, 16, 0, 16);
            categoryContainer.addView(emptyTv);
            return;
        }

        for (Map.Entry<String, Double> entry : categoryMap.entrySet()) {
            String category = entry.getKey();
            double catAmount = entry.getValue();
            double percentage = ExpenseCalculator.calculatePercentage(catAmount, expense);

            // Create clean card for each category progress
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundResource(R.drawable.bg_card);
            card.setPadding(32, 24, 32, 24);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, 16);
            card.setLayoutParams(lp);

            LinearLayout headerRow = new LinearLayout(this);
            headerRow.setOrientation(LinearLayout.HORIZONTAL);

            TextView titleTv = new TextView(this);
            titleTv.setText(category);
            titleTv.setTextSize(14);
            titleTv.setTextColor(getResources().getColor(R.color.text_primary));
            LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            titleTv.setLayoutParams(titleLp);

            TextView amountTv = new TextView(this);
            amountTv.setText(String.format(Locale.getDefault(), "$%.2f (%.1f%%)", catAmount, percentage));
            amountTv.setTextSize(13);
            amountTv.setTextColor(getResources().getColor(R.color.accent_expense));

            headerRow.addView(titleTv);
            headerRow.addView(amountTv);
            card.addView(headerRow);

            // Visual bar indicator
            LinearLayout barBg = new LinearLayout(this);
            barBg.setBackgroundColor(getResources().getColor(R.color.input_bg));
            LinearLayout.LayoutParams barBgLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 16);
            barBgLp.setMargins(0, 16, 0, 0);
            barBg.setLayoutParams(barBgLp);
            barBg.setWeightSum(100.0f);

            View barFill = new View(this);
            barFill.setBackgroundColor(getResources().getColor(R.color.accent_expense));
            float fillWeight = (float) Math.max(1.0, Math.min(100.0, percentage));
            LinearLayout.LayoutParams barFillLp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.MATCH_PARENT, fillWeight);
            barFill.setLayoutParams(barFillLp);

            barBg.addView(barFill);
            card.addView(barBg);

            categoryContainer.addView(card);
        }
    }
}