package com.kkgstore.store;

import android.app.Activity;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class AddEditCustomerActivity extends Activity {

    private EditText etName, etMobile, etOpeningBalance;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DemoLockManager.checkAndEnforceLock(this)) return;

        setContentView(R.layout.activity_add_edit_customer);
        dbHelper = new DatabaseHelper(this);

        etName = (EditText) findViewById(R.id.et_customer_name);
        etMobile = (EditText) findViewById(R.id.et_customer_mobile);
        etOpeningBalance = (EditText) findViewById(R.id.et_customer_opening_balance);
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
        String openBalStr = etOpeningBalance.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "कृपया ग्राहक का नाम दर्ज करें", Toast.LENGTH_SHORT).show();
            return;
        }

        double openBal = 0;
        if (!openBalStr.isEmpty()) {
            try { openBal = Double.parseDouble(openBalStr); } catch (Exception ignored) {}
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("mobile", mobile);
        cv.put("opening_balance", openBal);
        cv.put("is_active", 1);

        long id = db.insert("customers", null, cv);
        if (id > 0) {
            Toast.makeText(this, "ग्राहक सफलतापूर्वक जोड़ा गया", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "ग्राहक जोड़ने में समस्या आई", Toast.LENGTH_SHORT).show();
        }
    }
}