package com.digitalkhatabilling.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {
    public static class Customer {
        public String id;
        public String name;
        public String phone;
        public double totalGot = 0;
        public double totalGave = 0;
        public double balance = 0;
    }

    public static class Transaction {
        public String id;
        public String phone;
        public double amount;
        public String type;
        public String date;
        public String notes;
    }

    private BackendApi backendApi;
    private List<Customer> customerList = new ArrayList<>();
    private List<Customer> filteredCustomerList = new ArrayList<>();
    private List<Transaction> allTransactions = new ArrayList<>();
    
    private TextView tvTotalBalance, tvTotalGot, tvTotalGave;
    private EditText etSearch;
    private ListView lvCustomers;
    private Button btnAddCustomer, btnLogout;
    private ProgressBar progressBar;
    private CustomerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        backendApi = new BackendApi(this);

        tvTotalBalance = findViewById(R.id.tv_total_balance);
        tvTotalGot = findViewById(R.id.tv_total_got);
        tvTotalGave = findViewById(R.id.tv_total_gave);
        etSearch = findViewById(R.id.et_search);
        lvCustomers = findViewById(R.id.lv_customers);
        btnAddCustomer = findViewById(R.id.btn_add_customer);
        btnLogout = findViewById(R.id.btn_logout);
        progressBar = findViewById(R.id.progress_bar);

        adapter = new CustomerAdapter(this, filteredCustomerList);
        lvCustomers.setAdapter(adapter);

        lvCustomers.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Customer selected = filteredCustomerList.get(position);
                Intent intent = new Intent(MainActivity.this, CustomerDetailActivity.class);
                intent.putExtra("customer_name", selected.name);
                intent.putExtra("customer_phone", selected.phone);
                intent.putExtra("customer_id", selected.id);
                startActivity(intent);
            }
        });

        btnAddCustomer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddCustomerDialog();
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

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterCustomers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        progressBar.setVisibility(View.VISIBLE);
        backendApi.getRecords("customers", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(final String customerResponse) {
                backendApi.getRecords("khata_entries", new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(final String transactionResponse) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressBar.setVisibility(View.GONE);
                                processAndCalculateData(customerResponse, transactionResponse);
                            }
                        });
                    }

                    @Override
                    public void onFailure(final String errorMessage) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(MainActivity.this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                });
            }

            @Override
            public void onFailure(final String errorMessage) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(MainActivity.this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void processAndCalculateData(String customerJsonStr, String transactionJsonStr) {
        try {
            customerList.clear();
            allTransactions.clear();

            JSONObject custObj = new JSONObject(customerJsonStr);
            if (custObj.optBoolean("success")) {
                JSONArray records = custObj.optJSONArray("records");
                if (records != null) {
                    for (int i = 0; i < records.length(); i++) {
                        JSONObject rec = records.getJSONObject(i);
                        JSONObject data = rec.getJSONObject("data");
                        Customer c = new Customer();
                        c.id = rec.getString("id");
                        c.name = data.optString("name");
                        c.phone = data.optString("phone");
                        customerList.add(c);
                    }
                }
            }

            JSONObject transObj = new JSONObject(transactionJsonStr);
            if (transObj.optBoolean("success")) {
                JSONArray records = transObj.optJSONArray("records");
                if (records != null) {
                    for (int i = 0; i < records.length(); i++) {
                        JSONObject rec = records.getJSONObject(i);
                        JSONObject data = rec.getJSONObject("data");
                        Transaction t = new Transaction();
                        t.id = rec.getString("id");
                        t.phone = data.optString("customer_phone");
                        t.amount = data.optDouble("amount");
                        t.type = data.optString("type");
                        t.date = data.optString("date");
                        t.notes = data.optString("notes");
                        allTransactions.add(t);
                    }
                }
            }

            double aggregateGot = 0;
            double aggregateGave = 0;

            Map<String, List<Transaction>> transMap = new HashMap<>();
            for (Transaction t : allTransactions) {
                if (!transMap.containsKey(t.phone)) {
                    transMap.put(t.phone, new ArrayList<Transaction>());
                }
                transMap.get(t.phone).add(t);
            }

            for (Customer c : customerList) {
                c.totalGot = 0;
                c.totalGave = 0;
                List<Transaction> list = transMap.get(c.phone);
                if (list != null) {
                    for (Transaction t : list) {
                        if ("got".equals(t.type)) {
                            c.totalGot += t.amount;
                        } else if ("gave".equals(t.type)) {
                            c.totalGave += t.amount;
                        }
                    }
                }
                c.balance = c.totalGot - c.totalGave;
                aggregateGot += c.totalGot;
                aggregateGave += c.totalGave;
            }

            double aggregateBalance = aggregateGot - aggregateGave;
            tvTotalGot.setText("Total Got: \u20B9" + String.format("%.2f", aggregateGot));
            tvTotalGave.setText("Total Gave: \u20B9" + String.format("%.2f", aggregateGave));
            
            if (aggregateBalance >= 0) {
                tvTotalBalance.setText("\u20B9" + String.format("%.2f", aggregateBalance));
                tvTotalBalance.setTextColor(Color.parseColor("#00E676"));
            } else {
                tvTotalBalance.setText("\u20B9" + String.format("%.2f", Math.abs(aggregateBalance)));
                tvTotalBalance.setTextColor(Color.parseColor("#FF1744"));
            }

            filterCustomers(etSearch.getText().toString());

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Calculation or parsing error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void filterCustomers(String query) {
        filteredCustomerList.clear();
        String lowercaseQuery = query.toLowerCase().trim();
        for (Customer c : customerList) {
            if (c.name.toLowerCase().contains(lowercaseQuery) || c.phone.contains(lowercaseQuery)) {
                filteredCustomerList.add(c);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void showAddCustomerDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_customer, null);
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        final EditText etName = view.findViewById(R.id.dialog_et_name);
        final EditText etPhone = view.findViewById(R.id.dialog_et_phone);
        Button btnSave = view.findViewById(R.id.dialog_btn_save);
        Button btnCancel = view.findViewById(R.id.dialog_btn_cancel);

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = etName.getText().toString().trim();
                String phone = etPhone.getText().toString().trim();

                if (name.isEmpty() || phone.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter all details", Toast.LENGTH_SHORT).show();
                    return;
                }

                JSONObject fields = new JSONObject();
                try {
                    fields.put("name", name);
                    fields.put("phone", phone);
                } catch (Exception ignored) {}

                dialog.dismiss();
                progressBar.setVisibility(View.VISIBLE);

                backendApi.createRecord("customers", fields, new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(String response) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                loadData();
                            }
                        });
                    }

                    @Override
                    public void onFailure(final String errorMessage) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(MainActivity.this, "Add customer failed: " + errorMessage, Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                });
            }
        });

        dialog.show();
    }

    private static class CustomerAdapter extends BaseAdapter {
        private Context context;
        private List<Customer> list;

        public CustomerAdapter(Context context, List<Customer> list) {
            this.context = context;
            this.list = list;
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Object getItem(int position) {
            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.list_item_customer, parent, false);
            }

            TextView tvName = convertView.findViewById(R.id.item_customer_name);
            TextView tvPhone = convertView.findViewById(R.id.item_customer_phone);
            TextView tvBalance = convertView.findViewById(R.id.item_customer_balance);

            Customer c = list.get(position);
            tvName.setText(c.name);
            tvPhone.setText(c.phone);

            if (c.balance > 0) {
                tvBalance.setText("Jama: \u20B9" + String.format("%.2f", c.balance));
                tvBalance.setTextColor(Color.parseColor("#00E676"));
            } else if (c.balance < 0) {
                tvBalance.setText("Udhaar: \u20B9" + String.format("%.2f", Math.abs(c.balance)));
                tvBalance.setTextColor(Color.parseColor("#FF1744"));
            } else {
                tvBalance.setText("Settle: \u20B90.00");
                tvBalance.setTextColor(Color.WHITE);
            }

            return convertView;
        }
    }
}