package com.mumbaimedical.app;

import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class CustomerActivity extends Activity {

    private DatabaseHelper dbHelper;
    private ListView lvCustomers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer);

        dbHelper = new DatabaseHelper(this);
        lvCustomers = findViewById(R.id.lvCustomers);

        loadCustomers();
    }

    private void loadCustomers() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id AS _id, name, phone, pending_balance FROM " + DatabaseHelper.TABLE_CUSTOMERS, null);

        String[] from = new String[]{"name", "phone", "pending_balance"};
        int[] to = new int[]{R.id.tvSaleCust, R.id.tvSaleDate, R.id.tvSaleAmount};

        SimpleCursorAdapter adapter = new SimpleCursorAdapter(
                this,
                R.layout.item_sale,
                cursor,
                from,
                to,
                0
        );

        adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                if (view.getId() == R.id.tvSaleAmount) {
                    double bal = cursor.getDouble(cursor.getColumnIndexOrThrow("pending_balance"));
                    ((TextView) view).setText(String.format("Due: ₹ %.2f", bal));
                    return true;
                }
                return false;
            }
        });

        lvCustomers.setAdapter(adapter);
    }
}