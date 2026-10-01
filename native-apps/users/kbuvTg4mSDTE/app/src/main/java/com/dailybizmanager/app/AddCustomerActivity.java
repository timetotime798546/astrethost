package com.dailybizmanager.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class AddCustomerActivity extends Activity {

    private DatabaseHelper dbHelper;
    private EditText edtCustName, edtCustPhone, edtCustAddress;
    private Button btnSaveCustomer, btnDeleteCustomer;
    private int editCustomerId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_customer);

        dbHelper = new DatabaseHelper(this);

        TextView txtCustFormTitle = (TextView) findViewById(R.id.txtCustFormTitle);
        edtCustName = (EditText) findViewById(R.id.edtCustName);
        edtCustPhone = (EditText) findViewById(R.id.edtCustPhone);
        edtCustAddress = (EditText) findViewById(R.id.edtCustAddress);

        btnSaveCustomer = (Button) findViewById(R.id.btnSaveCustomer);
        Button btnCancelCust = (Button) findViewById(R.id.btnCancelCust);
        btnDeleteCustomer = (Button) findViewById(R.id.btnDeleteCustomer);

        if (getIntent().hasExtra("customer_id")) {
            editCustomerId = getIntent().getIntExtra("customer_id", -1);
            txtCustFormTitle.setText("Edit Customer Profile");
            btnDeleteCustomer.setVisibility(View.VISIBLE);
            loadCustomerDetails();
        }

        btnCancelCust.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnSaveCustomer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveCustomer();
            }
        });

        btnDeleteCustomer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmDelete();
            }
        });
    }

    private void loadCustomerDetails() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM customers WHERE id = ?", new String[]{String.valueOf(editCustomerId)});
        if (cursor.moveToFirst()) {
            edtCustName.setText(cursor.getString(cursor.getColumnIndexOrThrow("name")));
            edtCustPhone.setText(cursor.getString(cursor.getColumnIndexOrThrow("phone")));
            edtCustAddress.setText(cursor.getString(cursor.getColumnIndexOrThrow("address")));
        }
        cursor.close();
    }

    private void saveCustomer() {
        String name = edtCustName.getText().toString().trim();
        String phone = edtCustPhone.getText().toString().trim();
        String address = edtCustAddress.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Name and Phone are required", Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("phone", phone);
        values.put("address", address);

        if (editCustomerId == -1) {
            values.put("total_purchases", 0.0);
            values.put("amount_paid", 0.0);
            values.put("balance", 0.0);

            long result = db.insert("customers", null, values);
            if (result != -1) {
                Toast.makeText(this, "Customer added successfully", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Error inserting customer record", Toast.LENGTH_SHORT).show();
            }
        } else {
            db.update("customers", values, "id = ?", new String[]{String.valueOf(editCustomerId)});
            Toast.makeText(this, "Customer details updated", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Customer")
                .setMessage("Are you sure you want to delete this customer?")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SQLiteDatabase db = dbHelper.getWritableDatabase();
                        db.delete("customers", "id = ?", new String[]{String.valueOf(editCustomerId)});
                        Toast.makeText(AddCustomerActivity.this, "Customer deleted", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}