package com.mumbaimedical.app;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class AddCustomerActivity extends Activity {

    private DatabaseHelper dbHelper;
    private TextView tvTitle;
    private EditText etCustName, etCustPhone, etCustAddress;
    private Button btnSaveCustomer;
    private long customerId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_customer);

        dbHelper = new DatabaseHelper(this);

        tvTitle = findViewById(R.id.tvTitle);
        etCustName = findViewById(R.id.etCustName);
        etCustPhone = findViewById(R.id.etCustPhone);
        etCustAddress = findViewById(R.id.etCustAddress);
        btnSaveCustomer = findViewById(R.id.btnSaveCustomer);

        if (getIntent().hasExtra("customer_id")) {
            customerId = getIntent().getLongExtra("customer_id", -1);
            tvTitle.setText("Edit Customer Details");
            btnSaveCustomer.setText("Update Customer Info");
            loadCustomerDetails();
        }

        btnSaveCustomer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveCustomer();
            }
        });
    }

    private void loadCustomerDetails() {
        Cursor cursor = dbHelper.getCustomerById(customerId);
        if (cursor != null && cursor.moveToFirst()) {
            etCustName.setText(cursor.getString(cursor.getColumnIndexOrThrow("name")));
            etCustPhone.setText(cursor.getString(cursor.getColumnIndexOrThrow("phone")));
            etCustAddress.setText(cursor.getString(cursor.getColumnIndexOrThrow("address")));
            cursor.close();
        }
    }

    private void saveCustomer() {
        String name = etCustName.getText().toString().trim();
        String phone = etCustPhone.getText().toString().trim();
        String address = etCustAddress.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "Customer Name is required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (customerId == -1) {
            long newId = dbHelper.addCustomer(name, phone, address);
            if (newId > 0) {
                Toast.makeText(this, "Customer Registered Successfully!", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Error saving customer", Toast.LENGTH_SHORT).show();
            }
        } else {
            boolean updated = dbHelper.updateCustomer(customerId, name, phone, address);
            if (updated) {
                Toast.makeText(this, "Customer Details Updated!", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Error updating customer", Toast.LENGTH_SHORT).show();
            }
        }
    }
}