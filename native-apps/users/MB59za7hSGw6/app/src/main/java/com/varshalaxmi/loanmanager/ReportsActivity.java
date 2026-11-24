package com.varshalaxmi.loanmanager;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import java.util.Locale;

public class ReportsActivity extends Activity {

    private DatabaseHelper dbHelper;

    private TextView tvGiven, tvCollected, tvInterestEarned, tvOutstanding;
    private TextView tvTodayCollection, tvActiveLoans, tvCustomerCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        dbHelper = new DatabaseHelper(this);

        tvGiven = (TextView) findViewById(R.id.tv_rep_total_given);
        tvCollected = (TextView) findViewById(R.id.tv_rep_total_collected);
        tvInterestEarned = (TextView) findViewById(R.id.tv_rep_interest_earned);
        tvOutstanding = (TextView) findViewById(R.id.tv_rep_outstanding);

        tvTodayCollection = (TextView) findViewById(R.id.tv_rep_today_collection);
        tvActiveLoans = (TextView) findViewById(R.id.tv_rep_active_count);
        tvCustomerCount = (TextView) findViewById(R.id.tv_rep_customer_count);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadReportData();
    }

    private void loadReportData() {
        double totalGiven = dbHelper.getTotalLoanGivenAmount();
        double totalCollected = dbHelper.getTotalAmountCollected();
        double interestEarned = dbHelper.getTotalInterestEarned();
        double totalOutstanding = dbHelper.getTotalOutstandingAmountAll();

        double todayCollection = dbHelper.getTodayCollectionAmount();
        int activeLoans = dbHelper.getActiveLoanCount();
        int customerCount = dbHelper.getTotalCustomerCount();

        tvGiven.setText(String.format(Locale.US, "₹ %,.2f", totalGiven));
        tvCollected.setText(String.format(Locale.US, "₹ %,.2f", totalCollected));
        tvInterestEarned.setText(String.format(Locale.US, "₹ %,.2f", interestEarned));
        tvOutstanding.setText(String.format(Locale.US, "₹ %,.2f", totalOutstanding));

        tvTodayCollection.setText(String.format(Locale.US, "₹ %,.2f", todayCollection));
        tvActiveLoans.setText(String.valueOf(activeLoans));
        tvCustomerCount.setText(String.valueOf(customerCount));
    }
}