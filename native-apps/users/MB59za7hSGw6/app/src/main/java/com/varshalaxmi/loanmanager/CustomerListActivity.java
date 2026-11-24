package com.varshalaxmi.loanmanager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
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
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CustomerListActivity extends Activity {

    private DatabaseHelper dbHelper;
    private ListView lvCustomers;
    private EditText etSearch;
    private CustomerAdapter adapter;
    private List<CustomerModel> customerList = new ArrayList<>();

    private static class CustomerModel {
        long id;
        String name;
        String phone;
        double totalGiven;
        double totalOutstanding;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_list);

        dbHelper = new DatabaseHelper(this);
        lvCustomers = (ListView) findViewById(R.id.lv_customers);
        etSearch = (EditText) findViewById(R.id.et_search_customer);

        Button btnAddNew = (Button) findViewById(R.id.btn_add_new_customer_top);
        btnAddNew.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(CustomerListActivity.this, AddCustomerActivity.class));
            }
        });

        adapter = new CustomerAdapter(this, customerList);
        lvCustomers.setAdapter(adapter);

        lvCustomers.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                CustomerModel selected = customerList.get(position);
                Intent intent = new Intent(CustomerListActivity.this, CustomerDetailActivity.class);
                intent.putExtra("customer_id", selected.id);
                startActivity(intent);
            }
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadCustomers(s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCustomers(etSearch.getText().toString().trim());
    }

    private void loadCustomers(String filter) {
        customerList.clear();
        Cursor cursor;
        if (filter.isEmpty()) {
            cursor = dbHelper.getAllActiveCustomers();
        } else {
            cursor = dbHelper.searchCustomers(filter);
        }

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    CustomerModel cm = new CustomerModel();
                    cm.id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                    cm.name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                    cm.phone = cursor.getString(cursor.getColumnIndexOrThrow("phone"));
                    cm.totalGiven = dbHelper.getCustomerTotalPrincipalGiven(cm.id);
                    cm.totalOutstanding = dbHelper.getCustomerTotalOutstanding(cm.id);
                    customerList.add(cm);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        adapter.notifyDataSetChanged();
    }

    private static class CustomerAdapter extends BaseAdapter {
        private Context context;
        private List<CustomerModel> items;

        CustomerAdapter(Context context, List<CustomerModel> items) {
            this.context = context;
            this.items = items;
        }

        @Override
        public int getCount() { return items.size(); }

        @Override
        public Object getItem(int position) { return items.get(position); }

        @Override
        public long getItemId(int position) { return items.get(position).id; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.item_customer, parent, false);
            }
            CustomerModel item = items.get(position);

            TextView tvName = (TextView) convertView.findViewById(R.id.tv_item_customer_name);
            TextView tvPhone = (TextView) convertView.findViewById(R.id.tv_item_customer_phone);
            TextView tvGiven = (TextView) convertView.findViewById(R.id.tv_item_total_given);
            TextView tvDue = (TextView) convertView.findViewById(R.id.tv_item_outstanding);

            tvName.setText(item.name);
            tvPhone.setText(item.phone != null ? item.phone : "");
            tvGiven.setText(String.format(Locale.US, "Principal: ₹%,.2f", item.totalGiven));
            tvDue.setText(String.format(Locale.US, "Due: ₹%,.2f", item.totalOutstanding));

            return convertView;
        }
    }
}