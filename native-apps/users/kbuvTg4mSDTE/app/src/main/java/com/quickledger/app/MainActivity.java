package com.quickledger.app;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends Activity {

    private EditText etTitle;
    private EditText etAmount;
    private RadioGroup rgType;
    private Button btnAdd;
    private TextView tvTotalBalance;
    private TextView tvTotalIncome;
    private TextView tvTotalExpense;
    private LinearLayout listContainer;

    private ArrayList<JSONObject> ledgerItems;
    private SharedPreferences sharedPreferences;

    private static final String PREF_NAME = "QuickLedgerPrefs";
    private static final String DATA_KEY = "ledger_data";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        ledgerItems = new ArrayList<JSONObject>();

        etTitle = (EditText) findViewById(R.id.etTitle);
        etAmount = (EditText) findViewById(R.id.etAmount);
        rgType = (RadioGroup) findViewById(R.id.rgType);
        btnAdd = (Button) findViewById(R.id.btnAdd);
        tvTotalBalance = (TextView) findViewById(R.id.tvTotalBalance);
        tvTotalIncome = (TextView) findViewById(R.id.tvTotalIncome);
        tvTotalExpense = (TextView) findViewById(R.id.tvTotalExpense);
        listContainer = (LinearLayout) findViewById(R.id.listContainer);

        loadData();

        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addEntry();
            }
        });

        updateUI();
    }

    private void loadData() {
        String dataStr = sharedPreferences.getString(DATA_KEY, "[]");
        try {
            JSONArray array = new JSONArray(dataStr);
            ledgerItems.clear();
            for (int i = 0; i < array.length(); i++) {
                ledgerItems.add(array.getJSONObject(i));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveData() {
        JSONArray array = new JSONArray();
        for (int i = 0; i < ledgerItems.size(); i++) {
            array.put(ledgerItems.get(i));
        }
        sharedPreferences.edit().putString(DATA_KEY, array.toString()).apply();
    }

    private void addEntry() {
        String title = etTitle.getText().toString().trim();
        String amountStr = etAmount.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(MainActivity.this, "Please enter a description title", Toast.LENGTH_SHORT).show();
            return;
        }

        if (amountStr.isEmpty()) {
            Toast.makeText(MainActivity.this, "Please enter an amount", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = 0;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(MainActivity.this, "Please enter a valid numeric amount", Toast.LENGTH_SHORT).show();
            return;
        }

        if (amount <= 0) {
            Toast.makeText(MainActivity.this, "Amount must be greater than zero", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isIncome = rgType.getCheckedRadioButtonId() == R.id.rbIncome;

        try {
            JSONObject newItem = new JSONObject();
            newItem.put("id", String.valueOf(System.currentTimeMillis()));
            newItem.put("title", title);
            newItem.put("amount", amount);
            newItem.put("isIncome", isIncome);

            // Add new transaction to the top of the list
            ledgerItems.add(0, newItem);
            saveData();
            updateUI();

            // Reset Form Fields
            etTitle.setText("");
            etAmount.setText("");
            rgType.check(R.id.rbIncome);
            etTitle.clearFocus();
            etAmount.clearFocus();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this, "Failed to save transition record", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateUI() {
        listContainer.removeAllViews();
        double totalIncome = 0;
        double totalExpense = 0;

        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < ledgerItems.size(); i++) {
            final int index = i;
            final JSONObject item = ledgerItems.get(i);
            try {
                String title = item.getString("title");
                final double amount = item.getDouble("amount");
                final boolean isIncome = item.getBoolean("isIncome");

                if (isIncome) {
                    totalIncome += amount;
                } else {
                    totalExpense += amount;
                }

                View itemView = inflater.inflate(R.layout.item_ledger, listContainer, false);
                TextView tvTitle = (TextView) itemView.findViewById(R.id.itemTitle);
                TextView tvType = (TextView) itemView.findViewById(R.id.itemType);
                TextView tvAmt = (TextView) itemView.findViewById(R.id.itemAmount);
                TextView btnDel = (TextView) itemView.findViewById(R.id.btnDelete);

                tvTitle.setText(title);
                if (isIncome) {
                    tvType.setText("Income");
                    tvAmt.setText("+$" + String.format("%.2f", amount));
                    tvAmt.setTextColor(0xFF10B981); // Green (#10B981)
                } else {
                    tvType.setText("Expense");
                    tvAmt.setText("-$" + String.format("%.2f", amount));
                    tvAmt.setTextColor(0xFFEF4444); // Red (#EF4444)
                }

                btnDel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ledgerItems.remove(index);
                        saveData();
                        updateUI();
                    }
                });

                listContainer.addView(itemView);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        double totalBalance = totalIncome - totalExpense;

        tvTotalIncome.setText("$" + String.format("%.2f", totalIncome));
        tvTotalExpense.setText("$" + String.format("%.2f", totalExpense));

        if (totalBalance >= 0) {
            tvTotalBalance.setText("$" + String.format("%.2f", totalBalance));
            tvTotalBalance.setTextColor(0xFF10B981); // Positive Green
        } else {
            tvTotalBalance.setText("-$" + String.format("%.2f", Math.abs(totalBalance)));
            tvTotalBalance.setTextColor(0xFFEF4444); // Negative Red
        }
    }
}