package com.modernexpensetracker.app;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportsActivity extends Activity {

    private TextView btnBack;
    private Spinner spinnerFilterMonth;
    private TextView tvReportIncome, tvReportExpense, tvReportSavings;
    private LinearLayout layoutCategoryBreakdown;

    private BackendApi api;
    private String token;
    private List<Transaction> allTransactions = new ArrayList<>();
    private List<String> uniqueMonthsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        SharedPreferences prefs = getSharedPreferences("ExpenseTrackerPrefs", MODE_PRIVATE);
        token = prefs.getString("token", "");
        if (token.isEmpty()) {
            finish();
            return;
        }

        api = new BackendApi(this);

        btnBack = (TextView) findViewById(R.id.btnBack);
        spinnerFilterMonth = (Spinner) findViewById(R.id.spinnerFilterMonth);
        tvReportIncome = (TextView) findViewById(R.id.tvReportIncome);
        tvReportExpense = (TextView) findViewById(R.id.tvReportExpense);
        tvReportSavings = (TextView) findViewById(R.id.tvReportSavings);
        layoutCategoryBreakdown = (LinearLayout) findViewById(R.id.layoutCategoryBreakdown);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        fetchAllTransactionsForAnalysis();
    }

    private void fetchAllTransactionsForAnalysis() {
        api.getTransactions(token, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject resObj = new JSONObject(response);
                    if (resObj.optBoolean("success", false)) {
                        JSONArray recordsArray = resObj.getJSONArray("records");
                        allTransactions.clear();
                        uniqueMonthsList.clear();

                        for (int i = 0; i < recordsArray.length(); i++) {
                            JSONObject rec = recordsArray.getJSONObject(i);
                            Transaction t = Transaction.fromJson(rec);
                            if (t != null) {
                                allTransactions.add(t);
                                String monthStr = getMonthStrFromDate(t.getDate());
                                if (!uniqueMonthsList.contains(monthStr)) {
                                    uniqueMonthsList.add(monthStr);
                                }
                            }
                        }

                        if (uniqueMonthsList.isEmpty()) {
                            uniqueMonthsList.add("All");
                        } else {
                            uniqueMonthsList.add(0, "All");
                        }

                        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(ReportsActivity.this, android.R.layout.simple_spinner_item, uniqueMonthsList);
                        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerFilterMonth.setAdapter(monthAdapter);

                        spinnerFilterMonth.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                calculateFilteredMonthlyReport(uniqueMonthsList.get(position));
                            }

                            @Override
                            public void onNothingSelected(AdapterView<?> parent) {
                            }
                        });

                        // Calculate initial default
                        calculateFilteredMonthlyReport("All");
                    }
                } catch (Exception e) {
                    Toast.makeText(ReportsActivity.this, "Error compiling analysis framework.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(ReportsActivity.this, "Reports fetch failure: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getMonthStrFromDate(String date) {
        if (date != null && date.length() >= 7) {
            return date.substring(0, 7); // Returns "YYYY-MM"
        }
        return "Unknown";
    }

    /**
     * Completes calculations locally inside native Java
     */
    private void calculateFilteredMonthlyReport(String selectedMonth) {
        double totalInflow = 0.0;
        double totalOutflow = 0.0;

        Map<String, Double> categoryTotals = new HashMap<>();

        for (int i = 0; i < allTransactions.size(); i++) {
            Transaction t = allTransactions.get(i);
            String tMonth = getMonthStrFromDate(t.getDate());

            if ("All".equals(selectedMonth) || selectedMonth.equals(tMonth)) {
                if ("income".equalsIgnoreCase(t.getType())) {
                    totalInflow += t.getAmount();
                } else {
                    totalOutflow += t.getAmount();

                    // Track categorized outbound distribution
                    String cat = t.getCategory();
                    double currentSum = categoryTotals.containsKey(cat) ? categoryTotals.get(cat) : 0.0;
                    categoryTotals.put(cat, currentSum + t.getAmount());
                }
            }
        }

        double netSavings = totalInflow - totalOutflow;

        tvReportIncome.setText(String.format("+$%.2f", totalInflow));
        tvReportExpense.setText(String.format("-$%.2f", totalOutflow));
        tvReportSavings.setText(String.format("$%.2f", netSavings));

        if (netSavings >= 0) {
            tvReportSavings.setTextColor(Color.parseColor("#1B5E20"));
        } else {
            tvReportSavings.setTextColor(Color.parseColor("#B71C1C"));
        }

        // Generate dynamic views representing breakdown distribution
        layoutCategoryBreakdown.removeAllViews();

        if (categoryTotals.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText("No categorization metrics available for this period.");
            emptyText.setTextColor(Color.parseColor("#757575"));
            layoutCategoryBreakdown.addView(emptyText);
        } else {
            for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
                RelativeLayout itemLayout = new RelativeLayout(this);
                itemLayout.setPadding(0, 8, 0, 8);

                TextView tvCatName = new TextView(this);
                tvCatName.setText(entry.getKey());
                tvCatName.setTextColor(Color.parseColor("#212121"));
                tvCatName.setTextSize(14);
                
                TextView tvCatVal = new TextView(this);
                tvCatVal.setText(String.format("$%.2f", entry.getValue()));
                tvCatVal.setTextColor(Color.parseColor("#D32F2F"));
                tvCatVal.setTextSize(14);
                
                RelativeLayout.LayoutParams paramsName = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                paramsName.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                itemLayout.addView(tvCatName, paramsName);

                RelativeLayout.LayoutParams paramsVal = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                paramsVal.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                itemLayout.addView(tvCatVal, paramsVal);

                layoutCategoryBreakdown.addView(itemLayout);
            }
        }
    }
}