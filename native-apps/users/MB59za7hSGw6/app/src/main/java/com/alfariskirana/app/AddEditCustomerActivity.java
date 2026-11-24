package com.alfariskirana.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class AddEditCustomerActivity extends Activity {

    private DatabaseHelper dbHelper;
    private EditText etName;
    private EditText etMobile;
    private EditText etBalance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_customer);

        dbHelper = new DatabaseHelper(this);
        etName = (EditText) findViewById(R.id.et_customer_name);
        etMobile = (EditText) findViewById(R.id.et_customer_mobile);
        etBalance = (EditText) findViewById(R.id.et_customer_opening_balance);
        Button btnSave = (Button) findViewById(R.id.btn_save_customer);

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveCustomer();
            }
        });
    }

    private void saveCustomer() {
        String name = etName.getText().toString().trim();
        String mobile = etMobile.getText().toString().trim();
        String balStr = etBalance.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter customer name", Toast.LENGTH_SHORT).show();
            return;
        }

        double balance = balStr.isEmpty() ? 0 : Double.parseDouble(balStr);
        Customer c = new Customer(0L, name, mobile, balance);
        dbHelper.saveCustomer(c);
        Toast.makeText(this, "Customer added successfully", Toast.LENGTH_SHORT).show();
        finish();
    }
}