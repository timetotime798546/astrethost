package com.modernexpensetracker.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ReportsActivity extends Activity {

    private TextView tvPeriod, tvSavingsRate, tvSavingsSummary, tvTotalIncome, tvTotalExpenses;
    private LinearLayout layoutCategoryBreakdown;
    private BackendApi backendApi;

    private int targetYear;
    private int targetMonth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        backendApi = new BackendApi(this);

        Calendar now = Calendar.getInstance();
        targetYear = getIntent().getIntExtra("year", now.get(Calendar.YEAR));
        targetMonth = getIntent().getIntExtra("month", now.get(Calendar.MONTH));

        tvPeriod = (TextView) findViewById(R.id.tv_report_period);
        tvSavingsRate = (TextView) findViewById(R.id.tv_savings_rate);
        tvSavingsSummary = (TextView) findViewById(R.id.tv_savings_summary);
        tvTotalIncome = (TextView) findViewById(R.id.tv_report_total_income);
        tvTotalExpenses = (TextView) findViewById(R.id.tv_report_total_expenses);
        layoutCategoryBreakdown = (LinearLayout) findViewById(R.id.layout_category_breakdown);

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, targetYear);
        cal.set(Calendar.MONTH, targetMonth);
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.US);
        tvPeriod.setText("Financial Analysis for " + sdf.format(cal.getTime()));

        fetchAndRenderReport();
    }

    private void fetchAndRenderReport() {
        backendApi.getData("transactions", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                List<TransactionModel> monthlyTransactions = new ArrayList<TransactionModel>();
                try {
                    if (response.optBoolean("success", false) && response.has("records")) {
                        JSONArray records = response.getJSONArray("records");
                        String targetYM = String.format(Locale.US, "%04d-%02d", targetYear, targetMonth + 1);

                        for (int i = 0; i < records.length(); i++) {
                            JSONObject rec = records.getJSONObject(i);
                            String id = rec.optString("id", "");
                            JSONObject data = rec.optJSONObject("data");
                            if (data != null) {
                                TransactionModel t = TransactionModel.fromJson(id, data);
                                if (t.getDate() != null && t.getDate().startsWith(targetYM)) {
                                    monthlyTransactions.add(t);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    Toast.makeText(ReportsActivity.this, "Error parsing report", Toast.LENGTH_SHORT).show();
                }

                // Deterministic local offline calculations
                double totalIncome = CalculationHelper.calculateTotalIncome(monthlyTransactions);
                double totalExpense = CalculationHelper.calculateTotalExpense(monthlyTransactions);
                double savingsRate = CalculationHelper.calculateSavingsRate(totalIncome, totalExpense);
                double savedAmount = CalculationHelper.calculateNetBalance(totalIncome, totalExpense);

                tvTotalIncome.setText(String.format(Locale.US, "$%.2f", totalIncome));
                tvTotalExpenses.setText(String.format(Locale.US, "$%.2f", totalExpense));
                tvSavingsRate.setText(String.format(Locale.US, "%.1f%%", savingsRate));

                if (savedAmount >= 0) {
                    tvSavingsSummary.setText(String.format(Locale.US, "You saved $%.2f this month.", savedAmount));
                } else {
                    tvSavingsSummary.setText(String.format(Locale.US, "You spent $%.2f more than you earned this month.", Math.abs(savedAmount)));
                }

                List<CategorySummary> breakdowns = CalculationHelper.calculateCategoryBreakdown(monthlyTransactions);
                renderCategoryCards(breakdowns);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(ReportsActivity.this, "Failed to load report data: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderCategoryCards(List<CategorySummary> list) {
        layoutCategoryBreakdown.removeAllViews();

        if (list.isEmpty()) {
            TextView emptyTv = new TextView(this);
            emptyTv.setText("No expense data recorded for this month.");
            emptyTv.setPadding(0, 20, 0, 20);
            emptyTv.setTextColor(getResources().getColor(R.color.text_secondary));
            layoutCategoryBreakdown.addView(emptyTv);
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < list.size(); i++) {
            CategorySummary cs = list.get(i);
            View row = inflater.inflate(R.layout.item_category_report, layoutCategoryBreakdown, false);

            TextView tvIcon = (TextView) row.findViewById(R.id.tv_cat_icon);
            TextView tvName = (TextView) row.findViewById(R.id.tv_cat_name);
            TextView tvAmount = (TextView) row.findViewById(R.id.tv_cat_amount);
            ProgressBar pbPercent = (ProgressBar) row.findViewById(R.id.pb_cat_percent);
            TextView tvPercentText = (TextView) row.findViewById(R.id.tv_cat_percent_text);

            tvIcon.setText(CalculationHelper.getCategoryIcon(cs.getCategory()));
            tvName.setText(cs.getCategory());
            tvAmount.setText(String.format(Locale.US, "$%.2f", cs.getTotalAmount()));
            pbPercent.setProgress((int) Math.round(cs.getPercentage()));
            tvPercentText.setText(String.format(Locale.US, "%.1f%%", cs.getPercentage()));

            layoutCategoryBreakdown.addView(row);
        }
    }
}