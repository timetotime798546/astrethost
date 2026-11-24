package com.alfariskirana.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CustomerListActivity extends Activity {

    private DatabaseHelper dbHelper;
    private ListView lvCustomers;
    private EditText etSearch;
    private List<Customer> customerList = new ArrayList<Customer>();
    private CustomerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_list);

        dbHelper = new DatabaseHelper(this);
        lvCustomers = (ListView) findViewById(R.id.lv_customers);
        etSearch = (EditText) findViewById(R.id.et_search_customer);
        Button btnAdd = (Button) findViewById(R.id.btn_add_customer);

        adapter = new CustomerAdapter();
        lvCustomers.setAdapter(adapter);

        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CustomerListActivity.this, AddEditCustomerActivity.class);
                startActivity(intent);
            }
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadCustomers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCustomers(etSearch.getText().toString());
    }

    private void loadCustomers(String query) {
        customerList = dbHelper.getAllCustomers(query);
        adapter.notifyDataSetChanged();
    }

    private class CustomerAdapter extends BaseAdapter {
        @Override
        public int getCount() { return customerList.size(); }

        @Override
        public Object getItem(int position) { return customerList.get(position); }

        @Override
        public long getItemId(int position) { return customerList.get(position).getId(); }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(CustomerListActivity.this).inflate(R.layout.item_customer, parent, false);
            }

            final Customer cust = customerList.get(position);

            TextView tvName = (TextView) convertView.findViewById(R.id.item_customer_name);
            TextView tvMobile = (TextView) convertView.findViewById(R.id.item_customer_mobile);
            TextView tvBalance = (TextView) convertView.findViewById(R.id.item_customer_balance);
            Button btnLedger = (Button) convertView.findViewById(R.id.btn_open_ledger);

            tvName.setText(cust.getName());
            tvMobile.setText(cust.getMobile() == null || cust.getMobile().isEmpty() ? "No Mobile" : cust.getMobile());
            tvBalance.setText(String.format(Locale.getDefault(), "Udhaar: ₹ %.2f", cust.getPendingBalance()));

            if (cust.getPendingBalance() <= 0) {
                tvBalance.setTextColor(getResources().getColor(R.color.success));
                tvBalance.setText("Settled (₹ 0)");
            } else {
                tvBalance.setTextColor(getResources().getColor(R.color.danger));
            }

            btnLedger.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(CustomerListActivity.this, CustomerDetailActivity.class);
                    intent.putExtra("customer_id", cust.getId());
                    startActivity(intent);
                }
            });

            return convertView;
        }
    }
}