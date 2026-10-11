package com.gympro.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExpensesActivity extends Activity {
    private EditText etCategory, etAmount;
    private Button btnSave;
    private ListView lvExpenses;
    private ProgressBar pb;
    private BackendApi api;
    private List<JSONObject> expenseList = new ArrayList<>();
    private ArrayAdapter<JSONObject> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expenses);

        api = new BackendApi(this);

        etCategory = findViewById(R.id.etExpCategory);
        etAmount = findViewById(R.id.etExpAmount);
        btnSave = findViewById(R.id.btnSaveExpense);
        lvExpenses = findViewById(R.id.lvExpenses);
        pb = findViewById(R.id.pbExpenses);

        adapter = new ArrayAdapter<JSONObject>(this, android.R.layout.simple_list_item_2, android.R.id.text1, expenseList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = view.findViewById(android.R.id.text1);
                TextView text2 = view.findViewById(android.R.id.text2);

                JSONObject item = getItem(position);
                if (item != null) {
                    JSONObject data = item.optJSONObject("data");
                    if (data != null) {
                        text1.setText("Category: " + data.optString("category", "General"));
                        text1.setTextColor(0xFFFFFFFF);
                        String sub = "Amount: $" + String.format(Locale.US, "%.2f", data.optDouble("amount", 0)) + " | Date: " + data.optString("date");
                        text2.setText(sub);
                        text2.setTextColor(0xFFFF5B64);
                    }
                }
                return view;
            }
        };

        lvExpenses.setAdapter(adapter);

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recordExpense();
            }
        });

        loadExpenses();
    }

    private void loadExpenses() {
        pb.setVisibility(View.VISIBLE);
        api.getCollection("expenses", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                pb.setVisibility(View.GONE);
                expenseList.clear();
                JSONArray records = response.optJSONArray("records");
                if (records != null) {
                    for (int i = 0; i < records.length(); i++) {
                        JSONObject r = records.optJSONObject(i);
                        if (r != null) {
                            expenseList.add(r);
                        }
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onError(String errorMessage) {
                pb.setVisibility(View.GONE);
                Toast.makeText(ExpensesActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void recordExpense() {
        String category = etCategory.getText().toString().trim();
        String amtStr = etAmount.getText().toString().trim();

        if (category.isEmpty() || amtStr.isEmpty()) {
            Toast.makeText(this, "Category and Amount required", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = 0;
        try {
            amount = Double.parseDouble(amtStr);
        } catch (Exception ignored) {}

        String expDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());

        try {
            JSONObject data = new JSONObject();
            data.put("category", category);
            data.put("amount", amount);
            data.put("date", expDate);

            pb.setVisibility(View.VISIBLE);
            api.createRecord("expenses", data, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    pb.setVisibility(View.GONE);
                    etCategory.setText("");
                    etAmount.setText("");
                    Toast.makeText(ExpensesActivity.this, "Expense recorded!", Toast.LENGTH_SHORT).show();
                    loadExpenses();
                }

                @Override
                public void onError(String errorMessage) {
                    pb.setVisibility(View.GONE);
                    Toast.makeText(ExpensesActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "Error building request", Toast.LENGTH_SHORT).show();
        }
    }
}