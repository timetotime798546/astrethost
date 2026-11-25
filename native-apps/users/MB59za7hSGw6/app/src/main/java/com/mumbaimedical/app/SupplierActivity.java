package com.mumbaimedical.app;

import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class SupplierActivity extends Activity {

    private DatabaseHelper dbHelper;
    private ListView lvSuppliers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_supplier);

        dbHelper = new DatabaseHelper(this);
        lvSuppliers = findViewById(R.id.lvSuppliers);

        loadSuppliers();
    }

    private void loadSuppliers() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id AS _id, name, phone, pending_amount FROM " + DatabaseHelper.TABLE_SUPPLIERS, null);

        String[] from = new String[]{"name", "phone", "pending_amount"};
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
                    double amt = cursor.getDouble(cursor.getColumnIndexOrThrow("pending_amount"));
                    ((TextView) view).setText(String.format("Pending: ₹ %.2f", amt));
                    return true;
                }
                return false;
            }
        });

        lvSuppliers.setAdapter(adapter);
    }
}