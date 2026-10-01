package com.dailybizmanager.app;

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
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import java.util.ArrayList;

public class CustomersActivity extends Activity {

    private DatabaseHelper dbHelper;
    private ListView lstCustomers;
    private EditText edtCustomerSearch;
    private ArrayList<Customer> customerList;
    private CustomerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customers);

        dbHelper = new DatabaseHelper(this);
        lstCustomers = (ListView) findViewById(R.id.lstCustomers);
        edtCustomerSearch = (EditText) findViewById(R.id.edtCustomerSearch);
        Button btnAddNewCustomer = (Button) findViewById(R.id.btnAddNewCustomer);

        customerList = new ArrayList<>();

        btnAddNewCustomer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(CustomersActivity.this, AddCustomerActivity.class));
            }
        });

        edtCustomerSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadCustomers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        lstCustomers.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Customer clicked = customerList.get(position);
                Intent editIntent = new Intent(CustomersActivity.this, AddCustomerActivity.class);
                editIntent.putExtra("customer_id", clicked.id);
                startActivity(editIntent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCustomers(edtCustomerSearch.getText().toString());
    }

    private void loadCustomers(String query) {
        customerList.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String selection = null;
        String[] selectionArgs = null;

        if (query != null && !query.isEmpty()) {
            selection = "name LIKE ? OR phone LIKE ?";
            selectionArgs = new String[]{"%" + query + "%", "%" + query + "%"};
        }

        Cursor cursor = db.query("customers", null, selection, selectionArgs, null, null, "name ASC");

        while (cursor.moveToNext()) {
            Customer c = new Customer();
            c.id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
            c.name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
            c.phone = cursor.getString(cursor.getColumnIndexOrThrow("phone"));
            c.address = cursor.getString(cursor.getColumnIndexOrThrow("address"));
            c.totalPurchases = cursor.getDouble(cursor.getColumnIndexOrThrow("total_purchases"));
            c.amountPaid = cursor.getDouble(cursor.getColumnIndexOrThrow("amount_paid"));
            c.balance = cursor.getDouble(cursor.getColumnIndexOrThrow("balance"));
            customerList.add(c);
        }
        cursor.close();

        if (adapter == null) {
            adapter = new CustomerAdapter(this, customerList);
            lstCustomers.setAdapter(adapter);
        } else {
            adapter.notifyDataSetChanged();
        }
    }

    static class Customer {
        int id;
        String name;
        String phone;
        String address;
        double totalPurchases;
        double amountPaid;
        double balance;
    }

    private class CustomerAdapter extends BaseAdapter {
        private Context context;
        private ArrayList<Customer> items;

        public CustomerAdapter(Context context, ArrayList<Customer> items) {
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
                convertView = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_2, parent, false);
            }
            Customer c = items.get(position);

            TextView text1 = (TextView) convertView.findViewById(android.R.id.text1);
            TextView text2 = (TextView) convertView.findViewById(android.R.id.text2);

            text1.setText(c.name + " (" + c.phone + ")");
            text1.setTextSize(16sp);

            String info = "Purchases: $" + String.format("%.2f", c.totalPurchases) + " | Due: $" + String.format("%.2f", c.balance);
            text2.setText(info);
            text2.setTextColor(c.balance > 0 ? Color.RED : Color.GRAY);

            return convertView;
        }
    }
}