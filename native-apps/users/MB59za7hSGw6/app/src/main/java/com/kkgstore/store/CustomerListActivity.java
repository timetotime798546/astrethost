package com.kkgstore.store;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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

public class CustomerListActivity extends Activity {

    private DatabaseHelper dbHelper;
    private ListView lvCustomers;
    private EditText etSearch;
    private CustomerAdapter adapter;
    private List<CustomerModel> customerList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DemoLockManager.checkAndEnforceLock(this)) return;

        setContentView(R.layout.activity_customer_list);
        dbHelper = new DatabaseHelper(this);

        lvCustomers = (ListView) findViewById(R.id.lv_customers);
        etSearch = (EditText) findViewById(R.id.et_search_customer);
        Button btnAdd = (Button) findViewById(R.id.btn_add_customer);

        adapter = new CustomerAdapter(this, customerList);
        lvCustomers.setAdapter(adapter);

        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(CustomerListActivity.this, AddEditCustomerActivity.class));
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
        if (DemoLockManager.checkAndEnforceLock(this)) return;
        loadCustomers(etSearch.getText().toString().trim());
    }

    private void loadCustomers(String filter) {
        customerList.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String sql = "SELECT id, name, mobile, opening_balance FROM customers WHERE is_active=1";
        if (filter != null && filter.length() > 0) {
            sql += " AND (name LIKE '%" + filter + "%' OR mobile LIKE '%" + filter + "%')";
        }
        sql += " ORDER BY name ASC";

        Cursor cursor = db.rawQuery(sql, null);
        while (cursor.moveToNext()) {
            long id = cursor.getLong(0);
            String name = cursor.getString(1);
            String mobile = cursor.getString(2);
            double openingBalance = cursor.getDouble(3);

            double totalUdhaar = openingBalance;
            Cursor cU = db.rawQuery("SELECT SUM(amount) FROM udhaar_entries WHERE customer_id=" + id, null);
            if (cU.moveToFirst()) totalUdhaar += cU.getDouble(0);
            cU.close();

            double totalJama = 0;
            Cursor cJ = db.rawQuery("SELECT SUM(amount) FROM jama_entries WHERE customer_id=" + id, null);
            if (cJ.moveToFirst()) totalJama = cJ.getDouble(0);
            cJ.close();

            double pending = Math.max(0, totalUdhaar - totalJama);

            customerList.add(new CustomerModel(id, name, mobile, totalUdhaar, totalJama, pending));
        }
        cursor.close();
        adapter.notifyDataSetChanged();
    }

    private static class CustomerModel {
        long id;
        String name;
        String mobile;
        double totalUdhaar;
        double totalJama;
        double pending;

        CustomerModel(long id, String name, String mobile, double totalUdhaar, double totalJama, double pending) {
            this.id = id;
            this.name = name;
            this.mobile = mobile;
            this.totalUdhaar = totalUdhaar;
            this.totalJama = totalJama;
            this.pending = pending;
        }
    }

    private class CustomerAdapter extends BaseAdapter {
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

            final CustomerModel model = items.get(position);

            TextView tvName = (TextView) convertView.findViewById(R.id.item_customer_name);
            TextView tvMobile = (TextView) convertView.findViewById(R.id.item_customer_mobile);
            TextView tvUdhaar = (TextView) convertView.findViewById(R.id.item_customer_total_udhaar);
            TextView tvJama = (TextView) convertView.findViewById(R.id.item_customer_total_jama);
            TextView tvPending = (TextView) convertView.findViewById(R.id.item_customer_pending);
            Button btnLedger = (Button) convertView.findViewById(R.id.btn_open_ledger);

            tvName.setText(model.name);
            tvMobile.setText(model.mobile.isEmpty() ? "No Mobile" : model.mobile);
            tvUdhaar.setText(String.format("₹ %.2f", model.totalUdhaar));
            tvJama.setText(String.format("₹ %.2f", model.totalJama));
            tvPending.setText(String.format("₹ %.2f", model.pending));

            btnLedger.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(context, CustomerDetailActivity.class);
                    intent.putExtra("customer_id", model.id);
                    intent.putExtra("customer_name", model.name);
                    intent.putExtra("customer_mobile", model.mobile);
                    context.startActivity(intent);
                }
            });

            return convertView;
        }
    }
}