package com.pocketledgerpro.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends Activity {

    private FrameLayout contentFrame;
    private DataManager dataManager;
    private List<Transaction> transactionList;
    
    private String currentTab = "dashboard";
    private boolean isAddingIncome = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dataManager = new DataManager(this);
        transactionList = dataManager.getTransactions();

        contentFrame = findViewById(R.id.content_frame);
        
        setupNavigation();
        showDashboard();
    }

    private void setupNavigation() {
        findViewById(R.id.nav_dashboard).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDashboard();
            }
        });
        findViewById(R.id.nav_analytics).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAnalytics();
            }
        });
        findViewById(R.id.nav_settings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSettings();
            }
        });
    }

    private void updateNavUi(String activeTab) {
        currentTab = activeTab;
        ((TextView) ((ViewGroup) findViewById(R.id.nav_dashboard)).getChildAt(0))
                .setTextColor(activeTab.equals("dashboard") ? getResources().getColor(R.color.primary) : getResources().getColor(R.color.text_secondary));
        ((TextView) ((ViewGroup) findViewById(R.id.nav_analytics)).getChildAt(0))
                .setTextColor(activeTab.equals("analytics") ? getResources().getColor(R.color.primary) : getResources().getColor(R.color.text_secondary));
        ((TextView) ((ViewGroup) findViewById(R.id.nav_settings)).getChildAt(0))
                .setTextColor(activeTab.equals("settings") ? getResources().getColor(R.color.primary) : getResources().getColor(R.color.text_secondary));
    }

    private void showDashboard() {
        updateNavUi("dashboard");
        contentFrame.removeAllViews();
        View view = getLayoutInflater().inflate(R.layout.view_dashboard, contentFrame, false);

        TextView tvBalance = view.findViewById(R.id.tv_total_balance);
        TextView tvIncome = view.findViewById(R.id.tv_total_income);
        TextView tvExpenses = view.findViewById(R.id.tv_total_expenses);
        LinearLayout listContainer = view.findViewById(R.id.transaction_list_container);

        double totalIncome = 0;
        double totalExpense = 0;

        for (Transaction t : transactionList) {
            if (t.isIncome) totalIncome += t.amount;
            else totalExpense += t.amount;
        }

        double balance = totalIncome - totalExpense;

        tvBalance.setText(String.format(Locale.US, "$%.2f", balance));
        tvIncome.setText(String.format(Locale.US, "$%.2f", totalIncome));
        tvExpenses.setText(String.format(Locale.US, "$%.2f", totalExpense));

        // Populate List (reverse for recent)
        for (int i = transactionList.size() - 1; i >= 0; i--) {
            Transaction t = transactionList.get(i);
            View itemView = getLayoutInflater().inflate(R.layout.item_transaction, listContainer, false);
            
            TextView title = itemView.findViewById(R.id.tv_item_title);
            TextView meta = itemView.findViewById(R.id.tv_item_meta);
            TextView amount = itemView.findViewById(R.id.tv_item_amount);
            View indicator = itemView.findViewById(R.id.indicator);

            title.setText(t.title);
            meta.setText(t.category + " • " + t.date);
            amount.setText((t.isIncome ? "+$" : "-$") + String.format(Locale.US, "%.2f", t.amount));
            amount.setTextColor(t.isIncome ? getResources().getColor(R.color.income) : getResources().getColor(R.color.expense));
            indicator.setBackgroundColor(t.isIncome ? getResources().getColor(R.color.income) : getResources().getColor(R.color.expense));

            listContainer.addView(itemView);
        }

        view.findViewById(R.id.btn_quick_income).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isAddingIncome = true;
                showAddForm();
            }
        });

        view.findViewById(R.id.btn_quick_expense).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isAddingIncome = false;
                showAddForm();
            }
        });

        contentFrame.addView(view);
    }

    private void showAddForm() {
        contentFrame.removeAllViews();
        View view = getLayoutInflater().inflate(R.layout.view_add_transaction, contentFrame, false);

        final EditText etTitle = view.findViewById(R.id.et_title);
        final EditText etAmount = view.findViewById(R.id.et_amount);
        final Spinner spCategory = view.findViewById(R.id.sp_category);
        TextView tvHeader = view.findViewById(R.id.tv_add_title);

        tvHeader.setText(isAddingIncome ? "Add Income" : "Add Expense");
        tvHeader.setTextColor(isAddingIncome ? getResources().getColor(R.color.income) : getResources().getColor(R.color.expense));

        String[] categories = {"Food", "Travel", "Shopping", "Bills", "Health", "Salary", "Others"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, categories) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                ((TextView) v).setTextColor(Color.WHITE);
                return v;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategory.setAdapter(adapter);

        view.findViewById(R.id.btn_save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = etTitle.getText().toString().trim();
                String amountStr = etAmount.getText().toString().trim();

                if (title.isEmpty() || amountStr.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                double amt = Double.parseDouble(amountStr);
                String cat = spCategory.getSelectedItem().toString();
                String date = new SimpleDateFormat("dd MMM", Locale.US).format(new Date());

                Transaction t = new Transaction(UUID.randomUUID().toString(), title, amt, cat, date, isAddingIncome);
                transactionList.add(t);
                dataManager.saveTransactions(transactionList);
                
                showDashboard();
            }
        });

        view.findViewById(R.id.btn_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDashboard();
            }
        });

        contentFrame.addView(view);
    }

    private void showAnalytics() {
        updateNavUi("analytics");
        contentFrame.removeAllViews();
        View view = getLayoutInflater().inflate(R.layout.view_analytics, contentFrame, false);

        LinearLayout barsContainer = view.findViewById(R.id.analytics_bars);
        
        Map<String, Double> catTotals = new HashMap<>();
        double totalSpend = 0;

        for (Transaction t : transactionList) {
            if (!t.isIncome) {
                Double current = catTotals.get(t.category);
                if (current == null) current = 0.0;
                catTotals.put(t.category, current + t.amount);
                totalSpend += t.amount;
            }
        }

        if (totalSpend == 0) {
            TextView tv = new TextView(this);
            tv.setText("No expense data to analyze yet.");
            tv.setTextColor(getResources().getColor(R.color.text_secondary));
            barsContainer.addView(tv);
        } else {
            for (Map.Entry<String, Double> entry : catTotals.entrySet()) {
                View barView = new LinearLayout(this);
                ((LinearLayout) barView).setOrientation(LinearLayout.VERTICAL);
                barView.setPadding(0, 0, 0, 16);

                TextView label = new TextView(this);
                double percent = (entry.getValue() / totalSpend) * 100;
                label.setText(String.format(Locale.US, "%s: $%.2f (%.1f%%)", entry.getKey(), entry.getValue(), percent));
                label.setTextColor(Color.WHITE);
                label.setTextSize(14spToPx(14));

                View progressBg = new View(this);
                LinearLayout.LayoutParams lpBg = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 8);
                lpBg.topMargin = 8;
                progressBg.setLayoutParams(lpBg);
                progressBg.setBackgroundColor(Color.parseColor("#333333"));

                View progress = new View(this);
                LinearLayout.LayoutParams lpVal = new LinearLayout.LayoutParams(0, 8);
                lpVal.width = (int) (getResources().getDisplayMetrics().widthPixels * (percent / 100) * 0.8);
                progress.setLayoutParams(lpVal);
                progress.setBackgroundColor(getResources().getColor(R.color.primary));

                barView.addView(label);
                ((LinearLayout) barView).addView(progressBg);
                ((LinearLayout) barView).addView(progress);
                barsContainer.addView(barView);
            }
        }

        contentFrame.addView(view);
    }

    private void showSettings() {
        updateNavUi("settings");
        contentFrame.removeAllViews();
        View view = getLayoutInflater().inflate(R.layout.view_settings, contentFrame, false);

        view.findViewById(R.id.btn_reset_data).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dataManager.clearData();
                transactionList.clear();
                Toast.makeText(MainActivity.this, "All data cleared", Toast.LENGTH_SHORT).show();
                showDashboard();
            }
        });

        contentFrame.addView(view);
    }

    private float spToPx(float sp) {
        return sp * getResources().getDisplayMetrics().scaledDensity;
    }
}