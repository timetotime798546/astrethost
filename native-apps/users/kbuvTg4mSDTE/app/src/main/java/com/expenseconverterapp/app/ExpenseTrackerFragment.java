package com.expenseconverterapp.app;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ExpenseTrackerFragment extends Fragment {

    private EditText etTitle, etAmount;
    private Button btnAddIncome, btnAddExpense;
    private TextView tvTotalBalance;
    private ListView listViewExpenses;
    private BackendApi backendApi;

    private List<ExpenseItem> expenseList = new ArrayList<>();
    private ArrayAdapter<ExpenseItem> adapter;

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_expense, container, false);

        backendApi = new BackendApi(requireContext());

        etTitle = view.findViewById(R.id.etTitle);
        etAmount = view.findViewById(R.id.etAmount);
        btnAddIncome = view.findViewById(R.id.btnAddIncome);
        btnAddExpense = view.findViewById(R.id.btnAddExpense);
        tvTotalBalance = view.findViewById(R.id.tvTotalBalance);
        listViewExpenses = view.findViewById(R.id.listViewExpenses);

        adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, expenseList);
        listViewExpenses.setAdapter(adapter);

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

        loadExpenses();

        return view;
    }

    private void addTransaction(String type) {
        String title = etTitle.getText().toString().trim();
        String amtStr = etAmount.getText().toString().trim();
        if (title.isEmpty() || amtStr.isEmpty()) {
            Toast.makeText(requireContext(), "Enter title and amount", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amtStr);
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Invalid amount", Toast.LENGTH_SHORT).show();
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
        double balance = 0;
        for (ExpenseItem item : expenseList) {
            if ("income".equals(item.type)) {
                balance += item.amount;
            } else {
                balance -= item.amount;
            }
        }
        tvTotalBalance.setText(String.format("$%.2f", balance));
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
                adapter.notifyDataSetChanged();
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
                Toast.makeText(requireContext(), "Failed to add transaction", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(requireContext(), "Failed to delete transaction", Toast.LENGTH_SHORT).show();
            }
        }
    }
}