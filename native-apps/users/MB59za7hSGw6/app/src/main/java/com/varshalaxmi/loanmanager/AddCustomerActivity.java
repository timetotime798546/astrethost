package com.varshalaxmi.loanmanager;

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
    private EditText etName, etPhone, etAddress, etNotes;
    private long customerId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_customer);

        dbHelper = new DatabaseHelper(this);

        TextView tvTitle = (TextView) findViewById(R.id.tv_form_title);
        etName = (EditText) findViewById(R.id.et_cust_name);
        etPhone = (EditText) findViewById(R.id.et_cust_phone);
        etAddress = (EditText) findViewById(R.id.et_cust_address);
        etNotes = (EditText) findViewById(R.id.et_cust_notes);
        Button btnSave = (Button) findViewById(R.id.btn_save_customer);

        if (getIntent().hasExtra("customer_id")) {
            customerId = getIntent().getLongExtra("customer_id", -1);
            tvTitle.setText("Edit Borrower Profile");
            loadCustomerData(customerId);
        }

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveCustomer();
            }
        });
    }

    private void loadCustomerData(long id) {
        Cursor cursor = dbHelper.getCustomerById(id);
        if (cursor != null && cursor.moveToFirst()) {
            etName.setText(cursor.getString(cursor.getColumnIndexOrThrow("name")));
            etPhone.setText(cursor.getString(cursor.getColumnIndexOrThrow("phone")));
            etAddress.setText(cursor.getString(cursor.getColumnIndexOrThrow("address")));
            etNotes.setText(cursor.getString(cursor.getColumnIndexOrThrow("notes")));
            cursor.close();
        }
    }

    private void saveCustomer() {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String notes = etNotes.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter borrower name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (customerId > 0) {
            boolean updated = dbHelper.updateCustomer(customerId, name, phone, address, notes);
            if (updated) {
                Toast.makeText(this, "Customer updated successfully", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Failed to update customer", Toast.LENGTH_SHORT).show();
            }
        } else {
            long newId = dbHelper.addCustomer(name, phone, address, notes);
            if (newId > 0) {
                Toast.makeText(this, "Borrower created successfully", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Failed to save customer", Toast.LENGTH_SHORT).show();
            }
        }
    }
}