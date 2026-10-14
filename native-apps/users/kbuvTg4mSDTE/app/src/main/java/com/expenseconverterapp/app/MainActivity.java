package com.expenseconverterapp.app;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {

    private Button btnTabExpense, btnTabCurrency, btnLogout;
    private LinearLayout containerExpenses, containerCurrency;
    private BackendApi backendApi;

    // Expenses fields
    private EditText etTitle, etAmount;
    private Button btnAddIncome, btnAddExpense;
    private TextView tvTotalBalance;
    private ListView listViewExpenses;
    private List<ExpenseItem> expenseList = new ArrayList<>();
    private ArrayAdapter<ExpenseItem> expenseAdapter;

    // Currency converter fields
    private EditText etAmountToConvert;
    private Spinner spinnerCurrency;
    private Button btnConvert;
    private TextView tvConversionResult;
    private static final Map<String, Double> RATES = new HashMap<>();

    static {
        RATES.put("EUR (Euro)", 0.92);
        RATES.put("GBP (British Pound)", 0.79);
        RATES.put("JPY (Japanese Yen)", 155.0);
        RATES.put("CAD (Canadian Dollar)", 1.37);
        RATES.put("AUD (Australian Dollar)", 1.50);
        RATES.put("INR (Indian Rupee)", 83.3);
    }

    public static class ExpenseItem {
        String id;
        String title;
        double amount;
        String type;

        public ExpenseItem(String id, String title, double amount, String type) {
            this.id = id;
            this.title = title;
            this.amount = amount;
            this.type = type;
        }

        @Override
        public String toString() {
            String sign = "income".equals(type) ? "+$" : "-$";
            return title + " (" + sign + String.format("%.2f", amount) + ")";
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        backendApi = new BackendApi(this);

        btnTabExpense = (Button) findViewById(R.id.btnTabExpense);
        btnTabCurrency = (Button) findViewById(R.id.btnTabCurrency);
        btnLogout = (Button) findViewById(R.id.btnLogout);
        containerExpenses = (LinearLayout) findViewById(R.id.containerExpenses);
        containerCurrency = (LinearLayout) findViewById(R.id.containerCurrency);

        // Expense views
        etTitle = (EditText) findViewById(R.id.etTitle);
        etAmount = (EditText) findViewById(R.id.etAmount);
        btnAddIncome = (Button) findViewById(R.id.btnAddIncome);
        btnAddExpense = (Button) findViewById(R.id.btnAddExpense);
        tvTotalBalance = (TextView) findViewById(R.id.tvTotalBalance);
        listViewExpenses = (ListView) findViewById(R.id.listViewExpenses);

        expenseAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, expenseList);
        listViewExpenses.setAdapter(expenseAdapter);

        // Currency views
        etAmountToConvert = (EditText) findViewById(R.id.etAmountToConvert);
        spinnerCurrency = (Spinner) findViewById(R.id.spinnerCurrency);
        btnConvert = (Button) findViewById(R.id.btnConvert);
        tvConversionResult = (TextView) findViewById(R.id.tvConversionResult);

        String[] currencies = RATES.keySet().toArray(new String[0]);
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, currencies);
        spinnerCurrency.setAdapter(spinnerAdapter);

        // Navigation Tabs
        btnTabExpense.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                containerExpenses.setVisibility(View.VISIBLE);
                containerCurrency.setVisibility(View.GONE);
                btnTabExpense.setBackgroundColor(getResources().getColor(R.color.accentColor));
                btnTabCurrency.setBackgroundColor(getResources().getColor(R.color.primaryDarkColor));
            }
        });

        btnTabCurrency.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                containerExpenses.setVisibility(View.GONE);
                containerCurrency.setVisibility(View.VISIBLE);
                btnTabCurrency.setBackgroundColor(getResources().getColor(R.color.accentColor));
                btnTabExpense.setBackgroundColor(getResources().getColor(R.color.primaryDarkColor));
            }
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                backendApi.clearToken();
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                finish();
            }
        });

        // Expense operations
        btnAddIncome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addTransaction("income");
            }
        });

        btnAddExpense.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addTransaction("expense");
            }
        });

        listViewExpenses.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                ExpenseItem item = expenseList.get(position);
                new DeleteTask().execute(item.id);
                return true;
            }
        });

        // Currency conversion
        btnConvert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                convertCurrency();
            }
        });

        loadExpenses();
    }

    private void addTransaction(String type) {
        String title = etTitle.getText().toString().trim();
        String amtStr = etAmount.getText().toString().trim();
        if (title.isEmpty() || amtStr.isEmpty()) {
            Toast.makeText(this, "Enter title and amount", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amtStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            JSONObject data = new JSONObject();
            data.put("title", title);
            data.put("amount", amount);
            data.put("type", type);

            new CreateTask().execute(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadExpenses() {
        new LoadTask().execute();
    }

    private void calculateBalance() {
        double balance = 0.0;
        for (ExpenseItem item : expenseList) {
            if ("income".equals(item.type)) {
                balance += item.amount;
            } else {
                balance -= item.amount;
            }
        }
        tvTotalBalance.setText(String.format("$%.2f", balance));
    }

    private void convertCurrency() {
        String valStr = etAmountToConvert.getText().toString().trim();
        if (valStr.isEmpty()) {
            Toast.makeText(this, "Enter amount in USD", Toast.LENGTH_SHORT).show();
            return;
        }

        double usdAmount;
        try {
            usdAmount = Double.parseDouble(valStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
            return;
        }

        String selectedCurrency = (String) spinnerCurrency.getSelectedItem();
        Double rate = RATES.get(selectedCurrency);
        if (rate != null) {
            double converted = usdAmount * rate;
            String symbol = selectedCurrency.substring(0, 3);
            tvConversionResult.setText(String.format("Result: %.2f %s", converted, symbol));
        }
    }

    private class LoadTask extends AsyncTask<Void, Void, JSONObject> {
        @Override
        protected JSONObject doInBackground(Void... voids) {
            return backendApi.readData("expenses");
        }

        @Override
        protected void onPostExecute(JSONObject result) {
            if (result != null && result.optBoolean("success", false)) {
                expenseList.clear();
                JSONArray records = result.optJSONArray("records");
                if (records != null) {
                    for (int i = 0; i < records.length(); i++) {
                        JSONObject rec = records.optJSONObject(i);
                        if (rec != null) {
                            String id = rec.optString("id");
                            JSONObject data = rec.optJSONObject("data");
                            if (data != null) {
                                String title = data.optString("title");
                                double amount = data.optDouble("amount", 0.0);
                                String type = data.optString("type", "expense");
                                expenseList.add(new ExpenseItem(id, title, amount, type));
                            }
                        }
                    }
                }
                expenseAdapter.notifyDataSetChanged();
                calculateBalance();
            }
        }
    }

    private class CreateTask extends AsyncTask<JSONObject, Void, JSONObject> {
        @Override
        protected JSONObject doInBackground(JSONObject... params) {
            return backendApi.createData("expenses", params[0]);
        }

        @Override
        protected void onPostExecute(JSONObject result) {
            if (result != null && result.optBoolean("success", false)) {
                etTitle.setText("");
                etAmount.setText("");
                loadExpenses();
            } else {
                Toast.makeText(MainActivity.this, "Failed to add transaction", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class DeleteTask extends AsyncTask<String, Void, JSONObject> {
        @Override
        protected JSONObject doInBackground(String... params) {
            return backendApi.deleteData(params[0]);
        }

        @Override
        protected void onPostExecute(JSONObject result) {
            if (result != null && result.optBoolean("success", false)) {
                loadExpenses();
            } else {
                Toast.makeText(MainActivity.this, "Failed to delete transaction", Toast.LENGTH_SHORT).show();
            }
        }
    }
}