package com.gympro.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExpensesActivity extends Activity {

    private EditText etExpCategory;
    private EditText etExpAmount;
    private Button btnSaveExpense;
    private ProgressBar pbExpenses;
    private ListView lvExpenses;

    private BackendApi backendApi;
    private List<String> expenseItems;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expenses);

        backendApi = new BackendApi(this);

        etExpCategory = (EditText) findViewById(R.id.etExpCategory);
        etExpAmount = (EditText) findViewById(R.id.etExpAmount);
        btnSaveExpense = (Button) findViewById(R.id.btnSaveExpense);
        pbExpenses = (ProgressBar) findViewById(R.id.pbExpenses);
        lvExpenses = (ListView) findViewById(R.id.lvExpenses);

        expenseItems = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, expenseItems);
        lvExpenses.setAdapter(adapter);

        btnSaveExpense.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recordExpense();
            }
        });

        loadExpenses();
    }

    private void loadExpenses() {
        pbExpenses.setVisibility(View.VISIBLE);
        backendApi.getCollection("expenses", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                pbExpenses.setVisibility(View.GONE);
                expenseItems.clear();
                JSONArray records = response.optJSONArray("records");
                if (records != null) {
                    for (int i = 0; i < records.length(); i++) {
                        JSONObject rec = records.optJSONObject(i);
                        if (rec != null) {
                            JSONObject data = rec.optJSONObject("data");
                            if (data != null) {
                                String category = data.optString("category", "General");
                                double amount = data.optDouble("amount", 0.0);
                                String date = data.optString("date", "-");
                                expenseItems.add(category + ": $" + amount + " on " + date);
                            }
                        }
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onError(String errorMessage) {
                pbExpenses.setVisibility(View.GONE);
                Toast.makeText(ExpensesActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void recordExpense() {
        String category = etExpCategory.getText().toString().trim();
        String amountStr = etExpAmount.getText().toString().trim();

        if (category.isEmpty() || amountStr.isEmpty()) {
            Toast.makeText(this, "Category and Amount are required", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = 0.0;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (Exception e) {
            amount = 0.0;
        }

        pbExpenses.setVisibility(View.VISIBLE);
        btnSaveExpense.setEnabled(false);

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());

        try {
            JSONObject data = new JSONObject();
            data.put("category", category);
            data.put("amount", amount);
            data.put("date", today);
            data.put("description", category + " expense");

            backendApi.createRecord("expenses", data, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    pbExpenses.setVisibility(View.GONE);
                    btnSaveExpense.setEnabled(true);
                    etExpCategory.setText("");
                    etExpAmount.setText("");
                    Toast.makeText(ExpensesActivity.this, "Expense recorded", Toast.LENGTH_SHORT).show();
                    loadExpenses();
                }

                @Override
                public void onError(String errorMessage) {
                    pbExpenses.setVisibility(View.GONE);
                    btnSaveExpense.setEnabled(true);
                    Toast.makeText(ExpensesActivity.this, "Failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            pbExpenses.setVisibility(View.GONE);
            btnSaveExpense.setEnabled(true);
        }
    }
}