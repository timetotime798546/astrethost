package com.mumbaimedical.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class CustomerActivity extends Activity {

    private DatabaseHelper dbHelper;
    private ListView lvCustomers;
    private EditText etSearchCustomer;
    private Button btnAddCustomer;
    private TextView tvEmptyCustomers;
    private SimpleCursorAdapter adapter;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer);

        dbHelper = new DatabaseHelper(this);
        lvCustomers = findViewById(R.id.lvCustomers);
        etSearchCustomer = findViewById(R.id.etSearchCustomer);
        btnAddCustomer = findViewById(R.id.btnAddCustomer);
        tvEmptyCustomers = findViewById(R.id.tvEmptyCustomers);

        btnAddCustomer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(CustomerActivity.this, AddCustomerActivity.class));
            }
        });

        etSearchCustomer.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadCustomers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        lvCustomers.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(CustomerActivity.this, CustomerKhataActivity.class);
                intent.putExtra("customer_id", id);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCustomers(etSearchCustomer.getText().toString());
    }

    private void loadCustomers(String query) {
        Cursor cursor = dbHelper.searchCustomers(query);

        if (cursor == null || cursor.getCount() == 0) {
            tvEmptyCustomers.setVisibility(View.VISIBLE);
        } else {
            tvEmptyCustomers.setVisibility(View.GONE);
        }

        String[] from = new String[]{"name", "phone", "address", "pending_balance", "image_path"};
        int[] to = new int[]{R.id.tvCustomerName, R.id.tvCustomerPhone, R.id.tvCustomerAddress, R.id.tvPendingBalance, R.id.ivCustomerItemThumb};

        adapter = new SimpleCursorAdapter(
                this,
                R.layout.item_customer,
                cursor,
                from,
                to,
                0
        );

        adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                int id = view.getId();
                if (id == R.id.ivCustomerItemThumb) {
                    String photoPath = cursor.getString(columnIndex);
                    PhotoHelper.displayThumbnail(CustomerActivity.this, (ImageView) view, photoPath, 100, 100);
                    return true;
                } else if (id == R.id.tvCustomerPhone) {
                    String phone = cursor.getString(cursor.getColumnIndexOrThrow("phone"));
                    ((TextView) view).setText("Phone: " + (phone != null && !phone.isEmpty() ? phone : "N/A"));
                    return true;
                } else if (id == R.id.tvCustomerAddress) {
                    String addr = cursor.getString(cursor.getColumnIndexOrThrow("address"));
                    ((TextView) view).setText("Address: " + (addr != null && !addr.isEmpty() ? addr : "N/A"));
                    return true;
                } else if (id == R.id.tvPendingBalance) {
                    double balance = cursor.getDouble(cursor.getColumnIndexOrThrow("pending_balance"));
                    TextView tv = (TextView) view;
                    if (balance > 0) {
                        tv.setText(String.format("Due: ₹ %.2f", balance));
                        tv.setTextColor(getResources().getColor(R.color.warning_red));
                    } else if (balance < 0) {
                        tv.setText(String.format("Advance: ₹ %.2f", Math.abs(balance)));
                        tv.setTextColor(getResources().getColor(R.color.success_green));
                    } else {
                        tv.setText("Clear (₹ 0.00)");
                        tv.setTextColor(getResources().getColor(R.color.text_muted));
                    }
                    return true;
                }
                return false;
            }
        });

        lvCustomers.setAdapter(adapter);
    }
}